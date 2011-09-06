/*
 * Copyright (C) 2008 The Guava Authors
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

package com.google.common.util.concurrent;

import com.google.common.annotations.Beta;

import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * A delegating wrapper around a {@link ListenableFuture} that adds support for
 * the {@link #checkedGet()} and {@link #checkedGet(long, TimeUnit)} methods.
 *
 * @author Sven Mawson
 * @since 1.0
 */
@Beta
public abstract class AbstractCheckedFuture<V, X extends Exception>
    extends ForwardingListenableFuture.SimpleForwardingListenableFuture<V>
    implements CheckedFuture<V, X> {
  /**
   * Constructs an {@code AbstractCheckedFuture} that wraps a delegate.
   */
  protected AbstractCheckedFuture(ListenableFuture<V> delegate) {
    super(delegate);
  }

  /**
   * Translates from an {@link InterruptedException},
   * {@link CancellationException} or {@link ExecutionException} thrown by
   * {@code get} to an exception of type {@code X} to be thrown by
   * {@code checkedGet}. Subclasses must implement this method.
   *
   * <p>If {@code e} is an {@code InterruptedException}, the calling
   * {@code checkedGet} method has already restored the interrupt after catching
   * the exception. If an implementation of {@link #mapException(Exception)}
   * wishes to swallow the interrupt, it can do so by calling
   * {@link Thread#interrupted()}.
   *
   * <p>Subclasses may choose to throw, rather than return, a subclass of
   * {@code RuntimeException} to allow creating a CheckedFuture that throws
   * both checked and unchecked exceptions.
   */
  protected abstract X mapException(Exception e);

  /**
   * {@inheritDoc}
   *
   * <p>This implementation calls {@link #get()} and maps that method's standard
   * exceptions to instances of type {@code X} using {@link #mapException}.
   *
   * <p>In addition, if {@code get} throws an {@link InterruptedException}, this
   * implementation will set the current thread's interrupt status before
   * calling {@code mapException}.
   *
   * @throws X if {@link #get()} throws an {@link InterruptedException},
   *         {@link CancellationException}, or {@link ExecutionException}
   */
  @Override
  public V checkedGet() throws X {
    try {
      return get();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw mapException(e);
    } catch (CancellationException e) {
      throw mapException(e);
    } catch (ExecutionException e) {
      throw mapException(e);
    }
  }

  /**
   * {@inheritDoc}
   *
   * <p>This implementation calls {@link #get(long, TimeUnit)} and maps that
   * method's standard exceptions (excluding {@link TimeoutException}, which is
   * propagated) to instances of type {@code X} using {@link #mapException}.
   *
   * <p>In addition, if {@code get} throws an {@link InterruptedException}, this
   * implementation will set the current thread's interrupt status before
   * calling {@code mapException}.
   *
   * @throws X if {@link #get()} throws an {@link InterruptedException},
   *         {@link CancellationException}, or {@link ExecutionException}
   * @throws TimeoutException {@inheritDoc}
   */
  @Override
  public V checkedGet(long timeout, TimeUnit unit) throws TimeoutException, X {
    try {
      return get(timeout, unit);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw mapException(e);
    } catch (CancellationException e) {
      throw mapException(e);
    } catch (ExecutionException e) {
      throw mapException(e);
    }
  }
}
