/*
 * Copyright (C) 2008 The Guava Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.common.collect;

import static com.google.common.base.Preconditions.checkElementIndex;
import static com.google.common.base.Preconditions.checkPositionIndex;
import static com.google.common.collect.CollectPreconditions.checkEntryNotNull;
import static java.util.Objects.requireNonNull;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.VisibleForTesting;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Map.Entry;
import javax.annotation.CheckForNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * A hash-based implementation of {@link ImmutableMap}.
 *
 * @author Louis Wasserman
 */
@GwtCompatible(serializable = true, emulated = true)
@ElementTypesAreNonnullByDefault
final class RegularImmutableMap<K, V> extends ImmutableMap<K, V> {
  private static final byte ABSENT = -1;

  // Max size is halved due to indexing into double-sized alternatingKeysAndValues
  private static final int BYTE_MAX_SIZE = 1 << (Byte.SIZE - 1); // 2^7 = 128
  private static final int SHORT_MAX_SIZE = 1 << (Short.SIZE - 1); // 2^15 = 32_768

  private static final int BYTE_MASK = (1 << Byte.SIZE) - 1; // 2^8 - 1 = 255
  private static final int SHORT_MASK = (1 << Short.SIZE) - 1; // 2^16 - 1 = 65_535

  @SuppressWarnings("unchecked")
  static final ImmutableMap<Object, Object> EMPTY =
      new RegularImmutableMap<>(null, new Object[0], 0);

  /*
   * This is an implementation of ImmutableMap optimized especially for Android, which does not like
   * objects per entry.  Instead we use an open-addressed hash table.  This design is basically
   * equivalent to RegularImmutableSet, save that instead of having a hash table containing the
   * elements directly and null for empty positions, we store indices of the keys in the hash table,
   * and ABSENT for empty positions.  We then look up the keys in alternatingKeysAndValues.
   *
   * (The index actually stored is the index of the key in alternatingKeysAndValues, which is
   * double the index of the entry in entrySet.asList.)
   *
   * The basic data structure is described in https://en.wikipedia.org/wiki/Open_addressing.
   * The pointer to a key is stored in hashTable[Hashing.smear(key.hashCode()) % table.length],
   * save that if that location is already full, we try the next index, and the next, until we
   * find an empty table position.  Since the table has a power-of-two size, we use
   * & (table.length - 1) instead of % table.length, though.
   */

  @CheckForNull private final transient Object hashTable;
  @VisibleForTesting final transient @Nullable Object[] alternatingKeysAndValues;
  private final transient int size;

  @SuppressWarnings("unchecked")
  static <K, V> RegularImmutableMap<K, V> create(
      int n, @Nullable Object[] alternatingKeysAndValues) {
    if (n == 0) {
      return (RegularImmutableMap<K, V>) EMPTY;
    } else if (n == 1) {
      // requireNonNull is safe because the first `2*n` elements have been filled in.
      checkEntryNotNull(
          requireNonNull(alternatingKeysAndValues[0]), requireNonNull(alternatingKeysAndValues[1]));
      return new RegularImmutableMap<K, V>(null, alternatingKeysAndValues, 1);
    }
    checkPositionIndex(n, alternatingKeysAndValues.length >> 1);
    int tableSize = ImmutableSet.chooseTableSize(n);
    Object hashTable = createHashTable(alternatingKeysAndValues, n, tableSize, 0);
    return new RegularImmutableMap<K, V>(hashTable, alternatingKeysAndValues, n);
  }

  /**
   * Returns a hash table for the specified keys and values, and ensures that neither keys nor
   * values are null.
   */
  @CheckForNull
  static Object createHashTable(
      @Nullable Object[] alternatingKeysAndValues, int n, int tableSize, int keyOffset) {
    if (n == 1) {
      // for n=1 we don't create a hash table, but we need to do the checkEntryNotNull check!
      // requireNonNull is safe because the first `2*n` elements have been filled in.
      checkEntryNotNull(
          requireNonNull(alternatingKeysAndValues[keyOffset]),
          requireNonNull(alternatingKeysAndValues[keyOffset ^ 1]));
      return null;
    }
    int mask = tableSize - 1;
    if (tableSize <= BYTE_MAX_SIZE) {
      /*
       * Use 8 bits per entry. The value is unsigned to allow use up to a size of 2^8.
       *
       * The absent indicator of -1 signed becomes 2^8 - 1 unsigned, which reduces the actual max
       * size to 2^8 - 1. However, due to a load factor < 1 the limit is never approached.
       */
      byte[] hashTable = new byte[tableSize];
      Arrays.fill(hashTable, ABSENT);

      for (int i = 0; i < n; i++) {
        int keyIndex = 2 * i + keyOffset;
        // requireNonNull is safe because the first `2*n` elements have been filled in.
        Object key = requireNonNull(alternatingKeysAndValues[keyIndex]);
        Object value = requireNonNull(alternatingKeysAndValues[keyIndex ^ 1]);
        checkEntryNotNull(key, value);
        for (int h = Hashing.smear(key.hashCode()); ; h++) {
          h &= mask;
          int previousKeyIndex = hashTable[h] & BYTE_MASK; // unsigned read
          if (previousKeyIndex == BYTE_MASK) { // -1 signed becomes 255 unsigned
            hashTable[h] = (byte) keyIndex;
            break;
          } else if (key.equals(alternatingKeysAndValues[previousKeyIndex])) {
            throw duplicateKeyException(key, value, alternatingKeysAndValues, previousKeyIndex);
          }
        }
      }
      return hashTable;
    } else if (tableSize <= SHORT_MAX_SIZE) {
      /*
       * Use 16 bits per entry. The value is unsigned to allow use up to a size of 2^16.
       *
       * The absent indicator of -1 signed becomes 2^16 - 1 unsigned, which reduces the actual max
       * size to 2^16 - 1. However, due to a load factor < 1 the limit is never approached.
       */
      short[] hashTable = new short[tableSize];
      Arrays.fill(hashTable, ABSENT);

      for (int i = 0; i < n; i++) {
        int keyIndex = 2 * i + keyOffset;
        // requireNonNull is safe because the first `2*n` elements have been filled in.
        Object key = requireNonNull(alternatingKeysAndValues[keyIndex]);
        Object value = requireNonNull(alternatingKeysAndValues[keyIndex ^ 1]);
        checkEntryNotNull(key, value);
        for (int h = Hashing.smear(key.hashCode()); ; h++) {
          h &= mask;
          int previousKeyIndex = hashTable[h] & SHORT_MASK; // unsigned read
          if (previousKeyIndex == SHORT_MASK) { // -1 signed becomes 65_535 unsigned
            hashTable[h] = (short) keyIndex;
            break;
          } else if (key.equals(alternatingKeysAndValues[previousKeyIndex])) {
            throw duplicateKeyException(key, value, alternatingKeysAndValues, previousKeyIndex);
          }
        }
      }
      return hashTable;
    } else {
      /*
       * Use 32 bits per entry.
       */
      int[] hashTable = new int[tableSize];
      Arrays.fill(hashTable, ABSENT);

      for (int i = 0; i < n; i++) {
        int keyIndex = 2 * i + keyOffset;
        // requireNonNull is safe because the first `2*n` elements have been filled in.
        Object key = requireNonNull(alternatingKeysAndValues[keyIndex]);
        Object value = requireNonNull(alternatingKeysAndValues[keyIndex ^ 1]);
        checkEntryNotNull(key, value);
        for (int h = Hashing.smear(key.hashCode()); ; h++) {
          h &= mask;
          int previousKeyIndex = hashTable[h];
          if (previousKeyIndex == ABSENT) {
            hashTable[h] = keyIndex;
            break;
          } else if (key.equals(alternatingKeysAndValues[previousKeyIndex])) {
            throw duplicateKeyException(key, value, alternatingKeysAndValues, previousKeyIndex);
          }
        }
      }
      return hashTable;
    }
  }

  private static IllegalArgumentException duplicateKeyException(
      Object key, Object value, @Nullable Object[] alternatingKeysAndValues, int previousKeyIndex) {
    return new IllegalArgumentException(
        "Multiple entries with same key: "
            + key
            + "="
            + value
            + " and "
            + alternatingKeysAndValues[previousKeyIndex]
            + "="
            + alternatingKeysAndValues[previousKeyIndex ^ 1]);
  }

  private RegularImmutableMap(
      @CheckForNull Object hashTable, @Nullable Object[] alternatingKeysAndValues, int size) {
    this.hashTable = hashTable;
    this.alternatingKeysAndValues = alternatingKeysAndValues;
    this.size = size;
  }

  @Override
  public int size() {
    return size;
  }

  @SuppressWarnings("unchecked")
  @Override
  @CheckForNull
  public V get(@CheckForNull Object key) {
    Object result = get(hashTable, alternatingKeysAndValues, size, 0, key);
    /*
     * We can't simply cast the result of `RegularImmutableMap.get` to V because of a bug in our
     * nullness checker (resulting from https://github.com/jspecify/checker-framework/issues/8).
     */
    if (result == null) {
      return null;
    } else {
      return (V) result;
    }
  }

  @CheckForNull
  static Object get(
      @CheckForNull Object hashTableObject,
      @Nullable Object[] alternatingKeysAndValues,
      int size,
      int keyOffset,
      @CheckForNull Object key) {
    if (key == null) {
      return null;
    } else if (size == 1) {
      // requireNonNull is safe because the first 2 elements have been filled in.
      return requireNonNull(alternatingKeysAndValues[keyOffset]).equals(key)
          ? requireNonNull(alternatingKeysAndValues[keyOffset ^ 1])
          : null;
    } else if (hashTableObject == null) {
      return null;
    }
    if (hashTableObject instanceof byte[]) {
      byte[] hashTable = (byte[]) hashTableObject;
      int mask = hashTable.length - 1;
      for (int h = Hashing.smear(key.hashCode()); ; h++) {
        h &= mask;
        int keyIndex = hashTable[h] & BYTE_MASK; // unsigned read
        if (keyIndex == BYTE_MASK) { // -1 signed becomes 255 unsigned
          return null;
        } else if (key.equals(alternatingKeysAndValues[keyIndex])) {
          return alternatingKeysAndValues[keyIndex ^ 1];
        }
      }
    } else if (hashTableObject instanceof short[]) {
      short[] hashTable = (short[]) hashTableObject;
      int mask = hashTable.length - 1;
      for (int h = Hashing.smear(key.hashCode()); ; h++) {
        h &= mask;
        int keyIndex = hashTable[h] & SHORT_MASK; // unsigned read
        if (keyIndex == SHORT_MASK) { // -1 signed becomes 65_535 unsigned
          return null;
        } else if (key.equals(alternatingKeysAndValues[keyIndex])) {
          return alternatingKeysAndValues[keyIndex ^ 1];
        }
      }
    } else {
      int[] hashTable = (int[]) hashTableObject;
      int mask = hashTable.length - 1;
      for (int h = Hashing.smear(key.hashCode()); ; h++) {
        h &= mask;
        int keyIndex = hashTable[h];
        if (keyIndex == ABSENT) {
          return null;
        } else if (key.equals(alternatingKeysAndValues[keyIndex])) {
          return alternatingKeysAndValues[keyIndex ^ 1];
        }
      }
    }
  }

  @Override
  ImmutableSet<Entry<K, V>> createEntrySet() {
    return new EntrySet<>(this, alternatingKeysAndValues, 0, size);
  }

  static class EntrySet<K, V> extends ImmutableSet<Entry<K, V>> {
    private final transient ImmutableMap<K, V> map;
    private final transient @Nullable Object[] alternatingKeysAndValues;
    private final transient int keyOffset;
    private final transient int size;

    EntrySet(
        ImmutableMap<K, V> map,
        @Nullable Object[] alternatingKeysAndValues,
        int keyOffset,
        int size) {
      this.map = map;
      this.alternatingKeysAndValues = alternatingKeysAndValues;
      this.keyOffset = keyOffset;
      this.size = size;
    }

    @Override
    public UnmodifiableIterator<Entry<K, V>> iterator() {
      return asList().iterator();
    }

    @Override
    int copyIntoArray(Object[] dst, int offset) {
      return asList().copyIntoArray(dst, offset);
    }

    @Override
    ImmutableList<Entry<K, V>> createAsList() {
      return new ImmutableList<Entry<K, V>>() {
        @Override
        public Entry<K, V> get(int index) {
          checkElementIndex(index, size);
          /*
           * requireNonNull is safe because the first `2*(size+keyOffset)` elements have been filled
           * in.
           */
          @SuppressWarnings("unchecked")
          K key = (K) requireNonNull(alternatingKeysAndValues[2 * index + keyOffset]);
          @SuppressWarnings("unchecked")
          V value = (V) requireNonNull(alternatingKeysAndValues[2 * index + (keyOffset ^ 1)]);
          return new AbstractMap.SimpleImmutableEntry<K, V>(key, value);
        }

        @Override
        public int size() {
          return size;
        }

        @Override
        public boolean isPartialView() {
          return true;
        }
      };
    }

    @Override
    public boolean contains(@CheckForNull Object object) {
      if (object instanceof Entry) {
        Entry<?, ?> entry = (Entry<?, ?>) object;
        Object k = entry.getKey();
        Object v = entry.getValue();
        return v != null && v.equals(map.get(k));
      }
      return false;
    }

    @Override
    boolean isPartialView() {
      return true;
    }

    @Override
    public int size() {
      return size;
    }
  }

  @Override
  ImmutableSet<K> createKeySet() {
    @SuppressWarnings("unchecked")
    ImmutableList<K> keyList =
        (ImmutableList<K>) new KeysOrValuesAsList(alternatingKeysAndValues, 0, size);
    return new KeySet<K>(this, keyList);
  }

  static final class KeysOrValuesAsList extends ImmutableList<Object> {
    private final transient @Nullable Object[] alternatingKeysAndValues;
    private final transient int offset;
    private final transient int size;

    KeysOrValuesAsList(@Nullable Object[] alternatingKeysAndValues, int offset, int size) {
      this.alternatingKeysAndValues = alternatingKeysAndValues;
      this.offset = offset;
      this.size = size;
    }

    @Override
    public Object get(int index) {
      checkElementIndex(index, size);
      // requireNonNull is safe because the first `2*(size+offset)` elements have been filled in.
      return requireNonNull(alternatingKeysAndValues[2 * index + offset]);
    }

    @Override
    boolean isPartialView() {
      return true;
    }

    @Override
    public int size() {
      return size;
    }
  }

  static final class KeySet<K> extends ImmutableSet<K> {
    private final transient ImmutableMap<K, ?> map;
    private final transient ImmutableList<K> list;

    KeySet(ImmutableMap<K, ?> map, ImmutableList<K> list) {
      this.map = map;
      this.list = list;
    }

    @Override
    public UnmodifiableIterator<K> iterator() {
      return asList().iterator();
    }

    @Override
    int copyIntoArray(Object[] dst, int offset) {
      return asList().copyIntoArray(dst, offset);
    }

    @Override
    public ImmutableList<K> asList() {
      return list;
    }

    @Override
    public boolean contains(@CheckForNull Object object) {
      return map.get(object) != null;
    }

    @Override
    boolean isPartialView() {
      return true;
    }

    @Override
    public int size() {
      return map.size();
    }
  }

  @SuppressWarnings("unchecked")
  @Override
  ImmutableCollection<V> createValues() {
    return (ImmutableList<V>) new KeysOrValuesAsList(alternatingKeysAndValues, 1, size);
  }

  @Override
  boolean isPartialView() {
    return false;
  }

  // This class is never actually serialized directly, but we have to make the
  // warning go away (and suppressing would suppress for all nested classes too)
  private static final long serialVersionUID = 0;
}
