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
package com.google.common.collect;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.testing.Helpers.mapEntry;

import com.google.common.base.Function;
import com.google.common.collect.testing.MapTestSuiteBuilder;
import com.google.common.collect.testing.NavigableMapTestSuiteBuilder;
import com.google.common.collect.testing.SafeTreeMap;
import com.google.common.collect.testing.SampleElements;
import com.google.common.collect.testing.TestMapGenerator;
import com.google.common.collect.testing.TestStringSortedMapGenerator;
import com.google.common.collect.testing.features.CollectionSize;
import com.google.common.collect.testing.features.MapFeature;
import com.google.common.collect.testing.google.BiMapTestSuiteBuilder;
import com.google.common.collect.testing.google.TestStringBiMapGenerator;
import com.google.common.testing.SerializableTester;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * Test suites for wrappers in {@code Maps}.
 *
 * @author Louis Wasserman
 */
public class MapsCollectionTest extends TestCase {
  public static Test suite() {
    TestSuite suite = new TestSuite();
    suite.addTest(NavigableMapTestSuiteBuilder
        .using(new TestStringSortedMapGenerator() {
          @Override
          protected Map<String, String> create(Entry<String, String>[] entries) {
            SafeTreeMap<String, String> map = new SafeTreeMap<String, String>();
            for (Entry<String, String> entry : entries) {
              map.put(entry.getKey(), entry.getValue());
            }
            return Maps.unmodifiableNavigableMap(map);
          }
        })
        .named("unmodifiableNavigableMap[SafeTreeMap]")
        .withFeatures(CollectionSize.ANY,
            MapFeature.ALLOWS_NULL_VALUES)
        .createTestSuite());
    suite.addTest(NavigableMapTestSuiteBuilder
        .using(new TestStringSortedMapGenerator() {

          @Override
          protected Map<String, String> create(Entry<String, String>[] entries) {
            SafeTreeMap<String, String> map = new SafeTreeMap<String, String>();
            for (Entry<String, String> entry : entries) {
              map.put(entry.getKey(), entry.getValue());
            }
            return SerializableTester.reserialize(Maps.unmodifiableNavigableMap(map));
          }
        })
        .named("unmodifiableNavigableMap[SafeTreeMap], reserialized")
        .withFeatures(CollectionSize.ANY,
            MapFeature.ALLOWS_NULL_VALUES)
        .createTestSuite());
    suite.addTest(BiMapTestSuiteBuilder
        .using(new TestStringBiMapGenerator() {
          @Override
          protected BiMap<String, String> create(Entry<String, String>[] entries) {
            BiMap<String, String> bimap = HashBiMap.create(entries.length);
            for (Entry<String, String> entry : entries) {
              checkArgument(!bimap.containsKey(entry.getKey()));
              bimap.put(entry.getKey(), entry.getValue());
            }
            return Maps.unmodifiableBiMap(bimap);
          }
        })
        .named("unmodifiableBiMap[HashBiMap]")
        .withFeatures(
            CollectionSize.ANY,
            MapFeature.ALLOWS_NULL_VALUES,
            MapFeature.ALLOWS_NULL_KEYS,
            MapFeature.REJECTS_DUPLICATES_AT_CREATION)
        .createTestSuite());
    suite.addTest(BiMapTestSuiteBuilder
        .using(new TestStringBiMapGenerator() {
          @Override
          protected BiMap<String, String> create(Entry<String, String>[] entries) {
            BiMap<String, String> bimap = HashBiMap.create(entries.length);
            for (Entry<String, String> entry : entries) {
              checkArgument(!bimap.containsKey(entry.getKey()));
              bimap.put(entry.getKey(), entry.getValue());
            }
            return SerializableTester.reserialize(Maps.unmodifiableBiMap(bimap));
          }
        })
        .named("unmodifiableBiMap[HashBiMap], reserialized")
        .withFeatures(
            CollectionSize.ANY,
            MapFeature.ALLOWS_NULL_VALUES,
            MapFeature.ALLOWS_NULL_KEYS,
            MapFeature.REJECTS_DUPLICATES_AT_CREATION)
        .createTestSuite());
    suite.addTest(MapTestSuiteBuilder.using(new TestMapGenerator<String, Integer>(){
        @Override
        public SampleElements<Entry<String, Integer>> samples() {
          return new SampleElements<Map.Entry<String, Integer>>(
              mapEntry("x", 1),
              mapEntry("xxx", 3),
              mapEntry("xx", 2),
              mapEntry("xxxx", 4),
              mapEntry("aaaaa", 5));
        }

        @Override
        public Map<String, Integer> create(Object... elements) {
          Set<String> set = Sets.newLinkedHashSet();
          for (Object e : elements) {
            Map.Entry<?, ?> entry = (Entry<?, ?>) e;
            checkNotNull(entry.getValue());
            set.add((String) checkNotNull(entry.getKey()));
          }
          return Maps.asMap(set, new Function<String, Integer>() {
            @Override
            public Integer apply(String input) {
              return input.length();
            }
          });
        }

        @SuppressWarnings("unchecked")
        @Override
        public Entry<String, Integer>[] createArray(int length) {
          return new Entry[length];
        }

        @Override
        public Iterable<Entry<String, Integer>> order(List<Entry<String, Integer>> insertionOrder) {
          return insertionOrder;
        }

        @Override
        public String[] createKeyArray(int length) {
          return new String[length];
        }

        @Override
        public Integer[] createValueArray(int length) {
          return new Integer[length];
        }
      })
      .named("Maps.asMap")
      .withFeatures(CollectionSize.ANY,
          MapFeature.SUPPORTS_REMOVE)
      .createTestSuite());
    return suite;
  }
}
