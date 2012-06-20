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

import com.google.common.annotations.GwtIncompatible;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.collect.Synchronized.SynchronizedSortedSet;

import java.io.Serializable;
import java.util.Collections;
import java.util.Iterator;
import java.util.NavigableSet;
import java.util.SortedSet;

import javax.annotation.Nullable;

/**
 * Static utility methods pertaining to {@link NavigableSet} instances.
 *
 * @author Louis Wasserman
 * @see Sets
 */
@GwtIncompatible(value = "JDK 5 incompatibility")
final class NavigableSets {
  private NavigableSets() {}

  /**
   * Returns an unmodifiable view of the specified navigable set. This method
   * allows modules to provide users with "read-only" access to internal
   * navigable sets. Query operations on the returned navigable set "read
   * through" to the specified navigable set. Attempts to modify the returned
   * navigable set, whether direct, via its iterator, or via its
   * {@code descendingSet}, {@code subSet}, {@code headSet}, or
   * {@code tailSet} views, result in an
   * {@code UnsupportedOperationException}.
   *
   * <p>The returned navigable set will be serializable if the specified
   * navigable set is serializable.
   *
   * @param navigableSet the navigable set for which an unmodifiable view is to
   *        be returned.
   * @return an unmodifiable view of the specified navigable set.
   * @deprecated Use {@link Sets#unmodifiableNavigableSet}.
   */
  @Deprecated
  public static <E> NavigableSet<E> unmodifiableNavigableSet(
      NavigableSet<E> navigableSet) {
    return new UnmodifiableNavigableSet<E>(navigableSet);
  }

  private static class UnmodifiableNavigableSet<E>
      extends ForwardingSortedSet<E> implements NavigableSet<E>, Serializable {
    private final NavigableSet<E> navigableSet;
    private final SortedSet<E> unmodifiableSortedSet;

    UnmodifiableNavigableSet(NavigableSet<E> navigableSet) {
      this.navigableSet = Preconditions.checkNotNull(navigableSet);
      this.unmodifiableSortedSet =
          Collections.unmodifiableSortedSet(navigableSet);
    }

    @Override protected SortedSet<E> delegate() {
      return unmodifiableSortedSet;
    }

    @Override public E ceiling(E e) {
      return navigableSet.ceiling(e);
    }

    @Override public Iterator<E> descendingIterator() {
      return Iterators.unmodifiableIterator(navigableSet.descendingIterator());
    }

    private transient NavigableSet<E> descendingSet;

    @Override public NavigableSet<E> descendingSet() {
      if (descendingSet == null) {
        NavigableSet<E> dS =
            unmodifiableNavigableSet(navigableSet.descendingSet());
        descendingSet = dS;
        return dS;
      }
      return descendingSet;
    }

    @Override public E floor(E e) {
      return navigableSet.floor(e);
    }

    @Override public NavigableSet<E> headSet(E toElement, boolean inclusive) {
      return unmodifiableNavigableSet(
          navigableSet.headSet(toElement, inclusive));
    }

    @Override public E higher(E e) {
      return navigableSet.higher(e);
    }

    @Override public E lower(E e) {
      return navigableSet.lower(e);
    }

    @Override public E pollFirst() {
      throw new UnsupportedOperationException();
    }

    @Override public E pollLast() {
      throw new UnsupportedOperationException();
    }

    @Override public NavigableSet<E> subSet(E fromElement,
        boolean fromInclusive, E toElement, boolean toInclusive) {
      return unmodifiableNavigableSet(navigableSet.subSet(
          fromElement, fromInclusive, toElement, toInclusive));
    }

    @Override public NavigableSet<E> tailSet(E fromElement, boolean inclusive) {
      return unmodifiableNavigableSet(
          navigableSet.tailSet(fromElement, inclusive));
    }

    private static final long serialVersionUID = 0;
  }

  /**
   * Returns a synchronized (thread-safe) navigable set backed by the specified
   * navigable set.  In order to guarantee serial access, it is critical that
   * <b>all</b> access to the backing navigable set is accomplished
   * through the returned navigable set (or its views).
   *
   * <p>It is imperative that the user manually synchronize on the returned
   * sorted set when iterating over it or any of its {@code descendingSet},
   * {@code subSet}, {@code headSet}, or {@code tailSet} views. <pre>   {@code
   *
   *   NavigableSet<E> set = synchronizedNavigableSet(new TreeSet<E>());
   *    ...
   *   synchronized (set) {
   *     // Must be in the synchronized block
   *     Iterator<E> it = set.iterator();
   *     while (it.hasNext()){
   *       foo(it.next());
   *     }
   *   }}</pre>
   *
   * or: <pre>   {@code
   *
   *   NavigableSet<E> set = synchronizedNavigableSet(new TreeSet<E>());
   *   NavigableSet<E> set2 = set.descendingSet().headSet(foo);
   *    ...
   *   synchronized (set) { // Note: set, not set2!!!
   *     // Must be in the synchronized block
   *     Iterator<E> it = set2.descendingIterator();
   *     while (it.hasNext())
   *       foo(it.next());
   *     }
   *   }}</pre>
   *
   * Failure to follow this advice may result in non-deterministic behavior.
   *
   * <p>The returned navigable set will be serializable if the specified
   * navigable set is serializable.
   *
   * @param navigableSet the navigable set to be "wrapped" in a synchronized
   *    navigable set.
   * @return a synchronized view of the specified navigable set.
   * @deprecated Use {@link Sets#synchronizedNavigableSet}.
   */
  @Deprecated
  public static <E> NavigableSet<E> synchronizedNavigableSet(
      NavigableSet<E> navigableSet) {
    return synchronizedNavigableSet(navigableSet, null);
  }

  static <E> NavigableSet<E> synchronizedNavigableSet(
      NavigableSet<E> navigableSet, @Nullable Object mutex) {
    return new SynchronizedNavigableSet<E>(navigableSet, mutex);
  }

  @VisibleForTesting
  static class SynchronizedNavigableSet<E> extends SynchronizedSortedSet<E>
      implements NavigableSet<E> {

    SynchronizedNavigableSet(NavigableSet<E> delegate, @Nullable Object mutex) {
      super(delegate, mutex);
    }

    @Override NavigableSet<E> delegate() {
      return (NavigableSet<E>) super.delegate();
    }

    @Override public E ceiling(E e) {
      synchronized (mutex) {
        return delegate().ceiling(e);
      }
    }

    @Override public Iterator<E> descendingIterator() {
      return delegate().descendingIterator(); // manually synchronized
    }

    transient NavigableSet<E> descendingSet;

    @Override public NavigableSet<E> descendingSet() {
      synchronized (mutex) {
        if (descendingSet == null) {
          NavigableSet<E> dS =
              synchronizedNavigableSet(delegate().descendingSet(), mutex);
          descendingSet = dS;
          return dS;
        }
        return descendingSet;
      }
    }

    @Override public E floor(E e) {
      synchronized (mutex) {
        return delegate().floor(e);
      }
    }

    @Override public NavigableSet<E> headSet(E toElement, boolean inclusive) {
      synchronized (mutex) {
        return synchronizedNavigableSet(
            delegate().headSet(toElement, inclusive), mutex);
      }
    }

    @Override public E higher(E e) {
      synchronized (mutex) {
        return delegate().higher(e);
      }
    }

    @Override public E lower(E e) {
      synchronized (mutex) {
        return delegate().lower(e);
      }
    }

    @Override public E pollFirst() {
      synchronized (mutex) {
        return delegate().pollFirst();
      }
    }

    @Override public E pollLast() {
      synchronized (mutex) {
        return delegate().pollLast();
      }
    }

    @Override public NavigableSet<E> subSet(E fromElement,
        boolean fromInclusive, E toElement, boolean toInclusive) {
      synchronized (mutex) {
        return synchronizedNavigableSet(delegate().subSet(
            fromElement, fromInclusive, toElement, toInclusive), mutex);
      }
    }

    @Override public NavigableSet<E> tailSet(E fromElement, boolean inclusive) {
      synchronized (mutex) {
        return synchronizedNavigableSet(
            delegate().tailSet(fromElement, inclusive), mutex);
      }
    }

    @Override public SortedSet<E> headSet(E toElement) {
      return headSet(toElement, false);
    }

    @Override public SortedSet<E> subSet(E fromElement, E toElement) {
      return subSet(fromElement, true, toElement, false);
    }

    @Override public SortedSet<E> tailSet(E fromElement) {
      return tailSet(fromElement, true);
    }

    private static final long serialVersionUID = 0;
  }
}
