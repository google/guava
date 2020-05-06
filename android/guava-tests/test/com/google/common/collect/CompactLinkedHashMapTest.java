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

package com.google.common.collect;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.testing.MapTestSuiteBuilder;
import com.google.common.collect.testing.TestStringMapGenerator;
import com.google.common.collect.testing.features.CollectionFeature;
import com.google.common.collect.testing.features.CollectionSize;
import com.google.common.collect.testing.features.MapFeature;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Tests for {@code CompactLinkedHashMap}.
 *
 * @author Louis Wasserman
 */
public class CompactLinkedHashMapTest extends TestCase {
  public static Test suite() {
    TestSuite suite = new TestSuite();
    suite.addTest(
        MapTestSuiteBuilder.using(
                new TestStringMapGenerator() {
                  @Override
                  protected Map<String, String> create(Entry<String, String>[] entries) {
                    Map<String, String> map = CompactLinkedHashMap.create();
                    for (Entry<String, String> entry : entries) {
                      map.put(entry.getKey(), entry.getValue());
                    }
                    return map;
                  }
                })
            .named("CompactLinkedHashMap")
            .withFeatures(
                CollectionSize.ANY,
                CollectionFeature.SUPPORTS_ITERATOR_REMOVE,
                MapFeature.GENERAL_PURPOSE,
                MapFeature.ALLOWS_NULL_KEYS,
                MapFeature.ALLOWS_NULL_VALUES,
                CollectionFeature.SERIALIZABLE,
                CollectionFeature.KNOWN_ORDER)
            .createTestSuite());
    suite.addTestSuite(CompactLinkedHashMapTest.class);
    return suite;
  }

  public void testInsertionOrder() {
    Map<Integer, String> map = CompactLinkedHashMap.create();
    map.put(1, "a");
    map.put(4, "b");
    map.put(3, "d");
    map.put(2, "c");
    testHasMapEntriesInOrder(map, 1, "a", 4, "b", 3, "d", 2, "c");
  }

  public void testInsertionOrderAfterPutKeyTwice() {
    Map<Integer, String> map = CompactLinkedHashMap.create();
    map.put(1, "a");
    map.put(4, "b");
    map.put(3, "d");
    map.put(2, "c");
    map.put(1, "e");
    testHasMapEntriesInOrder(map, 1, "e", 4, "b", 3, "d", 2, "c");
  }

  public void testInsertionOrderAfterRemoveFirstEntry() {
    Map<Integer, String> map = CompactLinkedHashMap.create();
    map.put(1, "a");
    map.put(4, "b");
    map.put(3, "d");
    map.put(2, "c");
    map.remove(1);
    testHasMapEntriesInOrder(map, 4, "b", 3, "d", 2, "c");
  }

  public void testInsertionOrderAfterRemoveMiddleEntry() {
    Map<Integer, String> map = CompactLinkedHashMap.create();
    map.put(1, "a");
    map.put(4, "b");
    map.put(3, "d");
    map.put(2, "c");
    map.remove(4);
    testHasMapEntriesInOrder(map, 1, "a", 3, "d", 2, "c");
  }

  public void testInsertionOrderAfterRemoveLastEntry() {
    Map<Integer, String> map = CompactLinkedHashMap.create();
    map.put(1, "a");
    map.put(4, "b");
    map.put(3, "d");
    map.put(2, "c");
    map.remove(2);
    testHasMapEntriesInOrder(map, 1, "a", 4, "b", 3, "d");
  }

  public void testTrimToSize() {
    CompactLinkedHashMap<Integer, String> map = CompactLinkedHashMap.createWithExpectedSize(100);
    map.put(1, "a");
    map.put(4, "b");
    map.put(3, "d");
    map.put(2, "c");
    map.trimToSize();
    assertThat(map.entries).hasLength(4);
    assertThat(map.keys).hasLength(4);
    assertThat(map.values).hasLength(4);
    assertThat(map.links).hasLength(4);
    assertEquals(4, map.size());
    testHasMapEntriesInOrder(map, 1, "a", 4, "b", 3, "d", 2, "c");
  }

  private void testHasMapEntriesInOrder(Map<?, ?> map, Object... alternatingKeysAndValues) {
    List<? extends Entry<?, ?>> entries = Lists.newArrayList(map.entrySet());
    List<Object> keys = Lists.newArrayList(map.keySet());
    List<Object> values = Lists.newArrayList(map.values());
    assertEquals(2 * entries.size(), alternatingKeysAndValues.length);
    assertEquals(2 * keys.size(), alternatingKeysAndValues.length);
    assertEquals(2 * values.size(), alternatingKeysAndValues.length);
    for (int i = 0; i < map.size(); i++) {
      Object expectedKey = alternatingKeysAndValues[2 * i];
      Object expectedValue = alternatingKeysAndValues[2 * i + 1];
      Entry<Object, Object> expectedEntry = Maps.immutableEntry(expectedKey, expectedValue);
      assertEquals(expectedEntry, entries.get(i));
      assertEquals(expectedKey, keys.get(i));
      assertEquals(expectedValue, values.get(i));
    }
  }

  public void testAllocArraysDefault() {
    CompactLinkedHashMap<Integer, String> map = CompactLinkedHashMap.create();
    assertThat(map.needsAllocArrays()).isTrue();
    assertThat(map.entries).isNull();
    assertThat(map.keys).isNull();
    assertThat(map.values).isNull();
    assertThat(map.links).isNull();

    map.put(1, Integer.toString(1));
    assertThat(map.needsAllocArrays()).isFalse();
    assertThat(map.entries).hasLength(CompactHashing.DEFAULT_SIZE);
    assertThat(map.keys).hasLength(CompactHashing.DEFAULT_SIZE);
    assertThat(map.values).hasLength(CompactHashing.DEFAULT_SIZE);
    assertThat(map.links).hasLength(CompactHashing.DEFAULT_SIZE);
  }

  public void testAllocArraysExpectedSize() {
    for (int i = 0; i <= CompactHashing.DEFAULT_SIZE; i++) {
      CompactLinkedHashMap<Integer, String> map = CompactLinkedHashMap.createWithExpectedSize(i);
      assertThat(map.needsAllocArrays()).isTrue();
      assertThat(map.entries).isNull();
      assertThat(map.keys).isNull();
      assertThat(map.values).isNull();
      assertThat(map.links).isNull();

      map.put(1, Integer.toString(1));
      assertThat(map.needsAllocArrays()).isFalse();
      int expectedSize = Math.max(1, i);
      assertThat(map.entries).hasLength(expectedSize);
      assertThat(map.keys).hasLength(expectedSize);
      assertThat(map.values).hasLength(expectedSize);
      assertThat(map.links).hasLength(expectedSize);
    }
  }
}
