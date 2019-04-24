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
import java.util.zip.Adler32;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

/**
 * Benchmarks for comparing {@link Checksum}s and {@link HashFunction}s that wrap {@link Checksum}s.
 *
 * <p>Parameters for the benchmark are:
 *
 * <ul>
 *   <li>size: The length of the byte array to hash.
 * </ul>
 *
 * @author Colin Decker
 */
public class ChecksumBenchmark {

  // Use a constant seed for all of the benchmarks to ensure apples to apples comparisons.
  private static final int RANDOM_SEED = new Random().nextInt();

  @Param({"10", "1000", "100000", "1000000"})
  private int size;

  private byte[] testBytes;

  @BeforeExperiment
  void setUp() {
    testBytes = new byte[size];
    new Random(RANDOM_SEED).nextBytes(testBytes);
  }

  // CRC32

  @Benchmark
  byte crc32HashFunction(int reps) {
    return runHashFunction(reps, Hashing.crc32());
  }

  @Benchmark
  byte crc32Checksum(int reps) throws Exception {
    byte result = 0x01;
    for (int i = 0; i < reps; i++) {
      CRC32 checksum = new CRC32();
      checksum.update(testBytes);
      result = (byte) (result ^ checksum.getValue());
    }
    return result;
  }

  // Adler32

  @Benchmark
  byte adler32HashFunction(int reps) {
    return runHashFunction(reps, Hashing.adler32());
  }

  @Benchmark
  byte adler32Checksum(int reps) throws Exception {
    byte result = 0x01;
    for (int i = 0; i < reps; i++) {
      Adler32 checksum = new Adler32();
      checksum.update(testBytes);
      result = (byte) (result ^ checksum.getValue());
    }
    return result;
  }

  // Helpers + main

  private byte runHashFunction(int reps, HashFunction hashFunction) {
    byte result = 0x01;
    // Trick the JVM to prevent it from using the hash function non-polymorphically
    result ^= Hashing.crc32().hashInt(reps).asBytes()[0];
    result ^= Hashing.adler32().hashInt(reps).asBytes()[0];
    for (int i = 0; i < reps; i++) {
      result ^= hashFunction.hashBytes(testBytes).asBytes()[0];
    }
    return result;
  }
}
