/*
 * Copyright (C) 2013 The Guava Authors
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

package com.google.common.collect.testing.testers;

import static com.google.common.collect.testing.features.CollectionFeature.SUPPORTS_ITERATOR_REMOVE;
import static com.google.common.collect.testing.features.CollectionSize.ONE;
import static com.google.common.collect.testing.features.CollectionSize.ZERO;
import static com.google.common.collect.testing.features.MapFeature.ALLOWS_NULL_KEYS;
import static com.google.common.collect.testing.features.MapFeature.ALLOWS_NULL_KEY_QUERIES;
import static com.google.common.collect.testing.features.MapFeature.ALLOWS_NULL_VALUES;
import static com.google.common.collect.testing.features.MapFeature.ALLOWS_NULL_VALUE_QUERIES;
import static com.google.common.collect.testing.features.MapFeature.SUPPORTS_PUT;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.collect.testing.AbstractMapTester;
import com.google.common.collect.testing.Helpers;
import com.google.common.collect.testing.features.CollectionFeature;
import com.google.common.collect.testing.features.CollectionSize;
import com.google.common.collect.testing.features.MapFeature;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;
import org.junit.Ignore;

/**
 * Tests {@link java.util.Map#entrySet}.
 *
 * @author Louis Wasserman
 * @param <K> The key type of the map implementation under test.
 * @param <V> The value type of the map implementation under test.
 */
@GwtCompatible
@Ignore // Affects only Android test runner, which respects JUnit 4 annotations on JUnit 3 tests.
public class MapEntrySetTester<K, V> extends AbstractMapTester<K, V> {
  private enum IncomparableType {
    INSTANCE;
  }

  @CollectionSize.Require(ONE)
  @CollectionFeature.Require(SUPPORTS_ITERATOR_REMOVE)
  public void testEntrySetIteratorRemove() {
    Set<Entry<K, V>> entrySet = getMap().entrySet();
    Iterator<Entry<K, V>> entryItr = entrySet.iterator();
    assertEquals(e0(), entryItr.next());
    entryItr.remove();
    assertTrue(getMap().isEmpty());
    assertFalse(entrySet.contains(e0()));
  }

  public void testContainsEntryWithIncomparableKey() {
    try {
      assertFalse(getMap().entrySet().contains(Helpers.mapEntry(IncomparableType.INSTANCE, v0())));
    } catch (ClassCastException acceptable) {
      // allowed by the spec
    }
  }

  public void testContainsEntryWithIncomparableValue() {
    try {
      assertFalse(getMap().entrySet().contains(Helpers.mapEntry(k0(), IncomparableType.INSTANCE)));
    } catch (ClassCastException acceptable) {
      // allowed by the spec
    }
  }

  @MapFeature.Require(ALLOWS_NULL_KEY_QUERIES)
  public void testContainsEntryWithNullKeyAbsent() {
    assertFalse(getMap().entrySet().contains(Helpers.mapEntry(null, v0())));
  }

  @CollectionSize.Require(absent = ZERO)
  @MapFeature.Require(ALLOWS_NULL_KEYS)
  public void testContainsEntryWithNullKeyPresent() {
    initMapWithNullKey();
    assertTrue(getMap().entrySet().contains(Helpers.mapEntry(null, getValueForNullKey())));
  }

  @MapFeature.Require(ALLOWS_NULL_VALUE_QUERIES)
  public void testContainsEntryWithNullValueAbsent() {
    assertFalse(getMap().entrySet().contains(Helpers.mapEntry(k0(), null)));
  }

  @CollectionSize.Require(absent = ZERO)
  @MapFeature.Require(ALLOWS_NULL_VALUES)
  public void testContainsEntryWithNullValuePresent() {
    initMapWithNullValue();
    assertTrue(getMap().entrySet().contains(Helpers.mapEntry(getKeyForNullValue(), null)));
  }

  @MapFeature.Require(SUPPORTS_PUT)
  @CollectionSize.Require(absent = ZERO)
  public void testSetValue() {
    for (Entry<K, V> entry : getMap().entrySet()) {
      if (entry.getKey().equals(k0())) {
        assertEquals("entry.setValue() should return the old value", v0(), entry.setValue(v3()));
        break;
      }
    }
    expectReplacement(entry(k0(), v3()));
  }

  @MapFeature.Require({SUPPORTS_PUT, ALLOWS_NULL_VALUES})
  @CollectionSize.Require(absent = ZERO)
  public void testSetValueWithNullValuesPresent() {
    for (Entry<K, V> entry : getMap().entrySet()) {
      if (entry.getKey().equals(k0())) {
        assertEquals("entry.setValue() should return the old value", v0(), entry.setValue(null));
        break;
      }
    }
    expectReplacement(entry(k0(), (V) null));
  }

  @MapFeature.Require(value = SUPPORTS_PUT, absent = ALLOWS_NULL_VALUES)
  @CollectionSize.Require(absent = ZERO)
  public void testSetValueWithNullValuesAbsent() {
    for (Entry<K, V> entry : getMap().entrySet()) {
      try {
        entry.setValue(null);
        fail("Expected NullPointerException");
      } catch (NullPointerException exception) {
        break;
      }
    }
    expectUnchanged();
  }

  @GwtIncompatible // reflection
  public static Method getContainsEntryWithIncomparableKeyMethod() {
    return Helpers.getMethod(MapEntrySetTester.class, "testContainsEntryWithIncomparableKey");
  }

  @GwtIncompatible // reflection
  public static Method getContainsEntryWithIncomparableValueMethod() {
    return Helpers.getMethod(MapEntrySetTester.class, "testContainsEntryWithIncomparableValue");
  }

  @GwtIncompatible // reflection
  public static Method getSetValueMethod() {
    return Helpers.getMethod(MapEntrySetTester.class, "testSetValue");
  }

  @GwtIncompatible // reflection
  public static Method getSetValueWithNullValuesPresentMethod() {
    return Helpers.getMethod(MapEntrySetTester.class, "testSetValueWithNullValuesPresent");
  }

  @GwtIncompatible // reflection
  public static Method getSetValueWithNullValuesAbsentMethod() {
    return Helpers.getMethod(MapEntrySetTester.class, "testSetValueWithNullValuesAbsent");
  }
}
