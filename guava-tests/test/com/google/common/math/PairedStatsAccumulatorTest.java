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

package com.google.common.math;

import static com.google.common.math.StatsTesting.ALLOWED_ERROR;
import static com.google.common.math.StatsTesting.ALL_MANY_VALUES;
import static com.google.common.math.StatsTesting.EMPTY_STATS_ITERABLE;
import static com.google.common.math.StatsTesting.MANY_VALUES;
import static com.google.common.math.StatsTesting.MANY_VALUES_COUNT;
import static com.google.common.math.StatsTesting.MANY_VALUES_STATS_ITERABLE;
import static com.google.common.math.StatsTesting.MANY_VALUES_SUM_OF_PRODUCTS_OF_DELTAS;
import static com.google.common.math.StatsTesting.ONE_VALUE;
import static com.google.common.math.StatsTesting.ONE_VALUE_STATS;
import static com.google.common.math.StatsTesting.OTHER_MANY_VALUES;
import static com.google.common.math.StatsTesting.OTHER_MANY_VALUES_STATS;
import static com.google.common.math.StatsTesting.OTHER_ONE_VALUE;
import static com.google.common.math.StatsTesting.OTHER_ONE_VALUE_STATS;
import static com.google.common.math.StatsTesting.OTHER_TWO_VALUES;
import static com.google.common.math.StatsTesting.OTHER_TWO_VALUES_STATS;
import static com.google.common.math.StatsTesting.TWO_VALUES;
import static com.google.common.math.StatsTesting.TWO_VALUES_STATS;
import static com.google.common.math.StatsTesting.TWO_VALUES_SUM_OF_PRODUCTS_OF_DELTAS;
import static com.google.common.math.StatsTesting.assertDiagonalLinearTransformation;
import static com.google.common.math.StatsTesting.assertHorizontalLinearTransformation;
import static com.google.common.math.StatsTesting.assertLinearTransformationNaN;
import static com.google.common.math.StatsTesting.assertStatsApproxEqual;
import static com.google.common.math.StatsTesting.assertVerticalLinearTransformation;
import static com.google.common.math.StatsTesting.createFilledPairedStatsAccumulator;
import static com.google.common.truth.Truth.assertThat;

import com.google.common.math.StatsTesting.ManyValues;

import junit.framework.TestCase;

/**
 * Tests for {@link PairedStatsAccumulator}. This tests the stats methods for instances built with
 * {@link PairedStatsAccumulator#add}, and various error cases of that method. For tests of the
 * {@link PairedStatsAccumulator#snapshot} method which returns {@link PairedStats} instances, see
 * {@link PairedStatsTest}.
 *
 * @author Pete Gillin
 */
public class PairedStatsAccumulatorTest extends TestCase {

  private PairedStatsAccumulator emptyAccumulator;
  private PairedStatsAccumulator oneValueAccumulator;
  private PairedStatsAccumulator twoValuesAccumulator;
  private PairedStatsAccumulator manyValuesAccumulator;
  private PairedStatsAccumulator horizontalValuesAccumulator;
  private PairedStatsAccumulator verticalValuesAccumulator;
  private PairedStatsAccumulator constantValuesAccumulator;

  @Override
  protected void setUp() throws Exception {
    super.setUp();

    emptyAccumulator = new PairedStatsAccumulator();

    oneValueAccumulator = new PairedStatsAccumulator();
    oneValueAccumulator.add(ONE_VALUE, OTHER_ONE_VALUE);

    twoValuesAccumulator = createFilledPairedStatsAccumulator(TWO_VALUES, OTHER_TWO_VALUES);

    manyValuesAccumulator = createFilledPairedStatsAccumulator(MANY_VALUES, OTHER_MANY_VALUES);

    horizontalValuesAccumulator = new PairedStatsAccumulator();
    for (double x : MANY_VALUES) {
      horizontalValuesAccumulator.add(x, OTHER_ONE_VALUE);
    }

    verticalValuesAccumulator = new PairedStatsAccumulator();
    for (double y : OTHER_MANY_VALUES) {
      verticalValuesAccumulator.add(ONE_VALUE, y);
    }

    constantValuesAccumulator = new PairedStatsAccumulator();
    for (int i = 0; i < MANY_VALUES_COUNT; ++i) {
      constantValuesAccumulator.add(ONE_VALUE, OTHER_ONE_VALUE);
    }
  }

  public void testCount() {
    assertThat(emptyAccumulator.count()).isEqualTo(0);
    assertThat(oneValueAccumulator.count()).isEqualTo(1);
    assertThat(twoValuesAccumulator.count()).isEqualTo(2);
    assertThat(manyValuesAccumulator.count()).isEqualTo(MANY_VALUES_COUNT);
  }

  public void testXStats() {
    assertStatsApproxEqual(EMPTY_STATS_ITERABLE, emptyAccumulator.xStats());
    assertStatsApproxEqual(ONE_VALUE_STATS, oneValueAccumulator.xStats());
    assertStatsApproxEqual(TWO_VALUES_STATS, twoValuesAccumulator.xStats());
    assertStatsApproxEqual(MANY_VALUES_STATS_ITERABLE, manyValuesAccumulator.xStats());
  }

  public void testYStats() {
    assertStatsApproxEqual(EMPTY_STATS_ITERABLE, emptyAccumulator.yStats());
    assertStatsApproxEqual(OTHER_ONE_VALUE_STATS, oneValueAccumulator.yStats());
    assertStatsApproxEqual(OTHER_TWO_VALUES_STATS, twoValuesAccumulator.yStats());
    assertStatsApproxEqual(OTHER_MANY_VALUES_STATS, manyValuesAccumulator.yStats());
  }

  public void testPopulationCovariance() {
    try {
      emptyAccumulator.populationCovariance();
      fail("Expected IllegalStateException");
    } catch (IllegalStateException expected) {
    }
    assertThat(oneValueAccumulator.populationCovariance()).isWithin(0.0).of(0.0);
    assertThat(twoValuesAccumulator.populationCovariance())
        .isWithin(ALLOWED_ERROR)
        .of(TWO_VALUES_SUM_OF_PRODUCTS_OF_DELTAS / 2);
    // For datasets of many double values, we test many combinations of finite and non-finite
    // x-values:
    for (ManyValues values : ALL_MANY_VALUES) {
      PairedStatsAccumulator accumulator =
          createFilledPairedStatsAccumulator(values.asIterable(), OTHER_MANY_VALUES);
      double populationCovariance = accumulator.populationCovariance();
      if (values.hasAnyNonFinite()) {
        assertThat(populationCovariance).named("population covariance of " + values).isNaN();
      } else {
        assertThat(populationCovariance)
            .named("population covariance of " + values)
            .isWithin(ALLOWED_ERROR)
            .of(MANY_VALUES_SUM_OF_PRODUCTS_OF_DELTAS / MANY_VALUES_COUNT);
      }
    }
    assertThat(horizontalValuesAccumulator.populationCovariance()).isWithin(ALLOWED_ERROR).of(0.0);
    assertThat(verticalValuesAccumulator.populationCovariance()).isWithin(ALLOWED_ERROR).of(0.0);
    assertThat(constantValuesAccumulator.populationCovariance()).isWithin(ALLOWED_ERROR).of(0.0);
  }

  public void testSampleCovariance() {
    try {
      emptyAccumulator.sampleCovariance();
      fail("Expected IllegalStateException");
    } catch (IllegalStateException expected) {
    }
    try {
      oneValueAccumulator.sampleCovariance();
      fail("Expected IllegalStateException");
    } catch (IllegalStateException expected) {
    }
    assertThat(twoValuesAccumulator.sampleCovariance())
        .isWithin(ALLOWED_ERROR)
        .of(TWO_VALUES_SUM_OF_PRODUCTS_OF_DELTAS);
    assertThat(manyValuesAccumulator.sampleCovariance())
        .isWithin(ALLOWED_ERROR)
        .of(MANY_VALUES_SUM_OF_PRODUCTS_OF_DELTAS / (MANY_VALUES_COUNT - 1));
    assertThat(horizontalValuesAccumulator.sampleCovariance()).isWithin(ALLOWED_ERROR).of(0.0);
    assertThat(verticalValuesAccumulator.sampleCovariance()).isWithin(ALLOWED_ERROR).of(0.0);
    assertThat(constantValuesAccumulator.sampleCovariance()).isWithin(ALLOWED_ERROR).of(0.0);
  }

  public void testPearsonsCorrelationCoefficient() {
    try {
      emptyAccumulator.pearsonsCorrelationCoefficient();
      fail("Expected IllegalStateException");
    } catch (IllegalStateException expected) {
    }
    try {
      oneValueAccumulator.pearsonsCorrelationCoefficient();
      fail("Expected IllegalStateException");
    } catch (IllegalStateException expected) {
    }
    assertThat(twoValuesAccumulator.pearsonsCorrelationCoefficient())
        .isWithin(ALLOWED_ERROR)
        .of(
            twoValuesAccumulator.populationCovariance()
                / (twoValuesAccumulator.xStats().populationStandardDeviation()
                    * twoValuesAccumulator.yStats().populationStandardDeviation()));
    // For datasets of many double values, we test many combinations of finite and non-finite
    // y-values:
    for (ManyValues values : ALL_MANY_VALUES) {
      PairedStatsAccumulator accumulator =
          createFilledPairedStatsAccumulator(MANY_VALUES, values.asIterable());
      double pearsonsCorrelationCoefficient = accumulator.pearsonsCorrelationCoefficient();
      if (values.hasAnyNonFinite()) {
        assertThat(pearsonsCorrelationCoefficient)
            .named("Pearson's correlation coefficient of " + values)
            .isNaN();
      } else {
        assertThat(pearsonsCorrelationCoefficient)
            .named("Pearson's correlation coefficient of " + values)
            .isWithin(ALLOWED_ERROR)
            .of(
                accumulator.populationCovariance()
                    / (accumulator.xStats().populationStandardDeviation()
                        * accumulator.yStats().populationStandardDeviation()));
      }
    }
    try {
      horizontalValuesAccumulator.pearsonsCorrelationCoefficient();
      fail("Expected IllegalStateException");
    } catch (IllegalStateException expected) {
    }
    try {
      verticalValuesAccumulator.pearsonsCorrelationCoefficient();
      fail("Expected IllegalStateException");
    } catch (IllegalStateException expected) {
    }
    try {
      constantValuesAccumulator.pearsonsCorrelationCoefficient();
      fail("Expected IllegalStateException");
    } catch (IllegalStateException expected) {
    }
  }

  public void testLeastSquaresFit() {
    try {
      emptyAccumulator.leastSquaresFit();
      fail("Expected IllegalStateException");
    } catch (IllegalStateException expected) {
    }
    try {
      oneValueAccumulator.leastSquaresFit();
      fail("Expected IllegalStateException");
    } catch (IllegalStateException expected) {
    }
    assertDiagonalLinearTransformation(
        twoValuesAccumulator.leastSquaresFit(),
        twoValuesAccumulator.xStats().mean(),
        twoValuesAccumulator.yStats().mean(),
        twoValuesAccumulator.xStats().populationVariance(),
        twoValuesAccumulator.populationCovariance());
    // For datasets of many double values, we test many combinations of finite and non-finite
    // x-values:
    for (ManyValues values : ALL_MANY_VALUES) {
      PairedStatsAccumulator accumulator =
          createFilledPairedStatsAccumulator(values.asIterable(), OTHER_MANY_VALUES);
      LinearTransformation fit = accumulator.leastSquaresFit();
      if (values.hasAnyNonFinite()) {
        assertLinearTransformationNaN(fit);
      } else {
        assertDiagonalLinearTransformation(
            fit,
            accumulator.xStats().mean(),
            accumulator.yStats().mean(),
            accumulator.xStats().populationVariance(),
            accumulator.populationCovariance());
      }
    }
    assertHorizontalLinearTransformation(
        horizontalValuesAccumulator.leastSquaresFit(),
        horizontalValuesAccumulator.yStats().mean());
    assertVerticalLinearTransformation(
        verticalValuesAccumulator.leastSquaresFit(),
        verticalValuesAccumulator.xStats().mean());
    try {
      constantValuesAccumulator.leastSquaresFit();
      fail("Expected IllegalStateException");
    } catch (IllegalStateException expected) {
    }
  }
}
