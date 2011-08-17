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

import com.google.common.base.Preconditions;

import java.util.Comparator;

import javax.annotation.Nullable;

/**
 * List returned by {@code ImmutableSortedSet.asList()} when the set isn't empty.
 *
 * @author Jared Levy
 * @author Louis Wasserman
 */
@SuppressWarnings("serial")
final class ImmutableSortedAsList<E> extends ImmutableList<E> implements SortedIterable<E> {
  private final transient ImmutableSortedSet<E> backingSet;
  private final transient ImmutableList<E> backingList;

  ImmutableSortedAsList(
      ImmutableSortedSet<E> backingSet, ImmutableList<E> backingList) {
    this.backingSet = backingSet;
    this.backingList = backingList;
  }

  @Override public Comparator<? super E> comparator() {
    return backingSet.comparator();
  }

  // Override contains(), indexOf(), and lastIndexOf() to be O(log N) instead of O(N).

  @Override public boolean contains(@Nullable Object target) {
    // TODO: why not contains(target)?
    return backingSet.indexOf(target) >= 0;
  }

  @Override public int indexOf(@Nullable Object target) {
    return backingSet.indexOf(target);
  }

  @Override public int lastIndexOf(@Nullable Object target) {
    return backingSet.indexOf(target);
  }

  // The returned ImmutableSortedAsList maintains the contains(), indexOf(), and
  // lastIndexOf() performance benefits.
  @Override public ImmutableList<E> subList(int fromIndex, int toIndex) {
    Preconditions.checkPositionIndexes(fromIndex, toIndex, size());
    return (fromIndex == toIndex) ? ImmutableList.<E>of()
        : new RegularImmutableSortedSet<E>(
            backingList.subList(fromIndex, toIndex), backingSet.comparator())
            .asList();
  }

  // The ImmutableAsList serialized form has the correct behavior.
  @Override Object writeReplace() {
    return new ImmutableAsList.SerializedForm(backingSet);
  }

  @Override public UnmodifiableIterator<E> iterator() {
    return backingList.iterator();
  }

  @Override public E get(int index) {
    return backingList.get(index);
  }

  @Override public UnmodifiableListIterator<E> listIterator() {
    return backingList.listIterator();
  }

  @Override public UnmodifiableListIterator<E> listIterator(int index) {
    return backingList.listIterator(index);
  }

  @Override public int size() {
    return backingList.size();
  }

  @Override public boolean equals(@Nullable Object obj) {
    return backingList.equals(obj);
  }

  @Override public int hashCode() {
    return backingList.hashCode();
  }

  @Override boolean isPartialView() {
    return backingList.isPartialView();
  }
}
