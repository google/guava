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

package com.google.common.primitives;

import com.google.caliper.BeforeExperiment;
import com.google.caliper.Benchmark;
import java.util.Random;

/**
 * Benchmarks for certain methods of {@code UnsignedLongs}.
 *
 * @author Eamonn McManus
 */
public class UnsignedLongsBenchmark {
  private static final int ARRAY_SIZE = 0x10000;
  private static final int ARRAY_MASK = 0x0ffff;
  private static final Random RANDOM_SOURCE = new Random(314159265358979L);
  private static final long[] longs = new long[ARRAY_SIZE];
  private static final long[] divisors = new long[ARRAY_SIZE];
  private static final String[] decimalStrings = new String[ARRAY_SIZE];
  private static final String[] binaryStrings = new String[ARRAY_SIZE];
  private static final String[] hexStrings = new String[ARRAY_SIZE];
  private static final String[] prefixedHexStrings = new String[ARRAY_SIZE];

  @BeforeExperiment
  void setUp() {
    for (int i = 0; i < ARRAY_SIZE; i++) {
      longs[i] = random();
      divisors[i] = randomDivisor(longs[i]);
      decimalStrings[i] = UnsignedLongs.toString(longs[i]);
      binaryStrings[i] = UnsignedLongs.toString(longs[i], 2);
      hexStrings[i] = UnsignedLongs.toString(longs[i], 16);
      prefixedHexStrings[i] = "0x" + hexStrings[i];
    }
  }

  @Benchmark
  long divide(int reps) {
    long tmp = 0;
    for (int i = 0; i < reps; i++) {
      int j = i & ARRAY_MASK;
      tmp += UnsignedLongs.divide(longs[j], divisors[j]);
    }
    return tmp;
  }

  @Benchmark
  long remainder(int reps) {
    long tmp = 0;
    for (int i = 0; i < reps; i++) {
      int j = i & ARRAY_MASK;
      tmp += UnsignedLongs.remainder(longs[j], divisors[j]);
    }
    return tmp;
  }

  @Benchmark
  long parseUnsignedLong(int reps) {
    long tmp = 0;
    // Given that we make three calls per pass, we scale reps down in order
    // to do a comparable amount of work to other measurements.
    int scaledReps = reps / 3 + 1;
    for (int i = 0; i < scaledReps; i++) {
      int j = i & ARRAY_MASK;
      tmp += UnsignedLongs.parseUnsignedLong(decimalStrings[j]);
      tmp += UnsignedLongs.parseUnsignedLong(hexStrings[j], 16);
      tmp += UnsignedLongs.parseUnsignedLong(binaryStrings[j], 2);
    }
    return tmp;
  }

  @Benchmark
  long parseDecode10(int reps) {
    long tmp = 0;
    for (int i = 0; i < reps; i++) {
      int j = i & ARRAY_MASK;
      tmp += UnsignedLongs.decode(decimalStrings[j]);
    }
    return tmp;
  }

  @Benchmark
  long parseDecode16(int reps) {
    long tmp = 0;
    for (int i = 0; i < reps; i++) {
      int j = i & ARRAY_MASK;
      tmp += UnsignedLongs.decode(prefixedHexStrings[j]);
    }
    return tmp;
  }

  @Benchmark
  int toString(int reps) {
    int tmp = 0;
    // Given that we make three calls per pass, we scale reps down in order
    // to do a comparable amount of work to other measurements.
    int scaledReps = reps / 3 + 1;
    for (int i = 0; i < scaledReps; i++) {
      int j = i & ARRAY_MASK;
      long x = longs[j];
      tmp += UnsignedLongs.toString(x).length();
      tmp += UnsignedLongs.toString(x, 16).length();
      tmp += UnsignedLongs.toString(x, 2).length();
    }
    return tmp;
  }

  private static long random() {
    return RANDOM_SOURCE.nextLong();
  }

  // A random value that cannot be 0 and that is unsigned-less-than or equal
  // to the given dividend, so that we don't have half of our divisions being
  // trivial because the divisor is bigger than the dividend.
  // Using remainder here does not give us a uniform distribution but it should
  // not have a big impact on the measurement.
  private static long randomDivisor(long dividend) {
    long r = RANDOM_SOURCE.nextLong();
    if (dividend == -1) {
      return r;
    } else {
      return UnsignedLongs.remainder(r, dividend + 1);
    }
  }
}
