/*
 * Copyright (C) 2007 The Guava Authors
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

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.collect.testing.features.CollectionFeature;
import com.google.common.collect.testing.features.CollectionSize;
import com.google.common.collect.testing.features.MapFeature;
import com.google.common.collect.testing.google.BiMapTestSuiteBuilder;
import com.google.common.collect.testing.google.TestStringBiMapGenerator;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * Tests for {@link HashBiMap}.
 *
 * @author Mike Bostock
 */
@GwtCompatible(emulated = true)
public class HashBiMapTest extends TestCase {

  public static final class HashBiMapGenerator extends TestStringBiMapGenerator {
    @Override
    protected BiMap<String, String> create(Entry<String, String>[] entries) {
      BiMap<String, String> result = HashBiMap.create();
      for (Entry<String, String> entry : entries) {
        result.put(entry.getKey(), entry.getValue());
      }
      return result;
    }
  }

  @GwtIncompatible("suite")
  public static Test suite() {
    TestSuite suite = new TestSuite();
    suite.addTest(BiMapTestSuiteBuilder.using(new HashBiMapGenerator())
      .named("HashBiMap")
      .withFeatures(CollectionSize.ANY,
          CollectionFeature.SERIALIZABLE,
          CollectionFeature.SUPPORTS_ITERATOR_REMOVE,
          MapFeature.ALLOWS_NULL_KEYS,
          MapFeature.ALLOWS_NULL_VALUES,
          MapFeature.ALLOWS_ANY_NULL_QUERIES,
          MapFeature.GENERAL_PURPOSE)
      .createTestSuite());
    suite.addTestSuite(HashBiMapTest.class);
    return suite;
  }

  public void testMapConstructor() {
    /* Test with non-empty Map. */
    Map<String, String> map = ImmutableMap.of(
        "canada", "dollar",
        "chile", "peso",
        "switzerland", "franc");
    HashBiMap<String, String> bimap = HashBiMap.create(map);
    assertEquals("dollar", bimap.get("canada"));
    assertEquals("canada", bimap.inverse().get("dollar"));
  }

  private static final int N = 1000;

  public void testBashIt() throws Exception {
    BiMap<Integer, Integer> bimap = HashBiMap.create(N);
    BiMap<Integer, Integer> inverse = bimap.inverse();

    for (int i = 0; i < N; i++) {
      assertNull(bimap.put(2 * i, 2 * i + 1));
    }
    for (int i = 0; i < N; i++) {
      assertEquals(2 * i + 1, (int) bimap.get(2 * i));
    }
    for (int i = 0; i < N; i++) {
      assertEquals(2 * i, (int) inverse.get(2 * i + 1));
    }
    for (int i = 0; i < N; i++) {
      int oldValue = bimap.get(2 * i);
      assertEquals(2 * i + 1, (int) bimap.put(2 * i, oldValue - 2));
    }
    for (int i = 0; i < N; i++) {
      assertEquals(2 * i - 1, (int) bimap.get(2 * i));
    }
    for (int i = 0; i < N; i++) {
      assertEquals(2 * i, (int) inverse.get(2 * i - 1));
    }
    Set<Entry<Integer, Integer>> entries = bimap.entrySet();
    for (Entry<Integer, Integer> entry : entries) {
      entry.setValue(entry.getValue() + 2 * N);
    }
    for (int i = 0; i < N; i++) {
      assertEquals(2 * N + 2 * i - 1, (int) bimap.get(2 * i));
    }
  }

  public void testBiMapEntrySetIteratorRemove() {
    BiMap<Integer, String> map = HashBiMap.create();
    map.put(1, "one");
    Set<Map.Entry<Integer, String>> entries = map.entrySet();
    Iterator<Map.Entry<Integer, String>> iterator = entries.iterator();
    Map.Entry<Integer, String> entry = iterator.next();
    entry.setValue("two"); // changes the iterator's current entry value
    assertEquals("two", map.get(1));
    assertEquals(Integer.valueOf(1), map.inverse().get("two"));
    iterator.remove(); // removes the updated entry
    assertTrue(map.isEmpty());
  }
}
