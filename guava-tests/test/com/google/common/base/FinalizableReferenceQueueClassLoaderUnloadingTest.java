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

import com.google.common.testing.GcFinalization;

import junit.framework.TestCase;

import java.io.Closeable;
import java.lang.ref.WeakReference;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.CodeSource;
import java.security.Permission;
import java.security.PermissionCollection;
import java.security.Policy;
import java.security.ProtectionDomain;
import java.util.concurrent.Callable;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Tests that the {@code ClassLoader} of {@link FinalizableReferenceQueue} can be unloaded. These
 * tests are separate from {@link FinalizableReferenceQueueTest} so that they can be excluded from
 * coverage runs, as the coverage system interferes with them.
 *
 * @author Eamonn McManus
 */
public class FinalizableReferenceQueueClassLoaderUnloadingTest extends TestCase {

  /*
   * The following tests check that the use of FinalizableReferenceQueue does not prevent the
   * ClassLoader that loaded that class from later being garbage-collected. If anything continues
   * to reference the FinalizableReferenceQueue class then its ClassLoader cannot be
   * garbage-collected, even if there are no more instances of FinalizableReferenceQueue itself.
   * The code in FinalizableReferenceQueue goes to considerable trouble to ensure that there are
   * no such references and the tests here check that that trouble has not been in vain.
   *
   * When we reference FinalizableReferenceQueue in this test, we are referencing a class that is
   * loaded by this test and that will obviously remain loaded for as long as the test is running.
   * So in order to check ClassLoader garbage collection we need to create a new ClassLoader and
   * make it load its own version of FinalizableReferenceQueue. Then we need to interact with that
   * parallel version through reflection in order to exercise the parallel
   * FinalizableReferenceQueue, and then check that the parallel ClassLoader can be
   * garbage-collected after that.
   */

  public static class MyFinalizableWeakReference extends FinalizableWeakReference<Object> {
    public MyFinalizableWeakReference(Object x, FinalizableReferenceQueue queue) {
      super(x, queue);
    }

    @Override
    public void finalizeReferent() {
    }
  }

  private static class PermissivePolicy extends Policy {
    @Override
    public boolean implies(ProtectionDomain pd, Permission perm) {
      return true;
    }

    @Override
    public PermissionCollection getPermissions(CodeSource codesource) {
      throw new AssertionError();
    }

    @Override
    public void refresh() {
      throw new AssertionError();
    }
  }

  private WeakReference<ClassLoader> useFrqInSeparateLoader() throws Exception {
    final URLClassLoader myLoader = (URLClassLoader) getClass().getClassLoader();
    final URL[] urls = myLoader.getURLs();
    URLClassLoader sepLoader = new URLClassLoader(urls, myLoader.getParent());
    // sepLoader is the loader that we will use to load the parallel FinalizableReferenceQueue (FRQ)
    // and friends, and that we will eventually expect to see garbage-collected. The assumption
    // is that the ClassLoader of this test is a URLClassLoader, and that it loads FRQ itself
    // rather than delegating to a parent ClassLoader. If this assumption is violated the test will
    // fail and will need to be rewritten.

    Class<?> frqC = FinalizableReferenceQueue.class;
    Class<?> sepFrqC = sepLoader.loadClass(frqC.getName());
    assertNotSame(frqC, sepFrqC);
    // Check the assumptions above.

    // FRQ tries to load the Finalizer class (for the reference-collecting thread) in a few ways.
    // If the class is accessible to the system ClassLoader (ClassLoader.getSystemClassLoader())
    // then FRQ does not bother to load Finalizer.class through a separate ClassLoader. That happens
    // in our test environment, which foils the purpose of this test, so we disable the logic for
    // our test by setting a static field. We are changing the field in the parallel version of FRQ
    // and each test creates its own one of those, so there is no test interference here.
    Class<?> sepFrqSystemLoaderC =
        sepLoader.loadClass(FinalizableReferenceQueue.SystemLoader.class.getName());
    Field disabled = sepFrqSystemLoaderC.getDeclaredField("disabled");
    disabled.setAccessible(true);
    disabled.set(null, true);

    // Now make a parallel FRQ and an associated FinalizableWeakReference to an object, in order to
    // exercise some classes from the parallel ClassLoader.
    AtomicReference<Object> sepFrqA = new AtomicReference<Object>(sepFrqC.newInstance());
    Class<?> sepFwrC = sepLoader.loadClass(MyFinalizableWeakReference.class.getName());
    Constructor<?> sepFwrCons = sepFwrC.getConstructor(Object.class, sepFrqC);
    // The object that we will wrap in FinalizableWeakReference is a Stopwatch.
    Class<?> sepStopwatchC = sepLoader.loadClass(Stopwatch.class.getName());
    assertSame(sepLoader, sepStopwatchC.getClassLoader());
    AtomicReference<Object> sepStopwatchA =
        new AtomicReference<Object>(sepStopwatchC.getMethod("createUnstarted").invoke(null));
    AtomicReference<WeakReference<?>> sepStopwatchRef = new AtomicReference<WeakReference<?>>(
        (WeakReference<?>) sepFwrCons.newInstance(sepStopwatchA.get(), sepFrqA.get()));
    assertNotNull(sepStopwatchA.get());
    // Clear all references to the Stopwatch and wait for it to be gc'd.
    sepStopwatchA.set(null);
    GcFinalization.awaitClear(sepStopwatchRef.get());
    // Return a weak reference to the parallel ClassLoader. This is the reference that should
    // eventually become clear if there are no other references to the ClassLoader.
    return new WeakReference<ClassLoader>(sepLoader);
  }

  private void doTestUnloadable() throws Exception {
    WeakReference<ClassLoader> loaderRef = useFrqInSeparateLoader();
    GcFinalization.awaitClear(loaderRef);
  }

  public void testUnloadableWithoutSecurityManager() throws Exception {
    // Test that the use of a FinalizableReferenceQueue does not subsequently prevent the
    // loader of that class from being garbage-collected.
    SecurityManager oldSecurityManager = System.getSecurityManager();
    try {
      System.setSecurityManager(null);
      doTestUnloadable();
    } finally {
      System.setSecurityManager(oldSecurityManager);
    }
  }

  public void testUnloadableWithSecurityManager() throws Exception {
    // Test that the use of a FinalizableReferenceQueue does not subsequently prevent the
    // loader of that class from being garbage-collected even if there is a SecurityManager.
    // The SecurityManager environment makes such leaks more likely because when you create
    // a URLClassLoader with a SecurityManager, the creating code's AccessControlContext is
    // captured, and that references the creating code's ClassLoader.
    Policy oldPolicy = Policy.getPolicy();
    SecurityManager oldSecurityManager = System.getSecurityManager();
    try {
      Policy.setPolicy(new PermissivePolicy());
      System.setSecurityManager(new SecurityManager());
      doTestUnloadable();
    } finally {
      System.setSecurityManager(oldSecurityManager);
      Policy.setPolicy(oldPolicy);
    }
  }

  public static class FrqUser implements Callable<WeakReference<Object>> {
    public static FinalizableReferenceQueue frq = new FinalizableReferenceQueue();
    public static final Semaphore finalized = new Semaphore(0);

    @Override
    public WeakReference<Object> call() {
      WeakReference<Object> wr = new FinalizableWeakReference<Object>(new Integer(23), frq) {
        @Override
        public void finalizeReferent() {
          finalized.release();
        }
      };
      return wr;
    }
  }

  public void testUnloadableInStaticFieldIfClosed() throws Exception {
    Policy oldPolicy = Policy.getPolicy();
    SecurityManager oldSecurityManager = System.getSecurityManager();
    try {
      Policy.setPolicy(new PermissivePolicy());
      System.setSecurityManager(new SecurityManager());
      WeakReference<ClassLoader> loaderRef = doTestUnloadableInStaticFieldIfClosed();
      GcFinalization.awaitClear(loaderRef);
    } finally {
      System.setSecurityManager(oldSecurityManager);
      Policy.setPolicy(oldPolicy);
    }
  }

  // If you have a FinalizableReferenceQueue that is a static field of one of the classes of your
  // app (like the FrqUser class above), then the app's ClassLoader will never be gc'd. The reason
  // is that we attempt to run a thread in a separate ClassLoader that will detect when the FRQ
  // is no longer referenced, meaning that the app's ClassLoader has been gc'd, and when that
  // happens. But the thread's supposedly separate ClassLoader actually has a reference to the app's
  // ClasLoader via its AccessControlContext. It does not seem to be possible to make a
  // URLClassLoader without capturing this reference, and it probably would not be desirable for
  // security reasons anyway. Therefore, the FRQ.close() method provides a way to stop the thread
  // explicitly. This test checks that calling that method does allow an app's ClassLoader to be
  // gc'd even if there is a still a FinalizableReferenceQueue in a static field. (Setting the field
  // to null would also work, but only if there are no references to the FRQ anywhere else.)
  private WeakReference<ClassLoader> doTestUnloadableInStaticFieldIfClosed() throws Exception {
    final URLClassLoader myLoader = (URLClassLoader) getClass().getClassLoader();
    final URL[] urls = myLoader.getURLs();
    URLClassLoader sepLoader = new URLClassLoader(urls, myLoader.getParent());

    Class<?> frqC = FinalizableReferenceQueue.class;
    Class<?> sepFrqC = sepLoader.loadClass(frqC.getName());
    assertNotSame(frqC, sepFrqC);

    Class<?> sepFrqSystemLoaderC =
        sepLoader.loadClass(FinalizableReferenceQueue.SystemLoader.class.getName());
    Field disabled = sepFrqSystemLoaderC.getDeclaredField("disabled");
    disabled.setAccessible(true);
    disabled.set(null, true);

    Class<?> frqUserC = FrqUser.class;
    Class<?> sepFrqUserC = sepLoader.loadClass(frqUserC.getName());
    assertNotSame(frqUserC, sepFrqUserC);
    assertSame(sepLoader, sepFrqUserC.getClassLoader());

    Callable<?> sepFrqUser = (Callable<?>) sepFrqUserC.newInstance();
    WeakReference<?> finalizableWeakReference = (WeakReference<?>) sepFrqUser.call();

    GcFinalization.awaitClear(finalizableWeakReference);

    Field sepFrqUserFinalizedF = sepFrqUserC.getField("finalized");
    Semaphore finalizeCount = (Semaphore) sepFrqUserFinalizedF.get(null);
    boolean finalized = finalizeCount.tryAcquire(5, TimeUnit.SECONDS);
    assertTrue(finalized);

    Field sepFrqUserFrqF = sepFrqUserC.getField("frq");
    Closeable frq = (Closeable) sepFrqUserFrqF.get(null);
    frq.close();

    return new WeakReference<ClassLoader>(sepLoader);
  }
}
