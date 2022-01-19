package com.google.common.collect;

import static com.google.common.collect.Ordering.natural;
import static com.google.common.collect.testing.Helpers.mapEntry;
import static java.util.Comparator.reverseOrder;

import com.google.common.testing.CollectorTester;
import java.util.Comparator;
import java.util.Map.Entry;
import java.util.function.BiPredicate;
import java.util.stream.Collector;
import junit.framework.TestCase;

public final class AndroidAccessToCollectorsToImmutableSortedMapMergingTest extends TestCase {

  private static Collector<Entry<String, Integer>, ?, ImmutableSortedMap<String, Integer>>
      collector(Comparator<String> comparator) {
    return AndroidAccessToCollectors.toImmutableSortedMap(
        comparator, Entry::getKey, Entry::getValue, Integer::sum);
  }

  private static final BiPredicate<
          ImmutableSortedMap<String, Integer>, ImmutableSortedMap<String, Integer>>
      EQUIVALENCE =
          AndroidAccessToCollectorsTest.onResultOf(
                  (ImmutableSortedMap<String, Integer> map) -> map.entrySet().asList())
              .and(AndroidAccessToCollectorsTest.onResultOf(ImmutableSortedMap::comparator));

  public void testToImmutableSortedMapMergingEmpty() {
    CollectorTester.of(collector(natural()), EQUIVALENCE).expectCollects(ImmutableSortedMap.of());
  }

  public void testToImmutableSortedMapMergingSingleton() {
    CollectorTester.of(collector(natural()), EQUIVALENCE)
        .expectCollects(ImmutableSortedMap.of("a", 3), mapEntry("a", 1), mapEntry("a", 2));
  }

  public void testToImmutableSortedMapMergingDupes() {
    CollectorTester.of(collector(natural()), EQUIVALENCE)
        .expectCollects(
            ImmutableSortedMap.of("a", 1, "b", 7, "c", 9, "d", 8),
            mapEntry("a", 1),
            mapEntry("b", 2),
            mapEntry("c", 3),
            mapEntry("b", 5),
            mapEntry("c", 6),
            mapEntry("d", 8));
  }

  public void testToImmutableSortedMapMergingDupesSpecialComparator() {
    ImmutableSortedMap<String, Integer> expected =
        ImmutableSortedMap.<String, Integer>orderedBy(reverseOrder())
            .put("a", 1)
            .put("b", 7)
            .put("c", 9)
            .put("d", 8)
            .build();
    CollectorTester.of(collector(reverseOrder()), EQUIVALENCE)
        .expectCollects(
            expected,
            mapEntry("a", 1),
            mapEntry("b", 2),
            mapEntry("c", 3),
            mapEntry("b", 5),
            mapEntry("c", 6),
            mapEntry("d", 8));
  }
}
