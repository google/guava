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
import static com.google.common.collect.testing.Helpers.mapEntry;
import static com.google.common.collect.testing.features.CollectionFeature.ALLOWS_NULL_VALUES;
import static com.google.common.collect.testing.features.CollectionFeature.SUPPORTS_REMOVE;
import static com.google.common.collect.testing.google.AbstractMultisetSetCountTester.getSetCountDuplicateInitializingMethods;
import static com.google.common.collect.testing.google.MultisetCountTester.getCountDuplicateInitializingMethods;
import static com.google.common.collect.testing.google.MultisetElementSetTester.getElementSetDuplicateInitializingMethods;
import static com.google.common.collect.testing.google.MultisetIteratorTester.getIteratorDuplicateInitializingMethods;
import static com.google.common.collect.testing.google.MultisetRemoveTester.getRemoveDuplicateInitializingMethods;
import static java.lang.reflect.Proxy.newProxyInstance;

import com.google.common.annotations.GwtIncompatible;
import com.google.common.base.Ascii;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.base.Supplier;
import com.google.common.collect.Maps.EntryTransformer;
import com.google.common.collect.testing.SampleElements;
import com.google.common.collect.testing.SetTestSuiteBuilder;
import com.google.common.collect.testing.TestCollectionGenerator;
import com.google.common.collect.testing.TestListGenerator;
import com.google.common.collect.testing.TestStringSetGenerator;
import com.google.common.collect.testing.features.CollectionFeature;
import com.google.common.collect.testing.features.CollectionSize;
import com.google.common.collect.testing.features.Feature;
import com.google.common.collect.testing.features.MapFeature;
import com.google.common.collect.testing.google.ListMultimapTestSuiteBuilder;
import com.google.common.collect.testing.google.MultimapFeature;
import com.google.common.collect.testing.google.MultimapTestSuiteBuilder;
import com.google.common.collect.testing.google.MultisetTestSuiteBuilder;
import com.google.common.collect.testing.google.SetMultimapTestSuiteBuilder;
import com.google.common.collect.testing.google.TestListMultimapGenerator;
import com.google.common.collect.testing.google.TestMultimapGenerator;
import com.google.common.collect.testing.google.TestSetMultimapGenerator;
import com.google.common.collect.testing.google.TestStringListMultimapGenerator;
import com.google.common.collect.testing.google.TestStringMultisetGenerator;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Run collection tests on wrappers from {@link Multimaps}.
 *
 * @author Jared Levy
 */
@GwtIncompatible // suite // TODO(cpovirk): set up collect/gwt/suites version
public class MultimapsCollectionTest extends TestCase {

  private static final Feature<?>[] FOR_MAP_FEATURES_ONE = {
    CollectionSize.ONE,
    ALLOWS_NULL_VALUES,
    SUPPORTS_REMOVE,
    CollectionFeature.SUPPORTS_ITERATOR_REMOVE
  };

  private static final Feature<?>[] FOR_MAP_FEATURES_ANY = {
    CollectionSize.ANY,
    ALLOWS_NULL_VALUES,
    SUPPORTS_REMOVE,
    CollectionFeature.SUPPORTS_ITERATOR_REMOVE,
    MultisetTestSuiteBuilder.NoRecurse.NO_ENTRY_SET, // Cannot create entries with count > 1
  };

  static final Supplier<TreeSet<String>> STRING_TREESET_FACTORY =
      new Supplier<TreeSet<String>>() {
        @Override
        public TreeSet<String> get() {
          return new TreeSet<>(Ordering.natural().nullsLast());
        }
      };

  static void populateMultimapForGet(Multimap<Integer, String> multimap, String[] elements) {
    multimap.put(2, "foo");
    for (String element : elements) {
      multimap.put(3, element);
    }
  }

  static void populateMultimapForKeySet(Multimap<String, Integer> multimap, String[] elements) {
    for (String element : elements) {
      multimap.put(element, 2);
      multimap.put(element, 3);
    }
  }

  static void populateMultimapForValues(Multimap<Integer, String> multimap, String[] elements) {
    for (int i = 0; i < elements.length; i++) {
      multimap.put(i % 2, elements[i]);
    }
  }

  static void populateMultimapForKeys(Multimap<String, Integer> multimap, String[] elements) {
    for (int i = 0; i < elements.length; i++) {
      multimap.put(elements[i], i);
    }
  }

  /**
   * Implements {@code Multimap.put()} -- and no other methods -- for a {@code Map} by ignoring all
   * but the latest value for each key. This class exists only so that we can use {@link
   * MultimapsCollectionTest#populateMultimapForGet(Multimap, String[])} and similar methods to
   * populate a map to be passed to {@link Multimaps#forMap(Map)}. All tests should run against the
   * result of {@link #build()}.
   */
  private static final class PopulatableMapAsMultimap<K, V> extends ForwardingMultimap<K, V> {
    final Map<K, V> map;
    final SetMultimap<K, V> unusableDelegate;

    static <K, V> PopulatableMapAsMultimap<K, V> create() {
      return new PopulatableMapAsMultimap<>();
    }

    @SuppressWarnings("unchecked") // all methods throw immediately
    PopulatableMapAsMultimap() {
      this.map = newHashMap();
      this.unusableDelegate =
          (SetMultimap<K, V>)
              newProxyInstance(
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

    @Override
    protected Multimap<K, V> delegate() {
      return unusableDelegate;
    }

    @Override
    public boolean put(K key, V value) {
      map.put(key, value);
      return true;
    }

    SetMultimap<K, V> build() {
      return Multimaps.forMap(map);
    }
  }

  abstract static class TestEntriesGenerator
      implements TestCollectionGenerator<Entry<String, Integer>> {
    @Override
    public SampleElements<Entry<String, Integer>> samples() {
      return new SampleElements<>(
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
    public List<Entry<String, Integer>> order(List<Entry<String, Integer>> insertionOrder) {
      return insertionOrder;
    }
  }

  public abstract static class TestEntriesListGenerator extends TestEntriesGenerator
      implements TestListGenerator<Entry<String, Integer>> {
    @Override
    public List<Entry<String, Integer>> create(Object... elements) {
      return (List<Entry<String, Integer>>) super.create(elements);
    }
  }

  private static final Predicate<Entry<Integer, String>> FILTER_GET_PREDICATE =
      new Predicate<Entry<Integer, String>>() {
        @Override
        public boolean apply(Entry<Integer, String> entry) {
          return !"badvalue".equals(entry.getValue()) && 55556 != entry.getKey();
        }
      };

  private static final Predicate<Entry<String, Integer>> FILTER_KEYSET_PREDICATE =
      new Predicate<Entry<String, Integer>>() {
        @Override
        public boolean apply(Entry<String, Integer> entry) {
          return !"badkey".equals(entry.getKey()) && 55556 != entry.getValue();
        }
      };

  public static Test suite() {
    TestSuite suite = new TestSuite();

    suite.addTest(transformSuite());
    suite.addTest(filterSuite());

    suite.addTest(
        ListMultimapTestSuiteBuilder.using(
                new TestStringListMultimapGenerator() {
                  @Override
                  protected ListMultimap<String, String> create(Entry<String, String>[] entries) {
                    ListMultimap<String, String> multimap =
                        Multimaps.synchronizedListMultimap(
                            ArrayListMultimap.<String, String>create());
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
                MapFeature.ALLOWS_ANY_NULL_QUERIES,
                MapFeature.GENERAL_PURPOSE,
                MapFeature.FAILS_FAST_ON_CONCURRENT_MODIFICATION,
                CollectionFeature.SUPPORTS_ITERATOR_REMOVE,
                CollectionSize.ANY)
            .createTestSuite());

    suite.addTest(
        SetTestSuiteBuilder.using(
                new TestStringSetGenerator() {
                  @Override
                  protected Set<String> create(String[] elements) {
                    PopulatableMapAsMultimap<Integer, String> multimap =
                        PopulatableMapAsMultimap.create();
                    populateMultimapForGet(multimap, elements);
                    return multimap.build().get(3);
                  }
                })
            .named("Multimaps.forMap.get")
            .withFeatures(FOR_MAP_FEATURES_ONE)
            .createTestSuite());

    suite.addTest(
        SetTestSuiteBuilder.using(
                new TestStringSetGenerator() {
                  @Override
                  protected Set<String> create(String[] elements) {
                    PopulatableMapAsMultimap<String, Integer> multimap =
                        PopulatableMapAsMultimap.create();
                    populateMultimapForKeySet(multimap, elements);
                    return multimap.build().keySet();
                  }
                })
            .named("Multimaps.forMap.keySet")
            .withFeatures(FOR_MAP_FEATURES_ANY)
            .createTestSuite());

    // TODO: use collection testers on Multimaps.forMap.values

    suite.addTest(
        MultisetTestSuiteBuilder.using(
                new TestStringMultisetGenerator() {
                  @Override
                  protected Multiset<String> create(String[] elements) {
                    PopulatableMapAsMultimap<String, Integer> multimap =
                        PopulatableMapAsMultimap.create();
                    populateMultimapForKeys(multimap, elements);
                    return multimap.build().keys();
                  }
                })
            .named("Multimaps.forMap.keys")
            .withFeatures(FOR_MAP_FEATURES_ANY)
            .suppressing(getCountDuplicateInitializingMethods())
            .suppressing(getSetCountDuplicateInitializingMethods())
            .suppressing(getIteratorDuplicateInitializingMethods())
            .suppressing(getRemoveDuplicateInitializingMethods())
            .suppressing(getElementSetDuplicateInitializingMethods())
            .createTestSuite());

    // TODO: use collection testers on Multimaps.forMap.entries

    return suite;
  }

  abstract static class TransformedMultimapGenerator<M extends Multimap<String, String>>
      implements TestMultimapGenerator<String, String, M> {

    @Override
    public String[] createKeyArray(int length) {
      return new String[length];
    }

    @Override
    public String[] createValueArray(int length) {
      return new String[length];
    }

    @Override
    public SampleElements<String> sampleKeys() {
      return new SampleElements<>("one", "two", "three", "four", "five");
    }

    @Override
    public SampleElements<String> sampleValues() {
      return new SampleElements<>("january", "february", "march", "april", "may");
    }

    @Override
    public Collection<String> createCollection(Iterable<? extends String> values) {
      return Lists.newArrayList(values);
    }

    @Override
    public SampleElements<Entry<String, String>> samples() {
      return new SampleElements<>(
          mapEntry("one", "january"),
          mapEntry("two", "february"),
          mapEntry("three", "march"),
          mapEntry("four", "april"),
          mapEntry("five", "may"));
    }

    @Override
    public M create(Object... elements) {
      Multimap<String, String> multimap = ArrayListMultimap.create();
      for (Object o : elements) {
        @SuppressWarnings("unchecked")
        Entry<String, String> entry = (Entry<String, String>) o;
        multimap.put(entry.getKey(), Ascii.toUpperCase(entry.getValue()));
      }
      return transform(multimap);
    }

    abstract M transform(Multimap<String, String> multimap);

    @SuppressWarnings("unchecked")
    @Override
    public Entry<String, String>[] createArray(int length) {
      return new Entry[length];
    }

    @Override
    public Iterable<Entry<String, String>> order(List<Entry<String, String>> insertionOrder) {
      return insertionOrder;
    }

    static final Function<String, String> FUNCTION =
        new Function<String, String>() {
          @Override
          public String apply(String value) {
            return Ascii.toLowerCase(value);
          }
        };

    static final EntryTransformer<String, String, String> ENTRY_TRANSFORMER =
        new EntryTransformer<String, String, String>() {
          @Override
          public String transformEntry(String key, String value) {
            return Ascii.toLowerCase(value);
          }
        };
  }

  abstract static class TransformedListMultimapGenerator
      extends TransformedMultimapGenerator<ListMultimap<String, String>>
      implements TestListMultimapGenerator<String, String> {}

  private static Test transformSuite() {
    TestSuite suite = new TestSuite("Multimaps.transform*");
    suite.addTest(
        MultimapTestSuiteBuilder.using(
                new TransformedMultimapGenerator<Multimap<String, String>>() {
                  @Override
                  Multimap<String, String> transform(Multimap<String, String> multimap) {
                    return Multimaps.transformValues(multimap, FUNCTION);
                  }
                })
            .named("Multimaps.transformValues[Multimap]")
            .withFeatures(
                CollectionSize.ANY,
                MapFeature.SUPPORTS_REMOVE,
                CollectionFeature.SUPPORTS_ITERATOR_REMOVE,
                MapFeature.ALLOWS_NULL_KEYS,
                MapFeature.ALLOWS_ANY_NULL_QUERIES)
            .createTestSuite());
    suite.addTest(
        MultimapTestSuiteBuilder.using(
                new TransformedMultimapGenerator<Multimap<String, String>>() {
                  @Override
                  Multimap<String, String> transform(Multimap<String, String> multimap) {
                    return Multimaps.transformEntries(multimap, ENTRY_TRANSFORMER);
                  }
                })
            .named("Multimaps.transformEntries[Multimap]")
            .withFeatures(
                CollectionSize.ANY,
                MapFeature.SUPPORTS_REMOVE,
                CollectionFeature.SUPPORTS_ITERATOR_REMOVE,
                MapFeature.ALLOWS_NULL_KEYS,
                MapFeature.ALLOWS_ANY_NULL_QUERIES)
            .createTestSuite());
    suite.addTest(
        ListMultimapTestSuiteBuilder.using(
                new TransformedListMultimapGenerator() {
                  @Override
                  ListMultimap<String, String> transform(Multimap<String, String> multimap) {
                    return Multimaps.transformValues(
                        (ListMultimap<String, String>) multimap, FUNCTION);
                  }
                })
            .named("Multimaps.transformValues[ListMultimap]")
            .withFeatures(
                CollectionSize.ANY,
                MapFeature.SUPPORTS_REMOVE,
                CollectionFeature.SUPPORTS_ITERATOR_REMOVE,
                MapFeature.ALLOWS_NULL_KEYS,
                MapFeature.ALLOWS_ANY_NULL_QUERIES)
            .createTestSuite());
    suite.addTest(
        ListMultimapTestSuiteBuilder.using(
                new TransformedListMultimapGenerator() {
                  @Override
                  ListMultimap<String, String> transform(Multimap<String, String> multimap) {
                    return Multimaps.transformEntries(
                        (ListMultimap<String, String>) multimap, ENTRY_TRANSFORMER);
                  }
                })
            .named("Multimaps.transformEntries[ListMultimap]")
            .withFeatures(
                CollectionSize.ANY,
                MapFeature.SUPPORTS_REMOVE,
                CollectionFeature.SUPPORTS_ITERATOR_REMOVE,
                MapFeature.ALLOWS_NULL_KEYS,
                MapFeature.ALLOWS_ANY_NULL_QUERIES)
            .createTestSuite());

    // TODO: use collection testers on Multimaps.forMap.entries

    return suite;
  }

  abstract static class TestFilteredMultimapGenerator<M extends Multimap<String, Integer>>
      implements TestMultimapGenerator<String, Integer, M> {

    @Override
    public SampleElements<Entry<String, Integer>> samples() {
      return new SampleElements<>(
          mapEntry("one", 114),
          mapEntry("two", 37),
          mapEntry("three", 42),
          mapEntry("four", 19),
          mapEntry("five", 82));
    }

    @SuppressWarnings("unchecked")
    @Override
    public Entry<String, Integer>[] createArray(int length) {
      return new Entry[length];
    }

    @Override
    public Iterable<Entry<String, Integer>> order(List<Entry<String, Integer>> insertionOrder) {
      return insertionOrder;
    }

    @Override
    public String[] createKeyArray(int length) {
      return new String[length];
    }

    @Override
    public Integer[] createValueArray(int length) {
      return new Integer[length];
    }

    @Override
    public SampleElements<String> sampleKeys() {
      return new SampleElements<>("one", "two", "three", "four", "five");
    }

    @Override
    public SampleElements<Integer> sampleValues() {
      return new SampleElements<>(114, 37, 42, 19, 82);
    }
  }

  abstract static class FilteredSetMultimapGenerator
      extends TestFilteredMultimapGenerator<SetMultimap<String, Integer>>
      implements TestSetMultimapGenerator<String, Integer> {

    abstract SetMultimap<String, Integer> filter(SetMultimap<String, Integer> multimap);

    @Override
    public SetMultimap<String, Integer> create(Object... elements) {
      SetMultimap<String, Integer> multimap = LinkedHashMultimap.create();
      for (Object o : elements) {
        @SuppressWarnings("unchecked")
        Entry<String, Integer> entry = (Entry<String, Integer>) o;
        multimap.put(entry.getKey(), entry.getValue());
      }
      return filter(multimap);
    }

    @Override
    public Collection<Integer> createCollection(Iterable<? extends Integer> values) {
      return Sets.newLinkedHashSet(values);
    }
  }

  abstract static class FilteredListMultimapGenerator
      extends TestFilteredMultimapGenerator<ListMultimap<String, Integer>>
      implements TestListMultimapGenerator<String, Integer> {

    @Override
    public ListMultimap<String, Integer> create(Object... elements) {
      ListMultimap<String, Integer> multimap = LinkedListMultimap.create();
      for (Object o : elements) {
        @SuppressWarnings("unchecked")
        Entry<String, Integer> entry = (Entry<String, Integer>) o;
        multimap.put(entry.getKey(), entry.getValue());
      }
      return filter(multimap);
    }

    abstract ListMultimap<String, Integer> filter(ListMultimap<String, Integer> multimap);

    @Override
    public Collection<Integer> createCollection(Iterable<? extends Integer> values) {
      return Lists.newArrayList(values);
    }
  }

  private static Test filterSuite() {
    TestSuite suite = new TestSuite("Multimaps.filter*");
    suite.addTest(
        SetMultimapTestSuiteBuilder.using(
                new FilteredSetMultimapGenerator() {
                  @Override
                  SetMultimap<String, Integer> filter(SetMultimap<String, Integer> multimap) {
                    multimap.put("foo", 17);
                    multimap.put("bar", 32);
                    multimap.put("foo", 16);
                    return Multimaps.filterKeys(
                        multimap, Predicates.not(Predicates.in(ImmutableSet.of("foo", "bar"))));
                  }
                })
            .named("Multimaps.filterKeys[SetMultimap, Predicate]")
            .withFeatures(
                CollectionSize.ANY,
                MultimapFeature.VALUE_COLLECTIONS_SUPPORT_ITERATOR_REMOVE,
                MapFeature.GENERAL_PURPOSE,
                MapFeature.ALLOWS_NULL_KEYS,
                MapFeature.ALLOWS_NULL_VALUES,
                MapFeature.ALLOWS_ANY_NULL_QUERIES)
            .createTestSuite());

    suite.addTest(
        ListMultimapTestSuiteBuilder.using(
                new FilteredListMultimapGenerator() {
                  @Override
                  ListMultimap<String, Integer> filter(ListMultimap<String, Integer> multimap) {
                    multimap.put("foo", 17);
                    multimap.put("bar", 32);
                    multimap.put("foo", 16);
                    return Multimaps.filterKeys(
                        multimap, Predicates.not(Predicates.in(ImmutableSet.of("foo", "bar"))));
                  }
                })
            .named("Multimaps.filterKeys[ListMultimap, Predicate]")
            .withFeatures(
                CollectionSize.ANY,
                MultimapFeature.VALUE_COLLECTIONS_SUPPORT_ITERATOR_REMOVE,
                MapFeature.GENERAL_PURPOSE,
                MapFeature.ALLOWS_NULL_KEYS,
                MapFeature.ALLOWS_NULL_VALUES,
                MapFeature.ALLOWS_ANY_NULL_QUERIES)
            .createTestSuite());
    suite.addTest(
        ListMultimapTestSuiteBuilder.using(
                new FilteredListMultimapGenerator() {
                  @Override
                  ListMultimap<String, Integer> filter(ListMultimap<String, Integer> multimap) {
                    multimap.put("foo", 17);
                    multimap.put("bar", 32);
                    multimap.put("foo", 16);
                    multimap =
                        Multimaps.filterKeys(multimap, Predicates.not(Predicates.equalTo("foo")));
                    return Multimaps.filterKeys(
                        multimap, Predicates.not(Predicates.equalTo("bar")));
                  }
                })
            .named("Multimaps.filterKeys[Multimaps.filterKeys[ListMultimap], Predicate]")
            .withFeatures(
                CollectionSize.ANY,
                MultimapFeature.VALUE_COLLECTIONS_SUPPORT_ITERATOR_REMOVE,
                MapFeature.GENERAL_PURPOSE,
                MapFeature.ALLOWS_NULL_KEYS,
                MapFeature.ALLOWS_NULL_VALUES,
                MapFeature.ALLOWS_ANY_NULL_QUERIES)
            .createTestSuite());
    suite.addTest(
        SetMultimapTestSuiteBuilder.using(
                new FilteredSetMultimapGenerator() {
                  @Override
                  SetMultimap<String, Integer> filter(SetMultimap<String, Integer> multimap) {
                    multimap.put("one", 314);
                    multimap.put("two", 159);
                    multimap.put("one", 265);
                    return Multimaps.filterValues(
                        multimap, Predicates.not(Predicates.in(ImmutableSet.of(314, 159, 265))));
                  }
                })
            .named("Multimaps.filterValues[SetMultimap, Predicate]")
            .withFeatures(
                CollectionSize.ANY,
                MapFeature.GENERAL_PURPOSE,
                MapFeature.ALLOWS_NULL_KEYS,
                MapFeature.ALLOWS_NULL_VALUES,
                MapFeature.ALLOWS_ANY_NULL_QUERIES)
            .createTestSuite());
    suite.addTest(
        SetMultimapTestSuiteBuilder.using(
                new FilteredSetMultimapGenerator() {
                  @Override
                  SetMultimap<String, Integer> filter(SetMultimap<String, Integer> multimap) {
                    ImmutableSetMultimap<String, Integer> badEntries =
                        ImmutableSetMultimap.of("foo", 314, "one", 159, "two", 265, "bar", 358);
                    multimap.putAll(badEntries);
                    return Multimaps.filterEntries(
                        multimap, Predicates.not(Predicates.in(badEntries.entries())));
                  }
                })
            .named("Multimaps.filterEntries[SetMultimap, Predicate]")
            .withFeatures(
                CollectionSize.ANY,
                MapFeature.GENERAL_PURPOSE,
                MapFeature.ALLOWS_NULL_KEYS,
                MapFeature.ALLOWS_NULL_VALUES,
                MapFeature.ALLOWS_ANY_NULL_QUERIES)
            .createTestSuite());
    suite.addTest(
        SetMultimapTestSuiteBuilder.using(
                new FilteredSetMultimapGenerator() {
                  @Override
                  SetMultimap<String, Integer> filter(SetMultimap<String, Integer> multimap) {
                    ImmutableSetMultimap<String, Integer> badEntries =
                        ImmutableSetMultimap.of("foo", 314, "one", 159, "two", 265, "bar", 358);
                    multimap.putAll(badEntries);
                    multimap =
                        Multimaps.filterKeys(
                            multimap, Predicates.not(Predicates.in(ImmutableSet.of("foo", "bar"))));
                    return Multimaps.filterEntries(
                        multimap, Predicates.not(Predicates.in(badEntries.entries())));
                  }
                })
            .named("Multimaps.filterEntries[Multimaps.filterKeys[SetMultimap]]")
            .withFeatures(
                CollectionSize.ANY,
                MapFeature.GENERAL_PURPOSE,
                MapFeature.ALLOWS_NULL_KEYS,
                MapFeature.ALLOWS_NULL_VALUES,
                MapFeature.ALLOWS_ANY_NULL_QUERIES)
            .createTestSuite());
    suite.addTest(
        SetMultimapTestSuiteBuilder.using(
                new FilteredSetMultimapGenerator() {
                  @Override
                  SetMultimap<String, Integer> filter(SetMultimap<String, Integer> multimap) {
                    ImmutableSetMultimap<String, Integer> badEntries =
                        ImmutableSetMultimap.of("foo", 314, "one", 159, "two", 265, "bar", 358);
                    multimap.putAll(badEntries);
                    multimap =
                        Multimaps.filterEntries(
                            multimap,
                            Predicates.not(
                                Predicates.in(ImmutableMap.of("one", 159, "two", 265).entrySet())));
                    return Multimaps.filterKeys(
                        multimap, Predicates.not(Predicates.in(ImmutableSet.of("foo", "bar"))));
                  }
                })
            .named("Multimaps.filterKeys[Multimaps.filterEntries[SetMultimap]]")
            .withFeatures(
                CollectionSize.ANY,
                MapFeature.GENERAL_PURPOSE,
                MapFeature.ALLOWS_NULL_KEYS,
                MapFeature.ALLOWS_NULL_VALUES,
                MapFeature.ALLOWS_ANY_NULL_QUERIES)
            .createTestSuite());
    suite.addTest(
        SetMultimapTestSuiteBuilder.using(
                new FilteredSetMultimapGenerator() {
                  @Override
                  SetMultimap<String, Integer> filter(SetMultimap<String, Integer> multimap) {
                    ImmutableSetMultimap<String, Integer> badEntries =
                        ImmutableSetMultimap.of("foo", 314, "bar", 358);
                    multimap.putAll(badEntries);
                    multimap =
                        Multimaps.filterKeys(multimap, Predicates.not(Predicates.equalTo("foo")));
                    multimap =
                        Multimaps.filterKeys(multimap, Predicates.not(Predicates.equalTo("bar")));
                    return multimap;
                  }
                })
            .named("Multimaps.filterKeys[Multimaps.filterKeys[SetMultimap]]")
            .withFeatures(
                CollectionSize.ANY,
                MultimapFeature.VALUE_COLLECTIONS_SUPPORT_ITERATOR_REMOVE,
                MapFeature.GENERAL_PURPOSE,
                MapFeature.ALLOWS_NULL_KEYS,
                MapFeature.ALLOWS_NULL_VALUES,
                MapFeature.ALLOWS_ANY_NULL_QUERIES)
            .createTestSuite());
    return suite;
  }
}
