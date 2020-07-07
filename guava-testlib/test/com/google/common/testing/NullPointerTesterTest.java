/*
 * Copyright (C) 2005 The Guava Authors
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

package com.google.common.testing;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.truth.Truth.assertThat;

import com.google.common.base.Converter;
import com.google.common.base.Function;
import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableMultiset;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.ImmutableTable;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multiset;
import com.google.common.collect.Table;
import com.google.common.reflect.TypeToken;
import com.google.common.testing.NullPointerTester.Visibility;
import com.google.common.testing.anotherpackage.SomeClassThatDoesNotUseNullable;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import junit.framework.AssertionFailedError;
import junit.framework.TestCase;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Unit test for {@link NullPointerTester}.
 *
 * @author Kevin Bourrillion
 * @author Mick Killianey
 */
public class NullPointerTesterTest extends TestCase {

  /** Non-NPE RuntimeException. */
  public static class FooException extends RuntimeException {
    private static final long serialVersionUID = 1L;
  }

  /**
   * Class for testing all permutations of static/non-static one-argument methods using
   * methodParameter().
   */
  @SuppressWarnings("unused") // used by reflection
  public static class OneArg {

    public static void staticOneArgCorrectlyThrowsNpe(String s) {
      checkNotNull(s); // expect NPE here on null
    }

    public static void staticOneArgThrowsOtherThanNpe(String s) {
      throw new FooException(); // should catch as failure
    }

    public static void staticOneArgShouldThrowNpeButDoesnt(String s) {
      // should catch as failure
    }

    public static void staticOneArgCheckForNullCorrectlyDoesNotThrowNPE(
        @javax.annotation.CheckForNull String s) {
      // null?  no problem
    }

    public static void staticOneArgJsr305NullableCorrectlyDoesNotThrowNPE(
        @javax.annotation.Nullable String s) {
      // null?  no problem
    }

    public static void staticOneArgNullableCorrectlyDoesNotThrowNPE(@Nullable String s) {
      // null?  no problem
    }

    public static void staticOneArgCheckForNullCorrectlyThrowsOtherThanNPE(
        @javax.annotation.CheckForNull String s) {
      throw new FooException(); // ok, as long as it's not NullPointerException
    }

    public static void staticOneArgNullableCorrectlyThrowsOtherThanNPE(@Nullable String s) {
      throw new FooException(); // ok, as long as it's not NullPointerException
    }

    public static void staticOneArgCheckForNullThrowsNPE(@javax.annotation.CheckForNull String s) {
      checkNotNull(s); // doesn't check if you said you'd accept null, but you don't
    }

    public static void staticOneArgNullableThrowsNPE(@Nullable String s) {
      checkNotNull(s); // doesn't check if you said you'd accept null, but you don't
    }

    public void oneArgCorrectlyThrowsNpe(String s) {
      checkNotNull(s); // expect NPE here on null
    }

    public void oneArgThrowsOtherThanNpe(String s) {
      throw new FooException(); // should catch as failure
    }

    public void oneArgShouldThrowNpeButDoesnt(String s) {
      // should catch as failure
    }

    public void oneArgCheckForNullCorrectlyDoesNotThrowNPE(
        @javax.annotation.CheckForNull String s) {
      // null?  no problem
    }

    public void oneArgNullableCorrectlyDoesNotThrowNPE(@Nullable String s) {
      // null?  no problem
    }

    public void oneArgCheckForNullCorrectlyThrowsOtherThanNPE(
        @javax.annotation.CheckForNull String s) {
      throw new FooException(); // ok, as long as it's not NullPointerException
    }

    public void oneArgNullableCorrectlyThrowsOtherThanNPE(@Nullable String s) {
      throw new FooException(); // ok, as long as it's not NullPointerException
    }

    public void oneArgCheckForNullThrowsNPE(@javax.annotation.CheckForNull String s) {
      checkNotNull(s); // doesn't check if you said you'd accept null, but you don't
    }

    public void oneArgNullableThrowsNPE(@Nullable String s) {
      checkNotNull(s); // doesn't check if you said you'd accept null, but you don't
    }
  }

  private static final String[] STATIC_ONE_ARG_METHODS_SHOULD_PASS = {
    "staticOneArgCorrectlyThrowsNpe",
    "staticOneArgCheckForNullCorrectlyDoesNotThrowNPE",
    "staticOneArgCheckForNullCorrectlyThrowsOtherThanNPE",
    "staticOneArgCheckForNullThrowsNPE",
    "staticOneArgNullableCorrectlyDoesNotThrowNPE",
    "staticOneArgNullableCorrectlyThrowsOtherThanNPE",
    "staticOneArgNullableThrowsNPE",
  };
  private static final String[] STATIC_ONE_ARG_METHODS_SHOULD_FAIL = {
    "staticOneArgThrowsOtherThanNpe", "staticOneArgShouldThrowNpeButDoesnt",
  };
  private static final String[] NONSTATIC_ONE_ARG_METHODS_SHOULD_PASS = {
    "oneArgCorrectlyThrowsNpe",
    "oneArgCheckForNullCorrectlyDoesNotThrowNPE",
    "oneArgCheckForNullCorrectlyThrowsOtherThanNPE",
    "oneArgCheckForNullThrowsNPE",
    "oneArgNullableCorrectlyDoesNotThrowNPE",
    "oneArgNullableCorrectlyThrowsOtherThanNPE",
    "oneArgNullableThrowsNPE",
  };
  private static final String[] NONSTATIC_ONE_ARG_METHODS_SHOULD_FAIL = {
    "oneArgThrowsOtherThanNpe", "oneArgShouldThrowNpeButDoesnt",
  };

  private static class ThrowsIae {
    public static void christenPoodle(String name) {
      checkArgument(name != null);
    }
  }

  private static class ThrowsNpe {
    public static void christenPoodle(String name) {
      checkNotNull(name);
    }
  }

  private static class ThrowsUoe {
    public static void christenPoodle(String name) {
      throw new UnsupportedOperationException();
    }
  }

  private static class ThrowsSomethingElse {
    public static void christenPoodle(String name) {
      throw new RuntimeException();
    }
  }

  private interface InterfaceStaticMethodFailsToCheckNull {
    static String create(String s) {
      return "I don't check";
    }
  }

  private interface InterfaceStaticMethodChecksNull {
    static String create(String s) {
      return checkNotNull(s);
    }
  }

  private interface InterfaceDefaultMethodFailsToCheckNull {
    static InterfaceDefaultMethodFailsToCheckNull create() {
      return new InterfaceDefaultMethodFailsToCheckNull() {};
    }

    default void doNotCheckNull(String s) {}
  }

  private interface InterfaceDefaultMethodChecksNull {
    static InterfaceDefaultMethodChecksNull create() {
      return new InterfaceDefaultMethodChecksNull() {};
    }

    default void checksNull(String s) {
      checkNotNull(s);
    }
  }

  public void testInterfaceStaticMethod() {
    NullPointerTester tester = new NullPointerTester();
    tester.testAllPublicStaticMethods(InterfaceStaticMethodChecksNull.class);
    try {
      tester.testAllPublicStaticMethods(InterfaceStaticMethodFailsToCheckNull.class);
    } catch (AssertionError expected) {
      return;
    }
    fail();
  }

  public void testInterfaceDefaultMethod() {
    NullPointerTester tester = new NullPointerTester();
    tester.testAllPublicInstanceMethods(InterfaceDefaultMethodChecksNull.create());
    try {
      tester.testAllPublicInstanceMethods(InterfaceDefaultMethodFailsToCheckNull.create());
    } catch (AssertionError expected) {
      return;
    }
    fail();
  }

  public void testDontAcceptIae() {
    NullPointerTester tester = new NullPointerTester();
    tester.testAllPublicStaticMethods(ThrowsNpe.class);
    tester.testAllPublicStaticMethods(ThrowsUoe.class);
    try {
      tester.testAllPublicStaticMethods(ThrowsIae.class);
    } catch (AssertionFailedError expected) {
      return;
    }
    fail();
  }

  public void testStaticOneArgMethodsThatShouldPass() throws Exception {
    for (String methodName : STATIC_ONE_ARG_METHODS_SHOULD_PASS) {
      Method method = OneArg.class.getMethod(methodName, String.class);
      try {
        new NullPointerTester().testMethodParameter(new OneArg(), method, 0);
      } catch (AssertionFailedError unexpected) {
        fail("Should not have flagged method " + methodName);
      }
    }
  }

  public void testStaticOneArgMethodsThatShouldFail() throws Exception {
    for (String methodName : STATIC_ONE_ARG_METHODS_SHOULD_FAIL) {
      Method method = OneArg.class.getMethod(methodName, String.class);
      boolean foundProblem = false;
      try {
        new NullPointerTester().testMethodParameter(new OneArg(), method, 0);
      } catch (AssertionFailedError expected) {
        foundProblem = true;
      }
      assertTrue("Should report error in method " + methodName, foundProblem);
    }
  }

  public void testNonStaticOneArgMethodsThatShouldPass() throws Exception {
    OneArg foo = new OneArg();
    for (String methodName : NONSTATIC_ONE_ARG_METHODS_SHOULD_PASS) {
      Method method = OneArg.class.getMethod(methodName, String.class);
      try {
        new NullPointerTester().testMethodParameter(foo, method, 0);
      } catch (AssertionFailedError unexpected) {
        fail("Should not have flagged method " + methodName);
      }
    }
  }

  public void testNonStaticOneArgMethodsThatShouldFail() throws Exception {
    OneArg foo = new OneArg();
    for (String methodName : NONSTATIC_ONE_ARG_METHODS_SHOULD_FAIL) {
      Method method = OneArg.class.getMethod(methodName, String.class);
      boolean foundProblem = false;
      try {
        new NullPointerTester().testMethodParameter(foo, method, 0);
      } catch (AssertionFailedError expected) {
        foundProblem = true;
      }
      assertTrue("Should report error in method " + methodName, foundProblem);
    }
  }

  public void testMessageOtherException() throws Exception {
    Method method = OneArg.class.getMethod("staticOneArgThrowsOtherThanNpe", String.class);
    boolean foundProblem = false;
    try {
      new NullPointerTester().testMethodParameter(new OneArg(), method, 0);
    } catch (AssertionFailedError expected) {
      assertThat(expected.getMessage()).contains("index 0");
      assertThat(expected.getMessage()).contains("[null]");
      foundProblem = true;
    }
    assertTrue("Should report error when different exception is thrown", foundProblem);
  }

  public void testMessageNoException() throws Exception {
    Method method = OneArg.class.getMethod("staticOneArgShouldThrowNpeButDoesnt", String.class);
    boolean foundProblem = false;
    try {
      new NullPointerTester().testMethodParameter(new OneArg(), method, 0);
    } catch (AssertionFailedError expected) {
      assertThat(expected.getMessage()).contains("index 0");
      assertThat(expected.getMessage()).contains("[null]");
      foundProblem = true;
    }
    assertTrue("Should report error when no exception is thrown", foundProblem);
  }

  /**
   * Class for testing all permutations of nullable/non-nullable two-argument methods using
   * testMethod().
   *
   * <ul>
   *   <li>normalNormal: two params, neither is Nullable
   *   <li>nullableNormal: only first param is Nullable
   *   <li>normalNullable: only second param is Nullable
   *   <li>nullableNullable: both params are Nullable
   * </ul>
   */
  public static class TwoArg {
    /** Action to take on a null param. */
    public enum Action {
      THROW_A_NPE {
        @Override
        public void act() {
          throw new NullPointerException();
        }
      },
      THROW_OTHER {
        @Override
        public void act() {
          throw new FooException();
        }
      },
      JUST_RETURN {
        @Override
        public void act() {}
      };

      public abstract void act();
    }

    Action actionWhenFirstParamIsNull;
    Action actionWhenSecondParamIsNull;

    public TwoArg(Action actionWhenFirstParamIsNull, Action actionWhenSecondParamIsNull) {
      this.actionWhenFirstParamIsNull = actionWhenFirstParamIsNull;
      this.actionWhenSecondParamIsNull = actionWhenSecondParamIsNull;
    }

    /** Method that decides how to react to parameters. */
    public void reactToNullParameters(Object first, Object second) {
      if (first == null) {
        actionWhenFirstParamIsNull.act();
      }
      if (second == null) {
        actionWhenSecondParamIsNull.act();
      }
    }

    /** Two-arg method with no Nullable params. */
    @SuppressWarnings("GoodTime") // false positive; b/122617528
    public void normalNormal(String first, Integer second) {
      reactToNullParameters(first, second);
    }

    /** Two-arg method with the second param Nullable. */
    @SuppressWarnings("GoodTime") // false positive; b/122617528
    public void normalNullable(String first, @Nullable Integer second) {
      reactToNullParameters(first, second);
    }

    /** Two-arg method with the first param Nullable. */
    @SuppressWarnings("GoodTime") // false positive; b/122617528
    public void nullableNormal(@Nullable String first, Integer second) {
      reactToNullParameters(first, second);
    }

    /** Two-arg method with the both params Nullable. */
    @SuppressWarnings("GoodTime") // false positive; b/122617528
    public void nullableNullable(@Nullable String first, @Nullable Integer second) {
      reactToNullParameters(first, second);
    }

    /** To provide sanity during debugging. */
    @Override
    public String toString() {
      return rootLocaleFormat(
          "Bar(%s, %s)", actionWhenFirstParamIsNull, actionWhenSecondParamIsNull);
    }
  }

  public void verifyBarPass(Method method, TwoArg bar) {
    try {
      new NullPointerTester().testMethod(bar, method);
    } catch (AssertionFailedError incorrectError) {
      String errorMessage =
          rootLocaleFormat("Should not have flagged method %s for %s", method.getName(), bar);
      assertNull(errorMessage, incorrectError);
    }
  }

  public void verifyBarFail(Method method, TwoArg bar) {
    try {
      new NullPointerTester().testMethod(bar, method);
    } catch (AssertionFailedError expected) {
      return; // good...we wanted a failure
    }
    String errorMessage =
        rootLocaleFormat("Should have flagged method %s for %s", method.getName(), bar);
    fail(errorMessage);
  }

  public void testTwoArgNormalNormal() throws Exception {
    Method method = TwoArg.class.getMethod("normalNormal", String.class, Integer.class);
    for (TwoArg.Action first : TwoArg.Action.values()) {
      for (TwoArg.Action second : TwoArg.Action.values()) {
        TwoArg bar = new TwoArg(first, second);
        if (first.equals(TwoArg.Action.THROW_A_NPE) && second.equals(TwoArg.Action.THROW_A_NPE)) {
          verifyBarPass(method, bar); // require both params to throw NPE
        } else {
          verifyBarFail(method, bar);
        }
      }
    }
  }

  public void testTwoArgNormalNullable() throws Exception {
    Method method = TwoArg.class.getMethod("normalNullable", String.class, Integer.class);
    for (TwoArg.Action first : TwoArg.Action.values()) {
      for (TwoArg.Action second : TwoArg.Action.values()) {
        TwoArg bar = new TwoArg(first, second);
        if (first.equals(TwoArg.Action.THROW_A_NPE)) {
          verifyBarPass(method, bar); // only pass if 1st param throws NPE
        } else {
          verifyBarFail(method, bar);
        }
      }
    }
  }

  public void testTwoArgNullableNormal() throws Exception {
    Method method = TwoArg.class.getMethod("nullableNormal", String.class, Integer.class);
    for (TwoArg.Action first : TwoArg.Action.values()) {
      for (TwoArg.Action second : TwoArg.Action.values()) {
        TwoArg bar = new TwoArg(first, second);
        if (second.equals(TwoArg.Action.THROW_A_NPE)) {
          verifyBarPass(method, bar); // only pass if 2nd param throws NPE
        } else {
          verifyBarFail(method, bar);
        }
      }
    }
  }

  public void testTwoArgNullableNullable() throws Exception {
    Method method = TwoArg.class.getMethod("nullableNullable", String.class, Integer.class);
    for (TwoArg.Action first : TwoArg.Action.values()) {
      for (TwoArg.Action second : TwoArg.Action.values()) {
        TwoArg bar = new TwoArg(first, second);
        verifyBarPass(method, bar); // All args nullable:  anything goes!
      }
    }
  }

  /*
   * This next part consists of several sample classes that provide
   * demonstrations of conditions that cause NullPointerTester
   * to succeed/fail.
   */

  /** Lots of well-behaved methods. */
  @SuppressWarnings("unused") // used by reflection
  private static class PassObject extends SomeClassThatDoesNotUseNullable {
    public static void doThrow(Object arg) {
      if (arg == null) {
        throw new FooException();
      }
    }

    public void noArg() {}

    public void oneArg(String s) {
      checkNotNull(s);
    }

    void packagePrivateOneArg(String s) {
      checkNotNull(s);
    }

    protected void protectedOneArg(String s) {
      checkNotNull(s);
    }

    public void oneNullableArg(@Nullable String s) {}

    public void oneNullableArgThrows(@Nullable String s) {
      doThrow(s);
    }

    public void twoArg(String s, Integer i) {
      checkNotNull(s);
      i.intValue();
    }

    public void twoMixedArgs(String s, @Nullable Integer i) {
      checkNotNull(s);
    }

    public void twoMixedArgs(@Nullable Integer i, String s) {
      checkNotNull(s);
    }

    public void twoMixedArgsThrows(String s, @Nullable Integer i) {
      checkNotNull(s);
      doThrow(i);
    }

    public void twoMixedArgsThrows(@Nullable Integer i, String s) {
      checkNotNull(s);
      doThrow(i);
    }

    public void twoNullableArgs(@Nullable String s, @javax.annotation.Nullable Integer i) {}

    public void twoNullableArgsThrowsFirstArg(@Nullable String s, @Nullable Integer i) {
      doThrow(s);
    }

    public void twoNullableArgsThrowsSecondArg(@Nullable String s, @Nullable Integer i) {
      doThrow(i);
    }

    public static void staticOneArg(String s) {
      checkNotNull(s);
    }

    public static void staticOneNullableArg(@Nullable String s) {}

    public static void staticOneNullableArgThrows(@Nullable String s) {
      doThrow(s);
    }
  }

  public void testGoodClass() {
    shouldPass(new PassObject());
  }

  private static class FailOneArgDoesntThrowNPE extends PassObject {
    @Override
    public void oneArg(String s) {
      // Fail:  missing NPE for s
    }
  }

  public void testFailOneArgDoesntThrowNpe() {
    shouldFail(new FailOneArgDoesntThrowNPE());
  }

  private static class FailOneArgThrowsWrongType extends PassObject {
    @Override
    public void oneArg(String s) {
      doThrow(s); // Fail:  throwing non-NPE exception for null s
    }
  }

  public void testFailOneArgThrowsWrongType() {
    shouldFail(new FailOneArgThrowsWrongType());
  }

  private static class PassOneNullableArgThrowsNPE extends PassObject {
    @Override
    public void oneNullableArg(@Nullable String s) {
      checkNotNull(s); // ok to throw NPE
    }
  }

  public void testPassOneNullableArgThrowsNPE() {
    shouldPass(new PassOneNullableArgThrowsNPE());
  }

  private static class FailTwoArgsFirstArgDoesntThrowNPE extends PassObject {
    @Override
    public void twoArg(String s, Integer i) {
      // Fail: missing NPE for s
      i.intValue();
    }
  }

  public void testFailTwoArgsFirstArgDoesntThrowNPE() {
    shouldFail(new FailTwoArgsFirstArgDoesntThrowNPE());
  }

  private static class FailTwoArgsFirstArgThrowsWrongType extends PassObject {
    @Override
    public void twoArg(String s, Integer i) {
      doThrow(s); // Fail:  throwing non-NPE exception for null s
      i.intValue();
    }
  }

  public void testFailTwoArgsFirstArgThrowsWrongType() {
    shouldFail(new FailTwoArgsFirstArgThrowsWrongType());
  }

  private static class FailTwoArgsSecondArgDoesntThrowNPE extends PassObject {
    @Override
    public void twoArg(String s, Integer i) {
      checkNotNull(s);
      // Fail: missing NPE for i
    }
  }

  public void testFailTwoArgsSecondArgDoesntThrowNPE() {
    shouldFail(new FailTwoArgsSecondArgDoesntThrowNPE());
  }

  private static class FailTwoArgsSecondArgThrowsWrongType extends PassObject {
    @Override
    public void twoArg(String s, Integer i) {
      checkNotNull(s);
      doThrow(i); // Fail:  throwing non-NPE exception for null i
    }
  }

  public void testFailTwoArgsSecondArgThrowsWrongType() {
    shouldFail(new FailTwoArgsSecondArgThrowsWrongType());
  }

  private static class FailTwoMixedArgsFirstArgDoesntThrowNPE extends PassObject {
    @Override
    public void twoMixedArgs(String s, @Nullable Integer i) {
      // Fail: missing NPE for s
    }
  }

  public void testFailTwoMixedArgsFirstArgDoesntThrowNPE() {
    shouldFail(new FailTwoMixedArgsFirstArgDoesntThrowNPE());
  }

  private static class FailTwoMixedArgsFirstArgThrowsWrongType extends PassObject {
    @Override
    public void twoMixedArgs(String s, @Nullable Integer i) {
      doThrow(s); // Fail:  throwing non-NPE exception for null s
    }
  }

  public void testFailTwoMixedArgsFirstArgThrowsWrongType() {
    shouldFail(new FailTwoMixedArgsFirstArgThrowsWrongType());
  }

  private static class PassTwoMixedArgsNullableArgThrowsNPE extends PassObject {
    @Override
    public void twoMixedArgs(String s, @Nullable Integer i) {
      checkNotNull(s);
      i.intValue(); // ok to throw NPE?
    }
  }

  public void testPassTwoMixedArgsNullableArgThrowsNPE() {
    shouldPass(new PassTwoMixedArgsNullableArgThrowsNPE());
  }

  private static class PassTwoMixedArgSecondNullableArgThrowsOther extends PassObject {
    @Override
    public void twoMixedArgs(String s, @Nullable Integer i) {
      checkNotNull(s);
      doThrow(i); // ok to throw non-NPE exception for null i
    }
  }

  public void testPassTwoMixedArgSecondNullableArgThrowsOther() {
    shouldPass(new PassTwoMixedArgSecondNullableArgThrowsOther());
  }

  private static class FailTwoMixedArgsSecondArgDoesntThrowNPE extends PassObject {
    @Override
    public void twoMixedArgs(@Nullable Integer i, String s) {
      // Fail: missing NPE for null s
    }
  }

  public void testFailTwoMixedArgsSecondArgDoesntThrowNPE() {
    shouldFail(new FailTwoMixedArgsSecondArgDoesntThrowNPE());
  }

  private static class FailTwoMixedArgsSecondArgThrowsWrongType extends PassObject {
    @Override
    public void twoMixedArgs(@Nullable Integer i, String s) {
      doThrow(s); // Fail:  throwing non-NPE exception for null s
    }
  }

  public void testFailTwoMixedArgsSecondArgThrowsWrongType() {
    shouldFail(new FailTwoMixedArgsSecondArgThrowsWrongType());
  }

  private static class PassTwoNullableArgsFirstThrowsNPE extends PassObject {
    @Override
    public void twoNullableArgs(@Nullable String s, @Nullable Integer i) {
      checkNotNull(s); // ok to throw NPE?
    }
  }

  public void testPassTwoNullableArgsFirstThrowsNPE() {
    shouldPass(new PassTwoNullableArgsFirstThrowsNPE());
  }

  private static class PassTwoNullableArgsFirstThrowsOther extends PassObject {
    @Override
    public void twoNullableArgs(@Nullable String s, @Nullable Integer i) {
      doThrow(s); // ok to throw non-NPE exception for null s
    }
  }

  public void testPassTwoNullableArgsFirstThrowsOther() {
    shouldPass(new PassTwoNullableArgsFirstThrowsOther());
  }

  private static class PassTwoNullableArgsSecondThrowsNPE extends PassObject {
    @Override
    public void twoNullableArgs(@Nullable String s, @Nullable Integer i) {
      i.intValue(); // ok to throw NPE?
    }
  }

  public void testPassTwoNullableArgsSecondThrowsNPE() {
    shouldPass(new PassTwoNullableArgsSecondThrowsNPE());
  }

  private static class PassTwoNullableArgsSecondThrowsOther extends PassObject {
    @Override
    public void twoNullableArgs(@Nullable String s, @Nullable Integer i) {
      doThrow(i); // ok to throw non-NPE exception for null i
    }
  }

  public void testPassTwoNullableArgsSecondThrowsOther() {
    shouldPass(new PassTwoNullableArgsSecondThrowsOther());
  }

  private static class PassTwoNullableArgsNeitherThrowsAnything extends PassObject {
    @Override
    public void twoNullableArgs(@Nullable String s, @Nullable Integer i) {
      // ok to do nothing
    }
  }

  public void testPassTwoNullableArgsNeitherThrowsAnything() {
    shouldPass(new PassTwoNullableArgsNeitherThrowsAnything());
  }

  @SuppressWarnings("unused") // for NullPointerTester
  private abstract static class BaseClassThatFailsToThrow {
    public void oneArg(String s) {}
  }

  private static class SubclassWithBadSuperclass extends BaseClassThatFailsToThrow {}

  public void testSubclassWithBadSuperclass() {
    shouldFail(new SubclassWithBadSuperclass());
  }

  @SuppressWarnings("unused") // for NullPointerTester
  private abstract static class BaseClassThatFailsToThrowForPackagePrivate {
    void packagePrivateOneArg(String s) {}
  }

  private static class SubclassWithBadSuperclassForPackagePrivate
      extends BaseClassThatFailsToThrowForPackagePrivate {}

  public void testSubclassWithBadSuperclassForPackagePrivateMethod() {
    shouldFail(new SubclassWithBadSuperclassForPackagePrivate(), Visibility.PACKAGE);
  }

  @SuppressWarnings("unused") // for NullPointerTester
  private abstract static class BaseClassThatFailsToThrowForProtected {
    protected void protectedOneArg(String s) {}
  }

  private static class SubclassWithBadSuperclassForProtected
      extends BaseClassThatFailsToThrowForProtected {}

  public void testSubclassWithBadSuperclassForPackageProtectedMethod() {
    shouldFail(new SubclassWithBadSuperclassForProtected(), Visibility.PROTECTED);
  }

  private static class SubclassThatOverridesBadSuperclassMethod extends BaseClassThatFailsToThrow {
    @Override
    public void oneArg(@Nullable String s) {}
  }

  public void testSubclassThatOverridesBadSuperclassMethod() {
    shouldPass(new SubclassThatOverridesBadSuperclassMethod());
  }

  @SuppressWarnings("unused") // for NullPointerTester
  private static class SubclassOverridesTheWrongMethod extends BaseClassThatFailsToThrow {
    public void oneArg(@Nullable CharSequence s) {}
  }

  public void testSubclassOverridesTheWrongMethod() {
    shouldFail(new SubclassOverridesTheWrongMethod());
  }

  @SuppressWarnings("unused") // for NullPointerTester
  private static class ClassThatFailsToThrowForStatic {
    static void staticOneArg(String s) {}
  }

  public void testClassThatFailsToThrowForStatic() {
    shouldFail(ClassThatFailsToThrowForStatic.class);
  }

  private static class SubclassThatFailsToThrowForStatic extends ClassThatFailsToThrowForStatic {}

  public void testSubclassThatFailsToThrowForStatic() {
    shouldFail(SubclassThatFailsToThrowForStatic.class);
  }

  private static class SubclassThatTriesToOverrideBadStaticMethod
      extends ClassThatFailsToThrowForStatic {
    static void staticOneArg(@Nullable String s) {}
  }

  public void testSubclassThatTriesToOverrideBadStaticMethod() {
    shouldFail(SubclassThatTriesToOverrideBadStaticMethod.class);
  }

  private static final class HardToCreate {
    private HardToCreate(HardToCreate x) {}
  }

  @SuppressWarnings("unused") // used by reflection
  private static class CanCreateDefault {
    public void foo(@Nullable HardToCreate ignored, String required) {
      checkNotNull(required);
    }
  }

  public void testCanCreateDefault() {
    shouldPass(new CanCreateDefault());
  }

  @SuppressWarnings("unused") // used by reflection
  private static class CannotCreateDefault {
    public void foo(HardToCreate ignored, String required) {
      checkNotNull(ignored);
      checkNotNull(required);
    }
  }

  public void testCannotCreateDefault() {
    shouldFail(new CannotCreateDefault());
  }

  private static void shouldPass(Object instance, Visibility visibility) {
    new NullPointerTester().testInstanceMethods(instance, visibility);
  }

  private static void shouldPass(Object instance) {
    shouldPass(instance, Visibility.PACKAGE);
    shouldPass(instance, Visibility.PROTECTED);
    shouldPass(instance, Visibility.PUBLIC);
  }

  // TODO(cpovirk): eliminate surprising Object/Class overloading of shouldFail

  private static void shouldFail(Object instance, Visibility visibility) {
    try {
      new NullPointerTester().testInstanceMethods(instance, visibility);
    } catch (AssertionFailedError expected) {
      return;
    }
    fail("Should detect problem in " + instance.getClass().getSimpleName());
  }

  private static void shouldFail(Object instance) {
    shouldFail(instance, Visibility.PACKAGE);
    shouldFail(instance, Visibility.PROTECTED);
    shouldFail(instance, Visibility.PUBLIC);
  }

  private static void shouldFail(Class<?> cls, Visibility visibility) {
    try {
      new NullPointerTester().testStaticMethods(cls, visibility);
    } catch (AssertionFailedError expected) {
      return;
    }
    fail("Should detect problem in " + cls.getSimpleName());
  }

  private static void shouldFail(Class<?> cls) {
    shouldFail(cls, Visibility.PACKAGE);
  }

  @SuppressWarnings("unused") // used by reflection
  private static class PrivateClassWithPrivateConstructor {
    private PrivateClassWithPrivateConstructor(@Nullable Integer argument) {}
  }

  public void testPrivateClass() {
    NullPointerTester tester = new NullPointerTester();
    for (Constructor<?> constructor :
        PrivateClassWithPrivateConstructor.class.getDeclaredConstructors()) {
      tester.testConstructor(constructor);
    }
  }

  private interface Foo<T> {
    void doSomething(T bar, Integer baz);
  }

  private static class StringFoo implements Foo<String> {

    @Override
    public void doSomething(String bar, Integer baz) {
      checkNotNull(bar);
      checkNotNull(baz);
    }
  }

  public void testBridgeMethodIgnored() {
    new NullPointerTester().testAllPublicInstanceMethods(new StringFoo());
  }

  private abstract static class DefaultValueChecker {

    private final Map<Integer, Object> arguments = Maps.newHashMap();

    final DefaultValueChecker runTester() {
      new NullPointerTester().testInstanceMethods(this, Visibility.PACKAGE);
      return this;
    }

    final void assertNonNullValues(Object... expectedValues) {
      assertEquals(expectedValues.length, arguments.size());
      for (int i = 0; i < expectedValues.length; i++) {
        assertEquals("Default value for parameter #" + i, expectedValues[i], arguments.get(i));
      }
    }

    final Object getDefaultParameterValue(int position) {
      return arguments.get(position);
    }

    final void calledWith(Object... args) {
      for (int i = 0; i < args.length; i++) {
        if (args[i] != null) {
          arguments.put(i, args[i]);
        }
      }
      for (Object arg : args) {
        checkNotNull(arg); // to fulfill null check
      }
    }
  }

  private enum Gender {
    MALE,
    FEMALE
  }

  private static class AllDefaultValuesChecker extends DefaultValueChecker {

    @SuppressWarnings("unused") // called by NullPointerTester
    public void checkDefaultValuesForTheseTypes(
        Gender gender,
        Integer integer,
        int i,
        String string,
        CharSequence charSequence,
        List<String> list,
        ImmutableList<Integer> immutableList,
        Map<String, Integer> map,
        ImmutableMap<String, String> immutableMap,
        Set<String> set,
        ImmutableSet<Integer> immutableSet,
        SortedSet<Number> sortedSet,
        ImmutableSortedSet<Number> immutableSortedSet,
        Multiset<String> multiset,
        ImmutableMultiset<Integer> immutableMultiset,
        Multimap<String, Integer> multimap,
        ImmutableMultimap<String, Integer> immutableMultimap,
        Table<String, Integer, Exception> table,
        ImmutableTable<Integer, String, Exception> immutableTable) {
      calledWith(
          gender,
          integer,
          i,
          string,
          charSequence,
          list,
          immutableList,
          map,
          immutableMap,
          set,
          immutableSet,
          sortedSet,
          immutableSortedSet,
          multiset,
          immutableMultiset,
          multimap,
          immutableMultimap,
          table,
          immutableTable);
    }

    final void check() {
      runTester()
          .assertNonNullValues(
              Gender.MALE,
              Integer.valueOf(0),
              0,
              "",
              "",
              ImmutableList.of(),
              ImmutableList.of(),
              ImmutableMap.of(),
              ImmutableMap.of(),
              ImmutableSet.of(),
              ImmutableSet.of(),
              ImmutableSortedSet.of(),
              ImmutableSortedSet.of(),
              ImmutableMultiset.of(),
              ImmutableMultiset.of(),
              ImmutableMultimap.of(),
              ImmutableMultimap.of(),
              ImmutableTable.of(),
              ImmutableTable.of());
    }
  }

  public void testDefaultValues() {
    new AllDefaultValuesChecker().check();
  }

  private static class ObjectArrayDefaultValueChecker extends DefaultValueChecker {

    @SuppressWarnings("unused") // called by NullPointerTester
    public void checkArray(Object[] array, String s) {
      calledWith(array, s);
    }

    void check() {
      runTester();
      Object[] defaultArray = (Object[]) getDefaultParameterValue(0);
      assertThat(defaultArray).isEmpty();
    }
  }

  public void testObjectArrayDefaultValue() {
    new ObjectArrayDefaultValueChecker().check();
  }

  private static class StringArrayDefaultValueChecker extends DefaultValueChecker {

    @SuppressWarnings("unused") // called by NullPointerTester
    public void checkArray(String[] array, String s) {
      calledWith(array, s);
    }

    void check() {
      runTester();
      String[] defaultArray = (String[]) getDefaultParameterValue(0);
      assertThat(defaultArray).isEmpty();
    }
  }

  public void testStringArrayDefaultValue() {
    new StringArrayDefaultValueChecker().check();
  }

  private static class IntArrayDefaultValueChecker extends DefaultValueChecker {

    @SuppressWarnings("unused") // called by NullPointerTester
    public void checkArray(int[] array, String s) {
      calledWith(array, s);
    }

    void check() {
      runTester();
      int[] defaultArray = (int[]) getDefaultParameterValue(0);
      assertEquals(0, defaultArray.length);
    }
  }

  public void testIntArrayDefaultValue() {
    new IntArrayDefaultValueChecker().check();
  }

  private enum EmptyEnum {}

  private static class EmptyEnumDefaultValueChecker extends DefaultValueChecker {

    @SuppressWarnings("unused") // called by NullPointerTester
    public void checkArray(EmptyEnum object, String s) {
      calledWith(object, s);
    }

    void check() {
      try {
        runTester();
      } catch (AssertionFailedError expected) {
        return;
      }
      fail("Should have failed because enum has no constant");
    }
  }

  public void testEmptyEnumDefaultValue() {
    new EmptyEnumDefaultValueChecker().check();
  }

  private static class GenericClassTypeDefaultValueChecker extends DefaultValueChecker {

    @SuppressWarnings("unused") // called by NullPointerTester
    public void checkArray(Class<? extends List<?>> cls, String s) {
      calledWith(cls, s);
    }

    void check() {
      runTester();
      Class<?> defaultClass = (Class<?>) getDefaultParameterValue(0);
      assertEquals(List.class, defaultClass);
    }
  }

  public void testGenericClassDefaultValue() {
    new GenericClassTypeDefaultValueChecker().check();
  }

  private static class NonGenericClassTypeDefaultValueChecker extends DefaultValueChecker {

    @SuppressWarnings("unused") // called by NullPointerTester
    public void checkArray(@SuppressWarnings("rawtypes") Class cls, String s) {
      calledWith(cls, s);
    }

    void check() {
      runTester();
      Class<?> defaultClass = (Class<?>) getDefaultParameterValue(0);
      assertEquals(Object.class, defaultClass);
    }
  }

  public void testNonGenericClassDefaultValue() {
    new NonGenericClassTypeDefaultValueChecker().check();
  }

  private static class GenericTypeTokenDefaultValueChecker extends DefaultValueChecker {

    @SuppressWarnings("unused") // called by NullPointerTester
    public void checkArray(TypeToken<? extends List<? super Number>> type, String s) {
      calledWith(type, s);
    }

    void check() {
      runTester();
      TypeToken<?> defaultType = (TypeToken<?>) getDefaultParameterValue(0);
      assertTrue(new TypeToken<List<? super Number>>() {}.isSupertypeOf(defaultType));
    }
  }

  public void testGenericTypeTokenDefaultValue() {
    new GenericTypeTokenDefaultValueChecker().check();
  }

  private static class NonGenericTypeTokenDefaultValueChecker extends DefaultValueChecker {

    @SuppressWarnings("unused") // called by NullPointerTester
    public void checkArray(@SuppressWarnings("rawtypes") TypeToken type, String s) {
      calledWith(type, s);
    }

    void check() {
      runTester();
      TypeToken<?> defaultType = (TypeToken<?>) getDefaultParameterValue(0);
      assertEquals(new TypeToken<Object>() {}, defaultType);
    }
  }

  public void testNonGenericTypeTokenDefaultValue() {
    new NonGenericTypeTokenDefaultValueChecker().check();
  }

  private interface FromTo<F, T> extends Function<F, T> {}

  private static class GenericInterfaceDefaultValueChecker extends DefaultValueChecker {

    @SuppressWarnings("unused") // called by NullPointerTester
    public void checkArray(FromTo<String, Integer> f, String s) {
      calledWith(f, s);
    }

    void check() {
      runTester();
      FromTo<?, ?> defaultFunction = (FromTo<?, ?>) getDefaultParameterValue(0);
      assertEquals(0, defaultFunction.apply(null));
    }
  }

  public void testGenericInterfaceDefaultValue() {
    new GenericInterfaceDefaultValueChecker().check();
  }

  private interface NullRejectingFromTo<F, T> extends Function<F, T> {
    @Override
    public abstract T apply(F from);
  }

  private static class NullRejectingInterfaceDefaultValueChecker extends DefaultValueChecker {

    @SuppressWarnings("unused") // called by NullPointerTester
    public void checkArray(NullRejectingFromTo<String, Integer> f, String s) {
      calledWith(f, s);
    }

    void check() {
      runTester();
      NullRejectingFromTo<?, ?> defaultFunction =
          (NullRejectingFromTo<?, ?>) getDefaultParameterValue(0);
      assertNotNull(defaultFunction);
      try {
        defaultFunction.apply(null);
        fail("Proxy Should have rejected null");
      } catch (NullPointerException expected) {
      }
    }
  }

  public void testNullRejectingInterfaceDefaultValue() {
    new NullRejectingInterfaceDefaultValueChecker().check();
  }

  private static class MultipleInterfacesDefaultValueChecker extends DefaultValueChecker {

    @SuppressWarnings("unused") // called by NullPointerTester
    public <T extends FromTo<String, Integer> & Supplier<Long>> void checkArray(T f, String s) {
      calledWith(f, s);
    }

    void check() {
      runTester();
      FromTo<?, ?> defaultFunction = (FromTo<?, ?>) getDefaultParameterValue(0);
      assertEquals(0, defaultFunction.apply(null));
      Supplier<?> defaultSupplier = (Supplier<?>) defaultFunction;
      assertEquals(Long.valueOf(0), defaultSupplier.get());
    }
  }

  public void testMultipleInterfacesDefaultValue() {
    new MultipleInterfacesDefaultValueChecker().check();
  }

  private static class GenericInterface2DefaultValueChecker extends DefaultValueChecker {

    @SuppressWarnings("unused") // called by NullPointerTester
    public void checkArray(FromTo<String, FromTo<Integer, String>> f, String s) {
      calledWith(f, s);
    }

    void check() {
      runTester();
      FromTo<?, ?> defaultFunction = (FromTo<?, ?>) getDefaultParameterValue(0);
      FromTo<?, ?> returnValue = (FromTo<?, ?>) defaultFunction.apply(null);
      assertEquals("", returnValue.apply(null));
    }
  }

  public void testGenericInterfaceReturnedByGenericMethod() {
    new GenericInterface2DefaultValueChecker().check();
  }

  private abstract static class AbstractGenericDefaultValueChecker<T> extends DefaultValueChecker {

    @SuppressWarnings("unused") // called by NullPointerTester
    public void checkGeneric(T value, String s) {
      calledWith(value, s);
    }
  }

  private static class GenericDefaultValueResolvedToStringChecker
      extends AbstractGenericDefaultValueChecker<String> {
    void check() {
      runTester();
      assertEquals("", getDefaultParameterValue(0));
    }
  }

  public void testGenericTypeResolvedForDefaultValue() {
    new GenericDefaultValueResolvedToStringChecker().check();
  }

  private abstract static class AbstractGenericDefaultValueForPackagePrivateMethodChecker<T>
      extends DefaultValueChecker {

    @SuppressWarnings("unused") // called by NullPointerTester
    void checkGeneric(T value, String s) {
      calledWith(value, s);
    }
  }

  private static class DefaultValueForPackagePrivateMethodResolvedToStringChecker
      extends AbstractGenericDefaultValueForPackagePrivateMethodChecker<String> {
    void check() {
      runTester();
      assertEquals("", getDefaultParameterValue(0));
    }
  }

  public void testDefaultValueResolvedForPackagePrivateMethod() {
    new DefaultValueForPackagePrivateMethodResolvedToStringChecker().check();
  }

  private static class ConverterDefaultValueChecker extends DefaultValueChecker {

    @SuppressWarnings("unused") // called by NullPointerTester
    public void checkArray(Converter<String, Integer> c, String s) {
      calledWith(c, s);
    }

    void check() {
      runTester();
      @SuppressWarnings("unchecked") // We are checking it anyway
      Converter<String, Integer> defaultConverter =
          (Converter<String, Integer>) getDefaultParameterValue(0);
      assertEquals(Integer.valueOf(0), defaultConverter.convert("anything"));
      assertEquals("", defaultConverter.reverse().convert(123));
      assertNull(defaultConverter.convert(null));
      assertNull(defaultConverter.reverse().convert(null));
    }
  }

  public void testConverterDefaultValue() {
    new ConverterDefaultValueChecker().check();
  }

  private static class VisibilityMethods {

    @SuppressWarnings("unused") // Called by reflection
    private void privateMethod() {}

    @SuppressWarnings("unused") // Called by reflection
    void packagePrivateMethod() {}

    @SuppressWarnings("unused") // Called by reflection
    protected void protectedMethod() {}

    @SuppressWarnings("unused") // Called by reflection
    public void publicMethod() {}
  }

  public void testVisibility_public() throws Exception {
    assertFalse(
        Visibility.PUBLIC.isVisible(VisibilityMethods.class.getDeclaredMethod("privateMethod")));
    assertFalse(
        Visibility.PUBLIC.isVisible(
            VisibilityMethods.class.getDeclaredMethod("packagePrivateMethod")));
    assertFalse(
        Visibility.PUBLIC.isVisible(VisibilityMethods.class.getDeclaredMethod("protectedMethod")));
    assertTrue(
        Visibility.PUBLIC.isVisible(VisibilityMethods.class.getDeclaredMethod("publicMethod")));
  }

  public void testVisibility_protected() throws Exception {
    assertFalse(
        Visibility.PROTECTED.isVisible(VisibilityMethods.class.getDeclaredMethod("privateMethod")));
    assertFalse(
        Visibility.PROTECTED.isVisible(
            VisibilityMethods.class.getDeclaredMethod("packagePrivateMethod")));
    assertTrue(
        Visibility.PROTECTED.isVisible(
            VisibilityMethods.class.getDeclaredMethod("protectedMethod")));
    assertTrue(
        Visibility.PROTECTED.isVisible(VisibilityMethods.class.getDeclaredMethod("publicMethod")));
  }

  public void testVisibility_package() throws Exception {
    assertFalse(
        Visibility.PACKAGE.isVisible(VisibilityMethods.class.getDeclaredMethod("privateMethod")));
    assertTrue(
        Visibility.PACKAGE.isVisible(
            VisibilityMethods.class.getDeclaredMethod("packagePrivateMethod")));
    assertTrue(
        Visibility.PACKAGE.isVisible(VisibilityMethods.class.getDeclaredMethod("protectedMethod")));
    assertTrue(
        Visibility.PACKAGE.isVisible(VisibilityMethods.class.getDeclaredMethod("publicMethod")));
  }

  private class Inner {
    public Inner(String s) {
      checkNotNull(s);
    }
  }

  public void testNonStaticInnerClass() {
    try {
      new NullPointerTester().testAllPublicConstructors(Inner.class);
      fail();
    } catch (IllegalArgumentException expected) {
      assertThat(expected.getMessage()).contains("inner class");
    }
  }

  private static String rootLocaleFormat(String format, Object... args) {
    return String.format(Locale.ROOT, format, args);
  }

  static class OverridesEquals {
    @SuppressWarnings("EqualsHashCode")
    @Override
    public boolean equals(Object o) {
      return true;
    }
  }

  static class DoesNotOverrideEquals {
    public boolean equals(Object a, Object b) {
      return true;
    }
  }

  public void testEqualsMethod() {
    shouldPass(new OverridesEquals());
    shouldFail(new DoesNotOverrideEquals());
  }

  private static final class FailOnOneOfTwoConstructors {
    @SuppressWarnings("unused") // Called by reflection
    public FailOnOneOfTwoConstructors(String s) {}

    @SuppressWarnings("unused") // Called by reflection
    public FailOnOneOfTwoConstructors(Object o) {
      checkNotNull(o);
    }
  }

  public void testConstructor_Ignored_ShouldPass() throws Exception {
    new NullPointerTester()
        .ignore(FailOnOneOfTwoConstructors.class.getDeclaredConstructor(String.class))
        .testAllPublicConstructors(FailOnOneOfTwoConstructors.class);
  }

  public void testConstructor_ShouldFail() throws Exception {
    try {
      new NullPointerTester().testAllPublicConstructors(FailOnOneOfTwoConstructors.class);
    } catch (AssertionFailedError expected) {
      return;
    }
    fail("Should detect problem in " + FailOnOneOfTwoConstructors.class.getSimpleName());
  }
}
