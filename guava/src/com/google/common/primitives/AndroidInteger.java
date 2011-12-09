/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.google.common.primitives;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import javax.annotation.CheckForNull;

/**
 * Static utility methods derived from Android's {@code Integer.java}.
 */
final class AndroidInteger {
  /**
   * See {@link Ints#tryParse(String)} for the public interface.
   */
  @CheckForNull
  static Integer tryParse(String string) {
    return tryParse(string, 10);
  }

  /**
   * See {@link Ints#tryParse(String, int)} for the public interface.
   */
  @CheckForNull
  static Integer tryParse(String string, int radix) {
    checkNotNull(string);
    checkArgument(radix >= Character.MIN_RADIX,
        "Invalid radix %s, min radix is %s", radix, Character.MIN_RADIX);
    checkArgument(radix <= Character.MAX_RADIX,
        "Invalid radix %s, max radix is %s", radix, Character.MAX_RADIX);
    int length = string.length(), i = 0;
    if (length == 0) {
      return null;
    }
    boolean negative = string.charAt(i) == '-';
    if (negative && ++i == length) {
      return null;
    }
    return tryParse(string, i, radix, negative);
  }

  @CheckForNull
  private static Integer tryParse(String string, int offset, int radix,
      boolean negative) {
    int max = Integer.MIN_VALUE / radix;
    int result = 0, length = string.length();
    while (offset < length) {
      int digit = Character.digit(string.charAt(offset++), radix);
      if (digit == -1) {
        return null;
      }
      if (max > result) {
        return null;
      }
      int next = result * radix - digit;
      if (next > result) {
        return null;
      }
      result = next;
    }
    if (!negative) {
      result = -result;
      if (result < 0) {
        return null;
      }
    }
    // For GWT where ints do not overflow
    if (result > Integer.MAX_VALUE || result < Integer.MIN_VALUE) {
      return null;
    }
    return result;
  }

  private AndroidInteger() {}
}
