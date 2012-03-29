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
import com.google.common.collect.testing.TestStringSortedMapGenerator;

import java.util.Map;
import java.util.Map.Entry;

/**
 * Generators of sorted maps and derived collections.
 *
 * @author Kevin Bourrillion
 * @author Jesse Wilson
 * @author Jared Levy
 * @author Hayward Chan
 * @author Chris Povirk
 */
@GwtCompatible
public class SortedMapGenerators {
  public static class ImmutableSortedMapGenerator extends TestStringSortedMapGenerator {
    @Override public Map<String, String> create(Entry<String, String>[] entries) {
      ImmutableSortedMap.Builder<String, String> builder = ImmutableSortedMap.naturalOrder();
      for (Entry<String, String> entry : entries) {
        checkNotNull(entry);
        builder.put(entry.getKey(), entry.getValue());
      }
      return builder.build();
    }
  }
}
