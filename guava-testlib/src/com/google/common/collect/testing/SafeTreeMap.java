/*
 * Copyright (C) 2010 The Guava Authors
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

package com.google.common.collect.testing;

import com.google.common.annotations.GwtIncompatible;
import java.io.Serializable;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * A wrapper around {@code TreeMap} that aggressively checks to see if keys are mutually comparable.
 * This implementation passes the navigable map test suites.
 *
 * @author Louis Wasserman
 */
@GwtIncompatible
public final class SafeTreeMap<K, V> implements Serializable, NavigableMap<K, V> {
  @SuppressWarnings("unchecked")
  private static final Comparator<Object> NATURAL_ORDER =
      new Comparator<Object>() {
        @Override
        public int compare(Object o1, Object o2) {
          return ((Comparable<Object>) o1).compareTo(o2);
        }
      };

  private final NavigableMap<K, V> delegate;

  public SafeTreeMap() {
    this(new TreeMap<K, V>());
  }

  public SafeTreeMap(Comparator<? super K> comparator) {
    this(new TreeMap<K, V>(comparator));
  }

  public SafeTreeMap(Map<? extends K, ? extends V> map) {
    this(new TreeMap<K, V>(map));
  }

  public SafeTreeMap(SortedMap<K, ? extends V> map) {
    this(new TreeMap<K, V>(map));
  }

  private SafeTreeMap(NavigableMap<K, V> delegate) {
    this.delegate = delegate;
    if (delegate == null) {
      throw new NullPointerException();
    }
    for (K k : keySet()) {
      checkValid(k);
    }
  }

  @Override
  public Entry<K, V> ceilingEntry(K key) {
    return delegate.ceilingEntry(checkValid(key));
  }

  @Override
  public K ceilingKey(K key) {
    return delegate.ceilingKey(checkValid(key));
  }

  @Override
  public void clear() {
    delegate.clear();
  }

  @SuppressWarnings("unchecked")
  @Override
  public Comparator<? super K> comparator() {
    Comparator<? super K> comparator = delegate.comparator();
    if (comparator == null) {
      comparator = (Comparator<? super K>) NATURAL_ORDER;
    }
    return comparator;
  }

  @Override
  public boolean containsKey(Object key) {
    try {
      return delegate.containsKey(checkValid(key));
    } catch (NullPointerException | ClassCastException e) {
      return false;
    }
  }

  @Override
  public boolean containsValue(Object value) {
    return delegate.containsValue(value);
  }

  @Override
  public NavigableSet<K> descendingKeySet() {
    return delegate.descendingKeySet();
  }

  @Override
  public NavigableMap<K, V> descendingMap() {
    return new SafeTreeMap<>(delegate.descendingMap());
  }

  @Override
  public Set<Entry<K, V>> entrySet() {
    return new AbstractSet<Entry<K, V>>() {
      private Set<Entry<K, V>> delegate() {
        return delegate.entrySet();
      }

      @Override
      public boolean contains(Object object) {
        try {
          return delegate().contains(object);
        } catch (NullPointerException | ClassCastException e) {
          return false;
        }
      }

      @Override
      public Iterator<Entry<K, V>> iterator() {
        return delegate().iterator();
      }

      @Override
      public int size() {
        return delegate().size();
      }

      @Override
      public boolean remove(Object o) {
        return delegate().remove(o);
      }

      @Override
      public void clear() {
        delegate().clear();
      }
    };
  }

  @Override
  public Entry<K, V> firstEntry() {
    return delegate.firstEntry();
  }

  @Override
  public K firstKey() {
    return delegate.firstKey();
  }

  @Override
  public Entry<K, V> floorEntry(K key) {
    return delegate.floorEntry(checkValid(key));
  }

  @Override
  public K floorKey(K key) {
    return delegate.floorKey(checkValid(key));
  }

  @Override
  public V get(Object key) {
    return delegate.get(checkValid(key));
  }

  @Override
  public SortedMap<K, V> headMap(K toKey) {
    return headMap(toKey, false);
  }

  @Override
  public NavigableMap<K, V> headMap(K toKey, boolean inclusive) {
    return new SafeTreeMap<>(delegate.headMap(checkValid(toKey), inclusive));
  }

  @Override
  public Entry<K, V> higherEntry(K key) {
    return delegate.higherEntry(checkValid(key));
  }

  @Override
  public K higherKey(K key) {
    return delegate.higherKey(checkValid(key));
  }

  @Override
  public boolean isEmpty() {
    return delegate.isEmpty();
  }

  @Override
  public NavigableSet<K> keySet() {
    return navigableKeySet();
  }

  @Override
  public Entry<K, V> lastEntry() {
    return delegate.lastEntry();
  }

  @Override
  public K lastKey() {
    return delegate.lastKey();
  }

  @Override
  public Entry<K, V> lowerEntry(K key) {
    return delegate.lowerEntry(checkValid(key));
  }

  @Override
  public K lowerKey(K key) {
    return delegate.lowerKey(checkValid(key));
  }

  @Override
  public NavigableSet<K> navigableKeySet() {
    return delegate.navigableKeySet();
  }

  @Override
  public Entry<K, V> pollFirstEntry() {
    return delegate.pollFirstEntry();
  }

  @Override
  public Entry<K, V> pollLastEntry() {
    return delegate.pollLastEntry();
  }

  @Override
  public V put(K key, V value) {
    return delegate.put(checkValid(key), value);
  }

  @Override
  public void putAll(Map<? extends K, ? extends V> map) {
    for (K key : map.keySet()) {
      checkValid(key);
    }
    delegate.putAll(map);
  }

  @Override
  public V remove(Object key) {
    return delegate.remove(checkValid(key));
  }

  @Override
  public int size() {
    return delegate.size();
  }

  @Override
  public NavigableMap<K, V> subMap(K fromKey, boolean fromInclusive, K toKey, boolean toInclusive) {
    return new SafeTreeMap<>(
        delegate.subMap(checkValid(fromKey), fromInclusive, checkValid(toKey), toInclusive));
  }

  @Override
  public SortedMap<K, V> subMap(K fromKey, K toKey) {
    return subMap(fromKey, true, toKey, false);
  }

  @Override
  public SortedMap<K, V> tailMap(K fromKey) {
    return tailMap(fromKey, true);
  }

  @Override
  public NavigableMap<K, V> tailMap(K fromKey, boolean inclusive) {
    return new SafeTreeMap<>(delegate.tailMap(checkValid(fromKey), inclusive));
  }

  @Override
  public Collection<V> values() {
    return delegate.values();
  }

  private <T> T checkValid(T t) {
    // a ClassCastException is what's supposed to happen!
    @SuppressWarnings("unchecked")
    K k = (K) t;
    comparator().compare(k, k);
    return t;
  }

  @Override
  public boolean equals(Object obj) {
    return delegate.equals(obj);
  }

  @Override
  public int hashCode() {
    return delegate.hashCode();
  }

  @Override
  public String toString() {
    return delegate.toString();
  }

  private static final long serialVersionUID = 0L;
}
