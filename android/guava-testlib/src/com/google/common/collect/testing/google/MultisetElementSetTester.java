/*
 * Copyright (C) 2013 The Guava Authors
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

package com.google.common.collect.testing.google;

import static com.google.common.collect.testing.Helpers.assertEmpty;
import static com.google.common.collect.testing.Helpers.getMethod;
import static com.google.common.collect.testing.features.CollectionFeature.SUPPORTS_ADD;
import static com.google.common.collect.testing.features.CollectionFeature.SUPPORTS_REMOVE;
import static com.google.common.collect.testing.features.CollectionSize.SEVERAL;
import static com.google.common.collect.testing.features.CollectionSize.ZERO;
import static java.util.Arrays.asList;
import static java.util.Collections.singleton;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.annotations.J2ktIncompatible;
import com.google.common.collect.testing.features.CollectionFeature;
import com.google.common.collect.testing.features.CollectionSize;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;
import org.junit.Ignore;

/**
 * Tests for {@code Multiset.elementSet()} not covered by the derived {@code SetTestSuiteBuilder}.
 *
 * @author Louis Wasserman
 */
@GwtCompatible
@Ignore("test runners must not instantiate and run this directly, only via suites we build")
// @Ignore affects the Android test runner, which respects JUnit 4 annotations on JUnit 3 tests.
@SuppressWarnings("JUnit4ClassUsedInJUnit3")
public class MultisetElementSetTester<E> extends AbstractMultisetTester<E> {
  @CollectionFeature.Require(SUPPORTS_ADD)
  public void testElementSetReflectsAddAbsent() {
    Set<E> elementSet = getMultiset().elementSet();
    assertFalse(elementSet.contains(e3()));
    getMultiset().add(e3(), 4);
    assertTrue(elementSet.contains(e3()));
  }

  @CollectionSize.Require(absent = ZERO)
  @CollectionFeature.Require(SUPPORTS_REMOVE)
  public void testElementSetReflectsRemove() {
    Set<E> elementSet = getMultiset().elementSet();
    assertTrue(elementSet.contains(e0()));
    getMultiset().removeAll(singleton(e0()));
    assertFalse(elementSet.contains(e0()));
  }

  @CollectionSize.Require(absent = ZERO)
  @CollectionFeature.Require(SUPPORTS_REMOVE)
  public void testElementSetRemovePropagatesToMultiset() {
    Set<E> elementSet = getMultiset().elementSet();
    int size = getNumElements();
    int expectedSize = size - getMultiset().count(e0());
    assertTrue(elementSet.remove(e0()));
    assertFalse(getMultiset().contains(e0()));
    assertEquals(expectedSize, getMultiset().size());
  }

  @CollectionSize.Require(SEVERAL)
  @CollectionFeature.Require(SUPPORTS_REMOVE)
  public void testElementSetRemoveDuplicatePropagatesToMultiset() {
    initThreeCopies();
    int size = getNumElements();
    int expectedSize = size - getMultiset().count(e0());
    Set<E> elementSet = getMultiset().elementSet();
    assertTrue(elementSet.remove(e0()));
    assertEmpty(getMultiset());
    assertEquals(expectedSize, getMultiset().size());
  }

  @CollectionFeature.Require(SUPPORTS_REMOVE)
  public void testElementSetRemoveAbsent() {
    Set<E> elementSet = getMultiset().elementSet();
    assertFalse(elementSet.remove(e3()));
    expectUnchanged();
  }

  @CollectionFeature.Require(SUPPORTS_REMOVE)
  public void testElementSetClear() {
    getMultiset().elementSet().clear();
    assertEmpty(getMultiset());
  }

  /**
   * Returns {@link Method} instances for the read tests that assume multisets support duplicates so
   * that the test of {@code Multisets.forSet()} can suppress them.
   */
  @J2ktIncompatible
  @GwtIncompatible // reflection
  public static List<Method> getElementSetDuplicateInitializingMethods() {
    return asList(
        getMethod(
            MultisetElementSetTester.class, "testElementSetRemoveDuplicatePropagatesToMultiset"));
  }
}
