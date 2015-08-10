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

import com.google.common.annotations.Beta;
import com.google.common.annotations.GwtCompatible;

import java.util.Comparator;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.SortedSet;

import javax.annotation.Nullable;

/**
 * A sorted set which forwards all its method calls to another sorted set.
 * Subclasses should override one or more methods to modify the behavior of the
 * backing sorted set as desired per the <a
 * href="http://en.wikipedia.org/wiki/Decorator_pattern">decorator pattern</a>.
 *
 * <p><i>Warning:</i> The methods of {@code ForwardingSortedSet} forward
 * <i>indiscriminately</i> to the methods of the delegate. For example,
 * overriding {@link #add} alone <i>will not</i> change the behavior of {@link
 * #addAll}, which can lead to unexpected behavior. In this case, you should
 * override {@code addAll} as well, either providing your own implementation, or
 * delegating to the provided {@code standardAddAll} method.
 *
 * <p>Each of the {@code standard} methods, where appropriate, uses the set's
 * comparator (or the natural ordering of the elements, if there is no
 * comparator) to test element equality. As a result, if the comparator is not
 * consistent with equals, some of the standard implementations may violate the
 * {@code Set} contract.
 *
 * <p>The {@code standard} methods and the collection views they return are not
 * guaranteed to be thread-safe, even when all of the methods that they depend
 * on are thread-safe.
 *
 * @author Mike Bostock
 * @author Louis Wasserman
 * @since 2.0
 */
@GwtCompatible
public abstract class ForwardingSortedSet<E> extends ForwardingSet<E> implements SortedSet<E> {

  /** Constructor for use by subclasses. */
  protected ForwardingSortedSet() {}

  @Override
  protected abstract SortedSet<E> delegate();

  @Override
  public Comparator<? super E> comparator() {
    return delegate().comparator();
  }

  @Override
  public E first() {
    return delegate().first();
  }

  @Override
  public SortedSet<E> headSet(E toElement) {
    return delegate().headSet(toElement);
  }

  @Override
  public E last() {
    return delegate().last();
  }

  @Override
  public SortedSet<E> subSet(E fromElement, E toElement) {
    return delegate().subSet(fromElement, toElement);
  }

  @Override
  public SortedSet<E> tailSet(E fromElement) {
    return delegate().tailSet(fromElement);
  }

  // unsafe, but worst case is a CCE is thrown, which callers will be expecting
  @SuppressWarnings("unchecked")
  private int unsafeCompare(Object o1, Object o2) {
    Comparator<? super E> comparator = comparator();
    return (comparator == null)
        ? ((Comparable<Object>) o1).compareTo(o2)
        : ((Comparator<Object>) comparator).compare(o1, o2);
  }

  /**
   * A sensible definition of {@link #contains} in terms of the {@code first()}
   * method of {@link #tailSet}. If you override {@link #tailSet}, you may wish
   * to override {@link #contains} to forward to this implementation.
   *
   * @since 7.0
   */
  @Override
  @Beta
  protected boolean standardContains(@Nullable Object object) {
    try {
      // any ClassCastExceptions are caught
      @SuppressWarnings("unchecked")
      SortedSet<Object> self = (SortedSet<Object>) this;
      Object ceiling = self.tailSet(object).first();
      return unsafeCompare(ceiling, object) == 0;
    } catch (ClassCastException e) {
      return false;
    } catch (NoSuchElementException e) {
      return false;
    } catch (NullPointerException e) {
      return false;
    }
  }

  /**
   * A sensible definition of {@link #remove} in terms of the {@code iterator()}
   * method of {@link #tailSet}. If you override {@link #tailSet}, you may wish
   * to override {@link #remove} to forward to this implementation.
   *
   * @since 7.0
   */
  @Override
  @Beta
  protected boolean standardRemove(@Nullable Object object) {
    try {
      // any ClassCastExceptions are caught
      @SuppressWarnings("unchecked")
      SortedSet<Object> self = (SortedSet<Object>) this;
      Iterator<Object> iterator = self.tailSet(object).iterator();
      if (iterator.hasNext()) {
        Object ceiling = iterator.next();
        if (unsafeCompare(ceiling, object) == 0) {
          iterator.remove();
          return true;
        }
      }
    } catch (ClassCastException e) {
      return false;
    } catch (NullPointerException e) {
      return false;
    }
    return false;
  }

  /**
   * A sensible default implementation of {@link #subSet(Object, Object)} in
   * terms of {@link #headSet(Object)} and {@link #tailSet(Object)}. In some
   * situations, you may wish to override {@link #subSet(Object, Object)} to
   * forward to this implementation.
   *
   * @since 7.0
   */
  @Beta
  protected SortedSet<E> standardSubSet(E fromElement, E toElement) {
    return tailSet(fromElement).headSet(toElement);
  }
}
