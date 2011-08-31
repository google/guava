/*
 * Copyright (C) 2009 The Guava Authors
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

package com.google.common.collect.testing;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.SortedMap;

/**
 * Tests representing the contract of {@link SortedMap}. Concrete subclasses of
 * this base class test conformance of concrete {@link SortedMap} subclasses to
 * that contract.
 *
 * <p>This class is GWT compatible.
 *
 * @author Jared Levy
 */
// TODO: Use this class to test classes besides ImmutableSortedMap.
public abstract class SortedMapInterfaceTest<K, V>
    extends MapInterfaceTest<K, V> {

  /** A key type that is not assignable to any classes but Object. */
  private static final class IncompatibleComparableKeyType
      implements Comparable<IncompatibleComparableKeyType> {
    @Override public String toString() {
      return "IncompatibleComparableKeyType";
    }
    @Override
    public int compareTo(IncompatibleComparableKeyType o) {
      throw new ClassCastException();
    }
  }

  protected SortedMapInterfaceTest(boolean allowsNullKeys,
      boolean allowsNullValues, boolean supportsPut, boolean supportsRemove,
      boolean supportsClear) {
    super(allowsNullKeys, allowsNullValues, supportsPut, supportsRemove,
        supportsClear);
  }

  @Override protected abstract SortedMap<K, V> makeEmptyMap()
      throws UnsupportedOperationException;

  @Override protected abstract SortedMap<K, V> makePopulatedMap()
      throws UnsupportedOperationException;

  @Override protected SortedMap<K, V> makeEitherMap() {
    try {
      return makePopulatedMap();
    } catch (UnsupportedOperationException e) {
      return makeEmptyMap();
    }
  }

  @SuppressWarnings("unchecked") // Needed for null comparator
  public void testOrdering() {
    final SortedMap<K, V> map;
    try {
      map = makePopulatedMap();
    } catch (UnsupportedOperationException e) {
      return;
    }
    Iterator<K> iterator = map.keySet().iterator();
    K prior = iterator.next();
    Comparator<? super K> comparator = map.comparator();
    while (iterator.hasNext()) {
      K current = iterator.next();
      if (comparator == null) {
        Comparable comparable = (Comparable) prior;
        assertTrue(comparable.compareTo(current) < 0);
      } else {
        assertTrue(map.comparator().compare(prior, current) < 0);
      }
      current = prior;
    }
  }

  public void testEntrySetContainsEntryIncompatibleComparableKey() {
    final Map<K, V> map;
    final Set<Entry<K, V>> entrySet;
    try {
      map = makeEitherMap();
    } catch (UnsupportedOperationException e) {
      return;
    }
    assertInvariants(map);

    entrySet = map.entrySet();
    final V unmappedValue;
    try {
      unmappedValue = getValueNotInPopulatedMap();
    } catch (UnsupportedOperationException e) {
      return;
    }
    Entry<IncompatibleComparableKeyType, V> entry
        = mapEntry(new IncompatibleComparableKeyType(), unmappedValue);
    assertFalse(entrySet.contains(entry));
  }

  public void testFirstKeyEmpty() {
    final SortedMap<K, V> map;
    try {
      map = makeEmptyMap();
    } catch (UnsupportedOperationException e) {
      return;
    }
    try {
      map.firstKey();
      fail("Expected NoSuchElementException");
    } catch (NoSuchElementException expected) {}
    assertInvariants(map);
  }

  public void testFirstKeyNonEmpty() {
    final SortedMap<K, V> map;
    try {
      map = makePopulatedMap();
    } catch (UnsupportedOperationException e) {
      return;
    }
    K expected = map.keySet().iterator().next();
    assertEquals(expected, map.firstKey());
    assertInvariants(map);
  }

  public void testLastKeyEmpty() {
    final SortedMap<K, V> map;
    try {
      map = makeEmptyMap();
    } catch (UnsupportedOperationException e) {
      return;
    }
    try {
      map.lastKey();
      fail("Expected NoSuchElementException");
    } catch (NoSuchElementException expected) {}
    assertInvariants(map);
  }

  public void testLastKeyNonEmpty() {
    final SortedMap<K, V> map;
    try {
      map = makePopulatedMap();
    } catch (UnsupportedOperationException e) {
      return;
    }
    K expected = null;
    for (K key : map.keySet()) {
      expected = key;
    }
    assertEquals(expected, map.lastKey());
    assertInvariants(map);
  }

  private static <E> List<E> toList(Collection<E> collection) {
    return new ArrayList<E>(collection);
  }

  private static <E> List<E> subListSnapshot(
      List<E> list, int fromIndex, int toIndex) {
    List<E> subList = new ArrayList<E>();
    for (int i = fromIndex; i < toIndex; i++) {
      subList.add(list.get(i));
    }
    return Collections.unmodifiableList(subList);
  }

  public void testHeadMap() {
    final SortedMap<K, V> map;
    try {
      map = makeEitherMap();
    } catch (UnsupportedOperationException e) {
      return;
    }
    List<Entry<K, V>> list = toList(map.entrySet());
    for (int i = 0; i < list.size(); i++) {
      List<Entry<K, V>> expected = subListSnapshot(list, 0, i);
      SortedMap<K, V> headMap = map.headMap(list.get(i).getKey());
      assertEquals(expected, toList(headMap.entrySet()));
    }
  }

  public void testTailMap() {
    final SortedMap<K, V> map;
    try {
      map = makeEitherMap();
    } catch (UnsupportedOperationException e) {
      return;
    }
    List<Entry<K, V>> list = toList(map.entrySet());
    for (int i = 0; i < list.size(); i++) {
      List<Entry<K, V>> expected = subListSnapshot(list, i, list.size());
      SortedMap<K, V> tailMap = map.tailMap(list.get(i).getKey());
      assertEquals(expected, toList(tailMap.entrySet()));
    }
  }

  public void testSubMap() {
    final SortedMap<K, V> map;
    try {
      map = makeEitherMap();
    } catch (UnsupportedOperationException e) {
      return;
    }
    List<Entry<K, V>> list = toList(map.entrySet());
    for (int i = 0; i < list.size(); i++) {
      for (int j = i; j < list.size(); j++) {
        List<Entry<K, V>> expected = subListSnapshot(list, i, j);
        SortedMap<K, V> subMap
            = map.subMap(list.get(i).getKey(), list.get(j).getKey());
        assertEquals(expected, toList(subMap.entrySet()));
      }
    }
  }

  public void testSubMapIllegal() {
    final SortedMap<K, V> map;
    try {
      map = makePopulatedMap();
    } catch (UnsupportedOperationException e) {
      return;
    }
    if (map.size() < 2) {
      return;
    }
    Iterator<K> iterator = map.keySet().iterator();
    K first = iterator.next();
    K second = iterator.next();
    try {
      map.subMap(second, first);
      fail("Expected IllegalArgumentException");
    } catch (IllegalArgumentException expected) {}
  }

  public void testTailMapEntrySet() {
    final SortedMap<K, V> map;
    try {
      map = makePopulatedMap();
    } catch (UnsupportedOperationException e) {
      return;
    }
    if (map.size() < 3) {
      return;
    }
    Iterator<Entry<K, V>> iterator = map.entrySet().iterator();
    Entry<K, V> firstEntry = iterator.next();
    Entry<K, V> secondEntry = iterator.next();
    Entry<K, V> thirdEntry = iterator.next();
    SortedMap<K, V> tail = map.tailMap(secondEntry.getKey());
    Set<Entry<K, V>> tailEntrySet = tail.entrySet();
    assertTrue(tailEntrySet.contains(thirdEntry));
    assertTrue(tailEntrySet.contains(secondEntry));
    assertFalse(tailEntrySet.contains(firstEntry));
    assertEquals(tail.firstKey(), secondEntry.getKey());
  }

  public void testHeadMapEntrySet() {
    final SortedMap<K, V> map;
    try {
      map = makePopulatedMap();
    } catch (UnsupportedOperationException e) {
      return;
    }
    if (map.size() < 3) {
      return;
    }
    Iterator<Entry<K, V>> iterator = map.entrySet().iterator();
    Entry<K, V> firstEntry = iterator.next();
    Entry<K, V> secondEntry = iterator.next();
    Entry<K, V> thirdEntry = iterator.next();
    SortedMap<K, V> head = map.headMap(secondEntry.getKey());
    Set<Entry<K, V>> headEntrySet = head.entrySet();
    assertFalse(headEntrySet.contains(thirdEntry));
    assertFalse(headEntrySet.contains(secondEntry));
    assertTrue(headEntrySet.contains(firstEntry));
    assertEquals(head.firstKey(), firstEntry.getKey());
    assertEquals(head.lastKey(), firstEntry.getKey());
  }

  public void testTailMapWriteThrough() {
    final SortedMap<K, V> map;
    try {
      map = makePopulatedMap();
    } catch (UnsupportedOperationException e) {
      return;
    }
    if (map.size() < 2 || !supportsPut) {
      return;
    }
    Iterator<Entry<K, V>> iterator = map.entrySet().iterator();
    Entry<K, V> firstEntry = iterator.next();
    Entry<K, V> secondEntry = iterator.next();
    K key = secondEntry.getKey();
    SortedMap<K, V> subMap = map.tailMap(key);
    V value = getValueNotInPopulatedMap();
    subMap.put(key, value);
    assertEquals(secondEntry.getValue(), value);
    assertEquals(map.get(key), value);
    try {
      subMap.put(firstEntry.getKey(), value);
      fail("Expected IllegalArgumentException");
    } catch (IllegalArgumentException expected) {
    }
  }

  public void testTailMapRemoveThrough() {
    final SortedMap<K, V> map;
    try {
      map = makePopulatedMap();
    } catch (UnsupportedOperationException e) {
      return;
    }
    int oldSize = map.size();
    if (map.size() < 2 || !supportsRemove) {
      return;
    }
    Iterator<Entry<K, V>> iterator = map.entrySet().iterator();
    Entry<K, V> firstEntry = iterator.next();
    Entry<K, V> secondEntry = iterator.next();
    K key = secondEntry.getKey();
    SortedMap<K, V> subMap = map.tailMap(key);
    subMap.remove(key);
    assertNull(subMap.remove(firstEntry.getKey()));
    assertEquals(map.size(), oldSize - 1);
    assertFalse(map.containsKey(key));
    assertEquals(subMap.size(), oldSize - 2);
  }

  public void testTailMapClearThrough() {
    final SortedMap<K, V> map;
    try {
      map = makePopulatedMap();
    } catch (UnsupportedOperationException e) {
      return;
    }
    int oldSize = map.size();
    if (map.size() < 2 || !supportsClear) {
      return;
    }
    Iterator<Entry<K, V>> iterator = map.entrySet().iterator();
    Entry<K, V> firstEntry = iterator.next();
    Entry<K, V> secondEntry = iterator.next();
    K key = secondEntry.getKey();
    SortedMap<K, V> subMap = map.tailMap(key);
    int subMapSize = subMap.size();
    subMap.clear();
    assertEquals(map.size(), oldSize - subMapSize);
    assertTrue(subMap.isEmpty());
  }
}
