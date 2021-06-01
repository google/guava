/*
 * Copyright (C) 2011 The Guava Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.common.collect;

import static com.google.common.collect.BoundType.CLOSED;
import static com.google.common.collect.BoundType.OPEN;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.collect.Multiset.Entry;
import com.google.j2objc.annotations.Weak;
import java.util.Comparator;
import java.util.Iterator;
import java.util.NavigableSet;
import java.util.NoSuchElementException;
import java.util.SortedSet;
import javax.annotation.CheckForNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Provides static utility methods for creating and working with {@link SortedMultiset} instances.
 *
 * @author Louis Wasserman
 */
@GwtCompatible(emulated = true)
@ElementTypesAreNonnullByDefault
final class SortedMultisets {
  private SortedMultisets() {}

  /** A skeleton implementation for {@link SortedMultiset#elementSet}. */
  @SuppressWarnings("JdkObsolete") // TODO(b/6160855): Switch GWT emulations to NavigableSet.
  static class ElementSet<E extends @Nullable Object> extends Multisets.ElementSet<E>
      implements SortedSet<E> {
    @Weak private final SortedMultiset<E> multiset;

    ElementSet(SortedMultiset<E> multiset) {
      this.multiset = multiset;
    }

    @Override
    final SortedMultiset<E> multiset() {
      return multiset;
    }

    @Override
    public Iterator<E> iterator() {
      return Multisets.elementIterator(multiset().entrySet().iterator());
    }

    @Override
    public Comparator<? super E> comparator() {
      return multiset().comparator();
    }

    @Override
    public SortedSet<E> subSet(@ParametricNullness E fromElement, @ParametricNullness E toElement) {
      return multiset().subMultiset(fromElement, CLOSED, toElement, OPEN).elementSet();
    }

    @Override
    public SortedSet<E> headSet(@ParametricNullness E toElement) {
      return multiset().headMultiset(toElement, OPEN).elementSet();
    }

    @Override
    public SortedSet<E> tailSet(@ParametricNullness E fromElement) {
      return multiset().tailMultiset(fromElement, CLOSED).elementSet();
    }

    @Override
    @ParametricNullness
    public E first() {
      return getElementOrThrow(multiset().firstEntry());
    }

    @Override
    @ParametricNullness
    public E last() {
      return getElementOrThrow(multiset().lastEntry());
    }
  }

  /** A skeleton navigable implementation for {@link SortedMultiset#elementSet}. */
  @GwtIncompatible // Navigable
  static class NavigableElementSet<E extends @Nullable Object> extends ElementSet<E>
      implements NavigableSet<E> {
    NavigableElementSet(SortedMultiset<E> multiset) {
      super(multiset);
    }

    @Override
    @CheckForNull
    public E lower(@ParametricNullness E e) {
      return getElementOrNull(multiset().headMultiset(e, OPEN).lastEntry());
    }

    @Override
    @CheckForNull
    public E floor(@ParametricNullness E e) {
      return getElementOrNull(multiset().headMultiset(e, CLOSED).lastEntry());
    }

    @Override
    @CheckForNull
    public E ceiling(@ParametricNullness E e) {
      return getElementOrNull(multiset().tailMultiset(e, CLOSED).firstEntry());
    }

    @Override
    @CheckForNull
    public E higher(@ParametricNullness E e) {
      return getElementOrNull(multiset().tailMultiset(e, OPEN).firstEntry());
    }

    @Override
    public NavigableSet<E> descendingSet() {
      return new NavigableElementSet<E>(multiset().descendingMultiset());
    }

    @Override
    public Iterator<E> descendingIterator() {
      return descendingSet().iterator();
    }

    @Override
    @CheckForNull
    public E pollFirst() {
      return getElementOrNull(multiset().pollFirstEntry());
    }

    @Override
    @CheckForNull
    public E pollLast() {
      return getElementOrNull(multiset().pollLastEntry());
    }

    @Override
    public NavigableSet<E> subSet(
        @ParametricNullness E fromElement,
        boolean fromInclusive,
        @ParametricNullness E toElement,
        boolean toInclusive) {
      return new NavigableElementSet<E>(
          multiset()
              .subMultiset(
                  fromElement, BoundType.forBoolean(fromInclusive),
                  toElement, BoundType.forBoolean(toInclusive)));
    }

    @Override
    public NavigableSet<E> headSet(@ParametricNullness E toElement, boolean inclusive) {
      return new NavigableElementSet<E>(
          multiset().headMultiset(toElement, BoundType.forBoolean(inclusive)));
    }

    @Override
    public NavigableSet<E> tailSet(@ParametricNullness E fromElement, boolean inclusive) {
      return new NavigableElementSet<E>(
          multiset().tailMultiset(fromElement, BoundType.forBoolean(inclusive)));
    }
  }

  private static <E extends @Nullable Object> E getElementOrThrow(@CheckForNull Entry<E> entry) {
    if (entry == null) {
      throw new NoSuchElementException();
    }
    return entry.getElement();
  }

  @CheckForNull
  private static <E extends @Nullable Object> E getElementOrNull(@CheckForNull Entry<E> entry) {
    return (entry == null) ? null : entry.getElement();
  }
}
