/*
 * Copyright (C) 2017 The Guava Authors
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

import static com.google.common.base.Functions.toStringFunction;
import static com.google.common.collect.Maps.uniqueIndex;

import com.google.caliper.BeforeExperiment;
import com.google.caliper.Benchmark;
import com.google.caliper.Param;
import com.google.caliper.api.Footprint;
import com.google.common.collect.BenchmarkHelpers.BiMapImpl;
import com.google.common.collect.BenchmarkHelpers.MapImpl;
import com.google.common.collect.BenchmarkHelpers.MapsImplEnum;
import com.google.common.collect.BenchmarkHelpers.SortedMapImpl;
import com.google.common.collect.CollectionBenchmarkSampleData.Element;
import java.util.Arrays;
import java.util.Map;

/** Benchmarks for memory consumption of map implementations. */
public class MapsMemoryBenchmark {
  static final Map<String, MapsImplEnum> mapEnums =
      uniqueIndex(
          Iterables.<MapsImplEnum>concat(
              Arrays.asList(MapImpl.values()),
              Arrays.asList(SortedMapImpl.values()),
              Arrays.asList(BiMapImpl.values())),
          toStringFunction());

  @Param({
    "HashMapImpl",
    "LinkedHashMapImpl",
    "ConcurrentHashMapImpl",
    "CompactHashMapImpl",
    "CompactLinkedHashMapImpl",
    "ImmutableMapImpl",
    "TreeMapImpl",
    "ImmutableSortedMapImpl",
    "MapMakerWeakKeysWeakValues",
    "MapMakerWeakKeysStrongValues",
    "MapMakerStrongKeysWeakValues",
    "MapMakerStrongKeysStrongValues",
    "HashBiMapImpl",
    "ImmutableBiMapImpl"
  })
  String implName;

  MapsImplEnum mapsImpl;

  /**
   * A map of contents pre-created before experiment starts to only measure map creation cost. The
   * implementation for the creation of contents is independent and could be different from that of
   * the map under test.
   */
  Map<Element, Element> contents;

  /** Map pre-created before experiment starts to only measure iteration cost during experiment. */
  Map<Element, Element> map;

  CollectionBenchmarkSampleData elems;

  @Param({"0", "1", "100", "10000"})
  int elements;

  @BeforeExperiment
  public void prepareContents() throws Exception {
    mapsImpl = mapEnums.get(implName);
    elems = new CollectionBenchmarkSampleData(elements);
    contents = Maps.newHashMap();
    for (Element key : elems.getValuesInSet()) {
      contents.put(key, key);
    }
    map = mapsImpl.create(contents);
  }

  @Benchmark
  @Footprint(exclude = Element.class)
  public Map<Element, Element> create() throws Exception {
    return mapsImpl.create(contents);
  }

  @Benchmark
  public int iterate() {
    long retVal = 0;
    for (Object entry : map.entrySet()) {
      retVal += entry.hashCode();
    }
    return (int) retVal;
  }

  @Benchmark
  public int keyIterate() {
    long retVal = 0;
    for (Object key : map.keySet()) {
      retVal += key.hashCode();
    }
    return (int) retVal;
  }
}
