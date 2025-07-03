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

import static com.google.common.collect.testing.Helpers.assertContains;
import static com.google.common.collect.testing.Helpers.assertContentsAnyOrder;
import static com.google.common.collect.testing.Helpers.assertEmpty;
import static com.google.common.collect.testing.Helpers.mapEntry;
import static com.google.common.collect.testing.features.CollectionSize.SEVERAL;
import static com.google.common.collect.testing.features.CollectionSize.ZERO;
import static com.google.common.collect.testing.features.MapFeature.ALLOWS_NULL_KEYS;
import static com.google.common.collect.testing.features.MapFeature.ALLOWS_NULL_KEY_QUERIES;
import static com.google.common.collect.testing.features.MapFeature.ALLOWS_NULL_VALUES;
import static com.google.common.collect.testing.features.MapFeature.SUPPORTS_PUT;
import static com.google.common.collect.testing.features.MapFeature.SUPPORTS_REMOVE;
import static com.google.common.collect.testing.google.ReflectionFreeAssertThrows.assertThrows;
import static java.util.Collections.singletonList;

import com.google.common.annotations.GwtCompatible;
import com.google.common.collect.Multimap;
import com.google.common.collect.testing.features.CollectionSize;
import com.google.common.collect.testing.features.MapFeature;
import java.util.Collection;
import org.junit.Ignore;

/**
 * Tests for {@link Multimap#get(Object)}.
 *
 * @author Louis Wasserman
 */
@GwtCompatible
@Ignore("test runners must not instantiate and run this directly, only via suites we build")
// @Ignore affects the Android test runner, which respects JUnit 4 annotations on JUnit 3 tests.
@SuppressWarnings("JUnit4ClassUsedInJUnit3")
public class MultimapGetTester<K, V> extends AbstractMultimapTester<K, V, Multimap<K, V>> {
  public void testGetEmpty() {
    Collection<V> result = multimap().get(k3());
    assertEmpty(result);
    assertEquals(0, result.size());
  }

  @CollectionSize.Require(absent = ZERO)
  public void testGetNonEmpty() {
    Collection<V> result = multimap().get(k0());
    assertFalse(result.isEmpty());
    assertContentsAnyOrder(result, v0());
  }

  @CollectionSize.Require(SEVERAL)
  public void testGetMultiple() {
    resetContainer(mapEntry(k0(), v0()), mapEntry(k0(), v1()), mapEntry(k0(), v2()));
    assertGet(k0(), v0(), v1(), v2());
  }

  public void testGetAbsentKey() {
    assertGet(k4());
  }

  @CollectionSize.Require(SEVERAL)
  @MapFeature.Require(SUPPORTS_REMOVE)
  public void testPropagatesRemoveToMultimap() {
    resetContainer(mapEntry(k0(), v0()), mapEntry(k0(), v3()), mapEntry(k0(), v2()));
    Collection<V> result = multimap().get(k0());
    assertTrue(result.remove(v0()));
    assertFalse(multimap().containsEntry(k0(), v0()));
    assertEquals(2, multimap().size());
  }

  @CollectionSize.Require(absent = ZERO)
  @MapFeature.Require(SUPPORTS_REMOVE)
  public void testPropagatesRemoveLastElementToMultimap() {
    Collection<V> result = multimap().get(k0());
    assertTrue(result.remove(v0()));
    assertGet(k0());
  }

  @MapFeature.Require(SUPPORTS_PUT)
  public void testPropagatesAddToMultimap() {
    Collection<V> result = multimap().get(k0());
    assertTrue(result.add(v3()));
    assertTrue(multimap().containsKey(k0()));
    assertEquals(getNumElements() + 1, multimap().size());
    assertTrue(multimap().containsEntry(k0(), v3()));
  }

  @MapFeature.Require(SUPPORTS_PUT)
  public void testPropagatesAddAllToMultimap() {
    Collection<V> result = multimap().get(k0());
    assertTrue(result.addAll(singletonList(v3())));
    assertTrue(multimap().containsKey(k0()));
    assertEquals(getNumElements() + 1, multimap().size());
    assertTrue(multimap().containsEntry(k0(), v3()));
  }

  @CollectionSize.Require(absent = ZERO)
  @MapFeature.Require({SUPPORTS_REMOVE, SUPPORTS_PUT})
  public void testPropagatesRemoveLastThenAddToMultimap() {
    int oldSize = getNumElements();

    Collection<V> result = multimap().get(k0());
    assertTrue(result.remove(v0()));

    assertFalse(multimap().containsKey(k0()));
    assertFalse(multimap().containsEntry(k0(), v0()));
    assertEmpty(result);

    assertTrue(result.add(v1()));
    assertTrue(result.add(v2()));

    assertContentsAnyOrder(result, v1(), v2());
    assertContentsAnyOrder(multimap().get(k0()), v1(), v2());
    assertTrue(multimap().containsKey(k0()));
    assertFalse(multimap().containsEntry(k0(), v0()));
    assertTrue(multimap().containsEntry(k0(), v2()));
    assertEquals(oldSize + 1, multimap().size());
  }

  @MapFeature.Require(ALLOWS_NULL_KEYS)
  @CollectionSize.Require(absent = ZERO)
  public void testGetNullPresent() {
    initMultimapWithNullKey();
    assertContains(multimap().get(null), getValueForNullKey());
  }

  @MapFeature.Require(ALLOWS_NULL_KEY_QUERIES)
  public void testGetNullAbsent() {
    assertEmpty(multimap().get(null));
  }

  @MapFeature.Require(absent = ALLOWS_NULL_KEY_QUERIES)
  public void testGetNullForbidden() {
    assertThrows(NullPointerException.class, () -> multimap().get(null));
  }

  @MapFeature.Require(ALLOWS_NULL_VALUES)
  @CollectionSize.Require(absent = ZERO)
  public void testGetWithNullValue() {
    initMultimapWithNullValue();
    assertContains(multimap().get(getKeyForNullValue()), null);
  }
}
