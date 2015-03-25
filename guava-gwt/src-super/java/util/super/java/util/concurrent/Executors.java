/*
 * This file is a modified version of
 * http://gee.cs.oswego.edu/cgi-bin/viewcvs.cgi/jsr166/src/main/java/util/concurrent/Executors.java?revision=1.90
 * which contained the following notice:
 *
 * Written by Doug Lea with assistance from members of JCP JSR-166
 * Expert Group and released to the public domain, as explained at
 * http://creativecommons.org/publicdomain/zero/1.0/
 */

package java.util.concurrent;

/**
 * Emulation of executors.
 */
public class Executors {

  public static <T> Callable<T> callable(Runnable task, T result) {
    if (task == null) {
      throw new NullPointerException();
    }
    return new RunnableAdapter<T>(task, result);
  }

  public static Callable<Object> callable(Runnable task) {
    if (task == null) {
      throw new NullPointerException();
    }
    return new RunnableAdapter<Object>(task, null);
  }

  static final class RunnableAdapter<T> implements Callable<T> {

    final Runnable task;
    final T result;

    RunnableAdapter(Runnable task, T result) {
      this.task = task;
      this.result = result;
    }

    public T call() {
      task.run();
      return result;
    }
  }

  private Executors() {
  }
}
