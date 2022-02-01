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

import static com.google.common.math.Quantiles.median;
import static com.google.common.math.Quantiles.percentiles;
import static com.google.common.math.Quantiles.quartiles;
import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;
import static java.lang.Double.NEGATIVE_INFINITY;
import static java.lang.Double.NaN;
import static java.lang.Double.POSITIVE_INFINITY;
import static java.math.RoundingMode.CEILING;
import static java.math.RoundingMode.FLOOR;
import static java.math.RoundingMode.UNNECESSARY;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Ordering;
import com.google.common.math.Quantiles.ScaleAndIndexes;
import com.google.common.primitives.Doubles;
import com.google.common.primitives.Ints;
import com.google.common.primitives.Longs;
import com.google.common.truth.Correspondence;
import com.google.common.truth.Correspondence.BinaryPredicate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import junit.framework.TestCase;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Tests for {@link Quantiles}.
 *
 * @author Pete Gillin
 */
public class QuantilesTest extends TestCase {

  /*
   * Since Quantiles provides a fluent-style API, each test covers a chain of methods resulting in
   * the computation of one or more quantiles (or in an error) rather than individual methods. The
   * tests are divided into three sections:
   * 1. Tests on a hardcoded dataset for chains starting with median(), quartiles(), and scale(10);
   * 2. Tests on hardcoded datasets include non-finite values for chains starting with scale(10);
   * 3. Tests on a mechanically generated dataset for chains starting with percentiles();
   * 4. Tests of illegal usages of the API.
   */

  /*
   * Covering every combination would lead to an explosion in the number of tests. So we cover only:
   * - median with compute taking a double-collection and with computeInPlace;
   * - quartiles with index and with indexes taking int-varargs, and with compute taking a
   *   double-collection and with computeInPlace;
   * - scale with index and with indexes taking int-varargs, and with all overloads of compute
   *   taking a double-collection and with computeInPlace;
   * - scale with indexes taking integer-collection with compute taking a double-collection and with
   *   computeInPlace;
   * - (except that, for non-finite values, we don't do all combinations exhaustively);
   * - percentiles with index and with indexes taking int-varargs, and with compute taking a
   *   double-collection and with computeInPlace.
   */

  private static final double ALLOWED_ERROR = 1.0e-10;

  /**
   * A {@link Correspondence} which accepts finite values within {@link #ALLOWED_ERROR} of each
   * other.
   */
  private static final Correspondence<Number, Number> FINITE_QUANTILE_CORRESPONDENCE =
      Correspondence.tolerance(ALLOWED_ERROR);

  /**
   * A {@link Correspondence} which accepts either finite values within {@link #ALLOWED_ERROR} of
   * each other or identical non-finite values.
   */
  private static final Correspondence<Double, Double> QUANTILE_CORRESPONDENCE =
      Correspondence.from(
          new BinaryPredicate<Double, Double>() {
            @Override
            public boolean apply(@Nullable Double actual, @Nullable Double expected) {
              // Test for equality to allow non-finite values to match; otherwise, use the finite
              // test.
              return actual.equals(expected)
                  || FINITE_QUANTILE_CORRESPONDENCE.compare(actual, expected);
            }
          },
          "is identical to or " + FINITE_QUANTILE_CORRESPONDENCE);

  // 1. Tests on a hardcoded dataset for chains starting with median(), quartiles(), and scale(10):

  /** The squares of the 16 integers from 0 to 15, in an arbitrary order. */
  private static final ImmutableList<Double> SIXTEEN_SQUARES_DOUBLES =
      ImmutableList.of(
          25.0, 100.0, 0.0, 144.0, 9.0, 121.0, 4.0, 225.0, 169.0, 64.0, 49.0, 16.0, 36.0, 1.0, 81.0,
          196.0);

  private static final ImmutableList<Long> SIXTEEN_SQUARES_LONGS =
      ImmutableList.of(
          25L, 100L, 0L, 144L, 9L, 121L, 4L, 225L, 169L, 64L, 49L, 16L, 36L, 1L, 81L, 196L);
  private static final ImmutableList<Integer> SIXTEEN_SQUARES_INTEGERS =
      ImmutableList.of(25, 100, 0, 144, 9, 121, 4, 225, 169, 64, 49, 16, 36, 1, 81, 196);
  private static final double SIXTEEN_SQUARES_MIN = 0.0;
  private static final double SIXTEEN_SQUARES_DECILE_1 = 0.5 * (1.0 + 4.0);
  private static final double SIXTEEN_SQUARES_QUARTILE_1 = 0.25 * 9.0 + 0.75 * 16.0;
  private static final double SIXTEEN_SQUARES_MEDIAN = 0.5 * (49.0 + 64.0);
  private static final double SIXTEEN_SQUARES_QUARTILE_3 = 0.75 * 121.0 + 0.25 * 144.0;
  private static final double SIXTEEN_SQUARES_DECILE_8 = 144.0;
  private static final double SIXTEEN_SQUARES_MAX = 225.0;

  public void testMedian_compute_doubleCollection() {
    assertThat(median().compute(SIXTEEN_SQUARES_DOUBLES))
        .isWithin(ALLOWED_ERROR)
        .of(SIXTEEN_SQUARES_MEDIAN);
  }

  public void testMedian_computeInPlace() {
    double[] dataset = Doubles.toArray(SIXTEEN_SQUARES_DOUBLES);
    assertThat(median().computeInPlace(dataset)).isWithin(ALLOWED_ERROR).of(SIXTEEN_SQUARES_MEDIAN);
    assertThat(dataset).usingExactEquality().containsExactlyElementsIn(SIXTEEN_SQUARES_DOUBLES);
  }

  public void testQuartiles_index_compute_doubleCollection() {
    assertThat(quartiles().index(1).compute(SIXTEEN_SQUARES_DOUBLES))
        .isWithin(ALLOWED_ERROR)
        .of(SIXTEEN_SQUARES_QUARTILE_1);
  }

  public void testQuartiles_index_computeInPlace() {
    double[] dataset = Doubles.toArray(SIXTEEN_SQUARES_DOUBLES);
    assertThat(quartiles().index(1).computeInPlace(dataset))
        .isWithin(ALLOWED_ERROR)
        .of(SIXTEEN_SQUARES_QUARTILE_1);
    assertThat(dataset).usingExactEquality().containsExactlyElementsIn(SIXTEEN_SQUARES_DOUBLES);
  }

  public void testQuartiles_indexes_varargs_compute_doubleCollection() {
    assertThat(quartiles().indexes(1, 3).compute(SIXTEEN_SQUARES_DOUBLES))
        .comparingValuesUsing(QUANTILE_CORRESPONDENCE)
        .containsExactly(1, SIXTEEN_SQUARES_QUARTILE_1, 3, SIXTEEN_SQUARES_QUARTILE_3);
  }

  public void testQuartiles_indexes_varargs_computeInPlace() {
    double[] dataset = Doubles.toArray(SIXTEEN_SQUARES_DOUBLES);
    assertThat(quartiles().indexes(1, 3).computeInPlace(dataset))
        .comparingValuesUsing(QUANTILE_CORRESPONDENCE)
        .containsExactly(
            1, SIXTEEN_SQUARES_QUARTILE_1,
            3, SIXTEEN_SQUARES_QUARTILE_3);
    assertThat(dataset).usingExactEquality().containsExactlyElementsIn(SIXTEEN_SQUARES_DOUBLES);
  }

  public void testScale_index_compute_doubleCollection() {
    assertThat(Quantiles.scale(10).index(1).compute(SIXTEEN_SQUARES_DOUBLES))
        .isWithin(ALLOWED_ERROR)
        .of(SIXTEEN_SQUARES_DECILE_1);
  }

  public void testScale_index_compute_longCollection() {
    assertThat(Quantiles.scale(10).index(1).compute(SIXTEEN_SQUARES_LONGS))
        .isWithin(ALLOWED_ERROR)
        .of(SIXTEEN_SQUARES_DECILE_1);
  }

  public void testScale_index_compute_integerCollection() {
    assertThat(Quantiles.scale(10).index(1).compute(SIXTEEN_SQUARES_INTEGERS))
        .isWithin(ALLOWED_ERROR)
        .of(SIXTEEN_SQUARES_DECILE_1);
  }

  public void testScale_index_compute_doubleVarargs() {
    double[] dataset = Doubles.toArray(SIXTEEN_SQUARES_DOUBLES);
    assertThat(Quantiles.scale(10).index(1).compute(dataset))
        .isWithin(ALLOWED_ERROR)
        .of(SIXTEEN_SQUARES_DECILE_1);
    assertThat(dataset)
        .usingExactEquality()
        .containsExactlyElementsIn(SIXTEEN_SQUARES_DOUBLES)
        .inOrder();
  }

  public void testScale_index_compute_longVarargs() {
    long[] dataset = Longs.toArray(SIXTEEN_SQUARES_LONGS);
    assertThat(Quantiles.scale(10).index(1).compute(dataset))
        .isWithin(ALLOWED_ERROR)
        .of(SIXTEEN_SQUARES_DECILE_1);
    assertThat(dataset).asList().isEqualTo(SIXTEEN_SQUARES_LONGS);
  }

  public void testScale_index_compute_intVarargs() {
    int[] dataset = Ints.toArray(SIXTEEN_SQUARES_INTEGERS);
    assertThat(Quantiles.scale(10).index(1).compute(dataset))
        .isWithin(ALLOWED_ERROR)
        .of(SIXTEEN_SQUARES_DECILE_1);
    assertThat(dataset).asList().isEqualTo(SIXTEEN_SQUARES_INTEGERS);
  }

  public void testScale_index_computeInPlace() {
    double[] dataset = Doubles.toArray(SIXTEEN_SQUARES_DOUBLES);
    assertThat(Quantiles.scale(10).index(1).computeInPlace(dataset))
        .isWithin(ALLOWED_ERROR)
        .of(SIXTEEN_SQUARES_DECILE_1);
    assertThat(dataset).usingExactEquality().containsExactlyElementsIn(SIXTEEN_SQUARES_DOUBLES);
  }

  public void testScale_index_computeInPlace_explicitVarargs() {
    assertThat(Quantiles.scale(10).index(5).computeInPlace(78.9, 12.3, 45.6))
        .isWithin(ALLOWED_ERROR)
        .of(45.6);
  }

  public void testScale_indexes_varargs_compute_doubleCollection() {
    // Note that we specify index 1 twice, which by the method contract should be ignored.
    assertThat(Quantiles.scale(10).indexes(0, 10, 5, 1, 8, 1).compute(SIXTEEN_SQUARES_DOUBLES))
        .comparingValuesUsing(QUANTILE_CORRESPONDENCE)
        .containsExactly(
            0, SIXTEEN_SQUARES_MIN,
            10, SIXTEEN_SQUARES_MAX,
            5, SIXTEEN_SQUARES_MEDIAN,
            1, SIXTEEN_SQUARES_DECILE_1,
            8, SIXTEEN_SQUARES_DECILE_8);
  }

  public void testScale_indexes_varargs_compute_doubleCollection_snapshotsIndexes() {
    // This test is the same as testScale_indexes_varargs_compute_doubleCollection except that the
    // array of indexes to be calculated is modified between the calls to indexes and compute: since
    // the contract is that it is snapshotted, this shouldn't make any difference to the result.
    int[] indexes = {0, 10, 5, 1, 8, 10};
    ScaleAndIndexes intermediate = Quantiles.scale(10).indexes(indexes);
    indexes[0] = 3;
    assertThat(intermediate.compute(SIXTEEN_SQUARES_DOUBLES))
        .comparingValuesUsing(QUANTILE_CORRESPONDENCE)
        .containsExactly(
            0, SIXTEEN_SQUARES_MIN,
            10, SIXTEEN_SQUARES_MAX,
            5, SIXTEEN_SQUARES_MEDIAN,
            1, SIXTEEN_SQUARES_DECILE_1,
            8, SIXTEEN_SQUARES_DECILE_8);
  }

  public void testScale_indexes_largeVarargs_compute_doubleCollection() {
    int scale = Integer.MAX_VALUE;
    int otherIndex = (Integer.MAX_VALUE - 1) / 3; // this divides exactly
    // For the otherIndex calculation, we have q=Integer.MAX_VALUE, k=(Integer.MAX_VALUE-1)/3, and
    // N=16. Therefore k*(N-1)/q = 5-5/Integer.MAX_VALUE, which has floor 4 and fractional part
    // (1-5/Integer.MAX_VALUE).
    double otherValue = 16.0 * 5.0 / Integer.MAX_VALUE + 25.0 * (1.0 - 5.0 / Integer.MAX_VALUE);
    assertThat(
            Quantiles.scale(scale).indexes(0, scale, otherIndex).compute(SIXTEEN_SQUARES_DOUBLES))
        .comparingValuesUsing(QUANTILE_CORRESPONDENCE)
        .containsExactly(
            0, SIXTEEN_SQUARES_MIN, scale, SIXTEEN_SQUARES_MAX, otherIndex, otherValue);
  }

  public void testScale_indexes_varargs_compute_longCollection() {
    // Note that we specify index 1 twice, which by the method contract should be ignored.
    assertThat(Quantiles.scale(10).indexes(0, 10, 5, 1, 8, 1).compute(SIXTEEN_SQUARES_LONGS))
        .comparingValuesUsing(QUANTILE_CORRESPONDENCE)
        .containsExactly(
            0, SIXTEEN_SQUARES_MIN,
            10, SIXTEEN_SQUARES_MAX,
            5, SIXTEEN_SQUARES_MEDIAN,
            1, SIXTEEN_SQUARES_DECILE_1,
            8, SIXTEEN_SQUARES_DECILE_8);
  }

  public void testScale_indexes_varargs_compute_integerCollection() {
    // Note that we specify index 1 twice, which by the method contract should be ignored.
    assertThat(Quantiles.scale(10).indexes(0, 10, 5, 1, 8, 1).compute(SIXTEEN_SQUARES_INTEGERS))
        .comparingValuesUsing(QUANTILE_CORRESPONDENCE)
        .containsExactly(
            0, SIXTEEN_SQUARES_MIN,
            10, SIXTEEN_SQUARES_MAX,
            5, SIXTEEN_SQUARES_MEDIAN,
            1, SIXTEEN_SQUARES_DECILE_1,
            8, SIXTEEN_SQUARES_DECILE_8);
  }

  public void testScale_indexes_varargs_compute_indexOrderIsMaintained() {
    assertThat(Quantiles.scale(10).indexes(0, 10, 5, 1, 8, 1).compute(SIXTEEN_SQUARES_INTEGERS))
        .comparingValuesUsing(QUANTILE_CORRESPONDENCE)
        .containsExactly(
            0, SIXTEEN_SQUARES_MIN,
            10, SIXTEEN_SQUARES_MAX,
            5, SIXTEEN_SQUARES_MEDIAN,
            1, SIXTEEN_SQUARES_DECILE_1,
            8, SIXTEEN_SQUARES_DECILE_8)
        .inOrder();
  }

  public void testScale_indexes_varargs_compute_doubleVarargs() {
    double[] dataset = Doubles.toArray(SIXTEEN_SQUARES_DOUBLES);
    assertThat(Quantiles.scale(10).indexes(0, 10, 5, 1, 8, 1).compute(dataset))
        .comparingValuesUsing(QUANTILE_CORRESPONDENCE)
        .containsExactly(
            0, SIXTEEN_SQUARES_MIN,
            10, SIXTEEN_SQUARES_MAX,
            5, SIXTEEN_SQUARES_MEDIAN,
            1, SIXTEEN_SQUARES_DECILE_1,
            8, SIXTEEN_SQUARES_DECILE_8);
    assertThat(dataset)
        .usingExactEquality()
        .containsExactlyElementsIn(SIXTEEN_SQUARES_DOUBLES)
        .inOrder();
  }

  public void testScale_indexes_varargs_compute_longVarargs() {
    long[] dataset = Longs.toArray(SIXTEEN_SQUARES_LONGS);
    assertThat(Quantiles.scale(10).indexes(0, 10, 5, 1, 8, 1).compute(dataset))
        .comparingValuesUsing(QUANTILE_CORRESPONDENCE)
        .containsExactly(
            0, SIXTEEN_SQUARES_MIN,
            10, SIXTEEN_SQUARES_MAX,
            5, SIXTEEN_SQUARES_MEDIAN,
            1, SIXTEEN_SQUARES_DECILE_1,
            8, SIXTEEN_SQUARES_DECILE_8);
    assertThat(dataset).asList().isEqualTo(SIXTEEN_SQUARES_LONGS);
  }

  public void testScale_indexes_varargs_compute_intVarargs() {
    int[] dataset = Ints.toArray(SIXTEEN_SQUARES_INTEGERS);
    assertThat(Quantiles.scale(10).indexes(0, 10, 5, 1, 8, 1).compute(dataset))
        .comparingValuesUsing(QUANTILE_CORRESPONDENCE)
        .containsExactly(
            0, SIXTEEN_SQUARES_MIN,
            10, SIXTEEN_SQUARES_MAX,
            5, SIXTEEN_SQUARES_MEDIAN,
            1, SIXTEEN_SQUARES_DECILE_1,
            8, SIXTEEN_SQUARES_DECILE_8);
    assertThat(dataset).asList().isEqualTo(SIXTEEN_SQUARES_INTEGERS);
  }

  public void testScale_indexes_varargs_computeInPlace() {
    double[] dataset = Doubles.toArray(SIXTEEN_SQUARES_DOUBLES);
    assertThat(Quantiles.scale(10).indexes(0, 10, 5, 1, 8, 1).computeInPlace(dataset))
        .comparingValuesUsing(QUANTILE_CORRESPONDENCE)
        .containsExactly(
            0, SIXTEEN_SQUARES_MIN,
            10, SIXTEEN_SQUARES_MAX,
            5, SIXTEEN_SQUARES_MEDIAN,
            1, SIXTEEN_SQUARES_DECILE_1,
            8, SIXTEEN_SQUARES_DECILE_8);
    assertThat(dataset).usingExactEquality().containsExactlyElementsIn(SIXTEEN_SQUARES_DOUBLES);
  }

  public void testScale_indexes_varargs_computeInPlace_explicitVarargs() {
    assertThat(Quantiles.scale(10).indexes(0, 10).computeInPlace(78.9, 12.3, 45.6))
        .comparingValuesUsing(QUANTILE_CORRESPONDENCE)
        .containsExactly(
            0, 12.3,
            10, 78.9);
  }

  public void testScale_indexes_collection_compute_doubleCollection() {
    // Note that we specify index 1 twice, which by the method contract should be ignored.
    assertThat(
            Quantiles.scale(10)
                .indexes(ImmutableList.of(0, 10, 5, 1, 8, 1))
                .compute(SIXTEEN_SQUARES_DOUBLES))
        .comparingValuesUsing(QUANTILE_CORRESPONDENCE)
        .containsExactly(
            0, SIXTEEN_SQUARES_MIN,
            10, SIXTEEN_SQUARES_MAX,
            5, SIXTEEN_SQUARES_MEDIAN,
            1, SIXTEEN_SQUARES_DECILE_1,
            8, SIXTEEN_SQUARES_DECILE_8);
  }

  public void testScale_indexes_collection_computeInPlace() {
    double[] dataset = Doubles.toArray(SIXTEEN_SQUARES_DOUBLES);
    assertThat(
            Quantiles.scale(10)
                .indexes(ImmutableList.of(0, 10, 5, 1, 8, 1))
                .computeInPlace(dataset))
        .comparingValuesUsing(QUANTILE_CORRESPONDENCE)
        .containsExactly(
            0, SIXTEEN_SQUARES_MIN,
            10, SIXTEEN_SQUARES_MAX,
            5, SIXTEEN_SQUARES_MEDIAN,
            1, SIXTEEN_SQUARES_DECILE_1,
            8, SIXTEEN_SQUARES_DECILE_8);
    assertThat(dataset).usingExactEquality().containsExactlyElementsIn(SIXTEEN_SQUARES_DOUBLES);
  }

  // 2. Tests on hardcoded datasets include non-finite values for chains starting with scale(10):

  private static final ImmutableList<Double> ONE_TO_FIVE_AND_POSITIVE_INFINITY =
      ImmutableList.of(3.0, 5.0, POSITIVE_INFINITY, 1.0, 4.0, 2.0);
  private static final ImmutableList<Double> ONE_TO_FIVE_AND_NEGATIVE_INFINITY =
      ImmutableList.of(3.0, 5.0, NEGATIVE_INFINITY, 1.0, 4.0, 2.0);
  private static final ImmutableList<Double> NEGATIVE_INFINITY_AND_FIVE_POSITIVE_INFINITIES =
      ImmutableList.of(
          POSITIVE_INFINITY,
          POSITIVE_INFINITY,
          NEGATIVE_INFINITY,
          POSITIVE_INFINITY,
          POSITIVE_INFINITY,
          POSITIVE_INFINITY);
  private static final ImmutableList<Double> ONE_TO_FIVE_AND_NAN =
      ImmutableList.of(3.0, 5.0, NaN, 1.0, 4.0, 2.0);

  public void testScale_indexes_varargs_compute_doubleCollection_positiveInfinity() {
    assertThat(
            Quantiles.scale(10)
                .indexes(0, 1, 2, 8, 9, 10)
                .compute(ONE_TO_FIVE_AND_POSITIVE_INFINITY))
        .comparingValuesUsing(QUANTILE_CORRESPONDENCE)
        .containsExactly(
            0, 1.0,
            1, 1.5,
            2, 2.0,
            8, 5.0,
            9, POSITIVE_INFINITY, // interpolating between 5.0 and POSITIVE_INFNINITY
            10, POSITIVE_INFINITY);
  }

  public void testScale_index_compute_doubleCollection_positiveInfinity() {
    // interpolating between 5.0 and POSITIVE_INFNINITY
    assertThat(Quantiles.scale(10).index(9).compute(ONE_TO_FIVE_AND_POSITIVE_INFINITY))
        .isPositiveInfinity();
  }

  public void testScale_indexes_varargs_compute_doubleCollection_negativeInfinity() {
    assertThat(
            Quantiles.scale(10)
                .indexes(0, 1, 2, 8, 9, 10)
                .compute(ONE_TO_FIVE_AND_NEGATIVE_INFINITY))
        .comparingValuesUsing(QUANTILE_CORRESPONDENCE)
        .containsExactly(
            0, NEGATIVE_INFINITY,
            1, NEGATIVE_INFINITY, // interpolating between NEGATIVE_INFNINITY and 1.0
            2, 1.0,
            8, 4.0,
            9, 4.5,
            10, 5.0);
  }

  public void testScale_index_compute_doubleCollection_negativeInfinity() {
    // interpolating between NEGATIVE_INFNINITY and 1.0
    assertThat(Quantiles.scale(10).index(1).compute(ONE_TO_FIVE_AND_NEGATIVE_INFINITY))
        .isNegativeInfinity();
  }

  public void testScale_indexes_varargs_compute_doubleCollection_bothInfinities() {
    assertThat(
            Quantiles.scale(10)
                .indexes(0, 1, 2, 8, 9, 10)
                .compute(NEGATIVE_INFINITY_AND_FIVE_POSITIVE_INFINITIES))
        .comparingValuesUsing(QUANTILE_CORRESPONDENCE)
        .containsExactly(
            0, NEGATIVE_INFINITY,
            1, NaN, // interpolating between NEGATIVE_ and POSITIVE_INFINITY values
            2, POSITIVE_INFINITY,
            8, POSITIVE_INFINITY,
            9, POSITIVE_INFINITY, // interpolating between two POSITIVE_INFINITY values
            10, POSITIVE_INFINITY);
  }

  public void testScale_indexes_varargs_compute_doubleCollection_nan() {
    assertThat(Quantiles.scale(10).indexes(0, 1, 2, 8, 9, 10).compute(ONE_TO_FIVE_AND_NAN))
        .comparingValuesUsing(QUANTILE_CORRESPONDENCE)
        .containsExactly(
            0, NaN,
            1, NaN,
            2, NaN,
            8, NaN,
            9, NaN,
            10, NaN);
  }

  public void testScale_index_compute_doubleCollection_nan() {
    assertThat(Quantiles.scale(10).index(5).compute(ONE_TO_FIVE_AND_NAN)).isNaN();
  }

  // 3. Tests on a mechanically generated dataset for chains starting with percentiles():

  private static final int PSEUDORANDOM_DATASET_SIZE = 9951;
  private static final ImmutableList<Double> PSEUDORANDOM_DATASET = generatePseudorandomDataset();
  private static final ImmutableList<Double> PSEUDORANDOM_DATASET_SORTED =
      Ordering.natural().immutableSortedCopy(PSEUDORANDOM_DATASET);

  private static ImmutableList<Double> generatePseudorandomDataset() {
    Random random = new Random(2211275185798966364L);
    ImmutableList.Builder<Double> largeDatasetBuilder = ImmutableList.builder();
    for (int i = 0; i < PSEUDORANDOM_DATASET_SIZE; i++) {
      largeDatasetBuilder.add(random.nextGaussian());
    }
    return largeDatasetBuilder.build();
  }

  private static double expectedLargeDatasetPercentile(int index) {
    // We have q=100, k=index, and N=9951. Therefore k*(N-1)/q is 99.5*index. If index is even, that
    // is an integer 199*index/2. If index is odd, that is halfway between floor(199*index/2) and
    // ceil(199*index/2).
    if (index % 2 == 0) {
      int position = IntMath.divide(199 * index, 2, UNNECESSARY);
      return PSEUDORANDOM_DATASET_SORTED.get(position);
    } else {
      int positionFloor = IntMath.divide(199 * index, 2, FLOOR);
      int positionCeil = IntMath.divide(199 * index, 2, CEILING);
      double lowerValue = PSEUDORANDOM_DATASET_SORTED.get(positionFloor);
      double upperValue = PSEUDORANDOM_DATASET_SORTED.get(positionCeil);
      return (lowerValue + upperValue) / 2.0;
    }
  }

  public void testPercentiles_index_compute_doubleCollection() {
    for (int index = 0; index <= 100; index++) {
      assertWithMessage("quantile at index " + index)
          .that(percentiles().index(index).compute(PSEUDORANDOM_DATASET))
          .isWithin(ALLOWED_ERROR)
          .of(expectedLargeDatasetPercentile(index));
    }
  }

  @AndroidIncompatible // slow
  public void testPercentiles_index_computeInPlace() {
    // Assert that the computation gives the correct result for all possible percentiles.
    for (int index = 0; index <= 100; index++) {
      double[] dataset = Doubles.toArray(PSEUDORANDOM_DATASET);
      assertWithMessage("quantile at index " + index)
          .that(percentiles().index(index).computeInPlace(dataset))
          .isWithin(ALLOWED_ERROR)
          .of(expectedLargeDatasetPercentile(index));
    }

    // Assert that the dataset contains the same elements after the in-place computation (although
    // they may be reordered). We only do this for one index rather than for all indexes, as it is
    // quite expensives (quadratic in the size of PSEUDORANDOM_DATASET).
    double[] dataset = Doubles.toArray(PSEUDORANDOM_DATASET);
    @SuppressWarnings("unused")
    double actual = percentiles().index(33).computeInPlace(dataset);
    assertThat(dataset).usingExactEquality().containsExactlyElementsIn(PSEUDORANDOM_DATASET);
  }

  public void testPercentiles_indexes_varargsPairs_compute_doubleCollection() {
    for (int index1 = 0; index1 <= 100; index1++) {
      for (int index2 = 0; index2 <= 100; index2++) {
        ImmutableMap.Builder<Integer, Double> expectedBuilder = ImmutableMap.builder();
        expectedBuilder.put(index1, expectedLargeDatasetPercentile(index1));
        if (index2 != index1) {
          expectedBuilder.put(index2, expectedLargeDatasetPercentile(index2));
        }
        assertThat(percentiles().indexes(index1, index2).compute(PSEUDORANDOM_DATASET))
            .comparingValuesUsing(QUANTILE_CORRESPONDENCE)
            .containsExactlyEntriesIn(expectedBuilder.buildOrThrow());
      }
    }
  }

  public void testPercentiles_indexes_varargsAll_compute_doubleCollection() {
    ArrayList<Integer> indexes = new ArrayList<>();
    ImmutableMap.Builder<Integer, Double> expectedBuilder = ImmutableMap.builder();
    for (int index = 0; index <= 100; index++) {
      indexes.add(index);
      expectedBuilder.put(index, expectedLargeDatasetPercentile(index));
    }
    Random random = new Random(770683168895677741L);
    Collections.shuffle(indexes, random);
    assertThat(percentiles().indexes(Ints.toArray(indexes)).compute(PSEUDORANDOM_DATASET))
        .comparingValuesUsing(QUANTILE_CORRESPONDENCE)
        .containsExactlyEntriesIn(expectedBuilder.buildOrThrow());
  }

  @AndroidIncompatible // slow
  public void testPercentiles_indexes_varargsAll_computeInPlace() {
    double[] dataset = Doubles.toArray(PSEUDORANDOM_DATASET);
    List<Integer> indexes = new ArrayList<>();
    ImmutableMap.Builder<Integer, Double> expectedBuilder = ImmutableMap.builder();
    for (int index = 0; index <= 100; index++) {
      indexes.add(index);
      expectedBuilder.put(index, expectedLargeDatasetPercentile(index));
    }
    Random random = new Random(770683168895677741L);
    Collections.shuffle(indexes, random);
    assertThat(percentiles().indexes(Ints.toArray(indexes)).computeInPlace(dataset))
        .comparingValuesUsing(QUANTILE_CORRESPONDENCE)
        .containsExactlyEntriesIn(expectedBuilder.buildOrThrow());
    assertThat(dataset).usingExactEquality().containsExactlyElementsIn(PSEUDORANDOM_DATASET);
  }

  // 4. Tests of illegal usages of the API:

  private static final ImmutableList<Double> EMPTY_DATASET = ImmutableList.of();

  public void testScale_zero() {
    try {
      Quantiles.scale(0);
      fail("Expected IllegalArgumentException");
    } catch (IllegalArgumentException expected) {
    }
  }

  public void testScale_negative() {
    try {
      Quantiles.scale(-4);
      fail("Expected IllegalArgumentException");
    } catch (IllegalArgumentException expected) {
    }
  }

  public void testScale_index_negative() {
    Quantiles.Scale intermediate = Quantiles.scale(10);
    try {
      intermediate.index(-1);
      fail("Expected IllegalArgumentException");
    } catch (IllegalArgumentException expected) {
    }
  }

  public void testScale_index_tooHigh() {
    Quantiles.Scale intermediate = Quantiles.scale(10);
    try {
      intermediate.index(11);
      fail("Expected IllegalArgumentException");
    } catch (IllegalArgumentException expected) {
    }
  }

  public void testScale_indexes_varargs_negative() {
    Quantiles.Scale intermediate = Quantiles.scale(10);
    try {
      intermediate.indexes(1, -1, 3);
      fail("Expected IllegalArgumentException");
    } catch (IllegalArgumentException expected) {
    }
  }

  public void testScale_indexes_varargs_tooHigh() {
    Quantiles.Scale intermediate = Quantiles.scale(10);
    try {
      intermediate.indexes(1, 11, 3);
      fail("Expected IllegalArgumentException");
    } catch (IllegalArgumentException expected) {
    }
  }

  public void testScale_indexes_collection_negative() {
    Quantiles.Scale intermediate = Quantiles.scale(10);
    try {
      intermediate.indexes(ImmutableList.of(1, -1, 3));
      fail("Expected IllegalArgumentException");
    } catch (IllegalArgumentException expected) {
    }
  }

  public void testScale_indexes_collection_tooHigh() {
    Quantiles.Scale intermediate = Quantiles.scale(10);
    try {
      intermediate.indexes(ImmutableList.of(1, 11, 3));
      fail("Expected IllegalArgumentException");
    } catch (IllegalArgumentException expected) {
    }
  }

  public void testScale_index_compute_doubleCollection_empty() {
    Quantiles.ScaleAndIndex intermediate = Quantiles.scale(10).index(3);
    try {
      intermediate.compute(EMPTY_DATASET);
      fail("Expected IllegalArgumentException");
    } catch (IllegalArgumentException expected) {
    }
  }

  public void testScale_index_compute_doubleVarargs_empty() {
    Quantiles.ScaleAndIndex intermediate = Quantiles.scale(10).index(3);
    try {
      intermediate.compute(new double[] {});
      fail("Expected IllegalArgumentException");
    } catch (IllegalArgumentException expected) {
    }
  }

  public void testScale_index_compute_longVarargs_empty() {
    Quantiles.ScaleAndIndex intermediate = Quantiles.scale(10).index(3);
    try {
      intermediate.compute(new long[] {});
      fail("Expected IllegalArgumentException");
    } catch (IllegalArgumentException expected) {
    }
  }

  public void testScale_index_compute_intVarargs_empty() {
    Quantiles.ScaleAndIndex intermediate = Quantiles.scale(10).index(3);
    try {
      intermediate.compute(new int[] {});
      fail("Expected IllegalArgumentException");
    } catch (IllegalArgumentException expected) {
    }
  }

  public void testScale_index_computeInPlace_empty() {
    Quantiles.ScaleAndIndex intermediate = Quantiles.scale(10).index(3);
    try {
      intermediate.computeInPlace(new double[] {});
      fail("Expected IllegalArgumentException");
    } catch (IllegalArgumentException expected) {
    }
  }

  public void testScale_indexes_varargs_compute_doubleCollection_empty() {
    Quantiles.ScaleAndIndexes intermediate = Quantiles.scale(10).indexes(1, 3, 5);
    try {
      intermediate.compute(EMPTY_DATASET);
      fail("Expected IllegalArgumentException");
    } catch (IllegalArgumentException expected) {
    }
  }

  public void testScale_indexes_varargs_compute_doubleVarargs_empty() {
    Quantiles.ScaleAndIndexes intermediate = Quantiles.scale(10).indexes(1, 3, 5);
    try {
      intermediate.compute(new double[] {});
      fail("Expected IllegalArgumentException");
    } catch (IllegalArgumentException expected) {
    }
  }

  public void testScale_indexes_varargs_compute_longVarargs_empty() {
    Quantiles.ScaleAndIndexes intermediate = Quantiles.scale(10).indexes(1, 3, 5);
    try {
      intermediate.compute(new long[] {});
      fail("Expected IllegalArgumentException");
    } catch (IllegalArgumentException expected) {
    }
  }

  public void testScale_indexes_varargs_compute_intVarargs_empty() {
    Quantiles.ScaleAndIndexes intermediate = Quantiles.scale(10).indexes(1, 3, 5);
    try {
      intermediate.compute(new int[] {});
      fail("Expected IllegalArgumentException");
    } catch (IllegalArgumentException expected) {
    }
  }

  public void testScale_indexes_varargs_computeInPlace_empty() {
    Quantiles.ScaleAndIndexes intermediate = Quantiles.scale(10).indexes(1, 3, 5);
    try {
      intermediate.computeInPlace(new double[] {});
      fail("Expected IllegalArgumentException");
    } catch (IllegalArgumentException expected) {
    }
  }

  public void testScale_indexes_indexes_computeInPlace_empty() {
    int[] emptyIndexes = {};
    try {
      Quantiles.ScaleAndIndexes unused = Quantiles.scale(10).indexes(emptyIndexes);
      fail("Expected IllegalArgumentException");
    } catch (IllegalArgumentException expected) {
    }
  }
}
