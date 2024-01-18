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
import static com.google.common.base.Preconditions.checkElementIndex;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkPositionIndex;
import static com.google.common.base.Preconditions.checkPositionIndexes;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.CollectPreconditions.checkNonnegative;
import static com.google.common.collect.CollectPreconditions.checkRemove;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.annotations.J2ktIncompatible;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
import com.google.common.base.Objects;
import com.google.common.math.IntMath;
import com.google.common.primitives.Ints;
import java.io.Serializable;
import java.math.RoundingMode;
import java.util.AbstractList;
import java.util.AbstractSequentialList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import java.util.RandomAccess;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;
import javax.annotation.CheckForNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Static utility methods pertaining to {@link List} instances. Also see this class's counterparts
 * {@link Sets}, {@link Maps} and {@link Queues}.
 *
 * <p>See the Guava User Guide article on <a href=
 * "https://github.com/google/guava/wiki/CollectionUtilitiesExplained#lists">{@code Lists}</a>.
 *
 * @author Kevin Bourrillion
 * @author Mike Bostock
 * @author Louis Wasserman
 * @since 2.0
 */
@GwtCompatible(emulated = true)
@ElementTypesAreNonnullByDefault
public final class Lists {
  private Lists() {}

  // ArrayList

  /**
   * Creates a <i>mutable</i>, empty {@code ArrayList} instance (for Java 6 and earlier).
   *
   * <p><b>Note:</b> if mutability is not required, use {@link ImmutableList#of()} instead.
   *
   * <p><b>Note:</b> this method is now unnecessary and should be treated as deprecated. Instead,
   * use the {@code ArrayList} {@linkplain ArrayList#ArrayList() constructor} directly, taking
   * advantage of <a href="http://goo.gl/iz2Wi">"diamond" syntax</a>.
   */
  @GwtCompatible(serializable = true)
  public static <E extends @Nullable Object> ArrayList<E> newArrayList() {
    return new ArrayList<>();
  }

  /**
   * Creates a <i>mutable</i> {@code ArrayList} instance containing the given elements.
   *
   * <p><b>Note:</b> essentially the only reason to use this method is when you will need to add or
   * remove elements later. Otherwise, for non-null elements use {@link ImmutableList#of()} (for
   * varargs) or {@link ImmutableList#copyOf(Object[])} (for an array) instead. If any elements
   * might be null, or you need support for {@link List#set(int, Object)}, use {@link
   * Arrays#asList}.
   *
   * <p>Note that even when you do need the ability to add or remove, this method provides only a
   * tiny bit of syntactic sugar for {@code newArrayList(}{@link Arrays#asList asList}{@code
   * (...))}, or for creating an empty list then calling {@link Collections#addAll}. This method is
   * not actually very useful and will likely be deprecated in the future.
   */
  @SafeVarargs
  @GwtCompatible(serializable = true)
  public static <E extends @Nullable Object> ArrayList<E> newArrayList(E... elements) {
    checkNotNull(elements); // for GWT
    // Avoid integer overflow when a large array is passed in
    int capacity = computeArrayListCapacity(elements.length);
    ArrayList<E> list = new ArrayList<>(capacity);
    Collections.addAll(list, elements);
    return list;
  }

  /**
   * Creates a <i>mutable</i> {@code ArrayList} instance containing the given elements; a very thin
   * shortcut for creating an empty list then calling {@link Iterables#addAll}.
   *
   * <p><b>Note:</b> if mutability is not required and the elements are non-null, use {@link
   * ImmutableList#copyOf(Iterable)} instead. (Or, change {@code elements} to be a {@link
   * FluentIterable} and call {@code elements.toList()}.)
   *
   * <p><b>Note:</b> if {@code elements} is a {@link Collection}, you don't need this method. Use
   * the {@code ArrayList} {@linkplain ArrayList#ArrayList(Collection) constructor} directly, taking
   * advantage of <a href="http://goo.gl/iz2Wi">"diamond" syntax</a>.
   */
  @GwtCompatible(serializable = true)
  public static <E extends @Nullable Object> ArrayList<E> newArrayList(
      Iterable<? extends E> elements) {
    checkNotNull(elements); // for GWT
    // Let ArrayList's sizing logic work, if possible
    return (elements instanceof Collection)
        ? new ArrayList<>((Collection<? extends E>) elements)
        : newArrayList(elements.iterator());
  }

  /**
   * Creates a <i>mutable</i> {@code ArrayList} instance containing the given elements; a very thin
   * shortcut for creating an empty list and then calling {@link Iterators#addAll}.
   *
   * <p><b>Note:</b> if mutability is not required and the elements are non-null, use {@link
   * ImmutableList#copyOf(Iterator)} instead.
   */
  @GwtCompatible(serializable = true)
  public static <E extends @Nullable Object> ArrayList<E> newArrayList(
      Iterator<? extends E> elements) {
    ArrayList<E> list = newArrayList();
    Iterators.addAll(list, elements);
    return list;
  }

  @VisibleForTesting
  static int computeArrayListCapacity(int arraySize) {
    checkNonnegative(arraySize, "arraySize");

    // TODO(kevinb): Figure out the right behavior, and document it
    return Ints.saturatedCast(5L + arraySize + (arraySize / 10));
  }

  /**
   * Creates an {@code ArrayList} instance backed by an array with the specified initial size;
   * simply delegates to {@link ArrayList#ArrayList(int)}.
   *
   * <p><b>Note:</b> this method is now unnecessary and should be treated as deprecated. Instead,
   * use {@code new }{@link ArrayList#ArrayList(int) ArrayList}{@code <>(int)} directly, taking
   * advantage of <a href="http://goo.gl/iz2Wi">"diamond" syntax</a>. (Unlike here, there is no risk
   * of overload ambiguity, since the {@code ArrayList} constructors very wisely did not accept
   * varargs.)
   *
   * @param initialArraySize the exact size of the initial backing array for the returned array list
   *     ({@code ArrayList} documentation calls this value the "capacity")
   * @return a new, empty {@code ArrayList} which is guaranteed not to resize itself unless its size
   *     reaches {@code initialArraySize + 1}
   * @throws IllegalArgumentException if {@code initialArraySize} is negative
   */
  @GwtCompatible(serializable = true)
  public static <E extends @Nullable Object> ArrayList<E> newArrayListWithCapacity(
      int initialArraySize) {
    checkNonnegative(initialArraySize, "initialArraySize"); // for GWT.
    return new ArrayList<>(initialArraySize);
  }

  /**
   * Creates an {@code ArrayList} instance to hold {@code estimatedSize} elements, <i>plus</i> an
   * unspecified amount of padding; you almost certainly mean to call {@link
   * #newArrayListWithCapacity} (see that method for further advice on usage).
   *
   * <p><b>Note:</b> This method will soon be deprecated. Even in the rare case that you do want
   * some amount of padding, it's best if you choose your desired amount explicitly.
   *
   * @param estimatedSize an estimate of the eventual {@link List#size()} of the new list
   * @return a new, empty {@code ArrayList}, sized appropriately to hold the estimated number of
   *     elements
   * @throws IllegalArgumentException if {@code estimatedSize} is negative
   */
  @GwtCompatible(serializable = true)
  public static <E extends @Nullable Object> ArrayList<E> newArrayListWithExpectedSize(
      int estimatedSize) {
    return new ArrayList<>(computeArrayListCapacity(estimatedSize));
  }

  // LinkedList

  /**
   * Creates a <i>mutable</i>, empty {@code LinkedList} instance (for Java 6 and earlier).
   *
   * <p><b>Note:</b> if you won't be adding any elements to the list, use {@link ImmutableList#of()}
   * instead.
   *
   * <p><b>Performance note:</b> {@link ArrayList} and {@link java.util.ArrayDeque} consistently
   * outperform {@code LinkedList} except in certain rare and specific situations. Unless you have
   * spent a lot of time benchmarking your specific needs, use one of those instead.
   *
   * <p><b>Note:</b> this method is now unnecessary and should be treated as deprecated. Instead,
   * use the {@code LinkedList} {@linkplain LinkedList#LinkedList() constructor} directly, taking
   * advantage of <a href="http://goo.gl/iz2Wi">"diamond" syntax</a>.
   */
  @GwtCompatible(serializable = true)
  public static <E extends @Nullable Object> LinkedList<E> newLinkedList() {
    return new LinkedList<>();
  }

  /**
   * Creates a <i>mutable</i> {@code LinkedList} instance containing the given elements; a very thin
   * shortcut for creating an empty list then calling {@link Iterables#addAll}.
   *
   * <p><b>Note:</b> if mutability is not required and the elements are non-null, use {@link
   * ImmutableList#copyOf(Iterable)} instead. (Or, change {@code elements} to be a {@link
   * FluentIterable} and call {@code elements.toList()}.)
   *
   * <p><b>Performance note:</b> {@link ArrayList} and {@link java.util.ArrayDeque} consistently
   * outperform {@code LinkedList} except in certain rare and specific situations. Unless you have
   * spent a lot of time benchmarking your specific needs, use one of those instead.
   *
   * <p><b>Note:</b> if {@code elements} is a {@link Collection}, you don't need this method. Use
   * the {@code LinkedList} {@linkplain LinkedList#LinkedList(Collection) constructor} directly,
   * taking advantage of <a href="http://goo.gl/iz2Wi">"diamond" syntax</a>.
   */
  @GwtCompatible(serializable = true)
  public static <E extends @Nullable Object> LinkedList<E> newLinkedList(
      Iterable<? extends E> elements) {
    LinkedList<E> list = newLinkedList();
    Iterables.addAll(list, elements);
    return list;
  }

  /**
   * Creates an empty {@code CopyOnWriteArrayList} instance.
   *
   * <p><b>Note:</b> if you need an immutable empty {@link List}, use {@link Collections#emptyList}
   * instead.
   *
   * @return a new, empty {@code CopyOnWriteArrayList}
   * @since 12.0
   */
  @J2ktIncompatible
  @GwtIncompatible // CopyOnWriteArrayList
  public static <E extends @Nullable Object> CopyOnWriteArrayList<E> newCopyOnWriteArrayList() {
    return new CopyOnWriteArrayList<>();
  }

  /**
   * Creates a {@code CopyOnWriteArrayList} instance containing the given elements.
   *
   * @param elements the elements that the list should contain, in order
   * @return a new {@code CopyOnWriteArrayList} containing those elements
   * @since 12.0
   */
  @J2ktIncompatible
  @GwtIncompatible // CopyOnWriteArrayList
  public static <E extends @Nullable Object> CopyOnWriteArrayList<E> newCopyOnWriteArrayList(
      Iterable<? extends E> elements) {
    // We copy elements to an ArrayList first, rather than incurring the
    // quadratic cost of adding them to the COWAL directly.
    Collection<? extends E> elementsCollection =
        (elements instanceof Collection)
            ? (Collection<? extends E>) elements
            : newArrayList(elements);
    return new CopyOnWriteArrayList<>(elementsCollection);
  }

  /**
   * Returns an unmodifiable list containing the specified first element and backed by the specified
   * array of additional elements. Changes to the {@code rest} array will be reflected in the
   * returned list. Unlike {@link Arrays#asList}, the returned list is unmodifiable.
   *
   * <p>This is useful when a varargs method needs to use a signature such as {@code (Foo firstFoo,
   * Foo... moreFoos)}, in order to avoid overload ambiguity or to enforce a minimum argument count.
   *
   * <p>The returned list is serializable and implements {@link RandomAccess}.
   *
   * @param first the first element
   * @param rest an array of additional elements, possibly empty
   * @return an unmodifiable list containing the specified elements
   */
  public static <E extends @Nullable Object> List<E> asList(@ParametricNullness E first, E[] rest) {
    return new OnePlusArrayList<>(first, rest);
  }

  /**
   * Returns an unmodifiable list containing the specified first and second element, and backed by
   * the specified array of additional elements. Changes to the {@code rest} array will be reflected
   * in the returned list. Unlike {@link Arrays#asList}, the returned list is unmodifiable.
   *
   * <p>This is useful when a varargs method needs to use a signature such as {@code (Foo firstFoo,
   * Foo secondFoo, Foo... moreFoos)}, in order to avoid overload ambiguity or to enforce a minimum
   * argument count.
   *
   * <p>The returned list is serializable and implements {@link RandomAccess}.
   *
   * @param first the first element
   * @param second the second element
   * @param rest an array of additional elements, possibly empty
   * @return an unmodifiable list containing the specified elements
   */
  public static <E extends @Nullable Object> List<E> asList(
      @ParametricNullness E first, @ParametricNullness E second, E[] rest) {
    return new TwoPlusArrayList<>(first, second, rest);
  }

  /** @see Lists#asList(Object, Object[]) */
  private static class OnePlusArrayList<E extends @Nullable Object> extends AbstractList<E>
      implements Serializable, RandomAccess {
    @ParametricNullness final E first;
    final E[] rest;

    OnePlusArrayList(@ParametricNullness E first, E[] rest) {
      this.first = first;
      this.rest = checkNotNull(rest);
    }

    @Override
    public int size() {
      return IntMath.saturatedAdd(rest.length, 1);
    }

    @Override
    @ParametricNullness
    public E get(int index) {
      // check explicitly so the IOOBE will have the right message
      checkElementIndex(index, size());
      return (index == 0) ? first : rest[index - 1];
    }

    @J2ktIncompatible private static final long serialVersionUID = 0;
  }

  /** @see Lists#asList(Object, Object, Object[]) */
  private static class TwoPlusArrayList<E extends @Nullable Object> extends AbstractList<E>
      implements Serializable, RandomAccess {
    @ParametricNullness final E first;
    @ParametricNullness final E second;
    final E[] rest;

    TwoPlusArrayList(@ParametricNullness E first, @ParametricNullness E second, E[] rest) {
      this.first = first;
      this.second = second;
      this.rest = checkNotNull(rest);
    }

    @Override
    public int size() {
      return IntMath.saturatedAdd(rest.length, 2);
    }

    @Override
    @ParametricNullness
    public E get(int index) {
      switch (index) {
        case 0:
          return first;
        case 1:
          return second;
        default:
          // check explicitly so the IOOBE will have the right message
          checkElementIndex(index, size());
          return rest[index - 2];
      }
    }

    @J2ktIncompatible private static final long serialVersionUID = 0;
  }

  /**
   * Returns every possible list that can be formed by choosing one element from each of the given
   * lists in order; the "n-ary <a href="http://en.wikipedia.org/wiki/Cartesian_product">Cartesian
   * product</a>" of the lists. For example:
   *
   * <pre>{@code
   * Lists.cartesianProduct(ImmutableList.of(
   *     ImmutableList.of(1, 2),
   *     ImmutableList.of("A", "B", "C")))
   * }</pre>
   *
   * <p>returns a list containing six lists in the following order:
   *
   * <ul>
   *   <li>{@code ImmutableList.of(1, "A")}
   *   <li>{@code ImmutableList.of(1, "B")}
   *   <li>{@code ImmutableList.of(1, "C")}
   *   <li>{@code ImmutableList.of(2, "A")}
   *   <li>{@code ImmutableList.of(2, "B")}
   *   <li>{@code ImmutableList.of(2, "C")}
   * </ul>
   *
   * <p>The result is guaranteed to be in the "traditional", lexicographical order for Cartesian
   * products that you would get from nesting for loops:
   *
   * <pre>{@code
   * for (B b0 : lists.get(0)) {
   *   for (B b1 : lists.get(1)) {
   *     ...
   *     ImmutableList<B> tuple = ImmutableList.of(b0, b1, ...);
   *     // operate on tuple
   *   }
   * }
   * }</pre>
   *
   * <p>Note that if any input list is empty, the Cartesian product will also be empty. If no lists
   * at all are provided (an empty list), the resulting Cartesian product has one element, an empty
   * list (counter-intuitive, but mathematically consistent).
   *
   * <p><i>Performance notes:</i> while the cartesian product of lists of size {@code m, n, p} is a
   * list of size {@code m x n x p}, its actual memory consumption is much smaller. When the
   * cartesian product is constructed, the input lists are merely copied. Only as the resulting list
   * is iterated are the individual lists created, and these are not retained after iteration.
   *
   * @param lists the lists to choose elements from, in the order that the elements chosen from
   *     those lists should appear in the resulting lists
   * @param <B> any common base class shared by all axes (often just {@link Object})
   * @return the Cartesian product, as an immutable list containing immutable lists
   * @throws IllegalArgumentException if the size of the cartesian product would be greater than
   *     {@link Integer#MAX_VALUE}
   * @throws NullPointerException if {@code lists}, any one of the {@code lists}, or any element of
   *     a provided list is null
   * @since 19.0
   */
  public static <B> List<List<B>> cartesianProduct(List<? extends List<? extends B>> lists) {
    return CartesianList.create(lists);
  }

  /**
   * Returns every possible list that can be formed by choosing one element from each of the given
   * lists in order; the "n-ary <a href="http://en.wikipedia.org/wiki/Cartesian_product">Cartesian
   * product</a>" of the lists. For example:
   *
   * <pre>{@code
   * Lists.cartesianProduct(ImmutableList.of(
   *     ImmutableList.of(1, 2),
   *     ImmutableList.of("A", "B", "C")))
   * }</pre>
   *
   * <p>returns a list containing six lists in the following order:
   *
   * <ul>
   *   <li>{@code ImmutableList.of(1, "A")}
   *   <li>{@code ImmutableList.of(1, "B")}
   *   <li>{@code ImmutableList.of(1, "C")}
   *   <li>{@code ImmutableList.of(2, "A")}
   *   <li>{@code ImmutableList.of(2, "B")}
   *   <li>{@code ImmutableList.of(2, "C")}
   * </ul>
   *
   * <p>The result is guaranteed to be in the "traditional", lexicographical order for Cartesian
   * products that you would get from nesting for loops:
   *
   * <pre>{@code
   * for (B b0 : lists.get(0)) {
   *   for (B b1 : lists.get(1)) {
   *     ...
   *     ImmutableList<B> tuple = ImmutableList.of(b0, b1, ...);
   *     // operate on tuple
   *   }
   * }
   * }</pre>
   *
   * <p>Note that if any input list is empty, the Cartesian product will also be empty. If no lists
   * at all are provided (an empty list), the resulting Cartesian product has one element, an empty
   * list (counter-intuitive, but mathematically consistent).
   *
   * <p><i>Performance notes:</i> while the cartesian product of lists of size {@code m, n, p} is a
   * list of size {@code m x n x p}, its actual memory consumption is much smaller. When the
   * cartesian product is constructed, the input lists are merely copied. Only as the resulting list
   * is iterated are the individual lists created, and these are not retained after iteration.
   *
   * @param lists the lists to choose elements from, in the order that the elements chosen from
   *     those lists should appear in the resulting lists
   * @param <B> any common base class shared by all axes (often just {@link Object})
   * @return the Cartesian product, as an immutable list containing immutable lists
   * @throws IllegalArgumentException if the size of the cartesian product would be greater than
   *     {@link Integer#MAX_VALUE}
   * @throws NullPointerException if {@code lists}, any one of the {@code lists}, or any element of
   *     a provided list is null
   * @since 19.0
   */
  @SafeVarargs
  public static <B> List<List<B>> cartesianProduct(List<? extends B>... lists) {
    return cartesianProduct(Arrays.asList(lists));
  }

  /**
   * Returns a list that applies {@code function} to each element of {@code fromList}. The returned
   * list is a transformed view of {@code fromList}; changes to {@code fromList} will be reflected
   * in the returned list and vice versa.
   *
   * <p>Since functions are not reversible, the transform is one-way and new items cannot be stored
   * in the returned list. The {@code add}, {@code addAll} and {@code set} methods are unsupported
   * in the returned list.
   *
   * <p>The function is applied lazily, invoked when needed. This is necessary for the returned list
   * to be a view, but it means that the function will be applied many times for bulk operations
   * like {@link List#contains} and {@link List#hashCode}. For this to perform well, {@code
   * function} should be fast. To avoid lazy evaluation when the returned list doesn't need to be a
   * view, copy the returned list into a new list of your choosing.
   *
   * <p>If {@code fromList} implements {@link RandomAccess}, so will the returned list. The returned
   * list is threadsafe if the supplied list and function are.
   *
   * <p>If only a {@code Collection} or {@code Iterable} input is available, use {@link
   * Collections2#transform} or {@link Iterables#transform}.
   *
   * <p><b>Note:</b> serializing the returned list is implemented by serializing {@code fromList},
   * its contents, and {@code function} -- <i>not</i> by serializing the transformed values. This
   * can lead to surprising behavior, so serializing the returned list is <b>not recommended</b>.
   * Instead, copy the list using {@link ImmutableList#copyOf(Collection)} (for example), then
   * serialize the copy. Other methods similar to this do not implement serialization at all for
   * this reason.
   *
   * <p><b>Java 8 users:</b> many use cases for this method are better addressed by {@link
   * java.util.stream.Stream#map}. This method is not being deprecated, but we gently encourage you
   * to migrate to streams.
   */
  public static <F extends @Nullable Object, T extends @Nullable Object> List<T> transform(
      List<F> fromList, Function<? super F, ? extends T> function) {
    return (fromList instanceof RandomAccess)
        ? new TransformingRandomAccessList<>(fromList, function)
        : new TransformingSequentialList<>(fromList, function);
  }

  /**
   * Implementation of a sequential transforming list.
   *
   * @see Lists#transform
   */
  private static class TransformingSequentialList<
          F extends @Nullable Object, T extends @Nullable Object>
      extends AbstractSequentialList<T> implements Serializable {
    final List<F> fromList;
    final Function<? super F, ? extends T> function;

    TransformingSequentialList(List<F> fromList, Function<? super F, ? extends T> function) {
      this.fromList = checkNotNull(fromList);
      this.function = checkNotNull(function);
    }

    /**
     * The default implementation inherited is based on iteration and removal of each element which
     * can be overkill. That's why we forward this call directly to the backing list.
     */
    @Override
    protected void removeRange(int fromIndex, int toIndex) {
      fromList.subList(fromIndex, toIndex).clear();
    }

    @Override
    public int size() {
      return fromList.size();
    }

    @Override
    public ListIterator<T> listIterator(final int index) {
      return new TransformedListIterator<F, T>(fromList.listIterator(index)) {
        @Override
        @ParametricNullness
        T transform(@ParametricNullness F from) {
          return function.apply(from);
        }
      };
    }

    @Override
    public boolean removeIf(Predicate<? super T> filter) {
      checkNotNull(filter);
      return fromList.removeIf(element -> filter.test(function.apply(element)));
    }

    private static final long serialVersionUID = 0;
  }

  /**
   * Implementation of a transforming random access list. We try to make as many of these methods
   * pass-through to the source list as possible so that the performance characteristics of the
   * source list and transformed list are similar.
   *
   * @see Lists#transform
   */
  private static class TransformingRandomAccessList<
          F extends @Nullable Object, T extends @Nullable Object>
      extends AbstractList<T> implements RandomAccess, Serializable {
    final List<F> fromList;
    final Function<? super F, ? extends T> function;

    TransformingRandomAccessList(List<F> fromList, Function<? super F, ? extends T> function) {
      this.fromList = checkNotNull(fromList);
      this.function = checkNotNull(function);
    }

    /**
     * The default implementation inherited is based on iteration and removal of each element which
     * can be overkill. That's why we forward this call directly to the backing list.
     */
    @Override
    protected void removeRange(int fromIndex, int toIndex) {
      fromList.subList(fromIndex, toIndex).clear();
    }

    @Override
    @ParametricNullness
    public T get(int index) {
      return function.apply(fromList.get(index));
    }

    @Override
    public Iterator<T> iterator() {
      return listIterator();
    }

    @Override
    public ListIterator<T> listIterator(int index) {
      return new TransformedListIterator<F, T>(fromList.listIterator(index)) {
        @Override
        T transform(F from) {
          return function.apply(from);
        }
      };
    }

    // TODO: cpovirk - Why override `isEmpty` here but not in TransformingSequentialList?

    @Override
    public boolean isEmpty() {
      return fromList.isEmpty();
    }

    @Override
    public boolean removeIf(Predicate<? super T> filter) {
      checkNotNull(filter);
      return fromList.removeIf(element -> filter.test(function.apply(element)));
    }

    @Override
    @ParametricNullness
    public T remove(int index) {
      return function.apply(fromList.remove(index));
    }

    @Override
    public int size() {
      return fromList.size();
    }

    private static final long serialVersionUID = 0;
  }

  /**
   * Returns consecutive {@linkplain List#subList(int, int) sublists} of a list, each of the same
   * size (the final list may be smaller). For example, partitioning a list containing {@code [a, b,
   * c, d, e]} with a partition size of 3 yields {@code [[a, b, c], [d, e]]} -- an outer list
   * containing two inner lists of three and two elements, all in the original order.
   *
   * <p>The outer list is unmodifiable, but reflects the latest state of the source list. The inner
   * lists are sublist views of the original list, produced on demand using {@link List#subList(int,
   * int)}, and are subject to all the usual caveats about modification as explained in that API.
   *
   * @param list the list to return consecutive sublists of
   * @param size the desired size of each sublist (the last may be smaller)
   * @return a list of consecutive sublists
   * @throws IllegalArgumentException if {@code partitionSize} is nonpositive
   */
  public static <T extends @Nullable Object> List<List<T>> partition(List<T> list, int size) {
    checkNotNull(list);
    checkArgument(size > 0);
    return (list instanceof RandomAccess)
        ? new RandomAccessPartition<>(list, size)
        : new Partition<>(list, size);
  }

  private static class Partition<T extends @Nullable Object> extends AbstractList<List<T>> {
    final List<T> list;
    final int size;

    Partition(List<T> list, int size) {
      this.list = list;
      this.size = size;
    }

    @Override
    public List<T> get(int index) {
      checkElementIndex(index, size());
      int start = index * size;
      int end = Math.min(start + size, list.size());
      return list.subList(start, end);
    }

    @Override
    public int size() {
      return IntMath.divide(list.size(), size, RoundingMode.CEILING);
    }

    @Override
    public boolean isEmpty() {
      return list.isEmpty();
    }
  }

  private static class RandomAccessPartition<T extends @Nullable Object> extends Partition<T>
      implements RandomAccess {
    RandomAccessPartition(List<T> list, int size) {
      super(list, size);
    }
  }

  /**
   * Returns a view of the specified string as an immutable list of {@code Character} values.
   *
   * @since 7.0
   */
  public static ImmutableList<Character> charactersOf(String string) {
    return new StringAsImmutableList(checkNotNull(string));
  }

  /**
   * Returns a view of the specified {@code CharSequence} as a {@code List<Character>}, viewing
   * {@code sequence} as a sequence of Unicode code units. The view does not support any
   * modification operations, but reflects any changes to the underlying character sequence.
   *
   * @param sequence the character sequence to view as a {@code List} of characters
   * @return an {@code List<Character>} view of the character sequence
   * @since 7.0
   */
  public static List<Character> charactersOf(CharSequence sequence) {
    return new CharSequenceAsList(checkNotNull(sequence));
  }

  @SuppressWarnings("serial") // serialized using ImmutableList serialization
  private static final class StringAsImmutableList extends ImmutableList<Character> {

    private final String string;

    StringAsImmutableList(String string) {
      this.string = string;
    }

    @Override
    public int indexOf(@CheckForNull Object object) {
      return (object instanceof Character) ? string.indexOf((Character) object) : -1;
    }

    @Override
    public int lastIndexOf(@CheckForNull Object object) {
      return (object instanceof Character) ? string.lastIndexOf((Character) object) : -1;
    }

    @Override
    public ImmutableList<Character> subList(int fromIndex, int toIndex) {
      checkPositionIndexes(fromIndex, toIndex, size()); // for GWT
      return charactersOf(string.substring(fromIndex, toIndex));
    }

    @Override
    boolean isPartialView() {
      return false;
    }

    @Override
    public Character get(int index) {
      checkElementIndex(index, size()); // for GWT
      return string.charAt(index);
    }

    @Override
    public int size() {
      return string.length();
    }

    // redeclare to help optimizers with b/310253115
    @SuppressWarnings("RedundantOverride")
    @Override
    @J2ktIncompatible // serialization
    @GwtIncompatible // serialization
    Object writeReplace() {
      return super.writeReplace();
    }
  }

  private static final class CharSequenceAsList extends AbstractList<Character> {
    private final CharSequence sequence;

    CharSequenceAsList(CharSequence sequence) {
      this.sequence = sequence;
    }

    @Override
    public Character get(int index) {
      checkElementIndex(index, size()); // for GWT
      return sequence.charAt(index);
    }

    @Override
    public int size() {
      return sequence.length();
    }
  }

  /**
   * Returns a reversed view of the specified list. For example, {@code
   * Lists.reverse(Arrays.asList(1, 2, 3))} returns a list containing {@code 3, 2, 1}. The returned
   * list is backed by this list, so changes in the returned list are reflected in this list, and
   * vice-versa. The returned list supports all of the optional list operations supported by this
   * list.
   *
   * <p>The returned list is random-access if the specified list is random access.
   *
   * @since 7.0
   */
  public static <T extends @Nullable Object> List<T> reverse(List<T> list) {
    if (list instanceof ImmutableList) {
      // Avoid nullness warnings.
      List<?> reversed = ((ImmutableList<?>) list).reverse();
      @SuppressWarnings("unchecked")
      List<T> result = (List<T>) reversed;
      return result;
    } else if (list instanceof ReverseList) {
      return ((ReverseList<T>) list).getForwardList();
    } else if (list instanceof RandomAccess) {
      return new RandomAccessReverseList<>(list);
    } else {
      return new ReverseList<>(list);
    }
  }

  private static class ReverseList<T extends @Nullable Object> extends AbstractList<T> {
    private final List<T> forwardList;

    ReverseList(List<T> forwardList) {
      this.forwardList = checkNotNull(forwardList);
    }

    List<T> getForwardList() {
      return forwardList;
    }

    private int reverseIndex(int index) {
      int size = size();
      checkElementIndex(index, size);
      return (size - 1) - index;
    }

    private int reversePosition(int index) {
      int size = size();
      checkPositionIndex(index, size);
      return size - index;
    }

    @Override
    public void add(int index, @ParametricNullness T element) {
      forwardList.add(reversePosition(index), element);
    }

    @Override
    public void clear() {
      forwardList.clear();
    }

    @Override
    @ParametricNullness
    public T remove(int index) {
      return forwardList.remove(reverseIndex(index));
    }

    @Override
    protected void removeRange(int fromIndex, int toIndex) {
      subList(fromIndex, toIndex).clear();
    }

    @Override
    @ParametricNullness
    public T set(int index, @ParametricNullness T element) {
      return forwardList.set(reverseIndex(index), element);
    }

    @Override
    @ParametricNullness
    public T get(int index) {
      return forwardList.get(reverseIndex(index));
    }

    @Override
    public int size() {
      return forwardList.size();
    }

    @Override
    public List<T> subList(int fromIndex, int toIndex) {
      checkPositionIndexes(fromIndex, toIndex, size());
      return reverse(forwardList.subList(reversePosition(toIndex), reversePosition(fromIndex)));
    }

    @Override
    public Iterator<T> iterator() {
      return listIterator();
    }

    @Override
    public ListIterator<T> listIterator(int index) {
      int start = reversePosition(index);
      final ListIterator<T> forwardIterator = forwardList.listIterator(start);
      return new ListIterator<T>() {

        boolean canRemoveOrSet;

        @Override
        public void add(@ParametricNullness T e) {
          forwardIterator.add(e);
          forwardIterator.previous();
          canRemoveOrSet = false;
        }

        @Override
        public boolean hasNext() {
          return forwardIterator.hasPrevious();
        }

        @Override
        public boolean hasPrevious() {
          return forwardIterator.hasNext();
        }

        @Override
        @ParametricNullness
        public T next() {
          if (!hasNext()) {
            throw new NoSuchElementException();
          }
          canRemoveOrSet = true;
          return forwardIterator.previous();
        }

        @Override
        public int nextIndex() {
          return reversePosition(forwardIterator.nextIndex());
        }

        @Override
        @ParametricNullness
        public T previous() {
          if (!hasPrevious()) {
            throw new NoSuchElementException();
          }
          canRemoveOrSet = true;
          return forwardIterator.next();
        }

        @Override
        public int previousIndex() {
          return nextIndex() - 1;
        }

        @Override
        public void remove() {
          checkRemove(canRemoveOrSet);
          forwardIterator.remove();
          canRemoveOrSet = false;
        }

        @Override
        public void set(@ParametricNullness T e) {
          checkState(canRemoveOrSet);
          forwardIterator.set(e);
        }
      };
    }
  }

  private static class RandomAccessReverseList<T extends @Nullable Object> extends ReverseList<T>
      implements RandomAccess {
    RandomAccessReverseList(List<T> forwardList) {
      super(forwardList);
    }
  }

  /** An implementation of {@link List#hashCode()}. */
  static int hashCodeImpl(List<?> list) {
    // TODO(lowasser): worth optimizing for RandomAccess?
    int hashCode = 1;
    for (Object o : list) {
      hashCode = 31 * hashCode + (o == null ? 0 : o.hashCode());

      hashCode = ~~hashCode;
      // needed to deal with GWT integer overflow
    }
    return hashCode;
  }

  /** An implementation of {@link List#equals(Object)}. */
  static boolean equalsImpl(List<?> thisList, @CheckForNull Object other) {
    if (other == checkNotNull(thisList)) {
      return true;
    }
    if (!(other instanceof List)) {
      return false;
    }
    List<?> otherList = (List<?>) other;
    int size = thisList.size();
    if (size != otherList.size()) {
      return false;
    }
    if (thisList instanceof RandomAccess && otherList instanceof RandomAccess) {
      // avoid allocation and use the faster loop
      for (int i = 0; i < size; i++) {
        if (!Objects.equal(thisList.get(i), otherList.get(i))) {
          return false;
        }
      }
      return true;
    } else {
      return Iterators.elementsEqual(thisList.iterator(), otherList.iterator());
    }
  }

  /** An implementation of {@link List#addAll(int, Collection)}. */
  static <E extends @Nullable Object> boolean addAllImpl(
      List<E> list, int index, Iterable<? extends E> elements) {
    boolean changed = false;
    ListIterator<E> listIterator = list.listIterator(index);
    for (E e : elements) {
      listIterator.add(e);
      changed = true;
    }
    return changed;
  }

  /** An implementation of {@link List#indexOf(Object)}. */
  static int indexOfImpl(List<?> list, @CheckForNull Object element) {
    if (list instanceof RandomAccess) {
      return indexOfRandomAccess(list, element);
    } else {
      ListIterator<?> listIterator = list.listIterator();
      while (listIterator.hasNext()) {
        if (Objects.equal(element, listIterator.next())) {
          return listIterator.previousIndex();
        }
      }
      return -1;
    }
  }

  private static int indexOfRandomAccess(List<?> list, @CheckForNull Object element) {
    int size = list.size();
    if (element == null) {
      for (int i = 0; i < size; i++) {
        if (list.get(i) == null) {
          return i;
        }
      }
    } else {
      for (int i = 0; i < size; i++) {
        if (element.equals(list.get(i))) {
          return i;
        }
      }
    }
    return -1;
  }

  /** An implementation of {@link List#lastIndexOf(Object)}. */
  static int lastIndexOfImpl(List<?> list, @CheckForNull Object element) {
    if (list instanceof RandomAccess) {
      return lastIndexOfRandomAccess(list, element);
    } else {
      ListIterator<?> listIterator = list.listIterator(list.size());
      while (listIterator.hasPrevious()) {
        if (Objects.equal(element, listIterator.previous())) {
          return listIterator.nextIndex();
        }
      }
      return -1;
    }
  }

  private static int lastIndexOfRandomAccess(List<?> list, @CheckForNull Object element) {
    if (element == null) {
      for (int i = list.size() - 1; i >= 0; i--) {
        if (list.get(i) == null) {
          return i;
        }
      }
    } else {
      for (int i = list.size() - 1; i >= 0; i--) {
        if (element.equals(list.get(i))) {
          return i;
        }
      }
    }
    return -1;
  }

  /** Returns an implementation of {@link List#listIterator(int)}. */
  static <E extends @Nullable Object> ListIterator<E> listIteratorImpl(List<E> list, int index) {
    return new AbstractListWrapper<>(list).listIterator(index);
  }

  /** An implementation of {@link List#subList(int, int)}. */
  static <E extends @Nullable Object> List<E> subListImpl(
      final List<E> list, int fromIndex, int toIndex) {
    List<E> wrapper;
    if (list instanceof RandomAccess) {
      wrapper =
          new RandomAccessListWrapper<E>(list) {
            @Override
            public ListIterator<E> listIterator(int index) {
              return backingList.listIterator(index);
            }

            @J2ktIncompatible private static final long serialVersionUID = 0;
          };
    } else {
      wrapper =
          new AbstractListWrapper<E>(list) {
            @Override
            public ListIterator<E> listIterator(int index) {
              return backingList.listIterator(index);
            }

            @J2ktIncompatible private static final long serialVersionUID = 0;
          };
    }
    return wrapper.subList(fromIndex, toIndex);
  }

  private static class AbstractListWrapper<E extends @Nullable Object> extends AbstractList<E> {
    final List<E> backingList;

    AbstractListWrapper(List<E> backingList) {
      this.backingList = checkNotNull(backingList);
    }

    @Override
    public void add(int index, @ParametricNullness E element) {
      backingList.add(index, element);
    }

    @Override
    public boolean addAll(int index, Collection<? extends E> c) {
      return backingList.addAll(index, c);
    }

    @Override
    @ParametricNullness
    public E get(int index) {
      return backingList.get(index);
    }

    @Override
    @ParametricNullness
    public E remove(int index) {
      return backingList.remove(index);
    }

    @Override
    @ParametricNullness
    public E set(int index, @ParametricNullness E element) {
      return backingList.set(index, element);
    }

    @Override
    public boolean contains(@CheckForNull Object o) {
      return backingList.contains(o);
    }

    @Override
    public int size() {
      return backingList.size();
    }
  }

  private static class RandomAccessListWrapper<E extends @Nullable Object>
      extends AbstractListWrapper<E> implements RandomAccess {
    RandomAccessListWrapper(List<E> backingList) {
      super(backingList);
    }
  }

  /** Used to avoid http://bugs.sun.com/view_bug.do?bug_id=6558557 */
  static <T extends @Nullable Object> List<T> cast(Iterable<T> iterable) {
    return (List<T>) iterable;
  }
}
