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

import com.google.common.annotations.Beta;

import java.util.Map;

import javax.annotation.Nullable;

/**
 * A mapping from disjoint nonempty ranges to non-null values. Queries look up the value
 * associated with the range (if any) that contains a specified key.
 *
 * <p>In contrast to {@link RangeSet}, no "coalescing" is done of {@linkplain
 * Range#isConnected(Range) connected} ranges, even if they are mapped to the same value.
 *
 * @author Louis Wasserman
 * @since 14.0
 */
@Beta
public interface RangeMap<K extends Comparable, V> {
  /**
   * Returns the value associated with the specified key, or {@code null} if there is no
   * such value.
   *
   * <p>Specifically, if any range in this range map contains the specified key, the value
   * associated with that range is returned.
   */
  @Nullable
  V get(K key);

  /**
   * Returns the range containing this key and its associated value, if such a range is present
   * in the range map, or {@code null} otherwise.
   */
  @Nullable
  Map.Entry<Range<K>, V> getEntry(K key);

  /**
   * Returns the minimal range {@linkplain Range#encloses(Range) enclosing} the ranges
   * in this {@code RangeMap}.
   *
   * @throws NoSuchElementException if this range map is empty
   */
  Range<K> span();

  /**
   * Maps a range to a specified value (optional operation).
   *
   * <p>Specifically, after a call to {@code put(range, value)}, if
   * {@link Range#contains(Comparable) range.contains(k)}, then {@link #get(Comparable) get(k)}
   * will return {@code value}.
   *
   * <p>If {@code range} {@linkplain Range#isEmpty() is empty}, then this is a no-op.
   */
  void put(Range<K> range, V value);

  /**
   * Puts all the associations from {@code rangeMap} into this range map (optional operation).
   */
  void putAll(RangeMap<K, V> rangeMap);

  /**
   * Removes all associations from this range map (optional operation).
   */
  void clear();

  /**
   * Removes all associations from this range map in the specified range (optional operation).
   *
   * <p>If {@code !range.contains(k)}, {@link #get(Comparable) get(k)} will return the same result
   * before and after a call to {@code remove(range)}.  If {@code range.contains(k)}, then
   * after a call to {@code remove(range)}, {@code get(k)} will return {@code null}.
   */
  void remove(Range<K> range);

  /**
   * Returns a view of this range map as an unmodifiable {@code Map<Range<K>, V>}.
   * Modifications to this range map are guaranteed to read through to the returned {@code Map}.
   *
   * <p>The returned {@code Map} iterates over entries in ascending order of the bounds of the
   * {@code Range} entries.
   *
   * <p>It is guaranteed that no empty ranges will be in the returned {@code Map}.
   */
  Map<Range<K>, V> asMapOfRanges();
  
  /**
   * Returns a view of the part of this range map that intersects with {@code range}.
   * 
   * <p>For example, if {@code rangeMap} had the entries 
   * {@code [1, 5] => "foo", (6, 8) => "bar", (10, \u2025) => "baz"}
   * then {@code rangeMap.subRangeMap(Range.open(3, 12))} would return a range map
   * with the entries {@code (3, 5) => "foo", (6, 8) => "bar", (10, 12) => "baz"}.
   * 
   * <p>The returned range map supports all optional operations that this range map supports,
   * except for {@code asMapOfRanges().iterator().remove()}.
   * 
   * <p>The returned range map will throw an {@link IllegalArgumentException} on an attempt to 
   * insert a range not {@linkplain Range#encloses(Range) enclosed} by {@code range}. 
   */
  RangeMap<K, V> subRangeMap(Range<K> range);

  /**
   * Returns {@code true} if {@code obj} is another {@code RangeMap} that has an equivalent
   * {@link #asMapOfRanges()}.
   */
  @Override
  boolean equals(@Nullable Object o);

  /**
   * Returns {@code asMapOfRanges().hashCode()}.
   */
  @Override
  int hashCode();

  /**
   * Returns a readable string representation of this range map.
   */
  @Override
  String toString();
}
