// Copyright 2011 Google Inc. All Rights Reserved.

package com.google.common.math;

import static com.google.common.base.Preconditions.checkNotNull;

import java.math.BigInteger;

/**
 * A collection of preconditions for math functions.
 * 
 * @author Louis Wasserman
 */
final class MathPreconditions {
  static int checkPositive(String role, int x) {
    if (x <= 0) {
      throw new IllegalArgumentException(role + " (" + x + ") must be > 0");
    }
    return x;
  }

  static long checkPositive(String role, long x) {
    if (x <= 0) {
      throw new IllegalArgumentException(role + " (" + x + ") must be > 0");
    }
    return x;
  }

  static BigInteger checkPositive(String role, BigInteger x) {
    if (x.signum() <= 0) {
      throw new IllegalArgumentException(role + " (" + x + ") must be > 0");
    }
    return x;
  }

  static int checkNonNegative(String role, int x) {
    if (x < 0) {
      throw new IllegalArgumentException(role + " (" + x + ") must be >= 0");
    }
    return x;
  }

  static long checkNonNegative(String role, long x) {
    if (x < 0) {
      throw new IllegalArgumentException(role + " (" + x + ") must be >= 0");
    }
    return x;
  }

  static BigInteger checkNonNegative(String role, BigInteger x) {
    if (checkNotNull(x).signum() < 0) {
      throw new IllegalArgumentException(role + " (" + x + ") must be >= 0");
    }
    return x;
  }

  static void checkRoundingUnnecessary(boolean condition) {
    if (!condition) {
      throw new ArithmeticException("mode was UNNECESSARY, but rounding was necessary");
    }
  }

  static void checkInRange(boolean condition) {
    if (!condition) {
      throw new ArithmeticException("not in range");
    }
  }

  static void checkNoOverflow(boolean condition) {
    if (!condition) {
      throw new ArithmeticException("overflow");
    }
  }

  private MathPreconditions() {}
}
