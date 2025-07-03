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

import static com.google.common.collect.testing.Helpers.assertContains;
import static com.google.common.collect.testing.Helpers.assertEqualIgnoringOrder;
import static com.google.common.collect.testing.Helpers.mapEntry;
import static com.google.common.collect.testing.features.CollectionFeature.SUPPORTS_ITERATOR_REMOVE;
import static com.google.common.collect.testing.features.CollectionSize.ONE;
import static com.google.common.collect.testing.features.CollectionSize.ZERO;
import static com.google.common.collect.testing.features.MapFeature.ALLOWS_NULL_KEYS;
import static com.google.common.collect.testing.features.MapFeature.ALLOWS_NULL_KEY_QUERIES;
import static com.google.common.collect.testing.features.MapFeature.ALLOWS_NULL_VALUES;
import static com.google.common.collect.testing.features.MapFeature.ALLOWS_NULL_VALUE_QUERIES;
import static com.google.common.collect.testing.features.MapFeature.SUPPORTS_REMOVE;
import static java.util.Collections.singleton;

import com.google.common.annotations.GwtCompatible;
import com.google.common.collect.Multimap;
import com.google.common.collect.testing.features.CollectionFeature;
import com.google.common.collect.testing.features.CollectionSize;
import com.google.common.collect.testing.features.MapFeature;
import java.util.Iterator;
import java.util.Map.Entry;
import org.junit.Ignore;

/**
 * Tester for {@code Multimap.entries}.
 *
 * @author Louis Wasserman
 */
@GwtCompatible
@Ignore("test runners must not instantiate and run this directly, only via suites we build")
// @Ignore affects the Android test runner, which respects JUnit 4 annotations on JUnit 3 tests.
@SuppressWarnings("JUnit4ClassUsedInJUnit3")
public class MultimapEntriesTester<K, V> extends AbstractMultimapTester<K, V, Multimap<K, V>> {
  public void testEntries() {
    assertEqualIgnoringOrder(getSampleElements(), multimap().entries());
  }

  @CollectionSize.Require(absent = ZERO)
  @MapFeature.Require(ALLOWS_NULL_KEYS)
  public void testContainsEntryWithNullKeyPresent() {
    initMultimapWithNullKey();
    assertContains(multimap().entries(), mapEntry((K) null, getValueForNullKey()));
  }

  @MapFeature.Require(ALLOWS_NULL_KEY_QUERIES)
  public void testContainsEntryWithNullKeyAbsent() {
    assertFalse(multimap().entries().contains(mapEntry(null, v0())));
  }

  @CollectionSize.Require(absent = ZERO)
  @MapFeature.Require(ALLOWS_NULL_VALUES)
  public void testContainsEntryWithNullValuePresent() {
    initMultimapWithNullValue();
    assertContains(multimap().entries(), mapEntry(getKeyForNullValue(), (V) null));
  }

  @MapFeature.Require(ALLOWS_NULL_VALUE_QUERIES)
  public void testContainsEntryWithNullValueAbsent() {
    assertFalse(multimap().entries().contains(mapEntry(k0(), null)));
  }

  @CollectionSize.Require(absent = ZERO)
  @MapFeature.Require(SUPPORTS_REMOVE)
  public void testRemovePropagatesToMultimap() {
    assertTrue(multimap().entries().remove(mapEntry(k0(), v0())));
    expectMissing(mapEntry(k0(), v0()));
    assertEquals(getNumElements() - 1, multimap().size());
    assertFalse(multimap().containsEntry(k0(), v0()));
  }

  @CollectionSize.Require(absent = ZERO)
  @MapFeature.Require(SUPPORTS_REMOVE)
  public void testRemoveAllPropagatesToMultimap() {
    assertTrue(multimap().entries().removeAll(singleton(mapEntry(k0(), v0()))));
    expectMissing(mapEntry(k0(), v0()));
    assertEquals(getNumElements() - 1, multimap().size());
    assertFalse(multimap().containsEntry(k0(), v0()));
  }

  @CollectionSize.Require(absent = ZERO)
  @MapFeature.Require(SUPPORTS_REMOVE)
  /*
   * We are comparing Multimaps of the same type, so as long as they have value collections that
   * implement equals() (as with ListMultimap or SetMultimap, as opposed to a QueueMultimap or
   * something), our equality check is value-based.
   */
  @SuppressWarnings("UndefinedEquals")
  public void testRetainAllPropagatesToMultimap() {
    multimap().entries().retainAll(singleton(mapEntry(k0(), v0())));
    assertEquals(getSubjectGenerator().create(mapEntry(k0(), v0())), multimap());
    assertEquals(1, multimap().size());
    assertTrue(multimap().containsEntry(k0(), v0()));
  }

  @CollectionSize.Require(ONE)
  @CollectionFeature.Require(SUPPORTS_ITERATOR_REMOVE)
  public void testIteratorRemovePropagatesToMultimap() {
    Iterator<Entry<K, V>> iterator = multimap().entries().iterator();
    assertEquals(mapEntry(k0(), v0()), iterator.next());
    iterator.remove();
    assertTrue(multimap().isEmpty());
  }

  @CollectionSize.Require(absent = ZERO)
  @MapFeature.Require(SUPPORTS_REMOVE)
  public void testEntriesRemainValidAfterRemove() {
    Iterator<Entry<K, V>> iterator = multimap().entries().iterator();
    Entry<K, V> entry = iterator.next();
    K key = entry.getKey();
    V value = entry.getValue();
    multimap().removeAll(key);
    assertEquals(key, entry.getKey());
    assertEquals(value, entry.getValue());
  }
}
