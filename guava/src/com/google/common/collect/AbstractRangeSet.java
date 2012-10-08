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

import java.util.Iterator;

import javax.annotation.Nullable;

/**
 * A skeletal implementation of {@code RangeSet}.
 *
 * @author Louis Wasserman
 */
abstract class AbstractRangeSet<C extends Comparable> implements RangeSet<C> {
  AbstractRangeSet() {}

  @Override
  public boolean contains(C value) {
    return rangeContaining(value) != null;
  }

  @Override
  public Range<C> rangeContaining(C value) {
    checkNotNull(value);
    for (Range<C> range : asRanges()) {
      if (range.contains(value)) {
        return range;
      }
    }
    return null;
  }

  @Override
  public boolean isEmpty() {
    return asRanges().isEmpty();
  }

  /**
   * An implementation for {@code complement().asRanges().iterator()}.
   */
  final Iterator<Range<C>> standardComplementIterator() {
    return new AbstractIterator<Range<C>>() {
      final Iterator<Range<C>> positiveIterator = AbstractRangeSet.this.asRanges().iterator();
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
  public void add(Range<C> range) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void remove(Range<C> range) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean enclosesAll(RangeSet<C> other) {
    for (Range<C> range : other.asRanges()) {
      if (!encloses(range)) {
        return false;
      }
    }
    return true;
  }

  @Override
  public void addAll(RangeSet<C> other) {
    for (Range<C> range : other.asRanges()) {
      add(range);
    }
  }

  @Override
  public void removeAll(RangeSet<C> other) {
    for (Range<C> range : other.asRanges()) {
      remove(range);
    }
  }

  @Override
  public boolean encloses(Range<C> otherRange) {
    for (Range<C> range : asRanges()) {
      if (range.encloses(otherRange)) {
        return true;
      }
    }
    return false;
  }

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
