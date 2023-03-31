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

import com.google.common.annotations.GwtCompatible;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.SortedMap;

/**
 * Tests representing the contract of {@link SortedMap}. Concrete subclasses of this base class test
 * conformance of concrete {@link SortedMap} subclasses to that contract.
 *
 * @author Jared Levy
 */
// TODO: Use this class to test classes besides ImmutableSortedMap.
@GwtCompatible
public abstract class SortedMapInterfaceTest<K, V> extends MapInterfaceTest<K, V> {

  protected SortedMapInterfaceTest(
      boolean allowsNullKeys,
      boolean allowsNullValues,
      boolean supportsPut,
      boolean supportsRemove,
      boolean supportsClear) {
    super(allowsNullKeys, allowsNullValues, supportsPut, supportsRemove, supportsClear);
  }

  @Override
  protected abstract SortedMap<K, V> makeEmptyMap() throws UnsupportedOperationException;

  @Override
  protected abstract SortedMap<K, V> makePopulatedMap() throws UnsupportedOperationException;

  @Override
  protected SortedMap<K, V> makeEitherMap() {
    try {
      return makePopulatedMap();
    } catch (UnsupportedOperationException e) {
      return makeEmptyMap();
    }
  }

  public void testTailMapWriteThrough() {
    SortedMap<K, V> map;
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
    SortedMap<K, V> map;
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
    SortedMap<K, V> map;
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
    iterator.next(); // advance
    Entry<K, V> secondEntry = iterator.next();
    K key = secondEntry.getKey();
    SortedMap<K, V> subMap = map.tailMap(key);
    int subMapSize = subMap.size();
    subMap.clear();
    assertEquals(map.size(), oldSize - subMapSize);
    assertTrue(subMap.isEmpty());
  }
}
