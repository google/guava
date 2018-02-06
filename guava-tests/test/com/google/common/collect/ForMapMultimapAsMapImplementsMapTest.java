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

package com.google.common.collect;

import com.google.common.annotations.GwtCompatible;
import com.google.common.collect.testing.MapInterfaceTest;
import java.util.Collection;
import java.util.Map;

/**
 * Test {@link Multimap#asMap()} for a {@link Multimaps#forMap} multimap with {@link
 * MapInterfaceTest}.
 *
 * @author Jared Levy
 */
@GwtCompatible
public class ForMapMultimapAsMapImplementsMapTest extends AbstractMultimapAsMapImplementsMapTest {

  public ForMapMultimapAsMapImplementsMapTest() {
    super(true, true, true);
  }

  @Override
  protected Map<String, Collection<Integer>> makeEmptyMap() {
    Map<String, Integer> map = Maps.newHashMap();
    return Multimaps.forMap(map).asMap();
  }

  @Override
  protected Map<String, Collection<Integer>> makePopulatedMap() {
    Map<String, Integer> map = Maps.newHashMap();
    map.put("foo", 1);
    map.put("bar", 2);
    map.put("cow", 3);
    return Multimaps.forMap(map).asMap();
  }

  @Override
  public void testEntrySetRemoveAllNullFromEmpty() {
    try {
      super.testEntrySetRemoveAllNullFromEmpty();
    } catch (RuntimeException tolerated) {
      // GWT's HashMap.entrySet().removeAll(null) doesn't throws NPE.
    }
  }

  @Override
  public void testEntrySetRetainAllNullFromEmpty() {
    try {
      super.testEntrySetRetainAllNullFromEmpty();
    } catch (RuntimeException tolerated) {
      // GWT's HashMap.entrySet().retainAll(null) doesn't throws NPE.
    }
  }

  @Override
  public void testKeySetRemoveAllNullFromEmpty() {
    try {
      super.testKeySetRemoveAllNullFromEmpty();
    } catch (RuntimeException tolerated) {
      // GWT's HashMap.keySet().removeAll(null) doesn't throws NPE.
    }
  }

  @Override
  public void testKeySetRetainAllNullFromEmpty() {
    try {
      super.testKeySetRetainAllNullFromEmpty();
    } catch (RuntimeException tolerated) {
      // GWT's HashMap.keySet().retainAll(null) doesn't throws NPE.
    }
  }

  @Override
  public void testValuesRemoveAllNullFromEmpty() {
    try {
      super.testValuesRemoveAllNullFromEmpty();
    } catch (RuntimeException tolerated) {
      // GWT's HashMap.values().removeAll(null) doesn't throws NPE.
    }
  }

  @Override
  public void testValuesRetainAllNullFromEmpty() {
    try {
      super.testValuesRemoveAllNullFromEmpty();
    } catch (RuntimeException tolerated) {
      // GWT's HashMap.values().retainAll(null) doesn't throws NPE.
    }
  }
}
