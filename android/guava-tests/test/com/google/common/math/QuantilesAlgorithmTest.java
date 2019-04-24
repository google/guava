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

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import junit.framework.TestCase;

/**
 * Tests that the different algorithms benchmarked in {@link QuantilesBenchmark} are actually all
 * returning more-or-less the same answers.
 */
public class QuantilesAlgorithmTest extends TestCase {

  private static final Random RNG = new Random(82674067L);
  private static final int DATASET_SIZE = 1000;
  private static final double ALLOWED_ERROR = 1.0e-10;
  private static final QuantilesAlgorithm REFERENCE_ALGORITHM = QuantilesAlgorithm.SORTING;
  private static final Set<QuantilesAlgorithm> NON_REFERENCE_ALGORITHMS =
      Sets.difference(
          ImmutableSet.copyOf(QuantilesAlgorithm.values()), ImmutableSet.of(REFERENCE_ALGORITHM));

  private double[] dataset;

  @Override
  protected void setUp() {
    dataset = new double[DATASET_SIZE];
    for (int i = 0; i < DATASET_SIZE; i++) {
      dataset[i] = RNG.nextDouble();
    }
  }

  public void testSingleQuantile_median() {
    double referenceValue = REFERENCE_ALGORITHM.singleQuantile(1, 2, dataset.clone());
    for (QuantilesAlgorithm algorithm : NON_REFERENCE_ALGORITHMS) {
      assertWithMessage("Mismatch between %s and %s", algorithm, REFERENCE_ALGORITHM)
          .that(algorithm.singleQuantile(1, 2, dataset.clone()))
          .isWithin(ALLOWED_ERROR)
          .of(referenceValue);
    }
  }

  public void testSingleQuantile_percentile99() {
    double referenceValue = REFERENCE_ALGORITHM.singleQuantile(99, 100, dataset.clone());
    for (QuantilesAlgorithm algorithm : NON_REFERENCE_ALGORITHMS) {
      assertWithMessage("Mismatch between %s and %s", algorithm, REFERENCE_ALGORITHM)
          .that(algorithm.singleQuantile(99, 100, dataset.clone()))
          .isWithin(ALLOWED_ERROR)
          .of(referenceValue);
    }
  }

  public void testMultipleQuantile() {
    ImmutableSet<Integer> indexes = ImmutableSet.of(50, 90, 99);
    Map<Integer, Double> referenceQuantiles =
        REFERENCE_ALGORITHM.multipleQuantiles(indexes, 100, dataset.clone());
    assertThat(referenceQuantiles.keySet()).isEqualTo(indexes);
    for (QuantilesAlgorithm algorithm : NON_REFERENCE_ALGORITHMS) {
      Map<Integer, Double> quantiles = algorithm.multipleQuantiles(indexes, 100, dataset.clone());
      assertWithMessage("Wrong keys from " + algorithm).that(quantiles.keySet()).isEqualTo(indexes);
      for (int i : indexes) {
        assertWithMessage("Mismatch between %s and %s at %s", algorithm, REFERENCE_ALGORITHM, i)
            .that(quantiles.get(i))
            .isWithin(ALLOWED_ERROR)
            .of(referenceQuantiles.get(i));
      }
    }
  }
}
