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

import static com.google.common.math.MathTesting.ALL_DOUBLE_CANDIDATES;
import static com.google.common.math.MathTesting.ALL_ROUNDING_MODES;
import static com.google.common.math.MathTesting.ALL_SAFE_ROUNDING_MODES;
import static com.google.common.math.MathTesting.FRACTIONAL_DOUBLE_CANDIDATES;
import static com.google.common.math.MathTesting.INTEGRAL_DOUBLE_CANDIDATES;
import static com.google.common.math.MathTesting.NEGATIVE_INTEGER_CANDIDATES;
import static com.google.common.math.MathTesting.POSITIVE_FINITE_DOUBLE_CANDIDATES;
import static java.math.RoundingMode.CEILING;
import static java.math.RoundingMode.DOWN;
import static java.math.RoundingMode.FLOOR;
import static java.math.RoundingMode.HALF_DOWN;
import static java.math.RoundingMode.HALF_EVEN;
import static java.math.RoundingMode.HALF_UP;
import static java.math.RoundingMode.UNNECESSARY;
import static java.math.RoundingMode.UP;
import static java.util.Arrays.asList;

import com.google.common.testing.NullPointerTester;

import junit.framework.TestCase;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.Arrays;

/**
 * Tests for {@code DoubleMath}.
 *
 * @author Louis Wasserman
 */
public class DoubleMathTest extends TestCase {

  private static final BigDecimal MAX_INT_AS_BIG_DECIMAL = BigDecimal.valueOf(Integer.MAX_VALUE);
  private static final BigDecimal MIN_INT_AS_BIG_DECIMAL = BigDecimal.valueOf(Integer.MIN_VALUE);

  private static final BigDecimal MAX_LONG_AS_BIG_DECIMAL = BigDecimal.valueOf(Long.MAX_VALUE);
  private static final BigDecimal MIN_LONG_AS_BIG_DECIMAL = BigDecimal.valueOf(Long.MIN_VALUE);

  public void testConstantsMaxFactorial(){
    BigInteger MAX_DOUBLE_VALUE = BigDecimal.valueOf(Double.MAX_VALUE).toBigInteger();
    assertTrue(BigIntegerMath.factorial(DoubleMath.MAX_FACTORIAL).compareTo(MAX_DOUBLE_VALUE) <= 0);
    assertTrue(
        BigIntegerMath.factorial(DoubleMath.MAX_FACTORIAL + 1).compareTo(MAX_DOUBLE_VALUE) > 0);
  }

  public void testConstantsEverySixteenthFactorial() {
    for (int i = 0, n = 0; n <= DoubleMath.MAX_FACTORIAL; i++, n += 16) {
      assertEquals(
          BigIntegerMath.factorial(n).doubleValue(), DoubleMath.EVERY_SIXTEENTH_FACTORIAL[i]);
    }
  }
  
  public void testRoundIntegralDoubleToInt() {
    for (double d : INTEGRAL_DOUBLE_CANDIDATES) {
      for (RoundingMode mode : ALL_SAFE_ROUNDING_MODES) {
        BigDecimal expected = new BigDecimal(d).setScale(0, mode);
        boolean isInBounds = expected.compareTo(MAX_INT_AS_BIG_DECIMAL) <= 0
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

  public void testRoundFractionalDoubleToInt() {
    for (double d : FRACTIONAL_DOUBLE_CANDIDATES) {
      for (RoundingMode mode : ALL_SAFE_ROUNDING_MODES) {
        BigDecimal expected = new BigDecimal(d).setScale(0, mode);
        boolean isInBounds = expected.compareTo(MAX_INT_AS_BIG_DECIMAL) <= 0
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

  public void testRoundExactIntegralDoubleToInt() {
    for (double d : INTEGRAL_DOUBLE_CANDIDATES) {
      BigDecimal expected = new BigDecimal(d).setScale(0, UNNECESSARY);
      boolean isInBounds = expected.compareTo(MAX_INT_AS_BIG_DECIMAL) <= 0
          & expected.compareTo(MIN_INT_AS_BIG_DECIMAL) >= 0;

      try {
        assertEquals(expected.intValue(), DoubleMath.roundToInt(d, UNNECESSARY));
        assertTrue(isInBounds);
      } catch (ArithmeticException e) {
        assertFalse(isInBounds);
      }
    }
  }

  public void testRoundExactFractionalDoubleToIntFails() {
    for (double d : FRACTIONAL_DOUBLE_CANDIDATES) {
      try {
        DoubleMath.roundToInt(d, UNNECESSARY);
        fail("Expected ArithmeticException");
      } catch (ArithmeticException expected) {}
    }
  }

  public void testRoundNaNToIntAlwaysFails() {
    for (RoundingMode mode : ALL_ROUNDING_MODES) {
      try {
        DoubleMath.roundToInt(Double.NaN, mode);
        fail("Expected ArithmeticException");
      } catch (ArithmeticException expected) {}
    }
  }

  public void testRoundInfiniteToIntAlwaysFails() {
    for (RoundingMode mode : ALL_ROUNDING_MODES) {
      try {
        DoubleMath.roundToInt(Double.POSITIVE_INFINITY, mode);
        fail("Expected ArithmeticException");
      } catch (ArithmeticException expected) {}
      try {
        DoubleMath.roundToInt(Double.NEGATIVE_INFINITY, mode);
        fail("Expected ArithmeticException");
      } catch (ArithmeticException expected) {}
    }
  }

  public void testRoundIntegralDoubleToLong() {
    for (double d : INTEGRAL_DOUBLE_CANDIDATES) {
      for (RoundingMode mode : ALL_SAFE_ROUNDING_MODES) {
        BigDecimal expected = new BigDecimal(d).setScale(0, mode);
        boolean isInBounds = expected.compareTo(MAX_LONG_AS_BIG_DECIMAL) <= 0
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

  public void testRoundFractionalDoubleToLong() {
    for (double d : FRACTIONAL_DOUBLE_CANDIDATES) {
      for (RoundingMode mode : ALL_SAFE_ROUNDING_MODES) {
        BigDecimal expected = new BigDecimal(d).setScale(0, mode);
        boolean isInBounds = expected.compareTo(MAX_LONG_AS_BIG_DECIMAL) <= 0
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

  public void testRoundExactIntegralDoubleToLong() {
    for (double d : INTEGRAL_DOUBLE_CANDIDATES) {
      // every mode except UNNECESSARY
      BigDecimal expected = new BigDecimal(d).setScale(0, UNNECESSARY);
      boolean isInBounds = expected.compareTo(MAX_LONG_AS_BIG_DECIMAL) <= 0
          & expected.compareTo(MIN_LONG_AS_BIG_DECIMAL) >= 0;

      try {
        assertEquals(expected.longValue(), DoubleMath.roundToLong(d, UNNECESSARY));
        assertTrue(isInBounds);
      } catch (ArithmeticException e) {
        assertFalse(isInBounds);
      }
    }
  }

  public void testRoundExactFractionalDoubleToLongFails() {
    for (double d : FRACTIONAL_DOUBLE_CANDIDATES) {
      try {
        DoubleMath.roundToLong(d, UNNECESSARY);
        fail("Expected ArithmeticException");
      } catch (ArithmeticException expected) {}
    }
  }

  public void testRoundNaNToLongAlwaysFails() {
    for (RoundingMode mode : ALL_ROUNDING_MODES) {
      try {
        DoubleMath.roundToLong(Double.NaN, mode);
        fail("Expected ArithmeticException");
      } catch (ArithmeticException expected) {}
    }
  }

  public void testRoundInfiniteToLongAlwaysFails() {
    for (RoundingMode mode : ALL_ROUNDING_MODES) {
      try {
        DoubleMath.roundToLong(Double.POSITIVE_INFINITY, mode);
        fail("Expected ArithmeticException");
      } catch (ArithmeticException expected) {}
      try {
        DoubleMath.roundToLong(Double.NEGATIVE_INFINITY, mode);
        fail("Expected ArithmeticException");
      } catch (ArithmeticException expected) {}
    }
  }

  public void testRoundIntegralDoubleToBigInteger() {
    for (double d : INTEGRAL_DOUBLE_CANDIDATES) {
      for (RoundingMode mode : ALL_SAFE_ROUNDING_MODES) {
        BigDecimal expected = new BigDecimal(d).setScale(0, mode);
        assertEquals(expected.toBigInteger(), DoubleMath.roundToBigInteger(d, mode));
      }
    }
  }

  public void testRoundFractionalDoubleToBigInteger() {
    for (double d : FRACTIONAL_DOUBLE_CANDIDATES) {
      for (RoundingMode mode : ALL_SAFE_ROUNDING_MODES) {
        BigDecimal expected = new BigDecimal(d).setScale(0, mode);
        assertEquals(expected.toBigInteger(), DoubleMath.roundToBigInteger(d, mode));
      }
    }
  }

  public void testRoundExactIntegralDoubleToBigInteger() {
    for (double d : INTEGRAL_DOUBLE_CANDIDATES) {
      BigDecimal expected = new BigDecimal(d).setScale(0, UNNECESSARY);
      assertEquals(expected.toBigInteger(), DoubleMath.roundToBigInteger(d, UNNECESSARY));
    }
  }

  public void testRoundExactFractionalDoubleToBigIntegerFails() {
    for (double d : FRACTIONAL_DOUBLE_CANDIDATES) {
      try {
        DoubleMath.roundToBigInteger(d, UNNECESSARY);
        fail("Expected ArithmeticException");
      } catch (ArithmeticException expected) {}
    }
  }

  public void testRoundNaNToBigIntegerAlwaysFails() {
    for (RoundingMode mode : ALL_ROUNDING_MODES) {
      try {
        DoubleMath.roundToBigInteger(Double.NaN, mode);
        fail("Expected ArithmeticException");
      } catch (ArithmeticException expected) {}
    }
  }

  public void testRoundInfiniteToBigIntegerAlwaysFails() {
    for (RoundingMode mode : ALL_ROUNDING_MODES) {
      try {
        DoubleMath.roundToBigInteger(Double.POSITIVE_INFINITY, mode);
        fail("Expected ArithmeticException");
      } catch (ArithmeticException expected) {}
      try {
        DoubleMath.roundToBigInteger(Double.NEGATIVE_INFINITY, mode);
        fail("Expected ArithmeticException");
      } catch (ArithmeticException expected) {}
    }
  }

  public void testRoundLog2Floor() {
    for (double d : POSITIVE_FINITE_DOUBLE_CANDIDATES) {
      int log2 = DoubleMath.log2(d, FLOOR);
      assertTrue(StrictMath.pow(2.0, log2) <= d);
      assertTrue(StrictMath.pow(2.0, log2 + 1) > d);
    }
  }

  public void testRoundLog2Ceiling() {
    for (double d : POSITIVE_FINITE_DOUBLE_CANDIDATES) {
      int log2 = DoubleMath.log2(d, CEILING);
      assertTrue(StrictMath.pow(2.0, log2) >= d);
      double z = StrictMath.pow(2.0, log2 - 1);
      assertTrue(z < d);
    }
  }

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

  public void testRoundLog2Half() {
    // We don't expect perfect rounding accuracy.
    for (int exp : asList(-1022, -50, -1, 0, 1, 2, 3, 4, 100, 1022, 1023)) {
      for (RoundingMode mode : asList(HALF_EVEN, HALF_UP, HALF_DOWN)) {
        double x = Math.scalb(Math.sqrt(2) + 0.001, exp);
        double y = Math.scalb(Math.sqrt(2) - 0.001, exp);
        if (exp < 0) {
          assertEquals(exp + 1, DoubleMath.log2(x, mode));
          assertEquals(exp, DoubleMath.log2(y, mode));
        } else {
          assertEquals(exp + 1, DoubleMath.log2(x, mode));
          assertEquals(exp, DoubleMath.log2(y, mode));
        }
      }
    }
  }

  public void testRoundLog2ThrowsOnZerosInfinitiesAndNaN() {
    for (RoundingMode mode : ALL_ROUNDING_MODES) {
      for (double d :
          asList(0.0, -0.0, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NaN)) {
        try {
          DoubleMath.log2(d, mode);
          fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {}
      }
    }
  }

  public void testRoundLog2ThrowsOnNegative() {
    for (RoundingMode mode : ALL_ROUNDING_MODES) {
      for (double d : POSITIVE_FINITE_DOUBLE_CANDIDATES) {
        try {
          DoubleMath.log2(-d, mode);
          fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {}
      }
    }
  }

  public void testIsPowerOfTwoYes() {
    for (int i = -1074; i <= 1023; i++) {
      assertTrue(DoubleMath.isPowerOfTwo(StrictMath.pow(2.0, i)));
    }
  }

  public void testIsPowerOfTwo() {
    for (double x : ALL_DOUBLE_CANDIDATES) {
      boolean expected = x > 0 && !Double.isInfinite(x) && !Double.isNaN(x)
          && StrictMath.pow(2.0, DoubleMath.log2(x, FLOOR)) == x;
      assertEquals(expected, DoubleMath.isPowerOfTwo(x));
    }
  }

  public void testLog2Accuracy() {
    for (double d : POSITIVE_FINITE_DOUBLE_CANDIDATES) {
      double dmLog2 = DoubleMath.log2(d);
      double trueLog2 = trueLog2(d);
      assertTrue(Math.abs(dmLog2 - trueLog2) <= Math.ulp(trueLog2));
    }
  }

  public void testLog2SemiMonotonic(){
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
    assertEquals(Double.NEGATIVE_INFINITY, DoubleMath.log2(0.0));
    assertEquals(Double.NEGATIVE_INFINITY, DoubleMath.log2(-0.0));
  }

  public void testLog2NaNInfinity() {
    assertEquals(Double.POSITIVE_INFINITY, DoubleMath.log2(Double.POSITIVE_INFINITY));
    assertTrue(Double.isNaN(DoubleMath.log2(Double.NEGATIVE_INFINITY)));
    assertTrue(Double.isNaN(DoubleMath.log2(Double.NaN)));
  }

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

  public void testIsMathematicalIntegerIntegral() {
    for (double d : INTEGRAL_DOUBLE_CANDIDATES) {
      assertTrue(DoubleMath.isMathematicalInteger(d));
    }
  }

  public void testIsMathematicalIntegerFractional() {
    for (double d : FRACTIONAL_DOUBLE_CANDIDATES) {
      assertFalse(DoubleMath.isMathematicalInteger(d));
    }
  }

  public void testIsMathematicalIntegerNotFinite() {
    for (double d :
        Arrays.asList(Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NaN)) {
      assertFalse(DoubleMath.isMathematicalInteger(d));
    }
  }

  public void testFactorial() {
    for (int i = 0; i <= DoubleMath.MAX_FACTORIAL; i++) {
      double actual = BigIntegerMath.factorial(i).doubleValue();
      double result = DoubleMath.factorial(i);
      assertEquals(actual, result, Math.ulp(actual));
    }
  }

  public void testFactorialTooHigh() {
    assertEquals(Double.POSITIVE_INFINITY, DoubleMath.factorial(DoubleMath.MAX_FACTORIAL + 1));
    assertEquals(Double.POSITIVE_INFINITY, DoubleMath.factorial(DoubleMath.MAX_FACTORIAL + 20));
  }

  public void testFactorialNegative() {
    for (int n : NEGATIVE_INTEGER_CANDIDATES) {
      try {
        DoubleMath.factorial(n);
        fail("Expected IllegalArgumentException");
      } catch (IllegalArgumentException expected) {}
    }
  }
  
  public void testNullPointers() throws Exception {
    NullPointerTester tester = new NullPointerTester();
    tester.setDefault(RoundingMode.class, FLOOR);
    tester.setDefault(double.class, 3.0);
    tester.testAllPublicStaticMethods(DoubleMath.class);
  }
}
