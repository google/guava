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
import static com.google.common.math.MathTesting.ALL_SAFE_ROUNDING_MODES;
import static com.google.common.math.MathTesting.NEGATIVE_BIGINTEGER_CANDIDATES;
import static com.google.common.math.MathTesting.NONZERO_BIGINTEGER_CANDIDATES;
import static com.google.common.math.MathTesting.POSITIVE_BIGINTEGER_CANDIDATES;
import static java.math.BigInteger.ONE;
import static java.math.BigInteger.TEN;
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
import com.google.common.annotations.GwtIncompatible;
import com.google.common.testing.NullPointerTester;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import junit.framework.TestCase;

/**
 * Tests for BigIntegerMath.
 *
 * @author Louis Wasserman
 */
@GwtCompatible(emulated = true)
public class BigIntegerMathTest extends TestCase {
  public void testCeilingPowerOfTwo() {
    for (BigInteger x : POSITIVE_BIGINTEGER_CANDIDATES) {
      BigInteger result = BigIntegerMath.ceilingPowerOfTwo(x);
      assertTrue(BigIntegerMath.isPowerOfTwo(result));
      assertTrue(result.compareTo(x) >= 0);
      assertTrue(result.compareTo(x.add(x)) < 0);
    }
  }

  public void testFloorPowerOfTwo() {
    for (BigInteger x : POSITIVE_BIGINTEGER_CANDIDATES) {
      BigInteger result = BigIntegerMath.floorPowerOfTwo(x);
      assertTrue(BigIntegerMath.isPowerOfTwo(result));
      assertTrue(result.compareTo(x) <= 0);
      assertTrue(result.add(result).compareTo(x) > 0);
    }
  }

  public void testCeilingPowerOfTwoNegative() {
    for (BigInteger x : NEGATIVE_BIGINTEGER_CANDIDATES) {
      try {
        BigIntegerMath.ceilingPowerOfTwo(x);
        fail("Expected IllegalArgumentException");
      } catch (IllegalArgumentException expected) {
      }
    }
  }

  public void testFloorPowerOfTwoNegative() {
    for (BigInteger x : NEGATIVE_BIGINTEGER_CANDIDATES) {
      try {
        BigIntegerMath.floorPowerOfTwo(x);
        fail("Expected IllegalArgumentException");
      } catch (IllegalArgumentException expected) {
      }
    }
  }

  public void testCeilingPowerOfTwoZero() {
    try {
      BigIntegerMath.ceilingPowerOfTwo(BigInteger.ZERO);
      fail("Expected IllegalArgumentException");
    } catch (IllegalArgumentException expected) {
    }
  }

  public void testFloorPowerOfTwoZero() {
    try {
      BigIntegerMath.floorPowerOfTwo(BigInteger.ZERO);
      fail("Expected IllegalArgumentException");
    } catch (IllegalArgumentException expected) {
    }
  }

  @GwtIncompatible // TODO
  public void testConstantSqrt2PrecomputedBits() {
    assertEquals(
        BigIntegerMath.sqrt(
            BigInteger.ZERO.setBit(2 * BigIntegerMath.SQRT2_PRECOMPUTE_THRESHOLD + 1), FLOOR),
        BigIntegerMath.SQRT2_PRECOMPUTED_BITS);
  }

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
      } catch (IllegalArgumentException expected) {
      }
    }
  }

  public void testLog2NegativeAlwaysThrows() {
    for (RoundingMode mode : ALL_ROUNDING_MODES) {
      try {
        BigIntegerMath.log2(BigInteger.valueOf(-1), mode);
        fail("Expected IllegalArgumentException");
      } catch (IllegalArgumentException expected) {
      }
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

  @GwtIncompatible // TODO
  public void testLog10ZeroAlwaysThrows() {
    for (RoundingMode mode : ALL_ROUNDING_MODES) {
      try {
        BigIntegerMath.log10(ZERO, mode);
        fail("Expected IllegalArgumentException");
      } catch (IllegalArgumentException expected) {
      }
    }
  }

  @GwtIncompatible // TODO
  public void testLog10NegativeAlwaysThrows() {
    for (RoundingMode mode : ALL_ROUNDING_MODES) {
      try {
        BigIntegerMath.log10(BigInteger.valueOf(-1), mode);
        fail("Expected IllegalArgumentException");
      } catch (IllegalArgumentException expected) {
      }
    }
  }

  @GwtIncompatible // TODO
  public void testLog10Floor() {
    for (BigInteger x : POSITIVE_BIGINTEGER_CANDIDATES) {
      for (RoundingMode mode : asList(FLOOR, DOWN)) {
        int result = BigIntegerMath.log10(x, mode);
        assertTrue(TEN.pow(result).compareTo(x) <= 0);
        assertTrue(TEN.pow(result + 1).compareTo(x) > 0);
      }
    }
  }

  @GwtIncompatible // TODO
  public void testLog10Ceiling() {
    for (BigInteger x : POSITIVE_BIGINTEGER_CANDIDATES) {
      for (RoundingMode mode : asList(CEILING, UP)) {
        int result = BigIntegerMath.log10(x, mode);
        assertTrue(TEN.pow(result).compareTo(x) >= 0);
        assertTrue(result == 0 || TEN.pow(result - 1).compareTo(x) < 0);
      }
    }
  }

  // Relies on the correctness of log10(BigInteger, FLOOR).
  @GwtIncompatible // TODO
  public void testLog10Exact() {
    for (BigInteger x : POSITIVE_BIGINTEGER_CANDIDATES) {
      int logFloor = BigIntegerMath.log10(x, FLOOR);
      boolean expectSuccess = TEN.pow(logFloor).equals(x);
      try {
        assertEquals(logFloor, BigIntegerMath.log10(x, UNNECESSARY));
        assertTrue(expectSuccess);
      } catch (ArithmeticException e) {
        assertFalse(expectSuccess);
      }
    }
  }

  @GwtIncompatible // TODO
  public void testLog10HalfUp() {
    for (BigInteger x : POSITIVE_BIGINTEGER_CANDIDATES) {
      int result = BigIntegerMath.log10(x, HALF_UP);
      BigInteger x2 = x.pow(2);
      // x^2 < 10^(2 * result + 1), or else we would have rounded up
      assertTrue(TEN.pow(2 * result + 1).compareTo(x2) > 0);
      // x^2 >= 10^(2 * result - 1), or else we would have rounded down
      assertTrue(result == 0 || TEN.pow(2 * result - 1).compareTo(x2) <= 0);
    }
  }

  @GwtIncompatible // TODO
  public void testLog10HalfDown() {
    for (BigInteger x : POSITIVE_BIGINTEGER_CANDIDATES) {
      int result = BigIntegerMath.log10(x, HALF_DOWN);
      BigInteger x2 = x.pow(2);
      // x^2 <= 10^(2 * result + 1), or else we would have rounded up
      assertTrue(TEN.pow(2 * result + 1).compareTo(x2) >= 0);
      // x^2 > 10^(2 * result - 1), or else we would have rounded down
      assertTrue(result == 0 || TEN.pow(2 * result - 1).compareTo(x2) < 0);
    }
  }

  // Relies on the correctness of log10(BigInteger, {HALF_UP,HALF_DOWN}).
  @GwtIncompatible // TODO
  public void testLog10HalfEven() {
    for (BigInteger x : POSITIVE_BIGINTEGER_CANDIDATES) {
      int halfEven = BigIntegerMath.log10(x, HALF_EVEN);
      // Now figure out what rounding mode we should behave like (it depends if FLOOR was
      // odd/even).
      boolean floorWasEven = (BigIntegerMath.log10(x, FLOOR) & 1) == 0;
      assertEquals(BigIntegerMath.log10(x, floorWasEven ? HALF_DOWN : HALF_UP), halfEven);
    }
  }

  @GwtIncompatible // TODO
  public void testLog10TrivialOnPowerOf10() {
    BigInteger x = BigInteger.TEN.pow(100);
    for (RoundingMode mode : ALL_ROUNDING_MODES) {
      assertEquals(100, BigIntegerMath.log10(x, mode));
    }
  }

  @GwtIncompatible // TODO
  public void testSqrtZeroAlwaysZero() {
    for (RoundingMode mode : ALL_ROUNDING_MODES) {
      assertEquals(ZERO, BigIntegerMath.sqrt(ZERO, mode));
    }
  }

  @GwtIncompatible // TODO
  public void testSqrtNegativeAlwaysThrows() {
    for (RoundingMode mode : ALL_ROUNDING_MODES) {
      try {
        BigIntegerMath.sqrt(BigInteger.valueOf(-1), mode);
        fail("Expected IllegalArgumentException");
      } catch (IllegalArgumentException expected) {
      }
    }
  }

  @GwtIncompatible // TODO
  public void testSqrtFloor() {
    for (BigInteger x : POSITIVE_BIGINTEGER_CANDIDATES) {
      for (RoundingMode mode : asList(FLOOR, DOWN)) {
        BigInteger result = BigIntegerMath.sqrt(x, mode);
        assertTrue(result.compareTo(ZERO) > 0);
        assertTrue(result.pow(2).compareTo(x) <= 0);
        assertTrue(result.add(ONE).pow(2).compareTo(x) > 0);
      }
    }
  }

  @GwtIncompatible // TODO
  public void testSqrtCeiling() {
    for (BigInteger x : POSITIVE_BIGINTEGER_CANDIDATES) {
      for (RoundingMode mode : asList(CEILING, UP)) {
        BigInteger result = BigIntegerMath.sqrt(x, mode);
        assertTrue(result.compareTo(ZERO) > 0);
        assertTrue(result.pow(2).compareTo(x) >= 0);
        assertTrue(result.signum() == 0 || result.subtract(ONE).pow(2).compareTo(x) < 0);
      }
    }
  }

  // Relies on the correctness of sqrt(BigInteger, FLOOR).
  @GwtIncompatible // TODO
  public void testSqrtExact() {
    for (BigInteger x : POSITIVE_BIGINTEGER_CANDIDATES) {
      BigInteger floor = BigIntegerMath.sqrt(x, FLOOR);
      // We only expect an exception if x was not a perfect square.
      boolean isPerfectSquare = floor.pow(2).equals(x);
      try {
        assertEquals(floor, BigIntegerMath.sqrt(x, UNNECESSARY));
        assertTrue(isPerfectSquare);
      } catch (ArithmeticException e) {
        assertFalse(isPerfectSquare);
      }
    }
  }

  @GwtIncompatible // TODO
  public void testSqrtHalfUp() {
    for (BigInteger x : POSITIVE_BIGINTEGER_CANDIDATES) {
      BigInteger result = BigIntegerMath.sqrt(x, HALF_UP);
      BigInteger plusHalfSquared = result.pow(2).add(result).shiftLeft(2).add(ONE);
      BigInteger x4 = x.shiftLeft(2);
      // sqrt(x) < result + 0.5, so 4 * x < (result + 0.5)^2 * 4
      // (result + 0.5)^2 * 4 = (result^2 + result)*4 + 1
      assertTrue(x4.compareTo(plusHalfSquared) < 0);
      BigInteger minusHalfSquared = result.pow(2).subtract(result).shiftLeft(2).add(ONE);
      // sqrt(x) > result - 0.5, so 4 * x > (result - 0.5)^2 * 4
      // (result - 0.5)^2 * 4 = (result^2 - result)*4 + 1
      assertTrue(result.equals(ZERO) || x4.compareTo(minusHalfSquared) >= 0);
    }
  }

  @GwtIncompatible // TODO
  public void testSqrtHalfDown() {
    for (BigInteger x : POSITIVE_BIGINTEGER_CANDIDATES) {
      BigInteger result = BigIntegerMath.sqrt(x, HALF_DOWN);
      BigInteger plusHalfSquared = result.pow(2).add(result).shiftLeft(2).add(ONE);
      BigInteger x4 = x.shiftLeft(2);
      // sqrt(x) <= result + 0.5, so 4 * x <= (result + 0.5)^2 * 4
      // (result + 0.5)^2 * 4 = (result^2 + result)*4 + 1
      assertTrue(x4.compareTo(plusHalfSquared) <= 0);
      BigInteger minusHalfSquared = result.pow(2).subtract(result).shiftLeft(2).add(ONE);
      // sqrt(x) > result - 0.5, so 4 * x > (result - 0.5)^2 * 4
      // (result - 0.5)^2 * 4 = (result^2 - result)*4 + 1
      assertTrue(result.equals(ZERO) || x4.compareTo(minusHalfSquared) > 0);
    }
  }

  // Relies on the correctness of sqrt(BigInteger, {HALF_UP,HALF_DOWN}).
  @GwtIncompatible // TODO
  public void testSqrtHalfEven() {
    for (BigInteger x : POSITIVE_BIGINTEGER_CANDIDATES) {
      BigInteger halfEven = BigIntegerMath.sqrt(x, HALF_EVEN);
      // Now figure out what rounding mode we should behave like (it depends if FLOOR was
      // odd/even).
      boolean floorWasOdd = BigIntegerMath.sqrt(x, FLOOR).testBit(0);
      assertEquals(BigIntegerMath.sqrt(x, floorWasOdd ? HALF_UP : HALF_DOWN), halfEven);
    }
  }

  @GwtIncompatible // TODO
  @AndroidIncompatible // slow
  public void testDivNonZero() {
    for (BigInteger p : NONZERO_BIGINTEGER_CANDIDATES) {
      for (BigInteger q : NONZERO_BIGINTEGER_CANDIDATES) {
        for (RoundingMode mode : ALL_SAFE_ROUNDING_MODES) {
          BigInteger expected =
              new BigDecimal(p).divide(new BigDecimal(q), 0, mode).toBigIntegerExact();
          assertEquals(expected, BigIntegerMath.divide(p, q, mode));
        }
      }
    }
  }

  private static final BigInteger BAD_FOR_ANDROID_P = new BigInteger("-9223372036854775808");
  private static final BigInteger BAD_FOR_ANDROID_Q = new BigInteger("-1");

  private static final BigInteger BAD_FOR_GINGERBREAD_P = new BigInteger("-9223372036854775808");
  private static final BigInteger BAD_FOR_GINGERBREAD_Q = new BigInteger("-4294967296");

  @GwtIncompatible // TODO
  @AndroidIncompatible // slow
  public void testDivNonZeroExact() {
    boolean isAndroid = System.getProperties().getProperty("java.runtime.name").contains("Android");
    for (BigInteger p : NONZERO_BIGINTEGER_CANDIDATES) {
      for (BigInteger q : NONZERO_BIGINTEGER_CANDIDATES) {
        if (isAndroid && p.equals(BAD_FOR_ANDROID_P) && q.equals(BAD_FOR_ANDROID_Q)) {
          // https://code.google.com/p/android/issues/detail?id=196555
          continue;
        }
        if (isAndroid && p.equals(BAD_FOR_GINGERBREAD_P) && q.equals(BAD_FOR_GINGERBREAD_Q)) {
          // Works fine under Marshmallow, so I haven't filed a bug.
          continue;
        }

        boolean dividesEvenly = p.remainder(q).equals(ZERO);

        try {
          BigInteger quotient = BigIntegerMath.divide(p, q, UNNECESSARY);
          BigInteger undone = quotient.multiply(q);
          if (!p.equals(undone)) {
            failFormat("expected %s.multiply(%s) = %s; got %s", quotient, q, p, undone);
          }
          assertTrue(dividesEvenly);
        } catch (ArithmeticException e) {
          assertFalse(dividesEvenly);
        }
      }
    }
  }

  @GwtIncompatible // TODO
  public void testZeroDivIsAlwaysZero() {
    for (BigInteger q : NONZERO_BIGINTEGER_CANDIDATES) {
      for (RoundingMode mode : ALL_ROUNDING_MODES) {
        assertEquals(ZERO, BigIntegerMath.divide(ZERO, q, mode));
      }
    }
  }

  @GwtIncompatible // TODO
  public void testDivByZeroAlwaysFails() {
    for (BigInteger p : ALL_BIGINTEGER_CANDIDATES) {
      for (RoundingMode mode : ALL_ROUNDING_MODES) {
        try {
          BigIntegerMath.divide(p, ZERO, mode);
          fail("Expected ArithmeticException");
        } catch (ArithmeticException expected) {
        }
      }
    }
  }

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
    } catch (IllegalArgumentException expected) {
    }
  }

  public void testBinomialSmall() {
    runBinomialTest(0, 30);
  }

  @GwtIncompatible // too slow
  public void testBinomialLarge() {
    runBinomialTest(31, 100);
  }

  // Depends on the correctness of BigIntegerMath.factorial
  private static void runBinomialTest(int firstN, int lastN) {
    for (int n = firstN; n <= lastN; n++) {
      for (int k = 0; k <= n; k++) {
        BigInteger expected =
            BigIntegerMath.factorial(n)
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
      } catch (IllegalArgumentException expected) {
      }
      try {
        BigIntegerMath.binomial(n, n + 1);
        fail("Expected IllegalArgumentException");
      } catch (IllegalArgumentException expected) {
      }
    }
  }

  @GwtIncompatible // NullPointerTester
  public void testNullPointers() {
    NullPointerTester tester = new NullPointerTester();
    tester.setDefault(BigInteger.class, ONE);
    tester.setDefault(int.class, 1);
    tester.setDefault(long.class, 1L);
    tester.testAllPublicStaticMethods(BigIntegerMath.class);
  }

  @GwtIncompatible // String.format
  private static void failFormat(String template, Object... args) {
    fail(String.format(template, args));
  }
}
