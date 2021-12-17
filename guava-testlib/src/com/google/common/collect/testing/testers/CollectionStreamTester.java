/*
 * Copyright (C) 2013 The Guava Authors
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

import static com.google.common.collect.testing.features.CollectionFeature.KNOWN_ORDER;

import com.google.common.annotations.GwtCompatible;
import com.google.common.collect.testing.AbstractCollectionTester;
import com.google.common.collect.testing.Helpers;
import com.google.common.collect.testing.features.CollectionFeature;
import java.util.Arrays;
import org.junit.Ignore;

/**
 * A generic JUnit test which tests {@code stream} operations on a collection. Can't be invoked
 * directly; please see {@link com.google.common.collect.testing.CollectionTestSuiteBuilder}.
 *
 * @author Louis Wasserman
 */
@GwtCompatible
@Ignore // Affects only Android test runner, which respects JUnit 4 annotations on JUnit 3 tests.
public class CollectionStreamTester<E> extends AbstractCollectionTester<E> {
  /*
   * We're not really testing the implementation of Stream, only that we're getting a Stream
   * that corresponds to the expected elements.
   */

  @CollectionFeature.Require(absent = KNOWN_ORDER)
  public void testStreamToArrayUnknownOrder() {
    synchronized (collection) { // to allow Collections.synchronized* tests to pass
      Helpers.assertEqualIgnoringOrder(
          getSampleElements(), Arrays.asList(collection.stream().toArray()));
    }
  }

  @CollectionFeature.Require(KNOWN_ORDER)
  public void testStreamToArrayKnownOrder() {
    synchronized (collection) { // to allow Collections.synchronized* tests to pass
      assertEquals(getOrderedElements(), Arrays.asList(collection.stream().toArray()));
    }
  }

  public void testStreamCount() {
    synchronized (collection) { // to allow Collections.synchronized* tests to pass
      assertEquals(getNumElements(), collection.stream().count());
    }
  }
}
