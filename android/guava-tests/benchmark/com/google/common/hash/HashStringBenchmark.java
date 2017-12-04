/*
 * Copyright (C) 2017 The Guava Authors
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
import java.nio.charset.StandardCharsets;
import java.util.Random;

/** Benchmarks for the hashing of UTF-8 strings. */
public class HashStringBenchmark {
  static class MaxCodePoint {
    final int value;

    /**
     * Convert the input string to a code point. Accepts regular decimal numerals, hex strings, and
     * some symbolic names meaningful to humans.
     */
    private static int decode(String userFriendly) {
      try {
        return Integer.decode(userFriendly);
      } catch (NumberFormatException ignored) {
        if (userFriendly.matches("(?i)(?:American|English|ASCII)")) {
          // 1-byte UTF-8 sequences - "American" ASCII text
          return 0x80;
        } else if (userFriendly.matches("(?i)(?:French|Latin|Western.*European)")) {
          // Mostly 1-byte UTF-8 sequences, mixed with occasional 2-byte
          // sequences - "Western European" text
          return 0x90;
        } else if (userFriendly.matches("(?i)(?:Branch.*Prediction.*Hostile)")) {
          // Defeat branch predictor for: c < 0x80 ; branch taken 50% of the time.
          return 0x100;
        } else if (userFriendly.matches("(?i)(?:Greek|Cyrillic|European|ISO.?8859)")) {
          // Mostly 2-byte UTF-8 sequences - "European" text
          return 0x800;
        } else if (userFriendly.matches("(?i)(?:Chinese|Han|Asian|BMP)")) {
          // Mostly 3-byte UTF-8 sequences - "Asian" text
          return Character.MIN_SUPPLEMENTARY_CODE_POINT;
        } else if (userFriendly.matches("(?i)(?:Cuneiform|rare|exotic|supplementary.*)")) {
          // Mostly 4-byte UTF-8 sequences - "rare exotic" text
          return Character.MAX_CODE_POINT;
        } else {
          throw new IllegalArgumentException("Can't decode codepoint " + userFriendly);
        }
      }
    }

    public static MaxCodePoint valueOf(String userFriendly) {
      return new MaxCodePoint(userFriendly);
    }

    public MaxCodePoint(String userFriendly) {
      value = decode(userFriendly);
    }
  }

  /**
   * The default values of maxCodePoint below provide pretty good performance models of different
   * kinds of common human text.
   *
   * @see MaxCodePoint#decode
   */
  @Param({"0x80", "0x90", "0x100", "0x800", "0x10000", "0x10ffff"})
  MaxCodePoint maxCodePoint;

  @Param({"16384"})
  int charCount;

  @Param({"MURMUR3_32", "MURMUR3_128", "SHA1"})
  HashFunctionEnum hashFunctionEnum;

  private String[] strings;

  static final int SAMPLES = 0x100;
  static final int SAMPLE_MASK = 0xFF;

  /**
   * Compute arrays of valid unicode text, and store it in 3 forms: byte arrays, Strings, and
   * StringBuilders (in a CharSequence[] to make it a little harder for the JVM).
   */
  @BeforeExperiment
  void setUp() {
    final long seed = 99;
    final Random rnd = new Random(seed);
    strings = new String[SAMPLES];
    for (int i = 0; i < SAMPLES; i++) {
      StringBuilder sb = new StringBuilder();
      for (int j = 0; j < charCount; j++) {
        int codePoint;
        // discard illegal surrogate "codepoints"
        do {
          codePoint = rnd.nextInt(maxCodePoint.value);
        } while (Character.isSurrogate((char) codePoint));
        sb.appendCodePoint(codePoint);
      }
      strings[i] = sb.toString();
    }
  }

  @Benchmark
  int hashUtf8(int reps) {
    int res = 0;
    for (int i = 0; i < reps; i++) {
      res +=
          System.identityHashCode(
              hashFunctionEnum
                  .getHashFunction()
                  .hashString(strings[i & SAMPLE_MASK], StandardCharsets.UTF_8));
    }
    return res;
  }

  @Benchmark
  int hashUtf8Hasher(int reps) {
    int res = 0;
    for (int i = 0; i < reps; i++) {
      res +=
          System.identityHashCode(
              hashFunctionEnum
                  .getHashFunction()
                  .newHasher()
                  .putString(strings[i & SAMPLE_MASK], StandardCharsets.UTF_8)
                  .hash());
    }
    return res;
  }

  @Benchmark
  int hashUtf8GetBytes(int reps) {
    int res = 0;
    for (int i = 0; i < reps; i++) {
      res +=
          System.identityHashCode(
              hashFunctionEnum
                  .getHashFunction()
                  .hashBytes(strings[i & SAMPLE_MASK].getBytes(StandardCharsets.UTF_8)));
    }
    return res;
  }

  @Benchmark
  int hashUtf8GetBytesHasher(int reps) {
    int res = 0;
    for (int i = 0; i < reps; i++) {
      res +=
          System.identityHashCode(
              hashFunctionEnum
                  .getHashFunction()
                  .newHasher()
                  .putBytes(strings[i & SAMPLE_MASK].getBytes(StandardCharsets.UTF_8))
                  .hash());
    }
    return res;
  }
}
