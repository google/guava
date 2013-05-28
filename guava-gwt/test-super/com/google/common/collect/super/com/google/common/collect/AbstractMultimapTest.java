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
import java.util.Iterator;
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
}

