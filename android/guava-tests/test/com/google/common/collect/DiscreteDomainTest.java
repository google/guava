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

import static com.google.common.testing.SerializableTester.reserializeAndAssert;
import static org.junit.Assert.assertThrows;

import com.google.common.annotations.GwtIncompatible;
import java.math.BigInteger;
import junit.framework.TestCase;

/**
 * Tests for {@link DiscreteDomain}.
 *
 * @author Chris Povirk
 */
@GwtIncompatible // SerializableTester
public class DiscreteDomainTest extends TestCase {
  public void testSerialization() {
    reserializeAndAssert(DiscreteDomain.integers());
    reserializeAndAssert(DiscreteDomain.longs());
    reserializeAndAssert(DiscreteDomain.bigIntegers());
  }

  public void testIntegersOffset() {
    assertEquals(1, DiscreteDomain.integers().offset(0, 1).intValue());
    assertEquals(
        Integer.MAX_VALUE,
        DiscreteDomain.integers().offset(Integer.MIN_VALUE, (1L << 32) - 1).intValue());
  }

  public void testIntegersOffsetExceptions() {
    assertThrows(IllegalArgumentException.class, () -> DiscreteDomain.integers().offset(0, -1));
    assertThrows(
        IllegalArgumentException.class,
        () -> DiscreteDomain.integers().offset(Integer.MAX_VALUE, 1));
  }

  public void testLongsOffset() {
    assertEquals(1, DiscreteDomain.longs().offset(0L, 1).longValue());
    assertEquals(Long.MAX_VALUE, DiscreteDomain.longs().offset(0L, Long.MAX_VALUE).longValue());
  }

  public void testLongsOffsetExceptions() {
    assertThrows(IllegalArgumentException.class, () -> DiscreteDomain.longs().offset(0L, -1));
    assertThrows(
        IllegalArgumentException.class, () -> DiscreteDomain.longs().offset(Long.MAX_VALUE, 1));
  }

  public void testBigIntegersOffset() {
    assertEquals(BigInteger.ONE, DiscreteDomain.bigIntegers().offset(BigInteger.ZERO, 1));
    assertEquals(
        BigInteger.valueOf(Long.MAX_VALUE),
        DiscreteDomain.bigIntegers().offset(BigInteger.ZERO, Long.MAX_VALUE));
  }

  public void testBigIntegersOffsetExceptions() {
    assertThrows(
        IllegalArgumentException.class,
        () -> DiscreteDomain.bigIntegers().offset(BigInteger.ZERO, -1));
  }

  public void testCustomOffsetExceptions() {
    assertThrows(IllegalArgumentException.class, () -> new MyIntegerDomain().offset(0, -1));
    assertThrows(
        IllegalArgumentException.class, () -> new MyIntegerDomain().offset(Integer.MAX_VALUE, 1));
  }

  private static final class MyIntegerDomain extends DiscreteDomain<Integer> {
    static final DiscreteDomain<Integer> DELEGATE = DiscreteDomain.integers();

    @Override
    public Integer next(Integer value) {
      return DELEGATE.next(value);
    }

    @Override
    public Integer previous(Integer value) {
      return DELEGATE.previous(value);
    }

    // Do *not* override offset() to delegate: We want to test the default implementation.

    @Override
    public long distance(Integer start, Integer end) {
      return DELEGATE.distance(start, end);
    }

    @Override
    public Integer minValue() {
      return DELEGATE.minValue();
    }

    @Override
    public Integer maxValue() {
      return DELEGATE.maxValue();
    }
  }
}
