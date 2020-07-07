/*
 * Copyright (C) 2008 The Guava Authors
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
import static com.google.common.collect.CollectPreconditions.checkNonnegative;

import com.google.common.annotations.Beta;
import com.google.common.annotations.GwtCompatible;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.math.IntMath;
import com.google.common.primitives.Ints;
import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Spliterator;
import java.util.function.Consumer;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Provides static methods for working with {@code Collection} instances.
 *
 * <p><b>Java 8 users:</b> several common uses for this class are now more comprehensively addressed
 * by the new {@link java.util.stream.Stream} library. Read the method documentation below for
 * comparisons. These methods are not being deprecated, but we gently encourage you to migrate to
 * streams.
 *
 * @author Chris Povirk
 * @author Mike Bostock
 * @author Jared Levy
 * @since 2.0
 */
@GwtCompatible
public final class Collections2 {
  private Collections2() {}

  /**
   * Returns the elements of {@code unfiltered} that satisfy a predicate. The returned collection is
   * a live view of {@code unfiltered}; changes to one affect the other.
   *
   * <p>The resulting collection's iterator does not support {@code remove()}, but all other
   * collection methods are supported. When given an element that doesn't satisfy the predicate, the
   * collection's {@code add()} and {@code addAll()} methods throw an {@link
   * IllegalArgumentException}. When methods such as {@code removeAll()} and {@code clear()} are
   * called on the filtered collection, only elements that satisfy the filter will be removed from
   * the underlying collection.
   *
   * <p>The returned collection isn't threadsafe or serializable, even if {@code unfiltered} is.
   *
   * <p>Many of the filtered collection's methods, such as {@code size()}, iterate across every
   * element in the underlying collection and determine which elements satisfy the filter. When a
   * live view is <i>not</i> needed, it may be faster to copy {@code Iterables.filter(unfiltered,
   * predicate)} and use the copy.
   *
   * <p><b>Warning:</b> {@code predicate} must be <i>consistent with equals</i>, as documented at
   * {@link Predicate#apply}. Do not provide a predicate such as {@code
   * Predicates.instanceOf(ArrayList.class)}, which is inconsistent with equals. (See {@link
   * Iterables#filter(Iterable, Class)} for related functionality.)
   *
   * <p><b>{@code Stream} equivalent:</b> {@link java.util.stream.Stream#filter Stream.filter}.
   */
  // TODO(kevinb): how can we omit that Iterables link when building gwt
  // javadoc?
  public static <E> Collection<E> filter(Collection<E> unfiltered, Predicate<? super E> predicate) {
    if (unfiltered instanceof FilteredCollection) {
      // Support clear(), removeAll(), and retainAll() when filtering a filtered
      // collection.
      return ((FilteredCollection<E>) unfiltered).createCombined(predicate);
    }

    return new FilteredCollection<E>(checkNotNull(unfiltered), checkNotNull(predicate));
  }

  /**
   * Delegates to {@link Collection#contains}. Returns {@code false} if the {@code contains} method
   * throws a {@code ClassCastException} or {@code NullPointerException}.
   */
  static boolean safeContains(Collection<?> collection, @Nullable Object object) {
    checkNotNull(collection);
    try {
      return collection.contains(object);
    } catch (ClassCastException | NullPointerException e) {
      return false;
    }
  }

  /**
   * Delegates to {@link Collection#remove}. Returns {@code false} if the {@code remove} method
   * throws a {@code ClassCastException} or {@code NullPointerException}.
   */
  static boolean safeRemove(Collection<?> collection, @Nullable Object object) {
    checkNotNull(collection);
    try {
      return collection.remove(object);
    } catch (ClassCastException | NullPointerException e) {
      return false;
    }
  }

  static class FilteredCollection<E> extends AbstractCollection<E> {
    final Collection<E> unfiltered;
    final Predicate<? super E> predicate;

    FilteredCollection(Collection<E> unfiltered, Predicate<? super E> predicate) {
      this.unfiltered = unfiltered;
      this.predicate = predicate;
    }

    FilteredCollection<E> createCombined(Predicate<? super E> newPredicate) {
      return new FilteredCollection<E>(unfiltered, Predicates.<E>and(predicate, newPredicate));
      // .<E> above needed to compile in JDK 5
    }

    @Override
    public boolean add(E element) {
      checkArgument(predicate.apply(element));
      return unfiltered.add(element);
    }

    @Override
    public boolean addAll(Collection<? extends E> collection) {
      for (E element : collection) {
        checkArgument(predicate.apply(element));
      }
      return unfiltered.addAll(collection);
    }

    @Override
    public void clear() {
      Iterables.removeIf(unfiltered, predicate);
    }

    @Override
    public boolean contains(@Nullable Object element) {
      if (safeContains(unfiltered, element)) {
        @SuppressWarnings("unchecked") // element is in unfiltered, so it must be an E
        E e = (E) element;
        return predicate.apply(e);
      }
      return false;
    }

    @Override
    public boolean containsAll(Collection<?> collection) {
      return containsAllImpl(this, collection);
    }

    @Override
    public boolean isEmpty() {
      return !Iterables.any(unfiltered, predicate);
    }

    @Override
    public Iterator<E> iterator() {
      return Iterators.filter(unfiltered.iterator(), predicate);
    }

    @Override
    public Spliterator<E> spliterator() {
      return CollectSpliterators.filter(unfiltered.spliterator(), predicate);
    }

    @Override
    public void forEach(Consumer<? super E> action) {
      checkNotNull(action);
      unfiltered.forEach(
          (E e) -> {
            if (predicate.test(e)) {
              action.accept(e);
            }
          });
    }

    @Override
    public boolean remove(Object element) {
      return contains(element) && unfiltered.remove(element);
    }

    @Override
    public boolean removeAll(final Collection<?> collection) {
      return removeIf(collection::contains);
    }

    @Override
    public boolean retainAll(final Collection<?> collection) {
      return removeIf(element -> !collection.contains(element));
    }

    @Override
    public boolean removeIf(java.util.function.Predicate<? super E> filter) {
      checkNotNull(filter);
      return unfiltered.removeIf(element -> predicate.apply(element) && filter.test(element));
    }

    @Override
    public int size() {
      int size = 0;
      for (E e : unfiltered) {
        if (predicate.apply(e)) {
          size++;
        }
      }
      return size;
    }

    @Override
    public Object[] toArray() {
      // creating an ArrayList so filtering happens once
      return Lists.newArrayList(iterator()).toArray();
    }

    @Override
    public <T> T[] toArray(T[] array) {
      return Lists.newArrayList(iterator()).toArray(array);
    }
  }

  /**
   * Returns a collection that applies {@code function} to each element of {@code fromCollection}.
   * The returned collection is a live view of {@code fromCollection}; changes to one affect the
   * other.
   *
   * <p>The returned collection's {@code add()} and {@code addAll()} methods throw an {@link
   * UnsupportedOperationException}. All other collection methods are supported, as long as {@code
   * fromCollection} supports them.
   *
   * <p>The returned collection isn't threadsafe or serializable, even if {@code fromCollection} is.
   *
   * <p>When a live view is <i>not</i> needed, it may be faster to copy the transformed collection
   * and use the copy.
   *
   * <p>If the input {@code Collection} is known to be a {@code List}, consider {@link
   * Lists#transform}. If only an {@code Iterable} is available, use {@link Iterables#transform}.
   *
   * <p><b>{@code Stream} equivalent:</b> {@link java.util.stream.Stream#map Stream.map}.
   */
  public static <F, T> Collection<T> transform(
      Collection<F> fromCollection, Function<? super F, T> function) {
    return new TransformedCollection<>(fromCollection, function);
  }

  static class TransformedCollection<F, T> extends AbstractCollection<T> {
    final Collection<F> fromCollection;
    final Function<? super F, ? extends T> function;

    TransformedCollection(Collection<F> fromCollection, Function<? super F, ? extends T> function) {
      this.fromCollection = checkNotNull(fromCollection);
      this.function = checkNotNull(function);
    }

    @Override
    public void clear() {
      fromCollection.clear();
    }

    @Override
    public boolean isEmpty() {
      return fromCollection.isEmpty();
    }

    @Override
    public Iterator<T> iterator() {
      return Iterators.transform(fromCollection.iterator(), function);
    }

    @Override
    public Spliterator<T> spliterator() {
      return CollectSpliterators.map(fromCollection.spliterator(), function);
    }

    @Override
    public void forEach(Consumer<? super T> action) {
      checkNotNull(action);
      fromCollection.forEach((F f) -> action.accept(function.apply(f)));
    }

    @Override
    public boolean removeIf(java.util.function.Predicate<? super T> filter) {
      checkNotNull(filter);
      return fromCollection.removeIf(element -> filter.test(function.apply(element)));
    }

    @Override
    public int size() {
      return fromCollection.size();
    }
  }

  /**
   * Returns {@code true} if the collection {@code self} contains all of the elements in the
   * collection {@code c}.
   *
   * <p>This method iterates over the specified collection {@code c}, checking each element returned
   * by the iterator in turn to see if it is contained in the specified collection {@code self}. If
   * all elements are so contained, {@code true} is returned, otherwise {@code false}.
   *
   * @param self a collection which might contain all elements in {@code c}
   * @param c a collection whose elements might be contained by {@code self}
   */
  static boolean containsAllImpl(Collection<?> self, Collection<?> c) {
    for (Object o : c) {
      if (!self.contains(o)) {
        return false;
      }
    }
    return true;
  }

  /** An implementation of {@link Collection#toString()}. */
  static String toStringImpl(final Collection<?> collection) {
    StringBuilder sb = newStringBuilderForCollection(collection.size()).append('[');
    boolean first = true;
    for (Object o : collection) {
      if (!first) {
        sb.append(", ");
      }
      first = false;
      if (o == collection) {
        sb.append("(this Collection)");
      } else {
        sb.append(o);
      }
    }
    return sb.append(']').toString();
  }

  /** Returns best-effort-sized StringBuilder based on the given collection size. */
  static StringBuilder newStringBuilderForCollection(int size) {
    checkNonnegative(size, "size");
    return new StringBuilder((int) Math.min(size * 8L, Ints.MAX_POWER_OF_TWO));
  }

  /** Used to avoid http://bugs.sun.com/view_bug.do?bug_id=6558557 */
  static <T> Collection<T> cast(Iterable<T> iterable) {
    return (Collection<T>) iterable;
  }

  /**
   * Returns a {@link Collection} of all the permutations of the specified {@link Iterable}.
   *
   * <p><i>Notes:</i> This is an implementation of the algorithm for Lexicographical Permutations
   * Generation, described in Knuth's "The Art of Computer Programming", Volume 4, Chapter 7,
   * Section 7.2.1.2. The iteration order follows the lexicographical order. This means that the
   * first permutation will be in ascending order, and the last will be in descending order.
   *
   * <p>Duplicate elements are considered equal. For example, the list [1, 1] will have only one
   * permutation, instead of two. This is why the elements have to implement {@link Comparable}.
   *
   * <p>An empty iterable has only one permutation, which is an empty list.
   *
   * <p>This method is equivalent to {@code Collections2.orderedPermutations(list,
   * Ordering.natural())}.
   *
   * @param elements the original iterable whose elements have to be permuted.
   * @return an immutable {@link Collection} containing all the different permutations of the
   *     original iterable.
   * @throws NullPointerException if the specified iterable is null or has any null elements.
   * @since 12.0
   */
  @Beta
  public static <E extends Comparable<? super E>> Collection<List<E>> orderedPermutations(
      Iterable<E> elements) {
    return orderedPermutations(elements, Ordering.natural());
  }

  /**
   * Returns a {@link Collection} of all the permutations of the specified {@link Iterable} using
   * the specified {@link Comparator} for establishing the lexicographical ordering.
   *
   * <p>Examples:
   *
   * <pre>{@code
   * for (List<String> perm : orderedPermutations(asList("b", "c", "a"))) {
   *   println(perm);
   * }
   * // -> ["a", "b", "c"]
   * // -> ["a", "c", "b"]
   * // -> ["b", "a", "c"]
   * // -> ["b", "c", "a"]
   * // -> ["c", "a", "b"]
   * // -> ["c", "b", "a"]
   *
   * for (List<Integer> perm : orderedPermutations(asList(1, 2, 2, 1))) {
   *   println(perm);
   * }
   * // -> [1, 1, 2, 2]
   * // -> [1, 2, 1, 2]
   * // -> [1, 2, 2, 1]
   * // -> [2, 1, 1, 2]
   * // -> [2, 1, 2, 1]
   * // -> [2, 2, 1, 1]
   * }</pre>
   *
   * <p><i>Notes:</i> This is an implementation of the algorithm for Lexicographical Permutations
   * Generation, described in Knuth's "The Art of Computer Programming", Volume 4, Chapter 7,
   * Section 7.2.1.2. The iteration order follows the lexicographical order. This means that the
   * first permutation will be in ascending order, and the last will be in descending order.
   *
   * <p>Elements that compare equal are considered equal and no new permutations are created by
   * swapping them.
   *
   * <p>An empty iterable has only one permutation, which is an empty list.
   *
   * @param elements the original iterable whose elements have to be permuted.
   * @param comparator a comparator for the iterable's elements.
   * @return an immutable {@link Collection} containing all the different permutations of the
   *     original iterable.
   * @throws NullPointerException If the specified iterable is null, has any null elements, or if
   *     the specified comparator is null.
   * @since 12.0
   */
  @Beta
  public static <E> Collection<List<E>> orderedPermutations(
      Iterable<E> elements, Comparator<? super E> comparator) {
    return new OrderedPermutationCollection<E>(elements, comparator);
  }

  private static final class OrderedPermutationCollection<E> extends AbstractCollection<List<E>> {
    final ImmutableList<E> inputList;
    final Comparator<? super E> comparator;
    final int size;

    OrderedPermutationCollection(Iterable<E> input, Comparator<? super E> comparator) {
      this.inputList = ImmutableList.sortedCopyOf(comparator, input);
      this.comparator = comparator;
      this.size = calculateSize(inputList, comparator);
    }

    /**
     * The number of permutations with repeated elements is calculated as follows:
     *
     * <ul>
     *   <li>For an empty list, it is 1 (base case).
     *   <li>When r numbers are added to a list of n-r elements, the number of permutations is
     *       increased by a factor of (n choose r).
     * </ul>
     */
    private static <E> int calculateSize(
        List<E> sortedInputList, Comparator<? super E> comparator) {
      int permutations = 1;
      int n = 1;
      int r = 1;
      while (n < sortedInputList.size()) {
        int comparison = comparator.compare(sortedInputList.get(n - 1), sortedInputList.get(n));
        if (comparison < 0) {
          // We move to the next non-repeated element.
          permutations = IntMath.saturatedMultiply(permutations, IntMath.binomial(n, r));
          r = 0;
          if (permutations == Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
          }
        }
        n++;
        r++;
      }
      return IntMath.saturatedMultiply(permutations, IntMath.binomial(n, r));
    }

    @Override
    public int size() {
      return size;
    }

    @Override
    public boolean isEmpty() {
      return false;
    }

    @Override
    public Iterator<List<E>> iterator() {
      return new OrderedPermutationIterator<E>(inputList, comparator);
    }

    @Override
    public boolean contains(@Nullable Object obj) {
      if (obj instanceof List) {
        List<?> list = (List<?>) obj;
        return isPermutation(inputList, list);
      }
      return false;
    }

    @Override
    public String toString() {
      return "orderedPermutationCollection(" + inputList + ")";
    }
  }

  private static final class OrderedPermutationIterator<E> extends AbstractIterator<List<E>> {
    @Nullable List<E> nextPermutation;
    final Comparator<? super E> comparator;

    OrderedPermutationIterator(List<E> list, Comparator<? super E> comparator) {
      this.nextPermutation = Lists.newArrayList(list);
      this.comparator = comparator;
    }

    @Override
    protected List<E> computeNext() {
      if (nextPermutation == null) {
        return endOfData();
      }
      ImmutableList<E> next = ImmutableList.copyOf(nextPermutation);
      calculateNextPermutation();
      return next;
    }

    void calculateNextPermutation() {
      int j = findNextJ();
      if (j == -1) {
        nextPermutation = null;
        return;
      }

      int l = findNextL(j);
      Collections.swap(nextPermutation, j, l);
      int n = nextPermutation.size();
      Collections.reverse(nextPermutation.subList(j + 1, n));
    }

    int findNextJ() {
      for (int k = nextPermutation.size() - 2; k >= 0; k--) {
        if (comparator.compare(nextPermutation.get(k), nextPermutation.get(k + 1)) < 0) {
          return k;
        }
      }
      return -1;
    }

    int findNextL(int j) {
      E ak = nextPermutation.get(j);
      for (int l = nextPermutation.size() - 1; l > j; l--) {
        if (comparator.compare(ak, nextPermutation.get(l)) < 0) {
          return l;
        }
      }
      throw new AssertionError("this statement should be unreachable");
    }
  }

  /**
   * Returns a {@link Collection} of all the permutations of the specified {@link Collection}.
   *
   * <p><i>Notes:</i> This is an implementation of the Plain Changes algorithm for permutations
   * generation, described in Knuth's "The Art of Computer Programming", Volume 4, Chapter 7,
   * Section 7.2.1.2.
   *
   * <p>If the input list contains equal elements, some of the generated permutations will be equal.
   *
   * <p>An empty collection has only one permutation, which is an empty list.
   *
   * @param elements the original collection whose elements have to be permuted.
   * @return an immutable {@link Collection} containing all the different permutations of the
   *     original collection.
   * @throws NullPointerException if the specified collection is null or has any null elements.
   * @since 12.0
   */
  @Beta
  public static <E> Collection<List<E>> permutations(Collection<E> elements) {
    return new PermutationCollection<E>(ImmutableList.copyOf(elements));
  }

  private static final class PermutationCollection<E> extends AbstractCollection<List<E>> {
    final ImmutableList<E> inputList;

    PermutationCollection(ImmutableList<E> input) {
      this.inputList = input;
    }

    @Override
    public int size() {
      return IntMath.factorial(inputList.size());
    }

    @Override
    public boolean isEmpty() {
      return false;
    }

    @Override
    public Iterator<List<E>> iterator() {
      return new PermutationIterator<E>(inputList);
    }

    @Override
    public boolean contains(@Nullable Object obj) {
      if (obj instanceof List) {
        List<?> list = (List<?>) obj;
        return isPermutation(inputList, list);
      }
      return false;
    }

    @Override
    public String toString() {
      return "permutations(" + inputList + ")";
    }
  }

  private static class PermutationIterator<E> extends AbstractIterator<List<E>> {
    final List<E> list;
    final int[] c;
    final int[] o;
    int j;

    PermutationIterator(List<E> list) {
      this.list = new ArrayList<E>(list);
      int n = list.size();
      c = new int[n];
      o = new int[n];
      Arrays.fill(c, 0);
      Arrays.fill(o, 1);
      j = Integer.MAX_VALUE;
    }

    @Override
    protected List<E> computeNext() {
      if (j <= 0) {
        return endOfData();
      }
      ImmutableList<E> next = ImmutableList.copyOf(list);
      calculateNextPermutation();
      return next;
    }

    void calculateNextPermutation() {
      j = list.size() - 1;
      int s = 0;

      // Handle the special case of an empty list. Skip the calculation of the
      // next permutation.
      if (j == -1) {
        return;
      }

      while (true) {
        int q = c[j] + o[j];
        if (q < 0) {
          switchDirection();
          continue;
        }
        if (q == j + 1) {
          if (j == 0) {
            break;
          }
          s++;
          switchDirection();
          continue;
        }

        Collections.swap(list, j - c[j] + s, j - q + s);
        c[j] = q;
        break;
      }
    }

    void switchDirection() {
      o[j] = -o[j];
      j--;
    }
  }

  /** Returns {@code true} if the second list is a permutation of the first. */
  private static boolean isPermutation(List<?> first, List<?> second) {
    if (first.size() != second.size()) {
      return false;
    }
    Multiset<?> firstMultiset = HashMultiset.create(first);
    Multiset<?> secondMultiset = HashMultiset.create(second);
    return firstMultiset.equals(secondMultiset);
  }
}
