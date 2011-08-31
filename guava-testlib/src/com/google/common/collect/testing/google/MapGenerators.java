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

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.annotations.GwtCompatible;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.google.common.collect.testing.SampleElements;
import com.google.common.collect.testing.TestCollectionGenerator;
import com.google.common.collect.testing.TestListGenerator;
import com.google.common.collect.testing.TestMapEntrySetGenerator;
import com.google.common.collect.testing.TestStringSetGenerator;
import com.google.common.collect.testing.TestUnhashableCollectionGenerator;
import com.google.common.collect.testing.UnhashableObject;

import java.util.Collection;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

/**
 * Generators of different types of map and related collections, such as
 * keys, entries and values.
 *
 * @author Hayward Chan
 */
@GwtCompatible
public class MapGenerators {

  public static class ImmutableMapKeySetGenerator
      extends TestStringSetGenerator {
    @Override protected Set<String> create(String[] elements) {
      Builder<String, Integer> builder = ImmutableMap.builder();
      for (String key : elements) {
        builder.put(key, 4);
      }
      return builder.build().keySet();
    }
  }

  public static class ImmutableMapValuesGenerator
      implements TestCollectionGenerator<String> {

    @Override
    public SampleElements<String> samples() {
      return new SampleElements.Strings();
    }

    @Override
    public Collection<String> create(Object... elements) {
      Builder<Object, String> builder = ImmutableMap.builder();
      for (Object key : elements) {
        builder.put(key, String.valueOf(key));
      }
      return builder.build().values();
    }

    @Override
    public String[] createArray(int length) {
      return new String[length];
    }

    @Override
    public List<String> order(List<String> insertionOrder) {
      return insertionOrder;
    }
  }

  public static class ImmutableMapUnhashableValuesGenerator
      extends TestUnhashableCollectionGenerator<Collection<UnhashableObject>> {

    @Override public Collection<UnhashableObject> create(
        UnhashableObject[] elements) {
      Builder<Integer, UnhashableObject> builder = ImmutableMap.builder();
      int key = 1;
      for (UnhashableObject value : elements) {
        builder.put(key++, value);
      }
      return builder.build().values();
    }
  }

  public static class ImmutableMapEntrySetGenerator
      extends TestMapEntrySetGenerator<String, String> {

    public ImmutableMapEntrySetGenerator() {
      super(new SampleElements.Strings(), new SampleElements.Strings());
    }

    @Override public Set<Entry<String, String>> createFromEntries(
        Entry<String, String>[] entries) {
      Builder<String, String> builder = ImmutableMap.builder();
      for (Entry<String, String> entry : entries) {
        // This null-check forces NPE to be thrown for tests with null
        // elements.  Those tests aren't useful in testing entry sets
        // because entry sets never have null elements.
        checkNotNull(entry);
        builder.put(entry.getKey(), entry.getValue());
      }
      return builder.build().entrySet();
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
      Builder<Integer, String> builder = ImmutableMap.builder();
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
