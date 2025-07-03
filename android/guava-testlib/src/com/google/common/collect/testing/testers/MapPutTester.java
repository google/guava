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

import static com.google.common.collect.testing.Helpers.getMethod;
import static com.google.common.collect.testing.features.CollectionSize.ZERO;
import static com.google.common.collect.testing.features.MapFeature.ALLOWS_NULL_KEYS;
import static com.google.common.collect.testing.features.MapFeature.ALLOWS_NULL_VALUES;
import static com.google.common.collect.testing.features.MapFeature.FAILS_FAST_ON_CONCURRENT_MODIFICATION;
import static com.google.common.collect.testing.features.MapFeature.SUPPORTS_PUT;
import static com.google.common.collect.testing.testers.ReflectionFreeAssertThrows.assertThrows;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.annotations.J2ktIncompatible;
import com.google.common.collect.testing.AbstractMapTester;
import com.google.common.collect.testing.features.CollectionSize;
import com.google.common.collect.testing.features.MapFeature;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.lang.reflect.Method;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.Map.Entry;
import org.junit.Ignore;

/**
 * A generic JUnit test which tests {@code put} operations on a map. Can't be invoked directly;
 * please see {@link com.google.common.collect.testing.MapTestSuiteBuilder}.
 *
 * @author Chris Povirk
 * @author Kevin Bourrillion
 */
@GwtCompatible(emulated = true)
@Ignore("test runners must not instantiate and run this directly, only via suites we build")
// @Ignore affects the Android test runner, which respects JUnit 4 annotations on JUnit 3 tests.
@SuppressWarnings("JUnit4ClassUsedInJUnit3")
public class MapPutTester<K, V> extends AbstractMapTester<K, V> {
  private Entry<K, V> nullKeyEntry;
  private Entry<K, V> nullValueEntry;
  private Entry<K, V> nullKeyValueEntry;
  private Entry<K, V> presentKeyNullValueEntry;

  @Override
  public void setUp() throws Exception {
    super.setUp();
    nullKeyEntry = entry(null, v3());
    nullValueEntry = entry(k3(), null);
    nullKeyValueEntry = entry(null, null);
    presentKeyNullValueEntry = entry(k0(), null);
  }

  @MapFeature.Require(SUPPORTS_PUT)
  @CollectionSize.Require(absent = ZERO)
  public void testPut_supportedPresent() {
    assertEquals("put(present, value) should return the old value", v0(), getMap().put(k0(), v3()));
    expectReplacement(entry(k0(), v3()));
  }

  @MapFeature.Require(SUPPORTS_PUT)
  public void testPut_supportedNotPresent() {
    assertNull("put(notPresent, value) should return null", put(e3()));
    expectAdded(e3());
  }

  @MapFeature.Require({FAILS_FAST_ON_CONCURRENT_MODIFICATION, SUPPORTS_PUT})
  @CollectionSize.Require(absent = ZERO)
  public void testPutAbsentConcurrentWithEntrySetIteration() {
    assertThrows(
        ConcurrentModificationException.class,
        () -> {
          Iterator<Entry<K, V>> iterator = getMap().entrySet().iterator();
          put(e3());
          iterator.next();
        });
  }

  @MapFeature.Require({FAILS_FAST_ON_CONCURRENT_MODIFICATION, SUPPORTS_PUT})
  @CollectionSize.Require(absent = ZERO)
  public void testPutAbsentConcurrentWithKeySetIteration() {
    assertThrows(
        ConcurrentModificationException.class,
        () -> {
          Iterator<K> iterator = getMap().keySet().iterator();
          put(e3());
          iterator.next();
        });
  }

  @MapFeature.Require({FAILS_FAST_ON_CONCURRENT_MODIFICATION, SUPPORTS_PUT})
  @CollectionSize.Require(absent = ZERO)
  public void testPutAbsentConcurrentWithValueIteration() {
    assertThrows(
        ConcurrentModificationException.class,
        () -> {
          Iterator<V> iterator = getMap().values().iterator();
          put(e3());
          iterator.next();
        });
  }

  @MapFeature.Require(absent = SUPPORTS_PUT)
  public void testPut_unsupportedNotPresent() {
    assertThrows(UnsupportedOperationException.class, () -> put(e3()));
    expectUnchanged();
    expectMissing(e3());
  }

  @MapFeature.Require(absent = SUPPORTS_PUT)
  @CollectionSize.Require(absent = ZERO)
  public void testPut_unsupportedPresentExistingValue() {
    try {
      assertEquals("put(present, existingValue) should return present or throw", v0(), put(e0()));
    } catch (UnsupportedOperationException tolerated) {
    }
    expectUnchanged();
  }

  @MapFeature.Require(absent = SUPPORTS_PUT)
  @CollectionSize.Require(absent = ZERO)
  public void testPut_unsupportedPresentDifferentValue() {
    assertThrows(UnsupportedOperationException.class, () -> getMap().put(k0(), v3()));
    expectUnchanged();
  }

  @MapFeature.Require({SUPPORTS_PUT, ALLOWS_NULL_KEYS})
  public void testPut_nullKeySupportedNotPresent() {
    assertNull("put(null, value) should return null", put(nullKeyEntry));
    expectAdded(nullKeyEntry);
  }

  @MapFeature.Require({SUPPORTS_PUT, ALLOWS_NULL_KEYS})
  @CollectionSize.Require(absent = ZERO)
  public void testPut_nullKeySupportedPresent() {
    Entry<K, V> newEntry = entry(null, v3());
    initMapWithNullKey();
    assertEquals(
        "put(present, value) should return the associated value",
        getValueForNullKey(),
        put(newEntry));

    Entry<K, V>[] expected = createArrayWithNullKey();
    expected[getNullLocation()] = newEntry;
    expectContents(expected);
  }

  @MapFeature.Require(value = SUPPORTS_PUT, absent = ALLOWS_NULL_KEYS)
  public void testPut_nullKeyUnsupported() {
    assertThrows(NullPointerException.class, () -> put(nullKeyEntry));
    expectUnchanged();
    expectNullKeyMissingWhenNullKeysUnsupported(
        "Should not contain null key after unsupported put(null, value)");
  }

  @MapFeature.Require({SUPPORTS_PUT, ALLOWS_NULL_VALUES})
  public void testPut_nullValueSupported() {
    assertNull("put(key, null) should return null", put(nullValueEntry));
    expectAdded(nullValueEntry);
  }

  @MapFeature.Require(value = SUPPORTS_PUT, absent = ALLOWS_NULL_VALUES)
  public void testPut_nullValueUnsupported() {
    assertThrows(NullPointerException.class, () -> put(nullValueEntry));
    expectUnchanged();
    expectNullValueMissingWhenNullValuesUnsupported(
        "Should not contain null value after unsupported put(key, null)");
  }

  @MapFeature.Require({SUPPORTS_PUT, ALLOWS_NULL_VALUES})
  @CollectionSize.Require(absent = ZERO)
  public void testPut_replaceWithNullValueSupported() {
    assertEquals(
        "put(present, null) should return the associated value",
        v0(),
        put(presentKeyNullValueEntry));
    expectReplacement(presentKeyNullValueEntry);
  }

  @MapFeature.Require(value = SUPPORTS_PUT, absent = ALLOWS_NULL_VALUES)
  @CollectionSize.Require(absent = ZERO)
  public void testPut_replaceWithNullValueUnsupported() {
    assertThrows(NullPointerException.class, () -> put(presentKeyNullValueEntry));
    expectUnchanged();
    expectNullValueMissingWhenNullValuesUnsupported(
        "Should not contain null after unsupported put(present, null)");
  }

  @MapFeature.Require({SUPPORTS_PUT, ALLOWS_NULL_VALUES})
  @CollectionSize.Require(absent = ZERO)
  public void testPut_replaceNullValueWithNullSupported() {
    initMapWithNullValue();
    assertNull(
        "put(present, null) should return the associated value (null)",
        getMap().put(getKeyForNullValue(), null));
    expectContents(createArrayWithNullValue());
  }

  @MapFeature.Require({SUPPORTS_PUT, ALLOWS_NULL_VALUES})
  @CollectionSize.Require(absent = ZERO)
  public void testPut_replaceNullValueWithNonNullSupported() {
    Entry<K, V> newEntry = entry(getKeyForNullValue(), v3());
    initMapWithNullValue();
    assertNull("put(present, value) should return the associated value (null)", put(newEntry));

    Entry<K, V>[] expected = createArrayWithNullValue();
    expected[getNullLocation()] = newEntry;
    expectContents(expected);
  }

  @MapFeature.Require({SUPPORTS_PUT, ALLOWS_NULL_KEYS, ALLOWS_NULL_VALUES})
  public void testPut_nullKeyAndValueSupported() {
    assertNull("put(null, null) should return null", put(nullKeyValueEntry));
    expectAdded(nullKeyValueEntry);
  }

  @CanIgnoreReturnValue
  private V put(Entry<K, V> entry) {
    return getMap().put(entry.getKey(), entry.getValue());
  }

  /**
   * Returns the {@link Method} instance for {@link #testPut_nullKeyUnsupported()} so that tests of
   * {@link java.util.TreeMap} can suppress it with {@code
   * FeatureSpecificTestSuiteBuilder.suppressing()} until <a
   * href="https://bugs.openjdk.org/browse/JDK-5045147">JDK-5045147</a> is fixed.
   */
  @J2ktIncompatible
  @GwtIncompatible // reflection
  public static Method getPutNullKeyUnsupportedMethod() {
    return getMethod(MapPutTester.class, "testPut_nullKeyUnsupported");
  }
}
