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

import com.google.common.annotations.GwtCompatible;
import com.google.common.base.Predicate;
import java.util.Map.Entry;
import java.util.Set;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;

/**
 * Implementation of {@link Multimaps#filterKeys(SetMultimap, Predicate)}.
 *
 * @author Louis Wasserman
 */
@GwtCompatible
final class FilteredKeySetMultimap<K, V> extends FilteredKeyMultimap<K, V>
    implements FilteredSetMultimap<K, V> {

  FilteredKeySetMultimap(SetMultimap<K, V> unfiltered, Predicate<? super K> keyPredicate) {
    super(unfiltered, keyPredicate);
  }

  @Override
  public SetMultimap<K, V> unfiltered() {
    return (SetMultimap<K, V>) unfiltered;
  }

  @Override
  public Set<V> get(K key) {
    return (Set<V>) super.get(key);
  }

  @Override
  public Set<V> removeAll(Object key) {
    return (Set<V>) super.removeAll(key);
  }

  @Override
  public Set<V> replaceValues(K key, Iterable<? extends V> values) {
    return (Set<V>) super.replaceValues(key, values);
  }

  @Override
  public Set<Entry<K, V>> entries() {
    return (Set<Entry<K, V>>) super.entries();
  }

  @Override
  Set<Entry<K, V>> createEntries() {
    return new EntrySet();
  }

  class EntrySet extends Entries implements Set<Entry<K, V>> {
    @Override
    public int hashCode() {
      return Sets.hashCodeImpl(this);
    }

    @Override
    public boolean equals(@NullableDecl Object o) {
      return Sets.equalsImpl(this, o);
    }
  }
}
