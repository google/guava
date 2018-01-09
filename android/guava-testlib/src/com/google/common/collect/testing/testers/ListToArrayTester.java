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

import static com.google.common.collect.testing.features.CollectionSize.ZERO;

import com.google.common.annotations.GwtCompatible;
import com.google.common.collect.testing.features.CollectionSize;
import java.util.Arrays;
import org.junit.Ignore;

/**
 * A generic JUnit test which tests {@code toArray()} operations on a list. Can't be invoked
 * directly; please see {@link com.google.common.collect.testing.ListTestSuiteBuilder}.
 *
 * @author Chris Povirk
 */
@GwtCompatible
@Ignore // Affects only Android test runner, which respects JUnit 4 annotations on JUnit 3 tests.
public class ListToArrayTester<E> extends AbstractListTester<E> {
  // CollectionToArrayTester tests everything except ordering.

  public void testToArray_noArg() {
    Object[] actual = getList().toArray();
    assertArrayEquals("toArray() order should match list", createOrderedArray(), actual);
  }

  @CollectionSize.Require(absent = ZERO)
  public void testToArray_tooSmall() {
    Object[] actual = getList().toArray(new Object[0]);
    assertArrayEquals("toArray(tooSmall) order should match list", createOrderedArray(), actual);
  }

  public void testToArray_largeEnough() {
    Object[] actual = getList().toArray(new Object[getNumElements()]);
    assertArrayEquals("toArray(largeEnough) order should match list", createOrderedArray(), actual);
  }

  private static void assertArrayEquals(String message, Object[] expected, Object[] actual) {
    assertEquals(message, Arrays.asList(expected), Arrays.asList(actual));
  }
}
