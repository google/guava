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

import com.google.common.annotations.GwtCompatible;
import com.google.common.base.Objects;
import com.google.common.base.Optional;

import java.io.Serializable;
import java.util.Comparator;

import javax.annotation.Nullable;

/**
 * A generalized interval on any ordering, for internal use. Does not support {@code null}. Unlike
 * {@link Range}, this allows the use of an arbitrary comparator. This is designed for use in the
 * implementation of subcollections of sorted collection types.
 *
 * <p>Whenever possible, use {@code Range} instead, which is better supported.
 *
 * @author Louis Wasserman
 */
@GwtCompatible(serializable = true)
final class GeneralRange<T> implements Serializable {
  /**
   * Converts a Range to a GeneralRange.
   */
  static <T extends Comparable> GeneralRange<T> from(Range<T> range) {
    Optional<T> lowerEndpoint =
        range.hasLowerBound() ? Optional.of(range.lowerEndpoint()) : Optional.<T>absent();
    BoundType lowerBoundType = range.hasLowerBound() ? range.lowerBoundType() : OPEN;
    Optional<T> upperEndpoint =
        range.hasUpperBound() ? Optional.of(range.upperEndpoint()) : Optional.<T>absent();
    BoundType upperBoundType = range.hasUpperBound() ? range.upperBoundType() : OPEN;
    return new GeneralRange<T>(
        Ordering.natural(), lowerEndpoint, lowerBoundType, upperEndpoint, upperBoundType);
  }

  /**
   * Returns the whole range relative to the specified comparator.
   */
  static <T> GeneralRange<T> all(Comparator<? super T> comparator) {
    return new GeneralRange<T>(comparator, Optional.<T>absent(), OPEN, Optional.<T>absent(), OPEN);
  }

  /**
   * Returns everything above the endpoint relative to the specified comparator, with the specified
   * endpoint behavior.
   */
  static <T> GeneralRange<T> downTo(
      Comparator<? super T> comparator, T endpoint, BoundType boundType) {
    return new GeneralRange<T>(
        comparator, Optional.of(endpoint), boundType, Optional.<T>absent(), OPEN);
  }

  /**
   * Returns everything below the endpoint relative to the specified comparator, with the specified
   * endpoint behavior.
   */
  static <T> GeneralRange<T> upTo(
      Comparator<? super T> comparator, T endpoint, BoundType boundType) {
    return new GeneralRange<T>(
        comparator, Optional.<T>absent(), OPEN, Optional.of(endpoint), boundType);
  }

  /**
   * Returns everything between the endpoints relative to the specified comparator, with the
   * specified endpoint behavior.
   */
  static <T> GeneralRange<T> range(Comparator<? super T> comparator, T lower, BoundType lowerType,
      T upper, BoundType upperType) {
    return new GeneralRange<T>(
        comparator, Optional.of(lower), lowerType, Optional.of(upper), upperType);
  }

  private final Comparator<? super T> comparator;
  private final Optional<T> lowerEndpoint;
  private final BoundType lowerBoundType;
  private final Optional<T> upperEndpoint;
  private final BoundType upperBoundType;

  private GeneralRange(Comparator<? super T> comparator, Optional<T> lowerEndpoint,
      BoundType lowerBoundType, Optional<T> upperEndpoint, BoundType upperBoundType) {
    this.comparator = checkNotNull(comparator);
    this.lowerEndpoint = checkNotNull(lowerEndpoint);
    this.lowerBoundType = checkNotNull(lowerBoundType);
    this.upperEndpoint = checkNotNull(upperEndpoint);
    this.upperBoundType = checkNotNull(upperBoundType);
    if (lowerEndpoint.isPresent() && upperEndpoint.isPresent()) {
      int cmp = comparator.compare(lowerEndpoint.get(), upperEndpoint.get());
      // be consistent with Range
      checkArgument(
          cmp <= 0, "lowerEndpoint (%s) > upperEndpoint (%s)", lowerEndpoint, upperEndpoint);
      if (cmp == 0) {
        checkArgument(lowerBoundType != OPEN | upperBoundType != OPEN);
      }
    }
  }

  Comparator<? super T> comparator() {
    return comparator;
  }

  boolean hasLowerBound() {
    return lowerEndpoint.isPresent();
  }

  boolean hasUpperBound() {
    return upperEndpoint.isPresent();
  }

  boolean isEmpty() {
    return (hasUpperBound() && tooLow(upperEndpoint.get()))
        || (hasLowerBound() && tooHigh(lowerEndpoint.get()));
  }

  boolean tooLow(T t) {
    if (!hasLowerBound()) {
      return false;
    }
    T lbound = lowerEndpoint.get();
    int cmp = comparator.compare(t, lbound);
    return cmp < 0 | (cmp == 0 & lowerBoundType == OPEN);
  }

  boolean tooHigh(T t) {
    if (!hasUpperBound()) {
      return false;
    }
    T ubound = upperEndpoint.get();
    int cmp = comparator.compare(t, ubound);
    return cmp > 0 | (cmp == 0 & upperBoundType == OPEN);
  }

  boolean contains(T t) {
    checkNotNull(t);
    return !tooLow(t) && !tooHigh(t);
  }

  /**
   * Returns the intersection of the two ranges, or an empty range if their intersection is empty.
   */
  GeneralRange<T> intersect(GeneralRange<T> other) {
    checkNotNull(other);
    checkArgument(comparator.equals(other.comparator));

    Optional<T> lowEnd = lowerEndpoint;
    BoundType lowType = lowerBoundType;
    if (!hasLowerBound()) {
      lowEnd = other.lowerEndpoint;
      lowType = other.lowerBoundType;
    } else if (other.hasLowerBound()) {
      int cmp = comparator.compare(lowerEndpoint.get(), other.lowerEndpoint.get());
      if (cmp < 0 || (cmp == 0 && other.lowerBoundType == OPEN)) {
        lowEnd = other.lowerEndpoint;
        lowType = other.lowerBoundType;
      }
    }

    Optional<T> upEnd = upperEndpoint;
    BoundType upType = upperBoundType;
    if (!hasUpperBound()) {
      upEnd = other.upperEndpoint;
      upType = other.upperBoundType;
    } else if (other.hasUpperBound()) {
      int cmp = comparator.compare(upperEndpoint.get(), other.upperEndpoint.get());
      if (cmp > 0 || (cmp == 0 && other.upperBoundType == OPEN)) {
        upEnd = other.upperEndpoint;
        upType = other.upperBoundType;
      }
    }

    if (lowEnd.isPresent() && upEnd.isPresent()) {
      int cmp = comparator.compare(lowEnd.get(), upEnd.get());
      if (cmp > 0 || (cmp == 0 && lowType == OPEN && upType == OPEN)) {
        // force allowed empty range
        lowEnd = upEnd;
        lowType = OPEN;
        upType = CLOSED;
      }
    }

    return new GeneralRange<T>(comparator, lowEnd, lowType, upEnd, upType);
  }

  @Override
  public boolean equals(@Nullable Object obj) {
    if (obj instanceof GeneralRange) {
      GeneralRange<?> r = (GeneralRange<?>) obj;
      return comparator.equals(r.comparator) && lowerEndpoint.equals(r.lowerEndpoint)
          && lowerBoundType.equals(r.lowerBoundType) && upperEndpoint.equals(r.upperEndpoint)
          && upperBoundType.equals(r.upperBoundType);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(
        comparator, lowerEndpoint, lowerBoundType, upperEndpoint, upperBoundType);
  }

  private transient GeneralRange<T> reverse;

  /**
   * Returns the same range relative to the reversed comparator.
   */
  public GeneralRange<T> reverse() {
    GeneralRange<T> result = reverse;
    if (result == null) {
      result =
          new GeneralRange<T>(Ordering.from(comparator).reverse(), upperEndpoint, upperBoundType,
              lowerEndpoint, lowerBoundType);
      result.reverse = this;
      return this.reverse = result;
    }
    return result;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append(comparator).append(":");
    switch (lowerBoundType) {
      case CLOSED:
        builder.append('[');
        break;
      case OPEN:
        builder.append('(');
        break;
    }
    if (hasLowerBound()) {
      builder.append(lowerEndpoint.get());
    } else {
      builder.append("-\u221e");
    }
    builder.append(',');
    if (hasUpperBound()) {
      builder.append(upperEndpoint.get());
    } else {
      builder.append("\u221e");
    }
    switch (upperBoundType) {
      case CLOSED:
        builder.append(']');
        break;
      case OPEN:
        builder.append(')');
        break;
    }
    return builder.toString();
  }
}
