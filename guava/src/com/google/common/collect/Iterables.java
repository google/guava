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
import static com.google.common.collect.CollectPreconditions.checkRemove;

import com.google.common.annotations.Beta;
import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.RandomAccess;
import java.util.Set;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nullable;

/**
 * This class contains static utility methods that operate on or return objects
 * of type {@code Iterable}. Except as noted, each method has a corresponding
 * {@link Iterator}-based method in the {@link Iterators} class.
 *
 * <p><i>Performance notes:</i> Unless otherwise noted, all of the iterables
 * produced in this class are <i>lazy</i>, which means that their iterators
 * only advance the backing iteration when absolutely necessary.
 *
 * <p>See the Guava User Guide article on <a href=
 * "http://code.google.com/p/guava-libraries/wiki/CollectionUtilitiesExplained#Iterables">
 * {@code Iterables}</a>.
 *
 * @author Kevin Bourrillion
 * @author Jared Levy
 * @since 2.0 (imported from Google Collections Library)
 */
@GwtCompatible(emulated = true)
public final class Iterables {
  private Iterables() {}

  /** Returns an unmodifiable view of {@code iterable}. */
  public static <T> Iterable<T> unmodifiableIterable(
      final Iterable<T> iterable) {
    checkNotNull(iterable);
    if (iterable instanceof UnmodifiableIterable ||
        iterable instanceof ImmutableCollection) {
      return iterable;
    }
    return new UnmodifiableIterable<T>(iterable);
  }

  /**
   * Simply returns its argument.
   *
   * @deprecated no need to use this
   * @since 10.0
   */
  @Deprecated public static <E> Iterable<E> unmodifiableIterable(
      ImmutableCollection<E> iterable) {
    return checkNotNull(iterable);
  }

  private static final class UnmodifiableIterable<T> extends FluentIterable<T> {
    private final Iterable<T> iterable;

    private UnmodifiableIterable(Iterable<T> iterable) {
      this.iterable = iterable;
    }

    @Override
    public Iterator<T> iterator() {
      return Iterators.unmodifiableIterator(iterable.iterator());
    }

    @Override
    public String toString() {
      return iterable.toString();
    }
    // no equals and hashCode; it would break the contract!
  }

  /**
   * Returns the number of elements in {@code iterable}.
   */
  public static int size(Iterable<?> iterable) {
    return (iterable instanceof Collection)
        ? ((Collection<?>) iterable).size()
        : Iterators.size(iterable.iterator());
  }

  /**
   * Returns {@code true} if {@code iterable} contains any object for which {@code equals(element)}
   * is true.
   */
  public static boolean contains(Iterable<?> iterable, @Nullable Object element) {
    if (iterable instanceof Collection) {
      Collection<?> collection = (Collection<?>) iterable;
      return Collections2.safeContains(collection, element);
    }
    return Iterators.contains(iterable.iterator(), element);
  }

  /**
   * Removes, from an iterable, every element that belongs to the provided
   * collection.
   *
   * <p>This method calls {@link Collection#removeAll} if {@code iterable} is a
   * collection, and {@link Iterators#removeAll} otherwise.
   *
   * @param removeFrom the iterable to (potentially) remove elements from
   * @param elementsToRemove the elements to remove
   * @return {@code true} if any element was removed from {@code iterable}
   */
  public static boolean removeAll(
      Iterable<?> removeFrom, Collection<?> elementsToRemove) {
    return (removeFrom instanceof Collection)
        ? ((Collection<?>) removeFrom).removeAll(checkNotNull(elementsToRemove))
        : Iterators.removeAll(removeFrom.iterator(), elementsToRemove);
  }

  /**
   * Removes, from an iterable, every element that does not belong to the
   * provided collection.
   *
   * <p>This method calls {@link Collection#retainAll} if {@code iterable} is a
   * collection, and {@link Iterators#retainAll} otherwise.
   *
   * @param removeFrom the iterable to (potentially) remove elements from
   * @param elementsToRetain the elements to retain
   * @return {@code true} if any element was removed from {@code iterable}
   */
  public static boolean retainAll(
      Iterable<?> removeFrom, Collection<?> elementsToRetain) {
    return (removeFrom instanceof Collection)
        ? ((Collection<?>) removeFrom).retainAll(checkNotNull(elementsToRetain))
        : Iterators.retainAll(removeFrom.iterator(), elementsToRetain);
  }

  /**
   * Removes, from an iterable, every element that satisfies the provided
   * predicate.
   *
   * @param removeFrom the iterable to (potentially) remove elements from
   * @param predicate a predicate that determines whether an element should
   *     be removed
   * @return {@code true} if any elements were removed from the iterable
   *
   * @throws UnsupportedOperationException if the iterable does not support
   *     {@code remove()}.
   * @since 2.0
   */
  public static <T> boolean removeIf(
      Iterable<T> removeFrom, Predicate<? super T> predicate) {
    if (removeFrom instanceof RandomAccess && removeFrom instanceof List) {
      return removeIfFromRandomAccessList(
          (List<T>) removeFrom, checkNotNull(predicate));
    }
    return Iterators.removeIf(removeFrom.iterator(), predicate);
  }

  private static <T> boolean removeIfFromRandomAccessList(
      List<T> list, Predicate<? super T> predicate) {
    // Note: Not all random access lists support set() so we need to deal with
    // those that don't and attempt the slower remove() based solution.
    int from = 0;
    int to = 0;

    for (; from < list.size(); from++) {
      T element = list.get(from);
      if (!predicate.apply(element)) {
        if (from > to) {
          try {
            list.set(to, element);
          } catch (UnsupportedOperationException e) {
            slowRemoveIfForRemainingElements(list, predicate, to, from);
            return true;
          }
        }
        to++;
      }
    }

    // Clear the tail of any remaining items
    list.subList(to, list.size()).clear();
    return from != to;
  }

  private static <T> void slowRemoveIfForRemainingElements(List<T> list,
      Predicate<? super T> predicate, int to, int from) {
    // Here we know that:
    // * (to < from) and that both are valid indices.
    // * Everything with (index < to) should be kept.
    // * Everything with (to <= index < from) should be removed.
    // * The element with (index == from) should be kept.
    // * Everything with (index > from) has not been checked yet.

    // Check from the end of the list backwards (minimize expected cost of
    // moving elements when remove() is called). Stop before 'from' because
    // we already know that should be kept.
    for (int n = list.size() - 1; n > from; n--) {
      if (predicate.apply(list.get(n))) {
        list.remove(n);
      }
    }
    // And now remove everything in the range [to, from) (going backwards).
    for (int n = from - 1; n >= to; n--) {
      list.remove(n);
    }
  }

  /**
   * Removes and returns the first matching element, or returns {@code null} if there is none.
   */
  @Nullable
  static <T> T removeFirstMatching(Iterable<T> removeFrom, Predicate<? super T> predicate) {
    checkNotNull(predicate);
    Iterator<T> iterator = removeFrom.iterator();
    while (iterator.hasNext()) {
      T next = iterator.next();
      if (predicate.apply(next)) {
        iterator.remove();
        return next;
      }
    }
    return null;
  }

  /**
   * Determines whether two iterables contain equal elements in the same order.
   * More specifically, this method returns {@code true} if {@code iterable1}
   * and {@code iterable2} contain the same number of elements and every element
   * of {@code iterable1} is equal to the corresponding element of
   * {@code iterable2}.
   */
  @CheckReturnValue
  public static boolean elementsEqual(
      Iterable<?> iterable1, Iterable<?> iterable2) {
    if (iterable1 instanceof Collection && iterable2 instanceof Collection) {
      Collection<?> collection1 = (Collection<?>) iterable1;
      Collection<?> collection2 = (Collection<?>) iterable2;
      if (collection1.size() != collection2.size()) {
        return false;
      }
    }
    return Iterators.elementsEqual(iterable1.iterator(), iterable2.iterator());
  }

  /**
   * Returns a string representation of {@code iterable}, with the format {@code
   * [e1, e2, ..., en]} (that is, identical to {@link java.util.Arrays
   * Arrays}{@code .toString(Iterables.toArray(iterable))}). Note that for
   * <i>most</i> implementations of {@link Collection}, {@code
   * collection.toString()} also gives the same result, but that behavior is not
   * generally guaranteed.
   */
  public static String toString(Iterable<?> iterable) {
    return Iterators.toString(iterable.iterator());
  }

  /**
   * Returns the single element contained in {@code iterable}.
   *
   * @throws NoSuchElementException if the iterable is empty
   * @throws IllegalArgumentException if the iterable contains multiple
   *     elements
   */
  public static <T> T getOnlyElement(Iterable<T> iterable) {
    return Iterators.getOnlyElement(iterable.iterator());
  }

  /**
   * Returns the single element contained in {@code iterable}, or {@code
   * defaultValue} if the iterable is empty.
   *
   * @throws IllegalArgumentException if the iterator contains multiple
   *     elements
   */
  @Nullable
  public static <T> T getOnlyElement(
      Iterable<? extends T> iterable, @Nullable T defaultValue) {
    return Iterators.getOnlyElement(iterable.iterator(), defaultValue);
  }

  /**
   * Copies an iterable's elements into an array.
   *
   * @param iterable the iterable to copy
   * @param type the type of the elements
   * @return a newly-allocated array into which all the elements of the iterable
   *     have been copied
   */
  @GwtIncompatible("Array.newInstance(Class, int)")
  public static <T> T[] toArray(Iterable<? extends T> iterable, Class<T> type) {
    Collection<? extends T> collection = toCollection(iterable);
    T[] array = ObjectArrays.newArray(type, collection.size());
    return collection.toArray(array);
  }

  static <T> T[] toArray(Iterable<? extends T> iterable, T[] array) {
    Collection<? extends T> collection = toCollection(iterable);
    return collection.toArray(array);
  }

  /**
   * Copies an iterable's elements into an array.
   *
   * @param iterable the iterable to copy
   * @return a newly-allocated array into which all the elements of the iterable
   *     have been copied
   */
  static Object[] toArray(Iterable<?> iterable) {
    return toCollection(iterable).toArray();
  }

  /**
   * Converts an iterable into a collection. If the iterable is already a
   * collection, it is returned. Otherwise, an {@link java.util.ArrayList} is
   * created with the contents of the iterable in the same iteration order.
   */
  private static <E> Collection<E> toCollection(Iterable<E> iterable) {
    return (iterable instanceof Collection)
        ? (Collection<E>) iterable
        : Lists.newArrayList(iterable.iterator());
  }

  /**
   * Adds all elements in {@code iterable} to {@code collection}.
   *
   * @return {@code true} if {@code collection} was modified as a result of this
   *     operation.
   */
  public static <T> boolean addAll(
      Collection<T> addTo, Iterable<? extends T> elementsToAdd) {
    if (elementsToAdd instanceof Collection) {
      Collection<? extends T> c = Collections2.cast(elementsToAdd);
      return addTo.addAll(c);
    }
    return Iterators.addAll(addTo, checkNotNull(elementsToAdd).iterator());
  }

  /**
   * Returns the number of elements in the specified iterable that equal the
   * specified object. This implementation avoids a full iteration when the
   * iterable is a {@link Multiset} or {@link Set}.
   *
   * @see Collections#frequency
   */
  public static int frequency(Iterable<?> iterable, @Nullable Object element) {
    if ((iterable instanceof Multiset)) {
      return ((Multiset<?>) iterable).count(element);
    } else if ((iterable instanceof Set)) {
      return ((Set<?>) iterable).contains(element) ? 1 : 0;
    }
    return Iterators.frequency(iterable.iterator(), element);
  }

  /**
   * Returns an iterable whose iterators cycle indefinitely over the elements of
   * {@code iterable}.
   *
   * <p>That iterator supports {@code remove()} if {@code iterable.iterator()}
   * does. After {@code remove()} is called, subsequent cycles omit the removed
   * element, which is no longer in {@code iterable}. The iterator's
   * {@code hasNext()} method returns {@code true} until {@code iterable} is
   * empty.
   *
   * <p><b>Warning:</b> Typical uses of the resulting iterator may produce an
   * infinite loop. You should use an explicit {@code break} or be certain that
   * you will eventually remove all the elements.
   *
   * <p>To cycle over the iterable {@code n} times, use the following:
   * {@code Iterables.concat(Collections.nCopies(n, iterable))}
   */
  public static <T> Iterable<T> cycle(final Iterable<T> iterable) {
    checkNotNull(iterable);
    return new FluentIterable<T>() {
      @Override
      public Iterator<T> iterator() {
        return Iterators.cycle(iterable);
      }
      @Override public String toString() {
        return iterable.toString() + " (cycled)";
      }
    };
  }

  /**
   * Returns an iterable whose iterators cycle indefinitely over the provided
   * elements.
   *
   * <p>After {@code remove} is invoked on a generated iterator, the removed
   * element will no longer appear in either that iterator or any other iterator
   * created from the same source iterable. That is, this method behaves exactly
   * as {@code Iterables.cycle(Lists.newArrayList(elements))}. The iterator's
   * {@code hasNext} method returns {@code true} until all of the original
   * elements have been removed.
   *
   * <p><b>Warning:</b> Typical uses of the resulting iterator may produce an
   * infinite loop. You should use an explicit {@code break} or be certain that
   * you will eventually remove all the elements.
   *
   * <p>To cycle over the elements {@code n} times, use the following:
   * {@code Iterables.concat(Collections.nCopies(n, Arrays.asList(elements)))}
   */
  public static <T> Iterable<T> cycle(T... elements) {
    return cycle(Lists.newArrayList(elements));
  }

  /**
   * Combines two iterables into a single iterable. The returned iterable has an
   * iterator that traverses the elements in {@code a}, followed by the elements
   * in {@code b}. The source iterators are not polled until necessary.
   *
   * <p>The returned iterable's iterator supports {@code remove()} when the
   * corresponding input iterator supports it.
   */
  public static <T> Iterable<T> concat(
      Iterable<? extends T> a, Iterable<? extends T> b) {
    return concat(ImmutableList.of(a, b));
  }

  /**
   * Combines three iterables into a single iterable. The returned iterable has
   * an iterator that traverses the elements in {@code a}, followed by the
   * elements in {@code b}, followed by the elements in {@code c}. The source
   * iterators are not polled until necessary.
   *
   * <p>The returned iterable's iterator supports {@code remove()} when the
   * corresponding input iterator supports it.
   */
  public static <T> Iterable<T> concat(Iterable<? extends T> a,
      Iterable<? extends T> b, Iterable<? extends T> c) {
    return concat(ImmutableList.of(a, b, c));
  }

  /**
   * Combines four iterables into a single iterable. The returned iterable has
   * an iterator that traverses the elements in {@code a}, followed by the
   * elements in {@code b}, followed by the elements in {@code c}, followed by
   * the elements in {@code d}. The source iterators are not polled until
   * necessary.
   *
   * <p>The returned iterable's iterator supports {@code remove()} when the
   * corresponding input iterator supports it.
   */
  public static <T> Iterable<T> concat(Iterable<? extends T> a,
      Iterable<? extends T> b, Iterable<? extends T> c,
      Iterable<? extends T> d) {
    return concat(ImmutableList.of(a, b, c, d));
  }

  /**
   * Combines multiple iterables into a single iterable. The returned iterable
   * has an iterator that traverses the elements of each iterable in
   * {@code inputs}. The input iterators are not polled until necessary.
   *
   * <p>The returned iterable's iterator supports {@code remove()} when the
   * corresponding input iterator supports it.
   *
   * @throws NullPointerException if any of the provided iterables is null
   */
  public static <T> Iterable<T> concat(Iterable<? extends T>... inputs) {
    return concat(ImmutableList.copyOf(inputs));
  }

  /**
   * Combines multiple iterables into a single iterable. The returned iterable
   * has an iterator that traverses the elements of each iterable in
   * {@code inputs}. The input iterators are not polled until necessary.
   *
   * <p>The returned iterable's iterator supports {@code remove()} when the
   * corresponding input iterator supports it. The methods of the returned
   * iterable may throw {@code NullPointerException} if any of the input
   * iterators is null.
   */
  public static <T> Iterable<T> concat(
      final Iterable<? extends Iterable<? extends T>> inputs) {
    checkNotNull(inputs);
    return new FluentIterable<T>() {
      @Override
      public Iterator<T> iterator() {
        return Iterators.concat(iterators(inputs));
      }
    };
  }

  /**
   * Returns an iterator over the iterators of the given iterables.
   */
  private static <T> Iterator<Iterator<? extends T>> iterators(
      Iterable<? extends Iterable<? extends T>> iterables) {
    return new TransformedIterator<Iterable<? extends T>, Iterator<? extends T>>(
        iterables.iterator()) {
      @Override
      Iterator<? extends T> transform(Iterable<? extends T> from) {
        return from.iterator();
      }
    };
  }

  /**
   * Divides an iterable into unmodifiable sublists of the given size (the final
   * iterable may be smaller). For example, partitioning an iterable containing
   * {@code [a, b, c, d, e]} with a partition size of 3 yields {@code
   * [[a, b, c], [d, e]]} -- an outer iterable containing two inner lists of
   * three and two elements, all in the original order.
   *
   * <p>Iterators returned by the returned iterable do not support the {@link
   * Iterator#remove()} method. The returned lists implement {@link
   * RandomAccess}, whether or not the input list does.
   *
   * <p><b>Note:</b> if {@code iterable} is a {@link List}, use {@link
   * Lists#partition(List, int)} instead.
   *
   * @param iterable the iterable to return a partitioned view of
   * @param size the desired size of each partition (the last may be smaller)
   * @return an iterable of unmodifiable lists containing the elements of {@code
   *     iterable} divided into partitions
   * @throws IllegalArgumentException if {@code size} is nonpositive
   */
  public static <T> Iterable<List<T>> partition(
      final Iterable<T> iterable, final int size) {
    checkNotNull(iterable);
    checkArgument(size > 0);
    return new FluentIterable<List<T>>() {
      @Override
      public Iterator<List<T>> iterator() {
        return Iterators.partition(iterable.iterator(), size);
      }
    };
  }

  /**
   * Divides an iterable into unmodifiable sublists of the given size, padding
   * the final iterable with null values if necessary. For example, partitioning
   * an iterable containing {@code [a, b, c, d, e]} with a partition size of 3
   * yields {@code [[a, b, c], [d, e, null]]} -- an outer iterable containing
   * two inner lists of three elements each, all in the original order.
   *
   * <p>Iterators returned by the returned iterable do not support the {@link
   * Iterator#remove()} method.
   *
   * @param iterable the iterable to return a partitioned view of
   * @param size the desired size of each partition
   * @return an iterable of unmodifiable lists containing the elements of {@code
   *     iterable} divided into partitions (the final iterable may have
   *     trailing null elements)
   * @throws IllegalArgumentException if {@code size} is nonpositive
   */
  public static <T> Iterable<List<T>> paddedPartition(
      final Iterable<T> iterable, final int size) {
    checkNotNull(iterable);
    checkArgument(size > 0);
    return new FluentIterable<List<T>>() {
      @Override
      public Iterator<List<T>> iterator() {
        return Iterators.paddedPartition(iterable.iterator(), size);
      }
    };
  }

  /**
   * Returns the elements of {@code unfiltered} that satisfy a predicate. The
   * resulting iterable's iterator does not support {@code remove()}.
   */
  @CheckReturnValue
  public static <T> Iterable<T> filter(
      final Iterable<T> unfiltered, final Predicate<? super T> predicate) {
    checkNotNull(unfiltered);
    checkNotNull(predicate);
    return new FluentIterable<T>() {
      @Override
      public Iterator<T> iterator() {
        return Iterators.filter(unfiltered.iterator(), predicate);
      }
    };
  }

  /**
   * Returns all instances of class {@code type} in {@code unfiltered}. The
   * returned iterable has elements whose class is {@code type} or a subclass of
   * {@code type}. The returned iterable's iterator does not support
   * {@code remove()}.
   *
   * @param unfiltered an iterable containing objects of any type
   * @param type the type of elements desired
   * @return an unmodifiable iterable containing all elements of the original
   *     iterable that were of the requested type
   */
  @GwtIncompatible("Class.isInstance")
  public static <T> Iterable<T> filter(
      final Iterable<?> unfiltered, final Class<T> type) {
    checkNotNull(unfiltered);
    checkNotNull(type);
    return new FluentIterable<T>() {
      @Override
      public Iterator<T> iterator() {
        return Iterators.filter(unfiltered.iterator(), type);
      }
    };
  }

  /**
   * Returns {@code true} if any element in {@code iterable} satisfies the predicate.
   */
  public static <T> boolean any(
      Iterable<T> iterable, Predicate<? super T> predicate) {
    return Iterators.any(iterable.iterator(), predicate);
  }

  /**
   * Returns {@code true} if every element in {@code iterable} satisfies the
   * predicate. If {@code iterable} is empty, {@code true} is returned.
   */
  public static <T> boolean all(
      Iterable<T> iterable, Predicate<? super T> predicate) {
    return Iterators.all(iterable.iterator(), predicate);
  }

  /**
   * Returns the first element in {@code iterable} that satisfies the given
   * predicate; use this method only when such an element is known to exist. If
   * it is possible that <i>no</i> element will match, use {@link #tryFind} or
   * {@link #find(Iterable, Predicate, Object)} instead.
   *
   * @throws NoSuchElementException if no element in {@code iterable} matches
   *     the given predicate
   */
  public static <T> T find(Iterable<T> iterable,
      Predicate<? super T> predicate) {
    return Iterators.find(iterable.iterator(), predicate);
  }

  /**
   * Returns the first element in {@code iterable} that satisfies the given
   * predicate, or {@code defaultValue} if none found. Note that this can
   * usually be handled more naturally using {@code
   * tryFind(iterable, predicate).or(defaultValue)}.
   *
   * @since 7.0
   */
  @Nullable
  public static <T> T find(Iterable<? extends T> iterable,
      Predicate<? super T> predicate, @Nullable T defaultValue) {
    return Iterators.find(iterable.iterator(), predicate, defaultValue);
  }

  /**
   * Returns an {@link Optional} containing the first element in {@code
   * iterable} that satisfies the given predicate, if such an element exists.
   *
   * <p><b>Warning:</b> avoid using a {@code predicate} that matches {@code
   * null}. If {@code null} is matched in {@code iterable}, a
   * NullPointerException will be thrown.
   *
   * @since 11.0
   */
  public static <T> Optional<T> tryFind(Iterable<T> iterable,
      Predicate<? super T> predicate) {
    return Iterators.tryFind(iterable.iterator(), predicate);
  }

  /**
   * Returns the index in {@code iterable} of the first element that satisfies
   * the provided {@code predicate}, or {@code -1} if the Iterable has no such
   * elements.
   *
   * <p>More formally, returns the lowest index {@code i} such that
   * {@code predicate.apply(Iterables.get(iterable, i))} returns {@code true},
   * or {@code -1} if there is no such index.
   *
   * @since 2.0
   */
  public static <T> int indexOf(
      Iterable<T> iterable, Predicate<? super T> predicate) {
    return Iterators.indexOf(iterable.iterator(), predicate);
  }

  /**
   * Returns an iterable that applies {@code function} to each element of {@code
   * fromIterable}.
   *
   * <p>The returned iterable's iterator supports {@code remove()} if the
   * provided iterator does. After a successful {@code remove()} call,
   * {@code fromIterable} no longer contains the corresponding element.
   *
   * <p>If the input {@code Iterable} is known to be a {@code List} or other
   * {@code Collection}, consider {@link Lists#transform} and {@link
   * Collections2#transform}.
   */
  @CheckReturnValue
  public static <F, T> Iterable<T> transform(final Iterable<F> fromIterable,
      final Function<? super F, ? extends T> function) {
    checkNotNull(fromIterable);
    checkNotNull(function);
    return new FluentIterable<T>() {
      @Override
      public Iterator<T> iterator() {
        return Iterators.transform(fromIterable.iterator(), function);
      }
    };
  }

  /**
   * Returns the element at the specified position in an iterable.
   *
   * @param position position of the element to return
   * @return the element at the specified position in {@code iterable}
   * @throws IndexOutOfBoundsException if {@code position} is negative or
   *     greater than or equal to the size of {@code iterable}
   */
  public static <T> T get(Iterable<T> iterable, int position) {
    checkNotNull(iterable);
    return (iterable instanceof List)
        ? ((List<T>) iterable).get(position)
        : Iterators.get(iterable.iterator(), position);
  }

  /**
   * Returns the element at the specified position in an iterable or a default
   * value otherwise.
   *
   * @param position position of the element to return
   * @param defaultValue the default value to return if {@code position} is
   *     greater than or equal to the size of the iterable
   * @return the element at the specified position in {@code iterable} or
   *     {@code defaultValue} if {@code iterable} contains fewer than
   *     {@code position + 1} elements.
   * @throws IndexOutOfBoundsException if {@code position} is negative
   * @since 4.0
   */
  @Nullable
  public static <T> T get(Iterable<? extends T> iterable, int position, @Nullable T defaultValue) {
    checkNotNull(iterable);
    Iterators.checkNonnegative(position);
    if (iterable instanceof List) {
      List<? extends T> list = Lists.cast(iterable);
      return (position < list.size()) ? list.get(position) : defaultValue;
    } else {
      Iterator<? extends T> iterator = iterable.iterator();
      Iterators.advance(iterator, position);
      return Iterators.getNext(iterator, defaultValue);
    }
  }

  /**
   * Returns the first element in {@code iterable} or {@code defaultValue} if
   * the iterable is empty.  The {@link Iterators} analog to this method is
   * {@link Iterators#getNext}.
   *
   * <p>If no default value is desired (and the caller instead wants a
   * {@link NoSuchElementException} to be thrown), it is recommended that
   * {@code iterable.iterator().next()} is used instead.
   *
   * @param defaultValue the default value to return if the iterable is empty
   * @return the first element of {@code iterable} or the default value
   * @since 7.0
   */
  @Nullable
  public static <T> T getFirst(Iterable<? extends T> iterable, @Nullable T defaultValue) {
    return Iterators.getNext(iterable.iterator(), defaultValue);
  }

  /**
   * Returns the last element of {@code iterable}.
   *
   * @return the last element of {@code iterable}
   * @throws NoSuchElementException if the iterable is empty
   */
  public static <T> T getLast(Iterable<T> iterable) {
    // TODO(kevinb): Support a concurrently modified collection?
    if (iterable instanceof List) {
      List<T> list = (List<T>) iterable;
      if (list.isEmpty()) {
        throw new NoSuchElementException();
      }
      return getLastInNonemptyList(list);
    }

    return Iterators.getLast(iterable.iterator());
  }

  /**
   * Returns the last element of {@code iterable} or {@code defaultValue} if
   * the iterable is empty.
   *
   * @param defaultValue the value to return if {@code iterable} is empty
   * @return the last element of {@code iterable} or the default value
   * @since 3.0
   */
  @Nullable
  public static <T> T getLast(Iterable<? extends T> iterable, @Nullable T defaultValue) {
    if (iterable instanceof Collection) {
      Collection<? extends T> c = Collections2.cast(iterable);
      if (c.isEmpty()) {
        return defaultValue;
      } else if (iterable instanceof List) {
        return getLastInNonemptyList(Lists.cast(iterable));
      }
    }

    return Iterators.getLast(iterable.iterator(), defaultValue);
  }

  private static <T> T getLastInNonemptyList(List<T> list) {
    return list.get(list.size() - 1);
  }

  /**
   * Returns a view of {@code iterable} that skips its first
   * {@code numberToSkip} elements. If {@code iterable} contains fewer than
   * {@code numberToSkip} elements, the returned iterable skips all of its
   * elements.
   *
   * <p>Modifications to the underlying {@link Iterable} before a call to
   * {@code iterator()} are reflected in the returned iterator. That is, the
   * iterator skips the first {@code numberToSkip} elements that exist when the
   * {@code Iterator} is created, not when {@code skip()} is called.
   *
   * <p>The returned iterable's iterator supports {@code remove()} if the
   * iterator of the underlying iterable supports it. Note that it is
   * <i>not</i> possible to delete the last skipped element by immediately
   * calling {@code remove()} on that iterator, as the {@code Iterator}
   * contract states that a call to {@code remove()} before a call to
   * {@code next()} will throw an {@link IllegalStateException}.
   *
   * @since 3.0
   */
  public static <T> Iterable<T> skip(final Iterable<T> iterable,
      final int numberToSkip) {
    checkNotNull(iterable);
    checkArgument(numberToSkip >= 0, "number to skip cannot be negative");

    if (iterable instanceof List) {
      final List<T> list = (List<T>) iterable;
      return new FluentIterable<T>() {
        @Override
        public Iterator<T> iterator() {
          // TODO(kevinb): Support a concurrently modified collection?
          int toSkip = Math.min(list.size(), numberToSkip);
          return list.subList(toSkip, list.size()).iterator();
        }
      };
    }

    return new FluentIterable<T>() {
      @Override
      public Iterator<T> iterator() {
        final Iterator<T> iterator = iterable.iterator();

        Iterators.advance(iterator, numberToSkip);

        /*
         * We can't just return the iterator because an immediate call to its
         * remove() method would remove one of the skipped elements instead of
         * throwing an IllegalStateException.
         */
        return new Iterator<T>() {
          boolean atStart = true;

          @Override
          public boolean hasNext() {
            return iterator.hasNext();
          }

          @Override
          public T next() {
            T result = iterator.next();
            atStart = false; // not called if next() fails
            return result;
          }

          @Override
          public void remove() {
            checkRemove(!atStart);
            iterator.remove();
          }
        };
      }
    };
  }

  /**
   * Creates an iterable with the first {@code limitSize} elements of the given
   * iterable. If the original iterable does not contain that many elements, the
   * returned iterable will have the same behavior as the original iterable. The
   * returned iterable's iterator supports {@code remove()} if the original
   * iterator does.
   *
   * @param iterable the iterable to limit
   * @param limitSize the maximum number of elements in the returned iterable
   * @throws IllegalArgumentException if {@code limitSize} is negative
   * @since 3.0
   */
  public static <T> Iterable<T> limit(
      final Iterable<T> iterable, final int limitSize) {
    checkNotNull(iterable);
    checkArgument(limitSize >= 0, "limit is negative");
    return new FluentIterable<T>() {
      @Override
      public Iterator<T> iterator() {
        return Iterators.limit(iterable.iterator(), limitSize);
      }
    };
  }

  /**
   * Returns a view of the supplied iterable that wraps each generated
   * {@link Iterator} through {@link Iterators#consumingIterator(Iterator)}.
   *
   * <p>Note: If {@code iterable} is a {@link Queue}, the returned iterable will
   * get entries from {@link Queue#remove()} since {@link Queue}'s iteration
   * order is undefined.  Calling {@link Iterator#hasNext()} on a generated
   * iterator from the returned iterable may cause an item to be immediately
   * dequeued for return on a subsequent call to {@link Iterator#next()}.
   *
   * @param iterable the iterable to wrap
   * @return a view of the supplied iterable that wraps each generated iterator
   *     through {@link Iterators#consumingIterator(Iterator)}; for queues,
   *     an iterable that generates iterators that return and consume the
   *     queue's elements in queue order
   *
   * @see Iterators#consumingIterator(Iterator)
   * @since 2.0
   */
  public static <T> Iterable<T> consumingIterable(final Iterable<T> iterable) {
    if (iterable instanceof Queue) {
      return new FluentIterable<T>() {
        @Override
        public Iterator<T> iterator() {
          return new ConsumingQueueIterator<T>((Queue<T>) iterable);
        }

        @Override
        public String toString() {
          return "Iterables.consumingIterable(...)";
        }
      };
    }

    checkNotNull(iterable);

    return new FluentIterable<T>() {
      @Override
      public Iterator<T> iterator() {
        return Iterators.consumingIterator(iterable.iterator());
      }

      @Override
      public String toString() {
        return "Iterables.consumingIterable(...)";
      }
    };
  }

  private static class ConsumingQueueIterator<T> extends AbstractIterator<T> {
    private final Queue<T> queue;

    private ConsumingQueueIterator(Queue<T> queue) {
      this.queue = queue;
    }

    @Override public T computeNext() {
      try {
        return queue.remove();
      } catch (NoSuchElementException e) {
        return endOfData();
      }
    }
  }

  // Methods only in Iterables, not in Iterators

  /**
   * Determines if the given iterable contains no elements.
   *
   * <p>There is no precise {@link Iterator} equivalent to this method, since
   * one can only ask an iterator whether it has any elements <i>remaining</i>
   * (which one does using {@link Iterator#hasNext}).
   *
   * @return {@code true} if the iterable contains no elements
   */
  public static boolean isEmpty(Iterable<?> iterable) {
    if (iterable instanceof Collection) {
      return ((Collection<?>) iterable).isEmpty();
    }
    return !iterable.iterator().hasNext();
  }

  /**
   * Returns an iterable over the merged contents of all given
   * {@code iterables}. Equivalent entries will not be de-duplicated.
   *
   * <p>Callers must ensure that the source {@code iterables} are in
   * non-descending order as this method does not sort its input.
   *
   * <p>For any equivalent elements across all {@code iterables}, it is
   * undefined which element is returned first.
   *
   * @since 11.0
   */
  @Beta
  public static <T> Iterable<T> mergeSorted(
      final Iterable<? extends Iterable<? extends T>> iterables,
      final Comparator<? super T> comparator) {
    checkNotNull(iterables, "iterables");
    checkNotNull(comparator, "comparator");
    Iterable<T> iterable = new FluentIterable<T>() {
      @Override
      public Iterator<T> iterator() {
        return Iterators.mergeSorted(
            Iterables.transform(iterables, Iterables.<T>toIterator()),
            comparator);
      }
    };
    return new UnmodifiableIterable<T>(iterable);
  }

  // TODO(user): Is this the best place for this? Move to fluent functions?
  // Useful as a public method?
  private static <T> Function<Iterable<? extends T>, Iterator<? extends T>>
      toIterator() {
    return new Function<Iterable<? extends T>, Iterator<? extends T>>() {
      @Override
      public Iterator<? extends T> apply(Iterable<? extends T> iterable) {
        return iterable.iterator();
      }
    };
  }
}
