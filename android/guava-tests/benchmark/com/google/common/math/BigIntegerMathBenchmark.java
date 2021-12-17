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

import static com.google.common.math.MathBenchmarking.ARRAY_MASK;
import static com.google.common.math.MathBenchmarking.ARRAY_SIZE;
import static com.google.common.math.MathBenchmarking.RANDOM_SOURCE;
import static java.math.RoundingMode.CEILING;

import com.google.caliper.BeforeExperiment;
import com.google.caliper.Benchmark;
import com.google.caliper.Param;
import java.math.BigInteger;

/**
 * Benchmarks for the non-rounding methods of {@code BigIntegerMath}.
 *
 * @author Louis Wasserman
 */
public class BigIntegerMathBenchmark {
  private static final int[] factorials = new int[ARRAY_SIZE];
  private static final int[] slowFactorials = new int[ARRAY_SIZE];
  private static final int[] binomials = new int[ARRAY_SIZE];

  @Param({"50", "1000", "10000"})
  int factorialBound;

  @BeforeExperiment
  void setUp() {
    for (int i = 0; i < ARRAY_SIZE; i++) {
      factorials[i] = RANDOM_SOURCE.nextInt(factorialBound);
      slowFactorials[i] = RANDOM_SOURCE.nextInt(factorialBound);
      binomials[i] = RANDOM_SOURCE.nextInt(factorials[i] + 1);
    }
  }

  /** Previous version of BigIntegerMath.factorial, kept for timing purposes. */
  private static BigInteger oldSlowFactorial(int n) {
    if (n <= 20) {
      return BigInteger.valueOf(LongMath.factorial(n));
    } else {
      int k = 20;
      return BigInteger.valueOf(LongMath.factorial(k)).multiply(oldSlowFactorial(k, n));
    }
  }

  /** Returns the product of {@code n1} exclusive through {@code n2} inclusive. */
  private static BigInteger oldSlowFactorial(int n1, int n2) {
    assert n1 <= n2;
    if (IntMath.log2(n2, CEILING) * (n2 - n1) < Long.SIZE - 1) {
      // the result will definitely fit into a long
      long result = 1;
      for (int i = n1 + 1; i <= n2; i++) {
        result *= i;
      }
      return BigInteger.valueOf(result);
    }

    /*
     * We want each multiplication to have both sides with approximately the same number of digits.
     * Currently, we just divide the range in half.
     */
    int mid = (n1 + n2) >>> 1;
    return oldSlowFactorial(n1, mid).multiply(oldSlowFactorial(mid, n2));
  }

  @Benchmark
  int slowFactorial(int reps) {
    int tmp = 0;
    for (int i = 0; i < reps; i++) {
      int j = i & ARRAY_MASK;
      tmp += oldSlowFactorial(slowFactorials[j]).intValue();
    }
    return tmp;
  }

  @Benchmark
  int factorial(int reps) {
    int tmp = 0;
    for (int i = 0; i < reps; i++) {
      int j = i & ARRAY_MASK;
      tmp += BigIntegerMath.factorial(factorials[j]).intValue();
    }
    return tmp;
  }

  @Benchmark
  int binomial(int reps) {
    int tmp = 0;
    for (int i = 0; i < reps; i++) {
      int j = i & 0xffff;
      tmp += BigIntegerMath.binomial(factorials[j], binomials[j]).intValue();
    }
    return tmp;
  }
}
