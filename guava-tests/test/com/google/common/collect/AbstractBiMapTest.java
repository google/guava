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

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.testing.SerializableTester;

import junit.framework.TestCase;

import java.io.Serializable;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * Common tests for any {@code BiMap}.
 *
 * @author Kevin Bourrillion
 */
@GwtCompatible(emulated = true)
public abstract class AbstractBiMapTest extends TestCase {

  protected abstract BiMap<Integer, String> create();

  protected BiMap<Integer, String> bimap;
  protected Set<Entry<Integer, String>> entrySet;

  // public for GWT
  @Override public void setUp() throws Exception {
    super.setUp();
    bimap = create();
    entrySet = bimap.entrySet();
  }

  public void testClear() {
    bimap.clear();
    assertTrue(bimap.isEmpty());
    putOneTwoThree();
    bimap.clear();
    assertTrue(bimap.isEmpty());
  }

  public void testContainsKey() {
    assertFalse(bimap.containsKey(null));
    assertFalse(bimap.containsKey(1));
    assertFalse(bimap.containsKey("one"));

    bimap.put(1, "one");
    assertTrue(bimap.containsKey(1));

    bimap.put(null, null);
    assertTrue(bimap.containsKey(null));
  }

  public void testContainsValue() {
    assertFalse(bimap.containsValue(null));
    assertFalse(bimap.containsValue(1));
    assertFalse(bimap.containsValue("one"));

    bimap.put(1, "one");
    assertTrue(bimap.containsValue("one"));

    bimap.put(null, null);
    assertTrue(bimap.containsValue(null));
  }

  public void testEquals() {
    BiMap<Integer, String> biMap = create();
    assertEquals(biMap, biMap);
    assertEquals(create(), biMap);
    biMap.put(1, null);
    assertFalse(create().equals(biMap));
  }

  public void testGet() {
    assertNull(bimap.get(1));
    assertNull(bimap.get(null));
    assertNull(bimap.get("bad"));

    bimap.put(1, "one");
    bimap.put(0, null);
    bimap.put(null, "nothing");
    assertEquals("one", bimap.get(1));
    assertNull(bimap.get(0));
    assertEquals("nothing", bimap.get(null));
    assertNull(bimap.get("bad"));

    bimap.forcePut(null, null);
    assertNull(bimap.get(null));
    bimap.remove(null);
    assertNull(bimap.get(null));
  }

  public void testInverseSimple() {
    BiMap<String, Integer> inverse = bimap.inverse();
    bimap.put(1, "one");
    bimap.put(2, "two");
    assertEquals(ImmutableMap.of("one", 1, "two", 2), inverse);
    // see InverseBiMapTest

    assertSame(bimap, inverse.inverse());
  }

  public void testInversePut() {
    BiMap<String, Integer> inverse = bimap.inverse();
    bimap.put(1, "one");
    bimap.inverse().put("two", 2);
    assertEquals(ImmutableMap.of("one", 1, "two", 2), inverse);
    assertEquals(ImmutableMap.of(1, "one", 2, "two"), bimap);
  }

  public void testIsEmpty() {
    assertTrue(bimap.isEmpty());
    bimap.put(1, "one");
    assertFalse(bimap.isEmpty());
    bimap.remove(1);
    assertTrue(bimap.isEmpty());
  }

  public void testPut() {
    bimap.put(1, "one");
    assertEquals(ImmutableMap.of(1, "one"), bimap);

    bimap.put(2, "two");
    assertEquals(ImmutableMap.of(1, "one", 2, "two"), bimap);

    bimap.put(2, "two");
    assertEquals(ImmutableMap.of(1, "one", 2, "two"), bimap);

    bimap.put(1, "ONE");
    assertEquals(ImmutableMap.of(1, "ONE", 2, "two"), bimap);

    try {
      bimap.put(3, "two");
      fail();
    } catch (IllegalArgumentException e) {
    }
    assertEquals(ImmutableMap.of(1, "ONE", 2, "two"), bimap);

    bimap.put(-1, null);
    bimap.put(null, "null");
    Map<Integer, String> expected = Maps.newHashMap();
    expected.put(1, "ONE");
    expected.put(2, "two");
    expected.put(-1, null);
    expected.put(null, "null");

    assertEquals(expected, bimap);

    bimap.remove(-1);
    bimap.put(null, null);

    expected.remove(-1);
    expected.put(null, null);

    assertEquals(expected, bimap);
  }

  public void testPutNull() {
    bimap.put(-1, null);
    assertTrue(bimap.containsValue(null));
    bimap.put(1, "one");
    assertTrue(bimap.containsValue(null));
  }

  public void testPutAll() {
    bimap.put(1, "one");
    Map<Integer, String> newEntries = ImmutableMap.of(2, "two", 3, "three");
    bimap.putAll(newEntries);
    assertEquals(ImmutableMap.of(1, "one", 2, "two", 3, "three"), bimap);
  }

  public void testForcePut() {
    assertNull(bimap.forcePut(1, "one"));
    assertEquals(ImmutableMap.of(1, "one"), bimap);
    assertEquals("one", bimap.forcePut(1, "one"));
    assertEquals(ImmutableMap.of(1, "one"), bimap);
    assertEquals("one", bimap.forcePut(1, "ONE"));
    assertEquals(ImmutableMap.of(1, "ONE"), bimap);
    assertNull(bimap.forcePut(-1, "ONE")); // key 1 disappears without a trace
    assertEquals(ImmutableMap.of(-1, "ONE"), bimap);
    assertNull(bimap.forcePut(2, "two"));
    assertEquals(ImmutableMap.of(-1, "ONE", 2, "two"), bimap);
    assertEquals("two", bimap.forcePut(2, "ONE"));
    assertEquals(ImmutableMap.of(2, "ONE"), bimap);
  }

  public void testRemove() {
    Map<Integer, String> map = Maps.newHashMap();
    map.put(0, null);
    map.put(1, "one");
    map.put(null, "null");

    bimap.putAll(map);
    assertNull(bimap.remove(0));

    map.remove(0);
    assertEquals(map, bimap);

    assertEquals("null", bimap.remove(null));
    assertEquals(Collections.singletonMap(1, "one"), bimap);

    assertNull(bimap.remove(15));

    assertEquals("one", bimap.remove(1));
    assertTrue(bimap.isEmpty());
  }

  public void testSize() {
    assertEquals(0, bimap.size());
    bimap.put(1, "one");
    assertEquals(1, bimap.size());
    bimap.put(1, "ONE");
    assertEquals(1, bimap.size());
    bimap.put(2, "two");
    assertEquals(2, bimap.size());
    bimap.forcePut(1, "two");
    assertEquals(1, bimap.size());
  }

  public void testToString() {
    bimap.put(1, "one");
    bimap.put(2, "two");

    String string = bimap.toString();
    String expected = string.startsWith("{1")
        ? "{1=one, 2=two}"
        : "{2=two, 1=one}";
    assertEquals(expected, bimap.toString());
  }

  // Entry Set

  public void testEntrySetAdd() {
    try {
      entrySet.add(Maps.immutableEntry(1, "one"));
      fail();
    } catch (UnsupportedOperationException expected) {
    }
  }

  public void testEntrySetAddAll() {
    try {
      entrySet.addAll(Collections.singleton(Maps.immutableEntry(1, "one")));
      fail();
    } catch (UnsupportedOperationException expected) {
    }
  }

  public void testEntrySetClear() {
    entrySet.clear();
    assertTrue(entrySet.isEmpty());
    assertTrue(bimap.isEmpty());
    putOneTwoThree();
    entrySet.clear();
    assertTrue(entrySet.isEmpty());
    assertTrue(bimap.isEmpty());
  }

  public void testEntrySetContains() {
    assertFalse(entrySet.contains(Maps.immutableEntry(1, "one")));
    bimap.put(1, "one");
    assertTrue(entrySet.contains(Maps.immutableEntry(1, "one")));
    assertFalse(entrySet.contains(Maps.immutableEntry(1, "")));
    assertFalse(entrySet.contains(Maps.immutableEntry(0, "one")));
    assertFalse(entrySet.contains(Maps.immutableEntry(1, null)));
    assertFalse(entrySet.contains(Maps.immutableEntry(null, "one")));
    assertFalse(entrySet.contains(Maps.immutableEntry(null, null)));

    bimap.put(null, null);
    assertTrue(entrySet.contains(Maps.immutableEntry(1, "one")));
    assertTrue(entrySet.contains(Maps.immutableEntry(null, null)));
    assertFalse(entrySet.contains(Maps.immutableEntry(1, "")));
    assertFalse(entrySet.contains(Maps.immutableEntry(0, "one")));
    assertFalse(entrySet.contains(Maps.immutableEntry(1, null)));
    assertFalse(entrySet.contains(Maps.immutableEntry(null, "one")));

    bimap.put(null, "null");
    bimap.put(0, null);
    assertTrue(entrySet.contains(Maps.immutableEntry(1, "one")));
    assertTrue(entrySet.contains(Maps.immutableEntry(null, "null")));
    assertTrue(entrySet.contains(Maps.immutableEntry(0, null)));
    assertFalse(entrySet.contains(Maps.immutableEntry(1, "")));
    assertFalse(entrySet.contains(Maps.immutableEntry(0, "one")));
    assertFalse(entrySet.contains(Maps.immutableEntry(1, null)));
    assertFalse(entrySet.contains(Maps.immutableEntry(null, "one")));
    assertFalse(entrySet.contains(Maps.immutableEntry(null, null)));
  }

  public void testEntrySetIsEmpty() {
    assertTrue(entrySet.isEmpty());
    bimap.put(1, "one");
    assertFalse(entrySet.isEmpty());
    bimap.remove(1);
    assertTrue(entrySet.isEmpty());
  }

  public void testEntrySetRemove() {
    putOneTwoThree();
    assertTrue(bimap.containsKey(1));
    assertTrue(bimap.containsValue("one"));
    assertTrue(entrySet.remove(Maps.immutableEntry(1, "one")));
    assertFalse(bimap.containsKey(1));
    assertFalse(bimap.containsValue("one"));
    assertEquals(2, bimap.size());
    assertEquals(2, bimap.inverse().size());
    assertFalse(entrySet.remove(Maps.immutableEntry(2, "three")));
    assertFalse(entrySet.remove(3));
    assertEquals(2, bimap.size());
    assertEquals(2, bimap.inverse().size());
  }

  public void testEntrySetRemoveAll() {
    putOneTwoThree();
    assertTrue(bimap.containsKey(1));
    assertTrue(bimap.containsValue("one"));
    assertTrue(entrySet.removeAll(
        Collections.singleton(Maps.immutableEntry(1, "one"))));
    assertFalse(bimap.containsKey(1));
    assertFalse(bimap.containsValue("one"));
    assertEquals(2, bimap.size());
    assertEquals(2, bimap.inverse().size());
  }

  public void testEntrySetValue() {
    bimap.put(1, "one");
    Entry<Integer, String> entry = bimap.entrySet().iterator().next();
    bimap.put(2, "two");
    assertEquals("one", entry.getValue());
    bimap.put(1, "one");
    assertEquals("one", entry.getValue());
    assertEquals("one", bimap.get(1));
    assertEquals(Integer.valueOf(1), bimap.inverse().get("one"));
    bimap.put(1, "uno");
    assertEquals("uno", entry.getValue());
    assertEquals("uno", bimap.get(1));
    assertEquals(Integer.valueOf(1), bimap.inverse().get("uno"));
    assertEquals(2, bimap.size());
    assertEquals(2, bimap.inverse().size());
    try {
      entry.setValue("two");
      fail();
    } catch (IllegalArgumentException expected) {}
    assertEquals("uno", entry.getValue());
    assertEquals("uno", bimap.get(1));
    assertEquals(Integer.valueOf(1), bimap.inverse().get("uno"));
    assertEquals(2, bimap.size());
    assertEquals(2, bimap.inverse().size());
  }

  public void testEntrySetValueRemovedEntry() {
    bimap.put(1, "a");
    Entry<Integer, String> entry = bimap.entrySet().iterator().next();
    bimap.clear();
    try {
      entry.setValue("b");
      fail();
    } catch (IllegalStateException expected) {}
    assertEquals(0, bimap.size());
    assertEquals(0, bimap.inverse().size());
  }

  public void testEntrySetValueRemovedEntryNullOldValue() {
    bimap.put(1, null);
    Entry<Integer, String> entry = bimap.entrySet().iterator().next();
    bimap.clear();
    try {
      entry.setValue("b");
      fail();
    } catch (IllegalStateException expected) {}
    assertEquals(0, bimap.size());
    assertEquals(0, bimap.inverse().size());
  }

  public void testEntrySetValueRemovedEntryAddedEqualEntry() {
    bimap.put(1, "a");
    Entry<Integer, String> entry = bimap.entrySet().iterator().next();
    bimap.clear();
    bimap.put(1, "a");
    try {
      entry.setValue("b");
      fail();
    } catch (IllegalStateException expected) {}
    assertEquals(1, bimap.size());
    assertEquals("a", bimap.get(1));
    assertEquals(1, bimap.inverse().size());
    assertEquals((Integer) 1, bimap.inverse().get("a"));
  }

  public void testKeySetIteratorRemove() {
    putOneTwoThree();
    Iterator<Integer> iterator = bimap.keySet().iterator();
    iterator.next();
    iterator.remove();
    assertEquals(2, bimap.size());
    assertEquals(2, bimap.inverse().size());
  }

  public void testKeySetRemoveAll() {
    putOneTwoThree();
    Set<Integer> keySet = bimap.keySet();
    assertTrue(keySet.removeAll(asList(1, 3)));
    assertEquals(1, bimap.size());
    assertTrue(keySet.contains(2));
  }

  public void testKeySetRetainAll() {
    putOneTwoThree();
    Set<Integer> keySet = bimap.keySet();
    assertTrue(keySet.retainAll(Collections.singleton(2)));
    assertEquals(1, bimap.size());
    assertTrue(keySet.contains(2));
  }

  public void testEntriesIteratorRemove() {
    putOneTwoThree();
    Iterator<Entry<Integer, String>> iterator = bimap.entrySet().iterator();
    iterator.next();
    iterator.remove();
    assertEquals(2, bimap.size());
    assertEquals(2, bimap.inverse().size());
  }

  public void testEntriesRetainAll() {
    putOneTwoThree();
    Set<Map.Entry<Integer, String>> entries = bimap.entrySet();
    Map.Entry<Integer, String> entry = Maps.immutableEntry(2, "two");
    assertTrue(entries.retainAll(Collections.singleton(entry)));
    assertEquals(1, bimap.size());
    assertTrue(bimap.containsKey(2));
  }

  public void testValuesIteratorRemove() {
    putOneTwoThree();
    Iterator<String> iterator = bimap.values().iterator();
    iterator.next();
    iterator.remove();
    assertEquals(2, bimap.size());
    assertEquals(2, bimap.inverse().size());
  }

  public void testValuesToArray() {
    bimap.put(1, "one");
    String[] array = new String[3];
    array[1] = "garbage";
    assertSame(array, bimap.values().toArray(array));
    assertEquals("one", array[0]);
    assertNull(array[1]);
  }

  public void testValuesToString() {
    bimap.put(1, "one");
    assertEquals("[one]", bimap.values().toString());
  }

  @GwtIncompatible("SerializableTester")
  public void testSerialization() {
    bimap.put(1, "one");
    bimap.put(2, "two");
    bimap.put(3, "three");
    bimap.put(null, null);

    BiMap<Integer, String> copy =
        SerializableTester.reserializeAndAssert(bimap);
    assertEquals(bimap.inverse(), copy.inverse());
  }

  void putOneTwoThree() {
    bimap.put(1, "one");
    bimap.put(2, "two");
    bimap.put(3, "three");
  }

  @GwtIncompatible("used only by @GwtIncompatible code")
  private static class BiMapPair implements Serializable {
    final BiMap<Integer, String> forward;
    final BiMap<String, Integer> backward;

    BiMapPair(BiMap<Integer, String> original) {
      this.forward = original;
      this.backward = original.inverse();
    }

    private static final long serialVersionUID = 0;
  }

  @GwtIncompatible("SerializableTester")
  public void testSerializationWithInverseEqual() {
    bimap.put(1, "one");
    bimap.put(2, "two");
    bimap.put(3, "three");
    bimap.put(null, null);

    BiMapPair pair = new BiMapPair(bimap);
    BiMapPair copy = SerializableTester.reserialize(pair);
    assertEquals(pair.forward, copy.forward);
    assertEquals(pair.backward, copy.backward);

    copy.forward.put(4, "four");
    copy.backward.put("five", 5);
    assertEquals(copy.backward, copy.forward.inverse());
    assertEquals(copy.forward, copy.backward.inverse());

    assertTrue(copy.forward.containsKey(4));
    assertTrue(copy.forward.containsKey(5));
    assertTrue(copy.backward.containsValue(4));
    assertTrue(copy.backward.containsValue(5));
    assertTrue(copy.forward.containsValue("four"));
    assertTrue(copy.forward.containsValue("five"));
    assertTrue(copy.backward.containsKey("four"));
    assertTrue(copy.backward.containsKey("five"));
  }

  /**
   * The sameness checks ensure that a bimap and its inverse remain consistent,
   * even after the deserialized instances are updated. Also, the relationship
   * {@code a == b.inverse()} should continue to hold after both bimaps are
   * serialized and deserialized together.
   */
  @GwtIncompatible("SerializableTester")
  public void testSerializationWithInverseSame() {
    bimap.put(1, "one");
    bimap.put(2, "two");
    bimap.put(3, "three");
    bimap.put(null, null);

    BiMapPair pair = new BiMapPair(bimap);
    BiMapPair copy = SerializableTester.reserialize(pair);
    assertSame(copy.backward, copy.forward.inverse());
    assertSame(copy.forward, copy.backward.inverse());
  }
}
