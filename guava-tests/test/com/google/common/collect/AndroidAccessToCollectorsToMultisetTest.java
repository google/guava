package com.google.common.collect;

import static com.google.common.collect.testing.Helpers.mapEntry;

import com.google.common.testing.CollectorTester;
import java.util.Map.Entry;
import java.util.function.BiPredicate;
import java.util.stream.Collector;
import junit.framework.TestCase;

public final class AndroidAccessToCollectorsToMultisetTest extends TestCase {

  private static final BiPredicate<Multiset<String>, Multiset<String>> EQUIVALENCE =
      AndroidAccessToCollectorsTest.onResultOf(ms -> ImmutableList.copyOf(ms.entrySet()));

  private static Collector<Entry<String, Integer>, ?, Multiset<String>> collector() {
    return AndroidAccessToCollectors.toMultiset(
        Entry::getKey, Entry::getValue, LinkedHashMultiset::create);
  }

  public void testToMultisetEmpty() {
    CollectorTester.of(collector(), EQUIVALENCE).expectCollects(ImmutableMultiset.of());
  }

  public void testToMultisetSingleton() {
    CollectorTester.of(collector(), EQUIVALENCE)
        .expectCollects(ImmutableMultiset.of("a", "a", "a"), mapEntry("a", 3));
  }

  public void testToMultisetFunction() {
    CollectorTester.of(collector(), EQUIVALENCE)
        .expectCollects(
            ImmutableMultiset.of("a", "a", "b", "c", "c", "c"),
            mapEntry("a", 1),
            mapEntry("b", 1),
            mapEntry("a", 1),
            mapEntry("c", 3));
  }
}
