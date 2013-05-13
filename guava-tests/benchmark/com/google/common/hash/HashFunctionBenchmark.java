/*
 * Copyright (C) 2012 The Guava Authors
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

import com.google.caliper.Param;
import com.google.caliper.legacy.Benchmark;
import com.google.caliper.runner.CaliperMain;
import com.google.common.hash.HashFunction;

import java.util.Random;

/**
 * Benchmarks for comparing the various {@link HashFunction functions} that we provide.
 *
 * <p>Parameters for the benchmark are:
 * <ul>
 * <li>size: The length of the byte array to hash.
 * <li>hashFunctionEnum: The {@link HashFunction} to use for hashing.
 * </ul>
 *
 * @author Kurt Alfred Kluever
 */
public class HashFunctionBenchmark extends Benchmark {

  // Use a statically configured random instance for all of the benchmarks
  private static final Random random = new Random(42);

  @Param({"10", "1000", "100000", "1000000"})
  private int size;

  @Param HashFunctionEnum hashFunctionEnum;

  private byte[] testBytes;

  @Override public void setUp() {
    testBytes = new byte[size];
    random.nextBytes(testBytes);
  }

  public int timeHashFunction(int reps) {
    HashFunction hashFunction = hashFunctionEnum.getHashFunction();
    int result = 37;
    for (int i = 0; i < reps; i++) {
      result ^= hashFunction.hashBytes(testBytes).asBytes()[0];
    }
    return result;
  }

  public static void main(String[] args) {
    CaliperMain.main(HashFunctionBenchmark.class, args);
  }
}
