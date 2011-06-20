/*
 * This file is a modified version of 
 * http://gee.cs.oswego.edu/cgi-bin/viewcvs.cgi/jsr166/src/main/java/util/concurrent/AbstractExecutorService.java?revision=1.35
 * which contained the following notice:
 *
 * Written by Doug Lea with assistance from members of JCP JSR-166 Expert Group and released to the
 * public domain, as explained at http://creativecommons.org/publicdomain/zero/1.0/
 *
 * Rationale for copying:
 * Guava targets JDK5, whose AbstractExecutorService class lacks the newTaskFor protected
 * customization methods needed by MoreExecutors.listeningDecorator. This class is a copy of
 * AbstractExecutorService from the JSR166 CVS repository. It contains the desired methods.
 */

package com.google.common.util.concurrent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Provides default implementations of {@link ListeningExecutorService} execution methods. This
 * class implements the <tt>submit</tt>, <tt>invokeAny</tt> and <tt>invokeAll</tt> methods using a
 * {@link ListenableFutureTask} returned by <tt>newTaskFor</tt>.  For example, the implementation of
 * <tt>submit(Runnable)</tt> creates an associated <tt>ListenableFutureTask</tt> that is executed
 * and returned.
 *
 * @author Doug Lea
 */
abstract class AbstractListeningExecutorService implements ListeningExecutorService {
  /**
   * Returns a <tt>ListenableFutureTask</tt> for the given runnable and default value.
   *
   * @param runnable the runnable task being wrapped
   * @param value the default value for the returned future
   * @return a <tt>ListenableFutureTask</tt> which when run will run the underlying runnable and
   *         which, as a <tt>Future</tt>, will yield the given value as its result and provide for
   *         cancellation of the underlying task.
   */
  private <T> ListenableFutureTask<T> newTaskFor(Runnable runnable, T value) {
    return ListenableFutureTask.create(runnable, value);
  }

  /**
   * Returns a <tt>ListenableFutureTask</tt> for the given callable task.
   *
   * @param callable the callable task being wrapped
   * @return a <tt>ListenableFutureTask</tt> which when run will call the underlying callable and
   *         which, as a <tt>Future</tt>, will yield the callable's result as its result and provide
   *         for cancellation of the underlying task.
   */
  private <T> ListenableFutureTask<T> newTaskFor(Callable<T> callable) {
    return ListenableFutureTask.create(callable);
  }

  /**
   * @throws RejectedExecutionException {@inheritDoc}
   * @throws NullPointerException {@inheritDoc}
   */
  @Override public ListenableFuture<?> submit(Runnable task) {
    if (task == null) {
      throw new NullPointerException();
    }
    ListenableFutureTask<Void> ftask = newTaskFor(task, null);
    execute(ftask);
    return ftask;
  }

  /**
   * @throws RejectedExecutionException {@inheritDoc}
   * @throws NullPointerException {@inheritDoc}
   */
  @Override public <T> ListenableFuture<T> submit(Runnable task, T result) {
    if (task == null) {
      throw new NullPointerException();
    }
    ListenableFutureTask<T> ftask = newTaskFor(task, result);
    execute(ftask);
    return ftask;
  }

  /**
   * @throws RejectedExecutionException {@inheritDoc}
   * @throws NullPointerException {@inheritDoc}
   */
  @Override public <T> ListenableFuture<T> submit(Callable<T> task) {
    if (task == null) {
      throw new NullPointerException();
    }
    ListenableFutureTask<T> ftask = newTaskFor(task);
    execute(ftask);
    return ftask;
  }

  /**
   * the main mechanics of invokeAny.
   */
  private <T> T doInvokeAny(Collection<? extends Callable<T>> tasks, boolean timed, long nanos)
      throws InterruptedException, ExecutionException, TimeoutException {
    if (tasks == null) {
      throw new NullPointerException();
    }
    int ntasks = tasks.size();
    if (ntasks == 0) {
      throw new IllegalArgumentException();
    }
    List<Future<T>> futures = new ArrayList<Future<T>>(ntasks);
    ExecutorCompletionService<T> ecs = new ExecutorCompletionService<T>(this);

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

      // Start one task for sure; the rest incrementally
      futures.add(ecs.submit(it.next()));
      --ntasks;
      int active = 1;

      for (;;) {
        Future<T> f = ecs.poll();
        if (f == null) {
          if (ntasks > 0) {
            --ntasks;
            futures.add(ecs.submit(it.next()));
            ++active;
          } else if (active == 0) {
            break;
          } else if (timed) {
            f = ecs.poll(nanos, TimeUnit.NANOSECONDS);
            if (f == null) {
              throw new TimeoutException();
            }
            long now = System.nanoTime();
            nanos -= now - lastTime;
            lastTime = now;
          } else {
            f = ecs.take();
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
      for (Future<T> f : futures)
        f.cancel(true);
    }
  }

  @Override public <T> T invokeAny(Collection<? extends Callable<T>> tasks)
      throws InterruptedException, ExecutionException {
    try {
      return doInvokeAny(tasks, false, 0);
    } catch (TimeoutException cannotHappen) {
      // assert false;
      return null;
    }
  }

  @Override public <T> T invokeAny(
      Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
      throws InterruptedException, ExecutionException, TimeoutException {
    return doInvokeAny(tasks, true, unit.toNanos(timeout));
  }

  @Override public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks)
      throws InterruptedException {
    if (tasks == null) {
      throw new NullPointerException();
    }
    List<Future<T>> futures = new ArrayList<Future<T>>(tasks.size());
    boolean done = false;
    try {
      for (Callable<T> t : tasks) {
        ListenableFutureTask<T> f = newTaskFor(t);
        futures.add(f);
        execute(f);
      }
      for (Future<T> f : futures) {
        if (!f.isDone()) {
          try {
            f.get();
          } catch (CancellationException ignore) {
          } catch (ExecutionException ignore) {
          }
        }
      }
      done = true;
      return futures;
    } finally {
      if (!done) {
        for (Future<T> f : futures)
          f.cancel(true);
      }
    }
  }

  @Override public <T> List<Future<T>> invokeAll(
      Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
      throws InterruptedException {
    if (tasks == null || unit == null) {
      throw new NullPointerException();
    }
    long nanos = unit.toNanos(timeout);
    List<Future<T>> futures = new ArrayList<Future<T>>(tasks.size());
    boolean done = false;
    try {
      for (Callable<T> t : tasks)
        futures.add(newTaskFor(t));

      long lastTime = System.nanoTime();

      // Interleave time checks and calls to execute in case
      // executor doesn't have any/much parallelism.
      Iterator<Future<T>> it = futures.iterator();
      while (it.hasNext()) {
        execute((Runnable) (it.next()));
        long now = System.nanoTime();
        nanos -= now - lastTime;
        lastTime = now;
        if (nanos <= 0) {
          return futures;
        }
      }

      for (Future<T> f : futures) {
        if (!f.isDone()) {
          if (nanos <= 0) {
            return futures;
          }
          try {
            f.get(nanos, TimeUnit.NANOSECONDS);
          } catch (CancellationException ignore) {
          } catch (ExecutionException ignore) {
          } catch (TimeoutException toe) {
            return futures;
          }
          long now = System.nanoTime();
          nanos -= now - lastTime;
          lastTime = now;
        }
      }
      done = true;
      return futures;
    } finally {
      if (!done) {
        for (Future<T> f : futures)
          f.cancel(true);
      }
    }
  }
}
