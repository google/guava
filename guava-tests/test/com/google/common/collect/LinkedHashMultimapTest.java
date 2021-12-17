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

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.newHashSet;
import static com.google.common.collect.Sets.newLinkedHashSet;
import static com.google.common.collect.testing.Helpers.mapEntry;
import static com.google.common.collect.testing.IteratorFeature.MODIFIABLE;
import static com.google.common.truth.Truth.assertThat;
import static java.util.Arrays.asList;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.collect.testing.IteratorTester;
import com.google.common.collect.testing.features.CollectionFeature;
import com.google.common.collect.testing.features.CollectionSize;
import com.google.common.collect.testing.features.MapFeature;
import com.google.common.collect.testing.google.SetMultimapTestSuiteBuilder;
import com.google.common.collect.testing.google.TestStringSetMultimapGenerator;
import com.google.common.testing.EqualsTester;
import com.google.common.testing.SerializableTester;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Unit tests for {@code LinkedHashMultimap}.
 *
 * @author Jared Levy
 */
@GwtCompatible(emulated = true)
public class LinkedHashMultimapTest extends TestCase {

  @GwtIncompatible // suite
  public static Test suite() {
    TestSuite suite = new TestSuite();
    suite.addTest(
        SetMultimapTestSuiteBuilder.using(
                new TestStringSetMultimapGenerator() {
                  @Override
                  protected SetMultimap<String, String> create(Entry<String, String>[] entries) {
                    SetMultimap<String, String> multimap = LinkedHashMultimap.create();
                    for (Entry<String, String> entry : entries) {
                      multimap.put(entry.getKey(), entry.getValue());
                    }
                    return multimap;
                  }
                })
            .named("LinkedHashMultimap")
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
    suite.addTestSuite(LinkedHashMultimapTest.class);
    return suite;
  }

  public void testValueSetHashTableExpansion() {
    LinkedHashMultimap<String, Integer> multimap = LinkedHashMultimap.create();
    for (int z = 1; z <= 100; z++) {
      multimap.put("a", z);
      // The Eclipse compiler (and hence GWT) rejects a parameterized cast.
      @SuppressWarnings("unchecked")
      LinkedHashMultimap<String, Integer>.ValueSet valueSet =
          (LinkedHashMultimap.ValueSet) multimap.backingMap().get("a");
      assertEquals(z, valueSet.size());
      assertFalse(
          Hashing.needsResizing(
              valueSet.size(),
              valueSet.hashTable.length,
              LinkedHashMultimap.VALUE_SET_LOAD_FACTOR));
    }
  }

  private Multimap<String, Integer> initializeMultimap5() {
    Multimap<String, Integer> multimap = LinkedHashMultimap.create();
    multimap.put("foo", 5);
    multimap.put("bar", 4);
    multimap.put("foo", 3);
    multimap.put("cow", 2);
    multimap.put("bar", 1);
    return multimap;
  }

  public void testToString() {
    Multimap<String, Integer> multimap = LinkedHashMultimap.create();
    multimap.put("foo", 3);
    multimap.put("bar", 1);
    multimap.putAll("foo", Arrays.asList(-1, 2, 4));
    multimap.putAll("bar", Arrays.asList(2, 3));
    multimap.put("foo", 1);
    assertEquals("{foo=[3, -1, 2, 4, 1], bar=[1, 2, 3]}", multimap.toString());
  }

  public void testOrderingReadOnly() {
    Multimap<String, Integer> multimap = initializeMultimap5();
    assertOrderingReadOnly(multimap);
  }

  public void testOrderingUnmodifiable() {
    Multimap<String, Integer> multimap = initializeMultimap5();
    assertOrderingReadOnly(Multimaps.unmodifiableMultimap(multimap));
  }

  public void testOrderingSynchronized() {
    Multimap<String, Integer> multimap = initializeMultimap5();
    assertOrderingReadOnly(Multimaps.synchronizedMultimap(multimap));
  }

  @GwtIncompatible // SeriazableTester
  public void testSerializationOrdering() {
    Multimap<String, Integer> multimap = initializeMultimap5();
    Multimap<String, Integer> copy = SerializableTester.reserializeAndAssert(multimap);
    assertOrderingReadOnly(copy);
  }

  @GwtIncompatible // SeriazableTester
  public void testSerializationOrderingKeysAndEntries() {
    Multimap<String, Integer> multimap = LinkedHashMultimap.create();
    multimap.put("a", 1);
    multimap.put("b", 2);
    multimap.put("a", 3);
    multimap.put("c", 4);
    multimap.remove("a", 1);
    multimap = SerializableTester.reserializeAndAssert(multimap);
    assertThat(multimap.keySet()).containsExactly("a", "b", "c").inOrder();
    assertThat(multimap.entries())
        .containsExactly(mapEntry("b", 2), mapEntry("a", 3), mapEntry("c", 4))
        .inOrder();
    // note that the keys and entries are in different orders
  }

  private void assertOrderingReadOnly(Multimap<String, Integer> multimap) {
    assertThat(multimap.get("foo")).containsExactly(5, 3).inOrder();
    assertThat(multimap.get("bar")).containsExactly(4, 1).inOrder();
    assertThat(multimap.get("cow")).contains(2);

    assertThat(multimap.keySet()).containsExactly("foo", "bar", "cow").inOrder();
    assertThat(multimap.values()).containsExactly(5, 4, 3, 2, 1).inOrder();

    Iterator<Entry<String, Integer>> entryIterator = multimap.entries().iterator();
    assertEquals(Maps.immutableEntry("foo", 5), entryIterator.next());
    assertEquals(Maps.immutableEntry("bar", 4), entryIterator.next());
    assertEquals(Maps.immutableEntry("foo", 3), entryIterator.next());
    assertEquals(Maps.immutableEntry("cow", 2), entryIterator.next());
    assertEquals(Maps.immutableEntry("bar", 1), entryIterator.next());

    Iterator<Entry<String, Collection<Integer>>> collectionIterator =
        multimap.asMap().entrySet().iterator();
    Entry<String, Collection<Integer>> entry = collectionIterator.next();
    assertEquals("foo", entry.getKey());
    assertThat(entry.getValue()).containsExactly(5, 3).inOrder();
    entry = collectionIterator.next();
    assertEquals("bar", entry.getKey());
    assertThat(entry.getValue()).containsExactly(4, 1).inOrder();
    entry = collectionIterator.next();
    assertEquals("cow", entry.getKey());
    assertThat(entry.getValue()).contains(2);
  }

  public void testOrderingUpdates() {
    Multimap<String, Integer> multimap = initializeMultimap5();

    assertThat(multimap.replaceValues("foo", asList(6, 7))).containsExactly(5, 3).inOrder();
    assertThat(multimap.keySet()).containsExactly("foo", "bar", "cow").inOrder();
    assertThat(multimap.removeAll("foo")).containsExactly(6, 7).inOrder();
    assertThat(multimap.keySet()).containsExactly("bar", "cow").inOrder();
    assertTrue(multimap.remove("bar", 4));
    assertThat(multimap.keySet()).containsExactly("bar", "cow").inOrder();
    assertTrue(multimap.remove("bar", 1));
    assertThat(multimap.keySet()).contains("cow");
    multimap.put("bar", 9);
    assertThat(multimap.keySet()).containsExactly("cow", "bar").inOrder();
  }

  public void testToStringNullExact() {
    Multimap<String, Integer> multimap = LinkedHashMultimap.create();

    multimap.put("foo", 3);
    multimap.put("foo", -1);
    multimap.put(null, null);
    multimap.put("bar", 1);
    multimap.put("foo", 2);
    multimap.put(null, 0);
    multimap.put("bar", 2);
    multimap.put("bar", null);
    multimap.put("foo", null);
    multimap.put("foo", 4);
    multimap.put(null, -1);
    multimap.put("bar", 3);
    multimap.put("bar", 1);
    multimap.put("foo", 1);

    assertEquals(
        "{foo=[3, -1, 2, null, 4, 1], null=[null, 0, -1], bar=[1, 2, null, 3]}",
        multimap.toString());
  }

  public void testPutMultimapOrdered() {
    Multimap<String, Integer> multimap = LinkedHashMultimap.create();
    multimap.putAll(initializeMultimap5());
    assertOrderingReadOnly(multimap);
  }

  public void testKeysToString_ordering() {
    Multimap<String, Integer> multimap = initializeMultimap5();
    assertEquals("[foo x 2, bar x 2, cow]", multimap.keys().toString());
  }

  public void testCreate() {
    LinkedHashMultimap<String, Integer> multimap = LinkedHashMultimap.create();
    multimap.put("foo", 1);
    multimap.put("bar", 2);
    multimap.put("foo", 3);
    assertEquals(ImmutableSet.of(1, 3), multimap.get("foo"));
  }

  public void testCreateFromMultimap() {
    Multimap<String, Integer> multimap = LinkedHashMultimap.create();
    multimap.put("a", 1);
    multimap.put("b", 2);
    multimap.put("a", 3);
    multimap.put("c", 4);
    LinkedHashMultimap<String, Integer> copy = LinkedHashMultimap.create(multimap);
    new EqualsTester().addEqualityGroup(multimap, copy).testEquals();
  }

  public void testCreateFromSizes() {
    LinkedHashMultimap<String, Integer> multimap = LinkedHashMultimap.create(20, 15);
    multimap.put("foo", 1);
    multimap.put("bar", 2);
    multimap.put("foo", 3);
    assertEquals(ImmutableSet.of(1, 3), multimap.get("foo"));
  }

  public void testCreateFromIllegalSizes() {
    try {
      LinkedHashMultimap.create(-20, 15);
      fail();
    } catch (IllegalArgumentException expected) {
    }

    try {
      LinkedHashMultimap.create(20, -15);
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }

  @GwtIncompatible // unreasonably slow
  public void testGetIteration() {
    new IteratorTester<Integer>(
        6,
        MODIFIABLE,
        newLinkedHashSet(asList(2, 3, 4, 7, 8)),
        IteratorTester.KnownOrder.KNOWN_ORDER) {
      private Multimap<String, Integer> multimap;

      @Override
      protected Iterator<Integer> newTargetIterator() {
        multimap = LinkedHashMultimap.create();
        multimap.putAll("foo", asList(2, 3, 4));
        multimap.putAll("bar", asList(5, 6));
        multimap.putAll("foo", asList(7, 8));
        return multimap.get("foo").iterator();
      }

      @Override
      protected void verify(List<Integer> elements) {
        assertEquals(newHashSet(elements), multimap.get("foo"));
      }
    }.test();
  }

  @GwtIncompatible // unreasonably slow
  public void testEntriesIteration() {
    @SuppressWarnings("unchecked")
    Set<Entry<String, Integer>> set =
        Sets.newLinkedHashSet(
            asList(
                Maps.immutableEntry("foo", 2),
                Maps.immutableEntry("foo", 3),
                Maps.immutableEntry("bar", 4),
                Maps.immutableEntry("bar", 5),
                Maps.immutableEntry("foo", 6)));

    new IteratorTester<Entry<String, Integer>>(
        6, MODIFIABLE, set, IteratorTester.KnownOrder.KNOWN_ORDER) {
      private Multimap<String, Integer> multimap;

      @Override
      protected Iterator<Entry<String, Integer>> newTargetIterator() {
        multimap = LinkedHashMultimap.create();
        multimap.putAll("foo", asList(2, 3));
        multimap.putAll("bar", asList(4, 5));
        multimap.putAll("foo", asList(6));
        return multimap.entries().iterator();
      }

      @Override
      protected void verify(List<Entry<String, Integer>> elements) {
        assertEquals(newHashSet(elements), multimap.entries());
      }
    }.test();
  }

  @GwtIncompatible // unreasonably slow
  public void testKeysIteration() {
    new IteratorTester<String>(
        6,
        MODIFIABLE,
        newArrayList("foo", "foo", "bar", "bar", "foo"),
        IteratorTester.KnownOrder.KNOWN_ORDER) {
      private Multimap<String, Integer> multimap;

      @Override
      protected Iterator<String> newTargetIterator() {
        multimap = LinkedHashMultimap.create();
        multimap.putAll("foo", asList(2, 3));
        multimap.putAll("bar", asList(4, 5));
        multimap.putAll("foo", asList(6));
        return multimap.keys().iterator();
      }

      @Override
      protected void verify(List<String> elements) {
        assertEquals(elements, Lists.newArrayList(multimap.keys()));
      }
    }.test();
  }

  @GwtIncompatible // unreasonably slow
  public void testValuesIteration() {
    new IteratorTester<Integer>(
        6, MODIFIABLE, newArrayList(2, 3, 4, 5, 6), IteratorTester.KnownOrder.KNOWN_ORDER) {
      private Multimap<String, Integer> multimap;

      @Override
      protected Iterator<Integer> newTargetIterator() {
        multimap = LinkedHashMultimap.create();
        multimap.putAll("foo", asList(2, 3));
        multimap.putAll("bar", asList(4, 5));
        multimap.putAll("foo", asList(6));
        return multimap.values().iterator();
      }

      @Override
      protected void verify(List<Integer> elements) {
        assertEquals(elements, Lists.newArrayList(multimap.values()));
      }
    }.test();
  }

  @GwtIncompatible // unreasonably slow
  public void testKeySetIteration() {
    new IteratorTester<String>(
        6,
        MODIFIABLE,
        newLinkedHashSet(asList("foo", "bar", "baz", "dog", "cat")),
        IteratorTester.KnownOrder.KNOWN_ORDER) {
      private Multimap<String, Integer> multimap;

      @Override
      protected Iterator<String> newTargetIterator() {
        multimap = LinkedHashMultimap.create();
        multimap.putAll("foo", asList(2, 3));
        multimap.putAll("bar", asList(4, 5));
        multimap.putAll("foo", asList(6));
        multimap.putAll("baz", asList(7, 8));
        multimap.putAll("dog", asList(9));
        multimap.putAll("bar", asList(10, 11));
        multimap.putAll("cat", asList(12, 13, 14));
        return multimap.keySet().iterator();
      }

      @Override
      protected void verify(List<String> elements) {
        assertEquals(newHashSet(elements), multimap.keySet());
      }
    }.test();
  }

  @GwtIncompatible // unreasonably slow
  public void testAsSetIteration() {
    @SuppressWarnings("unchecked")
    Set<Entry<String, Collection<Integer>>> set =
        newLinkedHashSet(
            asList(
                Maps.immutableEntry("foo", (Collection<Integer>) Sets.newHashSet(2, 3, 6)),
                Maps.immutableEntry("bar", (Collection<Integer>) Sets.newHashSet(4, 5, 10, 11)),
                Maps.immutableEntry("baz", (Collection<Integer>) Sets.newHashSet(7, 8)),
                Maps.immutableEntry("dog", (Collection<Integer>) Sets.newHashSet(9)),
                Maps.immutableEntry("cat", (Collection<Integer>) Sets.newHashSet(12, 13, 14))));
    new IteratorTester<Entry<String, Collection<Integer>>>(
        6, MODIFIABLE, set, IteratorTester.KnownOrder.KNOWN_ORDER) {
      private Multimap<String, Integer> multimap;

      @Override
      protected Iterator<Entry<String, Collection<Integer>>> newTargetIterator() {
        multimap = LinkedHashMultimap.create();
        multimap.putAll("foo", asList(2, 3));
        multimap.putAll("bar", asList(4, 5));
        multimap.putAll("foo", asList(6));
        multimap.putAll("baz", asList(7, 8));
        multimap.putAll("dog", asList(9));
        multimap.putAll("bar", asList(10, 11));
        multimap.putAll("cat", asList(12, 13, 14));
        return multimap.asMap().entrySet().iterator();
      }

      @Override
      protected void verify(List<Entry<String, Collection<Integer>>> elements) {
        assertEquals(newHashSet(elements), multimap.asMap().entrySet());
      }
    }.test();
  }

  public void testKeysSpliterator() {
    List<Entry<String, Integer>> expectedEntries =
        asList(
            Maps.immutableEntry("foo", 2),
            Maps.immutableEntry("foo", 3),
            Maps.immutableEntry("bar", 4),
            Maps.immutableEntry("bar", 5),
            Maps.immutableEntry("foo", 6));
    Multimap<String, Integer> multimap = LinkedHashMultimap.create();
    for (Entry<String, Integer> entry : expectedEntries) {
      multimap.put(entry.getKey(), entry.getValue());
    }
    List<String> actualKeys = new ArrayList<>();
    multimap.keys().spliterator().forEachRemaining(actualKeys::add);
    assertThat(actualKeys)
        .containsExactlyElementsIn(Lists.transform(expectedEntries, Entry::getKey))
        .inOrder();
  }

  public void testEntriesSpliterator() {
    List<Entry<String, Integer>> expectedEntries =
        asList(
            Maps.immutableEntry("foo", 2),
            Maps.immutableEntry("foo", 3),
            Maps.immutableEntry("bar", 4),
            Maps.immutableEntry("bar", 5),
            Maps.immutableEntry("foo", 6));
    Multimap<String, Integer> multimap = LinkedHashMultimap.create();
    for (Entry<String, Integer> entry : expectedEntries) {
      multimap.put(entry.getKey(), entry.getValue());
    }
    List<Entry<String, Integer>> actualEntries = new ArrayList<>();
    multimap.entries().spliterator().forEachRemaining(actualEntries::add);
    assertThat(actualEntries).containsExactlyElementsIn(expectedEntries).inOrder();
  }

  public void testValuesSpliterator() {
    List<Entry<String, Integer>> expectedEntries =
        asList(
            Maps.immutableEntry("foo", 2),
            Maps.immutableEntry("foo", 3),
            Maps.immutableEntry("bar", 4),
            Maps.immutableEntry("bar", 5),
            Maps.immutableEntry("foo", 6));
    Multimap<String, Integer> multimap = LinkedHashMultimap.create();
    for (Entry<String, Integer> entry : expectedEntries) {
      multimap.put(entry.getKey(), entry.getValue());
    }
    List<Integer> actualValues = new ArrayList<>();
    multimap.values().spliterator().forEachRemaining(actualValues::add);
    assertThat(actualValues)
        .containsExactlyElementsIn(Lists.transform(expectedEntries, Entry::getValue))
        .inOrder();
  }
}
