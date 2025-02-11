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
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import org.jspecify.annotations.NullUnmarked;

/**
 * Benchmarking interners.
 *
 * @author Dimitris Andreou
 */
@NullUnmarked
public class InternersBenchmark {
  @CanIgnoreReturnValue
  @Benchmark
  int weakInterner(int reps) {
    Interner<String> interner = Interners.newWeakInterner();
    for (int i = 0; i < reps; i++) {
      String unused = interner.intern(Double.toHexString(Math.random()));
    }
    return reps;
  }

  @CanIgnoreReturnValue
  @Benchmark
  int strongInterner(int reps) {
    Interner<String> interner = Interners.newStrongInterner();
    for (int i = 0; i < reps; i++) {
      String unused = interner.intern(Double.toHexString(Math.random()));
    }
    return reps;
  }

  @CanIgnoreReturnValue
  @Benchmark
  int stringIntern(int reps) {
    for (int i = 0; i < reps; i++) {
      String unused = Double.toHexString(Math.random()).intern();
    }
    return reps;
  }
}
