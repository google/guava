/*
 * Copyright (C) 2012 The Guava Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.common.collect;

import static com.google.common.collect.BoundType.OPEN;

import com.google.common.annotations.GwtIncompatible;
import com.google.common.testing.CollectorTester;
import com.google.common.testing.SerializableTester;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import junit.framework.TestCase;

/**
 * Tests for {@code ImmutableRangeMap}.
 *
 * @author Louis Wasserman
 */
@GwtIncompatible // NavigableMap
public class ImmutableRangeMapTest extends TestCase {
  private static final ImmutableList<Range<Integer>> RANGES;
  private static final int MIN_BOUND = 0;
  private static final int MAX_BOUND = 10;

  static {
    ImmutableList.Builder<Range<Integer>> builder = ImmutableList.builder();

    builder.add(Range.<Integer>all());

    // Add one-ended ranges
    for (int i = MIN_BOUND; i <= MAX_BOUND; i++) {
      for (BoundType type : BoundType.values()) {
        builder.add(Range.upTo(i, type));
        builder.add(Range.downTo(i, type));
      }
    }

    // Add two-ended ranges
    for (int i = MIN_BOUND; i <= MAX_BOUND; i++) {
      for (int j = i + 1; j <= MAX_BOUND; j++) {
        for (BoundType lowerType : BoundType.values()) {
          for (BoundType upperType : BoundType.values()) {
            if (i == j & lowerType == OPEN & upperType == OPEN) {
              continue;
            }
            builder.add(Range.range(i, lowerType, j, upperType));
          }
        }
      }
    }
    RANGES = builder.build();
  }

  public void testBuilderRejectsEmptyRanges() {
    for (int i = MIN_BOUND; i <= MAX_BOUND; i++) {
      ImmutableRangeMap.Builder<Integer, Integer> builder = ImmutableRangeMap.builder();
      try {
        builder.put(Range.closedOpen(i, i), 1);
        fail("Expected IllegalArgumentException");
      } catch (IllegalArgumentException expected) {
        // success
      }
      try {
        builder.put(Range.openClosed(i, i), 1);
        fail("Expected IllegalArgumentException");
      } catch (IllegalArgumentException expected) {
      }
    }
  }

  public void testOverlapRejection() {
    for (Range<Integer> range1 : RANGES) {
      for (Range<Integer> range2 : RANGES) {
        boolean expectRejection =
            range1.isConnected(range2) && !range1.intersection(range2).isEmpty();
        ImmutableRangeMap.Builder<Integer, Integer> builder = ImmutableRangeMap.builder();
        builder.put(range1, 1).put(range2, 2);
        try {
          ImmutableRangeMap<Integer, Integer> unused = builder.build();
          assertFalse(expectRejection);
        } catch (IllegalArgumentException e) {
          assertTrue(expectRejection);
        }
      }
    }
  }

  public void testGet() {
    for (Range<Integer> range1 : RANGES) {
      for (Range<Integer> range2 : RANGES) {
        if (!range1.isConnected(range2) || range1.intersection(range2).isEmpty()) {
          ImmutableRangeMap<Integer, Integer> rangeMap =
              ImmutableRangeMap.<Integer, Integer>builder().put(range1, 1).put(range2, 2).build();

          for (int i = MIN_BOUND; i <= MAX_BOUND; i++) {
            Integer expectedValue = null;
            if (range1.contains(i)) {
              expectedValue = 1;
            } else if (range2.contains(i)) {
              expectedValue = 2;
            }

            assertEquals(expectedValue, rangeMap.get(i));
          }
        }
      }
    }
  }

  public void testSpanEmpty() {
    try {
      ImmutableRangeMap.of().span();
      fail("Expected NoSuchElementException");
    } catch (NoSuchElementException expected) {
    }
  }

  public void testSpanSingleRange() {
    for (Range<Integer> range : RANGES) {
      RangeMap<Integer, Integer> rangemap =
          ImmutableRangeMap.<Integer, Integer>builder().put(range, 1).build();
      assertEquals(range, rangemap.span());
    }
  }

  public void testSpanTwoRanges() {
    for (Range<Integer> range1 : RANGES) {
      for (Range<Integer> range2 : RANGES) {
        if (!range1.isConnected(range2) || range1.intersection(range2).isEmpty()) {
          RangeMap<Integer, Integer> rangemap =
              ImmutableRangeMap.<Integer, Integer>builder().put(range1, 1).put(range2, 2).build();
          assertEquals(range1.span(range2), rangemap.span());
        }
      }
    }
  }

  public void testGetEntry() {
    for (Range<Integer> range1 : RANGES) {
      for (Range<Integer> range2 : RANGES) {
        if (!range1.isConnected(range2) || range1.intersection(range2).isEmpty()) {
          ImmutableRangeMap<Integer, Integer> rangeMap =
              ImmutableRangeMap.<Integer, Integer>builder().put(range1, 1).put(range2, 2).build();

          for (int i = MIN_BOUND; i <= MAX_BOUND; i++) {
            Entry<Range<Integer>, Integer> expectedEntry = null;
            if (range1.contains(i)) {
              expectedEntry = Maps.immutableEntry(range1, 1);
            } else if (range2.contains(i)) {
              expectedEntry = Maps.immutableEntry(range2, 2);
            }

            assertEquals(expectedEntry, rangeMap.getEntry(i));
          }
        }
      }
    }
  }

  public void testGetLargeRangeMap() {
    ImmutableRangeMap.Builder<Integer, Integer> builder = ImmutableRangeMap.builder();
    for (int i = 0; i < 100; i++) {
      builder.put(Range.closedOpen(i, i + 1), i);
    }
    ImmutableRangeMap<Integer, Integer> map = builder.build();
    for (int i = 0; i < 100; i++) {
      assertEquals(Integer.valueOf(i), map.get(i));
    }
  }

  @AndroidIncompatible // slow
  public void testAsMapOfRanges() {
    for (Range<Integer> range1 : RANGES) {
      for (Range<Integer> range2 : RANGES) {
        if (!range1.isConnected(range2) || range1.intersection(range2).isEmpty()) {
          ImmutableRangeMap<Integer, Integer> rangeMap =
              ImmutableRangeMap.<Integer, Integer>builder().put(range1, 1).put(range2, 2).build();

          ImmutableMap<Range<Integer>, Integer> expectedAsMap =
              ImmutableMap.of(range1, 1, range2, 2);
          ImmutableMap<Range<Integer>, Integer> asMap = rangeMap.asMapOfRanges();
          ImmutableMap<Range<Integer>, Integer> descendingMap = rangeMap.asDescendingMapOfRanges();
          assertEquals(expectedAsMap, asMap);
          assertEquals(expectedAsMap, descendingMap);
          SerializableTester.reserializeAndAssert(asMap);
          SerializableTester.reserializeAndAssert(descendingMap);
          assertEquals(
              ImmutableList.copyOf(asMap.entrySet()).reverse(),
              ImmutableList.copyOf(descendingMap.entrySet()));

          for (Range<Integer> query : RANGES) {
            assertEquals(expectedAsMap.get(query), asMap.get(query));
          }
        }
      }
    }
  }


  public void testSubRangeMap() {
    for (Range<Integer> range1 : RANGES) {
      for (Range<Integer> range2 : RANGES) {
        if (!range1.isConnected(range2) || range1.intersection(range2).isEmpty()) {
          for (Range<Integer> subRange : RANGES) {
            ImmutableRangeMap<Integer, Integer> rangeMap =
                ImmutableRangeMap.<Integer, Integer>builder().put(range1, 1).put(range2, 2).build();

            ImmutableRangeMap.Builder<Integer, Integer> expectedBuilder =
                ImmutableRangeMap.builder();
            for (Entry<Range<Integer>, Integer> entry : rangeMap.asMapOfRanges().entrySet()) {
              if (entry.getKey().isConnected(subRange)
                  && !entry.getKey().intersection(subRange).isEmpty()) {
                expectedBuilder.put(entry.getKey().intersection(subRange), entry.getValue());
              }
            }

            ImmutableRangeMap<Integer, Integer> expected = expectedBuilder.build();
            assertEquals(expected, rangeMap.subRangeMap(subRange));
          }
        }
      }
    }
  }

  public void testSerialization() {
    ImmutableRangeMap<Integer, Integer> emptyRangeMap = ImmutableRangeMap.of();
    SerializableTester.reserializeAndAssert(emptyRangeMap);

    ImmutableRangeMap<Integer, Integer> nonEmptyRangeMap =
        new ImmutableRangeMap.Builder<Integer, Integer>()
            .put(Range.closed(2, 4), 5)
            .put(Range.open(6, 7), 3)
            .put(Range.closedOpen(8, 10), 4)
            .put(Range.openClosed(15, 17), 2)
            .build();

    ImmutableMap<Range<Integer>, Integer> test = nonEmptyRangeMap.asMapOfRanges();

    for (Range<Integer> range : test.keySet()) {
      SerializableTester.reserializeAndAssert(range);
    }

    SerializableTester.reserializeAndAssert(test.keySet());

    SerializableTester.reserializeAndAssert(nonEmptyRangeMap);
  }

  public void testToImmutableRangeMap() {
    Range<Integer> rangeOne = Range.closedOpen(1, 5);
    Range<Integer> rangeTwo = Range.openClosed(6, 7);
    ImmutableRangeMap<Integer, Integer> rangeMap =
        new ImmutableRangeMap.Builder<Integer, Integer>().put(rangeOne, 1).put(rangeTwo, 6).build();
    CollectorTester.of(
            ImmutableRangeMap.<Range<Integer>, Integer, Integer>toImmutableRangeMap(
                k -> k, k -> k.lowerEndpoint()))
        .expectCollects(rangeMap, rangeOne, rangeTwo);
  }
}
