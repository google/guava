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

import com.google.common.annotations.GwtIncompatible;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@GwtIncompatible
public class ImmutableMapFloodingTest extends AbstractHashFloodingTest<Map<Object, Object>> {
  public ImmutableMapFloodingTest() {
    super(
        Arrays.asList(ConstructionPathway.values()),
        n -> n * Math.log(n),
        ImmutableList.of(QueryOp.MAP_GET));
  }

  /** All the ways to create an ImmutableMap. */
  enum ConstructionPathway implements Construction<Map<Object, Object>> {
    COPY_OF_MAP {
      @Override
      public Map<Object, Object> create(List<?> keys) {
        Map<Object, Object> sourceMap = new LinkedHashMap<>();
        for (Object k : keys) {
          if (sourceMap.put(k, "dummy value") != null) {
            throw new UnsupportedOperationException("duplicate key");
          }
        }
        return ImmutableMap.copyOf(sourceMap);
      }
    },
    COPY_OF_ENTRIES {
      @Override
      public Map<Object, Object> create(List<?> keys) {
        return ImmutableMap.copyOf(
            Lists.transform(keys, k -> Maps.immutableEntry(k, "dummy value")));
      }
    },
    BUILDER_PUT_ONE_BY_ONE {
      @Override
      public Map<Object, Object> create(List<?> keys) {
        ImmutableMap.Builder<Object, Object> builder = ImmutableMap.builder();
        for (Object k : keys) {
          builder.put(k, "dummy value");
        }
        return builder.buildOrThrow();
      }
    },
    BUILDER_PUT_ENTRIES_ONE_BY_ONE {
      @Override
      public Map<Object, Object> create(List<?> keys) {
        ImmutableMap.Builder<Object, Object> builder = ImmutableMap.builder();
        for (Object k : keys) {
          builder.put(Maps.immutableEntry(k, "dummy value"));
        }
        return builder.buildOrThrow();
      }
    },
    BUILDER_PUT_ALL_MAP {
      @Override
      public Map<Object, Object> create(List<?> keys) {
        Map<Object, Object> sourceMap = new LinkedHashMap<>();
        for (Object k : keys) {
          if (sourceMap.put(k, "dummy value") != null) {
            throw new UnsupportedOperationException("duplicate key");
          }
        }
        return ImmutableMap.builder().putAll(sourceMap).buildOrThrow();
      }
    },
    BUILDER_PUT_ALL_ENTRIES {
      @Override
      public Map<Object, Object> create(List<?> keys) {
        return ImmutableMap.builder()
            .putAll(Lists.transform(keys, k -> Maps.immutableEntry(k, "dummy value")))
            .buildOrThrow();
      }
    },
    FORCE_JDK {
      @Override
      public Map<Object, Object> create(List<?> keys) {
        ImmutableMap.Builder<Object, Object> builder = ImmutableMap.builder();
        for (Object k : keys) {
          builder.put(k, "dummy value");
        }
        return builder.buildJdkBacked();
      }
    };
  }
}
