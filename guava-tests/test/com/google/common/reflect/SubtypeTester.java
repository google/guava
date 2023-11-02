/*
 * Copyright (C) 2016 The Guava Authors
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

import static com.google.common.base.Preconditions.checkState;
import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;

import com.google.errorprone.annotations.RequiredModifiers;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Comparator;
import javax.lang.model.element.Modifier;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Tester of subtyping relationships between two types.
 *
 * <p>Tests should inherit from this class, and declare subtyping relationship with public methods
 * annotated by {@link TestSubtype}.
 *
 * <p>These declaration methods rely on Java static type checking to make sure what we want to
 * assert as subtypes are really subtypes according to javac. For example:
 *
 * <pre>{@code
 * class MySubtypeTests extends SubtypeTester {
 *   @TestSubtype(suppressGetSubtype = true, suppressGetSupertype = true)
 *   public <T> Iterable<? extends T> listIsSubtypeOfIterable(List<T> list) {
 *     return isSubtype(list);
 *   }
 *
 *   @TestSubtype
 *   public List<String> intListIsNotSubtypeOfStringList(List<Integer> intList) {
 *     return notSubtype(intList);
 *   }
 * }
 *
 * public void testMySubtypes() throws Exception {
 *   new MySubtypeTests().testAllDeclarations();
 * }
 * }</pre>
 *
 * The calls to {@link #isSubtype} and {@link #notSubtype} tells the framework what assertions need
 * to be made.
 *
 * <p>The declaration methods must be public.
 */
@AndroidIncompatible // only used by android incompatible tests.
abstract class SubtypeTester implements Cloneable {

  /** Annotates a public method that declares subtype assertion. */
  @RequiredModifiers(Modifier.PUBLIC)
  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.METHOD)
  @interface TestSubtype {
    /** Suppresses the assertion on {@link TypeToken#getSubtype}. */
    boolean suppressGetSubtype() default false;

    /** Suppresses the assertion on {@link TypeToken#getSupertype}. */
    boolean suppressGetSupertype() default false;
  }

  private @Nullable Method method = null;

  /** Call this in a {@link TestSubtype} public method asserting subtype relationship. */
  final <T> T isSubtype(T sub) {
    Type returnType = method.getGenericReturnType();
    Type paramType = getOnlyParameterType();
    TestSubtype spec = method.getAnnotation(TestSubtype.class);
    assertWithMessage("%s is subtype of %s", paramType, returnType)
        .that(TypeToken.of(paramType).isSubtypeOf(returnType))
        .isTrue();
    assertWithMessage("%s is supertype of %s", returnType, paramType)
        .that(TypeToken.of(returnType).isSupertypeOf(paramType))
        .isTrue();
    if (!spec.suppressGetSubtype()) {
      assertThat(getSubtype(returnType, TypeToken.of(paramType).getRawType())).isEqualTo(paramType);
    }
    if (!spec.suppressGetSupertype()) {
      assertThat(getSupertype(paramType, TypeToken.of(returnType).getRawType()))
          .isEqualTo(returnType);
    }
    return sub;
  }

  /**
   * Call this in a {@link TestSubtype} public method asserting that subtype relationship does not
   * hold.
   */
  final <X> @Nullable X notSubtype(@SuppressWarnings("unused") Object sub) {
    Type returnType = method.getGenericReturnType();
    Type paramType = getOnlyParameterType();
    TestSubtype spec = method.getAnnotation(TestSubtype.class);
    assertWithMessage("%s is subtype of %s", paramType, returnType)
        .that(TypeToken.of(paramType).isSubtypeOf(returnType))
        .isFalse();
    assertWithMessage("%s is supertype of %s", returnType, paramType)
        .that(TypeToken.of(returnType).isSupertypeOf(paramType))
        .isFalse();
    if (!spec.suppressGetSubtype()) {
      try {
        assertThat(getSubtype(returnType, TypeToken.of(paramType).getRawType()))
            .isNotEqualTo(paramType);
      } catch (IllegalArgumentException notSubtype1) {
        // The raw class isn't even a subclass.
      }
    }
    if (!spec.suppressGetSupertype()) {
      try {
        assertThat(getSupertype(paramType, TypeToken.of(returnType).getRawType()))
            .isNotEqualTo(returnType);
      } catch (IllegalArgumentException notSubtype2) {
        // The raw class isn't even a subclass.
      }
    }
    return null;
  }

  final void testAllDeclarations() throws Exception {
    checkState(method == null);
    Method[] methods = getClass().getMethods();
    Arrays.sort(
        methods,
        new Comparator<Method>() {
          @Override
          public int compare(Method a, Method b) {
            return a.getName().compareTo(b.getName());
          }
        });
    for (Method method : methods) {
      if (method.isAnnotationPresent(TestSubtype.class)) {
        method.setAccessible(true);
        SubtypeTester tester = (SubtypeTester) clone();
        tester.method = method;
        method.invoke(tester, new Object[] {null});
      }
    }
  }

  private Type getOnlyParameterType() {
    assertThat(method.getGenericParameterTypes()).hasLength(1);
    return method.getGenericParameterTypes()[0];
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  private static Type getSupertype(Type type, Class<?> superclass) {
    Class rawType = superclass;
    return TypeToken.of(type).getSupertype(rawType).getType();
  }

  private static Type getSubtype(Type type, Class<?> subclass) {
    return TypeToken.of(type).getSubtype(subclass).getType();
  }
}
