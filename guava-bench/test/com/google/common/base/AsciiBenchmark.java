/*
 * Copyright (C) 2010 The Guava Authors
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
import com.google.common.collect.Lists;
import com.google.common.primitives.Chars;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Random;

/**
 * Benchmarks for the ASCII class.
 *
 * @author Kevin Bourrillion
 */
public class AsciiBenchmark {
  private static final String ALPHA = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
  private static final String NONALPHA = "0123456789`~-_=+[]{}|;:',.<>/?!@#$%^&*()\"\\";

  @Param({"20", "2000"})
  int size;

  @Param({"2", "20"})
  int nonAlphaRatio; // one non-alpha char per this many chars

  @Param boolean noWorkToDo;

  Random random;
  String testString;

  @BeforeExperiment
  void setUp() {
    random = new Random(0xdeadbeef); // fix the seed so results are comparable across runs

    int nonAlpha = size / nonAlphaRatio;
    int alpha = size - nonAlpha;

    List<Character> chars = Lists.newArrayListWithCapacity(size);
    for (int i = 0; i < alpha; i++) {
      chars.add(randomAlpha());
    }
    for (int i = 0; i < nonAlpha; i++) {
      chars.add(randomNonAlpha());
    }
    Collections.shuffle(chars, random);
    char[] array = Chars.toArray(chars);
    this.testString = new String(array);
  }

  private char randomAlpha() {
    return ALPHA.charAt(random.nextInt(ALPHA.length()));
  }

  private char randomNonAlpha() {
    return NONALPHA.charAt(random.nextInt(NONALPHA.length()));
  }

  @Benchmark
  int asciiStringToUpperCase(int reps) {
    String string = noWorkToDo ? Ascii.toUpperCase(testString) : testString;

    int dummy = 0;
    for (int i = 0; i < reps; i++) {
      dummy += Ascii.toUpperCase(string).length();
    }
    return dummy;
  }

  @Benchmark
  int asciiCharSequenceToUpperCase(int reps) {
    String string = noWorkToDo ? charSequenceToUpperCase(testString) : testString;

    int dummy = 0;
    for (int i = 0; i < reps; i++) {
      dummy += charSequenceToUpperCase(string).length();
    }
    return dummy;
  }

  @Benchmark
  int stringToUpperCase(int reps) {
    String string = noWorkToDo ? testString.toUpperCase(Locale.US) : testString;

    int dummy = 0;
    for (int i = 0; i < reps; i++) {
      dummy += string.toUpperCase(Locale.US).length();
    }
    return dummy;
  }

  @Benchmark
  boolean equalsIgnoreCaseCharSequence(int reps) {
    // This benchmark has no concept of "noWorkToDo".
    String upperString = testString.toUpperCase();
    CharSequence testSeq = new StringBuilder(testString);
    CharSequence upperSeq = new StringBuilder(upperString);
    CharSequence[] lhs = new CharSequence[] {testString, testSeq, testString, testSeq};
    CharSequence[] rhs = new CharSequence[] {upperString, upperString, upperSeq, upperSeq};

    boolean dummy = false;
    for (int i = 0; i < reps; i++) {
      dummy ^= Ascii.equalsIgnoreCase(lhs[i & 0x3], rhs[i & 0x3]);
    }
    return dummy;
  }

  @Benchmark
  boolean equalsIgnoreCaseStringOnly(int reps) {
    // This benchmark has no concept of "noWorkToDo".
    String lhs = testString;
    String rhs = testString.toUpperCase();

    boolean dummy = false;
    for (int i = 0; i < reps; i++) {
      dummy ^= Ascii.equalsIgnoreCase(lhs, rhs);
    }
    return dummy;
  }

  @Benchmark
  boolean equalsIgnoreCaseJDK(int reps) {
    // This benchmark has no concept of "noWorkToDo".
    String lhs = testString;
    String rhs = testString.toUpperCase();

    boolean dummy = false;
    for (int i = 0; i < reps; i++) {
      dummy ^= lhs.equalsIgnoreCase(rhs);
    }
    return dummy;
  }

  @Benchmark
  boolean isUpperCase(int reps) {
    // This benchmark has no concept of "noWorkToDo".
    char[] chars = testString.toCharArray();

    boolean dummy = false;
    for (int i = 0; i < reps; i++) {
      for (int n = 0; n < chars.length; n++) {
        dummy ^= Ascii.isUpperCase(chars[n]);
      }
    }
    return dummy;
  }

  static String charSequenceToUpperCase(CharSequence chars) {
    char[] newChars = new char[chars.length()];
    for (int i = 0; i < newChars.length; i++) {
      newChars[i] = Ascii.toUpperCase(chars.charAt(i));
    }
    return String.valueOf(newChars);
  }
}
