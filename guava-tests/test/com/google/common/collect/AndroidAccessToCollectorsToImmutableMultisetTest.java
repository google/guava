package com.google.common.collect;

import static com.google.common.collect.testing.Helpers.mapEntry;

import com.google.common.testing.CollectorTester;
import java.util.Map;
import java.util.function.BiPredicate;
import java.util.stream.Collector;
import junit.framework.TestCase;

public final class AndroidAccessToCollectorsToImmutableMultisetTest extends TestCase {

  private static final BiPredicate<ImmutableMultiset<String>, ImmutableMultiset<String>>
      EQUIVALENCE = AndroidAccessToCollectorsTest.onResultOf(ImmutableMultiset::asList);

  private static Collector<String, ?, ImmutableMultiset<String>> collector() {
    return AndroidAccessToCollectors.toImmutableMultiset();
  }

  private static Collector<Map.Entry<String, Integer>, ?, ImmutableMultiset<String>>
      collectorWithFunctions() {
    return AndroidAccessToCollectors.toImmutableMultiset(Map.Entry::getKey, Map.Entry::getValue);
  }

  public void testToImmutableMultisetEmpty() {
    CollectorTester.of(collector(), EQUIVALENCE).expectCollects(ImmutableMultiset.of());
  }

  public void testToImmutableMultisetSingleton() {
    CollectorTester.of(collector(), EQUIVALENCE)
        .expectCollects(ImmutableMultiset.of("a", "a", "a"), "a", "a", "a");
  }

  public void testToImmutableMultisetSingletonFunction() {
    CollectorTester.of(collectorWithFunctions(), EQUIVALENCE)
        .expectCollects(ImmutableMultiset.of("a", "a", "a"), mapEntry("a", 3));
  }

  public void testToImmutableMultiset() {
    CollectorTester.of(collector(), EQUIVALENCE)
        .expectCollects(
            ImmutableMultiset.of("a", "a", "b", "b", "c", "d", "d"),
            "a",
            "b",
            "a",
            "b",
            "c",
            "d",
            "d");
  }

  public void testToImmutableMultisetFunction() {
    CollectorTester.of(collectorWithFunctions(), EQUIVALENCE)
        .expectCollects(
            ImmutableMultiset.of("a", "a", "b", "c", "c", "c"),
            mapEntry("a", 1),
            mapEntry("b", 1),
            mapEntry("a", 1),
            mapEntry("c", 3));
  }
}
