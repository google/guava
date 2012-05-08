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

import static com.google.common.collect.testing.Helpers.assertContentsAnyOrder;
import static com.google.common.collect.testing.features.CollectionSize.ZERO;
import static com.google.common.collect.testing.features.MapFeature.ALLOWS_NULL_KEYS;
import static com.google.common.collect.testing.features.MapFeature.ALLOWS_NULL_QUERIES;
import static com.google.common.collect.testing.features.MapFeature.ALLOWS_NULL_VALUES;
import static com.google.common.collect.testing.features.MapFeature.SUPPORTS_PUT;
import static com.google.common.collect.testing.features.MapFeature.SUPPORTS_REMOVE;
import static org.junit.contrib.truth.Truth.ASSERT;

import com.google.common.annotations.GwtCompatible;
import com.google.common.collect.Multimap;
import com.google.common.collect.testing.features.CollectionSize;
import com.google.common.collect.testing.features.MapFeature;

import java.util.Collection;

/**
 * Tests for {@link Multimap#get(Object)}.
 *
 * @author Louis Wasserman
 */
@GwtCompatible
public class MultimapGetTester<K, V> extends AbstractMultimapTester<K, V> {
  public void testGetEmpty() {
    Collection<V> result = multimap().get(sampleKeys().e3);
    assertTrue(result.isEmpty());
    assertEquals(0, result.size());
  }

  @CollectionSize.Require(absent = ZERO)
  public void testGetNonEmpty() {
    Collection<V> result = multimap().get(sampleKeys().e0);
    assertFalse(result.isEmpty());
    assertContentsAnyOrder(result, sampleValues().e0);
  }

  @CollectionSize.Require(absent = ZERO)
  @MapFeature.Require(SUPPORTS_REMOVE)
  public void testGetPropagatesRemove() {
    Collection<V> result = multimap().get(sampleKeys().e0);
    assertTrue(result.remove(sampleValues().e0));
    assertFalse(multimap().containsKey(sampleKeys().e0));
    assertTrue(result.isEmpty());
    assertTrue(multimap().get(sampleKeys().e0).isEmpty());
  }

  @CollectionSize.Require(absent = ZERO)
  @MapFeature.Require({ SUPPORTS_REMOVE, SUPPORTS_PUT })
  public void testGetRemoveThenAddPropagates() {
    int oldSize = getNumElements();

    K k0 = sampleKeys().e0;
    V v0 = sampleValues().e0;

    Collection<V> result = multimap().get(k0);
    assertTrue(result.remove(v0));

    assertFalse(multimap().containsKey(k0));
    assertFalse(multimap().containsEntry(k0, v0));
    ASSERT.that(result).isEmpty();

    V v1 = sampleValues().e1;
    V v2 = sampleValues().e2;

    assertTrue(result.add(v1));
    assertTrue(result.add(v2));

    ASSERT.that(result).hasContentsAnyOrder(v1, v2);
    ASSERT.that(multimap().get(k0)).hasContentsAnyOrder(v1, v2);
    assertTrue(multimap().containsKey(k0));
    assertFalse(multimap().containsEntry(k0, v0));
    assertTrue(multimap().containsEntry(k0, v2));
    assertEquals(oldSize + 1, multimap().size());
  }

  @MapFeature.Require(ALLOWS_NULL_KEYS)
  @CollectionSize.Require(absent = ZERO)
  public void testGetNullPresent() {
    initMultimapWithNullKey();
    ASSERT.that(multimap().get(null)).hasContentsInOrder(getValueForNullKey());
  }

  @MapFeature.Require(ALLOWS_NULL_QUERIES)
  public void testGetNullAbsent() {
    ASSERT.that(multimap().get(null)).isEmpty();
  }

  @MapFeature.Require(absent = ALLOWS_NULL_QUERIES)
  public void testGetNullForbidden() {
    try {
      multimap().get(null);
      fail("Expected NullPointerException");
    } catch (NullPointerException expected) {
      // success
    }
  }

  @MapFeature.Require(ALLOWS_NULL_VALUES)
  @CollectionSize.Require(absent = ZERO)
  public void testGetWithNullValue() {
    initMultimapWithNullValue();
    ASSERT.that(multimap().get(getKeyForNullValue()))
        .hasContentsInOrder((V) null);
  }
}
