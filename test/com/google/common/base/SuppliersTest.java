/*
 * Copyright (C) 2007 Google Inc.
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

package com.google.common.base;

import static com.google.testing.util.SerializableTester.reserialize;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.collect.Lists;
import com.google.testing.util.NullPointerTester;

import junit.framework.TestCase;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Tests com.google.common.base.Suppliers.
 *
 * @author Laurence Gonsalves
 * @author Harry Heymann
 */
@GwtCompatible(emulated = true)
public class SuppliersTest extends TestCase {
  public void testCompose() {
    Supplier<Integer> fiveSupplier = new Supplier<Integer>() {
      public Integer get() {
        return 5;
      }
    };

    Function<Number,Integer> intValueFunction =
        new Function<Number,Integer>() {
          public Integer apply(Number x) {
            return x.intValue();
          }
        };

    Supplier<Integer> squareSupplier = Suppliers.compose(intValueFunction,
        fiveSupplier);

    assertEquals(Integer.valueOf(5), squareSupplier.get());
  }

  public void testComposeWithLists() {
    Supplier<ArrayList<Integer>> listSupplier
        = new Supplier<ArrayList<Integer>>() {
      public ArrayList<Integer> get() {
        return Lists.newArrayList(0);
      }
    };

    Function<List<Integer>, List<Integer>> addElementFunction =
        new Function<List<Integer>, List<Integer>>() {
          public List<Integer> apply(List<Integer> list) {
            ArrayList<Integer> result = Lists.newArrayList(list);
            result.add(1);
            return result;
          }
        };

    Supplier<List<Integer>> addSupplier = Suppliers.compose(addElementFunction,
        listSupplier);

    List<Integer> result = addSupplier.get();
    assertEquals(Integer.valueOf(0), result.get(0));
    assertEquals(Integer.valueOf(1), result.get(1));
  }

  static class CountingSupplier implements Supplier<Integer>, Serializable {
    transient int calls = 0;
    public Integer get() {
      calls++;
      return calls * 10;
    }
  }

  public void testMemoize() {
    CountingSupplier countingSupplier = new CountingSupplier();
    Supplier<Integer> memoizedSupplier = Suppliers.memoize(countingSupplier);
    checkMemoize(countingSupplier, memoizedSupplier);
  }

  @GwtIncompatible("SerializableTester")
  public void testMemoizeSerialized() {
    CountingSupplier countingSupplier = new CountingSupplier();
    Supplier<Integer> memoizedSupplier = Suppliers.memoize(countingSupplier);
    checkMemoize(countingSupplier, memoizedSupplier);
    // Calls to the original memoized supplier shouldn't affect its copy.
    memoizedSupplier.get();

    Supplier<Integer> copy = reserialize(memoizedSupplier);
    memoizedSupplier.get();

    CountingSupplier countingCopy = (CountingSupplier)
        ((Suppliers.MemoizingSupplier<Integer>) copy).delegate;
    checkMemoize(countingCopy, copy);
  }

  private void checkMemoize(
      CountingSupplier countingSupplier, Supplier<Integer> memoizedSupplier) {
    // the underlying supplier hasn't executed yet
    assertEquals(0, countingSupplier.calls);

    assertEquals(10, (int) memoizedSupplier.get());

    // now it has
    assertEquals(1, countingSupplier.calls);

    assertEquals(10, (int) memoizedSupplier.get());

    // it still should only have executed once due to memoization
    assertEquals(1, countingSupplier.calls);
  }

  public void testMemoizeExceptionThrown() {
    Supplier<Integer> exceptingSupplier = new Supplier<Integer>() {
      public Integer get() {
        throw new NullPointerException();
      }
    };

    Supplier<Integer> memoizedSupplier = Suppliers.memoize(exceptingSupplier);

    // call get() twice to make sure that memoization doesn't interfere
    // with throwing the exception
    for (int i = 0; i < 2; i++) {
      try {
        memoizedSupplier.get();
        fail("failed to throw NullPointerException");
      } catch (NullPointerException e) {
        // this is what should happen
      }
    }
  }

  @GwtIncompatible("Thread.sleep")
  public void testMemoizeWithExpiration() throws InterruptedException {
    CountingSupplier countingSupplier = new CountingSupplier();

    Supplier<Integer> memoizedSupplier = Suppliers.memoizeWithExpiration(
        countingSupplier, 75, TimeUnit.MILLISECONDS);

    checkExpiration(countingSupplier, memoizedSupplier);
  }

  @GwtIncompatible("Thread.sleep, SerializationTester")
  public void testMemoizeWithExpirationSerialized()
      throws InterruptedException {
    CountingSupplier countingSupplier = new CountingSupplier();

    Supplier<Integer> memoizedSupplier = Suppliers.memoizeWithExpiration(
        countingSupplier, 75, TimeUnit.MILLISECONDS);
    // Calls to the original memoized supplier shouldn't affect its copy.
    memoizedSupplier.get();

    Supplier<Integer> copy = reserialize(memoizedSupplier);
    memoizedSupplier.get();

    CountingSupplier countingCopy = (CountingSupplier)
        ((Suppliers.ExpiringMemoizingSupplier<Integer>) copy).delegate;
    checkExpiration(countingCopy, copy);
  }

  @GwtIncompatible("Thread.sleep")
  private void checkExpiration(
      CountingSupplier countingSupplier, Supplier<Integer> memoizedSupplier)
      throws InterruptedException {
    // the underlying supplier hasn't executed yet
    assertEquals(0, countingSupplier.calls);

    assertEquals(10, (int) memoizedSupplier.get());
    // now it has
    assertEquals(1, countingSupplier.calls);

    assertEquals(10, (int) memoizedSupplier.get());
    // it still should only have executed once due to memoization
    assertEquals(1, countingSupplier.calls);

    Thread.sleep(150);

    assertEquals(20, (int) memoizedSupplier.get());
    // old value expired
    assertEquals(2, countingSupplier.calls);

    assertEquals(20, (int) memoizedSupplier.get());
    // it still should only have executed twice due to memoization
    assertEquals(2, countingSupplier.calls);
  }

  public void testOfInstanceSuppliesSameInstance() {
    Object toBeSupplied = new Object();
    Supplier<Object> objectSupplier = Suppliers.ofInstance(toBeSupplied);
    assertSame(toBeSupplied,objectSupplier.get());
    assertSame(toBeSupplied,objectSupplier.get()); // idempotent
  }

  public void testOfInstanceSuppliesNull() {
    Supplier<Integer> nullSupplier = Suppliers.ofInstance(null);
    assertNull(nullSupplier.get());
  }

  @GwtIncompatible("Thread")
  public void testThreadSafe() throws InterruptedException {
    final Supplier<Integer> nonThreadSafe = new Supplier<Integer>() {
      int counter = 0;
      public Integer get() {
        int nextValue = counter + 1;
        Thread.yield();
        counter = nextValue;
        return counter;
      }
    };

    final int numThreads = 10;
    final int iterations = 1000;
    Thread[] threads = new Thread[numThreads];
    for (int i = 0; i < numThreads; i++) {
      threads[i] = new Thread() {
        @Override
        public void run() {
          for (int j = 0; j < iterations; j++) {
            Suppliers.synchronizedSupplier(nonThreadSafe).get();
          }
        }
      };
    }
    for (Thread t : threads) {
      t.start();
    }
    for (Thread t : threads) {
      t.join();
    }

    assertEquals(new Integer(numThreads * iterations + 1), nonThreadSafe.get());
  }

  @GwtIncompatible("SerializationTester")
  public void testSerialization() {
    assertEquals(
        Integer.valueOf(5), reserialize(Suppliers.ofInstance(5)).get());
    assertEquals(Integer.valueOf(5), reserialize(Suppliers.compose(
        Functions.identity(), Suppliers.ofInstance(5))).get());
    assertEquals(Integer.valueOf(5),
        reserialize(Suppliers.memoize(Suppliers.ofInstance(5))).get());
    assertEquals(Integer.valueOf(5),
        reserialize(Suppliers.memoizeWithExpiration(
            Suppliers.ofInstance(5), 30, TimeUnit.MINUTES)).get());
    assertEquals(Integer.valueOf(5), reserialize(
        Suppliers.synchronizedSupplier(Suppliers.ofInstance(5))).get());
  }

  @GwtIncompatible("NullPointerTest")
  public void testNullPointers() throws Exception {
    NullPointerTester tester = new NullPointerTester();
    tester.testAllPublicStaticMethods(Suppliers.class);
  }
}
