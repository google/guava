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

package com.google.common.base;

import com.google.caliper.BeforeExperiment;
import com.google.caliper.Benchmark;
import com.google.caliper.Param;
import java.util.Arrays;
import java.util.Iterator;

/**
 * Benchmarks {@link Joiner} against some common implementations of delimiter-based string joining.
 *
 * @author Adomas Paltanavicius
 */
public class JoinerBenchmark {

  private static final String DELIMITER_STRING = ",";
  private static final char DELIMITER_CHARACTER = ',';

  private static final Joiner JOINER_ON_STRING = Joiner.on(DELIMITER_STRING);
  private static final Joiner JOINER_ON_CHARACTER = Joiner.on(DELIMITER_CHARACTER);

  @Param({"3", "30", "300"})
  int count;

  @Param({"0", "1", "16", "32", "100"})
  int componentLength;

  private Iterable<String> components;

  @BeforeExperiment
  void setUp() {
    String component = Strings.repeat("a", componentLength);
    String[] raw = new String[count];
    Arrays.fill(raw, component);
    components = Arrays.asList(raw);
  }

  /** {@link Joiner} with a string delimiter. */
  @Benchmark
  int joinerWithStringDelimiter(int reps) {
    int dummy = 0;
    for (int i = 0; i < reps; i++) {
      dummy ^= JOINER_ON_STRING.join(components).length();
    }
    return dummy;
  }

  /** {@link Joiner} with a character delimiter. */
  @Benchmark
  int joinerWithCharacterDelimiter(int reps) {
    int dummy = 0;
    for (int i = 0; i < reps; i++) {
      dummy ^= JOINER_ON_CHARACTER.join(components).length();
    }
    return dummy;
  }

  /**
   * Mimics what the {@link Joiner} class does internally when no extra options like ignoring {@code
   * null} values are used.
   */
  @Benchmark
  int joinerInlined(int reps) {
    int dummy = 0;
    for (int i = 0; i < reps; i++) {
      StringBuilder sb = new StringBuilder();
      Iterator<String> iterator = components.iterator();
      if (iterator.hasNext()) {
        sb.append(iterator.next().toString());
        while (iterator.hasNext()) {
          sb.append(DELIMITER_STRING);
          sb.append(iterator.next());
        }
      }
      dummy ^= sb.toString().length();
    }
    return dummy;
  }

  /**
   * Only appends delimiter if the accumulated string is non-empty. Note: this isn't a candidate
   * implementation for Joiner since it fails on leading empty components.
   */
  @Benchmark
  int stringBuilderIsEmpty(int reps) {
    int dummy = 0;
    for (int i = 0; i < reps; i++) {
      StringBuilder sb = new StringBuilder();
      for (String comp : components) {
        if (sb.length() > 0) {
          sb.append(DELIMITER_STRING);
        }
        sb.append(comp);
      }
      dummy ^= sb.toString().length();
    }
    return dummy;
  }

  /**
   * Similar to the above, but keeps a boolean flag rather than checking for the string accumulated
   * so far being empty. As a result, it does not have the above-mentioned bug.
   */
  @Benchmark
  int booleanIfFirst(int reps) {
    int dummy = 0;
    for (int i = 0; i < reps; i++) {
      StringBuilder sb = new StringBuilder();
      boolean append = false;
      for (String comp : components) {
        if (append) {
          sb.append(DELIMITER_STRING);
        }
        sb.append(comp);
        append = true;
      }
      dummy ^= sb.toString().length();
    }
    return dummy;
  }

  /**
   * Starts with an empty delimiter and changes to the desired value at the end of the iteration.
   */
  @Benchmark
  int assignDelimiter(int reps) {
    int dummy = 0;
    for (int i = 0; i < reps; i++) {
      StringBuilder sb = new StringBuilder();
      String delim = "";
      for (String comp : components) {
        sb.append(delim);
        sb.append(comp);
        delim = DELIMITER_STRING;
      }
      dummy ^= sb.toString().length();
    }
    return dummy;
  }

  /**
   * Always append the delimiter after the component, and in the very end shortens the buffer to get
   * rid of the extra trailing delimiter.
   */
  @Benchmark
  int alwaysAppendThenBackUp(int reps) {
    int dummy = 0;
    for (int i = 0; i < reps; i++) {
      StringBuilder sb = new StringBuilder();
      for (String comp : components) {
        sb.append(comp);
        sb.append(DELIMITER_STRING);
      }
      if (sb.length() > 0) {
        sb.setLength(sb.length() - DELIMITER_STRING.length());
      }
      dummy ^= sb.toString().length();
    }
    return dummy;
  }
}
