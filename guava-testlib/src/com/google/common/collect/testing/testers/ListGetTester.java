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

import com.google.common.annotations.GwtCompatible;
import org.junit.Ignore;

/**
 * A generic JUnit test which tests {@code get()} operations on a list. Can't be invoked directly;
 * please see {@link com.google.common.collect.testing.ListTestSuiteBuilder}.
 *
 * @author Chris Povirk
 */
@GwtCompatible
@Ignore // Affects only Android test runner, which respects JUnit 4 annotations on JUnit 3 tests.
public class ListGetTester<E> extends AbstractListTester<E> {
  public void testGet_valid() {
    // This calls get() on each index and checks the result:
    expectContents(createOrderedArray());
  }

  public void testGet_negative() {
    try {
      getList().get(-1);
      fail("get(-1) should throw");
    } catch (IndexOutOfBoundsException expected) {
    }
  }

  public void testGet_tooLarge() {
    try {
      getList().get(getNumElements());
      fail("get(size) should throw");
    } catch (IndexOutOfBoundsException expected) {
    }
  }
}
