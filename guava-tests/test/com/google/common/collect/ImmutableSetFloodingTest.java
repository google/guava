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

import com.google.common.annotations.GwtIncompatible;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

@GwtIncompatible
public class ImmutableSetFloodingTest extends AbstractHashFloodingTest<Set<Object>> {
  public ImmutableSetFloodingTest() {
    super(
        Arrays.asList(ConstructionPathway.values()),
        n -> n * Math.log(n),
        ImmutableList.of(
            QueryOp.create(
                "contains",
                (s, o) -> {
                  boolean unused = s.contains(o);
                },
                Math::log)));
  }

  /** All the ways to construct an ImmutableSet. */
  enum ConstructionPathway implements Construction<Set<Object>> {
    OF {
      @Override
      public ImmutableSet<Object> create(List<?> list) {
        Object o1 = list.get(0);
        Object o2 = list.get(1);
        Object o3 = list.get(2);
        Object o4 = list.get(3);
        Object o5 = list.get(4);
        Object o6 = list.get(5);
        Object[] rest = list.subList(6, list.size()).toArray();
        return ImmutableSet.of(o1, o2, o3, o4, o5, o6, rest);
      }
    },
    COPY_OF_ARRAY {
      @Override
      public ImmutableSet<Object> create(List<?> list) {
        return ImmutableSet.copyOf(list.toArray());
      }
    },
    COPY_OF_LIST {
      @Override
      public ImmutableSet<Object> create(List<?> list) {
        return ImmutableSet.copyOf(list);
      }
    },
    BUILDER_ADD_ONE_BY_ONE {
      @Override
      public ImmutableSet<Object> create(List<?> list) {
        ImmutableSet.Builder<Object> builder = ImmutableSet.builder();
        for (Object o : list) {
          builder.add(o);
        }
        return builder.build();
      }
    },
    BUILDER_ADD_ARRAY {
      @Override
      public ImmutableSet<Object> create(List<?> list) {
        ImmutableSet.Builder<Object> builder = ImmutableSet.builder();
        builder.add(list.toArray());
        return builder.build();
      }
    },
    BUILDER_ADD_LIST {
      @Override
      public ImmutableSet<Object> create(List<?> list) {
        ImmutableSet.Builder<Object> builder = ImmutableSet.builder();
        builder.addAll(list);
        return builder.build();
      }
    };
  }
}
