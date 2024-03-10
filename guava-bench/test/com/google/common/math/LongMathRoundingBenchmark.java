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
import static com.google.common.math.MathBenchmarking.randomNonZeroBigInteger;
import static com.google.common.math.MathBenchmarking.randomPositiveBigInteger;

import com.google.caliper.BeforeExperiment;
import com.google.caliper.Benchmark;
import com.google.caliper.Param;
import java.math.RoundingMode;

/**
 * Benchmarks for the rounding methods of {@code LongMath}.
 *
 * @author Louis Wasserman
 */
public class LongMathRoundingBenchmark {
  @Param({"DOWN", "UP", "FLOOR", "CEILING", "HALF_EVEN", "HALF_UP", "HALF_DOWN"})
  RoundingMode mode;

  private static final long[] positive = new long[ARRAY_SIZE];
  private static final long[] nonzero = new long[ARRAY_SIZE];
  private static final long[] longs = new long[ARRAY_SIZE];

  @BeforeExperiment
  void setUp() {
    for (int i = 0; i < ARRAY_SIZE; i++) {
      positive[i] = randomPositiveBigInteger(Long.SIZE - 2).longValue();
      nonzero[i] = randomNonZeroBigInteger(Long.SIZE - 2).longValue();
      longs[i] = RANDOM_SOURCE.nextLong();
    }
  }

  @Benchmark
  int log2(int reps) {
    int tmp = 0;
    for (int i = 0; i < reps; i++) {
      int j = i & ARRAY_MASK;
      tmp += LongMath.log2(positive[j], mode);
    }
    return tmp;
  }

  @Benchmark
  int log10(int reps) {
    int tmp = 0;
    for (int i = 0; i < reps; i++) {
      int j = i & ARRAY_MASK;
      tmp += LongMath.log10(positive[j], mode);
    }
    return tmp;
  }

  @Benchmark
  int sqrt(int reps) {
    int tmp = 0;
    for (int i = 0; i < reps; i++) {
      int j = i & ARRAY_MASK;
      tmp += LongMath.sqrt(positive[j], mode);
    }
    return tmp;
  }

  @Benchmark
  int divide(int reps) {
    int tmp = 0;
    for (int i = 0; i < reps; i++) {
      int j = i & ARRAY_MASK;
      tmp += LongMath.divide(longs[j], nonzero[j], mode);
    }
    return tmp;
  }
}
