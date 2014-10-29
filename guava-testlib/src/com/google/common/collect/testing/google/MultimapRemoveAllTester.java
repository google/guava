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

import static com.google.common.collect.testing.features.CollectionSize.SEVERAL;
import static com.google.common.collect.testing.features.CollectionSize.ZERO;
import static com.google.common.collect.testing.features.MapFeature.ALLOWS_ANY_NULL_QUERIES;
import static com.google.common.collect.testing.features.MapFeature.ALLOWS_NULL_KEYS;
import static com.google.common.collect.testing.features.MapFeature.SUPPORTS_REMOVE;
import static com.google.common.truth.Truth.assertThat;

import com.google.common.annotations.GwtCompatible;
import com.google.common.collect.Multimap;
import com.google.common.collect.testing.Helpers;
import com.google.common.collect.testing.features.CollectionSize;
import com.google.common.collect.testing.features.MapFeature;

import java.util.Collection;

/**
 * Tests for {@link Multimap#removeAll(Object)}.
 *
 * @author Louis Wasserman
 */
@GwtCompatible
public class MultimapRemoveAllTester<K, V> extends AbstractMultimapTester<K, V, Multimap<K, V>> {
  @MapFeature.Require(SUPPORTS_REMOVE)
  public void testRemoveAllAbsentKey() {
    assertThat(multimap().removeAll(k3())).isEmpty();
    expectUnchanged();
  }

  @CollectionSize.Require(absent = ZERO)
  @MapFeature.Require(SUPPORTS_REMOVE)
  public void testRemoveAllPresentKey() {
    assertThat(multimap().removeAll(k0())).containsExactly(v0());
    expectMissing(e0());
  }

  @CollectionSize.Require(absent = ZERO)
  @MapFeature.Require(SUPPORTS_REMOVE)
  public void testRemoveAllPropagatesToGet() {
    Collection<V> getResult = multimap().get(k0());

    multimap().removeAll(k0());

    assertThat(getResult).isEmpty();
    expectMissing(e0());
  }

  @CollectionSize.Require(SEVERAL)
  @MapFeature.Require(SUPPORTS_REMOVE)
  public void testRemoveAllMultipleValues() {
    resetContainer(
        Helpers.mapEntry(k0(), v0()),
        Helpers.mapEntry(k0(), v1()),
        Helpers.mapEntry(k0(), v2()));

    assertThat(multimap().removeAll(k0()))
        .containsExactly(v0(), v1(), v2());
    assertTrue(multimap().isEmpty());
  }

  @CollectionSize.Require(absent = ZERO)
  @MapFeature.Require({ SUPPORTS_REMOVE, ALLOWS_NULL_KEYS })
  public void testRemoveAllNullKeyPresent() {
    initMultimapWithNullKey();

    assertThat(multimap().removeAll(null)).containsExactly(getValueForNullKey());

    expectMissing(Helpers.mapEntry((K) null, getValueForNullKey()));
  }

  @MapFeature.Require({ SUPPORTS_REMOVE, ALLOWS_ANY_NULL_QUERIES})
  public void testRemoveAllNullKeyAbsent() {
    assertThat(multimap().removeAll(null)).isEmpty();
    expectUnchanged();
  }
}
