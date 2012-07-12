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

import com.google.common.annotations.Beta;
import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.Nullable;

/**
 * A comparator, with additional methods to support common operations. This is
 * an "enriched" version of {@code Comparator}, in the same sense that {@link
 * FluentIterable} is an enriched {@link Iterable}). For example: <pre>   {@code
 *
 *   if (Ordering.from(comparator).reverse().isOrdered(list)) { ... }}</pre>
 *
 * The {@link #from(Comparator)} method returns the equivalent {@code Ordering}
 * instance for a pre-existing comparator. You can also skip the comparator step
 * and extend {@code Ordering} directly: <pre>   {@code
 *
 *   Ordering<String> byLengthOrdering = new Ordering<String>() {
 *     public int compare(String left, String right) {
 *       return Ints.compare(left.length(), right.length());
 *     }
 *   };}</pre>
 *
 * Except as noted, the orderings returned by the factory methods of this
 * class are serializable if and only if the provided instances that back them
 * are. For example, if {@code ordering} and {@code function} can themselves be
 * serialized, then {@code ordering.onResultOf(function)} can as well.
 *
 * <p>See the Guava User Guide article on <a href=
 * "http://code.google.com/p/guava-libraries/wiki/OrderingExplained">
 * {@code Ordering}</a>.
 *
 * @author Jesse Wilson
 * @author Kevin Bourrillion
 * @since 2.0 (imported from Google Collections Library)
 */
@GwtCompatible
public abstract class Ordering<T> implements Comparator<T> {
  // Natural order

  /**
   * Returns a serializable ordering that uses the natural order of the values.
   * The ordering throws a {@link NullPointerException} when passed a null
   * parameter.
   *
   * <p>The type specification is {@code <C extends Comparable>}, instead of
   * the technically correct {@code <C extends Comparable<? super C>>}, to
   * support legacy types from before Java 5.
   */
  @GwtCompatible(serializable = true)
  @SuppressWarnings("unchecked") // TODO(kevinb): right way to explain this??
  public static <C extends Comparable> Ordering<C> natural() {
    return (Ordering<C>) NaturalOrdering.INSTANCE;
  }

  // Static factories

  /**
   * Returns an ordering based on an <i>existing</i> comparator instance. Note
   * that there's no need to create a <i>new</i> comparator just to pass it in
   * here; simply subclass {@code Ordering} and implement its {@code compareTo}
   * method directly instead.
   *
   * @param comparator the comparator that defines the order
   * @return comparator itself if it is already an {@code Ordering}; otherwise
   *     an ordering that wraps that comparator
   */
  @GwtCompatible(serializable = true)
  public static <T> Ordering<T> from(Comparator<T> comparator) {
    return (comparator instanceof Ordering)
        ? (Ordering<T>) comparator
        : new ComparatorOrdering<T>(comparator);
  }

  /**
   * Simply returns its argument.
   *
   * @deprecated no need to use this
   */
  @GwtCompatible(serializable = true)
  @Deprecated public static <T> Ordering<T> from(Ordering<T> ordering) {
    return checkNotNull(ordering);
  }

  /**
   * Returns an ordering that compares objects according to the order in
   * which they appear in the given list. Only objects present in the list
   * (according to {@link Object#equals}) may be compared. This comparator
   * imposes a "partial ordering" over the type {@code T}. Subsequent changes
   * to the {@code valuesInOrder} list will have no effect on the returned
   * comparator. Null values in the list are not supported.
   *
   * <p>The returned comparator throws an {@link ClassCastException} when it
   * receives an input parameter that isn't among the provided values.
   *
   * <p>The generated comparator is serializable if all the provided values are
   * serializable.
   *
   * @param valuesInOrder the values that the returned comparator will be able
   *     to compare, in the order the comparator should induce
   * @return the comparator described above
   * @throws NullPointerException if any of the provided values is null
   * @throws IllegalArgumentException if {@code valuesInOrder} contains any
   *     duplicate values (according to {@link Object#equals})
   */
  @GwtCompatible(serializable = true)
  public static <T> Ordering<T> explicit(List<T> valuesInOrder) {
    return new ExplicitOrdering<T>(valuesInOrder);
  }

  /**
   * Returns an ordering that compares objects according to the order in
   * which they are given to this method. Only objects present in the argument
   * list (according to {@link Object#equals}) may be compared. This comparator
   * imposes a "partial ordering" over the type {@code T}. Null values in the
   * argument list are not supported.
   *
   * <p>The returned comparator throws a {@link ClassCastException} when it
   * receives an input parameter that isn't among the provided values.
   *
   * <p>The generated comparator is serializable if all the provided values are
   * serializable.
   *
   * @param leastValue the value which the returned comparator should consider
   *     the "least" of all values
   * @param remainingValuesInOrder the rest of the values that the returned
   *     comparator will be able to compare, in the order the comparator should
   *     follow
   * @return the comparator described above
   * @throws NullPointerException if any of the provided values is null
   * @throws IllegalArgumentException if any duplicate values (according to
   *     {@link Object#equals(Object)}) are present among the method arguments
   */
  @GwtCompatible(serializable = true)
  public static <T> Ordering<T> explicit(
      T leastValue, T... remainingValuesInOrder) {
    return explicit(Lists.asList(leastValue, remainingValuesInOrder));
  }

  // Ordering<Object> singletons

  /**
   * Returns an ordering which treats all values as equal, indicating "no
   * ordering." Passing this ordering to any <i>stable</i> sort algorithm
   * results in no change to the order of elements. Note especially that {@link
   * #sortedCopy} and {@link #immutableSortedCopy} are stable, and in the
   * returned instance these are implemented by simply copying the source list.
   *
   * <p>Example: <pre>   {@code
   *
   *   Ordering.allEqual().nullsLast().sortedCopy(
   *       asList(t, null, e, s, null, t, null))}</pre>
   *
   * Assuming {@code t}, {@code e} and {@code s} are non-null, this returns
   * {@code [t, e, s, t, null, null, null]} regardlesss of the true comparison
   * order of those three values (which might not even implement {@link
   * Comparable} at all).
   *
   * <p><b>Warning:</b> by definition, this comparator is not <i>consistent with
   * equals</i> (as defined {@linkplain Comparator here}). Avoid its use in
   * APIs, such as {@link TreeSet#TreeSet(Comparator)}, where such consistency
   * is expected.
   *
   * <p>The returned comparator is serializable.
   */
  @GwtCompatible(serializable = true)
  @SuppressWarnings("unchecked")
  public static Ordering<Object> allEqual() {
    return AllEqualOrdering.INSTANCE;
  }

  /**
   * Returns an ordering that compares objects by the natural ordering of their
   * string representations as returned by {@code toString()}. It does not
   * support null values.
   *
   * <p>The comparator is serializable.
   */
  @GwtCompatible(serializable = true)
  public static Ordering<Object> usingToString() {
    return UsingToStringOrdering.INSTANCE;
  }

  /**
   * Returns an arbitrary ordering over all objects, for which {@code compare(a,
   * b) == 0} implies {@code a == b} (identity equality). There is no meaning
   * whatsoever to the order imposed, but it is constant for the life of the VM.
   *
   * <p>Because the ordering is identity-based, it is not "consistent with
   * {@link Object#equals(Object)}" as defined by {@link Comparator}. Use
   * caution when building a {@link SortedSet} or {@link SortedMap} from it, as
   * the resulting collection will not behave exactly according to spec.
   *
   * <p>This ordering is not serializable, as its implementation relies on
   * {@link System#identityHashCode(Object)}, so its behavior cannot be
   * preserved across serialization.
   *
   * @since 2.0
   */
  public static Ordering<Object> arbitrary() {
    return ArbitraryOrderingHolder.ARBITRARY_ORDERING;
  }

  private static class ArbitraryOrderingHolder {
    static final Ordering<Object> ARBITRARY_ORDERING = new ArbitraryOrdering();
  }

  @VisibleForTesting static class ArbitraryOrdering extends Ordering<Object> {
    @SuppressWarnings("deprecation") // TODO(kevinb): ?
    private Map<Object, Integer> uids =
        Platform.tryWeakKeys(new MapMaker()).makeComputingMap(
            new Function<Object, Integer>() {
              final AtomicInteger counter = new AtomicInteger(0);
              @Override
              public Integer apply(Object from) {
                return counter.getAndIncrement();
              }
            });

    @Override public int compare(Object left, Object right) {
      if (left == right) {
        return 0;
      } else if (left == null) {
        return -1;
      } else if (right == null) {
        return 1;
      }
      int leftCode = identityHashCode(left);
      int rightCode = identityHashCode(right);
      if (leftCode != rightCode) {
        return leftCode < rightCode ? -1 : 1;
      }

      // identityHashCode collision (rare, but not as rare as you'd think)
      int result = uids.get(left).compareTo(uids.get(right));
      if (result == 0) {
        throw new AssertionError(); // extremely, extremely unlikely.
      }
      return result;
    }

    @Override public String toString() {
      return "Ordering.arbitrary()";
    }

    /*
     * We need to be able to mock identityHashCode() calls for tests, because it
     * can take 1-10 seconds to find colliding objects. Mocking frameworks that
     * can do magic to mock static method calls still can't do so for a system
     * class, so we need the indirection. In production, Hotspot should still
     * recognize that the call is 1-morphic and should still be willing to
     * inline it if necessary.
     */
    int identityHashCode(Object object) {
      return System.identityHashCode(object);
    }
  }

  // Constructor

  /**
   * Constructs a new instance of this class (only invokable by the subclass
   * constructor, typically implicit).
   */
  protected Ordering() {}

  // Instance-based factories (and any static equivalents)

  /**
   * Returns the reverse of this ordering; the {@code Ordering} equivalent to
   * {@link Collections#reverseOrder(Comparator)}.
   */
  // type parameter <S> lets us avoid the extra <String> in statements like:
  // Ordering<String> o = Ordering.<String>natural().reverse();
  @GwtCompatible(serializable = true)
  public <S extends T> Ordering<S> reverse() {
    return new ReverseOrdering<S>(this);
  }

  /**
   * Returns an ordering that treats {@code null} as less than all other values
   * and uses {@code this} to compare non-null values.
   */
  // type parameter <S> lets us avoid the extra <String> in statements like:
  // Ordering<String> o = Ordering.<String>natural().nullsFirst();
  @GwtCompatible(serializable = true)
  public <S extends T> Ordering<S> nullsFirst() {
    return new NullsFirstOrdering<S>(this);
  }

  /**
   * Returns an ordering that treats {@code null} as greater than all other
   * values and uses this ordering to compare non-null values.
   */
  // type parameter <S> lets us avoid the extra <String> in statements like:
  // Ordering<String> o = Ordering.<String>natural().nullsLast();
  @GwtCompatible(serializable = true)
  public <S extends T> Ordering<S> nullsLast() {
    return new NullsLastOrdering<S>(this);
  }

  /**
   * Returns a new ordering on {@code F} which orders elements by first applying
   * a function to them, then comparing those results using {@code this}. For
   * example, to compare objects by their string forms, in a case-insensitive
   * manner, use: <pre>   {@code
   *
   *   Ordering.from(String.CASE_INSENSITIVE_ORDER)
   *       .onResultOf(Functions.toStringFunction())}</pre>
   */
  @GwtCompatible(serializable = true)
  public <F> Ordering<F> onResultOf(Function<F, ? extends T> function) {
    return new ByFunctionOrdering<F, T>(function, this);
  }

  /**
   * Returns an ordering which first uses the ordering {@code this}, but which
   * in the event of a "tie", then delegates to {@code secondaryComparator}.
   * For example, to sort a bug list first by status and second by priority, you
   * might use {@code byStatus.compound(byPriority)}. For a compound ordering
   * with three or more components, simply chain multiple calls to this method.
   *
   * <p>An ordering produced by this method, or a chain of calls to this method,
   * is equivalent to one created using {@link Ordering#compound(Iterable)} on
   * the same component comparators.
   */
  @GwtCompatible(serializable = true)
  public <U extends T> Ordering<U> compound(
      Comparator<? super U> secondaryComparator) {
    return new CompoundOrdering<U>(this, checkNotNull(secondaryComparator));
  }

  /**
   * Returns an ordering which tries each given comparator in order until a
   * non-zero result is found, returning that result, and returning zero only if
   * all comparators return zero. The returned ordering is based on the state of
   * the {@code comparators} iterable at the time it was provided to this
   * method.
   *
   * <p>The returned ordering is equivalent to that produced using {@code
   * Ordering.from(comp1).compound(comp2).compound(comp3) . . .}.
   *
   * <p><b>Warning:</b> Supplying an argument with undefined iteration order,
   * such as a {@link HashSet}, will produce non-deterministic results.
   *
   * @param comparators the comparators to try in order
   */
  @GwtCompatible(serializable = true)
  public static <T> Ordering<T> compound(
      Iterable<? extends Comparator<? super T>> comparators) {
    return new CompoundOrdering<T>(comparators);
  }

  /**
   * Returns a new ordering which sorts iterables by comparing corresponding
   * elements pairwise until a nonzero result is found; imposes "dictionary
   * order". If the end of one iterable is reached, but not the other, the
   * shorter iterable is considered to be less than the longer one. For example,
   * a lexicographical natural ordering over integers considers {@code
   * [] < [1] < [1, 1] < [1, 2] < [2]}.
   *
   * <p>Note that {@code ordering.lexicographical().reverse()} is not
   * equivalent to {@code ordering.reverse().lexicographical()} (consider how
   * each would order {@code [1]} and {@code [1, 1]}).
   *
   * @since 2.0
   */
  @GwtCompatible(serializable = true)
  // type parameter <S> lets us avoid the extra <String> in statements like:
  // Ordering<Iterable<String>> o =
  //     Ordering.<String>natural().lexicographical();
  public <S extends T> Ordering<Iterable<S>> lexicographical() {
    /*
     * Note that technically the returned ordering should be capable of
     * handling not just {@code Iterable<S>} instances, but also any {@code
     * Iterable<? extends S>}. However, the need for this comes up so rarely
     * that it doesn't justify making everyone else deal with the very ugly
     * wildcard.
     */
    return new LexicographicalOrdering<S>(this);
  }

  // Regular instance methods

  // Override to add @Nullable
  @Override public abstract int compare(@Nullable T left, @Nullable T right);

  /**
   * Returns the least of the specified values according to this ordering. If
   * there are multiple least values, the first of those is returned. The
   * iterator will be left exhausted: its {@code hasNext()} method will return
   * {@code false}.
   *
   * @param iterator the iterator whose minimum element is to be determined
   * @throws NoSuchElementException if {@code iterator} is empty
   * @throws ClassCastException if the parameters are not <i>mutually
   *     comparable</i> under this ordering.
   *
   * @since 11.0
   */
  @Beta
  public <E extends T> E min(Iterator<E> iterator) {
    // let this throw NoSuchElementException as necessary
    E minSoFar = iterator.next();

    while (iterator.hasNext()) {
      minSoFar = min(minSoFar, iterator.next());
    }

    return minSoFar;
  }

  /**
   * Returns the least of the specified values according to this ordering. If
   * there are multiple least values, the first of those is returned.
   *
   * @param iterable the iterable whose minimum element is to be determined
   * @throws NoSuchElementException if {@code iterable} is empty
   * @throws ClassCastException if the parameters are not <i>mutually
   *     comparable</i> under this ordering.
   */
  public <E extends T> E min(Iterable<E> iterable) {
    return min(iterable.iterator());
  }

  /**
   * Returns the lesser of the two values according to this ordering. If the
   * values compare as 0, the first is returned.
   *
   * <p><b>Implementation note:</b> this method is invoked by the default
   * implementations of the other {@code min} overloads, so overriding it will
   * affect their behavior.
   *
   * @param a value to compare, returned if less than or equal to b.
   * @param b value to compare.
   * @throws ClassCastException if the parameters are not <i>mutually
   *     comparable</i> under this ordering.
   */
  public <E extends T> E min(@Nullable E a, @Nullable E b) {
    return compare(a, b) <= 0 ? a : b;
  }

  /**
   * Returns the least of the specified values according to this ordering. If
   * there are multiple least values, the first of those is returned.
   *
   * @param a value to compare, returned if less than or equal to the rest.
   * @param b value to compare
   * @param c value to compare
   * @param rest values to compare
   * @throws ClassCastException if the parameters are not <i>mutually
   *     comparable</i> under this ordering.
   */
  public <E extends T> E min(
      @Nullable E a, @Nullable E b, @Nullable E c, E... rest) {
    E minSoFar = min(min(a, b), c);

    for (E r : rest) {
      minSoFar = min(minSoFar, r);
    }

    return minSoFar;
  }

  /**
   * Returns the greatest of the specified values according to this ordering. If
   * there are multiple greatest values, the first of those is returned. The
   * iterator will be left exhausted: its {@code hasNext()} method will return
   * {@code false}.
   *
   * @param iterator the iterator whose maximum element is to be determined
   * @throws NoSuchElementException if {@code iterator} is empty
   * @throws ClassCastException if the parameters are not <i>mutually
   *     comparable</i> under this ordering.
   *
   * @since 11.0
   */
  @Beta
  public <E extends T> E max(Iterator<E> iterator) {
    // let this throw NoSuchElementException as necessary
    E maxSoFar = iterator.next();

    while (iterator.hasNext()) {
      maxSoFar = max(maxSoFar, iterator.next());
    }

    return maxSoFar;
  }

  /**
   * Returns the greatest of the specified values according to this ordering. If
   * there are multiple greatest values, the first of those is returned.
   *
   * @param iterable the iterable whose maximum element is to be determined
   * @throws NoSuchElementException if {@code iterable} is empty
   * @throws ClassCastException if the parameters are not <i>mutually
   *     comparable</i> under this ordering.
   */
  public <E extends T> E max(Iterable<E> iterable) {
    return max(iterable.iterator());
  }

  /**
   * Returns the greater of the two values according to this ordering. If the
   * values compare as 0, the first is returned.
   *
   * <p><b>Implementation note:</b> this method is invoked by the default
   * implementations of the other {@code max} overloads, so overriding it will
   * affect their behavior.
   *
   * @param a value to compare, returned if greater than or equal to b.
   * @param b value to compare.
   * @throws ClassCastException if the parameters are not <i>mutually
   *     comparable</i> under this ordering.
   */
  public <E extends T> E max(@Nullable E a, @Nullable E b) {
    return compare(a, b) >= 0 ? a : b;
  }

  /**
   * Returns the greatest of the specified values according to this ordering. If
   * there are multiple greatest values, the first of those is returned.
   *
   * @param a value to compare, returned if greater than or equal to the rest.
   * @param b value to compare
   * @param c value to compare
   * @param rest values to compare
   * @throws ClassCastException if the parameters are not <i>mutually
   *     comparable</i> under this ordering.
   */
  public <E extends T> E max(
      @Nullable E a, @Nullable E b, @Nullable E c, E... rest) {
    E maxSoFar = max(max(a, b), c);

    for (E r : rest) {
      maxSoFar = max(maxSoFar, r);
    }

    return maxSoFar;
  }

  /**
   * Returns the {@code k} least elements of the given iterable according to
   * this ordering, in order from least to greatest.  If there are fewer than
   * {@code k} elements present, all will be included.
   *
   * <p>The implementation does not necessarily use a <i>stable</i> sorting
   * algorithm; when multiple elements are equivalent, it is undefined which
   * will come first.
   *
   * @return an immutable {@code RandomAccess} list of the {@code k} least
   *     elements in ascending order
   * @throws IllegalArgumentException if {@code k} is negative
   * @since 8.0
   */
  @Beta
  public <E extends T> List<E> leastOf(Iterable<E> iterable, int k) {
    checkArgument(k >= 0, "%d is negative", k);

    // values is not an E[], but we use it as such for readability. Hack.
    @SuppressWarnings("unchecked")
    E[] values = (E[]) Iterables.toArray(iterable);

    // TODO(nshupe): also sort whole list if k is *near* values.length?
    // TODO(kevinb): benchmark this impl against hand-coded heap
    E[] resultArray;
    if (values.length <= k) {
      Arrays.sort(values, this);
      resultArray = values;
    } else {
      quicksortLeastK(values, 0, values.length - 1, k);

      // this is not an E[], but we use it as such for readability. Hack.
      @SuppressWarnings("unchecked")
      E[] tmp = (E[]) new Object[k];
      resultArray = tmp;
      System.arraycopy(values, 0, resultArray, 0, k);
    }

    return Collections.unmodifiableList(Arrays.asList(resultArray));
  }

  /**
   * Returns the {@code k} greatest elements of the given iterable according to
   * this ordering, in order from greatest to least. If there are fewer than
   * {@code k} elements present, all will be included.
   *
   * <p>The implementation does not necessarily use a <i>stable</i> sorting
   * algorithm; when multiple elements are equivalent, it is undefined which
   * will come first.
   *
   * @return an immutable {@code RandomAccess} list of the {@code k} greatest
   *     elements in <i>descending order</i>
   * @throws IllegalArgumentException if {@code k} is negative
   * @since 8.0
   */
  @Beta
  public <E extends T> List<E> greatestOf(Iterable<E> iterable, int k) {
    // TODO(kevinb): see if delegation is hurting performance noticeably
    // TODO(kevinb): if we change this implementation, add full unit tests.
    return reverse().leastOf(iterable, k);
  }

  private <E extends T> void quicksortLeastK(
      E[] values, int left, int right, int k) {
    if (right > left) {
      int pivotIndex = (left + right) >>> 1; // left + ((right - left) / 2)
      int pivotNewIndex = partition(values, left, right, pivotIndex);
      quicksortLeastK(values, left, pivotNewIndex - 1, k);
      if (pivotNewIndex < k) {
        quicksortLeastK(values, pivotNewIndex + 1, right, k);
      }
    }
  }

  private <E extends T> int partition(
      E[] values, int left, int right, int pivotIndex) {
    E pivotValue = values[pivotIndex];

    values[pivotIndex] = values[right];
    values[right] = pivotValue;

    int storeIndex = left;
    for (int i = left; i < right; i++) {
      if (compare(values[i], pivotValue) < 0) {
        ObjectArrays.swap(values, storeIndex, i);
        storeIndex++;
      }
    }
    ObjectArrays.swap(values, right, storeIndex);
    return storeIndex;
  }

  /**
   * Returns a copy of the given iterable sorted by this ordering. The input is
   * not modified. The returned list is modifiable, serializable, and has random
   * access.
   *
   * <p>Unlike {@link Sets#newTreeSet(Iterable)}, this method does not discard
   * elements that are duplicates according to the comparator. The sort
   * performed is <i>stable</i>, meaning that such elements will appear in the
   * resulting list in the same order they appeared in the input.
   *
   * @param iterable the elements to be copied and sorted
   * @return a new list containing the given elements in sorted order
   */
  public <E extends T> List<E> sortedCopy(Iterable<E> iterable) {
    @SuppressWarnings("unchecked") // does not escape, and contains only E's
    E[] array = (E[]) Iterables.toArray(iterable);
    Arrays.sort(array, this);
    return Lists.newArrayList(Arrays.asList(array));
  }

  /**
   * Returns an <i>immutable</i> copy of the given iterable sorted by this
   * ordering. The input is not modified.
   *
   * <p>Unlike {@link Sets#newTreeSet(Iterable)}, this method does not discard
   * elements that are duplicates according to the comparator. The sort
   * performed is <i>stable</i>, meaning that such elements will appear in the
   * resulting list in the same order they appeared in the input.
   *
   * @param iterable the elements to be copied and sorted
   * @return a new immutable list containing the given elements in sorted order
   * @throws NullPointerException if {@code iterable} or any of its elements is
   *     null
   * @since 3.0
   */
  public <E extends T> ImmutableList<E> immutableSortedCopy(
      Iterable<E> iterable) {
    @SuppressWarnings("unchecked") // we'll only ever have E's in here
    E[] elements = (E[]) Iterables.toArray(iterable);
    for (E e : elements) {
      checkNotNull(e);
    }
    Arrays.sort(elements, this);
    return ImmutableList.asImmutableList(elements);
  }

  /**
   * Returns {@code true} if each element in {@code iterable} after the first is
   * greater than or equal to the element that preceded it, according to this
   * ordering. Note that this is always true when the iterable has fewer than
   * two elements.
   */
  public boolean isOrdered(Iterable<? extends T> iterable) {
    Iterator<? extends T> it = iterable.iterator();
    if (it.hasNext()) {
      T prev = it.next();
      while (it.hasNext()) {
        T next = it.next();
        if (compare(prev, next) > 0) {
          return false;
        }
        prev = next;
      }
    }
    return true;
  }

  /**
   * Returns {@code true} if each element in {@code iterable} after the first is
   * <i>strictly</i> greater than the element that preceded it, according to
   * this ordering. Note that this is always true when the iterable has fewer
   * than two elements.
   */
  public boolean isStrictlyOrdered(Iterable<? extends T> iterable) {
    Iterator<? extends T> it = iterable.iterator();
    if (it.hasNext()) {
      T prev = it.next();
      while (it.hasNext()) {
        T next = it.next();
        if (compare(prev, next) >= 0) {
          return false;
        }
        prev = next;
      }
    }
    return true;
  }

  /**
   * {@link Collections#binarySearch(List, Object, Comparator) Searches}
   * {@code sortedList} for {@code key} using the binary search algorithm. The
   * list must be sorted using this ordering.
   *
   * @param sortedList the list to be searched
   * @param key the key to be searched for
   */
  public int binarySearch(List<? extends T> sortedList, @Nullable T key) {
    return Collections.binarySearch(sortedList, key, this);
  }

  /**
   * Exception thrown by a {@link Ordering#explicit(List)} or {@link
   * Ordering#explicit(Object, Object[])} comparator when comparing a value
   * outside the set of values it can compare. Extending {@link
   * ClassCastException} may seem odd, but it is required.
   */
  // TODO(kevinb): make this public, document it right
  @VisibleForTesting
  static class IncomparableValueException extends ClassCastException {
    final Object value;

    IncomparableValueException(Object value) {
      super("Cannot compare value: " + value);
      this.value = value;
    }

    private static final long serialVersionUID = 0;
  }

  // Never make these public
  static final int LEFT_IS_GREATER = 1;
  static final int RIGHT_IS_GREATER = -1;
}
