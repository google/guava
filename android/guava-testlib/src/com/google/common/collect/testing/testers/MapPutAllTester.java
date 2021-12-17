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
import static com.google.common.collect.testing.features.MapFeature.ALLOWS_NULL_VALUES;
import static com.google.common.collect.testing.features.MapFeature.FAILS_FAST_ON_CONCURRENT_MODIFICATION;
import static com.google.common.collect.testing.features.MapFeature.SUPPORTS_PUT;
import static java.util.Collections.singletonList;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.collect.testing.AbstractMapTester;
import com.google.common.collect.testing.Helpers;
import com.google.common.collect.testing.MinimalCollection;
import com.google.common.collect.testing.features.CollectionSize;
import com.google.common.collect.testing.features.MapFeature;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.junit.Ignore;

/**
 * A generic JUnit test which tests {@code putAll} operations on a map. Can't be invoked directly;
 * please see {@link com.google.common.collect.testing.MapTestSuiteBuilder}.
 *
 * @author Chris Povirk
 * @author Kevin Bourrillion
 */
@SuppressWarnings("unchecked") // too many "unchecked generic array creations"
@GwtCompatible(emulated = true)
@Ignore // Affects only Android test runner, which respects JUnit 4 annotations on JUnit 3 tests.
public class MapPutAllTester<K, V> extends AbstractMapTester<K, V> {
  private List<Entry<K, V>> containsNullKey;
  private List<Entry<K, V>> containsNullValue;

  @Override
  public void setUp() throws Exception {
    super.setUp();
    containsNullKey = singletonList(entry(null, v3()));
    containsNullValue = singletonList(entry(k3(), null));
  }

  @MapFeature.Require(SUPPORTS_PUT)
  public void testPutAll_supportedNothing() {
    getMap().putAll(emptyMap());
    expectUnchanged();
  }

  @MapFeature.Require(absent = SUPPORTS_PUT)
  public void testPutAll_unsupportedNothing() {
    try {
      getMap().putAll(emptyMap());
    } catch (UnsupportedOperationException tolerated) {
    }
    expectUnchanged();
  }

  @MapFeature.Require(SUPPORTS_PUT)
  public void testPutAll_supportedNonePresent() {
    putAll(createDisjointCollection());
    expectAdded(e3(), e4());
  }

  @MapFeature.Require(absent = SUPPORTS_PUT)
  public void testPutAll_unsupportedNonePresent() {
    try {
      putAll(createDisjointCollection());
      fail("putAll(nonePresent) should throw");
    } catch (UnsupportedOperationException expected) {
    }
    expectUnchanged();
    expectMissing(e3(), e4());
  }

  @MapFeature.Require(SUPPORTS_PUT)
  @CollectionSize.Require(absent = ZERO)
  public void testPutAll_supportedSomePresent() {
    putAll(MinimalCollection.of(e3(), e0()));
    expectAdded(e3());
  }

  @MapFeature.Require({FAILS_FAST_ON_CONCURRENT_MODIFICATION, SUPPORTS_PUT})
  @CollectionSize.Require(absent = ZERO)
  public void testPutAllSomePresentConcurrentWithEntrySetIteration() {
    try {
      Iterator<Entry<K, V>> iterator = getMap().entrySet().iterator();
      putAll(MinimalCollection.of(e3(), e0()));
      iterator.next();
      fail("Expected ConcurrentModificationException");
    } catch (ConcurrentModificationException expected) {
      // success
    }
  }

  @MapFeature.Require(absent = SUPPORTS_PUT)
  @CollectionSize.Require(absent = ZERO)
  public void testPutAll_unsupportedSomePresent() {
    try {
      putAll(MinimalCollection.of(e3(), e0()));
      fail("putAll(somePresent) should throw");
    } catch (UnsupportedOperationException expected) {
    }
    expectUnchanged();
  }

  @MapFeature.Require(absent = SUPPORTS_PUT)
  @CollectionSize.Require(absent = ZERO)
  public void testPutAll_unsupportedAllPresent() {
    try {
      putAll(MinimalCollection.of(e0()));
    } catch (UnsupportedOperationException tolerated) {
    }
    expectUnchanged();
  }

  @MapFeature.Require({SUPPORTS_PUT, ALLOWS_NULL_KEYS})
  public void testPutAll_nullKeySupported() {
    putAll(containsNullKey);
    expectAdded(containsNullKey.get(0));
  }

  @MapFeature.Require(value = SUPPORTS_PUT, absent = ALLOWS_NULL_KEYS)
  public void testPutAll_nullKeyUnsupported() {
    try {
      putAll(containsNullKey);
      fail("putAll(containsNullKey) should throw");
    } catch (NullPointerException expected) {
    }
    expectUnchanged();
    expectNullKeyMissingWhenNullKeysUnsupported(
        "Should not contain null key after unsupported putAll(containsNullKey)");
  }

  @MapFeature.Require({SUPPORTS_PUT, ALLOWS_NULL_VALUES})
  public void testPutAll_nullValueSupported() {
    putAll(containsNullValue);
    expectAdded(containsNullValue.get(0));
  }

  @MapFeature.Require(value = SUPPORTS_PUT, absent = ALLOWS_NULL_VALUES)
  public void testPutAll_nullValueUnsupported() {
    try {
      putAll(containsNullValue);
      fail("putAll(containsNullValue) should throw");
    } catch (NullPointerException expected) {
    }
    expectUnchanged();
    expectNullValueMissingWhenNullValuesUnsupported(
        "Should not contain null value after unsupported putAll(containsNullValue)");
  }

  @MapFeature.Require(SUPPORTS_PUT)
  public void testPutAll_nullCollectionReference() {
    try {
      getMap().putAll(null);
      fail("putAll(null) should throw NullPointerException");
    } catch (NullPointerException expected) {
    }
  }

  private Map<K, V> emptyMap() {
    return Collections.emptyMap();
  }

  private void putAll(Iterable<Entry<K, V>> entries) {
    Map<K, V> map = new LinkedHashMap<>();
    for (Entry<K, V> entry : entries) {
      map.put(entry.getKey(), entry.getValue());
    }
    getMap().putAll(map);
  }

  /**
   * Returns the {@link Method} instance for {@link #testPutAll_nullKeyUnsupported()} so that tests
   * can suppress it with {@code FeatureSpecificTestSuiteBuilder.suppressing()} until <a
   * href="http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=5045147">Sun bug 5045147</a> is fixed.
   */
  @GwtIncompatible // reflection
  public static Method getPutAllNullKeyUnsupportedMethod() {
    return Helpers.getMethod(MapPutAllTester.class, "testPutAll_nullKeyUnsupported");
  }
}
