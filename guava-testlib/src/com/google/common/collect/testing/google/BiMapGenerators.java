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

package com.google.common.collect.testing.google;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.annotations.GwtCompatible;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.Maps;
import com.google.common.collect.testing.SampleElements;
import com.google.common.collect.testing.TestMapEntrySetGenerator;
import com.google.common.collect.testing.TestStringSetGenerator;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * Generators of various {@link com.google.common.collect.BiMap}s and derived
 * collections.
 *
 * @author Jared Levy
 * @author Hayward Chan
 */
@GwtCompatible
public class BiMapGenerators {

  public static class ImmutableBiMapKeySetGenerator
      extends TestStringSetGenerator {
    @Override protected Set<String> create(String[] elements) {
      Map<String, Integer> map = Maps.newLinkedHashMap();
      for (int i = 0; i < elements.length; i++) {
        map.put(elements[i], i);
      }
      return ImmutableBiMap.copyOf(map).keySet();
    }
  }

  public static class ImmutableBiMapValuesGenerator
      extends TestStringSetGenerator {
    @Override protected Set<String> create(String[] elements) {
      Map<Integer, String> map = Maps.newLinkedHashMap();
      for (int i = 0; i < elements.length; i++) {
        map.put(i, elements[i]);
      }
      return ImmutableBiMap.copyOf(map).values();
    }
  }

  public static class ImmutableBiMapInverseEntrySetGenerator
      extends TestMapEntrySetGenerator<String, String> {

    public ImmutableBiMapInverseEntrySetGenerator() {
      super(new SampleElements.Strings(), new SampleElements.Strings());
    }
    @Override public Set<Entry<String, String>> createFromEntries(
        Entry<String, String>[] entries) {
      Map<String, String> map = Maps.newLinkedHashMap();
      for (Entry<String, String> entry : entries) {
        checkNotNull(entry);
        map.put(entry.getValue(), entry.getKey());
      }
      return ImmutableBiMap.copyOf(map).inverse().entrySet();
    }
  }

  public static class ImmutableBiMapInverseKeySetGenerator
      extends TestStringSetGenerator {
    @Override protected Set<String> create(String[] elements) {
      Map<Integer, String> map = Maps.newLinkedHashMap();
      for (int i = 0; i < elements.length; i++) {
        map.put(i, elements[i]);
      }
      return ImmutableBiMap.copyOf(map).inverse().keySet();
    }
  }

  public static class ImmutableBiMapInverseValuesGenerator
      extends TestStringSetGenerator {
    @Override protected Set<String> create(String[] elements) {
      Map<String, Integer> map = Maps.newLinkedHashMap();
      for (int i = 0; i < elements.length; i++) {
        map.put(elements[i], i);
      }
      return ImmutableBiMap.copyOf(map).inverse().values();
    }
  }

  public static class ImmutableBiMapEntrySetGenerator
      extends TestMapEntrySetGenerator<String, String> {

    public ImmutableBiMapEntrySetGenerator() {
      super(new SampleElements.Strings(), new SampleElements.Strings());
    }
    @Override public Set<Entry<String, String>> createFromEntries(
        Entry<String, String>[] entries) {
      Map<String, String> map = Maps.newLinkedHashMap();
      for (Entry<String, String> entry : entries) {
        checkNotNull(entry);
        map.put(entry.getKey(), entry.getValue());
      }
      return ImmutableBiMap.copyOf(map).entrySet();
    }
  }
}
