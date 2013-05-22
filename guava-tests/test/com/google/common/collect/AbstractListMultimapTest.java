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

import static com.google.common.collect.testing.IteratorFeature.MODIFIABLE;
import static java.util.Arrays.asList;
import static org.truth0.Truth.ASSERT;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.collect.testing.ListIteratorTester;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

/**
 * Tests for {@code ListMultimap} implementations.
 *
 * @author Jared Levy
 */
@GwtCompatible(emulated = true)
public abstract class AbstractListMultimapTest extends AbstractMultimapTest {

  @Override protected abstract ListMultimap<String, Integer> create();

  public void testPutAllReturn_existingElements() {
    Multimap<String, Integer> multimap = create();
    assertTrue(multimap.putAll("foo", asList(1, 2, 3)));
    assertTrue(multimap.put("foo", 1));
    assertTrue(multimap.putAll("foo", asList(1, 2, 3)));
    assertTrue(multimap.putAll("foo", asList(1, 3)));

    Multimap<String, Integer> other = create();
    other.putAll("foo", asList(1, 2));
    assertTrue(multimap.putAll(other));

    other.putAll("bar", asList(1, 2));
    assertTrue(multimap.putAll(other));
    assertTrue(other.putAll(multimap));
  }

  /**
   * Confirm that get() returns a collection equal to a List.
   */
  public void testGetEquals() {
    Multimap<String, Integer> multimap = create();
    multimap.put("foo", 1);
    multimap.put("foo", 3);
    assertEquals(ImmutableList.of(1, 3), multimap.get("foo"));
  }

  public void testAsMapEquals() {
    Multimap<String, Integer> multimap = getMultimap();
    multimap.put("foo", 1);
    multimap.put("foo", nullValue());
    multimap.put(nullKey(), 3);
    Map<String, Collection<Integer>> map = multimap.asMap();

    Map<String, Collection<Integer>> equalMap = Maps.newHashMap();
    equalMap.put("foo", asList(1, nullValue()));
    equalMap.put(nullKey(), asList(3));
    assertEquals(map, equalMap);
    assertEquals(equalMap, map);
    assertEquals(equalMap.hashCode(), multimap.hashCode());

    Map<String, Collection<Integer>> unequalMap = Maps.newHashMap();
    equalMap.put("foo", asList(3, nullValue()));
    equalMap.put(nullKey(), asList(1));
    assertFalse(map.equals(unequalMap));
    assertFalse(unequalMap.equals(map));
  }

  /**
   * Confirm that asMap().entrySet() returns values equal to a List.
   */
  public void testAsMapEntriesEquals() {
    Multimap<String, Integer> multimap = create();
    multimap.put("foo", 1);
    multimap.put("foo", 3);
    Iterator<Map.Entry<String, Collection<Integer>>> i =
        multimap.asMap().entrySet().iterator();
    Map.Entry<String, Collection<Integer>> entry = i.next();
    assertEquals("foo", entry.getKey());
    assertEquals(ImmutableList.of(1, 3), entry.getValue());
    assertFalse(i.hasNext());
  }

  public void testAsMapValuesRemove() {
    Multimap<String, Integer> multimap = create();
    multimap.put("foo", 1);
    multimap.put("foo", 3);
    multimap.put("bar", 3);
    Collection<Collection<Integer>> asMapValues = multimap.asMap().values();
    assertFalse(asMapValues.remove(asList(3, 1)));
    assertEquals(3, multimap.size());
    assertTrue(asMapValues.remove(asList(1, 3)));
    assertEquals(1, multimap.size());
  }

  /**
   * Test multimap.equals() for multimaps with different insertion orderings.
   */
  public void testEqualsOrdering() {
    Multimap<String, Integer> multimap = create();
    multimap.put("foo", 1);
    multimap.put("foo", 3);
    multimap.put("bar", 3);
    Multimap<String, Integer> multimap2 = create();
    multimap2.put("foo", 3);
    multimap2.put("foo", 1);
    multimap2.put("bar", 3);
    assertFalse(multimap.equals(multimap2));
  }

  /**
   * Test calling toString() on the multimap, which does not have a
   * deterministic iteration order for keys but does for values.
   */
  public void testToString() {
    String s = createSample().toString();
    assertTrue(s.equals("{foo=[3, -1, 2, 4, 1], bar=[1, 2, 3, 1]}")
        || s.equals("{bar=[1, 2, 3, 1], foo=[3, -1, 2, 4, 1]}"));
  }

  /**
   * Test updates through a list iterator retrieved by
   * multimap.get(key).listIterator(index).
   */
  public void testListIteratorIndexUpdate() {
    ListMultimap<String, Integer> multimap = create();
    multimap.putAll("foo", asList(1, 2, 3, 4, 5));
    ListIterator<Integer> iterator = multimap.get("foo").listIterator(1);

    assertEquals(2, iterator.next().intValue());
    iterator.set(6);
    ASSERT.that(multimap.get("foo")).has().allOf(1, 6, 3, 4, 5).inOrder();

    assertTrue(iterator.hasNext());
    assertEquals(3, iterator.next().intValue());
    iterator.remove();
    ASSERT.that(multimap.get("foo")).has().allOf(1, 6, 4, 5).inOrder();
    assertEquals(4, multimap.size());
  }

  @GwtIncompatible("unreasonable slow")
  public void testGetIteration() {
    List<Integer> addItems = ImmutableList.of(99, 88, 77);

    for (final int startIndex : new int[] {0, 3, 5}) {
      new ListIteratorTester<Integer>(3, addItems, MODIFIABLE,
          Lists.newArrayList(2, 3, 4, 7, 8), startIndex) {
        private ListMultimap<String, Integer> multimap;

        @Override protected ListIterator<Integer> newTargetIterator() {
          multimap = create();
          multimap.put("bar", 1);
          multimap.putAll("foo", asList(2, 3, 4));
          multimap.putAll("bar", asList(5, 6));
          multimap.putAll("foo", asList(7, 8));
          return multimap.get("foo").listIterator(startIndex);
        }

        @Override protected void verify(List<Integer> elements) {
          assertEquals(elements, multimap.get("foo"));
        }
      }.test();
    }
  }

  public void testListGetSet() {
    ListMultimap<String, Integer> map = create();
    map.put("bar", 1);
    map.get("bar").set(0, 2);
    assertEquals("{bar=[2]}", map.toString());
    assertEquals("[bar=2]", map.entries().toString());
  }

  public void testListPutAllIterable() {
    Multimap<String, Integer> map = create();
    map.putAll("foo", asList(1, 2));
    assertEquals("{foo=[1, 2]}", map.toString());
    assertEquals("[foo=1, foo=2]", map.entries().toString());
  }

  public void testListRemoveAll() {
    Multimap<String, Integer> map = create();
    map.put("bar", 1);
    map.put("foo", 2);
    map.put("bar", 3);
    map.put("bar", 4);
    map.removeAll("foo");
    assertEquals("[bar=1, bar=3, bar=4]", map.entries().toString());
    assertEquals("{bar=[1, 3, 4]}", map.toString());
    map.removeAll("bar");
    assertEquals("[]", map.entries().toString());
    assertEquals("{}", map.toString());
  }

  public void testListEquals() {
    Multimap<String, Integer> map1 = create();
    map1.put("bar", 1);
    map1.put("foo", 2);
    map1.put("bar", 3);
    Multimap<String, Integer> map2 = ArrayListMultimap.create();
    map2.putAll(map1);
    assertTrue(map1.equals(map2));
    assertTrue(map2.equals(map1));
    assertFalse(map1.equals(null));
    assertFalse(map1.equals(new Object()));
  }

  public void testListHashCode() {
    Multimap<String, Integer> map1 = create();
    map1.put("bar", 1);
    map1.put("foo", 2);
    map1.put("bar", 3);
    Multimap<String, Integer> map2 = ArrayListMultimap.create();
    map2.putAll(map1);
    assertEquals(map1.hashCode(), map2.hashCode());
  }

  public void testListAddIndex() {
    ListMultimap<String, Integer> multimap = create();
    multimap.put("bar", 11);
    multimap.put("bar", 12);
    multimap.get("bar").add(0, 13);
    ASSERT.that(multimap.get("bar")).has().allOf(13, 11, 12).inOrder();
  }

  /**
   * According to the AbstractCollection.retainAll() implementation,
   * {@code A.retainAll(B)} should keep all occurrences of each object in B,
   * so even though the collection that this test passes to retainAll() has
   * fewer occurrences of 2 than the multimap has, all of the 2s should be
   * retained.
   */
  public void testGetRetainAll() {
    // TODO: test this logic in ListRetainAllTester
    ListMultimap<String, Integer> multimap = create();
    multimap.putAll("foo", asList(1, 2, 2, 3, 3, 3));

    multimap.get("foo").retainAll(asList(1, 2, 4));
    ASSERT.that(multimap.get("foo")).has().allOf(1, 2, 2).inOrder();
  }

  /**
   * According to the AbstractCollection.removeAll() implementation,
   * {@code A.removeAll(B)} should remove all occurrences of each object in B,
   * so even though the collection that this test passes to removeAll() has
   * fewer occurrences of 2 and 3 than the multimap has, there should be no
   * 2s or 3s remaining in the collection.
   */
  public void testGetRemoveAll_someValuesRemain() {
    // TODO: test this logic in ListRemoveAllTester
    ListMultimap<String, Integer> multimap = create();
    multimap.putAll("foo", asList(1, 2, 2, 3, 3, 3));

    multimap.get("foo").removeAll(asList(2, 3, 3, 4));
    ASSERT.that(multimap.get("foo")).has().item(1);
  }
}
