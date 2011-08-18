/*
 * Copyright (C) 2009 The Guava Authors
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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.SortedLists.KeyAbsentBehavior.INVERTED_INSERTION_INDEX;
import static com.google.common.collect.SortedLists.KeyAbsentBehavior.NEXT_HIGHER;
import static com.google.common.collect.SortedLists.KeyPresentBehavior.ANY_PRESENT;
import static com.google.common.collect.SortedLists.KeyPresentBehavior.FIRST_AFTER;
import static com.google.common.collect.SortedLists.KeyPresentBehavior.FIRST_PRESENT;

import com.google.common.annotations.GwtCompatible;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;

import javax.annotation.Nullable;

/**
 * An immutable sorted set with one or more elements. TODO(jlevy): Consider
 * separate class for a single-element sorted set.
 *
 * @author Jared Levy
 * @author Louis Wasserman
 */
@GwtCompatible(serializable = true, emulated = true)
@SuppressWarnings("serial")
final class RegularImmutableSortedSet<E> extends ImmutableSortedSet<E> {

  private transient final ImmutableList<E> elements;

  RegularImmutableSortedSet(
      ImmutableList<E> elements, Comparator<? super E> comparator) {
    super(comparator);
    this.elements = elements;
    checkArgument(!elements.isEmpty());
  }

  @Override public UnmodifiableIterator<E> iterator() {
    return elements.iterator();
  }

  @Override public boolean isEmpty() {
    return false;
  }

  @Override
  public int size() {
    return elements.size();
  }

  @Override public boolean contains(Object o) {
    if (o == null) {
      return false;
    }
    try {
      return binarySearch(o) >= 0;
    } catch (ClassCastException e) {
      return false;
    }
  }

  @Override public boolean containsAll(Collection<?> targets) {
    // TODO(jlevy): For optimal performance, use a binary search when
    // targets.size() < size() / log(size())
    // TODO(kevinb): see if we can share code with OrderedIterator after it
    // graduates from labs.
    if (!SortedIterables.hasSameComparator(comparator(), targets) 
        || (targets.size() <= 1)) {
      return super.containsAll(targets);
    }

    /*
     * If targets is a sorted set with the same comparator, containsAll can run
     * in O(n) time stepping through the two collections.
     */
    Iterator<E> thisIterator = iterator();
    Iterator<?> thatIterator = targets.iterator();
    Object target = thatIterator.next();

    try {

      while (thisIterator.hasNext()) {

        int cmp = unsafeCompare(thisIterator.next(), target);

        if (cmp == 0) {

          if (!thatIterator.hasNext()) {

            return true;
          }

          target = thatIterator.next();

        } else if (cmp > 0) {
          return false;
        }
      }
    } catch (NullPointerException e) {
      return false;
    } catch (ClassCastException e) {
      return false;
    }

    return false;
  }

  private int binarySearch(Object key) {
    // TODO(kevinb): split this into binarySearch(E) and 
    // unsafeBinarySearch(Object), use each appropriately. name all methods that
    // might throw CCE "unsafe*".
    
    // Pretend the comparator can compare anything. If it turns out it can't
    // compare a and b, we should get a CCE on the subsequent line. Only methods
    // that are spec'd to throw CCE should call this.
    @SuppressWarnings("unchecked")
    Comparator<Object> unsafeComparator = (Comparator<Object>) comparator;

    return Collections.binarySearch(elements, key, unsafeComparator);
  }

  @Override boolean isPartialView() {
    return elements.isPartialView();
  }

  @Override public Object[] toArray() {
    return elements.toArray();
  }

  @Override public <T> T[] toArray(T[] array) {
    return elements.toArray(array);
  }

  @Override public boolean equals(@Nullable Object object) {
    if (object == this) {
      return true;
    }
    if (!(object instanceof Set)) {
      return false;
    }

    Set<?> that = (Set<?>) object;
    if (size() != that.size()) {
      return false;
    }

    if (SortedIterables.hasSameComparator(comparator, that)) {
      Iterator<?> otherIterator = that.iterator();
      try {
        Iterator<E> iterator = iterator();
        while (iterator.hasNext()) {
          Object element = iterator.next();
          Object otherElement = otherIterator.next();
          if (otherElement == null
              || unsafeCompare(element, otherElement) != 0) {
            return false;
          }
        }
        return true;
      } catch (ClassCastException e) {
        return false;
      } catch (NoSuchElementException e) {
        return false; // concurrent change to other set
      }
    }
    return this.containsAll(that);
  }

  @Override
  public E first() {
    return elements.get(0);
  }

  @Override
  public E last() {
    return elements.get(size() - 1);
  }

  @Override
  ImmutableSortedSet<E> headSetImpl(E toElement, boolean inclusive) {
    int index;
    if (inclusive) {
      index = SortedLists.binarySearch(
          elements, checkNotNull(toElement), comparator(), FIRST_AFTER, NEXT_HIGHER);
    } else {
      index = SortedLists.binarySearch(
          elements, checkNotNull(toElement), comparator(), FIRST_PRESENT, NEXT_HIGHER);
    }
    return createSubset(0, index);
  }

  @Override
  ImmutableSortedSet<E> subSetImpl(
      E fromElement, boolean fromInclusive, E toElement, boolean toInclusive) {
    return tailSetImpl(fromElement, fromInclusive)
        .headSetImpl(toElement, toInclusive);
  }

  @Override
  ImmutableSortedSet<E> tailSetImpl(E fromElement, boolean inclusive) {
    int index;
    if (inclusive) {
      index = SortedLists.binarySearch(
          elements, checkNotNull(fromElement), comparator(), FIRST_PRESENT, NEXT_HIGHER);
    } else {
      index = SortedLists.binarySearch(
          elements, checkNotNull(fromElement), comparator(), FIRST_AFTER, NEXT_HIGHER);
    }
    return createSubset(index, size());
  }

  // Pretend the comparator can compare anything. If it turns out it can't
  // compare two elements, it'll throw a CCE. Only methods that are specified to
  // throw CCE should call this.
  @SuppressWarnings("unchecked")
  Comparator<Object> unsafeComparator() {
    return (Comparator<Object>) comparator;
  }

  private ImmutableSortedSet<E> createSubset(int newFromIndex, int newToIndex) {
    if (newFromIndex == 0 && newToIndex == size()) {
      return this;
    } else if (newFromIndex < newToIndex) {
      return new RegularImmutableSortedSet<E>(
          elements.subList(newFromIndex, newToIndex), comparator);
    } else {
      return emptySet(comparator);
    }
  }

  @SuppressWarnings("unchecked")
  @Override int indexOf(@Nullable Object target) {
    if (target == null) {
      return -1;
    }
    int position;
    try {
      position = SortedLists.binarySearch(elements, (E) target, comparator(), 
          ANY_PRESENT, INVERTED_INSERTION_INDEX);
    } catch (ClassCastException e) {
      return -1;
    }
    // TODO(kevinb): reconsider if it's really worth making feeble attempts at 
    // sanity for inconsistent comparators.
    
    // The equals() check is needed when the comparator isn't compatible with
    // equals().
    return (position >= 0 && elements.get(position).equals(target))
        ? position : -1;
  }

  @Override ImmutableList<E> createAsList() {
    return new ImmutableSortedAsList<E>(this, elements);
  }
}
