/*
 * Copyright (C) 2015 The Guava Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.common.util.concurrent;

import com.google.common.collect.ImmutableSet;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URLClassLoader;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Tests our AtomicHelper fallback strategies in AbstractFuture.
 *
 * <p>On different platforms AbstractFuture uses different strategies for its core synchronization
 * primitives. The strategies are all implemented as subtypes of AtomicHelper and the strategy is
 * selected in the static initializer of AbstractFuture. This is convenient and performant but
 * introduces some testing difficulties. This test exercises the two fallback strategies in abstract
 * future.
 *
 * <ul>
 *   <li>SafeAtomicHelper: uses AtomicReferenceFieldsUpdaters to implement synchronization
 *   <li>SynchronizedHelper: uses {@code synchronized} blocks for synchronization
 * </ul>
 *
 * To force selection of our fallback strategies we load {@link AbstractFuture} (and all of {@code
 * com.google.common.util.concurrent} in degenerate class loaders which make certain platform
 * classes unavailable. Then we construct a test suite so we can run the normal AbstractFutureTest
 * test methods in these degenerate classloaders.
 */

public class AbstractFutureFallbackAtomicHelperTest extends TestCase {

  // stash these in static fields to avoid loading them over and over again (speeds up test
  // execution significantly)

  /**
   * This classloader disallows {@link sun.misc.Unsafe}, which will prevent us from selecting our
   * preferred strategy {@code UnsafeAtomicHelper}.
   */
  private static final ClassLoader NO_UNSAFE =
      getClassLoader(ImmutableSet.of(sun.misc.Unsafe.class.getName()));

  /**
   * This classloader disallows {@link sun.misc.Unsafe} and {@link AtomicReferenceFieldUpdater},
   * which will prevent us from selecting our {@code SafeAtomicHelper} strategy.
   */
  private static final ClassLoader NO_ATOMIC_REFERENCE_FIELD_UPDATER =
      getClassLoader(
          ImmutableSet.of(
              sun.misc.Unsafe.class.getName(), AtomicReferenceFieldUpdater.class.getName()));

  public static TestSuite suite() {
    // we create a test suite containing a test for every AbstractFutureTest test method and we
    // set it as the name of the test.  Then in runTest we can reflectively load and invoke the
    // corresponding method on AbstractFutureTest in the correct classloader.
    TestSuite suite = new TestSuite(AbstractFutureFallbackAtomicHelperTest.class.getName());
    for (Method method : AbstractFutureTest.class.getDeclaredMethods()) {
      if (Modifier.isPublic(method.getModifiers()) && method.getName().startsWith("test")) {
        suite.addTest(
            TestSuite.createTest(AbstractFutureFallbackAtomicHelperTest.class, method.getName()));
      }
    }
    return suite;
  }

  @Override
  public void runTest() throws Exception {
    // First ensure that our classloaders are initializing the correct helper versions
    checkHelperVersion(getClass().getClassLoader(), "UnsafeAtomicHelper");
    checkHelperVersion(NO_UNSAFE, "SafeAtomicHelper");
    checkHelperVersion(NO_ATOMIC_REFERENCE_FIELD_UPDATER, "SynchronizedHelper");

    // Run the corresponding AbstractFutureTest test method in a new classloader that disallows
    // certain core jdk classes.
    ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
    Thread.currentThread().setContextClassLoader(NO_UNSAFE);
    try {
      runTestMethod(NO_UNSAFE);
    } finally {
      Thread.currentThread().setContextClassLoader(oldClassLoader);
    }

    Thread.currentThread().setContextClassLoader(NO_ATOMIC_REFERENCE_FIELD_UPDATER);
    try {
      runTestMethod(NO_ATOMIC_REFERENCE_FIELD_UPDATER);
      // TODO(lukes): assert that the logs are full of errors
    } finally {
      Thread.currentThread().setContextClassLoader(oldClassLoader);
    }
  }

  private void runTestMethod(ClassLoader classLoader) throws Exception {
    Class<?> test = classLoader.loadClass(AbstractFutureTest.class.getName());
    test.getMethod(getName()).invoke(test.newInstance());
  }

  private void checkHelperVersion(ClassLoader classLoader, String expectedHelperClassName)
      throws Exception {
    // Make sure we are actually running with the expected helper implementation
    Class<?> abstractFutureClass = classLoader.loadClass(AbstractFuture.class.getName());
    Field helperField = abstractFutureClass.getDeclaredField("ATOMIC_HELPER");
    helperField.setAccessible(true);
    assertEquals(expectedHelperClassName, helperField.get(null).getClass().getSimpleName());
  }

  private static ClassLoader getClassLoader(final Set<String> disallowedClassNames) {
    final String concurrentPackage = SettableFuture.class.getPackage().getName();
    ClassLoader classLoader = AbstractFutureFallbackAtomicHelperTest.class.getClassLoader();
    // we delegate to the current classloader so both loaders agree on classes like TestCase
    return new URLClassLoader(ClassPathUtil.getClassPathUrls(), classLoader) {
      @Override
      public Class<?> loadClass(String name) throws ClassNotFoundException {
        if (disallowedClassNames.contains(name)) {
          throw new ClassNotFoundException("I'm sorry Dave, I'm afraid I can't do that.");
        }
        if (name.startsWith(concurrentPackage)) {
          Class<?> c = findLoadedClass(name);
          if (c == null) {
            return super.findClass(name);
          }
          return c;
        }
        return super.loadClass(name);
      }
    };
  }
}
