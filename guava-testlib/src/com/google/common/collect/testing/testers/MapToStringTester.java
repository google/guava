/*
 * Copyright (C) 2013 The Guava Authors
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

package com.google.common.collect.testing.testers;

import com.google.common.annotations.GwtCompatible;
import com.google.common.collect.testing.AbstractMapTester;
import com.google.common.collect.testing.features.CollectionFeature;
import com.google.common.collect.testing.features.CollectionSize;
import com.google.common.collect.testing.features.MapFeature;
import org.junit.Ignore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;

import static com.google.common.collect.testing.features.CollectionFeature.NON_STANDARD_TOSTRING;
import static com.google.common.collect.testing.features.CollectionSize.ONE;
import static com.google.common.collect.testing.features.CollectionSize.ZERO;
import static com.google.common.collect.testing.features.MapFeature.ALLOWS_NULL_KEYS;
import static com.google.common.collect.testing.features.MapFeature.ALLOWS_NULL_VALUES;

/**
 * A generic JUnit test which tests {@code toString()} operations on a map. Can't be invoked
 * directly; please see {@link com.google.common.collect.testing.MapTestSuiteBuilder}.
 *
 * @author Kevin Bourrillion
 * @author Louis Wasserman
 */
@GwtCompatible
@Ignore // Affects only Android test runner, which respects JUnit 4 annotations on JUnit 3 tests.
@SuppressWarnings("JUnit4ClassUsedInJUnit3")
public class MapToStringTester<K, V> extends AbstractMapTester<K, V> {

  private static final Integer LENGTH_OF_COMMA_AND_SPACE = 2;
  public void testToString_minimal() {
    assertNotNull("toString() should not return null", getMap().toString());
  }

  @CollectionSize.Require(ZERO)
  @CollectionFeature.Require(absent = NON_STANDARD_TOSTRING)
  public void testToString_size0() {
    assertEquals("emptyMap.toString should return {}", "{}", getMap().toString());
  }

  @CollectionSize.Require(ONE)
  @CollectionFeature.Require(absent = NON_STANDARD_TOSTRING)
  public void testToString_size1() {
    assertEquals("size1Map.toString should return {entry}", "{" + e0() + "}", getMap().toString());
  }

  @CollectionSize.Require(absent = ZERO)
  @CollectionFeature.Require(absent = NON_STANDARD_TOSTRING)
  @MapFeature.Require(ALLOWS_NULL_KEYS)
  public void testToStringWithNullKey() {
    initMapWithNullKey();
    testToString_formatting();
  }

  @CollectionSize.Require(absent = ZERO)
  @CollectionFeature.Require(absent = NON_STANDARD_TOSTRING)
  @MapFeature.Require(ALLOWS_NULL_VALUES)
  public void testToStringWithNullValue() {
    initMapWithNullValue();
    testToString_formatting();
  }

  @CollectionFeature.Require(absent = NON_STANDARD_TOSTRING)
  public void testToString_formatting() {
    String actual = getMap().toString();
    String expected = expectedToString(getMap(), getKeysParsedFromStringVersionOfMap(getMap(), actual));
    assertEquals(
        "map.toString() incorrect", expected, actual);
  }

  private String expectedToString(Map<K, V> map, List<K> keys) {
    Map<K, V> reference = new LinkedHashMap<>();
    for (K key : keys) {
      reference.put(key, map.get(key));
    }
    return reference.toString();
  }

  /**
   * For a given String, which is expected to be created by Map.toString() call,
   * creates a list of keys in the same order of appearance as they are in the string
   * @param map Any implementation of Map
   * @param mapDotToString map.toString(), to derive order of keys
   * @return List<String> keys from the map
   */
  private List<K> getKeysParsedFromStringVersionOfMap(Map<K, V> map, String mapDotToString) {

    Map<String, K> entryToKeyMap = new HashMap<>();
    for (Entry<K, V> entry: map.entrySet()) {
      entryToKeyMap.put(entry.toString(), entry.getKey());
    }
    String clipperMapString = mapDotToString.substring(1, mapDotToString.length() - 1); // getting rid of unnecessary "{" and "}"
    List<K> keyOrder = new ArrayList<>();
    while (!clipperMapString.isEmpty()) {
      String lastEntry = null;
      for (Entry<String, K> e : entryToKeyMap.entrySet()) {
        if (clipperMapString.indexOf(e.getKey()) == 0) {
          lastEntry = e.getKey();
          K topLevelKey = e.getValue();

          keyOrder.add(topLevelKey);
          if (clipperMapString.length() >= LENGTH_OF_COMMA_AND_SPACE + e.getKey().length()) {
            clipperMapString = clipperMapString.substring(LENGTH_OF_COMMA_AND_SPACE + e.getKey().length());
          } else { // When last segment of entry in clipperMapString is reached
            clipperMapString = clipperMapString.substring(e.getKey().length());
          }
          break;
        }
      }
      if (Objects.nonNull(lastEntry)) {
        entryToKeyMap.remove(lastEntry);
      } else {
        break;
      }
    }
    return keyOrder;
  }

}
