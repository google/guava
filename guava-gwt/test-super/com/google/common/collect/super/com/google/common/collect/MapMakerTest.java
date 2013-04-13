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

import static java.util.concurrent.TimeUnit.HOURS;

import com.google.common.annotations.GwtCompatible;
import com.google.common.base.Function;

import junit.framework.TestCase;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @author Charles Fry
 */
@GwtCompatible(emulated = true)
public class MapMakerTest extends TestCase {

  // "Basher tests", where we throw a bunch of stuff at a Cache and check basic invariants.

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

    @SuppressWarnings("deprecation") // test of deprecated method
    public void testExpiration_setTwice() {
      MapMaker maker = new MapMaker().expireAfterWrite(1, HOURS);
      try {
        // even to the same value is not allowed
        maker.expireAfterWrite(1, HOURS);
        fail();
      } catch (IllegalStateException expected) {
      }
    }

    public void testMaximumSize_setTwice() {
      MapMaker maker = new MapMaker().maximumSize(16);
      try {
        // even to the same value is not allowed
        maker.maximumSize(16);
        fail();
      } catch (IllegalStateException expected) {
      }
    }

    public void testReturnsPlainConcurrentHashMapWhenPossible() {
      Map<?, ?> map = new MapMaker()
          .initialCapacity(5)
          .makeMap();
      assertTrue(map instanceof ConcurrentHashMap);
    }
  }

  /** Tests of the built map with maximumSize. */
  public static class MaximumSizeTest extends TestCase {
    public void testPut_sizeIsZero() {
      ConcurrentMap<Object, Object> map =
          new MapMaker().maximumSize(0).makeMap();
      assertEquals(0, map.size());
      map.put(new Object(), new Object());
      assertEquals(0, map.size());
    }

    public void testSizeBasedEviction() {
      int numKeys = 10;
      int mapSize = 5;
      ConcurrentMap<Object, Object> map =
          new MapMaker().maximumSize(mapSize).makeMap();
      for (int i = 0; i < numKeys; i++) {
        map.put(i, i);
      }
      assertEquals(mapSize, map.size());
      for (int i = numKeys - mapSize; i < mapSize; i++) {
        assertTrue(map.containsKey(i));
      }
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

