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

import java.util.Deque;
import java.util.Iterator;

/**
 * A deque which forwards all its method calls to another deque. Subclasses
 * should override one or more methods to modify the behavior of the backing
 * deque as desired per the <a
 * href="http://en.wikipedia.org/wiki/Decorator_pattern">decorator pattern</a>.
 *
 * <p><b>Warning:</b> The methods of {@code ForwardingDeque} forward
 * <b>indiscriminately</b> to the methods of the delegate. For example,
 * overriding {@link #add} alone <b>will not</b> change the behavior of {@link
 * #offer} which can lead to unexpected behavior. In this case, you should
 * override {@code offer} as well.
 *
 * @author Kurt Alfred Kluever
 * @since 12.0
 */
public abstract class ForwardingDeque<E> extends ForwardingQueue<E>
    implements Deque<E> {

  /** Constructor for use by subclasses. */
  protected ForwardingDeque() {}

  @Override protected abstract Deque<E> delegate();

  @Override
  public void addFirst(E e) {
    delegate().addFirst(e);
  }

  @Override
  public void addLast(E e) {
    delegate().addLast(e);
  }

  @Override
  public Iterator<E> descendingIterator() {
    return delegate().descendingIterator();
  }

  @Override
  public E getFirst() {
    return delegate().getFirst();
  }

  @Override
  public E getLast() {
    return delegate().getLast();
  }

  @Override
  public boolean offerFirst(E e) {
    return delegate().offerFirst(e);
  }

  @Override
  public boolean offerLast(E e) {
    return delegate().offerLast(e);
  }

  @Override
  public E peekFirst() {
    return delegate().peekFirst();
  }

  @Override
  public E peekLast() {
    return delegate().peekLast();
  }

  @Override
  public E pollFirst() {
    return delegate().pollFirst();
  }

  @Override
  public E pollLast() {
    return delegate().pollLast();
  }

  @Override
  public E pop() {
    return delegate().pop();
  }

  @Override
  public void push(E e) {
    delegate().push(e);
  }

  @Override
  public E removeFirst() {
    return delegate().removeFirst();
  }

  @Override
  public E removeLast() {
    return delegate().removeLast();
  }

  @Override
  public boolean removeFirstOccurrence(Object o) {
    return delegate().removeFirstOccurrence(o);
  }

  @Override
  public boolean removeLastOccurrence(Object o) {
    return delegate().removeLastOccurrence(o);
  }
}
