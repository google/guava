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

import static com.google.common.math.BigIntegerMath.sqrt;
import static com.google.common.math.IntMath.checkedAdd;
import static com.google.common.math.IntMath.checkedMultiply;
import static com.google.common.math.IntMath.checkedSubtract;
import static com.google.common.math.IntMath.sqrt;
import static com.google.common.math.MathTesting.ALL_INTEGER_CANDIDATES;
import static com.google.common.math.MathTesting.ALL_ROUNDING_MODES;
import static com.google.common.math.MathTesting.ALL_SAFE_ROUNDING_MODES;
import static com.google.common.math.MathTesting.EXPONENTS;
import static com.google.common.math.MathTesting.NEGATIVE_INTEGER_CANDIDATES;
import static com.google.common.math.MathTesting.NONZERO_INTEGER_CANDIDATES;
import static com.google.common.math.MathTesting.POSITIVE_INTEGER_CANDIDATES;
import static com.google.common.math.TestPlatform.intsCanGoOutOfRange;
import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;
import static java.lang.Math.min;
import static java.math.RoundingMode.DOWN;
import static java.math.RoundingMode.FLOOR;
import static java.math.RoundingMode.UNNECESSARY;
import static org.junit.Assert.assertThrows;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.annotations.J2ktIncompatible;
import com.google.common.testing.NullPointerTester;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.Random;
import junit.framework.TestCase;
import org.jspecify.annotations.NullUnmarked;

/**
 * Tests for {@link IntMath}.
 *
 * @author Louis Wasserman
 */
@GwtCompatible
@NullUnmarked
@SuppressWarnings("IntMathMod") // We are testing IntMathMod against alternatives.
public class IntMathTest extends TestCase {
  public void testMaxSignedPowerOfTwo() {
    assertThat(IntMath.isPowerOfTwo(IntMath.MAX_SIGNED_POWER_OF_TWO)).isTrue();

    // Extra work required to make GWT happy.
    long value = IntMath.MAX_SIGNED_POWER_OF_TWO * 2L;
    assertThat(IntMath.isPowerOfTwo((int) value)).isFalse();
  }

  public void testCeilingPowerOfTwo() {
    for (int x : POSITIVE_INTEGER_CANDIDATES) {
      BigInteger expectedResult = BigIntegerMath.ceilingPowerOfTwo(bigInt(x));
      if (fitsInInt(expectedResult)) {
        assertThat(IntMath.ceilingPowerOfTwo(x)).isEqualTo(expectedResult.intValue());
      }
    }
  }

  public void testCeilingPowerOfTwo_overflows() {
    for (int x : POSITIVE_INTEGER_CANDIDATES) {
      BigInteger expectedResult = BigIntegerMath.ceilingPowerOfTwo(bigInt(x));
      if (!fitsInInt(expectedResult)) {
        assertThrows(ArithmeticException.class, () -> IntMath.ceilingPowerOfTwo(x));
      }
    }
  }

  public void testFloorPowerOfTwo() {
    for (int x : POSITIVE_INTEGER_CANDIDATES) {
      BigInteger expectedResult = BigIntegerMath.floorPowerOfTwo(bigInt(x));
      assertThat(IntMath.floorPowerOfTwo(x)).isEqualTo(expectedResult.intValue());
    }
  }

  public void testCeilingPowerOfTwoNegative() {
    for (int x : NEGATIVE_INTEGER_CANDIDATES) {
      assertThrows(IllegalArgumentException.class, () -> IntMath.ceilingPowerOfTwo(x));
    }
  }

  public void testFloorPowerOfTwoNegative() {
    for (int x : NEGATIVE_INTEGER_CANDIDATES) {
      assertThrows(IllegalArgumentException.class, () -> IntMath.floorPowerOfTwo(x));
    }
  }

  public void testCeilingPowerOfTwoZero() {
    assertThrows(IllegalArgumentException.class, () -> IntMath.ceilingPowerOfTwo(0));
  }

  public void testFloorPowerOfTwoZero() {
    assertThrows(IllegalArgumentException.class, () -> IntMath.floorPowerOfTwo(0));
  }

  // We want to test that we've defined the constant with the correct value.
  @SuppressWarnings("TruthConstantAsserts")
  @GwtIncompatible // BigIntegerMath // TODO(cpovirk): GWT-enable BigIntegerMath
  public void testConstantMaxPowerOfSqrt2Unsigned() {
    assertThat(IntMath.MAX_POWER_OF_SQRT2_UNSIGNED)
        .isEqualTo(sqrt(BigInteger.ZERO.setBit(2 * Integer.SIZE - 1), FLOOR).intValue());
  }

  @GwtIncompatible // pow()
  public void testConstantsPowersOf10() {
    for (int i = 0; i < IntMath.powersOf10.length - 1; i++) {
      assertThat(IntMath.powersOf10[i]).isEqualTo(IntMath.pow(10, i));
    }
  }

  @GwtIncompatible // BigIntegerMath // TODO(cpovirk): GWT-enable BigIntegerMath
  public void testMaxLog10ForLeadingZeros() {
    for (int i = 0; i < Integer.SIZE; i++) {
      assertThat(IntMath.maxLog10ForLeadingZeros[i])
          .isEqualTo(BigIntegerMath.log10(BigInteger.ONE.shiftLeft(Integer.SIZE - i), FLOOR));
    }
  }

  @GwtIncompatible // BigIntegerMath // TODO(cpovirk): GWT-enable BigIntegerMath
  public void testConstantsHalfPowersOf10() {
    for (int i = 0; i < IntMath.halfPowersOf10.length; i++) {
      assertThat(min(Integer.MAX_VALUE, sqrt(BigInteger.TEN.pow(2 * i + 1), FLOOR).longValue()))
          .isEqualTo(IntMath.halfPowersOf10[i]);
    }
  }

  public void testConstantsBiggestBinomials() {
    for (int k = 0; k < IntMath.biggestBinomials.length; k++) {
      assertThat(fitsInInt(BigIntegerMath.binomial(IntMath.biggestBinomials[k], k))).isTrue();
      assertThat(
              IntMath.biggestBinomials[k] == Integer.MAX_VALUE
                  || !fitsInInt(BigIntegerMath.binomial(IntMath.biggestBinomials[k] + 1, k)))
          .isTrue();
      // In the first case, any int is valid; in the second, we want to test that the next-bigger
      // int overflows.
    }
    assertThat(
            fitsInInt(
                BigIntegerMath.binomial(
                    2 * IntMath.biggestBinomials.length, IntMath.biggestBinomials.length)))
        .isFalse();
  }

  // We want to test that we've defined the constant with the correct value.
  @SuppressWarnings("TruthConstantAsserts")
  @GwtIncompatible // sqrt
  public void testPowersSqrtMaxInt() {
    assertThat(IntMath.FLOOR_SQRT_MAX_INT).isEqualTo(sqrt(Integer.MAX_VALUE, FLOOR));
  }

  @AndroidIncompatible // presumably slow
  public void testLessThanBranchFree() {
    for (int x : ALL_INTEGER_CANDIDATES) {
      for (int y : ALL_INTEGER_CANDIDATES) {
        if (LongMath.fitsInInt((long) x - y)) {
          assertThat(IntMath.lessThanBranchFree(x, y)).isEqualTo(x < y ? 1 : 0);
        }
      }
    }
  }

  @GwtIncompatible // java.math.BigInteger
  public void testIsPowerOfTwo() {
    for (int x : ALL_INTEGER_CANDIDATES) {
      // Checks for a single bit set.
      assertThat(IntMath.isPowerOfTwo(x)).isEqualTo(x > 0 && bigInt(x).bitCount() == 1);
    }
  }

  public void testLog2ZeroAlwaysThrows() {
    for (RoundingMode mode : ALL_ROUNDING_MODES) {
      assertThrows(IllegalArgumentException.class, () -> IntMath.log2(0, mode));
    }
  }

  public void testLog2NegativeAlwaysThrows() {
    for (int x : NEGATIVE_INTEGER_CANDIDATES) {
      for (RoundingMode mode : ALL_ROUNDING_MODES) {
        assertThrows(IllegalArgumentException.class, () -> IntMath.log2(x, mode));
      }
    }
  }

  // Relies on the correctness of BigIntegerMath.log2 for all modes except UNNECESSARY.
  public void testLog2MatchesBigInteger() {
    for (int x : POSITIVE_INTEGER_CANDIDATES) {
      for (RoundingMode mode : ALL_SAFE_ROUNDING_MODES) {
        assertThat(IntMath.log2(x, mode)).isEqualTo(BigIntegerMath.log2(bigInt(x), mode));
      }
    }
  }

  // Relies on the correctness of isPowerOfTwo(int).
  public void testLog2Exact() {
    for (int x : POSITIVE_INTEGER_CANDIDATES) {
      if (IntMath.isPowerOfTwo(x)) {
        assertThat(1 << IntMath.log2(x, UNNECESSARY)).isEqualTo(x);
      }
    }
  }

  // Relies on the correctness of isPowerOfTwo(int).
  public void testLog2Exact_notPowerOfTwo() {
    for (int x : POSITIVE_INTEGER_CANDIDATES) {
      if (!IntMath.isPowerOfTwo(x)) {
        assertThrows(ArithmeticException.class, () -> IntMath.log2(x, UNNECESSARY));
      }
    }
  }

  @GwtIncompatible // log10
  public void testLog10ZeroAlwaysThrows() {
    for (RoundingMode mode : ALL_ROUNDING_MODES) {
      assertThrows(IllegalArgumentException.class, () -> IntMath.log10(0, mode));
    }
  }

  @GwtIncompatible // log10
  public void testLog10NegativeAlwaysThrows() {
    for (int x : NEGATIVE_INTEGER_CANDIDATES) {
      for (RoundingMode mode : ALL_ROUNDING_MODES) {
        assertThrows(IllegalArgumentException.class, () -> IntMath.log10(x, mode));
      }
    }
  }

  // Relies on the correctness of BigIntegerMath.log10 for all modes except UNNECESSARY.
  @GwtIncompatible // BigIntegerMath // TODO(cpovirk): GWT-enable BigIntegerMath
  public void testLog10MatchesBigInteger() {
    for (int x : POSITIVE_INTEGER_CANDIDATES) {
      for (RoundingMode mode : ALL_SAFE_ROUNDING_MODES) {
        // The BigInteger implementation is tested separately, use it as the reference.
        assertThat(IntMath.log10(x, mode)).isEqualTo(BigIntegerMath.log10(bigInt(x), mode));
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
        assertThat(IntMath.log10(x, UNNECESSARY)).isEqualTo(floor);
        assertThat(expectSuccess).isTrue();
      } catch (ArithmeticException e) {
        assertThat(expectSuccess).isFalse();
      }
    }
  }

  @GwtIncompatible // log10
  public void testLog10TrivialOnPowerOfTen() {
    int x = 1000000;
    for (RoundingMode mode : ALL_ROUNDING_MODES) {
      assertThat(IntMath.log10(x, mode)).isEqualTo(6);
    }
  }

  // Simple test to cover sqrt(0) for all types and all modes.
  @GwtIncompatible // sqrt
  public void testSqrtZeroAlwaysZero() {
    for (RoundingMode mode : ALL_ROUNDING_MODES) {
      assertThat(sqrt(0, mode)).isEqualTo(0);
    }
  }

  @SuppressWarnings("EnumValuesLoopToEnumSet") // EnumSet.allOf isn't available under J2KT
  @GwtIncompatible // sqrt
  public void testSqrtNegativeAlwaysThrows() {
    for (int x : NEGATIVE_INTEGER_CANDIDATES) {
      for (RoundingMode mode : RoundingMode.values()) {
        assertThrows(IllegalArgumentException.class, () -> sqrt(x, mode));
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
        assertThat(bigInt(sqrt(x, mode))).isEqualTo(sqrt(bigInt(x), mode));
      }
    }
  }

  /* Relies on the correctness of sqrt(int, FLOOR). */
  @GwtIncompatible // sqrt
  public void testSqrtExactMatchesFloorOrThrows() {
    for (int x : POSITIVE_INTEGER_CANDIDATES) {
      int floor = sqrt(x, FLOOR);
      // We only expect an exception if x was not a perfect square.
      boolean isPerfectSquare = floor * floor == x;
      try {
        assertThat(sqrt(x, UNNECESSARY)).isEqualTo(floor);
        assertThat(isPerfectSquare).isTrue();
      } catch (ArithmeticException e) {
        assertThat(isPerfectSquare).isFalse();
      }
    }
  }

  @GwtIncompatible // 2147483646^2 expected=4
  public void testPow() {
    for (int i : ALL_INTEGER_CANDIDATES) {
      for (int pow : EXPONENTS) {
        assertWithMessage("%s^%s", i, pow)
            .that(IntMath.pow(i, pow))
            .isEqualTo(bigInt(i).pow(pow).intValue());
      }
    }
  }

  @AndroidIncompatible // slow
  @GwtIncompatible // Math.floorDiv gets wrong answers for negative divisors
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
              new BigDecimal(bigInt(p)).divide(new BigDecimal(bigInt(q)), 0, mode).intValue();
          assertWithMessage("%s/%s", p, q)
              .that(IntMath.divide(p, q, mode))
              .isEqualTo(force32(expected));
          // Check the assertions we make in the javadoc.
          if (mode == DOWN) {
            assertWithMessage("%s/%s", p, q).that(IntMath.divide(p, q, mode)).isEqualTo(p / q);
          } else if (mode == FLOOR) {
            assertWithMessage("⌊%s/%s⌋", p, q)
                .that(IntMath.divide(p, q, mode))
                .isEqualTo(Math.floorDiv(p, q));
          }
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
          assertWithMessage("%s/%s", p, q).that(IntMath.divide(p, q, UNNECESSARY) * q).isEqualTo(p);
          assertWithMessage("%s/%s not expected to divide evenly", p, q)
              .that(dividesEvenly)
              .isTrue();
        } catch (ArithmeticException e) {
          assertWithMessage("%s/%s expected to divide evenly", p, q).that(dividesEvenly).isFalse();
        }
      }
    }
  }

  public void testZeroDivIsAlwaysZero() {
    for (int q : NONZERO_INTEGER_CANDIDATES) {
      for (RoundingMode mode : ALL_ROUNDING_MODES) {
        assertThat(IntMath.divide(0, q, mode)).isEqualTo(0);
      }
    }
  }

  public void testDivByZeroAlwaysFails() {
    for (int p : ALL_INTEGER_CANDIDATES) {
      for (RoundingMode mode : ALL_ROUNDING_MODES) {
        assertThrows(ArithmeticException.class, () -> IntMath.divide(p, 0, mode));
      }
    }
  }

  public void testMod() {
    for (int x : ALL_INTEGER_CANDIDATES) {
      for (int m : POSITIVE_INTEGER_CANDIDATES) {
        assertThat(IntMath.mod(x, m)).isEqualTo(bigInt(x).mod(bigInt(m)).intValue());
      }
    }
  }

  public void testModNegativeModulusFails() {
    for (int x : POSITIVE_INTEGER_CANDIDATES) {
      for (int m : NEGATIVE_INTEGER_CANDIDATES) {
        assertThrows(ArithmeticException.class, () -> IntMath.mod(x, m));
      }
    }
  }

  public void testModZeroModulusFails() {
    for (int x : ALL_INTEGER_CANDIDATES) {
      assertThrows(ArithmeticException.class, () -> IntMath.mod(x, 0));
    }
  }

  public void testGCD() {
    for (int a : POSITIVE_INTEGER_CANDIDATES) {
      for (int b : POSITIVE_INTEGER_CANDIDATES) {
        assertThat(bigInt(IntMath.gcd(a, b))).isEqualTo(bigInt(a).gcd(bigInt(b)));
      }
    }
  }

  public void testGCDZero() {
    for (int a : POSITIVE_INTEGER_CANDIDATES) {
      assertThat(IntMath.gcd(a, 0)).isEqualTo(a);
      assertThat(IntMath.gcd(0, a)).isEqualTo(a);
    }
    assertThat(IntMath.gcd(0, 0)).isEqualTo(0);
  }

  public void testGCDNegativePositiveThrows() {
    for (int a : NEGATIVE_INTEGER_CANDIDATES) {
      assertThrows(IllegalArgumentException.class, () -> IntMath.gcd(a, 3));
      assertThrows(IllegalArgumentException.class, () -> IntMath.gcd(3, a));
    }
  }

  public void testGCDNegativeZeroThrows() {
    for (int a : NEGATIVE_INTEGER_CANDIDATES) {
      assertThrows(IllegalArgumentException.class, () -> IntMath.gcd(a, 0));
      assertThrows(IllegalArgumentException.class, () -> IntMath.gcd(0, a));
    }
  }

  @AndroidIncompatible // slow
  @SuppressWarnings("InlineMeInliner") // We need to test checkedAdd
  public void testCheckedAdd() {
    for (int a : ALL_INTEGER_CANDIDATES) {
      for (int b : ALL_INTEGER_CANDIDATES) {
        // TODO(cpovirk): Test against Math.addExact instead?
        BigInteger expectedResult = bigInt(a).add(bigInt(b));
        boolean expectedSuccess = fitsInInt(expectedResult);
        try {
          assertThat(checkedAdd(a, b)).isEqualTo(a + b);
          assertThat(expectedSuccess).isTrue();
        } catch (ArithmeticException e) {
          assertThat(expectedSuccess).isFalse();
        }
      }
    }
  }

  @SuppressWarnings("InlineMeInliner") // We need to test checkedSubtract
  @AndroidIncompatible // slow
  public void testCheckedSubtract() {
    for (int a : ALL_INTEGER_CANDIDATES) {
      for (int b : ALL_INTEGER_CANDIDATES) {
        // TODO(cpovirk): Test against Math.subtractExact instead?
        BigInteger expectedResult = bigInt(a).subtract(bigInt(b));
        boolean expectedSuccess = fitsInInt(expectedResult);
        try {
          assertThat(checkedSubtract(a, b)).isEqualTo(a - b);
          assertThat(expectedSuccess).isTrue();
        } catch (ArithmeticException e) {
          assertThat(expectedSuccess).isFalse();
        }
      }
    }
  }

  @SuppressWarnings("InlineMeInliner") // We need to test checkedMultiply
  @AndroidIncompatible // presumably slow
  public void testCheckedMultiply() {
    for (int a : ALL_INTEGER_CANDIDATES) {
      for (int b : ALL_INTEGER_CANDIDATES) {
        // TODO(cpovirk): Test against Math.multiplyExact instead?
        BigInteger expectedResult = bigInt(a).multiply(bigInt(b));
        boolean expectedSuccess = fitsInInt(expectedResult);
        try {
          assertThat(checkedMultiply(a, b)).isEqualTo(a * b);
          assertThat(expectedSuccess).isTrue();
        } catch (ArithmeticException e) {
          assertThat(expectedSuccess).isFalse();
        }
      }
    }
  }

  public void testCheckedPow() {
    for (int b : ALL_INTEGER_CANDIDATES) {
      for (int k : EXPONENTS) {
        BigInteger expectedResult = bigInt(b).pow(k);
        boolean expectedSuccess = fitsInInt(expectedResult);
        try {
          assertWithMessage("%s^%s", b, k)
              .that(IntMath.checkedPow(b, k))
              .isEqualTo(force32(expectedResult.intValue()));
          assertWithMessage("%s^%s should have succeeded", b, k).that(expectedSuccess).isTrue();
        } catch (ArithmeticException e) {
          assertWithMessage("%s^%s should have failed", b, k).that(expectedSuccess).isFalse();
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
            a, b, "s+", saturatedCast(bigInt(a).add(bigInt(b))), IntMath.saturatedAdd(a, b));
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
            saturatedCast(bigInt(a).subtract(bigInt(b))),
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
            saturatedCast(bigInt(a).multiply(bigInt(b))),
            IntMath.saturatedMultiply(a, b));
      }
    }
  }

  @GwtIncompatible // TODO
  public void testSaturatedPow() {
    for (int a : ALL_INTEGER_CANDIDATES) {
      for (int b : EXPONENTS) {
        assertOperationEquals(
            a, b, "s^", saturatedCast(bigInt(a).pow(b)), IntMath.saturatedPow(a, b));
      }
    }
  }

  private static final BigInteger MAX_INT = bigInt(Integer.MAX_VALUE);
  private static final BigInteger MIN_INT = bigInt(Integer.MIN_VALUE);

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
      assertThat(IntMath.factorial(n)).isEqualTo(expectedInt);
    }
  }

  public void testFactorialNegative() {
    for (int n : NEGATIVE_INTEGER_CANDIDATES) {
      assertThrows(IllegalArgumentException.class, () -> IntMath.factorial(n));
    }
  }

  // Depends on the correctness of BigIntegerMath.binomial.
  public void testBinomial() {
    for (int n = 0; n <= 50; n++) {
      for (int k = 0; k <= n; k++) {
        BigInteger expectedBig = BigIntegerMath.binomial(n, k);
        int expectedInt = fitsInInt(expectedBig) ? expectedBig.intValue() : Integer.MAX_VALUE;
        assertThat(IntMath.binomial(n, k)).isEqualTo(expectedInt);
      }
    }
  }

  public void testBinomialOutside() {
    for (int i = 0; i <= 50; i++) {
      int n = i;
      assertThrows(IllegalArgumentException.class, () -> IntMath.binomial(n, -1));
      assertThrows(IllegalArgumentException.class, () -> IntMath.binomial(n, n + 1));
    }
  }

  public void testBinomialNegative() {
    for (int n : NEGATIVE_INTEGER_CANDIDATES) {
      assertThrows(IllegalArgumentException.class, () -> IntMath.binomial(n, 0));
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
    assertWithMessage("The expectedMean should be the same as computeMeanSafely")
        .that(computeMeanSafely(x, y))
        .isEqualTo(expectedMean);
    assertMean(x, y);
  }

  /**
   * Helper method that asserts the arithmetic mean of x and y is equal to the result of
   * computeMeanSafely.
   */
  private static void assertMean(int x, int y) {
    int expectedMean = computeMeanSafely(x, y);
    assertThat(IntMath.mean(x, y)).isEqualTo(expectedMean);
    assertWithMessage("The mean of x and y should equal the mean of y and x")
        .that(IntMath.mean(y, x))
        .isEqualTo(expectedMean);
  }

  /**
   * Computes the mean in a way that is obvious and resilient to overflow by using BigInteger
   * arithmetic.
   */
  private static int computeMeanSafely(int x, int y) {
    BigInteger bigX = bigInt(x);
    BigInteger bigY = bigInt(y);
    @SuppressWarnings("ConstantTwo") // Android doesn't have BigDecimal.TWO yet
    BigDecimal two = BigDecimal.valueOf(2);
    BigDecimal bigMean = new BigDecimal(bigX.add(bigY)).divide(two, RoundingMode.FLOOR);
    return bigMean.intValueExact();
  }

  private static boolean fitsInInt(BigInteger big) {
    return big.bitLength() <= 31;
  }

  @J2ktIncompatible
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
      assertThat(IntMath.isPrime(i)).isEqualTo(LongMath.isPrime(i));
    }

    // Then check 1000 deterministic pseudo-random int values.
    Random rand = new Random(1);
    for (int i = 0; i < 1000; i++) {
      int n = rand.nextInt(Integer.MAX_VALUE);
      assertThat(IntMath.isPrime(n)).isEqualTo(LongMath.isPrime(n));
    }
  }

  public void testSaturatedAbs() {
    assertThat(IntMath.saturatedAbs(Integer.MIN_VALUE)).isEqualTo(Integer.MAX_VALUE);
    assertThat(IntMath.saturatedAbs(Integer.MAX_VALUE)).isEqualTo(Integer.MAX_VALUE);
    assertThat(IntMath.saturatedAbs(-Integer.MAX_VALUE)).isEqualTo(Integer.MAX_VALUE);
    assertThat(IntMath.saturatedAbs(0)).isEqualTo(0);
    assertThat(IntMath.saturatedAbs(1)).isEqualTo(1);
    assertThat(IntMath.saturatedAbs(-1)).isEqualTo(1);
    assertThat(IntMath.saturatedAbs(10)).isEqualTo(10);
    assertThat(IntMath.saturatedAbs(-10)).isEqualTo(10);
  }

  private static int force32(int value) {
    // GWT doesn't consistently overflow values to make them 32-bit, so we need to force it.
    // TODO(b/404577035): Remove this unless it's needed for J2CL.
    // One of its users, testDivNonZero, is currently @GwtIncompatible, but maybe it WOULD need it?
    // And if it's needed, maybe use our usual trick of ~~ instead?
    return value & 0xffffffff;
  }

  private static BigInteger bigInt(long value) {
    return BigInteger.valueOf(value);
  }
}
