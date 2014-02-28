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

/**
 * Tests {@link java.util.Map#entrySet}.
 * 
 * @author Louis Wasserman
 * @param <K> The key type of the map implementation under test.
 * @param <V> The value type of the map implementation under test.
 */
@GwtCompatible
public class MapEntrySetTester<K, V> extends AbstractMapTester<K, V> {
  private enum IncomparableType {
    INSTANCE;
  }
  
  @CollectionSize.Require(ONE)
  @CollectionFeature.Require(SUPPORTS_ITERATOR_REMOVE)
  public void testEntrySetIteratorRemove() {
    Set<Entry<K, V>> entrySet = getMap().entrySet();
    Iterator<Entry<K, V>> entryItr = entrySet.iterator();
    assertEquals(samples.e0, entryItr.next());
    entryItr.remove();
    assertTrue(getMap().isEmpty());
    assertFalse(entrySet.contains(samples.e0));
  }

  public void testContainsEntryWithIncomparableKey() {
    assertFalse(getMap()
        .entrySet().contains(Helpers.mapEntry(IncomparableType.INSTANCE, samples.e0.getValue())));
  }

  public void testContainsEntryWithIncomparableValue() {
    assertFalse(getMap()
        .entrySet().contains(Helpers.mapEntry(samples.e0.getKey(), IncomparableType.INSTANCE)));
  }
  
  @MapFeature.Require(ALLOWS_NULL_KEY_QUERIES)
  public void testContainsEntryWithNullKeyAbsent() {
    assertFalse(getMap()
        .entrySet().contains(Helpers.mapEntry(null, samples.e0.getValue())));
  }
  
  @CollectionSize.Require(absent = ZERO)
  @MapFeature.Require(ALLOWS_NULL_KEYS)
  public void testContainsEntryWithNullKeyPresent() {
    initMapWithNullKey();
    assertTrue(getMap()
        .entrySet().contains(Helpers.mapEntry(null, getValueForNullKey())));
  }
  
  @MapFeature.Require(ALLOWS_NULL_VALUE_QUERIES)
  public void testContainsEntryWithNullValueAbsent() {
    assertFalse(getMap()
        .entrySet().contains(Helpers.mapEntry(samples.e0.getKey(), null)));
  }
  
  @CollectionSize.Require(absent = ZERO)
  @MapFeature.Require(ALLOWS_NULL_VALUES)
  public void testContainsEntryWithNullValuePresent() {
    initMapWithNullValue();
    assertTrue(getMap()
        .entrySet().contains(Helpers.mapEntry(getKeyForNullValue(), null)));
  }
  
  @GwtIncompatible("reflection")
  public static Method getContainsEntryWithIncomparableKeyMethod() {
    return Helpers.getMethod(MapEntrySetTester.class, "testContainsEntryWithIncomparableKey");
  }
  
  @GwtIncompatible("reflection")
  public static Method getContainsEntryWithIncomparableValueMethod() {
    return Helpers.getMethod(MapEntrySetTester.class, "testContainsEntryWithIncomparableValue");
  }
}
