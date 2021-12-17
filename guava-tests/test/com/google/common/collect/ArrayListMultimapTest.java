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

import static com.google.common.truth.Truth.assertThat;
import static java.util.Arrays.asList;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.collect.testing.features.CollectionFeature;
import com.google.common.collect.testing.features.CollectionSize;
import com.google.common.collect.testing.features.MapFeature;
import com.google.common.collect.testing.google.ListMultimapTestSuiteBuilder;
import com.google.common.collect.testing.google.TestStringListMultimapGenerator;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.Map.Entry;
import java.util.RandomAccess;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Unit tests for {@code ArrayListMultimap}.
 *
 * @author Jared Levy
 */
@GwtCompatible(emulated = true)
public class ArrayListMultimapTest extends TestCase {

  @GwtIncompatible // suite
  public static Test suite() {
    TestSuite suite = new TestSuite();
    suite.addTest(
        ListMultimapTestSuiteBuilder.using(
                new TestStringListMultimapGenerator() {
                  @Override
                  protected ListMultimap<String, String> create(Entry<String, String>[] entries) {
                    ListMultimap<String, String> multimap = ArrayListMultimap.create();
                    for (Entry<String, String> entry : entries) {
                      multimap.put(entry.getKey(), entry.getValue());
                    }
                    return multimap;
                  }
                })
            .named("ArrayListMultimap")
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
    suite.addTestSuite(ArrayListMultimapTest.class);
    return suite;
  }

  protected ListMultimap<String, Integer> create() {
    return ArrayListMultimap.create();
  }

  /** Confirm that get() returns a List implementing RandomAccess. */
  public void testGetRandomAccess() {
    Multimap<String, Integer> multimap = create();
    multimap.put("foo", 1);
    multimap.put("foo", 3);
    assertTrue(multimap.get("foo") instanceof RandomAccess);
    assertTrue(multimap.get("bar") instanceof RandomAccess);
  }

  /** Confirm that removeAll() returns a List implementing RandomAccess. */
  public void testRemoveAllRandomAccess() {
    Multimap<String, Integer> multimap = create();
    multimap.put("foo", 1);
    multimap.put("foo", 3);
    assertTrue(multimap.removeAll("foo") instanceof RandomAccess);
    assertTrue(multimap.removeAll("bar") instanceof RandomAccess);
  }

  /** Confirm that replaceValues() returns a List implementing RandomAccess. */
  public void testReplaceValuesRandomAccess() {
    Multimap<String, Integer> multimap = create();
    multimap.put("foo", 1);
    multimap.put("foo", 3);
    assertTrue(multimap.replaceValues("foo", asList(2, 4)) instanceof RandomAccess);
    assertTrue(multimap.replaceValues("bar", asList(2, 4)) instanceof RandomAccess);
  }

  /** Test throwing ConcurrentModificationException when a sublist's ancestor's delegate changes. */
  public void testSublistConcurrentModificationException() {
    ListMultimap<String, Integer> multimap = create();
    multimap.putAll("foo", asList(1, 2, 3, 4, 5));
    List<Integer> list = multimap.get("foo");
    assertThat(multimap.get("foo")).containsExactly(1, 2, 3, 4, 5).inOrder();
    List<Integer> sublist = list.subList(0, 5);
    assertThat(sublist).containsExactly(1, 2, 3, 4, 5).inOrder();

    sublist.clear();
    assertTrue(sublist.isEmpty());
    multimap.put("foo", 6);

    try {
      sublist.isEmpty();
      fail("Expected ConcurrentModificationException");
    } catch (ConcurrentModificationException expected) {
    }
  }

  public void testCreateFromMultimap() {
    Multimap<String, Integer> multimap = create();
    multimap.put("foo", 1);
    multimap.put("foo", 3);
    multimap.put("bar", 2);
    ArrayListMultimap<String, Integer> copy = ArrayListMultimap.create(multimap);
    assertEquals(multimap, copy);
  }

  public void testCreate() {
    ArrayListMultimap<String, Integer> multimap = ArrayListMultimap.create();
    assertEquals(3, multimap.expectedValuesPerKey);
  }

  public void testCreateFromSizes() {
    ArrayListMultimap<String, Integer> multimap = ArrayListMultimap.create(15, 20);
    assertEquals(20, multimap.expectedValuesPerKey);
  }

  public void testCreateFromIllegalSizes() {
    try {
      ArrayListMultimap.create(15, -2);
      fail();
    } catch (IllegalArgumentException expected) {
    }

    try {
      ArrayListMultimap.create(-15, 2);
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }

  public void testCreateFromHashMultimap() {
    Multimap<String, Integer> original = HashMultimap.create();
    ArrayListMultimap<String, Integer> multimap = ArrayListMultimap.create(original);
    assertEquals(3, multimap.expectedValuesPerKey);
  }

  public void testCreateFromArrayListMultimap() {
    ArrayListMultimap<String, Integer> original = ArrayListMultimap.create(15, 20);
    ArrayListMultimap<String, Integer> multimap = ArrayListMultimap.create(original);
    assertEquals(20, multimap.expectedValuesPerKey);
  }

  public void testTrimToSize() {
    ArrayListMultimap<String, Integer> multimap = ArrayListMultimap.create();
    multimap.put("foo", 1);
    multimap.put("foo", 2);
    multimap.put("bar", 3);
    multimap.trimToSize();
    assertEquals(3, multimap.size());
    assertThat(multimap.get("foo")).containsExactly(1, 2).inOrder();
    assertThat(multimap.get("bar")).contains(3);
  }
}
