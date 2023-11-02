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

import com.google.common.annotations.GwtIncompatible;
import com.google.common.collect.Maps.IteratorBasedAbstractMap;
import java.util.Iterator;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.SortedMap;
import javax.annotation.CheckForNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Skeletal implementation of {@link NavigableMap}.
 *
 * @author Louis Wasserman
 */
@GwtIncompatible
@ElementTypesAreNonnullByDefault
abstract class AbstractNavigableMap<K extends @Nullable Object, V extends @Nullable Object>
    extends IteratorBasedAbstractMap<K, V> implements NavigableMap<K, V> {

  @Override
  @CheckForNull
  public abstract V get(@CheckForNull Object key);

  @Override
  @CheckForNull
  public Entry<K, V> firstEntry() {
    return Iterators.<@Nullable Entry<K, V>>getNext(entryIterator(), null);
  }

  @Override
  @CheckForNull
  public Entry<K, V> lastEntry() {
    return Iterators.<@Nullable Entry<K, V>>getNext(descendingEntryIterator(), null);
  }

  @Override
  @CheckForNull
  public Entry<K, V> pollFirstEntry() {
    return Iterators.pollNext(entryIterator());
  }

  @Override
  @CheckForNull
  public Entry<K, V> pollLastEntry() {
    return Iterators.pollNext(descendingEntryIterator());
  }

  @Override
  @ParametricNullness
  public K firstKey() {
    Entry<K, V> entry = firstEntry();
    if (entry == null) {
      throw new NoSuchElementException();
    } else {
      return entry.getKey();
    }
  }

  @Override
  @ParametricNullness
  public K lastKey() {
    Entry<K, V> entry = lastEntry();
    if (entry == null) {
      throw new NoSuchElementException();
    } else {
      return entry.getKey();
    }
  }

  @Override
  @CheckForNull
  public Entry<K, V> lowerEntry(@ParametricNullness K key) {
    return headMap(key, false).lastEntry();
  }

  @Override
  @CheckForNull
  public Entry<K, V> floorEntry(@ParametricNullness K key) {
    return headMap(key, true).lastEntry();
  }

  @Override
  @CheckForNull
  public Entry<K, V> ceilingEntry(@ParametricNullness K key) {
    return tailMap(key, true).firstEntry();
  }

  @Override
  @CheckForNull
  public Entry<K, V> higherEntry(@ParametricNullness K key) {
    return tailMap(key, false).firstEntry();
  }

  @Override
  @CheckForNull
  public K lowerKey(@ParametricNullness K key) {
    return Maps.keyOrNull(lowerEntry(key));
  }

  @Override
  @CheckForNull
  public K floorKey(@ParametricNullness K key) {
    return Maps.keyOrNull(floorEntry(key));
  }

  @Override
  @CheckForNull
  public K ceilingKey(@ParametricNullness K key) {
    return Maps.keyOrNull(ceilingEntry(key));
  }

  @Override
  @CheckForNull
  public K higherKey(@ParametricNullness K key) {
    return Maps.keyOrNull(higherEntry(key));
  }

  abstract Iterator<Entry<K, V>> descendingEntryIterator();

  @Override
  public SortedMap<K, V> subMap(@ParametricNullness K fromKey, @ParametricNullness K toKey) {
    return subMap(fromKey, true, toKey, false);
  }

  @Override
  public SortedMap<K, V> headMap(@ParametricNullness K toKey) {
    return headMap(toKey, false);
  }

  @Override
  public SortedMap<K, V> tailMap(@ParametricNullness K fromKey) {
    return tailMap(fromKey, true);
  }

  @Override
  public NavigableSet<K> navigableKeySet() {
    return new Maps.NavigableKeySet<>(this);
  }

  @Override
  public Set<K> keySet() {
    return navigableKeySet();
  }

  @Override
  public NavigableSet<K> descendingKeySet() {
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
    Iterator<Entry<K, V>> entryIterator() {
      return descendingEntryIterator();
    }
  }
}
