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
import static java.math.RoundingMode.UNNECESSARY;

import com.google.common.annotations.GwtCompatible;

import junit.framework.TestCase;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;

/**
 * Tests for {@link IntMath}.
 *
 * @author Louis Wasserman
 */
@GwtCompatible(emulated = true)
public class IntMathTest extends TestCase {
  
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

  public void testLog2ZeroAlwaysThrows() {
    for (RoundingMode mode : ALL_ROUNDING_MODES) {
      try {
        IntMath.log2(0, mode);
        fail("Expected IllegalArgumentException");
      } catch (IllegalArgumentException expected) {}
    }
  }

  public void testLog2NegativeAlwaysThrows() {
    for (int x : NEGATIVE_INTEGER_CANDIDATES) {
      for (RoundingMode mode : ALL_ROUNDING_MODES) {
        try {
          IntMath.log2(x, mode);
          fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException expected) {}
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

  // Relies on the correctness of BigIntegerMath.log10 for all modes except UNNECESSARY.

  // Relies on the correctness of log10(int, FLOOR) and of pow(int, int).

  // Simple test to cover sqrt(0) for all types and all modes.

  /* Relies on the correctness of BigIntegerMath.sqrt for all modes except UNNECESSARY. */

  /* Relies on the correctness of sqrt(int, FLOOR). */

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
        } catch (ArithmeticException expected) {}
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
        } catch (ArithmeticException expected) {}
      }
    }
  }

  public void testModZeroModulusFails() {
    for (int x : ALL_INTEGER_CANDIDATES) {
      try {
        IntMath.mod(x, 0);
        fail("Expected ArithmeticException");
      } catch (ArithmeticException expected) {}
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
      } catch (IllegalArgumentException expected) {}
      try {
        IntMath.gcd(3, a);
        fail("Expected IllegalArgumentException");
      } catch (IllegalArgumentException expected) {}
    }
  }

  public void testGCDNegativeZeroThrows() {
    for (int a : NEGATIVE_INTEGER_CANDIDATES) {
      try {
        IntMath.gcd(a, 0);
        fail("Expected IllegalArgumentException");
      } catch (IllegalArgumentException expected) {}
      try {
        IntMath.gcd(0, a);
        fail("Expected IllegalArgumentException");
      } catch (IllegalArgumentException expected) {}
    }
  }

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
      } catch (IllegalArgumentException expected) {}
    }
  }

  // Depends on the correctness of BigIntegerMath.binomial.

  /**
   * Helper method that asserts the arithmetic mean of x and y is equal
   * to the expectedMean.
   */
  private static void assertMean(int expectedMean, int x, int y) {
    assertEquals("The expectedMean should be the same as computeMeanSafely",
        expectedMean, computeMeanSafely(x, y));
    assertMean(x, y);
  }

  /**
   * Helper method that asserts the arithmetic mean of x and y is equal
   * to the result of computeMeanSafely.
   */
  private static void assertMean(int x, int y) {
    int expectedMean = computeMeanSafely(x, y);
    assertEquals(expectedMean, IntMath.mean(x, y));
    assertEquals("The mean of x and y should equal the mean of y and x",
        expectedMean, IntMath.mean(y, x));
  }

  /**
   * Computes the mean in a way that is obvious and resilient to
   * overflow by using BigInteger arithmetic.
   */
  private static int computeMeanSafely(int x, int y) {
    BigInteger bigX = BigInteger.valueOf(x);
    BigInteger bigY = BigInteger.valueOf(y);
    BigDecimal bigMean = new BigDecimal(bigX.add(bigY))
        .divide(BigDecimal.valueOf(2), BigDecimal.ROUND_FLOOR);
    // parseInt blows up on overflow as opposed to intValue() which does not.
    return Integer.parseInt(bigMean.toString());
  }

  private static boolean fitsInInt(BigInteger big) {
    return big.bitLength() <= 31;
  }

  private static int force32(int value) {
    // GWT doesn't consistently overflow values to make them 32-bit, so we need to force it.
    return value & 0xffffffff;
  }
}

