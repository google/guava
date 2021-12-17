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
import static com.google.common.collect.testing.features.CollectionSize.ONE;
import static com.google.common.collect.testing.features.CollectionSize.ZERO;
import static com.google.common.collect.testing.features.ListFeature.SUPPORTS_ADD_WITH_INDEX;
import static java.util.Collections.singletonList;

import com.google.common.annotations.GwtCompatible;
import com.google.common.collect.testing.MinimalCollection;
import com.google.common.collect.testing.features.CollectionFeature;
import com.google.common.collect.testing.features.CollectionSize;
import com.google.common.collect.testing.features.ListFeature;
import java.util.List;
import org.junit.Ignore;

/**
 * A generic JUnit test which tests {@code addAll(int, Collection)} operations on a list. Can't be
 * invoked directly; please see {@link com.google.common.collect.testing.ListTestSuiteBuilder}.
 *
 * @author Chris Povirk
 */
@SuppressWarnings("unchecked") // too many "unchecked generic array creations"
@GwtCompatible
@Ignore // Affects only Android test runner, which respects JUnit 4 annotations on JUnit 3 tests.
public class ListAddAllAtIndexTester<E> extends AbstractListTester<E> {
  @ListFeature.Require(SUPPORTS_ADD_WITH_INDEX)
  @CollectionSize.Require(absent = ZERO)
  public void testAddAllAtIndex_supportedAllPresent() {
    assertTrue(
        "addAll(n, allPresent) should return true",
        getList().addAll(0, MinimalCollection.of(e0())));
    expectAdded(0, e0());
  }

  @ListFeature.Require(absent = SUPPORTS_ADD_WITH_INDEX)
  @CollectionSize.Require(absent = ZERO)
  public void testAddAllAtIndex_unsupportedAllPresent() {
    try {
      getList().addAll(0, MinimalCollection.of(e0()));
      fail("addAll(n, allPresent) should throw");
    } catch (UnsupportedOperationException expected) {
    }
    expectUnchanged();
  }

  @ListFeature.Require(SUPPORTS_ADD_WITH_INDEX)
  @CollectionSize.Require(absent = ZERO)
  public void testAddAllAtIndex_supportedSomePresent() {
    assertTrue(
        "addAll(n, allPresent) should return true",
        getList().addAll(0, MinimalCollection.of(e0(), e3())));
    expectAdded(0, e0(), e3());
  }

  @ListFeature.Require(absent = SUPPORTS_ADD_WITH_INDEX)
  @CollectionSize.Require(absent = ZERO)
  public void testAddAllAtIndex_unsupportedSomePresent() {
    try {
      getList().addAll(0, MinimalCollection.of(e0(), e3()));
      fail("addAll(n, allPresent) should throw");
    } catch (UnsupportedOperationException expected) {
    }
    expectUnchanged();
    expectMissing(e3());
  }

  @ListFeature.Require(SUPPORTS_ADD_WITH_INDEX)
  public void testAddAllAtIndex_supportedNothing() {
    assertFalse("addAll(n, nothing) should return false", getList().addAll(0, emptyCollection()));
    expectUnchanged();
  }

  @ListFeature.Require(absent = SUPPORTS_ADD_WITH_INDEX)
  public void testAddAllAtIndex_unsupportedNothing() {
    try {
      assertFalse(
          "addAll(n, nothing) should return false or throw",
          getList().addAll(0, emptyCollection()));
    } catch (UnsupportedOperationException tolerated) {
    }
    expectUnchanged();
  }

  @ListFeature.Require(SUPPORTS_ADD_WITH_INDEX)
  public void testAddAllAtIndex_withDuplicates() {
    MinimalCollection<E> elementsToAdd = MinimalCollection.of(e0(), e1(), e0(), e1());
    assertTrue("addAll(n, hasDuplicates) should return true", getList().addAll(0, elementsToAdd));
    expectAdded(0, e0(), e1(), e0(), e1());
  }

  @ListFeature.Require(SUPPORTS_ADD_WITH_INDEX)
  @CollectionFeature.Require(ALLOWS_NULL_VALUES)
  public void testAddAllAtIndex_nullSupported() {
    List<E> containsNull = singletonList(null);
    assertTrue("addAll(n, containsNull) should return true", getList().addAll(0, containsNull));
    /*
     * We need (E) to force interpretation of null as the single element of a
     * varargs array, not the array itself
     */
    expectAdded(0, (E) null);
  }

  @ListFeature.Require(SUPPORTS_ADD_WITH_INDEX)
  @CollectionFeature.Require(absent = ALLOWS_NULL_VALUES)
  public void testAddAllAtIndex_nullUnsupported() {
    List<E> containsNull = singletonList(null);
    try {
      getList().addAll(0, containsNull);
      fail("addAll(n, containsNull) should throw");
    } catch (NullPointerException expected) {
    }
    expectUnchanged();
    expectNullMissingWhenNullUnsupported(
        "Should not contain null after unsupported addAll(n, containsNull)");
  }

  @ListFeature.Require(SUPPORTS_ADD_WITH_INDEX)
  @CollectionSize.Require(absent = {ZERO, ONE})
  public void testAddAllAtIndex_middle() {
    assertTrue(
        "addAll(middle, disjoint) should return true",
        getList().addAll(getNumElements() / 2, createDisjointCollection()));
    expectAdded(getNumElements() / 2, createDisjointCollection());
  }

  @ListFeature.Require(SUPPORTS_ADD_WITH_INDEX)
  @CollectionSize.Require(absent = ZERO)
  public void testAddAllAtIndex_end() {
    assertTrue(
        "addAll(end, disjoint) should return true",
        getList().addAll(getNumElements(), createDisjointCollection()));
    expectAdded(getNumElements(), createDisjointCollection());
  }

  @ListFeature.Require(SUPPORTS_ADD_WITH_INDEX)
  public void testAddAllAtIndex_nullCollectionReference() {
    try {
      getList().addAll(0, null);
      fail("addAll(n, null) should throw");
    } catch (NullPointerException expected) {
    }
    expectUnchanged();
  }

  @ListFeature.Require(SUPPORTS_ADD_WITH_INDEX)
  public void testAddAllAtIndex_negative() {
    try {
      getList().addAll(-1, MinimalCollection.of(e3()));
      fail("addAll(-1, e) should throw");
    } catch (IndexOutOfBoundsException expected) {
    }
    expectUnchanged();
    expectMissing(e3());
  }

  @ListFeature.Require(SUPPORTS_ADD_WITH_INDEX)
  public void testAddAllAtIndex_tooLarge() {
    try {
      getList().addAll(getNumElements() + 1, MinimalCollection.of(e3()));
      fail("addAll(size + 1, e) should throw");
    } catch (IndexOutOfBoundsException expected) {
    }
    expectUnchanged();
    expectMissing(e3());
  }
}
