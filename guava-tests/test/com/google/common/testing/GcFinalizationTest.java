// Copyright 2011 Google Inc. All Rights Reserved.

package com.google.common.testing;

import com.google.common.testing.GcFinalization.FinalizationPredicate;
import com.google.common.util.concurrent.SettableFuture;

import junit.framework.TestCase;

import java.lang.ref.WeakReference;
import java.util.WeakHashMap;
import java.util.concurrent.CountDownLatch;

/**
 * Tests for {@link GcFinalization}.
 *
 * @author martinrb@google.com (Martin Buchholz)
 * @author schmoe@google.com (mike nonemacher)
 */

public class GcFinalizationTest extends TestCase {

  public void testAwait_CountDownLatch() throws Exception {
    final CountDownLatch latch = new CountDownLatch(1);
    Object x = new Object() {
      protected void finalize() { latch.countDown(); }
    };
    x = null;  // Hint to the JIT that x is unreachable
    GcFinalization.await(latch);
    assertEquals(0, latch.getCount());
  }

  public void testAwaitDone_Future() throws Exception {
    final SettableFuture<Void> future = SettableFuture.create();
    Object x = new Object() {
      protected void finalize() { future.set(null); }
    };
    x = null;  // Hint to the JIT that x is unreachable
    GcFinalization.awaitDone(future);
    assertTrue(future.isDone());
    assertFalse(future.isCancelled());
  }

  public void testAwaitDone_Future_Cancel() throws Exception {
    final SettableFuture<Void> future = SettableFuture.create();
    Object x = new Object() {
      protected void finalize() { future.cancel(false); }
    };
    x = null;  // Hint to the JIT that x is unreachable
    GcFinalization.awaitDone(future);
    assertTrue(future.isDone());
    assertTrue(future.isCancelled());
  }

  public void testAwaitClear() throws Exception {
    final WeakReference<Object> ref = new WeakReference<Object>(new Object());
    GcFinalization.awaitClear(ref);
    assertNull(ref.get());
  }

  public void testAwaitDone_FinalizationPredicate() throws Exception {
    final WeakHashMap<Object, Object> map = new WeakHashMap<Object, Object>();
    map.put(new Object(), Boolean.TRUE);
    GcFinalization.awaitDone(new FinalizationPredicate() {
      public boolean isDone() {
        return map.isEmpty();
      }
    });
    assertTrue(map.isEmpty());
  }
}
