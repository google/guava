package com.google.common.math;

import static com.google.common.math.MathTesting.ALL_BIGINTEGER_CANDIDATES;
import static com.google.common.math.MathTesting.ALL_DOUBLE_CANDIDATES;
import static com.google.common.math.MathTesting.EXPONENTS;
import static com.google.common.math.MathTesting.FINITE_DOUBLE_CANDIDATES;

import java.math.BigInteger;

import junit.framework.TestCase;
import sun.misc.FpUtils;

public class DoubleUtilsTest extends TestCase {
  public strictfp void testScalbPositiveExponent() {
    for (int k : EXPONENTS) {
      for (double d : ALL_DOUBLE_CANDIDATES) {
        assertEquals(d * StrictMath.pow(2.0, k), DoubleUtils.scalb(d, k));
      }
    }
  }

  public void testGetExponent() {
    for (double d : ALL_DOUBLE_CANDIDATES) {
      assertEquals(FpUtils.getExponent(d), DoubleUtils.getExponent(d));
    }
  }

  public void testNextUp() {
    for (double d : FINITE_DOUBLE_CANDIDATES) {
      assertEquals(FpUtils.nextUp(d), DoubleUtils.next(d, true));
    }
  }

  public void testNextDown() {
    for (double d : FINITE_DOUBLE_CANDIDATES) {
      assertEquals(FpUtils.nextDown(d), DoubleUtils.next(d, false));
    }
  }
  
  public void testBigToDouble() {
    for (BigInteger b : ALL_BIGINTEGER_CANDIDATES) {
      assertEquals(b.doubleValue(), DoubleUtils.bigToDouble(b));
    }
  }
}
