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

import static com.google.common.collect.testing.features.CollectionFeature.SUPPORTS_ADD;
import static com.google.common.collect.testing.features.CollectionSize.ZERO;

import com.google.common.annotations.GwtCompatible;
import com.google.common.collect.testing.MinimalCollection;
import com.google.common.collect.testing.features.CollectionFeature;
import com.google.common.collect.testing.features.CollectionSize;
import org.junit.Ignore;

/**
 * A generic JUnit test which tests {@code addAll(Collection)} operations on a list. Can't be
 * invoked directly; please see {@link com.google.common.collect.testing.ListTestSuiteBuilder}.
 *
 * @author Chris Povirk
 */
@SuppressWarnings("unchecked") // too many "unchecked generic array creations"
@GwtCompatible
@Ignore // Affects only Android test runner, which respects JUnit 4 annotations on JUnit 3 tests.
public class ListAddAllTester<E> extends AbstractListTester<E> {
  @CollectionFeature.Require(SUPPORTS_ADD)
  @CollectionSize.Require(absent = ZERO)
  public void testAddAll_supportedAllPresent() {
    assertTrue(
        "addAll(allPresent) should return true", getList().addAll(MinimalCollection.of(e0())));
    expectAdded(e0());
  }

  @CollectionFeature.Require(absent = SUPPORTS_ADD)
  @CollectionSize.Require(absent = ZERO)
  public void testAddAll_unsupportedAllPresent() {
    try {
      getList().addAll(MinimalCollection.of(e0()));
      fail("addAll(allPresent) should throw");
    } catch (UnsupportedOperationException expected) {
    }
    expectUnchanged();
  }

  @CollectionFeature.Require(SUPPORTS_ADD)
  public void testAddAll_withDuplicates() {
    MinimalCollection<E> elementsToAdd = MinimalCollection.of(e0(), e1(), e0(), e1());
    assertTrue("addAll(hasDuplicates) should return true", getList().addAll(elementsToAdd));
    expectAdded(e0(), e1(), e0(), e1());
  }
}
