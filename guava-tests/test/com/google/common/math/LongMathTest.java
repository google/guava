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
import static com.google.common.math.MathTesting.ALL_LONG_CANDIDATES;
import static com.google.common.math.MathTesting.ALL_ROUNDING_MODES;
import static com.google.common.math.MathTesting.ALL_SAFE_ROUNDING_MODES;
import static com.google.common.math.MathTesting.EXPONENTS;
import static com.google.common.math.MathTesting.NEGATIVE_INTEGER_CANDIDATES;
import static com.google.common.math.MathTesting.NEGATIVE_LONG_CANDIDATES;
import static com.google.common.math.MathTesting.NONZERO_LONG_CANDIDATES;
import static com.google.common.math.MathTesting.POSITIVE_INTEGER_CANDIDATES;
import static com.google.common.math.MathTesting.POSITIVE_LONG_CANDIDATES;
import static java.math.BigInteger.valueOf;
import static java.math.RoundingMode.FLOOR;
import static java.math.RoundingMode.UNNECESSARY;

import com.google.common.testing.NullPointerTester;

import junit.framework.TestCase;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;

/**
 * Tests for LongMath.
 *
 * @author Louis Wasserman
 */
public class LongMathTest extends TestCase {
  public void testConstantMaxPowerOfSqrt2Unsigned() {
    assertEquals(BigIntegerMath.sqrt(BigInteger.ZERO.setBit(2 * Long.SIZE - 1), FLOOR).longValue(),
        LongMath.MAX_POWER_OF_SQRT2_UNSIGNED);
  }

  public void testConstantsPowersOf10() {
    for (int i = 0; i < LongMath.POWERS_OF_10.length; i++) {
      assertEquals(LongMath.checkedPow(10, i), LongMath.POWERS_OF_10[i]);
    }
    try {
      LongMath.checkedPow(10, LongMath.POWERS_OF_10.length);
      fail("Expected ArithmeticException");
    } catch (ArithmeticException expected) {}
  }

  public void testConstantsHalfPowersOf10() {
    for (int i = 0; i < LongMath.HALF_POWERS_OF_10.length; i++) {
      assertEquals(BigIntegerMath.sqrt(BigInteger.TEN.pow(2 * i + 1), FLOOR),
          BigInteger.valueOf(LongMath.HALF_POWERS_OF_10[i]));
    }
    BigInteger nextBigger =
        BigIntegerMath.sqrt(BigInteger.TEN.pow(2 * LongMath.HALF_POWERS_OF_10.length + 1), FLOOR);
    assertTrue(nextBigger.compareTo(BigInteger.valueOf(Long.MAX_VALUE)) > 0);
  }

  public void testConstantsSqrtMaxLong() {
    assertEquals(LongMath.sqrt(Long.MAX_VALUE, FLOOR), LongMath.FLOOR_SQRT_MAX_LONG);
  }

  public void testConstantsFactorials() {
    long expected = 1;
    for (int i = 0; i < LongMath.FACTORIALS.length; i++, expected *= i) {
      assertEquals(expected, LongMath.FACTORIALS[i]);
    }
    try {
      LongMath.checkedMultiply(
          LongMath.FACTORIALS[LongMath.FACTORIALS.length - 1], LongMath.FACTORIALS.length);
      fail("Expected ArithmeticException");
    } catch (ArithmeticException expect) {}
  }

  public void testConstantsBiggestBinomials() {
    for (int k = 0; k < LongMath.BIGGEST_BINOMIALS.length; k++) {
      assertTrue(fitsInLong(BigIntegerMath.binomial(LongMath.BIGGEST_BINOMIALS[k], k)));
      assertTrue(LongMath.BIGGEST_BINOMIALS[k] == Integer.MAX_VALUE
          || !fitsInLong(BigIntegerMath.binomial(LongMath.BIGGEST_BINOMIALS[k] + 1, k)));
      // In the first case, any long is valid; in the second, we want to test that the next-bigger
      // long overflows.
    }
    int k = LongMath.BIGGEST_BINOMIALS.length;
    assertFalse(fitsInLong(BigIntegerMath.binomial(2 * k, k)));
    // 2 * k is the smallest value for which we don't replace k with (n-k).
  }

  public void testConstantsBiggestSimpleBinomials() {
    for (int k = 0; k < LongMath.BIGGEST_SIMPLE_BINOMIALS.length; k++) {
      assertTrue(LongMath.BIGGEST_SIMPLE_BINOMIALS[k] <= LongMath.BIGGEST_BINOMIALS[k]);
      simpleBinomial(LongMath.BIGGEST_SIMPLE_BINOMIALS[k], k); // mustn't throw
      if (LongMath.BIGGEST_SIMPLE_BINOMIALS[k] < Integer.MAX_VALUE) {
        // unless all n are fair game with this k
        try {
          simpleBinomial(LongMath.BIGGEST_SIMPLE_BINOMIALS[k] + 1, k);
          fail("Expected ArithmeticException");
        } catch (ArithmeticException expected) {}
      }
    }
    try {
      int k = LongMath.BIGGEST_SIMPLE_BINOMIALS.length;
      simpleBinomial(2 * k, k);
      // 2 * k is the smallest value for which we don't replace k with (n-k).
      fail("Expected ArithmeticException");
    } catch (ArithmeticException expected) {}
  }

  // Throws an ArithmeticException if "the simple implementation" of binomial coefficients overflows
  private long simpleBinomial(int n, int k) {
    long accum = 1;
    for (int i = 0; i < k; i++) {
      accum = LongMath.checkedMultiply(accum, n - i);
      accum /= i + 1;
    }
    return accum;
  }

  public void testIsPowerOfTwo() {
    for (long x : ALL_LONG_CANDIDATES) {
      // Checks for a single bit set.
      boolean expected = x > 0 & (x & (x - 1)) == 0L;
      assertEquals(expected, LongMath.isPowerOfTwo(x));
    }
  }

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

  public void testLog10ZeroAlwaysThrows() {
    for (RoundingMode mode : ALL_ROUNDING_MODES) {
      try {
        LongMath.log10(0L, mode);
        fail("Expected IllegalArgumentException");
      } catch (IllegalArgumentException expected) {}
    }
  }

  public void testLog10NegativeAlwaysThrows() {
    for (long x : NEGATIVE_LONG_CANDIDATES) {
      for (RoundingMode mode : ALL_ROUNDING_MODES) {
        try {
          LongMath.log10(x, mode);
          fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException expected) {}
      }
    }
  }

  // Relies on the correctness of BigIntegerMath.log10 for all modes except UNNECESSARY.
  public void testLog10MatchesBigInteger() {
    for (long x : POSITIVE_LONG_CANDIDATES) {
      for (RoundingMode mode : ALL_SAFE_ROUNDING_MODES) {
        assertEquals(BigIntegerMath.log10(valueOf(x), mode), LongMath.log10(x, mode));
      }
    }
  }

  // Relies on the correctness of log10(long, FLOOR) and of pow(long, int).
  public void testLog10Exact() {
    for (long x : POSITIVE_LONG_CANDIDATES) {
      int floor = LongMath.log10(x, FLOOR);
      boolean expectSuccess = LongMath.pow(10, floor) == x;
      try {
        assertEquals(floor, LongMath.log10(x, UNNECESSARY));
        assertTrue(expectSuccess);
      } catch (ArithmeticException e) {
        assertFalse(expectSuccess);
      }
    }
  }

  public void testLog10TrivialOnPowerOf10() {
    long x = 1000000000000L;
    for (RoundingMode mode : ALL_ROUNDING_MODES) {
      assertEquals(12, LongMath.log10(x, mode));
    }
  }

  public void testSqrtNegativeAlwaysThrows() {
    for (long x : NEGATIVE_LONG_CANDIDATES) {
      for (RoundingMode mode : ALL_ROUNDING_MODES) {
        try {
          LongMath.sqrt(x, mode);
          fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException expected) {}
      }
    }
  }

  // Relies on the correctness of BigIntegerMath.sqrt for all modes except UNNECESSARY.
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
  public void testSqrtExactMatchesFloorOrThrows() {
    for (long x : POSITIVE_LONG_CANDIDATES) {
      long logFloor = LongMath.sqrt(x, FLOOR);
      // We only expect an exception if x was not a perfect square.
      boolean isPerfectSquare = (logFloor * logFloor == x);
      try {
        assertEquals(logFloor, LongMath.sqrt(x, UNNECESSARY));
        assertTrue(isPerfectSquare);
      } catch (ArithmeticException e) {
        assertFalse(isPerfectSquare);
      }
    }
  }

  public void testPow() {
    for (long i : ALL_LONG_CANDIDATES) {
      for (int exp : EXPONENTS) {
        assertEquals(LongMath.pow(i, exp), valueOf(i)
            .pow(exp)
            .longValue());
      }
    }
  }

  public void testDivNonZero() {
    for (long p : NONZERO_LONG_CANDIDATES) {
      for (long q : NONZERO_LONG_CANDIDATES) {
        for (RoundingMode mode : ALL_SAFE_ROUNDING_MODES) {
          long expected =
              new BigDecimal(valueOf(p)).divide(new BigDecimal(valueOf(q)), 0, mode).longValue();
          assertEquals(expected, LongMath.divide(p, q, mode));
        }
      }
    }
  }

  public void testDivNonZeroExact() {
    for (long p : NONZERO_LONG_CANDIDATES) {
      for (long q : NONZERO_LONG_CANDIDATES) {
        boolean dividesEvenly = (p % q) == 0L;

        try {
          assertEquals(p, LongMath.divide(p, q, UNNECESSARY) * q);
          assertTrue(dividesEvenly);
        } catch (ArithmeticException e) {
          assertFalse(dividesEvenly);
        }
      }
    }
  }

  public void testZeroDivIsAlwaysZero() {
    for (long q : NONZERO_LONG_CANDIDATES) {
      for (RoundingMode mode : ALL_ROUNDING_MODES) {
        assertEquals(0L, LongMath.divide(0L, q, mode));
      }
    }
  }

  public void testDivByZeroAlwaysFails() {
    for (long p : ALL_LONG_CANDIDATES) {
      for (RoundingMode mode : ALL_ROUNDING_MODES) {
        try {
          LongMath.divide(p, 0L, mode);
          fail("Expected ArithmeticException");
        } catch (ArithmeticException expected) {}
      }
    }
  }

  public void testIntMod() {
    for (long x : ALL_LONG_CANDIDATES) {
      for (int m : POSITIVE_INTEGER_CANDIDATES) {
        assertEquals(valueOf(x)
            .mod(valueOf(m))
            .intValue(), LongMath.mod(x, m));
      }
    }
  }

  public void testIntModNegativeModulusFails() {
    for (long x : ALL_LONG_CANDIDATES) {
      for (int m : NEGATIVE_INTEGER_CANDIDATES) {
        try {
          LongMath.mod(x, m);
          fail("Expected ArithmeticException");
        } catch (ArithmeticException expected) {}
      }
    }
  }

  public void testIntModZeroModulusFails() {
    for (long x : ALL_LONG_CANDIDATES) {
      try {
        LongMath.mod(x, 0);
        fail("Expected AE");
      } catch (ArithmeticException expected) {}
    }
  }

  public void testMod() {
    for (long x : ALL_LONG_CANDIDATES) {
      for (long m : POSITIVE_LONG_CANDIDATES) {
        assertEquals(valueOf(x)
            .mod(valueOf(m))
            .longValue(), LongMath.mod(x, m));
      }
    }
  }

  public void testModNegativeModulusFails() {
    for (long x : ALL_LONG_CANDIDATES) {
      for (long m : NEGATIVE_LONG_CANDIDATES) {
        try {
          LongMath.mod(x, m);
          fail("Expected ArithmeticException");
        } catch (ArithmeticException expected) {}
      }
    }
  }

  public void testGCD() {
    for (long a : POSITIVE_LONG_CANDIDATES) {
      for (long b : POSITIVE_LONG_CANDIDATES) {
        assertEquals(valueOf(a).gcd(valueOf(b)), valueOf(LongMath.gcd(a, b)));
      }
    }
  }

  public void testGCDZero() {
    for (long a : POSITIVE_LONG_CANDIDATES) {
      assertEquals(a, LongMath.gcd(a, 0));
      assertEquals(a, LongMath.gcd(0, a));
    }
    assertEquals(0, LongMath.gcd(0, 0));
  }

  public void testGCDNegativePositiveThrows() {
    for (long a : NEGATIVE_LONG_CANDIDATES) {
      try {
        LongMath.gcd(a, 3);
        fail("Expected IllegalArgumentException");
      } catch (IllegalArgumentException expected) {}
      try {
        LongMath.gcd(3, a);
        fail("Expected IllegalArgumentException");
      } catch (IllegalArgumentException expected) {}
    }
  }

  public void testGCDNegativeZeroThrows() {
    for (long a : NEGATIVE_LONG_CANDIDATES) {
      try {
        LongMath.gcd(a, 0);
        fail("Expected IllegalArgumentException");
      } catch (IllegalArgumentException expected) {}
      try {
        LongMath.gcd(0, a);
        fail("Expected IllegalArgumentException");
      } catch (IllegalArgumentException expected) {}
    }
  }

  public void testCheckedAdd() {
    for (long a : ALL_INTEGER_CANDIDATES) {
      for (long b : ALL_INTEGER_CANDIDATES) {
        BigInteger expectedResult = valueOf(a).add(valueOf(b));
        boolean expectedSuccess = fitsInLong(expectedResult);
        try {
          assertEquals(a + b, LongMath.checkedAdd(a, b));
          assertTrue(expectedSuccess);
        } catch (ArithmeticException e) {
          assertFalse(expectedSuccess);
        }
      }
    }
  }

  public void testCheckedSubtract() {
    for (long a : ALL_INTEGER_CANDIDATES) {
      for (long b : ALL_INTEGER_CANDIDATES) {
        BigInteger expectedResult = valueOf(a).subtract(valueOf(b));
        boolean expectedSuccess = fitsInLong(expectedResult);
        try {
          assertEquals(a - b, LongMath.checkedSubtract(a, b));
          assertTrue(expectedSuccess);
        } catch (ArithmeticException e) {
          assertFalse(expectedSuccess);
        }
      }
    }
  }

  public void testCheckedMultiply() {
    for (long a : ALL_INTEGER_CANDIDATES) {
      for (long b : ALL_INTEGER_CANDIDATES) {
        BigInteger expectedResult = valueOf(a).multiply(valueOf(b));
        boolean expectedSuccess = fitsInLong(expectedResult);
        try {
          assertEquals(a * b, LongMath.checkedMultiply(a, b));
          assertTrue(expectedSuccess);
        } catch (ArithmeticException e) {
          assertFalse(expectedSuccess);
        }
      }
    }
  }

  public void testCheckedPow() {
    for (long b : ALL_INTEGER_CANDIDATES) {
      for (int exp : EXPONENTS) {
        BigInteger expectedResult = valueOf(b).pow(exp);
        boolean expectedSuccess = fitsInLong(expectedResult);
        try {
          assertEquals(expectedResult.longValue(), LongMath.checkedPow(b, exp));
          assertTrue(expectedSuccess);
        } catch (ArithmeticException e) {
          assertFalse(expectedSuccess);
        }
      }
    }
  }

  // Depends on the correctness of BigIntegerMath.factorial.
  public void testFactorial() {
    for (int n = 0; n <= 50; n++) {
      BigInteger expectedBig = BigIntegerMath.factorial(n);
      long expectedLong = fitsInLong(expectedBig) ? expectedBig.longValue() : Long.MAX_VALUE;
      assertEquals(expectedLong, LongMath.factorial(n));
    }
  }

  public void testFactorialNegative() {
    for (int n : NEGATIVE_INTEGER_CANDIDATES) {
      try {
        LongMath.factorial(n);
        fail("Expected IllegalArgumentException");
      } catch (IllegalArgumentException expected) {}
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

  private boolean fitsInLong(BigInteger big) {
    return big.bitLength() <= 63;
  }

  public void testNullPointers() throws Exception {
    NullPointerTester tester = new NullPointerTester();
    tester.setDefault(RoundingMode.class, FLOOR);
    tester.setDefault(int.class, 1);
    tester.setDefault(long.class, 1L);
    tester.testAllPublicStaticMethods(LongMath.class);
  }
}
