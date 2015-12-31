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

import junit.framework.TestCase;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;

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

  // 1. Tests on a hardcoded dataset for chains starting with median(), quartiles(), and scale(10):

  /**
   * The squares of the 16 integers from 0 to 15, in an arbitrary order.
   */
  private static final ImmutableList<Double> SIXTEEN_SQUARES_DOUBLES = ImmutableList.of(25.0, 100.0,
      0.0, 144.0, 9.0, 121.0, 4.0, 225.0, 169.0, 64.0, 49.0, 16.0, 36.0, 1.0, 81.0, 196.0);
  private static final ImmutableList<Long> SIXTEEN_SQUARES_LONGS = ImmutableList.of(25L, 100L,
      0L, 144L, 9L, 121L, 4L, 225L, 169L, 64L, 49L, 16L, 36L, 1L, 81L, 196L);
  private static final ImmutableList<Integer> SIXTEEN_SQUARES_INTEGERS = ImmutableList.of(25, 100,
      0, 144, 9, 121, 4, 225, 169, 64, 49, 16, 36, 1, 81, 196);
  private static final double SIXTEEN_SQUARES_MIN = 0.0;
  private static final double SIXTEEN_SQUARES_DECILE_1 = 0.5 * (1.0 + 4.0);
  private static final double SIXTEEN_SQUARES_QUARTILE_1 = 0.25 * 9.0 + 0.75 * 16.0;
  private static final double SIXTEEN_SQUARES_MEDIAN = 0.5 * (49.0 + 64.0);
  private static final double SIXTEEN_SQUARES_QUARTILE_3 = 0.75 * 121.0 + 0.25 * 144.0;
  private static final double SIXTEEN_SQUARES_DECILE_8 = 144.0;
  private static final double SIXTEEN_SQUARES_MAX = 225.0;

  public void testMedian_compute_doubleCollection() {
    assertQuantile(1, SIXTEEN_SQUARES_MEDIAN, median().compute(SIXTEEN_SQUARES_DOUBLES));
  }

  public void testMedian_computeInPlace() {
    double[] dataset = Doubles.toArray(SIXTEEN_SQUARES_DOUBLES);
    assertQuantile(1, SIXTEEN_SQUARES_MEDIAN, median().computeInPlace(dataset));
    assertDatasetAnyOrder(SIXTEEN_SQUARES_DOUBLES, dataset);
  }

  public void testQuartiles_index_compute_doubleCollection() {
    assertQuantile(1,
        SIXTEEN_SQUARES_QUARTILE_1, quartiles().index(1).compute(SIXTEEN_SQUARES_DOUBLES));
  }

  public void testQuartiles_index_computeInPlace() {
    double[] dataset = Doubles.toArray(SIXTEEN_SQUARES_DOUBLES);
    assertQuantile(1, SIXTEEN_SQUARES_QUARTILE_1, quartiles().index(1).computeInPlace(dataset));
    assertDatasetAnyOrder(SIXTEEN_SQUARES_DOUBLES, dataset);
  }

  public void testQuartiles_indexes_varargs_compute_doubleCollection() {
    ImmutableMap<Integer, Double> expected = ImmutableMap.of(
        1, SIXTEEN_SQUARES_QUARTILE_1,
        3, SIXTEEN_SQUARES_QUARTILE_3
        );
    assertQuantilesMap(expected, quartiles().indexes(1, 3).compute(SIXTEEN_SQUARES_DOUBLES));
  }

  public void testQuartiles_indexes_varargs_computeInPlace() {
    double[] dataset = Doubles.toArray(SIXTEEN_SQUARES_DOUBLES);
    ImmutableMap<Integer, Double> expected = ImmutableMap.of(
        1, SIXTEEN_SQUARES_QUARTILE_1,
        3, SIXTEEN_SQUARES_QUARTILE_3
        );
    assertQuantilesMap(expected, quartiles().indexes(1, 3).computeInPlace(dataset));
    assertDatasetAnyOrder(SIXTEEN_SQUARES_DOUBLES, dataset);
  }

  public void testScale_index_compute_doubleCollection() {
    assertQuantile(1, SIXTEEN_SQUARES_DECILE_1,
        Quantiles.scale(10).index(1).compute(SIXTEEN_SQUARES_DOUBLES));
  }

  public void testScale_index_compute_longCollection() {
    assertQuantile(1, SIXTEEN_SQUARES_DECILE_1,
        Quantiles.scale(10).index(1).compute(SIXTEEN_SQUARES_LONGS));
  }

  public void testScale_index_compute_integerCollection() {
    assertQuantile(1, SIXTEEN_SQUARES_DECILE_1,
        Quantiles.scale(10).index(1).compute(SIXTEEN_SQUARES_INTEGERS));
  }

  public void testScale_index_compute_doubleVarargs() {
    double[] dataset = Doubles.toArray(SIXTEEN_SQUARES_DOUBLES);
    assertQuantile(1, SIXTEEN_SQUARES_DECILE_1, Quantiles.scale(10).index(1).compute(dataset));
    assertDatasetInOrder(SIXTEEN_SQUARES_DOUBLES, dataset);
  }

  public void testScale_index_compute_longVarargs() {
    long[] dataset = Longs.toArray(SIXTEEN_SQUARES_LONGS);
    assertQuantile(1, SIXTEEN_SQUARES_DECILE_1, Quantiles.scale(10).index(1).compute(dataset));
    assertDatasetInOrder(SIXTEEN_SQUARES_LONGS, dataset);
  }

  public void testScale_index_compute_intVarargs() {
    int[] dataset = Ints.toArray(SIXTEEN_SQUARES_INTEGERS);
    assertQuantile(1, SIXTEEN_SQUARES_DECILE_1, Quantiles.scale(10).index(1).compute(dataset));
    assertDatasetInOrder(SIXTEEN_SQUARES_INTEGERS, dataset);
  }

  public void testScale_index_computeInPlace() {
    double[] dataset = Doubles.toArray(SIXTEEN_SQUARES_DOUBLES);
    assertQuantile(1, SIXTEEN_SQUARES_DECILE_1,
        Quantiles.scale(10).index(1).computeInPlace(dataset));
    assertDatasetAnyOrder(SIXTEEN_SQUARES_DOUBLES, dataset);
  }

  public void testScale_index_computeInPlace_explicitVarargs() {
    assertQuantile(1, 45.6, Quantiles.scale(10).index(5).computeInPlace(78.9, 12.3, 45.6));
  }

  public void testScale_indexes_varargs_compute_doubleCollection() {
    ImmutableMap<Integer, Double> expected = ImmutableMap.of(
        0, SIXTEEN_SQUARES_MIN,
        10, SIXTEEN_SQUARES_MAX,
        5, SIXTEEN_SQUARES_MEDIAN,
        1, SIXTEEN_SQUARES_DECILE_1,
        8, SIXTEEN_SQUARES_DECILE_8
        );
    // Note that we specify index 1 twice, which by the method contract should be ignored.
    assertQuantilesMap(expected,
        Quantiles.scale(10).indexes(0, 10, 5, 1, 8, 1).compute(SIXTEEN_SQUARES_DOUBLES));
  }

  public void testScale_indexes_varargs_compute_doubleCollection_snapshotsIndexes() {
    // This test is the same as testScale_indexes_varargs_compute_doubleCollection except that the
    // array of indexes to be calculated is modified between the calls to indexes and compute: since
    // the contract is that it is snapshotted, this shouldn't make any difference to the result.
    ImmutableMap<Integer, Double> expected = ImmutableMap.of(
        0, SIXTEEN_SQUARES_MIN,
        10, SIXTEEN_SQUARES_MAX,
        5, SIXTEEN_SQUARES_MEDIAN,
        1, SIXTEEN_SQUARES_DECILE_1,
        8, SIXTEEN_SQUARES_DECILE_8
        );
    int[] indexes = { 0, 10, 5, 1, 8, 10 };
    ScaleAndIndexes intermediate = Quantiles.scale(10).indexes(indexes);
    indexes[0] = 3;
    assertQuantilesMap(expected, intermediate.compute(SIXTEEN_SQUARES_DOUBLES));
  }

  public void testScale_indexes_largeVarargs_compute_doubleCollection() {
    int scale = Integer.MAX_VALUE;
    int otherIndex = (Integer.MAX_VALUE - 1) / 3;  // this divides exactly
    // For the otherIndex calculation, we have q=Integer.MAX_VALUE, k=(Integer.MAX_VALUE-1)/3, and
    // N=16. Therefore k*(N-1)/q = 5-5/Integer.MAX_VALUE, which has floor 4 and fractional part
    // (1-5/Integer.MAX_VALUE).
    double otherValue = 16.0 * 5.0 / Integer.MAX_VALUE + 25.0 * (1.0 - 5.0 / Integer.MAX_VALUE);
    ImmutableMap<Integer, Double> expected = ImmutableMap.of(
        0, SIXTEEN_SQUARES_MIN,
        scale, SIXTEEN_SQUARES_MAX,
        otherIndex, otherValue
        );
    assertQuantilesMap(expected,
        Quantiles.scale(scale).indexes(0, scale, otherIndex).compute(SIXTEEN_SQUARES_DOUBLES));
  }

  public void testScale_indexes_varargs_compute_longCollection() {
    ImmutableMap<Integer, Double> expected = ImmutableMap.of(
        0, SIXTEEN_SQUARES_MIN,
        10, SIXTEEN_SQUARES_MAX,
        5, SIXTEEN_SQUARES_MEDIAN,
        1, SIXTEEN_SQUARES_DECILE_1,
        8, SIXTEEN_SQUARES_DECILE_8
        );
    // Note that we specify index 1 twice, which by the method contract should be ignored.
    assertQuantilesMap(expected,
        Quantiles.scale(10).indexes(0, 10, 5, 1, 8, 1).compute(SIXTEEN_SQUARES_LONGS));
  }

  public void testScale_indexes_varargs_compute_integerCollection() {
    ImmutableMap<Integer, Double> expected = ImmutableMap.of(
        0, SIXTEEN_SQUARES_MIN,
        10, SIXTEEN_SQUARES_MAX,
        5, SIXTEEN_SQUARES_MEDIAN,
        1, SIXTEEN_SQUARES_DECILE_1,
        8, SIXTEEN_SQUARES_DECILE_8
        );
    // Note that we specify index 1 twice, which by the method contract should be ignored.
    assertQuantilesMap(expected,
        Quantiles.scale(10).indexes(0, 10, 5, 1, 8, 1).compute(SIXTEEN_SQUARES_INTEGERS));
  }

  public void testScale_indexes_varargs_compute_doubleVarargs() {
    double[] dataset = Doubles.toArray(SIXTEEN_SQUARES_DOUBLES);
    ImmutableMap<Integer, Double> expected = ImmutableMap.of(
        0, SIXTEEN_SQUARES_MIN,
        10, SIXTEEN_SQUARES_MAX,
        5, SIXTEEN_SQUARES_MEDIAN,
        1, SIXTEEN_SQUARES_DECILE_1,
        8, SIXTEEN_SQUARES_DECILE_8
        );
    assertQuantilesMap(expected, Quantiles.scale(10).indexes(0, 10, 5, 1, 8, 1).compute(dataset));
    assertDatasetInOrder(SIXTEEN_SQUARES_DOUBLES, dataset);
  }

  public void testScale_indexes_varargs_compute_longVarargs() {
    long[] dataset = Longs.toArray(SIXTEEN_SQUARES_LONGS);
    ImmutableMap<Integer, Double> expected = ImmutableMap.of(
        0, SIXTEEN_SQUARES_MIN,
        10, SIXTEEN_SQUARES_MAX,
        5, SIXTEEN_SQUARES_MEDIAN,
        1, SIXTEEN_SQUARES_DECILE_1,
        8, SIXTEEN_SQUARES_DECILE_8
        );
    assertQuantilesMap(expected, Quantiles.scale(10).indexes(0, 10, 5, 1, 8, 1).compute(dataset));
    assertDatasetInOrder(SIXTEEN_SQUARES_LONGS, dataset);
  }

  public void testScale_indexes_varargs_compute_intVarargs() {
    int[] dataset = Ints.toArray(SIXTEEN_SQUARES_INTEGERS);
    ImmutableMap<Integer, Double> expected = ImmutableMap.of(
        0, SIXTEEN_SQUARES_MIN,
        10, SIXTEEN_SQUARES_MAX,
        5, SIXTEEN_SQUARES_MEDIAN,
        1, SIXTEEN_SQUARES_DECILE_1,
        8, SIXTEEN_SQUARES_DECILE_8
        );
    assertQuantilesMap(expected, Quantiles.scale(10).indexes(0, 10, 5, 1, 8, 1).compute(dataset));
    assertDatasetInOrder(SIXTEEN_SQUARES_INTEGERS, dataset);
  }

  public void testScale_indexes_varargs_computeInPlace() {
    double[] dataset = Doubles.toArray(SIXTEEN_SQUARES_DOUBLES);
    ImmutableMap<Integer, Double> expected = ImmutableMap.of(
        0, SIXTEEN_SQUARES_MIN,
        10, SIXTEEN_SQUARES_MAX,
        5, SIXTEEN_SQUARES_MEDIAN,
        1, SIXTEEN_SQUARES_DECILE_1,
        8, SIXTEEN_SQUARES_DECILE_8
        );
    assertQuantilesMap(expected,
        Quantiles.scale(10).indexes(0, 10, 5, 1, 8, 1).computeInPlace(dataset));
    assertDatasetAnyOrder(SIXTEEN_SQUARES_DOUBLES, dataset);
  }

  public void testScale_indexes_varargs_computeInPlace_explicitVarargs() {
    ImmutableMap<Integer, Double> expected = ImmutableMap.of(
        0, 12.3,
        10, 78.9
        );
    assertQuantilesMap(expected,
        Quantiles.scale(10).indexes(0, 10).computeInPlace(78.9, 12.3, 45.6));
  }

  public void testScale_indexes_collection_compute_doubleCollection() {
    ImmutableMap<Integer, Double> expected = ImmutableMap.of(
        0, SIXTEEN_SQUARES_MIN,
        10, SIXTEEN_SQUARES_MAX,
        5, SIXTEEN_SQUARES_MEDIAN,
        1, SIXTEEN_SQUARES_DECILE_1,
        8, SIXTEEN_SQUARES_DECILE_8
        );
    // Note that we specify index 1 twice, which by the method contract should be ignored.
    assertQuantilesMap(expected,
        Quantiles.scale(10).indexes(ImmutableList.of(0, 10, 5, 1, 8, 1))
            .compute(SIXTEEN_SQUARES_DOUBLES));
  }

  public void testScale_indexes_collection_computeInPlace() {
    double[] dataset = Doubles.toArray(SIXTEEN_SQUARES_DOUBLES);
    ImmutableMap<Integer, Double> expected = ImmutableMap.of(
        0, SIXTEEN_SQUARES_MIN,
        10, SIXTEEN_SQUARES_MAX,
        5, SIXTEEN_SQUARES_MEDIAN,
        1, SIXTEEN_SQUARES_DECILE_1,
        8, SIXTEEN_SQUARES_DECILE_8
        );
    assertQuantilesMap(expected,
        Quantiles.scale(10).indexes(ImmutableList.of(0, 10, 5, 1, 8, 1)).computeInPlace(dataset));
    assertDatasetAnyOrder(SIXTEEN_SQUARES_DOUBLES, dataset);
  }

  // 2. Tests on hardcoded datasets include non-finite values for chains starting with scale(10):

  private static final ImmutableList<Double> ONE_TO_FIVE_AND_POSITIVE_INFINITY =
      ImmutableList.of(3.0, 5.0, POSITIVE_INFINITY, 1.0, 4.0, 2.0);
  private static final ImmutableList<Double> ONE_TO_FIVE_AND_NEGATIVE_INFINITY =
      ImmutableList.of(3.0, 5.0, NEGATIVE_INFINITY, 1.0, 4.0, 2.0);
  private static final ImmutableList<Double> NEGATIVE_INFINITY_AND_FIVE_POSITIVE_INFINITIES =
      ImmutableList.of(POSITIVE_INFINITY, POSITIVE_INFINITY, NEGATIVE_INFINITY, POSITIVE_INFINITY,
          POSITIVE_INFINITY, POSITIVE_INFINITY);
  private static final ImmutableList<Double> ONE_TO_FIVE_AND_NAN =
      ImmutableList.of(3.0, 5.0, NaN, 1.0, 4.0, 2.0);

  public void testScale_indexes_varargs_compute_doubleCollection_positiveInfinity() {
    Map<Integer, Double> actual =
        Quantiles.scale(10).indexes(0, 1, 2, 8, 9, 10).compute(ONE_TO_FIVE_AND_POSITIVE_INFINITY);
    Map<Integer, Double> expected = ImmutableMap.<Integer, Double>builder()
        .put(0, 1.0)
        .put(1, 1.5)
        .put(2, 2.0)
        .put(8, 5.0)
        .put(9, POSITIVE_INFINITY)  // interpolating between 5.0 and POSITIVE_INFNINITY
        .put(10, POSITIVE_INFINITY)
        .build();
    assertQuantilesMap(expected, actual);
  }

  public void testScale_index_compute_doubleCollection_positiveInfinity() {
    assertQuantile(9, POSITIVE_INFINITY,  // interpolating between 5.0 and POSITIVE_INFNINITY
        Quantiles.scale(10).index(9).compute(ONE_TO_FIVE_AND_POSITIVE_INFINITY));
  }

  public void testScale_indexes_varargs_compute_doubleCollection_negativeInfinity() {
    Map<Integer, Double> actual =
        Quantiles.scale(10).indexes(0, 1, 2, 8, 9, 10).compute(ONE_TO_FIVE_AND_NEGATIVE_INFINITY);
    Map<Integer, Double> expected = ImmutableMap.<Integer, Double>builder()
        .put(0, NEGATIVE_INFINITY)
        .put(1, NEGATIVE_INFINITY)  // interpolating between NEGATIVE_INFNINITY and 1.0
        .put(2, 1.0)
        .put(8, 4.0)
        .put(9, 4.5)
        .put(10, 5.0)
        .build();
    assertQuantilesMap(expected, actual);
  }

  public void testScale_index_compute_doubleCollection_negativeInfinity() {
    assertQuantile(1, NEGATIVE_INFINITY,  // interpolating between NEGATIVE_INFNINITY and 1.0
        Quantiles.scale(10).index(1).compute(ONE_TO_FIVE_AND_NEGATIVE_INFINITY));
  }

  public void testScale_indexes_varargs_compute_doubleCollection_bothInfinities() {
    Map<Integer, Double> actual = Quantiles.scale(10).indexes(0, 1, 2, 8, 9, 10)
        .compute(NEGATIVE_INFINITY_AND_FIVE_POSITIVE_INFINITIES);
    Map<Integer, Double> expected = ImmutableMap.<Integer, Double>builder()
        .put(0, NEGATIVE_INFINITY)
        .put(1, NaN)  // interpolating between NEGATIVE_ and POSITIVE_INFINITY values
        .put(2, POSITIVE_INFINITY)
        .put(8, POSITIVE_INFINITY)
        .put(9, POSITIVE_INFINITY)  // interpolating between two POSITIVE_INFINITY values
        .put(10, POSITIVE_INFINITY)
        .build();
    assertQuantilesMap(expected, actual);
  }

  public void testScale_indexes_varargs_compute_doubleCollection_nan() {
    Map<Integer, Double> actual =
        Quantiles.scale(10).indexes(0, 1, 2, 8, 9, 10).compute(ONE_TO_FIVE_AND_NAN);
    Map<Integer, Double> expected = ImmutableMap.<Integer, Double>builder()
        .put(0, NaN)
        .put(1, NaN)
        .put(2, NaN)
        .put(8, NaN)
        .put(9, NaN)
        .put(10, NaN)
        .build();
    assertQuantilesMap(expected, actual);
  }

  public void testScale_index_compute_doubleCollection_nan() {
    assertQuantile(5, NaN, Quantiles.scale(10).index(5).compute(ONE_TO_FIVE_AND_NAN));
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
      double expected = expectedLargeDatasetPercentile(index);
      assertQuantile(index, expected, percentiles().index(index).compute(PSEUDORANDOM_DATASET));
    }
  }

  @AndroidIncompatible // slow
  public void testPercentiles_index_computeInPlace() {
    for (int index = 0; index <= 100; index++) {
      double[] dataset = Doubles.toArray(PSEUDORANDOM_DATASET);
      double expected = expectedLargeDatasetPercentile(index);
      assertQuantile(index, expected, percentiles().index(index).computeInPlace(dataset));
      assertDatasetAnyOrder(PSEUDORANDOM_DATASET, dataset);
    }
  }

  public void testPercentiles_indexes_varargsPairs_compute_doubleCollection() {
    for (int index1 = 0; index1 <= 100; index1++) {
      for (int index2 = 0; index2 <= 100; index2++) {
        ImmutableMap.Builder<Integer, Double> expectedBuilder = ImmutableMap.builder();
        expectedBuilder.put(index1, expectedLargeDatasetPercentile(index1));
        if (index2 != index1) {
          expectedBuilder.put(index2, expectedLargeDatasetPercentile(index2));
        }
        ImmutableMap<Integer, Double> expected = expectedBuilder.build();
        assertQuantilesMap(expected,
            percentiles().indexes(index1, index2).compute(PSEUDORANDOM_DATASET));
      }
    }
  }

  public void testPercentiles_indexes_varargsAll_compute_doubleCollection() {
    ArrayList<Integer> indexes = new ArrayList<Integer>(); 
    ImmutableMap.Builder<Integer, Double> expectedBuilder = ImmutableMap.builder();
    for (int index = 0; index <= 100; index++) {
      indexes.add(index);
      expectedBuilder.put(index, expectedLargeDatasetPercentile(index));
    }
    Random random = new Random(770683168895677741L);
    Collections.shuffle(indexes, random);
    assertQuantilesMap(expectedBuilder.build(),
        percentiles().indexes(Ints.toArray(indexes)).compute(PSEUDORANDOM_DATASET));
  }

  public void testPercentiles_indexes_varargsAll_computeInPlace() {
    double[] dataset = Doubles.toArray(PSEUDORANDOM_DATASET);
    List<Integer> indexes = new ArrayList<Integer>(); 
    ImmutableMap.Builder<Integer, Double> expectedBuilder = ImmutableMap.builder();
    for (int index = 0; index <= 100; index++) {
      indexes.add(index);
      expectedBuilder.put(index, expectedLargeDatasetPercentile(index));
    }
    Random random = new Random(770683168895677741L);
    Collections.shuffle(indexes, random);
    assertQuantilesMap(expectedBuilder.build(),
        percentiles().indexes(Ints.toArray(indexes)).computeInPlace(dataset));
    assertDatasetAnyOrder(PSEUDORANDOM_DATASET, dataset);
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

  /**
   * Assests that the actual quantile value returned matches the expected value, allowing
   * the margin of error {@link #ALLOWED_ERROR}. The assertion passes when the expected and actual
   * values are the same non-finite value.
   */
  private static void assertQuantile(int index, double expected, double actual) {
    if (expected == POSITIVE_INFINITY) {
      assertThat(actual).named("quantile at index " + index).isPositiveInfinity();
    } else if (expected == NEGATIVE_INFINITY) {
      assertThat(actual).named("quantile at index " + index).isNegativeInfinity();
    } else if (Double.isNaN(expected)) {
      assertThat(actual).named("quantile at index " + index).isNaN();
    } else {
      assertThat(actual).named("quantile at index " + index).isWithin(ALLOWED_ERROR).of(expected);
    }
  }

  /**
   * Assests that the actual map of quantile values returned matches the expected map, allowing
   * the margin of error {@link #ALLOWED_ERROR} on the values. The assertion passes when the
   * expected and actual values are the same non-finite value.
   */
  private static void assertQuantilesMap(
      Map<Integer, Double> expected, Map<Integer, Double> actual) {
    assertThat(actual.keySet()).isEqualTo(expected.keySet());
    for (int index : expected.keySet()) {
      assertQuantile(index, expected.get(index), actual.get(index));
    }
  }

  /**
   * Asserts that the actual dataset, as a double array, has the expected values, although not
   * necessarily in the same order. This tests the contract on the {@code computeInPlace} methods,
   * which may reorder the dataset.
   */
  private static void assertDatasetAnyOrder(Iterable<Double> expected, double[] actual) {
    assertThat(Doubles.asList(actual)).containsExactlyElementsIn(expected);
  }

  /**
   * Asserts that the actual dataset, as a double array, has the expected values, in the expected
   * order. This tests the contract on the {@code compute} methods, which may not mutate the
   * dataset.
   */
  private static void assertDatasetInOrder(Collection<Double> expected, double[] actual) {
    assertThat(actual).hasValuesWithin(0.0).of(Doubles.toArray(expected));
  }

  /**
   * Asserts that the actual dataset, as a long array, has the expected values, in the expected
   * order. This tests the contract on the {@code compute} methods, which may not mutate the
   * dataset.
   */
  private static void assertDatasetInOrder(Iterable<Long> expected, long[] actual) {
    assertThat(actual).asList().isEqualTo(expected);
  }

  /**
   * Asserts that the actual dataset, as an int array, has the expected values, in the expected
   * order. This tests the contract on the {@code compute} methods, which may not mutate the
   * dataset.
   */
  private static void assertDatasetInOrder(Iterable<Integer> expected, int[] actual) {
    assertThat(actual).asList().isEqualTo(expected);
  }
}
