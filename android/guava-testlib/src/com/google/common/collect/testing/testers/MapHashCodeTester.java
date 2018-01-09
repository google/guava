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

import static com.google.common.collect.testing.features.MapFeature.ALLOWS_NULL_KEYS;
import static com.google.common.collect.testing.features.MapFeature.ALLOWS_NULL_VALUES;

import com.google.common.annotations.GwtCompatible;
import com.google.common.collect.testing.AbstractMapTester;
import com.google.common.collect.testing.features.CollectionSize;
import com.google.common.collect.testing.features.MapFeature;
import java.util.Collection;
import java.util.Map.Entry;
import org.junit.Ignore;

/**
 * Tests {@link java.util.Map#hashCode}.
 *
 * @author George van den Driessche
 * @author Chris Povirk
 */
@GwtCompatible
@Ignore // Affects only Android test runner, which respects JUnit 4 annotations on JUnit 3 tests.
public class MapHashCodeTester<K, V> extends AbstractMapTester<K, V> {
  public void testHashCode() {
    int expectedHashCode = 0;
    for (Entry<K, V> entry : getSampleEntries()) {
      expectedHashCode += hash(entry);
    }
    assertEquals(
        "A Map's hashCode() should be the sum of those of its entries.",
        expectedHashCode,
        getMap().hashCode());
  }

  @CollectionSize.Require(absent = CollectionSize.ZERO)
  @MapFeature.Require(ALLOWS_NULL_KEYS)
  public void testHashCode_containingNullKey() {
    Entry<K, V> entryWithNull = entry(null, v3());
    runEntryWithNullTest(entryWithNull);
  }

  @CollectionSize.Require(absent = CollectionSize.ZERO)
  @MapFeature.Require(ALLOWS_NULL_VALUES)
  public void testHashCode_containingNullValue() {
    Entry<K, V> entryWithNull = entry(k3(), null);
    runEntryWithNullTest(entryWithNull);
  }

  private void runEntryWithNullTest(Entry<K, V> entryWithNull) {
    Collection<Entry<K, V>> entries = getSampleEntries(getNumEntries() - 1);

    entries.add(entryWithNull);

    int expectedHashCode = 0;
    for (Entry<K, V> entry : entries) {
      expectedHashCode += hash(entry);
    }

    resetContainer(getSubjectGenerator().create(entries.toArray()));
    assertEquals(
        "A Map's hashCode() should be the sum of those of its entries (where "
            + "a null element in an entry counts as having a hash of zero).",
        expectedHashCode,
        getMap().hashCode());
  }

  private static int hash(Entry<?, ?> e) {
    return (e.getKey() == null ? 0 : e.getKey().hashCode())
        ^ (e.getValue() == null ? 0 : e.getValue().hashCode());
  }
}
