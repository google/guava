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
import static com.google.common.collect.BoundType.OPEN;

import com.google.common.annotations.GwtCompatible;
import com.google.common.base.Objects;
import com.google.common.base.Optional;

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
@GwtCompatible
final class GeneralRange<T> {
  static <T> GeneralRange<T> all(Comparator<? super T> comparator) {
    return new GeneralRange<T>(comparator, Optional.<T>absent(), OPEN, Optional.<T>absent(), OPEN);
  }

  static <T> GeneralRange<T> downTo(
      Comparator<? super T> comparator, T endpoint, BoundType boundType) {
    return new GeneralRange<T>(
        comparator, Optional.of(endpoint), boundType, Optional.<T>absent(), OPEN);
  }

  static <T> GeneralRange<T> upTo(
      Comparator<? super T> comparator, T endpoint, BoundType boundType) {
    return new GeneralRange<T>(
        comparator, Optional.<T>absent(), OPEN, Optional.of(endpoint), boundType);
  }

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
}
