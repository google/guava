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

import static com.google.common.math.MathTesting.ALL_LONG_CANDIDATES;
import static com.google.common.math.MathTesting.ALL_ROUNDING_MODES;
import static com.google.common.math.MathTesting.ALL_SAFE_ROUNDING_MODES;
import static com.google.common.math.MathTesting.NEGATIVE_INTEGER_CANDIDATES;
import static com.google.common.math.MathTesting.NEGATIVE_LONG_CANDIDATES;
import static com.google.common.math.MathTesting.POSITIVE_LONG_CANDIDATES;
import static java.math.BigInteger.valueOf;
import static java.math.RoundingMode.UNNECESSARY;

import com.google.common.annotations.GwtCompatible;

import junit.framework.TestCase;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;

/**
 * Tests for LongMath.
 *
 * @author Louis Wasserman
 */
@GwtCompatible(emulated = true)
public class LongMathTest extends TestCase {
  
  public void testLessThanBranchFree() {
    for (long x : ALL_LONG_CANDIDATES) {
      for (long y : ALL_LONG_CANDIDATES) {
        BigInteger difference = BigInteger.valueOf(x).subtract(BigInteger.valueOf(y));
        if (fitsInLong(difference)) {
          int expected = (x < y) ? 1 : 0;
          int actual = LongMath.lessThanBranchFree(x, y);
          assertEquals(expected, actual);
        }
      }
    }
  }

  // Throws an ArithmeticException if "the simple implementation" of binomial coefficients overflows

  public void testLog2ZeroAlwaysThrows() {
    for (RoundingMode mode : ALL_ROUNDING_MODES) {
      try {
        LongMath.log2(0L, mode);
        fail("Expected IllegalArgumentException");
      } catch (IllegalArgumentException expected) {}
    }
  }

  public void testLog2NegativeAlwaysThrows() {
    for (long x : NEGATIVE_LONG_CANDIDATES) {
      for (RoundingMode mode : ALL_ROUNDING_MODES) {
        try {
          LongMath.log2(x, mode);
          fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException expected) {}
      }
    }
  }

  /* Relies on the correctness of BigIntegerMath.log2 for all modes except UNNECESSARY. */
  public void testLog2MatchesBigInteger() {
    for (long x : POSITIVE_LONG_CANDIDATES) {
      for (RoundingMode mode : ALL_SAFE_ROUNDING_MODES) {
        // The BigInteger implementation is tested separately, use it as the reference.
        assertEquals(BigIntegerMath.log2(valueOf(x), mode), LongMath.log2(x, mode));
      }
    }
  }

  /* Relies on the correctness of isPowerOfTwo(long). */
  public void testLog2Exact() {
    for (long x : POSITIVE_LONG_CANDIDATES) {
      // We only expect an exception if x was not a power of 2.
      boolean isPowerOf2 = LongMath.isPowerOfTwo(x);
      try {
        assertEquals(x, 1L << LongMath.log2(x, UNNECESSARY));
        assertTrue(isPowerOf2);
      } catch (ArithmeticException e) {
        assertFalse(isPowerOf2);
      }
    }
  }

  // Relies on the correctness of BigIntegerMath.log10 for all modes except UNNECESSARY.

  // Relies on the correctness of log10(long, FLOOR) and of pow(long, int).

  // Relies on the correctness of BigIntegerMath.sqrt for all modes except UNNECESSARY.

  /* Relies on the correctness of sqrt(long, FLOOR). */

  public void testGCDExhaustive() {
    for (long a : POSITIVE_LONG_CANDIDATES) {
      for (long b : POSITIVE_LONG_CANDIDATES) {
        assertEquals(valueOf(a).gcd(valueOf(b)), valueOf(LongMath.gcd(a, b)));
      }
    }
  }

  // Depends on the correctness of BigIntegerMath.factorial.

  // Depends on the correctness of BigIntegerMath.binomial.
  public void testBinomial() {
    for (int n = 0; n <= 70; n++) {
      for (int k = 0; k <= n; k++) {
        BigInteger expectedBig = BigIntegerMath.binomial(n, k);
        long expectedLong = fitsInLong(expectedBig) ? expectedBig.longValue() : Long.MAX_VALUE;
        assertEquals(expectedLong, LongMath.binomial(n, k));
      }
    }
  }

  public void testBinomialOutside() {
    for (int n = 0; n <= 50; n++) {
      try {
        LongMath.binomial(n, -1);
        fail("Expected IllegalArgumentException");
      } catch (IllegalArgumentException expected) {}
      try {
        LongMath.binomial(n, n + 1);
        fail("Expected IllegalArgumentException");
      } catch (IllegalArgumentException expected) {}
    }
  }

  public void testBinomialNegative() {
    for (int n : NEGATIVE_INTEGER_CANDIDATES) {
      try {
        LongMath.binomial(n, 0);
        fail("Expected IllegalArgumentException");
      } catch (IllegalArgumentException expected) {}
    }
  }
  
  public void testSqrtOfLongIsAtMostFloorSqrtMaxLong() {
    long sqrtMaxLong = (long) Math.sqrt(Long.MAX_VALUE);
    assertTrue(sqrtMaxLong <= LongMath.FLOOR_SQRT_MAX_LONG);
  }

  /**
   * Helper method that asserts the arithmetic mean of x and y is equal
   * to the expectedMean.
   */
  private static void assertMean(long expectedMean, long x, long y) {
    assertEquals("The expectedMean should be the same as computeMeanSafely",
        expectedMean, computeMeanSafely(x, y));
    assertMean(x, y);
  }

  /**
   * Helper method that asserts the arithmetic mean of x and y is equal
   *to the result of computeMeanSafely.
   */
  private static void assertMean(long x, long y) {
    long expectedMean = computeMeanSafely(x, y);
    assertEquals(expectedMean, LongMath.mean(x, y));
    assertEquals("The mean of x and y should equal the mean of y and x",
        expectedMean, LongMath.mean(y, x));
  }

  /**
   * Computes the mean in a way that is obvious and resilient to
   * overflow by using BigInteger arithmetic.
   */
  private static long computeMeanSafely(long x, long y) {
    BigInteger bigX = BigInteger.valueOf(x);
    BigInteger bigY = BigInteger.valueOf(y);
    BigDecimal bigMean = new BigDecimal(bigX.add(bigY))
        .divide(BigDecimal.valueOf(2), BigDecimal.ROUND_FLOOR);
    // parseInt blows up on overflow as opposed to intValue() which does not.
    return Long.parseLong(bigMean.toString());
  }

  private static boolean fitsInLong(BigInteger big) {
    return big.bitLength() <= 63;
  }
}

