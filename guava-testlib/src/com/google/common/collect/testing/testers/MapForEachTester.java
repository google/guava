/*
 * Copyright (C) 2015 The Guava Authors
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

package com.google.common.collect.testing.testers;

import static com.google.common.collect.testing.features.CollectionFeature.KNOWN_ORDER;
import static com.google.common.collect.testing.features.CollectionSize.ZERO;
import static com.google.common.collect.testing.features.MapFeature.ALLOWS_NULL_KEYS;
import static com.google.common.collect.testing.features.MapFeature.ALLOWS_NULL_VALUES;

import com.google.common.annotations.GwtCompatible;
import com.google.common.collect.testing.AbstractMapTester;
import com.google.common.collect.testing.Helpers;
import com.google.common.collect.testing.features.CollectionFeature;
import com.google.common.collect.testing.features.CollectionSize;
import com.google.common.collect.testing.features.MapFeature;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.junit.Ignore;

/**
 * A generic JUnit test which tests {@link Map#forEach}. Can't be invoked directly; please see
 * {@link com.google.common.collect.testing.MapTestSuiteBuilder}.
 *
 * @author Louis Wasserman
 */
@GwtCompatible
@Ignore // Affects only Android test runner, which respects JUnit 4 annotations on JUnit 3 tests.
public class MapForEachTester<K, V> extends AbstractMapTester<K, V> {
  @CollectionFeature.Require(KNOWN_ORDER)
  public void testForEachKnownOrder() {
    List<Entry<K, V>> entries = new ArrayList<>();
    getMap().forEach((k, v) -> entries.add(entry(k, v)));
    assertEquals(getOrderedElements(), entries);
  }

  @CollectionFeature.Require(absent = KNOWN_ORDER)
  public void testForEachUnknownOrder() {
    List<Entry<K, V>> entries = new ArrayList<>();
    getMap().forEach((k, v) -> entries.add(entry(k, v)));
    Helpers.assertEqualIgnoringOrder(getSampleEntries(), entries);
  }

  @MapFeature.Require(ALLOWS_NULL_KEYS)
  @CollectionSize.Require(absent = ZERO)
  public void testForEach_nullKeys() {
    initMapWithNullKey();
    List<Entry<K, V>> expectedEntries = Arrays.asList(createArrayWithNullKey());
    List<Entry<K, V>> entries = new ArrayList<>();
    getMap().forEach((k, v) -> entries.add(entry(k, v)));
    Helpers.assertEqualIgnoringOrder(expectedEntries, entries);
  }

  @MapFeature.Require(ALLOWS_NULL_VALUES)
  @CollectionSize.Require(absent = ZERO)
  public void testForEach_nullValues() {
    initMapWithNullValue();
    List<Entry<K, V>> expectedEntries = Arrays.asList(createArrayWithNullValue());
    List<Entry<K, V>> entries = new ArrayList<>();
    getMap().forEach((k, v) -> entries.add(entry(k, v)));
    Helpers.assertEqualIgnoringOrder(expectedEntries, entries);
  }
}
