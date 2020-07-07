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

package com.google.common.collect.testing.testers;

import static com.google.common.collect.testing.features.CollectionSize.ZERO;
import static com.google.common.collect.testing.features.MapFeature.ALLOWS_NULL_KEYS;
import static com.google.common.collect.testing.features.MapFeature.ALLOWS_NULL_KEY_QUERIES;

import com.google.common.annotations.GwtCompatible;
import com.google.common.collect.testing.AbstractMapTester;
import com.google.common.collect.testing.WrongType;
import com.google.common.collect.testing.features.CollectionSize;
import com.google.common.collect.testing.features.MapFeature;
import org.junit.Ignore;

/**
 * A generic JUnit test which tests {@code get} operations on a map. Can't be invoked directly;
 * please see {@link com.google.common.collect.testing.MapTestSuiteBuilder}.
 *
 * @author Kevin Bourrillion
 * @author Chris Povirk
 */
@GwtCompatible
@Ignore // Affects only Android test runner, which respects JUnit 4 annotations on JUnit 3 tests.
public class MapGetTester<K, V> extends AbstractMapTester<K, V> {
  @CollectionSize.Require(absent = ZERO)
  public void testGet_yes() {
    assertEquals("get(present) should return the associated value", v0(), get(k0()));
  }

  public void testGet_no() {
    assertNull("get(notPresent) should return null", get(k3()));
  }

  @MapFeature.Require(ALLOWS_NULL_KEY_QUERIES)
  public void testGet_nullNotContainedButAllowed() {
    assertNull("get(null) should return null", get(null));
  }

  @MapFeature.Require(absent = ALLOWS_NULL_KEY_QUERIES)
  public void testGet_nullNotContainedAndUnsupported() {
    try {
      assertNull("get(null) should return null or throw", get(null));
    } catch (NullPointerException tolerated) {
    }
  }

  @MapFeature.Require(ALLOWS_NULL_KEYS)
  @CollectionSize.Require(absent = ZERO)
  public void testGet_nonNullWhenNullContained() {
    initMapWithNullKey();
    assertNull("get(notPresent) should return null", get(k3()));
  }

  @MapFeature.Require(ALLOWS_NULL_KEYS)
  @CollectionSize.Require(absent = ZERO)
  public void testGet_nullContained() {
    initMapWithNullKey();
    assertEquals("get(null) should return the associated value", getValueForNullKey(), get(null));
  }

  public void testGet_wrongType() {
    try {
      assertNull("get(wrongType) should return null or throw", getMap().get(WrongType.VALUE));
    } catch (ClassCastException tolerated) {
    }
  }
}
