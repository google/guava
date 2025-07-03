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

import static com.google.common.base.StandardSystemProperty.JAVA_SPECIFICATION_VERSION;
import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.ImmutableSet;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URLClassLoader;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.jspecify.annotations.NullUnmarked;

/**
 * Tests our AtomicHelper fallback strategies in AbstractFuture.
 *
 * <p>On different platforms AbstractFuture uses different strategies for its core synchronization
 * primitives. The strategies are all implemented as subtypes of AtomicHelper and the strategy is
 * selected in the static initializer of AbstractFuture. This is convenient and performant but
 * introduces some testing difficulties. This test exercises the fallback strategies.
 *
 * <p>To force selection of our fallback strategies, we load {@link AbstractFuture} (and all of
 * {@code com.google.common.util.concurrent}) in degenerate class loaders which make certain
 * platform classes unavailable. Then we construct a test suite so we can run the normal
 * AbstractFutureTest test methods in these degenerate classloaders.
 */

@NullUnmarked
public class AbstractFutureFallbackAtomicHelperTest extends TestCase {

  // stash these in static fields to avoid loading them over and over again (speeds up test
  // execution significantly)

  /**
   * This classloader disallows {@link java.lang.invoke.VarHandle}, which will prevent us from
   * selecting the {@code VarHandleAtomicHelper} strategy.
   */
  private static final ClassLoader NO_VAR_HANDLE =
      getClassLoader(ImmutableSet.of("java.lang.invoke.VarHandle"));

  /**
   * This classloader disallows {@link java.lang.invoke.VarHandle} and {@link sun.misc.Unsafe},
   * which will prevent us from selecting the {@code UnsafeAtomicHelper} strategy.
   */
  private static final ClassLoader NO_UNSAFE =
      getClassLoader(ImmutableSet.of("java.lang.invoke.VarHandle", "sun.misc.Unsafe"));

  /**
   * This classloader disallows {@link java.lang.invoke.VarHandle}, {@link sun.misc.Unsafe} and
   * {@link AtomicReferenceFieldUpdater}, which will prevent us from selecting the {@code
   * AtomicReferenceFieldUpdaterAtomicHelper} strategy.
   */
  private static final ClassLoader NO_ATOMIC_REFERENCE_FIELD_UPDATER =
      getClassLoader(
          ImmutableSet.of(
              "java.lang.invoke.VarHandle",
              "sun.misc.Unsafe",
              AtomicReferenceFieldUpdater.class.getName()));

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
    /*
     * Note that we do not run this test under Android at the moment. For Android testing, see
     * AbstractFutureDefaultAtomicHelperTest.
     */

    // First, ensure that our classloaders are initializing the correct helper versions:

    if (isJava8()) {
      checkHelperVersion(getClass().getClassLoader(), "UnsafeAtomicHelper");
    } else {
      checkHelperVersion(getClass().getClassLoader(), "VarHandleAtomicHelper");
    }
    checkHelperVersion(NO_VAR_HANDLE, "UnsafeAtomicHelper");
    checkHelperVersion(NO_UNSAFE, "AtomicReferenceFieldUpdaterAtomicHelper");
    checkHelperVersion(NO_ATOMIC_REFERENCE_FIELD_UPDATER, "SynchronizedHelper");

    // Then, run the actual tests under each alternative classloader:

    /*
     * Under Java 8, there is no need to test the no-VarHandle case here: It's already tested by the
     * main AbstractFutureTest, which uses the default AtomicHelper, which we verified above to be
     * UnsafeAtomicHelper.
     */
    if (!isJava8()) {
      runTestMethod(NO_VAR_HANDLE);
    }

    runTestMethod(NO_UNSAFE);

    runTestMethod(NO_ATOMIC_REFERENCE_FIELD_UPDATER);
    // TODO(lukes): assert that the logs are full of errors
  }

  /**
   * Runs the corresponding {@link AbstractFutureTest} test method in a new classloader that
   * disallows certain core JDK classes.
   */
  private void runTestMethod(ClassLoader classLoader) throws Exception {
    ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();

    Thread.currentThread().setContextClassLoader(classLoader);
    try {
      Class<?> test = classLoader.loadClass(AbstractFutureTest.class.getName());
      test.getMethod(getName()).invoke(test.getDeclaredConstructor().newInstance());
    } finally {
      Thread.currentThread().setContextClassLoader(oldClassLoader);
    }
  }

  private void checkHelperVersion(ClassLoader classLoader, String expectedHelperClassName)
      throws Exception {
    // Make sure we are actually running with the expected helper implementation
    Class<?> abstractFutureStateClass = classLoader.loadClass(AbstractFutureState.class.getName());
    Method helperMethod = abstractFutureStateClass.getDeclaredMethod("atomicHelperTypeForTest");
    helperMethod.setAccessible(true);
    assertThat(helperMethod.invoke(null)).isEqualTo(expectedHelperClassName);
  }

  private static ClassLoader getClassLoader(Set<String> disallowedClassNames) {
    String concurrentPackage = SettableFuture.class.getPackage().getName();
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

  private static boolean isJava8() {
    return JAVA_SPECIFICATION_VERSION.value().equals("1.8");
  }
}
