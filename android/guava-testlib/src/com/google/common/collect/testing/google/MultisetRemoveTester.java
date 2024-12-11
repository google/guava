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
import static com.google.common.collect.testing.Helpers.copyToList;
import static com.google.common.collect.testing.Helpers.getMethod;
import static com.google.common.collect.testing.features.CollectionFeature.ALLOWS_NULL_QUERIES;
import static com.google.common.collect.testing.features.CollectionFeature.ALLOWS_NULL_VALUES;
import static com.google.common.collect.testing.features.CollectionFeature.SUPPORTS_REMOVE;
import static com.google.common.collect.testing.features.CollectionSize.SEVERAL;
import static com.google.common.collect.testing.features.CollectionSize.ZERO;
import static com.google.common.collect.testing.google.ReflectionFreeAssertThrows.assertThrows;
import static java.util.Arrays.asList;
import static java.util.Collections.singleton;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.annotations.J2ktIncompatible;
import com.google.common.collect.testing.WrongType;
import com.google.common.collect.testing.features.CollectionFeature;
import com.google.common.collect.testing.features.CollectionSize;
import java.lang.reflect.Method;
import java.util.List;
import org.junit.Ignore;

/**
 * Tests for {@code Multiset#remove}, {@code Multiset.removeAll}, and {@code Multiset.retainAll} not
 * already covered by the corresponding Collection testers.
 *
 * @author Jared Levy
 */
@GwtCompatible(emulated = true)
@Ignore("test runners must not instantiate and run this directly, only via suites we build")
// @Ignore affects the Android test runner, which respects JUnit 4 annotations on JUnit 3 tests.
@SuppressWarnings("JUnit4ClassUsedInJUnit3")
public class MultisetRemoveTester<E> extends AbstractMultisetTester<E> {
  @CollectionFeature.Require(SUPPORTS_REMOVE)
  public void testRemoveNegative() {
    assertThrows(IllegalArgumentException.class, () -> getMultiset().remove(e0(), -1));
    expectUnchanged();
  }

  @CollectionFeature.Require(absent = SUPPORTS_REMOVE)
  public void testRemoveUnsupported() {
    assertThrows(UnsupportedOperationException.class, () -> getMultiset().remove(e0(), 2));
  }

  @CollectionFeature.Require(SUPPORTS_REMOVE)
  public void testRemoveZeroNoOp() {
    int originalCount = getMultiset().count(e0());
    assertEquals("old count", originalCount, getMultiset().remove(e0(), 0));
    expectUnchanged();
  }

  @CollectionSize.Require(absent = ZERO)
  @CollectionFeature.Require(SUPPORTS_REMOVE)
  public void testRemove_occurrences_present() {
    assertEquals(
        "multiset.remove(present, 2) didn't return the old count",
        1,
        getMultiset().remove(e0(), 2));
    assertFalse(
        "multiset contains present after multiset.remove(present, 2)",
        getMultiset().contains(e0()));
    assertEquals(0, getMultiset().count(e0()));
  }

  @CollectionSize.Require(SEVERAL)
  @CollectionFeature.Require(SUPPORTS_REMOVE)
  public void testRemove_some_occurrences_present() {
    initThreeCopies();
    assertEquals(
        "multiset.remove(present, 2) didn't return the old count",
        3,
        getMultiset().remove(e0(), 2));
    assertTrue(
        "multiset contains present after multiset.remove(present, 2)",
        getMultiset().contains(e0()));
    assertEquals(1, getMultiset().count(e0()));
  }

  @CollectionFeature.Require(SUPPORTS_REMOVE)
  public void testRemove_occurrences_absent() {
    int distinct = getMultiset().elementSet().size();
    assertEquals("multiset.remove(absent, 0) didn't return 0", 0, getMultiset().remove(e3(), 2));
    assertEquals(distinct, getMultiset().elementSet().size());
  }

  @CollectionFeature.Require(absent = SUPPORTS_REMOVE)
  public void testRemove_occurrences_unsupported_absent() {
    // notice: we don't care whether it succeeds, or fails with UOE
    try {
      assertEquals(
          "multiset.remove(absent, 2) didn't return 0 or throw an exception",
          0,
          getMultiset().remove(e3(), 2));
    } catch (UnsupportedOperationException ok) {
    }
  }

  @CollectionFeature.Require(SUPPORTS_REMOVE)
  public void testRemove_occurrences_0() {
    int oldCount = getMultiset().count(e0());
    assertEquals(
        "multiset.remove(E, 0) didn't return the old count",
        oldCount,
        getMultiset().remove(e0(), 0));
  }

  @CollectionFeature.Require(SUPPORTS_REMOVE)
  public void testRemove_occurrences_negative() {
    assertThrows(IllegalArgumentException.class, () -> getMultiset().remove(e0(), -1));
  }

  @CollectionFeature.Require(SUPPORTS_REMOVE)
  public void testRemove_occurrences_wrongType() {
    assertEquals(
        "multiset.remove(wrongType, 1) didn't return 0",
        0,
        getMultiset().remove(WrongType.VALUE, 1));
  }

  @CollectionSize.Require(absent = ZERO)
  @CollectionFeature.Require({SUPPORTS_REMOVE, ALLOWS_NULL_VALUES})
  public void testRemove_nullPresent() {
    initCollectionWithNullElement();
    assertEquals(1, getMultiset().remove(null, 2));
    assertFalse(
        "multiset contains present after multiset.remove(present, 2)",
        getMultiset().contains(null));
    assertEquals(0, getMultiset().count(null));
  }

  @CollectionFeature.Require({SUPPORTS_REMOVE, ALLOWS_NULL_QUERIES})
  public void testRemove_nullAbsent() {
    assertEquals(0, getMultiset().remove(null, 2));
  }

  @CollectionFeature.Require(value = SUPPORTS_REMOVE, absent = ALLOWS_NULL_QUERIES)
  public void testRemove_nullForbidden() {
    assertThrows(NullPointerException.class, () -> getMultiset().remove(null, 2));
  }

  @CollectionSize.Require(SEVERAL)
  @CollectionFeature.Require(SUPPORTS_REMOVE)
  public void testRemoveAllIgnoresCount() {
    initThreeCopies();
    assertTrue(getMultiset().removeAll(singleton(e0())));
    assertEmpty(getMultiset());
  }

  @CollectionSize.Require(SEVERAL)
  @CollectionFeature.Require(SUPPORTS_REMOVE)
  public void testRetainAllIgnoresCount() {
    initThreeCopies();
    List<E> contents = copyToList(getMultiset());
    assertFalse(getMultiset().retainAll(singleton(e0())));
    expectContents(contents);
  }

  /**
   * Returns {@link Method} instances for the remove tests that assume multisets support duplicates
   * so that the test of {@code Multisets.forSet()} can suppress them.
   */
  @J2ktIncompatible
  @GwtIncompatible // reflection
  public static List<Method> getRemoveDuplicateInitializingMethods() {
    return asList(getMethod(MultisetRemoveTester.class, "testRemove_some_occurrences_present"));
  }
}
