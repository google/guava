/*
 * Copyright (C) 2011 The Guava Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.common.collect;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.BoundType.CLOSED;
import static com.google.common.collect.BoundType.OPEN;
import static com.google.common.collect.NullnessCasts.uncheckedCastNullableTToT;

import com.google.common.annotations.GwtCompatible;
import com.google.common.base.Objects;
import com.google.errorprone.annotations.concurrent.LazyInit;
import java.io.Serializable;
import java.util.Comparator;
import javax.annotation.CheckForNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * A generalized interval on any ordering, for internal use. Supports {@code null}. Unlike {@link
 * Range}, this allows the use of an arbitrary comparator. This is designed for use in the
 * implementation of subcollections of sorted collection types.
 *
 * <p>Whenever possible, use {@code Range} instead, which is better supported.
 *
 * @author Louis Wasserman
 */
@GwtCompatible(serializable = true)
@ElementTypesAreNonnullByDefault
final class GeneralRange<T extends @Nullable Object> implements Serializable {
  /** Converts a Range to a GeneralRange. */
  static <T extends Comparable> GeneralRange<T> from(Range<T> range) {
    T lowerEndpoint = range.hasLowerBound() ? range.lowerEndpoint() : null;
    BoundType lowerBoundType = range.hasLowerBound() ? range.lowerBoundType() : OPEN;

    T upperEndpoint = range.hasUpperBound() ? range.upperEndpoint() : null;
    BoundType upperBoundType = range.hasUpperBound() ? range.upperBoundType() : OPEN;
    return new GeneralRange<>(
        Ordering.natural(),
        range.hasLowerBound(),
        lowerEndpoint,
        lowerBoundType,
        range.hasUpperBound(),
        upperEndpoint,
        upperBoundType);
  }

  /** Returns the whole range relative to the specified comparator. */
  static <T extends @Nullable Object> GeneralRange<T> all(Comparator<? super T> comparator) {
    return new GeneralRange<>(comparator, false, null, OPEN, false, null, OPEN);
  }

  /**
   * Returns everything above the endpoint relative to the specified comparator, with the specified
   * endpoint behavior.
   */
  static <T extends @Nullable Object> GeneralRange<T> downTo(
      Comparator<? super T> comparator, @ParametricNullness T endpoint, BoundType boundType) {
    return new GeneralRange<>(comparator, true, endpoint, boundType, false, null, OPEN);
  }

  /**
   * Returns everything below the endpoint relative to the specified comparator, with the specified
   * endpoint behavior.
   */
  static <T extends @Nullable Object> GeneralRange<T> upTo(
      Comparator<? super T> comparator, @ParametricNullness T endpoint, BoundType boundType) {
    return new GeneralRange<>(comparator, false, null, OPEN, true, endpoint, boundType);
  }

  /**
   * Returns everything between the endpoints relative to the specified comparator, with the
   * specified endpoint behavior.
   */
  static <T extends @Nullable Object> GeneralRange<T> range(
      Comparator<? super T> comparator,
      @ParametricNullness T lower,
      BoundType lowerType,
      @ParametricNullness T upper,
      BoundType upperType) {
    return new GeneralRange<>(comparator, true, lower, lowerType, true, upper, upperType);
  }

  private final Comparator<? super T> comparator;
  private final boolean hasLowerBound;
  @CheckForNull private final T lowerEndpoint;
  private final BoundType lowerBoundType;
  private final boolean hasUpperBound;
  @CheckForNull private final T upperEndpoint;
  private final BoundType upperBoundType;

  private GeneralRange(
      Comparator<? super T> comparator,
      boolean hasLowerBound,
      @CheckForNull T lowerEndpoint,
      BoundType lowerBoundType,
      boolean hasUpperBound,
      @CheckForNull T upperEndpoint,
      BoundType upperBoundType) {
    this.comparator = checkNotNull(comparator);
    this.hasLowerBound = hasLowerBound;
    this.hasUpperBound = hasUpperBound;
    this.lowerEndpoint = lowerEndpoint;
    this.lowerBoundType = checkNotNull(lowerBoundType);
    this.upperEndpoint = upperEndpoint;
    this.upperBoundType = checkNotNull(upperBoundType);

    // Trigger any exception that the comparator would throw for the endpoints.
    /*
     * uncheckedCastNullableTToT is safe as long as the callers are careful to pass a "real" T
     * whenever they pass `true` for the matching `has*Bound` parameter.
     */
    if (hasLowerBound) {
      int unused =
          comparator.compare(
              uncheckedCastNullableTToT(lowerEndpoint), uncheckedCastNullableTToT(lowerEndpoint));
    }
    if (hasUpperBound) {
      int unused =
          comparator.compare(
              uncheckedCastNullableTToT(upperEndpoint), uncheckedCastNullableTToT(upperEndpoint));
    }

    if (hasLowerBound && hasUpperBound) {
      int cmp =
          comparator.compare(
              uncheckedCastNullableTToT(lowerEndpoint), uncheckedCastNullableTToT(upperEndpoint));
      // be consistent with Range
      checkArgument(
          cmp <= 0, "lowerEndpoint (%s) > upperEndpoint (%s)", lowerEndpoint, upperEndpoint);
      if (cmp == 0) {
        checkArgument(lowerBoundType != OPEN || upperBoundType != OPEN);
      }
    }
  }

  Comparator<? super T> comparator() {
    return comparator;
  }

  boolean hasLowerBound() {
    return hasLowerBound;
  }

  boolean hasUpperBound() {
    return hasUpperBound;
  }

  boolean isEmpty() {
    // The casts are safe because of the has*Bound() checks.
    return (hasUpperBound() && tooLow(uncheckedCastNullableTToT(getUpperEndpoint())))
        || (hasLowerBound() && tooHigh(uncheckedCastNullableTToT(getLowerEndpoint())));
  }

  boolean tooLow(@ParametricNullness T t) {
    if (!hasLowerBound()) {
      return false;
    }
    // The cast is safe because of the hasLowerBound() check.
    T lbound = uncheckedCastNullableTToT(getLowerEndpoint());
    int cmp = comparator.compare(t, lbound);
    return cmp < 0 | (cmp == 0 & getLowerBoundType() == OPEN);
  }

  boolean tooHigh(@ParametricNullness T t) {
    if (!hasUpperBound()) {
      return false;
    }
    // The cast is safe because of the hasUpperBound() check.
    T ubound = uncheckedCastNullableTToT(getUpperEndpoint());
    int cmp = comparator.compare(t, ubound);
    return cmp > 0 | (cmp == 0 & getUpperBoundType() == OPEN);
  }

  boolean contains(@ParametricNullness T t) {
    return !tooLow(t) && !tooHigh(t);
  }

  /**
   * Returns the intersection of the two ranges, or an empty range if their intersection is empty.
   */
  @SuppressWarnings("nullness") // TODO(cpovirk): Add casts as needed. Will be noisy and annoying...
  GeneralRange<T> intersect(GeneralRange<T> other) {
    checkNotNull(other);
    checkArgument(comparator.equals(other.comparator));

    boolean hasLowBound = this.hasLowerBound;
    T lowEnd = getLowerEndpoint();
    BoundType lowType = getLowerBoundType();
    if (!hasLowerBound()) {
      hasLowBound = other.hasLowerBound;
      lowEnd = other.getLowerEndpoint();
      lowType = other.getLowerBoundType();
    } else if (other.hasLowerBound()) {
      int cmp = comparator.compare(getLowerEndpoint(), other.getLowerEndpoint());
      if (cmp < 0 || (cmp == 0 && other.getLowerBoundType() == OPEN)) {
        lowEnd = other.getLowerEndpoint();
        lowType = other.getLowerBoundType();
      }
    }

    boolean hasUpBound = this.hasUpperBound;
    T upEnd = getUpperEndpoint();
    BoundType upType = getUpperBoundType();
    if (!hasUpperBound()) {
      hasUpBound = other.hasUpperBound;
      upEnd = other.getUpperEndpoint();
      upType = other.getUpperBoundType();
    } else if (other.hasUpperBound()) {
      int cmp = comparator.compare(getUpperEndpoint(), other.getUpperEndpoint());
      if (cmp > 0 || (cmp == 0 && other.getUpperBoundType() == OPEN)) {
        upEnd = other.getUpperEndpoint();
        upType = other.getUpperBoundType();
      }
    }

    if (hasLowBound && hasUpBound) {
      int cmp = comparator.compare(lowEnd, upEnd);
      if (cmp > 0 || (cmp == 0 && lowType == OPEN && upType == OPEN)) {
        // force allowed empty range
        lowEnd = upEnd;
        lowType = OPEN;
        upType = CLOSED;
      }
    }

    return new GeneralRange<>(comparator, hasLowBound, lowEnd, lowType, hasUpBound, upEnd, upType);
  }

  @Override
  public boolean equals(@CheckForNull Object obj) {
    if (obj instanceof GeneralRange) {
      GeneralRange<?> r = (GeneralRange<?>) obj;
      return comparator.equals(r.comparator)
          && hasLowerBound == r.hasLowerBound
          && hasUpperBound == r.hasUpperBound
          && getLowerBoundType().equals(r.getLowerBoundType())
          && getUpperBoundType().equals(r.getUpperBoundType())
          && Objects.equal(getLowerEndpoint(), r.getLowerEndpoint())
          && Objects.equal(getUpperEndpoint(), r.getUpperEndpoint());
    }
    return false;
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(
        comparator,
        getLowerEndpoint(),
        getLowerBoundType(),
        getUpperEndpoint(),
        getUpperBoundType());
  }

  @LazyInit @CheckForNull private transient GeneralRange<T> reverse;

  /** Returns the same range relative to the reversed comparator. */
  GeneralRange<T> reverse() {
    GeneralRange<T> result = reverse;
    if (result == null) {
      result =
          new GeneralRange<>(
              Ordering.from(comparator).reverse(),
              hasUpperBound,
              getUpperEndpoint(),
              getUpperBoundType(),
              hasLowerBound,
              getLowerEndpoint(),
              getLowerBoundType());
      result.reverse = this;
      return this.reverse = result;
    }
    return result;
  }

  @Override
  public String toString() {
    return comparator
        + ":"
        + (lowerBoundType == CLOSED ? '[' : '(')
        + (hasLowerBound ? lowerEndpoint : "-\u221e")
        + ','
        + (hasUpperBound ? upperEndpoint : "\u221e")
        + (upperBoundType == CLOSED ? ']' : ')');
  }

  @CheckForNull
  T getLowerEndpoint() {
    return lowerEndpoint;
  }

  BoundType getLowerBoundType() {
    return lowerBoundType;
  }

  @CheckForNull
  T getUpperEndpoint() {
    return upperEndpoint;
  }

  BoundType getUpperBoundType() {
    return upperBoundType;
  }
}
