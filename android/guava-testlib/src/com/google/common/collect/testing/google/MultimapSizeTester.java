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

import static com.google.common.collect.testing.Helpers.mapEntry;
import static com.google.common.collect.testing.features.CollectionSize.SEVERAL;
import static com.google.common.collect.testing.features.CollectionSize.ZERO;
import static com.google.common.collect.testing.features.MapFeature.ALLOWS_NULL_KEYS;
import static com.google.common.collect.testing.features.MapFeature.ALLOWS_NULL_VALUES;

import com.google.common.annotations.GwtCompatible;
import com.google.common.collect.Multimap;
import com.google.common.collect.testing.features.CollectionSize;
import com.google.common.collect.testing.features.MapFeature;
import java.util.Collection;
import java.util.Map.Entry;
import org.junit.Ignore;

/**
 * Tester for the {@code size} methods of {@code Multimap} and its views.
 *
 * @author Louis Wasserman
 */
@GwtCompatible
@Ignore // Affects only Android test runner, which respects JUnit 4 annotations on JUnit 3 tests.
public class MultimapSizeTester<K, V> extends AbstractMultimapTester<K, V, Multimap<K, V>> {

  public void testSize() {
    int expectedSize = getNumElements();
    Multimap<K, V> multimap = multimap();
    assertEquals(expectedSize, multimap.size());

    int size = 0;
    for (Entry<K, V> entry : multimap.entries()) {
      assertTrue(multimap.containsEntry(entry.getKey(), entry.getValue()));
      size++;
    }
    assertEquals(expectedSize, size);

    int size2 = 0;
    for (Entry<K, Collection<V>> entry2 : multimap.asMap().entrySet()) {
      size2 += entry2.getValue().size();
    }
    assertEquals(expectedSize, size2);
  }

  @CollectionSize.Require(ZERO)
  public void testIsEmptyYes() {
    assertTrue(multimap().isEmpty());
  }

  @CollectionSize.Require(absent = ZERO)
  public void testIsEmptyNo() {
    assertFalse(multimap().isEmpty());
  }

  @CollectionSize.Require(absent = ZERO)
  @MapFeature.Require(ALLOWS_NULL_KEYS)
  public void testSizeNullKey() {
    initMultimapWithNullKey();
    assertEquals(getNumElements(), multimap().size());
    assertFalse(multimap().isEmpty());
  }

  @CollectionSize.Require(absent = ZERO)
  @MapFeature.Require(ALLOWS_NULL_VALUES)
  public void testSizeNullValue() {
    initMultimapWithNullValue();
    assertEquals(getNumElements(), multimap().size());
    assertFalse(multimap().isEmpty());
  }

  @CollectionSize.Require(absent = ZERO)
  @MapFeature.Require({ALLOWS_NULL_KEYS, ALLOWS_NULL_VALUES})
  public void testSizeNullKeyAndValue() {
    initMultimapWithNullKeyAndValue();
    assertEquals(getNumElements(), multimap().size());
    assertFalse(multimap().isEmpty());
  }

  @CollectionSize.Require(SEVERAL)
  public void testSizeMultipleValues() {
    resetContainer(mapEntry(k0(), v0()), mapEntry(k0(), v1()), mapEntry(k0(), v2()));

    assertEquals(3, multimap().size());
    assertEquals(3, multimap().entries().size());
    assertEquals(3, multimap().keys().size());

    assertEquals(1, multimap().keySet().size());
    assertEquals(1, multimap().asMap().size());
  }
}
