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
import static com.google.common.collect.testing.features.CollectionSize.ONE;
import static com.google.common.collect.testing.features.CollectionSize.SEVERAL;
import static com.google.common.collect.testing.features.MapFeature.ALLOWS_NULL_KEYS;
import static com.google.common.collect.testing.features.MapFeature.ALLOWS_NULL_KEY_QUERIES;
import static com.google.common.collect.testing.features.MapFeature.SUPPORTS_REMOVE;
import static org.truth0.Truth.ASSERT;

import com.google.common.annotations.GwtCompatible;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multiset;
import com.google.common.collect.Multisets;
import com.google.common.collect.testing.Helpers;
import com.google.common.collect.testing.features.CollectionFeature;
import com.google.common.collect.testing.features.CollectionSize;
import com.google.common.collect.testing.features.MapFeature;

import java.util.Iterator;

/**
 * Tester for {@code Multimap.entries}.
 *
 * @author Louis Wasserman
 */
@GwtCompatible
public class MultimapKeysTester<K, V> extends AbstractMultimapTester<K, V, Multimap<K, V>> {
  @CollectionSize.Require(SEVERAL)
  public void testKeys() {
    resetContainer(
        Helpers.mapEntry(sampleKeys().e0, sampleValues().e0),
        Helpers.mapEntry(sampleKeys().e0, sampleValues().e1),
        Helpers.mapEntry(sampleKeys().e1, sampleValues().e0));
    Multiset<K> keys = multimap().keys();
    assertEquals(2, keys.count(sampleKeys().e0));
    assertEquals(1, keys.count(sampleKeys().e1));
    assertEquals(3, keys.size());
    ASSERT.that(keys).has().allOf(sampleKeys().e0, sampleKeys().e1);
    ASSERT.that(keys.entrySet()).has().allOf(
        Multisets.immutableEntry(sampleKeys().e0, 2),
        Multisets.immutableEntry(sampleKeys().e1, 1));
  }
  
  @MapFeature.Require(ALLOWS_NULL_KEY_QUERIES)
  public void testKeysCountAbsentNullKey() {
    assertEquals(0, multimap().keys().count(null));
  }
  
  @CollectionSize.Require(SEVERAL)
  @MapFeature.Require(ALLOWS_NULL_KEYS)
  public void testKeysWithNullKey() {
    resetContainer(
        Helpers.mapEntry((K) null, sampleValues().e0),
        Helpers.mapEntry((K) null, sampleValues().e1),
        Helpers.mapEntry(sampleKeys().e1, sampleValues().e0));
    Multiset<K> keys = multimap().keys();
    assertEquals(2, keys.count(null));
    assertEquals(1, keys.count(sampleKeys().e1));
    assertEquals(3, keys.size());
    ASSERT.that(keys).has().allOf(null, sampleKeys().e1);
    ASSERT.that(keys.entrySet()).has().allOf(
        Multisets.immutableEntry((K) null, 2),
        Multisets.immutableEntry(sampleKeys().e1, 1));
  }
  
  public void testKeysElementSet() {
    assertEquals(multimap().keySet(), multimap().keys().elementSet());
  }

  @MapFeature.Require(SUPPORTS_REMOVE)
  public void testKeysRemove() {
    int original = multimap().keys().remove(sampleKeys().e0, 1);
    assertEquals(Math.max(original - 1, 0), multimap().get(sampleKeys().e0).size());
  }
  
  @CollectionSize.Require(ONE)
  @CollectionFeature.Require(SUPPORTS_ITERATOR_REMOVE)
  public void testKeysEntrySetIteratorRemove() {
    Multiset<K> keys = multimap().keys();
    Iterator<Multiset.Entry<K>> itr = keys.entrySet().iterator();
    assertEquals(Multisets.immutableEntry(sampleKeys().e0, 1),
        itr.next());
    itr.remove();
    assertTrue(multimap().isEmpty());
  }
  
  @CollectionSize.Require(SEVERAL)
  @MapFeature.Require(SUPPORTS_REMOVE)
  public void testKeysEntrySetRemove() {
    resetContainer(
        Helpers.mapEntry(sampleKeys().e0, sampleValues().e0),
        Helpers.mapEntry(sampleKeys().e0, sampleValues().e1),
        Helpers.mapEntry(sampleKeys().e1, sampleValues().e0));
    assertTrue(multimap().keys().entrySet().remove(
        Multisets.immutableEntry(sampleKeys().e0, 2)));
    assertEquals(1, multimap().size());
    assertTrue(multimap().containsEntry(sampleKeys().e1, sampleValues().e0));
  }
}
