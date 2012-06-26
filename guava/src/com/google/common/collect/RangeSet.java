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

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.AbstractSet;
import java.util.Iterator;
import java.util.Set;

import javax.annotation.Nullable;

/**
 * A set of values of type {@code C} made up of zero or more <i>disjoint</i> {@linkplain Range
 * ranges}.
 *
 * <p>It is guaranteed that {@linkplain Range#isConnected connected} ranges will be
 * {@linkplain Range#span coalesced} together, and that {@linkplain Range#isEmpty empty} ranges
 * will never be held in a {@code RangeSet}.
 *
 * <p>For a {@link Set} whose contents are specified by a {@link Range}, see {@link ContiguousSet}.
 *
 * @author Kevin Bourrillion
 * @author Louis Wasserman
 */ abstract class RangeSet<C extends Comparable> {
  RangeSet() {}

  /**
   * Determines whether any of this range set's member ranges contains {@code value}.
   */
  public boolean contains(C value) {
    return rangeContaining(value) != null;
  }

  /**
   * Returns the unique range from this range set that {@linkplain Range#contains contains}
   * {@code value}, or {@code null} if this range set does not contain {@code value}.
   */
  public Range<C> rangeContaining(C value) {
    checkNotNull(value);
    for (Range<C> range : asRanges()) {
      if (range.contains(value)) {
        return range;
      }
    }
    return null;
  }

  /**
   * Returns a view of the {@linkplain Range#isConnected disconnected} ranges that make up this
   * range set.  The returned set may be empty. The iterators returned by its
   * {@link Iterable#iterator} method return the ranges in increasing order of lower bound
   * (equivalently, of upper bound).
   */
  public abstract Set<Range<C>> asRanges();

  /**
   * Returns {@code true} if this range set contains no ranges.
   */
  public boolean isEmpty() {
    return asRanges().isEmpty();
  }

  /**
   * Returns a view of the complement of this {@code RangeSet}.
   *
   * <p>The returned view supports the {@link #add} operation if this {@code RangeSet} supports
   * {@link #remove}, and vice versa.
   */
  public abstract RangeSet<C> complement();

  /**
   * A basic, simple implementation of {@link #complement}. This is not efficient on all methods;
   * for example, {@link #rangeContaining} and {@link #encloses} are linear-time.
   */
  static class StandardComplement<C extends Comparable> extends RangeSet<C> {
    final RangeSet<C> positive;

    public StandardComplement(RangeSet<C> positive) {
      this.positive = positive;
    }

    @Override
    public boolean contains(C value) {
      return !positive.contains(value);
    }

    @Override
    public void add(Range<C> range) {
      positive.remove(range);
    }

    @Override
    public void remove(Range<C> range) {
      positive.add(range);
    }

    private transient Set<Range<C>> asRanges;

    @Override
    public final Set<Range<C>> asRanges() {
      Set<Range<C>> result = asRanges;
      return (result == null) ? asRanges = createAsRanges() : result;
    }

    Set<Range<C>> createAsRanges() {
      return new AbstractSet<Range<C>>() {

        @Override
        public Iterator<Range<C>> iterator() {
          final Iterator<Range<C>> positiveIterator = positive.asRanges().iterator();
          return new AbstractIterator<Range<C>>() {
            Cut<C> prevCut = Cut.belowAll();

            @Override
            protected Range<C> computeNext() {
              while (positiveIterator.hasNext()) {
                Cut<C> oldCut = prevCut;
                Range<C> positiveRange = positiveIterator.next();
                prevCut = positiveRange.upperBound;
                if (oldCut.compareTo(positiveRange.lowerBound) < 0) {
                  return new Range<C>(oldCut, positiveRange.lowerBound);
                }
              }
              Cut<C> posInfinity = Cut.aboveAll();
              if (prevCut.compareTo(posInfinity) < 0) {
                Range<C> result = new Range<C>(prevCut, posInfinity);
                prevCut = posInfinity;
                return result;
              }
              return endOfData();
            }
          };
        }

        @Override
        public int size() {
          return Iterators.size(iterator());
        }
      };
    }

    @Override
    public RangeSet<C> complement() {
      return positive;
    }
  }

  /**
   * Adds the specified range to this {@code RangeSet} (optional operation). That is, for equal
   * range sets a and b, the result of {@code a.add(range)} is that {@code a} will be the minimal
   * range set for which both {@code a.enclosesAll(b)} and {@code a.encloses(range)}.
   *
   * <p>Note that {@code range} will be {@linkplain Range#span(Range) coalesced} with any ranges in
   * the range set that are {@linkplain Range#isConnected(Range) connected} with it.  Moreover,
   * if {@code range} is empty, this is a no-op.
   *
   * @throws UnsupportedOperationException if this range set does not support the {@code add}
   *         operation
   */
  public void add(Range<C> range) {
    throw new UnsupportedOperationException();
  }

  /**
   * Removes the specified range from this {@code RangeSet} (optional operation). After this
   * operation, if {@code range.contains(c)}, {@code this.contains(c)} will return {@code false}.
   *
   * <p>If {@code range} is empty, this is a no-op.
   *
   * @throws UnsupportedOperationException if this range set does not support the {@code remove}
   *         operation
   */
  public void remove(Range<C> range) {
    throw new UnsupportedOperationException();
  }

  /**
   * Returns {@code true} if there exists a member range in this range set which
   * {@linkplain Range#encloses encloses} the specified range.
   */
  public boolean encloses(Range<C> otherRange) {
    for (Range<C> range : asRanges()) {
      if (range.encloses(otherRange)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Returns {@code true} if for each member range in {@code other} there exists a member range in
   * this range set which {@linkplain Range#encloses encloses} it. It follows that
   * {@code this.contains(value)} whenever {@code other.contains(value)}. Returns {@code true} if
   * {@code other} is empty.
   *
   * <p>
   * This is equivalent to checking if this range set {@link #encloses} each of the ranges in
   * {@code other}.
   */
  public boolean enclosesAll(RangeSet<C> other) {
    for (Range<C> range : other.asRanges()) {
      if (!encloses(range)) {
        return false;
      }
    }
    return true;
  }

  /**
   * Adds all of the ranges from the specified range set to this range set (optional operation).
   * After this operation, this range set is the minimal range set that
   * {@linkplain #enclosesAll(RangeSet) encloses} both the original range set and {@code other}.
   *
   * <p>
   * This is equivalent to calling {@link #add} on each of the ranges in {@code other} in turn.
   *
   * @throws UnsupportedOperationException if this range set does not support the {@code addAll}
   *         operation
   */
  public void addAll(RangeSet<C> other) {
    for (Range<C> range : other.asRanges()) {
      this.add(range);
    }
  }

  /**
   * Removes all of the ranges from the specified range set from this range set (optional
   * operation). After this operation, if {@code other.contains(c)}, {@code this.contains(c)} will
   * return {@code false}.
   *
   * <p>
   * This is equivalent to calling {@link #remove} on each of the ranges in {@code other} in turn.
   *
   * @throws UnsupportedOperationException if this range set does not support the {@code removeAll}
   *         operation
   */
  public void removeAll(RangeSet<C> other) {
    for (Range<C> range : other.asRanges()) {
      this.remove(range);
    }
  }

  /**
   * Returns {@code true} if {@code obj} is another {@code RangeSet} that contains the same ranges
   * according to {@link Range#equals(Object)}.
   */
  @Override
  public boolean equals(@Nullable Object obj) {
    if (obj instanceof RangeSet) {
      RangeSet<?> other = (RangeSet<?>) obj;
      return this.asRanges().equals(other.asRanges());
    }
    return false;
  }

  @Override
  public final int hashCode() {
    return asRanges().hashCode();
  }

  /**
   * Returns a readable string representation of this range set. For example, if this
   * {@code RangeSet} consisted of {@code Ranges.closed(1, 3)} and {@code Ranges.greaterThan(4)},
   * this might return {@code " [1‥3](4‥+∞)}"}.
   */
  @Override
  public final String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append('{');
    for (Range<C> range : asRanges()) {
      builder.append(range);
    }
    builder.append('}');
    return builder.toString();
  }
}
