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

import static com.google.common.collect.testing.features.MapFeature.ALLOWS_NULL_KEYS;
import static com.google.common.collect.testing.features.MapFeature.ALLOWS_NULL_VALUES;
import static com.google.common.collect.testing.features.MapFeature.SUPPORTS_PUT;
import static org.truth0.Truth.ASSERT;

import com.google.common.annotations.GwtCompatible;
import com.google.common.collect.Multimap;
import com.google.common.collect.testing.Helpers;
import com.google.common.collect.testing.features.MapFeature;

import java.util.Collection;

/**
 * Tester for {@link Multimap#putAll(Multimap)}.
 *
 * @author Louis Wasserman
 */
@GwtCompatible
public class MultimapPutAllMultimapTester<K, V>
    extends AbstractMultimapTester<K, V, Multimap<K, V>> {
  @MapFeature.Require(absent = SUPPORTS_PUT)
  public void testPutUnsupported() {
    try {
      multimap().putAll(getSubjectGenerator().create(
          Helpers.mapEntry(sampleKeys().e3, sampleValues().e3)));
      fail("Expected UnsupportedOperationException");
    } catch (UnsupportedOperationException expected) {}
  }
  
  @MapFeature.Require(SUPPORTS_PUT)
  public void testPutAllIntoEmpty() {
    Multimap<K, V> target = getSubjectGenerator().create();
    assertEquals(!multimap().isEmpty(), target.putAll(multimap()));
    assertEquals(multimap(), target);    
  }
  
  @MapFeature.Require(SUPPORTS_PUT)
  public void testPutAll() {
    Multimap<K, V> source = getSubjectGenerator().create(
        Helpers.mapEntry(sampleKeys().e0, sampleValues().e3),
        Helpers.mapEntry(sampleKeys().e3, sampleValues().e3));
    assertTrue(multimap().putAll(source));
    assertTrue(multimap().containsEntry(sampleKeys().e0, sampleValues().e3));
    assertTrue(multimap().containsEntry(sampleKeys().e3, sampleValues().e3));
  }
  
  @MapFeature.Require({SUPPORTS_PUT, ALLOWS_NULL_VALUES})
  public void testPutAllWithNullValue() {
    Multimap<K, V> source = getSubjectGenerator().create(
        Helpers.mapEntry(sampleKeys().e0, null));
    assertTrue(multimap().putAll(source));
    assertTrue(multimap().containsEntry(sampleKeys().e0, null));
  }
  
  @MapFeature.Require({SUPPORTS_PUT, ALLOWS_NULL_KEYS})
  public void testPutAllWithNullKey() {
    Multimap<K, V> source = getSubjectGenerator().create(
        Helpers.mapEntry(null, sampleValues().e0));
    assertTrue(multimap().putAll(source));
    assertTrue(multimap().containsEntry(null, sampleValues().e0));
  }
  
  @MapFeature.Require(value = SUPPORTS_PUT, absent = ALLOWS_NULL_VALUES)
  public void testPutAllRejectsNullValue() {
    Multimap<K, V> source = getSubjectGenerator().create(
        Helpers.mapEntry(sampleKeys().e0, null));
    try {
      multimap().putAll(source);
      fail("Expected NullPointerException");
    } catch (NullPointerException expected) {}
    expectUnchanged();
  }
  
  @MapFeature.Require(value = SUPPORTS_PUT, absent = ALLOWS_NULL_KEYS)
  public void testPutAllRejectsNullKey() {
    Multimap<K, V> source = getSubjectGenerator().create(
        Helpers.mapEntry(null, sampleValues().e0));
    try {
      multimap().putAll(source);
      fail("Expected NullPointerException");
    } catch (NullPointerException expected) {}
    expectUnchanged();
  }

  @MapFeature.Require(SUPPORTS_PUT)
  public void testPutAllPropagatesToGet() {
    Multimap<K, V> source = getSubjectGenerator().create(
        Helpers.mapEntry(sampleKeys().e0, sampleValues().e3),
        Helpers.mapEntry(sampleKeys().e3, sampleValues().e3));
    Collection<V> getCollection = multimap().get(sampleKeys().e0);
    int getCollectionSize = getCollection.size();
    assertTrue(multimap().putAll(source));
    assertEquals(getCollectionSize + 1, getCollection.size());
    ASSERT.that(getCollection).has().allOf(sampleValues().e3);
  }
}
