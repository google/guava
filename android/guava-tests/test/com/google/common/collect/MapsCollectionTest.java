/*
 * Copyright (C) 2012 The Guava Authors
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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.testing.Helpers.mapEntry;

import com.google.common.base.Charsets;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Maps.EntryTransformer;
import com.google.common.collect.testing.Helpers;
import com.google.common.collect.testing.MapTestSuiteBuilder;
import com.google.common.collect.testing.NavigableMapTestSuiteBuilder;
import com.google.common.collect.testing.SafeTreeMap;
import com.google.common.collect.testing.SampleElements;
import com.google.common.collect.testing.SortedMapTestSuiteBuilder;
import com.google.common.collect.testing.TestMapGenerator;
import com.google.common.collect.testing.TestStringMapGenerator;
import com.google.common.collect.testing.TestStringSortedMapGenerator;
import com.google.common.collect.testing.features.CollectionFeature;
import com.google.common.collect.testing.features.CollectionSize;
import com.google.common.collect.testing.features.MapFeature;
import com.google.common.collect.testing.google.BiMapTestSuiteBuilder;
import com.google.common.collect.testing.google.TestStringBiMapGenerator;
import com.google.common.io.BaseEncoding;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import javax.annotation.CheckForNull;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Test suites for wrappers in {@code Maps}.
 *
 * @author Louis Wasserman
 */
public class MapsCollectionTest extends TestCase {
  public static Test suite() {
    TestSuite suite = new TestSuite();

    suite.addTest(
        NavigableMapTestSuiteBuilder.using(
                new TestStringSortedMapGenerator() {
                  @Override
                  protected SortedMap<String, String> create(Entry<String, String>[] entries) {
                    SafeTreeMap<String, String> map = new SafeTreeMap<>();
                    putEntries(map, entries);
                    return Maps.unmodifiableNavigableMap(map);
                  }
                })
            .named("unmodifiableNavigableMap[SafeTreeMap]")
            .withFeatures(
                CollectionSize.ANY, MapFeature.ALLOWS_NULL_VALUES, CollectionFeature.SERIALIZABLE)
            .createTestSuite());
    suite.addTest(
        BiMapTestSuiteBuilder.using(
                new TestStringBiMapGenerator() {
                  @Override
                  protected BiMap<String, String> create(Entry<String, String>[] entries) {
                    BiMap<String, String> bimap = HashBiMap.create(entries.length);
                    for (Entry<String, String> entry : entries) {
                      checkArgument(!bimap.containsKey(entry.getKey()));
                      bimap.put(entry.getKey(), entry.getValue());
                    }
                    return Maps.unmodifiableBiMap(bimap);
                  }
                })
            .named("unmodifiableBiMap[HashBiMap]")
            .withFeatures(
                CollectionSize.ANY,
                MapFeature.ALLOWS_NULL_VALUES,
                MapFeature.ALLOWS_NULL_KEYS,
                MapFeature.ALLOWS_ANY_NULL_QUERIES,
                MapFeature.REJECTS_DUPLICATES_AT_CREATION,
                CollectionFeature.SERIALIZABLE)
            .createTestSuite());
    suite.addTest(
        MapTestSuiteBuilder.using(
                new TestMapGenerator<String, Integer>() {
                  @Override
                  public SampleElements<Entry<String, Integer>> samples() {
                    return new SampleElements<>(
                        mapEntry("x", 1),
                        mapEntry("xxx", 3),
                        mapEntry("xx", 2),
                        mapEntry("xxxx", 4),
                        mapEntry("aaaaa", 5));
                  }

                  @Override
                  public Map<String, Integer> create(Object... elements) {
                    Set<String> set = Sets.newLinkedHashSet();
                    for (Object e : elements) {
                      Entry<?, ?> entry = (Entry<?, ?>) e;
                      checkNotNull(entry.getValue());
                      set.add((String) checkNotNull(entry.getKey()));
                    }
                    return Maps.asMap(
                        set,
                        new Function<String, Integer>() {
                          @Override
                          public Integer apply(String input) {
                            return input.length();
                          }
                        });
                  }

                  @SuppressWarnings("unchecked")
                  @Override
                  public Entry<String, Integer>[] createArray(int length) {
                    return new Entry[length];
                  }

                  @Override
                  public Iterable<Entry<String, Integer>> order(
                      List<Entry<String, Integer>> insertionOrder) {
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
                })
            .named("Maps.asMap[Set, Function]")
            .withFeatures(
                CollectionSize.ANY,
                MapFeature.SUPPORTS_REMOVE,
                CollectionFeature.SUPPORTS_ITERATOR_REMOVE)
            .createTestSuite());
    suite.addTest(
        SortedMapTestSuiteBuilder.using(
                new TestMapGenerator<String, Integer>() {
                  @Override
                  public String[] createKeyArray(int length) {
                    return new String[length];
                  }

                  @Override
                  public Integer[] createValueArray(int length) {
                    return new Integer[length];
                  }

                  @Override
                  public SampleElements<Entry<String, Integer>> samples() {
                    return new SampleElements<>(
                        mapEntry("a", 1),
                        mapEntry("aa", 2),
                        mapEntry("aba", 3),
                        mapEntry("bbbb", 4),
                        mapEntry("ccccc", 5));
                  }

                  @Override
                  public SortedMap<String, Integer> create(Object... elements) {
                    SortedSet<String> set = new NonNavigableSortedSet();
                    for (Object e : elements) {
                      Entry<?, ?> entry = (Entry<?, ?>) e;
                      checkNotNull(entry.getValue());
                      set.add((String) checkNotNull(entry.getKey()));
                    }
                    return Maps.asMap(
                        set,
                        new Function<String, Integer>() {
                          @Override
                          public Integer apply(String input) {
                            return input.length();
                          }
                        });
                  }

                  @SuppressWarnings("unchecked")
                  @Override
                  public Entry<String, Integer>[] createArray(int length) {
                    return new Entry[length];
                  }

                  @Override
                  public Iterable<Entry<String, Integer>> order(
                      List<Entry<String, Integer>> insertionOrder) {
                    Collections.sort(
                        insertionOrder,
                        new Comparator<Entry<String, Integer>>() {
                          @Override
                          public int compare(Entry<String, Integer> o1, Entry<String, Integer> o2) {
                            return o1.getKey().compareTo(o2.getKey());
                          }
                        });
                    return insertionOrder;
                  }
                })
            .named("Maps.asMap[SortedSet, Function]")
            .withFeatures(
                CollectionSize.ANY,
                CollectionFeature.SUPPORTS_ITERATOR_REMOVE,
                MapFeature.SUPPORTS_REMOVE)
            .createTestSuite());
    suite.addTest(
        NavigableMapTestSuiteBuilder.using(
                new TestMapGenerator<String, Integer>() {
                  @Override
                  public String[] createKeyArray(int length) {
                    return new String[length];
                  }

                  @Override
                  public Integer[] createValueArray(int length) {
                    return new Integer[length];
                  }

                  @Override
                  public SampleElements<Entry<String, Integer>> samples() {
                    return new SampleElements<>(
                        mapEntry("a", 1),
                        mapEntry("aa", 2),
                        mapEntry("aba", 3),
                        mapEntry("bbbb", 4),
                        mapEntry("ccccc", 5));
                  }

                  @Override
                  public NavigableMap<String, Integer> create(Object... elements) {
                    NavigableSet<String> set = Sets.newTreeSet(Ordering.natural());
                    for (Object e : elements) {
                      Entry<?, ?> entry = (Entry<?, ?>) e;
                      checkNotNull(entry.getValue());
                      set.add((String) checkNotNull(entry.getKey()));
                    }
                    return Maps.asMap(
                        set,
                        new Function<String, Integer>() {
                          @Override
                          public Integer apply(String input) {
                            return input.length();
                          }
                        });
                  }

                  @SuppressWarnings("unchecked")
                  @Override
                  public Entry<String, Integer>[] createArray(int length) {
                    return new Entry[length];
                  }

                  @Override
                  public Iterable<Entry<String, Integer>> order(
                      List<Entry<String, Integer>> insertionOrder) {
                    Collections.sort(
                        insertionOrder,
                        new Comparator<Entry<String, Integer>>() {
                          @Override
                          public int compare(Entry<String, Integer> o1, Entry<String, Integer> o2) {
                            return o1.getKey().compareTo(o2.getKey());
                          }
                        });
                    return insertionOrder;
                  }
                })
            .named("Maps.asMap[NavigableSet, Function]")
            .withFeatures(
                CollectionSize.ANY,
                MapFeature.SUPPORTS_REMOVE,
                CollectionFeature.SUPPORTS_ITERATOR_REMOVE)
            .createTestSuite());
    suite.addTest(filterSuite());
    suite.addTest(transformSuite());
    return suite;
  }

  static TestSuite filterSuite() {
    TestSuite suite = new TestSuite("Filter");
    suite.addTest(filterMapSuite());
    suite.addTest(filterBiMapSuite());
    suite.addTest(filterSortedMapSuite());
    suite.addTest(filterNavigableMapSuite());
    return suite;
  }

  static TestSuite filterMapSuite() {
    TestSuite suite = new TestSuite("FilterMap");
    suite.addTest(
        MapTestSuiteBuilder.using(
                new TestStringMapGenerator() {
                  @Override
                  protected Map<String, String> create(Entry<String, String>[] entries) {
                    Map<String, String> map = Maps.newHashMap();
                    putEntries(map, entries);
                    map.putAll(ENTRIES_TO_FILTER);
                    return Maps.filterKeys(map, FILTER_KEYS);
                  }
                })
            .named("Maps.filterKeys[Map, Predicate]")
            .withFeatures(
                MapFeature.ALLOWS_NULL_KEYS,
                MapFeature.ALLOWS_NULL_VALUES,
                MapFeature.ALLOWS_ANY_NULL_QUERIES,
                MapFeature.GENERAL_PURPOSE,
                CollectionSize.ANY)
            .createTestSuite());
    suite.addTest(
        MapTestSuiteBuilder.using(
                new TestStringMapGenerator() {
                  @Override
                  protected Map<String, String> create(Entry<String, String>[] entries) {
                    Map<String, String> map = Maps.newHashMap();
                    putEntries(map, entries);
                    map.putAll(ENTRIES_TO_FILTER);
                    return Maps.filterValues(map, FILTER_VALUES);
                  }
                })
            .named("Maps.filterValues[Map, Predicate]")
            .withFeatures(
                MapFeature.ALLOWS_NULL_KEYS,
                MapFeature.ALLOWS_NULL_VALUES,
                MapFeature.ALLOWS_ANY_NULL_QUERIES,
                MapFeature.GENERAL_PURPOSE,
                CollectionSize.ANY)
            .createTestSuite());
    suite.addTest(
        MapTestSuiteBuilder.using(
                new TestStringMapGenerator() {
                  @Override
                  protected Map<String, String> create(Entry<String, String>[] entries) {
                    Map<String, String> map = Maps.newHashMap();
                    putEntries(map, entries);
                    map.putAll(ENTRIES_TO_FILTER);
                    return Maps.filterEntries(map, FILTER_ENTRIES);
                  }
                })
            .named("Maps.filterEntries[Map, Predicate]")
            .withFeatures(
                MapFeature.ALLOWS_NULL_KEYS,
                MapFeature.ALLOWS_NULL_VALUES,
                MapFeature.ALLOWS_ANY_NULL_QUERIES,
                MapFeature.GENERAL_PURPOSE,
                CollectionSize.ANY)
            .createTestSuite());
    suite.addTest(
        MapTestSuiteBuilder.using(
                new TestStringMapGenerator() {
                  @Override
                  protected Map<String, String> create(Entry<String, String>[] entries) {
                    Map<String, String> map = Maps.newHashMap();
                    putEntries(map, entries);
                    map.putAll(ENTRIES_TO_FILTER);
                    map = Maps.filterEntries(map, FILTER_ENTRIES_1);
                    return Maps.filterEntries(map, FILTER_ENTRIES_2);
                  }
                })
            .named("Maps.filterEntries[Maps.filterEntries[Map, Predicate], Predicate]")
            .withFeatures(
                MapFeature.ALLOWS_NULL_KEYS,
                MapFeature.ALLOWS_NULL_VALUES,
                MapFeature.ALLOWS_ANY_NULL_QUERIES,
                MapFeature.GENERAL_PURPOSE,
                CollectionSize.ANY)
            .createTestSuite());
    return suite;
  }

  static TestSuite filterBiMapSuite() {
    TestSuite suite = new TestSuite("FilterBiMap");
    suite.addTest(
        BiMapTestSuiteBuilder.using(
                new TestStringBiMapGenerator() {
                  @Override
                  protected BiMap<String, String> create(Entry<String, String>[] entries) {
                    BiMap<String, String> map = HashBiMap.create();
                    putEntries(map, entries);
                    map.putAll(ENTRIES_TO_FILTER);
                    return Maps.filterKeys(map, FILTER_KEYS);
                  }
                })
            .named("Maps.filterKeys[BiMap, Predicate]")
            .withFeatures(
                MapFeature.ALLOWS_NULL_KEYS,
                MapFeature.ALLOWS_NULL_VALUES,
                MapFeature.GENERAL_PURPOSE,
                CollectionSize.ANY)
            .createTestSuite());
    suite.addTest(
        BiMapTestSuiteBuilder.using(
                new TestStringBiMapGenerator() {
                  @Override
                  protected BiMap<String, String> create(Entry<String, String>[] entries) {
                    BiMap<String, String> map = HashBiMap.create();
                    putEntries(map, entries);
                    map.putAll(ENTRIES_TO_FILTER);
                    return Maps.filterValues(map, FILTER_VALUES);
                  }
                })
            .named("Maps.filterValues[BiMap, Predicate]")
            .withFeatures(
                MapFeature.ALLOWS_NULL_KEYS,
                MapFeature.ALLOWS_NULL_VALUES,
                MapFeature.ALLOWS_ANY_NULL_QUERIES,
                MapFeature.GENERAL_PURPOSE,
                CollectionSize.ANY)
            .createTestSuite());
    suite.addTest(
        BiMapTestSuiteBuilder.using(
                new TestStringBiMapGenerator() {
                  @Override
                  protected BiMap<String, String> create(Entry<String, String>[] entries) {
                    BiMap<String, String> map = HashBiMap.create();
                    putEntries(map, entries);
                    map.putAll(ENTRIES_TO_FILTER);
                    return Maps.filterEntries(map, FILTER_ENTRIES);
                  }
                })
            .named("Maps.filterEntries[BiMap, Predicate]")
            .withFeatures(
                MapFeature.ALLOWS_NULL_KEYS,
                MapFeature.ALLOWS_NULL_VALUES,
                MapFeature.ALLOWS_ANY_NULL_QUERIES,
                MapFeature.GENERAL_PURPOSE,
                CollectionSize.ANY)
            .createTestSuite());
    return suite;
  }

  static TestSuite filterSortedMapSuite() {
    TestSuite suite = new TestSuite("FilterSortedMap");
    suite.addTest(
        SortedMapTestSuiteBuilder.using(
                new TestStringSortedMapGenerator() {
                  @Override
                  protected SortedMap<String, String> create(Entry<String, String>[] entries) {
                    SortedMap<String, String> map = new NonNavigableSortedMap();
                    putEntries(map, entries);
                    map.putAll(ENTRIES_TO_FILTER);
                    return Maps.filterKeys(map, FILTER_KEYS);
                  }
                })
            .named("Maps.filterKeys[SortedMap, Predicate]")
            .withFeatures(
                MapFeature.ALLOWS_NULL_VALUES, MapFeature.GENERAL_PURPOSE, CollectionSize.ANY)
            .createTestSuite());
    suite.addTest(
        SortedMapTestSuiteBuilder.using(
                new TestStringSortedMapGenerator() {
                  @Override
                  protected SortedMap<String, String> create(Entry<String, String>[] entries) {
                    SortedMap<String, String> map = new NonNavigableSortedMap();
                    putEntries(map, entries);
                    map.putAll(ENTRIES_TO_FILTER);
                    return Maps.filterValues(map, FILTER_VALUES);
                  }
                })
            .named("Maps.filterValues[SortedMap, Predicate]")
            .withFeatures(
                MapFeature.ALLOWS_NULL_VALUES, MapFeature.GENERAL_PURPOSE, CollectionSize.ANY)
            .createTestSuite());
    suite.addTest(
        SortedMapTestSuiteBuilder.using(
                new TestStringSortedMapGenerator() {
                  @Override
                  protected SortedMap<String, String> create(Entry<String, String>[] entries) {
                    SortedMap<String, String> map = new NonNavigableSortedMap();
                    putEntries(map, entries);
                    map.putAll(ENTRIES_TO_FILTER);
                    return Maps.filterEntries(map, FILTER_ENTRIES);
                  }
                })
            .named("Maps.filterEntries[SortedMap, Predicate]")
            .withFeatures(
                MapFeature.ALLOWS_NULL_VALUES, MapFeature.GENERAL_PURPOSE, CollectionSize.ANY)
            .createTestSuite());
    return suite;
  }

  static TestSuite filterNavigableMapSuite() {
    TestSuite suite = new TestSuite("FilterNavigableMap");
    suite.addTest(
        NavigableMapTestSuiteBuilder.using(
                new TestStringSortedMapGenerator() {
                  @Override
                  protected NavigableMap<String, String> create(Entry<String, String>[] entries) {
                    NavigableMap<String, String> map = new SafeTreeMap<>();
                    putEntries(map, entries);
                    map.put("banana", "toast");
                    map.put("eggplant", "spam");
                    return Maps.filterKeys(map, FILTER_KEYS);
                  }
                })
            .named("Maps.filterKeys[NavigableMap, Predicate]")
            .withFeatures(
                MapFeature.ALLOWS_NULL_VALUES, MapFeature.GENERAL_PURPOSE, CollectionSize.ANY)
            .createTestSuite());
    suite.addTest(
        NavigableMapTestSuiteBuilder.using(
                new TestStringSortedMapGenerator() {
                  @Override
                  protected NavigableMap<String, String> create(Entry<String, String>[] entries) {
                    NavigableMap<String, String> map = new SafeTreeMap<>();
                    putEntries(map, entries);
                    map.put("banana", "toast");
                    map.put("eggplant", "spam");
                    return Maps.filterValues(map, FILTER_VALUES);
                  }
                })
            .named("Maps.filterValues[NavigableMap, Predicate]")
            .withFeatures(
                MapFeature.ALLOWS_NULL_VALUES, MapFeature.GENERAL_PURPOSE, CollectionSize.ANY)
            .createTestSuite());
    suite.addTest(
        NavigableMapTestSuiteBuilder.using(
                new TestStringSortedMapGenerator() {
                  @Override
                  protected NavigableMap<String, String> create(Entry<String, String>[] entries) {
                    NavigableMap<String, String> map = new SafeTreeMap<>();
                    putEntries(map, entries);
                    map.put("banana", "toast");
                    map.put("eggplant", "spam");
                    return Maps.filterEntries(map, FILTER_ENTRIES);
                  }
                })
            .named("Maps.filterEntries[NavigableMap, Predicate]")
            .withFeatures(
                MapFeature.ALLOWS_NULL_VALUES, MapFeature.GENERAL_PURPOSE, CollectionSize.ANY)
            .createTestSuite());
    return suite;
  }

  static void putEntries(Map<String, String> map, Entry<String, String>[] entries) {
    for (Entry<String, String> entry : entries) {
      map.put(entry.getKey(), entry.getValue());
    }
  }

  static final Predicate<String> FILTER_KEYS =
      new Predicate<String>() {
        @Override
        public boolean apply(@CheckForNull String string) {
          return !"banana".equals(string) && !"eggplant".equals(string);
        }
      };

  static final Predicate<String> FILTER_VALUES =
      new Predicate<String>() {
        @Override
        public boolean apply(@CheckForNull String string) {
          return !"toast".equals(string) && !"spam".equals(string);
        }
      };

  static final Predicate<Entry<String, String>> FILTER_ENTRIES =
      new Predicate<Entry<String, String>>() {
        @Override
        public boolean apply(Entry<String, String> entry) {
          return !Helpers.mapEntry("banana", "toast").equals(entry)
              && !Helpers.mapEntry("eggplant", "spam").equals(entry);
        }
      };

  static final Predicate<Entry<String, String>> FILTER_ENTRIES_1 =
      new Predicate<Entry<String, String>>() {
        @Override
        public boolean apply(Entry<String, String> entry) {
          return !Helpers.mapEntry("banana", "toast").equals(entry);
        }
      };

  static final Predicate<Entry<String, String>> FILTER_ENTRIES_2 =
      new Predicate<Entry<String, String>>() {
        @Override
        public boolean apply(Entry<String, String> entry) {
          return !Helpers.mapEntry("eggplant", "spam").equals(entry);
        }
      };

  static final ImmutableMap<String, String> ENTRIES_TO_FILTER =
      ImmutableMap.of("banana", "toast", "eggplant", "spam");

  static final Predicate<Entry<String, String>> NOT_NULL_ENTRY =
      new Predicate<Entry<String, String>>() {
        @Override
        public boolean apply(Entry<String, String> entry) {
          return entry.getKey() != null && entry.getValue() != null;
        }
      };

  private static class NonNavigableSortedSet extends ForwardingSortedSet<String> {

    private final SortedSet<String> delegate = Sets.newTreeSet(Ordering.natural());

    @Override
    protected SortedSet<String> delegate() {
      return delegate;
    }
  }

  private static class NonNavigableSortedMap extends ForwardingSortedMap<String, String> {

    private final SortedMap<String, String> delegate = new SafeTreeMap<>(Ordering.natural());

    @Override
    protected SortedMap<String, String> delegate() {
      return delegate;
    }
  }

  private static String encode(String str) {
    return BaseEncoding.base64().encode(str.getBytes(Charsets.UTF_8));
  }

  private static final Function<String, String> DECODE_FUNCTION =
      new Function<String, String>() {
        @Override
        public String apply(String input) {
          return new String(BaseEncoding.base64().decode(input), Charsets.UTF_8);
        }
      };

  private static final EntryTransformer<String, String, String> DECODE_ENTRY_TRANSFORMER =
      new EntryTransformer<String, String, String>() {
        @Override
        public String transformEntry(String key, String value) {
          return DECODE_FUNCTION.apply(value);
        }
      };

  static TestSuite transformSuite() {
    TestSuite suite = new TestSuite("Maps.transform");
    suite.addTest(transformMapSuite());
    suite.addTest(transformSortedMapSuite());
    suite.addTest(transformNavigableMapSuite());
    return suite;
  }

  static TestSuite transformMapSuite() {
    TestSuite suite = new TestSuite("TransformMap");
    suite.addTest(
        MapTestSuiteBuilder.using(
                new TestStringMapGenerator() {
                  @Override
                  protected Map<String, String> create(Entry<String, String>[] entries) {
                    Map<String, String> map = Maps.newLinkedHashMap();
                    for (Entry<String, String> entry : entries) {
                      map.put(entry.getKey(), encode(entry.getValue()));
                    }
                    return Maps.transformValues(map, DECODE_FUNCTION);
                  }
                })
            .named("Maps.transformValues[Map, Function]")
            .withFeatures(
                CollectionSize.ANY,
                CollectionFeature.KNOWN_ORDER,
                MapFeature.ALLOWS_NULL_KEYS,
                MapFeature.ALLOWS_ANY_NULL_QUERIES,
                MapFeature.SUPPORTS_REMOVE,
                CollectionFeature.SUPPORTS_ITERATOR_REMOVE)
            .createTestSuite());
    suite.addTest(
        MapTestSuiteBuilder.using(
                new TestStringMapGenerator() {
                  @Override
                  protected Map<String, String> create(Entry<String, String>[] entries) {
                    Map<String, String> map = Maps.newLinkedHashMap();
                    for (Entry<String, String> entry : entries) {
                      map.put(entry.getKey(), encode(entry.getValue()));
                    }
                    return Maps.transformEntries(map, DECODE_ENTRY_TRANSFORMER);
                  }
                })
            .named("Maps.transformEntries[Map, EntryTransformer]")
            .withFeatures(
                CollectionSize.ANY,
                CollectionFeature.KNOWN_ORDER,
                MapFeature.ALLOWS_NULL_KEYS,
                MapFeature.ALLOWS_ANY_NULL_QUERIES,
                MapFeature.SUPPORTS_REMOVE,
                CollectionFeature.SUPPORTS_ITERATOR_REMOVE)
            .createTestSuite());
    return suite;
  }

  static TestSuite transformSortedMapSuite() {
    TestSuite suite = new TestSuite("TransformSortedMap");
    suite.addTest(
        SortedMapTestSuiteBuilder.using(
                new TestStringSortedMapGenerator() {
                  @Override
                  protected SortedMap<String, String> create(Entry<String, String>[] entries) {
                    SortedMap<String, String> map = new NonNavigableSortedMap();
                    for (Entry<String, String> entry : entries) {
                      map.put(entry.getKey(), encode(entry.getValue()));
                    }
                    return Maps.transformValues(map, DECODE_FUNCTION);
                  }
                })
            .named("Maps.transformValues[SortedMap, Function]")
            .withFeatures(
                CollectionSize.ANY,
                CollectionFeature.KNOWN_ORDER,
                MapFeature.SUPPORTS_REMOVE,
                CollectionFeature.SUPPORTS_ITERATOR_REMOVE)
            .createTestSuite());
    suite.addTest(
        SortedMapTestSuiteBuilder.using(
                new TestStringSortedMapGenerator() {
                  @Override
                  protected SortedMap<String, String> create(Entry<String, String>[] entries) {
                    SortedMap<String, String> map = new NonNavigableSortedMap();
                    for (Entry<String, String> entry : entries) {
                      map.put(entry.getKey(), encode(entry.getValue()));
                    }
                    return Maps.transformEntries(map, DECODE_ENTRY_TRANSFORMER);
                  }
                })
            .named("Maps.transformEntries[SortedMap, EntryTransformer]")
            .withFeatures(
                CollectionSize.ANY,
                CollectionFeature.KNOWN_ORDER,
                MapFeature.SUPPORTS_REMOVE,
                CollectionFeature.SUPPORTS_ITERATOR_REMOVE)
            .createTestSuite());
    return suite;
  }

  static TestSuite transformNavigableMapSuite() {
    TestSuite suite = new TestSuite("TransformNavigableMap");
    suite.addTest(
        NavigableMapTestSuiteBuilder.using(
                new TestStringSortedMapGenerator() {
                  @Override
                  protected NavigableMap<String, String> create(Entry<String, String>[] entries) {
                    NavigableMap<String, String> map = new SafeTreeMap<>();
                    for (Entry<String, String> entry : entries) {
                      map.put(entry.getKey(), encode(entry.getValue()));
                    }
                    return Maps.transformValues(map, DECODE_FUNCTION);
                  }
                })
            .named("Maps.transformValues[NavigableMap, Function]")
            .withFeatures(
                CollectionSize.ANY,
                CollectionFeature.KNOWN_ORDER,
                MapFeature.SUPPORTS_REMOVE,
                CollectionFeature.SUPPORTS_ITERATOR_REMOVE)
            .createTestSuite());
    suite.addTest(
        NavigableMapTestSuiteBuilder.using(
                new TestStringSortedMapGenerator() {
                  @Override
                  protected NavigableMap<String, String> create(Entry<String, String>[] entries) {
                    NavigableMap<String, String> map = new SafeTreeMap<>();
                    for (Entry<String, String> entry : entries) {
                      map.put(entry.getKey(), encode(entry.getValue()));
                    }
                    return Maps.transformEntries(map, DECODE_ENTRY_TRANSFORMER);
                  }
                })
            .named("Maps.transformEntries[NavigableMap, EntryTransformer]")
            .withFeatures(
                CollectionSize.ANY,
                CollectionFeature.KNOWN_ORDER,
                MapFeature.SUPPORTS_REMOVE,
                CollectionFeature.SUPPORTS_ITERATOR_REMOVE)
            .createTestSuite());
    return suite;
  }
}
