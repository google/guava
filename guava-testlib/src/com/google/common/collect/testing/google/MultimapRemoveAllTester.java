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
import static org.truth0.Truth.ASSERT;

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
    ASSERT.that(multimap().removeAll(sampleKeys().e3)).isEmpty();
    expectUnchanged();
  }

  @CollectionSize.Require(absent = ZERO)
  @MapFeature.Require(SUPPORTS_REMOVE)
  public void testRemoveAllPresentKey() {
    ASSERT.that(multimap().removeAll(sampleKeys().e0))
        .has().exactly(sampleValues().e0).inOrder();
    expectMissing(samples.e0);
  }

  @CollectionSize.Require(absent = ZERO)
  @MapFeature.Require(SUPPORTS_REMOVE)
  public void testRemoveAllPropagatesToGet() {
    Collection<V> getResult = multimap().get(sampleKeys().e0);

    multimap().removeAll(sampleKeys().e0);

    ASSERT.that(getResult).isEmpty();
    expectMissing(samples.e0);
  }

  @CollectionSize.Require(SEVERAL)
  @MapFeature.Require(SUPPORTS_REMOVE)
  public void testRemoveAllMultipleValues() {
    resetContainer(
        Helpers.mapEntry(sampleKeys().e0, sampleValues().e0),
        Helpers.mapEntry(sampleKeys().e0, sampleValues().e1),
        Helpers.mapEntry(sampleKeys().e0, sampleValues().e2));

    ASSERT.that(multimap().removeAll(sampleKeys().e0))
        .has().exactly(sampleValues().e0, sampleValues().e1, sampleValues().e2);
    assertTrue(multimap().isEmpty());
  }

  @CollectionSize.Require(absent = ZERO)
  @MapFeature.Require({ SUPPORTS_REMOVE, ALLOWS_NULL_KEYS })
  public void testRemoveAllNullKeyPresent() {
    initMultimapWithNullKey();

    ASSERT.that(multimap().removeAll(null)).has().exactly(getValueForNullKey()).inOrder();

    expectMissing(Helpers.mapEntry((K) null, getValueForNullKey()));
  }

  @MapFeature.Require({ SUPPORTS_REMOVE, ALLOWS_ANY_NULL_QUERIES})
  public void testRemoveAllNullKeyAbsent() {
    ASSERT.that(multimap().removeAll(null)).isEmpty();
    expectUnchanged();
  }
}
