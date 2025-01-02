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
import static com.google.common.collect.testing.testers.ReflectionFreeAssertThrows.assertThrows;

import com.google.common.annotations.GwtCompatible;
import com.google.common.collect.testing.AbstractMapTester;
import com.google.common.collect.testing.features.CollectionSize;
import com.google.common.collect.testing.features.MapFeature;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentMap;
import org.jspecify.annotations.NullMarked;
import org.junit.Ignore;

/**
 * A generic JUnit test which tests {@code putIfAbsent} operations on a concurrent map. Can't be
 * invoked directly; please see {@link
 * com.google.common.collect.testing.ConcurrentMapTestSuiteBuilder}.
 *
 * @author Louis Wasserman
 */
@GwtCompatible
@Ignore("test runners must not instantiate and run this directly, only via suites we build")
// @Ignore affects the Android test runner, which respects JUnit 4 annotations on JUnit 3 tests.
@SuppressWarnings("JUnit4ClassUsedInJUnit3")
@NullMarked
public class ConcurrentMapPutIfAbsentTester<K, V> extends AbstractMapTester<K, V> {
  @Override
  protected ConcurrentMap<K, V> getMap() {
    return (ConcurrentMap<K, V>) super.getMap();
  }

  @MapFeature.Require(SUPPORTS_PUT)
  public void testPutIfAbsent_supportedAbsent() {
    assertNull("putIfAbsent(notPresent, value) should return null", putIfAbsent(e3()));
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
    assertThrows(UnsupportedOperationException.class, () -> putIfAbsent(e3()));
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
          putIfAbsent(e0()));
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
    assertThrows(NullPointerException.class, () -> getMap().putIfAbsent(null, v3()));
    expectUnchanged();
    expectNullKeyMissingWhenNullKeysUnsupported(
        "Should not contain null key after unsupported putIfAbsent(null, value)");
  }

  @MapFeature.Require(value = SUPPORTS_PUT, absent = ALLOWS_NULL_VALUES)
  public void testPutIfAbsent_nullValueUnsupported() {
    assertThrows(NullPointerException.class, () -> getMap().putIfAbsent(k3(), null));
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

  @CanIgnoreReturnValue
  private V putIfAbsent(Entry<K, V> entry) {
    return getMap().putIfAbsent(entry.getKey(), entry.getValue());
  }
}
