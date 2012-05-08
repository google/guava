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

import com.google.common.annotations.GwtCompatible;
import com.google.common.collect.Multimap;
import com.google.common.collect.testing.Helpers;
import com.google.common.collect.testing.features.CollectionSize;
import com.google.common.collect.testing.features.MapFeature;

import java.util.List;

/**
 * Tester for {@link Multimap#put}.
 *
 * @author Louis Wasserman
 */
@GwtCompatible
public class MultimapPutTester<K, V> extends AbstractMultimapTester<K, V> {
  private static final Object[] EMPTY = new Object[0];

  @MapFeature.Require(SUPPORTS_PUT)
  public void testPutEmpty() {
    int size = getNumElements();

    K key = sampleKeys().e3;
    V value = sampleValues().e3;

    assertGet(key, EMPTY);

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

    assertGet(key, values.toArray());
    assertEquals(size + 2, multimap().size());
  }

  @MapFeature.Require({SUPPORTS_PUT, ALLOWS_NULL_VALUES})
  public void testPutNullValue() {
    int size = getNumElements();

    multimap().put(sampleKeys().e3, null);

    assertGet(sampleKeys().e3, new Object[] {null});
    assertEquals(size + 1, multimap().size());
  }

  @MapFeature.Require({SUPPORTS_PUT, ALLOWS_NULL_KEYS})
  public void testPutNullKey() {
    int size = getNumElements();

    multimap().put(null, sampleValues().e3);

    assertGet(null, sampleValues().e3);
    assertEquals(size + 1, multimap().size());
  }
}
