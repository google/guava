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

import static com.google.common.math.MathTesting.ALL_INTEGER_CANDIDATES;
import static com.google.common.math.MathTesting.ALL_ROUNDING_MODES;
import static com.google.common.math.MathTesting.ALL_SAFE_ROUNDING_MODES;
import static com.google.common.math.MathTesting.EXPONENTS;
import static com.google.common.math.MathTesting.NEGATIVE_INTEGER_CANDIDATES;
import static com.google.common.math.MathTesting.NONZERO_INTEGER_CANDIDATES;
import static com.google.common.math.MathTesting.POSITIVE_INTEGER_CANDIDATES;
import static com.google.common.math.TestPlatform.intsCanGoOutOfRange;
import static java.math.BigInteger.valueOf;
import static java.math.RoundingMode.FLOOR;
import static java.math.RoundingMode.UNNECESSARY;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.testing.NullPointerTester;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.Random;
import junit.framework.TestCase;

/**
 * Tests for {@link IntMath}.
 *
 * @author Louis Wasserman
 */
@GwtCompatible(emulated = true)
public class IntMathTest extends TestCase {
  public void testMaxSignedPowerOfTwo() {
    assertTrue(IntMath.isPowerOfTwo(IntMath.MAX_SIGNED_POWER_OF_TWO));

    // Extra work required to make GWT happy.
    long value = IntMath.MAX_SIGNED_POWER_OF_TWO * 2L;
    assertFalse(IntMath.isPowerOfTwo((int) value));
  }

  public void testCeilingPowerOfTwo() {
    for (int x : POSITIVE_INTEGER_CANDIDATES) {
      BigInteger expectedResult = BigIntegerMath.ceilingPowerOfTwo(BigInteger.valueOf(x));
      if (fitsInInt(expectedResult)) {
        assertEquals(expectedResult.intValue(), IntMath.ceilingPowerOfTwo(x));
      } else {
        try {
          IntMath.ceilingPowerOfTwo(x);
          fail("Expected ArithmeticException");
        } catch (ArithmeticException expected) {
        }
      }
    }
  }

  public void testFloorPowerOfTwo() {
    for (int x : POSITIVE_INTEGER_CANDIDATES) {
      BigInteger expectedResult = BigIntegerMath.floorPowerOfTwo(BigInteger.valueOf(x));
      assertEquals(expectedResult.intValue(), IntMath.floorPowerOfTwo(x));
    }
  }

  public void testCeilingPowerOfTwoNegative() {
    for (int x : NEGATIVE_INTEGER_CANDIDATES) {
      try {
        IntMath.ceilingPowerOfTwo(x);
        fail("Expected IllegalArgumentException");
      } catch (IllegalArgumentException expected) {
      }
    }
  }

  public void testFloorPowerOfTwoNegative() {
    for (int x : NEGATIVE_INTEGER_CANDIDATES) {
      try {
        IntMath.floorPowerOfTwo(x);
        fail("Expected IllegalArgumentException");
      } catch (IllegalArgumentException expected) {
      }
    }
  }

  public void testCeilingPowerOfTwoZero() {
    try {
      IntMath.ceilingPowerOfTwo(0);
      fail("Expected IllegalArgumentException");
    } catch (IllegalArgumentException expected) {
    }
  }

  public void testFloorPowerOfTwoZero() {
    try {
      IntMath.floorPowerOfTwo(0);
      fail("Expected IllegalArgumentException");
    } catch (IllegalArgumentException expected) {
    }
  }

  @GwtIncompatible // BigIntegerMath // TODO(cpovirk): GWT-enable BigIntegerMath
  public void testConstantMaxPowerOfSqrt2Unsigned() {
    assertEquals(
        /*expected=*/ BigIntegerMath.sqrt(BigInteger.ZERO.setBit(2 * Integer.SIZE - 1), FLOOR)
            .intValue(),
        /*actual=*/ IntMath.MAX_POWER_OF_SQRT2_UNSIGNED);
  }

  @GwtIncompatible // pow()
  public void testConstantsPowersOf10() {
    for (int i = 0; i < IntMath.powersOf10.length - 1; i++) {
      assertEquals(IntMath.pow(10, i), IntMath.powersOf10[i]);
    }
  }

  @GwtIncompatible // BigIntegerMath // TODO(cpovirk): GWT-enable BigIntegerMath
  public void testMaxLog10ForLeadingZeros() {
    for (int i = 0; i < Integer.SIZE; i++) {
      assertEquals(
          BigIntegerMath.log10(BigInteger.ONE.shiftLeft(Integer.SIZE - i), FLOOR),
          IntMath.maxLog10ForLeadingZeros[i]);
    }
  }

  @GwtIncompatible // BigIntegerMath // TODO(cpovirk): GWT-enable BigIntegerMath
  public void testConstantsHalfPowersOf10() {
    for (int i = 0; i < IntMath.halfPowersOf10.length; i++) {
      assertEquals(
          IntMath.halfPowersOf10[i],
          Math.min(
              Integer.MAX_VALUE,
              BigIntegerMath.sqrt(BigInteger.TEN.pow(2 * i + 1), FLOOR).longValue()));
    }
  }

  public void testConstantsBiggestBinomials() {
    for (int k = 0; k < IntMath.biggestBinomials.length; k++) {
      assertTrue(fitsInInt(BigIntegerMath.binomial(IntMath.biggestBinomials[k], k)));
      assertTrue(
          IntMath.biggestBinomials[k] == Integer.MAX_VALUE
              || !fitsInInt(BigIntegerMath.binomial(IntMath.biggestBinomials[k] + 1, k)));
      // In the first case, any int is valid; in the second, we want to test that the next-bigger
      // int overflows.
    }
    assertFalse(
        fitsInInt(
            BigIntegerMath.binomial(
                2 * IntMath.biggestBinomials.length, IntMath.biggestBinomials.length)));
  }

  @GwtIncompatible // sqrt
  public void testPowersSqrtMaxInt() {
    assertEquals(
        /*expected=*/ IntMath.sqrt(Integer.MAX_VALUE, FLOOR),
        /*actual=*/ IntMath.FLOOR_SQRT_MAX_INT);
  }

  @AndroidIncompatible // presumably slow
  public void testLessThanBranchFree() {
    for (int x : ALL_INTEGER_CANDIDATES) {
      for (int y : ALL_INTEGER_CANDIDATES) {
        if (LongMath.fitsInInt((long) x - y)) {
          int expected = (x < y) ? 1 : 0;
          int actual = IntMath.lessThanBranchFree(x, y);
          assertEquals(expected, actual);
        }
      }
    }
  }

  @GwtIncompatible // java.math.BigInteger
  public void testIsPowerOfTwo() {
    for (int x : ALL_INTEGER_CANDIDATES) {
      // Checks for a single bit set.
      BigInteger bigX = BigInteger.valueOf(x);
      boolean expected = (bigX.signum() > 0) && (bigX.bitCount() == 1);
      assertEquals(expected, IntMath.isPowerOfTwo(x));
    }
  }

  public void testLog2ZeroAlwaysThrows() {
    for (RoundingMode mode : ALL_ROUNDING_MODES) {
      try {
        IntMath.log2(0, mode);
        fail("Expected IllegalArgumentException");
      } catch (IllegalArgumentException expected) {
      }
    }
  }

  public void testLog2NegativeAlwaysThrows() {
    for (int x : NEGATIVE_INTEGER_CANDIDATES) {
      for (RoundingMode mode : ALL_ROUNDING_MODES) {
        try {
          IntMath.log2(x, mode);
          fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
        }
      }
    }
  }

  // Relies on the correctness of BigIntegrerMath.log2 for all modes except UNNECESSARY.
  public void testLog2MatchesBigInteger() {
    for (int x : POSITIVE_INTEGER_CANDIDATES) {
      for (RoundingMode mode : ALL_SAFE_ROUNDING_MODES) {
        assertEquals(BigIntegerMath.log2(valueOf(x), mode), IntMath.log2(x, mode));
      }
    }
  }

  // Relies on the correctness of isPowerOfTwo(int).
  public void testLog2Exact() {
    for (int x : POSITIVE_INTEGER_CANDIDATES) {
      // We only expect an exception if x was not a power of 2.
      boolean isPowerOf2 = IntMath.isPowerOfTwo(x);
      try {
        assertEquals(x, 1 << IntMath.log2(x, UNNECESSARY));
        assertTrue(isPowerOf2);
      } catch (ArithmeticException e) {
        assertFalse(isPowerOf2);
      }
    }
  }

  @GwtIncompatible // log10
  public void testLog10ZeroAlwaysThrows() {
    for (RoundingMode mode : ALL_ROUNDING_MODES) {
      try {
        IntMath.log10(0, mode);
        fail("Expected IllegalArgumentException");
      } catch (IllegalArgumentException expected) {
      }
    }
  }

  @GwtIncompatible // log10
  public void testLog10NegativeAlwaysThrows() {
    for (int x : NEGATIVE_INTEGER_CANDIDATES) {
      for (RoundingMode mode : ALL_ROUNDING_MODES) {
        try {
          IntMath.log10(x, mode);
          fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
        }
      }
    }
  }

  // Relies on the correctness of BigIntegerMath.log10 for all modes except UNNECESSARY.
  @GwtIncompatible // BigIntegerMath // TODO(cpovirk): GWT-enable BigIntegerMath
  public void testLog10MatchesBigInteger() {
    for (int x : POSITIVE_INTEGER_CANDIDATES) {
      for (RoundingMode mode : ALL_SAFE_ROUNDING_MODES) {
        // The BigInteger implementation is tested separately, use it as the reference.
        assertEquals(BigIntegerMath.log10(valueOf(x), mode), IntMath.log10(x, mode));
      }
    }
  }

  // Relies on the correctness of log10(int, FLOOR) and of pow(int, int).
  @GwtIncompatible // pow()
  public void testLog10Exact() {
    for (int x : POSITIVE_INTEGER_CANDIDATES) {
      int floor = IntMath.log10(x, FLOOR);
      boolean expectSuccess = IntMath.pow(10, floor) == x;
      try {
        assertEquals(floor, IntMath.log10(x, UNNECESSARY));
        assertTrue(expectSuccess);
      } catch (ArithmeticException e) {
        assertFalse(expectSuccess);
      }
    }
  }

  @GwtIncompatible // log10
  public void testLog10TrivialOnPowerOfTen() {
    int x = 1000000;
    for (RoundingMode mode : ALL_ROUNDING_MODES) {
      assertEquals(6, IntMath.log10(x, mode));
    }
  }

  // Simple test to cover sqrt(0) for all types and all modes.
  @GwtIncompatible // sqrt
  public void testSqrtZeroAlwaysZero() {
    for (RoundingMode mode : ALL_ROUNDING_MODES) {
      assertEquals(0, IntMath.sqrt(0, mode));
    }
  }

  @GwtIncompatible // sqrt
  public void testSqrtNegativeAlwaysThrows() {
    for (int x : NEGATIVE_INTEGER_CANDIDATES) {
      for (RoundingMode mode : RoundingMode.values()) {
        try {
          IntMath.sqrt(x, mode);
          fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
        }
      }
    }
  }

  /* Relies on the correctness of BigIntegerMath.sqrt for all modes except UNNECESSARY. */
  @GwtIncompatible // BigIntegerMath // TODO(cpovirk): GWT-enable BigIntegerMath
  public void testSqrtMatchesBigInteger() {
    for (int x : POSITIVE_INTEGER_CANDIDATES) {
      for (RoundingMode mode : ALL_SAFE_ROUNDING_MODES) {
        // The BigInteger implementation is tested separately, use it as the reference.
        // Promote the int value (rather than using intValue() on the expected value) to avoid
        // any risk of truncation which could lead to a false positive.
        assertEquals(BigIntegerMath.sqrt(valueOf(x), mode), valueOf(IntMath.sqrt(x, mode)));
      }
    }
  }

  /* Relies on the correctness of sqrt(int, FLOOR). */
  @GwtIncompatible // sqrt
  public void testSqrtExactMatchesFloorOrThrows() {
    for (int x : POSITIVE_INTEGER_CANDIDATES) {
      int floor = IntMath.sqrt(x, FLOOR);
      // We only expect an exception if x was not a perfect square.
      boolean isPerfectSquare = (floor * floor == x);
      try {
        assertEquals(floor, IntMath.sqrt(x, UNNECESSARY));
        assertTrue(isPerfectSquare);
      } catch (ArithmeticException e) {
        assertFalse(isPerfectSquare);
      }
    }
  }

  @GwtIncompatible // 2147483646^2 expected=4
  public void testPow() {
    for (int i : ALL_INTEGER_CANDIDATES) {
      for (int pow : EXPONENTS) {
        assertEquals(i + "^" + pow, BigInteger.valueOf(i).pow(pow).intValue(), IntMath.pow(i, pow));
      }
    }
  }

  @AndroidIncompatible // slow
  public void testDivNonZero() {
    for (int p : NONZERO_INTEGER_CANDIDATES) {
      for (int q : NONZERO_INTEGER_CANDIDATES) {
        for (RoundingMode mode : ALL_SAFE_ROUNDING_MODES) {
          // Skip some tests that fail due to GWT's non-compliant int implementation.
          // TODO(cpovirk): does this test fail for only some rounding modes or for all?
          if (p == -2147483648 && q == -1 && intsCanGoOutOfRange()) {
            continue;
          }
          int expected =
              new BigDecimal(valueOf(p)).divide(new BigDecimal(valueOf(q)), 0, mode).intValue();
          assertEquals(p + "/" + q, force32(expected), IntMath.divide(p, q, mode));
        }
      }
    }
  }

  @AndroidIncompatible // presumably slow
  public void testDivNonZeroExact() {
    for (int p : NONZERO_INTEGER_CANDIDATES) {
      for (int q : NONZERO_INTEGER_CANDIDATES) {
        // Skip some tests that fail due to GWT's non-compliant int implementation.
        if (p == -2147483648 && q == -1 && intsCanGoOutOfRange()) {
          continue;
        }
        boolean dividesEvenly = (p % q) == 0;
        try {
          assertEquals(p + "/" + q, p, IntMath.divide(p, q, UNNECESSARY) * q);
          assertTrue(p + "/" + q + " not expected to divide evenly", dividesEvenly);
        } catch (ArithmeticException e) {
          assertFalse(p + "/" + q + " expected to divide evenly", dividesEvenly);
        }
      }
    }
  }

  public void testZeroDivIsAlwaysZero() {
    for (int q : NONZERO_INTEGER_CANDIDATES) {
      for (RoundingMode mode : ALL_ROUNDING_MODES) {
        assertEquals(0, IntMath.divide(0, q, mode));
      }
    }
  }

  public void testDivByZeroAlwaysFails() {
    for (int p : ALL_INTEGER_CANDIDATES) {
      for (RoundingMode mode : ALL_ROUNDING_MODES) {
        try {
          IntMath.divide(p, 0, mode);
          fail("Expected ArithmeticException");
        } catch (ArithmeticException expected) {
        }
      }
    }
  }

  public void testMod() {
    for (int x : ALL_INTEGER_CANDIDATES) {
      for (int m : POSITIVE_INTEGER_CANDIDATES) {
        assertEquals(valueOf(x).mod(valueOf(m)).intValue(), IntMath.mod(x, m));
      }
    }
  }

  public void testModNegativeModulusFails() {
    for (int x : POSITIVE_INTEGER_CANDIDATES) {
      for (int m : NEGATIVE_INTEGER_CANDIDATES) {
        try {
          IntMath.mod(x, m);
          fail("Expected ArithmeticException");
        } catch (ArithmeticException expected) {
        }
      }
    }
  }

  public void testModZeroModulusFails() {
    for (int x : ALL_INTEGER_CANDIDATES) {
      try {
        IntMath.mod(x, 0);
        fail("Expected ArithmeticException");
      } catch (ArithmeticException expected) {
      }
    }
  }

  public void testGCD() {
    for (int a : POSITIVE_INTEGER_CANDIDATES) {
      for (int b : POSITIVE_INTEGER_CANDIDATES) {
        assertEquals(valueOf(a).gcd(valueOf(b)), valueOf(IntMath.gcd(a, b)));
      }
    }
  }

  public void testGCDZero() {
    for (int a : POSITIVE_INTEGER_CANDIDATES) {
      assertEquals(a, IntMath.gcd(a, 0));
      assertEquals(a, IntMath.gcd(0, a));
    }
    assertEquals(0, IntMath.gcd(0, 0));
  }

  public void testGCDNegativePositiveThrows() {
    for (int a : NEGATIVE_INTEGER_CANDIDATES) {
      try {
        IntMath.gcd(a, 3);
        fail("Expected IllegalArgumentException");
      } catch (IllegalArgumentException expected) {
      }
      try {
        IntMath.gcd(3, a);
        fail("Expected IllegalArgumentException");
      } catch (IllegalArgumentException expected) {
      }
    }
  }

  public void testGCDNegativeZeroThrows() {
    for (int a : NEGATIVE_INTEGER_CANDIDATES) {
      try {
        IntMath.gcd(a, 0);
        fail("Expected IllegalArgumentException");
      } catch (IllegalArgumentException expected) {
      }
      try {
        IntMath.gcd(0, a);
        fail("Expected IllegalArgumentException");
      } catch (IllegalArgumentException expected) {
      }
    }
  }

  @AndroidIncompatible // slow
  public void testCheckedAdd() {
    for (int a : ALL_INTEGER_CANDIDATES) {
      for (int b : ALL_INTEGER_CANDIDATES) {
        BigInteger expectedResult = valueOf(a).add(valueOf(b));
        boolean expectedSuccess = fitsInInt(expectedResult);
        try {
          assertEquals(a + b, IntMath.checkedAdd(a, b));
          assertTrue(expectedSuccess);
        } catch (ArithmeticException e) {
          assertFalse(expectedSuccess);
        }
      }
    }
  }

  @AndroidIncompatible // slow
  public void testCheckedSubtract() {
    for (int a : ALL_INTEGER_CANDIDATES) {
      for (int b : ALL_INTEGER_CANDIDATES) {
        BigInteger expectedResult = valueOf(a).subtract(valueOf(b));
        boolean expectedSuccess = fitsInInt(expectedResult);
        try {
          assertEquals(a - b, IntMath.checkedSubtract(a, b));
          assertTrue(expectedSuccess);
        } catch (ArithmeticException e) {
          assertFalse(expectedSuccess);
        }
      }
    }
  }

  @AndroidIncompatible // presumably slow
  public void testCheckedMultiply() {
    for (int a : ALL_INTEGER_CANDIDATES) {
      for (int b : ALL_INTEGER_CANDIDATES) {
        BigInteger expectedResult = valueOf(a).multiply(valueOf(b));
        boolean expectedSuccess = fitsInInt(expectedResult);
        try {
          assertEquals(a * b, IntMath.checkedMultiply(a, b));
          assertTrue(expectedSuccess);
        } catch (ArithmeticException e) {
          assertFalse(expectedSuccess);
        }
      }
    }
  }

  public void testCheckedPow() {
    for (int b : ALL_INTEGER_CANDIDATES) {
      for (int k : EXPONENTS) {
        BigInteger expectedResult = valueOf(b).pow(k);
        boolean expectedSuccess = fitsInInt(expectedResult);
        try {
          assertEquals(b + "^" + k, force32(expectedResult.intValue()), IntMath.checkedPow(b, k));
          assertTrue(b + "^" + k + " should have succeeded", expectedSuccess);
        } catch (ArithmeticException e) {
          assertFalse(b + "^" + k + " should have failed", expectedSuccess);
        }
      }
    }
  }

  @AndroidIncompatible // slow
  @GwtIncompatible // TODO
  public void testSaturatedAdd() {
    for (int a : ALL_INTEGER_CANDIDATES) {
      for (int b : ALL_INTEGER_CANDIDATES) {
        assertOperationEquals(
            a, b, "s+", saturatedCast(valueOf(a).add(valueOf(b))), IntMath.saturatedAdd(a, b));
      }
    }
  }

  @AndroidIncompatible // slow
  @GwtIncompatible // TODO
  public void testSaturatedSubtract() {
    for (int a : ALL_INTEGER_CANDIDATES) {
      for (int b : ALL_INTEGER_CANDIDATES) {
        assertOperationEquals(
            a,
            b,
            "s-",
            saturatedCast(valueOf(a).subtract(valueOf(b))),
            IntMath.saturatedSubtract(a, b));
      }
    }
  }

  @AndroidIncompatible // slow
  @GwtIncompatible // TODO
  public void testSaturatedMultiply() {
    for (int a : ALL_INTEGER_CANDIDATES) {
      for (int b : ALL_INTEGER_CANDIDATES) {
        assertOperationEquals(
            a,
            b,
            "s*",
            saturatedCast(valueOf(a).multiply(valueOf(b))),
            IntMath.saturatedMultiply(a, b));
      }
    }
  }

  @GwtIncompatible // TODO
  public void testSaturatedPow() {
    for (int a : ALL_INTEGER_CANDIDATES) {
      for (int b : EXPONENTS) {
        assertOperationEquals(
            a, b, "s^", saturatedCast(valueOf(a).pow(b)), IntMath.saturatedPow(a, b));
      }
    }
  }

  private static final BigInteger MAX_INT = BigInteger.valueOf(Integer.MAX_VALUE);
  private static final BigInteger MIN_INT = BigInteger.valueOf(Integer.MIN_VALUE);

  private static int saturatedCast(BigInteger big) {
    if (big.compareTo(MAX_INT) > 0) {
      return Integer.MAX_VALUE;
    }
    if (big.compareTo(MIN_INT) < 0) {
      return Integer.MIN_VALUE;
    }
    return big.intValue();
  }

  private void assertOperationEquals(int a, int b, String op, int expected, int actual) {
    if (expected != actual) {
      fail("Expected for " + a + " " + op + " " + b + " = " + expected + ", but got " + actual);
    }
  }

  // Depends on the correctness of BigIntegerMath.factorial.
  public void testFactorial() {
    for (int n = 0; n <= 50; n++) {
      BigInteger expectedBig = BigIntegerMath.factorial(n);
      int expectedInt = fitsInInt(expectedBig) ? expectedBig.intValue() : Integer.MAX_VALUE;
      assertEquals(expectedInt, IntMath.factorial(n));
    }
  }

  public void testFactorialNegative() {
    for (int n : NEGATIVE_INTEGER_CANDIDATES) {
      try {
        IntMath.factorial(n);
        fail("Expected IllegalArgumentException");
      } catch (IllegalArgumentException expected) {
      }
    }
  }

  // Depends on the correctness of BigIntegerMath.binomial.
  public void testBinomial() {
    for (int n = 0; n <= 50; n++) {
      for (int k = 0; k <= n; k++) {
        BigInteger expectedBig = BigIntegerMath.binomial(n, k);
        int expectedInt = fitsInInt(expectedBig) ? expectedBig.intValue() : Integer.MAX_VALUE;
        assertEquals(expectedInt, IntMath.binomial(n, k));
      }
    }
  }

  public void testBinomialOutside() {
    for (int n = 0; n <= 50; n++) {
      try {
        IntMath.binomial(n, -1);
        fail("Expected IllegalArgumentException");
      } catch (IllegalArgumentException expected) {
      }
      try {
        IntMath.binomial(n, n + 1);
        fail("Expected IllegalArgumentException");
      } catch (IllegalArgumentException expected) {
      }
    }
  }

  public void testBinomialNegative() {
    for (int n : NEGATIVE_INTEGER_CANDIDATES) {
      try {
        IntMath.binomial(n, 0);
        fail("Expected IllegalArgumentException");
      } catch (IllegalArgumentException expected) {
      }
    }
  }

  @AndroidIncompatible // slow
  @GwtIncompatible // java.math.BigInteger
  public void testMean() {
    // Odd-sized ranges have an obvious mean
    assertMean(2, 1, 3);

    assertMean(-2, -3, -1);
    assertMean(0, -1, 1);
    assertMean(1, -1, 3);
    assertMean((1 << 30) - 1, -1, Integer.MAX_VALUE);

    // Even-sized ranges should prefer the lower mean
    assertMean(2, 1, 4);
    assertMean(-3, -4, -1);
    assertMean(0, -1, 2);
    assertMean(0, Integer.MIN_VALUE + 2, Integer.MAX_VALUE);
    assertMean(0, 0, 1);
    assertMean(-1, -1, 0);
    assertMean(-1, Integer.MIN_VALUE, Integer.MAX_VALUE);

    // x == y == mean
    assertMean(1, 1, 1);
    assertMean(0, 0, 0);
    assertMean(-1, -1, -1);
    assertMean(Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE);
    assertMean(Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE);

    // Exhaustive checks
    for (int x : ALL_INTEGER_CANDIDATES) {
      for (int y : ALL_INTEGER_CANDIDATES) {
        assertMean(x, y);
      }
    }
  }

  /** Helper method that asserts the arithmetic mean of x and y is equal to the expectedMean. */
  private static void assertMean(int expectedMean, int x, int y) {
    assertEquals(
        "The expectedMean should be the same as computeMeanSafely",
        expectedMean,
        computeMeanSafely(x, y));
    assertMean(x, y);
  }

  /**
   * Helper method that asserts the arithmetic mean of x and y is equal to the result of
   * computeMeanSafely.
   */
  private static void assertMean(int x, int y) {
    int expectedMean = computeMeanSafely(x, y);
    assertEquals(expectedMean, IntMath.mean(x, y));
    assertEquals(
        "The mean of x and y should equal the mean of y and x", expectedMean, IntMath.mean(y, x));
  }

  /**
   * Computes the mean in a way that is obvious and resilient to overflow by using BigInteger
   * arithmetic.
   */
  private static int computeMeanSafely(int x, int y) {
    BigInteger bigX = BigInteger.valueOf(x);
    BigInteger bigY = BigInteger.valueOf(y);
    BigDecimal bigMean =
        new BigDecimal(bigX.add(bigY)).divide(BigDecimal.valueOf(2), BigDecimal.ROUND_FLOOR);
    // parseInt blows up on overflow as opposed to intValue() which does not.
    return Integer.parseInt(bigMean.toString());
  }

  private static boolean fitsInInt(BigInteger big) {
    return big.bitLength() <= 31;
  }

  @GwtIncompatible // NullPointerTester
  public void testNullPointers() {
    NullPointerTester tester = new NullPointerTester();
    tester.setDefault(int.class, 1);
    tester.testAllPublicStaticMethods(IntMath.class);
  }

  @GwtIncompatible // isPrime is GWT-incompatible
  public void testIsPrime() {
    // Defer correctness tests to Long.isPrime

    // Check the first 100,000 integers
    for (int i = 0; i < 100000; i++) {
      assertEquals(LongMath.isPrime(i), IntMath.isPrime(i));
    }

    // Then check 1000 deterministic pseudo-random int values.
    Random rand = new Random(1);
    for (int i = 0; i < 1000; i++) {
      int n = rand.nextInt(Integer.MAX_VALUE);
      assertEquals(LongMath.isPrime(n), IntMath.isPrime(n));
    }
  }

  private static int force32(int value) {
    // GWT doesn't consistently overflow values to make them 32-bit, so we need to force it.
    return value & 0xffffffff;
  }
}
