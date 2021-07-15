/*
 * Copyright (C) 2011 The Guava Authors
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.common.primitives;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Arrays;
import java.util.Comparator;

import com.google.common.annotations.Beta;
import com.google.common.annotations.GwtCompatible;

/**
 * Static utility methods pertaining to {@code short} primitives that interpret values as
 * <i>unsigned</i> (that is, any negative value {@code x} is treated as the positive value
 * {@code 2^32 + x}). The methods for which signedness is not an issue are in {@link Shorts}, as well
 * as signed versions of methods for which signedness is an issue.
 * 
 * <p>In addition, this class provides several static methods for converting an {@code short} to a
 * {@code String} and a {@code String} to an {@code short} that treat the {@code short} as an unsigned
 * number.
 * 
 * <p>Users of these utilities must be <i>extremely careful</i> not to mix up signed and unsigned
 * {@code short} values. When possible, it is recommended that the {@link UnsignedShort} wrapper
 * class be used, at a small efficiency penalty, to enforce the distinction in the type system.
 * 
 * @author Louis Wasserman
 * @author Johannes Schneider (<a href="mailto:js@cedarsoft.com">js@cedarsoft.com</a>)
 * @since 19.0
 */
@Beta
@GwtCompatible
public final class UnsignedShorts {
  static final int SHORT_MASK = 0xffff;

  private UnsignedShorts() {}

  static short flip(short value) {
    return (short) (value ^ Short.MIN_VALUE);
  }

  /**
   * Compares the two specified {@code short} values, treating them as unsigned values between
   * {@code 0} and {@code 2^32 - 1} inclusive.
   * 
   * @param a the first unsigned {@code short} to compare
   * @param b the second unsigned {@code short} to compare
   * @return a negative value if {@code a} is less than {@code b}; a positive value if {@code a} is
   *         greater than {@code b}; or zero if they are equal
   */
  public static int compare(short a, short b) {
    return Shorts.compare(flip(a), flip(b));
  }

  /**
   * Returns the value of the given {@code short} as an {@code int}, when treated as unsigned.
   */
  public static int toInt(short value) {
    return value & SHORT_MASK;
  }

  /**
   * Returns the least value present in {@code array}.
   * 
   * @param array a <i>nonempty</i> array of unsigned {@code short} values
   * @return the value present in {@code array} that is less than or equal to every other value in
   *         the array according to {@link #compare}
   * @throws IllegalArgumentException if {@code array} is empty
   */
  public static short min(short... array) {
    checkArgument(array.length > 0);
    short min = flip(array[0]);
    for (int i = 1; i < array.length; i++) {
      short next = flip(array[i]);
      if (next < min) {
        min = next;
      }
    }
    return flip(min);
  }

  /**
   * Returns the greatest value present in {@code array}.
   * 
   * @param array a <i>nonempty</i> array of unsigned {@code short} values
   * @return the value present in {@code array} that is greater than or equal to every other value
   *         in the array according to {@link #compare}
   * @throws IllegalArgumentException if {@code array} is empty
   */
  public static short max(short... array) {
    checkArgument(array.length > 0);
    short max = flip(array[0]);
    for (int i = 1; i < array.length; i++) {
      short next = flip(array[i]);
      if (next > max) {
        max = next;
      }
    }
    return flip(max);
  }

  /**
   * Returns a string containing the supplied unsigned {@code short} values separated by
   * {@code separator}. For example, {@code join("-", 1, 2, 3)} returns the string {@code "1-2-3"}.
   * 
   * @param separator the text that should appear between consecutive values in the resulting
   *        string (but not at the start or end)
   * @param array an array of unsigned {@code short} values, possibly empty
   */
  public static String join(String separator, short... array) {
    checkNotNull(separator);
    if (array.length == 0) {
      return "";
    }

    // For pre-sizing a builder, just get the right order of magnitude
    StringBuilder builder = new StringBuilder(array.length * 5);
    builder.append(array[0]);
    for (int i = 1; i < array.length; i++) {
      builder.append(separator).append(toString(array[i]));
    }
    return builder.toString();
  }

  /**
   * Returns a comparator that compares two {@code short} arrays lexicographically. That is, it
   * compares, using {@link #compare(short, short)}), the first pair of values that follow any common
   * prefix, or when one array is a prefix of the other, treats the shorter array as the lesser.
   * For example, {@code [] < [1] < [1, 2] < [2]}. Values are treated as unsigned.
   * 
   * <p>
   * The returned comparator is inconsistent with {@link Object#equals(Object)} (since arrays
   * support only identity equality), but it is consistent with {@link Arrays#equals(short[], short[])}.
   * 
   * @see <a href="http://en.wikipedia.org/wiki/Lexicographical_order"> Lexicographical order
   *      article at Wikipedia</a>
   */
  public static Comparator<short[]> lexicographicalComparator() {
    return LexicographicalComparator.INSTANCE;
  }

  enum LexicographicalComparator implements Comparator<short[]> {
    INSTANCE;

    @Override
    public int compare(short[] left, short[] right) {
      int minLength = Math.min(left.length, right.length);
      for (int i = 0; i < minLength; i++) {
        if (left[i] != right[i]) {
          return UnsignedShorts.compare(left[i], right[i]);
        }
      }
      return left.length - right.length;
    }
  }

  /**
   * Returns dividend / divisor, where the dividend and divisor are treated as unsigned 32-bit
   * quantities.
   * 
   * @param dividend the dividend (numerator)
   * @param divisor the divisor (denominator)
   * @throws ArithmeticException if divisor is 0
   */
  public static short divide(short dividend, short divisor) {
    return (short) (toInt(dividend) / toInt(divisor));
  }

  /**
   * Returns dividend % divisor, where the dividend and divisor are treated as unsigned 64-bit
   * quantities.
   * 
   * @param dividend the dividend (numerator)
   * @param divisor the divisor (denominator)
   * @throws ArithmeticException if divisor is 0
   */
  public static short remainder(short dividend, short divisor) {
    return (short) (toInt(dividend) % toInt(divisor));
  }

  /**
   * Returns the unsigned {@code short} value represented by the given decimal string.
   * 
   * @throws NumberFormatException if the string does not contain a valid unsigned integer, or if
   *         the value represented is too large to fit in an unsigned {@code short}.
   * @throws NullPointerException if {@code s} is null
   */
  public static short parseUnsignedShort(String s) {
    return parseUnsignedShort(s, 10);
  }

  /**
   * Returns the unsigned {@code short} value represented by a string with the given radix.
   * 
   * @param string the string containing the unsigned integer representation to be parsed.
   * @param radix the radix to use while parsing {@code s}; must be between
   *        {@link Character#MIN_RADIX} and {@link Character#MAX_RADIX}.
   * @throws NumberFormatException if the string does not contain a valid unsigned {@code short}, or
   *         if supplied radix is invalid.
   */
  public static short parseUnsignedShort(String string, int radix) {
    checkNotNull(string);
    int result = Integer.parseInt(string, radix);
    if ((result & SHORT_MASK) != result) {
      throw new NumberFormatException("Input " + string + " in base " + radix
          + " is not in the range of an unsigned short");
    }
    return (short) result;
  }

  /**
   * Returns a string representation of x, where x is treated as unsigned.
   */
  public static String toString(short x) {
    return toString(x, 10);
  }

  /**
   * Returns a string representation of {@code x} for the given radix, where {@code x} is treated
   * as unsigned.
   * 
   * @param x the value to convert to a string.
   * @param radix the radix to use while working with {@code x}
   * @throws IllegalArgumentException if {@code radix} is not between {@link Character#MIN_RADIX}
   *         and {@link Character#MAX_RADIX}.
   */
  public static String toString(short x, int radix) {
    int asInt = x & SHORT_MASK;
    return Integer.toString(asInt, radix);
  }
}
