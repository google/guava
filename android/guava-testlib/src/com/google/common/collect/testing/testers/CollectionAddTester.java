/*
 * Copyright (C) 2007 The Guava Authors
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

package com.google.common.collect.testing.testers;

import static com.google.common.collect.testing.Helpers.getMethod;
import static com.google.common.collect.testing.features.CollectionFeature.ALLOWS_NULL_VALUES;
import static com.google.common.collect.testing.features.CollectionFeature.FAILS_FAST_ON_CONCURRENT_MODIFICATION;
import static com.google.common.collect.testing.features.CollectionFeature.RESTRICTS_ELEMENTS;
import static com.google.common.collect.testing.features.CollectionFeature.SUPPORTS_ADD;
import static com.google.common.collect.testing.features.CollectionSize.ZERO;
import static com.google.common.collect.testing.testers.ReflectionFreeAssertThrows.assertThrows;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.annotations.J2ktIncompatible;
import com.google.common.collect.testing.AbstractCollectionTester;
import com.google.common.collect.testing.features.CollectionFeature;
import com.google.common.collect.testing.features.CollectionSize;
import java.lang.reflect.Method;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import org.junit.Ignore;

/**
 * A generic JUnit test which tests {@code add} operations on a collection. Can't be invoked
 * directly; please see {@link com.google.common.collect.testing.CollectionTestSuiteBuilder}.
 *
 * @author Chris Povirk
 * @author Kevin Bourrillion
 */
@GwtCompatible(emulated = true)
@Ignore("test runners must not instantiate and run this directly, only via suites we build")
// @Ignore affects the Android test runner, which respects JUnit 4 annotations on JUnit 3 tests.
@SuppressWarnings("JUnit4ClassUsedInJUnit3")
public class CollectionAddTester<E> extends AbstractCollectionTester<E> {
  @CollectionFeature.Require(SUPPORTS_ADD)
  public void testAdd_supportedNotPresent() {
    assertTrue("add(notPresent) should return true", collection.add(e3()));
    expectAdded(e3());
  }

  @CollectionFeature.Require(absent = SUPPORTS_ADD)
  public void testAdd_unsupportedNotPresent() {
    assertThrows(UnsupportedOperationException.class, () -> collection.add(e3()));
    expectUnchanged();
    expectMissing(e3());
  }

  @CollectionFeature.Require(absent = SUPPORTS_ADD)
  @CollectionSize.Require(absent = ZERO)
  public void testAdd_unsupportedPresent() {
    try {
      assertFalse("add(present) should return false or throw", collection.add(e0()));
    } catch (UnsupportedOperationException tolerated) {
    }
    expectUnchanged();
  }

  @CollectionFeature.Require(
      value = {SUPPORTS_ADD, ALLOWS_NULL_VALUES},
      absent = RESTRICTS_ELEMENTS)
  public void testAdd_nullSupported() {
    assertTrue("add(null) should return true", collection.add(null));
    expectAdded((E) null);
  }

  @CollectionFeature.Require(value = SUPPORTS_ADD, absent = ALLOWS_NULL_VALUES)
  public void testAdd_nullUnsupported() {
    assertThrows(NullPointerException.class, () -> collection.add(null));
    expectUnchanged();
    expectNullMissingWhenNullUnsupported("Should not contain null after unsupported add(null)");
  }

  @CollectionFeature.Require({SUPPORTS_ADD, FAILS_FAST_ON_CONCURRENT_MODIFICATION})
  @CollectionSize.Require(absent = ZERO)
  public void testAddConcurrentWithIteration() {
    assertThrows(
        ConcurrentModificationException.class,
        () -> {
          Iterator<E> iterator = collection.iterator();
          assertTrue(collection.add(e3()));
          iterator.next();
        });
  }

  /**
   * Returns the {@link Method} instance for {@link #testAdd_nullSupported()} so that tests of
   * {@link java.util.Collections#checkedCollection(java.util.Collection, Class)} can suppress it
   * with {@code FeatureSpecificTestSuiteBuilder.suppressing()} until <a
   * href="https://bugs.openjdk.org/browse/JDK-6409434">JDK-6409434</a> is fixed. It's unclear
   * whether nulls were to be permitted or forbidden, but presumably the eventual fix will be to
   * permit them, as it seems more likely that code would depend on that behavior than on the other.
   * Thus, we say the bug is in add(), which fails to support null.
   */
  @J2ktIncompatible
  @GwtIncompatible // reflection
  public static Method getAddNullSupportedMethod() {
    return getMethod(CollectionAddTester.class, "testAdd_nullSupported");
  }

  /**
   * Returns the {@link Method} instance for {@link #testAdd_nullSupported()} so that tests of
   * {@link java.util.TreeSet} can suppress it with {@code
   * FeatureSpecificTestSuiteBuilder.suppressing()} until <a
   * href="https://bugs.openjdk.org/browse/JDK-5045147">JDK-5045147</a> is fixed.
   */
  @J2ktIncompatible
  @GwtIncompatible // reflection
  public static Method getAddNullUnsupportedMethod() {
    return getMethod(CollectionAddTester.class, "testAdd_nullUnsupported");
  }

  /**
   * Returns the {@link Method} instance for {@link #testAdd_unsupportedNotPresent()} so that tests
   * can suppress it with {@code FeatureSpecificTestSuiteBuilder.suppressing()} while we figure out
   * what to do with <a
   * href="https://github.com/openjdk/jdk/blob/c25c4896ad9ef031e3cddec493aef66ff87c48a7/src/java.base/share/classes/java/util/concurrent/ConcurrentHashMap.java#L4830">{@code
   * ConcurrentHashMap} support for {@code entrySet().add()}</a>.
   */
  @J2ktIncompatible
  @GwtIncompatible // reflection
  public static Method getAddUnsupportedNotPresentMethod() {
    return getMethod(CollectionAddTester.class, "testAdd_unsupportedNotPresent");
  }
}
