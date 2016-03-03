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

import com.google.common.annotations.GwtCompatible;
import com.google.common.base.Objects;
import com.google.errorprone.annotations.CanIgnoreReturnValue;

import java.util.Collection;
import java.util.Iterator;

import javax.annotation.Nullable;

/**
 * A collection which forwards all its method calls to another collection.
 * Subclasses should override one or more methods to modify the behavior of the
 * backing collection as desired per the <a
 * href="http://en.wikipedia.org/wiki/Decorator_pattern">decorator pattern</a>.
 *
 * <p><b>Warning:</b> The methods of {@code ForwardingCollection} forward
 * <b>indiscriminately</b> to the methods of the delegate. For example,
 * overriding {@link #add} alone <b>will not</b> change the behavior of {@link
 * #addAll}, which can lead to unexpected behavior. In this case, you should
 * override {@code addAll} as well, either providing your own implementation, or
 * delegating to the provided {@code standardAddAll} method.
 *
 * <p>The {@code standard} methods are not guaranteed to be thread-safe, even
 * when all of the methods that they depend on are thread-safe.
 *
 * @author Kevin Bourrillion
 * @author Louis Wasserman
 * @since 2.0
 */
@GwtCompatible
public abstract class ForwardingCollection<E> extends ForwardingObject implements Collection<E> {
  // TODO(lowasser): identify places where thread safety is actually lost

  /** Constructor for use by subclasses. */
  protected ForwardingCollection() {}

  @Override
  protected abstract Collection<E> delegate();

  @Override
  public Iterator<E> iterator() {
    return delegate().iterator();
  }

  @Override
  public int size() {
    return delegate().size();
  }

  @CanIgnoreReturnValue
  @Override
  public boolean removeAll(Collection<?> collection) {
    return delegate().removeAll(collection);
  }

  @Override
  public boolean isEmpty() {
    return delegate().isEmpty();
  }

  @Override
  public boolean contains(Object object) {
    return delegate().contains(object);
  }

  @CanIgnoreReturnValue
  @Override
  public boolean add(E element) {
    return delegate().add(element);
  }

  @CanIgnoreReturnValue
  @Override
  public boolean remove(Object object) {
    return delegate().remove(object);
  }

  @Override
  public boolean containsAll(Collection<?> collection) {
    return delegate().containsAll(collection);
  }

  @CanIgnoreReturnValue
  @Override
  public boolean addAll(Collection<? extends E> collection) {
    return delegate().addAll(collection);
  }

  @CanIgnoreReturnValue
  @Override
  public boolean retainAll(Collection<?> collection) {
    return delegate().retainAll(collection);
  }

  @Override
  public void clear() {
    delegate().clear();
  }

  @Override
  public Object[] toArray() {
    return delegate().toArray();
  }

  @CanIgnoreReturnValue
  @Override
  public <T> T[] toArray(T[] array) {
    return delegate().toArray(array);
  }

  /**
   * A sensible definition of {@link #contains} in terms of {@link #iterator}.
   * If you override {@link #iterator}, you may wish to override {@link
   * #contains} to forward to this implementation.
   *
   * @since 7.0
   */
  protected boolean standardContains(@Nullable Object object) {
    return Iterators.contains(iterator(), object);
  }

  /**
   * A sensible definition of {@link #containsAll} in terms of {@link #contains}
   * . If you override {@link #contains}, you may wish to override {@link
   * #containsAll} to forward to this implementation.
   *
   * @since 7.0
   */
  protected boolean standardContainsAll(Collection<?> collection) {
    return Collections2.containsAllImpl(this, collection);
  }

  /**
   * A sensible definition of {@link #addAll} in terms of {@link #add}. If you
   * override {@link #add}, you may wish to override {@link #addAll} to forward
   * to this implementation.
   *
   * @since 7.0
   */
  protected boolean standardAddAll(Collection<? extends E> collection) {
    return Iterators.addAll(this, collection.iterator());
  }

  /**
   * A sensible definition of {@link #remove} in terms of {@link #iterator},
   * using the iterator's {@code remove} method. If you override {@link
   * #iterator}, you may wish to override {@link #remove} to forward to this
   * implementation.
   *
   * @since 7.0
   */
  protected boolean standardRemove(@Nullable Object object) {
    Iterator<E> iterator = iterator();
    while (iterator.hasNext()) {
      if (Objects.equal(iterator.next(), object)) {
        iterator.remove();
        return true;
      }
    }
    return false;
  }

  /**
   * A sensible definition of {@link #removeAll} in terms of {@link #iterator},
   * using the iterator's {@code remove} method. If you override {@link
   * #iterator}, you may wish to override {@link #removeAll} to forward to this
   * implementation.
   *
   * @since 7.0
   */
  protected boolean standardRemoveAll(Collection<?> collection) {
    return Iterators.removeAll(iterator(), collection);
  }

  /**
   * A sensible definition of {@link #retainAll} in terms of {@link #iterator},
   * using the iterator's {@code remove} method. If you override {@link
   * #iterator}, you may wish to override {@link #retainAll} to forward to this
   * implementation.
   *
   * @since 7.0
   */
  protected boolean standardRetainAll(Collection<?> collection) {
    return Iterators.retainAll(iterator(), collection);
  }

  /**
   * A sensible definition of {@link #clear} in terms of {@link #iterator},
   * using the iterator's {@code remove} method. If you override {@link
   * #iterator}, you may wish to override {@link #clear} to forward to this
   * implementation.
   *
   * @since 7.0
   */
  protected void standardClear() {
    Iterators.clear(iterator());
  }

  /**
   * A sensible definition of {@link #isEmpty} as {@code !iterator().hasNext}.
   * If you override {@link #isEmpty}, you may wish to override {@link #isEmpty}
   * to forward to this implementation. Alternately, it may be more efficient to
   * implement {@code isEmpty} as {@code size() == 0}.
   *
   * @since 7.0
   */
  protected boolean standardIsEmpty() {
    return !iterator().hasNext();
  }

  /**
   * A sensible definition of {@link #toString} in terms of {@link #iterator}.
   * If you override {@link #iterator}, you may wish to override {@link
   * #toString} to forward to this implementation.
   *
   * @since 7.0
   */
  protected String standardToString() {
    return Collections2.toStringImpl(this);
  }

  /**
   * A sensible definition of {@link #toArray()} in terms of {@link
   * #toArray(Object[])}. If you override {@link #toArray(Object[])}, you may
   * wish to override {@link #toArray} to forward to this implementation.
   *
   * @since 7.0
   */
  protected Object[] standardToArray() {
    Object[] newArray = new Object[size()];
    return toArray(newArray);
  }

  /**
   * A sensible definition of {@link #toArray(Object[])} in terms of {@link
   * #size} and {@link #iterator}. If you override either of these methods, you
   * may wish to override {@link #toArray} to forward to this implementation.
   *
   * @since 7.0
   */
  protected <T> T[] standardToArray(T[] array) {
    return ObjectArrays.toArrayImpl(this, array);
  }
}
