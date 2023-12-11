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

import com.google.common.annotations.GwtCompatible;
import com.google.common.base.Equivalence;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.errorprone.annotations.Immutable;
import java.io.Serializable;
import java.util.Comparator;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.SortedSet;
import javax.annotation.CheckForNull;

/**
 * A range (or "interval") defines the <i>boundaries</i> around a contiguous span of values of some
 * {@code Comparable} type; for example, "integers from 1 to 100 inclusive." Note that it is not
 * possible to <i>iterate</i> over these contained values. To do so, pass this range instance and an
 * appropriate {@link DiscreteDomain} to {@link ContiguousSet#create}.
 *
 * <h3>Types of ranges</h3>
 *
 * <p>Each end of the range may be bounded or unbounded. If bounded, there is an associated
 * <i>endpoint</i> value, and the range is considered to be either <i>open</i> (does not include the
 * endpoint) or <i>closed</i> (includes the endpoint) on that side. With three possibilities on each
 * side, this yields nine basic types of ranges, enumerated below. (Notation: a square bracket
 * ({@code [ ]}) indicates that the range is closed on that side; a parenthesis ({@code ( )}) means
 * it is either open or unbounded. The construct {@code {x | statement}} is read "the set of all
 * <i>x</i> such that <i>statement</i>.")
 *
 * <blockquote>
 *
 * <table>
 * <caption>Range Types</caption>
 * <tr><th>Notation        <th>Definition               <th>Factory method
 * <tr><td>{@code (a..b)}  <td>{@code {x | a < x < b}}  <td>{@link Range#open open}
 * <tr><td>{@code [a..b]}  <td>{@code {x | a <= x <= b}}<td>{@link Range#closed closed}
 * <tr><td>{@code (a..b]}  <td>{@code {x | a < x <= b}} <td>{@link Range#openClosed openClosed}
 * <tr><td>{@code [a..b)}  <td>{@code {x | a <= x < b}} <td>{@link Range#closedOpen closedOpen}
 * <tr><td>{@code (a..+∞)} <td>{@code {x | x > a}}      <td>{@link Range#greaterThan greaterThan}
 * <tr><td>{@code [a..+∞)} <td>{@code {x | x >= a}}     <td>{@link Range#atLeast atLeast}
 * <tr><td>{@code (-∞..b)} <td>{@code {x | x < b}}      <td>{@link Range#lessThan lessThan}
 * <tr><td>{@code (-∞..b]} <td>{@code {x | x <= b}}     <td>{@link Range#atMost atMost}
 * <tr><td>{@code (-∞..+∞)}<td>{@code {x}}              <td>{@link Range#all all}
 * </table>
 *
 * </blockquote>
 *
 * <p>When both endpoints exist, the upper endpoint may not be less than the lower. The endpoints
 * may be equal only if at least one of the bounds is closed:
 *
 * <ul>
 *   <li>{@code [a..a]} : a singleton range
 *   <li>{@code [a..a); (a..a]} : {@linkplain #isEmpty empty} ranges; also valid
 *   <li>{@code (a..a)} : <b>invalid</b>; an exception will be thrown
 * </ul>
 *
 * <h3>Warnings</h3>
 *
 * <ul>
 *   <li>Use immutable value types only, if at all possible. If you must use a mutable type, <b>do
 *       not</b> allow the endpoint instances to mutate after the range is created!
 *   <li>Your value type's comparison method should be {@linkplain Comparable consistent with
 *       equals} if at all possible. Otherwise, be aware that concepts used throughout this
 *       documentation such as "equal", "same", "unique" and so on actually refer to whether {@link
 *       Comparable#compareTo compareTo} returns zero, not whether {@link Object#equals equals}
 *       returns {@code true}.
 *   <li>A class which implements {@code Comparable<UnrelatedType>} is very broken, and will cause
 *       undefined horrible things to happen in {@code Range}. For now, the Range API does not
 *       prevent its use, because this would also rule out all ungenerified (pre-JDK1.5) data types.
 *       <b>This may change in the future.</b>
 * </ul>
 *
 * <h3>Other notes</h3>
 *
 * <ul>
 *   <li>All ranges are shallow-immutable.
 *   <li>Instances of this type are obtained using the static factory methods in this class.
 *   <li>Ranges are <i>convex</i>: whenever two values are contained, all values in between them
 *       must also be contained. More formally, for any {@code c1 <= c2 <= c3} of type {@code C},
 *       {@code r.contains(c1) && r.contains(c3)} implies {@code r.contains(c2)}). This means that a
 *       {@code Range<Integer>} can never be used to represent, say, "all <i>prime</i> numbers from
 *       1 to 100."
 *   <li>When evaluated as a {@link Predicate}, a range yields the same result as invoking {@link
 *       #contains}.
 *   <li>Terminology note: a range {@code a} is said to be the <i>maximal</i> range having property
 *       <i>P</i> if, for all ranges {@code b} also having property <i>P</i>, {@code a.encloses(b)}.
 *       Likewise, {@code a} is <i>minimal</i> when {@code b.encloses(a)} for all {@code b} having
 *       property <i>P</i>. See, for example, the definition of {@link #intersection intersection}.
 *   <li>A {@code Range} is serializable if it has no bounds, or if each bound is serializable.
 * </ul>
 *
 * <h3>Further reading</h3>
 *
 * <p>See the Guava User Guide article on <a
 * href="https://github.com/google/guava/wiki/RangesExplained">{@code Range}</a>.
 *
 * @author Kevin Bourrillion
 * @author Gregory Kick
 * @since 10.0
 */
@GwtCompatible
@SuppressWarnings("rawtypes")
@Immutable(containerOf = "C")
@ElementTypesAreNonnullByDefault
public final class Range<C extends Comparable> extends RangeGwtSerializationDependencies
    implements Predicate<C>, Serializable {

  static class LowerBoundFn implements Function<Range, Cut> {
    static final LowerBoundFn INSTANCE = new LowerBoundFn();

    @Override
    public Cut apply(Range range) {
      return range.lowerBound;
    }
  }

  static class UpperBoundFn implements Function<Range, Cut> {
    static final UpperBoundFn INSTANCE = new UpperBoundFn();

    @Override
    public Cut apply(Range range) {
      return range.upperBound;
    }
  }

  @SuppressWarnings("unchecked")
  static <C extends Comparable<?>> Function<Range<C>, Cut<C>> lowerBoundFn() {
    return (Function) LowerBoundFn.INSTANCE;
  }

  @SuppressWarnings("unchecked")
  static <C extends Comparable<?>> Function<Range<C>, Cut<C>> upperBoundFn() {
    return (Function) UpperBoundFn.INSTANCE;
  }

  static <C extends Comparable<?>> Ordering<Range<C>> rangeLexOrdering() {
    return (Ordering<Range<C>>) (Ordering) RangeLexOrdering.INSTANCE;
  }

  static <C extends Comparable<?>> Range<C> create(Cut<C> lowerBound, Cut<C> upperBound) {
    return new Range<>(lowerBound, upperBound);
  }

  /**
   * Returns a range that contains all values strictly greater than {@code lower} and strictly less
   * than {@code upper}.
   *
   * @throws IllegalArgumentException if {@code lower} is greater than <i>or equal to</i> {@code
   *     upper}
   * @throws ClassCastException if {@code lower} and {@code upper} are not mutually comparable
   * @since 14.0
   */
  public static <C extends Comparable<?>> Range<C> open(C lower, C upper) {
    return create(Cut.aboveValue(lower), Cut.belowValue(upper));
  }

  /**
   * Returns a range that contains all values greater than or equal to {@code lower} and less than
   * or equal to {@code upper}.
   *
   * @throws IllegalArgumentException if {@code lower} is greater than {@code upper}
   * @throws ClassCastException if {@code lower} and {@code upper} are not mutually comparable
   * @since 14.0
   */
  public static <C extends Comparable<?>> Range<C> closed(C lower, C upper) {
    return create(Cut.belowValue(lower), Cut.aboveValue(upper));
  }

  /**
   * Returns a range that contains all values greater than or equal to {@code lower} and strictly
   * less than {@code upper}.
   *
   * @throws IllegalArgumentException if {@code lower} is greater than {@code upper}
   * @throws ClassCastException if {@code lower} and {@code upper} are not mutually comparable
   * @since 14.0
   */
  public static <C extends Comparable<?>> Range<C> closedOpen(C lower, C upper) {
    return create(Cut.belowValue(lower), Cut.belowValue(upper));
  }

  /**
   * Returns a range that contains all values strictly greater than {@code lower} and less than or
   * equal to {@code upper}.
   *
   * @throws IllegalArgumentException if {@code lower} is greater than {@code upper}
   * @throws ClassCastException if {@code lower} and {@code upper} are not mutually comparable
   * @since 14.0
   */
  public static <C extends Comparable<?>> Range<C> openClosed(C lower, C upper) {
    return create(Cut.aboveValue(lower), Cut.aboveValue(upper));
  }

  /**
   * Returns a range that contains any value from {@code lower} to {@code upper}, where each
   * endpoint may be either inclusive (closed) or exclusive (open).
   *
   * @throws IllegalArgumentException if {@code lower} is greater than {@code upper}
   * @throws ClassCastException if {@code lower} and {@code upper} are not mutually comparable
   * @since 14.0
   */
  public static <C extends Comparable<?>> Range<C> range(
      C lower, BoundType lowerType, C upper, BoundType upperType) {
    checkNotNull(lowerType);
    checkNotNull(upperType);

    Cut<C> lowerBound =
        (lowerType == BoundType.OPEN) ? Cut.aboveValue(lower) : Cut.belowValue(lower);
    Cut<C> upperBound =
        (upperType == BoundType.OPEN) ? Cut.belowValue(upper) : Cut.aboveValue(upper);
    return create(lowerBound, upperBound);
  }

  /**
   * Returns a range that contains all values strictly less than {@code endpoint}.
   *
   * @since 14.0
   */
  public static <C extends Comparable<?>> Range<C> lessThan(C endpoint) {
    return create(Cut.<C>belowAll(), Cut.belowValue(endpoint));
  }

  /**
   * Returns a range that contains all values less than or equal to {@code endpoint}.
   *
   * @since 14.0
   */
  public static <C extends Comparable<?>> Range<C> atMost(C endpoint) {
    return create(Cut.<C>belowAll(), Cut.aboveValue(endpoint));
  }

  /**
   * Returns a range with no lower bound up to the given endpoint, which may be either inclusive
   * (closed) or exclusive (open).
   *
   * @since 14.0
   */
  public static <C extends Comparable<?>> Range<C> upTo(C endpoint, BoundType boundType) {
    switch (boundType) {
      case OPEN:
        return lessThan(endpoint);
      case CLOSED:
        return atMost(endpoint);
      default:
        throw new AssertionError();
    }
  }

  /**
   * Returns a range that contains all values strictly greater than {@code endpoint}.
   *
   * @since 14.0
   */
  public static <C extends Comparable<?>> Range<C> greaterThan(C endpoint) {
    return create(Cut.aboveValue(endpoint), Cut.<C>aboveAll());
  }

  /**
   * Returns a range that contains all values greater than or equal to {@code endpoint}.
   *
   * @since 14.0
   */
  public static <C extends Comparable<?>> Range<C> atLeast(C endpoint) {
    return create(Cut.belowValue(endpoint), Cut.<C>aboveAll());
  }

  /**
   * Returns a range from the given endpoint, which may be either inclusive (closed) or exclusive
   * (open), with no upper bound.
   *
   * @since 14.0
   */
  public static <C extends Comparable<?>> Range<C> downTo(C endpoint, BoundType boundType) {
    switch (boundType) {
      case OPEN:
        return greaterThan(endpoint);
      case CLOSED:
        return atLeast(endpoint);
      default:
        throw new AssertionError();
    }
  }

  private static final Range<Comparable> ALL = new Range<>(Cut.belowAll(), Cut.aboveAll());

  /**
   * Returns a range that contains every value of type {@code C}.
   *
   * @since 14.0
   */
  @SuppressWarnings("unchecked")
  public static <C extends Comparable<?>> Range<C> all() {
    return (Range) ALL;
  }

  /**
   * Returns a range that {@linkplain Range#contains(Comparable) contains} only the given value. The
   * returned range is {@linkplain BoundType#CLOSED closed} on both ends.
   *
   * @since 14.0
   */
  public static <C extends Comparable<?>> Range<C> singleton(C value) {
    return closed(value, value);
  }

  /**
   * Returns the minimal range that {@linkplain Range#contains(Comparable) contains} all of the
   * given values. The returned range is {@linkplain BoundType#CLOSED closed} on both ends.
   *
   * @throws ClassCastException if the values are not mutually comparable
   * @throws NoSuchElementException if {@code values} is empty
   * @throws NullPointerException if any of {@code values} is null
   * @since 14.0
   */
  public static <C extends Comparable<?>> Range<C> encloseAll(Iterable<C> values) {
    checkNotNull(values);
    if (values instanceof SortedSet) {
      SortedSet<C> set = (SortedSet<C>) values;
      Comparator<?> comparator = set.comparator();
      if (Ordering.<C>natural().equals(comparator) || comparator == null) {
        return closed(set.first(), set.last());
      }
    }
    Iterator<C> valueIterator = values.iterator();
    C min = checkNotNull(valueIterator.next());
    C max = min;
    while (valueIterator.hasNext()) {
      C value = checkNotNull(valueIterator.next());
      min = Ordering.<C>natural().min(min, value);
      max = Ordering.<C>natural().max(max, value);
    }
    return closed(min, max);
  }

  final Cut<C> lowerBound;
  final Cut<C> upperBound;

  private Range(Cut<C> lowerBound, Cut<C> upperBound) {
    this.lowerBound = checkNotNull(lowerBound);
    this.upperBound = checkNotNull(upperBound);
    if (lowerBound.compareTo(upperBound) > 0
        || lowerBound == Cut.<C>aboveAll()
        || upperBound == Cut.<C>belowAll()) {
      throw new IllegalArgumentException("Invalid range: " + toString(lowerBound, upperBound));
    }
  }

  /** Returns {@code true} if this range has a lower endpoint. */
  public boolean hasLowerBound() {
    return lowerBound != Cut.belowAll();
  }

  /**
   * Returns the lower endpoint of this range.
   *
   * @throws IllegalStateException if this range is unbounded below (that is, {@link
   *     #hasLowerBound()} returns {@code false})
   */
  public C lowerEndpoint() {
    return lowerBound.endpoint();
  }

  /**
   * Returns the type of this range's lower bound: {@link BoundType#CLOSED} if the range includes
   * its lower endpoint, {@link BoundType#OPEN} if it does not.
   *
   * @throws IllegalStateException if this range is unbounded below (that is, {@link
   *     #hasLowerBound()} returns {@code false})
   */
  public BoundType lowerBoundType() {
    return lowerBound.typeAsLowerBound();
  }

  /** Returns {@code true} if this range has an upper endpoint. */
  public boolean hasUpperBound() {
    return upperBound != Cut.aboveAll();
  }

  /**
   * Returns the upper endpoint of this range.
   *
   * @throws IllegalStateException if this range is unbounded above (that is, {@link
   *     #hasUpperBound()} returns {@code false})
   */
  public C upperEndpoint() {
    return upperBound.endpoint();
  }

  /**
   * Returns the type of this range's upper bound: {@link BoundType#CLOSED} if the range includes
   * its upper endpoint, {@link BoundType#OPEN} if it does not.
   *
   * @throws IllegalStateException if this range is unbounded above (that is, {@link
   *     #hasUpperBound()} returns {@code false})
   */
  public BoundType upperBoundType() {
    return upperBound.typeAsUpperBound();
  }

  /**
   * Returns {@code true} if this range is of the form {@code [v..v)} or {@code (v..v]}. (This does
   * not encompass ranges of the form {@code (v..v)}, because such ranges are <i>invalid</i> and
   * can't be constructed at all.)
   *
   * <p>Note that certain discrete ranges such as the integer range {@code (3..4)} are <b>not</b>
   * considered empty, even though they contain no actual values. In these cases, it may be helpful
   * to preprocess ranges with {@link #canonical(DiscreteDomain)}.
   */
  public boolean isEmpty() {
    return lowerBound.equals(upperBound);
  }

  /**
   * Returns {@code true} if {@code value} is within the bounds of this range. For example, on the
   * range {@code [0..2)}, {@code contains(1)} returns {@code true}, while {@code contains(2)}
   * returns {@code false}.
   */
  public boolean contains(C value) {
    checkNotNull(value);
    // let this throw CCE if there is some trickery going on
    return lowerBound.isLessThan(value) && !upperBound.isLessThan(value);
  }

  /**
   * @deprecated Provided only to satisfy the {@link Predicate} interface; use {@link #contains}
   *     instead.
   */
  @Deprecated
  @Override
  public boolean apply(C input) {
    return contains(input);
  }

  /**
   * Returns {@code true} if every element in {@code values} is {@linkplain #contains contained} in
   * this range.
   */
  public boolean containsAll(Iterable<? extends C> values) {
    if (Iterables.isEmpty(values)) {
      return true;
    }

    // this optimizes testing equality of two range-backed sets
    if (values instanceof SortedSet) {
      SortedSet<? extends C> set = (SortedSet<? extends C>) values;
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
   * Returns {@code true} if the bounds of {@code other} do not extend outside the bounds of this
   * range. Examples:
   *
   * <ul>
   *   <li>{@code [3..6]} encloses {@code [4..5]}
   *   <li>{@code (3..6)} encloses {@code (3..6)}
   *   <li>{@code [3..6]} encloses {@code [4..4)} (even though the latter is empty)
   *   <li>{@code (3..6]} does not enclose {@code [3..6]}
   *   <li>{@code [4..5]} does not enclose {@code (3..6)} (even though it contains every value
   *       contained by the latter range)
   *   <li>{@code [3..6]} does not enclose {@code (1..1]} (even though it contains every value
   *       contained by the latter range)
   * </ul>
   *
   * <p>Note that if {@code a.encloses(b)}, then {@code b.contains(v)} implies {@code
   * a.contains(v)}, but as the last two examples illustrate, the converse is not always true.
   *
   * <p>Being reflexive, antisymmetric and transitive, the {@code encloses} relation defines a
   * <i>partial order</i> over ranges. There exists a unique {@linkplain Range#all maximal} range
   * according to this relation, and also numerous {@linkplain #isEmpty minimal} ranges. Enclosure
   * also implies {@linkplain #isConnected connectedness}.
   */
  public boolean encloses(Range<C> other) {
    return lowerBound.compareTo(other.lowerBound) <= 0
        && upperBound.compareTo(other.upperBound) >= 0;
  }

  /**
   * Returns {@code true} if there exists a (possibly empty) range which is {@linkplain #encloses
   * enclosed} by both this range and {@code other}.
   *
   * <p>For example,
   *
   * <ul>
   *   <li>{@code [2, 4)} and {@code [5, 7)} are not connected
   *   <li>{@code [2, 4)} and {@code [3, 5)} are connected, because both enclose {@code [3, 4)}
   *   <li>{@code [2, 4)} and {@code [4, 6)} are connected, because both enclose the empty range
   *       {@code [4, 4)}
   * </ul>
   *
   * <p>Note that this range and {@code other} have a well-defined {@linkplain #span union} and
   * {@linkplain #intersection intersection} (as a single, possibly-empty range) if and only if this
   * method returns {@code true}.
   *
   * <p>The connectedness relation is both reflexive and symmetric, but does not form an {@linkplain
   * Equivalence equivalence relation} as it is not transitive.
   *
   * <p>Note that certain discrete ranges are not considered connected, even though there are no
   * elements "between them." For example, {@code [3, 5]} is not considered connected to {@code [6,
   * 10]}. In these cases, it may be desirable for both input ranges to be preprocessed with {@link
   * #canonical(DiscreteDomain)} before testing for connectedness.
   */
  public boolean isConnected(Range<C> other) {
    return lowerBound.compareTo(other.upperBound) <= 0
        && other.lowerBound.compareTo(upperBound) <= 0;
  }

  /**
   * Returns the maximal range {@linkplain #encloses enclosed} by both this range and {@code
   * connectedRange}, if such a range exists.
   *
   * <p>For example, the intersection of {@code [1..5]} and {@code (3..7)} is {@code (3..5]}. The
   * resulting range may be empty; for example, {@code [1..5)} intersected with {@code [5..7)}
   * yields the empty range {@code [5..5)}.
   *
   * <p>The intersection exists if and only if the two ranges are {@linkplain #isConnected
   * connected}.
   *
   * <p>The intersection operation is commutative, associative and idempotent, and its identity
   * element is {@link Range#all}).
   *
   * @throws IllegalArgumentException if {@code isConnected(connectedRange)} is {@code false}
   */
  public Range<C> intersection(Range<C> connectedRange) {
    int lowerCmp = lowerBound.compareTo(connectedRange.lowerBound);
    int upperCmp = upperBound.compareTo(connectedRange.upperBound);
    if (lowerCmp >= 0 && upperCmp <= 0) {
      return this;
    } else if (lowerCmp <= 0 && upperCmp >= 0) {
      return connectedRange;
    } else {
      Cut<C> newLower = (lowerCmp >= 0) ? lowerBound : connectedRange.lowerBound;
      Cut<C> newUpper = (upperCmp <= 0) ? upperBound : connectedRange.upperBound;

      // create() would catch this, but give a confusing error message
      checkArgument(
          newLower.compareTo(newUpper) <= 0,
          "intersection is undefined for disconnected ranges %s and %s",
          this,
          connectedRange);

      // TODO(kevinb): all the precondition checks in the constructor are redundant...
      return create(newLower, newUpper);
    }
  }

  /**
   * Returns the maximal range lying between this range and {@code otherRange}, if such a range
   * exists. The resulting range may be empty if the two ranges are adjacent but non-overlapping.
   *
   * <p>For example, the gap of {@code [1..5]} and {@code (7..10)} is {@code (5..7]}. The resulting
   * range may be empty; for example, the gap between {@code [1..5)} {@code [5..7)} yields the empty
   * range {@code [5..5)}.
   *
   * <p>The gap exists if and only if the two ranges are either disconnected or immediately adjacent
   * (any intersection must be an empty range).
   *
   * <p>The gap operation is commutative.
   *
   * @throws IllegalArgumentException if this range and {@code otherRange} have a nonempty
   *     intersection
   * @since 27.0
   */
  public Range<C> gap(Range<C> otherRange) {
    /*
     * For an explanation of the basic principle behind this check, see
     * https://stackoverflow.com/a/35754308/28465
     *
     * In that explanation's notation, our `overlap` check would be `x1 < y2 && y1 < x2`. We've
     * flipped one part of the check so that we're using "less than" in both cases (rather than a
     * mix of "less than" and "greater than"). We've also switched to "strictly less than" rather
     * than "less than or equal to" because of *handwave* the difference between "endpoints of
     * inclusive ranges" and "Cuts."
     */
    if (lowerBound.compareTo(otherRange.upperBound) < 0
        && otherRange.lowerBound.compareTo(upperBound) < 0) {
      throw new IllegalArgumentException(
          "Ranges have a nonempty intersection: " + this + ", " + otherRange);
    }

    boolean isThisFirst = this.lowerBound.compareTo(otherRange.lowerBound) < 0;
    Range<C> firstRange = isThisFirst ? this : otherRange;
    Range<C> secondRange = isThisFirst ? otherRange : this;
    return create(firstRange.upperBound, secondRange.lowerBound);
  }

  /**
   * Returns the minimal range that {@linkplain #encloses encloses} both this range and {@code
   * other}. For example, the span of {@code [1..3]} and {@code (5..7)} is {@code [1..7)}.
   *
   * <p><i>If</i> the input ranges are {@linkplain #isConnected connected}, the returned range can
   * also be called their <i>union</i>. If they are not, note that the span might contain values
   * that are not contained in either input range.
   *
   * <p>Like {@link #intersection(Range) intersection}, this operation is commutative, associative
   * and idempotent. Unlike it, it is always well-defined for any two input ranges.
   */
  public Range<C> span(Range<C> other) {
    int lowerCmp = lowerBound.compareTo(other.lowerBound);
    int upperCmp = upperBound.compareTo(other.upperBound);
    if (lowerCmp <= 0 && upperCmp >= 0) {
      return this;
    } else if (lowerCmp >= 0 && upperCmp <= 0) {
      return other;
    } else {
      Cut<C> newLower = (lowerCmp <= 0) ? lowerBound : other.lowerBound;
      Cut<C> newUpper = (upperCmp >= 0) ? upperBound : other.upperBound;
      return create(newLower, newUpper);
    }
  }

  /**
   * Returns the canonical form of this range in the given domain. The canonical form has the
   * following properties:
   *
   * <ul>
   *   <li>equivalence: {@code a.canonical().contains(v) == a.contains(v)} for all {@code v} (in
   *       other words, {@code ContiguousSet.create(a.canonical(domain), domain).equals(
   *       ContiguousSet.create(a, domain))}
   *   <li>uniqueness: unless {@code a.isEmpty()}, {@code ContiguousSet.create(a,
   *       domain).equals(ContiguousSet.create(b, domain))} implies {@code
   *       a.canonical(domain).equals(b.canonical(domain))}
   *   <li>idempotence: {@code a.canonical(domain).canonical(domain).equals(a.canonical(domain))}
   * </ul>
   *
   * <p>Furthermore, this method guarantees that the range returned will be one of the following
   * canonical forms:
   *
   * <ul>
   *   <li>[start..end)
   *   <li>[start..+∞)
   *   <li>(-∞..end) (only if type {@code C} is unbounded below)
   *   <li>(-∞..+∞) (only if type {@code C} is unbounded below)
   * </ul>
   */
  public Range<C> canonical(DiscreteDomain<C> domain) {
    checkNotNull(domain);
    Cut<C> lower = lowerBound.canonical(domain);
    Cut<C> upper = upperBound.canonical(domain);
    return (lower == lowerBound && upper == upperBound) ? this : create(lower, upper);
  }

  /**
   * Returns {@code true} if {@code object} is a range having the same endpoints and bound types as
   * this range. Note that discrete ranges such as {@code (1..4)} and {@code [2..3]} are <b>not</b>
   * equal to one another, despite the fact that they each contain precisely the same set of values.
   * Similarly, empty ranges are not equal unless they have exactly the same representation, so
   * {@code [3..3)}, {@code (3..3]}, {@code (4..4]} are all unequal.
   */
  @Override
  public boolean equals(@CheckForNull Object object) {
    if (object instanceof Range) {
      Range<?> other = (Range<?>) object;
      return lowerBound.equals(other.lowerBound) && upperBound.equals(other.upperBound);
    }
    return false;
  }

  /** Returns a hash code for this range. */
  @Override
  public int hashCode() {
    return lowerBound.hashCode() * 31 + upperBound.hashCode();
  }

  /**
   * Returns a string representation of this range, such as {@code "[3..5)"} (other examples are
   * listed in the class documentation).
   */
  @Override
  public String toString() {
    return toString(lowerBound, upperBound);
  }

  private static String toString(Cut<?> lowerBound, Cut<?> upperBound) {
    StringBuilder sb = new StringBuilder(16);
    lowerBound.describeAsLowerBound(sb);
    sb.append("..");
    upperBound.describeAsUpperBound(sb);
    return sb.toString();
  }

  Object readResolve() {
    if (this.equals(ALL)) {
      return all();
    } else {
      return this;
    }
  }

  @SuppressWarnings("unchecked") // this method may throw CCE
  static int compareOrThrow(Comparable left, Comparable right) {
    return left.compareTo(right);
  }

  /** Needed to serialize sorted collections of Ranges. */
  private static class RangeLexOrdering extends Ordering<Range<?>> implements Serializable {
    static final Ordering<Range<?>> INSTANCE = new RangeLexOrdering();

    @Override
    public int compare(Range<?> left, Range<?> right) {
      return ComparisonChain.start()
          .compare(left.lowerBound, right.lowerBound)
          .compare(left.upperBound, right.upperBound)
          .result();
    }

    private static final long serialVersionUID = 0;
  }

  private static final long serialVersionUID = 0;
}
