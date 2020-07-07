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

import com.google.caliper.Benchmark;
import com.google.caliper.Param;

/**
 * Some microbenchmarks for the {@link MoreObjects.ToStringHelper} class.
 *
 * @author Osvaldo Doederlein
 */
public class ToStringHelperBenchmark {

  @Param({"0", "2", "5", "10"})
  int dataSize;

  private static final String NAME = "abcdefgh";
  private static final String NAME3 = Strings.repeat(NAME, 3);

  private static void addEntries(MoreObjects.ToStringHelper helper) {
    helper
        .add(NAME, 10)
        .addValue(10L)
        .add(NAME, 3.14f)
        .addValue(3.14d)
        .add(NAME3, false)
        .add(NAME3, NAME3)
        .add(NAME3, 'x');
  }

  @Benchmark
  int toString(int reps) {
    int dummy = 0;
    for (int i = 0; i < reps; i++) {
      MoreObjects.ToStringHelper helper = MoreObjects.toStringHelper("klass").omitNullValues();
      for (int j = 0; j < dataSize; ++j) {
        addEntries(helper);
      }
      dummy ^= helper.toString().hashCode();
    }
    return dummy;
  }
}
