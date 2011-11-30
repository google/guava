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

/**
 * Android {@code Integer.java}, stripped down to {@code parseInt} methods.
 */
final class AndroidInteger {
  /**
   * Parses the specified string as a signed integer value using the specified
   * radix. The ASCII character \u002d ('-') is recognized as the minus sign.
   *
   * @param string
   *            the string representation of an integer value.
   * @param radix
   *            the radix to use when parsing.
   * @return the primitive integer value represented by {@code string} using
   *         {@code radix}.
   * @throws NumberFormatException
   *             if {@code string} is {@code null} or has a length of zero,
   *             {@code radix < Character.MIN_RADIX},
   *             {@code radix > Character.MAX_RADIX}, or if {@code string}
   *             can not be parsed as an integer value.
   */
  static int parseInt(String string, int radix) throws NumberFormatException {
    if (string == null || radix < Character.MIN_RADIX || radix > Character.MAX_RADIX) {
      throw new NumberFormatException("unable to parse '"+string+"' as integer");
    }
    int length = string.length(), i = 0;
    if (length == 0) {
      throw new NumberFormatException("unable to parse '"+string+"' as integer");
    }
    boolean negative = string.charAt(i) == '-';
    if (negative && ++i == length) {
      throw new NumberFormatException("unable to parse '"+string+"' as integer");
    }

    return parse(string, i, radix, negative);
  }

  private static int parse(String string, int offset, int radix, boolean negative)
      throws NumberFormatException {
    int max = Integer.MIN_VALUE / radix;
    int result = 0, length = string.length();
    while (offset < length) {
      int digit = Character.digit(string.charAt(offset++), radix);
      if (digit == -1) {
        throw new NumberFormatException("unable to parse '"+string+"' as integer");
      }
      if (max > result) {
        throw new NumberFormatException("unable to parse '"+string+"' as integer");
      }
      int next = result * radix - digit;
      if (next > result) {
        throw new NumberFormatException("unable to parse '"+string+"' as integer");
      }
      result = next;
    }
    if (!negative) {
      result = -result;
      if (result < 0) {
        throw new NumberFormatException("unable to parse '"+string+"' as integer");
      }
    }
    return result;
  }

  private AndroidInteger() {}
}
