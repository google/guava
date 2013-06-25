/*
 * Copyright (C) 2012 The Guava Authors
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

import static org.truth0.Truth.ASSERT;

import com.google.common.annotations.GwtCompatible;
import com.google.common.collect.testing.AnEnum;
import com.google.common.collect.testing.Helpers;
import com.google.common.collect.testing.TestEnumMapGenerator;

import junit.framework.TestCase;

import java.util.Map;
import java.util.Map.Entry;

/**
 * Tests for {@code ImmutableEnumMap}.
 *
 * @author Louis Wasserman
 */
@GwtCompatible(emulated = true)
public class ImmutableEnumMapTest extends TestCase {
  public static class ImmutableEnumMapGenerator extends TestEnumMapGenerator {
    @Override
    protected Map<AnEnum, String> create(Entry<AnEnum, String>[] entries) {
      Map<AnEnum, String> map = Maps.newHashMap();
      for (Entry<AnEnum, String> entry : entries) {
        map.put(entry.getKey(), entry.getValue());
      }
      return Maps.immutableEnumMap(map);
    }
  }

  public void testEmptyImmutableEnumMap() {
    ImmutableMap<AnEnum, String> map = Maps.immutableEnumMap(ImmutableMap.<AnEnum, String>of());
    assertEquals(ImmutableMap.of(), map);
  }

  public void testImmutableEnumMapOrdering() {
    ImmutableMap<AnEnum, String> map = Maps.immutableEnumMap(
        ImmutableMap.of(AnEnum.C, "c", AnEnum.A, "a", AnEnum.E, "e"));

    ASSERT.that(map.entrySet()).has().exactly(
        Helpers.mapEntry(AnEnum.A, "a"),
        Helpers.mapEntry(AnEnum.C, "c"),
        Helpers.mapEntry(AnEnum.E, "e")).inOrder();
  }
}

