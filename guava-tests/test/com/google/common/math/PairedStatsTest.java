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
import static com.google.common.math.StatsTesting.ALL_PAIRED_STATS;
import static com.google.common.math.StatsTesting.CONSTANT_VALUES_PAIRED_STATS;
import static com.google.common.math.StatsTesting.DUPLICATE_MANY_VALUES_PAIRED_STATS;
import static com.google.common.math.StatsTesting.EMPTY_PAIRED_STATS;
import static com.google.common.math.StatsTesting.EMPTY_STATS_ITERABLE;
import static com.google.common.math.StatsTesting.HORIZONTAL_VALUES_PAIRED_STATS;
import static com.google.common.math.StatsTesting.MANY_VALUES;
import static com.google.common.math.StatsTesting.MANY_VALUES_COUNT;
import static com.google.common.math.StatsTesting.MANY_VALUES_PAIRED_STATS;
import static com.google.common.math.StatsTesting.MANY_VALUES_STATS_ITERABLE;
import static com.google.common.math.StatsTesting.MANY_VALUES_STATS_VARARGS;
import static com.google.common.math.StatsTesting.MANY_VALUES_SUM_OF_PRODUCTS_OF_DELTAS;
import static com.google.common.math.StatsTesting.ONE_VALUE_PAIRED_STATS;
import static com.google.common.math.StatsTesting.ONE_VALUE_STATS;
import static com.google.common.math.StatsTesting.OTHER_MANY_VALUES;
import static com.google.common.math.StatsTesting.OTHER_MANY_VALUES_STATS;
import static com.google.common.math.StatsTesting.OTHER_ONE_VALUE_STATS;
import static com.google.common.math.StatsTesting.OTHER_TWO_VALUES_STATS;
import static com.google.common.math.StatsTesting.TWO_VALUES_PAIRED_STATS;
import static com.google.common.math.StatsTesting.TWO_VALUES_STATS;
import static com.google.common.math.StatsTesting.TWO_VALUES_SUM_OF_PRODUCTS_OF_DELTAS;
import static com.google.common.math.StatsTesting.VERTICAL_VALUES_PAIRED_STATS;
import static com.google.common.math.StatsTesting.assertDiagonalLinearTransformation;
import static com.google.common.math.StatsTesting.assertHorizontalLinearTransformation;
import static com.google.common.math.StatsTesting.assertLinearTransformationNaN;
import static com.google.common.math.StatsTesting.assertStatsApproxEqual;
import static com.google.common.math.StatsTesting.assertVerticalLinearTransformation;
import static com.google.common.math.StatsTesting.createPairedStatsOf;
import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;

import com.google.common.collect.ImmutableList;
import com.google.common.math.StatsTesting.ManyValues;
import com.google.common.testing.EqualsTester;
import com.google.common.testing.SerializableTester;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import junit.framework.TestCase;

/**
 * Tests for {@link PairedStats}. This tests instances created by {@link
 * PairedStatsAccumulator#snapshot}.
 *
 * @author Pete Gillin
 */
public class PairedStatsTest extends TestCase {

  public void testCount() {
    assertThat(EMPTY_PAIRED_STATS.count()).isEqualTo(0);
    assertThat(ONE_VALUE_PAIRED_STATS.count()).isEqualTo(1);
    assertThat(TWO_VALUES_PAIRED_STATS.count()).isEqualTo(2);
    assertThat(MANY_VALUES_PAIRED_STATS.count()).isEqualTo(MANY_VALUES_COUNT);
  }

  public void testXStats() {
    assertStatsApproxEqual(EMPTY_STATS_ITERABLE, EMPTY_PAIRED_STATS.xStats());
    assertStatsApproxEqual(ONE_VALUE_STATS, ONE_VALUE_PAIRED_STATS.xStats());
    assertStatsApproxEqual(TWO_VALUES_STATS, TWO_VALUES_PAIRED_STATS.xStats());
    assertStatsApproxEqual(MANY_VALUES_STATS_ITERABLE, MANY_VALUES_PAIRED_STATS.xStats());
  }

  public void testYStats() {
    assertStatsApproxEqual(EMPTY_STATS_ITERABLE, EMPTY_PAIRED_STATS.yStats());
    assertStatsApproxEqual(OTHER_ONE_VALUE_STATS, ONE_VALUE_PAIRED_STATS.yStats());
    assertStatsApproxEqual(OTHER_TWO_VALUES_STATS, TWO_VALUES_PAIRED_STATS.yStats());
    assertStatsApproxEqual(OTHER_MANY_VALUES_STATS, MANY_VALUES_PAIRED_STATS.yStats());
  }

  public void testPopulationCovariance() {
    try {
      EMPTY_PAIRED_STATS.populationCovariance();
      fail("Expected IllegalStateException");
    } catch (IllegalStateException expected) {
    }
    assertThat(ONE_VALUE_PAIRED_STATS.populationCovariance()).isWithin(0.0).of(0.0);
    assertThat(createSingleStats(Double.POSITIVE_INFINITY, 1.23).populationCovariance()).isNaN();
    assertThat(createSingleStats(Double.NEGATIVE_INFINITY, 1.23).populationCovariance()).isNaN();
    assertThat(createSingleStats(Double.NaN, 1.23).populationCovariance()).isNaN();
    assertThat(TWO_VALUES_PAIRED_STATS.populationCovariance())
        .isWithin(ALLOWED_ERROR)
        .of(TWO_VALUES_SUM_OF_PRODUCTS_OF_DELTAS / 2);
    // For datasets of many double values, we test many combinations of finite and non-finite
    // x-values:
    for (ManyValues values : ALL_MANY_VALUES) {
      PairedStats stats = createPairedStatsOf(values.asIterable(), OTHER_MANY_VALUES);
      double populationCovariance = stats.populationCovariance();
      if (values.hasAnyNonFinite()) {
        assertWithMessage("population covariance of " + values).that(populationCovariance).isNaN();
      } else {
        assertWithMessage("population covariance of " + values)
            .that(populationCovariance)
            .isWithin(ALLOWED_ERROR)
            .of(MANY_VALUES_SUM_OF_PRODUCTS_OF_DELTAS / MANY_VALUES_COUNT);
      }
    }
    assertThat(HORIZONTAL_VALUES_PAIRED_STATS.populationCovariance())
        .isWithin(ALLOWED_ERROR)
        .of(0.0);
    assertThat(VERTICAL_VALUES_PAIRED_STATS.populationCovariance()).isWithin(ALLOWED_ERROR).of(0.0);
    assertThat(CONSTANT_VALUES_PAIRED_STATS.populationCovariance()).isWithin(ALLOWED_ERROR).of(0.0);
  }

  public void testSampleCovariance() {
    try {
      EMPTY_PAIRED_STATS.sampleCovariance();
      fail("Expected IllegalStateException");
    } catch (IllegalStateException expected) {
    }
    try {
      ONE_VALUE_PAIRED_STATS.sampleCovariance();
      fail("Expected IllegalStateException");
    } catch (IllegalStateException expected) {
    }
    assertThat(TWO_VALUES_PAIRED_STATS.sampleCovariance())
        .isWithin(ALLOWED_ERROR)
        .of(TWO_VALUES_SUM_OF_PRODUCTS_OF_DELTAS);
    assertThat(MANY_VALUES_PAIRED_STATS.sampleCovariance())
        .isWithin(ALLOWED_ERROR)
        .of(MANY_VALUES_SUM_OF_PRODUCTS_OF_DELTAS / (MANY_VALUES_COUNT - 1));
    assertThat(HORIZONTAL_VALUES_PAIRED_STATS.sampleCovariance()).isWithin(ALLOWED_ERROR).of(0.0);
    assertThat(VERTICAL_VALUES_PAIRED_STATS.sampleCovariance()).isWithin(ALLOWED_ERROR).of(0.0);
    assertThat(CONSTANT_VALUES_PAIRED_STATS.sampleCovariance()).isWithin(ALLOWED_ERROR).of(0.0);
  }

  public void testPearsonsCorrelationCoefficient() {
    try {
      EMPTY_PAIRED_STATS.pearsonsCorrelationCoefficient();
      fail("Expected IllegalStateException");
    } catch (IllegalStateException expected) {
    }
    try {
      ONE_VALUE_PAIRED_STATS.pearsonsCorrelationCoefficient();
      fail("Expected IllegalStateException");
    } catch (IllegalStateException expected) {
    }
    try {
      createSingleStats(Double.POSITIVE_INFINITY, 1.23).pearsonsCorrelationCoefficient();
      fail("Expected IllegalStateException");
    } catch (IllegalStateException expected) {
    }
    assertThat(TWO_VALUES_PAIRED_STATS.pearsonsCorrelationCoefficient())
        .isWithin(ALLOWED_ERROR)
        .of(
            TWO_VALUES_PAIRED_STATS.populationCovariance()
                / (TWO_VALUES_PAIRED_STATS.xStats().populationStandardDeviation()
                    * TWO_VALUES_PAIRED_STATS.yStats().populationStandardDeviation()));
    // For datasets of many double values, we test many combinations of finite and non-finite
    // y-values:
    for (ManyValues values : ALL_MANY_VALUES) {
      PairedStats stats = createPairedStatsOf(MANY_VALUES, values.asIterable());
      double pearsonsCorrelationCoefficient = stats.pearsonsCorrelationCoefficient();
      if (values.hasAnyNonFinite()) {
        assertWithMessage("Pearson's correlation coefficient of " + values)
            .that(pearsonsCorrelationCoefficient)
            .isNaN();
      } else {
        assertWithMessage("Pearson's correlation coefficient of " + values)
            .that(pearsonsCorrelationCoefficient)
            .isWithin(ALLOWED_ERROR)
            .of(
                stats.populationCovariance()
                    / (stats.xStats().populationStandardDeviation()
                        * stats.yStats().populationStandardDeviation()));
      }
    }
    try {
      HORIZONTAL_VALUES_PAIRED_STATS.pearsonsCorrelationCoefficient();
      fail("Expected IllegalStateException");
    } catch (IllegalStateException expected) {
    }
    try {
      VERTICAL_VALUES_PAIRED_STATS.pearsonsCorrelationCoefficient();
      fail("Expected IllegalStateException");
    } catch (IllegalStateException expected) {
    }
    try {
      CONSTANT_VALUES_PAIRED_STATS.pearsonsCorrelationCoefficient();
      fail("Expected IllegalStateException");
    } catch (IllegalStateException expected) {
    }
  }

  public void testLeastSquaresFit() {
    try {
      EMPTY_PAIRED_STATS.leastSquaresFit();
      fail("Expected IllegalStateException");
    } catch (IllegalStateException expected) {
    }
    try {
      ONE_VALUE_PAIRED_STATS.leastSquaresFit();
      fail("Expected IllegalStateException");
    } catch (IllegalStateException expected) {
    }
    try {
      createSingleStats(Double.POSITIVE_INFINITY, 1.23).leastSquaresFit();
      fail("Expected IllegalStateException");
    } catch (IllegalStateException expected) {
    }
    assertDiagonalLinearTransformation(
        TWO_VALUES_PAIRED_STATS.leastSquaresFit(),
        TWO_VALUES_PAIRED_STATS.xStats().mean(),
        TWO_VALUES_PAIRED_STATS.yStats().mean(),
        TWO_VALUES_PAIRED_STATS.xStats().populationVariance(),
        TWO_VALUES_PAIRED_STATS.populationCovariance());
    // For datasets of many double values, we test many combinations of finite and non-finite
    // x-values:
    for (ManyValues values : ALL_MANY_VALUES) {
      PairedStats stats = createPairedStatsOf(values.asIterable(), OTHER_MANY_VALUES);
      LinearTransformation fit = stats.leastSquaresFit();
      if (values.hasAnyNonFinite()) {
        assertLinearTransformationNaN(fit);
      } else {
        assertDiagonalLinearTransformation(
            fit,
            stats.xStats().mean(),
            stats.yStats().mean(),
            stats.xStats().populationVariance(),
            stats.populationCovariance());
      }
    }
    assertHorizontalLinearTransformation(
        HORIZONTAL_VALUES_PAIRED_STATS.leastSquaresFit(),
        HORIZONTAL_VALUES_PAIRED_STATS.yStats().mean());
    assertVerticalLinearTransformation(
        VERTICAL_VALUES_PAIRED_STATS.leastSquaresFit(),
        VERTICAL_VALUES_PAIRED_STATS.xStats().mean());
    try {
      CONSTANT_VALUES_PAIRED_STATS.leastSquaresFit();
      fail("Expected IllegalStateException");
    } catch (IllegalStateException expected) {
    }
  }

  public void testEqualsAndHashCode() {
    new EqualsTester()
        .addEqualityGroup(
            MANY_VALUES_PAIRED_STATS,
            DUPLICATE_MANY_VALUES_PAIRED_STATS,
            SerializableTester.reserialize(MANY_VALUES_PAIRED_STATS))
        .addEqualityGroup(
            new PairedStats(MANY_VALUES_STATS_ITERABLE, OTHER_MANY_VALUES_STATS, 1.23),
            new PairedStats(MANY_VALUES_STATS_VARARGS, OTHER_MANY_VALUES_STATS, 1.23))
        .addEqualityGroup(
            new PairedStats(OTHER_MANY_VALUES_STATS, MANY_VALUES_STATS_ITERABLE, 1.23))
        .addEqualityGroup(
            new PairedStats(MANY_VALUES_STATS_ITERABLE, MANY_VALUES_STATS_ITERABLE, 1.23))
        .addEqualityGroup(new PairedStats(TWO_VALUES_STATS, MANY_VALUES_STATS_ITERABLE, 1.23))
        .addEqualityGroup(new PairedStats(MANY_VALUES_STATS_ITERABLE, ONE_VALUE_STATS, 1.23))
        .addEqualityGroup(
            new PairedStats(MANY_VALUES_STATS_ITERABLE, MANY_VALUES_STATS_ITERABLE, 1.234))
        .testEquals();
  }

  public void testSerializable() {
    SerializableTester.reserializeAndAssert(MANY_VALUES_PAIRED_STATS);
  }

  public void testToString() {
    assertThat(EMPTY_PAIRED_STATS.toString())
        .isEqualTo("PairedStats{xStats=Stats{count=0}, yStats=Stats{count=0}}");
    assertThat(MANY_VALUES_PAIRED_STATS.toString())
        .isEqualTo(
            "PairedStats{xStats="
                + MANY_VALUES_PAIRED_STATS.xStats()
                + ", yStats="
                + MANY_VALUES_PAIRED_STATS.yStats()
                + ", populationCovariance="
                + MANY_VALUES_PAIRED_STATS.populationCovariance()
                + "}");
  }

  private PairedStats createSingleStats(double x, double y) {
    return createPairedStatsOf(ImmutableList.of(x), ImmutableList.of(y));
  }

  public void testToByteArrayAndFromByteArrayRoundTrip() {
    for (PairedStats pairedStats : ALL_PAIRED_STATS) {
      byte[] pairedStatsByteArray = pairedStats.toByteArray();

      // Round trip to byte array and back
      assertThat(PairedStats.fromByteArray(pairedStatsByteArray)).isEqualTo(pairedStats);
    }
  }

  public void testFromByteArray_withNullInputThrowsNullPointerException() {
    try {
      PairedStats.fromByteArray(null);
      fail("Expected NullPointerException");
    } catch (NullPointerException expected) {
    }
  }

  public void testFromByteArray_withEmptyArrayInputThrowsIllegalArgumentException() {
    try {
      PairedStats.fromByteArray(new byte[0]);
      fail("Expected IllegalArgumentException");
    } catch (IllegalArgumentException expected) {
    }
  }

  public void testFromByteArray_withTooLongArrayInputThrowsIllegalArgumentException() {
    byte[] buffer = MANY_VALUES_PAIRED_STATS.toByteArray();
    byte[] tooLongByteArray =
        ByteBuffer.allocate(buffer.length + 2)
            .order(ByteOrder.LITTLE_ENDIAN)
            .put(buffer)
            .putChar('.')
            .array();
    try {
      PairedStats.fromByteArray(tooLongByteArray);
      fail("Expected IllegalArgumentException");
    } catch (IllegalArgumentException expected) {
    }
  }

  public void testFromByteArrayWithTooShortArrayInputThrowsIllegalArgumentException() {
    byte[] buffer = MANY_VALUES_PAIRED_STATS.toByteArray();
    byte[] tooShortByteArray =
        ByteBuffer.allocate(buffer.length - 1)
            .order(ByteOrder.LITTLE_ENDIAN)
            .put(buffer, 0, buffer.length - 1)
            .array();
    try {
      PairedStats.fromByteArray(tooShortByteArray);
      fail("Expected IllegalArgumentException");
    } catch (IllegalArgumentException expected) {
    }
  }
}
