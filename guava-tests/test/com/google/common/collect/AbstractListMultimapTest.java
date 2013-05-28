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

import java.util.ListIterator;

/**
 * Tests for {@code ListMultimap} implementations.
 *
 * @author Jared Levy
 */
@GwtCompatible
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
