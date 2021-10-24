/*
 * Copyright (C) 2012 The Guava Authors
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
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.math.DoubleUtils.ensureNonNegative;
import static com.google.common.math.StatsAccumulator.calculateNewMeanNonFinite;
import static com.google.common.primitives.Doubles.isFinite;
import static java.lang.Double.NaN;
import static java.lang.Double.doubleToLongBits;
import static java.lang.Double.isNaN;

import com.google.common.annotations.Beta;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Iterator;
import javax.annotation.CheckForNull;

/**
 * A bundle of statistical summary values -- sum, count, mean/average, min and max, and several
 * forms of variance -- that were computed from a single set of zero or more floating-point values.
 *
 * <p>There are two ways to obtain a {@code Stats} instance:
 *
 * <ul>
 *   <li>If all the values you want to summarize are already known, use the appropriate {@code
 *       Stats.of} factory method below. Primitive arrays, iterables and iterators of any kind of
 *       {@code Number}, and primitive varargs are supported.
 *   <li>Or, to avoid storing up all the data first, create a {@link StatsAccumulator} instance,
 *       feed values to it as you get them, then call {@link StatsAccumulator#snapshot}.
 * </ul>
 *
 * <p>Static convenience methods called {@code meanOf} are also provided for users who wish to
 * calculate <i>only</i> the mean.
 *
 * <p><b>Java 8 users:</b> If you are not using any of the variance statistics, you may wish to use
 * built-in JDK libraries instead of this class.
 *
 * @author Pete Gillin
 * @author Kevin Bourrillion
 * @since 20.0
 */
@Beta
@GwtIncompatible
@ElementTypesAreNonnullByDefault
public final class Stats implements Serializable {

  private final long count;
  private final double mean;
  private final double sumOfSquaresOfDeltas;
  private final double min;
  private final double max;

  /**
   * Internal constructor. Users should use {@link #of} or {@link StatsAccumulator#snapshot}.
   *
   * <p>To ensure that the created instance obeys its contract, the parameters should satisfy the
   * following constraints. This is the callers responsibility and is not enforced here.
   *
   * <ul>
   *   <li>If {@code count} is 0, {@code mean} may have any finite value (its only usage will be to
   *       get multiplied by 0 to calculate the sum), and the other parameters may have any values
   *       (they will not be used).
   *   <li>If {@code count} is 1, {@code sumOfSquaresOfDeltas} must be exactly 0.0 or {@link
   *       Double#NaN}.
   * </ul>
   */
  Stats(long count, double mean, double sumOfSquaresOfDeltas, double min, double max) {
    this.count = count;
    this.mean = mean;
    this.sumOfSquaresOfDeltas = sumOfSquaresOfDeltas;
    this.min = min;
    this.max = max;
  }

  /**
   * Returns statistics over a dataset containing the given values.
   *
   * @param values a series of values, which will be converted to {@code double} values (this may
   *     cause loss of precision)
   */
  public static Stats of(Iterable<? extends Number> values) {
    StatsAccumulator accumulator = new StatsAccumulator();
    accumulator.addAll(values);
    return accumulator.snapshot();
  }

  /**
   * Returns statistics over a dataset containing the given values. The iterator will be completely
   * consumed by this method.
   *
   * @param values a series of values, which will be converted to {@code double} values (this may
   *     cause loss of precision)
   */
  public static Stats of(Iterator<? extends Number> values) {
    StatsAccumulator accumulator = new StatsAccumulator();
    accumulator.addAll(values);
    return accumulator.snapshot();
  }

  /**
   * Returns statistics over a dataset containing the given values.
   *
   * @param values a series of values
   */
  public static Stats of(double... values) {
    StatsAccumulator acummulator = new StatsAccumulator();
    acummulator.addAll(values);
    return acummulator.snapshot();
  }

  /**
   * Returns statistics over a dataset containing the given values.
   *
   * @param values a series of values
   */
  public static Stats of(int... values) {
    StatsAccumulator acummulator = new StatsAccumulator();
    acummulator.addAll(values);
    return acummulator.snapshot();
  }

  /**
   * Returns statistics over a dataset containing the given values.
   *
   * @param values a series of values, which will be converted to {@code double} values (this may
   *     cause loss of precision for longs of magnitude over 2^53 (slightly over 9e15))
   */
  public static Stats of(long... values) {
    StatsAccumulator acummulator = new StatsAccumulator();
    acummulator.addAll(values);
    return acummulator.snapshot();
  }

  /** Returns the number of values. */
  public long count() {
    return count;
  }

  /**
   * Returns the <a href="http://en.wikipedia.org/wiki/Arithmetic_mean">arithmetic mean</a> of the
   * values. The count must be non-zero.
   *
   * <p>If these values are a sample drawn from a population, this is also an unbiased estimator of
   * the arithmetic mean of the population.
   *
   * <h3>Non-finite values</h3>
   *
   * <p>If the dataset contains {@link Double#NaN} then the result is {@link Double#NaN}. If it
   * contains both {@link Double#POSITIVE_INFINITY} and {@link Double#NEGATIVE_INFINITY} then the
   * result is {@link Double#NaN}. If it contains {@link Double#POSITIVE_INFINITY} and finite values
   * only or {@link Double#POSITIVE_INFINITY} only, the result is {@link Double#POSITIVE_INFINITY}.
   * If it contains {@link Double#NEGATIVE_INFINITY} and finite values only or {@link
   * Double#NEGATIVE_INFINITY} only, the result is {@link Double#NEGATIVE_INFINITY}.
   *
   * <p>If you only want to calculate the mean, use {@link #meanOf} instead of creating a {@link
   * Stats} instance.
   *
   * @throws IllegalStateException if the dataset is empty
   */
  public double mean() {
    checkState(count != 0);
    return mean;
  }

  /**
   * Returns the sum of the values.
   *
   * <h3>Non-finite values</h3>
   *
   * <p>If the dataset contains {@link Double#NaN} then the result is {@link Double#NaN}. If it
   * contains both {@link Double#POSITIVE_INFINITY} and {@link Double#NEGATIVE_INFINITY} then the
   * result is {@link Double#NaN}. If it contains {@link Double#POSITIVE_INFINITY} and finite values
   * only or {@link Double#POSITIVE_INFINITY} only, the result is {@link Double#POSITIVE_INFINITY}.
   * If it contains {@link Double#NEGATIVE_INFINITY} and finite values only or {@link
   * Double#NEGATIVE_INFINITY} only, the result is {@link Double#NEGATIVE_INFINITY}.
   */
  public double sum() {
    return mean * count;
  }

  /**
   * Returns the <a href="http://en.wikipedia.org/wiki/Variance#Population_variance">population
   * variance</a> of the values. The count must be non-zero.
   *
   * <p>This is guaranteed to return zero if the dataset contains only exactly one finite value. It
   * is not guaranteed to return zero when the dataset consists of the same value multiple times,
   * due to numerical errors. However, it is guaranteed never to return a negative result.
   *
   * <h3>Non-finite values</h3>
   *
   * <p>If the dataset contains any non-finite values ({@link Double#POSITIVE_INFINITY}, {@link
   * Double#NEGATIVE_INFINITY}, or {@link Double#NaN}) then the result is {@link Double#NaN}.
   *
   * @throws IllegalStateException if the dataset is empty
   */
  public double populationVariance() {
    checkState(count > 0);
    if (isNaN(sumOfSquaresOfDeltas)) {
      return NaN;
    }
    if (count == 1) {
      return 0.0;
    }
    return ensureNonNegative(sumOfSquaresOfDeltas) / count();
  }

  /**
   * Returns the <a
   * href="http://en.wikipedia.org/wiki/Standard_deviation#Definition_of_population_values">
   * population standard deviation</a> of the values. The count must be non-zero.
   *
   * <p>This is guaranteed to return zero if the dataset contains only exactly one finite value. It
   * is not guaranteed to return zero when the dataset consists of the same value multiple times,
   * due to numerical errors. However, it is guaranteed never to return a negative result.
   *
   * <h3>Non-finite values</h3>
   *
   * <p>If the dataset contains any non-finite values ({@link Double#POSITIVE_INFINITY}, {@link
   * Double#NEGATIVE_INFINITY}, or {@link Double#NaN}) then the result is {@link Double#NaN}.
   *
   * @throws IllegalStateException if the dataset is empty
   */
  public double populationStandardDeviation() {
    return Math.sqrt(populationVariance());
  }

  /**
   * Returns the <a href="http://en.wikipedia.org/wiki/Variance#Sample_variance">unbiased sample
   * variance</a> of the values. If this dataset is a sample drawn from a population, this is an
   * unbiased estimator of the population variance of the population. The count must be greater than
   * one.
   *
   * <p>This is not guaranteed to return zero when the dataset consists of the same value multiple
   * times, due to numerical errors. However, it is guaranteed never to return a negative result.
   *
   * <h3>Non-finite values</h3>
   *
   * <p>If the dataset contains any non-finite values ({@link Double#POSITIVE_INFINITY}, {@link
   * Double#NEGATIVE_INFINITY}, or {@link Double#NaN}) then the result is {@link Double#NaN}.
   *
   * @throws IllegalStateException if the dataset is empty or contains a single value
   */
  public double sampleVariance() {
    checkState(count > 1);
    if (isNaN(sumOfSquaresOfDeltas)) {
      return NaN;
    }
    return ensureNonNegative(sumOfSquaresOfDeltas) / (count - 1);
  }

  /**
   * Returns the <a
   * href="http://en.wikipedia.org/wiki/Standard_deviation#Corrected_sample_standard_deviation">
   * corrected sample standard deviation</a> of the values. If this dataset is a sample drawn from a
   * population, this is an estimator of the population standard deviation of the population which
   * is less biased than {@link #populationStandardDeviation()} (the unbiased estimator depends on
   * the distribution). The count must be greater than one.
   *
   * <p>This is not guaranteed to return zero when the dataset consists of the same value multiple
   * times, due to numerical errors. However, it is guaranteed never to return a negative result.
   *
   * <h3>Non-finite values</h3>
   *
   * <p>If the dataset contains any non-finite values ({@link Double#POSITIVE_INFINITY}, {@link
   * Double#NEGATIVE_INFINITY}, or {@link Double#NaN}) then the result is {@link Double#NaN}.
   *
   * @throws IllegalStateException if the dataset is empty or contains a single value
   */
  public double sampleStandardDeviation() {
    return Math.sqrt(sampleVariance());
  }

  /**
   * Returns the lowest value in the dataset. The count must be non-zero.
   *
   * <h3>Non-finite values</h3>
   *
   * <p>If the dataset contains {@link Double#NaN} then the result is {@link Double#NaN}. If it
   * contains {@link Double#NEGATIVE_INFINITY} and not {@link Double#NaN} then the result is {@link
   * Double#NEGATIVE_INFINITY}. If it contains {@link Double#POSITIVE_INFINITY} and finite values
   * only then the result is the lowest finite value. If it contains {@link
   * Double#POSITIVE_INFINITY} only then the result is {@link Double#POSITIVE_INFINITY}.
   *
   * @throws IllegalStateException if the dataset is empty
   */
  public double min() {
    checkState(count != 0);
    return min;
  }

  /**
   * Returns the highest value in the dataset. The count must be non-zero.
   *
   * <h3>Non-finite values</h3>
   *
   * <p>If the dataset contains {@link Double#NaN} then the result is {@link Double#NaN}. If it
   * contains {@link Double#POSITIVE_INFINITY} and not {@link Double#NaN} then the result is {@link
   * Double#POSITIVE_INFINITY}. If it contains {@link Double#NEGATIVE_INFINITY} and finite values
   * only then the result is the highest finite value. If it contains {@link
   * Double#NEGATIVE_INFINITY} only then the result is {@link Double#NEGATIVE_INFINITY}.
   *
   * @throws IllegalStateException if the dataset is empty
   */
  public double max() {
    checkState(count != 0);
    return max;
  }

  /**
   * {@inheritDoc}
   *
   * <p><b>Note:</b> This tests exact equality of the calculated statistics, including the floating
   * point values. Two instances are guaranteed to be considered equal if one is copied from the
   * other using {@code second = new StatsAccumulator().addAll(first).snapshot()}, if both were
   * obtained by calling {@code snapshot()} on the same {@link StatsAccumulator} without adding any
   * values in between the two calls, or if one is obtained from the other after round-tripping
   * through java serialization. However, floating point rounding errors mean that it may be false
   * for some instances where the statistics are mathematically equal, including instances
   * constructed from the same values in a different order... or (in the general case) even in the
   * same order. (It is guaranteed to return true for instances constructed from the same values in
   * the same order if {@code strictfp} is in effect, or if the system architecture guarantees
   * {@code strictfp}-like semantics.)
   */
  @Override
  public boolean equals(@CheckForNull Object obj) {
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    Stats other = (Stats) obj;
    return count == other.count
        && doubleToLongBits(mean) == doubleToLongBits(other.mean)
        && doubleToLongBits(sumOfSquaresOfDeltas) == doubleToLongBits(other.sumOfSquaresOfDeltas)
        && doubleToLongBits(min) == doubleToLongBits(other.min)
        && doubleToLongBits(max) == doubleToLongBits(other.max);
  }

  /**
   * {@inheritDoc}
   *
   * <p><b>Note:</b> This hash code is consistent with exact equality of the calculated statistics,
   * including the floating point values. See the note on {@link #equals} for details.
   */
  @Override
  public int hashCode() {
    return Objects.hashCode(count, mean, sumOfSquaresOfDeltas, min, max);
  }

  @Override
  public String toString() {
    if (count() > 0) {
      return MoreObjects.toStringHelper(this)
          .add("count", count)
          .add("mean", mean)
          .add("populationStandardDeviation", populationStandardDeviation())
          .add("min", min)
          .add("max", max)
          .toString();
    } else {
      return MoreObjects.toStringHelper(this).add("count", count).toString();
    }
  }

  double sumOfSquaresOfDeltas() {
    return sumOfSquaresOfDeltas;
  }

  /**
   * Returns the <a href="http://en.wikipedia.org/wiki/Arithmetic_mean">arithmetic mean</a> of the
   * values. The count must be non-zero.
   *
   * <p>The definition of the mean is the same as {@link Stats#mean}.
   *
   * @param values a series of values, which will be converted to {@code double} values (this may
   *     cause loss of precision)
   * @throws IllegalArgumentException if the dataset is empty
   */
  public static double meanOf(Iterable<? extends Number> values) {
    return meanOf(values.iterator());
  }

  /**
   * Returns the <a href="http://en.wikipedia.org/wiki/Arithmetic_mean">arithmetic mean</a> of the
   * values. The count must be non-zero.
   *
   * <p>The definition of the mean is the same as {@link Stats#mean}.
   *
   * @param values a series of values, which will be converted to {@code double} values (this may
   *     cause loss of precision)
   * @throws IllegalArgumentException if the dataset is empty
   */
  public static double meanOf(Iterator<? extends Number> values) {
    checkArgument(values.hasNext());
    long count = 1;
    double mean = values.next().doubleValue();
    while (values.hasNext()) {
      double value = values.next().doubleValue();
      count++;
      if (isFinite(value) && isFinite(mean)) {
        // Art of Computer Programming vol. 2, Knuth, 4.2.2, (15)
        mean += (value - mean) / count;
      } else {
        mean = calculateNewMeanNonFinite(mean, value);
      }
    }
    return mean;
  }

  /**
   * Returns the <a href="http://en.wikipedia.org/wiki/Arithmetic_mean">arithmetic mean</a> of the
   * values. The count must be non-zero.
   *
   * <p>The definition of the mean is the same as {@link Stats#mean}.
   *
   * @param values a series of values
   * @throws IllegalArgumentException if the dataset is empty
   */
  public static double meanOf(double... values) {
    checkArgument(values.length > 0);
    double mean = values[0];
    for (int index = 1; index < values.length; index++) {
      double value = values[index];
      if (isFinite(value) && isFinite(mean)) {
        // Art of Computer Programming vol. 2, Knuth, 4.2.2, (15)
        mean += (value - mean) / (index + 1);
      } else {
        mean = calculateNewMeanNonFinite(mean, value);
      }
    }
    return mean;
  }

  /**
   * Returns the <a href="http://en.wikipedia.org/wiki/Arithmetic_mean">arithmetic mean</a> of the
   * values. The count must be non-zero.
   *
   * <p>The definition of the mean is the same as {@link Stats#mean}.
   *
   * @param values a series of values
   * @throws IllegalArgumentException if the dataset is empty
   */
  public static double meanOf(int... values) {
    checkArgument(values.length > 0);
    double mean = values[0];
    for (int index = 1; index < values.length; index++) {
      double value = values[index];
      if (isFinite(value) && isFinite(mean)) {
        // Art of Computer Programming vol. 2, Knuth, 4.2.2, (15)
        mean += (value - mean) / (index + 1);
      } else {
        mean = calculateNewMeanNonFinite(mean, value);
      }
    }
    return mean;
  }

  /**
   * Returns the <a href="http://en.wikipedia.org/wiki/Arithmetic_mean">arithmetic mean</a> of the
   * values. The count must be non-zero.
   *
   * <p>The definition of the mean is the same as {@link Stats#mean}.
   *
   * @param values a series of values, which will be converted to {@code double} values (this may
   *     cause loss of precision for longs of magnitude over 2^53 (slightly over 9e15))
   * @throws IllegalArgumentException if the dataset is empty
   */
  public static double meanOf(long... values) {
    checkArgument(values.length > 0);
    double mean = values[0];
    for (int index = 1; index < values.length; index++) {
      double value = values[index];
      if (isFinite(value) && isFinite(mean)) {
        // Art of Computer Programming vol. 2, Knuth, 4.2.2, (15)
        mean += (value - mean) / (index + 1);
      } else {
        mean = calculateNewMeanNonFinite(mean, value);
      }
    }
    return mean;
  }

  // Serialization helpers

  /** The size of byte array representation in bytes. */
  static final int BYTES = (Long.SIZE + Double.SIZE * 4) / Byte.SIZE;

  /**
   * Gets a byte array representation of this instance.
   *
   * <p><b>Note:</b> No guarantees are made regarding stability of the representation between
   * versions.
   */
  public byte[] toByteArray() {
    ByteBuffer buff = ByteBuffer.allocate(BYTES).order(ByteOrder.LITTLE_ENDIAN);
    writeTo(buff);
    return buff.array();
  }

  /**
   * Writes to the given {@link ByteBuffer} a byte representation of this instance.
   *
   * <p><b>Note:</b> No guarantees are made regarding stability of the representation between
   * versions.
   *
   * @param buffer A {@link ByteBuffer} with at least BYTES {@link ByteBuffer#remaining}, ordered as
   *     {@link ByteOrder#LITTLE_ENDIAN}, to which a BYTES-long byte representation of this instance
   *     is written. In the process increases the position of {@link ByteBuffer} by BYTES.
   */
  void writeTo(ByteBuffer buffer) {
    checkNotNull(buffer);
    checkArgument(
        buffer.remaining() >= BYTES,
        "Expected at least Stats.BYTES = %s remaining , got %s",
        BYTES,
        buffer.remaining());
    buffer
        .putLong(count)
        .putDouble(mean)
        .putDouble(sumOfSquaresOfDeltas)
        .putDouble(min)
        .putDouble(max);
  }

  /**
   * Creates a Stats instance from the given byte representation which was obtained by {@link
   * #toByteArray}.
   *
   * <p><b>Note:</b> No guarantees are made regarding stability of the representation between
   * versions.
   */
  public static Stats fromByteArray(byte[] byteArray) {
    checkNotNull(byteArray);
    checkArgument(
        byteArray.length == BYTES,
        "Expected Stats.BYTES = %s remaining , got %s",
        BYTES,
        byteArray.length);
    return readFrom(ByteBuffer.wrap(byteArray).order(ByteOrder.LITTLE_ENDIAN));
  }

  /**
   * Creates a Stats instance from the byte representation read from the given {@link ByteBuffer}.
   *
   * <p><b>Note:</b> No guarantees are made regarding stability of the representation between
   * versions.
   *
   * @param buffer A {@link ByteBuffer} with at least BYTES {@link ByteBuffer#remaining}, ordered as
   *     {@link ByteOrder#LITTLE_ENDIAN}, from which a BYTES-long byte representation of this
   *     instance is read. In the process increases the position of {@link ByteBuffer} by BYTES.
   */
  static Stats readFrom(ByteBuffer buffer) {
    checkNotNull(buffer);
    checkArgument(
        buffer.remaining() >= BYTES,
        "Expected at least Stats.BYTES = %s remaining , got %s",
        BYTES,
        buffer.remaining());
    return new Stats(
        buffer.getLong(),
        buffer.getDouble(),
        buffer.getDouble(),
        buffer.getDouble(),
        buffer.getDouble());
  }

  private static final long serialVersionUID = 0;
}
