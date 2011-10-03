/*
 * Copyright (C) 2011 The Guava Authors
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

package com.google.common.cache;

import static com.google.common.cache.TestingCacheLoaders.constantLoader;
import static com.google.common.cache.TestingCacheLoaders.errorLoader;
import static com.google.common.cache.TestingCacheLoaders.exceptionLoader;
import static com.google.common.cache.TestingRemovalListeners.countingRemovalListener;

import com.google.common.cache.TestingCacheLoaders.CountingLoader;
import com.google.common.cache.TestingRemovalListeners.CountingRemovalListener;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.ExecutionError;
import com.google.common.util.concurrent.UncheckedExecutionException;

import junit.framework.TestCase;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.List;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReferenceArray;

/**
 * Tests relating to cache computation: concurrent computation, exceptions in computation, etc.
 *
 * @author mike nonemacher
 */
public class CacheComputationTest extends TestCase {
  public void testComputerThatReturnsNull_compute() throws ExecutionException {
    Cache<Object, Object> cache = CacheBuilder.newBuilder().build(constantLoader(null));
    CacheStats stats = cache.stats();
    assertEquals(0, stats.missCount());
    assertEquals(0, stats.loadCount());

    try {
      cache.get(new Object());
      fail();
    } catch (NullPointerException expected) {}
    stats = cache.stats();
    assertEquals(1, stats.missCount());
    assertEquals(1, stats.loadCount());

    try {
      cache.getUnchecked(new Object());
      fail();
    } catch (NullPointerException expected) {}
    stats = cache.stats();
    assertEquals(2, stats.missCount());
    assertEquals(2, stats.loadCount());

    try {
      cache.refresh(new Object());
      fail();
    } catch (NullPointerException expected) {}
    stats = cache.stats();
    assertEquals(2, stats.missCount());
    assertEquals(3, stats.loadCount());
  }

  public void testComputeError() throws ExecutionException {
    Error e = new Error();
    CacheLoader<Object, Object> loader = errorLoader(e);
    Cache<Object, Object> cache = CacheBuilder.newBuilder().build(loader);
    CacheStats stats = cache.stats();
    assertEquals(0, stats.missCount());
    assertEquals(0, stats.loadSuccessCount());
    assertEquals(0, stats.loadExceptionCount());

    try {
      cache.get(new Object());
      fail();
    } catch (ExecutionError expected) {
      assertSame(e, expected.getCause());
    }
    stats = cache.stats();
    assertEquals(1, stats.missCount());
    assertEquals(0, stats.loadSuccessCount());
    assertEquals(1, stats.loadExceptionCount());

    try {
      cache.getUnchecked(new Object());
      fail();
    } catch (ExecutionError expected) {
      assertSame(e, expected.getCause());
    }
    stats = cache.stats();
    assertEquals(2, stats.missCount());
    assertEquals(0, stats.loadSuccessCount());
    assertEquals(2, stats.loadExceptionCount());

    try {
      cache.refresh(new Object());
      fail();
    } catch (ExecutionError expected) {
      assertSame(e, expected.getCause());
    }
    stats = cache.stats();
    assertEquals(2, stats.missCount());
    assertEquals(0, stats.loadSuccessCount());
    assertEquals(3, stats.loadExceptionCount());
  }

  public void testComputeCheckedException() {
    Exception e = new Exception();
    CacheLoader<Object, Object> loader = exceptionLoader(e);
    Cache<Object, Object> cache = CacheBuilder.newBuilder().build(loader);
    CacheStats stats = cache.stats();
    assertEquals(0, stats.missCount());
    assertEquals(0, stats.loadSuccessCount());
    assertEquals(0, stats.loadExceptionCount());

    try {
      cache.get(new Object());
      fail();
    } catch (ExecutionException expected) {
      assertSame(e, expected.getCause());
    }
    stats = cache.stats();
    assertEquals(1, stats.missCount());
    assertEquals(0, stats.loadSuccessCount());
    assertEquals(1, stats.loadExceptionCount());

    try {
      cache.getUnchecked(new Object());
      fail();
    } catch (UncheckedExecutionException expected) {
      assertSame(e, expected.getCause());
    }
    stats = cache.stats();
    assertEquals(2, stats.missCount());
    assertEquals(0, stats.loadSuccessCount());
    assertEquals(2, stats.loadExceptionCount());

    try {
      cache.refresh(new Object());
      fail();
    } catch (ExecutionException expected) {
      assertSame(e, expected.getCause());
    }
    stats = cache.stats();
    assertEquals(2, stats.missCount());
    assertEquals(0, stats.loadSuccessCount());
    assertEquals(3, stats.loadExceptionCount());
  }

  public void testComputeUncheckedException() throws ExecutionException {
    Exception e = new RuntimeException();
    CacheLoader<Object, Object> loader = exceptionLoader(e);
    Cache<Object, Object> cache = CacheBuilder.newBuilder().build(loader);
    CacheStats stats = cache.stats();
    assertEquals(0, stats.missCount());
    assertEquals(0, stats.loadSuccessCount());
    assertEquals(0, stats.loadExceptionCount());

    try {
      cache.get(new Object());
      fail();
    } catch (UncheckedExecutionException expected) {
      assertSame(e, expected.getCause());
    }
    stats = cache.stats();
    assertEquals(1, stats.missCount());
    assertEquals(0, stats.loadSuccessCount());
    assertEquals(1, stats.loadExceptionCount());

    try {
      cache.getUnchecked(new Object());
      fail();
    } catch (UncheckedExecutionException expected) {
      assertSame(e, expected.getCause());
    }
    stats = cache.stats();
    assertEquals(2, stats.missCount());
    assertEquals(0, stats.loadSuccessCount());
    assertEquals(2, stats.loadExceptionCount());

    try {
      cache.refresh(new Object());
      fail();
    } catch (UncheckedExecutionException expected) {
      assertSame(e, expected.getCause());
    }
    stats = cache.stats();
    assertEquals(2, stats.missCount());
    assertEquals(0, stats.loadSuccessCount());
    assertEquals(3, stats.loadExceptionCount());
  }

  public void testRecomputeAfterFailure() throws ExecutionException {
    final AtomicInteger count = new AtomicInteger();
    CacheLoader<Integer, String> failOnceFunction = new CacheLoader<Integer, String>() {

      @Override
      public String load(Integer key) {
        if (count.getAndIncrement() == 0) {
          throw new IllegalStateException("exception to trigger failure on first load()");
        }
        return key.toString();
      }
    };
    CountingRemovalListener<Integer, String> removalListener = countingRemovalListener();
    Cache<Integer, String> cache = CacheBuilder.newBuilder()
        .weakValues()
        .removalListener(removalListener)
        .build(failOnceFunction);

    try {
      cache.getUnchecked(1);
      fail();
    } catch (UncheckedExecutionException e) {
      // expected
    }

    assertEquals("1", cache.getUnchecked(1));
    assertEquals(0, removalListener.getCount());

    count.set(0);
    try {
      cache.refresh(2);
      fail();
    } catch (UncheckedExecutionException e) {
      // expected
    }

    assertEquals("2", cache.getUnchecked(2));
    assertEquals(0, removalListener.getCount());

  }

  public void testRecomputeAfterValueReclamation() throws InterruptedException, ExecutionException {
    CountingLoader countingLoader = new CountingLoader();
    Cache<Object, Object> cache = CacheBuilder.newBuilder().weakValues().build(countingLoader);
    ConcurrentMap<Object, Object> map = cache.asMap();

    int iterations = 10;
    WeakReference<Object> ref = new WeakReference<Object>(null);
    int expectedComputations = 0;
    for (int i = 0; i < iterations; i++) {
      // The entry should get garbage collected and recomputed.
      Object oldValue = ref.get();
      if (oldValue == null) {
        expectedComputations++;
      }
      ref = new WeakReference<Object>(cache.getUnchecked(1));
      oldValue = null;
      Thread.sleep(i);
      System.gc();
    }
    assertEquals(expectedComputations, countingLoader.getCount());

    for (int i = 0; i < iterations; i++) {
      // The entry should get garbage collected and recomputed.
      Object oldValue = ref.get();
      if (oldValue == null) {
        expectedComputations++;
      }
      cache.refresh(1);
      ref = new WeakReference<Object>(map.get(1));
      oldValue = null;
      Thread.sleep(i);
      System.gc();
    }
    assertEquals(expectedComputations, countingLoader.getCount());
  }

  public void testRecomputeAfterSimulatedValueReclamation() throws ExecutionException {
    CountingLoader countingLoader = new CountingLoader();
    Cache<Object, Object> cache = CacheBuilder.newBuilder()
        .concurrencyLevel(1)
        .weakValues()
        .build(countingLoader);

    Object key = new Object();
    assertNotNull(cache.getUnchecked(key));

    CacheTesting.simulateValueReclamation(cache, key);

    // this blocks if computation can't deal with partially-collected values
    assertNotNull(cache.getUnchecked(key));
    assertEquals(1, cache.size());
    assertEquals(2, countingLoader.getCount());

    CacheTesting.simulateValueReclamation(cache, key);
    cache.refresh(key);
    assertEquals(1, cache.size());
    assertEquals(3, countingLoader.getCount());
  }

  public void testRecomputeAfterSimulatedKeyReclamation() throws ExecutionException {
    CountingLoader countingLoader = new CountingLoader();
    Cache<Object, Object> cache = CacheBuilder.newBuilder()
        .concurrencyLevel(1)
        .weakKeys()
        .build(countingLoader);

    Object key = new Object();
    assertNotNull(cache.getUnchecked(key));
    assertEquals(1, cache.size());

    CacheTesting.simulateKeyReclamation(cache, key);

    // this blocks if computation can't deal with partially-collected values
    assertNotNull(cache.getUnchecked(key));
    assertEquals(2, countingLoader.getCount());

    CacheTesting.simulateKeyReclamation(cache, key);
    cache.refresh(key);
    assertEquals(3, countingLoader.getCount());
  }

  /**
   * Make sure Cache correctly wraps ExecutionExceptions and UncheckedExecutionExceptions.
   */
  public void testComputationExceptionWithCause() {
    final Exception cause = new Exception();
    final UncheckedExecutionException uee = new UncheckedExecutionException(cause);
    final ExecutionException ee = new ExecutionException(cause);

    Cache<Object, Object> cacheUnchecked =
        CacheBuilder.newBuilder().build(exceptionLoader(uee));
    Cache<Object, Object> cacheChecked =
        CacheBuilder.newBuilder().build(exceptionLoader(ee));

    try {
      cacheUnchecked.get(new Object());
      fail();
    } catch (ExecutionException e) {
      fail();
    } catch (UncheckedExecutionException caughtEe) {
      assertSame(uee, caughtEe.getCause());
    }

    try {
      cacheUnchecked.getUnchecked(new Object());
      fail();
    } catch (UncheckedExecutionException caughtUee) {
      assertSame(uee, caughtUee.getCause());
    }

    try {
      cacheUnchecked.refresh(new Object());
      fail();
    } catch (ExecutionException e) {
      fail();
    } catch (UncheckedExecutionException caughtEe) {
      assertSame(uee, caughtEe.getCause());
    }

    try {
      cacheChecked.get(new Object());
      fail();
    } catch (ExecutionException caughtEe) {
      assertSame(ee, caughtEe.getCause());
    }

    try {
      cacheChecked.getUnchecked(new Object());
      fail();
    } catch (UncheckedExecutionException caughtUee) {
      assertSame(ee, caughtUee.getCause());
    }
    try {
      cacheChecked.refresh(new Object());
      fail();
    } catch (ExecutionException caughtEe) {
      assertSame(ee, caughtEe.getCause());
    }

  }

  public void testConcurrentComputation() throws InterruptedException {
    testConcurrentComputation(CacheBuilder.newBuilder());
  }

  public void testConcurrentExpirationComputation() throws InterruptedException {
    testConcurrentComputation(CacheBuilder.newBuilder().expireAfterWrite(10, TimeUnit.SECONDS));
  }

  private static void testConcurrentComputation(CacheBuilder<Object, Object> builder)
      throws InterruptedException {
    testConcurrentComputationDefault(builder);
    testConcurrentComputationNull(builder);
    testConcurrentComputationUncheckedException(builder);
    testConcurrentComputationCheckedException(builder);
  }

  /**
   * On a successful concurrent computation, only one thread does the work, but all the threads get
   * the same result.
   */
  private static void testConcurrentComputationDefault(CacheBuilder<Object, Object> builder)
      throws InterruptedException {

    int count = 10;
    final AtomicInteger callCount = new AtomicInteger();
    final CountDownLatch startSignal = new CountDownLatch(count + 1);
    final Object result = new Object();

    Cache<String, Object> cache = builder.build(
        new CacheLoader<String, Object>() {
          @Override public Object load(String key) throws InterruptedException {
            callCount.incrementAndGet();
            startSignal.await();
            return result;
          }
        });

    List<Object> resultArray = doConcurrentGet(cache, "bar", count, startSignal);

    assertEquals(1, callCount.get());
    for (int i = 0; i < count; i++) {
      assertSame("result(" + i + ") didn't match expected", result, resultArray.get(i));
    }
  }

  /**
   * On a concurrent computation that returns null, all threads should get a NullPointerException,
   * with the loader only called once. The result should not be cached (a later request should call
   * the loader again).
   */
  private static void testConcurrentComputationNull(CacheBuilder<Object, Object> builder)
      throws InterruptedException {

    int count = 10;
    final AtomicInteger callCount = new AtomicInteger();
    final CountDownLatch startSignal = new CountDownLatch(count + 1);

    Cache<String, String> cache = builder.build(
        new CacheLoader<String, String>() {
          @Override public String load(String key) throws InterruptedException {
            callCount.incrementAndGet();
            startSignal.await();
            return null;
          }
        });

    List<Object> result = doConcurrentGet(cache, "bar", count, startSignal);

    assertEquals(1, callCount.get());
    for (int i = 0; i < count; i++) {
      assertTrue(result.get(i) instanceof NullPointerException);
    }

    // subsequent calls should call the loader again, not get the old exception
    try {
      cache.getUnchecked("bar");
      fail();
    } catch (NullPointerException expected) {
    }
    assertEquals(2, callCount.get());
  }

  /**
   * On a concurrent computation that throws an unchecked exception, all threads should get the
   * (wrapped) exception, with the loader called only once. The result should not be cached (a later
   * request should call the loader again).
   */
  private static void testConcurrentComputationUncheckedException(
      CacheBuilder<Object, Object> builder) throws InterruptedException {

    int count = 10;
    final AtomicInteger callCount = new AtomicInteger();
    final CountDownLatch startSignal = new CountDownLatch(count + 1);
    final RuntimeException e = new RuntimeException();

    Cache<String, String> cache = builder.build(
        new CacheLoader<String, String>() {
          @Override public String load(String key) throws InterruptedException {
            callCount.incrementAndGet();
            startSignal.await();
            throw e;
          }
        });

    List<Object> result = doConcurrentGet(cache, "bar", count, startSignal);

    assertEquals(1, callCount.get());
    for (int i = 0; i < count; i++) {
      // doConcurrentGet alternates between calling getUnchecked and calling get, but an unchecked
      // exception thrown by the loader is always wrapped as an UncheckedExecutionException.
      assertTrue(result.get(i) instanceof UncheckedExecutionException);
      assertSame(e, ((UncheckedExecutionException) result.get(i)).getCause());
    }

    // subsequent calls should call the loader again, not get the old exception
    try {
      cache.getUnchecked("bar");
      fail();
    } catch (UncheckedExecutionException expected) {
    }
    assertEquals(2, callCount.get());
  }

  /**
   * On a concurrent computation that throws a checked exception, all threads should get the
   * (wrapped) exception, with the loader called only once. The result should not be cached (a later
   * request should call the loader again).
   */
  private static void testConcurrentComputationCheckedException(
      CacheBuilder<Object, Object> builder) throws InterruptedException {

    int count = 10;
    final AtomicInteger callCount = new AtomicInteger();
    final CountDownLatch startSignal = new CountDownLatch(count + 1);
    final IOException e = new IOException();

    Cache<String, String> cache = builder.build(
        new CacheLoader<String, String>() {
          @Override public String load(String key) throws IOException, InterruptedException {
            callCount.incrementAndGet();
            startSignal.await();
            throw e;
          }
        });

    List<Object> result = doConcurrentGet(cache, "bar", count, startSignal);

    assertEquals(1, callCount.get());
    for (int i = 0; i < count; i++) {
      // doConcurrentGet alternates between calling getUnchecked and calling get. If we call get(),
      // we should get an ExecutionException; if we call getUnchecked(), we should get an
      // UncheckedExecutionException.
      int mod = i % 3;
      if (mod == 0 || mod == 2) {
        assertTrue(result.get(i) instanceof ExecutionException);
        assertSame(e, ((ExecutionException) result.get(i)).getCause());
      } else {
        assertTrue(result.get(i) instanceof UncheckedExecutionException);
        assertSame(e, ((UncheckedExecutionException) result.get(i)).getCause());
      }
    }

    // subsequent calls should call the loader again, not get the old exception
    try {
      cache.getUnchecked("bar");
      fail();
    } catch (UncheckedExecutionException expected) {
    }
    assertEquals(2, callCount.get());
  }

  /**
   * Test-helper method that performs {@code nThreads} concurrent calls to {@code cache.get(key)}
   * or {@code cache.getUnchecked(key)}, and returns a List containing each of the results. The
   * result for any given call to {@code cache.get} or {@code cache.getUnchecked} is the value
   * returned, or the exception thrown.
   *
   * <p>As we iterate from {@code 0} to {@code nThreads}, threads with an even index will call
   * {@code getUnchecked}, and threads with an odd index will call {@code get}. If the cache throws
   * exceptions, this difference may be visible in the returned List.
   */
  private static <K> List<Object> doConcurrentGet(final Cache<K, ?> cache, final K key,
      int nThreads, final CountDownLatch gettersStartedSignal) throws InterruptedException {

    final AtomicReferenceArray<Object> result = new AtomicReferenceArray<Object>(nThreads);
    final CountDownLatch gettersComplete = new CountDownLatch(nThreads);
    for (int i = 0; i < nThreads; i++) {
      final int index = i;
      Thread thread = new Thread(new Runnable() {
        @Override public void run() {
          gettersStartedSignal.countDown();
          Object value = null;
          try {
            int mod = index % 3;
            if (mod == 0) {
              value = cache.get(key);
            } else if (mod == 1) {
              value = cache.getUnchecked(key);
            } else {
              cache.refresh(key);
              value = cache.get(key);
            }
            result.set(index, value);
          } catch (Throwable t) {
            result.set(index, t);
          }
          gettersComplete.countDown();
        }
      });
      thread.start();
      // we want to wait until each thread is WAITING - one thread waiting inside CacheLoader.load
      // (in startSignal.await()), and the others waiting for that thread's result.
      while (thread.isAlive() && thread.getState() != Thread.State.WAITING) {
        Thread.yield();
      }
    }
    gettersStartedSignal.countDown();
    gettersComplete.await();

    List<Object> resultList = Lists.newArrayListWithExpectedSize(nThreads);
    for (int i = 0; i < nThreads; i++) {
      resultList.add(result.get(i));
    }
    return resultList;
  }

  public void testAsMapDuringComputation() throws InterruptedException, ExecutionException {
    final CountDownLatch getStartedSignal = new CountDownLatch(2);
    final CountDownLatch letGetFinishSignal = new CountDownLatch(1);
    final CountDownLatch getFinishedSignal = new CountDownLatch(2);
    final String getKey = "get";
    final String refreshKey = "refresh";
    final String suffix = "Suffix";

    CacheLoader<String, String> computeFunction = new CacheLoader<String, String>() {
      @Override
      public String load(String key) throws InterruptedException {
        getStartedSignal.countDown();
        letGetFinishSignal.await();
        return key + suffix;
      }
    };

    final Cache<String, String> cache = CacheBuilder.newBuilder().build(computeFunction);
    ConcurrentMap<String,String> map = cache.asMap();
    map.put(refreshKey, refreshKey);
    assertEquals(1, map.size());
    assertFalse(map.containsKey(getKey));
    assertSame(refreshKey, map.get(refreshKey));

    new Thread() {
      @Override
      public void run() {
        cache.getUnchecked(getKey);
        getFinishedSignal.countDown();
      }
    }.start();
    new Thread() {
      @Override
      public void run() {
        try {
          cache.refresh(refreshKey);
        } catch (ExecutionException e) {
          throw new UncheckedExecutionException(e);
        }
        getFinishedSignal.countDown();
      }
    }.start();

    getStartedSignal.await();

    // computation is in progress; asMap shouldn't have changed
    assertEquals(1, map.size());
    assertFalse(map.containsKey(getKey));
    assertSame(refreshKey, map.get(refreshKey));

    // let computation complete
    letGetFinishSignal.countDown();
    getFinishedSignal.await();

    // asMap view should have been updated
    assertEquals(2, cache.size());
    assertEquals(getKey + suffix, map.get(getKey));
    assertEquals(refreshKey + suffix, map.get(refreshKey));
  }

  public void testInvalidateDuringComputation() throws InterruptedException, ExecutionException {
    // computation starts; invalidate() is called on the key being computed, computation finishes
    final CountDownLatch computationStarted = new CountDownLatch(2);
    final CountDownLatch letGetFinishSignal = new CountDownLatch(1);
    final CountDownLatch getFinishedSignal = new CountDownLatch(2);
    final String getKey = "get";
    final String refreshKey = "refresh";
    final String suffix = "Suffix";

    CacheLoader<String, String> computeFunction = new CacheLoader<String, String>() {
      @Override
      public String load(String key) throws InterruptedException {
        computationStarted.countDown();
        letGetFinishSignal.await();
        return key + suffix;
      }
    };

    final Cache<String, String> cache = CacheBuilder.newBuilder().build(computeFunction);
    ConcurrentMap<String,String> map = cache.asMap();
    map.put(refreshKey, refreshKey);

    new Thread() {
      @Override
      public void run() {
        cache.getUnchecked(getKey);
        getFinishedSignal.countDown();
      }
    }.start();
    new Thread() {
      @Override
      public void run() {
        try {
          cache.refresh(refreshKey);
        } catch (ExecutionException e) {
          throw new UncheckedExecutionException(e);
        }
        getFinishedSignal.countDown();
      }
    }.start();

    computationStarted.await();
    cache.invalidate(getKey);
    cache.invalidate(refreshKey);
    assertFalse(map.containsKey(getKey));
    assertFalse(map.containsKey(refreshKey));

    // let computation complete
    letGetFinishSignal.countDown();
    getFinishedSignal.await();

    // results should be visible
    assertEquals(2, cache.size());
    assertEquals(getKey + suffix, map.get(getKey));
    assertEquals(refreshKey + suffix, map.get(refreshKey));
  }

  public void testExpandDuringComputation() throws InterruptedException {
    final int count = 3;
    final AtomicInteger callCount = new AtomicInteger();
    // tells the computing thread when to start computing
    final CountDownLatch computeSignal = new CountDownLatch(1);
    // tells the main thread when computation is pending
    final CountDownLatch secondSignal = new CountDownLatch(1);
    // tells the main thread when the second get has started
    final CountDownLatch thirdSignal = new CountDownLatch(1);
    // tells the main thread when the third get has started
    final CountDownLatch fourthSignal = new CountDownLatch(1);
    // tells the test when all gets have returned
    final CountDownLatch doneSignal = new CountDownLatch(count);

    CacheLoader<String, String> computeFunction = new CacheLoader<String, String>() {
      @Override
      public String load(String key) throws InterruptedException {
        callCount.incrementAndGet();
        secondSignal.countDown();
        computeSignal.await();
        return key + "foo";
      }
    };

    final Cache<String, String> cache = CacheBuilder.newBuilder()
        .weakKeys()
        .build(computeFunction);

    final AtomicReferenceArray<String> result = new AtomicReferenceArray<String>(count);

    final String key = "bar";

    // start computing thread
    new Thread() {
      @Override
      public void run() {
        result.set(0, cache.getUnchecked(key));
        doneSignal.countDown();
      }
    }.start();

    // wait for computation to start
    secondSignal.await();

    // start waiting thread
    new Thread() {
      @Override
      public void run() {
        thirdSignal.countDown();
        result.set(1, cache.getUnchecked(key));
        doneSignal.countDown();
      }
    }.start();

    // give the second get a chance to run; it is okay for this to be racy
    // as the end result should be the same either way
    thirdSignal.await();
    Thread.yield();

    // Expand!
    CacheTesting.forceExpandSegment(cache, key);

    // start another waiting thread
    new Thread() {
      @Override
      public void run() {
        fourthSignal.countDown();
        result.set(2, cache.getUnchecked(key));
        doneSignal.countDown();
      }
    }.start();

    // give the third get a chance to run; it is okay for this to be racy
    // as the end result should be the same either way
    fourthSignal.await();
    Thread.yield();

    // let computation finish
    computeSignal.countDown();
    doneSignal.await();

    assertTrue(callCount.get() == 1);
    assertEquals("barfoo", result.get(0));
    assertEquals("barfoo", result.get(1));
    assertEquals("barfoo", result.get(2));
    assertEquals("barfoo", cache.getUnchecked(key));
  }

  public void testExpandDuringRefresh() throws InterruptedException, ExecutionException {
    final AtomicInteger callCount = new AtomicInteger();
    // tells the computing thread when to start computing
    final CountDownLatch computeSignal = new CountDownLatch(1);
    // tells the main thread when computation is pending
    final CountDownLatch secondSignal = new CountDownLatch(1);
    // tells the main thread when the second get has started
    final CountDownLatch thirdSignal = new CountDownLatch(1);
    // tells the main thread when the third get has started
    final CountDownLatch fourthSignal = new CountDownLatch(1);
    // tells the test when all gets have returned
    final CountDownLatch doneSignal = new CountDownLatch(3);
    final String suffix = "Suffix";

    CacheLoader<String, String> computeFunction = new CacheLoader<String, String>() {
      @Override
      public String load(String key) throws InterruptedException {
        callCount.incrementAndGet();
        secondSignal.countDown();
        computeSignal.await();
        return key + suffix;
      }
    };

    final AtomicReferenceArray<String> result = new AtomicReferenceArray<String>(2);

    final Cache<String, String> cache = CacheBuilder.newBuilder()
        .build(computeFunction);
    final String key = "bar";
    cache.asMap().put(key, key);

    // start computing thread
    new Thread() {
      @Override
      public void run() {
        try {
          cache.refresh(key);
        } catch (ExecutionException e) {
          throw new UncheckedExecutionException(e);
        }
        doneSignal.countDown();
      }
    }.start();

    // wait for computation to start
    secondSignal.await();

    // start waiting thread
    new Thread() {
      @Override
      public void run() {
        thirdSignal.countDown();
        result.set(0, cache.getUnchecked(key));
        doneSignal.countDown();
      }
    }.start();

    // give the second get a chance to run; it is okay for this to be racy
    // as the end result should be the same either way
    thirdSignal.await();
    Thread.yield();

    // Expand!
    CacheTesting.forceExpandSegment(cache, key);

    // start another waiting thread
    new Thread() {
      @Override
      public void run() {
        fourthSignal.countDown();
        result.set(1, cache.getUnchecked(key));
        doneSignal.countDown();
      }
    }.start();

    // give the third get a chance to run; it is okay for this to be racy
    // as the end result should be the same either way
    fourthSignal.await();
    Thread.yield();

    // let computation finish
    computeSignal.countDown();
    doneSignal.await();

    assertTrue(callCount.get() == 1);
    assertEquals(key, result.get(0));
    assertEquals(key, result.get(1));
    assertEquals(key + suffix, cache.getUnchecked(key));
  }
}
