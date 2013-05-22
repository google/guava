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
import static org.truth0.Truth.ASSERT;

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

  // the entries collection is more thoroughly tested in MultimapCollectionTest
  @SuppressWarnings("unchecked") // varargs
  public void testEntries() {
    multimap.put("foo", 1);
    multimap.put("foo", nullValue());
    multimap.put(nullKey(), 3);
    Collection<Entry<String, Integer>> entries = multimap.entries();
    ASSERT.that(entries).has().allOf(
        Maps.immutableEntry("foo", 1),
        Maps.immutableEntry("foo", nullValue()),
        Maps.immutableEntry(nullKey(), 3));
  }

  public void testAsMap() {
    multimap.put("foo", 1);
    multimap.put("foo", nullValue());
    multimap.put(nullKey(), 3);
    Map<String, Collection<Integer>> map = multimap.asMap();

    assertEquals(2, map.size());
    ASSERT.that(map.get("foo")).has().allOf(1, nullValue());
    ASSERT.that(map.get(nullKey())).has().item(3);
    assertNull(map.get("bar"));
    assertTrue(map.containsKey("foo"));
    assertTrue(map.containsKey(nullKey()));
    assertFalse(multimap.containsKey("bar"));

    ASSERT.that(map.remove("foo")).has().allOf(1, nullValue());
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

  public void testAsMapEntriesToArray() {
    multimap.put("foo", 1);
    multimap.put("foo", nullValue());
    multimap.put(nullKey(), 3);
    Collection<Entry<String, Collection<Integer>>> entries =
        multimap.asMap().entrySet();

    ASSERT.that(entries.toArray()).has().allOf(
        Maps.immutableEntry("foo", multimap.get("foo")),
        Maps.immutableEntry(nullKey(), multimap.get(nullKey())));
    ASSERT.that(entries.toArray(new Entry[2])).has().allOf(
        Maps.immutableEntry("foo", multimap.get("foo")),
        Maps.immutableEntry(nullKey(), multimap.get(nullKey())));
  }

  public void testAsMapValuesToArray() {
    multimap.put("foo", 1);
    multimap.put("foo", nullValue());
    multimap.put(nullKey(), 3);
    Collection<Collection<Integer>> values =
        multimap.asMap().values();

    ASSERT.that(values.toArray()).has().allOf(
        multimap.get("foo"), multimap.get(nullKey()));
    ASSERT.that(values.toArray(new Collection[2])).has().allOf(
        multimap.get("foo"), multimap.get(nullKey()));
  }

  public void testAsMapKeySetToArray() {
    multimap.put("foo", 1);
    multimap.put("foo", nullValue());
    multimap.put(nullKey(), 3);
    Set<String> keySet = multimap.asMap().keySet();

    ASSERT.that(keySet.toArray()).has().allOf("foo", nullKey());
    ASSERT.that(keySet.toArray(new String[2])).has().allOf("foo", nullKey());
  }

  public void testKeys() {
    multimap.put("foo", 1);
    multimap.put("foo", 5);
    multimap.put("foo", nullValue());
    multimap.put(nullKey(), 3);
    Multiset<String> multiset = multimap.keys();
    assertEquals(3, multiset.count("foo"));
    assertEquals(1, multiset.count(nullKey()));
    ASSERT.that(multiset.elementSet()).has().allOf("foo", nullKey());
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
    ASSERT.that(values).has().allOf(1, 3, 5);
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
    ASSERT.that(multimap.get("foo")).has().allOf(1, 3, 5, 7);
    ASSERT.that(multimap.get("bar")).has().allOf(6, 8);
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
    ASSERT.that(multimap.get("foo")).has().allOf(9, 13, 17);
    assertFalse(values.removeAll(asList(21, 25)));
    assertSize(4);

    assertTrue(values.retainAll(asList(13, 17, 19)));
    assertSize(3);
    ASSERT.that(multimap.get("foo")).has().allOf(13, 17);
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

    ASSERT.that(asList(v1, v2, v3)).has().allOf(1, 3, 5);
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

    ASSERT.that(asList(n1, n3)).has().allOf(v1, v3);
    assertSize(1);
    assertFalse(multimap.containsKey("foo"));
  }

  public void testGetPutAllCollection() {
    Collection<Integer> values = multimap.get("foo");
    Collection<Integer> collection = Lists.newArrayList(1, 3);
    multimap.putAll("foo", collection);
    ASSERT.that(values).has().allOf(1, 3);
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

    ASSERT.that(valuesFoo).has().allOf(1, 2);
    ASSERT.that(valuesBar).has().item(3);
    ASSERT.that(valuesCow).has().item(5);
    ASSERT.that(valuesNull).has().allOf(nullValue(), 2);
  }

  public void testGetRemove() {
    multimap.put("foo", 1);
    multimap.put("foo", 3);
    Collection<Integer> values = multimap.get("foo");
    multimap.remove("foo", 1);
    ASSERT.that(values).has().item(3);
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
    ASSERT.that(values).has().allOf(1, 5);

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

  @GwtIncompatible("SerializableTester")
  public void testSerializable() {
    multimap = createSample();
    assertEquals(multimap, SerializableTester.reserialize(multimap));
  }
}
