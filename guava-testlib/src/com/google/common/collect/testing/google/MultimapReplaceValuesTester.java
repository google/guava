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
import static com.google.common.collect.testing.features.MapFeature.SUPPORTS_REMOVE;

import com.google.common.annotations.GwtCompatible;
import com.google.common.collect.Multimap;
import com.google.common.collect.testing.Helpers;
import com.google.common.collect.testing.features.CollectionSize;
import com.google.common.collect.testing.features.MapFeature;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Tests for {@link Multimap#replaceValues(Object, Iterable)}.
 *
 * @author Louis Wasserman
 */
@GwtCompatible
public class MultimapReplaceValuesTester<K, V>
    extends AbstractMultimapTester<K, V, Multimap<K, V>> {
  @MapFeature.Require({SUPPORTS_PUT, SUPPORTS_REMOVE})
  public void testReplaceEmptyValues() {
    int size = multimap().size();
    K key = sampleKeys().e3;
    @SuppressWarnings("unchecked")
    List<V> values = Arrays.asList(sampleValues().e0, sampleValues().e2, sampleValues().e3);
    multimap().replaceValues(key, values);
    assertGet(key, values.toArray());
    assertEquals(size + values.size(), multimap().size());
  }

  @CollectionSize.Require(absent = ZERO)
  @MapFeature.Require({SUPPORTS_PUT, SUPPORTS_REMOVE})
  public void testReplaceNonEmptyValues() {
    List<K> keys = Helpers.copyToList(multimap().keySet());
    @SuppressWarnings("unchecked")
    List<V> values = Arrays.asList(sampleValues().e0, sampleValues().e2, sampleValues().e3);

    for (K k : keys) {
      resetContainer();

      int size = multimap().size();
      Collection<V> oldKeyValues = Helpers.copyToList(multimap().get(k));
      multimap().replaceValues(k, values);
      assertGet(k, values.toArray());
      assertEquals(size + values.size() - oldKeyValues.size(), multimap().size());
    }
  }

  @MapFeature.Require(absent = SUPPORTS_REMOVE)
  @CollectionSize.Require(absent = ZERO)
  public void testReplaceValuesRemoveNotSupported() {
    List<V> values = Collections.singletonList(sampleValues().e3);
    try {
      multimap().replaceValues(sampleKeys().e0, values);
      fail("Expected UnsupportedOperationException");
    } catch (UnsupportedOperationException expected) {
      // success
    }
  }

  @MapFeature.Require(absent = SUPPORTS_PUT)
  public void testReplaceValuesPutNotSupported() {
    List<V> values = Collections.singletonList(sampleValues().e3);
    try {
      multimap().replaceValues(sampleKeys().e0, values);
      fail("Expected UnsupportedOperationException");
    } catch (UnsupportedOperationException expected) {
      // success
    }
  }
}
