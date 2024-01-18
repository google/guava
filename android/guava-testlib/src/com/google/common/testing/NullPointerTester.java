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

import com.google.common.annotations.GwtIncompatible;
import com.google.common.annotations.J2ktIncompatible;
import com.google.common.base.Converter;
import com.google.common.base.Objects;
import com.google.common.collect.ClassToInstanceMap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.MutableClassToInstanceMap;
import com.google.common.reflect.Invokable;
import com.google.common.reflect.Parameter;
import com.google.common.reflect.Reflection;
import com.google.common.reflect.TypeToken;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentMap;
import junit.framework.Assert;
import junit.framework.AssertionFailedError;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * A test utility that verifies that your methods and constructors throw {@link
 * NullPointerException} or {@link UnsupportedOperationException} whenever null is passed to a
 * parameter whose declaration or type isn't annotated with an annotation with the simple name
 * {@code Nullable}, {@code CheckForNull}, {@code NullableType}, or {@code NullableDecl}.
 *
 * <p>The tested methods and constructors are invoked -- each time with one parameter being null and
 * the rest not null -- and the test fails if no expected exception is thrown. {@code
 * NullPointerTester} uses best effort to pick non-null default values for many common JDK and Guava
 * types, and also for interfaces and public classes that have public parameter-less constructors.
 * When the non-null default value for a particular parameter type cannot be provided by {@code
 * NullPointerTester}, the caller can provide a custom non-null default value for the parameter type
 * via {@link #setDefault}.
 *
 * @author Kevin Bourrillion
 * @since 10.0
 */
@GwtIncompatible
@J2ktIncompatible
@ElementTypesAreNonnullByDefault
public final class NullPointerTester {

  private final ClassToInstanceMap<Object> defaults = MutableClassToInstanceMap.create();
  private final List<Member> ignoredMembers = Lists.newArrayList();

  private ExceptionTypePolicy policy = ExceptionTypePolicy.NPE_OR_UOE;

  public NullPointerTester() {
    try {
      /*
       * Converter.apply has a non-nullable parameter type but doesn't throw for null arguments. For
       * more information, see the comments in that class.
       *
       * We already know that that's how it behaves, and subclasses of Converter can't change that
       * behavior. So there's no sense in making all subclass authors exclude the method from any
       * NullPointerTester tests that they have.
       */
      ignoredMembers.add(Converter.class.getMethod("apply", Object.class));
    } catch (NoSuchMethodException shouldBeImpossible) {
      // OK, fine: If it doesn't exist, then there's chance that we're going to be asked to test it.
    }
  }

  /**
   * Sets a default value that can be used for any parameter of type {@code type}. Returns this
   * object.
   */
  @CanIgnoreReturnValue
  public <T> NullPointerTester setDefault(Class<T> type, T value) {
    defaults.putInstance(type, checkNotNull(value));
    return this;
  }

  /**
   * Ignore {@code method} in the tests that follow. Returns this object.
   *
   * @since 13.0
   */
  @CanIgnoreReturnValue
  public NullPointerTester ignore(Method method) {
    ignoredMembers.add(checkNotNull(method));
    return this;
  }

  /**
   * Ignore {@code constructor} in the tests that follow. Returns this object.
   *
   * @since 22.0
   */
  @CanIgnoreReturnValue
  public NullPointerTester ignore(Constructor<?> constructor) {
    ignoredMembers.add(checkNotNull(constructor));
    return this;
  }

  /**
   * Runs {@link #testConstructor} on every constructor in class {@code c} that has at least {@code
   * minimalVisibility}.
   */
  public void testConstructors(Class<?> c, Visibility minimalVisibility) {
    for (Constructor<?> constructor : c.getDeclaredConstructors()) {
      if (minimalVisibility.isVisible(constructor) && !isIgnored(constructor)) {
        testConstructor(constructor);
      }
    }
  }

  /** Runs {@link #testConstructor} on every public constructor in class {@code c}. */
  public void testAllPublicConstructors(Class<?> c) {
    testConstructors(c, Visibility.PUBLIC);
  }

  /**
   * Runs {@link #testMethod} on every static method of class {@code c} that has at least {@code
   * minimalVisibility}, including those "inherited" from superclasses of the same package.
   */
  public void testStaticMethods(Class<?> c, Visibility minimalVisibility) {
    for (Method method : minimalVisibility.getStaticMethods(c)) {
      if (!isIgnored(method)) {
        testMethod(null, method);
      }
    }
  }

  /**
   * Runs {@link #testMethod} on every public static method of class {@code c}, including those
   * "inherited" from superclasses of the same package.
   */
  public void testAllPublicStaticMethods(Class<?> c) {
    testStaticMethods(c, Visibility.PUBLIC);
  }

  /**
   * Runs {@link #testMethod} on every instance method of the class of {@code instance} with at
   * least {@code minimalVisibility}, including those inherited from superclasses of the same
   * package.
   */
  public void testInstanceMethods(Object instance, Visibility minimalVisibility) {
    for (Method method : getInstanceMethodsToTest(instance.getClass(), minimalVisibility)) {
      testMethod(instance, method);
    }
  }

  ImmutableList<Method> getInstanceMethodsToTest(Class<?> c, Visibility minimalVisibility) {
    ImmutableList.Builder<Method> builder = ImmutableList.builder();
    for (Method method : minimalVisibility.getInstanceMethods(c)) {
      if (!isIgnored(method)) {
        builder.add(method);
      }
    }
    return builder.build();
  }

  /**
   * Runs {@link #testMethod} on every public instance method of the class of {@code instance},
   * including those inherited from superclasses of the same package.
   */
  public void testAllPublicInstanceMethods(Object instance) {
    testInstanceMethods(instance, Visibility.PUBLIC);
  }

  /**
   * Verifies that {@code method} produces a {@link NullPointerException} or {@link
   * UnsupportedOperationException} whenever <i>any</i> of its non-nullable parameters are null.
   *
   * @param instance the instance to invoke {@code method} on, or null if {@code method} is static
   */
  public void testMethod(@Nullable Object instance, Method method) {
    Class<?>[] types = method.getParameterTypes();
    for (int nullIndex = 0; nullIndex < types.length; nullIndex++) {
      testMethodParameter(instance, method, nullIndex);
    }
  }

  /**
   * Verifies that {@code ctor} produces a {@link NullPointerException} or {@link
   * UnsupportedOperationException} whenever <i>any</i> of its non-nullable parameters are null.
   */
  public void testConstructor(Constructor<?> ctor) {
    Class<?> declaringClass = ctor.getDeclaringClass();
    checkArgument(
        Modifier.isStatic(declaringClass.getModifiers())
            || declaringClass.getEnclosingClass() == null,
        "Cannot test constructor of non-static inner class: %s",
        declaringClass.getName());
    Class<?>[] types = ctor.getParameterTypes();
    for (int nullIndex = 0; nullIndex < types.length; nullIndex++) {
      testConstructorParameter(ctor, nullIndex);
    }
  }

  /**
   * Verifies that {@code method} produces a {@link NullPointerException} or {@link
   * UnsupportedOperationException} when the parameter in position {@code paramIndex} is null. If
   * this parameter is marked nullable, this method does nothing.
   *
   * @param instance the instance to invoke {@code method} on, or null if {@code method} is static
   */
  public void testMethodParameter(@Nullable Object instance, Method method, int paramIndex) {
    method.setAccessible(true);
    testParameter(instance, invokable(instance, method), paramIndex, method.getDeclaringClass());
  }

  /**
   * Verifies that {@code ctor} produces a {@link NullPointerException} or {@link
   * UnsupportedOperationException} when the parameter in position {@code paramIndex} is null. If
   * this parameter is marked nullable, this method does nothing.
   */
  public void testConstructorParameter(Constructor<?> ctor, int paramIndex) {
    ctor.setAccessible(true);
    testParameter(null, Invokable.from(ctor), paramIndex, ctor.getDeclaringClass());
  }

  /** Visibility of any method or constructor. */
  public enum Visibility {
    PACKAGE {
      @Override
      boolean isVisible(int modifiers) {
        return !Modifier.isPrivate(modifiers);
      }
    },

    PROTECTED {
      @Override
      boolean isVisible(int modifiers) {
        return Modifier.isPublic(modifiers) || Modifier.isProtected(modifiers);
      }
    },

    PUBLIC {
      @Override
      boolean isVisible(int modifiers) {
        return Modifier.isPublic(modifiers);
      }
    };

    abstract boolean isVisible(int modifiers);

    /** Returns {@code true} if {@code member} is visible under {@code this} visibility. */
    final boolean isVisible(Member member) {
      return isVisible(member.getModifiers());
    }

    final Iterable<Method> getStaticMethods(Class<?> cls) {
      ImmutableList.Builder<Method> builder = ImmutableList.builder();
      for (Method method : getVisibleMethods(cls)) {
        if (Invokable.from(method).isStatic()) {
          builder.add(method);
        }
      }
      return builder.build();
    }

    final Iterable<Method> getInstanceMethods(Class<?> cls) {
      ConcurrentMap<Signature, Method> map = Maps.newConcurrentMap();
      for (Method method : getVisibleMethods(cls)) {
        if (!Invokable.from(method).isStatic()) {
          map.putIfAbsent(new Signature(method), method);
        }
      }
      return map.values();
    }

    private ImmutableList<Method> getVisibleMethods(Class<?> cls) {
      // Don't use cls.getPackage() because it does nasty things like reading
      // a file.
      String visiblePackage = Reflection.getPackageName(cls);
      ImmutableList.Builder<Method> builder = ImmutableList.builder();
      for (Class<?> type : TypeToken.of(cls).getTypes().rawTypes()) {
        if (!Reflection.getPackageName(type).equals(visiblePackage)) {
          break;
        }
        for (Method method : type.getDeclaredMethods()) {
          if (!method.isSynthetic() && isVisible(method)) {
            builder.add(method);
          }
        }
      }
      return builder.build();
    }
  }

  private static final class Signature {
    private final String name;
    private final ImmutableList<Class<?>> parameterTypes;

    Signature(Method method) {
      this(method.getName(), ImmutableList.copyOf(method.getParameterTypes()));
    }

    Signature(String name, ImmutableList<Class<?>> parameterTypes) {
      this.name = name;
      this.parameterTypes = parameterTypes;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
      if (obj instanceof Signature) {
        Signature that = (Signature) obj;
        return name.equals(that.name) && parameterTypes.equals(that.parameterTypes);
      }
      return false;
    }

    @Override
    public int hashCode() {
      return Objects.hashCode(name, parameterTypes);
    }
  }

  /**
   * Verifies that {@code invokable} produces a {@link NullPointerException} or {@link
   * UnsupportedOperationException} when the parameter in position {@code paramIndex} is null. If
   * this parameter is marked nullable, this method does nothing.
   *
   * @param instance the instance to invoke {@code invokable} on, or null if {@code invokable} is
   *     static
   */
  private void testParameter(
      @Nullable Object instance, Invokable<?, ?> invokable, int paramIndex, Class<?> testedClass) {
    /*
     * com.google.common is starting to rely on type-use annotations, which aren't visible under
     * Android VMs and in open-source guava-android. So we skip testing there.
     */
    if (Reflection.getPackageName(testedClass).startsWith("com.google.common")) {
      return;
    }
    if (isPrimitiveOrNullable(invokable.getParameters().get(paramIndex))) {
      return; // there's nothing to test
    }
    @Nullable Object[] params = buildParamList(invokable, paramIndex);
    try {
      @SuppressWarnings("unchecked") // We'll get a runtime exception if the type is wrong.
      Invokable<Object, ?> unsafe = (Invokable<Object, ?>) invokable;
      unsafe.invoke(instance, params);
      Assert.fail(
          "No exception thrown for parameter at index "
              + paramIndex
              + " from "
              + invokable
              + Arrays.toString(params)
              + " for "
              + testedClass);
    } catch (InvocationTargetException e) {
      Throwable cause = e.getCause();
      if (policy.isExpectedType(cause)) {
        return;
      }
      AssertionFailedError error =
          new AssertionFailedError(
              String.format(
                  "wrong exception thrown from %s when passing null to %s parameter at index %s.%n"
                      + "Full parameters: %s%n"
                      + "Actual exception message: %s",
                  invokable,
                  invokable.getParameters().get(paramIndex).getType(),
                  paramIndex,
                  Arrays.toString(params),
                  cause));
      error.initCause(cause);
      throw error;
    } catch (IllegalAccessException e) {
      throw new RuntimeException(e);
    }
  }

  private @Nullable Object[] buildParamList(
      Invokable<?, ?> invokable, int indexOfParamToSetToNull) {
    ImmutableList<Parameter> params = invokable.getParameters();
    @Nullable Object[] args = new Object[params.size()];

    for (int i = 0; i < args.length; i++) {
      Parameter param = params.get(i);
      if (i != indexOfParamToSetToNull) {
        args[i] = getDefaultValue(param.getType());
        Assert.assertTrue(
            "Can't find or create a sample instance for type '"
                + param.getType()
                + "'; please provide one using NullPointerTester.setDefault()",
            args[i] != null || isNullable(param));
      }
    }
    return args;
  }

  private <T> @Nullable T getDefaultValue(TypeToken<T> type) {
    // We assume that all defaults are generics-safe, even if they aren't,
    // we take the risk.
    @SuppressWarnings("unchecked")
    T defaultValue = (T) defaults.getInstance(type.getRawType());
    if (defaultValue != null) {
      return defaultValue;
    }
    @SuppressWarnings("unchecked") // All arbitrary instances are generics-safe
    T arbitrary = (T) ArbitraryInstances.get(type.getRawType());
    if (arbitrary != null) {
      return arbitrary;
    }
    if (type.getRawType() == Class.class) {
      // If parameter is Class<? extends Foo>, we return Foo.class
      @SuppressWarnings("unchecked")
      T defaultClass = (T) getFirstTypeParameter(type.getType()).getRawType();
      return defaultClass;
    }
    if (type.getRawType() == TypeToken.class) {
      // If parameter is TypeToken<? extends Foo>, we return TypeToken<Foo>.
      @SuppressWarnings("unchecked")
      T defaultType = (T) getFirstTypeParameter(type.getType());
      return defaultType;
    }
    if (type.getRawType() == Converter.class) {
      TypeToken<?> convertFromType = type.resolveType(Converter.class.getTypeParameters()[0]);
      TypeToken<?> convertToType = type.resolveType(Converter.class.getTypeParameters()[1]);
      @SuppressWarnings("unchecked") // returns default for both F and T
      T defaultConverter = (T) defaultConverter(convertFromType, convertToType);
      return defaultConverter;
    }
    if (type.getRawType().isInterface()) {
      return newDefaultReturningProxy(type);
    }
    return null;
  }

  private <F, T> Converter<F, T> defaultConverter(
      final TypeToken<F> convertFromType, final TypeToken<T> convertToType) {
    return new Converter<F, T>() {
      @Override
      protected T doForward(F a) {
        return doConvert(convertToType);
      }

      @Override
      protected F doBackward(T b) {
        return doConvert(convertFromType);
      }

      private /*static*/ <S> S doConvert(TypeToken<S> type) {
        return checkNotNull(getDefaultValue(type));
      }
    };
  }

  private static TypeToken<?> getFirstTypeParameter(Type type) {
    if (type instanceof ParameterizedType) {
      return TypeToken.of(((ParameterizedType) type).getActualTypeArguments()[0]);
    } else {
      return TypeToken.of(Object.class);
    }
  }

  private <T> T newDefaultReturningProxy(final TypeToken<T> type) {
    return new DummyProxy() {
      @Override
      <R> @Nullable R dummyReturnValue(TypeToken<R> returnType) {
        return getDefaultValue(returnType);
      }
    }.newProxy(type);
  }

  private static Invokable<?, ?> invokable(@Nullable Object instance, Method method) {
    if (instance == null) {
      return Invokable.from(method);
    } else {
      return TypeToken.of(instance.getClass()).method(method);
    }
  }

  static boolean isPrimitiveOrNullable(Parameter param) {
    return param.getType().getRawType().isPrimitive() || isNullable(param);
  }

  private static final ImmutableSet<String> NULLABLE_ANNOTATION_SIMPLE_NAMES =
      ImmutableSet.of("CheckForNull", "Nullable", "NullableDecl", "NullableType");

  static boolean isNullable(Invokable<?, ?> invokable) {
    return NULLNESS_ANNOTATION_READER.isNullable(invokable);
  }

  static boolean isNullable(Parameter param) {
    return NULLNESS_ANNOTATION_READER.isNullable(param);
  }

  private static boolean containsNullable(Annotation[] annotations) {
    for (Annotation annotation : annotations) {
      if (NULLABLE_ANNOTATION_SIMPLE_NAMES.contains(annotation.annotationType().getSimpleName())) {
        return true;
      }
    }
    return false;
  }

  private boolean isIgnored(Member member) {
    return member.isSynthetic() || ignoredMembers.contains(member) || isEquals(member);
  }

  /**
   * Returns true if the given member is a method that overrides {@link Object#equals(Object)}.
   *
   * <p>The documentation for {@link Object#equals} says it should accept null, so don't require an
   * explicit {@code @NullableDecl} annotation (see <a
   * href="https://github.com/google/guava/issues/1819">#1819</a>).
   *
   * <p>It is not necessary to consider visibility, return type, or type parameter declarations. The
   * declaration of a method with the same name and formal parameters as {@link Object#equals} that
   * is not public and boolean-returning, or that declares any type parameters, would be rejected at
   * compile-time.
   */
  private static boolean isEquals(Member member) {
    if (!(member instanceof Method)) {
      return false;
    }
    Method method = (Method) member;
    if (!method.getName().contentEquals("equals")) {
      return false;
    }
    Class<?>[] parameters = method.getParameterTypes();
    if (parameters.length != 1) {
      return false;
    }
    if (!parameters[0].equals(Object.class)) {
      return false;
    }
    return true;
  }

  /** Strategy for exception type matching used by {@link NullPointerTester}. */
  private enum ExceptionTypePolicy {

    /**
     * Exceptions should be {@link NullPointerException} or {@link UnsupportedOperationException}.
     */
    NPE_OR_UOE() {
      @Override
      public boolean isExpectedType(Throwable cause) {
        return cause instanceof NullPointerException
            || cause instanceof UnsupportedOperationException;
      }
    },

    /**
     * Exceptions should be {@link NullPointerException}, {@link IllegalArgumentException}, or
     * {@link UnsupportedOperationException}.
     */
    NPE_IAE_OR_UOE() {
      @Override
      public boolean isExpectedType(Throwable cause) {
        return cause instanceof NullPointerException
            || cause instanceof IllegalArgumentException
            || cause instanceof UnsupportedOperationException;
      }
    };

    public abstract boolean isExpectedType(Throwable cause);
  }

  private static boolean annotatedTypeExists() {
    try {
      Class.forName("java.lang.reflect.AnnotatedType");
    } catch (ClassNotFoundException e) {
      return false;
    }
    return true;
  }

  private static final NullnessAnnotationReader NULLNESS_ANNOTATION_READER =
      annotatedTypeExists()
          ? NullnessAnnotationReader.FROM_DECLARATION_AND_TYPE_USE_ANNOTATIONS
          : NullnessAnnotationReader.FROM_DECLARATION_ANNOTATIONS_ONLY;

  /**
   * Looks for declaration nullness annotations and, if supported, type-use nullness annotations.
   *
   * <p>Under Android VMs, the methods for retrieving type-use annotations don't exist. This means
   * that {@link NullPointerTester} may misbehave under Android when used on classes that rely on
   * type-use annotations.
   *
   * <p>Under j2objc, the necessary APIs exist, but some (perhaps all) return stub values, like
   * empty arrays. Presumably {@link NullPointerTester} could likewise misbehave under j2objc, but I
   * don't know that anyone uses it there, anyway.
   */
  private enum NullnessAnnotationReader {
    @SuppressWarnings("Java7ApiChecker")
    FROM_DECLARATION_AND_TYPE_USE_ANNOTATIONS {
      @Override
      boolean isNullable(Invokable<?, ?> invokable) {
        return FROM_DECLARATION_ANNOTATIONS_ONLY.isNullable(invokable)
        ;
        // TODO(cpovirk): Should we also check isNullableTypeVariable?
      }

      @Override
      boolean isNullable(Parameter param) {
        return FROM_DECLARATION_ANNOTATIONS_ONLY.isNullable(param)
        ;
      }
    },
    FROM_DECLARATION_ANNOTATIONS_ONLY {
      @Override
      boolean isNullable(Invokable<?, ?> invokable) {
        return containsNullable(invokable.getAnnotations());
      }

      @Override
      boolean isNullable(Parameter param) {
        return containsNullable(param.getAnnotations());
      }
    };

    abstract boolean isNullable(Invokable<?, ?> invokable);

    abstract boolean isNullable(Parameter param);
  }
}
