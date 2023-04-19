/*
 * Copyright (C) 2011 The Guava Authors
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

import static com.google.common.collect.Maps.immutableEntry;

import com.google.common.base.Function;
import com.google.common.collect.testing.NavigableMapTestSuiteBuilder;
import com.google.common.collect.testing.SafeTreeMap;
import com.google.common.collect.testing.TestStringSortedMapGenerator;
import com.google.common.collect.testing.features.CollectionFeature;
import com.google.common.collect.testing.features.CollectionSize;
import com.google.common.collect.testing.features.MapFeature;
import com.google.common.collect.testing.testers.MapEntrySetTester;
import com.google.common.testing.EqualsTester;
import com.google.common.testing.ForwardingWrapperTester;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.Set;
import java.util.SortedMap;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Tests for {@code ForwardingNavigableMap}.
 *
 * @author Robert Konigsberg
 * @author Louis Wasserman
 */
public class ForwardingNavigableMapTest extends TestCase {
  static class StandardImplForwardingNavigableMap<K, V> extends ForwardingNavigableMap<K, V> {
    private final NavigableMap<K, V> backingMap;

    StandardImplForwardingNavigableMap(NavigableMap<K, V> backingMap) {
      this.backingMap = backingMap;
    }

    @Override
    protected NavigableMap<K, V> delegate() {
      return backingMap;
    }

    @Override
    public boolean containsKey(Object key) {
      return standardContainsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
      return standardContainsValue(value);
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> map) {
      standardPutAll(map);
    }

    @Override
    public @Nullable V remove(Object object) {
      return standardRemove(object);
    }

    @Override
    public boolean equals(@Nullable Object object) {
      return standardEquals(object);
    }

    @Override
    public int hashCode() {
      return standardHashCode();
    }

    @Override
    public Set<K> keySet() {
      /*
       * We can't use StandardKeySet, as NavigableMapTestSuiteBuilder assumes that our keySet is a
       * NavigableSet. We test StandardKeySet in the superclass, so it's still covered.
       */
      return navigableKeySet();
    }

    @Override
    public Collection<V> values() {
      return new StandardValues();
    }

    @Override
    public String toString() {
      return standardToString();
    }

    @Override
    public Set<Entry<K, V>> entrySet() {
      return new StandardEntrySet() {
        @Override
        public Iterator<Entry<K, V>> iterator() {
          return backingMap.entrySet().iterator();
        }
      };
    }

    @Override
    public void clear() {
      standardClear();
    }

    @Override
    public boolean isEmpty() {
      return standardIsEmpty();
    }

    @Override
    public SortedMap<K, V> subMap(K fromKey, K toKey) {
      return standardSubMap(fromKey, toKey);
    }

    @Override
    public @Nullable Entry<K, V> lowerEntry(K key) {
      return standardLowerEntry(key);
    }

    @Override
    public @Nullable K lowerKey(K key) {
      return standardLowerKey(key);
    }

    @Override
    public @Nullable Entry<K, V> floorEntry(K key) {
      return standardFloorEntry(key);
    }

    @Override
    public @Nullable K floorKey(K key) {
      return standardFloorKey(key);
    }

    @Override
    public @Nullable Entry<K, V> ceilingEntry(K key) {
      return standardCeilingEntry(key);
    }

    @Override
    public @Nullable K ceilingKey(K key) {
      return standardCeilingKey(key);
    }

    @Override
    public @Nullable Entry<K, V> higherEntry(K key) {
      return standardHigherEntry(key);
    }

    @Override
    public @Nullable K higherKey(K key) {
      return standardHigherKey(key);
    }

    @Override
    public @Nullable Entry<K, V> firstEntry() {
      return standardFirstEntry();
    }

    /*
     * We can't override lastEntry to delegate to standardLastEntry, as it would create an infinite
     * loop. Instead, we test standardLastEntry manually below.
     */

    @Override
    public @Nullable Entry<K, V> pollFirstEntry() {
      return standardPollFirstEntry();
    }

    @Override
    public @Nullable Entry<K, V> pollLastEntry() {
      return standardPollLastEntry();
    }

    @Override
    public NavigableMap<K, V> descendingMap() {
      return new StandardDescendingMap();
    }

    @Override
    public NavigableSet<K> navigableKeySet() {
      return new StandardNavigableKeySet();
    }

    @Override
    public NavigableSet<K> descendingKeySet() {
      return standardDescendingKeySet();
    }

    @Override
    public K firstKey() {
      return standardFirstKey();
    }

    @Override
    public SortedMap<K, V> headMap(K toKey) {
      return standardHeadMap(toKey);
    }

    @Override
    public K lastKey() {
      return standardLastKey();
    }

    @Override
    public SortedMap<K, V> tailMap(K fromKey) {
      return standardTailMap(fromKey);
    }
  }

  static class StandardLastEntryForwardingNavigableMap<K, V> extends ForwardingNavigableMap<K, V> {
    private final NavigableMap<K, V> backingMap;

    StandardLastEntryForwardingNavigableMap(NavigableMap<K, V> backingMap) {
      this.backingMap = backingMap;
    }

    @Override
    protected NavigableMap<K, V> delegate() {
      return backingMap;
    }

    @Override
    public @Nullable Entry<K, V> lastEntry() {
      return standardLastEntry();
    }
  }

  public static Test suite() {
    TestSuite suite = new TestSuite();

    suite.addTestSuite(ForwardingNavigableMapTest.class);
    suite.addTest(
        NavigableMapTestSuiteBuilder.using(
                new TestStringSortedMapGenerator() {
                  @Override
                  protected SortedMap<String, String> create(Entry<String, String>[] entries) {
                    NavigableMap<String, String> map = new SafeTreeMap<>();
                    for (Entry<String, String> entry : entries) {
                      map.put(entry.getKey(), entry.getValue());
                    }
                    return new StandardImplForwardingNavigableMap<>(map);
                  }
                })
            .named(
                "ForwardingNavigableMap[SafeTreeMap] with no comparator and standard "
                    + "implementations")
            .withFeatures(
                CollectionSize.ANY,
                CollectionFeature.KNOWN_ORDER,
                MapFeature.ALLOWS_NULL_VALUES,
                CollectionFeature.SUPPORTS_ITERATOR_REMOVE,
                MapFeature.GENERAL_PURPOSE)
            /*
             * StandardDescendingMap uses lowerEntry(), and TreeMap.lowerEntry() deliberately
             * produces immutable entries.
             *
             * TODO(cpovirk): Consider making StandardDescendingMap return a ForwardingEntry that
             * supports setValue().
             */
            .suppressing(
                MapEntrySetTester.getSetValueMethod(),
                MapEntrySetTester.getSetValueWithNullValuesAbsentMethod(),
                MapEntrySetTester.getSetValueWithNullValuesPresentMethod())
            .createTestSuite());
    // TODO(lowasser): add forwarding-to-ImmutableSortedMap test
    return suite;
  }

  public void testStandardLastEntry() {
    NavigableMap<String, Integer> forwarding =
        new StandardLastEntryForwardingNavigableMap<>(new SafeTreeMap<String, Integer>());
    assertNull(forwarding.lastEntry());
    forwarding.put("b", 2);
    assertEquals(immutableEntry("b", 2), forwarding.lastEntry());
    forwarding.put("c", 3);
    assertEquals(immutableEntry("c", 3), forwarding.lastEntry());
    forwarding.put("a", 1);
    assertEquals(immutableEntry("c", 3), forwarding.lastEntry());
    forwarding.remove("c");
    assertEquals(immutableEntry("b", 2), forwarding.lastEntry());
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  public void testForwarding() {
    new ForwardingWrapperTester()
        .testForwarding(
            NavigableMap.class,
            new Function<NavigableMap, NavigableMap>() {
              @Override
              public NavigableMap apply(NavigableMap delegate) {
                return wrap(delegate);
              }
            });
  }

  public void testEquals() {
    NavigableMap<Integer, String> map1 = ImmutableSortedMap.of(1, "one");
    NavigableMap<Integer, String> map2 = ImmutableSortedMap.of(2, "two");
    new EqualsTester()
        .addEqualityGroup(map1, wrap(map1), wrap(map1))
        .addEqualityGroup(map2, wrap(map2))
        .testEquals();
  }

  private static <K, V> NavigableMap<K, V> wrap(final NavigableMap<K, V> delegate) {
    return new ForwardingNavigableMap<K, V>() {
      @Override
      protected NavigableMap<K, V> delegate() {
        return delegate;
      }
    };
  }
}
