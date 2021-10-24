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
import static com.google.common.math.MathPreconditions.checkRoundingUnnecessary;

import com.google.common.annotations.GwtIncompatible;
import java.math.RoundingMode;

/**
 * Helper type to implement rounding {@code X} to a representable {@code double} value according to
 * a {@link RoundingMode}.
 */
@GwtIncompatible
@ElementTypesAreNonnullByDefault
abstract class ToDoubleRounder<X extends Number & Comparable<X>> {
  /**
   * Returns x rounded to either the greatest double less than or equal to the precise value of x,
   * or the least double greater than or equal to the precise value of x.
   */
  abstract double roundToDoubleArbitrarily(X x);

  /** Returns the sign of x: either -1, 0, or 1. */
  abstract int sign(X x);

  /** Returns d's value as an X, rounded with the specified mode. */
  abstract X toX(double d, RoundingMode mode);

  /** Returns a - b, guaranteed that both arguments are nonnegative. */
  abstract X minus(X a, X b);

  /** Rounds {@code x} to a {@code double}. */
  final double roundToDouble(X x, RoundingMode mode) {
    checkNotNull(x, "x");
    checkNotNull(mode, "mode");
    double roundArbitrarily = roundToDoubleArbitrarily(x);
    if (Double.isInfinite(roundArbitrarily)) {
      switch (mode) {
        case DOWN:
        case HALF_EVEN:
        case HALF_DOWN:
        case HALF_UP:
          return Double.MAX_VALUE * sign(x);
        case FLOOR:
          return (roundArbitrarily == Double.POSITIVE_INFINITY)
              ? Double.MAX_VALUE
              : Double.NEGATIVE_INFINITY;
        case CEILING:
          return (roundArbitrarily == Double.POSITIVE_INFINITY)
              ? Double.POSITIVE_INFINITY
              : -Double.MAX_VALUE;
        case UP:
          return roundArbitrarily;
        case UNNECESSARY:
          throw new ArithmeticException(x + " cannot be represented precisely as a double");
      }
    }
    X roundArbitrarilyAsX = toX(roundArbitrarily, RoundingMode.UNNECESSARY);
    int cmpXToRoundArbitrarily = x.compareTo(roundArbitrarilyAsX);
    switch (mode) {
      case UNNECESSARY:
        checkRoundingUnnecessary(cmpXToRoundArbitrarily == 0);
        return roundArbitrarily;
      case FLOOR:
        return (cmpXToRoundArbitrarily >= 0)
            ? roundArbitrarily
            : DoubleUtils.nextDown(roundArbitrarily);
      case CEILING:
        return (cmpXToRoundArbitrarily <= 0) ? roundArbitrarily : Math.nextUp(roundArbitrarily);
      case DOWN:
        if (sign(x) >= 0) {
          return (cmpXToRoundArbitrarily >= 0)
              ? roundArbitrarily
              : DoubleUtils.nextDown(roundArbitrarily);
        } else {
          return (cmpXToRoundArbitrarily <= 0) ? roundArbitrarily : Math.nextUp(roundArbitrarily);
        }
      case UP:
        if (sign(x) >= 0) {
          return (cmpXToRoundArbitrarily <= 0) ? roundArbitrarily : Math.nextUp(roundArbitrarily);
        } else {
          return (cmpXToRoundArbitrarily >= 0)
              ? roundArbitrarily
              : DoubleUtils.nextDown(roundArbitrarily);
        }
      case HALF_DOWN:
      case HALF_UP:
      case HALF_EVEN:
        {
          X roundFloor;
          double roundFloorAsDouble;
          X roundCeiling;
          double roundCeilingAsDouble;

          if (cmpXToRoundArbitrarily >= 0) {
            roundFloorAsDouble = roundArbitrarily;
            roundFloor = roundArbitrarilyAsX;
            roundCeilingAsDouble = Math.nextUp(roundArbitrarily);
            if (roundCeilingAsDouble == Double.POSITIVE_INFINITY) {
              return roundFloorAsDouble;
            }
            roundCeiling = toX(roundCeilingAsDouble, RoundingMode.CEILING);
          } else {
            roundCeilingAsDouble = roundArbitrarily;
            roundCeiling = roundArbitrarilyAsX;
            roundFloorAsDouble = DoubleUtils.nextDown(roundArbitrarily);
            if (roundFloorAsDouble == Double.NEGATIVE_INFINITY) {
              return roundCeilingAsDouble;
            }
            roundFloor = toX(roundFloorAsDouble, RoundingMode.FLOOR);
          }

          X deltaToFloor = minus(x, roundFloor);
          X deltaToCeiling = minus(roundCeiling, x);
          int diff = deltaToFloor.compareTo(deltaToCeiling);
          if (diff < 0) { // closer to floor
            return roundFloorAsDouble;
          } else if (diff > 0) { // closer to ceiling
            return roundCeilingAsDouble;
          }
          // halfway between the representable values; do the half-whatever logic
          switch (mode) {
            case HALF_EVEN:
              // roundFloorAsDouble and roundCeilingAsDouble are neighbors, so precisely
              // one of them should have an even long representation
              return ((Double.doubleToRawLongBits(roundFloorAsDouble) & 1L) == 0)
                  ? roundFloorAsDouble
                  : roundCeilingAsDouble;
            case HALF_DOWN:
              return (sign(x) >= 0) ? roundFloorAsDouble : roundCeilingAsDouble;
            case HALF_UP:
              return (sign(x) >= 0) ? roundCeilingAsDouble : roundFloorAsDouble;
            default:
              throw new AssertionError("impossible");
          }
        }
    }
    throw new AssertionError("impossible");
  }
}
