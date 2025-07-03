/*
 * Copyright (C) 2013 The Guava Authors
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

import static com.google.common.collect.testing.Helpers.assertContentsAnyOrder;
import static com.google.common.collect.testing.Helpers.assertEmpty;
import static com.google.common.collect.testing.Helpers.mapEntry;
import static com.google.common.collect.testing.features.CollectionSize.SEVERAL;
import static com.google.common.collect.testing.features.CollectionSize.ZERO;
import static com.google.common.collect.testing.features.MapFeature.ALLOWS_NULL_VALUES;
import static com.google.common.collect.testing.features.MapFeature.ALLOWS_NULL_VALUE_QUERIES;
import static com.google.common.collect.testing.features.MapFeature.SUPPORTS_PUT;
import static com.google.common.collect.testing.features.MapFeature.SUPPORTS_REMOVE;
import static com.google.common.collect.testing.google.ReflectionFreeAssertThrows.assertThrows;

import com.google.common.annotations.GwtCompatible;
import com.google.common.collect.Multimap;
import com.google.common.collect.testing.features.CollectionSize;
import com.google.common.collect.testing.features.MapFeature;
import java.util.Collection;
import org.junit.Ignore;

/**
 * Tests for {@code Multimap.asMap().get(Object)}.
 *
 * @author Louis Wasserman
 */
@GwtCompatible
@Ignore("test runners must not instantiate and run this directly, only via suites we build")
// @Ignore affects the Android test runner, which respects JUnit 4 annotations on JUnit 3 tests.
@SuppressWarnings("JUnit4ClassUsedInJUnit3")
public class MultimapAsMapGetTester<K, V> extends AbstractMultimapTester<K, V, Multimap<K, V>> {

  @CollectionSize.Require(SEVERAL)
  @MapFeature.Require(SUPPORTS_REMOVE)
  public void testPropagatesRemoveToMultimap() {
    resetContainer(mapEntry(k0(), v0()), mapEntry(k0(), v3()), mapEntry(k0(), v2()));
    Collection<V> result = multimap().asMap().get(k0());
    assertTrue(result.remove(v0()));
    assertFalse(multimap().containsEntry(k0(), v0()));
    assertEquals(2, multimap().size());
  }

  @CollectionSize.Require(absent = ZERO)
  @MapFeature.Require(SUPPORTS_REMOVE)
  public void testPropagatesRemoveLastElementToMultimap() {
    Collection<V> result = multimap().asMap().get(k0());
    assertTrue(result.remove(v0()));
    assertGet(k0());
  }

  @CollectionSize.Require(absent = ZERO)
  @MapFeature.Require(SUPPORTS_REMOVE)
  public void testPropagatesClearToMultimap() {
    Collection<V> result = multimap().asMap().get(k0());
    result.clear();
    assertGet(k0());
    assertEmpty(result);
  }

  @CollectionSize.Require(absent = ZERO)
  @MapFeature.Require({SUPPORTS_PUT, ALLOWS_NULL_VALUES})
  public void testAddNullValue() {
    Collection<V> result = multimap().asMap().get(k0());
    assertTrue(result.add(null));
    assertTrue(multimap().containsEntry(k0(), null));
  }

  @CollectionSize.Require(absent = ZERO)
  @MapFeature.Require({SUPPORTS_REMOVE, ALLOWS_NULL_VALUE_QUERIES})
  public void testRemoveNullValue() {
    Collection<V> result = multimap().asMap().get(k0());
    assertFalse(result.remove(null));
  }

  @CollectionSize.Require(absent = ZERO)
  @MapFeature.Require(value = SUPPORTS_PUT, absent = ALLOWS_NULL_VALUES)
  public void testAddNullValueUnsupported() {
    Collection<V> result = multimap().asMap().get(k0());
    assertThrows(NullPointerException.class, () -> result.add(null));
  }

  @CollectionSize.Require(absent = ZERO)
  @MapFeature.Require(SUPPORTS_PUT)
  public void testPropagatesAddToMultimap() {
    Collection<V> result = multimap().asMap().get(k0());
    result.add(v3());
    assertContentsAnyOrder(multimap().get(k0()), v0(), v3());
  }

  @CollectionSize.Require(absent = ZERO)
  @MapFeature.Require({SUPPORTS_REMOVE, SUPPORTS_PUT})
  public void testPropagatesRemoveThenAddToMultimap() {
    int oldSize = getNumElements();

    Collection<V> result = multimap().asMap().get(k0());
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

  @CollectionSize.Require(absent = ZERO)
  @MapFeature.Require(SUPPORTS_REMOVE)
  public void testReflectsMultimapRemove() {
    Collection<V> result = multimap().asMap().get(k0());
    multimap().removeAll(k0());
    assertEmpty(result);
  }
}
