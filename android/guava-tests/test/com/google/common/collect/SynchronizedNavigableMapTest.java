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

package com.google.common.collect;

import com.google.common.collect.Synchronized.SynchronizedNavigableMap;
import com.google.common.collect.Synchronized.SynchronizedNavigableSet;
import com.google.common.collect.Synchronized.SynchronizedSortedMap;
import com.google.common.collect.testing.NavigableMapTestSuiteBuilder;
import com.google.common.collect.testing.SafeTreeMap;
import com.google.common.collect.testing.TestStringSortedMapGenerator;
import com.google.common.collect.testing.features.CollectionFeature;
import com.google.common.collect.testing.features.CollectionSize;
import com.google.common.collect.testing.features.MapFeature;
import com.google.common.testing.SerializableTester;
import java.io.Serializable;
import java.util.Comparator;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.SortedMap;
import junit.framework.TestSuite;

/**
 * Tests for {@link Maps#synchronizedNavigableMap(NavigableMap)}.
 *
 * @author Louis Wasserman
 */
public class SynchronizedNavigableMapTest extends SynchronizedMapTest {
  @Override
  protected <K, V> NavigableMap<K, V> create() {
    @SuppressWarnings("unchecked")
    NavigableMap<K, V> innermost =
        new SafeTreeMap<>((Comparator<? super K>) Ordering.natural().nullsFirst());
    TestMap<K, V> inner = new TestMap<>(innermost, mutex);
    NavigableMap<K, V> outer = Synchronized.navigableMap(inner, mutex);
    return outer;
  }

  static class TestEntry<K, V> extends ForwardingMapEntry<K, V> implements Serializable {
    private final Entry<K, V> delegate;
    private final Object mutex;

    TestEntry(Entry<K, V> delegate, Object mutex) {
      this.delegate = delegate;
      this.mutex = mutex;
    }

    @Override
    protected Entry<K, V> delegate() {
      return delegate;
    }

    @Override
    public boolean equals(Object object) {
      assertTrue(Thread.holdsLock(mutex));
      return super.equals(object);
    }

    @Override
    public K getKey() {
      assertTrue(Thread.holdsLock(mutex));
      return super.getKey();
    }

    @Override
    public V getValue() {
      assertTrue(Thread.holdsLock(mutex));
      return super.getValue();
    }

    @Override
    public int hashCode() {
      assertTrue(Thread.holdsLock(mutex));
      return super.hashCode();
    }

    @Override
    public V setValue(V value) {
      assertTrue(Thread.holdsLock(mutex));
      return super.setValue(value);
    }

    private static final long serialVersionUID = 0;
  }

  static class TestMap<K, V> extends SynchronizedMapTest.TestMap<K, V>
      implements NavigableMap<K, V> {

    public TestMap(NavigableMap<K, V> delegate, Object mutex) {
      super(delegate, mutex);
    }

    @Override
    protected NavigableMap<K, V> delegate() {
      return (NavigableMap<K, V>) super.delegate();
    }

    @Override
    public Entry<K, V> ceilingEntry(K key) {
      assertTrue(Thread.holdsLock(mutex));
      return delegate().ceilingEntry(key);
    }

    @Override
    public K ceilingKey(K key) {
      assertTrue(Thread.holdsLock(mutex));
      return delegate().ceilingKey(key);
    }

    @Override
    public NavigableSet<K> descendingKeySet() {
      assertTrue(Thread.holdsLock(mutex));
      return delegate().descendingKeySet();
    }

    @Override
    public NavigableMap<K, V> descendingMap() {
      assertTrue(Thread.holdsLock(mutex));
      return delegate().descendingMap();
    }

    @Override
    public Entry<K, V> firstEntry() {
      assertTrue(Thread.holdsLock(mutex));
      return delegate().firstEntry();
    }

    @Override
    public Entry<K, V> floorEntry(K key) {
      assertTrue(Thread.holdsLock(mutex));
      return delegate().floorEntry(key);
    }

    @Override
    public K floorKey(K key) {
      assertTrue(Thread.holdsLock(mutex));
      return delegate().floorKey(key);
    }

    @Override
    public NavigableMap<K, V> headMap(K toKey, boolean inclusive) {
      assertTrue(Thread.holdsLock(mutex));
      return delegate().headMap(toKey, inclusive);
    }

    @Override
    public SortedMap<K, V> headMap(K toKey) {
      return headMap(toKey, false);
    }

    @Override
    public Entry<K, V> higherEntry(K key) {
      assertTrue(Thread.holdsLock(mutex));
      return delegate().higherEntry(key);
    }

    @Override
    public K higherKey(K key) {
      assertTrue(Thread.holdsLock(mutex));
      return delegate().higherKey(key);
    }

    @Override
    public Entry<K, V> lastEntry() {
      assertTrue(Thread.holdsLock(mutex));
      return delegate().lastEntry();
    }

    @Override
    public Entry<K, V> lowerEntry(K key) {
      assertTrue(Thread.holdsLock(mutex));
      return delegate().lowerEntry(key);
    }

    @Override
    public K lowerKey(K key) {
      assertTrue(Thread.holdsLock(mutex));
      return delegate().lowerKey(key);
    }

    @Override
    public NavigableSet<K> navigableKeySet() {
      assertTrue(Thread.holdsLock(mutex));
      return delegate().navigableKeySet();
    }

    @Override
    public Entry<K, V> pollFirstEntry() {
      assertTrue(Thread.holdsLock(mutex));
      return delegate().pollFirstEntry();
    }

    @Override
    public Entry<K, V> pollLastEntry() {
      assertTrue(Thread.holdsLock(mutex));
      return delegate().pollLastEntry();
    }

    @Override
    public NavigableMap<K, V> subMap(
        K fromKey, boolean fromInclusive, K toKey, boolean toInclusive) {
      assertTrue(Thread.holdsLock(mutex));
      return delegate().subMap(fromKey, fromInclusive, toKey, toInclusive);
    }

    @Override
    public SortedMap<K, V> subMap(K fromKey, K toKey) {
      return delegate().subMap(fromKey, true, toKey, false);
    }

    @Override
    public NavigableMap<K, V> tailMap(K fromKey, boolean inclusive) {
      assertTrue(Thread.holdsLock(mutex));
      return delegate().tailMap(fromKey, inclusive);
    }

    @Override
    public SortedMap<K, V> tailMap(K fromKey) {
      return tailMap(fromKey, true);
    }

    @Override
    public Comparator<? super K> comparator() {
      assertTrue(Thread.holdsLock(mutex));
      return delegate().comparator();
    }

    @Override
    public K firstKey() {
      assertTrue(Thread.holdsLock(mutex));
      return delegate().firstKey();
    }

    @Override
    public K lastKey() {
      assertTrue(Thread.holdsLock(mutex));
      return delegate().lastKey();
    }

    private static final long serialVersionUID = 0;
  }

  public static TestSuite suite() {
    TestSuite suite = new TestSuite();
    suite.addTestSuite(SynchronizedNavigableMapTest.class);
    suite.addTest(
        NavigableMapTestSuiteBuilder.using(
                new TestStringSortedMapGenerator() {
                  private final Object mutex = new Integer(1);

                  @Override
                  protected SortedMap<String, String> create(Entry<String, String>[] entries) {
                    NavigableMap<String, String> innermost = new SafeTreeMap<>();
                    for (Entry<String, String> entry : entries) {
                      innermost.put(entry.getKey(), entry.getValue());
                    }
                    TestMap<String, String> inner = new TestMap<>(innermost, mutex);
                    NavigableMap<String, String> outer = Synchronized.navigableMap(inner, mutex);
                    return outer;
                  }
                })
            .named("Maps.synchronizedNavigableMap[SafeTreeMap]")
            .withFeatures(
                CollectionSize.ANY,
                CollectionFeature.KNOWN_ORDER,
                MapFeature.GENERAL_PURPOSE,
                MapFeature.ALLOWS_NULL_VALUES,
                CollectionFeature.SUPPORTS_ITERATOR_REMOVE)
            .createTestSuite());

    return suite;
  }

  public void testComparator() {
    create().comparator();
  }

  public void testCeilingEntry() {
    create().ceilingEntry("a");
  }

  public void testCeilingKey() {
    create().ceilingKey("a");
  }

  public void testDescendingKeySet() {
    NavigableMap<String, Integer> map = create();
    NavigableSet<String> descendingKeySet = map.descendingKeySet();
    assertTrue(descendingKeySet instanceof SynchronizedNavigableSet);
    assertSame(mutex, ((SynchronizedNavigableSet<String>) descendingKeySet).mutex);
  }

  public void testDescendingMap() {
    NavigableMap<String, Integer> map = create();
    NavigableMap<String, Integer> descendingMap = map.descendingMap();
    assertTrue(descendingMap instanceof SynchronizedNavigableMap);
    assertSame(mutex, ((SynchronizedNavigableMap<String, Integer>) descendingMap).mutex);
  }

  public void testFirstEntry() {
    create().firstEntry();
  }

  public void testFirstKey() {
    NavigableMap<String, Integer> map = create();
    map.put("a", 1);
    map.firstKey();
  }

  public void testFloorEntry() {
    create().floorEntry("a");
  }

  public void testFloorKey() {
    create().floorKey("a");
  }

  public void testHeadMap_K() {
    NavigableMap<String, Integer> map = create();
    SortedMap<String, Integer> headMap = map.headMap("a");
    assertTrue(headMap instanceof SynchronizedSortedMap);
    assertSame(mutex, ((SynchronizedSortedMap<String, Integer>) headMap).mutex);
  }

  public void testHeadMap_K_B() {
    NavigableMap<String, Integer> map = create();
    NavigableMap<String, Integer> headMap = map.headMap("a", true);
    assertTrue(headMap instanceof SynchronizedNavigableMap);
    assertSame(mutex, ((SynchronizedNavigableMap<String, Integer>) headMap).mutex);
  }

  public void testHigherEntry() {
    create().higherEntry("a");
  }

  public void testHigherKey() {
    create().higherKey("a");
  }

  public void testLastEntry() {
    create().lastEntry();
  }

  public void testLastKey() {
    NavigableMap<String, Integer> map = create();
    map.put("a", 1);
    map.lastKey();
  }

  public void testLowerEntry() {
    create().lowerEntry("a");
  }

  public void testLowerKey() {
    create().lowerKey("a");
  }

  public void testNavigableKeySet() {
    NavigableMap<String, Integer> map = create();
    NavigableSet<String> navigableKeySet = map.navigableKeySet();
    assertTrue(navigableKeySet instanceof SynchronizedNavigableSet);
    assertSame(mutex, ((SynchronizedNavigableSet<String>) navigableKeySet).mutex);
  }

  public void testPollFirstEntry() {
    create().pollFirstEntry();
  }

  public void testPollLastEntry() {
    create().pollLastEntry();
  }

  public void testSubMap_K_K() {
    NavigableMap<String, Integer> map = create();
    SortedMap<String, Integer> subMap = map.subMap("a", "b");
    assertTrue(subMap instanceof SynchronizedSortedMap);
    assertSame(mutex, ((SynchronizedSortedMap<String, Integer>) subMap).mutex);
  }

  public void testSubMap_K_B_K_B() {
    NavigableMap<String, Integer> map = create();
    NavigableMap<String, Integer> subMap = map.subMap("a", true, "b", false);
    assertTrue(subMap instanceof SynchronizedNavigableMap);
    assertSame(mutex, ((SynchronizedNavigableMap<String, Integer>) subMap).mutex);
  }

  public void testTailMap_K() {
    NavigableMap<String, Integer> map = create();
    SortedMap<String, Integer> subMap = map.tailMap("a");
    assertTrue(subMap instanceof SynchronizedSortedMap);
    assertSame(mutex, ((SynchronizedSortedMap<String, Integer>) subMap).mutex);
  }

  public void testTailMap_K_B() {
    NavigableMap<String, Integer> map = create();
    NavigableMap<String, Integer> subMap = map.tailMap("a", true);
    assertTrue(subMap instanceof SynchronizedNavigableMap);
    assertSame(mutex, ((SynchronizedNavigableMap<String, Integer>) subMap).mutex);
  }

  @Override
  public void testSerialization() {
    SerializableTester.reserializeAndAssert(create());
  }
}
