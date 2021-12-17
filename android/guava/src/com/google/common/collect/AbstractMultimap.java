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
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.errorprone.annotations.concurrent.LazyInit;
import com.google.j2objc.annotations.WeakOuter;
import java.util.AbstractCollection;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import javax.annotation.CheckForNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * A skeleton {@code Multimap} implementation, not necessarily in terms of a {@code Map}.
 *
 * @author Louis Wasserman
 */
@GwtCompatible
@ElementTypesAreNonnullByDefault
abstract class AbstractMultimap<K extends @Nullable Object, V extends @Nullable Object>
    implements Multimap<K, V> {
  @Override
  public boolean isEmpty() {
    return size() == 0;
  }

  @Override
  public boolean containsValue(@CheckForNull Object value) {
    for (Collection<V> collection : asMap().values()) {
      if (collection.contains(value)) {
        return true;
      }
    }

    return false;
  }

  @Override
  public boolean containsEntry(@CheckForNull Object key, @CheckForNull Object value) {
    Collection<V> collection = asMap().get(key);
    return collection != null && collection.contains(value);
  }

  @CanIgnoreReturnValue
  @Override
  public boolean remove(@CheckForNull Object key, @CheckForNull Object value) {
    Collection<V> collection = asMap().get(key);
    return collection != null && collection.remove(value);
  }

  @CanIgnoreReturnValue
  @Override
  public boolean put(@ParametricNullness K key, @ParametricNullness V value) {
    return get(key).add(value);
  }

  @CanIgnoreReturnValue
  @Override
  public boolean putAll(@ParametricNullness K key, Iterable<? extends V> values) {
    checkNotNull(values);
    // make sure we only call values.iterator() once
    // and we only call get(key) if values is nonempty
    if (values instanceof Collection) {
      Collection<? extends V> valueCollection = (Collection<? extends V>) values;
      return !valueCollection.isEmpty() && get(key).addAll(valueCollection);
    } else {
      Iterator<? extends V> valueItr = values.iterator();
      return valueItr.hasNext() && Iterators.addAll(get(key), valueItr);
    }
  }

  @CanIgnoreReturnValue
  @Override
  public boolean putAll(Multimap<? extends K, ? extends V> multimap) {
    boolean changed = false;
    for (Entry<? extends K, ? extends V> entry : multimap.entries()) {
      changed |= put(entry.getKey(), entry.getValue());
    }
    return changed;
  }

  @CanIgnoreReturnValue
  @Override
  public Collection<V> replaceValues(@ParametricNullness K key, Iterable<? extends V> values) {
    checkNotNull(values);
    Collection<V> result = removeAll(key);
    putAll(key, values);
    return result;
  }

  @LazyInit @CheckForNull private transient Collection<Entry<K, V>> entries;

  @Override
  public Collection<Entry<K, V>> entries() {
    Collection<Entry<K, V>> result = entries;
    return (result == null) ? entries = createEntries() : result;
  }

  abstract Collection<Entry<K, V>> createEntries();

  @WeakOuter
  class Entries extends Multimaps.Entries<K, V> {
    @Override
    Multimap<K, V> multimap() {
      return AbstractMultimap.this;
    }

    @Override
    public Iterator<Entry<K, V>> iterator() {
      return entryIterator();
    }
  }

  @WeakOuter
  class EntrySet extends Entries implements Set<Entry<K, V>> {
    @Override
    public int hashCode() {
      return Sets.hashCodeImpl(this);
    }

    @Override
    public boolean equals(@CheckForNull Object obj) {
      return Sets.equalsImpl(this, obj);
    }
  }

  abstract Iterator<Entry<K, V>> entryIterator();

  @LazyInit @CheckForNull private transient Set<K> keySet;

  @Override
  public Set<K> keySet() {
    Set<K> result = keySet;
    return (result == null) ? keySet = createKeySet() : result;
  }

  abstract Set<K> createKeySet();

  @LazyInit @CheckForNull private transient Multiset<K> keys;

  @Override
  public Multiset<K> keys() {
    Multiset<K> result = keys;
    return (result == null) ? keys = createKeys() : result;
  }

  abstract Multiset<K> createKeys();

  @LazyInit @CheckForNull private transient Collection<V> values;

  @Override
  public Collection<V> values() {
    Collection<V> result = values;
    return (result == null) ? values = createValues() : result;
  }

  abstract Collection<V> createValues();

  @WeakOuter
  class Values extends AbstractCollection<V> {
    @Override
    public Iterator<V> iterator() {
      return valueIterator();
    }

    @Override
    public int size() {
      return AbstractMultimap.this.size();
    }

    @Override
    public boolean contains(@CheckForNull Object o) {
      return AbstractMultimap.this.containsValue(o);
    }

    @Override
    public void clear() {
      AbstractMultimap.this.clear();
    }
  }

  Iterator<V> valueIterator() {
    return Maps.valueIterator(entries().iterator());
  }

  @LazyInit @CheckForNull private transient Map<K, Collection<V>> asMap;

  @Override
  public Map<K, Collection<V>> asMap() {
    Map<K, Collection<V>> result = asMap;
    return (result == null) ? asMap = createAsMap() : result;
  }

  abstract Map<K, Collection<V>> createAsMap();

  // Comparison and hashing

  @Override
  public boolean equals(@CheckForNull Object object) {
    return Multimaps.equalsImpl(this, object);
  }

  /**
   * Returns the hash code for this multimap.
   *
   * <p>The hash code of a multimap is defined as the hash code of the map view, as returned by
   * {@link Multimap#asMap}.
   *
   * @see Map#hashCode
   */
  @Override
  public int hashCode() {
    return asMap().hashCode();
  }

  /**
   * Returns a string representation of the multimap, generated by calling {@code toString} on the
   * map returned by {@link Multimap#asMap}.
   *
   * @return a string representation of the multimap
   */
  @Override
  public String toString() {
    return asMap().toString();
  }
}
