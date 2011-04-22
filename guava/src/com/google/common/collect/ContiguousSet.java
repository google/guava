/*
 * Copyright (C) 2010 The Guava Authors
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

import java.util.Collection;

import javax.annotation.Nullable;

/**
 * A sorted set of contiguous values in a given {@link DiscreteDomain}.
 *
 * @author gak@google.com (Gregory Kick)
 */
@GwtCompatible
@SuppressWarnings("unchecked") // allow ungenerified Comparable types
final class ContiguousSet<C extends Comparable> extends ImmutableSortedSet<C> {
  private final Range<C> range;
  private final DiscreteDomain<C> domain;

  ContiguousSet(Range<C> range, DiscreteDomain<C> domain) {
    super(Ordering.natural());
    this.range = range;
    this.domain = domain;
  }

  @Override public ImmutableSortedSet<C> headSet(C toElement) {
    return headSetImpl(checkNotNull(toElement));
  }

  // Abstract method doesn't exist in GWT emulation
  /* @Override */ ImmutableSortedSet<C> headSetImpl(C toElement) {
    return range.intersection(Ranges.lessThan(toElement)).asSet(domain);
  }

  // Abstract method doesn't exist in GWT emulation
  /* @Override */ int indexOf(Object target) {
    return contains(target) ? (int) domain.distance(first(), (C) target) : -1;
  }

  @Override public ImmutableSortedSet<C> subSet(C fromElement, C toElement) {
    checkNotNull(fromElement);
    checkNotNull(toElement);
    checkArgument(comparator().compare(fromElement, toElement) <= 0);
    return subSetImpl(fromElement, toElement);
  }

  // Abstract method doesn't exist in GWT emulation
  /* @Override */ ImmutableSortedSet<C> subSetImpl(C fromElement, C toElement) {
    return range.intersection(Ranges.closedOpen(fromElement, toElement)).asSet(domain);
  }

  @Override public ImmutableSortedSet<C> tailSet(C fromElement) {
    return tailSetImpl(checkNotNull(fromElement));
  }

  // Abstract method doesn't exist in GWT emulation
  /* @Override */ ImmutableSortedSet<C> tailSetImpl(C fromElement) {
    return range.intersection(Ranges.atLeast(fromElement)).asSet(domain);
  }

  @Override public UnmodifiableIterator<C> iterator() {
    return new AbstractLinkedIterator<C>(first()) {
      final C last = last();

      @Override
      protected C computeNext(C previous) {
        return equalsOrThrow(previous, last) ? null : domain.next(previous);
      }
    };
  }

  private static boolean equalsOrThrow(Comparable<?> left,
      @Nullable Comparable<?> right) {
    return right != null && compareOrThrow(left, right) == 0;
  }

  @SuppressWarnings("unchecked") // this method may throw CCE
  private static int compareOrThrow(Comparable left, Comparable right) {
    return left.compareTo(right);
  }

  @Override boolean isPartialView() {
    return false;
  }

  @Override public C first() {
    return range.lowerBound.leastValueAbove(domain);
  }

  @Override public C last() {
    return range.upperBound.greatestValueBelow(domain);
  }

  @Override public int size() {
    long distance = domain.distance(first(), last());
    return (distance >= Integer.MAX_VALUE) ? Integer.MAX_VALUE : (int) distance + 1;
  }

  @Override public boolean contains(Object object) {
    try {
      return range.contains((C) object);
    } catch (ClassCastException e) {
      return false;
    }
  }

  @Override public boolean containsAll(Collection<?> targets) {
    try {
      return range.containsAll((Iterable<? extends C>) targets);
    } catch (ClassCastException e) {
      return false;
    }
  }

  // copied to make sure not to use the GWT-emulated version
  @Override public Object[] toArray() {
    return ObjectArrays.toArrayImpl(this);
  }

  // copied to make sure not to use the GWT-emulated version
  @Override public <T> T[] toArray(T[] other) {
    return ObjectArrays.toArrayImpl(this, other);
  }

  @Override public boolean equals(Object object) {
    if (object == this) {
      return true;
    } else if (object instanceof ContiguousSet) {
      ContiguousSet<?> that = (ContiguousSet<?>) object;
      if (this.domain.equals(that.domain)) {
        return this.first().equals(that.first())
            && this.last().equals(that.last());
      }
    }
    return super.equals(object);
  }

  // copied to make sure not to use the GWT-emulated version
  @Override public int hashCode() {
    return Sets.hashCodeImpl(this);
  }

  /**
   * Returns a short-hand representation of the contents such as
   * {@code "[1‥100]}"}.
   */
  @Override public String toString() {
    return Ranges.closed(first(), last()).toString();
  }

  private static final long serialVersionUID = 0;
}
