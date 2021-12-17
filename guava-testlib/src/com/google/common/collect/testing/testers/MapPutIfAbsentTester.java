/*
 * Copyright (C) 2015 The Guava Authors
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
import static com.google.common.collect.testing.features.MapFeature.ALLOWS_NULL_VALUES;
import static com.google.common.collect.testing.features.MapFeature.SUPPORTS_PUT;

import com.google.common.annotations.GwtCompatible;
import com.google.common.collect.testing.AbstractMapTester;
import com.google.common.collect.testing.features.CollectionSize;
import com.google.common.collect.testing.features.MapFeature;
import java.util.Map;
import org.junit.Ignore;

/**
 * A generic JUnit test which tests {@link Map#putIfAbsent}. Can't be invoked directly; please see
 * {@link com.google.common.collect.testing.MapTestSuiteBuilder}.
 *
 * @author Louis Wasserman
 */
@GwtCompatible
@Ignore // Affects only Android test runner, which respects JUnit 4 annotations on JUnit 3 tests.
public class MapPutIfAbsentTester<K, V> extends AbstractMapTester<K, V> {

  @MapFeature.Require(SUPPORTS_PUT)
  public void testPutIfAbsent_supportedAbsent() {
    assertNull(
        "putIfAbsent(notPresent, value) should return null", getMap().putIfAbsent(k3(), v3()));
    expectAdded(e3());
  }

  @MapFeature.Require(SUPPORTS_PUT)
  @CollectionSize.Require(absent = ZERO)
  public void testPutIfAbsent_supportedPresent() {
    assertEquals(
        "putIfAbsent(present, value) should return existing value",
        v0(),
        getMap().putIfAbsent(k0(), v3()));
    expectUnchanged();
  }

  @MapFeature.Require(absent = SUPPORTS_PUT)
  public void testPutIfAbsent_unsupportedAbsent() {
    try {
      getMap().putIfAbsent(k3(), v3());
      fail("putIfAbsent(notPresent, value) should throw");
    } catch (UnsupportedOperationException expected) {
    }
    expectUnchanged();
    expectMissing(e3());
  }

  @MapFeature.Require(absent = SUPPORTS_PUT)
  @CollectionSize.Require(absent = ZERO)
  public void testPutIfAbsent_unsupportedPresentExistingValue() {
    try {
      assertEquals(
          "putIfAbsent(present, existingValue) should return present or throw",
          v0(),
          getMap().putIfAbsent(k0(), v0()));
    } catch (UnsupportedOperationException tolerated) {
    }
    expectUnchanged();
  }

  @MapFeature.Require(absent = SUPPORTS_PUT)
  @CollectionSize.Require(absent = ZERO)
  public void testPutIfAbsent_unsupportedPresentDifferentValue() {
    try {
      getMap().putIfAbsent(k0(), v3());
    } catch (UnsupportedOperationException tolerated) {
    }
    expectUnchanged();
  }

  @MapFeature.Require(value = SUPPORTS_PUT, absent = ALLOWS_NULL_KEYS)
  public void testPutIfAbsent_nullKeyUnsupported() {
    try {
      getMap().putIfAbsent(null, v3());
      fail("putIfAbsent(null, value) should throw");
    } catch (NullPointerException expected) {
    }
    expectUnchanged();
    expectNullKeyMissingWhenNullKeysUnsupported(
        "Should not contain null key after unsupported putIfAbsent(null, value)");
  }

  @MapFeature.Require(value = SUPPORTS_PUT, absent = ALLOWS_NULL_VALUES)
  public void testPutIfAbsent_nullValueUnsupported() {
    try {
      getMap().putIfAbsent(k3(), null);
      fail("putIfAbsent(key, null) should throw");
    } catch (NullPointerException expected) {
    }
    expectUnchanged();
    expectNullValueMissingWhenNullValuesUnsupported(
        "Should not contain null value after unsupported put(key, null)");
  }

  @MapFeature.Require(value = SUPPORTS_PUT, absent = ALLOWS_NULL_VALUES)
  @CollectionSize.Require(absent = ZERO)
  public void testPutIfAbsent_putWithNullValueUnsupported() {
    try {
      getMap().putIfAbsent(k0(), null);
    } catch (NullPointerException tolerated) {
    }
    expectUnchanged();
    expectNullValueMissingWhenNullValuesUnsupported(
        "Should not contain null after unsupported putIfAbsent(present, null)");
  }
}
