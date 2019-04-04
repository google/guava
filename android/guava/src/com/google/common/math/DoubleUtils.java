/*
 * Copyright (C) 2011 The Guava Authors
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
import com.google.common.annotations.VisibleForTesting;

import static com.google.common.base.Preconditions.checkArgument;
import static java.lang.Double.*;
import static java.lang.Math.getExponent;

/**
 * Utilities for {@code double} primitives.
 *
 * @author Louis Wasserman
 */
@GwtIncompatible
final class DoubleUtils {
  private DoubleUtils() {}

  static double nextDown(double d) {
    return -Math.nextUp(-d);
  }

  // The mask for the significand, according to the {@link
  // Double#doubleToRawLongBits(double)} spec.
  static final long SIGNIFICAND_MASK = 0x000fffffffffffffL;

  static final int SIGNIFICAND_BITS = 52;

  /** The implicit 1 bit that is omitted in significands of normal doubles. */
  static final long IMPLICIT_BIT = SIGNIFICAND_MASK + 1;

  static long getSignificand(double d) {
    checkArgument(isFinite(d), "not a normal value");
    int exponent = getExponent(d);
    long bits = doubleToRawLongBits(d);
    bits &= SIGNIFICAND_MASK;
    return (exponent == MIN_EXPONENT - 1) ? bits << 1 : bits | IMPLICIT_BIT;
  }

  static boolean isFinite(double d) {
    return getExponent(d) <= MAX_EXPONENT;
  }

  static boolean isNormal(double d) {
    return getExponent(d) >= MIN_EXPONENT;
  }

  /*
   * Returns x scaled by a power of 2 such that it is in the range [1, 2). Assumes x is positive,
   * normal, and finite.
   */
  static double scaleNormalize(double x) {
    long significand = doubleToRawLongBits(x) & SIGNIFICAND_MASK;
    return longBitsToDouble(significand | ONE_BITS);
  }

  /** Returns its argument if it is non-negative, zero if it is negative. */
  static double ensureNonNegative(double value) {
    checkArgument(!isNaN(value));
    if (value > 0.0) {
      return value;
    } else {
      return 0.0;
    }
  }

  @VisibleForTesting static final long ONE_BITS = 0x3ff0000000000000L;
}
