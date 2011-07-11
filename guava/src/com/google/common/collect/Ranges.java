/*
 * Copyright (C) 2009 The Guava Authors
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

package com.google.common.collect;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.annotations.Beta;
import com.google.common.annotations.GwtCompatible;
import com.google.common.collect.Cut.AboveValue;
import com.google.common.collect.Cut.BelowValue;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Static methods pertaining to {@link Range} instances.
 *
 * @author Kevin Bourrillion
 * @author Gregory Kick
 * @since Guava release 10
 */
@GwtCompatible
@Beta
public final class Ranges {
  private Ranges() {}

  static <C extends Comparable<?>> Range<C> create(
      Cut<C> lowerBound, Cut<C> upperBound) {
    return new Range<C>(lowerBound, upperBound);
  }

  /**
   * Returns a range that contains all values strictly greater than {@code
   * lower} and strictly less than {@code upper}.
   *
   * @throws IllegalArgumentException if {@code lower} is greater than <i>or
   *     equal to</i> {@code upper}
   */
  public static <C extends Comparable<?>> Range<C> open(C lower, C upper) {
    return create(new AboveValue<C>(lower), new BelowValue<C>(upper));
  }

  /**
   * Returns a range that contains all values greater than or equal to
   * {@code lower} and less than or equal to {@code upper}.
   *
   * @throws IllegalArgumentException if {@code lower} is greater than {@code
   *     upper}
   */
  public static <C extends Comparable<?>> Range<C> closed(C lower, C upper) {
    return create(new BelowValue<C>(lower), new AboveValue<C>(upper));
  }

  /**
   * Returns a range that contains all values greater than or equal to
   * {@code lower} and strictly less than {@code upper}.
   *
   * @throws IllegalArgumentException if {@code lower} is greater than {@code
   *     upper}
   */
  public static <C extends Comparable<?>> Range<C> closedOpen(
      C lower, C upper) {
    return create(new BelowValue<C>(lower), new BelowValue<C>(upper));
  }

  /**
   * Returns a range that contains all values strictly greater than {@code
   * lower} and less than or equal to {@code upper}.
   *
   * @throws IllegalArgumentException if {@code lower} is greater than {@code
   *     upper}
   */
  public static <C extends Comparable<?>> Range<C> openClosed(
      C lower, C upper) {
    return create(new AboveValue<C>(lower), new AboveValue<C>(upper));
  }

  /**
   * Returns a range that contains any value from {@code lower} to {@code
   * upper}, where each endpoint may be either inclusive (closed) or exclusive
   * (open).
   *
   * @throws IllegalArgumentException if {@code lower} is greater than {@code
   *     upper}
   */
  public static <C extends Comparable<?>> Range<C> range(
      C lower, BoundType lowerType, C upper, BoundType upperType) {
    checkNotNull(lowerType);
    checkNotNull(upperType);

    Cut<C> lowerBound = (lowerType == BoundType.OPEN)
        ? new AboveValue<C>(lower)
        : new BelowValue<C>(lower);
    Cut<C> upperBound = (upperType == BoundType.OPEN)
        ? new BelowValue<C>(upper)
        : new AboveValue<C>(upper);
    return create(lowerBound, upperBound);
  }

  /**
   * Returns a range that contains all values strictly less than {@code
   * endpoint}.
   */
  public static <C extends Comparable<?>> Range<C> lessThan(C endpoint) {
    return create(Ranges.<C>noLowerBound(), new BelowValue<C>(endpoint));
  }

  /**
   * Returns a range that contains all values strictly greater than {@code
   * endpoint}.
   */
  public static <C extends Comparable<?>> Range<C> greaterThan(C endpoint) {
    return create(new AboveValue<C>(endpoint), Ranges.<C>noUpperBound());
  }

  /**
   * Returns a range that contains all values greater than or equal to
   * {@code endpoint}.
   */
  public static <C extends Comparable<?>> Range<C> atLeast(C endpoint) {
    return create(new BelowValue<C>(endpoint), Ranges.<C>noUpperBound());
  }

  /**
   * Returns a range that contains all values less than or equal to
   * {@code endpoint}.
   */
  public static <C extends Comparable<?>> Range<C> atMost(C endpoint) {
    return create(Ranges.<C>noLowerBound(), new AboveValue<C>(endpoint));
  }

  /** Returns a range that contains every value of type {@code C}. */
  public static <C extends Comparable<?>> Range<C> all() {
    return create(Ranges.<C>noLowerBound(), Ranges.<C>noUpperBound());
  }

  /**
   * Returns a range that {@linkplain Range#contains(Comparable) contains} only
   * the given value. The returned range is {@linkplain BoundType#CLOSED closed}
   * on both ends.
   */
  public static <C extends Comparable<?>> Range<C> singleton(C value) {
    return closed(value, value);
  }

  /**
   * Returns the minimal range that
   * {@linkplain Range#contains(Comparable) contains} all of the given values.
   * The returned range is {@linkplain BoundType#CLOSED closed} on both ends.
   *
   * @throws ClassCastException if the parameters are not <i>mutually
   *     comparable</i>
   * @throws NoSuchElementException if {@code values} is empty
   * @throws NullPointerException if any of {@code values} is null
   */
  public static <C extends Comparable<?>> Range<C> encloseAll(
      Iterable<C> values) {
    checkNotNull(values);
    if (values instanceof ContiguousSet) {
      return ((ContiguousSet<C>) values).range();
    }
    Iterator<C> valueIterator = values.iterator();
    C min = checkNotNull(valueIterator.next());
    C max = min;
    while (valueIterator.hasNext()) {
      C value = checkNotNull(valueIterator.next());
      min = Ordering.natural().min(min, value);
      max = Ordering.natural().max(max, value);
    }
    return closed(min, max);
  }

  @SuppressWarnings("unchecked")
  private static <C extends Comparable<?>> Cut<C> noLowerBound() {
    return (Cut<C>) Cut.BELOW_ALL;
  }

  @SuppressWarnings("unchecked")
  private static <C extends Comparable<?>> Cut<C> noUpperBound() {
    return (Cut<C>) Cut.ABOVE_ALL;
  }
}
