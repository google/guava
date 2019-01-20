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

import static com.google.common.collect.testing.features.CollectionSize.ONE;
import static com.google.common.collect.testing.features.CollectionSize.SEVERAL;
import static com.google.common.collect.testing.features.CollectionSize.ZERO;
import static com.google.common.collect.testing.features.MapFeature.SUPPORTS_REMOVE;

import com.google.common.annotations.GwtIncompatible;
import com.google.common.collect.testing.AbstractMapTester;
import com.google.common.collect.testing.Helpers;
import com.google.common.collect.testing.features.CollectionSize;
import com.google.common.collect.testing.features.MapFeature;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;
import java.util.NavigableMap;
import org.junit.Ignore;

/**
 * A generic JUnit test which tests operations on a NavigableMap. Can't be invoked directly; please
 * see {@code NavigableMapTestSuiteBuilder}.
 *
 * @author Jesse Wilson
 * @author Louis Wasserman
 */
@GwtIncompatible
@Ignore // Affects only Android test runner, which respects JUnit 4 annotations on JUnit 3 tests.
public class NavigableMapNavigationTester<K, V> extends AbstractMapTester<K, V> {

  private NavigableMap<K, V> navigableMap;
  private List<Entry<K, V>> entries;
  private Entry<K, V> a;
  private Entry<K, V> b;
  private Entry<K, V> c;

  @Override
  public void setUp() throws Exception {
    super.setUp();
    navigableMap = (NavigableMap<K, V>) getMap();
    entries =
        Helpers.copyToList(
            getSubjectGenerator()
                .getSampleElements(getSubjectGenerator().getCollectionSize().getNumElements()));
    Collections.sort(entries, Helpers.<K, V>entryComparator(navigableMap.comparator()));

    // some tests assume SEVERAL == 3
    if (entries.size() >= 1) {
      a = entries.get(0);
      if (entries.size() >= 3) {
        b = entries.get(1);
        c = entries.get(2);
      }
    }
  }

  /** Resets the contents of navigableMap to have entries a, c, for the navigation tests. */
  @SuppressWarnings("unchecked") // Needed to stop Eclipse whining
  private void resetWithHole() {
    Entry<K, V>[] entries = new Entry[] {a, c};
    super.resetMap(entries);
    navigableMap = (NavigableMap<K, V>) getMap();
  }

  @CollectionSize.Require(ZERO)
  public void testEmptyMapFirst() {
    assertNull(navigableMap.firstEntry());
  }

  @MapFeature.Require(SUPPORTS_REMOVE)
  @CollectionSize.Require(ZERO)
  public void testEmptyMapPollFirst() {
    assertNull(navigableMap.pollFirstEntry());
  }

  @CollectionSize.Require(ZERO)
  public void testEmptyMapNearby() {
    assertNull(navigableMap.lowerEntry(k0()));
    assertNull(navigableMap.lowerKey(k0()));
    assertNull(navigableMap.floorEntry(k0()));
    assertNull(navigableMap.floorKey(k0()));
    assertNull(navigableMap.ceilingEntry(k0()));
    assertNull(navigableMap.ceilingKey(k0()));
    assertNull(navigableMap.higherEntry(k0()));
    assertNull(navigableMap.higherKey(k0()));
  }

  @CollectionSize.Require(ZERO)
  public void testEmptyMapLast() {
    assertNull(navigableMap.lastEntry());
  }

  @MapFeature.Require(SUPPORTS_REMOVE)
  @CollectionSize.Require(ZERO)
  public void testEmptyMapPollLast() {
    assertNull(navigableMap.pollLastEntry());
  }

  @CollectionSize.Require(ONE)
  public void testSingletonMapFirst() {
    assertEquals(a, navigableMap.firstEntry());
  }

  @MapFeature.Require(SUPPORTS_REMOVE)
  @CollectionSize.Require(ONE)
  public void testSingletonMapPollFirst() {
    assertEquals(a, navigableMap.pollFirstEntry());
    assertTrue(navigableMap.isEmpty());
  }

  @CollectionSize.Require(ONE)
  public void testSingletonMapNearby() {
    assertNull(navigableMap.lowerEntry(k0()));
    assertNull(navigableMap.lowerKey(k0()));
    assertEquals(a, navigableMap.floorEntry(k0()));
    assertEquals(a.getKey(), navigableMap.floorKey(k0()));
    assertEquals(a, navigableMap.ceilingEntry(k0()));
    assertEquals(a.getKey(), navigableMap.ceilingKey(k0()));
    assertNull(navigableMap.higherEntry(k0()));
    assertNull(navigableMap.higherKey(k0()));
  }

  @CollectionSize.Require(ONE)
  public void testSingletonMapLast() {
    assertEquals(a, navigableMap.lastEntry());
  }

  @MapFeature.Require(SUPPORTS_REMOVE)
  @CollectionSize.Require(ONE)
  public void testSingletonMapPollLast() {
    assertEquals(a, navigableMap.pollLastEntry());
    assertTrue(navigableMap.isEmpty());
  }

  @CollectionSize.Require(SEVERAL)
  public void testFirst() {
    assertEquals(a, navigableMap.firstEntry());
  }

  @MapFeature.Require(SUPPORTS_REMOVE)
  @CollectionSize.Require(SEVERAL)
  public void testPollFirst() {
    assertEquals(a, navigableMap.pollFirstEntry());
    assertEquals(entries.subList(1, entries.size()), Helpers.copyToList(navigableMap.entrySet()));
  }

  @MapFeature.Require(absent = SUPPORTS_REMOVE)
  public void testPollFirstUnsupported() {
    try {
      navigableMap.pollFirstEntry();
      fail();
    } catch (UnsupportedOperationException e) {
    }
  }

  @CollectionSize.Require(SEVERAL)
  public void testLower() {
    resetWithHole();
    assertEquals(null, navigableMap.lowerEntry(a.getKey()));
    assertEquals(null, navigableMap.lowerKey(a.getKey()));
    assertEquals(a, navigableMap.lowerEntry(b.getKey()));
    assertEquals(a.getKey(), navigableMap.lowerKey(b.getKey()));
    assertEquals(a, navigableMap.lowerEntry(c.getKey()));
    assertEquals(a.getKey(), navigableMap.lowerKey(c.getKey()));
  }

  @CollectionSize.Require(SEVERAL)
  public void testFloor() {
    resetWithHole();
    assertEquals(a, navigableMap.floorEntry(a.getKey()));
    assertEquals(a.getKey(), navigableMap.floorKey(a.getKey()));
    assertEquals(a, navigableMap.floorEntry(b.getKey()));
    assertEquals(a.getKey(), navigableMap.floorKey(b.getKey()));
    assertEquals(c, navigableMap.floorEntry(c.getKey()));
    assertEquals(c.getKey(), navigableMap.floorKey(c.getKey()));
  }

  @CollectionSize.Require(SEVERAL)
  public void testCeiling() {
    resetWithHole();
    assertEquals(a, navigableMap.ceilingEntry(a.getKey()));
    assertEquals(a.getKey(), navigableMap.ceilingKey(a.getKey()));
    assertEquals(c, navigableMap.ceilingEntry(b.getKey()));
    assertEquals(c.getKey(), navigableMap.ceilingKey(b.getKey()));
    assertEquals(c, navigableMap.ceilingEntry(c.getKey()));
    assertEquals(c.getKey(), navigableMap.ceilingKey(c.getKey()));
  }

  @CollectionSize.Require(SEVERAL)
  public void testHigher() {
    resetWithHole();
    assertEquals(c, navigableMap.higherEntry(a.getKey()));
    assertEquals(c.getKey(), navigableMap.higherKey(a.getKey()));
    assertEquals(c, navigableMap.higherEntry(b.getKey()));
    assertEquals(c.getKey(), navigableMap.higherKey(b.getKey()));
    assertEquals(null, navigableMap.higherEntry(c.getKey()));
    assertEquals(null, navigableMap.higherKey(c.getKey()));
  }

  @CollectionSize.Require(SEVERAL)
  public void testLast() {
    assertEquals(c, navigableMap.lastEntry());
  }

  @MapFeature.Require(SUPPORTS_REMOVE)
  @CollectionSize.Require(SEVERAL)
  public void testPollLast() {
    assertEquals(c, navigableMap.pollLastEntry());
    assertEquals(
        entries.subList(0, entries.size() - 1), Helpers.copyToList(navigableMap.entrySet()));
  }

  @MapFeature.Require(absent = SUPPORTS_REMOVE)
  @CollectionSize.Require(SEVERAL)
  public void testPollLastUnsupported() {
    try {
      navigableMap.pollLastEntry();
      fail();
    } catch (UnsupportedOperationException e) {
    }
  }

  @CollectionSize.Require(SEVERAL)
  public void testDescendingNavigation() {
    List<Entry<K, V>> descending = new ArrayList<>(navigableMap.descendingMap().entrySet());
    Collections.reverse(descending);
    assertEquals(entries, descending);
  }

  @CollectionSize.Require(absent = ZERO)
  public void testHeadMapExclusive() {
    assertFalse(navigableMap.headMap(a.getKey(), false).containsKey(a.getKey()));
  }

  @CollectionSize.Require(absent = ZERO)
  public void testHeadMapInclusive() {
    assertTrue(navigableMap.headMap(a.getKey(), true).containsKey(a.getKey()));
  }

  @CollectionSize.Require(absent = ZERO)
  public void testTailMapExclusive() {
    assertFalse(navigableMap.tailMap(a.getKey(), false).containsKey(a.getKey()));
  }

  @CollectionSize.Require(absent = ZERO)
  public void testTailMapInclusive() {
    assertTrue(navigableMap.tailMap(a.getKey(), true).containsKey(a.getKey()));
  }
}
