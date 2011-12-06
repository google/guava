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

package com.google.common.collect.testing.google;

import static com.google.common.collect.testing.features.CollectionFeature.SUPPORTS_ADD;
import static com.google.common.collect.testing.features.CollectionFeature.SUPPORTS_CLEAR;
import static com.google.common.collect.testing.features.CollectionFeature.SUPPORTS_REMOVE;
import static com.google.common.collect.testing.features.CollectionFeature.SUPPORTS_REMOVE_ALL;
import static com.google.common.collect.testing.features.CollectionFeature.SUPPORTS_RETAIN_ALL;
import static com.google.common.collect.testing.features.CollectionSize.ONE;
import static com.google.common.collect.testing.features.CollectionSize.ZERO;

import com.google.common.annotations.GwtCompatible;
import com.google.common.collect.Multiset;
import com.google.common.collect.Multisets;
import com.google.common.collect.testing.WrongType;
import com.google.common.collect.testing.features.CollectionFeature;
import com.google.common.collect.testing.features.CollectionSize;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Iterator;

/**
 * A generic JUnit test which tests multiset-specific write operations.
 * Can't be invoked directly; please see {@link MultisetTestSuiteBuilder}.
 *
 * @author Jared Levy
 */
@GwtCompatible
public class MultisetWritesTester<E> extends AbstractMultisetTester<E> {
  /**
   * Returns the {@link Method} instance for
   * {@link #testEntrySet_iterator()} so that tests of
   * classes with unmodifiable iterators can suppress it.
   */
  public static Method getEntrySetIteratorMethod() {
    return Platform.getMethod(
        MultisetWritesTester.class, "testEntrySet_iterator");
  }

  @CollectionFeature.Require(SUPPORTS_ADD)
  public void testAddOccurrences() {
    int oldCount = getMultiset().count(samples.e0);
    assertEquals("multiset.add(E, int) should return the old count",
        oldCount, getMultiset().add(samples.e0, 2));
    assertEquals("multiset.count() incorrect after add(E, int)",
        oldCount + 2, getMultiset().count(samples.e0));
  }

  @CollectionFeature.Require(absent = SUPPORTS_ADD)
  public void testAddOccurrences_unsupported() {
    try {
      getMultiset().add(samples.e0, 2);
      fail("unsupported multiset.add(E, int) didn't throw exception");
    } catch (UnsupportedOperationException required) {}
  }

  @CollectionSize.Require(absent = ZERO)
  @CollectionFeature.Require(SUPPORTS_REMOVE)
  public void testRemove_occurrences_present() {
    assertEquals("multiset.remove(present, 2) didn't return the old count",
        1, getMultiset().remove(samples.e0, 2));
    assertFalse("multiset contains present after multiset.remove(present, 2)",
        getMultiset().contains(samples.e0));
  }

  @CollectionFeature.Require(SUPPORTS_REMOVE)
  public void testRemove_occurrences_absent() {
    assertEquals("multiset.remove(absent, 0) didn't return 0",
        0, getMultiset().remove(samples.e3, 2));
  }

  @CollectionFeature.Require(absent = SUPPORTS_REMOVE)
  public void testRemove_occurrences_unsupported_absent() {
    // notice: we don't care whether it succeeds, or fails with UOE
    try {
      assertEquals(
          "multiset.remove(absent, 2) didn't return 0 or throw an exception",
          0, getMultiset().remove(samples.e3, 2));
    } catch (UnsupportedOperationException ok) {}
  }

  @CollectionFeature.Require(SUPPORTS_REMOVE)
  public void testRemove_occurrences_0() {
    int oldCount = getMultiset().count(samples.e0);
    assertEquals("multiset.remove(E, 0) didn't return the old count",
        oldCount, getMultiset().remove(samples.e0, 0));
  }

  @CollectionFeature.Require(SUPPORTS_REMOVE)
  public void testRemove_occurrences_negative() {
    try {
      getMultiset().remove(samples.e0, -1);
      fail("multiset.remove(E, -1) didn't throw an exception");
    } catch (IllegalArgumentException required) {}
  }

  @CollectionFeature.Require(SUPPORTS_REMOVE)
  public void testRemove_occurrences_wrongType() {
    assertEquals("multiset.remove(wrongType, 1) didn't return 0",
        0, getMultiset().remove(WrongType.VALUE, 1));
  }

  @CollectionFeature.Require(SUPPORTS_CLEAR)
  public void testEntrySet_clear() {
    getMultiset().entrySet().clear();
    assertTrue("multiset not empty after entrySet().clear()",
        getMultiset().isEmpty());
  }

  @CollectionSize.Require(ONE)
  @CollectionFeature.Require(SUPPORTS_REMOVE)
  public void testEntrySet_iterator() {
    Iterator<Multiset.Entry<E>> iterator = getMultiset().entrySet().iterator();
    assertTrue(
        "non-empty multiset.entrySet() iterator.hasNext() returned false",
        iterator.hasNext());
    assertEquals("multiset.entrySet() iterator.next() returned incorrect entry",
        Multisets.immutableEntry(samples.e0, 1), iterator.next());
    assertFalse(
        "size 1 multiset.entrySet() iterator.hasNext() returned true "
            + "after next()",
        iterator.hasNext());
    iterator.remove();
    assertTrue(
        "multiset isn't empty after multiset.entrySet() iterator.remove()",
        getMultiset().isEmpty());
  }

  @CollectionSize.Require(absent = ZERO)
  @CollectionFeature.Require(absent = SUPPORTS_REMOVE)
  public void testEntrySet_iterator_remove_unsupported() {
    Iterator<Multiset.Entry<E>> iterator = getMultiset().entrySet().iterator();
    assertTrue(
        "non-empty multiset.entrySet() iterator.hasNext() returned false",
        iterator.hasNext());
    try {
      iterator.remove();
      fail("multiset.entrySet() iterator.remove() didn't throw an exception");
    } catch (UnsupportedOperationException expected) {}
  }

  @CollectionSize.Require(absent = ZERO)
  @CollectionFeature.Require(SUPPORTS_REMOVE)
  public void testEntrySet_remove_present() {
    assertTrue(
        "multiset.entrySet.remove(presentEntry) returned false",
        getMultiset().entrySet().remove(
            Multisets.immutableEntry(samples.e0, 1)));
    assertFalse(
        "multiset contains element after removing its entry",
        getMultiset().contains(samples.e0));
  }

  @CollectionSize.Require(absent = ZERO)
  @CollectionFeature.Require(SUPPORTS_REMOVE)
  public void testEntrySet_remove_missing() {
    assertFalse(
        "multiset.entrySet.remove(missingEntry) returned true",
        getMultiset().entrySet().remove(
            Multisets.immutableEntry(samples.e0, 2)));
    assertTrue(
        "multiset didn't contain element after removing a missing entry",
        getMultiset().contains(samples.e0));
  }

  @CollectionSize.Require(absent = ZERO)
  @CollectionFeature.Require(SUPPORTS_REMOVE_ALL)
  public void testEntrySet_removeAll_present() {
    assertTrue(
        "multiset.entrySet.removeAll(presentEntry) returned false",
        getMultiset().entrySet().removeAll(
            Collections.singleton(Multisets.immutableEntry(samples.e0, 1))));
    assertFalse(
        "multiset contains element after removing its entry",
        getMultiset().contains(samples.e0));
  }

  @CollectionSize.Require(absent = ZERO)
  @CollectionFeature.Require(SUPPORTS_REMOVE_ALL)
  public void testEntrySet_removeAll_missing() {
    assertFalse(
        "multiset.entrySet.remove(missingEntry) returned true",
        getMultiset().entrySet().removeAll(
            Collections.singleton(Multisets.immutableEntry(samples.e0, 2))));
    assertTrue(
        "multiset didn't contain element after removing a missing entry",
        getMultiset().contains(samples.e0));
  }

  @CollectionFeature.Require(SUPPORTS_REMOVE_ALL)
  public void testEntrySet_removeAll_null() {
    try {
      getMultiset().entrySet().removeAll(null);
      fail("multiset.entrySet.removeAll(null) didn't throw an exception");
    } catch (NullPointerException expected) {}
  }

  @CollectionSize.Require(ONE)
  @CollectionFeature.Require(SUPPORTS_RETAIN_ALL)
  public void testEntrySet_retainAll_present() {
    assertFalse(
        "multiset.entrySet.retainAll(presentEntry) returned false",
        getMultiset().entrySet().retainAll(
            Collections.singleton(Multisets.immutableEntry(samples.e0, 1))));
    assertTrue(
        "multiset doesn't contains element after retaining its entry",
        getMultiset().contains(samples.e0));
  }

  @CollectionSize.Require(ONE)
  @CollectionFeature.Require(SUPPORTS_RETAIN_ALL)
  public void testEntrySet_retainAll_missing() {
    assertTrue(
        "multiset.entrySet.retainAll(missingEntry) returned true",
        getMultiset().entrySet().retainAll(
            Collections.singleton(Multisets.immutableEntry(samples.e0, 2))));
    assertFalse(
        "multiset contains element after retaining a different entry",
        getMultiset().contains(samples.e0));
  }

  @CollectionFeature.Require(SUPPORTS_RETAIN_ALL)
  public void testEntrySet_retainAll_null() {
    try {
      getMultiset().entrySet().retainAll(null);
      // Returning successfully is not ideal, but tolerated.
    } catch (NullPointerException expected) {}
  }
}
