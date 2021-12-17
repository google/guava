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

  public void testCompareBooleans() {
    assertEquals(
        0,
        ComparisonChain.start()
            .compare(true, true)
            .compare(true, Boolean.TRUE)
            .compare(Boolean.TRUE, true)
            .compare(Boolean.TRUE, Boolean.TRUE)
            .result());
  }

  public void testDegenerate() {
    // kinda bogus, but who cares?
    assertEquals(0, ComparisonChain.start().result());
  }

  public void testOneEqual() {
    assertEquals(0, ComparisonChain.start().compare("a", "a").result());
  }

  public void testOneEqualUsingComparator() {
    assertEquals(
        0, ComparisonChain.start().compare("a", "A", String.CASE_INSENSITIVE_ORDER).result());
  }

  public void testManyEqual() {
    assertEquals(
        0,
        ComparisonChain.start()
            .compare(1, 1)
            .compare(1L, 1L)
            .compareFalseFirst(true, true)
            .compare(1.0, 1.0)
            .compare(1.0f, 1.0f)
            .compare("a", "a", Ordering.usingToString())
            .result());
  }

  public void testShortCircuitLess() {
    assertTrue(
        ComparisonChain.start().compare("a", "b").compare(DONT_COMPARE_ME, DONT_COMPARE_ME).result()
            < 0);
  }

  public void testShortCircuitGreater() {
    assertTrue(
        ComparisonChain.start().compare("b", "a").compare(DONT_COMPARE_ME, DONT_COMPARE_ME).result()
            > 0);
  }

  public void testShortCircuitSecondStep() {
    assertTrue(
        ComparisonChain.start()
                .compare("a", "a")
                .compare("a", "b")
                .compare(DONT_COMPARE_ME, DONT_COMPARE_ME)
                .result()
            < 0);
  }

  public void testCompareFalseFirst() {
    assertTrue(ComparisonChain.start().compareFalseFirst(true, true).result() == 0);
    assertTrue(ComparisonChain.start().compareFalseFirst(true, false).result() > 0);
    assertTrue(ComparisonChain.start().compareFalseFirst(false, true).result() < 0);
    assertTrue(ComparisonChain.start().compareFalseFirst(false, false).result() == 0);
  }

  public void testCompareTrueFirst() {
    assertTrue(ComparisonChain.start().compareTrueFirst(true, true).result() == 0);
    assertTrue(ComparisonChain.start().compareTrueFirst(true, false).result() < 0);
    assertTrue(ComparisonChain.start().compareTrueFirst(false, true).result() > 0);
    assertTrue(ComparisonChain.start().compareTrueFirst(false, false).result() == 0);
  }
}
