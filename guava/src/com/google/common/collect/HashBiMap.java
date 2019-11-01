/*
 * Copyright (C) 2007 The Guava Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.common.collect;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.CollectPreconditions.checkNonnegative;
import static com.google.common.collect.CollectPreconditions.checkRemove;
import static com.google.common.collect.Hashing.smearedHash;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.base.Objects;
import com.google.common.collect.Maps.IteratorBasedAbstractMap;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.errorprone.annotations.concurrent.LazyInit;
import com.google.j2objc.annotations.RetainedWith;
import com.google.j2objc.annotations.WeakOuter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * A {@link BiMap} backed by two hash tables. This implementation allows null keys and values. A
 * {@code HashBiMap} and its inverse are both serializable.
 *
 * <p>This implementation guarantees insertion-based iteration order of its keys.
 *
 * <p>See the Guava User Guide article on <a href=
 * "https://github.com/google/guava/wiki/NewCollectionTypesExplained#bimap"> {@code BiMap} </a>.
 *
 * @author Louis Wasserman
 * @author Mike Bostock
 * @since 2.0
 */
@GwtCompatible(emulated = true)
public final class HashBiMap<K, V> extends IteratorBasedAbstractMap<K, V>
    implements BiMap<K, V>, Serializable {

  /** Returns a new, empty {@code HashBiMap} with the default initial capacity (16). */
  public static <K, V> HashBiMap<K, V> create() {
    return create(16);
  }

  /**
   * Constructs a new, empty bimap with the specified expected size.
   *
   * @param expectedSize the expected number of entries
   * @throws IllegalArgumentException if the specified expected size is negative
   */
  public static <K, V> HashBiMap<K, V> create(int expectedSize) {
    return new HashBiMap<>(expectedSize);
  }

  /**
   * Constructs a new bimap containing initial values from {@code map}. The bimap is created with an
   * initial capacity sufficient to hold the mappings in the specified map.
   */
  public static <K, V> HashBiMap<K, V> create(Map<? extends K, ? extends V> map) {
    HashBiMap<K, V> bimap = create(map.size());
    bimap.putAll(map);
    return bimap;
  }

  private static final class BiEntry<K, V> extends ImmutableEntry<K, V> {
    final int keyHash;
    final int valueHash;

    @Nullable BiEntry<K, V> nextInKToVBucket;
    @Nullable BiEntry<K, V> nextInVToKBucket;

    @Nullable BiEntry<K, V> nextInKeyInsertionOrder;
    @Nullable BiEntry<K, V> prevInKeyInsertionOrder;

    BiEntry(K key, int keyHash, V value, int valueHash) {
      super(key, value);
      this.keyHash = keyHash;
      this.valueHash = valueHash;
    }
  }

  private static final double LOAD_FACTOR = 1.0;

  private transient BiEntry<K, V>[] hashTableKToV;
  private transient BiEntry<K, V>[] hashTableVToK;
  private transient @Nullable BiEntry<K, V> firstInKeyInsertionOrder;
  private transient @Nullable BiEntry<K, V> lastInKeyInsertionOrder;
  private transient int size;
  private transient int mask;
  private transient int modCount;

  private HashBiMap(int expectedSize) {
    init(expectedSize);
  }

  private void init(int expectedSize) {
    checkNonnegative(expectedSize, "expectedSize");
    int tableSize = Hashing.closedTableSize(expectedSize, LOAD_FACTOR);
    this.hashTableKToV = createTable(tableSize);
    this.hashTableVToK = createTable(tableSize);
    this.firstInKeyInsertionOrder = null;
    this.lastInKeyInsertionOrder = null;
    this.size = 0;
    this.mask = tableSize - 1;
    this.modCount = 0;
  }

  /**
   * Finds and removes {@code entry} from the bucket linked lists in both the key-to-value direction
   * and the value-to-key direction.
   */
  private void delete(BiEntry<K, V> entry) {
    int keyBucket = entry.keyHash & mask;
    BiEntry<K, V> prevBucketEntry = null;
    for (BiEntry<K, V> bucketEntry = hashTableKToV[keyBucket];
        true;
        bucketEntry = bucketEntry.nextInKToVBucket) {
      if (bucketEntry == entry) {
        if (prevBucketEntry == null) {
          hashTableKToV[keyBucket] = entry.nextInKToVBucket;
        } else {
          prevBucketEntry.nextInKToVBucket = entry.nextInKToVBucket;
        }
        break;
      }
      prevBucketEntry = bucketEntry;
    }

    int valueBucket = entry.valueHash & mask;
    prevBucketEntry = null;
    for (BiEntry<K, V> bucketEntry = hashTableVToK[valueBucket];
        true;
        bucketEntry = bucketEntry.nextInVToKBucket) {
      if (bucketEntry == entry) {
        if (prevBucketEntry == null) {
          hashTableVToK[valueBucket] = entry.nextInVToKBucket;
        } else {
          prevBucketEntry.nextInVToKBucket = entry.nextInVToKBucket;
        }
        break;
      }
      prevBucketEntry = bucketEntry;
    }

    if (entry.prevInKeyInsertionOrder == null) {
      firstInKeyInsertionOrder = entry.nextInKeyInsertionOrder;
    } else {
      entry.prevInKeyInsertionOrder.nextInKeyInsertionOrder = entry.nextInKeyInsertionOrder;
    }

    if (entry.nextInKeyInsertionOrder == null) {
      lastInKeyInsertionOrder = entry.prevInKeyInsertionOrder;
    } else {
      entry.nextInKeyInsertionOrder.prevInKeyInsertionOrder = entry.prevInKeyInsertionOrder;
    }

    size--;
    modCount++;
  }

  private void insert(BiEntry<K, V> entry, @Nullable BiEntry<K, V> oldEntryForKey) {
    int keyBucket = entry.keyHash & mask;
    entry.nextInKToVBucket = hashTableKToV[keyBucket];
    hashTableKToV[keyBucket] = entry;

    int valueBucket = entry.valueHash & mask;
    entry.nextInVToKBucket = hashTableVToK[valueBucket];
    hashTableVToK[valueBucket] = entry;

    if (oldEntryForKey == null) {
      entry.prevInKeyInsertionOrder = lastInKeyInsertionOrder;
      entry.nextInKeyInsertionOrder = null;
      if (lastInKeyInsertionOrder == null) {
        firstInKeyInsertionOrder = entry;
      } else {
        lastInKeyInsertionOrder.nextInKeyInsertionOrder = entry;
      }
      lastInKeyInsertionOrder = entry;
    } else {
      entry.prevInKeyInsertionOrder = oldEntryForKey.prevInKeyInsertionOrder;
      if (entry.prevInKeyInsertionOrder == null) {
        firstInKeyInsertionOrder = entry;
      } else {
        entry.prevInKeyInsertionOrder.nextInKeyInsertionOrder = entry;
      }
      entry.nextInKeyInsertionOrder = oldEntryForKey.nextInKeyInsertionOrder;
      if (entry.nextInKeyInsertionOrder == null) {
        lastInKeyInsertionOrder = entry;
      } else {
        entry.nextInKeyInsertionOrder.prevInKeyInsertionOrder = entry;
      }
    }

    size++;
    modCount++;
  }

  private BiEntry<K, V> seekByKey(@Nullable Object key, int keyHash) {
    for (BiEntry<K, V> entry = hashTableKToV[keyHash & mask];
        entry != null;
        entry = entry.nextInKToVBucket) {
      if (keyHash == entry.keyHash && Objects.equal(key, entry.key)) {
        return entry;
      }
    }
    return null;
  }

  private BiEntry<K, V> seekByValue(@Nullable Object value, int valueHash) {
    for (BiEntry<K, V> entry = hashTableVToK[valueHash & mask];
        entry != null;
        entry = entry.nextInVToKBucket) {
      if (valueHash == entry.valueHash && Objects.equal(value, entry.value)) {
        return entry;
      }
    }
    return null;
  }

  @Override
  public boolean containsKey(@Nullable Object key) {
    return seekByKey(key, smearedHash(key)) != null;
  }

  /**
   * Returns {@code true} if this BiMap contains an entry whose value is equal to {@code value} (or,
   * equivalently, if this inverse view contains a key that is equal to {@code value}).
   *
   * <p>Due to the property that values in a BiMap are unique, this will tend to execute in
   * faster-than-linear time.
   *
   * @param value the object to search for in the values of this BiMap
   * @return true if a mapping exists from a key to the specified value
   */
  @Override
  public boolean containsValue(@Nullable Object value) {
    return seekByValue(value, smearedHash(value)) != null;
  }

  @Override
  public @Nullable V get(@Nullable Object key) {
    return Maps.valueOrNull(seekByKey(key, smearedHash(key)));
  }

  @CanIgnoreReturnValue
  @Override
  public V put(@Nullable K key, @Nullable V value) {
    return put(key, value, false);
  }

  private V put(@Nullable K key, @Nullable V value, boolean force) {
    int keyHash = smearedHash(key);
    int valueHash = smearedHash(value);

    BiEntry<K, V> oldEntryForKey = seekByKey(key, keyHash);
    if (oldEntryForKey != null
        && valueHash == oldEntryForKey.valueHash
        && Objects.equal(value, oldEntryForKey.value)) {
      return value;
    }

    BiEntry<K, V> oldEntryForValue = seekByValue(value, valueHash);
    if (oldEntryForValue != null) {
      if (force) {
        delete(oldEntryForValue);
      } else {
        throw new IllegalArgumentException("value already present: " + value);
      }
    }

    BiEntry<K, V> newEntry = new BiEntry<>(key, keyHash, value, valueHash);
    if (oldEntryForKey != null) {
      delete(oldEntryForKey);
      insert(newEntry, oldEntryForKey);
      oldEntryForKey.prevInKeyInsertionOrder = null;
      oldEntryForKey.nextInKeyInsertionOrder = null;
      return oldEntryForKey.value;
    } else {
      insert(newEntry, null);
      rehashIfNecessary();
      return null;
    }
  }

  @CanIgnoreReturnValue
  @Override
  @Nullable
  public V forcePut(@Nullable K key, @Nullable V value) {
    return put(key, value, true);
  }

  private @Nullable K putInverse(@Nullable V value, @Nullable K key, boolean force) {
    int valueHash = smearedHash(value);
    int keyHash = smearedHash(key);

    BiEntry<K, V> oldEntryForValue = seekByValue(value, valueHash);
    BiEntry<K, V> oldEntryForKey = seekByKey(key, keyHash);
    if (oldEntryForValue != null
        && keyHash == oldEntryForValue.keyHash
        && Objects.equal(key, oldEntryForValue.key)) {
      return key;
    } else if (oldEntryForKey != null && !force) {
      throw new IllegalArgumentException("key already present: " + key);
    }

    /*
     * The ordering here is important: if we deleted the key entry and then the value entry,
     * the key entry's prev or next pointer might point to the dead value entry, and when we
     * put the new entry in the key entry's position in iteration order, it might invalidate
     * the linked list.
     */

    if (oldEntryForValue != null) {
      delete(oldEntryForValue);
    }

    if (oldEntryForKey != null) {
      delete(oldEntryForKey);
    }

    BiEntry<K, V> newEntry = new BiEntry<>(key, keyHash, value, valueHash);
    insert(newEntry, oldEntryForKey);

    if (oldEntryForKey != null) {
      oldEntryForKey.prevInKeyInsertionOrder = null;
      oldEntryForKey.nextInKeyInsertionOrder = null;
    }
    if (oldEntryForValue != null) {
      oldEntryForValue.prevInKeyInsertionOrder = null;
      oldEntryForValue.nextInKeyInsertionOrder = null;
    }
    rehashIfNecessary();
    return Maps.keyOrNull(oldEntryForValue);
  }

  private void rehashIfNecessary() {
    BiEntry<K, V>[] oldKToV = hashTableKToV;
    if (Hashing.needsResizing(size, oldKToV.length, LOAD_FACTOR)) {
      int newTableSize = oldKToV.length * 2;

      this.hashTableKToV = createTable(newTableSize);
      this.hashTableVToK = createTable(newTableSize);
      this.mask = newTableSize - 1;
      this.size = 0;

      for (BiEntry<K, V> entry = firstInKeyInsertionOrder;
          entry != null;
          entry = entry.nextInKeyInsertionOrder) {
        insert(entry, entry);
      }
      this.modCount++;
    }
  }

  @SuppressWarnings("unchecked")
  private BiEntry<K, V>[] createTable(int length) {
    return new BiEntry[length];
  }

  @CanIgnoreReturnValue
  @Override
  @Nullable
  public V remove(@Nullable Object key) {
    BiEntry<K, V> entry = seekByKey(key, smearedHash(key));
    if (entry == null) {
      return null;
    } else {
      delete(entry);
      entry.prevInKeyInsertionOrder = null;
      entry.nextInKeyInsertionOrder = null;
      return entry.value;
    }
  }

  @Override
  public void clear() {
    size = 0;
    Arrays.fill(hashTableKToV, null);
    Arrays.fill(hashTableVToK, null);
    firstInKeyInsertionOrder = null;
    lastInKeyInsertionOrder = null;
    modCount++;
  }

  @Override
  public int size() {
    return size;
  }

  abstract class Itr<T> implements Iterator<T> {
    BiEntry<K, V> next = firstInKeyInsertionOrder;
    BiEntry<K, V> toRemove = null;
    int expectedModCount = modCount;
    int remaining = size();

    @Override
    public boolean hasNext() {
      if (modCount != expectedModCount) {
        throw new ConcurrentModificationException();
      }
      return next != null && remaining > 0;
    }

    @Override
    public T next() {
      if (!hasNext()) {
        throw new NoSuchElementException();
      }

      BiEntry<K, V> entry = next;
      next = entry.nextInKeyInsertionOrder;
      toRemove = entry;
      remaining--;
      return output(entry);
    }

    @Override
    public void remove() {
      if (modCount != expectedModCount) {
        throw new ConcurrentModificationException();
      }
      checkRemove(toRemove != null);
      delete(toRemove);
      expectedModCount = modCount;
      toRemove = null;
    }

    abstract T output(BiEntry<K, V> entry);
  }

  @Override
  public Set<K> keySet() {
    return new KeySet();
  }

  @WeakOuter
  private final class KeySet extends Maps.KeySet<K, V> {
    KeySet() {
      super(HashBiMap.this);
    }

    @Override
    public Iterator<K> iterator() {
      return new Itr<K>() {
        @Override
        K output(BiEntry<K, V> entry) {
          return entry.key;
        }
      };
    }

    @Override
    public boolean remove(@Nullable Object o) {
      BiEntry<K, V> entry = seekByKey(o, smearedHash(o));
      if (entry == null) {
        return false;
      } else {
        delete(entry);
        entry.prevInKeyInsertionOrder = null;
        entry.nextInKeyInsertionOrder = null;
        return true;
      }
    }
  }

  @Override
  public Set<V> values() {
    return inverse().keySet();
  }

  @Override
  Iterator<Entry<K, V>> entryIterator() {
    return new Itr<Entry<K, V>>() {
      @Override
      Entry<K, V> output(BiEntry<K, V> entry) {
        return new MapEntry(entry);
      }

      class MapEntry extends AbstractMapEntry<K, V> {
        BiEntry<K, V> delegate;

        MapEntry(BiEntry<K, V> entry) {
          this.delegate = entry;
        }

        @Override
        public K getKey() {
          return delegate.key;
        }

        @Override
        public V getValue() {
          return delegate.value;
        }

        @Override
        public V setValue(V value) {
          V oldValue = delegate.value;
          int valueHash = smearedHash(value);
          if (valueHash == delegate.valueHash && Objects.equal(value, oldValue)) {
            return value;
          }
          checkArgument(seekByValue(value, valueHash) == null, "value already present: %s", value);
          delete(delegate);
          BiEntry<K, V> newEntry = new BiEntry<>(delegate.key, delegate.keyHash, value, valueHash);
          insert(newEntry, delegate);
          delegate.prevInKeyInsertionOrder = null;
          delegate.nextInKeyInsertionOrder = null;
          expectedModCount = modCount;
          if (toRemove == delegate) {
            toRemove = newEntry;
          }
          delegate = newEntry;
          return oldValue;
        }
      }
    };
  }

  @Override
  public void forEach(BiConsumer<? super K, ? super V> action) {
    checkNotNull(action);
    for (BiEntry<K, V> entry = firstInKeyInsertionOrder;
        entry != null;
        entry = entry.nextInKeyInsertionOrder) {
      action.accept(entry.key, entry.value);
    }
  }

  @Override
  public void replaceAll(BiFunction<? super K, ? super V, ? extends V> function) {
    checkNotNull(function);
    BiEntry<K, V> oldFirst = firstInKeyInsertionOrder;
    clear();
    for (BiEntry<K, V> entry = oldFirst; entry != null; entry = entry.nextInKeyInsertionOrder) {
      put(entry.key, function.apply(entry.key, entry.value));
    }
  }

  @LazyInit @MonotonicNonNull @RetainedWith private transient BiMap<V, K> inverse;

  @Override
  public BiMap<V, K> inverse() {
    BiMap<V, K> result = inverse;
    return (result == null) ? inverse = new Inverse() : result;
  }

  private final class Inverse extends IteratorBasedAbstractMap<V, K>
      implements BiMap<V, K>, Serializable {
    BiMap<K, V> forward() {
      return HashBiMap.this;
    }

    @Override
    public int size() {
      return size;
    }

    @Override
    public void clear() {
      forward().clear();
    }

    @Override
    public boolean containsKey(@Nullable Object value) {
      return forward().containsValue(value);
    }

    @Override
    public K get(@Nullable Object value) {
      return Maps.keyOrNull(seekByValue(value, smearedHash(value)));
    }

    @CanIgnoreReturnValue
    @Override
    @Nullable
    public K put(@Nullable V value, @Nullable K key) {
      return putInverse(value, key, false);
    }

    @Override
    @Nullable
    public K forcePut(@Nullable V value, @Nullable K key) {
      return putInverse(value, key, true);
    }

    @Override
    @Nullable
    public K remove(@Nullable Object value) {
      BiEntry<K, V> entry = seekByValue(value, smearedHash(value));
      if (entry == null) {
        return null;
      } else {
        delete(entry);
        entry.prevInKeyInsertionOrder = null;
        entry.nextInKeyInsertionOrder = null;
        return entry.key;
      }
    }

    @Override
    public BiMap<K, V> inverse() {
      return forward();
    }

    @Override
    public Set<V> keySet() {
      return new InverseKeySet();
    }

    @WeakOuter
    private final class InverseKeySet extends Maps.KeySet<V, K> {
      InverseKeySet() {
        super(Inverse.this);
      }

      @Override
      public boolean remove(@Nullable Object o) {
        BiEntry<K, V> entry = seekByValue(o, smearedHash(o));
        if (entry == null) {
          return false;
        } else {
          delete(entry);
          return true;
        }
      }

      @Override
      public Iterator<V> iterator() {
        return new Itr<V>() {
          @Override
          V output(BiEntry<K, V> entry) {
            return entry.value;
          }
        };
      }
    }

    @Override
    public Set<K> values() {
      return forward().keySet();
    }

    @Override
    Iterator<Entry<V, K>> entryIterator() {
      return new Itr<Entry<V, K>>() {
        @Override
        Entry<V, K> output(BiEntry<K, V> entry) {
          return new InverseEntry(entry);
        }

        class InverseEntry extends AbstractMapEntry<V, K> {
          BiEntry<K, V> delegate;

          InverseEntry(BiEntry<K, V> entry) {
            this.delegate = entry;
          }

          @Override
          public V getKey() {
            return delegate.value;
          }

          @Override
          public K getValue() {
            return delegate.key;
          }

          @Override
          public K setValue(K key) {
            K oldKey = delegate.key;
            int keyHash = smearedHash(key);
            if (keyHash == delegate.keyHash && Objects.equal(key, oldKey)) {
              return key;
            }
            checkArgument(seekByKey(key, keyHash) == null, "value already present: %s", key);
            delete(delegate);
            BiEntry<K, V> newEntry =
                new BiEntry<>(key, keyHash, delegate.value, delegate.valueHash);
            delegate = newEntry;
            insert(newEntry, null);
            expectedModCount = modCount;
            return oldKey;
          }
        }
      };
    }

    @Override
    public void forEach(BiConsumer<? super V, ? super K> action) {
      checkNotNull(action);
      HashBiMap.this.forEach((k, v) -> action.accept(v, k));
    }

    @Override
    public void replaceAll(BiFunction<? super V, ? super K, ? extends K> function) {
      checkNotNull(function);
      BiEntry<K, V> oldFirst = firstInKeyInsertionOrder;
      clear();
      for (BiEntry<K, V> entry = oldFirst; entry != null; entry = entry.nextInKeyInsertionOrder) {
        put(entry.value, function.apply(entry.value, entry.key));
      }
    }

    Object writeReplace() {
      return new InverseSerializedForm<>(HashBiMap.this);
    }
  }

  private static final class InverseSerializedForm<K, V> implements Serializable {
    private final HashBiMap<K, V> bimap;

    InverseSerializedForm(HashBiMap<K, V> bimap) {
      this.bimap = bimap;
    }

    Object readResolve() {
      return bimap.inverse();
    }
  }

  /**
   * @serialData the number of entries, first key, first value, second key, second value, and so on.
   */
  @GwtIncompatible // java.io.ObjectOutputStream
  private void writeObject(ObjectOutputStream stream) throws IOException {
    stream.defaultWriteObject();
    Serialization.writeMap(this, stream);
  }

  @GwtIncompatible // java.io.ObjectInputStream
  private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
    stream.defaultReadObject();
    int size = Serialization.readCount(stream);
    init(16); // resist hostile attempts to allocate gratuitous heap
    Serialization.populateMap(this, stream, size);
  }

  @GwtIncompatible // Not needed in emulated source
  private static final long serialVersionUID = 0;
}
