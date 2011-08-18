/*
 * Copyright (C) 2011 The Guava Authors
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

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.SortedLists.KeyAbsentBehavior.INVERTED_INSERTION_INDEX;
import static com.google.common.collect.SortedLists.KeyAbsentBehavior.NEXT_HIGHER;
import static com.google.common.collect.SortedLists.KeyAbsentBehavior.NEXT_LOWER;
import static com.google.common.collect.SortedLists.KeyPresentBehavior.ANY_PRESENT;

import com.google.common.primitives.Ints;

import java.util.Comparator;
import java.util.List;

import javax.annotation.Nullable;

/**
 * An immutable sorted multiset with one or more distinct elements.
 *
 * @author Louis Wasserman
 */
final class RegularImmutableSortedMultiset<E> extends ImmutableSortedMultiset<E> {
  private static final class CumulativeCountEntry<E> extends Multisets.AbstractEntry<E> {
    final E element;
    final int count;
    final long cumulativeCount;

    CumulativeCountEntry(E element, int count, @Nullable CumulativeCountEntry<E> previous) {
      this.element = element;
      this.count = count;
      this.cumulativeCount = count + ((previous == null) ? 0 : previous.cumulativeCount);
    }

    @Override
    public E getElement() {
      return element;
    }

    @Override
    public int getCount() {
      return count;
    }
  }

  static <E> RegularImmutableSortedMultiset<E> createFromSorted(Comparator<? super E> comparator,
      List<? extends Multiset.Entry<E>> entries) {
    List<CumulativeCountEntry<E>> newEntries = Lists.newArrayListWithCapacity(entries.size());
    CumulativeCountEntry<E> previous = null;
    for (Multiset.Entry<E> entry : entries) {
      newEntries.add(
          previous = new CumulativeCountEntry<E>(entry.getElement(), entry.getCount(), previous));
    }
    return new RegularImmutableSortedMultiset<E>(comparator, ImmutableList.copyOf(newEntries));
  }

  final transient ImmutableList<CumulativeCountEntry<E>> entries;

  RegularImmutableSortedMultiset(
      Comparator<? super E> comparator, ImmutableList<CumulativeCountEntry<E>> entries) {
    super(comparator);
    this.entries = entries;
    assert !entries.isEmpty();
  }

  ImmutableList<E> elementList() {
    return new TransformedImmutableList<CumulativeCountEntry<E>, E>(entries) {
      @Override
      E transform(CumulativeCountEntry<E> entry) {
        return entry.getElement();
      }
    };
  }

  @Override
  ImmutableSortedSet<E> createElementSet() {
    return new RegularImmutableSortedSet<E>(elementList(), comparator());
  }

  @Override
  ImmutableSortedSet<E> createDescendingElementSet() {
    return new RegularImmutableSortedSet<E>(elementList().reverse(), reverseComparator());
  }

  @SuppressWarnings("unchecked")
  @Override
  UnmodifiableIterator<Multiset.Entry<E>> entryIterator() {
    return (UnmodifiableIterator) entries.iterator();
  }

  @SuppressWarnings("unchecked")
  @Override
  UnmodifiableIterator<Multiset.Entry<E>> descendingEntryIterator() {
    return (UnmodifiableIterator) entries.reverse().iterator();
  }

  @Override
  public CumulativeCountEntry<E> firstEntry() {
    return entries.get(0);
  }

  @Override
  public CumulativeCountEntry<E> lastEntry() {
    return entries.get(entries.size() - 1);
  }

  @Override
  public int size() {
    CumulativeCountEntry<E> firstEntry = firstEntry();
    CumulativeCountEntry<E> lastEntry = lastEntry();
    return Ints.saturatedCast(
        lastEntry.cumulativeCount - firstEntry.cumulativeCount + firstEntry.count);
  }

  @Override
  int distinctElements() {
    return entries.size();
  }

  @Override
  boolean isPartialView() {
    return entries.isPartialView();
  }

  @SuppressWarnings("unchecked")
  @Override
  public int count(@Nullable Object element) {
    if (element == null) {
      return 0;
    }
    try {
      int index = SortedLists.binarySearch(
          elementList(), (E) element, comparator(), ANY_PRESENT, INVERTED_INSERTION_INDEX);
      return (index >= 0) ? entries.get(index).getCount() : 0;
    } catch (ClassCastException e) {
      return 0;
    }
  }

  @Override
  public ImmutableSortedMultiset<E> headMultiset(E upperBound, BoundType boundType) {
    int index;
    switch (boundType) {
      case OPEN:
        index = SortedLists.binarySearch(
            elementList(), checkNotNull(upperBound), comparator(), ANY_PRESENT, NEXT_HIGHER);
        break;
      case CLOSED:
        index = SortedLists.binarySearch(
            elementList(), checkNotNull(upperBound), comparator(), ANY_PRESENT, NEXT_LOWER) + 1;
        break;
      default:
        throw new AssertionError();
    }
    return createSubMultiset(0, index);
  }

  @Override
  public ImmutableSortedMultiset<E> tailMultiset(E lowerBound, BoundType boundType) {
    int index;
    switch (boundType) {
      case OPEN:
        index = SortedLists.binarySearch(
            elementList(), checkNotNull(lowerBound), comparator(), ANY_PRESENT, NEXT_LOWER) + 1;
        break;
      case CLOSED:
        index = SortedLists.binarySearch(
            elementList(), checkNotNull(lowerBound), comparator(), ANY_PRESENT, NEXT_HIGHER);
        break;
      default:
        throw new AssertionError();
    }
    return createSubMultiset(index, distinctElements());
  }

  private ImmutableSortedMultiset<E> createSubMultiset(int newFromIndex, int newToIndex) {
    if (newFromIndex == 0 && newToIndex == entries.size()) {
      return this;
    } else if (newFromIndex >= newToIndex) {
      return emptyMultiset(comparator());
    } else {
      return new RegularImmutableSortedMultiset<E>(
          comparator(), entries.subList(newFromIndex, newToIndex));
    }
  }
}
