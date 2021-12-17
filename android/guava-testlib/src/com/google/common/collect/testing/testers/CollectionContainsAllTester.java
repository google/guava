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

import static com.google.common.collect.testing.features.CollectionFeature.ALLOWS_NULL_QUERIES;
import static com.google.common.collect.testing.features.CollectionFeature.ALLOWS_NULL_VALUES;
import static com.google.common.collect.testing.features.CollectionSize.ZERO;

import com.google.common.annotations.GwtCompatible;
import com.google.common.collect.testing.AbstractCollectionTester;
import com.google.common.collect.testing.MinimalCollection;
import com.google.common.collect.testing.WrongType;
import com.google.common.collect.testing.features.CollectionFeature;
import com.google.common.collect.testing.features.CollectionSize;
import java.util.Collection;
import org.junit.Ignore;

/**
 * A generic JUnit test which tests {@code containsAll()} operations on a collection. Can't be
 * invoked directly; please see {@link
 * com.google.common.collect.testing.CollectionTestSuiteBuilder}.
 *
 * @author Kevin Bourrillion
 * @author Chris Povirk
 */
@SuppressWarnings("unchecked") // too many "unchecked generic array creations"
@GwtCompatible
@Ignore // Affects only Android test runner, which respects JUnit 4 annotations on JUnit 3 tests.
public class CollectionContainsAllTester<E> extends AbstractCollectionTester<E> {
  public void testContainsAll_empty() {
    assertTrue(
        "containsAll(empty) should return true", collection.containsAll(MinimalCollection.of()));
  }

  @CollectionSize.Require(absent = ZERO)
  public void testContainsAll_subset() {
    assertTrue(
        "containsAll(subset) should return true",
        collection.containsAll(MinimalCollection.of(e0())));
  }

  public void testContainsAll_sameElements() {
    assertTrue(
        "containsAll(sameElements) should return true",
        collection.containsAll(MinimalCollection.of(createSamplesArray())));
  }

  @SuppressWarnings("ModifyingCollectionWithItself")
  public void testContainsAll_self() {
    assertTrue("containsAll(this) should return true", collection.containsAll(collection));
  }

  public void testContainsAll_partialOverlap() {
    assertFalse(
        "containsAll(partialOverlap) should return false",
        collection.containsAll(MinimalCollection.of(e0(), e3())));
  }

  public void testContainsAll_disjoint() {
    assertFalse(
        "containsAll(disjoint) should return false",
        collection.containsAll(MinimalCollection.of(e3())));
  }

  @CollectionFeature.Require(absent = ALLOWS_NULL_QUERIES)
  public void testContainsAll_nullNotAllowed() {
    try {
      assertFalse(collection.containsAll(MinimalCollection.of((E) null)));
    } catch (NullPointerException tolerated) {
    }
  }

  @CollectionFeature.Require(ALLOWS_NULL_QUERIES)
  public void testContainsAll_nullAllowed() {
    assertFalse(collection.containsAll(MinimalCollection.of((E) null)));
  }

  @CollectionFeature.Require(ALLOWS_NULL_VALUES)
  @CollectionSize.Require(absent = ZERO)
  public void testContainsAll_nullPresent() {
    initCollectionWithNullElement();
    assertTrue(collection.containsAll(MinimalCollection.of((E) null)));
  }

  public void testContainsAll_wrongType() {
    Collection<WrongType> wrong = MinimalCollection.of(WrongType.VALUE);
    try {
      assertFalse(
          "containsAll(wrongType) should return false or throw", collection.containsAll(wrong));
    } catch (ClassCastException tolerated) {
    }
  }
}
