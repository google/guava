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

import static com.google.common.collect.testing.features.CollectionSize.SEVERAL;
import static com.google.common.collect.testing.features.MapFeature.SUPPORTS_REMOVE;

import com.google.common.annotations.GwtCompatible;
import com.google.common.collect.Maps;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.Sets;
import com.google.common.collect.testing.Helpers;
import com.google.common.collect.testing.features.CollectionSize;
import com.google.common.collect.testing.features.MapFeature;
import com.google.common.testing.EqualsTester;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.junit.Ignore;

/**
 * Testers for {@link SetMultimap#asMap}.
 *
 * @author Louis Wasserman
 * @param <K> The key type of the tested multimap.
 * @param <V> The value type of the tested multimap.
 */
@GwtCompatible
@Ignore // Affects only Android test runner, which respects JUnit 4 annotations on JUnit 3 tests.
public class SetMultimapAsMapTester<K, V> extends AbstractMultimapTester<K, V, SetMultimap<K, V>> {
  public void testAsMapValuesImplementSet() {
    for (Collection<V> valueCollection : multimap().asMap().values()) {
      assertTrue(valueCollection instanceof Set);
    }
  }

  public void testAsMapGetImplementsSet() {
    for (K key : multimap().keySet()) {
      assertTrue(multimap().asMap().get(key) instanceof Set);
    }
  }

  @MapFeature.Require(SUPPORTS_REMOVE)
  public void testAsMapRemoveImplementsSet() {
    List<K> keys = new ArrayList<K>(multimap().keySet());
    for (K key : keys) {
      resetCollection();
      assertTrue(multimap().asMap().remove(key) instanceof Set);
    }
  }

  @CollectionSize.Require(SEVERAL)
  public void testEquals() {
    resetContainer(
        Helpers.mapEntry(k0(), v0()), Helpers.mapEntry(k1(), v0()), Helpers.mapEntry(k0(), v3()));
    Map<K, Collection<V>> expected = Maps.newHashMap();
    expected.put(k0(), Sets.newHashSet(v0(), v3()));
    expected.put(k1(), Sets.newHashSet(v0()));
    new EqualsTester().addEqualityGroup(expected, multimap().asMap()).testEquals();
  }

  @CollectionSize.Require(SEVERAL)
  public void testEntrySetEquals() {
    resetContainer(
        Helpers.mapEntry(k0(), v0()), Helpers.mapEntry(k1(), v0()), Helpers.mapEntry(k0(), v3()));
    Set<Entry<K, Collection<V>>> expected = Sets.newHashSet();
    expected.add(Helpers.mapEntry(k0(), (Collection<V>) Sets.newHashSet(v0(), v3())));
    expected.add(Helpers.mapEntry(k1(), (Collection<V>) Sets.newHashSet(v0())));
    new EqualsTester().addEqualityGroup(expected, multimap().asMap().entrySet()).testEquals();
  }

  @CollectionSize.Require(SEVERAL)
  @MapFeature.Require(SUPPORTS_REMOVE)
  public void testValuesRemove() {
    resetContainer(
        Helpers.mapEntry(k0(), v0()), Helpers.mapEntry(k1(), v0()), Helpers.mapEntry(k0(), v3()));
    assertTrue(multimap().asMap().values().remove(Collections.singleton(v0())));
    assertEquals(2, multimap().size());
    assertEquals(Collections.singletonMap(k0(), Sets.newHashSet(v0(), v3())), multimap().asMap());
  }
}
