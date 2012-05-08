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

import com.google.common.annotations.GwtCompatible;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.testing.SampleElements;
import com.google.common.collect.testing.TestListGenerator;
import com.google.common.collect.testing.TestStringMapGenerator;
import com.google.common.collect.testing.TestUnhashableCollectionGenerator;
import com.google.common.collect.testing.UnhashableObject;

import java.util.Collection;
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

  public static class ImmutableMapValueListGenerator
      implements TestListGenerator<String> {
    @Override
    public SampleElements<String> samples() {
      return new SampleElements.Strings();
    }

    @Override
    public List<String> create(Object... elements) {
      ImmutableMap.Builder<Integer, String> builder = ImmutableMap.builder();
      for (int i = 0; i < elements.length; i++) {
        builder.put(i, toStringOrNull(elements[i]));
      }
      return builder.build().values().asList();
    }

    @Override
    public String[] createArray(int length) {
      return new String[length];
    }

    @Override
    public Iterable<String> order(List<String> insertionOrder) {
      return insertionOrder;
    }
  }

  private static String toStringOrNull(Object o) {
    return (o == null) ? null : o.toString();
  }
}
