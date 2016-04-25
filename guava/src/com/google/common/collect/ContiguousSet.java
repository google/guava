/*
 * Copyright (C) 2010 The Guava Authors
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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.annotations.Beta;
import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;

import java.util.Collections;
import java.util.NoSuchElementException;
import java.util.Set;

/**
 * A sorted set of contiguous values in a given {@link DiscreteDomain}.
 *
 * <p><b>Warning:</b> Be extremely careful what you do with conceptually large instances (such as
 * {@code ContiguousSet.create(Range.greaterThan(0), DiscreteDomain.integers()}). Certain
 * operations on such a set can be performed efficiently, but others (such as {@link Set#hashCode}
 * or {@link Collections#frequency}) can cause major performance problems.
 *
 * @author Gregory Kick
 * @since 10.0
 */
@Beta
@GwtCompatible(emulated = true)
@SuppressWarnings("rawtypes") // allow ungenerified Comparable types
public abstract class ContiguousSet<C extends Comparable> extends ImmutableSortedSet<C> {
  /**
   * Returns a {@code ContiguousSet} containing the same values in the given domain
   * {@linkplain Range#contains contained} by the range.
   *
   * @throws IllegalArgumentException if neither range nor the domain has a lower bound, or if
   *     neither has an upper bound
   *
   * @since 13.0
   */
  public static <C extends Comparable> ContiguousSet<C> create(
      Range<C> range, DiscreteDomain<C> domain) {
    checkNotNull(range);
    checkNotNull(domain);
    Range<C> effectiveRange = range;
    try {
      if (!range.hasLowerBound()) {
        effectiveRange = effectiveRange.intersection(Range.atLeast(domain.minValue()));
      }
      if (!range.hasUpperBound()) {
        effectiveRange = effectiveRange.intersection(Range.atMost(domain.maxValue()));
      }
    } catch (NoSuchElementException e) {
      throw new IllegalArgumentException(e);
    }

    // Per class spec, we are allowed to throw CCE if necessary
    boolean empty =
        effectiveRange.isEmpty()
            || Range.compareOrThrow(
                    range.lowerBound.leastValueAbove(domain),
                    range.upperBound.greatestValueBelow(domain))
                > 0;

    return empty
        ? new EmptyContiguousSet<C>(domain)
        : new RegularContiguousSet<C>(effectiveRange, domain);
  }

  final DiscreteDomain<C> domain;

  ContiguousSet(DiscreteDomain<C> domain) {
    super(Ordering.natural());
    this.domain = domain;
  }

  @Override
  public ContiguousSet<C> headSet(C toElement) {
    return headSetImpl(checkNotNull(toElement), false);
  }

  /**
   * @since 12.0
   */
  @GwtIncompatible // NavigableSet
  @Override
  public ContiguousSet<C> headSet(C toElement, boolean inclusive) {
    return headSetImpl(checkNotNull(toElement), inclusive);
  }

  @Override
  public ContiguousSet<C> subSet(C fromElement, C toElement) {
    checkNotNull(fromElement);
    checkNotNull(toElement);
    checkArgument(comparator().compare(fromElement, toElement) <= 0);
    return subSetImpl(fromElement, true, toElement, false);
  }

  /**
   * @since 12.0
   */
  @GwtIncompatible // NavigableSet
  @Override
  public ContiguousSet<C> subSet(
      C fromElement, boolean fromInclusive, C toElement, boolean toInclusive) {
    checkNotNull(fromElement);
    checkNotNull(toElement);
    checkArgument(comparator().compare(fromElement, toElement) <= 0);
    return subSetImpl(fromElement, fromInclusive, toElement, toInclusive);
  }

  @Override
  public ContiguousSet<C> tailSet(C fromElement) {
    return tailSetImpl(checkNotNull(fromElement), true);
  }

  /**
   * @since 12.0
   */
  @GwtIncompatible // NavigableSet
  @Override
  public ContiguousSet<C> tailSet(C fromElement, boolean inclusive) {
    return tailSetImpl(checkNotNull(fromElement), inclusive);
  }

  /*
   * These methods perform most headSet, subSet, and tailSet logic, besides parameter validation.
   */
  // TODO(kevinb): we can probably make these real @Overrides now
  /* @Override */
  abstract ContiguousSet<C> headSetImpl(C toElement, boolean inclusive);

  /* @Override */
  abstract ContiguousSet<C> subSetImpl(
      C fromElement, boolean fromInclusive, C toElement, boolean toInclusive);

  /* @Override */
  abstract ContiguousSet<C> tailSetImpl(C fromElement, boolean inclusive);

  /**
   * Returns the set of values that are contained in both this set and the other.
   *
   * <p>This method should always be used instead of
   * {@link Sets#intersection} for {@link ContiguousSet} instances.
   */
  public abstract ContiguousSet<C> intersection(ContiguousSet<C> other);

  /**
   * Returns a range, closed on both ends, whose endpoints are the minimum and maximum values
   * contained in this set.  This is equivalent to {@code range(CLOSED, CLOSED)}.
   *
   * @throws NoSuchElementException if this set is empty
   */
  public abstract Range<C> range();

  /**
   * Returns the minimal range with the given boundary types for which all values in this set are
   * {@linkplain Range#contains(Comparable) contained} within the range.
   *
   * <p>Note that this method will return ranges with unbounded endpoints if {@link BoundType#OPEN}
   * is requested for a domain minimum or maximum.  For example, if {@code set} was created from the
   * range {@code [1..Integer.MAX_VALUE]} then {@code set.range(CLOSED, OPEN)} must return
   * {@code [1..∞)}.
   *
   * @throws NoSuchElementException if this set is empty
   */
  public abstract Range<C> range(BoundType lowerBoundType, BoundType upperBoundType);

  /** Returns a short-hand representation of the contents such as {@code "[1..100]"}. */
  @Override
  public String toString() {
    return range().toString();
  }

  /**
   * Not supported. {@code ContiguousSet} instances are constructed with {@link #create}. This
   * method exists only to hide {@link ImmutableSet#builder} from consumers of {@code
   * ContiguousSet}.
   *
   * @throws UnsupportedOperationException always
   * @deprecated Use {@link #create}.
   */
  @Deprecated
  public static <E> ImmutableSortedSet.Builder<E> builder() {
    throw new UnsupportedOperationException();
  }
}
