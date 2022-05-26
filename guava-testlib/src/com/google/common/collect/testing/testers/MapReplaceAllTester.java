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

package com.google.common.collect.testing.testers;

import static com.google.common.collect.testing.features.CollectionFeature.KNOWN_ORDER;
import static com.google.common.collect.testing.features.CollectionSize.ZERO;
import static com.google.common.collect.testing.features.MapFeature.SUPPORTS_PUT;

import com.google.common.annotations.GwtCompatible;
import com.google.common.collect.testing.AbstractMapTester;
import com.google.common.collect.testing.Helpers;
import com.google.common.collect.testing.SampleElements;
import com.google.common.collect.testing.features.CollectionFeature;
import com.google.common.collect.testing.features.CollectionSize;
import com.google.common.collect.testing.features.MapFeature;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import org.junit.Ignore;

/**
 * A generic JUnit test which tests {@code replaceAll()} operations on a map. Can't be invoked
 * directly; please see {@link com.google.common.collect.testing.MapTestSuiteBuilder}.
 *
 * @author Louis Wasserman
 */
@GwtCompatible
@Ignore // Affects only Android test runner, which respects JUnit 4 annotations on JUnit 3 tests.
public class MapReplaceAllTester<K, V> extends AbstractMapTester<K, V> {
  private SampleElements<K> keys() {
    return new SampleElements<>(k0(), k1(), k2(), k3(), k4());
  }

  private SampleElements<V> values() {
    return new SampleElements<>(v0(), v1(), v2(), v3(), v4());
  }

  @MapFeature.Require(SUPPORTS_PUT)
  public void testReplaceAllRotate() {
    getMap()
        .replaceAll(
            (K k, V v) -> {
              int index = keys().asList().indexOf(k);
              return values().asList().get(index + 1);
            });
    List<Entry<K, V>> expectedEntries = new ArrayList<>();
    for (Entry<K, V> entry : getSampleEntries()) {
      int index = keys().asList().indexOf(entry.getKey());
      expectedEntries.add(Helpers.mapEntry(entry.getKey(), values().asList().get(index + 1)));
    }
    expectContents(expectedEntries);
  }

  @MapFeature.Require(SUPPORTS_PUT)
  @CollectionFeature.Require(KNOWN_ORDER)
  public void testReplaceAllPreservesOrder() {
    getMap()
        .replaceAll(
            (K k, V v) -> {
              int index = keys().asList().indexOf(k);
              return values().asList().get(index + 1);
            });
    List<Entry<K, V>> orderedEntries = getOrderedElements();
    int index = 0;
    for (K key : getMap().keySet()) {
      assertEquals(orderedEntries.get(index).getKey(), key);
      index++;
    }
  }

  @MapFeature.Require(absent = SUPPORTS_PUT)
  @CollectionSize.Require(absent = ZERO)
  public void testReplaceAll_unsupported() {
    try {
      getMap()
          .replaceAll(
              (K k, V v) -> {
                int index = keys().asList().indexOf(k);
                return values().asList().get(index + 1);
              });
      fail(
          "replaceAll() should throw UnsupportedOperation if a map does "
              + "not support it and is not empty.");
    } catch (UnsupportedOperationException expected) {
    }
    expectUnchanged();
  }

  @MapFeature.Require(absent = SUPPORTS_PUT)
  @CollectionSize.Require(ZERO)
  public void testReplaceAll_unsupportedByEmptyCollection() {
    try {
      getMap()
          .replaceAll(
              (K k, V v) -> {
                int index = keys().asList().indexOf(k);
                return values().asList().get(index + 1);
              });
    } catch (UnsupportedOperationException tolerated) {
    }
    expectUnchanged();
  }

  @MapFeature.Require(absent = SUPPORTS_PUT)
  public void testReplaceAll_unsupportedNoOpFunction() {
    try {
      getMap().replaceAll((K k, V v) -> v);
    } catch (UnsupportedOperationException tolerated) {
    }
    expectUnchanged();
  }
}
