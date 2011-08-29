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
import static com.google.common.collect.testing.IteratorFeature.MODIFIABLE;
import static java.util.Arrays.asList;
import static org.junit.contrib.truth.Truth.ASSERT;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.collect.testing.IteratorTester;
import com.google.common.testing.SerializableTester;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * Unit tests for {@code LinkedHashMultimap}.
 *
 * @author Jared Levy
 */
@GwtCompatible(emulated = true)
public class LinkedHashMultimapTest extends AbstractSetMultimapTest {

  @Override protected Multimap<String, Integer> create() {
    return LinkedHashMultimap.create();
  }

  private Multimap<String, Integer> initializeMultimap5() {
    Multimap<String, Integer> multimap = getMultimap();
    multimap.put("foo", 5);
    multimap.put("bar", 4);
    multimap.put("foo", 3);
    multimap.put("cow", 2);
    multimap.put("bar", 1);
    return multimap;
  }

  public void testToString() {
    assertEquals("{foo=[3, -1, 2, 4, 1], bar=[1, 2, 3]}",
        createSample().toString());
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

  @GwtIncompatible("SeriazableTester")
  public void testSerializationOrdering() {
    Multimap<String, Integer> multimap = initializeMultimap5();
    Multimap<String, Integer> copy
        = SerializableTester.reserializeAndAssert(multimap);
    assertOrderingReadOnly(copy);
  }

  private void assertOrderingReadOnly(Multimap<String, Integer> multimap) {
    ASSERT.that(multimap.get("foo")).hasContentsInOrder(5, 3);
    ASSERT.that(multimap.get("bar")).hasContentsInOrder(4, 1);
    ASSERT.that(multimap.get("cow")).hasContentsInOrder(2);

    ASSERT.that(multimap.keySet()).hasContentsInOrder("foo", "bar", "cow");
    ASSERT.that(multimap.values()).hasContentsInOrder(5, 4, 3, 2, 1);

    Iterator<Map.Entry<String, Integer>> entryIterator =
        multimap.entries().iterator();
    assertEquals(Maps.immutableEntry("foo", 5), entryIterator.next());
    assertEquals(Maps.immutableEntry("bar", 4), entryIterator.next());
    assertEquals(Maps.immutableEntry("foo", 3), entryIterator.next());
    assertEquals(Maps.immutableEntry("cow", 2), entryIterator.next());
    assertEquals(Maps.immutableEntry("bar", 1), entryIterator.next());

    Iterator<Map.Entry<String, Collection<Integer>>> collectionIterator =
        multimap.asMap().entrySet().iterator();
    Map.Entry<String, Collection<Integer>> entry = collectionIterator.next();
    assertEquals("foo", entry.getKey());
    ASSERT.that(entry.getValue()).hasContentsInOrder(5, 3);
    entry = collectionIterator.next();
    assertEquals("bar", entry.getKey());
    ASSERT.that(entry.getValue()).hasContentsInOrder(4, 1);
    entry = collectionIterator.next();
    assertEquals("cow", entry.getKey());
    ASSERT.that(entry.getValue()).hasContentsInOrder(2);
  }

  public void testOrderingUpdates() {
    Multimap<String, Integer> multimap = initializeMultimap5();

    ASSERT.that(multimap.replaceValues("foo", asList(6, 7))).hasContentsInOrder(5, 3);
    ASSERT.that(multimap.keySet()).hasContentsInOrder("foo", "bar", "cow");
    ASSERT.that(multimap.removeAll("foo")).hasContentsInOrder(6, 7);
    ASSERT.that(multimap.keySet()).hasContentsInOrder("bar", "cow");
    assertTrue(multimap.remove("bar", 4));
    ASSERT.that(multimap.keySet()).hasContentsInOrder("bar", "cow");
    assertTrue(multimap.remove("bar", 1));
    ASSERT.that(multimap.keySet()).hasContentsInOrder("cow");
    multimap.put("bar", 9);
    ASSERT.that(multimap.keySet()).hasContentsInOrder("cow", "bar");
  }

  public void testToStringNullExact() {
    Multimap<String, Integer> multimap = getMultimap();

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
    assertEquals(8, multimap.expectedValuesPerKey);
  }

  public void testCreateFromMultimap() {
    Multimap<String, Integer> multimap = createSample();
    LinkedHashMultimap<String, Integer> copy =
        LinkedHashMultimap.create(multimap);
    assertEquals(multimap, copy);
    assertEquals(8, copy.expectedValuesPerKey);
  }

  public void testCreateFromSizes() {
    LinkedHashMultimap<String, Integer> multimap
        = LinkedHashMultimap.create(20, 15);
    multimap.put("foo", 1);
    multimap.put("bar", 2);
    multimap.put("foo", 3);
    assertEquals(ImmutableSet.of(1, 3), multimap.get("foo"));
    assertEquals(15, multimap.expectedValuesPerKey);
  }

  public void testCreateFromIllegalSizes() {
    try {
      LinkedHashMultimap.create(-20, 15);
      fail();
    } catch (IllegalArgumentException expected) {}

    try {
      LinkedHashMultimap.create(20, -15);
      fail();
    } catch (IllegalArgumentException expected) {}
  }

  @GwtIncompatible("unreasonable slow")
  public void testGetIteration() {
    new IteratorTester<Integer>(6, MODIFIABLE,
        newLinkedHashSet(asList(2, 3, 4, 7, 8)),
        IteratorTester.KnownOrder.KNOWN_ORDER) {
      private Multimap<String, Integer> multimap;

      @Override protected Iterator<Integer> newTargetIterator() {
        multimap = create();
        multimap.putAll("foo", asList(2, 3, 4));
        multimap.putAll("bar", asList(5, 6));
        multimap.putAll("foo", asList(7, 8));
        return multimap.get("foo").iterator();
      }

      @Override protected void verify(List<Integer> elements) {
        assertEquals(newHashSet(elements), multimap.get("foo"));
      }
    }.test();
  }

  @GwtIncompatible("unreasonable slow")
  public void testEntriesIteration() {
    @SuppressWarnings("unchecked")
    Set<Entry<String, Integer>> set = Sets.newLinkedHashSet(asList(
        Maps.immutableEntry("foo", 2),
        Maps.immutableEntry("foo", 3),
        Maps.immutableEntry("bar", 4),
        Maps.immutableEntry("bar", 5),
        Maps.immutableEntry("foo", 6)));

    new IteratorTester<Entry<String, Integer>>(6, MODIFIABLE, set,
        IteratorTester.KnownOrder.KNOWN_ORDER) {
      private Multimap<String, Integer> multimap;

      @Override protected Iterator<Entry<String, Integer>> newTargetIterator() {
        multimap = create();
        multimap.putAll("foo", asList(2, 3));
        multimap.putAll("bar", asList(4, 5));
        multimap.putAll("foo", asList(6));
        return multimap.entries().iterator();
      }

      @Override protected void verify(List<Entry<String, Integer>> elements) {
        assertEquals(newHashSet(elements), multimap.entries());
      }
    }.test();
  }

  @GwtIncompatible("unreasonable slow")
  public void testKeysIteration() {
    new IteratorTester<String>(6, MODIFIABLE, newArrayList("foo", "foo", "bar",
        "bar", "foo"), IteratorTester.KnownOrder.KNOWN_ORDER) {
      private Multimap<String, Integer> multimap;

      @Override protected Iterator<String> newTargetIterator() {
        multimap = create();
        multimap.putAll("foo", asList(2, 3));
        multimap.putAll("bar", asList(4, 5));
        multimap.putAll("foo", asList(6));
        return multimap.keys().iterator();
      }

      @Override protected void verify(List<String> elements) {
        assertEquals(elements, Lists.newArrayList(multimap.keys()));
      }
    }.test();
  }

  @GwtIncompatible("unreasonable slow")
  public void testValuesIteration() {
    new IteratorTester<Integer>(6, MODIFIABLE, newArrayList(2, 3, 4, 5, 6),
        IteratorTester.KnownOrder.KNOWN_ORDER) {
      private Multimap<String, Integer> multimap;

      @Override protected Iterator<Integer> newTargetIterator() {
        multimap = create();
        multimap.putAll("foo", asList(2, 3));
        multimap.putAll("bar", asList(4, 5));
        multimap.putAll("foo", asList(6));
        return multimap.values().iterator();
      }

      @Override protected void verify(List<Integer> elements) {
        assertEquals(elements, Lists.newArrayList(multimap.values()));
      }
    }.test();
  }

  @GwtIncompatible("unreasonable slow")
  public void testKeySetIteration() {
    new IteratorTester<String>(6, MODIFIABLE, newLinkedHashSet(asList(
        "foo", "bar", "baz", "dog", "cat")),
        IteratorTester.KnownOrder.KNOWN_ORDER) {
      private Multimap<String, Integer> multimap;

      @Override protected Iterator<String> newTargetIterator() {
        multimap = create();
        multimap.putAll("foo", asList(2, 3));
        multimap.putAll("bar", asList(4, 5));
        multimap.putAll("foo", asList(6));
        multimap.putAll("baz", asList(7, 8));
        multimap.putAll("dog", asList(9));
        multimap.putAll("bar", asList(10, 11));
        multimap.putAll("cat", asList(12, 13, 14));
        return multimap.keySet().iterator();
      }

      @Override protected void verify(List<String> elements) {
        assertEquals(newHashSet(elements), multimap.keySet());
      }
    }.test();
  }

  @GwtIncompatible("unreasonable slow")
  public void testAsSetIteration() {
    @SuppressWarnings("unchecked")
    Set<Entry<String, Collection<Integer>>> set = newLinkedHashSet(asList(
        Maps.immutableEntry("foo",
            (Collection<Integer>) Sets.newHashSet(2, 3, 6)),
        Maps.immutableEntry("bar",
            (Collection<Integer>) Sets.newHashSet(4, 5, 10, 11)),
        Maps.immutableEntry("baz",
            (Collection<Integer>) Sets.newHashSet(7, 8)),
        Maps.immutableEntry("dog",
            (Collection<Integer>) Sets.newHashSet(9)),
        Maps.immutableEntry("cat",
            (Collection<Integer>) Sets.newHashSet(12, 13, 14))
    ));
    new IteratorTester<Entry<String, Collection<Integer>>>(6, MODIFIABLE, set,
        IteratorTester.KnownOrder.KNOWN_ORDER) {
      private Multimap<String, Integer> multimap;

      @Override protected Iterator<Entry<String, Collection<Integer>>>
          newTargetIterator() {
        multimap = create();
        multimap.putAll("foo", asList(2, 3));
        multimap.putAll("bar", asList(4, 5));
        multimap.putAll("foo", asList(6));
        multimap.putAll("baz", asList(7, 8));
        multimap.putAll("dog", asList(9));
        multimap.putAll("bar", asList(10, 11));
        multimap.putAll("cat", asList(12, 13, 14));
        return multimap.asMap().entrySet().iterator();
      }

      @Override protected void verify(
          List<Entry<String, Collection<Integer>>> elements) {
        assertEquals(newHashSet(elements), multimap.asMap().entrySet());
      }
    }.test();
  }

}
