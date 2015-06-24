/*
 * Copyright (C) 2012 The Guava Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.common.collect;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkPositionIndex;

import com.google.common.annotations.GwtCompatible;
import com.google.common.base.Predicate;
import com.google.j2objc.annotations.WeakOuter;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.annotation.Nullable;

/**
 * Implementation of {@link Multimaps#filterKeys(Multimap, Predicate)}.
 *
 * @author Louis Wasserman
 */
@GwtCompatible
class FilteredKeyMultimap<K, V> extends AbstractMultimap<K, V> implements FilteredMultimap<K, V> {
  final Multimap<K, V> unfiltered;
  final Predicate<? super K> keyPredicate;

  FilteredKeyMultimap(Multimap<K, V> unfiltered, Predicate<? super K> keyPredicate) {
    this.unfiltered = checkNotNull(unfiltered);
    this.keyPredicate = checkNotNull(keyPredicate);
  }

  @Override
  public Multimap<K, V> unfiltered() {
    return unfiltered;
  }

  @Override
  public Predicate<? super Entry<K, V>> entryPredicate() {
    return Maps.keyPredicateOnEntries(keyPredicate);
  }

  @Override
  public int size() {
    int size = 0;
    for (Collection<V> collection : asMap().values()) {
      size += collection.size();
    }
    return size;
  }

  @Override
  public boolean containsKey(@Nullable Object key) {
    if (unfiltered.containsKey(key)) {
      @SuppressWarnings("unchecked") // k is equal to a K, if not one itself
      K k = (K) key;
      return keyPredicate.apply(k);
    }
    return false;
  }

  @Override
  public Collection<V> removeAll(Object key) {
    return containsKey(key) ? unfiltered.removeAll(key) : unmodifiableEmptyCollection();
  }

  Collection<V> unmodifiableEmptyCollection() {
    if (unfiltered instanceof SetMultimap) {
      return ImmutableSet.of();
    } else {
      return ImmutableList.of();
    }
  }

  @Override
  public void clear() {
    keySet().clear();
  }

  @Override
  Set<K> createKeySet() {
    return Sets.filter(unfiltered.keySet(), keyPredicate);
  }

  @Override
  public Collection<V> get(K key) {
    if (keyPredicate.apply(key)) {
      return unfiltered.get(key);
    } else if (unfiltered instanceof SetMultimap) {
      return new AddRejectingSet<K, V>(key);
    } else {
      return new AddRejectingList<K, V>(key);
    }
  }

  static class AddRejectingSet<K, V> extends ForwardingSet<V> {
    final K key;

    AddRejectingSet(K key) {
      this.key = key;
    }

    @Override
    public boolean add(V element) {
      throw new IllegalArgumentException("Key does not satisfy predicate: " + key);
    }

    @Override
    public boolean addAll(Collection<? extends V> collection) {
      checkNotNull(collection);
      throw new IllegalArgumentException("Key does not satisfy predicate: " + key);
    }

    @Override
    protected Set<V> delegate() {
      return Collections.emptySet();
    }
  }

  static class AddRejectingList<K, V> extends ForwardingList<V> {
    final K key;

    AddRejectingList(K key) {
      this.key = key;
    }

    @Override
    public boolean add(V v) {
      add(0, v);
      return true;
    }

    @Override
    public boolean addAll(Collection<? extends V> collection) {
      addAll(0, collection);
      return true;
    }

    @Override
    public void add(int index, V element) {
      checkPositionIndex(index, 0);
      throw new IllegalArgumentException("Key does not satisfy predicate: " + key);
    }

    @Override
    public boolean addAll(int index, Collection<? extends V> elements) {
      checkNotNull(elements);
      checkPositionIndex(index, 0);
      throw new IllegalArgumentException("Key does not satisfy predicate: " + key);
    }

    @Override
    protected List<V> delegate() {
      return Collections.emptyList();
    }
  }

  @Override
  Iterator<Entry<K, V>> entryIterator() {
    throw new AssertionError("should never be called");
  }

  @Override
  Collection<Entry<K, V>> createEntries() {
    return new Entries();
  }

  @WeakOuter
  class Entries extends ForwardingCollection<Entry<K, V>> {
    @Override
    protected Collection<Entry<K, V>> delegate() {
      return Collections2.filter(unfiltered.entries(), entryPredicate());
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean remove(@Nullable Object o) {
      if (o instanceof Entry) {
        Entry<?, ?> entry = (Entry<?, ?>) o;
        if (unfiltered.containsKey(entry.getKey())
            // if this holds, then we know entry.getKey() is a K
            && keyPredicate.apply((K) entry.getKey())) {
          return unfiltered.remove(entry.getKey(), entry.getValue());
        }
      }
      return false;
    }
  }

  @Override
  Collection<V> createValues() {
    return new FilteredMultimapValues<K, V>(this);
  }

  @Override
  Map<K, Collection<V>> createAsMap() {
    return Maps.filterKeys(unfiltered.asMap(), keyPredicate);
  }

  @Override
  Multiset<K> createKeys() {
    return Multisets.filter(unfiltered.keys(), keyPredicate);
  }
}
