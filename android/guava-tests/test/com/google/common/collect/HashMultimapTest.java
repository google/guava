/*
 * Copyright (C) 2007 The Guava Authors
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

package com.google.common.collect;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.collect.testing.features.CollectionFeature;
import com.google.common.collect.testing.features.CollectionSize;
import com.google.common.collect.testing.features.MapFeature;
import com.google.common.collect.testing.google.SetMultimapTestSuiteBuilder;
import com.google.common.collect.testing.google.TestStringSetMultimapGenerator;
import java.util.Map.Entry;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Unit tests for {@link HashMultimap}.
 *
 * @author Jared Levy
 */
@GwtCompatible(emulated = true)
public class HashMultimapTest extends TestCase {

  @GwtIncompatible // suite
  public static Test suite() {
    TestSuite suite = new TestSuite();
    suite.addTest(
        SetMultimapTestSuiteBuilder.using(
                new TestStringSetMultimapGenerator() {
                  @Override
                  protected SetMultimap<String, String> create(Entry<String, String>[] entries) {
                    SetMultimap<String, String> multimap = HashMultimap.create();
                    for (Entry<String, String> entry : entries) {
                      multimap.put(entry.getKey(), entry.getValue());
                    }
                    return multimap;
                  }
                })
            .named("HashMultimap")
            .withFeatures(
                MapFeature.ALLOWS_NULL_KEYS,
                MapFeature.ALLOWS_NULL_VALUES,
                MapFeature.ALLOWS_ANY_NULL_QUERIES,
                MapFeature.GENERAL_PURPOSE,
                MapFeature.FAILS_FAST_ON_CONCURRENT_MODIFICATION,
                CollectionFeature.SUPPORTS_ITERATOR_REMOVE,
                CollectionFeature.SERIALIZABLE,
                CollectionSize.ANY)
            .createTestSuite());
    suite.addTestSuite(HashMultimapTest.class);
    return suite;
  }

  /*
   * The behavior of toString() is tested by TreeMultimap, which shares a
   * lot of code with HashMultimap and has deterministic iteration order.
   */
  public void testCreate() {
    HashMultimap<String, Integer> multimap = HashMultimap.create();
    multimap.put("foo", 1);
    multimap.put("bar", 2);
    multimap.put("foo", 3);
    assertEquals(ImmutableSet.of(1, 3), multimap.get("foo"));
    assertEquals(2, multimap.expectedValuesPerKey);
  }

  public void testCreateFromMultimap() {
    HashMultimap<String, Integer> multimap = HashMultimap.create();
    multimap.put("foo", 1);
    multimap.put("bar", 2);
    multimap.put("foo", 3);
    HashMultimap<String, Integer> copy = HashMultimap.create(multimap);
    assertEquals(multimap, copy);
    assertEquals(2, copy.expectedValuesPerKey);
  }

  public void testCreateFromSizes() {
    HashMultimap<String, Integer> multimap = HashMultimap.create(20, 15);
    multimap.put("foo", 1);
    multimap.put("bar", 2);
    multimap.put("foo", 3);
    assertEquals(ImmutableSet.of(1, 3), multimap.get("foo"));
    assertEquals(15, multimap.expectedValuesPerKey);
  }

  public void testCreateFromIllegalSizes() {
    try {
      HashMultimap.create(-20, 15);
      fail();
    } catch (IllegalArgumentException expected) {
    }

    try {
      HashMultimap.create(20, -15);
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }

  public void testEmptyMultimapsEqual() {
    Multimap<String, Integer> setMultimap = HashMultimap.create();
    Multimap<String, Integer> listMultimap = ArrayListMultimap.create();
    assertTrue(setMultimap.equals(listMultimap));
    assertTrue(listMultimap.equals(setMultimap));
  }
}
