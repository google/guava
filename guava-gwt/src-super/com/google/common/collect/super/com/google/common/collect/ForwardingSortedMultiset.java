/*
 * Copyright (C) 2011 The Guava Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.common.collect;

import java.util.Comparator;
import java.util.Iterator;
import java.util.SortedSet;

/**
 * A sorted multiset which forwards all its method calls to another sorted multiset. Subclasses
 * should override one or more methods to modify the behavior of the backing multiset as desired per
 * the <a href="http://en.wikipedia.org/wiki/Decorator_pattern">decorator pattern</a>.
 *
 * <p><b>Warning:</b> The methods of {@code ForwardingSortedMultiset} forward
 * <b>indiscriminately</b> to the methods of the delegate. For example, overriding {@link
 * #add(Object, int)} alone <b>will not</b> change the behavior of {@link #add(Object)}, which can
 * lead to unexpected behavior. In this case, you should override {@code add(Object)} as well,
 * either providing your own implementation, or delegating to the provided {@code standardAdd}
 * method.
 *
 * <p>The {@code standard} methods and any collection views they return are not guaranteed to be
 * thread-safe, even when all of the methods that they depend on are thread-safe.
 *
 * @author Louis Wasserman
 */
public abstract class ForwardingSortedMultiset<E> extends ForwardingMultiset<E>
    implements SortedMultiset<E> {
  /** Constructor for use by subclasses. */
  protected ForwardingSortedMultiset() {}

  @Override
  protected abstract SortedMultiset<E> delegate();

  @Override
  public SortedSet<E> elementSet() {
    return (SortedSet<E>) super.elementSet();
  }

  /**
   * A sensible implementation of {@link SortedMultiset#elementSet} in terms of the following
   * methods: {@link SortedMultiset#clear}, {@link SortedMultiset#comparator}, {@link
   * SortedMultiset#contains}, {@link SortedMultiset#containsAll}, {@link SortedMultiset#count},
   * {@link SortedMultiset#firstEntry} {@link SortedMultiset#headMultiset}, {@link
   * SortedMultiset#isEmpty}, {@link SortedMultiset#lastEntry}, {@link SortedMultiset#subMultiset},
   * {@link SortedMultiset#tailMultiset}, the {@code size()} and {@code iterator()} methods of
   * {@link SortedMultiset#entrySet}, and {@link SortedMultiset#remove(Object, int)}. In many
   * situations, you may wish to override {@link SortedMultiset#elementSet} to forward to this
   * implementation or a subclass thereof.
   */
  protected class StandardElementSet extends SortedMultisets.ElementSet<E> {
    /** Constructor for use by subclasses. */
    public StandardElementSet() {
      super(ForwardingSortedMultiset.this);
    }
  }

  @Override
  public Comparator<? super E> comparator() {
    return delegate().comparator();
  }

  @Override
  public SortedMultiset<E> descendingMultiset() {
    return delegate().descendingMultiset();
  }

  /**
   * A skeleton implementation of a descending multiset view. Normally, {@link
   * #descendingMultiset()} will not reflect any changes you make to the behavior of methods such as
   * {@link #add(Object)} or {@link #pollFirstEntry}. This skeleton implementation correctly
   * delegates each of its operations to the appropriate methods of this {@code
   * ForwardingSortedMultiset}.
   *
   * <p>In many cases, you may wish to override {@link #descendingMultiset()} to return an instance
   * of a subclass of {@code StandardDescendingMultiset}.
   */
  protected abstract class StandardDescendingMultiset extends DescendingMultiset<E> {
    /** Constructor for use by subclasses. */
    public StandardDescendingMultiset() {}

    @Override
    SortedMultiset<E> forwardMultiset() {
      return ForwardingSortedMultiset.this;
    }
  }

  @Override
  public Entry<E> firstEntry() {
    return delegate().firstEntry();
  }

  /**
   * A sensible definition of {@link #firstEntry()} in terms of {@code entrySet().iterator()}.
   *
   * <p>If you override {@link #entrySet()}, you may wish to override {@link #firstEntry()} to
   * forward to this implementation.
   */
  protected Entry<E> standardFirstEntry() {
    Iterator<Entry<E>> entryIterator = entrySet().iterator();
    if (!entryIterator.hasNext()) {
      return null;
    }
    Entry<E> entry = entryIterator.next();
    return Multisets.immutableEntry(entry.getElement(), entry.getCount());
  }

  @Override
  public Entry<E> lastEntry() {
    return delegate().lastEntry();
  }

  /**
   * A sensible definition of {@link #lastEntry()} in terms of {@code
   * descendingMultiset().entrySet().iterator()}.
   *
   * <p>If you override {@link #descendingMultiset} or {@link #entrySet()}, you may wish to override
   * {@link #firstEntry()} to forward to this implementation.
   */
  protected Entry<E> standardLastEntry() {
    Iterator<Entry<E>> entryIterator = descendingMultiset().entrySet().iterator();
    if (!entryIterator.hasNext()) {
      return null;
    }
    Entry<E> entry = entryIterator.next();
    return Multisets.immutableEntry(entry.getElement(), entry.getCount());
  }

  @Override
  public Entry<E> pollFirstEntry() {
    return delegate().pollFirstEntry();
  }

  /**
   * A sensible definition of {@link #pollFirstEntry()} in terms of {@code entrySet().iterator()}.
   *
   * <p>If you override {@link #entrySet()}, you may wish to override {@link #pollFirstEntry()} to
   * forward to this implementation.
   */
  protected Entry<E> standardPollFirstEntry() {
    Iterator<Entry<E>> entryIterator = entrySet().iterator();
    if (!entryIterator.hasNext()) {
      return null;
    }
    Entry<E> entry = entryIterator.next();
    entry = Multisets.immutableEntry(entry.getElement(), entry.getCount());
    entryIterator.remove();
    return entry;
  }

  @Override
  public Entry<E> pollLastEntry() {
    return delegate().pollLastEntry();
  }

  /**
   * A sensible definition of {@link #pollLastEntry()} in terms of {@code
   * descendingMultiset().entrySet().iterator()}.
   *
   * <p>If you override {@link #descendingMultiset()} or {@link #entrySet()}, you may wish to
   * override {@link #pollLastEntry()} to forward to this implementation.
   */
  protected Entry<E> standardPollLastEntry() {
    Iterator<Entry<E>> entryIterator = descendingMultiset().entrySet().iterator();
    if (!entryIterator.hasNext()) {
      return null;
    }
    Entry<E> entry = entryIterator.next();
    entry = Multisets.immutableEntry(entry.getElement(), entry.getCount());
    entryIterator.remove();
    return entry;
  }

  @Override
  public SortedMultiset<E> headMultiset(E upperBound, BoundType boundType) {
    return delegate().headMultiset(upperBound, boundType);
  }

  @Override
  public SortedMultiset<E> subMultiset(
      E lowerBound, BoundType lowerBoundType, E upperBound, BoundType upperBoundType) {
    return delegate().subMultiset(lowerBound, lowerBoundType, upperBound, upperBoundType);
  }

  /**
   * A sensible definition of {@link #subMultiset(Object, BoundType, Object, BoundType)} in terms of
   * {@link #headMultiset(Object, BoundType) headMultiset} and {@link #tailMultiset(Object,
   * BoundType) tailMultiset}.
   *
   * <p>If you override either of these methods, you may wish to override {@link
   * #subMultiset(Object, BoundType, Object, BoundType)} to forward to this implementation.
   */
  protected SortedMultiset<E> standardSubMultiset(
      E lowerBound, BoundType lowerBoundType, E upperBound, BoundType upperBoundType) {
    return tailMultiset(lowerBound, lowerBoundType).headMultiset(upperBound, upperBoundType);
  }

  @Override
  public SortedMultiset<E> tailMultiset(E lowerBound, BoundType boundType) {
    return delegate().tailMultiset(lowerBound, boundType);
  }
}
