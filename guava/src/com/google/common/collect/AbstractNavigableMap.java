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

import java.util.AbstractMap;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.SortedMap;

import javax.annotation.Nullable;

/**
 * Skeletal implementation of {@link NavigableMap}.
 * 
 * @author Louis Wasserman
 */
abstract class AbstractNavigableMap<K, V> extends AbstractMap<K, V> implements NavigableMap<K, V> {

  @Override
  @Nullable
  public abstract V get(@Nullable Object key);
  
  @Override
  @Nullable
  public Entry<K, V> firstEntry() {
    return Iterators.getNext(entryIterator(), null);
  }

  @Override
  @Nullable
  public Entry<K, V> lastEntry() {
    return Iterators.getNext(descendingEntryIterator(), null);
  }

  @Override
  @Nullable
  public Entry<K, V> pollFirstEntry() {
    return Iterators.pollNext(entryIterator());
  }

  @Override
  @Nullable
  public Entry<K, V> pollLastEntry() {
    return Iterators.pollNext(descendingEntryIterator());
  }

  @Override
  public K firstKey() {
    Entry<K, V> entry = firstEntry();
    if (entry == null) {
      throw new NoSuchElementException();
    } else {
      return entry.getKey();
    }
  }

  @Override
  public K lastKey() {
    Entry<K, V> entry = lastEntry();
    if (entry == null) {
      throw new NoSuchElementException();
    } else {
      return entry.getKey();
    }
  }

  @Override
  @Nullable
  public Entry<K, V> lowerEntry(K key) {
    return headMap(key, false).lastEntry();
  }

  @Override
  @Nullable
  public Entry<K, V> floorEntry(K key) {
    return headMap(key, true).lastEntry();
  }

  @Override
  @Nullable
  public Entry<K, V> ceilingEntry(K key) {
    return tailMap(key, true).firstEntry();
  }

  @Override
  @Nullable
  public Entry<K, V> higherEntry(K key) {
    return tailMap(key, false).firstEntry();
  }

  @Override
  public K lowerKey(K key) {
    return Maps.keyOrNull(lowerEntry(key));
  }

  @Override
  public K floorKey(K key) {
    return Maps.keyOrNull(floorEntry(key));
  }

  @Override
  public K ceilingKey(K key) {
    return Maps.keyOrNull(ceilingEntry(key));
  }

  @Override
  public K higherKey(K key) {
    return Maps.keyOrNull(higherEntry(key));
  }

  abstract Iterator<Entry<K, V>> entryIterator();

  abstract Iterator<Entry<K, V>> descendingEntryIterator();

  @Override
  public SortedMap<K, V> subMap(K fromKey, K toKey) {
    return subMap(fromKey, true, toKey, false);
  }

  @Override
  public SortedMap<K, V> headMap(K toKey) {
    return headMap(toKey, false);
  }

  @Override
  public SortedMap<K, V> tailMap(K fromKey) {
    return tailMap(fromKey, true);
  }

  @Override
  public NavigableSet<K> navigableKeySet() {
    return new Maps.NavigableKeySet<K, V>(this);
  }

  @Override
  public Set<K> keySet() {
    return navigableKeySet();
  }

  @Override
  public abstract int size();

  @Override
  public Set<Entry<K, V>> entrySet() {
    return new Maps.EntrySet<K, V>() {
      @Override
      Map<K, V> map() {
        return AbstractNavigableMap.this;
      }

      @Override
      public Iterator<Entry<K, V>> iterator() {
        return entryIterator();
      }
    };
  }

  @Override
  public NavigableSet<K> descendingKeySet() {
    return descendingMap().navigableKeySet();
  }

  @Override
  public NavigableMap<K, V> descendingMap() {
    return new DescendingMap();
  }
  
  private final class DescendingMap extends ForwardingMap<K, V> implements NavigableMap<K, V> {
    @Override
    protected NavigableMap<K, V> delegate() {
      return AbstractNavigableMap.this;
    }
    
    @Override
    public Comparator<? super K> comparator() {
      return Ordering.from(delegate().comparator()).<K>reverse();
    }

    @Override
    public K firstKey() {
      return delegate().lastKey();
    }

    @Override
    public K lastKey() {
      return delegate().firstKey();
    }

    @Override
    public Entry<K, V> lowerEntry(K key) {
      return delegate().higherEntry(key);
    }

    @Override
    public K lowerKey(K key) {
      return delegate().higherKey(key);
    }

    @Override
    public Entry<K, V> floorEntry(K key) {
      return delegate().ceilingEntry(key);
    }

    @Override
    public K floorKey(K key) {
      return delegate().ceilingKey(key);
    }

    @Override
    public Entry<K, V> ceilingEntry(K key) {
      return delegate().floorEntry(key);
    }

    @Override
    public K ceilingKey(K key) {
      return delegate().floorKey(key);
    }

    @Override
    public Entry<K, V> higherEntry(K key) {
      return delegate().lowerEntry(key);
    }

    @Override
    public K higherKey(K key) {
      return delegate().lowerKey(key);
    }

    @Override
    public Entry<K, V> firstEntry() {
      return delegate().lastEntry();
    }

    @Override
    public Entry<K, V> lastEntry() {
      return delegate().firstEntry();
    }

    @Override
    public Entry<K, V> pollFirstEntry() {
      return delegate().pollLastEntry();
    }

    @Override
    public Entry<K, V> pollLastEntry() {
      return delegate().pollFirstEntry();
    }

    @Override
    public NavigableMap<K, V> descendingMap() {
      return delegate();
    }

    @Override
    public NavigableSet<K> navigableKeySet() {
      return new Maps.NavigableKeySet<K, V>(this);
    }

    @Override
    public NavigableSet<K> descendingKeySet() {
      return delegate().navigableKeySet();
    }

    @Override
    public NavigableMap<K, V> subMap(
        K fromKey, boolean fromInclusive, K toKey, boolean toInclusive) {
      return delegate().subMap(toKey, toInclusive, fromKey, fromInclusive).descendingMap();
    }

    @Override
    public NavigableMap<K, V> headMap(K toKey, boolean inclusive) {
      return delegate().tailMap(toKey, inclusive).descendingMap();
    }

    @Override
    public NavigableMap<K, V> tailMap(K fromKey, boolean inclusive) {
      return delegate().headMap(fromKey, inclusive).descendingMap();
    }

    @Override
    public SortedMap<K, V> subMap(K fromKey, K toKey) {
      return subMap(fromKey, true, toKey, false);
    }

    @Override
    public SortedMap<K, V> headMap(K toKey) {
      return headMap(toKey, false);
    }

    @Override
    public SortedMap<K, V> tailMap(K fromKey) {
      return tailMap(fromKey, true);
    }

    @Override
    public Set<K> keySet() {
      return navigableKeySet();
    }

    @Override
    public Collection<V> values() {
      return new StandardValues();
    }

    @Override
    public Set<Entry<K, V>> entrySet() {
      return new Maps.EntrySet<K, V>() {
        @Override
        Map<K, V> map() {
          return DescendingMap.this;
        }

        @Override
        public Iterator<Entry<K, V>> iterator() {
          return AbstractNavigableMap.this.descendingEntryIterator();
        }
      };
    }

    @Override
    public String toString() {
      return standardToString();
    }
  }

}
