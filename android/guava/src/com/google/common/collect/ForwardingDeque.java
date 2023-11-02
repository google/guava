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
import com.google.common.annotations.J2ktIncompatible;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.util.Deque;
import java.util.Iterator;
import javax.annotation.CheckForNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * A deque which forwards all its method calls to another deque. Subclasses should override one or
 * more methods to modify the behavior of the backing deque as desired per the <a
 * href="http://en.wikipedia.org/wiki/Decorator_pattern">decorator pattern</a>.
 *
 * <p><b>Warning:</b> The methods of {@code ForwardingDeque} forward <b>indiscriminately</b> to the
 * methods of the delegate. For example, overriding {@link #add} alone <b>will not</b> change the
 * behavior of {@link #offer} which can lead to unexpected behavior. In this case, you should
 * override {@code offer} as well.
 *
 * <p><b>{@code default} method warning:</b> This class does <i>not</i> forward calls to {@code
 * default} methods. Instead, it inherits their default implementations. When those implementations
 * invoke methods, they invoke methods on the {@code ForwardingDeque}.
 *
 * @author Kurt Alfred Kluever
 * @since 12.0
 */
@J2ktIncompatible
@GwtIncompatible
@ElementTypesAreNonnullByDefault
public abstract class ForwardingDeque<E extends @Nullable Object> extends ForwardingQueue<E>
    implements Deque<E> {

  /** Constructor for use by subclasses. */
  protected ForwardingDeque() {}

  @Override
  protected abstract Deque<E> delegate();

  @Override
  public void addFirst(@ParametricNullness E e) {
    delegate().addFirst(e);
  }

  @Override
  public void addLast(@ParametricNullness E e) {
    delegate().addLast(e);
  }

  @Override
  public Iterator<E> descendingIterator() {
    return delegate().descendingIterator();
  }

  @Override
  @ParametricNullness
  public E getFirst() {
    return delegate().getFirst();
  }

  @Override
  @ParametricNullness
  public E getLast() {
    return delegate().getLast();
  }

  @CanIgnoreReturnValue // TODO(cpovirk): Consider removing this?
  @Override
  public boolean offerFirst(@ParametricNullness E e) {
    return delegate().offerFirst(e);
  }

  @CanIgnoreReturnValue // TODO(cpovirk): Consider removing this?
  @Override
  public boolean offerLast(@ParametricNullness E e) {
    return delegate().offerLast(e);
  }

  @Override
  @CheckForNull
  public E peekFirst() {
    return delegate().peekFirst();
  }

  @Override
  @CheckForNull
  public E peekLast() {
    return delegate().peekLast();
  }

  @CanIgnoreReturnValue // TODO(cpovirk): Consider removing this?
  @Override
  @CheckForNull
  public E pollFirst() {
    return delegate().pollFirst();
  }

  @CanIgnoreReturnValue // TODO(cpovirk): Consider removing this?
  @Override
  @CheckForNull
  public E pollLast() {
    return delegate().pollLast();
  }

  @CanIgnoreReturnValue
  @Override
  @ParametricNullness
  public E pop() {
    return delegate().pop();
  }

  @Override
  public void push(@ParametricNullness E e) {
    delegate().push(e);
  }

  @CanIgnoreReturnValue
  @Override
  @ParametricNullness
  public E removeFirst() {
    return delegate().removeFirst();
  }

  @CanIgnoreReturnValue
  @Override
  @ParametricNullness
  public E removeLast() {
    return delegate().removeLast();
  }

  @CanIgnoreReturnValue
  @Override
  public boolean removeFirstOccurrence(@CheckForNull Object o) {
    return delegate().removeFirstOccurrence(o);
  }

  @CanIgnoreReturnValue
  @Override
  public boolean removeLastOccurrence(@CheckForNull Object o) {
    return delegate().removeLastOccurrence(o);
  }
}
