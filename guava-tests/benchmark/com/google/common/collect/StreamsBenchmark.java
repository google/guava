/*
 * Copyright (C) 2016 The Guava Authors
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

import com.google.caliper.BeforeExperiment;
import com.google.caliper.Benchmark;
import com.google.caliper.Param;
import com.google.caliper.api.SkipThisScenarioException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.NoSuchElementException;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * Test stream operation speed.
 *
 * @author Louis Wasserman
 */
public class StreamsBenchmark {
  @Param({"1", "10", "100", "1000", "10000"})
  private int size;

  enum CollectionType {
    ARRAY_LIST(ArrayList::new),
    LINKED_LIST(LinkedList::new);

    final Supplier<Collection<Object>> supplier;

    private CollectionType(Supplier<Collection<Object>> supplier) {
      this.supplier = supplier;
    }
  }

  @Param private CollectionType source;

  enum Operation {
    FIND_FIRST {
      @Override
      Object operate(Stream<?> stream) {
        return stream.findFirst();
      }
    },
    STREAMS_ONLY_ELEMENT {
      @Override
      Object operate(Stream<?> stream) {
        try {
          return stream.collect(MoreCollectors.onlyElement());
        } catch (IllegalArgumentException | NoSuchElementException e) {
          throw new SkipThisScenarioException();
        }
      }
    },
    STREAMS_FIND_LAST {
      @Override
      Object operate(Stream<?> stream) {
        return Streams.findLast(stream);
      }
    },
    REDUCE_LAST {
      @Override
      Object operate(Stream<?> stream) {
        return stream.reduce((a, b) -> b);
      }
    },
    REDUCE_LAST_PARALLEL {
      @Override
      Object operate(Stream<?> stream) {
        return stream.parallel().reduce((a, b) -> b);
      }
    };

    abstract Object operate(Stream<?> stream);
  }

  @Param private Operation operation;

  Collection<Object> collection;

  @BeforeExperiment
  void setUp() {
    collection = source.supplier.get();
    for (int i = 0; i < size; i++) {
      collection.add(new Object());
    }
  }

  @Benchmark
  int runOperation(int reps) {
    int result = 0;
    for (int i = 0; i < reps; i++) {
      result += System.identityHashCode(operation.operate(collection.stream()));
    }
    return result;
  }
}
