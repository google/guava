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
import static com.google.common.collect.testing.features.MapFeature.ALLOWS_NULL_QUERIES;
import static com.google.common.collect.testing.features.MapFeature.ALLOWS_NULL_VALUES;
import static com.google.common.collect.testing.features.MapFeature.SUPPORTS_REMOVE;

import com.google.common.annotations.GwtCompatible;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.testing.features.CollectionSize;
import com.google.common.collect.testing.features.MapFeature;

import java.util.Collection;

/**
 * Tests for {@link Multimap#remove(Object, Object)}.
 *
 * @author Louis Wasserman
 */
@GwtCompatible
public class MultimapRemoveEntryTester<K, V> extends AbstractMultimapTester<K, V> {
  private static final Object[] EMPTY = new Object[0];

  @MapFeature.Require(SUPPORTS_REMOVE)
  public void testRemoveAbsent() {
    assertFalse(multimap().remove(sampleKeys().e0, sampleValues().e1));
    expectUnchanged();
  }

  @CollectionSize.Require(absent = ZERO)
  @MapFeature.Require(SUPPORTS_REMOVE)
  public void testRemovePropagatesToGet() {
    Collection<V> result = multimap().get(sampleKeys().e0);
    assertTrue(multimap().remove(sampleKeys().e0, sampleValues().e0));
    assertTrue(result.isEmpty());
    assertFalse(multimap().containsKey(sampleKeys().e0));
  }

  @CollectionSize.Require(absent = ZERO)
  @MapFeature.Require(SUPPORTS_REMOVE)
  public void testRemovePresent() {
    assertTrue(multimap().remove(sampleKeys().e0, sampleValues().e0));

    expectMissing(samples.e0);
    assertGet(sampleKeys().e0, EMPTY);
  }

  @CollectionSize.Require(absent = ZERO)
  @MapFeature.Require({ SUPPORTS_REMOVE, ALLOWS_NULL_KEYS })
  public void testRemoveNullKeyPresent() {
    initMultimapWithNullKey();

    assertTrue(multimap().remove(null, getValueForNullKey()));

    expectMissing(Maps.immutableEntry((K) null, getValueForNullKey()));
    assertGet(getKeyForNullValue(), EMPTY);
  }

  @CollectionSize.Require(absent = ZERO)
  @MapFeature.Require({ SUPPORTS_REMOVE, ALLOWS_NULL_VALUES })
  public void testRemoveNullValuePresent() {
    initMultimapWithNullValue();

    assertTrue(multimap().remove(getKeyForNullValue(), null));

    expectMissing(Maps.immutableEntry(getKeyForNullValue(), (V) null));
    assertGet(getKeyForNullValue(), EMPTY);
  }

  @MapFeature.Require({ SUPPORTS_REMOVE, ALLOWS_NULL_QUERIES})
  public void testRemoveNullKeyAbsent() {
    assertFalse(multimap().remove(null, sampleValues().e0));
    expectUnchanged();
  }

  @MapFeature.Require({ SUPPORTS_REMOVE, ALLOWS_NULL_QUERIES})
  public void testRemoveNullValueAbsent() {
    assertFalse(multimap().remove(sampleKeys().e0, null));
    expectUnchanged();
  }

  @MapFeature.Require(value = SUPPORTS_REMOVE, absent = ALLOWS_NULL_QUERIES)
  public void testRemoveNullValueForbidden() {
    try {
      multimap().remove(sampleKeys().e0, null);
      fail("Expected NullPointerException");
    } catch (NullPointerException expected) {
      // success
    }
    expectUnchanged();
  }

  @MapFeature.Require(value = SUPPORTS_REMOVE, absent = ALLOWS_NULL_QUERIES)
  public void testRemoveNullKeyForbidden() {
    try {
      multimap().remove(null, sampleValues().e0);
      fail("Expected NullPointerException");
    } catch (NullPointerException expected) {
      // success
    }
    expectUnchanged();
  }
}
