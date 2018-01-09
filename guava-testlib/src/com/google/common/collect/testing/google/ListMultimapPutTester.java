/*
 * Copyright (C) 2012 The Guava Authors
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

import static com.google.common.collect.testing.Helpers.copyToList;
import static com.google.common.collect.testing.features.CollectionSize.ZERO;
import static com.google.common.collect.testing.features.MapFeature.SUPPORTS_PUT;

import com.google.common.annotations.GwtCompatible;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.testing.Helpers;
import com.google.common.collect.testing.features.CollectionSize;
import com.google.common.collect.testing.features.MapFeature;
import java.util.List;
import java.util.Map.Entry;
import org.junit.Ignore;

/**
 * Testers for {@link ListMultimap#put(Object, Object)}.
 *
 * @author Louis Wasserman
 */
@GwtCompatible
@Ignore // Affects only Android test runner, which respects JUnit 4 annotations on JUnit 3 tests.
public class ListMultimapPutTester<K, V> extends AbstractListMultimapTester<K, V> {
  // MultimapPutTester tests non-duplicate values, but ignores ordering

  @MapFeature.Require(SUPPORTS_PUT)
  public void testPutAddsValueAtEnd() {
    for (K key : sampleKeys()) {
      for (V value : sampleValues()) {
        resetContainer();

        List<V> values = multimap().get(key);
        List<V> expectedValues = Helpers.copyToList(values);

        assertTrue(multimap().put(key, value));
        expectedValues.add(value);

        assertGet(key, expectedValues);
        assertEquals(value, values.get(values.size() - 1));
      }
    }
  }

  @MapFeature.Require(SUPPORTS_PUT)
  @CollectionSize.Require(absent = ZERO)
  public void testPutDuplicateValue() {
    List<Entry<K, V>> entries = copyToList(multimap().entries());

    for (Entry<K, V> entry : entries) {
      resetContainer();

      K k = entry.getKey();
      V v = entry.getValue();

      List<V> values = multimap().get(k);
      List<V> expectedValues = copyToList(values);

      assertTrue(multimap().put(k, v));
      expectedValues.add(v);
      assertGet(k, expectedValues);
      assertEquals(v, values.get(values.size() - 1));
    }
  }
}
