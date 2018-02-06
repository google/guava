/*
 * Copyright (C) 2010 The Guava Authors
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

package com.google.common.collect.testing.testers;

import static com.google.common.collect.testing.Helpers.assertEqualInOrder;
import static com.google.common.collect.testing.features.CollectionSize.ONE;
import static com.google.common.collect.testing.features.CollectionSize.SEVERAL;
import static com.google.common.collect.testing.features.CollectionSize.ZERO;

import com.google.common.annotations.GwtCompatible;
import com.google.common.collect.testing.AbstractMapTester;
import com.google.common.collect.testing.Helpers;
import com.google.common.collect.testing.features.CollectionSize;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.SortedMap;
import org.junit.Ignore;

/**
 * A generic JUnit test which tests operations on a SortedMap. Can't be invoked directly; please see
 * {@code SortedMapTestSuiteBuilder}.
 *
 * @author Jesse Wilson
 * @author Louis Wasserman
 */
@GwtCompatible
@Ignore // Affects only Android test runner, which respects JUnit 4 annotations on JUnit 3 tests.
public class SortedMapNavigationTester<K, V> extends AbstractMapTester<K, V> {

  private SortedMap<K, V> navigableMap;
  private Entry<K, V> a;
  private Entry<K, V> c;

  @Override
  public void setUp() throws Exception {
    super.setUp();
    navigableMap = (SortedMap<K, V>) getMap();
    List<Entry<K, V>> entries =
        Helpers.copyToList(
            getSubjectGenerator()
                .getSampleElements(getSubjectGenerator().getCollectionSize().getNumElements()));
    Collections.sort(entries, Helpers.<K, V>entryComparator(navigableMap.comparator()));

    // some tests assume SEVERAL == 3
    if (entries.size() >= 1) {
      a = entries.get(0);
      if (entries.size() >= 3) {
        c = entries.get(2);
      }
    }
  }

  @CollectionSize.Require(ZERO)
  public void testEmptyMapFirst() {
    try {
      navigableMap.firstKey();
      fail();
    } catch (NoSuchElementException e) {
    }
  }

  @CollectionSize.Require(ZERO)
  public void testEmptyMapLast() {
    try {
      assertNull(navigableMap.lastKey());
      fail();
    } catch (NoSuchElementException e) {
    }
  }

  @CollectionSize.Require(ONE)
  public void testSingletonMapFirst() {
    assertEquals(a.getKey(), navigableMap.firstKey());
  }

  @CollectionSize.Require(ONE)
  public void testSingletonMapLast() {
    assertEquals(a.getKey(), navigableMap.lastKey());
  }

  @CollectionSize.Require(SEVERAL)
  public void testFirst() {
    assertEquals(a.getKey(), navigableMap.firstKey());
  }

  @CollectionSize.Require(SEVERAL)
  public void testLast() {
    assertEquals(c.getKey(), navigableMap.lastKey());
  }

  @CollectionSize.Require(absent = ZERO)
  public void testHeadMapExclusive() {
    assertFalse(navigableMap.headMap(a.getKey()).containsKey(a.getKey()));
  }

  @CollectionSize.Require(absent = ZERO)
  public void testTailMapInclusive() {
    assertTrue(navigableMap.tailMap(a.getKey()).containsKey(a.getKey()));
  }

  public void testHeadMap() {
    List<Entry<K, V>> entries =
        Helpers.copyToList(
            getSubjectGenerator()
                .getSampleElements(getSubjectGenerator().getCollectionSize().getNumElements()));
    Collections.sort(entries, Helpers.<K, V>entryComparator(navigableMap.comparator()));
    for (int i = 0; i < entries.size(); i++) {
      assertEqualInOrder(
          entries.subList(0, i), navigableMap.headMap(entries.get(i).getKey()).entrySet());
    }
  }

  public void testTailMap() {
    List<Entry<K, V>> entries =
        Helpers.copyToList(
            getSubjectGenerator()
                .getSampleElements(getSubjectGenerator().getCollectionSize().getNumElements()));
    Collections.sort(entries, Helpers.<K, V>entryComparator(navigableMap.comparator()));
    for (int i = 0; i < entries.size(); i++) {
      assertEqualInOrder(
          entries.subList(i, entries.size()),
          navigableMap.tailMap(entries.get(i).getKey()).entrySet());
    }
  }

  public void testSubMap() {
    List<Entry<K, V>> entries =
        Helpers.copyToList(
            getSubjectGenerator()
                .getSampleElements(getSubjectGenerator().getCollectionSize().getNumElements()));
    Collections.sort(entries, Helpers.<K, V>entryComparator(navigableMap.comparator()));
    for (int i = 0; i < entries.size(); i++) {
      for (int j = i + 1; j < entries.size(); j++) {
        assertEqualInOrder(
            entries.subList(i, j),
            navigableMap.subMap(entries.get(i).getKey(), entries.get(j).getKey()).entrySet());
      }
    }
  }

  @CollectionSize.Require(SEVERAL)
  public void testSubMapIllegal() {
    try {
      navigableMap.subMap(c.getKey(), a.getKey());
      fail("Expected IllegalArgumentException");
    } catch (IllegalArgumentException expected) {
    }
  }

  @CollectionSize.Require(absent = ZERO)
  public void testOrderedByComparator() {
    @SuppressWarnings("unchecked")
    Comparator<? super K> comparator = navigableMap.comparator();
    if (comparator == null) {
      comparator =
          new Comparator<K>() {
            @SuppressWarnings("unchecked")
            @Override
            public int compare(K o1, K o2) {
              return ((Comparable) o1).compareTo(o2);
            }
          };
    }
    Iterator<Entry<K, V>> entryItr = navigableMap.entrySet().iterator();
    Entry<K, V> prevEntry = entryItr.next();
    while (entryItr.hasNext()) {
      Entry<K, V> nextEntry = entryItr.next();
      assertTrue(comparator.compare(prevEntry.getKey(), nextEntry.getKey()) < 0);
      prevEntry = nextEntry;
    }
  }
}
