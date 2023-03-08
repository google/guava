/*
 * Written by Doug Lea and Martin Buchholz with assistance from
 * members of JCP JSR-166 Expert Group and released to the public
 * domain, as explained at
 * http://creativecommons.org/publicdomain/zero/1.0/
 */

/*
 * Source:
 * http://gee.cs.oswego.edu/cgi-bin/viewcvs.cgi/jsr166/src/jsr166e/extra/AtomicDouble.java?revision=1.13
 * (Modified to adapt to guava coding conventions and
 * to use AtomicLongFieldUpdater instead of sun.misc.Unsafe)
 */

package com.google.common.util.concurrent;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.Double.doubleToRawLongBits;
import static java.lang.Double.longBitsToDouble;

import com.google.common.annotations.GwtIncompatible;
import com.google.common.annotations.J2ktIncompatible;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.j2objc.annotations.ReflectionSupport;
import java.util.concurrent.atomic.AtomicLongFieldUpdater;
import java.util.function.DoubleBinaryOperator;
import java.util.function.DoubleUnaryOperator;

/**
 * A {@code double} value that may be updated atomically. See the {@link
 * java.util.concurrent.atomic} package specification for description of the properties of atomic
 * variables. An {@code AtomicDouble} is used in applications such as atomic accumulation, and
 * cannot be used as a replacement for a {@link Double}. However, this class does extend {@code
 * Number} to allow uniform access by tools and utilities that deal with numerically-based classes.
 *
 * <p><a id="bitEquals"></a>This class compares primitive {@code double} values in methods such as
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
 * <p>It is possible to write a more scalable updater, at the cost of giving up strict atomicity.
 * See for example <a
 * href="http://gee.cs.oswego.edu/dl/jsr166/dist/docs/java.base/java/util/concurrent/atomic/DoubleAdder.html">
 * DoubleAdder</a>.
 *
 * @author Doug Lea
 * @author Martin Buchholz
 * @since 11.0
 */
@GwtIncompatible
@J2ktIncompatible
@ReflectionSupport(value = ReflectionSupport.Level.FULL)
@ElementTypesAreNonnullByDefault
public class AtomicDouble extends Number implements java.io.Serializable {
  private static final long serialVersionUID = 0L;

  private transient volatile long value;

  private static final AtomicLongFieldUpdater<AtomicDouble> updater =
      AtomicLongFieldUpdater.newUpdater(AtomicDouble.class, "value");

  /**
   * Creates a new {@code AtomicDouble} with the given initial value.
   *
   * @param initialValue the initial value
   */
  public AtomicDouble(double initialValue) {
    value = doubleToRawLongBits(initialValue);
  }

  /** Creates a new {@code AtomicDouble} with initial value {@code 0.0}. */
  public AtomicDouble() {
    // assert doubleToRawLongBits(0.0) == 0L;
  }

  /**
   * Gets the current value.
   *
   * @return the current value
   */
  public final double get() {
    return longBitsToDouble(value);
  }

  /**
   * Sets to the given value.
   *
   * @param newValue the new value
   */
  public final void set(double newValue) {
    long next = doubleToRawLongBits(newValue);
    value = next;
  }

  /**
   * Eventually sets to the given value.
   *
   * @param newValue the new value
   */
  public final void lazySet(double newValue) {
    long next = doubleToRawLongBits(newValue);
    updater.lazySet(this, next);
  }

  /**
   * Atomically sets to the given value and returns the old value.
   *
   * @param newValue the new value
   * @return the previous value
   */
  public final double getAndSet(double newValue) {
    long next = doubleToRawLongBits(newValue);
    return longBitsToDouble(updater.getAndSet(this, next));
  }

  /**
   * Atomically sets the value to the given updated value if the current value is <a
   * href="#bitEquals">bitwise equal</a> to the expected value.
   *
   * @param expect the expected value
   * @param update the new value
   * @return {@code true} if successful. False return indicates that the actual value was not
   *     bitwise equal to the expected value.
   */
  public final boolean compareAndSet(double expect, double update) {
    return updater.compareAndSet(this, doubleToRawLongBits(expect), doubleToRawLongBits(update));
  }

  /**
   * Atomically sets the value to the given updated value if the current value is <a
   * href="#bitEquals">bitwise equal</a> to the expected value.
   *
   * <p>May <a
   * href="http://download.oracle.com/javase/7/docs/api/java/util/concurrent/atomic/package-summary.html#Spurious">
   * fail spuriously</a> and does not provide ordering guarantees, so is only rarely an appropriate
   * alternative to {@code compareAndSet}.
   *
   * @param expect the expected value
   * @param update the new value
   * @return {@code true} if successful
   */
  public final boolean weakCompareAndSet(double expect, double update) {
    return updater.weakCompareAndSet(
        this, doubleToRawLongBits(expect), doubleToRawLongBits(update));
  }

  /**
   * Atomically adds the given value to the current value.
   *
   * @param delta the value to add
   * @return the previous value
   */
  @CanIgnoreReturnValue
  public final double getAndAdd(double delta) {
    return getAndAccumulate(delta, Double::sum);
  }

  /**
   * Atomically adds the given value to the current value.
   *
   * @param delta the value to add
   * @return the updated value
   */
  @CanIgnoreReturnValue
  public final double addAndGet(double delta) {
    return accumulateAndGet(delta, Double::sum);
  }

  /**
   * Atomically updates the current value with the results of applying the given function to the
   * current and given values.
   *
   * @param x the update value
   * @param accumulatorFunction the accumulator function
   * @return the previous value
   * @since 31.1
   */
  @CanIgnoreReturnValue
  public final double getAndAccumulate(double x, DoubleBinaryOperator accumulatorFunction) {
    checkNotNull(accumulatorFunction);
    return getAndUpdate(oldValue -> accumulatorFunction.applyAsDouble(oldValue, x));
  }

  /**
   * Atomically updates the current value with the results of applying the given function to the
   * current and given values.
   *
   * @param x the update value
   * @param accumulatorFunction the accumulator function
   * @return the updated value
   * @since 31.1
   */
  @CanIgnoreReturnValue
  public final double accumulateAndGet(double x, DoubleBinaryOperator accumulatorFunction) {
    checkNotNull(accumulatorFunction);
    return updateAndGet(oldValue -> accumulatorFunction.applyAsDouble(oldValue, x));
  }

  /**
   * Atomically updates the current value with the results of applying the given function.
   *
   * @param updateFunction the update function
   * @return the previous value
   * @since 31.1
   */
  @CanIgnoreReturnValue
  public final double getAndUpdate(DoubleUnaryOperator updateFunction) {
    while (true) {
      long current = value;
      double currentVal = longBitsToDouble(current);
      double nextVal = updateFunction.applyAsDouble(currentVal);
      long next = doubleToRawLongBits(nextVal);
      if (updater.compareAndSet(this, current, next)) {
        return currentVal;
      }
    }
  }

  /**
   * Atomically updates the current value with the results of applying the given function.
   *
   * @param updateFunction the update function
   * @return the updated value
   * @since 31.1
   */
  @CanIgnoreReturnValue
  public final double updateAndGet(DoubleUnaryOperator updateFunction) {
    while (true) {
      long current = value;
      double currentVal = longBitsToDouble(current);
      double nextVal = updateFunction.applyAsDouble(currentVal);
      long next = doubleToRawLongBits(nextVal);
      if (updater.compareAndSet(this, current, next)) {
        return nextVal;
      }
    }
  }

  /**
   * Returns the String representation of the current value.
   *
   * @return the String representation of the current value
   */
  @Override
  public String toString() {
    return Double.toString(get());
  }

  /**
   * Returns the value of this {@code AtomicDouble} as an {@code int} after a narrowing primitive
   * conversion.
   */
  @Override
  public int intValue() {
    return (int) get();
  }

  /**
   * Returns the value of this {@code AtomicDouble} as a {@code long} after a narrowing primitive
   * conversion.
   */
  @Override
  public long longValue() {
    return (long) get();
  }

  /**
   * Returns the value of this {@code AtomicDouble} as a {@code float} after a narrowing primitive
   * conversion.
   */
  @Override
  public float floatValue() {
    return (float) get();
  }

  /** Returns the value of this {@code AtomicDouble} as a {@code double}. */
  @Override
  public double doubleValue() {
    return get();
  }

  /**
   * Saves the state to a stream (that is, serializes it).
   *
   * @serialData The current value is emitted (a {@code double}).
   */
  private void writeObject(java.io.ObjectOutputStream s) throws java.io.IOException {
    s.defaultWriteObject();

    s.writeDouble(get());
  }

  /** Reconstitutes the instance from a stream (that is, deserializes it). */
  private void readObject(java.io.ObjectInputStream s)
      throws java.io.IOException, ClassNotFoundException {
    s.defaultReadObject();

    set(s.readDouble());
  }
}
