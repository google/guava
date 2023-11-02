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

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.CollectPreconditions.checkNonnegative;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.J2ktIncompatible;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import javax.annotation.CheckForNull;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * A comparator, with additional methods to support common operations. This is an "enriched" version
 * of {@code Comparator} for pre-Java-8 users, in the same sense that {@link FluentIterable} is an
 * enriched {@link Iterable} for pre-Java-8 users.
 *
 * <h3>Three types of methods</h3>
 *
 * Like other fluent types, there are three types of methods present: methods for <i>acquiring</i>,
 * <i>chaining</i>, and <i>using</i>.
 *
 * <h4>Acquiring</h4>
 *
 * <p>The common ways to get an instance of {@code Ordering} are:
 *
 * <ul>
 *   <li>Subclass it and implement {@link #compare} instead of implementing {@link Comparator}
 *       directly
 *   <li>Pass a <i>pre-existing</i> {@link Comparator} instance to {@link #from(Comparator)}
 *   <li>Use the natural ordering, {@link Ordering#natural}
 * </ul>
 *
 * <h4>Chaining</h4>
 *
 * <p>Then you can use the <i>chaining</i> methods to get an altered version of that {@code
 * Ordering}, including:
 *
 * <ul>
 *   <li>{@link #reverse}
 *   <li>{@link #compound(Comparator)}
 *   <li>{@link #onResultOf(Function)}
 *   <li>{@link #nullsFirst} / {@link #nullsLast}
 * </ul>
 *
 * <h4>Using</h4>
 *
 * <p>Finally, use the resulting {@code Ordering} anywhere a {@link Comparator} is required, or use
 * any of its special operations, such as:
 *
 * <ul>
 *   <li>{@link #immutableSortedCopy}
 *   <li>{@link #isOrdered} / {@link #isStrictlyOrdered}
 *   <li>{@link #min} / {@link #max}
 * </ul>
 *
 * <h3>Understanding complex orderings</h3>
 *
 * <p>Complex chained orderings like the following example can be challenging to understand.
 *
 * <pre>{@code
 * Ordering<Foo> ordering =
 *     Ordering.natural()
 *         .nullsFirst()
 *         .onResultOf(getBarFunction)
 *         .nullsLast();
 * }</pre>
 *
 * Note that each chaining method returns a new ordering instance which is backed by the previous
 * instance, but has the chance to act on values <i>before</i> handing off to that backing instance.
 * As a result, it usually helps to read chained ordering expressions <i>backwards</i>. For example,
 * when {@code compare} is called on the above ordering:
 *
 * <ol>
 *   <li>First, if only one {@code Foo} is null, that null value is treated as <i>greater</i>
 *   <li>Next, non-null {@code Foo} values are passed to {@code getBarFunction} (we will be
 *       comparing {@code Bar} values from now on)
 *   <li>Next, if only one {@code Bar} is null, that null value is treated as <i>lesser</i>
 *   <li>Finally, natural ordering is used (i.e. the result of {@code Bar.compareTo(Bar)} is
 *       returned)
 * </ol>
 *
 * <p>Alas, {@link #reverse} is a little different. As you read backwards through a chain and
 * encounter a call to {@code reverse}, continue working backwards until a result is determined, and
 * then reverse that result.
 *
 * <h3>Additional notes</h3>
 *
 * <p>Except as noted, the orderings returned by the factory methods of this class are serializable
 * if and only if the provided instances that back them are. For example, if {@code ordering} and
 * {@code function} can themselves be serialized, then {@code ordering.onResultOf(function)} can as
 * well.
 *
 * <h3>For Java 8 users</h3>
 *
 * <p>If you are using Java 8, this class is now obsolete. Most of its functionality is now provided
 * by {@link java.util.stream.Stream Stream} and by {@link Comparator} itself, and the rest can now
 * be found as static methods in our new {@link Comparators} class. See each method below for
 * further instructions. Whenever possible, you should change any references of type {@code
 * Ordering} to be of type {@code Comparator} instead. However, at this time we have no plan to
 * <i>deprecate</i> this class.
 *
 * <p>Many replacements involve adopting {@code Stream}, and these changes can sometimes make your
 * code verbose. Whenever following this advice, you should check whether {@code Stream} could be
 * adopted more comprehensively in your code; the end result may be quite a bit simpler.
 *
 * <h3>See also</h3>
 *
 * <p>See the Guava User Guide article on <a href=
 * "https://github.com/google/guava/wiki/OrderingExplained">{@code Ordering}</a>.
 *
 * @author Jesse Wilson
 * @author Kevin Bourrillion
 * @since 2.0
 */
@GwtCompatible
@ElementTypesAreNonnullByDefault
public abstract class Ordering<T extends @Nullable Object> implements Comparator<T> {
  // Natural order

  /**
   * Returns a serializable ordering that uses the natural order of the values. The ordering throws
   * a {@link NullPointerException} when passed a null parameter.
   *
   * <p>The type specification is {@code <C extends Comparable>}, instead of the technically correct
   * {@code <C extends Comparable<? super C>>}, to support legacy types from before Java 5.
   *
   * <p><b>Java 8 users:</b> use {@link Comparator#naturalOrder} instead.
   */
  @GwtCompatible(serializable = true)
  @SuppressWarnings("unchecked") // TODO(kevinb): right way to explain this??
  public static <C extends Comparable> Ordering<C> natural() {
    return (Ordering<C>) NaturalOrdering.INSTANCE;
  }

  // Static factories

  /**
   * Returns an ordering based on an <i>existing</i> comparator instance. Note that it is
   * unnecessary to create a <i>new</i> anonymous inner class implementing {@code Comparator} just
   * to pass it in here. Instead, simply subclass {@code Ordering} and implement its {@code compare}
   * method directly.
   *
   * <p>The returned object is serializable if {@code comparator} is serializable.
   *
   * <p><b>Java 8 users:</b> this class is now obsolete as explained in the class documentation, so
   * there is no need to use this method.
   *
   * @param comparator the comparator that defines the order
   * @return comparator itself if it is already an {@code Ordering}; otherwise an ordering that
   *     wraps that comparator
   */
  @GwtCompatible(serializable = true)
  public static <T extends @Nullable Object> Ordering<T> from(Comparator<T> comparator) {
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
  @Deprecated
  public static <T extends @Nullable Object> Ordering<T> from(Ordering<T> ordering) {
    return checkNotNull(ordering);
  }

  /**
   * Returns an ordering that compares objects according to the order in which they appear in the
   * given list. Only objects present in the list (according to {@link Object#equals}) may be
   * compared. This comparator imposes a "partial ordering" over the type {@code T}. Subsequent
   * changes to the {@code valuesInOrder} list will have no effect on the returned comparator. Null
   * values in the list are not supported.
   *
   * <p>The returned comparator throws a {@link ClassCastException} when it receives an input
   * parameter that isn't among the provided values.
   *
   * <p>The generated comparator is serializable if all the provided values are serializable.
   *
   * @param valuesInOrder the values that the returned comparator will be able to compare, in the
   *     order the comparator should induce
   * @return the comparator described above
   * @throws NullPointerException if any of the provided values is null
   * @throws IllegalArgumentException if {@code valuesInOrder} contains any duplicate values
   *     (according to {@link Object#equals})
   */
  // TODO(kevinb): provide replacement
  @GwtCompatible(serializable = true)
  public static <T> Ordering<T> explicit(List<T> valuesInOrder) {
    return new ExplicitOrdering<T>(valuesInOrder);
  }

  /**
   * Returns an ordering that compares objects according to the order in which they are given to
   * this method. Only objects present in the argument list (according to {@link Object#equals}) may
   * be compared. This comparator imposes a "partial ordering" over the type {@code T}. Null values
   * in the argument list are not supported.
   *
   * <p>The returned comparator throws a {@link ClassCastException} when it receives an input
   * parameter that isn't among the provided values.
   *
   * <p>The generated comparator is serializable if all the provided values are serializable.
   *
   * @param leastValue the value which the returned comparator should consider the "least" of all
   *     values
   * @param remainingValuesInOrder the rest of the values that the returned comparator will be able
   *     to compare, in the order the comparator should follow
   * @return the comparator described above
   * @throws NullPointerException if any of the provided values is null
   * @throws IllegalArgumentException if any duplicate values (according to {@link
   *     Object#equals(Object)}) are present among the method arguments
   */
  // TODO(kevinb): provide replacement
  @GwtCompatible(serializable = true)
  public static <T> Ordering<T> explicit(T leastValue, T... remainingValuesInOrder) {
    return explicit(Lists.asList(leastValue, remainingValuesInOrder));
  }

  // Ordering<Object> singletons

  /**
   * Returns an ordering which treats all values as equal, indicating "no ordering." Passing this
   * ordering to any <i>stable</i> sort algorithm results in no change to the order of elements.
   * Note especially that {@link #sortedCopy} and {@link #immutableSortedCopy} are stable, and in
   * the returned instance these are implemented by simply copying the source list.
   *
   * <p>Example:
   *
   * <pre>{@code
   * Ordering.allEqual().nullsLast().sortedCopy(
   *     asList(t, null, e, s, null, t, null))
   * }</pre>
   *
   * <p>Assuming {@code t}, {@code e} and {@code s} are non-null, this returns {@code [t, e, s, t,
   * null, null, null]} regardless of the true comparison order of those three values (which might
   * not even implement {@link Comparable} at all).
   *
   * <p><b>Warning:</b> by definition, this comparator is not <i>consistent with equals</i> (as
   * defined {@linkplain Comparator here}). Avoid its use in APIs, such as {@link
   * TreeSet#TreeSet(Comparator)}, where such consistency is expected.
   *
   * <p>The returned comparator is serializable.
   *
   * <p><b>Java 8 users:</b> Use the lambda expression {@code (a, b) -> 0} instead (in certain cases
   * you may need to cast that to {@code Comparator<YourType>}).
   *
   * @since 13.0
   */
  @GwtCompatible(serializable = true)
  @SuppressWarnings("unchecked")
  public static Ordering<@Nullable Object> allEqual() {
    return AllEqualOrdering.INSTANCE;
  }

  /**
   * Returns an ordering that compares objects by the natural ordering of their string
   * representations as returned by {@code toString()}. It does not support null values.
   *
   * <p>The comparator is serializable.
   *
   * <p><b>Java 8 users:</b> Use {@code Comparator.comparing(Object::toString)} instead.
   */
  @GwtCompatible(serializable = true)
  public static Ordering<Object> usingToString() {
    return UsingToStringOrdering.INSTANCE;
  }

  /**
   * Returns an arbitrary ordering over all objects, for which {@code compare(a, b) == 0} implies
   * {@code a == b} (identity equality). There is no meaning whatsoever to the order imposed, but it
   * is constant for the life of the VM.
   *
   * <p>Because the ordering is identity-based, it is not "consistent with {@link
   * Object#equals(Object)}" as defined by {@link Comparator}. Use caution when building a {@link
   * SortedSet} or {@link SortedMap} from it, as the resulting collection will not behave exactly
   * according to spec.
   *
   * <p>This ordering is not serializable, as its implementation relies on {@link
   * System#identityHashCode(Object)}, so its behavior cannot be preserved across serialization.
   *
   * @since 2.0
   */
  // TODO(kevinb): copy to Comparators, etc.
  @J2ktIncompatible // MapMaker
  public static Ordering<@Nullable Object> arbitrary() {
    return ArbitraryOrderingHolder.ARBITRARY_ORDERING;
  }

  @J2ktIncompatible // MapMaker
  private static class ArbitraryOrderingHolder {
    static final Ordering<@Nullable Object> ARBITRARY_ORDERING = new ArbitraryOrdering();
  }

  @J2ktIncompatible // MapMaker
  @VisibleForTesting
  static class ArbitraryOrdering extends Ordering<@Nullable Object> {

    private final AtomicInteger counter = new AtomicInteger(0);
    private final ConcurrentMap<Object, Integer> uids =
        Platform.tryWeakKeys(new MapMaker()).makeMap();

    private Integer getUid(Object obj) {
      Integer uid = uids.get(obj);
      if (uid == null) {
        // One or more integer values could be skipped in the event of a race
        // to generate a UID for the same object from multiple threads, but
        // that shouldn't be a problem.
        uid = counter.getAndIncrement();
        Integer alreadySet = uids.putIfAbsent(obj, uid);
        if (alreadySet != null) {
          uid = alreadySet;
        }
      }
      return uid;
    }

    @Override
    public int compare(@CheckForNull Object left, @CheckForNull Object right) {
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
      int result = getUid(left).compareTo(getUid(right));
      if (result == 0) {
        throw new AssertionError(); // extremely, extremely unlikely.
      }
      return result;
    }

    @Override
    public String toString() {
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
   * Constructs a new instance of this class (only invokable by the subclass constructor, typically
   * implicit).
   */
  protected Ordering() {}

  // Instance-based factories (and any static equivalents)

  /**
   * Returns the reverse of this ordering; the {@code Ordering} equivalent to {@link
   * Collections#reverseOrder(Comparator)}.
   *
   * <p><b>Java 8 users:</b> Use {@code thisComparator.reversed()} instead.
   */
  // type parameter <S> lets us avoid the extra <String> in statements like:
  // Ordering<String> o = Ordering.<String>natural().reverse();
  @GwtCompatible(serializable = true)
  public <S extends T> Ordering<S> reverse() {
    return new ReverseOrdering<S>(this);
  }

  /**
   * Returns an ordering that treats {@code null} as less than all other values and uses {@code
   * this} to compare non-null values.
   *
   * <p>The returned object is serializable if this object is serializable.
   *
   * <p><b>Java 8 users:</b> Use {@code Comparator.nullsFirst(thisComparator)} instead.
   */
  // type parameter <S> lets us avoid the extra <String> in statements like:
  // Ordering<String> o = Ordering.<String>natural().nullsFirst();
  @GwtCompatible(serializable = true)
  public <S extends T> Ordering<@Nullable S> nullsFirst() {
    return new NullsFirstOrdering<S>(this);
  }

  /**
   * Returns an ordering that treats {@code null} as greater than all other values and uses this
   * ordering to compare non-null values.
   *
   * <p>The returned object is serializable if this object is serializable.
   *
   * <p><b>Java 8 users:</b> Use {@code Comparator.nullsLast(thisComparator)} instead.
   */
  // type parameter <S> lets us avoid the extra <String> in statements like:
  // Ordering<String> o = Ordering.<String>natural().nullsLast();
  @GwtCompatible(serializable = true)
  public <S extends T> Ordering<@Nullable S> nullsLast() {
    return new NullsLastOrdering<S>(this);
  }

  /**
   * Returns a new ordering on {@code F} which orders elements by first applying a function to them,
   * then comparing those results using {@code this}. For example, to compare objects by their
   * string forms, in a case-insensitive manner, use:
   *
   * <pre>{@code
   * Ordering.from(String.CASE_INSENSITIVE_ORDER)
   *     .onResultOf(Functions.toStringFunction())
   * }</pre>
   *
   * <p><b>Java 8 users:</b> Use {@code Comparator.comparing(function, thisComparator)} instead (you
   * can omit the comparator if it is the natural order).
   */
  @GwtCompatible(serializable = true)
  public <F extends @Nullable Object> Ordering<F> onResultOf(Function<F, ? extends T> function) {
    return new ByFunctionOrdering<>(function, this);
  }

  <T2 extends T> Ordering<Entry<T2, ?>> onKeys() {
    return onResultOf(Maps.<T2>keyFunction());
  }

  /**
   * Returns an ordering which first uses the ordering {@code this}, but which in the event of a
   * "tie", then delegates to {@code secondaryComparator}. For example, to sort a bug list first by
   * status and second by priority, you might use {@code byStatus.compound(byPriority)}. For a
   * compound ordering with three or more components, simply chain multiple calls to this method.
   *
   * <p>An ordering produced by this method, or a chain of calls to this method, is equivalent to
   * one created using {@link Ordering#compound(Iterable)} on the same component comparators.
   *
   * <p>The returned object is serializable if this object and {@code secondaryComparator} are both
   * serializable.
   *
   * <p><b>Java 8 users:</b> Use {@code thisComparator.thenComparing(secondaryComparator)} instead.
   * Depending on what {@code secondaryComparator} is, one of the other overloads of {@code
   * thenComparing} may be even more useful.
   */
  @GwtCompatible(serializable = true)
  public <U extends T> Ordering<U> compound(Comparator<? super U> secondaryComparator) {
    return new CompoundOrdering<U>(this, checkNotNull(secondaryComparator));
  }

  /**
   * Returns an ordering which tries each given comparator in order until a non-zero result is
   * found, returning that result, and returning zero only if all comparators return zero. The
   * returned ordering is based on the state of the {@code comparators} iterable at the time it was
   * provided to this method.
   *
   * <p>The returned ordering is equivalent to that produced using {@code
   * Ordering.from(comp1).compound(comp2).compound(comp3) . . .}.
   *
   * <p>The returned object is serializable if each of the {@code comparators} is serializable.
   *
   * <p><b>Warning:</b> Supplying an argument with undefined iteration order, such as a {@link
   * HashSet}, will produce non-deterministic results.
   *
   * <p><b>Java 8 users:</b> Use a chain of calls to {@link Comparator#thenComparing(Comparator)},
   * or {@code comparatorCollection.stream().reduce(Comparator::thenComparing).get()} (if the
   * collection might be empty, also provide a default comparator as the {@code identity} parameter
   * to {@code reduce}).
   *
   * @param comparators the comparators to try in order
   */
  @GwtCompatible(serializable = true)
  public static <T extends @Nullable Object> Ordering<T> compound(
      Iterable<? extends Comparator<? super T>> comparators) {
    return new CompoundOrdering<T>(comparators);
  }

  /**
   * Returns a new ordering which sorts iterables by comparing corresponding elements pairwise until
   * a nonzero result is found; imposes "dictionary order". If the end of one iterable is reached,
   * but not the other, the shorter iterable is considered to be less than the longer one. For
   * example, a lexicographical natural ordering over integers considers {@code [] < [1] < [1, 1] <
   * [1, 2] < [2]}.
   *
   * <p>Note that {@code ordering.lexicographical().reverse()} is not equivalent to {@code
   * ordering.reverse().lexicographical()} (consider how each would order {@code [1]} and {@code [1,
   * 1]}).
   *
   * <p><b>Java 8 users:</b> Use {@link Comparators#lexicographical(Comparator)} instead.
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

  @Override
  public abstract int compare(@ParametricNullness T left, @ParametricNullness T right);

  /**
   * Returns the least of the specified values according to this ordering. If there are multiple
   * least values, the first of those is returned. The iterator will be left exhausted: its {@code
   * hasNext()} method will return {@code false}.
   *
   * <p><b>Java 8 users:</b> Use {@code Streams.stream(iterator).min(thisComparator).get()} instead
   * (but note that it does not guarantee which tied minimum element is returned).
   *
   * @param iterator the iterator whose minimum element is to be determined
   * @throws NoSuchElementException if {@code iterator} is empty
   * @throws ClassCastException if the parameters are not <i>mutually comparable</i> under this
   *     ordering.
   * @since 11.0
   */
  @ParametricNullness
  public <E extends T> E min(Iterator<E> iterator) {
    // let this throw NoSuchElementException as necessary
    E minSoFar = iterator.next();

    while (iterator.hasNext()) {
      minSoFar = min(minSoFar, iterator.next());
    }

    return minSoFar;
  }

  /**
   * Returns the least of the specified values according to this ordering. If there are multiple
   * least values, the first of those is returned.
   *
   * <p><b>Java 8 users:</b> If {@code iterable} is a {@link Collection}, use {@code
   * Collections.min(collection, thisComparator)} instead. Otherwise, use {@code
   * Streams.stream(iterable).min(thisComparator).get()} instead. Note that these alternatives do
   * not guarantee which tied minimum element is returned.
   *
   * @param iterable the iterable whose minimum element is to be determined
   * @throws NoSuchElementException if {@code iterable} is empty
   * @throws ClassCastException if the parameters are not <i>mutually comparable</i> under this
   *     ordering.
   */
  @ParametricNullness
  public <E extends T> E min(Iterable<E> iterable) {
    return min(iterable.iterator());
  }

  /**
   * Returns the lesser of the two values according to this ordering. If the values compare as 0,
   * the first is returned.
   *
   * <p><b>Implementation note:</b> this method is invoked by the default implementations of the
   * other {@code min} overloads, so overriding it will affect their behavior.
   *
   * <p><b>Note:</b> Consider using {@code Comparators.min(a, b, thisComparator)} instead. If {@code
   * thisComparator} is {@link Ordering#natural}, then use {@code Comparators.min(a, b)}.
   *
   * @param a value to compare, returned if less than or equal to b.
   * @param b value to compare.
   * @throws ClassCastException if the parameters are not <i>mutually comparable</i> under this
   *     ordering.
   */
  @ParametricNullness
  public <E extends T> E min(@ParametricNullness E a, @ParametricNullness E b) {
    return (compare(a, b) <= 0) ? a : b;
  }

  /**
   * Returns the least of the specified values according to this ordering. If there are multiple
   * least values, the first of those is returned.
   *
   * <p><b>Java 8 users:</b> Use {@code Collections.min(Arrays.asList(a, b, c...), thisComparator)}
   * instead (but note that it does not guarantee which tied minimum element is returned).
   *
   * @param a value to compare, returned if less than or equal to the rest.
   * @param b value to compare
   * @param c value to compare
   * @param rest values to compare
   * @throws ClassCastException if the parameters are not <i>mutually comparable</i> under this
   *     ordering.
   */
  @ParametricNullness
  public <E extends T> E min(
      @ParametricNullness E a, @ParametricNullness E b, @ParametricNullness E c, E... rest) {
    E minSoFar = min(min(a, b), c);

    for (E r : rest) {
      minSoFar = min(minSoFar, r);
    }

    return minSoFar;
  }

  /**
   * Returns the greatest of the specified values according to this ordering. If there are multiple
   * greatest values, the first of those is returned. The iterator will be left exhausted: its
   * {@code hasNext()} method will return {@code false}.
   *
   * <p><b>Java 8 users:</b> Use {@code Streams.stream(iterator).max(thisComparator).get()} instead
   * (but note that it does not guarantee which tied maximum element is returned).
   *
   * @param iterator the iterator whose maximum element is to be determined
   * @throws NoSuchElementException if {@code iterator} is empty
   * @throws ClassCastException if the parameters are not <i>mutually comparable</i> under this
   *     ordering.
   * @since 11.0
   */
  @ParametricNullness
  public <E extends T> E max(Iterator<E> iterator) {
    // let this throw NoSuchElementException as necessary
    E maxSoFar = iterator.next();

    while (iterator.hasNext()) {
      maxSoFar = max(maxSoFar, iterator.next());
    }

    return maxSoFar;
  }

  /**
   * Returns the greatest of the specified values according to this ordering. If there are multiple
   * greatest values, the first of those is returned.
   *
   * <p><b>Java 8 users:</b> If {@code iterable} is a {@link Collection}, use {@code
   * Collections.max(collection, thisComparator)} instead. Otherwise, use {@code
   * Streams.stream(iterable).max(thisComparator).get()} instead. Note that these alternatives do
   * not guarantee which tied maximum element is returned.
   *
   * @param iterable the iterable whose maximum element is to be determined
   * @throws NoSuchElementException if {@code iterable} is empty
   * @throws ClassCastException if the parameters are not <i>mutually comparable</i> under this
   *     ordering.
   */
  @ParametricNullness
  public <E extends T> E max(Iterable<E> iterable) {
    return max(iterable.iterator());
  }

  /**
   * Returns the greater of the two values according to this ordering. If the values compare as 0,
   * the first is returned.
   *
   * <p><b>Implementation note:</b> this method is invoked by the default implementations of the
   * other {@code max} overloads, so overriding it will affect their behavior.
   *
   * <p><b>Note:</b> Consider using {@code Comparators.max(a, b, thisComparator)} instead. If {@code
   * thisComparator} is {@link Ordering#natural}, then use {@code Comparators.max(a, b)}.
   *
   * @param a value to compare, returned if greater than or equal to b.
   * @param b value to compare.
   * @throws ClassCastException if the parameters are not <i>mutually comparable</i> under this
   *     ordering.
   */
  @ParametricNullness
  public <E extends T> E max(@ParametricNullness E a, @ParametricNullness E b) {
    return (compare(a, b) >= 0) ? a : b;
  }

  /**
   * Returns the greatest of the specified values according to this ordering. If there are multiple
   * greatest values, the first of those is returned.
   *
   * <p><b>Java 8 users:</b> Use {@code Collections.max(Arrays.asList(a, b, c...), thisComparator)}
   * instead (but note that it does not guarantee which tied maximum element is returned).
   *
   * @param a value to compare, returned if greater than or equal to the rest.
   * @param b value to compare
   * @param c value to compare
   * @param rest values to compare
   * @throws ClassCastException if the parameters are not <i>mutually comparable</i> under this
   *     ordering.
   */
  @ParametricNullness
  public <E extends T> E max(
      @ParametricNullness E a, @ParametricNullness E b, @ParametricNullness E c, E... rest) {
    E maxSoFar = max(max(a, b), c);

    for (E r : rest) {
      maxSoFar = max(maxSoFar, r);
    }

    return maxSoFar;
  }

  /**
   * Returns the {@code k} least elements of the given iterable according to this ordering, in order
   * from least to greatest. If there are fewer than {@code k} elements present, all will be
   * included.
   *
   * <p>The implementation does not necessarily use a <i>stable</i> sorting algorithm; when multiple
   * elements are equivalent, it is undefined which will come first.
   *
   * <p><b>Java 8 users:</b> Use {@code Streams.stream(iterable).collect(Comparators.least(k,
   * thisComparator))} instead.
   *
   * @return an immutable {@code RandomAccess} list of the {@code k} least elements in ascending
   *     order
   * @throws IllegalArgumentException if {@code k} is negative
   * @since 8.0
   */
  public <E extends T> List<E> leastOf(Iterable<E> iterable, int k) {
    if (iterable instanceof Collection) {
      Collection<E> collection = (Collection<E>) iterable;
      if (collection.size() <= 2L * k) {
        // In this case, just dumping the collection to an array and sorting is
        // faster than using the implementation for Iterator, which is
        // specialized for k much smaller than n.

        @SuppressWarnings("unchecked") // c only contains E's and doesn't escape
        E[] array = (E[]) collection.toArray();
        Arrays.sort(array, this);
        if (array.length > k) {
          array = Arrays.copyOf(array, k);
        }
        return Collections.unmodifiableList(Arrays.asList(array));
      }
    }
    return leastOf(iterable.iterator(), k);
  }

  /**
   * Returns the {@code k} least elements from the given iterator according to this ordering, in
   * order from least to greatest. If there are fewer than {@code k} elements present, all will be
   * included.
   *
   * <p>The implementation does not necessarily use a <i>stable</i> sorting algorithm; when multiple
   * elements are equivalent, it is undefined which will come first.
   *
   * <p><b>Java 8 users:</b> Use {@code Streams.stream(iterator).collect(Comparators.least(k,
   * thisComparator))} instead.
   *
   * @return an immutable {@code RandomAccess} list of the {@code k} least elements in ascending
   *     order
   * @throws IllegalArgumentException if {@code k} is negative
   * @since 14.0
   */
  public <E extends T> List<E> leastOf(Iterator<E> iterator, int k) {
    checkNotNull(iterator);
    checkNonnegative(k, "k");

    if (k == 0 || !iterator.hasNext()) {
      return Collections.emptyList();
    } else if (k >= Integer.MAX_VALUE / 2) {
      // k is really large; just do a straightforward sorted-copy-and-sublist
      ArrayList<E> list = Lists.newArrayList(iterator);
      Collections.sort(list, this);
      if (list.size() > k) {
        list.subList(k, list.size()).clear();
      }
      list.trimToSize();
      return Collections.unmodifiableList(list);
    } else {
      TopKSelector<E> selector = TopKSelector.least(k, this);
      selector.offerAll(iterator);
      return selector.topK();
    }
  }

  /**
   * Returns the {@code k} greatest elements of the given iterable according to this ordering, in
   * order from greatest to least. If there are fewer than {@code k} elements present, all will be
   * included.
   *
   * <p>The implementation does not necessarily use a <i>stable</i> sorting algorithm; when multiple
   * elements are equivalent, it is undefined which will come first.
   *
   * <p><b>Java 8 users:</b> Use {@code Streams.stream(iterable).collect(Comparators.greatest(k,
   * thisComparator))} instead.
   *
   * @return an immutable {@code RandomAccess} list of the {@code k} greatest elements in
   *     <i>descending order</i>
   * @throws IllegalArgumentException if {@code k} is negative
   * @since 8.0
   */
  public <E extends T> List<E> greatestOf(Iterable<E> iterable, int k) {
    // TODO(kevinb): see if delegation is hurting performance noticeably
    // TODO(kevinb): if we change this implementation, add full unit tests.
    return this.<E>reverse().leastOf(iterable, k);
  }

  /**
   * Returns the {@code k} greatest elements from the given iterator according to this ordering, in
   * order from greatest to least. If there are fewer than {@code k} elements present, all will be
   * included.
   *
   * <p>The implementation does not necessarily use a <i>stable</i> sorting algorithm; when multiple
   * elements are equivalent, it is undefined which will come first.
   *
   * <p><b>Java 8 users:</b> Use {@code Streams.stream(iterator).collect(Comparators.greatest(k,
   * thisComparator))} instead.
   *
   * @return an immutable {@code RandomAccess} list of the {@code k} greatest elements in
   *     <i>descending order</i>
   * @throws IllegalArgumentException if {@code k} is negative
   * @since 14.0
   */
  public <E extends T> List<E> greatestOf(Iterator<E> iterator, int k) {
    return this.<E>reverse().leastOf(iterator, k);
  }

  /**
   * Returns a <b>mutable</b> list containing {@code elements} sorted by this ordering; use this
   * only when the resulting list may need further modification, or may contain {@code null}. The
   * input is not modified. The returned list is serializable and has random access.
   *
   * <p>Unlike {@link Sets#newTreeSet(Iterable)}, this method does not discard elements that are
   * duplicates according to the comparator. The sort performed is <i>stable</i>, meaning that such
   * elements will appear in the returned list in the same order they appeared in {@code elements}.
   *
   * <p><b>Performance note:</b> According to our
   * benchmarking
   * on Open JDK 7, {@link #immutableSortedCopy} generally performs better (in both time and space)
   * than this method, and this method in turn generally performs better than copying the list and
   * calling {@link Collections#sort(List)}.
   */
  // TODO(kevinb): rerun benchmarks including new options
  public <E extends T> List<E> sortedCopy(Iterable<E> elements) {
    @SuppressWarnings("unchecked") // does not escape, and contains only E's
    E[] array = (E[]) Iterables.toArray(elements);
    Arrays.sort(array, this);
    return Lists.newArrayList(Arrays.asList(array));
  }

  /**
   * Returns an <b>immutable</b> list containing {@code elements} sorted by this ordering. The input
   * is not modified.
   *
   * <p>Unlike {@link Sets#newTreeSet(Iterable)}, this method does not discard elements that are
   * duplicates according to the comparator. The sort performed is <i>stable</i>, meaning that such
   * elements will appear in the returned list in the same order they appeared in {@code elements}.
   *
   * <p><b>Performance note:</b> According to our
   * benchmarking
   * on Open JDK 7, this method is the most efficient way to make a sorted copy of a collection.
   *
   * @throws NullPointerException if any element of {@code elements} is {@code null}
   * @since 3.0
   */
  // TODO(kevinb): rerun benchmarks including new options
  public <E extends @NonNull T> ImmutableList<E> immutableSortedCopy(Iterable<E> elements) {
    return ImmutableList.sortedCopyOf(this, elements);
  }

  /**
   * Returns {@code true} if each element in {@code iterable} after the first is greater than or
   * equal to the element that preceded it, according to this ordering. Note that this is always
   * true when the iterable has fewer than two elements.
   *
   * <p><b>Java 8 users:</b> Use the equivalent {@link Comparators#isInOrder(Iterable, Comparator)}
   * instead, since the rest of {@code Ordering} is mostly obsolete (as explained in the class
   * documentation).
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
   * Returns {@code true} if each element in {@code iterable} after the first is <i>strictly</i>
   * greater than the element that preceded it, according to this ordering. Note that this is always
   * true when the iterable has fewer than two elements.
   *
   * <p><b>Java 8 users:</b> Use the equivalent {@link Comparators#isInStrictOrder(Iterable,
   * Comparator)} instead, since the rest of {@code Ordering} is mostly obsolete (as explained in
   * the class documentation).
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
   * {@link Collections#binarySearch(List, Object, Comparator) Searches} {@code sortedList} for
   * {@code key} using the binary search algorithm. The list must be sorted using this ordering.
   *
   * @param sortedList the list to be searched
   * @param key the key to be searched for
   * @deprecated Use {@link Collections#binarySearch(List, Object, Comparator)} directly.
   */
  @Deprecated
  public int binarySearch(
      List<? extends T> sortedList, @ParametricNullness T key) {
    return Collections.binarySearch(sortedList, key, this);
  }

  /**
   * Exception thrown by a {@link Ordering#explicit(List)} or {@link Ordering#explicit(Object,
   * Object[])} comparator when comparing a value outside the set of values it can compare.
   * Extending {@link ClassCastException} may seem odd, but it is required.
   */
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
