/*
 * Copyright (C) 2013 The Guava Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.common.collect.testing.google;

import static com.google.common.collect.testing.features.CollectionSize.ZERO;
import static com.google.common.collect.testing.features.MapFeature.ALLOWS_NULL_KEYS;
import static com.google.common.collect.testing.features.MapFeature.ALLOWS_NULL_VALUES;

import com.google.common.annotations.GwtCompatible;
import com.google.common.collect.Multimap;
import com.google.common.collect.testing.Helpers;
import com.google.common.collect.testing.features.CollectionSize;
import com.google.common.collect.testing.features.MapFeature;
import com.google.common.testing.EqualsTester;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import org.junit.Ignore;

/**
 * Tester for {@code Multimap.equals}.
 *
 * @author Louis Wasserman
 */
@GwtCompatible
@Ignore // Affects only Android test runner, which respects JUnit 4 annotations on JUnit 3 tests.
public class MultimapEqualsTester<K, V> extends AbstractMultimapTester<K, V, Multimap<K, V>> {
  public void testEqualsTrue() {
    new EqualsTester()
        .addEqualityGroup(multimap(), getSubjectGenerator().create(getSampleElements().toArray()))
        .testEquals();
  }

  public void testEqualsFalse() {
    List<Entry<K, V>> targetEntries = new ArrayList<>(getSampleElements());
    targetEntries.add(Helpers.mapEntry(k0(), v3()));
    new EqualsTester()
        .addEqualityGroup(multimap())
        .addEqualityGroup(getSubjectGenerator().create(targetEntries.toArray()))
        .testEquals();
  }

  @CollectionSize.Require(absent = ZERO)
  @MapFeature.Require(ALLOWS_NULL_KEYS)
  public void testEqualsMultimapWithNullKey() {
    Multimap<K, V> original = multimap();
    initMultimapWithNullKey();
    Multimap<K, V> withNull = multimap();
    new EqualsTester()
        .addEqualityGroup(original)
        .addEqualityGroup(
            withNull, getSubjectGenerator().create((Object[]) createArrayWithNullKey()))
        .testEquals();
  }

  @CollectionSize.Require(absent = ZERO)
  @MapFeature.Require(ALLOWS_NULL_VALUES)
  public void testEqualsMultimapWithNullValue() {
    Multimap<K, V> original = multimap();
    initMultimapWithNullValue();
    Multimap<K, V> withNull = multimap();
    new EqualsTester()
        .addEqualityGroup(original)
        .addEqualityGroup(
            withNull, getSubjectGenerator().create((Object[]) createArrayWithNullValue()))
        .testEquals();
  }

  @CollectionSize.Require(absent = ZERO)
  public void testNotEqualsEmpty() {
    new EqualsTester()
        .addEqualityGroup(multimap())
        .addEqualityGroup(getSubjectGenerator().create())
        .testEquals();
  }

  public void testHashCodeMatchesAsMap() {
    assertEquals(multimap().asMap().hashCode(), multimap().hashCode());
  }
}
