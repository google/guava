/*
 * This file is a modified version of
 * http://gee.cs.oswego.edu/cgi-bin/viewcvs.cgi/jsr166/src/main/java/util/concurrent/CountDownLatch.java?revision=1.43
 * which contained the following notice:
 *
 * Written by Doug Lea with assistance from members of JCP JSR-166
 * Expert Group and released to the public domain, as explained at
 * http://creativecommons.org/publicdomain/zero/1.0/
 */

package java.util.concurrent;

/**
 * Emulation of CountDownLatch in GWT. Since GWT environment is single threaded, attempting to block
 * on the latch by calling {@link #await()} or {@link #await(long, TimeUnit)} when it is not ready
 * is considered illegal because it would lead to a deadlock. Both methods will throw {@link
 * IllegalStateException} to avoid the deadlock.
 */
public class CountDownLatch {

  private int count;

  public CountDownLatch(int count) {
    if (count < 0) {
      throw new IllegalArgumentException("count < 0");
    }
    this.count = count;
  }

  public void await() throws InterruptedException {
    if (count > 0) {
      throw new IllegalStateException("May not block. Count is " + count);
    }
  }

  public boolean await(long timeout, TimeUnit unit)
      throws InterruptedException {
    await();
    return true;
  }

  public void countDown() {
    if (count > 0) {
      count--;
    }
  }

  public long getCount() {
    return count;
  }

  public String toString() {
    return super.toString() + "[Count = " + count + "]";
  }
}
