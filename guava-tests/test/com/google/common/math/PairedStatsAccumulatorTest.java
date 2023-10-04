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
import static com.google.common.math.StatsTesting.OTHER_MANY_VALUES_COUNT;
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
import static com.google.common.math.StatsTesting.createPartitionedFilledPairedStatsAccumulator;
import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;
import static org.junit.Assert.assertThrows;

import com.google.common.math.StatsTesting.ManyValues;
import java.util.Collections;
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
  private PairedStatsAccumulator emptyAccumulatorByAddAllEmptyPairedStats;
  private PairedStatsAccumulator oneValueAccumulator;
  private PairedStatsAccumulator oneValueAccumulatorByAddAllEmptyPairedStats;
  private PairedStatsAccumulator twoValuesAccumulator;
  private PairedStatsAccumulator twoValuesAccumulatorByAddAllPartitionedPairedStats;
  private PairedStatsAccumulator manyValuesAccumulator;
  private PairedStatsAccumulator manyValuesAccumulatorByAddAllPartitionedPairedStats;
  private PairedStatsAccumulator horizontalValuesAccumulator;
  private PairedStatsAccumulator horizontalValuesAccumulatorByAddAllPartitionedPairedStats;
  private PairedStatsAccumulator verticalValuesAccumulator;
  private PairedStatsAccumulator verticalValuesAccumulatorByAddAllPartitionedPairedStats;
  private PairedStatsAccumulator constantValuesAccumulator;
  private PairedStatsAccumulator constantValuesAccumulatorByAddAllPartitionedPairedStats;

  @Override
  protected void setUp() throws Exception {
    super.setUp();

    emptyAccumulator = new PairedStatsAccumulator();

    emptyAccumulatorByAddAllEmptyPairedStats = new PairedStatsAccumulator();
    emptyAccumulatorByAddAllEmptyPairedStats.addAll(emptyAccumulator.snapshot());

    oneValueAccumulator = new PairedStatsAccumulator();
    oneValueAccumulator.add(ONE_VALUE, OTHER_ONE_VALUE);

    oneValueAccumulatorByAddAllEmptyPairedStats = new PairedStatsAccumulator();
    oneValueAccumulatorByAddAllEmptyPairedStats.add(ONE_VALUE, OTHER_ONE_VALUE);
    oneValueAccumulatorByAddAllEmptyPairedStats.addAll(emptyAccumulator.snapshot());

    twoValuesAccumulator = createFilledPairedStatsAccumulator(TWO_VALUES, OTHER_TWO_VALUES);
    twoValuesAccumulatorByAddAllPartitionedPairedStats =
        createPartitionedFilledPairedStatsAccumulator(TWO_VALUES, OTHER_TWO_VALUES, 1);

    manyValuesAccumulator = createFilledPairedStatsAccumulator(MANY_VALUES, OTHER_MANY_VALUES);
    manyValuesAccumulatorByAddAllPartitionedPairedStats =
        createPartitionedFilledPairedStatsAccumulator(MANY_VALUES, OTHER_MANY_VALUES, 2);

    horizontalValuesAccumulator =
        createFilledPairedStatsAccumulator(
            MANY_VALUES, Collections.nCopies(MANY_VALUES_COUNT, OTHER_ONE_VALUE));
    horizontalValuesAccumulatorByAddAllPartitionedPairedStats =
        createPartitionedFilledPairedStatsAccumulator(
            MANY_VALUES, Collections.nCopies(MANY_VALUES_COUNT, OTHER_ONE_VALUE), 2);

    verticalValuesAccumulator =
        createFilledPairedStatsAccumulator(
            Collections.nCopies(OTHER_MANY_VALUES_COUNT, ONE_VALUE), OTHER_MANY_VALUES);
    verticalValuesAccumulatorByAddAllPartitionedPairedStats =
        createPartitionedFilledPairedStatsAccumulator(
            Collections.nCopies(OTHER_MANY_VALUES_COUNT, ONE_VALUE), OTHER_MANY_VALUES, 2);

    constantValuesAccumulator =
        createFilledPairedStatsAccumulator(
            Collections.nCopies(MANY_VALUES_COUNT, ONE_VALUE),
            Collections.nCopies(MANY_VALUES_COUNT, OTHER_ONE_VALUE));
    constantValuesAccumulatorByAddAllPartitionedPairedStats =
        createPartitionedFilledPairedStatsAccumulator(
            Collections.nCopies(MANY_VALUES_COUNT, ONE_VALUE),
            Collections.nCopies(MANY_VALUES_COUNT, OTHER_ONE_VALUE),
            2);
  }

  public void testCount() {
    assertThat(emptyAccumulator.count()).isEqualTo(0);
    assertThat(emptyAccumulatorByAddAllEmptyPairedStats.count()).isEqualTo(0);
    assertThat(oneValueAccumulator.count()).isEqualTo(1);
    assertThat(oneValueAccumulatorByAddAllEmptyPairedStats.count()).isEqualTo(1);
    assertThat(twoValuesAccumulator.count()).isEqualTo(2);
    assertThat(twoValuesAccumulatorByAddAllPartitionedPairedStats.count()).isEqualTo(2);
    assertThat(manyValuesAccumulator.count()).isEqualTo(MANY_VALUES_COUNT);
    assertThat(manyValuesAccumulatorByAddAllPartitionedPairedStats.count())
        .isEqualTo(MANY_VALUES_COUNT);
  }

  public void testCountOverflow_doesNotThrow() {
    PairedStatsAccumulator accumulator = new PairedStatsAccumulator();
    accumulator.add(ONE_VALUE, OTHER_ONE_VALUE);
    for (int power = 1; power < Long.SIZE - 1; power++) {
      accumulator.addAll(accumulator.snapshot());
    }
    // Should overflow without throwing.
    accumulator.addAll(accumulator.snapshot());
    assertThat(accumulator.count()).isLessThan(0L);
  }

  public void testXStats() {
    assertStatsApproxEqual(EMPTY_STATS_ITERABLE, emptyAccumulator.xStats());
    assertStatsApproxEqual(EMPTY_STATS_ITERABLE, emptyAccumulatorByAddAllEmptyPairedStats.xStats());
    assertStatsApproxEqual(ONE_VALUE_STATS, oneValueAccumulator.xStats());
    assertStatsApproxEqual(ONE_VALUE_STATS, oneValueAccumulatorByAddAllEmptyPairedStats.xStats());
    assertStatsApproxEqual(TWO_VALUES_STATS, twoValuesAccumulator.xStats());
    assertStatsApproxEqual(
        TWO_VALUES_STATS, twoValuesAccumulatorByAddAllPartitionedPairedStats.xStats());
    assertStatsApproxEqual(MANY_VALUES_STATS_ITERABLE, manyValuesAccumulator.xStats());
    assertStatsApproxEqual(
        MANY_VALUES_STATS_ITERABLE, manyValuesAccumulatorByAddAllPartitionedPairedStats.xStats());
  }

  public void testYStats() {
    assertStatsApproxEqual(EMPTY_STATS_ITERABLE, emptyAccumulator.yStats());
    assertStatsApproxEqual(EMPTY_STATS_ITERABLE, emptyAccumulatorByAddAllEmptyPairedStats.yStats());
    assertStatsApproxEqual(OTHER_ONE_VALUE_STATS, oneValueAccumulator.yStats());
    assertStatsApproxEqual(
        OTHER_ONE_VALUE_STATS, oneValueAccumulatorByAddAllEmptyPairedStats.yStats());
    assertStatsApproxEqual(OTHER_TWO_VALUES_STATS, twoValuesAccumulator.yStats());
    assertStatsApproxEqual(
        OTHER_TWO_VALUES_STATS, twoValuesAccumulatorByAddAllPartitionedPairedStats.yStats());
    assertStatsApproxEqual(OTHER_MANY_VALUES_STATS, manyValuesAccumulator.yStats());
    assertStatsApproxEqual(
        OTHER_MANY_VALUES_STATS, manyValuesAccumulatorByAddAllPartitionedPairedStats.yStats());
  }

  public void testPopulationCovariance() {
    assertThrows(IllegalStateException.class, () -> emptyAccumulator.populationCovariance());
    assertThrows(
        IllegalStateException.class,
        () -> emptyAccumulatorByAddAllEmptyPairedStats.populationCovariance());
    assertThat(oneValueAccumulator.populationCovariance()).isWithin(0.0).of(0.0);
    assertThat(oneValueAccumulatorByAddAllEmptyPairedStats.populationCovariance())
        .isWithin(0.0)
        .of(0.0);
    assertThat(twoValuesAccumulator.populationCovariance())
        .isWithin(ALLOWED_ERROR)
        .of(TWO_VALUES_SUM_OF_PRODUCTS_OF_DELTAS / 2);
    assertThat(twoValuesAccumulatorByAddAllPartitionedPairedStats.populationCovariance())
        .isWithin(ALLOWED_ERROR)
        .of(TWO_VALUES_SUM_OF_PRODUCTS_OF_DELTAS / 2);
    assertThat(manyValuesAccumulator.populationCovariance())
        .isWithin(ALLOWED_ERROR)
        .of(MANY_VALUES_SUM_OF_PRODUCTS_OF_DELTAS / MANY_VALUES_COUNT);
    assertThat(manyValuesAccumulatorByAddAllPartitionedPairedStats.populationCovariance())
        .isWithin(ALLOWED_ERROR)
        .of(MANY_VALUES_SUM_OF_PRODUCTS_OF_DELTAS / MANY_VALUES_COUNT);
    // For datasets of many double values, we test many combinations of finite and non-finite
    // x-values:
    for (ManyValues values : ALL_MANY_VALUES) {
      PairedStatsAccumulator accumulator =
          createFilledPairedStatsAccumulator(values.asIterable(), OTHER_MANY_VALUES);
      PairedStatsAccumulator accumulatorByAddAllPartitionedPairedStats =
          createPartitionedFilledPairedStatsAccumulator(values.asIterable(), OTHER_MANY_VALUES, 2);
      double populationCovariance = accumulator.populationCovariance();
      double populationCovarianceByAddAllPartitionedPairedStats =
          accumulatorByAddAllPartitionedPairedStats.populationCovariance();
      if (values.hasAnyNonFinite()) {
        assertWithMessage("population covariance of " + values).that(populationCovariance).isNaN();
        assertWithMessage("population covariance by addAll(PairedStats) of " + values)
            .that(populationCovarianceByAddAllPartitionedPairedStats)
            .isNaN();
      } else {
        assertWithMessage("population covariance of " + values)
            .that(populationCovariance)
            .isWithin(ALLOWED_ERROR)
            .of(MANY_VALUES_SUM_OF_PRODUCTS_OF_DELTAS / MANY_VALUES_COUNT);
        assertWithMessage("population covariance by addAll(PairedStats) of " + values)
            .that(populationCovarianceByAddAllPartitionedPairedStats)
            .isWithin(ALLOWED_ERROR)
            .of(MANY_VALUES_SUM_OF_PRODUCTS_OF_DELTAS / MANY_VALUES_COUNT);
      }
    }
    assertThat(horizontalValuesAccumulator.populationCovariance()).isWithin(ALLOWED_ERROR).of(0.0);
    assertThat(horizontalValuesAccumulatorByAddAllPartitionedPairedStats.populationCovariance())
        .isWithin(ALLOWED_ERROR)
        .of(0.0);
    assertThat(verticalValuesAccumulator.populationCovariance()).isWithin(ALLOWED_ERROR).of(0.0);
    assertThat(verticalValuesAccumulatorByAddAllPartitionedPairedStats.populationCovariance())
        .isWithin(ALLOWED_ERROR)
        .of(0.0);
    assertThat(constantValuesAccumulator.populationCovariance()).isWithin(ALLOWED_ERROR).of(0.0);
    assertThat(constantValuesAccumulatorByAddAllPartitionedPairedStats.populationCovariance())
        .isWithin(ALLOWED_ERROR)
        .of(0.0);
  }

  public void testSampleCovariance() {
    assertThrows(IllegalStateException.class, () -> emptyAccumulator.sampleCovariance());
    assertThrows(
        IllegalStateException.class,
        () -> emptyAccumulatorByAddAllEmptyPairedStats.sampleCovariance());
    assertThrows(IllegalStateException.class, () -> oneValueAccumulator.sampleCovariance());
    assertThrows(
        IllegalStateException.class,
        () -> oneValueAccumulatorByAddAllEmptyPairedStats.sampleCovariance());
    assertThat(twoValuesAccumulator.sampleCovariance())
        .isWithin(ALLOWED_ERROR)
        .of(TWO_VALUES_SUM_OF_PRODUCTS_OF_DELTAS);
    assertThat(twoValuesAccumulatorByAddAllPartitionedPairedStats.sampleCovariance())
        .isWithin(ALLOWED_ERROR)
        .of(TWO_VALUES_SUM_OF_PRODUCTS_OF_DELTAS);
    assertThat(manyValuesAccumulator.sampleCovariance())
        .isWithin(ALLOWED_ERROR)
        .of(MANY_VALUES_SUM_OF_PRODUCTS_OF_DELTAS / (MANY_VALUES_COUNT - 1));
    assertThat(manyValuesAccumulatorByAddAllPartitionedPairedStats.sampleCovariance())
        .isWithin(ALLOWED_ERROR)
        .of(MANY_VALUES_SUM_OF_PRODUCTS_OF_DELTAS / (MANY_VALUES_COUNT - 1));
    assertThat(horizontalValuesAccumulator.sampleCovariance()).isWithin(ALLOWED_ERROR).of(0.0);
    assertThat(horizontalValuesAccumulatorByAddAllPartitionedPairedStats.sampleCovariance())
        .isWithin(ALLOWED_ERROR)
        .of(0.0);
    assertThat(verticalValuesAccumulator.sampleCovariance()).isWithin(ALLOWED_ERROR).of(0.0);
    assertThat(verticalValuesAccumulatorByAddAllPartitionedPairedStats.sampleCovariance())
        .isWithin(ALLOWED_ERROR)
        .of(0.0);
    assertThat(constantValuesAccumulator.sampleCovariance()).isWithin(ALLOWED_ERROR).of(0.0);
    assertThat(constantValuesAccumulatorByAddAllPartitionedPairedStats.sampleCovariance())
        .isWithin(ALLOWED_ERROR)
        .of(0.0);
  }

  public void testPearsonsCorrelationCoefficient() {
    assertThrows(
        IllegalStateException.class, () -> emptyAccumulator.pearsonsCorrelationCoefficient());
    assertThrows(
        IllegalStateException.class,
        () -> emptyAccumulatorByAddAllEmptyPairedStats.pearsonsCorrelationCoefficient());
    assertThrows(
        IllegalStateException.class, () -> oneValueAccumulator.pearsonsCorrelationCoefficient());
    assertThrows(
        IllegalStateException.class,
        () -> oneValueAccumulatorByAddAllEmptyPairedStats.pearsonsCorrelationCoefficient());
    assertThat(twoValuesAccumulator.pearsonsCorrelationCoefficient())
        .isWithin(ALLOWED_ERROR)
        .of(
            twoValuesAccumulator.populationCovariance()
                / (twoValuesAccumulator.xStats().populationStandardDeviation()
                    * twoValuesAccumulator.yStats().populationStandardDeviation()));
    assertThat(manyValuesAccumulator.pearsonsCorrelationCoefficient())
        .isWithin(ALLOWED_ERROR)
        .of(
            manyValuesAccumulator.populationCovariance()
                / (manyValuesAccumulator.xStats().populationStandardDeviation()
                    * manyValuesAccumulator.yStats().populationStandardDeviation()));
    assertThat(manyValuesAccumulatorByAddAllPartitionedPairedStats.pearsonsCorrelationCoefficient())
        .isWithin(ALLOWED_ERROR)
        .of(
            manyValuesAccumulatorByAddAllPartitionedPairedStats.populationCovariance()
                / (manyValuesAccumulatorByAddAllPartitionedPairedStats
                        .xStats()
                        .populationStandardDeviation()
                    * manyValuesAccumulatorByAddAllPartitionedPairedStats
                        .yStats()
                        .populationStandardDeviation()));
    // For datasets of many double values, we test many combinations of finite and non-finite
    // y-values:
    for (ManyValues values : ALL_MANY_VALUES) {
      PairedStatsAccumulator accumulator =
          createFilledPairedStatsAccumulator(MANY_VALUES, values.asIterable());
      PairedStatsAccumulator accumulatorByAddAllPartitionedPairedStats =
          createPartitionedFilledPairedStatsAccumulator(MANY_VALUES, values.asIterable(), 2);
      double pearsonsCorrelationCoefficient = accumulator.pearsonsCorrelationCoefficient();
      double pearsonsCorrelationCoefficientByAddAllPartitionedPairedStats =
          accumulatorByAddAllPartitionedPairedStats.pearsonsCorrelationCoefficient();
      if (values.hasAnyNonFinite()) {
        assertWithMessage("Pearson's correlation coefficient of " + values)
            .that(pearsonsCorrelationCoefficient)
            .isNaN();
        assertWithMessage("Pearson's correlation coefficient by addAll(PairedStats) of " + values)
            .that(pearsonsCorrelationCoefficient)
            .isNaN();
      } else {
        assertWithMessage("Pearson's correlation coefficient of " + values)
            .that(pearsonsCorrelationCoefficient)
            .isWithin(ALLOWED_ERROR)
            .of(
                accumulator.populationCovariance()
                    / (accumulator.xStats().populationStandardDeviation()
                        * accumulator.yStats().populationStandardDeviation()));
        assertWithMessage("Pearson's correlation coefficient by addAll(PairedStats) of " + values)
            .that(pearsonsCorrelationCoefficientByAddAllPartitionedPairedStats)
            .isWithin(ALLOWED_ERROR)
            .of(
                accumulatorByAddAllPartitionedPairedStats.populationCovariance()
                    / (accumulatorByAddAllPartitionedPairedStats
                            .xStats()
                            .populationStandardDeviation()
                        * accumulatorByAddAllPartitionedPairedStats
                            .yStats()
                            .populationStandardDeviation()));
      }
    }
    assertThrows(
        IllegalStateException.class,
        () -> horizontalValuesAccumulator.pearsonsCorrelationCoefficient());
    assertThrows(
        IllegalStateException.class,
        () ->
            horizontalValuesAccumulatorByAddAllPartitionedPairedStats
                .pearsonsCorrelationCoefficient());
    assertThrows(
        IllegalStateException.class,
        () -> verticalValuesAccumulator.pearsonsCorrelationCoefficient());
    assertThrows(
        IllegalStateException.class,
        () ->
            verticalValuesAccumulatorByAddAllPartitionedPairedStats
                .pearsonsCorrelationCoefficient());
    assertThrows(
        IllegalStateException.class,
        () -> constantValuesAccumulator.pearsonsCorrelationCoefficient());
    assertThrows(
        IllegalStateException.class,
        () ->
            constantValuesAccumulatorByAddAllPartitionedPairedStats
                .pearsonsCorrelationCoefficient());
  }

  public void testLeastSquaresFit() {
    assertThrows(IllegalStateException.class, () -> emptyAccumulator.leastSquaresFit());
    assertThrows(
        IllegalStateException.class,
        () -> emptyAccumulatorByAddAllEmptyPairedStats.leastSquaresFit());
    assertThrows(IllegalStateException.class, () -> oneValueAccumulator.leastSquaresFit());
    assertThrows(
        IllegalStateException.class,
        () -> oneValueAccumulatorByAddAllEmptyPairedStats.leastSquaresFit());
    assertDiagonalLinearTransformation(
        twoValuesAccumulator.leastSquaresFit(),
        twoValuesAccumulator.xStats().mean(),
        twoValuesAccumulator.yStats().mean(),
        twoValuesAccumulator.xStats().populationVariance(),
        twoValuesAccumulator.populationCovariance());
    assertDiagonalLinearTransformation(
        twoValuesAccumulatorByAddAllPartitionedPairedStats.leastSquaresFit(),
        twoValuesAccumulatorByAddAllPartitionedPairedStats.xStats().mean(),
        twoValuesAccumulatorByAddAllPartitionedPairedStats.yStats().mean(),
        twoValuesAccumulatorByAddAllPartitionedPairedStats.xStats().populationVariance(),
        twoValuesAccumulatorByAddAllPartitionedPairedStats.populationCovariance());
    assertDiagonalLinearTransformation(
        manyValuesAccumulator.leastSquaresFit(),
        manyValuesAccumulator.xStats().mean(),
        manyValuesAccumulator.yStats().mean(),
        manyValuesAccumulator.xStats().populationVariance(),
        manyValuesAccumulator.populationCovariance());
    assertDiagonalLinearTransformation(
        manyValuesAccumulatorByAddAllPartitionedPairedStats.leastSquaresFit(),
        manyValuesAccumulatorByAddAllPartitionedPairedStats.xStats().mean(),
        manyValuesAccumulatorByAddAllPartitionedPairedStats.yStats().mean(),
        manyValuesAccumulatorByAddAllPartitionedPairedStats.xStats().populationVariance(),
        manyValuesAccumulatorByAddAllPartitionedPairedStats.populationCovariance());
    // For datasets of many double values, we test many combinations of finite and non-finite
    // x-values:
    for (ManyValues values : ALL_MANY_VALUES) {
      PairedStatsAccumulator accumulator =
          createFilledPairedStatsAccumulator(values.asIterable(), OTHER_MANY_VALUES);
      PairedStatsAccumulator accumulatorByAddAllPartitionedPairedStats =
          createPartitionedFilledPairedStatsAccumulator(values.asIterable(), OTHER_MANY_VALUES, 2);
      LinearTransformation fit = accumulator.leastSquaresFit();
      LinearTransformation fitByAddAllPartitionedPairedStats =
          accumulatorByAddAllPartitionedPairedStats.leastSquaresFit();
      if (values.hasAnyNonFinite()) {
        assertLinearTransformationNaN(fit);
        assertLinearTransformationNaN(fitByAddAllPartitionedPairedStats);
      } else {
        assertDiagonalLinearTransformation(
            fit,
            accumulator.xStats().mean(),
            accumulator.yStats().mean(),
            accumulator.xStats().populationVariance(),
            accumulator.populationCovariance());
        assertDiagonalLinearTransformation(
            fitByAddAllPartitionedPairedStats,
            accumulatorByAddAllPartitionedPairedStats.xStats().mean(),
            accumulatorByAddAllPartitionedPairedStats.yStats().mean(),
            accumulatorByAddAllPartitionedPairedStats.xStats().populationVariance(),
            accumulatorByAddAllPartitionedPairedStats.populationCovariance());
      }
    }
    assertHorizontalLinearTransformation(
        horizontalValuesAccumulator.leastSquaresFit(), horizontalValuesAccumulator.yStats().mean());
    assertHorizontalLinearTransformation(
        horizontalValuesAccumulatorByAddAllPartitionedPairedStats.leastSquaresFit(),
        horizontalValuesAccumulatorByAddAllPartitionedPairedStats.yStats().mean());
    assertVerticalLinearTransformation(
        verticalValuesAccumulator.leastSquaresFit(), verticalValuesAccumulator.xStats().mean());
    assertVerticalLinearTransformation(
        verticalValuesAccumulatorByAddAllPartitionedPairedStats.leastSquaresFit(),
        verticalValuesAccumulatorByAddAllPartitionedPairedStats.xStats().mean());
    assertThrows(IllegalStateException.class, () -> constantValuesAccumulator.leastSquaresFit());
    assertThrows(
        IllegalStateException.class,
        () -> constantValuesAccumulatorByAddAllPartitionedPairedStats.leastSquaresFit());
  }
}
