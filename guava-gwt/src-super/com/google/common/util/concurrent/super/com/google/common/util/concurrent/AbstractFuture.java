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
import static com.google.common.base.Strings.isNullOrEmpty;
import static com.google.common.util.concurrent.Futures.getDone;
import static com.google.common.util.concurrent.MoreExecutors.directExecutor;

import com.google.common.util.concurrent.internal.InternalFutureFailureAccess;
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
import org.checkerframework.checker.nullness.qual.Nullable;

/** Emulation for AbstractFuture in GWT. */
@SuppressWarnings("nullness") // TODO(b/147136275): Remove once our checker understands & and |.
public abstract class AbstractFuture<V> extends InternalFutureFailureAccess
    implements ListenableFuture<V> {

  static final boolean GENERATE_CANCELLATION_CAUSES = false;

  /**
   * Tag interface marking trusted subclasses. This enables some optimizations. The implementation
   * of this interface must also be an AbstractFuture and must not override or expose for overriding
   * any of the public methods of ListenableFuture.
   */
  interface Trusted<V> extends ListenableFuture<V> {}

  abstract static class TrustedFuture<V> extends AbstractFuture<V> implements Trusted<V> {
    @Override
    public final V get() throws InterruptedException, ExecutionException {
      return super.get();
    }

    @Override
    public final V get(long timeout, TimeUnit unit)
        throws InterruptedException, ExecutionException, TimeoutException {
      return super.get(timeout, unit);
    }

    @Override
    public final boolean isDone() {
      return super.isDone();
    }

    @Override
    public final boolean isCancelled() {
      return super.isCancelled();
    }

    @Override
    public final void addListener(Runnable listener, Executor executor) {
      super.addListener(listener, executor);
    }

    @Override
    public final boolean cancel(boolean mayInterruptIfRunning) {
      return super.cancel(mayInterruptIfRunning);
    }
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

  @Override
  public boolean cancel(boolean mayInterruptIfRunning) {
    if (!state.permitsPublicUserToTransitionTo(State.CANCELLED)) {
      return false;
    }

    this.mayInterruptIfRunning = mayInterruptIfRunning;
    state = State.CANCELLED;
    notifyAndClearListeners();

    if (delegate != null) {
      // TODO(lukes): consider adding the StackOverflowError protection from the server version
      delegate.cancel(mayInterruptIfRunning);
    }

    return true;
  }

  protected void interruptTask() {}

  @Override
  public boolean isCancelled() {
    return state.isCancelled();
  }

  @Override
  public boolean isDone() {
    return state.isDone();
  }

  /*
   * ForwardingFluentFuture needs to override those methods, so they are not final.
   */
  @Override
  public V get() throws InterruptedException, ExecutionException {
    state.maybeThrowOnGet(throwable);
    return value;
  }

  @Override
  public V get(long timeout, TimeUnit unit)
      throws InterruptedException, ExecutionException, TimeoutException {
    checkNotNull(unit);
    return get();
  }

  @Override
  public void addListener(Runnable runnable, Executor executor) {
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
    // TODO(cpovirk): Should we do this at the end of the method, as in the server version?
    // TODO(cpovirk): Use maybePropagateCancellationTo?
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
    afterDone();
    // TODO(lukes): consider adding the StackOverflowError protection from the server version
    // TODO(cpovirk): consider clearing this.delegate
    for (Listener listener : listeners) {
      listener.execute();
    }
    listeners = null;
  }

  protected void afterDone() {}

  @Override
  protected final Throwable tryInternalFastPathGetFailure() {
    if (this instanceof Trusted) {
      return state == State.FAILURE ? throwable : null;
    }
    return null;
  }

  final Throwable trustedGetException() {
    checkState(state == State.FAILURE);
    return throwable;
  }

  final void maybePropagateCancellationTo(@Nullable Future<?> related) {
    if (related != null & isCancelled()) {
      related.cancel(wasInterrupted());
    }
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder().append(super.toString()).append("[status=");
    if (isCancelled()) {
      builder.append("CANCELLED");
    } else if (isDone()) {
      addDoneString(builder);
    } else {
      String pendingDescription;
      try {
        pendingDescription = pendingToString();
      } catch (RuntimeException e) {
        // Don't call getMessage or toString() on the exception, in case the exception thrown by the
        // subclass is implemented with bugs similar to the subclass.
        pendingDescription = "Exception thrown from implementation: " + e.getClass();
      }
      // The future may complete during or before the call to getPendingToString, so we use null
      // as a signal that we should try checking if the future is done again.
      if (!isNullOrEmpty(pendingDescription)) {
        builder.append("PENDING, info=[").append(pendingDescription).append("]");
      } else if (isDone()) {
        addDoneString(builder);
      } else {
        builder.append("PENDING");
      }
    }
    return builder.append("]").toString();
  }

  /**
   * Provide a human-readable explanation of why this future has not yet completed.
   *
   * @return null if an explanation cannot be provided because the future is done.
   */
  @Nullable
  String pendingToString() {
    if (state == State.DELEGATED) {
      return "setFuture=[" + delegate + "]";
    }
    return null;
  }

  private void addDoneString(StringBuilder builder) {
    try {
      V value = getDone(this);
      builder.append("SUCCESS, result=[").append(value).append("]");
    } catch (ExecutionException e) {
      builder.append("FAILURE, cause=[").append(e.getCause()).append("]");
    } catch (CancellationException e) {
      builder.append("CANCELLED");
    } catch (RuntimeException e) {
      builder.append("UNKNOWN, cause=[").append(e.getClass()).append(" thrown from get()]");
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
        log.log(
            Level.SEVERE,
            "RuntimeException while executing runnable " + command + " with executor " + executor,
            e);
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
        // don't copy the mayInterruptIfRunning bit, for consistency with the server, to ensure that
        // interruptTask() is called if and only if the bit is true and because we cannot infer the
        // interrupt status from non AbstractFuture futures.
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
