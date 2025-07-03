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

import static com.google.common.collect.testing.features.CollectionFeature.KNOWN_ORDER;
import static com.google.common.collect.testing.features.CollectionSize.ONE;
import static com.google.common.collect.testing.features.CollectionSize.SEVERAL;
import static com.google.common.collect.testing.features.CollectionSize.ZERO;

import com.google.common.annotations.GwtCompatible;
import com.google.common.collect.testing.features.CollectionFeature;
import com.google.common.collect.testing.features.CollectionSize;
import org.junit.Ignore;

/**
 * A generic JUnit test which tests {@code peek()} operations on a queue. Can't be invoked directly;
 * please see {@link com.google.common.collect.testing.CollectionTestSuiteBuilder}.
 *
 * @author Jared Levy
 */
@GwtCompatible
@Ignore("test runners must not instantiate and run this directly, only via suites we build")
// @Ignore affects the Android test runner, which respects JUnit 4 annotations on JUnit 3 tests.
@SuppressWarnings("JUnit4ClassUsedInJUnit3")
public class QueuePeekTester<E> extends AbstractQueueTester<E> {
  @CollectionSize.Require(ZERO)
  public void testPeek_empty() {
    assertNull("emptyQueue.peek() should return null", getQueue().peek());
    expectUnchanged();
  }

  @CollectionSize.Require(ONE)
  public void testPeek_size1() {
    assertEquals("size1Queue.peek() should return first element", e0(), getQueue().peek());
    expectUnchanged();
  }

  @CollectionFeature.Require(KNOWN_ORDER)
  @CollectionSize.Require(SEVERAL)
  public void testPeek_sizeMany() {
    assertEquals("sizeManyQueue.peek() should return first element", e0(), getQueue().peek());
    expectUnchanged();
  }
}
