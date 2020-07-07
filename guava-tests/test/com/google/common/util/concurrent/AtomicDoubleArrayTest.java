/*
 * Written by Doug Lea and Martin Buchholz with assistance from
 * members of JCP JSR-166 Expert Group and released to the public
 * domain, as explained at
 * http://creativecommons.org/publicdomain/zero/1.0/
 */

/*
 * Source:
 * http://gee.cs.oswego.edu/cgi-bin/viewcvs.cgi/jsr166/src/test/tck-jsr166e/AtomicDoubleArrayTest.java?revision=1.13
 * (Modified to adapt to guava coding conventions)
 */

package com.google.common.util.concurrent;

import java.util.Arrays;

/** Unit test for {@link AtomicDoubleArray}. */
public class AtomicDoubleArrayTest extends JSR166TestCase {

  private static final double[] VALUES = {
    Double.NEGATIVE_INFINITY,
    -Double.MAX_VALUE,
    (double) Long.MIN_VALUE,
    (double) Integer.MIN_VALUE,
    -Math.PI,
    -1.0,
    -Double.MIN_VALUE,
    -0.0,
    +0.0,
    Double.MIN_VALUE,
    1.0,
    Math.PI,
    (double) Integer.MAX_VALUE,
    (double) Long.MAX_VALUE,
    Double.MAX_VALUE,
    Double.POSITIVE_INFINITY,
    Double.NaN,
    Float.MAX_VALUE,
  };

  /** The notion of equality used by AtomicDoubleArray */
  static boolean bitEquals(double x, double y) {
    return Double.doubleToRawLongBits(x) == Double.doubleToRawLongBits(y);
  }

  static void assertBitEquals(double x, double y) {
    assertEquals(Double.doubleToRawLongBits(x), Double.doubleToRawLongBits(y));
  }

  /** constructor creates array of given size with all elements zero */
  public void testConstructor() {
    AtomicDoubleArray aa = new AtomicDoubleArray(SIZE);
    for (int i = 0; i < SIZE; i++) {
      assertBitEquals(0.0, aa.get(i));
    }
  }

  /** constructor with null array throws NPE */
  public void testConstructor2NPE() {
    double[] a = null;
    try {
      new AtomicDoubleArray(a);
      fail();
    } catch (NullPointerException success) {
    }
  }

  /** constructor with array is of same size and has all elements */
  public void testConstructor2() {
    AtomicDoubleArray aa = new AtomicDoubleArray(VALUES);
    assertEquals(VALUES.length, aa.length());
    for (int i = 0; i < VALUES.length; i++) {
      assertBitEquals(VALUES[i], aa.get(i));
    }
  }

  /** constructor with empty array has size 0 and contains no elements */
  public void testConstructorEmptyArray() {
    AtomicDoubleArray aa = new AtomicDoubleArray(new double[0]);
    assertEquals(0, aa.length());
    try {
      aa.get(0);
      fail();
    } catch (IndexOutOfBoundsException success) {
    }
  }

  /** constructor with length zero has size 0 and contains no elements */
  public void testConstructorZeroLength() {
    AtomicDoubleArray aa = new AtomicDoubleArray(0);
    assertEquals(0, aa.length());
    try {
      aa.get(0);
      fail();
    } catch (IndexOutOfBoundsException success) {
    }
  }

  /** get and set for out of bound indices throw IndexOutOfBoundsException */
  public void testIndexing() {
    AtomicDoubleArray aa = new AtomicDoubleArray(SIZE);
    for (int index : new int[] {-1, SIZE}) {
      try {
        aa.get(index);
        fail();
      } catch (IndexOutOfBoundsException success) {
      }
      try {
        aa.set(index, 1.0);
        fail();
      } catch (IndexOutOfBoundsException success) {
      }
      try {
        aa.lazySet(index, 1.0);
        fail();
      } catch (IndexOutOfBoundsException success) {
      }
      try {
        aa.compareAndSet(index, 1.0, 2.0);
        fail();
      } catch (IndexOutOfBoundsException success) {
      }
      try {
        aa.weakCompareAndSet(index, 1.0, 2.0);
        fail();
      } catch (IndexOutOfBoundsException success) {
      }
      try {
        aa.getAndAdd(index, 1.0);
        fail();
      } catch (IndexOutOfBoundsException success) {
      }
      try {
        aa.addAndGet(index, 1.0);
        fail();
      } catch (IndexOutOfBoundsException success) {
      }
    }
  }

  /** get returns the last value set at index */
  public void testGetSet() {
    AtomicDoubleArray aa = new AtomicDoubleArray(VALUES.length);
    for (int i = 0; i < VALUES.length; i++) {
      assertBitEquals(0.0, aa.get(i));
      aa.set(i, VALUES[i]);
      assertBitEquals(VALUES[i], aa.get(i));
      aa.set(i, -3.0);
      assertBitEquals(-3.0, aa.get(i));
    }
  }

  /** get returns the last value lazySet at index by same thread */
  public void testGetLazySet() {
    AtomicDoubleArray aa = new AtomicDoubleArray(VALUES.length);
    for (int i = 0; i < VALUES.length; i++) {
      assertBitEquals(0.0, aa.get(i));
      aa.lazySet(i, VALUES[i]);
      assertBitEquals(VALUES[i], aa.get(i));
      aa.lazySet(i, -3.0);
      assertBitEquals(-3.0, aa.get(i));
    }
  }

  /** compareAndSet succeeds in changing value if equal to expected else fails */
  public void testCompareAndSet() {
    AtomicDoubleArray aa = new AtomicDoubleArray(SIZE);
    for (int i : new int[] {0, SIZE - 1}) {
      double prev = 0.0;
      double unused = Math.E + Math.PI;
      for (double x : VALUES) {
        assertBitEquals(prev, aa.get(i));
        assertFalse(aa.compareAndSet(i, unused, x));
        assertBitEquals(prev, aa.get(i));
        assertTrue(aa.compareAndSet(i, prev, x));
        assertBitEquals(x, aa.get(i));
        prev = x;
      }
    }
  }

  /** compareAndSet in one thread enables another waiting for value to succeed */

  public void testCompareAndSetInMultipleThreads() throws InterruptedException {
    final AtomicDoubleArray a = new AtomicDoubleArray(1);
    a.set(0, 1.0);
    Thread t =
        newStartedThread(
            new CheckedRunnable() {
              @Override
              public void realRun() {
                while (!a.compareAndSet(0, 2.0, 3.0)) {
                  Thread.yield();
                }
              }
            });

    assertTrue(a.compareAndSet(0, 1.0, 2.0));
    awaitTermination(t);
    assertBitEquals(3.0, a.get(0));
  }

  /** repeated weakCompareAndSet succeeds in changing value when equal to expected */
  public void testWeakCompareAndSet() {
    AtomicDoubleArray aa = new AtomicDoubleArray(SIZE);
    for (int i : new int[] {0, SIZE - 1}) {
      double prev = 0.0;
      double unused = Math.E + Math.PI;
      for (double x : VALUES) {
        assertBitEquals(prev, aa.get(i));
        assertFalse(aa.weakCompareAndSet(i, unused, x));
        assertBitEquals(prev, aa.get(i));
        while (!aa.weakCompareAndSet(i, prev, x)) {;
        }
        assertBitEquals(x, aa.get(i));
        prev = x;
      }
    }
  }

  /** getAndSet returns previous value and sets to given value at given index */
  public void testGetAndSet() {
    AtomicDoubleArray aa = new AtomicDoubleArray(SIZE);
    for (int i : new int[] {0, SIZE - 1}) {
      double prev = 0.0;
      for (double x : VALUES) {
        assertBitEquals(prev, aa.getAndSet(i, x));
        prev = x;
      }
    }
  }

  /** getAndAdd returns previous value and adds given value */
  public void testGetAndAdd() {
    AtomicDoubleArray aa = new AtomicDoubleArray(SIZE);
    for (int i : new int[] {0, SIZE - 1}) {
      for (double x : VALUES) {
        for (double y : VALUES) {
          aa.set(i, x);
          double z = aa.getAndAdd(i, y);
          assertBitEquals(x, z);
          assertBitEquals(x + y, aa.get(i));
        }
      }
    }
  }

  /** addAndGet adds given value to current, and returns current value */
  public void testAddAndGet() {
    AtomicDoubleArray aa = new AtomicDoubleArray(SIZE);
    for (int i : new int[] {0, SIZE - 1}) {
      for (double x : VALUES) {
        for (double y : VALUES) {
          aa.set(i, x);
          double z = aa.addAndGet(i, y);
          assertBitEquals(x + y, z);
          assertBitEquals(x + y, aa.get(i));
        }
      }
    }
  }

  static final long COUNTDOWN = 100000;

  class Counter extends CheckedRunnable {
    final AtomicDoubleArray aa;
    volatile long counts;

    Counter(AtomicDoubleArray a) {
      aa = a;
    }

    @Override
    public void realRun() {
      for (; ; ) {
        boolean done = true;
        for (int i = 0; i < aa.length(); i++) {
          double v = aa.get(i);
          assertTrue(v >= 0);
          if (v != 0) {
            done = false;
            if (aa.compareAndSet(i, v, v - 1.0)) {
              ++counts;
            }
          }
        }
        if (done) {
          break;
        }
      }
    }
  }

  /**
   * Multiple threads using same array of counters successfully update a number of times equal to
   * total count
   */

  public void testCountingInMultipleThreads() throws InterruptedException {
    final AtomicDoubleArray aa = new AtomicDoubleArray(SIZE);
    for (int i = 0; i < SIZE; i++) {
      aa.set(i, (double) COUNTDOWN);
    }
    Counter c1 = new Counter(aa);
    Counter c2 = new Counter(aa);
    Thread t1 = newStartedThread(c1);
    Thread t2 = newStartedThread(c2);
    awaitTermination(t1);
    awaitTermination(t2);
    assertEquals(SIZE * COUNTDOWN, c1.counts + c2.counts);
  }

  /** a deserialized serialized array holds same values */
  public void testSerialization() throws Exception {
    AtomicDoubleArray x = new AtomicDoubleArray(SIZE);
    for (int i = 0; i < SIZE; i++) {
      x.set(i, (double) -i);
    }
    AtomicDoubleArray y = serialClone(x);
    assertTrue(x != y);
    assertEquals(x.length(), y.length());
    for (int i = 0; i < SIZE; i++) {
      assertBitEquals(x.get(i), y.get(i));
    }

    AtomicDoubleArray a = new AtomicDoubleArray(VALUES);
    AtomicDoubleArray b = serialClone(a);
    assertFalse(a.equals(b));
    assertFalse(b.equals(a));
    assertEquals(a.length(), b.length());
    for (int i = 0; i < VALUES.length; i++) {
      assertBitEquals(a.get(i), b.get(i));
    }
  }

  /** toString returns current value */
  public void testToString() {
    AtomicDoubleArray aa = new AtomicDoubleArray(VALUES);
    assertEquals(Arrays.toString(VALUES), aa.toString());
    assertEquals("[]", new AtomicDoubleArray(0).toString());
    assertEquals("[]", new AtomicDoubleArray(new double[0]).toString());
  }

  /** compareAndSet treats +0.0 and -0.0 as distinct values */
  public void testDistinctZeros() {
    AtomicDoubleArray aa = new AtomicDoubleArray(SIZE);
    for (int i : new int[] {0, SIZE - 1}) {
      assertFalse(aa.compareAndSet(i, -0.0, 7.0));
      assertFalse(aa.weakCompareAndSet(i, -0.0, 7.0));
      assertBitEquals(+0.0, aa.get(i));
      assertTrue(aa.compareAndSet(i, +0.0, -0.0));
      assertBitEquals(-0.0, aa.get(i));
      assertFalse(aa.compareAndSet(i, +0.0, 7.0));
      assertFalse(aa.weakCompareAndSet(i, +0.0, 7.0));
      assertBitEquals(-0.0, aa.get(i));
    }
  }
}
