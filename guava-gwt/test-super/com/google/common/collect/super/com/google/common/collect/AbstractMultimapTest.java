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

import junit.framework.TestCase;

import java.util.Collection;
import java.util.Collections;
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
}

