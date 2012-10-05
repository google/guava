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

package com.google.common.testing;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.junit.contrib.truth.Truth.ASSERT;

import com.google.common.base.Functions;
import com.google.common.collect.ImmutableList;
import com.google.common.testing.ClassSanityTester.FactoryMethodReturnsNullException;
import com.google.common.testing.ClassSanityTester.ParameterNotInstantiableException;
import com.google.common.testing.NullPointerTester.Visibility;

import junit.framework.AssertionFailedError;
import junit.framework.TestCase;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.util.AbstractList;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nullable;

/**
 * Unit tests for {@link ClassSanityTester}.
 *
 * @author Ben Yu
 */
public class ClassSanityTesterTest extends TestCase {

  private final ClassSanityTester tester = new ClassSanityTester();

  public void testEqualsOnReturnValues_good() throws Exception {
    tester.forAllPublicStaticMethods(GoodEqualsFactory.class).testEquals();
  }

  public static class GoodEqualsFactory {
    public static Object good(String a, int b,
        // oneConstantOnly doesn't matter since it's not nullable and can be only 1 value.
        @SuppressWarnings("unused") OneConstantEnum oneConstantOnly,
        // noConstant doesn't matter since it can only be null
        @SuppressWarnings("unused") @Nullable NoConstantEnum noConstant) {
      return new GoodEquals(a, b);
    }
    // instance method ignored
    public Object badIgnored() {
      return new BadEquals();
    }
    // primitive ignored
    public int returnsInt() {
      throw new UnsupportedOperationException();
    }
    // void ignored
    public void voidMethod() {
      throw new UnsupportedOperationException();
    }
    // non-public method ignored
    static Object badButNotPublic() {
      return new BadEquals();
    }
  }

  public void testEqualsOnReturnValues_bad() throws Exception {
    try {
      tester.forAllPublicStaticMethods(BadEqualsFactory.class).testEquals();
    } catch (AssertionFailedError expected) {
      return;
    }
    fail();
  }

  public static class BadEqualsFactory {
    /** oneConstantOnly matters now since it can be either null or the constant. */
    public static Object bad(String a, int b,
        @SuppressWarnings("unused") @Nullable OneConstantEnum oneConstantOnly) {
      return new GoodEquals(a, b);
    }
  }

  public void testNullsOnReturnValues_good() throws Exception {
    tester.forAllPublicStaticMethods(GoodNullsFactory.class).testNulls();
  }

  public static class GoodNullsFactory {
    public static Object good(String s) {
      return new GoodNulls(s);
    }
  }

  public void testNullsOnReturnValues_bad() throws Exception {
    try {
      tester
          .forAllPublicStaticMethods(BadNullsFactory.class)
          .thatReturn(Object.class)
          .testNulls();
    } catch (AssertionFailedError expected) {
      return;
    }
    fail();
  }

  public void testNullsOnReturnValues_returnTypeFiltered() throws Exception {
    tester
        .forAllPublicStaticMethods(BadNullsFactory.class)
        .thatReturn(Iterable.class)
        .testNulls();
  }
  
  public static class BadNullsFactory {
    public static Object bad(@SuppressWarnings("unused") String a) {
      return new BadNulls();
    }
  }

  public void testSerializableOnReturnValues_good() throws Exception {
    tester.forAllPublicStaticMethods(GoodSerializableFactory.class).testSerializable();
  }

  public static class GoodSerializableFactory {
    public static Object good(Runnable r) {
      return r;
    }
    public static Object good(AnInterface i) {
      return i;
    }
  }

  public void testSerializableOnReturnValues_bad() throws Exception {
    try {
      tester.forAllPublicStaticMethods(BadSerializableFactory.class).testSerializable();
      fail();
    } catch (RuntimeException expected) {}
  }

  public static class BadSerializableFactory {
    public static Object bad() {
      return new Serializable() {
        @SuppressWarnings("unused")
        private final Object notSerializable = new Object();
      };
    }
  }

  public void testEqualsAndSerializableOnReturnValues_equalsIsGoodButNotSerializable()
      throws Exception {
    try {
      tester.forAllPublicStaticMethods(GoodEqualsFactory.class).testEqualsAndSerializable();
      return;
    } catch (RuntimeException expected) {}
  }

  public void testEqualsAndSerializableOnReturnValues_serializableButNotEquals()
      throws Exception {
    try {
      tester.forAllPublicStaticMethods(GoodSerializableFactory.class).testEqualsAndSerializable();
    } catch (AssertionFailedError expected) {
      return;
    }
    fail("should have failed");
  }

  public void testEqualsAndSerializableOnReturnValues_good()
      throws Exception {
    tester.forAllPublicStaticMethods(GoodEqualsAndSerialiableFactory.class)
        .testEqualsAndSerializable();
  }

  public static class GoodEqualsAndSerialiableFactory {
    public static Object good(AnInterface s) {
      return Functions.constant(s);
    }
  }

  public void testEqualsForReturnValues_factoryReturnsNullButNotAnnotated() throws Exception {
    try {
      tester.forAllPublicStaticMethods(FactoryThatReturnsNullButNotAnnotated.class)
          .testEquals();
    } catch (AssertionFailedError expected) {
      return;
    }
    fail();
  }

  public void testNullsForReturnValues_factoryReturnsNullButNotAnnotated() throws Exception {
    try {
      tester.forAllPublicStaticMethods(FactoryThatReturnsNullButNotAnnotated.class)
          .testNulls();
    } catch (AssertionFailedError expected) {
      return;
    }
    fail();
  }

  public void testSerializableForReturnValues_factoryReturnsNullButNotAnnotated() throws Exception {
    try {
      tester.forAllPublicStaticMethods(FactoryThatReturnsNullButNotAnnotated.class)
          .testSerializable();
    } catch (AssertionFailedError expected) {
      return;
    }
    fail();
  }

  public void testEqualsAndSerializableForReturnValues_factoryReturnsNullButNotAnnotated()
      throws Exception {
    try {
      tester.forAllPublicStaticMethods(FactoryThatReturnsNullButNotAnnotated.class)
          .testEqualsAndSerializable();
    } catch (AssertionFailedError expected) {
      return;
    }
    fail();
  }

  public static class FactoryThatReturnsNullButNotAnnotated {
    public static Object bad() {
      return null;
    }
  }

  public void testEqualsForReturnValues_factoryReturnsNullAndAnnotated() throws Exception {
    tester.forAllPublicStaticMethods(FactoryThatReturnsNullAndAnnotated.class)
        .testEquals();
  }

  public void testNullsForReturnValues_factoryReturnsNullAndAnnotated() throws Exception {
    tester.forAllPublicStaticMethods(FactoryThatReturnsNullAndAnnotated.class)
        .testNulls();
  }

  public void testSerializableForReturnValues_factoryReturnsNullAndAnnotated() throws Exception {
    tester.forAllPublicStaticMethods(FactoryThatReturnsNullAndAnnotated.class)
        .testSerializable();
  }

  public void testEqualsAndSerializableForReturnValues_factoryReturnsNullAndAnnotated()
      throws Exception {
    tester.forAllPublicStaticMethods(FactoryThatReturnsNullAndAnnotated.class)
        .testEqualsAndSerializable();
  }

  public static class FactoryThatReturnsNullAndAnnotated {
    @Nullable public static Object bad() {
      return null;
    }
  }

  public void testGoodEquals() throws Exception {
    tester.testEquals(GoodEquals.class);
  }

  public void testEquals_interface() {
    tester.testEquals(AnInterface.class);
  }

  public void testEquals_abstractClass() {
    tester.testEquals(AnAbstractClass.class);
  }

  public void testEquals_enum() {
    tester.testEquals(OneConstantEnum.class);
  }

  public void testBadEquals() throws Exception {
    try {
      tester.testEquals(BadEquals.class);
    } catch (AssertionFailedError expected) {
      ASSERT.that(expected.getMessage()).contains("create(null)");
      return;
    }
    fail("should have failed");
  }

  public void testBadEquals_withParameterizedType() throws Exception {
    try {
      tester.testEquals(BadEqualsWithParameterizedType.class);
    } catch (AssertionFailedError expected) {
      ASSERT.that(expected.getMessage()).contains("create([[1]])");
      return;
    }
    fail("should have failed");
  }

  public void testParameterNotInstantiableForEqualsTest() throws Exception {
    try {
      tester.doTestEquals(ConstructorParameterNotInstantiable.class);
      fail("should have failed");
    } catch (ParameterNotInstantiableException expected) {}
  }

  public void testConstructorThrowsForEqualsTest() throws Exception {
    try {
      tester.doTestEquals(ConstructorThrows.class);
      fail("should have failed");
    } catch (InvocationTargetException expected) {}
  }

  public void testFactoryMethodReturnsNullForEqualsTest() throws Exception {
    try {
      tester.doTestEquals(FactoryMethodReturnsNullAndAnnotated.class);
      fail("should have failed");
    } catch (FactoryMethodReturnsNullException expected) {}
  }

  public void testFactoryMethodReturnsNullButNotAnnotatedInEqualsTest() throws Exception {
    try {
      tester.testEquals(FactoryMethodReturnsNullButNotAnnotated.class);
    } catch (AssertionFailedError expected) {
      return;
    }
    fail("should have failed");
  }

  public void testNoEqualsChecksOnEnum() throws Exception {
    tester.testEquals(OneConstantEnum.class);
    tester.testEquals(NoConstantEnum.class);
    tester.testEquals(TimeUnit.class);
  }

  public void testNoEqualsChecksOnInterface() throws Exception {
    tester.testEquals(Runnable.class);
  }

  public void testNoEqualsChecksOnAnnotation() throws Exception {
    tester.testEquals(Nullable.class);
  }

  public void testGoodNulls() throws Exception {
    tester.testNulls(GoodNulls.class);
  }

  public void testNoNullCheckNeededDespitNotInstantiable() throws Exception {
    tester.doTestNulls(NoNullCheckNeededDespitNotInstantiable.class, Visibility.PACKAGE);
  }

  public void testNulls_interface() {
    tester.testNulls(AnInterface.class);
  }

  public void testNulls_abstractClass() {
    tester.testNulls(AnAbstractClass.class);
  }

  public void testNulls_enum() throws Exception {
    tester.testNulls(OneConstantEnum.class);
    tester.testNulls(NoConstantEnum.class);
    tester.testNulls(TimeUnit.class);
  }

  public void testEnumFailsToCheckNull() throws Exception {
    try {
      tester.testNulls(EnumFailsToCheckNull.class);
    } catch (AssertionFailedError expected) {
      return;
    }
    fail("should have failed");
  }

  public void testNoNullChecksOnInterface() throws Exception {
    tester.testNulls(Runnable.class);
  }

  public void testNoNullChecksOnAnnotation() throws Exception {
    tester.testNulls(Nullable.class);
  }

  public void testBadNulls() throws Exception {
    try {
      tester.testNulls(BadNulls.class);
    } catch (AssertionFailedError expected) {
      return;
    }
    fail("should have failed");
  }

  public void testInstantiate_factoryMethodReturnsNullButNotAnnotated() throws Exception {
    try {
      tester.instantiate(FactoryMethodReturnsNullButNotAnnotated.class);
    } catch (AssertionFailedError expected) {
      ASSERT.that(expected.getMessage()).contains("@Nullable");
      return;
    }
    fail("should have failed");
  }

  public void testInstantiate_factoryMethodReturnsNullAndAnnotated() throws Exception {
    try {
      tester.instantiate(FactoryMethodReturnsNullAndAnnotated.class);
      fail("should have failed");
    } catch (FactoryMethodReturnsNullException expected) {}
  }

  public void testInstantiate_factoryMethodAcceptsNull() throws Exception {
    assertNull(tester.instantiate(FactoryMethodAcceptsNull.class).name);
  }

  public void testInstantiate_factoryMethodDoesNotAcceptNull() throws Exception {
    assertNotNull(tester.instantiate(FactoryMethodDoesNotAcceptNull.class).name);
  }

  public void testInstantiate_constructorAcceptsNull() throws Exception {
    assertNull(tester.instantiate(ConstructorAcceptsNull.class).name);
  }

  public void testInstantiate_constructorDoesNotAcceptNull() throws Exception {
    assertNotNull(tester.instantiate(ConstructorDoesNotAcceptNull.class).name);
  }

  public void testInstantiate_notInstantiable() throws Exception {
    assertNull(tester.instantiate(NotInstantiable.class));
  }

  public void testInstantiate_noConstantEnum() throws Exception {
    assertNull(tester.instantiate(NoConstantEnum.class));
  }

  public void testInstantiate_oneConstantEnum() throws Exception {
    assertEquals(OneConstantEnum.A, tester.instantiate(OneConstantEnum.class));
  }

  public void testInstantiate_interface() throws Exception {
    assertNull(tester.instantiate(Runnable.class));
  }

  public void testInstantiate_abstractClass() throws Exception {
    assertNull(tester.instantiate(AbstractList.class));
  }

  public void testInstantiate_annotation() throws Exception {
    assertNull(tester.instantiate(Nullable.class));
  }

  public void testInstantiate_setDefault() throws Exception {
    NotInstantiable x = new NotInstantiable();
    tester.setDefault(NotInstantiable.class, x);
    assertNotNull(tester.instantiate(ConstructorParameterNotInstantiable.class));
  }

  public void testInstantiate_setSampleInstances() throws Exception {
    NotInstantiable x = new NotInstantiable();
    tester.setSampleInstances(NotInstantiable.class, ImmutableList.of(x));
    assertNotNull(tester.instantiate(ConstructorParameterNotInstantiable.class));
  }

  public void testInstantiate_setSampleInstances_empty() throws Exception {
    tester.setSampleInstances(NotInstantiable.class, ImmutableList.<NotInstantiable>of());
    try {
      tester.instantiate(ConstructorParameterNotInstantiable.class);
      fail();
    } catch (ParameterNotInstantiableException expected) {}
  }

  public void testInstantiate_constructorThrows() throws Exception {
    try {
      tester.instantiate(ConstructorThrows.class);
      fail();
    } catch (InvocationTargetException expected) {}
  }

  public void testInstantiate_factoryMethodThrows() throws Exception {
    try {
      tester.instantiate(FactoryMethodThrows.class);
      fail();
    } catch (InvocationTargetException expected) {}
  }

  public void testInstantiate_constructorParameterNotInstantiable() throws Exception {
    try {
      tester.instantiate(ConstructorParameterNotInstantiable.class);
      fail();
    } catch (ParameterNotInstantiableException expected) {}
  }

  public void testInstantiate_factoryMethodParameterNotInstantiable() throws Exception {
    try {
      tester.instantiate(FactoryMethodParameterNotInstantiable.class);
      fail();
    } catch (ParameterNotInstantiableException expected) {}
  }

  public void testInstantiate_instantiableFactoryMethodChosen() throws Exception {
    assertEquals("good", tester.instantiate(InstantiableFactoryMethodChosen.class).name);
  }

  public void testInterfaceProxySerializable() throws Exception {
    SerializableTester.reserializeAndAssert(tester.instantiate(HasAnInterface.class));
  }

  static class HasAnInterface implements Serializable {
    private final AnInterface i;

    public HasAnInterface(AnInterface i) {
      this.i = i;
    }

    @Override public boolean equals(@Nullable Object obj) {
      if (obj instanceof HasAnInterface) {
        HasAnInterface that = (HasAnInterface) obj;
        return i.equals(that.i);
      } else {
        return false;
      }
    }

    @Override public int hashCode() {
      return i.hashCode();
    }
  }

  static class InstantiableFactoryMethodChosen {
    final String name;

    private InstantiableFactoryMethodChosen(String name) {
      this.name = name;
    }

    public InstantiableFactoryMethodChosen(NotInstantiable x) {
      checkNotNull(x);
      this.name = "x1";
    }

    public static InstantiableFactoryMethodChosen create(NotInstantiable x) {
      return new InstantiableFactoryMethodChosen(x);
    }

    public static InstantiableFactoryMethodChosen create(String s) {
      checkNotNull(s);
      return new InstantiableFactoryMethodChosen("good");
    }
  }

  public void testInstantiate_instantiableConstructorChosen() throws Exception {
    assertEquals("good", tester.instantiate(InstantiableConstructorChosen.class).name);
  }

  static class InstantiableConstructorChosen {
    final String name;

    public InstantiableConstructorChosen(String name) {
      checkNotNull(name);
      this.name = "good";
    }

    public InstantiableConstructorChosen(NotInstantiable x) {
      checkNotNull(x);
      this.name = "x1";
    }

    public static InstantiableFactoryMethodChosen create(NotInstantiable x) {
      return new InstantiableFactoryMethodChosen(x);
    }
  }

  static class GoodEquals {

    private final String a;
    private final int b;

    private GoodEquals(String a, int b) {
      this.a = checkNotNull(a);
      this.b = b;
    }

    // ignored by testEquals()
    GoodEquals(@SuppressWarnings("unused") NotInstantiable x) {
      this.a = "x";
      this.b = -1;
    }

    // will keep trying
    public GoodEquals(@SuppressWarnings("unused") NotInstantiable x, int b) {
      this.a = "x";
      this.b = b;
    }

    // keep trying
    @SuppressWarnings("unused")
    static GoodEquals create(int a, int b) {
      throw new RuntimeException();
    }

    // keep trying
    @SuppressWarnings("unused")
    @Nullable public static GoodEquals createMayReturnNull(int a, int b) {
      return null;
    }

    // Good!
    static GoodEquals create(String a, int b) {
      return new GoodEquals(a, b);
    }

    @Override public boolean equals(@Nullable Object obj) {
      if (obj instanceof GoodEquals) {
        GoodEquals that = (GoodEquals) obj;
        return a.equals(that.a) && b == that.b;
      } else {
        return false;
      }
    }

    @Override public int hashCode() {
      return 0;
    }
  }

  static class BadEquals {

    public BadEquals() {} // ignored by testEquals() since it has less parameters.

    public static BadEquals create(@SuppressWarnings("unused") @Nullable String s) {
      return new BadEquals();
    }

    @Override public boolean equals(@Nullable Object obj) {
      return obj instanceof BadEquals;
    }

    @Override public int hashCode() {
      return 0;
    }
  }

  static class BadEqualsWithParameterizedType {

    // ignored by testEquals() since it has less parameters.
    public BadEqualsWithParameterizedType() {}

    public static BadEqualsWithParameterizedType create(
        @SuppressWarnings("unused") ImmutableList<Iterable<? extends String>> s) {
      return new BadEqualsWithParameterizedType();
    }

    @Override public boolean equals(@Nullable Object obj) {
      return obj instanceof BadEqualsWithParameterizedType;
    }

    @Override public int hashCode() {
      return 0;
    }
  }

  static class GoodNulls {
    public GoodNulls(String s) {
      checkNotNull(s);
    }

    public void rejectNull(String s) {
      checkNotNull(s);
    }
  }

  public static class BadNulls {
    public void failsToRejectNull(@SuppressWarnings("unused") String s) {}
  }

  public static class NoNullCheckNeededDespitNotInstantiable {

    public NoNullCheckNeededDespitNotInstantiable(NotInstantiable x) {
      checkNotNull(x);
    }

    @SuppressWarnings("unused") // reflected
    void primitiveOnly(int i) {}

    @SuppressWarnings("unused") //reflected
    void nullableOnly(@Nullable String s) {}
    public void noParameter() {}

    @SuppressWarnings("unused") //reflected
    void primitiveAndNullable(@Nullable String s, int i) {}
  }

  static class FactoryMethodReturnsNullButNotAnnotated {
    private FactoryMethodReturnsNullButNotAnnotated() {}

    static FactoryMethodReturnsNullButNotAnnotated returnsNull() {
      return null;
    }
  }

  static class FactoryMethodReturnsNullAndAnnotated {
    private FactoryMethodReturnsNullAndAnnotated() {}

    @Nullable public static FactoryMethodReturnsNullAndAnnotated returnsNull() {
      return null;
    }
  }

  static class FactoryMethodAcceptsNull {

    final String name;

    private FactoryMethodAcceptsNull(String name) {
      this.name = name;
    }

    static FactoryMethodAcceptsNull create(@Nullable String name) {
      return new FactoryMethodAcceptsNull(name);
    }
  }

  static class FactoryMethodDoesNotAcceptNull {

    final String name;

    private FactoryMethodDoesNotAcceptNull(String name) {
      this.name = checkNotNull(name);
    }

    public static FactoryMethodDoesNotAcceptNull create(String name) {
      return new FactoryMethodDoesNotAcceptNull(name);
    }
  }

  static class ConstructorAcceptsNull {

    final String name;

    public ConstructorAcceptsNull(@Nullable String name) {
      this.name = name;
    }
  }

  static class ConstructorDoesNotAcceptNull {

    final String name;

    ConstructorDoesNotAcceptNull(String name) {
      this.name = checkNotNull(name);
    }
  }

  static class ConstructorParameterNotInstantiable {
    public ConstructorParameterNotInstantiable(@SuppressWarnings("unused") NotInstantiable x) {}
  }

  static class FactoryMethodParameterNotInstantiable {

    private FactoryMethodParameterNotInstantiable() {}

    static FactoryMethodParameterNotInstantiable create(
        @SuppressWarnings("unused") NotInstantiable x) {
      return new FactoryMethodParameterNotInstantiable();
    }
  }

  static class ConstructorThrows {
    public ConstructorThrows() {
      throw new RuntimeException();
    }
  }

  static class FactoryMethodThrows {
    private FactoryMethodThrows() {}

    public static FactoryMethodThrows create() {
      throw new RuntimeException();
    }
  }

  static class NotInstantiable {
    private NotInstantiable() {}
  }

  private enum NoConstantEnum {}

  private enum OneConstantEnum {
    A
  }

  private enum EnumFailsToCheckNull {
    A;

    @SuppressWarnings("unused") 
    public void failToCheckNull(String s) {}
  }

  private interface AnInterface {}

  private static abstract class AnAbstractClass {
    @SuppressWarnings("unused")
    public AnAbstractClass(String s) {}

    @SuppressWarnings("unused")
    public void failsToCheckNull(String s) {}
  }
}
