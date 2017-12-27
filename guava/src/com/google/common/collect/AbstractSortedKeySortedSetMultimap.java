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
import java.util.Collection;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;

/**
 * Basic implementation of a {@link SortedSetMultimap} with a sorted key set.
 *
 * <p>This superclass allows {@code TreeMultimap} to override methods to return navigable set and
 * map types in non-GWT only, while GWT code will inherit the SortedMap/SortedSet overrides.
 *
 * @author Louis Wasserman
 */
@GwtCompatible
abstract class AbstractSortedKeySortedSetMultimap<K, V> extends AbstractSortedSetMultimap<K, V> {

  AbstractSortedKeySortedSetMultimap(SortedMap<K, Collection<V>> map) {
    super(map);
  }

  @Override
  public SortedMap<K, Collection<V>> asMap() {
    return (SortedMap<K, Collection<V>>) super.asMap();
  }

  @Override
  SortedMap<K, Collection<V>> backingMap() {
    return (SortedMap<K, Collection<V>>) super.backingMap();
  }

  @Override
  public SortedSet<K> keySet() {
    return (SortedSet<K>) super.keySet();
  }

  @Override
  Set<K> createKeySet() {
    return createMaybeNavigableKeySet();
  }
}
