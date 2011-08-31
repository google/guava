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

import static com.google.common.collect.testing.features.CollectionFeature.SUPPORTS_CLEAR;
import static com.google.common.collect.testing.features.CollectionSize.ZERO;

import com.google.common.collect.testing.AbstractCollectionTester;
import com.google.common.collect.testing.features.CollectionFeature;
import com.google.common.collect.testing.features.CollectionSize;

/**
 * A generic JUnit test which tests {@code clear()} operations on a collection.
 * Can't be invoked directly; please see
 * {@link com.google.common.collect.testing.CollectionTestSuiteBuilder}.
 *
 * <p>This class is GWT compatible.
 *
 * @author George van den Driessche
 */
public class CollectionClearTester<E> extends AbstractCollectionTester<E> {
  @CollectionFeature.Require(SUPPORTS_CLEAR)
  public void testClear() {
    collection.clear();
    assertTrue("After clear(), a collection should be empty.",
        collection.isEmpty());
  }

  @CollectionFeature.Require(absent = SUPPORTS_CLEAR)
  @CollectionSize.Require(absent = ZERO)
  public void testClear_unsupported() {
    try {
      collection.clear();
      fail("clear() should throw UnsupportedOperation if a collection does "
          + "not support it and is not empty.");
    } catch (UnsupportedOperationException expected) {
    }
    expectUnchanged();
  }

  @CollectionFeature.Require(absent = SUPPORTS_CLEAR)
  @CollectionSize.Require(ZERO)
  public void testClear_unsupportedByEmptyCollection() {
    try {
      collection.clear();
    } catch (UnsupportedOperationException tolerated) {
    }
    expectUnchanged();
  }
}
