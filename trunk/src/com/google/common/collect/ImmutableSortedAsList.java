/*
 * Copyright (C) 2009 Google Inc.
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

import com.google.common.base.Preconditions;

/**
 * List returned by {@code ImmutableSortedSet.asList()} when the set isn't
 * empty.
 *
 * @author Jared Levy
 */
@SuppressWarnings("serial")
final class ImmutableSortedAsList<E> extends RegularImmutableList<E> {
  private final transient ImmutableSortedSet<E> set;

  ImmutableSortedAsList(Object[] array, int offset, int size,
      ImmutableSortedSet<E> set) {
    super(array, offset, size);
    this.set = set;
  }

  // Override contains(), indexOf(), and lastIndexOf() to be O(log N) instead of
  // O(N).

  @Override public boolean contains(Object target) {
    return set.indexOf(target) >= 0;
  }

  @Override public int indexOf(Object target) {
    return set.indexOf(target);
  }

  @Override public int lastIndexOf(Object target) {
    return set.indexOf(target);
  }

  // The returned ImmutableSortedAsList maintains the contains(), indexOf(), and
  // lastIndexOf() performance benefits.
  @Override public ImmutableList<E> subList(int fromIndex, int toIndex) {
    Preconditions.checkPositionIndexes(fromIndex, toIndex, size());
    return (fromIndex == toIndex)
        ? ImmutableList.<E>of()
        : new RegularImmutableSortedSet<E>(
            array(), set.comparator(),
            offset() + fromIndex, offset() + toIndex).asList();
  }

  // The ImmutableAsList serialized form has the correct behavior.
  @Override Object writeReplace() {
    return new ImmutableAsList.SerializedForm(set);
  }
}
