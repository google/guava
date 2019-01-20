/*
 * Copyright (C) 2015 The Guava Authors
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

package com.google.common.hash;

import com.google.caliper.BeforeExperiment;
import com.google.caliper.Benchmark;
import com.google.caliper.Param;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Random;

/**
 * Benchmarks for comparing the various {@link HashCode#equals} methods.
 *
 * <p>Parameters for the benchmark are:
 *
 * <ul>
 *   <li>size: the length of the byte array to hash
 *   <li>whereToDiffer: where in the array the bytes should differ
 *   <li>equalsImpl: which implementation of array equality to use
 * </ul>
 *
 * <p><b>Important note:</b> the primary goal of this benchmark is to ensure that varying {@code
 * whereToDiffer} produces no observable change in performance. We want to make sure that the array
 * equals implementation is *not* short-circuiting to prevent timing-based attacks. Being fast is
 * only a secondary goal.
 *
 * @author Kurt Alfred Kluever
 */
public class HashCodeBenchmark {

  // Use a statically configured random instance for all of the benchmarks
  private static final Random random = new Random(42);

  @Param({"1000", "100000"})
  private int size;

  @Param WhereToDiffer whereToDiffer;

  @Param EqualsImplementation equalsImpl;

  private enum WhereToDiffer {
    ONE_PERCENT_IN,
    LAST_BYTE,
    NOT_AT_ALL;
  }

  private enum EqualsImplementation {
    ANDING_BOOLEANS {
      @Override
      boolean doEquals(byte[] a, byte[] b) {
        if (a.length != b.length) {
          return false;
        }
        boolean areEqual = true;
        for (int i = 0; i < a.length; i++) {
          areEqual &= (a[i] == b[i]);
        }
        return areEqual;
      }
    },
    XORING_TO_BYTE {
      @Override
      boolean doEquals(byte[] a, byte[] b) {
        if (a.length != b.length) {
          return false;
        }
        byte result = 0;
        for (int i = 0; i < a.length; i++) {
          result = (byte) (result | a[i] ^ b[i]);
        }
        return (result == 0);
      }
    },
    XORING_TO_INT {
      @Override
      boolean doEquals(byte[] a, byte[] b) {
        if (a.length != b.length) {
          return false;
        }
        int result = 0;
        for (int i = 0; i < a.length; i++) {
          result |= a[i] ^ b[i];
        }
        return (result == 0);
      }
    },
    MESSAGE_DIGEST_IS_EQUAL {
      @Override
      boolean doEquals(byte[] a, byte[] b) {
        return MessageDigest.isEqual(a, b);
      }
    },
    ARRAYS_EQUALS {
      @Override
      boolean doEquals(byte[] a, byte[] b) {
        return Arrays.equals(a, b);
      }
    };

    abstract boolean doEquals(byte[] a, byte[] b);
  }

  private byte[] testBytesA;
  private byte[] testBytesB;

  @BeforeExperiment
  void setUp() {
    testBytesA = new byte[size];
    random.nextBytes(testBytesA);
    testBytesB = Arrays.copyOf(testBytesA, size);
    int indexToDifferAt = -1;
    switch (whereToDiffer) {
      case ONE_PERCENT_IN:
        indexToDifferAt = (int) (size * 0.01);
        break;
      case LAST_BYTE:
        indexToDifferAt = size - 1;
        break;
      case NOT_AT_ALL:
    }
    if (indexToDifferAt != -1) {
      testBytesA[indexToDifferAt] = (byte) (testBytesB[indexToDifferAt] - 1);
    }
  }

  @Benchmark
  boolean hashFunction(int reps) {
    boolean result = true;
    for (int i = 0; i < reps; i++) {
      result ^= equalsImpl.doEquals(testBytesA, testBytesB);
    }
    return result;
  }
}
