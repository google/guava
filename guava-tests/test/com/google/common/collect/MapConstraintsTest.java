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
import static org.truth0.Truth.ASSERT;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.base.Supplier;
import com.google.common.testing.SerializableTester;

import junit.framework.TestCase;

import java.io.Serializable;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.RandomAccess;
import java.util.Set;
import java.util.SortedSet;

/**
 * Tests for {@code MapConstraints}.
 *
 * @author Mike Bostock
 * @author Jared Levy
 */
@GwtCompatible(emulated = true)
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

  public void testNotNull() {
    MapConstraint<Object, Object> constraint = MapConstraints.notNull();
    constraint.checkKeyValue("foo", 1);
    assertEquals("Not null", constraint.toString());
    try {
      constraint.checkKeyValue(null, 1);
      fail("NullPointerException expected");
    } catch (NullPointerException expected) {}
    try {
      constraint.checkKeyValue("foo", null);
      fail("NullPointerException expected");
    } catch (NullPointerException expected) {}
    try {
      constraint.checkKeyValue(null, null);
      fail("NullPointerException expected");
    } catch (NullPointerException expected) {}
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
    assertFalse(map.values() instanceof Serializable);
    assertEquals(map.toString(), constrained.toString());
    assertEquals(map.hashCode(), constrained.hashCode());
    ASSERT.that(map.entrySet()).has().exactly(
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

  public void testConstrainedBiMapLegal() {
    BiMap<String, Integer> map = new AbstractBiMap<String, Integer>(
        Maps.<String, Integer>newLinkedHashMap(),
        Maps.<Integer, String>newLinkedHashMap()) {};
    BiMap<String, Integer> constrained = MapConstraints.constrainedBiMap(
        map, TEST_CONSTRAINT);
    map.put(TEST_KEY, TEST_VALUE);
    constrained.put("foo", 1);
    map.putAll(ImmutableMap.of("bar", 2));
    constrained.putAll(ImmutableMap.of("baz", 3));
    assertTrue(map.equals(constrained));
    assertTrue(constrained.equals(map));
    assertEquals(map.entrySet(), constrained.entrySet());
    assertEquals(map.keySet(), constrained.keySet());
    assertEquals(map.values(), constrained.values());
    assertEquals(map.toString(), constrained.toString());
    assertEquals(map.hashCode(), constrained.hashCode());
    ASSERT.that(map.entrySet()).has().exactly(
        Maps.immutableEntry(TEST_KEY, TEST_VALUE),
        Maps.immutableEntry("foo", 1),
        Maps.immutableEntry("bar", 2),
        Maps.immutableEntry("baz", 3)).inOrder();
  }

  public void testConstrainedBiMapIllegal() {
    BiMap<String, Integer> map = new AbstractBiMap<String, Integer>(
        Maps.<String, Integer>newLinkedHashMap(),
        Maps.<Integer, String>newLinkedHashMap()) {};
    BiMap<String, Integer> constrained = MapConstraints.constrainedBiMap(
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
    try {
      constrained.forcePut(TEST_KEY, 3);
      fail("TestKeyException expected");
    } catch (TestKeyException expected) {}
    try {
      constrained.inverse().forcePut(TEST_VALUE, "baz");
      fail("TestValueException expected");
    } catch (TestValueException expected) {}
    try {
      constrained.inverse().forcePut(3, TEST_KEY);
      fail("TestKeyException expected");
    } catch (TestKeyException expected) {}
    assertEquals(Collections.emptySet(), map.entrySet());
    assertEquals(Collections.emptySet(), constrained.entrySet());
  }

  public void testConstrainedMultimapLegal() {
    Multimap<String, Integer> multimap = LinkedListMultimap.create();
    Multimap<String, Integer> constrained = MapConstraints.constrainedMultimap(
        multimap, TEST_CONSTRAINT);
    multimap.put(TEST_KEY, TEST_VALUE);
    constrained.put("foo", 1);
    multimap.get("bar").add(2);
    constrained.get("baz").add(3);
    multimap.get("qux").addAll(Arrays.asList(4));
    constrained.get("zig").addAll(Arrays.asList(5));
    multimap.putAll("zag", Arrays.asList(6));
    constrained.putAll("bee", Arrays.asList(7));
    multimap.putAll(new ImmutableMultimap.Builder<String, Integer>()
        .put("bim", 8).build());
    constrained.putAll(new ImmutableMultimap.Builder<String, Integer>()
        .put("bop", 9).build());
    multimap.putAll(new ImmutableMultimap.Builder<String, Integer>()
        .put("dig", 10).build());
    constrained.putAll(new ImmutableMultimap.Builder<String, Integer>()
        .put("dag", 11).build());
    assertTrue(multimap.equals(constrained));
    assertTrue(constrained.equals(multimap));
    ASSERT.that(ImmutableList.copyOf(multimap.entries()))
        .is(ImmutableList.copyOf(constrained.entries()));
    ASSERT.that(constrained.asMap().get("foo")).has().item(1);
    assertNull(constrained.asMap().get("missing"));
    assertEquals(multimap.asMap(), constrained.asMap());
    assertEquals(multimap.values(), constrained.values());
    assertEquals(multimap.keys(), constrained.keys());
    assertEquals(multimap.keySet(), constrained.keySet());
    assertEquals(multimap.toString(), constrained.toString());
    assertEquals(multimap.hashCode(), constrained.hashCode());
    ASSERT.that(multimap.entries()).has().exactly(
        Maps.immutableEntry(TEST_KEY, TEST_VALUE),
        Maps.immutableEntry("foo", 1),
        Maps.immutableEntry("bar", 2),
        Maps.immutableEntry("baz", 3),
        Maps.immutableEntry("qux", 4),
        Maps.immutableEntry("zig", 5),
        Maps.immutableEntry("zag", 6),
        Maps.immutableEntry("bee", 7),
        Maps.immutableEntry("bim", 8),
        Maps.immutableEntry("bop", 9),
        Maps.immutableEntry("dig", 10),
        Maps.immutableEntry("dag", 11)).inOrder();
    assertFalse(constrained.asMap().values() instanceof Serializable);
    Iterator<Collection<Integer>> iterator =
        constrained.asMap().values().iterator();
    iterator.next();
    iterator.next().add(12);
    assertTrue(multimap.containsEntry("foo", 12));
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

  public void testConstrainedTypePreservingSet() {
    SetMultimap<String, Integer> multimap
        = MapConstraints.constrainedSetMultimap(
            LinkedHashMultimap.<String, Integer>create(),
            TEST_CONSTRAINT);
    multimap.put("foo", 1);
    Map.Entry<String, Collection<Integer>> entry
        = multimap.asMap().entrySet().iterator().next();
    assertTrue(entry.getValue() instanceof Set);
  }

  public void testConstrainedTypePreservingSortedSet() {
    Comparator<Integer> comparator = Collections.reverseOrder();
    SortedSetMultimap<String, Integer> delegate
        = TreeMultimap.create(Ordering.<String>natural(), comparator);
    SortedSetMultimap<String, Integer> multimap
        = MapConstraints.constrainedSortedSetMultimap(delegate,
            TEST_CONSTRAINT);
    multimap.put("foo", 1);
    Map.Entry<String, Collection<Integer>> entry
        = multimap.asMap().entrySet().iterator().next();
    assertTrue(entry.getValue() instanceof SortedSet);
    assertSame(comparator, multimap.valueComparator());
    assertSame(comparator, multimap.get("foo").comparator());
  }

  @SuppressWarnings("unchecked")
  public void testConstrainedMultimapIllegal() {
    Multimap<String, Integer> multimap = LinkedListMultimap.create();
    Multimap<String, Integer> constrained = MapConstraints.constrainedMultimap(
        multimap, TEST_CONSTRAINT);
    try {
      constrained.put(TEST_KEY, 1);
      fail("TestKeyException expected");
    } catch (TestKeyException expected) {}
    try {
      constrained.put("foo", TEST_VALUE);
      fail("TestValueException expected");
    } catch (TestValueException expected) {}
    try {
      constrained.put(TEST_KEY, TEST_VALUE);
      fail("TestKeyException expected");
    } catch (TestKeyException expected) {}
    try {
      constrained.get(TEST_KEY).add(1);
      fail("TestKeyException expected");
    } catch (TestKeyException expected) {}
    try {
      constrained.get("foo").add(TEST_VALUE);
      fail("TestValueException expected");
    } catch (TestValueException expected) {}
    try {
      constrained.get(TEST_KEY).add(TEST_VALUE);
      fail("TestKeyException expected");
    } catch (TestKeyException expected) {}
    try {
      constrained.get(TEST_KEY).addAll(Arrays.asList(1));
      fail("TestKeyException expected");
    } catch (TestKeyException expected) {}
    try {
      constrained.get("foo").addAll(Arrays.asList(1, TEST_VALUE));
      fail("TestValueException expected");
    } catch (TestValueException expected) {}
    try {
      constrained.get(TEST_KEY).addAll(Arrays.asList(1, TEST_VALUE));
      fail("TestKeyException expected");
    } catch (TestKeyException expected) {}
    try {
      constrained.putAll(TEST_KEY, Arrays.asList(1));
      fail("TestKeyException expected");
    } catch (TestKeyException expected) {}
    try {
      constrained.putAll("foo", Arrays.asList(1, TEST_VALUE));
      fail("TestValueException expected");
    } catch (TestValueException expected) {}
    try {
      constrained.putAll(TEST_KEY, Arrays.asList(1, TEST_VALUE));
      fail("TestKeyException expected");
    } catch (TestKeyException expected) {}
    try {
      constrained.putAll(new ImmutableMultimap.Builder<String, Integer>()
          .put(TEST_KEY, 2).put("foo", 1).build());
      fail("TestKeyException expected");
    } catch (TestKeyException expected) {}
    try {
      constrained.putAll(new ImmutableMultimap.Builder<String, Integer>()
          .put("bar", TEST_VALUE).put("foo", 1).build());
      fail("TestValueException expected");
    } catch (TestValueException expected) {}
    try {
      constrained.putAll(new ImmutableMultimap.Builder<String, Integer>()
          .put(TEST_KEY, TEST_VALUE).put("foo", 1).build());
      fail("TestKeyException expected");
    } catch (TestKeyException expected) {}
    try {
      constrained.entries().add(Maps.immutableEntry(TEST_KEY, 1));
      fail("UnsupportedOperationException expected");
    } catch (UnsupportedOperationException expected) {}
    try {
      constrained.entries().addAll(Arrays.asList(
          Maps.immutableEntry("foo", 1),
          Maps.immutableEntry(TEST_KEY, 2)));
      fail("UnsupportedOperationException expected");
    } catch (UnsupportedOperationException expected) {}
    assertTrue(multimap.isEmpty());
    assertTrue(constrained.isEmpty());
    constrained.put("foo", 1);
    try {
      constrained.asMap().get("foo").add(TEST_VALUE);
      fail("TestValueException expected");
    } catch (TestValueException expected) {}
    try {
      constrained.asMap().values().iterator().next().add(TEST_VALUE);
      fail("TestValueException expected");
    } catch (TestValueException expected) {}
    try {
      ((Collection<Integer>) constrained.asMap().values().toArray()[0])
          .add(TEST_VALUE);
      fail("TestValueException expected");
    } catch (TestValueException expected) {}
    ASSERT.that(ImmutableList.copyOf(multimap.entries()))
        .is(ImmutableList.copyOf(constrained.entries()));
    assertEquals(multimap.asMap(), constrained.asMap());
    assertEquals(multimap.values(), constrained.values());
    assertEquals(multimap.keys(), constrained.keys());
    assertEquals(multimap.keySet(), constrained.keySet());
    assertEquals(multimap.toString(), constrained.toString());
    assertEquals(multimap.hashCode(), constrained.hashCode());
  }

  private static class QueueSupplier implements Supplier<Queue<Integer>> {
    @Override
    public Queue<Integer> get() {
      return new LinkedList<Integer>();
    }
  }

  public void testConstrainedMultimapQueue() {
    Multimap<String, Integer> multimap = Multimaps.newMultimap(
        new HashMap<String, Collection<Integer>>(), new QueueSupplier());
    Multimap<String, Integer> constrained = MapConstraints.constrainedMultimap(
        multimap, TEST_CONSTRAINT);
    constrained.put("foo", 1);
    assertTrue(constrained.get("foo").contains(1));
    assertTrue(multimap.get("foo").contains(1));
    try {
      constrained.put(TEST_KEY, 1);
      fail("TestKeyException expected");
    } catch (TestKeyException expected) {}
    try {
      constrained.put("foo", TEST_VALUE);
      fail("TestValueException expected");
    } catch (TestValueException expected) {}
    try {
      constrained.get("foo").add(TEST_VALUE);
      fail("TestKeyException expected");
    } catch (TestValueException expected) {}
    try {
      constrained.get(TEST_KEY).add(1);
      fail("TestValueException expected");
    } catch (TestKeyException expected) {}
    assertEquals(1, constrained.size());
    assertEquals(1, multimap.size());
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

  public void testMultimapAsMapEntriesToArray() {
    Multimap<String, Integer> multimap = LinkedListMultimap.create();
    Multimap<String, Integer> constrained
        = MapConstraints.constrainedMultimap(multimap, TEST_CONSTRAINT);
    multimap.put("foo", 1);
    @SuppressWarnings("unchecked")
    Map.Entry<String, Collection<Integer>> entry
        = (Map.Entry<String, Collection<Integer>>)
            constrained.asMap().entrySet().toArray()[0];
    try {
      entry.setValue(Collections.<Integer>emptySet());
      fail("UnsupportedOperationException expected");
    } catch (UnsupportedOperationException expected) {}
    try {
      entry.getValue().add(TEST_VALUE);
      fail("TestValueException expected");
    } catch (TestValueException expected) {}
    assertFalse(multimap.containsValue(TEST_VALUE));
  }

  public void testMultimapAsMapValuesToArray() {
    Multimap<String, Integer> multimap = LinkedListMultimap.create();
    Multimap<String, Integer> constrained
        = MapConstraints.constrainedMultimap(multimap, TEST_CONSTRAINT);
    multimap.put("foo", 1);
    @SuppressWarnings("unchecked")
    Collection<Integer> collection
        = (Collection<Integer>) constrained.asMap().values().toArray()[0];
    try {
      collection.add(TEST_VALUE);
      fail("TestValueException expected");
    } catch (TestValueException expected) {}
    assertFalse(multimap.containsValue(TEST_VALUE));
  }

  public void testMultimapEntriesContainsNefariousEntry() {
    Multimap<String, Integer> multimap = LinkedListMultimap.create();
    Multimap<String, Integer> constrained
        = MapConstraints.constrainedMultimap(multimap, TEST_CONSTRAINT);
    multimap.put("foo", 1);
    Map.Entry<String, Integer> nefariousEntry
        = nefariousMapEntry(TEST_KEY, TEST_VALUE);
    Collection<Map.Entry<String, Integer>> entries = constrained.entries();
    assertFalse(entries.contains(nefariousEntry));
    assertFalse(multimap.containsValue(TEST_VALUE));
    assertFalse(entries.containsAll(Collections.singleton(nefariousEntry)));
    assertFalse(multimap.containsValue(TEST_VALUE));
  }

  public void testMultimapEntriesRemoveNefariousEntry() {
    Multimap<String, Integer> multimap = LinkedListMultimap.create();
    Multimap<String, Integer> constrained
        = MapConstraints.constrainedMultimap(multimap, TEST_CONSTRAINT);
    multimap.put("foo", 1);
    Map.Entry<String, Integer> nefariousEntry
        = nefariousMapEntry(TEST_KEY, TEST_VALUE);
    Collection<Map.Entry<String, Integer>> entries = constrained.entries();
    assertFalse(entries.remove(nefariousEntry));
    assertFalse(multimap.containsValue(TEST_VALUE));
    assertFalse(entries.removeAll(Collections.singleton(nefariousEntry)));
    assertFalse(multimap.containsValue(TEST_VALUE));
  }

  public void testMultimapAsMapEntriesContainsNefariousEntry() {
    Multimap<String, Integer> multimap = LinkedListMultimap.create();
    Multimap<String, Integer> constrained
        = MapConstraints.constrainedMultimap(multimap, TEST_CONSTRAINT);
    multimap.put("foo", 1);
    Map.Entry<String, ? extends Collection<Integer>> nefariousEntry
        = nefariousMapEntry(TEST_KEY, Collections.singleton(TEST_VALUE));
    Set<Map.Entry<String, Collection<Integer>>> entries
        = constrained.asMap().entrySet();
    assertFalse(entries.contains(nefariousEntry));
    assertFalse(multimap.containsValue(TEST_VALUE));
    assertFalse(entries.containsAll(Collections.singleton(nefariousEntry)));
    assertFalse(multimap.containsValue(TEST_VALUE));
  }

  public void testMultimapAsMapEntriesRemoveNefariousEntry() {
    Multimap<String, Integer> multimap = LinkedListMultimap.create();
    Multimap<String, Integer> constrained
        = MapConstraints.constrainedMultimap(multimap, TEST_CONSTRAINT);
    multimap.put("foo", 1);
    Map.Entry<String, ? extends Collection<Integer>> nefariousEntry
        = nefariousMapEntry(TEST_KEY, Collections.singleton(TEST_VALUE));
    Set<Map.Entry<String, Collection<Integer>>> entries
        = constrained.asMap().entrySet();
    assertFalse(entries.remove(nefariousEntry));
    assertFalse(multimap.containsValue(TEST_VALUE));
    assertFalse(entries.removeAll(Collections.singleton(nefariousEntry)));
    assertFalse(multimap.containsValue(TEST_VALUE));
  }

  public void testNefariousMapPutAll() {
    Map<String, Integer> map = Maps.newLinkedHashMap();
    Map<String, Integer> constrained = MapConstraints.constrainedMap(
        map, TEST_CONSTRAINT);
    Map<String, Integer> onceIterable = onceIterableMap("foo", 1);
    constrained.putAll(onceIterable);
    assertEquals((Integer) 1, constrained.get("foo"));
  }

  public void testNefariousMultimapPutAllIterable() {
    Multimap<String, Integer> multimap = LinkedListMultimap.create();
    Multimap<String, Integer> constrained = MapConstraints.constrainedMultimap(
        multimap, TEST_CONSTRAINT);
    Collection<Integer> onceIterable
        = ConstraintsTest.onceIterableCollection(1);
    constrained.putAll("foo", onceIterable);
    assertEquals(ImmutableList.of(1), constrained.get("foo"));
  }

  public void testNefariousMultimapPutAllMultimap() {
    Multimap<String, Integer> multimap = LinkedListMultimap.create();
    Multimap<String, Integer> constrained = MapConstraints.constrainedMultimap(
        multimap, TEST_CONSTRAINT);
    Multimap<String, Integer> onceIterable
        = Multimaps.forMap(onceIterableMap("foo", 1));
    constrained.putAll(onceIterable);
    assertEquals(ImmutableList.of(1), constrained.get("foo"));
  }

  public void testNefariousMultimapGetAddAll() {
    Multimap<String, Integer> multimap = LinkedListMultimap.create();
    Multimap<String, Integer> constrained = MapConstraints.constrainedMultimap(
        multimap, TEST_CONSTRAINT);
    Collection<Integer> onceIterable
        = ConstraintsTest.onceIterableCollection(1);
    constrained.get("foo").addAll(onceIterable);
    assertEquals(ImmutableList.of(1), constrained.get("foo"));
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

  @GwtIncompatible("SerializableTester")
  public void testSerialization() {
    // TODO: Test serialization of constrained collections.
    assertSame(MapConstraints.notNull(),
        SerializableTester.reserialize(MapConstraints.notNull()));
  }
}
