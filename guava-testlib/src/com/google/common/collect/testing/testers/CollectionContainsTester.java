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
import com.google.common.collect.testing.WrongType;
import com.google.common.collect.testing.features.CollectionFeature;
import com.google.common.collect.testing.features.CollectionSize;
import org.junit.Ignore;

/**
 * A generic JUnit test which tests {@code contains()} operations on a collection. Can't be invoked
 * directly; please see {@link com.google.common.collect.testing.CollectionTestSuiteBuilder}.
 *
 * @author Kevin Bourrillion
 * @author Chris Povirk
 */
@GwtCompatible
@Ignore // Affects only Android test runner, which respects JUnit 4 annotations on JUnit 3 tests.
public class CollectionContainsTester<E> extends AbstractCollectionTester<E> {
  @CollectionSize.Require(absent = ZERO)
  public void testContains_yes() {
    assertTrue("contains(present) should return true", collection.contains(e0()));
  }

  public void testContains_no() {
    assertFalse("contains(notPresent) should return false", collection.contains(e3()));
  }

  @CollectionFeature.Require(ALLOWS_NULL_QUERIES)
  public void testContains_nullNotContainedButQueriesSupported() {
    assertFalse("contains(null) should return false", collection.contains(null));
  }

  @CollectionFeature.Require(absent = ALLOWS_NULL_QUERIES)
  public void testContains_nullNotContainedAndUnsupported() {
    expectNullMissingWhenNullUnsupported("contains(null) should return false or throw");
  }

  @CollectionFeature.Require(ALLOWS_NULL_VALUES)
  @CollectionSize.Require(absent = ZERO)
  public void testContains_nonNullWhenNullContained() {
    initCollectionWithNullElement();
    assertFalse("contains(notPresent) should return false", collection.contains(e3()));
  }

  @CollectionFeature.Require(ALLOWS_NULL_VALUES)
  @CollectionSize.Require(absent = ZERO)
  public void testContains_nullContained() {
    initCollectionWithNullElement();
    assertTrue("contains(null) should return true", collection.contains(null));
  }

  public void testContains_wrongType() {
    try {
      assertFalse(
          "contains(wrongType) should return false or throw", collection.contains(WrongType.VALUE));
    } catch (ClassCastException tolerated) {
    }
  }
}
