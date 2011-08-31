/*
 * Copyright (C) 2011 The Guava Authors
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

import javax.annotation.Nullable;

/**
 * A descending wrapper around an {@code ImmutableSortedMultiset}
 * 
 * @author Louis Wasserman
 */
final class DescendingImmutableSortedMultiset<E> extends ImmutableSortedMultiset<E> {
  private final transient ImmutableSortedMultiset<E> forward;

  DescendingImmutableSortedMultiset(ImmutableSortedMultiset<E> forward) {
    super(forward.reverseComparator());
    this.forward = forward;
  }

  @Override
  public int count(@Nullable Object element) {
    return forward.count(element);
  }

  @Override
  public Entry<E> firstEntry() {
    return forward.lastEntry();
  }

  @Override
  public Entry<E> lastEntry() {
    return forward.firstEntry();
  }

  @Override
  public int size() {
    return forward.size();
  }

  @Override
  ImmutableSortedSet<E> createElementSet() {
    return forward.createDescendingElementSet();
  }

  @Override
  ImmutableSortedSet<E> createDescendingElementSet() {
    return forward.elementSet();
  }

  @Override
  UnmodifiableIterator<Entry<E>> descendingEntryIterator() {
    return forward.entryIterator();
  }

  @Override
  public ImmutableSortedMultiset<E> descendingMultiset() {
    return forward;
  }

  @Override
  public ImmutableSortedMultiset<E> headMultiset(E upperBound, BoundType boundType) {
    return forward.tailMultiset(upperBound, boundType).descendingMultiset();
  }

  @Override
  public ImmutableSortedMultiset<E> tailMultiset(E lowerBound, BoundType boundType) {
    return forward.headMultiset(lowerBound, boundType).descendingMultiset();
  }

  @Override
  UnmodifiableIterator<Entry<E>> entryIterator() {
    return forward.descendingEntryIterator();
  }

  @Override
  int distinctElements() {
    return forward.distinctElements();
  }

  @Override
  boolean isPartialView() {
    return forward.isPartialView();
  }
}
