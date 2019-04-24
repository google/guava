/*
 * Written by Doug Lea with assistance from members of JCP JSR-166
 * Expert Group and released to the public domain, as explained at
 * http://creativecommons.org/publicdomain/zero/1.0/
 */

/*
 * Source:
 * http://gee.cs.oswego.edu/cgi-bin/viewcvs.cgi/jsr166/src/jsr166e/extra/AtomicDoubleArray.java?revision=1.5
 * (Modified to adapt to guava coding conventions and
 * to use AtomicLongArray instead of sun.misc.Unsafe)
 */

package com.google.common.util.concurrent;

import static java.lang.Double.doubleToRawLongBits;
import static java.lang.Double.longBitsToDouble;

import com.google.common.annotations.GwtIncompatible;
import com.google.common.primitives.ImmutableLongArray;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.util.concurrent.atomic.AtomicLongArray;

/**
 * A {@code double} array in which elements may be updated atomically. See the {@link
 * java.util.concurrent.atomic} package specification for description of the properties of atomic
 * variables.
 *
 * <p><a name="bitEquals"></a>This class compares primitive {@code double} values in methods such as
 * {@link #compareAndSet} by comparing their bitwise representation using {@link
 * Double#doubleToRawLongBits}, which differs from both the primitive double {@code ==} operator and
 * from {@link Double#equals}, as if implemented by:
 *
 * <pre>{@code
 * static boolean bitEquals(double x, double y) {
 *   long xBits = Double.doubleToRawLongBits(x);
 *   long yBits = Double.doubleToRawLongBits(y);
 *   return xBits == yBits;
 * }
 * }</pre>
 *
 * @author Doug Lea
 * @author Martin Buchholz
 * @since 11.0
 */
@GwtIncompatible
public class AtomicDoubleArray implements java.io.Serializable {
  private static final long serialVersionUID = 0L;

  // Making this non-final is the lesser evil according to Effective
  // Java 2nd Edition Item 76: Write readObject methods defensively.
  private transient AtomicLongArray longs;

  /**
   * Creates a new {@code AtomicDoubleArray} of the given length, with all elements initially zero.
   *
   * @param length the length of the array
   */
  public AtomicDoubleArray(int length) {
    this.longs = new AtomicLongArray(length);
  }

  /**
   * Creates a new {@code AtomicDoubleArray} with the same length as, and all elements copied from,
   * the given array.
   *
   * @param array the array to copy elements from
   * @throws NullPointerException if array is null
   */
  public AtomicDoubleArray(double[] array) {
    final int len = array.length;
    long[] longArray = new long[len];
    for (int i = 0; i < len; i++) {
      longArray[i] = doubleToRawLongBits(array[i]);
    }
    this.longs = new AtomicLongArray(longArray);
  }

  /**
   * Returns the length of the array.
   *
   * @return the length of the array
   */
  public final int length() {
    return longs.length();
  }

  /**
   * Gets the current value at position {@code i}.
   *
   * @param i the index
   * @return the current value
   */
  public final double get(int i) {
    return longBitsToDouble(longs.get(i));
  }

  /**
   * Sets the element at position {@code i} to the given value.
   *
   * @param i the index
   * @param newValue the new value
   */
  public final void set(int i, double newValue) {
    long next = doubleToRawLongBits(newValue);
    longs.set(i, next);
  }

  /**
   * Eventually sets the element at position {@code i} to the given value.
   *
   * @param i the index
   * @param newValue the new value
   */
  public final void lazySet(int i, double newValue) {
    long next = doubleToRawLongBits(newValue);
    longs.lazySet(i, next);
  }

  /**
   * Atomically sets the element at position {@code i} to the given value and returns the old value.
   *
   * @param i the index
   * @param newValue the new value
   * @return the previous value
   */
  public final double getAndSet(int i, double newValue) {
    long next = doubleToRawLongBits(newValue);
    return longBitsToDouble(longs.getAndSet(i, next));
  }

  /**
   * Atomically sets the element at position {@code i} to the given updated value if the current
   * value is <a href="#bitEquals">bitwise equal</a> to the expected value.
   *
   * @param i the index
   * @param expect the expected value
   * @param update the new value
   * @return true if successful. False return indicates that the actual value was not equal to the
   *     expected value.
   */
  public final boolean compareAndSet(int i, double expect, double update) {
    return longs.compareAndSet(i, doubleToRawLongBits(expect), doubleToRawLongBits(update));
  }

  /**
   * Atomically sets the element at position {@code i} to the given updated value if the current
   * value is <a href="#bitEquals">bitwise equal</a> to the expected value.
   *
   * <p>May <a
   * href="http://download.oracle.com/javase/7/docs/api/java/util/concurrent/atomic/package-summary.html#Spurious">
   * fail spuriously</a> and does not provide ordering guarantees, so is only rarely an appropriate
   * alternative to {@code compareAndSet}.
   *
   * @param i the index
   * @param expect the expected value
   * @param update the new value
   * @return true if successful
   */
  public final boolean weakCompareAndSet(int i, double expect, double update) {
    return longs.weakCompareAndSet(i, doubleToRawLongBits(expect), doubleToRawLongBits(update));
  }

  /**
   * Atomically adds the given value to the element at index {@code i}.
   *
   * @param i the index
   * @param delta the value to add
   * @return the previous value
   */
  @CanIgnoreReturnValue
  public final double getAndAdd(int i, double delta) {
    while (true) {
      long current = longs.get(i);
      double currentVal = longBitsToDouble(current);
      double nextVal = currentVal + delta;
      long next = doubleToRawLongBits(nextVal);
      if (longs.compareAndSet(i, current, next)) {
        return currentVal;
      }
    }
  }

  /**
   * Atomically adds the given value to the element at index {@code i}.
   *
   * @param i the index
   * @param delta the value to add
   * @return the updated value
   */
  @CanIgnoreReturnValue
  public double addAndGet(int i, double delta) {
    while (true) {
      long current = longs.get(i);
      double currentVal = longBitsToDouble(current);
      double nextVal = currentVal + delta;
      long next = doubleToRawLongBits(nextVal);
      if (longs.compareAndSet(i, current, next)) {
        return nextVal;
      }
    }
  }

  /**
   * Returns the String representation of the current values of array.
   *
   * @return the String representation of the current values of array
   */
  @Override
  public String toString() {
    int iMax = length() - 1;
    if (iMax == -1) {
      return "[]";
    }

    // Double.toString(Math.PI).length() == 17
    StringBuilder b = new StringBuilder((17 + 2) * (iMax + 1));
    b.append('[');
    for (int i = 0; ; i++) {
      b.append(longBitsToDouble(longs.get(i)));
      if (i == iMax) {
        return b.append(']').toString();
      }
      b.append(',').append(' ');
    }
  }

  /**
   * Saves the state to a stream (that is, serializes it).
   *
   * @serialData The length of the array is emitted (int), followed by all of its elements (each a
   *     {@code double}) in the proper order.
   */
  private void writeObject(java.io.ObjectOutputStream s) throws java.io.IOException {
    s.defaultWriteObject();

    // Write out array length
    int length = length();
    s.writeInt(length);

    // Write out all elements in the proper order.
    for (int i = 0; i < length; i++) {
      s.writeDouble(get(i));
    }
  }

  /** Reconstitutes the instance from a stream (that is, deserializes it). */
  private void readObject(java.io.ObjectInputStream s)
      throws java.io.IOException, ClassNotFoundException {
    s.defaultReadObject();

    int length = s.readInt();
    ImmutableLongArray.Builder builder = ImmutableLongArray.builder();
    for (int i = 0; i < length; i++) {
      builder.add(doubleToRawLongBits(s.readDouble()));
    }
    this.longs = new AtomicLongArray(builder.build().toArray());
  }
}
