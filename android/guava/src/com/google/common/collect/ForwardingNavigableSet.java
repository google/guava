/*
 * Copyright (C) 2012 The Guava Authors
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
import java.util.Iterator;
import java.util.NavigableSet;
import java.util.SortedSet;
import org.jspecify.annotations.Nullable;

/**
 * A navigable set which forwards all its method calls to another navigable set. Subclasses should
 * override one or more methods to modify the behavior of the backing set as desired per the <a
 * href="http://en.wikipedia.org/wiki/Decorator_pattern">decorator pattern</a>.
 *
 * <p><b>Warning:</b> The methods of {@code ForwardingNavigableSet} forward <i>indiscriminately</i>
 * to the methods of the delegate. For example, overriding {@link #add} alone <i>will not</i> change
 * the behavior of {@link #addAll}, which can lead to unexpected behavior. In this case, you should
 * override {@code addAll} as well, either providing your own implementation, or delegating to the
 * provided {@code standardAddAll} method.
 *
 * <p><b>{@code default} method warning:</b> This class does <i>not</i> forward calls to {@code
 * default} methods. Instead, it inherits their default implementations. When those implementations
 * invoke methods, they invoke methods on the {@code ForwardingNavigableSet}.
 *
 * <p>Each of the {@code standard} methods uses the set's comparator (or the natural ordering of the
 * elements, if there is no comparator) to test element equality. As a result, if the comparator is
 * not consistent with equals, some of the standard implementations may violate the {@code Set}
 * contract.
 *
 * <p>The {@code standard} methods and the collection views they return are not guaranteed to be
 * thread-safe, even when all of the methods that they depend on are thread-safe.
 *
 * @author Louis Wasserman
 * @since 12.0
 */
@GwtIncompatible
public abstract class ForwardingNavigableSet<E extends @Nullable Object>
    extends ForwardingSortedSet<E> implements NavigableSet<E> {

  /** Constructor for use by subclasses. */
  protected ForwardingNavigableSet() {}

  @Override
  protected abstract NavigableSet<E> delegate();

  @Override
  public @Nullable E lower(@ParametricNullness E e) {
    return delegate().lower(e);
  }

  /**
   * A sensible definition of {@link #lower} in terms of the {@code descendingIterator} method of
   * {@link #headSet(Object, boolean)}. If you override {@link #headSet(Object, boolean)}, you may
   * wish to override {@link #lower} to forward to this implementation.
   */
  protected @Nullable E standardLower(@ParametricNullness E e) {
    return Iterators.getNext(headSet(e, false).descendingIterator(), null);
  }

  @Override
  public @Nullable E floor(@ParametricNullness E e) {
    return delegate().floor(e);
  }

  /**
   * A sensible definition of {@link #floor} in terms of the {@code descendingIterator} method of
   * {@link #headSet(Object, boolean)}. If you override {@link #headSet(Object, boolean)}, you may
   * wish to override {@link #floor} to forward to this implementation.
   */
  protected @Nullable E standardFloor(@ParametricNullness E e) {
    return Iterators.getNext(headSet(e, true).descendingIterator(), null);
  }

  @Override
  public @Nullable E ceiling(@ParametricNullness E e) {
    return delegate().ceiling(e);
  }

  /**
   * A sensible definition of {@link #ceiling} in terms of the {@code iterator} method of {@link
   * #tailSet(Object, boolean)}. If you override {@link #tailSet(Object, boolean)}, you may wish to
   * override {@link #ceiling} to forward to this implementation.
   */
  protected @Nullable E standardCeiling(@ParametricNullness E e) {
    return Iterators.getNext(tailSet(e, true).iterator(), null);
  }

  @Override
  public @Nullable E higher(@ParametricNullness E e) {
    return delegate().higher(e);
  }

  /**
   * A sensible definition of {@link #higher} in terms of the {@code iterator} method of {@link
   * #tailSet(Object, boolean)}. If you override {@link #tailSet(Object, boolean)}, you may wish to
   * override {@link #higher} to forward to this implementation.
   */
  protected @Nullable E standardHigher(@ParametricNullness E e) {
    return Iterators.getNext(tailSet(e, false).iterator(), null);
  }

  @Override
  public @Nullable E pollFirst() {
    return delegate().pollFirst();
  }

  /**
   * A sensible definition of {@link #pollFirst} in terms of the {@code iterator} method. If you
   * override {@link #iterator} you may wish to override {@link #pollFirst} to forward to this
   * implementation.
   */
  protected @Nullable E standardPollFirst() {
    return Iterators.pollNext(iterator());
  }

  @Override
  public @Nullable E pollLast() {
    return delegate().pollLast();
  }

  /**
   * A sensible definition of {@link #pollLast} in terms of the {@code descendingIterator} method.
   * If you override {@link #descendingIterator} you may wish to override {@link #pollLast} to
   * forward to this implementation.
   */
  protected @Nullable E standardPollLast() {
    return Iterators.pollNext(descendingIterator());
  }

  @ParametricNullness
  protected E standardFirst() {
    return iterator().next();
  }

  @ParametricNullness
  protected E standardLast() {
    return descendingIterator().next();
  }

  @Override
  public NavigableSet<E> descendingSet() {
    return delegate().descendingSet();
  }

  /**
   * A sensible implementation of {@link NavigableSet#descendingSet} in terms of the other methods
   * of {@link NavigableSet}, notably including {@link NavigableSet#descendingIterator}.
   *
   * <p>In many cases, you may wish to override {@link ForwardingNavigableSet#descendingSet} to
   * forward to this implementation or a subclass thereof.
   *
   * @since 12.0
   */
  protected class StandardDescendingSet extends Sets.DescendingSet<E> {
    /** Constructor for use by subclasses. */
    public StandardDescendingSet() {
      super(ForwardingNavigableSet.this);
    }
  }

  @Override
  public Iterator<E> descendingIterator() {
    return delegate().descendingIterator();
  }

  @Override
  public NavigableSet<E> subSet(
      @ParametricNullness E fromElement,
      boolean fromInclusive,
      @ParametricNullness E toElement,
      boolean toInclusive) {
    return delegate().subSet(fromElement, fromInclusive, toElement, toInclusive);
  }

  /**
   * A sensible definition of {@link #subSet(Object, boolean, Object, boolean)} in terms of the
   * {@code headSet} and {@code tailSet} methods. In many cases, you may wish to override {@link
   * #subSet(Object, boolean, Object, boolean)} to forward to this implementation.
   */
  protected NavigableSet<E> standardSubSet(
      @ParametricNullness E fromElement,
      boolean fromInclusive,
      @ParametricNullness E toElement,
      boolean toInclusive) {
    return tailSet(fromElement, fromInclusive).headSet(toElement, toInclusive);
  }

  /**
   * A sensible definition of {@link #subSet(Object, Object)} in terms of the {@link #subSet(Object,
   * boolean, Object, boolean)} method. If you override {@link #subSet(Object, boolean, Object,
   * boolean)}, you may wish to override {@link #subSet(Object, Object)} to forward to this
   * implementation.
   */
  @Override
  protected SortedSet<E> standardSubSet(
      @ParametricNullness E fromElement, @ParametricNullness E toElement) {
    return subSet(fromElement, true, toElement, false);
  }

  @Override
  public NavigableSet<E> headSet(@ParametricNullness E toElement, boolean inclusive) {
    return delegate().headSet(toElement, inclusive);
  }

  /**
   * A sensible definition of {@link #headSet(Object)} in terms of the {@link #headSet(Object,
   * boolean)} method. If you override {@link #headSet(Object, boolean)}, you may wish to override
   * {@link #headSet(Object)} to forward to this implementation.
   */
  protected SortedSet<E> standardHeadSet(@ParametricNullness E toElement) {
    return headSet(toElement, false);
  }

  @Override
  public NavigableSet<E> tailSet(@ParametricNullness E fromElement, boolean inclusive) {
    return delegate().tailSet(fromElement, inclusive);
  }

  /**
   * A sensible definition of {@link #tailSet(Object)} in terms of the {@link #tailSet(Object,
   * boolean)} method. If you override {@link #tailSet(Object, boolean)}, you may wish to override
   * {@link #tailSet(Object)} to forward to this implementation.
   */
  protected SortedSet<E> standardTailSet(@ParametricNullness E fromElement) {
    return tailSet(fromElement, true);
  }
}
