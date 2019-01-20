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
import static com.google.common.collect.testing.features.CollectionFeature.SUPPORTS_ADD;

import com.google.common.annotations.GwtCompatible;
import com.google.common.collect.testing.features.CollectionFeature;
import org.junit.Ignore;

/**
 * A generic JUnit test which tests offer operations on a queue. Can't be invoked directly; please
 * see {@link com.google.common.collect.testing.CollectionTestSuiteBuilder}.
 *
 * @author Jared Levy
 */
@SuppressWarnings("unchecked") // too many "unchecked generic array creations"
@GwtCompatible
@Ignore // Affects only Android test runner, which respects JUnit 4 annotations on JUnit 3 tests.
public class QueueOfferTester<E> extends AbstractQueueTester<E> {
  @CollectionFeature.Require(SUPPORTS_ADD)
  public void testOffer_supportedNotPresent() {
    assertTrue("offer(notPresent) should return true", getQueue().offer(e3()));
    expectAdded(e3());
  }

  @CollectionFeature.Require({SUPPORTS_ADD, ALLOWS_NULL_VALUES})
  public void testOffer_nullSupported() {
    assertTrue("offer(null) should return true", getQueue().offer(null));
    expectAdded((E) null);
  }

  @CollectionFeature.Require(value = SUPPORTS_ADD, absent = ALLOWS_NULL_VALUES)
  public void testOffer_nullUnsupported() {
    try {
      getQueue().offer(null);
      fail("offer(null) should throw");
    } catch (NullPointerException expected) {
    }
    expectUnchanged();
    expectNullMissingWhenNullUnsupported("Should not contain null after unsupported offer(null)");
  }
}
