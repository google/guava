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

import static com.google.common.collect.testing.features.CollectionFeature.SUPPORTS_ADD;
import static com.google.common.collect.testing.features.CollectionSize.SEVERAL;
import static com.google.common.collect.testing.features.CollectionSize.ZERO;
import static java.util.Collections.nCopies;

import com.google.common.annotations.GwtCompatible;
import com.google.common.collect.testing.features.CollectionFeature;
import com.google.common.collect.testing.features.CollectionSize;
import org.junit.Ignore;

/**
 * A generic JUnit test which tests conditional {@code setCount()} operations on a multiset. Can't
 * be invoked directly; please see {@link MultisetTestSuiteBuilder}.
 *
 * @author Chris Povirk
 */
@GwtCompatible
@Ignore // Affects only Android test runner, which respects JUnit 4 annotations on JUnit 3 tests.
public class MultisetSetCountConditionallyTester<E> extends AbstractMultisetSetCountTester<E> {
  @Override
  void setCountCheckReturnValue(E element, int count) {
    assertTrue(
        "setCount() with the correct expected present count should return true",
        setCount(element, count));
  }

  @Override
  void setCountNoCheckReturnValue(E element, int count) {
    setCount(element, count);
  }

  private boolean setCount(E element, int count) {
    return getMultiset().setCount(element, getMultiset().count(element), count);
  }

  private void assertSetCountNegativeOldCount() {
    try {
      getMultiset().setCount(e3(), -1, 1);
      fail("calling setCount() with a negative oldCount should throw IllegalArgumentException");
    } catch (IllegalArgumentException expected) {
    }
  }

  // Negative oldCount.

  @CollectionFeature.Require(SUPPORTS_ADD)
  public void testSetCountConditional_negativeOldCount_addSupported() {
    assertSetCountNegativeOldCount();
  }

  @CollectionFeature.Require(absent = SUPPORTS_ADD)
  public void testSetCountConditional_negativeOldCount_addUnsupported() {
    try {
      assertSetCountNegativeOldCount();
    } catch (UnsupportedOperationException tolerated) {
    }
  }

  // Incorrect expected present count.

  @CollectionFeature.Require(SUPPORTS_ADD)
  public void testSetCountConditional_oldCountTooLarge() {
    assertFalse(
        "setCount() with a too-large oldCount should return false",
        getMultiset().setCount(e0(), 2, 3));
    expectUnchanged();
  }

  @CollectionSize.Require(absent = ZERO)
  @CollectionFeature.Require(SUPPORTS_ADD)
  public void testSetCountConditional_oldCountTooSmallZero() {
    assertFalse(
        "setCount() with a too-small oldCount should return false",
        getMultiset().setCount(e0(), 0, 2));
    expectUnchanged();
  }

  @CollectionSize.Require(SEVERAL)
  @CollectionFeature.Require(SUPPORTS_ADD)
  public void testSetCountConditional_oldCountTooSmallNonzero() {
    initThreeCopies();
    assertFalse(
        "setCount() with a too-small oldCount should return false",
        getMultiset().setCount(e0(), 1, 5));
    expectContents(nCopies(3, e0()));
  }

  /*
   * TODO: test that unmodifiable multisets either throw UOE or return false
   * when both are valid options. Currently we test the UOE cases and the
   * return-false cases but not their intersection
   */
}
