/*
 * Copyright (C) 2010 The Guava Authors
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
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import javax.annotation.Nullable;
import javax.annotation.concurrent.GuardedBy;

/**
 * A synchronization abstraction supporting waiting on arbitrary boolean conditions.
 *
 * <p>This class is intended as a replacement for {@link ReentrantLock}. Code using {@code Monitor}
 * is less error-prone and more readable than code using {@code ReentrantLock}, without significant
 * performance loss. {@code Monitor} even has the potential for performance gain by optimizing the
 * evaluation and signaling of conditions.  Signaling is entirely
 * <a href="http://en.wikipedia.org/wiki/Monitor_(synchronization)#Implicit_signaling">
 * implicit</a>.
 * By eliminating explicit signaling, this class can guarantee that only one thread is awakened
 * when a condition becomes true (no "signaling storms" due to use of {@link
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
 * monitor cleanly: <pre>   {@code
 *
 *   monitor.enter();
 *   try {
 *     // do things while occupying the monitor
 *   } finally {
 *     monitor.leave();
 *   }}</pre>
 *
 * A call to any of the <i>enter</i> methods with <b>boolean</b> return type should always appear as
 * the condition of an <i>if</i> statement containing a <i>try/finally</i> block to ensure that the
 * current thread leaves the monitor cleanly: <pre>   {@code
 *
 *   if (monitor.tryEnter()) {
 *     try {
 *       // do things while occupying the monitor
 *     } finally {
 *       monitor.leave();
 *     }
 *   } else {
 *     // do other things since the monitor was not available
 *   }}</pre>
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
 * logical conditions being awaited. <pre>   {@code
 *
 *   public class SafeBox<V> {
 *     private V value;
 *
 *     public synchronized V get() throws InterruptedException {
 *       while (value == null) {
 *         wait();
 *       }
 *       V result = value;
 *       value = null;
 *       notifyAll();
 *       return result;
 *     }
 *
 *     public synchronized void set(V newValue) throws InterruptedException {
 *       while (value != null) {
 *         wait();
 *       }
 *       value = newValue;
 *       notifyAll();
 *     }
 *   }}</pre>
 * 
 * <h3>{@code ReentrantLock}</h3>
 * 
 * <p>This version is much more verbose than the {@code synchronized} version, and still suffers
 * from the need for the programmer to remember to use {@code while} instead of {@code if}.
 * However, one advantage is that we can introduce two separate {@code Condition} objects, which
 * allows us to use {@code signal()} instead of {@code signalAll()}, which may be a performance
 * benefit. <pre>   {@code
 *
 *   public class SafeBox<V> {
 *     private final ReentrantLock lock = new ReentrantLock();
 *     private final Condition valuePresent = lock.newCondition();
 *     private final Condition valueAbsent = lock.newCondition();
 *     private V value;
 *
 *     public V get() throws InterruptedException {
 *       lock.lock();
 *       try {
 *         while (value == null) {
 *           valuePresent.await();
 *         }
 *         V result = value;
 *         value = null;
 *         valueAbsent.signal();
 *         return result;
 *       } finally {
 *         lock.unlock();
 *       }
 *     }
 *
 *     public void set(V newValue) throws InterruptedException {
 *       lock.lock();
 *       try {
 *         while (value != null) {
 *           valueAbsent.await();
 *         }
 *         value = newValue;
 *         valuePresent.signal();
 *       } finally {
 *         lock.unlock();
 *       }
 *     }
 *   }}</pre>
 * 
 * <h3>{@code Monitor}</h3>
 * 
 * <p>This version adds some verbosity around the {@code Guard} objects, but removes that same
 * verbosity, and more, from the {@code get} and {@code set} methods. {@code Monitor} implements the
 * same efficient signaling as we had to hand-code in the {@code ReentrantLock} version above.
 * Finally, the programmer no longer has to hand-code the wait loop, and therefore doesn't have to
 * remember to use {@code while} instead of {@code if}. <pre>   {@code
 *
 *   public class SafeBox<V> {
 *     private final Monitor monitor = new Monitor();
 *     private final Monitor.Guard valuePresent = new Monitor.Guard(monitor) {
 *       public boolean isSatisfied() {
 *         return value != null;
 *       }
 *     };
 *     private final Monitor.Guard valueAbsent = new Monitor.Guard(monitor) {
 *       public boolean isSatisfied() {
 *         return value == null;
 *       }
 *     };
 *     private V value;
 *
 *     public V get() throws InterruptedException {
 *       monitor.enterWhen(valuePresent);
 *       try {
 *         V result = value;
 *         value = null;
 *         return result;
 *       } finally {
 *         monitor.leave();
 *       }
 *     }
 *
 *     public void set(V newValue) throws InterruptedException {
 *       monitor.enterWhen(valueAbsent);
 *       try {
 *         value = newValue;
 *       } finally {
 *         monitor.leave();
 *       }
 *     }
 *   }}</pre>
 * 
 * @author Justin T. Sampson
 * @since 10.0
 */
@Beta
public final class Monitor {
  // TODO: Use raw LockSupport or AbstractQueuedSynchronizer instead of ReentrantLock.

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
  @Beta
  public abstract static class Guard {
    
    final Monitor monitor;
    final Condition condition;

    @GuardedBy("monitor.lock")
    int waiterCount = 0;

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

    @Override
    public final boolean equals(Object other) {
      // Overridden as final to ensure identity semantics in Monitor.activeGuards.
      return this == other;
    }
    
    @Override
    public final int hashCode() {
      // Overridden as final to ensure identity semantics in Monitor.activeGuards.
      return super.hashCode();
    }

  }

  /**
   * Whether this monitor is fair.
   */
  private final boolean fair;
  
  /**
   * The lock underlying this monitor.
   */
  private final ReentrantLock lock;

  /**
   * The guards associated with this monitor that currently have waiters ({@code waiterCount > 0}).
   * This is an ArrayList rather than, say, a HashSet so that iteration and almost all adds don't
   * incur any object allocation overhead.
   */
  @GuardedBy("lock")
  private final ArrayList<Guard> activeGuards = Lists.newArrayListWithCapacity(1);

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
   *        fast) one
   */
  public Monitor(boolean fair) {
    this.fair = fair;
    this.lock = new ReentrantLock(fair);
  }

  /**
   * Enters this monitor. Blocks indefinitely.
   */
  public void enter() {
    lock.lock();
  }

  /**
   * Enters this monitor. Blocks indefinitely, but may be interrupted.
   */
  public void enterInterruptibly() throws InterruptedException {
    lock.lockInterruptibly();
  }

  /**
   * Enters this monitor. Blocks at most the given time.
   *
   * @return whether the monitor was entered
   */
  public boolean enter(long time, TimeUnit unit) {
    final ReentrantLock lock = this.lock;
    if (!fair && lock.tryLock()) {
      return true;
    }
    long startNanos = System.nanoTime();
    long timeoutNanos = unit.toNanos(time);
    long remainingNanos = timeoutNanos;
    boolean interruptIgnored = false;
    try {
      while (true) {
        try {
          return lock.tryLock(remainingNanos, TimeUnit.NANOSECONDS);
        } catch (InterruptedException ignored) {
          interruptIgnored = true;
          remainingNanos = (timeoutNanos - (System.nanoTime() - startNanos));
        }
      }
    } finally {
      if (interruptIgnored) {
        Thread.currentThread().interrupt();
      }
    }
  }

  /**
   * Enters this monitor. Blocks at most the given time, and may be interrupted.
   *
   * @return whether the monitor was entered
   */
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
   */
  public void enterWhen(Guard guard) throws InterruptedException {
    if (guard.monitor != this) {
      throw new IllegalMonitorStateException();
    }
    final ReentrantLock lock = this.lock;
    boolean reentrant = lock.isHeldByCurrentThread();
    boolean success = false;
    lock.lockInterruptibly();
    try {
      waitInterruptibly(guard, reentrant);
      success = true;
    } finally {
      if (!success) {
        lock.unlock();
      }
    }
  }

  /**
   * Enters this monitor when the guard is satisfied. Blocks indefinitely.
   */
  public void enterWhenUninterruptibly(Guard guard) {
    if (guard.monitor != this) {
      throw new IllegalMonitorStateException();
    }
    final ReentrantLock lock = this.lock;
    boolean reentrant = lock.isHeldByCurrentThread();
    boolean success = false;
    lock.lock();
    try {
      waitUninterruptibly(guard, reentrant);
      success = true;
    } finally {
      if (!success) {
        lock.unlock();
      }
    }
  }

  /**
   * Enters this monitor when the guard is satisfied. Blocks at most the given time, including both
   * the time to acquire the lock and the time to wait for the guard to be satisfied, and may be
   * interrupted.
   *
   * @return whether the monitor was entered
   */
  public boolean enterWhen(Guard guard, long time, TimeUnit unit) throws InterruptedException {
    if (guard.monitor != this) {
      throw new IllegalMonitorStateException();
    }
    final ReentrantLock lock = this.lock;
    boolean reentrant = lock.isHeldByCurrentThread();
    long remainingNanos;
    if (!fair && lock.tryLock()) {
      remainingNanos = unit.toNanos(time);
    } else {
      long startNanos = System.nanoTime();
      if (!lock.tryLock(time, unit)) {
        return false;
      }
      remainingNanos = unit.toNanos(time) - (System.nanoTime() - startNanos);
    }
    boolean satisfied = false;
    try {
      satisfied = waitInterruptibly(guard, remainingNanos, reentrant);
    } finally {
      if (!satisfied) {
        lock.unlock();
      }
    }
    return satisfied;
  }

  /**
   * Enters this monitor when the guard is satisfied. Blocks at most the given time, including
   * both the time to acquire the lock and the time to wait for the guard to be satisfied.
   *
   * @return whether the monitor was entered
   */
  public boolean enterWhenUninterruptibly(Guard guard, long time, TimeUnit unit) {
    if (guard.monitor != this) {
      throw new IllegalMonitorStateException();
    }
    final ReentrantLock lock = this.lock;
    boolean reentrant = lock.isHeldByCurrentThread();
    boolean interruptIgnored = false;
    try {
      long remainingNanos;
      if (!fair && lock.tryLock()) {
        remainingNanos = unit.toNanos(time);
      } else {
        long startNanos = System.nanoTime();
        long timeoutNanos = unit.toNanos(time);
        remainingNanos = timeoutNanos;
        while (true) {
          try {
            if (lock.tryLock(remainingNanos, TimeUnit.NANOSECONDS)) {
              break;
            } else {
              return false;
            }
          } catch (InterruptedException ignored) {
            interruptIgnored = true;
          } finally {
            remainingNanos = (timeoutNanos - (System.nanoTime() - startNanos));
          }
        }
      }
      boolean satisfied = false;
      try {
        satisfied = waitUninterruptibly(guard, remainingNanos, reentrant);
      } finally {
        if (!satisfied) {
          lock.unlock();
        }
      }
      return satisfied;
    } finally {
      if (interruptIgnored) {
        Thread.currentThread().interrupt();
      }
    }
  }

  /**
   * Enters this monitor if the guard is satisfied. Blocks indefinitely acquiring the lock, but
   * does not wait for the guard to be satisfied.
   *
   * @return whether the monitor was entered
   */
  public boolean enterIf(Guard guard) {
    if (guard.monitor != this) {
      throw new IllegalMonitorStateException();
    }
    final ReentrantLock lock = this.lock;
    lock.lock();
    boolean satisfied = false;
    try {
      satisfied = guard.isSatisfied();
    } finally {
      if (!satisfied) {
        lock.unlock();
      }
    }
    return satisfied;
  }

  /**
   * Enters this monitor if the guard is satisfied. Blocks indefinitely acquiring the lock, but does
   * not wait for the guard to be satisfied, and may be interrupted.
   *
   * @return whether the monitor was entered
   */
  public boolean enterIfInterruptibly(Guard guard) throws InterruptedException {
    if (guard.monitor != this) {
      throw new IllegalMonitorStateException();
    }
    final ReentrantLock lock = this.lock;
    lock.lockInterruptibly();
    boolean satisfied = false;
    try {
      satisfied = guard.isSatisfied();
    } finally {
      if (!satisfied) {
        lock.unlock();
      }
    }
    return satisfied;
  }

  /**
   * Enters this monitor if the guard is satisfied. Blocks at most the given time acquiring the
   * lock, but does not wait for the guard to be satisfied.
   *
   * @return whether the monitor was entered
   */
  public boolean enterIf(Guard guard, long time, TimeUnit unit) {
    if (guard.monitor != this) {
      throw new IllegalMonitorStateException();
    }
    final ReentrantLock lock = this.lock;
    if (!enter(time, unit)) {
      return false;
    }
    boolean satisfied = false;
    try {
      satisfied = guard.isSatisfied();
    } finally {
      if (!satisfied) {
        lock.unlock();
      }
    }
    return satisfied;
  }

  /**
   * Enters this monitor if the guard is satisfied. Blocks at most the given time acquiring the
   * lock, but does not wait for the guard to be satisfied, and may be interrupted.
   *
   * @return whether the monitor was entered
   */
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
      satisfied = guard.isSatisfied();
    } finally {
      if (!satisfied) {
        lock.unlock();
      }
    }
    return satisfied;
  }

  /**
   * Enters this monitor if it is possible to do so immediately and the guard is satisfied. Does not
   * block acquiring the lock and does not wait for the guard to be satisfied.
   *
   * <p><b>Note:</b> This method disregards the fairness setting of this monitor.
   *
   * @return whether the monitor was entered
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
      satisfied = guard.isSatisfied();
    } finally {
      if (!satisfied) {
        lock.unlock();
      }
    }
    return satisfied;
  }

  /**
   * Waits for the guard to be satisfied. Waits indefinitely, but may be interrupted. May be
   * called only by a thread currently occupying this monitor.
   */
  public void waitFor(Guard guard) throws InterruptedException {
    if (guard.monitor != this) {
      throw new IllegalMonitorStateException();
    }
    if (!lock.isHeldByCurrentThread()) {
      throw new IllegalMonitorStateException();
    }
    waitInterruptibly(guard, true);
  }

  /**
   * Waits for the guard to be satisfied. Waits indefinitely. May be called only by a thread
   * currently occupying this monitor.
   */
  public void waitForUninterruptibly(Guard guard) {
    if (guard.monitor != this) {
      throw new IllegalMonitorStateException();
    }
    if (!lock.isHeldByCurrentThread()) {
      throw new IllegalMonitorStateException();
    }
    waitUninterruptibly(guard, true);
  }

  /**
   * Waits for the guard to be satisfied. Waits at most the given time, and may be interrupted.
   * May be called only by a thread currently occupying this monitor.
   *
   * @return whether the guard is now satisfied
   */
  public boolean waitFor(Guard guard, long time, TimeUnit unit) throws InterruptedException {
    if (guard.monitor != this) {
      throw new IllegalMonitorStateException();
    }
    if (!lock.isHeldByCurrentThread()) {
      throw new IllegalMonitorStateException();
    }
    return waitInterruptibly(guard, unit.toNanos(time), true);
  }

  /**
   * Waits for the guard to be satisfied. Waits at most the given time. May be called only by a
   * thread currently occupying this monitor.
   *
   * @return whether the guard is now satisfied
   */
  public boolean waitForUninterruptibly(Guard guard, long time, TimeUnit unit) {
    if (guard.monitor != this) {
      throw new IllegalMonitorStateException();
    }
    if (!lock.isHeldByCurrentThread()) {
      throw new IllegalMonitorStateException();
    }
    return waitUninterruptibly(guard, unit.toNanos(time), true);
  }

  /**
   * Leaves this monitor. May be called only by a thread currently occupying this monitor.
   */
  public void leave() {
    final ReentrantLock lock = this.lock;
    if (!lock.isHeldByCurrentThread()) {
      throw new IllegalMonitorStateException();
    }
    try {
      signalConditionsOfSatisfiedGuards(null);
    } finally {
      lock.unlock();
    }
  }

  /**
   * Returns whether this monitor is using a fair ordering policy.
   */
  public boolean isFair() {
    return lock.isFair();
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
    if (guard.monitor != this) {
      throw new IllegalMonitorStateException();
    }
    lock.lock();
    try {
      return guard.waiterCount > 0;
    } finally {
      lock.unlock();
    }
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

  @GuardedBy("lock")
  private void signalConditionsOfSatisfiedGuards(@Nullable Guard interruptedGuard) {
    final ArrayList<Guard> guards = this.activeGuards;
    final int guardCount = guards.size();
    try {
      for (int i = 0; i < guardCount; i++) {
        Guard guard = guards.get(i);
        if ((guard == interruptedGuard) && (guard.waiterCount == 1)) {
          // That one waiter was just interrupted and is throwing InterruptedException rather than
          // paying attention to the guard being satisfied, so find another waiter on another guard.
          continue;
        }
        if (guard.isSatisfied()) {
          guard.condition.signal();
          return;
        }
      }
    } catch (Throwable throwable) {
      for (int i = 0; i < guardCount; i++) {
        Guard guard = guards.get(i);
        guard.condition.signalAll();
      }
      throw Throwables.propagate(throwable);
    }
  }
  
  @GuardedBy("lock")
  private void incrementWaiters(Guard guard) {
    int waiters = guard.waiterCount++;
    if (waiters == 0) {
      activeGuards.add(guard);
    }
  }

  @GuardedBy("lock")
  private void decrementWaiters(Guard guard) {
    int waiters = --guard.waiterCount;
    if (waiters == 0) {
      activeGuards.remove(guard);
    }
  }

  @GuardedBy("lock")
  private void waitInterruptibly(Guard guard, boolean signalBeforeWaiting)
      throws InterruptedException {
    if (!guard.isSatisfied()) {
      if (signalBeforeWaiting) {
        signalConditionsOfSatisfiedGuards(null);
      }
      incrementWaiters(guard);
      try {
        final Condition condition = guard.condition;
        do {
          try {
            condition.await();
          } catch (InterruptedException interrupt) {
            try {
              signalConditionsOfSatisfiedGuards(guard);
            } catch (Throwable throwable) {
              Thread.currentThread().interrupt();
              throw Throwables.propagate(throwable);
            }
            throw interrupt;
          }
        } while (!guard.isSatisfied());
      } finally {
        decrementWaiters(guard);
      }
    }
  }

  @GuardedBy("lock")
  private void waitUninterruptibly(Guard guard, boolean signalBeforeWaiting) {
    if (!guard.isSatisfied()) {
      if (signalBeforeWaiting) {
        signalConditionsOfSatisfiedGuards(null);
      }
      incrementWaiters(guard);
      try {
        final Condition condition = guard.condition;
        do {
          condition.awaitUninterruptibly();
        } while (!guard.isSatisfied());
      } finally {
        decrementWaiters(guard);
      }
    }
  }

  @GuardedBy("lock")
  private boolean waitInterruptibly(Guard guard, long remainingNanos, boolean signalBeforeWaiting)
      throws InterruptedException {
    if (!guard.isSatisfied()) {
      if (signalBeforeWaiting) {
        signalConditionsOfSatisfiedGuards(null);
      }
      incrementWaiters(guard);
      try {
        final Condition condition = guard.condition;
        do {
          if (remainingNanos <= 0) {
            return false;
          }
          try {
            remainingNanos = condition.awaitNanos(remainingNanos);
          } catch (InterruptedException interrupt) {
            try {
              signalConditionsOfSatisfiedGuards(guard);
            } catch (Throwable throwable) {
              Thread.currentThread().interrupt();
              throw Throwables.propagate(throwable);
            }
            throw interrupt;
          }
        } while (!guard.isSatisfied());
      } finally {
        decrementWaiters(guard);
      }
    }
    return true;
  }

  @GuardedBy("lock")
  private boolean waitUninterruptibly(Guard guard, long timeoutNanos,
      boolean signalBeforeWaiting) {
    if (!guard.isSatisfied()) {
      long startNanos = System.nanoTime();
      if (signalBeforeWaiting) {
        signalConditionsOfSatisfiedGuards(null);
      }
      boolean interruptIgnored = false;
      try {
        incrementWaiters(guard);
        try {
          final Condition condition = guard.condition;
          long remainingNanos = timeoutNanos;
          do {
            if (remainingNanos <= 0) {
              return false;
            }
            try {
              remainingNanos = condition.awaitNanos(remainingNanos);
            } catch (InterruptedException ignored) {
              try {
                signalConditionsOfSatisfiedGuards(guard);
              } catch (Throwable throwable) {
                Thread.currentThread().interrupt();
                throw Throwables.propagate(throwable);
              }
              interruptIgnored = true;
              remainingNanos = (timeoutNanos - (System.nanoTime() - startNanos));
            }
          } while (!guard.isSatisfied());
        } finally {
          decrementWaiters(guard);
        }
      } finally {
        if (interruptIgnored) {
          Thread.currentThread().interrupt();
        }
      }
    }
    return true;
  }

}
