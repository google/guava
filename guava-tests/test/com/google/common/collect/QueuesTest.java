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

import com.google.common.util.concurrent.Uninterruptibles;

import junit.framework.TestCase;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;

/**
 * Tests for {@link Queues}.
 *
 * @author Dimitris Andreou
 */

public class QueuesTest extends TestCase {
  /*
   * All the following tests relate to BlockingQueue methods in Queues.
   */

  public static List<BlockingQueue<Object>> blockingQueues() {
    return ImmutableList.<BlockingQueue<Object>>of(
        new LinkedBlockingQueue<Object>(),
        new LinkedBlockingQueue<Object>(10),
        new SynchronousQueue<Object>(),
        new ArrayBlockingQueue<Object>(10),
        new PriorityBlockingQueue<Object>(10, Ordering.arbitrary()));
  }

  private ExecutorService threadPool;

  @Override
  public void setUp() {
    threadPool = Executors.newCachedThreadPool();
  }

  @Override
  public void tearDown() throws InterruptedException {
    // notice that if a Producer is interrupted (a bug), the Producer will go into an infinite
    // loop, which will be noticed here
    threadPool.shutdown();
    assertTrue("Some worker didn't finish in time",
        threadPool.awaitTermination(1, TimeUnit.SECONDS));
  }

  private static <T> int drain(BlockingQueue<T> q, Collection<? super T> buffer, int maxElements,
      long timeout, TimeUnit unit, boolean interruptibly) throws InterruptedException {
    return interruptibly
        ? Queues.drain(q, buffer, maxElements, timeout, unit)
        : Queues.drainUninterruptibly(q, buffer, maxElements, timeout, unit);
  }

  public void testMultipleProducers() throws Exception {
    for (BlockingQueue<Object> q : blockingQueues()) {
      testMultipleProducers(q);
    }
  }

  private void testMultipleProducers(BlockingQueue<Object> q)
      throws InterruptedException {
    for (boolean interruptibly : new boolean[] { true, false }) {
      threadPool.submit(new Producer(q, 20));
      threadPool.submit(new Producer(q, 20));
      threadPool.submit(new Producer(q, 20));
      threadPool.submit(new Producer(q, 20));
      threadPool.submit(new Producer(q, 20));

      List<Object> buf = Lists.newArrayList();
      int elements = drain(q, buf, 100, Long.MAX_VALUE, TimeUnit.NANOSECONDS, interruptibly);
      assertEquals(100, elements);
      assertEquals(100, buf.size());
      assertDrained(q);
    }
  }

  public void testDrainTimesOut() throws Exception {
    for (BlockingQueue<Object> q : blockingQueues()) {
      testDrainTimesOut(q);
    }
  }

  private void testDrainTimesOut(BlockingQueue<Object> q) throws Exception {
    for (boolean interruptibly : new boolean[] { true, false }) {
      assertEquals(0, Queues.drain(q, ImmutableList.of(), 1, 10, TimeUnit.MILLISECONDS));

      // producing one, will ask for two
      Future<?> submitter = threadPool.submit(new Producer(q, 1));

      // make sure we time out
      long startTime = System.nanoTime();

      int drained = drain(q, Lists.newArrayList(), 2, 10, TimeUnit.MILLISECONDS, interruptibly);
      assertTrue(drained <= 1);

      assertTrue((System.nanoTime() - startTime) >= TimeUnit.MILLISECONDS.toNanos(10));

      // If even the first one wasn't there, clean up so that the next test doesn't see an element.
      submitter.get();
      if (drained == 0) {
        assertNotNull(q.poll());
      }
    }
  }

  public void testZeroElements() throws Exception {
    for (BlockingQueue<Object> q : blockingQueues()) {
      testZeroElements(q);
    }
  }

  private void testZeroElements(BlockingQueue<Object> q) throws InterruptedException {
    for (boolean interruptibly : new boolean[] { true, false }) {
      // asking to drain zero elements
      assertEquals(0, drain(q, ImmutableList.of(), 0, 10, TimeUnit.MILLISECONDS, interruptibly));
    }
  }

  public void testEmpty() throws Exception {
    for (BlockingQueue<Object> q : blockingQueues()) {
      testEmpty(q);
    }
  }

  private void testEmpty(BlockingQueue<Object> q) {
    assertDrained(q);
  }

  public void testNegativeMaxElements() throws Exception {
    for (BlockingQueue<Object> q : blockingQueues()) {
      testNegativeMaxElements(q);
    }
  }

  private void testNegativeMaxElements(BlockingQueue<Object> q) throws InterruptedException {
    threadPool.submit(new Producer(q, 1));

    List<Object> buf = Lists.newArrayList();
    int elements = Queues.drain(q, buf, -1, Long.MAX_VALUE, TimeUnit.NANOSECONDS);
    assertEquals(elements, 0);
    assertTrue(buf.isEmpty());

    // Clean up produced element to free the producer thread, otherwise it will complain
    // when we shutdown the threadpool.
    Queues.drain(q, buf, 1, Long.MAX_VALUE, TimeUnit.NANOSECONDS);
  }

  public void testDrain_throws() throws Exception {
    for (BlockingQueue<Object> q : blockingQueues()) {
      testDrain_throws(q);
    }
  }

  private void testDrain_throws(BlockingQueue<Object> q) {
    threadPool.submit(new Interrupter(Thread.currentThread()));
    try {
      Queues.drain(q, ImmutableList.of(), 100, Long.MAX_VALUE, TimeUnit.NANOSECONDS);
      fail();
    } catch (InterruptedException expected) {
    }
  }

  public void testDrainUninterruptibly_doesNotThrow() throws Exception {
    for (BlockingQueue<Object> q : blockingQueues()) {
      testDrainUninterruptibly_doesNotThrow(q);
    }
  }

  private void testDrainUninterruptibly_doesNotThrow(final BlockingQueue<Object> q) {
    final Thread mainThread = Thread.currentThread();
    threadPool.submit(new Runnable() {
      public void run() {
        new Producer(q, 50).run();
        new Interrupter(mainThread).run();
        new Producer(q, 50).run();
      }
    });
    List<Object> buf = Lists.newArrayList();
    int elements =
        Queues.drainUninterruptibly(q, buf, 100, Long.MAX_VALUE, TimeUnit.NANOSECONDS);
    // so when this drains all elements, we know the thread has also been interrupted in between
    assertTrue(Thread.interrupted());
    assertEquals(100, elements);
    assertEquals(100, buf.size());
  }

  public void testNewLinkedBlockingQueueCapacity() {
    try {
      Queues.newLinkedBlockingQueue(0);
      fail("Should have thrown IllegalArgumentException");
    } catch (IllegalArgumentException expected) {
      // any capacity less than 1 should throw IllegalArgumentException
    }
    assertEquals(1, Queues.newLinkedBlockingQueue(1).remainingCapacity());
    assertEquals(11, Queues.newLinkedBlockingQueue(11).remainingCapacity());
  }

  /**
   * Checks that #drain() invocations behave correctly for a drained (empty) queue.
   */
  private void assertDrained(BlockingQueue<Object> q) {
    assertNull(q.peek());
    assertInterruptibleDrained(q);
    assertUninterruptibleDrained(q);
  }

  private void assertInterruptibleDrained(BlockingQueue<Object> q) {
    // nothing to drain, thus this should wait doing nothing
    try {
      assertEquals(0, Queues.drain(q, ImmutableList.of(), 0, 10, TimeUnit.MILLISECONDS));
    } catch (InterruptedException e) {
      throw new AssertionError();
    }

    // but does the wait actually occurs?
    threadPool.submit(new Interrupter(Thread.currentThread()));
    try {
      // if waiting works, this should get stuck
      Queues.drain(q, Lists.newArrayList(), 1, Long.MAX_VALUE, TimeUnit.NANOSECONDS);
      fail();
    } catch (InterruptedException expected) {
      // we indeed waited; a slow thread had enough time to interrupt us
    }
  }

  // same as above; uninterruptible version
  private void assertUninterruptibleDrained(BlockingQueue<Object> q) {
    assertEquals(0,
        Queues.drainUninterruptibly(q, ImmutableList.of(), 0, 10, TimeUnit.MILLISECONDS));

    // but does the wait actually occurs?
    threadPool.submit(new Interrupter(Thread.currentThread()));

    long startTime = System.nanoTime();
    Queues.drainUninterruptibly(
        q, Lists.newArrayList(), 1, 10, TimeUnit.MILLISECONDS);
    assertTrue((System.nanoTime() - startTime) >= TimeUnit.MILLISECONDS.toNanos(10));
    // wait for interrupted status and clear it
    while (!Thread.interrupted()) { Thread.yield(); }
  }

  private static class Producer implements Runnable {
    final BlockingQueue<Object> q;
    final int elements;

    Producer(BlockingQueue<Object> q, int elements) {
      this.q = q;
      this.elements = elements;
    }

    @Override public void run() {
      try {
        for (int i = 0; i < elements; i++) {
          q.put(new Object());
        }
      } catch (InterruptedException e) {
        // TODO(user): replace this when there is a better way to spawn threads in tests and
        // have threads propagate their errors back to the test thread.
        e.printStackTrace();
        // never returns, so that #tearDown() notices that one worker isn't done
        Uninterruptibles.sleepUninterruptibly(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
      }
    }
  }

  private static class Interrupter implements Runnable {
    final Thread threadToInterrupt;

    Interrupter(Thread threadToInterrupt) {
      this.threadToInterrupt = threadToInterrupt;
    }

    @Override public void run() {
      try {
        Thread.sleep(100);
      } catch (InterruptedException e) {
        throw new AssertionError();
      } finally {
        threadToInterrupt.interrupt();
      }
    }
  }
}
