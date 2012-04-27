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
import com.google.common.base.Function;

import java.io.Serializable;
import java.util.AbstractMap;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.TreeMap;

import javax.annotation.Nullable;

/**
 * A mapping from keys to values that efficiently supports mapping entire ranges at once. This
 * implementation does not support null values.
 *
 * @author Louis Wasserman
 */
@GwtIncompatible("NavigableMap") final class RangeMap<K extends Comparable, V>
    implements Function<K, V>, Serializable {
  private final NavigableMap<Cut<K>, RangeValue<K, V>> map;

  /**
   * Creates a new, empty {@code RangeMap}.
   */
  public static <K extends Comparable, V> RangeMap<K, V> create() {
    return new RangeMap<K, V>(new TreeMap<Cut<K>, RangeValue<K, V>>());
  }

  private RangeMap(NavigableMap<Cut<K>, RangeValue<K, V>> map) {
    this.map = map;
  }

  /**
   * Equivalent to {@link #get(Comparable) get(K)}, provided only to satisfy the {@link Function}
   * interface. When using a reference of type {@code RangeMap}, always invoke
   * {@link #get(Comparable) get(K)} directly instead.
   */
  @Override
  public V apply(K input) {
    return get(input);
  }

  /**
   * Returns the value associated with {@code key}, or {@code null} if there is no such value.
   */
  @Nullable
  public V get(K key) {
    Entry<Cut<K>, RangeValue<K, V>> lowerEntry = map.lowerEntry(Cut.aboveValue(key));
    if (lowerEntry != null && lowerEntry.getValue().getKey().contains(key)) {
      return lowerEntry.getValue().getValue();
    }
    return null;
  }

  /**
   * Associates {@code value} with every key {@linkplain Range#contains contained} in {@code
   * keyRange}.
   *
   * <p>This method takes amortized <i>O(log n)</i> time.
   */
  public void put(Range<K> keyRange, V value) {
    checkNotNull(keyRange);
    checkNotNull(value);
    if (keyRange.isEmpty()) {
      return;
    }
    clear(keyRange);
    putRange(new RangeValue<K, V>(keyRange, value));
  }

  /**
   * Puts all the associations from the specified {@code RangeMap} into this {@code RangeMap}.
   */
  public void putAll(RangeMap<K, V> rangeMap) {
    checkNotNull(rangeMap);
    for (RangeValue<K, V> rangeValue : rangeMap.map.values()) {
      put(rangeValue.getKey(), rangeValue.getValue());
    }
  }

  /**
   * Clears all associations from this {@code RangeMap}.
   */
  public void clear() {
    map.clear();
  }

  /**
   * Removes all associations to keys {@linkplain Range#contains contained} in {@code
   * rangeToClear}.
   */
  public void clear(Range<K> rangeToClear) {
    checkNotNull(rangeToClear);
    if (rangeToClear.isEmpty()) {
      return;
    }

    Entry<Cut<K>, RangeValue<K, V>> lowerThanLB = map.lowerEntry(rangeToClear.lowerBound);
    // We will use { } to denote the ends of rangeToClear, and < > to denote the ends of
    // other ranges currently in the map.  For example, < { > indicates that we know that
    // rangeToClear.lowerBound is between the bounds of some range already in the map.

    if (lowerThanLB != null) {
      RangeValue<K, V> lowerRangeValue = lowerThanLB.getValue();
      Cut<K> upperCut = lowerRangeValue.getUpperBound();
      if (upperCut.compareTo(rangeToClear.lowerBound) >= 0) { // < { >
        RangeValue<K, V> replacement = lowerRangeValue.withUpperBound(rangeToClear.lowerBound);
        if (replacement == null) {
          removeRange(lowerRangeValue);
        } else {
          putRange(replacement); // overwrites old range
        }
        if (upperCut.compareTo(rangeToClear.upperBound) >= 0) { // < { } >
          putRange(lowerRangeValue.withLowerBound(rangeToClear.upperBound));
          return; // we must be done
        }
      }
    }

    Entry<Cut<K>, RangeValue<K, V>> lowerThanUB = map.lowerEntry(rangeToClear.upperBound);
    if (lowerThanUB != null) {
      RangeValue<K, V> lowerRangeValue = lowerThanUB.getValue();
      Cut<K> upperCut = lowerRangeValue.getUpperBound();
      if (upperCut.compareTo(rangeToClear.upperBound) >= 0) { // < } >
        // we can't have < { } >, we already dealt with that
        removeRange(lowerRangeValue);
        putRange(lowerRangeValue.withLowerBound(rangeToClear.upperBound));
      }
    }

    // everything left with { < } is a { < > }, so we clear it indiscriminately
    map.subMap(rangeToClear.lowerBound, rangeToClear.upperBound).clear();
  }

  private void removeRange(RangeValue<K, V> rangeValue) {
    RangeValue<K, V> removed = map.remove(rangeValue.getLowerBound());
    assert removed == rangeValue;
  }

  private void putRange(@Nullable RangeValue<K, V> rangeValue) {
    if (rangeValue != null && !rangeValue.getKey().isEmpty()) {
      map.put(rangeValue.getLowerBound(), rangeValue);
    }
  }

  private static final class RangeValue<K extends Comparable, V> extends AbstractMap.SimpleEntry<
      Range<K>, V> {

    RangeValue(Range<K> key, V value) {
      super(checkNotNull(key), checkNotNull(value));
      assert !key.isEmpty();
    }

    Cut<K> getLowerBound() {
      return getKey().lowerBound;
    }

    Cut<K> getUpperBound() {
      return getKey().upperBound;
    }

    @Nullable
    RangeValue<K, V> withLowerBound(Cut<K> newLowerBound) {
      Range<K> newRange = new Range<K>(newLowerBound, getUpperBound());
      return newRange.isEmpty() ? null : new RangeValue<K, V>(newRange, getValue());
    }

    @Nullable
    RangeValue<K, V> withUpperBound(Cut<K> newUpperBound) {
      Range<K> newRange = new Range<K>(getLowerBound(), newUpperBound);
      return newRange.isEmpty() ? null : new RangeValue<K, V>(newRange, getValue());
    }

    private static final long serialVersionUID = 0L;
  }

  /**
   * Compares the specified object with this {@code RangeMap} for equality. It is guaranteed that:
   * <ul>
   * <li>The relation defined by this method is reflexive, symmetric, and transitive, as required
   * by the contract of {@link Object#equals(Object)}.
   * <li>Two empty range maps are always equal.
   * <li>If two range maps are equal, and the same operation is performed on each, the resulting
   * range maps are also equal.
   * <li>If {@code rangeMap1.equals(rangeMap2)}, it is guaranteed that {@code rangeMap1.get(k)}
   * is equal to {@code rangeMap2.get(k)}.
   * </ul>
   */
  @Override
  public boolean equals(@Nullable Object o) {
    return o instanceof RangeMap && map.equals(((RangeMap) o).map);
  }

  @Override
  public int hashCode() {
    return map.hashCode();
  }

  @Override
  public String toString() {
    return map.toString();
  }

  private static final long serialVersionUID = 0L;
}
