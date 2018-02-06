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

import static com.google.common.collect.testing.features.CollectionFeature.ALLOWS_NULL_QUERIES;
import static com.google.common.collect.testing.features.CollectionFeature.ALLOWS_NULL_VALUES;
import static com.google.common.collect.testing.features.CollectionFeature.FAILS_FAST_ON_CONCURRENT_MODIFICATION;
import static com.google.common.collect.testing.features.CollectionFeature.SUPPORTS_REMOVE;
import static com.google.common.collect.testing.features.CollectionSize.SEVERAL;
import static com.google.common.collect.testing.features.CollectionSize.ZERO;

import com.google.common.annotations.GwtCompatible;
import com.google.common.collect.testing.AbstractCollectionTester;
import com.google.common.collect.testing.WrongType;
import com.google.common.collect.testing.features.CollectionFeature;
import com.google.common.collect.testing.features.CollectionSize;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import org.junit.Ignore;

/**
 * A generic JUnit test which tests {@code remove} operations on a collection. Can't be invoked
 * directly; please see {@link com.google.common.collect.testing.CollectionTestSuiteBuilder}.
 *
 * @author George van den Driessche
 */
@SuppressWarnings("unchecked") // too many "unchecked generic array creations"
@GwtCompatible
@Ignore // Affects only Android test runner, which respects JUnit 4 annotations on JUnit 3 tests.
public class CollectionRemoveTester<E> extends AbstractCollectionTester<E> {
  @CollectionFeature.Require(SUPPORTS_REMOVE)
  @CollectionSize.Require(absent = ZERO)
  public void testRemove_present() {
    int initialSize = collection.size();
    assertTrue("remove(present) should return true", collection.remove(e0()));
    assertEquals(
        "remove(present) should decrease a collection's size by one.",
        initialSize - 1,
        collection.size());
    expectMissing(e0());
  }

  @CollectionFeature.Require({SUPPORTS_REMOVE, FAILS_FAST_ON_CONCURRENT_MODIFICATION})
  @CollectionSize.Require(SEVERAL)
  public void testRemovePresentConcurrentWithIteration() {
    try {
      Iterator<E> iterator = collection.iterator();
      assertTrue(collection.remove(e0()));
      iterator.next();
      fail("Expected ConcurrentModificationException");
    } catch (ConcurrentModificationException expected) {
      // success
    }
  }

  @CollectionFeature.Require(SUPPORTS_REMOVE)
  public void testRemove_notPresent() {
    assertFalse("remove(notPresent) should return false", collection.remove(e3()));
    expectUnchanged();
  }

  @CollectionFeature.Require({SUPPORTS_REMOVE, ALLOWS_NULL_VALUES})
  @CollectionSize.Require(absent = ZERO)
  public void testRemove_nullPresent() {
    collection = getSubjectGenerator().create(createArrayWithNullElement());

    int initialSize = collection.size();
    assertTrue("remove(null) should return true", collection.remove(null));
    assertEquals(
        "remove(present) should decrease a collection's size by one.",
        initialSize - 1,
        collection.size());
    expectMissing((E) null);
  }

  @CollectionFeature.Require(absent = SUPPORTS_REMOVE)
  @CollectionSize.Require(absent = ZERO)
  public void testRemove_unsupported() {
    try {
      collection.remove(e0());
      fail("remove(present) should throw UnsupportedOperationException");
    } catch (UnsupportedOperationException expected) {
    }
    expectUnchanged();
    assertTrue("remove(present) should not remove the element", collection.contains(e0()));
  }

  @CollectionFeature.Require(absent = SUPPORTS_REMOVE)
  public void testRemove_unsupportedNotPresent() {
    try {
      assertFalse(
          "remove(notPresent) should return false or throw UnsupportedOperationException",
          collection.remove(e3()));
    } catch (UnsupportedOperationException tolerated) {
    }
    expectUnchanged();
    expectMissing(e3());
  }

  @CollectionFeature.Require(value = SUPPORTS_REMOVE, absent = ALLOWS_NULL_QUERIES)
  public void testRemove_nullNotSupported() {
    try {
      assertFalse(
          "remove(null) should return false or throw NullPointerException",
          collection.remove(null));
    } catch (NullPointerException tolerated) {
    }
    expectUnchanged();
  }

  @CollectionFeature.Require({SUPPORTS_REMOVE, ALLOWS_NULL_QUERIES})
  public void testRemove_nullAllowed() {
    assertFalse("remove(null) should return false", collection.remove(null));
    expectUnchanged();
  }

  @CollectionFeature.Require(absent = SUPPORTS_REMOVE)
  @CollectionSize.Require(absent = ZERO)
  public void testIteratorRemove_unsupported() {
    Iterator<E> iterator = collection.iterator();
    iterator.next();
    try {
      iterator.remove();
      fail("iterator.remove() should throw UnsupportedOperationException");
    } catch (UnsupportedOperationException expected) {
    }
    expectUnchanged();
    assertTrue(collection.contains(e0()));
  }

  @CollectionFeature.Require(SUPPORTS_REMOVE)
  public void testRemove_wrongType() {
    try {
      assertFalse(collection.remove(WrongType.VALUE));
    } catch (ClassCastException tolerated) {
    }
    expectUnchanged();
  }
}
