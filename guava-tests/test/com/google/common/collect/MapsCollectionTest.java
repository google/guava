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

import com.google.common.collect.testing.NavigableMapTestSuiteBuilder;
import com.google.common.collect.testing.SafeTreeMap;
import com.google.common.collect.testing.TestStringMapGenerator;
import com.google.common.collect.testing.features.CollectionSize;
import com.google.common.collect.testing.features.MapFeature;
import com.google.common.collect.testing.google.BiMapTestSuiteBuilder;
import com.google.common.collect.testing.google.TestStringBiMapGenerator;
import com.google.common.testing.SerializableTester;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.util.Map;
import java.util.Map.Entry;

/**
 * Test suites for wrappers in {@code Maps}.
 * 
 * @author Louis Wasserman
 */
public class MapsCollectionTest extends TestCase {
  public static Test suite() {
    TestSuite suite = new TestSuite();
    suite.addTest(NavigableMapTestSuiteBuilder
        .using(new TestStringMapGenerator() {
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
        .using(new TestStringMapGenerator() {

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
    return suite;
  }
}
