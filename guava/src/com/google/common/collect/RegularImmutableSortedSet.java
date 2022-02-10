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

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.Spliterator;
import java.util.function.Consumer;
import javax.annotation.CheckForNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * An immutable sorted set with one or more elements. TODO(jlevy): Consider separate class for a
 * single-element sorted set.
 *
 * @author Jared Levy
 * @author Louis Wasserman
 */
@GwtCompatible(serializable = true, emulated = true)
@SuppressWarnings({"serial", "rawtypes"})
@ElementTypesAreNonnullByDefault
final class RegularImmutableSortedSet<E> extends ImmutableSortedSet<E> {
  static final RegularImmutableSortedSet<Comparable> NATURAL_EMPTY_SET =
      new RegularImmutableSortedSet<>(ImmutableList.<Comparable>of(), Ordering.natural());

  private final transient ImmutableList<E> elements;

  RegularImmutableSortedSet(ImmutableList<E> elements, Comparator<? super E> comparator) {
    super(comparator);
    this.elements = elements;
  }

  @Override
  @CheckForNull
  Object[] internalArray() {
    return elements.internalArray();
  }

  @Override
  int internalArrayStart() {
    return elements.internalArrayStart();
  }

  @Override
  int internalArrayEnd() {
    return elements.internalArrayEnd();
  }

  @Override
  public UnmodifiableIterator<E> iterator() {
    return elements.iterator();
  }

  @GwtIncompatible // NavigableSet
  @Override
  public UnmodifiableIterator<E> descendingIterator() {
    return elements.reverse().iterator();
  }

  @Override
  public Spliterator<E> spliterator() {
    return asList().spliterator();
  }

  @Override
  public void forEach(Consumer<? super E> action) {
    elements.forEach(action);
  }

  @Override
  public int size() {
    return elements.size();
  }

  @Override
  public boolean contains(@CheckForNull Object o) {
    try {
      return o != null && unsafeBinarySearch(o) >= 0;
    } catch (ClassCastException e) {
      return false;
    }
  }

  @Override
  public boolean containsAll(Collection<?> targets) {
    // TODO(jlevy): For optimal performance, use a binary search when
    // targets.size() < size() / log(size())
    // TODO(kevinb): see if we can share code with OrderedIterator after it
    // graduates from labs.
    if (targets instanceof Multiset) {
      targets = ((Multiset<?>) targets).elementSet();
    }
    if (!SortedIterables.hasSameComparator(comparator(), targets) || (targets.size() <= 1)) {
      return super.containsAll(targets);
    }

    /*
     * If targets is a sorted set with the same comparator, containsAll can run
     * in O(n) time stepping through the two collections.
     */
    Iterator<E> thisIterator = iterator();

    Iterator<?> thatIterator = targets.iterator();
    // known nonempty since we checked targets.size() > 1

    if (!thisIterator.hasNext()) {
      return false;
    }

    Object target = thatIterator.next();
    E current = thisIterator.next();
    try {
      while (true) {
        int cmp = unsafeCompare(current, target);

        if (cmp < 0) {
          if (!thisIterator.hasNext()) {
            return false;
          }
          current = thisIterator.next();
        } else if (cmp == 0) {
          if (!thatIterator.hasNext()) {
            return true;
          }
          target = thatIterator.next();

        } else if (cmp > 0) {
          return false;
        }
      }
    } catch (NullPointerException | ClassCastException e) {
      return false;
    }
  }

  private int unsafeBinarySearch(Object key) throws ClassCastException {
    return Collections.binarySearch(elements, key, unsafeComparator());
  }

  @Override
  boolean isPartialView() {
    return elements.isPartialView();
  }

  @Override
  int copyIntoArray(@Nullable Object[] dst, int offset) {
    return elements.copyIntoArray(dst, offset);
  }

  @Override
  public boolean equals(@CheckForNull Object object) {
    if (object == this) {
      return true;
    }
    if (!(object instanceof Set)) {
      return false;
    }

    Set<?> that = (Set<?>) object;
    if (size() != that.size()) {
      return false;
    } else if (isEmpty()) {
      return true;
    }

    if (SortedIterables.hasSameComparator(comparator, that)) {
      Iterator<?> otherIterator = that.iterator();
      try {
        Iterator<E> iterator = iterator();
        while (iterator.hasNext()) {
          Object element = iterator.next();
          Object otherElement = otherIterator.next();
          if (otherElement == null || unsafeCompare(element, otherElement) != 0) {
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
    return containsAll(that);
  }

  @Override
  public E first() {
    if (isEmpty()) {
      throw new NoSuchElementException();
    }
    return elements.get(0);
  }

  @Override
  public E last() {
    if (isEmpty()) {
      throw new NoSuchElementException();
    }
    return elements.get(size() - 1);
  }

  @Override
  @CheckForNull
  public E lower(E element) {
    int index = headIndex(element, false) - 1;
    return (index == -1) ? null : elements.get(index);
  }

  @Override
  @CheckForNull
  public E floor(E element) {
    int index = headIndex(element, true) - 1;
    return (index == -1) ? null : elements.get(index);
  }

  @Override
  @CheckForNull
  public E ceiling(E element) {
    int index = tailIndex(element, true);
    return (index == size()) ? null : elements.get(index);
  }

  @Override
  @CheckForNull
  public E higher(E element) {
    int index = tailIndex(element, false);
    return (index == size()) ? null : elements.get(index);
  }

  @Override
  ImmutableSortedSet<E> headSetImpl(E toElement, boolean inclusive) {
    return getSubSet(0, headIndex(toElement, inclusive));
  }

  int headIndex(E toElement, boolean inclusive) {
    int index = Collections.binarySearch(elements, checkNotNull(toElement), comparator());
    if (index >= 0) {
      return inclusive ? index + 1 : index;
    } else {
      return ~index;
    }
  }

  @Override
  ImmutableSortedSet<E> subSetImpl(
      E fromElement, boolean fromInclusive, E toElement, boolean toInclusive) {
    return tailSetImpl(fromElement, fromInclusive).headSetImpl(toElement, toInclusive);
  }

  @Override
  ImmutableSortedSet<E> tailSetImpl(E fromElement, boolean inclusive) {
    return getSubSet(tailIndex(fromElement, inclusive), size());
  }

  int tailIndex(E fromElement, boolean inclusive) {
    int index = Collections.binarySearch(elements, checkNotNull(fromElement), comparator());
    if (index >= 0) {
      return inclusive ? index : index + 1;
    } else {
      return ~index;
    }
  }

  // Pretend the comparator can compare anything. If it turns out it can't
  // compare two elements, it'll throw a CCE. Only methods that are specified to
  // throw CCE should call this.
  @SuppressWarnings("unchecked")
  Comparator<Object> unsafeComparator() {
    return (Comparator<Object>) comparator;
  }

  RegularImmutableSortedSet<E> getSubSet(int newFromIndex, int newToIndex) {
    if (newFromIndex == 0 && newToIndex == size()) {
      return this;
    } else if (newFromIndex < newToIndex) {
      return new RegularImmutableSortedSet<E>(
          elements.subList(newFromIndex, newToIndex), comparator);
    } else {
      return emptySet(comparator);
    }
  }

  @Override
  int indexOf(@CheckForNull Object target) {
    if (target == null) {
      return -1;
    }
    int position;
    try {
      position = Collections.binarySearch(elements, target, unsafeComparator());
    } catch (ClassCastException e) {
      return -1;
    }
    return (position >= 0) ? position : -1;
  }

  @Override
  ImmutableList<E> createAsList() {
    return (size() <= 1) ? elements : new ImmutableSortedAsList<E>(this, elements);
  }

  @Override
  ImmutableSortedSet<E> createDescendingSet() {
    Comparator<? super E> reversedOrder = Collections.reverseOrder(comparator);
    return isEmpty()
        ? emptySet(reversedOrder)
        : new RegularImmutableSortedSet<E>(elements.reverse(), reversedOrder);
  }
}
