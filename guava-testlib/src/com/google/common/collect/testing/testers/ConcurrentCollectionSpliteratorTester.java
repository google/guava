/*
 * Copyright (C) 2025 The Guava Authors
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

import com.google.common.annotations.GwtCompatible;
import com.google.common.collect.testing.AbstractCollectionTester;
import java.util.Spliterator;
import org.jspecify.annotations.NullMarked;
import org.junit.Ignore;

/**
 * A generic JUnit test which tests that a collection's spliterator has the {@link
 * Spliterator#CONCURRENT} characteristic. This tester is intended for use with concurrent
 * collections such as views of {@link java.util.concurrent.ConcurrentMap}.
 *
 * <p>Can't be invoked directly; please see {@link
 * com.google.common.collect.testing.ConcurrentCollectionTestSuiteBuilder} or {@link
 * com.google.common.collect.testing.ConcurrentSetTestSuiteBuilder}.
 *
 * @author Nickita Khylkouski
 */
@GwtCompatible
@Ignore("test runners must not instantiate and run this directly, only via suites we build")
// @Ignore affects the Android test runner, which respects JUnit 4 annotations on JUnit 3 tests.
@SuppressWarnings("JUnit4ClassUsedInJUnit3")
@NullMarked
public class ConcurrentCollectionSpliteratorTester<E> extends AbstractCollectionTester<E> {

  /**
   * Tests that the spliterator returned by {@code collection.spliterator()} has the {@code
   * CONCURRENT} characteristic.
   */
  public void testSpliteratorHasConcurrent() {
    Spliterator<E> spliterator = collection.spliterator();
    assertTrue(
        "spliterator() should have CONCURRENT characteristic",
        spliterator.hasCharacteristics(Spliterator.CONCURRENT));
  }
}
