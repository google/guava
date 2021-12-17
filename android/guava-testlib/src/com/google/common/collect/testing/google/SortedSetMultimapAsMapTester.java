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

import static com.google.common.collect.testing.features.MapFeature.SUPPORTS_REMOVE;

import com.google.common.annotations.GwtCompatible;
import com.google.common.collect.SortedSetMultimap;
import com.google.common.collect.testing.features.MapFeature;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.SortedSet;
import org.junit.Ignore;

/**
 * Testers for {@link SortedSetMultimap#asMap}.
 *
 * @author Louis Wasserman
 * @param <K> The key type of the tested multimap.
 * @param <V> The value type of the tested multimap.
 */
@GwtCompatible
@Ignore // Affects only Android test runner, which respects JUnit 4 annotations on JUnit 3 tests.
public class SortedSetMultimapAsMapTester<K, V>
    extends AbstractMultimapTester<K, V, SortedSetMultimap<K, V>> {
  public void testAsMapValuesImplementSortedSet() {
    for (Collection<V> valueCollection : multimap().asMap().values()) {
      SortedSet<V> valueSet = (SortedSet<V>) valueCollection;
      assertEquals(multimap().valueComparator(), valueSet.comparator());
    }
  }

  public void testAsMapGetImplementsSortedSet() {
    for (K key : multimap().keySet()) {
      SortedSet<V> valueSet = (SortedSet<V>) multimap().asMap().get(key);
      assertEquals(multimap().valueComparator(), valueSet.comparator());
    }
  }

  @MapFeature.Require(SUPPORTS_REMOVE)
  public void testAsMapRemoveImplementsSortedSet() {
    List<K> keys = new ArrayList<>(multimap().keySet());
    for (K key : keys) {
      resetCollection();
      SortedSet<V> valueSet = (SortedSet<V>) multimap().asMap().remove(key);
      assertEquals(multimap().valueComparator(), valueSet.comparator());
    }
  }
}
