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

import com.google.common.annotations.Beta;
import com.google.common.annotations.GwtIncompatible;
import java.util.NoSuchElementException;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * A set comprising zero or more {@linkplain Range#isEmpty nonempty}, {@linkplain
 * Range#isConnected(Range) disconnected} ranges of type {@code C}.
 *
 * <p>Implementations that choose to support the {@link #add(Range)} operation are required to
 * ignore empty ranges and coalesce connected ranges. For example:
 *
 * <pre>{@code
 * RangeSet<Integer> rangeSet = TreeRangeSet.create();
 * rangeSet.add(Range.closed(1, 10)); // {[1, 10]}
 * rangeSet.add(Range.closedOpen(11, 15)); // disconnected range; {[1, 10], [11, 15)}
 * rangeSet.add(Range.closedOpen(15, 20)); // connected range; {[1, 10], [11, 20)}
 * rangeSet.add(Range.openClosed(0, 0)); // empty range; {[1, 10], [11, 20)}
 * rangeSet.remove(Range.open(5, 10)); // splits [1, 10]; {[1, 5], [10, 10], [11, 20)}
 * }</pre>
 *
 * <p>Note that the behavior of {@link Range#isEmpty()} and {@link Range#isConnected(Range)} may not
 * be as expected on discrete ranges. See the Javadoc of those methods for details.
 *
 * <p>For a {@link Set} whose contents are specified by a {@link Range}, see {@link ContiguousSet}.
 *
 * <p>See the Guava User Guide article on <a href=
 * "https://github.com/google/guava/wiki/NewCollectionTypesExplained#rangeset"> RangeSets</a>.
 *
 * @author Kevin Bourrillion
 * @author Louis Wasserman
 * @since 14.0
 */
@Beta
@GwtIncompatible
public interface RangeSet<C extends Comparable> {
  // TODO(lowasser): consider adding default implementations of some of these methods

  // Query methods

  /** Determines whether any of this range set's member ranges contains {@code value}. */
  boolean contains(C value);

  /**
   * Returns the unique range from this range set that {@linkplain Range#contains contains} {@code
   * value}, or {@code null} if this range set does not contain {@code value}.
   */
  Range<C> rangeContaining(C value);

  /**
   * Returns {@code true} if there exists a non-empty range enclosed by both a member range in this
   * range set and the specified range. This is equivalent to calling {@code
   * subRangeSet(otherRange)} and testing whether the resulting range set is non-empty.
   *
   * @since 20.0
   */
  boolean intersects(Range<C> otherRange);

  /**
   * Returns {@code true} if there exists a member range in this range set which {@linkplain
   * Range#encloses encloses} the specified range.
   */
  boolean encloses(Range<C> otherRange);

  /**
   * Returns {@code true} if for each member range in {@code other} there exists a member range in
   * this range set which {@linkplain Range#encloses encloses} it. It follows that {@code
   * this.contains(value)} whenever {@code other.contains(value)}. Returns {@code true} if {@code
   * other} is empty.
   *
   * <p>This is equivalent to checking if this range set {@link #encloses} each of the ranges in
   * {@code other}.
   */
  boolean enclosesAll(RangeSet<C> other);

  /**
   * Returns {@code true} if for each range in {@code other} there exists a member range in this
   * range set which {@linkplain Range#encloses encloses} it. Returns {@code true} if {@code other}
   * is empty.
   *
   * <p>This is equivalent to checking if this range set {@link #encloses} each range in {@code
   * other}.
   *
   * @since 21.0
   */
  default boolean enclosesAll(Iterable<Range<C>> other) {
    for (Range<C> range : other) {
      if (!encloses(range)) {
        return false;
      }
    }
    return true;
  }

  /** Returns {@code true} if this range set contains no ranges. */
  boolean isEmpty();

  /**
   * Returns the minimal range which {@linkplain Range#encloses(Range) encloses} all ranges in this
   * range set.
   *
   * @throws NoSuchElementException if this range set is {@linkplain #isEmpty() empty}
   */
  Range<C> span();

  // Views

  /**
   * Returns a view of the {@linkplain Range#isConnected disconnected} ranges that make up this
   * range set. The returned set may be empty. The iterators returned by its {@link
   * Iterable#iterator} method return the ranges in increasing order of lower bound (equivalently,
   * of upper bound).
   */
  Set<Range<C>> asRanges();

  /**
   * Returns a descending view of the {@linkplain Range#isConnected disconnected} ranges that make
   * up this range set. The returned set may be empty. The iterators returned by its {@link
   * Iterable#iterator} method return the ranges in decreasing order of lower bound (equivalently,
   * of upper bound).
   *
   * @since 19.0
   */
  Set<Range<C>> asDescendingSetOfRanges();

  /**
   * Returns a view of the complement of this {@code RangeSet}.
   *
   * <p>The returned view supports the {@link #add} operation if this {@code RangeSet} supports
   * {@link #remove}, and vice versa.
   */
  RangeSet<C> complement();

  /**
   * Returns a view of the intersection of this {@code RangeSet} with the specified range.
   *
   * <p>The returned view supports all optional operations supported by this {@code RangeSet}, with
   * the caveat that an {@link IllegalArgumentException} is thrown on an attempt to {@linkplain
   * #add(Range) add} any range not {@linkplain Range#encloses(Range) enclosed} by {@code view}.
   */
  RangeSet<C> subRangeSet(Range<C> view);

  // Modification

  /**
   * Adds the specified range to this {@code RangeSet} (optional operation). That is, for equal
   * range sets a and b, the result of {@code a.add(range)} is that {@code a} will be the minimal
   * range set for which both {@code a.enclosesAll(b)} and {@code a.encloses(range)}.
   *
   * <p>Note that {@code range} will be {@linkplain Range#span(Range) coalesced} with any ranges in
   * the range set that are {@linkplain Range#isConnected(Range) connected} with it. Moreover, if
   * {@code range} is empty, this is a no-op.
   *
   * @throws UnsupportedOperationException if this range set does not support the {@code add}
   *     operation
   */
  void add(Range<C> range);

  /**
   * Removes the specified range from this {@code RangeSet} (optional operation). After this
   * operation, if {@code range.contains(c)}, {@code this.contains(c)} will return {@code false}.
   *
   * <p>If {@code range} is empty, this is a no-op.
   *
   * @throws UnsupportedOperationException if this range set does not support the {@code remove}
   *     operation
   */
  void remove(Range<C> range);

  /**
   * Removes all ranges from this {@code RangeSet} (optional operation). After this operation,
   * {@code this.contains(c)} will return false for all {@code c}.
   *
   * <p>This is equivalent to {@code remove(Range.all())}.
   *
   * @throws UnsupportedOperationException if this range set does not support the {@code clear}
   *     operation
   */
  void clear();

  /**
   * Adds all of the ranges from the specified range set to this range set (optional operation).
   * After this operation, this range set is the minimal range set that {@linkplain
   * #enclosesAll(RangeSet) encloses} both the original range set and {@code other}.
   *
   * <p>This is equivalent to calling {@link #add} on each of the ranges in {@code other} in turn.
   *
   * @throws UnsupportedOperationException if this range set does not support the {@code addAll}
   *     operation
   */
  void addAll(RangeSet<C> other);

  /**
   * Adds all of the specified ranges to this range set (optional operation). After this operation,
   * this range set is the minimal range set that {@linkplain #enclosesAll(RangeSet) encloses} both
   * the original range set and each range in {@code other}.
   *
   * <p>This is equivalent to calling {@link #add} on each of the ranges in {@code other} in turn.
   *
   * @throws UnsupportedOperationException if this range set does not support the {@code addAll}
   *     operation
   * @since 21.0
   */
  default void addAll(Iterable<Range<C>> ranges) {
    for (Range<C> range : ranges) {
      add(range);
    }
  }

  /**
   * Removes all of the ranges from the specified range set from this range set (optional
   * operation). After this operation, if {@code other.contains(c)}, {@code this.contains(c)} will
   * return {@code false}.
   *
   * <p>This is equivalent to calling {@link #remove} on each of the ranges in {@code other} in
   * turn.
   *
   * @throws UnsupportedOperationException if this range set does not support the {@code removeAll}
   *     operation
   */
  void removeAll(RangeSet<C> other);

  /**
   * Removes all of the specified ranges from this range set (optional operation).
   *
   * <p>This is equivalent to calling {@link #remove} on each of the ranges in {@code other} in
   * turn.
   *
   * @throws UnsupportedOperationException if this range set does not support the {@code removeAll}
   *     operation
   * @since 21.0
   */
  default void removeAll(Iterable<Range<C>> ranges) {
    for (Range<C> range : ranges) {
      remove(range);
    }
  }

  // Object methods

  /**
   * Returns {@code true} if {@code obj} is another {@code RangeSet} that contains the same ranges
   * according to {@link Range#equals(Object)}.
   */
  @Override
  boolean equals(@Nullable Object obj);

  /** Returns {@code asRanges().hashCode()}. */
  @Override
  int hashCode();

  /**
   * Returns a readable string representation of this range set. For example, if this {@code
   * RangeSet} consisted of {@code Range.closed(1, 3)} and {@code Range.greaterThan(4)}, this might
   * return {@code " [1..3](4..+∞)}"}.
   */
  @Override
  String toString();
}
