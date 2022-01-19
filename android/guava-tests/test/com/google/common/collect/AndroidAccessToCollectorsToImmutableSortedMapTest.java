package com.google.common.collect;

import static com.google.common.collect.Ordering.natural;
import static com.google.common.collect.testing.Helpers.mapEntry;
import static com.google.common.truth.Truth.assertThat;
import static java.util.Comparator.reverseOrder;

import com.google.common.testing.CollectorTester;
import java.util.Comparator;
import java.util.Map.Entry;
import java.util.function.BiPredicate;
import java.util.stream.Collector;
import java.util.stream.Stream;
import junit.framework.TestCase;

public final class AndroidAccessToCollectorsToImmutableSortedMapTest extends TestCase {

  private static Collector<Entry<String, Integer>, ?, ImmutableSortedMap<String, Integer>>
      collector(Comparator<String> comparator) {
    return AndroidAccessToCollectors.toImmutableSortedMap(
        comparator, Entry::getKey, Entry::getValue);
  }

  private static final BiPredicate<
          ImmutableSortedMap<String, Integer>, ImmutableSortedMap<String, Integer>>
      EQUIVALENCE =
          AndroidAccessToCollectorsTest.onResultOf(
                  (ImmutableSortedMap<String, Integer> map) -> map.entrySet().asList())
              .and(AndroidAccessToCollectorsTest.onResultOf(ImmutableSortedMap::comparator));

  public void testToImmutableSortedMapEmpty() {
    CollectorTester.of(collector(natural()), EQUIVALENCE).expectCollects(ImmutableSortedMap.of());
  }

  public void testSanity() {
    ImmutableSortedMap<String, Integer> result =
        Stream.<Entry<String, Integer>>of().collect(collector(natural()));
    ImmutableSortedMap<Object, Object> reference = ImmutableSortedMap.of();
    assertThat(result.comparator()).isEqualTo(reference.comparator());
  }

  public void testToImmutableSortedMapSingleton() {
    CollectorTester.of(collector(natural()), EQUIVALENCE)
        .expectCollects(ImmutableSortedMap.of("a", 1), mapEntry("a", 1));
  }

  public void testToImmutableSortedMap() {
    CollectorTester.of(collector(natural()), EQUIVALENCE)
        .expectCollects(
            ImmutableSortedMap.of("a", 1, "b", 2, "c", 3),
            mapEntry("a", 1),
            mapEntry("b", 2),
            mapEntry("c", 3));
  }

  public void testToImmutableSortedMapSpecialComparator() {
    ImmutableSortedMap<String, Integer> expected =
        ImmutableSortedMap.<String, Integer>orderedBy(reverseOrder())
            .put("a", 1)
            .put("b", 2)
            .put("c", 3)
            .build();
    CollectorTester.of(collector(reverseOrder()), EQUIVALENCE)
        .expectCollects(expected, mapEntry("a", 1), mapEntry("b", 2), mapEntry("c", 3));
  }

  public void testToImmutableSortedMapDupes() {
    Collector<Entry<String, Integer>, ?, ImmutableSortedMap<String, Integer>> collector =
        collector(natural());
    try {
      ImmutableSortedMap<String, Integer> unused =
          Stream.of(mapEntry("a", 1), mapEntry("b", 2), mapEntry("a", 3)).collect(collector);
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }
}
