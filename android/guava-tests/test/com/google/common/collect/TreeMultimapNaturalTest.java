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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.truth.Truth.assertThat;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.collect.testing.DerivedComparable;
import com.google.common.collect.testing.Helpers;
import com.google.common.collect.testing.NavigableMapTestSuiteBuilder;
import com.google.common.collect.testing.NavigableSetTestSuiteBuilder;
import com.google.common.collect.testing.SampleElements;
import com.google.common.collect.testing.TestSortedMapGenerator;
import com.google.common.collect.testing.TestStringSetGenerator;
import com.google.common.collect.testing.TestStringSortedSetGenerator;
import com.google.common.collect.testing.features.CollectionFeature;
import com.google.common.collect.testing.features.CollectionSize;
import com.google.common.collect.testing.features.MapFeature;
import com.google.common.collect.testing.google.SortedSetMultimapTestSuiteBuilder;
import com.google.common.collect.testing.google.TestStringSetMultimapGenerator;
import com.google.common.testing.SerializableTester;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Unit tests for {@code TreeMultimap} with natural ordering.
 *
 * @author Jared Levy
 */
@GwtCompatible(emulated = true)
public class TreeMultimapNaturalTest extends TestCase {

  @GwtIncompatible // suite
  public static Test suite() {
    TestSuite suite = new TestSuite();
    // TODO(lowasser): should we force TreeMultimap to be more thorough about checking nulls?
    suite.addTest(
        SortedSetMultimapTestSuiteBuilder.using(
                new TestStringSetMultimapGenerator() {
                  @Override
                  protected SetMultimap<String, String> create(Entry<String, String>[] entries) {
                    SetMultimap<String, String> multimap =
                        TreeMultimap.create(
                            Ordering.natural().nullsFirst(), Ordering.natural().nullsFirst());
                    for (Entry<String, String> entry : entries) {
                      multimap.put(entry.getKey(), entry.getValue());
                    }
                    return multimap;
                  }

                  @Override
                  public Iterable<Entry<String, String>> order(
                      List<Entry<String, String>> insertionOrder) {
                    return new Ordering<Entry<String, String>>() {
                      @Override
                      public int compare(Entry<String, String> left, Entry<String, String> right) {
                        return ComparisonChain.start()
                            .compare(left.getKey(), right.getKey(), Ordering.natural().nullsFirst())
                            .compare(
                                left.getValue(), right.getValue(), Ordering.natural().nullsFirst())
                            .result();
                      }
                    }.sortedCopy(insertionOrder);
                  }
                })
            .named("TreeMultimap nullsFirst")
            .withFeatures(
                MapFeature.ALLOWS_NULL_KEYS,
                MapFeature.ALLOWS_NULL_VALUES,
                MapFeature.ALLOWS_ANY_NULL_QUERIES,
                MapFeature.GENERAL_PURPOSE,
                MapFeature.FAILS_FAST_ON_CONCURRENT_MODIFICATION,
                CollectionFeature.SUPPORTS_ITERATOR_REMOVE,
                CollectionFeature.KNOWN_ORDER,
                CollectionFeature.SERIALIZABLE,
                CollectionSize.ANY)
            .createTestSuite());
    suite.addTest(
        NavigableSetTestSuiteBuilder.using(
                new TestStringSortedSetGenerator() {
                  @Override
                  protected NavigableSet<String> create(String[] elements) {
                    TreeMultimap<String, Integer> multimap =
                        TreeMultimap.create(Ordering.natural().nullsFirst(), Ordering.natural());
                    for (int i = 0; i < elements.length; i++) {
                      multimap.put(elements[i], i);
                    }
                    return multimap.keySet();
                  }

                  @Override
                  public List<String> order(List<String> insertionOrder) {
                    return Ordering.natural().nullsFirst().sortedCopy(insertionOrder);
                  }
                })
            .named("TreeMultimap.keySet")
            .withFeatures(
                CollectionFeature.ALLOWS_NULL_VALUES,
                CollectionFeature.REMOVE_OPERATIONS,
                CollectionFeature.KNOWN_ORDER,
                CollectionSize.ANY)
            .createTestSuite());
    suite.addTest(
        NavigableMapTestSuiteBuilder.using(
                new TestSortedMapGenerator<String, Collection<String>>() {

                  @Override
                  public String[] createKeyArray(int length) {
                    return new String[length];
                  }

                  @SuppressWarnings("unchecked")
                  @Override
                  public Collection<String>[] createValueArray(int length) {
                    return new Collection[length];
                  }

                  @Override
                  public SampleElements<Entry<String, Collection<String>>> samples() {
                    return new SampleElements<>(
                        Helpers.mapEntry("a", (Collection<String>) ImmutableSortedSet.of("alex")),
                        Helpers.mapEntry(
                            "b", (Collection<String>) ImmutableSortedSet.of("bob", "bagel")),
                        Helpers.mapEntry(
                            "c", (Collection<String>) ImmutableSortedSet.of("carl", "carol")),
                        Helpers.mapEntry(
                            "d", (Collection<String>) ImmutableSortedSet.of("david", "dead")),
                        Helpers.mapEntry(
                            "e", (Collection<String>) ImmutableSortedSet.of("eric", "elaine")));
                  }

                  @SuppressWarnings("unchecked")
                  @Override
                  public Entry<String, Collection<String>>[] createArray(int length) {
                    return new Entry[length];
                  }

                  @Override
                  public Iterable<Entry<String, Collection<String>>> order(
                      List<Entry<String, Collection<String>>> insertionOrder) {
                    return new Ordering<Entry<String, ?>>() {
                      @Override
                      public int compare(Entry<String, ?> left, Entry<String, ?> right) {
                        return left.getKey().compareTo(right.getKey());
                      }
                    }.sortedCopy(insertionOrder);
                  }

                  @Override
                  public NavigableMap<String, Collection<String>> create(Object... elements) {
                    TreeMultimap<String, String> multimap = TreeMultimap.create();
                    for (Object o : elements) {
                      @SuppressWarnings("unchecked")
                      Entry<String, Collection<String>> entry =
                          (Entry<String, Collection<String>>) o;
                      checkArgument(!multimap.containsKey(entry.getKey()));
                      multimap.putAll(entry.getKey(), entry.getValue());
                    }
                    return multimap.asMap();
                  }

                  @Override
                  public Entry<String, Collection<String>> belowSamplesLesser() {
                    return Helpers.mapEntry(
                        "-- a", (Collection<String>) ImmutableSortedSet.of("--below"));
                  }

                  @Override
                  public Entry<String, Collection<String>> belowSamplesGreater() {
                    return Helpers.mapEntry(
                        "-- b", (Collection<String>) ImmutableSortedSet.of("--below"));
                  }

                  @Override
                  public Entry<String, Collection<String>> aboveSamplesLesser() {
                    return Helpers.mapEntry(
                        "~~ b", (Collection<String>) ImmutableSortedSet.of("~above"));
                  }

                  @Override
                  public Entry<String, Collection<String>> aboveSamplesGreater() {
                    return Helpers.mapEntry(
                        "~~ c", (Collection<String>) ImmutableSortedSet.of("~above"));
                  }
                })
            .named("TreeMultimap.asMap")
            .withFeatures(
                MapFeature.SUPPORTS_REMOVE,
                MapFeature.REJECTS_DUPLICATES_AT_CREATION,
                CollectionFeature.SUPPORTS_ITERATOR_REMOVE,
                CollectionFeature.KNOWN_ORDER,
                CollectionSize.ANY)
            .createTestSuite());
    suite.addTest(
        NavigableSetTestSuiteBuilder.using(
                new TestStringSetGenerator() {
                  @Override
                  protected Set<String> create(String[] elements) {
                    TreeMultimap<Integer, String> multimap =
                        TreeMultimap.create(Ordering.natural(), Ordering.natural().nullsFirst());
                    multimap.putAll(1, Arrays.asList(elements));
                    return multimap.get(1);
                  }

                  @Override
                  public List<String> order(List<String> insertionOrder) {
                    return Ordering.natural().nullsFirst().sortedCopy(insertionOrder);
                  }
                })
            .named("TreeMultimap.get")
            .withFeatures(
                CollectionFeature.ALLOWS_NULL_VALUES,
                CollectionFeature.GENERAL_PURPOSE,
                CollectionFeature.KNOWN_ORDER,
                CollectionSize.ANY)
            .createTestSuite());
    suite.addTest(
        NavigableSetTestSuiteBuilder.using(
                new TestStringSetGenerator() {
                  @Override
                  protected Set<String> create(String[] elements) {
                    TreeMultimap<Integer, String> multimap =
                        TreeMultimap.create(Ordering.natural(), Ordering.natural().nullsFirst());
                    multimap.putAll(1, Arrays.asList(elements));
                    return (Set<String>) multimap.asMap().entrySet().iterator().next().getValue();
                  }

                  @Override
                  public List<String> order(List<String> insertionOrder) {
                    return Ordering.natural().nullsFirst().sortedCopy(insertionOrder);
                  }
                })
            .named("TreeMultimap.asMap.entrySet collection")
            .withFeatures(
                CollectionFeature.ALLOWS_NULL_VALUES,
                CollectionFeature.GENERAL_PURPOSE,
                CollectionFeature.KNOWN_ORDER,
                CollectionSize.ONE,
                CollectionSize.SEVERAL)
            .createTestSuite());
    suite.addTestSuite(TreeMultimapNaturalTest.class);
    return suite;
  }

  protected SetMultimap<String, Integer> create() {
    return TreeMultimap.create();
  }

  /** Create and populate a {@code TreeMultimap} with the natural ordering of keys and values. */
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
    assertEquals("{bar=[1, 2, 3], foo=[-1, 1, 2, 3, 4]}", multimap.toString());
  }

  public void testOrderedGet() {
    TreeMultimap<String, Integer> multimap = createPopulate();
    assertThat(multimap.get("foo")).containsExactly(1, 3, 7).inOrder();
    assertThat(multimap.get("google")).containsExactly(2, 6).inOrder();
    assertThat(multimap.get("tree")).containsExactly(0, 4).inOrder();
  }

  public void testOrderedKeySet() {
    TreeMultimap<String, Integer> multimap = createPopulate();
    assertThat(multimap.keySet()).containsExactly("foo", "google", "tree").inOrder();
  }

  public void testOrderedAsMapEntries() {
    TreeMultimap<String, Integer> multimap = createPopulate();
    Iterator<Entry<String, Collection<Integer>>> iterator = multimap.asMap().entrySet().iterator();
    Entry<String, Collection<Integer>> entry = iterator.next();
    assertEquals("foo", entry.getKey());
    assertThat(entry.getValue()).containsExactly(1, 3, 7);
    entry = iterator.next();
    assertEquals("google", entry.getKey());
    assertThat(entry.getValue()).containsExactly(2, 6);
    entry = iterator.next();
    assertEquals("tree", entry.getKey());
    assertThat(entry.getValue()).containsExactly(0, 4);
  }

  public void testOrderedEntries() {
    TreeMultimap<String, Integer> multimap = createPopulate();
    assertThat(multimap.entries())
        .containsExactly(
            Maps.immutableEntry("foo", 1),
            Maps.immutableEntry("foo", 3),
            Maps.immutableEntry("foo", 7),
            Maps.immutableEntry("google", 2),
            Maps.immutableEntry("google", 6),
            Maps.immutableEntry("tree", 0),
            Maps.immutableEntry("tree", 4))
        .inOrder();
  }

  public void testOrderedValues() {
    TreeMultimap<String, Integer> multimap = createPopulate();
    assertThat(multimap.values()).containsExactly(1, 3, 7, 2, 6, 0, 4).inOrder();
  }

  public void testMultimapConstructor() {
    SetMultimap<String, Integer> multimap = create();
    multimap.putAll("bar", Arrays.asList(3, 1, 2));
    multimap.putAll("foo", Arrays.asList(2, 3, 1, -1, 4));
    TreeMultimap<String, Integer> copy = TreeMultimap.create(multimap);
    assertEquals(multimap, copy);
  }

  private static final Comparator<Double> KEY_COMPARATOR = Ordering.natural();

  private static final Comparator<Double> VALUE_COMPARATOR =
      Ordering.natural().reverse().nullsFirst();

  /**
   * Test that creating one TreeMultimap from another does not copy the comparators from the source
   * TreeMultimap.
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

  /** Test that creating one TreeMultimap from a non-TreeMultimap results in natural ordering. */
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

  /** Test that creating one TreeMultimap from a SortedSetMultimap uses natural ordering. */
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

  @GwtIncompatible // SerializableTester
  public void testExplicitComparatorSerialization() {
    TreeMultimap<String, Integer> multimap = createPopulate();
    TreeMultimap<String, Integer> copy = SerializableTester.reserializeAndAssert(multimap);
    assertThat(copy.values()).containsExactly(1, 3, 7, 2, 6, 0, 4).inOrder();
    assertThat(copy.keySet()).containsExactly("foo", "google", "tree").inOrder();
    assertEquals(multimap.keyComparator(), copy.keyComparator());
    assertEquals(multimap.valueComparator(), copy.valueComparator());
  }

  @GwtIncompatible // SerializableTester
  public void testTreeMultimapDerived() {
    TreeMultimap<DerivedComparable, DerivedComparable> multimap = TreeMultimap.create();
    assertEquals(ImmutableMultimap.of(), multimap);
    multimap.put(new DerivedComparable("foo"), new DerivedComparable("f"));
    multimap.put(new DerivedComparable("foo"), new DerivedComparable("o"));
    multimap.put(new DerivedComparable("foo"), new DerivedComparable("o"));
    multimap.put(new DerivedComparable("bar"), new DerivedComparable("b"));
    multimap.put(new DerivedComparable("bar"), new DerivedComparable("a"));
    multimap.put(new DerivedComparable("bar"), new DerivedComparable("r"));
    assertThat(multimap.keySet())
        .containsExactly(new DerivedComparable("bar"), new DerivedComparable("foo"))
        .inOrder();
    assertThat(multimap.values())
        .containsExactly(
            new DerivedComparable("a"),
            new DerivedComparable("b"),
            new DerivedComparable("r"),
            new DerivedComparable("f"),
            new DerivedComparable("o"))
        .inOrder();
    assertEquals(Ordering.natural(), multimap.keyComparator());
    assertEquals(Ordering.natural(), multimap.valueComparator());
    SerializableTester.reserializeAndAssert(multimap);
  }

  @GwtIncompatible // SerializableTester
  public void testTreeMultimapNonGeneric() {
    TreeMultimap<LegacyComparable, LegacyComparable> multimap = TreeMultimap.create();
    assertEquals(ImmutableMultimap.of(), multimap);
    multimap.put(new LegacyComparable("foo"), new LegacyComparable("f"));
    multimap.put(new LegacyComparable("foo"), new LegacyComparable("o"));
    multimap.put(new LegacyComparable("foo"), new LegacyComparable("o"));
    multimap.put(new LegacyComparable("bar"), new LegacyComparable("b"));
    multimap.put(new LegacyComparable("bar"), new LegacyComparable("a"));
    multimap.put(new LegacyComparable("bar"), new LegacyComparable("r"));
    assertThat(multimap.keySet())
        .containsExactly(new LegacyComparable("bar"), new LegacyComparable("foo"))
        .inOrder();
    assertThat(multimap.values())
        .containsExactly(
            new LegacyComparable("a"),
            new LegacyComparable("b"),
            new LegacyComparable("r"),
            new LegacyComparable("f"),
            new LegacyComparable("o"))
        .inOrder();
    assertEquals(Ordering.natural(), multimap.keyComparator());
    assertEquals(Ordering.natural(), multimap.valueComparator());
    SerializableTester.reserializeAndAssert(multimap);
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
    assertEquals(ImmutableMap.of("google", googleValues, "tree", treeValues), asMap.tailMap("g"));
    assertEquals(ImmutableMap.of("google", googleValues, "foo", fooValues), asMap.headMap("h"));
    assertEquals(ImmutableMap.of("google", googleValues), asMap.subMap("g", "h"));
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

  @GwtIncompatible // reflection
  public void testKeySetBridgeMethods() {
    for (Method m : TreeMultimap.class.getMethods()) {
      if (m.getName().equals("keySet") && m.getReturnType().equals(SortedSet.class)) {
        return;
      }
    }
    fail("No bridge method found");
  }

  @GwtIncompatible // reflection
  public void testAsMapBridgeMethods() {
    for (Method m : TreeMultimap.class.getMethods()) {
      if (m.getName().equals("asMap") && m.getReturnType().equals(SortedMap.class)) {
        return;
      }
    }
  }

  @GwtIncompatible // reflection
  public void testGetBridgeMethods() {
    for (Method m : TreeMultimap.class.getMethods()) {
      if (m.getName().equals("get") && m.getReturnType().equals(SortedSet.class)) {
        return;
      }
    }
    fail("No bridge method found");
  }
}
