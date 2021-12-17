/*
 * Copyright (C) 2015 The Guava Authors
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

import com.google.caliper.BeforeExperiment;
import com.google.caliper.Benchmark;
import com.google.caliper.Param;
import com.google.caliper.api.SkipThisScenarioException;
import java.util.Random;

/** A benchmark that tries invoking {@code Set.contains} on many different sets. */
public class MultipleSetContainsBenchmark {

  @Param({"0.0", "0.1", "0.7", "1.0"})
  double emptySetProportion;

  @Param({"0.0", "0.1", "0.7", "1.0"})
  double singletonSetProportion;

  @Param({"0.2", "0.8"})
  double hitRate;

  static final Object PRESENT = new Object();
  static final Object ABSENT = new Object();

  @SuppressWarnings("unchecked")
  private final ImmutableSet<Object>[] sets = new ImmutableSet[0x1000];

  private final Object[] queries = new Object[0x1000];

  @BeforeExperiment
  void setUp() {
    if (emptySetProportion + singletonSetProportion > 1.01) {
      throw new SkipThisScenarioException();
    }

    Random rng = new Random();
    for (int i = 0; i < 0x1000; i++) {
      double setSize = rng.nextDouble();
      if (setSize < emptySetProportion) {
        sets[i] = ImmutableSet.of();
      } else if (setSize < emptySetProportion + singletonSetProportion) {
        sets[i] = ImmutableSet.of(PRESENT);
      } else {
        sets[i] = ImmutableSet.of(PRESENT, new Object());
      }

      if (rng.nextDouble() < hitRate) {
        queries[i] = PRESENT;
      } else {
        queries[i] = ABSENT;
      }
    }
  }

  @Benchmark
  public boolean contains(int reps) {
    ImmutableSet<Object>[] sets = this.sets;
    Object[] queries = this.queries;
    boolean result = false;
    for (int i = 0; i < reps; i++) {
      int j = i & 0xFFF;
      result ^= sets[j].contains(queries[j]);
    }
    return result;
  }
}
