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

import static com.google.common.math.MathTesting.ALL_BIGINTEGER_CANDIDATES;
import static com.google.common.math.MathTesting.ALL_ROUNDING_MODES;
import static com.google.common.math.MathTesting.POSITIVE_BIGINTEGER_CANDIDATES;
import static java.math.BigInteger.ONE;
import static java.math.BigInteger.ZERO;
import static java.math.RoundingMode.CEILING;
import static java.math.RoundingMode.DOWN;
import static java.math.RoundingMode.FLOOR;
import static java.math.RoundingMode.HALF_DOWN;
import static java.math.RoundingMode.HALF_EVEN;
import static java.math.RoundingMode.HALF_UP;
import static java.math.RoundingMode.UNNECESSARY;
import static java.math.RoundingMode.UP;
import static java.util.Arrays.asList;

import com.google.common.annotations.GwtCompatible;

import junit.framework.TestCase;

import java.math.BigInteger;
import java.math.RoundingMode;

/**
 * Tests for BigIntegerMath.
 *
 * @author Louis Wasserman
 */
@GwtCompatible(emulated = true)
public class BigIntegerMathTest extends TestCase {

  public void testIsPowerOfTwo() {
    for (BigInteger x : ALL_BIGINTEGER_CANDIDATES) {
      // Checks for a single bit set.
      boolean expected = x.signum() > 0 & x.and(x.subtract(ONE)).equals(ZERO);
      assertEquals(expected, BigIntegerMath.isPowerOfTwo(x));
    }
  }

  public void testLog2ZeroAlwaysThrows() {
    for (RoundingMode mode : ALL_ROUNDING_MODES) {
      try {
        BigIntegerMath.log2(ZERO, mode);
        fail("Expected IllegalArgumentException");
      } catch (IllegalArgumentException expected) {}
    }
  }

  public void testLog2NegativeAlwaysThrows() {
    for (RoundingMode mode : ALL_ROUNDING_MODES) {
      try {
        BigIntegerMath.log2(BigInteger.valueOf(-1), mode);
        fail("Expected IllegalArgumentException");
      } catch (IllegalArgumentException expected) {}
    }
  }

  public void testLog2Floor() {
    for (BigInteger x : POSITIVE_BIGINTEGER_CANDIDATES) {
      for (RoundingMode mode : asList(FLOOR, DOWN)) {
        int result = BigIntegerMath.log2(x, mode);
        assertTrue(ZERO.setBit(result).compareTo(x) <= 0);
        assertTrue(ZERO.setBit(result + 1).compareTo(x) > 0);
      }
    }
  }

  public void testLog2Ceiling() {
    for (BigInteger x : POSITIVE_BIGINTEGER_CANDIDATES) {
      for (RoundingMode mode : asList(CEILING, UP)) {
        int result = BigIntegerMath.log2(x, mode);
        assertTrue(ZERO.setBit(result).compareTo(x) >= 0);
        assertTrue(result == 0 || ZERO.setBit(result - 1).compareTo(x) < 0);
      }
    }
  }

  // Relies on the correctness of isPowerOfTwo(BigInteger).
  public void testLog2Exact() {
    for (BigInteger x : POSITIVE_BIGINTEGER_CANDIDATES) {
      // We only expect an exception if x was not a power of 2.
      boolean isPowerOf2 = BigIntegerMath.isPowerOfTwo(x);
      try {
        assertEquals(x, ZERO.setBit(BigIntegerMath.log2(x, UNNECESSARY)));
        assertTrue(isPowerOf2);
      } catch (ArithmeticException e) {
        assertFalse(isPowerOf2);
      }
    }
  }

  public void testLog2HalfUp() {
    for (BigInteger x : POSITIVE_BIGINTEGER_CANDIDATES) {
      int result = BigIntegerMath.log2(x, HALF_UP);
      BigInteger x2 = x.pow(2);
      // x^2 < 2^(2 * result + 1), or else we would have rounded up
      assertTrue(ZERO.setBit(2 * result + 1).compareTo(x2) > 0);
      // x^2 >= 2^(2 * result - 1), or else we would have rounded down
      assertTrue(result == 0 || ZERO.setBit(2 * result - 1).compareTo(x2) <= 0);
    }
  }

  public void testLog2HalfDown() {
    for (BigInteger x : POSITIVE_BIGINTEGER_CANDIDATES) {
      int result = BigIntegerMath.log2(x, HALF_DOWN);
      BigInteger x2 = x.pow(2);
      // x^2 <= 2^(2 * result + 1), or else we would have rounded up
      assertTrue(ZERO.setBit(2 * result + 1).compareTo(x2) >= 0);
      // x^2 > 2^(2 * result - 1), or else we would have rounded down
      assertTrue(result == 0 || ZERO.setBit(2 * result - 1).compareTo(x2) < 0);
    }
  }

  // Relies on the correctness of log2(BigInteger, {HALF_UP,HALF_DOWN}).
  public void testLog2HalfEven() {
    for (BigInteger x : POSITIVE_BIGINTEGER_CANDIDATES) {
      int halfEven = BigIntegerMath.log2(x, HALF_EVEN);
      // Now figure out what rounding mode we should behave like (it depends if FLOOR was
      // odd/even).
      boolean floorWasEven = (BigIntegerMath.log2(x, FLOOR) & 1) == 0;
      assertEquals(BigIntegerMath.log2(x, floorWasEven ? HALF_DOWN : HALF_UP), halfEven);
    }
  }

  // Relies on the correctness of log10(BigInteger, FLOOR).

  // Relies on the correctness of log10(BigInteger, {HALF_UP,HALF_DOWN}).

  // Relies on the correctness of sqrt(BigInteger, FLOOR).

  // Relies on the correctness of sqrt(BigInteger, {HALF_UP,HALF_DOWN}).

  public void testFactorial() {
    BigInteger expected = BigInteger.ONE;
    for (int i = 1; i <= 200; i++) {
      expected = expected.multiply(BigInteger.valueOf(i));
      assertEquals(expected, BigIntegerMath.factorial(i));
    }
  }

  public void testFactorial0() {
    assertEquals(BigInteger.ONE, BigIntegerMath.factorial(0));
  }

  public void testFactorialNegative() {
    try {
      BigIntegerMath.factorial(-1);
      fail("Expected IllegalArgumentException");
    } catch (IllegalArgumentException expected) {}
  }

  public void testBinomialSmall() {
    runBinomialTest(0, 30);
  }

  // Depends on the correctness of BigIntegerMath.factorial
  private static void runBinomialTest(int firstN, int lastN) {
    for (int n = firstN; n <= lastN; n++) {
      for (int k = 0; k <= n; k++) {
        BigInteger expected = BigIntegerMath
            .factorial(n)
            .divide(BigIntegerMath.factorial(k))
            .divide(BigIntegerMath.factorial(n - k));
        assertEquals(expected, BigIntegerMath.binomial(n, k));
      }
    }
  }

  public void testBinomialOutside() {
    for (int n = 0; n <= 50; n++) {
      try {
        BigIntegerMath.binomial(n, -1);
        fail("Expected IllegalArgumentException");
      } catch (IllegalArgumentException expected) {}
      try {
        BigIntegerMath.binomial(n, n + 1);
        fail("Expected IllegalArgumentException");
      } catch (IllegalArgumentException expected) {}
    }
  }
}

