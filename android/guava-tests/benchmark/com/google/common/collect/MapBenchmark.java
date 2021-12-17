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

import static com.google.common.collect.Lists.newArrayList;

import com.google.caliper.BeforeExperiment;
import com.google.caliper.Benchmark;
import com.google.caliper.Param;
import com.google.common.collect.CollectionBenchmarkSampleData.Element;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;

/**
 * A microbenchmark that tests the performance of get() and iteration on various map
 * implementations. Forked from {@link SetContainsBenchmark}.
 *
 * @author Nicholaus Shupe
 */
public class MapBenchmark {
  @Param({"Hash", "LinkedHM", "MapMaker1", "Immutable"})
  private Impl impl;

  public enum Impl {
    Hash {
      @Override
      Map<Element, Element> create(Collection<Element> keys) {
        Map<Element, Element> map = Maps.newHashMap();
        for (Element element : keys) {
          map.put(element, element);
        }
        return map;
      }
    },
    LinkedHM {
      @Override
      Map<Element, Element> create(Collection<Element> keys) {
        Map<Element, Element> map = Maps.newLinkedHashMap();
        for (Element element : keys) {
          map.put(element, element);
        }
        return map;
      }
    },
    UnmodHM {
      @Override
      Map<Element, Element> create(Collection<Element> keys) {
        return Collections.unmodifiableMap(Hash.create(keys));
      }
    },
    SyncHM {
      @Override
      Map<Element, Element> create(Collection<Element> keys) {
        return Collections.synchronizedMap(Hash.create(keys));
      }
    },
    Tree {
      @Override
      Map<Element, Element> create(Collection<Element> keys) {
        Map<Element, Element> map = Maps.newTreeMap();
        for (Element element : keys) {
          map.put(element, element);
        }
        return map;
      }
    },
    SkipList {
      @Override
      Map<Element, Element> create(Collection<Element> keys) {
        Map<Element, Element> map = new ConcurrentSkipListMap<>();
        for (Element element : keys) {
          map.put(element, element);
        }
        return map;
      }
    },
    ConcurrentHM1 {
      @Override
      Map<Element, Element> create(Collection<Element> keys) {
        Map<Element, Element> map = new ConcurrentHashMap<>(keys.size(), 0.75f, 1);
        for (Element element : keys) {
          map.put(element, element);
        }
        return map;
      }
    },
    ConcurrentHM16 {
      @Override
      Map<Element, Element> create(Collection<Element> keys) {
        Map<Element, Element> map = new ConcurrentHashMap<>(keys.size(), 0.75f, 16);
        for (Element element : keys) {
          map.put(element, element);
        }
        return map;
      }
    },
    MapMaker1 {
      @Override
      Map<Element, Element> create(Collection<Element> keys) {
        Map<Element, Element> map = new MapMaker().concurrencyLevel(1).makeMap();
        for (Element element : keys) {
          map.put(element, element);
        }
        return map;
      }
    },
    MapMaker16 {
      @Override
      Map<Element, Element> create(Collection<Element> keys) {
        Map<Element, Element> map = new MapMaker().concurrencyLevel(16).makeMap();
        for (Element element : keys) {
          map.put(element, element);
        }
        return map;
      }
    },
    Immutable {
      @Override
      Map<Element, Element> create(Collection<Element> keys) {
        ImmutableMap.Builder<Element, Element> builder = ImmutableMap.builder();
        for (Element element : keys) {
          builder.put(element, element);
        }
        return builder.build();
      }
    },
    ImmutableSorted {
      @Override
      Map<Element, Element> create(Collection<Element> keys) {
        ImmutableSortedMap.Builder<Element, Element> builder = ImmutableSortedMap.naturalOrder();
        for (Element element : keys) {
          builder.put(element, element);
        }
        return builder.build();
      }
    };

    abstract Map<Element, Element> create(Collection<Element> contents);
  }

  @Param({"5", "50", "500", "5000", "50000"})
  private int size;

  // TODO: look at exact (==) hits vs. equals() hits?
  @Param("0.9")
  private double hitRate;

  @Param("true")
  private boolean isUserTypeFast;

  // "" means no fixed seed
  @Param("")
  private SpecialRandom random;

  @Param("false")
  private boolean sortedData;

  // the following must be set during setUp
  private Element[] queries;
  private Map<Element, Element> mapToTest;

  private Collection<Element> values;

  @BeforeExperiment
  void setUp() {
    CollectionBenchmarkSampleData sampleData =
        new CollectionBenchmarkSampleData(isUserTypeFast, random, hitRate, size);

    if (sortedData) {
      List<Element> valueList = newArrayList(sampleData.getValuesInSet());
      Collections.sort(valueList);
      values = valueList;
    } else {
      values = sampleData.getValuesInSet();
    }
    this.mapToTest = impl.create(values);
    this.queries = sampleData.getQueries();
  }

  @Benchmark
  boolean get(int reps) {
    // Paranoia: acting on hearsay that accessing fields might be slow
    // Should write a benchmark to test that!
    Map<Element, Element> map = mapToTest;
    Element[] queries = this.queries;

    // Allows us to use & instead of %, acting on hearsay that division
    // operators (/%) are disproportionately expensive; should test this too!
    int mask = queries.length - 1;

    boolean dummy = false;
    for (int i = 0; i < reps; i++) {
      dummy ^= map.get(queries[i & mask]) != null;
    }
    return dummy;
  }

  @Benchmark
  int createAndPopulate(int reps) {
    int dummy = 0;
    for (int i = 0; i < reps; i++) {
      dummy += impl.create(values).size();
    }
    return dummy;
  }

  @Benchmark
  boolean createPopulateAndRemove(int reps) {
    boolean dummy = false;
    for (int i = 1; i < reps; i++) {
      Map<Element, Element> map = impl.create(values);
      for (Element value : values) {
        dummy |= map.remove(value) == null;
      }
    }
    return dummy;
  }

  @Benchmark
  boolean iterateWithEntrySet(int reps) {
    Map<Element, Element> map = mapToTest;

    boolean dummy = false;
    for (int i = 0; i < reps; i++) {
      for (Map.Entry<Element, Element> entry : map.entrySet()) {
        dummy ^= entry.getKey() != entry.getValue();
      }
    }
    return dummy;
  }

  @Benchmark
  boolean iterateWithKeySetAndGet(int reps) {
    Map<Element, Element> map = mapToTest;

    boolean dummy = false;
    for (int i = 0; i < reps; i++) {
      for (Element key : map.keySet()) {
        Element value = map.get(key);
        dummy ^= key != value;
      }
    }
    return dummy;
  }

  @Benchmark
  boolean iterateValuesAndGet(int reps) {
    Map<Element, Element> map = mapToTest;

    boolean dummy = false;
    for (int i = 0; i < reps; i++) {
      for (Element key : map.values()) {
        // This normally wouldn't make sense, but because our keys are our values it kind of does
        Element value = map.get(key);
        dummy ^= key != value;
      }
    }
    return dummy;
  }
}
