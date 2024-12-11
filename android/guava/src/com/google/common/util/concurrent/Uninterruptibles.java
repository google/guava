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

import static com.google.common.base.Verify.verify;
import static com.google.common.util.concurrent.Internal.toNanosSaturated;
import static java.util.concurrent.TimeUnit.NANOSECONDS;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.annotations.J2ktIncompatible;
import com.google.common.base.Preconditions;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.time.Duration;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Utilities for treating interruptible operations as uninterruptible. In all cases, if a thread is
 * interrupted during such a call, the call continues to block until the result is available or the
 * timeout elapses, and only then re-interrupts the thread.
 *
 * @author Anthony Zana
 * @since 10.0
 */
@GwtCompatible(emulated = true)
@ElementTypesAreNonnullByDefault
public final class Uninterruptibles {

  // Implementation Note: As of 3-7-11, the logic for each blocking/timeout
  // methods is identical, save for method being invoked.

  /** Invokes {@code latch.}{@link CountDownLatch#await() await()} uninterruptibly. */
  @J2ktIncompatible
  @GwtIncompatible // concurrency
  public static void awaitUninterruptibly(CountDownLatch latch) {
    boolean interrupted = false;
    try {
      while (true) {
        try {
          latch.await();
          return;
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

  /**
   * Invokes {@code latch.}{@link CountDownLatch#await(long, TimeUnit) await(timeout, unit)}
   * uninterruptibly.
   *
   * @since NEXT (but since 28.0 in the JRE flavor)
   */
  @J2ktIncompatible
  @GwtIncompatible // concurrency
  @SuppressWarnings("Java7ApiChecker")
  @IgnoreJRERequirement // Users will use this only if they're already using Duration.
  public static boolean awaitUninterruptibly(CountDownLatch latch, Duration timeout) {
    return awaitUninterruptibly(latch, toNanosSaturated(timeout), TimeUnit.NANOSECONDS);
  }

  /**
   * Invokes {@code latch.}{@link CountDownLatch#await(long, TimeUnit) await(timeout, unit)}
   * uninterruptibly.
   */
  @J2ktIncompatible
  @GwtIncompatible // concurrency
  @SuppressWarnings("GoodTime") // should accept a java.time.Duration
  public static boolean awaitUninterruptibly(CountDownLatch latch, long timeout, TimeUnit unit) {
    boolean interrupted = false;
    try {
      long remainingNanos = unit.toNanos(timeout);
      long end = System.nanoTime() + remainingNanos;

      while (true) {
        try {
          // CountDownLatch treats negative timeouts just like zero.
          return latch.await(remainingNanos, NANOSECONDS);
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

  /**
   * Invokes {@code condition.}{@link Condition#await(long, TimeUnit) await(timeout, unit)}
   * uninterruptibly.
   *
   * @since NEXT (but since 28.0 in the JRE flavor)
   */
  @J2ktIncompatible
  @GwtIncompatible // concurrency
  @SuppressWarnings("Java7ApiChecker")
  @IgnoreJRERequirement // Users will use this only if they're already using Duration.
  public static boolean awaitUninterruptibly(Condition condition, Duration timeout) {
    return awaitUninterruptibly(condition, toNanosSaturated(timeout), TimeUnit.NANOSECONDS);
  }

  /**
   * Invokes {@code condition.}{@link Condition#await(long, TimeUnit) await(timeout, unit)}
   * uninterruptibly.
   *
   * @since 23.6
   */
  @J2ktIncompatible
  @GwtIncompatible // concurrency
  @SuppressWarnings("GoodTime") // should accept a java.time.Duration
  public static boolean awaitUninterruptibly(Condition condition, long timeout, TimeUnit unit) {
    boolean interrupted = false;
    try {
      long remainingNanos = unit.toNanos(timeout);
      long end = System.nanoTime() + remainingNanos;

      while (true) {
        try {
          return condition.await(remainingNanos, NANOSECONDS);
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

  /** Invokes {@code toJoin.}{@link Thread#join() join()} uninterruptibly. */
  @J2ktIncompatible
  @GwtIncompatible // concurrency
  public static void joinUninterruptibly(Thread toJoin) {
    boolean interrupted = false;
    try {
      while (true) {
        try {
          toJoin.join();
          return;
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

  /**
   * Invokes {@code unit.}{@link TimeUnit#timedJoin(Thread, long) timedJoin(toJoin, timeout)}
   * uninterruptibly.
   *
   * @since NEXT (but since 28.0 in the JRE flavor)
   */
  @J2ktIncompatible
  @GwtIncompatible // concurrency
  @SuppressWarnings("Java7ApiChecker")
  @IgnoreJRERequirement // Users will use this only if they're already using Duration.
  public static void joinUninterruptibly(Thread toJoin, Duration timeout) {
    joinUninterruptibly(toJoin, toNanosSaturated(timeout), TimeUnit.NANOSECONDS);
  }

  /**
   * Invokes {@code unit.}{@link TimeUnit#timedJoin(Thread, long) timedJoin(toJoin, timeout)}
   * uninterruptibly.
   */
  @J2ktIncompatible
  @GwtIncompatible // concurrency
  @SuppressWarnings("GoodTime") // should accept a java.time.Duration
  public static void joinUninterruptibly(Thread toJoin, long timeout, TimeUnit unit) {
    Preconditions.checkNotNull(toJoin);
    boolean interrupted = false;
    try {
      long remainingNanos = unit.toNanos(timeout);
      long end = System.nanoTime() + remainingNanos;
      while (true) {
        try {
          // TimeUnit.timedJoin() treats negative timeouts just like zero.
          NANOSECONDS.timedJoin(toJoin, remainingNanos);
          return;
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
  @CanIgnoreReturnValue
  @ParametricNullness
  public static <V extends @Nullable Object> V getUninterruptibly(Future<V> future)
      throws ExecutionException {
    boolean interrupted = false;
    try {
      while (true) {
        try {
          return future.get();
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
   * @since NEXT (but since 28.0 in the JRE flavor)
   */
  @CanIgnoreReturnValue
  @J2ktIncompatible
  @GwtIncompatible // java.time.Duration
  @ParametricNullness
  @SuppressWarnings("Java7ApiChecker")
  @IgnoreJRERequirement // Users will use this only if they're already using Duration.
  public static <V extends @Nullable Object> V getUninterruptibly(
      Future<V> future, Duration timeout) throws ExecutionException, TimeoutException {
    return getUninterruptibly(future, toNanosSaturated(timeout), TimeUnit.NANOSECONDS);
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
  @J2ktIncompatible
  @GwtIncompatible // TODO
  @SuppressWarnings("GoodTime") // should accept a java.time.Duration
  @ParametricNullness
  public static <V extends @Nullable Object> V getUninterruptibly(
      Future<V> future, long timeout, TimeUnit unit) throws ExecutionException, TimeoutException {
    boolean interrupted = false;
    try {
      long remainingNanos = unit.toNanos(timeout);
      long end = System.nanoTime() + remainingNanos;

      while (true) {
        try {
          // Future treats negative timeouts just like zero.
          return future.get(remainingNanos, NANOSECONDS);
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

  /** Invokes {@code queue.}{@link BlockingQueue#take() take()} uninterruptibly. */
  @J2ktIncompatible
  @GwtIncompatible // concurrency
  public static <E> E takeUninterruptibly(BlockingQueue<E> queue) {
    boolean interrupted = false;
    try {
      while (true) {
        try {
          return queue.take();
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

  /**
   * Invokes {@code queue.}{@link BlockingQueue#put(Object) put(element)} uninterruptibly.
   *
   * @throws ClassCastException if the class of the specified element prevents it from being added
   *     to the given queue
   * @throws IllegalArgumentException if some property of the specified element prevents it from
   *     being added to the given queue
   */
  @J2ktIncompatible
  @GwtIncompatible // concurrency
  public static <E> void putUninterruptibly(BlockingQueue<E> queue, E element) {
    boolean interrupted = false;
    try {
      while (true) {
        try {
          queue.put(element);
          return;
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

  // TODO(user): Support Sleeper somehow (wrapper or interface method)?
  /**
   * Invokes {@code unit.}{@link TimeUnit#sleep(long) sleep(sleepFor)} uninterruptibly.
   *
   * @since NEXT (but since 28.0 in the JRE flavor)
   */
  @J2ktIncompatible
  @GwtIncompatible // concurrency
  @SuppressWarnings("Java7ApiChecker")
  @IgnoreJRERequirement // Users will use this only if they're already using Duration.
  public static void sleepUninterruptibly(Duration sleepFor) {
    sleepUninterruptibly(toNanosSaturated(sleepFor), TimeUnit.NANOSECONDS);
  }

  // TODO(user): Support Sleeper somehow (wrapper or interface method)?
  /** Invokes {@code unit.}{@link TimeUnit#sleep(long) sleep(sleepFor)} uninterruptibly. */
  @J2ktIncompatible
  @GwtIncompatible // concurrency
  @SuppressWarnings("GoodTime") // should accept a java.time.Duration
  public static void sleepUninterruptibly(long sleepFor, TimeUnit unit) {
    boolean interrupted = false;
    try {
      long remainingNanos = unit.toNanos(sleepFor);
      long end = System.nanoTime() + remainingNanos;
      while (true) {
        try {
          // TimeUnit.sleep() treats negative timeouts just like zero.
          NANOSECONDS.sleep(remainingNanos);
          return;
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

  /**
   * Invokes {@code semaphore.}{@link Semaphore#tryAcquire(int, long, TimeUnit) tryAcquire(1,
   * timeout, unit)} uninterruptibly.
   *
   * @since NEXT (but since 28.0 in the JRE flavor)
   */
  @J2ktIncompatible
  @GwtIncompatible // concurrency
  @SuppressWarnings("Java7ApiChecker")
  @IgnoreJRERequirement // Users will use this only if they're already using Duration.
  public static boolean tryAcquireUninterruptibly(Semaphore semaphore, Duration timeout) {
    return tryAcquireUninterruptibly(semaphore, toNanosSaturated(timeout), TimeUnit.NANOSECONDS);
  }

  /**
   * Invokes {@code semaphore.}{@link Semaphore#tryAcquire(int, long, TimeUnit) tryAcquire(1,
   * timeout, unit)} uninterruptibly.
   *
   * @since 18.0
   */
  @J2ktIncompatible
  @GwtIncompatible // concurrency
  @SuppressWarnings("GoodTime") // should accept a java.time.Duration
  public static boolean tryAcquireUninterruptibly(
      Semaphore semaphore, long timeout, TimeUnit unit) {
    return tryAcquireUninterruptibly(semaphore, 1, timeout, unit);
  }

  /**
   * Invokes {@code semaphore.}{@link Semaphore#tryAcquire(int, long, TimeUnit) tryAcquire(permits,
   * timeout, unit)} uninterruptibly.
   *
   * @since NEXT (but since 28.0 in the JRE flavor)
   */
  @J2ktIncompatible
  @GwtIncompatible // concurrency
  @SuppressWarnings("Java7ApiChecker")
  @IgnoreJRERequirement // Users will use this only if they're already using Duration.
  public static boolean tryAcquireUninterruptibly(
      Semaphore semaphore, int permits, Duration timeout) {
    return tryAcquireUninterruptibly(
        semaphore, permits, toNanosSaturated(timeout), TimeUnit.NANOSECONDS);
  }

  /**
   * Invokes {@code semaphore.}{@link Semaphore#tryAcquire(int, long, TimeUnit) tryAcquire(permits,
   * timeout, unit)} uninterruptibly.
   *
   * @since 18.0
   */
  @J2ktIncompatible
  @GwtIncompatible // concurrency
  @SuppressWarnings("GoodTime") // should accept a java.time.Duration
  public static boolean tryAcquireUninterruptibly(
      Semaphore semaphore, int permits, long timeout, TimeUnit unit) {
    boolean interrupted = false;
    try {
      long remainingNanos = unit.toNanos(timeout);
      long end = System.nanoTime() + remainingNanos;

      while (true) {
        try {
          // Semaphore treats negative timeouts just like zero.
          return semaphore.tryAcquire(permits, remainingNanos, NANOSECONDS);
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

  /**
   * Invokes {@code lock.}{@link Lock#tryLock(long, TimeUnit) tryLock(timeout, unit)}
   * uninterruptibly.
   *
   * @since NEXT (but since 30.0 in the JRE flavor)
   */
  @J2ktIncompatible
  @GwtIncompatible // concurrency
  @SuppressWarnings("Java7ApiChecker")
  @IgnoreJRERequirement // Users will use this only if they're already using Duration.
  public static boolean tryLockUninterruptibly(Lock lock, Duration timeout) {
    return tryLockUninterruptibly(lock, toNanosSaturated(timeout), TimeUnit.NANOSECONDS);
  }

  /**
   * Invokes {@code lock.}{@link Lock#tryLock(long, TimeUnit) tryLock(timeout, unit)}
   * uninterruptibly.
   *
   * @since 30.0
   */
  @J2ktIncompatible
  @GwtIncompatible // concurrency
  @SuppressWarnings("GoodTime") // should accept a java.time.Duration
  public static boolean tryLockUninterruptibly(Lock lock, long timeout, TimeUnit unit) {
    boolean interrupted = false;
    try {
      long remainingNanos = unit.toNanos(timeout);
      long end = System.nanoTime() + remainingNanos;

      while (true) {
        try {
          return lock.tryLock(remainingNanos, NANOSECONDS);
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

  /**
   * Invokes {@code executor.}{@link ExecutorService#awaitTermination(long, TimeUnit)
   * awaitTermination(long, TimeUnit)} uninterruptibly with no timeout.
   *
   * @since 30.0
   */
  @J2ktIncompatible
  @GwtIncompatible // concurrency
  public static void awaitTerminationUninterruptibly(ExecutorService executor) {
    // TODO(cpovirk): We could optimize this to avoid calling nanoTime() at all.
    verify(awaitTerminationUninterruptibly(executor, Long.MAX_VALUE, NANOSECONDS));
  }

  /**
   * Invokes {@code executor.}{@link ExecutorService#awaitTermination(long, TimeUnit)
   * awaitTermination(long, TimeUnit)} uninterruptibly.
   *
   * @since NEXT (but since 30.0 in the JRE flavor)
   */
  @J2ktIncompatible
  @GwtIncompatible // concurrency
  @SuppressWarnings("Java7ApiChecker")
  @IgnoreJRERequirement // Users will use this only if they're already using Duration.
  public static boolean awaitTerminationUninterruptibly(
      ExecutorService executor, Duration timeout) {
    return awaitTerminationUninterruptibly(executor, toNanosSaturated(timeout), NANOSECONDS);
  }

  /**
   * Invokes {@code executor.}{@link ExecutorService#awaitTermination(long, TimeUnit)
   * awaitTermination(long, TimeUnit)} uninterruptibly.
   *
   * @since 30.0
   */
  @J2ktIncompatible
  @GwtIncompatible // concurrency
  @SuppressWarnings("GoodTime")
  public static boolean awaitTerminationUninterruptibly(
      ExecutorService executor, long timeout, TimeUnit unit) {
    boolean interrupted = false;
    try {
      long remainingNanos = unit.toNanos(timeout);
      long end = System.nanoTime() + remainingNanos;

      while (true) {
        try {
          return executor.awaitTermination(remainingNanos, NANOSECONDS);
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

  // TODO(user): Add support for waitUninterruptibly.

  private Uninterruptibles() {}
}
