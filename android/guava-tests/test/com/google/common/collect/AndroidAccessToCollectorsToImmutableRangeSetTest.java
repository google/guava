package com.google.common.collect;

import com.google.common.annotations.GwtIncompatible;
import com.google.common.testing.CollectorTester;
import java.util.stream.Collector;
import java.util.stream.Stream;
import junit.framework.TestCase;

@GwtIncompatible
public final class AndroidAccessToCollectorsToImmutableRangeSetTest extends TestCase {

  private static Collector<Range<Integer>, ?, ImmutableRangeSet<Integer>> collector() {
    return AndroidAccessToCollectors.toImmutableRangeSet();
  }

  public void testToImmutableRangeSetEmpty() {
    CollectorTester.of(collector()).expectCollects(ImmutableRangeSet.of());
  }

  public void testToImmutableRangeSetSingleton() {
    Range<Integer> range = Range.closed(2, 3);
    CollectorTester.of(collector()).expectCollects(ImmutableRangeSet.of(range), range);
  }

  public void testToImmutableRangeSetAdjacentSingleton() {
    Range<Integer> range1 = Range.closedOpen(2, 3);
    Range<Integer> range2 = Range.closedOpen(3, 4);
    Range<Integer> range = Range.closedOpen(2, 4);
    CollectorTester.of(collector()).expectCollects(ImmutableRangeSet.of(range), range1, range2);
  }

  public void testToImmutableRangeSet() {
    Range<Integer> range1 = Range.closed(1, 2);
    Range<Integer> range2 = Range.closed(4, 5);
    Range<Integer> range3 = Range.closed(7, 8);
    ImmutableRangeSet<Integer> ranges =
        ImmutableRangeSet.<Integer>builder().add(range1).add(range2).add(range3).build();
    CollectorTester.of(collector()).expectCollects(ranges, range1, range2, range3);
  }

  public void testToImmutableRangeSetAdjacent() {
    Range<Integer> range1 = Range.closed(1, 2);
    Range<Integer> range2 = Range.closed(4, 5);
    Range<Integer> range3 = Range.closed(7, 8);
    Range<Integer> range4 = Range.open(5, 7); // adjacent between 2 & 3
    Range<Integer> merged = Range.closed(4, 8); // fusion of 2, 3 & 4
    ImmutableRangeSet<Integer> ranges =
        ImmutableRangeSet.<Integer>builder().add(range1).add(merged).build();
    CollectorTester.of(collector()).expectCollects(ranges, range1, range2, range3, range4);
  }

  public void testToImmutableRangeSetOverlap() {
    Range<Integer> range1 = Range.closed(1, 2);
    Range<Integer> range2 = Range.closed(4, 7);
    Range<Integer> range3 = Range.closed(5, 8);
    Collector<Range<Integer>, ?, ImmutableRangeSet<Integer>> collector = collector();
    try {
      Stream.of(range1, range2, range3).collect(collector);
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }
}
