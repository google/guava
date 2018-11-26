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
import static com.google.common.truth.Truth.assertThat;

import com.google.common.base.Functions;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.testing.ClassSanityTester.FactoryMethodReturnsNullException;
import com.google.common.testing.ClassSanityTester.ParameterHasNoDistinctValueException;
import com.google.common.testing.ClassSanityTester.ParameterNotInstantiableException;
import com.google.common.testing.NullPointerTester.Visibility;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import junit.framework.AssertionFailedError;
import junit.framework.TestCase;
import org.checkerframework.checker.nullness.qual.Nullable;

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
    public static Object good(
        String a,
        int b,
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

  public void testForAllPublicStaticMethods_noPublicStaticMethods() throws Exception {
    try {
      tester.forAllPublicStaticMethods(NoPublicStaticMethods.class).testEquals();
    } catch (AssertionFailedError expected) {
      assertThat(expected)
          .hasMessageThat()
          .isEqualTo(
              "No public static methods that return java.lang.Object or subtype are found in "
                  + NoPublicStaticMethods.class
                  + ".");
      return;
    }
    fail();
  }

  public void testEqualsOnReturnValues_bad() throws Exception {
    try {
      tester.forAllPublicStaticMethods(BadEqualsFactory.class).testEquals();
    } catch (AssertionFailedError expected) {
      return;
    }
    fail();
  }

  private static class BadEqualsFactory {
    /** oneConstantOnly matters now since it can be either null or the constant. */
    @SuppressWarnings("unused") // Called by reflection
    public static Object bad(String a, int b, @Nullable OneConstantEnum oneConstantOnly) {
      return new GoodEquals(a, b);
    }
  }

  public void testNullsOnReturnValues_good() throws Exception {
    tester.forAllPublicStaticMethods(GoodNullsFactory.class).testNulls();
  }

  private static class GoodNullsFactory {
    @SuppressWarnings("unused") // Called by reflection
    public static Object good(String s) {
      return new GoodNulls(s);
    }
  }

  public void testNullsOnReturnValues_bad() throws Exception {
    try {
      tester.forAllPublicStaticMethods(BadNullsFactory.class).thatReturn(Object.class).testNulls();
    } catch (AssertionFailedError expected) {
      return;
    }
    fail();
  }

  public void testNullsOnReturnValues_returnTypeFiltered() throws Exception {
    try {
      tester
          .forAllPublicStaticMethods(BadNullsFactory.class)
          .thatReturn(Iterable.class)
          .testNulls();
    } catch (AssertionFailedError expected) {
      assertThat(expected)
          .hasMessageThat()
          .isEqualTo(
              "No public static methods that return java.lang.Iterable or subtype are found in "
                  + BadNullsFactory.class
                  + ".");
      return;
    }
    fail();
  }

  public static class BadNullsFactory {
    public static Object bad(@SuppressWarnings("unused") String a) {
      return new BadNulls();
    }
  }

  @AndroidIncompatible // TODO(cpovirk): ClassNotFoundException... ClassSanityTesterTest$AnInterface
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
    } catch (AssertionFailedError expected) {
      return;
    }
    fail();
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
    } catch (AssertionFailedError expected) {
      return;
    }
    fail("should have failed");
  }

  public void testEqualsAndSerializableOnReturnValues_serializableButNotEquals() throws Exception {
    try {
      tester.forAllPublicStaticMethods(GoodSerializableFactory.class).testEqualsAndSerializable();
    } catch (AssertionFailedError expected) {
      return;
    }
    fail("should have failed");
  }

  @AndroidIncompatible // TODO(cpovirk): ClassNotFoundException... ClassSanityTesterTest$AnInterface
  public void testEqualsAndSerializableOnReturnValues_good() throws Exception {
    tester
        .forAllPublicStaticMethods(GoodEqualsAndSerialiableFactory.class)
        .testEqualsAndSerializable();
  }

  public static class GoodEqualsAndSerialiableFactory {
    public static Object good(AnInterface s) {
      return Functions.constant(s);
    }
  }

  public void testEqualsForReturnValues_factoryReturnsNullButNotAnnotated() throws Exception {
    try {
      tester.forAllPublicStaticMethods(FactoryThatReturnsNullButNotAnnotated.class).testEquals();
    } catch (AssertionFailedError expected) {
      return;
    }
    fail();
  }

  public void testNullsForReturnValues_factoryReturnsNullButNotAnnotated() throws Exception {
    try {
      tester.forAllPublicStaticMethods(FactoryThatReturnsNullButNotAnnotated.class).testNulls();
    } catch (AssertionFailedError expected) {
      return;
    }
    fail();
  }

  public void testSerializableForReturnValues_factoryReturnsNullButNotAnnotated() throws Exception {
    try {
      tester
          .forAllPublicStaticMethods(FactoryThatReturnsNullButNotAnnotated.class)
          .testSerializable();
    } catch (AssertionFailedError expected) {
      return;
    }
    fail();
  }

  public void testEqualsAndSerializableForReturnValues_factoryReturnsNullButNotAnnotated()
      throws Exception {
    try {
      tester
          .forAllPublicStaticMethods(FactoryThatReturnsNullButNotAnnotated.class)
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
    tester.forAllPublicStaticMethods(FactoryThatReturnsNullAndAnnotated.class).testEquals();
  }

  public void testNullsForReturnValues_factoryReturnsNullAndAnnotated() throws Exception {
    tester.forAllPublicStaticMethods(FactoryThatReturnsNullAndAnnotated.class).testNulls();
  }

  public void testSerializableForReturnValues_factoryReturnsNullAndAnnotated() throws Exception {
    tester.forAllPublicStaticMethods(FactoryThatReturnsNullAndAnnotated.class).testSerializable();
  }

  public void testEqualsAndSerializableForReturnValues_factoryReturnsNullAndAnnotated()
      throws Exception {
    tester
        .forAllPublicStaticMethods(FactoryThatReturnsNullAndAnnotated.class)
        .testEqualsAndSerializable();
  }

  public static class FactoryThatReturnsNullAndAnnotated {
    @Nullable
    public static Object bad() {
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
      assertThat(expected.getMessage()).contains("create(null)");
      return;
    }
    fail("should have failed");
  }

  public void testBadEquals_withParameterizedType() throws Exception {
    try {
      tester.testEquals(BadEqualsWithParameterizedType.class);
    } catch (AssertionFailedError expected) {
      assertThat(expected.getMessage()).contains("create([[1]])");
      return;
    }
    fail("should have failed");
  }

  public void testBadEquals_withSingleParameterValue() throws Exception {
    try {
      tester.doTestEquals(ConstructorParameterWithOptionalNotInstantiable.class);
      fail();
    } catch (ParameterHasNoDistinctValueException expected) {
    }
  }

  public void testGoodReferentialEqualityComparison() throws Exception {
    tester.testEquals(UsesEnum.class);
    tester.testEquals(UsesReferentialEquality.class);
    tester.testEquals(SameListInstance.class);
  }

  public void testStreamParameterSkippedForNullTesting() throws Exception {
    tester.testNulls(WithStreamParameter.class);
  }

  @AndroidIncompatible // problem with equality of Type objects?
  public void testEqualsUsingReferentialEquality() throws Exception {
    assertBadUseOfReferentialEquality(SameIntegerInstance.class);
    assertBadUseOfReferentialEquality(SameLongInstance.class);
    assertBadUseOfReferentialEquality(SameFloatInstance.class);
    assertBadUseOfReferentialEquality(SameDoubleInstance.class);
    assertBadUseOfReferentialEquality(SameShortInstance.class);
    assertBadUseOfReferentialEquality(SameByteInstance.class);
    assertBadUseOfReferentialEquality(SameCharacterInstance.class);
    assertBadUseOfReferentialEquality(SameBooleanInstance.class);
    assertBadUseOfReferentialEquality(SameObjectInstance.class);
    assertBadUseOfReferentialEquality(SameStringInstance.class);
    assertBadUseOfReferentialEquality(SameInterfaceInstance.class);
  }

  private void assertBadUseOfReferentialEquality(Class<?> cls) throws Exception {
    try {
      tester.testEquals(cls);
    } catch (AssertionFailedError expected) {
      assertThat(expected.getMessage()).contains(cls.getSimpleName() + "(");
      return;
    }
    fail("should have failed for " + cls);
  }

  public void testParameterNotInstantiableForEqualsTest() throws Exception {
    try {
      tester.doTestEquals(ConstructorParameterNotInstantiable.class);
      fail("should have failed");
    } catch (ParameterNotInstantiableException expected) {
    }
  }

  public void testNoDistinctValueForEqualsTest() throws Exception {
    try {
      tester.doTestEquals(ConstructorParameterSingleValue.class);
      fail("should have failed");
    } catch (ParameterHasNoDistinctValueException expected) {
    }
  }

  public void testConstructorThrowsForEqualsTest() throws Exception {
    try {
      tester.doTestEquals(ConstructorThrows.class);
      fail("should have failed");
    } catch (InvocationTargetException expected) {
    }
  }

  public void testFactoryMethodReturnsNullForEqualsTest() throws Exception {
    try {
      tester.doTestEquals(FactoryMethodReturnsNullAndAnnotated.class);
      fail("should have failed");
    } catch (FactoryMethodReturnsNullException expected) {
    }
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
    tester.testEquals(MyAnnotation.class);
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

  public void testNulls_parameterOptionalNotInstantiable() throws Exception {
    tester.testNulls(ConstructorParameterWithOptionalNotInstantiable.class);
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
    tester.testNulls(MyAnnotation.class);
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
      assertThat(expected.getMessage()).contains("@Nullable");
      return;
    }
    fail("should have failed");
  }

  public void testInstantiate_factoryMethodReturnsNullAndAnnotated() throws Exception {
    try {
      tester.instantiate(FactoryMethodReturnsNullAndAnnotated.class);
      fail("should have failed");
    } catch (FactoryMethodReturnsNullException expected) {
    }
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
    assertNull(tester.instantiate(MyAnnotation.class));
  }

  public void testInstantiate_setDefault() throws Exception {
    NotInstantiable x = new NotInstantiable();
    tester.setDefault(NotInstantiable.class, x);
    assertNotNull(tester.instantiate(ConstructorParameterNotInstantiable.class));
  }

  public void testSetDistinctValues_equalInstances() {
    try {
      tester.setDistinctValues(String.class, "", "");
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }

  public void testInstantiate_setDistinctValues() throws Exception {
    NotInstantiable x = new NotInstantiable();
    NotInstantiable y = new NotInstantiable();
    tester.setDistinctValues(NotInstantiable.class, x, y);
    assertNotNull(tester.instantiate(ConstructorParameterNotInstantiable.class));
    tester.testEquals(ConstructorParameterMapOfNotInstantiable.class);
  }

  public void testInstantiate_constructorThrows() throws Exception {
    try {
      tester.instantiate(ConstructorThrows.class);
      fail();
    } catch (InvocationTargetException expected) {
    }
  }

  public void testInstantiate_factoryMethodThrows() throws Exception {
    try {
      tester.instantiate(FactoryMethodThrows.class);
      fail();
    } catch (InvocationTargetException expected) {
    }
  }

  public void testInstantiate_constructorParameterNotInstantiable() throws Exception {
    try {
      tester.instantiate(ConstructorParameterNotInstantiable.class);
      fail();
    } catch (ParameterNotInstantiableException expected) {
    }
  }

  public void testInstantiate_factoryMethodParameterNotInstantiable() throws Exception {
    try {
      tester.instantiate(FactoryMethodParameterNotInstantiable.class);
      fail();
    } catch (ParameterNotInstantiableException expected) {
    }
  }

  public void testInstantiate_instantiableFactoryMethodChosen() throws Exception {
    assertEquals("good", tester.instantiate(InstantiableFactoryMethodChosen.class).name);
  }

  @AndroidIncompatible // TODO(cpovirk): ClassNotFoundException... ClassSanityTesterTest$AnInterface
  public void testInterfaceProxySerializable() throws Exception {
    SerializableTester.reserializeAndAssert(tester.instantiate(HasAnInterface.class));
  }

  public void testReturnValuesFromAnotherPackageIgnoredForNullTests() throws Exception {
    new ClassSanityTester().forAllPublicStaticMethods(JdkObjectFactory.class).testNulls();
  }

  /** String doesn't check nulls as we expect. But the framework should ignore. */
  private static class JdkObjectFactory {
    @SuppressWarnings("unused") // Called by reflection
    public static Object create() {
      return new ArrayList<>();
    }
  }

  static class HasAnInterface implements Serializable {
    private final AnInterface i;

    public HasAnInterface(AnInterface i) {
      this.i = i;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
      if (obj instanceof HasAnInterface) {
        HasAnInterface that = (HasAnInterface) obj;
        return i.equals(that.i);
      } else {
        return false;
      }
    }

    @Override
    public int hashCode() {
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

  public void testEquals_setOfNonInstantiable() throws Exception {
    try {
      new ClassSanityTester().doTestEquals(SetWrapper.class);
      fail();
    } catch (ParameterNotInstantiableException expected) {
    }
  }

  private abstract static class Wrapper {
    private final Object wrapped;

    Wrapper(Object wrapped) {
      this.wrapped = checkNotNull(wrapped);
    }

    @Override
    public boolean equals(@Nullable Object obj) {
      // In general getClass().isInstance() is bad for equals.
      // But here we fully control the subclasses to ensure symmetry.
      if (getClass().isInstance(obj)) {
        Wrapper that = (Wrapper) obj;
        return wrapped.equals(that.wrapped);
      }
      return false;
    }

    @Override
    public int hashCode() {
      return wrapped.hashCode();
    }

    @Override
    public String toString() {
      return wrapped.toString();
    }
  }

  private static class SetWrapper extends Wrapper {
    public SetWrapper(Set<NotInstantiable> wrapped) {
      super(wrapped);
    }
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

    // Good!
    static GoodEquals create(String a, int b) {
      return new GoodEquals(a, b);
    }

    // keep trying
    @SuppressWarnings("unused")
    @Nullable
    public static GoodEquals createMayReturnNull(int a, int b) {
      return null;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
      if (obj instanceof GoodEquals) {
        GoodEquals that = (GoodEquals) obj;
        return a.equals(that.a) && b == that.b;
      } else {
        return false;
      }
    }

    @Override
    public int hashCode() {
      return 0;
    }
  }

  static class BadEquals {

    public BadEquals() {} // ignored by testEquals() since it has less parameters.

    public static BadEquals create(@SuppressWarnings("unused") @Nullable String s) {
      return new BadEquals();
    }

    @Override
    public boolean equals(@Nullable Object obj) {
      return obj instanceof BadEquals;
    }

    @Override
    public int hashCode() {
      return 0;
    }
  }

  static class SameIntegerInstance {
    private final Integer i;

    public SameIntegerInstance(Integer i) {
      this.i = checkNotNull(i);
    }

    @Override
    public int hashCode() {
      return i.hashCode();
    }

    @Override
    @SuppressWarnings("NumericEquality")
    public boolean equals(Object obj) {
      if (obj instanceof SameIntegerInstance) {
        SameIntegerInstance that = (SameIntegerInstance) obj;
        return i == that.i;
      }
      return false;
    }
  }

  static class SameLongInstance {
    private final Long i;

    public SameLongInstance(Long i) {
      this.i = checkNotNull(i);
    }

    @Override
    public int hashCode() {
      return i.hashCode();
    }

    @Override
    @SuppressWarnings("NumericEquality")
    public boolean equals(Object obj) {
      if (obj instanceof SameLongInstance) {
        SameLongInstance that = (SameLongInstance) obj;
        return i == that.i;
      }
      return false;
    }
  }

  static class SameFloatInstance {
    private final Float i;

    public SameFloatInstance(Float i) {
      this.i = checkNotNull(i);
    }

    @Override
    public int hashCode() {
      return i.hashCode();
    }

    @Override
    @SuppressWarnings("NumericEquality")
    public boolean equals(Object obj) {
      if (obj instanceof SameFloatInstance) {
        SameFloatInstance that = (SameFloatInstance) obj;
        return i == that.i;
      }
      return false;
    }
  }

  static class SameDoubleInstance {
    private final Double i;

    public SameDoubleInstance(Double i) {
      this.i = checkNotNull(i);
    }

    @Override
    public int hashCode() {
      return i.hashCode();
    }

    @Override
    @SuppressWarnings("NumericEquality")
    public boolean equals(Object obj) {
      if (obj instanceof SameDoubleInstance) {
        SameDoubleInstance that = (SameDoubleInstance) obj;
        return i == that.i;
      }
      return false;
    }
  }

  static class SameShortInstance {
    private final Short i;

    public SameShortInstance(Short i) {
      this.i = checkNotNull(i);
    }

    @Override
    public int hashCode() {
      return i.hashCode();
    }

    @Override
    @SuppressWarnings("NumericEquality")
    public boolean equals(Object obj) {
      if (obj instanceof SameShortInstance) {
        SameShortInstance that = (SameShortInstance) obj;
        return i == that.i;
      }
      return false;
    }
  }

  static class SameByteInstance {
    private final Byte i;

    public SameByteInstance(Byte i) {
      this.i = checkNotNull(i);
    }

    @Override
    public int hashCode() {
      return i.hashCode();
    }

    @Override
    @SuppressWarnings("NumericEquality")
    public boolean equals(Object obj) {
      if (obj instanceof SameByteInstance) {
        SameByteInstance that = (SameByteInstance) obj;
        return i == that.i;
      }
      return false;
    }
  }

  static class SameCharacterInstance {
    private final Character i;

    public SameCharacterInstance(Character i) {
      this.i = checkNotNull(i);
    }

    @Override
    public int hashCode() {
      return i.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
      if (obj instanceof SameCharacterInstance) {
        SameCharacterInstance that = (SameCharacterInstance) obj;
        return i == that.i;
      }
      return false;
    }
  }

  static class SameBooleanInstance {
    private final Boolean i;

    public SameBooleanInstance(Boolean i) {
      this.i = checkNotNull(i);
    }

    @Override
    public int hashCode() {
      return i.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
      if (obj instanceof SameBooleanInstance) {
        SameBooleanInstance that = (SameBooleanInstance) obj;
        return i == that.i;
      }
      return false;
    }
  }

  static class SameStringInstance {
    private final String s;

    public SameStringInstance(String s) {
      this.s = checkNotNull(s);
    }

    @Override
    public int hashCode() {
      return s.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
      if (obj instanceof SameStringInstance) {
        SameStringInstance that = (SameStringInstance) obj;
        return s == that.s;
      }
      return false;
    }
  }

  static class SameObjectInstance {
    private final Object s;

    public SameObjectInstance(Object s) {
      this.s = checkNotNull(s);
    }

    @Override
    public int hashCode() {
      return s.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
      if (obj instanceof SameObjectInstance) {
        SameObjectInstance that = (SameObjectInstance) obj;
        return s == that.s;
      }
      return false;
    }
  }

  static class SameInterfaceInstance {
    private final Runnable s;

    public SameInterfaceInstance(Runnable s) {
      this.s = checkNotNull(s);
    }

    @Override
    public int hashCode() {
      return s.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
      if (obj instanceof SameInterfaceInstance) {
        SameInterfaceInstance that = (SameInterfaceInstance) obj;
        return s == that.s;
      }
      return false;
    }
  }

  static class SameListInstance {
    private final List<?> s;

    public SameListInstance(List<?> s) {
      this.s = checkNotNull(s);
    }

    @Override
    public int hashCode() {
      return System.identityHashCode(s);
    }

    @Override
    public boolean equals(Object obj) {
      if (obj instanceof SameListInstance) {
        SameListInstance that = (SameListInstance) obj;
        return s == that.s;
      }
      return false;
    }
  }

  static class WithStreamParameter {
    private final List<?> list;

    // This should be ignored.
    public WithStreamParameter(Stream<?> s, String str) {
      this.list = s.collect(Collectors.toList());
      checkNotNull(str);
    }
  }

  static class UsesReferentialEquality {
    private final ReferentialEquality s;

    public UsesReferentialEquality(ReferentialEquality s) {
      this.s = checkNotNull(s);
    }

    @Override
    public int hashCode() {
      return s.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
      if (obj instanceof UsesReferentialEquality) {
        UsesReferentialEquality that = (UsesReferentialEquality) obj;
        return s == that.s;
      }
      return false;
    }
  }

  static class UsesEnum {
    private final TimeUnit s;

    public UsesEnum(TimeUnit s) {
      this.s = checkNotNull(s);
    }

    @Override
    public int hashCode() {
      return s.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
      if (obj instanceof UsesEnum) {
        UsesEnum that = (UsesEnum) obj;
        return s == that.s;
      }
      return false;
    }
  }

  public static class ReferentialEquality {
    public ReferentialEquality() {}
  }

  static class BadEqualsWithParameterizedType {

    // ignored by testEquals() since it has less parameters.
    public BadEqualsWithParameterizedType() {}

    public static BadEqualsWithParameterizedType create(
        @SuppressWarnings("unused") ImmutableList<Iterable<? extends String>> s) {
      return new BadEqualsWithParameterizedType();
    }

    @Override
    public boolean equals(@Nullable Object obj) {
      return obj instanceof BadEqualsWithParameterizedType;
    }

    @Override
    public int hashCode() {
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

    @SuppressWarnings("unused") // reflected
    void nullableOnly(@Nullable String s) {}

    public void noParameter() {}

    @SuppressWarnings("unused") // reflected
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

    @Nullable
    public static FactoryMethodReturnsNullAndAnnotated returnsNull() {
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

  static class ConstructorParameterMapOfNotInstantiable {
    private final Map<NotInstantiable, NotInstantiable> m;

    public ConstructorParameterMapOfNotInstantiable(Map<NotInstantiable, NotInstantiable> m) {
      this.m = checkNotNull(m);
    }

    @Override
    public boolean equals(@Nullable Object obj) {
      if (obj instanceof ConstructorParameterMapOfNotInstantiable) {
        return m.equals(((ConstructorParameterMapOfNotInstantiable) obj).m);
      } else {
        return false;
      }
    }

    @Override
    public int hashCode() {
      return m.hashCode();
    }
  }

  // Test that we should get a distinct parameter error when doing equals test.
  static class ConstructorParameterWithOptionalNotInstantiable {
    public ConstructorParameterWithOptionalNotInstantiable(Optional<NotInstantiable> x) {
      checkNotNull(x);
    }

    @Override
    public boolean equals(@Nullable Object obj) {
      throw new UnsupportedOperationException();
    }

    @Override
    public int hashCode() {
      throw new UnsupportedOperationException();
    }
  }

  static class ConstructorParameterSingleValue {
    public ConstructorParameterSingleValue(@SuppressWarnings("unused") Singleton s) {}

    @Override
    public boolean equals(Object obj) {
      return obj instanceof ConstructorParameterSingleValue;
    }

    @Override
    public int hashCode() {
      return 1;
    }

    public static class Singleton {
      public static final Singleton INSTANCE = new Singleton();

      private Singleton() {}
    }
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

  private abstract static class AnAbstractClass {
    @SuppressWarnings("unused")
    public AnAbstractClass(String s) {}

    @SuppressWarnings("unused")
    public void failsToCheckNull(String s) {}
  }

  private static class NoPublicStaticMethods {
    @SuppressWarnings("unused") // To test non-public factory isn't used.
    static String notPublic() {
      return "";
    }
  }

  @interface MyAnnotation {}
}
