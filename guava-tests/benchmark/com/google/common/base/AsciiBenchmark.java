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

import com.google.caliper.Param;
import com.google.caliper.Runner;
import com.google.caliper.SimpleBenchmark;
import com.google.common.base.Ascii;
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
public class AsciiBenchmark extends SimpleBenchmark {
  private static String ALPHA =
      "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
  private static String NONALPHA =
      "0123456789`~-_=+[]{}|;:',.<>/?!@#$%^&*()\"\\";

  @Param({"20", "2000"}) int size;
  @Param({"2", "20"}) int nonAlphaRatio; // one non-alpha char per this many chars
  @Param boolean noWorkToDo;

  Random random;
  String testString;

  @Override protected void setUp() {
    random = new Random();

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

  public int timeAsciiToUpperCase(int reps) {
    String string = noWorkToDo
        ? Ascii.toUpperCase(testString)
        : testString;

    int dummy = 0;
    for (int i = 0; i < reps; i++) {
      dummy += Ascii.toUpperCase(string).length();
    }
    return dummy;
  }

  public int timeStringToUpperCase1(int reps) {
    String string = noWorkToDo
        ? testString.toUpperCase()
        : testString;

    int dummy = 0;
    for (int i = 0; i < reps; i++) {
      dummy += string.toUpperCase().length();
    }
    return dummy;
  }

  public int timeStringToUpperCase2(int reps) {
    String string = noWorkToDo
        ? testString.toUpperCase(Locale.US)
        : testString;

    int dummy = 0;
    for (int i = 0; i < reps; i++) {
      dummy += string.toUpperCase(Locale.US).length();
    }
    return dummy;
  }

  public static void main(String[] args) {
    Runner.main(AsciiBenchmark.class, args);
  }
}
