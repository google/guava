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

import com.google.common.annotations.Beta;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.base.Objects;

import java.util.Collection;
import java.util.Comparator;
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
 * @since 14.0
 */
@Beta
@GwtIncompatible("uses NavigableMap")
public class TreeRangeSet<C extends Comparable<?>>
    extends AbstractRangeSet<C> {

  private final NavigableMap<Cut<C>, Range<C>> rangesByLowerBound;

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
    this.rangesByLowerBound = rangesByLowerCut;
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
      return rangesByLowerBound.values();
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
    Entry<Cut<C>, Range<C>> floorEntry = rangesByLowerBound.floorEntry(Cut.belowValue(value));
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
    Entry<Cut<C>, Range<C>> floorEntry = rangesByLowerBound.floorEntry(range.lowerBound);
    return floorEntry != null && floorEntry.getValue().encloses(range);
  }

  @Override
  public Range<C> span() {
    Entry<Cut<C>, Range<C>> firstEntry = rangesByLowerBound.firstEntry();
    Entry<Cut<C>, Range<C>> lastEntry = rangesByLowerBound.lastEntry();
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

    Entry<Cut<C>, Range<C>> entryBelowLB = rangesByLowerBound.lowerEntry(lbToAdd);
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

    Entry<Cut<C>, Range<C>> entryBelowUB = rangesByLowerBound.floorEntry(ubToAdd);
    if (entryBelowUB != null) {
      // { >
      Range<C> rangeBelowUB = entryBelowUB.getValue();
      if (rangeBelowUB.upperBound.compareTo(ubToAdd) >= 0) {
        // { > }, and we need to coalesce
        ubToAdd = rangeBelowUB.upperBound;
      }
    }

    // Remove ranges which are strictly enclosed.
    rangesByLowerBound.subMap(lbToAdd, ubToAdd).clear();

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

    Entry<Cut<C>, Range<C>> entryBelowLB = rangesByLowerBound.lowerEntry(rangeToRemove.lowerBound);
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

    Entry<Cut<C>, Range<C>> entryBelowUB = rangesByLowerBound.floorEntry(rangeToRemove.upperBound);
    if (entryBelowUB != null) {
      // { >
      Range<C> rangeBelowUB = entryBelowUB.getValue();
      if (rangeBelowUB.upperBound.compareTo(rangeToRemove.upperBound) >= 0) {
        // { > }
        replaceRangeWithSameLowerBound(
            new Range<C>(rangeToRemove.upperBound, rangeBelowUB.upperBound));
      }
    }

    rangesByLowerBound.subMap(rangeToRemove.lowerBound, rangeToRemove.upperBound).clear();
  }

  private void replaceRangeWithSameLowerBound(Range<C> range) {
    if (range.isEmpty()) {
      rangesByLowerBound.remove(range.lowerBound);
    } else {
      rangesByLowerBound.put(range.lowerBound, range);
    }
  }

  private transient RangeSet<C> complement;

  @Override
  public RangeSet<C> complement() {
    RangeSet<C> result = complement;
    return (result == null) ? complement = new Complement() : result;
  }
  
  private static final class RangesByUpperBound<C extends Comparable<?>>
      extends AbstractNavigableMap<Cut<C>, Range<C>> {
    private final NavigableMap<Cut<C>, Range<C>> rangesByLowerBound;
    
    /**
     * upperBoundWindow represents the headMap/subMap/tailMap view of the entire "ranges by upper
     * bound" map; it's a constraint on the *keys*, and does not affect the values.
     */
    private final Range<Cut<C>> upperBoundWindow;
    
    RangesByUpperBound(NavigableMap<Cut<C>, Range<C>> rangesByLowerBound) {
      this.rangesByLowerBound = rangesByLowerBound;
      this.upperBoundWindow = Range.all();
    }

    private RangesByUpperBound(
        NavigableMap<Cut<C>, Range<C>> rangesByLowerBound, Range<Cut<C>> upperBoundWindow) {
      this.rangesByLowerBound = rangesByLowerBound;
      this.upperBoundWindow = upperBoundWindow;
    }

    private NavigableMap<Cut<C>, Range<C>> subMap(Range<Cut<C>> window) {
      if (window.isConnected(upperBoundWindow)) {
        return new RangesByUpperBound<C>(rangesByLowerBound, window.intersection(upperBoundWindow));
      } else {
        return ImmutableSortedMap.of();
      }
    }

    @Override
    public NavigableMap<Cut<C>, Range<C>> subMap(
        Cut<C> fromKey, boolean fromInclusive, Cut<C> toKey, boolean toInclusive) {
      return subMap(Range.range(
          fromKey, BoundType.forBoolean(fromInclusive),
          toKey, BoundType.forBoolean(toInclusive)));
    }

    @Override
    public NavigableMap<Cut<C>, Range<C>> headMap(Cut<C> toKey, boolean inclusive) {
      return subMap(Range.upTo(toKey, BoundType.forBoolean(inclusive)));
    }

    @Override
    public NavigableMap<Cut<C>, Range<C>> tailMap(Cut<C> fromKey, boolean inclusive) {
      return subMap(Range.downTo(fromKey, BoundType.forBoolean(inclusive)));
    }

    @Override
    public Comparator<? super Cut<C>> comparator() {
      return Ordering.<Cut<C>>natural();
    }

    @Override
    public boolean containsKey(@Nullable Object key) {
      return get(key) != null;
    }

    @Override
    public Range<C> get(@Nullable Object key) {
      if (key instanceof Cut) {
        try {
          @SuppressWarnings("unchecked") // we catch CCEs
          Cut<C> cut = (Cut<C>) key;
          if (!upperBoundWindow.contains(cut)) {
            return null;
          }
          Entry<Cut<C>, Range<C>> candidate = rangesByLowerBound.lowerEntry(cut);
          if (candidate != null && candidate.getValue().upperBound.equals(cut)) {
            return candidate.getValue();
          }
        } catch (ClassCastException e) {
          return null;
        }
      }
      return null;
    }

    @Override
    Iterator<Entry<Cut<C>, Range<C>>> entryIterator() {
      /*
       * We want to start the iteration at the first range where the upper bound is in
       * upperBoundWindow.
       */
      final Iterator<Range<C>> backingItr;
      if (!upperBoundWindow.hasLowerBound()) {
        backingItr = rangesByLowerBound.values().iterator();
      } else {
        Entry<Cut<C>, Range<C>> lowerEntry =
            rangesByLowerBound.lowerEntry(upperBoundWindow.lowerEndpoint());
        if (lowerEntry == null) {
          backingItr = rangesByLowerBound.values().iterator();
        } else if (upperBoundWindow.lowerBound.isLessThan(lowerEntry.getValue().upperBound)) {
          backingItr = rangesByLowerBound.tailMap(lowerEntry.getKey(), true).values().iterator();
        } else {
          backingItr = rangesByLowerBound.tailMap(upperBoundWindow.lowerEndpoint(), true)
              .values().iterator();
        }
      }
      return new AbstractIterator<Entry<Cut<C>, Range<C>>>() {
        @Override
        protected Entry<Cut<C>, Range<C>> computeNext() {
          if (!backingItr.hasNext()) {
            return endOfData();
          }
          Range<C> range = backingItr.next();
          if (upperBoundWindow.upperBound.isLessThan(range.upperBound)) {
            return endOfData();
          } else {
            return Maps.immutableEntry(range.upperBound, range);
          }
        }
      };
    }

    @Override
    Iterator<Entry<Cut<C>, Range<C>>> descendingEntryIterator() {
      final PeekingIterator<Range<C>> backingItr;
      if (upperBoundWindow.hasUpperBound()) {
        backingItr = Iterators.peekingIterator(rangesByLowerBound.headMap(
            upperBoundWindow.upperEndpoint(), false).descendingMap().values().iterator());
      } else {
        backingItr =
            Iterators.peekingIterator(rangesByLowerBound.descendingMap().values().iterator());
      }
      if (backingItr.hasNext()
          && upperBoundWindow.upperBound.isLessThan(backingItr.peek().upperBound)) {
        backingItr.next();
      }
      return new AbstractIterator<Entry<Cut<C>, Range<C>>>() {
        @Override
        protected Entry<Cut<C>, Range<C>> computeNext() {
          if (!backingItr.hasNext()) {
            return endOfData();
          }
          Range<C> range = backingItr.next();
          return upperBoundWindow.lowerBound.isLessThan(range.upperBound) 
              ? Maps.immutableEntry(range.upperBound, range)
              : endOfData();
        }
      };
    }

    @Override
    public int size() {
      if (upperBoundWindow.equals(Range.all())) {
        return rangesByLowerBound.size();
      }
      return Iterators.size(entryIterator());
    }
    
    @Override
    public boolean isEmpty() {
      return upperBoundWindow.equals(Range.all())
          ? rangesByLowerBound.isEmpty()
          : !entryIterator().hasNext();
    }
  }
  
  private static final class ComplementRangesByLowerBound<C extends Comparable<?>>
      extends AbstractNavigableMap<Cut<C>, Range<C>> {
    private final NavigableMap<Cut<C>, Range<C>> positiveRangesByLowerBound;
    private final NavigableMap<Cut<C>, Range<C>> positiveRangesByUpperBound;
    
    /**
     * complementLowerBoundWindow represents the headMap/subMap/tailMap view of the entire
     * "complement ranges by lower bound" map; it's a constraint on the *keys*, and does not affect
     * the values.
     */
    private final Range<Cut<C>> complementLowerBoundWindow;
    
    ComplementRangesByLowerBound(NavigableMap<Cut<C>, Range<C>> positiveRangesByLowerBound) {
      this(positiveRangesByLowerBound, Range.<Cut<C>>all());
    }

    private ComplementRangesByLowerBound(NavigableMap<Cut<C>, Range<C>> positiveRangesByLowerBound,
        Range<Cut<C>> window) {
      this.positiveRangesByLowerBound = positiveRangesByLowerBound;
      this.positiveRangesByUpperBound = new RangesByUpperBound<C>(positiveRangesByLowerBound);
      this.complementLowerBoundWindow = window;
    }

    private NavigableMap<Cut<C>, Range<C>> subMap(Range<Cut<C>> subWindow) {
      if (!complementLowerBoundWindow.isConnected(subWindow)) {
        return ImmutableSortedMap.of(); 
      } else {
        subWindow = subWindow.intersection(complementLowerBoundWindow);
        return new ComplementRangesByLowerBound<C>(positiveRangesByLowerBound, subWindow);
      }
    }

    @Override
    public NavigableMap<Cut<C>, Range<C>> subMap(
        Cut<C> fromKey, boolean fromInclusive, Cut<C> toKey, boolean toInclusive) {
      return subMap(Range.range(
          fromKey, BoundType.forBoolean(fromInclusive),
          toKey, BoundType.forBoolean(toInclusive)));
    }

    @Override
    public NavigableMap<Cut<C>, Range<C>> headMap(Cut<C> toKey, boolean inclusive) {
      return subMap(Range.upTo(toKey, BoundType.forBoolean(inclusive)));
    }

    @Override
    public NavigableMap<Cut<C>, Range<C>> tailMap(Cut<C> fromKey, boolean inclusive) {
      return subMap(Range.downTo(fromKey, BoundType.forBoolean(inclusive)));
    }

    @Override
    public Comparator<? super Cut<C>> comparator() {
      return Ordering.<Cut<C>>natural();
    }

    @Override
    Iterator<Entry<Cut<C>, Range<C>>> entryIterator() {
      /*
       * firstComplementRangeLowerBound is the first complement range lower bound inside
       * complementLowerBoundWindow. Complement range lower bounds are either positive range upper
       * bounds, or Cut.belowAll().
       *
       * positiveItr starts at the first positive range with lower bound greater than
       * firstComplementRangeLowerBound. (Positive range lower bounds correspond to complement range
       * upper bounds.)
       */
      Collection<Range<C>> positiveRanges;
      if (complementLowerBoundWindow.hasLowerBound()) {
        positiveRanges = positiveRangesByUpperBound.tailMap(
            complementLowerBoundWindow.lowerEndpoint(),
            complementLowerBoundWindow.lowerBoundType() == BoundType.CLOSED).values();
      } else {
        positiveRanges = positiveRangesByUpperBound.values();
      }
      final PeekingIterator<Range<C>> positiveItr = Iterators.peekingIterator(
          positiveRanges.iterator());
      final Cut<C> firstComplementRangeLowerBound;
      if (complementLowerBoundWindow.contains(Cut.<C>belowAll()) && 
          (!positiveItr.hasNext() || positiveItr.peek().lowerBound != Cut.<C>belowAll())) {
        firstComplementRangeLowerBound = Cut.belowAll();
      } else if (positiveItr.hasNext()) {
        firstComplementRangeLowerBound = positiveItr.next().upperBound;
      } else {
        return Iterators.emptyIterator();
      }
      return new AbstractIterator<Entry<Cut<C>, Range<C>>>() {
        Cut<C> nextComplementRangeLowerBound = firstComplementRangeLowerBound;

        @Override
        protected Entry<Cut<C>, Range<C>> computeNext() {
          if (complementLowerBoundWindow.upperBound.isLessThan(nextComplementRangeLowerBound)
              || nextComplementRangeLowerBound == Cut.<C>aboveAll()) {
            return endOfData();
          }
          Range<C> negativeRange;
          if (positiveItr.hasNext()) {
            Range<C> positiveRange = positiveItr.next();
            negativeRange = Range.create(nextComplementRangeLowerBound, positiveRange.lowerBound);
            nextComplementRangeLowerBound = positiveRange.upperBound;
          } else {
            negativeRange = Range.create(nextComplementRangeLowerBound, Cut.<C>aboveAll());
            nextComplementRangeLowerBound = Cut.aboveAll();
          }
          return Maps.immutableEntry(negativeRange.lowerBound, negativeRange);
        }
      };
    }

    @Override
    Iterator<Entry<Cut<C>, Range<C>>> descendingEntryIterator() {
      Iterator<Range<C>> itr;
      /*
       * firstComplementRangeUpperBound is the upper bound of the last complement range with lower
       * bound inside complementLowerBoundWindow.
       *
       * positiveItr starts at the first positive range with upper bound less than
       * firstComplementRangeUpperBound. (Positive range upper bounds correspond to complement range
       * lower bounds.)
       */
      Cut<C> startingPoint = complementLowerBoundWindow.hasUpperBound()
          ? complementLowerBoundWindow.upperEndpoint()
          : Cut.<C>aboveAll();
      boolean inclusive = complementLowerBoundWindow.hasUpperBound()
          && complementLowerBoundWindow.upperBoundType() == BoundType.CLOSED;
      final PeekingIterator<Range<C>> positiveItr =
          Iterators.peekingIterator(positiveRangesByUpperBound.headMap(startingPoint, inclusive)
              .descendingMap().values().iterator());
      Cut<C> cut;
      if (positiveItr.hasNext()) {
        cut = positiveRangesByLowerBound.higherKey(positiveItr.peek().upperBound);
      } else if (!complementLowerBoundWindow.contains(Cut.<C>belowAll())
          || positiveRangesByLowerBound.containsKey(Cut.belowAll())) {
        return Iterators.emptyIterator();
      } else {
        cut = positiveRangesByLowerBound.higherKey(Cut.<C>belowAll());
      }
      final Cut<C> firstComplementRangeUpperBound = Objects.firstNonNull(cut, Cut.<C>aboveAll());
      return new AbstractIterator<Entry<Cut<C>, Range<C>>>() {
        Cut<C> nextComplementRangeUpperBound = firstComplementRangeUpperBound;

        @Override
        protected Entry<Cut<C>, Range<C>> computeNext() {
          if (nextComplementRangeUpperBound == Cut.<C>belowAll()) {
            return endOfData();
          } else if (positiveItr.hasNext()) {
            Range<C> positiveRange = positiveItr.next();
            Range<C> negativeRange =
                Range.create(positiveRange.upperBound, nextComplementRangeUpperBound);
            nextComplementRangeUpperBound = positiveRange.lowerBound;
            if (complementLowerBoundWindow.lowerBound.isLessThan(negativeRange.lowerBound)) {
              return Maps.immutableEntry(negativeRange.lowerBound, negativeRange);
            }
          } else if (complementLowerBoundWindow.lowerBound.isLessThan(Cut.<C>belowAll())) {
            Range<C> negativeRange =
                Range.create(Cut.<C>belowAll(), nextComplementRangeUpperBound);
            nextComplementRangeUpperBound = Cut.belowAll();
            return Maps.immutableEntry(Cut.<C>belowAll(), negativeRange);
          }
          return endOfData();
        }
      };
    }

    @Override
    public int size() {
      return Iterators.size(entryIterator());
    }

    @Override
    @Nullable
    public Range<C> get(Object key) {
      if (key instanceof Cut) {
        try {
          @SuppressWarnings("unchecked")
          Cut<C> cut = (Cut<C>) key;
          // tailMap respects the current window
          Entry<Cut<C>, Range<C>> firstEntry = tailMap(cut, true).firstEntry();
          if (firstEntry != null && firstEntry.getKey().equals(cut)) {
            return firstEntry.getValue();
          }
        } catch (ClassCastException e) {
          return null;
        }
      }
      return null;
    }

    @Override
    public boolean containsKey(Object key) {
      return get(key) != null;
    }
  }

  private final class Complement extends TreeRangeSet<C> {
    Complement() {
      super(new ComplementRangesByLowerBound<C>(TreeRangeSet.this.rangesByLowerBound));
    }

    @Override
    public void add(Range<C> rangeToAdd) {
      TreeRangeSet.this.remove(rangeToAdd);
    }

    @Override
    public void remove(Range<C> rangeToRemove) {
      TreeRangeSet.this.add(rangeToRemove);
    }

    @Override
    public boolean contains(C value) {
      return !TreeRangeSet.this.contains(value);
    }
    
    @Override
    public RangeSet<C> complement() {
      return TreeRangeSet.this;
    }
  }
}
