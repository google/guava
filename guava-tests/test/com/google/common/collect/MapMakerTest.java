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

import static com.google.common.util.concurrent.Uninterruptibles.awaitUninterruptibly;

import com.google.common.base.Function;
import com.google.common.collect.MapMaker.RemovalNotification;
import com.google.common.collect.MapMakerInternalMapTest.QueuingRemovalListener;
import com.google.common.testing.NullPointerTester;

import junit.framework.TestCase;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Charles Fry
 */
public class MapMakerTest extends TestCase {

  public void testNullParameters() throws Exception {
    NullPointerTester tester = new NullPointerTester();
    tester.testAllPublicInstanceMethods(new MapMaker());
  }

  public void testRemovalNotification_clear() throws InterruptedException {
    // If a clear() happens while a computation is pending, we should not get a removal
    // notification.

    final CountDownLatch computingLatch = new CountDownLatch(1);
    Function<String, String> computingFunction = new DelayingIdentityLoader(computingLatch);
    QueuingRemovalListener<String, String> listener = new QueuingRemovalListener<String, String>();

    @SuppressWarnings("deprecation") // test of deprecated code
    final ConcurrentMap<String, String> map = new MapMaker()
        .concurrencyLevel(1)
        .removalListener(listener)
        .makeComputingMap(computingFunction);

    // seed the map, so its segment's count > 0
    map.put("a", "a");

    final CountDownLatch computationStarted = new CountDownLatch(1);
    final CountDownLatch computationComplete = new CountDownLatch(1);
    new Thread(new Runnable() {
      @Override public void run() {
        computationStarted.countDown();
        map.get("b");
        computationComplete.countDown();
      }
    }).start();

    // wait for the computingEntry to be created
    computationStarted.await();
    map.clear();
    // let the computation proceed
    computingLatch.countDown();
    // don't check map.size() until we know the get("b") call is complete
    computationComplete.await();

    // At this point, the listener should be holding the seed value (a -> a), and the map should
    // contain the computed value (b -> b), since the clear() happened before the computation
    // completed.
    assertEquals(1, listener.size());
    RemovalNotification<String, String> notification = listener.remove();
    assertEquals("a", notification.getKey());
    assertEquals("a", notification.getValue());
    assertEquals(1, map.size());
    assertEquals("b", map.get("b"));
  }

  // "Basher tests", where we throw a bunch of stuff at a Cache and check basic invariants.

  /**
   * This is a less carefully-controlled version of {@link #testRemovalNotification_clear} - this is
   * a black-box test that tries to create lots of different thread-interleavings, and asserts that
   * each computation is affected by a call to {@code clear()} (and therefore gets passed to the
   * removal listener), or else is not affected by the {@code clear()} (and therefore exists in the
   * map afterward).
   */

  public void testRemovalNotification_clear_basher() throws InterruptedException {
    // If a clear() happens close to the end of computation, one of two things should happen:
    // - computation ends first: the removal listener is called, and the map does not contain the
    //   key/value pair
    // - clear() happens first: the removal listener is not called, and the map contains the pair
    CountDownLatch computationLatch = new CountDownLatch(1);
    QueuingRemovalListener<String, String> listener = new QueuingRemovalListener<String, String>();

    @SuppressWarnings("deprecation") // test of deprecated code
    final Map<String, String> map = new MapMaker()
        .removalListener(listener)
        .concurrencyLevel(20)
        .makeComputingMap(new DelayingIdentityLoader<String>(computationLatch));

    int nThreads = 100;
    int nTasks = 1000;
    int nSeededEntries = 100;
    Set<String> expectedKeys = Sets.newHashSetWithExpectedSize(nTasks + nSeededEntries);
    // seed the map, so its segments have a count>0; otherwise, clear() won't visit the in-progress
    // entries
    for (int i = 0; i < nSeededEntries; i++) {
      String s = "b" + i;
      map.put(s, s);
      expectedKeys.add(s);
    }

    final AtomicInteger computedCount = new AtomicInteger();
    ExecutorService threadPool = Executors.newFixedThreadPool(nThreads);
    final CountDownLatch tasksFinished = new CountDownLatch(nTasks);
    for (int i = 0; i < nTasks; i++) {
      final String s = "a" + i;
      threadPool.submit(new Runnable() {
        @Override public void run() {
          map.get(s);
          computedCount.incrementAndGet();
          tasksFinished.countDown();
        }
      });
      expectedKeys.add(s);
    }

    computationLatch.countDown();
    // let some computations complete
    while (computedCount.get() < nThreads) {
      Thread.yield();
    }
    map.clear();
    tasksFinished.await();

    // Check all of the removal notifications we received: they should have had correctly-associated
    // keys and values. (An earlier bug saw removal notifications for in-progress computations,
    // which had real keys with null values.)
    Map<String, String> removalNotifications = Maps.newHashMap();
    for (RemovalNotification<String, String> notification : listener) {
      removalNotifications.put(notification.getKey(), notification.getValue());
      assertEquals("Unexpected key/value pair passed to removalListener",
          notification.getKey(), notification.getValue());
    }

    // All of the seed values should have been visible, so we should have gotten removal
    // notifications for all of them.
    for (int i = 0; i < nSeededEntries; i++) {
      assertEquals("b" + i, removalNotifications.get("b" + i));
    }

    // Each of the values added to the map should either still be there, or have seen a removal
    // notification.
    assertEquals(expectedKeys, Sets.union(map.keySet(), removalNotifications.keySet()));
    assertTrue(Sets.intersection(map.keySet(), removalNotifications.keySet()).isEmpty());
  }

  static final class DelayingIdentityLoader<T> implements Function<T, T> {
    private final CountDownLatch delayLatch;

    DelayingIdentityLoader(CountDownLatch delayLatch) {
      this.delayLatch = delayLatch;
    }

    @Override public T apply(T key) {
      awaitUninterruptibly(delayLatch);
      return key;
    }
  }
}
