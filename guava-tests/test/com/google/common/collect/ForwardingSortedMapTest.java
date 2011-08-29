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

import com.google.common.collect.testing.MapTestSuiteBuilder;
import com.google.common.collect.testing.SafeTreeMap;
import com.google.common.collect.testing.TestStringMapGenerator;
import com.google.common.collect.testing.features.CollectionFeature;
import com.google.common.collect.testing.features.CollectionSize;
import com.google.common.collect.testing.features.MapFeature;

import junit.framework.Test;
import junit.framework.TestSuite;

import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;

/**
 * Tests for {@code ForwardingSortedMap}.
 *
 * @author Robert Konigsberg
 */
public class ForwardingSortedMapTest extends ForwardingMapTest {
  static class StandardImplForwardingSortedMap<K, V>
      extends ForwardingSortedMap<K, V> {
    private final SortedMap<K, V> backingMap;

    StandardImplForwardingSortedMap(SortedMap<K, V> backingMap) {
      this.backingMap = backingMap;
    }

    @Override protected SortedMap<K, V> delegate() {
      return backingMap;
    }

    @Override public boolean containsKey(Object key) {
      return standardContainsKey(key);
    }

    @Override public boolean containsValue(Object value) {
      return standardContainsValue(value);
    }

    @Override public void putAll(Map<? extends K, ? extends V> map) {
      standardPutAll(map);
    }

    @Override public V remove(Object object) {
      return standardRemove(object);
    }

    @Override public boolean equals(Object object) {
      return standardEquals(object);
    }

    @Override public int hashCode() {
      return standardHashCode();
    }

    @Override public Set<K> keySet() {
      return new StandardKeySet();
    }

    @Override public Collection<V> values() {
      return new StandardValues();
    }

    @Override public String toString() {
      return standardToString();
    }

    @Override public Set<Entry<K, V>> entrySet() {
      return new StandardEntrySet() {
        @Override
        public Iterator<Entry<K, V>> iterator() {
          return backingMap.entrySet().iterator();
        }
      };
    }

    @Override public void clear() {
      standardClear();
    }

    @Override public boolean isEmpty() {
      return standardIsEmpty();
    }

    @Override public SortedMap<K, V> subMap(K fromKey, K toKey) {
      return standardSubMap(fromKey, toKey);
    }
  }

  public static Test suite() {
    TestSuite suite = new TestSuite();

    suite.addTestSuite(ForwardingSortedMapTest.class);
    suite.addTest(MapTestSuiteBuilder.using(new TestStringMapGenerator() {
      @Override protected Map<String, String> create(
          Entry<String, String>[] entries) {
        SortedMap<String, String> map = new SafeTreeMap<String, String>();
        for (Entry<String, String> entry : entries) {
          map.put(entry.getKey(), entry.getValue());
        }
        return new StandardImplForwardingSortedMap<String, String>(map);
      }

      @Override public Iterable<Entry<String, String>> order(
          List<Entry<String, String>> insertionOrder) {
        return sort(insertionOrder);
      }
    }).named("ForwardingSortedMap[SafeTreeMap] with no comparator and standard "
        + "implementations").withFeatures(CollectionSize.ANY,
        CollectionFeature.KNOWN_ORDER, MapFeature.ALLOWS_NULL_VALUES,
        MapFeature.GENERAL_PURPOSE).createTestSuite());
    suite.addTest(MapTestSuiteBuilder.using(new TestStringMapGenerator() {
      private final Comparator<String> comparator =
          Ordering.natural().nullsFirst();

      @Override protected Map<String, String> create(
          Entry<String, String>[] entries) {
        SortedMap<String, String> map =
            new SafeTreeMap<String, String>(comparator);
        for (Entry<String, String> entry : entries) {
          map.put(entry.getKey(), entry.getValue());
        }
        return new StandardImplForwardingSortedMap<String, String>(map);
      }

      @Override public Iterable<Entry<String, String>> order(
          List<Entry<String, String>> insertionOrder) {
        return sort(insertionOrder);
      }
    }).named("ForwardingSortedMap[SafeTreeMap] with natural comparator and "
        + "standard implementations").withFeatures(CollectionSize.ANY,
        CollectionFeature.KNOWN_ORDER, MapFeature.ALLOWS_NULL_VALUES,
        MapFeature.ALLOWS_NULL_KEYS, MapFeature.GENERAL_PURPOSE)
        .createTestSuite());
    suite.addTest(MapTestSuiteBuilder.using(new TestStringMapGenerator() {
      @Override protected Map<String, String> create(
          Entry<String, String>[] entries) {
        ImmutableSortedMap.Builder<String, String> builder =
            ImmutableSortedMap.naturalOrder();
        for (Entry<String, String> entry : entries) {
          builder.put(entry.getKey(), entry.getValue());
        }
        return new StandardImplForwardingSortedMap<String, String>(
            builder.build());
      }
      
      @Override public Iterable<Entry<String, String>> order(
          List<Entry<String, String>> insertionOrder) {
        return sort(insertionOrder);
      }
    }).named("ForwardingSortedMap[ImmutableSortedMap] with standard "
        + "implementations").withFeatures(
        CollectionSize.ANY, MapFeature.REJECTS_DUPLICATES_AT_CREATION,
        MapFeature.ALLOWS_NULL_QUERIES)
        .createTestSuite());

    return suite;
  }
  
  private static Iterable<Entry<String, String>> sort(
      List<Entry<String, String>> entries) {
    SortedMap<String, String> map =
        new SafeTreeMap<String, String>(Ordering.natural().nullsFirst());
    for (Entry<String, String> entry : entries) {
      map.put(entry.getKey(), entry.getValue());
    }
    return map.entrySet();
  }

  @Override public void setUp() throws Exception {
    super.setUp();
    /*
     * Class parameters must be raw, so we can't create a proxy with generic
     * type arguments. The created proxy only records calls and returns null, so
     * the type is irrelevant at runtime.
     */
    @SuppressWarnings("unchecked")
    final SortedMap<String, Boolean> sortedMap =
        createProxyInstance(SortedMap.class);
    forward = new ForwardingSortedMap<String, Boolean>() {
      @Override protected SortedMap<String, Boolean> delegate() {
        return sortedMap;
      }
    };
  }

  public void testComparator() {
    forward().comparator();
    assertEquals("[comparator]", getCalls());
  }

  public void testFirstKey() {
    forward().firstKey();
    assertEquals("[firstKey]", getCalls());
  }

  public void testHeadMap_K() {
    forward().headMap("asdf");
    assertEquals("[headMap(Object)]", getCalls());
  }

  public void testLastKey() {
    forward().lastKey();
    assertEquals("[lastKey]", getCalls());
  }

  public void testSubMap_K_K() {
    forward().subMap("first", "last");
    assertEquals("[subMap(Object,Object)]", getCalls());
  }

  public void testTailMap_K() {
    forward().tailMap("last");
    assertEquals("[tailMap(Object)]", getCalls());
  }

  @Override SortedMap<String, Boolean> forward() {
    return (SortedMap<String, Boolean>) super.forward();
  }
}
