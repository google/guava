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

import java.math.BigInteger;
import java.util.Random;

/**
 * Utilities for benchmarks.
 *
 * <p>In many cases, we wish to vary the order of magnitude of the input as much as we want to vary
 * the input itself, so most methods which generate values use an exponential distribution varying
 * the order of magnitude of the generated values uniformly at random.
 *
 * @author Louis Wasserman
 */
final class MathBenchmarking {
  static final int ARRAY_SIZE = 0x10000;
  static final int ARRAY_MASK = 0x0ffff;
  static final Random RANDOM_SOURCE = new Random(314159265358979L);
  static final int MAX_EXPONENT = 100;

  /*
   * Duplicated from LongMath.
   * binomial(biggestBinomials[k], k) fits in a long, but not
   * binomial(biggestBinomials[k] + 1, k).
   */
  static final int[] biggestBinomials = {
    Integer.MAX_VALUE,
    Integer.MAX_VALUE,
    Integer.MAX_VALUE,
    3810779,
    121977,
    16175,
    4337,
    1733,
    887,
    534,
    361,
    265,
    206,
    169,
    143,
    125,
    111,
    101,
    94,
    88,
    83,
    79,
    76,
    74,
    72,
    70,
    69,
    68,
    67,
    67,
    66,
    66,
    66,
    66
  };

  /**
   * Generates values in a distribution equivalent to randomNonNegativeBigInteger but omitting zero.
   */
  static BigInteger randomPositiveBigInteger(int numBits) {
    BigInteger result;
    do {
      result = randomNonNegativeBigInteger(numBits);
    } while (result.signum() == 0);
    return result;
  }

  /**
   * Generates a number in [0, 2^numBits) with an exponential distribution. The floor of the log2 of
   * the result is chosen uniformly at random in [0, numBits), and then the result is chosen in that
   * range uniformly at random. Zero is treated as having log2 == 0.
   */
  static BigInteger randomNonNegativeBigInteger(int numBits) {
    int digits = RANDOM_SOURCE.nextInt(numBits);
    if (digits == 0) {
      return new BigInteger(1, RANDOM_SOURCE);
    } else {
      return new BigInteger(digits, RANDOM_SOURCE).setBit(digits);
    }
  }

  /**
   * Equivalent to calling randomPositiveBigInteger(numBits) and then flipping the sign with 50%
   * probability.
   */
  static BigInteger randomNonZeroBigInteger(int numBits) {
    BigInteger result = randomPositiveBigInteger(numBits);
    return RANDOM_SOURCE.nextBoolean() ? result : result.negate();
  }

  /**
   * Chooses a number in (-2^numBits, 2^numBits) at random, with density concentrated in numbers of
   * lower magnitude.
   */
  static BigInteger randomBigInteger(int numBits) {
    while (true) {
      if (RANDOM_SOURCE.nextBoolean()) {
        return randomNonNegativeBigInteger(numBits);
      }
      BigInteger neg = randomNonNegativeBigInteger(numBits).negate();
      if (neg.signum() != 0) {
        return neg;
      }
    }
  }

  /**
   * Generates a number in [0, 2^numBits) with an exponential distribution. The floor of the log2 of
   * the absolute value of the result is chosen uniformly at random in [0, numBits), and then the
   * result is chosen from those possibilities uniformly at random.
   *
   * <p>Zero is treated as having log2 == 0.
   */
  static double randomDouble(int maxExponent) {
    double result = RANDOM_SOURCE.nextDouble();
    result = Math.scalb(result, RANDOM_SOURCE.nextInt(maxExponent + 1));
    return RANDOM_SOURCE.nextBoolean() ? result : -result;
  }

  /** Returns a random integer between zero and {@code MAX_EXPONENT}. */
  static int randomExponent() {
    return RANDOM_SOURCE.nextInt(MAX_EXPONENT + 1);
  }

  static double randomPositiveDouble() {
    return Math.exp(randomDouble(6));
  }
}
