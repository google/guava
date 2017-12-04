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
import static com.google.common.math.MathBenchmarking.randomDouble;
import static com.google.common.math.MathBenchmarking.randomPositiveDouble;

import com.google.caliper.BeforeExperiment;
import com.google.caliper.Benchmark;
import com.google.caliper.Param;
import java.math.RoundingMode;

/**
 * Benchmarks for the rounding methods of {@code DoubleMath}.
 *
 * @author Louis Wasserman
 */
public class DoubleMathRoundingBenchmark {
  private static final double[] doubleInIntRange = new double[ARRAY_SIZE];
  private static final double[] doubleInLongRange = new double[ARRAY_SIZE];
  private static final double[] positiveDoubles = new double[ARRAY_SIZE];

  @Param({"DOWN", "UP", "FLOOR", "CEILING", "HALF_EVEN", "HALF_UP", "HALF_DOWN"})
  RoundingMode mode;

  @BeforeExperiment
  void setUp() {
    for (int i = 0; i < ARRAY_SIZE; i++) {
      doubleInIntRange[i] = randomDouble(Integer.SIZE - 2);
      doubleInLongRange[i] = randomDouble(Long.SIZE - 2);
      positiveDoubles[i] = randomPositiveDouble();
    }
  }

  @Benchmark
  int roundToInt(int reps) {
    int tmp = 0;
    for (int i = 0; i < reps; i++) {
      int j = i & ARRAY_MASK;
      tmp += DoubleMath.roundToInt(doubleInIntRange[j], mode);
    }
    return tmp;
  }

  @Benchmark
  long roundToLong(int reps) {
    long tmp = 0;
    for (int i = 0; i < reps; i++) {
      int j = i & ARRAY_MASK;
      tmp += DoubleMath.roundToLong(doubleInLongRange[j], mode);
    }
    return tmp;
  }

  @Benchmark
  int roundToBigInteger(int reps) {
    int tmp = 0;
    for (int i = 0; i < reps; i++) {
      int j = i & ARRAY_MASK;
      tmp += DoubleMath.roundToBigInteger(positiveDoubles[j], mode).intValue();
    }
    return tmp;
  }

  @Benchmark
  int log2Round(int reps) {
    int tmp = 0;
    for (int i = 0; i < reps; i++) {
      int j = i & ARRAY_MASK;
      tmp += DoubleMath.log2(positiveDoubles[j], mode);
    }
    return tmp;
  }
}
