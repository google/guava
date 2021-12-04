/*
 * Copyright (C) 2013 The Guava Authors
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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.truth.Truth.assertThat;
import static java.lang.Double.NEGATIVE_INFINITY;
import static java.lang.Double.NaN;
import static java.lang.Double.POSITIVE_INFINITY;
import static org.junit.Assert.fail;

import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.primitives.Doubles;
import com.google.common.primitives.Ints;
import java.math.BigInteger;
import java.util.List;
import java.util.stream.DoubleStream;

/**
 * Inputs, expected outputs, and helper methods for tests of {@link StatsAccumulator}, {@link
 * Stats}, {@link PairedStatsAccumulator}, and {@link PairedStats}.
 *
 * @author Pete Gillin
 */
class StatsTesting {

  static final double ALLOWED_ERROR = 1e-10;

  // Inputs and their statistics:

  static final double ONE_VALUE = 12.34;

  static final double OTHER_ONE_VALUE = -56.78;

  static final ImmutableList<Double> TWO_VALUES = ImmutableList.of(12.34, -56.78);
  static final double TWO_VALUES_MEAN = (12.34 - 56.78) / 2;
  static final double TWO_VALUES_SUM_OF_SQUARES_OF_DELTAS =
      (12.34 - TWO_VALUES_MEAN) * (12.34 - TWO_VALUES_MEAN)
          + (-56.78 - TWO_VALUES_MEAN) * (-56.78 - TWO_VALUES_MEAN);
  static final double TWO_VALUES_MAX = 12.34;
  static final double TWO_VALUES_MIN = -56.78;

  static final ImmutableList<Double> OTHER_TWO_VALUES = ImmutableList.of(123.456, -789.012);
  static final double OTHER_TWO_VALUES_MEAN = (123.456 - 789.012) / 2;
  static final double TWO_VALUES_SUM_OF_PRODUCTS_OF_DELTAS =
      (12.34 - TWO_VALUES_MEAN) * (123.456 - OTHER_TWO_VALUES_MEAN)
          + (-56.78 - TWO_VALUES_MEAN) * (-789.012 - OTHER_TWO_VALUES_MEAN);

  /**
   * Helper class for testing with non-finite values. {@link #ALL_MANY_VALUES} gives a number
   * instances with many combinations of finite and non-finite values. All have {@link
   * #MANY_VALUES_COUNT} values. If all the values are finite then the mean is {@link
   * #MANY_VALUES_MEAN} and the sum-of-squares-of-deltas is {@link
   * #MANY_VALUES_SUM_OF_SQUARES_OF_DELTAS}. The smallest and largest finite values are always
   * {@link #MANY_VALUES_MIN} and {@link #MANY_VALUES_MAX}, although setting non-finite values will
   * change the true min and max.
   */
  static class ManyValues {

    private final ImmutableList<Double> values;

    ManyValues(double[] values) {
      this.values = ImmutableList.copyOf(Doubles.asList(values));
    }

    ImmutableList<Double> asIterable() {
      return values;
    }

    double[] asArray() {
      return Doubles.toArray(values);
    }

    boolean hasAnyPositiveInfinity() {
      return Iterables.any(values, Predicates.equalTo(POSITIVE_INFINITY));
    }

    boolean hasAnyNegativeInfinity() {
      return Iterables.any(values, Predicates.equalTo(NEGATIVE_INFINITY));
    }

    boolean hasAnyNaN() {
      return Iterables.any(values, Predicates.equalTo(NaN));
    }

    boolean hasAnyNonFinite() {
      return hasAnyPositiveInfinity() || hasAnyNegativeInfinity() || hasAnyNaN();
    }

    @Override
    public String toString() {
      return values.toString();
    }

    private static ImmutableList<ManyValues> createAll() {
      ImmutableList.Builder<ManyValues> builder = ImmutableList.builder();
      double[] values = new double[5];
      for (double first : ImmutableList.of(1.1, POSITIVE_INFINITY, NEGATIVE_INFINITY, NaN)) {
        values[0] = first;
        values[1] = -44.44;
        for (double third : ImmutableList.of(33.33, POSITIVE_INFINITY, NEGATIVE_INFINITY, NaN)) {
          values[2] = third;
          values[3] = 555.555;
          for (double fifth : ImmutableList.of(-2.2, POSITIVE_INFINITY, NEGATIVE_INFINITY, NaN)) {
            values[4] = fifth;
            builder.add(new ManyValues(values));
          }
        }
      }
      return builder.build();
    }
  }

  static final ImmutableList<ManyValues> ALL_MANY_VALUES = ManyValues.createAll();

  static final ImmutableList<Double> MANY_VALUES =
      ImmutableList.of(1.1, -44.44, 33.33, 555.555, -2.2);
  static final int MANY_VALUES_COUNT = 5;
  static final double MANY_VALUES_MEAN = (1.1 - 44.44 + 33.33 + 555.555 - 2.2) / 5;
  static final double MANY_VALUES_SUM_OF_SQUARES_OF_DELTAS =
      (1.1 - MANY_VALUES_MEAN) * (1.1 - MANY_VALUES_MEAN)
          + (-44.44 - MANY_VALUES_MEAN) * (-44.44 - MANY_VALUES_MEAN)
          + (33.33 - MANY_VALUES_MEAN) * (33.33 - MANY_VALUES_MEAN)
          + (555.555 - MANY_VALUES_MEAN) * (555.555 - MANY_VALUES_MEAN)
          + (-2.2 - MANY_VALUES_MEAN) * (-2.2 - MANY_VALUES_MEAN);
  static final double MANY_VALUES_MAX = 555.555;
  static final double MANY_VALUES_MIN = -44.44;

  // Doubles which will overflow if summed:
  static final double[] LARGE_VALUES = {Double.MAX_VALUE, Double.MAX_VALUE / 2.0};
  static final double LARGE_VALUES_MEAN = 0.75 * Double.MAX_VALUE;

  static final ImmutableList<Double> OTHER_MANY_VALUES =
      ImmutableList.of(1.11, -2.22, 33.3333, -44.4444, 555.555555);
  static final int OTHER_MANY_VALUES_COUNT = 5;
  static final double OTHER_MANY_VALUES_MEAN = (1.11 - 2.22 + 33.3333 - 44.4444 + 555.555555) / 5;

  static final double MANY_VALUES_SUM_OF_PRODUCTS_OF_DELTAS =
      (1.1 - MANY_VALUES_MEAN) * (1.11 - OTHER_MANY_VALUES_MEAN)
          + (-44.44 - MANY_VALUES_MEAN) * (-2.22 - OTHER_MANY_VALUES_MEAN)
          + (33.33 - MANY_VALUES_MEAN) * (33.3333 - OTHER_MANY_VALUES_MEAN)
          + (555.555 - MANY_VALUES_MEAN) * (-44.4444 - OTHER_MANY_VALUES_MEAN)
          + (-2.2 - MANY_VALUES_MEAN) * (555.555555 - OTHER_MANY_VALUES_MEAN);

  static final ImmutableList<Integer> INTEGER_MANY_VALUES =
      ImmutableList.of(11, -22, 3333, -4444, 555555);
  static final int INTEGER_MANY_VALUES_COUNT = 5;
  static final double INTEGER_MANY_VALUES_MEAN = (11.0 - 22.0 + 3333.0 - 4444.0 + 555555.0) / 5;
  static final double INTEGER_MANY_VALUES_SUM_OF_SQUARES_OF_DELTAS =
      (11.0 - INTEGER_MANY_VALUES_MEAN) * (11.0 - INTEGER_MANY_VALUES_MEAN)
          + (-22.0 - INTEGER_MANY_VALUES_MEAN) * (-22.0 - INTEGER_MANY_VALUES_MEAN)
          + (3333.0 - INTEGER_MANY_VALUES_MEAN) * (3333.0 - INTEGER_MANY_VALUES_MEAN)
          + (-4444.0 - INTEGER_MANY_VALUES_MEAN) * (-4444.0 - INTEGER_MANY_VALUES_MEAN)
          + (555555.0 - INTEGER_MANY_VALUES_MEAN) * (555555.0 - INTEGER_MANY_VALUES_MEAN);
  static final double INTEGER_MANY_VALUES_MAX = 555555.0;
  static final double INTEGER_MANY_VALUES_MIN = -4444.0;

  // Integers which will overflow if summed (using integer arithmetic):
  static final int[] LARGE_INTEGER_VALUES = {Integer.MAX_VALUE, Integer.MAX_VALUE / 2};
  static final double LARGE_INTEGER_VALUES_MEAN =
      BigInteger.valueOf(Integer.MAX_VALUE)
          .multiply(BigInteger.valueOf(3L))
          .divide(BigInteger.valueOf(4L))
          .doubleValue();
  static final double LARGE_INTEGER_VALUES_POPULATION_VARIANCE =
      BigInteger.valueOf(Integer.MAX_VALUE)
          .multiply(BigInteger.valueOf(Integer.MAX_VALUE))
          .divide(BigInteger.valueOf(16L))
          .doubleValue();

  static final ImmutableList<Long> LONG_MANY_VALUES =
      ImmutableList.of(1111L, -2222L, 33333333L, -44444444L, 5555555555L);
  static final int LONG_MANY_VALUES_COUNT = 5;
  static final double LONG_MANY_VALUES_MEAN =
      (1111.0 - 2222.0 + 33333333.0 - 44444444.0 + 5555555555.0) / 5;
  static final double LONG_MANY_VALUES_SUM_OF_SQUARES_OF_DELTAS =
      (1111.0 - LONG_MANY_VALUES_MEAN) * (1111.0 - LONG_MANY_VALUES_MEAN)
          + (-2222.0 - LONG_MANY_VALUES_MEAN) * (-2222.0 - LONG_MANY_VALUES_MEAN)
          + (33333333.0 - LONG_MANY_VALUES_MEAN) * (33333333.0 - LONG_MANY_VALUES_MEAN)
          + (-44444444.0 - LONG_MANY_VALUES_MEAN) * (-44444444.0 - LONG_MANY_VALUES_MEAN)
          + (5555555555.0 - LONG_MANY_VALUES_MEAN) * (5555555555.0 - LONG_MANY_VALUES_MEAN);
  static final double LONG_MANY_VALUES_MAX = 5555555555.0;
  static final double LONG_MANY_VALUES_MIN = -44444444.0;

  // Longs which will overflow if summed (using long arithmetic):
  static final long[] LARGE_LONG_VALUES = {Long.MAX_VALUE, Long.MAX_VALUE / 2};
  static final double LARGE_LONG_VALUES_MEAN =
      BigInteger.valueOf(Long.MAX_VALUE)
          .multiply(BigInteger.valueOf(3L))
          .divide(BigInteger.valueOf(4L))
          .doubleValue();
  static final double LARGE_LONG_VALUES_POPULATION_VARIANCE =
      BigInteger.valueOf(Long.MAX_VALUE)
          .multiply(BigInteger.valueOf(Long.MAX_VALUE))
          .divide(BigInteger.valueOf(16L))
          .doubleValue();

  /**
   * Returns a stream of a million primitive doubles. The stream is parallel, which should cause
   * {@code collect} calls to run in multi-threaded mode, so testing the combiner as well as the
   * supplier and accumulator.
   */
  static DoubleStream megaPrimitiveDoubleStream() {
    return DoubleStream.iterate(0.0, x -> x + 1.0).limit(MEGA_STREAM_COUNT).parallel();
  }

  /** Returns a stream containing half the values from {@link #megaPrimitiveDoubleStream}. */
  static DoubleStream megaPrimitiveDoubleStreamPart1() {
    return DoubleStream.iterate(0.0, x -> x + 2.0).limit(MEGA_STREAM_COUNT / 2).parallel();
  }

  /**
   * Returns a stream containing the values from {@link #megaPrimitiveDoubleStream} not in {@link
   * #megaPrimitiveDoubleStreamPart1()}.
   */
  static DoubleStream megaPrimitiveDoubleStreamPart2() {
    return DoubleStream.iterate(999_999.0, x -> x - 2.0).limit(MEGA_STREAM_COUNT / 2).parallel();
  }

  static final long MEGA_STREAM_COUNT = 1_000_000;
  static final double MEGA_STREAM_MEAN = 999_999.0 / 2;
  static final double MEGA_STREAM_POPULATION_VARIANCE = 999_999.0 * 1_000_001.0 / 12;
  static final double MEGA_STREAM_MIN = 0.0;
  static final double MEGA_STREAM_MAX = 999_999.0;

  // Stats instances:

  static final Stats EMPTY_STATS_VARARGS = Stats.of();
  static final Stats EMPTY_STATS_ITERABLE = Stats.of(ImmutableList.<Double>of());
  static final Stats ONE_VALUE_STATS = Stats.of(ONE_VALUE);
  static final Stats OTHER_ONE_VALUE_STATS = Stats.of(OTHER_ONE_VALUE);
  static final Stats TWO_VALUES_STATS = Stats.of(TWO_VALUES);
  static final Stats OTHER_TWO_VALUES_STATS = Stats.of(OTHER_TWO_VALUES);
  static final Stats MANY_VALUES_STATS_VARARGS = Stats.of(1.1, -44.44, 33.33, 555.555, -2.2);
  static final Stats MANY_VALUES_STATS_ITERABLE = Stats.of(MANY_VALUES);
  static final Stats MANY_VALUES_STATS_ITERATOR = Stats.of(MANY_VALUES.iterator());
  static final Stats MANY_VALUES_STATS_SNAPSHOT = buildManyValuesStatsSnapshot();
  static final Stats LARGE_VALUES_STATS = Stats.of(LARGE_VALUES);
  static final Stats OTHER_MANY_VALUES_STATS = Stats.of(OTHER_MANY_VALUES);
  static final Stats INTEGER_MANY_VALUES_STATS_VARARGS =
      Stats.of(Ints.toArray(INTEGER_MANY_VALUES));
  static final Stats INTEGER_MANY_VALUES_STATS_ITERABLE = Stats.of(INTEGER_MANY_VALUES);
  static final Stats LARGE_INTEGER_VALUES_STATS = Stats.of(LARGE_INTEGER_VALUES);
  static final Stats LONG_MANY_VALUES_STATS_ITERATOR = Stats.of(LONG_MANY_VALUES.iterator());
  static final Stats LONG_MANY_VALUES_STATS_SNAPSHOT = buildLongManyValuesStatsSnapshot();
  static final Stats LARGE_LONG_VALUES_STATS = Stats.of(LARGE_LONG_VALUES);

  private static Stats buildManyValuesStatsSnapshot() {
    StatsAccumulator accumulator = new StatsAccumulator();
    accumulator.addAll(MANY_VALUES);
    Stats stats = accumulator.snapshot();
    accumulator.add(999.999); // should do nothing to the snapshot
    return stats;
  }

  private static Stats buildLongManyValuesStatsSnapshot() {
    StatsAccumulator accumulator = new StatsAccumulator();
    accumulator.addAll(LONG_MANY_VALUES);
    return accumulator.snapshot();
  }

  static final ImmutableList<Stats> ALL_STATS =
      ImmutableList.of(
          EMPTY_STATS_VARARGS,
          EMPTY_STATS_ITERABLE,
          ONE_VALUE_STATS,
          OTHER_ONE_VALUE_STATS,
          TWO_VALUES_STATS,
          OTHER_TWO_VALUES_STATS,
          MANY_VALUES_STATS_VARARGS,
          MANY_VALUES_STATS_ITERABLE,
          MANY_VALUES_STATS_ITERATOR,
          MANY_VALUES_STATS_SNAPSHOT,
          LARGE_VALUES_STATS,
          OTHER_MANY_VALUES_STATS,
          INTEGER_MANY_VALUES_STATS_VARARGS,
          INTEGER_MANY_VALUES_STATS_ITERABLE,
          LARGE_INTEGER_VALUES_STATS,
          LONG_MANY_VALUES_STATS_ITERATOR,
          LONG_MANY_VALUES_STATS_SNAPSHOT,
          LARGE_LONG_VALUES_STATS);

  // PairedStats instances:

  static final PairedStats EMPTY_PAIRED_STATS =
      createPairedStatsOf(ImmutableList.<Double>of(), ImmutableList.<Double>of());
  static final PairedStats ONE_VALUE_PAIRED_STATS =
      createPairedStatsOf(ImmutableList.of(ONE_VALUE), ImmutableList.of(OTHER_ONE_VALUE));
  static final PairedStats TWO_VALUES_PAIRED_STATS =
      createPairedStatsOf(TWO_VALUES, OTHER_TWO_VALUES);
  static final PairedStats MANY_VALUES_PAIRED_STATS = buildManyValuesPairedStats();
  static final PairedStats DUPLICATE_MANY_VALUES_PAIRED_STATS =
      createPairedStatsOf(MANY_VALUES, OTHER_MANY_VALUES);
  static final PairedStats HORIZONTAL_VALUES_PAIRED_STATS = buildHorizontalValuesPairedStats();
  static final PairedStats VERTICAL_VALUES_PAIRED_STATS = buildVerticalValuesPairedStats();
  static final PairedStats CONSTANT_VALUES_PAIRED_STATS = buildConstantValuesPairedStats();

  private static PairedStats buildManyValuesPairedStats() {
    PairedStatsAccumulator accumulator =
        createFilledPairedStatsAccumulator(MANY_VALUES, OTHER_MANY_VALUES);
    PairedStats stats = accumulator.snapshot();
    accumulator.add(99.99, 9999.9999); // should do nothing to the snapshot
    return stats;
  }

  private static PairedStats buildHorizontalValuesPairedStats() {
    PairedStatsAccumulator accumulator = new PairedStatsAccumulator();
    for (double x : MANY_VALUES) {
      accumulator.add(x, OTHER_ONE_VALUE);
    }
    return accumulator.snapshot();
  }

  private static PairedStats buildVerticalValuesPairedStats() {
    PairedStatsAccumulator accumulator = new PairedStatsAccumulator();
    for (double y : OTHER_MANY_VALUES) {
      accumulator.add(ONE_VALUE, y);
    }
    return accumulator.snapshot();
  }

  private static PairedStats buildConstantValuesPairedStats() {
    PairedStatsAccumulator accumulator = new PairedStatsAccumulator();
    for (int i = 0; i < MANY_VALUES_COUNT; ++i) {
      accumulator.add(ONE_VALUE, OTHER_ONE_VALUE);
    }
    return accumulator.snapshot();
  }

  static final ImmutableList<PairedStats> ALL_PAIRED_STATS =
      ImmutableList.of(
          EMPTY_PAIRED_STATS,
          ONE_VALUE_PAIRED_STATS,
          TWO_VALUES_PAIRED_STATS,
          MANY_VALUES_PAIRED_STATS,
          DUPLICATE_MANY_VALUES_PAIRED_STATS,
          HORIZONTAL_VALUES_PAIRED_STATS,
          VERTICAL_VALUES_PAIRED_STATS,
          CONSTANT_VALUES_PAIRED_STATS);

  // Helper methods:

  static void assertStatsApproxEqual(Stats expectedStats, Stats actualStats) {
    assertThat(actualStats.count()).isEqualTo(expectedStats.count());
    if (expectedStats.count() == 0) {
      try {
        actualStats.mean();
        fail("Expected IllegalStateException");
      } catch (IllegalStateException expected) {
      }
      try {
        actualStats.populationVariance();
        fail("Expected IllegalStateException");
      } catch (IllegalStateException expected) {
      }
      try {
        actualStats.min();
        fail("Expected IllegalStateException");
      } catch (IllegalStateException expected) {
      }
      try {
        actualStats.max();
        fail("Expected IllegalStateException");
      } catch (IllegalStateException expected) {
      }
    } else if (expectedStats.count() == 1) {
      assertThat(actualStats.mean()).isWithin(ALLOWED_ERROR).of(expectedStats.mean());
      assertThat(actualStats.populationVariance()).isWithin(0.0).of(0.0);
      assertThat(actualStats.min()).isWithin(ALLOWED_ERROR).of(expectedStats.min());
      assertThat(actualStats.max()).isWithin(ALLOWED_ERROR).of(expectedStats.max());
    } else {
      assertThat(actualStats.mean()).isWithin(ALLOWED_ERROR).of(expectedStats.mean());
      assertThat(actualStats.populationVariance())
          .isWithin(ALLOWED_ERROR)
          .of(expectedStats.populationVariance());
      assertThat(actualStats.min()).isWithin(ALLOWED_ERROR).of(expectedStats.min());
      assertThat(actualStats.max()).isWithin(ALLOWED_ERROR).of(expectedStats.max());
    }
  }

  /**
   * Asserts that {@code transformation} is diagonal (i.e. neither horizontal or vertical) and
   * passes through both {@code (x1, y1)} and {@code (x1 + xDelta, y1 + yDelta)}. Includes
   * assertions about all the public instance methods of {@link LinearTransformation} (on both
   * {@code transformation} and its inverse). Since the transformation is expected to be diagonal,
   * neither {@code xDelta} nor {@code yDelta} may be zero.
   */
  static void assertDiagonalLinearTransformation(
      LinearTransformation transformation, double x1, double y1, double xDelta, double yDelta) {
    checkArgument(xDelta != 0.0);
    checkArgument(yDelta != 0.0);
    assertThat(transformation.isHorizontal()).isFalse();
    assertThat(transformation.isVertical()).isFalse();
    assertThat(transformation.inverse().isHorizontal()).isFalse();
    assertThat(transformation.inverse().isVertical()).isFalse();
    assertThat(transformation.transform(x1)).isWithin(ALLOWED_ERROR).of(y1);
    assertThat(transformation.transform(x1 + xDelta)).isWithin(ALLOWED_ERROR).of(y1 + yDelta);
    assertThat(transformation.inverse().transform(y1)).isWithin(ALLOWED_ERROR).of(x1);
    assertThat(transformation.inverse().transform(y1 + yDelta))
        .isWithin(ALLOWED_ERROR)
        .of(x1 + xDelta);
    assertThat(transformation.slope()).isWithin(ALLOWED_ERROR).of(yDelta / xDelta);
    assertThat(transformation.inverse().slope()).isWithin(ALLOWED_ERROR).of(xDelta / yDelta);
    assertThat(transformation.inverse()).isSameInstanceAs(transformation.inverse());
    assertThat(transformation.inverse().inverse()).isSameInstanceAs(transformation);
  }

  /**
   * Asserts that {@code transformation} is horizontal with the given value of {@code y}. Includes
   * assertions about all the public instance methods of {@link LinearTransformation}, including an
   * assertion that {@link LinearTransformation#transform} and {@link LinearTransformation#slope} on
   * its inverse throws as expected.
   */
  static void assertHorizontalLinearTransformation(LinearTransformation transformation, double y) {
    assertThat(transformation.isHorizontal()).isTrue();
    assertThat(transformation.isVertical()).isFalse();
    assertThat(transformation.inverse().isHorizontal()).isFalse();
    assertThat(transformation.inverse().isVertical()).isTrue();
    assertThat(transformation.transform(-1.0)).isWithin(ALLOWED_ERROR).of(y);
    assertThat(transformation.transform(1.0)).isWithin(ALLOWED_ERROR).of(y);
    try {
      transformation.inverse().transform(0.0);
      fail("Expected IllegalStateException");
    } catch (IllegalStateException expected) {
    }
    assertThat(transformation.slope()).isWithin(ALLOWED_ERROR).of(0.0);
    try {
      transformation.inverse().slope();
      fail("Expected IllegalStateException");
    } catch (IllegalStateException expected) {
    }
    assertThat(transformation.inverse()).isSameInstanceAs(transformation.inverse());
    assertThat(transformation.inverse().inverse()).isSameInstanceAs(transformation);
  }

  /**
   * Asserts that {@code transformation} is vertical with the given value of {@code x}. Includes
   * assertions about all the public instance methods of {@link LinearTransformation}, including
   * assertions that {@link LinearTransformation#slope} and {@link LinearTransformation#transform}
   * throw as expected.
   */
  static void assertVerticalLinearTransformation(LinearTransformation transformation, double x) {
    assertThat(transformation.isHorizontal()).isFalse();
    assertThat(transformation.isVertical()).isTrue();
    assertThat(transformation.inverse().isHorizontal()).isTrue();
    assertThat(transformation.inverse().isVertical()).isFalse();
    try {
      transformation.transform(0.0);
      fail("Expected IllegalStateException");
    } catch (IllegalStateException expected) {
    }
    assertThat(transformation.inverse().transform(-1.0)).isWithin(ALLOWED_ERROR).of(x);
    assertThat(transformation.inverse().transform(1.0)).isWithin(ALLOWED_ERROR).of(x);
    try {
      transformation.slope();
      fail("Expected IllegalStateException");
    } catch (IllegalStateException expected) {
    }
    assertThat(transformation.inverse().slope()).isWithin(ALLOWED_ERROR).of(0.0);
    assertThat(transformation.inverse()).isSameInstanceAs(transformation.inverse());
    assertThat(transformation.inverse().inverse()).isSameInstanceAs(transformation);
  }

  /**
   * Asserts that {@code transformation} behaves as expected for {@link
   * LinearTransformation#forNaN}.
   */
  static void assertLinearTransformationNaN(LinearTransformation transformation) {
    assertThat(transformation.isHorizontal()).isFalse();
    assertThat(transformation.isVertical()).isFalse();
    assertThat(transformation.slope()).isNaN();
    assertThat(transformation.transform(0.0)).isNaN();
    assertThat(transformation.inverse()).isSameInstanceAs(transformation);
  }

  /**
   * Creates a {@link PairedStats} from with the given lists of {@code x} and {@code y} values,
   * which must be of the same size.
   */
  static PairedStats createPairedStatsOf(List<Double> xValues, List<Double> yValues) {
    return createFilledPairedStatsAccumulator(xValues, yValues).snapshot();
  }

  /**
   * Creates a {@link PairedStatsAccumulator} filled with the given lists of {@code x} and {@code y}
   * values, which must be of the same size.
   */
  static PairedStatsAccumulator createFilledPairedStatsAccumulator(
      List<Double> xValues, List<Double> yValues) {
    checkArgument(xValues.size() == yValues.size());
    PairedStatsAccumulator accumulator = new PairedStatsAccumulator();
    for (int index = 0; index < xValues.size(); index++) {
      accumulator.add(xValues.get(index), yValues.get(index));
    }
    return accumulator;
  }

  /**
   * Creates a {@link PairedStatsAccumulator} filled with the given lists of {@code x} and {@code y}
   * values, which must be of the same size, added in groups of {@code partitionSize} using {@link
   * PairedStatsAccumulator#addAll(PairedStats)}.
   */
  static PairedStatsAccumulator createPartitionedFilledPairedStatsAccumulator(
      List<Double> xValues, List<Double> yValues, int partitionSize) {
    checkArgument(xValues.size() == yValues.size());
    checkArgument(partitionSize > 0);
    PairedStatsAccumulator accumulator = new PairedStatsAccumulator();
    List<List<Double>> xPartitions = Lists.partition(xValues, partitionSize);
    List<List<Double>> yPartitions = Lists.partition(yValues, partitionSize);
    for (int index = 0; index < xPartitions.size(); index++) {
      accumulator.addAll(createPairedStatsOf(xPartitions.get(index), yPartitions.get(index)));
    }
    return accumulator;
  }

  private StatsTesting() {}
}
