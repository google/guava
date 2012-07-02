/*
 * Copyright (C) 2009 The Guava Authors
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

import com.google.common.annotations.Beta;
import com.google.common.collect.Lists;
import com.google.common.collect.Queues;
import com.google.common.util.concurrent.Service.State; // javadoc needs this

import java.util.List;
import java.util.Queue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Nullable;
import javax.annotation.concurrent.GuardedBy;

/**
 * Base class for implementing services that can handle {@link #doStart} and
 * {@link #doStop} requests, responding to them with {@link #notifyStarted()}
 * and {@link #notifyStopped()} callbacks. Its subclasses must manage threads
 * manually; consider {@link AbstractExecutionThreadService} if you need only a
 * single execution thread.
 *
 * @author Jesse Wilson
 * @author Luke Sandberg
 * @since 1.0
 */
@Beta
public abstract class AbstractService implements Service {
  private static final Logger logger = Logger.getLogger(
      AbstractService.class.getName());
  private final ReentrantLock lock = new ReentrantLock();

  private final Transition startup = new Transition();
  private final Transition shutdown = new Transition();

  /**
   * The listeners to notify during a state transition.
   */
  @GuardedBy("lock")
  private final List<ListenerExecutorPair> listeners = Lists.newArrayList();

  /**
   * The queue of listeners that are waiting to be executed.
   *
   * <p>Enqueue operations should be protected by {@link #lock} while dequeue
   * operations should be protected by the implicit lock on this object. Dequeue
   * operations should be executed atomically with the execution of the
   * {@link Runnable} and additionally the {@link #lock} should not be held when
   * the listeners are being executed. Use {@link #executeListeners} for this
   * operation.  This is necessary to ensure that elements on the queue are
   * executed in the correct order.  Enqueue operations should be protected so
   * that listeners are added in the correct order. We use a concurrent queue
   * implementation so that enqueues can be executed concurrently with dequeues.
   */
  @GuardedBy("queuedListeners")
  private final Queue<Runnable> queuedListeners =
      Queues.newConcurrentLinkedQueue();

  /**
   * The exception that caused this service to fail.  This will be {@code null}
   * unless the service has failed.
   */
  @GuardedBy("lock")
  @Nullable
  private Throwable failure;

  /**
   * The internal state, which equals external state unless
   * shutdownWhenStartupFinishes is true.
   */
  @GuardedBy("lock")
  private State state = State.NEW;

  /**
   * If true, the user requested a shutdown while the service was still starting
   * up.
   */
  @GuardedBy("lock")
  private boolean shutdownWhenStartupFinishes = false;

  protected AbstractService() {
    // Add a listener to update the futures.  This needs to be added first so
    // that it is executed before the other listeners.  This way the other
    // listeners can access the completed futures.
    addListener(
        new Listener() {
          @Override public void starting() {}

          @Override public void running() {
            startup.set(State.RUNNING);
          }

          @Override public void stopping(State from) {
            if (from == State.STARTING) {
              startup.set(State.STOPPING);
            }
          }

          @Override public void terminated(State from) {
            if (from == State.NEW) {
              startup.set(State.TERMINATED);
            }
            shutdown.set(State.TERMINATED);
          }

          @Override public void failed(State from, Throwable failure) {
            switch (from) {
              case STARTING:
                startup.setException(failure);
                shutdown.setException(
                    new Exception("Service failed to start.", failure));
                break;
              case RUNNING:
                shutdown.setException(
                    new Exception("Service failed while running", failure));
                break;
              case STOPPING:
                shutdown.setException(failure);
                break;
              case TERMINATED:  /* fall-through */
              case FAILED:  /* fall-through */
              case NEW:  /* fall-through */
              default:
                throw new AssertionError("Unexpected from state: " + from);
            }
          }
        },
        MoreExecutors.sameThreadExecutor());
  }

  /**
   * This method is called by {@link #start} to initiate service startup. The
   * invocation of this method should cause a call to {@link #notifyStarted()},
   * either during this method's run, or after it has returned. If startup
   * fails, the invocation should cause a call to {@link
   * #notifyFailed(Throwable)} instead.
   *
   * <p>This method should return promptly; prefer to do work on a different
   * thread where it is convenient. It is invoked exactly once on service
   * startup, even when {@link #start} is called multiple times.
   */
  protected abstract void doStart();

  /**
   * This method should be used to initiate service shutdown. The invocation
   * of this method should cause a call to {@link #notifyStopped()}, either
   * during this method's run, or after it has returned. If shutdown fails, the
   * invocation should cause a call to {@link #notifyFailed(Throwable)} instead.
   *
   * <p>This method should return promptly; prefer to do work on a different
   * thread where it is convenient. It is invoked exactly once on service
   * shutdown, even when {@link #stop} is called multiple times.
   */
  protected abstract void doStop();

  @Override
  public final ListenableFuture<State> start() {
    lock.lock();
    try {
      if (state == State.NEW) {
        state = State.STARTING;
        starting();
        doStart();
      }
    } catch (Throwable startupFailure) {
      notifyFailed(startupFailure);
    } finally {
      lock.unlock();
      executeListeners();
    }

    return startup;
  }

  @Override
  public final ListenableFuture<State> stop() {
    lock.lock();
    try {
      switch (state) {
        case NEW:
          state = State.TERMINATED;
          terminated(State.NEW);
          break;
        case STARTING:
          shutdownWhenStartupFinishes = true;
          stopping(State.STARTING);
          break;
        case RUNNING:
          state = State.STOPPING;
          stopping(State.RUNNING);
          doStop();
          break;
        case STOPPING:
        case TERMINATED:
        case FAILED:
          // do nothing
          break;
        default:
          throw new AssertionError("Unexpected state: " + state);
      }
    } catch (Throwable shutdownFailure) {
      notifyFailed(shutdownFailure);
    } finally {
      lock.unlock();
      executeListeners();
    }

    return shutdown;
  }

  @Override
  public State startAndWait() {
    return Futures.getUnchecked(start());
  }

  @Override
  public State stopAndWait() {
    return Futures.getUnchecked(stop());
  }

  /**
   * Implementing classes should invoke this method once their service has
   * started. It will cause the service to transition from {@link
   * State#STARTING} to {@link State#RUNNING}.
   *
   * @throws IllegalStateException if the service is not
   *     {@link State#STARTING}.
   */
  protected final void notifyStarted() {
    lock.lock();
    try {
      if (state != State.STARTING) {
        IllegalStateException failure = new IllegalStateException(
            "Cannot notifyStarted() when the service is " + state);
        notifyFailed(failure);
        throw failure;
      }

      if (shutdownWhenStartupFinishes) {
        state = State.STOPPING;
        // We don't call listeners here because we already did that when we set the
        // shutdownWhenStartupFinishes flag.
        doStop();
      } else {
        state = State.RUNNING;
        running();
      }
    } finally {
      lock.unlock();
      executeListeners();
    }
  }

  /**
   * Implementing classes should invoke this method once their service has
   * stopped. It will cause the service to transition from {@link
   * State#STOPPING} to {@link State#TERMINATED}.
   *
   * @throws IllegalStateException if the service is neither {@link
   *     State#STOPPING} nor {@link State#RUNNING}.
   */
  protected final void notifyStopped() {
    lock.lock();
    try {
      if (state != State.STOPPING && state != State.RUNNING) {
        IllegalStateException failure = new IllegalStateException(
            "Cannot notifyStopped() when the service is " + state);
        notifyFailed(failure);
        throw failure;
      }
      State previous = state;
      state = State.TERMINATED;
      terminated(previous);
    } finally {
      lock.unlock();
      executeListeners();
    }
  }

  /**
   * Invoke this method to transition the service to the
   * {@link State#FAILED}. The service will <b>not be stopped</b> if it
   * is running. Invoke this method when a service has failed critically or
   * otherwise cannot be started nor stopped.
   */
  protected final void notifyFailed(Throwable cause) {
    checkNotNull(cause);

    lock.lock();
    try {
      switch (state) {
        case NEW:  /* fall-through */
        case TERMINATED:
          throw new IllegalStateException(
              "Failed while in state:" + state, cause);
        case RUNNING:  /* fall-through */
        case STARTING:  /* fall-through */
        case STOPPING:
          State previous = state;
          failure = cause;
          state = State.FAILED;
          failed(previous, cause);
          break;
        case FAILED:
          // Do nothing
          break;
        default:
          throw new AssertionError("Unexpected state: " + state);
      }
    } finally {
      lock.unlock();
      executeListeners();
    }
  }

  @Override
  public final boolean isRunning() {
    return state() == State.RUNNING;
  }

  @Override
  public final State state() {
    lock.lock();
    try {
      if (shutdownWhenStartupFinishes && state == State.STARTING) {
        return State.STOPPING;
      } else {
        return state;
      }
    } finally {
      lock.unlock();
    }
  }

  @Override
  public final void addListener(Listener listener, Executor executor) {
    checkNotNull(listener, "listener");
    checkNotNull(executor, "executor");
    lock.lock();
    try {
      if (state != State.TERMINATED && state != State.FAILED) {
        listeners.add(new ListenerExecutorPair(listener, executor));
      }
    } finally {
      lock.unlock();
    }
  }

  @Override public String toString() {
    return getClass().getSimpleName() + " [" + state() + "]";
  }

  /**
   * A change from one service state to another, plus the result of the change.
   */
  private class Transition extends AbstractFuture<State> {
    @Override
    public State get(long timeout, TimeUnit unit)
        throws InterruptedException, TimeoutException, ExecutionException {
      try {
        return super.get(timeout, unit);
      } catch (TimeoutException e) {
        throw new TimeoutException(AbstractService.this.toString());
      }
    }
  }

  /**
   * Attempts to execute all the listeners in {@link #queuedListeners} while not holding the
   * {@link #lock}.
   */
  private void executeListeners() {
    if (!lock.isHeldByCurrentThread()) {
      synchronized (queuedListeners) {
        Runnable listener;
        while ((listener = queuedListeners.poll()) != null) {
          listener.run();
        }
      }
    }
  }

  @GuardedBy("lock")
  private void starting() {
    for (final ListenerExecutorPair pair : listeners) {
      queuedListeners.add(new Runnable() {
        @Override public void run() {
          pair.execute(new Runnable() {
            @Override public void run() {
              pair.listener.starting();
            }
          });
        }
      });
    }
  }

  @GuardedBy("lock")
  private void running() {
    for (final ListenerExecutorPair pair : listeners) {
      queuedListeners.add(new Runnable() {
        @Override public void run() {
          pair.execute(new Runnable() {
            @Override public void run() {
              pair.listener.running();
            }
          });
        }
      });
    }
  }

  @GuardedBy("lock")
  private void stopping(final State from) {
    for (final ListenerExecutorPair pair : listeners) {
      queuedListeners.add(new Runnable() {
        @Override public void run() {
          pair.execute(new Runnable() {
            @Override public void run() {
              pair.listener.stopping(from);
            }
          });
        }
      });
    }
  }

  @GuardedBy("lock")
  private void terminated(final State from) {
    for (final ListenerExecutorPair pair : listeners) {
      queuedListeners.add(new Runnable() {
        @Override public void run() {
          pair.execute(new Runnable() {
            @Override public void run() {
              pair.listener.terminated(from);
            }
          });
        }
      });
    }
    // There are no more state transitions so we can clear this out.
    listeners.clear();
  }

  @GuardedBy("lock")
  private void failed(final State from, final Throwable cause) {
    for (final ListenerExecutorPair pair : listeners) {
      queuedListeners.add(new Runnable() {
        @Override public void run() {
          pair.execute(new Runnable() {
            @Override public void run() {
              pair.listener.failed(from, cause);
            }
          });
        }
      });
    }
    // There are no more state transitions so we can clear this out.
    listeners.clear();
  }

  /**
   * A {@link Service.Listener} that schedules the callbacks of the delegate listener on an
   * {@link Executor}.
   */
  private static class ListenerExecutorPair {
    final Listener listener;
    final Executor executor;

    ListenerExecutorPair(Listener listener, Executor executor) {
      this.listener = listener;
      this.executor = executor;
    }

    /**
     * Executes the given {@link Runnable} on {@link #executor} logging and swallowing all
     * exceptions
     */
    void execute(Runnable runnable) {
      try {
        executor.execute(runnable);
      } catch (Exception e) {
        logger.log(Level.SEVERE, "Exception while executing listener " + listener
            + " with executor " + executor, e);
      }
    }
  }
}
