/*
 * Copyright (C) 2011 The Guava Authors
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
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.util.Arrays;
import java.util.Comparator;

/**
 * Static utility methods pertaining to {@code int} primitives that interpret values as
 * <i>unsigned</i> (that is, any negative value {@code x} is treated as the positive value {@code
 * 2^32 + x}). The methods for which signedness is not an issue are in {@link Ints}, as well as
 * signed versions of methods for which signedness is an issue.
 *
 * <p>In addition, this class provides several static methods for converting an {@code int} to a
 * {@code String} and a {@code String} to an {@code int} that treat the {@code int} as an unsigned
 * number.
 *
 * <p>Users of these utilities must be <i>extremely careful</i> not to mix up signed and unsigned
 * {@code int} values. When possible, it is recommended that the {@link UnsignedInteger} wrapper
 * class be used, at a small efficiency penalty, to enforce the distinction in the type system.
 *
 * <p>See the Guava User Guide article on <a
 * href="https://github.com/google/guava/wiki/PrimitivesExplained#unsigned-support">unsigned
 * primitive utilities</a>.
 *
 * @author Louis Wasserman
 * @since 11.0
 */
@GwtCompatible
@ElementTypesAreNonnullByDefault
public final class UnsignedInts {
  static final long INT_MASK = 0xffffffffL;

  private UnsignedInts() {}

  static int flip(int value) {
    return value ^ Integer.MIN_VALUE;
  }

  /**
   * Compares the two specified {@code int} values, treating them as unsigned values between {@code
   * 0} and {@code 2^32 - 1} inclusive.
   *
   * <p><b>Java 8 users:</b> use {@link Integer#compareUnsigned(int, int)} instead.
   *
   * @param a the first unsigned {@code int} to compare
   * @param b the second unsigned {@code int} to compare
   * @return a negative value if {@code a} is less than {@code b}; a positive value if {@code a} is
   *     greater than {@code b}; or zero if they are equal
   */
  public static int compare(int a, int b) {
    return Ints.compare(flip(a), flip(b));
  }

  /**
   * Returns the value of the given {@code int} as a {@code long}, when treated as unsigned.
   *
   * <p><b>Java 8 users:</b> use {@link Integer#toUnsignedLong(int)} instead.
   */
  public static long toLong(int value) {
    return value & INT_MASK;
  }

  /**
   * Returns the {@code int} value that, when treated as unsigned, is equal to {@code value}, if
   * possible.
   *
   * @param value a value between 0 and 2<sup>32</sup>-1 inclusive
   * @return the {@code int} value that, when treated as unsigned, equals {@code value}
   * @throws IllegalArgumentException if {@code value} is negative or greater than or equal to
   *     2<sup>32</sup>
   * @since 21.0
   */
  public static int checkedCast(long value) {
    checkArgument((value >> Integer.SIZE) == 0, "out of range: %s", value);
    return (int) value;
  }

  /**
   * Returns the {@code int} value that, when treated as unsigned, is nearest in value to {@code
   * value}.
   *
   * @param value any {@code long} value
   * @return {@code 2^32 - 1} if {@code value >= 2^32}, {@code 0} if {@code value <= 0}, and {@code
   *     value} cast to {@code int} otherwise
   * @since 21.0
   */
  public static int saturatedCast(long value) {
    if (value <= 0) {
      return 0;
    } else if (value >= (1L << 32)) {
      return -1;
    } else {
      return (int) value;
    }
  }

  /**
   * Returns the least value present in {@code array}, treating values as unsigned.
   *
   * @param array a <i>nonempty</i> array of unsigned {@code int} values
   * @return the value present in {@code array} that is less than or equal to every other value in
   *     the array according to {@link #compare}
   * @throws IllegalArgumentException if {@code array} is empty
   */
  public static int min(int... array) {
    checkArgument(array.length > 0);
    int min = flip(array[0]);
    for (int i = 1; i < array.length; i++) {
      int next = flip(array[i]);
      if (next < min) {
        min = next;
      }
    }
    return flip(min);
  }

  /**
   * Returns the greatest value present in {@code array}, treating values as unsigned.
   *
   * @param array a <i>nonempty</i> array of unsigned {@code int} values
   * @return the value present in {@code array} that is greater than or equal to every other value
   *     in the array according to {@link #compare}
   * @throws IllegalArgumentException if {@code array} is empty
   */
  public static int max(int... array) {
    checkArgument(array.length > 0);
    int max = flip(array[0]);
    for (int i = 1; i < array.length; i++) {
      int next = flip(array[i]);
      if (next > max) {
        max = next;
      }
    }
    return flip(max);
  }

  /**
   * Returns a string containing the supplied unsigned {@code int} values separated by {@code
   * separator}. For example, {@code join("-", 1, 2, 3)} returns the string {@code "1-2-3"}.
   *
   * @param separator the text that should appear between consecutive values in the resulting string
   *     (but not at the start or end)
   * @param array an array of unsigned {@code int} values, possibly empty
   */
  public static String join(String separator, int... array) {
    checkNotNull(separator);
    if (array.length == 0) {
      return "";
    }

    // For pre-sizing a builder, just get the right order of magnitude
    StringBuilder builder = new StringBuilder(array.length * 5);
    builder.append(toString(array[0]));
    for (int i = 1; i < array.length; i++) {
      builder.append(separator).append(toString(array[i]));
    }
    return builder.toString();
  }

  /**
   * Returns a comparator that compares two arrays of unsigned {@code int} values <a
   * href="http://en.wikipedia.org/wiki/Lexicographical_order">lexicographically</a>. That is, it
   * compares, using {@link #compare(int, int)}), the first pair of values that follow any common
   * prefix, or when one array is a prefix of the other, treats the shorter array as the lesser. For
   * example, {@code [] < [1] < [1, 2] < [2] < [1 << 31]}.
   *
   * <p>The returned comparator is inconsistent with {@link Object#equals(Object)} (since arrays
   * support only identity equality), but it is consistent with {@link Arrays#equals(int[], int[])}.
   */
  public static Comparator<int[]> lexicographicalComparator() {
    return LexicographicalComparator.INSTANCE;
  }

  enum LexicographicalComparator implements Comparator<int[]> {
    INSTANCE;

    @Override
    public int compare(int[] left, int[] right) {
      int minLength = Math.min(left.length, right.length);
      for (int i = 0; i < minLength; i++) {
        if (left[i] != right[i]) {
          return UnsignedInts.compare(left[i], right[i]);
        }
      }
      return left.length - right.length;
    }

    @Override
    public String toString() {
      return "UnsignedInts.lexicographicalComparator()";
    }
  }

  /**
   * Sorts the array, treating its elements as unsigned 32-bit integers.
   *
   * @since 23.1
   */
  public static void sort(int[] array) {
    checkNotNull(array);
    sort(array, 0, array.length);
  }

  /**
   * Sorts the array between {@code fromIndex} inclusive and {@code toIndex} exclusive, treating its
   * elements as unsigned 32-bit integers.
   *
   * @since 23.1
   */
  public static void sort(int[] array, int fromIndex, int toIndex) {
    checkNotNull(array);
    checkPositionIndexes(fromIndex, toIndex, array.length);
    for (int i = fromIndex; i < toIndex; i++) {
      array[i] = flip(array[i]);
    }
    Arrays.sort(array, fromIndex, toIndex);
    for (int i = fromIndex; i < toIndex; i++) {
      array[i] = flip(array[i]);
    }
  }

  /**
   * Sorts the elements of {@code array} in descending order, interpreting them as unsigned 32-bit
   * integers.
   *
   * @since 23.1
   */
  public static void sortDescending(int[] array) {
    checkNotNull(array);
    sortDescending(array, 0, array.length);
  }

  /**
   * Sorts the elements of {@code array} between {@code fromIndex} inclusive and {@code toIndex}
   * exclusive in descending order, interpreting them as unsigned 32-bit integers.
   *
   * @since 23.1
   */
  public static void sortDescending(int[] array, int fromIndex, int toIndex) {
    checkNotNull(array);
    checkPositionIndexes(fromIndex, toIndex, array.length);
    for (int i = fromIndex; i < toIndex; i++) {
      array[i] ^= Integer.MAX_VALUE;
    }
    Arrays.sort(array, fromIndex, toIndex);
    for (int i = fromIndex; i < toIndex; i++) {
      array[i] ^= Integer.MAX_VALUE;
    }
  }

  /**
   * Returns dividend / divisor, where the dividend and divisor are treated as unsigned 32-bit
   * quantities.
   *
   * <p><b>Java 8 users:</b> use {@link Integer#divideUnsigned(int, int)} instead.
   *
   * @param dividend the dividend (numerator)
   * @param divisor the divisor (denominator)
   * @throws ArithmeticException if divisor is 0
   */
  public static int divide(int dividend, int divisor) {
    return (int) (toLong(dividend) / toLong(divisor));
  }

  /**
   * Returns dividend % divisor, where the dividend and divisor are treated as unsigned 32-bit
   * quantities.
   *
   * <p><b>Java 8 users:</b> use {@link Integer#remainderUnsigned(int, int)} instead.
   *
   * @param dividend the dividend (numerator)
   * @param divisor the divisor (denominator)
   * @throws ArithmeticException if divisor is 0
   */
  public static int remainder(int dividend, int divisor) {
    return (int) (toLong(dividend) % toLong(divisor));
  }

  /**
   * Returns the unsigned {@code int} value represented by the given string.
   *
   * <p>Accepts a decimal, hexadecimal, or octal number given by specifying the following prefix:
   *
   * <ul>
   *   <li>{@code 0x}<i>HexDigits</i>
   *   <li>{@code 0X}<i>HexDigits</i>
   *   <li>{@code #}<i>HexDigits</i>
   *   <li>{@code 0}<i>OctalDigits</i>
   * </ul>
   *
   * @throws NumberFormatException if the string does not contain a valid unsigned {@code int} value
   * @since 13.0
   */
  @CanIgnoreReturnValue
  public static int decode(String stringValue) {
    ParseRequest request = ParseRequest.fromString(stringValue);

    try {
      return parseUnsignedInt(request.rawValue, request.radix);
    } catch (NumberFormatException e) {
      NumberFormatException decodeException =
          new NumberFormatException("Error parsing value: " + stringValue);
      decodeException.initCause(e);
      throw decodeException;
    }
  }

  /**
   * Returns the unsigned {@code int} value represented by the given decimal string.
   *
   * <p><b>Java 8 users:</b> use {@link Integer#parseUnsignedInt(String)} instead.
   *
   * @throws NumberFormatException if the string does not contain a valid unsigned {@code int} value
   * @throws NullPointerException if {@code s} is null (in contrast to {@link
   *     Integer#parseInt(String)})
   */
  @CanIgnoreReturnValue
  public static int parseUnsignedInt(String s) {
    return parseUnsignedInt(s, 10);
  }

  /**
   * Returns the unsigned {@code int} value represented by a string with the given radix.
   *
   * <p><b>Java 8 users:</b> use {@link Integer#parseUnsignedInt(String, int)} instead.
   *
   * @param string the string containing the unsigned integer representation to be parsed.
   * @param radix the radix to use while parsing {@code s}; must be between {@link
   *     Character#MIN_RADIX} and {@link Character#MAX_RADIX}.
   * @throws NumberFormatException if the string does not contain a valid unsigned {@code int}, or
   *     if supplied radix is invalid.
   * @throws NullPointerException if {@code s} is null (in contrast to {@link
   *     Integer#parseInt(String)})
   */
  @CanIgnoreReturnValue
  public static int parseUnsignedInt(String string, int radix) {
    checkNotNull(string);
    long result = Long.parseLong(string, radix);
    if ((result & INT_MASK) != result) {
      throw new NumberFormatException(
          "Input " + string + " in base " + radix + " is not in the range of an unsigned integer");
    }
    return (int) result;
  }

  /**
   * Returns a string representation of x, where x is treated as unsigned.
   *
   * <p><b>Java 8 users:</b> use {@link Integer#toUnsignedString(int)} instead.
   */
  public static String toString(int x) {
    return toString(x, 10);
  }

  /**
   * Returns a string representation of {@code x} for the given radix, where {@code x} is treated as
   * unsigned.
   *
   * <p><b>Java 8 users:</b> use {@link Integer#toUnsignedString(int, int)} instead.
   *
   * @param x the value to convert to a string.
   * @param radix the radix to use while working with {@code x}
   * @throws IllegalArgumentException if {@code radix} is not between {@link Character#MIN_RADIX}
   *     and {@link Character#MAX_RADIX}.
   */
  public static String toString(int x, int radix) {
    long asLong = x & INT_MASK;
    return Long.toString(asLong, radix);
  }
}
