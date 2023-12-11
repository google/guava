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

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.annotations.GwtIncompatible;
import com.google.common.annotations.J2ktIncompatible;
import com.google.common.primitives.Longs;
import com.google.errorprone.annotations.concurrent.GuardedBy;
import com.google.j2objc.annotations.Weak;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import javax.annotation.CheckForNull;

/**
 * A synchronization abstraction supporting waiting on arbitrary boolean conditions.
 *
 * <p>This class is intended as a replacement for {@link ReentrantLock}. Code using {@code Monitor}
 * is less error-prone and more readable than code using {@code ReentrantLock}, without significant
 * performance loss. {@code Monitor} even has the potential for performance gain by optimizing the
 * evaluation and signaling of conditions. Signaling is entirely <a
 * href="http://en.wikipedia.org/wiki/Monitor_(synchronization)#Implicit_signaling">implicit</a>. By
 * eliminating explicit signaling, this class can guarantee that only one thread is awakened when a
 * condition becomes true (no "signaling storms" due to use of {@link
 * java.util.concurrent.locks.Condition#signalAll Condition.signalAll}) and that no signals are lost
 * (no "hangs" due to incorrect use of {@link java.util.concurrent.locks.Condition#signal
 * Condition.signal}).
 *
 * <p>A thread is said to <i>occupy</i> a monitor if it has <i>entered</i> the monitor but not yet
 * <i>left</i>. Only one thread may occupy a given monitor at any moment. A monitor is also
 * reentrant, so a thread may enter a monitor any number of times, and then must leave the same
 * number of times. The <i>enter</i> and <i>leave</i> operations have the same synchronization
 * semantics as the built-in Java language synchronization primitives.
 *
 * <p>A call to any of the <i>enter</i> methods with <b>void</b> return type should always be
 * followed immediately by a <i>try/finally</i> block to ensure that the current thread leaves the
 * monitor cleanly:
 *
 * <pre>{@code
 * monitor.enter();
 * try {
 *   // do things while occupying the monitor
 * } finally {
 *   monitor.leave();
 * }
 * }</pre>
 *
 * <p>A call to any of the <i>enter</i> methods with <b>boolean</b> return type should always appear
 * as the condition of an <i>if</i> statement containing a <i>try/finally</i> block to ensure that
 * the current thread leaves the monitor cleanly:
 *
 * <pre>{@code
 * if (monitor.tryEnter()) {
 *   try {
 *     // do things while occupying the monitor
 *   } finally {
 *     monitor.leave();
 *   }
 * } else {
 *   // do other things since the monitor was not available
 * }
 * }</pre>
 *
 * <h2>Comparison with {@code synchronized} and {@code ReentrantLock}</h2>
 *
 * <p>The following examples show a simple threadsafe holder expressed using {@code synchronized},
 * {@link ReentrantLock}, and {@code Monitor}.
 *
 * <h3>{@code synchronized}</h3>
 *
 * <p>This version is the fewest lines of code, largely because the synchronization mechanism used
 * is built into the language and runtime. But the programmer has to remember to avoid a couple of
 * common bugs: The {@code wait()} must be inside a {@code while} instead of an {@code if}, and
 * {@code notifyAll()} must be used instead of {@code notify()} because there are two different
 * logical conditions being awaited.
 *
 * <pre>{@code
 * public class SafeBox<V> {
 *   private V value;
 *
 *   public synchronized V get() throws InterruptedException {
 *     while (value == null) {
 *       wait();
 *     }
 *     V result = value;
 *     value = null;
 *     notifyAll();
 *     return result;
 *   }
 *
 *   public synchronized void set(V newValue) throws InterruptedException {
 *     while (value != null) {
 *       wait();
 *     }
 *     value = newValue;
 *     notifyAll();
 *   }
 * }
 * }</pre>
 *
 * <h3>{@code ReentrantLock}</h3>
 *
 * <p>This version is much more verbose than the {@code synchronized} version, and still suffers
 * from the need for the programmer to remember to use {@code while} instead of {@code if}. However,
 * one advantage is that we can introduce two separate {@code Condition} objects, which allows us to
 * use {@code signal()} instead of {@code signalAll()}, which may be a performance benefit.
 *
 * <pre>{@code
 * public class SafeBox<V> {
 *   private V value;
 *   private final ReentrantLock lock = new ReentrantLock();
 *   private final Condition valuePresent = lock.newCondition();
 *   private final Condition valueAbsent = lock.newCondition();
 *
 *   public V get() throws InterruptedException {
 *     lock.lock();
 *     try {
 *       while (value == null) {
 *         valuePresent.await();
 *       }
 *       V result = value;
 *       value = null;
 *       valueAbsent.signal();
 *       return result;
 *     } finally {
 *       lock.unlock();
 *     }
 *   }
 *
 *   public void set(V newValue) throws InterruptedException {
 *     lock.lock();
 *     try {
 *       while (value != null) {
 *         valueAbsent.await();
 *       }
 *       value = newValue;
 *       valuePresent.signal();
 *     } finally {
 *       lock.unlock();
 *     }
 *   }
 * }
 * }</pre>
 *
 * <h3>{@code Monitor}</h3>
 *
 * <p>This version adds some verbosity around the {@code Guard} objects, but removes that same
 * verbosity, and more, from the {@code get} and {@code set} methods. {@code Monitor} implements the
 * same efficient signaling as we had to hand-code in the {@code ReentrantLock} version above.
 * Finally, the programmer no longer has to hand-code the wait loop, and therefore doesn't have to
 * remember to use {@code while} instead of {@code if}.
 *
 * <pre>{@code
 * public class SafeBox<V> {
 *   private V value;
 *   private final Monitor monitor = new Monitor();
 *   private final Monitor.Guard valuePresent = monitor.newGuard(() -> value != null);
 *   private final Monitor.Guard valueAbsent = monitor.newGuard(() -> value == null);
 *
 *   public V get() throws InterruptedException {
 *     monitor.enterWhen(valuePresent);
 *     try {
 *       V result = value;
 *       value = null;
 *       return result;
 *     } finally {
 *       monitor.leave();
 *     }
 *   }
 *
 *   public void set(V newValue) throws InterruptedException {
 *     monitor.enterWhen(valueAbsent);
 *     try {
 *       value = newValue;
 *     } finally {
 *       monitor.leave();
 *     }
 *   }
 * }
 * }</pre>
 *
 * @author Justin T. Sampson
 * @author Martin Buchholz
 * @since 10.0
 */
@J2ktIncompatible
@GwtIncompatible
@SuppressWarnings("GuardedBy") // TODO(b/35466881): Fix or suppress.
@ElementTypesAreNonnullByDefault
public final class Monitor {
  // TODO(user): Use raw LockSupport or AbstractQueuedSynchronizer instead of ReentrantLock.
  // TODO(user): "Port" jsr166 tests for ReentrantLock.
  //
  // TODO(user): Change API to make it impossible to use a Guard with the "wrong" monitor,
  //    by making the monitor implicit, and to eliminate other sources of IMSE.
  //    Imagine:
  //    guard.lock();
  //    try { /* monitor locked and guard satisfied here */ }
  //    finally { guard.unlock(); }
  // Here are Justin's design notes about this:
  //
  // This idea has come up from time to time, and I think one of my
  // earlier versions of Monitor even did something like this. I ended
  // up strongly favoring the current interface.
  //
  // I probably can't remember all the reasons (it's possible you
  // could find them in the code review archives), but here are a few:
  //
  // 1. What about leaving/unlocking? Are you going to do
  //    guard.enter() paired with monitor.leave()? That might get
  //    confusing. It's nice for the finally block to look as close as
  //    possible to the thing right before the try. You could have
  //    guard.leave(), but that's a little odd as well because the
  //    guard doesn't have anything to do with leaving. You can't
  //    really enforce that the guard you're leaving is the same one
  //    you entered with, and it doesn't actually matter.
  //
  // 2. Since you can enter the monitor without a guard at all, some
  //    places you'll have monitor.enter()/monitor.leave() and other
  //    places you'll have guard.enter()/guard.leave() even though
  //    it's the same lock being acquired underneath. Always using
  //    monitor.enterXXX()/monitor.leave() will make it really clear
  //    which lock is held at any point in the code.
  //
  // 3. I think "enterWhen(notEmpty)" reads better than "notEmpty.enter()".
  //
  // TODO(user): Implement ReentrantLock features:
  //    - toString() method
  //    - getOwner() method
  //    - getQueuedThreads() method
  //    - getWaitingThreads(Guard) method
  //    - implement Serializable
  //    - redo the API to be as close to identical to ReentrantLock as possible,
  //      since, after all, this class is also a reentrant mutual exclusion lock!?

  /*
   * One of the key challenges of this class is to prevent lost signals, while trying hard to
   * minimize unnecessary signals. One simple and correct algorithm is to signal some other waiter
   * with a satisfied guard (if one exists) whenever any thread occupying the monitor exits the
   * monitor, either by unlocking all of its held locks, or by starting to wait for a guard. This
   * includes exceptional exits, so all control paths involving signalling must be protected by a
   * finally block.
   *
   * Further optimizations of this algorithm become increasingly subtle. A wait that terminates
   * without the guard being satisfied (due to timeout, but not interrupt) can then immediately exit
   * the monitor without signalling. If it timed out without being signalled, it does not need to
   * "pass on" the signal to another thread. If it *was* signalled, then its guard must have been
   * satisfied at the time of signal, and has since been modified by some other thread to be
   * non-satisfied before reacquiring the lock, and that other thread takes over the responsibility
   * of signaling the next waiter.
   *
   * Unlike the underlying Condition, if we are not careful, an interrupt *can* cause a signal to be
   * lost, because the signal may be sent to a condition whose sole waiter has just been
   * interrupted.
   *
   * Imagine a monitor with multiple guards. A thread enters the monitor, satisfies all the guards,
   * and leaves, calling signalNextWaiter. With traditional locks and conditions, all the conditions
   * need to be signalled because it is not known which if any of them have waiters (and hasWaiters
   * can't be used reliably because of a check-then-act race). With our Monitor guards, we only
   * signal the first active guard that is satisfied. But the corresponding thread may have already
   * been interrupted and is waiting to reacquire the lock while still registered in activeGuards,
   * in which case the signal is a no-op, and the bigger-picture signal is lost unless interrupted
   * threads take special action by participating in the signal-passing game.
   */

  /*
   * Timeout handling is intricate, especially given our ambitious goals:
   * - Avoid underflow and overflow of timeout values when specified timeouts are close to
   *   Long.MIN_VALUE or Long.MAX_VALUE.
   * - Favor responding to interrupts over timeouts.
   * - System.nanoTime() is expensive enough that we want to call it the minimum required number of
   *   times, typically once before invoking a blocking method. This often requires keeping track of
   *   the first time in a method that nanoTime() has been invoked, for which the special value 0L
   *   is reserved to mean "uninitialized". If timeout is non-positive, then nanoTime need never be
   *   called.
   * - Keep behavior of fair and non-fair instances consistent.
   */

  /**
   * A boolean condition for which a thread may wait. A {@code Guard} is associated with a single
   * {@code Monitor}. The monitor may check the guard at arbitrary times from any thread occupying
   * the monitor, so code should not be written to rely on how often a guard might or might not be
   * checked.
   *
   * <p>If a {@code Guard} is passed into any method of a {@code Monitor} other than the one it is
   * associated with, an {@link IllegalMonitorStateException} is thrown.
   *
   * @since 10.0
   */
  public abstract static class Guard {

    @Weak final Monitor monitor;
    final Condition condition;

    @GuardedBy("monitor.lock")
    int waiterCount = 0;

    /** The next active guard */
    @GuardedBy("monitor.lock")
    @CheckForNull
    Guard next;

    protected Guard(Monitor monitor) {
      this.monitor = checkNotNull(monitor, "monitor");
      this.condition = monitor.lock.newCondition();
    }

    /**
     * Evaluates this guard's boolean condition. This method is always called with the associated
     * monitor already occupied. Implementations of this method must depend only on state protected
     * by the associated monitor, and must not modify that state.
     */
    public abstract boolean isSatisfied();
  }

  /** Whether this monitor is fair. */
  private final boolean fair;

  /** The lock underlying this monitor. */
  private final ReentrantLock lock;

  /**
   * The guards associated with this monitor that currently have waiters ({@code waiterCount > 0}).
   * A linked list threaded through the Guard.next field.
   */
  @GuardedBy("lock")
  @CheckForNull
  private Guard activeGuards = null;

  /**
   * Creates a monitor with a non-fair (but fast) ordering policy. Equivalent to {@code
   * Monitor(false)}.
   */
  public Monitor() {
    this(false);
  }

  /**
   * Creates a monitor with the given ordering policy.
   *
   * @param fair whether this monitor should use a fair ordering policy rather than a non-fair (but
   *     fast) one
   */
  public Monitor(boolean fair) {
    this.fair = fair;
    this.lock = new ReentrantLock(fair);
  }

  /** Enters this monitor. Blocks indefinitely. */
  public void enter() {
    lock.lock();
  }

  /**
   * Enters this monitor. Blocks at most the given time.
   *
   * @return whether the monitor was entered
   */
  @SuppressWarnings("GoodTime") // should accept a java.time.Duration
  public boolean enter(long time, TimeUnit unit) {
    final long timeoutNanos = toSafeNanos(time, unit);
    final ReentrantLock lock = this.lock;
    if (!fair && lock.tryLock()) {
      return true;
    }
    boolean interrupted = Thread.interrupted();
    try {
      final long startTime = System.nanoTime();
      for (long remainingNanos = timeoutNanos; ; ) {
        try {
          return lock.tryLock(remainingNanos, TimeUnit.NANOSECONDS);
        } catch (InterruptedException interrupt) {
          interrupted = true;
          remainingNanos = remainingNanos(startTime, timeoutNanos);
        }
      }
    } finally {
      if (interrupted) {
        Thread.currentThread().interrupt();
      }
    }
  }

  /**
   * Enters this monitor. Blocks indefinitely, but may be interrupted.
   *
   * @throws InterruptedException if interrupted while waiting
   */
  public void enterInterruptibly() throws InterruptedException {
    lock.lockInterruptibly();
  }

  /**
   * Enters this monitor. Blocks at most the given time, and may be interrupted.
   *
   * @return whether the monitor was entered
   * @throws InterruptedException if interrupted while waiting
   */
  @SuppressWarnings("GoodTime") // should accept a java.time.Duration
  public boolean enterInterruptibly(long time, TimeUnit unit) throws InterruptedException {
    return lock.tryLock(time, unit);
  }

  /**
   * Enters this monitor if it is possible to do so immediately. Does not block.
   *
   * <p><b>Note:</b> This method disregards the fairness setting of this monitor.
   *
   * @return whether the monitor was entered
   */
  public boolean tryEnter() {
    return lock.tryLock();
  }

  /**
   * Enters this monitor when the guard is satisfied. Blocks indefinitely, but may be interrupted.
   *
   * @throws InterruptedException if interrupted while waiting
   */
  public void enterWhen(Guard guard) throws InterruptedException {
    if (guard.monitor != this) {
      throw new IllegalMonitorStateException();
    }
    final ReentrantLock lock = this.lock;
    boolean signalBeforeWaiting = lock.isHeldByCurrentThread();
    lock.lockInterruptibly();

    boolean satisfied = false;
    try {
      if (!guard.isSatisfied()) {
        await(guard, signalBeforeWaiting);
      }
      satisfied = true;
    } finally {
      if (!satisfied) {
        leave();
      }
    }
  }

  /**
   * Enters this monitor when the guard is satisfied. Blocks at most the given time, including both
   * the time to acquire the lock and the time to wait for the guard to be satisfied, and may be
   * interrupted.
   *
   * @return whether the monitor was entered, which guarantees that the guard is now satisfied
   * @throws InterruptedException if interrupted while waiting
   */
  @SuppressWarnings("GoodTime") // should accept a java.time.Duration
  public boolean enterWhen(Guard guard, long time, TimeUnit unit) throws InterruptedException {
    final long timeoutNanos = toSafeNanos(time, unit);
    if (guard.monitor != this) {
      throw new IllegalMonitorStateException();
    }
    final ReentrantLock lock = this.lock;
    boolean reentrant = lock.isHeldByCurrentThread();
    long startTime = 0L;

    locked:
    {
      if (!fair) {
        // Check interrupt status to get behavior consistent with fair case.
        if (Thread.interrupted()) {
          throw new InterruptedException();
        }
        if (lock.tryLock()) {
          break locked;
        }
      }
      startTime = initNanoTime(timeoutNanos);
      if (!lock.tryLock(time, unit)) {
        return false;
      }
    }

    boolean satisfied = false;
    boolean threw = true;
    try {
      satisfied =
          guard.isSatisfied()
              || awaitNanos(
                  guard,
                  (startTime == 0L) ? timeoutNanos : remainingNanos(startTime, timeoutNanos),
                  reentrant);
      threw = false;
      return satisfied;
    } finally {
      if (!satisfied) {
        try {
          // Don't need to signal if timed out, but do if interrupted
          if (threw && !reentrant) {
            signalNextWaiter();
          }
        } finally {
          lock.unlock();
        }
      }
    }
  }

  /** Enters this monitor when the guard is satisfied. Blocks indefinitely. */
  public void enterWhenUninterruptibly(Guard guard) {
    if (guard.monitor != this) {
      throw new IllegalMonitorStateException();
    }
    final ReentrantLock lock = this.lock;
    boolean signalBeforeWaiting = lock.isHeldByCurrentThread();
    lock.lock();

    boolean satisfied = false;
    try {
      if (!guard.isSatisfied()) {
        awaitUninterruptibly(guard, signalBeforeWaiting);
      }
      satisfied = true;
    } finally {
      if (!satisfied) {
        leave();
      }
    }
  }

  /**
   * Enters this monitor when the guard is satisfied. Blocks at most the given time, including both
   * the time to acquire the lock and the time to wait for the guard to be satisfied.
   *
   * @return whether the monitor was entered, which guarantees that the guard is now satisfied
   */
  @SuppressWarnings("GoodTime") // should accept a java.time.Duration
  public boolean enterWhenUninterruptibly(Guard guard, long time, TimeUnit unit) {
    final long timeoutNanos = toSafeNanos(time, unit);
    if (guard.monitor != this) {
      throw new IllegalMonitorStateException();
    }
    final ReentrantLock lock = this.lock;
    long startTime = 0L;
    boolean signalBeforeWaiting = lock.isHeldByCurrentThread();
    boolean interrupted = Thread.interrupted();
    try {
      if (fair || !lock.tryLock()) {
        startTime = initNanoTime(timeoutNanos);
        for (long remainingNanos = timeoutNanos; ; ) {
          try {
            if (lock.tryLock(remainingNanos, TimeUnit.NANOSECONDS)) {
              break;
            } else {
              return false;
            }
          } catch (InterruptedException interrupt) {
            interrupted = true;
            remainingNanos = remainingNanos(startTime, timeoutNanos);
          }
        }
      }

      boolean satisfied = false;
      try {
        while (true) {
          try {
            if (guard.isSatisfied()) {
              satisfied = true;
            } else {
              final long remainingNanos;
              if (startTime == 0L) {
                startTime = initNanoTime(timeoutNanos);
                remainingNanos = timeoutNanos;
              } else {
                remainingNanos = remainingNanos(startTime, timeoutNanos);
              }
              satisfied = awaitNanos(guard, remainingNanos, signalBeforeWaiting);
            }
            return satisfied;
          } catch (InterruptedException interrupt) {
            interrupted = true;
            signalBeforeWaiting = false;
          }
        }
      } finally {
        if (!satisfied) {
          lock.unlock(); // No need to signal if timed out
        }
      }
    } finally {
      if (interrupted) {
        Thread.currentThread().interrupt();
      }
    }
  }

  /**
   * Enters this monitor if the guard is satisfied. Blocks indefinitely acquiring the lock, but does
   * not wait for the guard to be satisfied.
   *
   * @return whether the monitor was entered, which guarantees that the guard is now satisfied
   */
  public boolean enterIf(Guard guard) {
    if (guard.monitor != this) {
      throw new IllegalMonitorStateException();
    }
    final ReentrantLock lock = this.lock;
    lock.lock();

    boolean satisfied = false;
    try {
      return satisfied = guard.isSatisfied();
    } finally {
      if (!satisfied) {
        lock.unlock();
      }
    }
  }

  /**
   * Enters this monitor if the guard is satisfied. Blocks at most the given time acquiring the
   * lock, but does not wait for the guard to be satisfied.
   *
   * @return whether the monitor was entered, which guarantees that the guard is now satisfied
   */
  @SuppressWarnings("GoodTime") // should accept a java.time.Duration
  public boolean enterIf(Guard guard, long time, TimeUnit unit) {
    if (guard.monitor != this) {
      throw new IllegalMonitorStateException();
    }
    if (!enter(time, unit)) {
      return false;
    }

    boolean satisfied = false;
    try {
      return satisfied = guard.isSatisfied();
    } finally {
      if (!satisfied) {
        lock.unlock();
      }
    }
  }

  /**
   * Enters this monitor if the guard is satisfied. Blocks indefinitely acquiring the lock, but does
   * not wait for the guard to be satisfied, and may be interrupted.
   *
   * @return whether the monitor was entered, which guarantees that the guard is now satisfied
   * @throws InterruptedException if interrupted while waiting
   */
  public boolean enterIfInterruptibly(Guard guard) throws InterruptedException {
    if (guard.monitor != this) {
      throw new IllegalMonitorStateException();
    }
    final ReentrantLock lock = this.lock;
    lock.lockInterruptibly();

    boolean satisfied = false;
    try {
      return satisfied = guard.isSatisfied();
    } finally {
      if (!satisfied) {
        lock.unlock();
      }
    }
  }

  /**
   * Enters this monitor if the guard is satisfied. Blocks at most the given time acquiring the
   * lock, but does not wait for the guard to be satisfied, and may be interrupted.
   *
   * @return whether the monitor was entered, which guarantees that the guard is now satisfied
   */
  @SuppressWarnings("GoodTime") // should accept a java.time.Duration
  public boolean enterIfInterruptibly(Guard guard, long time, TimeUnit unit)
      throws InterruptedException {
    if (guard.monitor != this) {
      throw new IllegalMonitorStateException();
    }
    final ReentrantLock lock = this.lock;
    if (!lock.tryLock(time, unit)) {
      return false;
    }

    boolean satisfied = false;
    try {
      return satisfied = guard.isSatisfied();
    } finally {
      if (!satisfied) {
        lock.unlock();
      }
    }
  }

  /**
   * Enters this monitor if it is possible to do so immediately and the guard is satisfied. Does not
   * block acquiring the lock and does not wait for the guard to be satisfied.
   *
   * <p><b>Note:</b> This method disregards the fairness setting of this monitor.
   *
   * @return whether the monitor was entered, which guarantees that the guard is now satisfied
   */
  public boolean tryEnterIf(Guard guard) {
    if (guard.monitor != this) {
      throw new IllegalMonitorStateException();
    }
    final ReentrantLock lock = this.lock;
    if (!lock.tryLock()) {
      return false;
    }

    boolean satisfied = false;
    try {
      return satisfied = guard.isSatisfied();
    } finally {
      if (!satisfied) {
        lock.unlock();
      }
    }
  }

  /**
   * Waits for the guard to be satisfied. Waits indefinitely, but may be interrupted. May be called
   * only by a thread currently occupying this monitor.
   *
   * @throws InterruptedException if interrupted while waiting
   */
  public void waitFor(Guard guard) throws InterruptedException {
    if (!((guard.monitor == this) && lock.isHeldByCurrentThread())) {
      throw new IllegalMonitorStateException();
    }
    if (!guard.isSatisfied()) {
      await(guard, true);
    }
  }

  /**
   * Waits for the guard to be satisfied. Waits at most the given time, and may be interrupted. May
   * be called only by a thread currently occupying this monitor.
   *
   * @return whether the guard is now satisfied
   * @throws InterruptedException if interrupted while waiting
   */
  @SuppressWarnings("GoodTime") // should accept a java.time.Duration
  public boolean waitFor(Guard guard, long time, TimeUnit unit) throws InterruptedException {
    final long timeoutNanos = toSafeNanos(time, unit);
    if (!((guard.monitor == this) && lock.isHeldByCurrentThread())) {
      throw new IllegalMonitorStateException();
    }
    if (guard.isSatisfied()) {
      return true;
    }
    if (Thread.interrupted()) {
      throw new InterruptedException();
    }
    return awaitNanos(guard, timeoutNanos, true);
  }

  /**
   * Waits for the guard to be satisfied. Waits indefinitely. May be called only by a thread
   * currently occupying this monitor.
   */
  public void waitForUninterruptibly(Guard guard) {
    if (!((guard.monitor == this) && lock.isHeldByCurrentThread())) {
      throw new IllegalMonitorStateException();
    }
    if (!guard.isSatisfied()) {
      awaitUninterruptibly(guard, true);
    }
  }

  /**
   * Waits for the guard to be satisfied. Waits at most the given time. May be called only by a
   * thread currently occupying this monitor.
   *
   * @return whether the guard is now satisfied
   */
  @SuppressWarnings("GoodTime") // should accept a java.time.Duration
  public boolean waitForUninterruptibly(Guard guard, long time, TimeUnit unit) {
    final long timeoutNanos = toSafeNanos(time, unit);
    if (!((guard.monitor == this) && lock.isHeldByCurrentThread())) {
      throw new IllegalMonitorStateException();
    }
    if (guard.isSatisfied()) {
      return true;
    }
    boolean signalBeforeWaiting = true;
    final long startTime = initNanoTime(timeoutNanos);
    boolean interrupted = Thread.interrupted();
    try {
      for (long remainingNanos = timeoutNanos; ; ) {
        try {
          return awaitNanos(guard, remainingNanos, signalBeforeWaiting);
        } catch (InterruptedException interrupt) {
          interrupted = true;
          if (guard.isSatisfied()) {
            return true;
          }
          signalBeforeWaiting = false;
          remainingNanos = remainingNanos(startTime, timeoutNanos);
        }
      }
    } finally {
      if (interrupted) {
        Thread.currentThread().interrupt();
      }
    }
  }

  /** Leaves this monitor. May be called only by a thread currently occupying this monitor. */
  public void leave() {
    final ReentrantLock lock = this.lock;
    try {
      // No need to signal if we will still be holding the lock when we return
      if (lock.getHoldCount() == 1) {
        signalNextWaiter();
      }
    } finally {
      lock.unlock(); // Will throw IllegalMonitorStateException if not held
    }
  }

  /** Returns whether this monitor is using a fair ordering policy. */
  public boolean isFair() {
    return fair;
  }

  /**
   * Returns whether this monitor is occupied by any thread. This method is designed for use in
   * monitoring of the system state, not for synchronization control.
   */
  public boolean isOccupied() {
    return lock.isLocked();
  }

  /**
   * Returns whether the current thread is occupying this monitor (has entered more times than it
   * has left).
   */
  public boolean isOccupiedByCurrentThread() {
    return lock.isHeldByCurrentThread();
  }

  /**
   * Returns the number of times the current thread has entered this monitor in excess of the number
   * of times it has left. Returns 0 if the current thread is not occupying this monitor.
   */
  public int getOccupiedDepth() {
    return lock.getHoldCount();
  }

  /**
   * Returns an estimate of the number of threads waiting to enter this monitor. The value is only
   * an estimate because the number of threads may change dynamically while this method traverses
   * internal data structures. This method is designed for use in monitoring of the system state,
   * not for synchronization control.
   */
  public int getQueueLength() {
    return lock.getQueueLength();
  }

  /**
   * Returns whether any threads are waiting to enter this monitor. Note that because cancellations
   * may occur at any time, a {@code true} return does not guarantee that any other thread will ever
   * enter this monitor. This method is designed primarily for use in monitoring of the system
   * state.
   */
  public boolean hasQueuedThreads() {
    return lock.hasQueuedThreads();
  }

  /**
   * Queries whether the given thread is waiting to enter this monitor. Note that because
   * cancellations may occur at any time, a {@code true} return does not guarantee that this thread
   * will ever enter this monitor. This method is designed primarily for use in monitoring of the
   * system state.
   */
  public boolean hasQueuedThread(Thread thread) {
    return lock.hasQueuedThread(thread);
  }

  /**
   * Queries whether any threads are waiting for the given guard to become satisfied. Note that
   * because timeouts and interrupts may occur at any time, a {@code true} return does not guarantee
   * that the guard becoming satisfied in the future will awaken any threads. This method is
   * designed primarily for use in monitoring of the system state.
   */
  public boolean hasWaiters(Guard guard) {
    return getWaitQueueLength(guard) > 0;
  }

  /**
   * Returns an estimate of the number of threads waiting for the given guard to become satisfied.
   * Note that because timeouts and interrupts may occur at any time, the estimate serves only as an
   * upper bound on the actual number of waiters. This method is designed for use in monitoring of
   * the system state, not for synchronization control.
   */
  public int getWaitQueueLength(Guard guard) {
    if (guard.monitor != this) {
      throw new IllegalMonitorStateException();
    }
    lock.lock();
    try {
      return guard.waiterCount;
    } finally {
      lock.unlock();
    }
  }

  /**
   * Returns unit.toNanos(time), additionally ensuring the returned value is not at risk of
   * overflowing or underflowing, by bounding the value between 0 and (Long.MAX_VALUE / 4) * 3.
   * Actually waiting for more than 219 years is not supported!
   */
  private static long toSafeNanos(long time, TimeUnit unit) {
    long timeoutNanos = unit.toNanos(time);
    return Longs.constrainToRange(timeoutNanos, 0L, (Long.MAX_VALUE / 4) * 3);
  }

  /**
   * Returns System.nanoTime() unless the timeout has already elapsed. Returns 0L if and only if the
   * timeout has already elapsed.
   */
  private static long initNanoTime(long timeoutNanos) {
    if (timeoutNanos <= 0L) {
      return 0L;
    } else {
      long startTime = System.nanoTime();
      return (startTime == 0L) ? 1L : startTime;
    }
  }

  /**
   * Returns the remaining nanos until the given timeout, or 0L if the timeout has already elapsed.
   * Caller must have previously sanitized timeoutNanos using toSafeNanos.
   */
  private static long remainingNanos(long startTime, long timeoutNanos) {
    // assert timeoutNanos == 0L || startTime != 0L;

    // TODO : NOT CORRECT, BUT TESTS PASS ANYWAYS!
    // if (true) return timeoutNanos;
    // ONLY 2 TESTS FAIL IF WE DO:
    // if (true) return 0;

    return (timeoutNanos <= 0L) ? 0L : timeoutNanos - (System.nanoTime() - startTime);
  }

  /**
   * Signals some other thread waiting on a satisfied guard, if one exists.
   *
   * <p>We manage calls to this method carefully, to signal only when necessary, but never losing a
   * signal, which is the classic problem of this kind of concurrency construct. We must signal if
   * the current thread is about to relinquish the lock and may have changed the state protected by
   * the monitor, thereby causing some guard to be satisfied.
   *
   * <p>In addition, any thread that has been signalled when its guard was satisfied acquires the
   * responsibility of signalling the next thread when it again relinquishes the lock. Unlike a
   * normal Condition, there is no guarantee that an interrupted thread has not been signalled,
   * since the concurrency control must manage multiple Conditions. So this method must generally be
   * called when waits are interrupted.
   *
   * <p>On the other hand, if a signalled thread wakes up to discover that its guard is still not
   * satisfied, it does *not* need to call this method before returning to wait. This can only
   * happen due to spurious wakeup (ignorable) or another thread acquiring the lock before the
   * current thread can and returning the guard to the unsatisfied state. In the latter case the
   * other thread (last thread modifying the state protected by the monitor) takes over the
   * responsibility of signalling the next waiter.
   *
   * <p>This method must not be called from within a beginWaitingFor/endWaitingFor block, or else
   * the current thread's guard might be mistakenly signalled, leading to a lost signal.
   */
  @GuardedBy("lock")
  private void signalNextWaiter() {
    for (Guard guard = activeGuards; guard != null; guard = guard.next) {
      if (isSatisfied(guard)) {
        guard.condition.signal();
        break;
      }
    }
  }

  /**
   * Exactly like signalNextWaiter, but caller guarantees that guardToSkip need not be considered,
   * because caller has previously checked that guardToSkip.isSatisfied() returned false. An
   * optimization for the case that guardToSkip.isSatisfied() may be expensive.
   *
   * <p>We decided against using this method, since in practice, isSatisfied() is likely to be very
   * cheap (typically one field read). Resurrect this method if you find that not to be true.
   */
  //   @GuardedBy("lock")
  //   private void signalNextWaiterSkipping(Guard guardToSkip) {
  //     for (Guard guard = activeGuards; guard != null; guard = guard.next) {
  //       if (guard != guardToSkip && isSatisfied(guard)) {
  //         guard.condition.signal();
  //         break;
  //       }
  //     }
  //   }

  /**
   * Exactly like guard.isSatisfied(), but in addition signals all waiting threads in the (hopefully
   * unlikely) event that isSatisfied() throws.
   */
  @GuardedBy("lock")
  private boolean isSatisfied(Guard guard) {
    try {
      return guard.isSatisfied();
    } catch (Throwable throwable) {
      // Any Exception is either a RuntimeException or sneaky checked exception.
      signalAllWaiters();
      throw throwable;
    }
  }

  /** Signals all threads waiting on guards. */
  @GuardedBy("lock")
  private void signalAllWaiters() {
    for (Guard guard = activeGuards; guard != null; guard = guard.next) {
      guard.condition.signalAll();
    }
  }

  /** Records that the current thread is about to wait on the specified guard. */
  @GuardedBy("lock")
  private void beginWaitingFor(Guard guard) {
    int waiters = guard.waiterCount++;
    if (waiters == 0) {
      // push guard onto activeGuards
      guard.next = activeGuards;
      activeGuards = guard;
    }
  }

  /** Records that the current thread is no longer waiting on the specified guard. */
  @GuardedBy("lock")
  private void endWaitingFor(Guard guard) {
    int waiters = --guard.waiterCount;
    if (waiters == 0) {
      // unlink guard from activeGuards
      for (Guard p = activeGuards, pred = null; ; pred = p, p = p.next) {
        if (p == guard) {
          if (pred == null) {
            activeGuards = p.next;
          } else {
            pred.next = p.next;
          }
          p.next = null; // help GC
          break;
        }
      }
    }
  }

  /*
   * Methods that loop waiting on a guard's condition until the guard is satisfied, while recording
   * this fact so that other threads know to check our guard and signal us. It's caller's
   * responsibility to ensure that the guard is *not* currently satisfied.
   */

  @GuardedBy("lock")
  private void await(Guard guard, boolean signalBeforeWaiting) throws InterruptedException {
    if (signalBeforeWaiting) {
      signalNextWaiter();
    }
    beginWaitingFor(guard);
    try {
      do {
        guard.condition.await();
      } while (!guard.isSatisfied());
    } finally {
      endWaitingFor(guard);
    }
  }

  @GuardedBy("lock")
  private void awaitUninterruptibly(Guard guard, boolean signalBeforeWaiting) {
    if (signalBeforeWaiting) {
      signalNextWaiter();
    }
    beginWaitingFor(guard);
    try {
      do {
        guard.condition.awaitUninterruptibly();
      } while (!guard.isSatisfied());
    } finally {
      endWaitingFor(guard);
    }
  }

  /** Caller should check before calling that guard is not satisfied. */
  @GuardedBy("lock")
  private boolean awaitNanos(Guard guard, long nanos, boolean signalBeforeWaiting)
      throws InterruptedException {
    boolean firstTime = true;
    try {
      do {
        if (nanos <= 0L) {
          return false;
        }
        if (firstTime) {
          if (signalBeforeWaiting) {
            signalNextWaiter();
          }
          beginWaitingFor(guard);
          firstTime = false;
        }
        nanos = guard.condition.awaitNanos(nanos);
      } while (!guard.isSatisfied());
      return true;
    } finally {
      if (!firstTime) {
        endWaitingFor(guard);
      }
    }
  }
}
