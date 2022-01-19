package com.google.common.collect;

import com.google.common.testing.CollectorTester;
import java.util.function.BiPredicate;
import java.util.stream.Collector;
import junit.framework.TestCase;

public final class AndroidAccessToCollectorsToImmutableSetTest extends TestCase {

  private static final BiPredicate<ImmutableSet<String>, ImmutableSet<String>> EQUIVALENCE =
      AndroidAccessToCollectorsTest.onResultOf(ImmutableSet::asList);

  private static Collector<String, ?, ImmutableSet<String>> collector() {
    return AndroidAccessToCollectors.toImmutableSet();
  }

  public void testToImmutableSetEmpty() {
    CollectorTester.of(collector(), EQUIVALENCE).expectCollects(ImmutableSet.of());
  }

  public void testToImmutableSetSingleton() {
    CollectorTester.of(collector(), EQUIVALENCE).expectCollects(ImmutableSet.of("a"), "a");
  }

  public void testToImmutableSetSingletonDupes() {
    CollectorTester.of(collector(), EQUIVALENCE)
        .expectCollects(ImmutableSet.of("a"), "a", "a", "a");
  }

  public void testToImmutableSet() {
    CollectorTester.of(collector(), EQUIVALENCE)
        .expectCollects(ImmutableSet.of("a", "b", "c", "d"), "a", "b", "c", "d");
  }

  public void testToImmutableSetDupes() {
    CollectorTester.of(collector(), EQUIVALENCE)
        .expectCollects(ImmutableSet.of("a", "b", "c", "d"), "a", "b", "a", "b", "c", "d", "d");
  }
}
