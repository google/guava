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

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.CharMatcher.FastMatcher;

/**
 * An immutable version of CharMatcher for medium-sized sets of characters that uses a hash table
 * with linear probing to check for matches.
 *
 * @author Christopher Swenson
 */
@GwtCompatible(emulated = true)
final class MediumCharMatcher extends FastMatcher {
  static final int MAX_SIZE = 1023;
  private final char[] table;
  private final boolean containsZero;
  private final long filter;

  private MediumCharMatcher(char[] table, long filter, boolean containsZero,
      String description) {
    super(description);
    this.table = table;
    this.filter = filter;
    this.containsZero = containsZero;
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

  @Override
  public boolean matches(char c) {
    if (c == 0) {
      return containsZero;
    }
    if (!checkFilter(c)) {
      return false;
    }
    int mask = table.length - 1;
    int startingIndex = c & mask;
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
}

