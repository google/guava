/*
 * Copyright (C) 2010 The Guava Authors
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

import com.google.common.annotations.Beta;
import com.google.common.annotations.GwtCompatible;

import java.io.Serializable;
import java.math.BigInteger;

/**
 * Factories for common {@link DiscreteDomain}s.
 *
 * @author gak@google.com (Gregory Kick)
 * @since Guava release 10
 */
@GwtCompatible
@Beta
public final class DiscreteDomains {
  private DiscreteDomains() {}

  public static DiscreteDomain<Integer> integers() {
    return Integers.INSTANCE;
  }

  private static final class Integers extends DiscreteDomain<Integer>
      implements Serializable {
    private static final Integers INSTANCE = new Integers();

    @Override public Integer next(Integer value) {
      int i = value;
      return (i == Integer.MAX_VALUE) ? null : i + 1;
    }

    @Override public Integer previous(Integer value) {
      int i = value;
      return (i == Integer.MIN_VALUE) ? null : i - 1;
    }

    @Override public long distance(Integer start, Integer end) {
      return (long) end - start;
    }

    @Override public Integer minValue() {
      return Integer.MIN_VALUE;
    }

    @Override public Integer maxValue() {
      return Integer.MAX_VALUE;
    }

    private Object readResolve() {
      return INSTANCE;
    }

    private static final long serialVersionUID = 0;
  }

  public static DiscreteDomain<Long> longs() {
    return Longs.INSTANCE;
  }

  private static final class Longs extends DiscreteDomain<Long>
      implements Serializable {
    private static final Longs INSTANCE = new Longs();

    @Override public Long next(Long value) {
      long l = value;
      return (l == Long.MAX_VALUE) ? null : l + 1;
    }

    @Override public Long previous(Long value) {
      long l = value;
      return (l == Long.MIN_VALUE) ? null : l - 1;
    }

    @Override public long distance(Long start, Long end) {
      long result = end - start;
      if (end > start && result < 0) { // overflow
        return Long.MAX_VALUE;
      }
      if (end < start && result > 0) { // underflow
        return Long.MIN_VALUE;
      }
      return result;
    }

    @Override public Long minValue() {
      return Long.MIN_VALUE;
    }

    @Override public Long maxValue() {
      return Long.MAX_VALUE;
    }

    private Object readResolve() {
      return INSTANCE;
    }

    private static final long serialVersionUID = 0;
  }

  static DiscreteDomain<BigInteger> bigIntegers() {
    return BigIntegers.INSTANCE;
  }

  private static final class BigIntegers extends DiscreteDomain<BigInteger>
      implements Serializable {
    private static final BigIntegers INSTANCE = new BigIntegers();

    private static final BigInteger MIN_LONG =
        BigInteger.valueOf(Long.MIN_VALUE);
    private static final BigInteger MAX_LONG =
        BigInteger.valueOf(Long.MAX_VALUE);

    @Override public BigInteger next(BigInteger value) {
      return value.add(BigInteger.ONE);
    }

    @Override public BigInteger previous(BigInteger value) {
      return value.subtract(BigInteger.ONE);
    }

    @Override public long distance(BigInteger start, BigInteger end) {
      return start.subtract(end).max(MIN_LONG).min(MAX_LONG).longValue();
    }

    private Object readResolve() {
      return INSTANCE;
    }

    private static final long serialVersionUID = 0;
  }
}
