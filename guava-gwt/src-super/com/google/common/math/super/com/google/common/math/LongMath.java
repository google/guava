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
import static com.google.common.math.MathPreconditions.checkNonNegative;
import static com.google.common.math.MathPreconditions.checkPositive;
import static com.google.common.math.MathPreconditions.checkRoundingUnnecessary;
import static java.math.RoundingMode.HALF_EVEN;
import static java.math.RoundingMode.HALF_UP;

import com.google.common.annotations.Beta;
import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.VisibleForTesting;

import java.math.RoundingMode;

/**
 * A class for arithmetic on values of type {@code long}. Where possible, methods are defined and
 * named analogously to their {@code BigInteger} counterparts.
 *
 * <p>The implementations of many methods in this class are based on material from Henry S. Warren,
 * Jr.'s <i>Hacker's Delight</i>, (Addison Wesley, 2002).
 *
 * <p>Similar functionality for {@code int} and for {@link BigInteger} can be found in
 * {@link IntMath} and {@link BigIntegerMath} respectively.  For other common operations on
 * {@code long} values, see {@link com.google.common.primitives.Longs}.
 *
 * @author Louis Wasserman
 * @since 11.0
 */
@Beta
@GwtCompatible(emulated = true)
public final class LongMath {
  // NOTE: Whenever both tests are cheap and functional, it's faster to use &, | instead of &&, ||

  /**
   * Returns {@code true} if {@code x} represents a power of two.
   *
   * <p>This differs from {@code Long.bitCount(x) == 1}, because
   * {@code Long.bitCount(Long.MIN_VALUE) == 1}, but {@link Long#MIN_VALUE} is not a power of two.
   */
  public static boolean isPowerOfTwo(long x) {
    return x > 0 & (x & (x - 1)) == 0;
  }

  /**
   * Returns the base-2 logarithm of {@code x}, rounded according to the specified rounding mode.
   *
   * @throws IllegalArgumentException if {@code x <= 0}
   * @throws ArithmeticException if {@code mode} is {@link RoundingMode#UNNECESSARY} and {@code x}
   *         is not a power of two
   */
  @SuppressWarnings("fallthrough")
  public static int log2(long x, RoundingMode mode) {
    checkPositive("x", x);
    switch (mode) {
      case UNNECESSARY:
        checkRoundingUnnecessary(isPowerOfTwo(x));
        // fall through
      case DOWN:
      case FLOOR:
        return (Long.SIZE - 1) - Long.numberOfLeadingZeros(x);

      case UP:
      case CEILING:
        return Long.SIZE - Long.numberOfLeadingZeros(x - 1);

      case HALF_DOWN:
      case HALF_UP:
      case HALF_EVEN:
        // Since sqrt(2) is irrational, log2(x) - logFloor cannot be exactly 0.5
        int leadingZeros = Long.numberOfLeadingZeros(x);
        long cmp = MAX_POWER_OF_SQRT2_UNSIGNED >>> leadingZeros;
        // floor(2^(logFloor + 0.5))
        int logFloor = (Long.SIZE - 1) - leadingZeros;
        return (x <= cmp) ? logFloor : logFloor + 1;

      default:
        throw new AssertionError("impossible");
    }
  }

  /** The biggest half power of two that fits into an unsigned long */
  @VisibleForTesting static final long MAX_POWER_OF_SQRT2_UNSIGNED = 0xB504F333F9DE6484L;

  // MAX_LOG10_FOR_LEADING_ZEROS[i] == floor(log10(2^(Long.SIZE - i)))
  @VisibleForTesting static final byte[] MAX_LOG10_FOR_LEADING_ZEROS = {
      19, 18, 18, 18, 18, 17, 17, 17, 16, 16, 16, 15, 15, 15, 15, 14, 14, 14, 13, 13, 13, 12, 12,
      12, 12, 11, 11, 11, 10, 10, 10, 9, 9, 9, 9, 8, 8, 8, 7, 7, 7, 6, 6, 6, 6, 5, 5, 5, 4, 4, 4,
      3, 3, 3, 3, 2, 2, 2, 1, 1, 1, 0, 0, 0 };

  // HALF_POWERS_OF_10[i] = largest long less than 10^(i + 0.5)

  static final long[] FACTORIALS = {
      1L,
      1L,
      1L * 2,
      1L * 2 * 3,
      1L * 2 * 3 * 4,
      1L * 2 * 3 * 4 * 5,
      1L * 2 * 3 * 4 * 5 * 6,
      1L * 2 * 3 * 4 * 5 * 6 * 7,
      1L * 2 * 3 * 4 * 5 * 6 * 7 * 8,
      1L * 2 * 3 * 4 * 5 * 6 * 7 * 8 * 9,
      1L * 2 * 3 * 4 * 5 * 6 * 7 * 8 * 9 * 10,
      1L * 2 * 3 * 4 * 5 * 6 * 7 * 8 * 9 * 10 * 11,
      1L * 2 * 3 * 4 * 5 * 6 * 7 * 8 * 9 * 10 * 11 * 12,
      1L * 2 * 3 * 4 * 5 * 6 * 7 * 8 * 9 * 10 * 11 * 12 * 13,
      1L * 2 * 3 * 4 * 5 * 6 * 7 * 8 * 9 * 10 * 11 * 12 * 13 * 14,
      1L * 2 * 3 * 4 * 5 * 6 * 7 * 8 * 9 * 10 * 11 * 12 * 13 * 14 * 15,
      1L * 2 * 3 * 4 * 5 * 6 * 7 * 8 * 9 * 10 * 11 * 12 * 13 * 14 * 15 * 16,
      1L * 2 * 3 * 4 * 5 * 6 * 7 * 8 * 9 * 10 * 11 * 12 * 13 * 14 * 15 * 16 * 17,
      1L * 2 * 3 * 4 * 5 * 6 * 7 * 8 * 9 * 10 * 11 * 12 * 13 * 14 * 15 * 16 * 17 * 18,
      1L * 2 * 3 * 4 * 5 * 6 * 7 * 8 * 9 * 10 * 11 * 12 * 13 * 14 * 15 * 16 * 17 * 18 * 19,
      1L * 2 * 3 * 4 * 5 * 6 * 7 * 8 * 9 * 10 * 11 * 12 * 13 * 14 * 15 * 16 * 17 * 18 * 19 * 20
  };

  /**
   * Returns {@code n} choose {@code k}, also known as the binomial coefficient of {@code n} and
   * {@code k}, or {@link Long#MAX_VALUE} if the result does not fit in a {@code long}.
   *
   * @throws IllegalArgumentException if {@code n < 0}, {@code k < 0}, or {@code k > n}
   */
  public static long binomial(int n, int k) {
    checkNonNegative("n", n);
    checkNonNegative("k", k);
    checkArgument(k <= n, "k (%s) > n (%s)", k, n);
    if (k > (n >> 1)) {
      k = n - k;
    }
    if (k >= BIGGEST_BINOMIALS.length || n > BIGGEST_BINOMIALS[k]) {
      return Long.MAX_VALUE;
    }
    long result = 1;
    if (k < BIGGEST_SIMPLE_BINOMIALS.length && n <= BIGGEST_SIMPLE_BINOMIALS[k]) {
      // guaranteed not to overflow
      for (int i = 0; i < k; i++) {
        result *= n - i;
        result /= i + 1;
      }
    } else {
      // We want to do this in long math for speed, but want to avoid overflow.
      // Dividing by the GCD suffices to avoid overflow in all the remaining cases.
      for (int i = 1; i <= k; i++, n--) {
        int d = IntMath.gcd(n, i);
        result /= i / d; // (i/d) is guaranteed to divide result
        result *= n / d;
      }
    }
    return result;
  }

  /*
   * binomial(BIGGEST_BINOMIALS[k], k) fits in a long, but not
   * binomial(BIGGEST_BINOMIALS[k] + 1, k).
   */
  static final int[] BIGGEST_BINOMIALS =
      {Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE, 3810779, 121977, 16175, 4337, 1733,
          887, 534, 361, 265, 206, 169, 143, 125, 111, 101, 94, 88, 83, 79, 76, 74, 72, 70, 69, 68,
          67, 67, 66, 66, 66, 66};

  /*
   * binomial(BIGGEST_SIMPLE_BINOMIALS[k], k) doesn't need to use the slower GCD-based impl,
   * but binomial(BIGGEST_SIMPLE_BINOMIALS[k] + 1, k) does.
   */
  @VisibleForTesting static final int[] BIGGEST_SIMPLE_BINOMIALS =
      {Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE, 2642246, 86251, 11724, 3218, 1313,
          684, 419, 287, 214, 169, 139, 119, 105, 95, 87, 81, 76, 73, 70, 68, 66, 64, 63, 62, 62,
          61, 61, 61};
  // These values were generated by using checkedMultiply to see when the simple multiply/divide
  // algorithm would lead to an overflow.

  private LongMath() {}
}
