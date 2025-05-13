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
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.jspecify.annotations.NullUnmarked;

/**
 * Tests our AtomicHelper fallback strategy in AggregateFutureState.
 *
 * <p>On different platforms AggregateFutureState uses different strategies for its core
 * synchronization primitives. The strategies are all implemented as subtypes of AtomicHelper and
 * the strategy is selected in the static initializer of AggregateFutureState. This is convenient
 * and performant but introduces some testing difficulties. This test exercises the two fallback
 * strategies.
 *
 * <ul>
 *   <li>SafeAtomicHelper: uses Atomic FieldsUpdaters to implement synchronization
 *   <li>SynchronizedHelper: uses {@code synchronized} blocks for synchronization
 * </ul>
 *
 * To force selection of our fallback strategies we load {@link AggregateFutureState} (and all of
 * {@code com.google.common.util.concurrent}) in degenerate class loaders which make certain
 * platform classes unavailable. Then we construct a test suite so we can run the normal FuturesTest
 * test methods in these degenerate classloaders.
 */

@NullUnmarked
public class AggregateFutureStateFallbackAtomicHelperTest extends TestCase {

  /**
   * This classloader disallows {@code AtomicReferenceFieldUpdater} and {@code
   * AtomicIntegerFieldUpdater}, which will prevent us from selecting the {@code SafeAtomicHelper}
   * strategy.
   *
   * <p>Stashing this in a static field avoids loading it over and over again and speeds up test
   * execution significantly.
   */
  private static final ClassLoader NO_ATOMIC_FIELD_UPDATER =
      getClassLoader(
          ImmutableSet.of(
              AtomicIntegerFieldUpdater.class.getName(),
              AtomicReferenceFieldUpdater.class.getName()));

  public static TestSuite suite() {
    // we create a test suite containing a test for every FuturesTest test method and we
    // set it as the name of the test.  Then in runTest we can reflectively load and invoke the
    // corresponding method on FuturesTest in the correct classloader.
    TestSuite suite = new TestSuite(AggregateFutureStateFallbackAtomicHelperTest.class.getName());
    for (Method method : FuturesTest.class.getDeclaredMethods()) {
      if (Modifier.isPublic(method.getModifiers())
          && method.getName().startsWith("test")
          /*
           * When we block access to AtomicReferenceFieldUpdater, we can't even reflect on
           * AbstractFuture, since it declares methods that use that type in their signatures.
           */
          && !method.getName().equals("testFutures_nullChecks")) {
        suite.addTest(
            TestSuite.createTest(
                AggregateFutureStateFallbackAtomicHelperTest.class, method.getName()));
      }
    }
    return suite;
  }

  @Override
  public void runTest() throws Exception {
    /*
     * Note that we do not run this test under Android at the moment. For Android testing, see
     * AggregateFutureStateDefaultAtomicHelperTest.
     */

    // First, ensure that our classloaders are initializing the correct helper versions:

    checkHelperVersion(getClass().getClassLoader(), "SafeAtomicHelper");
    checkHelperVersion(NO_ATOMIC_FIELD_UPDATER, "SynchronizedAtomicHelper");

    // Then, run the actual tests under each alternative classloader:

    runTestMethod(NO_ATOMIC_FIELD_UPDATER);
    // TODO(lukes): assert that the logs are full of errors
  }

  /**
   * Runs the corresponding {@link FuturesTest} test method in a new classloader that disallows
   * certain core JDK classes.
   */
  private void runTestMethod(ClassLoader classLoader) throws Exception {
    ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
    Thread.currentThread().setContextClassLoader(classLoader);
    try {
      Class<?> test = classLoader.loadClass(FuturesTest.class.getName());
      Object testInstance = test.getDeclaredConstructor().newInstance();
      test.getMethod("setUp").invoke(testInstance);
      test.getMethod(getName()).invoke(testInstance);
      test.getMethod("tearDown").invoke(testInstance);
    } finally {
      Thread.currentThread().setContextClassLoader(oldClassLoader);
    }
  }

  private void checkHelperVersion(ClassLoader classLoader, String expectedHelperClassName)
      throws Exception {
    // Make sure we are actually running with the expected helper implementation
    Class<?> aggregateFutureStateClass =
        classLoader.loadClass(AggregateFutureState.class.getName());
    Field helperField = aggregateFutureStateClass.getDeclaredField("ATOMIC_HELPER");
    helperField.setAccessible(true);
    assertEquals(expectedHelperClassName, helperField.get(null).getClass().getSimpleName());
  }

  private static ClassLoader getClassLoader(Set<String> blocklist) {
    String concurrentPackage = SettableFuture.class.getPackage().getName();
    ClassLoader classLoader = AggregateFutureStateFallbackAtomicHelperTest.class.getClassLoader();
    // we delegate to the current classloader so both loaders agree on classes like TestCase
    return new URLClassLoader(ClassPathUtil.getClassPathUrls(), classLoader) {
      @Override
      public Class<?> loadClass(String name) throws ClassNotFoundException {
        if (blocklist.contains(name)) {
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
