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

import com.google.common.annotations.VisibleForTesting;

import java.math.BigDecimal;
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
 */
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

  /**
   * Returns the base-10 logarithm of {@code x}, rounded according to the specified rounding mode.
   *
   * @throws IllegalArgumentException if {@code x <= 0}
   * @throws ArithmeticException if {@code mode} is {@link RoundingMode#UNNECESSARY} and {@code x}
   *         is not a power of ten
   */
  @SuppressWarnings("fallthrough")
  public static int log10(BigInteger x, RoundingMode mode) {
    checkPositive("x", x);
    if (fitsInLong(x)) {
      return LongMath.log10(x.longValue(), mode);
    }

    // capacity of 10 suffices for all x <= 10^(2^10).
    List<BigInteger> powersOf10 = new ArrayList<BigInteger>(10);
    BigInteger powerOf10 = BigInteger.TEN;
    while (x.compareTo(powerOf10) >= 0) {
      powersOf10.add(powerOf10);
      powerOf10 = powerOf10.pow(2);
    }
    BigInteger floorPow = BigInteger.ONE;
    int floorLog = 0;
    for (int i = powersOf10.size() - 1; i >= 0; i--) {
      BigInteger powOf10 = powersOf10.get(i);
      floorLog *= 2;
      BigInteger tenPow = powOf10.multiply(floorPow);
      if (x.compareTo(tenPow) >= 0) {
        floorPow = tenPow;
        floorLog++;
      }
    }
    switch (mode) {
      case UNNECESSARY:
        checkRoundingUnnecessary(floorPow.equals(x));
        // fall through
      case FLOOR:
      case DOWN:
        return floorLog;

      case CEILING:
      case UP:
        return floorPow.equals(x) ? floorLog : floorLog + 1;

      case HALF_DOWN:
      case HALF_UP:
      case HALF_EVEN:
        // Since sqrt(10) is irrational, log10(x) - floorLog can never be exactly 0.5
        BigInteger x2 = x.pow(2);
        BigInteger halfPowerSquared = floorPow.pow(2).multiply(BigInteger.TEN);
        return (x2.compareTo(halfPowerSquared) <= 0) ? floorLog : floorLog + 1;
      default:
        throw new AssertionError();
    }
  }

  /**
   * Returns the square root of {@code x}, rounded with the specified rounding mode.
   *
   * @throws IllegalArgumentException if {@code x < 0}
   * @throws ArithmeticException if {@code mode} is {@link RoundingMode#UNNECESSARY} and
   *         {@code sqrt(x)} is not an integer
   */
  @SuppressWarnings("fallthrough")
  public static BigInteger sqrt(BigInteger x, RoundingMode mode) {
    checkNonNegative("x", x);
    if (fitsInLong(x)) {
      return BigInteger.valueOf(LongMath.sqrt(x.longValue(), mode));
    }
    BigInteger sqrtFloor = sqrtFloor(x);
    switch (mode) {
      case UNNECESSARY:
        checkRoundingUnnecessary(sqrtFloor.pow(2).equals(x)); // fall through
      case FLOOR:
      case DOWN:
        return sqrtFloor;
      case CEILING:
      case UP:
        return sqrtFloor.pow(2).equals(x) ? sqrtFloor : sqrtFloor.add(BigInteger.ONE);
      case HALF_DOWN:
      case HALF_UP:
      case HALF_EVEN:
        BigInteger halfSquare = sqrtFloor.pow(2).add(sqrtFloor);
        /*
         * We wish to test whether or not x <= (sqrtFloor + 0.5)^2 = halfSquare + 0.25. Since both
         * x and halfSquare are integers, this is equivalent to testing whether or not x <=
         * halfSquare.
         */
        return (halfSquare.compareTo(x) >= 0) ? sqrtFloor : sqrtFloor.add(BigInteger.ONE);
      default:
        throw new AssertionError();
    }
  }

  private static BigInteger sqrtFloor(BigInteger x) {
    // Hackers's Delight, Figure 11-1
    int s = (log2(x, CEILING) + 1) >> 1;
    BigInteger sqrt0 = BigInteger.ZERO.setBit(s);
    BigInteger sqrt1 = sqrt0.add(x.shiftRight(s)).shiftRight(1);
    while (sqrt1.compareTo(sqrt0) < 0) {
      sqrt0 = sqrt1;
      sqrt1 = sqrt0.add(x.divide(sqrt0)).shiftRight(1);
    }
    return sqrt0;
  }

  /**
   * Returns the result of dividing {@code p} by {@code q}, rounding using the specified
   * {@code RoundingMode}.
   *
   * @throws ArithmeticException if {@code q == 0}, or if {@code mode == UNNECESSARY} and {@code a}
   *         is not an integer multiple of {@code b}
   */
  public static BigInteger divide(BigInteger p, BigInteger q, RoundingMode mode){
    BigDecimal pDec = new BigDecimal(p);
    BigDecimal qDec = new BigDecimal(q);
    return pDec.divide(qDec, 0, mode).toBigIntegerExact();
  }

  /**
   * Returns {@code n!}. Warning: the result takes <i>O(n log n)</i> memory.
   *
   * <p><b>Warning</b>: the result takes <i>O(n log n)</i> space, so use cautiously.
   *
   * @throws IllegalArgumentException if {@code n < 0}
   */
  public static BigInteger factorial(int n) {
    checkNonNegative("n", n);
    if (n < LongMath.FACTORIALS.length) {
      return BigInteger.valueOf(LongMath.factorial(n));
    } else {
      int k = LongMath.FACTORIALS.length - 1;
      return BigInteger.valueOf(LongMath.factorial(k)).multiply(factorial(k, n));
    }
  }

  /**
   * Returns the product of {@code n1} exclusive through {@code n2} inclusive.
   */
  private static BigInteger factorial(int n1, int n2) {
    assert n1 <= n2;
    if (IntMath.log2(n2, CEILING) * (n2 - n1) < Long.SIZE - 1) {
      // the result will definitely fit into a long
      long result = 1;
      for (int i = n1 + 1; i <= n2; i++) {
        result *= i;
      }
      return BigInteger.valueOf(result);
    }

    /*
     * We want each multiplication to have both sides with approximately the same number of digits.
     * Currently, we just divide the range in half.
     */
    int mid = (n1 + n2) >>> 1;
    return factorial(n1, mid).multiply(factorial(mid, n2));
  }

  /**
   * Returns {@code n} choose {@code k}, also known as the binomial coefficient of {@code n} and
   * {@code k}.
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
    if (k < LongMath.BIGGEST_BINOMIALS.length && n <= LongMath.BIGGEST_BINOMIALS[k]) {
      return BigInteger.valueOf(LongMath.binomial(n, k));
    }
    BigInteger result = BigInteger.ONE;
    for (int i = 0; i < k; i++) {
      result = result.multiply(BigInteger.valueOf(n - i));
      result = result.divide(BigInteger.valueOf(i + 1));
    }
    return result;
  }

  // Returns true if BigInteger.valueOf(x.longValue()).equals(x).
  static boolean fitsInLong(BigInteger x) {
    return x.bitLength() <= Long.SIZE - 1;
  }

  private BigIntegerMath() {}
}
