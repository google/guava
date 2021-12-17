/*
 * Copyright (C) 2013 The Guava Authors
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

import static com.google.common.collect.testing.Helpers.assertEmpty;
import static com.google.common.collect.testing.features.CollectionSize.ZERO;
import static com.google.common.collect.testing.features.MapFeature.SUPPORTS_REMOVE;
import static com.google.common.collect.testing.google.GoogleHelpers.assertEmpty;

import com.google.common.annotations.GwtCompatible;
import com.google.common.collect.Multimap;
import com.google.common.collect.testing.features.CollectionSize;
import com.google.common.collect.testing.features.MapFeature;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import org.junit.Ignore;

/**
 * Tests for {@link Multimap#clear()}.
 *
 * @author Louis Wasserman
 */
@GwtCompatible
@Ignore // Affects only Android test runner, which respects JUnit 4 annotations on JUnit 3 tests.
public class MultimapClearTester<K, V> extends AbstractMultimapTester<K, V, Multimap<K, V>> {
  @CollectionSize.Require(absent = ZERO)
  @MapFeature.Require(absent = SUPPORTS_REMOVE)
  public void testClearUnsupported() {
    try {
      multimap().clear();
      fail("Expected UnsupportedOperationException");
    } catch (UnsupportedOperationException expected) {
    }
  }

  private void assertCleared() {
    assertEquals(0, multimap().size());
    assertEmpty(multimap());
    assertEquals(multimap(), getSubjectGenerator().create());
    assertEmpty(multimap().entries());
    assertEmpty(multimap().asMap());
    assertEmpty(multimap().keySet());
    assertEmpty(multimap().keys());
    assertEmpty(multimap().values());
    for (K key : sampleKeys()) {
      assertGet(key);
    }
  }

  @MapFeature.Require(SUPPORTS_REMOVE)
  public void testClear() {
    multimap().clear();
    assertCleared();
  }

  @MapFeature.Require(SUPPORTS_REMOVE)
  public void testClearThroughEntries() {
    multimap().entries().clear();
    assertCleared();
  }

  @MapFeature.Require(SUPPORTS_REMOVE)
  public void testClearThroughAsMap() {
    multimap().asMap().clear();
    assertCleared();
  }

  @MapFeature.Require(SUPPORTS_REMOVE)
  public void testClearThroughKeySet() {
    multimap().keySet().clear();
    assertCleared();
  }

  @MapFeature.Require(SUPPORTS_REMOVE)
  public void testClearThroughKeys() {
    multimap().keys().clear();
    assertCleared();
  }

  @MapFeature.Require(SUPPORTS_REMOVE)
  public void testClearThroughValues() {
    multimap().values().clear();
    assertCleared();
  }

  @MapFeature.Require(SUPPORTS_REMOVE)
  @CollectionSize.Require(absent = ZERO)
  public void testClearPropagatesToGet() {
    for (K key : sampleKeys()) {
      resetContainer();
      Collection<V> collection = multimap().get(key);
      multimap().clear();
      assertEmpty(collection);
    }
  }

  @MapFeature.Require(SUPPORTS_REMOVE)
  @CollectionSize.Require(absent = ZERO)
  public void testClearPropagatesToAsMapGet() {
    for (K key : sampleKeys()) {
      resetContainer();
      Collection<V> collection = multimap().asMap().get(key);
      if (collection != null) {
        multimap().clear();
        assertEmpty(collection);
      }
    }
  }

  @MapFeature.Require(SUPPORTS_REMOVE)
  public void testClearPropagatesToAsMap() {
    Map<K, Collection<V>> asMap = multimap().asMap();
    multimap().clear();
    assertEmpty(asMap);
  }

  @MapFeature.Require(SUPPORTS_REMOVE)
  public void testClearPropagatesToEntries() {
    Collection<Entry<K, V>> entries = multimap().entries();
    multimap().clear();
    assertEmpty(entries);
  }
}
