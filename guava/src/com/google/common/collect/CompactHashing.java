/*
 * Copyright (C) 2019 The Guava Authors
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

import com.google.common.annotations.GwtIncompatible;
import com.google.common.base.Objects;
import com.google.common.primitives.Ints;
import java.util.Arrays;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Helper classes and static methods for implementing compact hash-based collections.
 *
 * @author Jon Noack
 */
@GwtIncompatible
final class CompactHashing {
  private CompactHashing() {}

  /** Indicates blank table entries. */
  static final byte UNSET = 0;

  /** Number of bits used to store the numbers of hash table bits (max 30). */
  private static final int HASH_TABLE_BITS_MAX_BITS = 5;

  /** Use high bits of metadata for modification count. */
  static final int MODIFICATION_COUNT_INCREMENT = (1 << HASH_TABLE_BITS_MAX_BITS);

  /** Bitmask that selects the low bits of metadata to get hashTableBits. */
  static final int HASH_TABLE_BITS_MASK = (1 << HASH_TABLE_BITS_MAX_BITS) - 1;

  /** Maximum size of a compact hash-based collection (2^30 - 1 because 0 is UNSET). */
  static final int MAX_SIZE = Ints.MAX_POWER_OF_TWO - 1;

  /** Default size of a compact hash-based collection. */
  static final int DEFAULT_SIZE = 3;

  /**
   * Minimum size of the hash table of a compact hash-based collection. Because small hash tables
   * use a byte[], any smaller size uses the same amount of memory due to object padding.
   */
  private static final int MIN_HASH_TABLE_SIZE = 4;

  private static final int BYTE_MAX_SIZE = 1 << Byte.SIZE; // 2^8 = 256
  private static final int BYTE_MASK = (1 << Byte.SIZE) - 1; // 2^8 - 1 = 255

  private static final int SHORT_MAX_SIZE = 1 << Short.SIZE; // 2^16 = 65_536
  private static final int SHORT_MASK = (1 << Short.SIZE) - 1; // 2^16 - 1 = 65_535

  /**
   * Returns the power of 2 hashtable size required to hold the expected number of items or the
   * minimum hashtable size, whichever is greater.
   */
  static int tableSize(int expectedSize) {
    // We use entries next == 0 to indicate UNSET, so actual capacity is 1 less than requested.
    return Math.max(MIN_HASH_TABLE_SIZE, Hashing.closedTableSize(expectedSize + 1, 1.0f));
  }

  /** Creates and returns a properly-sized array with the given number of buckets. */
  static Object createTable(int buckets) {
    if (buckets < 2
        || buckets > Ints.MAX_POWER_OF_TWO
        || Integer.highestOneBit(buckets) != buckets) {
      throw new IllegalArgumentException("must be power of 2 between 2^1 and 2^30: " + buckets);
    }
    if (buckets <= BYTE_MAX_SIZE) {
      return new byte[buckets];
    } else if (buckets <= SHORT_MAX_SIZE) {
      return new short[buckets];
    } else {
      return new int[buckets];
    }
  }

  static void tableClear(Object table) {
    if (table instanceof byte[]) {
      Arrays.fill((byte[]) table, (byte) 0);
    } else if (table instanceof short[]) {
      Arrays.fill((short[]) table, (short) 0);
    } else {
      Arrays.fill((int[]) table, 0);
    }
  }

  static int tableGet(Object table, int index) {
    if (table instanceof byte[]) {
      return ((byte[]) table)[index] & BYTE_MASK; // unsigned read
    } else if (table instanceof short[]) {
      return ((short[]) table)[index] & SHORT_MASK; // unsigned read
    } else {
      return ((int[]) table)[index];
    }
  }

  static void tableSet(Object table, int index, int entry) {
    if (table instanceof byte[]) {
      ((byte[]) table)[index] = (byte) entry; // unsigned write
    } else if (table instanceof short[]) {
      ((short[]) table)[index] = (short) entry; // unsigned write
    } else {
      ((int[]) table)[index] = entry;
    }
  }

  /**
   * Returns a larger power of 2 hashtable size given the current mask.
   *
   * <p>For hashtable sizes less than or equal to 32, the returned power of 2 is 4x the current
   * hashtable size to reduce expensive rehashing. Otherwise the returned power of 2 is 2x the
   * current hashtable size.
   */
  static int newCapacity(int mask) {
    return ((mask < 32) ? 4 : 2) * (mask + 1);
  }

  /** Returns the hash prefix given the current mask. */
  static int getHashPrefix(int value, int mask) {
    return value & ~mask;
  }

  /** Returns the index, or 0 if the entry is "null". */
  static int getNext(int entry, int mask) {
    return entry & mask;
  }

  /** Returns a new value combining the prefix and suffix using the given mask. */
  static int maskCombine(int prefix, int suffix, int mask) {
    return (prefix & ~mask) | (suffix & mask);
  }

  static int remove(
      @Nullable Object key,
      @Nullable Object value,
      int mask,
      Object table,
      int[] entries,
      Object[] keys,
      Object @Nullable [] values) {
    int hash = Hashing.smearedHash(key);
    int tableIndex = hash & mask;
    int next = tableGet(table, tableIndex);
    if (next == UNSET) {
      return -1;
    }
    int hashPrefix = getHashPrefix(hash, mask);
    int lastEntryIndex = -1;
    do {
      int entryIndex = next - 1;
      int entry = entries[entryIndex];
      if (getHashPrefix(entry, mask) == hashPrefix
          && Objects.equal(key, keys[entryIndex])
          && (values == null || Objects.equal(value, values[entryIndex]))) {
        int newNext = getNext(entry, mask);
        if (lastEntryIndex == -1) {
          // we need to update the root link from table[]
          tableSet(table, tableIndex, newNext);
        } else {
          // we need to update the link from the chain
          entries[lastEntryIndex] = maskCombine(entries[lastEntryIndex], newNext, mask);
        }

        return entryIndex;
      }
      lastEntryIndex = entryIndex;
      next = getNext(entry, mask);
    } while (next != UNSET);
    return -1;
  }
}
