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

import static com.google.common.collect.Iterables.get;
import static com.google.common.collect.Iterables.size;
import static com.google.common.math.MathTesting.ALL_DOUBLE_CANDIDATES;
import static com.google.common.math.MathTesting.ALL_ROUNDING_MODES;
import static com.google.common.math.MathTesting.ALL_SAFE_ROUNDING_MODES;
import static com.google.common.math.MathTesting.DOUBLE_CANDIDATES_EXCEPT_NAN;
import static com.google.common.math.MathTesting.FINITE_DOUBLE_CANDIDATES;
import static com.google.common.math.MathTesting.FRACTIONAL_DOUBLE_CANDIDATES;
import static com.google.common.math.MathTesting.INFINITIES;
import static com.google.common.math.MathTesting.INTEGRAL_DOUBLE_CANDIDATES;
import static com.google.common.math.MathTesting.NEGATIVE_INTEGER_CANDIDATES;
import static com.google.common.math.MathTesting.POSITIVE_FINITE_DOUBLE_CANDIDATES;
import static com.google.common.math.ReflectionFreeAssertThrows.assertThrows;
import static com.google.common.truth.Truth.assertThat;
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
import com.google.common.annotations.J2ktIncompatible;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.primitives.Doubles;
import com.google.common.testing.NullPointerTester;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.Arrays;
import junit.framework.TestCase;
import org.jspecify.annotations.NullUnmarked;

/**
 * Tests for {@code DoubleMath}.
 *
 * @author Louis Wasserman
 */
@GwtCompatible(emulated = true)
@NullUnmarked
public class DoubleMathTest extends TestCase {

  private static final BigDecimal MAX_INT_AS_BIG_DECIMAL = BigDecimal.valueOf(Integer.MAX_VALUE);
  private static final BigDecimal MIN_INT_AS_BIG_DECIMAL = BigDecimal.valueOf(Integer.MIN_VALUE);

  private static final BigDecimal MAX_LONG_AS_BIG_DECIMAL = BigDecimal.valueOf(Long.MAX_VALUE);
  private static final BigDecimal MIN_LONG_AS_BIG_DECIMAL = BigDecimal.valueOf(Long.MIN_VALUE);

  public void testConstantsMaxFactorial() {
    BigInteger maxDoubleValue = BigDecimal.valueOf(Double.MAX_VALUE).toBigInteger();
    assertTrue(BigIntegerMath.factorial(DoubleMath.MAX_FACTORIAL).compareTo(maxDoubleValue) <= 0);
    assertTrue(
        BigIntegerMath.factorial(DoubleMath.MAX_FACTORIAL + 1).compareTo(maxDoubleValue) > 0);
  }

  public void testConstantsEverySixteenthFactorial() {
    for (int i = 0, n = 0; n <= DoubleMath.MAX_FACTORIAL; i++, n += 16) {
      assertThat(DoubleMath.everySixteenthFactorial[i])
          .isEqualTo(BigIntegerMath.factorial(n).doubleValue());
    }
  }

  @GwtIncompatible // DoubleMath.roundToInt(double, RoundingMode)
  public void testRoundIntegralDoubleToInt() {
    for (double d : INTEGRAL_DOUBLE_CANDIDATES) {
      for (RoundingMode mode : ALL_SAFE_ROUNDING_MODES) {
        BigDecimal expected = new BigDecimal(d).setScale(0, mode);
        boolean isInBounds =
            expected.compareTo(MAX_INT_AS_BIG_DECIMAL) <= 0
                & expected.compareTo(MIN_INT_AS_BIG_DECIMAL) >= 0;

        try {
          assertEquals(expected.intValue(), DoubleMath.roundToInt(d, mode));
          assertTrue(isInBounds);
        } catch (ArithmeticException e) {
          assertFalse(isInBounds);
        }
      }
    }
  }

  @GwtIncompatible // DoubleMath.roundToInt(double, RoundingMode)
  public void testRoundFractionalDoubleToInt() {
    for (double d : FRACTIONAL_DOUBLE_CANDIDATES) {
      for (RoundingMode mode : ALL_SAFE_ROUNDING_MODES) {
        BigDecimal expected = new BigDecimal(d).setScale(0, mode);
        boolean isInBounds =
            expected.compareTo(MAX_INT_AS_BIG_DECIMAL) <= 0
                & expected.compareTo(MIN_INT_AS_BIG_DECIMAL) >= 0;

        try {
          assertEquals(
              "Rounding " + d + " with mode " + mode,
              expected.intValue(),
              DoubleMath.roundToInt(d, mode));
          assertTrue(isInBounds);
        } catch (ArithmeticException e) {
          assertFalse(isInBounds);
        }
      }
    }
  }

  @GwtIncompatible // DoubleMath.roundToInt(double, RoundingMode)
  public void testRoundExactIntegralDoubleToInt() {
    for (double d : INTEGRAL_DOUBLE_CANDIDATES) {
      BigDecimal expected = new BigDecimal(d).setScale(0, UNNECESSARY);
      boolean isInBounds =
          expected.compareTo(MAX_INT_AS_BIG_DECIMAL) <= 0
              & expected.compareTo(MIN_INT_AS_BIG_DECIMAL) >= 0;

      try {
        assertEquals(expected.intValue(), DoubleMath.roundToInt(d, UNNECESSARY));
        assertTrue(isInBounds);
      } catch (ArithmeticException e) {
        assertFalse(isInBounds);
      }
    }
  }

  @GwtIncompatible // DoubleMath.roundToInt(double, RoundingMode)
  public void testRoundExactFractionalDoubleToIntFails() {
    for (double d : FRACTIONAL_DOUBLE_CANDIDATES) {
      assertThrows(ArithmeticException.class, () -> DoubleMath.roundToInt(d, UNNECESSARY));
    }
  }

  @GwtIncompatible // DoubleMath.roundToInt(double, RoundingMode)
  public void testRoundNaNToIntAlwaysFails() {
    for (RoundingMode mode : ALL_ROUNDING_MODES) {
      assertThrows(ArithmeticException.class, () -> DoubleMath.roundToInt(Double.NaN, mode));
    }
  }

  @GwtIncompatible // DoubleMath.roundToInt(double, RoundingMode)
  public void testRoundInfiniteToIntAlwaysFails() {
    for (RoundingMode mode : ALL_ROUNDING_MODES) {
      assertThrows(
          ArithmeticException.class, () -> DoubleMath.roundToInt(Double.POSITIVE_INFINITY, mode));
      assertThrows(
          ArithmeticException.class, () -> DoubleMath.roundToInt(Double.NEGATIVE_INFINITY, mode));
    }
  }

  @GwtIncompatible // DoubleMath.roundToLong(double, RoundingMode)
  public void testRoundIntegralDoubleToLong() {
    for (double d : INTEGRAL_DOUBLE_CANDIDATES) {
      for (RoundingMode mode : ALL_SAFE_ROUNDING_MODES) {
        BigDecimal expected = new BigDecimal(d).setScale(0, mode);
        boolean isInBounds =
            expected.compareTo(MAX_LONG_AS_BIG_DECIMAL) <= 0
                & expected.compareTo(MIN_LONG_AS_BIG_DECIMAL) >= 0;

        try {
          assertEquals(expected.longValue(), DoubleMath.roundToLong(d, mode));
          assertTrue(isInBounds);
        } catch (ArithmeticException e) {
          assertFalse(isInBounds);
        }
      }
    }
  }

  @GwtIncompatible // DoubleMath.roundToLong(double, RoundingMode)
  public void testRoundFractionalDoubleToLong() {
    for (double d : FRACTIONAL_DOUBLE_CANDIDATES) {
      for (RoundingMode mode : ALL_SAFE_ROUNDING_MODES) {
        BigDecimal expected = new BigDecimal(d).setScale(0, mode);
        boolean isInBounds =
            expected.compareTo(MAX_LONG_AS_BIG_DECIMAL) <= 0
                & expected.compareTo(MIN_LONG_AS_BIG_DECIMAL) >= 0;

        try {
          assertEquals(expected.longValue(), DoubleMath.roundToLong(d, mode));
          assertTrue(isInBounds);
        } catch (ArithmeticException e) {
          assertFalse(isInBounds);
        }
      }
    }
  }

  @GwtIncompatible // DoubleMath.roundToLong(double, RoundingMode)
  public void testRoundExactIntegralDoubleToLong() {
    for (double d : INTEGRAL_DOUBLE_CANDIDATES) {
      // every mode except UNNECESSARY
      BigDecimal expected = new BigDecimal(d).setScale(0, UNNECESSARY);
      boolean isInBounds =
          expected.compareTo(MAX_LONG_AS_BIG_DECIMAL) <= 0
              & expected.compareTo(MIN_LONG_AS_BIG_DECIMAL) >= 0;

      try {
        assertEquals(expected.longValue(), DoubleMath.roundToLong(d, UNNECESSARY));
        assertTrue(isInBounds);
      } catch (ArithmeticException e) {
        assertFalse(isInBounds);
      }
    }
  }

  @GwtIncompatible // DoubleMath.roundToLong(double, RoundingMode)
  public void testRoundExactFractionalDoubleToLongFails() {
    for (double d : FRACTIONAL_DOUBLE_CANDIDATES) {
      assertThrows(ArithmeticException.class, () -> DoubleMath.roundToLong(d, UNNECESSARY));
    }
  }

  @GwtIncompatible // DoubleMath.roundToLong(double, RoundingMode)
  public void testRoundNaNToLongAlwaysFails() {
    for (RoundingMode mode : ALL_ROUNDING_MODES) {
      assertThrows(ArithmeticException.class, () -> DoubleMath.roundToLong(Double.NaN, mode));
    }
  }

  @GwtIncompatible // DoubleMath.roundToLong(double, RoundingMode)
  public void testRoundInfiniteToLongAlwaysFails() {
    for (RoundingMode mode : ALL_ROUNDING_MODES) {
      assertThrows(
          ArithmeticException.class, () -> DoubleMath.roundToLong(Double.POSITIVE_INFINITY, mode));
      assertThrows(
          ArithmeticException.class, () -> DoubleMath.roundToLong(Double.NEGATIVE_INFINITY, mode));
    }
  }

  @GwtIncompatible // DoubleMath.roundToBigInteger(double, RoundingMode)
  public void testRoundIntegralDoubleToBigInteger() {
    for (double d : INTEGRAL_DOUBLE_CANDIDATES) {
      for (RoundingMode mode : ALL_SAFE_ROUNDING_MODES) {
        BigDecimal expected = new BigDecimal(d).setScale(0, mode);
        assertEquals(expected.toBigInteger(), DoubleMath.roundToBigInteger(d, mode));
      }
    }
  }

  @GwtIncompatible // DoubleMath.roundToBigInteger(double, RoundingMode)
  public void testRoundFractionalDoubleToBigInteger() {
    for (double d : FRACTIONAL_DOUBLE_CANDIDATES) {
      for (RoundingMode mode : ALL_SAFE_ROUNDING_MODES) {
        BigDecimal expected = new BigDecimal(d).setScale(0, mode);
        assertEquals(expected.toBigInteger(), DoubleMath.roundToBigInteger(d, mode));
      }
    }
  }

  @GwtIncompatible // DoubleMath.roundToBigInteger(double, RoundingMode)
  public void testRoundExactIntegralDoubleToBigInteger() {
    for (double d : INTEGRAL_DOUBLE_CANDIDATES) {
      BigDecimal expected = new BigDecimal(d).setScale(0, UNNECESSARY);
      assertEquals(expected.toBigInteger(), DoubleMath.roundToBigInteger(d, UNNECESSARY));
    }
  }

  @GwtIncompatible // DoubleMath.roundToBigInteger(double, RoundingMode)
  public void testRoundExactFractionalDoubleToBigIntegerFails() {
    for (double d : FRACTIONAL_DOUBLE_CANDIDATES) {
      assertThrows(ArithmeticException.class, () -> DoubleMath.roundToBigInteger(d, UNNECESSARY));
    }
  }

  @GwtIncompatible // DoubleMath.roundToBigInteger(double, RoundingMode)
  public void testRoundNaNToBigIntegerAlwaysFails() {
    for (RoundingMode mode : ALL_ROUNDING_MODES) {
      assertThrows(ArithmeticException.class, () -> DoubleMath.roundToBigInteger(Double.NaN, mode));
    }
  }

  @GwtIncompatible // DoubleMath.roundToBigInteger(double, RoundingMode)
  public void testRoundInfiniteToBigIntegerAlwaysFails() {
    for (RoundingMode mode : ALL_ROUNDING_MODES) {
      assertThrows(
          ArithmeticException.class,
          () -> DoubleMath.roundToBigInteger(Double.POSITIVE_INFINITY, mode));
      assertThrows(
          ArithmeticException.class,
          () -> DoubleMath.roundToBigInteger(Double.NEGATIVE_INFINITY, mode));
    }
  }

  @GwtIncompatible // DoubleMath.roundToBigInteger(double, RoundingMode)
  public void testRoundLog2Floor() {
    for (double d : POSITIVE_FINITE_DOUBLE_CANDIDATES) {
      int log2 = DoubleMath.log2(d, FLOOR);
      assertTrue(StrictMath.pow(2.0, log2) <= d);
      assertTrue(StrictMath.pow(2.0, log2 + 1) > d);
    }
  }

  @GwtIncompatible // DoubleMath.log2(double, RoundingMode), StrictMath
  public void testRoundLog2Ceiling() {
    for (double d : POSITIVE_FINITE_DOUBLE_CANDIDATES) {
      int log2 = DoubleMath.log2(d, CEILING);
      assertTrue(StrictMath.pow(2.0, log2) >= d);
      double z = StrictMath.pow(2.0, log2 - 1);
      assertTrue(z < d);
    }
  }

  @GwtIncompatible // DoubleMath.log2(double, RoundingMode), StrictMath
  public void testRoundLog2Down() {
    for (double d : POSITIVE_FINITE_DOUBLE_CANDIDATES) {
      int log2 = DoubleMath.log2(d, DOWN);
      if (d >= 1.0) {
        assertTrue(log2 >= 0);
        assertTrue(StrictMath.pow(2.0, log2) <= d);
        assertTrue(StrictMath.pow(2.0, log2 + 1) > d);
      } else {
        assertTrue(log2 <= 0);
        assertTrue(StrictMath.pow(2.0, log2) >= d);
        assertTrue(StrictMath.pow(2.0, log2 - 1) < d);
      }
    }
  }

  @GwtIncompatible // DoubleMath.log2(double, RoundingMode), StrictMath
  public void testRoundLog2Up() {
    for (double d : POSITIVE_FINITE_DOUBLE_CANDIDATES) {
      int log2 = DoubleMath.log2(d, UP);
      if (d >= 1.0) {
        assertTrue(log2 >= 0);
        assertTrue(StrictMath.pow(2.0, log2) >= d);
        assertTrue(StrictMath.pow(2.0, log2 - 1) < d);
      } else {
        assertTrue(log2 <= 0);
        assertTrue(StrictMath.pow(2.0, log2) <= d);
        assertTrue(StrictMath.pow(2.0, log2 + 1) > d);
      }
    }
  }

  @GwtIncompatible // DoubleMath.log2(double, RoundingMode)
  public void testRoundLog2Half() {
    // We don't expect perfect rounding accuracy.
    for (int exp : asList(-1022, -50, -1, 0, 1, 2, 3, 4, 100, 1022, 1023)) {
      for (RoundingMode mode : asList(HALF_EVEN, HALF_UP, HALF_DOWN)) {
        double x = Math.scalb(Math.sqrt(2) + 0.001, exp);
        double y = Math.scalb(Math.sqrt(2) - 0.001, exp);
        assertEquals(exp + 1, DoubleMath.log2(x, mode));
        assertEquals(exp, DoubleMath.log2(y, mode));
      }
    }
  }

  @GwtIncompatible // DoubleMath.log2(double, RoundingMode)
  public void testRoundLog2Exact() {
    for (double x : POSITIVE_FINITE_DOUBLE_CANDIDATES) {
      boolean isPowerOfTwo = StrictMath.pow(2.0, DoubleMath.log2(x, FLOOR)) == x;
      try {
        int log2 = DoubleMath.log2(x, UNNECESSARY);
        assertThat(Math.scalb(1.0, log2)).isEqualTo(x);
        assertTrue(isPowerOfTwo);
      } catch (ArithmeticException e) {
        assertFalse(isPowerOfTwo);
      }
    }
  }

  @GwtIncompatible // DoubleMath.log2(double, RoundingMode)
  public void testRoundLog2ThrowsOnZerosInfinitiesAndNaN() {
    for (RoundingMode mode : ALL_ROUNDING_MODES) {
      for (double d :
          asList(0.0, -0.0, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NaN)) {
        assertThrows(IllegalArgumentException.class, () -> DoubleMath.log2(d, mode));
      }
    }
  }

  @GwtIncompatible // DoubleMath.log2(double, RoundingMode)
  public void testRoundLog2ThrowsOnNegative() {
    for (RoundingMode mode : ALL_ROUNDING_MODES) {
      for (double d : POSITIVE_FINITE_DOUBLE_CANDIDATES) {
        assertThrows(IllegalArgumentException.class, () -> DoubleMath.log2(-d, mode));
      }
    }
  }

  @GwtIncompatible // DoubleMath.isPowerOfTwo, DoubleMath.log2(double, RoundingMode), StrictMath
  public void testIsPowerOfTwoYes() {
    for (int i = -1074; i <= 1023; i++) {
      assertTrue(DoubleMath.isPowerOfTwo(StrictMath.pow(2.0, i)));
    }
  }

  @GwtIncompatible // DoubleMath.isPowerOfTwo, DoubleMath.log2(double, RoundingMode), StrictMath
  public void testIsPowerOfTwo() {
    for (double x : ALL_DOUBLE_CANDIDATES) {
      boolean expected =
          x > 0
              && !Double.isInfinite(x)
              && !Double.isNaN(x)
              && StrictMath.pow(2.0, DoubleMath.log2(x, FLOOR)) == x;
      assertEquals(expected, DoubleMath.isPowerOfTwo(x));
    }
  }

  @GwtIncompatible // #trueLog2, Math.ulp
  public void testLog2Accuracy() {
    for (double d : POSITIVE_FINITE_DOUBLE_CANDIDATES) {
      double dmLog2 = DoubleMath.log2(d);
      double trueLog2 = trueLog2(d);
      assertTrue(Math.abs(dmLog2 - trueLog2) <= Math.ulp(trueLog2));
    }
  }

  public void testLog2SemiMonotonic() {
    for (double d : POSITIVE_FINITE_DOUBLE_CANDIDATES) {
      assertTrue(DoubleMath.log2(d + 0.01) >= DoubleMath.log2(d));
    }
  }

  public void testLog2Negative() {
    for (double d : POSITIVE_FINITE_DOUBLE_CANDIDATES) {
      assertTrue(Double.isNaN(DoubleMath.log2(-d)));
    }
  }

  public void testLog2Zero() {
    assertThat(DoubleMath.log2(0.0)).isNegativeInfinity();
    assertThat(DoubleMath.log2(-0.0)).isNegativeInfinity();
  }

  public void testLog2NaNInfinity() {
    assertThat(DoubleMath.log2(Double.POSITIVE_INFINITY)).isPositiveInfinity();
    assertTrue(Double.isNaN(DoubleMath.log2(Double.NEGATIVE_INFINITY)));
    assertTrue(Double.isNaN(DoubleMath.log2(Double.NaN)));
  }

  @GwtIncompatible // StrictMath
  @SuppressWarnings("strictfp") // Guava still supports Java 8
  private strictfp double trueLog2(double d) {
    double trueLog2 = StrictMath.log(d) / StrictMath.log(2);
    // increment until it's >= the true value
    while (StrictMath.pow(2.0, trueLog2) < d) {
      trueLog2 = StrictMath.nextUp(trueLog2);
    }
    // decrement until it's <= the true value
    while (StrictMath.pow(2.0, trueLog2) > d) {
      trueLog2 = StrictMath.nextAfter(trueLog2, Double.NEGATIVE_INFINITY);
    }
    if (StrictMath.abs(StrictMath.pow(2.0, trueLog2) - d)
        > StrictMath.abs(StrictMath.pow(2.0, StrictMath.nextUp(trueLog2)) - d)) {
      trueLog2 = StrictMath.nextUp(trueLog2);
    }
    return trueLog2;
  }

  @GwtIncompatible // DoubleMath.isMathematicalInteger
  public void testIsMathematicalIntegerIntegral() {
    for (double d : INTEGRAL_DOUBLE_CANDIDATES) {
      assertTrue(DoubleMath.isMathematicalInteger(d));
    }
  }

  @GwtIncompatible // DoubleMath.isMathematicalInteger
  public void testIsMathematicalIntegerFractional() {
    for (double d : FRACTIONAL_DOUBLE_CANDIDATES) {
      assertFalse(DoubleMath.isMathematicalInteger(d));
    }
  }

  @GwtIncompatible // DoubleMath.isMathematicalInteger
  public void testIsMathematicalIntegerNotFinite() {
    for (double d : Arrays.asList(Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NaN)) {
      assertFalse(DoubleMath.isMathematicalInteger(d));
    }
  }

  @GwtIncompatible // Math.ulp
  public void testFactorial() {
    for (int i = 0; i <= DoubleMath.MAX_FACTORIAL; i++) {
      double actual = BigIntegerMath.factorial(i).doubleValue();
      double result = DoubleMath.factorial(i);
      assertThat(result).isWithin(Math.ulp(actual)).of(actual);
    }
  }

  public void testFactorialTooHigh() {
    assertThat(DoubleMath.factorial(DoubleMath.MAX_FACTORIAL + 1)).isPositiveInfinity();
    assertThat(DoubleMath.factorial(DoubleMath.MAX_FACTORIAL + 20)).isPositiveInfinity();
  }

  public void testFactorialNegative() {
    for (int n : NEGATIVE_INTEGER_CANDIDATES) {
      assertThrows(IllegalArgumentException.class, () -> DoubleMath.factorial(n));
    }
  }

  private static final ImmutableList<Double> FINITE_TOLERANCE_CANDIDATES =
      ImmutableList.of(-0.0, 0.0, 1.0, 100.0, 10000.0, Double.MAX_VALUE);

  private static final Iterable<Double> TOLERANCE_CANDIDATES =
      ImmutableList.copyOf(
          Iterables.concat(
              FINITE_TOLERANCE_CANDIDATES, ImmutableList.of(Double.POSITIVE_INFINITY)));

  private static final ImmutableList<Double> BAD_TOLERANCE_CANDIDATES =
      ImmutableList.copyOf(
          Doubles.asList(
              -Double.MIN_VALUE,
              -Double.MIN_NORMAL,
              -1,
              -20,
              Double.NaN,
              Double.NEGATIVE_INFINITY,
              -0.001));

  public void testFuzzyEqualsFinite() {
    for (double a : FINITE_DOUBLE_CANDIDATES) {
      for (double b : FINITE_DOUBLE_CANDIDATES) {
        for (double tolerance : FINITE_TOLERANCE_CANDIDATES) {
          assertEquals(Math.abs(a - b) <= tolerance, DoubleMath.fuzzyEquals(a, b, tolerance));
        }
      }
    }
  }

  public void testFuzzyInfiniteVersusFiniteWithFiniteTolerance() {
    for (double inf : INFINITIES) {
      for (double a : FINITE_DOUBLE_CANDIDATES) {
        for (double tolerance : FINITE_TOLERANCE_CANDIDATES) {
          assertFalse(DoubleMath.fuzzyEquals(a, inf, tolerance));
          assertFalse(DoubleMath.fuzzyEquals(inf, a, tolerance));
        }
      }
    }
  }

  public void testFuzzyInfiniteVersusInfiniteWithFiniteTolerance() {
    for (double inf : INFINITIES) {
      for (double tolerance : FINITE_TOLERANCE_CANDIDATES) {
        assertTrue(DoubleMath.fuzzyEquals(inf, inf, tolerance));
        assertFalse(DoubleMath.fuzzyEquals(inf, -inf, tolerance));
      }
    }
  }

  public void testFuzzyEqualsInfiniteTolerance() {
    for (double a : DOUBLE_CANDIDATES_EXCEPT_NAN) {
      for (double b : DOUBLE_CANDIDATES_EXCEPT_NAN) {
        assertTrue(DoubleMath.fuzzyEquals(a, b, Double.POSITIVE_INFINITY));
      }
    }
  }

  public void testFuzzyEqualsOneNaN() {
    for (double a : DOUBLE_CANDIDATES_EXCEPT_NAN) {
      for (double tolerance : TOLERANCE_CANDIDATES) {
        assertFalse(DoubleMath.fuzzyEquals(a, Double.NaN, tolerance));
        assertFalse(DoubleMath.fuzzyEquals(Double.NaN, a, tolerance));
      }
    }
  }

  public void testFuzzyEqualsTwoNaNs() {
    for (double tolerance : TOLERANCE_CANDIDATES) {
      assertTrue(DoubleMath.fuzzyEquals(Double.NaN, Double.NaN, tolerance));
    }
  }

  public void testFuzzyEqualsZeroTolerance() {
    // make sure we test -0 tolerance
    for (double zero : Doubles.asList(0.0, -0.0)) {
      for (double a : ALL_DOUBLE_CANDIDATES) {
        for (double b : ALL_DOUBLE_CANDIDATES) {
          assertEquals(
              a == b || (Double.isNaN(a) && Double.isNaN(b)), DoubleMath.fuzzyEquals(a, b, zero));
        }
      }
    }
  }

  public void testFuzzyEqualsBadTolerance() {
    for (double tolerance : BAD_TOLERANCE_CANDIDATES) {
      assertThrows(IllegalArgumentException.class, () -> DoubleMath.fuzzyEquals(1, 2, tolerance));
    }
  }

  /*
   * We've split testFuzzyCompare() into multiple tests so that our internal Android test runner has
   * a better chance of completing each within its per-test-method timeout.
   */

  public void testFuzzyCompare0() {
    runTestFuzzyCompare(0);
  }

  public void testFuzzyCompare1() {
    runTestFuzzyCompare(1);
  }

  public void testFuzzyCompare2() {
    runTestFuzzyCompare(2);
  }

  public void testFuzzyCompare3() {
    runTestFuzzyCompare(3);
  }

  public void testFuzzyCompare4() {
    runTestFuzzyCompare(4);
  }

  public void testFuzzyCompare5() {
    runTestFuzzyCompare(5);
  }

  public void testFuzzyCompare6() {
    runTestFuzzyCompare(6);
  }

  public void testFuzzyCompare7() {
    assertEquals(7, size(TOLERANCE_CANDIDATES));
  }

  private static void runTestFuzzyCompare(int toleranceIndex) {
    double tolerance = get(TOLERANCE_CANDIDATES, toleranceIndex);
    for (double a : ALL_DOUBLE_CANDIDATES) {
      for (double b : ALL_DOUBLE_CANDIDATES) {
        int expected = DoubleMath.fuzzyEquals(a, b, tolerance) ? 0 : Double.compare(a, b);
        int actual = DoubleMath.fuzzyCompare(a, b, tolerance);
        assertEquals(Integer.signum(expected), Integer.signum(actual));
      }
    }
  }

  public void testFuzzyCompareBadTolerance() {
    for (double tolerance : BAD_TOLERANCE_CANDIDATES) {
      assertThrows(IllegalArgumentException.class, () -> DoubleMath.fuzzyCompare(1, 2, tolerance));
    }
  }

  @GwtIncompatible // DoubleMath.mean
  @SuppressWarnings("deprecation") // test of deprecated method
  public void testMean_doubleVarargs() {
    assertThat(DoubleMath.mean(1.1, -2.2, 4.4, -8.8)).isWithin(1.0e-10).of(-1.375);
    assertThat(DoubleMath.mean(1.1)).isWithin(1.0e-10).of(1.1);
    assertThrows(IllegalArgumentException.class, () -> DoubleMath.mean(Double.NaN));
    assertThrows(IllegalArgumentException.class, () -> DoubleMath.mean(Double.POSITIVE_INFINITY));
  }

  @GwtIncompatible // DoubleMath.mean
  @SuppressWarnings("deprecation") // test of deprecated method
  public void testMean_intVarargs() {
    assertThat(DoubleMath.mean(11, -22, 44, -88)).isWithin(1.0e-10).of(-13.75);
    assertThat(DoubleMath.mean(11)).isWithin(1.0e-10).of(11.0);
  }

  @GwtIncompatible // DoubleMath.mean
  @SuppressWarnings("deprecation") // test of deprecated method
  public void testMean_longVarargs() {
    assertThat(DoubleMath.mean(11L, -22L, 44L, -88L)).isWithin(1.0e-10).of(-13.75);
    assertThat(DoubleMath.mean(11L)).isWithin(1.0e-10).of(11.0);
  }

  @GwtIncompatible // DoubleMath.mean
  @SuppressWarnings("deprecation") // test of deprecated method
  public void testMean_emptyVarargs() {
    assertThrows(IllegalArgumentException.class, () -> DoubleMath.mean());
  }

  @GwtIncompatible // DoubleMath.mean
  @SuppressWarnings("deprecation") // test of deprecated method
  public void testMean_doubleIterable() {
    assertThat(DoubleMath.mean(ImmutableList.of(1.1, -2.2, 4.4, -8.8)))
        .isWithin(1.0e-10)
        .of(-1.375);
    assertThat(DoubleMath.mean(ImmutableList.of(1.1))).isWithin(1.0e-10).of(1.1);
    assertThrows(IllegalArgumentException.class, () -> DoubleMath.mean(ImmutableList.<Double>of()));
    assertThrows(
        IllegalArgumentException.class, () -> DoubleMath.mean(ImmutableList.of(Double.NaN)));
    assertThrows(
        IllegalArgumentException.class,
        () -> DoubleMath.mean(ImmutableList.of(Double.POSITIVE_INFINITY)));
  }

  @GwtIncompatible // DoubleMath.mean
  @SuppressWarnings("deprecation") // test of deprecated method
  public void testMean_intIterable() {
    assertThat(DoubleMath.mean(ImmutableList.of(11, -22, 44, -88))).isWithin(1.0e-10).of(-13.75);
    assertThat(DoubleMath.mean(ImmutableList.of(11))).isWithin(1.0e-10).of(11);
    assertThrows(
        IllegalArgumentException.class, () -> DoubleMath.mean(ImmutableList.<Integer>of()));
  }

  @GwtIncompatible // DoubleMath.mean
  @SuppressWarnings("deprecation") // test of deprecated method
  public void testMean_longIterable() {
    assertThat(DoubleMath.mean(ImmutableList.of(11L, -22L, 44L, -88L)))
        .isWithin(1.0e-10)
        .of(-13.75);
    assertThat(DoubleMath.mean(ImmutableList.of(11L))).isWithin(1.0e-10).of(11);
    assertThrows(IllegalArgumentException.class, () -> DoubleMath.mean(ImmutableList.<Long>of()));
  }

  @GwtIncompatible // DoubleMath.mean
  @SuppressWarnings("deprecation") // test of deprecated method
  public void testMean_intIterator() {
    assertThat(DoubleMath.mean(ImmutableList.of(11, -22, 44, -88).iterator()))
        .isWithin(1.0e-10)
        .of(-13.75);
    assertThat(DoubleMath.mean(ImmutableList.of(11).iterator())).isWithin(1.0e-10).of(11);
    assertThrows(
        IllegalArgumentException.class,
        () -> DoubleMath.mean(ImmutableList.<Integer>of().iterator()));
  }

  @GwtIncompatible // DoubleMath.mean
  @SuppressWarnings("deprecation") // test of deprecated method
  public void testMean_longIterator() {
    assertThat(DoubleMath.mean(ImmutableList.of(11L, -22L, 44L, -88L).iterator()))
        .isWithin(1.0e-10)
        .of(-13.75);
    assertThat(DoubleMath.mean(ImmutableList.of(11L).iterator())).isWithin(1.0e-10).of(11);
    assertThrows(
        IllegalArgumentException.class, () -> DoubleMath.mean(ImmutableList.<Long>of().iterator()));
  }

  @J2ktIncompatible
  @GwtIncompatible // NullPointerTester
  public void testNullPointers() {
    NullPointerTester tester = new NullPointerTester();
    tester.setDefault(double.class, 3.0);
    tester.testAllPublicStaticMethods(DoubleMath.class);
  }
}
