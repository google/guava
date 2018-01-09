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

import static com.google.common.collect.testing.features.MapFeature.SUPPORTS_REMOVE;

import com.google.common.annotations.GwtCompatible;
import com.google.common.collect.BiMap;
import com.google.common.collect.testing.features.MapFeature;
import org.junit.Ignore;

/**
 * Tester for {@code BiMap.clear}.
 *
 * @author Louis Wasserman
 */
@GwtCompatible
@Ignore // Affects only Android test runner, which respects JUnit 4 annotations on JUnit 3 tests.
public class BiMapClearTester<K, V> extends AbstractBiMapTester<K, V> {
  @MapFeature.Require(SUPPORTS_REMOVE)
  public void testClearClearsInverse() {
    BiMap<V, K> inv = getMap().inverse();
    getMap().clear();
    assertTrue(getMap().isEmpty());
    assertTrue(inv.isEmpty());
  }

  @MapFeature.Require(SUPPORTS_REMOVE)
  public void testKeySetClearClearsInverse() {
    BiMap<V, K> inv = getMap().inverse();
    getMap().keySet().clear();
    assertTrue(getMap().isEmpty());
    assertTrue(inv.isEmpty());
  }

  @MapFeature.Require(SUPPORTS_REMOVE)
  public void testValuesClearClearsInverse() {
    BiMap<V, K> inv = getMap().inverse();
    getMap().values().clear();
    assertTrue(getMap().isEmpty());
    assertTrue(inv.isEmpty());
  }

  @MapFeature.Require(SUPPORTS_REMOVE)
  public void testClearInverseClears() {
    BiMap<V, K> inv = getMap().inverse();
    inv.clear();
    assertTrue(getMap().isEmpty());
    assertTrue(inv.isEmpty());
  }

  @MapFeature.Require(SUPPORTS_REMOVE)
  public void testClearInverseKeySetClears() {
    BiMap<V, K> inv = getMap().inverse();
    inv.keySet().clear();
    assertTrue(getMap().isEmpty());
    assertTrue(inv.isEmpty());
  }

  @MapFeature.Require(SUPPORTS_REMOVE)
  public void testClearInverseValuesClears() {
    BiMap<V, K> inv = getMap().inverse();
    inv.values().clear();
    assertTrue(getMap().isEmpty());
    assertTrue(inv.isEmpty());
  }
}
