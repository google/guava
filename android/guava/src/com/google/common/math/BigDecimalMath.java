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

import com.google.common.annotations.GwtIncompatible;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * A class for arithmetic on {@link BigDecimal} that is not covered by its built-in methods.
 *
 * @author Louis Wasserman
 * @since 30.0
 */
@GwtIncompatible
@ElementTypesAreNonnullByDefault
public class BigDecimalMath {
  private BigDecimalMath() {}

  /**
   * Returns {@code x}, rounded to a {@code double} with the specified rounding mode. If {@code x}
   * is precisely representable as a {@code double}, its {@code double} value will be returned;
   * otherwise, the rounding will choose between the two nearest representable values with {@code
   * mode}.
   *
   * <p>For the case of {@link RoundingMode#HALF_DOWN}, {@code HALF_UP}, and {@code HALF_EVEN},
   * infinite {@code double} values are considered infinitely far away. For example, 2^2000 is not
   * representable as a double, but {@code roundToDouble(BigDecimal.valueOf(2).pow(2000), HALF_UP)}
   * will return {@code Double.MAX_VALUE}, not {@code Double.POSITIVE_INFINITY}.
   *
   * <p>For the case of {@link RoundingMode#HALF_EVEN}, this implementation uses the IEEE 754
   * default rounding mode: if the two nearest representable values are equally near, the one with
   * the least significant bit zero is chosen. (In such cases, both of the nearest representable
   * values are even integers; this method returns the one that is a multiple of a greater power of
   * two.)
   *
   * @throws ArithmeticException if {@code mode} is {@link RoundingMode#UNNECESSARY} and {@code x}
   *     is not precisely representable as a {@code double}
   * @since 30.0
   */
  public static double roundToDouble(BigDecimal x, RoundingMode mode) {
    return BigDecimalToDoubleRounder.INSTANCE.roundToDouble(x, mode);
  }

  private static class BigDecimalToDoubleRounder extends ToDoubleRounder<BigDecimal> {
    static final BigDecimalToDoubleRounder INSTANCE = new BigDecimalToDoubleRounder();

    private BigDecimalToDoubleRounder() {}

    @Override
    double roundToDoubleArbitrarily(BigDecimal bigDecimal) {
      return bigDecimal.doubleValue();
    }

    @Override
    int sign(BigDecimal bigDecimal) {
      return bigDecimal.signum();
    }

    @Override
    BigDecimal toX(double d, RoundingMode mode) {
      return new BigDecimal(d);
    }

    @Override
    BigDecimal minus(BigDecimal a, BigDecimal b) {
      return a.subtract(b);
    }
  }
}
