/*
 * Copyright (C) 2011 The Guava Authors
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

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.annotations.GwtIncompatible;
import com.google.common.base.Objects;

import java.util.AbstractSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.TreeMap;

import javax.annotation.Nullable;

/**
 * An implementation of {@link RangeSet} backed by a {@link TreeMap}.
 *
 * @author Louis Wasserman
 */
@GwtIncompatible("uses NavigableMap")final class TreeRangeSet<C extends Comparable<?>>
    extends AbstractRangeSet<C> {

  private final NavigableMap<Cut<C>, Range<C>> rangesByLowerCut;

  /**
   * Creates an empty {@code TreeRangeSet} instance.
   */
  public static <C extends Comparable<?>> TreeRangeSet<C> create() {
    return new TreeRangeSet<C>(new TreeMap<Cut<C>, Range<C>>());
  }

  /**
   * Returns a {@code TreeRangeSet} initialized with the ranges in the specified range set.
   */
  public static <C extends Comparable<?>> TreeRangeSet<C> create(RangeSet<C> rangeSet) {
    TreeRangeSet<C> result = create();
    result.addAll(rangeSet);
    return result;
  }

  private TreeRangeSet(NavigableMap<Cut<C>, Range<C>> rangesByLowerCut) {
    this.rangesByLowerCut = rangesByLowerCut;
  }

  private transient Set<Range<C>> asRanges;

  @Override
  public Set<Range<C>> asRanges() {
    Set<Range<C>> result = asRanges;
    return (result == null) ? asRanges = new AsRanges() : result;
  }

  final class AsRanges extends ForwardingCollection<Range<C>> implements Set<Range<C>> {
    @Override
    protected Collection<Range<C>> delegate() {
      return rangesByLowerCut.values();
    }

    @Override
    public int hashCode() {
      return Sets.hashCodeImpl(this);
    }

    @Override
    public boolean equals(@Nullable Object o) {
      return Sets.equalsImpl(this, o);
    }
  }

  @Override
  @Nullable
  public Range<C> rangeContaining(C value) {
    checkNotNull(value);
    Entry<Cut<C>, Range<C>> floorEntry = rangesByLowerCut.floorEntry(Cut.belowValue(value));
    if (floorEntry != null && floorEntry.getValue().contains(value)) {
      return floorEntry.getValue();
    } else {
      // TODO(kevinb): revisit this design choice
      return null;
    }
  }

  @Override
  public boolean encloses(Range<C> range) {
    checkNotNull(range);
    Entry<Cut<C>, Range<C>> floorEntry = rangesByLowerCut.floorEntry(range.lowerBound);
    return floorEntry != null && floorEntry.getValue().encloses(range);
  }

  @Override
  public Range<C> span() {
    Entry<Cut<C>, Range<C>> firstEntry = rangesByLowerCut.firstEntry();
    Entry<Cut<C>, Range<C>> lastEntry = rangesByLowerCut.lastEntry();
    if (firstEntry == null) {
      throw new NoSuchElementException();
    }
    return Range.create(firstEntry.getValue().lowerBound, lastEntry.getValue().upperBound);
  }

  @Override
  public void add(Range<C> rangeToAdd) {
    checkNotNull(rangeToAdd);

    if (rangeToAdd.isEmpty()) {
      return;
    }

    // We will use { } to illustrate ranges currently in the range set, and < >
    // to illustrate rangeToAdd.
    Cut<C> lbToAdd = rangeToAdd.lowerBound;
    Cut<C> ubToAdd = rangeToAdd.upperBound;

    Entry<Cut<C>, Range<C>> entryBelowLB = rangesByLowerCut.lowerEntry(lbToAdd);
    if (entryBelowLB != null) {
      // { <
      Range<C> rangeBelowLB = entryBelowLB.getValue();
      if (rangeBelowLB.upperBound.compareTo(lbToAdd) >= 0) {
        // { < }, and we will need to coalesce
        if (rangeBelowLB.upperBound.compareTo(ubToAdd) >= 0) {
          // { < > }
          ubToAdd = rangeBelowLB.upperBound;
          /*
           * TODO(cpovirk): can we just "return;" here? Or, can we remove this if() entirely? If
           * not, add tests to demonstrate the problem with each approach
           */
        }
        lbToAdd = rangeBelowLB.lowerBound;
      }
    }

    Entry<Cut<C>, Range<C>> entryBelowUB = rangesByLowerCut.floorEntry(ubToAdd);
    if (entryBelowUB != null) {
      // { >
      Range<C> rangeBelowUB = entryBelowUB.getValue();
      if (rangeBelowUB.upperBound.compareTo(ubToAdd) >= 0) {
        // { > }, and we need to coalesce
        ubToAdd = rangeBelowUB.upperBound;
      }
    }

    // Remove ranges which are strictly enclosed.
    rangesByLowerCut.subMap(lbToAdd, ubToAdd).clear();

    replaceRangeWithSameLowerBound(new Range<C>(lbToAdd, ubToAdd));
  }

  @Override
  public void remove(Range<C> rangeToRemove) {
    checkNotNull(rangeToRemove);

    if (rangeToRemove.isEmpty()) {
      return;
    }

    // We will use { } to illustrate ranges currently in the range set, and < >
    // to illustrate rangeToRemove.

    Entry<Cut<C>, Range<C>> entryBelowLB = rangesByLowerCut.lowerEntry(rangeToRemove.lowerBound);
    if (entryBelowLB != null) {
      // { <
      Range<C> rangeBelowLB = entryBelowLB.getValue();
      if (rangeBelowLB.upperBound.compareTo(rangeToRemove.lowerBound) >= 0) {
        // { < }, and we will need to subdivide
        if (rangeBelowLB.upperBound.compareTo(rangeToRemove.upperBound) >= 0) {
          // { < > }
          replaceRangeWithSameLowerBound(
              new Range<C>(rangeToRemove.upperBound, rangeBelowLB.upperBound));
        }
        replaceRangeWithSameLowerBound(
            new Range<C>(rangeBelowLB.lowerBound, rangeToRemove.lowerBound));
      }
    }

    Entry<Cut<C>, Range<C>> entryBelowUB = rangesByLowerCut.floorEntry(rangeToRemove.upperBound);
    if (entryBelowUB != null) {
      // { >
      Range<C> rangeBelowUB = entryBelowUB.getValue();
      if (rangeBelowUB.upperBound.compareTo(rangeToRemove.upperBound) >= 0) {
        // { > }
        replaceRangeWithSameLowerBound(
            new Range<C>(rangeToRemove.upperBound, rangeBelowUB.upperBound));
      }
    }

    rangesByLowerCut.subMap(rangeToRemove.lowerBound, rangeToRemove.upperBound).clear();
  }

  private void replaceRangeWithSameLowerBound(Range<C> range) {
    if (range.isEmpty()) {
      rangesByLowerCut.remove(range.lowerBound);
    } else {
      rangesByLowerCut.put(range.lowerBound, range);
    }
  }

  private transient RangeSet<C> complement;

  @Override
  public RangeSet<C> complement() {
    RangeSet<C> result = complement;
    return (result == null) ? complement = new Complement() : result;
  }

  private final class Complement extends AbstractRangeSet<C> {
    private RangeSet<C> positive() {
      return TreeRangeSet.this;
    }

    @Override
    public boolean contains(C value) {
      return !positive().contains(value);
    }

    @Override
    public Range<C> rangeContaining(C value) {
      Cut<C> valueCut = Cut.belowValue(value);

      Entry<Cut<C>, Range<C>> entryBelow = rangesByLowerCut.floorEntry(valueCut);
      Cut<C> lowerBound;
      if (entryBelow == null) {
        lowerBound = Cut.belowAll();
      } else {
        Range<C> rangeBelow = entryBelow.getValue();
        if (rangeBelow.contains(value)) {
          return null;
        } else {
          lowerBound = rangeBelow.upperBound;
        }
      }

      Cut<C> upperBound =
          Objects.firstNonNull(rangesByLowerCut.higherKey(valueCut), Cut.<C>aboveAll());
      return new Range<C>(lowerBound, upperBound);
    }

    @Override
    public Set<Range<C>> asRanges() {
      return new AbstractSet<Range<C>>() {
        @Override
        public Iterator<Range<C>> iterator() {
          return TreeRangeSet.this.standardComplementIterator();
        }

        @Override
        public int size() {
          boolean positiveBoundedBelow = !rangesByLowerCut.containsKey(Cut.belowAll());

          Entry<Cut<C>, Range<C>> lastEntry = rangesByLowerCut.lastEntry();
          boolean positiveBoundedAbove = lastEntry == null || lastEntry.getValue().hasUpperBound();

          int size = rangesByLowerCut.size() - 1;
          if (positiveBoundedBelow) {
            size++;
          }
          if (positiveBoundedAbove) {
            size++;
          }
          return size;
        }
      };
    }

    @Override
    public Range<C> span() {
      Cut<C> spanLowerBound;
      Entry<Cut<C>, Range<C>> firstEntry = rangesByLowerCut.firstEntry();
      if (firstEntry == null) {
        return Range.all();
      } else if (firstEntry.getValue().hasLowerBound()) {
        spanLowerBound = Cut.belowAll();
      } else {
        spanLowerBound = firstEntry.getValue().upperBound;
        if (Cut.aboveAll().equals(spanLowerBound)) {
          // TreeRangeSet.this contains the single range Range.all(), so the complement is empty
          throw new NoSuchElementException();
        }
      }

      Cut<C> spanUpperBound;
      Entry<Cut<C>, Range<C>> lastEntry = rangesByLowerCut.lastEntry();
      if (lastEntry.getValue().hasUpperBound()) {
        spanUpperBound = Cut.aboveAll();
      } else {
        spanUpperBound = lastEntry.getValue().lowerBound;
      }
      return Range.create(spanLowerBound, spanUpperBound);
    }

    @Override
    public boolean isEmpty() {
      return positive().equals(ImmutableRangeSet.<C>all());
    }

    @Override
    public RangeSet<C> complement() {
      return positive();
    }

    @Override
    public void add(Range<C> range) {
      positive().remove(range);
    }

    @Override
    public void remove(Range<C> range) {
      positive().add(range);
    }

    @Nullable
    Range<C> floorRange(Cut<C> cut) {
      // Ranges from the positive set that might border the complement range being requested.
      Iterator<Range<C>> candidatePositiveRanges =
          rangesByLowerCut.headMap(cut, false).descendingMap().values().iterator();

      if (candidatePositiveRanges.hasNext()) {
        Range<C> firstCandidate = candidatePositiveRanges.next();
        // If cut is |, and firstRange is { }, then we only know { |
        if (firstCandidate.upperBound.compareTo(cut) <= 0) {
          // { } |
          Cut<C> resultLowerBound = firstCandidate.upperBound;
          Cut<C> resultUpperBound = Objects.firstNonNull(
              rangesByLowerCut.higherKey(resultLowerBound), Cut.<C>aboveAll());
          return Range.create(resultLowerBound, resultUpperBound);
        } else if (candidatePositiveRanges.hasNext()) {
          // } { | }
          return Range.create(
              candidatePositiveRanges.next().upperBound, firstCandidate.lowerBound);
        } else if (Cut.belowAll().equals(firstCandidate.lowerBound)) {
          return null;
        } else {
          return Range.create(Cut.<C>belowAll(), firstCandidate.lowerBound);
        }
      } else if (rangesByLowerCut.isEmpty()) {
        return Range.all();
      } else {
        return Range.create(Cut.<C>belowAll(), rangesByLowerCut.firstKey());
      }
    }

    @Override
    public boolean encloses(Range<C> range) {
      checkNotNull(range);
      Range<C> floorRange = floorRange(range.lowerBound);
      return floorRange != null && floorRange.encloses(range);
    }
  }
}
