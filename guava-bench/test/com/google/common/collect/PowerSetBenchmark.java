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

package com.google.common.collect;

import static com.google.common.collect.DiscreteDomain.integers;

import com.google.caliper.BeforeExperiment;
import com.google.caliper.Benchmark;
import com.google.caliper.Param;
import java.util.Set;

/**
 * Very simple powerSet iteration benchmark.
 *
 * @author Kevin Bourrillion
 */
public class PowerSetBenchmark {
  @Param({"2", "4", "8", "16"})
  int elements;

  Set<Set<Integer>> powerSet;

  @BeforeExperiment
  void setUp() {
    Set<Integer> set = ContiguousSet.create(Range.closed(1, elements), integers());
    powerSet = Sets.powerSet(set);
  }

  @Benchmark
  int iteration(int reps) {
    int sum = 0;
    for (int i = 0; i < reps; i++) {
      for (Set<Integer> subset : powerSet) {
        for (Integer value : subset) {
          sum += value;
        }
      }
    }
    return sum;
  }
}
