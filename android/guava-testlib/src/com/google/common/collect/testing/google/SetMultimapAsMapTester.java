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

import static com.google.common.collect.Sets.newHashSet;
import static com.google.common.collect.testing.Helpers.mapEntry;
import static com.google.common.collect.testing.features.CollectionSize.SEVERAL;
import static com.google.common.collect.testing.features.MapFeature.SUPPORTS_REMOVE;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonMap;

import com.google.common.annotations.GwtCompatible;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.testing.features.CollectionSize;
import com.google.common.collect.testing.features.MapFeature;
import com.google.common.testing.EqualsTester;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.junit.Ignore;

/**
 * Testers for {@link SetMultimap#asMap}.
 *
 * @author Louis Wasserman
 * @param <K> The key type of the tested multimap.
 * @param <V> The value type of the tested multimap.
 */
@GwtCompatible
@Ignore("test runners must not instantiate and run this directly, only via suites we build")
// @Ignore affects the Android test runner, which respects JUnit 4 annotations on JUnit 3 tests.
@SuppressWarnings("JUnit4ClassUsedInJUnit3")
@NullMarked
public class SetMultimapAsMapTester<K extends @Nullable Object, V extends @Nullable Object>
    extends AbstractMultimapTester<K, V, SetMultimap<K, V>> {
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
    List<K> keys = new ArrayList<>(multimap().keySet());
    for (K key : keys) {
      resetCollection();
      assertTrue(multimap().asMap().remove(key) instanceof Set);
    }
  }

  @CollectionSize.Require(SEVERAL)
  public void testEquals() {
    resetContainer(mapEntry(k0(), v0()), mapEntry(k1(), v0()), mapEntry(k0(), v3()));
    Map<K, Collection<V>> expected = new HashMap<>();
    expected.put(k0(), newHashSet(v0(), v3()));
    expected.put(k1(), newHashSet(v0()));
    new EqualsTester().addEqualityGroup(expected, multimap().asMap()).testEquals();
  }

  @CollectionSize.Require(SEVERAL)
  public void testEntrySetEquals() {
    resetContainer(mapEntry(k0(), v0()), mapEntry(k1(), v0()), mapEntry(k0(), v3()));
    Set<Entry<K, Collection<V>>> expected = new HashSet<>();
    expected.add(mapEntry(k0(), (Collection<V>) newHashSet(v0(), v3())));
    expected.add(mapEntry(k1(), (Collection<V>) newHashSet(v0())));
    new EqualsTester().addEqualityGroup(expected, multimap().asMap().entrySet()).testEquals();
  }

  @CollectionSize.Require(SEVERAL)
  @MapFeature.Require(SUPPORTS_REMOVE)
  /*
   * SetMultimap.asMap essentially returns a Map<K, Set<V>>; we just can't declare it that way.
   * Thus, calls like asMap().values().remove(someSet) are safe because they are comparing a set to
   * a collection of other sets.
   */
  @SuppressWarnings("CollectionUndefinedEquality")
  public void testValuesRemove() {
    resetContainer(mapEntry(k0(), v0()), mapEntry(k1(), v0()), mapEntry(k0(), v3()));
    assertTrue(multimap().asMap().values().remove(singleton(v0())));
    assertEquals(2, multimap().size());
    assertEquals(singletonMap(k0(), newHashSet(v0(), v3())), multimap().asMap());
  }
}
