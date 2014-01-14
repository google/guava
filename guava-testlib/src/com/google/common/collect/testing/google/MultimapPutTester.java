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

import static com.google.common.collect.testing.features.CollectionSize.ZERO;
import static com.google.common.collect.testing.features.MapFeature.ALLOWS_NULL_KEYS;
import static com.google.common.collect.testing.features.MapFeature.ALLOWS_NULL_VALUES;
import static com.google.common.collect.testing.features.MapFeature.SUPPORTS_PUT;
import static org.truth0.Truth.ASSERT;

import com.google.common.annotations.GwtCompatible;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.testing.Helpers;
import com.google.common.collect.testing.features.CollectionSize;
import com.google.common.collect.testing.features.MapFeature;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

/**
 * Tester for {@link Multimap#put}.
 *
 * @author Louis Wasserman
 */
@GwtCompatible
public class MultimapPutTester<K, V> extends AbstractMultimapTester<K, V, Multimap<K, V>> {
  @MapFeature.Require(absent = SUPPORTS_PUT)
  public void testPutUnsupported() {
    try {
      multimap().put(sampleKeys().e3, sampleValues().e3);
      fail("Expected UnsupportedOperationException");
    } catch (UnsupportedOperationException expected) {}
  }
  
  @MapFeature.Require(SUPPORTS_PUT)
  public void testPutEmpty() {
    int size = getNumElements();

    K key = sampleKeys().e3;
    V value = sampleValues().e3;

    assertGet(key, ImmutableList.<V>of());

    assertTrue(multimap().put(key, value));

    assertGet(key, value);
    assertEquals(size + 1, multimap().size());
  }

  @MapFeature.Require(SUPPORTS_PUT)
  @CollectionSize.Require(absent = ZERO)
  public void testPutPresent() {
    int size = getNumElements();

    K key = sampleKeys().e0;
    V oldValue = sampleValues().e0;
    V newValue = sampleValues().e3;

    assertGet(key, oldValue);

    assertTrue(multimap().put(key, newValue));

    assertGet(key, oldValue, newValue);
    assertEquals(size + 1, multimap().size());
  }

  @MapFeature.Require(SUPPORTS_PUT)
  public void testPutTwoElements() {
    int size = getNumElements();

    K key = sampleKeys().e0;
    V v1 = sampleValues().e3;
    V v2 = sampleValues().e4;

    List<V> values = Helpers.copyToList(multimap().get(key));

    assertTrue(multimap().put(key, v1));
    assertTrue(multimap().put(key, v2));

    values.add(v1);
    values.add(v2);

    assertGet(key, values);
    assertEquals(size + 2, multimap().size());
  }

  @MapFeature.Require({SUPPORTS_PUT, ALLOWS_NULL_VALUES})
  public void testPutNullValue_supported() {
    int size = getNumElements();

    multimap().put(sampleKeys().e3, null);

    assertGet(sampleKeys().e3, Lists.newArrayList((V) null)); // ImmutableList.of can't take null.
    assertEquals(size + 1, multimap().size());
  }

  @MapFeature.Require(value = SUPPORTS_PUT, absent = ALLOWS_NULL_VALUES)
  public void testPutNullValue_unsupported() {
    try {
      multimap().put(sampleKeys().e1, null);
      fail();
    } catch (NullPointerException expected) {
    }

    expectUnchanged();
  }

  @MapFeature.Require({SUPPORTS_PUT, ALLOWS_NULL_KEYS})
  public void testPutNullKey() {
    int size = getNumElements();

    multimap().put(null, sampleValues().e3);

    assertGet(null, sampleValues().e3);
    assertEquals(size + 1, multimap().size());
  }

  @MapFeature.Require(SUPPORTS_PUT)
  public void testPutNotPresentKeyPropagatesToGet() {
    int size = getNumElements();
    Collection<V> collection = multimap().get(sampleKeys().e3);
    ASSERT.that(collection).isEmpty();
    multimap().put(sampleKeys().e3, sampleValues().e3);
    ASSERT.that(collection).has().item(sampleValues().e3);
    assertEquals(size + 1, multimap().size());
  }

  @MapFeature.Require(SUPPORTS_PUT)
  public void testPutNotPresentKeyPropagatesToEntries() {
    Collection<Entry<K, V>> entries = multimap().entries();
    assertFalse(entries.contains(Helpers.mapEntry(sampleKeys().e3, sampleValues().e3)));
    multimap().put(sampleKeys().e3, sampleValues().e3);
    ASSERT.that(entries).has().allOf(Helpers.mapEntry(sampleKeys().e3, sampleValues().e3));
  }

  @CollectionSize.Require(absent = ZERO)
  @MapFeature.Require(SUPPORTS_PUT)
  public void testPutPresentKeyPropagatesToEntries() {
    Collection<Entry<K, V>> entries = multimap().entries();
    assertFalse(entries.contains(Helpers.mapEntry(sampleKeys().e0, sampleValues().e3)));
    multimap().put(sampleKeys().e0, sampleValues().e3);
    ASSERT.that(entries).has().allOf(Helpers.mapEntry(sampleKeys().e0, sampleValues().e3));
  }

  @MapFeature.Require(SUPPORTS_PUT)
  @CollectionSize.Require(absent = ZERO)
  public void testPutPresentKeyPropagatesToGet() {
    List<K> keys = Helpers.copyToList(multimap().keySet());
    for (K key : keys) {
      resetContainer();

      int size = getNumElements();

      Collection<V> collection = multimap().get(key);
      Collection<V> expectedCollection = Helpers.copyToList(collection);

      multimap().put(key, sampleValues().e3);
      expectedCollection.add(sampleValues().e3);
      ASSERT.that(collection).has().exactlyAs(expectedCollection);
      assertEquals(size + 1, multimap().size());
    }
  }

  @MapFeature.Require(SUPPORTS_PUT)
  @CollectionSize.Require(absent = ZERO)
  public void testPutPresentKeyPropagatesToAsMapGet() {
    List<K> keys = Helpers.copyToList(multimap().keySet());
    for (K key : keys) {
      resetContainer();

      int size = getNumElements();

      Collection<V> collection = multimap().asMap().get(key);
      assertNotNull(collection);
      Collection<V> expectedCollection = Helpers.copyToList(collection);

      multimap().put(key, sampleValues().e3);
      expectedCollection.add(sampleValues().e3);
      ASSERT.that(collection).has().exactlyAs(expectedCollection);
      assertEquals(size + 1, multimap().size());
    }
  }

  @MapFeature.Require(SUPPORTS_PUT)
  @CollectionSize.Require(absent = ZERO)
  public void testPutPresentKeyPropagatesToAsMapEntrySet() {
    List<K> keys = Helpers.copyToList(multimap().keySet());
    for (K key : keys) {
      resetContainer();

      int size = getNumElements();

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
      Collection<V> expectedCollection = Helpers.copyToList(collection);

      multimap().put(key, sampleValues().e3);
      expectedCollection.add(sampleValues().e3);
      ASSERT.that(collection).has().exactlyAs(expectedCollection);
      assertEquals(size + 1, multimap().size());
    }
  }
}
