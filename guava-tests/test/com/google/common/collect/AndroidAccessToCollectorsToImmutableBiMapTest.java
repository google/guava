package com.google.common.collect;

import static com.google.common.collect.testing.Helpers.mapEntry;

import com.google.common.testing.CollectorTester;
import java.util.Map.Entry;
import java.util.function.BiPredicate;
import java.util.stream.Collector;
import java.util.stream.Stream;
import junit.framework.TestCase;

public final class AndroidAccessToCollectorsToImmutableBiMapTest extends TestCase {

  private static Collector<Entry<String, Integer>, ?, ImmutableBiMap<String, Integer>> collector() {
    return AndroidAccessToCollectors.toImmutableBiMap(Entry::getKey, Entry::getValue);
  }

  private static final BiPredicate<ImmutableBiMap<String, Integer>, ImmutableBiMap<String, Integer>>
      EQUIVALENCE = AndroidAccessToCollectorsTest.onResultOf(map -> map.entrySet().asList());

  public void testToImmutableBiMapEmpty() {
    CollectorTester.of(collector(), EQUIVALENCE).expectCollects(ImmutableBiMap.of());
  }

  public void testToImmutableBiMapSingleton() {
    CollectorTester.of(collector(), EQUIVALENCE)
        .expectCollects(ImmutableBiMap.of("a", 1), mapEntry("a", 1));
  }

  public void testToImmutableBiMap() {
    CollectorTester.of(collector(), EQUIVALENCE)
        .expectCollects(
            ImmutableBiMap.of("a", 1, "b", 2, "c", 3),
            mapEntry("a", 1),
            mapEntry("b", 2),
            mapEntry("c", 3));
  }

  public void testToImmutableBiMapDupeKeys() {
    Collector<Entry<String, Integer>, ?, ImmutableBiMap<String, Integer>> collector = collector();
    try {
      ImmutableBiMap<String, Integer> unused =
          Stream.of(mapEntry("a", 1), mapEntry("b", 2), mapEntry("a", 3)).collect(collector);
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }

  public void testToImmutableBiMapDupeValues() {
    Collector<Entry<String, Integer>, ?, ImmutableBiMap<String, Integer>> collector = collector();
    try {
      ImmutableBiMap<String, Integer> unused =
          Stream.of(mapEntry("a", 1), mapEntry("b", 2), mapEntry("c", 1)).collect(collector);
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }
}
