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
import static com.google.common.collect.testing.features.MapFeature.SUPPORTS_REMOVE;

import com.google.common.annotations.GwtCompatible;
import com.google.common.collect.testing.AbstractMapTester;
import com.google.common.collect.testing.features.CollectionSize;
import com.google.common.collect.testing.features.MapFeature;
import java.util.Map;
import org.junit.Ignore;

/**
 * A generic JUnit test which tests {@link Map#compute}. Can't be invoked directly; please see
 * {@link com.google.common.collect.testing.MapTestSuiteBuilder}.
 *
 * @author Louis Wasserman
 */
@GwtCompatible
@Ignore // Affects only Android test runner, which respects JUnit 4 annotations on JUnit 3 tests.
public class MapComputeTester<K, V> extends AbstractMapTester<K, V> {
  @MapFeature.Require({SUPPORTS_PUT, SUPPORTS_REMOVE})
  public void testCompute_absentToPresent() {
    assertEquals(
        "Map.compute(absent, functionReturningValue) should return value",
        v3(),
        getMap()
            .compute(
                k3(),
                (k, v) -> {
                  assertEquals(k3(), k);
                  assertNull(v);
                  return v3();
                }));
    expectAdded(e3());
    assertEquals(getNumElements() + 1, getMap().size());
  }

  @MapFeature.Require({SUPPORTS_PUT, SUPPORTS_REMOVE})
  public void testCompute_absentToAbsent() {
    assertNull(
        "Map.compute(absent, functionReturningNull) should return null",
        getMap()
            .compute(
                k3(),
                (k, v) -> {
                  assertEquals(k3(), k);
                  assertNull(v);
                  return null;
                }));
    expectUnchanged();
    assertEquals(getNumElements(), getMap().size());
  }

  @MapFeature.Require({SUPPORTS_PUT, SUPPORTS_REMOVE})
  @CollectionSize.Require(absent = ZERO)
  public void testCompute_presentToPresent() {
    assertEquals(
        "Map.compute(present, functionReturningValue) should return new value",
        v3(),
        getMap()
            .compute(
                k0(),
                (k, v) -> {
                  assertEquals(k0(), k);
                  assertEquals(v0(), v);
                  return v3();
                }));
    expectReplacement(entry(k0(), v3()));
    assertEquals(getNumElements(), getMap().size());
  }

  @MapFeature.Require({SUPPORTS_PUT, SUPPORTS_REMOVE})
  @CollectionSize.Require(absent = ZERO)
  public void testCompute_presentToAbsent() {
    assertNull(
        "Map.compute(present, functionReturningNull) should return null",
        getMap()
            .compute(
                k0(),
                (k, v) -> {
                  assertEquals(k0(), k);
                  assertEquals(v0(), v);
                  return null;
                }));
    expectMissing(e0());
    expectMissingKeys(k0());
    assertEquals(getNumElements() - 1, getMap().size());
  }

  @MapFeature.Require({SUPPORTS_PUT, SUPPORTS_REMOVE, ALLOWS_NULL_VALUES})
  @CollectionSize.Require(absent = ZERO)
  public void testCompute_presentNullToPresentNonnull() {
    initMapWithNullValue();
    V value = getValueForNullKey();
    assertEquals(
        "Map.compute(presentMappedToNull, functionReturningValue) should return new value",
        value,
        getMap()
            .compute(
                getKeyForNullValue(),
                (k, v) -> {
                  assertEquals(getKeyForNullValue(), k);
                  assertNull(v);
                  return value;
                }));
    expectReplacement(entry(getKeyForNullValue(), value));
    assertEquals(getNumElements(), getMap().size());
  }

  @MapFeature.Require({SUPPORTS_PUT, SUPPORTS_REMOVE, ALLOWS_NULL_VALUES})
  @CollectionSize.Require(absent = ZERO)
  public void testCompute_presentNullToNull() {
    // The spec is somewhat ambiguous about this case, but the actual default implementation
    // in Map will remove a present null.
    initMapWithNullValue();
    assertNull(
        "Map.compute(presentMappedToNull, functionReturningNull) should return null",
        getMap()
            .compute(
                getKeyForNullValue(),
                (k, v) -> {
                  assertEquals(getKeyForNullValue(), k);
                  assertNull(v);
                  return null;
                }));
    expectMissingKeys(getKeyForNullValue());
    assertEquals(getNumElements() - 1, getMap().size());
  }

  @MapFeature.Require({SUPPORTS_PUT, SUPPORTS_REMOVE, ALLOWS_NULL_KEYS})
  @CollectionSize.Require(absent = ZERO)
  public void testCompute_nullKeyPresentToPresent() {
    initMapWithNullKey();
    assertEquals(
        "Map.compute(present, functionReturningValue) should return new value",
        v3(),
        getMap()
            .compute(
                null,
                (k, v) -> {
                  assertNull(k);
                  assertEquals(getValueForNullKey(), v);
                  return v3();
                }));
    assertEquals(getNumElements(), getMap().size());
  }

  static class ExpectedException extends RuntimeException {}

  @MapFeature.Require({SUPPORTS_PUT, SUPPORTS_REMOVE})
  @CollectionSize.Require(absent = ZERO)
  public void testCompute_presentFunctionThrows() {
    try {
      getMap()
          .compute(
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

  @MapFeature.Require({SUPPORTS_PUT, SUPPORTS_REMOVE})
  public void testCompute_absentFunctionThrows() {
    try {
      getMap()
          .compute(
              k3(),
              (k, v) -> {
                assertEquals(k3(), k);
                assertNull(v);
                throw new ExpectedException();
              });
      fail("Expected ExpectedException");
    } catch (ExpectedException expected) {
    }
    expectUnchanged();
  }
}
