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
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * An immutable value object capturing some basic statistics about a collection of paired double
 * values (e.g. points on a plane). Build instances with {@link PairedStatsAccumulator#snapshot}.
 *
 * @author Pete Gillin
 * @since 20.0
 */
@Beta
@GwtIncompatible
public final class PairedStats implements Serializable {

  private final Stats xStats;
  private final Stats yStats;
  private final double sumOfProductsOfDeltas;

  /**
   * Internal constructor. Users should use {@link PairedStatsAccumulator#snapshot}.
   *
   * <p>To ensure that the created instance obeys its contract, the parameters should satisfy the
   * following constraints. This is the callers responsibility and is not enforced here.
   *
   * <ul>
   *   <li>Both {@code xStats} and {@code yStats} must have the same {@code count}.
   *   <li>If that {@code count} is 1, {@code sumOfProductsOfDeltas} must be exactly 0.0.
   *   <li>If that {@code count} is more than 1, {@code sumOfProductsOfDeltas} must be finite.
   * </ul>
   */
  PairedStats(Stats xStats, Stats yStats, double sumOfProductsOfDeltas) {
    this.xStats = xStats;
    this.yStats = yStats;
    this.sumOfProductsOfDeltas = sumOfProductsOfDeltas;
  }

  /** Returns the number of pairs in the dataset. */
  public long count() {
    return xStats.count();
  }

  /** Returns the statistics on the {@code x} values alone. */
  public Stats xStats() {
    return xStats;
  }

  /** Returns the statistics on the {@code y} values alone. */
  public Stats yStats() {
    return yStats;
  }

  /**
   * Returns the population covariance of the values. The count must be non-zero.
   *
   * <p>This is guaranteed to return zero if the dataset contains a single pair of finite values. It
   * is not guaranteed to return zero when the dataset consists of the same pair of values multiple
   * times, due to numerical errors.
   *
   * <h3>Non-finite values</h3>
   *
   * <p>If the dataset contains any non-finite values ({@link Double#POSITIVE_INFINITY}, {@link
   * Double#NEGATIVE_INFINITY}, or {@link Double#NaN}) then the result is {@link Double#NaN}.
   *
   * @throws IllegalStateException if the dataset is empty
   */
  public double populationCovariance() {
    checkState(count() != 0);
    return sumOfProductsOfDeltas / count();
  }

  /**
   * Returns the sample covariance of the values. The count must be greater than one.
   *
   * <p>This is not guaranteed to return zero when the dataset consists of the same pair of values
   * multiple times, due to numerical errors.
   *
   * <h3>Non-finite values</h3>
   *
   * <p>If the dataset contains any non-finite values ({@link Double#POSITIVE_INFINITY}, {@link
   * Double#NEGATIVE_INFINITY}, or {@link Double#NaN}) then the result is {@link Double#NaN}.
   *
   * @throws IllegalStateException if the dataset is empty or contains a single pair of values
   */
  public double sampleCovariance() {
    checkState(count() > 1);
    return sumOfProductsOfDeltas / (count() - 1);
  }

  /**
   * Returns the <a href="http://mathworld.wolfram.com/CorrelationCoefficient.html">Pearson's or
   * product-moment correlation coefficient</a> of the values. The count must greater than one, and
   * the {@code x} and {@code y} values must both have non-zero population variance (i.e. {@code
   * xStats().populationVariance() > 0.0 && yStats().populationVariance() > 0.0}). The result is not
   * guaranteed to be exactly +/-1 even when the data are perfectly (anti-)correlated, due to
   * numerical errors. However, it is guaranteed to be in the inclusive range [-1, +1].
   *
   * <h3>Non-finite values</h3>
   *
   * <p>If the dataset contains any non-finite values ({@link Double#POSITIVE_INFINITY}, {@link
   * Double#NEGATIVE_INFINITY}, or {@link Double#NaN}) then the result is {@link Double#NaN}.
   *
   * @throws IllegalStateException if the dataset is empty or contains a single pair of values, or
   *     either the {@code x} and {@code y} dataset has zero population variance
   */
  public double pearsonsCorrelationCoefficient() {
    checkState(count() > 1);
    if (isNaN(sumOfProductsOfDeltas)) {
      return NaN;
    }
    double xSumOfSquaresOfDeltas = xStats().sumOfSquaresOfDeltas();
    double ySumOfSquaresOfDeltas = yStats().sumOfSquaresOfDeltas();
    checkState(xSumOfSquaresOfDeltas > 0.0);
    checkState(ySumOfSquaresOfDeltas > 0.0);
    // The product of two positive numbers can be zero if the multiplication underflowed. We
    // force a positive value by effectively rounding up to MIN_VALUE.
    double productOfSumsOfSquaresOfDeltas =
        ensurePositive(xSumOfSquaresOfDeltas * ySumOfSquaresOfDeltas);
    return ensureInUnitRange(sumOfProductsOfDeltas / Math.sqrt(productOfSumsOfSquaresOfDeltas));
  }

  /**
   * Returns a linear transformation giving the best fit to the data according to <a
   * href="http://mathworld.wolfram.com/LeastSquaresFitting.html">Ordinary Least Squares linear
   * regression</a> of {@code y} as a function of {@code x}. The count must be greater than one, and
   * either the {@code x} or {@code y} data must have a non-zero population variance (i.e. {@code
   * xStats().populationVariance() > 0.0 || yStats().populationVariance() > 0.0}). The result is
   * guaranteed to be horizontal if there is variance in the {@code x} data but not the {@code y}
   * data, and vertical if there is variance in the {@code y} data but not the {@code x} data.
   *
   * <p>This fit minimizes the root-mean-square error in {@code y} as a function of {@code x}. This
   * error is defined as the square root of the mean of the squares of the differences between the
   * actual {@code y} values of the data and the values predicted by the fit for the {@code x}
   * values (i.e. it is the square root of the mean of the squares of the vertical distances between
   * the data points and the best fit line). For this fit, this error is a fraction {@code sqrt(1 -
   * R*R)} of the population standard deviation of {@code y}, where {@code R} is the Pearson's
   * correlation coefficient (as given by {@link #pearsonsCorrelationCoefficient()}).
   *
   * <p>The corresponding root-mean-square error in {@code x} as a function of {@code y} is a
   * fraction {@code sqrt(1/(R*R) - 1)} of the population standard deviation of {@code x}. This fit
   * does not normally minimize that error: to do that, you should swap the roles of {@code x} and
   * {@code y}.
   *
   * <h3>Non-finite values</h3>
   *
   * <p>If the dataset contains any non-finite values ({@link Double#POSITIVE_INFINITY}, {@link
   * Double#NEGATIVE_INFINITY}, or {@link Double#NaN}) then the result is {@link
   * LinearTransformation#forNaN()}.
   *
   * @throws IllegalStateException if the dataset is empty or contains a single pair of values, or
   *     both the {@code x} and {@code y} dataset must have zero population variance
   */
  public LinearTransformation leastSquaresFit() {
    checkState(count() > 1);
    if (isNaN(sumOfProductsOfDeltas)) {
      return LinearTransformation.forNaN();
    }
    double xSumOfSquaresOfDeltas = xStats.sumOfSquaresOfDeltas();
    if (xSumOfSquaresOfDeltas > 0.0) {
      if (yStats.sumOfSquaresOfDeltas() > 0.0) {
        return LinearTransformation.mapping(xStats.mean(), yStats.mean())
            .withSlope(sumOfProductsOfDeltas / xSumOfSquaresOfDeltas);
      } else {
        return LinearTransformation.horizontal(yStats.mean());
      }
    } else {
      checkState(yStats.sumOfSquaresOfDeltas() > 0.0);
      return LinearTransformation.vertical(xStats.mean());
    }
  }

  /**
   * {@inheritDoc}
   *
   * <p><b>Note:</b> This tests exact equality of the calculated statistics, including the floating
   * point values. Two instances are guaranteed to be considered equal if one is copied from the
   * other using {@code second = new PairedStatsAccumulator().addAll(first).snapshot()}, if both
   * were obtained by calling {@code snapshot()} on the same {@link PairedStatsAccumulator} without
   * adding any values in between the two calls, or if one is obtained from the other after
   * round-tripping through java serialization. However, floating point rounding errors mean that it
   * may be false for some instances where the statistics are mathematically equal, including
   * instances constructed from the same values in a different order... or (in the general case)
   * even in the same order. (It is guaranteed to return true for instances constructed from the
   * same values in the same order if {@code strictfp} is in effect, or if the system architecture
   * guarantees {@code strictfp}-like semantics.)
   */
  @Override
  public boolean equals(@Nullable Object obj) {
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    PairedStats other = (PairedStats) obj;
    return xStats.equals(other.xStats)
        && yStats.equals(other.yStats)
        && doubleToLongBits(sumOfProductsOfDeltas) == doubleToLongBits(other.sumOfProductsOfDeltas);
  }

  /**
   * {@inheritDoc}
   *
   * <p><b>Note:</b> This hash code is consistent with exact equality of the calculated statistics,
   * including the floating point values. See the note on {@link #equals} for details.
   */
  @Override
  public int hashCode() {
    return Objects.hashCode(xStats, yStats, sumOfProductsOfDeltas);
  }

  @Override
  public String toString() {
    if (count() > 0) {
      return MoreObjects.toStringHelper(this)
          .add("xStats", xStats)
          .add("yStats", yStats)
          .add("populationCovariance", populationCovariance())
          .toString();
    } else {
      return MoreObjects.toStringHelper(this)
          .add("xStats", xStats)
          .add("yStats", yStats)
          .toString();
    }
  }

  double sumOfProductsOfDeltas() {
    return sumOfProductsOfDeltas;
  }

  private static double ensurePositive(double value) {
    if (value > 0.0) {
      return value;
    } else {
      return Double.MIN_VALUE;
    }
  }

  private static double ensureInUnitRange(double value) {
    if (value >= 1.0) {
      return 1.0;
    }
    if (value <= -1.0) {
      return -1.0;
    }
    return value;
  }

  // Serialization helpers

  /** The size of byte array representation in bytes. */
  private static final int BYTES = Stats.BYTES * 2 + Double.SIZE / Byte.SIZE;

  /**
   * Gets a byte array representation of this instance.
   *
   * <p><b>Note:</b> No guarantees are made regarding stability of the representation between
   * versions.
   */
  public byte[] toByteArray() {
    ByteBuffer buffer = ByteBuffer.allocate(BYTES).order(ByteOrder.LITTLE_ENDIAN);
    xStats.writeTo(buffer);
    yStats.writeTo(buffer);
    buffer.putDouble(sumOfProductsOfDeltas);
    return buffer.array();
  }

  /**
   * Creates a {@link PairedStats} instance from the given byte representation which was obtained by
   * {@link #toByteArray}.
   *
   * <p><b>Note:</b> No guarantees are made regarding stability of the representation between
   * versions.
   */
  public static PairedStats fromByteArray(byte[] byteArray) {
    checkNotNull(byteArray);
    checkArgument(
        byteArray.length == BYTES,
        "Expected PairedStats.BYTES = %s, got %s",
        BYTES,
        byteArray.length);
    ByteBuffer buffer = ByteBuffer.wrap(byteArray).order(ByteOrder.LITTLE_ENDIAN);
    Stats xStats = Stats.readFrom(buffer);
    Stats yStats = Stats.readFrom(buffer);
    double sumOfProductsOfDeltas = buffer.getDouble();
    return new PairedStats(xStats, yStats, sumOfProductsOfDeltas);
  }

  private static final long serialVersionUID = 0;
}
