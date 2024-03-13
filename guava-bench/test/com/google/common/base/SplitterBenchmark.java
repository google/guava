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
import com.google.common.collect.Iterables;

/**
 * Microbenchmark for {@link Splitter#on} with char vs String with length == 1.
 *
 * @author Paul Lindner
 */
public class SplitterBenchmark {
  // overall size of string
  @Param({"1", "10", "100", "1000"})
  int length;
  // Number of matching strings
  @Param({"xxxx", "xxXx", "xXxX", "XXXX"})
  String text;

  private String input;

  private static final Splitter CHAR_SPLITTER = Splitter.on('X');
  private static final Splitter STRING_SPLITTER = Splitter.on("X");

  @BeforeExperiment
  void setUp() {
    input = Strings.repeat(text, length);
  }

  @Benchmark
  void charSplitter(int reps) {
    int total = 0;

    for (int i = 0; i < reps; i++) {
      total += Iterables.size(CHAR_SPLITTER.split(input));
    }
  }

  @Benchmark
  void stringSplitter(int reps) {
    int total = 0;

    for (int i = 0; i < reps; i++) {
      total += Iterables.size(STRING_SPLITTER.split(input));
    }
  }
}
