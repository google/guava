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
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.math.MathPreconditions.checkNonNegative;
import static com.google.common.math.MathPreconditions.checkPositive;
import static com.google.common.math.MathPreconditions.checkRoundingUnnecessary;
import static java.math.RoundingMode.CEILING;
import static java.math.RoundingMode.FLOOR;
import static java.math.RoundingMode.HALF_EVEN;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.VisibleForTesting;

import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * A class for arithmetic on values of type {@code BigInteger}.
 *
 * <p>The implementations of many methods in this class are based on material from Henry S. Warren,
 * Jr.'s <i>Hacker's Delight</i>, (Addison Wesley, 2002).
 *
 * <p>Similar functionality for {@code int} and for {@code long} can be found in
 * {@link IntMath} and {@link LongMath} respectively.
 *
 * @author Louis Wasserman
 * @since 11.0
 */
@GwtCompatible(emulated = true)
public final class BigIntegerMath {
  /**
   * Returns {@code true} if {@code x} represents a power of two.
   */
  public static boolean isPowerOfTwo(BigInteger x) {
    checkNotNull(x);
    return x.signum() > 0 && x.getLowestSetBit() == x.bitLength() - 1;
  }

  /**
   * Returns the base-2 logarithm of {@code x}, rounded according to the specified rounding mode.
   *
   * @throws IllegalArgumentException if {@code x <= 0}
   * @throws ArithmeticException if {@code mode} is {@link RoundingMode#UNNECESSARY} and {@code x}
   *         is not a power of two
   */
  @SuppressWarnings("fallthrough")
  // TODO(kevinb): remove after this warning is disabled globally
  public static int log2(BigInteger x, RoundingMode mode) {
    checkPositive("x", checkNotNull(x));
    int logFloor = x.bitLength() - 1;
    switch (mode) {
      case UNNECESSARY:
        checkRoundingUnnecessary(isPowerOfTwo(x)); // fall through
      case DOWN:
      case FLOOR:
        return logFloor;

      case UP:
      case CEILING:
        return isPowerOfTwo(x) ? logFloor : logFloor + 1;

      case HALF_DOWN:
      case HALF_UP:
      case HALF_EVEN:
        if (logFloor < SQRT2_PRECOMPUTE_THRESHOLD) {
          BigInteger halfPower = SQRT2_PRECOMPUTED_BITS.shiftRight(
              SQRT2_PRECOMPUTE_THRESHOLD - logFloor);
          if (x.compareTo(halfPower) <= 0) {
            return logFloor;
          } else {
            return logFloor + 1;
          }
        }
        /*
         * Since sqrt(2) is irrational, log2(x) - logFloor cannot be exactly 0.5
         *
         * To determine which side of logFloor.5 the logarithm is, we compare x^2 to 2^(2 *
         * logFloor + 1).
         */
        BigInteger x2 = x.pow(2);
        int logX2Floor = x2.bitLength() - 1;
        return (logX2Floor < 2 * logFloor + 1) ? logFloor : logFloor + 1;

      default:
        throw new AssertionError();
    }
  }

  /*
   * The maximum number of bits in a square root for which we'll precompute an explicit half power
   * of two. This can be any value, but higher values incur more class load time and linearly
   * increasing memory consumption.
   */
  @VisibleForTesting static final int SQRT2_PRECOMPUTE_THRESHOLD = 256;

  @VisibleForTesting static final BigInteger SQRT2_PRECOMPUTED_BITS =
      new BigInteger("16a09e667f3bcc908b2fb1366ea957d3e3adec17512775099da2f590b0667322a", 16);

  private static final double LN_10 = Math.log(10);
  private static final double LN_2 = Math.log(2);

  /**
   * Returns {@code n!}, that is, the product of the first {@code n} positive
   * integers, or {@code 1} if {@code n == 0}.
   *
   * <p><b>Warning</b>: the result takes <i>O(n log n)</i> space, so use cautiously.
   *
   * <p>This uses an efficient binary recursive algorithm to compute the factorial
   * with balanced multiplies.  It also removes all the 2s from the intermediate
   * products (shifting them back in at the end).
   *
   * @throws IllegalArgumentException if {@code n < 0}
   */
  public static BigInteger factorial(int n) {
    checkNonNegative("n", n);

    // If the factorial is small enough, just use LongMath to do it.
    if (n < LongMath.factorials.length) {
      return BigInteger.valueOf(LongMath.factorials[n]);
    }

    // Pre-allocate space for our list of intermediate BigIntegers.
    int approxSize = IntMath.divide(n * IntMath.log2(n, CEILING), Long.SIZE, CEILING);
    ArrayList<BigInteger> bignums = new ArrayList<BigInteger>(approxSize);

    // Start from the pre-computed maximum long factorial.
    int startingNumber = LongMath.factorials.length;
    long product = LongMath.factorials[startingNumber - 1];
    // Strip off 2s from this value.
    int shift = Long.numberOfTrailingZeros(product);
    product >>= shift;

    // Use floor(log2(num)) + 1 to prevent overflow of multiplication.
    int productBits = LongMath.log2(product, FLOOR) + 1;
    int bits = LongMath.log2(startingNumber, FLOOR) + 1;
    // Check for the next power of two boundary, to save us a CLZ operation.
    int nextPowerOfTwo = 1 << (bits - 1);

    // Iteratively multiply the longs as big as they can go.
    for (long num = startingNumber; num <= n; num++) {
      // Check to see if the floor(log2(num)) + 1 has changed.
      if ((num & nextPowerOfTwo) != 0) {
        nextPowerOfTwo <<= 1;
        bits++;
      }
      // Get rid of the 2s in num.
      int tz = Long.numberOfTrailingZeros(num);
      long normalizedNum = num >> tz;
      shift += tz;
      // Adjust floor(log2(num)) + 1.
      int normalizedBits = bits - tz;
      // If it won't fit in a long, then we store off the intermediate product.
      if (normalizedBits + productBits >= Long.SIZE) {
        bignums.add(BigInteger.valueOf(product));
        product = 1;
        productBits = 0;
      }
      product *= normalizedNum;
      productBits = LongMath.log2(product, FLOOR) + 1;
    }
    // Check for leftovers.
    if (product > 1) {
      bignums.add(BigInteger.valueOf(product));
    }
    // Efficiently multiply all the intermediate products together.
    return listProduct(bignums).shiftLeft(shift);
  }

  static BigInteger listProduct(List<BigInteger> nums) {
    return listProduct(nums, 0, nums.size());
  }

  static BigInteger listProduct(List<BigInteger> nums, int start, int end) {
    switch (end - start) {
      case 0:
        return BigInteger.ONE;
      case 1:
        return nums.get(start);
      case 2:
        return nums.get(start).multiply(nums.get(start + 1));
      case 3:
        return nums.get(start).multiply(nums.get(start + 1)).multiply(nums.get(start + 2));
      default:
        // Otherwise, split the list in half and recursively do this.
        int m = (end + start) >>> 1;
        return listProduct(nums, start, m).multiply(listProduct(nums, m, end));
    }
  }

 /**
   * Returns {@code n} choose {@code k}, also known as the binomial coefficient of {@code n} and
   * {@code k}, that is, {@code n! / (k! (n - k)!)}.
   *
   * <p><b>Warning</b>: the result can take as much as <i>O(k log n)</i> space.
   *
   * @throws IllegalArgumentException if {@code n < 0}, {@code k < 0}, or {@code k > n}
   */
  public static BigInteger binomial(int n, int k) {
    checkNonNegative("n", n);
    checkNonNegative("k", k);
    checkArgument(k <= n, "k (%s) > n (%s)", k, n);
    if (k > (n >> 1)) {
      k = n - k;
    }
    if (k < LongMath.biggestBinomials.length && n <= LongMath.biggestBinomials[k]) {
      return BigInteger.valueOf(LongMath.binomial(n, k));
    }

    BigInteger accum = BigInteger.ONE;

    long numeratorAccum = n;
    long denominatorAccum = 1;

    int bits = LongMath.log2(n, RoundingMode.CEILING);

    int numeratorBits = bits;

    for (int i = 1; i < k; i++) {
      int p = n - i;
      int q = i + 1;

      // log2(p) >= bits - 1, because p >= n/2

      if (numeratorBits + bits >= Long.SIZE - 1) {
        // The numerator is as big as it can get without risking overflow.
        // Multiply numeratorAccum / denominatorAccum into accum.
        accum = accum
            .multiply(BigInteger.valueOf(numeratorAccum))
            .divide(BigInteger.valueOf(denominatorAccum));
        numeratorAccum = p;
        denominatorAccum = q;
        numeratorBits = bits;
      } else {
        // We can definitely multiply into the long accumulators without overflowing them.
        numeratorAccum *= p;
        denominatorAccum *= q;
        numeratorBits += bits;
      }
    }
    return accum
        .multiply(BigInteger.valueOf(numeratorAccum))
        .divide(BigInteger.valueOf(denominatorAccum));
  }

  // Returns true if BigInteger.valueOf(x.longValue()).equals(x).

  private BigIntegerMath() {}
}

