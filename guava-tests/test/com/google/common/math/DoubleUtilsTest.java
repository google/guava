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

package com.google.common.math;

import static com.google.common.math.MathTesting.ALL_BIGINTEGER_CANDIDATES;
import static com.google.common.math.MathTesting.FINITE_DOUBLE_CANDIDATES;
import static com.google.common.math.MathTesting.POSITIVE_FINITE_DOUBLE_CANDIDATES;

import junit.framework.TestCase;

import sun.misc.FpUtils;

import java.math.BigInteger;

/**
 * Tests for {@link DoubleUtils}.
 * 
 * @author Louis Wasserman
 */
public class DoubleUtilsTest extends TestCase {
  public void testNextDown() {
    for (double d : FINITE_DOUBLE_CANDIDATES) {
      assertEquals(FpUtils.nextDown(d), DoubleUtils.nextDown(d));
    }
  }
  
  public void testBigToDouble() {
    for (BigInteger b : ALL_BIGINTEGER_CANDIDATES) {
      assertEquals(b.doubleValue(), DoubleUtils.bigToDouble(b));
    }
  }

  public void testEnsureNonNegative() {
    assertEquals(0.0, DoubleUtils.ensureNonNegative(0.0));
    for (double positiveValue : POSITIVE_FINITE_DOUBLE_CANDIDATES) {
      assertEquals(positiveValue, DoubleUtils.ensureNonNegative(positiveValue));
      assertEquals(0.0, DoubleUtils.ensureNonNegative(-positiveValue));
    }
    assertEquals(Double.POSITIVE_INFINITY, DoubleUtils.ensureNonNegative(Double.POSITIVE_INFINITY));
    assertEquals(0.0, DoubleUtils.ensureNonNegative(Double.NEGATIVE_INFINITY));
    try {
      DoubleUtils.ensureNonNegative(Double.NaN);
      fail("Expected IllegalArgumentException from ensureNonNegative(Double.NaN)");
    } catch (IllegalArgumentException expected) {
    }
  }
}
