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

import com.google.common.annotations.GwtIncompatible;

import java.util.Collection;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;

import javax.annotation.Nullable;

/**
 * An implementation of {@link RangeSet} backed by a {@link TreeMap}.
 *
 * @author Louis Wasserman
 */
@GwtIncompatible("uses NavigableMap") final class TreeRangeSet<C extends Comparable>
    extends RangeSet<C> {
  // TODO(user): override inefficient defaults

  private final NavigableMap<Cut<C>, Range<C>> rangesByLowerCut;

  /**
   * Creates an empty {@code TreeRangeSet} instance.
   */
  public static <C extends Comparable> TreeRangeSet<C> create() {
    return new TreeRangeSet<C>(new TreeMap<Cut<C>, Range<C>>());
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
    return (result == null) ? complement = createComplement() : result;
  }

  private RangeSet<C> createComplement() {
    return new StandardComplement<C>(this);
  }
}
