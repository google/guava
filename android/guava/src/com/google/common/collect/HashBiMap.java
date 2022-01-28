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
import static com.google.common.collect.NullnessCasts.uncheckedCastNullableTToT;
import static com.google.common.collect.NullnessCasts.unsafeNull;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.base.Objects;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.errorprone.annotations.concurrent.LazyInit;
import com.google.j2objc.annotations.RetainedWith;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import javax.annotation.CheckForNull;
import org.checkerframework.checker.nullness.qual.Nullable;

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
@ElementTypesAreNonnullByDefault
public final class HashBiMap<K extends @Nullable Object, V extends @Nullable Object>
    extends AbstractMap<K, V> implements BiMap<K, V>, Serializable {

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

  private static final int ABSENT = -1;
  private static final int ENDPOINT = -2;

  /** Maps an "entry" to the key of that entry. */
  transient @Nullable K[] keys;
  /** Maps an "entry" to the value of that entry. */
  transient @Nullable V[] values;

  transient int size;
  transient int modCount;
  /** Maps a bucket to the "entry" of its first element. */
  private transient int[] hashTableKToV;
  /** Maps a bucket to the "entry" of its first element. */
  private transient int[] hashTableVToK;
  /** Maps an "entry" to the "entry" that follows it in its bucket. */
  private transient int[] nextInBucketKToV;
  /** Maps an "entry" to the "entry" that follows it in its bucket. */
  private transient int[] nextInBucketVToK;
  /** The "entry" of the first element in insertion order. */
  private transient int firstInInsertionOrder;
  /** The "entry" of the last element in insertion order. */
  private transient int lastInInsertionOrder;
  /** Maps an "entry" to the "entry" that precedes it in insertion order. */
  private transient int[] prevInInsertionOrder;
  /** Maps an "entry" to the "entry" that follows it in insertion order. */
  private transient int[] nextInInsertionOrder;

  private HashBiMap(int expectedSize) {
    init(expectedSize);
  }

  @SuppressWarnings("unchecked")
  void init(int expectedSize) {
    CollectPreconditions.checkNonnegative(expectedSize, "expectedSize");
    int tableSize = Hashing.closedTableSize(expectedSize, 1.0);
    size = 0;

    keys = (K[]) new Object[expectedSize];
    values = (V[]) new Object[expectedSize];

    hashTableKToV = createFilledWithAbsent(tableSize);
    hashTableVToK = createFilledWithAbsent(tableSize);
    nextInBucketKToV = createFilledWithAbsent(expectedSize);
    nextInBucketVToK = createFilledWithAbsent(expectedSize);

    firstInInsertionOrder = ENDPOINT;
    lastInInsertionOrder = ENDPOINT;

    prevInInsertionOrder = createFilledWithAbsent(expectedSize);
    nextInInsertionOrder = createFilledWithAbsent(expectedSize);
  }

  /** Returns an int array of the specified size, filled with ABSENT. */
  private static int[] createFilledWithAbsent(int size) {
    int[] array = new int[size];
    Arrays.fill(array, ABSENT);
    return array;
  }

  /** Equivalent to {@code Arrays.copyOf(array, newSize)}, save that the new elements are ABSENT. */
  private static int[] expandAndFillWithAbsent(int[] array, int newSize) {
    int oldSize = array.length;
    int[] result = Arrays.copyOf(array, newSize);
    Arrays.fill(result, oldSize, newSize, ABSENT);
    return result;
  }

  @Override
  public int size() {
    return size;
  }

  /**
   * Ensures that all of the internal structures in the HashBiMap are ready for this many elements.
   */
  private void ensureCapacity(int minCapacity) {
    if (nextInBucketKToV.length < minCapacity) {
      int oldCapacity = nextInBucketKToV.length;
      int newCapacity = ImmutableCollection.Builder.expandedCapacity(oldCapacity, minCapacity);

      keys = Arrays.copyOf(keys, newCapacity);
      values = Arrays.copyOf(values, newCapacity);
      nextInBucketKToV = expandAndFillWithAbsent(nextInBucketKToV, newCapacity);
      nextInBucketVToK = expandAndFillWithAbsent(nextInBucketVToK, newCapacity);
      prevInInsertionOrder = expandAndFillWithAbsent(prevInInsertionOrder, newCapacity);
      nextInInsertionOrder = expandAndFillWithAbsent(nextInInsertionOrder, newCapacity);
    }

    if (hashTableKToV.length < minCapacity) {
      int newTableSize = Hashing.closedTableSize(minCapacity, 1.0);
      hashTableKToV = createFilledWithAbsent(newTableSize);
      hashTableVToK = createFilledWithAbsent(newTableSize);

      for (int entryToRehash = 0; entryToRehash < size; entryToRehash++) {
        int keyHash = Hashing.smearedHash(keys[entryToRehash]);
        int keyBucket = bucket(keyHash);
        nextInBucketKToV[entryToRehash] = hashTableKToV[keyBucket];
        hashTableKToV[keyBucket] = entryToRehash;

        int valueHash = Hashing.smearedHash(values[entryToRehash]);
        int valueBucket = bucket(valueHash);
        nextInBucketVToK[entryToRehash] = hashTableVToK[valueBucket];
        hashTableVToK[valueBucket] = entryToRehash;
      }
    }
  }

  /**
   * Returns the bucket (in either the K-to-V or V-to-K tables) where elements with the specified
   * hash could be found, if present, or could be inserted.
   */
  private int bucket(int hash) {
    return hash & (hashTableKToV.length - 1);
  }

  /** Given a key, returns the index of the entry in the tables, or ABSENT if not found. */
  int findEntryByKey(@CheckForNull Object key) {
    return findEntryByKey(key, Hashing.smearedHash(key));
  }

  /**
   * Given a key and its hash, returns the index of the entry in the tables, or ABSENT if not found.
   */
  int findEntryByKey(@CheckForNull Object key, int keyHash) {
    return findEntry(key, keyHash, hashTableKToV, nextInBucketKToV, keys);
  }

  /** Given a value, returns the index of the entry in the tables, or ABSENT if not found. */
  int findEntryByValue(@CheckForNull Object value) {
    return findEntryByValue(value, Hashing.smearedHash(value));
  }

  /**
   * Given a value and its hash, returns the index of the entry in the tables, or ABSENT if not
   * found.
   */
  int findEntryByValue(@CheckForNull Object value, int valueHash) {
    return findEntry(value, valueHash, hashTableVToK, nextInBucketVToK, values);
  }

  int findEntry(
      @CheckForNull Object o,
      int oHash,
      int[] hashTable,
      int[] nextInBucket,
      @Nullable Object[] array) {
    for (int entry = hashTable[bucket(oHash)]; entry != ABSENT; entry = nextInBucket[entry]) {
      if (Objects.equal(array[entry], o)) {
        return entry;
      }
    }
    return ABSENT;
  }

  @Override
  public boolean containsKey(@CheckForNull Object key) {
    return findEntryByKey(key) != ABSENT;
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
  public boolean containsValue(@CheckForNull Object value) {
    return findEntryByValue(value) != ABSENT;
  }

  @Override
  @CheckForNull
  public V get(@CheckForNull Object key) {
    int entry = findEntryByKey(key);
    return (entry == ABSENT) ? null : values[entry];
  }

  @CheckForNull
  K getInverse(@CheckForNull Object value) {
    int entry = findEntryByValue(value);
    return (entry == ABSENT) ? null : keys[entry];
  }

  @Override
  @CanIgnoreReturnValue
  @CheckForNull
  public V put(@ParametricNullness K key, @ParametricNullness V value) {
    return put(key, value, false);
  }

  @CheckForNull
  V put(@ParametricNullness K key, @ParametricNullness V value, boolean force) {
    int keyHash = Hashing.smearedHash(key);
    int entryForKey = findEntryByKey(key, keyHash);
    if (entryForKey != ABSENT) {
      V oldValue = values[entryForKey];
      if (Objects.equal(oldValue, value)) {
        return value;
      } else {
        replaceValueInEntry(entryForKey, value, force);
        return oldValue;
      }
    }

    int valueHash = Hashing.smearedHash(value);
    int valueEntry = findEntryByValue(value, valueHash);
    if (force) {
      if (valueEntry != ABSENT) {
        removeEntryValueHashKnown(valueEntry, valueHash);
      }
    } else {
      checkArgument(valueEntry == ABSENT, "Value already present: %s", value);
    }

    ensureCapacity(size + 1);
    keys[size] = key;
    values[size] = value;

    insertIntoTableKToV(size, keyHash);
    insertIntoTableVToK(size, valueHash);

    setSucceeds(lastInInsertionOrder, size);
    setSucceeds(size, ENDPOINT);
    size++;
    modCount++;
    return null;
  }

  @Override
  @CanIgnoreReturnValue
  @CheckForNull
  public V forcePut(@ParametricNullness K key, @ParametricNullness V value) {
    return put(key, value, true);
  }

  @CanIgnoreReturnValue
  @CheckForNull
  K putInverse(@ParametricNullness V value, @ParametricNullness K key, boolean force) {
    int valueHash = Hashing.smearedHash(value);
    int entryForValue = findEntryByValue(value, valueHash);
    if (entryForValue != ABSENT) {
      K oldKey = keys[entryForValue];
      if (Objects.equal(oldKey, key)) {
        return key;
      } else {
        replaceKeyInEntry(entryForValue, key, force);
        return oldKey;
      }
    }

    int predecessor = lastInInsertionOrder;
    int keyHash = Hashing.smearedHash(key);
    int keyEntry = findEntryByKey(key, keyHash);
    if (force) {
      if (keyEntry != ABSENT) {
        predecessor = prevInInsertionOrder[keyEntry];
        removeEntryKeyHashKnown(keyEntry, keyHash);
      }
    } else {
      checkArgument(keyEntry == ABSENT, "Key already present: %s", key);
    }

    // insertion point for new entry is after predecessor
    // note predecessor must still be a valid entry: either we deleted an entry that was *not*
    // predecessor, or we didn't delete anything

    ensureCapacity(size + 1);
    keys[size] = key;
    values[size] = value;

    insertIntoTableKToV(size, keyHash);
    insertIntoTableVToK(size, valueHash);

    int successor =
        (predecessor == ENDPOINT) ? firstInInsertionOrder : nextInInsertionOrder[predecessor];
    setSucceeds(predecessor, size);
    setSucceeds(size, successor);
    size++;
    modCount++;
    return null;
  }

  /**
   * Updates the pointers of the insertion order linked list so that {@code next} follows {@code
   * prev}. {@code ENDPOINT} represents either the first or last entry in the entire map (as
   * appropriate).
   */
  private void setSucceeds(int prev, int next) {
    if (prev == ENDPOINT) {
      firstInInsertionOrder = next;
    } else {
      nextInInsertionOrder[prev] = next;
    }
    if (next == ENDPOINT) {
      lastInInsertionOrder = prev;
    } else {
      prevInInsertionOrder[next] = prev;
    }
  }

  /**
   * Updates the K-to-V hash table to include the entry at the specified index, which is assumed to
   * have not yet been added.
   */
  private void insertIntoTableKToV(int entry, int keyHash) {
    checkArgument(entry != ABSENT);
    int keyBucket = bucket(keyHash);
    nextInBucketKToV[entry] = hashTableKToV[keyBucket];
    hashTableKToV[keyBucket] = entry;
  }

  /**
   * Updates the V-to-K hash table to include the entry at the specified index, which is assumed to
   * have not yet been added.
   */
  private void insertIntoTableVToK(int entry, int valueHash) {
    checkArgument(entry != ABSENT);
    int valueBucket = bucket(valueHash);
    nextInBucketVToK[entry] = hashTableVToK[valueBucket];
    hashTableVToK[valueBucket] = entry;
  }

  /**
   * Updates the K-to-V hash table to remove the entry at the specified index, which is assumed to
   * be present. Does not update any other data structures.
   */
  private void deleteFromTableKToV(int entry, int keyHash) {
    checkArgument(entry != ABSENT);
    int keyBucket = bucket(keyHash);

    if (hashTableKToV[keyBucket] == entry) {
      hashTableKToV[keyBucket] = nextInBucketKToV[entry];
      nextInBucketKToV[entry] = ABSENT;
      return;
    }

    int prevInBucket = hashTableKToV[keyBucket];
    for (int entryInBucket = nextInBucketKToV[prevInBucket];
        entryInBucket != ABSENT;
        entryInBucket = nextInBucketKToV[entryInBucket]) {
      if (entryInBucket == entry) {
        nextInBucketKToV[prevInBucket] = nextInBucketKToV[entry];
        nextInBucketKToV[entry] = ABSENT;
        return;
      }
      prevInBucket = entryInBucket;
    }
    throw new AssertionError("Expected to find entry with key " + keys[entry]);
  }

  /**
   * Updates the V-to-K hash table to remove the entry at the specified index, which is assumed to
   * be present. Does not update any other data structures.
   */
  private void deleteFromTableVToK(int entry, int valueHash) {
    checkArgument(entry != ABSENT);
    int valueBucket = bucket(valueHash);

    if (hashTableVToK[valueBucket] == entry) {
      hashTableVToK[valueBucket] = nextInBucketVToK[entry];
      nextInBucketVToK[entry] = ABSENT;
      return;
    }

    int prevInBucket = hashTableVToK[valueBucket];
    for (int entryInBucket = nextInBucketVToK[prevInBucket];
        entryInBucket != ABSENT;
        entryInBucket = nextInBucketVToK[entryInBucket]) {
      if (entryInBucket == entry) {
        nextInBucketVToK[prevInBucket] = nextInBucketVToK[entry];
        nextInBucketVToK[entry] = ABSENT;
        return;
      }
      prevInBucket = entryInBucket;
    }
    throw new AssertionError("Expected to find entry with value " + values[entry]);
  }

  /**
   * Updates the specified entry to point to the new value: removes the old value from the V-to-K
   * mapping and puts the new one in. The entry does not move in the insertion order of the bimap.
   */
  private void replaceValueInEntry(int entry, @ParametricNullness V newValue, boolean force) {
    checkArgument(entry != ABSENT);
    int newValueHash = Hashing.smearedHash(newValue);
    int newValueIndex = findEntryByValue(newValue, newValueHash);
    if (newValueIndex != ABSENT) {
      if (force) {
        removeEntryValueHashKnown(newValueIndex, newValueHash);
        if (entry == size) { // this entry got moved to newValueIndex
          entry = newValueIndex;
        }
      } else {
        throw new IllegalArgumentException("Value already present in map: " + newValue);
      }
    }
    // we do *not* update insertion order, and it isn't a structural modification!
    deleteFromTableVToK(entry, Hashing.smearedHash(values[entry]));
    values[entry] = newValue;
    insertIntoTableVToK(entry, newValueHash);
  }

  /**
   * Updates the specified entry to point to the new value: removes the old value from the V-to-K
   * mapping and puts the new one in. The entry is moved to the end of the insertion order, or to
   * the position of the new key if it was previously present.
   */
  private void replaceKeyInEntry(int entry, @ParametricNullness K newKey, boolean force) {
    checkArgument(entry != ABSENT);
    int newKeyHash = Hashing.smearedHash(newKey);
    int newKeyIndex = findEntryByKey(newKey, newKeyHash);

    int newPredecessor = lastInInsertionOrder;
    int newSuccessor = ENDPOINT;
    if (newKeyIndex != ABSENT) {
      if (force) {
        newPredecessor = prevInInsertionOrder[newKeyIndex];
        newSuccessor = nextInInsertionOrder[newKeyIndex];
        removeEntryKeyHashKnown(newKeyIndex, newKeyHash);
        if (entry == size) { // this entry got moved to newKeyIndex
          entry = newKeyIndex;
        }
      } else {
        throw new IllegalArgumentException("Key already present in map: " + newKey);
      }
    }
    if (newPredecessor == entry) {
      newPredecessor = prevInInsertionOrder[entry];
    } else if (newPredecessor == size) {
      newPredecessor = newKeyIndex;
    }

    if (newSuccessor == entry) {
      newSuccessor = nextInInsertionOrder[entry];
    } else if (newSuccessor == size) {
      newSuccessor = newKeyIndex;
    }

    int oldPredecessor = prevInInsertionOrder[entry];
    int oldSuccessor = nextInInsertionOrder[entry];
    setSucceeds(oldPredecessor, oldSuccessor); // remove from insertion order linked list

    deleteFromTableKToV(entry, Hashing.smearedHash(keys[entry]));
    keys[entry] = newKey;
    insertIntoTableKToV(entry, Hashing.smearedHash(newKey));

    // insert into insertion order linked list, usually at the end
    setSucceeds(newPredecessor, entry);
    setSucceeds(entry, newSuccessor);
  }

  @Override
  @CanIgnoreReturnValue
  @CheckForNull
  public V remove(@CheckForNull Object key) {
    int keyHash = Hashing.smearedHash(key);
    int entry = findEntryByKey(key, keyHash);
    if (entry == ABSENT) {
      return null;
    } else {
      V value = values[entry];
      removeEntryKeyHashKnown(entry, keyHash);
      return value;
    }
  }

  @CheckForNull
  K removeInverse(@CheckForNull Object value) {
    int valueHash = Hashing.smearedHash(value);
    int entry = findEntryByValue(value, valueHash);
    if (entry == ABSENT) {
      return null;
    } else {
      K key = keys[entry];
      removeEntryValueHashKnown(entry, valueHash);
      return key;
    }
  }

  /** Removes the entry at the specified index with no additional data. */
  void removeEntry(int entry) {
    removeEntryKeyHashKnown(entry, Hashing.smearedHash(keys[entry]));
  }

  /** Removes the entry at the specified index, given the hash of its key and value. */
  private void removeEntry(int entry, int keyHash, int valueHash) {
    checkArgument(entry != ABSENT);
    deleteFromTableKToV(entry, keyHash);
    deleteFromTableVToK(entry, valueHash);

    int oldPredecessor = prevInInsertionOrder[entry];
    int oldSuccessor = nextInInsertionOrder[entry];
    setSucceeds(oldPredecessor, oldSuccessor);

    moveEntryToIndex(size - 1, entry);
    keys[size - 1] = null;
    values[size - 1] = null;
    size--;
    modCount++;
  }

  /** Removes the entry at the specified index, given the hash of its key. */
  void removeEntryKeyHashKnown(int entry, int keyHash) {
    removeEntry(entry, keyHash, Hashing.smearedHash(values[entry]));
  }

  /** Removes the entry at the specified index, given the hash of its value. */
  void removeEntryValueHashKnown(int entry, int valueHash) {
    removeEntry(entry, Hashing.smearedHash(keys[entry]), valueHash);
  }

  /**
   * Moves the entry previously positioned at {@code src} to {@code dest}. Assumes the entry
   * previously at {@code src} has already been removed from the data structures.
   */
  private void moveEntryToIndex(int src, int dest) {
    if (src == dest) {
      return;
    }
    int predecessor = prevInInsertionOrder[src];
    int successor = nextInInsertionOrder[src];
    setSucceeds(predecessor, dest);
    setSucceeds(dest, successor);

    K key = keys[src];
    V value = values[src];

    keys[dest] = key;
    values[dest] = value;

    // update pointers in hashTableKToV
    int keyHash = Hashing.smearedHash(key);
    int keyBucket = bucket(keyHash);
    if (hashTableKToV[keyBucket] == src) {
      hashTableKToV[keyBucket] = dest;
    } else {
      int prevInBucket = hashTableKToV[keyBucket];
      for (int entryInBucket = nextInBucketKToV[prevInBucket];
          /* should never reach end */ ;
          entryInBucket = nextInBucketKToV[entryInBucket]) {
        if (entryInBucket == src) {
          nextInBucketKToV[prevInBucket] = dest;
          break;
        }
        prevInBucket = entryInBucket;
      }
    }
    nextInBucketKToV[dest] = nextInBucketKToV[src];
    nextInBucketKToV[src] = ABSENT;

    // update pointers in hashTableVToK
    int valueHash = Hashing.smearedHash(value);
    int valueBucket = bucket(valueHash);
    if (hashTableVToK[valueBucket] == src) {
      hashTableVToK[valueBucket] = dest;
    } else {
      int prevInBucket = hashTableVToK[valueBucket];
      for (int entryInBucket = nextInBucketVToK[prevInBucket];
          /* should never reach end*/ ;
          entryInBucket = nextInBucketVToK[entryInBucket]) {
        if (entryInBucket == src) {
          nextInBucketVToK[prevInBucket] = dest;
          break;
        }
        prevInBucket = entryInBucket;
      }
    }
    nextInBucketVToK[dest] = nextInBucketVToK[src];
    nextInBucketVToK[src] = ABSENT;
  }

  @Override
  public void clear() {
    Arrays.fill(keys, 0, size, null);
    Arrays.fill(values, 0, size, null);
    Arrays.fill(hashTableKToV, ABSENT);
    Arrays.fill(hashTableVToK, ABSENT);
    Arrays.fill(nextInBucketKToV, 0, size, ABSENT);
    Arrays.fill(nextInBucketVToK, 0, size, ABSENT);
    Arrays.fill(prevInInsertionOrder, 0, size, ABSENT);
    Arrays.fill(nextInInsertionOrder, 0, size, ABSENT);
    size = 0;
    firstInInsertionOrder = ENDPOINT;
    lastInInsertionOrder = ENDPOINT;
    modCount++;
  }

  /** Shared supertype of keySet, values, entrySet, and inverse.entrySet. */
  abstract static class View<
          K extends @Nullable Object, V extends @Nullable Object, T extends @Nullable Object>
      extends AbstractSet<T> {
    final HashBiMap<K, V> biMap;

    View(HashBiMap<K, V> biMap) {
      this.biMap = biMap;
    }

    @ParametricNullness
    abstract T forEntry(int entry);

    @Override
    public Iterator<T> iterator() {
      return new Iterator<T>() {
        private int index = biMap.firstInInsertionOrder;
        private int indexToRemove = ABSENT;
        private int expectedModCount = biMap.modCount;

        // Calls to setValue on inverse entries can move already-visited entries to the end.
        // Make sure we don't visit those.
        private int remaining = biMap.size;

        private void checkForComodification() {
          if (biMap.modCount != expectedModCount) {
            throw new ConcurrentModificationException();
          }
        }

        @Override
        public boolean hasNext() {
          checkForComodification();
          return index != ENDPOINT && remaining > 0;
        }

        @Override
        @ParametricNullness
        public T next() {
          if (!hasNext()) {
            throw new NoSuchElementException();
          }
          T result = forEntry(index);
          indexToRemove = index;
          index = biMap.nextInInsertionOrder[index];
          remaining--;
          return result;
        }

        @Override
        public void remove() {
          checkForComodification();
          CollectPreconditions.checkRemove(indexToRemove != ABSENT);
          biMap.removeEntry(indexToRemove);
          if (index == biMap.size) {
            index = indexToRemove;
          }
          indexToRemove = ABSENT;
          expectedModCount = biMap.modCount;
        }
      };
    }

    @Override
    public int size() {
      return biMap.size;
    }

    @Override
    public void clear() {
      biMap.clear();
    }
  }

  private transient Set<K> keySet;

  @Override
  public Set<K> keySet() {
    Set<K> result = keySet;
    return (result == null) ? keySet = new KeySet() : result;
  }

  final class KeySet extends View<K, V, K> {
    KeySet() {
      super(HashBiMap.this);
    }

    @Override
    @ParametricNullness
    K forEntry(int entry) {
      // The cast is safe because we call forEntry only for indexes that contain entries.
      return uncheckedCastNullableTToT(keys[entry]);
    }

    @Override
    public boolean contains(@CheckForNull Object o) {
      return HashBiMap.this.containsKey(o);
    }

    @Override
    public boolean remove(@CheckForNull Object o) {
      int oHash = Hashing.smearedHash(o);
      int entry = findEntryByKey(o, oHash);
      if (entry != ABSENT) {
        removeEntryKeyHashKnown(entry, oHash);
        return true;
      } else {
        return false;
      }
    }
  }

  private transient Set<V> valueSet;

  @Override
  public Set<V> values() {
    Set<V> result = valueSet;
    return (result == null) ? valueSet = new ValueSet() : result;
  }

  final class ValueSet extends View<K, V, V> {
    ValueSet() {
      super(HashBiMap.this);
    }

    @Override
    @ParametricNullness
    V forEntry(int entry) {
      // The cast is safe because we call forEntry only for indexes that contain entries.
      return uncheckedCastNullableTToT(values[entry]);
    }

    @Override
    public boolean contains(@CheckForNull Object o) {
      return HashBiMap.this.containsValue(o);
    }

    @Override
    public boolean remove(@CheckForNull Object o) {
      int oHash = Hashing.smearedHash(o);
      int entry = findEntryByValue(o, oHash);
      if (entry != ABSENT) {
        removeEntryValueHashKnown(entry, oHash);
        return true;
      } else {
        return false;
      }
    }
  }

  private transient Set<Entry<K, V>> entrySet;

  @Override
  public Set<Entry<K, V>> entrySet() {
    Set<Entry<K, V>> result = entrySet;
    return (result == null) ? entrySet = new EntrySet() : result;
  }

  final class EntrySet extends View<K, V, Entry<K, V>> {
    EntrySet() {
      super(HashBiMap.this);
    }

    @Override
    public boolean contains(@CheckForNull Object o) {
      if (o instanceof Entry) {
        Entry<?, ?> e = (Entry<?, ?>) o;
        Object k = e.getKey();
        Object v = e.getValue();
        int eIndex = findEntryByKey(k);
        return eIndex != ABSENT && Objects.equal(v, values[eIndex]);
      }
      return false;
    }

    @Override
    @CanIgnoreReturnValue
    public boolean remove(@CheckForNull Object o) {
      if (o instanceof Entry) {
        Entry<?, ?> e = (Entry<?, ?>) o;
        Object k = e.getKey();
        Object v = e.getValue();
        int kHash = Hashing.smearedHash(k);
        int eIndex = findEntryByKey(k, kHash);
        if (eIndex != ABSENT && Objects.equal(v, values[eIndex])) {
          removeEntryKeyHashKnown(eIndex, kHash);
          return true;
        }
      }
      return false;
    }

    @Override
    Entry<K, V> forEntry(int entry) {
      return new EntryForKey(entry);
    }
  }

  /**
   * An {@code Entry} implementation that attempts to follow its key around the map -- that is, if
   * the key is moved, deleted, or reinserted, it will account for that -- while not doing any extra
   * work if the key has not moved. One quirk: The {@link #getValue()} method can return {@code
   * null} even for a map which supposedly does not contain null elements, if the key is not present
   * when {@code getValue()} is called.
   */
  final class EntryForKey extends AbstractMapEntry<K, V> {
    @ParametricNullness final K key;
    int index;

    EntryForKey(int index) {
      // The cast is safe because we call forEntry only for indexes that contain entries.
      this.key = uncheckedCastNullableTToT(keys[index]);
      this.index = index;
    }

    void updateIndex() {
      if (index == ABSENT || index > size || !Objects.equal(keys[index], key)) {
        index = findEntryByKey(key);
      }
    }

    @Override
    @ParametricNullness
    public K getKey() {
      return key;
    }

    @Override
    @ParametricNullness
    public V getValue() {
      updateIndex();
      /*
       * If the entry has been removed from the map, we return null, even though that might not be a
       * valid value. That's the best we can do, short of holding a reference to the most recently
       * seen value. And while we *could* do that, we aren't required to: Map.Entry explicitly says
       * that behavior is undefined when the backing map is modified through another API. (It even
       * permits us to throw IllegalStateException. Maybe we should have done that, but we probably
       * shouldn't change now for fear of breaking people.)
       *
       * If the entry is still in the map, then updateIndex ensured that `index` points to the right
       * element. Because that element is present, uncheckedCastNullableTToT is safe.
       */
      return (index == ABSENT) ? unsafeNull() : uncheckedCastNullableTToT(values[index]);
    }

    @Override
    @ParametricNullness
    public V setValue(@ParametricNullness V value) {
      updateIndex();
      if (index == ABSENT) {
        HashBiMap.this.put(key, value);
        return unsafeNull(); // See the discussion in getValue().
      }
      /*
       * The cast is safe because updateIndex found the entry for this key. (If it hadn't, then we
       * would have returned above.) Thus, we know that it and its corresponding value are in
       * position `index`.
       */
      V oldValue = uncheckedCastNullableTToT(values[index]);
      if (Objects.equal(oldValue, value)) {
        return value;
      }
      replaceValueInEntry(index, value, false);
      return oldValue;
    }
  }

  @LazyInit @RetainedWith @CheckForNull private transient BiMap<V, K> inverse;

  @Override
  public BiMap<V, K> inverse() {
    BiMap<V, K> result = inverse;
    return (result == null) ? inverse = new Inverse<K, V>(this) : result;
  }

  static class Inverse<K extends @Nullable Object, V extends @Nullable Object>
      extends AbstractMap<V, K> implements BiMap<V, K>, Serializable {
    private final HashBiMap<K, V> forward;

    Inverse(HashBiMap<K, V> forward) {
      this.forward = forward;
    }

    @Override
    public int size() {
      return forward.size;
    }

    @Override
    public boolean containsKey(@CheckForNull Object key) {
      return forward.containsValue(key);
    }

    @Override
    @CheckForNull
    public K get(@CheckForNull Object key) {
      return forward.getInverse(key);
    }

    @Override
    public boolean containsValue(@CheckForNull Object value) {
      return forward.containsKey(value);
    }

    @Override
    @CanIgnoreReturnValue
    @CheckForNull
    public K put(@ParametricNullness V value, @ParametricNullness K key) {
      return forward.putInverse(value, key, false);
    }

    @Override
    @CanIgnoreReturnValue
    @CheckForNull
    public K forcePut(@ParametricNullness V value, @ParametricNullness K key) {
      return forward.putInverse(value, key, true);
    }

    @Override
    public BiMap<K, V> inverse() {
      return forward;
    }

    @Override
    @CanIgnoreReturnValue
    @CheckForNull
    public K remove(@CheckForNull Object value) {
      return forward.removeInverse(value);
    }

    @Override
    public void clear() {
      forward.clear();
    }

    @Override
    public Set<V> keySet() {
      return forward.values();
    }

    @Override
    public Set<K> values() {
      return forward.keySet();
    }

    private transient Set<Entry<V, K>> inverseEntrySet;

    @Override
    public Set<Entry<V, K>> entrySet() {
      Set<Entry<V, K>> result = inverseEntrySet;
      return (result == null) ? inverseEntrySet = new InverseEntrySet<K, V>(forward) : result;
    }

    @GwtIncompatible("serialization")
    private void readObject(ObjectInputStream in) throws ClassNotFoundException, IOException {
      in.defaultReadObject();
      this.forward.inverse = this;
    }
  }

  static class InverseEntrySet<K extends @Nullable Object, V extends @Nullable Object>
      extends View<K, V, Entry<V, K>> {
    InverseEntrySet(HashBiMap<K, V> biMap) {
      super(biMap);
    }

    @Override
    public boolean contains(@CheckForNull Object o) {
      if (o instanceof Entry) {
        Entry<?, ?> e = (Entry<?, ?>) o;
        Object v = e.getKey();
        Object k = e.getValue();
        int eIndex = biMap.findEntryByValue(v);
        return eIndex != ABSENT && Objects.equal(biMap.keys[eIndex], k);
      }
      return false;
    }

    @Override
    public boolean remove(@CheckForNull Object o) {
      if (o instanceof Entry) {
        Entry<?, ?> e = (Entry<?, ?>) o;
        Object v = e.getKey();
        Object k = e.getValue();
        int vHash = Hashing.smearedHash(v);
        int eIndex = biMap.findEntryByValue(v, vHash);
        if (eIndex != ABSENT && Objects.equal(biMap.keys[eIndex], k)) {
          biMap.removeEntryValueHashKnown(eIndex, vHash);
          return true;
        }
      }
      return false;
    }

    @Override
    Entry<V, K> forEntry(int entry) {
      return new EntryForValue<K, V>(biMap, entry);
    }
  }

  /**
   * An {@code Entry} implementation that attempts to follow its value around the map -- that is, if
   * the value is moved, deleted, or reinserted, it will account for that -- while not doing any
   * extra work if the value has not moved.
   */
  static final class EntryForValue<K extends @Nullable Object, V extends @Nullable Object>
      extends AbstractMapEntry<V, K> {
    final HashBiMap<K, V> biMap;
    @ParametricNullness final V value;
    int index;

    EntryForValue(HashBiMap<K, V> biMap, int index) {
      this.biMap = biMap;
      // The cast is safe because we call forEntry only for indexes that contain entries.
      this.value = uncheckedCastNullableTToT(biMap.values[index]);
      this.index = index;
    }

    private void updateIndex() {
      if (index == ABSENT || index > biMap.size || !Objects.equal(value, biMap.values[index])) {
        index = biMap.findEntryByValue(value);
      }
    }

    @Override
    @ParametricNullness
    public V getKey() {
      return value;
    }

    @Override
    @ParametricNullness
    public K getValue() {
      updateIndex();
      // For discussion of unsafeNull() and uncheckedCastNullableTToT(), see EntryForKey.getValue().
      return (index == ABSENT) ? unsafeNull() : uncheckedCastNullableTToT(biMap.keys[index]);
    }

    @Override
    @ParametricNullness
    public K setValue(@ParametricNullness K key) {
      updateIndex();
      if (index == ABSENT) {
        biMap.putInverse(value, key, false);
        return unsafeNull(); // see EntryForKey.setValue()
      }
      K oldKey = uncheckedCastNullableTToT(biMap.keys[index]); // see EntryForKey.setValue()
      if (Objects.equal(oldKey, key)) {
        return key;
      }
      biMap.replaceKeyInEntry(index, key, false);
      return oldKey;
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
}
