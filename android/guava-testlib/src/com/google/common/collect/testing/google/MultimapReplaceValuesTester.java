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

import static com.google.common.collect.testing.Helpers.assertContentsAnyOrder;
import static com.google.common.collect.testing.features.CollectionSize.ZERO;
import static com.google.common.collect.testing.features.MapFeature.ALLOWS_NULL_KEYS;
import static com.google.common.collect.testing.features.MapFeature.ALLOWS_NULL_VALUES;
import static com.google.common.collect.testing.features.MapFeature.SUPPORTS_PUT;
import static com.google.common.collect.testing.features.MapFeature.SUPPORTS_REMOVE;

import com.google.common.annotations.GwtCompatible;
import com.google.common.collect.Multimap;
import com.google.common.collect.testing.Helpers;
import com.google.common.collect.testing.features.CollectionSize;
import com.google.common.collect.testing.features.MapFeature;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.junit.Ignore;

/**
 * Tests for {@link Multimap#replaceValues(Object, Iterable)}.
 *
 * @author Louis Wasserman
 */
@GwtCompatible
@Ignore // Affects only Android test runner, which respects JUnit 4 annotations on JUnit 3 tests.
public class MultimapReplaceValuesTester<K, V>
    extends AbstractMultimapTester<K, V, Multimap<K, V>> {

  @MapFeature.Require({SUPPORTS_PUT, SUPPORTS_REMOVE, ALLOWS_NULL_VALUES})
  public void testReplaceValuesWithNullValue() {
    @SuppressWarnings("unchecked")
    List<V> values = Arrays.asList(v0(), null, v3());
    multimap().replaceValues(k0(), values);
    assertGet(k0(), values);
  }

  @MapFeature.Require({SUPPORTS_PUT, SUPPORTS_REMOVE, ALLOWS_NULL_KEYS})
  public void testReplaceValuesWithNullKey() {
    @SuppressWarnings("unchecked")
    List<V> values = Arrays.asList(v0(), v2(), v3());
    multimap().replaceValues(null, values);
    assertGet(null, values);
  }

  @MapFeature.Require({SUPPORTS_PUT, SUPPORTS_REMOVE})
  public void testReplaceEmptyValues() {
    int size = multimap().size();
    @SuppressWarnings("unchecked")
    List<V> values = Arrays.asList(v0(), v2(), v3());
    multimap().replaceValues(k3(), values);
    assertGet(k3(), values);
    assertEquals(size + values.size(), multimap().size());
  }

  @MapFeature.Require({SUPPORTS_PUT, SUPPORTS_REMOVE})
  public void testReplaceValuesWithEmpty() {
    int size = multimap().size();
    List<V> oldValues = new ArrayList<>(multimap().get(k0()));
    @SuppressWarnings("unchecked")
    List<V> values = Collections.emptyList();
    assertEquals(oldValues, new ArrayList<V>(multimap().replaceValues(k0(), values)));
    assertGet(k0());
    assertEquals(size - oldValues.size(), multimap().size());
  }

  @MapFeature.Require({SUPPORTS_PUT, SUPPORTS_REMOVE})
  public void testReplaceValuesWithDuplicates() {
    int size = multimap().size();
    List<V> oldValues = new ArrayList<>(multimap().get(k0()));
    List<V> values = Arrays.asList(v0(), v3(), v0());
    assertEquals(oldValues, new ArrayList<V>(multimap().replaceValues(k0(), values)));
    assertEquals(size - oldValues.size() + multimap().get(k0()).size(), multimap().size());
    assertTrue(multimap().get(k0()).containsAll(values));
  }

  @CollectionSize.Require(absent = ZERO)
  @MapFeature.Require({SUPPORTS_PUT, SUPPORTS_REMOVE})
  public void testReplaceNonEmptyValues() {
    List<K> keys = Helpers.copyToList(multimap().keySet());
    @SuppressWarnings("unchecked")
    List<V> values = Arrays.asList(v0(), v2(), v3());

    for (K k : keys) {
      resetContainer();

      int size = multimap().size();
      Collection<V> oldKeyValues = Helpers.copyToList(multimap().get(k));
      multimap().replaceValues(k, values);
      assertGet(k, values);
      assertEquals(size + values.size() - oldKeyValues.size(), multimap().size());
    }
  }

  @MapFeature.Require({SUPPORTS_PUT, SUPPORTS_REMOVE})
  public void testReplaceValuesPropagatesToGet() {
    Collection<V> getCollection = multimap().get(k0());
    @SuppressWarnings("unchecked")
    List<V> values = Arrays.asList(v0(), v2(), v3());
    multimap().replaceValues(k0(), values);
    assertContentsAnyOrder(getCollection, v0(), v2(), v3());
  }

  @MapFeature.Require(absent = SUPPORTS_REMOVE)
  @CollectionSize.Require(absent = ZERO)
  public void testReplaceValuesRemoveNotSupported() {
    List<V> values = Collections.singletonList(v3());
    try {
      multimap().replaceValues(k0(), values);
      fail("Expected UnsupportedOperationException");
    } catch (UnsupportedOperationException expected) {
      // success
    }
  }

  @MapFeature.Require(absent = SUPPORTS_PUT)
  public void testReplaceValuesPutNotSupported() {
    List<V> values = Collections.singletonList(v3());
    try {
      multimap().replaceValues(k0(), values);
      fail("Expected UnsupportedOperationException");
    } catch (UnsupportedOperationException expected) {
      // success
    }
  }
}
