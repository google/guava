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
import static com.google.common.collect.Maps.immutableEntry;
import static com.google.common.collect.Sets.newHashSet;
import static com.google.common.collect.testing.Helpers.mapEntry;
import static com.google.common.collect.testing.Helpers.nefariousMapEntry;
import static com.google.common.collect.testing.IteratorFeature.MODIFIABLE;
import static com.google.common.truth.Truth.assertThat;
import static java.util.Arrays.asList;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.base.Equivalence;
import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.base.Predicates;
import com.google.common.base.Supplier;
import com.google.common.collect.Maps.EntryTransformer;
import com.google.common.collect.testing.IteratorTester;
import com.google.common.collect.testing.google.UnmodifiableCollectionTests;
import com.google.common.testing.CollectorTester;
import com.google.common.testing.EqualsTester;
import com.google.common.testing.NullPointerTester;
import com.google.common.testing.SerializableTester;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableSet;
import java.util.Queue;
import java.util.RandomAccess;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.BiPredicate;
import java.util.stream.Collector;
import java.util.stream.Stream;
import junit.framework.TestCase;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Unit test for {@code Multimaps}.
 *
 * @author Jared Levy
 */
@GwtCompatible(emulated = true)
public class MultimapsTest extends TestCase {

  private static final Comparator<Integer> INT_COMPARATOR =
      Ordering.<Integer>natural().reverse().nullsFirst();

  public void testMultimapCollectorGenerics() {
    ListMultimap<Integer, String> unused =
        Stream.of("foo", "bar", "quux")
            .collect(
                Multimaps.toMultimap(
                    String::length, s -> s, MultimapBuilder.treeKeys().arrayListValues()::build));
  }

  public void testToMultimap() {
    Collector<Entry<String, Integer>, ?, TreeMultimap<String, Integer>> collector =
        Multimaps.toMultimap(Entry::getKey, Entry::getValue, TreeMultimap::create);
    BiPredicate<Multimap<?, ?>, Multimap<?, ?>> equivalence =
        Equivalence.equals()
            .onResultOf((Multimap<?, ?> mm) -> ImmutableList.copyOf(mm.asMap().entrySet()))
            .and(Equivalence.equals());
    TreeMultimap<String, Integer> empty = TreeMultimap.create();
    TreeMultimap<String, Integer> filled = TreeMultimap.create();
    filled.put("a", 1);
    filled.put("a", 2);
    filled.put("b", 2);
    filled.put("c", 3);
    CollectorTester.of(collector, equivalence)
        .expectCollects(empty)
        .expectCollects(
            filled, mapEntry("a", 1), mapEntry("a", 2), mapEntry("b", 2), mapEntry("c", 3));
  }

  public void testFlatteningToMultimap() {
    Collector<String, ?, ListMultimap<Character, Character>> collector =
        Multimaps.flatteningToMultimap(
            str -> str.charAt(0),
            str -> str.substring(1).chars().mapToObj(c -> (char) c),
            MultimapBuilder.linkedHashKeys().arrayListValues()::build);
    BiPredicate<Multimap<?, ?>, Multimap<?, ?>> equivalence =
        Equivalence.equals()
            .onResultOf((Multimap<?, ?> mm) -> ImmutableList.copyOf(mm.asMap().entrySet()))
            .and(Equivalence.equals());
    ListMultimap<Character, Character> empty =
        MultimapBuilder.linkedHashKeys().arrayListValues().build();
    ListMultimap<Character, Character> filled =
        MultimapBuilder.linkedHashKeys().arrayListValues().build();
    filled.putAll('b', Arrays.asList('a', 'n', 'a', 'n', 'a'));
    filled.putAll('a', Arrays.asList('p', 'p', 'l', 'e'));
    filled.putAll('c', Arrays.asList('a', 'r', 'r', 'o', 't'));
    filled.putAll('a', Arrays.asList('s', 'p', 'a', 'r', 'a', 'g', 'u', 's'));
    filled.putAll('c', Arrays.asList('h', 'e', 'r', 'r', 'y'));
    CollectorTester.of(collector, equivalence)
        .expectCollects(empty)
        .expectCollects(filled, "banana", "apple", "carrot", "asparagus", "cherry");
  }

  @SuppressWarnings("deprecation")
  public void testUnmodifiableListMultimapShortCircuit() {
    ListMultimap<String, Integer> mod = ArrayListMultimap.create();
    ListMultimap<String, Integer> unmod = Multimaps.unmodifiableListMultimap(mod);
    assertNotSame(mod, unmod);
    assertSame(unmod, Multimaps.unmodifiableListMultimap(unmod));
    ImmutableListMultimap<String, Integer> immutable =
        ImmutableListMultimap.of("a", 1, "b", 2, "a", 3);
    assertSame(immutable, Multimaps.unmodifiableListMultimap(immutable));
    assertSame(
        immutable, Multimaps.unmodifiableListMultimap((ListMultimap<String, Integer>) immutable));
  }

  @SuppressWarnings("deprecation")
  public void testUnmodifiableSetMultimapShortCircuit() {
    SetMultimap<String, Integer> mod = HashMultimap.create();
    SetMultimap<String, Integer> unmod = Multimaps.unmodifiableSetMultimap(mod);
    assertNotSame(mod, unmod);
    assertSame(unmod, Multimaps.unmodifiableSetMultimap(unmod));
    ImmutableSetMultimap<String, Integer> immutable =
        ImmutableSetMultimap.of("a", 1, "b", 2, "a", 3);
    assertSame(immutable, Multimaps.unmodifiableSetMultimap(immutable));
    assertSame(
        immutable, Multimaps.unmodifiableSetMultimap((SetMultimap<String, Integer>) immutable));
  }

  @SuppressWarnings("deprecation")
  public void testUnmodifiableMultimapShortCircuit() {
    Multimap<String, Integer> mod = HashMultimap.create();
    Multimap<String, Integer> unmod = Multimaps.unmodifiableMultimap(mod);
    assertNotSame(mod, unmod);
    assertSame(unmod, Multimaps.unmodifiableMultimap(unmod));
    ImmutableMultimap<String, Integer> immutable = ImmutableMultimap.of("a", 1, "b", 2, "a", 3);
    assertSame(immutable, Multimaps.unmodifiableMultimap(immutable));
    assertSame(immutable, Multimaps.unmodifiableMultimap((Multimap<String, Integer>) immutable));
  }

  @GwtIncompatible // slow (~10s)
  public void testUnmodifiableArrayListMultimap() {
    checkUnmodifiableMultimap(ArrayListMultimap.<String, Integer>create(), true);
  }

  @GwtIncompatible // SerializableTester
  public void testSerializingUnmodifiableArrayListMultimap() {
    Multimap<String, Integer> unmodifiable =
        prepareUnmodifiableTests(ArrayListMultimap.<String, Integer>create(), true, null, null);
    SerializableTester.reserializeAndAssert(unmodifiable);
  }

  public void testUnmodifiableArrayListMultimapRandomAccess() {
    ListMultimap<String, Integer> delegate = ArrayListMultimap.create();
    delegate.put("foo", 1);
    delegate.put("foo", 3);
    ListMultimap<String, Integer> multimap = Multimaps.unmodifiableListMultimap(delegate);
    assertTrue(multimap.get("foo") instanceof RandomAccess);
    assertTrue(multimap.get("bar") instanceof RandomAccess);
  }

  public void testUnmodifiableLinkedListMultimapRandomAccess() {
    ListMultimap<String, Integer> delegate = LinkedListMultimap.create();
    delegate.put("foo", 1);
    delegate.put("foo", 3);
    ListMultimap<String, Integer> multimap = Multimaps.unmodifiableListMultimap(delegate);
    assertFalse(multimap.get("foo") instanceof RandomAccess);
    assertFalse(multimap.get("bar") instanceof RandomAccess);
  }

  @GwtIncompatible // slow (~10s)
  public void testUnmodifiableHashMultimap() {
    checkUnmodifiableMultimap(HashMultimap.<String, Integer>create(), false);
  }

  @GwtIncompatible // SerializableTester
  public void testSerializingUnmodifiableHashMultimap() {
    Multimap<String, Integer> unmodifiable =
        prepareUnmodifiableTests(HashMultimap.<String, Integer>create(), false, null, null);
    SerializableTester.reserializeAndAssert(unmodifiable);
  }

  @GwtIncompatible // slow (~10s)
  public void testUnmodifiableTreeMultimap() {
    checkUnmodifiableMultimap(TreeMultimap.<String, Integer>create(), false, "null", 42);
  }

  @GwtIncompatible // SerializableTester
  public void testSerializingUnmodifiableTreeMultimap() {
    Multimap<String, Integer> unmodifiable =
        prepareUnmodifiableTests(TreeMultimap.<String, Integer>create(), false, "null", 42);
    SerializableTester.reserializeAndAssert(unmodifiable);
  }

  @GwtIncompatible // slow (~10s)
  public void testUnmodifiableSynchronizedArrayListMultimap() {
    checkUnmodifiableMultimap(
        Multimaps.synchronizedListMultimap(ArrayListMultimap.<String, Integer>create()), true);
  }

  @GwtIncompatible // SerializableTester
  public void testSerializingUnmodifiableSynchronizedArrayListMultimap() {
    Multimap<String, Integer> unmodifiable =
        prepareUnmodifiableTests(
            Multimaps.synchronizedListMultimap(ArrayListMultimap.<String, Integer>create()),
            true,
            null,
            null);
    SerializableTester.reserializeAndAssert(unmodifiable);
  }

  @GwtIncompatible // slow (~10s)
  public void testUnmodifiableSynchronizedHashMultimap() {
    checkUnmodifiableMultimap(
        Multimaps.synchronizedSetMultimap(HashMultimap.<String, Integer>create()), false);
  }

  @GwtIncompatible // SerializableTester
  public void testSerializingUnmodifiableSynchronizedHashMultimap() {
    Multimap<String, Integer> unmodifiable =
        prepareUnmodifiableTests(
            Multimaps.synchronizedSetMultimap(HashMultimap.<String, Integer>create()),
            false,
            null,
            null);
    SerializableTester.reserializeAndAssert(unmodifiable);
  }

  @GwtIncompatible // slow (~10s)
  public void testUnmodifiableSynchronizedTreeMultimap() {
    TreeMultimap<String, Integer> delegate =
        TreeMultimap.create(Ordering.<String>natural(), INT_COMPARATOR);
    SortedSetMultimap<String, Integer> multimap = Multimaps.synchronizedSortedSetMultimap(delegate);
    checkUnmodifiableMultimap(multimap, false, "null", 42);
    assertSame(INT_COMPARATOR, multimap.valueComparator());
  }

  @GwtIncompatible // SerializableTester
  public void testSerializingUnmodifiableSynchronizedTreeMultimap() {
    TreeMultimap<String, Integer> delegate =
        TreeMultimap.create(Ordering.<String>natural(), INT_COMPARATOR);
    SortedSetMultimap<String, Integer> multimap = Multimaps.synchronizedSortedSetMultimap(delegate);
    Multimap<String, Integer> unmodifiable = prepareUnmodifiableTests(multimap, false, "null", 42);
    SerializableTester.reserializeAndAssert(unmodifiable);
    assertSame(INT_COMPARATOR, multimap.valueComparator());
  }

  public void testUnmodifiableMultimapIsView() {
    Multimap<String, Integer> mod = HashMultimap.create();
    Multimap<String, Integer> unmod = Multimaps.unmodifiableMultimap(mod);
    assertEquals(mod, unmod);
    mod.put("foo", 1);
    assertTrue(unmod.containsEntry("foo", 1));
    assertEquals(mod, unmod);
  }

  @SuppressWarnings("unchecked")
  public void testUnmodifiableMultimapEntries() {
    Multimap<String, Integer> mod = HashMultimap.create();
    Multimap<String, Integer> unmod = Multimaps.unmodifiableMultimap(mod);
    mod.put("foo", 1);
    Entry<String, Integer> entry = unmod.entries().iterator().next();
    try {
      entry.setValue(2);
      fail("UnsupportedOperationException expected");
    } catch (UnsupportedOperationException expected) {
    }
    entry = (Entry<String, Integer>) unmod.entries().toArray()[0];
    try {
      entry.setValue(2);
      fail("UnsupportedOperationException expected");
    } catch (UnsupportedOperationException expected) {
    }
    Entry<String, Integer>[] array = (Entry<String, Integer>[]) new Entry<?, ?>[2];
    assertSame(array, unmod.entries().toArray(array));
    try {
      array[0].setValue(2);
      fail("UnsupportedOperationException expected");
    } catch (UnsupportedOperationException expected) {
    }
    assertFalse(unmod.entries().contains(nefariousMapEntry("pwnd", 2)));
    assertFalse(unmod.keys().contains("pwnd"));
  }

  /**
   * The supplied multimap will be mutated and an unmodifiable instance used in its stead. The
   * multimap must support null keys and values.
   */
  private static void checkUnmodifiableMultimap(
      Multimap<String, Integer> multimap, boolean permitsDuplicates) {
    checkUnmodifiableMultimap(multimap, permitsDuplicates, null, null);
  }

  /**
   * The supplied multimap will be mutated and an unmodifiable instance used in its stead. If the
   * multimap does not support null keys or values, alternatives may be specified for tests
   * involving nulls.
   */
  private static void checkUnmodifiableMultimap(
      Multimap<String, Integer> multimap,
      boolean permitsDuplicates,
      @Nullable String nullKey,
      @Nullable Integer nullValue) {
    Multimap<String, Integer> unmodifiable =
        prepareUnmodifiableTests(multimap, permitsDuplicates, nullKey, nullValue);

    UnmodifiableCollectionTests.assertMultimapIsUnmodifiable(unmodifiable, "test", 123);

    assertUnmodifiableIterableInTandem(unmodifiable.keys(), multimap.keys());

    assertUnmodifiableIterableInTandem(unmodifiable.keySet(), multimap.keySet());

    assertUnmodifiableIterableInTandem(unmodifiable.entries(), multimap.entries());

    assertUnmodifiableIterableInTandem(
        unmodifiable.asMap().entrySet(), multimap.asMap().entrySet());

    assertEquals(multimap.toString(), unmodifiable.toString());
    assertEquals(multimap.hashCode(), unmodifiable.hashCode());
    assertEquals(multimap, unmodifiable);

    assertThat(unmodifiable.asMap().get("bar")).containsExactly(5, -1);
    assertNull(unmodifiable.asMap().get("missing"));

    assertFalse(unmodifiable.entries() instanceof Serializable);
  }

  /** Prepares the multimap for unmodifiable tests, returning an unmodifiable view of the map. */
  private static Multimap<String, Integer> prepareUnmodifiableTests(
      Multimap<String, Integer> multimap,
      boolean permitsDuplicates,
      @Nullable String nullKey,
      @Nullable Integer nullValue) {
    multimap.clear();
    multimap.put("foo", 1);
    multimap.put("foo", 2);
    multimap.put("foo", 3);
    multimap.put("bar", 5);
    multimap.put("bar", -1);
    multimap.put(nullKey, nullValue);
    multimap.put("foo", nullValue);
    multimap.put(nullKey, 5);
    multimap.put("foo", 2);

    if (permitsDuplicates) {
      assertEquals(9, multimap.size());
    } else {
      assertEquals(8, multimap.size());
    }

    Multimap<String, Integer> unmodifiable;
    if (multimap instanceof SortedSetMultimap) {
      unmodifiable =
          Multimaps.unmodifiableSortedSetMultimap((SortedSetMultimap<String, Integer>) multimap);
    } else if (multimap instanceof SetMultimap) {
      unmodifiable = Multimaps.unmodifiableSetMultimap((SetMultimap<String, Integer>) multimap);
    } else if (multimap instanceof ListMultimap) {
      unmodifiable = Multimaps.unmodifiableListMultimap((ListMultimap<String, Integer>) multimap);
    } else {
      unmodifiable = Multimaps.unmodifiableMultimap(multimap);
    }
    return unmodifiable;
  }

  private static <T> void assertUnmodifiableIterableInTandem(
      Iterable<T> unmodifiable, Iterable<T> modifiable) {
    UnmodifiableCollectionTests.assertIteratorIsUnmodifiable(unmodifiable.iterator());
    UnmodifiableCollectionTests.assertIteratorsInOrder(
        unmodifiable.iterator(), modifiable.iterator());
  }

  public void testInvertFrom() {
    ImmutableMultimap<Integer, String> empty = ImmutableMultimap.of();

    // typical usage example - sad that ArrayListMultimap.create() won't work
    Multimap<String, Integer> multimap =
        Multimaps.invertFrom(empty, ArrayListMultimap.<String, Integer>create());
    assertTrue(multimap.isEmpty());

    ImmutableMultimap<Integer, String> single =
        new ImmutableMultimap.Builder<Integer, String>().put(1, "one").put(2, "two").build();

    // copy into existing multimap
    assertSame(multimap, Multimaps.invertFrom(single, multimap));

    ImmutableMultimap<String, Integer> expected =
        new ImmutableMultimap.Builder<String, Integer>().put("one", 1).put("two", 2).build();

    assertEquals(expected, multimap);
  }

  public void testAsMap_multimap() {
    Multimap<String, Integer> multimap =
        Multimaps.newMultimap(new HashMap<String, Collection<Integer>>(), new QueueSupplier());
    Map<String, Collection<Integer>> map = Multimaps.asMap(multimap);
    assertSame(multimap.asMap(), map);
  }

  public void testAsMap_listMultimap() {
    ListMultimap<String, Integer> listMultimap = ArrayListMultimap.create();
    Map<String, List<Integer>> map = Multimaps.asMap(listMultimap);
    assertSame(listMultimap.asMap(), map);
  }

  public void testAsMap_setMultimap() {
    SetMultimap<String, Integer> setMultimap = LinkedHashMultimap.create();
    Map<String, Set<Integer>> map = Multimaps.asMap(setMultimap);
    assertSame(setMultimap.asMap(), map);
  }

  public void testAsMap_sortedSetMultimap() {
    SortedSetMultimap<String, Integer> sortedSetMultimap = TreeMultimap.create();
    Map<String, SortedSet<Integer>> map = Multimaps.asMap(sortedSetMultimap);
    assertSame(sortedSetMultimap.asMap(), map);
  }

  public void testForMap() {
    Map<String, Integer> map = Maps.newHashMap();
    map.put("foo", 1);
    map.put("bar", 2);
    Multimap<String, Integer> multimap = HashMultimap.create();
    multimap.put("foo", 1);
    multimap.put("bar", 2);
    Multimap<String, Integer> multimapView = Multimaps.forMap(map);
    new EqualsTester().addEqualityGroup(multimap, multimapView).addEqualityGroup(map).testEquals();
    Multimap<String, Integer> multimap2 = HashMultimap.create();
    multimap2.put("foo", 1);
    assertFalse(multimapView.equals(multimap2));
    multimap2.put("bar", 1);
    assertFalse(multimapView.equals(multimap2));
    ListMultimap<String, Integer> listMultimap =
        new ImmutableListMultimap.Builder<String, Integer>().put("foo", 1).put("bar", 2).build();
    assertFalse("SetMultimap equals ListMultimap", multimapView.equals(listMultimap));
    assertEquals(multimap.hashCode(), multimapView.hashCode());
    assertEquals(multimap.size(), multimapView.size());
    assertTrue(multimapView.containsKey("foo"));
    assertTrue(multimapView.containsValue(1));
    assertTrue(multimapView.containsEntry("bar", 2));
    assertEquals(Collections.singleton(1), multimapView.get("foo"));
    assertEquals(Collections.singleton(2), multimapView.get("bar"));
    try {
      multimapView.put("baz", 3);
      fail("UnsupportedOperationException expected");
    } catch (UnsupportedOperationException expected) {
    }
    try {
      multimapView.putAll("baz", Collections.singleton(3));
      fail("UnsupportedOperationException expected");
    } catch (UnsupportedOperationException expected) {
    }
    try {
      multimapView.putAll(multimap);
      fail("UnsupportedOperationException expected");
    } catch (UnsupportedOperationException expected) {
    }
    try {
      multimapView.replaceValues("foo", Collections.<Integer>emptySet());
      fail("UnsupportedOperationException expected");
    } catch (UnsupportedOperationException expected) {
    }
    multimapView.remove("bar", 2);
    assertFalse(multimapView.containsKey("bar"));
    assertFalse(map.containsKey("bar"));
    assertEquals(map.keySet(), multimapView.keySet());
    assertEquals(map.keySet(), multimapView.keys().elementSet());
    assertThat(multimapView.keys()).contains("foo");
    assertThat(multimapView.values()).contains(1);
    assertThat(multimapView.entries()).contains(Maps.immutableEntry("foo", 1));
    assertThat(multimapView.asMap().entrySet())
        .contains(Maps.immutableEntry("foo", (Collection<Integer>) Collections.singleton(1)));
    multimapView.clear();
    assertFalse(multimapView.containsKey("foo"));
    assertFalse(map.containsKey("foo"));
    assertTrue(map.isEmpty());
    assertTrue(multimapView.isEmpty());
    multimap.clear();
    assertEquals(multimap.toString(), multimapView.toString());
    assertEquals(multimap.hashCode(), multimapView.hashCode());
    assertEquals(multimap.size(), multimapView.size());
    assertEquals(multimapView, ArrayListMultimap.create());
  }

  @GwtIncompatible // SerializableTester
  public void testForMapSerialization() {
    Map<String, Integer> map = Maps.newHashMap();
    map.put("foo", 1);
    map.put("bar", 2);
    Multimap<String, Integer> multimapView = Multimaps.forMap(map);
    SerializableTester.reserializeAndAssert(multimapView);
  }

  public void testForMapRemoveAll() {
    Map<String, Integer> map = Maps.newHashMap();
    map.put("foo", 1);
    map.put("bar", 2);
    map.put("cow", 3);
    Multimap<String, Integer> multimap = Multimaps.forMap(map);
    assertEquals(3, multimap.size());
    assertEquals(Collections.emptySet(), multimap.removeAll("dog"));
    assertEquals(3, multimap.size());
    assertTrue(multimap.containsKey("bar"));
    assertEquals(Collections.singleton(2), multimap.removeAll("bar"));
    assertEquals(2, multimap.size());
    assertFalse(multimap.containsKey("bar"));
  }

  public void testForMapAsMap() {
    Map<String, Integer> map = Maps.newHashMap();
    map.put("foo", 1);
    map.put("bar", 2);
    Map<String, Collection<Integer>> asMap = Multimaps.forMap(map).asMap();
    assertEquals(Collections.singleton(1), asMap.get("foo"));
    assertNull(asMap.get("cow"));
    assertTrue(asMap.containsKey("foo"));
    assertFalse(asMap.containsKey("cow"));

    Set<Entry<String, Collection<Integer>>> entries = asMap.entrySet();
    assertFalse(entries.contains((Object) 4.5));
    assertFalse(entries.remove((Object) 4.5));
    assertFalse(entries.contains(Maps.immutableEntry("foo", Collections.singletonList(1))));
    assertFalse(entries.remove(Maps.immutableEntry("foo", Collections.singletonList(1))));
    assertFalse(entries.contains(Maps.immutableEntry("foo", Sets.newLinkedHashSet(asList(1, 2)))));
    assertFalse(entries.remove(Maps.immutableEntry("foo", Sets.newLinkedHashSet(asList(1, 2)))));
    assertFalse(entries.contains(Maps.immutableEntry("foo", Collections.singleton(2))));
    assertFalse(entries.remove(Maps.immutableEntry("foo", Collections.singleton(2))));
    assertTrue(map.containsKey("foo"));
    assertTrue(entries.contains(Maps.immutableEntry("foo", Collections.singleton(1))));
    assertTrue(entries.remove(Maps.immutableEntry("foo", Collections.singleton(1))));
    assertFalse(map.containsKey("foo"));
  }

  public void testForMapGetIteration() {
    IteratorTester<Integer> tester =
        new IteratorTester<Integer>(
            4, MODIFIABLE, newHashSet(1), IteratorTester.KnownOrder.KNOWN_ORDER) {
          private Multimap<String, Integer> multimap;

          @Override
          protected Iterator<Integer> newTargetIterator() {
            Map<String, Integer> map = Maps.newHashMap();
            map.put("foo", 1);
            map.put("bar", 2);
            multimap = Multimaps.forMap(map);
            return multimap.get("foo").iterator();
          }

          @Override
          protected void verify(List<Integer> elements) {
            assertEquals(newHashSet(elements), multimap.get("foo"));
          }
        };

    tester.test();
  }

  private enum Color {
    BLUE,
    RED,
    YELLOW,
    GREEN
  }

  private abstract static class CountingSupplier<E> implements Supplier<E>, Serializable {
    int count;

    abstract E getImpl();

    @Override
    public E get() {
      count++;
      return getImpl();
    }
  }

  private static class QueueSupplier extends CountingSupplier<Queue<Integer>> {
    @Override
    public Queue<Integer> getImpl() {
      return new LinkedList<>();
    }

    private static final long serialVersionUID = 0;
  }

  public void testNewMultimapWithCollectionRejectingNegativeElements() {
    CountingSupplier<Set<Integer>> factory =
        new SetSupplier() {
          @Override
          public Set<Integer> getImpl() {
            final Set<Integer> backing = super.getImpl();
            return new ForwardingSet<Integer>() {
              @Override
              protected Set<Integer> delegate() {
                return backing;
              }

              @Override
              public boolean add(Integer element) {
                checkArgument(element >= 0);
                return super.add(element);
              }

              @Override
              public boolean addAll(Collection<? extends Integer> collection) {
                return standardAddAll(collection);
              }
            };
          }
        };

    Map<Color, Collection<Integer>> map = Maps.newEnumMap(Color.class);
    Multimap<Color, Integer> multimap = Multimaps.newMultimap(map, factory);
    try {
      multimap.put(Color.BLUE, -1);
      fail("Expected IllegalArgumentException");
    } catch (IllegalArgumentException expected) {
    }
    multimap.put(Color.RED, 1);
    multimap.put(Color.BLUE, 2);
    try {
      multimap.put(Color.GREEN, -1);
      fail("Expected IllegalArgumentException");
    } catch (IllegalArgumentException expected) {
    }
    assertThat(multimap.entries())
        .containsExactly(Maps.immutableEntry(Color.RED, 1), Maps.immutableEntry(Color.BLUE, 2));
  }

  public void testNewMultimap() {
    // The ubiquitous EnumArrayBlockingQueueMultimap
    CountingSupplier<Queue<Integer>> factory = new QueueSupplier();

    Map<Color, Collection<Integer>> map = Maps.newEnumMap(Color.class);
    Multimap<Color, Integer> multimap = Multimaps.newMultimap(map, factory);
    assertEquals(0, factory.count);
    multimap.putAll(Color.BLUE, asList(3, 1, 4));
    assertEquals(1, factory.count);
    multimap.putAll(Color.RED, asList(2, 7, 1, 8));
    assertEquals(2, factory.count);
    assertEquals("[3, 1, 4]", multimap.get(Color.BLUE).toString());

    Multimap<Color, Integer> ummodifiable = Multimaps.unmodifiableMultimap(multimap);
    assertEquals("[3, 1, 4]", ummodifiable.get(Color.BLUE).toString());

    Collection<Integer> collection = multimap.get(Color.BLUE);
    // Explicitly call `equals`; `assertEquals` might return fast
    assertTrue(collection.equals(collection));

    assertFalse(multimap.keySet() instanceof SortedSet);
    assertFalse(multimap.asMap() instanceof SortedMap);
  }

  public void testNewMultimapValueCollectionMatchesNavigableSet() {
    Supplier<TreeSet<Integer>> factory = new SortedSetSupplier();
    Map<Color, Collection<Integer>> map = Maps.newEnumMap(Color.class);
    Multimap<Color, Integer> multimap = Multimaps.newMultimap(map, factory);
    assertTrue(multimap.get(Color.BLUE) instanceof NavigableSet);
  }

  public void testNewMultimapValueCollectionMatchesList() {
    Supplier<LinkedList<Integer>> factory = new ListSupplier();
    Map<Color, Collection<Integer>> map = Maps.newEnumMap(Color.class);
    Multimap<Color, Integer> multimap = Multimaps.newMultimap(map, factory);
    assertTrue(multimap.get(Color.BLUE) instanceof List);
  }

  @GwtIncompatible // SerializableTester
  public void testNewMultimapSerialization() {
    CountingSupplier<Queue<Integer>> factory = new QueueSupplier();
    Map<Color, Collection<Integer>> map = Maps.newEnumMap(Color.class);
    Multimap<Color, Integer> multimap = Multimaps.newMultimap(map, factory);
    multimap.putAll(Color.BLUE, asList(3, 1, 4));
    multimap.putAll(Color.RED, asList(2, 7, 1, 8));
    SerializableTester.reserializeAndAssert(multimap);
  }

  private static class ListSupplier extends CountingSupplier<LinkedList<Integer>> {
    @Override
    public LinkedList<Integer> getImpl() {
      return new LinkedList<>();
    }

    private static final long serialVersionUID = 0;
  }

  public void testNewListMultimap() {
    CountingSupplier<LinkedList<Integer>> factory = new ListSupplier();
    Map<Color, Collection<Integer>> map = Maps.newTreeMap();
    ListMultimap<Color, Integer> multimap = Multimaps.newListMultimap(map, factory);
    assertEquals(0, factory.count);
    multimap.putAll(Color.BLUE, asList(3, 1, 4, 1));
    assertEquals(1, factory.count);
    multimap.putAll(Color.RED, asList(2, 7, 1, 8));
    assertEquals(2, factory.count);
    assertEquals("{BLUE=[3, 1, 4, 1], RED=[2, 7, 1, 8]}", multimap.toString());
    assertFalse(multimap.get(Color.BLUE) instanceof RandomAccess);

    assertTrue(multimap.keySet() instanceof SortedSet);
    assertTrue(multimap.asMap() instanceof SortedMap);
  }

  @GwtIncompatible // SerializableTester
  public void testNewListMultimapSerialization() {
    CountingSupplier<LinkedList<Integer>> factory = new ListSupplier();
    Map<Color, Collection<Integer>> map = Maps.newTreeMap();
    ListMultimap<Color, Integer> multimap = Multimaps.newListMultimap(map, factory);
    multimap.putAll(Color.BLUE, asList(3, 1, 4, 1));
    multimap.putAll(Color.RED, asList(2, 7, 1, 8));
    SerializableTester.reserializeAndAssert(multimap);
  }

  private static class SetSupplier extends CountingSupplier<Set<Integer>> {
    @Override
    public Set<Integer> getImpl() {
      return new HashSet<>(4);
    }

    private static final long serialVersionUID = 0;
  }

  public void testNewSetMultimap() {
    CountingSupplier<Set<Integer>> factory = new SetSupplier();
    Map<Color, Collection<Integer>> map = Maps.newHashMap();
    SetMultimap<Color, Integer> multimap = Multimaps.newSetMultimap(map, factory);
    assertEquals(0, factory.count);
    multimap.putAll(Color.BLUE, asList(3, 1, 4));
    assertEquals(1, factory.count);
    multimap.putAll(Color.RED, asList(2, 7, 1, 8));
    assertEquals(2, factory.count);
    assertEquals(Sets.newHashSet(4, 3, 1), multimap.get(Color.BLUE));
  }

  @GwtIncompatible // SerializableTester
  public void testNewSetMultimapSerialization() {
    CountingSupplier<Set<Integer>> factory = new SetSupplier();
    Map<Color, Collection<Integer>> map = Maps.newHashMap();
    SetMultimap<Color, Integer> multimap = Multimaps.newSetMultimap(map, factory);
    multimap.putAll(Color.BLUE, asList(3, 1, 4));
    multimap.putAll(Color.RED, asList(2, 7, 1, 8));
    SerializableTester.reserializeAndAssert(multimap);
  }

  private static class SortedSetSupplier extends CountingSupplier<TreeSet<Integer>> {
    @Override
    public TreeSet<Integer> getImpl() {
      return Sets.newTreeSet(INT_COMPARATOR);
    }

    private static final long serialVersionUID = 0;
  }

  public void testNewSortedSetMultimap() {
    CountingSupplier<TreeSet<Integer>> factory = new SortedSetSupplier();
    Map<Color, Collection<Integer>> map = Maps.newEnumMap(Color.class);
    SortedSetMultimap<Color, Integer> multimap = Multimaps.newSortedSetMultimap(map, factory);
    // newSortedSetMultimap calls the factory once to determine the comparator.
    assertEquals(1, factory.count);
    multimap.putAll(Color.BLUE, asList(3, 1, 4));
    assertEquals(2, factory.count);
    multimap.putAll(Color.RED, asList(2, 7, 1, 8));
    assertEquals(3, factory.count);
    assertEquals("[4, 3, 1]", multimap.get(Color.BLUE).toString());
    assertEquals(INT_COMPARATOR, multimap.valueComparator());
  }

  @GwtIncompatible // SerializableTester
  public void testNewSortedSetMultimapSerialization() {
    CountingSupplier<TreeSet<Integer>> factory = new SortedSetSupplier();
    Map<Color, Collection<Integer>> map = Maps.newEnumMap(Color.class);
    SortedSetMultimap<Color, Integer> multimap = Multimaps.newSortedSetMultimap(map, factory);
    multimap.putAll(Color.BLUE, asList(3, 1, 4));
    multimap.putAll(Color.RED, asList(2, 7, 1, 8));
    SerializableTester.reserializeAndAssert(multimap);
    assertEquals(INT_COMPARATOR, multimap.valueComparator());
  }

  public void testIndex() {
    final Multimap<String, Object> stringToObject =
        new ImmutableMultimap.Builder<String, Object>()
            .put("1", 1)
            .put("1", 1L)
            .put("1", "1")
            .put("2", 2)
            .put("2", 2L)
            .build();

    ImmutableMultimap<String, Object> outputMap =
        Multimaps.index(stringToObject.values(), Functions.toStringFunction());
    assertEquals(stringToObject, outputMap);
  }

  public void testIndexIterator() {
    final Multimap<String, Object> stringToObject =
        new ImmutableMultimap.Builder<String, Object>()
            .put("1", 1)
            .put("1", 1L)
            .put("1", "1")
            .put("2", 2)
            .put("2", 2L)
            .build();

    ImmutableMultimap<String, Object> outputMap =
        Multimaps.index(stringToObject.values().iterator(), Functions.toStringFunction());
    assertEquals(stringToObject, outputMap);
  }

  public void testIndex_ordering() {
    final Multimap<Integer, String> expectedIndex =
        new ImmutableListMultimap.Builder<Integer, String>()
            .put(4, "Inky")
            .put(6, "Blinky")
            .put(5, "Pinky")
            .put(5, "Pinky")
            .put(5, "Clyde")
            .build();

    final List<String> badGuys = Arrays.asList("Inky", "Blinky", "Pinky", "Pinky", "Clyde");
    final Function<String, Integer> stringLengthFunction =
        new Function<String, Integer>() {
          @Override
          public Integer apply(String input) {
            return input.length();
          }
        };

    Multimap<Integer, String> index = Multimaps.index(badGuys, stringLengthFunction);

    assertEquals(expectedIndex, index);
  }

  public void testIndex_nullValue() {
    List<Integer> values = Arrays.asList(1, null);
    try {
      Multimaps.index(values, Functions.identity());
      fail();
    } catch (NullPointerException expected) {
    }
  }

  public void testIndex_nullKey() {
    List<Integer> values = Arrays.asList(1, 2);
    try {
      Multimaps.index(values, Functions.constant(null));
      fail();
    } catch (NullPointerException expected) {
    }
  }

  @GwtIncompatible(value = "untested")
  public void testTransformValues() {
    SetMultimap<String, Integer> multimap =
        ImmutableSetMultimap.of("a", 2, "b", -3, "b", 3, "a", 4, "c", 6);
    Function<Integer, Integer> square =
        new Function<Integer, Integer>() {
          @Override
          public Integer apply(Integer in) {
            return in * in;
          }
        };
    Multimap<String, Integer> transformed = Multimaps.transformValues(multimap, square);
    assertThat(transformed.entries())
        .containsExactly(
            immutableEntry("a", 4),
            immutableEntry("a", 16),
            immutableEntry("b", 9),
            immutableEntry("b", 9),
            immutableEntry("c", 36))
        .inOrder();
  }

  @GwtIncompatible(value = "untested")
  public void testTransformValuesIsView() {
    Multimap<String, String> multimap = LinkedListMultimap.create();
    multimap.put("a", "a");
    Multimap<String, Integer> transformed =
        Multimaps.transformValues(
            multimap,
            new Function<String, Integer>() {

              @Override
              public Integer apply(String str) {
                return str.length();
              }
            });
    Entry<String, String> entry = multimap.entries().iterator().next();
    entry.setValue("bbb");
    assertThat(transformed.entries()).containsExactly(immutableEntry("a", 3));
  }

  @GwtIncompatible(value = "untested")
  public void testTransformListValues() {
    ListMultimap<String, Integer> multimap =
        ImmutableListMultimap.of("a", 2, "b", -3, "b", 3, "a", 4, "c", 6);
    Function<Integer, Integer> square =
        new Function<Integer, Integer>() {
          @Override
          public Integer apply(Integer in) {
            return in * in;
          }
        };
    ListMultimap<String, Integer> transformed = Multimaps.transformValues(multimap, square);
    assertThat(transformed.entries())
        .containsExactly(
            immutableEntry("a", 4),
            immutableEntry("a", 16),
            immutableEntry("b", 9),
            immutableEntry("b", 9),
            immutableEntry("c", 36))
        .inOrder();
  }

  @GwtIncompatible(value = "untested")
  public void testTransformEntries() {
    SetMultimap<String, Integer> multimap = ImmutableSetMultimap.of("a", 1, "a", 4, "b", -6);
    EntryTransformer<String, Integer, String> transformer =
        new EntryTransformer<String, Integer, String>() {
          @Override
          public String transformEntry(String key, Integer value) {
            return (value >= 0) ? key : "no" + key;
          }
        };
    Multimap<String, String> transformed = Multimaps.transformEntries(multimap, transformer);
    assertThat(transformed.entries())
        .containsExactly(
            immutableEntry("a", "a"), immutableEntry("a", "a"), immutableEntry("b", "nob"))
        .inOrder();
  }

  @GwtIncompatible(value = "untested")
  public void testTransformListEntries() {
    ListMultimap<String, Integer> multimap =
        ImmutableListMultimap.of("a", 1, "a", 4, "b", 6, "a", 4);
    EntryTransformer<String, Integer, String> transformer =
        new EntryTransformer<String, Integer, String>() {
          @Override
          public String transformEntry(String key, Integer value) {
            return key + value;
          }
        };
    ListMultimap<String, String> transformed = Multimaps.transformEntries(multimap, transformer);
    assertEquals(ImmutableListMultimap.of("a", "a1", "a", "a4", "a", "a4", "b", "b6"), transformed);
    assertEquals("{a=[a1, a4, a4], b=[b6]}", transformed.toString());
  }

  public <K, V> void testSynchronizedMultimapSampleCodeCompilation() {
    K key = null;

    Multimap<K, V> multimap = Multimaps.synchronizedMultimap(HashMultimap.<K, V>create());
    Collection<V> values = multimap.get(key); // Needn't be in synchronized block
    synchronized (multimap) { // Synchronizing on multimap, not values!
      Iterator<V> i = values.iterator(); // Must be in synchronized block
      while (i.hasNext()) {
        foo(i.next());
      }
    }
  }

  private static void foo(Object o) {}

  public void testFilteredKeysSetMultimapReplaceValues() {
    SetMultimap<String, Integer> multimap = LinkedHashMultimap.create();
    multimap.put("foo", 1);
    multimap.put("bar", 2);
    multimap.put("baz", 3);
    multimap.put("bar", 4);

    SetMultimap<String, Integer> filtered =
        Multimaps.filterKeys(multimap, Predicates.in(ImmutableSet.of("foo", "bar")));

    assertEquals(ImmutableSet.of(), filtered.replaceValues("baz", ImmutableSet.<Integer>of()));

    try {
      filtered.replaceValues("baz", ImmutableSet.of(5));
      fail("Expected IllegalArgumentException");
    } catch (IllegalArgumentException expected) {
    }
  }

  public void testFilteredKeysSetMultimapGetBadValue() {
    SetMultimap<String, Integer> multimap = LinkedHashMultimap.create();
    multimap.put("foo", 1);
    multimap.put("bar", 2);
    multimap.put("baz", 3);
    multimap.put("bar", 4);

    SetMultimap<String, Integer> filtered =
        Multimaps.filterKeys(multimap, Predicates.in(ImmutableSet.of("foo", "bar")));
    Set<Integer> bazSet = filtered.get("baz");
    assertThat(bazSet).isEmpty();
    try {
      bazSet.add(5);
      fail("Expected IllegalArgumentException");
    } catch (IllegalArgumentException expected) {
    }
    try {
      bazSet.addAll(ImmutableSet.of(6, 7));
      fail("Expected IllegalArgumentException");
    } catch (IllegalArgumentException expected) {
    }
  }

  public void testFilteredKeysListMultimapGetBadValue() {
    ListMultimap<String, Integer> multimap = ArrayListMultimap.create();
    multimap.put("foo", 1);
    multimap.put("bar", 2);
    multimap.put("baz", 3);
    multimap.put("bar", 4);

    ListMultimap<String, Integer> filtered =
        Multimaps.filterKeys(multimap, Predicates.in(ImmutableSet.of("foo", "bar")));
    List<Integer> bazList = filtered.get("baz");
    assertThat(bazList).isEmpty();
    try {
      bazList.add(5);
      fail("Expected IllegalArgumentException");
    } catch (IllegalArgumentException expected) {
    }
    try {
      bazList.add(0, 6);
      fail("Expected IllegalArgumentException");
    } catch (IllegalArgumentException expected) {
    }
    try {
      bazList.addAll(ImmutableList.of(7, 8));
      fail("Expected IllegalArgumentException");
    } catch (IllegalArgumentException expected) {
    }
    try {
      bazList.addAll(0, ImmutableList.of(9, 10));
      fail("Expected IllegalArgumentException");
    } catch (IllegalArgumentException expected) {
    }
  }

  @GwtIncompatible // NullPointerTester
  public void testNullPointers() {
    new NullPointerTester().testAllPublicStaticMethods(Multimaps.class);
  }
}
