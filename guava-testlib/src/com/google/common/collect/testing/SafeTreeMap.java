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

import java.io.Serializable;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * A wrapper around {@code TreeMap} that aggressively checks to see if keys are
 * mutually comparable. This implementation passes the navigable map test
 * suites.
 *
 * @author Louis Wasserman
 */
public final class SafeTreeMap<K, V> implements Serializable, SortedMap<K, V> {
  @SuppressWarnings("unchecked")
  private static final Comparator<Object> NATURAL_ORDER = new Comparator<Object>() {
    @Override public int compare(Object o1, Object o2) {
      return ((Comparable<Object>) o1).compareTo(o2);
    }
  };
  private final SortedMap<K, V> delegate;

  public SafeTreeMap() {
    this(new TreeMap<K, V>());
  }

  public SafeTreeMap(Comparator<? super K> comparator) {
    this(new TreeMap<K, V>(comparator));
  }

  public SafeTreeMap(Map<? extends K, ? extends V> map) {
    this(new TreeMap<K, V>(map));
  }

  private SafeTreeMap(SortedMap<K, V> delegate) {
    this.delegate = delegate;
    if (delegate == null) {
      throw new NullPointerException();
    }
    for (K k : keySet()) {
      checkValid(k);
    }
  }

  @Override public void clear() {
    delegate.clear();
  }

  @SuppressWarnings("unchecked")
  @Override public Comparator<? super K> comparator() {
    Comparator<? super K> comparator = delegate.comparator();
    if (comparator == null) {
      comparator = (Comparator<? super K>) NATURAL_ORDER;
    }
    return comparator;
  }

  @Override public boolean containsKey(Object key) {
    try {
      return delegate.containsKey(checkValid(key));
    } catch (NullPointerException e) {
      return false;
    } catch (ClassCastException e) {
      return false;
    }
  }

  @Override public boolean containsValue(Object value) {
    return delegate.containsValue(value);
  }

  @Override public Set<Entry<K, V>> entrySet() {
    return new AbstractSet<Entry<K, V>>() {
      private Set<Entry<K, V>> delegate() {
        return delegate.entrySet();
      }

      @Override
      public boolean contains(Object object) {
        try {
          return delegate().contains(object);
        } catch (NullPointerException e) {
          return false;
        } catch (ClassCastException e) {
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

  @Override public K firstKey() {
    return delegate.firstKey();
  }

  @Override public V get(Object key) {
    return delegate.get(checkValid(key));
  }

  @Override public SortedMap<K, V> headMap(K toKey) {
    return new SafeTreeMap<K, V>(delegate.headMap(checkValid(toKey)));
  }

  @Override public boolean isEmpty() {
    return delegate.isEmpty();
  }

  @Override public Set<K> keySet() {
    return delegate.keySet();
  }

  @Override public K lastKey() {
    return delegate.lastKey();
  }

  @Override public V put(K key, V value) {
    return delegate.put(checkValid(key), value);
  }

  @Override public void putAll(Map<? extends K, ? extends V> map) {
    for (K key : map.keySet()) {
      checkValid(key);
    }
    delegate.putAll(map);
  }

  @Override public V remove(Object key) {
    return delegate.remove(checkValid(key));
  }

  @Override public int size() {
    return delegate.size();
  }

  @Override public SortedMap<K, V> subMap(K fromKey, K toKey) {
    return new SafeTreeMap<K, V>(delegate.subMap(checkValid(fromKey), checkValid(toKey)));
  }

  @Override public SortedMap<K, V> tailMap(K fromKey) {
    return new SafeTreeMap<K, V>(delegate.tailMap(checkValid(fromKey)));
  }

  @Override public Collection<V> values() {
    return delegate.values();
  }

  private <T> T checkValid(T t) {
    // a ClassCastException is what's supposed to happen!
    @SuppressWarnings("unchecked")
    K k = (K) t;
    comparator().compare(k, k);
    return t;
  }

  @Override public boolean equals(Object obj) {
    return delegate.equals(obj);
  }

  @Override public int hashCode() {
    return delegate.hashCode();
  }

  @Override public String toString() {
    return delegate.toString();
  }

  private static final long serialVersionUID = 0L;

}
