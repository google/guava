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
import static com.google.common.collect.testing.features.MapFeature.ALLOWS_NULL_VALUE_QUERIES;
import static com.google.common.collect.testing.features.MapFeature.SUPPORTS_REMOVE;

import com.google.common.annotations.GwtCompatible;
import com.google.common.collect.testing.AbstractMapTester;
import com.google.common.collect.testing.features.CollectionSize;
import com.google.common.collect.testing.features.MapFeature;
import java.util.concurrent.ConcurrentMap;
import org.junit.Ignore;

/**
 * Tester for {@link ConcurrentMap#remove}. Can't be invoked directly; please see {@link
 * com.google.common.collect.testing.ConcurrentMapTestSuiteBuilder}.
 *
 * @author Louis Wasserman
 */
@GwtCompatible
@Ignore // Affects only Android test runner, which respects JUnit 4 annotations on JUnit 3 tests.
public class ConcurrentMapRemoveTester<K, V> extends AbstractMapTester<K, V> {
  @Override
  protected ConcurrentMap<K, V> getMap() {
    return (ConcurrentMap<K, V>) super.getMap();
  }

  @MapFeature.Require(SUPPORTS_REMOVE)
  @CollectionSize.Require(absent = ZERO)
  public void testRemove_supportedPresent() {
    assertTrue(getMap().remove(k0(), v0()));
    expectMissing(e0());
  }

  @MapFeature.Require(SUPPORTS_REMOVE)
  public void testRemove_supportedPresentKeyWrongValue() {
    assertFalse(getMap().remove(k0(), v3()));
    expectUnchanged();
  }

  @MapFeature.Require(SUPPORTS_REMOVE)
  public void testRemove_supportedWrongKeyPresentValue() {
    assertFalse(getMap().remove(k3(), v0()));
    expectUnchanged();
  }

  @MapFeature.Require(SUPPORTS_REMOVE)
  public void testRemove_supportedAbsentKeyAbsentValue() {
    assertFalse(getMap().remove(k3(), v3()));
    expectUnchanged();
  }

  @MapFeature.Require(value = SUPPORTS_REMOVE, absent = ALLOWS_NULL_KEY_QUERIES)
  public void testRemove_nullKeyQueriesUnsupported() {
    try {
      assertFalse(getMap().remove(null, v3()));
    } catch (NullPointerException tolerated) {
      // since the operation would be a no-op, the exception is not required
    }
    expectUnchanged();
  }

  @MapFeature.Require(value = SUPPORTS_REMOVE, absent = ALLOWS_NULL_VALUE_QUERIES)
  public void testRemove_nullValueQueriesUnsupported() {
    try {
      assertFalse(getMap().remove(k3(), null));
    } catch (NullPointerException tolerated) {
      // since the operation would be a no-op, the exception is not required
    }
    expectUnchanged();
  }

  @MapFeature.Require(absent = SUPPORTS_REMOVE)
  @CollectionSize.Require(absent = ZERO)
  public void testRemove_unsupportedPresent() {
    try {
      getMap().remove(k0(), v0());
      fail("Expected UnsupportedOperationException");
    } catch (UnsupportedOperationException expected) {
    }
    expectUnchanged();
  }

  @MapFeature.Require(absent = SUPPORTS_REMOVE)
  public void testRemove_unsupportedAbsent() {
    try {
      assertFalse(getMap().remove(k0(), v3()));
    } catch (UnsupportedOperationException tolerated) {
      // since the operation would be a no-op, the exception is not required
    }
    expectUnchanged();
  }
}
