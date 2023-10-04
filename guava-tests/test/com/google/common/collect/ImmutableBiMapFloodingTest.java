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
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Stream;

@GwtIncompatible
public class ImmutableBiMapFloodingTest extends AbstractHashFloodingTest<BiMap<Object, Object>> {
  public ImmutableBiMapFloodingTest() {
    super(
        EnumSet.allOf(ConstructionPathway.class).stream()
            .flatMap(
                path ->
                    Stream.<Construction<BiMap<Object, Object>>>of(
                        keys ->
                            path.create(
                                Lists.transform(
                                    keys, key -> Maps.immutableEntry(key, new Object()))),
                        keys ->
                            path.create(
                                Lists.transform(
                                    keys, key -> Maps.immutableEntry(new Object(), key))),
                        keys ->
                            path.create(
                                Lists.transform(keys, key -> Maps.immutableEntry(key, key)))))
            .collect(ImmutableList.toImmutableList()),
        n -> n * Math.log(n),
        ImmutableList.of(
            QueryOp.create("BiMap.get", BiMap::get, Math::log),
            QueryOp.create("BiMap.inverse.get", (bm, o) -> bm.inverse().get(o), Math::log)));
  }

  /** All the ways to create an ImmutableBiMap. */
  enum ConstructionPathway {
    COPY_OF_MAP {
      @Override
      public ImmutableBiMap<Object, Object> create(List<Entry<?, ?>> entries) {
        Map<Object, Object> sourceMap = new LinkedHashMap<>();
        for (Entry<?, ?> entry : entries) {
          if (sourceMap.put(entry.getKey(), entry.getValue()) != null) {
            throw new UnsupportedOperationException("duplicate key");
          }
        }
        return ImmutableBiMap.copyOf(sourceMap);
      }
    },
    COPY_OF_ENTRIES {
      @Override
      public ImmutableBiMap<Object, Object> create(List<Entry<?, ?>> entries) {
        return ImmutableBiMap.copyOf(entries);
      }
    },
    BUILDER_PUT_ONE_BY_ONE {
      @Override
      public ImmutableBiMap<Object, Object> create(List<Entry<?, ?>> entries) {
        ImmutableBiMap.Builder<Object, Object> builder = ImmutableBiMap.builder();
        for (Entry<?, ?> entry : entries) {
          builder.put(entry.getKey(), entry.getValue());
        }
        return builder.buildOrThrow();
      }
    },
    BUILDER_PUT_ALL_MAP {
      @Override
      public ImmutableBiMap<Object, Object> create(List<Entry<?, ?>> entries) {
        Map<Object, Object> sourceMap = new LinkedHashMap<>();
        for (Entry<?, ?> entry : entries) {
          if (sourceMap.put(entry.getKey(), entry.getValue()) != null) {
            throw new UnsupportedOperationException("duplicate key");
          }
        }
        ImmutableBiMap.Builder<Object, Object> builder = ImmutableBiMap.builder();
        builder.putAll(sourceMap);
        return builder.buildOrThrow();
      }
    },
    BUILDER_PUT_ALL_ENTRIES {
      @Override
      public ImmutableBiMap<Object, Object> create(List<Entry<?, ?>> entries) {
        return ImmutableBiMap.builder().putAll(entries).buildOrThrow();
      }
    },
    FORCE_JDK {
      @Override
      public ImmutableBiMap<Object, Object> create(List<Entry<?, ?>> entries) {
        return ImmutableBiMap.builder().putAll(entries).buildJdkBacked();
      }
    };

    @CanIgnoreReturnValue
    public abstract ImmutableBiMap<Object, Object> create(List<Entry<?, ?>> entries);
  }
}
