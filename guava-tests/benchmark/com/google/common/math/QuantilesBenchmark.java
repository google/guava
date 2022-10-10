/*
 * Copyright (C) 2014 The Guava Authors
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

package com.google.common.math;

import com.google.caliper.BeforeExperiment;
import com.google.caliper.Benchmark;
import com.google.caliper.Param;
import com.google.common.collect.ContiguousSet;
import com.google.common.collect.DiscreteDomain;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Range;
import java.util.Random;

/** Benchmarks some algorithms providing the same functionality as {@link Quantiles}. */
public class QuantilesBenchmark {

  private static final ContiguousSet<Integer> ALL_DECILE_INDEXES =
      ContiguousSet.create(Range.closed(0, 10), DiscreteDomain.integers());

  @Param({"10", "100", "1000", "10000", "100000"})
  int datasetSize;

  @Param QuantilesAlgorithm algorithm;

  private double[][] datasets = new double[0x100][];

  @BeforeExperiment
  void setUp() {
    Random rng = new Random();
    for (int i = 0; i < 0x100; i++) {
      datasets[i] = new double[datasetSize];
      for (int j = 0; j < datasetSize; j++) {
        datasets[i][j] = rng.nextDouble();
      }
    }
  }

  private double[] dataset(int i) {
    // We must test on a fresh clone of the dataset each time. Doing sorts and quickselects on a
    // dataset which is already sorted or partially sorted is cheating.
    return datasets[i & 0xFF].clone();
  }

  @Benchmark
  double median(int reps) {
    double dummy = 0.0;
    for (int i = 0; i < reps; i++) {
      dummy += algorithm.singleQuantile(1, 2, dataset(i));
    }
    return dummy;
  }

  @Benchmark
  double percentile90(int reps) {
    double dummy = 0.0;
    for (int i = 0; i < reps; i++) {
      dummy += algorithm.singleQuantile(90, 100, dataset(i));
    }
    return dummy;
  }

  @Benchmark
  double percentile99(int reps) {
    double dummy = 0.0;
    for (int i = 0; i < reps; i++) {
      dummy += algorithm.singleQuantile(99, 100, dataset(i));
    }
    return dummy;
  }

  @Benchmark
  double percentiles90And99(int reps) {
    double dummy = 0.0;
    for (int i = 0; i < reps; i++) {
      dummy += algorithm.multipleQuantiles(ImmutableSet.of(90, 99), 100, dataset(i)).get(90);
    }
    return dummy;
  }

  @Benchmark
  double threePercentiles(int reps) {
    double dummy = 0.0;
    for (int i = 0; i < reps; i++) {
      dummy += algorithm.multipleQuantiles(ImmutableSet.of(90, 95, 99), 100, dataset(i)).get(90);
    }
    return dummy;
  }

  @Benchmark
  double allDeciles(int reps) {
    double dummy = 0.0;
    for (int i = 0; i < reps; i++) {
      dummy += algorithm.multipleQuantiles(ALL_DECILE_INDEXES, 10, dataset(i)).get(9);
    }
    return dummy;
  }
}
