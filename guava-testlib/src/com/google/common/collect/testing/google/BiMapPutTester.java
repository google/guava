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
import static com.google.common.collect.testing.features.MapFeature.SUPPORTS_PUT;

import com.google.common.annotations.GwtCompatible;
import com.google.common.collect.testing.Helpers;
import com.google.common.collect.testing.features.CollectionSize;
import com.google.common.collect.testing.features.MapFeature;

/**
 * Tester for {@code BiMap.put} and {@code BiMap.forcePut}.
 */
@GwtCompatible
public class BiMapPutTester<K, V> extends AbstractBiMapTester<K, V> {
  
  @SuppressWarnings("unchecked")
  @MapFeature.Require(SUPPORTS_PUT)
  @CollectionSize.Require(ZERO)
  public void testPutWithSameValueFails() {
    K k0 = samples.e0.getKey();
    K k1 = samples.e1.getKey();
    V v0 = samples.e0.getValue();
    getMap().put(k0, v0);
    try {
      getMap().put(k1, v0);
      fail("Expected IllegalArgumentException");
    } catch (IllegalArgumentException expected) {
      // success
    }
    // verify that the bimap is unchanged
    expectAdded(samples.e0);
  }

  @SuppressWarnings("unchecked")
  @MapFeature.Require(SUPPORTS_PUT)
  @CollectionSize.Require(ZERO)
  public void testPutPresentKeyDifferentValue() {
    K k0 = samples.e0.getKey();
    V v0 = samples.e0.getValue();
    V v1 = samples.e1.getValue();
    getMap().put(k0, v0);
    getMap().put(k0, v1);
    // verify that the bimap is changed, and that the old inverse mapping 
    // from v1 -> v0 is deleted
    expectContents(Helpers.mapEntry(k0, v1));
  }
  
  @SuppressWarnings("unchecked")
  @MapFeature.Require(SUPPORTS_PUT)
  @CollectionSize.Require(ZERO)
  public void putDistinctKeysDistinctValues() {
    getMap().put(samples.e0.getKey(), samples.e0.getValue());
    getMap().put(samples.e1.getKey(), samples.e1.getValue());
    expectAdded(samples.e0, samples.e1);
  }

  @SuppressWarnings("unchecked")
  @MapFeature.Require(SUPPORTS_PUT)
  @CollectionSize.Require(ZERO)
  public void testForcePutOverwritesOldValueEntry() {
    K k0 = samples.e0.getKey();
    K k1 = samples.e1.getKey();
    V v0 = samples.e0.getValue();
    getMap().put(k0, v0);
    getMap().forcePut(k1, v0);
    // verify that the bimap is unchanged
    expectAdded(Helpers.mapEntry(k1, v0));
  }
  
  @SuppressWarnings("unchecked")
  @MapFeature.Require(SUPPORTS_PUT)
  @CollectionSize.Require(ZERO)
  public void testInversePut() {
    K k0 = samples.e0.getKey();
    V v0 = samples.e0.getValue();
    K k1 = samples.e1.getKey();
    V v1 = samples.e1.getValue();
    getMap().put(k0, v0);
    getMap().inverse().put(v1, k1);
    expectAdded(samples.e0, samples.e1);
  }
}
