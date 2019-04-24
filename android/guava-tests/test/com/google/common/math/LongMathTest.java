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
import static com.google.common.math.MathTesting.EXPONENTS;
import static com.google.common.math.MathTesting.NEGATIVE_INTEGER_CANDIDATES;
import static com.google.common.math.MathTesting.NEGATIVE_LONG_CANDIDATES;
import static com.google.common.math.MathTesting.NONZERO_LONG_CANDIDATES;
import static com.google.common.math.MathTesting.POSITIVE_INTEGER_CANDIDATES;
import static com.google.common.math.MathTesting.POSITIVE_LONG_CANDIDATES;
import static com.google.common.truth.Truth.assert_;
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
 * Tests for LongMath.
 *
 * @author Louis Wasserman
 */
@GwtCompatible(emulated = true)
public class LongMathTest extends TestCase {
  @SuppressWarnings("ConstantOverflow")
  public void testMaxSignedPowerOfTwo() {
    assertTrue(LongMath.isPowerOfTwo(LongMath.MAX_SIGNED_POWER_OF_TWO));
    assertFalse(LongMath.isPowerOfTwo(LongMath.MAX_SIGNED_POWER_OF_TWO * 2));
  }

  public void testCeilingPowerOfTwo() {
    for (long x : POSITIVE_LONG_CANDIDATES) {
      BigInteger expectedResult = BigIntegerMath.ceilingPowerOfTwo(BigInteger.valueOf(x));
      if (fitsInLong(expectedResult)) {
        assertEquals(expectedResult.longValue(), LongMath.ceilingPowerOfTwo(x));
      } else {
        try {
          LongMath.ceilingPowerOfTwo(x);
          fail("Expected ArithmeticException");
        } catch (ArithmeticException expected) {
        }
      }
    }
  }

  public void testFloorPowerOfTwo() {
    for (long x : POSITIVE_LONG_CANDIDATES) {
      BigInteger expectedResult = BigIntegerMath.floorPowerOfTwo(BigInteger.valueOf(x));
      assertEquals(expectedResult.longValue(), LongMath.floorPowerOfTwo(x));
    }
  }

  public void testCeilingPowerOfTwoNegative() {
    for (long x : NEGATIVE_LONG_CANDIDATES) {
      try {
        LongMath.ceilingPowerOfTwo(x);
        fail("Expected IllegalArgumentException");
      } catch (IllegalArgumentException expected) {
      }
    }
  }

  public void testFloorPowerOfTwoNegative() {
    for (long x : NEGATIVE_LONG_CANDIDATES) {
      try {
        LongMath.floorPowerOfTwo(x);
        fail("Expected IllegalArgumentException");
      } catch (IllegalArgumentException expected) {
      }
    }
  }

  public void testCeilingPowerOfTwoZero() {
    try {
      LongMath.ceilingPowerOfTwo(0L);
      fail("Expected IllegalArgumentException");
    } catch (IllegalArgumentException expected) {
    }
  }

  public void testFloorPowerOfTwoZero() {
    try {
      LongMath.floorPowerOfTwo(0L);
      fail("Expected IllegalArgumentException");
    } catch (IllegalArgumentException expected) {
    }
  }

  @GwtIncompatible // TODO
  public void testConstantMaxPowerOfSqrt2Unsigned() {
    assertEquals(
        /*expected=*/ BigIntegerMath.sqrt(BigInteger.ZERO.setBit(2 * Long.SIZE - 1), FLOOR)
            .longValue(),
        /*actual=*/ LongMath.MAX_POWER_OF_SQRT2_UNSIGNED);
  }

  @GwtIncompatible // BigIntegerMath // TODO(cpovirk): GWT-enable BigIntegerMath
  public void testMaxLog10ForLeadingZeros() {
    for (int i = 0; i < Long.SIZE; i++) {
      assertEquals(
          BigIntegerMath.log10(BigInteger.ONE.shiftLeft(Long.SIZE - i), FLOOR),
          LongMath.maxLog10ForLeadingZeros[i]);
    }
  }

  @GwtIncompatible // TODO
  public void testConstantsPowersOf10() {
    for (int i = 0; i < LongMath.powersOf10.length; i++) {
      assertEquals(LongMath.checkedPow(10, i), LongMath.powersOf10[i]);
    }
    try {
      LongMath.checkedPow(10, LongMath.powersOf10.length);
      fail("Expected ArithmeticException");
    } catch (ArithmeticException expected) {
    }
  }

  @GwtIncompatible // TODO
  public void testConstantsHalfPowersOf10() {
    for (int i = 0; i < LongMath.halfPowersOf10.length; i++) {
      assertEquals(
          BigIntegerMath.sqrt(BigInteger.TEN.pow(2 * i + 1), FLOOR),
          BigInteger.valueOf(LongMath.halfPowersOf10[i]));
    }
    BigInteger nextBigger =
        BigIntegerMath.sqrt(BigInteger.TEN.pow(2 * LongMath.halfPowersOf10.length + 1), FLOOR);
    assertTrue(nextBigger.compareTo(BigInteger.valueOf(Long.MAX_VALUE)) > 0);
  }

  @GwtIncompatible // TODO
  public void testConstantsSqrtMaxLong() {
    assertEquals(
        /*expected=*/ LongMath.sqrt(Long.MAX_VALUE, FLOOR),
        /*actual=*/ LongMath.FLOOR_SQRT_MAX_LONG);
  }

  @GwtIncompatible // TODO
  public void testConstantsFactorials() {
    long expected = 1;
    for (int i = 0; i < LongMath.factorials.length; i++, expected *= i) {
      assertEquals(expected, LongMath.factorials[i]);
    }
    try {
      LongMath.checkedMultiply(
          LongMath.factorials[LongMath.factorials.length - 1], LongMath.factorials.length);
      fail("Expected ArithmeticException");
    } catch (ArithmeticException expect) {
    }
  }

  @GwtIncompatible // TODO
  public void testConstantsBiggestBinomials() {
    for (int k = 0; k < LongMath.biggestBinomials.length; k++) {
      assertTrue(fitsInLong(BigIntegerMath.binomial(LongMath.biggestBinomials[k], k)));
      assertTrue(
          LongMath.biggestBinomials[k] == Integer.MAX_VALUE
              || !fitsInLong(BigIntegerMath.binomial(LongMath.biggestBinomials[k] + 1, k)));
      // In the first case, any long is valid; in the second, we want to test that the next-bigger
      // long overflows.
    }
    int k = LongMath.biggestBinomials.length;
    assertFalse(fitsInLong(BigIntegerMath.binomial(2 * k, k)));
    // 2 * k is the smallest value for which we don't replace k with (n-k).
  }

  @GwtIncompatible // TODO
  public void testConstantsBiggestSimpleBinomials() {
    for (int k = 0; k < LongMath.biggestSimpleBinomials.length; k++) {
      assertTrue(LongMath.biggestSimpleBinomials[k] <= LongMath.biggestBinomials[k]);
      long unused = simpleBinomial(LongMath.biggestSimpleBinomials[k], k); // mustn't throw
      if (LongMath.biggestSimpleBinomials[k] < Integer.MAX_VALUE) {
        // unless all n are fair game with this k
        try {
          simpleBinomial(LongMath.biggestSimpleBinomials[k] + 1, k);
          fail("Expected ArithmeticException");
        } catch (ArithmeticException expected) {
        }
      }
    }
    try {
      int k = LongMath.biggestSimpleBinomials.length;
      simpleBinomial(2 * k, k);
      // 2 * k is the smallest value for which we don't replace k with (n-k).
      fail("Expected ArithmeticException");
    } catch (ArithmeticException expected) {
    }
  }

  @AndroidIncompatible // slow
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
  @GwtIncompatible // TODO
  private long simpleBinomial(int n, int k) {
    long accum = 1;
    for (int i = 0; i < k; i++) {
      accum = LongMath.checkedMultiply(accum, n - i);
      accum /= i + 1;
    }
    return accum;
  }

  @GwtIncompatible // java.math.BigInteger
  public void testIsPowerOfTwo() {
    for (long x : ALL_LONG_CANDIDATES) {
      // Checks for a single bit set.
      BigInteger bigX = BigInteger.valueOf(x);
      boolean expected = (bigX.signum() > 0) && (bigX.bitCount() == 1);
      assertEquals(expected, LongMath.isPowerOfTwo(x));
    }
  }

  public void testLog2ZeroAlwaysThrows() {
    for (RoundingMode mode : ALL_ROUNDING_MODES) {
      try {
        LongMath.log2(0L, mode);
        fail("Expected IllegalArgumentException");
      } catch (IllegalArgumentException expected) {
      }
    }
  }

  public void testLog2NegativeAlwaysThrows() {
    for (long x : NEGATIVE_LONG_CANDIDATES) {
      for (RoundingMode mode : ALL_ROUNDING_MODES) {
        try {
          LongMath.log2(x, mode);
          fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
        }
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

  @GwtIncompatible // TODO
  public void testLog10ZeroAlwaysThrows() {
    for (RoundingMode mode : ALL_ROUNDING_MODES) {
      try {
        LongMath.log10(0L, mode);
        fail("Expected IllegalArgumentException");
      } catch (IllegalArgumentException expected) {
      }
    }
  }

  @GwtIncompatible // TODO
  public void testLog10NegativeAlwaysThrows() {
    for (long x : NEGATIVE_LONG_CANDIDATES) {
      for (RoundingMode mode : ALL_ROUNDING_MODES) {
        try {
          LongMath.log10(x, mode);
          fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
        }
      }
    }
  }

  // Relies on the correctness of BigIntegerMath.log10 for all modes except UNNECESSARY.
  @GwtIncompatible // TODO
  public void testLog10MatchesBigInteger() {
    for (long x : POSITIVE_LONG_CANDIDATES) {
      for (RoundingMode mode : ALL_SAFE_ROUNDING_MODES) {
        assertEquals(BigIntegerMath.log10(valueOf(x), mode), LongMath.log10(x, mode));
      }
    }
  }

  // Relies on the correctness of log10(long, FLOOR) and of pow(long, int).
  @GwtIncompatible // TODO
  public void testLog10Exact() {
    for (long x : POSITIVE_LONG_CANDIDATES) {
      int floor = LongMath.log10(x, FLOOR);
      boolean expectedSuccess = LongMath.pow(10, floor) == x;
      try {
        assertEquals(floor, LongMath.log10(x, UNNECESSARY));
        assertTrue(expectedSuccess);
      } catch (ArithmeticException e) {
        if (expectedSuccess) {
          failFormat("expected log10(%s, UNNECESSARY) = %s; got ArithmeticException", x, floor);
        }
      }
    }
  }

  @GwtIncompatible // TODO
  public void testLog10TrivialOnPowerOf10() {
    long x = 1000000000000L;
    for (RoundingMode mode : ALL_ROUNDING_MODES) {
      assertEquals(12, LongMath.log10(x, mode));
    }
  }

  @GwtIncompatible // TODO
  public void testSqrtNegativeAlwaysThrows() {
    for (long x : NEGATIVE_LONG_CANDIDATES) {
      for (RoundingMode mode : ALL_ROUNDING_MODES) {
        try {
          LongMath.sqrt(x, mode);
          fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
        }
      }
    }
  }

  // Relies on the correctness of BigIntegerMath.sqrt for all modes except UNNECESSARY.
  @GwtIncompatible // TODO
  public void testSqrtMatchesBigInteger() {
    for (long x : POSITIVE_LONG_CANDIDATES) {
      for (RoundingMode mode : ALL_SAFE_ROUNDING_MODES) {
        // Promote the long value (rather than using longValue() on the expected value) to avoid
        // any risk of truncation which could lead to a false positive.
        assertEquals(BigIntegerMath.sqrt(valueOf(x), mode), valueOf(LongMath.sqrt(x, mode)));
      }
    }
  }

  /* Relies on the correctness of sqrt(long, FLOOR). */
  @GwtIncompatible // TODO
  public void testSqrtExactMatchesFloorOrThrows() {
    for (long x : POSITIVE_LONG_CANDIDATES) {
      long sqrtFloor = LongMath.sqrt(x, FLOOR);
      // We only expect an exception if x was not a perfect square.
      boolean isPerfectSquare = (sqrtFloor * sqrtFloor == x);
      try {
        assertEquals(sqrtFloor, LongMath.sqrt(x, UNNECESSARY));
        assertTrue(isPerfectSquare);
      } catch (ArithmeticException e) {
        assertFalse(isPerfectSquare);
      }
    }
  }

  @GwtIncompatible // TODO
  public void testPow() {
    for (long i : ALL_LONG_CANDIDATES) {
      for (int exp : EXPONENTS) {
        assertEquals(LongMath.pow(i, exp), valueOf(i).pow(exp).longValue());
      }
    }
  }

  @GwtIncompatible // TODO
  @AndroidIncompatible // TODO(cpovirk): File BigDecimal.divide() rounding bug.
  public void testDivNonZero() {
    for (long p : NONZERO_LONG_CANDIDATES) {
      for (long q : NONZERO_LONG_CANDIDATES) {
        for (RoundingMode mode : ALL_SAFE_ROUNDING_MODES) {
          long expected =
              new BigDecimal(valueOf(p)).divide(new BigDecimal(valueOf(q)), 0, mode).longValue();
          long actual = LongMath.divide(p, q, mode);
          if (expected != actual) {
            failFormat("expected divide(%s, %s, %s) = %s; got %s", p, q, mode, expected, actual);
          }
        }
      }
    }
  }

  @GwtIncompatible // TODO
  @AndroidIncompatible // Bug in older versions of Android we test against, since fixed.
  public void testDivNonZeroExact() {
    for (long p : NONZERO_LONG_CANDIDATES) {
      for (long q : NONZERO_LONG_CANDIDATES) {
        boolean expectedSuccess = (p % q) == 0L;

        try {
          assertEquals(p, LongMath.divide(p, q, UNNECESSARY) * q);
          assertTrue(expectedSuccess);
        } catch (ArithmeticException e) {
          if (expectedSuccess) {
            failFormat(
                "expected divide(%s, %s, UNNECESSARY) to succeed; got ArithmeticException", p, q);
          }
        }
      }
    }
  }

  @GwtIncompatible // TODO
  public void testZeroDivIsAlwaysZero() {
    for (long q : NONZERO_LONG_CANDIDATES) {
      for (RoundingMode mode : ALL_ROUNDING_MODES) {
        assertEquals(0L, LongMath.divide(0L, q, mode));
      }
    }
  }

  @GwtIncompatible // TODO
  public void testDivByZeroAlwaysFails() {
    for (long p : ALL_LONG_CANDIDATES) {
      for (RoundingMode mode : ALL_ROUNDING_MODES) {
        try {
          LongMath.divide(p, 0L, mode);
          fail("Expected ArithmeticException");
        } catch (ArithmeticException expected) {
        }
      }
    }
  }

  @GwtIncompatible // TODO
  public void testIntMod() {
    for (long x : ALL_LONG_CANDIDATES) {
      for (int m : POSITIVE_INTEGER_CANDIDATES) {
        assertEquals(valueOf(x).mod(valueOf(m)).intValue(), LongMath.mod(x, m));
      }
    }
  }

  @GwtIncompatible // TODO
  public void testIntModNegativeModulusFails() {
    for (long x : ALL_LONG_CANDIDATES) {
      for (int m : NEGATIVE_INTEGER_CANDIDATES) {
        try {
          LongMath.mod(x, m);
          fail("Expected ArithmeticException");
        } catch (ArithmeticException expected) {
        }
      }
    }
  }

  @GwtIncompatible // TODO
  public void testIntModZeroModulusFails() {
    for (long x : ALL_LONG_CANDIDATES) {
      try {
        LongMath.mod(x, 0);
        fail("Expected AE");
      } catch (ArithmeticException expected) {
      }
    }
  }

  @AndroidIncompatible // slow
  @GwtIncompatible // TODO
  public void testMod() {
    for (long x : ALL_LONG_CANDIDATES) {
      for (long m : POSITIVE_LONG_CANDIDATES) {
        assertEquals(valueOf(x).mod(valueOf(m)).longValue(), LongMath.mod(x, m));
      }
    }
  }

  @GwtIncompatible // TODO
  public void testModNegativeModulusFails() {
    for (long x : ALL_LONG_CANDIDATES) {
      for (long m : NEGATIVE_LONG_CANDIDATES) {
        try {
          LongMath.mod(x, m);
          fail("Expected ArithmeticException");
        } catch (ArithmeticException expected) {
        }
      }
    }
  }

  public void testGCDExhaustive() {
    for (long a : POSITIVE_LONG_CANDIDATES) {
      for (long b : POSITIVE_LONG_CANDIDATES) {
        assertEquals(valueOf(a).gcd(valueOf(b)), valueOf(LongMath.gcd(a, b)));
      }
    }
  }

  @GwtIncompatible // TODO
  public void testGCDZero() {
    for (long a : POSITIVE_LONG_CANDIDATES) {
      assertEquals(a, LongMath.gcd(a, 0));
      assertEquals(a, LongMath.gcd(0, a));
    }
    assertEquals(0, LongMath.gcd(0, 0));
  }

  @GwtIncompatible // TODO
  public void testGCDNegativePositiveThrows() {
    for (long a : NEGATIVE_LONG_CANDIDATES) {
      try {
        LongMath.gcd(a, 3);
        fail("Expected IllegalArgumentException");
      } catch (IllegalArgumentException expected) {
      }
      try {
        LongMath.gcd(3, a);
        fail("Expected IllegalArgumentException");
      } catch (IllegalArgumentException expected) {
      }
    }
  }

  @GwtIncompatible // TODO
  public void testGCDNegativeZeroThrows() {
    for (long a : NEGATIVE_LONG_CANDIDATES) {
      try {
        LongMath.gcd(a, 0);
        fail("Expected IllegalArgumentException");
      } catch (IllegalArgumentException expected) {
      }
      try {
        LongMath.gcd(0, a);
        fail("Expected IllegalArgumentException");
      } catch (IllegalArgumentException expected) {
      }
    }
  }

  @AndroidIncompatible // slow
  @GwtIncompatible // TODO
  public void testCheckedAdd() {
    for (long a : ALL_LONG_CANDIDATES) {
      for (long b : ALL_LONG_CANDIDATES) {
        BigInteger expectedResult = valueOf(a).add(valueOf(b));
        boolean expectedSuccess = fitsInLong(expectedResult);
        try {
          assertEquals(a + b, LongMath.checkedAdd(a, b));
          assertTrue(expectedSuccess);
        } catch (ArithmeticException e) {
          if (expectedSuccess) {
            failFormat(
                "expected checkedAdd(%s, %s) = %s; got ArithmeticException", a, b, expectedResult);
          }
        }
      }
    }
  }

  @GwtIncompatible // TODO
  @AndroidIncompatible // slow
  public void testCheckedSubtract() {
    for (long a : ALL_LONG_CANDIDATES) {
      for (long b : ALL_LONG_CANDIDATES) {
        BigInteger expectedResult = valueOf(a).subtract(valueOf(b));
        boolean expectedSuccess = fitsInLong(expectedResult);
        try {
          assertEquals(a - b, LongMath.checkedSubtract(a, b));
          assertTrue(expectedSuccess);
        } catch (ArithmeticException e) {
          if (expectedSuccess) {
            failFormat(
                "expected checkedSubtract(%s, %s) = %s; got ArithmeticException",
                a, b, expectedResult);
          }
        }
      }
    }
  }

  @AndroidIncompatible // slow
  public void testCheckedMultiply() {
    boolean isAndroid = TestPlatform.isAndroid();
    for (long a : ALL_LONG_CANDIDATES) {
      for (long b : ALL_LONG_CANDIDATES) {
        if (isAndroid && a == -4294967296L && b == 2147483648L) {
          /*
           * Bug in older versions of Android we test against, since fixed: -9223372036854775808L /
           * -4294967296L = -9223372036854775808L!
           *
           * To be clear, this bug affects not the test's computation of the expected result but the
           * _actual prod code_. But it probably affects only unusual cases.
           */
          continue;
        }
        BigInteger expectedResult = valueOf(a).multiply(valueOf(b));
        boolean expectedSuccess = fitsInLong(expectedResult);
        try {
          assertEquals(a * b, LongMath.checkedMultiply(a, b));
          assertTrue(expectedSuccess);
        } catch (ArithmeticException e) {
          if (expectedSuccess) {
            failFormat(
                "expected checkedMultiply(%s, %s) = %s; got ArithmeticException",
                a, b, expectedResult);
          }
        }
      }
    }
  }

  @GwtIncompatible // TODO
  public void testCheckedPow() {
    for (long b : ALL_LONG_CANDIDATES) {
      for (int exp : EXPONENTS) {
        BigInteger expectedResult = valueOf(b).pow(exp);
        boolean expectedSuccess = fitsInLong(expectedResult);
        try {
          assertEquals(expectedResult.longValue(), LongMath.checkedPow(b, exp));
          assertTrue(expectedSuccess);
        } catch (ArithmeticException e) {
          if (expectedSuccess) {
            failFormat(
                "expected checkedPow(%s, %s) = %s; got ArithmeticException",
                b, exp, expectedResult);
          }
        }
      }
    }
  }

  @AndroidIncompatible // slow
  @GwtIncompatible // TODO
  public void testSaturatedAdd() {
    for (long a : ALL_LONG_CANDIDATES) {
      for (long b : ALL_LONG_CANDIDATES) {
        assertOperationEquals(
            a, b, "s+", saturatedCast(valueOf(a).add(valueOf(b))), LongMath.saturatedAdd(a, b));
      }
    }
  }

  @AndroidIncompatible // slow
  @GwtIncompatible // TODO
  public void testSaturatedSubtract() {
    for (long a : ALL_LONG_CANDIDATES) {
      for (long b : ALL_LONG_CANDIDATES) {
        assertOperationEquals(
            a,
            b,
            "s-",
            saturatedCast(valueOf(a).subtract(valueOf(b))),
            LongMath.saturatedSubtract(a, b));
      }
    }
  }

  @AndroidIncompatible // slow
  @GwtIncompatible // TODO
  public void testSaturatedMultiply() {
    for (long a : ALL_LONG_CANDIDATES) {
      for (long b : ALL_LONG_CANDIDATES) {
        assertOperationEquals(
            a,
            b,
            "s*",
            saturatedCast(valueOf(a).multiply(valueOf(b))),
            LongMath.saturatedMultiply(a, b));
      }
    }
  }

  @GwtIncompatible // TODO
  public void testSaturatedPow() {
    for (long a : ALL_LONG_CANDIDATES) {
      for (int b : EXPONENTS) {
        assertOperationEquals(
            a, b, "s^", saturatedCast(valueOf(a).pow(b)), LongMath.saturatedPow(a, b));
      }
    }
  }

  private void assertOperationEquals(long a, long b, String op, long expected, long actual) {
    if (expected != actual) {
      fail("Expected for " + a + " " + op + " " + b + " = " + expected + ", but got " + actual);
    }
  }

  // Depends on the correctness of BigIntegerMath.factorial.
  @GwtIncompatible // TODO
  public void testFactorial() {
    for (int n = 0; n <= 50; n++) {
      BigInteger expectedBig = BigIntegerMath.factorial(n);
      long expectedLong = fitsInLong(expectedBig) ? expectedBig.longValue() : Long.MAX_VALUE;
      assertEquals(expectedLong, LongMath.factorial(n));
    }
  }

  @GwtIncompatible // TODO
  public void testFactorialNegative() {
    for (int n : NEGATIVE_INTEGER_CANDIDATES) {
      try {
        LongMath.factorial(n);
        fail("Expected IllegalArgumentException");
      } catch (IllegalArgumentException expected) {
      }
    }
  }

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

  @GwtIncompatible // Slow
  public void testBinomial_exhaustiveNotOverflowing() {
    // Tests all of the inputs to LongMath.binomial that won't cause it to overflow, that weren't
    // tested in the previous method, for k >= 3.
    for (int k = 3; k < LongMath.biggestBinomials.length; k++) {
      for (int n = 70; n <= LongMath.biggestBinomials[k]; n++) {
        assertEquals(BigIntegerMath.binomial(n, k).longValue(), LongMath.binomial(n, k));
      }
    }
  }

  public void testBinomialOutside() {
    for (int n = 0; n <= 50; n++) {
      try {
        LongMath.binomial(n, -1);
        fail("Expected IllegalArgumentException");
      } catch (IllegalArgumentException expected) {
      }
      try {
        LongMath.binomial(n, n + 1);
        fail("Expected IllegalArgumentException");
      } catch (IllegalArgumentException expected) {
      }
    }
  }

  public void testBinomialNegative() {
    for (int n : NEGATIVE_INTEGER_CANDIDATES) {
      try {
        LongMath.binomial(n, 0);
        fail("Expected IllegalArgumentException");
      } catch (IllegalArgumentException expected) {
      }
    }
  }

  @GwtIncompatible // far too slow
  public void testSqrtOfPerfectSquareAsDoubleIsPerfect() {
    // This takes just over a minute on my machine.
    for (long n = 0; n <= LongMath.FLOOR_SQRT_MAX_LONG; n++) {
      long actual = (long) Math.sqrt(n * n);
      assertTrue(actual == n);
    }
  }

  public void testSqrtOfLongIsAtMostFloorSqrtMaxLong() {
    long sqrtMaxLong = (long) Math.sqrt(Long.MAX_VALUE);
    assertTrue(sqrtMaxLong <= LongMath.FLOOR_SQRT_MAX_LONG);
  }

  @AndroidIncompatible // slow
  @GwtIncompatible // java.math.BigInteger
  public void testMean() {
    // Odd-sized ranges have an obvious mean
    assertMean(2, 1, 3);

    assertMean(-2, -3, -1);
    assertMean(0, -1, 1);
    assertMean(1, -1, 3);
    assertMean((1L << 62) - 1, -1, Long.MAX_VALUE);

    // Even-sized ranges should prefer the lower mean
    assertMean(2, 1, 4);
    assertMean(-3, -4, -1);
    assertMean(0, -1, 2);
    assertMean(0, Long.MIN_VALUE + 2, Long.MAX_VALUE);
    assertMean(0, 0, 1);
    assertMean(-1, -1, 0);
    assertMean(-1, Long.MIN_VALUE, Long.MAX_VALUE);

    // x == y == mean
    assertMean(1, 1, 1);
    assertMean(0, 0, 0);
    assertMean(-1, -1, -1);
    assertMean(Long.MIN_VALUE, Long.MIN_VALUE, Long.MIN_VALUE);
    assertMean(Long.MAX_VALUE, Long.MAX_VALUE, Long.MAX_VALUE);

    // Exhaustive checks
    for (long x : ALL_LONG_CANDIDATES) {
      for (long y : ALL_LONG_CANDIDATES) {
        assertMean(x, y);
      }
    }
  }

  /** Helper method that asserts the arithmetic mean of x and y is equal to the expectedMean. */
  private static void assertMean(long expectedMean, long x, long y) {
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
  private static void assertMean(long x, long y) {
    long expectedMean = computeMeanSafely(x, y);
    assertEquals(expectedMean, LongMath.mean(x, y));
    assertEquals(
        "The mean of x and y should equal the mean of y and x", expectedMean, LongMath.mean(y, x));
  }

  /**
   * Computes the mean in a way that is obvious and resilient to overflow by using BigInteger
   * arithmetic.
   */
  private static long computeMeanSafely(long x, long y) {
    BigInteger bigX = BigInteger.valueOf(x);
    BigInteger bigY = BigInteger.valueOf(y);
    BigDecimal bigMean =
        new BigDecimal(bigX.add(bigY)).divide(BigDecimal.valueOf(2), BigDecimal.ROUND_FLOOR);
    // parseInt blows up on overflow as opposed to intValue() which does not.
    return Long.parseLong(bigMean.toString());
  }

  private static boolean fitsInLong(BigInteger big) {
    return big.bitLength() <= 63;
  }

  private static final BigInteger MAX_LONG = BigInteger.valueOf(Long.MAX_VALUE);
  private static final BigInteger MIN_LONG = BigInteger.valueOf(Long.MIN_VALUE);

  private static long saturatedCast(BigInteger big) {
    if (big.compareTo(MAX_LONG) > 0) {
      return Long.MAX_VALUE;
    }
    if (big.compareTo(MIN_LONG) < 0) {
      return Long.MIN_VALUE;
    }
    return big.longValue();
  }

  @GwtIncompatible // NullPointerTester
  public void testNullPointers() {
    NullPointerTester tester = new NullPointerTester();
    tester.setDefault(int.class, 1);
    tester.setDefault(long.class, 1L);
    tester.testAllPublicStaticMethods(LongMath.class);
  }

  @GwtIncompatible // isPrime is GWT-incompatible
  public void testIsPrimeSmall() {
    // Check the first 1000 integers
    for (int i = 2; i < 1000; i++) {
      assertEquals(BigInteger.valueOf(i).isProbablePrime(100), LongMath.isPrime(i));
    }
  }

  @GwtIncompatible // isPrime is GWT-incompatible
  public void testIsPrimeManyConstants() {
    // Test the thorough test inputs, which also includes special constants in the Miller-Rabin
    // tests.
    for (long l : POSITIVE_LONG_CANDIDATES) {
      assertEquals(BigInteger.valueOf(l).isProbablePrime(100), LongMath.isPrime(l));
    }
  }

  @GwtIncompatible // isPrime is GWT-incompatible
  public void testIsPrimeOnUniformRandom() {
    Random rand = new Random(1);
    for (int bits = 10; bits < 63; bits++) {
      for (int i = 0; i < 2000; i++) {
        // A random long between 0 and Long.MAX_VALUE, inclusive.
        long l = rand.nextLong() & ((1L << bits) - 1);
        assertEquals(BigInteger.valueOf(l).isProbablePrime(100), LongMath.isPrime(l));
      }
    }
  }

  @GwtIncompatible // isPrime is GWT-incompatible
  public void testIsPrimeOnRandomPrimes() {
    Random rand = new Random(1);
    for (int bits = 10; bits < 63; bits++) {
      for (int i = 0; i < 100; i++) {
        long p = BigInteger.probablePrime(bits, rand).longValue();
        assertTrue(LongMath.isPrime(p));
      }
    }
  }

  @GwtIncompatible // isPrime is GWT-incompatible
  public void testIsPrimeOnRandomComposites() {
    Random rand = new Random(1);
    for (int bits = 5; bits < 32; bits++) {
      for (int i = 0; i < 100; i++) {
        long p = BigInteger.probablePrime(bits, rand).longValue();
        long q = BigInteger.probablePrime(bits, rand).longValue();
        assertFalse(LongMath.isPrime(p * q));
      }
    }
  }

  @GwtIncompatible // isPrime is GWT-incompatible
  public void testIsPrimeThrowsOnNegative() {
    try {
      LongMath.isPrime(-1);
      fail("Expected IllegalArgumentException");
    } catch (IllegalArgumentException expected) {
    }
  }

  private static void failFormat(String template, Object... args) {
    assert_().fail(template, args);
  }
}
