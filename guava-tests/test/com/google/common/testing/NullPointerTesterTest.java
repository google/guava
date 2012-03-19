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

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Function;
import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableMultiset;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.ImmutableTable;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multiset;
import com.google.common.collect.Sets;
import com.google.common.collect.Table;
import com.google.common.reflect.TypeToken;
import com.google.common.testing.NullPointerTester.Visibility;

import junit.framework.AssertionFailedError;
import junit.framework.TestCase;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;

import javax.annotation.Nullable;

/**
 * Unit test for {@link NullPointerTester}.
 *
 * @author Kevin Bourrillion
 * @author Mick Killianey
 */
public class NullPointerTesterTest extends TestCase {

  private NullPointerTester tester;

  @Override protected void setUp() throws Exception {
    super.setUp();
    tester = new NullPointerTester();
  }

  /** Non-NPE RuntimeException. */
  public static class FooException extends RuntimeException {
    private static final long serialVersionUID = 1L;
  }

  /**
   * Class for testing all permutations of static/non-static one-argument
   * methods using methodParameter().
   */
  public static class OneArg {

    public static void staticOneArgCorrectlyThrowsNpe(String s) {
      checkNotNull(s); // expect NPE here on null
    }
    public static void staticOneArgThrowsOtherThanNpe(String s) {
      throw new FooException();  // should catch as failure
    }
    public static void staticOneArgShouldThrowNpeButDoesnt(String s) {
      // should catch as failure
    }
    public static void
    staticOneArgNullableCorrectlyDoesNotThrowNPE(@Nullable String s) {
      // null?  no problem
    }
    public static void
    staticOneArgNullableCorrectlyThrowsOtherThanNPE(@Nullable String s) {
      throw new FooException(); // ok, as long as it's not NullPointerException
    }
    public static void
    staticOneArgNullableThrowsNPE(@Nullable String s) {
      checkNotNull(s); // doesn't check if you said you'd accept null, but you don't
    }

    public void oneArgCorrectlyThrowsNpe(String s) {
      checkNotNull(s); // expect NPE here on null
    }
    public void oneArgThrowsOtherThanNpe(String s) {
      throw new FooException();  // should catch as failure
    }
    public void oneArgShouldThrowNpeButDoesnt(String s) {
      // should catch as failure
    }
    public void oneArgNullableCorrectlyDoesNotThrowNPE(@Nullable String s) {
      // null?  no problem
    }
    public void oneArgNullableCorrectlyThrowsOtherThanNPE(@Nullable String s) {
      throw new FooException(); // ok, as long as it's not NullPointerException
    }
    public void oneArgNullableThrowsNPE(@Nullable String s) {
      checkNotNull(s); // doesn't check if you said you'd accept null, but you don't
    }
  }

  private static final String[] STATIC_ONE_ARG_METHODS_SHOULD_PASS = {
    "staticOneArgCorrectlyThrowsNpe",
    "staticOneArgNullableCorrectlyDoesNotThrowNPE",
    "staticOneArgNullableCorrectlyThrowsOtherThanNPE",
    "staticOneArgNullableThrowsNPE",
  };
  private static final String[] STATIC_ONE_ARG_METHODS_SHOULD_FAIL = {
    "staticOneArgThrowsOtherThanNpe",
    "staticOneArgShouldThrowNpeButDoesnt",
  };
  private static final String[] NONSTATIC_ONE_ARG_METHODS_SHOULD_PASS = {
    "oneArgCorrectlyThrowsNpe",
    "oneArgNullableCorrectlyDoesNotThrowNPE",
    "oneArgNullableCorrectlyThrowsOtherThanNPE",
    "oneArgNullableThrowsNPE",
  };
  private static final String[] NONSTATIC_ONE_ARG_METHODS_SHOULD_FAIL = {
    "oneArgThrowsOtherThanNpe",
    "oneArgShouldThrowNpeButDoesnt",
  };

  public void testStaticOneArgMethodsThatShouldPass() throws Exception {
    for (String methodName : STATIC_ONE_ARG_METHODS_SHOULD_PASS) {
      Method method = OneArg.class.getMethod(methodName, String.class);
      try {
        tester.testMethodParameter(OneArg.class, method, 0);
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
        tester.testMethodParameter(OneArg.class, method, 0);
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
        tester.testMethodParameter(foo, method, 0);
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
        tester.testMethodParameter(foo, method, 0);
      } catch (AssertionFailedError expected) {
        foundProblem = true;
      }
      assertTrue("Should report error in method " + methodName, foundProblem);
    }
  }

  /**
   * Class for testing all permutations of nullable/non-nullable two-argument
   * methods using testMethod().
   *
   *   normalNormal:  two params, neither is Nullable
   *   nullableNormal:  only first param is Nullable
   *   normalNullable:  only second param is Nullable
   *   nullableNullable:  both params are Nullable
   */
  public static class TwoArg {
    /** Action to take on a null param. */
    public enum Action {
      THROW_A_NPE {
        @Override public void act() {
          throw new NullPointerException();
        }
      },
      THROW_OTHER {
        @Override public void act() {
          throw new FooException();
        }
      },
      JUST_RETURN {
        @Override public void act() {}
      };

      public abstract void act();
    }
    Action actionWhenFirstParamIsNull;
    Action actionWhenSecondParamIsNull;

    public TwoArg(
        Action actionWhenFirstParamIsNull,
        Action actionWhenSecondParamIsNull) {
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
    public void nullableNullable(
        @Nullable String first, @Nullable Integer second) {
      reactToNullParameters(first, second);
    }

    /** To provide sanity during debugging. */
    @Override public String toString() {
      return String.format("Bar(%s, %s)",
          actionWhenFirstParamIsNull, actionWhenSecondParamIsNull);
    }
  }

  public void verifyBarPass(Method method, TwoArg bar) {
    try {
      tester.testMethod(bar, method);
    } catch (AssertionFailedError incorrectError) {
      String errorMessage = String.format(
          "Should not have flagged method %s for %s", method.getName(), bar);
      assertNull(errorMessage, incorrectError);
    }
  }

  public void verifyBarFail(Method method, TwoArg bar) {
    try {
      tester.testMethod(bar, method);
    } catch (AssertionFailedError expected) {
      return; // good...we wanted a failure
    }
    String errorMessage = String.format(
        "Should have flagged method %s for %s", method.getName(), bar);
    fail(errorMessage);
  }

  public void testTwoArgNormalNormal() throws Exception {
    Method method = TwoArg.class.getMethod(
        "normalNormal", String.class, Integer.class);
    for (TwoArg.Action first : TwoArg.Action.values()) {
      for (TwoArg.Action second : TwoArg.Action.values()) {
        TwoArg bar = new TwoArg(first, second);
        if (first.equals(TwoArg.Action.THROW_A_NPE)
            && second.equals(TwoArg.Action.THROW_A_NPE)) {
          verifyBarPass(method, bar); // require both params to throw NPE
        } else {
          verifyBarFail(method, bar);
        }
      }
    }
  }

  public void testTwoArgNormalNullable() throws Exception {
    Method method = TwoArg.class.getMethod(
        "normalNullable", String.class, Integer.class);
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
    Method method = TwoArg.class.getMethod(
        "nullableNormal", String.class, Integer.class);
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
    Method method = TwoArg.class.getMethod(
        "nullableNullable", String.class, Integer.class);
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
   *
   * Add naughty classes to failClasses to verify that NullPointerTest
   * raises an AssertionFailedError.
   *
   * Add acceptable classes to passClasses to verify that NullPointerTest
   * doesn't complain.
   */

  /** List of classes that NullPointerTester should pass as acceptable. */
  static List<Class<?>> failClasses = Lists.newArrayList();

  /** List of classes that NullPointerTester should flag as problematic. */
  static List<Class<?>> passClasses = Lists.newArrayList();

  /** Lots of well-behaved methods. */
  public static class PassObject {
    public static void doThrow(Object arg) {
      if (arg == null) {
        throw new FooException();
      }
    }
    public void noArg() {}
    public void oneArg(String s) { checkNotNull(s); }
    public void oneNullableArg(@Nullable String s) {}
    public void oneNullableArgThrows(@Nullable String s) { doThrow(s); }

    public void twoArg(String s, Integer i) { checkNotNull(s); i.intValue(); }
    public void twoMixedArgs(String s, @Nullable Integer i) { checkNotNull(s); }
    public void twoMixedArgsThrows(String s, @Nullable Integer i) {
      checkNotNull(s); doThrow(i);
    }
    public void twoMixedArgs(@Nullable Integer i, String s) { checkNotNull(s); }
    public void twoMixedArgsThrows(@Nullable Integer i, String s) {
      checkNotNull(s); doThrow(i);
    }
    public void twoNullableArgs(@Nullable String s,
        @javax.annotation.Nullable Integer i) { }
    public void twoNullableArgsThrowsFirstArg(
        @Nullable String s, @Nullable Integer i) {
      doThrow(s);
    }
    public void twoNullableArgsThrowsSecondArg(
        @Nullable String s, @Nullable Integer i) {
      doThrow(i);
    }
    public static void staticOneArg(String s) { checkNotNull(s); }
    public static void staticOneNullableArg(@Nullable String s) { }
    public static void staticOneNullableArgThrows(@Nullable String s) {
      doThrow(s);
    }
  }
  static { passClasses.add(PassObject.class); }

  static class FailOneArgDoesntThrowNPE extends PassObject {
    @Override public void oneArg(String s) {
      // Fail:  missing NPE for s
    }
  }
  static { failClasses.add(FailOneArgDoesntThrowNPE.class); }

  static class FailOneArgThrowsWrongType extends PassObject {
    @Override public void oneArg(String s) {
      doThrow(s); // Fail:  throwing non-NPE exception for null s
    }
  }
  static { failClasses.add(FailOneArgThrowsWrongType.class); }

  static class PassOneNullableArgThrowsNPE extends PassObject {
    @Override public void oneNullableArg(@Nullable String s) {
      checkNotNull(s); // ok to throw NPE
    }
  }
  static { passClasses.add(PassOneNullableArgThrowsNPE.class); }

  static class FailTwoArgsFirstArgDoesntThrowNPE extends PassObject {
    @Override public void twoArg(String s, Integer i) {
      // Fail: missing NPE for s
      i.intValue();
    }
  }
  static { failClasses.add(FailTwoArgsFirstArgDoesntThrowNPE.class); }

  static class FailTwoArgsFirstArgThrowsWrongType extends PassObject {
    @Override public void twoArg(String s, Integer i) {
      doThrow(s); // Fail:  throwing non-NPE exception for null s
      i.intValue();
    }
  }
  static { failClasses.add(FailTwoArgsFirstArgThrowsWrongType.class); }

  static class FailTwoArgsSecondArgDoesntThrowNPE extends PassObject {
    @Override public void twoArg(String s, Integer i) {
      checkNotNull(s);
      // Fail: missing NPE for i
    }
  }
  static { failClasses.add(FailTwoArgsSecondArgDoesntThrowNPE.class); }

  static class FailTwoArgsSecondArgThrowsWrongType extends PassObject {
    @Override public void twoArg(String s, Integer i) {
      checkNotNull(s);
      doThrow(i); // Fail:  throwing non-NPE exception for null i
    }
  }
  static { failClasses.add(FailTwoArgsSecondArgThrowsWrongType.class); }

  static class FailTwoMixedArgsFirstArgDoesntThrowNPE extends PassObject {
    @Override public void twoMixedArgs(String s, @Nullable Integer i) {
      // Fail: missing NPE for s
    }
  }
  static { failClasses.add(FailTwoMixedArgsFirstArgDoesntThrowNPE.class); }

  static class FailTwoMixedArgsFirstArgThrowsWrongType extends PassObject {
    @Override public void twoMixedArgs(String s, @Nullable Integer i) {
      doThrow(s); // Fail:  throwing non-NPE exception for null s
    }
  }
  static { failClasses.add(FailTwoMixedArgsFirstArgThrowsWrongType.class); }

  static class PassTwoMixedArgsNullableArgThrowsNPE extends PassObject {
    @Override public void twoMixedArgs(String s, @Nullable Integer i) {
      checkNotNull(s);
      i.intValue(); // ok to throw NPE?
    }
  }
  static { passClasses.add(PassTwoMixedArgsNullableArgThrowsNPE.class); }

  static class PassTwoMixedArgSecondNullableArgThrowsOther extends PassObject {
    @Override public void twoMixedArgs(String s, @Nullable Integer i) {
      checkNotNull(s);
      doThrow(i); // ok to throw non-NPE exception for null i
    }
  }
  static { passClasses.add(PassTwoMixedArgSecondNullableArgThrowsOther.class); }

  static class FailTwoMixedArgsSecondArgDoesntThrowNPE extends PassObject {
    @Override public void twoMixedArgs(@Nullable Integer i, String s) {
      // Fail: missing NPE for null s
    }
  }
  static { failClasses.add(FailTwoMixedArgsSecondArgDoesntThrowNPE.class); }

  static class FailTwoMixedArgsSecondArgThrowsWrongType extends PassObject {
    @Override public void twoMixedArgs(@Nullable Integer i, String s) {
      doThrow(s); // Fail:  throwing non-NPE exception for null s
    }
  }
  static { failClasses.add(FailTwoMixedArgsSecondArgThrowsWrongType.class); }

  static class PassTwoNullableArgsFirstThrowsNPE extends PassObject {
    @Override public void twoNullableArgs(
        @Nullable String s, @Nullable Integer i) {
      checkNotNull(s); // ok to throw NPE?
    }
  }
  static { passClasses.add(PassTwoNullableArgsFirstThrowsNPE.class); }

  static class PassTwoNullableArgsFirstThrowsOther extends PassObject {
    @Override public void twoNullableArgs(
        @Nullable String s, @Nullable Integer i) {
      doThrow(s); // ok to throw non-NPE exception for null s
    }
  }
  static { passClasses.add(PassTwoNullableArgsFirstThrowsOther.class); }

  static class PassTwoNullableArgsSecondThrowsNPE extends PassObject {
    @Override public void twoNullableArgs(
        @Nullable String s, @Nullable Integer i) {
      i.intValue(); // ok to throw NPE?
    }
  }
  static { passClasses.add(PassTwoNullableArgsSecondThrowsNPE.class); }

  static class PassTwoNullableArgsSecondThrowsOther extends PassObject {
    @Override public void twoNullableArgs(
        @Nullable String s, @Nullable Integer i) {
      doThrow(i); // ok to throw non-NPE exception for null i
    }
  }
  static { passClasses.add(PassTwoNullableArgsSecondThrowsOther.class); }

  static class PassTwoNullableArgsNeitherThrowsAnything extends PassObject {
    @Override public void twoNullableArgs(
        @Nullable String s, @Nullable Integer i) {
      // ok to do nothing
    }
  }
  static { passClasses.add(PassTwoNullableArgsNeitherThrowsAnything.class); }

  /** Sanity check:  it's easy to make typos. */
  private void checkClasses(String message, List<Class<?>> classes) {
    Set<Class<?>> set = Sets.newHashSet(classes);
    for (Class<?> clazz : classes) {
      if (!set.remove(clazz)) {
        fail(String.format("%s: %s appears twice. Typo?",
            message, clazz.getSimpleName()));
      }
    }
  }

  public void testDidntMakeTypoInTestCases() {
    checkClasses("passClass", passClasses);
    checkClasses("failClasses", failClasses);
    List<Class<?>> allClasses = Lists.newArrayList(passClasses);
    allClasses.addAll(failClasses);
    checkClasses("allClasses", allClasses);
  }

  public void testShouldNotFindProblemInPassClass() throws Exception {
    for (Class<?> passClass : passClasses) {
      Object instance = passClass.newInstance();
      try {
        tester.testAllPublicInstanceMethods(instance);
      } catch (AssertionFailedError e) {
        assertNull("Should not detect problem in " + passClass.getSimpleName(),
            e);
      }
    }
  }

  public void testShouldFindProblemInFailClass() throws Exception {
    for (Class<?> failClass : failClasses) {
      Object instance = failClass.newInstance();
      boolean foundProblem = false;
      try {
        tester.testAllPublicInstanceMethods(instance);
      } catch (AssertionFailedError e) {
        foundProblem = true;
      }
      assertTrue("Should detect problem in " + failClass.getSimpleName(),
          foundProblem);
    }
  }

  private static class PrivateClassWithPrivateConstructor {
    private PrivateClassWithPrivateConstructor(@Nullable Integer argument) {}
  }

  public void testPrivateClass() {
    NullPointerTester tester = new NullPointerTester();
    for (Constructor<?> constructor
        : PrivateClassWithPrivateConstructor.class.getDeclaredConstructors()) {
      tester.testConstructor(constructor);
    }
  }
  
  private interface Foo<T> {
    void doSomething(T bar, Integer baz);
  }
  
  private static class StringFoo implements Foo<String> {

    @Override public void doSomething(String bar, Integer baz) {
      checkNotNull(bar);
      checkNotNull(baz);
    }
  }
  
  public void testBridgeMethodIgnored() {
    new NullPointerTester().testAllPublicInstanceMethods(new StringFoo());
  }

  private static abstract class DefaultValueChecker {
    
    private final Map<Integer, Object> arguments = Maps.newHashMap();

    final DefaultValueChecker runTester() {
      new NullPointerTester()
          .testAllPublicInstanceMethods(this);
      return this;
    }
    
    final void assertNonNullValues(Object... expectedValues) {
      assertEquals(expectedValues.length, arguments.size());
      for (int i = 0; i < expectedValues.length; i++) {
        assertEquals("Default value for parameter #" + i,
            expectedValues[i], arguments.get(i));
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
    MALE, FEMALE
  }
  
  private static class AllDefaultValuesChecker extends DefaultValueChecker {

    @SuppressWarnings("unused") // called by NullPointerTester
    public void checkDefaultValuesForTheseTypes(
        Gender gender,
        Integer integer, int i,
        String string, CharSequence charSequence,
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
          integer, i,
          string, charSequence,
          list, immutableList,
          map, immutableMap,
          set, immutableSet,
          sortedSet, immutableSortedSet,
          multiset, immutableMultiset,
          multimap, immutableMultimap,
          table, immutableTable);
    }

    final void check() {
      runTester().assertNonNullValues(
          Gender.MALE,
          Integer.valueOf(0), 0,
          "", "",
          ImmutableList.of(), ImmutableList.of(),
          ImmutableMap.of(), ImmutableMap.of(),
          ImmutableSet.of(), ImmutableSet.of(),
          ImmutableSortedSet.of(), ImmutableSortedSet.of(),
          ImmutableMultiset.of(), ImmutableMultiset.of(),
          ImmutableMultimap.of(), ImmutableMultimap.of(),
          ImmutableTable.of(), ImmutableTable.of());
    }
  }

  public void testDefaultValues() {
    new AllDefaultValuesChecker().check();
  }

  private static class ObjectArrayDefaultValueChecker
      extends DefaultValueChecker {

    @SuppressWarnings("unused") // called by NullPointerTester
    public void checkArray(Object[] array, String s) {
      calledWith(array, s);
    }

    void check() {
      runTester();
      Object[] defaultArray = (Object[]) getDefaultParameterValue(0);
      assertEquals(0, defaultArray.length);
    }
  }

  public void testObjectArrayDefaultValue() {
    new ObjectArrayDefaultValueChecker().check();
  }

  private static class StringArrayDefaultValueChecker
      extends DefaultValueChecker {

    @SuppressWarnings("unused") // called by NullPointerTester
    public void checkArray(String[] array, String s) {
      calledWith(array, s);
    }

    void check() {
      runTester();
      String[] defaultArray = (String[]) getDefaultParameterValue(0);
      assertEquals(0, defaultArray.length);
    }
  }

  public void testStringArrayDefaultValue() {
    new StringArrayDefaultValueChecker().check();
  }

  private static class IntArrayDefaultValueChecker
      extends DefaultValueChecker {

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

  private static class EmptyEnumDefaultValueChecker
      extends DefaultValueChecker {

    @SuppressWarnings("unused") // called by NullPointerTester
    public void checkArray(EmptyEnum object, String s) {
      calledWith(object, s);
    }

    void check() {
      try {
        runTester();
      } catch (AssertionError expected) {
        return;
      }
      fail("Should have failed because enum has no constant");
    }
  }

  public void testEmptyEnumDefaultValue() {
    new EmptyEnumDefaultValueChecker().check();
  }

  private static class GenericClassTypeDefaultValueChecker
      extends DefaultValueChecker {

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

  private static class NonGenericClassTypeDefaultValueChecker
      extends DefaultValueChecker {

    @SuppressWarnings("unused") // called by NullPointerTester
    public void checkArray(Class cls, String s) {
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

  private static class GenericTypeTokenDefaultValueChecker
      extends DefaultValueChecker {

    @SuppressWarnings("unused") // called by NullPointerTester
    public void checkArray(
        TypeToken<? extends List<? super Number>> type, String s) {
      calledWith(type, s);
    }

    void check() {
      runTester();
      TypeToken<?> defaultType = (TypeToken<?>) getDefaultParameterValue(0);
      assertTrue(new TypeToken<List<? super Number>>() {}
          .isAssignableFrom(defaultType));
    }
  }

  public void testGenericTypeTokenDefaultValue() {
    new GenericTypeTokenDefaultValueChecker().check();
  }

  private static class NonGenericTypeTokenDefaultValueChecker
      extends DefaultValueChecker {

    @SuppressWarnings("unused") // called by NullPointerTester
    public void checkArray(TypeToken type, String s) {
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

  private static class GenericInterfaceDefaultValueChecker
      extends DefaultValueChecker {

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

  private static class MultipleInterfacesDefaultValueChecker
      extends DefaultValueChecker {

    @SuppressWarnings("unused") // called by NullPointerTester
    public <T extends FromTo<String, Integer> & Supplier<Long>> void checkArray(
        T f, String s) {
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

  private static class GenericInterface2DefaultValueChecker
      extends DefaultValueChecker {

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

  public void tesGenericInterfaceReturnedByGenericMethod() {
    new GenericInterface2DefaultValueChecker().check();
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
    assertFalse(Visibility.PUBLIC.isVisible(
        VisibilityMethods.class.getDeclaredMethod("privateMethod")));
    assertFalse(Visibility.PUBLIC.isVisible(
        VisibilityMethods.class.getDeclaredMethod("packagePrivateMethod")));
    assertFalse(Visibility.PUBLIC.isVisible(
        VisibilityMethods.class.getDeclaredMethod("protectedMethod")));
    assertTrue(Visibility.PUBLIC.isVisible(
        VisibilityMethods.class.getDeclaredMethod("publicMethod")));
  }

  public void testVisibility_protected() throws Exception {
    assertFalse(Visibility.PROTECTED.isVisible(
        VisibilityMethods.class.getDeclaredMethod("privateMethod")));
    assertFalse(Visibility.PROTECTED.isVisible(
        VisibilityMethods.class.getDeclaredMethod("packagePrivateMethod")));
    assertTrue(Visibility.PROTECTED.isVisible(
        VisibilityMethods.class.getDeclaredMethod("protectedMethod")));
    assertTrue(Visibility.PROTECTED.isVisible(
        VisibilityMethods.class.getDeclaredMethod("publicMethod")));
  }

  public void testVisibility_package() throws Exception {
    assertFalse(Visibility.PACKAGE.isVisible(
        VisibilityMethods.class.getDeclaredMethod("privateMethod")));
    assertTrue(Visibility.PACKAGE.isVisible(
        VisibilityMethods.class.getDeclaredMethod("packagePrivateMethod")));
    assertTrue(Visibility.PACKAGE.isVisible(
        VisibilityMethods.class.getDeclaredMethod("protectedMethod")));
    assertTrue(Visibility.PACKAGE.isVisible(
        VisibilityMethods.class.getDeclaredMethod("publicMethod")));
  }

  /*
   *
   * TODO(kevinb): This is only a very small start.
   * Must come back and finish.
   *
   */

}
