/*
 * Copyright (C) 2014 The Guava Authors
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

package com.google.common.util.concurrent;

import static com.google.common.collect.Iterables.cycle;
import static com.google.common.collect.Iterables.limit;

import com.google.caliper.BeforeExperiment;
import com.google.caliper.Benchmark;
import com.google.caliper.Param;
import com.google.caliper.api.Footprint;
import com.google.caliper.api.VmOptions;
import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableList;
import com.google.common.primitives.Ints;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/** A benchmark comparing the various striped implementations. */
@VmOptions({"-Xms12g", "-Xmx12g", "-d64"})
public class StripedBenchmark {
  private static final Supplier<Lock> LOCK_SUPPLIER =
      new Supplier<Lock>() {
        @Override
        public Lock get() {
          return new ReentrantLock();
        }
      };

  @Param({"2", "8", "64", "1024", "65536"})
  int numStripes;

  @Param Impl impl;

  enum Impl {
    EAGER {
      @Override
      Striped<Lock> get(int stripes) {
        return Striped.lock(stripes);
      }
    },
    LAZY_SMALL {
      @Override
      Striped<Lock> get(int stripes) {
        return new Striped.SmallLazyStriped<>(stripes, LOCK_SUPPLIER);
      }
    },
    LAZY_LARGE {
      @Override
      Striped<Lock> get(int stripes) {
        return new Striped.LargeLazyStriped<>(stripes, LOCK_SUPPLIER);
      }
    };

    abstract Striped<Lock> get(int stripes);
  }

  private Striped<Lock> striped;
  private int[] stripes;
  private List<Integer> bulkGetSet;

  @BeforeExperiment
  void setUp() {
    this.striped = impl.get(numStripes);
    stripes = new int[numStripes];
    for (int i = 0; i < numStripes; i++) {
      stripes[i] = i;
    }
    List<Integer> asList = Ints.asList(stripes);
    Collections.shuffle(asList, new Random(0xdeadbeef));

    // do bulk gets with exactly 10 keys (possibly <10 stripes) (or less if numStripes is smaller)
    bulkGetSet = ImmutableList.copyOf(limit(cycle(asList), 10));
  }

  @Footprint
  Object sizeOfStriped() {
    return impl.get(numStripes);
  }

  // a place to put the locks in sizeOfPopulatedStriped so they don't get GC'd before we measure
  final List<Lock> locks = new ArrayList<>(numStripes);

  @Footprint
  Object sizeOfPopulatedStriped() {
    locks.clear();
    Striped<Lock> striped = impl.get(numStripes);
    for (int i : stripes) {
      locks.add(striped.getAt(i));
    }
    return striped;
  }

  @Benchmark
  long timeConstruct(long reps) {
    long rvalue = 0;
    int numStripesLocal = numStripes;
    Impl implLocal = impl;
    for (long i = 0; i < reps; i++) {
      rvalue += implLocal.get(numStripesLocal).hashCode();
    }
    return rvalue;
  }

  @Benchmark
  long timeGetAt(long reps) {
    long rvalue = 0;
    int[] stripesLocal = stripes;
    int mask = numStripes - 1;
    Striped<Lock> stripedLocal = striped;
    for (long i = 0; i < reps; i++) {
      rvalue += stripedLocal.getAt(stripesLocal[(int) (i & mask)]).hashCode();
    }
    return rvalue;
  }

  @Benchmark
  long timeBulkGet(long reps) {
    long rvalue = 0;
    List<Integer> bulkGetSetLocal = bulkGetSet;
    Striped<Lock> stripedLocal = striped;
    for (long i = 0; i < reps; i++) {
      rvalue += stripedLocal.bulkGet(bulkGetSetLocal).hashCode();
    }
    return rvalue;
  }
}
