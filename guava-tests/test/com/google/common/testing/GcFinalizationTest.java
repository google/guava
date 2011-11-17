// Copyright 2011 Google Inc. All Rights Reserved.

package com.google.common.testing;

import com.google.common.testing.GcFinalization.FinalizationPredicate;
import com.google.common.util.concurrent.SettableFuture;

import junit.framework.TestCase;

import java.lang.ref.WeakReference;
import java.util.WeakHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Tests for {@link GcFinalization}.
 *
 * @author martinrb@google.com (Martin Buchholz)
 * @author schmoe@google.com (mike nonemacher)
 */

public class GcFinalizationTest extends TestCase {

  //----------------------------------------------------------------
  // Ordinary tests of successful method execution
  //----------------------------------------------------------------

  public void testAwait_CountDownLatch() {
    final CountDownLatch latch = new CountDownLatch(1);
    Object x = new Object() {
      protected void finalize() { latch.countDown(); }
    };
    x = null;  // Hint to the JIT that x is unreachable
    GcFinalization.await(latch);
    assertEquals(0, latch.getCount());
  }

  public void testAwaitDone_Future() {
    final SettableFuture<Void> future = SettableFuture.create();
    Object x = new Object() {
      protected void finalize() { future.set(null); }
    };
    x = null;  // Hint to the JIT that x is unreachable
    GcFinalization.awaitDone(future);
    assertTrue(future.isDone());
    assertFalse(future.isCancelled());
  }

  public void testAwaitDone_Future_Cancel() {
    final SettableFuture<Void> future = SettableFuture.create();
    Object x = new Object() {
      protected void finalize() { future.cancel(false); }
    };
    x = null;  // Hint to the JIT that x is unreachable
    GcFinalization.awaitDone(future);
    assertTrue(future.isDone());
    assertTrue(future.isCancelled());
  }

  public void testAwaitClear() {
    final WeakReference<Object> ref = new WeakReference<Object>(new Object());
    GcFinalization.awaitClear(ref);
    assertNull(ref.get());
  }

  public void testAwaitDone_FinalizationPredicate() {
    final WeakHashMap<Object, Object> map = new WeakHashMap<Object, Object>();
    map.put(new Object(), Boolean.TRUE);
    GcFinalization.awaitDone(new FinalizationPredicate() {
      public boolean isDone() {
        return map.isEmpty();
      }
    });
    assertTrue(map.isEmpty());
  }

  //----------------------------------------------------------------
  // Test that interrupts result in RuntimeException, not InterruptedException.
  // Trickier than it looks, because runFinalization swallows interrupts.
  //----------------------------------------------------------------

  class Interruptenator extends Thread {
    final AtomicBoolean shutdown;
    Interruptenator(final Thread interruptee) {
      this(interruptee, new AtomicBoolean(false));
    }
    Interruptenator(final Thread interruptee,
                    final AtomicBoolean shutdown) {
      super(new Runnable() {
          public void run() {
            while (!shutdown.get()) {
              interruptee.interrupt();
              Thread.yield();
            }}});
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
    assertTrue(e.getMessage().contains("Unexpected interrupt"));
    assertTrue(e.getCause() instanceof InterruptedException);
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
        GcFinalization.awaitDone(new FinalizationPredicate() {
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

}
