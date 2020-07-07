/*
 * Copyright (C) 2012 The Guava Authors
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

import static com.google.common.truth.Truth.assertThat;

import com.google.common.annotations.GwtCompatible;
import java.math.BigInteger;
import java.math.RoundingMode;
import junit.framework.TestCase;

/**
 * Unit tests for {@link MathPreconditions}.
 *
 * @author Ben Yu
 */
@GwtCompatible
public class MathPreconditionsTest extends TestCase {

  public void testCheckPositive_zeroInt() {
    try {
      MathPreconditions.checkPositive("int", 0);
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }

  public void testCheckPositive_maxInt() {
    MathPreconditions.checkPositive("int", Integer.MAX_VALUE);
  }

  public void testCheckPositive_minInt() {
    try {
      MathPreconditions.checkPositive("int", Integer.MIN_VALUE);
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }

  public void testCheckPositive_positiveInt() {
    MathPreconditions.checkPositive("int", 1);
  }

  public void testCheckPositive_negativeInt() {
    try {
      MathPreconditions.checkPositive("int", -1);
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }

  public void testCheckPositive_zeroLong() {
    try {
      MathPreconditions.checkPositive("long", 0L);
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }

  public void testCheckPositive_maxLong() {
    MathPreconditions.checkPositive("long", Long.MAX_VALUE);
  }

  public void testCheckPositive_minLong() {
    try {
      MathPreconditions.checkPositive("long", Long.MIN_VALUE);
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }

  public void testCheckPositive_positiveLong() {
    MathPreconditions.checkPositive("long", 1);
  }

  public void testCheckPositive_negativeLong() {
    try {
      MathPreconditions.checkPositive("long", -1L);
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }

  public void testCheckPositive_zeroBigInteger() {
    try {
      MathPreconditions.checkPositive("BigInteger", BigInteger.ZERO);
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }

  public void testCheckPositive_postiveBigInteger() {
    MathPreconditions.checkPositive("BigInteger", BigInteger.ONE);
  }

  public void testCheckPositive_negativeBigInteger() {
    try {
      MathPreconditions.checkPositive("BigInteger", BigInteger.ZERO.negate());
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }

  public void testCheckNonNegative_zeroInt() {
    MathPreconditions.checkNonNegative("int", 0);
  }

  public void testCheckNonNegative_maxInt() {
    MathPreconditions.checkNonNegative("int", Integer.MAX_VALUE);
  }

  public void testCheckNonNegative_minInt() {
    try {
      MathPreconditions.checkNonNegative("int", Integer.MIN_VALUE);
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }

  public void testCheckNonNegative_positiveInt() {
    MathPreconditions.checkNonNegative("int", 1);
  }

  public void testCheckNonNegative_negativeInt() {
    try {
      MathPreconditions.checkNonNegative("int", -1);
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }

  public void testCheckNonNegative_zeroLong() {
    MathPreconditions.checkNonNegative("long", 0L);
  }

  public void testCheckNonNegative_maxLong() {
    MathPreconditions.checkNonNegative("long", Long.MAX_VALUE);
  }

  public void testCheckNonNegative_minLong() {
    try {
      MathPreconditions.checkNonNegative("long", Long.MIN_VALUE);
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }

  public void testCheckNonNegative_positiveLong() {
    MathPreconditions.checkNonNegative("long", 1L);
  }

  public void testCheckNonNegative_negativeLong() {
    try {
      MathPreconditions.checkNonNegative("int", -1L);
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }

  public void testCheckNonNegative_zeroBigInteger() {
    MathPreconditions.checkNonNegative("BigInteger", BigInteger.ZERO);
  }

  public void testCheckNonNegative_positiveBigInteger() {
    MathPreconditions.checkNonNegative("BigInteger", BigInteger.ONE);
  }

  public void testCheckNonNegative_negativeBigInteger() {
    try {
      MathPreconditions.checkNonNegative("int", BigInteger.ONE.negate());
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }

  public void testCheckNonNegative_zeroFloat() {
    MathPreconditions.checkNonNegative("float", 0f);
  }

  public void testCheckNonNegative_maxFloat() {
    MathPreconditions.checkNonNegative("float", Float.MAX_VALUE);
  }

  public void testCheckNonNegative_minFloat() {
    MathPreconditions.checkNonNegative("float", Float.MIN_VALUE);
  }

  public void testCheckNonNegative_positiveFloat() {
    MathPreconditions.checkNonNegative("float", 1f);
  }

  public void testCheckNonNegative_negativeFloat() {
    try {
      MathPreconditions.checkNonNegative("float", -1f);
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }

  public void testCheckNonNegative_nanFloat() {
    try {
      MathPreconditions.checkNonNegative("float", Float.NaN);
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }

  public void testCheckNonNegative_zeroDouble() {
    MathPreconditions.checkNonNegative("double", 0d);
  }

  public void testCheckNonNegative_maxDouble() {
    MathPreconditions.checkNonNegative("double", Double.MAX_VALUE);
  }

  public void testCheckNonNegative_minDouble() {
    MathPreconditions.checkNonNegative("double", Double.MIN_VALUE);
  }

  public void testCheckNonNegative_positiveDouble() {
    MathPreconditions.checkNonNegative("double", 1d);
  }

  public void testCheckNonNegative_negativeDouble() {
    try {
      MathPreconditions.checkNonNegative("double", -1d);
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }

  public void testCheckNonNegative_nanDouble() {
    try {
      MathPreconditions.checkNonNegative("double", Double.NaN);
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }

  public void testCheckRoundingUnnnecessary_success() {
    MathPreconditions.checkRoundingUnnecessary(true);
  }

  public void testCheckRoundingUnnecessary_failure() {
    try {
      MathPreconditions.checkRoundingUnnecessary(false);
      fail();
    } catch (ArithmeticException expected) {
    }
  }

  public void testCheckInRange_success() {
    MathPreconditions.checkInRangeForRoundingInputs(true, 1.0, RoundingMode.UP);
  }

  public void testCheckInRange_failure() {
    try {
      MathPreconditions.checkInRangeForRoundingInputs(false, 1.0, RoundingMode.UP);
      fail();
    } catch (ArithmeticException expected) {
      assertThat(expected).hasMessageThat().contains("1.0");
      assertThat(expected).hasMessageThat().contains("UP");
    }
  }

  public void testCheckNoOverflow_success() {
    MathPreconditions.checkNoOverflow(true, "testCheckNoOverflow_success", 0, 0);
  }

  public void testCheckNoOverflow_failure() {
    try {
      MathPreconditions.checkNoOverflow(false, "testCheckNoOverflow_failure", 0, 0);
      fail();
    } catch (ArithmeticException expected) {
      assertThat(expected).hasMessageThat().contains("testCheckNoOverflow_failure(0, 0)");
    }
  }
}
