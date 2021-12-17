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

import static com.google.common.collect.testing.features.CollectionSize.ZERO;
import static com.google.common.collect.testing.features.MapFeature.ALLOWS_NULL_KEYS;
import static com.google.common.collect.testing.features.MapFeature.ALLOWS_NULL_VALUES;
import static com.google.common.collect.testing.features.MapFeature.SUPPORTS_PUT;

import com.google.common.annotations.GwtCompatible;
import com.google.common.collect.testing.AbstractMapTester;
import com.google.common.collect.testing.features.CollectionSize;
import com.google.common.collect.testing.features.MapFeature;
import java.util.Map;
import java.util.Map.Entry;
import junit.framework.AssertionFailedError;
import org.junit.Ignore;

/**
 * A generic JUnit test which tests {@link Map#computeIfPresent}. Can't be invoked directly; please
 * see {@link com.google.common.collect.testing.MapTestSuiteBuilder}.
 *
 * @author Louis Wasserman
 */
@GwtCompatible
@Ignore // Affects only Android test runner, which respects JUnit 4 annotations on JUnit 3 tests.
public class MapComputeIfPresentTester<K, V> extends AbstractMapTester<K, V> {

  @MapFeature.Require(SUPPORTS_PUT)
  public void testComputeIfPresent_supportedAbsent() {
    assertNull(
        "computeIfPresent(notPresent, function) should return null",
        getMap()
            .computeIfPresent(
                k3(),
                (k, v) -> {
                  throw new AssertionFailedError();
                }));
    expectUnchanged();
  }

  @MapFeature.Require(SUPPORTS_PUT)
  @CollectionSize.Require(absent = ZERO)
  public void testComputeIfPresent_supportedPresent() {
    assertEquals(
        "computeIfPresent(present, function) should return new value",
        v3(),
        getMap()
            .computeIfPresent(
                k0(),
                (k, v) -> {
                  assertEquals(k0(), k);
                  assertEquals(v0(), v);
                  return v3();
                }));
    expectReplacement(entry(k0(), v3()));
  }

  @MapFeature.Require(SUPPORTS_PUT)
  @CollectionSize.Require(absent = ZERO)
  public void testComputeIfPresent_functionReturnsNull() {
    assertNull(
        "computeIfPresent(present, returnsNull) should return null",
        getMap()
            .computeIfPresent(
                k0(),
                (k, v) -> {
                  assertEquals(k0(), k);
                  assertEquals(v0(), v);
                  return null;
                }));
    expectMissing(e0());
  }

  @MapFeature.Require({SUPPORTS_PUT, ALLOWS_NULL_VALUES})
  @CollectionSize.Require(absent = ZERO)
  public void testComputeIfPresent_nullTreatedAsAbsent() {
    initMapWithNullValue();
    assertNull(
        "computeIfPresent(presentAssignedToNull, function) should return null",
        getMap()
            .computeIfPresent(
                getKeyForNullValue(),
                (k, v) -> {
                  throw new AssertionFailedError();
                }));
    expectReplacement(entry(getKeyForNullValue(), null));
  }

  static class ExpectedException extends RuntimeException {}

  @MapFeature.Require(SUPPORTS_PUT)
  @CollectionSize.Require(absent = ZERO)
  public void testComputeIfPresent_functionThrows() {
    try {
      getMap()
          .computeIfPresent(
              k0(),
              (k, v) -> {
                assertEquals(k0(), k);
                assertEquals(v0(), v);
                throw new ExpectedException();
              });
      fail("Expected ExpectedException");
    } catch (ExpectedException expected) {
    }
    expectUnchanged();
  }

  @MapFeature.Require({SUPPORTS_PUT, ALLOWS_NULL_KEYS})
  @CollectionSize.Require(absent = ZERO)
  public void testComputeIfPresent_nullKeySupportedPresent() {
    initMapWithNullKey();
    assertEquals(
        "computeIfPresent(null, function) should return new value",
        v3(),
        getMap()
            .computeIfPresent(
                null,
                (k, v) -> {
                  assertNull(k);
                  assertEquals(getValueForNullKey(), v);
                  return v3();
                }));

    Entry<K, V>[] expected = createArrayWithNullKey();
    expected[getNullLocation()] = entry(null, v3());
    expectContents(expected);
  }

  @MapFeature.Require({SUPPORTS_PUT, ALLOWS_NULL_KEYS})
  public void testComputeIfPresent_nullKeySupportedAbsent() {
    assertNull(
        "computeIfPresent(null, function) should return null",
        getMap()
            .computeIfPresent(
                null,
                (k, v) -> {
                  throw new AssertionFailedError();
                }));
    expectUnchanged();
  }

  @MapFeature.Require(absent = SUPPORTS_PUT)
  public void testComputeIfPresent_unsupportedAbsent() {
    try {
      getMap()
          .computeIfPresent(
              k3(),
              (k, v) -> {
                throw new AssertionFailedError();
              });
    } catch (UnsupportedOperationException tolerated) {
    }
    expectUnchanged();
  }

  @MapFeature.Require(absent = SUPPORTS_PUT)
  @CollectionSize.Require(absent = ZERO)
  public void testComputeIfPresent_unsupportedPresent() {
    try {
      getMap().computeIfPresent(k0(), (k, v) -> v3());
      fail("Expected UnsupportedOperationException");
    } catch (UnsupportedOperationException expected) {
    }
    expectUnchanged();
  }
}
