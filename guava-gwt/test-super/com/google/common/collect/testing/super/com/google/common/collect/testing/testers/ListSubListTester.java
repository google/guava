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

import static com.google.common.collect.testing.features.CollectionFeature.SERIALIZABLE_INCLUDING_VIEWS;
import static com.google.common.collect.testing.features.CollectionSize.ONE;
import static com.google.common.collect.testing.features.CollectionSize.ZERO;
import static com.google.common.collect.testing.features.ListFeature.SUPPORTS_ADD_WITH_INDEX;
import static com.google.common.collect.testing.features.ListFeature.SUPPORTS_REMOVE_WITH_INDEX;
import static com.google.common.collect.testing.features.ListFeature.SUPPORTS_SET;
import static java.util.Collections.emptyList;

import com.google.common.annotations.GwtCompatible;
import com.google.common.collect.testing.Helpers;
import com.google.common.collect.testing.features.CollectionFeature;
import com.google.common.collect.testing.features.CollectionSize;
import com.google.common.collect.testing.features.ListFeature;
import com.google.common.testing.SerializableTester;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * A generic JUnit test which tests {@code subList()} operations on a list.
 * Can't be invoked directly; please see
 * {@link com.google.common.collect.testing.ListTestSuiteBuilder}.
 *
 * @author Chris Povirk
 */
@SuppressWarnings("unchecked") // too many "unchecked generic array creations"
@GwtCompatible(emulated = true)
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
    assertEquals("subList(0, 0) should be empty",
        emptyList(), getList().subList(0, 0));
  }

  public void testSubList_entireList() {
    assertEquals("subList(0, size) should be equal to the original list",
        getList(), getList().subList(0, getNumElements()));
  }

  @ListFeature.Require(SUPPORTS_REMOVE_WITH_INDEX)
  @CollectionSize.Require(absent = ZERO)
  public void testSubList_subListRemoveAffectsOriginal() {
    List<E> subList = getList().subList(0, 1);
    subList.remove(0);
    List<E> expected =
        Arrays.asList(createSamplesArray()).subList(1, getNumElements());
    expectContents(expected);
  }

  @ListFeature.Require(SUPPORTS_REMOVE_WITH_INDEX)
  @CollectionSize.Require(absent = ZERO)
  public void testSubList_subListClearAffectsOriginal() {
    List<E> subList = getList().subList(0, 1);
    subList.clear();
    List<E> expected =
        Arrays.asList(createSamplesArray()).subList(1, getNumElements());
    expectContents(expected);
  }

  @ListFeature.Require(SUPPORTS_ADD_WITH_INDEX)
  public void testSubList_subListAddAffectsOriginal() {
    List<E> subList = getList().subList(0, 0);
    subList.add(samples.e3);
    expectAdded(0, samples.e3);
  }

  @ListFeature.Require(SUPPORTS_SET)
  @CollectionSize.Require(absent = ZERO)
  public void testSubList_subListSetAffectsOriginal() {
    List<E> subList = getList().subList(0, 1);
    subList.set(0, samples.e3);
    List<E> expected = Helpers.copyToList(createSamplesArray());
    expected.set(0, samples.e3);
    expectContents(expected);
  }

  @ListFeature.Require(SUPPORTS_SET)
  @CollectionSize.Require(absent = ZERO)
  public void testSubList_originalListSetAffectsSubList() {
    List<E> subList = getList().subList(0, 1);
    getList().set(0, samples.e3);
    assertEquals("A set() call to a list after a sublist has been created "
        + "should be reflected in the sublist",
        Collections.singletonList(samples.e3), subList);
  }

  @ListFeature.Require(SUPPORTS_REMOVE_WITH_INDEX)
  @CollectionSize.Require(absent = {ZERO, ONE})
  public void testSubList_subListRemoveAffectsOriginalLargeList() {
    List<E> subList = getList().subList(1, 3);
    subList.remove(samples.e2);
    List<E> expected = Helpers.copyToList(createSamplesArray());
    expected.remove(2);
    expectContents(expected);
  }

  @ListFeature.Require(SUPPORTS_ADD_WITH_INDEX)
  @CollectionSize.Require(absent = {ZERO, ONE})
  public void testSubList_subListAddAtIndexAffectsOriginalLargeList() {
    List<E> subList = getList().subList(2, 3);
    subList.add(0, samples.e3);
    expectAdded(2, samples.e3);
  }

  @ListFeature.Require(SUPPORTS_SET)
  @CollectionSize.Require(absent = {ZERO, ONE})
  public void testSubList_subListSetAffectsOriginalLargeList() {
    List<E> subList = getList().subList(1, 2);
    subList.set(0, samples.e3);
    List<E> expected = Helpers.copyToList(createSamplesArray());
    expected.set(1, samples.e3);
    expectContents(expected);
  }

  @ListFeature.Require(SUPPORTS_SET)
  @CollectionSize.Require(absent = {ZERO, ONE})
  public void testSubList_originalListSetAffectsSubListLargeList() {
    List<E> subList = getList().subList(1, 3);
    getList().set(1, samples.e3);
    assertEquals("A set() call to a list after a sublist has been created "
        + "should be reflected in the sublist",
        Arrays.asList(samples.e3, samples.e2), subList);
  }

  public void testSubList_ofSubListEmpty() {
    List<E> subList = getList().subList(0, 0).subList(0, 0);
    assertEquals("subList(0, 0).subList(0, 0) should be an empty list",
        emptyList(), subList);
  }

  @CollectionSize.Require(absent = {ZERO, ONE})
  public void testSubList_ofSubListNonEmpty() {
    List<E> subList = getList().subList(0, 2).subList(1, 2);
    assertEquals("subList(0, 2).subList(1, 2) "
        + "should be a single-element list of the element at index 1",
        Collections.singletonList(getOrderedElements().get(1)), subList);
  }

  @CollectionSize.Require(absent = {ZERO})
  public void testSubList_size() {
    List<E> list = getList();
    int size = getNumElements();
    assertEquals(list.subList(0, size).size(),
                 size);
    assertEquals(list.subList(0, size - 1).size(),
                 size - 1);
    assertEquals(list.subList(1, size).size(),
                 size - 1);
    assertEquals(list.subList(size, size).size(),
                 0);
    assertEquals(list.subList(0, 0).size(),
                 0);
  }

  @CollectionSize.Require(absent = {ZERO})
  public void testSubList_isEmpty() {
    List<E> list = getList();
    int size = getNumElements();
    for (List<E> subList : Arrays.asList(
        list.subList(0, size),
        list.subList(0, size - 1),
        list.subList(1, size),
        list.subList(0, 0),
        list.subList(size, size))) {
      assertEquals(subList.isEmpty(), subList.size() == 0);
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
    assertEquals(copy.indexOf(list.get(0)),
                 0);
    assertEquals(head.indexOf(list.get(0)),
                 0);
    assertEquals(tail.indexOf(list.get(1)),
                 0);
    // The following assumes all elements are distinct.
    assertEquals(copy.indexOf(list.get(size - 1)),
                 size - 1);
    assertEquals(head.indexOf(list.get(size - 2)),
                 size - 2);
    assertEquals(tail.indexOf(list.get(size - 1)),
                 size - 2);
    assertEquals(head.indexOf(list.get(size - 1)),
                 -1);
    assertEquals(tail.indexOf(list.get(0)),
                 -1);
  }

  @CollectionSize.Require(absent = {ZERO, ONE})
  public void testSubList_lastIndexOf() {
    List<E> list = getList();
    int size = list.size();
    List<E> copy = list.subList(0, size);
    List<E> head = list.subList(0, size - 1);
    List<E> tail = list.subList(1, size);
    assertEquals(copy.lastIndexOf(list.get(size - 1)),
                 size - 1);
    assertEquals(head.lastIndexOf(list.get(size - 2)),
                 size - 2);
    assertEquals(tail.lastIndexOf(list.get(size - 1)),
                 size - 2);
    // The following assumes all elements are distinct.
    assertEquals(copy.lastIndexOf(list.get(0)),
                 0);
    assertEquals(head.lastIndexOf(list.get(0)),
                 0);
    assertEquals(tail.lastIndexOf(list.get(1)),
                 0);
    assertEquals(head.lastIndexOf(list.get(size - 1)),
                 -1);
    assertEquals(tail.lastIndexOf(list.get(0)),
                 -1);
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

  /*
   * TODO: perform all List tests on subList(), but beware infinite recursion
   */
}

