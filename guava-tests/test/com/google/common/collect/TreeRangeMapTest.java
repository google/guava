/*
 * Copyright (C) 2011 The Guava Authors
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
import static com.google.common.collect.testing.Helpers.mapEntry;

import com.google.common.annotations.GwtIncompatible;
import com.google.common.collect.testing.MapTestSuiteBuilder;
import com.google.common.collect.testing.SampleElements;
import com.google.common.collect.testing.TestMapGenerator;
import com.google.common.collect.testing.features.CollectionFeature;
import com.google.common.collect.testing.features.CollectionSize;
import com.google.common.collect.testing.features.MapFeature;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Tests for {@code TreeRangeMap}.
 *
 * @author Louis Wasserman
 */
@GwtIncompatible // NavigableMap
public class TreeRangeMapTest extends TestCase {
  public static Test suite() {
    TestSuite suite = new TestSuite();
    suite.addTestSuite(TreeRangeMapTest.class);
    suite.addTest(
        MapTestSuiteBuilder.using(
                new TestMapGenerator<Range<Integer>, String>() {
                  @Override
                  public SampleElements<Entry<Range<Integer>, String>> samples() {
                    return new SampleElements<>(
                        mapEntry(Range.singleton(0), "banana"),
                        mapEntry(Range.closedOpen(3, 5), "frisbee"),
                        mapEntry(Range.atMost(-1), "fruitcake"),
                        mapEntry(Range.open(10, 15), "elephant"),
                        mapEntry(Range.closed(20, 22), "umbrella"));
                  }

                  @Override
                  public Map<Range<Integer>, String> create(Object... elements) {
                    RangeMap<Integer, String> rangeMap = TreeRangeMap.create();
                    for (Object o : elements) {
                      @SuppressWarnings("unchecked")
                      Entry<Range<Integer>, String> entry = (Entry<Range<Integer>, String>) o;
                      rangeMap.put(entry.getKey(), entry.getValue());
                    }
                    return rangeMap.asMapOfRanges();
                  }

                  @SuppressWarnings("unchecked")
                  @Override
                  public Entry<Range<Integer>, String>[] createArray(int length) {
                    return new Entry[length];
                  }

                  @Override
                  public Iterable<Entry<Range<Integer>, String>> order(
                      List<Entry<Range<Integer>, String>> insertionOrder) {
                    return Range.<Integer>rangeLexOrdering().onKeys().sortedCopy(insertionOrder);
                  }

                  @SuppressWarnings("unchecked")
                  @Override
                  public Range<Integer>[] createKeyArray(int length) {
                    return new Range[length];
                  }

                  @Override
                  public String[] createValueArray(int length) {
                    return new String[length];
                  }
                })
            .named("TreeRangeMap.asMapOfRanges")
            .withFeatures(
                CollectionSize.ANY,
                MapFeature.SUPPORTS_REMOVE,
                MapFeature.ALLOWS_ANY_NULL_QUERIES,
                CollectionFeature.KNOWN_ORDER,
                CollectionFeature.SUPPORTS_ITERATOR_REMOVE)
            .createTestSuite());

    suite.addTest(
        MapTestSuiteBuilder.using(
                new TestMapGenerator<Range<Integer>, String>() {
                  @Override
                  public SampleElements<Entry<Range<Integer>, String>> samples() {
                    return new SampleElements<>(
                        mapEntry(Range.singleton(0), "banana"),
                        mapEntry(Range.closedOpen(3, 5), "frisbee"),
                        mapEntry(Range.atMost(-1), "fruitcake"),
                        mapEntry(Range.open(10, 15), "elephant"),
                        mapEntry(Range.closed(20, 22), "umbrella"));
                  }

                  @Override
                  public Map<Range<Integer>, String> create(Object... elements) {
                    RangeMap<Integer, String> rangeMap = TreeRangeMap.create();
                    for (Object o : elements) {
                      @SuppressWarnings("unchecked")
                      Entry<Range<Integer>, String> entry = (Entry<Range<Integer>, String>) o;
                      rangeMap.put(entry.getKey(), entry.getValue());
                    }
                    return rangeMap.subRangeMap(Range.atMost(22)).asMapOfRanges();
                  }

                  @SuppressWarnings("unchecked")
                  @Override
                  public Entry<Range<Integer>, String>[] createArray(int length) {
                    return new Entry[length];
                  }

                  @Override
                  public Iterable<Entry<Range<Integer>, String>> order(
                      List<Entry<Range<Integer>, String>> insertionOrder) {
                    return Range.<Integer>rangeLexOrdering().onKeys().sortedCopy(insertionOrder);
                  }

                  @SuppressWarnings("unchecked")
                  @Override
                  public Range<Integer>[] createKeyArray(int length) {
                    return new Range[length];
                  }

                  @Override
                  public String[] createValueArray(int length) {
                    return new String[length];
                  }
                })
            .named("TreeRangeMap.subRangeMap.asMapOfRanges")
            .withFeatures(
                CollectionSize.ANY,
                MapFeature.SUPPORTS_REMOVE,
                MapFeature.ALLOWS_ANY_NULL_QUERIES,
                CollectionFeature.KNOWN_ORDER)
            .createTestSuite());

    suite.addTest(
        MapTestSuiteBuilder.using(
                new TestMapGenerator<Range<Integer>, String>() {
                  @Override
                  public SampleElements<Entry<Range<Integer>, String>> samples() {
                    return new SampleElements<>(
                        mapEntry(Range.singleton(0), "banana"),
                        mapEntry(Range.closedOpen(3, 5), "frisbee"),
                        mapEntry(Range.atMost(-1), "fruitcake"),
                        mapEntry(Range.open(10, 15), "elephant"),
                        mapEntry(Range.closed(20, 22), "umbrella"));
                  }

                  @Override
                  public Map<Range<Integer>, String> create(Object... elements) {
                    RangeMap<Integer, String> rangeMap = TreeRangeMap.create();
                    for (Object o : elements) {
                      @SuppressWarnings("unchecked")
                      Entry<Range<Integer>, String> entry = (Entry<Range<Integer>, String>) o;
                      rangeMap.put(entry.getKey(), entry.getValue());
                    }
                    return rangeMap.asDescendingMapOfRanges();
                  }

                  @SuppressWarnings("unchecked")
                  @Override
                  public Entry<Range<Integer>, String>[] createArray(int length) {
                    return new Entry[length];
                  }

                  @Override
                  public Iterable<Entry<Range<Integer>, String>> order(
                      List<Entry<Range<Integer>, String>> insertionOrder) {
                    return Range.<Integer>rangeLexOrdering()
                        .reverse()
                        .onKeys()
                        .sortedCopy(insertionOrder);
                  }

                  @SuppressWarnings("unchecked")
                  @Override
                  public Range<Integer>[] createKeyArray(int length) {
                    return new Range[length];
                  }

                  @Override
                  public String[] createValueArray(int length) {
                    return new String[length];
                  }
                })
            .named("TreeRangeMap.asDescendingMapOfRanges")
            .withFeatures(
                CollectionSize.ANY,
                MapFeature.SUPPORTS_REMOVE,
                MapFeature.ALLOWS_ANY_NULL_QUERIES,
                CollectionFeature.KNOWN_ORDER,
                CollectionFeature.SUPPORTS_ITERATOR_REMOVE)
            .createTestSuite());

    suite.addTest(
        MapTestSuiteBuilder.using(
                new TestMapGenerator<Range<Integer>, String>() {
                  @Override
                  public SampleElements<Entry<Range<Integer>, String>> samples() {
                    return new SampleElements<>(
                        mapEntry(Range.singleton(0), "banana"),
                        mapEntry(Range.closedOpen(3, 5), "frisbee"),
                        mapEntry(Range.atMost(-1), "fruitcake"),
                        mapEntry(Range.open(10, 15), "elephant"),
                        mapEntry(Range.closed(20, 22), "umbrella"));
                  }

                  @Override
                  public Map<Range<Integer>, String> create(Object... elements) {
                    RangeMap<Integer, String> rangeMap = TreeRangeMap.create();
                    for (Object o : elements) {
                      @SuppressWarnings("unchecked")
                      Entry<Range<Integer>, String> entry = (Entry<Range<Integer>, String>) o;
                      rangeMap.put(entry.getKey(), entry.getValue());
                    }
                    return rangeMap.subRangeMap(Range.atMost(22)).asDescendingMapOfRanges();
                  }

                  @SuppressWarnings("unchecked")
                  @Override
                  public Entry<Range<Integer>, String>[] createArray(int length) {
                    return new Entry[length];
                  }

                  @Override
                  public Iterable<Entry<Range<Integer>, String>> order(
                      List<Entry<Range<Integer>, String>> insertionOrder) {
                    return Range.<Integer>rangeLexOrdering()
                        .reverse()
                        .onKeys()
                        .sortedCopy(insertionOrder);
                  }

                  @SuppressWarnings("unchecked")
                  @Override
                  public Range<Integer>[] createKeyArray(int length) {
                    return new Range[length];
                  }

                  @Override
                  public String[] createValueArray(int length) {
                    return new String[length];
                  }
                })
            .named("TreeRangeMap.subRangeMap.asDescendingMapOfRanges")
            .withFeatures(
                CollectionSize.ANY,
                MapFeature.SUPPORTS_REMOVE,
                MapFeature.ALLOWS_ANY_NULL_QUERIES,
                CollectionFeature.KNOWN_ORDER)
            .createTestSuite());
    return suite;
  }

  private static final ImmutableList<Range<Integer>> RANGES;
  private static final int MIN_BOUND = -2;
  private static final int MAX_BOUND = 2;

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
      for (int j = i; j <= MAX_BOUND; j++) {
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

  public void testSpanSingleRange() {
    for (Range<Integer> range : RANGES) {
      RangeMap<Integer, Integer> rangeMap = TreeRangeMap.create();
      rangeMap.put(range, 1);

      try {
        assertEquals(range, rangeMap.span());
        assertFalse(range.isEmpty());
      } catch (NoSuchElementException e) {
        assertTrue(range.isEmpty());
      }
    }
  }

  public void testSpanTwoRanges() {
    for (Range<Integer> range1 : RANGES) {
      for (Range<Integer> range2 : RANGES) {
        RangeMap<Integer, Integer> rangeMap = TreeRangeMap.create();
        rangeMap.put(range1, 1);
        rangeMap.put(range2, 2);

        Range<Integer> expected;
        if (range1.isEmpty()) {
          if (range2.isEmpty()) {
            expected = null;
          } else {
            expected = range2;
          }
        } else {
          if (range2.isEmpty()) {
            expected = range1;
          } else {
            expected = range1.span(range2);
          }
        }

        try {
          assertEquals(expected, rangeMap.span());
          assertNotNull(expected);
        } catch (NoSuchElementException e) {
          assertNull(expected);
        }
      }
    }
  }

  public void testAllRangesAlone() {
    for (Range<Integer> range : RANGES) {
      Map<Integer, Integer> model = Maps.newHashMap();
      putModel(model, range, 1);
      RangeMap<Integer, Integer> test = TreeRangeMap.create();
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
        RangeMap<Integer, Integer> test = TreeRangeMap.create();
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
          RangeMap<Integer, Integer> test = TreeRangeMap.create();
          test.put(range1, 1);
          test.put(range2, 2);
          test.put(range3, 3);
          verify(model, test);
        }
      }
    }
  }

  public void testPutAll() {
    for (Range<Integer> range1 : RANGES) {
      for (Range<Integer> range2 : RANGES) {
        for (Range<Integer> range3 : RANGES) {
          Map<Integer, Integer> model = Maps.newHashMap();
          putModel(model, range1, 1);
          putModel(model, range2, 2);
          putModel(model, range3, 3);
          RangeMap<Integer, Integer> test = TreeRangeMap.create();
          RangeMap<Integer, Integer> test2 = TreeRangeMap.create();
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

  public void testPutAndRemove() {
    for (Range<Integer> rangeToPut : RANGES) {
      for (Range<Integer> rangeToRemove : RANGES) {
        Map<Integer, Integer> model = Maps.newHashMap();
        putModel(model, rangeToPut, 1);
        removeModel(model, rangeToRemove);
        RangeMap<Integer, Integer> test = TreeRangeMap.create();
        test.put(rangeToPut, 1);
        test.remove(rangeToRemove);
        verify(model, test);
      }
    }
  }

  public void testPutTwoAndRemove() {
    for (Range<Integer> rangeToPut1 : RANGES) {
      for (Range<Integer> rangeToPut2 : RANGES) {
        for (Range<Integer> rangeToRemove : RANGES) {
          Map<Integer, Integer> model = Maps.newHashMap();
          putModel(model, rangeToPut1, 1);
          putModel(model, rangeToPut2, 2);
          removeModel(model, rangeToRemove);
          RangeMap<Integer, Integer> test = TreeRangeMap.create();
          test.put(rangeToPut1, 1);
          test.put(rangeToPut2, 2);
          test.remove(rangeToRemove);
          verify(model, test);
        }
      }
    }
  }

  // identical to testPutTwoAndRemove,
  // verifies that putCoalescing() doesn't cause any mappings to change relative to put()
  public void testPutCoalescingTwoAndRemove() {
    for (Range<Integer> rangeToPut1 : RANGES) {
      for (Range<Integer> rangeToPut2 : RANGES) {
        for (Range<Integer> rangeToRemove : RANGES) {
          Map<Integer, Integer> model = Maps.newHashMap();
          putModel(model, rangeToPut1, 1);
          putModel(model, rangeToPut2, 2);
          removeModel(model, rangeToRemove);
          RangeMap<Integer, Integer> test = TreeRangeMap.create();
          test.putCoalescing(rangeToPut1, 1);
          test.putCoalescing(rangeToPut2, 2);
          test.remove(rangeToRemove);
          verify(model, test);
        }
      }
    }
  }

  public void testPutCoalescing() {
    // {[0..1): 1, [1..2): 1, [2..3): 2} -> {[0..2): 1, [2..3): 2}
    RangeMap<Integer, Integer> rangeMap = TreeRangeMap.create();
    rangeMap.putCoalescing(Range.closedOpen(0, 1), 1);
    rangeMap.putCoalescing(Range.closedOpen(1, 2), 1);
    rangeMap.putCoalescing(Range.closedOpen(2, 3), 2);
    assertEquals(
        ImmutableMap.of(Range.closedOpen(0, 2), 1, Range.closedOpen(2, 3), 2),
        rangeMap.asMapOfRanges());
  }

  public void testPutCoalescingEmpty() {
    RangeMap<Integer, Integer> rangeMap = TreeRangeMap.create();
    rangeMap.put(Range.closedOpen(0, 1), 1);
    rangeMap.put(Range.closedOpen(1, 2), 1);
    assertEquals(
        ImmutableMap.of(Range.closedOpen(0, 1), 1, Range.closedOpen(1, 2), 1),
        rangeMap.asMapOfRanges());

    rangeMap.putCoalescing(Range.closedOpen(1, 1), 1); // empty range coalesces connected ranges
    assertEquals(ImmutableMap.of(Range.closedOpen(0, 2), 1), rangeMap.asMapOfRanges());
  }

  public void testPutCoalescingComplex() {
    // {[0..1): 1, [1..3): 1, [3..5): 1, [7..10): 2, [12..15): 2, [18..19): 3}
    RangeMap<Integer, Integer> rangeMap = TreeRangeMap.create();
    rangeMap.put(Range.closedOpen(0, 1), 1);
    rangeMap.put(Range.closedOpen(1, 3), 1);
    rangeMap.put(Range.closedOpen(3, 5), 1);
    rangeMap.put(Range.closedOpen(7, 10), 2);
    rangeMap.put(Range.closedOpen(12, 15), 2);
    rangeMap.put(Range.closedOpen(18, 19), 3);

    rangeMap.putCoalescing(Range.closedOpen(-5, -4), 0); // disconnected
    rangeMap.putCoalescing(Range.closedOpen(-6, -5), 0); // lower than minimum

    rangeMap.putCoalescing(Range.closedOpen(2, 4), 1); // between
    rangeMap.putCoalescing(Range.closedOpen(9, 14), 0); // different value
    rangeMap.putCoalescing(Range.closedOpen(17, 20), 3); // enclosing

    rangeMap.putCoalescing(Range.closedOpen(22, 23), 4); // disconnected
    rangeMap.putCoalescing(Range.closedOpen(23, 25), 4); // greater than minimum

    // {[-6..-4): 0, [0..1): 1, [1..5): 1, [7..9): 2,
    //  [9..14): 0, [14..15): 2, [17..20): 3, [22..25): 4}
    assertEquals(
        new ImmutableMap.Builder<>()
            .put(Range.closedOpen(-6, -4), 0)
            .put(Range.closedOpen(0, 1), 1) // not coalesced
            .put(Range.closedOpen(1, 5), 1)
            .put(Range.closedOpen(7, 9), 2)
            .put(Range.closedOpen(9, 14), 0)
            .put(Range.closedOpen(14, 15), 2)
            .put(Range.closedOpen(17, 20), 3)
            .put(Range.closedOpen(22, 25), 4)
            .build(),
        rangeMap.asMapOfRanges());
  }

  public void testSubRangeMapExhaustive() {
    for (Range<Integer> range1 : RANGES) {
      for (Range<Integer> range2 : RANGES) {
        RangeMap<Integer, Integer> rangeMap = TreeRangeMap.create();
        rangeMap.put(range1, 1);
        rangeMap.put(range2, 2);

        for (Range<Integer> subRange : RANGES) {
          RangeMap<Integer, Integer> expected = TreeRangeMap.create();
          for (Entry<Range<Integer>, Integer> entry : rangeMap.asMapOfRanges().entrySet()) {
            if (entry.getKey().isConnected(subRange)) {
              expected.put(entry.getKey().intersection(subRange), entry.getValue());
            }
          }
          RangeMap<Integer, Integer> subRangeMap = rangeMap.subRangeMap(subRange);
          assertEquals(expected, subRangeMap);
          assertEquals(expected.asMapOfRanges(), subRangeMap.asMapOfRanges());
          assertEquals(expected.asDescendingMapOfRanges(), subRangeMap.asDescendingMapOfRanges());
          assertEquals(
              ImmutableList.copyOf(subRangeMap.asMapOfRanges().entrySet()).reverse(),
              ImmutableList.copyOf(subRangeMap.asDescendingMapOfRanges().entrySet()));

          if (!expected.asMapOfRanges().isEmpty()) {
            assertEquals(expected.span(), subRangeMap.span());
          }

          for (int i = MIN_BOUND; i <= MAX_BOUND; i++) {
            assertEquals(expected.get(i), subRangeMap.get(i));
          }

          for (Range<Integer> query : RANGES) {
            assertEquals(
                expected.asMapOfRanges().get(query), subRangeMap.asMapOfRanges().get(query));
          }
        }
      }
    }
  }

  public void testSubSubRangeMap() {
    RangeMap<Integer, Integer> rangeMap = TreeRangeMap.create();
    rangeMap.put(Range.open(3, 7), 1);
    rangeMap.put(Range.closed(9, 10), 2);
    rangeMap.put(Range.closed(12, 16), 3);
    RangeMap<Integer, Integer> sub1 = rangeMap.subRangeMap(Range.closed(5, 11));
    assertEquals(
        ImmutableMap.of(Range.closedOpen(5, 7), 1, Range.closed(9, 10), 2), sub1.asMapOfRanges());
    RangeMap<Integer, Integer> sub2 = sub1.subRangeMap(Range.open(6, 15));
    assertEquals(
        ImmutableMap.of(Range.open(6, 7), 1, Range.closed(9, 10), 2), sub2.asMapOfRanges());
  }

  public void testSubRangeMapPut() {
    RangeMap<Integer, Integer> rangeMap = TreeRangeMap.create();
    rangeMap.put(Range.open(3, 7), 1);
    rangeMap.put(Range.closed(9, 10), 2);
    rangeMap.put(Range.closed(12, 16), 3);
    RangeMap<Integer, Integer> sub = rangeMap.subRangeMap(Range.closed(5, 11));
    assertEquals(
        ImmutableMap.of(Range.closedOpen(5, 7), 1, Range.closed(9, 10), 2), sub.asMapOfRanges());
    sub.put(Range.closed(7, 9), 4);
    assertEquals(
        ImmutableMap.of(
            Range.closedOpen(5, 7), 1, Range.closed(7, 9), 4, Range.openClosed(9, 10), 2),
        sub.asMapOfRanges());
    assertEquals(
        ImmutableMap.of(
            Range.open(3, 7),
            1,
            Range.closed(7, 9),
            4,
            Range.openClosed(9, 10),
            2,
            Range.closed(12, 16),
            3),
        rangeMap.asMapOfRanges());

    try {
      sub.put(Range.open(9, 12), 5);
      fail("Expected IllegalArgumentException");
    } catch (IllegalArgumentException expected) {
    }

    sub = sub.subRangeMap(Range.closedOpen(5, 5));
    sub.put(Range.closedOpen(5, 5), 6); // should be a no-op
    assertEquals(
        ImmutableMap.of(
            Range.open(3, 7),
            1,
            Range.closed(7, 9),
            4,
            Range.openClosed(9, 10),
            2,
            Range.closed(12, 16),
            3),
        rangeMap.asMapOfRanges());
  }

  public void testSubRangeMapPutCoalescing() {
    RangeMap<Integer, Integer> rangeMap = TreeRangeMap.create();
    rangeMap.put(Range.open(3, 7), 1);
    rangeMap.put(Range.closed(9, 10), 2);
    rangeMap.put(Range.closed(12, 16), 3);
    RangeMap<Integer, Integer> sub = rangeMap.subRangeMap(Range.closed(5, 11));
    assertEquals(
        ImmutableMap.of(Range.closedOpen(5, 7), 1, Range.closed(9, 10), 2), sub.asMapOfRanges());
    sub.putCoalescing(Range.closed(7, 9), 2);
    assertEquals(
        ImmutableMap.of(Range.closedOpen(5, 7), 1, Range.closed(7, 10), 2), sub.asMapOfRanges());
    assertEquals(
        ImmutableMap.of(Range.open(3, 7), 1, Range.closed(7, 10), 2, Range.closed(12, 16), 3),
        rangeMap.asMapOfRanges());

    sub.putCoalescing(Range.singleton(7), 1);
    assertEquals(
        ImmutableMap.of(Range.closed(5, 7), 1, Range.openClosed(7, 10), 2), sub.asMapOfRanges());
    assertEquals(
        ImmutableMap.of(
            Range.open(3, 5),
            1,
            Range.closed(5, 7),
            1,
            Range.openClosed(7, 10),
            2,
            Range.closed(12, 16),
            3),
        rangeMap.asMapOfRanges());

    try {
      sub.putCoalescing(Range.open(9, 12), 5);
      fail("Expected IllegalArgumentException");
    } catch (IllegalArgumentException expected) {
    }
  }

  public void testSubRangeMapRemove() {
    RangeMap<Integer, Integer> rangeMap = TreeRangeMap.create();
    rangeMap.put(Range.open(3, 7), 1);
    rangeMap.put(Range.closed(9, 10), 2);
    rangeMap.put(Range.closed(12, 16), 3);
    RangeMap<Integer, Integer> sub = rangeMap.subRangeMap(Range.closed(5, 11));
    assertEquals(
        ImmutableMap.of(Range.closedOpen(5, 7), 1, Range.closed(9, 10), 2), sub.asMapOfRanges());
    sub.remove(Range.closed(7, 9));
    assertEquals(
        ImmutableMap.of(Range.closedOpen(5, 7), 1, Range.openClosed(9, 10), 2),
        sub.asMapOfRanges());
    assertEquals(
        ImmutableMap.of(Range.open(3, 7), 1, Range.openClosed(9, 10), 2, Range.closed(12, 16), 3),
        rangeMap.asMapOfRanges());

    sub.remove(Range.closed(3, 9));
    assertEquals(ImmutableMap.of(Range.openClosed(9, 10), 2), sub.asMapOfRanges());
    assertEquals(
        ImmutableMap.of(Range.open(3, 5), 1, Range.openClosed(9, 10), 2, Range.closed(12, 16), 3),
        rangeMap.asMapOfRanges());
  }

  public void testSubRangeMapClear() {
    RangeMap<Integer, Integer> rangeMap = TreeRangeMap.create();
    rangeMap.put(Range.open(3, 7), 1);
    rangeMap.put(Range.closed(9, 10), 2);
    rangeMap.put(Range.closed(12, 16), 3);
    RangeMap<Integer, Integer> sub = rangeMap.subRangeMap(Range.closed(5, 11));
    sub.clear();
    assertEquals(
        ImmutableMap.of(Range.open(3, 5), 1, Range.closed(12, 16), 3), rangeMap.asMapOfRanges());
  }

  private void verify(Map<Integer, Integer> model, RangeMap<Integer, Integer> test) {
    for (int i = MIN_BOUND - 1; i <= MAX_BOUND + 1; i++) {
      assertEquals(model.get(i), test.get(i));

      Entry<Range<Integer>, Integer> entry = test.getEntry(i);
      assertEquals(model.containsKey(i), entry != null);
      if (entry != null) {
        assertTrue(test.asMapOfRanges().entrySet().contains(entry));
      }
    }
    for (Range<Integer> range : test.asMapOfRanges().keySet()) {
      assertFalse(range.isEmpty());
    }
  }

  private static void putModel(Map<Integer, Integer> model, Range<Integer> range, int value) {
    for (int i = MIN_BOUND - 1; i <= MAX_BOUND + 1; i++) {
      if (range.contains(i)) {
        model.put(i, value);
      }
    }
  }

  private static void removeModel(Map<Integer, Integer> model, Range<Integer> range) {
    for (int i = MIN_BOUND - 1; i <= MAX_BOUND + 1; i++) {
      if (range.contains(i)) {
        model.remove(i);
      }
    }
  }
}
