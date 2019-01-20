/*
 * Copyright (C) 2011 The Guava Authors
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

import com.google.common.base.Function;
import com.google.common.primitives.Ints;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import junit.framework.TestCase;

/**
 * Basher test for {@link ConcurrentHashMultiset}: start a bunch of threads, have each of them do
 * operations at random. Each thread keeps track of the per-key deltas that it's directly
 * responsible for; after all threads have completed, we sum the per-key deltas and compare to the
 * existing multiset values.
 *
 * @author mike nonemacher
 */

public class ConcurrentHashMultisetBasherTest extends TestCase {

  public void testAddAndRemove_ConcurrentHashMap() throws Exception {
    testAddAndRemove(new ConcurrentHashMap<String, AtomicInteger>());
  }

  public void testAddAndRemove_ConcurrentSkipListMap() throws Exception {
    testAddAndRemove(new ConcurrentSkipListMap<String, AtomicInteger>());
  }

  public void testAddAndRemove_MapMakerMap() throws Exception {
    MapMaker mapMaker = new MapMaker();
    // force MapMaker to use its own MapMakerInternalMap
    mapMaker.useCustomMap = true;
    testAddAndRemove(mapMaker.<String, AtomicInteger>makeMap());
  }

  private void testAddAndRemove(ConcurrentMap<String, AtomicInteger> map)
      throws ExecutionException, InterruptedException {

    final ConcurrentHashMultiset<String> multiset = new ConcurrentHashMultiset<>(map);
    int nThreads = 20;
    int tasksPerThread = 10;
    int nTasks = nThreads * tasksPerThread;
    ExecutorService pool = Executors.newFixedThreadPool(nThreads);
    ImmutableList<String> keys = ImmutableList.of("a", "b", "c");
    try {
      List<Future<int[]>> futures = Lists.newArrayListWithExpectedSize(nTasks);
      for (int i = 0; i < nTasks; i++) {
        futures.add(pool.submit(new MutateTask(multiset, keys)));
      }

      int[] deltas = new int[3];
      for (Future<int[]> future : futures) {
        int[] taskDeltas = future.get();
        for (int i = 0; i < deltas.length; i++) {
          deltas[i] += taskDeltas[i];
        }
      }

      List<Integer> actualCounts =
          Lists.transform(
              keys,
              new Function<String, Integer>() {
                @Override
                public Integer apply(String key) {
                  return multiset.count(key);
                }
              });
      assertEquals("Counts not as expected", Ints.asList(deltas), actualCounts);
    } finally {
      pool.shutdownNow();
    }

    // Since we have access to the backing map, verify that there are no zeroes in the map
    for (AtomicInteger value : map.values()) {
      assertTrue("map should not contain a zero", value.get() != 0);
    }
  }

  private static class MutateTask implements Callable<int[]> {
    private final ConcurrentHashMultiset<String> multiset;
    private final ImmutableList<String> keys;
    private final Random random = new Random();

    private MutateTask(ConcurrentHashMultiset<String> multiset, ImmutableList<String> keys) {
      this.multiset = multiset;
      this.keys = keys;
    }

    @Override
    public int[] call() throws Exception {
      int iterations = 100000;
      int nKeys = keys.size();
      int[] deltas = new int[nKeys];
      Operation[] operations = Operation.values();
      for (int i = 0; i < iterations; i++) {
        int keyIndex = random.nextInt(nKeys);
        String key = keys.get(keyIndex);
        Operation op = operations[random.nextInt(operations.length)];
        switch (op) {
          case ADD:
            {
              int delta = random.nextInt(10);
              multiset.add(key, delta);
              deltas[keyIndex] += delta;
              break;
            }
          case SET_COUNT:
            {
              int newValue = random.nextInt(3);
              int oldValue = multiset.setCount(key, newValue);
              deltas[keyIndex] += (newValue - oldValue);
              break;
            }
          case SET_COUNT_IF:
            {
              int newValue = random.nextInt(3);
              int oldValue = multiset.count(key);
              if (multiset.setCount(key, oldValue, newValue)) {
                deltas[keyIndex] += (newValue - oldValue);
              }
              break;
            }
          case REMOVE:
            {
              int delta = random.nextInt(6); // [0, 5]
              int oldValue = multiset.remove(key, delta);
              deltas[keyIndex] -= Math.min(delta, oldValue);
              break;
            }
          case REMOVE_EXACTLY:
            {
              int delta = random.nextInt(5); // [0, 4]
              if (multiset.removeExactly(key, delta)) {
                deltas[keyIndex] -= delta;
              }
              break;
            }
        }
      }
      return deltas;
    }

    private enum Operation {
      ADD,
      SET_COUNT,
      SET_COUNT_IF,
      REMOVE,
      REMOVE_EXACTLY,
      ;
    }
  }
}
