/*
 * Copyright (C) 2007 The Guava Authors
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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.annotations.Beta;
import com.google.common.collect.Lists;
import com.google.common.collect.Queues;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Factory and utility methods for {@link java.util.concurrent.Executor}, {@link
 * ExecutorService}, and {@link ThreadFactory}.
 *
 * @author Eric Fellheimer
 * @author Kyle Littlefield
 * @author Justin Mahoney
 * @since 3.0
 */
public final class MoreExecutors {
  private MoreExecutors() {}

  /**
   * Converts the given ThreadPoolExecutor into an ExecutorService that exits
   * when the application is complete.  It does so by using daemon threads and
   * adding a shutdown hook to wait for their completion.
   *
   * <p>This is mainly for fixed thread pools.
   * See {@link Executors#newFixedThreadPool(int)}.
   *
   * @param executor the executor to modify to make sure it exits when the
   *        application is finished
   * @param terminationTimeout how long to wait for the executor to
   *        finish before terminating the JVM
   * @param timeUnit unit of time for the time parameter
   * @return an unmodifiable version of the input which will not hang the JVM
   */
  @Beta
  public static ExecutorService getExitingExecutorService(
      ThreadPoolExecutor executor, long terminationTimeout, TimeUnit timeUnit) {
    executor.setThreadFactory(new ThreadFactoryBuilder()
        .setDaemon(true)
        .setThreadFactory(executor.getThreadFactory())
        .build());

    ExecutorService service = Executors.unconfigurableExecutorService(executor);

    addDelayedShutdownHook(service, terminationTimeout, timeUnit);

    return service;
  }

  /**
   * Converts the given ScheduledThreadPoolExecutor into a
   * ScheduledExecutorService that exits when the application is complete.  It
   * does so by using daemon threads and adding a shutdown hook to wait for
   * their completion.
   *
   * <p>This is mainly for fixed thread pools.
   * See {@link Executors#newScheduledThreadPool(int)}.
   *
   * @param executor the executor to modify to make sure it exits when the
   *        application is finished
   * @param terminationTimeout how long to wait for the executor to
   *        finish before terminating the JVM
   * @param timeUnit unit of time for the time parameter
   * @return an unmodifiable version of the input which will not hang the JVM
   */
  @Beta
  public static ScheduledExecutorService getExitingScheduledExecutorService(
      ScheduledThreadPoolExecutor executor, long terminationTimeout,
      TimeUnit timeUnit) {
    executor.setThreadFactory(new ThreadFactoryBuilder()
        .setDaemon(true)
        .setThreadFactory(executor.getThreadFactory())
        .build());

    ScheduledExecutorService service =
        Executors.unconfigurableScheduledExecutorService(executor);

    addDelayedShutdownHook(service, terminationTimeout, timeUnit);

    return service;
  }

  /**
   * Add a shutdown hook to wait for thread completion in the given
   * {@link ExecutorService service}.  This is useful if the given service uses
   * daemon threads, and we want to keep the JVM from exiting immediately on
   * shutdown, instead giving these daemon threads a chance to terminate
   * normally.
   * @param service ExecutorService which uses daemon threads
   * @param terminationTimeout how long to wait for the executor to finish
   *        before terminating the JVM
   * @param timeUnit unit of time for the time parameter
   */
  @Beta
  public static void addDelayedShutdownHook(
      final ExecutorService service, final long terminationTimeout,
      final TimeUnit timeUnit) {
    Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
      @Override
      public void run() {
        try {
          // We'd like to log progress and failures that may arise in the
          // following code, but unfortunately the behavior of logging
          // is undefined in shutdown hooks.
          // This is because the logging code installs a shutdown hook of its
          // own. See Cleaner class inside {@link LogManager}.
          service.shutdown();
          service.awaitTermination(terminationTimeout, timeUnit);
        } catch (InterruptedException ignored) {
          // We're shutting down anyway, so just ignore.
        }
      }
    }, "DelayedShutdownHook-for-" + service));
  }

  /**
   * Converts the given ThreadPoolExecutor into an ExecutorService that exits
   * when the application is complete.  It does so by using daemon threads and
   * adding a shutdown hook to wait for their completion.
   *
   * <p>This method waits 120 seconds before continuing with JVM termination,
   * even if the executor has not finished its work.
   *
   * <p>This is mainly for fixed thread pools.
   * See {@link Executors#newFixedThreadPool(int)}.
   *
   * @param executor the executor to modify to make sure it exits when the
   *        application is finished
   * @return an unmodifiable version of the input which will not hang the JVM
   */
  @Beta
  public static ExecutorService getExitingExecutorService(
      ThreadPoolExecutor executor) {
    return getExitingExecutorService(executor, 120, TimeUnit.SECONDS);
  }

  /**
   * Converts the given ThreadPoolExecutor into a ScheduledExecutorService that
   * exits when the application is complete.  It does so by using daemon threads
   * and adding a shutdown hook to wait for their completion.
   *
   * <p>This method waits 120 seconds before continuing with JVM termination,
   * even if the executor has not finished its work.
   *
   * <p>This is mainly for fixed thread pools.
   * See {@link Executors#newScheduledThreadPool(int)}.
   *
   * @param executor the executor to modify to make sure it exits when the
   *        application is finished
   * @return an unmodifiable version of the input which will not hang the JVM
   */
  @Beta
  public static ScheduledExecutorService getExitingScheduledExecutorService(
      ScheduledThreadPoolExecutor executor) {
    return getExitingScheduledExecutorService(executor, 120, TimeUnit.SECONDS);
  }

  /**
   * Creates an executor service that runs each task in the thread
   * that invokes {@code execute/submit}, as in {@link CallerRunsPolicy}  This
   * applies both to individually submitted tasks and to collections of tasks
   * submitted via {@code invokeAll} or {@code invokeAny}.  In the latter case,
   * tasks will run serially on the calling thread.  Tasks are run to
   * completion before a {@code Future} is returned to the caller (unless the
   * executor has been shutdown).
   *
   * <p>Although all tasks are immediately executed in the thread that
   * submitted the task, this {@code ExecutorService} imposes a small
   * locking overhead on each task submission in order to implement shutdown
   * and termination behavior.
   *
   * <p>The implementation deviates from the {@code ExecutorService}
   * specification with regards to the {@code shutdownNow} method.  First,
   * "best-effort" with regards to canceling running tasks is implemented
   * as "no-effort".  No interrupts or other attempts are made to stop
   * threads executing tasks.  Second, the returned list will always be empty,
   * as any submitted task is considered to have started execution.
   * This applies also to tasks given to {@code invokeAll} or {@code invokeAny}
   * which are pending serial execution, even the subset of the tasks that
   * have not yet started execution.  It is unclear from the
   * {@code ExecutorService} specification if these should be included, and
   * it's much easier to implement the interpretation that they not be.
   * Finally, a call to {@code shutdown} or {@code shutdownNow} may result
   * in concurrent calls to {@code invokeAll/invokeAny} throwing
   * RejectedExecutionException, although a subset of the tasks may already
   * have been executed.
   *
   * @since 10.0 (<a href="http://code.google.com/p/guava-libraries/wiki/Compatibility"
   *        >mostly source-compatible</a> since 3.0)
   */
  public static ListeningExecutorService sameThreadExecutor() {
    return new SameThreadExecutorService();
  }

  // See sameThreadExecutor javadoc for behavioral notes.
  private static class SameThreadExecutorService
      extends AbstractListeningExecutorService {
    /**
     * Lock used whenever accessing the state variables
     * (runningTasks, shutdown, terminationCondition) of the executor
     */
    private final Lock lock = new ReentrantLock();

    /** Signaled after the executor is shutdown and running tasks are done */
    private final Condition termination = lock.newCondition();

    /*
     * Conceptually, these two variables describe the executor being in
     * one of three states:
     *   - Active: shutdown == false
     *   - Shutdown: runningTasks > 0 and shutdown == true
     *   - Terminated: runningTasks == 0 and shutdown == true
     */
    private int runningTasks = 0;
    private boolean shutdown = false;

    @Override
    public void execute(Runnable command) {
      startTask();
      try {
        command.run();
      } finally {
        endTask();
      }
    }

    @Override
    public boolean isShutdown() {
      lock.lock();
      try {
        return shutdown;
      } finally {
        lock.unlock();
      }
    }

    @Override
    public void shutdown() {
      lock.lock();
      try {
        shutdown = true;
      } finally {
        lock.unlock();
      }
    }

    // See sameThreadExecutor javadoc for unusual behavior of this method.
    @Override
    public List<Runnable> shutdownNow() {
      shutdown();
      return Collections.emptyList();
    }

    @Override
    public boolean isTerminated() {
      lock.lock();
      try {
        return shutdown && runningTasks == 0;
      } finally {
        lock.unlock();
      }
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit)
        throws InterruptedException {
      long nanos = unit.toNanos(timeout);
      lock.lock();
      try {
        for (;;) {
          if (isTerminated()) {
            return true;
          } else if (nanos <= 0) {
            return false;
          } else {
            nanos = termination.awaitNanos(nanos);
          }
        }
      } finally {
        lock.unlock();
      }
    }

    /**
     * Checks if the executor has been shut down and increments the running
     * task count.
     *
     * @throws RejectedExecutionException if the executor has been previously
     *         shutdown
     */
    private void startTask() {
      lock.lock();
      try {
        if (isShutdown()) {
          throw new RejectedExecutionException("Executor already shutdown");
        }
        runningTasks++;
      } finally {
        lock.unlock();
      }
    }

    /**
     * Decrements the running task count.
     */
    private void endTask() {
      lock.lock();
      try {
        runningTasks--;
        if (isTerminated()) {
          termination.signalAll();
        }
      } finally {
        lock.unlock();
      }
    }
  }

  /**
   * Creates an {@link ExecutorService} whose {@code submit} and {@code
   * invokeAll} methods submit {@link ListenableFutureTask} instances to the
   * given delegate executor. Those methods, as well as {@code execute} and
   * {@code invokeAny}, are implemented in terms of calls to {@code
   * delegate.execute}. All other methods are forwarded unchanged to the
   * delegate. This implies that the returned {@code ListeningExecutorService}
   * never calls the delegate's {@code submit}, {@code invokeAll}, and {@code
   * invokeAny} methods, so any special handling of tasks must be implemented in
   * the delegate's {@code execute} method or by wrapping the returned {@code
   * ListeningExecutorService}.
   *
   * <p>If the delegate executor was already an instance of {@code
   * ListeningExecutorService}, it is returned untouched, and the rest of this
   * documentation does not apply.
   *
   * @since 10.0
   */
  public static ListeningExecutorService listeningDecorator(
      ExecutorService delegate) {
    return (delegate instanceof ListeningExecutorService)
        ? (ListeningExecutorService) delegate
        : (delegate instanceof ScheduledExecutorService)
        ? new ScheduledListeningDecorator((ScheduledExecutorService) delegate)
        : new ListeningDecorator(delegate);
  }

  /**
   * Creates a {@link ScheduledExecutorService} whose {@code submit} and {@code
   * invokeAll} methods submit {@link ListenableFutureTask} instances to the
   * given delegate executor. Those methods, as well as {@code execute} and
   * {@code invokeAny}, are implemented in terms of calls to {@code
   * delegate.execute}. All other methods are forwarded unchanged to the
   * delegate. This implies that the returned {@code
   * SchedulingListeningExecutorService} never calls the delegate's {@code
   * submit}, {@code invokeAll}, and {@code invokeAny} methods, so any special
   * handling of tasks must be implemented in the delegate's {@code execute}
   * method or by wrapping the returned {@code
   * SchedulingListeningExecutorService}.
   *
   * <p>If the delegate executor was already an instance of {@code
   * ListeningScheduledExecutorService}, it is returned untouched, and the rest
   * of this documentation does not apply.
   *
   * @since 10.0
   */
  public static ListeningScheduledExecutorService listeningDecorator(
      ScheduledExecutorService delegate) {
    return (delegate instanceof ListeningScheduledExecutorService)
        ? (ListeningScheduledExecutorService) delegate
        : new ScheduledListeningDecorator(delegate);
  }

  private static class ListeningDecorator
      extends AbstractListeningExecutorService {
    final ExecutorService delegate;

    ListeningDecorator(ExecutorService delegate) {
      this.delegate = checkNotNull(delegate);
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit)
        throws InterruptedException {
      return delegate.awaitTermination(timeout, unit);
    }

    @Override
    public boolean isShutdown() {
      return delegate.isShutdown();
    }

    @Override
    public boolean isTerminated() {
      return delegate.isTerminated();
    }

    @Override
    public void shutdown() {
      delegate.shutdown();
    }

    @Override
    public List<Runnable> shutdownNow() {
      return delegate.shutdownNow();
    }

    @Override
    public void execute(Runnable command) {
      delegate.execute(command);
    }
  }

  private static class ScheduledListeningDecorator
      extends ListeningDecorator implements ListeningScheduledExecutorService {
    @SuppressWarnings("hiding")
    final ScheduledExecutorService delegate;

    ScheduledListeningDecorator(ScheduledExecutorService delegate) {
      super(delegate);
      this.delegate = checkNotNull(delegate);
    }

    @Override
    public ScheduledFuture<?> schedule(
        Runnable command, long delay, TimeUnit unit) {
      return delegate.schedule(command, delay, unit);
    }

    @Override
    public <V> ScheduledFuture<V> schedule(
        Callable<V> callable, long delay, TimeUnit unit) {
      return delegate.schedule(callable, delay, unit);
    }

    @Override
    public ScheduledFuture<?> scheduleAtFixedRate(
        Runnable command, long initialDelay, long period, TimeUnit unit) {
      return delegate.scheduleAtFixedRate(command, initialDelay, period, unit);
    }

    @Override
    public ScheduledFuture<?> scheduleWithFixedDelay(
        Runnable command, long initialDelay, long delay, TimeUnit unit) {
      return delegate.scheduleWithFixedDelay(
          command, initialDelay, delay, unit);
    }
  }

  /*
   * This following method is a modified version of one found in
   * http://gee.cs.oswego.edu/cgi-bin/viewcvs.cgi/jsr166/src/test/tck/AbstractExecutorServiceTest.java?revision=1.30
   * which contained the following notice:
   *
   * Written by Doug Lea with assistance from members of JCP JSR-166
   * Expert Group and released to the public domain, as explained at
   * http://creativecommons.org/publicdomain/zero/1.0/
   * Other contributors include Andrew Wright, Jeffrey Hayes,
   * Pat Fisher, Mike Judd.
   */

  /**
   * An implementation of {@link ExecutorService#invokeAny} for {@link ListeningExecutorService}
   * implementations.
   */ static <T> T invokeAnyImpl(ListeningExecutorService executorService,
      Collection<? extends Callable<T>> tasks, boolean timed, long nanos)
          throws InterruptedException, ExecutionException, TimeoutException {
    int ntasks = tasks.size();
    checkArgument(ntasks > 0);
    List<Future<T>> futures = Lists.newArrayListWithCapacity(ntasks);
    BlockingQueue<Future<T>> futureQueue = Queues.newLinkedBlockingQueue();

    // For efficiency, especially in executors with limited
    // parallelism, check to see if previously submitted tasks are
    // done before submitting more of them. This interleaving
    // plus the exception mechanics account for messiness of main
    // loop.

    try {
      // Record exceptions so that if we fail to obtain any
      // result, we can throw the last exception we got.
      ExecutionException ee = null;
      long lastTime = timed ? System.nanoTime() : 0;
      Iterator<? extends Callable<T>> it = tasks.iterator();

      futures.add(submitAndAddQueueListener(executorService, it.next(), futureQueue));
      --ntasks;
      int active = 1;

      for (;;) {
        Future<T> f = futureQueue.poll();
        if (f == null) {
          if (ntasks > 0) {
            --ntasks;
            futures.add(submitAndAddQueueListener(executorService, it.next(), futureQueue));
            ++active;
          } else if (active == 0) {
            break;
          } else if (timed) {
            f = futureQueue.poll(nanos, TimeUnit.NANOSECONDS);
            if (f == null) {
              throw new TimeoutException();
            }
            long now = System.nanoTime();
            nanos -= now - lastTime;
            lastTime = now;
          } else {
            f = futureQueue.take();
          }
        }
        if (f != null) {
          --active;
          try {
            return f.get();
          } catch (ExecutionException eex) {
            ee = eex;
          } catch (RuntimeException rex) {
            ee = new ExecutionException(rex);
          }
        }
      }

      if (ee == null) {
        ee = new ExecutionException(null);
      }
      throw ee;
    } finally {
      for (Future<T> f : futures) {
        f.cancel(true);
      }
    }
  }

  /**
   * Submits the task and adds a listener that adds the future to {@code queue} when it completes.
   */
  private static <T> ListenableFuture<T> submitAndAddQueueListener(
      ListeningExecutorService executorService, Callable<T> task,
      final BlockingQueue<Future<T>> queue) {
    final ListenableFuture<T> future = executorService.submit(task);
    future.addListener(new Runnable() {
      @Override public void run() {
        queue.add(future);
      }
    }, MoreExecutors.sameThreadExecutor());
    return future;
  }
}
