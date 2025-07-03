/*
 * Copyright (C) 2012 The Guava Authors
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

package com.google.common.collect.testing.google;

import static com.google.common.collect.testing.Helpers.assertEqualIgnoringOrder;
import static com.google.common.collect.testing.Helpers.copyToList;
import static com.google.common.collect.testing.Helpers.mapEntry;
import static com.google.common.collect.testing.features.CollectionSize.ZERO;
import static com.google.common.collect.testing.features.MapFeature.ALLOWS_NULL_KEYS;
import static com.google.common.collect.testing.features.MapFeature.ALLOWS_NULL_KEY_QUERIES;
import static com.google.common.collect.testing.features.MapFeature.ALLOWS_NULL_VALUES;
import static com.google.common.collect.testing.features.MapFeature.ALLOWS_NULL_VALUE_QUERIES;
import static com.google.common.collect.testing.features.MapFeature.SUPPORTS_REMOVE;
import static com.google.common.collect.testing.google.ReflectionFreeAssertThrows.assertThrows;

import com.google.common.annotations.GwtCompatible;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multimap;
import com.google.common.collect.testing.features.CollectionSize;
import com.google.common.collect.testing.features.MapFeature;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import org.junit.Ignore;

/**
 * Tests for {@link Multimap#remove(Object, Object)}.
 *
 * @author Louis Wasserman
 */
@GwtCompatible
@Ignore("test runners must not instantiate and run this directly, only via suites we build")
// @Ignore affects the Android test runner, which respects JUnit 4 annotations on JUnit 3 tests.
@SuppressWarnings("JUnit4ClassUsedInJUnit3")
public class MultimapRemoveEntryTester<K, V> extends AbstractMultimapTester<K, V, Multimap<K, V>> {
  @MapFeature.Require(SUPPORTS_REMOVE)
  public void testRemoveAbsent() {
    assertFalse(multimap().remove(k0(), v1()));
    expectUnchanged();
  }

  @CollectionSize.Require(absent = ZERO)
  @MapFeature.Require(SUPPORTS_REMOVE)
  public void testRemovePresent() {
    assertTrue(multimap().remove(k0(), v0()));

    assertFalse(multimap().containsEntry(k0(), v0()));
    expectMissing(e0());
    assertEquals(getNumElements() - 1, multimap().size());
    assertGet(k0(), ImmutableList.<V>of());
  }

  @CollectionSize.Require(absent = ZERO)
  @MapFeature.Require({SUPPORTS_REMOVE, ALLOWS_NULL_KEYS})
  public void testRemoveNullKeyPresent() {
    initMultimapWithNullKey();

    assertTrue(multimap().remove(null, getValueForNullKey()));

    expectMissing(mapEntry((K) null, getValueForNullKey()));
    assertGet(getKeyForNullValue(), ImmutableList.<V>of());
  }

  @CollectionSize.Require(absent = ZERO)
  @MapFeature.Require({SUPPORTS_REMOVE, ALLOWS_NULL_VALUES})
  public void testRemoveNullValuePresent() {
    initMultimapWithNullValue();

    assertTrue(multimap().remove(getKeyForNullValue(), null));

    expectMissing(mapEntry(getKeyForNullValue(), (V) null));
    assertGet(getKeyForNullValue(), ImmutableList.<V>of());
  }

  @MapFeature.Require({SUPPORTS_REMOVE, ALLOWS_NULL_KEY_QUERIES})
  public void testRemoveNullKeyAbsent() {
    assertFalse(multimap().remove(null, v0()));
    expectUnchanged();
  }

  @MapFeature.Require({SUPPORTS_REMOVE, ALLOWS_NULL_VALUE_QUERIES})
  public void testRemoveNullValueAbsent() {
    assertFalse(multimap().remove(k0(), null));
    expectUnchanged();
  }

  @MapFeature.Require(value = SUPPORTS_REMOVE, absent = ALLOWS_NULL_VALUE_QUERIES)
  public void testRemoveNullValueForbidden() {
    assertThrows(NullPointerException.class, () -> multimap().remove(k0(), null));
    expectUnchanged();
  }

  @MapFeature.Require(value = SUPPORTS_REMOVE, absent = ALLOWS_NULL_KEY_QUERIES)
  public void testRemoveNullKeyForbidden() {
    assertThrows(NullPointerException.class, () -> multimap().remove(null, v0()));
    expectUnchanged();
  }

  @MapFeature.Require(SUPPORTS_REMOVE)
  @CollectionSize.Require(absent = ZERO)
  public void testRemovePropagatesToGet() {
    List<Entry<K, V>> entries = copyToList(multimap().entries());
    for (Entry<K, V> entry : entries) {
      resetContainer();

      K key = entry.getKey();
      V value = entry.getValue();
      Collection<V> collection = multimap().get(key);
      assertNotNull(collection);
      Collection<V> expectedCollection = copyToList(collection);

      multimap().remove(key, value);
      expectedCollection.remove(value);

      assertEqualIgnoringOrder(expectedCollection, collection);
      assertEquals(!expectedCollection.isEmpty(), multimap().containsKey(key));
    }
  }

  @MapFeature.Require(SUPPORTS_REMOVE)
  @CollectionSize.Require(absent = ZERO)
  public void testRemovePropagatesToAsMap() {
    List<Entry<K, V>> entries = copyToList(multimap().entries());
    for (Entry<K, V> entry : entries) {
      resetContainer();

      K key = entry.getKey();
      V value = entry.getValue();
      Collection<V> collection = multimap().asMap().get(key);
      assertNotNull(collection);
      Collection<V> expectedCollection = copyToList(collection);

      multimap().remove(key, value);
      expectedCollection.remove(value);

      assertEqualIgnoringOrder(expectedCollection, collection);
      assertEquals(!expectedCollection.isEmpty(), multimap().containsKey(key));
    }
  }

  @MapFeature.Require(SUPPORTS_REMOVE)
  @CollectionSize.Require(absent = ZERO)
  public void testRemovePropagatesToAsMapEntrySet() {
    List<Entry<K, V>> entries = copyToList(multimap().entries());
    for (Entry<K, V> entry : entries) {
      resetContainer();

      K key = entry.getKey();
      V value = entry.getValue();

      Iterator<Entry<K, Collection<V>>> asMapItr = multimap().asMap().entrySet().iterator();
      Collection<V> collection = null;
      while (asMapItr.hasNext()) {
        Entry<K, Collection<V>> asMapEntry = asMapItr.next();
        if (key.equals(asMapEntry.getKey())) {
          collection = asMapEntry.getValue();
          break;
        }
      }
      assertNotNull(collection);
      Collection<V> expectedCollection = copyToList(collection);

      multimap().remove(key, value);
      expectedCollection.remove(value);

      assertEqualIgnoringOrder(expectedCollection, collection);
      assertEquals(!expectedCollection.isEmpty(), multimap().containsKey(key));
    }
  }
}
