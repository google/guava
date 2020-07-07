/*
 * Copyright (C) 2009 The Guava Authors
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

package com.google.common.collect.testing.google;

import com.google.common.annotations.GwtCompatible;
import org.junit.Ignore;

/**
 * A generic JUnit test which tests unconditional {@code setCount()} operations on a multiset. Can't
 * be invoked directly; please see {@link MultisetTestSuiteBuilder}.
 *
 * @author Chris Povirk
 */
@GwtCompatible
@Ignore // Affects only Android test runner, which respects JUnit 4 annotations on JUnit 3 tests.
public class MultisetSetCountUnconditionallyTester<E> extends AbstractMultisetSetCountTester<E> {
  @Override
  void setCountCheckReturnValue(E element, int count) {
    assertEquals(
        "multiset.setCount() should return the old count",
        getMultiset().count(element),
        setCount(element, count));
  }

  @Override
  void setCountNoCheckReturnValue(E element, int count) {
    setCount(element, count);
  }

  private int setCount(E element, int count) {
    return getMultiset().setCount(element, count);
  }
}
