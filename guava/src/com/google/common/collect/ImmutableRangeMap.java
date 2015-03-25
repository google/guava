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

import com.google.common.annotations.Beta;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.collect.SortedLists.KeyAbsentBehavior;
import com.google.common.collect.SortedLists.KeyPresentBehavior;

import java.io.Serializable;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;

import javax.annotation.Nullable;

/**
 * An immutable implementation of {@code RangeMap}, supporting all query operations efficiently.
 *
 * <p>Like all {@code RangeMap} implementations, this supports neither null keys nor null values.
 *
 * @author Louis Wasserman
 * @since 14.0
 */
@Beta
@GwtIncompatible("NavigableMap")
public class ImmutableRangeMap<K extends Comparable<?>, V> implements RangeMap<K, V>, Serializable {

  private static final ImmutableRangeMap<Comparable<?>, Object> EMPTY =
      new ImmutableRangeMap<Comparable<?>, Object>(
          ImmutableList.<Range<Comparable<?>>>of(), ImmutableList.of());

  /**
   * Returns an empty immutable range map.
   */
  @SuppressWarnings("unchecked")
  public static <K extends Comparable<?>, V> ImmutableRangeMap<K, V> of() {
    return (ImmutableRangeMap<K, V>) EMPTY;
  }

  /**
   * Returns an immutable range map mapping a single range to a single value.
   */
  public static <K extends Comparable<?>, V> ImmutableRangeMap<K, V> of(
      Range<K> range, V value) {
    return new ImmutableRangeMap<K, V>(ImmutableList.of(range), ImmutableList.of(value));
  }

  @SuppressWarnings("unchecked")
  public static <K extends Comparable<?>, V> ImmutableRangeMap<K, V> copyOf(
      RangeMap<K, ? extends V> rangeMap) {
    if (rangeMap instanceof ImmutableRangeMap) {
      return (ImmutableRangeMap<K, V>) rangeMap;
    }
    Map<Range<K>, ? extends V> map = rangeMap.asMapOfRanges();
    ImmutableList.Builder<Range<K>> rangesBuilder = new ImmutableList.Builder<Range<K>>(map.size());
    ImmutableList.Builder<V> valuesBuilder = new ImmutableList.Builder<V>(map.size());
    for (Entry<Range<K>, ? extends V> entry : map.entrySet()) {
      rangesBuilder.add(entry.getKey());
      valuesBuilder.add(entry.getValue());
    }
    return new ImmutableRangeMap<K, V>(rangesBuilder.build(), valuesBuilder.build());
  }

  /**
   * Returns a new builder for an immutable range map.
   */
  public static <K extends Comparable<?>, V> Builder<K, V> builder() {
    return new Builder<K, V>();
  }

  /**
   * A builder for immutable range maps. Overlapping ranges are prohibited.
   */
  public static final class Builder<K extends Comparable<?>, V> {
    private final RangeSet<K> keyRanges;
    private final RangeMap<K, V> rangeMap;

    public Builder() {
      this.keyRanges = TreeRangeSet.create();
      this.rangeMap = TreeRangeMap.create();
    }

    /**
     * Associates the specified range with the specified value.
     *
     * @throws IllegalArgumentException if {@code range} overlaps with any other ranges inserted
     *         into this builder, or if {@code range} is empty
     */
    public Builder<K, V> put(Range<K> range, V value) {
      checkNotNull(range);
      checkNotNull(value);
      checkArgument(!range.isEmpty(), "Range must not be empty, but was %s", range);
      if (!keyRanges.complement().encloses(range)) {
        // it's an error case; we can afford an expensive lookup
        for (Entry<Range<K>, V> entry : rangeMap.asMapOfRanges().entrySet()) {
          Range<K> key = entry.getKey();
          if (key.isConnected(range) && !key.intersection(range).isEmpty()) {
            throw new IllegalArgumentException(
                "Overlapping ranges: range " + range + " overlaps with entry " + entry);
          }
        }
      }
      keyRanges.add(range);
      rangeMap.put(range, value);
      return this;
    }

    /**
     * Copies all associations from the specified range map into this builder.
     *
     * @throws IllegalArgumentException if any of the ranges in {@code rangeMap} overlap with ranges
     *         already in this builder
     */
    public Builder<K, V> putAll(RangeMap<K, ? extends V> rangeMap) {
      for (Entry<Range<K>, ? extends V> entry : rangeMap.asMapOfRanges().entrySet()) {
        put(entry.getKey(), entry.getValue());
      }
      return this;
    }

    /**
     * Returns an {@code ImmutableRangeMap} containing the associations previously added to this
     * builder.
     */
    public ImmutableRangeMap<K, V> build() {
      Map<Range<K>, V> map = rangeMap.asMapOfRanges();
      ImmutableList.Builder<Range<K>> rangesBuilder =
          new ImmutableList.Builder<Range<K>>(map.size());
      ImmutableList.Builder<V> valuesBuilder = new ImmutableList.Builder<V>(map.size());
      for (Entry<Range<K>, V> entry : map.entrySet()) {
        rangesBuilder.add(entry.getKey());
        valuesBuilder.add(entry.getValue());
      }
      return new ImmutableRangeMap<K, V>(rangesBuilder.build(), valuesBuilder.build());
    }
  }

  private final transient ImmutableList<Range<K>> ranges;
  private final transient ImmutableList<V> values;

  ImmutableRangeMap(ImmutableList<Range<K>> ranges, ImmutableList<V> values) {
    this.ranges = ranges;
    this.values = values;
  }

  @Override
  @Nullable
  public V get(K key) {
    int index = SortedLists.binarySearch(ranges, Range.<K>lowerBoundFn(),
        Cut.belowValue(key), KeyPresentBehavior.ANY_PRESENT, KeyAbsentBehavior.NEXT_LOWER);
    if (index == -1) {
      return null;
    } else {
      Range<K> range = ranges.get(index);
      return range.contains(key) ? values.get(index) : null;
    }
  }

  @Override
  @Nullable
  public Map.Entry<Range<K>, V> getEntry(K key) {
    int index = SortedLists.binarySearch(ranges, Range.<K>lowerBoundFn(),
        Cut.belowValue(key), KeyPresentBehavior.ANY_PRESENT, KeyAbsentBehavior.NEXT_LOWER);
    if (index == -1) {
      return null;
    } else {
      Range<K> range = ranges.get(index);
      return range.contains(key) ? Maps.immutableEntry(range, values.get(index)) : null;
    }
  }

  @Override
  public Range<K> span() {
    if (ranges.isEmpty()) {
      throw new NoSuchElementException();
    }
    Range<K> firstRange = ranges.get(0);
    Range<K> lastRange = ranges.get(ranges.size() - 1);
    return Range.create(firstRange.lowerBound, lastRange.upperBound);
  }

  @Override
  public void put(Range<K> range, V value) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void putAll(RangeMap<K, V> rangeMap) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void clear() {
    throw new UnsupportedOperationException();
  }

  @Override
  public void remove(Range<K> range) {
    throw new UnsupportedOperationException();
  }

  @Override
  public ImmutableMap<Range<K>, V> asMapOfRanges() {
    if (ranges.isEmpty()) {
      return ImmutableMap.of();
    }
    RegularImmutableSortedSet<Range<K>> rangeSet =
        new RegularImmutableSortedSet<Range<K>>(ranges, Range.RANGE_LEX_ORDERING);
    return new RegularImmutableSortedMap<Range<K>, V>(rangeSet, values);
  }
  
  @Override
  public ImmutableRangeMap<K, V> subRangeMap(final Range<K> range) {
    if (checkNotNull(range).isEmpty()) {
      return ImmutableRangeMap.of();
    } else if (ranges.isEmpty() || range.encloses(span())) {
      return this;
    }
    int lowerIndex = SortedLists.binarySearch(
        ranges, Range.<K>upperBoundFn(), range.lowerBound,
        KeyPresentBehavior.FIRST_AFTER, KeyAbsentBehavior.NEXT_HIGHER);
    int upperIndex = SortedLists.binarySearch(ranges, 
        Range.<K>lowerBoundFn(), range.upperBound,
        KeyPresentBehavior.ANY_PRESENT, KeyAbsentBehavior.NEXT_HIGHER);
    if (lowerIndex >= upperIndex) {
      return ImmutableRangeMap.of();
    }
    final int off = lowerIndex;
    final int len = upperIndex - lowerIndex;
    ImmutableList<Range<K>> subRanges = new ImmutableList<Range<K>>() {
      @Override
      public int size() {
        return len;
      }

      @Override
      public Range<K> get(int index) {
        checkElementIndex(index, len);
        if (index == 0 || index == len - 1) {
          return ranges.get(index + off).intersection(range);
        } else {
          return ranges.get(index + off);
        }
      }

      @Override
      boolean isPartialView() {
        return true;
      }
    };
    final ImmutableRangeMap<K, V> outer = this;
    return new ImmutableRangeMap<K, V>(
        subRanges, values.subList(lowerIndex, upperIndex)) {
          @Override
          public ImmutableRangeMap<K, V> subRangeMap(Range<K> subRange) {
            if (range.isConnected(subRange)) {
              return outer.subRangeMap(subRange.intersection(range));
            } else {
              return ImmutableRangeMap.of();
            }
          }
    };
  }

  @Override
  public int hashCode() {
    return asMapOfRanges().hashCode();
  }

  @Override
  public boolean equals(@Nullable Object o) {
    if (o instanceof RangeMap) {
      RangeMap<?, ?> rangeMap = (RangeMap<?, ?>) o;
      return asMapOfRanges().equals(rangeMap.asMapOfRanges());
    }
    return false;
  }

  @Override
  public String toString() {
    return asMapOfRanges().toString();
  }

  /**
   * This class is used to serialize ImmutableRangeMap instances.
   * Serializes the {@link #asMapOfRanges()} form.
   */
  private static class SerializedForm<K extends Comparable<?>, V> implements Serializable {

    private final ImmutableMap<Range<K>, V> mapOfRanges;

    SerializedForm(ImmutableMap<Range<K>, V> mapOfRanges) {
      this.mapOfRanges = mapOfRanges;
    }

    Object readResolve() {
      if (mapOfRanges.isEmpty()) {
        return of();
      } else {
        return createRangeMap();
      }
    }

    Object createRangeMap() {
      Builder<K, V> builder = new Builder<K, V>();
      for (Entry<Range<K>, V> entry : mapOfRanges.entrySet()) {
        builder.put(entry.getKey(), entry.getValue());
      }
      return builder.build();
    }

    private static final long serialVersionUID = 0;
  }

  Object writeReplace() {
    return new SerializedForm<K, V>(asMapOfRanges());
  }

  private static final long serialVersionUID = 0;
}
