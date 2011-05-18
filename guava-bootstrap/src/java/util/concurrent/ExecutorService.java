/*
 * This file is a modified version of 
 * http://gee.cs.oswego.edu/cgi-bin/viewcvs.cgi/jsr166/src/main/java/util/concurrent/ExecutorService.java?revision=1.51
 * which contained the following notice:
 *
 * Written by Doug Lea with assistance from members of JCP JSR-166
 * Expert Group and released to the public domain, as explained at
 * http://creativecommons.org/publicdomain/zero/1.0/
 */

package java.util.concurrent;

import java.util.Collection;
import java.util.List;

public interface ExecutorService extends Executor {
  void shutdown();

  List<Runnable> shutdownNow();

  boolean isShutdown();

  boolean isTerminated();

  boolean awaitTermination(long timeout, TimeUnit unit)
      throws InterruptedException;

  <T> Future<T> submit(Callable<T> task);

  <T> Future<T> submit(Runnable task, T result);

  Future<?> submit(Runnable task);

  <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks)
      throws InterruptedException;

  <T> List<Future<T>> invokeAll(
      Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
      throws InterruptedException;

  <T> T invokeAny(Collection<? extends Callable<T>> tasks)
      throws InterruptedException, ExecutionException;

  <T> T invokeAny(
      Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
      throws InterruptedException, ExecutionException, TimeoutException;
}
