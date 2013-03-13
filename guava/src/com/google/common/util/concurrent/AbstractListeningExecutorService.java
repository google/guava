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

import static com.google.common.util.concurrent.MoreExecutors.invokeAnyImpl;

import com.google.common.annotations.Beta;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.annotation.Nullable;

/**
 * Implements {@link ListeningExecutorService} execution methods atop the abstract {@link #execute}
 * method. More concretely, the {@code submit}, {@code invokeAny} and {@code invokeAll} methods
 * create {@link ListenableFutureTask} instances and pass them to {@link #execute}.
 *
 * <p>In addition to {@link #execute}, subclasses must implement all methods related to shutdown and
 * termination.
 *
 * @author Doug Lea
 * @since 14.0
 */
@Beta
public abstract class AbstractListeningExecutorService implements ListeningExecutorService {
  @Override
  public ListenableFuture<?> submit(Runnable task) {
    ListenableFutureTask<Void> ftask = ListenableFutureTask.create(task, null);
    execute(ftask);
    return ftask;
  }

  @Override
  public <T> ListenableFuture<T> submit(Runnable task, @Nullable T result) {
    ListenableFutureTask<T> ftask = ListenableFutureTask.create(task, result);
    execute(ftask);
    return ftask;
  }

  @Override
  public <T> ListenableFuture<T> submit(Callable<T> task) {
    ListenableFutureTask<T> ftask = ListenableFutureTask.create(task);
    execute(ftask);
    return ftask;
  }

  @Override
  public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException,
      ExecutionException {
    try {
      return invokeAnyImpl(this, tasks, false, 0);
    } catch (TimeoutException cannotHappen) {
      throw new AssertionError();
    }
  }

  @Override
  public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
      throws InterruptedException, ExecutionException, TimeoutException {
    return invokeAnyImpl(this, tasks, true, unit.toNanos(timeout));
  }

  @Override
  public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks)
      throws InterruptedException {
    if (tasks == null) {
      throw new NullPointerException();
    }
    List<Future<T>> futures = new ArrayList<Future<T>>(tasks.size());
    boolean done = false;
    try {
      for (Callable<T> t : tasks) {
        ListenableFutureTask<T> f = ListenableFutureTask.create(t);
        futures.add(f);
        execute(f);
      }
      for (Future<T> f : futures) {
        if (!f.isDone()) {
          try {
            f.get();
          } catch (CancellationException ignore) {} catch (ExecutionException ignore) {}
        }
      }
      done = true;
      return futures;
    } finally {
      if (!done) {
        for (Future<T> f : futures) {
          f.cancel(true);
        }
      }
    }
  }

  @Override
  public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout,
      TimeUnit unit) throws InterruptedException {
    if (tasks == null || unit == null) {
      throw new NullPointerException();
    }
    long nanos = unit.toNanos(timeout);
    List<Future<T>> futures = new ArrayList<Future<T>>(tasks.size());
    boolean done = false;
    try {
      for (Callable<T> t : tasks) {
        futures.add(ListenableFutureTask.create(t));
      }

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
          } catch (CancellationException ignore) {} catch (ExecutionException ignore) {} catch (TimeoutException toe) {
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
        for (Future<T> f : futures) {
          f.cancel(true);
        }
      }
    }
  }
}
