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

package com.google.common.collect;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.annotations.GwtCompatible;
import junit.framework.AssertionFailedError;
import junit.framework.TestCase;

/**
 * Unit test for {@link ComparisonChain}.
 *
 * @author Kevin Bourrillion
 */
@GwtCompatible
public class ComparisonChainTest extends TestCase {
  private static final DontCompareMe DONT_COMPARE_ME = new DontCompareMe();

  private static class DontCompareMe implements Comparable<DontCompareMe> {
    @Override
    public int compareTo(DontCompareMe o) {
      throw new AssertionFailedError();
    }
  }

  @SuppressWarnings("deprecation")
  public void testCompareBooleans() {
    assertThat(
            ComparisonChain.start()
                .compare(true, true)
                .compare(true, Boolean.TRUE)
                .compare(Boolean.TRUE, true)
                .compare(Boolean.TRUE, Boolean.TRUE)
                .result())
        .isEqualTo(0);
  }

  public void testDegenerate() {
    // kinda bogus, but who cares?
    assertThat(ComparisonChain.start().result()).isEqualTo(0);
  }

  public void testOneEqual() {
    assertThat(ComparisonChain.start().compare("a", "a").result()).isEqualTo(0);
  }

  public void testOneEqualUsingComparator() {
    assertThat(ComparisonChain.start().compare("a", "A", String.CASE_INSENSITIVE_ORDER).result())
        .isEqualTo(0);
  }

  public void testManyEqual() {
    assertThat(
            ComparisonChain.start()
                .compare(1, 1)
                .compare(1L, 1L)
                .compareFalseFirst(true, true)
                .compare(1.0, 1.0)
                .compare(1.0f, 1.0f)
                .compare("a", "a", Ordering.usingToString())
                .result())
        .isEqualTo(0);
  }

  public void testShortCircuitLess() {
    assertThat(
            ComparisonChain.start()
                .compare("a", "b")
                .compare(DONT_COMPARE_ME, DONT_COMPARE_ME)
                .result())
        .isLessThan(0);
  }

  public void testShortCircuitGreater() {
    assertThat(
            ComparisonChain.start()
                .compare("b", "a")
                .compare(DONT_COMPARE_ME, DONT_COMPARE_ME)
                .result())
        .isGreaterThan(0);
  }

  public void testShortCircuitSecondStep() {
    assertThat(
            ComparisonChain.start()
                .compare("a", "a")
                .compare("a", "b")
                .compare(DONT_COMPARE_ME, DONT_COMPARE_ME)
                .result())
        .isLessThan(0);
  }

  public void testCompareFalseFirst() {
    assertThat(ComparisonChain.start().compareFalseFirst(true, true).result()).isEqualTo(0);
    assertThat(ComparisonChain.start().compareFalseFirst(true, false).result()).isGreaterThan(0);
    assertThat(ComparisonChain.start().compareFalseFirst(false, true).result()).isLessThan(0);
    assertThat(ComparisonChain.start().compareFalseFirst(false, false).result()).isEqualTo(0);
  }

  public void testCompareTrueFirst() {
    assertThat(ComparisonChain.start().compareTrueFirst(true, true).result()).isEqualTo(0);
    assertThat(ComparisonChain.start().compareTrueFirst(true, false).result()).isLessThan(0);
    assertThat(ComparisonChain.start().compareTrueFirst(false, true).result()).isGreaterThan(0);
    assertThat(ComparisonChain.start().compareTrueFirst(false, false).result()).isEqualTo(0);
  }
}
