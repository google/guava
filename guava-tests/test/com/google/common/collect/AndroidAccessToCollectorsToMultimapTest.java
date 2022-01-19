package com.google.common.collect;

import static com.google.common.collect.Lists.charactersOf;
import static com.google.common.collect.testing.Helpers.mapEntry;

import com.google.common.testing.CollectorTester;
import java.util.Arrays;
import java.util.Map.Entry;
import java.util.function.BiPredicate;
import java.util.stream.Collector;
import junit.framework.TestCase;

public final class AndroidAccessToCollectorsToMultimapTest extends TestCase {

  public void testToMultimap() {
    Collector<Entry<String, Integer>, ?, ListMultimap<String, Integer>> collector =
        AndroidAccessToCollectors.toMultimap(
            Entry::getKey, Entry::getValue, ArrayListMultimap::create);
    BiPredicate<ListMultimap<String, Integer>, ListMultimap<String, Integer>> equivalence =
        AndroidAccessToCollectorsTest.onResultOf(
            multimap -> ImmutableList.copyOf(multimap.entries()));

    ArrayListMultimap<String, Integer> empty = ArrayListMultimap.create();
    ArrayListMultimap<String, Integer> expected = ArrayListMultimap.create();
    expected.put("a", 1);
    expected.put("b", 2);
    expected.put("a", 3);
    expected.put("c", 4);
    CollectorTester.of(collector, equivalence)
        .expectCollects(empty)
        .expectCollects(
            expected, mapEntry("a", 1), mapEntry("b", 2), mapEntry("a", 3), mapEntry("c", 4));
  }

  public void testFlatteningToMultimap() {
    Collector<String, ?, ListMultimap<Character, Character>> collector =
        AndroidAccessToCollectors.flatteningToMultimap(
            str -> str.charAt(0),
            str -> charactersOf(str.substring(1)).stream(),
            ArrayListMultimap::create);
    BiPredicate<ListMultimap<Character, Character>, ListMultimap<Character, Character>>
        equivalence =
            AndroidAccessToCollectorsTest.onResultOf(
                multimap -> ImmutableList.copyOf(multimap.entries()));
    ArrayListMultimap<Character, Character> empty = ArrayListMultimap.create();
    ArrayListMultimap<Character, Character> expected = ArrayListMultimap.create();
    expected.putAll('b', Arrays.asList('a', 'n', 'a', 'n', 'a'));
    expected.putAll('a', Arrays.asList('p', 'p', 'l', 'e'));
    expected.putAll('c', Arrays.asList('a', 'r', 'r', 'o', 't'));
    expected.putAll('a', Arrays.asList('s', 'p', 'a', 'r', 'a', 'g', 'u', 's'));
    expected.putAll('c', Arrays.asList('h', 'e', 'r', 'r', 'y'));
    CollectorTester.of(collector, equivalence)
        .expectCollects(empty)
        .expectCollects(expected, "banana", "apple", "carrot", "asparagus", "cherry");
  }
}
