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

import static com.google.common.math.ReflectionFreeAssertThrows.assertThrows;
import static com.google.common.truth.Truth.assertThat;

import com.google.common.annotations.GwtCompatible;
import java.math.BigInteger;
import java.math.RoundingMode;
import junit.framework.TestCase;
import org.jspecify.annotations.NullUnmarked;

/**
 * Unit tests for {@link MathPreconditions}.
 *
 * @author Ben Yu
 */
@GwtCompatible
@NullUnmarked
public class MathPreconditionsTest extends TestCase {

  public void testCheckPositive_zeroInt() {
    assertThrows(IllegalArgumentException.class, () -> MathPreconditions.checkPositive("int", 0));
  }

  public void testCheckPositive_maxInt() {
    MathPreconditions.checkPositive("int", Integer.MAX_VALUE);
  }

  public void testCheckPositive_minInt() {
    assertThrows(
        IllegalArgumentException.class,
        () -> MathPreconditions.checkPositive("int", Integer.MIN_VALUE));
  }

  public void testCheckPositive_positiveInt() {
    MathPreconditions.checkPositive("int", 1);
  }

  public void testCheckPositive_negativeInt() {
    assertThrows(IllegalArgumentException.class, () -> MathPreconditions.checkPositive("int", -1));
  }

  public void testCheckPositive_zeroLong() {
    assertThrows(IllegalArgumentException.class, () -> MathPreconditions.checkPositive("long", 0L));
  }

  public void testCheckPositive_maxLong() {
    MathPreconditions.checkPositive("long", Long.MAX_VALUE);
  }

  public void testCheckPositive_minLong() {
    assertThrows(
        IllegalArgumentException.class,
        () -> MathPreconditions.checkPositive("long", Long.MIN_VALUE));
  }

  public void testCheckPositive_positiveLong() {
    MathPreconditions.checkPositive("long", 1);
  }

  public void testCheckPositive_negativeLong() {
    assertThrows(
        IllegalArgumentException.class, () -> MathPreconditions.checkPositive("long", -1L));
  }

  public void testCheckPositive_zeroBigInteger() {
    assertThrows(
        IllegalArgumentException.class,
        () -> MathPreconditions.checkPositive("BigInteger", BigInteger.ZERO));
  }

  public void testCheckPositive_positiveBigInteger() {
    MathPreconditions.checkPositive("BigInteger", BigInteger.ONE);
  }

  public void testCheckPositive_negativeBigInteger() {
    assertThrows(
        IllegalArgumentException.class,
        () -> MathPreconditions.checkPositive("BigInteger", BigInteger.ZERO.negate()));
  }

  public void testCheckNonNegative_zeroInt() {
    MathPreconditions.checkNonNegative("int", 0);
  }

  public void testCheckNonNegative_maxInt() {
    MathPreconditions.checkNonNegative("int", Integer.MAX_VALUE);
  }

  public void testCheckNonNegative_minInt() {
    assertThrows(
        IllegalArgumentException.class,
        () -> MathPreconditions.checkNonNegative("int", Integer.MIN_VALUE));
  }

  public void testCheckNonNegative_positiveInt() {
    MathPreconditions.checkNonNegative("int", 1);
  }

  public void testCheckNonNegative_negativeInt() {
    assertThrows(
        IllegalArgumentException.class, () -> MathPreconditions.checkNonNegative("int", -1));
  }

  public void testCheckNonNegative_zeroLong() {
    MathPreconditions.checkNonNegative("long", 0L);
  }

  public void testCheckNonNegative_maxLong() {
    MathPreconditions.checkNonNegative("long", Long.MAX_VALUE);
  }

  public void testCheckNonNegative_minLong() {
    assertThrows(
        IllegalArgumentException.class,
        () -> MathPreconditions.checkNonNegative("long", Long.MIN_VALUE));
  }

  public void testCheckNonNegative_positiveLong() {
    MathPreconditions.checkNonNegative("long", 1L);
  }

  public void testCheckNonNegative_negativeLong() {
    assertThrows(
        IllegalArgumentException.class, () -> MathPreconditions.checkNonNegative("int", -1L));
  }

  public void testCheckNonNegative_zeroBigInteger() {
    MathPreconditions.checkNonNegative("BigInteger", BigInteger.ZERO);
  }

  public void testCheckNonNegative_positiveBigInteger() {
    MathPreconditions.checkNonNegative("BigInteger", BigInteger.ONE);
  }

  public void testCheckNonNegative_negativeBigInteger() {
    assertThrows(
        IllegalArgumentException.class,
        () -> MathPreconditions.checkNonNegative("int", BigInteger.ONE.negate()));
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
    assertThrows(
        IllegalArgumentException.class, () -> MathPreconditions.checkNonNegative("float", -1f));
  }

  public void testCheckNonNegative_nanFloat() {
    assertThrows(
        IllegalArgumentException.class,
        () -> MathPreconditions.checkNonNegative("float", Float.NaN));
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
    assertThrows(
        IllegalArgumentException.class, () -> MathPreconditions.checkNonNegative("double", -1d));
  }

  public void testCheckNonNegative_nanDouble() {
    assertThrows(
        IllegalArgumentException.class,
        () -> MathPreconditions.checkNonNegative("double", Double.NaN));
  }

  public void testCheckRoundingUnnecessary_success() {
    MathPreconditions.checkRoundingUnnecessary(true);
  }

  public void testCheckRoundingUnnecessary_failure() {
    assertThrows(
        ArithmeticException.class, () -> MathPreconditions.checkRoundingUnnecessary(false));
  }

  public void testCheckInRange_success() {
    MathPreconditions.checkInRangeForRoundingInputs(true, 1.0, RoundingMode.UP);
  }

  public void testCheckInRange_failure() {
    ArithmeticException expected =
        assertThrows(
            ArithmeticException.class,
            () -> MathPreconditions.checkInRangeForRoundingInputs(false, 1.0, RoundingMode.UP));
    assertThat(expected).hasMessageThat().contains("1.0");
    assertThat(expected).hasMessageThat().contains("UP");
  }

  public void testCheckNoOverflow_success() {
    MathPreconditions.checkNoOverflow(true, "testCheckNoOverflow_success", 0, 0);
  }

  public void testCheckNoOverflow_failure() {
    ArithmeticException expected =
        assertThrows(
            ArithmeticException.class,
            () -> MathPreconditions.checkNoOverflow(false, "testCheckNoOverflow_failure", 0, 0));
    assertThat(expected).hasMessageThat().contains("testCheckNoOverflow_failure(0, 0)");
  }

  public void testNulls() {
    /*
     * Don't bother testing. All non-primitive parameters are used only to construct error messages.
     * We never want to pass null for them, so we haven't annotated them to say that null is
     * allowed. But at the same time, it seems wasteful to bother inserting the checkNotNull calls
     * that NullPointerTester wants.
     *
     * (This empty method disables the automatic null testing provided by PackageSanityTests.)
     */
  }
}
