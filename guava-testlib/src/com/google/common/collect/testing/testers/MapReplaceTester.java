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
import static com.google.common.collect.testing.features.MapFeature.ALLOWS_NULL_KEY_QUERIES;
import static com.google.common.collect.testing.features.MapFeature.ALLOWS_NULL_VALUES;
import static com.google.common.collect.testing.features.MapFeature.ALLOWS_NULL_VALUE_QUERIES;
import static com.google.common.collect.testing.features.MapFeature.SUPPORTS_PUT;

import com.google.common.annotations.GwtCompatible;
import com.google.common.collect.testing.AbstractMapTester;
import com.google.common.collect.testing.features.CollectionSize;
import com.google.common.collect.testing.features.MapFeature;
import java.util.Map;
import org.junit.Ignore;

/**
 * A generic JUnit test which tests {@link Map#replace(Object, Object)}. Can't be invoked directly;
 * please see {@link com.google.common.collect.testing.ConcurrentMapTestSuiteBuilder}.
 *
 * @author Louis Wasserman
 */
@GwtCompatible
@Ignore // Affects only Android test runner, which respects JUnit 4 annotations on JUnit 3 tests.
public class MapReplaceTester<K, V> extends AbstractMapTester<K, V> {

  @MapFeature.Require(SUPPORTS_PUT)
  @CollectionSize.Require(absent = ZERO)
  public void testReplace_supportedPresent() {
    try {
      assertEquals(v0(), getMap().replace(k0(), v3()));
      expectReplacement(entry(k0(), v3()));
    } catch (ClassCastException tolerated) { // for ClassToInstanceMap
      expectUnchanged();
    }
  }

  @MapFeature.Require(SUPPORTS_PUT)
  @CollectionSize.Require(absent = ZERO)
  public void testReplace_supportedPresentNoChange() {
    assertEquals(v0(), getMap().replace(k0(), v0()));
    expectUnchanged();
  }

  @MapFeature.Require(SUPPORTS_PUT)
  public void testReplace_supportedAbsent() {
    assertNull(getMap().replace(k3(), v3()));
    expectUnchanged();
  }

  @MapFeature.Require(value = SUPPORTS_PUT, absent = ALLOWS_NULL_VALUES)
  @CollectionSize.Require(absent = ZERO)
  public void testReplace_presentNullValueUnsupported() {
    try {
      getMap().replace(k0(), null);
      fail("Expected NullPointerException");
    } catch (NullPointerException expected) {
    }
    expectUnchanged();
  }

  @MapFeature.Require(value = SUPPORTS_PUT, absent = ALLOWS_NULL_VALUE_QUERIES)
  public void testReplace_absentNullValueUnsupported() {
    try {
      getMap().replace(k3(), null);
    } catch (NullPointerException tolerated) {
      // permitted not to throw because it would be a no-op
    }
    expectUnchanged();
  }

  @MapFeature.Require(value = SUPPORTS_PUT, absent = ALLOWS_NULL_KEY_QUERIES)
  public void testReplace_absentNullKeyUnsupported() {
    try {
      getMap().replace(null, v3());
    } catch (NullPointerException tolerated) {
      // permitted not to throw because it would be a no-op
    }
    expectUnchanged();
  }

  @MapFeature.Require(absent = SUPPORTS_PUT)
  @CollectionSize.Require(absent = ZERO)
  public void testReplace_unsupportedPresent() {
    try {
      getMap().replace(k0(), v3());
      fail("Expected UnsupportedOperationException");
    } catch (UnsupportedOperationException expected) {
    } catch (ClassCastException tolerated) {
      // for ClassToInstanceMap
    }

    expectUnchanged();
  }
}
