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
import static com.google.common.collect.testing.features.CollectionSize.ONE;
import static com.google.common.collect.testing.features.CollectionSize.ZERO;
import static com.google.common.collect.testing.features.MapFeature.ALLOWS_NULL_KEYS;
import static com.google.common.collect.testing.features.MapFeature.ALLOWS_NULL_VALUES;
import static com.google.common.collect.testing.features.MapFeature.REJECTS_DUPLICATES_AT_CREATION;
import static com.google.common.collect.testing.testers.ReflectionFreeAssertThrows.assertThrows;
import static java.util.Arrays.asList;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.annotations.J2ktIncompatible;
import com.google.common.collect.testing.AbstractMapTester;
import com.google.common.collect.testing.features.CollectionSize;
import com.google.common.collect.testing.features.MapFeature;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map.Entry;
import org.junit.Ignore;

/**
 * A generic JUnit test which tests creation (typically through a constructor or static factory
 * method) of a map. Can't be invoked directly; please see {@link
 * com.google.common.collect.testing.MapTestSuiteBuilder}.
 *
 * @author Chris Povirk
 * @author Kevin Bourrillion
 */
@GwtCompatible(emulated = true)
@Ignore("test runners must not instantiate and run this directly, only via suites we build")
// @Ignore affects the Android test runner, which respects JUnit 4 annotations on JUnit 3 tests.
@SuppressWarnings("JUnit4ClassUsedInJUnit3")
public class MapCreationTester<K, V> extends AbstractMapTester<K, V> {
  @MapFeature.Require(ALLOWS_NULL_KEYS)
  @CollectionSize.Require(absent = ZERO)
  public void testCreateWithNullKeySupported() {
    initMapWithNullKey();
    expectContents(createArrayWithNullKey());
  }

  @MapFeature.Require(absent = ALLOWS_NULL_KEYS)
  @CollectionSize.Require(absent = ZERO)
  public void testCreateWithNullKeyUnsupported() {
    assertThrows(NullPointerException.class, () -> initMapWithNullKey());
  }

  @MapFeature.Require(ALLOWS_NULL_VALUES)
  @CollectionSize.Require(absent = ZERO)
  public void testCreateWithNullValueSupported() {
    initMapWithNullValue();
    expectContents(createArrayWithNullValue());
  }

  @MapFeature.Require(absent = ALLOWS_NULL_VALUES)
  @CollectionSize.Require(absent = ZERO)
  public void testCreateWithNullValueUnsupported() {
    assertThrows(NullPointerException.class, () -> initMapWithNullValue());
  }

  @MapFeature.Require({ALLOWS_NULL_KEYS, ALLOWS_NULL_VALUES})
  @CollectionSize.Require(absent = ZERO)
  public void testCreateWithNullKeyAndValueSupported() {
    Entry<K, V>[] entries = createSamplesArray();
    entries[getNullLocation()] = entry(null, null);
    resetMap(entries);
    expectContents(entries);
  }

  @MapFeature.Require(value = ALLOWS_NULL_KEYS, absent = REJECTS_DUPLICATES_AT_CREATION)
  @CollectionSize.Require(absent = {ZERO, ONE})
  public void testCreateWithDuplicates_nullDuplicatesNotRejected() {
    expectFirstRemoved(getEntriesMultipleNullKeys());
  }

  @MapFeature.Require(absent = REJECTS_DUPLICATES_AT_CREATION)
  @CollectionSize.Require(absent = {ZERO, ONE})
  public void testCreateWithDuplicates_nonNullDuplicatesNotRejected() {
    expectFirstRemoved(getEntriesMultipleNonNullKeys());
  }

  @MapFeature.Require({ALLOWS_NULL_KEYS, REJECTS_DUPLICATES_AT_CREATION})
  @CollectionSize.Require(absent = {ZERO, ONE})
  public void testCreateWithDuplicates_nullDuplicatesRejected() {
    Entry<K, V>[] entries = getEntriesMultipleNullKeys();
    assertThrows(IllegalArgumentException.class, () -> resetMap(entries));
  }

  @MapFeature.Require(REJECTS_DUPLICATES_AT_CREATION)
  @CollectionSize.Require(absent = {ZERO, ONE})
  public void testCreateWithDuplicates_nonNullDuplicatesRejected() {
    Entry<K, V>[] entries = getEntriesMultipleNonNullKeys();
    assertThrows(IllegalArgumentException.class, () -> resetMap(entries));
  }

  private Entry<K, V>[] getEntriesMultipleNullKeys() {
    Entry<K, V>[] entries = createArrayWithNullKey();
    entries[0] = entry(null, entries[0].getValue());
    return entries;
  }

  private Entry<K, V>[] getEntriesMultipleNonNullKeys() {
    Entry<K, V>[] entries = createSamplesArray();
    entries[0] = entry(k1(), v0());
    return entries;
  }

  private void expectFirstRemoved(Entry<K, V>[] entries) {
    resetMap(entries);

    List<Entry<K, V>> expectedWithDuplicateRemoved = asList(entries).subList(1, getNumElements());
    expectContents(expectedWithDuplicateRemoved);
  }

  /**
   * Returns the {@link Method} instance for {@link #testCreateWithNullKeyUnsupported()} so that
   * tests can suppress it with {@code FeatureSpecificTestSuiteBuilder.suppressing()} until <a
   * href="https://bugs.openjdk.org/browse/JDK-5045147">JDK-5045147</a> is fixed.
   */
  @J2ktIncompatible
  @GwtIncompatible // reflection
  public static Method getCreateWithNullKeyUnsupportedMethod() {
    return getMethod(MapCreationTester.class, "testCreateWithNullKeyUnsupported");
  }
}
