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
package com.google.common.collect;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkElementIndex;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.SortedLists.KeyAbsentBehavior.NEXT_LOWER;
import static com.google.common.collect.SortedLists.KeyPresentBehavior.ANY_PRESENT;

import com.google.common.base.Function;

import java.io.Serializable;
import java.util.Comparator;

/**
 * An efficient immutable implementation of a {@link RangeSet}.
 *
 * @author Louis Wasserman
 */
final class ImmutableRangeSet<C extends Comparable> extends AbstractRangeSet<C>
    implements Serializable {

  @SuppressWarnings("unchecked")
  private static final ImmutableRangeSet EMPTY = new ImmutableRangeSet(ImmutableList.of());

  @SuppressWarnings("unchecked")
  private static final ImmutableRangeSet ALL = new ImmutableRangeSet(ImmutableList.of(Range.all()));

  /**
   * Returns an empty immutable range set.
   */
  @SuppressWarnings("unchecked")
  public static <C extends Comparable> ImmutableRangeSet<C> of() {
    return EMPTY;
  }

  /**
   * Returns an immutable range set containing the single range {@link Range#all()}.
   */
  @SuppressWarnings("unchecked")
  public static <C extends Comparable> ImmutableRangeSet<C> all() {
    return ALL;
  }

  /**
   * Returns an immutable range set containing the specified single range. If {@link Range#isEmpty()
   * range.isEmpty()}, this is equivalent to {@link ImmutableRangeSet#of()}.
   */
  public static <C extends Comparable> ImmutableRangeSet<C> of(Range<C> range) {
    checkNotNull(range);
    if (range.isEmpty()) {
      return of();
    } else if (range.equals(Range.all())) {
      return all();
    } else {
      return new ImmutableRangeSet<C>(ImmutableList.of(range));
    }
  }

  /**
   * Returns an immutable copy of the specified {@code RangeSet}.
   */
  public static <C extends Comparable> ImmutableRangeSet<C> copyOf(RangeSet<C> rangeSet) {
    checkNotNull(rangeSet);
    if (rangeSet.isEmpty()) {
      return of();
    } else if (rangeSet.encloses(Range.<C>all())) {
      return all();
    }

    if (rangeSet instanceof ImmutableRangeSet) {
      ImmutableRangeSet<C> immutableRangeSet = (ImmutableRangeSet<C>) rangeSet;
      if (!immutableRangeSet.isPartialView()) {
        return immutableRangeSet;
      }
    }
    return new ImmutableRangeSet<C>(ImmutableList.copyOf(rangeSet.asRanges()));
  }

  ImmutableRangeSet(ImmutableList<Range<C>> ranges) {
    this.ranges = ranges;
  }

  private ImmutableRangeSet(ImmutableList<Range<C>> ranges, ImmutableRangeSet<C> complement) {
    this.ranges = ranges;
    this.complement = complement;
  }

  private transient final ImmutableList<Range<C>> ranges;

  @Override
  public boolean encloses(Range<C> otherRange) {
    Function<Range<C>, Cut<C>> lowerBoundFunction = new Function<Range<C>, Cut<C>>() {
      @Override
      public Cut<C> apply(Range<C> input) {
        return input.lowerBound;
      }
    };
    int index = SortedLists.binarySearch(ranges,
        lowerBoundFunction,
        otherRange.lowerBound,
        Ordering.natural(),
        ANY_PRESENT,
        NEXT_LOWER);
    return index != -1 && ranges.get(index).encloses(otherRange);
  }

  @Override
  public Range<C> rangeContaining(C value) {
    Function<Range<C>, Cut<C>> lowerBoundFunction = new Function<Range<C>, Cut<C>>() {
      @Override
      public Cut<C> apply(Range<C> input) {
        return input.lowerBound;
      }
    };
    int index = SortedLists.binarySearch(ranges,
        lowerBoundFunction,
        Cut.belowValue(value),
        Ordering.natural(),
        ANY_PRESENT,
        NEXT_LOWER);
    if (index != -1) {
      Range<C> range = ranges.get(index);
      return range.contains(value) ? range : null;
    }
    return null;
  }

  @Override
  public boolean isEmpty() {
    return ranges.isEmpty();
  }

  @Override
  public void add(Range<C> range) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void addAll(RangeSet<C> other) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void remove(Range<C> range) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void removeAll(RangeSet<C> other) {
    throw new UnsupportedOperationException();
  }

  @Override
  public ImmutableSet<Range<C>> asRanges() {
    if (ranges.isEmpty()) {
      return ImmutableSet.of();
    }
    return new RegularImmutableSortedSet<Range<C>>(ranges, new Comparator<Range<C>>() {
      @Override
      public int compare(Range<C> range1, Range<C> range2) {
        return ComparisonChain.start().compare(range1.lowerBound, range2.lowerBound)
            .compare(range1.upperBound, range2.upperBound).result();
      }
    });
  }

  private transient ImmutableRangeSet<C> complement;

  private final class ComplementRanges extends ImmutableList<Range<C>> {
    // True if the "positive" range set is empty or bounded below.
    private final boolean positiveBoundedBelow;

    // True if the "positive" range set is empty or bounded above.
    private final boolean positiveBoundedAbove;

    private final int size;

    ComplementRanges() {
      this.positiveBoundedBelow = ranges.get(0).hasLowerBound();
      this.positiveBoundedAbove = Iterables.getLast(ranges).hasUpperBound();

      int size = ranges.size() - 1;
      if (positiveBoundedBelow) {
        size++;
      }
      if (positiveBoundedAbove) {
        size++;
      }
      this.size = size;
    }

    @Override
    public int size() {
      return size;
    }

    @Override
    public Range<C> get(int index) {
      checkElementIndex(index, size);

      Cut<C> lowerBound;
      if (positiveBoundedBelow) {
        lowerBound = (index == 0) ? Cut.<C>belowAll() : ranges.get(index - 1).upperBound;
      } else {
        lowerBound = ranges.get(index).upperBound;
      }

      Cut<C> upperBound;
      if (positiveBoundedAbove && index == size - 1) {
        upperBound = Cut.<C>aboveAll();
      } else {
        upperBound = ranges.get(index + (positiveBoundedBelow ? 0 : 1)).lowerBound;
      }

      return new Range<C>(lowerBound, upperBound);
    }

    @Override
    boolean isPartialView() {
      return true;
    }
  }

  @Override
  public ImmutableRangeSet<C> complement() {
    ImmutableRangeSet<C> result = complement;
    if (result != null) {
      return result;
    } else if (ranges.isEmpty()) {
      return complement = all();
    } else if (ranges.size() == 1 && ranges.get(0).equals(Range.all())) {
      return complement = of();
    } else {
      ImmutableList<Range<C>> complementRanges = new ComplementRanges();
      result = complement = new ImmutableRangeSet<C>(complementRanges, this);
    }
    return result;
  }

  boolean isPartialView() {
    return ranges.isPartialView();
  }

  /**
   * Returns a new builder for an immutable range set.
   */
  public static <C extends Comparable<?>> Builder<C> builder() {
    return new Builder<C>();
  }

  /**
   * A builder for immutable range sets.
   */
  public static class Builder<C extends Comparable<?>> {
    private final RangeSet<C> rangeSet;

    public Builder() {
      this.rangeSet = TreeRangeSet.create();
    }

    /**
     * Add the specified range to this builder.  {@linkplain Range#isConnected Connected} ranges
     * will be {@linkplain Range#span(Range) coalesced}.
     *
     * @throws IllegalArgumentException if {@code range} is empty or overlaps any ranges already
     *         added to the builder
     */
    public Builder<C> add(Range<C> range) {
      if (range.isEmpty()) {
        throw new IllegalArgumentException("range must not be empty, but was " + range);
      } else if (!rangeSet.complement().encloses(range)) {
        for (Range<C> currentRange : rangeSet.asRanges()) {
          checkArgument(
              !currentRange.isConnected(range) || currentRange.intersection(range).isEmpty(),
              "Ranges may not overlap, but received %s and %s", currentRange, range);
        }
        throw new AssertionError("should have thrown an IAE above");
      }
      rangeSet.add(range);
      return this;
    }

    /**
     * Add all ranges from the specified range set to this builder. Duplicate or connected ranges
     * are permitted, and will be merged in the resulting immutable range set.
     */
    public Builder<C> addAll(RangeSet<C> ranges) {
      for (Range<C> range : ranges.asRanges()) {
        add(range);
      }
      return this;
    }

    /**
     * Returns an {@code ImmutableRangeSet} containing the ranges added to this builder.
     */
    public ImmutableRangeSet<C> build() {
      return copyOf(rangeSet);
    }
  }

  private static final class SerializedForm<C extends Comparable> implements Serializable {
    private final ImmutableList<Range<C>> ranges;

    SerializedForm(ImmutableList<Range<C>> ranges) {
      this.ranges = ranges;
    }

    Object readResolve() {
      if (ranges.isEmpty()) {
        return of();
      } else if (ranges.equals(ImmutableList.of(Range.all()))) {
        return all();
      } else {
        return new ImmutableRangeSet<C>(ranges);
      }
    }
  }

  Object writeReplace() {
    return new SerializedForm<C>(ranges);
  }
}
