/*
 * Copyright (C) 2009 The Guava Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.common.collect;

import com.google.common.annotations.GwtCompatible;

import java.util.Comparator;

/**
 * List returned by {@code ImmutableSortedSet.asList()} when the set isn't empty.
 *
 * @author Jared Levy
 * @author Louis Wasserman
 */
@GwtCompatible(emulated = true)
@SuppressWarnings("serial")
final class ImmutableSortedAsList<E> extends RegularImmutableAsList<E>
    implements SortedIterable<E> {
  ImmutableSortedAsList(
      ImmutableSortedSet<E> backingSet, ImmutableList<E> backingList) {
    super(backingSet, backingList);
  }

  @Override
  ImmutableSortedSet<E> delegateCollection() {
    return (ImmutableSortedSet<E>) super.delegateCollection();
  }

  @Override public Comparator<? super E> comparator() {
    return delegateCollection().comparator();
  }

  // Override indexOf() and lastIndexOf() to be O(log N) instead of O(N).

  @Override
  public boolean contains(Object target) {
    // Necessary for ISS's with comparators inconsistent with equals.
    return indexOf(target) >= 0;
  }
}

