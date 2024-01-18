/*
 * Copyright (C) 2018 The Guava Authors
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

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

import com.google.common.util.concurrent.InterruptibleTask.Blocker;
import java.nio.channels.spi.AbstractInterruptibleChannel;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;
import junit.framework.TestCase;
import org.checkerframework.checker.nullness.qual.Nullable;

public final class InterruptibleTaskTest extends TestCase {

  // Regression test for a deadlock where a task could be stuck busy waiting for the task to
  // transition to DONE
  public void testInterruptThrows() throws Exception {
    final CountDownLatch isInterruptibleRegistered = new CountDownLatch(1);
    InterruptibleTask<@Nullable Void> task =
        new InterruptibleTask<@Nullable Void>() {
          @Override
          @Nullable Void runInterruptibly() throws Exception {
            BrokenChannel bc = new BrokenChannel();
            bc.doBegin();
            isInterruptibleRegistered.countDown();
            new CountDownLatch(1).await(); // the interrupt will wake us up
            return null;
          }

          @Override
          boolean isDone() {
            return false;
          }

          @Override
          String toPendingString() {
            return "";
          }

          @Override
          void afterRanInterruptiblySuccess(@Nullable Void result) {}

          @Override
          void afterRanInterruptiblyFailure(Throwable error) {}
        };
    Thread runner = new Thread(task);
    runner.start();
    isInterruptibleRegistered.await();
    RuntimeException expected = assertThrows(RuntimeException.class, () -> task.interruptTask());
    assertThat(expected)
        .hasMessageThat()
        .isEqualTo("I bet you didn't think Thread.interrupt could throw");
    // We need to wait for the runner to exit.  It used to be that the runner would get stuck in the
    // busy loop when interrupt threw.
    runner.join(TimeUnit.SECONDS.toMillis(10));
  }

  static final class BrokenChannel extends AbstractInterruptibleChannel {
    @Override
    protected void implCloseChannel() {
      throw new RuntimeException("I bet you didn't think Thread.interrupt could throw");
    }

    void doBegin() {
      super.begin();
    }
  }

  /**
   * Because Thread.interrupt() can invoke arbitrary code, it can be slow (e.g. perform IO). To
   * protect ourselves from that we want to make sure that tasks don't spin too much waiting for the
   * interrupting thread to complete the protocol.
   */
  /*
   * This test hangs (or maybe is just *very* slow) under Android.
   *
   * TODO(b/218700094): Ideally, get this to pass under Android. Failing that, convince ourselves
   * that the test isn't exposing a real problem with InterruptibleTask, one that could matter in
   * prod.
   */
  @AndroidIncompatible
  public void testInterruptIsSlow() throws Exception {
    final CountDownLatch isInterruptibleRegistered = new CountDownLatch(1);
    final SlowChannel slowChannel = new SlowChannel();
    final InterruptibleTask<@Nullable Void> task =
        new InterruptibleTask<@Nullable Void>() {
          @Override
          @Nullable Void runInterruptibly() throws Exception {
            slowChannel.doBegin();
            isInterruptibleRegistered.countDown();
            try {
              new CountDownLatch(1).await(); // the interrupt will wake us up
            } catch (InterruptedException ie) {
              // continue
            }
            LockSupport.unpark(Thread.currentThread()); // simulate a spurious wakeup.
            return null;
          }

          @Override
          boolean isDone() {
            return false;
          }

          @Override
          String toPendingString() {
            return "";
          }

          @Override
          void afterRanInterruptiblySuccess(@Nullable Void result) {}

          @Override
          void afterRanInterruptiblyFailure(Throwable error) {}
        };
    Thread runner = new Thread(task, "runner");
    runner.start();
    isInterruptibleRegistered.await();
    // trigger the interrupt on another thread since it will block
    Thread interrupter =
        new Thread("Interrupter") {
          @Override
          public void run() {
            task.interruptTask();
          }
        };
    interrupter.start();
    // this will happen once the interrupt has been set which means that
    // 1. the runner has been woken up
    // 2. the interrupter is stuck in the call the Thread.interrupt()

    // after some period of time the runner thread should become blocked on the task because it is
    // waiting for the slow interrupting thread to complete Thread.interrupt
    awaitBlockedOnInstanceOf(runner, InterruptibleTask.Blocker.class);

    Blocker blocker = (Blocker) LockSupport.getBlocker(runner);
    Thread owner = blocker.getOwner();
    assertThat(owner).isSameInstanceAs(interrupter);

    slowChannel.exitClose.countDown(); // release the interrupter

    // We need to wait for the runner to exit.  To make sure that the interrupting thread wakes it
    // back up.
    runner.join(TimeUnit.SECONDS.toMillis(10));
  }

  // waits for the given thread to be blocked on the given object
  private static void awaitBlockedOnInstanceOf(Thread t, Class<?> blocker)
      throws InterruptedException {
    while (!isThreadBlockedOnInstanceOf(t, blocker)) {
      if (t.getState() == Thread.State.TERMINATED) {
        throw new RuntimeException("Thread " + t + " exited unexpectedly");
      }
      Thread.sleep(1);
    }
  }

  private static boolean isThreadBlockedOnInstanceOf(Thread t, Class<?> blocker) {
    return t.getState() == Thread.State.WAITING && blocker.isInstance(LockSupport.getBlocker(t));
  }

  static final class SlowChannel extends AbstractInterruptibleChannel {
    final CountDownLatch exitClose = new CountDownLatch(1);

    @Override
    protected void implCloseChannel() {
      Uninterruptibles.awaitUninterruptibly(exitClose);
    }

    void doBegin() {
      super.begin();
    }
  }
}
