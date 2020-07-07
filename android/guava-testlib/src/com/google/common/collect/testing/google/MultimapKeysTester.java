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

import static com.google.common.collect.testing.Helpers.assertContainsAllOf;
import static com.google.common.collect.testing.features.CollectionFeature.SUPPORTS_ITERATOR_REMOVE;
import static com.google.common.collect.testing.features.CollectionSize.ONE;
import static com.google.common.collect.testing.features.CollectionSize.SEVERAL;
import static com.google.common.collect.testing.features.MapFeature.ALLOWS_NULL_KEYS;
import static com.google.common.collect.testing.features.MapFeature.ALLOWS_NULL_KEY_QUERIES;
import static com.google.common.collect.testing.features.MapFeature.SUPPORTS_REMOVE;

import com.google.common.annotations.GwtCompatible;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multiset;
import com.google.common.collect.Multisets;
import com.google.common.collect.testing.Helpers;
import com.google.common.collect.testing.features.CollectionFeature;
import com.google.common.collect.testing.features.CollectionSize;
import com.google.common.collect.testing.features.MapFeature;
import java.util.Iterator;
import org.junit.Ignore;

/**
 * Tester for {@code Multimap.entries}.
 *
 * @author Louis Wasserman
 */
@GwtCompatible
@Ignore // Affects only Android test runner, which respects JUnit 4 annotations on JUnit 3 tests.
public class MultimapKeysTester<K, V> extends AbstractMultimapTester<K, V, Multimap<K, V>> {
  @CollectionSize.Require(SEVERAL)
  public void testKeys() {
    resetContainer(
        Helpers.mapEntry(k0(), v0()), Helpers.mapEntry(k0(), v1()), Helpers.mapEntry(k1(), v0()));
    Multiset<K> keys = multimap().keys();
    assertEquals(2, keys.count(k0()));
    assertEquals(1, keys.count(k1()));
    assertEquals(3, keys.size());
    assertContainsAllOf(keys, k0(), k1());
    assertContainsAllOf(
        keys.entrySet(), Multisets.immutableEntry(k0(), 2), Multisets.immutableEntry(k1(), 1));
  }

  @MapFeature.Require(ALLOWS_NULL_KEY_QUERIES)
  public void testKeysCountAbsentNullKey() {
    assertEquals(0, multimap().keys().count(null));
  }

  @CollectionSize.Require(SEVERAL)
  @MapFeature.Require(ALLOWS_NULL_KEYS)
  public void testKeysWithNullKey() {
    resetContainer(
        Helpers.mapEntry((K) null, v0()),
        Helpers.mapEntry((K) null, v1()),
        Helpers.mapEntry(k1(), v0()));
    Multiset<K> keys = multimap().keys();
    assertEquals(2, keys.count(null));
    assertEquals(1, keys.count(k1()));
    assertEquals(3, keys.size());
    assertContainsAllOf(keys, null, k1());
    assertContainsAllOf(
        keys.entrySet(), Multisets.immutableEntry((K) null, 2), Multisets.immutableEntry(k1(), 1));
  }

  public void testKeysElementSet() {
    assertEquals(multimap().keySet(), multimap().keys().elementSet());
  }

  @MapFeature.Require(SUPPORTS_REMOVE)
  public void testKeysRemove() {
    int original = multimap().keys().remove(k0(), 1);
    assertEquals(Math.max(original - 1, 0), multimap().get(k0()).size());
  }

  @CollectionSize.Require(ONE)
  @CollectionFeature.Require(SUPPORTS_ITERATOR_REMOVE)
  public void testKeysEntrySetIteratorRemove() {
    Multiset<K> keys = multimap().keys();
    Iterator<Multiset.Entry<K>> itr = keys.entrySet().iterator();
    assertEquals(Multisets.immutableEntry(k0(), 1), itr.next());
    itr.remove();
    assertTrue(multimap().isEmpty());
  }

  @CollectionSize.Require(SEVERAL)
  @MapFeature.Require(SUPPORTS_REMOVE)
  public void testKeysEntrySetRemove() {
    resetContainer(
        Helpers.mapEntry(k0(), v0()), Helpers.mapEntry(k0(), v1()), Helpers.mapEntry(k1(), v0()));
    assertTrue(multimap().keys().entrySet().remove(Multisets.immutableEntry(k0(), 2)));
    assertEquals(1, multimap().size());
    assertTrue(multimap().containsEntry(k1(), v0()));
  }
}
