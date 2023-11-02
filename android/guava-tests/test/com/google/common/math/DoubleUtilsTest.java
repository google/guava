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
import static org.junit.Assert.assertThrows;

import java.lang.reflect.Method;
import java.math.BigInteger;
import junit.framework.TestCase;

/**
 * Tests for {@link DoubleUtils}.
 *
 * @author Louis Wasserman
 */
public class DoubleUtilsTest extends TestCase {
  @AndroidIncompatible // no FpUtils and no Math.nextDown in old versions
  public void testNextDown() throws Exception {
    Method jdkNextDown = getJdkNextDown();
    for (double d : FINITE_DOUBLE_CANDIDATES) {
      assertEquals(jdkNextDown.invoke(null, d), DoubleUtils.nextDown(d));
    }
  }

  private static Method getJdkNextDown() throws Exception {
    try {
      return Math.class.getMethod("nextDown", double.class);
    } catch (NoSuchMethodException expectedBeforeJava8) {
      return Class.forName("sun.misc.FpUtils").getMethod("nextDown", double.class);
    }
  }

  @AndroidIncompatible // TODO(cpovirk): File bug for BigDecimal.doubleValue().
  public void testBigToDouble() {
    for (BigInteger b : ALL_BIGINTEGER_CANDIDATES) {
      if (b.doubleValue() != DoubleUtils.bigToDouble(b)) {
        failFormat(
            "Converting %s to double: expected doubleValue %s but got bigToDouble %s",
            b, b.doubleValue(), DoubleUtils.bigToDouble(b));
      }
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
    assertThrows(IllegalArgumentException.class, () -> DoubleUtils.ensureNonNegative(Double.NaN));
  }

  public void testOneBits() {
    assertEquals(DoubleUtils.ONE_BITS, Double.doubleToRawLongBits(1.0));
  }

  private static void failFormat(String template, Object... args) {
    fail(String.format(template, args));
  }
}
