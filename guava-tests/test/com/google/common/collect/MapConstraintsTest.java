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

import static com.google.common.collect.testing.Helpers.nefariousMapEntry;
import static com.google.common.truth.Truth.assertThat;

import com.google.common.annotations.GwtCompatible;
import com.google.common.base.Supplier;
import java.io.Serializable;
import java.util.AbstractMap;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.RandomAccess;
import java.util.Set;
import junit.framework.TestCase;

/**
 * Tests for {@code MapConstraints}.
 *
 * @author Mike Bostock
 * @author Jared Levy
 */
@GwtCompatible
public class MapConstraintsTest extends TestCase {

  private static final String TEST_KEY = "test";

  private static final Integer TEST_VALUE = 42;

  static final class TestKeyException extends IllegalArgumentException {
    private static final long serialVersionUID = 0;
  }

  static final class TestValueException extends IllegalArgumentException {
    private static final long serialVersionUID = 0;
  }

  static final MapConstraint<String, Integer> TEST_CONSTRAINT
      = new TestConstraint();

  private static final class TestConstraint
      implements MapConstraint<String, Integer>, Serializable {
    @Override
    public void checkKeyValue(String key, Integer value) {
      if (TEST_KEY.equals(key)) {
        throw new TestKeyException();
      }
      if (TEST_VALUE.equals(value)) {
        throw new TestValueException();
      }
    }
    private static final long serialVersionUID = 0;
  }

  public void testConstrainedMapLegal() {
    Map<String, Integer> map = Maps.newLinkedHashMap();
    Map<String, Integer> constrained = MapConstraints.constrainedMap(
        map, TEST_CONSTRAINT);
    map.put(TEST_KEY, TEST_VALUE);
    constrained.put("foo", 1);
    map.putAll(ImmutableMap.of("bar", 2));
    constrained.putAll(ImmutableMap.of("baz", 3));
    assertTrue(map.equals(constrained));
    assertTrue(constrained.equals(map));
    assertEquals(map.entrySet(), constrained.entrySet());
    assertEquals(map.keySet(), constrained.keySet());
    assertEquals(HashMultiset.create(map.values()),
        HashMultiset.create(constrained.values()));
    assertThat(map.values()).isNotInstanceOf(Serializable.class);
    assertEquals(map.toString(), constrained.toString());
    assertEquals(map.hashCode(), constrained.hashCode());
    assertThat(map.entrySet()).containsExactly(
        Maps.immutableEntry(TEST_KEY, TEST_VALUE),
        Maps.immutableEntry("foo", 1),
        Maps.immutableEntry("bar", 2),
        Maps.immutableEntry("baz", 3)).inOrder();
  }

  public void testConstrainedMapIllegal() {
    Map<String, Integer> map = Maps.newLinkedHashMap();
    Map<String, Integer> constrained = MapConstraints.constrainedMap(
        map, TEST_CONSTRAINT);
    try {
      constrained.put(TEST_KEY, TEST_VALUE);
      fail("TestKeyException expected");
    } catch (TestKeyException expected) {}
    try {
      constrained.put("baz", TEST_VALUE);
      fail("TestValueException expected");
    } catch (TestValueException expected) {}
    try {
      constrained.put(TEST_KEY, 3);
      fail("TestKeyException expected");
    } catch (TestKeyException expected) {}
    try {
      constrained.putAll(ImmutableMap.of("baz", 3, TEST_KEY, 4));
      fail("TestKeyException expected");
    } catch (TestKeyException expected) {}
    assertEquals(Collections.emptySet(), map.entrySet());
    assertEquals(Collections.emptySet(), constrained.entrySet());
  }

  public void testConstrainedTypePreservingList() {
    ListMultimap<String, Integer> multimap
        = MapConstraints.constrainedListMultimap(
            LinkedListMultimap.<String, Integer>create(),
            TEST_CONSTRAINT);
    multimap.put("foo", 1);
    Map.Entry<String, Collection<Integer>> entry
        = multimap.asMap().entrySet().iterator().next();
    assertTrue(entry.getValue() instanceof List);
    assertFalse(multimap.entries() instanceof Set);
    assertFalse(multimap.get("foo") instanceof RandomAccess);
  }

  public void testConstrainedTypePreservingRandomAccessList() {
    ListMultimap<String, Integer> multimap
        = MapConstraints.constrainedListMultimap(
            ArrayListMultimap.<String, Integer>create(),
            TEST_CONSTRAINT);
    multimap.put("foo", 1);
    Map.Entry<String, Collection<Integer>> entry
        = multimap.asMap().entrySet().iterator().next();
    assertTrue(entry.getValue() instanceof List);
    assertFalse(multimap.entries() instanceof Set);
    assertTrue(multimap.get("foo") instanceof RandomAccess);
  }

  private static class QueueSupplier implements Supplier<Queue<Integer>> {
    @Override
    public Queue<Integer> get() {
      return new LinkedList<Integer>();
    }
  }

  public void testMapEntrySetToArray() {
    Map<String, Integer> map = Maps.newLinkedHashMap();
    Map<String, Integer> constrained
        = MapConstraints.constrainedMap(map, TEST_CONSTRAINT);
    map.put("foo", 1);
    @SuppressWarnings("unchecked")
    Map.Entry<String, Integer> entry
        = (Map.Entry) constrained.entrySet().toArray()[0];
    try {
      entry.setValue(TEST_VALUE);
      fail("TestValueException expected");
    } catch (TestValueException expected) {}
    assertFalse(map.containsValue(TEST_VALUE));
  }

  public void testMapEntrySetContainsNefariousEntry() {
    Map<String, Integer> map = Maps.newTreeMap();
    Map<String, Integer> constrained
        = MapConstraints.constrainedMap(map, TEST_CONSTRAINT);
    map.put("foo", 1);
    Map.Entry<String, Integer> nefariousEntry
        = nefariousMapEntry(TEST_KEY, TEST_VALUE);
    Set<Map.Entry<String, Integer>> entries = constrained.entrySet();
    assertFalse(entries.contains(nefariousEntry));
    assertFalse(map.containsValue(TEST_VALUE));
    assertFalse(entries.containsAll(Collections.singleton(nefariousEntry)));
    assertFalse(map.containsValue(TEST_VALUE));
  }

  public void testNefariousMapPutAll() {
    Map<String, Integer> map = Maps.newLinkedHashMap();
    Map<String, Integer> constrained = MapConstraints.constrainedMap(
        map, TEST_CONSTRAINT);
    Map<String, Integer> onceIterable = onceIterableMap("foo", 1);
    constrained.putAll(onceIterable);
    assertEquals((Integer) 1, constrained.get("foo"));
  }

  /**
   * Returns a "nefarious" map, which permits only one call to its views'
   * iterator() methods. This verifies that the constrained map uses a
   * defensive copy instead of potentially checking the elements in one snapshot
   * and adding the elements from another.
   *
   * @param key the key to be contained in the map
   * @param value the value to be contained in the map
   */
  static <K, V> Map<K, V> onceIterableMap(K key, V value) {
    final Map.Entry<K, V> entry = Maps.immutableEntry(key, value);
    return new AbstractMap<K, V>() {
      boolean iteratorCalled;
      @Override public int size() {
        /*
         * We could make the map empty, but that seems more likely to trigger
         * special cases (so maybe we should test both empty and nonempty...).
         */
        return 1;
      }
      @Override public Set<Entry<K, V>> entrySet() {
        return new ForwardingSet<Entry<K, V>>() {
          @Override protected Set<Entry<K, V>> delegate() {
            return Collections.singleton(entry);
          }
          @Override public Iterator<Entry<K, V>> iterator() {
            assertFalse("Expected only one call to iterator()", iteratorCalled);
            iteratorCalled = true;
            return super.iterator();
          }
        };
      }
      @Override public Set<K> keySet() {
        throw new UnsupportedOperationException();
      }
      @Override public Collection<V> values() {
        throw new UnsupportedOperationException();
      }
    };
  }
}
