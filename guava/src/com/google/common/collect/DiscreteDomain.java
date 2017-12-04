/*
 * Copyright (C) 2009 The Guava Authors
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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.CollectPreconditions.checkNonnegative;

import com.google.common.annotations.GwtCompatible;
import com.google.common.primitives.Ints;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.io.Serializable;
import java.math.BigInteger;
import java.util.NoSuchElementException;

/**
 * A descriptor for a <i>discrete</i> {@code Comparable} domain such as all {@link Integer}
 * instances. A discrete domain is one that supports the three basic operations: {@link #next},
 * {@link #previous} and {@link #distance}, according to their specifications. The methods {@link
 * #minValue} and {@link #maxValue} should also be overridden for bounded types.
 *
 * <p>A discrete domain always represents the <i>entire</i> set of values of its type; it cannot
 * represent partial domains such as "prime integers" or "strings of length 5."
 *
 * <p>See the Guava User Guide section on <a href=
 * "https://github.com/google/guava/wiki/RangesExplained#discrete-domains"> {@code
 * DiscreteDomain}</a>.
 *
 * @author Kevin Bourrillion
 * @since 10.0
 */
@GwtCompatible
public abstract class DiscreteDomain<C extends Comparable> {

  /**
   * Returns the discrete domain for values of type {@code Integer}.
   *
   * @since 14.0 (since 10.0 as {@code DiscreteDomains.integers()})
   */
  public static DiscreteDomain<Integer> integers() {
    return IntegerDomain.INSTANCE;
  }

  private static final class IntegerDomain extends DiscreteDomain<Integer> implements Serializable {
    private static final IntegerDomain INSTANCE = new IntegerDomain();

    IntegerDomain() {
      super(true);
    }

    @Override
    public Integer next(Integer value) {
      int i = value;
      return (i == Integer.MAX_VALUE) ? null : i + 1;
    }

    @Override
    public Integer previous(Integer value) {
      int i = value;
      return (i == Integer.MIN_VALUE) ? null : i - 1;
    }

    @Override
    Integer offset(Integer origin, long distance) {
      checkNonnegative(distance, "distance");
      return Ints.checkedCast(origin.longValue() + distance);
    }

    @Override
    public long distance(Integer start, Integer end) {
      return (long) end - start;
    }

    @Override
    public Integer minValue() {
      return Integer.MIN_VALUE;
    }

    @Override
    public Integer maxValue() {
      return Integer.MAX_VALUE;
    }

    private Object readResolve() {
      return INSTANCE;
    }

    @Override
    public String toString() {
      return "DiscreteDomain.integers()";
    }

    private static final long serialVersionUID = 0;
  }

  /**
   * Returns the discrete domain for values of type {@code Long}.
   *
   * @since 14.0 (since 10.0 as {@code DiscreteDomains.longs()})
   */
  public static DiscreteDomain<Long> longs() {
    return LongDomain.INSTANCE;
  }

  private static final class LongDomain extends DiscreteDomain<Long> implements Serializable {
    private static final LongDomain INSTANCE = new LongDomain();

    LongDomain() {
      super(true);
    }

    @Override
    public Long next(Long value) {
      long l = value;
      return (l == Long.MAX_VALUE) ? null : l + 1;
    }

    @Override
    public Long previous(Long value) {
      long l = value;
      return (l == Long.MIN_VALUE) ? null : l - 1;
    }

    @Override
    Long offset(Long origin, long distance) {
      checkNonnegative(distance, "distance");
      long result = origin + distance;
      if (result < 0) {
        checkArgument(origin < 0, "overflow");
      }
      return result;
    }

    @Override
    public long distance(Long start, Long end) {
      long result = end - start;
      if (end > start && result < 0) { // overflow
        return Long.MAX_VALUE;
      }
      if (end < start && result > 0) { // underflow
        return Long.MIN_VALUE;
      }
      return result;
    }

    @Override
    public Long minValue() {
      return Long.MIN_VALUE;
    }

    @Override
    public Long maxValue() {
      return Long.MAX_VALUE;
    }

    private Object readResolve() {
      return INSTANCE;
    }

    @Override
    public String toString() {
      return "DiscreteDomain.longs()";
    }

    private static final long serialVersionUID = 0;
  }

  /**
   * Returns the discrete domain for values of type {@code BigInteger}.
   *
   * @since 15.0
   */
  public static DiscreteDomain<BigInteger> bigIntegers() {
    return BigIntegerDomain.INSTANCE;
  }

  private static final class BigIntegerDomain extends DiscreteDomain<BigInteger>
      implements Serializable {
    private static final BigIntegerDomain INSTANCE = new BigIntegerDomain();

    BigIntegerDomain() {
      super(true);
    }

    private static final BigInteger MIN_LONG = BigInteger.valueOf(Long.MIN_VALUE);
    private static final BigInteger MAX_LONG = BigInteger.valueOf(Long.MAX_VALUE);

    @Override
    public BigInteger next(BigInteger value) {
      return value.add(BigInteger.ONE);
    }

    @Override
    public BigInteger previous(BigInteger value) {
      return value.subtract(BigInteger.ONE);
    }

    @Override
    BigInteger offset(BigInteger origin, long distance) {
      checkNonnegative(distance, "distance");
      return origin.add(BigInteger.valueOf(distance));
    }

    @Override
    public long distance(BigInteger start, BigInteger end) {
      return end.subtract(start).max(MIN_LONG).min(MAX_LONG).longValue();
    }

    private Object readResolve() {
      return INSTANCE;
    }

    @Override
    public String toString() {
      return "DiscreteDomain.bigIntegers()";
    }

    private static final long serialVersionUID = 0;
  }

  final boolean supportsFastOffset;

  /** Constructor for use by subclasses. */
  protected DiscreteDomain() {
    this(false);
  }

  /** Private constructor for built-in DiscreteDomains supporting fast offset. */
  private DiscreteDomain(boolean supportsFastOffset) {
    this.supportsFastOffset = supportsFastOffset;
  }

  /**
   * Returns, conceptually, "origin + distance", or equivalently, the result of calling {@link
   * #next} on {@code origin} {@code distance} times.
   */
  C offset(C origin, long distance) {
    checkNonnegative(distance, "distance");
    for (long i = 0; i < distance; i++) {
      origin = next(origin);
    }
    return origin;
  }

  /**
   * Returns the unique least value of type {@code C} that is greater than {@code value}, or {@code
   * null} if none exists. Inverse operation to {@link #previous}.
   *
   * @param value any value of type {@code C}
   * @return the least value greater than {@code value}, or {@code null} if {@code value} is {@code
   *     maxValue()}
   */
  public abstract C next(C value);

  /**
   * Returns the unique greatest value of type {@code C} that is less than {@code value}, or {@code
   * null} if none exists. Inverse operation to {@link #next}.
   *
   * @param value any value of type {@code C}
   * @return the greatest value less than {@code value}, or {@code null} if {@code value} is {@code
   *     minValue()}
   */
  public abstract C previous(C value);

  /**
   * Returns a signed value indicating how many nested invocations of {@link #next} (if positive) or
   * {@link #previous} (if negative) are needed to reach {@code end} starting from {@code start}.
   * For example, if {@code end = next(next(next(start)))}, then {@code distance(start, end) == 3}
   * and {@code distance(end, start) == -3}. As well, {@code distance(a, a)} is always zero.
   *
   * <p>Note that this function is necessarily well-defined for any discrete type.
   *
   * @return the distance as described above, or {@link Long#MIN_VALUE} or {@link Long#MAX_VALUE} if
   *     the distance is too small or too large, respectively.
   */
  public abstract long distance(C start, C end);

  /**
   * Returns the minimum value of type {@code C}, if it has one. The minimum value is the unique
   * value for which {@link Comparable#compareTo(Object)} never returns a positive value for any
   * input of type {@code C}.
   *
   * <p>The default implementation throws {@code NoSuchElementException}.
   *
   * @return the minimum value of type {@code C}; never null
   * @throws NoSuchElementException if the type has no (practical) minimum value; for example,
   *     {@link java.math.BigInteger}
   */
  @CanIgnoreReturnValue
  public C minValue() {
    throw new NoSuchElementException();
  }

  /**
   * Returns the maximum value of type {@code C}, if it has one. The maximum value is the unique
   * value for which {@link Comparable#compareTo(Object)} never returns a negative value for any
   * input of type {@code C}.
   *
   * <p>The default implementation throws {@code NoSuchElementException}.
   *
   * @return the maximum value of type {@code C}; never null
   * @throws NoSuchElementException if the type has no (practical) maximum value; for example,
   *     {@link java.math.BigInteger}
   */
  @CanIgnoreReturnValue
  public C maxValue() {
    throw new NoSuchElementException();
  }
}
