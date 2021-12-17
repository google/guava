/*
 * Copyright (C) 2008 The Guava Authors
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

import static com.google.common.collect.testing.features.CollectionSize.ZERO;
import static com.google.common.collect.testing.features.MapFeature.ALLOWS_NULL_VALUES;
import static com.google.common.collect.testing.features.MapFeature.ALLOWS_NULL_VALUE_QUERIES;

import com.google.common.annotations.GwtCompatible;
import com.google.common.collect.testing.AbstractMapTester;
import com.google.common.collect.testing.WrongType;
import com.google.common.collect.testing.features.CollectionSize;
import com.google.common.collect.testing.features.MapFeature;
import org.junit.Ignore;

/**
 * A generic JUnit test which tests {@code containsValue()} operations on a map. Can't be invoked
 * directly; please see {@link com.google.common.collect.testing.MapTestSuiteBuilder}.
 *
 * @author George van den Driessche
 * @author Chris Povirk
 */
@GwtCompatible
@Ignore // Affects only Android test runner, which respects JUnit 4 annotations on JUnit 3 tests.
public class MapContainsValueTester<K, V> extends AbstractMapTester<K, V> {
  @CollectionSize.Require(absent = ZERO)
  public void testContains_yes() {
    assertTrue("containsValue(present) should return true", getMap().containsValue(v0()));
  }

  public void testContains_no() {
    assertFalse("containsValue(notPresent) should return false", getMap().containsValue(v3()));
  }

  @MapFeature.Require(ALLOWS_NULL_VALUE_QUERIES)
  public void testContains_nullNotContainedButAllowed() {
    assertFalse("containsValue(null) should return false", getMap().containsValue(null));
  }

  @MapFeature.Require(absent = ALLOWS_NULL_VALUE_QUERIES)
  public void testContains_nullNotContainedAndUnsupported() {
    expectNullValueMissingWhenNullValuesUnsupported(
        "containsValue(null) should return false or throw");
  }

  @MapFeature.Require(ALLOWS_NULL_VALUES)
  @CollectionSize.Require(absent = ZERO)
  public void testContains_nonNullWhenNullContained() {
    initMapWithNullValue();
    assertFalse("containsValue(notPresent) should return false", getMap().containsValue(v3()));
  }

  @MapFeature.Require(ALLOWS_NULL_VALUES)
  @CollectionSize.Require(absent = ZERO)
  public void testContains_nullContained() {
    initMapWithNullValue();
    assertTrue("containsValue(null) should return true", getMap().containsValue(null));
  }

  public void testContains_wrongType() {
    try {
      // noinspection SuspiciousMethodCalls
      assertFalse(
          "containsValue(wrongType) should return false or throw",
          getMap().containsValue(WrongType.VALUE));
    } catch (ClassCastException tolerated) {
    }
  }
}
