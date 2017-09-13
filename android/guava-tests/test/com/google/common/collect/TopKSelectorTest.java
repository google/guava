/*
 * Copyright (C) 2014 The Guava Authors
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

import com.google.common.math.IntMath;
import com.google.common.primitives.Ints;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import junit.framework.TestCase;

/**
 * Tests for {@code TopKSelector}.
 *
 * @author Louis Wasserman
 */
public class TopKSelectorTest extends TestCase {

  public void testNegativeK() {
    try {
      TopKSelector.least(-1);
      fail();
    } catch (IllegalArgumentException expected) {
    }
    try {
      TopKSelector.greatest(-1);
      fail();
    } catch (IllegalArgumentException expected) {
    }
    try {
      TopKSelector.least(-1, Ordering.natural());
      fail();
    } catch (IllegalArgumentException expected) {
    }
    try {
      TopKSelector.greatest(-1, Ordering.natural());
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }

  public void testZeroK() {
    TopKSelector<Integer> top = TopKSelector.least(0);
    for (int i = 0; i < 10; i++) {
      top.offer(i);
    }
    assertThat(top.topK()).isEmpty();
  }

  public void testNoElementsOffered() {
    TopKSelector<Integer> top = TopKSelector.least(10);
    assertThat(top.topK()).isEmpty();
  }

  public void testOfferedFewerThanK() {
    TopKSelector<Integer> top = TopKSelector.least(10);
    top.offer(3);
    top.offer(5);
    top.offer(2);
    assertThat(top.topK()).containsExactly(2, 3, 5).inOrder();
  }

  public void testOfferedKPlusOne() {
    for (List<Integer> list : Collections2.permutations(Ints.asList(1, 2, 3, 4, 5))) {
      TopKSelector<Integer> top = TopKSelector.least(4);
      top.offerAll(list);
      assertThat(top.topK()).containsExactly(1, 2, 3, 4).inOrder();
    }
  }

  public void testOfferedThreeK() {
    for (List<Integer> list : Collections2.permutations(Ints.asList(1, 2, 3, 4, 5, 6))) {
      TopKSelector<Integer> top = TopKSelector.least(2);
      top.offerAll(list);
      assertThat(top.topK()).containsExactly(1, 2).inOrder();
    }
  }

  public void testDifferentComparator() {
    TopKSelector<String> top = TopKSelector.least(3, String.CASE_INSENSITIVE_ORDER);
    top.offerAll(ImmutableList.of("a", "B", "c", "D", "e", "F"));
    assertThat(top.topK()).containsExactly("a", "B", "c").inOrder();
  }

  public void testWorstCase() {
    int n = 2000000;
    int k = 200000;
    final long[] compareCalls = {0};
    Comparator<Integer> cmp =
        new Comparator<Integer>() {
          @Override
          public int compare(Integer o1, Integer o2) {
            compareCalls[0]++;
            return o1.compareTo(o2);
          }
        };
    TopKSelector<Integer> top = TopKSelector.least(k, cmp);
    top.offer(1);
    for (int i = 1; i < n; i++) {
      top.offer(0);
    }
    assertThat(top.topK()).containsExactlyElementsIn(Collections.nCopies(k, 0));
    assertThat(compareCalls[0]).isAtMost(10L * n * IntMath.log2(k, RoundingMode.CEILING));
  }
}
