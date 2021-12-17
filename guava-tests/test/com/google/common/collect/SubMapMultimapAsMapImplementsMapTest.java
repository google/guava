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

package com.google.common.collect;

import com.google.common.annotations.GwtCompatible;
import com.google.common.collect.testing.MapInterfaceTest;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

/**
 * Test {@code TreeMultimap.asMap().subMap()} with {@link MapInterfaceTest}.
 *
 * @author Jared Levy
 */
@GwtCompatible
public class SubMapMultimapAsMapImplementsMapTest extends AbstractMultimapAsMapImplementsMapTest {

  public SubMapMultimapAsMapImplementsMapTest() {
    super(true, true, true);
  }

  private TreeMultimap<String, Integer> createMultimap() {
    TreeMultimap<String, Integer> multimap =
        TreeMultimap.create(
            Ordering.<String>natural().nullsFirst(), Ordering.<Integer>natural().nullsFirst());
    multimap.put("a", -1);
    multimap.put("a", -3);
    multimap.put("z", -2);
    return multimap;
  }

  @Override
  protected Map<String, Collection<Integer>> makeEmptyMap() {
    return createMultimap().asMap().subMap("e", "p");
  }

  @Override
  protected Map<String, Collection<Integer>> makePopulatedMap() {
    TreeMultimap<String, Integer> multimap = createMultimap();
    multimap.put("f", 1);
    multimap.put("f", 2);
    multimap.put("g", 3);
    multimap.put("h", 4);
    return multimap.asMap().subMap("e", "p");
  }

  @Override
  protected String getKeyNotInPopulatedMap() {
    return "a";
  }

  @Override
  protected Collection<Integer> getValueNotInPopulatedMap() {
    return Collections.singleton(-2);
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
