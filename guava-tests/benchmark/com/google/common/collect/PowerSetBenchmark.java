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

import static com.google.common.collect.DiscreteDomains.integers;

import com.google.caliper.Param;
import com.google.caliper.Runner;
import com.google.caliper.SimpleBenchmark;
import com.google.common.collect.Ranges;
import com.google.common.collect.Sets;

import java.util.Set;

/**
 * Very simple powerSet iteration benchmark.
 *
 * @author Kevin Bourrillion
 */
public class PowerSetBenchmark extends SimpleBenchmark {
  @Param({"2", "4", "8", "16"}) int elements;

  Set<Set<Integer>> powerSet;

  @Override protected void setUp() {
    Set<Integer> set = Ranges.closed(1, elements).asSet(integers());
    powerSet = Sets.powerSet(set);
  }

  public int timeIteration(int reps) {
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

  public static void main(String[] args) {
    Runner.main(PowerSetBenchmark.class, args);
  }
}
