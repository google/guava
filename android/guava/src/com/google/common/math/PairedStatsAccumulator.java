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

import static com.google.common.base.Preconditions.checkState;
import static com.google.common.primitives.Doubles.isFinite;
import static java.lang.Double.NaN;
import static java.lang.Double.isNaN;

import com.google.common.annotations.Beta;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.primitives.Doubles;

/**
 * A mutable object which accumulates paired double values (e.g. points on a plane) and tracks some
 * basic statistics over all the values added so far. This class is not thread safe.
 *
 * @author Pete Gillin
 * @since 20.0
 */
@Beta
@GwtIncompatible
@ElementTypesAreNonnullByDefault
public final class PairedStatsAccumulator {

  // These fields must satisfy the requirements of PairedStats' constructor as well as those of the
  // stat methods of this class.
  private final StatsAccumulator xStats = new StatsAccumulator();
  private final StatsAccumulator yStats = new StatsAccumulator();
  private double sumOfProductsOfDeltas = 0.0;

  /** Adds the given pair of values to the dataset. */
  public void add(double x, double y) {
    // We extend the recursive expression for the one-variable case at Art of Computer Programming
    // vol. 2, Knuth, 4.2.2, (16) to the two-variable case. We have two value series x_i and y_i.
    // We define the arithmetic means X_n = 1/n \sum_{i=1}^n x_i, and Y_n = 1/n \sum_{i=1}^n y_i.
    // We also define the sum of the products of the differences from the means
    //           C_n = \sum_{i=1}^n x_i y_i - n X_n Y_n
    // for all n >= 1. Then for all n > 1:
    //       C_{n-1} = \sum_{i=1}^{n-1} x_i y_i - (n-1) X_{n-1} Y_{n-1}
    // C_n - C_{n-1} = x_n y_n - n X_n Y_n + (n-1) X_{n-1} Y_{n-1}
    //               = x_n y_n - X_n [ y_n + (n-1) Y_{n-1} ] + [ n X_n - x_n ] Y_{n-1}
    //               = x_n y_n - X_n y_n - x_n Y_{n-1} + X_n Y_{n-1}
    //               = (x_n - X_n) (y_n - Y_{n-1})
    xStats.add(x);
    if (isFinite(x) && isFinite(y)) {
      if (xStats.count() > 1) {
        sumOfProductsOfDeltas += (x - xStats.mean()) * (y - yStats.mean());
      }
    } else {
      sumOfProductsOfDeltas = NaN;
    }
    yStats.add(y);
  }

  /**
   * Adds the given statistics to the dataset, as if the individual values used to compute the
   * statistics had been added directly.
   */
  public void addAll(PairedStats values) {
    if (values.count() == 0) {
      return;
    }

    xStats.addAll(values.xStats());
    if (yStats.count() == 0) {
      sumOfProductsOfDeltas = values.sumOfProductsOfDeltas();
    } else {
      // This is a generalized version of the calculation in add(double, double) above. Note that
      // non-finite inputs will have sumOfProductsOfDeltas = NaN, so non-finite values will result
      // in NaN naturally.
      sumOfProductsOfDeltas +=
          values.sumOfProductsOfDeltas()
              + (values.xStats().mean() - xStats.mean())
                  * (values.yStats().mean() - yStats.mean())
                  * values.count();
    }
    yStats.addAll(values.yStats());
  }

  /** Returns an immutable snapshot of the current statistics. */
  public PairedStats snapshot() {
    return new PairedStats(xStats.snapshot(), yStats.snapshot(), sumOfProductsOfDeltas);
  }

  /** Returns the number of pairs in the dataset. */
  public long count() {
    return xStats.count();
  }

  /** Returns an immutable snapshot of the statistics on the {@code x} values alone. */
  public Stats xStats() {
    return xStats.snapshot();
  }

  /** Returns an immutable snapshot of the statistics on the {@code y} values alone. */
  public Stats yStats() {
    return yStats.snapshot();
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
  public final double sampleCovariance() {
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
  public final double pearsonsCorrelationCoefficient() {
    checkState(count() > 1);
    if (isNaN(sumOfProductsOfDeltas)) {
      return NaN;
    }
    double xSumOfSquaresOfDeltas = xStats.sumOfSquaresOfDeltas();
    double ySumOfSquaresOfDeltas = yStats.sumOfSquaresOfDeltas();
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
   *     both the {@code x} and {@code y} dataset have zero population variance
   */
  public final LinearTransformation leastSquaresFit() {
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

  private double ensurePositive(double value) {
    if (value > 0.0) {
      return value;
    } else {
      return Double.MIN_VALUE;
    }
  }

  private static double ensureInUnitRange(double value) {
    return Doubles.constrainToRange(value, -1.0, 1.0);
  }
}
