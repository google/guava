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
import static java.math.BigInteger.valueOf;
import static java.math.RoundingMode.FLOOR;
import static java.math.RoundingMode.UNNECESSARY;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.testing.NullPointerTester;

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
  @GwtIncompatible("BigIntegerMath") // TODO(cpovirk): GWT-enable BigIntegerMath
  public void testConstantMaxPowerOfSqrt2Unsigned() {
    assertEquals(
        BigIntegerMath.sqrt(BigInteger.ZERO.setBit(2 * Integer.SIZE - 1), FLOOR).intValue(),
        IntMath.MAX_POWER_OF_SQRT2_UNSIGNED);
  }

  @GwtIncompatible("pow()")
  public void testConstantsPowersOf10() {
    for (int i = 0; i < IntMath.POWERS_OF_10.length; i++) {
      assertEquals(IntMath.pow(10, i), IntMath.POWERS_OF_10[i]);
    }
  }

  @GwtIncompatible("BigIntegerMath") // TODO(cpovirk): GWT-enable BigIntegerMath
  public void testConstantsHalfPowersOf10() {
    for (int i = 0; i < IntMath.HALF_POWERS_OF_10.length; i++) {
      assert IntMath.HALF_POWERS_OF_10[i]
          == Math.min(Integer.MAX_VALUE,
              BigIntegerMath.sqrt(BigInteger.TEN.pow(2 * i + 1), FLOOR).longValue());
    }
  }

  @GwtIncompatible("BigIntegerMath") // TODO(cpovirk): GWT-enable BigIntegerMath
  public void testConstantsBiggestBinomials(){
    for (int k = 0; k < IntMath.BIGGEST_BINOMIALS.length; k++) {
      assertTrue(fitsInInt(BigIntegerMath.binomial(IntMath.BIGGEST_BINOMIALS[k], k)));
      assertTrue(IntMath.BIGGEST_BINOMIALS[k] == Integer.MAX_VALUE
          || !fitsInInt(BigIntegerMath.binomial(IntMath.BIGGEST_BINOMIALS[k] + 1, k)));
      // In the first case, any int is valid; in the second, we want to test that the next-bigger
      // int overflows.
    }
    assertFalse(
        fitsInInt(BigIntegerMath.binomial(
            2 * IntMath.BIGGEST_BINOMIALS.length, IntMath.BIGGEST_BINOMIALS.length)));
  }
  
  @GwtIncompatible("sqrt")
  public void testPowersSqrtMaxInt() {
    assertEquals(IntMath.sqrt(Integer.MAX_VALUE, FLOOR), IntMath.FLOOR_SQRT_MAX_INT);
  }

  public void testIsPowerOfTwo() {
    for (int x : ALL_INTEGER_CANDIDATES) {
      // Checks for a single bit set.
      boolean expected = x > 0 & (x & (x - 1)) == 0;
      assertEquals(expected, IntMath.isPowerOfTwo(x));
    }
  }

  @GwtIncompatible("log2")
  public void testLog2ZeroAlwaysThrows() {
    for (RoundingMode mode : ALL_ROUNDING_MODES) {
      try {
        IntMath.log2(0, mode);
        fail("Expected IllegalArgumentException");
      } catch (IllegalArgumentException expected) {}
    }
  }

  @GwtIncompatible("log2")
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
  @GwtIncompatible("BigIntegerMath") // TODO(cpovirk): GWT-enable BigIntegerMath
  public void testLog2MatchesBigInteger() {
    for (int x : POSITIVE_INTEGER_CANDIDATES) {
      for (RoundingMode mode : ALL_SAFE_ROUNDING_MODES) {
        assertEquals(BigIntegerMath.log2(valueOf(x), mode), IntMath.log2(x, mode));
      }
    }
  }

  // Relies on the correctness of isPowerOfTwo(int).
  @GwtIncompatible("log2")
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

  @GwtIncompatible("log10")
  public void testLog10ZeroAlwaysThrows() {
    for (RoundingMode mode : ALL_ROUNDING_MODES) {
      try {
        IntMath.log10(0, mode);
        fail("Expected IllegalArgumentException");
      } catch (IllegalArgumentException expected) {}
    }
  }

  @GwtIncompatible("log10")
  public void testLog10NegativeAlwaysThrows() {
    for (int x : NEGATIVE_INTEGER_CANDIDATES) {
      for (RoundingMode mode : ALL_ROUNDING_MODES) {
        try {
          IntMath.log10(x, mode);
          fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException expected) {}
      }
    }
  }

  // Relies on the correctness of BigIntegerMath.log10 for all modes except UNNECESSARY.
  @GwtIncompatible("BigIntegerMath") // TODO(cpovirk): GWT-enable BigIntegerMath
  public void testLog10MatchesBigInteger() {
    for (int x : POSITIVE_INTEGER_CANDIDATES) {
      for (RoundingMode mode : ALL_SAFE_ROUNDING_MODES) {
        // The BigInteger implementation is tested separately, use it as the reference.
        assertEquals(BigIntegerMath.log10(valueOf(x), mode), IntMath.log10(x, mode));
      }
    }
  }

  // Relies on the correctness of log10(int, FLOOR) and of pow(int, int).
  @GwtIncompatible("pow()")
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

  @GwtIncompatible("log10")
  public void testLog10TrivialOnPowerOfTen() {
    int x = 1000000;
    for (RoundingMode mode : ALL_ROUNDING_MODES) {
      assertEquals(6, IntMath.log10(x, mode));
    }
  }

  // Simple test to cover sqrt(0) for all types and all modes.
  @GwtIncompatible("sqrt")
  public void testSqrtZeroAlwaysZero() {
    for (RoundingMode mode : ALL_ROUNDING_MODES) {
      assertEquals(0, IntMath.sqrt(0, mode));
    }
  }

  @GwtIncompatible("sqrt")
  public void testSqrtNegativeAlwaysThrows() {
    for (int x : NEGATIVE_INTEGER_CANDIDATES) {
      for (RoundingMode mode : RoundingMode.values()) {
        try {
          IntMath.sqrt(x, mode);
          fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException expected) {}
      }
    }
  }

  /* Relies on the correctness of BigIntegerMath.sqrt for all modes except UNNECESSARY. */
  @GwtIncompatible("BigIntegerMath") // TODO(cpovirk): GWT-enable BigIntegerMath
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
  @GwtIncompatible("sqrt")
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

  @GwtIncompatible("2147483646^2 expected=4")
  public void testPow() {
    for (int i : ALL_INTEGER_CANDIDATES) {
      for (int pow : EXPONENTS) {
        assertEquals(i + "^" + pow, BigInteger.valueOf(i).pow(pow).intValue(), IntMath.pow(i, pow));
      }
    }
  }

  @GwtIncompatible("-2147483648/1 expected=2147483648")
  public void testDivNonZero() {
    for (int p : NONZERO_INTEGER_CANDIDATES) {
      for (int q : NONZERO_INTEGER_CANDIDATES) {
        for (RoundingMode mode : ALL_SAFE_ROUNDING_MODES) {
          int expected =
              new BigDecimal(valueOf(p)).divide(new BigDecimal(valueOf(q)), 0, mode).intValue();
          assertEquals(p + "/" + q, expected, IntMath.divide(p, q, mode));
        }
      }
    }
  }

  @GwtIncompatible("-2147483648/-1 not expected to divide evenly")
  public void testDivNonZeroExact() {
    for (int p : NONZERO_INTEGER_CANDIDATES) {
      for (int q : NONZERO_INTEGER_CANDIDATES) {
        boolean dividesEvenly = (p % q) == 0;

        try {
          assertEquals(p + "/" + q, p, IntMath.divide(p, q, UNNECESSARY) * q);
          assertTrue(p + "/" + q + " expected to divide evenly", dividesEvenly);
        } catch (ArithmeticException e) {
          assertFalse(p + "/" + q + " not expected to divide evenly", dividesEvenly);
        }
      }
    }
  }

  @GwtIncompatible("pow()")
  public void testZeroDivIsAlwaysZero() {
    for (int q : NONZERO_INTEGER_CANDIDATES) {
      for (RoundingMode mode : ALL_ROUNDING_MODES) {
        assertEquals(0, IntMath.divide(0, q, mode));
      }
    }
  }

  @GwtIncompatible("pow()")
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

  @GwtIncompatible("-2147483648^1 expected=2147483648")
  public void testCheckedPow() {
    for (int b : ALL_INTEGER_CANDIDATES) {
      for (int k : EXPONENTS) {
        BigInteger expectedResult = valueOf(b).pow(k);
        boolean expectedSuccess = fitsInInt(expectedResult);
        try {
          assertEquals(b + "^" + k, expectedResult.intValue(), IntMath.checkedPow(b, k));
          assertTrue(b + "^" + k + " should have succeeded", expectedSuccess);
        } catch (ArithmeticException e) {
          assertFalse(b + "^" + k + " should have failed", expectedSuccess);
        }
      }
    }
  }

  // Depends on the correctness of BigIntegerMath.factorial.
  @GwtIncompatible("BigIntegerMath") // TODO(cpovirk): GWT-enable BigIntegerMath
  public void testFactorial() {
    for (int n = 0; n <= 50; n++) {
      BigInteger expectedBig = BigIntegerMath.factorial(n);
      int expectedInt = fitsInInt(expectedBig) ? expectedBig.intValue() : Integer.MAX_VALUE;
      assertEquals(expectedInt, IntMath.factorial(n));
    }
  }

  @GwtIncompatible("factorial")
  public void testFactorialNegative() {
    for (int n : NEGATIVE_INTEGER_CANDIDATES) {
      try {
        IntMath.factorial(n);
        fail("Expected IllegalArgumentException");
      } catch (IllegalArgumentException expected) {}
    }
  }

  // Depends on the correctness of BigIntegerMath.binomial.
  @GwtIncompatible("BigIntegerMath") // TODO(cpovirk): GWT-enable BigIntegerMath
  public void testBinomial() {
    for (int n = 0; n <= 50; n++) {
      for (int k = 0; k <= n; k++) {
        BigInteger expectedBig = BigIntegerMath.binomial(n, k);
        int expectedInt = fitsInInt(expectedBig) ? expectedBig.intValue() : Integer.MAX_VALUE;
        assertEquals(expectedInt, IntMath.binomial(n, k));
      }
    }
  }

  @GwtIncompatible("binomial")
  public void testBinomialOutside() {
    for (int n = 0; n <= 50; n++) {
      try {
        IntMath.binomial(n, -1);
        fail("Expected IllegalArgumentException");
      } catch (IllegalArgumentException expected) {}
      try {
        IntMath.binomial(n, n + 1);
        fail("Expected IllegalArgumentException");
      } catch (IllegalArgumentException expected) {}
    }
  }

  @GwtIncompatible("binomial")
  public void testBinomialNegative() {
    for (int n : NEGATIVE_INTEGER_CANDIDATES) {
      try {
        IntMath.binomial(n, 0);
        fail("Expected IllegalArgumentException");
      } catch (IllegalArgumentException expected) {}
    }
  }

  private boolean fitsInInt(BigInteger big) {
    return big.bitLength() <= 31;
  }
  
  @GwtIncompatible("NullPointerTester")
  public void testNullPointers() throws Exception {
    NullPointerTester tester = new NullPointerTester();
    tester.setDefault(int.class, 1);
    tester.setDefault(RoundingMode.class, FLOOR);
    tester.testAllPublicStaticMethods(IntMath.class);
  }
}
