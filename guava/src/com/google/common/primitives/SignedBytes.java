/*
 * Copyright (C) 2009 The Guava Authors
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

package com.google.common.primitives;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkPositionIndexes;

import com.google.common.annotations.GwtCompatible;
import java.util.Arrays;
import java.util.Comparator;

/**
 * Static utility methods pertaining to {@code byte} primitives that interpret values as signed. The
 * corresponding methods that treat the values as unsigned are found in {@link UnsignedBytes}, and
 * the methods for which signedness is not an issue are in {@link Bytes}.
 *
 * <p>See the Guava User Guide article on <a
 * href="https://github.com/google/guava/wiki/PrimitivesExplained">primitive utilities</a>.
 *
 * @author Kevin Bourrillion
 * @since 1.0
 */
// TODO(kevinb): how to prevent warning on UnsignedBytes when building GWT
// javadoc?
@GwtCompatible
@ElementTypesAreNonnullByDefault
public final class SignedBytes {
  private SignedBytes() {}

  /**
   * The largest power of two that can be represented as a signed {@code byte}.
   *
   * @since 10.0
   */
  public static final byte MAX_POWER_OF_TWO = 1 << 6;

  /**
   * Returns the {@code byte} value that is equal to {@code value}, if possible.
   *
   * @param value any value in the range of the {@code byte} type
   * @return the {@code byte} value that equals {@code value}
   * @throws IllegalArgumentException if {@code value} is greater than {@link Byte#MAX_VALUE} or
   *     less than {@link Byte#MIN_VALUE}
   */
  public static byte checkedCast(long value) {
    byte result = (byte) value;
    checkArgument(result == value, "Out of range: %s", value);
    return result;
  }

  /**
   * Returns the {@code byte} nearest in value to {@code value}.
   *
   * @param value any {@code long} value
   * @return the same value cast to {@code byte} if it is in the range of the {@code byte} type,
   *     {@link Byte#MAX_VALUE} if it is too large, or {@link Byte#MIN_VALUE} if it is too small
   */
  public static byte saturatedCast(long value) {
    if (value > Byte.MAX_VALUE) {
      return Byte.MAX_VALUE;
    }
    if (value < Byte.MIN_VALUE) {
      return Byte.MIN_VALUE;
    }
    return (byte) value;
  }

  /**
   * Compares the two specified {@code byte} values. The sign of the value returned is the same as
   * that of {@code ((Byte) a).compareTo(b)}.
   *
   * <p><b>Note:</b> this method behaves identically to the JDK 7 method {@link Byte#compare}.
   *
   * @param a the first {@code byte} to compare
   * @param b the second {@code byte} to compare
   * @return a negative value if {@code a} is less than {@code b}; a positive value if {@code a} is
   *     greater than {@code b}; or zero if they are equal
   */
  // TODO(kevinb): if Ints.compare etc. are ever removed, *maybe* remove this
  // one too, which would leave compare methods only on the Unsigned* classes.
  public static int compare(byte a, byte b) {
    return a - b; // safe due to restricted range
  }

  /**
   * Returns the least value present in {@code array}.
   *
   * @param array a <i>nonempty</i> array of {@code byte} values
   * @return the value present in {@code array} that is less than or equal to every other value in
   *     the array
   * @throws IllegalArgumentException if {@code array} is empty
   */
  public static byte min(byte... array) {
    checkArgument(array.length > 0);
    byte min = array[0];
    for (int i = 1; i < array.length; i++) {
      if (array[i] < min) {
        min = array[i];
      }
    }
    return min;
  }

  /**
   * Returns the greatest value present in {@code array}.
   *
   * @param array a <i>nonempty</i> array of {@code byte} values
   * @return the value present in {@code array} that is greater than or equal to every other value
   *     in the array
   * @throws IllegalArgumentException if {@code array} is empty
   */
  public static byte max(byte... array) {
    checkArgument(array.length > 0);
    byte max = array[0];
    for (int i = 1; i < array.length; i++) {
      if (array[i] > max) {
        max = array[i];
      }
    }
    return max;
  }

  /**
   * Returns a string containing the supplied {@code byte} values separated by {@code separator}.
   * For example, {@code join(":", 0x01, 0x02, -0x01)} returns the string {@code "1:2:-1"}.
   *
   * @param separator the text that should appear between consecutive values in the resulting string
   *     (but not at the start or end)
   * @param array an array of {@code byte} values, possibly empty
   */
  public static String join(String separator, byte... array) {
    checkNotNull(separator);
    if (array.length == 0) {
      return "";
    }

    // For pre-sizing a builder, just get the right order of magnitude
    StringBuilder builder = new StringBuilder(array.length * 5);
    builder.append(array[0]);
    for (int i = 1; i < array.length; i++) {
      builder.append(separator).append(array[i]);
    }
    return builder.toString();
  }

  /**
   * Returns a comparator that compares two {@code byte} arrays <a
   * href="http://en.wikipedia.org/wiki/Lexicographical_order">lexicographically</a>. That is, it
   * compares, using {@link #compare(byte, byte)}), the first pair of values that follow any common
   * prefix, or when one array is a prefix of the other, treats the shorter array as the lesser. For
   * example, {@code [] < [0x01] < [0x01, 0x80] < [0x01, 0x7F] < [0x02]}. Values are treated as
   * signed.
   *
   * <p>The returned comparator is inconsistent with {@link Object#equals(Object)} (since arrays
   * support only identity equality), but it is consistent with {@link
   * java.util.Arrays#equals(byte[], byte[])}.
   *
   * @since 2.0
   */
  public static Comparator<byte[]> lexicographicalComparator() {
    return LexicographicalComparator.INSTANCE;
  }

  private enum LexicographicalComparator implements Comparator<byte[]> {
    INSTANCE;

    @Override
    public int compare(byte[] left, byte[] right) {
      int minLength = Math.min(left.length, right.length);
      for (int i = 0; i < minLength; i++) {
        int result = SignedBytes.compare(left[i], right[i]);
        if (result != 0) {
          return result;
        }
      }
      return left.length - right.length;
    }

    @Override
    public String toString() {
      return "SignedBytes.lexicographicalComparator()";
    }
  }

  /**
   * Sorts the elements of {@code array} in descending order.
   *
   * @since 23.1
   */
  public static void sortDescending(byte[] array) {
    checkNotNull(array);
    sortDescending(array, 0, array.length);
  }

  /**
   * Sorts the elements of {@code array} between {@code fromIndex} inclusive and {@code toIndex}
   * exclusive in descending order.
   *
   * @since 23.1
   */
  public static void sortDescending(byte[] array, int fromIndex, int toIndex) {
    checkNotNull(array);
    checkPositionIndexes(fromIndex, toIndex, array.length);
    Arrays.sort(array, fromIndex, toIndex);
    Bytes.reverse(array, fromIndex, toIndex);
  }
}
