/*
 * Copyright (C) 2015 The Guava Authors
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

import static com.google.common.collect.testing.features.CollectionSize.ZERO;
import static com.google.common.collect.testing.features.MapFeature.ALLOWS_NULL_KEYS;
import static com.google.common.collect.testing.features.MapFeature.ALLOWS_NULL_KEY_QUERIES;
import static com.google.common.collect.testing.features.MapFeature.ALLOWS_NULL_VALUES;

import com.google.common.annotations.GwtCompatible;
import com.google.common.collect.testing.AbstractMapTester;
import com.google.common.collect.testing.WrongType;
import com.google.common.collect.testing.features.CollectionSize;
import com.google.common.collect.testing.features.MapFeature;
import java.util.Map;
import org.junit.Ignore;

/**
 * A generic JUnit test which tests {@link Map#getOrDefault}. Can't be invoked directly; please see
 * {@link com.google.common.collect.testing.MapTestSuiteBuilder}.
 *
 * @author Louis Wasserman
 */
@GwtCompatible
@Ignore // Affects only Android test runner, which respects JUnit 4 annotations on JUnit 3 tests.
public class MapGetOrDefaultTester<K, V> extends AbstractMapTester<K, V> {
  @CollectionSize.Require(absent = ZERO)
  public void testGetOrDefault_present() {
    assertEquals(
        "getOrDefault(present, def) should return the associated value",
        v0(),
        getMap().getOrDefault(k0(), v3()));
  }

  @CollectionSize.Require(absent = ZERO)
  public void testGetOrDefault_presentNullDefault() {
    assertEquals(
        "getOrDefault(present, null) should return the associated value",
        v0(),
        getMap().getOrDefault(k0(), null));
  }

  public void testGetOrDefault_absent() {
    assertEquals(
        "getOrDefault(absent, def) should return the default value",
        v3(),
        getMap().getOrDefault(k3(), v3()));
  }

  public void testGetOrDefault_absentNullDefault() {
    assertNull("getOrDefault(absent, null) should return null", getMap().getOrDefault(k3(), null));
  }

  @MapFeature.Require(ALLOWS_NULL_KEY_QUERIES)
  public void testGetOrDefault_absentNull() {
    assertEquals(
        "getOrDefault(null, def) should return the default value",
        v3(),
        getMap().getOrDefault(null, v3()));
  }

  @MapFeature.Require(absent = ALLOWS_NULL_KEY_QUERIES)
  public void testGetOrDefault_nullAbsentAndUnsupported() {
    try {
      assertEquals(
          "getOrDefault(null, def) should return default or throw",
          v3(),
          getMap().getOrDefault(null, v3()));
    } catch (NullPointerException tolerated) {
    }
  }

  @MapFeature.Require(ALLOWS_NULL_KEYS)
  @CollectionSize.Require(absent = ZERO)
  public void testGetOrDefault_nonNullWhenNullContained() {
    initMapWithNullKey();
    assertEquals(
        "getOrDefault(absent, default) should return default",
        v3(),
        getMap().getOrDefault(k3(), v3()));
  }

  @MapFeature.Require(ALLOWS_NULL_KEYS)
  @CollectionSize.Require(absent = ZERO)
  public void testGetOrDefault_presentNull() {
    initMapWithNullKey();
    assertEquals(
        "getOrDefault(null, default) should return the associated value",
        getValueForNullKey(),
        getMap().getOrDefault(null, v3()));
  }

  @MapFeature.Require(ALLOWS_NULL_VALUES)
  @CollectionSize.Require(absent = ZERO)
  public void testGetOrDefault_presentMappedToNull() {
    initMapWithNullValue();
    assertNull(
        "getOrDefault(mappedToNull, default) should return null",
        getMap().getOrDefault(getKeyForNullValue(), v3()));
  }

  public void testGet_wrongType() {
    try {
      assertEquals(
          "getOrDefault(wrongType, default) should return default or throw",
          v3(),
          getMap().getOrDefault(WrongType.VALUE, v3()));
    } catch (ClassCastException tolerated) {
    }
  }
}
