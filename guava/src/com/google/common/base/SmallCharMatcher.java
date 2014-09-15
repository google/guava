/*
 * Copyright (C) 2012 The Guava Authors
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

package com.google.common.base;

import com.google.common.annotations.GwtIncompatible;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.CharMatcher.NamedFastMatcher;

import java.util.BitSet;

/**
 * An immutable version of CharMatcher for smallish sets of characters that uses a hash table
 * with linear probing to check for matches.
 *
 * @author Christopher Swenson
 */
@GwtIncompatible("no precomputation is done in GWT")
final class SmallCharMatcher extends NamedFastMatcher {
  static final int MAX_SIZE = 1023;
  private final char[] table;
  private final boolean containsZero;
  private final long filter;

  private SmallCharMatcher(char[] table, long filter, boolean containsZero,
      String description) {
    super(description);
    this.table = table;
    this.filter = filter;
    this.containsZero = containsZero;
  }
  
  private static final int C1 = 0xcc9e2d51;
  private static final int C2 = 0x1b873593;

  /*
   * This method was rewritten in Java from an intermediate step of the Murmur hash function in
   * http://code.google.com/p/smhasher/source/browse/trunk/MurmurHash3.cpp, which contained the
   * following header:
   *
   * MurmurHash3 was written by Austin Appleby, and is placed in the public domain. The author
   * hereby disclaims copyright to this source code.
   */
  static int smear(int hashCode) {
    return C2 * Integer.rotateLeft(hashCode * C1, 15);
  }

  private boolean checkFilter(int c) {
    return 1 == (1 & (filter >> c));
  }

  // This is all essentially copied from ImmutableSet, but we have to duplicate because
  // of dependencies.

  // Represents how tightly we can pack things, as a maximum.
  private static final double DESIRED_LOAD_FACTOR = 0.5;

 /**
  * Returns an array size suitable for the backing array of a hash table that
  * uses open addressing with linear probing in its implementation.  The
  * returned size is the smallest power of two that can hold setSize elements
  * with the desired load factor.
  */
  @VisibleForTesting static int chooseTableSize(int setSize) {
    if (setSize == 1) {
      return 2;
    }
    // Correct the size for open addressing to match desired load factor.
    // Round up to the next highest power of 2.
    int tableSize = Integer.highestOneBit(setSize - 1) << 1;
    while (tableSize * DESIRED_LOAD_FACTOR < setSize) {
      tableSize <<= 1;
    }
    return tableSize;
  }

  static CharMatcher from(BitSet chars, String description) {
    // Compute the filter.
    long filter = 0;
    int size = chars.cardinality();
    boolean containsZero = chars.get(0);
    // Compute the hash table.
    char[] table = new char[chooseTableSize(size)];
    int mask = table.length - 1;
    for (int c = chars.nextSetBit(0); c != -1; c = chars.nextSetBit(c + 1)) {
      // Compute the filter at the same time.
      filter |= 1L << c;
      int index = smear(c) & mask;
      while (true) {
        // Check for empty.
        if (table[index] == 0) {
          table[index] = (char) c;
          break;
        }
        // Linear probing.
        index = (index + 1) & mask;
      }
    }
    return new SmallCharMatcher(table, filter, containsZero, description);
  }

  @Override
  public boolean matches(char c) {
    if (c == 0) {
      return containsZero;
    }
    if (!checkFilter(c)) {
      return false;
    }
    int mask = table.length - 1;
    int startingIndex = smear(c) & mask;
    int index = startingIndex;
    do {
      // Check for empty.
      if (table[index] == 0) {
        return false;
      // Check for match.
      } else if (table[index] == c) {
        return true;
      } else {
        // Linear probing.
        index = (index + 1) & mask;
      }
      // Check to see if we wrapped around the whole table.
    } while (index != startingIndex);
    return false;
  }

  @Override
  void setBits(BitSet table) {
    if (containsZero) {
      table.set(0);
    }
    for (char c : this.table) {
      if (c != 0) {
        table.set(c);
      }
    }
  }
}
