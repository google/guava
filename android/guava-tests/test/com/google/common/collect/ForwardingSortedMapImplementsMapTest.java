/*
 * Copyright (C) 2007 The Guava Authors
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

import com.google.common.annotations.GwtCompatible;
import com.google.common.collect.testing.MapInterfaceTest;
import com.google.common.collect.testing.SortedMapInterfaceTest;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Tests for {@link ForwardingSortedMap} using {@link MapInterfaceTest}.
 *
 * @author George van den Driessche
 */
@GwtCompatible
public class ForwardingSortedMapImplementsMapTest extends SortedMapInterfaceTest<String, Integer> {

  private static class SimpleForwardingSortedMap<K, V> extends ForwardingSortedMap<K, V> {
    final SortedMap<K, V> delegate;

    SimpleForwardingSortedMap(SortedMap<K, V> delegate) {
      this.delegate = delegate;
    }

    @Override
    protected SortedMap<K, V> delegate() {
      return delegate;
    }
  }

  public ForwardingSortedMapImplementsMapTest() {
    super(true, true, true, true, true);
  }

  @Override
  protected SortedMap<String, Integer> makeEmptyMap() {
    return new SimpleForwardingSortedMap<>(
        new TreeMap<String, Integer>(Ordering.natural().nullsFirst()));
  }

  @Override
  protected SortedMap<String, Integer> makePopulatedMap() {
    final SortedMap<String, Integer> sortedMap = makeEmptyMap();
    sortedMap.put("one", 1);
    sortedMap.put("two", 2);
    sortedMap.put("three", 3);
    return sortedMap;
  }

  @Override
  protected String getKeyNotInPopulatedMap() throws UnsupportedOperationException {
    return "minus one";
  }

  @Override
  protected Integer getValueNotInPopulatedMap() throws UnsupportedOperationException {
    return -1;
  }

  @Override
  public void testContainsKey() {
    try {
      super.testContainsKey();
    } catch (ClassCastException tolerated) {
    }
  }

  @Override
  public void testEntrySetContainsEntryIncompatibleKey() {
    try {
      super.testEntrySetContainsEntryIncompatibleKey();
    } catch (ClassCastException tolerated) {
    }
  }

  @Override
  public void testEntrySetRemoveAllNullFromEmpty() {
    try {
      super.testEntrySetRemoveAllNullFromEmpty();
    } catch (RuntimeException tolerated) {
      // GWT's TreeMap.entrySet().removeAll(null) doesn't throws NPE.
    }
  }

  @Override
  public void testEntrySetRetainAllNullFromEmpty() {
    try {
      super.testEntrySetRetainAllNullFromEmpty();
    } catch (RuntimeException tolerated) {
      // GWT's TreeMap.entrySet().retainAll(null) doesn't throws NPE.
    }
  }

  @Override
  public void testKeySetRemoveAllNullFromEmpty() {
    try {
      super.testKeySetRemoveAllNullFromEmpty();
    } catch (RuntimeException tolerated) {
      // GWT's TreeMap.keySet().removeAll(null) doesn't throws NPE.
    }
  }

  @Override
  public void testKeySetRetainAllNullFromEmpty() {
    try {
      super.testKeySetRetainAllNullFromEmpty();
    } catch (RuntimeException tolerated) {
      // GWT's TreeMap.keySet().retainAll(null) doesn't throws NPE.
    }
  }

  @Override
  public void testValuesRemoveAllNullFromEmpty() {
    try {
      super.testValuesRemoveAllNullFromEmpty();
    } catch (RuntimeException tolerated) {
      // GWT's TreeMap.values().removeAll(null) doesn't throws NPE.
    }
  }

  @Override
  public void testValuesRetainAllNullFromEmpty() {
    try {
      super.testValuesRemoveAllNullFromEmpty();
    } catch (RuntimeException tolerated) {
      // GWT's TreeMap.values().retainAll(null) doesn't throws NPE.
    }
  }
}
