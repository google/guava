/*
 * Copyright (C) 2012 The Guava Authors
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

import static com.google.common.collect.testing.Helpers.assertContentsInOrder;
import static com.google.common.collect.testing.Helpers.copyToList;
import static com.google.common.collect.testing.Helpers.mapEntry;
import static com.google.common.collect.testing.features.CollectionSize.SEVERAL;
import static com.google.common.collect.testing.features.MapFeature.SUPPORTS_REMOVE;

import com.google.common.annotations.GwtCompatible;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.testing.features.CollectionSize;
import com.google.common.collect.testing.features.MapFeature;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map.Entry;
import org.junit.Ignore;

/**
 * Testers for {@link ListMultimap#remove(Object, Object)}.
 *
 * @author Louis Wasserman
 */
@GwtCompatible
@Ignore // Affects only Android test runner, which respects JUnit 4 annotations on JUnit 3 tests.
public class ListMultimapRemoveTester<K, V> extends AbstractListMultimapTester<K, V> {
  @SuppressWarnings("unchecked")
  @MapFeature.Require(SUPPORTS_REMOVE)
  @CollectionSize.Require(SEVERAL)
  public void testMultimapRemoveDeletesFirstOccurrence() {
    resetContainer(mapEntry(k0(), v0()), mapEntry(k0(), v1()), mapEntry(k0(), v0()));

    List<V> list = multimap().get(k0());
    multimap().remove(k0(), v0());
    assertContentsInOrder(list, v1(), v0());
  }

  @SuppressWarnings("unchecked")
  @MapFeature.Require(SUPPORTS_REMOVE)
  @CollectionSize.Require(SEVERAL)
  public void testRemoveAtIndexFromGetPropagates() {
    List<V> values = Arrays.asList(v0(), v1(), v0());

    for (int i = 0; i < 3; i++) {
      resetContainer(mapEntry(k0(), v0()), mapEntry(k0(), v1()), mapEntry(k0(), v0()));
      List<V> expectedValues = copyToList(values);

      multimap().get(k0()).remove(i);
      expectedValues.remove(i);

      assertGet(k0(), expectedValues);
    }
  }

  @SuppressWarnings("unchecked")
  @MapFeature.Require(SUPPORTS_REMOVE)
  @CollectionSize.Require(SEVERAL)
  public void testRemoveAtIndexFromAsMapPropagates() {
    List<V> values = Arrays.asList(v0(), v1(), v0());

    for (int i = 0; i < 3; i++) {
      resetContainer(mapEntry(k0(), v0()), mapEntry(k0(), v1()), mapEntry(k0(), v0()));
      List<V> expectedValues = copyToList(values);

      List<V> asMapValue = (List<V>) multimap().asMap().get(k0());
      asMapValue.remove(i);
      expectedValues.remove(i);

      assertGet(k0(), expectedValues);
    }
  }

  @SuppressWarnings("unchecked")
  @MapFeature.Require(SUPPORTS_REMOVE)
  @CollectionSize.Require(SEVERAL)
  public void testRemoveAtIndexFromAsMapEntrySetPropagates() {
    List<V> values = Arrays.asList(v0(), v1(), v0());

    for (int i = 0; i < 3; i++) {
      resetContainer(mapEntry(k0(), v0()), mapEntry(k0(), v1()), mapEntry(k0(), v0()));
      List<V> expectedValues = copyToList(values);

      Entry<K, Collection<V>> asMapEntry = multimap().asMap().entrySet().iterator().next();
      List<V> asMapValue = (List<V>) asMapEntry.getValue();
      asMapValue.remove(i);
      expectedValues.remove(i);

      assertGet(k0(), expectedValues);
    }
  }
}
