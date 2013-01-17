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

package com.google.common.collect.testing.google;

import static com.google.common.collect.testing.Helpers.mapEntry;

import com.google.common.annotations.GwtCompatible;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.collect.Ordering;
import com.google.common.collect.testing.AnEnum;
import com.google.common.collect.testing.SampleElements;
import com.google.common.collect.testing.TestEnumMapGenerator;
import com.google.common.collect.testing.TestListGenerator;
import com.google.common.collect.testing.TestStringListGenerator;
import com.google.common.collect.testing.TestStringMapGenerator;
import com.google.common.collect.testing.TestUnhashableCollectionGenerator;
import com.google.common.collect.testing.UnhashableObject;

import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Generators of different types of map and related collections, such as
 * keys, entries and values.
 *
 * @author Hayward Chan
 */
@GwtCompatible
public class MapGenerators {
  public static class ImmutableMapGenerator
      extends TestStringMapGenerator {
    @Override protected Map<String, String> create(Entry<String, String>[] entries) {
      ImmutableMap.Builder<String, String> builder = ImmutableMap.builder();
      for (Entry<String, String> entry : entries) {
        builder.put(entry.getKey(), entry.getValue());
      }
      return builder.build();
    }
  }
  
  public static class ImmutableMapCopyOfGenerator
      extends TestStringMapGenerator {
    @Override protected Map<String, String> create(Entry<String, String>[] entries) {
      Map<String, String> builder = Maps.newLinkedHashMap();
      for (Entry<String, String> entry : entries) {
        builder.put(entry.getKey(), entry.getValue());
      }
      return ImmutableMap.copyOf(builder);
    }
  }

  public static class ImmutableMapUnhashableValuesGenerator
      extends TestUnhashableCollectionGenerator<Collection<UnhashableObject>> {

    @Override public Collection<UnhashableObject> create(
        UnhashableObject[] elements) {
      ImmutableMap.Builder<Integer, UnhashableObject> builder = ImmutableMap.builder();
      int key = 1;
      for (UnhashableObject value : elements) {
        builder.put(key++, value);
      }
      return builder.build().values();
    }
  }

  public static class ImmutableMapKeyListGenerator extends TestStringListGenerator {
    @Override
    public List<String> create(String[] elements) {
      ImmutableMap.Builder<String, Integer> builder = ImmutableMap.builder();
      for (int i = 0; i < elements.length; i++) {
        builder.put(elements[i], i);
      }
      return builder.build().keySet().asList();
    }
  }

  public static class ImmutableMapValueListGenerator extends TestStringListGenerator {
    @Override
    public List<String> create(String[] elements) {
      ImmutableMap.Builder<Integer, String> builder = ImmutableMap.builder();
      for (int i = 0; i < elements.length; i++) {
        builder.put(i, elements[i]);
      }
      return builder.build().values().asList();
    }
  }

  public static class ImmutableMapEntryListGenerator
      implements TestListGenerator<Entry<String, Integer>> {

    @Override
    public SampleElements<Entry<String, Integer>> samples() {
      return new SampleElements<Entry<String, Integer>>(
          mapEntry("foo", 5),
          mapEntry("bar", 3),
          mapEntry("baz", 17),
          mapEntry("quux", 1),
          mapEntry("toaster", -2));
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
    public List<Entry<String, Integer>> create(Object... elements) {
      ImmutableMap.Builder<String, Integer> builder = ImmutableMap.builder();
      for (Object o : elements) {
        @SuppressWarnings("unchecked")
        Entry<String, Integer> entry = (Entry<String, Integer>) o;
        builder.put(entry);
      }
      return builder.build().entrySet().asList();
    }
  }

  public static class ImmutableEnumMapGenerator extends TestEnumMapGenerator {
    @Override
    protected Map<AnEnum, String> create(Entry<AnEnum, String>[] entries) {
      Map<AnEnum, String> map = Maps.newHashMap();
      for (Entry<AnEnum, String> entry : entries) {
        // checkArgument(!map.containsKey(entry.getKey()));
        map.put(entry.getKey(), entry.getValue());
      }
      return Maps.immutableEnumMap(map);
    }
  }

  public static class ImmutableMapCopyOfEnumMapGenerator extends TestEnumMapGenerator {
    @Override
    protected Map<AnEnum, String> create(Entry<AnEnum, String>[] entries) {
      EnumMap<AnEnum, String> map = new EnumMap<AnEnum, String>(AnEnum.class);
      for (Entry<AnEnum, String> entry : entries) {
        map.put(entry.getKey(), entry.getValue());
      }
      return ImmutableMap.copyOf(map);
    }

    @Override
    public Iterable<Entry<AnEnum, String>> order(List<Entry<AnEnum, String>> insertionOrder) {
      return new Ordering<Entry<AnEnum, String>>() {

        @Override
        public int compare(Entry<AnEnum, String> left, Entry<AnEnum, String> right) {
          return left.getKey().compareTo(right.getKey());
        }

      }.sortedCopy(insertionOrder);
    }
  }

  private static String toStringOrNull(Object o) {
    return (o == null) ? null : o.toString();
  }
}
