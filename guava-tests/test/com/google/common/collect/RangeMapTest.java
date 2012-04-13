/*
 * Copyright (C) 2011 The Guava Authors
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

import static com.google.common.collect.BoundType.OPEN;

import junit.framework.TestCase;

import java.util.Map;

/**
 * Tests for {@code RangeMap}.
 *
 * @author Louis Wasserman
 */
public class RangeMapTest extends TestCase {
  private static final ImmutableList<Range<Integer>> RANGES;
  private static final int MIN_BOUND = -1;
  private static final int MAX_BOUND = 1;
  static {
    ImmutableList.Builder<Range<Integer>> builder = ImmutableList.builder();

    builder.add(Ranges.<Integer>all());

    // Add one-ended ranges
    for (int i = MIN_BOUND; i <= MAX_BOUND; i++) {
      for (BoundType type : BoundType.values()) {
        builder.add(Ranges.upTo(i, type));
        builder.add(Ranges.downTo(i, type));
      }
    }

    // Add two-ended ranges

    for (int i = MIN_BOUND; i <= MAX_BOUND; i++) {
      for (int j = i; j <= MAX_BOUND; j++) {
        for (BoundType lowerType : BoundType.values()) {
          for (BoundType upperType : BoundType.values()) {
            if (i == j & lowerType == OPEN & upperType == OPEN) {
              continue;
            }
            builder.add(Ranges.range(i, lowerType, j, upperType));
          }
        }
      }
    }
    RANGES = builder.build();
  }

  public void testAllRangesAlone() {
    for (Range<Integer> range : RANGES) {
      Map<Integer, Integer> model = Maps.newHashMap();
      putModel(model, range, 1);
      RangeMap<Integer, Integer> test = RangeMap.create();
      test.put(range, 1);
      verify(model, test);
    }
  }

  public void testAllRangePairs() {
    for (Range<Integer> range1 : RANGES) {
      for (Range<Integer> range2 : RANGES) {
        Map<Integer, Integer> model = Maps.newHashMap();
        putModel(model, range1, 1);
        putModel(model, range2, 2);
        RangeMap<Integer, Integer> test = RangeMap.create();
        test.put(range1, 1);
        test.put(range2, 2);
        verify(model, test);
      }
    }
  }

  public void testAllRangeTriples() {
    for (Range<Integer> range1 : RANGES) {
      for (Range<Integer> range2 : RANGES) {
        for (Range<Integer> range3 : RANGES) {
          Map<Integer, Integer> model = Maps.newHashMap();
          putModel(model, range1, 1);
          putModel(model, range2, 2);
          putModel(model, range3, 3);
          RangeMap<Integer, Integer> test = RangeMap.create();
          test.put(range1, 1);
          test.put(range2, 2);
          test.put(range3, 3);
          verify(model, test);
        }
      }
    }
  }

  public void testPutAll1() {
    for (Range<Integer> range1 : RANGES) {
      for (Range<Integer> range2 : RANGES) {
        for (Range<Integer> range3 : RANGES) {
          Map<Integer, Integer> model = Maps.newHashMap();
          putModel(model, range1, 1);
          putModel(model, range2, 2);
          putModel(model, range3, 3);
          RangeMap<Integer, Integer> test = RangeMap.create();
          RangeMap<Integer, Integer> test2 = RangeMap.create();
          // put range2 and range3 into test2, and then put test2 into test
          test.put(range1, 1);
          test2.put(range2, 2);
          test2.put(range3, 3);
          test.putAll(test2);
          verify(model, test);
        }
      }
    }
  }

  public void testPutAll2() {
    for (Range<Integer> range1 : RANGES) {
      for (Range<Integer> range2 : RANGES) {
        for (Range<Integer> range3 : RANGES) {
          Map<Integer, Integer> model = Maps.newHashMap();
          putModel(model, range1, 1);
          putModel(model, range2, 2);
          putModel(model, range3, 3);
          RangeMap<Integer, Integer> test = RangeMap.create();
          RangeMap<Integer, Integer> test2 = RangeMap.create();
          // put range3 into test2, and then put test2 into test
          test.put(range1, 1);
          test.put(range2, 2);
          test2.put(range3, 3);
          test.putAll(test2);
          verify(model, test);
        }
      }
    }
  }

  private void verify(Map<Integer, Integer> model, RangeMap<Integer, Integer> test) {
    for (int i = MIN_BOUND - 1; i <= MAX_BOUND + 1; i++) {
      assertEquals(model.get(i), test.get(i));
    }
  }

  private void putModel(Map<Integer, Integer> model, Range<Integer> range, int value) {
    for (int i = MIN_BOUND - 1; i <= MAX_BOUND + 1; i++) {
      if (range.contains(i)) {
        model.put(i, value);
      }
    }
  }
}
