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

package com.google.common.math;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.math.DoubleUtils.IMPLICIT_BIT;
import static com.google.common.math.DoubleUtils.SIGNIFICAND_BITS;
import static com.google.common.math.DoubleUtils.getExponent;
import static com.google.common.math.DoubleUtils.getSignificand;
import static com.google.common.math.DoubleUtils.isFinite;
import static com.google.common.math.DoubleUtils.isNormal;
import static com.google.common.math.DoubleUtils.next;
import static com.google.common.math.DoubleUtils.scaleNormalize;
import static com.google.common.math.MathPreconditions.checkInRange;
import static com.google.common.math.MathPreconditions.checkNonNegative;
import static com.google.common.math.MathPreconditions.checkRoundingUnnecessary;

import java.math.BigInteger;
import java.math.RoundingMode;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.annotations.Beta;

/**
 * A class for arithmetic on doubles that is not covered by {@link java.lang.Math}.
 *
 * @author Louis Wasserman
 * @since 11.0
 */
@Beta
public final class DoubleMath {
  /*
   * This method returns a value y such that rounding y DOWN (towards zero) gives the same result
   * as rounding x according to the specified mode.
   */
  static double roundIntermediate(double x, RoundingMode mode) {
    if (!isFinite(x)) {
      throw new ArithmeticException("input is infinite or NaN");
    }
    switch (mode) {
      case UNNECESSARY:
        checkRoundingUnnecessary(isMathematicalInteger(x));
        return x;

      case FLOOR:
        return (x >= 0.0) ? x : Math.floor(x);

      case CEILING:
        return (x >= 0.0) ? Math.ceil(x) : x;

      case DOWN:
        return x;

      case UP:
        return (x >= 0.0) ? Math.ceil(x) : Math.floor(x);

      case HALF_EVEN:
        return Math.rint(x);

      case HALF_UP:
        if (isMathematicalInteger(x)) {
          return x;
        } else {
          return (x >= 0.0) ? x + 0.5 : x - 0.5;
        }

      case HALF_DOWN:
        if (isMathematicalInteger(x)) {
          return x;
        } else if (x >= 0.0) {
          double z = x + 0.5;
          return (z == x) ? x : next(z, false); // x + 0.5 - epsilon
        } else {
          double z = x - 0.5;
          return (z == x) ? x : next(z, true); // x - 0.5 + epsilon
        }

      default:
        throw new AssertionError();
    }
  }
  
  /**
   * Returns the {@code int} value that is equal to {@code x} rounded with the specified rounding
   * mode, if possible.
   *
   * @throws ArithmeticException if
   *         <ul>
   *         <li>{@code x} is infinite or NaN
   *         <li>{@code x}, after being rounded to a mathematical integer using the specified
   *         rounding mode, is either less than {@code Integer.MIN_VALUE} or greater than {@code
   *         Integer.MAX_VALUE}
   *         <li>{@code x} is not a mathematical integer and {@code mode} is
   *         {@link RoundingMode#UNNECESSARY}
   *         </ul>
   */
  public static int roundToInt(double x, RoundingMode mode) {
    double z = roundIntermediate(x, mode);
    checkInRange(z > MIN_INT_AS_DOUBLE - 1.0 & z < MAX_INT_AS_DOUBLE + 1.0);
    return (int) z;
  }

  private static final double MIN_INT_AS_DOUBLE = -0x1p31;
  private static final double MAX_INT_AS_DOUBLE = 0x1p31 - 1.0;

  /**
   * Returns the {@code long} value that is equal to {@code x} rounded with the specified rounding
   * mode, if possible.
   *
   * @throws ArithmeticException if
   *         <ul>
   *         <li>{@code x} is infinite or NaN
   *         <li>{@code x}, after being rounded to a mathematical integer using the specified
   *         rounding mode, is either less than {@code Long.MIN_VALUE} or greater than {@code
   *         Long.MAX_VALUE}
   *         <li>{@code x} is not a mathematical integer and {@code mode} is
   *         {@link RoundingMode#UNNECESSARY}
   *         </ul>
   */
  public static long roundToLong(double x, RoundingMode mode) {
    double z = roundIntermediate(x, mode);
    checkInRange(MIN_LONG_AS_DOUBLE - z < 1.0 & z < MAX_LONG_AS_DOUBLE_PLUS_ONE);
    return (long) z;
  }

  private static final double MIN_LONG_AS_DOUBLE = -0x1p63;
  /*
   * We cannot store Long.MAX_VALUE as a double without losing precision.  Instead, we store
   * Long.MAX_VALUE + 1 == -Long.MIN_VALUE, and then offset all comparisons by 1.
   */
  private static final double MAX_LONG_AS_DOUBLE_PLUS_ONE = 0x1p63;

  /**
   * Returns the {@code BigInteger} value that is equal to {@code x} rounded with the specified
   * rounding mode, if possible.
   *
   * @throws ArithmeticException if
   *         <ul>
   *         <li>{@code x} is infinite or NaN
   *         <li>{@code x} is not a mathematical integer and {@code mode} is
   *         {@link RoundingMode#UNNECESSARY}
   *         </ul>
   */
  public static BigInteger roundToBigInteger(double x, RoundingMode mode) {
    x = roundIntermediate(x, mode);
    if (MIN_LONG_AS_DOUBLE - x < 1.0 & x < MAX_LONG_AS_DOUBLE_PLUS_ONE) {
      return BigInteger.valueOf((long) x);
    }
    int exponent = getExponent(x);
    if (exponent < 0) {
      return BigInteger.ZERO;
    }
    long significand = getSignificand(x);
    BigInteger result = BigInteger.valueOf(significand).shiftLeft(exponent - SIGNIFICAND_BITS);
    return (x < 0) ? result.negate() : result;
  }

  /**
   * Returns {@code true} if {@code x} is exactly equal to {@code 2^k} for some finite integer
   * {@code k}.
   */
  public static boolean isPowerOfTwo(double x) {
    return x > 0.0 && isFinite(x) && LongMath.isPowerOfTwo(getSignificand(x));
  }

  /**
   * Returns the base 2 logarithm of a double value.
   *
   * <p>Special cases:
   * <ul>
   * <li>If {@code x} is NaN or less than zero, the result is NaN.
   * <li>If {@code x} is positive infinity, the result is positive infinity.
   * <li>If {@code x} is positive or negative zero, the result is negative infinity.
   * </ul>
   *
   * <p>The computed result must be within 1 ulp of the exact result.
   *
   * <p>If the result of this method will be immediately rounded to an {@code int},
   * {@link #log2(double, RoundingMode)} is faster.
   */
  public static double log2(double x) {
    return Math.log(x) / LN_2; // surprisingly within 1 ulp according to tests
  }

  private static final double LN_2 = Math.log(2);

  /**
   * Returns the base 2 logarithm of a double value, rounded with the specified rounding mode to an
   * {@code int}.
   *
   * <p>Regardless of the rounding mode, this is faster than {@code (int) log2(x)}.
   *
   * @throws IllegalArgumentException if {@code x <= 0.0}, {@code x} is NaN, or {@code x} is
   *         infinite
   */
  @SuppressWarnings("fallthrough")
  public static int log2(double x, RoundingMode mode) {
    checkArgument(x > 0.0 && isFinite(x), "x must be positive and finite");
    int exponent = getExponent(x);
    if (!isNormal(x)) {
      return log2(x * IMPLICIT_BIT, mode) - SIGNIFICAND_BITS;
      // Do the calculation on a normal value.
    }
    // x is positive, finite, and normal
    boolean increment;
    switch (mode) {
      case UNNECESSARY:
        checkRoundingUnnecessary(isPowerOfTwo(x));
        // fall through
      case FLOOR:
        increment = false;
        break;
      case CEILING:
        increment = !isPowerOfTwo(x);
        break;
      case DOWN:
        increment = exponent < 0 & !isPowerOfTwo(x);
        break;
      case UP:
        increment = exponent >= 0 & !isPowerOfTwo(x);
        break;
      case HALF_DOWN:
      case HALF_EVEN:
      case HALF_UP:
        double xScaled = scaleNormalize(x);
        // sqrt(2) is irrational, and the spec is relative to the "exact numerical result,"
        // so log2(x) is never exactly exponent + 0.5.
        increment = (xScaled * xScaled) > 2.0;
        break;
      default:
        throw new AssertionError();
    }
    return increment ? exponent + 1 : exponent;
  }

  /**
   * Returns {@code true} if {@code x} represents a mathematical integer.
   * 
   * <p>This is equivalent to, but not necessarily implemented as, the expression {@code
   * !Double.isNaN(x) && !Double.isInfinite(x) && x == Math.rint(x)}.
   */
  public static boolean isMathematicalInteger(double x) {
    return isFinite(x)
        && (x == 0.0 || SIGNIFICAND_BITS
            - Long.numberOfTrailingZeros(getSignificand(x)) <= getExponent(x));
  }

  /**
   * Returns {@code n!}, that is, the product of the first {@code n} positive
   * integers, {@code 1} if {@code n == 0}, or e n!}, or
   * {@link Double#POSITIVE_INFINITY} if {@code n! > Double.MAX_VALUE}.
   *
   * <p>The result is within 1 ulp of the true value.
   *
   * @throws IllegalArgumentException if {@code n < 0}
   */
  public static double factorial(int n) {
    checkNonNegative("n", n);
    if (n > MAX_FACTORIAL) {
      return Double.POSITIVE_INFINITY;
    } else {
      // Multiplying the last (n & 0xf) values into their own accumulator gives a more accurate
      // result than multiplying by EVERY_SIXTEENTH_FACTORIAL[n >> 4] directly.
      double accum = 1.0;
      for (int i = 1 + (n & ~0xf); i <= n; i++) {
        accum *= i;
      }
      return accum * EVERY_SIXTEENTH_FACTORIAL[n >> 4];
    }
  }

  @VisibleForTesting
  static final int MAX_FACTORIAL = 170;

  @VisibleForTesting
  static final double[] EVERY_SIXTEENTH_FACTORIAL = {
      0x1.0p0,
      0x1.30777758p44,
      0x1.956ad0aae33a4p117,
      0x1.ee69a78d72cb6p202,
      0x1.fe478ee34844ap295,
      0x1.c619094edabffp394,
      0x1.3638dd7bd6347p498,
      0x1.7cac197cfe503p605,
      0x1.1e5dfc140e1e5p716,
      0x1.8ce85fadb707ep829,
      0x1.95d5f3d928edep945};
}
