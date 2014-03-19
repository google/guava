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

import static com.google.common.math.MathPreconditions.checkNonNegative;
import static java.lang.Math.log;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.primitives.Booleans;

/**
 * A class for arithmetic on doubles that is not covered by {@link java.lang.Math}.
 *
 * @author Louis Wasserman
 * @since 11.0
 */
@GwtCompatible(emulated = true)
public final class DoubleMath {
  /*
   * This method returns a value y such that rounding y DOWN (towards zero) gives the same result
   * as rounding x according to the specified mode.
   */

  private static final double MIN_INT_AS_DOUBLE = -0x1p31;
  private static final double MAX_INT_AS_DOUBLE = 0x1p31 - 1.0;

  private static final double MIN_LONG_AS_DOUBLE = -0x1p63;
  /*
   * We cannot store Long.MAX_VALUE as a double without losing precision.  Instead, we store
   * Long.MAX_VALUE + 1 == -Long.MIN_VALUE, and then offset all comparisons by 1.
   */
  private static final double MAX_LONG_AS_DOUBLE_PLUS_ONE = 0x1p63;

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
   * <p>The computed result is within 1 ulp of the exact result.
   *
   * <p>If the result of this method will be immediately rounded to an {@code int},
   * {@link #log2(double, RoundingMode)} is faster.
   */
  public static double log2(double x) {
    return log(x) / LN_2; // surprisingly within 1 ulp according to tests
  }

  private static final double LN_2 = log(2);

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
      // result than multiplying by everySixteenthFactorial[n >> 4] directly.
      double accum = 1.0;
      for (int i = 1 + (n & ~0xf); i <= n; i++) {
        accum *= i;
      }
      return accum * everySixteenthFactorial[n >> 4];
    }
  }

  @VisibleForTesting
  static final int MAX_FACTORIAL = 170;

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
      0x1.95d5f3d928edep945};

  /**
   * Returns {@code true} if {@code a} and {@code b} are within {@code tolerance} of each other.
   *
   * <p>Technically speaking, this is equivalent to
   * {@code Math.abs(a - b) <= tolerance || Double.valueOf(a).equals(Double.valueOf(b))}.
   *
   * <p>Notable special cases include:
   * <ul>
   * <li>All NaNs are fuzzily equal.
   * <li>If {@code a == b}, then {@code a} and {@code b} are always fuzzily equal.
   * <li>Positive and negative zero are always fuzzily equal.
   * <li>If {@code tolerance} is zero, and neither {@code a} nor {@code b} is NaN, then
   * {@code a} and {@code b} are fuzzily equal if and only if {@code a == b}.
   * <li>With {@link Double#POSITIVE_INFINITY} tolerance, all non-NaN values are fuzzily equal.
   * <li>With finite tolerance, {@code Double.POSITIVE_INFINITY} and {@code
   * Double.NEGATIVE_INFINITY} are fuzzily equal only to themselves.
   * </li>
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
    return
          Math.copySign(a - b, 1.0) <= tolerance
           // copySign(x, 1.0) is a branch-free version of abs(x), but with different NaN semantics
          || (a == b) // needed to ensure that infinities equal themselves
          || (Double.isNaN(a) && Double.isNaN(b));
  }

  /**
   * Compares {@code a} and {@code b} "fuzzily," with a tolerance for nearly-equal values.
   *
   * <p>This method is equivalent to
   * {@code fuzzyEquals(a, b, tolerance) ? 0 : Double.compare(a, b)}. In particular, like
   * {@link Double#compare(double, double)}, it treats all NaN values as equal and greater than all
   * other values (including {@link Double#POSITIVE_INFINITY}).
   *
   * <p>This is <em>not</em> a total ordering and is <em>not</em> suitable for use in
   * {@link Comparable#compareTo} implementations.  In particular, it is not transitive.
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

  private DoubleMath() {}
}

