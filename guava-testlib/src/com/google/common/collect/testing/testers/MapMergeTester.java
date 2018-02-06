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
import static com.google.common.collect.testing.features.MapFeature.SUPPORTS_REMOVE;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.collect.testing.AbstractMapTester;
import com.google.common.collect.testing.Helpers;
import com.google.common.collect.testing.features.CollectionSize;
import com.google.common.collect.testing.features.MapFeature;
import java.lang.reflect.Method;
import java.util.Map;
import junit.framework.AssertionFailedError;
import org.junit.Ignore;

/**
 * A generic JUnit test which tests {@link Map#merge}. Can't be invoked directly; please see {@link
 * com.google.common.collect.testing.MapTestSuiteBuilder}.
 *
 * @author Louis Wasserman
 */
@GwtCompatible(emulated = true)
@Ignore // Affects only Android test runner, which respects JUnit 4 annotations on JUnit 3 tests.
public class MapMergeTester<K, V> extends AbstractMapTester<K, V> {
  @MapFeature.Require(SUPPORTS_PUT)
  public void testAbsent() {
    assertEquals(
        "Map.merge(absent, value, function) should return value",
        v3(),
        getMap()
            .merge(
                k3(),
                v3(),
                (oldV, newV) -> {
                  throw new AssertionFailedError(
                      "Should not call merge function if key was absent");
                }));
    expectAdded(e3());
  }

  @MapFeature.Require({SUPPORTS_PUT, ALLOWS_NULL_VALUES})
  @CollectionSize.Require(absent = ZERO)
  public void testMappedToNull() {
    initMapWithNullValue();
    assertEquals(
        "Map.merge(keyMappedToNull, value, function) should return value",
        v3(),
        getMap()
            .merge(
                getKeyForNullValue(),
                v3(),
                (oldV, newV) -> {
                  throw new AssertionFailedError(
                      "Should not call merge function if key was mapped to null");
                }));
    expectReplacement(entry(getKeyForNullValue(), v3()));
  }

  @MapFeature.Require({SUPPORTS_PUT, ALLOWS_NULL_KEYS})
  public void testMergeAbsentNullKey() {
    assertEquals(
        "Map.merge(null, value, function) should return value",
        v3(),
        getMap()
            .merge(
                null,
                v3(),
                (oldV, newV) -> {
                  throw new AssertionFailedError(
                      "Should not call merge function if key was absent");
                }));
    expectAdded(entry(null, v3()));
  }

  @MapFeature.Require(SUPPORTS_PUT)
  @CollectionSize.Require(absent = ZERO)
  public void testMergePresent() {
    assertEquals(
        "Map.merge(present, value, function) should return function result",
        v4(),
        getMap()
            .merge(
                k0(),
                v3(),
                (oldV, newV) -> {
                  assertEquals(v0(), oldV);
                  assertEquals(v3(), newV);
                  return v4();
                }));
    expectReplacement(entry(k0(), v4()));
  }

  private static class ExpectedException extends RuntimeException {}

  @MapFeature.Require(SUPPORTS_PUT)
  @CollectionSize.Require(absent = ZERO)
  public void testMergeFunctionThrows() {
    try {
      getMap()
          .merge(
              k0(),
              v3(),
              (oldV, newV) -> {
                assertEquals(v0(), oldV);
                assertEquals(v3(), newV);
                throw new ExpectedException();
              });
      fail("Expected ExpectedException");
    } catch (ExpectedException expected) {
    }
    expectUnchanged();
  }

  @MapFeature.Require(SUPPORTS_REMOVE)
  @CollectionSize.Require(absent = ZERO)
  public void testMergePresentToNull() {
    assertNull(
        "Map.merge(present, value, functionReturningNull) should return null",
        getMap()
            .merge(
                k0(),
                v3(),
                (oldV, newV) -> {
                  assertEquals(v0(), oldV);
                  assertEquals(v3(), newV);
                  return null;
                }));
    expectMissing(e0());
  }

  public void testMergeNullValue() {
    try {
      getMap()
          .merge(
              k0(),
              null,
              (oldV, newV) -> {
                throw new AssertionFailedError("Should not call merge function if value was null");
              });
      fail("Expected NullPointerException or UnsupportedOperationException");
    } catch (NullPointerException | UnsupportedOperationException expected) {
    }
  }

  public void testMergeNullFunction() {
    try {
      getMap().merge(k0(), v3(), null);
      fail("Expected NullPointerException or UnsupportedOperationException");
    } catch (NullPointerException | UnsupportedOperationException expected) {
    }
  }

  @MapFeature.Require(absent = SUPPORTS_PUT)
  public void testMergeUnsupported() {
    try {
      getMap()
          .merge(
              k3(),
              v3(),
              (oldV, newV) -> {
                throw new AssertionFailedError();
              });
      fail("Expected UnsupportedOperationException");
    } catch (UnsupportedOperationException expected) {
    }
  }

  /**
   * Returns the {@link Method} instance for {@link #testMergeNullValue()} so that tests of {@link
   * Hashtable} can suppress it with {@code FeatureSpecificTestSuiteBuilder.suppressing()}.
   */
  @GwtIncompatible // reflection
  public static Method getMergeNullValueMethod() {
    return Helpers.getMethod(MapMergeTester.class, "testMergeNullValue");
  }
}
