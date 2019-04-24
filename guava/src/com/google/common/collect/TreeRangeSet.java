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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.annotations.Beta;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.MoreObjects;
import java.io.Serializable;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.TreeMap;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * An implementation of {@link RangeSet} backed by a {@link TreeMap}.
 *
 * @author Louis Wasserman
 * @since 14.0
 */
@Beta
@GwtIncompatible // uses NavigableMap
public class TreeRangeSet<C extends Comparable<?>> extends AbstractRangeSet<C>
    implements Serializable {

  @VisibleForTesting final NavigableMap<Cut<C>, Range<C>> rangesByLowerBound;

  /** Creates an empty {@code TreeRangeSet} instance. */
  public static <C extends Comparable<?>> TreeRangeSet<C> create() {
    return new TreeRangeSet<C>(new TreeMap<Cut<C>, Range<C>>());
  }

  /** Returns a {@code TreeRangeSet} initialized with the ranges in the specified range set. */
  public static <C extends Comparable<?>> TreeRangeSet<C> create(RangeSet<C> rangeSet) {
    TreeRangeSet<C> result = create();
    result.addAll(rangeSet);
    return result;
  }

  /**
   * Returns a {@code TreeRangeSet} representing the union of the specified ranges.
   *
   * <p>This is the smallest {@code RangeSet} which encloses each of the specified ranges. An
   * element will be contained in this {@code RangeSet} if and only if it is contained in at least
   * one {@code Range} in {@code ranges}.
   *
   * @since 21.0
   */
  public static <C extends Comparable<?>> TreeRangeSet<C> create(Iterable<Range<C>> ranges) {
    TreeRangeSet<C> result = create();
    result.addAll(ranges);
    return result;
  }

  private TreeRangeSet(NavigableMap<Cut<C>, Range<C>> rangesByLowerCut) {
    this.rangesByLowerBound = rangesByLowerCut;
  }

  private transient @MonotonicNonNull Set<Range<C>> asRanges;
  private transient @MonotonicNonNull Set<Range<C>> asDescendingSetOfRanges;

  @Override
  public Set<Range<C>> asRanges() {
    Set<Range<C>> result = asRanges;
    return (result == null) ? asRanges = new AsRanges(rangesByLowerBound.values()) : result;
  }

  @Override
  public Set<Range<C>> asDescendingSetOfRanges() {
    Set<Range<C>> result = asDescendingSetOfRanges;
    return (result == null)
        ? asDescendingSetOfRanges = new AsRanges(rangesByLowerBound.descendingMap().values())
        : result;
  }

  final class AsRanges extends ForwardingCollection<Range<C>> implements Set<Range<C>> {

    final Collection<Range<C>> delegate;

    AsRanges(Collection<Range<C>> delegate) {
      this.delegate = delegate;
    }

    @Override
    protected Collection<Range<C>> delegate() {
      return delegate;
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
  public @Nullable Range<C> rangeContaining(C value) {
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
  public boolean intersects(Range<C> range) {
    checkNotNull(range);
    Entry<Cut<C>, Range<C>> ceilingEntry = rangesByLowerBound.ceilingEntry(range.lowerBound);
    if (ceilingEntry != null
        && ceilingEntry.getValue().isConnected(range)
        && !ceilingEntry.getValue().intersection(range).isEmpty()) {
      return true;
    }
    Entry<Cut<C>, Range<C>> priorEntry = rangesByLowerBound.lowerEntry(range.lowerBound);
    return priorEntry != null
        && priorEntry.getValue().isConnected(range)
        && !priorEntry.getValue().intersection(range).isEmpty();
  }

  @Override
  public boolean encloses(Range<C> range) {
    checkNotNull(range);
    Entry<Cut<C>, Range<C>> floorEntry = rangesByLowerBound.floorEntry(range.lowerBound);
    return floorEntry != null && floorEntry.getValue().encloses(range);
  }

  private @Nullable Range<C> rangeEnclosing(Range<C> range) {
    checkNotNull(range);
    Entry<Cut<C>, Range<C>> floorEntry = rangesByLowerBound.floorEntry(range.lowerBound);
    return (floorEntry != null && floorEntry.getValue().encloses(range))
        ? floorEntry.getValue()
        : null;
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

    replaceRangeWithSameLowerBound(Range.create(lbToAdd, ubToAdd));
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
        if (rangeToRemove.hasUpperBound()
            && rangeBelowLB.upperBound.compareTo(rangeToRemove.upperBound) >= 0) {
          // { < > }
          replaceRangeWithSameLowerBound(
              Range.create(rangeToRemove.upperBound, rangeBelowLB.upperBound));
        }
        replaceRangeWithSameLowerBound(
            Range.create(rangeBelowLB.lowerBound, rangeToRemove.lowerBound));
      }
    }

    Entry<Cut<C>, Range<C>> entryBelowUB = rangesByLowerBound.floorEntry(rangeToRemove.upperBound);
    if (entryBelowUB != null) {
      // { >
      Range<C> rangeBelowUB = entryBelowUB.getValue();
      if (rangeToRemove.hasUpperBound()
          && rangeBelowUB.upperBound.compareTo(rangeToRemove.upperBound) >= 0) {
        // { > }
        replaceRangeWithSameLowerBound(
            Range.create(rangeToRemove.upperBound, rangeBelowUB.upperBound));
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

  private transient @MonotonicNonNull RangeSet<C> complement;

  @Override
  public RangeSet<C> complement() {
    RangeSet<C> result = complement;
    return (result == null) ? complement = new Complement() : result;
  }

  @VisibleForTesting
  static final class RangesByUpperBound<C extends Comparable<?>>
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
      return subMap(
          Range.range(
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
          backingItr =
              rangesByLowerBound
                  .tailMap(upperBoundWindow.lowerEndpoint(), true)
                  .values()
                  .iterator();
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
      Collection<Range<C>> candidates;
      if (upperBoundWindow.hasUpperBound()) {
        candidates =
            rangesByLowerBound
                .headMap(upperBoundWindow.upperEndpoint(), false)
                .descendingMap()
                .values();
      } else {
        candidates = rangesByLowerBound.descendingMap().values();
      }
      final PeekingIterator<Range<C>> backingItr = Iterators.peekingIterator(candidates.iterator());
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

    private ComplementRangesByLowerBound(
        NavigableMap<Cut<C>, Range<C>> positiveRangesByLowerBound, Range<Cut<C>> window) {
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
      return subMap(
          Range.range(
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
        positiveRanges =
            positiveRangesByUpperBound
                .tailMap(
                    complementLowerBoundWindow.lowerEndpoint(),
                    complementLowerBoundWindow.lowerBoundType() == BoundType.CLOSED)
                .values();
      } else {
        positiveRanges = positiveRangesByUpperBound.values();
      }
      final PeekingIterator<Range<C>> positiveItr =
          Iterators.peekingIterator(positiveRanges.iterator());
      final Cut<C> firstComplementRangeLowerBound;
      if (complementLowerBoundWindow.contains(Cut.<C>belowAll())
          && (!positiveItr.hasNext() || positiveItr.peek().lowerBound != Cut.<C>belowAll())) {
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
      /*
       * firstComplementRangeUpperBound is the upper bound of the last complement range with lower
       * bound inside complementLowerBoundWindow.
       *
       * positiveItr starts at the first positive range with upper bound less than
       * firstComplementRangeUpperBound. (Positive range upper bounds correspond to complement range
       * lower bounds.)
       */
      Cut<C> startingPoint =
          complementLowerBoundWindow.hasUpperBound()
              ? complementLowerBoundWindow.upperEndpoint()
              : Cut.<C>aboveAll();
      boolean inclusive =
          complementLowerBoundWindow.hasUpperBound()
              && complementLowerBoundWindow.upperBoundType() == BoundType.CLOSED;
      final PeekingIterator<Range<C>> positiveItr =
          Iterators.peekingIterator(
              positiveRangesByUpperBound
                  .headMap(startingPoint, inclusive)
                  .descendingMap()
                  .values()
                  .iterator());
      Cut<C> cut;
      if (positiveItr.hasNext()) {
        cut =
            (positiveItr.peek().upperBound == Cut.<C>aboveAll())
                ? positiveItr.next().lowerBound
                : positiveRangesByLowerBound.higherKey(positiveItr.peek().upperBound);
      } else if (!complementLowerBoundWindow.contains(Cut.<C>belowAll())
          || positiveRangesByLowerBound.containsKey(Cut.belowAll())) {
        return Iterators.emptyIterator();
      } else {
        cut = positiveRangesByLowerBound.higherKey(Cut.<C>belowAll());
      }
      final Cut<C> firstComplementRangeUpperBound =
          MoreObjects.firstNonNull(cut, Cut.<C>aboveAll());
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
            Range<C> negativeRange = Range.create(Cut.<C>belowAll(), nextComplementRangeUpperBound);
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
    public @Nullable Range<C> get(Object key) {
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

  private static final class SubRangeSetRangesByLowerBound<C extends Comparable<?>>
      extends AbstractNavigableMap<Cut<C>, Range<C>> {
    /**
     * lowerBoundWindow is the headMap/subMap/tailMap view; it only restricts the keys, and does not
     * affect the values.
     */
    private final Range<Cut<C>> lowerBoundWindow;

    /**
     * restriction is the subRangeSet view; ranges are truncated to their intersection with
     * restriction.
     */
    private final Range<C> restriction;

    private final NavigableMap<Cut<C>, Range<C>> rangesByLowerBound;
    private final NavigableMap<Cut<C>, Range<C>> rangesByUpperBound;

    private SubRangeSetRangesByLowerBound(
        Range<Cut<C>> lowerBoundWindow,
        Range<C> restriction,
        NavigableMap<Cut<C>, Range<C>> rangesByLowerBound) {
      this.lowerBoundWindow = checkNotNull(lowerBoundWindow);
      this.restriction = checkNotNull(restriction);
      this.rangesByLowerBound = checkNotNull(rangesByLowerBound);
      this.rangesByUpperBound = new RangesByUpperBound<C>(rangesByLowerBound);
    }

    private NavigableMap<Cut<C>, Range<C>> subMap(Range<Cut<C>> window) {
      if (!window.isConnected(lowerBoundWindow)) {
        return ImmutableSortedMap.of();
      } else {
        return new SubRangeSetRangesByLowerBound<C>(
            lowerBoundWindow.intersection(window), restriction, rangesByLowerBound);
      }
    }

    @Override
    public NavigableMap<Cut<C>, Range<C>> subMap(
        Cut<C> fromKey, boolean fromInclusive, Cut<C> toKey, boolean toInclusive) {
      return subMap(
          Range.range(
              fromKey,
              BoundType.forBoolean(fromInclusive),
              toKey,
              BoundType.forBoolean(toInclusive)));
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
    public @Nullable Range<C> get(@Nullable Object key) {
      if (key instanceof Cut) {
        try {
          @SuppressWarnings("unchecked") // we catch CCE's
          Cut<C> cut = (Cut<C>) key;
          if (!lowerBoundWindow.contains(cut)
              || cut.compareTo(restriction.lowerBound) < 0
              || cut.compareTo(restriction.upperBound) >= 0) {
            return null;
          } else if (cut.equals(restriction.lowerBound)) {
            // it might be present, truncated on the left
            Range<C> candidate = Maps.valueOrNull(rangesByLowerBound.floorEntry(cut));
            if (candidate != null && candidate.upperBound.compareTo(restriction.lowerBound) > 0) {
              return candidate.intersection(restriction);
            }
          } else {
            Range<C> result = rangesByLowerBound.get(cut);
            if (result != null) {
              return result.intersection(restriction);
            }
          }
        } catch (ClassCastException e) {
          return null;
        }
      }
      return null;
    }

    @Override
    Iterator<Entry<Cut<C>, Range<C>>> entryIterator() {
      if (restriction.isEmpty()) {
        return Iterators.emptyIterator();
      }
      final Iterator<Range<C>> completeRangeItr;
      if (lowerBoundWindow.upperBound.isLessThan(restriction.lowerBound)) {
        return Iterators.emptyIterator();
      } else if (lowerBoundWindow.lowerBound.isLessThan(restriction.lowerBound)) {
        // starts at the first range with upper bound strictly greater than restriction.lowerBound
        completeRangeItr =
            rangesByUpperBound.tailMap(restriction.lowerBound, false).values().iterator();
      } else {
        // starts at the first range with lower bound above lowerBoundWindow.lowerBound
        completeRangeItr =
            rangesByLowerBound
                .tailMap(
                    lowerBoundWindow.lowerBound.endpoint(),
                    lowerBoundWindow.lowerBoundType() == BoundType.CLOSED)
                .values()
                .iterator();
      }
      final Cut<Cut<C>> upperBoundOnLowerBounds =
          Ordering.natural()
              .min(lowerBoundWindow.upperBound, Cut.belowValue(restriction.upperBound));
      return new AbstractIterator<Entry<Cut<C>, Range<C>>>() {
        @Override
        protected Entry<Cut<C>, Range<C>> computeNext() {
          if (!completeRangeItr.hasNext()) {
            return endOfData();
          }
          Range<C> nextRange = completeRangeItr.next();
          if (upperBoundOnLowerBounds.isLessThan(nextRange.lowerBound)) {
            return endOfData();
          } else {
            nextRange = nextRange.intersection(restriction);
            return Maps.immutableEntry(nextRange.lowerBound, nextRange);
          }
        }
      };
    }

    @Override
    Iterator<Entry<Cut<C>, Range<C>>> descendingEntryIterator() {
      if (restriction.isEmpty()) {
        return Iterators.emptyIterator();
      }
      Cut<Cut<C>> upperBoundOnLowerBounds =
          Ordering.natural()
              .min(lowerBoundWindow.upperBound, Cut.belowValue(restriction.upperBound));
      final Iterator<Range<C>> completeRangeItr =
          rangesByLowerBound
              .headMap(
                  upperBoundOnLowerBounds.endpoint(),
                  upperBoundOnLowerBounds.typeAsUpperBound() == BoundType.CLOSED)
              .descendingMap()
              .values()
              .iterator();
      return new AbstractIterator<Entry<Cut<C>, Range<C>>>() {
        @Override
        protected Entry<Cut<C>, Range<C>> computeNext() {
          if (!completeRangeItr.hasNext()) {
            return endOfData();
          }
          Range<C> nextRange = completeRangeItr.next();
          if (restriction.lowerBound.compareTo(nextRange.upperBound) >= 0) {
            return endOfData();
          }
          nextRange = nextRange.intersection(restriction);
          if (lowerBoundWindow.contains(nextRange.lowerBound)) {
            return Maps.immutableEntry(nextRange.lowerBound, nextRange);
          } else {
            return endOfData();
          }
        }
      };
    }

    @Override
    public int size() {
      return Iterators.size(entryIterator());
    }
  }

  @Override
  public RangeSet<C> subRangeSet(Range<C> view) {
    return view.equals(Range.<C>all()) ? this : new SubRangeSet(view);
  }

  private final class SubRangeSet extends TreeRangeSet<C> {
    private final Range<C> restriction;

    SubRangeSet(Range<C> restriction) {
      super(
          new SubRangeSetRangesByLowerBound<C>(
              Range.<Cut<C>>all(), restriction, TreeRangeSet.this.rangesByLowerBound));
      this.restriction = restriction;
    }

    @Override
    public boolean encloses(Range<C> range) {
      if (!restriction.isEmpty() && restriction.encloses(range)) {
        Range<C> enclosing = TreeRangeSet.this.rangeEnclosing(range);
        return enclosing != null && !enclosing.intersection(restriction).isEmpty();
      }
      return false;
    }

    @Override
    public @Nullable Range<C> rangeContaining(C value) {
      if (!restriction.contains(value)) {
        return null;
      }
      Range<C> result = TreeRangeSet.this.rangeContaining(value);
      return (result == null) ? null : result.intersection(restriction);
    }

    @Override
    public void add(Range<C> rangeToAdd) {
      checkArgument(
          restriction.encloses(rangeToAdd),
          "Cannot add range %s to subRangeSet(%s)",
          rangeToAdd,
          restriction);
      super.add(rangeToAdd);
    }

    @Override
    public void remove(Range<C> rangeToRemove) {
      if (rangeToRemove.isConnected(restriction)) {
        TreeRangeSet.this.remove(rangeToRemove.intersection(restriction));
      }
    }

    @Override
    public boolean contains(C value) {
      return restriction.contains(value) && TreeRangeSet.this.contains(value);
    }

    @Override
    public void clear() {
      TreeRangeSet.this.remove(restriction);
    }

    @Override
    public RangeSet<C> subRangeSet(Range<C> view) {
      if (view.encloses(restriction)) {
        return this;
      } else if (view.isConnected(restriction)) {
        return new SubRangeSet(restriction.intersection(view));
      } else {
        return ImmutableRangeSet.of();
      }
    }
  }
}
