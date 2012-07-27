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

/**
 * An immutable small version of CharMatcher that uses an efficient hash table implementation, with
 * non-power-of-2 sizing to try to use no reprobing, if possible.
 *
 * @author Christopher Swenson
 */
@GwtCompatible(emulated = true)
final class SmallCharMatcher extends CharMatcher {
  static final int MAX_SIZE = 63;
  static final int MAX_TABLE_SIZE = 128;
  private final boolean reprobe;
  private final char[] table;
  private final boolean containsZero;
  final long filter;

  private SmallCharMatcher(char[] table, long filter, boolean containsZero,
      boolean reprobe, String description) {
    super(description);
    this.table = table;
    this.filter = filter;
    this.containsZero = containsZero;
    this.reprobe = reprobe;
  }

  private boolean checkFilter(int c) {
    return 1 == (1 & (filter >> c));
  }

  @Override
  public CharMatcher precomputed() {
    return this;
  }

  @Override
  public boolean matches(char c) {
    if (c == 0) {
      return containsZero;
    }
    if (!checkFilter(c)) {
      return false;
    }
    int index = c % table.length;
    if (index < 0) {
      index += table.length;
    }
    while (true) {
      // Check for empty.
      if (table[index] == 0) {
        return false;
      } else if (table[index] == c) {
        return true;
      } else if (reprobe) {
        // Linear probing will terminate eventually.
        index = (index + 1) % table.length;
      } else {
        return false;
      }
    }
  }
}

