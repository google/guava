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

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.truth.Truth.assertThat;
import static java.lang.Long.MAX_VALUE;
import static java.lang.Thread.currentThread;
import static java.util.concurrent.Executors.newCachedThreadPool;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.NANOSECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;

import com.google.common.base.Stopwatch;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;
import junit.framework.TestCase;

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
        new LinkedBlockingDeque<Object>(),
        new LinkedBlockingDeque<Object>(10),
        new PriorityBlockingQueue<Object>(10, Ordering.arbitrary()));
  }

  /*
   * We need to perform operations in a thread pool, even for simple cases, because the queue might
   * be a SynchronousQueue.
   */
  private ExecutorService threadPool;

  @Override
  public void setUp() {
    threadPool = newCachedThreadPool();
  }

  @Override
  public void tearDown() throws InterruptedException {
    threadPool.shutdown();
    assertTrue("Some worker didn't finish in time", threadPool.awaitTermination(10, SECONDS));
  }

  private static <T> int drain(
      BlockingQueue<T> q,
      Collection<? super T> buffer,
      int maxElements,
      long timeout,
      TimeUnit unit,
      boolean interruptibly)
      throws InterruptedException {
    return interruptibly
        ? Queues.drain(q, buffer, maxElements, timeout, unit)
        : Queues.drainUninterruptibly(q, buffer, maxElements, timeout, unit);
  }

  public void testMultipleProducers() throws Exception {
    for (BlockingQueue<Object> q : blockingQueues()) {
      testMultipleProducers(q);
    }
  }

  private void testMultipleProducers(BlockingQueue<Object> q) throws InterruptedException {
    for (boolean interruptibly : new boolean[] {true, false}) {
      @SuppressWarnings("unused") // https://errorprone.info/bugpattern/FutureReturnValueIgnored
      Future<?> possiblyIgnoredError = threadPool.submit(new Producer(q, 20));
      @SuppressWarnings("unused") // https://errorprone.info/bugpattern/FutureReturnValueIgnored
      Future<?> possiblyIgnoredError1 = threadPool.submit(new Producer(q, 20));
      @SuppressWarnings("unused") // https://errorprone.info/bugpattern/FutureReturnValueIgnored
      Future<?> possiblyIgnoredError2 = threadPool.submit(new Producer(q, 20));
      @SuppressWarnings("unused") // https://errorprone.info/bugpattern/FutureReturnValueIgnored
      Future<?> possiblyIgnoredError3 = threadPool.submit(new Producer(q, 20));
      @SuppressWarnings("unused") // https://errorprone.info/bugpattern/FutureReturnValueIgnored
      Future<?> possiblyIgnoredError4 = threadPool.submit(new Producer(q, 20));

      List<Object> buf = newArrayList();
      int elements = drain(q, buf, 100, MAX_VALUE, NANOSECONDS, interruptibly);
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
    for (boolean interruptibly : new boolean[] {true, false}) {
      assertEquals(0, Queues.drain(q, ImmutableList.of(), 1, 10, MILLISECONDS));

      Producer producer = new Producer(q, 1);
      // producing one, will ask for two
      Future<?> producerThread = threadPool.submit(producer);
      producer.beganProducing.await();

      // make sure we time out
      Stopwatch timer = Stopwatch.createStarted();

      int drained = drain(q, newArrayList(), 2, 10, MILLISECONDS, interruptibly);
      assertThat(drained).isAtMost(1);

      assertThat(timer.elapsed(MILLISECONDS)).isAtLeast(10L);

      // If even the first one wasn't there, clean up so that the next test doesn't see an element.
      producerThread.cancel(true);
      producer.doneProducing.await();
      if (drained == 0) {
        q.poll(); // not necessarily there if producer was interrupted
      }
    }
  }

  public void testZeroElements() throws Exception {
    for (BlockingQueue<Object> q : blockingQueues()) {
      testZeroElements(q);
    }
  }

  private void testZeroElements(BlockingQueue<Object> q) throws InterruptedException {
    for (boolean interruptibly : new boolean[] {true, false}) {
      // asking to drain zero elements
      assertEquals(0, drain(q, ImmutableList.of(), 0, 10, MILLISECONDS, interruptibly));
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
    @SuppressWarnings("unused") // https://errorprone.info/bugpattern/FutureReturnValueIgnored
    Future<?> possiblyIgnoredError = threadPool.submit(new Producer(q, 1));

    List<Object> buf = newArrayList();
    int elements = Queues.drain(q, buf, -1, MAX_VALUE, NANOSECONDS);
    assertEquals(0, elements);
    assertThat(buf).isEmpty();

    // Free the producer thread, and give subsequent tests a clean slate.
    q.take();
  }

  public void testDrain_throws() throws Exception {
    for (BlockingQueue<Object> q : blockingQueues()) {
      testDrain_throws(q);
    }
  }

  private void testDrain_throws(BlockingQueue<Object> q) {
    @SuppressWarnings("unused") // https://errorprone.info/bugpattern/FutureReturnValueIgnored
    Future<?> possiblyIgnoredError = threadPool.submit(new Interrupter(currentThread()));
    try {
      Queues.drain(q, ImmutableList.of(), 100, MAX_VALUE, NANOSECONDS);
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
    final Thread mainThread = currentThread();
    @SuppressWarnings("unused") // https://errorprone.info/bugpattern/FutureReturnValueIgnored
    Future<?> possiblyIgnoredError =
        threadPool.submit(
            new Callable<Void>() {
              @Override
              public Void call() throws InterruptedException {
                new Producer(q, 50).call();
                new Interrupter(mainThread).run();
                new Producer(q, 50).call();
                return null;
              }
            });
    List<Object> buf = newArrayList();
    int elements = Queues.drainUninterruptibly(q, buf, 100, MAX_VALUE, NANOSECONDS);
    // so when this drains all elements, we know the thread has also been interrupted in between
    assertTrue(Thread.interrupted());
    assertEquals(100, elements);
    assertEquals(100, buf.size());
  }

  public void testNewLinkedBlockingDequeCapacity() {
    try {
      Queues.newLinkedBlockingDeque(0);
      fail("Should have thrown IllegalArgumentException");
    } catch (IllegalArgumentException expected) {
      // any capacity less than 1 should throw IllegalArgumentException
    }
    assertEquals(1, Queues.newLinkedBlockingDeque(1).remainingCapacity());
    assertEquals(11, Queues.newLinkedBlockingDeque(11).remainingCapacity());
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

  /** Checks that #drain() invocations behave correctly for a drained (empty) queue. */
  private void assertDrained(BlockingQueue<Object> q) {
    assertNull(q.peek());
    assertInterruptibleDrained(q);
    assertUninterruptibleDrained(q);
  }

  private void assertInterruptibleDrained(BlockingQueue<Object> q) {
    // nothing to drain, thus this should wait doing nothing
    try {
      assertEquals(0, Queues.drain(q, ImmutableList.of(), 0, 10, MILLISECONDS));
    } catch (InterruptedException e) {
      throw new AssertionError();
    }

    // but does the wait actually occurs?
    @SuppressWarnings("unused") // https://errorprone.info/bugpattern/FutureReturnValueIgnored
    Future<?> possiblyIgnoredError = threadPool.submit(new Interrupter(currentThread()));
    try {
      // if waiting works, this should get stuck
      Queues.drain(q, newArrayList(), 1, MAX_VALUE, NANOSECONDS);
      fail();
    } catch (InterruptedException expected) {
      // we indeed waited; a slow thread had enough time to interrupt us
    }
  }

  // same as above; uninterruptible version
  private void assertUninterruptibleDrained(BlockingQueue<Object> q) {
    assertEquals(0, Queues.drainUninterruptibly(q, ImmutableList.of(), 0, 10, MILLISECONDS));

    // but does the wait actually occurs?
    @SuppressWarnings("unused") // https://errorprone.info/bugpattern/FutureReturnValueIgnored
    Future<?> possiblyIgnoredError = threadPool.submit(new Interrupter(currentThread()));

    Stopwatch timer = Stopwatch.createStarted();
    Queues.drainUninterruptibly(q, newArrayList(), 1, 10, MILLISECONDS);
    assertThat(timer.elapsed(MILLISECONDS)).isAtLeast(10L);
    // wait for interrupted status and clear it
    while (!Thread.interrupted()) {
      Thread.yield();
    }
  }

  private static class Producer implements Callable<Void> {
    final BlockingQueue<Object> q;
    final int elements;
    final CountDownLatch beganProducing = new CountDownLatch(1);
    final CountDownLatch doneProducing = new CountDownLatch(1);

    Producer(BlockingQueue<Object> q, int elements) {
      this.q = q;
      this.elements = elements;
    }

    @Override
    public Void call() throws InterruptedException {
      try {
        beganProducing.countDown();
        for (int i = 0; i < elements; i++) {
          q.put(new Object());
        }
        return null;
      } finally {
        doneProducing.countDown();
      }
    }
  }

  private static class Interrupter implements Runnable {
    final Thread threadToInterrupt;

    Interrupter(Thread threadToInterrupt) {
      this.threadToInterrupt = threadToInterrupt;
    }

    @Override
    public void run() {
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
