/*
 * Copyright (C) 2005 The Guava Authors
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

package com.google.common.base;

import com.google.common.base.internal.Finalizer;
import com.google.common.testing.GcFinalization;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.Collections;
import junit.framework.TestCase;

/**
 * Unit test for {@link FinalizableReferenceQueue}.
 *
 * @author Bob Lee
 */
public class FinalizableReferenceQueueTest extends TestCase {

  private FinalizableReferenceQueue frq;

  @Override
  protected void tearDown() throws Exception {
    frq = null;
  }

  public void testFinalizeReferentCalled() {
    final MockReference reference = new MockReference(frq = new FinalizableReferenceQueue());

    GcFinalization.awaitDone(
        new GcFinalization.FinalizationPredicate() {
          @Override
          public boolean isDone() {
            return reference.finalizeReferentCalled;
          }
        });
  }

  static class MockReference extends FinalizableWeakReference<Object> {

    volatile boolean finalizeReferentCalled;

    MockReference(FinalizableReferenceQueue frq) {
      super(new Object(), frq);
    }

    @Override
    public void finalizeReferent() {
      finalizeReferentCalled = true;
    }
  }

  /**
   * Keeps a weak reference to the underlying reference queue. When this reference is cleared, we
   * know that the background thread has stopped and released its strong reference.
   */
  private WeakReference<ReferenceQueue<Object>> queueReference;

  public void testThatFinalizerStops() {
    weaklyReferenceQueue();
    GcFinalization.awaitClear(queueReference);
  }

  /** If we don't keep a strong reference to the reference object, it won't be enqueued. */
  FinalizableWeakReference<Object> reference;

  /** Create the FRQ in a method that goes out of scope so that we're sure it will be reclaimed. */
  private void weaklyReferenceQueue() {
    frq = new FinalizableReferenceQueue();
    queueReference = new WeakReference<>(frq.queue);

    /*
     * Queue and clear a reference for good measure. We test later on that
     * the finalizer thread stopped, but we should test that it actually
     * started first.
     */
    reference =
        new FinalizableWeakReference<Object>(new Object(), frq) {
          @Override
          public void finalizeReferent() {
            reference = null;
            frq = null;
          }
        };
  }

  @AndroidIncompatible // no concept of separate ClassLoaders
  public void testDecoupledLoader() {
    FinalizableReferenceQueue.DecoupledLoader decoupledLoader =
        new FinalizableReferenceQueue.DecoupledLoader() {
          @Override
          URLClassLoader newLoader(URL base) {
            return new DecoupledClassLoader(new URL[] {base});
          }
        };

    Class<?> finalizerCopy = decoupledLoader.loadFinalizer();

    assertNotNull(finalizerCopy);
    assertNotSame(Finalizer.class, finalizerCopy);

    assertNotNull(FinalizableReferenceQueue.getStartFinalizer(finalizerCopy));
  }

  static class DecoupledClassLoader extends URLClassLoader {

    public DecoupledClassLoader(URL[] urls) {
      super(urls);
    }

    @Override
    protected synchronized Class<?> loadClass(String name, boolean resolve)
        throws ClassNotFoundException {
      // Force Finalizer to load from this class loader, not its parent.
      if (name.equals(Finalizer.class.getName())) {
        Class<?> clazz = findClass(name);
        if (resolve) {
          resolveClass(clazz);
        }
        return clazz;
      }

      return super.loadClass(name, resolve);
    }
  }

  @AndroidIncompatible // TODO(cpovirk): How significant is this failure?
  public void testGetFinalizerUrl() {
    assertNotNull(getClass().getResource("internal/Finalizer.class"));
  }

  public void testFinalizeClassHasNoNestedClasses() throws Exception {
    // Ensure that the Finalizer class has no nested classes.
    // See https://code.google.com/p/guava-libraries/issues/detail?id=1505
    assertEquals(Collections.emptyList(), Arrays.asList(Finalizer.class.getDeclaredClasses()));
  }
}
