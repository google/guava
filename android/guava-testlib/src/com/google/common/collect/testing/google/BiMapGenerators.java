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

import com.google.common.annotations.GwtCompatible;
import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.Maps;
import java.util.Arrays;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Generators of various {@link com.google.common.collect.BiMap}s and derived collections.
 *
 * @author Jared Levy
 * @author Hayward Chan
 */
@GwtCompatible
public class BiMapGenerators {
  public static class ImmutableBiMapGenerator extends TestStringBiMapGenerator {
    @Override
    protected BiMap<String, String> create(Entry<String, String>[] entries) {
      ImmutableBiMap.Builder<String, String> builder = ImmutableBiMap.builder();
      for (Entry<String, String> entry : entries) {
        builder.put(entry.getKey(), entry.getValue());
      }
      return builder.build();
    }
  }

  public static class ImmutableBiMapCopyOfGenerator extends TestStringBiMapGenerator {
    @Override
    protected BiMap<String, String> create(Entry<String, String>[] entries) {
      Map<String, String> builder = Maps.newLinkedHashMap();
      for (Entry<String, String> entry : entries) {
        builder.put(entry.getKey(), entry.getValue());
      }
      return ImmutableBiMap.copyOf(builder);
    }
  }

  public static class ImmutableBiMapCopyOfEntriesGenerator extends TestStringBiMapGenerator {
    @Override
    protected BiMap<String, String> create(Entry<String, String>[] entries) {
      return ImmutableBiMap.copyOf(Arrays.asList(entries));
    }
  }
}
