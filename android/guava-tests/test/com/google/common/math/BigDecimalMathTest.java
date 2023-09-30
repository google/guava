/*
 * Copyright (C) 2020 The Guava Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.common.math;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;
import static java.math.RoundingMode.CEILING;
import static java.math.RoundingMode.DOWN;
import static java.math.RoundingMode.FLOOR;
import static java.math.RoundingMode.HALF_DOWN;
import static java.math.RoundingMode.HALF_EVEN;
import static java.math.RoundingMode.HALF_UP;
import static java.math.RoundingMode.UNNECESSARY;
import static java.math.RoundingMode.UP;
import static java.math.RoundingMode.values;
import static org.junit.Assert.assertThrows;

import com.google.common.annotations.GwtIncompatible;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import junit.framework.TestCase;

@GwtIncompatible
public class BigDecimalMathTest extends TestCase {
  private static final class RoundToDoubleTester {
    private final BigDecimal input;
    private final Map<RoundingMode, Double> expectedValues = new EnumMap<>(RoundingMode.class);
    private boolean unnecessaryShouldThrow = false;

    RoundToDoubleTester(BigDecimal input) {
      this.input = input;
    }

    RoundToDoubleTester setExpectation(double expectedValue, RoundingMode... modes) {
      for (RoundingMode mode : modes) {
        Double previous = expectedValues.put(mode, expectedValue);
        if (previous != null) {
          throw new AssertionError();
        }
      }
      return this;
    }

    public RoundToDoubleTester roundUnnecessaryShouldThrow() {
      unnecessaryShouldThrow = true;
      return this;
    }

    public void test() {
      assertThat(expectedValues.keySet())
          .containsAtLeastElementsIn(EnumSet.complementOf(EnumSet.of(UNNECESSARY)));
      for (Map.Entry<RoundingMode, Double> entry : expectedValues.entrySet()) {
        RoundingMode mode = entry.getKey();
        Double expectation = entry.getValue();
        assertWithMessage("roundToDouble(" + input + ", " + mode + ")")
            .that(BigDecimalMath.roundToDouble(input, mode))
            .isEqualTo(expectation);
      }

      if (!expectedValues.containsKey(UNNECESSARY)) {
        assertWithMessage("Expected roundUnnecessaryShouldThrow call")
            .that(unnecessaryShouldThrow)
            .isTrue();
        assertThrows(
            "Expected ArithmeticException for roundToDouble(" + input + ", UNNECESSARY)",
            ArithmeticException.class,
            () -> BigDecimalMath.roundToDouble(input, UNNECESSARY));
      }
    }
  }

  public void testRoundToDouble_zero() {
    new RoundToDoubleTester(BigDecimal.ZERO).setExpectation(0.0, values()).test();
  }

  public void testRoundToDouble_oneThird() {
    new RoundToDoubleTester(
            BigDecimal.ONE.divide(BigDecimal.valueOf(3), new MathContext(50, HALF_EVEN)))
        .roundUnnecessaryShouldThrow()
        .setExpectation(0.33333333333333337, UP, CEILING)
        .setExpectation(0.3333333333333333, HALF_EVEN, FLOOR, DOWN, HALF_UP, HALF_DOWN)
        .test();
  }

  public void testRoundToDouble_halfMinDouble() {
    BigDecimal minDouble = new BigDecimal(Double.MIN_VALUE);
    BigDecimal halfMinDouble = minDouble.divide(BigDecimal.valueOf(2));
    new RoundToDoubleTester(halfMinDouble)
        .roundUnnecessaryShouldThrow()
        .setExpectation(Double.MIN_VALUE, UP, CEILING, HALF_UP)
        .setExpectation(0.0, HALF_EVEN, FLOOR, DOWN, HALF_DOWN)
        .test();
  }

  public void testRoundToDouble_halfNegativeMinDouble() {
    BigDecimal minDouble = new BigDecimal(-Double.MIN_VALUE);
    BigDecimal halfMinDouble = minDouble.divide(BigDecimal.valueOf(2));
    new RoundToDoubleTester(halfMinDouble)
        .roundUnnecessaryShouldThrow()
        .setExpectation(-Double.MIN_VALUE, UP, FLOOR, HALF_UP)
        .setExpectation(-0.0, HALF_EVEN, CEILING, DOWN, HALF_DOWN)
        .test();
  }

  public void testRoundToDouble_smallPositive() {
    new RoundToDoubleTester(BigDecimal.valueOf(16)).setExpectation(16.0, values()).test();
  }

  public void testRoundToDouble_maxPreciselyRepresentable() {
    new RoundToDoubleTester(BigDecimal.valueOf(1L << 53))
        .setExpectation(Math.pow(2, 53), values())
        .test();
  }

  public void testRoundToDouble_maxPreciselyRepresentablePlusOne() {
    double twoToThe53 = Math.pow(2, 53);
    // the representable doubles are 2^53 and 2^53 + 2.
    // 2^53+1 is halfway between, so HALF_UP will go up and HALF_DOWN will go down.
    new RoundToDoubleTester(BigDecimal.valueOf((1L << 53) + 1))
        .setExpectation(twoToThe53, DOWN, FLOOR, HALF_DOWN, HALF_EVEN)
        .setExpectation(Math.nextUp(twoToThe53), CEILING, UP, HALF_UP)
        .roundUnnecessaryShouldThrow()
        .test();
  }

  public void testRoundToDouble_twoToThe54PlusOne() {
    double twoToThe54 = Math.pow(2, 54);
    // the representable doubles are 2^54 and 2^54 + 4
    // 2^54+1 is less than halfway between, so HALF_DOWN and HALF_UP will both go down.
    new RoundToDoubleTester(BigDecimal.valueOf((1L << 54) + 1))
        .setExpectation(twoToThe54, DOWN, FLOOR, HALF_DOWN, HALF_UP, HALF_EVEN)
        .setExpectation(Math.nextUp(twoToThe54), CEILING, UP)
        .roundUnnecessaryShouldThrow()
        .test();
  }

  public void testRoundToDouble_twoToThe54PlusOneHalf() {
    double twoToThe54 = Math.pow(2, 54);
    // the representable doubles are 2^54 and 2^54 + 4
    // 2^54+1 is less than halfway between, so HALF_DOWN and HALF_UP will both go down.
    new RoundToDoubleTester(BigDecimal.valueOf(1L << 54).add(new BigDecimal(0.5)))
        .setExpectation(twoToThe54, DOWN, FLOOR, HALF_DOWN, HALF_UP, HALF_EVEN)
        .setExpectation(Math.nextUp(twoToThe54), CEILING, UP)
        .roundUnnecessaryShouldThrow()
        .test();
  }

  public void testRoundToDouble_twoToThe54PlusThree() {
    double twoToThe54 = Math.pow(2, 54);
    // the representable doubles are 2^54 and 2^54 + 4
    // 2^54+3 is more than halfway between, so HALF_DOWN and HALF_UP will both go up.
    new RoundToDoubleTester(BigDecimal.valueOf((1L << 54) + 3))
        .setExpectation(twoToThe54, DOWN, FLOOR)
        .setExpectation(Math.nextUp(twoToThe54), CEILING, UP, HALF_DOWN, HALF_UP, HALF_EVEN)
        .roundUnnecessaryShouldThrow()
        .test();
  }

  public void testRoundToDouble_twoToThe54PlusFour() {
    new RoundToDoubleTester(BigDecimal.valueOf((1L << 54) + 4))
        .setExpectation(Math.pow(2, 54) + 4, values())
        .test();
  }

  public void testRoundToDouble_maxDouble() {
    BigDecimal maxDoubleAsBD = new BigDecimal(Double.MAX_VALUE);
    new RoundToDoubleTester(maxDoubleAsBD).setExpectation(Double.MAX_VALUE, values()).test();
  }

  public void testRoundToDouble_maxDoublePlusOne() {
    BigDecimal maxDoubleAsBD = new BigDecimal(Double.MAX_VALUE).add(BigDecimal.ONE);
    new RoundToDoubleTester(maxDoubleAsBD)
        .setExpectation(Double.MAX_VALUE, DOWN, FLOOR, HALF_EVEN, HALF_UP, HALF_DOWN)
        .setExpectation(Double.POSITIVE_INFINITY, UP, CEILING)
        .roundUnnecessaryShouldThrow()
        .test();
  }

  public void testRoundToDouble_wayTooBig() {
    BigDecimal bi = BigDecimal.valueOf(2).pow(2 * Double.MAX_EXPONENT);
    new RoundToDoubleTester(bi)
        .setExpectation(Double.MAX_VALUE, DOWN, FLOOR, HALF_EVEN, HALF_UP, HALF_DOWN)
        .setExpectation(Double.POSITIVE_INFINITY, UP, CEILING)
        .roundUnnecessaryShouldThrow()
        .test();
  }

  public void testRoundToDouble_smallNegative() {
    new RoundToDoubleTester(BigDecimal.valueOf(-16)).setExpectation(-16.0, values()).test();
  }

  public void testRoundToDouble_minPreciselyRepresentable() {
    new RoundToDoubleTester(BigDecimal.valueOf(-1L << 53))
        .setExpectation(-Math.pow(2, 53), values())
        .test();
  }

  public void testRoundToDouble_minPreciselyRepresentableMinusOne() {
    // the representable doubles are -2^53 and -2^53 - 2.
    // -2^53-1 is halfway between, so HALF_UP will go up and HALF_DOWN will go down.
    new RoundToDoubleTester(BigDecimal.valueOf((-1L << 53) - 1))
        .setExpectation(-Math.pow(2, 53), DOWN, CEILING, HALF_DOWN, HALF_EVEN)
        .setExpectation(DoubleUtils.nextDown(-Math.pow(2, 53)), FLOOR, UP, HALF_UP)
        .roundUnnecessaryShouldThrow()
        .test();
  }

  public void testRoundToDouble_negativeTwoToThe54MinusOne() {
    new RoundToDoubleTester(BigDecimal.valueOf((-1L << 54) - 1))
        .setExpectation(-Math.pow(2, 54), DOWN, CEILING, HALF_DOWN, HALF_UP, HALF_EVEN)
        .setExpectation(DoubleUtils.nextDown(-Math.pow(2, 54)), FLOOR, UP)
        .roundUnnecessaryShouldThrow()
        .test();
  }

  public void testRoundToDouble_negativeTwoToThe54MinusThree() {
    new RoundToDoubleTester(BigDecimal.valueOf((-1L << 54) - 3))
        .setExpectation(-Math.pow(2, 54), DOWN, CEILING)
        .setExpectation(
            DoubleUtils.nextDown(-Math.pow(2, 54)), FLOOR, UP, HALF_DOWN, HALF_UP, HALF_EVEN)
        .roundUnnecessaryShouldThrow()
        .test();
  }

  public void testRoundToDouble_negativeTwoToThe54MinusFour() {
    new RoundToDoubleTester(BigDecimal.valueOf((-1L << 54) - 4))
        .setExpectation(-Math.pow(2, 54) - 4, values())
        .test();
  }

  public void testRoundToDouble_minDouble() {
    BigDecimal minDoubleAsBD = new BigDecimal(-Double.MAX_VALUE);
    new RoundToDoubleTester(minDoubleAsBD).setExpectation(-Double.MAX_VALUE, values()).test();
  }

  public void testRoundToDouble_minDoubleMinusOne() {
    BigDecimal minDoubleAsBD = new BigDecimal(-Double.MAX_VALUE).subtract(BigDecimal.ONE);
    new RoundToDoubleTester(minDoubleAsBD)
        .setExpectation(-Double.MAX_VALUE, DOWN, CEILING, HALF_EVEN, HALF_UP, HALF_DOWN)
        .setExpectation(Double.NEGATIVE_INFINITY, UP, FLOOR)
        .roundUnnecessaryShouldThrow()
        .test();
  }

  public void testRoundToDouble_negativeWayTooBig() {
    BigDecimal bi = BigDecimal.valueOf(2).pow(2 * Double.MAX_EXPONENT).negate();
    new RoundToDoubleTester(bi)
        .setExpectation(-Double.MAX_VALUE, DOWN, CEILING, HALF_EVEN, HALF_UP, HALF_DOWN)
        .setExpectation(Double.NEGATIVE_INFINITY, UP, FLOOR)
        .roundUnnecessaryShouldThrow()
        .test();
  }
}
