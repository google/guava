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

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Ranges.create;

import com.google.common.annotations.Beta;
import com.google.common.annotations.GwtCompatible;
import com.google.common.base.Predicate;

import java.io.Serializable;
import java.util.Collections;
import java.util.Comparator;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.SortedSet;

import javax.annotation.Nullable;

/**
 * A range, sometimes known as an <i>interval</i>, is a <i>convex</i>
 * (informally, "contiguous" or "unbroken") portion of a particular domain.
 * Formally, convexity means that for any {@code a <= b <= c},
 * {@code range.contains(a) && range.contains(c)} implies that {@code
 * range.contains(b)}.
 *
 * <p>A range is characterized by its lower and upper <i>bounds</i> (extremes),
 * each of which can <i>open</i> (exclusive of its endpoint), <i>closed</i>
 * (inclusive of its endpoint), or <i>unbounded</i>. This yields nine basic
 * types of ranges:
 *
 * <ul>
 * <li>{@code (a..b) = {x | a < x < b}}
 * <li>{@code [a..b] = {x | a <= x <= b}}
 * <li>{@code [a..b) = {x | a <= x < b}}
 * <li>{@code (a..b] = {x | a < x <= b}}
 * <li>{@code (a..+∞) = {x | x > a}}
 * <li>{@code [a..+∞) = {x | x >= a}}
 * <li>{@code (-∞..b) = {x | x < b}}
 * <li>{@code (-∞..b] = {x | x <= b}}
 * <li>{@code (-∞..+∞) = all values}
 * </ul>
 *
 * (The notation {@code {x | statement}} is read "the set of all <i>x</i> such
 * that <i>statement</i>.")
 *
 * <p>Notice that we use a square bracket ({@code [ ]}) to denote that an range
 * is closed on that end, and a parenthesis ({@code ( )}) when it is open or
 * unbounded.
 *
 * <p>The values {@code a} and {@code b} used above are called <i>endpoints</i>.
 * The upper endpoint may not be less than the lower endpoint. The endpoints may
 * be equal only if at least one of the bounds is closed:
 *
 * <ul>
 * <li>{@code [a..a]} : singleton range
 * <li>{@code [a..a); (a..a]} : {@linkplain #isEmpty empty}, but valid
 * <li>{@code (a..a)} : <b>invalid</b>
 * </ul>
 *
 * <p>Instances of this type can be obtained using the static factory methods in
 * the {@link Ranges} class.
 *
 * <p>Instances of {@code Range} are immutable. It is strongly encouraged to
 * use this class only with immutable data types. When creating a range over a
 * mutable type, take great care not to allow the value objects to mutate after
 * the range is created.
 *
 * <p>In this and other range-related specifications, concepts like "equal",
 * "same", "unique" and so on are based on {@link Comparable#compareTo}
 * returning zero, not on {@link Object#equals} returning {@code true}. Of
 * course, when these methods are kept <i>consistent</i> (as defined in {@link
 * Comparable}), this is not an issue.
 *
 * <p>A range {@code a} is said to be the <i>maximal</i> range having property
 * <i>P</i> if, for all ranges {@code b} also having property <i>P</i>, {@code
 * a.encloses(b)}. Likewise, {@code a} is <i>minimal</i> when {@code
 * b.encloses(a)} for all {@code b} having property <i>P</i>. See, for example,
 * the definition of {@link #intersection}.
 *
 * <p>This class can be used with any type which implements {@code Comparable};
 * it does not require {@code Comparable<? super C>} because this would be
 * incompatible with pre-Java 5 types. If this class is used with a perverse
 * {@code Comparable} type ({@code Foo implements Comparable<Bar>} where {@code
 * Bar} is not a supertype of {@code Foo}), any of its methods may throw {@link
 * ClassCastException}. (There is no good reason for such a type to exist.)
 *
 * <p>When evaluated as a {@link Predicate}, a range yields the same result as
 * invoking {@link #contains}.
 *
 * @author Kevin Bourrillion
 * @author Gregory Kick
 * @since 10.0
 */
@GwtCompatible
@Beta
public final class Range<C extends Comparable>
    implements Predicate<C>, Serializable {
  final Cut<C> lowerBound;
  final Cut<C> upperBound;

  Range(Cut<C> lowerBound, Cut<C> upperBound) {
    if (lowerBound.compareTo(upperBound) > 0) {
      throw new IllegalArgumentException(
          "Invalid range: " + toString(lowerBound, upperBound));
    }
    this.lowerBound = lowerBound;
    this.upperBound = upperBound;
  }

  /**
   * Returns {@code true} if this range has a lower endpoint.
   */
  public boolean hasLowerBound() {
    return lowerBound != Cut.belowAll();
  }

  /**
   * Returns the lower endpoint of this range.
   *
   * @throws IllegalStateException if this range is unbounded below (that is,
   *     {@link #hasLowerBound()} returns {@code false})
   */
  public C lowerEndpoint() {
    return lowerBound.endpoint();
  }

  /**
   * Returns the type of this range's lower bound: {@link BoundType#CLOSED} if
   * the range includes its lower endpoint, {@link BoundType#OPEN} if it does
   * not.
   *
   * @throws IllegalStateException if this range is unbounded below (that is,
   *     {@link #hasLowerBound()} returns {@code false})
   */
  public BoundType lowerBoundType() {
    return lowerBound.typeAsLowerBound();
  }

  /**
   * Returns {@code true} if this range has an upper endpoint.
   */
  public boolean hasUpperBound() {
    return upperBound != Cut.aboveAll();
  }

  /**
   * Returns the upper endpoint of this range.
   *
   * @throws IllegalStateException if this range is unbounded above (that is,
   *     {@link #hasUpperBound()} returns {@code false})
   */
  public C upperEndpoint() {
    return upperBound.endpoint();
  }

  /**
   * Returns the type of this range's upper bound: {@link BoundType#CLOSED} if
   * the range includes its upper endpoint, {@link BoundType#OPEN} if it does
   * not.
   *
   * @throws IllegalStateException if this range is unbounded above (that is,
   *     {@link #hasUpperBound()} returns {@code false})
   */
  public BoundType upperBoundType() {
    return upperBound.typeAsUpperBound();
  }

  /**
   * Returns {@code true} if this range is of the form {@code [v..v)} or {@code
   * (v..v]}. (This does not encompass ranges of the form {@code (v..v)},
   * because such ranges are <i>invalid</i> and can't be constructed at all.)
   *
   * <p>Note that certain discrete ranges such as the integer range {@code
   * (3..4)} are <b>not</b> considered empty, even though they contain no actual
   * values.
   */
  public boolean isEmpty() {
    return lowerBound.equals(upperBound);
  }

  /**
   * Returns {@code true} if {@code value} is within the bounds of this
   * range. For example, on the range {@code [0..2)}, {@code contains(1)}
   * returns {@code true}, while {@code contains(2)} returns {@code false}.
   */
  public boolean contains(C value) {
    checkNotNull(value);
    // let this throw CCE if there is some trickery going on
    return lowerBound.isLessThan(value) && !upperBound.isLessThan(value);
  }

  /**
   * Equivalent to {@link #contains}; provided only to satisfy the {@link
   * Predicate} interface. When using a reference of type {@code Range}, always
   * invoke {@link #contains} directly instead.
   */
  @Override public boolean apply(C input) {
    return contains(input);
  }

  /**
   * Returns {@code true} if every element in {@code values} is {@linkplain
   * #contains contained} in this range.
   */
  public boolean containsAll(Iterable<? extends C> values) {
    if (Iterables.isEmpty(values)) {
      return true;
    }

    // this optimizes testing equality of two range-backed sets
    if (values instanceof SortedSet) {
      SortedSet<? extends C> set = cast(values);
      Comparator<?> comparator = set.comparator();
      if (Ordering.natural().equals(comparator) || comparator == null) {
        return contains(set.first()) && contains(set.last());
      }
    }

    for (C value : values) {
      if (!contains(value)) {
        return false;
      }
    }
    return true;
  }

  /**
   * Returns {@code true} if the bounds of {@code other} do not extend outside
   * the bounds of this range. Examples:
   *
   * <ul>
   * <li>{@code [3..6]} encloses {@code [4..5]}
   * <li>{@code (3..6)} encloses {@code (3..6)}
   * <li>{@code [3..6]} encloses {@code [4..4)} (even though the latter is
   *     empty)
   * <li>{@code (3..6]} does not enclose {@code [3..6]}
   * <li>{@code [4..5]} does not enclose {@code (3..6)} (even though it contains
   *     every value contained by the latter range)
   * <li>{@code [3..6]} does not enclose {@code (1..1]} (even though it contains
   *     every value contained by the latter range)
   * </ul>
   *
   * Note that if {@code a.encloses(b)}, then {@code b.contains(v)} implies
   * {@code a.contains(v)}, but as the last two examples illustrate, the
   * converse is not always true.
   *
   * <p>The encloses relation has the following properties:
   *
   * <ul>
   * <li>reflexive: {@code a.encloses(a)} is always true
   * <li>antisymmetric: {@code a.encloses(b) && b.encloses(a)} implies {@code
   *     a.equals(b)}
   * <li>transitive: {@code a.encloses(b) && b.encloses(c)} implies {@code
   *     a.encloses(c)}
   * <li>not a total ordering: {@code !a.encloses(b)} does not imply {@code
   *     b.encloses(a)}
   * <li>there exists a {@linkplain Ranges#all maximal} range, for which
   *     {@code encloses} is always true
   * <li>there also exist {@linkplain #isEmpty minimal} ranges, for
   *     which {@code encloses(b)} is always false when {@code !equals(b)}
   * <li>if {@code a.encloses(b)}, then {@link #isConnected a.isConnected(b)}
   *     is {@code true}. 
   * </ul>
   */
  public boolean encloses(Range<C> other) {
    return lowerBound.compareTo(other.lowerBound) <= 0
        && upperBound.compareTo(other.upperBound) >= 0;
  }

  /**
   * Returns the maximal range {@linkplain #encloses enclosed} by both this
   * range and {@code other}, if such a range exists.
   * 
   * <p>For example, the intersection of {@code [1..5]} and {@code (3..7)} is
   * {@code (3..5]}. The resulting range may be empty; for example, 
   * {@code [1..5)} intersected with {@code [5..7)} yields the empty range
   * {@code [5..5)}.
   * 
   * <p>Generally, the intersection exists if and only if this range and 
   * {@code other} are {@linkplain #isConnected connected}.
   *
   * <p>The intersection operation has the following properties:
   *
   * <ul>
   * <li>commutative: {@code a.intersection(b)} produces the same result as
   *     {@code b.intersection(a)}
   * <li>associative: {@code a.intersection(b).intersection(c)} produces the
   *     same result as {@code a.intersection(b.intersection(c))}
   * <li>idempotent: {@code a.intersection(a)} equals {@code a}
   * <li>identity ({@link Ranges#all}): {@code a.intersection(Ranges.all())}
   *     equals {@code a}
   * </ul>
   *
   * @throws IllegalArgumentException if no range exists that is enclosed by
   *     both these ranges
   */
  public Range<C> intersection(Range<C> other) {
    Cut<C> newLower = Ordering.natural().max(lowerBound, other.lowerBound);
    Cut<C> newUpper = Ordering.natural().min(upperBound, other.upperBound);
    return create(newLower, newUpper);
  }

  /**
   * Returns {@code true} if there exists a (possibly empty) range which is
   * {@linkplain #encloses enclosed} by both this range and {@code other}.
   * 
   * <p>For example,
   * <ul>
   * <li>{@code [2, 4)} and {@code [5, 7)} are not connected
   * <li>{@code [2, 4)} and {@code [3, 5)} are connected, because both enclose
   *     {@code [3, 4)}
   * <li>{@code [2, 4)} and {@code [4, 6)} are connected, because both enclose
   *     the empty range {@code [4, 4)}
   * </ul>
   * 
   * <p>Note that this range and {@code other} have a well-defined {@linkplain
   * #span union} and {@linkplain #intersection intersection} (as a single,
   * possibly-empty range) if and only if this method returns {@code true}.
   * 
   * <p>The connectedness relation has the following properties:
   *
   * <ul>
   * <li>symmetric: {@code a.isConnected(b)} produces the same result as
   *     {@code b.isConnected(a)}
   * <li>reflexive: {@code a.isConnected(a)} returns {@code true}
   * </ul>
   */
  public boolean isConnected(Range<C> other) {
    return lowerBound.compareTo(other.upperBound) <= 0
        && other.lowerBound.compareTo(upperBound) <= 0;
  }

  /**
   * Returns the minimal range that {@linkplain #encloses encloses} both this
   * range and {@code other}. For example, the span of {@code [1..3]} and
   * {@code (5..7)} is {@code [1..7)}. Note that the span may contain values
   * that are not contained by either original range.
   *
   * <p>The span operation has the following properties:
   *
   * <ul>
   * <li>closed: the range {@code a.span(b)} exists for all ranges {@code a} and
   *     {@code b}
   * <li>commutative: {@code a.span(b)} equals {@code b.span(a)}
   * <li>associative: {@code a.span(b).span(c)} equals {@code a.span(b.span(c))}
   * <li>idempotent: {@code a.span(a)} equals {@code a}
   * </ul>
   * 
   * <p>Note that the returned range is also called the <i>union</i> of this
   * range and {@code other} if and only if the ranges are 
   * {@linkplain #isConnected connected}.
   */
  public Range<C> span(Range<C> other) {
    Cut<C> newLower = Ordering.natural().min(lowerBound, other.lowerBound);
    Cut<C> newUpper = Ordering.natural().max(upperBound, other.upperBound);
    return create(newLower, newUpper);
  }

  /**
   * Returns an {@link ImmutableSortedSet} containing the same values in the
   * given domain {@linkplain Range#contains contained} by this range.
   *
   * <p><b>Note:</b> {@code a.asSet().equals(b.asSet())} does not imply {@code
   * a.equals(b)}! For example, {@code a} and {@code b} could be {@code [2..4]}
   * and {@code (1..5)}, or the empty ranges {@code [3..3)} and {@code [4..4)}.
   *
   * <p><b>Warning:</b> Be extremely careful what you do with the {@code asSet}
   * view of a large range (such as {@code Ranges.greaterThan(0)}). Certain
   * operations on such a set can be performed efficiently, but others (such as
   * {@link Set#hashCode} or {@link Collections#frequency}) can cause major
   * performance problems.
   *
   * <p>The returned set's {@link Object#toString} method returns a short-hand
   * form of set's contents such as {@code "[1..100]}"}.
   *
   * @throws IllegalArgumentException if neither this range nor the domain has a
   *     lower bound, or if neither has an upper bound
   */
  // TODO(kevinb): commit in spec to which methods are efficient?
  @GwtCompatible(serializable = false)
  public ContiguousSet<C> asSet(DiscreteDomain<C> domain) {
    checkNotNull(domain);
    Range<C> effectiveRange = this;
    try {
      if (!hasLowerBound()) {
        effectiveRange = effectiveRange.intersection(
            Ranges.atLeast(domain.minValue()));
      }
      if (!hasUpperBound()) {
        effectiveRange = effectiveRange.intersection(
            Ranges.atMost(domain.maxValue()));
      }
    } catch (NoSuchElementException e) {
      throw new IllegalArgumentException(e);
    }

    // Per class spec, we are allowed to throw CCE if necessary
    boolean empty = effectiveRange.isEmpty()
        || compareOrThrow(
            lowerBound.leastValueAbove(domain),
            upperBound.greatestValueBelow(domain)) > 0;

    return empty
        ? new EmptyContiguousSet<C>(domain)
        : new RegularContiguousSet<C>(effectiveRange, domain);
  }

  /**
   * Returns the canonical form of this range in the given domain. The canonical
   * form has the following properties:
   *
   * <ul>
   * <li>equivalence: {@code a.canonical().contains(v) == a.contains(v)} for
   *     all {@code v} (in other words, {@code
   *     a.canonical(domain).asSet(domain).equals(a.asSet(domain))}
   * <li>uniqueness: unless {@code a.isEmpty()},
   *     {@code a.asSet(domain).equals(b.asSet(domain))} implies
   *     {@code a.canonical(domain).equals(b.canonical(domain))}
   * <li>idempotence: {@code
   *     a.canonical(domain).canonical(domain).equals(a.canonical(domain))}
   * </ul>
   *
   * Furthermore, this method guarantees that the range returned will be one
   * of the following canonical forms:
   *
   * <ul>
   * <li>[start..end)
   * <li>[start..+∞)
   * <li>(-∞..end) (only if type {@code C} is unbounded below)
   * <li>(-∞..+∞) (only if type {@code C} is unbounded below)
   * </ul>
   */
  public Range<C> canonical(DiscreteDomain<C> domain) {
    checkNotNull(domain);
    Cut<C> lower = lowerBound.canonical(domain);
    Cut<C> upper = upperBound.canonical(domain);
    return (lower == lowerBound && upper == upperBound)
        ? this : create(lower, upper);
  }

  /**
   * Returns {@code true} if {@code object} is a range having the same
   * endpoints and bound types as this range. Note that discrete ranges
   * such as {@code (1..4)} and {@code [2..3]} are <b>not</b> equal to one
   * another, despite the fact that they each contain precisely the same set of
   * values. Similarly, empty ranges are not equal unless they have exactly
   * the same representation, so {@code [3..3)}, {@code (3..3]}, {@code (4..4]}
   * are all unequal.
   */
  @Override public boolean equals(@Nullable Object object) {
    if (object instanceof Range) {
      Range<?> other = (Range<?>) object;
      return lowerBound.equals(other.lowerBound)
          && upperBound.equals(other.upperBound);
    }
    return false;
  }

  /** Returns a hash code for this range. */
  @Override public int hashCode() {
    return lowerBound.hashCode() * 31 + upperBound.hashCode();
  }

  /**
   * Returns a string representation of this range, such as {@code "[3..5)"}
   * (other examples are listed in the class documentation).
   */
  @Override public String toString() {
    return toString(lowerBound, upperBound);
  }

  private static String toString(Cut<?> lowerBound, Cut<?> upperBound) {
    StringBuilder sb = new StringBuilder(16);
    lowerBound.describeAsLowerBound(sb);
    sb.append('\u2025');
    upperBound.describeAsUpperBound(sb);
    return sb.toString();
  }

  /**
   * Used to avoid http://bugs.sun.com/view_bug.do?bug_id=6558557
   */
  private static <T> SortedSet<T> cast(Iterable<T> iterable) {
    return (SortedSet<T>) iterable;
  }

  @SuppressWarnings("unchecked") // this method may throw CCE
  static int compareOrThrow(Comparable left, Comparable right) {
    return left.compareTo(right);
  }

  private static final long serialVersionUID = 0;
}
