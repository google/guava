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

package com.google.common.base;

import com.google.caliper.BeforeExperiment;
import com.google.caliper.Benchmark;
import com.google.caliper.Param;

import java.util.Random;

/**
 * Benchmark for the {@link Utf8} class.
 *
 *
 * @author Martin Buchholz
 */
public class Utf8Benchmark {

  static class MaxCodePoint {
    final int value;

    /**
     * Convert the input string to a code point.  Accepts regular
     * decimal numerals, hex strings, and some symbolic names
     * meaningful to humans.
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
   * The default values of maxCodePoint below provide pretty good
   * performance models of different kinds of common human text.
   * @see MaxCodePoint#decode
   */
  @Param({"0x80", "0x90", "0x800", "0x10000", "0x10ffff"}) MaxCodePoint maxCodePoint;

  @Param({"100"}) int byteArrayCount;
  @Param({"16384"}) int charCount;
  private byte[][] byteArrays;

  @BeforeExperiment void setUp() {
    final long seed = 99;
    final Random rnd = new Random(seed);
    byteArrays = new byte[byteArrayCount][];
    for (int i = 0; i < byteArrayCount; i++) {
      StringBuilder sb = new StringBuilder();
      for (int j = 0; j < charCount; j++) {
        int codePoint;
        do {
          codePoint = rnd.nextInt(maxCodePoint.value);
        } while (isSurrogate(codePoint));
        sb.appendCodePoint(codePoint);
      }
      byte[] bytes = sb.toString().getBytes(Charsets.UTF_8);
      byteArrays[i] = bytes;
    }
  }

  /**
   * Benchmarks {@link Utf8#isWellFormed} on valid byte arrays
   * containing pseudo-randomly-generated codePoints less than {@code
   * maxCodePoint}.  A constant seed is used, so separate runs perform
   * identical computations.
   */
  @Benchmark void isWellFormed(int reps) {
    for (int i = 0; i < reps; i++) {
      for (byte[] byteArray : byteArrays) {
        if (!Utf8.isWellFormed(byteArray)) {
          throw new Error("unexpected invalid UTF-8");
        }
      }
    }
  }

  /** Character.isSurrogate was added in Java SE 7. */
  private boolean isSurrogate(int c) {
    return (Character.MIN_HIGH_SURROGATE <= c &&
            c <= Character.MAX_LOW_SURROGATE);
  }
}
