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
import com.google.common.collect.testing.MinimalCollection;
import com.google.common.collect.testing.WrongType;
import com.google.common.collect.testing.features.CollectionFeature;
import com.google.common.collect.testing.features.CollectionSize;
import java.util.AbstractSet;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import org.junit.Ignore;

/**
 * A generic JUnit test which tests {@code removeAll} operations on a collection. Can't be invoked
 * directly; please see {@link com.google.common.collect.testing.CollectionTestSuiteBuilder}.
 *
 * @author George van den Driessche
 * @author Chris Povirk
 */
@SuppressWarnings("unchecked") // too many "unchecked generic array creations"
@GwtCompatible
@Ignore // Affects only Android test runner, which respects JUnit 4 annotations on JUnit 3 tests.
public class CollectionRemoveAllTester<E> extends AbstractCollectionTester<E> {
  @CollectionFeature.Require(SUPPORTS_REMOVE)
  public void testRemoveAll_emptyCollection() {
    assertFalse(
        "removeAll(emptyCollection) should return false",
        collection.removeAll(MinimalCollection.of()));
    expectUnchanged();
  }

  @CollectionFeature.Require(SUPPORTS_REMOVE)
  public void testRemoveAll_nonePresent() {
    assertFalse(
        "removeAll(disjointCollection) should return false",
        collection.removeAll(MinimalCollection.of(e3())));
    expectUnchanged();
  }

  @CollectionFeature.Require(SUPPORTS_REMOVE)
  @CollectionSize.Require(absent = ZERO)
  public void testRemoveAll_allPresent() {
    assertTrue(
        "removeAll(intersectingCollection) should return true",
        collection.removeAll(MinimalCollection.of(e0())));
    expectMissing(e0());
  }

  @CollectionFeature.Require(SUPPORTS_REMOVE)
  @CollectionSize.Require(absent = ZERO)
  public void testRemoveAll_somePresent() {
    assertTrue(
        "removeAll(intersectingCollection) should return true",
        collection.removeAll(MinimalCollection.of(e0(), e3())));
    expectMissing(e0());
  }

  @CollectionFeature.Require({SUPPORTS_REMOVE, FAILS_FAST_ON_CONCURRENT_MODIFICATION})
  @CollectionSize.Require(SEVERAL)
  public void testRemoveAllSomePresentConcurrentWithIteration() {
    try {
      Iterator<E> iterator = collection.iterator();
      assertTrue(collection.removeAll(MinimalCollection.of(e0(), e3())));
      iterator.next();
      fail("Expected ConcurrentModificationException");
    } catch (ConcurrentModificationException expected) {
      // success
    }
  }

  /** Trigger the {@code other.size() >= this.size()} case in {@link AbstractSet#removeAll}. */
  @CollectionFeature.Require(SUPPORTS_REMOVE)
  @CollectionSize.Require(absent = ZERO)
  public void testRemoveAll_somePresentLargeCollectionToRemove() {
    assertTrue(
        "removeAll(largeIntersectingCollection) should return true",
        collection.removeAll(MinimalCollection.of(e0(), e0(), e0(), e3(), e3(), e3())));
    expectMissing(e0());
  }

  @CollectionFeature.Require(absent = SUPPORTS_REMOVE)
  public void testRemoveAll_unsupportedEmptyCollection() {
    try {
      assertFalse(
          "removeAll(emptyCollection) should return false or throw "
              + "UnsupportedOperationException",
          collection.removeAll(MinimalCollection.of()));
    } catch (UnsupportedOperationException tolerated) {
    }
    expectUnchanged();
  }

  @CollectionFeature.Require(absent = SUPPORTS_REMOVE)
  public void testRemoveAll_unsupportedNonePresent() {
    try {
      assertFalse(
          "removeAll(disjointCollection) should return false or throw "
              + "UnsupportedOperationException",
          collection.removeAll(MinimalCollection.of(e3())));
    } catch (UnsupportedOperationException tolerated) {
    }
    expectUnchanged();
  }

  @CollectionFeature.Require(absent = SUPPORTS_REMOVE)
  @CollectionSize.Require(absent = ZERO)
  public void testRemoveAll_unsupportedPresent() {
    try {
      collection.removeAll(MinimalCollection.of(e0()));
      fail("removeAll(intersectingCollection) should throw UnsupportedOperationException");
    } catch (UnsupportedOperationException expected) {
    }
    expectUnchanged();
    assertTrue(collection.contains(e0()));
  }

  /*
   * AbstractCollection fails the removeAll(null) test when the subject
   * collection is empty, but we'd still like to test removeAll(null) when we
   * can. We split the test into empty and non-empty cases. This allows us to
   * suppress only the former.
   */

  @CollectionFeature.Require(SUPPORTS_REMOVE)
  @CollectionSize.Require(ZERO)
  public void testRemoveAll_nullCollectionReferenceEmptySubject() {
    try {
      collection.removeAll(null);
      // Returning successfully is not ideal, but tolerated.
    } catch (NullPointerException tolerated) {
    }
  }

  @CollectionFeature.Require(SUPPORTS_REMOVE)
  @CollectionSize.Require(absent = ZERO)
  public void testRemoveAll_nullCollectionReferenceNonEmptySubject() {
    try {
      collection.removeAll(null);
      fail("removeAll(null) should throw NullPointerException");
    } catch (NullPointerException expected) {
    }
  }

  @CollectionFeature.Require(value = SUPPORTS_REMOVE, absent = ALLOWS_NULL_QUERIES)
  public void testRemoveAll_containsNullNo() {
    MinimalCollection<?> containsNull = MinimalCollection.of((Object) null);
    try {
      assertFalse(
          "removeAll(containsNull) should return false or throw",
          collection.removeAll(containsNull));
    } catch (NullPointerException tolerated) {
    }
    expectUnchanged();
  }

  @CollectionFeature.Require({SUPPORTS_REMOVE, ALLOWS_NULL_QUERIES})
  public void testRemoveAll_containsNullNoButAllowed() {
    MinimalCollection<?> containsNull = MinimalCollection.of((Object) null);
    assertFalse("removeAll(containsNull) should return false", collection.removeAll(containsNull));
    expectUnchanged();
  }

  @CollectionFeature.Require({SUPPORTS_REMOVE, ALLOWS_NULL_VALUES})
  @CollectionSize.Require(absent = ZERO)
  public void testRemoveAll_containsNullYes() {
    initCollectionWithNullElement();
    assertTrue(
        "removeAll(containsNull) should return true",
        collection.removeAll(Collections.singleton(null)));
    // TODO: make this work with MinimalCollection
  }

  @CollectionFeature.Require(SUPPORTS_REMOVE)
  public void testRemoveAll_containsWrongType() {
    try {
      assertFalse(
          "removeAll(containsWrongType) should return false or throw",
          collection.removeAll(MinimalCollection.of(WrongType.VALUE)));
    } catch (ClassCastException tolerated) {
    }
    expectUnchanged();
  }
}
