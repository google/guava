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

import static com.google.common.truth.Truth.assertThat;

import com.google.common.annotations.GwtIncompatible;
import com.google.common.base.internal.Finalizer;
import com.google.common.collect.Sets;
import com.google.common.testing.GcFinalization;
import java.io.Closeable;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.ref.Cleaner;
import java.lang.ref.Cleaner.Cleanable;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.net.ServerSocket;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import org.jspecify.annotations.NullUnmarked;
import org.jspecify.annotations.Nullable;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Unit test for {@link FinalizableReferenceQueue}.
 *
 * @author Bob Lee
 */
// - depends on details of GC and classloading
// - .class files aren't available
// - possibly no real concept of separate ClassLoaders?
@AndroidIncompatible
@GwtIncompatible
@RunWith(JUnit4.class)
@NullUnmarked
public class FinalizableReferenceQueueTest {

  private @Nullable FinalizableReferenceQueue frq;

  @After
  public void tearDown() throws Exception {
    frq = null;
  }

  @Test
  public void testFinalizeReferentCalled() {
    MockReference reference = new MockReference(frq = new FinalizableReferenceQueue());

    GcFinalization.awaitDone(() -> reference.finalizeReferentCalled);
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

  @Test
  public void testThatFinalizerStops() {
    weaklyReferenceQueue();
    GcFinalization.awaitClear(queueReference);
  }

  /** If we don't keep a strong reference to the reference object, it won't be enqueued. */
  @Nullable FinalizableWeakReference<Object> reference;

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

  @Test
  public void testDecoupledLoader() {
    FinalizableReferenceQueue.DecoupledLoader decoupledLoader =
        new FinalizableReferenceQueue.DecoupledLoader() {
          @Override
          URLClassLoader newLoader(URL base) {
            return new DecoupledClassLoader(new URL[] {base});
          }
        };

    Class<?> finalizerCopy = decoupledLoader.loadFinalizer();

    assertThat(finalizerCopy).isNotNull();
    assertThat(finalizerCopy).isNotSameInstanceAs(Finalizer.class);

    assertThat(FinalizableReferenceQueue.getStartFinalizer(finalizerCopy)).isNotNull();
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

  @Test
  public void testGetFinalizerUrl() {
    assertThat(getClass().getResource("internal/Finalizer.class")).isNotNull();
  }

  @Test
  public void testFinalizeClassHasNoNestedClasses() throws Exception {
    // Ensure that the Finalizer class has no nested classes.
    // See https://github.com/google/guava/issues/1505
    assertThat(Finalizer.class.getDeclaredClasses()).isEmpty();
  }

  static class MyServerExampleWithFrq implements Closeable {
    private static final FinalizableReferenceQueue frq = new FinalizableReferenceQueue();

    private static final Set<Reference<?>> references = Sets.newConcurrentHashSet();

    private final ServerSocket serverSocket;

    private MyServerExampleWithFrq() throws IOException {
      this.serverSocket = new ServerSocket(0);
    }

    static MyServerExampleWithFrq create(AtomicBoolean finalizeReferentRan) throws IOException {
      MyServerExampleWithFrq myServer = new MyServerExampleWithFrq();
      ServerSocket serverSocket = myServer.serverSocket;
      Reference<?> reference =
          new FinalizablePhantomReference<MyServerExampleWithFrq>(myServer, frq) {
            @Override
            public void finalizeReferent() {
              references.remove(this);
              if (!serverSocket.isClosed()) {
                try {
                  serverSocket.close();
                  finalizeReferentRan.set(true);
                } catch (IOException e) {
                  throw new UncheckedIOException(e);
                }
              }
            }
          };
      references.add(reference);
      return myServer;
    }

    @Override
    public void close() throws IOException {
      serverSocket.close();
    }
  }

  private ServerSocket makeMyServerExampleWithFrq(AtomicBoolean finalizeReferentRan)
      throws IOException {
    MyServerExampleWithFrq myServer = MyServerExampleWithFrq.create(finalizeReferentRan);
    assertThat(myServer.serverSocket.isClosed()).isFalse();
    return myServer.serverSocket;
  }

  @Test
  public void testMyServerExampleWithFrq() throws Exception {
    AtomicBoolean finalizeReferentRan = new AtomicBoolean(false);
    ServerSocket serverSocket = makeMyServerExampleWithFrq(finalizeReferentRan);
    GcFinalization.awaitDone(finalizeReferentRan::get);
    assertThat(serverSocket.isClosed()).isTrue();
  }

  @SuppressWarnings("Java8ApiChecker")
  static class MyServerExampleWithCleaner implements AutoCloseable {
    private static final Cleaner cleaner = Cleaner.create();

    private static Runnable closeServerSocketRunnable(
        ServerSocket serverSocket, AtomicBoolean cleanerRan) {
      return () -> {
        try {
          serverSocket.close();
          cleanerRan.set(true);
        } catch (IOException e) {
          throw new UncheckedIOException(e);
        }
      };
    }

    private final ServerSocket serverSocket;
    private final Cleanable cleanable;

    MyServerExampleWithCleaner(AtomicBoolean cleanerRan) throws IOException {
      this.serverSocket = new ServerSocket(0);
      this.cleanable = cleaner.register(this, closeServerSocketRunnable(serverSocket, cleanerRan));
    }

    @Override
    public void close() {
      cleanable.clean();
    }
  }

  @SuppressWarnings("Java8ApiChecker")
  private ServerSocket makeMyServerExampleWithCleaner(AtomicBoolean cleanerRan) throws IOException {
    MyServerExampleWithCleaner myServer = new MyServerExampleWithCleaner(cleanerRan);
    assertThat(myServer.serverSocket.isClosed()).isFalse();
    return myServer.serverSocket;
  }

  @SuppressWarnings("Java8ApiChecker")
  @Test
  public void testMyServerExampleWithCleaner() throws Exception {
    try {
      Class.forName("java.lang.ref.Cleaner");
    } catch (ClassNotFoundException beforeJava9) {
      return;
    }
    AtomicBoolean cleanerRan = new AtomicBoolean(false);
    ServerSocket serverSocket = makeMyServerExampleWithCleaner(cleanerRan);
    GcFinalization.awaitDone(cleanerRan::get);
    assertThat(serverSocket.isClosed()).isTrue();
  }
}
