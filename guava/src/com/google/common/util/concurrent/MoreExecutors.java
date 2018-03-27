/*
 * Copyright (C) 2007 The Guava Authors
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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Supplier;
import com.google.common.base.Throwables;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.errorprone.annotations.concurrent.GuardedBy;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.Delayed;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Factory and utility methods for {@link java.util.concurrent.Executor}, {@link ExecutorService},
 * and {@link ThreadFactory}.
 *
 * @author Eric Fellheimer
 * @author Kyle Littlefield
 * @author Justin Mahoney
 * @since 3.0
 */
public final class MoreExecutors {
  private MoreExecutors() {}

  /**
   * Converts the given ThreadPoolExecutor into an ExecutorService that exits when the application
   * is complete. It does so by using daemon threads and adding a shutdown hook to wait for their
   * completion.
   *
   * <p>This is mainly for fixed thread pools. See {@link Executors#newFixedThreadPool(int)}.
   *
   * @param executor the executor to modify to make sure it exits when the application is finished
   * @param terminationTimeout how long to wait for the executor to finish before terminating the
   *     JVM
   * @param timeUnit unit of time for the time parameter
   * @return an unmodifiable version of the input which will not hang the JVM
   */
  public static ExecutorService getExitingExecutorService(
      ThreadPoolExecutor executor, long terminationTimeout, TimeUnit timeUnit) {
    return new Application().getExitingExecutorService(executor, terminationTimeout, timeUnit);
  }

  /**
   * Converts the given ThreadPoolExecutor into an ExecutorService that exits when the application
   * is complete. It does so by using daemon threads and adding a shutdown hook to wait for their
   * completion.
   *
   * <p>This method waits 120 seconds before continuing with JVM termination, even if the executor
   * has not finished its work.
   *
   * <p>This is mainly for fixed thread pools. See {@link Executors#newFixedThreadPool(int)}.
   *
   * @param executor the executor to modify to make sure it exits when the application is finished
   * @return an unmodifiable version of the input which will not hang the JVM
   */

    // concurrency
  public static ExecutorService getExitingExecutorService(ThreadPoolExecutor executor) {
    return new Application().getExitingExecutorService(executor);
  }

  /**
   * Converts the given ScheduledThreadPoolExecutor into a ScheduledExecutorService that exits when
   * the application is complete. It does so by using daemon threads and adding a shutdown hook to
   * wait for their completion.
   *
   * <p>This is mainly for fixed thread pools. See {@link Executors#newScheduledThreadPool(int)}.
   *
   * @param executor the executor to modify to make sure it exits when the application is finished
   * @param terminationTimeout how long to wait for the executor to finish before terminating the
   *     JVM
   * @param timeUnit unit of time for the time parameter
   * @return an unmodifiable version of the input which will not hang the JVM
   */

    // TODO
  public static ScheduledExecutorService getExitingScheduledExecutorService(
      ScheduledThreadPoolExecutor executor, long terminationTimeout, TimeUnit timeUnit) {
    return new Application()
        .getExitingScheduledExecutorService(executor, terminationTimeout, timeUnit);
  }

  /**
   * Converts the given ScheduledThreadPoolExecutor into a ScheduledExecutorService that exits when
   * the application is complete. It does so by using daemon threads and adding a shutdown hook to
   * wait for their completion.
   *
   * <p>This method waits 120 seconds before continuing with JVM termination, even if the executor
   * has not finished its work.
   *
   * <p>This is mainly for fixed thread pools. See {@link Executors#newScheduledThreadPool(int)}.
   *
   * @param executor the executor to modify to make sure it exits when the application is finished
   * @return an unmodifiable version of the input which will not hang the JVM
   */

    // TODO
  public static ScheduledExecutorService getExitingScheduledExecutorService(
      ScheduledThreadPoolExecutor executor) {
    return new Application().getExitingScheduledExecutorService(executor);
  }

  /**
   * Add a shutdown hook to wait for thread completion in the given {@link ExecutorService service}.
   * This is useful if the given service uses daemon threads, and we want to keep the JVM from
   * exiting immediately on shutdown, instead giving these daemon threads a chance to terminate
   * normally.
   *
   * @param service ExecutorService which uses daemon threads
   * @param terminationTimeout how long to wait for the executor to finish before terminating the
   *     JVM
   * @param timeUnit unit of time for the time parameter
   */

    // TODO
  public static void addDelayedShutdownHook(
      ExecutorService service, long terminationTimeout, TimeUnit timeUnit) {
    new Application().addDelayedShutdownHook(service, terminationTimeout, timeUnit);
  }

  /** Represents the current application to register shutdown hooks. */
    // TODO
  @VisibleForTesting
  static class Application {

    final ExecutorService getExitingExecutorService(
        ThreadPoolExecutor executor, long terminationTimeout, TimeUnit timeUnit) {
      useDaemonThreadFactory(executor);
      ExecutorService service = Executors.unconfigurableExecutorService(executor);
      addDelayedShutdownHook(executor, terminationTimeout, timeUnit);
      return service;
    }

    final ExecutorService getExitingExecutorService(ThreadPoolExecutor executor) {
      return getExitingExecutorService(executor, 120, TimeUnit.SECONDS);
    }

    final ScheduledExecutorService getExitingScheduledExecutorService(
        ScheduledThreadPoolExecutor executor, long terminationTimeout, TimeUnit timeUnit) {
      useDaemonThreadFactory(executor);
      ScheduledExecutorService service = Executors.unconfigurableScheduledExecutorService(executor);
      addDelayedShutdownHook(executor, terminationTimeout, timeUnit);
      return service;
    }

    final ScheduledExecutorService getExitingScheduledExecutorService(
        ScheduledThreadPoolExecutor executor) {
      return getExitingScheduledExecutorService(executor, 120, TimeUnit.SECONDS);
    }

    final void addDelayedShutdownHook(
        final ExecutorService service, final long terminationTimeout, final TimeUnit timeUnit) {
      checkNotNull(service);
      checkNotNull(timeUnit);
      addShutdownHook(
          MoreExecutors.newThread(
              "DelayedShutdownHook-for-" + service,
              new Runnable() {
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
              }));
    }

    @VisibleForTesting
    void addShutdownHook(Thread hook) {
      Runtime.getRuntime().addShutdownHook(hook);
    }
  }

    // TODO
  private static void useDaemonThreadFactory(ThreadPoolExecutor executor) {
    executor.setThreadFactory(
        new ThreadFactoryBuilder()
            .setDaemon(true)
            .setThreadFactory(executor.getThreadFactory())
            .build());
  }

  // See newDirectExecutorService javadoc for behavioral notes.
    // TODO
  private static final class DirectExecutorService extends AbstractListeningExecutorService {
    /** Lock used whenever accessing the state variables (runningTasks, shutdown) of the executor */
    private final Object lock = new Object();

    /*
     * Conceptually, these two variables describe the executor being in
     * one of three states:
     *   - Active: shutdown == false
     *   - Shutdown: runningTasks > 0 and shutdown == true
     *   - Terminated: runningTasks == 0 and shutdown == true
     */
    @GuardedBy("lock")
    private int runningTasks = 0;

    @GuardedBy("lock")
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
      synchronized (lock) {
        return shutdown;
      }
    }

    @Override
    public void shutdown() {
      synchronized (lock) {
        shutdown = true;
        if (runningTasks == 0) {
          lock.notifyAll();
        }
      }
    }

    // See newDirectExecutorService javadoc for unusual behavior of this method.
    @Override
    public List<Runnable> shutdownNow() {
      shutdown();
      return Collections.emptyList();
    }

    @Override
    public boolean isTerminated() {
      synchronized (lock) {
        return shutdown && runningTasks == 0;
      }
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
      long nanos = unit.toNanos(timeout);
      synchronized (lock) {
        while (true) {
          if (shutdown && runningTasks == 0) {
            return true;
          } else if (nanos <= 0) {
            return false;
          } else {
            long now = System.nanoTime();
            TimeUnit.NANOSECONDS.timedWait(lock, nanos);
            nanos -= System.nanoTime() - now; // subtract the actual time we waited
          }
        }
      }
    }

    /**
     * Checks if the executor has been shut down and increments the running task count.
     *
     * @throws RejectedExecutionException if the executor has been previously shutdown
     */
    private void startTask() {
      synchronized (lock) {
        if (shutdown) {
          throw new RejectedExecutionException("Executor already shutdown");
        }
        runningTasks++;
      }
    }

    /** Decrements the running task count. */
    private void endTask() {
      synchronized (lock) {
        int numRunning = --runningTasks;
        if (numRunning == 0) {
          lock.notifyAll();
        }
      }
    }
  }

  /**
   * Creates an executor service that runs each task in the thread that invokes {@code
   * execute/submit}, as in {@link CallerRunsPolicy} This applies both to individually submitted
   * tasks and to collections of tasks submitted via {@code invokeAll} or {@code invokeAny}. In the
   * latter case, tasks will run serially on the calling thread. Tasks are run to completion before
   * a {@code Future} is returned to the caller (unless the executor has been shutdown).
   *
   * <p>Although all tasks are immediately executed in the thread that submitted the task, this
   * {@code ExecutorService} imposes a small locking overhead on each task submission in order to
   * implement shutdown and termination behavior.
   *
   * <p>The implementation deviates from the {@code ExecutorService} specification with regards to
   * the {@code shutdownNow} method. First, "best-effort" with regards to canceling running tasks is
   * implemented as "no-effort". No interrupts or other attempts are made to stop threads executing
   * tasks. Second, the returned list will always be empty, as any submitted task is considered to
   * have started execution. This applies also to tasks given to {@code invokeAll} or {@code
   * invokeAny} which are pending serial execution, even the subset of the tasks that have not yet
   * started execution. It is unclear from the {@code ExecutorService} specification if these should
   * be included, and it's much easier to implement the interpretation that they not be. Finally, a
   * call to {@code shutdown} or {@code shutdownNow} may result in concurrent calls to {@code
   * invokeAll/invokeAny} throwing RejectedExecutionException, although a subset of the tasks may
   * already have been executed.
   *
   * @since 18.0 (present as MoreExecutors.sameThreadExecutor() since 10.0)
   */
    // TODO
  public static ListeningExecutorService newDirectExecutorService() {
    return new DirectExecutorService();
  }

  /**
   * Returns an {@link Executor} that runs each task in the thread that invokes {@link
   * Executor#execute execute}, as in {@link CallerRunsPolicy}.
   *
   * <p>This instance is equivalent to:
   *
   * <pre>{@code
   * final class DirectExecutor implements Executor {
   *   public void execute(Runnable r) {
   *     r.run();
   *   }
   * }
   * }</pre>
   *
   * <p>This should be preferred to {@link #newDirectExecutorService()} because implementing the
   * {@link ExecutorService} subinterface necessitates significant performance overhead.
   *
   *
   * @since 18.0
   */
  public static Executor directExecutor() {
    return DirectExecutor.INSTANCE;
  }

  /** See {@link #directExecutor} for behavioral notes. */
  private enum DirectExecutor implements Executor {
    INSTANCE;

    @Override
    public void execute(Runnable command) {
      command.run();
    }

    @Override
    public String toString() {
      return "MoreExecutors.directExecutor()";
    }
  }

  /**
   * Returns an {@link Executor} that runs each task executed sequentially, such that no two tasks
   * are running concurrently. Submitted tasks have a happens-before order as defined in the Java
   * Language Specification.
   *
   * <p>The executor uses {@code delegate} in order to {@link Executor#execute execute} each task in
   * turn, and does not create any threads of its own.
   *
   * <p>After execution begins on a thread from the {@code delegate} {@link Executor}, tasks are
   * polled and executed from a task queue until there are no more tasks. The thread will not be
   * released until there are no more tasks to run.
   *
   * <p>If a task is submitted while a thread is executing tasks from the task queue, the thread
   * will not be released until that submitted task is also complete.
   *
   * <p>If a task is {@linkplain Thread#interrupt interrupted} while a task is running:
   *
   * <ol>
   *   <li>execution will not stop until the task queue is empty.
   *   <li>tasks will begin execution with the thread marked as not interrupted - any interruption
   *       applies only to the task that was running at the point of interruption.
   *   <li>if the thread was interrupted before the SequentialExecutor's worker begins execution,
   *       the interrupt will be restored to the thread after it completes so that its {@code
   *       delegate} Executor may process the interrupt.
   *   <li>subtasks are run with the thread uninterrupted and interrupts received during execution
   *       of a task are ignored.
   * </ol>
   *
   * <p>{@code RuntimeException}s thrown by tasks are simply logged and the executor keeps trucking.
   * If an {@code Error} is thrown, the error will propagate and execution will stop until the next
   * time a task is submitted.
   *
   * <p>When an {@code Error} is thrown by an executed task, previously submitted tasks may never
   * run. An attempt will be made to restart execution on the next call to {@code execute}. If the
   * {@code delegate} has begun to reject execution, the previously submitted tasks may never run,
   * despite not throwing a RejectedExecutionException synchronously with the call to {@code
   * execute}. If this behaviour is problematic, use an Executor with a single thread (e.g. {@link
   * Executors#newSingleThreadExecutor}).
   *
   * @since 23.3 (since 23.1 as {@code sequentialExecutor})
   */

  public static Executor newSequentialExecutor(Executor delegate) {
    return new SequentialExecutor(delegate);
  }



    // TODO
  private static class ListeningDecorator extends AbstractListeningExecutorService {
    private final ExecutorService delegate;

    ListeningDecorator(ExecutorService delegate) {
      this.delegate = checkNotNull(delegate);
    }

    @Override
    public final boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
      return delegate.awaitTermination(timeout, unit);
    }

    @Override
    public final boolean isShutdown() {
      return delegate.isShutdown();
    }

    @Override
    public final boolean isTerminated() {
      return delegate.isTerminated();
    }

    @Override
    public final void shutdown() {
      delegate.shutdown();
    }

    @Override
    public final List<Runnable> shutdownNow() {
      return delegate.shutdownNow();
    }

    @Override
    public final void execute(Runnable command) {
      delegate.execute(command);
    }
  }


  /*
   * This following method is a modified version of one found in
   * http://gee.cs.oswego.edu/cgi-bin/viewcvs.cgi/jsr166/src/test/tck/AbstractExecutorServiceTest.java?revision=1.30
   * which contained the following notice:
   *
   * Written by Doug Lea with assistance from members of JCP JSR-166 Expert Group and released to
   * the public domain, as explained at http://creativecommons.org/publicdomain/zero/1.0/
   *
   * Other contributors include Andrew Wright, Jeffrey Hayes, Pat Fisher, Mike Judd.
   */

  /**
   * An implementation of {@link ExecutorService#invokeAny} for {@link ListeningExecutorService}
   * implementations.
   */
    static <T> T invokeAnyImpl(
      ListeningExecutorService executorService,
      Collection<? extends Callable<T>> tasks,
      boolean timed,
      long timeout,
      TimeUnit unit)
      throws InterruptedException, ExecutionException, TimeoutException {
    checkNotNull(executorService);
    checkNotNull(unit);
    int ntasks = tasks.size();
    checkArgument(ntasks > 0);
    List<Future<T>> futures = new ArrayList<Future<T>>(ntasks);
    BlockingQueue<Future<T>> futureQueue = new LinkedBlockingQueue();
    long timeoutNanos = unit.toNanos(timeout);

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

      while (true) {
        Future<T> f = futureQueue.poll();
        if (f == null) {
          if (ntasks > 0) {
            --ntasks;
            futures.add(submitAndAddQueueListener(executorService, it.next(), futureQueue));
            ++active;
          } else if (active == 0) {
            break;
          } else if (timed) {
            f = futureQueue.poll(timeoutNanos, TimeUnit.NANOSECONDS);
            if (f == null) {
              throw new TimeoutException();
            }
            long now = System.nanoTime();
            timeoutNanos -= now - lastTime;
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
    // TODO
  private static <T> ListenableFuture<T> submitAndAddQueueListener(
      ListeningExecutorService executorService,
      Callable<T> task,
      final BlockingQueue<Future<T>> queue) {
    final ListenableFuture<T> future = executorService.submit(task);
    future.addListener(
        new Runnable() {
          @Override
          public void run() {
            queue.add(future);
          }
        },
        directExecutor());
    return future;
  }

  /**
   * Returns a default thread factory used to create new threads.
   *
   * <p>On AppEngine, returns {@code ThreadManager.currentRequestThreadFactory()}. Otherwise,
   * returns {@link Executors#defaultThreadFactory()}.
   *
   * @since 14.0
   */

    // concurrency
  public static ThreadFactory platformThreadFactory() {
    if (!isAppEngine()) {
      return Executors.defaultThreadFactory();
    }
    try {
      return (ThreadFactory)
          Class.forName("com.google.appengine.api.ThreadManager")
              .getMethod("currentRequestThreadFactory")
              .invoke(null);
    } catch (IllegalAccessException | ClassNotFoundException | NoSuchMethodException e) {
      throw new RuntimeException("Couldn't invoke ThreadManager.currentRequestThreadFactory", e);
    } catch (InvocationTargetException e) {
      throw Throwables.propagate(e.getCause());
    }
  }

    // TODO
  private static boolean isAppEngine() {
    if (System.getProperty("com.google.appengine.runtime.environment") == null) {
      return false;
    }
    try {
      // If the current environment is null, we're not inside AppEngine.
      return Class.forName("com.google.apphosting.api.ApiProxy")
              .getMethod("getCurrentEnvironment")
              .invoke(null)
          != null;
    } catch (ClassNotFoundException e) {
      // If ApiProxy doesn't exist, we're not on AppEngine at all.
      return false;
    } catch (InvocationTargetException e) {
      // If ApiProxy throws an exception, we're not in a proper AppEngine environment.
      return false;
    } catch (IllegalAccessException e) {
      // If the method isn't accessible, we're not on a supported version of AppEngine;
      return false;
    } catch (NoSuchMethodException e) {
      // If the method doesn't exist, we're not on a supported version of AppEngine;
      return false;
    }
  }

  /**
   * Creates a thread using {@link #platformThreadFactory}, and sets its name to {@code name} unless
   * changing the name is forbidden by the security manager.
   */
    // concurrency
  static Thread newThread(String name, Runnable runnable) {
    checkNotNull(name);
    checkNotNull(runnable);
    Thread result = platformThreadFactory().newThread(runnable);
    try {
      result.setName(name);
    } catch (SecurityException e) {
      // OK if we can't set the name in this environment.
    }
    return result;
  }

  // TODO(lukes): provide overloads for ListeningExecutorService? ListeningScheduledExecutorService?
  // TODO(lukes): provide overloads that take constant strings? Function<Runnable, String>s to
  // calculate names?

  /**
   * Creates an {@link Executor} that renames the {@link Thread threads} that its tasks run in.
   *
   * <p>The names are retrieved from the {@code nameSupplier} on the thread that is being renamed
   * right before each task is run. The renaming is best effort, if a {@link SecurityManager}
   * prevents the renaming then it will be skipped but the tasks will still execute.
   *
   *
   * @param executor The executor to decorate
   * @param nameSupplier The source of names for each task
   */
    // concurrency
  static Executor renamingDecorator(final Executor executor, final Supplier<String> nameSupplier) {
    checkNotNull(executor);
    checkNotNull(nameSupplier);
    if (isAppEngine()) {
      // AppEngine doesn't support thread renaming, so don't even try
      return executor;
    }
    return new Executor() {
      @Override
      public void execute(Runnable command) {
        executor.execute(Callables.threadRenaming(command, nameSupplier));
      }
    };
  }

  /**
   * Creates an {@link ExecutorService} that renames the {@link Thread threads} that its tasks run
   * in.
   *
   * <p>The names are retrieved from the {@code nameSupplier} on the thread that is being renamed
   * right before each task is run. The renaming is best effort, if a {@link SecurityManager}
   * prevents the renaming then it will be skipped but the tasks will still execute.
   *
   *
   * @param service The executor to decorate
   * @param nameSupplier The source of names for each task
   */
    // concurrency
  static ExecutorService renamingDecorator(
      final ExecutorService service, final Supplier<String> nameSupplier) {
    checkNotNull(service);
    checkNotNull(nameSupplier);
    if (isAppEngine()) {
      // AppEngine doesn't support thread renaming, so don't even try.
      return service;
    }
    return new WrappingExecutorService(service) {
      @Override
      protected <T> Callable<T> wrapTask(Callable<T> callable) {
        return Callables.threadRenaming(callable, nameSupplier);
      }

      @Override
      protected Runnable wrapTask(Runnable command) {
        return Callables.threadRenaming(command, nameSupplier);
      }
    };
  }

  /**
   * Creates a {@link ScheduledExecutorService} that renames the {@link Thread threads} that its
   * tasks run in.
   *
   * <p>The names are retrieved from the {@code nameSupplier} on the thread that is being renamed
   * right before each task is run. The renaming is best effort, if a {@link SecurityManager}
   * prevents the renaming then it will be skipped but the tasks will still execute.
   *
   *
   * @param service The executor to decorate
   * @param nameSupplier The source of names for each task
   */
    // concurrency
  static ScheduledExecutorService renamingDecorator(
      final ScheduledExecutorService service, final Supplier<String> nameSupplier) {
    checkNotNull(service);
    checkNotNull(nameSupplier);
    if (isAppEngine()) {
      // AppEngine doesn't support thread renaming, so don't even try.
      return service;
    }
    return new WrappingScheduledExecutorService(service) {
      @Override
      protected <T> Callable<T> wrapTask(Callable<T> callable) {
        return Callables.threadRenaming(callable, nameSupplier);
      }

      @Override
      protected Runnable wrapTask(Runnable command) {
        return Callables.threadRenaming(command, nameSupplier);
      }
    };
  }

  /**
   * Shuts down the given executor service gradually, first disabling new submissions and later, if
   * necessary, cancelling remaining tasks.
   *
   * <p>The method takes the following steps:
   *
   * <ol>
   *   <li>calls {@link ExecutorService#shutdown()}, disabling acceptance of new submitted tasks.
   *   <li>awaits executor service termination for half of the specified timeout.
   *   <li>if the timeout expires, it calls {@link ExecutorService#shutdownNow()}, cancelling
   *       pending tasks and interrupting running tasks.
   *   <li>awaits executor service termination for the other half of the specified timeout.
   * </ol>
   *
   * <p>If, at any step of the process, the calling thread is interrupted, the method calls {@link
   * ExecutorService#shutdownNow()} and returns.
   *
   * @param service the {@code ExecutorService} to shut down
   * @param timeout the maximum time to wait for the {@code ExecutorService} to terminate
   * @param unit the time unit of the timeout argument
   * @return {@code true} if the {@code ExecutorService} was terminated successfully, {@code false}
   *     if the call timed out or was interrupted
   * @since 17.0
   */

  @CanIgnoreReturnValue
    // concurrency
  public static boolean shutdownAndAwaitTermination(
      ExecutorService service, long timeout, TimeUnit unit) {
    long halfTimeoutNanos = unit.toNanos(timeout) / 2;
    // Disable new tasks from being submitted
    service.shutdown();
    try {
      // Wait for half the duration of the timeout for existing tasks to terminate
      if (!service.awaitTermination(halfTimeoutNanos, TimeUnit.NANOSECONDS)) {
        // Cancel currently executing tasks
        service.shutdownNow();
        // Wait the other half of the timeout for tasks to respond to being cancelled
        service.awaitTermination(halfTimeoutNanos, TimeUnit.NANOSECONDS);
      }
    } catch (InterruptedException ie) {
      // Preserve interrupt status
      Thread.currentThread().interrupt();
      // (Re-)Cancel if current thread also interrupted
      service.shutdownNow();
    }
    return service.isTerminated();
  }

  /**
   * Returns an Executor that will propagate {@link RejectedExecutionException} from the delegate
   * executor to the given {@code future}.
   *
   * <p>Note, the returned executor can only be used once.
   */
  static Executor rejectionPropagatingExecutor(
      final Executor delegate, final AbstractFuture<?> future) {
    checkNotNull(delegate);
    checkNotNull(future);
    if (delegate == directExecutor()) {
      // directExecutor() cannot throw RejectedExecutionException
      return delegate;
    }
    return new Executor() {
      boolean thrownFromDelegate = true;

      @Override
      public void execute(final Runnable command) {
        try {
          delegate.execute(
              new Runnable() {
                @Override
                public void run() {
                  thrownFromDelegate = false;
                  command.run();
                }
              });
        } catch (RejectedExecutionException e) {
          if (thrownFromDelegate) {
            // wrap exception?
            future.setException(e);
          }
          // otherwise it must have been thrown from a transitive call and the delegate runnable
          // should have handled it.
        }
      }
    };
  }
}
