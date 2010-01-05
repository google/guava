/*
 * Copyright (C) 2009 Google Inc.
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

package com.google.common.primitives;

import java.util.Comparator;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Static utility methods pertaining to {@code byte} primitives that interpret
 * values as <i>unsigned</i> (that is, any negative value {@code b} is treated
 * as the positive value {@code 256 + b}). The corresponding methods that treat
 * the values as signed are found in {@link SignedBytes}, and the methods for
 * which signedness is not an issue are in {@link Bytes}.
 *
 * @author Kevin Bourrillion
 * @since 2009.09.15 <b>tentative</b>
 */
public final class UnsignedBytes {
  private UnsignedBytes() {}

  /**
   * Returns the {@code byte} value that, when treated as unsigned, is equal to
   * {@code value}, if possible.
   *
   * @param value a value between 0 and 255 inclusive
   * @return the {@code byte} value that, when treated as unsigned, equals
   *     {@code value}
   * @throws IllegalArgumentException if {@code value} is negative or greater
   *     than 255
   */
  public static byte checkedCast(long value) {
    checkArgument(value >> 8 == 0, "out of range: %s", value);
    return (byte) value;
  }

  /**
   * Returns the {@code byte} value that, when treated as unsigned, is nearest
   * in value to {@code value}.
   *
   * @param value any {@code long} value
   * @return {@code (byte) 255} if {@code value >= 255}, {@code (byte) 0} if
   *     {@code value <= 0}, and {@code value} cast to {@code byte} otherwise
   */
  public static byte saturatedCast(long value) {
    if (value > 255) {
      return (byte) 255; // -1
    }
    if (value < 0) {
      return (byte) 0;
    }
    return (byte) value;
  }

  /**
   * Compares the two specified {@code byte} values, treating them as unsigned
   * values between 0 and 255 inclusive. For example, {@code (byte) -127} is
   * considered greater than {@code (byte) 127} because it is seen as having
   * the value of positive {@code 129}.
   *
   * @param a the first {@code byte} to compare
   * @param b the second {@code byte} to compare
   * @return a negative value if {@code a} is less than {@code b}; a positive
   *     value if {@code a} is greater than {@code b}; or zero if they are equal
   */
  public static int compare(byte a, byte b) {
    return (a & 0xFF) - (b & 0xFF);
  }

  /**
   * Returns the least value present in {@code array}.
   *
   * @param array a <i>nonempty</i> array of {@code byte} values
   * @return the value present in {@code array} that is less than or equal to
   *     every other value in the array
   * @throws IllegalArgumentException if {@code array} is empty
   */
  public static byte min(byte... array) {
    checkArgument(array.length > 0);
    int min = array[0] & 0xFF;
    for (int i = 1; i < array.length; i++) {
      int next = array[i] & 0xFF;
      if (next < min) {
        min = next;
      }
    }
    return (byte) min;
  }

  /**
   * Returns the greatest value present in {@code array}.
   *
   * @param array a <i>nonempty</i> array of {@code byte} values
   * @return the value present in {@code array} that is greater than or equal
   *     to every other value in the array
   * @throws IllegalArgumentException if {@code array} is empty
   */
  public static byte max(byte... array) {
    checkArgument(array.length > 0);
    int max = array[0] & 0xFF;
    for (int i = 1; i < array.length; i++) {
      int next = array[i] & 0xFF;
      if (next > max) {
        max = next;
      }
    }
    return (byte) max;
  }

  /**
   * Returns a string containing the supplied {@code byte} values separated by
   * {@code separator}. For example, {@code join(":", (byte) 1, (byte) 2,
   * (byte) 255)} returns the string {@code "1:2:255"}.
   *
   * @param separator the text that should appear between consecutive values in
   *     the resulting string (but not at the start or end)
   * @param array an array of {@code byte} values, possibly empty
   */
  public static String join(String separator, byte... array) {
    checkNotNull(separator);
    if (array.length == 0) {
      return "";
    }

    // For pre-sizing a builder, just get the right order of magnitude
    StringBuilder builder = new StringBuilder(array.length * 5);
    builder.append(array[0] & 0xFF);
    for (int i = 1; i < array.length; i++) {
      builder.append(separator).append(array[i] & 0xFF);
    }
    return builder.toString();
  }

  /**
   * Returns a comparator that compares two {@code byte} arrays
   * lexicographically. That is, it compares, using {@link
   * #compare(byte, byte)}), the first pair of values that follow any common
   * prefix, or when one array is a prefix of the other, treats the shorter
   * array as the lesser. For example, {@code [] < [0x01] < [0x01, 0x7F] <
   * [0x01, 0x80] < [0x02]}. Values are treated as unsigned.
   *
   * <p>The returned comparator is inconsistent with {@link
   * Object#equals(Object)} (since arrays support only identity equality), but
   * it is consistent with {@link java.util.Arrays#equals(byte[], byte[])}.
   *
   * @see <a href="http://en.wikipedia.org/wiki/Lexicographical_order">
   *     Lexicographical order</a> article at Wikipedia
   * @since 2010.01.04 <b>tentative</b>
   */
  public static Comparator<byte[]> lexicographicalComparator() {
    return LexicographicalComparator.INSTANCE;
  }

  private enum LexicographicalComparator implements Comparator<byte[]> {
    INSTANCE;

    public int compare(byte[] left, byte[] right) {
      int minLength = Math.min(left.length, right.length);
      for (int i = 0; i < minLength; i++) {
        int result = UnsignedBytes.compare(left[i], right[i]);
        if (result != 0) {
          return result;
        }
      }
      return left.length - right.length;
    }
  }
}
