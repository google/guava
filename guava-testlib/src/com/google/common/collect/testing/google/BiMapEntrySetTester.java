/*
 * Copyright (C) 2017 The Guava Authors
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

import static com.google.common.collect.testing.features.CollectionSize.SEVERAL;
import static com.google.common.collect.testing.features.CollectionSize.ZERO;
import static com.google.common.collect.testing.features.MapFeature.ALLOWS_NULL_VALUES;
import static com.google.common.collect.testing.features.MapFeature.SUPPORTS_PUT;

import com.google.common.annotations.GwtCompatible;
import com.google.common.collect.testing.features.CollectionSize;
import com.google.common.collect.testing.features.MapFeature;
import java.util.Map.Entry;
import org.junit.Ignore;

/** Tester for {@code BiMap.entrySet} and methods on the entries in the set. */
@GwtCompatible
@Ignore // Affects only Android test runner, which respects JUnit 4 annotations on JUnit 3 tests.
public class BiMapEntrySetTester<K, V> extends AbstractBiMapTester<K, V> {
  @MapFeature.Require(SUPPORTS_PUT)
  @CollectionSize.Require(absent = ZERO)
  public void testSetValue_valueAbsent() {
    for (Entry<K, V> entry : getMap().entrySet()) {
      if (entry.getKey().equals(k0())) {
        assertEquals("entry.setValue() should return the old value", v0(), entry.setValue(v3()));
      }
    }
    expectReplacement(entry(k0(), v3()));
  }

  @MapFeature.Require(SUPPORTS_PUT)
  @CollectionSize.Require(SEVERAL)
  public void testSetValue_valuePresent() {
    for (Entry<K, V> entry : getMap().entrySet()) {
      if (entry.getKey().equals(k0())) {
        try {
          entry.setValue(v1());
          fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
        }
      }
    }
    expectUnchanged();
  }

  @MapFeature.Require(value = SUPPORTS_PUT, absent = ALLOWS_NULL_VALUES)
  @CollectionSize.Require(absent = ZERO)
  public void testSetValueNullUnsupported() {
    for (Entry<K, V> entry : getMap().entrySet()) {
      try {
        entry.setValue(null);
        fail("Expected NullPointerException");
      } catch (NullPointerException expected) {
      }
      expectUnchanged();
    }
  }

  @MapFeature.Require({SUPPORTS_PUT, ALLOWS_NULL_VALUES})
  @CollectionSize.Require(absent = ZERO)
  public void testSetValueNullSupported() {
    for (Entry<K, V> entry : getMap().entrySet()) {
      if (entry.getKey().equals(k0())) {
        entry.setValue(null);
      }
    }
    expectReplacement(entry(k0(), (V) null));
  }
}
