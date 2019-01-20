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
import com.google.common.base.Predicates;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.RandomAccess;
import java.util.Set;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * An assortment of mainly legacy static utility methods that operate on or return objects of type
 * {@code Iterable}. Except as noted, each method has a corresponding {@link Iterator}-based method
 * in the {@link Iterators} class.
 *
 * <p><b>Java 8 users:</b> several common uses for this class are now more comprehensively addressed
 * by the new {@link java.util.stream.Stream} library. Read the method documentation below for
 * comparisons. This class is not being deprecated, but we gently encourage you to migrate to
 * streams.
 *
 * <p><i>Performance notes:</i> Unless otherwise noted, all of the iterables produced in this class
 * are <i>lazy</i>, which means that their iterators only advance the backing iteration when
 * absolutely necessary.
 *
 * <p>See the Guava User Guide article on <a href=
 * "https://github.com/google/guava/wiki/CollectionUtilitiesExplained#iterables"> {@code
 * Iterables}</a>.
 *
 * @author Kevin Bourrillion
 * @author Jared Levy
 * @since 2.0
 */
@GwtCompatible(emulated = true)
public final class Iterables {
  private Iterables() {}

  /** Returns an unmodifiable view of {@code iterable}. */
  public static <T> Iterable<T> unmodifiableIterable(final Iterable<? extends T> iterable) {
    checkNotNull(iterable);
    if (iterable instanceof UnmodifiableIterable || iterable instanceof ImmutableCollection) {
      @SuppressWarnings("unchecked") // Since it's unmodifiable, the covariant cast is safe
      Iterable<T> result = (Iterable<T>) iterable;
      return result;
    }
    return new UnmodifiableIterable<>(iterable);
  }

  /**
   * Simply returns its argument.
   *
   * @deprecated no need to use this
   * @since 10.0
   */
  @Deprecated
  public static <E> Iterable<E> unmodifiableIterable(ImmutableCollection<E> iterable) {
    return checkNotNull(iterable);
  }

  private static final class UnmodifiableIterable<T> extends FluentIterable<T> {
    private final Iterable<? extends T> iterable;

    private UnmodifiableIterable(Iterable<? extends T> iterable) {
      this.iterable = iterable;
    }

    @Override
    public Iterator<T> iterator() {
      return Iterators.unmodifiableIterator(iterable.iterator());
    }

    @Override
    public void forEach(Consumer<? super T> action) {
      iterable.forEach(action);
    }

    @SuppressWarnings("unchecked") // safe upcast, assuming no one has a crazy Spliterator subclass
    @Override
    public Spliterator<T> spliterator() {
      return (Spliterator<T>) iterable.spliterator();
    }

    @Override
    public String toString() {
      return iterable.toString();
    }
    // no equals and hashCode; it would break the contract!
  }

  /** Returns the number of elements in {@code iterable}. */
  public static int size(Iterable<?> iterable) {
    return (iterable instanceof Collection)
        ? ((Collection<?>) iterable).size()
        : Iterators.size(iterable.iterator());
  }

  /**
   * Returns {@code true} if {@code iterable} contains any element {@code o} for which {@code
   * Objects.equals(o, element)} would return {@code true}. Otherwise returns {@code false}, even in
   * cases where {@link Collection#contains} might throw {@link NullPointerException} or {@link
   * ClassCastException}.
   */
  public static boolean contains(Iterable<?> iterable, @Nullable Object element) {
    if (iterable instanceof Collection) {
      Collection<?> collection = (Collection<?>) iterable;
      return Collections2.safeContains(collection, element);
    }
    return Iterators.contains(iterable.iterator(), element);
  }

  /**
   * Removes, from an iterable, every element that belongs to the provided collection.
   *
   * <p>This method calls {@link Collection#removeAll} if {@code iterable} is a collection, and
   * {@link Iterators#removeAll} otherwise.
   *
   * @param removeFrom the iterable to (potentially) remove elements from
   * @param elementsToRemove the elements to remove
   * @return {@code true} if any element was removed from {@code iterable}
   */
  @CanIgnoreReturnValue
  public static boolean removeAll(Iterable<?> removeFrom, Collection<?> elementsToRemove) {
    return (removeFrom instanceof Collection)
        ? ((Collection<?>) removeFrom).removeAll(checkNotNull(elementsToRemove))
        : Iterators.removeAll(removeFrom.iterator(), elementsToRemove);
  }

  /**
   * Removes, from an iterable, every element that does not belong to the provided collection.
   *
   * <p>This method calls {@link Collection#retainAll} if {@code iterable} is a collection, and
   * {@link Iterators#retainAll} otherwise.
   *
   * @param removeFrom the iterable to (potentially) remove elements from
   * @param elementsToRetain the elements to retain
   * @return {@code true} if any element was removed from {@code iterable}
   */
  @CanIgnoreReturnValue
  public static boolean retainAll(Iterable<?> removeFrom, Collection<?> elementsToRetain) {
    return (removeFrom instanceof Collection)
        ? ((Collection<?>) removeFrom).retainAll(checkNotNull(elementsToRetain))
        : Iterators.retainAll(removeFrom.iterator(), elementsToRetain);
  }

  /**
   * Removes, from an iterable, every element that satisfies the provided predicate.
   *
   * <p>Removals may or may not happen immediately as each element is tested against the predicate.
   * The behavior of this method is not specified if {@code predicate} is dependent on {@code
   * removeFrom}.
   *
   * <p><b>Java 8 users:</b> if {@code removeFrom} is a {@link Collection}, use {@code
   * removeFrom.removeIf(predicate)} instead.
   *
   * @param removeFrom the iterable to (potentially) remove elements from
   * @param predicate a predicate that determines whether an element should be removed
   * @return {@code true} if any elements were removed from the iterable
   * @throws UnsupportedOperationException if the iterable does not support {@code remove()}.
   * @since 2.0
   */
  @CanIgnoreReturnValue
  public static <T> boolean removeIf(Iterable<T> removeFrom, Predicate<? super T> predicate) {
    if (removeFrom instanceof Collection) {
      return ((Collection<T>) removeFrom).removeIf(predicate);
    }
    return Iterators.removeIf(removeFrom.iterator(), predicate);
  }

  /** Removes and returns the first matching element, or returns {@code null} if there is none. */
  static <T> @Nullable T removeFirstMatching(
      Iterable<T> removeFrom, Predicate<? super T> predicate) {
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
   * Determines whether two iterables contain equal elements in the same order. More specifically,
   * this method returns {@code true} if {@code iterable1} and {@code iterable2} contain the same
   * number of elements and every element of {@code iterable1} is equal to the corresponding element
   * of {@code iterable2}.
   */
  public static boolean elementsEqual(Iterable<?> iterable1, Iterable<?> iterable2) {
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
   * Returns a string representation of {@code iterable}, with the format {@code [e1, e2, ..., en]}
   * (that is, identical to {@link java.util.Arrays Arrays}{@code
   * .toString(Iterables.toArray(iterable))}). Note that for <i>most</i> implementations of {@link
   * Collection}, {@code collection.toString()} also gives the same result, but that behavior is not
   * generally guaranteed.
   */
  public static String toString(Iterable<?> iterable) {
    return Iterators.toString(iterable.iterator());
  }

  /**
   * Returns the single element contained in {@code iterable}.
   *
   * <p><b>Java 8 users:</b> the {@code Stream} equivalent to this method is {@code
   * stream.collect(MoreCollectors.onlyElement())}.
   *
   * @throws NoSuchElementException if the iterable is empty
   * @throws IllegalArgumentException if the iterable contains multiple elements
   */
  public static <T> T getOnlyElement(Iterable<T> iterable) {
    return Iterators.getOnlyElement(iterable.iterator());
  }

  /**
   * Returns the single element contained in {@code iterable}, or {@code defaultValue} if the
   * iterable is empty.
   *
   * <p><b>Java 8 users:</b> the {@code Stream} equivalent to this method is {@code
   * stream.collect(MoreCollectors.toOptional()).orElse(defaultValue)}.
   *
   * @throws IllegalArgumentException if the iterator contains multiple elements
   */
  public static <T> @Nullable T getOnlyElement(
      Iterable<? extends T> iterable, @Nullable T defaultValue) {
    return Iterators.getOnlyElement(iterable.iterator(), defaultValue);
  }

  /**
   * Copies an iterable's elements into an array.
   *
   * @param iterable the iterable to copy
   * @param type the type of the elements
   * @return a newly-allocated array into which all the elements of the iterable have been copied
   */
  @GwtIncompatible // Array.newInstance(Class, int)
  public static <T> T[] toArray(Iterable<? extends T> iterable, Class<T> type) {
    return toArray(iterable, ObjectArrays.newArray(type, 0));
  }

  static <T> T[] toArray(Iterable<? extends T> iterable, T[] array) {
    Collection<? extends T> collection = castOrCopyToCollection(iterable);
    return collection.toArray(array);
  }

  /**
   * Copies an iterable's elements into an array.
   *
   * @param iterable the iterable to copy
   * @return a newly-allocated array into which all the elements of the iterable have been copied
   */
  static Object[] toArray(Iterable<?> iterable) {
    return castOrCopyToCollection(iterable).toArray();
  }

  /**
   * Converts an iterable into a collection. If the iterable is already a collection, it is
   * returned. Otherwise, an {@link java.util.ArrayList} is created with the contents of the
   * iterable in the same iteration order.
   */
  private static <E> Collection<E> castOrCopyToCollection(Iterable<E> iterable) {
    return (iterable instanceof Collection)
        ? (Collection<E>) iterable
        : Lists.newArrayList(iterable.iterator());
  }

  /**
   * Adds all elements in {@code iterable} to {@code collection}.
   *
   * @return {@code true} if {@code collection} was modified as a result of this operation.
   */
  @CanIgnoreReturnValue
  public static <T> boolean addAll(Collection<T> addTo, Iterable<? extends T> elementsToAdd) {
    if (elementsToAdd instanceof Collection) {
      Collection<? extends T> c = Collections2.cast(elementsToAdd);
      return addTo.addAll(c);
    }
    return Iterators.addAll(addTo, checkNotNull(elementsToAdd).iterator());
  }

  /**
   * Returns the number of elements in the specified iterable that equal the specified object. This
   * implementation avoids a full iteration when the iterable is a {@link Multiset} or {@link Set}.
   *
   * <p><b>Java 8 users:</b> In most cases, the {@code Stream} equivalent of this method is {@code
   * stream.filter(element::equals).count()}. If {@code element} might be null, use {@code
   * stream.filter(Predicate.isEqual(element)).count()} instead.
   *
   * @see java.util.Collections#frequency(Collection, Object) Collections.frequency(Collection,
   *     Object)
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
   * Returns an iterable whose iterators cycle indefinitely over the elements of {@code iterable}.
   *
   * <p>That iterator supports {@code remove()} if {@code iterable.iterator()} does. After {@code
   * remove()} is called, subsequent cycles omit the removed element, which is no longer in {@code
   * iterable}. The iterator's {@code hasNext()} method returns {@code true} until {@code iterable}
   * is empty.
   *
   * <p><b>Warning:</b> Typical uses of the resulting iterator may produce an infinite loop. You
   * should use an explicit {@code break} or be certain that you will eventually remove all the
   * elements.
   *
   * <p>To cycle over the iterable {@code n} times, use the following: {@code
   * Iterables.concat(Collections.nCopies(n, iterable))}
   *
   * <p><b>Java 8 users:</b> The {@code Stream} equivalent of this method is {@code
   * Stream.generate(() -> iterable).flatMap(Streams::stream)}.
   */
  public static <T> Iterable<T> cycle(final Iterable<T> iterable) {
    checkNotNull(iterable);
    return new FluentIterable<T>() {
      @Override
      public Iterator<T> iterator() {
        return Iterators.cycle(iterable);
      }

      @Override
      public Spliterator<T> spliterator() {
        return Stream.generate(() -> iterable).flatMap(Streams::stream).spliterator();
      }

      @Override
      public String toString() {
        return iterable.toString() + " (cycled)";
      }
    };
  }

  /**
   * Returns an iterable whose iterators cycle indefinitely over the provided elements.
   *
   * <p>After {@code remove} is invoked on a generated iterator, the removed element will no longer
   * appear in either that iterator or any other iterator created from the same source iterable.
   * That is, this method behaves exactly as {@code Iterables.cycle(Lists.newArrayList(elements))}.
   * The iterator's {@code hasNext} method returns {@code true} until all of the original elements
   * have been removed.
   *
   * <p><b>Warning:</b> Typical uses of the resulting iterator may produce an infinite loop. You
   * should use an explicit {@code break} or be certain that you will eventually remove all the
   * elements.
   *
   * <p>To cycle over the elements {@code n} times, use the following: {@code
   * Iterables.concat(Collections.nCopies(n, Arrays.asList(elements)))}
   *
   * <p><b>Java 8 users:</b> If passing a single element {@code e}, the {@code Stream} equivalent of
   * this method is {@code Stream.generate(() -> e)}. Otherwise, put the elements in a collection
   * and use {@code Stream.generate(() -> collection).flatMap(Collection::stream)}.
   */
  @SafeVarargs
  public static <T> Iterable<T> cycle(T... elements) {
    return cycle(Lists.newArrayList(elements));
  }

  /**
   * Combines two iterables into a single iterable. The returned iterable has an iterator that
   * traverses the elements in {@code a}, followed by the elements in {@code b}. The source
   * iterators are not polled until necessary.
   *
   * <p>The returned iterable's iterator supports {@code remove()} when the corresponding input
   * iterator supports it.
   *
   * <p><b>Java 8 users:</b> The {@code Stream} equivalent of this method is {@code Stream.concat(a,
   * b)}.
   */
  public static <T> Iterable<T> concat(Iterable<? extends T> a, Iterable<? extends T> b) {
    return FluentIterable.concat(a, b);
  }

  /**
   * Combines three iterables into a single iterable. The returned iterable has an iterator that
   * traverses the elements in {@code a}, followed by the elements in {@code b}, followed by the
   * elements in {@code c}. The source iterators are not polled until necessary.
   *
   * <p>The returned iterable's iterator supports {@code remove()} when the corresponding input
   * iterator supports it.
   *
   * <p><b>Java 8 users:</b> The {@code Stream} equivalent of this method is {@code
   * Streams.concat(a, b, c)}.
   */
  public static <T> Iterable<T> concat(
      Iterable<? extends T> a, Iterable<? extends T> b, Iterable<? extends T> c) {
    return FluentIterable.concat(a, b, c);
  }

  /**
   * Combines four iterables into a single iterable. The returned iterable has an iterator that
   * traverses the elements in {@code a}, followed by the elements in {@code b}, followed by the
   * elements in {@code c}, followed by the elements in {@code d}. The source iterators are not
   * polled until necessary.
   *
   * <p>The returned iterable's iterator supports {@code remove()} when the corresponding input
   * iterator supports it.
   *
   * <p><b>Java 8 users:</b> The {@code Stream} equivalent of this method is {@code
   * Streams.concat(a, b, c, d)}.
   */
  public static <T> Iterable<T> concat(
      Iterable<? extends T> a,
      Iterable<? extends T> b,
      Iterable<? extends T> c,
      Iterable<? extends T> d) {
    return FluentIterable.concat(a, b, c, d);
  }

  /**
   * Combines multiple iterables into a single iterable. The returned iterable has an iterator that
   * traverses the elements of each iterable in {@code inputs}. The input iterators are not polled
   * until necessary.
   *
   * <p>The returned iterable's iterator supports {@code remove()} when the corresponding input
   * iterator supports it.
   *
   * <p><b>Java 8 users:</b> The {@code Stream} equivalent of this method is {@code
   * Streams.concat(...)}.
   *
   * @throws NullPointerException if any of the provided iterables is null
   */
  @SafeVarargs
  public static <T> Iterable<T> concat(Iterable<? extends T>... inputs) {
    return FluentIterable.concat(inputs);
  }

  /**
   * Combines multiple iterables into a single iterable. The returned iterable has an iterator that
   * traverses the elements of each iterable in {@code inputs}. The input iterators are not polled
   * until necessary.
   *
   * <p>The returned iterable's iterator supports {@code remove()} when the corresponding input
   * iterator supports it. The methods of the returned iterable may throw {@code
   * NullPointerException} if any of the input iterators is null.
   *
   * <p><b>Java 8 users:</b> The {@code Stream} equivalent of this method is {@code
   * streamOfStreams.flatMap(s -> s)}.
   */
  public static <T> Iterable<T> concat(Iterable<? extends Iterable<? extends T>> inputs) {
    return FluentIterable.concat(inputs);
  }

  /**
   * Divides an iterable into unmodifiable sublists of the given size (the final iterable may be
   * smaller). For example, partitioning an iterable containing {@code [a, b, c, d, e]} with a
   * partition size of 3 yields {@code [[a, b, c], [d, e]]} -- an outer iterable containing two
   * inner lists of three and two elements, all in the original order.
   *
   * <p>Iterators returned by the returned iterable do not support the {@link Iterator#remove()}
   * method. The returned lists implement {@link RandomAccess}, whether or not the input list does.
   *
   * <p><b>Note:</b> if {@code iterable} is a {@link List}, use {@link Lists#partition(List, int)}
   * instead.
   *
   * @param iterable the iterable to return a partitioned view of
   * @param size the desired size of each partition (the last may be smaller)
   * @return an iterable of unmodifiable lists containing the elements of {@code iterable} divided
   *     into partitions
   * @throws IllegalArgumentException if {@code size} is nonpositive
   */
  public static <T> Iterable<List<T>> partition(final Iterable<T> iterable, final int size) {
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
   * Divides an iterable into unmodifiable sublists of the given size, padding the final iterable
   * with null values if necessary. For example, partitioning an iterable containing {@code [a, b,
   * c, d, e]} with a partition size of 3 yields {@code [[a, b, c], [d, e, null]]} -- an outer
   * iterable containing two inner lists of three elements each, all in the original order.
   *
   * <p>Iterators returned by the returned iterable do not support the {@link Iterator#remove()}
   * method.
   *
   * @param iterable the iterable to return a partitioned view of
   * @param size the desired size of each partition
   * @return an iterable of unmodifiable lists containing the elements of {@code iterable} divided
   *     into partitions (the final iterable may have trailing null elements)
   * @throws IllegalArgumentException if {@code size} is nonpositive
   */
  public static <T> Iterable<List<T>> paddedPartition(final Iterable<T> iterable, final int size) {
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
   * Returns a view of {@code unfiltered} containing all elements that satisfy the input predicate
   * {@code retainIfTrue}. The returned iterable's iterator does not support {@code remove()}.
   *
   * <p><b>{@code Stream} equivalent:</b> {@link Stream#filter}.
   */
  public static <T> Iterable<T> filter(
      final Iterable<T> unfiltered, final Predicate<? super T> retainIfTrue) {
    checkNotNull(unfiltered);
    checkNotNull(retainIfTrue);
    return new FluentIterable<T>() {
      @Override
      public Iterator<T> iterator() {
        return Iterators.filter(unfiltered.iterator(), retainIfTrue);
      }

      @Override
      public void forEach(Consumer<? super T> action) {
        checkNotNull(action);
        unfiltered.forEach(
            (T a) -> {
              if (retainIfTrue.test(a)) {
                action.accept(a);
              }
            });
      }

      @Override
      public Spliterator<T> spliterator() {
        return CollectSpliterators.filter(unfiltered.spliterator(), retainIfTrue);
      }
    };
  }

  /**
   * Returns a view of {@code unfiltered} containing all elements that are of the type {@code
   * desiredType}. The returned iterable's iterator does not support {@code remove()}.
   *
   * <p><b>{@code Stream} equivalent:</b> {@code stream.filter(type::isInstance).map(type::cast)}.
   * This does perform a little more work than necessary, so another option is to insert an
   * unchecked cast at some later point:
   *
   * <pre>
   * {@code @SuppressWarnings("unchecked") // safe because of ::isInstance check
   * ImmutableList<NewType> result =
   *     (ImmutableList) stream.filter(NewType.class::isInstance).collect(toImmutableList());}
   * </pre>
   */
  @SuppressWarnings("unchecked")
  @GwtIncompatible // Class.isInstance
  public static <T> Iterable<T> filter(final Iterable<?> unfiltered, final Class<T> desiredType) {
    checkNotNull(unfiltered);
    checkNotNull(desiredType);
    return (Iterable<T>) filter(unfiltered, Predicates.instanceOf(desiredType));
  }

  /**
   * Returns {@code true} if any element in {@code iterable} satisfies the predicate.
   *
   * <p><b>{@code Stream} equivalent:</b> {@link Stream#anyMatch}.
   */
  public static <T> boolean any(Iterable<T> iterable, Predicate<? super T> predicate) {
    return Iterators.any(iterable.iterator(), predicate);
  }

  /**
   * Returns {@code true} if every element in {@code iterable} satisfies the predicate. If {@code
   * iterable} is empty, {@code true} is returned.
   *
   * <p><b>{@code Stream} equivalent:</b> {@link Stream#allMatch}.
   */
  public static <T> boolean all(Iterable<T> iterable, Predicate<? super T> predicate) {
    return Iterators.all(iterable.iterator(), predicate);
  }

  /**
   * Returns the first element in {@code iterable} that satisfies the given predicate; use this
   * method only when such an element is known to exist. If it is possible that <i>no</i> element
   * will match, use {@link #tryFind} or {@link #find(Iterable, Predicate, Object)} instead.
   *
   * <p><b>{@code Stream} equivalent:</b> {@code stream.filter(predicate).findFirst().get()}
   *
   * @throws NoSuchElementException if no element in {@code iterable} matches the given predicate
   */
  public static <T> T find(Iterable<T> iterable, Predicate<? super T> predicate) {
    return Iterators.find(iterable.iterator(), predicate);
  }

  /**
   * Returns the first element in {@code iterable} that satisfies the given predicate, or {@code
   * defaultValue} if none found. Note that this can usually be handled more naturally using {@code
   * tryFind(iterable, predicate).or(defaultValue)}.
   *
   * <p><b>{@code Stream} equivalent:</b> {@code
   * stream.filter(predicate).findFirst().orElse(defaultValue)}
   *
   * @since 7.0
   */
  public static <T> @Nullable T find(
      Iterable<? extends T> iterable, Predicate<? super T> predicate, @Nullable T defaultValue) {
    return Iterators.find(iterable.iterator(), predicate, defaultValue);
  }

  /**
   * Returns an {@link Optional} containing the first element in {@code iterable} that satisfies the
   * given predicate, if such an element exists.
   *
   * <p><b>Warning:</b> avoid using a {@code predicate} that matches {@code null}. If {@code null}
   * is matched in {@code iterable}, a NullPointerException will be thrown.
   *
   * <p><b>{@code Stream} equivalent:</b> {@code stream.filter(predicate).findFirst()}
   *
   * @since 11.0
   */
  public static <T> Optional<T> tryFind(Iterable<T> iterable, Predicate<? super T> predicate) {
    return Iterators.tryFind(iterable.iterator(), predicate);
  }

  /**
   * Returns the index in {@code iterable} of the first element that satisfies the provided {@code
   * predicate}, or {@code -1} if the Iterable has no such elements.
   *
   * <p>More formally, returns the lowest index {@code i} such that {@code
   * predicate.apply(Iterables.get(iterable, i))} returns {@code true}, or {@code -1} if there is no
   * such index.
   *
   * @since 2.0
   */
  public static <T> int indexOf(Iterable<T> iterable, Predicate<? super T> predicate) {
    return Iterators.indexOf(iterable.iterator(), predicate);
  }

  /**
   * Returns a view containing the result of applying {@code function} to each element of {@code
   * fromIterable}.
   *
   * <p>The returned iterable's iterator supports {@code remove()} if {@code fromIterable}'s
   * iterator does. After a successful {@code remove()} call, {@code fromIterable} no longer
   * contains the corresponding element.
   *
   * <p>If the input {@code Iterable} is known to be a {@code List} or other {@code Collection},
   * consider {@link Lists#transform} and {@link Collections2#transform}.
   *
   * <p><b>{@code Stream} equivalent:</b> {@link Stream#map}
   */
  public static <F, T> Iterable<T> transform(
      final Iterable<F> fromIterable, final Function<? super F, ? extends T> function) {
    checkNotNull(fromIterable);
    checkNotNull(function);
    return new FluentIterable<T>() {
      @Override
      public Iterator<T> iterator() {
        return Iterators.transform(fromIterable.iterator(), function);
      }

      @Override
      public void forEach(Consumer<? super T> action) {
        checkNotNull(action);
        fromIterable.forEach((F f) -> action.accept(function.apply(f)));
      }

      @Override
      public Spliterator<T> spliterator() {
        return CollectSpliterators.map(fromIterable.spliterator(), function);
      }
    };
  }

  /**
   * Returns the element at the specified position in an iterable.
   *
   * <p><b>{@code Stream} equivalent:</b> {@code stream.skip(position).findFirst().get()} (throws
   * {@code NoSuchElementException} if out of bounds)
   *
   * @param position position of the element to return
   * @return the element at the specified position in {@code iterable}
   * @throws IndexOutOfBoundsException if {@code position} is negative or greater than or equal to
   *     the size of {@code iterable}
   */
  public static <T> T get(Iterable<T> iterable, int position) {
    checkNotNull(iterable);
    return (iterable instanceof List)
        ? ((List<T>) iterable).get(position)
        : Iterators.get(iterable.iterator(), position);
  }

  /**
   * Returns the element at the specified position in an iterable or a default value otherwise.
   *
   * <p><b>{@code Stream} equivalent:</b> {@code
   * stream.skip(position).findFirst().orElse(defaultValue)} (returns the default value if the index
   * is out of bounds)
   *
   * @param position position of the element to return
   * @param defaultValue the default value to return if {@code position} is greater than or equal to
   *     the size of the iterable
   * @return the element at the specified position in {@code iterable} or {@code defaultValue} if
   *     {@code iterable} contains fewer than {@code position + 1} elements.
   * @throws IndexOutOfBoundsException if {@code position} is negative
   * @since 4.0
   */
  public static <T> @Nullable T get(
      Iterable<? extends T> iterable, int position, @Nullable T defaultValue) {
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
   * Returns the first element in {@code iterable} or {@code defaultValue} if the iterable is empty.
   * The {@link Iterators} analog to this method is {@link Iterators#getNext}.
   *
   * <p>If no default value is desired (and the caller instead wants a {@link
   * NoSuchElementException} to be thrown), it is recommended that {@code
   * iterable.iterator().next()} is used instead.
   *
   * <p>To get the only element in a single-element {@code Iterable}, consider using {@link
   * #getOnlyElement(Iterable)} or {@link #getOnlyElement(Iterable, Object)} instead.
   *
   * <p><b>{@code Stream} equivalent:</b> {@code stream.findFirst().orElse(defaultValue)}
   *
   * @param defaultValue the default value to return if the iterable is empty
   * @return the first element of {@code iterable} or the default value
   * @since 7.0
   */
  public static <T> @Nullable T getFirst(Iterable<? extends T> iterable, @Nullable T defaultValue) {
    return Iterators.getNext(iterable.iterator(), defaultValue);
  }

  /**
   * Returns the last element of {@code iterable}. If {@code iterable} is a {@link List} with {@link
   * RandomAccess} support, then this operation is guaranteed to be {@code O(1)}.
   *
   * <p><b>{@code Stream} equivalent:</b> {@link Streams#findLast Streams.findLast(stream).get()}
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
   * Returns the last element of {@code iterable} or {@code defaultValue} if the iterable is empty.
   * If {@code iterable} is a {@link List} with {@link RandomAccess} support, then this operation is
   * guaranteed to be {@code O(1)}.
   *
   * <p><b>{@code Stream} equivalent:</b> {@code Streams.findLast(stream).orElse(defaultValue)}
   *
   * @param defaultValue the value to return if {@code iterable} is empty
   * @return the last element of {@code iterable} or the default value
   * @since 3.0
   */
  public static <T> @Nullable T getLast(Iterable<? extends T> iterable, @Nullable T defaultValue) {
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
   * Returns a view of {@code iterable} that skips its first {@code numberToSkip} elements. If
   * {@code iterable} contains fewer than {@code numberToSkip} elements, the returned iterable skips
   * all of its elements.
   *
   * <p>Modifications to the underlying {@link Iterable} before a call to {@code iterator()} are
   * reflected in the returned iterator. That is, the iterator skips the first {@code numberToSkip}
   * elements that exist when the {@code Iterator} is created, not when {@code skip()} is called.
   *
   * <p>The returned iterable's iterator supports {@code remove()} if the iterator of the underlying
   * iterable supports it. Note that it is <i>not</i> possible to delete the last skipped element by
   * immediately calling {@code remove()} on that iterator, as the {@code Iterator} contract states
   * that a call to {@code remove()} before a call to {@code next()} will throw an {@link
   * IllegalStateException}.
   *
   * <p><b>{@code Stream} equivalent:</b> {@link Stream#skip}
   *
   * @since 3.0
   */
  public static <T> Iterable<T> skip(final Iterable<T> iterable, final int numberToSkip) {
    checkNotNull(iterable);
    checkArgument(numberToSkip >= 0, "number to skip cannot be negative");

    return new FluentIterable<T>() {
      @Override
      public Iterator<T> iterator() {
        if (iterable instanceof List) {
          final List<T> list = (List<T>) iterable;
          int toSkip = Math.min(list.size(), numberToSkip);
          return list.subList(toSkip, list.size()).iterator();
        }
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

      @Override
      public Spliterator<T> spliterator() {
        if (iterable instanceof List) {
          final List<T> list = (List<T>) iterable;
          int toSkip = Math.min(list.size(), numberToSkip);
          return list.subList(toSkip, list.size()).spliterator();
        } else {
          return Streams.stream(iterable).skip(numberToSkip).spliterator();
        }
      }
    };
  }

  /**
   * Returns a view of {@code iterable} containing its first {@code limitSize} elements. If {@code
   * iterable} contains fewer than {@code limitSize} elements, the returned view contains all of its
   * elements. The returned iterable's iterator supports {@code remove()} if {@code iterable}'s
   * iterator does.
   *
   * <p><b>{@code Stream} equivalent:</b> {@link Stream#limit}
   *
   * @param iterable the iterable to limit
   * @param limitSize the maximum number of elements in the returned iterable
   * @throws IllegalArgumentException if {@code limitSize} is negative
   * @since 3.0
   */
  public static <T> Iterable<T> limit(final Iterable<T> iterable, final int limitSize) {
    checkNotNull(iterable);
    checkArgument(limitSize >= 0, "limit is negative");
    return new FluentIterable<T>() {
      @Override
      public Iterator<T> iterator() {
        return Iterators.limit(iterable.iterator(), limitSize);
      }

      @Override
      public Spliterator<T> spliterator() {
        return Streams.stream(iterable).limit(limitSize).spliterator();
      }
    };
  }

  /**
   * Returns a view of the supplied iterable that wraps each generated {@link Iterator} through
   * {@link Iterators#consumingIterator(Iterator)}.
   *
   * <p>Note: If {@code iterable} is a {@link Queue}, the returned iterable will get entries from
   * {@link Queue#remove()} since {@link Queue}'s iteration order is undefined. Calling {@link
   * Iterator#hasNext()} on a generated iterator from the returned iterable may cause an item to be
   * immediately dequeued for return on a subsequent call to {@link Iterator#next()}.
   *
   * @param iterable the iterable to wrap
   * @return a view of the supplied iterable that wraps each generated iterator through {@link
   *     Iterators#consumingIterator(Iterator)}; for queues, an iterable that generates iterators
   *     that return and consume the queue's elements in queue order
   * @see Iterators#consumingIterator(Iterator)
   * @since 2.0
   */
  public static <T> Iterable<T> consumingIterable(final Iterable<T> iterable) {
    checkNotNull(iterable);

    return new FluentIterable<T>() {
      @Override
      public Iterator<T> iterator() {
        return (iterable instanceof Queue)
            ? new ConsumingQueueIterator<>((Queue<T>) iterable)
            : Iterators.consumingIterator(iterable.iterator());
      }

      @Override
      public String toString() {
        return "Iterables.consumingIterable(...)";
      }
    };
  }

  // Methods only in Iterables, not in Iterators

  /**
   * Determines if the given iterable contains no elements.
   *
   * <p>There is no precise {@link Iterator} equivalent to this method, since one can only ask an
   * iterator whether it has any elements <i>remaining</i> (which one does using {@link
   * Iterator#hasNext}).
   *
   * <p><b>{@code Stream} equivalent:</b> {@code !stream.findAny().isPresent()}
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
   * Returns an iterable over the merged contents of all given {@code iterables}. Equivalent entries
   * will not be de-duplicated.
   *
   * <p>Callers must ensure that the source {@code iterables} are in non-descending order as this
   * method does not sort its input.
   *
   * <p>For any equivalent elements across all {@code iterables}, it is undefined which element is
   * returned first.
   *
   * @since 11.0
   */
  @Beta
  public static <T> Iterable<T> mergeSorted(
      final Iterable<? extends Iterable<? extends T>> iterables,
      final Comparator<? super T> comparator) {
    checkNotNull(iterables, "iterables");
    checkNotNull(comparator, "comparator");
    Iterable<T> iterable =
        new FluentIterable<T>() {
          @Override
          public Iterator<T> iterator() {
            return Iterators.mergeSorted(
                Iterables.transform(iterables, Iterables.<T>toIterator()), comparator);
          }
        };
    return new UnmodifiableIterable<>(iterable);
  }

  // TODO(user): Is this the best place for this? Move to fluent functions?
  // Useful as a public method?
  static <T> Function<Iterable<? extends T>, Iterator<? extends T>> toIterator() {
    return new Function<Iterable<? extends T>, Iterator<? extends T>>() {
      @Override
      public Iterator<? extends T> apply(Iterable<? extends T> iterable) {
        return iterable.iterator();
      }
    };
  }
}
