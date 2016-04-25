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

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.base.Function;
import com.google.common.testing.NullPointerTester;

import junit.framework.TestCase;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;

/**
 * @author Charles Fry
 */
@GwtCompatible(emulated = true)
public class MapMakerTest extends TestCase {

  @GwtIncompatible // NullPointerTester
  public void testNullParameters() throws Exception {
    NullPointerTester tester = new NullPointerTester();
    tester.testAllPublicInstanceMethods(new MapMaker());
  }

  @GwtIncompatible // threads

  public void testRemovalNotification_clear() throws InterruptedException {
    // If a clear() happens while a computation is pending, we should not get a removal
    // notification.

    final CountDownLatch computingLatch = new CountDownLatch(1);
    Function<String, String> computingFunction = new DelayingIdentityLoader<String>(computingLatch);

    @SuppressWarnings("deprecation") // test of deprecated code
    final ConcurrentMap<String, String> map = new MapMaker()
        .concurrencyLevel(1)
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

    assertEquals(1, map.size());
    assertEquals("b", map.get("b"));
  }

  @GwtIncompatible // threads
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

  /*
   * TODO(cpovirk): eliminate duplication between these tests and those in LegacyMapMakerTests and
   * anywhere else
   */

  /** Tests for the builder. */
  public static class MakerTest extends TestCase {
    public void testInitialCapacity_negative() {
      MapMaker maker = new MapMaker();
      try {
        maker.initialCapacity(-1);
        fail();
      } catch (IllegalArgumentException expected) {
      }
    }

    // TODO(cpovirk): enable when ready
    public void xtestInitialCapacity_setTwice() {
      MapMaker maker = new MapMaker().initialCapacity(16);
      try {
        // even to the same value is not allowed
        maker.initialCapacity(16);
        fail();
      } catch (IllegalArgumentException expected) {
      }
    }

    public void testReturnsPlainConcurrentHashMapWhenPossible() {
      Map<?, ?> map = new MapMaker()
          .initialCapacity(5)
          .makeMap();
      assertTrue(map instanceof ConcurrentHashMap);
    }
  }

  /** Tests for recursive computation. */
  public static class RecursiveComputationTest extends TestCase {
    Function<Integer, String> recursiveComputer
        = new Function<Integer, String>() {
      @Override
      public String apply(Integer key) {
        if (key > 0) {
          return key + ", " + recursiveMap.get(key - 1);
        } else {
          return "0";
        }
      }
    };

    ConcurrentMap<Integer, String> recursiveMap = new MapMaker()
        .makeComputingMap(recursiveComputer);

    public void testRecursiveComputation() {
      assertEquals("3, 2, 1, 0", recursiveMap.get(3));
    }
  }

  /**
   * Tests for computing functionality.
   */
  public static class ComputingTest extends TestCase {
    public void testComputerThatReturnsNull() {
      ConcurrentMap<Integer, String> map = new MapMaker()
          .makeComputingMap(new Function<Integer, String>() {
            @Override
            public String apply(Integer key) {
              return null;
            }
          });
      try {
        map.get(1);
        fail();
      } catch (NullPointerException e) { /* expected */ }
    }

    public void testRuntimeException() {
      final RuntimeException e = new RuntimeException();

      ConcurrentMap<Object, Object> map = new MapMaker().makeComputingMap(
          new Function<Object, Object>() {
        @Override
        public Object apply(Object from) {
          throw e;
        }
      });

      try {
        map.get(new Object());
        fail();
      } catch (ComputationException ce) {
        assertSame(e, ce.getCause());
      }
    }
  }
}
