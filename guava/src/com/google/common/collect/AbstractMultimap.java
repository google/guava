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

import com.google.common.annotations.GwtCompatible;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.annotation.Nullable;

/**
 * A skeleton {@code Multimap} implementation, not necessarily in terms of a {@code Map}.
 * 
 * @author Louis Wasserman
 */
@GwtCompatible
abstract class AbstractMultimap<K, V> implements Multimap<K, V> {
  @Override
  public boolean isEmpty() {
    return size() == 0;
  }

  @Override
  public boolean containsValue(@Nullable Object value) {
    for (Collection<V> collection : asMap().values()) {
      if (collection.contains(value)) {
        return true;
      }
    }

    return false;
  }

  @Override
  public boolean containsEntry(@Nullable Object key, @Nullable Object value) {
    Collection<V> collection = asMap().get(key);
    return collection != null && collection.contains(value);
  }
  
  @Override
  public boolean remove(@Nullable Object key, @Nullable Object value) {
    Collection<V> collection = asMap().get(key);
    return collection != null && collection.remove(value);
  }

  @Override
  public boolean put(@Nullable K key, @Nullable V value) {
    return get(key).add(value);
  }

  @Override
  public boolean putAll(@Nullable K key, Iterable<? extends V> values) {
    checkNotNull(values);
    return values.iterator().hasNext() && Iterables.addAll(get(key), values);
  }

  @Override
  public boolean putAll(Multimap<? extends K, ? extends V> multimap) {
    boolean changed = false;
    for (Map.Entry<? extends K, ? extends V> entry : multimap.entries()) {
      changed |= put(entry.getKey(), entry.getValue());
    }
    return changed;
  }

  @Override
  public Collection<V> replaceValues(@Nullable K key, Iterable<? extends V> values) {
    checkNotNull(values);
    Collection<V> result = removeAll(key);
    putAll(key, values);
    return result;
  }
  
  private transient Collection<Entry<K, V>> entries;

  @Override
  public Collection<Entry<K, V>> entries() {
    Collection<Entry<K, V>> result = entries;
    return (result == null) ? entries = createEntries() : result;
  }
  
  Collection<Entry<K, V>> createEntries() {
    if (this instanceof SetMultimap) {
      return new Multimaps.EntrySet<K, V>() {
        @Override
        Multimap<K, V> multimap() {
          return AbstractMultimap.this;
        }

        @Override
        public Iterator<Entry<K, V>> iterator() {
          return entryIterator();
        }
      };
    }
    return new Multimaps.Entries<K, V>() {
      @Override
      Multimap<K, V> multimap() {
        return AbstractMultimap.this;
      }

      @Override
      public Iterator<Entry<K, V>> iterator() {
        return entryIterator();
      }
    };
  }
  
  abstract Iterator<Entry<K, V>> entryIterator();

  private transient Set<K> keySet;

  @Override
  public Set<K> keySet() {
    Set<K> result = keySet;
    return (result == null) ? keySet = createKeySet() : result;
  }

  Set<K> createKeySet() {
    return new Maps.KeySet<K, Collection<V>>() {
      @Override
      Map<K, Collection<V>> map() {
        return asMap();
      }
    };
  }
  
  private transient Multiset<K> keys;
  
  @Override
  public Multiset<K> keys() {
    Multiset<K> result = keys;
    return (result == null) ? keys = createKeys() : result;
  }
  
  Multiset<K> createKeys() {
    return new Multimaps.Keys<K, V>(this);
  }
  
  private transient Collection<V> values;
  
  @Override
  public Collection<V> values() {
    Collection<V> result = values;
    return (result == null) ? values = createValues() : result;
  }
  
  Collection<V> createValues() {
    return new Multimaps.Values<K, V>(this);
  }
  
  private transient Map<K, Collection<V>> asMap;
  
  @Override
  public Map<K, Collection<V>> asMap() {
    Map<K, Collection<V>> result = asMap;
    return (result == null) ? asMap = createAsMap() : result;
  }
  
  abstract Map<K, Collection<V>> createAsMap();

  // Comparison and hashing

  @Override public boolean equals(@Nullable Object object) {
    if (object == this) {
      return true;
    }
    if (object instanceof Multimap) {
      Multimap<?, ?> that = (Multimap<?, ?>) object;
      return this.asMap().equals(that.asMap());
    }
    return false;
  }

  /**
   * Returns the hash code for this multimap.
   *
   * <p>The hash code of a multimap is defined as the hash code of the map view,
   * as returned by {@link Multimap#asMap}.
   *
   * @see Map#hashCode
   */
  @Override public int hashCode() {
    return asMap().hashCode();
  }

  /**
   * Returns a string representation of the multimap, generated by calling
   * {@code toString} on the map returned by {@link Multimap#asMap}.
   *
   * @return a string representation of the multimap
   */
  @Override
  public String toString() {
    return asMap().toString();
  }
}
