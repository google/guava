package com.google.common.collect;

import static com.google.common.collect.Ordering.natural;
import static java.util.Comparator.reverseOrder;

import com.google.common.testing.CollectorTester;
import java.util.Comparator;
import java.util.function.BiPredicate;
import java.util.stream.Collector;
import junit.framework.TestCase;

public final class AndroidAccessToCollectorsToImmutableSortedSetTest extends TestCase {

  private static Collector<String, ?, ImmutableSortedSet<String>> collector(
      Comparator<String> comparator) {
    return AndroidAccessToCollectors.toImmutableSortedSet(comparator);
  }

  private static final BiPredicate<ImmutableSortedSet<String>, ImmutableSortedSet<String>>
      EQUIVALENCE =
          AndroidAccessToCollectorsTest.onResultOf(ImmutableSortedSet<String>::asList)
              .and(AndroidAccessToCollectorsTest.onResultOf(ImmutableSortedSet::comparator));

  public void testToImmutableSortedSetEmpty() {
    CollectorTester.of(collector(natural()), EQUIVALENCE).expectCollects(ImmutableSortedSet.of());
  }

  public void testToImmutableSortedSetSingleton() {
    CollectorTester.of(collector(natural()), EQUIVALENCE)
        .expectCollects(ImmutableSortedSet.of("a"), "a");
  }

  public void testToImmutableSortedSetSingletonDupes() {
    CollectorTester.of(collector(natural()), EQUIVALENCE)
        .expectCollects(ImmutableSortedSet.of("a"), "a", "a", "a");
  }

  public void testToImmutableSortedSet() {
    CollectorTester.of(collector(natural()), EQUIVALENCE)
        .expectCollects(ImmutableSortedSet.of("a", "b", "c", "d"), "a", "c", "b", "d");
  }

  public void testToImmutableSortedSetDupes() {
    CollectorTester.of(collector(natural()), EQUIVALENCE)
        .expectCollects(
            ImmutableSortedSet.of("a", "b", "c", "d"), "a", "d", "c", "b", "a", "d", "c", "b");
  }

  public void testToImmutableSortedCustomComparator() {
    ImmutableSortedSet<String> expected =
        ImmutableSortedSet.<String>orderedBy(reverseOrder()).add("a", "b", "c", "d").build();
    CollectorTester.of(collector(reverseOrder()), EQUIVALENCE)
        .expectCollects(expected, "a", "d", "c", "b", "a", "d", "c", "b");
  }
}
