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

import java.util.Comparator;
import java.util.TreeMap;

/**
 * GWT emulated version of {@link EmptyImmutableSortedMap}.
 *
 * @author Chris Povirk
 */
final class EmptyImmutableSortedMap<K, V> extends ImmutableSortedMap<K, V> {
  private EmptyImmutableSortedMap(Comparator<? super K> comparator) {
    super(new TreeMap<K, V>(comparator), comparator);
  }

  @SuppressWarnings("unchecked")
  private static final ImmutableSortedMap<Object, Object> NATURAL_EMPTY_MAP =
      new EmptyImmutableSortedMap<Object, Object>(NATURAL_ORDER);

  static <K, V> ImmutableSortedMap<K, V> forComparator(Comparator<? super K> comparator) {
    if (comparator == NATURAL_ORDER) {
      return (ImmutableSortedMap) NATURAL_EMPTY_MAP;
    }
    return new EmptyImmutableSortedMap<K, V>(comparator);
  }
}
