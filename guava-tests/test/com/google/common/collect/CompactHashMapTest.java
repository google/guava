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

import static com.google.common.collect.Iterables.getOnlyElement;
import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.testing.MapTestSuiteBuilder;
import com.google.common.collect.testing.TestStringMapGenerator;
import com.google.common.collect.testing.features.CollectionFeature;
import com.google.common.collect.testing.features.CollectionSize;
import com.google.common.collect.testing.features.MapFeature;
import java.util.Map;
import java.util.Map.Entry;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Tests for {@code CompactHashMap}.
 *
 * @author Louis Wasserman
 */
public class CompactHashMapTest extends TestCase {
  public static Test suite() {
    TestSuite suite = new TestSuite();
    suite.addTest(
        MapTestSuiteBuilder.using(
                new TestStringMapGenerator() {
                  @Override
                  protected Map<String, String> create(Entry<String, String>[] entries) {
                    Map<String, String> map = CompactHashMap.create();
                    for (Entry<String, String> entry : entries) {
                      map.put(entry.getKey(), entry.getValue());
                    }
                    return map;
                  }
                })
            .named("CompactHashMap")
            .withFeatures(
                CollectionSize.ANY,
                MapFeature.GENERAL_PURPOSE,
                MapFeature.ALLOWS_NULL_KEYS,
                MapFeature.ALLOWS_NULL_VALUES,
                CollectionFeature.SERIALIZABLE,
                CollectionFeature.SUPPORTS_ITERATOR_REMOVE)
            .createTestSuite());
    suite.addTestSuite(CompactHashMapTest.class);
    return suite;
  }

  public void testTrimToSize() {
    CompactHashMap<Integer, String> map = CompactHashMap.createWithExpectedSize(100);
    for (int i = 0; i < 10; i++) {
      map.put(i, Integer.toString(i));
    }
    map.trimToSize();
    assertThat(map.entries).hasLength(10);
    assertThat(map.keys).hasLength(10);
    assertThat(map.values).hasLength(10);
    assertEquals(10, map.size());
    for (int i = 0; i < 10; i++) {
      assertEquals(Integer.toString(i), map.get(i));
    }
  }

  public void testEntrySetValueAfterRemoved() {
    CompactHashMap<Integer, String> map = CompactHashMap.create();
    map.put(1, "1");
    Entry<Integer, String> entry = getOnlyElement(map.entrySet());
    map.remove(1);
    entry.setValue("one");
    assertThat(map).containsEntry(1, "one");
  }

  public void testAllocArraysDefault() {
    CompactHashMap<Integer, String> map = CompactHashMap.create();
    assertThat(map.needsAllocArrays()).isTrue();
    assertThat(map.entries).isNull();
    assertThat(map.keys).isNull();
    assertThat(map.values).isNull();

    map.put(1, "1");
    assertThat(map.needsAllocArrays()).isFalse();
    assertThat(map.entries).hasLength(CompactHashMap.DEFAULT_SIZE);
    assertThat(map.keys).hasLength(CompactHashMap.DEFAULT_SIZE);
    assertThat(map.values).hasLength(CompactHashMap.DEFAULT_SIZE);
  }

  public void testAllocArraysExpectedSize() {
    for (int i = 0; i <= CompactHashMap.DEFAULT_SIZE; i++) {
      CompactHashMap<Integer, String> map = CompactHashMap.createWithExpectedSize(i);
      assertThat(map.needsAllocArrays()).isTrue();
      assertThat(map.entries).isNull();
      assertThat(map.keys).isNull();
      assertThat(map.values).isNull();

      map.put(1, "1");
      assertThat(map.needsAllocArrays()).isFalse();
      int expectedSize = Math.max(1, i);
      assertThat(map.entries).hasLength(expectedSize);
      assertThat(map.keys).hasLength(expectedSize);
      assertThat(map.values).hasLength(expectedSize);
    }
  }
}
