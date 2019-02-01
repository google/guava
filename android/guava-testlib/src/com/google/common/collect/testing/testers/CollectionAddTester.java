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

import static com.google.common.collect.testing.features.CollectionFeature.ALLOWS_NULL_VALUES;
import static com.google.common.collect.testing.features.CollectionFeature.FAILS_FAST_ON_CONCURRENT_MODIFICATION;
import static com.google.common.collect.testing.features.CollectionFeature.RESTRICTS_ELEMENTS;
import static com.google.common.collect.testing.features.CollectionFeature.SUPPORTS_ADD;
import static com.google.common.collect.testing.features.CollectionSize.ZERO;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.collect.testing.AbstractCollectionTester;
import com.google.common.collect.testing.Helpers;
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
@SuppressWarnings("unchecked") // too many "unchecked generic array creations"
@GwtCompatible(emulated = true)
@Ignore // Affects only Android test runner, which respects JUnit 4 annotations on JUnit 3 tests.
public class CollectionAddTester<E> extends AbstractCollectionTester<E> {
  @CollectionFeature.Require(SUPPORTS_ADD)
  public void testAdd_supportedNotPresent() {
    assertTrue("add(notPresent) should return true", collection.add(e3()));
    expectAdded(e3());
  }

  @CollectionFeature.Require(absent = SUPPORTS_ADD)
  public void testAdd_unsupportedNotPresent() {
    try {
      collection.add(e3());
      fail("add(notPresent) should throw");
    } catch (UnsupportedOperationException expected) {
    }
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
    try {
      collection.add(null);
      fail("add(null) should throw");
    } catch (NullPointerException expected) {
    }
    expectUnchanged();
    expectNullMissingWhenNullUnsupported("Should not contain null after unsupported add(null)");
  }

  @CollectionFeature.Require({SUPPORTS_ADD, FAILS_FAST_ON_CONCURRENT_MODIFICATION})
  @CollectionSize.Require(absent = ZERO)
  public void testAddConcurrentWithIteration() {
    try {
      Iterator<E> iterator = collection.iterator();
      assertTrue(collection.add(e3()));
      iterator.next();
      fail("Expected ConcurrentModificationException");
    } catch (ConcurrentModificationException expected) {
      // success
    }
  }

  /**
   * Returns the {@link Method} instance for {@link #testAdd_nullSupported()} so that tests of
   * {@link java.util.Collections#checkedCollection(java.util.Collection, Class)} can suppress it
   * with {@code FeatureSpecificTestSuiteBuilder.suppressing()} until <a
   * href="http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6409434">Sun bug 6409434</a> is fixed.
   * It's unclear whether nulls were to be permitted or forbidden, but presumably the eventual fix
   * will be to permit them, as it seems more likely that code would depend on that behavior than on
   * the other. Thus, we say the bug is in add(), which fails to support null.
   */
  @GwtIncompatible // reflection
  public static Method getAddNullSupportedMethod() {
    return Helpers.getMethod(CollectionAddTester.class, "testAdd_nullSupported");
  }

  /**
   * Returns the {@link Method} instance for {@link #testAdd_nullSupported()} so that tests of
   * {@link java.util.TreeSet} can suppress it with {@code
   * FeatureSpecificTestSuiteBuilder.suppressing()} until <a
   * href="http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=5045147">Sun bug 5045147</a> is fixed.
   */
  @GwtIncompatible // reflection
  public static Method getAddNullUnsupportedMethod() {
    return Helpers.getMethod(CollectionAddTester.class, "testAdd_nullUnsupported");
  }

  /**
   * Returns the {@link Method} instance for {@link #testAdd_unsupportedNotPresent()} so that tests
   * can suppress it with {@code FeatureSpecificTestSuiteBuilder.suppressing()} while we figure out
   * what to do with <a href="http://goo.gl/qJBruX">{@code ConcurrentHashMap} support for {@code
   * entrySet().add()}</a>.
   */
  @GwtIncompatible // reflection
  public static Method getAddUnsupportedNotPresentMethod() {
    return Helpers.getMethod(CollectionAddTester.class, "testAdd_unsupportedNotPresent");
  }
}
