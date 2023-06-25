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
import static com.google.common.math.DoubleUtils.isFinite;
import static java.lang.Double.NaN;

import com.google.common.annotations.GwtIncompatible;
import com.google.common.annotations.J2ktIncompatible;
import com.google.errorprone.annotations.concurrent.LazyInit;
import javax.annotation.CheckForNull;

/**
 * The representation of a linear transformation between real numbers {@code x} and {@code y}.
 * Graphically, this is the specification of a straight line on a plane. The transformation can be
 * expressed as {@code y = m * x + c} for finite {@code m} and {@code c}, unless it is a vertical
 * transformation in which case {@code x} has a constant value for all {@code y}. In the
 * non-vertical case, {@code m} is the slope of the transformation (and a horizontal transformation
 * has zero slope).
 *
 * @author Pete Gillin
 * @since 20.0
 */
@J2ktIncompatible
@GwtIncompatible
@ElementTypesAreNonnullByDefault
public abstract class LinearTransformation {

  /**
   * Start building an instance which maps {@code x = x1} to {@code y = y1}. Both arguments must be
   * finite. Call either {@link LinearTransformationBuilder#and} or {@link
   * LinearTransformationBuilder#withSlope} on the returned object to finish building the instance.
   */
  public static LinearTransformationBuilder mapping(double x1, double y1) {
    checkArgument(isFinite(x1) && isFinite(y1));
    return new LinearTransformationBuilder(x1, y1);
  }

  /**
   * This is an intermediate stage in the construction process. It is returned by {@link
   * LinearTransformation#mapping}. You almost certainly don't want to keep instances around, but
   * instead use method chaining. This represents a single point mapping, i.e. a mapping between one
   * {@code x} and {@code y} value pair.
   *
   * @since 20.0
   */
  public static final class LinearTransformationBuilder {

    private final double x1;
    private final double y1;

    private LinearTransformationBuilder(double x1, double y1) {
      this.x1 = x1;
      this.y1 = y1;
    }

    /**
     * Finish building an instance which also maps {@code x = x2} to {@code y = y2}. These values
     * must not both be identical to the values given in the first mapping. If only the {@code x}
     * values are identical, the transformation is vertical. If only the {@code y} values are
     * identical, the transformation is horizontal (i.e. the slope is zero).
     */
    public LinearTransformation and(double x2, double y2) {
      checkArgument(isFinite(x2) && isFinite(y2));
      if (x2 == x1) {
        checkArgument(y2 != y1);
        return new VerticalLinearTransformation(x1);
      } else {
        return withSlope((y2 - y1) / (x2 - x1));
      }
    }

    /**
     * Finish building an instance with the given slope, i.e. the rate of change of {@code y} with
     * respect to {@code x}. The slope must not be {@code NaN}. It may be infinite, in which case
     * the transformation is vertical. (If it is zero, the transformation is horizontal.)
     */
    public LinearTransformation withSlope(double slope) {
      checkArgument(!Double.isNaN(slope));
      if (isFinite(slope)) {
        double yIntercept = y1 - x1 * slope;
        return new RegularLinearTransformation(slope, yIntercept);
      } else {
        return new VerticalLinearTransformation(x1);
      }
    }
  }

  /**
   * Builds an instance representing a vertical transformation with a constant value of {@code x}.
   * (The inverse of this will be a horizontal transformation.)
   */
  public static LinearTransformation vertical(double x) {
    checkArgument(isFinite(x));
    return new VerticalLinearTransformation(x);
  }

  /**
   * Builds an instance representing a horizontal transformation with a constant value of {@code y}.
   * (The inverse of this will be a vertical transformation.)
   */
  public static LinearTransformation horizontal(double y) {
    checkArgument(isFinite(y));
    double slope = 0.0;
    return new RegularLinearTransformation(slope, y);
  }

  /**
   * Builds an instance for datasets which contains {@link Double#NaN}. The {@link #isHorizontal}
   * and {@link #isVertical} methods return {@code false} and the {@link #slope}, and {@link
   * #transform} methods all return {@link Double#NaN}. The {@link #inverse} method returns the same
   * instance.
   */
  public static LinearTransformation forNaN() {
    return NaNLinearTransformation.INSTANCE;
  }

  /** Returns whether this is a vertical transformation. */
  public abstract boolean isVertical();

  /** Returns whether this is a horizontal transformation. */
  public abstract boolean isHorizontal();

  /**
   * Returns the slope of the transformation, i.e. the rate of change of {@code y} with respect to
   * {@code x}. This must not be called on a vertical transformation (i.e. when {@link
   * #isVertical()} is true).
   */
  public abstract double slope();

  /**
   * Returns the {@code y} corresponding to the given {@code x}. This must not be called on a
   * vertical transformation (i.e. when {@link #isVertical()} is true).
   */
  public abstract double transform(double x);

  /**
   * Returns the inverse linear transformation. The inverse of a horizontal transformation is a
   * vertical transformation, and vice versa. The inverse of the {@link #forNaN} transformation is
   * itself. In all other cases, the inverse is a transformation such that applying both the
   * original transformation and its inverse to a value gives you the original value give-or-take
   * numerical errors. Calling this method multiple times on the same instance will always return
   * the same instance. Calling this method on the result of calling this method on an instance will
   * always return that original instance.
   */
  public abstract LinearTransformation inverse();

  private static final class RegularLinearTransformation extends LinearTransformation {

    final double slope;
    final double yIntercept;

    @CheckForNull @LazyInit LinearTransformation inverse;

    RegularLinearTransformation(double slope, double yIntercept) {
      this.slope = slope;
      this.yIntercept = yIntercept;
      this.inverse = null; // to be lazily initialized
    }

    RegularLinearTransformation(double slope, double yIntercept, LinearTransformation inverse) {
      this.slope = slope;
      this.yIntercept = yIntercept;
      this.inverse = inverse;
    }

    @Override
    public boolean isVertical() {
      return false;
    }

    @Override
    public boolean isHorizontal() {
      return (slope == 0.0);
    }

    @Override
    public double slope() {
      return slope;
    }

    @Override
    public double transform(double x) {
      return x * slope + yIntercept;
    }

    @Override
    public LinearTransformation inverse() {
      LinearTransformation result = inverse;
      return (result == null) ? inverse = createInverse() : result;
    }

    @Override
    public String toString() {
      return String.format("y = %g * x + %g", slope, yIntercept);
    }

    private LinearTransformation createInverse() {
      if (slope != 0.0) {
        return new RegularLinearTransformation(1.0 / slope, -1.0 * yIntercept / slope, this);
      } else {
        return new VerticalLinearTransformation(yIntercept, this);
      }
    }
  }

  private static final class VerticalLinearTransformation extends LinearTransformation {

    final double x;

    @CheckForNull @LazyInit LinearTransformation inverse;

    VerticalLinearTransformation(double x) {
      this.x = x;
      this.inverse = null; // to be lazily initialized
    }

    VerticalLinearTransformation(double x, LinearTransformation inverse) {
      this.x = x;
      this.inverse = inverse;
    }

    @Override
    public boolean isVertical() {
      return true;
    }

    @Override
    public boolean isHorizontal() {
      return false;
    }

    @Override
    public double slope() {
      throw new IllegalStateException();
    }

    @Override
    public double transform(double x) {
      throw new IllegalStateException();
    }

    @Override
    public LinearTransformation inverse() {
      LinearTransformation result = inverse;
      return (result == null) ? inverse = createInverse() : result;
    }

    @Override
    public String toString() {
      return String.format("x = %g", x);
    }

    private LinearTransformation createInverse() {
      return new RegularLinearTransformation(0.0, x, this);
    }
  }

  private static final class NaNLinearTransformation extends LinearTransformation {

    static final NaNLinearTransformation INSTANCE = new NaNLinearTransformation();

    @Override
    public boolean isVertical() {
      return false;
    }

    @Override
    public boolean isHorizontal() {
      return false;
    }

    @Override
    public double slope() {
      return NaN;
    }

    @Override
    public double transform(double x) {
      return NaN;
    }

    @Override
    public LinearTransformation inverse() {
      return this;
    }

    @Override
    public String toString() {
      return "NaN";
    }
  }
}
