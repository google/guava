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
import static com.google.common.collect.Hashing.smearedHash;
import static java.util.Objects.requireNonNull;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.annotations.J2ktIncompatible;
import com.google.common.collect.Maps.IteratorBasedAbstractMap;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.errorprone.annotations.concurrent.LazyInit;
import com.google.j2objc.annotations.RetainedWith;
import com.google.j2objc.annotations.Weak;
import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import org.jspecify.annotations.Nullable;

/**
 * A {@link BiMap} backed by two hash tables. This implementation allows null keys and values. A
 * {@code HashBiMap} and its inverse are both serializable.
 *
 * <p>This implementation guarantees insertion-based iteration order of its keys.
 *
 * <p>See the Guava User Guide article on <a href=
 * "https://github.com/google/guava/wiki/NewCollectionTypesExplained#bimap">{@code BiMap} </a>.
 *
 * @author Louis Wasserman
 * @author Mike Bostock
 * @since 2.0
 */
@GwtCompatible
public final class HashBiMap<K extends @Nullable Object, V extends @Nullable Object>
    extends IteratorBasedAbstractMap<K, V> implements BiMap<K, V>, Serializable {

  /** Returns a new, empty {@code HashBiMap} with the default initial capacity (16). */
  public static <K extends @Nullable Object, V extends @Nullable Object> HashBiMap<K, V> create() {
    return create(16);
  }

  /**
   * Constructs a new, empty bimap with the specified expected size.
   *
   * @param expectedSize the expected number of entries
   * @throws IllegalArgumentException if the specified expected size is negative
   */
  public static <K extends @Nullable Object, V extends @Nullable Object> HashBiMap<K, V> create(
      int expectedSize) {
    return new HashBiMap<>(expectedSize);
  }

  /**
   * Constructs a new bimap containing initial values from {@code map}. The bimap is created with an
   * initial capacity sufficient to hold the mappings in the specified map.
   */
  public static <K extends @Nullable Object, V extends @Nullable Object> HashBiMap<K, V> create(
      Map<? extends K, ? extends V> map) {
    HashBiMap<K, V> bimap = create(map.size());
    bimap.putAll(map);
    return bimap;
  }

  /**
   * An immutable key-value pair with mutable links to other pairs in its hash buckets and in
   * iteration order.
   */
  static final class Node<K extends @Nullable Object, V extends @Nullable Object> {
    @ParametricNullness final K key;
    @ParametricNullness final V value;

    final int keyHash;
    final int valueHash;

    // All Node instances are strongly reachable from owning HashBiMap through
    // "HashBiMap.hashTableKToV" and "Node.nextInKToVBucket" references.
    // Under that assumption, the remaining references can be safely marked as @Weak.
    // Using @Weak is necessary to avoid retain-cycles between Node instances on iOS,
    // which would cause memory leaks when non-empty HashBiMap with cyclic Node
    // instances is deallocated.
    @Nullable Node<K, V> nextInKToVBucket;
    @Weak @Nullable Node<K, V> nextInVToKBucket;

    @Weak @Nullable Node<K, V> nextInKeyInsertionOrder;
    @Weak @Nullable Node<K, V> prevInKeyInsertionOrder;

    Node(@ParametricNullness K key, int keyHash, @ParametricNullness V value, int valueHash) {
      this.key = key;
      this.value = value;
      this.keyHash = keyHash;
      this.valueHash = valueHash;
    }
  }

  private static final double LOAD_FACTOR = 1.0;

  /*
   * The following two arrays may *contain* nulls, but they are never *themselves* null: Even though
   * they are not initialized inline in the constructor, they are initialized from init(), which the
   * constructor calls (as does readObject()).
   */
  @SuppressWarnings("nullness:initialization.field.uninitialized") // For J2KT (see above)
  private transient @Nullable Node<K, V>[] hashTableKToV;

  @SuppressWarnings("nullness:initialization.field.uninitialized") // For J2KT (see above)
  private transient @Nullable Node<K, V>[] hashTableVToK;

  @Weak private transient @Nullable Node<K, V> firstInKeyInsertionOrder;
  @Weak private transient @Nullable Node<K, V> lastInKeyInsertionOrder;
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
   * Finds and removes {@code node} from the key-to-value hash table, the value-to-key hash table,
   * and the iteration-order chain. This includes clearing its own references to other entries.
   */
  private void delete(Node<K, V> node) {
    int keyBucket = node.keyHash & mask;
    Node<K, V> prevBucketNode = null;
    for (Node<K, V> bucketNode = hashTableKToV[keyBucket];
        true;
        bucketNode = bucketNode.nextInKToVBucket) {
      if (bucketNode == node) {
        if (prevBucketNode == null) {
          hashTableKToV[keyBucket] = node.nextInKToVBucket;
        } else {
          prevBucketNode.nextInKToVBucket = node.nextInKToVBucket;
        }
        break;
      }
      prevBucketNode = bucketNode;
    }

    int valueBucket = node.valueHash & mask;
    prevBucketNode = null;
    for (Node<K, V> bucketNode = hashTableVToK[valueBucket];
        true;
        bucketNode = bucketNode.nextInVToKBucket) {
      if (bucketNode == node) {
        if (prevBucketNode == null) {
          hashTableVToK[valueBucket] = node.nextInVToKBucket;
        } else {
          prevBucketNode.nextInVToKBucket = node.nextInVToKBucket;
        }
        break;
      }
      prevBucketNode = bucketNode;
    }

    if (node.prevInKeyInsertionOrder == null) {
      firstInKeyInsertionOrder = node.nextInKeyInsertionOrder;
    } else {
      node.prevInKeyInsertionOrder.nextInKeyInsertionOrder = node.nextInKeyInsertionOrder;
    }

    if (node.nextInKeyInsertionOrder == null) {
      lastInKeyInsertionOrder = node.prevInKeyInsertionOrder;
    } else {
      node.nextInKeyInsertionOrder.prevInKeyInsertionOrder = node.prevInKeyInsertionOrder;
    }

    node.prevInKeyInsertionOrder = null;
    node.nextInKeyInsertionOrder = null;
    node.nextInKToVBucket = null;
    node.nextInVToKBucket = null;

    size--;
    modCount++;
  }

  private void insertPlacingAtEndOfIterationOrder(Node<K, V> node) {
    insertIntoHashBucketsOnly(node);

    node.prevInKeyInsertionOrder = lastInKeyInsertionOrder;
    if (lastInKeyInsertionOrder == null) {
      firstInKeyInsertionOrder = node;
    } else {
      lastInKeyInsertionOrder.nextInKeyInsertionOrder = node;
    }
    lastInKeyInsertionOrder = node;
  }

  private void insertSplicingIntoIterationOrder(
      Node<K, V> node,
      @Nullable Node<K, V> prevInKeyInsertionOrder,
      @Nullable Node<K, V> nextInKeyInsertionOrder) {
    insertIntoHashBucketsOnly(node);

    node.prevInKeyInsertionOrder = prevInKeyInsertionOrder;
    if (prevInKeyInsertionOrder == null) {
      firstInKeyInsertionOrder = node;
    } else {
      prevInKeyInsertionOrder.nextInKeyInsertionOrder = node;
    }

    node.nextInKeyInsertionOrder = nextInKeyInsertionOrder;
    if (nextInKeyInsertionOrder == null) {
      lastInKeyInsertionOrder = node;
    } else {
      nextInKeyInsertionOrder.prevInKeyInsertionOrder = node;
    }
  }

  private void insertIntoHashBucketsOnly(Node<K, V> node) {
    int keyBucket = node.keyHash & mask;
    node.nextInKToVBucket = hashTableKToV[keyBucket];
    hashTableKToV[keyBucket] = node;

    int valueBucket = node.valueHash & mask;
    node.nextInVToKBucket = hashTableVToK[valueBucket];
    hashTableVToK[valueBucket] = node;

    size++;
    modCount++;
  }

  private void replaceNodeForKey(Node<K, V> oldNode, Node<K, V> newNode) {
    Node<K, V> prevInKeyInsertionOrder = oldNode.prevInKeyInsertionOrder;
    Node<K, V> nextInKeyInsertionOrder = oldNode.nextInKeyInsertionOrder;
    delete(oldNode); // clears the two fields we just read
    insertSplicingIntoIterationOrder(newNode, prevInKeyInsertionOrder, nextInKeyInsertionOrder);
  }

  private @Nullable Node<K, V> seekByKey(@Nullable Object key, int keyHash) {
    for (Node<K, V> node = hashTableKToV[keyHash & mask];
        node != null;
        node = node.nextInKToVBucket) {
      if (keyHash == node.keyHash && Objects.equals(key, node.key)) {
        return node;
      }
    }
    return null;
  }

  private @Nullable Node<K, V> seekByValue(@Nullable Object value, int valueHash) {
    for (Node<K, V> node = hashTableVToK[valueHash & mask];
        node != null;
        node = node.nextInVToKBucket) {
      if (valueHash == node.valueHash && Objects.equals(value, node.value)) {
        return node;
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
    return valueOrNull(seekByKey(key, smearedHash(key)));
  }

  @CanIgnoreReturnValue
  @Override
  public @Nullable V put(@ParametricNullness K key, @ParametricNullness V value) {
    return put(key, value, false);
  }

  private @Nullable V put(@ParametricNullness K key, @ParametricNullness V value, boolean force) {
    int keyHash = smearedHash(key);
    int valueHash = smearedHash(value);

    Node<K, V> oldNodeForKey = seekByKey(key, keyHash);
    if (oldNodeForKey != null
        && valueHash == oldNodeForKey.valueHash
        && Objects.equals(value, oldNodeForKey.value)) {
      return value;
    }

    Node<K, V> oldNodeForValue = seekByValue(value, valueHash);
    if (oldNodeForValue != null) {
      if (force) {
        delete(oldNodeForValue);
      } else {
        throw new IllegalArgumentException("value already present: " + value);
      }
    }

    Node<K, V> newNode = new Node<>(key, keyHash, value, valueHash);
    if (oldNodeForKey != null) {
      replaceNodeForKey(oldNodeForKey, newNode);
      return oldNodeForKey.value;
    } else {
      insertPlacingAtEndOfIterationOrder(newNode);
      rehashIfNecessary();
      return null;
    }
  }

  @CanIgnoreReturnValue
  @Override
  public @Nullable V forcePut(@ParametricNullness K key, @ParametricNullness V value) {
    return put(key, value, true);
  }

  @CanIgnoreReturnValue
  private @Nullable K putInverse(
      @ParametricNullness V value, @ParametricNullness K key, boolean force) {
    int valueHash = smearedHash(value);
    int keyHash = smearedHash(key);

    Node<K, V> oldNodeForValue = seekByValue(value, valueHash);
    Node<K, V> oldNodeForKey = seekByKey(key, keyHash);
    if (oldNodeForValue != null
        && keyHash == oldNodeForValue.keyHash
        && Objects.equals(key, oldNodeForValue.key)) {
      return key;
    } else if (oldNodeForKey != null && !force) {
      throw new IllegalArgumentException("key already present: " + key);
    }

    if (oldNodeForValue != null) {
      delete(oldNodeForValue);
    }

    Node<K, V> newNode = new Node<>(key, keyHash, value, valueHash);
    if (oldNodeForKey != null) {
      replaceNodeForKey(oldNodeForKey, newNode);
    } else {
      insertPlacingAtEndOfIterationOrder(newNode);
    }

    // TODO(cpovirk): Don't perform rehash check if we replaced an existing entry (as in `put`)?
    rehashIfNecessary();
    return keyOrNull(oldNodeForValue);
  }

  private void rehashIfNecessary() {
    @Nullable Node<K, V>[] oldKToV = hashTableKToV;
    if (Hashing.needsResizing(size, oldKToV.length, LOAD_FACTOR)) {
      int newTableSize = oldKToV.length * 2;

      this.hashTableKToV = createTable(newTableSize);
      this.hashTableVToK = createTable(newTableSize);
      this.mask = newTableSize - 1;
      this.size = 0;

      for (Node<K, V> node = firstInKeyInsertionOrder;
          node != null;
          node = node.nextInKeyInsertionOrder) {
        insertSplicingIntoIterationOrder(
            node, node.prevInKeyInsertionOrder, node.nextInKeyInsertionOrder);
      }
      this.modCount++;
    }
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  private @Nullable Node<K, V>[] createTable(int length) {
    return new @Nullable Node[length];
  }

  @CanIgnoreReturnValue
  @Override
  public @Nullable V remove(@Nullable Object key) {
    Node<K, V> node = seekByKey(key, smearedHash(key));
    if (node == null) {
      return null;
    } else {
      delete(node);
      node.prevInKeyInsertionOrder = null;
      node.nextInKeyInsertionOrder = null;
      return node.value;
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

  private abstract static class BiIterator<
          K extends @Nullable Object, V extends @Nullable Object, T extends @Nullable Object>
      implements Iterator<T> {
    final HashBiMap<K, V> biMap;

    @Nullable Node<K, V> next;
    @Nullable Node<K, V> toRemove;
    int expectedModCount;
    int remaining;

    BiIterator(HashBiMap<K, V> biMap) {
      this.biMap = biMap;
      next = biMap.firstInKeyInsertionOrder;
      expectedModCount = biMap.modCount;
      remaining = biMap.size();
    }

    @Override
    public final boolean hasNext() {
      if (biMap.modCount != expectedModCount) {
        throw new ConcurrentModificationException();
      }
      return next != null && remaining > 0;
    }

    @Override
    public final T next() {
      if (!hasNext()) {
        throw new NoSuchElementException();
      }

      // requireNonNull is safe because of the hasNext check.
      Node<K, V> node = requireNonNull(next);
      next = node.nextInKeyInsertionOrder;
      toRemove = node;
      remaining--;
      return output(node);
    }

    @Override
    public final void remove() {
      if (biMap.modCount != expectedModCount) {
        throw new ConcurrentModificationException();
      }
      if (toRemove == null) {
        throw new IllegalStateException("no calls to next() since the last call to remove()");
      }
      biMap.delete(toRemove);
      expectedModCount = biMap.modCount;
      toRemove = null;
    }

    abstract T output(Node<K, V> node);
  }

  @Override
  public Set<K> keySet() {
    return new KeySet();
  }

  private final class KeySet extends Maps.KeySet<K, V> {
    KeySet() {
      super(HashBiMap.this);
    }

    @Override
    public Iterator<K> iterator() {
      return new BiIterator<K, V, K>(HashBiMap.this) {
        @Override
        @ParametricNullness
        K output(Node<K, V> node) {
          return node.key;
        }
      };
    }

    @Override
    public boolean remove(@Nullable Object o) {
      Node<K, V> node = seekByKey(o, smearedHash(o));
      if (node == null) {
        return false;
      } else {
        delete(node);
        node.prevInKeyInsertionOrder = null;
        node.nextInKeyInsertionOrder = null;
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
    return new BiIterator<K, V, Entry<K, V>>(HashBiMap.this) {
      @Override
      Entry<K, V> output(Node<K, V> node) {
        return new MapEntry(node);
      }

      final class MapEntry extends AbstractMapEntry<K, V> {
        private Node<K, V> node;

        MapEntry(Node<K, V> node) {
          this.node = node;
        }

        @Override
        @ParametricNullness
        public K getKey() {
          return node.key;
        }

        @Override
        @ParametricNullness
        public V getValue() {
          return node.value;
        }

        @Override
        @ParametricNullness
        public V setValue(@ParametricNullness V value) {
          V oldValue = node.value;
          int valueHash = smearedHash(value);
          if (valueHash == node.valueHash && Objects.equals(value, oldValue)) {
            return value;
          }
          checkArgument(seekByValue(value, valueHash) == null, "value already present: %s", value);
          Node<K, V> newNode = new Node<>(node.key, node.keyHash, value, valueHash);
          replaceNodeForKey(node, newNode);
          expectedModCount = modCount;
          if (Objects.equals(toRemove, node)) {
            toRemove = newNode;
          }
          node = newNode;
          return oldValue;
        }
      }
    };
  }

  @Override
  public void forEach(BiConsumer<? super K, ? super V> action) {
    checkNotNull(action);
    for (Node<K, V> node = firstInKeyInsertionOrder;
        node != null;
        node = node.nextInKeyInsertionOrder) {
      action.accept(node.key, node.value);
    }
  }

  @Override
  public void replaceAll(BiFunction<? super K, ? super V, ? extends V> function) {
    checkNotNull(function);
    Node<K, V> oldFirst = firstInKeyInsertionOrder;
    clear();
    for (Node<K, V> node = oldFirst; node != null; node = node.nextInKeyInsertionOrder) {
      put(node.key, function.apply(node.key, node.value));
    }
  }

  @LazyInit @RetainedWith private transient @Nullable BiMap<V, K> inverse;

  @Override
  public BiMap<V, K> inverse() {
    BiMap<V, K> result = inverse;
    return (result == null) ? inverse = new Inverse<>(this) : result;
  }

  private static final class Inverse<K extends @Nullable Object, V extends @Nullable Object>
      extends IteratorBasedAbstractMap<V, K> implements BiMap<V, K>, Serializable {
    final HashBiMap<K, V> obverse;

    Inverse(HashBiMap<K, V> obverse) {
      this.obverse = obverse;
    }

    @Override
    public int size() {
      return obverse.size;
    }

    @Override
    public void clear() {
      obverse.clear();
    }

    @Override
    public boolean containsKey(@Nullable Object key) {
      Object obverseValue = key;
      return obverse.containsValue(obverseValue);
    }

    @Override
    public @Nullable K get(@Nullable Object key) {
      Object obverseValue = key;
      return keyOrNull(obverse.seekByValue(obverseValue, smearedHash(obverseValue)));
    }

    @CanIgnoreReturnValue
    @Override
    public @Nullable K put(@ParametricNullness V key, @ParametricNullness K value) {
      K obverseKey = value;
      V obverseValue = key;
      return obverse.putInverse(obverseValue, obverseKey, false);
    }

    @CanIgnoreReturnValue
    @Override
    public @Nullable K forcePut(@ParametricNullness V key, @ParametricNullness K value) {
      K obverseKey = value;
      V obverseValue = key;
      return obverse.putInverse(obverseValue, obverseKey, true);
    }

    @CanIgnoreReturnValue
    @Override
    public @Nullable K remove(@Nullable Object key) {
      Object obverseValue = key;
      Node<K, V> node = obverse.seekByValue(obverseValue, smearedHash(obverseValue));
      if (node == null) {
        return null;
      }
      obverse.delete(node);
      node.prevInKeyInsertionOrder = null;
      node.nextInKeyInsertionOrder = null;
      return node.key;
    }

    @Override
    public BiMap<K, V> inverse() {
      return obverse;
    }

    @Override
    public Set<V> keySet() {
      return new InverseKeySet();
    }

    private final class InverseKeySet extends Maps.KeySet<V, K> {
      InverseKeySet() {
        super(Inverse.this);
      }

      @Override
      public boolean remove(@Nullable Object o) {
        Node<K, V> node = obverse.seekByValue(o, smearedHash(o));
        if (node == null) {
          return false;
        } else {
          obverse.delete(node);
          return true;
        }
      }

      @Override
      public Iterator<V> iterator() {
        return new BiIterator<K, V, V>(obverse) {
          @Override
          @ParametricNullness
          V output(Node<K, V> node) {
            return node.value;
          }
        };
      }
    }

    @Override
    public Set<K> values() {
      return obverse.keySet();
    }

    @Override
    Iterator<Entry<V, K>> entryIterator() {
      return new BiIterator<K, V, Entry<V, K>>(obverse) {
        @Override
        Entry<V, K> output(Node<K, V> node) {
          return new InverseEntry(node);
        }

        final class InverseEntry extends AbstractMapEntry<V, K> {
          private Node<K, V> node;

          InverseEntry(Node<K, V> node) {
            this.node = node;
          }

          @Override
          @ParametricNullness
          public V getKey() {
            return node.value;
          }

          @Override
          @ParametricNullness
          public K getValue() {
            return node.key;
          }

          @Override
          @ParametricNullness
          public K setValue(@ParametricNullness K value) {
            K obverseKey = value;
            return setObverseKey(obverseKey);
          }

          @ParametricNullness
          private K setObverseKey(@ParametricNullness K obverseKey) {
            int obverseKeyHash = smearedHash(obverseKey);
            if (obverseKeyHash == node.keyHash && Objects.equals(obverseKey, node.key)) {
              return obverseKey;
            }
            checkArgument(
                obverse.seekByKey(obverseKey, obverseKeyHash) == null,
                "value already present: %s",
                obverseKey);
            obverse.delete(node);
            Node<K, V> newNode = new Node<>(obverseKey, obverseKeyHash, node.value, node.valueHash);
            obverse.insertPlacingAtEndOfIterationOrder(newNode);
            expectedModCount = obverse.modCount;
            K oldObverseKey = node.key;
            if (Objects.equals(toRemove, node)) {
              toRemove = newNode;
            }
            node = newNode;
            return oldObverseKey;
          }
        }
      };
    }

    @Override
    public void forEach(BiConsumer<? super V, ? super K> action) {
      checkNotNull(action);
      obverse.forEach((k, v) -> action.accept(v, k));
    }

    @Override
    public void replaceAll(BiFunction<? super V, ? super K, ? extends K> function) {
      checkNotNull(function);
      Node<K, V> oldFirst = obverse.firstInKeyInsertionOrder;
      clear();
      for (Node<K, V> node = oldFirst; node != null; node = node.nextInKeyInsertionOrder) {
        put(node.value, function.apply(node.value, node.key));
      }
    }

    Object writeReplace() {
      return new InverseSerializedForm<>(obverse);
    }

    @GwtIncompatible
    @J2ktIncompatible
        private void readObject(ObjectInputStream in) throws InvalidObjectException {
      throw new InvalidObjectException("Use InverseSerializedForm");
    }
  }

  private static <K extends @Nullable Object> @Nullable K keyOrNull(@Nullable Node<K, ?> node) {
    return node == null ? null : node.key;
  }

  private static <V extends @Nullable Object> @Nullable V valueOrNull(@Nullable Node<?, V> node) {
    return node == null ? null : node.value;
  }

  private static final class InverseSerializedForm<
          K extends @Nullable Object, V extends @Nullable Object>
      implements Serializable {
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
  @GwtIncompatible
  @J2ktIncompatible
    private void writeObject(ObjectOutputStream stream) throws IOException {
    stream.defaultWriteObject();
    Serialization.writeMap(this, stream);
  }

  @GwtIncompatible
  @J2ktIncompatible
    private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
    stream.defaultReadObject();
    int size = stream.readInt();
    init(16); // resist hostile attempts to allocate gratuitous heap
    Serialization.populateMap(this, stream, size);
  }

  @GwtIncompatible @J2ktIncompatible private static final long serialVersionUID = 0;
}
