/*
 * Copyright (C) 2011 The Guava Authors
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

package com.google.common.testing;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

import com.google.common.testing.GcFinalization.FinalizationPredicate;
import com.google.common.util.concurrent.SettableFuture;
import java.lang.ref.WeakReference;
import java.util.WeakHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import junit.framework.TestCase;
import org.jspecify.annotations.NullUnmarked;
import org.jspecify.annotations.Nullable;

/**
 * Tests for {@link GcFinalization}.
 *
 * @author Martin Buchholz
 * @author mike nonemacher
 */
@AndroidIncompatible // depends on details of gc

@NullUnmarked
public class GcFinalizationTest extends TestCase {

  // ----------------------------------------------------------------
  // Ordinary tests of successful method execution
  // ----------------------------------------------------------------

  public void testAwait_countDownLatch() {
    CountDownLatch latch = new CountDownLatch(1);
    Object unused =
        new Object() {
          @SuppressWarnings({"removal", "Finalize"}) // b/260137033
          @Override
          protected void finalize() {
            latch.countDown();
          }
        };
    unused = null; // Hint to the JIT that unused is unreachable
    GcFinalization.await(latch);
    assertEquals(0, latch.getCount());
  }

  public void testAwaitDone_future() {
    SettableFuture<@Nullable Void> future = SettableFuture.create();
    Object unused =
        new Object() {
          @SuppressWarnings({"removal", "Finalize"}) // b/260137033
          @Override
          protected void finalize() {
            future.set(null);
          }
        };
    unused = null; // Hint to the JIT that unused is unreachable
    GcFinalization.awaitDone(future);
    assertTrue(future.isDone());
    assertFalse(future.isCancelled());
  }

  public void testAwaitDone_future_cancel() {
    SettableFuture<@Nullable Void> future = SettableFuture.create();
    Object unused =
        new Object() {
          @SuppressWarnings({"removal", "Finalize"}) // b/260137033
          @Override
          protected void finalize() {
            future.cancel(false);
          }
        };
    unused = null; // Hint to the JIT that unused is unreachable
    GcFinalization.awaitDone(future);
    assertTrue(future.isDone());
    assertTrue(future.isCancelled());
  }

  public void testAwaitClear() {
    WeakReference<Object> ref = new WeakReference<>(new Object());
    GcFinalization.awaitClear(ref);
    assertNull(ref.get());
  }

  public void testAwaitDone_finalizationPredicate() {
    WeakHashMap<Object, Object> map = new WeakHashMap<>();
    map.put(new Object(), Boolean.TRUE);
    GcFinalization.awaitDone(
        new FinalizationPredicate() {
          @Override
          public boolean isDone() {
            return map.isEmpty();
          }
        });
    assertTrue(map.isEmpty());
  }

  // ----------------------------------------------------------------
  // Test that interrupts result in RuntimeException, not InterruptedException.
  // Trickier than it looks, because runFinalization swallows interrupts.
  // ----------------------------------------------------------------

  class Interruptenator extends Thread {
    final AtomicBoolean shutdown;

    Interruptenator(Thread interruptee) {
      this(interruptee, new AtomicBoolean(false));
    }

    @SuppressWarnings("ThreadPriorityCheck") // TODO: b/175898629 - Consider onSpinWait.
    Interruptenator(Thread interruptee, AtomicBoolean shutdown) {
      super(
          new Runnable() {
            @Override
            public void run() {
              while (!shutdown.get()) {
                interruptee.interrupt();
                Thread.yield();
              }
            }
          });
      this.shutdown = shutdown;
      start();
    }

    @SuppressWarnings("ThreadPriorityCheck") // TODO: b/175898629 - Consider onSpinWait.
    void shutdown() {
      shutdown.set(true);
      while (this.isAlive()) {
        Thread.yield();
      }
    }
  }

  void assertWrapsInterruptedException(RuntimeException e) {
    assertThat(e).hasMessageThat().contains("Unexpected interrupt");
    assertThat(e).hasCauseThat().isInstanceOf(InterruptedException.class);
  }

  public void testAwait_countDownLatch_interrupted() {
    Interruptenator interruptenator = new Interruptenator(Thread.currentThread());
    try {
      CountDownLatch latch = new CountDownLatch(1);
      RuntimeException expected =
          assertThrows(RuntimeException.class, () -> GcFinalization.await(latch));
      assertWrapsInterruptedException(expected);
    } finally {
      interruptenator.shutdown();
      Thread.interrupted();
    }
  }

  public void testAwaitDone_future_interrupted_interrupted() {
    Interruptenator interruptenator = new Interruptenator(Thread.currentThread());
    try {
      SettableFuture<@Nullable Void> future = SettableFuture.create();
      RuntimeException expected =
          assertThrows(RuntimeException.class, () -> GcFinalization.awaitDone(future));
      assertWrapsInterruptedException(expected);
    } finally {
      interruptenator.shutdown();
      Thread.interrupted();
    }
  }

  public void testAwaitClear_interrupted() {
    Interruptenator interruptenator = new Interruptenator(Thread.currentThread());
    try {
      WeakReference<Object> ref = new WeakReference<Object>(Boolean.TRUE);
      RuntimeException expected =
          assertThrows(RuntimeException.class, () -> GcFinalization.awaitClear(ref));
      assertWrapsInterruptedException(expected);
    } finally {
      interruptenator.shutdown();
      Thread.interrupted();
    }
  }

  public void testAwaitDone_finalizationPredicate_interrupted() {
    Interruptenator interruptenator = new Interruptenator(Thread.currentThread());
    try {
      RuntimeException expected =
          assertThrows(
              RuntimeException.class,
              () ->
                  GcFinalization.awaitDone(
                      new FinalizationPredicate() {
                        @Override
                        public boolean isDone() {
                          return false;
                        }
                      }));
      assertWrapsInterruptedException(expected);
    } finally {
      interruptenator.shutdown();
      Thread.interrupted();
    }
  }

  /**
   * awaitFullGc() is not quite as reliable a way to ensure calling of a specific finalize method as
   * the more direct await* methods, but should be reliable enough in practice to avoid flakiness of
   * this test. (And if it isn't, we'd like to know about it first!)
   */
  public void testAwaitFullGc() {
    CountDownLatch finalizerRan = new CountDownLatch(1);
    WeakReference<Object> ref =
        new WeakReference<Object>(
            new Object() {
              @SuppressWarnings({"removal", "Finalize"}) // b/260137033
              @Override
              protected void finalize() {
                finalizerRan.countDown();
              }
            });

    // Don't copy this into your own test!
    // Use e.g. awaitClear or await(CountDownLatch) instead.
    GcFinalization.awaitFullGc();

    // Attempt to help with some flakiness that we've seen: b/387521512.
    GcFinalization.awaitFullGc();

    assertEquals(0, finalizerRan.getCount());
    assertNull(ref.get());
  }
}
