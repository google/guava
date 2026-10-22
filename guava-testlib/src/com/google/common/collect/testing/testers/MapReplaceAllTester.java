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

import static com.google.common.collect.testing.Helpers.assertEqualIgnoringOrder;
import static com.google.common.collect.testing.Helpers.copyToSet;
import static com.google.common.collect.testing.Helpers.mapEntry;
import static com.google.common.collect.testing.features.CollectionFeature.KNOWN_ORDER;
import static com.google.common.collect.testing.features.CollectionSize.SEVERAL;
import static com.google.common.collect.testing.features.CollectionSize.ZERO;
import static com.google.common.collect.testing.features.MapFeature.SUPPORTS_PUT;
import static org.junit.Assert.assertThrows;

import com.google.common.annotations.GwtCompatible;
import com.google.common.collect.testing.AbstractMapTester;
import com.google.common.collect.testing.SampleElements;
import com.google.common.collect.testing.features.CollectionFeature;
import com.google.common.collect.testing.features.CollectionSize;
import com.google.common.collect.testing.features.MapFeature;
import com.google.common.collect.testing.testers.TestExceptions.SomeUncheckedException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;
import org.jspecify.annotations.Nullable;

/**
 * A generic JUnit test which tests {@code replaceAll()} operations on a map. Can't be invoked
 * directly; please see {@link com.google.common.collect.testing.MapTestSuiteBuilder}.
 *
 * @author Louis Wasserman
 */
@GwtCompatible
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
      expectedEntries.add(mapEntry(entry.getKey(), values().asList().get(index + 1)));
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

  @MapFeature.Require(SUPPORTS_PUT)
  @CollectionSize.Require(SEVERAL)
  public void testReplaceAllThrowsMidwayThroughNewValue() {
    doTestReplaceAllThrowsMidwayThrough((k, v) -> v3());
  }

  @MapFeature.Require(SUPPORTS_PUT)
  @CollectionSize.Require(SEVERAL)
  public void testReplaceAllThrowsMidwayThroughExistingValue() {
    doTestReplaceAllThrowsMidwayThrough(
        (k, v) -> {
          int index = keys().asList().indexOf(k);
          return values().asList().get((index + 1) % getNumEntries());
        });
  }

  private void doTestReplaceAllThrowsMidwayThrough(BiFunction<K, V, V> functionForFirstEntry) {
    Map<K, V> map = getMap();

    /*
     * We set up a stateful function that replaces the first value it encounters (and stores the new
     * entry) and then throws afterward.
     *
     * We expect either:
     *
     * - The first value is replaced in the map, and everything else is untouched.
     *
     * - The entire map is untouched because the map either computes all replacements before making
     *   changes or else rolls back changes in case of an exception.
     */

    AtomicReference<@Nullable Entry<K, V>> firstReplacement = new AtomicReference<>();
    assertThrows(
        SomeUncheckedException.class,
        () ->
            map.replaceAll(
                (k, v) -> {
                  if (firstReplacement.get() == null) {
                    V newValue = functionForFirstEntry.apply(k, v);
                    firstReplacement.set(mapEntry(k, newValue));
                    return newValue;
                  }
                  throw new SomeUncheckedException();
                }));
    Entry<K, V> replacement = firstReplacement.get();
    if (Objects.equals(map.get(replacement.getKey()), replacement.getValue())) {
      expectReplacement(replacement);
    } else {
      expectUnchanged();
    }

    List<V> expectedValues = new ArrayList<>();
    for (Entry<K, V> entry : map.entrySet()) {
      expectedValues.add(entry.getValue());
    }
    assertEqualIgnoringOrder(expectedValues, map.values());
    assertEquals(expectedValues.size(), map.values().size());
    for (V value : values().asList()) {
      if (expectedValues.contains(value)) {
        assertTrue("Should contain value " + value, map.containsValue(value));
        assertTrue("values() should contain " + value, map.values().contains(value));
      } else {
        expectMissingValues(value);
        assertFalse("values() should not contain " + value, map.values().contains(value));
      }
    }

    if (map.values() instanceof Set) {
      Set<V> values = (Set<V>) getMap().values();
      assertEquals(
          "values() Set should not contain duplicate values",
          expectedValues.size(),
          copyToSet(expectedValues).size());
      assertEquals(copyToSet(expectedValues), values);
    }
  }

  @MapFeature.Require(absent = SUPPORTS_PUT)
  @CollectionSize.Require(absent = ZERO)
  public void testReplaceAll_unsupported() {
    assertThrows(
        UnsupportedOperationException.class,
        () ->
            getMap()
                .replaceAll(
                    (K k, V v) -> {
                      int index = keys().asList().indexOf(k);
                      return values().asList().get(index + 1);
                    }));
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
