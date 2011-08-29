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
import static org.junit.contrib.truth.Truth.ASSERT;

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

  /**
   * Test adding duplicate key-value pairs to multimap.
   */
  public void testDuplicates() {
    Multimap<String, Integer> multimap = create();
    multimap.put("foo", 1);
    multimap.put("foo", 3);
    multimap.put("bar", 3);
    multimap.put("foo", 1);
    assertEquals(4, multimap.size());
    assertTrue(multimap.containsEntry("foo", 1));
    multimap.remove("foo", 1);
    assertEquals(3, multimap.size());
    assertTrue(multimap.containsEntry("foo", 1));
  }

  /**
   * Test returned boolean when adding duplicate key-value pairs to multimap.
   */
  public void testPutReturn() {
    Multimap<String, Integer> multimap = create();
    assertTrue(multimap.put("foo", 1));
    assertTrue(multimap.put("foo", 1));
    assertTrue(multimap.put("foo", 3));
    assertTrue(multimap.put("bar", 5));
  }

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
   * Test the ordering of the values returned by multimap.get().
   */
  public void testPutGetOrdering() {
    Multimap<String, Integer> multimap = create();
    multimap.put("foo", 1);
    multimap.put("foo", 3);
    multimap.put("bar", 3);
    Iterator<Integer> values = multimap.get("foo").iterator();
    assertEquals(Integer.valueOf(1), values.next());
    assertEquals(Integer.valueOf(3), values.next());
  }

  /**
   * Test List-specific methods on List returned by get().
   */
  public void testListMethods() {
    ListMultimap<String, Integer> multimap = create();
    multimap.put("foo", 1);
    multimap.put("foo", 3);
    multimap.put("foo", 5);
    List<Integer> list = multimap.get("foo");

    list.add(1, 2);
    assertEquals(4, multimap.size());
    ASSERT.that(multimap.get("foo")).hasContentsInOrder(1, 2, 3, 5);

    list.addAll(3, asList(4, 8));
    assertEquals(6, multimap.size());
    ASSERT.that(multimap.get("foo")).hasContentsInOrder(1, 2, 3, 4, 8, 5);

    assertEquals(8, list.get(4).intValue());
    assertEquals(4, list.indexOf(8));
    assertEquals(4, list.lastIndexOf(8));

    list.remove(4);
    assertEquals(5, multimap.size());
    ASSERT.that(multimap.get("foo")).hasContentsInOrder(1, 2, 3, 4, 5);

    list.set(4, 10);
    assertEquals(5, multimap.size());
    ASSERT.that(multimap.get("foo")).hasContentsInOrder(1, 2, 3, 4, 10);
  }
  
  public void testListMethodsIncludingSublist() {
    ListMultimap<String, Integer> multimap = create();
    multimap.put("foo", 1);
    multimap.put("foo", 2);
    multimap.put("foo", 3);
    multimap.put("foo", 4);
    multimap.put("foo", 10);
    List<Integer> list = multimap.get("foo");
    
    List<Integer> sublist = list.subList(1, 4);
    ASSERT.that(sublist).hasContentsInOrder(2, 3, 4);
    list.set(3, 6);
    ASSERT.that(multimap.get("foo")).hasContentsInOrder(1, 2, 3, 6, 10);
  }

  /**
   * Test sublist of List returned by get() after the original list is updated.
   */
  public void testSublistAfterListUpdate() {
    ListMultimap<String, Integer> multimap = create();
    multimap.put("foo", 1);
    multimap.put("foo", 2);
    multimap.put("foo", 3);
    multimap.put("foo", 4);
    multimap.put("foo", 5);

    List<Integer> list = multimap.get("foo");
    List<Integer> sublist = list.subList(1, 4);
    ASSERT.that(sublist).hasContentsInOrder(2, 3, 4);
    list.set(3, 6);
    ASSERT.that(multimap.get("foo")).hasContentsInOrder(1, 2, 3, 6, 5);
    ASSERT.that(sublist).hasContentsInOrder(2, 3, 6);
  }

  /**
   * Test ListIterator methods that don't change the multimap.
   */
  public void testListIteratorNavigate() {
    ListMultimap<String, Integer> multimap = create();
    multimap.put("foo", 1);
    multimap.put("foo", 3);
    List<Integer> list = multimap.get("foo");
    ListIterator<Integer> iterator = list.listIterator();

    assertFalse(iterator.hasPrevious());
    assertTrue(iterator.hasNext());
    assertEquals(0, iterator.nextIndex());
    assertEquals(-1, iterator.previousIndex());

    assertEquals(1, iterator.next().intValue());
    assertEquals(3, iterator.next().intValue());
    assertTrue(iterator.hasPrevious());
    assertFalse(iterator.hasNext());

    assertEquals(3, iterator.previous().intValue());
    assertEquals(1, iterator.previous().intValue());
  }

  /**
   * Test ListIterator methods that change the multimap.
   */
  public void testListIteratorUpdate() {
    ListMultimap<String, Integer> multimap = create();
    multimap.put("foo", 1);
    multimap.put("foo", 3);
    multimap.put("foo", 5);
    List<Integer> list = multimap.get("foo");
    ListIterator<Integer> iterator = list.listIterator();

    assertEquals(1, iterator.next().intValue());
    iterator.set(2);
    ASSERT.that(multimap.get("foo")).hasContentsInOrder(2, 3, 5);

    assertEquals(3, iterator.next().intValue());
    iterator.remove();
    ASSERT.that(multimap.get("foo")).hasContentsInOrder(2, 5);
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
   * Test calling set() on a sublist.
   */
  public void testSublistSet() {
    ListMultimap<String, Integer> multimap = create();
    multimap.putAll("foo", asList(1, 2, 3, 4, 5));
    List<Integer> list = multimap.get("foo");
    ASSERT.that(multimap.get("foo")).hasContentsInOrder(1, 2, 3, 4, 5);
    List<Integer> sublist = list.subList(1, 4);
    ASSERT.that(sublist).hasContentsInOrder(2, 3, 4);

    sublist.set(1, 6);
    ASSERT.that(multimap.get("foo")).hasContentsInOrder(1, 2, 6, 4, 5);
  }

  /**
   * Test removing elements from a sublist.
   */
  public void testSublistRemove() {
    ListMultimap<String, Integer> multimap = create();
    multimap.putAll("foo", asList(1, 2, 3, 4, 5));
    List<Integer> list = multimap.get("foo");
    ASSERT.that(multimap.get("foo")).hasContentsInOrder(1, 2, 3, 4, 5);
    List<Integer> sublist = list.subList(1, 4);
    ASSERT.that(sublist).hasContentsInOrder(2, 3, 4);

    sublist.remove(1);
    assertEquals(4, multimap.size());
    ASSERT.that(multimap.get("foo")).hasContentsInOrder(1, 2, 4, 5);

    sublist.removeAll(Collections.singleton(4));
    assertEquals(3, multimap.size());
    ASSERT.that(multimap.get("foo")).hasContentsInOrder(1, 2, 5);

    sublist.remove(0);
    assertEquals(2, multimap.size());
    ASSERT.that(multimap.get("foo")).hasContentsInOrder(1, 5);
  }

  /**
   * Test adding elements to a sublist.
   */
  public void testSublistAdd() {
    ListMultimap<String, Integer> multimap = create();
    multimap.putAll("foo", asList(1, 2, 3, 4, 5));
    List<Integer> list = multimap.get("foo");
    ASSERT.that(multimap.get("foo")).hasContentsInOrder(1, 2, 3, 4, 5);
    List<Integer> sublist = list.subList(1, 4);
    ASSERT.that(sublist).hasContentsInOrder(2, 3, 4);

    sublist.add(6);
    assertEquals(6, multimap.size());
    ASSERT.that(multimap.get("foo")).hasContentsInOrder(1, 2, 3, 4, 6, 5);

    sublist.add(0, 7);
    assertEquals(7, multimap.size());
    ASSERT.that(multimap.get("foo")).hasContentsInOrder(1, 7, 2, 3, 4, 6, 5);
  }

  /**
   * Test clearing a sublist.
   */
  public void testSublistClear() {
    ListMultimap<String, Integer> multimap = create();
    multimap.putAll("foo", asList(1, 2, 3, 4, 5));
    List<Integer> list = multimap.get("foo");
    ASSERT.that(multimap.get("foo")).hasContentsInOrder(1, 2, 3, 4, 5);
    List<Integer> sublist = list.subList(1, 4);
    ASSERT.that(sublist).hasContentsInOrder(2, 3, 4);

    sublist.clear();
    assertEquals(2, multimap.size());
    ASSERT.that(multimap.get("foo")).hasContentsInOrder(1, 5);
  }

  /**
   * Test adding elements to an empty sublist with an empty ancestor.
   */
  public void testSublistAddToEmpty() {
    ListMultimap<String, Integer> multimap = create();
    multimap.putAll("foo", asList(1, 2, 3, 4, 5));
    List<Integer> list = multimap.get("foo");
    ASSERT.that(multimap.get("foo")).hasContentsInOrder(1, 2, 3, 4, 5);
    List<Integer> sublist = list.subList(0, 5);
    ASSERT.that(sublist).hasContentsInOrder(1, 2, 3, 4, 5);

    sublist.retainAll(Collections.EMPTY_LIST);
    assertTrue(multimap.isEmpty());

    sublist.add(6);
    assertEquals(1, multimap.size());
    assertTrue(multimap.containsEntry("foo", 6));
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
    ASSERT.that(multimap.get("foo")).hasContentsInOrder(1, 6, 3, 4, 5);

    assertTrue(iterator.hasNext());
    assertEquals(3, iterator.next().intValue());
    iterator.remove();
    ASSERT.that(multimap.get("foo")).hasContentsInOrder(1, 6, 4, 5);
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
    ASSERT.that(multimap.get("bar")).hasContentsInOrder(13, 11, 12);
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
    ASSERT.that(multimap.get("foo")).hasContentsInOrder(1, 2, 2);
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
    ASSERT.that(multimap.get("foo")).hasContentsInOrder(1);
  }
}
