/*
 * Copyright (C) 2016 The Guava Authors
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

package com.google.common.graph;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.Ordering;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.TreeMap;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

/** Tests for {@link MapIteratorCache} and {@link MapRetrievalCache}. */
@AndroidIncompatible
// TODO(cpovirk): Figure out Android JUnit 4 support. Does it work with Gingerbread? @RunWith?
@RunWith(Parameterized.class)
public final class MapCacheTest {
  private final MapIteratorCache<String, String> mapCache;

  public MapCacheTest(MapIteratorCache<String, String> mapCache) {
    this.mapCache = mapCache;
  }

  @Parameters
  public static Collection<Object[]> parameters() {
    Comparator<String> nullsLast = Ordering.natural().nullsLast();

    return Arrays.asList(
        new Object[][] {
          {new MapIteratorCache<String, String>(new HashMap<String, String>())},
          {new MapIteratorCache<String, String>(new TreeMap<String, String>(nullsLast))},
          {new MapRetrievalCache<String, String>(new HashMap<String, String>())},
          {new MapRetrievalCache<String, String>(new TreeMap<String, String>(nullsLast))}
        });
  }

  @Before
  public void init() {
    mapCache.clear();
  }

  @Test
  public void testKeySetIterator() {
    mapCache.put("A", "A_value");
    mapCache.put("B", "B_value");
    mapCache.put("C", "C_value");

    assertThat(mapCache.unmodifiableKeySet()).hasSize(3);
    for (String key : mapCache.unmodifiableKeySet()) {
      assertThat(mapCache.get(key)).isEqualTo(key + "_value");
    }
  }

  @Test
  public void testPutNewValue() {
    assertThat(mapCache.put("key", "value")).isNull();
    assertThat(mapCache.get("key")).isEqualTo("value"); // ensure key/value is cached
    assertThat(mapCache.put("key", "new value")).isEqualTo("value");
    assertThat(mapCache.get("key")).isEqualTo("new value");
  }

  @Test
  public void testRemoveEqualKeyWithDifferentReference() {
    String fooReference1 = new String("foo");
    String fooReference2 = new String("foo");
    assertThat(fooReference1).isNotSameInstanceAs(fooReference2);

    assertThat(mapCache.put(fooReference1, "bar")).isNull();
    assertThat(mapCache.get(fooReference1)).isEqualTo("bar"); // ensure first reference is cached
    assertThat(mapCache.remove(fooReference2)).isEqualTo("bar");
    assertThat(mapCache.get(fooReference1)).isNull();
  }
}
