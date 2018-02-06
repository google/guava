/*
 * Copyright (C) 2008 The Guava Authors
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

import static com.google.common.collect.testing.features.CollectionSize.SEVERAL;
import static com.google.common.collect.testing.features.CollectionSize.ZERO;
import static com.google.common.collect.testing.features.MapFeature.ALLOWS_NULL_KEYS;
import static com.google.common.collect.testing.features.MapFeature.ALLOWS_NULL_KEY_QUERIES;
import static com.google.common.collect.testing.features.MapFeature.FAILS_FAST_ON_CONCURRENT_MODIFICATION;
import static com.google.common.collect.testing.features.MapFeature.SUPPORTS_REMOVE;

import com.google.common.annotations.GwtCompatible;
import com.google.common.collect.testing.AbstractMapTester;
import com.google.common.collect.testing.WrongType;
import com.google.common.collect.testing.features.CollectionSize;
import com.google.common.collect.testing.features.MapFeature;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.Map.Entry;
import org.junit.Ignore;

/**
 * A generic JUnit test which tests {@code remove} operations on a map. Can't be invoked directly;
 * please see {@link com.google.common.collect.testing.MapTestSuiteBuilder}.
 *
 * @author George van den Driessche
 * @author Chris Povirk
 */
@SuppressWarnings("unchecked") // too many "unchecked generic array creations"
@GwtCompatible
@Ignore // Affects only Android test runner, which respects JUnit 4 annotations on JUnit 3 tests.
public class MapRemoveTester<K, V> extends AbstractMapTester<K, V> {
  @MapFeature.Require(SUPPORTS_REMOVE)
  @CollectionSize.Require(absent = ZERO)
  public void testRemove_present() {
    int initialSize = getMap().size();
    assertEquals("remove(present) should return the associated value", v0(), getMap().remove(k0()));
    assertEquals(
        "remove(present) should decrease a map's size by one.", initialSize - 1, getMap().size());
    expectMissing(e0());
  }

  @MapFeature.Require({FAILS_FAST_ON_CONCURRENT_MODIFICATION, SUPPORTS_REMOVE})
  @CollectionSize.Require(SEVERAL)
  public void testRemovePresentConcurrentWithEntrySetIteration() {
    try {
      Iterator<Entry<K, V>> iterator = getMap().entrySet().iterator();
      getMap().remove(k0());
      iterator.next();
      fail("Expected ConcurrentModificationException");
    } catch (ConcurrentModificationException expected) {
      // success
    }
  }

  @MapFeature.Require({FAILS_FAST_ON_CONCURRENT_MODIFICATION, SUPPORTS_REMOVE})
  @CollectionSize.Require(SEVERAL)
  public void testRemovePresentConcurrentWithKeySetIteration() {
    try {
      Iterator<K> iterator = getMap().keySet().iterator();
      getMap().remove(k0());
      iterator.next();
      fail("Expected ConcurrentModificationException");
    } catch (ConcurrentModificationException expected) {
      // success
    }
  }

  @MapFeature.Require({FAILS_FAST_ON_CONCURRENT_MODIFICATION, SUPPORTS_REMOVE})
  @CollectionSize.Require(SEVERAL)
  public void testRemovePresentConcurrentWithValuesIteration() {
    try {
      Iterator<V> iterator = getMap().values().iterator();
      getMap().remove(k0());
      iterator.next();
      fail("Expected ConcurrentModificationException");
    } catch (ConcurrentModificationException expected) {
      // success
    }
  }

  @MapFeature.Require(SUPPORTS_REMOVE)
  public void testRemove_notPresent() {
    assertNull("remove(notPresent) should return null", getMap().remove(k3()));
    expectUnchanged();
  }

  @MapFeature.Require({SUPPORTS_REMOVE, ALLOWS_NULL_KEYS})
  @CollectionSize.Require(absent = ZERO)
  public void testRemove_nullPresent() {
    initMapWithNullKey();

    int initialSize = getMap().size();
    assertEquals(
        "remove(null) should return the associated value",
        getValueForNullKey(),
        getMap().remove(null));
    assertEquals(
        "remove(present) should decrease a map's size by one.", initialSize - 1, getMap().size());
    expectMissing(entry(null, getValueForNullKey()));
  }

  @MapFeature.Require(absent = SUPPORTS_REMOVE)
  @CollectionSize.Require(absent = ZERO)
  public void testRemove_unsupported() {
    try {
      getMap().remove(k0());
      fail("remove(present) should throw UnsupportedOperationException");
    } catch (UnsupportedOperationException expected) {
    }
    expectUnchanged();
    assertEquals("remove(present) should not remove the element", v0(), get(k0()));
  }

  @MapFeature.Require(absent = SUPPORTS_REMOVE)
  public void testRemove_unsupportedNotPresent() {
    try {
      assertNull(
          "remove(notPresent) should return null or throw UnsupportedOperationException",
          getMap().remove(k3()));
    } catch (UnsupportedOperationException tolerated) {
    }
    expectUnchanged();
    expectMissing(e3());
  }

  @MapFeature.Require(value = SUPPORTS_REMOVE, absent = ALLOWS_NULL_KEY_QUERIES)
  public void testRemove_nullQueriesNotSupported() {
    try {
      assertNull(
          "remove(null) should return null or throw NullPointerException", getMap().remove(null));
    } catch (NullPointerException tolerated) {
    }
    expectUnchanged();
  }

  @MapFeature.Require({SUPPORTS_REMOVE, ALLOWS_NULL_KEY_QUERIES})
  public void testRemove_nullSupportedMissing() {
    assertNull("remove(null) should return null", getMap().remove(null));
    expectUnchanged();
  }

  @MapFeature.Require(SUPPORTS_REMOVE)
  public void testRemove_wrongType() {
    try {
      assertNull(getMap().remove(WrongType.VALUE));
    } catch (ClassCastException tolerated) {
    }
    expectUnchanged();
  }
}
