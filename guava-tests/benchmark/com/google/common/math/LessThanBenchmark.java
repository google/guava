/*
 * Copyright (C) 2013 The Guava Authors
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

import com.google.caliper.Param;
import com.google.caliper.legacy.Benchmark;
import com.google.caliper.runner.CaliperMain;

import java.util.Random;

/**
 * Benchmarks for various ways of writing the expression {@code foo + ((bar < baz) ? 1 : 0)}.
 *
 * @author Louis Wasserman
 */
public class LessThanBenchmark extends Benchmark {
  static final int SAMPLE_SIZE = 0x1000;
  static final int SAMPLE_MASK = 0x0FFF;

  @Param("1234")
  int randomSeed;

  int[] xInts;
  int[] yInts;

  long[] xLongs;
  long[] yLongs;

  int[] constant;
  
  private static final long NONNEGATIVE_LONG_MASK = 0x7FFFFFFFFFFFFFFFL;

  @Override
  protected void setUp() {
    Random random = new Random(randomSeed);
    xInts = new int[SAMPLE_SIZE];
    yInts = new int[SAMPLE_SIZE];
    xLongs = new long[SAMPLE_SIZE];
    yLongs = new long[SAMPLE_SIZE];
    constant = new int[SAMPLE_SIZE];
    for (int i = 0; i < SAMPLE_SIZE; i++) {
      xInts[i] = random.nextInt(Integer.MAX_VALUE);
      yInts[i] = random.nextInt(Integer.MAX_VALUE);
      xLongs[i] = random.nextLong() & NONNEGATIVE_LONG_MASK;
      yLongs[i] = random.nextLong() & NONNEGATIVE_LONG_MASK;
      constant[i] = random.nextInt();
    }
  }

  public int timeBranchFreeLtIntInlined(int reps) {
    int tmp = 0;
    for (int i = 0; i < reps; i++) {
      int j = i & SAMPLE_MASK;
      int x = xInts[j];
      int y = yInts[j];
      int z = constant[j];
      tmp += z + ((x - y) >>> (Integer.SIZE - 1));
    }
    return tmp;
  }

  public int timeBranchFreeLtInt(int reps) {
    int tmp = 0;
    for (int i = 0; i < reps; i++) {
      int j = i & SAMPLE_MASK;
      int x = xInts[j];
      int y = yInts[j];
      int z = constant[j];
      tmp += z + IntMath.lessThanBranchFree(x, y);
    }
    return tmp;
  }

  public int timeTernaryLtIntAddOutsideTernary(int reps) {
    int tmp = 0;
    for (int i = 0; i < reps; i++) {
      int j = i & SAMPLE_MASK;
      int x = xInts[j];
      int y = yInts[j];
      int z = constant[j];
      tmp += z + ((x < y) ? 1 : 0);
    }
    return tmp;
  }

  public int timeTernaryLtIntAddInsideTernary(int reps) {
    int tmp = 0;
    for (int i = 0; i < reps; i++) {
      int j = i & SAMPLE_MASK;
      int x = xInts[j];
      int y = yInts[j];
      int z = constant[j];
      tmp += (x < y) ? z + 1 : z;
    }
    return tmp;
  }

  public int timeBranchFreeLtLongInlined(int reps) {
    int tmp = 0;
    for (int i = 0; i < reps; i++) {
      int j = i & SAMPLE_MASK;
      long x = xLongs[j];
      long y = yLongs[j];
      int z = constant[j];
      tmp += z + (int) ((x - y) >>> (Long.SIZE - 1));
    }
    return tmp;
  }

  public int timeBranchFreeLtLong(int reps) {
    int tmp = 0;
    for (int i = 0; i < reps; i++) {
      int j = i & SAMPLE_MASK;
      long x = xLongs[j];
      long y = yLongs[j];
      int z = constant[j];
      tmp += z + LongMath.lessThanBranchFree(x, y);
    }
    return tmp;
  }

  public int timeTernaryLtLongAddOutsideTernary(int reps) {
    int tmp = 0;
    for (int i = 0; i < reps; i++) {
      int j = i & SAMPLE_MASK;
      long x = xLongs[j];
      long y = yLongs[j];
      int z = constant[j];
      tmp += z + ((x < y) ? 1 : 0);
    }
    return tmp;
  }

  public int timeTernaryLtLongAddInsideTernary(int reps) {
    int tmp = 0;
    for (int i = 0; i < reps; i++) {
      int j = i & SAMPLE_MASK;
      long x = xLongs[j];
      long y = yLongs[j];
      int z = constant[j];
      tmp += (x < y) ? z + 1 : z;
    }
    return tmp;
  }

  public static void main(String[] args) {
    CaliperMain.main(LessThanBenchmark.class, args);
  }
}
