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
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.util.ListIterator;

/**
 * A list iterator which forwards all its method calls to another list iterator. Subclasses should
 * override one or more methods to modify the behavior of the backing iterator as desired per the <a
 * href="http://en.wikipedia.org/wiki/Decorator_pattern">decorator pattern</a>.
 *
 * <p><b>{@code default} method warning:</b> This class forwards calls to <i>only some</i> {@code
 * default} methods. Specifically, it forwards calls only for methods that existed <a
 * href="https://docs.oracle.com/javase/7/docs/api/java/util/ListIterator.html">before {@code
 * default} methods were introduced</a>. For newer methods, like {@code forEachRemaining}, it
 * inherits their default implementations. When those implementations invoke methods, they invoke
 * methods on the {@code ForwardingListIterator}.
 *
 * @author Mike Bostock
 * @since 2.0
 */
@GwtCompatible
public abstract class ForwardingListIterator<E> extends ForwardingIterator<E>
    implements ListIterator<E> {

  /** Constructor for use by subclasses. */
  protected ForwardingListIterator() {}

  @Override
  protected abstract ListIterator<E> delegate();

  @Override
  public void add(E element) {
    delegate().add(element);
  }

  @Override
  public boolean hasPrevious() {
    return delegate().hasPrevious();
  }

  @Override
  public int nextIndex() {
    return delegate().nextIndex();
  }

  @CanIgnoreReturnValue
  @Override
  public E previous() {
    return delegate().previous();
  }

  @Override
  public int previousIndex() {
    return delegate().previousIndex();
  }

  @Override
  public void set(E element) {
    delegate().set(element);
  }
}
