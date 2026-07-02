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
import static com.google.common.math.LongMath.checkedAdd;
import static com.google.common.math.LongMath.checkedMultiply;
import static com.google.common.math.LongMath.checkedSubtract;
import static com.google.common.math.LongMath.sqrt;
import static com.google.common.math.MathTesting.ALL_LONG_CANDIDATES;
import static com.google.common.math.MathTesting.ALL_ROUNDING_MODES;
import static com.google.common.math.MathTesting.ALL_SAFE_ROUNDING_MODES;
import static com.google.common.math.MathTesting.EXPONENTS;
import static com.google.common.math.MathTesting.NEGATIVE_INTEGER_CANDIDATES;
import static com.google.common.math.MathTesting.NEGATIVE_LONG_CANDIDATES;
import static com.google.common.math.MathTesting.NONZERO_LONG_CANDIDATES;
import static com.google.common.math.MathTesting.POSITIVE_INTEGER_CANDIDATES;
import static com.google.common.math.MathTesting.POSITIVE_LONG_CANDIDATES;
import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;
import static java.lang.Math.multiplyExact;
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
import java.util.EnumSet;
import java.util.Random;
import junit.framework.TestCase;
import org.jspecify.annotations.NullUnmarked;

/**
 * Tests for LongMath.
 *
 * @author Louis Wasserman
 */
@GwtCompatible
@NullUnmarked
@SuppressWarnings("LongMathMod") // We are testing LongMathMod against alternatives.
public class LongMathTest extends TestCase {
  @SuppressWarnings("ConstantOverflow")
  public void testMaxSignedPowerOfTwo() {
    assertThat(LongMath.isPowerOfTwo(LongMath.MAX_SIGNED_POWER_OF_TWO)).isTrue();
    assertThat(LongMath.isPowerOfTwo(LongMath.MAX_SIGNED_POWER_OF_TWO * 2)).isFalse();
  }

  public void testCeilingPowerOfTwo() {
    for (long x : POSITIVE_LONG_CANDIDATES) {
      BigInteger expectedResult = BigIntegerMath.ceilingPowerOfTwo(bigInt(x));
      if (fitsInLong(expectedResult)) {
        assertThat(LongMath.ceilingPowerOfTwo(x))
            .isEqualTo(BigIntegerMath.ceilingPowerOfTwo(bigInt(x)).longValue());
      }
    }
  }

  public void testCeilingPowerOfTwo_overflows() {
    for (long x : POSITIVE_LONG_CANDIDATES) {
      BigInteger expectedResult = BigIntegerMath.ceilingPowerOfTwo(bigInt(x));
      if (!fitsInLong(expectedResult)) {
        assertThrows(ArithmeticException.class, () -> LongMath.ceilingPowerOfTwo(x));
      }
    }
  }

  public void testFloorPowerOfTwo() {
    for (long x : POSITIVE_LONG_CANDIDATES) {
      BigInteger expectedResult = BigIntegerMath.floorPowerOfTwo(bigInt(x));
      assertThat(LongMath.floorPowerOfTwo(x)).isEqualTo(expectedResult.longValue());
    }
  }

  public void testCeilingPowerOfTwoNegative() {
    for (long x : NEGATIVE_LONG_CANDIDATES) {
      assertThrows(IllegalArgumentException.class, () -> LongMath.ceilingPowerOfTwo(x));
    }
  }

  public void testFloorPowerOfTwoNegative() {
    for (long x : NEGATIVE_LONG_CANDIDATES) {
      assertThrows(IllegalArgumentException.class, () -> LongMath.floorPowerOfTwo(x));
    }
  }

  public void testCeilingPowerOfTwoZero() {
    assertThrows(IllegalArgumentException.class, () -> LongMath.ceilingPowerOfTwo(0L));
  }

  public void testFloorPowerOfTwoZero() {
    assertThrows(IllegalArgumentException.class, () -> LongMath.floorPowerOfTwo(0L));
  }

  // We want to test that we've defined the constant with the correct value.
  @SuppressWarnings("TruthConstantAsserts")
  @GwtIncompatible // TODO
  public void testConstantMaxPowerOfSqrt2Unsigned() {
    assertThat(LongMath.MAX_POWER_OF_SQRT2_UNSIGNED)
        .isEqualTo(sqrt(BigInteger.ZERO.setBit(2 * Long.SIZE - 1), FLOOR).longValue());
  }

  @GwtIncompatible // BigIntegerMath // TODO(cpovirk): GWT-enable BigIntegerMath
  public void testMaxLog10ForLeadingZeros() {
    for (int i = 0; i < Long.SIZE; i++) {
      assertThat(LongMath.maxLog10ForLeadingZeros[i])
          .isEqualTo(BigIntegerMath.log10(BigInteger.ONE.shiftLeft(Long.SIZE - i), FLOOR));
    }
  }

  @GwtIncompatible // TODO
  public void testConstantsPowersOf10() {
    for (int i = 0; i < LongMath.powersOf10.length; i++) {
      assertThat(LongMath.powersOf10[i]).isEqualTo(LongMath.checkedPow(10, i));
    }
    assertThrows(
        ArithmeticException.class, () -> LongMath.checkedPow(10, LongMath.powersOf10.length));
  }

  @GwtIncompatible // TODO
  public void testConstantsHalfPowersOf10() {
    for (int i = 0; i < LongMath.halfPowersOf10.length; i++) {
      assertThat(bigInt(LongMath.halfPowersOf10[i]))
          .isEqualTo(sqrt(BigInteger.TEN.pow(2 * i + 1), FLOOR));
    }
    BigInteger nextBigger = sqrt(BigInteger.TEN.pow(2 * LongMath.halfPowersOf10.length + 1), FLOOR);
    assertThat(nextBigger).isGreaterThan(bigInt(Long.MAX_VALUE));
  }

  // We want to test that we've defined the constant with the correct value.
  @SuppressWarnings("TruthConstantAsserts")
  @GwtIncompatible // TODO
  public void testConstantsSqrtMaxLong() {
    assertThat(LongMath.FLOOR_SQRT_MAX_LONG).isEqualTo(sqrt(Long.MAX_VALUE, FLOOR));
  }

  @GwtIncompatible // TODO
  public void testConstantsFactorials() {
    long expected = 1;
    for (int i = 0; i < LongMath.factorials.length; i++, expected *= i) {
      assertThat(LongMath.factorials[i]).isEqualTo(expected);
    }
    assertThrows(
        ArithmeticException.class,
        () ->
            multiplyExact(
                LongMath.factorials[LongMath.factorials.length - 1],
                (long) LongMath.factorials.length));
  }

  @GwtIncompatible // TODO
  public void testConstantsBiggestBinomials() {
    for (int k = 0; k < LongMath.biggestBinomials.length; k++) {
      assertThat(fitsInLong(BigIntegerMath.binomial(LongMath.biggestBinomials[k], k))).isTrue();
      assertThat(
              LongMath.biggestBinomials[k] == Integer.MAX_VALUE
                  || !fitsInLong(BigIntegerMath.binomial(LongMath.biggestBinomials[k] + 1, k)))
          .isTrue();
      // In the first case, any long is valid; in the second, we want to test that the next-bigger
      // long overflows.
    }
    int k = LongMath.biggestBinomials.length;
    assertThat(fitsInLong(BigIntegerMath.binomial(2 * k, k))).isFalse();
    // 2 * k is the smallest value for which we don't replace k with (n-k).
  }

  @GwtIncompatible // TODO
  public void testConstantsBiggestSimpleBinomials() {
    for (int i = 0; i < LongMath.biggestSimpleBinomials.length; i++) {
      int k = i;
      assertThat(LongMath.biggestSimpleBinomials[k]).isAtMost(LongMath.biggestBinomials[k]);
      long unused = simpleBinomial(LongMath.biggestSimpleBinomials[k], k); // mustn't throw
      if (LongMath.biggestSimpleBinomials[k] < Integer.MAX_VALUE) {
        // unless all n are fair game with this k
        assertThrows(
            ArithmeticException.class,
            () -> simpleBinomial(LongMath.biggestSimpleBinomials[k] + 1, k));
      }
    }
    int k = LongMath.biggestSimpleBinomials.length;
    assertThrows(ArithmeticException.class, () -> simpleBinomial(2 * k, k));
  }

  @AndroidIncompatible // slow
  public void testLessThanBranchFree() {
    for (long x : ALL_LONG_CANDIDATES) {
      for (long y : ALL_LONG_CANDIDATES) {
        if (fitsInLong(bigInt(x).subtract(bigInt(y)))) {
          assertThat(LongMath.lessThanBranchFree(x, y)).isEqualTo(x < y ? 1 : 0);
        }
      }
    }
  }

  // Throws an ArithmeticException if "the simple implementation" of binomial coefficients overflows
  @GwtIncompatible // TODO
  private long simpleBinomial(int n, int k) {
    long accum = 1;
    for (long i = 0; i < k; i++) {
      accum = multiplyExact(accum, n - i);
      accum /= i + 1;
    }
    return accum;
  }

  @GwtIncompatible // java.math.BigInteger
  public void testIsPowerOfTwo() {
    for (long x : ALL_LONG_CANDIDATES) {
      // Checks for a single bit set.
      assertThat(LongMath.isPowerOfTwo(x)).isEqualTo(x > 0 && bigInt(x).bitCount() == 1);
    }
  }

  public void testLog2ZeroAlwaysThrows() {
    for (RoundingMode mode : ALL_ROUNDING_MODES) {
      assertThrows(IllegalArgumentException.class, () -> LongMath.log2(0L, mode));
    }
  }

  public void testLog2NegativeAlwaysThrows() {
    for (long x : NEGATIVE_LONG_CANDIDATES) {
      for (RoundingMode mode : ALL_ROUNDING_MODES) {
        assertThrows(IllegalArgumentException.class, () -> LongMath.log2(x, mode));
      }
    }
  }

  /* Relies on the correctness of BigIntegerMath.log2 for all modes except UNNECESSARY. */
  public void testLog2MatchesBigInteger() {
    for (long x : POSITIVE_LONG_CANDIDATES) {
      for (RoundingMode mode : ALL_SAFE_ROUNDING_MODES) {
        // The BigInteger implementation is tested separately, use it as the reference.
        assertThat(LongMath.log2(x, mode)).isEqualTo(BigIntegerMath.log2(bigInt(x), mode));
      }
    }
  }

  // Relies on the correctness of isPowerOfTwo(long).
  public void testLog2Exact() {
    for (long x : POSITIVE_LONG_CANDIDATES) {
      if (LongMath.isPowerOfTwo(x)) {
        assertThat(1L << LongMath.log2(x, UNNECESSARY)).isEqualTo(x);
      }
    }
  }

  // Relies on the correctness of isPowerOfTwo(long).
  public void testLog2Exact_notPowerOfTwo() {
    for (long x : POSITIVE_LONG_CANDIDATES) {
      if (!LongMath.isPowerOfTwo(x)) {
        assertThrows(ArithmeticException.class, () -> LongMath.log2(x, UNNECESSARY));
      }
    }
  }

  @GwtIncompatible // TODO
  public void testLog10ZeroAlwaysThrows() {
    for (RoundingMode mode : ALL_ROUNDING_MODES) {
      assertThrows(IllegalArgumentException.class, () -> LongMath.log10(0L, mode));
    }
  }

  @GwtIncompatible // TODO
  public void testLog10NegativeAlwaysThrows() {
    for (long x : NEGATIVE_LONG_CANDIDATES) {
      for (RoundingMode mode : ALL_ROUNDING_MODES) {
        assertThrows(IllegalArgumentException.class, () -> LongMath.log10(x, mode));
      }
    }
  }

  // Relies on the correctness of BigIntegerMath.log10 for all modes except UNNECESSARY.
  @GwtIncompatible // TODO
  public void testLog10MatchesBigInteger() {
    for (long x : POSITIVE_LONG_CANDIDATES) {
      for (RoundingMode mode : ALL_SAFE_ROUNDING_MODES) {
        assertThat(LongMath.log10(x, mode)).isEqualTo(BigIntegerMath.log10(bigInt(x), mode));
      }
    }
  }

  // Relies on the correctness of log10(long, FLOOR) and of pow(long, int).
  @GwtIncompatible // TODO
  public void testLog10Exact() {
    for (long x : POSITIVE_LONG_CANDIDATES) {
      int floor = LongMath.log10(x, FLOOR);
      boolean expectedSuccess = LongMath.pow(10, floor) == x;
      if (expectedSuccess) {
        assertThat(LongMath.log10(x, UNNECESSARY)).isEqualTo(floor);
      } else {
        assertThrows(ArithmeticException.class, () -> LongMath.log10(x, UNNECESSARY));
      }
    }
  }

  @GwtIncompatible // TODO
  public void testLog10TrivialOnPowerOf10() {
    long x = 1000000000000L;
    for (RoundingMode mode : ALL_ROUNDING_MODES) {
      assertThat(LongMath.log10(x, mode)).isEqualTo(12);
    }
  }

  @GwtIncompatible // TODO
  public void testSqrtNegativeAlwaysThrows() {
    for (long x : NEGATIVE_LONG_CANDIDATES) {
      for (RoundingMode mode : ALL_ROUNDING_MODES) {
        assertThrows(IllegalArgumentException.class, () -> sqrt(x, mode));
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
        assertThat(bigInt(sqrt(x, mode))).isEqualTo(sqrt(bigInt(x), mode));
      }
    }
  }

  /* Relies on the correctness of sqrt(long, FLOOR). */
  @GwtIncompatible // TODO
  public void testSqrtExactMatchesFloorOrThrows() {
    for (long x : POSITIVE_LONG_CANDIDATES) {
      long sqrtFloor = sqrt(x, FLOOR);
      // We only expect an exception if x was not a perfect square.
      boolean isPerfectSquare = sqrtFloor * sqrtFloor == x;
      if (isPerfectSquare) {
        assertThat(sqrt(x, UNNECESSARY)).isEqualTo(sqrtFloor);
      } else {
        assertThrows(ArithmeticException.class, () -> sqrt(x, UNNECESSARY));
      }
    }
  }

  @GwtIncompatible // TODO
  public void testPow() {
    for (long i : ALL_LONG_CANDIDATES) {
      for (int exp : EXPONENTS) {
        assertThat(LongMath.pow(i, exp)).isEqualTo(bigInt(i).pow(exp).longValue());
      }
    }
  }

  @J2ktIncompatible // J2kt BigDecimal.divide also has the rounding bug
  @GwtIncompatible // TODO
  @AndroidIncompatible // TODO(cpovirk): File BigDecimal.divide() rounding bug.
  public void testDivNonZero() {
    for (long p : NONZERO_LONG_CANDIDATES) {
      for (long q : NONZERO_LONG_CANDIDATES) {
        for (RoundingMode mode : ALL_SAFE_ROUNDING_MODES) {
          long expected =
              new BigDecimal(bigInt(p)).divide(new BigDecimal(bigInt(q)), 0, mode).longValue();
          long actual = LongMath.divide(p, q, mode);
          if (expected != actual) {
            failFormat("expected divide(%s, %s, %s) = %s; got %s", p, q, mode, expected, actual);
          }
          // Check the assertions we make in the javadoc.
          if (mode == DOWN) {
            assertThat(LongMath.divide(p, q, mode)).isEqualTo(p / q);
          } else if (mode == FLOOR) {
            assertThat(LongMath.divide(p, q, mode)).isEqualTo(Math.floorDiv(p, q));
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
          assertThat(LongMath.divide(p, q, UNNECESSARY) * q).isEqualTo(p);
          assertThat(expectedSuccess).isTrue();
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
        assertThat(LongMath.divide(0L, q, mode)).isEqualTo(0L);
      }
    }
  }

  @GwtIncompatible // TODO
  public void testDivByZeroAlwaysFails() {
    for (long p : ALL_LONG_CANDIDATES) {
      for (RoundingMode mode : ALL_ROUNDING_MODES) {
        assertThrows(ArithmeticException.class, () -> LongMath.divide(p, 0L, mode));
      }
    }
  }

  @GwtIncompatible // TODO
  public void testIntMod() {
    for (long x : ALL_LONG_CANDIDATES) {
      for (int m : POSITIVE_INTEGER_CANDIDATES) {
        assertThat(LongMath.mod(x, m)).isEqualTo(bigInt(x).mod(bigInt(m)).intValue());
      }
    }
  }

  @GwtIncompatible // TODO
  public void testIntModNegativeModulusFails() {
    for (long x : ALL_LONG_CANDIDATES) {
      for (int m : NEGATIVE_INTEGER_CANDIDATES) {
        assertThrows(ArithmeticException.class, () -> LongMath.mod(x, m));
      }
    }
  }

  @GwtIncompatible // TODO
  public void testIntModZeroModulusFails() {
    for (long x : ALL_LONG_CANDIDATES) {
      assertThrows(ArithmeticException.class, () -> LongMath.mod(x, 0));
    }
  }

  @AndroidIncompatible // slow
  @GwtIncompatible // TODO
  public void testMod() {
    for (long x : ALL_LONG_CANDIDATES) {
      for (long m : POSITIVE_LONG_CANDIDATES) {
        assertThat(LongMath.mod(x, m)).isEqualTo(bigInt(x).mod(bigInt(m)).longValue());
      }
    }
  }

  @GwtIncompatible // TODO
  public void testModNegativeModulusFails() {
    for (long x : ALL_LONG_CANDIDATES) {
      for (long m : NEGATIVE_LONG_CANDIDATES) {
        assertThrows(ArithmeticException.class, () -> LongMath.mod(x, m));
      }
    }
  }

  public void testGCDExhaustive() {
    for (long a : POSITIVE_LONG_CANDIDATES) {
      for (long b : POSITIVE_LONG_CANDIDATES) {
        assertThat(bigInt(LongMath.gcd(a, b))).isEqualTo(bigInt(a).gcd(bigInt(b)));
      }
    }
  }

  @GwtIncompatible // TODO
  public void testGCDZero() {
    for (long a : POSITIVE_LONG_CANDIDATES) {
      assertThat(LongMath.gcd(a, 0)).isEqualTo(a);
      assertThat(LongMath.gcd(0, a)).isEqualTo(a);
    }
    assertThat(LongMath.gcd(0, 0)).isEqualTo(0);
  }

  @GwtIncompatible // TODO
  public void testGCDNegativePositiveThrows() {
    for (long a : NEGATIVE_LONG_CANDIDATES) {
      assertThrows(IllegalArgumentException.class, () -> LongMath.gcd(a, 3));
      assertThrows(IllegalArgumentException.class, () -> LongMath.gcd(3, a));
    }
  }

  @GwtIncompatible // TODO
  public void testGCDNegativeZeroThrows() {
    for (long a : NEGATIVE_LONG_CANDIDATES) {
      assertThrows(IllegalArgumentException.class, () -> LongMath.gcd(a, 0));
      assertThrows(IllegalArgumentException.class, () -> LongMath.gcd(0, a));
    }
  }

  @SuppressWarnings("InlineMeInliner") // We need to test checkedAdd
  @AndroidIncompatible // slow
  public void testCheckedAdd() {
    for (long a : ALL_LONG_CANDIDATES) {
      for (long b : ALL_LONG_CANDIDATES) {
        // TODO: cpovirk - Test against Math.addExact instead?
        BigInteger expectedResult = bigInt(a).add(bigInt(b));
        boolean expectedSuccess = fitsInLong(expectedResult);
        try {
          assertThat(checkedAdd(a, b)).isEqualTo(a + b);
          assertThat(expectedSuccess).isTrue();
        } catch (ArithmeticException e) {
          if (expectedSuccess) {
            failFormat(
                "expected checkedAdd(%s, %s) = %s; got ArithmeticException", a, b, expectedResult);
          }
        }
      }
    }
  }

  @SuppressWarnings("InlineMeInliner") // We need to test checkedSubtract
  @AndroidIncompatible // slow
  public void testCheckedSubtract() {
    for (long a : ALL_LONG_CANDIDATES) {
      for (long b : ALL_LONG_CANDIDATES) {
        // TODO: cpovirk - Test against Math.subtractExact instead?
        BigInteger expectedResult = bigInt(a).subtract(bigInt(b));
        boolean expectedSuccess = fitsInLong(expectedResult);
        try {
          assertThat(checkedSubtract(a, b)).isEqualTo(a - b);
          assertThat(expectedSuccess).isTrue();
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

  @SuppressWarnings("InlineMeInliner") // We need to test checkedMultiply
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
        // TODO: cpovirk - Test against Math.multiplyExact instead?
        BigInteger expectedResult = bigInt(a).multiply(bigInt(b));
        boolean expectedSuccess = fitsInLong(expectedResult);
        try {
          assertThat(checkedMultiply(a, b)).isEqualTo(a * b);
          assertThat(expectedSuccess).isTrue();
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
        BigInteger expectedResult = bigInt(b).pow(exp);
        boolean expectedSuccess = fitsInLong(expectedResult);
        try {
          assertThat(LongMath.checkedPow(b, exp)).isEqualTo(expectedResult.longValue());
          assertThat(expectedSuccess).isTrue();
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
            a, b, "s+", saturatedCast(bigInt(a).add(bigInt(b))), LongMath.saturatedAdd(a, b));
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
            saturatedCast(bigInt(a).subtract(bigInt(b))),
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
            saturatedCast(bigInt(a).multiply(bigInt(b))),
            LongMath.saturatedMultiply(a, b));
      }
    }
  }

  @GwtIncompatible // TODO
  public void testSaturatedPow() {
    for (long a : ALL_LONG_CANDIDATES) {
      for (int b : EXPONENTS) {
        assertOperationEquals(
            a, b, "s^", saturatedCast(bigInt(a).pow(b)), LongMath.saturatedPow(a, b));
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
      assertThat(LongMath.factorial(n)).isEqualTo(expectedLong);
    }
  }

  @GwtIncompatible // TODO
  public void testFactorialNegative() {
    for (int n : NEGATIVE_INTEGER_CANDIDATES) {
      assertThrows(IllegalArgumentException.class, () -> LongMath.factorial(n));
    }
  }

  // Depends on the correctness of BigIntegerMath.binomial.
  public void testBinomial() {
    for (int n = 0; n <= 70; n++) {
      for (int k = 0; k <= n; k++) {
        BigInteger expectedBig = BigIntegerMath.binomial(n, k);
        long expectedLong = fitsInLong(expectedBig) ? expectedBig.longValue() : Long.MAX_VALUE;
        assertThat(LongMath.binomial(n, k)).isEqualTo(expectedLong);
      }
    }
  }


  @GwtIncompatible // Slow
  public void testBinomial_exhaustiveNotOverflowing() {
    // Tests all of the inputs to LongMath.binomial that won't cause it to overflow, that weren't
    // tested in the previous method, for k >= 3.
    for (int k = 3; k < LongMath.biggestBinomials.length; k++) {
      for (int n = 70; n <= LongMath.biggestBinomials[k]; n++) {
        assertThat(LongMath.binomial(n, k)).isEqualTo(BigIntegerMath.binomial(n, k).longValue());
      }
    }
  }

  public void testBinomialOutside() {
    for (int i = 0; i <= 50; i++) {
      int n = i;
      assertThrows(IllegalArgumentException.class, () -> LongMath.binomial(n, -1));
      assertThrows(IllegalArgumentException.class, () -> LongMath.binomial(n, n + 1));
    }
  }

  public void testBinomialNegative() {
    for (int n : NEGATIVE_INTEGER_CANDIDATES) {
      assertThrows(IllegalArgumentException.class, () -> LongMath.binomial(n, 0));
    }
  }


  @AndroidIncompatible // slow enough to cause a timeout
  @J2ktIncompatible // slow enough to cause flakiness
  @GwtIncompatible // far too slow
  public void testSqrtOfPerfectSquareAsDoubleIsPerfect() {
    // This takes just over a minute on my machine.
    for (long n = 0; n <= LongMath.FLOOR_SQRT_MAX_LONG; n++) {
      long actual = (long) Math.sqrt((double) (n * n));
      assertThat(actual).isEqualTo(n);
    }
  }

  public void testSqrtOfLongIsAtMostFloorSqrtMaxLong() {
    long sqrtMaxLong = (long) Math.sqrt(Long.MAX_VALUE);
    assertThat(sqrtMaxLong).isAtMost(LongMath.FLOOR_SQRT_MAX_LONG);
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
    assertWithMessage("The expectedMean should be the same as computeMeanSafely")
        .that(computeMeanSafely(x, y))
        .isEqualTo(expectedMean);
    assertMean(x, y);
  }

  /**
   * Helper method that asserts the arithmetic mean of x and y is equal to the result of
   * computeMeanSafely.
   */
  private static void assertMean(long x, long y) {
    long expectedMean = computeMeanSafely(x, y);
    assertThat(LongMath.mean(x, y)).isEqualTo(expectedMean);
    assertWithMessage("The mean of x and y should equal the mean of y and x")
        .that(LongMath.mean(y, x))
        .isEqualTo(expectedMean);
  }

  /**
   * Computes the mean in a way that is obvious and resilient to overflow by using BigInteger
   * arithmetic.
   */
  private static long computeMeanSafely(long x, long y) {
    BigInteger bigX = bigInt(x);
    BigInteger bigY = bigInt(y);
    @SuppressWarnings("ConstantTwo") // Android doesn't have BigDecimal.TWO yet
    BigDecimal two = BigDecimal.valueOf(2);
    BigDecimal bigMean = new BigDecimal(bigX.add(bigY)).divide(two, RoundingMode.FLOOR);
    return bigMean.longValueExact();
  }

  private static boolean fitsInLong(BigInteger big) {
    return big.bitLength() <= 63;
  }

  private static final BigInteger MAX_LONG = bigInt(Long.MAX_VALUE);
  private static final BigInteger MIN_LONG = bigInt(Long.MIN_VALUE);

  private static long saturatedCast(BigInteger big) {
    if (big.compareTo(MAX_LONG) > 0) {
      return Long.MAX_VALUE;
    }
    if (big.compareTo(MIN_LONG) < 0) {
      return Long.MIN_VALUE;
    }
    return big.longValue();
  }

  @J2ktIncompatible
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
      assertThat(LongMath.isPrime(i)).isEqualTo(bigInt(i).isProbablePrime(100));
    }
  }

  @GwtIncompatible // isPrime is GWT-incompatible
  public void testIsPrimeManyConstants() {
    // Test the thorough test inputs, which also includes special constants in the Miller-Rabin
    // tests.
    for (long l : POSITIVE_LONG_CANDIDATES) {
      assertThat(LongMath.isPrime(l)).isEqualTo(bigInt(l).isProbablePrime(100));
    }
  }

  @GwtIncompatible // isPrime is GWT-incompatible
  public void testIsPrimeOnUniformRandom() {
    Random rand = new Random(1);
    for (int bits = 10; bits < 63; bits++) {
      for (int i = 0; i < 2000; i++) {
        // A random long between 0 and Long.MAX_VALUE, inclusive.
        long l = rand.nextLong() & ((1L << bits) - 1);
        assertThat(LongMath.isPrime(l)).isEqualTo(bigInt(l).isProbablePrime(100));
      }
    }
  }

  @GwtIncompatible // isPrime is GWT-incompatible
  public void testIsPrimeOnRandomPrimes() {
    Random rand = new Random(1);
    for (int bits = 10; bits < 63; bits++) {
      for (int i = 0; i < 100; i++) {
        long p = BigInteger.probablePrime(bits, rand).longValue();
        assertThat(LongMath.isPrime(p)).isTrue();
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
        assertThat(LongMath.isPrime(p * q)).isFalse();
      }
    }
  }

  @GwtIncompatible // isPrime is GWT-incompatible
  public void testIsPrimeThrowsOnNegative() {
    assertThrows(IllegalArgumentException.class, () -> LongMath.isPrime(-1));
  }

  private static final long[] roundToDoubleTestCandidates = {
    0,
    16,
    1L << 53,
    (1L << 53) + 1,
    (1L << 53) + 2,
    (1L << 53) + 3,
    (1L << 53) + 4,
    1L << 54,
    (1L << 54) + 1,
    (1L << 54) + 2,
    (1L << 54) + 3,
    (1L << 54) + 4,
    0x7ffffffffffffe00L, // halfway between 2^63 and next-lower double
    0x7ffffffffffffe01L, // above + 1
    0x7ffffffffffffdffL, // above - 1
    Long.MAX_VALUE - (1L << 11) + 1,
    Long.MAX_VALUE - 2,
    Long.MAX_VALUE - 1,
    Long.MAX_VALUE,
    -16,
    -1L << 53,
    -(1L << 53) - 1,
    -(1L << 53) - 2,
    -(1L << 53) - 3,
    -(1L << 53) - 4,
    -1L << 54,
    -(1L << 54) - 1,
    -(1L << 54) - 2,
    -(1L << 54) - 3,
    -(1L << 54) - 4,
    Long.MIN_VALUE + 2,
    Long.MIN_VALUE + 1,
    Long.MIN_VALUE
  };

  @J2ktIncompatible // EnumSet.complementOf
  @GwtIncompatible
  public void testRoundToDoubleAgainstBigInteger() {
    for (RoundingMode roundingMode : EnumSet.complementOf(EnumSet.of(UNNECESSARY))) {
      for (long candidate : roundToDoubleTestCandidates) {
        assertThat(LongMath.roundToDouble(candidate, roundingMode))
            .isEqualTo(BigIntegerMath.roundToDouble(bigInt(candidate), roundingMode));
      }
    }
  }

  @GwtIncompatible
  public void testRoundToDoubleAgainstBigIntegerUnnecessary() {
    for (long candidate : roundToDoubleTestCandidates) {
      Double expectedDouble = null;
      try {
        expectedDouble = BigIntegerMath.roundToDouble(bigInt(candidate), UNNECESSARY);
      } catch (ArithmeticException expected) {
        // do nothing
      }

      if (expectedDouble != null) {
        assertThat(LongMath.roundToDouble(candidate, UNNECESSARY)).isEqualTo(expectedDouble);
      } else {
        assertThrows(
            ArithmeticException.class, () -> LongMath.roundToDouble(candidate, UNNECESSARY));
      }
    }
  }

  public void testSaturatedAbs() {
    assertThat(LongMath.saturatedAbs(Long.MIN_VALUE)).isEqualTo(Long.MAX_VALUE);
    assertThat(LongMath.saturatedAbs(Long.MAX_VALUE)).isEqualTo(Long.MAX_VALUE);
    assertThat(LongMath.saturatedAbs(-Long.MAX_VALUE)).isEqualTo(Long.MAX_VALUE);
    assertThat(LongMath.saturatedAbs(0)).isEqualTo(0);
    assertThat(LongMath.saturatedAbs(1)).isEqualTo(1);
    assertThat(LongMath.saturatedAbs(-1)).isEqualTo(1);
    assertThat(LongMath.saturatedAbs(10)).isEqualTo(10);
    assertThat(LongMath.saturatedAbs(-10)).isEqualTo(10);
  }

  private static void failFormat(String template, Object... args) {
    assertWithMessage(template, args).fail();
  }

  private static BigInteger bigInt(long value) {
    return BigInteger.valueOf(value);
  }
}
