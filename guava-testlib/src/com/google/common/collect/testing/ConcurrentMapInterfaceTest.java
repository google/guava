/*
 * Copyright (C) 2008 The Guava Authors
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

import java.util.concurrent.ConcurrentMap;

/**
 * Tests representing the contract of {@link ConcurrentMap}. Concrete
 * subclasses of this base class test conformance of concrete
 * {@link ConcurrentMap} subclasses to that contract.
 *
 * <p>This class is GWT compatible.
 *
 * <p>The tests in this class for null keys and values only check maps for
 * which null keys and values are not allowed. There are currently no
 * {@link ConcurrentMap} implementations that support nulls.
 *
 * @author Jared Levy
 */
public abstract class ConcurrentMapInterfaceTest<K, V>
    extends MapInterfaceTest<K, V> {

  protected ConcurrentMapInterfaceTest(boolean allowsNullKeys,
      boolean allowsNullValues, boolean supportsPut, boolean supportsRemove,
      boolean supportsClear) {
    super(allowsNullKeys, allowsNullValues, supportsPut, supportsRemove,
        supportsClear);
  }

  /**
   * Creates a new value that is not expected to be found in
   * {@link #makePopulatedMap()} and differs from the value returned by
   * {@link #getValueNotInPopulatedMap()}.
   *
   * @return a value
   * @throws UnsupportedOperationException if it's not possible to make a value
   * that will not be found in the map
   */
  protected abstract V getSecondValueNotInPopulatedMap()
      throws UnsupportedOperationException;

  @Override protected abstract ConcurrentMap<K, V> makeEmptyMap()
      throws UnsupportedOperationException;

  @Override protected abstract ConcurrentMap<K, V> makePopulatedMap()
      throws UnsupportedOperationException;

  @Override protected ConcurrentMap<K, V> makeEitherMap() {
    try {
      return makePopulatedMap();
    } catch (UnsupportedOperationException e) {
      return makeEmptyMap();
    }
  }

  public void testPutIfAbsentNewKey() {
    final ConcurrentMap<K, V> map;
    final K keyToPut;
    final V valueToPut;
    try {
      map = makeEitherMap();
      keyToPut = getKeyNotInPopulatedMap();
      valueToPut = getValueNotInPopulatedMap();
    } catch (UnsupportedOperationException e) {
      return;
    }
    if (supportsPut) {
      int initialSize = map.size();
      V oldValue = map.putIfAbsent(keyToPut, valueToPut);
      assertEquals(valueToPut, map.get(keyToPut));
      assertTrue(map.containsKey(keyToPut));
      assertTrue(map.containsValue(valueToPut));
      assertEquals(initialSize + 1, map.size());
      assertNull(oldValue);
    } else {
      try {
        map.putIfAbsent(keyToPut, valueToPut);
        fail("Expected UnsupportedOperationException.");
      } catch (UnsupportedOperationException e) {
        // Expected.
      }
    }
    assertInvariants(map);
  }

  public void testPutIfAbsentExistingKey() {
    final ConcurrentMap<K, V> map;
    final K keyToPut;
    final V valueToPut;
    try {
      map = makePopulatedMap();
      valueToPut = getValueNotInPopulatedMap();
    } catch (UnsupportedOperationException e) {
      return;
    }
    keyToPut = map.keySet().iterator().next();
    if (supportsPut) {
      V oldValue = map.get(keyToPut);
      int initialSize = map.size();
      assertEquals(oldValue, map.putIfAbsent(keyToPut, valueToPut));
      assertEquals(oldValue, map.get(keyToPut));
      assertTrue(map.containsKey(keyToPut));
      assertTrue(map.containsValue(oldValue));
      assertFalse(map.containsValue(valueToPut));
      assertEquals(initialSize, map.size());
    } else {
      try {
        map.putIfAbsent(keyToPut, valueToPut);
        fail("Expected UnsupportedOperationException.");
      } catch (UnsupportedOperationException e) {
        // Expected.
      }
    }
    assertInvariants(map);
  }

  public void testPutIfAbsentNullKey() {
    if (allowsNullKeys) {
      return;   // Not yet implemented
    }
    final ConcurrentMap<K, V> map;
    final V valueToPut;
    try {
      map = makeEitherMap();
      valueToPut = getValueNotInPopulatedMap();
    } catch (UnsupportedOperationException e) {
      return;
    }
    int initialSize = map.size();
    if (supportsPut) {
      try {
        map.putIfAbsent(null, valueToPut);
        fail("Expected NullPointerException");
      } catch (NullPointerException e) {
        // Expected.
      }
    } else {
      try {
        map.putIfAbsent(null, valueToPut);
        fail("Expected UnsupportedOperationException or NullPointerException");
      } catch (UnsupportedOperationException e) {
        // Expected.
      } catch (NullPointerException e) {
        // Expected.
      }
    }
    assertEquals(initialSize, map.size());
    assertInvariants(map);
  }

  public void testPutIfAbsentNewKeyNullValue() {
    if (allowsNullValues) {
      return;   // Not yet implemented
    }
    final ConcurrentMap<K, V> map;
    final K keyToPut;
    try {
      map = makeEitherMap();
      keyToPut = getKeyNotInPopulatedMap();
    } catch (UnsupportedOperationException e) {
      return;
    }
    int initialSize = map.size();
    if (supportsPut) {
      try {
        map.putIfAbsent(keyToPut, null);
        fail("Expected NullPointerException");
      } catch (NullPointerException e) {
        // Expected.
      }
    } else {
      try {
        map.putIfAbsent(keyToPut, null);
        fail("Expected UnsupportedOperationException or NullPointerException");
      } catch (UnsupportedOperationException e) {
        // Expected.
      } catch (NullPointerException e) {
        // Expected.
      }
    }
    assertEquals(initialSize, map.size());
    assertInvariants(map);
  }

  public void testPutIfAbsentExistingKeyNullValue() {
    if (allowsNullValues) {
      return;   // Not yet implemented
    }
    final ConcurrentMap<K, V> map;
    final K keyToPut;
    try {
      map = makePopulatedMap();
    } catch (UnsupportedOperationException e) {
      return;
    }
    keyToPut = map.keySet().iterator().next();
    int initialSize = map.size();
    if (supportsPut) {
      try {
        assertNull(map.putIfAbsent(keyToPut, null));
      } catch (NullPointerException e) {
        // Optional.
      }
    } else {
      try {
        map.putIfAbsent(keyToPut, null);
        fail("Expected UnsupportedOperationException or NullPointerException");
      } catch (UnsupportedOperationException e) {
        // Expected.
      } catch (NullPointerException e) {
        // Expected.
      }
    }
    assertEquals(initialSize, map.size());
    assertInvariants(map);
  }

  public void testRemoveKeyValueExisting() {
    final ConcurrentMap<K, V> map;
    final K keyToRemove;
    try {
      map = makePopulatedMap();
    } catch (UnsupportedOperationException e) {
      return;
    }
    keyToRemove = map.keySet().iterator().next();
    V oldValue = map.get(keyToRemove);
    if (supportsRemove) {
      int initialSize = map.size();
      assertTrue(map.remove(keyToRemove, oldValue));
      assertFalse(map.containsKey(keyToRemove));
      assertEquals(initialSize - 1, map.size());
    } else {
      try {
        map.remove(keyToRemove, oldValue);
        fail("Expected UnsupportedOperationException.");
      } catch (UnsupportedOperationException e) {
        // Expected.
      }
    }
    assertInvariants(map);
  }

  public void testRemoveKeyValueMissingKey() {
    final ConcurrentMap<K, V> map;
    final K keyToRemove;
    final V valueToRemove;
    try {
      map = makePopulatedMap();
      keyToRemove = getKeyNotInPopulatedMap();
      valueToRemove = getValueNotInPopulatedMap();
    } catch (UnsupportedOperationException e) {
      return;
    }
    if (supportsRemove) {
      int initialSize = map.size();
      assertFalse(map.remove(keyToRemove, valueToRemove));
      assertEquals(initialSize, map.size());
    } else {
      try {
        map.remove(keyToRemove, valueToRemove);
        fail("Expected UnsupportedOperationException.");
      } catch (UnsupportedOperationException e) {
        // Expected.
      }
    }
    assertInvariants(map);
  }

  public void testRemoveKeyValueDifferentValue() {
    final ConcurrentMap<K, V> map;
    final K keyToRemove;
    final V valueToRemove;
    try {
      map = makePopulatedMap();
      valueToRemove = getValueNotInPopulatedMap();
    } catch (UnsupportedOperationException e) {
      return;
    }
    keyToRemove = map.keySet().iterator().next();
    if (supportsRemove) {
      int initialSize = map.size();
      V oldValue = map.get(keyToRemove);
      assertFalse(map.remove(keyToRemove, valueToRemove));
      assertEquals(oldValue, map.get(keyToRemove));
      assertTrue(map.containsKey(keyToRemove));
      assertEquals(initialSize, map.size());
    } else {
      try {
        map.remove(keyToRemove, valueToRemove);
        fail("Expected UnsupportedOperationException.");
      } catch (UnsupportedOperationException e) {
        // Expected.
      }
    }
    assertInvariants(map);
  }

  public void testRemoveKeyValueNullKey() {
    if (allowsNullKeys) {
      return;   // Not yet implemented
    }
    final ConcurrentMap<K, V> map;
    final V valueToRemove;
    try {
      map = makeEitherMap();
      valueToRemove = getValueNotInPopulatedMap();
    } catch (UnsupportedOperationException e) {
      return;
    }
    int initialSize = map.size();
    if (supportsRemove) {
      try {
        assertFalse(map.remove(null, valueToRemove));
      } catch (NullPointerException e) {
        // Optional.
      }
    } else {
      try {
        assertFalse(map.remove(null, valueToRemove));
      } catch (UnsupportedOperationException e) {
        // Optional.
      } catch (NullPointerException e) {
        // Optional.
      }
    }
    assertEquals(initialSize, map.size());
    assertInvariants(map);
  }

  public void testRemoveKeyValueExistingKeyNullValue() {
    if (allowsNullValues) {
      return;   // Not yet implemented
    }
    final ConcurrentMap<K, V> map;
    final K keyToRemove;
    try {
      map = makePopulatedMap();
    } catch (UnsupportedOperationException e) {
      return;
    }
    keyToRemove = map.keySet().iterator().next();
    int initialSize = map.size();
    if (supportsRemove) {
      try {
        assertFalse(map.remove(keyToRemove, null));
      } catch (NullPointerException e) {
        // Optional.
      }
    } else {
      try {
        assertFalse(map.remove(keyToRemove, null));
      } catch (UnsupportedOperationException e) {
        // Optional.
      } catch (NullPointerException e) {
        // Optional.
      }
    }
    assertEquals(initialSize, map.size());
    assertInvariants(map);
  }

  public void testRemoveKeyValueMissingKeyNullValue() {
    if (allowsNullValues) {
      return;   // Not yet implemented
    }
    final ConcurrentMap<K, V> map;
    final K keyToRemove;
    try {
      map = makeEitherMap();
      keyToRemove = getKeyNotInPopulatedMap();
    } catch (UnsupportedOperationException e) {
      return;
    }
    int initialSize = map.size();
    if (supportsRemove) {
      try {
        assertFalse(map.remove(keyToRemove, null));
      } catch (NullPointerException e) {
        // Optional.
      }
    } else {
      try {
        assertFalse(map.remove(keyToRemove, null));
      } catch (UnsupportedOperationException e) {
        // Optional.
      } catch (NullPointerException e) {
        // Optional.
      }
    }
    assertEquals(initialSize, map.size());
    assertInvariants(map);
  }

  /* Replace2 tests call 2-parameter replace(key, value) */

  public void testReplace2ExistingKey() {
    final ConcurrentMap<K, V> map;
    final K keyToReplace;
    final V newValue;
    try {
      map = makePopulatedMap();
      newValue = getValueNotInPopulatedMap();
    } catch (UnsupportedOperationException e) {
      return;
    }
    keyToReplace = map.keySet().iterator().next();
    if (supportsPut) {
      V oldValue = map.get(keyToReplace);
      int initialSize = map.size();
      assertEquals(oldValue, map.replace(keyToReplace, newValue));
      assertEquals(newValue, map.get(keyToReplace));
      assertTrue(map.containsKey(keyToReplace));
      assertTrue(map.containsValue(newValue));
      assertEquals(initialSize, map.size());
    } else {
      try {
        map.replace(keyToReplace, newValue);
        fail("Expected UnsupportedOperationException.");
      } catch (UnsupportedOperationException e) {
        // Expected.
      }
    }
    assertInvariants(map);
  }

  public void testReplace2MissingKey() {
    final ConcurrentMap<K, V> map;
    final K keyToReplace;
    final V newValue;
    try {
      map = makeEitherMap();
      keyToReplace = getKeyNotInPopulatedMap();
      newValue = getValueNotInPopulatedMap();
    } catch (UnsupportedOperationException e) {
      return;
    }
    if (supportsPut) {
      int initialSize = map.size();
      assertNull(map.replace(keyToReplace, newValue));
      assertNull(map.get(keyToReplace));
      assertFalse(map.containsKey(keyToReplace));
      assertFalse(map.containsValue(newValue));
      assertEquals(initialSize, map.size());
    } else {
      try {
        map.replace(keyToReplace, newValue);
        fail("Expected UnsupportedOperationException.");
      } catch (UnsupportedOperationException e) {
        // Expected.
      }
    }
    assertInvariants(map);
  }

  public void testReplace2NullKey() {
    if (allowsNullKeys) {
      return;   // Not yet implemented
    }
    final ConcurrentMap<K, V> map;
    final V valueToReplace;
    try {
      map = makeEitherMap();
      valueToReplace = getValueNotInPopulatedMap();
    } catch (UnsupportedOperationException e) {
      return;
    }
    int initialSize = map.size();
    if (supportsPut) {
      try {
        assertNull(map.replace(null, valueToReplace));
      } catch (NullPointerException e) {
        // Optional.
      }
    } else {
      try {
        assertNull(map.replace(null, valueToReplace));
      } catch (UnsupportedOperationException e) {
        // Optional.
      } catch (NullPointerException e) {
        // Optional.
      }
    }
    assertEquals(initialSize, map.size());
    assertInvariants(map);
  }

  public void testReplace2ExistingKeyNullValue() {
    if (allowsNullValues) {
      return;   // Not yet implemented
    }
    final ConcurrentMap<K, V> map;
    final K keyToReplace;
    try {
      map = makePopulatedMap();
    } catch (UnsupportedOperationException e) {
      return;
    }
    keyToReplace = map.keySet().iterator().next();
    int initialSize = map.size();
    if (supportsPut) {
      try {
        map.replace(keyToReplace, null);
        fail("Expected NullPointerException");
      } catch (NullPointerException e) {
        // Expected.
      }
    } else {
      try {
        map.replace(keyToReplace, null);
        fail("Expected UnsupportedOperationException or NullPointerException");
      } catch (UnsupportedOperationException e) {
        // Expected.
      } catch (NullPointerException e) {
        // Expected.
      }
    }
    assertEquals(initialSize, map.size());
    assertInvariants(map);
  }

  public void testReplace2MissingKeyNullValue() {
    if (allowsNullValues) {
      return;   // Not yet implemented
    }
    final ConcurrentMap<K, V> map;
    final K keyToReplace;
    try {
      map = makeEitherMap();
      keyToReplace = getKeyNotInPopulatedMap();
    } catch (UnsupportedOperationException e) {
      return;
    }
    int initialSize = map.size();
    if (supportsPut) {
      try {
        assertNull(map.replace(keyToReplace, null));
      } catch (NullPointerException e) {
        // Optional.
      }
    } else {
      try {
        assertNull(map.replace(keyToReplace, null));
      } catch (UnsupportedOperationException e) {
        // Optional.
      } catch (NullPointerException e) {
        // Optional.
      }
    }
    assertEquals(initialSize, map.size());
    assertInvariants(map);
  }

  /*
   * Replace3 tests call 3-parameter replace(key, oldValue, newValue)
   */

  public void testReplace3ExistingKeyValue() {
    final ConcurrentMap<K, V> map;
    final K keyToReplace;
    final V oldValue;
    final V newValue;
    try {
      map = makePopulatedMap();
      newValue = getValueNotInPopulatedMap();
    } catch (UnsupportedOperationException e) {
      return;
    }
    keyToReplace = map.keySet().iterator().next();
    oldValue = map.get(keyToReplace);
    if (supportsPut) {
      int initialSize = map.size();
      assertTrue(map.replace(keyToReplace, oldValue, newValue));
      assertEquals(newValue, map.get(keyToReplace));
      assertTrue(map.containsKey(keyToReplace));
      assertTrue(map.containsValue(newValue));
      assertFalse(map.containsValue(oldValue));
      assertEquals(initialSize, map.size());
    } else {
      try {
        map.replace(keyToReplace, oldValue, newValue);
        fail("Expected UnsupportedOperationException.");
      } catch (UnsupportedOperationException e) {
        // Expected.
      }
    }
    assertInvariants(map);
  }

  public void testReplace3ExistingKeyDifferentValue() {
    final ConcurrentMap<K, V> map;
    final K keyToReplace;
    final V oldValue;
    final V newValue;
    try {
      map = makePopulatedMap();
      oldValue = getValueNotInPopulatedMap();
      newValue = getSecondValueNotInPopulatedMap();
    } catch (UnsupportedOperationException e) {
      return;
    }
    keyToReplace = map.keySet().iterator().next();
    final V originalValue = map.get(keyToReplace);
    int initialSize = map.size();
    if (supportsPut) {
      assertFalse(map.replace(keyToReplace, oldValue, newValue));
    } else {
      try {
        map.replace(keyToReplace, oldValue, newValue);
        fail("Expected UnsupportedOperationException.");
      } catch (UnsupportedOperationException e) {
        // Expected.
      }
    }
    assertTrue(map.containsKey(keyToReplace));
    assertFalse(map.containsValue(newValue));
    assertFalse(map.containsValue(oldValue));
    assertEquals(originalValue, map.get(keyToReplace));
    assertEquals(initialSize, map.size());
    assertInvariants(map);
  }

  public void testReplace3MissingKey() {
    final ConcurrentMap<K, V> map;
    final K keyToReplace;
    final V oldValue;
    final V newValue;
    try {
      map = makeEitherMap();
      keyToReplace = getKeyNotInPopulatedMap();
      oldValue = getValueNotInPopulatedMap();
      newValue = getSecondValueNotInPopulatedMap();
    } catch (UnsupportedOperationException e) {
      return;
    }
    int initialSize = map.size();
    if (supportsPut) {
      assertFalse(map.replace(keyToReplace, oldValue, newValue));
    } else {
      try {
        map.replace(keyToReplace, oldValue, newValue);
        fail("Expected UnsupportedOperationException.");
      } catch (UnsupportedOperationException e) {
        // Expected.
      }
    }
    assertFalse(map.containsKey(keyToReplace));
    assertFalse(map.containsValue(newValue));
    assertFalse(map.containsValue(oldValue));
    assertEquals(initialSize, map.size());
    assertInvariants(map);
  }

  public void testReplace3NullKey() {
    if (allowsNullKeys) {
      return;   // Not yet implemented
    }
    final ConcurrentMap<K, V> map;
    final V oldValue;
    final V newValue;
    try {
      map = makeEitherMap();
      oldValue = getValueNotInPopulatedMap();
      newValue = getSecondValueNotInPopulatedMap();
    } catch (UnsupportedOperationException e) {
      return;
    }
    int initialSize = map.size();
    if (supportsPut) {
      try {
        assertFalse(map.replace(null, oldValue, newValue));
      } catch (NullPointerException e) {
        // Optional.
      }
    } else {
      try {
        assertFalse(map.replace(null, oldValue, newValue));
      } catch (UnsupportedOperationException e) {
        // Optional.
      } catch (NullPointerException e) {
        // Optional.
      }
    }
    assertEquals(initialSize, map.size());
    assertInvariants(map);
  }

  public void testReplace3ExistingKeyNullOldValue() {
    if (allowsNullValues) {
      return;   // Not yet implemented
    }
    final ConcurrentMap<K, V> map;
    final K keyToReplace;
    final V newValue;
    try {
      map = makePopulatedMap();
      newValue = getValueNotInPopulatedMap();
    } catch (UnsupportedOperationException e) {
      return;
    }
    keyToReplace = map.keySet().iterator().next();
    final V originalValue = map.get(keyToReplace);
    int initialSize = map.size();
    if (supportsPut) {
      try {
        assertFalse(map.replace(keyToReplace, null, newValue));
      } catch (NullPointerException e) {
        // Optional.
      }
    } else {
      try {
        assertFalse(map.replace(keyToReplace, null, newValue));
      } catch (UnsupportedOperationException e) {
        // Optional.
      } catch (NullPointerException e) {
        // Optional.
      }
    }
    assertEquals(initialSize, map.size());
    assertEquals(originalValue, map.get(keyToReplace));
    assertInvariants(map);
  }

  public void testReplace3MissingKeyNullOldValue() {
    if (allowsNullValues) {
      return;   // Not yet implemented
    }
    final ConcurrentMap<K, V> map;
    final K keyToReplace;
    final V newValue;
    try {
      map = makeEitherMap();
      keyToReplace = getKeyNotInPopulatedMap();
      newValue = getValueNotInPopulatedMap();
    } catch (UnsupportedOperationException e) {
      return;
    }
    int initialSize = map.size();
    if (supportsPut) {
      try {
        assertFalse(map.replace(keyToReplace, null, newValue));
      } catch (NullPointerException e) {
        // Optional.
      }
    } else {
      try {
        assertFalse(map.replace(keyToReplace, null, newValue));
      } catch (UnsupportedOperationException e) {
        // Optional.
      } catch (NullPointerException e) {
        // Optional.
      }
    }
    assertEquals(initialSize, map.size());
    assertInvariants(map);
  }

  public void testReplace3MissingKeyNullNewValue() {
    if (allowsNullValues) {
      return;   // Not yet implemented
    }
    final ConcurrentMap<K, V> map;
    final K keyToReplace;
    final V oldValue;
    try {
      map = makeEitherMap();
      keyToReplace = getKeyNotInPopulatedMap();
      oldValue = getValueNotInPopulatedMap();
    } catch (UnsupportedOperationException e) {
      return;
    }
    int initialSize = map.size();
    if (supportsPut) {
      try {
        map.replace(keyToReplace, oldValue, null);
      } catch (NullPointerException e) {
        // Optional.
      }
    } else {
      try {
        map.replace(keyToReplace, oldValue, null);
      } catch (UnsupportedOperationException e) {
        // Optional.
      } catch (NullPointerException e) {
        // Optional.
      }
    }
    assertEquals(initialSize, map.size());
    assertInvariants(map);
  }

  public void testReplace3ExistingKeyValueNullNewValue() {
    if (allowsNullValues) {
      return;   // Not yet implemented
    }
    final ConcurrentMap<K, V> map;
    final K keyToReplace;
    final V oldValue;
    try {
      map = makePopulatedMap();
    } catch (UnsupportedOperationException e) {
      return;
    }
    keyToReplace = map.keySet().iterator().next();
    oldValue = map.get(keyToReplace);
    int initialSize = map.size();
    if (supportsPut) {
      try {
        map.replace(keyToReplace, oldValue, null);
        fail("Expected NullPointerException");
      } catch (NullPointerException e) {
        // Expected.
      }
    } else {
      try {
        map.replace(keyToReplace, oldValue, null);
        fail("Expected UnsupportedOperationException or NullPointerException");
      } catch (UnsupportedOperationException e) {
        // Expected.
      } catch (NullPointerException e) {
        // Expected.
      }
    }
    assertEquals(initialSize, map.size());
    assertEquals(oldValue, map.get(keyToReplace));
    assertInvariants(map);
  }
}
