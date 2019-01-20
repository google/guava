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

import com.google.caliper.BeforeExperiment;
import com.google.caliper.Benchmark;
import com.google.caliper.Param;
import java.util.Random;

/**
 * Benchmarks for comparing the various {@link HashFunction functions} that we provide.
 *
 * <p>Parameters for the benchmark are:
 *
 * <ul>
 *   <li>size: The length of the byte array to hash.
 *   <li>hashFunctionEnum: The {@link HashFunction} to use for hashing.
 * </ul>
 *
 * @author Kurt Alfred Kluever
 */
public class HashFunctionBenchmark {

  // Use a statically configured random instance for all of the benchmarks
  private static final Random random = new Random(42);

  @Param({"10", "1000", "100000", "1000000"})
  private int size;

  @Param HashFunctionEnum hashFunctionEnum;

  private byte[] testBytes;

  @BeforeExperiment
  void setUp() {
    testBytes = new byte[size];
    random.nextBytes(testBytes);
  }

  @Benchmark
  int hasher(int reps) {
    HashFunction hashFunction = hashFunctionEnum.getHashFunction();
    int result = 37;
    for (int i = 0; i < reps; i++) {
      result ^= hashFunction.newHasher().putBytes(testBytes).hash().asBytes()[0];
    }
    return result;
  }

  @Benchmark
  int hashFunction(int reps) {
    HashFunction hashFunction = hashFunctionEnum.getHashFunction();
    int result = 37;
    for (int i = 0; i < reps; i++) {
      result ^= hashFunction.hashBytes(testBytes).asBytes()[0];
    }
    return result;
  }

  @Benchmark
  int hashFunctionWithOffset(int reps) {
    HashFunction hashFunction = hashFunctionEnum.getHashFunction();
    int result = 37;
    for (int i = 0; i < reps; i++) {
      result ^= hashFunction.hashBytes(testBytes, 1, testBytes.length - 1).asBytes()[0];
    }
    return result;
  }
}
