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

import static com.google.common.truth.Truth.assertThat;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
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

/**
 * Unit test for {@link Preconditions}.
 *
 * @author Kevin Bourrillion
 * @author Jared Levy
 */
@GwtCompatible(emulated = true)
public class PreconditionsTest extends TestCase {
  public void testCheckArgument_simple_success() {
    Preconditions.checkArgument(true);
  }

  public void testCheckArgument_simple_failure() {
    try {
      Preconditions.checkArgument(false);
      fail("no exception thrown");
    } catch (IllegalArgumentException expected) {
    }
  }

  public void testCheckArgument_simpleMessage_success() {
    Preconditions.checkArgument(true, IGNORE_ME);
  }

  public void testCheckArgument_simpleMessage_failure() {
    try {
      Preconditions.checkArgument(false, new Message());
      fail("no exception thrown");
    } catch (IllegalArgumentException expected) {
      verifySimpleMessage(expected);
    }
  }

  public void testCheckArgument_nullMessage_failure() {
    try {
      Preconditions.checkArgument(false, null);
      fail("no exception thrown");
    } catch (IllegalArgumentException expected) {
      assertThat(expected).hasMessageThat().isEqualTo("null");
    }
  }

  public void testCheckArgument_nullMessageWithArgs_failure() {
    try {
      Preconditions.checkArgument(false, null, "b", "d");
      fail("no exception thrown");
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessageThat().isEqualTo("null [b, d]");
    }
  }

  public void testCheckArgument_nullArgs_failure() {
    try {
      Preconditions.checkArgument(false, "A %s C %s E", null, null);
      fail("no exception thrown");
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessageThat().isEqualTo("A null C null E");
    }
  }

  public void testCheckArgument_notEnoughArgs_failure() {
    try {
      Preconditions.checkArgument(false, "A %s C %s E", "b");
      fail("no exception thrown");
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessageThat().isEqualTo("A b C %s E");
    }
  }

  public void testCheckArgument_tooManyArgs_failure() {
    try {
      Preconditions.checkArgument(false, "A %s C %s E", "b", "d", "f");
      fail("no exception thrown");
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessageThat().isEqualTo("A b C d E [f]");
    }
  }

  public void testCheckArgument_singleNullArg_failure() {
    try {
      Preconditions.checkArgument(false, "A %s C", (Object) null);
      fail("no exception thrown");
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessageThat().isEqualTo("A null C");
    }
  }

  public void testCheckArgument_singleNullArray_failure() {
    try {
      Preconditions.checkArgument(false, "A %s C", (Object[]) null);
      fail("no exception thrown");
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessageThat().isEqualTo("A (Object[])null C");
    }
  }

  public void testCheckArgument_complexMessage_success() {
    Preconditions.checkArgument(true, "%s", IGNORE_ME);
  }

  public void testCheckArgument_complexMessage_failure() {
    try {
      Preconditions.checkArgument(false, FORMAT, 5);
      fail("no exception thrown");
    } catch (IllegalArgumentException expected) {
      verifyComplexMessage(expected);
    }
  }

  public void testCheckState_simple_success() {
    Preconditions.checkState(true);
  }

  public void testCheckState_simple_failure() {
    try {
      Preconditions.checkState(false);
      fail("no exception thrown");
    } catch (IllegalStateException expected) {
    }
  }

  public void testCheckState_simpleMessage_success() {
    Preconditions.checkState(true, IGNORE_ME);
  }

  public void testCheckState_simpleMessage_failure() {
    try {
      Preconditions.checkState(false, new Message());
      fail("no exception thrown");
    } catch (IllegalStateException expected) {
      verifySimpleMessage(expected);
    }
  }

  public void testCheckState_nullMessage_failure() {
    try {
      Preconditions.checkState(false, null);
      fail("no exception thrown");
    } catch (IllegalStateException expected) {
      assertThat(expected).hasMessageThat().isEqualTo("null");
    }
  }

  public void testCheckState_complexMessage_success() {
    Preconditions.checkState(true, "%s", IGNORE_ME);
  }

  public void testCheckState_complexMessage_failure() {
    try {
      Preconditions.checkState(false, FORMAT, 5);
      fail("no exception thrown");
    } catch (IllegalStateException expected) {
      verifyComplexMessage(expected);
    }
  }

  private static final String NON_NULL_STRING = "foo";

  public void testCheckNotNull_simple_success() {
    String result = Preconditions.checkNotNull(NON_NULL_STRING);
    assertSame(NON_NULL_STRING, result);
  }

  public void testCheckNotNull_simple_failure() {
    try {
      Preconditions.checkNotNull(null);
      fail("no exception thrown");
    } catch (NullPointerException expected) {
    }
  }

  public void testCheckNotNull_simpleMessage_success() {
    String result = Preconditions.checkNotNull(NON_NULL_STRING, IGNORE_ME);
    assertSame(NON_NULL_STRING, result);
  }

  public void testCheckNotNull_simpleMessage_failure() {
    try {
      Preconditions.checkNotNull(null, new Message());
      fail("no exception thrown");
    } catch (NullPointerException expected) {
      verifySimpleMessage(expected);
    }
  }

  public void testCheckNotNull_complexMessage_success() {
    String result = Preconditions.checkNotNull(NON_NULL_STRING, "%s", IGNORE_ME);
    assertSame(NON_NULL_STRING, result);
  }

  public void testCheckNotNull_complexMessage_failure() {
    try {
      Preconditions.checkNotNull(null, FORMAT, 5);
      fail("no exception thrown");
    } catch (NullPointerException expected) {
      verifyComplexMessage(expected);
    }
  }

  public void testCheckElementIndex_ok() {
    assertEquals(0, Preconditions.checkElementIndex(0, 1));
    assertEquals(0, Preconditions.checkElementIndex(0, 2));
    assertEquals(1, Preconditions.checkElementIndex(1, 2));
  }

  public void testCheckElementIndex_badSize() {
    try {
      Preconditions.checkElementIndex(1, -1);
      fail();
    } catch (IllegalArgumentException expected) {
      // don't care what the message text is, as this is an invalid usage of
      // the Preconditions class, unlike all the other exceptions it throws
    }
  }

  public void testCheckElementIndex_negative() {
    try {
      Preconditions.checkElementIndex(-1, 1);
      fail();
    } catch (IndexOutOfBoundsException expected) {
      assertThat(expected).hasMessageThat().isEqualTo("index (-1) must not be negative");
    }
  }

  public void testCheckElementIndex_tooHigh() {
    try {
      Preconditions.checkElementIndex(1, 1);
      fail();
    } catch (IndexOutOfBoundsException expected) {
      assertThat(expected).hasMessageThat().isEqualTo("index (1) must be less than size (1)");
    }
  }

  public void testCheckElementIndex_withDesc_negative() {
    try {
      Preconditions.checkElementIndex(-1, 1, "foo");
      fail();
    } catch (IndexOutOfBoundsException expected) {
      assertThat(expected).hasMessageThat().isEqualTo("foo (-1) must not be negative");
    }
  }

  public void testCheckElementIndex_withDesc_tooHigh() {
    try {
      Preconditions.checkElementIndex(1, 1, "foo");
      fail();
    } catch (IndexOutOfBoundsException expected) {
      assertThat(expected).hasMessageThat().isEqualTo("foo (1) must be less than size (1)");
    }
  }

  public void testCheckPositionIndex_ok() {
    assertEquals(0, Preconditions.checkPositionIndex(0, 0));
    assertEquals(0, Preconditions.checkPositionIndex(0, 1));
    assertEquals(1, Preconditions.checkPositionIndex(1, 1));
  }

  public void testCheckPositionIndex_badSize() {
    try {
      Preconditions.checkPositionIndex(1, -1);
      fail();
    } catch (IllegalArgumentException expected) {
      // don't care what the message text is, as this is an invalid usage of
      // the Preconditions class, unlike all the other exceptions it throws
    }
  }

  public void testCheckPositionIndex_negative() {
    try {
      Preconditions.checkPositionIndex(-1, 1);
      fail();
    } catch (IndexOutOfBoundsException expected) {
      assertThat(expected).hasMessageThat().isEqualTo("index (-1) must not be negative");
    }
  }

  public void testCheckPositionIndex_tooHigh() {
    try {
      Preconditions.checkPositionIndex(2, 1);
      fail();
    } catch (IndexOutOfBoundsException expected) {
      assertThat(expected)
          .hasMessageThat()
          .isEqualTo("index (2) must not be greater than size (1)");
    }
  }

  public void testCheckPositionIndex_withDesc_negative() {
    try {
      Preconditions.checkPositionIndex(-1, 1, "foo");
      fail();
    } catch (IndexOutOfBoundsException expected) {
      assertThat(expected).hasMessageThat().isEqualTo("foo (-1) must not be negative");
    }
  }

  public void testCheckPositionIndex_withDesc_tooHigh() {
    try {
      Preconditions.checkPositionIndex(2, 1, "foo");
      fail();
    } catch (IndexOutOfBoundsException expected) {
      assertThat(expected).hasMessageThat().isEqualTo("foo (2) must not be greater than size (1)");
    }
  }

  public void testCheckPositionIndexes_ok() {
    Preconditions.checkPositionIndexes(0, 0, 0);
    Preconditions.checkPositionIndexes(0, 0, 1);
    Preconditions.checkPositionIndexes(0, 1, 1);
    Preconditions.checkPositionIndexes(1, 1, 1);
  }

  public void testCheckPositionIndexes_badSize() {
    try {
      Preconditions.checkPositionIndexes(1, 1, -1);
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }

  public void testCheckPositionIndex_startNegative() {
    try {
      Preconditions.checkPositionIndexes(-1, 1, 1);
      fail();
    } catch (IndexOutOfBoundsException expected) {
      assertThat(expected).hasMessageThat().isEqualTo("start index (-1) must not be negative");
    }
  }

  public void testCheckPositionIndexes_endTooHigh() {
    try {
      Preconditions.checkPositionIndexes(0, 2, 1);
      fail();
    } catch (IndexOutOfBoundsException expected) {
      assertThat(expected)
          .hasMessageThat()
          .isEqualTo("end index (2) must not be greater than size (1)");
    }
  }

  public void testCheckPositionIndexes_reversed() {
    try {
      Preconditions.checkPositionIndexes(1, 0, 1);
      fail();
    } catch (IndexOutOfBoundsException expected) {
      assertThat(expected)
          .hasMessageThat()
          .isEqualTo("end index (0) must not be less than start index (1)");
    }
  }

  @GwtIncompatible("Reflection")
  public void testAllOverloads_checkArgument() throws Exception {
    for (ImmutableList<Class<?>> sig : allSignatures(boolean.class)) {
      Method checkArgumentMethod =
          Preconditions.class.getMethod("checkArgument", sig.toArray(new Class<?>[] {}));
      checkArgumentMethod.invoke(null /* static method */, getParametersForSignature(true, sig));

      Object[] failingParams = getParametersForSignature(false, sig);
      try {
        checkArgumentMethod.invoke(null /* static method */, failingParams);
        fail();
      } catch (InvocationTargetException ite) {
        assertFailureCause(ite.getCause(), IllegalArgumentException.class, failingParams);
      }
    }
  }

  @GwtIncompatible("Reflection")
  public void testAllOverloads_checkState() throws Exception {
    for (ImmutableList<Class<?>> sig : allSignatures(boolean.class)) {
      Method checkArgumentMethod =
          Preconditions.class.getMethod("checkState", sig.toArray(new Class<?>[] {}));
      checkArgumentMethod.invoke(null /* static method */, getParametersForSignature(true, sig));

      Object[] failingParams = getParametersForSignature(false, sig);
      try {
        checkArgumentMethod.invoke(null /* static method */, failingParams);
        fail();
      } catch (InvocationTargetException ite) {
        assertFailureCause(ite.getCause(), IllegalStateException.class, failingParams);
      }
    }
  }

  @GwtIncompatible("Reflection")
  public void testAllOverloads_checkNotNull() throws Exception {
    for (ImmutableList<Class<?>> sig : allSignatures(Object.class)) {
      Method checkArgumentMethod =
          Preconditions.class.getMethod("checkNotNull", sig.toArray(new Class<?>[] {}));
      checkArgumentMethod.invoke(
          null /* static method */, getParametersForSignature(new Object(), sig));

      Object[] failingParams = getParametersForSignature(null, sig);
      try {
        checkArgumentMethod.invoke(null /* static method */, failingParams);
        fail();
      } catch (InvocationTargetException ite) {
        assertFailureCause(ite.getCause(), NullPointerException.class, failingParams);
      }
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
  private Object[] getParametersForSignature(Object firstParam, ImmutableList<Class<?>> sig) {
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
    Preconditions.checkState(boxedBoolean.booleanValue(), "", 1);
    // ambiguous without the cast to Object because the boxed predicate prevents any overload from
    // being selected in phase 1
    Preconditions.checkState(boxedBoolean, "", (Object) boxedLong);

    // ternaries introduce their own problems. because of the ternary (which requires a boxing
    // operation) no overload can be selected in phase 1.  and in phase 2 it is ambiguous since it
    // matches with the second parameter being boxed and without it being boxed.  The cast to Object
    // avoids this.
    Preconditions.checkState(aBoolean, "", aBoolean ? "" : anInt, (Object) anInt);

    // ambiguous without the .booleanValue() call since the boxing forces us into phase 2 resolution
    short s = 2;
    Preconditions.checkState(boxedBoolean.booleanValue(), "", s);
  }

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
