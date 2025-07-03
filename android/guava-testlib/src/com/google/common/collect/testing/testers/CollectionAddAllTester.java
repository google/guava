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
import static java.util.Collections.singletonList;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.annotations.J2ktIncompatible;
import com.google.common.collect.testing.AbstractCollectionTester;
import com.google.common.collect.testing.MinimalCollection;
import com.google.common.collect.testing.features.CollectionFeature;
import com.google.common.collect.testing.features.CollectionSize;
import java.lang.reflect.Method;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.junit.Ignore;

/**
 * A generic JUnit test which tests addAll operations on a collection. Can't be invoked directly;
 * please see {@link com.google.common.collect.testing.CollectionTestSuiteBuilder}.
 *
 * @author Chris Povirk
 * @author Kevin Bourrillion
 */
@GwtCompatible(emulated = true)
@Ignore("test runners must not instantiate and run this directly, only via suites we build")
// @Ignore affects the Android test runner, which respects JUnit 4 annotations on JUnit 3 tests.
@SuppressWarnings("JUnit4ClassUsedInJUnit3")
public class CollectionAddAllTester<E extends @Nullable Object>
    extends AbstractCollectionTester<E> {
  @CollectionFeature.Require(SUPPORTS_ADD)
  public void testAddAll_supportedNothing() {
    assertFalse("addAll(nothing) should return false", collection.addAll(emptyCollection()));
    expectUnchanged();
  }

  @CollectionFeature.Require(absent = SUPPORTS_ADD)
  public void testAddAll_unsupportedNothing() {
    try {
      assertFalse(
          "addAll(nothing) should return false or throw", collection.addAll(emptyCollection()));
    } catch (UnsupportedOperationException tolerated) {
    }
    expectUnchanged();
  }

  @CollectionFeature.Require(SUPPORTS_ADD)
  public void testAddAll_supportedNonePresent() {
    assertTrue(
        "addAll(nonePresent) should return true", collection.addAll(createDisjointCollection()));
    expectAdded(e3(), e4());
  }

  @CollectionFeature.Require(absent = SUPPORTS_ADD)
  public void testAddAll_unsupportedNonePresent() {
    assertThrows(
        UnsupportedOperationException.class, () -> collection.addAll(createDisjointCollection()));
    expectUnchanged();
    expectMissing(e3(), e4());
  }

  @CollectionFeature.Require(SUPPORTS_ADD)
  @CollectionSize.Require(absent = ZERO)
  public void testAddAll_supportedSomePresent() {
    assertTrue(
        "addAll(somePresent) should return true",
        collection.addAll(MinimalCollection.of(e3(), e0())));
    assertTrue("should contain " + e3(), collection.contains(e3()));
    assertTrue("should contain " + e0(), collection.contains(e0()));
  }

  @CollectionFeature.Require(absent = SUPPORTS_ADD)
  @CollectionSize.Require(absent = ZERO)
  public void testAddAll_unsupportedSomePresent() {
    assertThrows(
        UnsupportedOperationException.class,
        () -> collection.addAll(MinimalCollection.of(e3(), e0())));
    expectUnchanged();
  }

  @CollectionFeature.Require({SUPPORTS_ADD, FAILS_FAST_ON_CONCURRENT_MODIFICATION})
  @CollectionSize.Require(absent = ZERO)
  public void testAddAllConcurrentWithIteration() {
    assertThrows(
        ConcurrentModificationException.class,
        () -> {
          Iterator<E> iterator = collection.iterator();
          assertTrue(collection.addAll(MinimalCollection.of(e3(), e0())));
          iterator.next();
        });
  }

  @CollectionFeature.Require(absent = SUPPORTS_ADD)
  @CollectionSize.Require(absent = ZERO)
  public void testAddAll_unsupportedAllPresent() {
    try {
      assertFalse(
          "addAll(allPresent) should return false or throw",
          collection.addAll(MinimalCollection.of(e0())));
    } catch (UnsupportedOperationException tolerated) {
    }
    expectUnchanged();
  }

  @CollectionFeature.Require(
      value = {SUPPORTS_ADD, ALLOWS_NULL_VALUES},
      absent = RESTRICTS_ELEMENTS)
  public void testAddAll_nullSupported() {
    List<E> containsNull = singletonList(null);
    assertTrue("addAll(containsNull) should return true", collection.addAll(containsNull));
    /*
     * We need (E) to force interpretation of null as the single element of a
     * varargs array, not the array itself
     */
    expectAdded((E) null);
  }

  @CollectionFeature.Require(value = SUPPORTS_ADD, absent = ALLOWS_NULL_VALUES)
  public void testAddAll_nullUnsupported() {
    List<E> containsNull = singletonList(null);
    assertThrows(NullPointerException.class, () -> collection.addAll(containsNull));
    expectUnchanged();
    expectNullMissingWhenNullUnsupported(
        "Should not contain null after unsupported addAll(containsNull)");
  }

  @CollectionFeature.Require(SUPPORTS_ADD)
  public void testAddAll_nullCollectionReference() {
    assertThrows(NullPointerException.class, () -> collection.addAll(null));
  }

  /**
   * Returns the {@link Method} instance for {@link #testAddAll_nullUnsupported()} so that tests can
   * suppress it with {@code FeatureSpecificTestSuiteBuilder.suppressing()} until <a
   * href="https://bugs.openjdk.org/browse/JDK-5045147">JDK-5045147</a> is fixed.
   */
  @J2ktIncompatible
  @GwtIncompatible // reflection
  public static Method getAddAllNullUnsupportedMethod() {
    return getMethod(CollectionAddAllTester.class, "testAddAll_nullUnsupported");
  }

  /**
   * Returns the {@link Method} instance for {@link #testAddAll_unsupportedNonePresent()} so that
   * tests can suppress it with {@code FeatureSpecificTestSuiteBuilder.suppressing()} while we
   * figure out what to do with <a
   * href="https://github.com/openjdk/jdk/blob/c25c4896ad9ef031e3cddec493aef66ff87c48a7/src/java.base/share/classes/java/util/concurrent/ConcurrentHashMap.java#L4830">{@code
   * ConcurrentHashMap} support for {@code entrySet().add()}</a>.
   */
  @J2ktIncompatible
  @GwtIncompatible // reflection
  public static Method getAddAllUnsupportedNonePresentMethod() {
    return getMethod(CollectionAddAllTester.class, "testAddAll_unsupportedNonePresent");
  }

  /**
   * Returns the {@link Method} instance for {@link #testAddAll_unsupportedSomePresent()} so that
   * tests can suppress it with {@code FeatureSpecificTestSuiteBuilder.suppressing()} while we
   * figure out what to do with <a
   * href="https://github.com/openjdk/jdk/blob/c25c4896ad9ef031e3cddec493aef66ff87c48a7/src/java.base/share/classes/java/util/concurrent/ConcurrentHashMap.java#L4830">{@code
   * ConcurrentHashMap} support for {@code entrySet().add()}</a>.
   */
  @J2ktIncompatible
  @GwtIncompatible // reflection
  public static Method getAddAllUnsupportedSomePresentMethod() {
    return getMethod(CollectionAddAllTester.class, "testAddAll_unsupportedSomePresent");
  }
}
