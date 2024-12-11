/*
 * Copyright (C) 2016 The Guava Authors
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
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collector;
import javax.annotation.CheckForNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Provides static methods for working with {@link Comparator} instances. For many other helpful
 * comparator utilities, see either {@code Comparator} itself (for Java 8+), or {@code
 * com.google.common.collect.Ordering} (otherwise).
 *
 * <h3>Relationship to {@code Ordering}</h3>
 *
 * <p>In light of the significant enhancements to {@code Comparator} in Java 8, the overwhelming
 * majority of usages of {@code Ordering} can be written using only built-in JDK APIs. This class is
 * intended to "fill the gap" and provide those features of {@code Ordering} not already provided by
 * the JDK.
 *
 * @since 21.0
 * @author Louis Wasserman
 */
@GwtCompatible
@ElementTypesAreNonnullByDefault
public final class Comparators {
  private Comparators() {}

  /**
   * Returns a new comparator which sorts iterables by comparing corresponding elements pairwise
   * until a nonzero result is found; imposes "dictionary order." If the end of one iterable is
   * reached, but not the other, the shorter iterable is considered to be less than the longer one.
   * For example, a lexicographical natural ordering over integers considers {@code [] < [1] < [1,
   * 1] < [1, 2] < [2]}.
   *
   * <p>Note that {@code Collections.reverseOrder(lexicographical(comparator))} is not equivalent to
   * {@code lexicographical(Collections.reverseOrder(comparator))} (consider how each would order
   * {@code [1]} and {@code [1, 1]}).
   */
  // Note: 90% of the time we don't add type parameters or wildcards that serve only to "tweak" the
  // desired return type. However, *nested* generics introduce a special class of problems that we
  // think tip it over into being worthwhile.
  public static <T extends @Nullable Object, S extends T> Comparator<Iterable<S>> lexicographical(
      Comparator<T> comparator) {
    return new LexicographicalOrdering<S>(checkNotNull(comparator));
  }

  /**
   * Returns {@code true} if each element in {@code iterable} after the first is greater than or
   * equal to the element that preceded it, according to the specified comparator. Note that this is
   * always true when the iterable has fewer than two elements.
   */
  public static <T extends @Nullable Object> boolean isInOrder(
      Iterable<? extends T> iterable, Comparator<T> comparator) {
    checkNotNull(comparator);
    Iterator<? extends T> it = iterable.iterator();
    if (it.hasNext()) {
      T prev = it.next();
      while (it.hasNext()) {
        T next = it.next();
        if (comparator.compare(prev, next) > 0) {
          return false;
        }
        prev = next;
      }
    }
    return true;
  }

  /**
   * Returns {@code true} if each element in {@code iterable} after the first is <i>strictly</i>
   * greater than the element that preceded it, according to the specified comparator. Note that
   * this is always true when the iterable has fewer than two elements.
   */
  public static <T extends @Nullable Object> boolean isInStrictOrder(
      Iterable<? extends T> iterable, Comparator<T> comparator) {
    checkNotNull(comparator);
    Iterator<? extends T> it = iterable.iterator();
    if (it.hasNext()) {
      T prev = it.next();
      while (it.hasNext()) {
        T next = it.next();
        if (comparator.compare(prev, next) >= 0) {
          return false;
        }
        prev = next;
      }
    }
    return true;
  }

  /**
   * Returns a {@code Collector} that returns the {@code k} smallest (relative to the specified
   * {@code Comparator}) input elements, in ascending order, as an unmodifiable {@code List}. Ties
   * are broken arbitrarily.
   *
   * <p>For example:
   *
   * <pre>{@code
   * Stream.of("foo", "quux", "banana", "elephant")
   *     .collect(least(2, comparingInt(String::length)))
   * // returns {"foo", "quux"}
   * }</pre>
   *
   * <p>This {@code Collector} uses O(k) memory and takes expected time O(n) (worst-case O(n log
   * k)), as opposed to e.g. {@code Stream.sorted(comparator).limit(k)}, which currently takes O(n
   * log n) time and O(n) space.
   *
   * @throws IllegalArgumentException if {@code k < 0}
   * @since 33.2.0 (available since 22.0 in guava-jre)
   */
  @SuppressWarnings("Java7ApiChecker")
  @IgnoreJRERequirement // Users will use this only if they're already using streams.
  public static <T extends @Nullable Object> Collector<T, ?, List<T>> least(
      int k, Comparator<? super T> comparator) {
    checkNonnegative(k, "k");
    checkNotNull(comparator);
    return Collector.of(
        () -> TopKSelector.<T>least(k, comparator),
        TopKSelector::offer,
        TopKSelector::combine,
        TopKSelector::topK,
        Collector.Characteristics.UNORDERED);
  }

  /**
   * Returns a {@code Collector} that returns the {@code k} greatest (relative to the specified
   * {@code Comparator}) input elements, in descending order, as an unmodifiable {@code List}. Ties
   * are broken arbitrarily.
   *
   * <p>For example:
   *
   * <pre>{@code
   * Stream.of("foo", "quux", "banana", "elephant")
   *     .collect(greatest(2, comparingInt(String::length)))
   * // returns {"elephant", "banana"}
   * }</pre>
   *
   * <p>This {@code Collector} uses O(k) memory and takes expected time O(n) (worst-case O(n log
   * k)), as opposed to e.g. {@code Stream.sorted(comparator.reversed()).limit(k)}, which currently
   * takes O(n log n) time and O(n) space.
   *
   * @throws IllegalArgumentException if {@code k < 0}
   * @since 33.2.0 (available since 22.0 in guava-jre)
   */
  @SuppressWarnings("Java7ApiChecker")
  @IgnoreJRERequirement // Users will use this only if they're already using streams.
  public static <T extends @Nullable Object> Collector<T, ?, List<T>> greatest(
      int k, Comparator<? super T> comparator) {
    return least(k, comparator.reversed());
  }

  /**
   * Returns a comparator of {@link Optional} values which treats {@link Optional#empty} as less
   * than all other values, and orders the rest using {@code valueComparator} on the contained
   * value.
   *
   * @since NEXT (but since 22.0 in the JRE flavor)
   */
  @SuppressWarnings("Java7ApiChecker")
  @IgnoreJRERequirement // Users will use this only if they're already using Optional.
  public static <T> Comparator<Optional<T>> emptiesFirst(Comparator<? super T> valueComparator) {
    checkNotNull(valueComparator);
    return Comparator.<Optional<T>, @Nullable T>comparing(
        o -> orElseNull(o), Comparator.nullsFirst(valueComparator));
  }

  /**
   * Returns a comparator of {@link Optional} values which treats {@link Optional#empty} as greater
   * than all other values, and orders the rest using {@code valueComparator} on the contained
   * value.
   *
   * @since NEXT (but since 22.0 in the JRE flavor)
   */
  @SuppressWarnings("Java7ApiChecker")
  @IgnoreJRERequirement // Users will use this only if they're already using Optional.
  public static <T> Comparator<Optional<T>> emptiesLast(Comparator<? super T> valueComparator) {
    checkNotNull(valueComparator);
    return Comparator.<Optional<T>, @Nullable T>comparing(
        o -> orElseNull(o), Comparator.nullsLast(valueComparator));
  }

  @SuppressWarnings("Java7ApiChecker")
  @IgnoreJRERequirement // helper for emptiesFirst+emptiesLast
  /*
   * If we make these calls inline inside the lambda inside emptiesFirst()/emptiesLast(), we get an
   * Animal Sniffer error, despite the @IgnoreJRERequirement annotation there. For details, see
   * ImmutableSortedMultiset.
   */
  @CheckForNull
  private static <T> T orElseNull(Optional<T> optional) {
    return optional.orElse(null);
  }

  /**
   * Returns the minimum of the two values. If the values compare as 0, the first is returned.
   *
   * <p>The recommended solution for finding the {@code minimum} of some values depends on the type
   * of your data and the number of elements you have. Read more in the Guava User Guide article on
   * <a href="https://github.com/google/guava/wiki/CollectionUtilitiesExplained#comparators">{@code
   * Comparators}</a>.
   *
   * @param a first value to compare, returned if less than or equal to b.
   * @param b second value to compare.
   * @throws ClassCastException if the parameters are not <i>mutually comparable</i>.
   * @since 30.0
   */
  public static <T extends Comparable<? super T>> T min(T a, T b) {
    return (a.compareTo(b) <= 0) ? a : b;
  }

  /**
   * Returns the minimum of the two values, according to the given comparator. If the values compare
   * as equal, the first is returned.
   *
   * <p>The recommended solution for finding the {@code minimum} of some values depends on the type
   * of your data and the number of elements you have. Read more in the Guava User Guide article on
   * <a href="https://github.com/google/guava/wiki/CollectionUtilitiesExplained#comparators">{@code
   * Comparators}</a>.
   *
   * @param a first value to compare, returned if less than or equal to b
   * @param b second value to compare.
   * @throws ClassCastException if the parameters are not <i>mutually comparable</i> using the given
   *     comparator.
   * @since 30.0
   */
  @ParametricNullness
  public static <T extends @Nullable Object> T min(
      @ParametricNullness T a, @ParametricNullness T b, Comparator<? super T> comparator) {
    return (comparator.compare(a, b) <= 0) ? a : b;
  }

  /**
   * Returns the maximum of the two values. If the values compare as 0, the first is returned.
   *
   * <p>The recommended solution for finding the {@code maximum} of some values depends on the type
   * of your data and the number of elements you have. Read more in the Guava User Guide article on
   * <a href="https://github.com/google/guava/wiki/CollectionUtilitiesExplained#comparators">{@code
   * Comparators}</a>.
   *
   * @param a first value to compare, returned if greater than or equal to b.
   * @param b second value to compare.
   * @throws ClassCastException if the parameters are not <i>mutually comparable</i>.
   * @since 30.0
   */
  public static <T extends Comparable<? super T>> T max(T a, T b) {
    return (a.compareTo(b) >= 0) ? a : b;
  }

  /**
   * Returns the maximum of the two values, according to the given comparator. If the values compare
   * as equal, the first is returned.
   *
   * <p>The recommended solution for finding the {@code maximum} of some values depends on the type
   * of your data and the number of elements you have. Read more in the Guava User Guide article on
   * <a href="https://github.com/google/guava/wiki/CollectionUtilitiesExplained#comparators">{@code
   * Comparators}</a>.
   *
   * @param a first value to compare, returned if greater than or equal to b.
   * @param b second value to compare.
   * @throws ClassCastException if the parameters are not <i>mutually comparable</i> using the given
   *     comparator.
   * @since 30.0
   */
  @ParametricNullness
  public static <T extends @Nullable Object> T max(
      @ParametricNullness T a, @ParametricNullness T b, Comparator<? super T> comparator) {
    return (comparator.compare(a, b) >= 0) ? a : b;
  }
}
