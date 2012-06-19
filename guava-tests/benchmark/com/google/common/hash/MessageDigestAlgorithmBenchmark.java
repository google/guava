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

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import java.util.Random;

/**
 * Benchmarks for comparing {@link MessageDigest}s and {@link HashFunction}s that wrap
 * {@link MessageDigest}s.
 *
 * <p>Parameters for the benchmark are:
 * <ul>
 * <li>size: The length of the byte array to hash.
 * </ul>
 *
 * @author Kurt Alfred Kluever
 */
public class MessageDigestAlgorithmBenchmark extends SimpleBenchmark {

  // Use a constant seed for all of the benchmarks to ensure apples to apples comparisons.
  private static final int RANDOM_SEED = new Random().nextInt();

  @Param({"10", "1000", "100000", "1000000"})
  private int size;

  private byte[] testBytes;

  @Override public void setUp() {
    testBytes = new byte[size];
    new Random(RANDOM_SEED).nextBytes(testBytes);
  }

  // MD5

  public byte timeMd5HashFunction(int reps) {
    return runHashFunction(reps, Hashing.md5());
  }

  public byte timeMd5MessageDigest(int reps) throws Exception {
    return runMessageDigest(reps, "MD5");
  }

  // SHA-1

  public byte timeSha1HashFunction(int reps) {
    return runHashFunction(reps, Hashing.sha1());
  }

  public byte timeSha1MessageDigest(int reps) throws Exception {
    return runMessageDigest(reps, "SHA-1");
  }

  // SHA-256

  public byte timeSha256HashFunction(int reps) {
    return runHashFunction(reps, Hashing.sha256());
  }

  public byte timeSha256MessageDigest(int reps) throws Exception {
    return runMessageDigest(reps, "SHA-256");
  }

  // SHA-512

  public byte timeSha512HashFunction(int reps) {
    return runHashFunction(reps, Hashing.sha512());
  }

  public byte timeSha512MessageDigest(int reps) throws Exception {
    return runMessageDigest(reps, "SHA-512");
  }

  // Helpers + main

  private byte runHashFunction(int reps, HashFunction hashFunction) {
    byte result = 0x01;
    // Trick the JVM to prevent it from using the hash function non-polymorphically
    result ^= Hashing.sha1().hashInt(reps).asBytes()[0];
    result ^= Hashing.md5().hashInt(reps).asBytes()[0];
    for (int i = 0; i < reps; i++) {
      result ^= hashFunction.hashBytes(testBytes).asBytes()[0];
    }
    return result;
  }

  private byte runMessageDigest(int reps, String algorithm) throws NoSuchAlgorithmException {
    byte result = 0x01;
    for (int i = 0; i < reps; i++) {
      MessageDigest md = MessageDigest.getInstance(algorithm);
      md.update(testBytes);
      result ^= md.digest()[0];
    }
    return result;
  }

  public static void main(String[] args) {
    Runner.main(MessageDigestAlgorithmBenchmark.class, args);
  }
}
