/*
 * Copyright (C) 2011 The Guava Authors
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

import static java.util.concurrent.TimeUnit.NANOSECONDS;

import com.google.common.annotations.Beta;
import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.base.Preconditions;
import com.google.common.collect.EvictingQueue;

import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Utilities for treating interruptible operations as uninterruptible.
 * In all cases, if a thread is interrupted during such a call, the call
 * continues to block until the result is available or the timeout elapses,
 * and only then re-interrupts the thread.
 *
 * @author Anthony Zana
 * @since 10.0
 */
@Beta
@GwtCompatible(emulated = true)
public final class Uninterruptibles {

  // Implementation Note: As of 3-7-11, the logic for each blocking/timeout
  // methods is identical, save for method being invoked.

  /**
   * Invokes {@code latch.}{@link CountDownLatch#await() await()}
   * uninterruptibly.
   */
  @GwtIncompatible // concurrency
  public static void awaitUninterruptibly(final CountDownLatch latch) {
    runUninterruptibly(new InterruptibleRunnableWithoutTimeout() {
      @Override
      public void run() throws InterruptedException {
        latch.await();
      }
    });
  }

  /**
   * Invokes
   * {@code latch.}{@link CountDownLatch#await(long, TimeUnit)
   * await(timeout, unit)} uninterruptibly.
   */
  @GwtIncompatible // concurrency
  public static boolean awaitUninterruptibly(
      final CountDownLatch latch, long timeout, TimeUnit unit) {
    // CountDownLatch treats negative timeouts just like zero.
    return callUninterruptibly(new InterruptibleCallableWithTimeout<Boolean>() {
      @Override
      Boolean call(long timeout, TimeUnit unit) throws InterruptedException {
        return latch.await(timeout, unit);
      }
    }, sanitizeTime(timeout), unit);
  }

  /**
   * Invokes {@code toJoin.}{@link Thread#join() join()} uninterruptibly.
   */
  @GwtIncompatible // concurrency
  public static void joinUninterruptibly(final Thread toJoin) {
    runUninterruptibly(new InterruptibleRunnableWithoutTimeout() {
      @Override
      void run() throws InterruptedException {
        toJoin.join();
      }
    });
  }

  /**
   * Invokes {@code future.}{@link Future#get() get()} uninterruptibly.
   * To get uninterruptibility and remove checked exceptions, see
   * {@link Futures#getUnchecked}.
   * <p/>
   * <p>If instead, you wish to treat {@link InterruptedException} uniformly
   * with other exceptions, see {@link Futures#getChecked(Future, Class)
   * Futures.getChecked}.
   *
   * @throws ExecutionException    if the computation threw an exception
   * @throws CancellationException if the computation was cancelled
   */
  public static <V> V getUninterruptibly(final Future<V> future) throws ExecutionException {
    final Queue<ExecutionException> executionException = EvictingQueue.create(1);
    V result = Uninterruptibles.callUninterruptibly(new InterruptibleCallableWithoutTimeout<V>() {
      @Override
      V call() throws InterruptedException {
        try {
          return future.get();
        } catch (ExecutionException e) {
          executionException.offer(e);
        }
        return null;
      }
    });
    if (!executionException.isEmpty()) {
      throw executionException.poll();
    }
    return result;
  }

  /**
   * Invokes
   * {@code future.}{@link Future#get(long, TimeUnit) get(timeout, unit)}
   * uninterruptibly.
   * <p/>
   * <p>If instead, you wish to treat {@link InterruptedException} uniformly
   * with other exceptions, see {@link Futures#getChecked(Future, Class)
   * Futures.getChecked}.
   *
   * @throws ExecutionException    if the computation threw an exception
   * @throws CancellationException if the computation was cancelled
   * @throws TimeoutException      if the wait timed out
   */
  @GwtIncompatible // TODO
  public static <V> V getUninterruptibly(final Future<V> future, long timeout, TimeUnit unit)
      throws ExecutionException, TimeoutException {
    final Queue<ExecutionException> executionException = EvictingQueue.create(1);
    final Queue<TimeoutException> timeoutException = EvictingQueue.create(1);
    V result = callUninterruptibly(new InterruptibleCallableWithTimeout<V>() {
      @Override
      V call(long timeout, TimeUnit unit) throws InterruptedException {
        try {
          return future.get(timeout, unit);
        } catch (ExecutionException e) {
          executionException.offer(e);
        } catch (TimeoutException e) {
          timeoutException.offer(e);
        }
        return null;
      }
    }, sanitizeTime(timeout), unit);
    if (!executionException.isEmpty()) {
      throw executionException.poll();
    } else if (!timeoutException.isEmpty()) {
      throw timeoutException.poll();
    }
    return result;
  }

  /**
   * Invokes
   * {@code unit.}{@link TimeUnit#timedJoin(Thread, long)
   * timedJoin(toJoin, timeout)} uninterruptibly.
   */
  @GwtIncompatible // concurrency
  public static void joinUninterruptibly(final Thread toJoin, long timeout, TimeUnit unit) {
    Preconditions.checkNotNull(toJoin);
    runUninterruptibly(new InterruptibleRunnableWithTimeout() {
      @Override
      void run(long timeout, TimeUnit unit) throws InterruptedException {
        unit.timedJoin(toJoin, timeout);
      }
    }, sanitizeTime(timeout), unit);
  }

  /**
   * Invokes {@code queue.}{@link BlockingQueue#take() take()} uninterruptibly.
   */
  @GwtIncompatible // concurrency
  public static <E> E takeUninterruptibly(final BlockingQueue<E> queue) {
    return callUninterruptibly(new InterruptibleCallableWithoutTimeout<E>() {
      @Override
      E call() throws InterruptedException {
        return queue.take();
      }
    });
  }

  /**
   * Invokes {@code queue.}{@link BlockingQueue#put(Object) put(element)}
   * uninterruptibly.
   *
   * @throws ClassCastException       if the class of the specified element prevents
   *                                  it from being added to the given queue
   * @throws IllegalArgumentException if some property of the specified element
   *                                  prevents it from being added to the given queue
   */
  @GwtIncompatible // concurrency
  public static <E> void putUninterruptibly(final BlockingQueue<E> queue, final E element) {
    runUninterruptibly(new InterruptibleRunnableWithoutTimeout() {
      @Override
      void run() throws InterruptedException {
        queue.put(element);
      }
    });
  }

  // TODO(user): Support Sleeper somehow (wrapper or interface method)?

  /**
   * Invokes {@code unit.}{@link TimeUnit#sleep(long) sleep(sleepFor)}
   * uninterruptibly.
   */
  @GwtIncompatible // concurrency
  public static void sleepUninterruptibly(long sleepFor, TimeUnit unit) {
    runUninterruptibly(new InterruptibleRunnableWithTimeout() {
      @Override
      void run(long timeout, TimeUnit unit) throws InterruptedException {
        unit.sleep(timeout);
      }
    }, sanitizeTime(sleepFor), unit);
  }

  /**
   * Invokes {@code semaphore.}{@link Semaphore#tryAcquire(int, long, TimeUnit)
   * tryAcquire(1, timeout, unit)} uninterruptibly.
   *
   * @since 18.0
   */
  @GwtIncompatible // concurrency
  public static boolean tryAcquireUninterruptibly(
      Semaphore semaphore, long timeout, TimeUnit unit) {
    return tryAcquireUninterruptibly(semaphore, 1, timeout, unit);
  }

  /**
   * Invokes {@code semaphore.}{@link Semaphore#tryAcquire(int, long, TimeUnit)
   * tryAcquire(permits, timeout, unit)} uninterruptibly.
   *
   * @since 18.0
   */
  @GwtIncompatible // concurrency
  public static boolean tryAcquireUninterruptibly(
      final Semaphore semaphore, final int permits, long timeout, TimeUnit unit) {
    return callUninterruptibly(new InterruptibleCallableWithTimeout<Boolean>() {
      @Override
      Boolean call(long timeout, TimeUnit unit) throws InterruptedException {
        return semaphore.tryAcquire(permits, timeout, unit);
      }
    }, sanitizeTime(timeout), unit);
  }

  @GwtIncompatible // concurrency
  private static void runUninterruptibly(
      InterruptibleRunnableWithoutTimeout function) {
    callUninterruptibly(function, TimeDuration.infinity());
  }

  @GwtIncompatible // concurrency
  private static void runUninterruptibly(
      InterruptibleRunnableWithTimeout function, long timeout, TimeUnit unit) {
    callUninterruptibly(function, TimeDuration.of(timeout, unit));
  }

  @GwtIncompatible // concurrency
  private static <V> V callUninterruptibly(
      InterruptibleCallableWithoutTimeout<V> function) {
    return callUninterruptibly(function, TimeDuration.infinity());
  }

  @GwtIncompatible // concurrency
  private static <V> V callUninterruptibly(
      InterruptibleCallableWithTimeout<V> function, long timeout, TimeUnit unit) {
    return callUninterruptibly(function, TimeDuration.of(timeout, unit));
  }


  @GwtIncompatible // concurrency
  private static <V> V callUninterruptibly(
      InterruptibleCallable<V> function, TimeDuration timeDuration) {
    boolean interrupted = false;
    try {
      // If time duration is infinity, the following two statements are effectively dead code and
      // have no side-effects
      final long remainingNanos = timeDuration.getUnit().toNanos(timeDuration.getDuration());
      final long end = System.nanoTime() + remainingNanos;
      while (true) {
        try {
          return function.call(timeDuration);
        } catch (InterruptedException e) {
          interrupted = true;
          timeDuration = timeDuration.isInfinity() ? timeDuration :
              TimeDuration.of(sanitizeTime(end - System.nanoTime()), NANOSECONDS);
        }
      }
    } finally {
      if (interrupted) {
        Thread.currentThread().interrupt();
      }
    }
  }

  private static long sanitizeTime(long time) {
    return time < 0 ? 0 : time;
  }

  // TODO(user): Add support for waitUninterruptibly.

  private Uninterruptibles() {
  }
}