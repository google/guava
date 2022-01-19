package com.google.common.collect;

import static com.google.common.collect.testing.Helpers.mapEntry;

import com.google.common.testing.CollectorTester;
import java.util.Map.Entry;
import java.util.function.BiPredicate;
import java.util.stream.Collector;
import java.util.stream.Stream;
import junit.framework.TestCase;

public final class AndroidAccessToCollectorsToImmutableMapTest extends TestCase {

  private static Collector<Entry<String, Integer>, ?, ImmutableMap<String, Integer>> collector() {
    return AndroidAccessToCollectors.toImmutableMap(Entry::getKey, Entry::getValue);
  }

  private static final BiPredicate<ImmutableMap<String, Integer>, ImmutableMap<String, Integer>>
      EQUIVALENCE = AndroidAccessToCollectorsTest.onResultOf(map -> map.entrySet().asList());

  public void testToImmutableMapEmpty() {
    CollectorTester.of(collector(), EQUIVALENCE).expectCollects(ImmutableMap.of());
  }

  public void testToImmutableMapSingleton() {
    CollectorTester.of(collector(), EQUIVALENCE)
        .expectCollects(ImmutableMap.of("a", 1), mapEntry("a", 1));
  }

  public void testToImmutableMap() {
    CollectorTester.of(collector(), EQUIVALENCE)
        .expectCollects(
            ImmutableMap.of("a", 1, "b", 2, "c", 3),
            mapEntry("a", 1),
            mapEntry("b", 2),
            mapEntry("c", 3));
  }

  public void testToImmutableMapDupes() {
    Collector<Entry<String, Integer>, ?, ImmutableMap<String, Integer>> collector = collector();
    try {
      ImmutableMap<String, Integer> unused =
          Stream.of(mapEntry("a", 1), mapEntry("b", 2), mapEntry("a", 3)).collect(collector);
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }
}
