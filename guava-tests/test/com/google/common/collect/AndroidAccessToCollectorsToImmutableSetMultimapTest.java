package com.google.common.collect;

import static com.google.common.collect.Lists.charactersOf;
import static com.google.common.collect.testing.Helpers.mapEntry;

import com.google.common.testing.CollectorTester;
import java.util.Arrays;
import java.util.Map.Entry;
import java.util.stream.Collector;
import junit.framework.TestCase;

public final class AndroidAccessToCollectorsToImmutableSetMultimapTest extends TestCase {

  public void testToImmutableSetMultimap() {
    Collector<Entry<String, Integer>, ?, ImmutableSetMultimap<String, Integer>> collector =
        AndroidAccessToCollectors.toImmutableSetMultimap(Entry::getKey, Entry::getValue);
    CollectorTester.of(
            collector,
            AndroidAccessToCollectorsTest.onResultOf(multimap -> multimap.entries().asList()))
        .expectCollects(ImmutableSetMultimap.of())
        .expectCollects(
            ImmutableSetMultimap.of("a", 1, "b", 2, "a", 3, "c", 4),
            mapEntry("a", 1),
            mapEntry("b", 2),
            mapEntry("a", 3),
            mapEntry("c", 4));
  }

  public void testFlatteningToImmutableSetMultimap() {
    Collector<String, ?, ImmutableSetMultimap<Character, Character>> collector =
        AndroidAccessToCollectors.flatteningToImmutableSetMultimap(
            str -> str.charAt(0), str -> charactersOf(str.substring(1)).stream());
    ImmutableSetMultimap<Character, Character> expected =
        ImmutableSetMultimap.<Character, Character>builder()
            .putAll('b', Arrays.asList('a', 'n', 'a', 'n', 'a'))
            .putAll('a', Arrays.asList('p', 'p', 'l', 'e'))
            .putAll('c', Arrays.asList('a', 'r', 'r', 'o', 't'))
            .putAll('a', Arrays.asList('s', 'p', 'a', 'r', 'a', 'g', 'u', 's'))
            .putAll('c', Arrays.asList('h', 'e', 'r', 'r', 'y'))
            .build();
    CollectorTester.of(
            collector,
            AndroidAccessToCollectorsTest.onResultOf(multimap -> multimap.entries().asList()))
        .expectCollects(ImmutableSetMultimap.of())
        .expectCollects(expected, "banana", "apple", "carrot", "asparagus", "cherry");
  }
}
