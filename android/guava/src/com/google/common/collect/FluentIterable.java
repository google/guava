/*
 * Copyright (C) 2008 The Guava Authors
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

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.annotations.Beta;
import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.SortedSet;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;

/**
 * An expanded {@code Iterable} API, providing functionality similar to Java 8's powerful <a href=
 * "https://docs.oracle.com/javase/8/docs/api/java/util/stream/package-summary.html#package.description"
 * >streams library</a> in a slightly different way.
 *
 * <p>The following types of methods are provided:
 *
 * <ul>
 *   <li>chaining methods which return a new {@code FluentIterable} based in some way on the
 *       contents of the current one (for example {@link #transform})
 *   <li>element extraction methods which facilitate the retrieval of certain elements (for example
 *       {@link #last})
 *   <li>query methods which answer questions about the {@code FluentIterable}'s contents (for
 *       example {@link #anyMatch})
 *   <li>conversion methods which copy the {@code FluentIterable}'s contents into a new collection
 *       or array (for example {@link #toList})
 * </ul>
 *
 * <p>Several lesser-used features are currently available only as static methods on the {@link
 * Iterables} class.
 *
 * <p><a name="streams"></a>
 *
 * <h3>Comparison to streams</h3>
 *
 * <p>Starting with Java 8, the core Java class libraries provide a new "Streams" library (in {@code
 * java.util.stream}), which is similar to {@code FluentIterable} but generally more powerful. Key
 * differences include:
 *
 * <ul>
 *   <li>A stream is <i>single-use</i>; it becomes invalid as soon as any "terminal operation" such
 *       as {@code findFirst()} or {@code iterator()} is invoked. (Even though {@code Stream}
 *       contains all the right method <i>signatures</i> to implement {@link Iterable}, it does not
 *       actually do so, to avoid implying repeat-iterability.) {@code FluentIterable}, on the other
 *       hand, is multiple-use, and does implement {@link Iterable}.
 *   <li>Streams offer many features not found here, including {@code min/max}, {@code distinct},
 *       {@code reduce}, {@code sorted}, the very powerful {@code collect}, and built-in support for
 *       parallelizing stream operations.
 *   <li>{@code FluentIterable} contains several features not available on {@code Stream}, which are
 *       noted in the method descriptions below.
 *   <li>Streams include primitive-specialized variants such as {@code IntStream}, the use of which
 *       is strongly recommended.
 *   <li>Streams are standard Java, not requiring a third-party dependency (but do render your code
 *       incompatible with Java 7 and earlier).
 * </ul>
 *
 * <h3>Example</h3>
 *
 * <p>Here is an example that accepts a list from a database call, filters it based on a predicate,
 * transforms it by invoking {@code toString()} on each element, and returns the first 10 elements
 * as a {@code List}:
 *
 * <pre>{@code
 * ImmutableList<String> results =
 *     FluentIterable.from(database.getClientList())
 *         .filter(Client::isActiveInLastMonth)
 *         .transform(Object::toString)
 *         .limit(10)
 *         .toList();
 * }</pre>
 *
 * The approximate stream equivalent is:
 *
 * <pre>{@code
 * List<String> results =
 *     database.getClientList()
 *         .stream()
 *         .filter(Client::isActiveInLastMonth)
 *         .map(Object::toString)
 *         .limit(10)
 *         .collect(Collectors.toList());
 * }</pre>
 *
 * @author Marcin Mikosik
 * @since 12.0
 */
@GwtCompatible(emulated = true)
public abstract class FluentIterable<E> implements Iterable<E> {
  // We store 'iterable' and use it instead of 'this' to allow Iterables to perform instanceof
  // checks on the _original_ iterable when FluentIterable.from is used.
  // To avoid a self retain cycle under j2objc, we store Optional.absent() instead of
  // Optional.of(this). To access the iterator delegate, call #getDelegate(), which converts to
  // absent() back to 'this'.
  private final Optional<Iterable<E>> iterableDelegate;

  /** Constructor for use by subclasses. */
  protected FluentIterable() {
    this.iterableDelegate = Optional.absent();
  }

  FluentIterable(Iterable<E> iterable) {
    checkNotNull(iterable);
    this.iterableDelegate = Optional.fromNullable(this != iterable ? iterable : null);
  }

  private Iterable<E> getDelegate() {
    return iterableDelegate.or(this);
  }

  /**
   * Returns a fluent iterable that wraps {@code iterable}, or {@code iterable} itself if it is
   * already a {@code FluentIterable}.
   *
   * <p><b>{@code Stream} equivalent:</b> {@code iterable.stream()} if {@code iterable} is a {@link
   * Collection}; {@code StreamSupport.stream(iterable.spliterator(), false)} otherwise.
   */
  public static <E> FluentIterable<E> from(final Iterable<E> iterable) {
    return (iterable instanceof FluentIterable)
        ? (FluentIterable<E>) iterable
        : new FluentIterable<E>(iterable) {
          @Override
          public Iterator<E> iterator() {
            return iterable.iterator();
          }
        };
  }

  /**
   * Returns a fluent iterable containing {@code elements} in the specified order.
   *
   * <p>The returned iterable is an unmodifiable view of the input array.
   *
   * <p><b>{@code Stream} equivalent:</b> {@link java.util.stream.Stream#of(Object[])
   * Stream.of(T...)}.
   *
   * @since 20.0 (since 18.0 as an overload of {@code of})
   */
  @Beta
  public static <E> FluentIterable<E> from(E[] elements) {
    return from(Arrays.asList(elements));
  }

  /**
   * Construct a fluent iterable from another fluent iterable. This is obviously never necessary,
   * but is intended to help call out cases where one migration from {@code Iterable} to {@code
   * FluentIterable} has obviated the need to explicitly convert to a {@code FluentIterable}.
   *
   * @deprecated instances of {@code FluentIterable} don't need to be converted to {@code
   *     FluentIterable}
   */
  @Deprecated
  public static <E> FluentIterable<E> from(FluentIterable<E> iterable) {
    return checkNotNull(iterable);
  }

  /**
   * Returns a fluent iterable that combines two iterables. The returned iterable has an iterator
   * that traverses the elements in {@code a}, followed by the elements in {@code b}. The source
   * iterators are not polled until necessary.
   *
   * <p>The returned iterable's iterator supports {@code remove()} when the corresponding input
   * iterator supports it.
   *
   * <p><b>{@code Stream} equivalent:</b> {@link Stream#concat}.
   *
   * @since 20.0
   */
  @Beta
  public static <T> FluentIterable<T> concat(Iterable<? extends T> a, Iterable<? extends T> b) {
    return concatNoDefensiveCopy(a, b);
  }

  /**
   * Returns a fluent iterable that combines three iterables. The returned iterable has an iterator
   * that traverses the elements in {@code a}, followed by the elements in {@code b}, followed by
   * the elements in {@code c}. The source iterators are not polled until necessary.
   *
   * <p>The returned iterable's iterator supports {@code remove()} when the corresponding input
   * iterator supports it.
   *
   * <p><b>{@code Stream} equivalent:</b> use nested calls to {@link Stream#concat}, or see the
   * advice in {@link #concat(Iterable...)}.
   *
   * @since 20.0
   */
  @Beta
  public static <T> FluentIterable<T> concat(
      Iterable<? extends T> a, Iterable<? extends T> b, Iterable<? extends T> c) {
    return concatNoDefensiveCopy(a, b, c);
  }

  /**
   * Returns a fluent iterable that combines four iterables. The returned iterable has an iterator
   * that traverses the elements in {@code a}, followed by the elements in {@code b}, followed by
   * the elements in {@code c}, followed by the elements in {@code d}. The source iterators are not
   * polled until necessary.
   *
   * <p>The returned iterable's iterator supports {@code remove()} when the corresponding input
   * iterator supports it.
   *
   * <p><b>{@code Stream} equivalent:</b> use nested calls to {@link Stream#concat}, or see the
   * advice in {@link #concat(Iterable...)}.
   *
   * @since 20.0
   */
  @Beta
  public static <T> FluentIterable<T> concat(
      Iterable<? extends T> a,
      Iterable<? extends T> b,
      Iterable<? extends T> c,
      Iterable<? extends T> d) {
    return concatNoDefensiveCopy(a, b, c, d);
  }

  /**
   * Returns a fluent iterable that combines several iterables. The returned iterable has an
   * iterator that traverses the elements of each iterable in {@code inputs}. The input iterators
   * are not polled until necessary.
   *
   * <p>The returned iterable's iterator supports {@code remove()} when the corresponding input
   * iterator supports it.
   *
   * <p><b>{@code Stream} equivalent:</b> to concatenate an arbitrary number of streams, use {@code
   * Stream.of(stream1, stream2, ...).flatMap(s -> s)}. If the sources are iterables, use {@code
   * Stream.of(iter1, iter2, ...).flatMap(Streams::stream)}.
   *
   * @throws NullPointerException if any of the provided iterables is {@code null}
   * @since 20.0
   */
  @Beta
  public static <T> FluentIterable<T> concat(Iterable<? extends T>... inputs) {
    return concatNoDefensiveCopy(Arrays.copyOf(inputs, inputs.length));
  }

  /**
   * Returns a fluent iterable that combines several iterables. The returned iterable has an
   * iterator that traverses the elements of each iterable in {@code inputs}. The input iterators
   * are not polled until necessary.
   *
   * <p>The returned iterable's iterator supports {@code remove()} when the corresponding input
   * iterator supports it. The methods of the returned iterable may throw {@code
   * NullPointerException} if any of the input iterators is {@code null}.
   *
   * <p><b>{@code Stream} equivalent:</b> {@code streamOfStreams.flatMap(s -> s)} or {@code
   * streamOfIterables.flatMap(Streams::stream)}. (See {@link Streams#stream}.)
   *
   * @since 20.0
   */
  @Beta
  public static <T> FluentIterable<T> concat(
      final Iterable<? extends Iterable<? extends T>> inputs) {
    checkNotNull(inputs);
    return new FluentIterable<T>() {
      @Override
      public Iterator<T> iterator() {
        return Iterators.concat(Iterators.transform(inputs.iterator(), Iterables.<T>toIterator()));
      }
    };
  }

  /** Concatenates a varargs array of iterables without making a defensive copy of the array. */
  private static <T> FluentIterable<T> concatNoDefensiveCopy(
      final Iterable<? extends T>... inputs) {
    for (Iterable<? extends T> input : inputs) {
      checkNotNull(input);
    }
    return new FluentIterable<T>() {
      @Override
      public Iterator<T> iterator() {
        return Iterators.concat(
            /* lazily generate the iterators on each input only as needed */
            new AbstractIndexedListIterator<Iterator<? extends T>>(inputs.length) {
              @Override
              public Iterator<? extends T> get(int i) {
                return inputs[i].iterator();
              }
            });
      }
    };
  }

  /**
   * Returns a fluent iterable containing no elements.
   *
   * <p><b>{@code Stream} equivalent:</b> {@code Stream.empty()}.
   *
   * @since 20.0
   */
  @Beta
  public static <E> FluentIterable<E> of() {
    return FluentIterable.from(ImmutableList.<E>of());
  }

  /**
   * Returns a fluent iterable containing the specified elements in order.
   *
   * <p><b>{@code Stream} equivalent:</b> {@link java.util.stream.Stream#of(Object[])
   * Stream.of(T...)}.
   *
   * @since 20.0
   */
  @Beta
  public static <E> FluentIterable<E> of(@NullableDecl E element, E... elements) {
    return from(Lists.asList(element, elements));
  }

  /**
   * Returns a string representation of this fluent iterable, with the format {@code [e1, e2, ...,
   * en]}.
   *
   * <p><b>{@code Stream} equivalent:</b> {@code stream.collect(Collectors.joining(", ", "[", "]"))}
   * or (less efficiently) {@code stream.collect(Collectors.toList()).toString()}.
   */
  @Override
  public String toString() {
    return Iterables.toString(getDelegate());
  }

  /**
   * Returns the number of elements in this fluent iterable.
   *
   * <p><b>{@code Stream} equivalent:</b> {@code stream.count()}.
   */
  public final int size() {
    return Iterables.size(getDelegate());
  }

  /**
   * Returns {@code true} if this fluent iterable contains any object for which {@code
   * equals(target)} is true.
   *
   * <p><b>{@code Stream} equivalent:</b> {@code stream.anyMatch(Predicate.isEqual(target))}.
   */
  public final boolean contains(@NullableDecl Object target) {
    return Iterables.contains(getDelegate(), target);
  }

  /**
   * Returns a fluent iterable whose {@code Iterator} cycles indefinitely over the elements of this
   * fluent iterable.
   *
   * <p>That iterator supports {@code remove()} if {@code iterable.iterator()} does. After {@code
   * remove()} is called, subsequent cycles omit the removed element, which is no longer in this
   * fluent iterable. The iterator's {@code hasNext()} method returns {@code true} until this fluent
   * iterable is empty.
   *
   * <p><b>Warning:</b> Typical uses of the resulting iterator may produce an infinite loop. You
   * should use an explicit {@code break} or be certain that you will eventually remove all the
   * elements.
   *
   * <p><b>{@code Stream} equivalent:</b> if the source iterable has only a single element {@code
   * e}, use {@code Stream.generate(() -> e)}. Otherwise, collect your stream into a collection and
   * use {@code Stream.generate(() -> collection).flatMap(Collection::stream)}.
   */
  public final FluentIterable<E> cycle() {
    return from(Iterables.cycle(getDelegate()));
  }

  /**
   * Returns a fluent iterable whose iterators traverse first the elements of this fluent iterable,
   * followed by those of {@code other}. The iterators are not polled until necessary.
   *
   * <p>The returned iterable's {@code Iterator} supports {@code remove()} when the corresponding
   * {@code Iterator} supports it.
   *
   * <p><b>{@code Stream} equivalent:</b> {@link Stream#concat}.
   *
   * @since 18.0
   */
  @Beta
  public final FluentIterable<E> append(Iterable<? extends E> other) {
    return FluentIterable.concat(getDelegate(), other);
  }

  /**
   * Returns a fluent iterable whose iterators traverse first the elements of this fluent iterable,
   * followed by {@code elements}.
   *
   * <p><b>{@code Stream} equivalent:</b> {@code Stream.concat(thisStream, Stream.of(elements))}.
   *
   * @since 18.0
   */
  @Beta
  public final FluentIterable<E> append(E... elements) {
    return FluentIterable.concat(getDelegate(), Arrays.asList(elements));
  }

  /**
   * Returns the elements from this fluent iterable that satisfy a predicate. The resulting fluent
   * iterable's iterator does not support {@code remove()}.
   *
   * <p><b>{@code Stream} equivalent:</b> {@link Stream#filter} (same).
   */
  public final FluentIterable<E> filter(Predicate<? super E> predicate) {
    return from(Iterables.filter(getDelegate(), predicate));
  }

  /**
   * Returns the elements from this fluent iterable that are instances of class {@code type}.
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
  @GwtIncompatible // Class.isInstance
  public final <T> FluentIterable<T> filter(Class<T> type) {
    return from(Iterables.filter(getDelegate(), type));
  }

  /**
   * Returns {@code true} if any element in this fluent iterable satisfies the predicate.
   *
   * <p><b>{@code Stream} equivalent:</b> {@link Stream#anyMatch} (same).
   */
  public final boolean anyMatch(Predicate<? super E> predicate) {
    return Iterables.any(getDelegate(), predicate);
  }

  /**
   * Returns {@code true} if every element in this fluent iterable satisfies the predicate. If this
   * fluent iterable is empty, {@code true} is returned.
   *
   * <p><b>{@code Stream} equivalent:</b> {@link Stream#allMatch} (same).
   */
  public final boolean allMatch(Predicate<? super E> predicate) {
    return Iterables.all(getDelegate(), predicate);
  }

  /**
   * Returns an {@link Optional} containing the first element in this fluent iterable that satisfies
   * the given predicate, if such an element exists.
   *
   * <p><b>Warning:</b> avoid using a {@code predicate} that matches {@code null}. If {@code null}
   * is matched in this fluent iterable, a {@link NullPointerException} will be thrown.
   *
   * <p><b>{@code Stream} equivalent:</b> {@code stream.filter(predicate).findFirst()}.
   */
  public final Optional<E> firstMatch(Predicate<? super E> predicate) {
    return Iterables.tryFind(getDelegate(), predicate);
  }

  /**
   * Returns a fluent iterable that applies {@code function} to each element of this fluent
   * iterable.
   *
   * <p>The returned fluent iterable's iterator supports {@code remove()} if this iterable's
   * iterator does. After a successful {@code remove()} call, this fluent iterable no longer
   * contains the corresponding element.
   *
   * <p><b>{@code Stream} equivalent:</b> {@link Stream#map}.
   */
  public final <T> FluentIterable<T> transform(Function<? super E, T> function) {
    return from(Iterables.transform(getDelegate(), function));
  }

  /**
   * Applies {@code function} to each element of this fluent iterable and returns a fluent iterable
   * with the concatenated combination of results. {@code function} returns an Iterable of results.
   *
   * <p>The returned fluent iterable's iterator supports {@code remove()} if this function-returned
   * iterables' iterator does. After a successful {@code remove()} call, the returned fluent
   * iterable no longer contains the corresponding element.
   *
   * <p><b>{@code Stream} equivalent:</b> {@link Stream#flatMap} (using a function that produces
   * streams, not iterables).
   *
   * @since 13.0 (required {@code Function<E, Iterable<T>>} until 14.0)
   */
  public <T> FluentIterable<T> transformAndConcat(
      Function<? super E, ? extends Iterable<? extends T>> function) {
    return FluentIterable.concat(transform(function));
  }

  /**
   * Returns an {@link Optional} containing the first element in this fluent iterable. If the
   * iterable is empty, {@code Optional.absent()} is returned.
   *
   * <p><b>{@code Stream} equivalent:</b> if the goal is to obtain any element, {@link
   * Stream#findAny}; if it must specifically be the <i>first</i> element, {@code Stream#findFirst}.
   *
   * @throws NullPointerException if the first element is null; if this is a possibility, use {@code
   *     iterator().next()} or {@link Iterables#getFirst} instead.
   */
  public final Optional<E> first() {
    Iterator<E> iterator = getDelegate().iterator();
    return iterator.hasNext() ? Optional.of(iterator.next()) : Optional.<E>absent();
  }

  /**
   * Returns an {@link Optional} containing the last element in this fluent iterable. If the
   * iterable is empty, {@code Optional.absent()} is returned. If the underlying {@code iterable} is
   * a {@link List} with {@link java.util.RandomAccess} support, then this operation is guaranteed
   * to be {@code O(1)}.
   *
   * <p><b>{@code Stream} equivalent:</b> {@code stream.reduce((a, b) -> b)}.
   *
   * @throws NullPointerException if the last element is null; if this is a possibility, use {@link
   *     Iterables#getLast} instead.
   */
  public final Optional<E> last() {
    // Iterables#getLast was inlined here so we don't have to throw/catch a NSEE

    // TODO(kevinb): Support a concurrently modified collection?
    Iterable<E> iterable = getDelegate();
    if (iterable instanceof List) {
      List<E> list = (List<E>) iterable;
      if (list.isEmpty()) {
        return Optional.absent();
      }
      return Optional.of(list.get(list.size() - 1));
    }
    Iterator<E> iterator = iterable.iterator();
    if (!iterator.hasNext()) {
      return Optional.absent();
    }

    /*
     * TODO(kevinb): consider whether this "optimization" is worthwhile. Users with SortedSets tend
     * to know they are SortedSets and probably would not call this method.
     */
    if (iterable instanceof SortedSet) {
      SortedSet<E> sortedSet = (SortedSet<E>) iterable;
      return Optional.of(sortedSet.last());
    }

    while (true) {
      E current = iterator.next();
      if (!iterator.hasNext()) {
        return Optional.of(current);
      }
    }
  }

  /**
   * Returns a view of this fluent iterable that skips its first {@code numberToSkip} elements. If
   * this fluent iterable contains fewer than {@code numberToSkip} elements, the returned fluent
   * iterable skips all of its elements.
   *
   * <p>Modifications to this fluent iterable before a call to {@code iterator()} are reflected in
   * the returned fluent iterable. That is, the its iterator skips the first {@code numberToSkip}
   * elements that exist when the iterator is created, not when {@code skip()} is called.
   *
   * <p>The returned fluent iterable's iterator supports {@code remove()} if the {@code Iterator} of
   * this fluent iterable supports it. Note that it is <i>not</i> possible to delete the last
   * skipped element by immediately calling {@code remove()} on the returned fluent iterable's
   * iterator, as the {@code Iterator} contract states that a call to {@code * remove()} before a
   * call to {@code next()} will throw an {@link IllegalStateException}.
   *
   * <p><b>{@code Stream} equivalent:</b> {@link Stream#skip} (same).
   */
  public final FluentIterable<E> skip(int numberToSkip) {
    return from(Iterables.skip(getDelegate(), numberToSkip));
  }

  /**
   * Creates a fluent iterable with the first {@code size} elements of this fluent iterable. If this
   * fluent iterable does not contain that many elements, the returned fluent iterable will have the
   * same behavior as this fluent iterable. The returned fluent iterable's iterator supports {@code
   * remove()} if this fluent iterable's iterator does.
   *
   * <p><b>{@code Stream} equivalent:</b> {@link Stream#limit} (same).
   *
   * @param maxSize the maximum number of elements in the returned fluent iterable
   * @throws IllegalArgumentException if {@code size} is negative
   */
  public final FluentIterable<E> limit(int maxSize) {
    return from(Iterables.limit(getDelegate(), maxSize));
  }

  /**
   * Determines whether this fluent iterable is empty.
   *
   * <p><b>{@code Stream} equivalent:</b> {@code !stream.findAny().isPresent()}.
   */
  public final boolean isEmpty() {
    return !getDelegate().iterator().hasNext();
  }

  /**
   * Returns an {@code ImmutableList} containing all of the elements from this fluent iterable in
   * proper sequence.
   *
   * <p><b>{@code Stream} equivalent:</b> {@code ImmutableList.copyOf(stream.iterator())}, or pass
   * {@link ImmutableList#toImmutableList} to {@code stream.collect()}.
   *
   * @throws NullPointerException if any element is {@code null}
   * @since 14.0 (since 12.0 as {@code toImmutableList()}).
   */
  public final ImmutableList<E> toList() {
    return ImmutableList.copyOf(getDelegate());
  }

  /**
   * Returns an {@code ImmutableList} containing all of the elements from this {@code
   * FluentIterable} in the order specified by {@code comparator}. To produce an {@code
   * ImmutableList} sorted by its natural ordering, use {@code toSortedList(Ordering.natural())}.
   *
   * <p><b>{@code Stream} equivalent:</b> {@code
   * ImmutableList.copyOf(stream.sorted(comparator).iterator())}, or pass {@link
   * ImmutableList#toImmutableList} to {@code stream.sorted(comparator).collect()}.
   *
   * @param comparator the function by which to sort list elements
   * @throws NullPointerException if any element of this iterable is {@code null}
   * @since 14.0 (since 13.0 as {@code toSortedImmutableList()}).
   */
  public final ImmutableList<E> toSortedList(Comparator<? super E> comparator) {
    return Ordering.from(comparator).immutableSortedCopy(getDelegate());
  }

  /**
   * Returns an {@code ImmutableSet} containing all of the elements from this fluent iterable with
   * duplicates removed.
   *
   * <p><b>{@code Stream} equivalent:</b> {@code ImmutableSet.copyOf(stream.iterator())}, or pass
   * {@link ImmutableSet#toImmutableSet} to {@code stream.collect()}.
   *
   * @throws NullPointerException if any element is {@code null}
   * @since 14.0 (since 12.0 as {@code toImmutableSet()}).
   */
  public final ImmutableSet<E> toSet() {
    return ImmutableSet.copyOf(getDelegate());
  }

  /**
   * Returns an {@code ImmutableSortedSet} containing all of the elements from this {@code
   * FluentIterable} in the order specified by {@code comparator}, with duplicates (determined by
   * {@code comparator.compare(x, y) == 0}) removed. To produce an {@code ImmutableSortedSet} sorted
   * by its natural ordering, use {@code toSortedSet(Ordering.natural())}.
   *
   * <p><b>{@code Stream} equivalent:</b> {@code ImmutableSortedSet.copyOf(comparator,
   * stream.iterator())}, or pass {@link ImmutableSortedSet#toImmutableSortedSet} to {@code
   * stream.collect()}.
   *
   * @param comparator the function by which to sort set elements
   * @throws NullPointerException if any element of this iterable is {@code null}
   * @since 14.0 (since 12.0 as {@code toImmutableSortedSet()}).
   */
  public final ImmutableSortedSet<E> toSortedSet(Comparator<? super E> comparator) {
    return ImmutableSortedSet.copyOf(comparator, getDelegate());
  }

  /**
   * Returns an {@code ImmutableMultiset} containing all of the elements from this fluent iterable.
   *
   * <p><b>{@code Stream} equivalent:</b> {@code ImmutableMultiset.copyOf(stream.iterator())}, or
   * pass {@link ImmutableMultiset#toImmutableMultiset} to {@code stream.collect()}.
   *
   * @throws NullPointerException if any element is null
   * @since 19.0
   */
  public final ImmutableMultiset<E> toMultiset() {
    return ImmutableMultiset.copyOf(getDelegate());
  }

  /**
   * Returns an immutable map whose keys are the distinct elements of this {@code FluentIterable}
   * and whose value for each key was computed by {@code valueFunction}. The map's iteration order
   * is the order of the first appearance of each key in this iterable.
   *
   * <p>When there are multiple instances of a key in this iterable, it is unspecified whether
   * {@code valueFunction} will be applied to more than one instance of that key and, if it is,
   * which result will be mapped to that key in the returned map.
   *
   * <p><b>{@code Stream} equivalent:</b> use {@code stream.collect(ImmutableMap.toImmutableMap(k ->
   * k, valueFunction))}. {@code ImmutableMap.copyOf(stream.collect(Collectors.toMap(k -> k,
   * valueFunction)))} behaves similarly, but may not preserve the order of entries.
   *
   * @throws NullPointerException if any element of this iterable is {@code null}, or if {@code
   *     valueFunction} produces {@code null} for any key
   * @since 14.0
   */
  public final <V> ImmutableMap<E, V> toMap(Function<? super E, V> valueFunction) {
    return Maps.toMap(getDelegate(), valueFunction);
  }

  /**
   * Creates an index {@code ImmutableListMultimap} that contains the results of applying a
   * specified function to each item in this {@code FluentIterable} of values. Each element of this
   * iterable will be stored as a value in the resulting multimap, yielding a multimap with the same
   * size as this iterable. The key used to store that value in the multimap will be the result of
   * calling the function on that value. The resulting multimap is created as an immutable snapshot.
   * In the returned multimap, keys appear in the order they are first encountered, and the values
   * corresponding to each key appear in the same order as they are encountered.
   *
   * <p><b>{@code Stream} equivalent:</b> {@code stream.collect(Collectors.groupingBy(keyFunction))}
   * behaves similarly, but returns a mutable {@code Map<K, List<E>>} instead, and may not preserve
   * the order of entries.
   *
   * @param keyFunction the function used to produce the key for each value
   * @throws NullPointerException if any element of this iterable is {@code null}, or if {@code
   *     keyFunction} produces {@code null} for any key
   * @since 14.0
   */
  public final <K> ImmutableListMultimap<K, E> index(Function<? super E, K> keyFunction) {
    return Multimaps.index(getDelegate(), keyFunction);
  }

  /**
   * Returns a map with the contents of this {@code FluentIterable} as its {@code values}, indexed
   * by keys derived from those values. In other words, each input value produces an entry in the
   * map whose key is the result of applying {@code keyFunction} to that value. These entries appear
   * in the same order as they appeared in this fluent iterable. Example usage:
   *
   * <pre>{@code
   * Color red = new Color("red", 255, 0, 0);
   * ...
   * FluentIterable<Color> allColors = FluentIterable.from(ImmutableSet.of(red, green, blue));
   *
   * Map<String, Color> colorForName = allColors.uniqueIndex(toStringFunction());
   * assertThat(colorForName).containsEntry("red", red);
   * }</pre>
   *
   * <p>If your index may associate multiple values with each key, use {@link #index(Function)
   * index}.
   *
   * <p><b>{@code Stream} equivalent:</b> use {@code
   * stream.collect(ImmutableMap.toImmutableMap(keyFunction, v -> v))}. {@code
   * ImmutableMap.copyOf(stream.collect(Collectors.toMap(keyFunction, v -> v)))}, but be aware that
   * this may not preserve the order of entries.
   *
   * @param keyFunction the function used to produce the key for each value
   * @return a map mapping the result of evaluating the function {@code keyFunction} on each value
   *     in this fluent iterable to that value
   * @throws IllegalArgumentException if {@code keyFunction} produces the same key for more than one
   *     value in this fluent iterable
   * @throws NullPointerException if any element of this iterable is {@code null}, or if {@code
   *     keyFunction} produces {@code null} for any key
   * @since 14.0
   */
  public final <K> ImmutableMap<K, E> uniqueIndex(Function<? super E, K> keyFunction) {
    return Maps.uniqueIndex(getDelegate(), keyFunction);
  }

  /**
   * Returns an array containing all of the elements from this fluent iterable in iteration order.
   *
   * <p><b>{@code Stream} equivalent:</b> if an object array is acceptable, use {@code
   * stream.toArray()}; if {@code type} is a class literal such as {@code MyType.class}, use {@code
   * stream.toArray(MyType[]::new)}. Otherwise use {@code stream.toArray( len -> (E[])
   * Array.newInstance(type, len))}.
   *
   * @param type the type of the elements
   * @return a newly-allocated array into which all the elements of this fluent iterable have been
   *     copied
   */
  @GwtIncompatible // Array.newArray(Class, int)
  public final E[] toArray(Class<E> type) {
    return Iterables.toArray(getDelegate(), type);
  }

  /**
   * Copies all the elements from this fluent iterable to {@code collection}. This is equivalent to
   * calling {@code Iterables.addAll(collection, this)}.
   *
   * <p><b>{@code Stream} equivalent:</b> {@code stream.forEachOrdered(collection::add)} or {@code
   * stream.forEach(collection::add)}.
   *
   * @param collection the collection to copy elements to
   * @return {@code collection}, for convenience
   * @since 14.0
   */
  @CanIgnoreReturnValue
  public final <C extends Collection<? super E>> C copyInto(C collection) {
    checkNotNull(collection);
    Iterable<E> iterable = getDelegate();
    if (iterable instanceof Collection) {
      collection.addAll(Collections2.cast(iterable));
    } else {
      for (E item : iterable) {
        collection.add(item);
      }
    }
    return collection;
  }

  /**
   * Returns a {@link String} containing all of the elements of this fluent iterable joined with
   * {@code joiner}.
   *
   * <p><b>{@code Stream} equivalent:</b> {@code joiner.join(stream.iterator())}, or, if you are not
   * using any optional {@code Joiner} features, {@code
   * stream.collect(Collectors.joining(delimiter)}.
   *
   * @since 18.0
   */
  @Beta
  public final String join(Joiner joiner) {
    return joiner.join(this);
  }

  /**
   * Returns the element at the specified position in this fluent iterable.
   *
   * <p><b>{@code Stream} equivalent:</b> {@code stream.skip(position).findFirst().get()} (but note
   * that this throws different exception types, and throws an exception if {@code null} would be
   * returned).
   *
   * @param position position of the element to return
   * @return the element at the specified position in this fluent iterable
   * @throws IndexOutOfBoundsException if {@code position} is negative or greater than or equal to
   *     the size of this fluent iterable
   */
  // TODO(kevinb): add @NullableDecl?
  public final E get(int position) {
    return Iterables.get(getDelegate(), position);
  }

  /** Function that transforms {@code Iterable<E>} into a fluent iterable. */
  private static class FromIterableFunction<E> implements Function<Iterable<E>, FluentIterable<E>> {
    @Override
    public FluentIterable<E> apply(Iterable<E> fromObject) {
      return FluentIterable.from(fromObject);
    }
  }
}
