package com.google.common.collect;

import static com.google.common.collect.testing.Helpers.mapEntry;

import com.google.common.testing.CollectorTester;
import java.util.Map.Entry;
import java.util.function.BiPredicate;
import java.util.stream.Collector;
import junit.framework.TestCase;

public final class AndroidAccessToCollectorsToImmutableMapMergingTest extends TestCase {

  private static Collector<Entry<String, Integer>, ?, ImmutableMap<String, Integer>> collector() {
    return AndroidAccessToCollectors.toImmutableMap(Entry::getKey, Entry::getValue, Integer::sum);
  }

  private static final BiPredicate<ImmutableMap<String, Integer>, ImmutableMap<String, Integer>>
      EQUIVALENCE = AndroidAccessToCollectorsTest.onResultOf(map -> map.entrySet().asList());

  public void testToImmutableMapMergingEmpty() {
    CollectorTester.of(collector(), EQUIVALENCE).expectCollects(ImmutableMap.of());
  }

  public void testToImmutableMapMergingSingleton() {
    CollectorTester.of(collector(), EQUIVALENCE)
        .expectCollects(ImmutableMap.of("a", 3), mapEntry("a", 1), mapEntry("a", 2));
  }

  public void testToImmutableMapMergingDupes() {
    CollectorTester.of(collector(), EQUIVALENCE)
        .expectCollects(
            ImmutableMap.of("a", 1, "b", 7, "c", 9, "d", 8),
            mapEntry("a", 1),
            mapEntry("b", 2),
            mapEntry("c", 3),
            mapEntry("b", 5),
            mapEntry("c", 6),
            mapEntry("d", 8));
  }
}
