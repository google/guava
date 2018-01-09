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
 * A generic JUnit test which tests {@link Map#replace(Object, Object, Object)}. Can't be invoked
 * directly; please see {@link com.google.common.collect.testing.MapTestSuiteBuilder}.
 *
 * @author Louis Wasserman
 */
@GwtCompatible
@Ignore // Affects only Android test runner, which respects JUnit 4 annotations on JUnit 3 tests.
public class MapReplaceEntryTester<K, V> extends AbstractMapTester<K, V> {

  @MapFeature.Require(SUPPORTS_PUT)
  @CollectionSize.Require(absent = ZERO)
  public void testReplaceEntry_supportedPresent() {
    try {
      assertTrue(getMap().replace(k0(), v0(), v3()));
      expectReplacement(entry(k0(), v3()));
    } catch (ClassCastException tolerated) { // for ClassToInstanceMap
      expectUnchanged();
    }
  }

  @MapFeature.Require(SUPPORTS_PUT)
  @CollectionSize.Require(absent = ZERO)
  public void testReplaceEntry_supportedPresentUnchanged() {
    assertTrue(getMap().replace(k0(), v0(), v0()));
    expectUnchanged();
  }

  @MapFeature.Require(SUPPORTS_PUT)
  @CollectionSize.Require(absent = ZERO)
  public void testReplaceEntry_supportedWrongValue() {
    assertFalse(getMap().replace(k0(), v3(), v4()));
    expectUnchanged();
  }

  @MapFeature.Require(SUPPORTS_PUT)
  public void testReplaceEntry_supportedAbsentKey() {
    assertFalse(getMap().replace(k3(), v3(), v4()));
    expectUnchanged();
  }

  @MapFeature.Require(value = SUPPORTS_PUT, absent = ALLOWS_NULL_VALUES)
  @CollectionSize.Require(absent = ZERO)
  public void testReplaceEntry_presentNullValueUnsupported() {
    try {
      getMap().replace(k0(), v0(), null);
      fail("Expected NullPointerException");
    } catch (NullPointerException expected) {
    }
    expectUnchanged();
  }

  @MapFeature.Require(value = SUPPORTS_PUT, absent = ALLOWS_NULL_VALUE_QUERIES)
  @CollectionSize.Require(absent = ZERO)
  public void testReplaceEntry_wrongValueNullValueUnsupported() {
    try {
      assertFalse(getMap().replace(k0(), v3(), null));
    } catch (NullPointerException tolerated) {
      // the operation would be a no-op, so exceptions are allowed but not required
    }
    expectUnchanged();
  }

  @MapFeature.Require(value = SUPPORTS_PUT, absent = ALLOWS_NULL_VALUE_QUERIES)
  public void testReplaceEntry_absentKeyNullValueUnsupported() {
    try {
      assertFalse(getMap().replace(k3(), v3(), null));
    } catch (NullPointerException tolerated) {
      // the operation would be a no-op, so exceptions are allowed but not required
    }
    expectUnchanged();
  }

  @MapFeature.Require({SUPPORTS_PUT, ALLOWS_NULL_VALUE_QUERIES})
  public void testReplaceEntry_nullDifferentFromAbsent() {
    assertFalse(getMap().replace(k3(), null, v3()));
    expectUnchanged();
  }

  @MapFeature.Require(value = SUPPORTS_PUT, absent = ALLOWS_NULL_VALUE_QUERIES)
  public void testReplaceEntry_expectNullUnsupported() {
    try {
      assertFalse(getMap().replace(k3(), null, v3()));
    } catch (NullPointerException tolerated) {
      // the operation would be a no-op, so exceptions are allowed but not required
    }
    expectUnchanged();
  }

  @MapFeature.Require(absent = SUPPORTS_PUT)
  @CollectionSize.Require(absent = ZERO)
  public void testReplaceEntry_unsupportedPresent() {
    try {
      getMap().replace(k0(), v0(), v3());
      fail("Expected UnsupportedOperationException");
    } catch (UnsupportedOperationException expected) {
    }
    expectUnchanged();
  }

  @MapFeature.Require(absent = SUPPORTS_PUT)
  @CollectionSize.Require(absent = ZERO)
  public void testReplaceEntry_unsupportedWrongValue() {
    try {
      getMap().replace(k0(), v3(), v4());
    } catch (UnsupportedOperationException tolerated) {
      // the operation would be a no-op, so exceptions are allowed but not required
    }
    expectUnchanged();
  }

  @MapFeature.Require(absent = SUPPORTS_PUT)
  public void testReplaceEntry_unsupportedAbsentKey() {
    try {
      getMap().replace(k3(), v3(), v4());
    } catch (UnsupportedOperationException tolerated) {
      // the operation would be a no-op, so exceptions are allowed but not required
    }
    expectUnchanged();
  }
}
