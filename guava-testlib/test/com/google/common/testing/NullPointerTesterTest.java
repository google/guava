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
import static com.google.common.truth.Truth.assertWithMessage;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

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
import com.google.common.collect.Multimap;
import com.google.common.collect.Multiset;
import com.google.common.collect.Table;
import com.google.common.reflect.TypeToken;
import com.google.common.testing.NullPointerTester.Visibility;
import com.google.common.testing.anotherpackage.SomeClassThatDoesNotUseNullable;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.errorprone.annotations.DoNotCall;
import com.google.errorprone.annotations.FormatMethod;
import com.google.errorprone.annotations.Keep;
import com.google.testing.junit.testparameterinjector.TestParameter;
import com.google.testing.junit.testparameterinjector.TestParameterInjector;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import javax.annotation.CheckForNull;
import org.jspecify.annotations.NullUnmarked;
import org.jspecify.annotations.Nullable;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Unit test for {@link NullPointerTester}.
 *
 * @author Kevin Bourrillion
 * @author Mick Killianey
 */
@SuppressWarnings({
  "CheckReturnValue",
  "unused", // many methods tested reflectively -- maybe prefer local @Keep annotations?
})
@NullUnmarked
@RunWith(TestParameterInjector.class)
public class NullPointerTesterTest {

  /** Non-NPE RuntimeException. */
  public static class FooException extends RuntimeException {
    private static final long serialVersionUID = 1L;
  }

  /**
   * Class for testing all permutations of static/non-static one-argument methods using
   * methodParameter().
   */
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

    public static void staticOneArgCheckForNullCorrectlyDoesNotThrowNpe(@CheckForNull String s) {
      // null?  no problem
    }

    public static void staticOneArgJsr305NullableCorrectlyDoesNotThrowNpe(
        @javax.annotation.Nullable String s) {
      // null?  no problem
    }

    public static void staticOneArgNullableCorrectlyDoesNotThrowNpe(@Nullable String s) {
      // null?  no problem
    }

    public static void staticOneArgCheckForNullCorrectlyThrowsOtherThanNpe(@CheckForNull String s) {
      throw new FooException(); // ok, as long as it's not NullPointerException
    }

    public static void staticOneArgNullableCorrectlyThrowsOtherThanNpe(@Nullable String s) {
      throw new FooException(); // ok, as long as it's not NullPointerException
    }

    public static void staticOneArgCheckForNullThrowsNpe(@CheckForNull String s) {
      checkNotNull(s); // doesn't check if you said you'd accept null, but you don't
    }

    public static void staticOneArgNullableThrowsNpe(@Nullable String s) {
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

    public void oneArgCheckForNullCorrectlyDoesNotThrowNpe(@CheckForNull String s) {
      // null?  no problem
    }

    public void oneArgNullableCorrectlyDoesNotThrowNpe(@Nullable String s) {
      // null?  no problem
    }

    public void oneArgCheckForNullCorrectlyThrowsOtherThanNpe(@CheckForNull String s) {
      throw new FooException(); // ok, as long as it's not NullPointerException
    }

    public void oneArgNullableCorrectlyThrowsOtherThanNpe(@Nullable String s) {
      throw new FooException(); // ok, as long as it's not NullPointerException
    }

    public void oneArgCheckForNullThrowsNpe(@CheckForNull String s) {
      checkNotNull(s); // doesn't check if you said you'd accept null, but you don't
    }

    public void oneArgNullableThrowsNpe(@Nullable String s) {
      checkNotNull(s); // doesn't check if you said you'd accept null, but you don't
    }
  }

  private static class ThrowsIae {
    @Keep
    public static void christenPoodle(String name) {
      checkArgument(name != null);
    }
  }

  private static class ThrowsNpe {
    @Keep
    public static void christenPoodle(String name) {
      checkNotNull(name);
    }
  }

  private static class ThrowsUoe {
    @DoNotCall
    @Keep
    public static void christenPoodle(String unused) {
      throw new UnsupportedOperationException();
    }
  }

  private static class ThrowsSomethingElse {
    @DoNotCall
    @Keep
    public static void christenPoodle(String unused) {
      throw new RuntimeException();
    }
  }

  private interface InterfaceStaticMethodFailsToCheckNull {
    static String create(String unused) {
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

    default void doNotCheckNull(String unused) {}
  }

  private interface InterfaceDefaultMethodChecksNull {
    static InterfaceDefaultMethodChecksNull create() {
      return new InterfaceDefaultMethodChecksNull() {};
    }

    default void checksNull(String s) {
      checkNotNull(s);
    }
  }

  @Test
  public void interfaceStaticMethod() {
    NullPointerTester tester = new NullPointerTester();
    tester.testAllPublicStaticMethods(InterfaceStaticMethodChecksNull.class);
    assertThrows(
        AssertionError.class,
        () -> tester.testAllPublicStaticMethods(InterfaceStaticMethodFailsToCheckNull.class));
  }

  @Test
  public void interfaceDefaultMethod() {
    NullPointerTester tester = new NullPointerTester();
    tester.testAllPublicInstanceMethods(InterfaceDefaultMethodChecksNull.create());
    assertThrows(
        AssertionError.class,
        () -> tester.testAllPublicInstanceMethods(InterfaceDefaultMethodFailsToCheckNull.create()));
  }

  @Test
  public void dontAcceptIae() {
    NullPointerTester tester = new NullPointerTester();
    tester.testAllPublicStaticMethods(ThrowsNpe.class);
    tester.testAllPublicStaticMethods(ThrowsUoe.class);
    assertThrows(AssertionError.class, () -> tester.testAllPublicStaticMethods(ThrowsIae.class));
  }

  @Test
  public void staticOneArgMethodsThatShouldPass(
      @TestParameter({
            "staticOneArgCorrectlyThrowsNpe",
            "staticOneArgCheckForNullCorrectlyDoesNotThrowNpe",
            "staticOneArgCheckForNullCorrectlyThrowsOtherThanNpe",
            "staticOneArgCheckForNullThrowsNpe",
            "staticOneArgNullableCorrectlyDoesNotThrowNpe",
            "staticOneArgNullableCorrectlyThrowsOtherThanNpe",
            "staticOneArgNullableThrowsNpe"
          })
          String methodName)
      throws Exception {
    Method method = OneArg.class.getMethod(methodName, String.class);
    new NullPointerTester().testMethodParameter(new OneArg(), method, 0);
  }

  @Test
  public void staticOneArgMethodsThatShouldFail(
      @TestParameter({"staticOneArgThrowsOtherThanNpe", "staticOneArgShouldThrowNpeButDoesnt"})
          String methodName)
      throws Exception {
    Method method = OneArg.class.getMethod(methodName, String.class);
    assertThrows(
        "Should report error in method " + methodName,
        AssertionError.class,
        () -> new NullPointerTester().testMethodParameter(new OneArg(), method, 0));
  }

  @Test
  public void nonStaticOneArgMethodsThatShouldPass(
      @TestParameter({
            "oneArgCorrectlyThrowsNpe",
            "oneArgCheckForNullCorrectlyDoesNotThrowNpe",
            "oneArgCheckForNullCorrectlyThrowsOtherThanNpe",
            "oneArgCheckForNullThrowsNpe",
            "oneArgNullableCorrectlyDoesNotThrowNpe",
            "oneArgNullableCorrectlyThrowsOtherThanNpe",
            "oneArgNullableThrowsNpe"
          })
          String methodName)
      throws Exception {
    OneArg foo = new OneArg();
    Method method = OneArg.class.getMethod(methodName, String.class);
    new NullPointerTester().testMethodParameter(foo, method, 0);
  }

  @Test
  public void nonStaticOneArgMethodsThatShouldFail(
      @TestParameter({"oneArgThrowsOtherThanNpe", "oneArgShouldThrowNpeButDoesnt"})
          String methodName)
      throws Exception {
    OneArg foo = new OneArg();
    Method method = OneArg.class.getMethod(methodName, String.class);
    assertThrows(
        "Should report error in method " + methodName,
        AssertionError.class,
        () -> new NullPointerTester().testMethodParameter(foo, method, 0));
  }

  @Test
  public void messageOtherException() throws Exception {
    Method method = OneArg.class.getMethod("staticOneArgThrowsOtherThanNpe", String.class);
    AssertionError expected =
        assertThrows(
            AssertionError.class,
            () -> new NullPointerTester().testMethodParameter(new OneArg(), method, 0));
    assertThat(expected).hasMessageThat().contains("index 0");
    assertThat(expected).hasMessageThat().contains("[null]");
  }

  @Test
  public void messageNoException() throws Exception {
    Method method = OneArg.class.getMethod("staticOneArgShouldThrowNpeButDoesnt", String.class);
    AssertionError expected =
        assertThrows(
            AssertionError.class,
            () -> new NullPointerTester().testMethodParameter(new OneArg(), method, 0));
    assertThat(expected).hasMessageThat().contains("index 0");
    assertThat(expected).hasMessageThat().contains("[null]");
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
    public void reactToNullParameters(@Nullable Object first, @Nullable Object second) {
      if (first == null) {
        actionWhenFirstParamIsNull.act();
      }
      if (second == null) {
        actionWhenSecondParamIsNull.act();
      }
    }

    /** Two-arg method with no Nullable params. */
    public void normalNormal(String first, Integer second) {
      reactToNullParameters(first, second);
    }

    /** Two-arg method with the second param Nullable. */
    public void normalNullable(String first, @Nullable Integer second) {
      reactToNullParameters(first, second);
    }

    /** Two-arg method with the first param Nullable. */
    public void nullableNormal(@Nullable String first, Integer second) {
      reactToNullParameters(first, second);
    }

    /** Two-arg method with the both params Nullable. */
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
    } catch (AssertionError incorrectError) {
      String errorMessage =
          rootLocaleFormat("Should not have flagged method %s for %s", method.getName(), bar);
      assertWithMessage(errorMessage).that(incorrectError).isNull();
    }
  }

  public void verifyBarFail(Method method, TwoArg bar) {
    assertThrows(
        rootLocaleFormat("Should have flagged method %s for %s", method.getName(), bar),
        AssertionError.class,
        () -> new NullPointerTester().testMethod(bar, method));
  }

  @Test
  public void twoArgNormalNormal(
      @TestParameter TwoArg.Action first, @TestParameter TwoArg.Action second) throws Exception {
    Method method = TwoArg.class.getMethod("normalNormal", String.class, Integer.class);
    TwoArg bar = new TwoArg(first, second);
    if (first.equals(TwoArg.Action.THROW_A_NPE) && second.equals(TwoArg.Action.THROW_A_NPE)) {
      verifyBarPass(method, bar); // require both params to throw NPE
    } else {
      verifyBarFail(method, bar);
    }
  }

  @Test
  public void twoArgNormalNullable(
      @TestParameter TwoArg.Action first, @TestParameter TwoArg.Action second) throws Exception {
    Method method = TwoArg.class.getMethod("normalNullable", String.class, Integer.class);
    TwoArg bar = new TwoArg(first, second);
    if (first.equals(TwoArg.Action.THROW_A_NPE)) {
      verifyBarPass(method, bar); // only pass if 1st param throws NPE
    } else {
      verifyBarFail(method, bar);
    }
  }

  @Test
  public void twoArgNullableNormal(
      @TestParameter TwoArg.Action first, @TestParameter TwoArg.Action second) throws Exception {
    Method method = TwoArg.class.getMethod("nullableNormal", String.class, Integer.class);
    TwoArg bar = new TwoArg(first, second);
    if (second.equals(TwoArg.Action.THROW_A_NPE)) {
      verifyBarPass(method, bar); // only pass if 2nd param throws NPE
    } else {
      verifyBarFail(method, bar);
    }
  }

  @Test
  public void twoArgNullableNullable(
      @TestParameter TwoArg.Action first, @TestParameter TwoArg.Action second) throws Exception {
    Method method = TwoArg.class.getMethod("nullableNullable", String.class, Integer.class);
    TwoArg bar = new TwoArg(first, second);
    verifyBarPass(method, bar); // All args nullable:  anything goes!
  }

  /*
   * This next part consists of several sample classes that provide
   * demonstrations of conditions that cause NullPointerTester
   * to succeed/fail.
   */

  /** Lots of well-behaved methods. */
  private static class PassObject extends SomeClassThatDoesNotUseNullable {
    @Keep
    public static void throwFooExceptionIfNull(Object arg) {
      if (arg == null) {
        throw new FooException();
      }
    }

    @Keep
    public void noArg() {}

    @Keep
    public void oneArg(String s) {
      checkNotNull(s);
    }

    void packagePrivateOneArg(String s) {
      checkNotNull(s);
    }

    @Keep
    protected void protectedOneArg(String s) {
      checkNotNull(s);
    }

    @Keep
    public void oneNullableArg(@Nullable String s) {}

    @Keep
    public void oneNullableArgThrows(@Nullable String s) {
      throwFooExceptionIfNull(s);
    }

    @Keep
    public void twoArg(String s, Integer i) {
      checkNotNull(s);
      checkNotNull(i);
    }

    @Keep
    public void twoMixedArgs(String s, @Nullable Integer i) {
      checkNotNull(s);
    }

    @Keep
    public void twoMixedArgs(@Nullable Integer i, String s) {
      checkNotNull(s);
    }

    @Keep
    public void twoMixedArgsThrows(String s, @Nullable Integer i) {
      checkNotNull(s);
      throwFooExceptionIfNull(i);
    }

    @Keep
    public void twoMixedArgsThrows(@Nullable Integer i, String s) {
      checkNotNull(s);
      throwFooExceptionIfNull(i);
    }

    @Keep
    public void twoNullableArgs(@Nullable String s, @javax.annotation.Nullable Integer i) {}

    @Keep
    public void twoNullableArgsThrowsFirstArg(@Nullable String s, @Nullable Integer i) {
      throwFooExceptionIfNull(s);
    }

    @Keep
    public void twoNullableArgsThrowsSecondArg(@Nullable String s, @Nullable Integer i) {
      throwFooExceptionIfNull(i);
    }

    @Keep
    public static void staticOneArg(String s) {
      checkNotNull(s);
    }

    @Keep
    public static void staticOneNullableArg(@Nullable String s) {}

    @Keep
    public static void staticOneNullableArgThrows(@Nullable String s) {
      throwFooExceptionIfNull(s);
    }
  }

  @Test
  public void goodClass() {
    shouldPass(new PassObject());
  }

  private static class FailOneArgDoesntThrowNpe extends PassObject {
    @Override
    public void oneArg(String s) {
      // Fail: missing NPE for s
    }
  }

  @Test
  public void failOneArgDoesntThrowNpe() {
    shouldFail(new FailOneArgDoesntThrowNpe());
  }

  private static class FailOneArgThrowsWrongType extends PassObject {
    @Override
    public void oneArg(String s) {
      throwFooExceptionIfNull(s); // Fail: throwing non-NPE exception for null s
    }
  }

  @Test
  public void failOneArgThrowsWrongType() {
    shouldFail(new FailOneArgThrowsWrongType());
  }

  private static class PassOneNullableArgThrowsNpe extends PassObject {
    @Override
    public void oneNullableArg(@Nullable String s) {
      checkNotNull(s); // ok to throw NPE
    }
  }

  @Test
  public void passOneNullableArgThrowsNpe() {
    shouldPass(new PassOneNullableArgThrowsNpe());
  }

  private static class FailTwoArgsFirstArgDoesntThrowNpe extends PassObject {
    @Override
    public void twoArg(String s, Integer i) {
      // Fail: missing NPE for s
      checkNotNull(i);
    }
  }

  @Test
  public void failTwoArgsFirstArgDoesntThrowNpe() {
    shouldFail(new FailTwoArgsFirstArgDoesntThrowNpe());
  }

  private static class FailTwoArgsFirstArgThrowsWrongType extends PassObject {
    @Override
    public void twoArg(String s, Integer i) {
      throwFooExceptionIfNull(s); // Fail: throwing non-NPE exception for null s
      checkNotNull(i);
    }
  }

  @Test
  public void failTwoArgsFirstArgThrowsWrongType() {
    shouldFail(new FailTwoArgsFirstArgThrowsWrongType());
  }

  private static class FailTwoArgsSecondArgDoesntThrowNpe extends PassObject {
    @Override
    public void twoArg(String s, Integer i) {
      checkNotNull(s);
      // Fail: missing NPE for i
    }
  }

  @Test
  public void failTwoArgsSecondArgDoesntThrowNpe() {
    shouldFail(new FailTwoArgsSecondArgDoesntThrowNpe());
  }

  private static class FailTwoArgsSecondArgThrowsWrongType extends PassObject {
    @Override
    public void twoArg(String s, Integer i) {
      checkNotNull(s);
      throwFooExceptionIfNull(i); // Fail: throwing non-NPE exception for null i
    }
  }

  @Test
  public void failTwoArgsSecondArgThrowsWrongType() {
    shouldFail(new FailTwoArgsSecondArgThrowsWrongType());
  }

  private static class FailTwoMixedArgsFirstArgDoesntThrowNpe extends PassObject {
    @Override
    public void twoMixedArgs(String s, @Nullable Integer i) {
      // Fail: missing NPE for s
    }
  }

  @Test
  public void failTwoMixedArgsFirstArgDoesntThrowNpe() {
    shouldFail(new FailTwoMixedArgsFirstArgDoesntThrowNpe());
  }

  private static class FailTwoMixedArgsFirstArgThrowsWrongType extends PassObject {
    @Override
    public void twoMixedArgs(String s, @Nullable Integer i) {
      throwFooExceptionIfNull(s); // Fail: throwing non-NPE exception for null s
    }
  }

  @Test
  public void failTwoMixedArgsFirstArgThrowsWrongType() {
    shouldFail(new FailTwoMixedArgsFirstArgThrowsWrongType());
  }

  private static class PassTwoMixedArgsNullableArgThrowsNpe extends PassObject {
    @Override
    public void twoMixedArgs(String s, @Nullable Integer i) {
      checkNotNull(s);
      checkNotNull(i); // ok to throw NPE?
    }
  }

  @Test
  public void passTwoMixedArgsNullableArgThrowsNpe() {
    shouldPass(new PassTwoMixedArgsNullableArgThrowsNpe());
  }

  private static class PassTwoMixedArgSecondNullableArgThrowsOther extends PassObject {
    @Override
    public void twoMixedArgs(String s, @Nullable Integer i) {
      checkNotNull(s);
      throwFooExceptionIfNull(i); // ok to throw non-NPE exception for null i
    }
  }

  @Test
  public void passTwoMixedArgSecondNullableArgThrowsOther() {
    shouldPass(new PassTwoMixedArgSecondNullableArgThrowsOther());
  }

  private static class FailTwoMixedArgsSecondArgDoesntThrowNpe extends PassObject {
    @Override
    public void twoMixedArgs(@Nullable Integer i, String s) {
      // Fail: missing NPE for null s
    }
  }

  @Test
  public void failTwoMixedArgsSecondArgDoesntThrowNpe() {
    shouldFail(new FailTwoMixedArgsSecondArgDoesntThrowNpe());
  }

  private static class FailTwoMixedArgsSecondArgThrowsWrongType extends PassObject {
    @Override
    public void twoMixedArgs(@Nullable Integer i, String s) {
      throwFooExceptionIfNull(s); // Fail: throwing non-NPE exception for null s
    }
  }

  @Test
  public void failTwoMixedArgsSecondArgThrowsWrongType() {
    shouldFail(new FailTwoMixedArgsSecondArgThrowsWrongType());
  }

  private static class PassTwoNullableArgsFirstThrowsNpe extends PassObject {
    @Override
    public void twoNullableArgs(@Nullable String s, @Nullable Integer i) {
      checkNotNull(s); // ok to throw NPE?
    }
  }

  @Test
  public void passTwoNullableArgsFirstThrowsNpe() {
    shouldPass(new PassTwoNullableArgsFirstThrowsNpe());
  }

  private static class PassTwoNullableArgsFirstThrowsOther extends PassObject {
    @Override
    public void twoNullableArgs(@Nullable String s, @Nullable Integer i) {
      throwFooExceptionIfNull(s); // ok to throw non-NPE exception for null s
    }
  }

  @Test
  public void passTwoNullableArgsFirstThrowsOther() {
    shouldPass(new PassTwoNullableArgsFirstThrowsOther());
  }

  private static class PassTwoNullableArgsSecondThrowsNpe extends PassObject {
    @Override
    public void twoNullableArgs(@Nullable String s, @Nullable Integer i) {
      checkNotNull(i); // ok to throw NPE?
    }
  }

  @Test
  public void passTwoNullableArgsSecondThrowsNpe() {
    shouldPass(new PassTwoNullableArgsSecondThrowsNpe());
  }

  private static class PassTwoNullableArgsSecondThrowsOther extends PassObject {
    @Override
    public void twoNullableArgs(@Nullable String s, @Nullable Integer i) {
      throwFooExceptionIfNull(i); // ok to throw non-NPE exception for null i
    }
  }

  @Test
  public void passTwoNullableArgsSecondThrowsOther() {
    shouldPass(new PassTwoNullableArgsSecondThrowsOther());
  }

  private static class PassTwoNullableArgsNeitherThrowsAnything extends PassObject {
    @Override
    public void twoNullableArgs(@Nullable String s, @Nullable Integer i) {
      // ok to do nothing
    }
  }

  @Test
  public void passTwoNullableArgsNeitherThrowsAnything() {
    shouldPass(new PassTwoNullableArgsNeitherThrowsAnything());
  }

  private abstract static class BaseClassThatFailsToThrow {
    @Keep
    public void oneArg(String s) {}
  }

  private static class SubclassWithBadSuperclass extends BaseClassThatFailsToThrow {}

  @Test
  public void subclassWithBadSuperclass() {
    shouldFail(new SubclassWithBadSuperclass());
  }

  private abstract static class BaseClassThatFailsToThrowForPackagePrivate {
    void packagePrivateOneArg(String s) {}
  }

  private static class SubclassWithBadSuperclassForPackagePrivate
      extends BaseClassThatFailsToThrowForPackagePrivate {}

  @Test
  public void subclassWithBadSuperclassForPackagePrivateMethod() {
    shouldFail(new SubclassWithBadSuperclassForPackagePrivate(), Visibility.PACKAGE);
  }

  private abstract static class BaseClassThatFailsToThrowForProtected {
    @Keep
    protected void protectedOneArg(String s) {}
  }

  private static class SubclassWithBadSuperclassForProtected
      extends BaseClassThatFailsToThrowForProtected {}

  @Test
  public void subclassWithBadSuperclassForPackageProtectedMethod() {
    shouldFail(new SubclassWithBadSuperclassForProtected(), Visibility.PROTECTED);
  }

  private static class SubclassThatOverridesBadSuperclassMethod extends BaseClassThatFailsToThrow {
    @Override
    public void oneArg(@Nullable String s) {}
  }

  @Test
  public void subclassThatOverridesBadSuperclassMethod() {
    shouldPass(new SubclassThatOverridesBadSuperclassMethod());
  }

  private static class SubclassOverridesTheWrongMethod extends BaseClassThatFailsToThrow {
    @Keep
    public void oneArg(@Nullable CharSequence s) {}
  }

  @Test
  public void subclassOverridesTheWrongMethod() {
    shouldFail(new SubclassOverridesTheWrongMethod());
  }

  private static class ClassThatFailsToThrowForStatic {
    static void staticOneArg(String s) {}
  }

  @Test
  public void classThatFailsToThrowForStatic() {
    shouldFail(ClassThatFailsToThrowForStatic.class);
  }

  private static class SubclassThatFailsToThrowForStatic extends ClassThatFailsToThrowForStatic {}

  @Test
  public void subclassThatFailsToThrowForStatic() {
    shouldFail(SubclassThatFailsToThrowForStatic.class);
  }

  private static class SubclassThatTriesToOverrideBadStaticMethod
      extends ClassThatFailsToThrowForStatic {
    static void staticOneArg(String unused) {}
  }

  @Test
  public void subclassThatTriesToOverrideBadStaticMethod() {
    shouldFail(SubclassThatTriesToOverrideBadStaticMethod.class);
  }

  private static final class HardToCreate {
    private HardToCreate(String unused) {}
  }

  private static class CanCreateDefault {
    @Keep
    public void foo(@Nullable HardToCreate ignored, String required) {
      checkNotNull(required);
    }
  }

  @Test
  public void canCreateDefault() {
    shouldPass(new CanCreateDefault());
  }

  private static class CannotCreateDefault {
    @Keep
    public void foo(HardToCreate ignored, String required) {
      checkNotNull(ignored);
      checkNotNull(required);
    }
  }

  @Test
  public void cannotCreateDefault() {
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
    assertThrows(
        "Expected NullPointerTester to fail for "
            + instance.getClass().getSimpleName()
            + " with visibility "
            + visibility,
        AssertionError.class,
        () -> new NullPointerTester().testInstanceMethods(instance, visibility));
  }

  private static void shouldFail(Object instance) {
    shouldFail(instance, Visibility.PACKAGE);
    shouldFail(instance, Visibility.PROTECTED);
    shouldFail(instance, Visibility.PUBLIC);
  }

  private static void shouldFail(Class<?> cls, Visibility visibility) {
    assertThrows(
        "Expected NullPointerTester to fail for static methods of "
            + cls.getSimpleName()
            + " with visibility "
            + visibility,
        AssertionError.class,
        () -> new NullPointerTester().testStaticMethods(cls, visibility));
  }

  private static void shouldFail(Class<?> cls) {
    shouldFail(cls, Visibility.PACKAGE);
  }

  private static class PrivateClassWithPrivateConstructor {
    private PrivateClassWithPrivateConstructor(@Nullable Integer argument) {}
  }

  @Test
  public void privateClass() {
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

  @Test
  public void bridgeMethodIgnored() {
    new NullPointerTester().testAllPublicInstanceMethods(new StringFoo());
  }

  private abstract static class DefaultValueChecker {

    private final Map<Integer, Object> arguments = new HashMap<>();

    @CanIgnoreReturnValue
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

    @Keep
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
              0,
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

  @Test
  public void defaultValues() {
    new AllDefaultValuesChecker().check();
  }

  private static class ObjectArrayDefaultValueChecker extends DefaultValueChecker {

    @Keep
    public void checkArray(Object[] array, String s) {
      calledWith(array, s);
    }

    void check() {
      runTester();
      Object[] defaultArray = (Object[]) getDefaultParameterValue(0);
      assertThat(defaultArray).isEmpty();
    }
  }

  @Test
  public void objectArrayDefaultValue() {
    new ObjectArrayDefaultValueChecker().check();
  }

  private static class StringArrayDefaultValueChecker extends DefaultValueChecker {

    @Keep
    public void checkArray(String[] array, String s) {
      calledWith(array, s);
    }

    void check() {
      runTester();
      String[] defaultArray = (String[]) getDefaultParameterValue(0);
      assertThat(defaultArray).isEmpty();
    }
  }

  @Test
  public void stringArrayDefaultValue() {
    new StringArrayDefaultValueChecker().check();
  }

  private static class IntArrayDefaultValueChecker extends DefaultValueChecker {

    @Keep
    public void checkArray(int[] array, String s) {
      calledWith(array, s);
    }

    void check() {
      runTester();
      int[] defaultArray = (int[]) getDefaultParameterValue(0);
      assertThat(defaultArray).isEmpty();
    }
  }

  @Test
  public void intArrayDefaultValue() {
    new IntArrayDefaultValueChecker().check();
  }

  private enum EmptyEnum {}

  private static class EmptyEnumDefaultValueChecker extends DefaultValueChecker {

    @Keep
    public void checkArray(EmptyEnum object, String s) {
      calledWith(object, s);
    }

    void check() {
      assertThrows(AssertionError.class, () -> runTester());
    }
  }

  @Test
  public void emptyEnumDefaultValue() {
    new EmptyEnumDefaultValueChecker().check();
  }

  private static class GenericClassTypeDefaultValueChecker extends DefaultValueChecker {

    @Keep
    public void checkArray(Class<? extends List<?>> cls, String s) {
      calledWith(cls, s);
    }

    void check() {
      runTester();
      Class<?> defaultClass = (Class<?>) getDefaultParameterValue(0);
      assertThat(defaultClass).isEqualTo(List.class);
    }
  }

  @Test
  public void genericClassDefaultValue() {
    new GenericClassTypeDefaultValueChecker().check();
  }

  private static class NonGenericClassTypeDefaultValueChecker extends DefaultValueChecker {

    @Keep
    public void checkArray(@SuppressWarnings("rawtypes") Class cls, String s) {
      calledWith(cls, s);
    }

    void check() {
      runTester();
      Class<?> defaultClass = (Class<?>) getDefaultParameterValue(0);
      assertThat(defaultClass).isEqualTo(Object.class);
    }
  }

  @Test
  public void nonGenericClassDefaultValue() {
    new NonGenericClassTypeDefaultValueChecker().check();
  }

  private static class GenericTypeTokenDefaultValueChecker extends DefaultValueChecker {

    @Keep
    public void checkArray(TypeToken<? extends List<? super Number>> type, String s) {
      calledWith(type, s);
    }

    void check() {
      runTester();
      TypeToken<?> defaultType = (TypeToken<?>) getDefaultParameterValue(0);
      assertTrue(new TypeToken<List<? super Number>>() {}.isSupertypeOf(defaultType));
    }
  }

  @Test
  public void genericTypeTokenDefaultValue() {
    new GenericTypeTokenDefaultValueChecker().check();
  }

  private static class NonGenericTypeTokenDefaultValueChecker extends DefaultValueChecker {

    @Keep
    public void checkArray(@SuppressWarnings("rawtypes") TypeToken type, String s) {
      calledWith(type, s);
    }

    void check() {
      runTester();
      TypeToken<?> defaultType = (TypeToken<?>) getDefaultParameterValue(0);
      assertEquals(new TypeToken<Object>() {}, defaultType);
    }
  }

  @Test
  public void nonGenericTypeTokenDefaultValue() {
    new NonGenericTypeTokenDefaultValueChecker().check();
  }

  private interface FromTo<F, T> extends Function<F, T> {}

  private static class GenericInterfaceDefaultValueChecker extends DefaultValueChecker {

    @Keep
    public void checkArray(FromTo<String, Integer> f, String s) {
      calledWith(f, s);
    }

    void check() {
      runTester();
      FromTo<?, ?> defaultFunction = (FromTo<?, ?>) getDefaultParameterValue(0);
      assertEquals(0, defaultFunction.apply(null));
    }
  }

  @Test
  public void genericInterfaceDefaultValue() {
    new GenericInterfaceDefaultValueChecker().check();
  }

  private interface NullRejectingFromTo<F, T> extends Function<F, T> {
    @Override
    T apply(F from);
  }

  private static class NullRejectingInterfaceDefaultValueChecker extends DefaultValueChecker {

    @Keep
    public void checkArray(NullRejectingFromTo<String, Integer> f, String s) {
      calledWith(f, s);
    }

    void check() {
      runTester();
      NullRejectingFromTo<?, ?> defaultFunction =
          (NullRejectingFromTo<?, ?>) getDefaultParameterValue(0);
      assertThat(defaultFunction).isNotNull();
      assertThrows(NullPointerException.class, () -> defaultFunction.apply(null));
    }
  }

  @Test
  public void nullRejectingInterfaceDefaultValue() {
    new NullRejectingInterfaceDefaultValueChecker().check();
  }

  private static class MultipleInterfacesDefaultValueChecker extends DefaultValueChecker {

    @Keep
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

  @Test
  public void multipleInterfacesDefaultValue() {
    new MultipleInterfacesDefaultValueChecker().check();
  }

  private static class GenericInterface2DefaultValueChecker extends DefaultValueChecker {

    @Keep
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

  @Test
  public void genericInterfaceReturnedByGenericMethod() {
    new GenericInterface2DefaultValueChecker().check();
  }

  private abstract static class AbstractGenericDefaultValueChecker<T> extends DefaultValueChecker {

    @Keep
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

  @Test
  public void genericTypeResolvedForDefaultValue() {
    new GenericDefaultValueResolvedToStringChecker().check();
  }

  private abstract static class AbstractGenericDefaultValueForPackagePrivateMethodChecker<T>
      extends DefaultValueChecker {

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

  @Test
  public void defaultValueResolvedForPackagePrivateMethod() {
    new DefaultValueForPackagePrivateMethodResolvedToStringChecker().check();
  }

  private static class ConverterDefaultValueChecker extends DefaultValueChecker {

    @Keep
    public void checkArray(Converter<String, Integer> c, String s) {
      calledWith(c, s);
    }

    void check() {
      runTester();
      @SuppressWarnings("unchecked") // We are checking it anyway
      Converter<String, Integer> defaultConverter =
          (Converter<String, Integer>) getDefaultParameterValue(0);
      assertEquals(Integer.valueOf(0), defaultConverter.convert("anything"));
      assertThat(defaultConverter.reverse().convert(123)).isEqualTo("");
      assertThat(defaultConverter.convert(null)).isNull();
      assertThat(defaultConverter.reverse().convert(null)).isNull();
    }
  }

  @Test
  public void converterDefaultValue() {
    new ConverterDefaultValueChecker().check();
  }

  private static class VisibilityMethods {

    private void privateMethod() {}

    void packagePrivateMethod() {}

    @Keep
    protected void protectedMethod() {}

    @Keep
    public void publicMethod() {}
  }

  @Test
  public void visibility_public() throws Exception {
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

  @Test
  public void visibility_protected() throws Exception {
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

  @Test
  public void visibility_package() throws Exception {
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

  @SuppressWarnings("ClassCanBeStatic") // We want to test an inner class.
  private class Inner {
    @Keep
    public Inner(String s) {
      checkNotNull(s);
    }
  }

  @Test
  public void nonStaticInnerClass() {
    IllegalArgumentException expected =
        assertThrows(
            IllegalArgumentException.class,
            () -> new NullPointerTester().testAllPublicConstructors(Inner.class));
    assertThat(expected).hasMessageThat().contains("inner class");
  }

  @FormatMethod
  private static String rootLocaleFormat(String format, Object... args) {
    return String.format(Locale.ROOT, format, args);
  }

  static class OverridesEquals {
    @SuppressWarnings("EqualsHashCode")
    @Override
    public boolean equals(@Nullable Object o) {
      return true;
    }
  }

  static class DoesNotOverrideEquals {
    public boolean equals(Object a, Object b) {
      return true;
    }
  }

  @Test
  public void equalsMethod() {
    shouldPass(new OverridesEquals());
    shouldFail(new DoesNotOverrideEquals());
  }

  private static final class FailOnOneOfTwoConstructors {
    @Keep
    public FailOnOneOfTwoConstructors(String s) {}

    @Keep
    public FailOnOneOfTwoConstructors(Object o) {
      checkNotNull(o);
    }
  }

  @Test
  public void constructor_ignored_shouldPass() throws Exception {
    new NullPointerTester()
        .ignore(FailOnOneOfTwoConstructors.class.getDeclaredConstructor(String.class))
        .testAllPublicConstructors(FailOnOneOfTwoConstructors.class);
  }

  @Test
  public void constructor_shouldFail() {
    assertThrows(
        "Expected NullPointerTester to fail for constructors of FailOnOneOfTwoConstructors",
        AssertionError.class,
        () -> new NullPointerTester().testAllPublicConstructors(FailOnOneOfTwoConstructors.class));
  }

  public static class NullBounds<T extends @Nullable Object, U extends T, X> {
    boolean xWasCalled;

    public void x(X x) {
      xWasCalled = true;
      checkNotNull(x);
    }

    public void t(T t) {
      fail("Method with parameter <T extends @Nullable Object> should not be called");
    }

    public void u(U u) {
      fail(
          "Method with parameter <U extends T> where <T extends @Nullable Object> should not be"
              + " called");
    }

    public <A extends @Nullable Object> void a(A a) {
      fail("Method with parameter <A extends @Nullable Object> should not be called");
    }

    public <A extends B, B extends @Nullable Object> void b(A a) {
      fail(
          "Method with parameter <A extends B> where <B extends @Nullable Object> should not be"
              + " called");
    }
  }

  @Test
  public void nullBounds() {
    // NullBounds has methods whose parameters are type variables that have
    // "extends @Nullable Object" as a bound. This test ensures that NullPointerTester considers
    // those parameters to be @Nullable, so it won't call the methods.
    NullBounds<?, ?, ?> nullBounds = new NullBounds<>();
    new NullPointerTester().testAllPublicInstanceMethods(nullBounds);
    assertThat(nullBounds.xWasCalled).isTrue();
  }
}
