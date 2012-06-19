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
import com.google.caliper.Runner;
import com.google.caliper.SimpleBenchmark;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;

import java.util.Random;

/**
 * Benchmarks for comparing the various {@link HashFunction functions} that we provide.
 *
 * <p>Parameters for the benchmark are:
 * <ul>
 * <li>size: The length of the byte array to hash.
 * </ul>
 *
 * @author Kurt Alfred Kluever
 */
public class HashFunctionBenchmark extends SimpleBenchmark {

  // Use a constant seed for all of the benchmarks to ensure apples to apples comparisons.
  private static final int RANDOM_SEED = new Random().nextInt();

  @Param({"10", "1000", "100000", "1000000"})
  private int size;

  private byte[] testBytes;

  @Override public void setUp() {
    testBytes = new byte[size];
    new Random(RANDOM_SEED).nextBytes(testBytes);
  }

  public int timeMurmur32HashFunction(int reps) {
    return runHashFunction(reps, Hashing.murmur3_32());
  }

  public int timeMurmur128HashFunction(int reps) {
    return runHashFunction(reps, Hashing.murmur3_128());
  }

  public int timeMd5HashFunction(int reps) {
    return runHashFunction(reps, Hashing.md5());
  }

  public int timeSha1HashFunction(int reps) {
    return runHashFunction(reps, Hashing.sha1());
  }

  public int timeSha256HashFunction(int reps) {
    return runHashFunction(reps, Hashing.sha256());
  }

  public int timeSha512HashFunction(int reps) {
    return runHashFunction(reps, Hashing.sha512());
  }

  private int runHashFunction(int reps, HashFunction hashFunction) {
    int result = 37;
    for (int i = 0; i < reps; i++) {
      result ^= hashFunction.hashBytes(testBytes).asInt();
    }
    return result;
  }

  public static void main(String[] args) {
    Runner.main(HashFunctionBenchmark.class, args);
  }
}
