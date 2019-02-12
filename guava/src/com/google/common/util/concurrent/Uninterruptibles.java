/*
 * Copyright (C) 2011 The Guava Authors
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

import static java.util.concurrent.TimeUnit.NANOSECONDS;

import com.google.common.annotations.Beta;
import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.base.Preconditions;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.Condition;

/**
 * Utilities for treating interruptible operations as uninterruptible. In all cases, if a thread is
 * interrupted during such a call, the call continues to block until the result is available or the
 * timeout elapses, and only then re-interrupts the thread.
 *
 * @author Anthony Zana
 * @since 10.0
 */
@Beta
@GwtCompatible(emulated = true)
public final class Uninterruptibles {

  /**
   * Shared logic
   * 
   * @param <T> type on which to perform the blocking operation
   * @param <R> return type
   */
  static abstract class Interruptible<T,R> {
    // only need to override those which are used
    R await(T target) throws InterruptedException {
      throw new UnsupportedOperationException();
    }
    R await(T target, Object arg) throws InterruptedException {
      return await(target);
    }
    R await(T target, int count, long timeout, TimeUnit unit) throws InterruptedException {
      throw new UnsupportedOperationException();
    }

    final R awaitUninterruptibly(T target) {
      return awaitUninterruptibly(target, null);
    }

    final R awaitUninterruptibly(T target, Object arg) {
      boolean interrupted = false;
      try {
        while (true) {
          try {
            return await(target, arg);
          } catch (InterruptedException e) {
            interrupted = true;
          }
        }
      } finally {
        if (interrupted) {
          Thread.currentThread().interrupt();
        }
      }
    }

    final R awaitUninterruptibly(T target, long timeout, TimeUnit unit) {
      return awaitUninterruptibly(target, 1, timeout, unit);
    }

    final R awaitUninterruptibly(T target, int count, long timeout, TimeUnit unit) {
      boolean interrupted = false;
      try {
        long remainingNanos = unit.toNanos(timeout);
        long end = System.nanoTime() + remainingNanos;

        while (true) {
          try {
            return await(target, count, remainingNanos, NANOSECONDS);
          } catch (InterruptedException e) {
            interrupted = true;
            remainingNanos = end - System.nanoTime();
          }
        }
      } finally {
        if (interrupted) {
          Thread.currentThread().interrupt();
        }
      }
    }
  }

  /** Invokes {@code latch.}{@link CountDownLatch#await() await()} uninterruptibly. */
  @GwtIncompatible // concurrency
  public static void awaitUninterruptibly(CountDownLatch latch) {
    LATCH.awaitUninterruptibly(latch);
  }

  /**
   * Invokes {@code latch.}{@link CountDownLatch#await(long, TimeUnit) await(timeout, unit)}
   * uninterruptibly.
   */
  @CanIgnoreReturnValue // TODO(cpovirk): Consider being more strict.
  @GwtIncompatible // concurrency
  @SuppressWarnings("GoodTime") // should accept a java.time.Duration
  public static boolean awaitUninterruptibly(CountDownLatch latch, long timeout, TimeUnit unit) {
    return LATCH.awaitUninterruptibly(latch, timeout, unit);
  }

  private static final Interruptible<CountDownLatch,Boolean> LATCH
      = new Interruptible<CountDownLatch,Boolean>() {
    @Override Boolean await(CountDownLatch latch) throws InterruptedException {
      latch.await();
      return Boolean.TRUE;
    }
    @Override Boolean await(CountDownLatch latch, int unused,
        long timeout, TimeUnit unit) throws InterruptedException {
      return latch.await(timeout, unit);
    }
  };

  /**
   * Invokes {@code condition.}{@link Condition#await(long, TimeUnit) await(timeout, unit)}
   * uninterruptibly.
   *
   * @since 23.6
   */
  @GwtIncompatible // concurrency
  @SuppressWarnings("GoodTime") // should accept a java.time.Duration
  public static boolean awaitUninterruptibly(Condition condition, long timeout, TimeUnit unit) {
    return CONDITION.awaitUninterruptibly(condition, timeout, unit);
  }

  private static final Interruptible<Condition,Boolean> CONDITION
      = new Interruptible<Condition,Boolean>() {
    @Override Boolean await(Condition condition, int unused,
        long timeout, TimeUnit unit) throws InterruptedException {
      return condition.await(timeout, unit);
    }
  };

  /** Invokes {@code toJoin.}{@link Thread#join() join()} uninterruptibly. */
  @GwtIncompatible // concurrency
  public static void joinUninterruptibly(Thread toJoin) {
    THREAD_JOIN.awaitUninterruptibly(toJoin);
  }

  /**
   * Invokes {@code unit.}{@link TimeUnit#timedJoin(Thread, long) timedJoin(toJoin, timeout)}
   * uninterruptibly.
   */
  @GwtIncompatible // concurrency
  @SuppressWarnings("GoodTime") // should accept a java.time.Duration
  public static void joinUninterruptibly(Thread toJoin, long timeout, TimeUnit unit) {
    Preconditions.checkNotNull(toJoin);
    THREAD_JOIN.awaitUninterruptibly(toJoin, timeout, unit);
  }

  private static final Interruptible<Thread,Void> THREAD_JOIN = new Interruptible<Thread,Void>() {
    @Override Void await(Thread thread) throws InterruptedException {
      thread.join();
      return null;
    }
    @Override Void await(Thread thread, int unused,
        long timeout, TimeUnit unit) throws InterruptedException {
      unit.timedJoin(thread, timeout);
      return null;
    }
  };

  /**
   * Invokes {@code future.}{@link Future#get() get()} uninterruptibly.
   *
   * <p>Similar methods:
   *
   * <ul>
   *   <li>To retrieve a result from a {@code Future} that is already done, use {@link
   *       Futures#getDone Futures.getDone}.
   *   <li>To treat {@link InterruptedException} uniformly with other exceptions, use {@link
   *       Futures#getChecked(Future, Class) Futures.getChecked}.
   *   <li>To get uninterruptibility and remove checked exceptions, use {@link
   *       Futures#getUnchecked}.
   * </ul>
   *
   * @throws ExecutionException if the computation threw an exception
   * @throws CancellationException if the computation was cancelled
   */
  @SuppressWarnings("unchecked")
  @CanIgnoreReturnValue
  public static <V> V getUninterruptibly(Future<V> future) throws ExecutionException {
    return (V) FUTURE_GET.awaitUninterruptibly(future);
  }

  /**
   * Invokes {@code future.}{@link Future#get(long, TimeUnit) get(timeout, unit)} uninterruptibly.
   *
   * <p>Similar methods:
   *
   * <ul>
   *   <li>To retrieve a result from a {@code Future} that is already done, use {@link
   *       Futures#getDone Futures.getDone}.
   *   <li>To treat {@link InterruptedException} uniformly with other exceptions, use {@link
   *       Futures#getChecked(Future, Class, long, TimeUnit) Futures.getChecked}.
   *   <li>To get uninterruptibility and remove checked exceptions, use {@link
   *       Futures#getUnchecked}.
   * </ul>
   *
   * @throws ExecutionException if the computation threw an exception
   * @throws CancellationException if the computation was cancelled
   * @throws TimeoutException if the wait timed out
   */
  @CanIgnoreReturnValue
  @GwtIncompatible // TODO
  @SuppressWarnings({"GoodTime", "unchecked"}) // should accept a java.time.Duration
  public static <V> V getUninterruptibly(Future<V> future, long timeout, TimeUnit unit)
      throws ExecutionException, TimeoutException {
    return (V) FUTURE_GET.awaitUninterruptibly(future, timeout, unit);
  }

  private static final Interruptible<Future<? extends Object>,Object> FUTURE_GET
      = new Interruptible<Future<? extends Object>,Object>() {
    @Override Object await(Future<? extends Object> future) throws InterruptedException {
      try {
        return future.get();
      } catch (ExecutionException e) {
        throw throwUnchecked(e);
      }
    }
    @Override Object await(Future<? extends Object> future,
        int unused, long time, TimeUnit unit) throws InterruptedException {
      try {
        return future.get(time, unit);
      } catch (ExecutionException | TimeoutException e) {
        throw throwUnchecked(e);
      }
    }
  };

  /** Invokes {@code queue.}{@link BlockingQueue#take() take()} uninterruptibly. */
  @SuppressWarnings("unchecked")
  @GwtIncompatible // concurrency
  public static <E> E takeUninterruptibly(BlockingQueue<E> queue) {
    return (E) QUEUE_TAKE.awaitUninterruptibly(queue);
  }

  private static final Interruptible<BlockingQueue<? extends Object>,Object> QUEUE_TAKE
      = new Interruptible<BlockingQueue<? extends Object>,Object>() {
    @Override Object await(BlockingQueue<? extends Object> queue) throws InterruptedException {
      return queue.take();
    }
  };

  /**
   * Invokes {@code queue.}{@link BlockingQueue#put(Object) put(element)} uninterruptibly.
   *
   * @throws ClassCastException if the class of the specified element prevents it from being added
   *     to the given queue
   * @throws IllegalArgumentException if some property of the specified element prevents it from
   *     being added to the given queue
   */
  @GwtIncompatible // concurrency
  public static <E> void putUninterruptibly(BlockingQueue<E> queue, E element) {
    QUEUE_PUT.awaitUninterruptibly(queue, element);
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  private static final Interruptible<BlockingQueue,Void> QUEUE_PUT
      = new Interruptible<BlockingQueue,Void>() {
    @Override Void await(BlockingQueue queue, Object element) throws InterruptedException {
      queue.put(element);
      return null;
    }
  };

  // TODO(user): Support Sleeper somehow (wrapper or interface method)?
  /** Invokes {@code unit.}{@link TimeUnit#sleep(long) sleep(sleepFor)} uninterruptibly. */
  @GwtIncompatible // concurrency
  @SuppressWarnings("GoodTime") // should accept a java.time.Duration
  public static void sleepUninterruptibly(long sleepFor, TimeUnit unit) {
    SLEEP.awaitUninterruptibly(null, sleepFor, unit);
  }

  private static final Interruptible<Void,Void> SLEEP = new Interruptible<Void,Void>() {
    @Override Void await(
        Void none, int unused, long timeout, TimeUnit unit) throws InterruptedException {
      unit.sleep(timeout);
      return null;
    }
  };

  /**
   * Invokes {@code semaphore.}{@link Semaphore#tryAcquire(int, long, TimeUnit) tryAcquire(1,
   * timeout, unit)} uninterruptibly.
   *
   * @since 18.0
   */
  @GwtIncompatible // concurrency
  @SuppressWarnings("GoodTime") // should accept a java.time.Duration
  public static boolean tryAcquireUninterruptibly(
      Semaphore semaphore, long timeout, TimeUnit unit) {
    return SEMAPHORE.awaitUninterruptibly(semaphore, timeout, unit);
  }

  /**
   * Invokes {@code semaphore.}{@link Semaphore#tryAcquire(int, long, TimeUnit) tryAcquire(permits,
   * timeout, unit)} uninterruptibly.
   *
   * @since 18.0
   */
  @GwtIncompatible // concurrency
  @SuppressWarnings("GoodTime") // should accept a java.time.Duration
  public static boolean tryAcquireUninterruptibly(
      Semaphore semaphore, int permits, long timeout, TimeUnit unit) {
    return SEMAPHORE.awaitUninterruptibly(semaphore, permits, timeout, unit);
  }

  private static final Interruptible<Semaphore,Boolean> SEMAPHORE
      = new Interruptible<Semaphore,Boolean>() {
    @Override Boolean await(
        Semaphore semaphore, int permits, long timeout, TimeUnit unit) throws InterruptedException {
      return semaphore.tryAcquire(permits, timeout, unit);
    }
  };

  // used to propagate checked exceptions for Future.get methods, bypassing compiler check
  @SuppressWarnings("unchecked")
  private static <E extends Exception> RuntimeException throwUnchecked(Exception e) throws E {
      throw (E) e;
  }

  // TODO(user): Add support for waitUninterruptibly.

  private Uninterruptibles() {}
}
