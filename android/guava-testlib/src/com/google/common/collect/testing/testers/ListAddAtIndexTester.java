/*
 * Copyright (C) 2008 The Guava Authors
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
import static com.google.common.collect.testing.features.CollectionSize.ONE;
import static com.google.common.collect.testing.features.CollectionSize.ZERO;
import static com.google.common.collect.testing.features.ListFeature.SUPPORTS_ADD_WITH_INDEX;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.collect.testing.Helpers;
import com.google.common.collect.testing.features.CollectionFeature;
import com.google.common.collect.testing.features.CollectionSize;
import com.google.common.collect.testing.features.ListFeature;
import java.lang.reflect.Method;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import org.junit.Ignore;

/**
 * A generic JUnit test which tests {@code add(int, Object)} operations on a list. Can't be invoked
 * directly; please see {@link com.google.common.collect.testing.ListTestSuiteBuilder}.
 *
 * @author Chris Povirk
 */
@SuppressWarnings("unchecked") // too many "unchecked generic array creations"
@GwtCompatible(emulated = true)
@Ignore // Affects only Android test runner, which respects JUnit 4 annotations on JUnit 3 tests.
public class ListAddAtIndexTester<E> extends AbstractListTester<E> {
  @ListFeature.Require(SUPPORTS_ADD_WITH_INDEX)
  @CollectionSize.Require(absent = ZERO)
  public void testAddAtIndex_supportedPresent() {
    getList().add(0, e0());
    expectAdded(0, e0());
  }

  @ListFeature.Require(absent = SUPPORTS_ADD_WITH_INDEX)
  @CollectionSize.Require(absent = ZERO)
  /*
   * absent = ZERO isn't required, since unmodList.add() must
   * throw regardless, but it keeps the method name accurate.
   */
  public void testAddAtIndex_unsupportedPresent() {
    try {
      getList().add(0, e0());
      fail("add(n, present) should throw");
    } catch (UnsupportedOperationException expected) {
    }
    expectUnchanged();
  }

  @ListFeature.Require(SUPPORTS_ADD_WITH_INDEX)
  public void testAddAtIndex_supportedNotPresent() {
    getList().add(0, e3());
    expectAdded(0, e3());
  }

  @CollectionFeature.Require(FAILS_FAST_ON_CONCURRENT_MODIFICATION)
  @ListFeature.Require(SUPPORTS_ADD_WITH_INDEX)
  public void testAddAtIndexConcurrentWithIteration() {
    try {
      Iterator<E> iterator = collection.iterator();
      getList().add(0, e3());
      iterator.next();
      fail("Expected ConcurrentModificationException");
    } catch (ConcurrentModificationException expected) {
      // success
    }
  }

  @ListFeature.Require(absent = SUPPORTS_ADD_WITH_INDEX)
  public void testAddAtIndex_unsupportedNotPresent() {
    try {
      getList().add(0, e3());
      fail("add(n, notPresent) should throw");
    } catch (UnsupportedOperationException expected) {
    }
    expectUnchanged();
    expectMissing(e3());
  }

  @ListFeature.Require(SUPPORTS_ADD_WITH_INDEX)
  @CollectionSize.Require(absent = {ZERO, ONE})
  public void testAddAtIndex_middle() {
    getList().add(getNumElements() / 2, e3());
    expectAdded(getNumElements() / 2, e3());
  }

  @ListFeature.Require(SUPPORTS_ADD_WITH_INDEX)
  @CollectionSize.Require(absent = ZERO)
  public void testAddAtIndex_end() {
    getList().add(getNumElements(), e3());
    expectAdded(getNumElements(), e3());
  }

  @ListFeature.Require(SUPPORTS_ADD_WITH_INDEX)
  @CollectionFeature.Require(ALLOWS_NULL_VALUES)
  public void testAddAtIndex_nullSupported() {
    getList().add(0, null);
    expectAdded(0, (E) null);
  }

  @ListFeature.Require(SUPPORTS_ADD_WITH_INDEX)
  @CollectionFeature.Require(absent = ALLOWS_NULL_VALUES)
  public void testAddAtIndex_nullUnsupported() {
    try {
      getList().add(0, null);
      fail("add(n, null) should throw");
    } catch (NullPointerException expected) {
    }
    expectUnchanged();
    expectNullMissingWhenNullUnsupported("Should not contain null after unsupported add(n, null)");
  }

  @ListFeature.Require(SUPPORTS_ADD_WITH_INDEX)
  public void testAddAtIndex_negative() {
    try {
      getList().add(-1, e3());
      fail("add(-1, e) should throw");
    } catch (IndexOutOfBoundsException expected) {
    }
    expectUnchanged();
    expectMissing(e3());
  }

  @ListFeature.Require(SUPPORTS_ADD_WITH_INDEX)
  public void testAddAtIndex_tooLarge() {
    try {
      getList().add(getNumElements() + 1, e3());
      fail("add(size + 1, e) should throw");
    } catch (IndexOutOfBoundsException expected) {
    }
    expectUnchanged();
    expectMissing(e3());
  }

  /**
   * Returns the {@link Method} instance for {@link #testAddAtIndex_nullSupported()} so that tests
   * can suppress it. See {@link CollectionAddTester#getAddNullSupportedMethod()} for details.
   */
  @GwtIncompatible // reflection
  public static Method getAddNullSupportedMethod() {
    return Helpers.getMethod(ListAddAtIndexTester.class, "testAddAtIndex_nullSupported");
  }
}
