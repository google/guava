/*
 * Copyright (C) 2011 The Guava Authors
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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.annotations.Beta;
import com.google.common.annotations.GwtCompatible;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Comparator;

/**
 * Static utility methods pertaining to {@code long} primitives that interpret values as
 * <i>unsigned</i> (that is, any negative value {@code x} is treated as the positive value
 * {@code 2^64 + x}). The methods for which signedness is not an issue are in {@link Longs}, as well
 * as signed versions of methods for which signedness is an issue.
 *
 * <p>In addition, this class provides several static methods for converting a {@code long} to a
 * {@code String} and a {@code String} to a {@code long} that treat the long as an unsigned number.
 *
 * @author Louis Wasserman
 * @author Brian Milch
 * @author Peter Epstein
 * @since Guava release 10
 */
@Beta
@GwtCompatible
public final class UnsignedLongs {
  private UnsignedLongs() {}

  public static final long MAX_VALUE = -1L; // Equivalent to 2^64 - 1

  public static BigInteger toBigInteger(long unsigned) {
    BigInteger result = BigInteger.valueOf(unsigned & Long.MAX_VALUE);
    if (unsigned < 0) {
      result = result.setBit(63);
    }
    return result;
  }

  /**
   * Returns the {@code long} value that, when treated as unsigned, is equal to {@code value}, if
   * possible.
   *
   * @param value a value between 0 inclusive and 2^64 exclusive
   * @return the {@code long} value that, when treated as unsigned, equals {@code value}
   * @throws IllegalArgumentException if {@code value} is negative or greater than or equal to 2^64
   */
  public static long checkedCast(BigInteger value) {
    checkNotNull(value);
    checkArgument(value.signum() >= 0 && value.bitLength() <= 64, "out of range: %s", value);
    return value.longValue();
  }

  /**
   * Returns the {@code long} value that, when treated as unsigned, is nearest in value to
   * {@code value}.
   *
   * @param value any {@code BigInteger} value
   * @return {@link #MAX_VALUE} if {@code value >= 2^64}, {@code 0} if {@code value <= 0}, and
   *         {@code value.longValue()} otherwise
   */
  public static long saturatedCast(BigInteger value) {
    checkNotNull(value);
    if (value.signum() < 0) {
      return 0;
    } else if (value.bitLength() > 64) {
      return MAX_VALUE;
    } else {
      return value.longValue();
    }
  }

  /**
   * Compares the two specified {@code long} values, treating them as unsigned values between
   * {@code 0} and {@code 2^64 - 1} inclusive.
   *
   * @param a the first unsigned {@code long} to compare
   * @param b the second unsigned {@code long} to compare
   * @return a negative value if {@code a} is less than {@code b}; a positive value if {@code a} is
   *         greater than {@code b}; or zero if they are equal
   */
  public static int compare(long a, long b) {
    return Longs.compare(a + Long.MIN_VALUE, b + Long.MIN_VALUE);
  }

  /**
   * Returns the least value present in {@code array}.
   *
   * @param array a <i>nonempty</i> array of unsigned {@code long} values
   * @return the value present in {@code array} that is less than or equal to every other value in
   *         the array according to {@link #compare}
   * @throws IllegalArgumentException if {@code array} is empty
   */
  public static long min(long... array) {
    checkArgument(array.length > 0);
    long min = array[0] + Long.MIN_VALUE;
    for (int i = 1; i < array.length; i++) {
      long next = array[i] + Long.MIN_VALUE;
      if (next < min) {
        min = next;
      }
    }
    return min - Long.MIN_VALUE;
  }

  /**
   * Returns the greatest value present in {@code array}.
   *
   * @param array a <i>nonempty</i> array of unsigned {@code long} values
   * @return the value present in {@code array} that is greater than or equal to every other value
   *         in the array according to {@link #compare}
   * @throws IllegalArgumentException if {@code array} is empty
   */
  public static long max(long... array) {
    checkArgument(array.length > 0);
    long max = array[0] + Long.MIN_VALUE;
    for (int i = 1; i < array.length; i++) {
      long next = array[i] + Long.MIN_VALUE;
      if (next > max) {
        max = next;
      }
    }
    return max - Long.MIN_VALUE;
  }

  /**
   * Returns a string containing the supplied unsigned {@code long} values separated
   * by {@code separator}. For example, {@code join("-", 1, 2, 3)} returns
   * the string {@code "1-2-3"}.
   *
   * @param separator the text that should appear between consecutive values in
   *     the resulting string (but not at the start or end)
   * @param array an array of unsigned {@code long} values, possibly empty
   */
  public static String join(String separator, long... array) {
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
   * Returns a comparator that compares two {@code long} arrays
   * lexicographically. That is, it compares, using {@link
   * #compare(long, long)}), the first pair of values that follow any
   * common prefix, or when one array is a prefix of the other, treats the
   * shorter array as the lesser. For example,
   * {@code [] < [1L] < [1L, 2L] < [2L]}.  Values are treated as unsigned.
   *
   * <p>The returned comparator is inconsistent with {@link
   * Object#equals(Object)} (since arrays support only identity equality), but
   * it is consistent with {@link Arrays#equals(long[], long[])}.
   *
   * @see <a href="http://en.wikipedia.org/wiki/Lexicographical_order">
   *     Lexicographical order article at Wikipedia</a>
   */
  public static Comparator<long[]> lexicographicalComparator() {
    return LexicographicalComparator.INSTANCE;
  }

  enum LexicographicalComparator implements Comparator<long[]> {
    INSTANCE;

    @Override
    public int compare(long[] left, long[] right) {
      int minLength = Math.min(left.length, right.length);
      for (int i = 0; i < minLength; i++) {
        if (left[i] != right[i]) {
          return UnsignedLongs.compare(left[i], right[i]);
        }
      }
      return left.length - right.length;
    }
  }

  /**
   * Returns dividend / divisor, where the dividend and divisor are treated as
   * unsigned 64-bit quantities.
   *
   * @param dividend the dividend (numerator)
   * @param divisor  the divisor (denominator)
   * @throws ArithmeticException if divisor is 0
   */ static long divide(long dividend, long divisor) {
    if (divisor < 0) { // i.e., divisor >= 2^63:
      if (dividend + Long.MIN_VALUE < divisor + Long.MIN_VALUE) {
        return 0; // dividend < divisor
      } else {
        return 1; // dividend >= divisor
      }
    }

    // Optimization - use signed division if dividend < 2^63
    if (dividend >= 0) {
      return dividend / divisor;
    }

    /*
     * Otherwise, approximate the quotient, check, and correct if necessary.
     * Our approximation is guaranteed to be either exact or one less than the
     * correct value. This follows from fact that floor(floor(x)/i) ==
     * floor(x/i) for any real x and integer i != 0.  The proof is not
     * quite trivial.
     */
    long quotient = ((dividend >>> 1) / divisor) << 1;
    long rem = dividend - quotient * divisor;
    return quotient +
        (rem + Long.MIN_VALUE >= divisor + Long.MIN_VALUE ? 1 : 0);
  }

  /**
   * Returns dividend % divisor, where the dividend and divisor are treated as
   * unsigned 64-bit quantities.
   *
   * @param dividend the dividend (numerator)
   * @param divisor  the divisor (denominator)
   * @throws ArithmeticException if divisor is 0
   */
  static long remainder(long dividend, long divisor) {
    if (divisor < 0) { // i.e., divisor >= 2^63:
      if (dividend + Long.MIN_VALUE < divisor + Long.MIN_VALUE) {
        return dividend; // dividend < divisor
      } else {
        return dividend - divisor; // dividend >= divisor
      }
    }

    // Optimization - use signed modulus if dividend < 2^63
    if (dividend >= 0) {
      return dividend % divisor;
    }

    /*
     * Otherwise, approximate the quotient, check, and correct if necessary.
     * Our approximation is guaranteed to be either exact or one less than the
     * correct value. This follows from fact that floor(floor(x)/i) ==
     * floor(x/i) for any real x and integer i != 0.  The proof is not
     * quite trivial.
     */
    long quotient = ((dividend >>> 1) / divisor) << 1;
    long rem = dividend - quotient * divisor;
    return rem -
        (rem + Long.MIN_VALUE >= divisor + Long.MIN_VALUE ? divisor : 0);
  }

  /**
   * Returns the unsigned long value represented by the given decimal string.
   *
   * @throws NumberFormatException if the string does not contain a valid
   * unsigned integer, or if the value represented is too large to fit in an
   * unsigned long.
   * @throws NullPointerException if {@code s} is null
   */
  public static long parseUnsignedLong(String s) {
    return parseUnsignedLong(s, 10);
  }

  /**
   * Returns the unsigned long value represented by a string with the given
   * radix.
   *
   * @param s the string containing the unsigned long representation to be
   * parsed.
   * @param radix the radix to use while parsing {@code s}; must be between
   * Character.MIN_RADIX and Character.MAX_RADIX.
   * @throws NumberFormatException if the string does not contain a valid
   * unsigned integer with the given radix, or if the value represented is
   * too large to fit in an unsigned long.  Also thrown if supplied radix is
   * invalid.
   * @throws NullPointerException if {@code s} is null
   */
  public static long parseUnsignedLong(String s, int radix) {
    checkNotNull(s);
    if (s.length() == 0) {
      throw new NumberFormatException("empty string");
    }
    if (radix < Character.MIN_RADIX || radix > Character.MAX_RADIX) {
      throw new NumberFormatException("illegal radix:" + radix);
    }

    int max_safe_pos = maxSafeDigits[radix] - 1;
    long value = 0;
    for (int pos = 0; pos < s.length(); pos++) {
      int digit = Character.digit(s.charAt(pos), radix);
      if (digit == -1) {
        throw new NumberFormatException(s);
      }
      if (pos > max_safe_pos && overflowInParse(value, digit, radix)) {
        throw new NumberFormatException("Too large for unsigned long: " + s);
      }
      value = (value * radix) + digit;
    }

    return value;
  }

  /**
   * Returns true if (current * radix) + digit is a number too large to be represented by an
   * unsigned long. This is useful for detecting overflow while parsing a string representation of
   * a number. Does not verify whether supplied radix is valid, passing an invalid radix will give
   * undefined results or an ArrayIndexOutOfBoundsException.
   */
  private static boolean overflowInParse(long current, int digit, int radix) {
    if (current >= 0) {
      if (current < maxValueDivs[radix]) {
        return false;
      }
      if (current > maxValueDivs[radix]) {
        return true;
      }
      // current == maxValueDivs[radix]
      return (digit > maxValueMods[radix]);
    }

    // current < 0: high bit is set
    return true;
  }

  /**
   * Returns a string representation of x, where x is treated as unsigned.
   */
  public static String toString(long x) {
    return toString(x, 10);
  }

  /**
   * Returns a string representation of {@code x} for the given
   * radix, where {@code x} is treated as unsigned.
   *
   * @param x the value to convert to a string.
   * @param radix the radix to use while working with {@code x}; must be
   * between Character.MIN_RADIX and Character.MAX_RADIX.  Otherwise,
   * the radix {@code 10} is used.
   */
  public static String toString(long x, int radix) {
    if(radix < Character.MIN_RADIX | radix > Character.MAX_RADIX){
      radix = 10;
    }
    if (x == 0) {
      // Simply return "0"
      return "0";
    } else {
      char[] buf = new char[64];
      int i = buf.length;
      if (x < 0) {
        // Split x into high-order and low-order halves.
        // Individual digits are generated from the bottom half into which
        // bits are moved continously from the top half.
        long top = x >>> 32;
        long bot = (x & 0xffffffffl) + ((top % radix) << 32);
        top /= radix;
        while ((bot > 0) || (top > 0)) {
          buf[--i] = Character.forDigit((int)(bot % radix), radix);
          bot = (bot / radix) + ((top % radix) << 32);
          top /= radix;
        }
      } else {
        // Simple modulo/division approach
        while (x > 0) {
          buf[--i] = Character.forDigit((int)(x % radix), radix);
          x /= radix;
        }
      }
      // Generate string
      return new String(buf, i, buf.length - i);
    }
  }

  // calculated as 0xffffffffffffffff / radix
  private static final long[] maxValueDivs = new long[Character.MAX_RADIX + 1];
  private static final int[] maxValueMods = new int[Character.MAX_RADIX + 1];
  private static final int[] maxSafeDigits = new int[Character.MAX_RADIX + 1];
  static {
    BigInteger overflow = new BigInteger("10000000000000000", 16);
    for (int i = Character.MIN_RADIX; i <= Character.MAX_RADIX; i++) {
      maxValueDivs[i] = divide(MAX_VALUE, i);
      maxValueMods[i] = (int) remainder(MAX_VALUE, i);
      maxSafeDigits[i] = overflow.toString(i).length() - 1;
    }
  }
}
