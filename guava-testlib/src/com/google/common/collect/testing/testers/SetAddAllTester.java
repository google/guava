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

/**
 * A generic JUnit test which tests addAll operations on a set. Can't be
 * invoked directly; please see
 * {@link com.google.common.collect.testing.SetTestSuiteBuilder}.
 *
 * @author Kevin Bourrillion
 */
@SuppressWarnings("unchecked") // too many "unchecked generic array creations"
@GwtCompatible
public class SetAddAllTester<E> extends AbstractSetTester<E> {
  @CollectionFeature.Require(SUPPORTS_ADD)
  @CollectionSize.Require(absent = ZERO)
  public void testAddAll_supportedSomePresent() {
    assertTrue("add(somePresent) should return true",
        getSet().addAll(MinimalCollection.of(samples.e3, samples.e0)));
    expectAdded(samples.e3);
  }

  @CollectionFeature.Require(SUPPORTS_ADD)
  public void testAddAll_withDuplicates() {
    MinimalCollection<E> elementsToAdd
        = MinimalCollection.of(samples.e3, samples.e4, samples.e3, samples.e4);
    assertTrue("add(hasDuplicates) should return true",
        getSet().addAll(elementsToAdd));
    expectAdded(samples.e3, samples.e4);
  }

  @CollectionFeature.Require(SUPPORTS_ADD)
  @CollectionSize.Require(absent = ZERO)
  public void testAddAll_supportedAllPresent() {
    assertFalse("add(allPresent) should return false",
        getSet().addAll(MinimalCollection.of(samples.e0)));
    expectUnchanged();
  }
}
