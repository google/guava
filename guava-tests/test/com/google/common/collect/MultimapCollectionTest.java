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
import static com.google.common.collect.testing.features.CollectionFeature.SUPPORTS_REMOVE;
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
import com.google.common.collect.testing.features.CollectionFeature;
import com.google.common.collect.testing.features.CollectionSize;
import com.google.common.collect.testing.features.Feature;
import com.google.common.collect.testing.features.ListFeature;
import com.google.common.collect.testing.features.MapFeature;
import com.google.common.collect.testing.google.ListMultimapTestSuiteBuilder;
import com.google.common.collect.testing.google.MultisetTestSuiteBuilder;
import com.google.common.collect.testing.google.MultisetWritesTester;
import com.google.common.collect.testing.google.SetMultimapTestSuiteBuilder;
import com.google.common.collect.testing.google.SortedSetMultimapTestSuiteBuilder;
import com.google.common.collect.testing.google.TestStringListMultimapGenerator;
import com.google.common.collect.testing.google.TestStringMultisetGenerator;
import com.google.common.collect.testing.google.TestStringSetMultimapGenerator;
import com.google.common.collect.testing.testers.CollectionIteratorTester;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

/**
 * Run collection tests on {@link Multimap} implementations.
 *
 * @author Jared Levy
 */
@GwtIncompatible("suite") // TODO(cpovirk): set up collect/gwt/suites version
public class MultimapCollectionTest extends TestCase {

  static final Feature<?>[] COLLECTION_FEATURES_ORDER = {
    CollectionSize.ANY,
    CollectionFeature.ALLOWS_NULL_VALUES,
    CollectionFeature.KNOWN_ORDER,
    CollectionFeature.GENERAL_PURPOSE
  };
  static final Feature<?>[] COLLECTION_FEATURES_REMOVE = {
    CollectionSize.ANY,
    CollectionFeature.ALLOWS_NULL_VALUES,
    CollectionFeature.SUPPORTS_REMOVE
  };

  static final Feature<?>[] COLLECTION_FEATURES_REMOVE_ORDER = {
    CollectionSize.ANY,
    CollectionFeature.ALLOWS_NULL_VALUES,
    CollectionFeature.KNOWN_ORDER,
    CollectionFeature.SUPPORTS_REMOVE
  };

  private static final Feature<?>[] FOR_MAP_FEATURES_ONE = {
    CollectionSize.ONE,
    ALLOWS_NULL_VALUES,
    SUPPORTS_REMOVE,
  };

  private static final Feature<?>[] FOR_MAP_FEATURES_ANY = {
    CollectionSize.ANY,
    ALLOWS_NULL_VALUES,
    SUPPORTS_REMOVE,
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

    suite.addTest(SetMultimapTestSuiteBuilder.using(new TestStringSetMultimapGenerator() {
          @Override
          protected SetMultimap<String, String> create(Entry<String, String>[] entries) {
            SetMultimap<String, String> multimap = HashMultimap.create();
            for (Entry<String, String> entry : entries) {
              multimap.put(entry.getKey(), entry.getValue());
            }
            return multimap;
          }
        })
        .named("HashMultimap")
        .withFeatures(
            MapFeature.ALLOWS_NULL_KEYS,
            MapFeature.ALLOWS_NULL_VALUES,
            MapFeature.GENERAL_PURPOSE,
            MapFeature.FAILS_FAST_ON_CONCURRENT_MODIFICATION,
            CollectionSize.ANY)
            .createTestSuite());

    suite.addTest(SetMultimapTestSuiteBuilder.using(new TestStringSetMultimapGenerator() {
        @Override
        protected SetMultimap<String, String> create(Entry<String, String>[] entries) {
          SetMultimap<String, String> multimap = LinkedHashMultimap.create();
          for (Entry<String, String> entry : entries) {
            multimap.put(entry.getKey(), entry.getValue());
          }
          return multimap;
        }
      })
      .named("LinkedHashMultimap")
      .withFeatures(
          MapFeature.ALLOWS_NULL_KEYS,
          MapFeature.ALLOWS_NULL_VALUES,
          MapFeature.GENERAL_PURPOSE,
          MapFeature.FAILS_FAST_ON_CONCURRENT_MODIFICATION,
          CollectionFeature.KNOWN_ORDER,
          CollectionSize.ANY)
      .createTestSuite());

    suite.addTest(SortedSetMultimapTestSuiteBuilder.using(new TestStringSetMultimapGenerator() {
        @Override
        protected SetMultimap<String, String> create(Entry<String, String>[] entries) {
          SetMultimap<String, String> multimap = TreeMultimap.create(
              Ordering.natural().nullsFirst(), Ordering.natural().nullsFirst());
          for (Entry<String, String> entry : entries) {
            multimap.put(entry.getKey(), entry.getValue());
          }
          return multimap;
        }

        @Override
        public Iterable<Entry<String, String>> order(List<Entry<String, String>> insertionOrder) {
          return new Ordering<Entry<String, String>>() {
            @Override
            public int compare(Entry<String, String> left, Entry<String, String> right) {
              return ComparisonChain.start()
                  .compare(left.getKey(), right.getKey(), Ordering.natural().nullsFirst())
                  .compare(left.getValue(), right.getValue(), Ordering.natural().nullsFirst())
                  .result();
            }
          }.sortedCopy(insertionOrder);
        }
      })
      .named("TreeMultimap nullsFirst")
      .withFeatures(
          MapFeature.ALLOWS_NULL_KEYS,
          MapFeature.ALLOWS_NULL_VALUES,
          MapFeature.GENERAL_PURPOSE,
          MapFeature.FAILS_FAST_ON_CONCURRENT_MODIFICATION,
          CollectionFeature.KNOWN_ORDER,
          CollectionSize.ANY)
      .createTestSuite());

    suite.addTest(ListMultimapTestSuiteBuilder.using(new TestStringListMultimapGenerator() {
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
          MapFeature.GENERAL_PURPOSE,
          MapFeature.FAILS_FAST_ON_CONCURRENT_MODIFICATION,
          CollectionSize.ANY)
      .createTestSuite());

    suite.addTest(ListMultimapTestSuiteBuilder.using(new TestStringListMultimapGenerator() {
        @Override
        protected ListMultimap<String, String> create(Entry<String, String>[] entries) {
          ListMultimap<String, String> multimap = Multimaps.synchronizedListMultimap(
              ArrayListMultimap.<String, String> create());
          for (Entry<String, String> entry : entries) {
            multimap.put(entry.getKey(), entry.getValue());
          }
          return multimap;
        }
      })
      .named("synchronized ArrayListMultimap")
      .withFeatures(
          MapFeature.ALLOWS_NULL_KEYS,
          MapFeature.ALLOWS_NULL_VALUES,
          MapFeature.GENERAL_PURPOSE,
          MapFeature.FAILS_FAST_ON_CONCURRENT_MODIFICATION,
          CollectionSize.ANY)
      .createTestSuite());

    suite.addTest(ListMultimapTestSuiteBuilder.using(new TestStringListMultimapGenerator() {
        @Override
        protected ListMultimap<String, String> create(Entry<String, String>[] entries) {
          ListMultimap<String, String> multimap = LinkedListMultimap.create();
          for (Entry<String, String> entry : entries) {
            multimap.put(entry.getKey(), entry.getValue());
          }
          return multimap;
        }
      })
      .named("LinkedListMultimap")
      .withFeatures(
          MapFeature.ALLOWS_NULL_KEYS,
          MapFeature.ALLOWS_NULL_VALUES,
          MapFeature.GENERAL_PURPOSE,
          CollectionFeature.KNOWN_ORDER,
          CollectionSize.ANY)
      .createTestSuite());

    suite.addTest(ListMultimapTestSuiteBuilder.using(new TestStringListMultimapGenerator() {
        @Override
        protected ListMultimap<String, String> create(Entry<String, String>[] entries) {
          ImmutableListMultimap.Builder<String, String> builder = ImmutableListMultimap.builder();
          for (Entry<String, String> entry : entries) {
            builder.put(entry.getKey(), entry.getValue());
          }
          return builder.build();
        }
      })
      .named("ImmutableListMultimap")
      .withFeatures(
          MapFeature.ALLOWS_NULL_QUERIES,
          CollectionFeature.KNOWN_ORDER,
          CollectionSize.ANY)
      .createTestSuite());

    suite.addTest(SetMultimapTestSuiteBuilder.using(new TestStringSetMultimapGenerator() {
        @Override
        protected SetMultimap<String, String> create(Entry<String, String>[] entries) {
          ImmutableSetMultimap.Builder<String, String> builder = ImmutableSetMultimap.builder();
          for (Entry<String, String> entry : entries) {
            builder.put(entry.getKey(), entry.getValue());
          }
          return builder.build();
        }
      })
      .named("ImmutableSetMultimap")
      .withFeatures(
          MapFeature.ALLOWS_NULL_QUERIES,
          CollectionFeature.KNOWN_ORDER,
          CollectionSize.ANY)
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
        new TestEntriesGenerator() {
          @Override Multimap<String, Integer> createMultimap() {
            Multimap<String, Integer> multimap = LinkedHashMultimap.create();
            multimap.put("badkey", 3);
            multimap.put("a", 55556);
            return Multimaps.filterEntries(multimap, FILTER_KEYSET_PREDICATE);
          }
        })
        .named("Multimap.filterEntries.entries")
        .withFeatures(CollectionSize.ANY, CollectionFeature.SUPPORTS_REMOVE,
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
        CollectionFeature.SUPPORTS_REMOVE,
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
        CollectionFeature.SUPPORTS_REMOVE).createTestSuite());

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
        .withFeatures(CollectionSize.ANY, CollectionFeature.SUPPORTS_REMOVE)
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
            CollectionFeature.SUPPORTS_REMOVE).createTestSuite());

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
        .withFeatures(CollectionSize.ANY, CollectionFeature.SUPPORTS_REMOVE)
        .createTestSuite());

    // TODO: use collection testers on Multimaps.forMap.entries

    return suite;
  }
}
