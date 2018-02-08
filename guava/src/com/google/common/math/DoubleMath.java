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

package com.google.common.math;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.math.DoubleUtils.IMPLICIT_BIT;
import static com.google.common.math.DoubleUtils.SIGNIFICAND_BITS;
import static com.google.common.math.DoubleUtils.getSignificand;
import static com.google.common.math.DoubleUtils.isFinite;
import static com.google.common.math.DoubleUtils.isNormal;
import static com.google.common.math.DoubleUtils.scaleNormalize;
import static com.google.common.math.MathPreconditions.checkInRange;
import static com.google.common.math.MathPreconditions.checkNonNegative;
import static com.google.common.math.MathPreconditions.checkRoundingUnnecessary;
import static java.lang.Math.abs;
import static java.lang.Math.copySign;
import static java.lang.Math.getExponent;
import static java.lang.Math.log;
import static java.lang.Math.rint;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.primitives.Booleans;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.Iterator;

/**
 * A class for arithmetic on doubles that is not covered by {@link java.lang.Math}.
 *
 * @author Louis Wasserman
 * @since 11.0
 */
@GwtCompatible(emulated = true)
public final class DoubleMath {
  /*
   * This method returns a value y such that rounding y DOWN (towards zero) gives the same result as
   * rounding x according to the specified mode.
   */
  @GwtIncompatible // #isMathematicalInteger, com.google.common.math.DoubleUtils
  static double roundIntermediate(double x, RoundingMode mode) {
    if (!isFinite(x)) {
      throw new ArithmeticException("input is infinite or NaN");
    }
    switch (mode) {
      case UNNECESSARY:
        checkRoundingUnnecessary(isMathematicalInteger(x));
        return x;

      case FLOOR:
        if (x >= 0.0 || isMathematicalInteger(x)) {
          return x;
        } else {
          return (long) x - 1;
        }

      case CEILING:
        if (x <= 0.0 || isMathematicalInteger(x)) {
          return x;
        } else {
          return (long) x + 1;
        }

      case DOWN:
        return x;

      case UP:
        if (isMathematicalInteger(x)) {
          return x;
        } else {
          return (long) x + (x > 0 ? 1 : -1);
        }

      case HALF_EVEN:
        return rint(x);

      case HALF_UP:
        {
          double z = rint(x);
          if (abs(x - z) == 0.5) {
            return x + copySign(0.5, x);
          } else {
            return z;
          }
        }

      case HALF_DOWN:
        {
          double z = rint(x);
          if (abs(x - z) == 0.5) {
            return x;
          } else {
            return z;
          }
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
   *     <ul>
   *       <li>{@code x} is infinite or NaN
   *       <li>{@code x}, after being rounded to a mathematical integer using the specified rounding
   *           mode, is either less than {@code Integer.MIN_VALUE} or greater than {@code
   *           Integer.MAX_VALUE}
   *       <li>{@code x} is not a mathematical integer and {@code mode} is {@link
   *           RoundingMode#UNNECESSARY}
   *     </ul>
   */
  @GwtIncompatible // #roundIntermediate
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
   *     <ul>
   *       <li>{@code x} is infinite or NaN
   *       <li>{@code x}, after being rounded to a mathematical integer using the specified rounding
   *           mode, is either less than {@code Long.MIN_VALUE} or greater than {@code
   *           Long.MAX_VALUE}
   *       <li>{@code x} is not a mathematical integer and {@code mode} is {@link
   *           RoundingMode#UNNECESSARY}
   *     </ul>
   */
  @GwtIncompatible // #roundIntermediate
  public static long roundToLong(double x, RoundingMode mode) {
    double z = roundIntermediate(x, mode);
    checkInRange(MIN_LONG_AS_DOUBLE - z < 1.0 & z < MAX_LONG_AS_DOUBLE_PLUS_ONE);
    return (long) z;
  }

  private static final double MIN_LONG_AS_DOUBLE = -0x1p63;
  /*
   * We cannot store Long.MAX_VALUE as a double without losing precision. Instead, we store
   * Long.MAX_VALUE + 1 == -Long.MIN_VALUE, and then offset all comparisons by 1.
   */
  private static final double MAX_LONG_AS_DOUBLE_PLUS_ONE = 0x1p63;

  /**
   * Returns the {@code BigInteger} value that is equal to {@code x} rounded with the specified
   * rounding mode, if possible.
   *
   * @throws ArithmeticException if
   *     <ul>
   *       <li>{@code x} is infinite or NaN
   *       <li>{@code x} is not a mathematical integer and {@code mode} is {@link
   *           RoundingMode#UNNECESSARY}
   *     </ul>
   */
  // #roundIntermediate, java.lang.Math.getExponent, com.google.common.math.DoubleUtils
  @GwtIncompatible
  public static BigInteger roundToBigInteger(double x, RoundingMode mode) {
    x = roundIntermediate(x, mode);
    if (MIN_LONG_AS_DOUBLE - x < 1.0 & x < MAX_LONG_AS_DOUBLE_PLUS_ONE) {
      return BigInteger.valueOf((long) x);
    }
    int exponent = getExponent(x);
    long significand = getSignificand(x);
    BigInteger result = BigInteger.valueOf(significand).shiftLeft(exponent - SIGNIFICAND_BITS);
    return (x < 0) ? result.negate() : result;
  }

  /**
   * Returns {@code true} if {@code x} is exactly equal to {@code 2^k} for some finite integer
   * {@code k}.
   */
  @GwtIncompatible // com.google.common.math.DoubleUtils
  public static boolean isPowerOfTwo(double x) {
    if (x > 0.0 && isFinite(x)) {
      long significand = getSignificand(x);
      return (significand & (significand - 1)) == 0;
    }
    return false;
  }

  /**
   * Returns the base 2 logarithm of a double value.
   *
   * <p>Special cases:
   *
   * <ul>
   *   <li>If {@code x} is NaN or less than zero, the result is NaN.
   *   <li>If {@code x} is positive infinity, the result is positive infinity.
   *   <li>If {@code x} is positive or negative zero, the result is negative infinity.
   * </ul>
   *
   * <p>The computed result is within 1 ulp of the exact result.
   *
   * <p>If the result of this method will be immediately rounded to an {@code int}, {@link
   * #log2(double, RoundingMode)} is faster.
   */
  public static double log2(double x) {
    return log(x) / LN_2; // surprisingly within 1 ulp according to tests
  }

  /**
   * Returns the base 2 logarithm of a double value, rounded with the specified rounding mode to an
   * {@code int}.
   *
   * <p>Regardless of the rounding mode, this is faster than {@code (int) log2(x)}.
   *
   * @throws IllegalArgumentException if {@code x <= 0.0}, {@code x} is NaN, or {@code x} is
   *     infinite
   */
  @GwtIncompatible // java.lang.Math.getExponent, com.google.common.math.DoubleUtils
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

  private static final double LN_2 = log(2);

  /**
   * Returns {@code true} if {@code x} represents a mathematical integer.
   *
   * <p>This is equivalent to, but not necessarily implemented as, the expression {@code
   * !Double.isNaN(x) && !Double.isInfinite(x) && x == Math.rint(x)}.
   */
  @GwtIncompatible // java.lang.Math.getExponent, com.google.common.math.DoubleUtils
  public static boolean isMathematicalInteger(double x) {
    return isFinite(x)
        && (x == 0.0
            || SIGNIFICAND_BITS - Long.numberOfTrailingZeros(getSignificand(x)) <= getExponent(x));
  }

  /**
   * Returns {@code n!}, that is, the product of the first {@code n} positive integers, {@code 1} if
   * {@code n == 0}, or {@code n!}, or {@link Double#POSITIVE_INFINITY} if {@code n! >
   * Double.MAX_VALUE}.
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
      // result than multiplying by everySixteenthFactorial[n >> 4] directly.
      double accum = 1.0;
      for (int i = 1 + (n & ~0xf); i <= n; i++) {
        accum *= i;
      }
      return accum * everySixteenthFactorial[n >> 4];
    }
  }

  @VisibleForTesting static final int MAX_FACTORIAL = 170;

  @VisibleForTesting
  static final double[] everySixteenthFactorial = {
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
    0x1.95d5f3d928edep945
  };

  /**
   * Returns {@code true} if {@code a} and {@code b} are within {@code tolerance} of each other.
   *
   * <p>Technically speaking, this is equivalent to {@code Math.abs(a - b) <= tolerance ||
   * Double.valueOf(a).equals(Double.valueOf(b))}.
   *
   * <p>Notable special cases include:
   *
   * <ul>
   *   <li>All NaNs are fuzzily equal.
   *   <li>If {@code a == b}, then {@code a} and {@code b} are always fuzzily equal.
   *   <li>Positive and negative zero are always fuzzily equal.
   *   <li>If {@code tolerance} is zero, and neither {@code a} nor {@code b} is NaN, then {@code a}
   *       and {@code b} are fuzzily equal if and only if {@code a == b}.
   *   <li>With {@link Double#POSITIVE_INFINITY} tolerance, all non-NaN values are fuzzily equal.
   *   <li>With finite tolerance, {@code Double.POSITIVE_INFINITY} and {@code
   *       Double.NEGATIVE_INFINITY} are fuzzily equal only to themselves.
   * </ul>
   *
   * <p>This is reflexive and symmetric, but <em>not</em> transitive, so it is <em>not</em> an
   * equivalence relation and <em>not</em> suitable for use in {@link Object#equals}
   * implementations.
   *
   * @throws IllegalArgumentException if {@code tolerance} is {@code < 0} or NaN
   * @since 13.0
   */
  public static boolean fuzzyEquals(double a, double b, double tolerance) {
    MathPreconditions.checkNonNegative("tolerance", tolerance);
    return Math.copySign(a - b, 1.0) <= tolerance
        // copySign(x, 1.0) is a branch-free version of abs(x), but with different NaN semantics
        || (a == b) // needed to ensure that infinities equal themselves
        || (Double.isNaN(a) && Double.isNaN(b));
  }

  /**
   * Compares {@code a} and {@code b} "fuzzily," with a tolerance for nearly-equal values.
   *
   * <p>This method is equivalent to {@code fuzzyEquals(a, b, tolerance) ? 0 : Double.compare(a,
   * b)}. In particular, like {@link Double#compare(double, double)}, it treats all NaN values as
   * equal and greater than all other values (including {@link Double#POSITIVE_INFINITY}).
   *
   * <p>This is <em>not</em> a total ordering and is <em>not</em> suitable for use in {@link
   * Comparable#compareTo} implementations. In particular, it is not transitive.
   *
   * @throws IllegalArgumentException if {@code tolerance} is {@code < 0} or NaN
   * @since 13.0
   */
  public static int fuzzyCompare(double a, double b, double tolerance) {
    if (fuzzyEquals(a, b, tolerance)) {
      return 0;
    } else if (a < b) {
      return -1;
    } else if (a > b) {
      return 1;
    } else {
      return Booleans.compare(Double.isNaN(a), Double.isNaN(b));
    }
  }

  /**
   * Returns the <a href="http://en.wikipedia.org/wiki/Arithmetic_mean">arithmetic mean</a> of
   * {@code values}.
   *
   * <p>If these values are a sample drawn from a population, this is also an unbiased estimator of
   * the arithmetic mean of the population.
   *
   * @param values a nonempty series of values
   * @throws IllegalArgumentException if {@code values} is empty or contains any non-finite value
   * @deprecated Use {@link Stats#meanOf} instead, noting the less strict handling of non-finite
   *     values.
   */
  @Deprecated
  // com.google.common.math.DoubleUtils
  @GwtIncompatible
  public static double mean(double... values) {
    checkArgument(values.length > 0, "Cannot take mean of 0 values");
    long count = 1;
    double mean = checkFinite(values[0]);
    for (int index = 1; index < values.length; ++index) {
      checkFinite(values[index]);
      count++;
      // Art of Computer Programming vol. 2, Knuth, 4.2.2, (15)
      mean += (values[index] - mean) / count;
    }
    return mean;
  }

  /**
   * Returns the <a href="http://en.wikipedia.org/wiki/Arithmetic_mean">arithmetic mean</a> of
   * {@code values}.
   *
   * <p>If these values are a sample drawn from a population, this is also an unbiased estimator of
   * the arithmetic mean of the population.
   *
   * @param values a nonempty series of values
   * @throws IllegalArgumentException if {@code values} is empty
   * @deprecated Use {@link Stats#meanOf} instead, noting the less strict handling of non-finite
   *     values.
   */
  @Deprecated
  public static double mean(int... values) {
    checkArgument(values.length > 0, "Cannot take mean of 0 values");
    // The upper bound on the the length of an array and the bounds on the int values mean that, in
    // this case only, we can compute the sum as a long without risking overflow or loss of
    // precision. So we do that, as it's slightly quicker than the Knuth algorithm.
    long sum = 0;
    for (int index = 0; index < values.length; ++index) {
      sum += values[index];
    }
    return (double) sum / values.length;
  }

  /**
   * Returns the <a href="http://en.wikipedia.org/wiki/Arithmetic_mean">arithmetic mean</a> of
   * {@code values}.
   *
   * <p>If these values are a sample drawn from a population, this is also an unbiased estimator of
   * the arithmetic mean of the population.
   *
   * @param values a nonempty series of values, which will be converted to {@code double} values
   *     (this may cause loss of precision for longs of magnitude over 2^53 (slightly over 9e15))
   * @throws IllegalArgumentException if {@code values} is empty
   * @deprecated Use {@link Stats#meanOf} instead, noting the less strict handling of non-finite
   *     values.
   */
  @Deprecated
  public static double mean(long... values) {
    checkArgument(values.length > 0, "Cannot take mean of 0 values");
    long count = 1;
    double mean = values[0];
    for (int index = 1; index < values.length; ++index) {
      count++;
      // Art of Computer Programming vol. 2, Knuth, 4.2.2, (15)
      mean += (values[index] - mean) / count;
    }
    return mean;
  }

  /**
   * Returns the <a href="http://en.wikipedia.org/wiki/Arithmetic_mean">arithmetic mean</a> of
   * {@code values}.
   *
   * <p>If these values are a sample drawn from a population, this is also an unbiased estimator of
   * the arithmetic mean of the population.
   *
   * @param values a nonempty series of values, which will be converted to {@code double} values
   *     (this may cause loss of precision)
   * @throws IllegalArgumentException if {@code values} is empty or contains any non-finite value
   * @deprecated Use {@link Stats#meanOf} instead, noting the less strict handling of non-finite
   *     values.
   */
  @Deprecated
  // com.google.common.math.DoubleUtils
  @GwtIncompatible
  public static double mean(Iterable<? extends Number> values) {
    return mean(values.iterator());
  }

  /**
   * Returns the <a href="http://en.wikipedia.org/wiki/Arithmetic_mean">arithmetic mean</a> of
   * {@code values}.
   *
   * <p>If these values are a sample drawn from a population, this is also an unbiased estimator of
   * the arithmetic mean of the population.
   *
   * @param values a nonempty series of values, which will be converted to {@code double} values
   *     (this may cause loss of precision)
   * @throws IllegalArgumentException if {@code values} is empty or contains any non-finite value
   * @deprecated Use {@link Stats#meanOf} instead, noting the less strict handling of non-finite
   *     values.
   */
  @Deprecated
  // com.google.common.math.DoubleUtils
  @GwtIncompatible
  public static double mean(Iterator<? extends Number> values) {
    checkArgument(values.hasNext(), "Cannot take mean of 0 values");
    long count = 1;
    double mean = checkFinite(values.next().doubleValue());
    while (values.hasNext()) {
      double value = checkFinite(values.next().doubleValue());
      count++;
      // Art of Computer Programming vol. 2, Knuth, 4.2.2, (15)
      mean += (value - mean) / count;
    }
    return mean;
  }

  @GwtIncompatible // com.google.common.math.DoubleUtils
  @CanIgnoreReturnValue
  private static double checkFinite(double argument) {
    checkArgument(isFinite(argument));
    return argument;
  }

  private DoubleMath() {}
}
