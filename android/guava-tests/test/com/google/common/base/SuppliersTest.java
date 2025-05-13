/*
 * Copyright (C) 2007 The Guava Authors
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

import static com.google.common.base.ReflectionFreeAssertThrows.assertThrows;
import static com.google.common.testing.SerializableTester.reserialize;
import static com.google.common.truth.Truth.assertThat;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.NANOSECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.annotations.J2ktIncompatible;
import com.google.common.collect.Lists;
import com.google.common.testing.ClassSanityTester;
import com.google.common.testing.EqualsTester;
import java.io.NotSerializableException;
import java.io.Serializable;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import junit.framework.TestCase;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Tests com.google.common.base.Suppliers.
 *
 * @author Laurence Gonsalves
 * @author Harry Heymann
 */
@NullMarked
@GwtCompatible(emulated = true)
public class SuppliersTest extends TestCase {

  static class CountingSupplier implements Supplier<Integer> {
    int calls = 0;

    @Override
    public Integer get() {
      calls++;
      return calls * 10;
    }

    @Override
    public String toString() {
      return "CountingSupplier";
    }
  }

  static class ThrowingSupplier implements Supplier<Integer> {
    @Override
    public Integer get() {
      throw new NullPointerException();
    }
  }

  static class SerializableCountingSupplier extends CountingSupplier implements Serializable {
    @GwtIncompatible @J2ktIncompatible private static final long serialVersionUID = 0L;
  }

  static class SerializableThrowingSupplier extends ThrowingSupplier implements Serializable {
    @GwtIncompatible @J2ktIncompatible private static final long serialVersionUID = 0L;
  }

  static void checkMemoize(CountingSupplier countingSupplier, Supplier<Integer> memoizedSupplier) {
    // the underlying supplier hasn't executed yet
    assertEquals(0, countingSupplier.calls);

    assertEquals(10, (int) memoizedSupplier.get());

    // now it has
    assertEquals(1, countingSupplier.calls);

    assertEquals(10, (int) memoizedSupplier.get());

    // it still should only have executed once due to memoization
    assertEquals(1, countingSupplier.calls);
  }

  public void testMemoize() {
    memoizeTest(new CountingSupplier());
    memoizeTest(new SerializableCountingSupplier());
  }

  private void memoizeTest(CountingSupplier countingSupplier) {
    Supplier<Integer> memoizedSupplier = Suppliers.memoize(countingSupplier);
    checkMemoize(countingSupplier, memoizedSupplier);
  }

  public void testMemoize_redudantly() {
    memoizeRedudantlyTest(new CountingSupplier());
    memoizeRedudantlyTest(new SerializableCountingSupplier());
  }

  private void memoizeRedudantlyTest(CountingSupplier countingSupplier) {
    Supplier<Integer> memoizedSupplier = Suppliers.memoize(countingSupplier);
    assertSame(memoizedSupplier, Suppliers.memoize(memoizedSupplier));
  }

  public void testMemoizeExceptionThrown() {
    memoizeExceptionThrownTest(new ThrowingSupplier());
    memoizeExceptionThrownTest(new SerializableThrowingSupplier());
  }

  private void memoizeExceptionThrownTest(ThrowingSupplier throwingSupplier) {
    Supplier<Integer> memoizedSupplier = Suppliers.memoize(throwingSupplier);
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

  @J2ktIncompatible
  @GwtIncompatible // SerializableTester
  public void testMemoizeNonSerializable() throws Exception {
    CountingSupplier countingSupplier = new CountingSupplier();
    Supplier<Integer> memoizedSupplier = Suppliers.memoize(countingSupplier);
    assertThat(memoizedSupplier.toString()).isEqualTo("Suppliers.memoize(CountingSupplier)");
    checkMemoize(countingSupplier, memoizedSupplier);
    // Calls to the original memoized supplier shouldn't affect its copy.
    Object unused = memoizedSupplier.get();
    assertThat(memoizedSupplier.toString())
        .isEqualTo("Suppliers.memoize(<supplier that returned 10>)");

    // Should get an exception when we try to serialize.
    RuntimeException ex = assertThrows(RuntimeException.class, () -> reserialize(memoizedSupplier));
    assertThat(ex).hasCauseThat().isInstanceOf(NotSerializableException.class);
  }

  @J2ktIncompatible
  @GwtIncompatible // SerializableTester
  public void testMemoizeSerializable() throws Exception {
    SerializableCountingSupplier countingSupplier = new SerializableCountingSupplier();
    Supplier<Integer> memoizedSupplier = Suppliers.memoize(countingSupplier);
    assertThat(memoizedSupplier.toString()).isEqualTo("Suppliers.memoize(CountingSupplier)");
    checkMemoize(countingSupplier, memoizedSupplier);
    // Calls to the original memoized supplier shouldn't affect its copy.
    Object unused = memoizedSupplier.get();
    assertThat(memoizedSupplier.toString())
        .isEqualTo("Suppliers.memoize(<supplier that returned 10>)");

    Supplier<Integer> copy = reserialize(memoizedSupplier);
    Object unused2 = memoizedSupplier.get();

    CountingSupplier countingCopy =
        (CountingSupplier) ((Suppliers.MemoizingSupplier<Integer>) copy).delegate;
    checkMemoize(countingCopy, copy);
  }

  public void testCompose() {
    Supplier<Integer> fiveSupplier =
        new Supplier<Integer>() {
          @Override
          public Integer get() {
            return 5;
          }
        };

    Function<Number, Integer> intValueFunction =
        new Function<Number, Integer>() {
          @Override
          public Integer apply(Number x) {
            return x.intValue();
          }
        };

    Supplier<Integer> squareSupplier = Suppliers.compose(intValueFunction, fiveSupplier);

    assertEquals(Integer.valueOf(5), squareSupplier.get());
  }

  public void testComposeWithLists() {
    Supplier<ArrayList<Integer>> listSupplier =
        new Supplier<ArrayList<Integer>>() {
          @Override
          public ArrayList<Integer> get() {
            return Lists.newArrayList(0);
          }
        };

    Function<List<Integer>, List<Integer>> addElementFunction =
        new Function<List<Integer>, List<Integer>>() {
          @Override
          public List<Integer> apply(List<Integer> list) {
            ArrayList<Integer> result = new ArrayList<>(list);
            result.add(1);
            return result;
          }
        };

    Supplier<List<Integer>> addSupplier = Suppliers.compose(addElementFunction, listSupplier);

    List<Integer> result = addSupplier.get();
    assertEquals(Integer.valueOf(0), result.get(0));
    assertEquals(Integer.valueOf(1), result.get(1));
  }

  @J2ktIncompatible
  @GwtIncompatible // Thread.sleep
  @SuppressWarnings("DoNotCall")
  public void testMemoizeWithExpiration_longTimeUnit() throws InterruptedException {
    CountingSupplier countingSupplier = new CountingSupplier();

    Supplier<Integer> memoizedSupplier =
        Suppliers.memoizeWithExpiration(countingSupplier, 75, MILLISECONDS);

    checkExpiration(countingSupplier, memoizedSupplier);
  }

  @J2ktIncompatible
  @GwtIncompatible // Thread.sleep
  public void testMemoizeWithExpiration_duration() throws InterruptedException {
    CountingSupplier countingSupplier = new CountingSupplier();

    Supplier<Integer> memoizedSupplier =
        Suppliers.memoizeWithExpiration(countingSupplier, Duration.ofMillis(75));

    checkExpiration(countingSupplier, memoizedSupplier);
  }

  @SuppressWarnings("DoNotCall")
  public void testMemoizeWithExpiration_longTimeUnitNegative() throws InterruptedException {
    assertThrows(
        IllegalArgumentException.class,
        () -> Suppliers.memoizeWithExpiration(() -> "", 0, MILLISECONDS));

    assertThrows(
        IllegalArgumentException.class,
        () -> Suppliers.memoizeWithExpiration(() -> "", -1, MILLISECONDS));
  }

  @J2ktIncompatible // Duration
  @GwtIncompatible // Duration
  public void testMemoizeWithExpiration_durationNegative() throws InterruptedException {
    assertThrows(
        IllegalArgumentException.class,
        () -> Suppliers.memoizeWithExpiration(() -> "", Duration.ZERO));

    assertThrows(
        IllegalArgumentException.class,
        () -> Suppliers.memoizeWithExpiration(() -> "", Duration.ofMillis(-1)));
  }

  @J2ktIncompatible
  @GwtIncompatible // Thread.sleep, SerializationTester
  @SuppressWarnings("DoNotCall")
  public void testMemoizeWithExpirationSerialized() throws InterruptedException {
    SerializableCountingSupplier countingSupplier = new SerializableCountingSupplier();

    Supplier<Integer> memoizedSupplier =
        Suppliers.memoizeWithExpiration(countingSupplier, 75, MILLISECONDS);
    // Calls to the original memoized supplier shouldn't affect its copy.
    Object unused = memoizedSupplier.get();

    Supplier<Integer> copy = reserialize(memoizedSupplier);
    Object unused2 = memoizedSupplier.get();

    CountingSupplier countingCopy =
        (CountingSupplier) ((Suppliers.ExpiringMemoizingSupplier<Integer>) copy).delegate;
    checkExpiration(countingCopy, copy);
  }

  @J2ktIncompatible
  @GwtIncompatible // Thread.sleep
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
    assertSame(toBeSupplied, objectSupplier.get());
    assertSame(toBeSupplied, objectSupplier.get()); // idempotent
  }

  public void testOfInstanceSuppliesNull() {
    Supplier<@Nullable Integer> nullSupplier = Suppliers.ofInstance(null);
    assertNull(nullSupplier.get());
  }

  @J2ktIncompatible
  @GwtIncompatible // Thread
  @SuppressWarnings("DoNotCall")
  public void testExpiringMemoizedSupplierThreadSafe() throws Throwable {
    Function<Supplier<Boolean>, Supplier<Boolean>> memoizer =
        new Function<Supplier<Boolean>, Supplier<Boolean>>() {
          @Override
          public Supplier<Boolean> apply(Supplier<Boolean> supplier) {
            return Suppliers.memoizeWithExpiration(supplier, Long.MAX_VALUE, NANOSECONDS);
          }
        };
    testSupplierThreadSafe(memoizer);
  }

  @J2ktIncompatible
  @GwtIncompatible // Thread
  public void testMemoizedSupplierThreadSafe() throws Throwable {
    Function<Supplier<Boolean>, Supplier<Boolean>> memoizer =
        new Function<Supplier<Boolean>, Supplier<Boolean>>() {
          @Override
          public Supplier<Boolean> apply(Supplier<Boolean> supplier) {
            return Suppliers.memoize(supplier);
          }
        };
    testSupplierThreadSafe(memoizer);
  }

  @J2ktIncompatible
  @GwtIncompatible // Thread
  private void testSupplierThreadSafe(Function<Supplier<Boolean>, Supplier<Boolean>> memoizer)
      throws Throwable {
    AtomicInteger count = new AtomicInteger(0);
    AtomicReference<Throwable> thrown = new AtomicReference<>(null);
    int numThreads = 3;
    Thread[] threads = new Thread[numThreads];
    long timeout = SECONDS.toNanos(60);

    Supplier<Boolean> supplier =
        new Supplier<Boolean>() {
          boolean isWaiting(Thread thread) {
            switch (thread.getState()) {
              case BLOCKED:
              case WAITING:
              case TIMED_WAITING:
                return true;
              default:
                return false;
            }
          }

          int waitingThreads() {
            int waitingThreads = 0;
            for (Thread thread : threads) {
              if (isWaiting(thread)) {
                waitingThreads++;
              }
            }
            return waitingThreads;
          }

          @Override
          @SuppressWarnings("ThreadPriorityCheck") // doing our best to test for races
          public Boolean get() {
            // Check that this method is called exactly once, by the first
            // thread to synchronize.
            long t0 = System.nanoTime();
            while (waitingThreads() != numThreads - 1) {
              if (System.nanoTime() - t0 > timeout) {
                thrown.set(
                    new TimeoutException(
                        "timed out waiting for other threads to block"
                            + " synchronizing on supplier"));
                break;
              }
              Thread.yield();
            }
            count.getAndIncrement();
            return Boolean.TRUE;
          }
        };

    Supplier<Boolean> memoizedSupplier = memoizer.apply(supplier);

    for (int i = 0; i < numThreads; i++) {
      threads[i] =
          new Thread() {
            @Override
            public void run() {
              assertSame(Boolean.TRUE, memoizedSupplier.get());
            }
          };
    }
    for (Thread t : threads) {
      t.start();
    }
    for (Thread t : threads) {
      t.join();
    }

    if (thrown.get() != null) {
      throw thrown.get();
    }
    assertEquals(1, count.get());
  }

  @J2ktIncompatible
  @GwtIncompatible // Thread
  @SuppressWarnings("ThreadPriorityCheck") // doing our best to test for races
  public void testSynchronizedSupplierThreadSafe() throws InterruptedException {
    Supplier<Integer> nonThreadSafe =
        new Supplier<Integer>() {
          int counter = 0;

          @Override
          public Integer get() {
            int nextValue = counter + 1;
            Thread.yield();
            counter = nextValue;
            return counter;
          }
        };

    int numThreads = 10;
    int iterations = 1000;
    Thread[] threads = new Thread[numThreads];
    for (int i = 0; i < numThreads; i++) {
      threads[i] =
          new Thread() {
            @Override
            public void run() {
              for (int j = 0; j < iterations; j++) {
                Object unused = Suppliers.synchronizedSupplier(nonThreadSafe).get();
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

    assertEquals(numThreads * iterations + 1, (int) nonThreadSafe.get());
  }

  public void testSupplierFunction() {
    Supplier<Integer> supplier = Suppliers.ofInstance(14);
    Function<Supplier<Integer>, Integer> supplierFunction = Suppliers.supplierFunction();

    assertEquals(14, (int) supplierFunction.apply(supplier));
  }

  @J2ktIncompatible
  @GwtIncompatible // SerializationTester
  @SuppressWarnings("DoNotCall")
  public void testSerialization() {
    assertEquals(Integer.valueOf(5), reserialize(Suppliers.ofInstance(5)).get());
    assertEquals(
        Integer.valueOf(5),
        reserialize(Suppliers.compose(Functions.identity(), Suppliers.ofInstance(5))).get());
    assertEquals(Integer.valueOf(5), reserialize(Suppliers.memoize(Suppliers.ofInstance(5))).get());
    assertEquals(
        Integer.valueOf(5),
        reserialize(Suppliers.memoizeWithExpiration(Suppliers.ofInstance(5), 30, SECONDS)).get());
    assertEquals(
        Integer.valueOf(5),
        reserialize(Suppliers.synchronizedSupplier(Suppliers.ofInstance(5))).get());
  }

  @J2ktIncompatible
  @GwtIncompatible // reflection
  public void testSuppliersNullChecks() throws Exception {
    new ClassSanityTester()
        .setDefault(Duration.class, Duration.ofSeconds(1))
        .forAllPublicStaticMethods(Suppliers.class)
        .testNulls();
  }

  @J2ktIncompatible
  @GwtIncompatible // reflection
  @AndroidIncompatible // TODO(cpovirk): ClassNotFoundException: com.google.common.base.Function
  public void testSuppliersSerializable() throws Exception {
    new ClassSanityTester()
        .setDefault(Duration.class, Duration.ofSeconds(1))
        .forAllPublicStaticMethods(Suppliers.class)
        .testSerializable();
  }

  public void testOfInstance_equals() {
    new EqualsTester()
        .addEqualityGroup(Suppliers.ofInstance("foo"), Suppliers.ofInstance("foo"))
        .addEqualityGroup(Suppliers.ofInstance("bar"))
        .testEquals();
  }

  public void testCompose_equals() {
    new EqualsTester()
        .addEqualityGroup(
            Suppliers.compose(Functions.constant(1), Suppliers.ofInstance("foo")),
            Suppliers.compose(Functions.constant(1), Suppliers.ofInstance("foo")))
        .addEqualityGroup(Suppliers.compose(Functions.constant(2), Suppliers.ofInstance("foo")))
        .addEqualityGroup(Suppliers.compose(Functions.constant(1), Suppliers.ofInstance("bar")))
        .testEquals();
  }
}
