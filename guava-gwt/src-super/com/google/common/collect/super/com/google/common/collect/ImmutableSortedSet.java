/*
 * Copyright (C) 2009 The Guava Authors
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

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collector;
import javax.annotation.CheckForNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * GWT emulation of {@link com.google.common.collect.ImmutableSortedSet}.
 *
 * @author Hayward Chan
 */
@ElementTypesAreNonnullByDefault
public abstract class ImmutableSortedSet<E> extends ForwardingImmutableSet<E>
    implements SortedSet<E>, SortedIterable<E> {
  // TODO(cpovirk): split into ImmutableSortedSet/ForwardingImmutableSortedSet?

  // In the non-emulated source, this is in ImmutableSortedSetFauxverideShim,
  // which overrides ImmutableSet & which ImmutableSortedSet extends.
  // It is necessary here because otherwise the builder() method
  // would be inherited from the emulated ImmutableSet.
  // TODO(cpovirk): should we be including other methods from the shim here and
  // in ImmutableSortedMap?
  @Deprecated
  public static <E> ImmutableSortedSet.Builder<E> builder() {
    throw new UnsupportedOperationException();
  }

  private static final Comparator<?> NATURAL_ORDER = Ordering.natural();

  // TODO(b/345814817): Move this to RegularImmutableSortedSet?
  @SuppressWarnings({"unchecked", "ClassInitializationDeadlock"})
  private static final ImmutableSortedSet<Object> NATURAL_EMPTY_SET =
      new RegularImmutableSortedSet<Object>(
          new TreeSet<Object>((Comparator<Object>) NATURAL_ORDER), false);

  static <E> ImmutableSortedSet<E> emptySet(Comparator<? super E> comparator) {
    checkNotNull(comparator);
    if (NATURAL_ORDER.equals(comparator)) {
      return of();
    } else {
      return new RegularImmutableSortedSet<E>(new TreeSet<E>(comparator), false);
    }
  }

  public static <E> Collector<E, ?, ImmutableSortedSet<E>> toImmutableSortedSet(
      Comparator<? super E> comparator) {
    return CollectCollectors.toImmutableSortedSet(comparator);
  }

  @SuppressWarnings("unchecked")
  public static <E> ImmutableSortedSet<E> of() {
    return (ImmutableSortedSet<E>) NATURAL_EMPTY_SET;
  }

  public static <E extends Comparable<? super E>> ImmutableSortedSet<E> of(E e1) {
    return ofInternal(Ordering.natural(), e1);
  }

  @SuppressWarnings("unchecked")
  public static <E extends Comparable<? super E>> ImmutableSortedSet<E> of(E e1, E e2) {
    return ofInternal(Ordering.natural(), e1, e2);
  }

  @SuppressWarnings("unchecked")
  public static <E extends Comparable<? super E>> ImmutableSortedSet<E> of(E e1, E e2, E e3) {
    return ofInternal(Ordering.natural(), e1, e2, e3);
  }

  @SuppressWarnings("unchecked")
  public static <E extends Comparable<? super E>> ImmutableSortedSet<E> of(E e1, E e2, E e3, E e4) {
    return ofInternal(Ordering.natural(), e1, e2, e3, e4);
  }

  @SuppressWarnings("unchecked")
  public static <E extends Comparable<? super E>> ImmutableSortedSet<E> of(
      E e1, E e2, E e3, E e4, E e5) {
    return ofInternal(Ordering.natural(), e1, e2, e3, e4, e5);
  }

  @SuppressWarnings("unchecked")
  public static <E extends Comparable<? super E>> ImmutableSortedSet<E> of(
      E e1, E e2, E e3, E e4, E e5, E e6, E... remaining) {
    int size = remaining.length + 6;
    List<E> all = new ArrayList<E>(size);
    Collections.addAll(all, e1, e2, e3, e4, e5, e6);
    Collections.addAll(all, remaining);
    // This is messed up. See TODO at top of file.
    return ofInternal(Ordering.natural(), (E[]) all.toArray(new Comparable<?>[0]));
  }

  private static <E> ImmutableSortedSet<E> ofInternal(
      Comparator<? super E> comparator, E... elements) {
    checkNotNull(elements);
    switch (elements.length) {
      case 0:
        return emptySet(comparator);
      default:
        SortedSet<E> delegate = new TreeSet<E>(comparator);
        for (E element : elements) {
          checkNotNull(element);
          delegate.add(element);
        }
        return new RegularImmutableSortedSet<E>(delegate, false);
    }
  }

  // Unsafe, see ImmutableSortedSetFauxverideShim.
  @SuppressWarnings("unchecked")
  public static <E> ImmutableSortedSet<E> copyOf(Collection<? extends E> elements) {
    return copyOfInternal((Ordering<E>) Ordering.natural(), (Collection<E>) elements, false);
  }

  // Unsafe, see ImmutableSortedSetFauxverideShim.
  @SuppressWarnings("unchecked")
  public static <E> ImmutableSortedSet<E> copyOf(Iterable<? extends E> elements) {
    return copyOfInternal((Ordering<E>) Ordering.natural(), (Iterable<E>) elements, false);
  }

  // Unsafe, see ImmutableSortedSetFauxverideShim.
  @SuppressWarnings("unchecked")
  public static <E> ImmutableSortedSet<E> copyOf(Iterator<? extends E> elements) {
    return copyOfInternal((Ordering<E>) Ordering.natural(), (Iterator<E>) elements);
  }

  public static <E extends Comparable<? super E>> ImmutableSortedSet<E> copyOf(E[] elements) {
    return ofInternal(Ordering.natural(), elements);
  }

  public static <E> ImmutableSortedSet<E> copyOf(
      Comparator<? super E> comparator, Iterable<? extends E> elements) {
    checkNotNull(comparator);
    return copyOfInternal(comparator, elements, false);
  }

  public static <E> ImmutableSortedSet<E> copyOf(
      Comparator<? super E> comparator, Collection<? extends E> elements) {
    checkNotNull(comparator);
    return copyOfInternal(comparator, elements, false);
  }

  public static <E> ImmutableSortedSet<E> copyOf(
      Comparator<? super E> comparator, Iterator<? extends E> elements) {
    checkNotNull(comparator);
    return copyOfInternal(comparator, elements);
  }

  // We use NATURAL_ORDER only if the input was already using natural ordering.
  @SuppressWarnings("unchecked")
  public static <E> ImmutableSortedSet<E> copyOfSorted(SortedSet<E> sortedSet) {
    Comparator<? super E> comparator = sortedSet.comparator();
    if (comparator == null) {
      comparator = (Comparator<? super E>) NATURAL_ORDER;
    }
    return copyOfInternal(comparator, sortedSet.iterator());
  }

  private static <E> ImmutableSortedSet<E> copyOfInternal(
      Comparator<? super E> comparator, Iterable<? extends E> elements, boolean fromSortedSet) {
    checkNotNull(comparator);

    boolean hasSameComparator = fromSortedSet || hasSameComparator(elements, comparator);
    if (hasSameComparator && (elements instanceof ImmutableSortedSet)) {
      @SuppressWarnings("unchecked")
      ImmutableSortedSet<E> result = (ImmutableSortedSet<E>) elements;
      boolean isSubset =
          (result instanceof RegularImmutableSortedSet)
              && ((RegularImmutableSortedSet) result).isSubset;
      if (!isSubset) {
        // Only return the original copy if this immutable sorted set isn't
        // a subset of another, to avoid memory leak.
        return result;
      }
    }
    return copyOfInternal(comparator, elements.iterator());
  }

  private static <E> ImmutableSortedSet<E> copyOfInternal(
      Comparator<? super E> comparator, Iterator<? extends E> elements) {
    checkNotNull(comparator);
    if (!elements.hasNext()) {
      return emptySet(comparator);
    }
    SortedSet<E> delegate = new TreeSet<E>(comparator);
    while (elements.hasNext()) {
      E element = elements.next();
      checkNotNull(element);
      delegate.add(element);
    }
    return new RegularImmutableSortedSet<E>(delegate, false);
  }

  private static boolean hasSameComparator(Iterable<?> elements, Comparator<?> comparator) {
    if (elements instanceof SortedSet) {
      SortedSet<?> sortedSet = (SortedSet<?>) elements;
      Comparator<?> comparator2 = sortedSet.comparator();
      return (comparator2 == null)
          ? comparator == Ordering.natural()
          : comparator.equals(comparator2);
    }
    return false;
  }

  // Assumes that delegate doesn't have null elements and comparator.
  static <E> ImmutableSortedSet<E> unsafeDelegateSortedSet(
      SortedSet<E> delegate, boolean isSubset) {
    return delegate.isEmpty()
        ? emptySet(delegate.comparator())
        : new RegularImmutableSortedSet<E>(delegate, isSubset);
  }

  private final transient SortedSet<E> sortedDelegate;

  /**
   * Scary constructor for ContiguousSet. This constructor (in this file, the GWT emulation of
   * ImmutableSortedSet) creates an empty sortedDelegate, which, in a vacuum, sets this object's
   * contents to empty. By contrast, the non-GWT constructor with the same signature uses the
   * comparator only as a comparator. It does NOT assume empty contents. (It requires an
   * implementation of iterator() to define its contents, and methods like contains() are
   * implemented in terms of that method (though they will likely be overridden by subclasses for
   * performance reasons).) This means that a call to this method have can different behavior in GWT
   * and non-GWT environments UNLESS subclasses are careful to always override all methods
   * implemented in terms of sortedDelegate (except comparator()).
   */
  ImmutableSortedSet(Comparator<? super E> comparator) {
    this(Sets.newTreeSet(comparator));
  }

  ImmutableSortedSet(SortedSet<E> sortedDelegate) {
    super(sortedDelegate);
    this.sortedDelegate = Collections.unmodifiableSortedSet(sortedDelegate);
  }

  public Comparator<? super E> comparator() {
    return sortedDelegate.comparator();
  }

  @Override // needed to unify SortedIterable and Collection iterator() methods
  public UnmodifiableIterator<E> iterator() {
    return super.iterator();
  }

  @Override
  public Object[] toArray() {
    /*
     * ObjectArrays.toArrayImpl returns `@Nullable Object[]` rather than `Object[]` but only because
     * it can be used with collections that may contain null. This collection is a collection of
     * non-null elements, so we can treat it as a plain `Object[]`.
     */
    @SuppressWarnings("nullness")
    Object[] result = ObjectArrays.toArrayImpl(this);
    return result;
  }

  @Override
  @SuppressWarnings("nullness") // b/192354773 in our checker affects toArray declarations
  public <T extends @Nullable Object> T[] toArray(T[] other) {
    return ObjectArrays.toArrayImpl(this, other);
  }

  @Override
  public boolean contains(@Nullable Object object) {
    try {
      // This set never contains null.  We need to explicitly check here
      // because some comparator might throw NPE (e.g. the natural ordering).
      return object != null && sortedDelegate.contains(object);
    } catch (ClassCastException e) {
      return false;
    }
  }

  @Override
  public boolean containsAll(Collection<?> targets) {
    for (Object target : targets) {
      if (target == null) {
        // This set never contains null.  We need to explicitly check here
        // because some comparator might throw NPE (e.g. the natural ordering).
        return false;
      }
    }
    try {
      return sortedDelegate.containsAll(targets);
    } catch (ClassCastException e) {
      return false;
    }
  }

  public E first() {
    return sortedDelegate.first();
  }

  public ImmutableSortedSet<E> headSet(E toElement) {
    checkNotNull(toElement);
    try {
      return unsafeDelegateSortedSet(sortedDelegate.headSet(toElement), true);
    } catch (IllegalArgumentException e) {
      return emptySet(comparator());
    }
  }

  @CheckForNull
  E higher(E e) {
    checkNotNull(e);
    Iterator<E> iterator = tailSet(e).iterator();
    while (iterator.hasNext()) {
      E higher = iterator.next();
      if (comparator().compare(e, higher) < 0) {
        return higher;
      }
    }
    return null;
  }

  @CheckForNull
  public E ceiling(E e) {
    ImmutableSortedSet<E> set = tailSet(e, true);
    return !set.isEmpty() ? set.first() : null;
  }

  @CheckForNull
  public E floor(E e) {
    ImmutableSortedSet<E> set = headSet(e, true);
    return !set.isEmpty() ? set.last() : null;
  }

  public ImmutableSortedSet<E> headSet(E toElement, boolean inclusive) {
    checkNotNull(toElement);
    if (inclusive) {
      E tmp = higher(toElement);
      if (tmp == null) {
        return this;
      }
      toElement = tmp;
    }
    return headSet(toElement);
  }

  public E last() {
    return sortedDelegate.last();
  }

  public ImmutableSortedSet<E> subSet(E fromElement, E toElement) {
    return subSet(fromElement, true, toElement, false);
  }

  ImmutableSortedSet<E> subSet(
      E fromElement, boolean fromInclusive, E toElement, boolean toInclusive) {
    checkNotNull(fromElement);
    checkNotNull(toElement);
    int cmp = comparator().compare(fromElement, toElement);
    checkArgument(cmp <= 0, "fromElement (%s) is less than toElement (%s)", fromElement, toElement);
    if (cmp == 0 && !(fromInclusive && toInclusive)) {
      return emptySet(comparator());
    }
    return tailSet(fromElement, fromInclusive).headSet(toElement, toInclusive);
  }

  public ImmutableSortedSet<E> tailSet(E fromElement) {
    checkNotNull(fromElement);
    try {
      return unsafeDelegateSortedSet(sortedDelegate.tailSet(fromElement), true);
    } catch (IllegalArgumentException e) {
      return emptySet(comparator());
    }
  }

  public ImmutableSortedSet<E> tailSet(E fromElement, boolean inclusive) {
    checkNotNull(fromElement);
    if (!inclusive) {
      E tmp = higher(fromElement);
      if (tmp == null) {
        return emptySet(comparator());
      }
      fromElement = tmp;
    }
    return tailSet(fromElement);
  }

  public static <E> Builder<E> orderedBy(Comparator<E> comparator) {
    return new Builder<E>(comparator);
  }

  public static <E extends Comparable<?>> Builder<E> reverseOrder() {
    return new Builder<E>(Ordering.natural().reverse());
  }

  public static <E extends Comparable<?>> Builder<E> naturalOrder() {
    return new Builder<E>(Ordering.natural());
  }

  public static final class Builder<E> extends ImmutableSet.Builder<E> {
    private final Comparator<? super E> comparator;

    public Builder(Comparator<? super E> comparator) {
      this.comparator = checkNotNull(comparator);
    }

    Builder(Comparator<? super E> comparator, int expectedSize) {
      super(expectedSize);
      this.comparator = checkNotNull(comparator);
    }

    @CanIgnoreReturnValue
    @Override
    public Builder<E> add(E element) {
      super.add(element);
      return this;
    }

    @CanIgnoreReturnValue
    @Override
    public Builder<E> add(E... elements) {
      super.add(elements);
      return this;
    }

    @CanIgnoreReturnValue
    @Override
    public Builder<E> addAll(Iterable<? extends E> elements) {
      super.addAll(elements);
      return this;
    }

    @CanIgnoreReturnValue
    @Override
    public Builder<E> addAll(Iterator<? extends E> elements) {
      super.addAll(elements);
      return this;
    }

    @CanIgnoreReturnValue
    Builder<E> combine(Builder<E> builder) {
      super.combine(builder);
      return this;
    }

    @Override
    public ImmutableSortedSet<E> build() {
      return copyOfInternal(comparator, contents.iterator());
    }
  }
}
