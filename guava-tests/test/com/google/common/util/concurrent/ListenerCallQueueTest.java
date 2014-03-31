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

import com.google.common.util.concurrent.ListenerCallQueue.Callback;

import junit.framework.TestCase;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Tests for {@link ListenerCallQueue}.
 */
public class ListenerCallQueueTest extends TestCase {

  private static final Callback<Object> THROWING_CALLBACK = new Callback<Object>("throwing()") {
    @Override public void call(Object object) {
      throw new RuntimeException();
    }
  };

  public void testAddAndExecute() {
    Object listenerInstance = new Object();
    ListenerCallQueue<Object> queue =
        new ListenerCallQueue<Object>(listenerInstance, MoreExecutors.sameThreadExecutor());

    AtomicInteger counter = new AtomicInteger();
    queue.add(incrementingCallback(counter, 1));
    queue.add(incrementingCallback(counter, 2));
    queue.add(incrementingCallback(counter, 3));
    queue.add(incrementingCallback(counter, 4));
    assertEquals(0, counter.get());
    queue.execute();
    assertEquals(4, counter.get());
  }

  public void testAddAndExecute_withExceptions() {
    Object listenerInstance = new Object();
    ListenerCallQueue<Object> queue =
        new ListenerCallQueue<Object>(listenerInstance, MoreExecutors.sameThreadExecutor());

    AtomicInteger counter = new AtomicInteger();
    queue.add(incrementingCallback(counter, 1));
    queue.add(THROWING_CALLBACK);
    queue.add(incrementingCallback(counter, 2));
    queue.add(THROWING_CALLBACK);
    queue.add(incrementingCallback(counter, 3));
    queue.add(THROWING_CALLBACK);
    queue.add(incrementingCallback(counter, 4));
    queue.add(THROWING_CALLBACK);
    assertEquals(0, counter.get());
    queue.execute();
    assertEquals(4, counter.get());
  }

  public void testAddAndExecute_multithreaded() throws InterruptedException {
    ExecutorService service = Executors.newFixedThreadPool(4);
    try {
      ListenerCallQueue<Object> queue =
          new ListenerCallQueue<Object>(new Object(), service);

      final CountDownLatch latch = new CountDownLatch(1);
      AtomicInteger counter = new AtomicInteger();
      queue.add(incrementingCallback(counter, 1));
      queue.add(incrementingCallback(counter, 2));
      queue.add(incrementingCallback(counter, 3));
      queue.add(incrementingCallback(counter, 4));
      queue.add(countDownCallback(latch));
      assertEquals(0, counter.get());
      queue.execute();
      latch.await();
      assertEquals(4, counter.get());
    } finally {
      service.shutdown();
    }
  }

  public void testAddAndExecute_multithreaded_withThrowingRunnable() throws InterruptedException {
    ExecutorService service = Executors.newFixedThreadPool(4);
    try {
      ListenerCallQueue<Object> queue =
          new ListenerCallQueue<Object>(new Object(), service);

      final CountDownLatch latch = new CountDownLatch(1);
      AtomicInteger counter = new AtomicInteger();
      queue.add(incrementingCallback(counter, 1));
      queue.add(THROWING_CALLBACK);
      queue.add(incrementingCallback(counter, 2));
      queue.add(THROWING_CALLBACK);
      queue.add(incrementingCallback(counter, 3));
      queue.add(THROWING_CALLBACK);
      queue.add(incrementingCallback(counter, 4));
      queue.add(THROWING_CALLBACK);
      queue.add(countDownCallback(latch));
      assertEquals(0, counter.get());
      queue.execute();
      latch.await();
      assertEquals(4, counter.get());
    } finally {
      service.shutdown();
    }
  }

  private Callback<Object> incrementingCallback(final AtomicInteger counter, final int expected) {
    return new Callback<Object>("incrementing") {
      @Override void call(Object listener) {
        assertEquals(expected, counter.incrementAndGet());
      }
    };
  }

  private Callback<Object> countDownCallback(final CountDownLatch latch) {
    return new Callback<Object>("countDown") {
      @Override void call(Object listener) {
        latch.countDown();
      }
    };
  }
}
