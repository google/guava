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
import static com.google.common.base.Predicates.instanceOf;
import static com.google.common.collect.CollectPreconditions.checkRemove;
import static com.google.common.collect.NullnessCasts.uncheckedCastNullableTToT;
import static java.util.Objects.requireNonNull;

import com.google.common.annotations.Beta;
import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.base.Function;
import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.primitives.Ints;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import java.util.PriorityQueue;
import java.util.Queue;
import javax.annotation.CheckForNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * This class contains static utility methods that operate on or return objects of type {@link
 * Iterator}. Except as noted, each method has a corresponding {@link Iterable}-based method in the
 * {@link Iterables} class.
 *
 * <p><i>Performance notes:</i> Unless otherwise noted, all of the iterators produced in this class
 * are <i>lazy</i>, which means that they only advance the backing iteration when absolutely
 * necessary.
 *
 * <p>See the Guava User Guide section on <a href=
 * "https://github.com/google/guava/wiki/CollectionUtilitiesExplained#iterables">{@code
 * Iterators}</a>.
 *
 * @author Kevin Bourrillion
 * @author Jared Levy
 * @since 2.0
 */
@GwtCompatible(emulated = true)
@ElementTypesAreNonnullByDefault
public final class Iterators {
  private Iterators() {}

  /**
   * Returns the empty iterator.
   *
   * <p>The {@link Iterable} equivalent of this method is {@link ImmutableSet#of()}.
   */
  static <T extends @Nullable Object> UnmodifiableIterator<T> emptyIterator() {
    return emptyListIterator();
  }

  /**
   * Returns the empty iterator.
   *
   * <p>The {@link Iterable} equivalent of this method is {@link ImmutableSet#of()}.
   */
  // Casting to any type is safe since there are no actual elements.
  @SuppressWarnings("unchecked")
  static <T extends @Nullable Object> UnmodifiableListIterator<T> emptyListIterator() {
    return (UnmodifiableListIterator<T>) ArrayItr.EMPTY;
  }

  /**
   * This is an enum singleton rather than an anonymous class so ProGuard can figure out it's only
   * referenced by emptyModifiableIterator().
   */
  private enum EmptyModifiableIterator implements Iterator<Object> {
    INSTANCE;

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
  }

  /**
   * Returns the empty {@code Iterator} that throws {@link IllegalStateException} instead of {@link
   * UnsupportedOperationException} on a call to {@link Iterator#remove()}.
   */
  // Casting to any type is safe since there are no actual elements.
  @SuppressWarnings("unchecked")
  static <T extends @Nullable Object> Iterator<T> emptyModifiableIterator() {
    return (Iterator<T>) EmptyModifiableIterator.INSTANCE;
  }

  /** Returns an unmodifiable view of {@code iterator}. */
  public static <T extends @Nullable Object> UnmodifiableIterator<T> unmodifiableIterator(
      Iterator<? extends T> iterator) {
    checkNotNull(iterator);
    if (iterator instanceof UnmodifiableIterator) {
      @SuppressWarnings("unchecked") // Since it's unmodifiable, the covariant cast is safe
      UnmodifiableIterator<T> result = (UnmodifiableIterator<T>) iterator;
      return result;
    }
    return new UnmodifiableIterator<T>() {
      @Override
      public boolean hasNext() {
        return iterator.hasNext();
      }

      @Override
      @ParametricNullness
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
  public static <T extends @Nullable Object> UnmodifiableIterator<T> unmodifiableIterator(
      UnmodifiableIterator<T> iterator) {
    return checkNotNull(iterator);
  }

  /**
   * Returns the number of elements remaining in {@code iterator}. The iterator will be left
   * exhausted: its {@code hasNext()} method will return {@code false}.
   */
  public static int size(Iterator<?> iterator) {
    long count = 0L;
    while (iterator.hasNext()) {
      iterator.next();
      count++;
    }
    return Ints.saturatedCast(count);
  }

  /** Returns {@code true} if {@code iterator} contains {@code element}. */
  public static boolean contains(Iterator<?> iterator, @CheckForNull Object element) {
    if (element == null) {
      while (iterator.hasNext()) {
        if (iterator.next() == null) {
          return true;
        }
      }
    } else {
      while (iterator.hasNext()) {
        if (element.equals(iterator.next())) {
          return true;
        }
      }
    }
    return false;
  }

  /**
   * Traverses an iterator and removes every element that belongs to the provided collection. The
   * iterator will be left exhausted: its {@code hasNext()} method will return {@code false}.
   *
   * @param removeFrom the iterator to (potentially) remove elements from
   * @param elementsToRemove the elements to remove
   * @return {@code true} if any element was removed from {@code iterator}
   */
  @CanIgnoreReturnValue
  public static boolean removeAll(Iterator<?> removeFrom, Collection<?> elementsToRemove) {
    checkNotNull(elementsToRemove);
    boolean result = false;
    while (removeFrom.hasNext()) {
      if (elementsToRemove.contains(removeFrom.next())) {
        removeFrom.remove();
        result = true;
      }
    }
    return result;
  }

  /**
   * Removes every element that satisfies the provided predicate from the iterator. The iterator
   * will be left exhausted: its {@code hasNext()} method will return {@code false}.
   *
   * @param removeFrom the iterator to (potentially) remove elements from
   * @param predicate a predicate that determines whether an element should be removed
   * @return {@code true} if any elements were removed from the iterator
   * @since 2.0
   */
  @CanIgnoreReturnValue
  public static <T extends @Nullable Object> boolean removeIf(
      Iterator<T> removeFrom, Predicate<? super T> predicate) {
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
   * Traverses an iterator and removes every element that does not belong to the provided
   * collection. The iterator will be left exhausted: its {@code hasNext()} method will return
   * {@code false}.
   *
   * @param removeFrom the iterator to (potentially) remove elements from
   * @param elementsToRetain the elements to retain
   * @return {@code true} if any element was removed from {@code iterator}
   */
  @CanIgnoreReturnValue
  public static boolean retainAll(Iterator<?> removeFrom, Collection<?> elementsToRetain) {
    checkNotNull(elementsToRetain);
    boolean result = false;
    while (removeFrom.hasNext()) {
      if (!elementsToRetain.contains(removeFrom.next())) {
        removeFrom.remove();
        result = true;
      }
    }
    return result;
  }

  /**
   * Determines whether two iterators contain equal elements in the same order. More specifically,
   * this method returns {@code true} if {@code iterator1} and {@code iterator2} contain the same
   * number of elements and every element of {@code iterator1} is equal to the corresponding element
   * of {@code iterator2}.
   *
   * <p>Note that this will modify the supplied iterators, since they will have been advanced some
   * number of elements forward.
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
   * Returns a string representation of {@code iterator}, with the format {@code [e1, e2, ..., en]}.
   * The iterator will be left exhausted: its {@code hasNext()} method will return {@code false}.
   */
  public static String toString(Iterator<?> iterator) {
    StringBuilder sb = new StringBuilder().append('[');
    boolean first = true;
    while (iterator.hasNext()) {
      if (!first) {
        sb.append(", ");
      }
      first = false;
      sb.append(iterator.next());
    }
    return sb.append(']').toString();
  }

  /**
   * Returns the single element contained in {@code iterator}.
   *
   * @throws NoSuchElementException if the iterator is empty
   * @throws IllegalArgumentException if the iterator contains multiple elements. The state of the
   *     iterator is unspecified.
   */
  @ParametricNullness
  public static <T extends @Nullable Object> T getOnlyElement(Iterator<T> iterator) {
    T first = iterator.next();
    if (!iterator.hasNext()) {
      return first;
    }

    StringBuilder sb = new StringBuilder().append("expected one element but was: <").append(first);
    for (int i = 0; i < 4 && iterator.hasNext(); i++) {
      sb.append(", ").append(iterator.next());
    }
    if (iterator.hasNext()) {
      sb.append(", ...");
    }
    sb.append('>');

    throw new IllegalArgumentException(sb.toString());
  }

  /**
   * Returns the single element contained in {@code iterator}, or {@code defaultValue} if the
   * iterator is empty.
   *
   * @throws IllegalArgumentException if the iterator contains multiple elements. The state of the
   *     iterator is unspecified.
   */
  @ParametricNullness
  public static <T extends @Nullable Object> T getOnlyElement(
      Iterator<? extends T> iterator, @ParametricNullness T defaultValue) {
    return iterator.hasNext() ? getOnlyElement(iterator) : defaultValue;
  }

  /**
   * Copies an iterator's elements into an array. The iterator will be left exhausted: its {@code
   * hasNext()} method will return {@code false}.
   *
   * @param iterator the iterator to copy
   * @param type the type of the elements
   * @return a newly-allocated array into which all the elements of the iterator have been copied
   */
  @GwtIncompatible // Array.newInstance(Class, int)
  // For discussion of this signature, see the corresponding overload of *Iterables*.toArray.
  public static <T> @Nullable T[] toArray(Iterator<? extends @Nullable T> iterator, Class<T> type) {
    List<@Nullable T> list = Lists.newArrayList(iterator);
    return Iterables.toArray(list, type);
  }

  /**
   * Adds all elements in {@code iterator} to {@code collection}. The iterator will be left
   * exhausted: its {@code hasNext()} method will return {@code false}.
   *
   * @return {@code true} if {@code collection} was modified as a result of this operation
   */
  @CanIgnoreReturnValue
  public static <T extends @Nullable Object> boolean addAll(
      Collection<T> addTo, Iterator<? extends T> iterator) {
    checkNotNull(addTo);
    checkNotNull(iterator);
    boolean wasModified = false;
    while (iterator.hasNext()) {
      wasModified |= addTo.add(iterator.next());
    }
    return wasModified;
  }

  /**
   * Returns the number of elements in the specified iterator that equal the specified object. The
   * iterator will be left exhausted: its {@code hasNext()} method will return {@code false}.
   *
   * @see Collections#frequency
   */
  public static int frequency(Iterator<?> iterator, @CheckForNull Object element) {
    int count = 0;
    while (contains(iterator, element)) {
      // Since it lives in the same class, we know contains gets to the element and then stops,
      // though that isn't currently publicly documented.
      count++;
    }
    return count;
  }

  /**
   * Returns an iterator that cycles indefinitely over the elements of {@code iterable}.
   *
   * <p>The returned iterator supports {@code remove()} if the provided iterator does. After {@code
   * remove()} is called, subsequent cycles omit the removed element, which is no longer in {@code
   * iterable}. The iterator's {@code hasNext()} method returns {@code true} until {@code iterable}
   * is empty.
   *
   * <p><b>Warning:</b> Typical uses of the resulting iterator may produce an infinite loop. You
   * should use an explicit {@code break} or be certain that you will eventually remove all the
   * elements.
   */
  public static <T extends @Nullable Object> Iterator<T> cycle(Iterable<T> iterable) {
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
      @ParametricNullness
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
   * <p>The returned iterator supports {@code remove()}. After {@code remove()} is called,
   * subsequent cycles omit the removed element, but {@code elements} does not change. The
   * iterator's {@code hasNext()} method returns {@code true} until all of the original elements
   * have been removed.
   *
   * <p><b>Warning:</b> Typical uses of the resulting iterator may produce an infinite loop. You
   * should use an explicit {@code break} or be certain that you will eventually remove all the
   * elements.
   */
  @SafeVarargs
  public static <T extends @Nullable Object> Iterator<T> cycle(T... elements) {
    return cycle(Lists.newArrayList(elements));
  }

  /**
   * Returns an Iterator that walks the specified array, nulling out elements behind it. This can
   * avoid memory leaks when an element is no longer necessary.
   *
   * <p>This method accepts an array with element type {@code @Nullable T}, but callers must pass an
   * array whose contents are initially non-null. The {@code @Nullable} annotation indicates that
   * this method will write nulls into the array during iteration.
   *
   * <p>This is mainly just to avoid the intermediate ArrayDeque in ConsumingQueueIterator.
   */
  private static <I extends Iterator<?>> Iterator<I> consumingForArray(@Nullable I... elements) {
    return new UnmodifiableIterator<I>() {
      int index = 0;

      @Override
      public boolean hasNext() {
        return index < elements.length;
      }

      @Override
      public I next() {
        if (!hasNext()) {
          throw new NoSuchElementException();
        }
        /*
         * requireNonNull is safe because our callers always pass non-null arguments. Each element
         * of the array becomes null only when we iterate past it and then clear it.
         */
        I result = requireNonNull(elements[index]);
        elements[index] = null;
        index++;
        return result;
      }
    };
  }

  /**
   * Combines two iterators into a single iterator. The returned iterator iterates across the
   * elements in {@code a}, followed by the elements in {@code b}. The source iterators are not
   * polled until necessary.
   *
   * <p>The returned iterator supports {@code remove()} when the corresponding input iterator
   * supports it.
   */
  public static <T extends @Nullable Object> Iterator<T> concat(
      Iterator<? extends T> a, Iterator<? extends T> b) {
    checkNotNull(a);
    checkNotNull(b);
    return concat(consumingForArray(a, b));
  }

  /**
   * Combines three iterators into a single iterator. The returned iterator iterates across the
   * elements in {@code a}, followed by the elements in {@code b}, followed by the elements in
   * {@code c}. The source iterators are not polled until necessary.
   *
   * <p>The returned iterator supports {@code remove()} when the corresponding input iterator
   * supports it.
   */
  public static <T extends @Nullable Object> Iterator<T> concat(
      Iterator<? extends T> a, Iterator<? extends T> b, Iterator<? extends T> c) {
    checkNotNull(a);
    checkNotNull(b);
    checkNotNull(c);
    return concat(consumingForArray(a, b, c));
  }

  /**
   * Combines four iterators into a single iterator. The returned iterator iterates across the
   * elements in {@code a}, followed by the elements in {@code b}, followed by the elements in
   * {@code c}, followed by the elements in {@code d}. The source iterators are not polled until
   * necessary.
   *
   * <p>The returned iterator supports {@code remove()} when the corresponding input iterator
   * supports it.
   */
  public static <T extends @Nullable Object> Iterator<T> concat(
      Iterator<? extends T> a,
      Iterator<? extends T> b,
      Iterator<? extends T> c,
      Iterator<? extends T> d) {
    checkNotNull(a);
    checkNotNull(b);
    checkNotNull(c);
    checkNotNull(d);
    return concat(consumingForArray(a, b, c, d));
  }

  /**
   * Combines multiple iterators into a single iterator. The returned iterator iterates across the
   * elements of each iterator in {@code inputs}. The input iterators are not polled until
   * necessary.
   *
   * <p>The returned iterator supports {@code remove()} when the corresponding input iterator
   * supports it.
   *
   * @throws NullPointerException if any of the provided iterators is null
   */
  public static <T extends @Nullable Object> Iterator<T> concat(Iterator<? extends T>... inputs) {
    return concatNoDefensiveCopy(Arrays.copyOf(inputs, inputs.length));
  }

  /**
   * Combines multiple iterators into a single iterator. The returned iterator iterates across the
   * elements of each iterator in {@code inputs}. The input iterators are not polled until
   * necessary.
   *
   * <p>The returned iterator supports {@code remove()} when the corresponding input iterator
   * supports it. The methods of the returned iterator may throw {@code NullPointerException} if any
   * of the input iterators is null.
   */
  public static <T extends @Nullable Object> Iterator<T> concat(
      Iterator<? extends Iterator<? extends T>> inputs) {
    return new ConcatenatedIterator<>(inputs);
  }

  /** Concats a varargs array of iterators without making a defensive copy of the array. */
  static <T extends @Nullable Object> Iterator<T> concatNoDefensiveCopy(
      Iterator<? extends T>... inputs) {
    for (Iterator<? extends T> input : checkNotNull(inputs)) {
      checkNotNull(input);
    }
    return concat(consumingForArray(inputs));
  }

  /**
   * Divides an iterator into unmodifiable sublists of the given size (the final list may be
   * smaller). For example, partitioning an iterator containing {@code [a, b, c, d, e]} with a
   * partition size of 3 yields {@code [[a, b, c], [d, e]]} -- an outer iterator containing two
   * inner lists of three and two elements, all in the original order.
   *
   * <p>The returned lists implement {@link java.util.RandomAccess}.
   *
   * <p><b>Note:</b> The current implementation eagerly allocates storage for {@code size} elements.
   * As a consequence, passing values like {@code Integer.MAX_VALUE} can lead to {@link
   * OutOfMemoryError}.
   *
   * @param iterator the iterator to return a partitioned view of
   * @param size the desired size of each partition (the last may be smaller)
   * @return an iterator of immutable lists containing the elements of {@code iterator} divided into
   *     partitions
   * @throws IllegalArgumentException if {@code size} is nonpositive
   */
  public static <T extends @Nullable Object> UnmodifiableIterator<List<T>> partition(
      Iterator<T> iterator, int size) {
    return partitionImpl(iterator, size, false);
  }

  /**
   * Divides an iterator into unmodifiable sublists of the given size, padding the final iterator
   * with null values if necessary. For example, partitioning an iterator containing {@code [a, b,
   * c, d, e]} with a partition size of 3 yields {@code [[a, b, c], [d, e, null]]} -- an outer
   * iterator containing two inner lists of three elements each, all in the original order.
   *
   * <p>The returned lists implement {@link java.util.RandomAccess}.
   *
   * @param iterator the iterator to return a partitioned view of
   * @param size the desired size of each partition
   * @return an iterator of immutable lists containing the elements of {@code iterator} divided into
   *     partitions (the final iterable may have trailing null elements)
   * @throws IllegalArgumentException if {@code size} is nonpositive
   */
  public static <T extends @Nullable Object>
      UnmodifiableIterator<List<@Nullable T>> paddedPartition(Iterator<T> iterator, int size) {
    return partitionImpl(iterator, size, true);
  }

  private static <T extends @Nullable Object> UnmodifiableIterator<List<@Nullable T>> partitionImpl(
      Iterator<T> iterator, int size, boolean pad) {
    checkNotNull(iterator);
    checkArgument(size > 0);
    return new UnmodifiableIterator<List<@Nullable T>>() {
      @Override
      public boolean hasNext() {
        return iterator.hasNext();
      }

      @Override
      public List<@Nullable T> next() {
        if (!hasNext()) {
          throw new NoSuchElementException();
        }
        @SuppressWarnings("unchecked") // we only put Ts in it
        @Nullable
        T[] array = (@Nullable T[]) new Object[size];
        int count = 0;
        for (; count < size && iterator.hasNext(); count++) {
          array[count] = iterator.next();
        }
        for (int i = count; i < size; i++) {
          array[i] = null; // for GWT
        }

        List<@Nullable T> list = Collections.unmodifiableList(Arrays.asList(array));
        // TODO(b/192579700): Use a ternary once it no longer confuses our nullness checker.
        if (pad || count == size) {
          return list;
        } else {
          return list.subList(0, count);
        }
      }
    };
  }

  /**
   * Returns a view of {@code unfiltered} containing all elements that satisfy the input predicate
   * {@code retainIfTrue}.
   */
  public static <T extends @Nullable Object> UnmodifiableIterator<T> filter(
      Iterator<T> unfiltered, Predicate<? super T> retainIfTrue) {
    checkNotNull(unfiltered);
    checkNotNull(retainIfTrue);
    return new AbstractIterator<T>() {
      @Override
      @CheckForNull
      protected T computeNext() {
        while (unfiltered.hasNext()) {
          T element = unfiltered.next();
          if (retainIfTrue.apply(element)) {
            return element;
          }
        }
        return endOfData();
      }
    };
  }

  /**
   * Returns a view of {@code unfiltered} containing all elements that are of the type {@code
   * desiredType}.
   */
  @SuppressWarnings("unchecked") // can cast to <T> because non-Ts are removed
  @GwtIncompatible // Class.isInstance
  public static <T> UnmodifiableIterator<T> filter(Iterator<?> unfiltered, Class<T> desiredType) {
    return (UnmodifiableIterator<T>) filter(unfiltered, instanceOf(desiredType));
  }

  /**
   * Returns {@code true} if one or more elements returned by {@code iterator} satisfy the given
   * predicate.
   */
  public static <T extends @Nullable Object> boolean any(
      Iterator<T> iterator, Predicate<? super T> predicate) {
    return indexOf(iterator, predicate) != -1;
  }

  /**
   * Returns {@code true} if every element returned by {@code iterator} satisfies the given
   * predicate. If {@code iterator} is empty, {@code true} is returned.
   */
  public static <T extends @Nullable Object> boolean all(
      Iterator<T> iterator, Predicate<? super T> predicate) {
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
   * Returns the first element in {@code iterator} that satisfies the given predicate; use this
   * method only when such an element is known to exist. If no such element is found, the iterator
   * will be left exhausted: its {@code hasNext()} method will return {@code false}. If it is
   * possible that <i>no</i> element will match, use {@link #tryFind} or {@link #find(Iterator,
   * Predicate, Object)} instead.
   *
   * @throws NoSuchElementException if no element in {@code iterator} matches the given predicate
   */
  @ParametricNullness
  public static <T extends @Nullable Object> T find(
      Iterator<T> iterator, Predicate<? super T> predicate) {
    checkNotNull(iterator);
    checkNotNull(predicate);
    while (iterator.hasNext()) {
      T t = iterator.next();
      if (predicate.apply(t)) {
        return t;
      }
    }
    throw new NoSuchElementException();
  }

  /**
   * Returns the first element in {@code iterator} that satisfies the given predicate. If no such
   * element is found, {@code defaultValue} will be returned from this method and the iterator will
   * be left exhausted: its {@code hasNext()} method will return {@code false}. Note that this can
   * usually be handled more naturally using {@code tryFind(iterator, predicate).or(defaultValue)}.
   *
   * @since 7.0
   */
  // For discussion of this signature, see the corresponding overload of *Iterables*.find.
  @CheckForNull
  public static <T extends @Nullable Object> T find(
      Iterator<? extends T> iterator,
      Predicate<? super T> predicate,
      @CheckForNull T defaultValue) {
    checkNotNull(iterator);
    checkNotNull(predicate);
    while (iterator.hasNext()) {
      T t = iterator.next();
      if (predicate.apply(t)) {
        return t;
      }
    }
    return defaultValue;
  }

  /**
   * Returns an {@link Optional} containing the first element in {@code iterator} that satisfies the
   * given predicate, if such an element exists. If no such element is found, an empty {@link
   * Optional} will be returned from this method and the iterator will be left exhausted: its {@code
   * hasNext()} method will return {@code false}.
   *
   * <p><b>Warning:</b> avoid using a {@code predicate} that matches {@code null}. If {@code null}
   * is matched in {@code iterator}, a NullPointerException will be thrown.
   *
   * @since 11.0
   */
  public static <T> Optional<T> tryFind(Iterator<T> iterator, Predicate<? super T> predicate) {
    checkNotNull(iterator);
    checkNotNull(predicate);
    while (iterator.hasNext()) {
      T t = iterator.next();
      if (predicate.apply(t)) {
        return Optional.of(t);
      }
    }
    return Optional.absent();
  }

  /**
   * Returns the index in {@code iterator} of the first element that satisfies the provided {@code
   * predicate}, or {@code -1} if the Iterator has no such elements.
   *
   * <p>More formally, returns the lowest index {@code i} such that {@code
   * predicate.apply(Iterators.get(iterator, i))} returns {@code true}, or {@code -1} if there is no
   * such index.
   *
   * <p>If -1 is returned, the iterator will be left exhausted: its {@code hasNext()} method will
   * return {@code false}. Otherwise, the iterator will be set to the element which satisfies the
   * {@code predicate}.
   *
   * @since 2.0
   */
  public static <T extends @Nullable Object> int indexOf(
      Iterator<T> iterator, Predicate<? super T> predicate) {
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
   * Returns a view containing the result of applying {@code function} to each element of {@code
   * fromIterator}.
   *
   * <p>The returned iterator supports {@code remove()} if {@code fromIterator} does. After a
   * successful {@code remove()} call, {@code fromIterator} no longer contains the corresponding
   * element.
   */
  public static <F extends @Nullable Object, T extends @Nullable Object> Iterator<T> transform(
      Iterator<F> fromIterator, Function<? super F, ? extends T> function) {
    checkNotNull(function);
    return new TransformedIterator<F, T>(fromIterator) {
      @ParametricNullness
      @Override
      T transform(@ParametricNullness F from) {
        return function.apply(from);
      }
    };
  }

  /**
   * Advances {@code iterator} {@code position + 1} times, returning the element at the {@code
   * position}th position.
   *
   * @param position position of the element to return
   * @return the element at the specified position in {@code iterator}
   * @throws IndexOutOfBoundsException if {@code position} is negative or greater than or equal to
   *     the number of elements remaining in {@code iterator}
   */
  @ParametricNullness
  public static <T extends @Nullable Object> T get(Iterator<T> iterator, int position) {
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

  /**
   * Advances {@code iterator} {@code position + 1} times, returning the element at the {@code
   * position}th position or {@code defaultValue} otherwise.
   *
   * @param position position of the element to return
   * @param defaultValue the default value to return if the iterator is empty or if {@code position}
   *     is greater than the number of elements remaining in {@code iterator}
   * @return the element at the specified position in {@code iterator} or {@code defaultValue} if
   *     {@code iterator} produces fewer than {@code position + 1} elements.
   * @throws IndexOutOfBoundsException if {@code position} is negative
   * @since 4.0
   */
  @ParametricNullness
  public static <T extends @Nullable Object> T get(
      Iterator<? extends T> iterator, int position, @ParametricNullness T defaultValue) {
    checkNonnegative(position);
    advance(iterator, position);
    return getNext(iterator, defaultValue);
  }

  static void checkNonnegative(int position) {
    if (position < 0) {
      throw new IndexOutOfBoundsException("position (" + position + ") must not be negative");
    }
  }

  /**
   * Returns the next element in {@code iterator} or {@code defaultValue} if the iterator is empty.
   * The {@link Iterables} analog to this method is {@link Iterables#getFirst}.
   *
   * @param defaultValue the default value to return if the iterator is empty
   * @return the next element of {@code iterator} or the default value
   * @since 7.0
   */
  @ParametricNullness
  public static <T extends @Nullable Object> T getNext(
      Iterator<? extends T> iterator, @ParametricNullness T defaultValue) {
    return iterator.hasNext() ? iterator.next() : defaultValue;
  }

  /**
   * Advances {@code iterator} to the end, returning the last element.
   *
   * @return the last element of {@code iterator}
   * @throws NoSuchElementException if the iterator is empty
   */
  @ParametricNullness
  public static <T extends @Nullable Object> T getLast(Iterator<T> iterator) {
    while (true) {
      T current = iterator.next();
      if (!iterator.hasNext()) {
        return current;
      }
    }
  }

  /**
   * Advances {@code iterator} to the end, returning the last element or {@code defaultValue} if the
   * iterator is empty.
   *
   * @param defaultValue the default value to return if the iterator is empty
   * @return the last element of {@code iterator}
   * @since 3.0
   */
  @ParametricNullness
  public static <T extends @Nullable Object> T getLast(
      Iterator<? extends T> iterator, @ParametricNullness T defaultValue) {
    return iterator.hasNext() ? getLast(iterator) : defaultValue;
  }

  /**
   * Calls {@code next()} on {@code iterator}, either {@code numberToAdvance} times or until {@code
   * hasNext()} returns {@code false}, whichever comes first.
   *
   * @return the number of elements the iterator was advanced
   * @since 13.0 (since 3.0 as {@code Iterators.skip})
   */
  @CanIgnoreReturnValue
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
   * Returns a view containing the first {@code limitSize} elements of {@code iterator}. If {@code
   * iterator} contains fewer than {@code limitSize} elements, the returned view contains all of its
   * elements. The returned iterator supports {@code remove()} if {@code iterator} does.
   *
   * @param iterator the iterator to limit
   * @param limitSize the maximum number of elements in the returned iterator
   * @throws IllegalArgumentException if {@code limitSize} is negative
   * @since 3.0
   */
  public static <T extends @Nullable Object> Iterator<T> limit(
      Iterator<T> iterator, int limitSize) {
    checkNotNull(iterator);
    checkArgument(limitSize >= 0, "limit is negative");
    return new Iterator<T>() {
      private int count;

      @Override
      public boolean hasNext() {
        return count < limitSize && iterator.hasNext();
      }

      @Override
      @ParametricNullness
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
   * Returns a view of the supplied {@code iterator} that removes each element from the supplied
   * {@code iterator} as it is returned.
   *
   * <p>The provided iterator must support {@link Iterator#remove()} or else the returned iterator
   * will fail on the first call to {@code next}.
   *
   * @param iterator the iterator to remove and return elements from
   * @return an iterator that removes and returns elements from the supplied iterator
   * @since 2.0
   */
  public static <T extends @Nullable Object> Iterator<T> consumingIterator(Iterator<T> iterator) {
    checkNotNull(iterator);
    return new UnmodifiableIterator<T>() {
      @Override
      public boolean hasNext() {
        return iterator.hasNext();
      }

      @Override
      @ParametricNullness
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
   * Deletes and returns the next value from the iterator, or returns {@code null} if there is no
   * such value.
   */
  @CheckForNull
  static <T extends @Nullable Object> T pollNext(Iterator<T> iterator) {
    if (iterator.hasNext()) {
      T result = iterator.next();
      iterator.remove();
      return result;
    } else {
      return null;
    }
  }

  // Methods only in Iterators, not in Iterables

  /** Clears the iterator using its remove method. */
  static void clear(Iterator<?> iterator) {
    checkNotNull(iterator);
    while (iterator.hasNext()) {
      iterator.next();
      iterator.remove();
    }
  }

  /**
   * Returns an iterator containing the elements of {@code array} in order. The returned iterator is
   * a view of the array; subsequent changes to the array will be reflected in the iterator.
   *
   * <p><b>Note:</b> It is often preferable to represent your data using a collection type, for
   * example using {@link Arrays#asList(Object[])}, making this method unnecessary.
   *
   * <p>The {@code Iterable} equivalent of this method is either {@link Arrays#asList(Object[])},
   * {@link ImmutableList#copyOf(Object[])}}, or {@link ImmutableList#of}.
   */
  @SafeVarargs
  public static <T extends @Nullable Object> UnmodifiableIterator<T> forArray(T... array) {
    return forArray(array, 0, array.length, 0);
  }

  /**
   * Returns a list iterator containing the elements in the specified range of {@code array} in
   * order, starting at the specified index.
   *
   * <p>The {@code Iterable} equivalent of this method is {@code
   * Arrays.asList(array).subList(offset, offset + length).listIterator(index)}.
   */
  static <T extends @Nullable Object> UnmodifiableListIterator<T> forArray(
      T[] array, int offset, int length, int index) {
    checkArgument(length >= 0);
    int end = offset + length;

    // Technically we should give a slightly more descriptive error on overflow
    Preconditions.checkPositionIndexes(offset, end, array.length);
    Preconditions.checkPositionIndex(index, length);
    if (length == 0) {
      return emptyListIterator();
    }
    return new ArrayItr<>(array, offset, length, index);
  }

  private static final class ArrayItr<T extends @Nullable Object>
      extends AbstractIndexedListIterator<T> {
    static final UnmodifiableListIterator<Object> EMPTY = new ArrayItr<>(new Object[0], 0, 0, 0);

    private final T[] array;
    private final int offset;

    ArrayItr(T[] array, int offset, int length, int index) {
      super(length, index);
      this.array = array;
      this.offset = offset;
    }

    @Override
    @ParametricNullness
    protected T get(int index) {
      return array[offset + index];
    }
  }

  /**
   * Returns an iterator containing only {@code value}.
   *
   * <p>The {@link Iterable} equivalent of this method is {@link Collections#singleton}.
   */
  public static <T extends @Nullable Object> UnmodifiableIterator<T> singletonIterator(
      @ParametricNullness T value) {
    return new UnmodifiableIterator<T>() {
      boolean done;

      @Override
      public boolean hasNext() {
        return !done;
      }

      @Override
      @ParametricNullness
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
   * <p>This method has no equivalent in {@link Iterables} because viewing an {@code Enumeration} as
   * an {@code Iterable} is impossible. However, the contents can be <i>copied</i> into a collection
   * using {@link Collections#list}.
   *
   * <p><b>Java 9 users:</b> use {@code enumeration.asIterator()} instead, unless it is important to
   * return an {@code UnmodifiableIterator} instead of a plain {@code Iterator}.
   */
  public static <T extends @Nullable Object> UnmodifiableIterator<T> forEnumeration(
      Enumeration<T> enumeration) {
    checkNotNull(enumeration);
    return new UnmodifiableIterator<T>() {
      @Override
      public boolean hasNext() {
        return enumeration.hasMoreElements();
      }

      @Override
      @ParametricNullness
      public T next() {
        return enumeration.nextElement();
      }
    };
  }

  /**
   * Adapts an {@code Iterator} to the {@code Enumeration} interface.
   *
   * <p>The {@code Iterable} equivalent of this method is either {@link Collections#enumeration} (if
   * you have a {@link Collection}), or {@code Iterators.asEnumeration(collection.iterator())}.
   */
  public static <T extends @Nullable Object> Enumeration<T> asEnumeration(Iterator<T> iterator) {
    checkNotNull(iterator);
    return new Enumeration<T>() {
      @Override
      public boolean hasMoreElements() {
        return iterator.hasNext();
      }

      @Override
      @ParametricNullness
      public T nextElement() {
        return iterator.next();
      }
    };
  }

  /** Implementation of PeekingIterator that avoids peeking unless necessary. */
  private static class PeekingImpl<E extends @Nullable Object> implements PeekingIterator<E> {

    private final Iterator<? extends E> iterator;
    private boolean hasPeeked;
    @CheckForNull private E peekedElement;

    public PeekingImpl(Iterator<? extends E> iterator) {
      this.iterator = checkNotNull(iterator);
    }

    @Override
    public boolean hasNext() {
      return hasPeeked || iterator.hasNext();
    }

    @Override
    @ParametricNullness
    public E next() {
      if (!hasPeeked) {
        return iterator.next();
      }
      // The cast is safe because of the hasPeeked check.
      E result = uncheckedCastNullableTToT(peekedElement);
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
    @ParametricNullness
    public E peek() {
      if (!hasPeeked) {
        peekedElement = iterator.next();
        hasPeeked = true;
      }
      // The cast is safe because of the hasPeeked check.
      return uncheckedCastNullableTToT(peekedElement);
    }
  }

  /**
   * Returns a {@code PeekingIterator} backed by the given iterator.
   *
   * <p>Calls to the {@code peek} method with no intervening calls to {@code next} do not affect the
   * iteration, and hence return the same object each time. A subsequent call to {@code next} is
   * guaranteed to return the same object again. For example:
   *
   * <pre>{@code
   * PeekingIterator<String> peekingIterator =
   *     Iterators.peekingIterator(Iterators.forArray("a", "b"));
   * String a1 = peekingIterator.peek(); // returns "a"
   * String a2 = peekingIterator.peek(); // also returns "a"
   * String a3 = peekingIterator.next(); // also returns "a"
   * }</pre>
   *
   * <p>Any structural changes to the underlying iteration (aside from those performed by the
   * iterator's own {@link PeekingIterator#remove()} method) will leave the iterator in an undefined
   * state.
   *
   * <p>The returned iterator does not support removal after peeking, as explained by {@link
   * PeekingIterator#remove()}.
   *
   * <p>Note: If the given iterator is already a {@code PeekingIterator}, it <i>might</i> be
   * returned to the caller, although this is neither guaranteed to occur nor required to be
   * consistent. For example, this method <i>might</i> choose to pass through recognized
   * implementations of {@code PeekingIterator} when the behavior of the implementation is known to
   * meet the contract guaranteed by this method.
   *
   * <p>There is no {@link Iterable} equivalent to this method, so use this method to wrap each
   * individual iterator as it is generated.
   *
   * @param iterator the backing iterator. The {@link PeekingIterator} assumes ownership of this
   *     iterator, so users should cease making direct calls to it after calling this method.
   * @return a peeking iterator backed by that iterator. Apart from the additional {@link
   *     PeekingIterator#peek()} method, this iterator behaves exactly the same as {@code iterator}.
   */
  public static <T extends @Nullable Object> PeekingIterator<T> peekingIterator(
      Iterator<? extends T> iterator) {
    if (iterator instanceof PeekingImpl) {
      // Safe to cast <? extends T> to <T> because PeekingImpl only uses T
      // covariantly (and cannot be subclassed to add non-covariant uses).
      @SuppressWarnings("unchecked")
      PeekingImpl<T> peeking = (PeekingImpl<T>) iterator;
      return peeking;
    }
    return new PeekingImpl<>(iterator);
  }

  /**
   * Simply returns its argument.
   *
   * @deprecated no need to use this
   * @since 10.0
   */
  @Deprecated
  public static <T extends @Nullable Object> PeekingIterator<T> peekingIterator(
      PeekingIterator<T> iterator) {
    return checkNotNull(iterator);
  }

  /**
   * Returns an iterator over the merged contents of all given {@code iterators}, traversing every
   * element of the input iterators. Equivalent entries will not be de-duplicated.
   *
   * <p>Callers must ensure that the source {@code iterators} are in non-descending order as this
   * method does not sort its input.
   *
   * <p>For any equivalent elements across all {@code iterators}, it is undefined which element is
   * returned first.
   *
   * @since 11.0
   */
  @Beta
  public static <T extends @Nullable Object> UnmodifiableIterator<T> mergeSorted(
      Iterable<? extends Iterator<? extends T>> iterators, Comparator<? super T> comparator) {
    checkNotNull(iterators, "iterators");
    checkNotNull(comparator, "comparator");

    return new MergingIterator<>(iterators, comparator);
  }

  /**
   * An iterator that performs a lazy N-way merge, calculating the next value each time the iterator
   * is polled. This amortizes the sorting cost over the iteration and requires less memory than
   * sorting all elements at once.
   *
   * <p>Retrieving a single element takes approximately O(log(M)) time, where M is the number of
   * iterators. (Retrieving all elements takes approximately O(N*log(M)) time, where N is the total
   * number of elements.)
   */
  private static class MergingIterator<T extends @Nullable Object> extends UnmodifiableIterator<T> {
    final Queue<PeekingIterator<T>> queue;

    public MergingIterator(
        Iterable<? extends Iterator<? extends T>> iterators, Comparator<? super T> itemComparator) {
      // A comparator that's used by the heap, allowing the heap
      // to be sorted based on the top of each iterator.
      Comparator<PeekingIterator<T>> heapComparator =
          (PeekingIterator<T> o1, PeekingIterator<T> o2) ->
              itemComparator.compare(o1.peek(), o2.peek());

      queue = new PriorityQueue<>(2, heapComparator);

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
    @ParametricNullness
    public T next() {
      PeekingIterator<T> nextIter = queue.remove();
      T next = nextIter.next();
      if (nextIter.hasNext()) {
        queue.add(nextIter);
      }
      return next;
    }
  }

  private static class ConcatenatedIterator<T extends @Nullable Object> implements Iterator<T> {
    /* The last iterator to return an element.  Calls to remove() go to this iterator. */
    @CheckForNull private Iterator<? extends T> toRemove;

    /* The iterator currently returning elements. */
    private Iterator<? extends T> iterator;

    /*
     * We track the "meta iterators," the iterators-of-iterators, below.  Usually, topMetaIterator
     * is the only one in use, but if we encounter nested concatenations, we start a deque of
     * meta-iterators rather than letting the nesting get arbitrarily deep.  This keeps each
     * operation O(1).
     */

    @CheckForNull private Iterator<? extends Iterator<? extends T>> topMetaIterator;

    // Only becomes nonnull if we encounter nested concatenations.
    @CheckForNull private Deque<Iterator<? extends Iterator<? extends T>>> metaIterators;

    ConcatenatedIterator(Iterator<? extends Iterator<? extends T>> metaIterator) {
      iterator = emptyIterator();
      topMetaIterator = checkNotNull(metaIterator);
    }

    // Returns a nonempty meta-iterator or, if all meta-iterators are empty, null.
    @CheckForNull
    private Iterator<? extends Iterator<? extends T>> getTopMetaIterator() {
      while (topMetaIterator == null || !topMetaIterator.hasNext()) {
        if (metaIterators != null && !metaIterators.isEmpty()) {
          topMetaIterator = metaIterators.removeFirst();
        } else {
          return null;
        }
      }
      return topMetaIterator;
    }

    @Override
    public boolean hasNext() {
      while (!checkNotNull(iterator).hasNext()) {
        // this weird checkNotNull positioning appears required by our tests, which expect
        // both hasNext and next to throw NPE if an input iterator is null.

        topMetaIterator = getTopMetaIterator();
        if (topMetaIterator == null) {
          return false;
        }

        iterator = topMetaIterator.next();

        if (iterator instanceof ConcatenatedIterator) {
          // Instead of taking linear time in the number of nested concatenations, unpack
          // them into the queue
          @SuppressWarnings("unchecked")
          ConcatenatedIterator<T> topConcat = (ConcatenatedIterator<T>) iterator;
          iterator = topConcat.iterator;

          // topConcat.topMetaIterator, then topConcat.metaIterators, then this.topMetaIterator,
          // then this.metaIterators

          if (this.metaIterators == null) {
            this.metaIterators = new ArrayDeque<>();
          }
          this.metaIterators.addFirst(this.topMetaIterator);
          if (topConcat.metaIterators != null) {
            while (!topConcat.metaIterators.isEmpty()) {
              this.metaIterators.addFirst(topConcat.metaIterators.removeLast());
            }
          }
          this.topMetaIterator = topConcat.topMetaIterator;
        }
      }
      return true;
    }

    @Override
    @ParametricNullness
    public T next() {
      if (hasNext()) {
        toRemove = iterator;
        return iterator.next();
      } else {
        throw new NoSuchElementException();
      }
    }

    @Override
    public void remove() {
      if (toRemove == null) {
        throw new IllegalStateException("no calls to next() since the last call to remove()");
      }
      toRemove.remove();
      toRemove = null;
    }
  }

  /** Used to avoid http://bugs.sun.com/view_bug.do?bug_id=6558557 */
  static <T extends @Nullable Object> ListIterator<T> cast(Iterator<T> iterator) {
    return (ListIterator<T>) iterator;
  }
}
