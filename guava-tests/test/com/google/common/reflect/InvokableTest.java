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

package com.google.common.reflect;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.testing.EqualsTester;
import com.google.common.testing.NullPointerTester;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.TypeVariable;
import java.util.Collections;
import junit.framework.TestCase;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;

/**
 * Unit tests for {@link Invokable}.
 *
 * @author Ben Yu
 */
@AndroidIncompatible // lots of failures, possibly some related to bad equals() implementations?
public class InvokableTest extends TestCase {

  public void testConstructor_returnType() throws Exception {
    assertEquals(Prepender.class, Prepender.constructor().getReturnType().getType());
  }

  private static class WithConstructorAndTypeParameter<T> {
    @SuppressWarnings("unused") // by reflection
    <X> WithConstructorAndTypeParameter() {}
  }

  public void testConstructor_returnType_hasTypeParameter() throws Exception {
    @SuppressWarnings("rawtypes") // Foo.class for Foo<T> is always raw type
    Class<WithConstructorAndTypeParameter> type = WithConstructorAndTypeParameter.class;
    @SuppressWarnings("rawtypes") // Foo.class
    Constructor<WithConstructorAndTypeParameter> constructor = type.getDeclaredConstructor();
    Invokable<?, ?> factory = Invokable.from(constructor);
    assertThat(factory.getTypeParameters()).hasLength(2);
    assertEquals(type.getTypeParameters()[0], factory.getTypeParameters()[0]);
    assertEquals(constructor.getTypeParameters()[0], factory.getTypeParameters()[1]);
    ParameterizedType returnType = (ParameterizedType) factory.getReturnType().getType();
    assertEquals(type, returnType.getRawType());
    assertEquals(
        ImmutableList.copyOf(type.getTypeParameters()),
        ImmutableList.copyOf(returnType.getActualTypeArguments()));
  }

  public void testConstructor_exceptionTypes() throws Exception {
    assertEquals(
        ImmutableList.of(TypeToken.of(NullPointerException.class)),
        Prepender.constructor(String.class, int.class).getExceptionTypes());
  }

  public void testConstructor_typeParameters() throws Exception {
    TypeVariable<?>[] variables = Prepender.constructor().getTypeParameters();
    assertThat(variables).hasLength(1);
    assertEquals("A", variables[0].getName());
  }

  public void testConstructor_parameters() throws Exception {
    Invokable<?, Prepender> delegate = Prepender.constructor(String.class, int.class);
    ImmutableList<Parameter> parameters = delegate.getParameters();
    assertEquals(2, parameters.size());
    assertEquals(String.class, parameters.get(0).getType().getType());
    assertTrue(parameters.get(0).isAnnotationPresent(NotBlank.class));
    assertEquals(int.class, parameters.get(1).getType().getType());
    assertFalse(parameters.get(1).isAnnotationPresent(NotBlank.class));
    new EqualsTester()
        .addEqualityGroup(parameters.get(0))
        .addEqualityGroup(parameters.get(1))
        .testEquals();
  }

  public void testConstructor_call() throws Exception {
    Invokable<?, Prepender> delegate = Prepender.constructor(String.class, int.class);
    Prepender prepender = delegate.invoke(null, "a", 1);
    assertEquals("a", prepender.prefix);
    assertEquals(1, prepender.times);
  }

  public void testConstructor_returning() throws Exception {
    Invokable<?, Prepender> delegate =
        Prepender.constructor(String.class, int.class).returning(Prepender.class);
    Prepender prepender = delegate.invoke(null, "a", 1);
    assertEquals("a", prepender.prefix);
    assertEquals(1, prepender.times);
  }

  public void testConstructor_invalidReturning() throws Exception {
    Invokable<?, Prepender> delegate = Prepender.constructor(String.class, int.class);
    try {
      delegate.returning(SubPrepender.class);
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }

  public void testStaticMethod_returnType() throws Exception {
    Invokable<?, ?> delegate = Prepender.method("prepend", String.class, Iterable.class);
    assertEquals(new TypeToken<Iterable<String>>() {}, delegate.getReturnType());
  }

  public void testStaticMethod_exceptionTypes() throws Exception {
    Invokable<?, ?> delegate = Prepender.method("prepend", String.class, Iterable.class);
    assertEquals(ImmutableList.of(), delegate.getExceptionTypes());
  }

  public void testStaticMethod_typeParameters() throws Exception {
    Invokable<?, ?> delegate = Prepender.method("prepend", String.class, Iterable.class);
    TypeVariable<?>[] variables = delegate.getTypeParameters();
    assertThat(variables).hasLength(1);
    assertEquals("T", variables[0].getName());
  }

  public void testStaticMethod_parameters() throws Exception {
    Invokable<?, ?> delegate = Prepender.method("prepend", String.class, Iterable.class);
    ImmutableList<Parameter> parameters = delegate.getParameters();
    assertEquals(2, parameters.size());
    assertEquals(String.class, parameters.get(0).getType().getType());
    assertTrue(parameters.get(0).isAnnotationPresent(NotBlank.class));
    assertEquals(new TypeToken<Iterable<String>>() {}, parameters.get(1).getType());
    assertFalse(parameters.get(1).isAnnotationPresent(NotBlank.class));
    new EqualsTester()
        .addEqualityGroup(parameters.get(0))
        .addEqualityGroup(parameters.get(1))
        .testEquals();
  }

  public void testStaticMethod_call() throws Exception {
    Invokable<?, ?> delegate = Prepender.method("prepend", String.class, Iterable.class);
    @SuppressWarnings("unchecked") // prepend() returns Iterable<String>
    Iterable<String> result =
        (Iterable<String>) delegate.invoke(null, "a", ImmutableList.of("b", "c"));
    assertEquals(ImmutableList.of("a", "b", "c"), ImmutableList.copyOf(result));
  }

  public void testStaticMethod_returning() throws Exception {
    Invokable<?, Iterable<String>> delegate =
        Prepender.method("prepend", String.class, Iterable.class)
            .returning(new TypeToken<Iterable<String>>() {});
    assertEquals(new TypeToken<Iterable<String>>() {}, delegate.getReturnType());
    Iterable<String> result = delegate.invoke(null, "a", ImmutableList.of("b", "c"));
    assertEquals(ImmutableList.of("a", "b", "c"), ImmutableList.copyOf(result));
  }

  public void testStaticMethod_returningRawType() throws Exception {
    @SuppressWarnings("rawtypes") // the purpose is to test raw type
    Invokable<?, Iterable> delegate =
        Prepender.method("prepend", String.class, Iterable.class).returning(Iterable.class);
    assertEquals(new TypeToken<Iterable<String>>() {}, delegate.getReturnType());
    @SuppressWarnings("unchecked") // prepend() returns Iterable<String>
    Iterable<String> result = delegate.invoke(null, "a", ImmutableList.of("b", "c"));
    assertEquals(ImmutableList.of("a", "b", "c"), ImmutableList.copyOf(result));
  }

  public void testStaticMethod_invalidReturning() throws Exception {
    Invokable<?, Object> delegate = Prepender.method("prepend", String.class, Iterable.class);
    try {
      delegate.returning(new TypeToken<Iterable<Integer>>() {});
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }

  public void testInstanceMethod_returnType() throws Exception {
    Invokable<?, ?> delegate = Prepender.method("prepend", Iterable.class);
    assertEquals(new TypeToken<Iterable<String>>() {}, delegate.getReturnType());
  }

  public void testInstanceMethod_exceptionTypes() throws Exception {
    Invokable<?, ?> delegate = Prepender.method("prepend", Iterable.class);
    assertEquals(
        ImmutableList.of(
            TypeToken.of(IllegalArgumentException.class), TypeToken.of(NullPointerException.class)),
        delegate.getExceptionTypes());
  }

  public void testInstanceMethod_typeParameters() throws Exception {
    Invokable<?, ?> delegate = Prepender.method("prepend", Iterable.class);
    assertThat(delegate.getTypeParameters()).isEmpty();
  }

  public void testInstanceMethod_parameters() throws Exception {
    Invokable<?, ?> delegate = Prepender.method("prepend", Iterable.class);
    ImmutableList<Parameter> parameters = delegate.getParameters();
    assertEquals(1, parameters.size());
    assertEquals(new TypeToken<Iterable<String>>() {}, parameters.get(0).getType());
    assertThat(parameters.get(0).getAnnotations()).isEmpty();
    new EqualsTester().addEqualityGroup(parameters.get(0)).testEquals();
  }

  public void testInstanceMethod_call() throws Exception {
    Invokable<Prepender, ?> delegate = Prepender.method("prepend", Iterable.class);
    @SuppressWarnings("unchecked") // prepend() returns Iterable<String>
    Iterable<String> result =
        (Iterable<String>) delegate.invoke(new Prepender("a", 2), ImmutableList.of("b", "c"));
    assertEquals(ImmutableList.of("a", "a", "b", "c"), ImmutableList.copyOf(result));
  }

  public void testInstanceMethod_returning() throws Exception {
    Invokable<Prepender, Iterable<String>> delegate =
        Prepender.method("prepend", Iterable.class).returning(new TypeToken<Iterable<String>>() {});
    assertEquals(new TypeToken<Iterable<String>>() {}, delegate.getReturnType());
    Iterable<String> result = delegate.invoke(new Prepender("a", 2), ImmutableList.of("b", "c"));
    assertEquals(ImmutableList.of("a", "a", "b", "c"), ImmutableList.copyOf(result));
  }

  public void testInstanceMethod_returningRawType() throws Exception {
    @SuppressWarnings("rawtypes") // the purpose is to test raw type
    Invokable<Prepender, Iterable> delegate =
        Prepender.method("prepend", Iterable.class).returning(Iterable.class);
    assertEquals(new TypeToken<Iterable<String>>() {}, delegate.getReturnType());
    @SuppressWarnings("unchecked") // prepend() returns Iterable<String>
    Iterable<String> result = delegate.invoke(new Prepender("a", 2), ImmutableList.of("b", "c"));
    assertEquals(ImmutableList.of("a", "a", "b", "c"), ImmutableList.copyOf(result));
  }

  public void testInstanceMethod_invalidReturning() throws Exception {
    Invokable<?, Object> delegate = Prepender.method("prepend", Iterable.class);
    try {
      delegate.returning(new TypeToken<Iterable<Integer>>() {});
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }

  public void testPrivateInstanceMethod_isOverridable() throws Exception {
    Invokable<?, ?> delegate = Prepender.method("privateMethod");
    assertTrue(delegate.isPrivate());
    assertFalse(delegate.isOverridable());
    assertFalse(delegate.isVarArgs());
  }

  public void testPrivateFinalInstanceMethod_isOverridable() throws Exception {
    Invokable<?, ?> delegate = Prepender.method("privateFinalMethod");
    assertTrue(delegate.isPrivate());
    assertTrue(delegate.isFinal());
    assertFalse(delegate.isOverridable());
    assertFalse(delegate.isVarArgs());
  }

  public void testStaticMethod_isOverridable() throws Exception {
    Invokable<?, ?> delegate = Prepender.method("staticMethod");
    assertTrue(delegate.isStatic());
    assertFalse(delegate.isOverridable());
    assertFalse(delegate.isVarArgs());
  }

  public void testStaticFinalMethod_isFinal() throws Exception {
    Invokable<?, ?> delegate = Prepender.method("staticFinalMethod");
    assertTrue(delegate.isStatic());
    assertTrue(delegate.isFinal());
    assertFalse(delegate.isOverridable());
    assertFalse(delegate.isVarArgs());
  }

  static class Foo {}

  public void testConstructor_isOverridablel() throws Exception {
    Invokable<?, ?> delegate = Invokable.from(Foo.class.getDeclaredConstructor());
    assertFalse(delegate.isOverridable());
    assertFalse(delegate.isVarArgs());
  }

  public void testMethod_isVarArgs() throws Exception {
    Invokable<?, ?> delegate = Prepender.method("privateVarArgsMethod", String[].class);
    assertTrue(delegate.isVarArgs());
  }

  public void testConstructor_isVarArgs() throws Exception {
    Invokable<?, ?> delegate = Prepender.constructor(String[].class);
    assertTrue(delegate.isVarArgs());
  }

  public void testGetOwnerType_constructor() throws Exception {
    Invokable<String, String> invokable = Invokable.from(String.class.getConstructor());
    assertEquals(TypeToken.of(String.class), invokable.getOwnerType());
  }

  public void testGetOwnerType_method() throws Exception {
    Invokable<?, ?> invokable = Invokable.from(String.class.getMethod("length"));
    assertEquals(TypeToken.of(String.class), invokable.getOwnerType());
  }

  private static final class FinalClass {
    @SuppressWarnings("unused") // used by reflection
    void notFinalMethod() {}
  }

  public void testNonFinalMethodInFinalClass_isOverridable() throws Exception {
    Invokable<?, ?> delegate = Invokable.from(FinalClass.class.getDeclaredMethod("notFinalMethod"));
    assertFalse(delegate.isOverridable());
    assertFalse(delegate.isVarArgs());
  }

  private class InnerWithDefaultConstructor {
    class NestedInner {}
  }

  public void testInnerClassDefaultConstructor() {
    Constructor<?> constructor = InnerWithDefaultConstructor.class.getDeclaredConstructors()[0];
    assertEquals(0, Invokable.from(constructor).getParameters().size());
  }

  public void testNestedInnerClassDefaultConstructor() {
    Constructor<?> constructor =
        InnerWithDefaultConstructor.NestedInner.class.getDeclaredConstructors()[0];
    assertEquals(0, Invokable.from(constructor).getParameters().size());
  }

  private class InnerWithOneParameterConstructor {
    @SuppressWarnings("unused") // called by reflection
    public InnerWithOneParameterConstructor(String s) {}
  }

  public void testInnerClassWithOneParameterConstructor() {
    Constructor<?> constructor =
        InnerWithOneParameterConstructor.class.getDeclaredConstructors()[0];
    Invokable<?, ?> invokable = Invokable.from(constructor);
    assertEquals(1, invokable.getParameters().size());
    assertEquals(TypeToken.of(String.class), invokable.getParameters().get(0).getType());
  }

  private class InnerWithAnnotatedConstructorParameter {
    @SuppressWarnings("unused") // called by reflection
    InnerWithAnnotatedConstructorParameter(@NullableDecl String s) {}
  }

  public void testInnerClassWithAnnotatedConstructorParameter() {
    Constructor<?> constructor =
        InnerWithAnnotatedConstructorParameter.class.getDeclaredConstructors()[0];
    Invokable<?, ?> invokable = Invokable.from(constructor);
    assertEquals(1, invokable.getParameters().size());
    assertEquals(TypeToken.of(String.class), invokable.getParameters().get(0).getType());
  }

  private class InnerWithGenericConstructorParameter {
    @SuppressWarnings("unused") // called by reflection
    InnerWithGenericConstructorParameter(Iterable<String> it, String s) {}
  }

  public void testInnerClassWithGenericConstructorParameter() {
    Constructor<?> constructor =
        InnerWithGenericConstructorParameter.class.getDeclaredConstructors()[0];
    Invokable<?, ?> invokable = Invokable.from(constructor);
    assertEquals(2, invokable.getParameters().size());
    assertEquals(new TypeToken<Iterable<String>>() {}, invokable.getParameters().get(0).getType());
    assertEquals(TypeToken.of(String.class), invokable.getParameters().get(1).getType());
  }

  public void testAnonymousClassDefaultConstructor() {
    final int i = 1;
    final String s = "hello world";
    Class<?> anonymous =
        new Runnable() {
          @Override
          public void run() {
            System.out.println(s + i);
          }
        }.getClass();
    Constructor<?> constructor = anonymous.getDeclaredConstructors()[0];
    assertEquals(0, Invokable.from(constructor).getParameters().size());
  }

  public void testAnonymousClassWithTwoParametersConstructor() {
    abstract class Base {
      @SuppressWarnings("unused") // called by reflection
      Base(String s, int i) {}
    }
    Class<?> anonymous = new Base("test", 0) {}.getClass();
    Constructor<?> constructor = anonymous.getDeclaredConstructors()[0];
    assertEquals(2, Invokable.from(constructor).getParameters().size());
  }

  public void testLocalClassDefaultConstructor() {
    final int i = 1;
    final String s = "hello world";
    class LocalWithDefaultConstructor implements Runnable {
      @Override
      public void run() {
        System.out.println(s + i);
      }
    }
    Constructor<?> constructor = LocalWithDefaultConstructor.class.getDeclaredConstructors()[0];
    assertEquals(0, Invokable.from(constructor).getParameters().size());
  }

  public void testStaticAnonymousClassDefaultConstructor() throws Exception {
    doTestStaticAnonymousClassDefaultConstructor();
  }

  private static void doTestStaticAnonymousClassDefaultConstructor() {
    final int i = 1;
    final String s = "hello world";
    Class<?> anonymous =
        new Runnable() {
          @Override
          public void run() {
            System.out.println(s + i);
          }
        }.getClass();
    Constructor<?> constructor = anonymous.getDeclaredConstructors()[0];
    assertEquals(0, Invokable.from(constructor).getParameters().size());
  }

  public void testAnonymousClassInConstructor() {
    new AnonymousClassInConstructor();
  }

  private static class AnonymousClassInConstructor {
    AnonymousClassInConstructor() {
      final int i = 1;
      final String s = "hello world";
      Class<?> anonymous =
          new Runnable() {
            @Override
            public void run() {
              System.out.println(s + i);
            }
          }.getClass();
      Constructor<?> constructor = anonymous.getDeclaredConstructors()[0];
      assertEquals(0, Invokable.from(constructor).getParameters().size());
    }
  }

  public void testLocalClassInInstanceInitializer() {
    new LocalClassInInstanceInitializer();
  }

  private static class LocalClassInInstanceInitializer {
    {
      class Local {}
      Constructor<?> constructor = Local.class.getDeclaredConstructors()[0];
      assertEquals(0, Invokable.from(constructor).getParameters().size());
    }
  }

  public void testLocalClassInStaticInitializer() {
    new LocalClassInStaticInitializer();
  }

  private static class LocalClassInStaticInitializer {
    static {
      class Local {}
      Constructor<?> constructor = Local.class.getDeclaredConstructors()[0];
      assertEquals(0, Invokable.from(constructor).getParameters().size());
    }
  }

  public void testLocalClassWithSeeminglyHiddenThisInStaticInitializer_BUG() {
    new LocalClassWithSeeminglyHiddenThisInStaticInitializer();
  }

  /**
   * This class demonstrates a bug in getParameters() when the local class is inside static
   * initializer.
   */
  private static class LocalClassWithSeeminglyHiddenThisInStaticInitializer {
    static {
      class Local {
        @SuppressWarnings("unused") // through reflection
        Local(LocalClassWithSeeminglyHiddenThisInStaticInitializer outer) {}
      }
      Constructor<?> constructor = Local.class.getDeclaredConstructors()[0];
      int miscalculated = 0;
      assertEquals(miscalculated, Invokable.from(constructor).getParameters().size());
    }
  }

  public void testLocalClassWithOneParameterConstructor() throws Exception {
    final int i = 1;
    final String s = "hello world";
    class LocalWithOneParameterConstructor {
      @SuppressWarnings("unused") // called by reflection
      public LocalWithOneParameterConstructor(String x) {
        System.out.println(s + i);
      }
    }
    Constructor<?> constructor =
        LocalWithOneParameterConstructor.class.getDeclaredConstructors()[0];
    Invokable<?, ?> invokable = Invokable.from(constructor);
    assertEquals(1, invokable.getParameters().size());
    assertEquals(TypeToken.of(String.class), invokable.getParameters().get(0).getType());
  }

  public void testLocalClassWithAnnotatedConstructorParameter() throws Exception {
    class LocalWithAnnotatedConstructorParameter {
      @SuppressWarnings("unused") // called by reflection
      LocalWithAnnotatedConstructorParameter(@NullableDecl String s) {}
    }
    Constructor<?> constructor =
        LocalWithAnnotatedConstructorParameter.class.getDeclaredConstructors()[0];
    Invokable<?, ?> invokable = Invokable.from(constructor);
    assertEquals(1, invokable.getParameters().size());
    assertEquals(TypeToken.of(String.class), invokable.getParameters().get(0).getType());
  }

  public void testLocalClassWithGenericConstructorParameter() throws Exception {
    class LocalWithGenericConstructorParameter {
      @SuppressWarnings("unused") // called by reflection
      LocalWithGenericConstructorParameter(Iterable<String> it, String s) {}
    }
    Constructor<?> constructor =
        LocalWithGenericConstructorParameter.class.getDeclaredConstructors()[0];
    Invokable<?, ?> invokable = Invokable.from(constructor);
    assertEquals(2, invokable.getParameters().size());
    assertEquals(new TypeToken<Iterable<String>>() {}, invokable.getParameters().get(0).getType());
    assertEquals(TypeToken.of(String.class), invokable.getParameters().get(1).getType());
  }

  public void testEquals() throws Exception {
    new EqualsTester()
        .addEqualityGroup(Prepender.constructor(), Prepender.constructor())
        .addEqualityGroup(Prepender.constructor(String.class, int.class))
        .addEqualityGroup(Prepender.method("privateMethod"), Prepender.method("privateMethod"))
        .addEqualityGroup(Prepender.method("privateFinalMethod"))
        .testEquals();
  }

  public void testNulls() {
    new NullPointerTester().testAllPublicStaticMethods(Invokable.class);
    new NullPointerTester().testAllPublicInstanceMethods(Prepender.method("staticMethod"));
  }

  @Retention(RetentionPolicy.RUNTIME)
  private @interface NotBlank {}

  /** Class for testing constructor, static method and instance method. */
  @SuppressWarnings("unused") // most are called by reflection
  private static class Prepender {

    private final String prefix;
    private final int times;

    Prepender(@NotBlank String prefix, int times) throws NullPointerException {
      this.prefix = prefix;
      this.times = times;
    }

    Prepender(String... varargs) {
      this(null, 0);
    }

    // just for testing
    private <A> Prepender() {
      this(null, 0);
    }

    static <T> Iterable<String> prepend(@NotBlank String first, Iterable<String> tail) {
      return Iterables.concat(ImmutableList.of(first), tail);
    }

    Iterable<String> prepend(Iterable<String> tail)
        throws IllegalArgumentException, NullPointerException {
      return Iterables.concat(Collections.nCopies(times, prefix), tail);
    }

    static Invokable<?, Prepender> constructor(Class<?>... parameterTypes) throws Exception {
      Constructor<Prepender> constructor = Prepender.class.getDeclaredConstructor(parameterTypes);
      return Invokable.from(constructor);
    }

    static Invokable<Prepender, Object> method(String name, Class<?>... parameterTypes) {
      try {
        Method method = Prepender.class.getDeclaredMethod(name, parameterTypes);
        @SuppressWarnings("unchecked") // The method is from Prepender.
        Invokable<Prepender, Object> invokable =
            (Invokable<Prepender, Object>) Invokable.from(method);
        return invokable;
      } catch (NoSuchMethodException e) {
        throw new IllegalArgumentException(e);
      }
    }

    private void privateMethod() {}

    private final void privateFinalMethod() {}

    static void staticMethod() {}

    static final void staticFinalMethod() {}

    private void privateVarArgsMethod(String... varargs) {}
  }

  private static class SubPrepender extends Prepender {
    @SuppressWarnings("unused") // needed to satisfy compiler, never called
    public SubPrepender() throws NullPointerException {
      throw new AssertionError();
    }
  }
}
