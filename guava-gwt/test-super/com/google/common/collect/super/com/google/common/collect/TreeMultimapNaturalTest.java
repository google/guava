/*
 * Copyright (C) 2007 The Guava Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.common.collect;

import static org.truth0.Truth.ASSERT;

import com.google.common.annotations.GwtCompatible;

import junit.framework.TestCase;

import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;

/**
 * Unit tests for {@code TreeMultimap} with natural ordering.
 *
 * @author Jared Levy
 */
@GwtCompatible(emulated = true)
public class TreeMultimapNaturalTest extends TestCase {

  protected SetMultimap<String, Integer> create() {
    return TreeMultimap.create();
  }

  /**
   * Create and populate a {@code TreeMultimap} with the natural ordering of
   * keys and values.
   */
  private TreeMultimap<String, Integer> createPopulate() {
    TreeMultimap<String, Integer> multimap = TreeMultimap.create();
    multimap.put("google", 2);
    multimap.put("google", 6);
    multimap.put("foo", 3);
    multimap.put("foo", 1);
    multimap.put("foo", 7);
    multimap.put("tree", 4);
    multimap.put("tree", 0);
    return multimap;
  }

  public void testToString() {
    SetMultimap<String, Integer> multimap = create();
    multimap.putAll("bar", Arrays.asList(3, 1, 2));
    multimap.putAll("foo", Arrays.asList(2, 3, 1, -1, 4));
    assertEquals("{bar=[1, 2, 3], foo=[-1, 1, 2, 3, 4]}",
        multimap.toString());
  }

  public void testOrderedGet() {
    TreeMultimap<String, Integer> multimap = createPopulate();
    ASSERT.that(multimap.get("foo")).has().exactly(1, 3, 7).inOrder();
    ASSERT.that(multimap.get("google")).has().exactly(2, 6).inOrder();
    ASSERT.that(multimap.get("tree")).has().exactly(0, 4).inOrder();
  }

  public void testOrderedKeySet() {
    TreeMultimap<String, Integer> multimap = createPopulate();
    ASSERT.that(multimap.keySet()).has().exactly("foo", "google", "tree").inOrder();
  }

  public void testOrderedAsMapEntries() {
    TreeMultimap<String, Integer> multimap = createPopulate();
    Iterator<Map.Entry<String, Collection<Integer>>> iterator =
        multimap.asMap().entrySet().iterator();
    Map.Entry<String, Collection<Integer>> entry = iterator.next();
    assertEquals("foo", entry.getKey());
    ASSERT.that(entry.getValue()).has().exactly(1, 3, 7);
    entry = iterator.next();
    assertEquals("google", entry.getKey());
    ASSERT.that(entry.getValue()).has().exactly(2, 6);
    entry = iterator.next();
    assertEquals("tree", entry.getKey());
    ASSERT.that(entry.getValue()).has().exactly(0, 4);
  }

  public void testOrderedEntries() {
    TreeMultimap<String, Integer> multimap = createPopulate();
    ASSERT.that(multimap.entries()).has().exactly(
        Maps.immutableEntry("foo", 1),
        Maps.immutableEntry("foo", 3),
        Maps.immutableEntry("foo", 7),
        Maps.immutableEntry("google", 2),
        Maps.immutableEntry("google", 6),
        Maps.immutableEntry("tree", 0),
        Maps.immutableEntry("tree", 4)).inOrder();
  }

  public void testOrderedValues() {
    TreeMultimap<String, Integer> multimap = createPopulate();
    ASSERT.that(multimap.values()).has().exactly(
        1, 3, 7, 2, 6, 0, 4).inOrder();
  }

  public void testMultimapConstructor() {
    SetMultimap<String, Integer> multimap = create();
    multimap.putAll("bar", Arrays.asList(3, 1, 2));
    multimap.putAll("foo", Arrays.asList(2, 3, 1, -1, 4));
    TreeMultimap<String, Integer> copy = TreeMultimap.create(multimap);
    assertEquals(multimap, copy);
  }

  private static final Comparator<Double> KEY_COMPARATOR =
      Ordering.natural();

  private static final Comparator<Double> VALUE_COMPARATOR =
      Ordering.natural().reverse().nullsFirst();

  /**
   * Test that creating one TreeMultimap from another does not copy the
   * comparators from the source TreeMultimap.
   */
  public void testCreateFromTreeMultimap() {
    Multimap<Double, Double> tree = TreeMultimap.create(KEY_COMPARATOR, VALUE_COMPARATOR);
    tree.put(1.0, 2.0);
    tree.put(2.0, 3.0);
    tree.put(3.0, 4.0);
    tree.put(4.0, 5.0);

    TreeMultimap<Double, Double> copyFromTree = TreeMultimap.create(tree);
    assertEquals(tree, copyFromTree);
    assertSame(Ordering.natural(), copyFromTree.keyComparator());
    assertSame(Ordering.natural(), copyFromTree.valueComparator());
    assertSame(Ordering.natural(), copyFromTree.get(1.0).comparator());
  }

  /**
   * Test that creating one TreeMultimap from a non-TreeMultimap
   * results in natural ordering.
   */
  public void testCreateFromHashMultimap() {
    Multimap<Double, Double> hash = HashMultimap.create();
    hash.put(1.0, 2.0);
    hash.put(2.0, 3.0);
    hash.put(3.0, 4.0);
    hash.put(4.0, 5.0);

    TreeMultimap<Double, Double> copyFromHash = TreeMultimap.create(hash);
    assertEquals(hash, copyFromHash);
    assertEquals(Ordering.natural(), copyFromHash.keyComparator());
    assertEquals(Ordering.natural(), copyFromHash.valueComparator());
  }

  /**
   * Test that creating one TreeMultimap from a SortedSetMultimap uses natural
   * ordering.
   */
  public void testCreateFromSortedSetMultimap() {
    SortedSetMultimap<Double, Double> tree = TreeMultimap.create(KEY_COMPARATOR, VALUE_COMPARATOR);
    tree.put(1.0, 2.0);
    tree.put(2.0, 3.0);
    tree.put(3.0, 4.0);
    tree.put(4.0, 5.0);

    SortedSetMultimap<Double, Double> sorted = Multimaps.unmodifiableSortedSetMultimap(tree);
    TreeMultimap<Double, Double> copyFromSorted = TreeMultimap.create(sorted);
    assertEquals(tree, copyFromSorted);
    assertSame(Ordering.natural(), copyFromSorted.keyComparator());
    assertSame(Ordering.natural(), copyFromSorted.valueComparator());
    assertSame(Ordering.natural(), copyFromSorted.get(1.0).comparator());
  }

  public void testComparators() {
    TreeMultimap<String, Integer> multimap = TreeMultimap.create();
    assertEquals(Ordering.natural(), multimap.keyComparator());
    assertEquals(Ordering.natural(), multimap.valueComparator());
  }

  public void testTreeMultimapAsMapSorted() {
    TreeMultimap<String, Integer> multimap = createPopulate();
    SortedMap<String, Collection<Integer>> asMap = multimap.asMap();
    assertEquals(Ordering.natural(), asMap.comparator());
    assertEquals("foo", asMap.firstKey());
    assertEquals("tree", asMap.lastKey());
    Set<Integer> fooValues = ImmutableSet.of(1, 3, 7);
    Set<Integer> googleValues = ImmutableSet.of(2, 6);
    Set<Integer> treeValues = ImmutableSet.of(4, 0);
    assertEquals(ImmutableMap.of("google", googleValues, "tree", treeValues),
        asMap.tailMap("g"));
    assertEquals(ImmutableMap.of("google", googleValues, "foo", fooValues),
        asMap.headMap("h"));
    assertEquals(ImmutableMap.of("google", googleValues),
        asMap.subMap("g", "h"));
  }

  public void testTailSetClear() {
    TreeMultimap<String, Integer> multimap = TreeMultimap.create();
    multimap.put("a", 1);
    multimap.put("a", 11);
    multimap.put("b", 2);
    multimap.put("c", 3);
    multimap.put("d", 4);
    multimap.put("e", 5);
    multimap.put("e", 55);

    multimap.keySet().tailSet("d").clear();
    assertEquals(ImmutableSet.of("a", "b", "c"), multimap.keySet());
    assertEquals(4, multimap.size());
    assertEquals(4, multimap.values().size());
    assertEquals(4, multimap.keys().size());
  }
}

