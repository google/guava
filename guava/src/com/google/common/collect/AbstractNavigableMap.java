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

import static com.google.common.collect.Iterators.pollNext;
import static com.google.common.collect.Maps.keyOrNull;

import com.google.common.annotations.GwtIncompatible;
import com.google.common.collect.Maps.IteratorBasedAbstractMap;
import java.util.Iterator;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.SortedMap;
import org.jspecify.annotations.Nullable;

/**
 * Skeletal implementation of {@link NavigableMap}.
 *
 * @author Louis Wasserman
 */
@GwtIncompatible
abstract class AbstractNavigableMap<K extends @Nullable Object, V extends @Nullable Object>
    extends IteratorBasedAbstractMap<K, V> implements NavigableMap<K, V> {

  @Override
  public abstract @Nullable V get(@Nullable Object key);

  /*
   * firstEntry+lastEntry+pollFirstEntry+pollLastEntry are all required to return snapshots of the
   * Entry. If an AbstractNavigableMap subclass may return live entries from its entryIterator or
   * descendingEntryIterator, then it must override those *Entry methods to return snapshot entries
   * because the implementations in this class would pass through the live views.
   *
   * In particular, the subclass may need to override the poll* methods to snapshot the entry
   * *before* removing it from the map: Snapshotting the result of super.pollFirstEntry() is not
   * enough if the Entry implementation came from the entrySet of a map that is implemented like
   * TreeMap. See Maps.snapshotFirst.
   */

  @Override
  public @Nullable Entry<K, V> firstEntry() {
    return Iterators.<@Nullable Entry<K, V>>getNext(entryIterator(), null);
  }

  @Override
  public @Nullable Entry<K, V> lastEntry() {
    return Iterators.<@Nullable Entry<K, V>>getNext(descendingEntryIterator(), null);
  }

  @Override
  public @Nullable Entry<K, V> pollFirstEntry() {
    return pollNext(entryIterator());
  }

  @Override
  public @Nullable Entry<K, V> pollLastEntry() {
    return pollNext(descendingEntryIterator());
  }

  @Override
  @ParametricNullness
  public final K firstKey() {
    Entry<K, V> entry = firstEntry();
    if (entry == null) {
      throw new NoSuchElementException();
    } else {
      return entry.getKey();
    }
  }

  @Override
  @ParametricNullness
  public final K lastKey() {
    Entry<K, V> entry = lastEntry();
    if (entry == null) {
      throw new NoSuchElementException();
    } else {
      return entry.getKey();
    }
  }

  @Override
  public final @Nullable Entry<K, V> lowerEntry(@ParametricNullness K key) {
    return headMap(key, false).lastEntry();
  }

  @Override
  public final @Nullable Entry<K, V> floorEntry(@ParametricNullness K key) {
    return headMap(key, true).lastEntry();
  }

  @Override
  public final @Nullable Entry<K, V> ceilingEntry(@ParametricNullness K key) {
    return tailMap(key, true).firstEntry();
  }

  @Override
  public final @Nullable Entry<K, V> higherEntry(@ParametricNullness K key) {
    return tailMap(key, false).firstEntry();
  }

  @Override
  public final @Nullable K lowerKey(@ParametricNullness K key) {
    return keyOrNull(lowerEntry(key));
  }

  @Override
  public final @Nullable K floorKey(@ParametricNullness K key) {
    return keyOrNull(floorEntry(key));
  }

  @Override
  public final @Nullable K ceilingKey(@ParametricNullness K key) {
    return keyOrNull(ceilingEntry(key));
  }

  @Override
  public final @Nullable K higherKey(@ParametricNullness K key) {
    return keyOrNull(higherEntry(key));
  }

  abstract Iterator<Entry<K, V>> descendingEntryIterator();

  @Override
  public final SortedMap<K, V> subMap(@ParametricNullness K fromKey, @ParametricNullness K toKey) {
    return subMap(fromKey, true, toKey, false);
  }

  @Override
  public final SortedMap<K, V> headMap(@ParametricNullness K toKey) {
    return headMap(toKey, false);
  }

  @Override
  public final SortedMap<K, V> tailMap(@ParametricNullness K fromKey) {
    return tailMap(fromKey, true);
  }

  @Override
  public NavigableSet<K> navigableKeySet() {
    return new Maps.NavigableKeySet<>(this);
  }

  @Override
  public final Set<K> keySet() {
    return navigableKeySet();
  }

  @Override
  public final NavigableSet<K> descendingKeySet() {
    return descendingMap().navigableKeySet();
  }

  @Override
  public NavigableMap<K, V> descendingMap() {
    return new DescendingMap();
  }

  private final class DescendingMap extends Maps.DescendingMap<K, V> {
    @Override
    NavigableMap<K, V> forward() {
      return AbstractNavigableMap.this;
    }

    @Override
    Iterator<Entry<K, V>> internalEntryIterator() {
      return descendingEntryIterator();
    }
  }
}
