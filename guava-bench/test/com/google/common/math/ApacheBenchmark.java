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
import static com.google.common.math.MathBenchmarking.randomBigInteger;
import static com.google.common.math.MathBenchmarking.randomNonNegativeBigInteger;

import com.google.caliper.BeforeExperiment;
import com.google.caliper.Benchmark;
import com.google.caliper.Param;

/**
 * Benchmarks against the Apache Commons Math utilities.
 *
 * <p>Note: the Apache benchmarks are not open sourced to avoid the extra dependency.
 *
 * @author Louis Wasserman
 */
public class ApacheBenchmark {
  private enum Impl {
    GUAVA {
      @Override
      public double factorialDouble(int n) {
        return DoubleMath.factorial(n);
      }

      @Override
      public int gcdInt(int a, int b) {
        return IntMath.gcd(a, b);
      }

      @Override
      public long gcdLong(long a, long b) {
        return LongMath.gcd(a, b);
      }

      @Override
      public long binomialCoefficient(int n, int k) {
        return LongMath.binomial(n, k);
      }

      @Override
      public boolean noAddOverflow(int a, int b) {
        try {
          int unused = IntMath.checkedAdd(a, b);
          return true;
        } catch (ArithmeticException e) {
          return false;
        }
      }

      @Override
      public boolean noAddOverflow(long a, long b) {
        try {
          long unused = LongMath.checkedAdd(a, b);
          return true;
        } catch (ArithmeticException e) {
          return false;
        }
      }

      @Override
      public boolean noMulOverflow(int a, int b) {
        try {
          int unused = IntMath.checkedMultiply(a, b);
          return true;
        } catch (ArithmeticException e) {
          return false;
        }
      }

      @Override
      public boolean noMulOverflow(long a, long b) {
        try {
          long unused = LongMath.checkedMultiply(a, b);
          return true;
        } catch (ArithmeticException e) {
          return false;
        }
      }
    };

    public abstract double factorialDouble(int n);

    public abstract long binomialCoefficient(int n, int k);

    public abstract int gcdInt(int a, int b);

    public abstract long gcdLong(long a, long b);

    public abstract boolean noAddOverflow(int a, int b);

    public abstract boolean noAddOverflow(long a, long b);

    public abstract boolean noMulOverflow(int a, int b);

    public abstract boolean noMulOverflow(long a, long b);
  }

  private final int[] factorials = new int[ARRAY_SIZE];
  private final int[][] binomials = new int[ARRAY_SIZE][2];
  private final int[][] nonnegInt = new int[ARRAY_SIZE][2];
  private final long[][] nonnegLong = new long[ARRAY_SIZE][2];
  private final int[][] intsToAdd = new int[ARRAY_SIZE][2];
  private final int[][] intsToMul = new int[ARRAY_SIZE][2];
  private final long[][] longsToAdd = new long[ARRAY_SIZE][2];
  private final long[][] longsToMul = new long[ARRAY_SIZE][2];

  @Param({"APACHE", "GUAVA"})
  Impl impl;

  @BeforeExperiment
  void setUp() {
    for (int i = 0; i < ARRAY_SIZE; i++) {
      factorials[i] = RANDOM_SOURCE.nextInt(200);
      for (int j = 0; j < 2; j++) {
        nonnegInt[i][j] = randomNonNegativeBigInteger(Integer.SIZE - 2).intValue();
        nonnegLong[i][j] = randomNonNegativeBigInteger(Long.SIZE - 2).longValue();
      }
      do {
        for (int j = 0; j < 2; j++) {
          intsToAdd[i][j] = randomBigInteger(Integer.SIZE - 2).intValue();
        }
      } while (!Impl.GUAVA.noAddOverflow(intsToAdd[i][0], intsToAdd[i][1]));
      do {
        for (int j = 0; j < 2; j++) {
          longsToAdd[i][j] = randomBigInteger(Long.SIZE - 2).longValue();
        }
      } while (!Impl.GUAVA.noAddOverflow(longsToAdd[i][0], longsToAdd[i][1]));
      do {
        for (int j = 0; j < 2; j++) {
          intsToMul[i][j] = randomBigInteger(Integer.SIZE - 2).intValue();
        }
      } while (!Impl.GUAVA.noMulOverflow(intsToMul[i][0], intsToMul[i][1]));
      do {
        for (int j = 0; j < 2; j++) {
          longsToMul[i][j] = randomBigInteger(Long.SIZE - 2).longValue();
        }
      } while (!Impl.GUAVA.noMulOverflow(longsToMul[i][0], longsToMul[i][1]));

      int k = binomials[i][1] = RANDOM_SOURCE.nextInt(MathBenchmarking.biggestBinomials.length);
      binomials[i][0] = RANDOM_SOURCE.nextInt(MathBenchmarking.biggestBinomials[k] - k) + k;
    }
  }

  @Benchmark
  long factorialDouble(int reps) {
    long tmp = 0;
    for (int i = 0; i < reps; i++) {
      int j = i & ARRAY_MASK;
      tmp += Double.doubleToRawLongBits(impl.factorialDouble(factorials[j]));
    }
    return tmp;
  }

  @Benchmark
  int intGCD(int reps) {
    int tmp = 0;
    for (int i = 0; i < reps; i++) {
      int j = i & ARRAY_MASK;
      tmp += impl.gcdInt(nonnegInt[j][0], nonnegInt[j][1]);
    }
    return tmp;
  }

  @Benchmark
  long longGCD(int reps) {
    long tmp = 0;
    for (int i = 0; i < reps; i++) {
      int j = i & ARRAY_MASK;
      tmp += impl.gcdLong(nonnegLong[j][0], nonnegLong[j][1]);
    }
    return tmp;
  }

  @Benchmark
  long binomialCoefficient(int reps) {
    long tmp = 0;
    for (int i = 0; i < reps; i++) {
      int j = i & ARRAY_MASK;
      tmp += impl.binomialCoefficient(binomials[j][0], binomials[j][1]);
    }
    return tmp;
  }

  @Benchmark
  int intAddOverflow(int reps) {
    int tmp = 0;
    for (int i = 0; i < reps; i++) {
      int j = i & ARRAY_MASK;
      if (impl.noAddOverflow(intsToAdd[j][0], intsToAdd[j][1])) {
        tmp++;
      }
    }
    return tmp;
  }

  @Benchmark
  int longAddOverflow(int reps) {
    int tmp = 0;
    for (int i = 0; i < reps; i++) {
      int j = i & ARRAY_MASK;
      if (impl.noAddOverflow(longsToAdd[j][0], longsToAdd[j][1])) {
        tmp++;
      }
    }
    return tmp;
  }

  @Benchmark
  int intMulOverflow(int reps) {
    int tmp = 0;
    for (int i = 0; i < reps; i++) {
      int j = i & ARRAY_MASK;
      if (impl.noMulOverflow(intsToMul[j][0], intsToMul[j][1])) {
        tmp++;
      }
    }
    return tmp;
  }

  @Benchmark
  int longMulOverflow(int reps) {
    int tmp = 0;
    for (int i = 0; i < reps; i++) {
      int j = i & ARRAY_MASK;
      if (impl.noMulOverflow(longsToMul[j][0], longsToMul[j][1])) {
        tmp++;
      }
    }
    return tmp;
  }
}
