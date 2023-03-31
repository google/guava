/*
 * Copyright (C) 2010 The Guava Authors
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

package com.google.common.collect.testing;

import static java.util.Collections.sort;

import com.google.common.annotations.GwtIncompatible;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;
import com.google.common.collect.testing.Helpers.NullsBeforeTwo;
import com.google.common.collect.testing.features.CollectionFeature;
import com.google.common.collect.testing.features.CollectionSize;
import com.google.common.collect.testing.features.MapFeature;
import com.google.common.testing.SerializableTester;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.SortedMap;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Tests for SafeTreeMap.
 *
 * @author Louis Wasserman
 */
public class SafeTreeMapTest extends TestCase {
  public static Test suite() {
    TestSuite suite = new TestSuite();
    suite.addTestSuite(SafeTreeMapTest.class);
    suite.addTest(
        NavigableMapTestSuiteBuilder.using(
                new TestStringSortedMapGenerator() {
                  @Override
                  protected SortedMap<String, String> create(Entry<String, String>[] entries) {
                    NavigableMap<String, String> map = new SafeTreeMap<>(Ordering.natural());
                    for (Entry<String, String> entry : entries) {
                      map.put(entry.getKey(), entry.getValue());
                    }
                    return map;
                  }
                })
            .withFeatures(
                CollectionSize.ANY,
                CollectionFeature.KNOWN_ORDER,
                CollectionFeature.SERIALIZABLE,
                MapFeature.ALLOWS_NULL_VALUES,
                CollectionFeature.SUPPORTS_ITERATOR_REMOVE,
                MapFeature.GENERAL_PURPOSE)
            .named("SafeTreeMap with natural comparator")
            .createTestSuite());
    suite.addTest(
        NavigableMapTestSuiteBuilder.using(
                new TestStringSortedMapGenerator() {
                  @Override
                  protected SortedMap<String, String> create(Entry<String, String>[] entries) {
                    NavigableMap<String, String> map = new SafeTreeMap<>(NullsBeforeTwo.INSTANCE);
                    for (Entry<String, String> entry : entries) {
                      map.put(entry.getKey(), entry.getValue());
                    }
                    return map;
                  }

                  @Override
                  public Iterable<Entry<String, String>> order(
                      List<Entry<String, String>> insertionOrder) {
                    sort(
                        insertionOrder,
                        Helpers.<String, String>entryComparator(NullsBeforeTwo.INSTANCE));
                    return insertionOrder;
                  }
                })
            .withFeatures(
                CollectionSize.ANY,
                CollectionFeature.KNOWN_ORDER,
                MapFeature.ALLOWS_NULL_KEYS,
                MapFeature.ALLOWS_NULL_VALUES,
                MapFeature.ALLOWS_ANY_NULL_QUERIES,
                MapFeature.GENERAL_PURPOSE,
                CollectionFeature.SUPPORTS_ITERATOR_REMOVE,
                CollectionFeature.SERIALIZABLE)
            .named("SafeTreeMap with null-friendly comparator")
            .createTestSuite());
    return suite;
  }

  @GwtIncompatible // SerializableTester
  public void testViewSerialization() {
    Map<String, Integer> map = ImmutableSortedMap.of("one", 1, "two", 2, "three", 3);
    SerializableTester.reserializeAndAssert(map.entrySet());
    SerializableTester.reserializeAndAssert(map.keySet());
    assertEquals(
        Lists.newArrayList(map.values()),
        Lists.newArrayList(SerializableTester.reserialize(map.values())));
  }
}
