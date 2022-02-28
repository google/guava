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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.util.concurrent.Futures.immediateCancelledFuture;
import static com.google.common.util.concurrent.Internal.toNanosSaturated;
import static com.google.common.util.concurrent.MoreExecutors.directExecutor;
import static java.util.Objects.requireNonNull;
import static java.util.concurrent.TimeUnit.NANOSECONDS;

import com.google.common.annotations.GwtIncompatible;
import com.google.common.base.Supplier;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.errorprone.annotations.concurrent.GuardedBy;
import com.google.j2objc.annotations.WeakOuter;
import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.CheckForNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Base class for services that can implement {@link #startUp} and {@link #shutDown} but while in
 * the "running" state need to perform a periodic task. Subclasses can implement {@link #startUp},
 * {@link #shutDown} and also a {@link #runOneIteration} method that will be executed periodically.
 *
 * <p>This class uses the {@link ScheduledExecutorService} returned from {@link #executor} to run
 * the {@link #startUp} and {@link #shutDown} methods and also uses that service to schedule the
 * {@link #runOneIteration} that will be executed periodically as specified by its {@link
 * Scheduler}. When this service is asked to stop via {@link #stopAsync} it will cancel the periodic
 * task (but not interrupt it) and wait for it to stop before running the {@link #shutDown} method.
 *
 * <p>Subclasses are guaranteed that the life cycle methods ({@link #runOneIteration}, {@link
 * #startUp} and {@link #shutDown}) will never run concurrently. Notably, if any execution of {@link
 * #runOneIteration} takes longer than its schedule defines, then subsequent executions may start
 * late. Also, all life cycle methods are executed with a lock held, so subclasses can safely modify
 * shared state without additional synchronization necessary for visibility to later executions of
 * the life cycle methods.
 *
 * <h3>Usage Example</h3>
 *
 * <p>Here is a sketch of a service which crawls a website and uses the scheduling capabilities to
 * rate limit itself.
 *
 * <pre>{@code
 * class CrawlingService extends AbstractScheduledService {
 *   private Set<Uri> visited;
 *   private Queue<Uri> toCrawl;
 *   protected void startUp() throws Exception {
 *     toCrawl = readStartingUris();
 *   }
 *
 *   protected void runOneIteration() throws Exception {
 *     Uri uri = toCrawl.remove();
 *     Collection<Uri> newUris = crawl(uri);
 *     visited.add(uri);
 *     for (Uri newUri : newUris) {
 *       if (!visited.contains(newUri)) { toCrawl.add(newUri); }
 *     }
 *   }
 *
 *   protected void shutDown() throws Exception {
 *     saveUris(toCrawl);
 *   }
 *
 *   protected Scheduler scheduler() {
 *     return Scheduler.newFixedRateSchedule(0, 1, TimeUnit.SECONDS);
 *   }
 * }
 * }</pre>
 *
 * <p>This class uses the life cycle methods to read in a list of starting URIs and save the set of
 * outstanding URIs when shutting down. Also, it takes advantage of the scheduling functionality to
 * rate limit the number of queries we perform.
 *
 * @author Luke Sandberg
 * @since 11.0
 */
@GwtIncompatible
@ElementTypesAreNonnullByDefault
public abstract class AbstractScheduledService implements Service {
  private static final Logger logger = Logger.getLogger(AbstractScheduledService.class.getName());

  /**
   * A scheduler defines the policy for how the {@link AbstractScheduledService} should run its
   * task.
   *
   * <p>Consider using the {@link #newFixedDelaySchedule} and {@link #newFixedRateSchedule} factory
   * methods, these provide {@link Scheduler} instances for the common use case of running the
   * service with a fixed schedule. If more flexibility is needed then consider subclassing {@link
   * CustomScheduler}.
   *
   * @author Luke Sandberg
   * @since 11.0
   */
  public abstract static class Scheduler {
    /**
     * Returns a {@link Scheduler} that schedules the task using the {@link
     * ScheduledExecutorService#scheduleWithFixedDelay} method.
     *
     * @param initialDelay the time to delay first execution
     * @param delay the delay between the termination of one execution and the commencement of the
     *     next
     * @since 28.0
     */
    public static Scheduler newFixedDelaySchedule(Duration initialDelay, Duration delay) {
      return newFixedDelaySchedule(
          toNanosSaturated(initialDelay), toNanosSaturated(delay), NANOSECONDS);
    }

    /**
     * Returns a {@link Scheduler} that schedules the task using the {@link
     * ScheduledExecutorService#scheduleWithFixedDelay} method.
     *
     * @param initialDelay the time to delay first execution
     * @param delay the delay between the termination of one execution and the commencement of the
     *     next
     * @param unit the time unit of the initialDelay and delay parameters
     */
    @SuppressWarnings("GoodTime") // should accept a java.time.Duration
    public static Scheduler newFixedDelaySchedule(
        final long initialDelay, final long delay, final TimeUnit unit) {
      checkNotNull(unit);
      checkArgument(delay > 0, "delay must be > 0, found %s", delay);
      return new Scheduler() {
        @Override
        public Cancellable schedule(
            AbstractService service, ScheduledExecutorService executor, Runnable task) {
          return new FutureAsCancellable(
              executor.scheduleWithFixedDelay(task, initialDelay, delay, unit));
        }
      };
    }

    /**
     * Returns a {@link Scheduler} that schedules the task using the {@link
     * ScheduledExecutorService#scheduleAtFixedRate} method.
     *
     * @param initialDelay the time to delay first execution
     * @param period the period between successive executions of the task
     * @since 28.0
     */
    public static Scheduler newFixedRateSchedule(Duration initialDelay, Duration period) {
      return newFixedRateSchedule(
          toNanosSaturated(initialDelay), toNanosSaturated(period), NANOSECONDS);
    }

    /**
     * Returns a {@link Scheduler} that schedules the task using the {@link
     * ScheduledExecutorService#scheduleAtFixedRate} method.
     *
     * @param initialDelay the time to delay first execution
     * @param period the period between successive executions of the task
     * @param unit the time unit of the initialDelay and period parameters
     */
    @SuppressWarnings("GoodTime") // should accept a java.time.Duration
    public static Scheduler newFixedRateSchedule(
        final long initialDelay, final long period, final TimeUnit unit) {
      checkNotNull(unit);
      checkArgument(period > 0, "period must be > 0, found %s", period);
      return new Scheduler() {
        @Override
        public Cancellable schedule(
            AbstractService service, ScheduledExecutorService executor, Runnable task) {
          return new FutureAsCancellable(
              executor.scheduleAtFixedRate(task, initialDelay, period, unit));
        }
      };
    }

    /** Schedules the task to run on the provided executor on behalf of the service. */
    abstract Cancellable schedule(
        AbstractService service, ScheduledExecutorService executor, Runnable runnable);

    private Scheduler() {}
  }

  /* use AbstractService for state management */
  private final AbstractService delegate = new ServiceDelegate();

  @WeakOuter
  private final class ServiceDelegate extends AbstractService {

    // A handle to the running task so that we can stop it when a shutdown has been requested.
    // These two fields are volatile because their values will be accessed from multiple threads.
    @CheckForNull private volatile Cancellable runningTask;
    @CheckForNull private volatile ScheduledExecutorService executorService;

    // This lock protects the task so we can ensure that none of the template methods (startUp,
    // shutDown or runOneIteration) run concurrently with one another.
    // TODO(lukes): why don't we use ListenableFuture to sequence things? Then we could drop the
    // lock.
    private final ReentrantLock lock = new ReentrantLock();

    @WeakOuter
    class Task implements Runnable {
      @Override
      public void run() {
        lock.lock();
        try {
          /*
           * requireNonNull is safe because Task isn't run (or at least it doesn't succeed in taking
           * the lock) until after it's scheduled and the runningTask field is set.
           */
          if (requireNonNull(runningTask).isCancelled()) {
            // task may have been cancelled while blocked on the lock.
            return;
          }
          AbstractScheduledService.this.runOneIteration();
        } catch (Throwable t) {
          try {
            shutDown();
          } catch (Exception ignored) {
            logger.log(
                Level.WARNING,
                "Error while attempting to shut down the service after failure.",
                ignored);
          }
          notifyFailed(t);
          // requireNonNull is safe now, just as it was above.
          requireNonNull(runningTask).cancel(false); // prevent future invocations.
        } finally {
          lock.unlock();
        }
      }
    }

    private final Runnable task = new Task();

    @Override
    protected final void doStart() {
      executorService =
          MoreExecutors.renamingDecorator(
              executor(),
              new Supplier<String>() {
                @Override
                public String get() {
                  return serviceName() + " " + state();
                }
              });
      executorService.execute(
          new Runnable() {
            @Override
            public void run() {
              lock.lock();
              try {
                startUp();
                runningTask = scheduler().schedule(delegate, executorService, task);
                notifyStarted();
              } catch (Throwable t) {
                notifyFailed(t);
                if (runningTask != null) {
                  // prevent the task from running if possible
                  runningTask.cancel(false);
                }
              } finally {
                lock.unlock();
              }
            }
          });
    }

    @Override
    protected final void doStop() {
      // Both requireNonNull calls are safe because doStop can run only after a successful doStart.
      requireNonNull(runningTask);
      requireNonNull(executorService);
      runningTask.cancel(false);
      executorService.execute(
          new Runnable() {
            @Override
            public void run() {
              try {
                lock.lock();
                try {
                  if (state() != State.STOPPING) {
                    // This means that the state has changed since we were scheduled. This implies
                    // that an execution of runOneIteration has thrown an exception and we have
                    // transitioned to a failed state, also this means that shutDown has already
                    // been called, so we do not want to call it again.
                    return;
                  }
                  shutDown();
                } finally {
                  lock.unlock();
                }
                notifyStopped();
              } catch (Throwable t) {
                notifyFailed(t);
              }
            }
          });
    }

    @Override
    public String toString() {
      return AbstractScheduledService.this.toString();
    }
  }

  /** Constructor for use by subclasses. */
  protected AbstractScheduledService() {}

  /**
   * Run one iteration of the scheduled task. If any invocation of this method throws an exception,
   * the service will transition to the {@link Service.State#FAILED} state and this method will no
   * longer be called.
   */
  protected abstract void runOneIteration() throws Exception;

  /**
   * Start the service.
   *
   * <p>By default this method does nothing.
   */
  protected void startUp() throws Exception {}

  /**
   * Stop the service. This is guaranteed not to run concurrently with {@link #runOneIteration}.
   *
   * <p>By default this method does nothing.
   */
  protected void shutDown() throws Exception {}

  /**
   * Returns the {@link Scheduler} object used to configure this service. This method will only be
   * called once.
   */
  // TODO(cpovirk): @ForOverride
  protected abstract Scheduler scheduler();

  /**
   * Returns the {@link ScheduledExecutorService} that will be used to execute the {@link #startUp},
   * {@link #runOneIteration} and {@link #shutDown} methods. If this method is overridden the
   * executor will not be {@linkplain ScheduledExecutorService#shutdown shutdown} when this service
   * {@linkplain Service.State#TERMINATED terminates} or {@linkplain Service.State#TERMINATED
   * fails}. Subclasses may override this method to supply a custom {@link ScheduledExecutorService}
   * instance. This method is guaranteed to only be called once.
   *
   * <p>By default this returns a new {@link ScheduledExecutorService} with a single thread pool
   * that sets the name of the thread to the {@linkplain #serviceName() service name}. Also, the
   * pool will be {@linkplain ScheduledExecutorService#shutdown() shut down} when the service
   * {@linkplain Service.State#TERMINATED terminates} or {@linkplain Service.State#TERMINATED
   * fails}.
   */
  protected ScheduledExecutorService executor() {
    @WeakOuter
    class ThreadFactoryImpl implements ThreadFactory {
      @Override
      public Thread newThread(Runnable runnable) {
        return MoreExecutors.newThread(serviceName(), runnable);
      }
    }
    final ScheduledExecutorService executor =
        Executors.newSingleThreadScheduledExecutor(new ThreadFactoryImpl());
    // Add a listener to shutdown the executor after the service is stopped. This ensures that the
    // JVM shutdown will not be prevented from exiting after this service has stopped or failed.
    // Technically this listener is added after start() was called so it is a little gross, but it
    // is called within doStart() so we know that the service cannot terminate or fail concurrently
    // with adding this listener so it is impossible to miss an event that we are interested in.
    addListener(
        new Listener() {
          @Override
          public void terminated(State from) {
            executor.shutdown();
          }

          @Override
          public void failed(State from, Throwable failure) {
            executor.shutdown();
          }
        },
        directExecutor());
    return executor;
  }

  /**
   * Returns the name of this service. {@link AbstractScheduledService} may include the name in
   * debugging output.
   *
   * @since 14.0
   */
  protected String serviceName() {
    return getClass().getSimpleName();
  }

  @Override
  public String toString() {
    return serviceName() + " [" + state() + "]";
  }

  @Override
  public final boolean isRunning() {
    return delegate.isRunning();
  }

  @Override
  public final State state() {
    return delegate.state();
  }

  /** @since 13.0 */
  @Override
  public final void addListener(Listener listener, Executor executor) {
    delegate.addListener(listener, executor);
  }

  /** @since 14.0 */
  @Override
  public final Throwable failureCause() {
    return delegate.failureCause();
  }

  /** @since 15.0 */
  @CanIgnoreReturnValue
  @Override
  public final Service startAsync() {
    delegate.startAsync();
    return this;
  }

  /** @since 15.0 */
  @CanIgnoreReturnValue
  @Override
  public final Service stopAsync() {
    delegate.stopAsync();
    return this;
  }

  /** @since 15.0 */
  @Override
  public final void awaitRunning() {
    delegate.awaitRunning();
  }

  /** @since 28.0 */
  @Override
  public final void awaitRunning(Duration timeout) throws TimeoutException {
    Service.super.awaitRunning(timeout);
  }

  /** @since 15.0 */
  @Override
  public final void awaitRunning(long timeout, TimeUnit unit) throws TimeoutException {
    delegate.awaitRunning(timeout, unit);
  }

  /** @since 15.0 */
  @Override
  public final void awaitTerminated() {
    delegate.awaitTerminated();
  }

  /** @since 28.0 */
  @Override
  public final void awaitTerminated(Duration timeout) throws TimeoutException {
    Service.super.awaitTerminated(timeout);
  }

  /** @since 15.0 */
  @Override
  public final void awaitTerminated(long timeout, TimeUnit unit) throws TimeoutException {
    delegate.awaitTerminated(timeout, unit);
  }

  interface Cancellable {
    void cancel(boolean mayInterruptIfRunning);

    boolean isCancelled();
  }

  private static final class FutureAsCancellable implements Cancellable {
    private final Future<?> delegate;

    FutureAsCancellable(Future<?> delegate) {
      this.delegate = delegate;
    }

    @Override
    public void cancel(boolean mayInterruptIfRunning) {
      delegate.cancel(mayInterruptIfRunning);
    }

    @Override
    public boolean isCancelled() {
      return delegate.isCancelled();
    }
  }

  /**
   * A {@link Scheduler} that provides a convenient way for the {@link AbstractScheduledService} to
   * use a dynamically changing schedule. After every execution of the task, assuming it hasn't been
   * cancelled, the {@link #getNextSchedule} method will be called.
   *
   * @author Luke Sandberg
   * @since 11.0
   */
  public abstract static class CustomScheduler extends Scheduler {

    /** A callable class that can reschedule itself using a {@link CustomScheduler}. */
    private final class ReschedulableCallable implements Callable<@Nullable Void> {

      /** The underlying task. */
      private final Runnable wrappedRunnable;

      /** The executor on which this Callable will be scheduled. */
      private final ScheduledExecutorService executor;

      /**
       * The service that is managing this callable. This is used so that failure can be reported
       * properly.
       */
      /*
       * This reference is part of a reference cycle, which is typically something we want to avoid
       * under j2objc -- but it is not detected by our j2objc cycle test. The cycle:
       *
       * - CustomScheduler.service contains an instance of ServiceDelegate. (It needs it so that it
       *   can call notifyFailed.)
       *
       * - ServiceDelegate.runningTask contains an instance of ReschedulableCallable (at least in
       *   the case that the service is using CustomScheduler). (It needs it so that it can cancel
       *   the task and detect whether it has been cancelled.)
       *
       * - ReschedulableCallable has a reference back to its enclosing CustomScheduler. (It needs it
       *   so that it can call getNextSchedule).
       *
       * Maybe there is a way to avoid this cycle. But we think the cycle is safe enough to ignore:
       * Each task is retained for only as long as it is running -- so it's retained only as long as
       * it would already be retained by the underlying executor.
       *
       * If the cycle test starts reporting this cycle in the future, we should add an entry to
       * cycle_suppress_list.txt.
       */
      private final AbstractService service;

      /**
       * This lock is used to ensure safe and correct cancellation, it ensures that a new task is
       * not scheduled while a cancel is ongoing. Also it protects the currentFuture variable to
       * ensure that it is assigned atomically with being scheduled.
       */
      private final ReentrantLock lock = new ReentrantLock();

      /** The future that represents the next execution of this task. */
      @GuardedBy("lock")
      @CheckForNull
      private SupplantableFuture cancellationDelegate;

      ReschedulableCallable(
          AbstractService service, ScheduledExecutorService executor, Runnable runnable) {
        this.wrappedRunnable = runnable;
        this.executor = executor;
        this.service = service;
      }

      @Override
      @CheckForNull
      public Void call() throws Exception {
        wrappedRunnable.run();
        reschedule();
        return null;
      }

      /**
       * Atomically reschedules this task and assigns the new future to {@link
       * #cancellationDelegate}.
       */
      @CanIgnoreReturnValue
      public Cancellable reschedule() {
        // invoke the callback outside the lock, prevents some shenanigans.
        Schedule schedule;
        try {
          schedule = CustomScheduler.this.getNextSchedule();
        } catch (Throwable t) {
          service.notifyFailed(t);
          return new FutureAsCancellable(immediateCancelledFuture());
        }
        // We reschedule ourselves with a lock held for two reasons. 1. we want to make sure that
        // cancel calls cancel on the correct future. 2. we want to make sure that the assignment
        // to currentFuture doesn't race with itself so that currentFuture is assigned in the
        // correct order.
        Throwable scheduleFailure = null;
        Cancellable toReturn;
        lock.lock();
        try {
          toReturn = initializeOrUpdateCancellationDelegate(schedule);
        } catch (Throwable e) {
          // If an exception is thrown by the subclass then we need to make sure that the service
          // notices and transitions to the FAILED state. We do it by calling notifyFailed directly
          // because the service does not monitor the state of the future so if the exception is not
          // caught and forwarded to the service the task would stop executing but the service would
          // have no idea.
          // TODO(lukes): consider building everything in terms of ListenableScheduledFuture then
          // the AbstractService could monitor the future directly. Rescheduling is still hard...
          // but it would help with some of these lock ordering issues.
          scheduleFailure = e;
          toReturn = new FutureAsCancellable(immediateCancelledFuture());
        } finally {
          lock.unlock();
        }
        // Call notifyFailed outside the lock to avoid lock ordering issues.
        if (scheduleFailure != null) {
          service.notifyFailed(scheduleFailure);
        }
        return toReturn;
      }

      @GuardedBy("lock")
      /*
       * The GuardedBy checker warns us that we're not holding cancellationDelegate.lock. But in
       * fact we are holding it because it is the same as this.lock, which we know we are holding,
       * thanks to @GuardedBy above. (cancellationDelegate.lock is initialized to this.lock in the
       * call to `new SupplantableFuture` below.)
       */
      @SuppressWarnings("GuardedBy")
      private Cancellable initializeOrUpdateCancellationDelegate(Schedule schedule) {
        if (cancellationDelegate == null) {
          return cancellationDelegate = new SupplantableFuture(lock, submitToExecutor(schedule));
        }
        if (!cancellationDelegate.currentFuture.isCancelled()) {
          cancellationDelegate.currentFuture = submitToExecutor(schedule);
        }
        return cancellationDelegate;
      }

      private ScheduledFuture<@Nullable Void> submitToExecutor(Schedule schedule) {
        return executor.schedule(this, schedule.delay, schedule.unit);
      }
    }

    /**
     * Contains the most recently submitted {@code Future}, which may be cancelled or updated,
     * always under a lock.
     */
    private static final class SupplantableFuture implements Cancellable {
      private final ReentrantLock lock;

      @GuardedBy("lock")
      private Future<@Nullable Void> currentFuture;

      SupplantableFuture(ReentrantLock lock, Future<@Nullable Void> currentFuture) {
        this.lock = lock;
        this.currentFuture = currentFuture;
      }

      @Override
      public void cancel(boolean mayInterruptIfRunning) {
        /*
         * Lock to ensure that a task cannot be rescheduled while a cancel is ongoing.
         *
         * In theory, cancel() could execute arbitrary listeners -- bad to do while holding a lock.
         * However, we don't expose currentFuture to users, so they can't attach listeners. And the
         * Future might not even be a ListenableFuture, just a plain Future. That said, similar
         * problems can exist with methods like FutureTask.done(), not to mention slow calls to
         * Thread.interrupt() (as discussed in InterruptibleTask). At the end of the day, it's
         * unlikely that cancel() will be slow, so we can probably get away with calling it while
         * holding a lock. Still, it would be nice to avoid somehow.
         */
        lock.lock();
        try {
          currentFuture.cancel(mayInterruptIfRunning);
        } finally {
          lock.unlock();
        }
      }

      @Override
      public boolean isCancelled() {
        lock.lock();
        try {
          return currentFuture.isCancelled();
        } finally {
          lock.unlock();
        }
      }
    }

    @Override
    final Cancellable schedule(
        AbstractService service, ScheduledExecutorService executor, Runnable runnable) {
      return new ReschedulableCallable(service, executor, runnable).reschedule();
    }

    /**
     * A value object that represents an absolute delay until a task should be invoked.
     *
     * @author Luke Sandberg
     * @since 11.0
     */
    protected static final class Schedule {

      private final long delay;
      private final TimeUnit unit;

      /**
       * @param delay the time from now to delay execution
       * @param unit the time unit of the delay parameter
       */
      public Schedule(long delay, TimeUnit unit) {
        this.delay = delay;
        this.unit = checkNotNull(unit);
      }

      /**
       * @param delay the time from now to delay execution
       * @since 31.1
       */
      public Schedule(Duration delay) {
        this(toNanosSaturated(delay), NANOSECONDS);
      }
    }

    /**
     * Calculates the time at which to next invoke the task.
     *
     * <p>This is guaranteed to be called immediately after the task has completed an iteration and
     * on the same thread as the previous execution of {@link
     * AbstractScheduledService#runOneIteration}.
     *
     * @return a schedule that defines the delay before the next execution.
     */
    // TODO(cpovirk): @ForOverride
    protected abstract Schedule getNextSchedule() throws Exception;
  }
}
