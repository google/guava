/*
 * Copyright (C) 2011 The Guava Authors
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
import static com.google.common.base.Preconditions.checkElementIndex;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.BoundType.CLOSED;
import static java.util.Objects.requireNonNull;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.annotations.J2ktIncompatible;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.Collection;
import javax.annotation.CheckForNull;

/**
 * An implementation of {@link ContiguousSet} that contains one or more elements.
 *
 * @author Gregory Kick
 */
@GwtCompatible(emulated = true)
@SuppressWarnings("rawtypes") // https://github.com/google/guava/issues/989
@ElementTypesAreNonnullByDefault
final class RegularContiguousSet<C extends Comparable> extends ContiguousSet<C> {
  private final Range<C> range;

  RegularContiguousSet(Range<C> range, DiscreteDomain<C> domain) {
    super(domain);
    this.range = range;
  }

  private ContiguousSet<C> intersectionInCurrentDomain(Range<C> other) {
    return range.isConnected(other)
        ? ContiguousSet.create(range.intersection(other), domain)
        : new EmptyContiguousSet<C>(domain);
  }

  @Override
  ContiguousSet<C> headSetImpl(C toElement, boolean inclusive) {
    return intersectionInCurrentDomain(Range.upTo(toElement, BoundType.forBoolean(inclusive)));
  }

  @Override
  @SuppressWarnings("unchecked") // TODO(cpovirk): Use a shared unsafeCompare method.
  ContiguousSet<C> subSetImpl(
      C fromElement, boolean fromInclusive, C toElement, boolean toInclusive) {
    if (fromElement.compareTo(toElement) == 0 && !fromInclusive && !toInclusive) {
      // Range would reject our attempt to create (x, x).
      return new EmptyContiguousSet<>(domain);
    }
    return intersectionInCurrentDomain(
        Range.range(
            fromElement, BoundType.forBoolean(fromInclusive),
            toElement, BoundType.forBoolean(toInclusive)));
  }

  @Override
  ContiguousSet<C> tailSetImpl(C fromElement, boolean inclusive) {
    return intersectionInCurrentDomain(Range.downTo(fromElement, BoundType.forBoolean(inclusive)));
  }

  @GwtIncompatible // not used by GWT emulation
  @Override
  int indexOf(@CheckForNull Object target) {
    if (!contains(target)) {
      return -1;
    }
    // The cast is safe because of the contains check—at least for any reasonable Comparable class.
    @SuppressWarnings("unchecked")
    // requireNonNull is safe because of the contains check.
    C c = (C) requireNonNull(target);
    return (int) domain.distance(first(), c);
  }

  @Override
  public UnmodifiableIterator<C> iterator() {
    return new AbstractSequentialIterator<C>(first()) {
      final C last = last();

      @Override
      @CheckForNull
      protected C computeNext(C previous) {
        return equalsOrThrow(previous, last) ? null : domain.next(previous);
      }
    };
  }

  @GwtIncompatible // NavigableSet
  @Override
  public UnmodifiableIterator<C> descendingIterator() {
    return new AbstractSequentialIterator<C>(last()) {
      final C first = first();

      @Override
      @CheckForNull
      protected C computeNext(C previous) {
        return equalsOrThrow(previous, first) ? null : domain.previous(previous);
      }
    };
  }

  private static boolean equalsOrThrow(Comparable<?> left, @CheckForNull Comparable<?> right) {
    return right != null && Range.compareOrThrow(left, right) == 0;
  }

  @Override
  boolean isPartialView() {
    return false;
  }

  @Override
  public C first() {
    // requireNonNull is safe because we checked the range is not empty in ContiguousSet.create.
    return requireNonNull(range.lowerBound.leastValueAbove(domain));
  }

  @Override
  public C last() {
    // requireNonNull is safe because we checked the range is not empty in ContiguousSet.create.
    return requireNonNull(range.upperBound.greatestValueBelow(domain));
  }

  @Override
  ImmutableList<C> createAsList() {
    if (domain.supportsFastOffset) {
      return new ImmutableAsList<C>() {
        @Override
        ImmutableSortedSet<C> delegateCollection() {
          return RegularContiguousSet.this;
        }

        @Override
        public C get(int i) {
          checkElementIndex(i, size());
          return domain.offset(first(), i);
        }

        // redeclare to help optimizers with b/310253115
        @SuppressWarnings("RedundantOverride")
        @Override
        @J2ktIncompatible // serialization
        @GwtIncompatible // serialization
        Object writeReplace() {
          return super.writeReplace();
        }
      };
    } else {
      return super.createAsList();
    }
  }

  @Override
  public int size() {
    long distance = domain.distance(first(), last());
    return (distance >= Integer.MAX_VALUE) ? Integer.MAX_VALUE : (int) distance + 1;
  }

  @Override
  public boolean contains(@CheckForNull Object object) {
    if (object == null) {
      return false;
    }
    try {
      @SuppressWarnings("unchecked") // The worst case is usually CCE, which we catch.
      C c = (C) object;
      return range.contains(c);
    } catch (ClassCastException e) {
      return false;
    }
  }

  @Override
  public boolean containsAll(Collection<?> targets) {
    return Collections2.containsAllImpl(this, targets);
  }

  @Override
  public boolean isEmpty() {
    return false;
  }

  @Override
  @SuppressWarnings("unchecked") // TODO(cpovirk): Use a shared unsafeCompare method.
  public ContiguousSet<C> intersection(ContiguousSet<C> other) {
    checkNotNull(other);
    checkArgument(this.domain.equals(other.domain));
    if (other.isEmpty()) {
      return other;
    } else {
      C lowerEndpoint = Ordering.<C>natural().max(this.first(), other.first());
      C upperEndpoint = Ordering.<C>natural().min(this.last(), other.last());
      return (lowerEndpoint.compareTo(upperEndpoint) <= 0)
          ? ContiguousSet.create(Range.closed(lowerEndpoint, upperEndpoint), domain)
          : new EmptyContiguousSet<C>(domain);
    }
  }

  @Override
  public Range<C> range() {
    return range(CLOSED, CLOSED);
  }

  @Override
  public Range<C> range(BoundType lowerBoundType, BoundType upperBoundType) {
    return Range.create(
        range.lowerBound.withLowerBoundType(lowerBoundType, domain),
        range.upperBound.withUpperBoundType(upperBoundType, domain));
  }

  @Override
  public boolean equals(@CheckForNull Object object) {
    if (object == this) {
      return true;
    } else if (object instanceof RegularContiguousSet) {
      RegularContiguousSet<?> that = (RegularContiguousSet<?>) object;
      if (this.domain.equals(that.domain)) {
        return this.first().equals(that.first()) && this.last().equals(that.last());
      }
    }
    return super.equals(object);
  }

  // copied to make sure not to use the GWT-emulated version
  @Override
  public int hashCode() {
    return Sets.hashCodeImpl(this);
  }

  @GwtIncompatible // serialization
  @J2ktIncompatible
  private static final class SerializedForm<C extends Comparable> implements Serializable {
    final Range<C> range;
    final DiscreteDomain<C> domain;

    private SerializedForm(Range<C> range, DiscreteDomain<C> domain) {
      this.range = range;
      this.domain = domain;
    }

    private Object readResolve() {
      return new RegularContiguousSet<>(range, domain);
    }
  }

  @GwtIncompatible // serialization
  @J2ktIncompatible
  @Override
  Object writeReplace() {
    return new SerializedForm<>(range, domain);
  }

  @GwtIncompatible // serialization
  @J2ktIncompatible
  private void readObject(ObjectInputStream stream) throws InvalidObjectException {
    throw new InvalidObjectException("Use SerializedForm");
  }

  private static final long serialVersionUID = 0;
}
