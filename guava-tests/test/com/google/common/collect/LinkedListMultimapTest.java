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
import static com.google.common.collect.testing.IteratorFeature.SUPPORTS_REMOVE;
import static com.google.common.collect.testing.IteratorFeature.SUPPORTS_SET;
import static com.google.common.truth.Truth.assertThat;
import static java.util.Arrays.asList;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.collect.testing.IteratorTester;
import com.google.common.collect.testing.ListIteratorTester;
import com.google.common.collect.testing.features.CollectionFeature;
import com.google.common.collect.testing.features.CollectionSize;
import com.google.common.collect.testing.features.MapFeature;
import com.google.common.collect.testing.google.ListMultimapTestSuiteBuilder;
import com.google.common.collect.testing.google.TestStringListMultimapGenerator;
import com.google.common.testing.EqualsTester;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map.Entry;
import java.util.RandomAccess;
import java.util.Set;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Tests for {@code LinkedListMultimap}.
 *
 * @author Mike Bostock
 */
@GwtCompatible(emulated = true)
public class LinkedListMultimapTest extends TestCase {

  @GwtIncompatible // suite
  public static Test suite() {
    TestSuite suite = new TestSuite();
    suite.addTest(
        ListMultimapTestSuiteBuilder.using(
                new TestStringListMultimapGenerator() {
                  @Override
                  protected ListMultimap<String, String> create(Entry<String, String>[] entries) {
                    ListMultimap<String, String> multimap = LinkedListMultimap.create();
                    for (Entry<String, String> entry : entries) {
                      multimap.put(entry.getKey(), entry.getValue());
                    }
                    return multimap;
                  }
                })
            .named("LinkedListMultimap")
            .withFeatures(
                MapFeature.ALLOWS_NULL_KEYS,
                MapFeature.ALLOWS_NULL_VALUES,
                MapFeature.ALLOWS_ANY_NULL_QUERIES,
                MapFeature.GENERAL_PURPOSE,
                CollectionFeature.SUPPORTS_ITERATOR_REMOVE,
                CollectionFeature.SERIALIZABLE,
                CollectionFeature.KNOWN_ORDER,
                CollectionSize.ANY)
            .createTestSuite());
    suite.addTestSuite(LinkedListMultimapTest.class);
    return suite;
  }

  protected LinkedListMultimap<String, Integer> create() {
    return LinkedListMultimap.create();
  }

  /** Confirm that get() returns a List that doesn't implement RandomAccess. */
  public void testGetRandomAccess() {
    Multimap<String, Integer> multimap = create();
    multimap.put("foo", 1);
    multimap.put("foo", 3);
    assertFalse(multimap.get("foo") instanceof RandomAccess);
    assertFalse(multimap.get("bar") instanceof RandomAccess);
  }

  /**
   * Confirm that removeAll() returns a List that implements RandomAccess, even though get()
   * doesn't.
   */
  public void testRemoveAllRandomAccess() {
    Multimap<String, Integer> multimap = create();
    multimap.put("foo", 1);
    multimap.put("foo", 3);
    assertTrue(multimap.removeAll("foo") instanceof RandomAccess);
    assertTrue(multimap.removeAll("bar") instanceof RandomAccess);
  }

  /**
   * Confirm that replaceValues() returns a List that implements RandomAccess, even though get()
   * doesn't.
   */
  public void testReplaceValuesRandomAccess() {
    Multimap<String, Integer> multimap = create();
    multimap.put("foo", 1);
    multimap.put("foo", 3);
    assertTrue(multimap.replaceValues("foo", Arrays.asList(2, 4)) instanceof RandomAccess);
    assertTrue(multimap.replaceValues("bar", Arrays.asList(2, 4)) instanceof RandomAccess);
  }

  public void testCreateFromMultimap() {
    Multimap<String, Integer> multimap = LinkedListMultimap.create();
    multimap.put("foo", 1);
    multimap.put("bar", 3);
    multimap.put("foo", 2);
    LinkedListMultimap<String, Integer> copy = LinkedListMultimap.create(multimap);
    assertEquals(multimap, copy);
    assertThat(copy.entries()).containsExactlyElementsIn(multimap.entries()).inOrder();
  }

  public void testCreateFromSize() {
    LinkedListMultimap<String, Integer> multimap = LinkedListMultimap.create(20);
    multimap.put("foo", 1);
    multimap.put("bar", 2);
    multimap.put("foo", 3);
    assertEquals(ImmutableList.of(1, 3), multimap.get("foo"));
  }

  public void testCreateFromIllegalSize() {
    try {
      LinkedListMultimap.create(-20);
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }

  public void testLinkedGetAdd() {
    LinkedListMultimap<String, Integer> map = create();
    map.put("bar", 1);
    Collection<Integer> foos = map.get("foo");
    foos.add(2);
    foos.add(3);
    map.put("bar", 4);
    map.put("foo", 5);
    assertEquals("{bar=[1, 4], foo=[2, 3, 5]}", map.toString());
    assertEquals("[bar=1, foo=2, foo=3, bar=4, foo=5]", map.entries().toString());
  }

  public void testLinkedGetInsert() {
    ListMultimap<String, Integer> map = create();
    map.put("bar", 1);
    List<Integer> foos = map.get("foo");
    foos.add(2);
    foos.add(0, 3);
    map.put("bar", 4);
    map.put("foo", 5);
    assertEquals("{bar=[1, 4], foo=[3, 2, 5]}", map.toString());
    assertEquals("[bar=1, foo=3, foo=2, bar=4, foo=5]", map.entries().toString());
  }

  public void testLinkedPutInOrder() {
    Multimap<String, Integer> map = create();
    map.put("foo", 1);
    map.put("bar", 2);
    map.put("bar", 3);
    assertEquals("{foo=[1], bar=[2, 3]}", map.toString());
    assertEquals("[foo=1, bar=2, bar=3]", map.entries().toString());
  }

  public void testLinkedPutOutOfOrder() {
    Multimap<String, Integer> map = create();
    map.put("bar", 1);
    map.put("foo", 2);
    map.put("bar", 3);
    assertEquals("{bar=[1, 3], foo=[2]}", map.toString());
    assertEquals("[bar=1, foo=2, bar=3]", map.entries().toString());
  }

  public void testLinkedPutAllMultimap() {
    Multimap<String, Integer> src = create();
    src.put("bar", 1);
    src.put("foo", 2);
    src.put("bar", 3);
    Multimap<String, Integer> dst = create();
    dst.putAll(src);
    assertEquals("{bar=[1, 3], foo=[2]}", dst.toString());
    assertEquals("[bar=1, foo=2, bar=3]", src.entries().toString());
  }

  public void testLinkedReplaceValues() {
    Multimap<String, Integer> map = create();
    map.put("bar", 1);
    map.put("foo", 2);
    map.put("bar", 3);
    map.put("bar", 4);
    assertEquals("{bar=[1, 3, 4], foo=[2]}", map.toString());
    map.replaceValues("bar", asList(1, 2));
    assertEquals("[bar=1, foo=2, bar=2]", map.entries().toString());
    assertEquals("{bar=[1, 2], foo=[2]}", map.toString());
  }

  public void testLinkedClear() {
    ListMultimap<String, Integer> map = create();
    map.put("foo", 1);
    map.put("foo", 2);
    map.put("bar", 3);
    List<Integer> foos = map.get("foo");
    Collection<Integer> values = map.values();
    assertEquals(asList(1, 2), foos);
    assertThat(values).containsExactly(1, 2, 3).inOrder();
    map.clear();
    assertEquals(Collections.emptyList(), foos);
    assertThat(values).isEmpty();
    assertEquals("[]", map.entries().toString());
    assertEquals("{}", map.toString());
  }

  public void testLinkedKeySet() {
    Multimap<String, Integer> map = create();
    map.put("bar", 1);
    map.put("foo", 2);
    map.put("bar", 3);
    map.put("bar", 4);
    assertEquals("[bar, foo]", map.keySet().toString());
    map.keySet().remove("bar");
    assertEquals("{foo=[2]}", map.toString());
  }

  public void testLinkedKeys() {
    Multimap<String, Integer> map = create();
    map.put("bar", 1);
    map.put("foo", 2);
    map.put("bar", 3);
    map.put("bar", 4);
    assertEquals("[bar=1, foo=2, bar=3, bar=4]", map.entries().toString());
    assertThat(map.keys()).containsExactly("bar", "foo", "bar", "bar").inOrder();
    map.keys().remove("bar"); // bar is no longer the first key!
    assertEquals("{foo=[2], bar=[3, 4]}", map.toString());
  }

  public void testLinkedValues() {
    Multimap<String, Integer> map = create();
    map.put("bar", 1);
    map.put("foo", 2);
    map.put("bar", 3);
    map.put("bar", 4);
    assertEquals("[1, 2, 3, 4]", map.values().toString());
    map.values().remove(2);
    assertEquals("{bar=[1, 3, 4]}", map.toString());
  }

  public void testLinkedEntries() {
    Multimap<String, Integer> map = create();
    map.put("bar", 1);
    map.put("foo", 2);
    map.put("bar", 3);
    Iterator<Entry<String, Integer>> entries = map.entries().iterator();
    Entry<String, Integer> entry = entries.next();
    assertEquals("bar", entry.getKey());
    assertEquals(1, (int) entry.getValue());
    entry = entries.next();
    assertEquals("foo", entry.getKey());
    assertEquals(2, (int) entry.getValue());
    entry.setValue(4);
    entry = entries.next();
    assertEquals("bar", entry.getKey());
    assertEquals(3, (int) entry.getValue());
    assertFalse(entries.hasNext());
    entries.remove();
    assertEquals("{bar=[1], foo=[4]}", map.toString());
  }

  public void testLinkedAsMapEntries() {
    Multimap<String, Integer> map = create();
    map.put("bar", 1);
    map.put("foo", 2);
    map.put("bar", 3);
    Iterator<Entry<String, Collection<Integer>>> entries = map.asMap().entrySet().iterator();
    Entry<String, Collection<Integer>> entry = entries.next();
    assertEquals("bar", entry.getKey());
    assertThat(entry.getValue()).containsExactly(1, 3).inOrder();
    try {
      entry.setValue(Arrays.<Integer>asList());
      fail("UnsupportedOperationException expected");
    } catch (UnsupportedOperationException expected) {
    }
    entries.remove(); // clear
    entry = entries.next();
    assertEquals("foo", entry.getKey());
    assertThat(entry.getValue()).contains(2);
    assertFalse(entries.hasNext());
    assertEquals("{foo=[2]}", map.toString());
  }

  public void testEntriesAfterMultimapUpdate() {
    ListMultimap<String, Integer> multimap = create();
    multimap.put("foo", 2);
    multimap.put("bar", 3);
    Collection<Entry<String, Integer>> entries = multimap.entries();
    Iterator<Entry<String, Integer>> iterator = entries.iterator();
    Entry<String, Integer> entrya = iterator.next();
    Entry<String, Integer> entryb = iterator.next();

    assertEquals(2, (int) multimap.get("foo").set(0, 4));
    assertFalse(multimap.containsEntry("foo", 2));
    assertTrue(multimap.containsEntry("foo", 4));
    assertTrue(multimap.containsEntry("bar", 3));
    assertEquals(4, (int) entrya.getValue());
    assertEquals(3, (int) entryb.getValue());

    assertTrue(multimap.put("foo", 5));
    assertTrue(multimap.containsEntry("foo", 5));
    assertTrue(multimap.containsEntry("foo", 4));
    assertTrue(multimap.containsEntry("bar", 3));
    assertEquals(4, (int) entrya.getValue());
    assertEquals(3, (int) entryb.getValue());
  }

  @SuppressWarnings("unchecked")
  @GwtIncompatible // unreasonably slow
  public void testEntriesIteration() {
    List<Entry<String, Integer>> addItems =
        ImmutableList.of(
            Maps.immutableEntry("foo", 99),
            Maps.immutableEntry("foo", 88),
            Maps.immutableEntry("bar", 77));

    for (final int startIndex : new int[] {0, 3, 5}) {
      List<Entry<String, Integer>> list =
          Lists.newArrayList(
              Maps.immutableEntry("foo", 2),
              Maps.immutableEntry("foo", 3),
              Maps.immutableEntry("bar", 4),
              Maps.immutableEntry("bar", 5),
              Maps.immutableEntry("foo", 6));
      new ListIteratorTester<Entry<String, Integer>>(
          3, addItems, ImmutableList.of(SUPPORTS_REMOVE), list, startIndex) {
        private LinkedListMultimap<String, Integer> multimap;

        @Override
        protected ListIterator<Entry<String, Integer>> newTargetIterator() {
          multimap = create();
          multimap.putAll("foo", asList(2, 3));
          multimap.putAll("bar", asList(4, 5));
          multimap.put("foo", 6);
          return multimap.entries().listIterator(startIndex);
        }

        @Override
        protected void verify(List<Entry<String, Integer>> elements) {
          assertEquals(elements, multimap.entries());
        }
      }.test();
    }
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
        multimap = create();
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
    List<Integer> addItems = ImmutableList.of(99, 88, 77);

    for (final int startIndex : new int[] {0, 3, 5}) {
      new ListIteratorTester<Integer>(
          3,
          addItems,
          ImmutableList.of(SUPPORTS_REMOVE, SUPPORTS_SET),
          Lists.newArrayList(2, 3, 4, 5, 6),
          startIndex) {
        private LinkedListMultimap<String, Integer> multimap;

        @Override
        protected ListIterator<Integer> newTargetIterator() {
          multimap = create();
          multimap.put("bar", 2);
          multimap.putAll("foo", Arrays.asList(3, 4));
          multimap.put("bar", 5);
          multimap.put("foo", 6);
          return multimap.values().listIterator(startIndex);
        }

        @Override
        protected void verify(List<Integer> elements) {
          assertEquals(elements, multimap.values());
        }
      }.test();
    }
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

      @Override
      protected void verify(List<String> elements) {
        assertEquals(newHashSet(elements), multimap.keySet());
      }
    }.test();
  }

  @SuppressWarnings("unchecked")
  @GwtIncompatible // unreasonably slow
  public void testAsSetIteration() {
    Set<Entry<String, Collection<Integer>>> set =
        Sets.newLinkedHashSet(
            asList(
                Maps.immutableEntry("foo", (Collection<Integer>) asList(2, 3, 6)),
                Maps.immutableEntry("bar", (Collection<Integer>) asList(4, 5, 10, 11)),
                Maps.immutableEntry("baz", (Collection<Integer>) asList(7, 8)),
                Maps.immutableEntry("dog", (Collection<Integer>) asList(9)),
                Maps.immutableEntry("cat", (Collection<Integer>) asList(12, 13, 14))));

    new IteratorTester<Entry<String, Collection<Integer>>>(
        6, MODIFIABLE, set, IteratorTester.KnownOrder.KNOWN_ORDER) {
      private Multimap<String, Integer> multimap;

      @Override
      protected Iterator<Entry<String, Collection<Integer>>> newTargetIterator() {
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

      @Override
      protected void verify(List<Entry<String, Collection<Integer>>> elements) {
        assertEquals(newHashSet(elements), multimap.asMap().entrySet());
      }
    }.test();
  }

  public void testEquals() {
    new EqualsTester()
        .addEqualityGroup(
            LinkedListMultimap.create(), LinkedListMultimap.create(), LinkedListMultimap.create(1))
        .testEquals();
  }
}
