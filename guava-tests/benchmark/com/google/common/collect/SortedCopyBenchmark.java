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
import java.util.List;
import java.util.Random;

/**
 * Provides supporting data for performance notes in the documentation of {@link
 * Ordering#sortedCopy} and {@link Ordering#immutableSortedCopy}, as well as for
 * automated code suggestions.
 *
 */
public class SortedCopyBenchmark {
  @Param({"1", "10", "1000", "1000000"}) int size; // logarithmic triangular

  @Param boolean mutable;

  @Param InputOrder inputOrder;

  enum InputOrder {
    SORTED {
      @Override void arrange(List<Integer> list) {
        Collections.sort(list);
      }
    },
    ALMOST_SORTED {
      @Override void arrange(List<Integer> list) {
        Collections.sort(list);
        if (list.size() > 1) {
          // Start looking in the middle, for the heck of it
          int i = (list.size() - 1) / 2;
          // Find two that are different - it would be extraordinarily unlikely not to
          while (list.get(i).equals(list.get(i + 1))) {
            i++;
          }
          Collections.swap(list, i, i + 1);
        }
      }
    },
    RANDOM {
      @Override void arrange(List<Integer> list) {}
    };

    abstract void arrange(List<Integer> list);
  }

  private ImmutableList<Integer> input;

  @BeforeExperiment
  void setUp() {
    checkArgument(size > 0, "empty collection not supported");
    List<Integer> temp = new ArrayList<Integer>(size);

    Random random = new Random();
    while (temp.size() < size) {
      temp.add(random.nextInt());
    }
    inputOrder.arrange(temp);
    input = ImmutableList.copyOf(temp);
  }

  @Benchmark
  int collections(int reps) {
    int dummy = 0;
    // Yes, this could be done more elegantly
    if (mutable) {
      for (int i = 0; i < reps; i++) {
        List<Integer> copy = new ArrayList<Integer>(input);
        Collections.sort(copy);
        dummy += copy.get(0);
      }
    } else {
      for (int i = 0; i < reps; i++) {
        List<Integer> copy = new ArrayList<Integer>(input);
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

  private static final Ordering<Integer> ORDERING = Ordering.natural();
}
