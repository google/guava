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

import com.google.common.base.Function;
import com.google.common.collect.testing.Helpers.NullsBeforeTwo;
import com.google.common.collect.testing.SafeTreeMap;
import com.google.common.collect.testing.SortedMapTestSuiteBuilder;
import com.google.common.collect.testing.TestStringSortedMapGenerator;
import com.google.common.collect.testing.features.CollectionFeature;
import com.google.common.collect.testing.features.CollectionSize;
import com.google.common.collect.testing.features.MapFeature;
import com.google.common.testing.EqualsTester;
import com.google.common.testing.ForwardingWrapperTester;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Tests for {@code ForwardingSortedMap}.
 *
 * @author Robert KonigsbergSortedMapFeature
 */
public class ForwardingSortedMapTest extends TestCase {
  static class StandardImplForwardingSortedMap<K, V> extends ForwardingSortedMap<K, V> {
    private final SortedMap<K, V> backingSortedMap;

    StandardImplForwardingSortedMap(SortedMap<K, V> backingSortedMap) {
      this.backingSortedMap = backingSortedMap;
    }

    @Override
    protected SortedMap<K, V> delegate() {
      return backingSortedMap;
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
      return new StandardKeySet();
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
          return backingSortedMap.entrySet().iterator();
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
  }

  public static Test suite() {
    TestSuite suite = new TestSuite();

    suite.addTestSuite(ForwardingSortedMapTest.class);
    suite.addTest(
        SortedMapTestSuiteBuilder.using(
                new TestStringSortedMapGenerator() {
                  @Override
                  protected SortedMap<String, String> create(Entry<String, String>[] entries) {
                    SortedMap<String, String> map = new SafeTreeMap<>();
                    for (Entry<String, String> entry : entries) {
                      map.put(entry.getKey(), entry.getValue());
                    }
                    return new StandardImplForwardingSortedMap<>(map);
                  }
                })
            .named(
                "ForwardingSortedMap[SafeTreeMap] with no comparator and standard "
                    + "implementations")
            .withFeatures(
                CollectionSize.ANY,
                CollectionFeature.KNOWN_ORDER,
                MapFeature.ALLOWS_NULL_VALUES,
                MapFeature.GENERAL_PURPOSE,
                CollectionFeature.SUPPORTS_ITERATOR_REMOVE)
            .createTestSuite());
    suite.addTest(
        SortedMapTestSuiteBuilder.using(
                new TestStringSortedMapGenerator() {
                  private final Comparator<String> comparator = NullsBeforeTwo.INSTANCE;

                  @Override
                  protected SortedMap<String, String> create(Entry<String, String>[] entries) {
                    SortedMap<String, String> map = new SafeTreeMap<>(comparator);
                    for (Entry<String, String> entry : entries) {
                      map.put(entry.getKey(), entry.getValue());
                    }
                    return new StandardImplForwardingSortedMap<>(map);
                  }
                })
            .named(
                "ForwardingSortedMap[SafeTreeMap] with natural comparator and "
                    + "standard implementations")
            .withFeatures(
                CollectionSize.ANY,
                CollectionFeature.KNOWN_ORDER,
                MapFeature.ALLOWS_NULL_VALUES,
                MapFeature.ALLOWS_NULL_KEYS,
                MapFeature.ALLOWS_ANY_NULL_QUERIES,
                MapFeature.GENERAL_PURPOSE,
                CollectionFeature.SUPPORTS_ITERATOR_REMOVE)
            .createTestSuite());
    suite.addTest(
        SortedMapTestSuiteBuilder.using(
                new TestStringSortedMapGenerator() {
                  @Override
                  protected SortedMap<String, String> create(Entry<String, String>[] entries) {
                    ImmutableSortedMap.Builder<String, String> builder =
                        ImmutableSortedMap.naturalOrder();
                    for (Entry<String, String> entry : entries) {
                      builder.put(entry.getKey(), entry.getValue());
                    }
                    return new StandardImplForwardingSortedMap<>(builder.build());
                  }
                })
            .named("ForwardingSortedMap[ImmutableSortedMap] with standard " + "implementations")
            .withFeatures(
                CollectionSize.ANY,
                MapFeature.REJECTS_DUPLICATES_AT_CREATION,
                MapFeature.ALLOWS_ANY_NULL_QUERIES)
            .createTestSuite());

    return suite;
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  public void testForwarding() {
    new ForwardingWrapperTester()
        .testForwarding(
            SortedMap.class,
            new Function<SortedMap, SortedMap>() {
              @Override
              public SortedMap apply(SortedMap delegate) {
                return wrap(delegate);
              }
            });
  }

  public void testEquals() {
    SortedMap<Integer, String> map1 = ImmutableSortedMap.of(1, "one");
    SortedMap<Integer, String> map2 = ImmutableSortedMap.of(2, "two");
    new EqualsTester()
        .addEqualityGroup(map1, wrap(map1), wrap(map1))
        .addEqualityGroup(map2, wrap(map2))
        .testEquals();
  }

  private static <K, V> SortedMap<K, V> wrap(final SortedMap<K, V> delegate) {
    return new ForwardingSortedMap<K, V>() {
      @Override
      protected SortedMap<K, V> delegate() {
        return delegate;
      }
    };
  }
}
