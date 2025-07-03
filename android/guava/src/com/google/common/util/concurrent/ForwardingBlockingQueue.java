/*
 * Copyright (C) 2010 The Guava Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.common.util.concurrent;

import com.google.common.annotations.GwtIncompatible;
import com.google.common.annotations.J2ktIncompatible;
import com.google.common.collect.ForwardingQueue;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.util.Collection;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import org.jspecify.annotations.Nullable;

/**
 * A {@link BlockingQueue} which forwards all its method calls to another {@link BlockingQueue}.
 * Subclasses should override one or more methods to modify the behavior of the backing collection
 * as desired per the <a href="http://en.wikipedia.org/wiki/Decorator_pattern">decorator
 * pattern</a>.
 *
 * <p><b>{@code default} method warning:</b> This class does <i>not</i> forward calls to {@code
 * default} methods. Instead, it inherits their default implementations. When those implementations
 * invoke methods, they invoke methods on the {@code ForwardingBlockingQueue}.
 *
 * @author Raimundo Mirisola
 * @param <E> the type of elements held in this collection
 * @since 4.0
 */
@J2ktIncompatible
@GwtIncompatible
public abstract class ForwardingBlockingQueue<E> extends ForwardingQueue<E>
    implements BlockingQueue<E> {

  /** Constructor for use by subclasses. */
  protected ForwardingBlockingQueue() {}

  @Override
  protected abstract BlockingQueue<E> delegate();

  @CanIgnoreReturnValue
  @Override
  public int drainTo(Collection<? super E> c, int maxElements) {
    return delegate().drainTo(c, maxElements);
  }

  @CanIgnoreReturnValue
  @Override
  public int drainTo(Collection<? super E> c) {
    return delegate().drainTo(c);
  }

  @CanIgnoreReturnValue // TODO(kak): consider removing this
  @Override
  public boolean offer(E e, long timeout, TimeUnit unit) throws InterruptedException {
    return delegate().offer(e, timeout, unit);
  }

  @CanIgnoreReturnValue // TODO(kak): consider removing this
  @Override
  public @Nullable E poll(long timeout, TimeUnit unit) throws InterruptedException {
    return delegate().poll(timeout, unit);
  }

  @Override
  public void put(E e) throws InterruptedException {
    delegate().put(e);
  }

  @Override
  public int remainingCapacity() {
    return delegate().remainingCapacity();
  }

  @CanIgnoreReturnValue // TODO(kak): consider removing this
  @Override
  public E take() throws InterruptedException {
    return delegate().take();
  }
}
