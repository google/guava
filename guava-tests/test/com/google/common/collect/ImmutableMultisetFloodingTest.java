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
import java.util.Arrays;
import java.util.List;

@GwtIncompatible
public class ImmutableMultisetFloodingTest extends AbstractHashFloodingTest<Multiset<Object>> {
  public ImmutableMultisetFloodingTest() {
    super(
        Arrays.asList(ConstructionPathway.values()),
        n -> n * Math.log(n),
        ImmutableList.of(
            QueryOp.create(
                "count",
                (ms, o) -> {
                  int unused = ms.count(o);
                },
                Math::log)));
  }

  /** All the ways to create an ImmutableMultiset. */
  enum ConstructionPathway implements Construction<Multiset<Object>> {
    COPY_OF_COLLECTION {
      @Override
      public ImmutableMultiset<Object> create(List<?> keys) {
        return ImmutableMultiset.copyOf(keys);
      }
    },
    COPY_OF_ITERATOR {
      @Override
      public ImmutableMultiset<Object> create(List<?> keys) {
        return ImmutableMultiset.copyOf(keys.iterator());
      }
    },
    BUILDER_ADD_ENTRY_BY_ENTRY {
      @Override
      public ImmutableMultiset<Object> create(List<?> keys) {
        ImmutableMultiset.Builder<Object> builder = ImmutableMultiset.builder();
        for (Object o : keys) {
          builder.add(o);
        }
        return builder.build();
      }
    },
    BUILDER_ADD_ALL_COLLECTION {
      @Override
      public ImmutableMultiset<Object> create(List<?> keys) {
        ImmutableMultiset.Builder<Object> builder = ImmutableMultiset.builder();
        builder.addAll(keys);
        return builder.build();
      }
    };

    @CanIgnoreReturnValue
    @Override
    public abstract ImmutableMultiset<Object> create(List<?> keys);
  }
}
