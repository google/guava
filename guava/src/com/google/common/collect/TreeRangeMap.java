/*
 * Copyright (C) 2012 The Guava Authors
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

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.annotations.Beta;
import com.google.common.annotations.GwtIncompatible;

import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.Set;

import javax.annotation.Nullable;

/**
 * An implementation of {@code RangeMap} based on a {@code TreeMap}, supporting
 * all optional operations.
 *
 * <p>Like all {@code RangeMap} implementations, this supports neither null
 * keys nor null values.
 *
 * @author Louis Wasserman
 * @since 14.0
 */
@Beta
@GwtIncompatible("NavigableMap")
public final class TreeRangeMap<K extends Comparable, V> implements RangeMap<K, V> {

  private final NavigableMap<Cut<K>, RangeMapEntry<K, V>> entriesByLowerBound;

  public static <K extends Comparable, V> TreeRangeMap<K, V> create() {
    return new TreeRangeMap<K, V>();
  }

  private TreeRangeMap() {
    this.entriesByLowerBound = Maps.newTreeMap();
  }

  private static final class RangeMapEntry<K extends Comparable, V>
      extends AbstractMapEntry<Range<K>, V> {
    private final Range<K> range;
    private final V value;

    RangeMapEntry(Cut<K> lowerBound, Cut<K> upperBound, V value) {
      this(new Range<K>(lowerBound, upperBound), value);
    }

    RangeMapEntry(Range<K> range, V value) {
      this.range = range;
      this.value = value;
    }

    @Override
    public Range<K> getKey() {
      return range;
    }

    @Override
    public V getValue() {
      return value;
    }

    public boolean contains(K value) {
      return range.contains(value);
    }

    Cut<K> getLowerBound() {
      return range.lowerBound;
    }

    Cut<K> getUpperBound() {
      return range.upperBound;
    }
  }

  @Override
  @Nullable
  public V get(K key) {
    Entry<Range<K>, V> entry = getEntry(key);
    return (entry == null) ? null : entry.getValue();
  }

  @Override
  @Nullable
  public Entry<Range<K>, V> getEntry(K key) {
    Map.Entry<Cut<K>, RangeMapEntry<K, V>> mapEntry =
        entriesByLowerBound.floorEntry(Cut.belowValue(key));
    if (mapEntry != null && mapEntry.getValue().contains(key)) {
      return mapEntry.getValue();
    } else {
      return null;
    }
  }

  @Override
  public void put(Range<K> range, V value) {
    if (!range.isEmpty()) {
      checkNotNull(value);
      remove(range);
      entriesByLowerBound.put(range.lowerBound, new RangeMapEntry<K, V>(range, value));
    }
  }

  @Override
  public void putAll(RangeMap<K, V> rangeMap) {
    for (Map.Entry<Range<K>, V> entry : rangeMap.asMapOfRanges().entrySet()) {
      put(entry.getKey(), entry.getValue());
    }
  }

  @Override
  public void clear() {
    entriesByLowerBound.clear();
  }

  @Override
  public Range<K> span() {
    Entry<Cut<K>, RangeMapEntry<K, V>> firstEntry = entriesByLowerBound.firstEntry();
    Entry<Cut<K>, RangeMapEntry<K, V>> lastEntry = entriesByLowerBound.lastEntry();
    if (firstEntry == null) {
      throw new NoSuchElementException();
    }
    return Range.create(
        firstEntry.getValue().getKey().lowerBound,
        lastEntry.getValue().getKey().upperBound);
  }

  private void putRangeMapEntry(Cut<K> lowerBound, Cut<K> upperBound, V value) {
    entriesByLowerBound.put(lowerBound, new RangeMapEntry<K, V>(lowerBound, upperBound, value));
  }

  @Override
  public void remove(Range<K> rangeToRemove) {
    if (rangeToRemove.isEmpty()) {
      return;
    }

    /*
     * The comments for this method will use [ ] to indicate the bounds of rangeToRemove and ( ) to
     * indicate the bounds of ranges in the range map.
     */
    Map.Entry<Cut<K>, RangeMapEntry<K, V>> mapEntryBelowToTruncate =
        entriesByLowerBound.lowerEntry(rangeToRemove.lowerBound);
    if (mapEntryBelowToTruncate != null) {
      // we know ( [
      RangeMapEntry<K, V> rangeMapEntry = mapEntryBelowToTruncate.getValue();
      if (rangeMapEntry.getUpperBound().compareTo(rangeToRemove.lowerBound) > 0) {
        // we know ( [ )
        if (rangeMapEntry.getUpperBound().compareTo(rangeToRemove.upperBound) > 0) {
          // we know ( [ ] ), so insert the range ] ) back into the map --
          // it's being split apart
          putRangeMapEntry(rangeToRemove.upperBound, rangeMapEntry.getUpperBound(),
              mapEntryBelowToTruncate.getValue().getValue());
        }
        // overwrite mapEntryToTruncateBelow with a truncated range
        putRangeMapEntry(rangeMapEntry.getLowerBound(), rangeToRemove.lowerBound,
            mapEntryBelowToTruncate.getValue().getValue());
      }
    }

    Map.Entry<Cut<K>, RangeMapEntry<K, V>> mapEntryAboveToTruncate =
        entriesByLowerBound.lowerEntry(rangeToRemove.upperBound);
    if (mapEntryAboveToTruncate != null) {
      // we know ( ]
      RangeMapEntry<K, V> rangeMapEntry = mapEntryAboveToTruncate.getValue();
      if (rangeMapEntry.getUpperBound().compareTo(rangeToRemove.upperBound) > 0) {
        // we know ( ] ), and since we dealt with truncating below already,
        // we know [ ( ] )
        putRangeMapEntry(rangeToRemove.upperBound, rangeMapEntry.getUpperBound(),
            mapEntryAboveToTruncate.getValue().getValue());
        entriesByLowerBound.remove(rangeToRemove.lowerBound);
      }
    }
    entriesByLowerBound.subMap(rangeToRemove.lowerBound, rangeToRemove.upperBound).clear();
  }

  @Override
  public Map<Range<K>, V> asMapOfRanges() {
    return new AsMapOfRanges();
  }

  private final class AsMapOfRanges extends AbstractMap<Range<K>, V> {

    @Override
    public boolean containsKey(@Nullable Object key) {
      return get(key) != null;
    }

    @Override
    public V get(@Nullable Object key) {
      if (key instanceof Range) {
        Range<?> range = (Range<?>) key;
        RangeMapEntry<K, V> rangeMapEntry = entriesByLowerBound.get(range.lowerBound);
        if (rangeMapEntry.getKey().equals(range)) {
          return rangeMapEntry.getValue();
        }
      }
      return null;
    }

    @Override
    public Set<Entry<Range<K>, V>> entrySet() {
      return new AbstractSet<Entry<Range<K>, V>>() {

        @SuppressWarnings("unchecked") // it's safe to upcast iterators
        @Override
        public Iterator<Entry<Range<K>, V>> iterator() {
          return (Iterator) Iterators.
              unmodifiableIterator(entriesByLowerBound.values().iterator());
        }

        @Override
        public int size() {
          return entriesByLowerBound.size();
        }
      };
    }
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
  public int hashCode() {
    return asMapOfRanges().hashCode();
  }

  @Override
  public String toString() {
    return entriesByLowerBound.values().toString();
  }
}
