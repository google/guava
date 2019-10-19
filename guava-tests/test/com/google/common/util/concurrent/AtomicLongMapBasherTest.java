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

package com.google.common.util.concurrent;

import com.google.common.annotations.GwtIncompatible;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import junit.framework.TestCase;

/**
 * Basher test for {@link AtomicLongMap}.
 *
 * @author mike nonemacher
 */
@GwtIncompatible // threads

public class AtomicLongMapBasherTest extends TestCase {
  private final Random random = new Random(301);

  public void testModify_basher() throws InterruptedException {
    int nTasks = 3000;
    int nThreads = 100;
    final int getsPerTask = 1000;
    final int deltaRange = 10000;
    final String key = "key";

    final AtomicLong sum = new AtomicLong();
    final AtomicLongMap<String> map = AtomicLongMap.create();

    ExecutorService threadPool = Executors.newFixedThreadPool(nThreads);
    for (int i = 0; i < nTasks; i++) {
      @SuppressWarnings("unused") // go/futurereturn-lsc
      Future<?> possiblyIgnoredError =
          threadPool.submit(
              new Runnable() {
                @Override
                public void run() {
                  int threadSum = 0;
                  for (int j = 0; j < getsPerTask; j++) {
                    long delta = random.nextInt(deltaRange);
                    int behavior = random.nextInt(10);
                    switch (behavior) {
                      case 0:
                        map.incrementAndGet(key);
                        threadSum++;
                        break;
                      case 1:
                        map.decrementAndGet(key);
                        threadSum--;
                        break;
                      case 2:
                        map.addAndGet(key, delta);
                        threadSum += delta;
                        break;
                      case 3:
                        map.getAndIncrement(key);
                        threadSum++;
                        break;
                      case 4:
                        map.getAndDecrement(key);
                        threadSum--;
                        break;
                      case 5:
                        map.getAndAdd(key, delta);
                        threadSum += delta;
                        break;
                      case 6:
                        long oldValue = map.put(key, delta);
                        threadSum += delta - oldValue;
                        break;
                      case 7:
                        oldValue = map.get(key);
                        if (map.replace(key, oldValue, delta)) {
                          threadSum += delta - oldValue;
                        }
                        break;
                      case 8:
                        oldValue = map.remove(key);
                        threadSum -= oldValue;
                        break;
                      case 9:
                        oldValue = map.get(key);
                        if (map.remove(key, oldValue)) {
                          threadSum -= oldValue;
                        }
                        break;
                      default:
                        throw new AssertionError();
                    }
                  }
                  sum.addAndGet(threadSum);
                }
              });
    }

    threadPool.shutdown();
    assertTrue(threadPool.awaitTermination(300, TimeUnit.SECONDS));

    assertEquals(sum.get(), map.get(key));
  }
}
