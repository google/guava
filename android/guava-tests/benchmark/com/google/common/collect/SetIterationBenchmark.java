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

package com.google.common.collect;

import com.google.caliper.BeforeExperiment;
import com.google.caliper.Benchmark;
import com.google.caliper.Param;
import com.google.common.collect.BenchmarkHelpers.SetImpl;
import com.google.common.collect.CollectionBenchmarkSampleData.Element;
import java.util.Set;

/**
 * Test iteration speed at various size for {@link Set} instances.
 *
 * @author Christopher Swenson
 */
public class SetIterationBenchmark {
  @Param({
    "3", "6", "11", "23", "45", "91", "181", "362", "724", "1448", "2896", "5793", "11585", "23170",
    "46341", "92682", "185364", "370728", "741455", "1482910", "2965821", "5931642"
  })
  private int size;

  // "" means no fixed seed
  @Param("1234")
  private SpecialRandom random;

  @Param({"ImmutableSetImpl", "HashSetImpl"})
  private SetImpl impl;

  // the following must be set during setUp
  private Set<Element> setToTest;

  @BeforeExperiment
  void setUp() {
    CollectionBenchmarkSampleData sampleData =
        new CollectionBenchmarkSampleData(true, random, 0.8, size);
    setToTest = (Set<Element>) impl.create(sampleData.getValuesInSet());
  }

  @Benchmark
  int iteration(int reps) {
    int x = 0;

    for (int i = 0; i < reps; i++) {
      for (Element y : setToTest) {
        x ^= System.identityHashCode(y);
      }
    }
    return x;
  }
}
