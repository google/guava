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

import static com.google.common.util.concurrent.MoreExecutors.directExecutor;

import com.google.common.collect.ConcurrentHashMultiset;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultiset;
import com.google.common.collect.Multiset;
import com.google.common.testing.TestLogHandler;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;
import junit.framework.TestCase;

/** Tests for {@link ListenerCallQueue}. */
public class ListenerCallQueueTest extends TestCase {

  private static final ListenerCallQueue.Event<Object> THROWING_EVENT =
      new ListenerCallQueue.Event<Object>() {
        @Override
        public void call(Object object) {
          throw new RuntimeException();
        }

        @Override
        public String toString() {
          return "throwing()";
        }
      };

  public void testEnqueueAndDispatch() {
    Object listener = new Object();
    ListenerCallQueue<Object> queue = new ListenerCallQueue<>();
    queue.addListener(listener, directExecutor());

    Multiset<Object> counters = ConcurrentHashMultiset.create();
    queue.enqueue(incrementingEvent(counters, listener, 1));
    queue.enqueue(incrementingEvent(counters, listener, 2));
    queue.enqueue(incrementingEvent(counters, listener, 3));
    queue.enqueue(incrementingEvent(counters, listener, 4));
    assertEquals(0, counters.size());
    queue.dispatch();
    assertEquals(multiset(listener, 4), counters);
  }

  public void testEnqueueAndDispatch_multipleListeners() {
    Object listener1 = new Object();
    ListenerCallQueue<Object> queue = new ListenerCallQueue<>();
    queue.addListener(listener1, directExecutor());

    Multiset<Object> counters = ConcurrentHashMultiset.create();
    queue.enqueue(incrementingEvent(counters, listener1, 1));
    queue.enqueue(incrementingEvent(counters, listener1, 2));

    Object listener2 = new Object();
    queue.addListener(listener2, directExecutor());
    queue.enqueue(incrementingEvent(counters, multiset(listener1, 3, listener2, 1)));
    queue.enqueue(incrementingEvent(counters, multiset(listener1, 4, listener2, 2)));
    assertEquals(0, counters.size());
    queue.dispatch();
    assertEquals(multiset(listener1, 4, listener2, 2), counters);
  }

  public void testEnqueueAndDispatch_withExceptions() {
    Object listener = new Object();
    ListenerCallQueue<Object> queue = new ListenerCallQueue<>();
    queue.addListener(listener, directExecutor());

    Multiset<Object> counters = ConcurrentHashMultiset.create();
    queue.enqueue(incrementingEvent(counters, listener, 1));
    queue.enqueue(THROWING_EVENT);
    queue.enqueue(incrementingEvent(counters, listener, 2));
    queue.enqueue(THROWING_EVENT);
    queue.enqueue(incrementingEvent(counters, listener, 3));
    queue.enqueue(THROWING_EVENT);
    queue.enqueue(incrementingEvent(counters, listener, 4));
    queue.enqueue(THROWING_EVENT);
    assertEquals(0, counters.size());
    queue.dispatch();
    assertEquals(multiset(listener, 4), counters);
  }

  static final class MyListener {
    @Override
    public String toString() {
      return "MyListener";
    }
  }

  public void testEnqueueAndDispatch_withLabeledExceptions() {
    Object listener = new MyListener();
    ListenerCallQueue<Object> queue = new ListenerCallQueue<>();
    queue.addListener(listener, directExecutor());
    queue.enqueue(THROWING_EVENT, "custom-label");

    Logger logger = Logger.getLogger(ListenerCallQueue.class.getName());
    logger.setLevel(Level.SEVERE);
    TestLogHandler logHandler = new TestLogHandler();
    logger.addHandler(logHandler);
    try {
      queue.dispatch();
    } finally {
      logger.removeHandler(logHandler);
    }

    assertEquals(1, logHandler.getStoredLogRecords().size());
    assertEquals(
        "Exception while executing callback: MyListener custom-label",
        logHandler.getStoredLogRecords().get(0).getMessage());
  }

  public void testEnqueueAndDispatch_multithreaded() throws InterruptedException {
    Object listener = new Object();
    ExecutorService service = Executors.newFixedThreadPool(4);
    ListenerCallQueue<Object> queue = new ListenerCallQueue<>();
    try {
      queue.addListener(listener, service);

      final CountDownLatch latch = new CountDownLatch(1);
      Multiset<Object> counters = ConcurrentHashMultiset.create();
      queue.enqueue(incrementingEvent(counters, listener, 1));
      queue.enqueue(incrementingEvent(counters, listener, 2));
      queue.enqueue(incrementingEvent(counters, listener, 3));
      queue.enqueue(incrementingEvent(counters, listener, 4));
      queue.enqueue(countDownEvent(latch));
      assertEquals(0, counters.size());
      queue.dispatch();
      latch.await();
      assertEquals(multiset(listener, 4), counters);
    } finally {
      service.shutdown();
    }
  }

  public void testEnqueueAndDispatch_multithreaded_withThrowingRunnable()
      throws InterruptedException {
    Object listener = new Object();
    ExecutorService service = Executors.newFixedThreadPool(4);
    ListenerCallQueue<Object> queue = new ListenerCallQueue<>();
    try {
      queue.addListener(listener, service);

      final CountDownLatch latch = new CountDownLatch(1);
      Multiset<Object> counters = ConcurrentHashMultiset.create();
      queue.enqueue(incrementingEvent(counters, listener, 1));
      queue.enqueue(THROWING_EVENT);
      queue.enqueue(incrementingEvent(counters, listener, 2));
      queue.enqueue(THROWING_EVENT);
      queue.enqueue(incrementingEvent(counters, listener, 3));
      queue.enqueue(THROWING_EVENT);
      queue.enqueue(incrementingEvent(counters, listener, 4));
      queue.enqueue(THROWING_EVENT);
      queue.enqueue(countDownEvent(latch));
      assertEquals(0, counters.size());
      queue.dispatch();
      latch.await();
      assertEquals(multiset(listener, 4), counters);
    } finally {
      service.shutdown();
    }
  }

  private ListenerCallQueue.Event<Object> incrementingEvent(
      Multiset<Object> counters, Object expectedListener, int expectedCount) {
    return incrementingEvent(counters, multiset(expectedListener, expectedCount));
  }

  private ListenerCallQueue.Event<Object> incrementingEvent(
      final Multiset<Object> counters, final Multiset<Object> expected) {
    return new ListenerCallQueue.Event<Object>() {
      @Override
      public void call(Object listener) {
        counters.add(listener);
        assertEquals(expected.count(listener), counters.count(listener));
      }

      @Override
      public String toString() {
        return "incrementing";
      }
    };
  }

  private static <T> ImmutableMultiset<T> multiset(T value, int count) {
    return multiset(ImmutableMap.of(value, count));
  }

  private static <T> ImmutableMultiset<T> multiset(T value1, int count1, T value2, int count2) {
    return multiset(ImmutableMap.of(value1, count1, value2, count2));
  }

  private static <T> ImmutableMultiset<T> multiset(Map<T, Integer> counts) {
    ImmutableMultiset.Builder<T> builder = ImmutableMultiset.builder();
    for (Entry<T, Integer> entry : counts.entrySet()) {
      builder.addCopies(entry.getKey(), entry.getValue());
    }
    return builder.build();
  }

  private ListenerCallQueue.Event<Object> countDownEvent(final CountDownLatch latch) {
    return new ListenerCallQueue.Event<Object>() {
      @Override
      public void call(Object listener) {
        latch.countDown();
      }

      @Override
      public String toString() {
        return "countDown";
      }
    };
  }
}
