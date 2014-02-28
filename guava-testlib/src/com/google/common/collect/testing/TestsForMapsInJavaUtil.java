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

import com.google.common.collect.testing.features.CollectionFeature;
import com.google.common.collect.testing.features.CollectionSize;
import com.google.common.collect.testing.features.MapFeature;

import junit.framework.Test;
import junit.framework.TestSuite;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Generates a test suite covering the {@link Map} implementations in the
 * {@link java.util} package. Can be subclassed to specify tests that should
 * be suppressed.
 *
 * @author Kevin Bourrillion
 */
public class TestsForMapsInJavaUtil {

  public static Test suite() {
    return new TestsForMapsInJavaUtil().allTests();
  }

  public Test allTests() {
    TestSuite suite = new TestSuite("java.util Maps");
    suite.addTest(testsForEmptyMap());
    suite.addTest(testsForSingletonMap());
    suite.addTest(testsForHashMap());
    suite.addTest(testsForLinkedHashMap());
    suite.addTest(testsForEnumMap());
    suite.addTest(testsForConcurrentHashMap());
    return suite;
  }

  protected Collection<Method> suppressForEmptyMap() {
    return Collections.emptySet();
  }
  protected Collection<Method> suppressForSingletonMap() {
    return Collections.emptySet();
  }
  protected Collection<Method> suppressForHashMap() {
    return Collections.emptySet();
  }
  protected Collection<Method> suppressForLinkedHashMap() {
    return Collections.emptySet();
  }
  protected Collection<Method> suppressForTreeMapNatural() {
    return Collections.emptySet();
  }
  protected Collection<Method> suppressForTreeMapWithComparator() {
    return Collections.emptySet();
  }
  protected Collection<Method> suppressForEnumMap() {
    return Collections.emptySet();
  }
  protected Collection<Method> suppressForConcurrentHashMap() {
    return Collections.emptySet();
  }
  protected Collection<Method> suppressForConcurrentSkipListMap() {
    return Collections.emptySet();
  }

  public Test testsForEmptyMap() {
    return MapTestSuiteBuilder
        .using(new TestStringMapGenerator() {
            @Override protected Map<String, String> create(
                Entry<String, String>[] entries) {
              return Collections.emptyMap();
            }
          })
        .named("emptyMap")
        .withFeatures(
            CollectionFeature.SERIALIZABLE,
            CollectionSize.ZERO)
        .suppressing(suppressForEmptyMap())
        .createTestSuite();
  }

  public Test testsForSingletonMap() {
    return MapTestSuiteBuilder
        .using(new TestStringMapGenerator() {
            @Override protected Map<String, String> create(
                Entry<String, String>[] entries) {
              return Collections.singletonMap(
                  entries[0].getKey(), entries[0].getValue());
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
    return MapTestSuiteBuilder
        .using(new TestStringMapGenerator() {
            @Override protected Map<String, String> create(
                Entry<String, String>[] entries) {
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

  public Test testsForLinkedHashMap() {
    return MapTestSuiteBuilder
        .using(new TestStringMapGenerator() {
            @Override protected Map<String, String> create(
                Entry<String, String>[] entries) {
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

  public Test testsForEnumMap() {
    return MapTestSuiteBuilder
        .using(new TestEnumMapGenerator() {
            @Override protected Map<AnEnum, String> create(
                Entry<AnEnum, String>[] entries) {
              return populate(
                  new EnumMap<AnEnum, String>(AnEnum.class), entries);
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
    return MapTestSuiteBuilder
        .using(new TestStringMapGenerator() {
          @Override protected Map<String, String> create(
              Entry<String, String>[] entries) {
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

  // TODO: IdentityHashMap, AbstractMap

  private static Map<String, String> toHashMap(
      Entry<String, String>[] entries) {
    return populate(new HashMap<String, String>(), entries);
  }

  // TODO: call conversion constructors or factory methods instead of using
  // populate() on an empty map
  private static <T, M extends Map<T, String>> M populate(
      M map, Entry<T, String>[] entries) {
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
