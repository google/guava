/*
 * Copyright (C) 2015 The Guava Authors
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

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.util.concurrent.Futures.getDone;
import static com.google.common.util.concurrent.MoreExecutors.directExecutor;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Nullable;

/**
 * Emulation for AbstractFuture in GWT.
 */
public abstract class AbstractFuture<V> implements ListenableFuture<V> {

  abstract static class TrustedFuture<V> extends AbstractFuture<V> {
    /*
     * We don't need to override any of methods that we override in the prod version (and in fact we
     * can't) because they are already final.
     */
  }

  private static final Logger log = Logger.getLogger(AbstractFuture.class.getName());

  private State state;
  private V value;
  private Future<? extends V> delegate;
  private Throwable throwable;
  private boolean mayInterruptIfRunning;
  private List<Listener> listeners;

  protected AbstractFuture() {
    state = State.PENDING;
    listeners = new ArrayList<Listener>();
  }

  /*
   * TODO(cpovirk): Consider making cancel() final (under GWT only, since we can't change the
   * server) by migrating our overrides to use afterDone().
   */
  @Override
  public boolean cancel(boolean mayInterruptIfRunning) {
    if (!state.permitsPublicUserToTransitionTo(State.CANCELLED)) {
      return false;
    }

    this.mayInterruptIfRunning = mayInterruptIfRunning;
    state = State.CANCELLED;
    notifyAndClearListeners();

    if (delegate != null) {
      delegate.cancel(mayInterruptIfRunning);
    }

    return true;
  }

  protected void interruptTask() {}

  @Override
  public final boolean isCancelled() {
    return state.isCancelled();
  }

  @Override
  public final boolean isDone() {
    return state.isDone();
  }

  /*
   * We let people override {@code get()} in the server version (though perhaps we shouldn't). Here,
   * we don't want that, and anyway, users can't, thanks to the package-private parameter.
   */
  @Override
  public final V get() throws InterruptedException, ExecutionException {
    state.maybeThrowOnGet(throwable);
    return value;
  }

  @Override
  public final V get(long timeout, TimeUnit unit)
      throws InterruptedException, ExecutionException, TimeoutException {
    checkNotNull(unit);
    return get();
  }

  @Override
  public final void addListener(Runnable runnable, Executor executor) {
    Listener listener = new Listener(runnable, executor);
    if (isDone()) {
      listener.execute();
    } else {
      listeners.add(listener);
    }
  }

  protected boolean setException(Throwable throwable) {
    checkNotNull(throwable);
    if (!state.permitsPublicUserToTransitionTo(State.FAILURE)) {
      return false;
    }

    forceSetException(throwable);
    return true;
  }

  private void forceSetException(Throwable throwable) {
    this.throwable = throwable;
    this.state = State.FAILURE;
    notifyAndClearListeners();
  }

  protected boolean set(V value) {
    if (!state.permitsPublicUserToTransitionTo(State.VALUE)) {
      return false;
    }

    forceSet(value);
    return true;
  }

  private void forceSet(V value) {
    this.value = value;
    this.state = State.VALUE;
    notifyAndClearListeners();
  }

  protected boolean setFuture(ListenableFuture<? extends V> future) {
    checkNotNull(future);

    // If this future is already cancelled, cancel the delegate.
    if (isCancelled()) {
      future.cancel(mayInterruptIfRunning);
    }

    if (!state.permitsPublicUserToTransitionTo(State.DELEGATED)) {
      return false;
    }

    state = State.DELEGATED;
    this.delegate = future;

    future.addListener(new SetFuture(future), directExecutor());
    return true;
  }

  protected final boolean wasInterrupted() {
    return mayInterruptIfRunning;
  }

  private void notifyAndClearListeners() {
    // TODO(cpovirk): consider clearing this.delegate
    for (Listener listener : listeners) {
      listener.execute();
    }
    listeners = null;
    afterDone();
  }

  protected void afterDone() {}

  final Throwable trustedGetException() {
    checkState(state == State.FAILURE);
    return throwable;
  }

  final void maybePropagateCancellation(@Nullable Future<?> related) {
    if (related != null & isCancelled()) {
      related.cancel(wasInterrupted());
    }
  }

  private enum State {
    PENDING {
      @Override
      boolean isDone() {
        return false;
      }

      @Override
      void maybeThrowOnGet(Throwable cause) throws ExecutionException {
        throw new IllegalStateException("Cannot get() on a pending future.");
      }

      @Override
      boolean permitsPublicUserToTransitionTo(State state) {
        return !state.equals(PENDING);
      }
    },
    DELEGATED {
      @Override
      boolean isDone() {
        return false;
      }

      @Override
      void maybeThrowOnGet(Throwable cause) throws ExecutionException {
        throw new IllegalStateException("Cannot get() on a pending future.");
      }

      boolean permitsPublicUserToTransitionTo(State state) {
        return state.equals(CANCELLED);
      }
    },
    VALUE,
    FAILURE {
      @Override
      void maybeThrowOnGet(Throwable cause) throws ExecutionException {
        throw new ExecutionException(cause);
      }
    },
    CANCELLED {
      @Override
      boolean isCancelled() {
        return true;
      }

      @Override
      void maybeThrowOnGet(Throwable cause) throws ExecutionException {
        // TODO(cpovirk): chain in a CancellationException created at the cancel() call?
        throw new CancellationException();
      }
    };

    boolean isDone() {
      return true;
    }

    boolean isCancelled() {
      return false;
    }

    void maybeThrowOnGet(Throwable cause) throws ExecutionException {}

    boolean permitsPublicUserToTransitionTo(State state) {
      return false;
    }
  }

  private static final class Listener {
    final Runnable command;
    final Executor executor;

    Listener(Runnable command, Executor executor) {
      this.command = checkNotNull(command);
      this.executor = checkNotNull(executor);
    }

    void execute() {
      try {
        executor.execute(command);
      } catch (RuntimeException e) {
        log.log(Level.SEVERE, "RuntimeException while executing runnable "
            + command + " with executor " + executor, e);
      }
    }
  }

  private final class SetFuture implements Runnable {
    final ListenableFuture<? extends V> delegate;

    SetFuture(ListenableFuture<? extends V> delegate) {
      this.delegate = delegate;
    }

    @Override
    public void run() {
      if (isCancelled()) {
        return;
      }

      if (delegate instanceof AbstractFuture) {
        AbstractFuture<? extends V> other = (AbstractFuture<? extends V>) delegate;
        value = other.value;
        throwable = other.throwable;
        mayInterruptIfRunning = other.mayInterruptIfRunning;
        state = other.state;

        notifyAndClearListeners();
        return;
      }

      /*
       * Almost everything in GWT is an AbstractFuture (which is as good as TrustedFuture under
       * GWT). But ImmediateFuture and UncheckedThrowingFuture aren't, so we still need this case.
       */
      try {
        forceSet(getDone(delegate));
      } catch (ExecutionException exception) {
        forceSetException(exception.getCause());
      } catch (CancellationException cancellation) {
        cancel(false);
      } catch (Throwable t) {
        forceSetException(t);
      }
    }
  }
}
