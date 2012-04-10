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

import com.google.common.annotations.Beta;
import com.google.common.base.Objects;
import com.google.common.collect.ClassToInstanceMap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.MutableClassToInstanceMap;
import com.google.common.reflect.AbstractInvocationHandler;
import com.google.common.reflect.Reflection;
import com.google.common.reflect.TypeToken;

import junit.framework.Assert;
import junit.framework.AssertionFailedError;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;

import javax.annotation.Nullable;

/**
 * A test utility that verifies that your methods and constructors throw {@link
 * NullPointerException} or {@link UnsupportedOperationException} whenever null
 * is passed to a parameter that isn't annotated with {@link Nullable}.
 *
 * <p>The tested methods and constructors are invoked -- each time with one
 * parameter being null and the rest not null -- and the test fails if no
 * expected exception is thrown. {@code NullPointerTester} uses best effort to
 * pick non-null default values for many common JDK and Guava types, and also
 * for interfaces and public classes that have public parameter-less
 * constructors. When the non-null default value for a particular parameter type
 * cannot be provided by {@code NullPointerTester}, the caller can provide a
 * custom non-null default value for the parameter type via {@link #setDefault}.
 *
 * @author Kevin Bourrillion
 * @since 10.0
 */
@Beta
public final class NullPointerTester {

  private final ClassToInstanceMap<Object> defaults =
      MutableClassToInstanceMap.create();
  private final List<Member> ignoredMembers = Lists.newArrayList();

  /**
   * Sets a default value that can be used for any parameter of type
   * {@code type}. Returns this object.
   */
  public <T> NullPointerTester setDefault(Class<T> type, T value) {
    defaults.put(type, value);
    return this;
  }

  /**
   * Ignore {@code member} in the tests that follow. Returns this object.
   * @deprecated Use {@link #ignore(Method)} instead. This method will be
   *     removed from Guava in Guava release 13.0.
   */
  @Deprecated
  public NullPointerTester ignore(Member member) {
    ignoredMembers.add(member);
    return this;
  }

  /**
   * Ignore {@code method} in the tests that follow. Returns this object.
   *
   * @since 13.0
   */
  public NullPointerTester ignore(Method method) {
    ignoredMembers.add(method);
    return this;
  }

  /**
   * Runs {@link #testConstructor} on every constructor in class {@code c} that
   * has at least {@code minimalVisibility}.
   */
  public void testConstructors(Class<?> c, Visibility minimalVisibility) {
    for (Constructor<?> constructor : c.getDeclaredConstructors()) {
      if (minimalVisibility.isVisible(constructor) && !isIgnored(constructor)) {
        testConstructor(constructor);
      }
    }
  }

  /**
   * Runs {@link #testConstructor} on every public constructor in class {@code
   * c}.
   */
  public void testAllPublicConstructors(Class<?> c) {
    testConstructors(c, Visibility.PUBLIC);
  }

  /**
   * Runs {@link #testMethod} on every static method of class {@code c} that has
   * at least {@code minimalVisibility}, including those "inherited" from
   * superclasses of the same package.
   */
  public void testStaticMethods(Class<?> c, Visibility minimalVisibility) {
    for (Method method : minimalVisibility.getStaticMethods(c)) {
      if (!isIgnored(method)) {
        testMethod(null, method);
      }
    }
  }

  /**
   * Runs {@link #testMethod} on every public static method of class {@code c},
   * including those "inherited" from superclasses of the same package.
   */
  public void testAllPublicStaticMethods(Class<?> c) {
    testStaticMethods(c, Visibility.PUBLIC);
  }

  /**
   * Runs {@link #testMethod} on every instance method of the class of
   * {@code instance} with at least {@code minimalVisibility}, including those
   * inherited from superclasses of the same package.
   */
  public void testInstanceMethods(
      Object instance, Visibility minimalVisibility) {
    Class<?> c = instance.getClass();
    for (Method method : minimalVisibility.getInstanceMethods(c)) {
      if (!isIgnored(method)) {
        testMethod(instance, method);
      }
    }
  }

  /**
   * Runs {@link #testMethod} on every public instance method of the class of
   * {@code instance}, including those inherited from superclasses of the same
   * package.
   */
  public void testAllPublicInstanceMethods(Object instance) {
    testInstanceMethods(instance, Visibility.PUBLIC);
  }

  /**
   * Verifies that {@code method} produces a {@link NullPointerException}
   * or {@link UnsupportedOperationException} whenever <i>any</i> of its
   * non-{@link Nullable} parameters are null.
   *
   * @param instance the instance to invoke {@code method} on, or null if
   *     {@code method} is static
   */
  public void testMethod(Object instance, Method method) {
    Class<?>[] types = method.getParameterTypes();
    for (int nullIndex = 0; nullIndex < types.length; nullIndex++) {
      testMethodParameter(instance, method, nullIndex);
    }
  }

  /**
   * Verifies that {@code ctor} produces a {@link NullPointerException} or
   * {@link UnsupportedOperationException} whenever <i>any</i> of its
   * non-{@link Nullable} parameters are null.
   */
  public void testConstructor(Constructor<?> ctor) {
    Class<?>[] types = ctor.getParameterTypes();
    for (int nullIndex = 0; nullIndex < types.length; nullIndex++) {
      testConstructorParameter(ctor, nullIndex);
    }
  }

  /**
   * Verifies that {@code method} produces a {@link NullPointerException} or
   * {@link UnsupportedOperationException} when the parameter in position {@code
   * paramIndex} is null.  If this parameter is marked {@link Nullable}, this
   * method does nothing.
   *
   * @param instance the instance to invoke {@code method} on, or null if
   *     {@code method} is static
   */
  public void testMethodParameter(
      final Object instance, final Method method, int paramIndex) {
    method.setAccessible(true);
    testFunctorParameter(instance, new Functor() {
        @Override public Type[] getParameterTypes() {
          Type[] unresolved = method.getGenericParameterTypes();
          if (isStatic(method)) {
            return unresolved;
          } else {
            TypeToken<?> type = TypeToken.of(instance.getClass());
            Type[] resolved = new Type[unresolved.length];
            for (int i = 0; i < unresolved.length; i++) {
              resolved[i] = type.resolveType(unresolved[i]).getType();
            }
            return resolved;
          }
        }
        @Override public Annotation[][] getParameterAnnotations() {
          return method.getParameterAnnotations();
        }
        @Override public void invoke(Object object, Object[] params)
            throws InvocationTargetException, IllegalAccessException {
          method.invoke(object, params);
        }
        @Override public String toString() {
          return method.toString();
        }
      }, paramIndex, method.getDeclaringClass());
  }

  /**
   * Verifies that {@code ctor} produces a {@link NullPointerException} or
   * {@link UnsupportedOperationException} when the parameter in position {@code
   * paramIndex} is null.  If this parameter is marked {@link Nullable}, this
   * method does nothing.
   */
  public void testConstructorParameter(
      final Constructor<?> ctor, int paramIndex) {
    ctor.setAccessible(true);
    testFunctorParameter(null, new Functor() {
        @Override public Type[] getParameterTypes() {
          return ctor.getGenericParameterTypes();
        }
        @Override public Annotation[][] getParameterAnnotations() {
          return ctor.getParameterAnnotations();
        }
        @Override public void invoke(Object instance, Object[] params)
            throws InvocationTargetException, IllegalAccessException,
            InstantiationException {
          ctor.newInstance(params);
        }
      }, paramIndex, ctor.getDeclaringClass());
  }

  /** Visibility of any method or constructor. */
  public enum Visibility {

    PACKAGE {
      @Override boolean isVisible(int modifiers) {
        return !Modifier.isPrivate(modifiers);
      }
    },

    PROTECTED {
      @Override boolean isVisible(int modifiers) {
        return Modifier.isPublic(modifiers) || Modifier.isProtected(modifiers);
      }
    },

    PUBLIC {
      @Override boolean isVisible(int modifiers) {
        return Modifier.isPublic(modifiers);
      }
    };

    abstract boolean isVisible(int modifiers);

    /**
     * Returns {@code true} if {@code member} is visible under {@code this}
     * visibility.
     */
    final boolean isVisible(Member member) {
      return isVisible(member.getModifiers());
    }

    final Iterable<Method> getStaticMethods(Class<?> cls) {
      ImmutableList.Builder<Method> builder = ImmutableList.builder();
      for (Method method : getVisibleMethods(cls)) {
        if (isStatic(method)) {
          builder.add(method);
        }
      }
      return builder.build();
    }

    final Iterable<Method> getInstanceMethods(Class<?> cls) {
      ConcurrentMap<Signature, Method> map = Maps.newConcurrentMap();
      for (Method method : getVisibleMethods(cls)) {
        if (!isStatic(method)) {
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
      for (Class<?> type : TypeToken.of(cls).getTypes().classes().rawTypes()) {
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

  // TODO(benyu): Use labs/reflect/Signature if it graduates.
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

    @Override public boolean equals(Object obj) {
      if (obj instanceof Signature) {
        Signature that = (Signature) obj;
        return name.equals(that.name)
            && parameterTypes.equals(that.parameterTypes);
      }
      return false;
    }

    @Override public int hashCode() {
      return Objects.hashCode(name, parameterTypes);
    }
  }

  /**
   * Verifies that {@code func} produces a {@link NullPointerException} or
   * {@link UnsupportedOperationException} when the parameter in position {@code
   * paramIndex} is null.  If this parameter is marked {@link Nullable}, this
   * method does nothing.
   *
   * @param instance the instance to invoke {@code func} on, or null if
   *     {@code func} is static
   */
  private void testFunctorParameter(Object instance, Functor func,
      int paramIndex, Class<?> testedClass) {
    if (parameterIsPrimitiveOrNullable(func, paramIndex)) {
      return; // there's nothing to test
    }
    Object[] params = buildParamList(func, paramIndex);
    try {
      func.invoke(instance, params);
      Assert.fail("No exception thrown from " + func +
          Arrays.toString(params) + " for " + testedClass);
    } catch (InvocationTargetException e) {
      Throwable cause = e.getCause();
      if (cause instanceof NullPointerException ||
          cause instanceof UnsupportedOperationException) {
        return;
      }
      AssertionFailedError error = new AssertionFailedError(
          "wrong exception thrown from " + func + ": " + cause);
      error.initCause(cause);
      throw error;
    } catch (IllegalAccessException e) {
      throw new RuntimeException(e);
    } catch (InstantiationException e) {
      throw new RuntimeException(e);
    }
  }

  private static boolean parameterIsPrimitiveOrNullable(
      Functor func, int paramIndex) {
    if (TypeToken.of(func.getParameterTypes()[paramIndex]).getRawType()
        .isPrimitive()) {
      return true;
    }
    Annotation[] annotations = func.getParameterAnnotations()[paramIndex];
    for (Annotation annotation : annotations) {
      if (annotation instanceof Nullable) {
        return true;
      }
    }
    return false;
  }

  private Object[] buildParamList(Functor func, int indexOfParamToSetToNull) {
    Type[] types = func.getParameterTypes();
    Object[] params = new Object[types.length];

    for (int i = 0; i < types.length; i++) {
      if (i != indexOfParamToSetToNull) {
        TypeToken<?> type = TypeToken.of(types[i]);
        params[i] = getDefaultValue(type);
        if (!parameterIsPrimitiveOrNullable(func, i)) {
          Assert.assertTrue("No default value found for " + type.getRawType(),
              params[i] != null);
        }
      }
    }
    return params;
  }

  private <T> T getDefaultValue(TypeToken<T> type) {
    // We assume that all defaults are generics-safe, even if they aren't,
    // we take the risk.
    @SuppressWarnings("unchecked")
    T defaultValue = (T) defaults.getInstance(type.getRawType());
    if (defaultValue != null) {
      return defaultValue;
    }
    @SuppressWarnings("unchecked") // All null values are generics-safe
    T nullValue = (T) ArbitraryInstances.get(type.getRawType());
    if (nullValue != null) {
      return nullValue;
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
    if (type.getRawType().isInterface()) {
      return newDefaultReturningProxy(type);
    }
    return null;
  }

  private static TypeToken<?> getFirstTypeParameter(Type type) {
    if (type instanceof ParameterizedType) {
      return TypeToken.of(
          ((ParameterizedType) type).getActualTypeArguments()[0]);
    } else {
      return TypeToken.of(Object.class);
    }
  }

  @SuppressWarnings("unchecked") // T implemented with dynamic proxy.
  private <T> T newDefaultReturningProxy(final TypeToken<T> type) {
    Set<Class<? super T>> interfaceClasses =
        type.getTypes().interfaces().rawTypes();
    return (T) Proxy.newProxyInstance(
        interfaceClasses.iterator().next().getClassLoader(),
        interfaceClasses.toArray(new Class<?>[interfaceClasses.size()]),
        new AbstractInvocationHandler() {
          @Override protected Object handleInvocation(
              Object proxy, Method method, Object[] args) {
            return getDefaultValue(
                type.resolveType(method.getGenericReturnType()));
          }
          @Override public String toString() {
            return "NullPointerTester proxy for " + type;
          }
        });
  }

  private interface Functor {
    Type[] getParameterTypes();
    Annotation[][] getParameterAnnotations();
    void invoke(Object o, Object[] params)
        throws InvocationTargetException, IllegalAccessException,
            InstantiationException;
  }

  private static boolean isStatic(Member member) {
    return Modifier.isStatic(member.getModifiers());
  }

  private boolean isIgnored(Member member) {
    return member.isSynthetic() || ignoredMembers.contains(member);
  }
}
