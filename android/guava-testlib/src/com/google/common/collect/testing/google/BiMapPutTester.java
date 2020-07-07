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

import static com.google.common.collect.testing.features.CollectionSize.ONE;
import static com.google.common.collect.testing.features.CollectionSize.SEVERAL;
import static com.google.common.collect.testing.features.CollectionSize.ZERO;
import static com.google.common.collect.testing.features.MapFeature.ALLOWS_NULL_KEYS;
import static com.google.common.collect.testing.features.MapFeature.ALLOWS_NULL_VALUES;
import static com.google.common.collect.testing.features.MapFeature.SUPPORTS_PUT;

import com.google.common.annotations.GwtCompatible;
import com.google.common.collect.testing.Helpers;
import com.google.common.collect.testing.features.CollectionSize;
import com.google.common.collect.testing.features.MapFeature;
import org.junit.Ignore;

/** Tester for {@code BiMap.put} and {@code BiMap.forcePut}. */
@GwtCompatible
@Ignore // Affects only Android test runner, which respects JUnit 4 annotations on JUnit 3 tests.
public class BiMapPutTester<K, V> extends AbstractBiMapTester<K, V> {

  @SuppressWarnings("unchecked")
  @MapFeature.Require(SUPPORTS_PUT)
  @CollectionSize.Require(ZERO)
  public void testPutWithSameValueFails() {
    getMap().put(k0(), v0());
    try {
      getMap().put(k1(), v0());
      fail("Expected IllegalArgumentException");
    } catch (IllegalArgumentException expected) {
      // success
    }
    // verify that the bimap is unchanged
    expectAdded(e0());
  }

  @SuppressWarnings("unchecked")
  @MapFeature.Require(SUPPORTS_PUT)
  @CollectionSize.Require(ZERO)
  public void testPutPresentKeyDifferentValue() {
    getMap().put(k0(), v0());
    getMap().put(k0(), v1());
    // verify that the bimap is changed, and that the old inverse mapping
    // from v1 -> v0 is deleted
    expectContents(Helpers.mapEntry(k0(), v1()));
  }

  @SuppressWarnings("unchecked")
  @MapFeature.Require(SUPPORTS_PUT)
  @CollectionSize.Require(ZERO)
  public void putDistinctKeysDistinctValues() {
    getMap().put(k0(), v0());
    getMap().put(k1(), v1());
    expectAdded(e0(), e1());
  }

  @SuppressWarnings("unchecked")
  @MapFeature.Require(SUPPORTS_PUT)
  @CollectionSize.Require(ONE)
  public void testForcePutKeyPresent() {
    getMap().forcePut(k0(), v1());
    expectContents(Helpers.mapEntry(k0(), v1()));
    assertFalse(getMap().containsValue(v0()));
    assertNull(getMap().inverse().get(v0()));
    assertEquals(1, getMap().size());
    assertTrue(getMap().containsKey(k0()));
  }

  @SuppressWarnings("unchecked")
  @MapFeature.Require(SUPPORTS_PUT)
  @CollectionSize.Require(ONE)
  public void testForcePutValuePresent() {
    getMap().forcePut(k1(), v0());
    expectContents(Helpers.mapEntry(k1(), v0()));
    assertEquals(k1(), getMap().inverse().get(v0()));
    assertEquals(1, getMap().size());
    assertFalse(getMap().containsKey(k0()));
  }

  @SuppressWarnings("unchecked")
  @MapFeature.Require(SUPPORTS_PUT)
  @CollectionSize.Require(SEVERAL)
  public void testForcePutKeyAndValuePresent() {
    getMap().forcePut(k0(), v1());
    expectContents(Helpers.mapEntry(k0(), v1()), Helpers.mapEntry(k2(), v2()));
    assertEquals(2, getMap().size());
    assertFalse(getMap().containsKey(k1()));
    assertFalse(getMap().containsValue(v0()));
  }

  @SuppressWarnings("unchecked")
  @MapFeature.Require({SUPPORTS_PUT, ALLOWS_NULL_KEYS})
  @CollectionSize.Require(ONE)
  public void testForcePutNullKeyPresent() {
    initMapWithNullKey();

    getMap().forcePut(null, v1());

    expectContents(Helpers.mapEntry((K) null, v1()));

    assertFalse(getMap().containsValue(v0()));

    assertTrue(getMap().containsValue(v1()));
    assertTrue(getMap().inverse().containsKey(v1()));
    assertNull(getMap().inverse().get(v1()));
    assertEquals(v1(), getMap().get(null));
    assertEquals(1, getMap().size());
  }

  @SuppressWarnings("unchecked")
  @MapFeature.Require({SUPPORTS_PUT, ALLOWS_NULL_VALUES})
  @CollectionSize.Require(ONE)
  public void testForcePutNullValuePresent() {
    initMapWithNullValue();

    getMap().forcePut(k1(), null);

    expectContents(Helpers.mapEntry(k1(), (V) null));

    assertFalse(getMap().containsKey(k0()));

    assertTrue(getMap().containsKey(k1()));
    assertTrue(getMap().inverse().containsKey(null));
    assertNull(getMap().get(k1()));
    assertEquals(k1(), getMap().inverse().get(null));
    assertEquals(1, getMap().size());
  }

  // nb: inverse is run through its own entire suite

  @SuppressWarnings("unchecked")
  @MapFeature.Require(SUPPORTS_PUT)
  @CollectionSize.Require(ZERO)
  public void testInversePut() {
    getMap().put(k0(), v0());
    getMap().inverse().put(v1(), k1());
    expectAdded(e0(), e1());
  }
}
