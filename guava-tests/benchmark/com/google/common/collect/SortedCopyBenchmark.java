/*
 * Copyright (C) 2014 The Guava Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.common.collect;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.caliper.BeforeExperiment;
import com.google.caliper.Benchmark;
import com.google.caliper.Param;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;

/**
 * Provides supporting data for performance notes in the documentation of {@link
 * Ordering#sortedCopy} and {@link Ordering#immutableSortedCopy}, as well as for automated code
 * suggestions.
 *
 */
public class SortedCopyBenchmark {
  @Param({"1", "10", "1000", "1000000"})
  int size; // logarithmic triangular

  @Param boolean mutable;

  @Param InputOrder inputOrder;

  enum InputOrder {
    SORTED {
      @Override
      void arrange(List<Integer> list) {
        Collections.sort(list);
      }
    },
    ALMOST_SORTED {
      @Override
      void arrange(List<Integer> list) {
        Collections.sort(list);
        if (list.size() > 1) {
          int i = (list.size() - 1) / 2;
          Collections.swap(list, i, i + 1);
        }
      }
    },
    RANDOM {
      @Override
      void arrange(List<Integer> list) {}
    };

    abstract void arrange(List<Integer> list);
  }

  private ImmutableList<Integer> input;

  @BeforeExperiment
  void setUp() {
    checkArgument(size > 0, "empty collection not supported");
    Set<Integer> set = new LinkedHashSet<>(size);

    Random random = new Random();
    while (set.size() < size) {
      set.add(random.nextInt());
    }
    List<Integer> list = new ArrayList<>(set);
    inputOrder.arrange(list);
    input = ImmutableList.copyOf(list);
  }

  @Benchmark
  int collections(int reps) {
    int dummy = 0;
    // Yes, this could be done more elegantly
    if (mutable) {
      for (int i = 0; i < reps; i++) {
        List<Integer> copy = new ArrayList<>(input);
        Collections.sort(copy);
        dummy += copy.get(0);
      }
    } else {
      for (int i = 0; i < reps; i++) {
        List<Integer> copy = new ArrayList<>(input);
        Collections.sort(copy);
        dummy += ImmutableList.copyOf(copy).get(0);
      }
    }
    return dummy;
  }

  @Benchmark
  int ordering(int reps) {
    int dummy = 0;
    if (mutable) {
      for (int i = 0; i < reps; i++) {
        dummy += ORDERING.sortedCopy(input).get(0);
      }
    } else {
      for (int i = 0; i < reps; i++) {
        dummy += ORDERING.immutableSortedCopy(input).get(0);
      }
    }
    return dummy;
  }

  @Benchmark
  int sortedSet(int reps) {
    int dummy = 0;
    if (mutable) {
      for (int i = 0; i < reps; i++) {
        dummy += new TreeSet<Integer>(input).first();
      }
    } else {
      for (int i = 0; i < reps; i++) {
        dummy += ImmutableSortedSet.copyOf(input).first();
      }
    }
    return dummy;
  }

  private static final Ordering<Integer> ORDERING = Ordering.natural();
}
