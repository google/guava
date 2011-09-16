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

import java.io.Serializable;
import java.util.Comparator;

import javax.annotation.Nullable;

import com.google.common.annotations.GwtCompatible;
import com.google.common.base.Objects;

/**
 * A generalized interval on any ordering, for internal use. Supports {@code null}. Unlike
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
    @Nullable
    T lowerEndpoint = range.hasLowerBound() ? range.lowerEndpoint() : null;
    BoundType lowerBoundType = range.hasLowerBound() ? range.lowerBoundType() : OPEN;

    @Nullable
    T upperEndpoint = range.hasUpperBound() ? range.upperEndpoint() : null;
    BoundType upperBoundType = range.hasUpperBound() ? range.upperBoundType() : OPEN;
    return new GeneralRange<T>(Ordering.natural(), range.hasLowerBound(), lowerEndpoint,
        lowerBoundType, range.hasUpperBound(), upperEndpoint, upperBoundType);
  }

  /**
   * Returns the whole range relative to the specified comparator.
   */
  static <T> GeneralRange<T> all(Comparator<? super T> comparator) {
    return new GeneralRange<T>(comparator, false, null, OPEN, false, null, OPEN);
  }

  /**
   * Returns everything above the endpoint relative to the specified comparator, with the specified
   * endpoint behavior.
   */
  static <T> GeneralRange<T> downTo(Comparator<? super T> comparator, @Nullable T endpoint,
      BoundType boundType) {
    return new GeneralRange<T>(comparator, true, endpoint, boundType, false, null, OPEN);
  }

  /**
   * Returns everything below the endpoint relative to the specified comparator, with the specified
   * endpoint behavior.
   */
  static <T> GeneralRange<T> upTo(Comparator<? super T> comparator, @Nullable T endpoint,
      BoundType boundType) {
    return new GeneralRange<T>(comparator, false, null, OPEN, true, endpoint, boundType);
  }

  /**
   * Returns everything between the endpoints relative to the specified comparator, with the
   * specified endpoint behavior.
   */
  static <T> GeneralRange<T> range(Comparator<? super T> comparator, @Nullable T lower,
      BoundType lowerType, @Nullable T upper, BoundType upperType) {
    return new GeneralRange<T>(comparator, true, lower, lowerType, true, upper, upperType);
  }

  private final Comparator<? super T> comparator;
  private final boolean hasLowerBound;
  @Nullable
  private final T lowerEndpoint;
  private final BoundType lowerBoundType;
  private final boolean hasUpperBound;
  @Nullable
  private final T upperEndpoint;
  private final BoundType upperBoundType;

  private GeneralRange(Comparator<? super T> comparator, boolean hasLowerBound,
      @Nullable T lowerEndpoint, BoundType lowerBoundType, boolean hasUpperBound,
      @Nullable T upperEndpoint, BoundType upperBoundType) {
    this.comparator = checkNotNull(comparator);
    this.hasLowerBound = hasLowerBound;
    this.hasUpperBound = hasUpperBound;
    this.lowerEndpoint = lowerEndpoint;
    this.lowerBoundType = checkNotNull(lowerBoundType);
    this.upperEndpoint = upperEndpoint;
    this.upperBoundType = checkNotNull(upperBoundType);

    if (hasLowerBound) {
      comparator.compare(lowerEndpoint, lowerEndpoint);
    }
    if (hasUpperBound) {
      comparator.compare(upperEndpoint, upperEndpoint);
    }
    if (hasLowerBound && hasUpperBound) {
      int cmp = comparator.compare(lowerEndpoint, upperEndpoint);
      // be consistent with Range
      checkArgument(cmp <= 0, "lowerEndpoint (%s) > upperEndpoint (%s)", lowerEndpoint,
          upperEndpoint);
      if (cmp == 0) {
        checkArgument(lowerBoundType != OPEN | upperBoundType != OPEN);
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
    return (hasUpperBound() && tooLow(upperEndpoint))
        || (hasLowerBound() && tooHigh(lowerEndpoint));
  }

  boolean tooLow(@Nullable T t) {
    if (!hasLowerBound()) {
      return false;
    }
    T lbound = lowerEndpoint;
    int cmp = comparator.compare(t, lbound);
    return cmp < 0 | (cmp == 0 & lowerBoundType == OPEN);
  }

  boolean tooHigh(@Nullable T t) {
    if (!hasUpperBound()) {
      return false;
    }
    T ubound = upperEndpoint;
    int cmp = comparator.compare(t, ubound);
    return cmp > 0 | (cmp == 0 & upperBoundType == OPEN);
  }

  boolean contains(@Nullable T t) {
    return !tooLow(t) && !tooHigh(t);
  }

  /**
   * Returns the intersection of the two ranges, or an empty range if their intersection is empty.
   */
  GeneralRange<T> intersect(GeneralRange<T> other) {
    checkNotNull(other);
    checkArgument(comparator.equals(other.comparator));

    boolean hasLowBound = this.hasLowerBound;
    @Nullable
    T lowEnd = lowerEndpoint;
    BoundType lowType = lowerBoundType;
    if (!hasLowerBound()) {
      hasLowBound = other.hasLowerBound;
      lowEnd = other.lowerEndpoint;
      lowType = other.lowerBoundType;
    } else if (other.hasLowerBound()) {
      int cmp = comparator.compare(lowerEndpoint, other.lowerEndpoint);
      if (cmp < 0 || (cmp == 0 && other.lowerBoundType == OPEN)) {
        lowEnd = other.lowerEndpoint;
        lowType = other.lowerBoundType;
      }
    }

    boolean hasUpBound = this.hasUpperBound;
    @Nullable
    T upEnd = upperEndpoint;
    BoundType upType = upperBoundType;
    if (!hasUpperBound()) {
      hasUpBound = other.hasUpperBound;
      upEnd = other.upperEndpoint;
      upType = other.upperBoundType;
    } else if (other.hasUpperBound()) {
      int cmp = comparator.compare(upperEndpoint, other.upperEndpoint);
      if (cmp > 0 || (cmp == 0 && other.upperBoundType == OPEN)) {
        upEnd = other.upperEndpoint;
        upType = other.upperBoundType;
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

    return new GeneralRange<T>(comparator, hasLowBound, lowEnd, lowType, hasUpBound, upEnd, upType);
  }

  @Override
  public boolean equals(@Nullable Object obj) {
    if (obj instanceof GeneralRange) {
      GeneralRange<?> r = (GeneralRange<?>) obj;
      return comparator.equals(r.comparator) && hasLowerBound == r.hasLowerBound
          && hasUpperBound == r.hasUpperBound && lowerBoundType.equals(r.lowerBoundType)
          && upperBoundType.equals(r.upperBoundType)
          && Objects.equal(lowerEndpoint, r.lowerEndpoint)
          && Objects.equal(upperEndpoint, r.upperEndpoint);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(comparator, lowerEndpoint, lowerBoundType, upperEndpoint,
        upperBoundType);
  }

  private transient GeneralRange<T> reverse;

  /**
   * Returns the same range relative to the reversed comparator.
   */
  public GeneralRange<T> reverse() {
    GeneralRange<T> result = reverse;
    if (result == null) {
      result =
          new GeneralRange<T>(Ordering.from(comparator).reverse(), hasUpperBound, upperEndpoint,
              upperBoundType, hasLowerBound, lowerEndpoint, lowerBoundType);
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
      builder.append(lowerEndpoint);
    } else {
      builder.append("-\u221e");
    }
    builder.append(',');
    if (hasUpperBound()) {
      builder.append(upperEndpoint);
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
