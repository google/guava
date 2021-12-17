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

import static com.google.common.collect.testing.Helpers.assertContentsAnyOrder;
import static com.google.common.collect.testing.Helpers.assertContentsInOrder;
import static com.google.common.collect.testing.Helpers.assertEqualIgnoringOrder;
import static com.google.common.collect.testing.features.CollectionFeature.SUPPORTS_ITERATOR_REMOVE;
import static com.google.common.collect.testing.features.CollectionSize.SEVERAL;
import static com.google.common.collect.testing.features.CollectionSize.ZERO;
import static com.google.common.collect.testing.features.MapFeature.ALLOWS_NULL_KEYS;
import static com.google.common.collect.testing.features.MapFeature.ALLOWS_NULL_KEY_QUERIES;
import static com.google.common.collect.testing.features.MapFeature.SUPPORTS_PUT;
import static com.google.common.collect.testing.features.MapFeature.SUPPORTS_REMOVE;

import com.google.common.annotations.GwtCompatible;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;
import com.google.common.collect.testing.Helpers;
import com.google.common.collect.testing.features.CollectionFeature;
import com.google.common.collect.testing.features.CollectionSize;
import com.google.common.collect.testing.features.MapFeature;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import org.junit.Ignore;

/**
 * Tests for {@link Multimap#asMap}.
 *
 * @author Louis Wasserman
 */
@GwtCompatible
@Ignore // Affects only Android test runner, which respects JUnit 4 annotations on JUnit 3 tests.
public class MultimapAsMapTester<K, V> extends AbstractMultimapTester<K, V, Multimap<K, V>> {
  public void testAsMapGet() {
    for (K key : sampleKeys()) {
      List<V> expectedValues = new ArrayList<>();
      for (Entry<K, V> entry : getSampleElements()) {
        if (entry.getKey().equals(key)) {
          expectedValues.add(entry.getValue());
        }
      }

      Collection<V> collection = multimap().asMap().get(key);
      if (expectedValues.isEmpty()) {
        assertNull(collection);
      } else {
        assertEqualIgnoringOrder(expectedValues, collection);
      }
    }
  }

  @CollectionSize.Require(absent = ZERO)
  @MapFeature.Require(ALLOWS_NULL_KEYS)
  public void testAsMapGetNullKeyPresent() {
    initMultimapWithNullKey();
    assertContentsAnyOrder(multimap().asMap().get(null), getValueForNullKey());
  }

  @MapFeature.Require(ALLOWS_NULL_KEY_QUERIES)
  public void testAsMapGetNullKeyAbsent() {
    assertNull(multimap().asMap().get(null));
  }

  @MapFeature.Require(absent = ALLOWS_NULL_KEY_QUERIES)
  public void testAsMapGetNullKeyUnsupported() {
    try {
      multimap().asMap().get(null);
      fail("Expected NullPointerException");
    } catch (NullPointerException expected) {
    }
  }

  @CollectionSize.Require(absent = ZERO)
  @MapFeature.Require(SUPPORTS_REMOVE)
  public void testAsMapRemove() {
    assertContentsInOrder(multimap().asMap().remove(k0()), v0());
    assertGet(k0());
    assertEquals(getNumElements() - 1, multimap().size());
  }

  @CollectionSize.Require(SEVERAL)
  @MapFeature.Require(SUPPORTS_PUT)
  public void testAsMapEntrySetReflectsPutSameKey() {
    resetContainer(Helpers.mapEntry(k0(), v0()), Helpers.mapEntry(k0(), v3()));

    Set<Entry<K, Collection<V>>> asMapEntrySet = multimap().asMap().entrySet();
    Collection<V> valueCollection = Iterables.getOnlyElement(asMapEntrySet).getValue();
    assertContentsAnyOrder(valueCollection, v0(), v3());
    assertTrue(multimap().put(k0(), v4()));
    assertContentsAnyOrder(valueCollection, v0(), v3(), v4());
  }

  @CollectionSize.Require(SEVERAL)
  @MapFeature.Require(SUPPORTS_PUT)
  public void testAsMapEntrySetReflectsPutDifferentKey() {
    resetContainer(Helpers.mapEntry(k0(), v0()), Helpers.mapEntry(k0(), v3()));

    Set<Entry<K, Collection<V>>> asMapEntrySet = multimap().asMap().entrySet();
    assertTrue(multimap().put(k1(), v4()));
    assertEquals(2, asMapEntrySet.size());
  }

  @CollectionSize.Require(SEVERAL)
  @MapFeature.Require({SUPPORTS_PUT, SUPPORTS_REMOVE})
  public void testAsMapEntrySetRemovePropagatesToMultimap() {
    resetContainer(Helpers.mapEntry(k0(), v0()), Helpers.mapEntry(k0(), v3()));
    Set<Entry<K, Collection<V>>> asMapEntrySet = multimap().asMap().entrySet();
    Entry<K, Collection<V>> asMapEntry0 = Iterables.getOnlyElement(asMapEntrySet);
    assertTrue(multimap().put(k1(), v4()));
    assertTrue(asMapEntrySet.remove(asMapEntry0));
    assertEquals(1, multimap().size());
    assertContentsInOrder(multimap().keySet(), k1());
  }

  @CollectionSize.Require(SEVERAL)
  @CollectionFeature.Require(SUPPORTS_ITERATOR_REMOVE)
  public void testAsMapEntrySetIteratorRemovePropagatesToMultimap() {
    resetContainer(Helpers.mapEntry(k0(), v0()), Helpers.mapEntry(k0(), v3()));
    Set<Entry<K, Collection<V>>> asMapEntrySet = multimap().asMap().entrySet();
    Iterator<Entry<K, Collection<V>>> asMapEntryItr = asMapEntrySet.iterator();
    asMapEntryItr.next();
    asMapEntryItr.remove();
    assertTrue(multimap().isEmpty());
  }
}
