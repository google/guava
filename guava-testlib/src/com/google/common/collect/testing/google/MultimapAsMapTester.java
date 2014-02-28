/*
 * Copyright (C) 2013 The Guava Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.common.collect.testing.google;

import static com.google.common.collect.testing.features.CollectionFeature.SUPPORTS_ITERATOR_REMOVE;
import static com.google.common.collect.testing.features.CollectionSize.SEVERAL;
import static com.google.common.collect.testing.features.CollectionSize.ZERO;
import static com.google.common.collect.testing.features.MapFeature.ALLOWS_NULL_KEYS;
import static com.google.common.collect.testing.features.MapFeature.ALLOWS_NULL_KEY_QUERIES;
import static com.google.common.collect.testing.features.MapFeature.SUPPORTS_PUT;
import static com.google.common.collect.testing.features.MapFeature.SUPPORTS_REMOVE;
import static org.truth0.Truth.ASSERT;

import com.google.common.annotations.GwtCompatible;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;
import com.google.common.collect.testing.Helpers;
import com.google.common.collect.testing.features.CollectionFeature;
import com.google.common.collect.testing.features.CollectionSize;
import com.google.common.collect.testing.features.MapFeature;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

/**
 * Tests for {@link Multimap#asMap}.
 *
 * @author Louis Wasserman
 */
@GwtCompatible
public class MultimapAsMapTester<K, V> extends AbstractMultimapTester<K, V, Multimap<K, V>> {
  public void testAsMapGet() {
    for (K key : sampleKeys()) {
      List<V> expectedValues = new ArrayList<V>();
      for (Entry<K, V> entry : getSampleElements()) {
        if (entry.getKey().equals(key)) {
          expectedValues.add(entry.getValue());
        }
      }

      Collection<V> collection = multimap().asMap().get(key);
      if (expectedValues.isEmpty()) {
        ASSERT.that(collection).isNull();
      } else {
        ASSERT.that(collection).has().exactlyAs(expectedValues);
      }
    }
  }
 
  @CollectionSize.Require(absent = ZERO)
  @MapFeature.Require(ALLOWS_NULL_KEYS)
  public void testAsMapGetNullKeyPresent() {
    initMultimapWithNullKey();
    ASSERT.that(multimap().asMap().get(null)).has().exactly(getValueForNullKey());
  }
 
  @MapFeature.Require(ALLOWS_NULL_KEY_QUERIES)
  public void testAsMapGetNullKeyAbsent() {
    ASSERT.that(multimap().asMap().get(null)).isNull();
  }
 
  @MapFeature.Require(absent = ALLOWS_NULL_KEY_QUERIES)
  public void testAsMapGetNullKeyUnsupported() {
    try {
      multimap().asMap().get(null);
      fail("Expected NullPointerException");
    } catch (NullPointerException expected) {}
  }
  
  @CollectionSize.Require(absent = ZERO)
  @MapFeature.Require(SUPPORTS_REMOVE)
  public void testAsMapRemove() {
    ASSERT.that(multimap().asMap().remove(sampleKeys().e0)).iteratesAs(sampleValues().e0);
    assertGet(sampleKeys().e0);
    assertEquals(getNumElements() - 1, multimap().size());
  }

  @CollectionSize.Require(SEVERAL)
  @MapFeature.Require(SUPPORTS_PUT)
  public void testAsMapEntrySetReflectsPutSameKey() {
    resetContainer(
        Helpers.mapEntry(sampleKeys().e0, sampleValues().e0),
        Helpers.mapEntry(sampleKeys().e0, sampleValues().e3));
    
    Set<Entry<K, Collection<V>>> asMapEntrySet = multimap().asMap().entrySet();
    Collection<V> valueCollection = Iterables.getOnlyElement(asMapEntrySet).getValue();
    ASSERT.that(valueCollection)
        .has().exactly(sampleValues().e0, sampleValues().e3);
    assertTrue(multimap().put(sampleKeys().e0, sampleValues().e4));
    ASSERT.that(valueCollection)
        .has().exactly(sampleValues().e0, sampleValues().e3, sampleValues().e4);
  }

  @CollectionSize.Require(SEVERAL)
  @MapFeature.Require(SUPPORTS_PUT)
  public void testAsMapEntrySetReflectsPutDifferentKey() {
    resetContainer(
        Helpers.mapEntry(sampleKeys().e0, sampleValues().e0),
        Helpers.mapEntry(sampleKeys().e0, sampleValues().e3));
    
    Set<Entry<K, Collection<V>>> asMapEntrySet = multimap().asMap().entrySet();
    assertTrue(multimap().put(sampleKeys().e1, sampleValues().e4));
    assertEquals(2, asMapEntrySet.size());
  }

  @CollectionSize.Require(SEVERAL)
  @MapFeature.Require({SUPPORTS_PUT, SUPPORTS_REMOVE})
  public void testAsMapEntrySetRemovePropagatesToMultimap() {
    resetContainer(
        Helpers.mapEntry(sampleKeys().e0, sampleValues().e0),
        Helpers.mapEntry(sampleKeys().e0, sampleValues().e3));
    Set<Entry<K, Collection<V>>> asMapEntrySet = multimap().asMap().entrySet();
    Entry<K, Collection<V>> asMapEntry0 = Iterables.getOnlyElement(asMapEntrySet);
    assertTrue(multimap().put(sampleKeys().e1, sampleValues().e4));
    assertTrue(asMapEntrySet.remove(asMapEntry0));
    assertEquals(1, multimap().size());
    ASSERT.that(multimap().keySet()).iteratesAs(sampleKeys().e1);
  }

  @CollectionSize.Require(SEVERAL)
  @CollectionFeature.Require(SUPPORTS_ITERATOR_REMOVE)
  public void testAsMapEntrySetIteratorRemovePropagatesToMultimap() {
    resetContainer(
        Helpers.mapEntry(sampleKeys().e0, sampleValues().e0),
        Helpers.mapEntry(sampleKeys().e0, sampleValues().e3));
    Set<Entry<K, Collection<V>>> asMapEntrySet = multimap().asMap().entrySet();
    Iterator<Entry<K, Collection<V>>> asMapEntryItr = asMapEntrySet.iterator();
    asMapEntryItr.next();
    asMapEntryItr.remove();
    assertTrue(multimap().isEmpty());
  }
}
