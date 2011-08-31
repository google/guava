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
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.ImmutableSortedMap.Builder;
import com.google.common.collect.testing.SampleElements;
import com.google.common.collect.testing.TestCollectionGenerator;
import com.google.common.collect.testing.TestMapEntrySetGenerator;
import com.google.common.collect.testing.TestStringSetGenerator;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

/**
 * Generators of sorted maps and derived collections.
 *
 * @author Kevin Bourrillion
 * @author Jesse Wilson
 * @author Jared Levy
 * @author Hayward Chan
 */
@GwtCompatible
public class SortedMapGenerators {

  public static final Comparator<Entry<String, String>> ENTRY_COMPARATOR
      = new Comparator<Entry<String, String>>() {
          @Override
          public int compare(
              Entry<String, String> o1, Entry<String, String> o2) {
            return o1.getKey().compareTo(o2.getKey());
          }
  };

  public static class ImmutableSortedMapKeySetGenerator
      extends TestStringSetGenerator {

    @Override protected Set<String> create(String[] elements) {
      Builder<String, Integer> builder = ImmutableSortedMap.naturalOrder();
      for (String key : elements) {
        builder.put(key, 4);
      }
      return builder.build().keySet();
    }
    @Override public List<String> order(List<String> insertionOrder) {
      Collections.sort(insertionOrder);
      return insertionOrder;
    }
  }

  public static class ImmutableSortedMapValuesGenerator
      implements TestCollectionGenerator<String> {

    @Override
    public SampleElements<String> samples() {
      return new SampleElements.Strings();
    }

    @Override
    public Collection<String> create(Object... elements) {
      Builder<Integer, String> builder = ImmutableSortedMap.naturalOrder();
      for (int i = 0; i < elements.length; i++) {
        builder.put(i, toStringOrNull(elements[i]));
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

  public static class ImmutableSortedMapSubMapEntryGenerator
      extends TestMapEntrySetGenerator<String, String> {

    public ImmutableSortedMapSubMapEntryGenerator() {
      super(new SampleElements.Strings(), new SampleElements.Strings());
    }

    @Override public Set<Entry<String, String>> createFromEntries(
        Entry<String, String>[] entries) {
      Builder<String, String> builder = ImmutableSortedMap.naturalOrder();
      builder.put(SampleElements.Strings.BEFORE_FIRST, "begin");
      builder.put(SampleElements.Strings.AFTER_LAST, "end");
      for (Entry<String, String> entry : entries) {
        checkNotNull(entry);
        builder.put(entry.getKey(), entry.getValue());
      }
      return builder.build().subMap(SampleElements.Strings.MIN_ELEMENT,
          SampleElements.Strings.AFTER_LAST).entrySet();
    }
    @Override public List<Entry<String, String>> order(
        List<Entry<String, String>> insertionOrder) {
      Collections.sort(insertionOrder, ENTRY_COMPARATOR);
      return insertionOrder;
    }
  }

  public static class ImmutableSortedMapHeadMapKeySetGenerator
      extends TestStringSetGenerator {
    @Override protected Set<String> create(String[] elements) {
      Builder<String, Integer> builder = ImmutableSortedMap.naturalOrder();
      builder.put(SampleElements.Strings.AFTER_LAST, -1);
      for (String key : elements) {
        builder.put(key, 4);
      }
      return builder.build().headMap(
          SampleElements.Strings.AFTER_LAST).keySet();
    }
    @Override public List<String> order(List<String> insertionOrder) {
      Collections.sort(insertionOrder);
      return insertionOrder;
    }
  }

  public static class ImmutableSortedMapTailMapValuesGenerator
      implements TestCollectionGenerator<String> {

    @Override
    public SampleElements<String> samples() {
      return new SampleElements.Strings();
    }

    @Override
    public Collection<String> create(Object... elements) {
      Builder<Integer, String> builder = ImmutableSortedMap.naturalOrder();
      builder.put(-1, "begin");
      for (int i = 0; i < elements.length; i++) {
        builder.put(i, toStringOrNull(elements[i]));
      }
      return builder.build().tailMap(0).values();
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

  public static class ImmutableSortedMapEntrySetGenerator
      extends TestMapEntrySetGenerator<String, String> {

    public ImmutableSortedMapEntrySetGenerator() {
      super(new SampleElements.Strings(), new SampleElements.Strings());
    }

    @Override public Set<Entry<String, String>> createFromEntries(
        Entry<String, String>[] entries) {
      Builder<String, String> builder = ImmutableSortedMap.naturalOrder();
      for (Entry<String, String> entry : entries) {
        checkNotNull(entry);
        builder.put(entry.getKey(), entry.getValue());
      }
      return builder.build().entrySet();
    }

    @Override public List<Entry<String, String>> order(
        List<Entry<String, String>> insertionOrder) {
      Collections.sort(insertionOrder, ENTRY_COMPARATOR);
      return insertionOrder;
    }
  }

  private static String toStringOrNull(Object o) {
    return (o == null) ? null : o.toString();
  }
}
