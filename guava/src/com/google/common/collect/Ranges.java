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

import com.google.common.annotations.Beta;
import com.google.common.annotations.GwtCompatible;

import java.util.NoSuchElementException;

/**
 * Static methods pertaining to {@link Range} instances.  Each of the
 * {@link Range nine types of ranges} can be constructed with a corresponding
 * factory method:
 *
 * <dl>
 * <dt>{@code (a..b)}
 * <dd>{@link #open}
 * <dt>{@code [a..b]}
 * <dd>{@link #closed}
 * <dt>{@code [a..b)}
 * <dd>{@link #closedOpen}
 * <dt>{@code (a..b]}
 * <dd>{@link #openClosed}
 * <dt>{@code (a..+∞)}
 * <dd>{@link #greaterThan}
 * <dt>{@code [a..+∞)}
 * <dd>{@link #atLeast}
 * <dt>{@code (-∞..b)}
 * <dd>{@link #lessThan}
 * <dt>{@code (-∞..b]}
 * <dd>{@link #atMost}
 * <dt>{@code (-∞..+∞)}
 * <dd>{@link #all}
 * </dl>
 *
 * <p>Additionally, {@link Range} instances can be constructed by passing the
 * {@link BoundType bound types} explicitly.
 *
 * <dl>
 * <dt>Bounded on both ends
 * <dd>{@link #range}
 * <dt>Unbounded on top ({@code (a..+∞)} or {@code (a..+∞)})
 * <dd>{@link #downTo}
 * <dt>Unbounded on bottom ({@code (-∞..b)} or {@code (-∞..b]})
 * <dd>{@link #upTo}
 * </dl>
 *
 * <p>See the Guava User Guide article on <a href=
 * "http://code.google.com/p/guava-libraries/wiki/RangesExplained">
 * {@code Range}</a>.
 *
 * @author Kevin Bourrillion
 * @author Gregory Kick
 * @since 10.0
 * @deprecated Use the corresponding method in {@link Range}.
 */
@Deprecated
@GwtCompatible
@Beta
public final class Ranges {
  private Ranges() {}

  /**
   * Returns a range that contains all values strictly greater than {@code
   * lower} and strictly less than {@code upper}.
   *
   * @throws IllegalArgumentException if {@code lower} is greater than <i>or
   *     equal to</i> {@code upper}
   */
  public static <C extends Comparable<?>> Range<C> open(C lower, C upper) {
    return Range.open(lower, upper);
  }

  /**
   * Returns a range that contains all values greater than or equal to
   * {@code lower} and less than or equal to {@code upper}.
   *
   * @throws IllegalArgumentException if {@code lower} is greater than {@code
   *     upper}
   */
  public static <C extends Comparable<?>> Range<C> closed(C lower, C upper) {
    return Range.closed(lower, upper);
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
    return Range.closedOpen(lower, upper);
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
    return Range.openClosed(lower, upper);
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
    return Range.range(lower, lowerType, upper, upperType);
  }

  /**
   * Returns a range that contains all values strictly less than {@code
   * endpoint}.
   */
  public static <C extends Comparable<?>> Range<C> lessThan(C endpoint) {
    return Range.lessThan(endpoint);
  }

  /**
   * Returns a range that contains all values less than or equal to
   * {@code endpoint}.
   */
  public static <C extends Comparable<?>> Range<C> atMost(C endpoint) {
    return Range.atMost(endpoint);
  }

  /**
   * Returns a range with no lower bound up to the given endpoint, which may be
   * either inclusive (closed) or exclusive (open).
   */
  public static <C extends Comparable<?>> Range<C> upTo(
      C endpoint, BoundType boundType) {
    return Range.upTo(endpoint, boundType);
  }

  /**
   * Returns a range that contains all values strictly greater than {@code
   * endpoint}.
   */
  public static <C extends Comparable<?>> Range<C> greaterThan(C endpoint) {
    return Range.greaterThan(endpoint);
  }

  /**
   * Returns a range that contains all values greater than or equal to
   * {@code endpoint}.
   */
  public static <C extends Comparable<?>> Range<C> atLeast(C endpoint) {
    return Range.atLeast(endpoint);
  }

  /**
   * Returns a range from the given endpoint, which may be either inclusive
   * (closed) or exclusive (open), with no upper bound.
   */
  public static <C extends Comparable<?>> Range<C> downTo(
      C endpoint, BoundType boundType) {
    return Range.downTo(endpoint, boundType);
  }

  /** Returns a range that contains every value of type {@code C}. */
  public static <C extends Comparable<?>> Range<C> all() {
    return Range.all();
  }

  /**
   * Returns a range that {@linkplain Range#contains(Comparable) contains} only
   * the given value. The returned range is {@linkplain BoundType#CLOSED closed}
   * on both ends.
   */
  public static <C extends Comparable<?>> Range<C> singleton(C value) {
    return Range.singleton(value);
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
    return Range.encloseAll(values);
  }
}
