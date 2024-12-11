/*
 * Copyright (C) 2006 The Guava Authors
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

package com.google.common.base;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkElementIndex;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkPositionIndex;
import static com.google.common.base.Preconditions.checkPositionIndexes;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.base.ReflectionFreeAssertThrows.assertThrows;
import static com.google.common.truth.Truth.assertThat;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.annotations.J2ktIncompatible;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.testing.ArbitraryInstances;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import junit.framework.AssertionFailedError;
import junit.framework.TestCase;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Unit test for {@link Preconditions}.
 *
 * @author Kevin Bourrillion
 * @author Jared Levy
 */
@ElementTypesAreNonnullByDefault
@SuppressWarnings("LenientFormatStringValidation") // Intentional for testing
@GwtCompatible(emulated = true)
public class PreconditionsTest extends TestCase {
  public void testCheckArgument_simple_success() {
    checkArgument(true);
  }

  public void testCheckArgument_simple_failure() {
    assertThrows(IllegalArgumentException.class, () -> checkArgument(false));
  }

  public void testCheckArgument_simpleMessage_success() {
    checkArgument(true, IGNORE_ME);
  }

  public void testCheckArgument_simpleMessage_failure() {
    IllegalArgumentException expected =
        assertThrows(IllegalArgumentException.class, () -> checkArgument(false, new Message()));
    verifySimpleMessage(expected);
  }

  public void testCheckArgument_nullMessage_failure() {
    IllegalArgumentException expected =
        assertThrows(IllegalArgumentException.class, () -> checkArgument(false, null));
    assertThat(expected).hasMessageThat().isEqualTo("null");
  }

  public void testCheckArgument_nullMessageWithArgs_failure() {
    IllegalArgumentException e =
        assertThrows(IllegalArgumentException.class, () -> checkArgument(false, null, "b", "d"));
    assertThat(e).hasMessageThat().isEqualTo("null [b, d]");
  }

  public void testCheckArgument_nullArgs_failure() {
    IllegalArgumentException e =
        assertThrows(
            IllegalArgumentException.class, () -> checkArgument(false, "A %s C %s E", null, null));
    assertThat(e).hasMessageThat().isEqualTo("A null C null E");
  }

  public void testCheckArgument_notEnoughArgs_failure() {
    IllegalArgumentException e =
        assertThrows(
            IllegalArgumentException.class, () -> checkArgument(false, "A %s C %s E", "b"));
    assertThat(e).hasMessageThat().isEqualTo("A b C %s E");
  }

  public void testCheckArgument_tooManyArgs_failure() {
    IllegalArgumentException e =
        assertThrows(
            IllegalArgumentException.class,
            () -> checkArgument(false, "A %s C %s E", "b", "d", "f"));
    assertThat(e).hasMessageThat().isEqualTo("A b C d E [f]");
  }

  public void testCheckArgument_singleNullArg_failure() {
    IllegalArgumentException e =
        assertThrows(
            IllegalArgumentException.class, () -> checkArgument(false, "A %s C", (Object) null));
    assertThat(e).hasMessageThat().isEqualTo("A null C");
  }

  @J2ktIncompatible // TODO(b/319404022): Allow passing null array as varargs
  public void testCheckArgument_singleNullArray_failure() {
    IllegalArgumentException e =
        assertThrows(
            IllegalArgumentException.class, () -> checkArgument(false, "A %s C", (Object[]) null));
    assertThat(e).hasMessageThat().isEqualTo("A (Object[])null C");
  }

  public void testCheckArgument_complexMessage_success() {
    checkArgument(true, "%s", IGNORE_ME);
  }

  public void testCheckArgument_complexMessage_failure() {
    IllegalArgumentException expected =
        assertThrows(IllegalArgumentException.class, () -> checkArgument(false, FORMAT, 5));
    verifyComplexMessage(expected);
  }

  public void testCheckState_simple_success() {
    checkState(true);
  }

  public void testCheckState_simple_failure() {
    assertThrows(IllegalStateException.class, () -> checkState(false));
  }

  public void testCheckState_simpleMessage_success() {
    checkState(true, IGNORE_ME);
  }

  public void testCheckState_simpleMessage_failure() {
    IllegalStateException expected =
        assertThrows(IllegalStateException.class, () -> checkState(false, new Message()));
    verifySimpleMessage(expected);
  }

  public void testCheckState_nullMessage_failure() {
    IllegalStateException expected =
        assertThrows(IllegalStateException.class, () -> checkState(false, null));
    assertThat(expected).hasMessageThat().isEqualTo("null");
  }

  public void testCheckState_complexMessage_success() {
    checkState(true, "%s", IGNORE_ME);
  }

  public void testCheckState_complexMessage_failure() {
    IllegalStateException expected =
        assertThrows(IllegalStateException.class, () -> checkState(false, FORMAT, 5));
    verifyComplexMessage(expected);
  }

  private static final String NON_NULL_STRING = "foo";

  public void testCheckNotNull_simple_success() {
    String result = checkNotNull(NON_NULL_STRING);
    assertSame(NON_NULL_STRING, result);
  }

  public void testCheckNotNull_simple_failure() {
    assertThrows(NullPointerException.class, () -> checkNotNull(null));
  }

  public void testCheckNotNull_simpleMessage_success() {
    String result = checkNotNull(NON_NULL_STRING, IGNORE_ME);
    assertSame(NON_NULL_STRING, result);
  }

  public void testCheckNotNull_simpleMessage_failure() {
    NullPointerException expected =
        assertThrows(NullPointerException.class, () -> checkNotNull(null, new Message()));
    verifySimpleMessage(expected);
  }

  public void testCheckNotNull_complexMessage_success() {
    String result = checkNotNull(NON_NULL_STRING, "%s", IGNORE_ME);
    assertSame(NON_NULL_STRING, result);
  }

  public void testCheckNotNull_complexMessage_failure() {
    NullPointerException expected =
        assertThrows(NullPointerException.class, () -> checkNotNull(null, FORMAT, 5));
    verifyComplexMessage(expected);
  }

  public void testCheckElementIndex_ok() {
    assertEquals(0, checkElementIndex(0, 1));
    assertEquals(0, checkElementIndex(0, 2));
    assertEquals(1, checkElementIndex(1, 2));
  }

  public void testCheckElementIndex_badSize() {
    assertThrows(IllegalArgumentException.class, () -> checkElementIndex(1, -1));
  }

  public void testCheckElementIndex_negative() {
    IndexOutOfBoundsException expected =
        assertThrows(IndexOutOfBoundsException.class, () -> checkElementIndex(-1, 1));
    assertThat(expected).hasMessageThat().isEqualTo("index (-1) must not be negative");
  }

  public void testCheckElementIndex_tooHigh() {
    IndexOutOfBoundsException expected =
        assertThrows(IndexOutOfBoundsException.class, () -> checkElementIndex(1, 1));
    assertThat(expected).hasMessageThat().isEqualTo("index (1) must be less than size (1)");
  }

  public void testCheckElementIndex_withDesc_negative() {
    IndexOutOfBoundsException expected =
        assertThrows(IndexOutOfBoundsException.class, () -> checkElementIndex(-1, 1, "foo"));
    assertThat(expected).hasMessageThat().isEqualTo("foo (-1) must not be negative");
  }

  public void testCheckElementIndex_withDesc_tooHigh() {
    IndexOutOfBoundsException expected =
        assertThrows(IndexOutOfBoundsException.class, () -> checkElementIndex(1, 1, "foo"));
    assertThat(expected).hasMessageThat().isEqualTo("foo (1) must be less than size (1)");
  }

  public void testCheckPositionIndex_ok() {
    assertEquals(0, checkPositionIndex(0, 0));
    assertEquals(0, checkPositionIndex(0, 1));
    assertEquals(1, checkPositionIndex(1, 1));
  }

  public void testCheckPositionIndex_badSize() {
    assertThrows(IllegalArgumentException.class, () -> checkPositionIndex(1, -1));
  }

  public void testCheckPositionIndex_negative() {
    IndexOutOfBoundsException expected =
        assertThrows(IndexOutOfBoundsException.class, () -> checkPositionIndex(-1, 1));
    assertThat(expected).hasMessageThat().isEqualTo("index (-1) must not be negative");
  }

  public void testCheckPositionIndex_tooHigh() {
    IndexOutOfBoundsException expected =
        assertThrows(IndexOutOfBoundsException.class, () -> checkPositionIndex(2, 1));
    assertThat(expected).hasMessageThat().isEqualTo("index (2) must not be greater than size (1)");
  }

  public void testCheckPositionIndex_withDesc_negative() {
    IndexOutOfBoundsException expected =
        assertThrows(IndexOutOfBoundsException.class, () -> checkPositionIndex(-1, 1, "foo"));
    assertThat(expected).hasMessageThat().isEqualTo("foo (-1) must not be negative");
  }

  public void testCheckPositionIndex_withDesc_tooHigh() {
    IndexOutOfBoundsException expected =
        assertThrows(IndexOutOfBoundsException.class, () -> checkPositionIndex(2, 1, "foo"));
    assertThat(expected).hasMessageThat().isEqualTo("foo (2) must not be greater than size (1)");
  }

  public void testCheckPositionIndexes_ok() {
    checkPositionIndexes(0, 0, 0);
    checkPositionIndexes(0, 0, 1);
    checkPositionIndexes(0, 1, 1);
    checkPositionIndexes(1, 1, 1);
  }

  public void testCheckPositionIndexes_badSize() {
    assertThrows(IllegalArgumentException.class, () -> checkPositionIndexes(1, 1, -1));
  }

  public void testCheckPositionIndex_startNegative() {
    IndexOutOfBoundsException expected =
        assertThrows(IndexOutOfBoundsException.class, () -> checkPositionIndexes(-1, 1, 1));
    assertThat(expected).hasMessageThat().isEqualTo("start index (-1) must not be negative");
  }

  public void testCheckPositionIndexes_endTooHigh() {
    IndexOutOfBoundsException expected =
        assertThrows(IndexOutOfBoundsException.class, () -> checkPositionIndexes(0, 2, 1));
    assertThat(expected)
        .hasMessageThat()
        .isEqualTo("end index (2) must not be greater than size (1)");
  }

  public void testCheckPositionIndexes_reversed() {
    IndexOutOfBoundsException expected =
        assertThrows(IndexOutOfBoundsException.class, () -> checkPositionIndexes(1, 0, 1));
    assertThat(expected)
        .hasMessageThat()
        .isEqualTo("end index (0) must not be less than start index (1)");
  }

  @GwtIncompatible("Reflection")
  @J2ktIncompatible
  public void testAllOverloads_checkArgument() throws Exception {
    for (ImmutableList<Class<?>> sig : allSignatures(boolean.class)) {
      Method checkArgumentMethod =
          Preconditions.class.getMethod("checkArgument", sig.toArray(new Class<?>[] {}));
      checkArgumentMethod.invoke(null /* static method */, getParametersForSignature(true, sig));

      Object[] failingParams = getParametersForSignature(false, sig);
      InvocationTargetException ite =
          assertThrows(
              InvocationTargetException.class,
              () -> checkArgumentMethod.invoke(null /* static method */, failingParams));
      assertFailureCause(ite.getCause(), IllegalArgumentException.class, failingParams);
    }
  }

  @GwtIncompatible("Reflection")
  @J2ktIncompatible
  public void testAllOverloads_checkState() throws Exception {
    for (ImmutableList<Class<?>> sig : allSignatures(boolean.class)) {
      Method checkArgumentMethod =
          Preconditions.class.getMethod("checkState", sig.toArray(new Class<?>[] {}));
      checkArgumentMethod.invoke(null /* static method */, getParametersForSignature(true, sig));

      Object[] failingParams = getParametersForSignature(false, sig);
      InvocationTargetException ite =
          assertThrows(
              InvocationTargetException.class,
              () -> checkArgumentMethod.invoke(null /* static method */, failingParams));
      assertFailureCause(ite.getCause(), IllegalStateException.class, failingParams);
    }
  }

  @GwtIncompatible("Reflection")
  @J2ktIncompatible
  public void testAllOverloads_checkNotNull() throws Exception {
    for (ImmutableList<Class<?>> sig : allSignatures(Object.class)) {
      Method checkArgumentMethod =
          Preconditions.class.getMethod("checkNotNull", sig.toArray(new Class<?>[] {}));
      checkArgumentMethod.invoke(
          null /* static method */, getParametersForSignature(new Object(), sig));

      Object[] failingParams = getParametersForSignature(null, sig);
      InvocationTargetException ite =
          assertThrows(
              InvocationTargetException.class,
              () -> checkArgumentMethod.invoke(null /* static method */, failingParams));
      assertFailureCause(ite.getCause(), NullPointerException.class, failingParams);
    }
  }

  /**
   * Asserts that the given throwable has the given class and then asserts on the message as using
   * the full set of method parameters.
   */
  private void assertFailureCause(
      Throwable throwable, Class<? extends Throwable> clazz, Object[] params) {
    assertThat(throwable).isInstanceOf(clazz);
    if (params.length == 1) {
      assertThat(throwable).hasMessageThat().isNull();
    } else if (params.length == 2) {
      assertThat(throwable).hasMessageThat().isEmpty();
    } else {
      assertThat(throwable)
          .hasMessageThat()
          .isEqualTo(Strings.lenientFormat("", Arrays.copyOfRange(params, 2, params.length)));
    }
  }

  /**
   * Returns an array containing parameters for invoking a checkArgument, checkNotNull or checkState
   * method reflectively
   *
   * @param firstParam The first parameter
   * @param sig The method signature
   */
  @GwtIncompatible("ArbitraryInstances")
  @J2ktIncompatible
  private Object[] getParametersForSignature(
      @Nullable Object firstParam, ImmutableList<Class<?>> sig) {
    Object[] params = new Object[sig.size()];
    params[0] = firstParam;
    if (params.length > 1) {
      params[1] = "";
      if (params.length > 2) {
        // fill in the rest of the array with arbitrary instances
        for (int i = 2; i < params.length; i++) {
          params[i] = ArbitraryInstances.get(sig.get(i));
        }
      }
    }
    return params;
  }

  private static final ImmutableList<Class<?>> possibleParamTypes =
      ImmutableList.of(char.class, int.class, long.class, Object.class);

  /**
   * Returns a list of parameters for invoking an overload of checkState, checkArgument or
   * checkNotNull
   *
   * @param predicateType The first parameter to the method (boolean or Object)
   */
  private static ImmutableList<ImmutableList<Class<?>>> allSignatures(Class<?> predicateType) {
    ImmutableSet.Builder<ImmutableList<Class<?>>> allOverloads = ImmutableSet.builder();
    // The first two are for the overloads that don't take formatting args, e.g.
    // checkArgument(boolean) and checkArgument(boolean, Object)
    allOverloads.add(ImmutableList.<Class<?>>of(predicateType));
    allOverloads.add(ImmutableList.<Class<?>>of(predicateType, Object.class));

    List<List<Class<?>>> typesLists = new ArrayList<>();
    for (int i = 0; i < 2; i++) {
      typesLists.add(possibleParamTypes);
      for (List<Class<?>> curr : Lists.cartesianProduct(typesLists)) {
        allOverloads.add(
            ImmutableList.<Class<?>>builder()
                .add(predicateType)
                .add(String.class) // the format string
                .addAll(curr)
                .build());
      }
    }
    return allOverloads.build().asList();
  }

  // 'test' to demonstrate some potentially ambiguous overloads.  This 'test' is kind of strange,
  // but essentially each line will be a call to a Preconditions method that, but for a documented
  // change would be a compiler error.
  // See http://docs.oracle.com/javase/specs/jls/se7/html/jls-15.html#jls-15.12.2 for the spec on
  // how javac selects overloads
  @SuppressWarnings("null")
  public void overloadSelection() {
    Boolean boxedBoolean = null;
    boolean aBoolean = true;
    Long boxedLong = null;
    int anInt = 1;
    // With a boxed predicate, no overloads can be selected in phase 1
    // ambiguous without the call to .booleanValue to unbox the Boolean
    checkState(boxedBoolean.booleanValue(), "", 1);
    // ambiguous without the cast to Object because the boxed predicate prevents any overload from
    // being selected in phase 1
    checkState(boxedBoolean, "", (Object) boxedLong);

    // ternaries introduce their own problems. because of the ternary (which requires a boxing
    // operation) no overload can be selected in phase 1.  and in phase 2 it is ambiguous since it
    // matches with the second parameter being boxed and without it being boxed.  The cast to Object
    // avoids this.
    checkState(aBoolean, "", aBoolean ? "" : anInt, (Object) anInt);

    // ambiguous without the .booleanValue() call since the boxing forces us into phase 2 resolution
    short s = 2;
    checkState(boxedBoolean.booleanValue(), "", s);
  }

  @J2ktIncompatible
  @GwtIncompatible // NullPointerTester
  public void testNullPointers() {
    /*
     * Don't bother testing: Preconditions defines a bunch of methods that accept a template (or
     * even entire message) that simultaneously:
     *
     * - _shouldn't_ be null, so we don't annotate it with @Nullable
     *
     * - _can_ be null without causing a runtime failure (because we don't want the interesting
     *   details of precondition failure to be hidden by an exception we throw about an unexpectedly
     *   null _failure message_)
     *
     * That combination upsets NullPointerTester, which wants any call that passes null for a
     * non-@Nullable parameter to trigger a NullPointerException.
     *
     * (We still define this empty method to keep PackageSanityTests from generating its own
     * automated nullness tests, which would fail.)
     */
  }

  private static final Object IGNORE_ME =
      new Object() {
        @Override
        public String toString() {
          throw new AssertionFailedError();
        }
      };

  private static class Message {
    boolean invoked;

    @Override
    public String toString() {
      assertFalse(invoked);
      invoked = true;
      return "A message";
    }
  }

  private static final String FORMAT = "I ate %s pies.";

  private static void verifySimpleMessage(Exception e) {
    assertThat(e).hasMessageThat().isEqualTo("A message");
  }

  private static void verifyComplexMessage(Exception e) {
    assertThat(e).hasMessageThat().isEqualTo("I ate 5 pies.");
  }
}
