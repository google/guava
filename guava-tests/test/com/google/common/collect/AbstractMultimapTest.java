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

import static java.util.Arrays.asList;
import static org.junit.contrib.truth.Truth.ASSERT;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.testing.SerializableTester;

import junit.framework.TestCase;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Set;

/**
 * Tests for {@code Multimap} implementations. Caution: when subclassing avoid
 * accidental naming collisions with tests in this class!
 *
 * @author Jared Levy
 */
@GwtCompatible(emulated = true)
public abstract class AbstractMultimapTest extends TestCase {

  private Multimap<String, Integer> multimap;

  protected abstract Multimap<String, Integer> create();

  protected Multimap<String, Integer> createSample() {
    Multimap<String, Integer> sample = create();
    sample.putAll("foo", asList(3, -1, 2, 4, 1));
    sample.putAll("bar", asList(1, 2, 3, 1));
    return sample;
  }

  // public for GWT
  @Override public void setUp() throws Exception {
    super.setUp();
    multimap = create();
  }

  protected Multimap<String, Integer> getMultimap() {
    return multimap;
  }

  /**
   * Returns the key to use as a null placeholder in tests. The default
   * implementation returns {@code null}, but tests for multimaps that don't
   * support null keys should override it.
   */
  protected String nullKey() {
    return null;
  }

  /**
   * Returns the value to use as a null placeholder in tests. The default
   * implementation returns {@code null}, but tests for multimaps that don't
   * support null values should override it.
   */
  protected Integer nullValue() {
    return null;
  }

  /**
   * Validate multimap size by calling {@code size()} and also by iterating
   * through the entries. This tests cases where the {@code entries()} list is
   * stored separately, such as the {@link LinkedHashMultimap}. It also
   * verifies that the multimap contains every multimap entry.
   */
  protected void assertSize(int expectedSize) {
    assertEquals(expectedSize, multimap.size());

    int size = 0;
    for (Entry<String, Integer> entry : multimap.entries()) {
      assertTrue(multimap.containsEntry(entry.getKey(), entry.getValue()));
      size++;
    }
    assertEquals(expectedSize, size);

    int size2 = 0;
    for (Entry<String, Collection<Integer>> entry2 :
        multimap.asMap().entrySet()) {
      size2 += entry2.getValue().size();
    }
    assertEquals(expectedSize, size2);
  }

  protected boolean removedCollectionsAreModifiable() {
    return false;
  }

  public void testSize0() {
    assertSize(0);
  }

  public void testSize1() {
    multimap.put("foo", 1);
    assertSize(1);
  }

  public void testSize2Keys() {
    multimap.put("foo", 1);
    multimap.put("bar", 5);
    assertSize(2);
  }

  public void testSize2Values() {
    multimap.put("foo", 1);
    multimap.put("foo", 7);
    assertSize(2);
  }

  public void testSizeNull() {
    multimap.put("foo", 1);
    multimap.put("bar", 5);
    multimap.put(nullKey(), nullValue());
    multimap.put("foo", nullValue());
    multimap.put(nullKey(), 5);
    assertSize(5);
  }

  public void testIsEmptyYes() {
    assertTrue(multimap.isEmpty());
  }

  public void testIsEmptyNo() {
    multimap.put("foo", 1);
    assertFalse(multimap.isEmpty());
  }

  public void testIsEmptyNull() {
    multimap.put(nullKey(), nullValue());
    assertFalse(multimap.isEmpty());
  }

  public void testIsEmptyRemoved() {
    multimap.put("foo", 1);
    multimap.remove("foo", 1);
    assertTrue(multimap.isEmpty());
  }

  public void testContainsKeyTrue() {
    multimap.put("foo", 1);
    assertTrue(multimap.containsKey("foo"));
  }

  public void testContainsKeyFalse() {
    multimap.put("foo", 1);
    assertFalse(multimap.containsKey("bar"));
    assertFalse(multimap.containsKey(nullKey()));
  }

  public void testContainsKeyNull() {
    multimap.put(nullKey(), 1);
    assertTrue(multimap.containsKey(nullKey()));
  }

  public void testContainsValueTrue() {
    multimap.put("foo", 1);
    assertTrue(multimap.containsValue(1));
  }

  public void testContainsValueFalse() {
    multimap.put("foo", 1);
    assertFalse(multimap.containsValue(2));
    assertFalse(multimap.containsValue(nullValue()));
  }

  public void testContainsValueNull() {
    multimap.put("foo", nullValue());
    assertTrue(multimap.containsValue(nullValue()));
  }

  public void testContainsKeyValueTrue() {
    multimap.put("foo", 1);
    assertTrue(multimap.containsEntry("foo", 1));
  }

  public void testContainsKeyValueRemoved() {
    multimap.put("foo", 1);
    multimap.remove("foo", 1);
    assertFalse(multimap.containsEntry("foo", 1));
  }

  public void testGet0() {
    multimap.put("foo", 1);
    Collection<Integer> values = multimap.get("bar");
    assertEquals(0, values.size());
  }

  public void testGet1() {
    multimap.put("foo", 1);
    multimap.put("bar", 3);
    Collection<Integer> values = multimap.get("bar");
    assertEquals(1, values.size());
    assertTrue(values.contains(3));
    assertFalse(values.contains(5));
  }

  public void testGet2() {
    multimap.put("foo", 1);
    multimap.put("foo", 3);
    Collection<Integer> values = multimap.get("foo");
    assertEquals(2, values.size());
    assertTrue(values.contains(1));
    assertTrue(values.contains(3));
  }

  public void testGetNull() {
    multimap.put(nullKey(), nullValue());
    multimap.put(nullKey(), 3);
    Collection<Integer> values = multimap.get(nullKey());
    assertEquals(2, values.size());
    assertTrue(values.contains(nullValue()));
    assertTrue(values.contains(3));
  }

  public void testPutAllIterable() {
    Iterable<Integer> iterable = new Iterable<Integer>() {
      @Override
      public Iterator<Integer> iterator() {
        return Lists.newArrayList(1, 3).iterator();
      }
    };
    multimap.putAll("foo", iterable);
    assertTrue(multimap.containsEntry("foo", 1));
    assertTrue(multimap.containsEntry("foo", 3));
    assertSize(2);

    Iterable<Integer> emptyIterable = new Iterable<Integer>() {
      @Override
      public Iterator<Integer> iterator() {
        return Iterators.emptyIterator();
      }
    };
    multimap.putAll("bar", emptyIterable);
    assertSize(2);
    assertEquals(Collections.singleton("foo"), multimap.keySet());
  }

  public void testPutAllCollection() {
    Collection<Integer> collection = Lists.newArrayList(1, 3);
    multimap.putAll("foo", collection);
    assertTrue(multimap.containsEntry("foo", 1));
    assertTrue(multimap.containsEntry("foo", 3));
    assertSize(2);

    Collection<Integer> emptyCollection = Lists.newArrayList();
    multimap.putAll("bar", emptyCollection);
    assertSize(2);
    assertEquals(Collections.singleton("foo"), multimap.keySet());
  }

  public void testPutAllCollectionNull() {
    Collection<Integer> collection = Lists.newArrayList(1, nullValue());
    multimap.putAll(nullKey(), collection);
    assertTrue(multimap.containsEntry(nullKey(), 1));
    assertTrue(multimap.containsEntry(nullKey(), nullValue()));
    assertSize(2);
  }

  public void testPutAllEmptyCollection() {
    Collection<Integer> collection = Lists.newArrayList();
    multimap.putAll("foo", collection);
    assertSize(0);
    assertTrue(multimap.isEmpty());
  }

  public void testPutAllMultimap() {
    multimap.put("foo", 2);
    multimap.put("cow", 5);
    multimap.put(nullKey(), 2);
    Multimap<String, Integer> multimap2 = create();
    multimap2.put("foo", 1);
    multimap2.put("bar", 3);
    multimap2.put(nullKey(), nullValue());
    multimap.putAll(multimap2);
    assertTrue(multimap.containsEntry("foo", 2));
    assertTrue(multimap.containsEntry("cow", 5));
    assertTrue(multimap.containsEntry("foo", 1));
    assertTrue(multimap.containsEntry("bar", 3));
    assertTrue(multimap.containsEntry(nullKey(), nullValue()));
    assertTrue(multimap.containsEntry(nullKey(), 2));
    assertSize(6);
  }

  public void testPutAllReturn_emptyCollection() {
    assertFalse(multimap.putAll("foo", new ArrayList<Integer>()));
    assertFalse(multimap.putAll(create()));
  }

  public void testPutAllReturn_nonEmptyCollection() {
    assertTrue(multimap.putAll("foo", asList(1, 2, 3)));
    assertTrue(multimap.putAll("foo", asList(4, 5, 6)));
    assertFalse(multimap.putAll(create()));

    Multimap<String, Integer> other = create();
    other.putAll("bar", asList(7, 8, 9));
    assertTrue(multimap.putAll(other));
  }

  private void checkRemovedCollection(Collection<Integer> collection) {
    if (removedCollectionsAreModifiable()) {
      collection.add(9876);
      collection.remove(9876);
      assertFalse(collection.contains(9876));
    } else {
      try {
        collection.add(9876);
        fail();
      } catch (UnsupportedOperationException expected) {
      }
    }
  }

  public void testReplaceValues() {
    multimap.put("foo", 1);
    multimap.put("bar", 3);
    Collection<Integer> values = asList(2, nullValue());
    Collection<Integer> oldValues = multimap.replaceValues("foo", values);
    assertTrue(multimap.containsEntry("foo", 2));
    assertTrue(multimap.containsEntry("foo", nullValue()));
    assertTrue(multimap.containsEntry("bar", 3));
    assertSize(3);
    assertTrue(oldValues.contains(1));
    assertEquals(1, oldValues.size());
    checkRemovedCollection(oldValues);
  }

  public void testReplaceValuesEmpty() {
    multimap.put("foo", 1);
    multimap.put("bar", 3);
    Collection<Integer> values = asList();
    Collection<Integer> oldValues = multimap.replaceValues("foo", values);
    assertFalse(multimap.containsKey("foo"));
    assertTrue(multimap.containsEntry("bar", 3));
    assertSize(1);
    assertTrue(oldValues.contains(1));
    assertEquals(1, oldValues.size());
    checkRemovedCollection(oldValues);
  }

  public void testReplaceValuesNull() {
    multimap.put(nullKey(), 1);
    multimap.put("bar", 3);
    Collection<Integer> values = asList(2, nullValue());
    Collection<Integer> oldValues = multimap.replaceValues(nullKey(), values);
    assertTrue(multimap.containsEntry(nullKey(), 2));
    assertTrue(multimap.containsEntry(nullKey(), nullValue()));
    assertTrue(multimap.containsEntry("bar", 3));
    assertSize(3);
    assertTrue(oldValues.contains(1));
    assertEquals(1, oldValues.size());
    checkRemovedCollection(oldValues);
  }

  public void testReplaceValuesNotPresent() {
    multimap.put("bar", 3);
    Collection<Integer> values = asList(2, 4);
    Collection<Integer> oldValues = multimap.replaceValues("foo", values);
    assertTrue(multimap.containsEntry("foo", 2));
    assertTrue(multimap.containsEntry("foo", 4));
    assertTrue(multimap.containsEntry("bar", 3));
    assertSize(3);
    assertNotNull(oldValues);
    assertTrue(oldValues.isEmpty());
    checkRemovedCollection(oldValues);
  }

  public void testReplaceValuesDuplicates() {
    Collection<Integer> values = Lists.newArrayList(1, 2, 3, 2, 1);
    multimap.put("bar", 3);
    Collection<Integer> oldValues = multimap.replaceValues("bar", values);
    Collection<Integer> replacedValues = multimap.get("bar");
    assertSize(multimap.size());
    assertEquals(replacedValues.size(), multimap.size());
    assertEquals(1, oldValues.size());
    assertTrue(oldValues.contains(3));
    checkRemovedCollection(oldValues);
  }

  public void testRemove() {
    multimap.put("foo", 1);
    multimap.put("foo", 3);

    assertTrue(multimap.remove("foo", 1));
    assertFalse(multimap.containsEntry("foo", 1));
    assertTrue(multimap.containsEntry("foo", 3));
    assertSize(1);

    assertFalse(multimap.remove("bar", 3));
    assertTrue(multimap.containsEntry("foo", 3));
    assertSize(1);

    assertFalse(multimap.remove("foo", 2));
    assertTrue(multimap.containsEntry("foo", 3));
    assertSize(1);

    assertTrue(multimap.remove("foo", 3));
    assertFalse(multimap.containsKey("foo"));
    assertSize(0);
  }

  public void testRemoveNull() {
    multimap.put(nullKey(), 1);
    multimap.put(nullKey(), 3);
    multimap.put(nullKey(), nullValue());

    assertTrue(multimap.remove(nullKey(), 1));
    assertFalse(multimap.containsEntry(nullKey(), 1));
    assertTrue(multimap.containsEntry(nullKey(), 3));
    assertTrue(multimap.containsEntry(nullKey(), nullValue()));
    assertSize(2);

    assertTrue(multimap.remove(nullKey(), nullValue()));
    assertFalse(multimap.containsEntry(nullKey(), 1));
    assertTrue(multimap.containsEntry(nullKey(), 3));
    assertFalse(multimap.containsEntry(nullKey(), nullValue()));
    assertSize(1);
  }

  public void testRemoveAll() {
    multimap.put("foo", 1);
    multimap.put("foo", 3);
    Collection<Integer> removed = multimap.removeAll("foo");
    assertFalse(multimap.containsKey("foo"));
    assertSize(0);
    assertTrue(removed.contains(1));
    assertTrue(removed.contains(3));
    assertEquals(2, removed.size());
    checkRemovedCollection(removed);
  }

  public void testRemoveAllNull() {
    multimap.put(nullKey(), 1);
    multimap.put(nullKey(), nullValue());
    Collection<Integer> removed = multimap.removeAll(nullKey());
    assertFalse(multimap.containsKey(nullKey()));
    assertSize(0);
    assertTrue(removed.contains(1));
    assertTrue(removed.contains(nullValue()));
    assertEquals(2, removed.size());
    checkRemovedCollection(removed);
  }

  public void testRemoveAllNotPresent() {
    multimap.put("foo", 1);
    multimap.put("foo", 3);
    Collection<Integer> removed = multimap.removeAll("bar");
    assertSize(2);
    assertNotNull(removed);
    assertTrue(removed.isEmpty());
    checkRemovedCollection(removed);
  }

  public void testClear() {
    multimap.put("foo", 1);
    multimap.put("bar", 3);
    multimap.clear();
    assertEquals(0, multimap.keySet().size());
    assertSize(0);
  }

  public void testKeySet() {
    multimap.put("foo", 1);
    multimap.put("foo", nullValue());
    multimap.put(nullKey(), 3);
    Set<String> keys = multimap.keySet();
    assertEquals(2, keys.size());
    assertTrue(keys.contains("foo"));
    assertTrue(keys.contains(nullKey()));
    assertTrue(keys.containsAll(Lists.newArrayList("foo", nullKey())));
    assertFalse(keys.containsAll(Lists.newArrayList("foo", "bar")));
  }

  public void testValues() {
    multimap.put("foo", 1);
    multimap.put("foo", nullValue());
    multimap.put(nullKey(), 3);
    Collection<Integer> values = multimap.values();
    assertEquals(3, values.size());
    assertTrue(values.contains(1));
    assertTrue(values.contains(3));
    assertTrue(values.contains(nullValue()));
    assertFalse(values.contains(5));
  }

  public void testValuesClear() {
    multimap.put("foo", 1);
    multimap.put("foo", nullValue());
    multimap.put(nullKey(), 3);
    Collection<Integer> values = multimap.values();
    values.clear();
    assertTrue(multimap.isEmpty());
    assertTrue(values.isEmpty());
    assertFalse(multimap.containsEntry("foo", 1));
  }

  public void testValuesRemoveAllNullFromEmpty() {
    try {
      multimap.values().removeAll(null);
      // Returning successfully is not ideal, but tolerated.
    } catch (NullPointerException expected) {}
  }

  public void testValuesRetainAllNullFromEmpty() {
    try {
      multimap.values().retainAll(null);
      // Returning successfully is not ideal, but tolerated.
    } catch (NullPointerException expected) {}
  }

  // the entries collection is more thoroughly tested in MultimapCollectionTest
  @SuppressWarnings("unchecked") // varargs
  public void testEntries() {
    multimap.put("foo", 1);
    multimap.put("foo", nullValue());
    multimap.put(nullKey(), 3);
    Collection<Entry<String, Integer>> entries = multimap.entries();
    ASSERT.that(entries).hasContentsAnyOrder(
        Maps.immutableEntry("foo", 1),
        Maps.immutableEntry("foo", nullValue()),
        Maps.immutableEntry(nullKey(), 3));
  }

  public void testNoSuchElementException() {
    Iterator<Entry<String, Integer>> entries =
        multimap.entries().iterator();
    try {
      entries.next();
      fail();
    } catch (NoSuchElementException expected) {}
  }

  public void testAsMap() {
    multimap.put("foo", 1);
    multimap.put("foo", nullValue());
    multimap.put(nullKey(), 3);
    Map<String, Collection<Integer>> map = multimap.asMap();

    assertEquals(2, map.size());
    ASSERT.that(map.get("foo")).hasContentsAnyOrder(1, nullValue());
    ASSERT.that(map.get(nullKey())).hasContentsAnyOrder(3);
    assertNull(map.get("bar"));
    assertTrue(map.containsKey("foo"));
    assertTrue(map.containsKey(nullKey()));
    assertFalse(multimap.containsKey("bar"));

    ASSERT.that(map.remove("foo")).hasContentsAnyOrder(1, nullValue());
    assertFalse(multimap.containsKey("foo"));
    assertEquals(1, multimap.size());
    assertNull(map.remove("bar"));
    multimap.get(nullKey()).add(5);
    assertTrue(multimap.containsEntry(nullKey(), 5));
    assertEquals(2, multimap.size());
    multimap.get(nullKey()).clear();
    assertTrue(multimap.isEmpty());
    assertEquals(0, multimap.size());

    try {
      map.put("bar", asList(4, 8));
      fail("Expected UnsupportedOperationException");
    } catch (UnsupportedOperationException expected) {}

    multimap.put("bar", 5);
    assertSize(1);
    map.clear();
    assertSize(0);
  }

  public void testAsMapEntries() {
    multimap.put("foo", 1);
    multimap.put("foo", nullValue());
    multimap.put(nullKey(), 3);
    Collection<Entry<String, Collection<Integer>>> entries =
        multimap.asMap().entrySet();
    assertEquals(2, entries.size());

    assertTrue(entries.contains(
        Maps.immutableEntry("foo", multimap.get("foo"))));
    assertFalse(entries.contains(
        Maps.immutableEntry("bar", multimap.get("foo"))));
    assertFalse(entries.contains(
        Maps.immutableEntry("bar", null)));
    assertFalse(entries.contains(
        Maps.immutableEntry("foo", null)));
    assertFalse(entries.contains(
        Maps.immutableEntry("foo", asList(1, 4))));
    assertFalse(entries.contains("foo"));

    Iterator<Entry<String, Collection<Integer>>> iterator =
        entries.iterator();
    for (int i = 0; i < 2; i++) {
      assertTrue(iterator.hasNext());
      Entry<String, Collection<Integer>> entry = iterator.next();
      if ("foo".equals(entry.getKey())) {
        assertEquals(2, entry.getValue().size());
        assertTrue(entry.getValue().contains(1));
        assertTrue(entry.getValue().contains(nullValue()));
      } else {
        assertEquals(nullKey(), entry.getKey());
        assertEquals(1, entry.getValue().size());
        assertTrue(entry.getValue().contains(3));
      }
    }
    assertFalse(iterator.hasNext());
  }

  public void testAsMapToString() {
    multimap.put("foo", 1);
    assertEquals("{foo=[1]}", multimap.asMap().toString());
  }

  public void testKeys() {
    multimap.put("foo", 1);
    multimap.put("foo", 5);
    multimap.put("foo", nullValue());
    multimap.put(nullKey(), 3);
    Multiset<String> multiset = multimap.keys();
    assertEquals(3, multiset.count("foo"));
    assertEquals(1, multiset.count(nullKey()));
    ASSERT.that(multiset.elementSet()).hasContentsAnyOrder("foo", nullKey());
    assertEquals(2, multiset.entrySet().size());
    assertEquals(4, multiset.size());

    Set<Multiset.Entry<String>> entries = multimap.keys().entrySet();
    assertTrue(entries.contains(Multisets.immutableEntry("foo", 3)));
    assertFalse(entries.contains(Multisets.immutableEntry("foo", 2)));
    assertFalse(entries.contains(Maps.immutableEntry("foo", 3)));

    Multiset<String> foo3null1 =
        HashMultiset.create(asList("foo", "foo", nullKey(), "foo"));
    assertEquals(foo3null1, multiset);
    assertEquals(multiset, foo3null1);
    assertFalse(multiset.equals(
        HashMultiset.create(asList("foo", "foo", nullKey(), nullKey()))));
    assertEquals(foo3null1.hashCode(), multiset.hashCode());
    assertEquals(foo3null1.entrySet(), multiset.entrySet());
    assertEquals(multiset.entrySet(), foo3null1.entrySet());
    assertEquals(foo3null1.entrySet().hashCode(),
        multiset.entrySet().hashCode());

    assertEquals(0, multiset.remove("bar", 1));
    assertEquals(1, multiset.remove(nullKey(), 4));
    assertFalse(multimap.containsKey(nullKey()));
    assertSize(3);
    assertEquals("foo", entries.iterator().next().getElement());

    assertEquals(3, multiset.remove("foo", 1));
    assertTrue(multimap.containsKey("foo"));
    assertSize(2);
    assertEquals(2, multiset.setCount("foo", 0));
    assertEquals(0, multiset.setCount("bar", 0));
  }

  public void testKeysAdd() {
    multimap.put("foo", 1);
    Multiset<String> multiset = multimap.keys();

    try {
      multiset.add("bar");
      fail();
    } catch (UnsupportedOperationException expected) {}

    try {
      multiset.add("bar", 2);
      fail();
    } catch (UnsupportedOperationException expected) {}
  }

  public void testKeysContainsAll() {
    multimap.put("foo", 1);
    multimap.put("foo", 5);
    multimap.put("foo", nullValue());
    multimap.put(nullKey(), 3);
    Multiset<String> multiset = multimap.keys();

    assertTrue(multiset.containsAll(asList("foo", nullKey())));
    assertFalse(multiset.containsAll(asList("foo", "bar")));
  }

  public void testKeysClear() {
    multimap.put("foo", 1);
    multimap.put("foo", 5);
    multimap.put("foo", nullValue());
    multimap.put(nullKey(), 3);
    Multiset<String> multiset = multimap.keys();

    multiset.clear();
    assertTrue(multiset.isEmpty());
    assertTrue(multimap.isEmpty());
    assertSize(0);
    assertFalse(multimap.containsKey("foo"));
    assertFalse(multimap.containsKey(nullKey()));
  }

  public void testKeysToString() {
    multimap.put("foo", 7);
    multimap.put("foo", 8);
    assertEquals("[foo x 2]", multimap.keys().toString());
  }

  public void testKeysEntrySetIterator() {
    multimap.put("foo", 7);
    multimap.put("foo", 8);
    Iterator<Multiset.Entry<String>> iterator
        = multimap.keys().entrySet().iterator();
    assertTrue(iterator.hasNext());
    assertEquals(Multisets.immutableEntry("foo", 2), iterator.next());
    iterator.remove();
    assertFalse(iterator.hasNext());
    assertSize(0);
  }

  public void testKeysEntrySetToString() {
    multimap.put("foo", 7);
    multimap.put("foo", 8);
    assertEquals("[foo x 2]", multimap.keys().entrySet().toString());
  }

  public void testKeysEntrySetRemove() {
    multimap.putAll("foo", asList(1, 2, 3));
    multimap.putAll("bar", asList(4, 5));
    Set<Multiset.Entry<String>> entries = multimap.keys().entrySet();
    assertTrue(entries.remove(Multisets.immutableEntry("bar", 2)));
    assertEquals("[foo x 3]", multimap.keys().entrySet().toString());

    // doesn't exist in entries, should have no effect
    assertFalse(entries.remove(Multisets.immutableEntry("foo", 2)));
    assertEquals("[foo x 3]", multimap.keys().entrySet().toString());
    assertEquals("Multimap size after keys().entrySet().remove(entry)",
        3, multimap.size());
  }

  public void testEqualsTrue() {
    multimap.put("foo", 1);
    multimap.put("foo", nullValue());
    multimap.put(nullKey(), 3);
    assertEquals(multimap, multimap);

    Multimap<String, Integer> multimap2 = create();
    multimap2.put(nullKey(), 3);
    multimap2.put("foo", 1);
    multimap2.put("foo", nullValue());

    assertEquals(multimap, multimap2);
    assertEquals(multimap.hashCode(), multimap2.hashCode());
  }

  public void testEqualsFalse() {
    multimap.put("foo", 1);
    multimap.put("foo", 3);
    multimap.put("bar", 3);

    Multimap<String, Integer> multimap2 = create();
    multimap2.put("bar", 3);
    multimap2.put("bar", 1);
    assertFalse(multimap.equals(multimap2));

    multimap2.put("foo", 3);
    assertFalse(multimap.equals(multimap2));

    assertFalse(multimap.equals(nullValue()));
    assertFalse(multimap.equals("foo"));
  }

  public void testValuesIterator() {
    multimap.put("foo", 1);
    multimap.put("foo", 2);
    multimap.put(nullKey(), 4);
    int sum = 0;
    for (int i : multimap.values()) {
      sum += i;
    }
    assertEquals(7, sum);
  }

  public void testValuesIteratorEmpty() {
    int sum = 0;
    for (int i : multimap.values()) {
      sum += i;
    }
    assertEquals(0, sum);
  }

  public void testGetAddQuery() {
    multimap.put("foo", 1);
    multimap.put("foo", 3);
    multimap.put("bar", 4);
    Collection<Integer> values = multimap.get("foo");
    multimap.put("foo", 5);
    multimap.put("bar", 6);

    /* Verify that values includes effect of put. */
    assertEquals(3, values.size());
    assertTrue(values.contains(1));
    assertTrue(values.contains(5));
    assertFalse(values.contains(6));
    ASSERT.that(values).hasContentsAnyOrder(1, 3, 5);
    assertTrue(values.containsAll(asList(3, 5)));
    assertFalse(values.isEmpty());
    assertEquals(multimap.get("foo"), values);
    assertEquals(multimap.get("foo").hashCode(), values.hashCode());
    assertEquals(multimap.get("foo").toString(), values.toString());
  }

  public void testGetAddAll() {
    multimap.put("foo", 1);
    multimap.put("foo", 3);
    multimap.get("foo").addAll(asList(5, 7));
    multimap.get("bar").addAll(asList(6, 8));
    multimap.get("cow").addAll(Arrays.<Integer>asList());
    assertSize(6);
    ASSERT.that(multimap.get("foo")).hasContentsAnyOrder(1, 3, 5, 7);
    ASSERT.that(multimap.get("bar")).hasContentsAnyOrder(6, 8);
    ASSERT.that(multimap.get("cow")).isEmpty();
  }

  public void testGetRemoveAddQuery() {
    multimap.put("foo", 1);
    multimap.put("foo", 3);
    multimap.put("bar", 4);
    Collection<Integer> values = multimap.get("foo");
    Iterator<Integer> iterator = values.iterator();
    multimap.remove("foo", 1);
    multimap.remove("foo", 3);

    /* Verify that values includes effect of remove */
    assertEquals(0, values.size());
    assertFalse(values.contains(1));
    assertFalse(values.contains(6));
    assertTrue(values.isEmpty());
    assertEquals(multimap.get("foo"), values);
    assertEquals(multimap.get("foo").hashCode(), values.hashCode());
    assertEquals(multimap.get("foo").toString(), values.toString());

    multimap.put("foo", 5);

    /* Verify that values includes effect of put. */
    assertEquals(1, values.size());
    assertFalse(values.contains(1));
    assertTrue(values.contains(5));
    assertFalse(values.contains(6));
    assertEquals(5, values.iterator().next().intValue());
    assertFalse(values.isEmpty());
    assertEquals(multimap.get("foo"), values);
    assertEquals(multimap.get("foo").hashCode(), values.hashCode());
    assertEquals(multimap.get("foo").toString(), values.toString());

    try {
      iterator.hasNext();
    } catch (ConcurrentModificationException expected) {}
  }

  public void testModifyCollectionFromGet() {
    multimap.put("foo", 1);
    multimap.put("foo", 3);
    multimap.put("bar", 4);
    Collection<Integer> values = multimap.get("foo");

    assertTrue(values.add(5));
    assertSize(4);
    assertEquals(3, multimap.get("foo").size());
    assertTrue(multimap.containsEntry("foo", 5));

    values.clear();
    assertSize(1);
    assertFalse(multimap.containsKey("foo"));

    assertTrue(values.addAll(asList(7, 9)));
    assertSize(3);
    assertEquals(2, multimap.get("foo").size());
    assertTrue(multimap.containsEntry("foo", 7));
    assertTrue(multimap.containsEntry("foo", 9));
    assertFalse(values.addAll(Collections.<Integer>emptyList()));
    assertSize(3);

    assertTrue(values.remove(7));
    assertSize(2);
    assertEquals(1, multimap.get("foo").size());
    assertFalse(multimap.containsEntry("foo", 7));
    assertTrue(multimap.containsEntry("foo", 9));
    assertFalse(values.remove(77));
    assertSize(2);

    assertTrue(values.add(11));
    assertTrue(values.add(13));
    assertTrue(values.add(15));
    assertTrue(values.add(17));

    assertTrue(values.removeAll(asList(11, 15)));
    assertSize(4);
    ASSERT.that(multimap.get("foo")).hasContentsAnyOrder(9, 13, 17);
    assertFalse(values.removeAll(asList(21, 25)));
    assertSize(4);

    assertTrue(values.retainAll(asList(13, 17, 19)));
    assertSize(3);
    ASSERT.that(multimap.get("foo")).hasContentsAnyOrder(13, 17);
    assertFalse(values.retainAll(asList(13, 17, 19)));
    assertSize(3);

    values.remove(13);
    values.remove(17);
    assertTrue(multimap.get("foo").isEmpty());
    assertSize(1);
    assertFalse(multimap.containsKey("foo"));
  }

  public void testGetIterator() {
    multimap.put("foo", 1);
    multimap.put("foo", 3);
    multimap.put("foo", 5);
    multimap.put("bar", 4);
    Collection<Integer> values = multimap.get("foo");

    Iterator<Integer> iterator = values.iterator();
    assertTrue(iterator.hasNext());
    Integer v1 = iterator.next();
    assertTrue(iterator.hasNext());
    Integer v2 = iterator.next();
    iterator.remove();
    assertTrue(iterator.hasNext());
    Integer v3 = iterator.next();
    assertFalse(iterator.hasNext());

    ASSERT.that(asList(v1, v2, v3)).hasContentsAnyOrder(1, 3, 5);
    assertSize(3);
    assertTrue(multimap.containsEntry("foo", v1));
    assertFalse(multimap.containsEntry("foo", v2));
    assertTrue(multimap.containsEntry("foo", v3));

    iterator = values.iterator();
    assertTrue(iterator.hasNext());
    Integer n1 = iterator.next();
    iterator.remove();
    assertTrue(iterator.hasNext());
    Integer n3 = iterator.next();
    iterator.remove();
    assertFalse(iterator.hasNext());

    ASSERT.that(asList(n1, n3)).hasContentsAnyOrder(v1, v3);
    assertSize(1);
    assertFalse(multimap.containsKey("foo"));
  }

  public void testGetClear() {
    multimap.put("foo", 1);
    multimap.put("bar", 3);
    Collection<Integer> values = multimap.get("foo");
    multimap.clear();
    assertTrue(values.isEmpty());
  }

  public void testGetPutAllCollection() {
    Collection<Integer> values = multimap.get("foo");
    Collection<Integer> collection = Lists.newArrayList(1, 3);
    multimap.putAll("foo", collection);
    ASSERT.that(values).hasContentsAnyOrder(1, 3);
  }

  public void testGetPutAllMultimap() {
    multimap.put("foo", 2);
    multimap.put("cow", 5);
    multimap.put(nullKey(), 2);
    Collection<Integer> valuesFoo = multimap.get("foo");
    Collection<Integer> valuesBar = multimap.get("bar");
    Collection<Integer> valuesCow = multimap.get("cow");
    Collection<Integer> valuesNull = multimap.get(nullKey());
    Multimap<String, Integer> multimap2 = create();
    multimap2.put("foo", 1);
    multimap2.put("bar", 3);
    multimap2.put(nullKey(), nullValue());
    multimap.putAll(multimap2);

    ASSERT.that(valuesFoo).hasContentsAnyOrder(1, 2);
    ASSERT.that(valuesBar).hasContentsAnyOrder(3);
    ASSERT.that(valuesCow).hasContentsAnyOrder(5);
    ASSERT.that(valuesNull).hasContentsAnyOrder(nullValue(), 2);
  }

  public void testGetRemove() {
    multimap.put("foo", 1);
    multimap.put("foo", 3);
    Collection<Integer> values = multimap.get("foo");
    multimap.remove("foo", 1);
    ASSERT.that(values).hasContentsAnyOrder(3);
  }

  public void testGetRemoveAll() {
    multimap.put("foo", 1);
    multimap.put("foo", 3);
    Collection<Integer> values = multimap.get("foo");
    multimap.removeAll("foo");
    assertTrue(values.isEmpty());
  }

  public void testGetReplaceValues() {
    multimap.put("foo", 1);
    multimap.put("foo", 3);
    Collection<Integer> values = multimap.get("foo");
    multimap.replaceValues("foo", asList(1, 5));
    ASSERT.that(values).hasContentsAnyOrder(1, 5);

    multimap.replaceValues("foo", new ArrayList<Integer>());
    assertTrue(multimap.isEmpty());
    assertSize(0);
    assertTrue(values.isEmpty());
  }

  public void testEntriesUpdate() {
    multimap.put("foo", 1);
    Collection<Entry<String, Integer>> entries = multimap.entries();
    Iterator<Entry<String, Integer>> iterator = entries.iterator();

    assertTrue(iterator.hasNext());
    Entry<String, Integer> entry = iterator.next();
    assertEquals("foo", entry.getKey());
    assertEquals(1, entry.getValue().intValue());
    iterator.remove();
    assertFalse(iterator.hasNext());
    assertTrue(multimap.isEmpty());
    assertSize(0);

    try {
      entries.add(Maps.immutableEntry("bar", 2));
      fail("UnsupportedOperationException expected");
    } catch (UnsupportedOperationException expected) {}
    assertSize(0);
    assertFalse(multimap.containsEntry("bar", 2));

    multimap.put("bar", 2);
    assertSize(1);
    assertTrue(entries.contains(Maps.immutableEntry("bar", 2)));

    entries.clear();
    assertTrue(multimap.isEmpty());
    assertSize(0);
  }

  public void testEntriesRemove() {
    multimap.put("foo", 1);
    multimap.put("foo", nullValue());
    multimap.put(nullKey(), 3);
    Collection<Entry<String, Integer>> entries = multimap.entries();

    assertTrue(entries.remove(Maps.immutableEntry("foo", nullValue())));
    assertSize(2);
    assertFalse(multimap.containsEntry("foo", nullValue()));

    assertFalse(entries.remove(Maps.immutableEntry("foo", 3)));
    assertFalse(entries.remove(3.5));
    assertSize(2);
  }

  @SuppressWarnings("unchecked")
  public void testEntriesRemoveAll() {
    multimap.put("foo", 1);
    multimap.put("foo", 2);
    multimap.put("bar", 3);

    assertFalse(multimap.entries().removeAll(
        Collections.singleton(Maps.immutableEntry("foo", 3))));
    assertSize(3);

    assertTrue(multimap.entries().removeAll(asList(
        Maps.immutableEntry("foo", 3), Maps.immutableEntry("bar", 3))));
    assertSize(2);
    assertFalse(multimap.containsKey("bar"));
  }

  public void testEntriesRemoveAllNullFromEmpty() {
    try {
      multimap.entries().removeAll(null);
      // Returning successfully is not ideal, but tolerated.
    } catch (NullPointerException expected) {}
  }

  @SuppressWarnings("unchecked")
  public void testEntriesRetainAll() {
    multimap.put("foo", 1);
    multimap.put("foo", 2);
    multimap.put("bar", 3);

    assertFalse(multimap.entries().retainAll(asList(
        Maps.immutableEntry("foo", 1), Maps.immutableEntry("foo", 2),
        Maps.immutableEntry("foo", 3), Maps.immutableEntry("bar", 3))));
    assertSize(3);

    assertTrue(multimap.entries().retainAll(asList(
        Maps.immutableEntry("foo", 3), Maps.immutableEntry("bar", 3))));
    assertSize(1);
    assertTrue(multimap.containsEntry("bar", 3));
  }

  public void testEntriesRetainAllNullFromEmpty() {
    try {
      multimap.entries().retainAll(null);
      // Returning successfully is not ideal, but tolerated.
    } catch (NullPointerException expected) {}
  }

  public void testEntriesIterator() {
    multimap.put("foo", 3);
    Iterator<Entry<String, Integer>> iterator
        = multimap.entries().iterator();
    assertTrue(iterator.hasNext());
    assertEquals(Maps.immutableEntry("foo", 3), iterator.next());
    iterator.remove();
    assertFalse(iterator.hasNext());
    assertSize(0);
  }

  public void testEntriesToString() {
    multimap.put("foo", 3);
    Collection<Entry<String, Integer>> entries = multimap.entries();
    assertEquals("[foo=3]", entries.toString());
  }

  public void testEntriesToArray() {
    multimap.put("foo", 3);
    Collection<Entry<String, Integer>> entries = multimap.entries();
    Entry<?, ?>[] array = new Entry<?, ?>[3];
    assertSame(array, entries.toArray(array));
    assertEquals(Maps.immutableEntry("foo", 3), array[0]);
    assertNull(array[1]);
  }

  /**
   * Test calling setValue() on an entry returned by multimap.entries().
   */
  public void testEntrySetValue() {
    multimap.put("foo", 1);
    multimap.put("bar", 1);
    Collection<Entry<String, Integer>> entries = multimap.entries();
    Iterator<Entry<String, Integer>> iterator = entries.iterator();
    Entry<String, Integer> entrya = iterator.next();
    Entry<String, Integer> entryb = iterator.next();
    try {
      entrya.setValue(3);
      fail();
    } catch (UnsupportedOperationException expected) {}
    assertTrue(multimap.containsEntry("foo", 1));
    assertTrue(multimap.containsEntry("bar", 1));
    assertFalse(multimap.containsEntry("foo", 2));
    assertFalse(multimap.containsEntry("bar", 2));
    assertEquals(1, (int) entrya.getValue());
    assertEquals(1, (int) entryb.getValue());
  }

  /** Verify that the entries remain valid after iterating past them. */
  public void testEntriesCopy() {
    multimap.put("foo", 1);
    multimap.put("foo", 2);
    multimap.put("bar", 3);

    Set<Entry<String, Integer>> copy = Sets.newHashSet(multimap.entries());
    assertEquals(3, copy.size());
    assertTrue(copy.contains(Maps.immutableEntry("foo", 1)));
    assertTrue(copy.contains(Maps.immutableEntry("foo", 2)));
    assertTrue(copy.contains(Maps.immutableEntry("bar", 3)));
    assertFalse(copy.contains(Maps.immutableEntry("bar", 1)));

    multimap.removeAll("foo");
    assertEquals(3, copy.size());
    assertTrue(copy.contains(Maps.immutableEntry("foo", 1)));
    assertTrue(copy.contains(Maps.immutableEntry("foo", 2)));
    assertTrue(copy.contains(Maps.immutableEntry("bar", 3)));
    assertFalse(copy.contains(Maps.immutableEntry("bar", 1)));
  }

  public void testKeySetRemove() {
    multimap.put("foo", 1);
    multimap.put("foo", nullValue());
    multimap.put(nullKey(), 3);
    Set<String> keys = multimap.keySet();
    assertTrue(keys.remove("foo"));
    assertFalse(keys.remove("bar"));
    assertSize(1);
    assertFalse(multimap.containsKey("foo"));
    assertTrue(multimap.containsEntry(nullKey(), 3));
  }

  public void testKeySetRemoveAllNullFromEmpty() {
    try {
      multimap.keySet().removeAll(null);
      fail();
    } catch (NullPointerException expected) {}
  }

  public void testKeySetRetainAllNullFromEmpty() {
    try {
      multimap.keySet().retainAll(null);
      // Returning successfully is not ideal, but tolerated.
    } catch (NullPointerException expected) {}
  }

  public void testKeySetIterator() {
    multimap.put("foo", 1);
    multimap.put("foo", nullValue());
    multimap.put(nullKey(), 3);

    Iterator<String> iterator = multimap.keySet().iterator();
    while (iterator.hasNext()) {
      String key = iterator.next();
      if ("foo".equals(key)) {
        iterator.remove();
      }
    }
    assertSize(1);
    assertFalse(multimap.containsKey("foo"));
    assertTrue(multimap.containsEntry(nullKey(), 3));

    iterator = multimap.keySet().iterator();
    assertEquals(nullKey(), iterator.next());
    iterator.remove();
    assertTrue(multimap.isEmpty());
    assertSize(0);
  }

  public void testKeySetClear() {
    multimap.put("foo", 1);
    multimap.put("foo", nullValue());
    multimap.put(nullKey(), 3);

    multimap.keySet().clear();
    assertTrue(multimap.isEmpty());
    assertSize(0);
  }

  public void testValuesIteratorRemove() {
    multimap.put("foo", 1);
    multimap.put("foo", 2);
    multimap.put(nullKey(), 4);

    Iterator<Integer> iterator = multimap.values().iterator();
    while (iterator.hasNext()) {
      int value = iterator.next();
      if ((value % 2) == 0) {
        iterator.remove();
      }
    }

    assertSize(1);
    assertTrue(multimap.containsEntry("foo", 1));
  }

  public void testAsMapEntriesUpdate() {
    multimap.put("foo", 1);
    multimap.put("foo", 3);
    Collection<Entry<String, Collection<Integer>>> entries =
        multimap.asMap().entrySet();
    Entry<String, Collection<Integer>> entry = entries.iterator().next();
    Collection<Integer> values = entry.getValue();

    multimap.put("foo", 5);
    assertEquals(3, values.size());
    assertTrue(values.contains(5));

    values.add(7);
    assertSize(4);
    assertTrue(multimap.containsValue(7));

    multimap.put("bar", 4);
    assertEquals(2, entries.size());
    assertSize(5);

    assertTrue(entries.remove(entry));
    assertSize(1);
    assertFalse(multimap.containsKey("foo"));
    assertTrue(multimap.containsKey("bar"));
    assertFalse(entries.remove("foo"));
    assertFalse(entries.remove(
        Maps.immutableEntry("foo", Collections.singleton(2))));
    assertSize(1);

    Iterator<Entry<String, Collection<Integer>>> iterator =
        entries.iterator();
    assertTrue(iterator.hasNext());
    iterator.next();
    iterator.remove();
    assertFalse(iterator.hasNext());
    assertSize(0);
    assertTrue(multimap.isEmpty());

    multimap.put("bar", 8);
    assertSize(1);
    entries.clear();
    assertSize(0);
  }

  public void testToStringNull() {
    multimap.put("foo", 3);
    multimap.put("foo", -1);
    multimap.put(nullKey(), nullValue());
    multimap.put("bar", 1);
    multimap.put("foo", 2);
    multimap.put(nullKey(), 0);
    multimap.put("bar", 2);
    multimap.put("bar", nullValue());
    multimap.put("foo", nullValue());
    multimap.put("foo", 4);
    multimap.put(nullKey(), -1);
    multimap.put("bar", 3);
    multimap.put("bar", 1);
    multimap.put("foo", 1);

    // This test is brittle. The original test was meant to validate the
    // contents of the string itself, but key and value ordering tend
    // to change under unpredictable circumstances. Instead, we're just ensuring
    // that the string not return null and, implicitly, not throw an exception.
    assertNotNull(multimap.toString());
  }

  @GwtIncompatible("SerializableTester")
  public void testSerializable() {
    multimap = createSample();
    assertEquals(multimap, SerializableTester.reserialize(multimap));
  }

  public void testEmptyToString() {
    Multimap<String, Integer> map = create();
    assertEquals("{}", map.toString());
    assertEquals("[]", map.entries().toString());
  }

  public void testEmptyGetToString() {
    Multimap<String, Integer> map = create();
    map.get("foo"); // shouldn't have any side-effect
    assertEquals("{}", map.toString());
    assertEquals("[]", map.entries().toString());
  }

  public void testGetRemoveToString() {
    Multimap<String, Integer> map = create();
    map.put("bar", 1);
    map.put("foo", 2);
    map.put("bar", 3);
    map.get("foo").remove(2);
    map.get("bar").remove(1);
    assertEquals("{bar=[3]}", map.toString());
    assertEquals("[bar=3]", map.entries().toString());
  }

  public void testRemoveToString() {
    Multimap<String, Integer> map = create();
    map.put("foo", 1);
    map.put("foo", 2);
    map.remove("foo", 1);
    assertEquals("[foo=2]", map.entries().toString());
  }
}
