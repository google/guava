/*
 * Copyright (C) 2009 The Guava Authors
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

package com.google.common.collect.testing;

import static java.util.Arrays.asList;

import com.google.common.annotations.GwtIncompatible;
import com.google.common.collect.testing.features.CollectionFeature;
import com.google.common.collect.testing.features.CollectionSize;
import com.google.common.collect.testing.features.MapFeature;
import com.google.common.collect.testing.testers.MapEntrySetTester;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * Generates a test suite covering the {@link Map} implementations in the {@link java.util} package.
 * Can be subclassed to specify tests that should be suppressed.
 *
 * @author Kevin Bourrillion
 */
@GwtIncompatible
public class TestsForMapsInJavaUtil {

  public static Test suite() {
    return new TestsForMapsInJavaUtil().allTests();
  }

  public Test allTests() {
    TestSuite suite = new TestSuite("java.util Maps");
    suite.addTest(testsForCheckedMap());
    suite.addTest(testsForCheckedNavigableMap());
    suite.addTest(testsForCheckedSortedMap());
    suite.addTest(testsForEmptyMap());
    suite.addTest(testsForEmptyNavigableMap());
    suite.addTest(testsForEmptySortedMap());
    suite.addTest(testsForSingletonMap());
    suite.addTest(testsForHashMap());
    suite.addTest(testsForHashtable());
    suite.addTest(testsForLinkedHashMap());
    suite.addTest(testsForSynchronizedNavigableMap());
    suite.addTest(testsForTreeMapNatural());
    suite.addTest(testsForTreeMapWithComparator());
    suite.addTest(testsForUnmodifiableMap());
    suite.addTest(testsForUnmodifiableNavigableMap());
    suite.addTest(testsForUnmodifiableSortedMap());
    suite.addTest(testsForEnumMap());
    suite.addTest(testsForConcurrentHashMap());
    suite.addTest(testsForConcurrentSkipListMapNatural());
    suite.addTest(testsForConcurrentSkipListMapWithComparator());
    return suite;
  }

  protected Collection<Method> suppressForCheckedMap() {
    return Collections.emptySet();
  }

  protected Collection<Method> suppressForCheckedNavigableMap() {
    return Collections.emptySet();
  }

  protected Collection<Method> suppressForCheckedSortedMap() {
    return Collections.emptySet();
  }

  protected Collection<Method> suppressForEmptyMap() {
    return Collections.emptySet();
  }

  private Collection<Method> suppressForEmptyNavigableMap() {
    return Collections.emptySet();
  }

  private Collection<Method> suppressForEmptySortedMap() {
    return Collections.emptySet();
  }

  protected Collection<Method> suppressForSingletonMap() {
    return Collections.emptySet();
  }

  protected Collection<Method> suppressForHashMap() {
    return Collections.emptySet();
  }

  protected Collection<Method> suppressForHashtable() {
    return Collections.emptySet();
  }

  protected Collection<Method> suppressForLinkedHashMap() {
    return Collections.emptySet();
  }

  protected Collection<Method> suppressForSynchronizedNavigableMap() {
    return Collections.emptySet();
  }

  protected Collection<Method> suppressForTreeMapNatural() {
    return Collections.emptySet();
  }

  protected Collection<Method> suppressForTreeMapWithComparator() {
    return Collections.emptySet();
  }

  protected Collection<Method> suppressForUnmodifiableMap() {
    return Collections.emptySet();
  }

  protected Collection<Method> suppressForUnmodifiableNavigableMap() {
    return Collections.emptySet();
  }

  protected Collection<Method> suppressForUnmodifiableSortedMap() {
    return Collections.emptySet();
  }

  protected Collection<Method> suppressForEnumMap() {
    return Collections.emptySet();
  }

  protected Collection<Method> suppressForConcurrentHashMap() {
    return Collections.emptySet();
  }

  protected Collection<Method> suppressForConcurrentSkipListMap() {
    return asList(
        MapEntrySetTester.getSetValueMethod(),
        MapEntrySetTester.getSetValueWithNullValuesAbsentMethod(),
        MapEntrySetTester.getSetValueWithNullValuesPresentMethod());
  }

  public Test testsForCheckedMap() {
    return MapTestSuiteBuilder.using(
            new TestStringMapGenerator() {
              @Override
              protected Map<String, String> create(Entry<String, String>[] entries) {
                Map<String, String> map = populate(new HashMap<String, String>(), entries);
                return Collections.checkedMap(map, String.class, String.class);
              }
            })
        .named("checkedMap/HashMap")
        .withFeatures(
            MapFeature.GENERAL_PURPOSE,
            MapFeature.ALLOWS_NULL_KEYS,
            MapFeature.ALLOWS_NULL_VALUES,
            MapFeature.ALLOWS_ANY_NULL_QUERIES,
            MapFeature.FAILS_FAST_ON_CONCURRENT_MODIFICATION,
            MapFeature.RESTRICTS_KEYS,
            MapFeature.RESTRICTS_VALUES,
            CollectionFeature.SUPPORTS_ITERATOR_REMOVE,
            CollectionFeature.SERIALIZABLE,
            CollectionSize.ANY)
        .suppressing(suppressForCheckedMap())
        .createTestSuite();
  }

  public Test testsForCheckedNavigableMap() {
    return SortedMapTestSuiteBuilder.using(
            new TestStringSortedMapGenerator() {
              @Override
              protected NavigableMap<String, String> create(Entry<String, String>[] entries) {
                NavigableMap<String, String> map = populate(new TreeMap<String, String>(), entries);
                return Collections.checkedNavigableMap(map, String.class, String.class);
              }
            })
        .named("checkedNavigableMap/TreeMap, natural")
        .withFeatures(
            MapFeature.GENERAL_PURPOSE,
            MapFeature.ALLOWS_NULL_VALUES,
            MapFeature.FAILS_FAST_ON_CONCURRENT_MODIFICATION,
            MapFeature.RESTRICTS_KEYS,
            MapFeature.RESTRICTS_VALUES,
            CollectionFeature.KNOWN_ORDER,
            CollectionFeature.SUPPORTS_ITERATOR_REMOVE,
            CollectionFeature.SERIALIZABLE,
            CollectionSize.ANY)
        .suppressing(suppressForCheckedNavigableMap())
        .createTestSuite();
  }

  public Test testsForCheckedSortedMap() {
    return SortedMapTestSuiteBuilder.using(
            new TestStringSortedMapGenerator() {
              @Override
              protected SortedMap<String, String> create(Entry<String, String>[] entries) {
                SortedMap<String, String> map = populate(new TreeMap<String, String>(), entries);
                return Collections.checkedSortedMap(map, String.class, String.class);
              }
            })
        .named("checkedSortedMap/TreeMap, natural")
        .withFeatures(
            MapFeature.GENERAL_PURPOSE,
            MapFeature.ALLOWS_NULL_VALUES,
            MapFeature.FAILS_FAST_ON_CONCURRENT_MODIFICATION,
            MapFeature.RESTRICTS_KEYS,
            MapFeature.RESTRICTS_VALUES,
            CollectionFeature.KNOWN_ORDER,
            CollectionFeature.SUPPORTS_ITERATOR_REMOVE,
            CollectionFeature.SERIALIZABLE,
            CollectionSize.ANY)
        .suppressing(suppressForCheckedSortedMap())
        .createTestSuite();
  }

  public Test testsForEmptyMap() {
    return MapTestSuiteBuilder.using(
            new TestStringMapGenerator() {
              @Override
              protected Map<String, String> create(Entry<String, String>[] entries) {
                return Collections.emptyMap();
              }
            })
        .named("emptyMap")
        .withFeatures(CollectionFeature.SERIALIZABLE, CollectionSize.ZERO)
        .suppressing(suppressForEmptyMap())
        .createTestSuite();
  }

  public Test testsForEmptyNavigableMap() {
    return MapTestSuiteBuilder.using(
            new TestStringSortedMapGenerator() {
              @Override
              protected NavigableMap<String, String> create(Entry<String, String>[] entries) {
                return Collections.emptyNavigableMap();
              }
            })
        .named("emptyNavigableMap")
        .withFeatures(CollectionFeature.SERIALIZABLE, CollectionSize.ZERO)
        .suppressing(suppressForEmptyNavigableMap())
        .createTestSuite();
  }

  public Test testsForEmptySortedMap() {
    return MapTestSuiteBuilder.using(
            new TestStringSortedMapGenerator() {
              @Override
              protected SortedMap<String, String> create(Entry<String, String>[] entries) {
                return Collections.emptySortedMap();
              }
            })
        .named("emptySortedMap")
        .withFeatures(CollectionFeature.SERIALIZABLE, CollectionSize.ZERO)
        .suppressing(suppressForEmptySortedMap())
        .createTestSuite();
  }

  public Test testsForSingletonMap() {
    return MapTestSuiteBuilder.using(
            new TestStringMapGenerator() {
              @Override
              protected Map<String, String> create(Entry<String, String>[] entries) {
                return Collections.singletonMap(entries[0].getKey(), entries[0].getValue());
              }
            })
        .named("singletonMap")
        .withFeatures(
            MapFeature.ALLOWS_NULL_KEYS,
            MapFeature.ALLOWS_NULL_VALUES,
            MapFeature.ALLOWS_ANY_NULL_QUERIES,
            CollectionFeature.SERIALIZABLE,
            CollectionSize.ONE)
        .suppressing(suppressForSingletonMap())
        .createTestSuite();
  }

  public Test testsForHashMap() {
    return MapTestSuiteBuilder.using(
            new TestStringMapGenerator() {
              @Override
              protected Map<String, String> create(Entry<String, String>[] entries) {
                return toHashMap(entries);
              }
            })
        .named("HashMap")
        .withFeatures(
            MapFeature.GENERAL_PURPOSE,
            MapFeature.ALLOWS_NULL_KEYS,
            MapFeature.ALLOWS_NULL_VALUES,
            MapFeature.ALLOWS_ANY_NULL_QUERIES,
            MapFeature.FAILS_FAST_ON_CONCURRENT_MODIFICATION,
            CollectionFeature.SUPPORTS_ITERATOR_REMOVE,
            CollectionFeature.SERIALIZABLE,
            CollectionSize.ANY)
        .suppressing(suppressForHashMap())
        .createTestSuite();
  }

  public Test testsForHashtable() {
    return MapTestSuiteBuilder.using(
            new TestStringMapGenerator() {
              @Override
              protected Map<String, String> create(Entry<String, String>[] entries) {
                return populate(new Hashtable<String, String>(), entries);
              }
            })
        .withFeatures(
            MapFeature.GENERAL_PURPOSE,
            MapFeature.FAILS_FAST_ON_CONCURRENT_MODIFICATION,
            MapFeature.RESTRICTS_KEYS,
            MapFeature.SUPPORTS_REMOVE,
            CollectionFeature.FAILS_FAST_ON_CONCURRENT_MODIFICATION,
            CollectionFeature.SERIALIZABLE,
            CollectionFeature.SUPPORTS_ITERATOR_REMOVE,
            CollectionFeature.SUPPORTS_REMOVE,
            CollectionSize.ANY)
        .named("Hashtable")
        .suppressing(suppressForHashtable())
        .createTestSuite();
  }

  public Test testsForLinkedHashMap() {
    return MapTestSuiteBuilder.using(
            new TestStringMapGenerator() {
              @Override
              protected Map<String, String> create(Entry<String, String>[] entries) {
                return populate(new LinkedHashMap<String, String>(), entries);
              }
            })
        .named("LinkedHashMap")
        .withFeatures(
            MapFeature.GENERAL_PURPOSE,
            MapFeature.ALLOWS_NULL_KEYS,
            MapFeature.ALLOWS_NULL_VALUES,
            MapFeature.ALLOWS_ANY_NULL_QUERIES,
            MapFeature.FAILS_FAST_ON_CONCURRENT_MODIFICATION,
            CollectionFeature.SUPPORTS_ITERATOR_REMOVE,
            CollectionFeature.KNOWN_ORDER,
            CollectionFeature.SERIALIZABLE,
            CollectionSize.ANY)
        .suppressing(suppressForLinkedHashMap())
        .createTestSuite();
  }

  /**
   * Tests regular NavigableMap behavior of synchronizedNavigableMap(treeMap); does not test the
   * fact that it's synchronized.
   */
  public Test testsForSynchronizedNavigableMap() {
    return NavigableMapTestSuiteBuilder.using(
            new TestStringSortedMapGenerator() {
              @Override
              protected SortedMap<String, String> create(Entry<String, String>[] entries) {
                NavigableMap<String, String> delegate = populate(new TreeMap<>(), entries);
                return Collections.synchronizedNavigableMap(delegate);
              }
            })
        .named("synchronizedNavigableMap/TreeMap, natural")
        .withFeatures(
            MapFeature.GENERAL_PURPOSE,
            MapFeature.ALLOWS_NULL_VALUES,
            MapFeature.FAILS_FAST_ON_CONCURRENT_MODIFICATION,
            CollectionFeature.SUPPORTS_ITERATOR_REMOVE,
            CollectionFeature.KNOWN_ORDER,
            CollectionFeature.SERIALIZABLE,
            CollectionSize.ANY)
        .suppressing(suppressForSynchronizedNavigableMap())
        .createTestSuite();
  }

  public Test testsForTreeMapNatural() {
    return NavigableMapTestSuiteBuilder.using(
            new TestStringSortedMapGenerator() {
              @Override
              protected SortedMap<String, String> create(Entry<String, String>[] entries) {
                /*
                 * TODO(cpovirk): it would be nice to create an input Map and use
                 * the copy constructor here and in the other tests
                 */
                return populate(new TreeMap<String, String>(), entries);
              }
            })
        .named("TreeMap, natural")
        .withFeatures(
            MapFeature.GENERAL_PURPOSE,
            MapFeature.ALLOWS_NULL_VALUES,
            MapFeature.FAILS_FAST_ON_CONCURRENT_MODIFICATION,
            CollectionFeature.SUPPORTS_ITERATOR_REMOVE,
            CollectionFeature.KNOWN_ORDER,
            CollectionFeature.SERIALIZABLE,
            CollectionSize.ANY)
        .suppressing(suppressForTreeMapNatural())
        .createTestSuite();
  }

  public Test testsForTreeMapWithComparator() {
    return NavigableMapTestSuiteBuilder.using(
            new TestStringSortedMapGenerator() {
              @Override
              protected SortedMap<String, String> create(Entry<String, String>[] entries) {
                return populate(
                    new TreeMap<String, String>(arbitraryNullFriendlyComparator()), entries);
              }
            })
        .named("TreeMap, with comparator")
        .withFeatures(
            MapFeature.GENERAL_PURPOSE,
            MapFeature.ALLOWS_NULL_KEYS,
            MapFeature.ALLOWS_NULL_VALUES,
            MapFeature.ALLOWS_ANY_NULL_QUERIES,
            MapFeature.FAILS_FAST_ON_CONCURRENT_MODIFICATION,
            CollectionFeature.SUPPORTS_ITERATOR_REMOVE,
            CollectionFeature.KNOWN_ORDER,
            CollectionFeature.SERIALIZABLE,
            CollectionSize.ANY)
        .suppressing(suppressForTreeMapWithComparator())
        .createTestSuite();
  }

  public Test testsForUnmodifiableMap() {
    return MapTestSuiteBuilder.using(
            new TestStringMapGenerator() {
              @Override
              protected Map<String, String> create(Entry<String, String>[] entries) {
                return Collections.unmodifiableMap(toHashMap(entries));
              }
            })
        .named("unmodifiableMap/HashMap")
        .withFeatures(
            MapFeature.ALLOWS_NULL_KEYS,
            MapFeature.ALLOWS_NULL_VALUES,
            MapFeature.ALLOWS_ANY_NULL_QUERIES,
            CollectionFeature.SERIALIZABLE,
            CollectionSize.ANY)
        .suppressing(suppressForUnmodifiableMap())
        .createTestSuite();
  }

  public Test testsForUnmodifiableNavigableMap() {
    return MapTestSuiteBuilder.using(
            new TestStringSortedMapGenerator() {
              @Override
              protected NavigableMap<String, String> create(Entry<String, String>[] entries) {
                return Collections.unmodifiableNavigableMap(populate(new TreeMap<>(), entries));
              }
            })
        .named("unmodifiableNavigableMap/TreeMap, natural")
        .withFeatures(
            MapFeature.ALLOWS_NULL_VALUES,
            CollectionFeature.KNOWN_ORDER,
            CollectionFeature.SERIALIZABLE,
            CollectionSize.ANY)
        .suppressing(suppressForUnmodifiableNavigableMap())
        .createTestSuite();
  }

  public Test testsForUnmodifiableSortedMap() {
    return MapTestSuiteBuilder.using(
            new TestStringSortedMapGenerator() {
              @Override
              protected SortedMap<String, String> create(Entry<String, String>[] entries) {
                SortedMap<String, String> map = populate(new TreeMap<String, String>(), entries);
                return Collections.unmodifiableSortedMap(map);
              }
            })
        .named("unmodifiableSortedMap/TreeMap, natural")
        .withFeatures(
            MapFeature.ALLOWS_NULL_VALUES,
            CollectionFeature.KNOWN_ORDER,
            CollectionFeature.SERIALIZABLE,
            CollectionSize.ANY)
        .suppressing(suppressForUnmodifiableSortedMap())
        .createTestSuite();
  }

  public Test testsForEnumMap() {
    return MapTestSuiteBuilder.using(
            new TestEnumMapGenerator() {
              @Override
              protected Map<AnEnum, String> create(Entry<AnEnum, String>[] entries) {
                return populate(new EnumMap<AnEnum, String>(AnEnum.class), entries);
              }
            })
        .named("EnumMap")
        .withFeatures(
            MapFeature.GENERAL_PURPOSE,
            MapFeature.ALLOWS_NULL_VALUES,
            MapFeature.RESTRICTS_KEYS,
            CollectionFeature.SUPPORTS_ITERATOR_REMOVE,
            CollectionFeature.KNOWN_ORDER,
            CollectionFeature.SERIALIZABLE,
            CollectionSize.ANY)
        .suppressing(suppressForEnumMap())
        .createTestSuite();
  }

  public Test testsForConcurrentHashMap() {
    return MapTestSuiteBuilder.using(
            new TestStringMapGenerator() {
              @Override
              protected Map<String, String> create(Entry<String, String>[] entries) {
                return populate(new ConcurrentHashMap<String, String>(), entries);
              }
            })
        .named("ConcurrentHashMap")
        .withFeatures(
            MapFeature.GENERAL_PURPOSE,
            CollectionFeature.SUPPORTS_ITERATOR_REMOVE,
            CollectionFeature.SERIALIZABLE,
            CollectionSize.ANY)
        .suppressing(suppressForConcurrentHashMap())
        .createTestSuite();
  }

  public Test testsForConcurrentSkipListMapNatural() {
    return NavigableMapTestSuiteBuilder.using(
            new TestStringSortedMapGenerator() {
              @Override
              protected SortedMap<String, String> create(Entry<String, String>[] entries) {
                return populate(new ConcurrentSkipListMap<String, String>(), entries);
              }
            })
        .named("ConcurrentSkipListMap, natural")
        .withFeatures(
            MapFeature.GENERAL_PURPOSE,
            CollectionFeature.SUPPORTS_ITERATOR_REMOVE,
            CollectionFeature.KNOWN_ORDER,
            CollectionFeature.SERIALIZABLE,
            CollectionSize.ANY)
        .suppressing(suppressForConcurrentSkipListMap())
        .createTestSuite();
  }

  public Test testsForConcurrentSkipListMapWithComparator() {
    return NavigableMapTestSuiteBuilder.using(
            new TestStringSortedMapGenerator() {
              @Override
              protected SortedMap<String, String> create(Entry<String, String>[] entries) {
                return populate(
                    new ConcurrentSkipListMap<String, String>(arbitraryNullFriendlyComparator()),
                    entries);
              }
            })
        .named("ConcurrentSkipListMap, with comparator")
        .withFeatures(
            MapFeature.GENERAL_PURPOSE,
            CollectionFeature.SUPPORTS_ITERATOR_REMOVE,
            CollectionFeature.KNOWN_ORDER,
            CollectionFeature.SERIALIZABLE,
            CollectionSize.ANY)
        .suppressing(suppressForConcurrentSkipListMap())
        .createTestSuite();
  }

  // TODO: IdentityHashMap, AbstractMap

  private static Map<String, String> toHashMap(Entry<String, String>[] entries) {
    return populate(new HashMap<String, String>(), entries);
  }

  // TODO: call conversion constructors or factory methods instead of using
  // populate() on an empty map
  private static <T, M extends Map<T, String>> M populate(M map, Entry<T, String>[] entries) {
    for (Entry<T, String> entry : entries) {
      map.put(entry.getKey(), entry.getValue());
    }
    return map;
  }

  static <T> Comparator<T> arbitraryNullFriendlyComparator() {
    return new NullFriendlyComparator<T>();
  }

  private static final class NullFriendlyComparator<T> implements Comparator<T>, Serializable {
    @Override
    public int compare(T left, T right) {
      return String.valueOf(left).compareTo(String.valueOf(right));
    }
  }
}
