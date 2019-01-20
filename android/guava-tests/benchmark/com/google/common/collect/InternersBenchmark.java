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

package com.google.common.collect;

import com.google.caliper.Benchmark;

/**
 * Benchmarking interners.
 *
 * @author Dimitris Andreou
 */
public class InternersBenchmark {
  @Benchmark
  int weakInterner(int reps) {
    Interner<String> interner = Interners.newWeakInterner();
    for (int i = 0; i < reps; i++) {
      interner.intern(Double.toHexString(Math.random()));
    }
    return reps;
  }

  @Benchmark
  int strongInterner(int reps) {
    Interner<String> interner = Interners.newStrongInterner();
    for (int i = 0; i < reps; i++) {
      interner.intern(Double.toHexString(Math.random()));
    }
    return reps;
  }

  @Benchmark
  int stringIntern(int reps) {
    for (int i = 0; i < reps; i++) {
      String unused = Double.toHexString(Math.random()).intern();
    }
    return reps;
  }
}
