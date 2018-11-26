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

import com.google.common.testing.GcFinalization.FinalizationPredicate;
import com.google.common.util.concurrent.SettableFuture;
import java.lang.ref.WeakReference;
import java.util.WeakHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import junit.framework.TestCase;

/**
 * Tests for {@link GcFinalization}.
 *
 * @author Martin Buchholz
 * @author mike nonemacher
 */

public class GcFinalizationTest extends TestCase {

  // ----------------------------------------------------------------
  // Ordinary tests of successful method execution
  // ----------------------------------------------------------------

  public void testAwait_CountDownLatch() {
    final CountDownLatch latch = new CountDownLatch(1);
    Object x =
        new Object() {
          @Override
          protected void finalize() {
            latch.countDown();
          }
        };
    x = null; // Hint to the JIT that x is unreachable
    GcFinalization.await(latch);
    assertEquals(0, latch.getCount());
  }

  public void testAwaitDone_Future() {
    final SettableFuture<Void> future = SettableFuture.create();
    Object x =
        new Object() {
          @Override
          protected void finalize() {
            future.set(null);
          }
        };
    x = null; // Hint to the JIT that x is unreachable
    GcFinalization.awaitDone(future);
    assertTrue(future.isDone());
    assertFalse(future.isCancelled());
  }

  public void testAwaitDone_Future_Cancel() {
    final SettableFuture<Void> future = SettableFuture.create();
    Object x =
        new Object() {
          @Override
          protected void finalize() {
            future.cancel(false);
          }
        };
    x = null; // Hint to the JIT that x is unreachable
    GcFinalization.awaitDone(future);
    assertTrue(future.isDone());
    assertTrue(future.isCancelled());
  }

  public void testAwaitClear() {
    final WeakReference<Object> ref = new WeakReference<>(new Object());
    GcFinalization.awaitClear(ref);
    assertNull(ref.get());
  }

  public void testAwaitDone_FinalizationPredicate() {
    final WeakHashMap<Object, Object> map = new WeakHashMap<>();
    map.put(new Object(), Boolean.TRUE);
    GcFinalization.awaitDone(
        new FinalizationPredicate() {
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

    Interruptenator(final Thread interruptee) {
      this(interruptee, new AtomicBoolean(false));
    }

    Interruptenator(final Thread interruptee, final AtomicBoolean shutdown) {
      super(
          new Runnable() {
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

  public void testAwait_CountDownLatch_Interrupted() {
    Interruptenator interruptenator = new Interruptenator(Thread.currentThread());
    try {
      final CountDownLatch latch = new CountDownLatch(1);
      try {
        GcFinalization.await(latch);
        fail("should throw");
      } catch (RuntimeException expected) {
        assertWrapsInterruptedException(expected);
      }
    } finally {
      interruptenator.shutdown();
      Thread.interrupted();
    }
  }

  public void testAwaitDone_Future_Interrupted_Interrupted() {
    Interruptenator interruptenator = new Interruptenator(Thread.currentThread());
    try {
      final SettableFuture<Void> future = SettableFuture.create();
      try {
        GcFinalization.awaitDone(future);
        fail("should throw");
      } catch (RuntimeException expected) {
        assertWrapsInterruptedException(expected);
      }
    } finally {
      interruptenator.shutdown();
      Thread.interrupted();
    }
  }

  public void testAwaitClear_Interrupted() {
    Interruptenator interruptenator = new Interruptenator(Thread.currentThread());
    try {
      final WeakReference<Object> ref = new WeakReference<Object>(Boolean.TRUE);
      try {
        GcFinalization.awaitClear(ref);
        fail("should throw");
      } catch (RuntimeException expected) {
        assertWrapsInterruptedException(expected);
      }
    } finally {
      interruptenator.shutdown();
      Thread.interrupted();
    }
  }

  public void testAwaitDone_FinalizationPredicate_Interrupted() {
    Interruptenator interruptenator = new Interruptenator(Thread.currentThread());
    try {
      try {
        GcFinalization.awaitDone(
            new FinalizationPredicate() {
              public boolean isDone() {
                return false;
              }
            });
        fail("should throw");
      } catch (RuntimeException expected) {
        assertWrapsInterruptedException(expected);
      }
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
    final CountDownLatch finalizerRan = new CountDownLatch(1);
    final WeakReference<Object> ref =
        new WeakReference<Object>(
            new Object() {
              @Override
              protected void finalize() {
                finalizerRan.countDown();
              }
            });

    // Don't copy this into your own test!
    // Use e.g. awaitClear or await(CountDownLatch) instead.
    GcFinalization.awaitFullGc();

    // If this test turns out to be flaky, add a second call to awaitFullGc()
    // GcFinalization.awaitFullGc();

    assertEquals(0, finalizerRan.getCount());
    assertNull(ref.get());
  }
}
