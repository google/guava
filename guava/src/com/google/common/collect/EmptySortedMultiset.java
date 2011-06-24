/*
 * Copyright (C) 2011 The Guava Authors
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.common.collect;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.Range.BoundType;

import java.util.Comparator;
import java.util.Iterator;

import javax.annotation.Nullable;

/**
 * A {@link SortedMultiset} whose bounds are such that no element can ever be a
 * member.
 * 
 * @author Louis Wasserman
 */
final class EmptySortedMultiset<E> extends AbstractSortedMultiset<E> {
  private static final EmptySortedMultiset<Comparable> NATURAL_INSTANCE =
      new EmptySortedMultiset<Comparable>(Ordering.natural());

  @SuppressWarnings("unchecked")
  static <E extends Comparable> SortedMultiset<E> natural() {
    return (EmptySortedMultiset) NATURAL_INSTANCE;
  }

  @SuppressWarnings("unchecked")
  static <E> SortedMultiset<E> instance(
      @Nullable Comparator<? super E> comparator) {
    if (comparator == null || Ordering.natural().equals(comparator)) {
      return (EmptySortedMultiset) NATURAL_INSTANCE;
    }
    return new EmptySortedMultiset(comparator);
  }

  EmptySortedMultiset(Comparator<? super E> comparator) {
    super(comparator);
  }

  @Override
  public SortedMultiset<E> headMultiset(E upperBound, BoundType boundType) {
    return this;
  }

  @Override
  public SortedMultiset<E> tailMultiset(E fromElement, BoundType boundType) {
    return this;
  }

  @Override
  Iterator<Multiset.Entry<E>> descendingEntryIterator() {
    return Iterators.emptyIterator();
  }

  @Override
  Iterator<Multiset.Entry<E>> entryIterator() {
    return Iterators.emptyIterator();
  }

  @Override
  int distinctElements() {
    return 0;
  }

  @Override
  public int add(E element, int occurrences) {
    checkNotNull(element);
    throw new IllegalArgumentException();
  }

  @Override
  public int remove(Object element, int occurrences) {
    return 0;
  }

  @Override
  public void clear() {
  }
}
