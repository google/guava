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
import static com.google.common.math.MathBenchmarking.randomNonZeroBigInteger;
import static com.google.common.math.MathBenchmarking.randomPositiveBigInteger;

import com.google.caliper.BeforeExperiment;
import com.google.caliper.Benchmark;
import com.google.caliper.Param;
import java.math.BigInteger;
import java.math.RoundingMode;

/**
 * Benchmarks for the rounding methods of {@code BigIntegerMath}.
 *
 * @author Louis Wasserman
 */
public class BigIntegerMathRoundingBenchmark {
  private static final BigInteger[] nonzero1 = new BigInteger[ARRAY_SIZE];
  private static final BigInteger[] nonzero2 = new BigInteger[ARRAY_SIZE];
  private static final BigInteger[] positive = new BigInteger[ARRAY_SIZE];

  @Param({"DOWN", "UP", "FLOOR", "CEILING", "HALF_EVEN", "HALF_UP", "HALF_DOWN"})
  RoundingMode mode;

  @BeforeExperiment
  void setUp() {
    for (int i = 0; i < ARRAY_SIZE; i++) {
      positive[i] = randomPositiveBigInteger(1024);
      nonzero1[i] = randomNonZeroBigInteger(1024);
      nonzero2[i] = randomNonZeroBigInteger(1024);
    }
  }

  @Benchmark
  int log2(int reps) {
    int tmp = 0;
    for (int i = 0; i < reps; i++) {
      int j = i & ARRAY_MASK;
      tmp += BigIntegerMath.log2(positive[j], mode);
    }
    return tmp;
  }

  @Benchmark
  int log10(int reps) {
    int tmp = 0;
    for (int i = 0; i < reps; i++) {
      int j = i & ARRAY_MASK;
      tmp += BigIntegerMath.log10(positive[j], mode);
    }
    return tmp;
  }

  @Benchmark
  int sqrt(int reps) {
    int tmp = 0;
    for (int i = 0; i < reps; i++) {
      int j = i & ARRAY_MASK;
      tmp += BigIntegerMath.sqrt(positive[j], mode).intValue();
    }
    return tmp;
  }

  @Benchmark
  int divide(int reps) {
    int tmp = 0;
    for (int i = 0; i < reps; i++) {
      int j = i & ARRAY_MASK;
      tmp += BigIntegerMath.divide(nonzero1[j], nonzero2[j], mode).intValue();
    }
    return tmp;
  }

  @Benchmark
  long roundToDouble(int reps) {
    long tmp = 0;
    for (int i = 0; i < reps; i++) {
      int j = i & ARRAY_MASK;
      tmp += Double.doubleToRawLongBits(BigIntegerMath.roundToDouble(nonzero1[j], mode));
    }
    return tmp;
  }
}
