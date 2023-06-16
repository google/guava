/*
 * Copyright (C) 2010 The Guava Authors
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
import com.google.common.base.Function;
import java.math.BigInteger;
import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Random;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Benchmarks to compare performance of MinMaxPriorityQueue and PriorityQueue.
 *
 * @author Sverre Sundsdal
 */
public class MinMaxPriorityQueueBenchmark {
  @Param private ComparatorType comparator;

  // TODO(kevinb): add 1000000 back when we have the ability to throw
  // NotApplicableException in the expensive comparator case.
  @Param({"100", "10000"})
  private int size;

  @Param private HeapType heap;

  private Queue<Integer> queue;

  private final Random random = new Random();

  @BeforeExperiment
  void setUp() {
    queue = heap.create(comparator.get());
    for (int i = 0; i < size; i++) {
      queue.add(random.nextInt());
    }
  }

  @Benchmark
  void pollAndAdd(int reps) {
    for (int i = 0; i < reps; i++) {
      // TODO(kevinb): precompute random #s?
      queue.add(queue.poll() ^ random.nextInt());
    }
  }

  @Benchmark
  void populate(int reps) {
    for (int i = 0; i < reps; i++) {
      queue.clear();
      for (int j = 0; j < size; j++) {
        // TODO(kevinb): precompute random #s?
        queue.add(random.nextInt());
      }
    }
  }

  /**
   * Implementation of the InvertedMinMaxPriorityQueue which forwards all calls to a
   * MinMaxPriorityQueue, except poll, which is forwarded to pollMax. That way we can benchmark
   * pollMax using the same code that benchmarks poll.
   */
  static final class InvertedMinMaxPriorityQueue<T> extends ForwardingQueue<T> {
    MinMaxPriorityQueue<T> mmHeap;

    public InvertedMinMaxPriorityQueue(Comparator<T> comparator) {
      mmHeap = MinMaxPriorityQueue.orderedBy(comparator).create();
    }

    @Override
    protected Queue<T> delegate() {
      return mmHeap;
    }

    @Override
    public @Nullable T poll() {
      return mmHeap.pollLast();
    }
  }

  public enum HeapType {
    MIN_MAX {
      @Override
      public Queue<Integer> create(Comparator<Integer> comparator) {
        return MinMaxPriorityQueue.orderedBy(comparator).create();
      }
    },
    PRIORITY_QUEUE {
      @Override
      public Queue<Integer> create(Comparator<Integer> comparator) {
        return new PriorityQueue<>(11, comparator);
      }
    },
    INVERTED_MIN_MAX {
      @Override
      public Queue<Integer> create(Comparator<Integer> comparator) {
        return new InvertedMinMaxPriorityQueue<>(comparator);
      }
    };

    public abstract Queue<Integer> create(Comparator<Integer> comparator);
  }

  /**
   * Does a CPU intensive operation on Integer and returns a BigInteger Used to implement an
   * ordering that spends a lot of cpu.
   */
  static class ExpensiveComputation implements Function<Integer, BigInteger> {
    @Override
    public BigInteger apply(Integer from) {
      BigInteger v = BigInteger.valueOf(from);
      // Math.sin is very slow for values outside 4*pi
      // Need to take absolute value to avoid inverting the value.
      for (double i = 0; i < 100; i += 20) {
        v =
            v.add(
                v.multiply(
                    BigInteger.valueOf(((Double) Math.abs(Math.sin(i) * 10.0)).longValue())));
      }
      return v;
    }
  }

  public enum ComparatorType {
    CHEAP {
      @Override
      public Comparator<Integer> get() {
        return Ordering.natural();
      }
    },
    EXPENSIVE {
      @Override
      public Comparator<Integer> get() {
        return Ordering.natural().onResultOf(new ExpensiveComputation());
      }
    };

    public abstract Comparator<Integer> get();
  }
}
