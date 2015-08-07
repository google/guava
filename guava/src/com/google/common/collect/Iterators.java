/*
 * Copyright (C) 2007 The Guava Authors
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
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.base.Predicates.equalTo;
import static com.google.common.base.Predicates.in;
import static com.google.common.base.Predicates.instanceOf;
import static com.google.common.base.Predicates.not;
import static com.google.common.collect.CollectPreconditions.checkRemove;

import com.google.common.annotations.Beta;
import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.base.Function;
import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import java.util.PriorityQueue;
import java.util.Queue;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nullable;

/**
 * This class contains static utility methods that operate on or return objects
 * of type {@link Iterator}. Except as noted, each method has a corresponding
 * {@link Iterable}-based method in the {@link Iterables} class.
 *
 * <p><i>Performance notes:</i> Unless otherwise noted, all of the iterators
 * produced in this class are <i>lazy</i>, which means that they only advance
 * the backing iteration when absolutely necessary.
 *
 * <p>See the Guava User Guide section on <a href=
 * "https://github.com/google/guava/wiki/CollectionUtilitiesExplained#iterables">
 * {@code Iterators}</a>.
 *
 * @author Kevin Bourrillion
 * @author Jared Levy
 * @since 2.0
 */
@GwtCompatible(emulated = true)
public final class Iterators {
  private Iterators() {}

  static final UnmodifiableListIterator<Object> EMPTY_LIST_ITERATOR =
      new UnmodifiableListIterator<Object>() {
        @Override
        public boolean hasNext() {
          return false;
        }

        @Override
        public Object next() {
          throw new NoSuchElementException();
        }

        @Override
        public boolean hasPrevious() {
          return false;
        }

        @Override
        public Object previous() {
          throw new NoSuchElementException();
        }

        @Override
        public int nextIndex() {
          return 0;
        }

        @Override
        public int previousIndex() {
          return -1;
        }
      };

  /**
   * Returns the empty iterator.
   *
   * <p>The {@link Iterable} equivalent of this method is {@link
   * ImmutableSet#of()}.
   *
   * @deprecated Use {@code ImmutableSet.<T>of().iterator()} instead; or for
   *     Java 7 or later, {@link Collections#emptyIterator}. This method is
   *     scheduled for removal in May 2016.
   */
  @Deprecated
  public static <T> UnmodifiableIterator<T> emptyIterator() {
    return emptyListIterator();
  }

  /**
   * Returns the empty iterator.
   *
   * <p>The {@link Iterable} equivalent of this method is {@link
   * ImmutableSet#of()}.
   */
  // Casting to any type is safe since there are no actual elements.
  @SuppressWarnings("unchecked")
  static <T> UnmodifiableListIterator<T> emptyListIterator() {
    return (UnmodifiableListIterator<T>) EMPTY_LIST_ITERATOR;
  }

  private static final Iterator<Object> EMPTY_MODIFIABLE_ITERATOR =
      new Iterator<Object>() {
        @Override
        public boolean hasNext() {
          return false;
        }

        @Override
        public Object next() {
          throw new NoSuchElementException();
        }

        @Override
        public void remove() {
          checkRemove(false);
        }
      };

  /**
   * Returns the empty {@code Iterator} that throws
   * {@link IllegalStateException} instead of
   * {@link UnsupportedOperationException} on a call to
   * {@link Iterator#remove()}.
   */
  // Casting to any type is safe since there are no actual elements.
  @SuppressWarnings("unchecked")
  static <T> Iterator<T> emptyModifiableIterator() {
    return (Iterator<T>) EMPTY_MODIFIABLE_ITERATOR;
  }

  /** Returns an unmodifiable view of {@code iterator}. */
  public static <T> UnmodifiableIterator<T> unmodifiableIterator(final Iterator<T> iterator) {
    checkNotNull(iterator);
    if (iterator instanceof UnmodifiableIterator) {
      return (UnmodifiableIterator<T>) iterator;
    }
    return new UnmodifiableIterator<T>() {
      @Override
      public boolean hasNext() {
        return iterator.hasNext();
      }

      @Override
      public T next() {
        return iterator.next();
      }
    };
  }

  /**
   * Simply returns its argument.
   *
   * @deprecated no need to use this
   * @since 10.0
   */
  @Deprecated
  public static <T> UnmodifiableIterator<T> unmodifiableIterator(UnmodifiableIterator<T> iterator) {
    return checkNotNull(iterator);
  }

  /**
   * Returns the number of elements remaining in {@code iterator}. The iterator
   * will be left exhausted: its {@code hasNext()} method will return
   * {@code false}.
   */
  public static int size(Iterator<?> iterator) {
    int count = 0;
    while (iterator.hasNext()) {
      iterator.next();
      count++;
    }
    return count;
  }

  /**
   * Returns {@code true} if {@code iterator} contains {@code element}.
   */
  public static boolean contains(Iterator<?> iterator, @Nullable Object element) {
    return any(iterator, equalTo(element));
  }

  /**
   * Traverses an iterator and removes every element that belongs to the
   * provided collection. The iterator will be left exhausted: its
   * {@code hasNext()} method will return {@code false}.
   *
   * @param removeFrom the iterator to (potentially) remove elements from
   * @param elementsToRemove the elements to remove
   * @return {@code true} if any element was removed from {@code iterator}
   */
  public static boolean removeAll(Iterator<?> removeFrom, Collection<?> elementsToRemove) {
    return removeIf(removeFrom, in(elementsToRemove));
  }

  /**
   * Removes every element that satisfies the provided predicate from the
   * iterator. The iterator will be left exhausted: its {@code hasNext()}
   * method will return {@code false}.
   *
   * @param removeFrom the iterator to (potentially) remove elements from
   * @param predicate a predicate that determines whether an element should
   *     be removed
   * @return {@code true} if any elements were removed from the iterator
   * @since 2.0
   */
  public static <T> boolean removeIf(Iterator<T> removeFrom, Predicate<? super T> predicate) {
    checkNotNull(predicate);
    boolean modified = false;
    while (removeFrom.hasNext()) {
      if (predicate.apply(removeFrom.next())) {
        removeFrom.remove();
        modified = true;
      }
    }
    return modified;
  }

  /**
   * Traverses an iterator and removes every element that does not belong to the
   * provided collection. The iterator will be left exhausted: its
   * {@code hasNext()} method will return {@code false}.
   *
   * @param removeFrom the iterator to (potentially) remove elements from
   * @param elementsToRetain the elements to retain
   * @return {@code true} if any element was removed from {@code iterator}
   */
  public static boolean retainAll(Iterator<?> removeFrom, Collection<?> elementsToRetain) {
    return removeIf(removeFrom, not(in(elementsToRetain)));
  }

  /**
   * Determines whether two iterators contain equal elements in the same order.
   * More specifically, this method returns {@code true} if {@code iterator1}
   * and {@code iterator2} contain the same number of elements and every element
   * of {@code iterator1} is equal to the corresponding element of
   * {@code iterator2}.
   *
   * <p>Note that this will modify the supplied iterators, since they will have
   * been advanced some number of elements forward.
   */
  public static boolean elementsEqual(Iterator<?> iterator1, Iterator<?> iterator2) {
    while (iterator1.hasNext()) {
      if (!iterator2.hasNext()) {
        return false;
      }
      Object o1 = iterator1.next();
      Object o2 = iterator2.next();
      if (!Objects.equal(o1, o2)) {
        return false;
      }
    }
    return !iterator2.hasNext();
  }

  /**
   * Returns a string representation of {@code iterator}, with the format
   * {@code [e1, e2, ..., en]}. The iterator will be left exhausted: its
   * {@code hasNext()} method will return {@code false}.
   */
  public static String toString(Iterator<?> iterator) {
    return Collections2.STANDARD_JOINER
        .appendTo(new StringBuilder().append('['), iterator)
        .append(']')
        .toString();
  }

  /**
   * Returns the single element contained in {@code iterator}.
   *
   * @throws NoSuchElementException if the iterator is empty
   * @throws IllegalArgumentException if the iterator contains multiple
   *     elements.  The state of the iterator is unspecified.
   */
  public static <T> T getOnlyElement(Iterator<T> iterator) {
    T first = iterator.next();
    if (!iterator.hasNext()) {
      return first;
    }

    StringBuilder sb = new StringBuilder();
    sb.append("expected one element but was: <" + first);
    for (int i = 0; i < 4 && iterator.hasNext(); i++) {
      sb.append(", " + iterator.next());
    }
    if (iterator.hasNext()) {
      sb.append(", ...");
    }
    sb.append('>');

    throw new IllegalArgumentException(sb.toString());
  }

  /**
   * Returns the single element contained in {@code iterator}, or {@code
   * defaultValue} if the iterator is empty.
   *
   * @throws IllegalArgumentException if the iterator contains multiple
   *     elements.  The state of the iterator is unspecified.
   */
  @Nullable
  public static <T> T getOnlyElement(Iterator<? extends T> iterator, @Nullable T defaultValue) {
    return iterator.hasNext() ? getOnlyElement(iterator) : defaultValue;
  }

  /**
   * Copies an iterator's elements into an array. The iterator will be left
   * exhausted: its {@code hasNext()} method will return {@code false}.
   *
   * @param iterator the iterator to copy
   * @param type the type of the elements
   * @return a newly-allocated array into which all the elements of the iterator
   *         have been copied
   */
  @GwtIncompatible("Array.newInstance(Class, int)")
  public static <T> T[] toArray(Iterator<? extends T> iterator, Class<T> type) {
    List<T> list = Lists.newArrayList(iterator);
    return Iterables.toArray(list, type);
  }

  /**
   * Adds all elements in {@code iterator} to {@code collection}. The iterator
   * will be left exhausted: its {@code hasNext()} method will return
   * {@code false}.
   *
   * @return {@code true} if {@code collection} was modified as a result of this
   *         operation
   */
  public static <T> boolean addAll(Collection<T> addTo, Iterator<? extends T> iterator) {
    checkNotNull(addTo);
    checkNotNull(iterator);
    boolean wasModified = false;
    while (iterator.hasNext()) {
      wasModified |= addTo.add(iterator.next());
    }
    return wasModified;
  }

  /**
   * Returns the number of elements in the specified iterator that equal the
   * specified object. The iterator will be left exhausted: its
   * {@code hasNext()} method will return {@code false}.
   *
   * @see Collections#frequency
   */
  public static int frequency(Iterator<?> iterator, @Nullable Object element) {
    return size(filter(iterator, equalTo(element)));
  }

  /**
   * Returns an iterator that cycles indefinitely over the elements of {@code
   * iterable}.
   *
   * <p>The returned iterator supports {@code remove()} if the provided iterator
   * does. After {@code remove()} is called, subsequent cycles omit the removed
   * element, which is no longer in {@code iterable}. The iterator's
   * {@code hasNext()} method returns {@code true} until {@code iterable} is
   * empty.
   *
   * <p><b>Warning:</b> Typical uses of the resulting iterator may produce an
   * infinite loop. You should use an explicit {@code break} or be certain that
   * you will eventually remove all the elements.
   */
  public static <T> Iterator<T> cycle(final Iterable<T> iterable) {
    checkNotNull(iterable);
    return new Iterator<T>() {
      Iterator<T> iterator = emptyModifiableIterator();

      @Override
      public boolean hasNext() {
        /*
         * Don't store a new Iterator until we know the user can't remove() the last returned
         * element anymore. Otherwise, when we remove from the old iterator, we may be invalidating
         * the new one. The result is a ConcurrentModificationException or other bad behavior.
         *
         * (If we decide that we really, really hate allocating two Iterators per cycle instead of
         * one, we can optimistically store the new Iterator and then be willing to throw it out if
         * the user calls remove().)
         */
        return iterator.hasNext() || iterable.iterator().hasNext();
      }

      @Override
      public T next() {
        if (!iterator.hasNext()) {
          iterator = iterable.iterator();
          if (!iterator.hasNext()) {
            throw new NoSuchElementException();
          }
        }
        return iterator.next();
      }

      @Override
      public void remove() {
        iterator.remove();
      }
    };
  }

  /**
   * Returns an iterator that cycles indefinitely over the provided elements.
   *
   * <p>The returned iterator supports {@code remove()}. After {@code remove()}
   * is called, subsequent cycles omit the removed
   * element, but {@code elements} does not change. The iterator's
   * {@code hasNext()} method returns {@code true} until all of the original
   * elements have been removed.
   *
   * <p><b>Warning:</b> Typical uses of the resulting iterator may produce an
   * infinite loop. You should use an explicit {@code break} or be certain that
   * you will eventually remove all the elements.
   */
  public static <T> Iterator<T> cycle(T... elements) {
    return cycle(Lists.newArrayList(elements));
  }

  /**
   * Combines two iterators into a single iterator. The returned iterator
   * iterates across the elements in {@code a}, followed by the elements in
   * {@code b}. The source iterators are not polled until necessary.
   *
   * <p>The returned iterator supports {@code remove()} when the corresponding
   * input iterator supports it.
   *
   * <p><b>Note:</b> the current implementation is not suitable for nested
   * concatenated iterators, i.e. the following should be avoided when in a loop:
   * {@code iterator = Iterators.concat(iterator, suffix);}, since iteration over the
   * resulting iterator has a cubic complexity to the depth of the nesting.
   */
  public static <T> Iterator<T> concat(Iterator<? extends T> a, Iterator<? extends T> b) {
    checkNotNull(a);
    checkNotNull(b);
    return concat(new ConsumingQueueIterator<Iterator<? extends T>>(a, b));
  }

  /**
   * Combines three iterators into a single iterator. The returned iterator
   * iterates across the elements in {@code a}, followed by the elements in
   * {@code b}, followed by the elements in {@code c}. The source iterators
   * are not polled until necessary.
   *
   * <p>The returned iterator supports {@code remove()} when the corresponding
   * input iterator supports it.
   *
   * <p><b>Note:</b> the current implementation is not suitable for nested
   * concatenated iterators, i.e. the following should be avoided when in a loop:
   * {@code iterator = Iterators.concat(iterator, suffix);}, since iteration over the
   * resulting iterator has a cubic complexity to the depth of the nesting.
   */
  public static <T> Iterator<T> concat(
      Iterator<? extends T> a, Iterator<? extends T> b, Iterator<? extends T> c) {
    checkNotNull(a);
    checkNotNull(b);
    checkNotNull(c);
    return concat(new ConsumingQueueIterator<Iterator<? extends T>>(a, b, c));
  }

  /**
   * Combines four iterators into a single iterator. The returned iterator
   * iterates across the elements in {@code a}, followed by the elements in
   * {@code b}, followed by the elements in {@code c}, followed by the elements
   * in {@code d}. The source iterators are not polled until necessary.
   *
   * <p>The returned iterator supports {@code remove()} when the corresponding
   * input iterator supports it.
   *
   * <p><b>Note:</b> the current implementation is not suitable for nested
   * concatenated iterators, i.e. the following should be avoided when in a loop:
   * {@code iterator = Iterators.concat(iterator, suffix);}, since iteration over the
   * resulting iterator has a cubic complexity to the depth of the nesting.
   */
  public static <T> Iterator<T> concat(
      Iterator<? extends T> a,
      Iterator<? extends T> b,
      Iterator<? extends T> c,
      Iterator<? extends T> d) {
    checkNotNull(a);
    checkNotNull(b);
    checkNotNull(c);
    checkNotNull(d);
    return concat(new ConsumingQueueIterator<Iterator<? extends T>>(a, b, c, d));
  }

  /**
   * Combines multiple iterators into a single iterator. The returned iterator
   * iterates across the elements of each iterator in {@code inputs}. The input
   * iterators are not polled until necessary.
   *
   * <p>The returned iterator supports {@code remove()} when the corresponding
   * input iterator supports it.
   *
   * <p><b>Note:</b> the current implementation is not suitable for nested
   * concatenated iterators, i.e. the following should be avoided when in a loop:
   * {@code iterator = Iterators.concat(iterator, suffix);}, since iteration over the
   * resulting iterator has a cubic complexity to the depth of the nesting.
   *
   * @throws NullPointerException if any of the provided iterators is null
   */
  public static <T> Iterator<T> concat(Iterator<? extends T>... inputs) {
    for (Iterator<? extends T> input : checkNotNull(inputs)) {
      checkNotNull(input);
    }
    return concat(new ConsumingQueueIterator<Iterator<? extends T>>(inputs));
  }

  /**
   * Combines multiple iterators into a single iterator. The returned iterator
   * iterates across the elements of each iterator in {@code inputs}. The input
   * iterators are not polled until necessary.
   *
   * <p>The returned iterator supports {@code remove()} when the corresponding
   * input iterator supports it. The methods of the returned iterator may throw
   * {@code NullPointerException} if any of the input iterators is null.
   *
   * <p><b>Note:</b> the current implementation is not suitable for nested
   * concatenated iterators, i.e. the following should be avoided when in a loop:
   * {@code iterator = Iterators.concat(iterator, suffix);}, since iteration over the
   * resulting iterator has a cubic complexity to the depth of the nesting.
   */
  public static <T> Iterator<T> concat(final Iterator<? extends Iterator<? extends T>> inputs) {
    checkNotNull(inputs);
    return new Iterator<T>() {
      Iterator<? extends T> current = emptyIterator();
      Iterator<? extends T> removeFrom;

      @Override
      public boolean hasNext() {
        // http://code.google.com/p/google-collections/issues/detail?id=151
        // current.hasNext() might be relatively expensive, worth minimizing.
        boolean currentHasNext;
        // checkNotNull eager for GWT
        // note: it must be here & not where 'current' is assigned,
        // because otherwise we'll have called inputs.next() before throwing
        // the first NPE, and the next time around we'll call inputs.next()
        // again, incorrectly moving beyond the error.
        while (!(currentHasNext = checkNotNull(current).hasNext()) && inputs.hasNext()) {
          current = inputs.next();
        }
        return currentHasNext;
      }

      @Override
      public T next() {
        if (!hasNext()) {
          throw new NoSuchElementException();
        }
        removeFrom = current;
        return current.next();
      }

      @Override
      public void remove() {
        checkRemove(removeFrom != null);
        removeFrom.remove();
        removeFrom = null;
      }
    };
  }

  /**
   * Divides an iterator into unmodifiable sublists of the given size (the final
   * list may be smaller). For example, partitioning an iterator containing
   * {@code [a, b, c, d, e]} with a partition size of 3 yields {@code
   * [[a, b, c], [d, e]]} -- an outer iterator containing two inner lists of
   * three and two elements, all in the original order.
   *
   * <p>The returned lists implement {@link java.util.RandomAccess}.
   *
   * @param iterator the iterator to return a partitioned view of
   * @param size the desired size of each partition (the last may be smaller)
   * @return an iterator of immutable lists containing the elements of {@code
   *     iterator} divided into partitions
   * @throws IllegalArgumentException if {@code size} is nonpositive
   */
  public static <T> UnmodifiableIterator<List<T>> partition(Iterator<T> iterator, int size) {
    return partitionImpl(iterator, size, false);
  }

  /**
   * Divides an iterator into unmodifiable sublists of the given size, padding
   * the final iterator with null values if necessary. For example, partitioning
   * an iterator containing {@code [a, b, c, d, e]} with a partition size of 3
   * yields {@code [[a, b, c], [d, e, null]]} -- an outer iterator containing
   * two inner lists of three elements each, all in the original order.
   *
   * <p>The returned lists implement {@link java.util.RandomAccess}.
   *
   * @param iterator the iterator to return a partitioned view of
   * @param size the desired size of each partition
   * @return an iterator of immutable lists containing the elements of {@code
   *     iterator} divided into partitions (the final iterable may have
   *     trailing null elements)
   * @throws IllegalArgumentException if {@code size} is nonpositive
   */
  public static <T> UnmodifiableIterator<List<T>> paddedPartition(Iterator<T> iterator, int size) {
    return partitionImpl(iterator, size, true);
  }

  private static <T> UnmodifiableIterator<List<T>> partitionImpl(
      final Iterator<T> iterator, final int size, final boolean pad) {
    checkNotNull(iterator);
    checkArgument(size > 0);
    return new UnmodifiableIterator<List<T>>() {
      @Override
      public boolean hasNext() {
        return iterator.hasNext();
      }

      @Override
      public List<T> next() {
        if (!hasNext()) {
          throw new NoSuchElementException();
        }
        Object[] array = new Object[size];
        int count = 0;
        for (; count < size && iterator.hasNext(); count++) {
          array[count] = iterator.next();
        }
        for (int i = count; i < size; i++) {
          array[i] = null; // for GWT
        }

        @SuppressWarnings("unchecked") // we only put Ts in it
        List<T> list = Collections.unmodifiableList((List<T>) Arrays.asList(array));
        return (pad || count == size) ? list : list.subList(0, count);
      }
    };
  }

  /**
   * Returns the elements of {@code unfiltered} that satisfy a predicate.
   */
  @CheckReturnValue
  public static <T> UnmodifiableIterator<T> filter(
      final Iterator<T> unfiltered, final Predicate<? super T> predicate) {
    checkNotNull(unfiltered);
    checkNotNull(predicate);
    return new AbstractIterator<T>() {
      @Override
      protected T computeNext() {
        while (unfiltered.hasNext()) {
          T element = unfiltered.next();
          if (predicate.apply(element)) {
            return element;
          }
        }
        return endOfData();
      }
    };
  }

  /**
   * Returns all instances of class {@code type} in {@code unfiltered}. The
   * returned iterator has elements whose class is {@code type} or a subclass of
   * {@code type}.
   *
   * @param unfiltered an iterator containing objects of any type
   * @param type the type of elements desired
   * @return an unmodifiable iterator containing all elements of the original
   *     iterator that were of the requested type
   */
  @SuppressWarnings("unchecked") // can cast to <T> because non-Ts are removed
  @GwtIncompatible("Class.isInstance")
  @CheckReturnValue
  public static <T> UnmodifiableIterator<T> filter(Iterator<?> unfiltered, Class<T> type) {
    return (UnmodifiableIterator<T>) filter(unfiltered, instanceOf(type));
  }

  /**
   * Returns {@code true} if one or more elements returned by {@code iterator}
   * satisfy the given predicate.
   */
  public static <T> boolean any(Iterator<T> iterator, Predicate<? super T> predicate) {
    return indexOf(iterator, predicate) != -1;
  }

  /**
   * Returns {@code true} if every element returned by {@code iterator}
   * satisfies the given predicate. If {@code iterator} is empty, {@code true}
   * is returned.
   */
  public static <T> boolean all(Iterator<T> iterator, Predicate<? super T> predicate) {
    checkNotNull(predicate);
    while (iterator.hasNext()) {
      T element = iterator.next();
      if (!predicate.apply(element)) {
        return false;
      }
    }
    return true;
  }

  /**
   * Returns the first element in {@code iterator} that satisfies the given
   * predicate; use this method only when such an element is known to exist. If
   * no such element is found, the iterator will be left exhausted: its {@code
   * hasNext()} method will return {@code false}. If it is possible that
   * <i>no</i> element will match, use {@link #tryFind} or {@link
   * #find(Iterator, Predicate, Object)} instead.
   *
   * @throws NoSuchElementException if no element in {@code iterator} matches
   *     the given predicate
   */
  public static <T> T find(Iterator<T> iterator, Predicate<? super T> predicate) {
    return filter(iterator, predicate).next();
  }

  /**
   * Returns the first element in {@code iterator} that satisfies the given
   * predicate. If no such element is found, {@code defaultValue} will be
   * returned from this method and the iterator will be left exhausted: its
   * {@code hasNext()} method will return {@code false}. Note that this can
   * usually be handled more naturally using {@code
   * tryFind(iterator, predicate).or(defaultValue)}.
   *
   * @since 7.0
   */
  @Nullable
  public static <T> T find(
      Iterator<? extends T> iterator, Predicate<? super T> predicate, @Nullable T defaultValue) {
    return getNext(filter(iterator, predicate), defaultValue);
  }

  /**
   * Returns an {@link Optional} containing the first element in {@code
   * iterator} that satisfies the given predicate, if such an element exists. If
   * no such element is found, an empty {@link Optional} will be returned from
   * this method and the iterator will be left exhausted: its {@code
   * hasNext()} method will return {@code false}.
   *
   * <p><b>Warning:</b> avoid using a {@code predicate} that matches {@code
   * null}. If {@code null} is matched in {@code iterator}, a
   * NullPointerException will be thrown.
   *
   * @since 11.0
   */
  public static <T> Optional<T> tryFind(Iterator<T> iterator, Predicate<? super T> predicate) {
    UnmodifiableIterator<T> filteredIterator = filter(iterator, predicate);
    return filteredIterator.hasNext()
        ? Optional.of(filteredIterator.next())
        : Optional.<T>absent();
  }

  /**
   * Returns the index in {@code iterator} of the first element that satisfies
   * the provided {@code predicate}, or {@code -1} if the Iterator has no such
   * elements.
   *
   * <p>More formally, returns the lowest index {@code i} such that
   * {@code predicate.apply(Iterators.get(iterator, i))} returns {@code true},
   * or {@code -1} if there is no such index.
   *
   * <p>If -1 is returned, the iterator will be left exhausted: its
   * {@code hasNext()} method will return {@code false}.  Otherwise,
   * the iterator will be set to the element which satisfies the
   * {@code predicate}.
   *
   * @since 2.0
   */
  public static <T> int indexOf(Iterator<T> iterator, Predicate<? super T> predicate) {
    checkNotNull(predicate, "predicate");
    for (int i = 0; iterator.hasNext(); i++) {
      T current = iterator.next();
      if (predicate.apply(current)) {
        return i;
      }
    }
    return -1;
  }

  /**
   * Returns an iterator that applies {@code function} to each element of {@code
   * fromIterator}.
   *
   * <p>The returned iterator supports {@code remove()} if the provided iterator
   * does. After a successful {@code remove()} call, {@code fromIterator} no
   * longer contains the corresponding element.
   */
  public static <F, T> Iterator<T> transform(
      final Iterator<F> fromIterator, final Function<? super F, ? extends T> function) {
    checkNotNull(function);
    return new TransformedIterator<F, T>(fromIterator) {
      @Override
      T transform(F from) {
        return function.apply(from);
      }
    };
  }

  /**
   * Advances {@code iterator} {@code position + 1} times, returning the
   * element at the {@code position}th position.
   *
   * @param position position of the element to return
   * @return the element at the specified position in {@code iterator}
   * @throws IndexOutOfBoundsException if {@code position} is negative or
   *     greater than or equal to the number of elements remaining in
   *     {@code iterator}
   */
  public static <T> T get(Iterator<T> iterator, int position) {
    checkNonnegative(position);
    int skipped = advance(iterator, position);
    if (!iterator.hasNext()) {
      throw new IndexOutOfBoundsException(
          "position ("
              + position
              + ") must be less than the number of elements that remained ("
              + skipped
              + ")");
    }
    return iterator.next();
  }

  static void checkNonnegative(int position) {
    if (position < 0) {
      throw new IndexOutOfBoundsException("position (" + position + ") must not be negative");
    }
  }

  /**
   * Advances {@code iterator} {@code position + 1} times, returning the
   * element at the {@code position}th position or {@code defaultValue}
   * otherwise.
   *
   * @param position position of the element to return
   * @param defaultValue the default value to return if the iterator is empty
   *     or if {@code position} is greater than the number of elements
   *     remaining in {@code iterator}
   * @return the element at the specified position in {@code iterator} or
   *     {@code defaultValue} if {@code iterator} produces fewer than
   *     {@code position + 1} elements.
   * @throws IndexOutOfBoundsException if {@code position} is negative
   * @since 4.0
   */
  @Nullable
  public static <T> T get(Iterator<? extends T> iterator, int position, @Nullable T defaultValue) {
    checkNonnegative(position);
    advance(iterator, position);
    return getNext(iterator, defaultValue);
  }

  /**
   * Returns the next element in {@code iterator} or {@code defaultValue} if
   * the iterator is empty.  The {@link Iterables} analog to this method is
   * {@link Iterables#getFirst}.
   *
   * @param defaultValue the default value to return if the iterator is empty
   * @return the next element of {@code iterator} or the default value
   * @since 7.0
   */
  @Nullable
  public static <T> T getNext(Iterator<? extends T> iterator, @Nullable T defaultValue) {
    return iterator.hasNext() ? iterator.next() : defaultValue;
  }

  /**
   * Advances {@code iterator} to the end, returning the last element.
   *
   * @return the last element of {@code iterator}
   * @throws NoSuchElementException if the iterator is empty
   */
  public static <T> T getLast(Iterator<T> iterator) {
    while (true) {
      T current = iterator.next();
      if (!iterator.hasNext()) {
        return current;
      }
    }
  }

  /**
   * Advances {@code iterator} to the end, returning the last element or
   * {@code defaultValue} if the iterator is empty.
   *
   * @param defaultValue the default value to return if the iterator is empty
   * @return the last element of {@code iterator}
   * @since 3.0
   */
  @Nullable
  public static <T> T getLast(Iterator<? extends T> iterator, @Nullable T defaultValue) {
    return iterator.hasNext() ? getLast(iterator) : defaultValue;
  }

  /**
   * Calls {@code next()} on {@code iterator}, either {@code numberToAdvance} times
   * or until {@code hasNext()} returns {@code false}, whichever comes first.
   *
   * @return the number of elements the iterator was advanced
   * @since 13.0 (since 3.0 as {@code Iterators.skip})
   */
  public static int advance(Iterator<?> iterator, int numberToAdvance) {
    checkNotNull(iterator);
    checkArgument(numberToAdvance >= 0, "numberToAdvance must be nonnegative");

    int i;
    for (i = 0; i < numberToAdvance && iterator.hasNext(); i++) {
      iterator.next();
    }
    return i;
  }

  /**
   * Creates an iterator returning the first {@code limitSize} elements of the
   * given iterator. If the original iterator does not contain that many
   * elements, the returned iterator will have the same behavior as the original
   * iterator. The returned iterator supports {@code remove()} if the original
   * iterator does.
   *
   * @param iterator the iterator to limit
   * @param limitSize the maximum number of elements in the returned iterator
   * @throws IllegalArgumentException if {@code limitSize} is negative
   * @since 3.0
   */
  public static <T> Iterator<T> limit(final Iterator<T> iterator, final int limitSize) {
    checkNotNull(iterator);
    checkArgument(limitSize >= 0, "limit is negative");
    return new Iterator<T>() {
      private int count;

      @Override
      public boolean hasNext() {
        return count < limitSize && iterator.hasNext();
      }

      @Override
      public T next() {
        if (!hasNext()) {
          throw new NoSuchElementException();
        }
        count++;
        return iterator.next();
      }

      @Override
      public void remove() {
        iterator.remove();
      }
    };
  }

  /**
   * Returns a view of the supplied {@code iterator} that removes each element
   * from the supplied {@code iterator} as it is returned.
   *
   * <p>The provided iterator must support {@link Iterator#remove()} or
   * else the returned iterator will fail on the first call to {@code
   * next}.
   *
   * @param iterator the iterator to remove and return elements from
   * @return an iterator that removes and returns elements from the
   *     supplied iterator
   * @since 2.0
   */
  public static <T> Iterator<T> consumingIterator(final Iterator<T> iterator) {
    checkNotNull(iterator);
    return new UnmodifiableIterator<T>() {
      @Override
      public boolean hasNext() {
        return iterator.hasNext();
      }

      @Override
      public T next() {
        T next = iterator.next();
        iterator.remove();
        return next;
      }

      @Override
      public String toString() {
        return "Iterators.consumingIterator(...)";
      }
    };
  }

  /**
   * Deletes and returns the next value from the iterator, or returns
   * {@code null} if there is no such value.
   */
  @Nullable
  static <T> T pollNext(Iterator<T> iterator) {
    if (iterator.hasNext()) {
      T result = iterator.next();
      iterator.remove();
      return result;
    } else {
      return null;
    }
  }

  // Methods only in Iterators, not in Iterables

  /**
   * Clears the iterator using its remove method.
   */
  static void clear(Iterator<?> iterator) {
    checkNotNull(iterator);
    while (iterator.hasNext()) {
      iterator.next();
      iterator.remove();
    }
  }

  /**
   * Returns an iterator containing the elements of {@code array} in order. The
   * returned iterator is a view of the array; subsequent changes to the array
   * will be reflected in the iterator.
   *
   * <p><b>Note:</b> It is often preferable to represent your data using a
   * collection type, for example using {@link Arrays#asList(Object[])}, making
   * this method unnecessary.
   *
   * <p>The {@code Iterable} equivalent of this method is either {@link
   * Arrays#asList(Object[])}, {@link ImmutableList#copyOf(Object[])}},
   * or {@link ImmutableList#of}.
   */
  public static <T> UnmodifiableIterator<T> forArray(final T... array) {
    return forArray(array, 0, array.length, 0);
  }

  /**
   * Returns a list iterator containing the elements in the specified range of
   * {@code array} in order, starting at the specified index.
   *
   * <p>The {@code Iterable} equivalent of this method is {@code
   * Arrays.asList(array).subList(offset, offset + length).listIterator(index)}.
   */
  static <T> UnmodifiableListIterator<T> forArray(
      final T[] array, final int offset, int length, int index) {
    checkArgument(length >= 0);
    int end = offset + length;

    // Technically we should give a slightly more descriptive error on overflow
    Preconditions.checkPositionIndexes(offset, end, array.length);
    Preconditions.checkPositionIndex(index, length);
    if (length == 0) {
      return emptyListIterator();
    }

    /*
     * We can't use call the two-arg constructor with arguments (offset, end)
     * because the returned Iterator is a ListIterator that may be moved back
     * past the beginning of the iteration.
     */
    return new AbstractIndexedListIterator<T>(length, index) {
      @Override
      protected T get(int index) {
        return array[offset + index];
      }
    };
  }

  /**
   * Returns an iterator containing only {@code value}.
   *
   * <p>The {@link Iterable} equivalent of this method is {@link
   * Collections#singleton}.
   */
  public static <T> UnmodifiableIterator<T> singletonIterator(@Nullable final T value) {
    return new UnmodifiableIterator<T>() {
      boolean done;

      @Override
      public boolean hasNext() {
        return !done;
      }

      @Override
      public T next() {
        if (done) {
          throw new NoSuchElementException();
        }
        done = true;
        return value;
      }
    };
  }

  /**
   * Adapts an {@code Enumeration} to the {@code Iterator} interface.
   *
   * <p>This method has no equivalent in {@link Iterables} because viewing an
   * {@code Enumeration} as an {@code Iterable} is impossible. However, the
   * contents can be <i>copied</i> into a collection using {@link
   * Collections#list}.
   */
  public static <T> UnmodifiableIterator<T> forEnumeration(final Enumeration<T> enumeration) {
    checkNotNull(enumeration);
    return new UnmodifiableIterator<T>() {
      @Override
      public boolean hasNext() {
        return enumeration.hasMoreElements();
      }

      @Override
      public T next() {
        return enumeration.nextElement();
      }
    };
  }

  /**
   * Adapts an {@code Iterator} to the {@code Enumeration} interface.
   *
   * <p>The {@code Iterable} equivalent of this method is either {@link
   * Collections#enumeration} (if you have a {@link Collection}), or
   * {@code Iterators.asEnumeration(collection.iterator())}.
   */
  public static <T> Enumeration<T> asEnumeration(final Iterator<T> iterator) {
    checkNotNull(iterator);
    return new Enumeration<T>() {
      @Override
      public boolean hasMoreElements() {
        return iterator.hasNext();
      }

      @Override
      public T nextElement() {
        return iterator.next();
      }
    };
  }

  /**
   * Implementation of PeekingIterator that avoids peeking unless necessary.
   */
  private static class PeekingImpl<E> implements PeekingIterator<E> {

    private final Iterator<? extends E> iterator;
    private boolean hasPeeked;
    private E peekedElement;

    public PeekingImpl(Iterator<? extends E> iterator) {
      this.iterator = checkNotNull(iterator);
    }

    @Override
    public boolean hasNext() {
      return hasPeeked || iterator.hasNext();
    }

    @Override
    public E next() {
      if (!hasPeeked) {
        return iterator.next();
      }
      E result = peekedElement;
      hasPeeked = false;
      peekedElement = null;
      return result;
    }

    @Override
    public void remove() {
      checkState(!hasPeeked, "Can't remove after you've peeked at next");
      iterator.remove();
    }

    @Override
    public E peek() {
      if (!hasPeeked) {
        peekedElement = iterator.next();
        hasPeeked = true;
      }
      return peekedElement;
    }
  }

  /**
   * Returns a {@code PeekingIterator} backed by the given iterator.
   *
   * <p>Calls to the {@code peek} method with no intervening calls to {@code
   * next} do not affect the iteration, and hence return the same object each
   * time. A subsequent call to {@code next} is guaranteed to return the same
   * object again. For example: <pre>   {@code
   *
   *   PeekingIterator<String> peekingIterator =
   *       Iterators.peekingIterator(Iterators.forArray("a", "b"));
   *   String a1 = peekingIterator.peek(); // returns "a"
   *   String a2 = peekingIterator.peek(); // also returns "a"
   *   String a3 = peekingIterator.next(); // also returns "a"}</pre>
   *
   * <p>Any structural changes to the underlying iteration (aside from those
   * performed by the iterator's own {@link PeekingIterator#remove()} method)
   * will leave the iterator in an undefined state.
   *
   * <p>The returned iterator does not support removal after peeking, as
   * explained by {@link PeekingIterator#remove()}.
   *
   * <p>Note: If the given iterator is already a {@code PeekingIterator},
   * it <i>might</i> be returned to the caller, although this is neither
   * guaranteed to occur nor required to be consistent.  For example, this
   * method <i>might</i> choose to pass through recognized implementations of
   * {@code PeekingIterator} when the behavior of the implementation is
   * known to meet the contract guaranteed by this method.
   *
   * <p>There is no {@link Iterable} equivalent to this method, so use this
   * method to wrap each individual iterator as it is generated.
   *
   * @param iterator the backing iterator. The {@link PeekingIterator} assumes
   *     ownership of this iterator, so users should cease making direct calls
   *     to it after calling this method.
   * @return a peeking iterator backed by that iterator. Apart from the
   *     additional {@link PeekingIterator#peek()} method, this iterator behaves
   *     exactly the same as {@code iterator}.
   */
  public static <T> PeekingIterator<T> peekingIterator(Iterator<? extends T> iterator) {
    if (iterator instanceof PeekingImpl) {
      // Safe to cast <? extends T> to <T> because PeekingImpl only uses T
      // covariantly (and cannot be subclassed to add non-covariant uses).
      @SuppressWarnings("unchecked")
      PeekingImpl<T> peeking = (PeekingImpl<T>) iterator;
      return peeking;
    }
    return new PeekingImpl<T>(iterator);
  }

  /**
   * Simply returns its argument.
   *
   * @deprecated no need to use this
   * @since 10.0
   */
  @Deprecated
  public static <T> PeekingIterator<T> peekingIterator(PeekingIterator<T> iterator) {
    return checkNotNull(iterator);
  }

  /**
   * Returns an iterator over the merged contents of all given
   * {@code iterators}, traversing every element of the input iterators.
   * Equivalent entries will not be de-duplicated.
   *
   * <p>Callers must ensure that the source {@code iterators} are in
   * non-descending order as this method does not sort its input.
   *
   * <p>For any equivalent elements across all {@code iterators}, it is
   * undefined which element is returned first.
   *
   * @since 11.0
   */
  @Beta
  public static <T> UnmodifiableIterator<T> mergeSorted(
      Iterable<? extends Iterator<? extends T>> iterators, Comparator<? super T> comparator) {
    checkNotNull(iterators, "iterators");
    checkNotNull(comparator, "comparator");

    return new MergingIterator<T>(iterators, comparator);
  }

  /**
   * An iterator that performs a lazy N-way merge, calculating the next value
   * each time the iterator is polled. This amortizes the sorting cost over the
   * iteration and requires less memory than sorting all elements at once.
   *
   * <p>Retrieving a single element takes approximately O(log(M)) time, where M
   * is the number of iterators. (Retrieving all elements takes approximately
   * O(N*log(M)) time, where N is the total number of elements.)
   */
  private static class MergingIterator<T> extends UnmodifiableIterator<T> {
    final Queue<PeekingIterator<T>> queue;

    public MergingIterator(
        Iterable<? extends Iterator<? extends T>> iterators,
        final Comparator<? super T> itemComparator) {
      // A comparator that's used by the heap, allowing the heap
      // to be sorted based on the top of each iterator.
      Comparator<PeekingIterator<T>> heapComparator =
          new Comparator<PeekingIterator<T>>() {
            @Override
            public int compare(PeekingIterator<T> o1, PeekingIterator<T> o2) {
              return itemComparator.compare(o1.peek(), o2.peek());
            }
          };

      queue = new PriorityQueue<PeekingIterator<T>>(2, heapComparator);

      for (Iterator<? extends T> iterator : iterators) {
        if (iterator.hasNext()) {
          queue.add(Iterators.peekingIterator(iterator));
        }
      }
    }

    @Override
    public boolean hasNext() {
      return !queue.isEmpty();
    }

    @Override
    public T next() {
      PeekingIterator<T> nextIter = queue.remove();
      T next = nextIter.next();
      if (nextIter.hasNext()) {
        queue.add(nextIter);
      }
      return next;
    }
  }

  /**
   * Used to avoid http://bugs.sun.com/view_bug.do?bug_id=6558557
   */
  static <T> ListIterator<T> cast(Iterator<T> iterator) {
    return (ListIterator<T>) iterator;
  }
}
