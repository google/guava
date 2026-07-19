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

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.math.MathPreconditions.checkNonNegative;

import com.google.common.annotations.Beta;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.annotations.J2ktIncompatible;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;

/**
 * A class for arithmetic on {@link BigDecimal} that is not covered by its built-in methods.
 *
 * @author Louis Wasserman
 * @since 30.0
 */
@J2ktIncompatible
@GwtIncompatible
public final class BigDecimalMath {
  private BigDecimalMath() {}

  /**
   * Returns {@code x} converted to a {@link BigInteger}, ensuring that the magnitude of {@code x}
   * does not exceed {@code maxIntegerDigits} before performing conversion.
   *
   * <p>Converting a {@link BigDecimal} with a very large negative scale to a {@code BigInteger}
   * requires materializing the unscaled value multiplied by {@code 10^-scale}. When {@code x} is
   * parsed from uncontrolled string or JSON inputs (such as {@code "1e123456789"}), directly
   * calling {@link BigDecimal#toBigInteger()} or {@link BigDecimal#toBigIntegerExact()} can exhaust
   * CPU and memory. This method safely verifies that the number of decimal digits required for the
   * integer representation fits within {@code maxIntegerDigits} before conversion.
   *
   * <p>Choosing a reasonable value for {@code maxIntegerDigits} depends on the range of expected
   * valid values for your input. A value of 100 is generally sufficient for most common use cases
   * and provides a large safety margin against resource exhaustion from malicious inputs.
   *
   * @param x the {@code BigDecimal} to convert to a {@code BigInteger}
   * @param maxIntegerDigits the maximum allowable number of digits in the integer part (for values
   *     where {@code |x| < 1}, the integer part is considered to have 0 digits)
   * @return {@code x} converted to a {@code BigInteger}
   * @throws IllegalArgumentException if {@code maxIntegerDigits < 0}
   * @throws ArithmeticException if the integer part of {@code x} requires more than {@code
   *     maxIntegerDigits} digits
   * @since NEXT
   */
  @Beta
  public static BigInteger toBigInteger(BigDecimal x, int maxIntegerDigits) {
    checkNotNull(x);
    checkNonNegative("maxIntegerDigits", maxIntegerDigits);
    // If x is 0 or |x| < 1 (i.e. precision <= scale), the integer part is BigInteger.ZERO.
    if (x.signum() == 0 || x.precision() <= x.scale()) {
      return BigInteger.ZERO;
    }
    checkMaxIntegerDigits(x, maxIntegerDigits);
    return x.toBigInteger();
  }

  /**
   * Returns {@code x} converted to a {@link BigInteger}, checking that {@code x} has no fractional
   * part and ensuring that the magnitude of {@code x} does not exceed {@code maxIntegerDigits}
   * before performing conversion.
   *
   * <p>Converting a {@link BigDecimal} with a very large negative scale to a {@code BigInteger}
   * requires materializing the unscaled value multiplied by {@code 10^-scale}. When {@code x} is
   * parsed from uncontrolled string or JSON inputs (such as {@code "1e123456789"}), directly
   * calling {@link BigDecimal#toBigInteger()} or {@link BigDecimal#toBigIntegerExact()} can exhaust
   * CPU and memory. This method safely verifies that the number of decimal digits required for the
   * integer representation fits within {@code maxIntegerDigits} before conversion.
   *
   * <p>Choosing a reasonable value for {@code maxIntegerDigits} depends on the range of expected
   * valid values for your input. A value of 100 is generally sufficient for most common use cases
   * and provides a large safety margin against resource exhaustion from malicious inputs.
   *
   * @param x the {@code BigDecimal} to convert to a {@code BigInteger}
   * @param maxIntegerDigits the maximum allowable number of digits in the integer part (for values
   *     where {@code |x| < 1}, the integer part is considered to have 0 digits)
   * @return {@code x} converted to a {@code BigInteger}
   * @throws IllegalArgumentException if {@code maxIntegerDigits < 0}
   * @throws ArithmeticException if {@code x} has a nonzero fractional part or if the integer part
   *     of {@code x} requires more than {@code maxIntegerDigits} digits
   * @since NEXT
   */
  @Beta
  public static BigInteger toBigIntegerExact(BigDecimal x, int maxIntegerDigits) {
    checkNotNull(x);
    checkNonNegative("maxIntegerDigits", maxIntegerDigits);
    // If x is 0, regardless of scale, the integer value is BigInteger.ZERO.
    if (x.signum() == 0) {
      return BigInteger.ZERO;
    }
    // If |x| < 1 (i.e. precision <= scale) and x != 0, x has a nonzero fractional part and cannot
    // be represented exactly as a BigInteger.
    if (x.precision() <= x.scale()) {
      throw new ArithmeticException(
          x + " has a nonzero fractional part and cannot be represented exactly as a BigInteger.");
    }
    checkMaxIntegerDigits(x, maxIntegerDigits);
    return x.toBigIntegerExact();
  }

  private static void checkMaxIntegerDigits(BigDecimal x, int maxIntegerDigits) {
    long integerDigits = (long) x.precision() - x.scale();
    if (integerDigits > maxIntegerDigits) {
      throw new ArithmeticException(
          String.format(
              "BigDecimal (%s) requires %s integer digits, which is > maxIntegerDigits (%s)",
              x, integerDigits, maxIntegerDigits));
    }
  }

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

  private static final class BigDecimalToDoubleRounder extends ToDoubleRounder<BigDecimal> {
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
