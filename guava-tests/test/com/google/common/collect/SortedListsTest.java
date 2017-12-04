/*
 * Copyright (C) 2010 The Guava Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.common.collect;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.collect.SortedLists.KeyAbsentBehavior;
import com.google.common.collect.SortedLists.KeyPresentBehavior;
import com.google.common.testing.NullPointerTester;
import java.util.List;
import junit.framework.TestCase;

/**
 * Tests for SortedLists.
 *
 * @author Louis Wasserman
 */
@GwtCompatible(emulated = true)
public class SortedListsTest extends TestCase {
  private static final ImmutableList<Integer> LIST_WITH_DUPS =
      ImmutableList.of(1, 1, 2, 4, 4, 4, 8);

  private static final ImmutableList<Integer> LIST_WITHOUT_DUPS = ImmutableList.of(1, 2, 4, 8);

  void assertModelAgrees(
      List<Integer> list,
      Integer key,
      int answer,
      KeyPresentBehavior presentBehavior,
      KeyAbsentBehavior absentBehavior) {
    switch (presentBehavior) {
      case FIRST_PRESENT:
        if (list.contains(key)) {
          assertEquals(list.indexOf(key), answer);
          return;
        }
        break;
      case LAST_PRESENT:
        if (list.contains(key)) {
          assertEquals(list.lastIndexOf(key), answer);
          return;
        }
        break;
      case ANY_PRESENT:
        if (list.contains(key)) {
          assertEquals(key, list.get(answer));
          return;
        }
        break;
      case FIRST_AFTER:
        if (list.contains(key)) {
          assertEquals(list.lastIndexOf(key) + 1, answer);
          return;
        }
        break;
      case LAST_BEFORE:
        if (list.contains(key)) {
          assertEquals(list.indexOf(key) - 1, answer);
          return;
        }
        break;
      default:
        throw new AssertionError();
    }
    // key is not present
    int nextHigherIndex = list.size();
    for (int i = list.size() - 1; i >= 0 && list.get(i) > key; i--) {
      nextHigherIndex = i;
    }
    switch (absentBehavior) {
      case NEXT_LOWER:
        assertEquals(nextHigherIndex - 1, answer);
        return;
      case NEXT_HIGHER:
        assertEquals(nextHigherIndex, answer);
        return;
      case INVERTED_INSERTION_INDEX:
        assertEquals(-1 - nextHigherIndex, answer);
        return;
      default:
        throw new AssertionError();
    }
  }

  public void testWithoutDups() {
    for (KeyPresentBehavior presentBehavior : KeyPresentBehavior.values()) {
      for (KeyAbsentBehavior absentBehavior : KeyAbsentBehavior.values()) {
        for (int key = 0; key <= 10; key++) {
          assertModelAgrees(
              LIST_WITHOUT_DUPS,
              key,
              SortedLists.binarySearch(LIST_WITHOUT_DUPS, key, presentBehavior, absentBehavior),
              presentBehavior,
              absentBehavior);
        }
      }
    }
  }

  public void testWithDups() {
    for (KeyPresentBehavior presentBehavior : KeyPresentBehavior.values()) {
      for (KeyAbsentBehavior absentBehavior : KeyAbsentBehavior.values()) {
        for (int key = 0; key <= 10; key++) {
          assertModelAgrees(
              LIST_WITH_DUPS,
              key,
              SortedLists.binarySearch(LIST_WITH_DUPS, key, presentBehavior, absentBehavior),
              presentBehavior,
              absentBehavior);
        }
      }
    }
  }

  @GwtIncompatible // NullPointerTester
  public void testNulls() {
    new NullPointerTester().testAllPublicStaticMethods(SortedLists.class);
  }
}
