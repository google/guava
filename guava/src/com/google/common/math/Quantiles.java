/*
 * Copyright (C) 2014 The Guava Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.common.math;

import static com.google.common.base.Preconditions.checkArgument;
import static java.lang.Double.NEGATIVE_INFINITY;
import static java.lang.Double.NaN;
import static java.lang.Double.POSITIVE_INFINITY;
import static java.util.Arrays.sort;
import static java.util.Collections.unmodifiableMap;

import com.google.common.annotations.Beta;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.primitives.Doubles;
import com.google.common.primitives.Ints;
import java.math.RoundingMode;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Provides a fluent API for calculating <a
 * href="http://en.wikipedia.org/wiki/Quantile">quantiles</a>.
 *
 * <h3>Examples</h3>
 *
 * <p>To compute the median:
 *
 * <pre>{@code
 * double myMedian = median().compute(myDataset);
 * }</pre>
 *
 * where {@link #median()} has been statically imported.
 *
 * <p>To compute the 99th percentile:
 *
 * <pre>{@code
 * double myPercentile99 = percentiles().index(99).compute(myDataset);
 * }</pre>
 *
 * where {@link #percentiles()} has been statically imported.
 *
 * <p>To compute median and the 90th and 99th percentiles:
 *
 * <pre>{@code
 * Map<Integer, Double> myPercentiles =
 *     percentiles().indexes(50, 90, 99).compute(myDataset);
 * }</pre>
 *
 * where {@link #percentiles()} has been statically imported: {@code myPercentiles} maps the keys
 * 50, 90, and 99, to their corresponding quantile values.
 *
 * <p>To compute quartiles, use {@link #quartiles()} instead of {@link #percentiles()}. To compute
 * arbitrary q-quantiles, use {@link #scale scale(q)}.
 *
 * <p>These examples all take a copy of your dataset. If you have a double array, you are okay with
 * it being arbitrarily reordered, and you want to avoid that copy, you can use {@code
 * computeInPlace} instead of {@code compute}.
 *
 * <h3>Definition and notes on interpolation</h3>
 *
 * <p>The definition of the kth q-quantile of N values is as follows: define x = k * (N - 1) / q; if
 * x is an integer, the result is the value which would appear at index x in the sorted dataset
 * (unless there are {@link Double#NaN NaN} values, see below); otherwise, the result is the average
 * of the values which would appear at the indexes floor(x) and ceil(x) weighted by (1-frac(x)) and
 * frac(x) respectively. This is the same definition as used by Excel and by S, it is the Type 7
 * definition in <a
 * href="http://stat.ethz.ch/R-manual/R-devel/library/stats/html/quantile.html">R</a>, and it is
 * described by <a
 * href="http://en.wikipedia.org/wiki/Quantile#Estimating_the_quantiles_of_a_population">
 * wikipedia</a> as providing "Linear interpolation of the modes for the order statistics for the
 * uniform distribution on [0,1]."
 *
 * <h3>Handling of non-finite values</h3>
 *
 * <p>If any values in the input are {@link Double#NaN NaN} then all values returned are {@link
 * Double#NaN NaN}. (This is the one occasion when the behaviour is not the same as you'd get from
 * sorting with {@link java.util.Arrays#sort(double[]) Arrays.sort(double[])} or {@link
 * java.util.Collections#sort(java.util.List) Collections.sort(List&lt;Double&gt;)} and selecting
 * the required value(s). Those methods would sort {@link Double#NaN NaN} as if it is greater than
 * any other value and place them at the end of the dataset, even after {@link
 * Double#POSITIVE_INFINITY POSITIVE_INFINITY}.)
 *
 * <p>Otherwise, {@link Double#NEGATIVE_INFINITY NEGATIVE_INFINITY} and {@link
 * Double#POSITIVE_INFINITY POSITIVE_INFINITY} sort to the beginning and the end of the dataset, as
 * you would expect.
 *
 * <p>If required to do a weighted average between an infinity and a finite value, or between an
 * infinite value and itself, the infinite value is returned. If required to do a weighted average
 * between {@link Double#NEGATIVE_INFINITY NEGATIVE_INFINITY} and {@link Double#POSITIVE_INFINITY
 * POSITIVE_INFINITY}, {@link Double#NaN NaN} is returned (note that this will only happen if the
 * dataset contains no finite values).
 *
 * <h3>Performance</h3>
 *
 * <p>The average time complexity of the computation is O(N) in the size of the dataset. There is a
 * worst case time complexity of O(N^2). You are extremely unlikely to hit this quadratic case on
 * randomly ordered data (the probability decreases faster than exponentially in N), but if you are
 * passing in unsanitized user data then a malicious user could force it. A light shuffle of the
 * data using an unpredictable seed should normally be enough to thwart this attack.
 *
 * <p>The time taken to compute multiple quantiles on the same dataset using {@link Scale#indexes
 * indexes} is generally less than the total time taken to compute each of them separately, and
 * sometimes much less. For example, on a large enough dataset, computing the 90th and 99th
 * percentiles together takes about 55% as long as computing them separately.
 *
 * <p>When calling {@link ScaleAndIndex#compute} (in {@linkplain ScaleAndIndexes#compute either
 * form}), the memory requirement is 8*N bytes for the copy of the dataset plus an overhead which is
 * independent of N (but depends on the quantiles being computed). When calling {@link
 * ScaleAndIndex#computeInPlace computeInPlace} (in {@linkplain ScaleAndIndexes#computeInPlace
 * either form}), only the overhead is required. The number of object allocations is independent of
 * N in both cases.
 *
 * @author Pete Gillin
 * @since 20.0
 */
@Beta
@GwtIncompatible
public final class Quantiles {

  /** Specifies the computation of a median (i.e. the 1st 2-quantile). */
  public static ScaleAndIndex median() {
    return scale(2).index(1);
  }

  /** Specifies the computation of quartiles (i.e. 4-quantiles). */
  public static Scale quartiles() {
    return scale(4);
  }

  /** Specifies the computation of percentiles (i.e. 100-quantiles). */
  public static Scale percentiles() {
    return scale(100);
  }

  /**
   * Specifies the computation of q-quantiles.
   *
   * @param scale the scale for the quantiles to be calculated, i.e. the q of the q-quantiles, which
   *     must be positive
   */
  public static Scale scale(int scale) {
    return new Scale(scale);
  }

  /**
   * Describes the point in a fluent API chain where only the scale (i.e. the q in q-quantiles) has
   * been specified.
   *
   * @since 20.0
   */
  public static final class Scale {

    private final int scale;

    private Scale(int scale) {
      checkArgument(scale > 0, "Quantile scale must be positive");
      this.scale = scale;
    }

    /**
     * Specifies a single quantile index to be calculated, i.e. the k in the kth q-quantile.
     *
     * @param index the quantile index, which must be in the inclusive range [0, q] for q-quantiles
     */
    public ScaleAndIndex index(int index) {
      return new ScaleAndIndex(scale, index);
    }

    /**
     * Specifies multiple quantile indexes to be calculated, each index being the k in the kth
     * q-quantile.
     *
     * @param indexes the quantile indexes, each of which must be in the inclusive range [0, q] for
     *     q-quantiles; the order of the indexes is unimportant, duplicates will be ignored, and the
     *     set will be snapshotted when this method is called
     */
    public ScaleAndIndexes indexes(int... indexes) {
      return new ScaleAndIndexes(scale, indexes.clone());
    }

    /**
     * Specifies multiple quantile indexes to be calculated, each index being the k in the kth
     * q-quantile.
     *
     * @param indexes the quantile indexes, each of which must be in the inclusive range [0, q] for
     *     q-quantiles; the order of the indexes is unimportant, duplicates will be ignored, and the
     *     set will be snapshotted when this method is called
     */
    public ScaleAndIndexes indexes(Collection<Integer> indexes) {
      return new ScaleAndIndexes(scale, Ints.toArray(indexes));
    }
  }

  /**
   * Describes the point in a fluent API chain where the scale and a single quantile index (i.e. the
   * q and the k in the kth q-quantile) have been specified.
   *
   * @since 20.0
   */
  public static final class ScaleAndIndex {

    private final int scale;
    private final int index;

    private ScaleAndIndex(int scale, int index) {
      checkIndex(index, scale);
      this.scale = scale;
      this.index = index;
    }

    /**
     * Computes the quantile value of the given dataset.
     *
     * @param dataset the dataset to do the calculation on, which must be non-empty, which will be
     *     cast to doubles (with any associated lost of precision), and which will not be mutated by
     *     this call (it is copied instead)
     * @return the quantile value
     */
    public double compute(Collection<? extends Number> dataset) {
      return computeInPlace(Doubles.toArray(dataset));
    }

    /**
     * Computes the quantile value of the given dataset.
     *
     * @param dataset the dataset to do the calculation on, which must be non-empty, which will not
     *     be mutated by this call (it is copied instead)
     * @return the quantile value
     */
    public double compute(double... dataset) {
      return computeInPlace(dataset.clone());
    }

    /**
     * Computes the quantile value of the given dataset.
     *
     * @param dataset the dataset to do the calculation on, which must be non-empty, which will be
     *     cast to doubles (with any associated lost of precision), and which will not be mutated by
     *     this call (it is copied instead)
     * @return the quantile value
     */
    public double compute(long... dataset) {
      return computeInPlace(longsToDoubles(dataset));
    }

    /**
     * Computes the quantile value of the given dataset.
     *
     * @param dataset the dataset to do the calculation on, which must be non-empty, which will be
     *     cast to doubles, and which will not be mutated by this call (it is copied instead)
     * @return the quantile value
     */
    public double compute(int... dataset) {
      return computeInPlace(intsToDoubles(dataset));
    }

    /**
     * Computes the quantile value of the given dataset, performing the computation in-place.
     *
     * @param dataset the dataset to do the calculation on, which must be non-empty, and which will
     *     be arbitrarily reordered by this method call
     * @return the quantile value
     */
    public double computeInPlace(double... dataset) {
      checkArgument(dataset.length > 0, "Cannot calculate quantiles of an empty dataset");
      if (containsNaN(dataset)) {
        return NaN;
      }

      // Calculate the quotient and remainder in the integer division x = k * (N-1) / q, i.e.
      // index * (dataset.length - 1) / scale. If there is no remainder, we can just find the value
      // whose index in the sorted dataset equals the quotient; if there is a remainder, we
      // interpolate between that and the next value.

      // Since index and (dataset.length - 1) are non-negative ints, their product can be expressed
      // as a long, without risk of overflow:
      long numerator = (long) index * (dataset.length - 1);
      // Since scale is a positive int, index is in [0, scale], and (dataset.length - 1) is a
      // non-negative int, we can do long-arithmetic on index * (dataset.length - 1) / scale to get
      // a rounded ratio and a remainder which can be expressed as ints, without risk of overflow:
      int quotient = (int) LongMath.divide(numerator, scale, RoundingMode.DOWN);
      int remainder = (int) (numerator - (long) quotient * scale);
      selectInPlace(quotient, dataset, 0, dataset.length - 1);
      if (remainder == 0) {
        return dataset[quotient];
      } else {
        selectInPlace(quotient + 1, dataset, quotient + 1, dataset.length - 1);
        return interpolate(dataset[quotient], dataset[quotient + 1], remainder, scale);
      }
    }
  }

  /**
   * Describes the point in a fluent API chain where the scale and a multiple quantile indexes (i.e.
   * the q and a set of values for the k in the kth q-quantile) have been specified.
   *
   * @since 20.0
   */
  public static final class ScaleAndIndexes {

    private final int scale;
    private final int[] indexes;

    private ScaleAndIndexes(int scale, int[] indexes) {
      for (int index : indexes) {
        checkIndex(index, scale);
      }
      this.scale = scale;
      this.indexes = indexes;
    }

    /**
     * Computes the quantile values of the given dataset.
     *
     * @param dataset the dataset to do the calculation on, which must be non-empty, which will be
     *     cast to doubles (with any associated lost of precision), and which will not be mutated by
     *     this call (it is copied instead)
     * @return an unmodifiable map of results: the keys will be the specified quantile indexes, and
     *     the values the corresponding quantile values
     */
    public Map<Integer, Double> compute(Collection<? extends Number> dataset) {
      return computeInPlace(Doubles.toArray(dataset));
    }

    /**
     * Computes the quantile values of the given dataset.
     *
     * @param dataset the dataset to do the calculation on, which must be non-empty, which will not
     *     be mutated by this call (it is copied instead)
     * @return an unmodifiable map of results: the keys will be the specified quantile indexes, and
     *     the values the corresponding quantile values
     */
    public Map<Integer, Double> compute(double... dataset) {
      return computeInPlace(dataset.clone());
    }

    /**
     * Computes the quantile values of the given dataset.
     *
     * @param dataset the dataset to do the calculation on, which must be non-empty, which will be
     *     cast to doubles (with any associated lost of precision), and which will not be mutated by
     *     this call (it is copied instead)
     * @return an unmodifiable map of results: the keys will be the specified quantile indexes, and
     *     the values the corresponding quantile values
     */
    public Map<Integer, Double> compute(long... dataset) {
      return computeInPlace(longsToDoubles(dataset));
    }

    /**
     * Computes the quantile values of the given dataset.
     *
     * @param dataset the dataset to do the calculation on, which must be non-empty, which will be
     *     cast to doubles, and which will not be mutated by this call (it is copied instead)
     * @return an unmodifiable map of results: the keys will be the specified quantile indexes, and
     *     the values the corresponding quantile values
     */
    public Map<Integer, Double> compute(int... dataset) {
      return computeInPlace(intsToDoubles(dataset));
    }

    /**
     * Computes the quantile values of the given dataset, performing the computation in-place.
     *
     * @param dataset the dataset to do the calculation on, which must be non-empty, and which will
     *     be arbitrarily reordered by this method call
     * @return an unmodifiable map of results: the keys will be the specified quantile indexes, and
     *     the values the corresponding quantile values
     */
    public Map<Integer, Double> computeInPlace(double... dataset) {
      checkArgument(dataset.length > 0, "Cannot calculate quantiles of an empty dataset");
      if (containsNaN(dataset)) {
        Map<Integer, Double> nanMap = new HashMap<>();
        for (int index : indexes) {
          nanMap.put(index, NaN);
        }
        return unmodifiableMap(nanMap);
      }

      // Calculate the quotients and remainders in the integer division x = k * (N - 1) / q, i.e.
      // index * (dataset.length - 1) / scale for each index in indexes. For each, if there is no
      // remainder, we can just select the value whose index in the sorted dataset equals the
      // quotient; if there is a remainder, we interpolate between that and the next value.

      int[] quotients = new int[indexes.length];
      int[] remainders = new int[indexes.length];
      // The indexes to select. In the worst case, we'll need one each side of each quantile.
      int[] requiredSelections = new int[indexes.length * 2];
      int requiredSelectionsCount = 0;
      for (int i = 0; i < indexes.length; i++) {
        // Since index and (dataset.length - 1) are non-negative ints, their product can be
        // expressed as a long, without risk of overflow:
        long numerator = (long) indexes[i] * (dataset.length - 1);
        // Since scale is a positive int, index is in [0, scale], and (dataset.length - 1) is a
        // non-negative int, we can do long-arithmetic on index * (dataset.length - 1) / scale to
        // get a rounded ratio and a remainder which can be expressed as ints, without risk of
        // overflow:
        int quotient = (int) LongMath.divide(numerator, scale, RoundingMode.DOWN);
        int remainder = (int) (numerator - (long) quotient * scale);
        quotients[i] = quotient;
        remainders[i] = remainder;
        requiredSelections[requiredSelectionsCount] = quotient;
        requiredSelectionsCount++;
        if (remainder != 0) {
          requiredSelections[requiredSelectionsCount] = quotient + 1;
          requiredSelectionsCount++;
        }
      }
      sort(requiredSelections, 0, requiredSelectionsCount);
      selectAllInPlace(
          requiredSelections, 0, requiredSelectionsCount - 1, dataset, 0, dataset.length - 1);
      Map<Integer, Double> ret = new HashMap<>();
      for (int i = 0; i < indexes.length; i++) {
        int quotient = quotients[i];
        int remainder = remainders[i];
        if (remainder == 0) {
          ret.put(indexes[i], dataset[quotient]);
        } else {
          ret.put(
              indexes[i], interpolate(dataset[quotient], dataset[quotient + 1], remainder, scale));
        }
      }
      return unmodifiableMap(ret);
    }
  }

  /** Returns whether any of the values in {@code dataset} are {@code NaN}. */
  private static boolean containsNaN(double... dataset) {
    for (double value : dataset) {
      if (Double.isNaN(value)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Returns a value a fraction {@code (remainder / scale)} of the way between {@code lower} and
   * {@code upper}. Assumes that {@code lower <= upper}. Correctly handles infinities (but not
   * {@code NaN}).
   */
  private static double interpolate(double lower, double upper, double remainder, double scale) {
    if (lower == NEGATIVE_INFINITY) {
      if (upper == POSITIVE_INFINITY) {
        // Return NaN when lower == NEGATIVE_INFINITY and upper == POSITIVE_INFINITY:
        return NaN;
      }
      // Return NEGATIVE_INFINITY when NEGATIVE_INFINITY == lower <= upper < POSITIVE_INFINITY:
      return NEGATIVE_INFINITY;
    }
    if (upper == POSITIVE_INFINITY) {
      // Return POSITIVE_INFINITY when NEGATIVE_INFINITY < lower <= upper == POSITIVE_INFINITY:
      return POSITIVE_INFINITY;
    }
    return lower + (upper - lower) * remainder / scale;
  }

  private static void checkIndex(int index, int scale) {
    if (index < 0 || index > scale) {
      throw new IllegalArgumentException(
          "Quantile indexes must be between 0 and the scale, which is " + scale);
    }
  }

  private static double[] longsToDoubles(long[] longs) {
    int len = longs.length;
    double[] doubles = new double[len];
    for (int i = 0; i < len; i++) {
      doubles[i] = longs[i];
    }
    return doubles;
  }

  private static double[] intsToDoubles(int[] ints) {
    int len = ints.length;
    double[] doubles = new double[len];
    for (int i = 0; i < len; i++) {
      doubles[i] = ints[i];
    }
    return doubles;
  }

  /**
   * Performs an in-place selection to find the element which would appear at a given index in a
   * dataset if it were sorted. The following preconditions should hold:
   *
   * <ul>
   *   <li>{@code required}, {@code from}, and {@code to} should all be indexes into {@code array};
   *   <li>{@code required} should be in the range [{@code from}, {@code to}];
   *   <li>all the values with indexes in the range [0, {@code from}) should be less than or equal
   *       to all the values with indexes in the range [{@code from}, {@code to}];
   *   <li>all the values with indexes in the range ({@code to}, {@code array.length - 1}] should be
   *       greater than or equal to all the values with indexes in the range [{@code from}, {@code
   *       to}].
   * </ul>
   *
   * This method will reorder the values with indexes in the range [{@code from}, {@code to}] such
   * that all the values with indexes in the range [{@code from}, {@code required}) are less than or
   * equal to the value with index {@code required}, and all the values with indexes in the range
   * ({@code required}, {@code to}] are greater than or equal to that value. Therefore, the value at
   * {@code required} is the value which would appear at that index in the sorted dataset.
   */
  private static void selectInPlace(int required, double[] array, int from, int to) {
    // If we are looking for the least element in the range, we can just do a linear search for it.
    // (We will hit this whenever we are doing quantile interpolation: our first selection finds
    // the lower value, our second one finds the upper value by looking for the next least element.)
    if (required == from) {
      int min = from;
      for (int index = from + 1; index <= to; index++) {
        if (array[min] > array[index]) {
          min = index;
        }
      }
      if (min != from) {
        swap(array, min, from);
      }
      return;
    }

    // Let's play quickselect! We'll repeatedly partition the range [from, to] containing the
    // required element, as long as it has more than one element.
    while (to > from) {
      int partitionPoint = partition(array, from, to);
      if (partitionPoint >= required) {
        to = partitionPoint - 1;
      }
      if (partitionPoint <= required) {
        from = partitionPoint + 1;
      }
    }
  }

  /**
   * Performs a partition operation on the slice of {@code array} with elements in the range [{@code
   * from}, {@code to}]. Uses the median of {@code from}, {@code to}, and the midpoint between them
   * as a pivot. Returns the index which the slice is partitioned around, i.e. if it returns {@code
   * ret} then we know that the values with indexes in [{@code from}, {@code ret}) are less than or
   * equal to the value at {@code ret} and the values with indexes in ({@code ret}, {@code to}] are
   * greater than or equal to that.
   */
  private static int partition(double[] array, int from, int to) {
    // Select a pivot, and move it to the start of the slice i.e. to index from.
    movePivotToStartOfSlice(array, from, to);
    double pivot = array[from];

    // Move all elements with indexes in (from, to] which are greater than the pivot to the end of
    // the array. Keep track of where those elements begin.
    int partitionPoint = to;
    for (int i = to; i > from; i--) {
      if (array[i] > pivot) {
        swap(array, partitionPoint, i);
        partitionPoint--;
      }
    }

    // We now know that all elements with indexes in (from, partitionPoint] are less than or equal
    // to the pivot at from, and all elements with indexes in (partitionPoint, to] are greater than
    // it. We swap the pivot into partitionPoint and we know the array is partitioned around that.
    swap(array, from, partitionPoint);
    return partitionPoint;
  }

  /**
   * Selects the pivot to use, namely the median of the values at {@code from}, {@code to}, and
   * halfway between the two (rounded down), from {@code array}, and ensure (by swapping elements if
   * necessary) that that pivot value appears at the start of the slice i.e. at {@code from}.
   * Expects that {@code from} is strictly less than {@code to}.
   */
  private static void movePivotToStartOfSlice(double[] array, int from, int to) {
    int mid = (from + to) >>> 1;
    // We want to make a swap such that either array[to] <= array[from] <= array[mid], or
    // array[mid] <= array[from] <= array[to]. We know that from < to, so we know mid < to
    // (although it's possible that mid == from, if to == from + 1). Note that the postcondition
    // would be impossible to fulfil if mid == to unless we also have array[from] == array[to].
    boolean toLessThanMid = (array[to] < array[mid]);
    boolean midLessThanFrom = (array[mid] < array[from]);
    boolean toLessThanFrom = (array[to] < array[from]);
    if (toLessThanMid == midLessThanFrom) {
      // Either array[to] < array[mid] < array[from] or array[from] <= array[mid] <= array[to].
      swap(array, mid, from);
    } else if (toLessThanMid != toLessThanFrom) {
      // Either array[from] <= array[to] < array[mid] or array[mid] <= array[to] < array[from].
      swap(array, from, to);
    }
    // The postcondition now holds. So the median, our chosen pivot, is at from.
  }

  /**
   * Performs an in-place selection, like {@link #selectInPlace}, to select all the indexes {@code
   * allRequired[i]} for {@code i} in the range [{@code requiredFrom}, {@code requiredTo}]. These
   * indexes must be sorted in the array and must all be in the range [{@code from}, {@code to}].
   */
  private static void selectAllInPlace(
      int[] allRequired, int requiredFrom, int requiredTo, double[] array, int from, int to) {
    // Choose the first selection to do...
    int requiredChosen = chooseNextSelection(allRequired, requiredFrom, requiredTo, from, to);
    int required = allRequired[requiredChosen];

    // ...do the first selection...
    selectInPlace(required, array, from, to);

    // ...then recursively perform the selections in the range below...
    int requiredBelow = requiredChosen - 1;
    while (requiredBelow >= requiredFrom && allRequired[requiredBelow] == required) {
      requiredBelow--; // skip duplicates of required in the range below
    }
    if (requiredBelow >= requiredFrom) {
      selectAllInPlace(allRequired, requiredFrom, requiredBelow, array, from, required - 1);
    }

    // ...and then recursively perform the selections in the range above.
    int requiredAbove = requiredChosen + 1;
    while (requiredAbove <= requiredTo && allRequired[requiredAbove] == required) {
      requiredAbove++; // skip duplicates of required in the range above
    }
    if (requiredAbove <= requiredTo) {
      selectAllInPlace(allRequired, requiredAbove, requiredTo, array, required + 1, to);
    }
  }

  /**
   * Chooses the next selection to do from the required selections. It is required that the array
   * {@code allRequired} is sorted and that {@code allRequired[i]} are in the range [{@code from},
   * {@code to}] for all {@code i} in the range [{@code requiredFrom}, {@code requiredTo}]. The
   * value returned by this method is the {@code i} in that range such that {@code allRequired[i]}
   * is as close as possible to the center of the range [{@code from}, {@code to}]. Choosing the
   * value closest to the center of the range first is the most efficient strategy because it
   * minimizes the size of the subranges from which the remaining selections must be done.
   */
  private static int chooseNextSelection(
      int[] allRequired, int requiredFrom, int requiredTo, int from, int to) {
    if (requiredFrom == requiredTo) {
      return requiredFrom; // only one thing to choose, so choose it
    }

    // Find the center and round down. The true center is either centerFloor or halfway between
    // centerFloor and centerFloor + 1.
    int centerFloor = (from + to) >>> 1;

    // Do a binary search until we're down to the range of two which encloses centerFloor (unless
    // all values are lower or higher than centerFloor, in which case we find the two highest or
    // lowest respectively). If centerFloor is in allRequired, we will definitely find it. If not,
    // but centerFloor + 1 is, we'll definitely find that. The closest value to the true (unrounded)
    // center will be at either low or high.
    int low = requiredFrom;
    int high = requiredTo;
    while (high > low + 1) {
      int mid = (low + high) >>> 1;
      if (allRequired[mid] > centerFloor) {
        high = mid;
      } else if (allRequired[mid] < centerFloor) {
        low = mid;
      } else {
        return mid; // allRequired[mid] = centerFloor, so we can't get closer than that
      }
    }

    // Now pick the closest of the two candidates. Note that there is no rounding here.
    if (from + to - allRequired[low] - allRequired[high] > 0) {
      return high;
    } else {
      return low;
    }
  }

  /** Swaps the values at {@code i} and {@code j} in {@code array}. */
  private static void swap(double[] array, int i, int j) {
    double temp = array[i];
    array[i] = array[j];
    array[j] = temp;
  }
}
