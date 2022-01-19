package com.google.common.collect;

import static com.google.common.collect.testing.Helpers.mapEntry;

import com.google.common.annotations.GwtIncompatible;
import com.google.common.testing.CollectorTester;
import java.util.Map.Entry;
import java.util.stream.Collector;
import java.util.stream.Stream;
import junit.framework.TestCase;

@GwtIncompatible
public final class AndroidAccessToCollectorsToImmutableRangeMapTest extends TestCase {

  private static Collector<Entry<Range<Integer>, String>, ?, ImmutableRangeMap<Integer, String>>
      collector() {
    return AndroidAccessToCollectors.toImmutableRangeMap(Entry::getKey, Entry::getValue);
  }

  public void testToImmutableRangeMapEmpty() {
    CollectorTester.of(collector()).expectCollects(ImmutableRangeMap.of());
  }

  public void testToImmutableRangeMapSingleton() {
    Range<Integer> range = Range.closed(1, 2);
    CollectorTester.of(collector())
        .expectCollects(ImmutableRangeMap.of(range, "one"), mapEntry(range, "one"));
  }

  public void testToImmutableRangeMap() {
    Range<Integer> range1 = Range.closedOpen(1, 2);
    Range<Integer> range2 = Range.closedOpen(2, 3);
    Range<Integer> range3 = Range.closedOpen(5, 6);

    CollectorTester.of(collector())
        .expectCollects(
            ImmutableRangeMap.<Integer, String>builder()
                .put(range1, "one")
                .put(range2, "two")
                .put(range3, "three")
                .build(),
            mapEntry(range1, "one"),
            mapEntry(range2, "two"),
            mapEntry(range3, "three"));
  }

  public void testToImmutableRangeMapOverlap() {
    Collector<Entry<Range<Integer>, String>, ?, ImmutableRangeMap<Integer, String>> collector =
        collector();
    Range<Integer> range1 = Range.closedOpen(1, 3);
    Range<Integer> range2 = Range.closedOpen(2, 4);
    try {
      ImmutableRangeMap<Integer, String> unused =
          Stream.of(mapEntry(range1, "one"), mapEntry(range2, "two")).collect(collector);
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }
}
