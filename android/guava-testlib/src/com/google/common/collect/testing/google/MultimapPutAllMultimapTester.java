/*
 * Copyright (C) 2013 The Guava Authors
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

import static com.google.common.collect.testing.Helpers.assertContains;
import static com.google.common.collect.testing.Helpers.mapEntry;
import static com.google.common.collect.testing.features.MapFeature.ALLOWS_NULL_KEYS;
import static com.google.common.collect.testing.features.MapFeature.ALLOWS_NULL_VALUES;
import static com.google.common.collect.testing.features.MapFeature.SUPPORTS_PUT;
import static com.google.common.collect.testing.google.ReflectionFreeAssertThrows.assertThrows;

import com.google.common.annotations.GwtCompatible;
import com.google.common.collect.Multimap;
import com.google.common.collect.testing.features.MapFeature;
import java.util.Collection;
import org.junit.Ignore;

/**
 * Tester for {@link Multimap#putAll(Multimap)}.
 *
 * @author Louis Wasserman
 */
@GwtCompatible
@Ignore("test runners must not instantiate and run this directly, only via suites we build")
// @Ignore affects the Android test runner, which respects JUnit 4 annotations on JUnit 3 tests.
@SuppressWarnings("JUnit4ClassUsedInJUnit3")
public class MultimapPutAllMultimapTester<K, V>
    extends AbstractMultimapTester<K, V, Multimap<K, V>> {
  @MapFeature.Require(absent = SUPPORTS_PUT)
  public void testPutUnsupported() {
    assertThrows(
        UnsupportedOperationException.class,
        () -> multimap().putAll(getSubjectGenerator().create(mapEntry(k3(), v3()))));
  }

  @MapFeature.Require(SUPPORTS_PUT)
  // Empty multimaps *do* have defined equals semantics.
  @SuppressWarnings("UndefinedEquals")
  public void testPutAllIntoEmpty() {
    Multimap<K, V> target = getSubjectGenerator().create();
    assertEquals(!multimap().isEmpty(), target.putAll(multimap()));
    assertEquals(multimap(), target);
  }

  @MapFeature.Require(SUPPORTS_PUT)
  public void testPutAll() {
    Multimap<K, V> source =
        getSubjectGenerator().create(mapEntry(k0(), v3()), mapEntry(k3(), v3()));
    assertTrue(multimap().putAll(source));
    assertTrue(multimap().containsEntry(k0(), v3()));
    assertTrue(multimap().containsEntry(k3(), v3()));
  }

  @MapFeature.Require({SUPPORTS_PUT, ALLOWS_NULL_VALUES})
  public void testPutAllWithNullValue() {
    Multimap<K, V> source = getSubjectGenerator().create(mapEntry(k0(), null));
    assertTrue(multimap().putAll(source));
    assertTrue(multimap().containsEntry(k0(), null));
  }

  @MapFeature.Require({SUPPORTS_PUT, ALLOWS_NULL_KEYS})
  public void testPutAllWithNullKey() {
    Multimap<K, V> source = getSubjectGenerator().create(mapEntry(null, v0()));
    assertTrue(multimap().putAll(source));
    assertTrue(multimap().containsEntry(null, v0()));
  }

  @MapFeature.Require(value = SUPPORTS_PUT, absent = ALLOWS_NULL_VALUES)
  public void testPutAllRejectsNullValue() {
    Multimap<K, V> source = getSubjectGenerator().create(mapEntry(k0(), null));
    assertThrows(NullPointerException.class, () -> multimap().putAll(source));
    expectUnchanged();
  }

  @MapFeature.Require(value = SUPPORTS_PUT, absent = ALLOWS_NULL_KEYS)
  public void testPutAllRejectsNullKey() {
    Multimap<K, V> source = getSubjectGenerator().create(mapEntry(null, v0()));
    assertThrows(NullPointerException.class, () -> multimap().putAll(source));
    expectUnchanged();
  }

  @MapFeature.Require(SUPPORTS_PUT)
  public void testPutAllPropagatesToGet() {
    Multimap<K, V> source =
        getSubjectGenerator().create(mapEntry(k0(), v3()), mapEntry(k3(), v3()));
    Collection<V> getCollection = multimap().get(k0());
    int getCollectionSize = getCollection.size();
    assertTrue(multimap().putAll(source));
    assertEquals(getCollectionSize + 1, getCollection.size());
    assertContains(getCollection, v3());
  }
}
