/*
 * Copyright (C) 2012 The Guava Authors
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

package com.google.common.collect.testing.google;

import static com.google.common.collect.testing.features.CollectionSize.ZERO;
import static com.google.common.collect.testing.features.MapFeature.ALLOWS_NULL_KEYS;
import static com.google.common.collect.testing.features.MapFeature.ALLOWS_NULL_KEY_QUERIES;
import static com.google.common.collect.testing.features.MapFeature.ALLOWS_NULL_VALUES;
import static com.google.common.collect.testing.features.MapFeature.ALLOWS_NULL_VALUE_QUERIES;

import com.google.common.annotations.GwtCompatible;
import com.google.common.collect.Multimap;
import com.google.common.collect.testing.features.CollectionSize;
import com.google.common.collect.testing.features.MapFeature;
import org.junit.Ignore;

/**
 * Tester for {@link Multimap#containsEntry}.
 *
 * @author Louis Wasserman
 */
@GwtCompatible
@Ignore // Affects only Android test runner, which respects JUnit 4 annotations on JUnit 3 tests.
public class MultimapContainsEntryTester<K, V>
    extends AbstractMultimapTester<K, V, Multimap<K, V>> {
  @CollectionSize.Require(absent = ZERO)
  public void testContainsEntryYes() {
    assertTrue(multimap().containsEntry(k0(), v0()));
  }

  public void testContainsEntryNo() {
    assertFalse(multimap().containsEntry(k3(), v3()));
  }

  public void testContainsEntryAgreesWithGet() {
    for (K k : sampleKeys()) {
      for (V v : sampleValues()) {
        assertEquals(multimap().get(k).contains(v), multimap().containsEntry(k, v));
      }
    }
  }

  @CollectionSize.Require(absent = ZERO)
  @MapFeature.Require({ALLOWS_NULL_KEYS, ALLOWS_NULL_VALUES})
  public void testContainsEntryNullYes() {
    initMultimapWithNullKeyAndValue();
    assertTrue(multimap().containsEntry(null, null));
  }

  @MapFeature.Require({ALLOWS_NULL_KEY_QUERIES, ALLOWS_NULL_VALUE_QUERIES})
  public void testContainsEntryNullNo() {
    assertFalse(multimap().containsEntry(null, null));
  }

  @MapFeature.Require(absent = ALLOWS_NULL_KEY_QUERIES)
  public void testContainsEntryNullDisallowedBecauseKeyQueriesDisallowed() {
    try {
      multimap().containsEntry(null, v3());
      fail("Expected NullPointerException");
    } catch (NullPointerException expected) {
      // success
    }
  }

  @MapFeature.Require(absent = ALLOWS_NULL_VALUE_QUERIES)
  public void testContainsEntryNullDisallowedBecauseValueQueriesDisallowed() {
    try {
      multimap().containsEntry(k3(), null);
      fail("Expected NullPointerException");
    } catch (NullPointerException expected) {
      // success
    }
  }
}
