package com.google.common.collect;

import static com.google.common.collect.testing.Helpers.mapEntry;

import com.google.common.collect.AndroidAccessToCollectorsTest.MyEnum;
import com.google.common.testing.CollectorTester;
import java.util.Map.Entry;
import java.util.function.BiPredicate;
import java.util.stream.Collector;
import java.util.stream.Stream;
import junit.framework.TestCase;

public final class AndroidAccessToCollectorsToImmutableEnumMapTest extends TestCase {

  private static Collector<Entry<MyEnum, Integer>, ?, ImmutableMap<MyEnum, Integer>> collector() {
    return AndroidAccessToCollectors.toImmutableEnumMap(Entry::getKey, Entry::getValue);
  }

  private static Collector<Entry<MyEnum, Integer>, ?, ImmutableMap<MyEnum, Integer>>
      mergingCollector() {
    return AndroidAccessToCollectors.toImmutableEnumMap(
        Entry::getKey, Entry::getValue, Integer::sum);
  }

  private static final BiPredicate<ImmutableMap<MyEnum, Integer>, ImmutableMap<MyEnum, Integer>>
      EQUIVALENCE = AndroidAccessToCollectorsTest.onResultOf(map -> map.entrySet().asList());

  public void testToImmutableEnumMap() {
    CollectorTester.of(collector(), EQUIVALENCE)
        .expectCollects(ImmutableMap.of())
        .expectCollects(ImmutableMap.of(MyEnum.A, 3), mapEntry(MyEnum.A, 3))
        .expectCollects(
            ImmutableMap.of(MyEnum.A, 3, MyEnum.B, 5, MyEnum.C, 7, MyEnum.D, 9),
            mapEntry(MyEnum.A, 3),
            mapEntry(MyEnum.B, 5),
            mapEntry(MyEnum.C, 7),
            mapEntry(MyEnum.D, 9));
  }

  public void testToImmutableEnumMapThrowsOnDupes() {
    Collector<Entry<MyEnum, Integer>, ?, ImmutableMap<MyEnum, Integer>> collector = collector();
    try {
      ImmutableMap<MyEnum, Integer> unused =
          Stream.of(mapEntry(MyEnum.A, 1), mapEntry(MyEnum.A, 1)).collect(collector);
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }

  public void testToImmutableEnumMapMerging() {
    CollectorTester.of(mergingCollector(), EQUIVALENCE)
        .expectCollects(ImmutableMap.of())
        .expectCollects(ImmutableMap.of(MyEnum.A, 3), mapEntry(MyEnum.A, 1), mapEntry(MyEnum.A, 2))
        .expectCollects(
            ImmutableMap.of(MyEnum.A, 6, MyEnum.B, 2, MyEnum.C, 3, MyEnum.D, 10),
            mapEntry(MyEnum.A, 1),
            mapEntry(MyEnum.B, 2),
            mapEntry(MyEnum.A, 5),
            mapEntry(MyEnum.C, 3),
            mapEntry(MyEnum.D, 4),
            mapEntry(MyEnum.D, 6));
  }
}
