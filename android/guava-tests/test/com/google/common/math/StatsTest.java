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
import static com.google.common.math.StatsTesting.ALL_STATS;
import static com.google.common.math.StatsTesting.EMPTY_STATS_ITERABLE;
import static com.google.common.math.StatsTesting.EMPTY_STATS_VARARGS;
import static com.google.common.math.StatsTesting.INTEGER_MANY_VALUES;
import static com.google.common.math.StatsTesting.INTEGER_MANY_VALUES_COUNT;
import static com.google.common.math.StatsTesting.INTEGER_MANY_VALUES_MAX;
import static com.google.common.math.StatsTesting.INTEGER_MANY_VALUES_MEAN;
import static com.google.common.math.StatsTesting.INTEGER_MANY_VALUES_MIN;
import static com.google.common.math.StatsTesting.INTEGER_MANY_VALUES_STATS_ITERABLE;
import static com.google.common.math.StatsTesting.INTEGER_MANY_VALUES_STATS_VARARGS;
import static com.google.common.math.StatsTesting.INTEGER_MANY_VALUES_SUM_OF_SQUARES_OF_DELTAS;
import static com.google.common.math.StatsTesting.LARGE_INTEGER_VALUES_MEAN;
import static com.google.common.math.StatsTesting.LARGE_INTEGER_VALUES_POPULATION_VARIANCE;
import static com.google.common.math.StatsTesting.LARGE_INTEGER_VALUES_STATS;
import static com.google.common.math.StatsTesting.LARGE_LONG_VALUES_MEAN;
import static com.google.common.math.StatsTesting.LARGE_LONG_VALUES_POPULATION_VARIANCE;
import static com.google.common.math.StatsTesting.LARGE_LONG_VALUES_STATS;
import static com.google.common.math.StatsTesting.LARGE_VALUES_MEAN;
import static com.google.common.math.StatsTesting.LARGE_VALUES_STATS;
import static com.google.common.math.StatsTesting.LONG_MANY_VALUES;
import static com.google.common.math.StatsTesting.LONG_MANY_VALUES_COUNT;
import static com.google.common.math.StatsTesting.LONG_MANY_VALUES_MAX;
import static com.google.common.math.StatsTesting.LONG_MANY_VALUES_MEAN;
import static com.google.common.math.StatsTesting.LONG_MANY_VALUES_MIN;
import static com.google.common.math.StatsTesting.LONG_MANY_VALUES_STATS_ITERATOR;
import static com.google.common.math.StatsTesting.LONG_MANY_VALUES_STATS_SNAPSHOT;
import static com.google.common.math.StatsTesting.LONG_MANY_VALUES_SUM_OF_SQUARES_OF_DELTAS;
import static com.google.common.math.StatsTesting.MANY_VALUES;
import static com.google.common.math.StatsTesting.MANY_VALUES_COUNT;
import static com.google.common.math.StatsTesting.MANY_VALUES_MAX;
import static com.google.common.math.StatsTesting.MANY_VALUES_MEAN;
import static com.google.common.math.StatsTesting.MANY_VALUES_MIN;
import static com.google.common.math.StatsTesting.MANY_VALUES_STATS_ITERABLE;
import static com.google.common.math.StatsTesting.MANY_VALUES_STATS_ITERATOR;
import static com.google.common.math.StatsTesting.MANY_VALUES_STATS_SNAPSHOT;
import static com.google.common.math.StatsTesting.MANY_VALUES_STATS_VARARGS;
import static com.google.common.math.StatsTesting.MANY_VALUES_SUM_OF_SQUARES_OF_DELTAS;
import static com.google.common.math.StatsTesting.ONE_VALUE;
import static com.google.common.math.StatsTesting.ONE_VALUE_STATS;
import static com.google.common.math.StatsTesting.TWO_VALUES;
import static com.google.common.math.StatsTesting.TWO_VALUES_MAX;
import static com.google.common.math.StatsTesting.TWO_VALUES_MEAN;
import static com.google.common.math.StatsTesting.TWO_VALUES_MIN;
import static com.google.common.math.StatsTesting.TWO_VALUES_STATS;
import static com.google.common.math.StatsTesting.TWO_VALUES_SUM_OF_SQUARES_OF_DELTAS;
import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;
import static java.lang.Double.NEGATIVE_INFINITY;
import static java.lang.Double.NaN;
import static java.lang.Double.POSITIVE_INFINITY;
import static java.lang.Math.sqrt;

import com.google.common.collect.ImmutableList;
import com.google.common.math.StatsTesting.ManyValues;
import com.google.common.primitives.Ints;
import com.google.common.primitives.Longs;
import com.google.common.testing.EqualsTester;
import com.google.common.testing.SerializableTester;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import junit.framework.TestCase;

/**
 * Tests for {@link Stats}. This tests instances created by both {@link Stats#of} and {@link
 * StatsAccumulator#snapshot}.
 *
 * @author Pete Gillin
 */
public class StatsTest extends TestCase {

  public void testCount() {
    assertThat(EMPTY_STATS_VARARGS.count()).isEqualTo(0);
    assertThat(EMPTY_STATS_ITERABLE.count()).isEqualTo(0);
    assertThat(ONE_VALUE_STATS.count()).isEqualTo(1);
    assertThat(TWO_VALUES_STATS.count()).isEqualTo(2);
    assertThat(MANY_VALUES_STATS_VARARGS.count()).isEqualTo(MANY_VALUES_COUNT);
    assertThat(MANY_VALUES_STATS_ITERABLE.count()).isEqualTo(MANY_VALUES_COUNT);
    assertThat(MANY_VALUES_STATS_ITERATOR.count()).isEqualTo(MANY_VALUES_COUNT);
    assertThat(MANY_VALUES_STATS_SNAPSHOT.count()).isEqualTo(MANY_VALUES_COUNT);
    assertThat(INTEGER_MANY_VALUES_STATS_VARARGS.count()).isEqualTo(INTEGER_MANY_VALUES_COUNT);
    assertThat(INTEGER_MANY_VALUES_STATS_ITERABLE.count()).isEqualTo(INTEGER_MANY_VALUES_COUNT);
    assertThat(LONG_MANY_VALUES_STATS_ITERATOR.count()).isEqualTo(LONG_MANY_VALUES_COUNT);
    assertThat(LONG_MANY_VALUES_STATS_SNAPSHOT.count()).isEqualTo(LONG_MANY_VALUES_COUNT);
  }

  public void testMean() {
    try {
      EMPTY_STATS_VARARGS.mean();
      fail("Expected IllegalStateException");
    } catch (IllegalStateException expected) {
    }
    try {
      EMPTY_STATS_ITERABLE.mean();
      fail("Expected IllegalStateException");
    } catch (IllegalStateException expected) {
    }
    assertThat(ONE_VALUE_STATS.mean()).isWithin(ALLOWED_ERROR).of(ONE_VALUE);
    assertThat(Stats.of(POSITIVE_INFINITY).mean()).isPositiveInfinity();
    assertThat(Stats.of(NEGATIVE_INFINITY).mean()).isNegativeInfinity();
    assertThat(Stats.of(NaN).mean()).isNaN();
    assertThat(TWO_VALUES_STATS.mean()).isWithin(ALLOWED_ERROR).of(TWO_VALUES_MEAN);
    // For datasets of many double values created from an array, we test many combinations of finite
    // and non-finite values:
    for (ManyValues values : ALL_MANY_VALUES) {
      double mean = Stats.of(values.asArray()).mean();
      if (values.hasAnyNaN()) {
        assertWithMessage("mean of " + values).that(mean).isNaN();
      } else if (values.hasAnyPositiveInfinity() && values.hasAnyNegativeInfinity()) {
        assertWithMessage("mean of " + values).that(mean).isNaN();
      } else if (values.hasAnyPositiveInfinity()) {
        assertWithMessage("mean of " + values).that(mean).isPositiveInfinity();
      } else if (values.hasAnyNegativeInfinity()) {
        assertWithMessage("mean of " + values).that(mean).isNegativeInfinity();
      } else {
        assertWithMessage("mean of " + values)
            .that(mean)
            .isWithin(ALLOWED_ERROR)
            .of(MANY_VALUES_MEAN);
      }
    }
    assertThat(MANY_VALUES_STATS_ITERABLE.mean()).isWithin(ALLOWED_ERROR).of(MANY_VALUES_MEAN);
    assertThat(MANY_VALUES_STATS_ITERATOR.mean()).isWithin(ALLOWED_ERROR).of(MANY_VALUES_MEAN);
    assertThat(MANY_VALUES_STATS_SNAPSHOT.mean()).isWithin(ALLOWED_ERROR).of(MANY_VALUES_MEAN);
    assertThat(LARGE_VALUES_STATS.mean())
        .isWithin(ALLOWED_ERROR * Double.MAX_VALUE)
        .of(LARGE_VALUES_MEAN);
    assertThat(INTEGER_MANY_VALUES_STATS_VARARGS.mean())
        .isWithin(ALLOWED_ERROR)
        .of(INTEGER_MANY_VALUES_MEAN);
    assertThat(INTEGER_MANY_VALUES_STATS_ITERABLE.mean())
        .isWithin(ALLOWED_ERROR)
        .of(INTEGER_MANY_VALUES_MEAN);
    assertThat(LARGE_INTEGER_VALUES_STATS.mean())
        .isWithin(ALLOWED_ERROR * Integer.MAX_VALUE)
        .of(LARGE_INTEGER_VALUES_MEAN);
    assertThat(LONG_MANY_VALUES_STATS_ITERATOR.mean())
        .isWithin(ALLOWED_ERROR)
        .of(LONG_MANY_VALUES_MEAN);
    assertThat(LONG_MANY_VALUES_STATS_SNAPSHOT.mean())
        .isWithin(ALLOWED_ERROR)
        .of(LONG_MANY_VALUES_MEAN);
    assertThat(LARGE_LONG_VALUES_STATS.mean())
        .isWithin(ALLOWED_ERROR * Long.MAX_VALUE)
        .of(LARGE_LONG_VALUES_MEAN);
  }

  public void testSum() {
    assertThat(EMPTY_STATS_VARARGS.sum()).isEqualTo(0.0);
    assertThat(EMPTY_STATS_ITERABLE.sum()).isEqualTo(0.0);
    assertThat(ONE_VALUE_STATS.sum()).isWithin(ALLOWED_ERROR).of(ONE_VALUE);
    assertThat(TWO_VALUES_STATS.sum()).isWithin(ALLOWED_ERROR).of(TWO_VALUES_MEAN * 2);
    assertThat(MANY_VALUES_STATS_VARARGS.sum())
        .isWithin(ALLOWED_ERROR)
        .of(MANY_VALUES_MEAN * MANY_VALUES_COUNT);
    assertThat(MANY_VALUES_STATS_ITERABLE.sum())
        .isWithin(ALLOWED_ERROR)
        .of(MANY_VALUES_MEAN * MANY_VALUES_COUNT);
    assertThat(MANY_VALUES_STATS_ITERATOR.sum())
        .isWithin(ALLOWED_ERROR)
        .of(MANY_VALUES_MEAN * MANY_VALUES_COUNT);
    assertThat(MANY_VALUES_STATS_SNAPSHOT.sum())
        .isWithin(ALLOWED_ERROR)
        .of(MANY_VALUES_MEAN * MANY_VALUES_COUNT);
    assertThat(INTEGER_MANY_VALUES_STATS_VARARGS.sum())
        .isWithin(ALLOWED_ERROR)
        .of(INTEGER_MANY_VALUES_MEAN * INTEGER_MANY_VALUES_COUNT);
    assertThat(INTEGER_MANY_VALUES_STATS_ITERABLE.sum())
        .isWithin(ALLOWED_ERROR)
        .of(INTEGER_MANY_VALUES_MEAN * INTEGER_MANY_VALUES_COUNT);
    assertThat(LONG_MANY_VALUES_STATS_ITERATOR.sum())
        .isWithin(ALLOWED_ERROR)
        .of(LONG_MANY_VALUES_MEAN * LONG_MANY_VALUES_COUNT);
    assertThat(LONG_MANY_VALUES_STATS_SNAPSHOT.sum())
        .isWithin(ALLOWED_ERROR)
        .of(LONG_MANY_VALUES_MEAN * LONG_MANY_VALUES_COUNT);
  }

  public void testPopulationVariance() {
    try {
      EMPTY_STATS_VARARGS.populationVariance();
      fail("Expected IllegalStateException");
    } catch (IllegalStateException expected) {
    }
    try {
      EMPTY_STATS_ITERABLE.populationVariance();
      fail("Expected IllegalStateException");
    } catch (IllegalStateException expected) {
    }
    assertThat(ONE_VALUE_STATS.populationVariance()).isWithin(0.0).of(0.0);
    assertThat(Stats.of(POSITIVE_INFINITY).populationVariance()).isNaN();
    assertThat(Stats.of(NEGATIVE_INFINITY).populationVariance()).isNaN();
    assertThat(Stats.of(NaN).populationVariance()).isNaN();
    assertThat(TWO_VALUES_STATS.populationVariance())
        .isWithin(ALLOWED_ERROR)
        .of(TWO_VALUES_SUM_OF_SQUARES_OF_DELTAS / 2);
    assertThat(MANY_VALUES_STATS_VARARGS.populationVariance())
        .isWithin(ALLOWED_ERROR)
        .of(MANY_VALUES_SUM_OF_SQUARES_OF_DELTAS / MANY_VALUES_COUNT);
    // For datasets of many double values created from an iterable, we test many combinations of
    // finite and non-finite values:
    for (ManyValues values : ALL_MANY_VALUES) {
      double populationVariance = Stats.of(values.asIterable()).populationVariance();
      if (values.hasAnyNonFinite()) {
        assertWithMessage("population variance of " + values).that(populationVariance).isNaN();
      } else {
        assertWithMessage("population variance of " + values)
            .that(populationVariance)
            .isWithin(ALLOWED_ERROR)
            .of(MANY_VALUES_SUM_OF_SQUARES_OF_DELTAS / MANY_VALUES_COUNT);
      }
    }
    assertThat(MANY_VALUES_STATS_ITERATOR.populationVariance())
        .isWithin(ALLOWED_ERROR)
        .of(MANY_VALUES_SUM_OF_SQUARES_OF_DELTAS / MANY_VALUES_COUNT);
    assertThat(MANY_VALUES_STATS_SNAPSHOT.populationVariance())
        .isWithin(ALLOWED_ERROR)
        .of(MANY_VALUES_SUM_OF_SQUARES_OF_DELTAS / MANY_VALUES_COUNT);
    assertThat(INTEGER_MANY_VALUES_STATS_VARARGS.populationVariance())
        .isWithin(ALLOWED_ERROR)
        .of(INTEGER_MANY_VALUES_SUM_OF_SQUARES_OF_DELTAS / INTEGER_MANY_VALUES_COUNT);
    assertThat(INTEGER_MANY_VALUES_STATS_ITERABLE.populationVariance())
        .isWithin(ALLOWED_ERROR)
        .of(INTEGER_MANY_VALUES_SUM_OF_SQUARES_OF_DELTAS / INTEGER_MANY_VALUES_COUNT);
    assertThat(LARGE_INTEGER_VALUES_STATS.populationVariance())
        .isWithin(ALLOWED_ERROR * Integer.MAX_VALUE * Integer.MAX_VALUE)
        .of(LARGE_INTEGER_VALUES_POPULATION_VARIANCE);
    assertThat(LONG_MANY_VALUES_STATS_ITERATOR.populationVariance())
        .isWithin(ALLOWED_ERROR)
        .of(LONG_MANY_VALUES_SUM_OF_SQUARES_OF_DELTAS / LONG_MANY_VALUES_COUNT);
    assertThat(LONG_MANY_VALUES_STATS_SNAPSHOT.populationVariance())
        .isWithin(ALLOWED_ERROR)
        .of(LONG_MANY_VALUES_SUM_OF_SQUARES_OF_DELTAS / LONG_MANY_VALUES_COUNT);
    assertThat(LARGE_LONG_VALUES_STATS.populationVariance())
        .isWithin(ALLOWED_ERROR * Long.MAX_VALUE * Long.MAX_VALUE)
        .of(LARGE_LONG_VALUES_POPULATION_VARIANCE);
  }

  public void testPopulationStandardDeviation() {
    try {
      EMPTY_STATS_VARARGS.populationStandardDeviation();
      fail("Expected IllegalStateException");
    } catch (IllegalStateException expected) {
    }
    try {
      EMPTY_STATS_ITERABLE.populationStandardDeviation();
      fail("Expected IllegalStateException");
    } catch (IllegalStateException expected) {
    }
    assertThat(ONE_VALUE_STATS.populationStandardDeviation()).isWithin(0.0).of(0.0);
    assertThat(TWO_VALUES_STATS.populationStandardDeviation())
        .isWithin(ALLOWED_ERROR)
        .of(sqrt(TWO_VALUES_SUM_OF_SQUARES_OF_DELTAS / 2));
    assertThat(MANY_VALUES_STATS_VARARGS.populationStandardDeviation())
        .isWithin(ALLOWED_ERROR)
        .of(sqrt(MANY_VALUES_SUM_OF_SQUARES_OF_DELTAS / MANY_VALUES_COUNT));
    assertThat(MANY_VALUES_STATS_ITERABLE.populationStandardDeviation())
        .isWithin(ALLOWED_ERROR)
        .of(sqrt(MANY_VALUES_SUM_OF_SQUARES_OF_DELTAS / MANY_VALUES_COUNT));
    assertThat(MANY_VALUES_STATS_ITERATOR.populationStandardDeviation())
        .isWithin(ALLOWED_ERROR)
        .of(sqrt(MANY_VALUES_SUM_OF_SQUARES_OF_DELTAS / MANY_VALUES_COUNT));
    assertThat(MANY_VALUES_STATS_SNAPSHOT.populationStandardDeviation())
        .isWithin(ALLOWED_ERROR)
        .of(sqrt(MANY_VALUES_SUM_OF_SQUARES_OF_DELTAS / MANY_VALUES_COUNT));
    assertThat(INTEGER_MANY_VALUES_STATS_VARARGS.populationStandardDeviation())
        .isWithin(ALLOWED_ERROR)
        .of(sqrt(INTEGER_MANY_VALUES_SUM_OF_SQUARES_OF_DELTAS / INTEGER_MANY_VALUES_COUNT));
    assertThat(INTEGER_MANY_VALUES_STATS_ITERABLE.populationStandardDeviation())
        .isWithin(ALLOWED_ERROR)
        .of(sqrt(INTEGER_MANY_VALUES_SUM_OF_SQUARES_OF_DELTAS / INTEGER_MANY_VALUES_COUNT));
    assertThat(LONG_MANY_VALUES_STATS_ITERATOR.populationStandardDeviation())
        .isWithin(ALLOWED_ERROR)
        .of(sqrt(LONG_MANY_VALUES_SUM_OF_SQUARES_OF_DELTAS / LONG_MANY_VALUES_COUNT));
    assertThat(LONG_MANY_VALUES_STATS_SNAPSHOT.populationStandardDeviation())
        .isWithin(ALLOWED_ERROR)
        .of(sqrt(LONG_MANY_VALUES_SUM_OF_SQUARES_OF_DELTAS / LONG_MANY_VALUES_COUNT));
  }

  public void testSampleVariance() {
    try {
      EMPTY_STATS_VARARGS.sampleVariance();
      fail("Expected IllegalStateException");
    } catch (IllegalStateException expected) {
    }
    try {
      EMPTY_STATS_ITERABLE.sampleVariance();
      fail("Expected IllegalStateException");
    } catch (IllegalStateException expected) {
    }
    try {
      ONE_VALUE_STATS.sampleVariance();
      fail("Expected IllegalStateException");
    } catch (IllegalStateException expected) {
    }
    assertThat(TWO_VALUES_STATS.sampleVariance())
        .isWithin(ALLOWED_ERROR)
        .of(TWO_VALUES_SUM_OF_SQUARES_OF_DELTAS);
    assertThat(MANY_VALUES_STATS_VARARGS.sampleVariance())
        .isWithin(ALLOWED_ERROR)
        .of(MANY_VALUES_SUM_OF_SQUARES_OF_DELTAS / (MANY_VALUES_COUNT - 1));
    assertThat(MANY_VALUES_STATS_ITERABLE.sampleVariance())
        .isWithin(ALLOWED_ERROR)
        .of(MANY_VALUES_SUM_OF_SQUARES_OF_DELTAS / (MANY_VALUES_COUNT - 1));
    assertThat(MANY_VALUES_STATS_ITERATOR.sampleVariance())
        .isWithin(ALLOWED_ERROR)
        .of(MANY_VALUES_SUM_OF_SQUARES_OF_DELTAS / (MANY_VALUES_COUNT - 1));
    assertThat(MANY_VALUES_STATS_SNAPSHOT.sampleVariance())
        .isWithin(ALLOWED_ERROR)
        .of(MANY_VALUES_SUM_OF_SQUARES_OF_DELTAS / (MANY_VALUES_COUNT - 1));
    assertThat(INTEGER_MANY_VALUES_STATS_VARARGS.sampleVariance())
        .isWithin(ALLOWED_ERROR)
        .of(INTEGER_MANY_VALUES_SUM_OF_SQUARES_OF_DELTAS / (INTEGER_MANY_VALUES_COUNT - 1));
    assertThat(INTEGER_MANY_VALUES_STATS_ITERABLE.sampleVariance())
        .isWithin(ALLOWED_ERROR)
        .of(INTEGER_MANY_VALUES_SUM_OF_SQUARES_OF_DELTAS / (INTEGER_MANY_VALUES_COUNT - 1));
    assertThat(LONG_MANY_VALUES_STATS_ITERATOR.sampleVariance())
        .isWithin(ALLOWED_ERROR)
        .of(LONG_MANY_VALUES_SUM_OF_SQUARES_OF_DELTAS / (LONG_MANY_VALUES_COUNT - 1));
    assertThat(LONG_MANY_VALUES_STATS_SNAPSHOT.sampleVariance())
        .isWithin(ALLOWED_ERROR)
        .of(LONG_MANY_VALUES_SUM_OF_SQUARES_OF_DELTAS / (LONG_MANY_VALUES_COUNT - 1));
  }

  public void testSampleStandardDeviation() {
    try {
      EMPTY_STATS_VARARGS.sampleStandardDeviation();
      fail("Expected IllegalStateException");
    } catch (IllegalStateException expected) {
    }
    try {
      EMPTY_STATS_ITERABLE.sampleStandardDeviation();
      fail("Expected IllegalStateException");
    } catch (IllegalStateException expected) {
    }
    try {
      ONE_VALUE_STATS.sampleStandardDeviation();
      fail("Expected IllegalStateException");
    } catch (IllegalStateException expected) {
    }
    assertThat(TWO_VALUES_STATS.sampleStandardDeviation())
        .isWithin(ALLOWED_ERROR)
        .of(sqrt(TWO_VALUES_SUM_OF_SQUARES_OF_DELTAS));
    assertThat(MANY_VALUES_STATS_VARARGS.sampleStandardDeviation())
        .isWithin(ALLOWED_ERROR)
        .of(sqrt(MANY_VALUES_SUM_OF_SQUARES_OF_DELTAS / (MANY_VALUES_COUNT - 1)));
    assertThat(MANY_VALUES_STATS_ITERABLE.sampleStandardDeviation())
        .isWithin(ALLOWED_ERROR)
        .of(sqrt(MANY_VALUES_SUM_OF_SQUARES_OF_DELTAS / (MANY_VALUES_COUNT - 1)));
    assertThat(MANY_VALUES_STATS_ITERATOR.sampleStandardDeviation())
        .isWithin(ALLOWED_ERROR)
        .of(sqrt(MANY_VALUES_SUM_OF_SQUARES_OF_DELTAS / (MANY_VALUES_COUNT - 1)));
    assertThat(MANY_VALUES_STATS_SNAPSHOT.sampleStandardDeviation())
        .isWithin(ALLOWED_ERROR)
        .of(sqrt(MANY_VALUES_SUM_OF_SQUARES_OF_DELTAS / (MANY_VALUES_COUNT - 1)));
    assertThat(INTEGER_MANY_VALUES_STATS_VARARGS.sampleStandardDeviation())
        .isWithin(ALLOWED_ERROR)
        .of(sqrt(INTEGER_MANY_VALUES_SUM_OF_SQUARES_OF_DELTAS / (INTEGER_MANY_VALUES_COUNT - 1)));
    assertThat(INTEGER_MANY_VALUES_STATS_ITERABLE.sampleStandardDeviation())
        .isWithin(ALLOWED_ERROR)
        .of(sqrt(INTEGER_MANY_VALUES_SUM_OF_SQUARES_OF_DELTAS / (INTEGER_MANY_VALUES_COUNT - 1)));
    assertThat(LONG_MANY_VALUES_STATS_ITERATOR.sampleStandardDeviation())
        .isWithin(ALLOWED_ERROR)
        .of(sqrt(LONG_MANY_VALUES_SUM_OF_SQUARES_OF_DELTAS / (LONG_MANY_VALUES_COUNT - 1)));
    assertThat(LONG_MANY_VALUES_STATS_SNAPSHOT.sampleStandardDeviation())
        .isWithin(ALLOWED_ERROR)
        .of(sqrt(LONG_MANY_VALUES_SUM_OF_SQUARES_OF_DELTAS / (LONG_MANY_VALUES_COUNT - 1)));
  }

  public void testMax() {
    try {
      EMPTY_STATS_VARARGS.max();
      fail("Expected IllegalStateException");
    } catch (IllegalStateException expected) {
    }
    try {
      EMPTY_STATS_ITERABLE.max();
      fail("Expected IllegalStateException");
    } catch (IllegalStateException expected) {
    }
    assertThat(ONE_VALUE_STATS.max()).isWithin(ALLOWED_ERROR).of(ONE_VALUE);
    assertThat(Stats.of(POSITIVE_INFINITY).max()).isPositiveInfinity();
    assertThat(Stats.of(NEGATIVE_INFINITY).max()).isNegativeInfinity();
    assertThat(Stats.of(NaN).max()).isNaN();
    assertThat(TWO_VALUES_STATS.max()).isWithin(ALLOWED_ERROR).of(TWO_VALUES_MAX);
    assertThat(MANY_VALUES_STATS_VARARGS.max()).isWithin(ALLOWED_ERROR).of(MANY_VALUES_MAX);
    assertThat(MANY_VALUES_STATS_ITERABLE.max()).isWithin(ALLOWED_ERROR).of(MANY_VALUES_MAX);
    // For datasets of many double values created from an iterator, we test many combinations of
    // finite and non-finite values:
    for (ManyValues values : ALL_MANY_VALUES) {
      double max = Stats.of(values.asIterable().iterator()).max();
      if (values.hasAnyNaN()) {
        assertWithMessage("max of " + values).that(max).isNaN();
      } else if (values.hasAnyPositiveInfinity()) {
        assertWithMessage("max of " + values).that(max).isPositiveInfinity();
      } else {
        assertWithMessage("max of " + values).that(max).isWithin(ALLOWED_ERROR).of(MANY_VALUES_MAX);
      }
    }
    assertThat(MANY_VALUES_STATS_SNAPSHOT.max()).isWithin(ALLOWED_ERROR).of(MANY_VALUES_MAX);
    assertThat(INTEGER_MANY_VALUES_STATS_VARARGS.max())
        .isWithin(ALLOWED_ERROR)
        .of(INTEGER_MANY_VALUES_MAX);
    assertThat(INTEGER_MANY_VALUES_STATS_ITERABLE.max())
        .isWithin(ALLOWED_ERROR)
        .of(INTEGER_MANY_VALUES_MAX);
    assertThat(LONG_MANY_VALUES_STATS_ITERATOR.max())
        .isWithin(ALLOWED_ERROR)
        .of(LONG_MANY_VALUES_MAX);
    assertThat(LONG_MANY_VALUES_STATS_SNAPSHOT.max())
        .isWithin(ALLOWED_ERROR)
        .of(LONG_MANY_VALUES_MAX);
  }

  public void testMin() {
    try {
      EMPTY_STATS_VARARGS.min();
      fail("Expected IllegalStateException");
    } catch (IllegalStateException expected) {
    }
    try {
      EMPTY_STATS_ITERABLE.min();
      fail("Expected IllegalStateException");
    } catch (IllegalStateException expected) {
    }
    assertThat(ONE_VALUE_STATS.min()).isWithin(ALLOWED_ERROR).of(ONE_VALUE);
    assertThat(Stats.of(POSITIVE_INFINITY).min()).isPositiveInfinity();
    assertThat(Stats.of(NEGATIVE_INFINITY).min()).isNegativeInfinity();
    assertThat(Stats.of(NaN).min()).isNaN();
    assertThat(TWO_VALUES_STATS.min()).isWithin(ALLOWED_ERROR).of(TWO_VALUES_MIN);
    assertThat(MANY_VALUES_STATS_VARARGS.min()).isWithin(ALLOWED_ERROR).of(MANY_VALUES_MIN);
    assertThat(MANY_VALUES_STATS_ITERABLE.min()).isWithin(ALLOWED_ERROR).of(MANY_VALUES_MIN);
    assertThat(MANY_VALUES_STATS_ITERATOR.min()).isWithin(ALLOWED_ERROR).of(MANY_VALUES_MIN);
    // For datasets of many double values created from an accumulator snapshot, we test many
    // combinations of finite and non-finite values:
    for (ManyValues values : ALL_MANY_VALUES) {
      StatsAccumulator accumulator = new StatsAccumulator();
      accumulator.addAll(values.asIterable());
      double min = accumulator.snapshot().min();
      if (values.hasAnyNaN()) {
        assertWithMessage("min of " + values).that(min).isNaN();
      } else if (values.hasAnyNegativeInfinity()) {
        assertWithMessage("min of " + values).that(min).isNegativeInfinity();
      } else {
        assertWithMessage("min of " + values).that(min).isWithin(ALLOWED_ERROR).of(MANY_VALUES_MIN);
      }
    }
    assertThat(INTEGER_MANY_VALUES_STATS_VARARGS.min())
        .isWithin(ALLOWED_ERROR)
        .of(INTEGER_MANY_VALUES_MIN);
    assertThat(INTEGER_MANY_VALUES_STATS_ITERABLE.min())
        .isWithin(ALLOWED_ERROR)
        .of(INTEGER_MANY_VALUES_MIN);
    assertThat(LONG_MANY_VALUES_STATS_ITERATOR.min())
        .isWithin(ALLOWED_ERROR)
        .of(LONG_MANY_VALUES_MIN);
    assertThat(LONG_MANY_VALUES_STATS_SNAPSHOT.min())
        .isWithin(ALLOWED_ERROR)
        .of(LONG_MANY_VALUES_MIN);
  }

  public void testEqualsAndHashCode() {
    new EqualsTester()
        .addEqualityGroup(
            Stats.of(1.0, 1.0, 5.0, 5.0),
            Stats.of(1.0, 1.0, 5.0, 5.0),
            Stats.of(ImmutableList.of(1.0, 1.0, 5.0, 5.0)),
            Stats.of(ImmutableList.of(1.0, 1.0, 5.0, 5.0).iterator()),
            SerializableTester.reserialize(Stats.of(1.0, 1.0, 5.0, 5.0)))
        .addEqualityGroup(Stats.of(1.0, 5.0))
        .addEqualityGroup(Stats.of(1.0, 5.0, 1.0, 6.0))
        .addEqualityGroup(Stats.of(2.0, 6.0, 2.0, 6.0))
        .addEqualityGroup(
            new Stats(5, -5.5, 55.5, -5.55, 5.55), new Stats(5, -5.5, 55.5, -5.55, 5.55))
        .addEqualityGroup(new Stats(6, -5.5, 55.5, -5.55, 5.55))
        .addEqualityGroup(new Stats(5, -5.6, 55.5, -5.55, 5.55))
        .addEqualityGroup(new Stats(5, -5.5, 55.6, -5.55, 5.55))
        .addEqualityGroup(new Stats(5, -5.5, 55.5, -5.56, 5.55))
        .addEqualityGroup(new Stats(5, -5.5, 55.5, -5.55, 5.56))
        .testEquals();
  }

  public void testSerializable() {
    SerializableTester.reserializeAndAssert(MANY_VALUES_STATS_ITERABLE);
  }

  public void testToString() {
    assertThat(EMPTY_STATS_VARARGS.toString()).isEqualTo("Stats{count=0}");
    assertThat(MANY_VALUES_STATS_ITERABLE.toString())
        .isEqualTo(
            "Stats{count="
                + MANY_VALUES_STATS_ITERABLE.count()
                + ", mean="
                + MANY_VALUES_STATS_ITERABLE.mean()
                + ", populationStandardDeviation="
                + MANY_VALUES_STATS_ITERABLE.populationStandardDeviation()
                + ", min="
                + MANY_VALUES_STATS_ITERABLE.min()
                + ", max="
                + MANY_VALUES_STATS_ITERABLE.max()
                + "}");
  }

  public void testMeanOf() {
    try {
      Stats.meanOf();
      fail("Expected IllegalArgumentException");
    } catch (IllegalArgumentException expected) {
    }
    try {
      Stats.meanOf(ImmutableList.<Number>of());
      fail("Expected IllegalArgumentException");
    } catch (IllegalArgumentException expected) {
    }
    assertThat(Stats.meanOf(ONE_VALUE)).isWithin(ALLOWED_ERROR).of(ONE_VALUE);
    assertThat(Stats.meanOf(POSITIVE_INFINITY)).isPositiveInfinity();
    assertThat(Stats.meanOf(NEGATIVE_INFINITY)).isNegativeInfinity();
    assertThat(Stats.meanOf(NaN)).isNaN();
    assertThat(Stats.meanOf(TWO_VALUES)).isWithin(ALLOWED_ERROR).of(TWO_VALUES_MEAN);
    // For datasets of many double values created from an array, we test many combinations of finite
    // and non-finite values:
    for (ManyValues values : ALL_MANY_VALUES) {
      double mean = Stats.meanOf(values.asArray());
      if (values.hasAnyNaN()) {
        assertWithMessage("mean of " + values).that(mean).isNaN();
      } else if (values.hasAnyPositiveInfinity() && values.hasAnyNegativeInfinity()) {
        assertWithMessage("mean of " + values).that(mean).isNaN();
      } else if (values.hasAnyPositiveInfinity()) {
        assertWithMessage("mean of " + values).that(mean).isPositiveInfinity();
      } else if (values.hasAnyNegativeInfinity()) {
        assertWithMessage("mean of " + values).that(mean).isNegativeInfinity();
      } else {
        assertWithMessage("mean of " + values)
            .that(mean)
            .isWithin(ALLOWED_ERROR)
            .of(MANY_VALUES_MEAN);
      }
    }
    assertThat(Stats.meanOf(MANY_VALUES)).isWithin(ALLOWED_ERROR).of(MANY_VALUES_MEAN);
    assertThat(Stats.meanOf(MANY_VALUES.iterator())).isWithin(ALLOWED_ERROR).of(MANY_VALUES_MEAN);
    assertThat(Stats.meanOf(INTEGER_MANY_VALUES))
        .isWithin(ALLOWED_ERROR)
        .of(INTEGER_MANY_VALUES_MEAN);
    assertThat(Stats.meanOf(Ints.toArray(INTEGER_MANY_VALUES)))
        .isWithin(ALLOWED_ERROR)
        .of(INTEGER_MANY_VALUES_MEAN);
    assertThat(Stats.meanOf(LONG_MANY_VALUES)).isWithin(ALLOWED_ERROR).of(LONG_MANY_VALUES_MEAN);
    assertThat(Stats.meanOf(Longs.toArray(LONG_MANY_VALUES)))
        .isWithin(ALLOWED_ERROR)
        .of(LONG_MANY_VALUES_MEAN);
  }

  public void testToByteArrayAndFromByteArrayRoundTrip() {
    for (Stats stats : ALL_STATS) {
      byte[] statsByteArray = stats.toByteArray();

      // Round trip to byte array and back
      assertThat(Stats.fromByteArray(statsByteArray)).isEqualTo(stats);
    }
  }

  public void testFromByteArray_withNullInputThrowsNullPointerException() {
    try {
      Stats.fromByteArray(null);
      fail("Expected NullPointerException");
    } catch (NullPointerException expected) {
    }
  }

  public void testFromByteArray_withEmptyArrayInputThrowsIllegalArgumentException() {
    try {
      Stats.fromByteArray(new byte[0]);
      fail("Expected IllegalArgumentException");
    } catch (IllegalArgumentException expected) {
    }
  }

  public void testFromByteArray_withTooLongArrayInputThrowsIllegalArgumentException() {
    byte[] buffer = MANY_VALUES_STATS_VARARGS.toByteArray();
    byte[] tooLongByteArray =
        ByteBuffer.allocate(buffer.length + 2)
            .order(ByteOrder.LITTLE_ENDIAN)
            .put(buffer)
            .putChar('.')
            .array();
    try {
      Stats.fromByteArray(tooLongByteArray);
      fail("Expected IllegalArgumentException");
    } catch (IllegalArgumentException expected) {
    }
  }

  public void testFromByteArrayWithTooShortArrayInputThrowsIllegalArgumentException() {
    byte[] buffer = MANY_VALUES_STATS_VARARGS.toByteArray();
    byte[] tooShortByteArray =
        ByteBuffer.allocate(buffer.length - 1)
            .order(ByteOrder.LITTLE_ENDIAN)
            .put(buffer, 0, Stats.BYTES - 1)
            .array();
    try {
      Stats.fromByteArray(tooShortByteArray);
      fail("Expected IllegalArgumentException");
    } catch (IllegalArgumentException expected) {
    }
  }
}
