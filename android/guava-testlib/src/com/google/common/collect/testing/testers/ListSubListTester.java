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

import static com.google.common.collect.testing.Helpers.getMethod;
import static com.google.common.collect.testing.features.CollectionFeature.SERIALIZABLE_INCLUDING_VIEWS;
import static com.google.common.collect.testing.features.CollectionSize.ONE;
import static com.google.common.collect.testing.features.CollectionSize.ZERO;
import static com.google.common.collect.testing.features.ListFeature.SUPPORTS_ADD_WITH_INDEX;
import static com.google.common.collect.testing.features.ListFeature.SUPPORTS_REMOVE_WITH_INDEX;
import static com.google.common.collect.testing.features.ListFeature.SUPPORTS_SET;
import static java.util.Collections.emptyList;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.collect.testing.Helpers;
import com.google.common.collect.testing.features.CollectionFeature;
import com.google.common.collect.testing.features.CollectionSize;
import com.google.common.collect.testing.features.ListFeature;
import com.google.common.testing.SerializableTester;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.Ignore;

/**
 * A generic JUnit test which tests {@code subList()} operations on a list. Can't be invoked
 * directly; please see {@link com.google.common.collect.testing.ListTestSuiteBuilder}.
 *
 * @author Chris Povirk
 */
@SuppressWarnings("unchecked") // too many "unchecked generic array creations"
@GwtCompatible(emulated = true)
@Ignore // Affects only Android test runner, which respects JUnit 4 annotations on JUnit 3 tests.
public class ListSubListTester<E> extends AbstractListTester<E> {
  public void testSubList_startNegative() {
    try {
      getList().subList(-1, 0);
      fail("subList(-1, 0) should throw");
    } catch (IndexOutOfBoundsException expected) {
    }
  }

  public void testSubList_endTooLarge() {
    try {
      getList().subList(0, getNumElements() + 1);
      fail("subList(0, size + 1) should throw");
    } catch (IndexOutOfBoundsException expected) {
    }
  }

  public void testSubList_startGreaterThanEnd() {
    try {
      getList().subList(1, 0);
      fail("subList(1, 0) should throw");
    } catch (IndexOutOfBoundsException expected) {
    } catch (IllegalArgumentException expected) {
      /*
       * The subList() docs claim that this should be an
       * IndexOutOfBoundsException, but many JDK implementations throw
       * IllegalArgumentException:
       * http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4506427
       */
    }
  }

  public void testSubList_empty() {
    assertEquals("subList(0, 0) should be empty", emptyList(), getList().subList(0, 0));
  }

  public void testSubList_entireList() {
    assertEquals(
        "subList(0, size) should be equal to the original list",
        getList(),
        getList().subList(0, getNumElements()));
  }

  @ListFeature.Require(SUPPORTS_REMOVE_WITH_INDEX)
  @CollectionSize.Require(absent = ZERO)
  public void testSubList_subListRemoveAffectsOriginal() {
    List<E> subList = getList().subList(0, 1);
    subList.remove(0);
    List<E> expected = Arrays.asList(createSamplesArray()).subList(1, getNumElements());
    expectContents(expected);
  }

  @ListFeature.Require(SUPPORTS_REMOVE_WITH_INDEX)
  @CollectionSize.Require(absent = ZERO)
  public void testSubList_subListClearAffectsOriginal() {
    List<E> subList = getList().subList(0, 1);
    subList.clear();
    List<E> expected = Arrays.asList(createSamplesArray()).subList(1, getNumElements());
    expectContents(expected);
  }

  @ListFeature.Require(SUPPORTS_ADD_WITH_INDEX)
  public void testSubList_subListAddAffectsOriginal() {
    List<E> subList = getList().subList(0, 0);
    subList.add(e3());
    expectAdded(0, e3());
  }

  @ListFeature.Require(SUPPORTS_SET)
  @CollectionSize.Require(absent = ZERO)
  public void testSubList_subListSetAffectsOriginal() {
    List<E> subList = getList().subList(0, 1);
    subList.set(0, e3());
    List<E> expected = Helpers.copyToList(createSamplesArray());
    expected.set(0, e3());
    expectContents(expected);
  }

  @ListFeature.Require(SUPPORTS_SET)
  @CollectionSize.Require(absent = ZERO)
  public void testSubList_originalListSetAffectsSubList() {
    List<E> subList = getList().subList(0, 1);
    getList().set(0, e3());
    assertEquals(
        "A set() call to a list after a sublist has been created "
            + "should be reflected in the sublist",
        Collections.singletonList(e3()),
        subList);
  }

  @ListFeature.Require(SUPPORTS_REMOVE_WITH_INDEX)
  @CollectionSize.Require(absent = {ZERO, ONE})
  public void testSubList_subListRemoveAffectsOriginalLargeList() {
    List<E> subList = getList().subList(1, 3);
    subList.remove(e2());
    List<E> expected = Helpers.copyToList(createSamplesArray());
    expected.remove(2);
    expectContents(expected);
  }

  @ListFeature.Require(SUPPORTS_ADD_WITH_INDEX)
  @CollectionSize.Require(absent = {ZERO, ONE})
  public void testSubList_subListAddAtIndexAffectsOriginalLargeList() {
    List<E> subList = getList().subList(2, 3);
    subList.add(0, e3());
    expectAdded(2, e3());
  }

  @ListFeature.Require(SUPPORTS_SET)
  @CollectionSize.Require(absent = {ZERO, ONE})
  public void testSubList_subListSetAffectsOriginalLargeList() {
    List<E> subList = getList().subList(1, 2);
    subList.set(0, e3());
    List<E> expected = Helpers.copyToList(createSamplesArray());
    expected.set(1, e3());
    expectContents(expected);
  }

  @ListFeature.Require(SUPPORTS_SET)
  @CollectionSize.Require(absent = {ZERO, ONE})
  public void testSubList_originalListSetAffectsSubListLargeList() {
    List<E> subList = getList().subList(1, 3);
    getList().set(1, e3());
    assertEquals(
        "A set() call to a list after a sublist has been created "
            + "should be reflected in the sublist",
        Arrays.asList(e3(), e2()),
        subList);
  }

  public void testSubList_ofSubListEmpty() {
    List<E> subList = getList().subList(0, 0).subList(0, 0);
    assertEquals("subList(0, 0).subList(0, 0) should be an empty list", emptyList(), subList);
  }

  @CollectionSize.Require(absent = {ZERO, ONE})
  public void testSubList_ofSubListNonEmpty() {
    List<E> subList = getList().subList(0, 2).subList(1, 2);
    assertEquals(
        "subList(0, 2).subList(1, 2) "
            + "should be a single-element list of the element at index 1",
        Collections.singletonList(getOrderedElements().get(1)),
        subList);
  }

  @CollectionSize.Require(absent = {ZERO})
  public void testSubList_size() {
    List<E> list = getList();
    int size = getNumElements();
    assertEquals(size, list.subList(0, size).size());
    assertEquals(size - 1, list.subList(0, size - 1).size());
    assertEquals(size - 1, list.subList(1, size).size());
    assertEquals(0, list.subList(size, size).size());
    assertEquals(0, list.subList(0, 0).size());
  }

  @CollectionSize.Require(absent = {ZERO})
  public void testSubList_isEmpty() {
    List<E> list = getList();
    int size = getNumElements();
    for (List<E> subList :
        Arrays.asList(
            list.subList(0, size),
            list.subList(0, size - 1),
            list.subList(1, size),
            list.subList(0, 0),
            list.subList(size, size))) {
      assertEquals(subList.size() == 0, subList.isEmpty());
    }
  }

  @CollectionSize.Require(absent = {ZERO, ONE})
  public void testSubList_get() {
    List<E> list = getList();
    int size = getNumElements();
    List<E> copy = list.subList(0, size);
    List<E> head = list.subList(0, size - 1);
    List<E> tail = list.subList(1, size);
    assertEquals(list.get(0), copy.get(0));
    assertEquals(list.get(size - 1), copy.get(size - 1));
    assertEquals(list.get(1), tail.get(0));
    assertEquals(list.get(size - 1), tail.get(size - 2));
    assertEquals(list.get(0), head.get(0));
    assertEquals(list.get(size - 2), head.get(size - 2));
    for (List<E> subList : Arrays.asList(copy, head, tail)) {
      for (int index : Arrays.asList(-1, subList.size())) {
        try {
          subList.get(index);
          fail("expected IndexOutOfBoundsException");
        } catch (IndexOutOfBoundsException expected) {
        }
      }
    }
  }

  @CollectionSize.Require(absent = {ZERO, ONE})
  public void testSubList_contains() {
    List<E> list = getList();
    int size = getNumElements();
    List<E> copy = list.subList(0, size);
    List<E> head = list.subList(0, size - 1);
    List<E> tail = list.subList(1, size);
    assertTrue(copy.contains(list.get(0)));
    assertTrue(head.contains(list.get(0)));
    assertTrue(tail.contains(list.get(1)));
    // The following assumes all elements are distinct.
    assertTrue(copy.contains(list.get(size - 1)));
    assertTrue(head.contains(list.get(size - 2)));
    assertTrue(tail.contains(list.get(size - 1)));
    assertFalse(head.contains(list.get(size - 1)));
    assertFalse(tail.contains(list.get(0)));
  }

  @CollectionSize.Require(absent = {ZERO, ONE})
  public void testSubList_indexOf() {
    List<E> list = getList();
    int size = getNumElements();
    List<E> copy = list.subList(0, size);
    List<E> head = list.subList(0, size - 1);
    List<E> tail = list.subList(1, size);
    assertEquals(0, copy.indexOf(list.get(0)));
    assertEquals(0, head.indexOf(list.get(0)));
    assertEquals(0, tail.indexOf(list.get(1)));
    // The following assumes all elements are distinct.
    assertEquals(size - 1, copy.indexOf(list.get(size - 1)));
    assertEquals(size - 2, head.indexOf(list.get(size - 2)));
    assertEquals(size - 2, tail.indexOf(list.get(size - 1)));
    assertEquals(-1, head.indexOf(list.get(size - 1)));
    assertEquals(-1, tail.indexOf(list.get(0)));
  }

  @CollectionSize.Require(absent = {ZERO, ONE})
  public void testSubList_lastIndexOf() {
    List<E> list = getList();
    int size = list.size();
    List<E> copy = list.subList(0, size);
    List<E> head = list.subList(0, size - 1);
    List<E> tail = list.subList(1, size);
    assertEquals(size - 1, copy.lastIndexOf(list.get(size - 1)));
    assertEquals(size - 2, head.lastIndexOf(list.get(size - 2)));
    assertEquals(size - 2, tail.lastIndexOf(list.get(size - 1)));
    // The following assumes all elements are distinct.
    assertEquals(0, copy.lastIndexOf(list.get(0)));
    assertEquals(0, head.lastIndexOf(list.get(0)));
    assertEquals(0, tail.lastIndexOf(list.get(1)));
    assertEquals(-1, head.lastIndexOf(list.get(size - 1)));
    assertEquals(-1, tail.lastIndexOf(list.get(0)));
  }

  @CollectionFeature.Require(SERIALIZABLE_INCLUDING_VIEWS)
  public void testReserializeWholeSubList() {
    SerializableTester.reserializeAndAssert(getList().subList(0, getNumElements()));
  }

  @CollectionFeature.Require(SERIALIZABLE_INCLUDING_VIEWS)
  public void testReserializeEmptySubList() {
    SerializableTester.reserializeAndAssert(getList().subList(0, 0));
  }

  @CollectionFeature.Require(SERIALIZABLE_INCLUDING_VIEWS)
  @CollectionSize.Require(absent = {ZERO, ONE})
  public void testReserializeSubList() {
    SerializableTester.reserializeAndAssert(getList().subList(0, 2));
  }

  /**
   * Returns the {@link Method} instance for {@link #testSubList_originalListSetAffectsSubList()} so
   * that tests of {@link CopyOnWriteArrayList} can suppress them with {@code
   * FeatureSpecificTestSuiteBuilder.suppressing()} until <a
   * href="http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6570631">Sun bug 6570631</a> is fixed.
   */
  @GwtIncompatible // reflection
  public static Method getSubListOriginalListSetAffectsSubListMethod() {
    return getMethod(ListSubListTester.class, "testSubList_originalListSetAffectsSubList");
  }

  /**
   * Returns the {@link Method} instance for {@link
   * #testSubList_originalListSetAffectsSubListLargeList()} ()} so that tests of {@link
   * CopyOnWriteArrayList} can suppress them with {@code
   * FeatureSpecificTestSuiteBuilder.suppressing()} until <a
   * href="http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6570631">Sun bug 6570631</a> is fixed.
   */
  @GwtIncompatible // reflection
  public static Method getSubListOriginalListSetAffectsSubListLargeListMethod() {
    return getMethod(ListSubListTester.class, "testSubList_originalListSetAffectsSubListLargeList");
  }

  /**
   * Returns the {@link Method} instance for {@link
   * #testSubList_subListRemoveAffectsOriginalLargeList()} so that tests of {@link
   * CopyOnWriteArrayList} can suppress it with {@code
   * FeatureSpecificTestSuiteBuilder.suppressing()} until <a
   * href="http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6570575">Sun bug 6570575</a> is fixed.
   */
  @GwtIncompatible // reflection
  public static Method getSubListSubListRemoveAffectsOriginalLargeListMethod() {
    return getMethod(ListSubListTester.class, "testSubList_subListRemoveAffectsOriginalLargeList");
  }

  /*
   * TODO: perform all List tests on subList(), but beware infinite recursion
   */
}
