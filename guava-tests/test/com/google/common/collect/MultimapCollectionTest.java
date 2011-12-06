/*
 * Copyright (C) 2008 The Guava Authors
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

import static com.google.common.collect.Maps.newHashMap;
import static com.google.common.collect.testing.features.CollectionFeature.ALLOWS_NULL_VALUES;
import static com.google.common.collect.testing.features.CollectionFeature.REMOVE_OPERATIONS;
import static com.google.common.collect.testing.google.AbstractMultisetSetCountTester.getSetCountDuplicateInitializingMethods;
import static com.google.common.collect.testing.google.MultisetIteratorTester.getIteratorDuplicateInitializingMethods;
import static com.google.common.collect.testing.google.MultisetReadsTester.getReadsDuplicateInitializingMethods;
import static java.lang.reflect.Proxy.newProxyInstance;

import com.google.common.annotations.GwtIncompatible;
import com.google.common.base.Functions;
import com.google.common.base.Predicate;
import com.google.common.base.Supplier;
import com.google.common.collect.testing.CollectionTestSuiteBuilder;
import com.google.common.collect.testing.ListTestSuiteBuilder;
import com.google.common.collect.testing.SampleElements;
import com.google.common.collect.testing.SetTestSuiteBuilder;
import com.google.common.collect.testing.TestCollectionGenerator;
import com.google.common.collect.testing.TestListGenerator;
import com.google.common.collect.testing.TestStringCollectionGenerator;
import com.google.common.collect.testing.TestStringListGenerator;
import com.google.common.collect.testing.TestStringSetGenerator;
import com.google.common.collect.testing.TestStringSortedSetGenerator;
import com.google.common.collect.testing.features.CollectionFeature;
import com.google.common.collect.testing.features.CollectionSize;
import com.google.common.collect.testing.features.Feature;
import com.google.common.collect.testing.features.ListFeature;
import com.google.common.collect.testing.google.MultisetTestSuiteBuilder;
import com.google.common.collect.testing.google.MultisetWritesTester;
import com.google.common.collect.testing.google.TestStringMultisetGenerator;
import com.google.common.collect.testing.testers.CollectionIteratorTester;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Run collection tests on {@link Multimap} implementations.
 *
 * @author Jared Levy
 */
@GwtIncompatible("suite") // TODO(cpovirk): set up collect/gwt/suites version
public class MultimapCollectionTest extends TestCase {

  private static final Feature<?>[] COLLECTION_FEATURES = {
    CollectionSize.ANY,
    CollectionFeature.ALLOWS_NULL_VALUES,
    CollectionFeature.GENERAL_PURPOSE
  };

  static final Feature<?>[] COLLECTION_FEATURES_ORDER = {
    CollectionSize.ANY,
    CollectionFeature.ALLOWS_NULL_VALUES,
    CollectionFeature.KNOWN_ORDER,
    CollectionFeature.GENERAL_PURPOSE
  };
  static final Feature<?>[] COLLECTION_FEATURES_REMOVE = {
    CollectionSize.ANY,
    CollectionFeature.ALLOWS_NULL_VALUES,
    CollectionFeature.REMOVE_OPERATIONS
  };

  static final Feature<?>[] COLLECTION_FEATURES_REMOVE_ORDER = {
    CollectionSize.ANY,
    CollectionFeature.ALLOWS_NULL_VALUES,
    CollectionFeature.KNOWN_ORDER,
    CollectionFeature.REMOVE_OPERATIONS
  };

  private static final Feature<?>[] LIST_FEATURES = {
    CollectionSize.ANY,
    CollectionFeature.ALLOWS_NULL_VALUES,
    ListFeature.GENERAL_PURPOSE
  };

  private static final Feature<?>[] LIST_FEATURES_REMOVE_SET = {
    CollectionSize.ANY,
    CollectionFeature.ALLOWS_NULL_VALUES,
    ListFeature.REMOVE_OPERATIONS,
    ListFeature.SUPPORTS_SET
  };

  private static final Feature<?>[] FOR_MAP_FEATURES_ONE = {
    CollectionSize.ONE,
    ALLOWS_NULL_VALUES,
    REMOVE_OPERATIONS,
  };

  private static final Feature<?>[] FOR_MAP_FEATURES_ANY = {
    CollectionSize.ANY,
    ALLOWS_NULL_VALUES,
    REMOVE_OPERATIONS,
  };

  static final Supplier<TreeSet<String>> STRING_TREESET_FACTORY
      = new Supplier<TreeSet<String>>() {
        @Override
        public TreeSet<String> get() {
          return new TreeSet<String>(Ordering.natural().nullsLast());
        }
      };

  static void populateMultimapForGet(
      Multimap<Integer, String> multimap, String[] elements) {
    multimap.put(2, "foo");
    for (String element : elements) {
      multimap.put(3, element);
    }
  }

  static void populateMultimapForKeySet(
      Multimap<String, Integer> multimap, String[] elements) {
    for (String element : elements) {
      multimap.put(element, 2);
      multimap.put(element, 3);
    }
  }

  static void populateMultimapForValues(
      Multimap<Integer, String> multimap, String[] elements) {
    for (int i = 0; i < elements.length; i++) {
      multimap.put(i % 2, elements[i]);
    }
  }

  static void populateMultimapForKeys(
      Multimap<String, Integer> multimap, String[] elements) {
    for (int i = 0; i < elements.length; i++) {
      multimap.put(elements[i], i);
    }
  }

  /**
   * Implements {@code Multimap.put()} -- and no other methods -- for a {@code
   * Map} by ignoring all but the latest value for each key. This class exists
   * only so that we can use
   * {@link MultimapCollectionTest#populateMultimapForGet(Multimap, String[])}
   * and similar methods to populate a map to be passed to
   * {@link Multimaps#forMap(Map)}. All tests should run against the result of
   * {@link #build()}.
   */
  private static final class PopulatableMapAsMultimap<K, V>
      extends ForwardingMultimap<K, V> {
    final Map<K, V> map;
    final SetMultimap<K, V> unusableDelegate;

    static <K, V> PopulatableMapAsMultimap<K, V> create() {
      return new PopulatableMapAsMultimap<K, V>();
    }

    @SuppressWarnings("unchecked") // all methods throw immediately
    PopulatableMapAsMultimap() {
      this.map = newHashMap();
      this.unusableDelegate = (SetMultimap<K, V>) newProxyInstance(
          SetMultimap.class.getClassLoader(),
          new Class<?>[] {SetMultimap.class},
          new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args)
                throws Throwable {
              throw new UnsupportedOperationException();
            }
          });
    }

    @Override protected Multimap<K, V> delegate() {
      return unusableDelegate;
    }

    @Override public boolean put(K key, V value) {
      map.put(key, value);
      return true;
    }

    SetMultimap<K, V> build() {
      return Multimaps.forMap(map);
    }
  }

  static abstract class TestEntriesGenerator
      implements TestCollectionGenerator<Entry<String, Integer>> {
    @Override
    public SampleElements<Entry<String, Integer>> samples() {
      return new SampleElements<Entry<String, Integer>>(
          Maps.immutableEntry("bar", 1),
          Maps.immutableEntry("bar", 2),
          Maps.immutableEntry("foo", 3),
          Maps.immutableEntry("bar", 3),
          Maps.immutableEntry("cat", 2));
    }

    @Override
    public Collection<Entry<String, Integer>> create(Object... elements) {
      Multimap<String, Integer> multimap = createMultimap();
      for (Object element : elements) {
        @SuppressWarnings("unchecked")
        Entry<String, Integer> entry = (Entry<String, Integer>) element;
        multimap.put(entry.getKey(), entry.getValue());
      }
      return multimap.entries();
    }

    abstract Multimap<String, Integer> createMultimap();

    @Override
    @SuppressWarnings("unchecked")
    public Entry<String, Integer>[] createArray(int length) {
      return (Entry<String, Integer>[]) new Entry<?, ?>[length];
    }

    @Override
    public List<Entry<String, Integer>> order(
        List<Entry<String, Integer>> insertionOrder) {
      return insertionOrder;
    }
  }

  public static abstract class TestEntriesListGenerator
      extends TestEntriesGenerator
      implements TestListGenerator<Entry<String, Integer>> {
    @Override public List<Entry<String, Integer>> create(Object... elements) {
      return (List<Entry<String, Integer>>) super.create(elements);
    }
  }

  private static abstract class TestEntrySetGenerator
      extends TestEntriesGenerator {
    @Override abstract SetMultimap<String, Integer> createMultimap();

    @Override public Set<Entry<String, Integer>> create(Object... elements) {
      return (Set<Entry<String, Integer>>) super.create(elements);
    }
  }

  private static final Predicate<Map.Entry<Integer, String>> FILTER_GET_PREDICATE
      = new Predicate<Map.Entry<Integer, String>>() {
        @Override public boolean apply(Entry<Integer, String> entry) {
          return !"badvalue".equals(entry.getValue()) && 55556 != entry.getKey();
        }
    };

  private static final Predicate<Map.Entry<String, Integer>> FILTER_KEYSET_PREDICATE
    = new Predicate<Map.Entry<String, Integer>>() {
      @Override public boolean apply(Entry<String, Integer> entry) {
        return !"badkey".equals(entry.getKey()) && 55556 != entry.getValue();
      }
  };

  public static Test suite() {
    TestSuite suite = new TestSuite();

    suite.addTest(SetTestSuiteBuilder.using(new TestStringSetGenerator() {
          @Override protected Set<String> create(String[] elements) {
            SetMultimap<Integer, String> multimap = HashMultimap.create();
            populateMultimapForGet(multimap, elements);
            return multimap.get(3);
          }
        })
        .named("HashMultimap.get")
        .withFeatures(COLLECTION_FEATURES)
        .createTestSuite());

    suite.addTest(SetTestSuiteBuilder.using(new TestStringSetGenerator() {
          @Override protected Set<String> create(String[] elements) {
            SetMultimap<Integer, String> multimap
                = LinkedHashMultimap.create();
            populateMultimapForGet(multimap, elements);
            return multimap.get(3);
          }
        })
        .named("LinkedHashMultimap.get")
        .withFeatures(COLLECTION_FEATURES_ORDER)
        .createTestSuite());

    suite.addTest(SetTestSuiteBuilder.using(
        new TestStringSortedSetGenerator() {
          @Override protected SortedSet<String> create(String[] elements) {
            SortedSetMultimap<Integer, String> multimap =
                TreeMultimap.create(Ordering.natural().nullsFirst(),
                    Ordering.natural().nullsLast());
            populateMultimapForGet(multimap, elements);
            return multimap.get(3);
          }
        })
        .named("TreeMultimap.get")
        .withFeatures(COLLECTION_FEATURES_ORDER)
        .createTestSuite());

    suite.addTest(ListTestSuiteBuilder.using(new TestStringListGenerator() {
          @Override protected List<String> create(String[] elements) {
            ListMultimap<Integer, String> multimap
                = ArrayListMultimap.create();
            populateMultimapForGet(multimap, elements);
            return multimap.get(3);
          }
        })
        .named("ArrayListMultimap.get")
        .withFeatures(LIST_FEATURES)
        .createTestSuite());

    suite.addTest(ListTestSuiteBuilder.using(new TestStringListGenerator() {
          @Override protected List<String> create(String[] elements) {
            ListMultimap<Integer, String> multimap
                = Multimaps.synchronizedListMultimap(
                ArrayListMultimap.<Integer, String>create());
            populateMultimapForGet(multimap, elements);
            return multimap.get(3);
          }
        })
        .named("synchronized ArrayListMultimap.get")
        .withFeatures(LIST_FEATURES)
        .createTestSuite());

    suite.addTest(ListTestSuiteBuilder.using(new TestStringListGenerator() {
          @Override protected List<String> create(String[] elements) {
            ListMultimap<Integer, String> multimap
                = LinkedListMultimap.create();
            populateMultimapForGet(multimap, elements);
            return multimap.get(3);
          }
        })
        .named("LinkedListMultimap.get")
        .withFeatures(LIST_FEATURES)
        .createTestSuite());

    suite.addTest(ListTestSuiteBuilder.using(new TestStringListGenerator() {
          @Override protected List<String> create(String[] elements) {
            ImmutableListMultimap.Builder<Integer, String> builder
                = ImmutableListMultimap.builder();
            ListMultimap<Integer, String> multimap
                = builder.put(2, "foo")
                .putAll(3, elements)
                .build();
            return multimap.get(3);
          }
        })
        .named("ImmutableListMultimap.get")
        .withFeatures(CollectionSize.ANY)
        .createTestSuite());

    suite.addTest(SetTestSuiteBuilder.using(
        new TestStringSetGenerator() {
          @Override protected Set<String> create(String[] elements) {
            PopulatableMapAsMultimap<Integer, String> multimap
                = PopulatableMapAsMultimap.create();
            populateMultimapForGet(multimap, elements);
            return multimap.build().get(3);
          }
        })
        .named("Multimaps.forMap.get")
        .withFeatures(FOR_MAP_FEATURES_ONE)
        .createTestSuite());

    suite.addTest(SetTestSuiteBuilder.using(new TestStringSetGenerator() {
          @Override protected Set<String> create(String[] elements) {
            SetMultimap<Integer, String> multimap
                = LinkedHashMultimap.create();
            populateMultimapForGet(multimap, elements);
            multimap.put(3, "badvalue");
            multimap.put(55556, "foo");
            return (Set<String>) Multimaps.filterEntries(multimap, FILTER_GET_PREDICATE).get(3);
          }
        })
        .named("Multimaps.filterEntries.get")
        .withFeatures(COLLECTION_FEATURES_ORDER)
        .suppressing(CollectionIteratorTester.getIteratorKnownOrderRemoveSupportedMethod())
        .createTestSuite());

    suite.addTest(SetTestSuiteBuilder.using(new TestStringSetGenerator() {
          @Override protected Set<String> create(String[] elements) {
            Multimap<String, Integer> multimap = HashMultimap.create();
            populateMultimapForKeySet(multimap, elements);
            return multimap.keySet();
          }
        })
        .named("HashMultimap.keySet")
        .withFeatures(COLLECTION_FEATURES_REMOVE)
        .createTestSuite());

    suite.addTest(SetTestSuiteBuilder.using(new TestStringSetGenerator() {
          @Override protected Set<String> create(String[] elements) {
            Multimap<String, Integer> multimap
                = LinkedHashMultimap.create();
            populateMultimapForKeySet(multimap, elements);
            return multimap.keySet();
          }
        })
        .named("LinkedHashMultimap.keySet")
        .withFeatures(COLLECTION_FEATURES_REMOVE_ORDER)
        .createTestSuite());

    suite.addTest(SetTestSuiteBuilder.using(
        new TestStringSortedSetGenerator() {
          @Override protected SortedSet<String> create(String[] elements) {
            TreeMultimap<String, Integer> multimap =
                TreeMultimap.create(Ordering.natural().nullsFirst(),
                    Ordering.natural().nullsLast());
            populateMultimapForKeySet(multimap, elements);
            return multimap.keySet();
          }
        })
        .named("TreeMultimap.keySet")
        .withFeatures(COLLECTION_FEATURES_REMOVE_ORDER)
        .createTestSuite());

    suite.addTest(SetTestSuiteBuilder.using(new TestStringSetGenerator() {
          @Override protected Set<String> create(String[] elements) {
            Multimap<String, Integer> multimap
                = ArrayListMultimap.create();
            populateMultimapForKeySet(multimap, elements);
            return multimap.keySet();
          }
        })
        .named("ArrayListMultimap.keySet")
        .withFeatures(COLLECTION_FEATURES_REMOVE)
        .createTestSuite());

    suite.addTest(SetTestSuiteBuilder.using(new TestStringSetGenerator() {
          @Override protected Set<String> create(String[] elements) {
            Multimap<String, Integer> multimap
                = LinkedListMultimap.create();
            populateMultimapForKeySet(multimap, elements);
            return multimap.keySet();
          }
        })
        .named("LinkedListMultimap.keySet")
        .withFeatures(COLLECTION_FEATURES_REMOVE_ORDER)
        .createTestSuite());

    suite.addTest(SetTestSuiteBuilder.using(new TestStringSetGenerator() {
          @Override protected Set<String> create(String[] elements) {
            ImmutableListMultimap.Builder<String, Integer> builder
                = ImmutableListMultimap.builder();
            for (String element : elements) {
              builder.put(element, 2);
              builder.put(element, 3);
            }
            Multimap<String, Integer> multimap = builder.build();
            return multimap.keySet();
          }
        })
        .named("ImmutableListMultimap.keySet")
        .withFeatures(CollectionSize.ANY, CollectionFeature.KNOWN_ORDER)
        .createTestSuite());

    suite.addTest(SetTestSuiteBuilder.using(
        new TestStringSetGenerator() {
          @Override protected Set<String> create(String[] elements) {
            PopulatableMapAsMultimap<String, Integer> multimap
                = PopulatableMapAsMultimap.create();
            populateMultimapForKeySet(multimap, elements);
            return multimap.build().keySet();
          }
        })
        .named("Multimaps.forMap.keySet")
        .withFeatures(FOR_MAP_FEATURES_ANY)
        .createTestSuite());

    suite.addTest(SetTestSuiteBuilder.using(
        new TestStringSetGenerator() {
        @Override protected Set<String> create(String[] elements) {
          SetMultimap<String, Integer> multimap = LinkedHashMultimap.create();
          populateMultimapForKeySet(multimap, elements);
          multimap.put("badkey", 3);
          multimap.put("a", 55556);
          return Multimaps.filterEntries(multimap, FILTER_KEYSET_PREDICATE).keySet();
          }
        })
        .named("Multimaps.filterEntries.keySet")
        .withFeatures(COLLECTION_FEATURES_REMOVE_ORDER)
        .suppressing(CollectionIteratorTester.getIteratorKnownOrderRemoveSupportedMethod())
        .createTestSuite());

    suite.addTest(CollectionTestSuiteBuilder.using(
        new TestStringCollectionGenerator() {
          @Override public Collection<String> create(String[] elements) {
            Multimap<Integer, String> multimap = HashMultimap.create();
            populateMultimapForValues(multimap, elements);
            return multimap.values();
          }
        })
        .named("HashMultimap.values")
        .withFeatures(COLLECTION_FEATURES_REMOVE)
        .createTestSuite());

    suite.addTest(CollectionTestSuiteBuilder.using(
        new TestStringCollectionGenerator() {
          @Override public Collection<String> create(String[] elements) {
            Multimap<Integer, String> multimap
                = LinkedHashMultimap.create();
            populateMultimapForValues(multimap, elements);
            return multimap.values();
          }
        })
        .named("LinkedHashMultimap.values")
        .withFeatures(COLLECTION_FEATURES_REMOVE_ORDER)
        .createTestSuite());

    suite.addTest(CollectionTestSuiteBuilder.using(
        new TestStringCollectionGenerator() {
          @Override public Collection<String> create(String[] elements) {
            Multimap<Integer, String> multimap
                = TreeMultimap.create(Ordering.natural().nullsFirst(),
                    Ordering.natural().nullsLast());
            populateMultimapForValues(multimap, elements);
            return multimap.values();
          }
        })
        .named("TreeMultimap.values")
        .withFeatures(COLLECTION_FEATURES_REMOVE)
        .createTestSuite());

    suite.addTest(CollectionTestSuiteBuilder.using(
        new TestStringCollectionGenerator() {
          @Override public Collection<String> create(String[] elements) {
            Multimap<Integer, String> multimap
                = ArrayListMultimap.create();
            populateMultimapForValues(multimap, elements);
            return multimap.values();
          }
        })
        .named("ArrayListMultimap.values")
        .withFeatures(COLLECTION_FEATURES_REMOVE)
        .createTestSuite());

    suite.addTest(ListTestSuiteBuilder.using(
        new TestStringListGenerator() {
          @Override public List<String> create(String[] elements) {
            LinkedListMultimap<Integer, String> multimap
                = LinkedListMultimap.create();
            populateMultimapForValues(multimap, elements);
            return multimap.values();
          }
        })
        .named("LinkedListMultimap.values")
        .withFeatures(LIST_FEATURES_REMOVE_SET)
        .createTestSuite());

    suite.addTest(CollectionTestSuiteBuilder.using(
        new TestStringCollectionGenerator() {
          @Override public Collection<String> create(String[] elements) {
            ImmutableListMultimap.Builder<Integer, String> builder
                = ImmutableListMultimap.builder();
            for (int i = 0; i < elements.length; i++) {
              builder.put(i % 2, elements[i]);
            }
            return builder.build().values();
          }
        })
        .named("ImmutableListMultimap.values")
        .withFeatures(CollectionSize.ANY)
        .createTestSuite());

    suite.addTest(CollectionTestSuiteBuilder.using(
        new TestStringCollectionGenerator() {
          @Override public Collection<String> create(String[] elements) {
            Multimap<Integer, String> multimap
                = LinkedHashMultimap.create();
            populateMultimapForValues(multimap, elements);
            multimap.put(3, "badvalue");
            multimap.put(55556, "foo");
            return Multimaps.filterEntries(multimap, FILTER_GET_PREDICATE).values();
          }
        })
        .named("Multimaps.filterEntries.values")
        .withFeatures(COLLECTION_FEATURES_REMOVE_ORDER)
        .suppressing(CollectionIteratorTester.getIteratorKnownOrderRemoveSupportedMethod())
        .createTestSuite());

    // TODO: use collection testers on Multimaps.forMap.values

    suite.addTest(MultisetTestSuiteBuilder.using(
        new TestStringMultisetGenerator() {
          @Override protected Multiset<String> create(String[] elements) {
            Multimap<String, Integer> multimap = HashMultimap.create();
            populateMultimapForKeys(multimap, elements);
            return multimap.keys();
          }
        })
        .named("HashMultimap.keys")
        .withFeatures(COLLECTION_FEATURES_REMOVE)
        .createTestSuite());

    suite.addTest(MultisetTestSuiteBuilder.using(
        new TestStringMultisetGenerator() {
          @Override protected Multiset<String> create(String[] elements) {
            Multimap<String, Integer> multimap
                = LinkedHashMultimap.create();
            populateMultimapForKeys(multimap, elements);
            return multimap.keys();
          }
        })
        .named("LinkedHashMultimap.keys")
        .withFeatures(COLLECTION_FEATURES_REMOVE_ORDER)
        .createTestSuite());

    suite.addTest(MultisetTestSuiteBuilder.using(
        new TestStringMultisetGenerator() {
          @Override protected Multiset<String> create(String[] elements) {
            Multimap<String, Integer> multimap
                = TreeMultimap.create(Ordering.natural().nullsFirst(),
                    Ordering.natural().nullsLast());
            populateMultimapForKeys(multimap, elements);
            return multimap.keys();
          }

          @Override public List<String> order(List<String> insertionOrder) {
            Collections.sort(insertionOrder, Ordering.natural().nullsFirst());
            return insertionOrder;
          }
        })
        .named("TreeMultimap.keys")
        .withFeatures(COLLECTION_FEATURES_REMOVE_ORDER)
        .createTestSuite());

    suite.addTest(MultisetTestSuiteBuilder.using(
        new TestStringMultisetGenerator() {
          @Override protected Multiset<String> create(String[] elements) {
            Multimap<String, Integer> multimap
                = ArrayListMultimap.create();
            populateMultimapForKeys(multimap, elements);
            return multimap.keys();
          }
        })
        .named("ArrayListMultimap.keys")
        .withFeatures(COLLECTION_FEATURES_REMOVE)
        .createTestSuite());

    suite.addTest(MultisetTestSuiteBuilder.using(
        new TestStringMultisetGenerator() {
          @Override protected Multiset<String> create(String[] elements) {
            Multimap<String, Integer> multimap
                = Multimaps.synchronizedListMultimap(
                    ArrayListMultimap.<String, Integer>create());
            populateMultimapForKeys(multimap, elements);
            return multimap.keys();
          }
        })
        .named("synchronized ArrayListMultimap.keys")
        .withFeatures(COLLECTION_FEATURES_REMOVE)
        .createTestSuite());

    suite.addTest(MultisetTestSuiteBuilder.using(
        new TestStringMultisetGenerator() {
          @Override protected Multiset<String> create(String[] elements) {
            Multimap<String, Integer> multimap
                = LinkedListMultimap.create();
            populateMultimapForKeys(multimap, elements);
            return multimap.keys();
          }
        })
        .named("LinkedListMultimap.keys")
        .withFeatures(COLLECTION_FEATURES_REMOVE_ORDER)
        .createTestSuite());

    suite.addTest(MultisetTestSuiteBuilder.using(
        new TestStringMultisetGenerator() {
          @Override protected Multiset<String> create(String[] elements) {
            ImmutableListMultimap.Builder<String, Integer> builder
                = ImmutableListMultimap.builder();
            for (int i = 0; i < elements.length; i++) {
              builder.put(elements[i], i);
            }
            Multimap<String, Integer> multimap = builder.build();
            return multimap.keys();
          }
        })
        .named("ImmutableListMultimap.keys")
        .withFeatures(CollectionSize.ANY, CollectionFeature.KNOWN_ORDER)
        .createTestSuite());

    suite.addTest(MultisetTestSuiteBuilder.using(
        new TestStringMultisetGenerator() {
          @Override protected Multiset<String> create(String[] elements) {
            PopulatableMapAsMultimap<String, Integer> multimap
                = PopulatableMapAsMultimap.create();
            populateMultimapForKeys(multimap, elements);
            return multimap.build().keys();
          }
        })
        .named("Multimaps.forMap.keys")
        .withFeatures(FOR_MAP_FEATURES_ANY)
        .suppressing(getReadsDuplicateInitializingMethods())
        .suppressing(getSetCountDuplicateInitializingMethods())
        .suppressing(getIteratorDuplicateInitializingMethods())
        .createTestSuite());

    suite.addTest(MultisetTestSuiteBuilder.using(
        new TestStringMultisetGenerator() {
        @Override protected Multiset<String> create(String[] elements) {
          SetMultimap<String, Integer> multimap = LinkedHashMultimap.create();
          populateMultimapForKeys(multimap, elements);
          multimap.put("badkey", 3);
          multimap.put("a", 55556);
          return Multimaps.filterEntries(multimap, FILTER_KEYSET_PREDICATE).keys();
          }
        })
        .named("Multimaps.filterEntries.keys")
        .withFeatures(COLLECTION_FEATURES_REMOVE_ORDER)
        .suppressing(CollectionIteratorTester.getIteratorKnownOrderRemoveSupportedMethod())
        .suppressing(MultisetWritesTester.getEntrySetIteratorMethod())
        .suppressing(getIteratorDuplicateInitializingMethods())
        .createTestSuite());

    suite.addTest(CollectionTestSuiteBuilder.using(
        new TestEntrySetGenerator() {
          @Override SetMultimap<String, Integer> createMultimap() {
            return HashMultimap.create();
          }
        })
        .named("HashMultimap.entries")
        .withFeatures(CollectionSize.ANY, CollectionFeature.REMOVE_OPERATIONS)
        .createTestSuite());

    suite.addTest(CollectionTestSuiteBuilder.using(
        new TestEntrySetGenerator() {
          @Override SetMultimap<String, Integer> createMultimap() {
            return LinkedHashMultimap.create();
          }
        })
        .named("LinkedHashMultimap.entries")
        .withFeatures(CollectionSize.ANY, CollectionFeature.REMOVE_OPERATIONS,
            CollectionFeature.KNOWN_ORDER)
        .createTestSuite());

    suite.addTest(CollectionTestSuiteBuilder.using(
        new TestEntrySetGenerator() {
          @Override SetMultimap<String, Integer> createMultimap() {
            return TreeMultimap.create(Ordering.natural().nullsFirst(),
                Ordering.natural().nullsLast());
          }
        })
        .named("TreeMultimap.entries")
        .withFeatures(CollectionSize.ANY, CollectionFeature.REMOVE_OPERATIONS,
            CollectionFeature.KNOWN_ORDER)
        .createTestSuite());

    suite.addTest(CollectionTestSuiteBuilder.using(
        new TestEntriesGenerator() {
          @Override Multimap<String, Integer> createMultimap() {
            return ArrayListMultimap.create();
          }
        })
        .named("ArrayListMultimap.entries")
        .withFeatures(CollectionSize.ANY, CollectionFeature.REMOVE_OPERATIONS)
        .createTestSuite());

    suite.addTest(CollectionTestSuiteBuilder.using(
        new TestEntriesGenerator() {
          @Override Multimap<String, Integer> createMultimap() {
            return Multimaps.synchronizedListMultimap(
                ArrayListMultimap.<String, Integer>create());
          }
        })
        .named("synchronized ArrayListMultimap.entries")
        .withFeatures(CollectionSize.ANY, CollectionFeature.REMOVE_OPERATIONS)
        .createTestSuite());

    suite.addTest(ListTestSuiteBuilder.using(
        new TestEntriesListGenerator() {
          @Override Multimap<String, Integer> createMultimap() {
            return LinkedListMultimap.create();
          }
        })
        .named("LinkedListMultimap.entries")
        .withFeatures(CollectionSize.ANY, ListFeature.REMOVE_OPERATIONS,
            CollectionFeature.KNOWN_ORDER)
        .createTestSuite());

    suite.addTest(CollectionTestSuiteBuilder.using(
        new TestEntriesGenerator() {
          @Override Multimap<String, Integer> createMultimap() {
            return ImmutableListMultimap.of();
          }

          @Override public Collection<Entry<String, Integer>> create(
              Object... elements) {
            ImmutableListMultimap.Builder<String, Integer> builder
                = ImmutableListMultimap.builder();
            for (Object element : elements) {
              @SuppressWarnings("unchecked")
              Entry<String, Integer> entry = (Entry<String, Integer>) element;
              builder.put(entry.getKey(), entry.getValue());
            }
            return builder.build().entries();
          }
        })
        .named("ImmutableListMultimap.entries")
        .withFeatures(CollectionSize.ANY, CollectionFeature.KNOWN_ORDER)
        .createTestSuite());

    suite.addTest(CollectionTestSuiteBuilder.using(
        new TestEntriesGenerator() {
          @Override Multimap<String, Integer> createMultimap() {
            Multimap<String, Integer> multimap = LinkedHashMultimap.create();
            multimap.put("badkey", 3);
            multimap.put("a", 55556);
            return Multimaps.filterEntries(multimap, FILTER_KEYSET_PREDICATE);
          }
        })
        .named("Multimap.filterEntries.entries")
        .withFeatures(CollectionSize.ANY, CollectionFeature.REMOVE_OPERATIONS,
            CollectionFeature.KNOWN_ORDER)
        .suppressing(CollectionIteratorTester.getIteratorKnownOrderRemoveSupportedMethod())
        .createTestSuite());

    suite.addTest(ListTestSuiteBuilder.using(new TestStringListGenerator() {
      @Override protected List<String> create(String[] elements) {
        ListMultimap<Integer, String> multimap = ArrayListMultimap.create();
        populateMultimapForGet(multimap, elements);
        return Multimaps.transformValues(
            multimap, Functions.<String> identity()).get(3);
      }
    }).named("Multimaps.transformValues[ListMultimap].get").withFeatures(
        CollectionSize.ANY, CollectionFeature.ALLOWS_NULL_VALUES,
        CollectionFeature.REMOVE_OPERATIONS,
        ListFeature.SUPPORTS_REMOVE_WITH_INDEX).createTestSuite());

    suite.addTest(SetTestSuiteBuilder.using(new TestStringSetGenerator() {
      @Override protected Set<String> create(String[] elements) {
        ListMultimap<String, Integer> multimap = ArrayListMultimap.create();
        populateMultimapForKeySet(multimap, elements);
        return Multimaps.transformValues(
            multimap, Functions.<Integer> identity()).keySet();
      }
    }).named("Multimaps.transformValues[ListMultimap].keySet").withFeatures(
        CollectionSize.ANY, CollectionFeature.ALLOWS_NULL_VALUES,
        CollectionFeature.REMOVE_OPERATIONS).createTestSuite());

    suite.addTest(MultisetTestSuiteBuilder.using(
        new TestStringMultisetGenerator() {
          @Override protected Multiset<String> create(String[] elements) {
            ListMultimap<String, Integer> multimap
                = ArrayListMultimap.create();
            populateMultimapForKeys(multimap, elements);
            return Multimaps.transformValues(
                multimap, Functions.<Integer> identity()).keys();
          }
        })
        .named("Multimaps.transform[ListMultimap].keys")
        .withFeatures(COLLECTION_FEATURES_REMOVE)
        .createTestSuite());

    suite.addTest(
        CollectionTestSuiteBuilder.using(new TestStringCollectionGenerator() {
          @Override public Collection<String> create(String[] elements) {
            ListMultimap<Integer, String> multimap = ArrayListMultimap.create();
            populateMultimapForValues(multimap, elements);
            return Multimaps.transformValues(
                multimap, Functions.<String> identity()).values();
          }
        }).named("Multimaps.transformValues[ListMultimap].values").withFeatures(
            COLLECTION_FEATURES_REMOVE).createTestSuite());

    suite.addTest(CollectionTestSuiteBuilder.using(new TestEntriesGenerator() {
      @Override public Collection<Entry<String, Integer>> create(
          Object... elements) {
        ListMultimap<String, Integer> multimap = ArrayListMultimap.create();
        for (Object element : elements) {
          @SuppressWarnings("unchecked")
          Entry<String, Integer> entry = (Entry<String, Integer>) element;
          multimap.put(entry.getKey(), entry.getValue());
        }
        return Multimaps.transformValues(
            multimap, Functions.<Integer> identity()).entries();
      }

      @Override Multimap<String, Integer> createMultimap() {
        return Multimaps.transformValues(
            ArrayListMultimap.<String, Integer> create(),
            Functions.<Integer> identity());
      }
    }).named("Multimaps.transformValues[ListMultimap].entries")
        .withFeatures(CollectionSize.ANY, CollectionFeature.REMOVE_OPERATIONS)
        .createTestSuite());

    suite.addTest(
        CollectionTestSuiteBuilder.using(new TestStringCollectionGenerator() {
          @Override protected Collection<String> create(String[] elements) {
            Multimap<Integer, String> multimap = ArrayListMultimap.create();
            populateMultimapForGet(multimap, elements);
            return Multimaps.transformValues(
                multimap, Functions.<String> identity()).get(3);
          }
        }).named("Multimaps.transformValues[Multimap].get").withFeatures(
            CollectionSize.ANY, CollectionFeature.ALLOWS_NULL_VALUES,
            CollectionFeature.REMOVE_OPERATIONS).createTestSuite());

    suite.addTest(SetTestSuiteBuilder.using(new TestStringSetGenerator() {
      @Override protected Set<String> create(String[] elements) {
        Multimap<String, Integer> multimap = ArrayListMultimap.create();
        populateMultimapForKeySet(multimap, elements);
        return Multimaps.transformValues(
            multimap, Functions.<Integer> identity()).keySet();
      }
    }).named("Multimaps.transformValues[Multimap].keySet").withFeatures(
        COLLECTION_FEATURES_REMOVE).createTestSuite());

    suite.addTest(MultisetTestSuiteBuilder.using(
        new TestStringMultisetGenerator() {
          @Override protected Multiset<String> create(String[] elements) {
            Multimap<String, Integer> multimap
                = ArrayListMultimap.create();
            populateMultimapForKeys(multimap, elements);
            return Multimaps.transformValues(
                multimap, Functions.<Integer> identity()).keys();
          }
        })
        .named("Multimaps.transformValues[Multimap].keys")
        .withFeatures(COLLECTION_FEATURES_REMOVE)
        .createTestSuite());

    suite.addTest(
        CollectionTestSuiteBuilder.using(new TestStringCollectionGenerator() {
          @Override public Collection<String> create(String[] elements) {
            Multimap<Integer, String> multimap = ArrayListMultimap.create();
            populateMultimapForValues(multimap, elements);
            return Multimaps.transformValues(
                multimap, Functions.<String> identity()).values();
          }
        }).named("Multimaps.transformValues[Multimap].values").withFeatures(
            COLLECTION_FEATURES_REMOVE).createTestSuite());

    suite.addTest(CollectionTestSuiteBuilder.using(new TestEntriesGenerator() {
      @Override public Collection<Entry<String, Integer>> create(
          Object... elements) {
        Multimap<String, Integer> multimap = ArrayListMultimap.create();
        for (Object element : elements) {
          @SuppressWarnings("unchecked")
          Entry<String, Integer> entry = (Entry<String, Integer>) element;
          multimap.put(entry.getKey(), entry.getValue());
        }
        return Multimaps.transformValues(
            multimap, Functions.<Integer> identity()).entries();
      }
     @Override Multimap<String, Integer> createMultimap() {
       return Multimaps.transformValues(
           (Multimap<String, Integer>)
                ArrayListMultimap.<String, Integer> create(),
                Functions.<Integer> identity());
      }
    }).named("Multimaps.transformValues[Multimap].entries")
        .withFeatures(CollectionSize.ANY, CollectionFeature.REMOVE_OPERATIONS)
        .createTestSuite());

    // TODO: use collection testers on Multimaps.forMap.entries

    return suite;
  }
}
