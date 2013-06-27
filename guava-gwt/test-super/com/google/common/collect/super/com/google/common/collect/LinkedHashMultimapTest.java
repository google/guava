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
import com.google.common.testing.EqualsTester;

import junit.framework.TestCase;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

/**
 * Unit tests for {@code LinkedHashMultimap}.
 *
 * @author Jared Levy
 */
@GwtCompatible(emulated = true)
public class LinkedHashMultimapTest extends TestCase {

  public void testValueSetHashTableExpansion() {
    LinkedHashMultimap<String, Integer> multimap = LinkedHashMultimap.create();
    for (int z = 1; z <= 100; z++) {
      multimap.put("a", z);
      // The Eclipse compiler (and hence GWT) rejects a parameterized cast.
      @SuppressWarnings("unchecked")
      LinkedHashMultimap<String, Integer>.ValueSet valueSet =
          (LinkedHashMultimap.ValueSet) multimap.backingMap().get("a");
      assertEquals(z, valueSet.size());
      assertFalse(Hashing.needsResizing(valueSet.size(), valueSet.hashTable.length, 
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
    assertEquals("{foo=[3, -1, 2, 4, 1], bar=[1, 2, 3]}",
        multimap.toString());
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

  private void assertOrderingReadOnly(Multimap<String, Integer> multimap) {
    ASSERT.that(multimap.get("foo")).has().exactly(5, 3).inOrder();
    ASSERT.that(multimap.get("bar")).has().exactly(4, 1).inOrder();
    ASSERT.that(multimap.get("cow")).has().item(2);

    ASSERT.that(multimap.keySet()).has().exactly("foo", "bar", "cow").inOrder();
    ASSERT.that(multimap.values()).has().exactly(5, 4, 3, 2, 1).inOrder();

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
    ASSERT.that(entry.getValue()).has().exactly(5, 3).inOrder();
    entry = collectionIterator.next();
    assertEquals("bar", entry.getKey());
    ASSERT.that(entry.getValue()).has().exactly(4, 1).inOrder();
    entry = collectionIterator.next();
    assertEquals("cow", entry.getKey());
    ASSERT.that(entry.getValue()).has().item(2);
  }

  public void testOrderingUpdates() {
    Multimap<String, Integer> multimap = initializeMultimap5();

    ASSERT.that(multimap.replaceValues("foo", asList(6, 7))).has().exactly(5, 3).inOrder();
    ASSERT.that(multimap.keySet()).has().exactly("foo", "bar", "cow").inOrder();
    ASSERT.that(multimap.removeAll("foo")).has().exactly(6, 7).inOrder();
    ASSERT.that(multimap.keySet()).has().exactly("bar", "cow").inOrder();
    assertTrue(multimap.remove("bar", 4));
    ASSERT.that(multimap.keySet()).has().exactly("bar", "cow").inOrder();
    assertTrue(multimap.remove("bar", 1));
    ASSERT.that(multimap.keySet()).has().item("cow");
    multimap.put("bar", 9);
    ASSERT.that(multimap.keySet()).has().exactly("cow", "bar").inOrder();
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
    LinkedHashMultimap<String, Integer> copy =
        LinkedHashMultimap.create(multimap);
    new EqualsTester()
        .addEqualityGroup(multimap, copy)
        .testEquals();
  }

  public void testCreateFromSizes() {
    LinkedHashMultimap<String, Integer> multimap
        = LinkedHashMultimap.create(20, 15);
    multimap.put("foo", 1);
    multimap.put("bar", 2);
    multimap.put("foo", 3);
    assertEquals(ImmutableSet.of(1, 3), multimap.get("foo"));
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
}

