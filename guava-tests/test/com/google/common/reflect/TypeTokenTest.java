/*
 * Copyright (C) 2007 The Guava Authors
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

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.primitives.Primitives;
import com.google.common.testing.EqualsTester;
import com.google.common.testing.NullPointerTester;
import com.google.common.testing.SerializableTester;
import com.google.common.truth.IterableSubject;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import junit.framework.TestCase;

/**
 * Test cases for {@link TypeToken}.
 *
 * @author Sven Mawson
 * @author Ben Yu
 */
@AndroidIncompatible // lots of failures, possibly some related to bad equals() implementations?
public class TypeTokenTest extends TestCase {

  private abstract static class StringList implements List<String> {}

  private abstract static class IntegerList implements List<Integer> {}

  public void testValueEqualityNotInstanceEquality() {
    TypeToken<List<String>> a = new TypeToken<List<String>>() {};
    TypeToken<List<String>> b = new TypeToken<List<String>>() {};
    assertEquals(a, b);
  }

  public <T> void testVariableTypeTokenNotAllowed() {
    try {
      new TypeToken<T>() {};
      fail();
    } catch (IllegalStateException expected) {
    }
  }

  public void testRawTypeIsCorrect() {
    TypeToken<List<String>> token = new TypeToken<List<String>>() {};
    assertEquals(List.class, token.getRawType());
  }

  public void testTypeIsCorrect() {
    TypeToken<List<String>> token = new TypeToken<List<String>>() {};
    assertEquals(StringList.class.getGenericInterfaces()[0], token.getType());
  }

  @SuppressWarnings("rawtypes") // Trying to test TypeToken.of(List.class)
  public void testGetClass() {
    TypeToken<List> token = TypeToken.of(List.class);
    assertEquals(new TypeToken<List>() {}, token);
  }

  public void testGetType() {
    TypeToken<?> t = TypeToken.of(StringList.class.getGenericInterfaces()[0]);
    assertEquals(new TypeToken<List<String>>() {}, t);
  }

  public void testNonStaticLocalClass() {
    class Local<T> {}
    TypeToken<Local<String>> type = new TypeToken<Local<String>>() {};
    assertEquals(Types.newParameterizedType(Local.class, String.class), type.getType());
    assertEquals(new Local<String>() {}.getClass().getGenericSuperclass(), type.getType());
  }

  public void testStaticLocalClass() {
    doTestStaticLocalClass();
  }

  private static void doTestStaticLocalClass() {
    class Local<T> {}
    TypeToken<Local<String>> type = new TypeToken<Local<String>>() {};
    assertEquals(Types.newParameterizedType(Local.class, String.class), type.getType());
    assertEquals(new Local<String>() {}.getClass().getGenericSuperclass(), type.getType());
  }

  public void testGenericArrayType() {
    TypeToken<List<String>[]> token = new TypeToken<List<String>[]>() {};
    assertEquals(List[].class, token.getRawType());
    assertThat(token.getType()).isInstanceOf(GenericArrayType.class);
  }

  public void testMultiDimensionalGenericArrayType() {
    TypeToken<List<Long>[][][]> token = new TypeToken<List<Long>[][][]>() {};
    assertEquals(List[][][].class, token.getRawType());
    assertThat(token.getType()).isInstanceOf(GenericArrayType.class);
  }

  public <T> void testGenericVariableTypeArrays() {
    assertEquals("T[]", new TypeToken<T[]>() {}.toString());
  }

  public void testResolveType() throws Exception {
    Method getFromList = List.class.getMethod("get", int.class);
    TypeToken<?> returnType =
        new TypeToken<List<String>>() {}.resolveType(getFromList.getGenericReturnType());
    assertEquals(String.class, returnType.getType());
  }

  public <F extends Enum<F> & Function<String, Integer> & Iterable<Long>>
      void testResolveType_fromTypeVariable() {
    TypeToken<?> f = TypeToken.of(new TypeCapture<F>() {}.capture());
    assertEquals(String.class, f.resolveType(Function.class.getTypeParameters()[0]).getType());
    assertEquals(Integer.class, f.resolveType(Function.class.getTypeParameters()[1]).getType());
    assertEquals(Long.class, f.resolveType(Iterable.class.getTypeParameters()[0]).getType());
  }

  public <E extends Comparable<Iterable<String>> & Iterable<Integer>>
      void testResolveType_fromTypeVariable_onlyDirectBoundsAreUsed() {
    TypeToken<?> e = TypeToken.of(new TypeCapture<E>() {}.capture());
    assertEquals(Integer.class, e.resolveType(Iterable.class.getTypeParameters()[0]).getType());
  }

  public void testResolveType_fromWildcard() {
    ParameterizedType withWildcardType =
        (ParameterizedType) new TypeCapture<Comparable<? extends Iterable<String>>>() {}.capture();
    TypeToken<?> wildcardType = TypeToken.of(withWildcardType.getActualTypeArguments()[0]);
    assertEquals(
        String.class, wildcardType.resolveType(Iterable.class.getTypeParameters()[0]).getType());
  }

  public void testGetTypes_noSuperclass() {
    TypeToken<Object>.TypeSet types = new TypeToken<Object>() {}.getTypes();
    assertThat(types).contains(TypeToken.of(Object.class));
    assertThat(types.rawTypes()).contains(Object.class);
    assertThat(types.interfaces()).isEmpty();
    assertThat(types.interfaces().rawTypes()).isEmpty();
    assertThat(types.classes()).contains(TypeToken.of(Object.class));
    assertThat(types.classes().rawTypes()).contains(Object.class);
  }

  public void testGetTypes_fromInterface() {
    TypeToken<Interface1>.TypeSet types = new TypeToken<Interface1>() {}.getTypes();
    assertThat(types).contains(TypeToken.of(Interface1.class));
    assertThat(types.rawTypes()).contains(Interface1.class);
    assertThat(types.interfaces()).contains(TypeToken.of(Interface1.class));
    assertThat(types.interfaces().rawTypes()).contains(Interface1.class);
    assertThat(types.classes()).isEmpty();
    assertThat(types.classes().rawTypes()).isEmpty();
  }

  public void testGetTypes_fromPrimitive() {
    TypeToken<Integer>.TypeSet types = TypeToken.of(int.class).getTypes();
    assertThat(types).contains(TypeToken.of(int.class));
    assertThat(types.rawTypes()).contains(int.class);
    assertThat(types.interfaces()).isEmpty();
    assertThat(types.interfaces().rawTypes()).isEmpty();
    assertThat(types.classes()).contains(TypeToken.of(int.class));
    assertThat(types.classes().rawTypes()).contains(int.class);
  }

  public void testGetTypes_withInterfacesAndSuperclasses() {
    abstract class Class2 extends Class1 implements Interface12 {}
    abstract class Class3<T> extends Class2 implements Interface3<T> {}
    TypeToken<Class3<String>>.TypeSet types = new TypeToken<Class3<String>>() {}.getTypes();
    makeUnmodifiable(types)
        .containsExactly(
            new TypeToken<Class3<String>>() {},
            new TypeToken<Interface3<String>>() {},
            new TypeToken<Iterable<String>>() {},
            TypeToken.of(Class2.class),
            TypeToken.of(Interface12.class),
            TypeToken.of(Interface1.class),
            TypeToken.of(Interface2.class),
            TypeToken.of(Class1.class),
            TypeToken.of(Object.class));
    makeUnmodifiable(types.interfaces())
        .containsExactly(
            new TypeToken<Interface3<String>>() {},
            TypeToken.of(Interface12.class),
            TypeToken.of(Interface1.class),
            TypeToken.of(Interface2.class),
            new TypeToken<Iterable<String>>() {});
    makeUnmodifiable(types.classes())
        .containsExactly(
            new TypeToken<Class3<String>>() {},
            TypeToken.of(Class2.class),
            TypeToken.of(Class1.class),
            TypeToken.of(Object.class));
    assertSubtypeFirst(types);
  }

  public void testGetTypes_rawTypes_withInterfacesAndSuperclasses() {
    abstract class Class2 extends Class1 implements Interface12 {}
    abstract class Class3<T> extends Class2 implements Interface3<T> {}
    TypeToken<Class3<String>>.TypeSet types = new TypeToken<Class3<String>>() {}.getTypes();
    makeUnmodifiable(types.rawTypes())
        .containsExactly(
            Class3.class,
            Interface3.class,
            Iterable.class,
            Class2.class,
            Interface12.class,
            Interface1.class,
            Interface2.class,
            Class1.class,
            Object.class);
    makeUnmodifiable(types.interfaces().rawTypes())
        .containsExactly(
            Interface3.class,
            Interface12.class,
            Interface1.class,
            Interface2.class,
            Iterable.class);
    makeUnmodifiable(types.classes().rawTypes())
        .containsExactly(Class3.class, Class2.class, Class1.class, Object.class);
    assertSubtypeFirst(types);
  }

  public <A extends Class1 & Interface1, B extends A>
      void testGetTypes_ignoresTypeVariablesByDefault() {
    TypeToken<?>.TypeSet types = TypeToken.of(new TypeCapture<B>() {}.capture()).getTypes();
    makeUnmodifiable(types)
        .containsExactly(
            TypeToken.of(Interface1.class), TypeToken.of(Class1.class), TypeToken.of(Object.class));
    assertSubtypeFirst(types);
    makeUnmodifiable(types.interfaces()).containsExactly(TypeToken.of(Interface1.class));
    makeUnmodifiable(types.classes())
        .containsExactly(TypeToken.of(Class1.class), TypeToken.of(Object.class))
        .inOrder();
  }

  public <A extends Class1 & Interface1, B extends A>
      void testGetTypes_rawTypes_ignoresTypeVariablesByDefault() {
    TypeToken<?>.TypeSet types = TypeToken.of(new TypeCapture<B>() {}.capture()).getTypes();
    makeUnmodifiable(types.rawTypes())
        .containsExactly(Interface1.class, Class1.class, Object.class);
    makeUnmodifiable(types.interfaces().rawTypes()).containsExactly(Interface1.class);
    makeUnmodifiable(types.classes().rawTypes())
        .containsExactly(Class1.class, Object.class)
        .inOrder();
  }

  public <A extends Interface1 & Interface2 & Interface3<String>> void testGetTypes_manyBounds() {
    TypeToken<?>.TypeSet types = TypeToken.of(new TypeCapture<A>() {}.capture()).getTypes();
    makeUnmodifiable(types.rawTypes())
        .containsExactly(Interface1.class, Interface2.class, Interface3.class, Iterable.class);
  }

  private static void assertSubtypeFirst(TypeToken<?>.TypeSet types) {
    assertSubtypeTokenBeforeSupertypeToken(types);
    assertSubtypeTokenBeforeSupertypeToken(types.interfaces());
    assertSubtypeTokenBeforeSupertypeToken(types.classes());
    assertSubtypeBeforeSupertype(types.rawTypes());
    assertSubtypeBeforeSupertype(types.interfaces().rawTypes());
    assertSubtypeBeforeSupertype(types.classes().rawTypes());
  }

  private static void assertSubtypeTokenBeforeSupertypeToken(
      Iterable<? extends TypeToken<?>> types) {
    int i = 0;
    for (TypeToken<?> left : types) {
      int j = 0;
      for (TypeToken<?> right : types) {
        if (left.isSupertypeOf(right)) {
          assertTrue(left + " should be after " + right, i >= j);
        }
        j++;
      }
      i++;
    }
  }

  private static void assertSubtypeBeforeSupertype(Iterable<? extends Class<?>> types) {
    int i = 0;
    for (Class<?> left : types) {
      int j = 0;
      for (Class<?> right : types) {
        if (left.isAssignableFrom(right)) {
          assertTrue(left + " should be after " + right, i >= j);
        }
        j++;
      }
      i++;
    }
  }

  // Tests to make sure assertSubtypeBeforeSupertype() works.

  public void testAssertSubtypeTokenBeforeSupertypeToken_empty() {
    assertSubtypeTokenBeforeSupertypeToken(ImmutableList.<TypeToken<?>>of());
  }

  public void testAssertSubtypeTokenBeforeSupertypeToken_oneType() {
    assertSubtypeTokenBeforeSupertypeToken(ImmutableList.of(TypeToken.of(String.class)));
  }

  public void testAssertSubtypeTokenBeforeSupertypeToken_subtypeFirst() {
    assertSubtypeTokenBeforeSupertypeToken(
        ImmutableList.of(TypeToken.of(String.class), TypeToken.of(CharSequence.class)));
  }

  public void testAssertSubtypeTokenBeforeSupertypeToken_supertypeFirst() {
    try {
      assertSubtypeTokenBeforeSupertypeToken(
          ImmutableList.of(TypeToken.of(CharSequence.class), TypeToken.of(String.class)));
    } catch (AssertionError expected) {
      return;
    }
    fail();
  }

  public void testAssertSubtypeTokenBeforeSupertypeToken_duplicate() {
    try {
      assertSubtypeTokenBeforeSupertypeToken(
          ImmutableList.of(TypeToken.of(String.class), TypeToken.of(String.class)));
    } catch (AssertionError expected) {
      return;
    }
    fail();
  }

  public void testAssertSubtypeBeforeSupertype_empty() {
    assertSubtypeBeforeSupertype(ImmutableList.<Class<?>>of());
  }

  public void testAssertSubtypeBeforeSupertype_oneType() {
    assertSubtypeBeforeSupertype(ImmutableList.of(String.class));
  }

  public void testAssertSubtypeBeforeSupertype_subtypeFirst() {
    assertSubtypeBeforeSupertype(ImmutableList.of(String.class, CharSequence.class));
  }

  public void testAssertSubtypeBeforeSupertype_supertypeFirst() {
    try {
      assertSubtypeBeforeSupertype(ImmutableList.of(CharSequence.class, String.class));
    } catch (AssertionError expected) {
      return;
    }
    fail();
  }

  public void testAssertSubtypeBeforeSupertype_duplicate() {
    try {
      assertSubtypeBeforeSupertype(ImmutableList.of(String.class, String.class));
    } catch (AssertionError expected) {
      return;
    }
    fail();
  }

  public void testGetGenericSuperclass_noSuperclass() {
    assertNull(new TypeToken<Object>() {}.getGenericSuperclass());
    assertEquals(TypeToken.of(Object.class), new TypeToken<Object[]>() {}.getGenericSuperclass());
    assertNull(new TypeToken<List<String>>() {}.getGenericSuperclass());
    assertEquals(
        TypeToken.of(Object.class), new TypeToken<List<String>[]>() {}.getGenericSuperclass());
  }

  public void testGetGenericSuperclass_withSuperclass() {
    TypeToken<? super ArrayList<String>> superToken =
        new TypeToken<ArrayList<String>>() {}.getGenericSuperclass();
    assertEquals(ArrayList.class.getSuperclass(), superToken.getRawType());
    assertEquals(
        String.class, ((ParameterizedType) superToken.getType()).getActualTypeArguments()[0]);
    assertEquals(TypeToken.of(Base.class), TypeToken.of(Sub.class).getGenericSuperclass());
    assertEquals(TypeToken.of(Object.class), TypeToken.of(Sub[].class).getGenericSuperclass());
  }

  public <T> void testGetGenericSuperclass_typeVariable_unbounded() {
    assertEquals(
        TypeToken.of(Object.class),
        TypeToken.of(new TypeCapture<T>() {}.capture()).getGenericSuperclass());
    assertEquals(TypeToken.of(Object.class), new TypeToken<T[]>() {}.getGenericSuperclass());
  }

  public <T extends ArrayList<String> & CharSequence>
      void testGetGenericSuperclass_typeVariable_boundIsClass() {
    assertEquals(
        new TypeToken<ArrayList<String>>() {},
        TypeToken.of(new TypeCapture<T>() {}.capture()).getGenericSuperclass());
    assertEquals(TypeToken.of(Object.class), new TypeToken<T[]>() {}.getGenericSuperclass());
  }

  public <T extends Enum<T> & CharSequence>
      void testGetGenericSuperclass_typeVariable_boundIsFBoundedClass() {
    assertEquals(
        new TypeToken<Enum<T>>() {},
        TypeToken.of(new TypeCapture<T>() {}.capture()).getGenericSuperclass());
    assertEquals(TypeToken.of(Object.class), new TypeToken<T[]>() {}.getGenericSuperclass());
  }

  public <T extends List<String> & CharSequence>
      void testGetGenericSuperclass_typeVariable_boundIsInterface() {
    assertNull(TypeToken.of(new TypeCapture<T>() {}.capture()).getGenericSuperclass());
    assertEquals(TypeToken.of(Object.class), new TypeToken<T[]>() {}.getGenericSuperclass());
  }

  public <T extends ArrayList<String> & CharSequence, T1 extends T>
      void testGetGenericSuperclass_typeVariable_boundIsTypeVariableAndClass() {
    assertEquals(
        TypeToken.of(new TypeCapture<T>() {}.capture()),
        TypeToken.of(new TypeCapture<T1>() {}.capture()).getGenericSuperclass());
    assertEquals(TypeToken.of(Object.class), new TypeToken<T[]>() {}.getGenericSuperclass());
  }

  public <T extends List<String> & CharSequence, T1 extends T>
      void testGetGenericSuperclass_typeVariable_boundIsTypeVariableAndInterface() {
    assertNull(TypeToken.of(new TypeCapture<T1>() {}.capture()).getGenericSuperclass());
    assertEquals(TypeToken.of(Object.class), new TypeToken<T1[]>() {}.getGenericSuperclass());
  }

  public void testGetGenericSuperclass_wildcard_lowerBounded() {
    assertEquals(
        TypeToken.of(Object.class),
        TypeToken.of(Types.supertypeOf(String.class)).getGenericSuperclass());
    assertEquals(
        new TypeToken<Object>() {},
        TypeToken.of(Types.supertypeOf(String[].class)).getGenericSuperclass());
    assertEquals(
        new TypeToken<Object>() {},
        TypeToken.of(Types.supertypeOf(CharSequence.class)).getGenericSuperclass());
  }

  public void testGetGenericSuperclass_wildcard_boundIsClass() {
    assertEquals(
        TypeToken.of(Object.class),
        TypeToken.of(Types.subtypeOf(Object.class)).getGenericSuperclass());
    assertEquals(
        new TypeToken<Object[]>() {},
        TypeToken.of(Types.subtypeOf(Object[].class)).getGenericSuperclass());
  }

  public void testGetGenericSuperclass_wildcard_boundIsInterface() {
    assertNull(TypeToken.of(Types.subtypeOf(CharSequence.class)).getGenericSuperclass());
    assertEquals(
        new TypeToken<CharSequence[]>() {},
        TypeToken.of(Types.subtypeOf(CharSequence[].class)).getGenericSuperclass());
  }

  public <T> void testGetGenericInterfaces_typeVariable_unbounded() {
    assertThat(TypeToken.of(new TypeCapture<T>() {}.capture()).getGenericInterfaces()).isEmpty();
    assertHasArrayInterfaces(new TypeToken<T[]>() {});
  }

  public <T extends NoInterface> void testGetGenericInterfaces_typeVariable_boundIsClass() {
    assertThat(TypeToken.of(new TypeCapture<T>() {}.capture()).getGenericInterfaces()).isEmpty();
    assertHasArrayInterfaces(new TypeToken<T[]>() {});
  }

  public <T extends NoInterface & Iterable<String>>
      void testGetGenericInterfaces_typeVariable_boundsAreClassWithInterface() {
    makeUnmodifiable(TypeToken.of(new TypeCapture<T>() {}.capture()).getGenericInterfaces())
        .containsExactly(new TypeToken<Iterable<String>>() {});
    assertHasArrayInterfaces(new TypeToken<T[]>() {});
  }

  public <T extends CharSequence & Iterable<String>>
      void testGetGenericInterfaces_typeVariable_boundsAreInterfaces() {
    makeUnmodifiable(TypeToken.of(new TypeCapture<T>() {}.capture()).getGenericInterfaces())
        .containsExactly(TypeToken.of(CharSequence.class), new TypeToken<Iterable<String>>() {});
    assertHasArrayInterfaces(new TypeToken<T[]>() {});
  }

  public <T extends CharSequence & Iterable<T>>
      void testGetGenericInterfaces_typeVariable_boundsAreFBoundedInterfaces() {
    makeUnmodifiable(TypeToken.of(new TypeCapture<T>() {}.capture()).getGenericInterfaces())
        .containsExactly(TypeToken.of(CharSequence.class), new TypeToken<Iterable<T>>() {});
    assertHasArrayInterfaces(new TypeToken<T[]>() {});
  }

  public <T extends Base & Iterable<T>>
      void testGetGenericInterfaces_typeVariable_boundsAreClassWithFBoundedInterface() {
    makeUnmodifiable(TypeToken.of(new TypeCapture<T>() {}.capture()).getGenericInterfaces())
        .containsExactly(new TypeToken<Iterable<T>>() {});
    assertHasArrayInterfaces(new TypeToken<T[]>() {});
  }

  public <T extends NoInterface, T1 extends T, T2 extends T1>
      void testGetGenericInterfaces_typeVariable_boundIsTypeVariableAndClass() {
    assertThat(TypeToken.of(new TypeCapture<T2>() {}.capture()).getGenericInterfaces()).isEmpty();
    assertHasArrayInterfaces(new TypeToken<T2[]>() {});
  }

  public <T extends Iterable<T>, T1 extends T, T2 extends T1>
      void testGetGenericInterfaces_typeVariable_boundIsTypeVariableAndInterface() {
    makeUnmodifiable(TypeToken.of(new TypeCapture<T2>() {}.capture()).getGenericInterfaces())
        .containsExactly(TypeToken.of(new TypeCapture<T1>() {}.capture()));
    assertHasArrayInterfaces(new TypeToken<T2[]>() {});
  }

  public void testGetGenericInterfaces_wildcard_lowerBounded() {
    assertThat(TypeToken.of(Types.supertypeOf(String.class)).getGenericInterfaces()).isEmpty();
    assertThat(TypeToken.of(Types.supertypeOf(String[].class)).getGenericInterfaces()).isEmpty();
  }

  public void testGetGenericInterfaces_wildcard_boundIsClass() {
    assertThat(TypeToken.of(Types.subtypeOf(Object.class)).getGenericInterfaces()).isEmpty();
    assertThat(TypeToken.of(Types.subtypeOf(Object[].class)).getGenericInterfaces()).isEmpty();
  }

  public void testGetGenericInterfaces_wildcard_boundIsInterface() {
    TypeToken<Iterable<String>> interfaceType = new TypeToken<Iterable<String>>() {};
    makeUnmodifiable(TypeToken.of(Types.subtypeOf(interfaceType.getType())).getGenericInterfaces())
        .containsExactly(interfaceType);
    assertHasArrayInterfaces(new TypeToken<Iterable<String>[]>() {});
  }

  public void testGetGenericInterfaces_noInterface() {
    assertThat(new TypeToken<NoInterface>() {}.getGenericInterfaces()).isEmpty();
    assertHasArrayInterfaces(new TypeToken<NoInterface[]>() {});
  }

  public void testGetGenericInterfaces_withInterfaces() {
    Map<Class<?>, Type> interfaceMap = Maps.newHashMap();
    for (TypeToken<?> interfaceType :
        new TypeToken<Implementation<Integer, String>>() {}.getGenericInterfaces()) {
      interfaceMap.put(interfaceType.getRawType(), interfaceType.getType());
    }
    assertEquals(
        ImmutableMap.of(
            Iterable.class, new TypeToken<Iterable<String>>() {}.getType(),
            Map.class, new TypeToken<Map<Integer, String>>() {}.getType()),
        interfaceMap);
  }

  private interface Interface1 {}

  private interface Interface2 {}

  private interface Interface3<T> extends Iterable<T> {}

  private interface Interface12 extends Interface1, Interface2 {}

  private static class Class1 implements Interface1 {}

  private static final class NoInterface {}

  private abstract static class Implementation<K, V> implements Iterable<V>, Map<K, V> {}

  private abstract static class First<T> {}

  private abstract static class Second<D> extends First<D> {}

  private abstract static class Third<T, D> extends Second<T> {}

  private abstract static class Fourth<T, D> extends Third<D, T> {}

  private static class ConcreteIS extends Fourth<Integer, String> {}

  private static class ConcreteSI extends Fourth<String, Integer> {}

  public void testAssignableClassToClass() {
    @SuppressWarnings("rawtypes") // To test TypeToken<List>
    TypeToken<List> tokL = new TypeToken<List>() {};
    assertTrue(tokL.isSupertypeOf(List.class));
    assertTrue(tokL.isSupertypeOf(ArrayList.class));
    assertFalse(tokL.isSupertypeOf(List[].class));

    TypeToken<Number> tokN = new TypeToken<Number>() {};
    assertTrue(tokN.isSupertypeOf(Number.class));
    assertTrue(tokN.isSupertypeOf(Integer.class));
  }

  public <T> void testAssignableParameterizedTypeToObject() {
    assertTrue(
        TypeToken.of(Object.class).isSupertypeOf(TypeToken.of(new TypeCapture<T>() {}.capture())));
    assertFalse(
        TypeToken.of(int.class).isSupertypeOf(TypeToken.of(new TypeCapture<T>() {}.capture())));
  }

  public <T, T1 extends T> void testAssignableGenericArrayToGenericArray() {
    assertTrue(new TypeToken<T[]>() {}.isSupertypeOf(new TypeToken<T[]>() {}));
    assertTrue(new TypeToken<T[]>() {}.isSupertypeOf(new TypeToken<T1[]>() {}));
    assertFalse(new TypeToken<T[]>() {}.isSupertypeOf(new TypeToken<T[][]>() {}));
  }

  public <T, T1 extends T> void testAssignableGenericArrayToClass() {
    assertTrue(TypeToken.of(Object[].class.getSuperclass()).isSupertypeOf(new TypeToken<T[]>() {}));
    for (Class<?> interfaceType : Object[].class.getInterfaces()) {
      assertTrue(TypeToken.of(interfaceType).isSupertypeOf(new TypeToken<T[]>() {}));
    }
    assertTrue(TypeToken.of(Object.class).isSupertypeOf(new TypeToken<T[]>() {}));
    assertFalse(TypeToken.of(String.class).isSupertypeOf(new TypeToken<T[]>() {}));
  }

  public void testAssignableWildcardBoundedByArrayToArrayClass() {
    Type wildcardType = Types.subtypeOf(Object[].class);
    assertTrue(TypeToken.of(Object[].class).isSupertypeOf(wildcardType));
    assertTrue(TypeToken.of(Object.class).isSupertypeOf(wildcardType));
    assertFalse(TypeToken.of(wildcardType).isSupertypeOf(wildcardType));
    assertFalse(TypeToken.of(int[].class).isSupertypeOf(wildcardType));
  }

  public void testAssignableWildcardTypeParameterToClassTypeParameter() {
    TypeToken<?> wildcardType = new TypeToken<Iterable<? extends Object[]>>() {};
    assertFalse(new TypeToken<Iterable<Object[]>>() {}.isSupertypeOf(wildcardType));
    assertFalse(new TypeToken<Iterable<Object>>() {}.isSupertypeOf(wildcardType));
    assertTrue(wildcardType.isSupertypeOf(wildcardType));
    assertFalse(new TypeToken<Iterable<int[]>>() {}.isSupertypeOf(wildcardType));
  }

  public void testAssignableArrayClassToBoundedWildcard() {
    TypeToken<?> subtypeOfArray = TypeToken.of(Types.subtypeOf(Object[].class));
    TypeToken<?> supertypeOfArray = TypeToken.of(Types.supertypeOf(Object[].class));
    assertFalse(subtypeOfArray.isSupertypeOf(Object[].class));
    assertFalse(subtypeOfArray.isSupertypeOf(Object[][].class));
    assertFalse(subtypeOfArray.isSupertypeOf(String[].class));
    assertTrue(supertypeOfArray.isSupertypeOf(Object[].class));
    assertFalse(supertypeOfArray.isSupertypeOf(Object.class));
    assertTrue(supertypeOfArray.isSupertypeOf(Object[][].class));
    assertTrue(supertypeOfArray.isSupertypeOf(String[].class));
  }

  public void testAssignableClassTypeParameterToWildcardTypeParameter() {
    TypeToken<?> subtypeOfArray = new TypeToken<Iterable<? extends Object[]>>() {};
    TypeToken<?> supertypeOfArray = new TypeToken<Iterable<? super Object[]>>() {};
    assertTrue(subtypeOfArray.isSupertypeOf(new TypeToken<Iterable<Object[]>>() {}));
    assertTrue(subtypeOfArray.isSupertypeOf(new TypeToken<Iterable<Object[][]>>() {}));
    assertTrue(subtypeOfArray.isSupertypeOf(new TypeToken<Iterable<String[]>>() {}));
    assertTrue(supertypeOfArray.isSupertypeOf(new TypeToken<Iterable<Object[]>>() {}));
    assertTrue(supertypeOfArray.isSupertypeOf(new TypeToken<Iterable<Object>>() {}));
    assertFalse(supertypeOfArray.isSupertypeOf(new TypeToken<Iterable<Object[][]>>() {}));
    assertFalse(supertypeOfArray.isSupertypeOf(new TypeToken<Iterable<String[]>>() {}));
  }

  public void testAssignableNonParameterizedClassToWildcard() {
    TypeToken<?> supertypeOfString = TypeToken.of(Types.supertypeOf(String.class));
    assertFalse(supertypeOfString.isSupertypeOf(supertypeOfString));
    assertFalse(supertypeOfString.isSupertypeOf(Object.class));
    assertFalse(supertypeOfString.isSupertypeOf(CharSequence.class));
    assertTrue(supertypeOfString.isSupertypeOf(String.class));
    assertTrue(supertypeOfString.isSupertypeOf(Types.subtypeOf(String.class)));
  }

  public void testAssignableWildcardBoundedByIntArrayToArrayClass() {
    Type wildcardType = Types.subtypeOf(int[].class);
    assertTrue(TypeToken.of(int[].class).isSupertypeOf(wildcardType));
    assertTrue(TypeToken.of(Object.class).isSupertypeOf(wildcardType));
    assertFalse(TypeToken.of(wildcardType).isSupertypeOf(wildcardType));
    assertFalse(TypeToken.of(Object[].class).isSupertypeOf(wildcardType));
  }

  public void testAssignableWildcardTypeParameterBoundedByIntArrayToArrayClassTypeParameter() {
    TypeToken<?> wildcardType = new TypeToken<Iterable<? extends int[]>>() {};
    assertFalse(new TypeToken<Iterable<int[]>>() {}.isSupertypeOf(wildcardType));
    assertFalse(new TypeToken<Iterable<Object>>() {}.isSupertypeOf(wildcardType));
    assertTrue(wildcardType.isSupertypeOf(wildcardType));
    assertFalse(new TypeToken<Iterable<Object[]>>() {}.isSupertypeOf(wildcardType));
  }

  public void testAssignableWildcardToWildcard() {
    TypeToken<?> subtypeOfArray = TypeToken.of(Types.subtypeOf(Object[].class));
    TypeToken<?> supertypeOfArray = TypeToken.of(Types.supertypeOf(Object[].class));
    assertTrue(supertypeOfArray.isSupertypeOf(subtypeOfArray));
    assertFalse(supertypeOfArray.isSupertypeOf(supertypeOfArray));
    assertFalse(subtypeOfArray.isSupertypeOf(subtypeOfArray));
    assertFalse(subtypeOfArray.isSupertypeOf(supertypeOfArray));
  }

  public void testAssignableWildcardTypeParameterToWildcardTypeParameter() {
    TypeToken<?> subtypeOfArray = new TypeToken<Iterable<? extends Object[]>>() {};
    TypeToken<?> supertypeOfArray = new TypeToken<Iterable<? super Object[]>>() {};
    assertFalse(supertypeOfArray.isSupertypeOf(subtypeOfArray));
    assertTrue(supertypeOfArray.isSupertypeOf(supertypeOfArray));
    assertTrue(subtypeOfArray.isSupertypeOf(subtypeOfArray));
    assertFalse(subtypeOfArray.isSupertypeOf(supertypeOfArray));
  }

  public <T> void testAssignableGenericArrayToArrayClass() {
    assertTrue(TypeToken.of(Object[].class).isSupertypeOf(new TypeToken<T[]>() {}));
    assertTrue(TypeToken.of(Object[].class).isSupertypeOf(new TypeToken<T[][]>() {}));
    assertTrue(TypeToken.of(Object[][].class).isSupertypeOf(new TypeToken<T[][]>() {}));
  }

  public void testAssignableParameterizedTypeToClass() {
    @SuppressWarnings("rawtypes") // Trying to test raw class
    TypeToken<List> tokL = new TypeToken<List>() {};
    assertTrue(tokL.isSupertypeOf(StringList.class));
    assertTrue(tokL.isSupertypeOf(StringList.class.getGenericInterfaces()[0]));

    @SuppressWarnings("rawtypes") // Trying to test raw class
    TypeToken<Second> tokS = new TypeToken<Second>() {};
    assertTrue(tokS.isSupertypeOf(Second.class));
    assertTrue(tokS.isSupertypeOf(Third.class.getGenericSuperclass()));
  }

  public void testAssignableArrayToClass() {
    @SuppressWarnings("rawtypes") // Trying to test raw class
    TypeToken<List[]> tokL = new TypeToken<List[]>() {};
    assertTrue(tokL.isSupertypeOf(List[].class));
    assertFalse(tokL.isSupertypeOf(List.class));

    @SuppressWarnings("rawtypes") // Trying to test raw class
    TypeToken<Second[]> tokS = new TypeToken<Second[]>() {};
    assertTrue(tokS.isSupertypeOf(Second[].class));
    assertTrue(tokS.isSupertypeOf(Third[].class));
  }

  @SuppressWarnings("rawtypes") // Trying to test raw class
  public void testAssignableTokenToClass() {
    TypeToken<List> tokL = new TypeToken<List>() {};
    assertTrue(tokL.isSupertypeOf(new TypeToken<List>() {}));
    assertTrue(tokL.isSupertypeOf(new TypeToken<List<String>>() {}));
    assertTrue(tokL.isSupertypeOf(new TypeToken<List<?>>() {}));

    TypeToken<Second> tokS = new TypeToken<Second>() {};
    assertTrue(tokS.isSupertypeOf(new TypeToken<Second>() {}));
    assertTrue(tokS.isSupertypeOf(new TypeToken<Third>() {}));
    assertTrue(tokS.isSupertypeOf(new TypeToken<Third<String, Integer>>() {}));

    TypeToken<List[]> tokA = new TypeToken<List[]>() {};
    assertTrue(tokA.isSupertypeOf(new TypeToken<List[]>() {}));
    assertTrue(tokA.isSupertypeOf(new TypeToken<List<String>[]>() {}));
    assertTrue(tokA.isSupertypeOf(new TypeToken<List<?>[]>() {}));
  }

  public void testAssignableClassToType() {
    TypeToken<List<String>> tokenL = new TypeToken<List<String>>() {};
    assertTrue(tokenL.isSupertypeOf(StringList.class));
    assertFalse(tokenL.isSupertypeOf(List.class));

    TypeToken<First<String>> tokenF = new TypeToken<First<String>>() {};
    assertTrue(tokenF.isSupertypeOf(ConcreteIS.class));
    assertFalse(tokenF.isSupertypeOf(ConcreteSI.class));
  }

  public void testAssignableClassToArrayType() {
    TypeToken<List<String>[]> tokenL = new TypeToken<List<String>[]>() {};
    assertTrue(tokenL.isSupertypeOf(StringList[].class));
    assertFalse(tokenL.isSupertypeOf(List[].class));
  }

  public void testAssignableParameterizedTypeToType() {
    TypeToken<List<String>> tokenL = new TypeToken<List<String>>() {};
    assertTrue(tokenL.isSupertypeOf(StringList.class.getGenericInterfaces()[0]));
    assertFalse(tokenL.isSupertypeOf(IntegerList.class.getGenericInterfaces()[0]));

    TypeToken<First<String>> tokenF = new TypeToken<First<String>>() {};
    assertTrue(tokenF.isSupertypeOf(ConcreteIS.class.getGenericSuperclass()));
    assertFalse(tokenF.isSupertypeOf(ConcreteSI.class.getGenericSuperclass()));
  }

  public void testGenericArrayTypeToArrayType() {
    TypeToken<List<String>[]> tokL = new TypeToken<List<String>[]>() {};
    TypeToken<ArrayList<String>[]> token = new TypeToken<ArrayList<String>[]>() {};
    assertTrue(tokL.isSupertypeOf(tokL.getType()));
    assertTrue(tokL.isSupertypeOf(token.getType()));
  }

  public void testAssignableTokenToType() {
    TypeToken<List<String>> tokenL = new TypeToken<List<String>>() {};
    assertTrue(tokenL.isSupertypeOf(new TypeToken<List<String>>() {}));
    assertTrue(tokenL.isSupertypeOf(new TypeToken<ArrayList<String>>() {}));
    assertTrue(tokenL.isSupertypeOf(new TypeToken<StringList>() {}));

    TypeToken<First<String>> tokenF = new TypeToken<First<String>>() {};
    assertTrue(tokenF.isSupertypeOf(new TypeToken<Second<String>>() {}));
    assertTrue(tokenF.isSupertypeOf(new TypeToken<Third<String, Integer>>() {}));
    assertFalse(tokenF.isSupertypeOf(new TypeToken<Third<Integer, String>>() {}));
    assertTrue(tokenF.isSupertypeOf(new TypeToken<Fourth<Integer, String>>() {}));
    assertFalse(tokenF.isSupertypeOf(new TypeToken<Fourth<String, Integer>>() {}));
    assertTrue(tokenF.isSupertypeOf(new TypeToken<ConcreteIS>() {}));
    assertFalse(tokenF.isSupertypeOf(new TypeToken<ConcreteSI>() {}));
  }

  public void testAssignableWithWildcards() {
    TypeToken<?> unboundedToken = new TypeToken<List<?>>() {};
    TypeToken<?> upperBoundToken = new TypeToken<List<? extends Number>>() {};
    TypeToken<?> lowerBoundToken = new TypeToken<List<? super Number>>() {};
    TypeToken<?> concreteToken = new TypeToken<List<Number>>() {};
    TypeToken<?> subtypeToken = new TypeToken<List<Integer>>() {};
    TypeToken<?> supertypeToken = new TypeToken<List<Serializable>>() {};
    List<TypeToken<?>> allTokens =
        ImmutableList.of(
            unboundedToken,
            upperBoundToken,
            lowerBoundToken,
            concreteToken,
            subtypeToken,
            supertypeToken);

    for (TypeToken<?> typeToken : allTokens) {
      assertTrue(typeToken.toString(), unboundedToken.isSupertypeOf(typeToken));
    }

    assertFalse(upperBoundToken.isSupertypeOf(unboundedToken));
    assertTrue(upperBoundToken.isSupertypeOf(upperBoundToken));
    assertFalse(upperBoundToken.isSupertypeOf(lowerBoundToken));
    assertTrue(upperBoundToken.isSupertypeOf(concreteToken));
    assertTrue(upperBoundToken.isSupertypeOf(subtypeToken));
    assertFalse(upperBoundToken.isSupertypeOf(supertypeToken));

    assertFalse(lowerBoundToken.isSupertypeOf(unboundedToken));
    assertFalse(lowerBoundToken.isSupertypeOf(upperBoundToken));
    assertTrue(lowerBoundToken.isSupertypeOf(lowerBoundToken));
    assertTrue(lowerBoundToken.isSupertypeOf(concreteToken));
    assertFalse(lowerBoundToken.isSupertypeOf(subtypeToken));
    assertTrue(lowerBoundToken.isSupertypeOf(supertypeToken));

    for (TypeToken<?> typeToken : allTokens) {
      assertEquals(
          typeToken.toString(), typeToken == concreteToken, concreteToken.isSupertypeOf(typeToken));
    }

    for (TypeToken<?> typeToken : allTokens) {
      assertEquals(
          typeToken.toString(), typeToken == subtypeToken, subtypeToken.isSupertypeOf(typeToken));
    }

    for (TypeToken<?> typeToken : allTokens) {
      assertEquals(
          typeToken.toString(),
          typeToken == supertypeToken,
          supertypeToken.isSupertypeOf(typeToken));
    }
  }

  public <N1 extends Number, N2 extends Number, N11 extends N1>
      void testisSupertypeOf_typeVariable() {
    assertAssignable(
        TypeToken.of(new TypeCapture<N1>() {}.capture()),
        TypeToken.of(new TypeCapture<N1>() {}.capture()));
    assertNotAssignable(new TypeToken<List<N11>>() {}, new TypeToken<List<N1>>() {});
    assertNotAssignable(
        new TypeToken<Number>() {}, TypeToken.of(new TypeCapture<N1>() {}.capture()));
    assertAssignable(
        TypeToken.of(new TypeCapture<N11>() {}.capture()),
        TypeToken.of(new TypeCapture<N1>() {}.capture()));
    assertNotAssignable(
        TypeToken.of(new TypeCapture<N2>() {}.capture()),
        TypeToken.of(new TypeCapture<N1>() {}.capture()));
  }

  public <N1 extends Number, N2 extends Number, N11 extends N1>
      void testisSupertypeOf_equalWildcardTypes() {
    assertAssignable(
        new TypeToken<List<? extends N1>>() {}, new TypeToken<List<? extends N1>>() {});
    assertAssignable(new TypeToken<List<? super N1>>() {}, new TypeToken<List<? super N1>>() {});
    assertAssignable(
        new TypeToken<List<? extends Number>>() {}, new TypeToken<List<? extends Number>>() {});
    assertAssignable(
        new TypeToken<List<? super Number>>() {}, new TypeToken<List<? super Number>>() {});
  }

  public <N> void testisSupertypeOf_wildcard_noBound() {
    assertAssignable(new TypeToken<List<? super N>>() {}, new TypeToken<List<?>>() {});
    assertAssignable(new TypeToken<List<N>>() {}, new TypeToken<List<?>>() {});
  }

  public <N1 extends Number, N2 extends Number, N11 extends N1>
      void testisSupertypeOf_wildcardType_upperBoundMatch() {
    // ? extends T
    assertAssignable(new TypeToken<List<N11>>() {}, new TypeToken<List<? extends N1>>() {});
    assertNotAssignable(new TypeToken<List<N1>>() {}, new TypeToken<List<? extends N11>>() {});
    assertNotAssignable(new TypeToken<List<Number>>() {}, new TypeToken<List<? extends N11>>() {});

    // ? extends Number
    assertAssignable(new TypeToken<List<N1>>() {}, new TypeToken<List<? extends Number>>() {});
    assertAssignable(new TypeToken<ArrayList<N1>>() {}, new TypeToken<List<? extends Number>>() {});
    assertAssignable(
        new TypeToken<List<? extends N11>>() {}, new TypeToken<List<? extends Number>>() {});
  }

  public <N1 extends Number, N2 extends Number, N11 extends N1>
      void testisSupertypeOf_wildcardType_lowerBoundMatch() {
    // ? super T
    assertAssignable(new TypeToken<List<N1>>() {}, new TypeToken<List<? super N11>>() {});
    assertAssignable(new TypeToken<ArrayList<Number>>() {}, new TypeToken<List<? super N1>>() {});
    assertNotAssignable(
        new TypeToken<ArrayList<? super N11>>() {}, new TypeToken<List<? super Number>>() {});
    assertAssignable(
        new TypeToken<ArrayList<? super N1>>() {}, new TypeToken<List<? super N11>>() {});
    assertAssignable(
        new TypeToken<ArrayList<? super Number>>() {}, new TypeToken<List<? super N11>>() {});

    // ? super Number
    assertNotAssignable(
        new TypeToken<ArrayList<N11>>() {}, new TypeToken<List<? super Number>>() {});
    assertAssignable(
        new TypeToken<ArrayList<Number>>() {}, new TypeToken<List<? super Number>>() {});
    assertAssignable(
        new TypeToken<ArrayList<Object>>() {}, new TypeToken<List<? super Number>>() {});
  }

  public <L extends List<R>, R extends List<L>>
      void testisSupertypeOf_recursiveTypeVariableBounds() {
    assertAssignable(
        TypeToken.of(new TypeCapture<L>() {}.capture()),
        TypeToken.of(new TypeCapture<L>() {}.capture()));
    assertNotAssignable(
        TypeToken.of(new TypeCapture<R>() {}.capture()),
        TypeToken.of(new TypeCapture<L>() {}.capture()));
    assertAssignable(TypeToken.of(new TypeCapture<L>() {}.capture()), new TypeToken<List<R>>() {});
  }

  public void testisSupertypeOf_resolved() {
    assertFalse(Assignability.of().isAssignable());
    assertTrue(new Assignability<Integer, Integer>() {}.isAssignable());
    assertTrue(new Assignability<Integer, Object>() {}.isAssignable());
    assertFalse(new Assignability<Integer, String>() {}.isAssignable());
    TypeTokenTest.<Number, Integer>assignabilityTestWithTypeVariables();
  }

  public <From extends String & List<? extends String>> void testMultipleTypeBoundsAssignability() {
    assertTrue(new Assignability<From, String>() {}.isAssignable());
    assertFalse(new Assignability<From, Number>() {}.isAssignable());
    assertTrue(new Assignability<From, Iterable<? extends CharSequence>>() {}.isAssignable());
    assertFalse(new Assignability<From, Iterable<Object>>() {}.isAssignable());
  }

  private static <N1 extends Number, N11 extends N1> void assignabilityTestWithTypeVariables() {
    assertTrue(new Assignability<N11, N1>() {}.isAssignable());
    assertTrue(new Assignability<N11, Number>() {}.isAssignable());
    assertFalse(new Assignability<Number, N11>() {}.isAssignable());
  }

  public void testIsArray_arrayClasses() {
    assertTrue(TypeToken.of(Object[].class).isArray());
    assertTrue(TypeToken.of(Object[][].class).isArray());
    assertTrue(TypeToken.of(char[].class).isArray());
    assertTrue(TypeToken.of(char[][].class).isArray());
    assertTrue(TypeToken.of(byte[].class).isArray());
    assertTrue(TypeToken.of(short[].class).isArray());
    assertTrue(TypeToken.of(int[].class).isArray());
    assertTrue(TypeToken.of(long[].class).isArray());
    assertTrue(TypeToken.of(float[].class).isArray());
    assertTrue(TypeToken.of(double[].class).isArray());
    assertFalse(TypeToken.of(Object.class).isArray());
    assertFalse(TypeToken.of(void.class).isArray());
  }

  public <T> void testIsArray_genericArrayClasses() {
    assertFalse(TypeToken.of(new TypeCapture<T>() {}.capture()).isArray());
    assertTrue(new TypeToken<T[]>() {}.isArray());
    assertTrue(new TypeToken<T[][]>() {}.isArray());
  }

  public void testIsArray_wildcardType() {
    assertTrue(TypeToken.of(Types.subtypeOf(Object[].class)).isArray());
    assertTrue(TypeToken.of(Types.subtypeOf(int[].class)).isArray());
    assertFalse(TypeToken.of(Types.subtypeOf(Object.class)).isArray());
    assertFalse(TypeToken.of(Types.supertypeOf(Object[].class)).isArray());
  }

  public <T extends Integer> void testPrimitiveWrappingAndUnwrapping() {
    for (Class<?> type : Primitives.allPrimitiveTypes()) {
      assertIsPrimitive(TypeToken.of(type));
    }
    for (Class<?> type : Primitives.allWrapperTypes()) {
      assertIsWrapper(TypeToken.of(type));
    }
    assertNotPrimitiveNorWrapper(TypeToken.of(String.class));
    assertNotPrimitiveNorWrapper(TypeToken.of(Object[].class));
    assertNotPrimitiveNorWrapper(TypeToken.of(Types.subtypeOf(Object.class)));
    assertNotPrimitiveNorWrapper(new TypeToken<List<String>>() {});
    assertNotPrimitiveNorWrapper(TypeToken.of(new TypeCapture<T>() {}.capture()));
  }

  public void testGetComponentType_arrayClasses() {
    assertEquals(Object.class, TypeToken.of(Object[].class).getComponentType().getType());
    assertEquals(Object[].class, TypeToken.of(Object[][].class).getComponentType().getType());
    assertEquals(char.class, TypeToken.of(char[].class).getComponentType().getType());
    assertEquals(char[].class, TypeToken.of(char[][].class).getComponentType().getType());
    assertEquals(byte.class, TypeToken.of(byte[].class).getComponentType().getType());
    assertEquals(short.class, TypeToken.of(short[].class).getComponentType().getType());
    assertEquals(int.class, TypeToken.of(int[].class).getComponentType().getType());
    assertEquals(long.class, TypeToken.of(long[].class).getComponentType().getType());
    assertEquals(float.class, TypeToken.of(float[].class).getComponentType().getType());
    assertEquals(double.class, TypeToken.of(double[].class).getComponentType().getType());
    assertNull(TypeToken.of(Object.class).getComponentType());
    assertNull(TypeToken.of(void.class).getComponentType());
  }

  public <T> void testGetComponentType_genericArrayClasses() {
    assertNull(TypeToken.of(new TypeCapture<T>() {}.capture()).getComponentType());
    assertEquals(
        TypeToken.of(new TypeCapture<T>() {}.capture()),
        new TypeToken<T[]>() {}.getComponentType());
    assertEquals(new TypeToken<T[]>() {}, new TypeToken<T[][]>() {}.getComponentType());
  }

  public void testGetComponentType_wildcardType() {
    assertEquals(
        Types.subtypeOf(Object.class),
        TypeToken.of(Types.subtypeOf(Object[].class)).getComponentType().getType());
    assertEquals(
        Types.subtypeOf(Object[].class),
        Types.newArrayType(
            TypeToken.of(Types.subtypeOf(Object[].class)).getComponentType().getType()));
    assertEquals(
        int.class, TypeToken.of(Types.subtypeOf(int[].class)).getComponentType().getType());
    assertNull(TypeToken.of(Types.subtypeOf(Object.class)).getComponentType());
    assertNull(TypeToken.of(Types.supertypeOf(Object[].class)).getComponentType());
  }

  private interface NumberList<T extends Number> {}

  public void testImplicitUpperBoundForWildcards() {
    assertAssignable(
        new TypeToken<NumberList<? extends Number>>() {}, new TypeToken<NumberList<?>>() {});
    assertAssignable(
        new TypeToken<NumberList<? super Integer>>() {}, new TypeToken<NumberList<?>>() {});
  }

  public <T extends Readable & Appendable> void testMultiBound() {
    assertAssignable(new TypeToken<List<T>>() {}, new TypeToken<List<? extends Readable>>() {});
    assertAssignable(new TypeToken<List<T>>() {}, new TypeToken<List<? extends Appendable>>() {});
  }

  public void testToGenericType() {
    assertEquals(TypeToken.of(String.class), TypeToken.toGenericType(String.class));
    assertEquals(new TypeToken<int[]>() {}, TypeToken.toGenericType(int[].class));
    @SuppressWarnings("rawtypes") // Iterable.class
    TypeToken<? extends Iterable> genericType = TypeToken.toGenericType(Iterable.class);
    assertEquals(Iterable.class, genericType.getRawType());
    assertEquals(
        Types.newParameterizedType(Iterable.class, Iterable.class.getTypeParameters()[0]),
        genericType.getType());
  }

  public void testToGenericType_staticMemberClass() throws Exception {
    Method getStaticAnonymousClassMethod =
        TypeTokenTest.class.getDeclaredMethod("getStaticAnonymousClass", Object.class);
    ParameterizedType javacReturnType =
        (ParameterizedType) getStaticAnonymousClassMethod.getGenericReturnType();

    ParameterizedType parameterizedType =
        (ParameterizedType) TypeToken.toGenericType(GenericClass.class).getType();
    assertThat(parameterizedType.getOwnerType()).isEqualTo(javacReturnType.getOwnerType());
  }

  public static <T> GenericClass<T> getStaticAnonymousClass(final T value) {
    return new GenericClass<T>() {
      @SuppressWarnings("unused")
      public T innerValue = value;
    };
  }

  private interface ListIterable<T> extends Iterable<List<T>> {}

  private interface StringListIterable extends ListIterable<String> {}

  private interface ListArrayIterable<T> extends Iterable<List<T>[]> {}

  private interface StringListArrayIterable extends ListIterable<String> {}

  public void testGetSupertype_withTypeVariable() {
    ParameterizedType expectedType =
        Types.newParameterizedType(
            Iterable.class,
            Types.newParameterizedType(List.class, ListIterable.class.getTypeParameters()[0]));
    assertEquals(
        expectedType, TypeToken.of(ListIterable.class).getSupertype(Iterable.class).getType());
  }

  public <A, T extends Number & Iterable<A>>
      void testGetSupertype_typeVariableWithMultipleBounds() {
    assertEquals(
        Number.class, new TypeToken<T>(getClass()) {}.getSupertype(Number.class).getType());
    assertEquals(
        new TypeToken<Iterable<A>>() {},
        new TypeToken<T>(getClass()) {}.getSupertype(Iterable.class));
  }

  public void testGetSupertype_withoutTypeVariable() {
    ParameterizedType expectedType =
        Types.newParameterizedType(
            Iterable.class, Types.newParameterizedType(List.class, String.class));
    assertEquals(
        expectedType,
        TypeToken.of(StringListIterable.class).getSupertype(Iterable.class).getType());
  }

  public void testGetSupertype_chained() {
    @SuppressWarnings("unchecked") // StringListIterable extensd ListIterable<String>
    TypeToken<ListIterable<String>> listIterableType =
        (TypeToken<ListIterable<String>>)
            TypeToken.of(StringListIterable.class).getSupertype(ListIterable.class);
    ParameterizedType expectedType =
        Types.newParameterizedType(
            Iterable.class, Types.newParameterizedType(List.class, String.class));
    assertEquals(expectedType, listIterableType.getSupertype(Iterable.class).getType());
  }

  public void testGetSupertype_withArray() {
    assertEquals(
        new TypeToken<Iterable<List<String>>[]>() {},
        TypeToken.of(StringListIterable[].class).getSupertype(Iterable[].class));
    assertEquals(int[].class, TypeToken.of(int[].class).getSupertype(int[].class).getType());
    assertEquals(Object.class, TypeToken.of(int[].class).getSupertype(Object.class).getType());
    assertEquals(int[][].class, TypeToken.of(int[][].class).getSupertype(int[][].class).getType());
    assertEquals(
        Object[].class, TypeToken.of(String[].class).getSupertype(Object[].class).getType());
    assertEquals(Object.class, TypeToken.of(String[].class).getSupertype(Object.class).getType());
  }

  public void testGetSupertype_fromWildcard() {
    @SuppressWarnings("unchecked") // can't do new TypeToken<? extends ...>() {}
    TypeToken<? extends List<String>> type =
        (TypeToken<? extends List<String>>)
            TypeToken.of(Types.subtypeOf(new TypeToken<List<String>>() {}.getType()));
    assertEquals(new TypeToken<Iterable<String>>() {}, type.getSupertype(Iterable.class));
  }

  public <T extends Iterable<String>> void testGetSupertype_fromTypeVariable() {
    @SuppressWarnings("unchecked") // to construct TypeToken<T> from TypeToken.of()
    TypeToken<T> typeVariableToken = (TypeToken<T>) TypeToken.of(new TypeCapture<T>() {}.capture());
    assertEquals(
        new TypeToken<Iterable<String>>() {}, typeVariableToken.getSupertype(Iterable.class));
  }

  @SuppressWarnings("rawtypes") // purpose is to test raw type
  public void testGetSupertype_fromRawClass() {
    assertEquals(
        Types.newParameterizedType(Iterable.class, List.class.getTypeParameters()[0]),
        new TypeToken<List>() {}.getSupertype(Iterable.class).getType());
  }

  @SuppressWarnings({"rawtypes", "unchecked"}) // purpose is to test raw type
  public void testGetSupertype_notSupertype() {
    try {
      new TypeToken<List<String>>() {}.getSupertype((Class) String.class);
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }

  public void testGetSupertype_fromArray() {
    assertEquals(
        new TypeToken<Iterable<String>[]>() {},
        new TypeToken<List<String>[]>() {}.getSupertype(Iterable[].class));
  }

  private interface ListMap<K, V> extends Map<K, List<V>> {}

  public void testGetSupertype_fullyGenericType() {
    ParameterizedType expectedType =
        Types.newParameterizedType(
            Map.class,
            ListMap.class.getTypeParameters()[0],
            Types.newParameterizedType(List.class, ListMap.class.getTypeParameters()[1]));
    assertEquals(expectedType, TypeToken.of(ListMap.class).getSupertype(Map.class).getType());
  }

  public void testGetSupertype_fullySpecializedType() {
    Type expectedType = new TypeToken<Map<String, List<Object>>>() {}.getType();
    assertEquals(
        expectedType,
        new TypeToken<ListMap<String, Object>>() {}.getSupertype(Map.class).getType());
  }

  private interface StringListMap<V> extends ListMap<String, V> {}

  public <V> void testGetSupertype_partiallySpecializedType() {
    Type expectedType = new TypeToken<Map<String, List<V>>>() {}.getType();
    assertEquals(
        expectedType, new TypeToken<StringListMap<V>>() {}.getSupertype(Map.class).getType());
  }

  public void testGetSubtype_withTypeVariable() {
    assertEquals(
        new TypeToken<ListIterable<String>>() {},
        new TypeToken<Iterable<List<String>>>() {}.getSubtype(ListIterable.class));
    assertEquals(
        new TypeToken<ListArrayIterable<String>>() {},
        new TypeToken<Iterable<List<String>[]>>() {}.getSubtype(ListArrayIterable.class));
    assertEquals(
        new TypeToken<ListArrayIterable<String>[]>() {},
        new TypeToken<Iterable<List<String>[]>[]>() {}.getSubtype(ListArrayIterable[].class));
  }

  public void testGetSubtype_withoutTypeVariable() {
    assertEquals(
        StringListIterable.class,
        TypeToken.of(Iterable.class).getSubtype(StringListIterable.class).getType());
    assertEquals(
        StringListIterable[].class,
        TypeToken.of(Iterable[].class).getSubtype(StringListIterable[].class).getType());
    assertEquals(
        TypeToken.of(StringListArrayIterable.class),
        new TypeToken<Iterable<List<String>>>() {}.getSubtype(StringListArrayIterable.class));
    assertEquals(
        TypeToken.of(StringListArrayIterable[].class),
        new TypeToken<Iterable<List<String>>[]>() {}.getSubtype(StringListArrayIterable[].class));
  }

  public void testGetSubtype_withArray() {
    assertEquals(
        TypeToken.of(StringListIterable[].class),
        TypeToken.of(Iterable[].class).getSubtype(StringListIterable[].class));
    assertEquals(
        TypeToken.of(String[].class), TypeToken.of(Object[].class).getSubtype(String[].class));
    assertEquals(TypeToken.of(int[].class), TypeToken.of(Object.class).getSubtype(int[].class));
  }

  public void testGetSubtype_fromWildcard() {
    @SuppressWarnings("unchecked") // can't do new TypeToken<? extends ...>() {}
    TypeToken<? super Iterable<String>> type =
        (TypeToken<? super Iterable<String>>)
            TypeToken.of(Types.supertypeOf(new TypeToken<Iterable<String>>() {}.getType()));
    assertEquals(new TypeToken<List<String>>() {}, type.getSubtype(List.class));
  }

  public void testGetSubtype_fromWildcard_lowerBoundNotSupertype() {
    @SuppressWarnings("unchecked") // can't do new TypeToken<? extends ...>() {}
    TypeToken<? super Iterable<String>> type =
        (TypeToken<? super Iterable<String>>)
            TypeToken.of(Types.supertypeOf(new TypeToken<ImmutableList<String>>() {}.getType()));
    try {
      type.getSubtype(List.class);
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }

  public void testGetSubtype_fromWildcard_upperBounded() {
    @SuppressWarnings("unchecked") // can't do new TypeToken<? extends ...>() {}
    TypeToken<? extends Iterable<String>> type =
        (TypeToken<? extends Iterable<String>>)
            TypeToken.of(Types.subtypeOf(new TypeToken<Iterable<String>>() {}.getType()));
    try {
      type.getSubtype(Iterable.class);
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }

  public <T extends Iterable<String>> void testGetSubtype_fromTypeVariable() {
    try {
      TypeToken.of(new TypeCapture<T>() {}.capture()).getSubtype(List.class);
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }

  @SuppressWarnings("rawtypes") // purpose is to test raw type
  public void testGetSubtype_fromRawClass() {
    assertEquals(List.class, new TypeToken<Iterable>() {}.getSubtype(List.class).getType());
  }

  public void testGetSubtype_fromArray() {
    assertEquals(
        new TypeToken<List<String>[]>() {},
        new TypeToken<Iterable<String>[]>() {}.getSubtype(List[].class));
  }

  public void testGetSubtype_toWildcard() {
    class TwoTypeArgs<K, V> {}
    class StringForFirstTypeArg<V> extends TwoTypeArgs<String, V> {}
    TypeToken<TwoTypeArgs<?, ?>> supertype = new TypeToken<TwoTypeArgs<?, ?>>() {};
    TypeToken<StringForFirstTypeArg<String>> subtype =
        new TypeToken<StringForFirstTypeArg<String>>() {};
    assertTrue(subtype.isSubtypeOf(supertype));
    assertEquals(
        new TypeToken<StringForFirstTypeArg<?>>() {}, supertype.getSubtype(subtype.getRawType()));
  }

  private static class TwoTypeArgs<K, V> {
    class InnerType<K2, V2> {}
  }

  private static class StringForFirstTypeArg<V> extends TwoTypeArgs<String, V> {
    class StringInnerType<V2> extends InnerType<String, V2> {}
  }

  public void testGetSubtype_innerTypeOfGenericClassTranslatesOwnerTypeVars() {
    TypeToken<TwoTypeArgs<?, ?>.InnerType<?, ?>> supertype =
        new TypeToken<TwoTypeArgs<?, ?>.InnerType<?, ?>>() {};
    TypeToken<StringForFirstTypeArg<Integer>.StringInnerType<Long>> subtype =
        new TypeToken<StringForFirstTypeArg<Integer>.StringInnerType<Long>>() {};
    assertTrue(subtype.isSubtypeOf(supertype));
    ParameterizedType actualSubtype =
        (ParameterizedType) supertype.getSubtype(subtype.getRawType()).getType();
    assertEquals(StringForFirstTypeArg.StringInnerType.class, actualSubtype.getRawType());
    assertThat(actualSubtype.getActualTypeArguments()[0]).isInstanceOf(WildcardType.class);
    ParameterizedType actualOwnerType = (ParameterizedType) actualSubtype.getOwnerType();
    assertEquals(StringForFirstTypeArg.class, actualOwnerType.getRawType());
  }

  public void testGetSubtype_outerTypeVarTranslatesInnerTypeVar() {
    class TwoTypeArgs<K, V> {}
    class StringForFirstTypeArg<V> extends TwoTypeArgs<String, V> {}
    class OuterTypeVar<V> extends StringForFirstTypeArg<List<V>> {}
    TypeToken<StringForFirstTypeArg<List<?>>> type =
        new TypeToken<StringForFirstTypeArg<List<?>>>() {};
    assertEquals(new TypeToken<OuterTypeVar<?>>() {}, type.getSubtype(OuterTypeVar.class));
  }

  public void testGetSubtype_toWildcardWithBounds() {
    class TwoTypeArgs<K, V> {}
    class StringForFirstTypeArg<V> extends TwoTypeArgs<String, V> {}
    TypeToken<TwoTypeArgs<?, ? extends Number>> supertype =
        new TypeToken<TwoTypeArgs<?, ? extends Number>>() {};
    TypeToken<StringForFirstTypeArg<Integer>> subtype =
        new TypeToken<StringForFirstTypeArg<Integer>>() {};
    assertTrue(subtype.isSubtypeOf(supertype));

    // TODO(benyu): This should check equality to an expected value, see discussion in cl/98674873
    TypeToken<?> unused = supertype.getSubtype(subtype.getRawType());
  }

  public void testGetSubtype_baseClassWithNoTypeArgs() {
    class SingleGenericExtendsBase<T> extends Base {}
    TypeToken<Base> supertype = new TypeToken<Base>() {};
    TypeToken<SingleGenericExtendsBase<String>> subtype =
        new TypeToken<SingleGenericExtendsBase<String>>() {};
    assertTrue(subtype.isSubtypeOf(supertype));
    ParameterizedType actualSubtype =
        (ParameterizedType) supertype.getSubtype(subtype.getRawType()).getType();
    assertEquals(SingleGenericExtendsBase.class, actualSubtype.getRawType());
  }

  public void testGetSubtype_baseClassInGenericClassWithNoTypeArgs() {
    class SingleGenericExtendsBase<T> implements GenericClass.Base {}
    TypeToken<GenericClass.Base> supertype = new TypeToken<GenericClass.Base>() {};
    TypeToken<SingleGenericExtendsBase<String>> subtype =
        new TypeToken<SingleGenericExtendsBase<String>>() {};
    assertTrue(subtype.isSubtypeOf(supertype));
    ParameterizedType actualSubtype =
        (ParameterizedType) supertype.getSubtype(subtype.getRawType()).getType();
    assertEquals(SingleGenericExtendsBase.class, actualSubtype.getRawType());
    assertTrue(TypeToken.of(actualSubtype).isSubtypeOf(supertype));
  }

  public void testGetSubtype_genericSubtypeOfNonGenericType() {
    TypeToken<Serializable> supertype = new TypeToken<Serializable>() {};
    TypeToken<ArrayList<String>> subtype = new TypeToken<ArrayList<String>>() {};
    assertTrue(subtype.isSubtypeOf(supertype));
    ParameterizedType actualSubtype =
        (ParameterizedType) supertype.getSubtype(subtype.getRawType()).getType();
    assertEquals(ArrayList.class, actualSubtype.getRawType());
    assertThat(actualSubtype.getActualTypeArguments()[0]).isInstanceOf(TypeVariable.class);
    assertTrue(TypeToken.of(actualSubtype).isSubtypeOf(supertype));
  }

  private interface MySpecialList<E, F> extends List<E> {}

  public void testGetSubtype_genericSubtypeOfGenericTypeWithFewerParameters() {
    TypeToken<List<String>> supertype = new TypeToken<List<String>>() {};
    TypeToken<MySpecialList<String, ?>> subtype = new TypeToken<MySpecialList<String, ?>>() {};
    assertTrue(subtype.isSubtypeOf(supertype));
    ParameterizedType actualSubtype =
        (ParameterizedType) supertype.getSubtype(subtype.getRawType()).getType();
    assertEquals(MySpecialList.class, actualSubtype.getRawType());
    assertThat(actualSubtype.getActualTypeArguments()[0]).isEqualTo(String.class);
    assertThat(actualSubtype.getActualTypeArguments()[1]).isInstanceOf(TypeVariable.class);
    assertTrue(TypeToken.of(actualSubtype).isSubtypeOf(supertype));
  }

  public void testGetSubtype_genericSubtypeOfRawTypeWithFewerTypeParameters() {
    TypeToken<List> supertype = new TypeToken<List>() {};
    TypeToken<MySpecialList> subtype = new TypeToken<MySpecialList>() {};
    assertTrue(subtype.isSubtypeOf(supertype));
    Class<?> actualSubtype = (Class<?>) supertype.getSubtype(subtype.getRawType()).getType();
    assertEquals(MySpecialList.class, actualSubtype);
    assertTrue(TypeToken.of(actualSubtype).isSubtypeOf(supertype));
  }

  public void testGetSubtype_baseClassWithLessTypeArgs() {
    class SingleGenericExtendsBase<T> extends Base {}
    class DoubleGenericExtendsSingleGeneric<T1, TUnused> extends SingleGenericExtendsBase<T1> {}
    TypeToken<SingleGenericExtendsBase<?>> supertype =
        new TypeToken<SingleGenericExtendsBase<?>>() {};
    TypeToken<DoubleGenericExtendsSingleGeneric<String, Integer>> subtype =
        new TypeToken<DoubleGenericExtendsSingleGeneric<String, Integer>>() {};
    assertTrue(subtype.isSubtypeOf(supertype));
    ParameterizedType actualSubtype =
        (ParameterizedType) supertype.getSubtype(subtype.getRawType()).getType();
    assertEquals(DoubleGenericExtendsSingleGeneric.class, actualSubtype.getRawType());
    assertThat(actualSubtype.getActualTypeArguments()[0]).isInstanceOf(WildcardType.class);
  }

  public <T> void testGetSubtype_manyGenericArgs() {
    class FourTypeArgs<T1, T2, T3, T4> {}
    class ThreeTypeArgs<T1, T2, T3> extends FourTypeArgs<T1, T2, T3, String> {}
    TypeToken<FourTypeArgs<T, Integer, ?, ?>> supertype =
        new TypeToken<FourTypeArgs<T, Integer, ?, ?>>() {};
    TypeToken<ThreeTypeArgs<T, Integer, String>> subtype =
        new TypeToken<ThreeTypeArgs<T, Integer, String>>() {};
    assertTrue(subtype.isSubtypeOf(supertype));
    assertEquals(
        new TypeToken<ThreeTypeArgs<T, Integer, ?>>() {},
        supertype.getSubtype(subtype.getRawType()));
  }

  public void testGetSubtype_recursiveTypeBoundInSubtypeTranslatedAsIs() {
    class BaseWithTypeVar<T> {}
    class Outer<O> {
      class Sub<X> extends BaseWithTypeVar<List<X>> {}

      class Sub2<Y extends Sub2<Y>> extends BaseWithTypeVar<List<Y>> {}
    }
    ParameterizedType subtype =
        (ParameterizedType)
            new TypeToken<BaseWithTypeVar<List<?>>>() {}.getSubtype(Outer.Sub.class).getType();
    assertEquals(Outer.Sub.class, subtype.getRawType());
    assertThat(subtype.getActualTypeArguments()[0]).isInstanceOf(WildcardType.class);
    ParameterizedType owner = (ParameterizedType) subtype.getOwnerType();
    assertEquals(Outer.class, owner.getRawType());
    // This returns a strange ? extends Sub2<Y> type, which isn't ideal.
    TypeToken<?> unused = new TypeToken<BaseWithTypeVar<List<?>>>() {}.getSubtype(Outer.Sub2.class);
  }

  public void testGetSubtype_subtypeSameAsDeclaringType() throws Exception {
    class Bar<T> {}
    class SubBar<T> extends Bar<T> {
      @SuppressWarnings("unused")
      Bar<T> delegate;

      TypeToken<SubBar<T>> fieldTypeAsSubBar() {
        return new TypeToken<SubBar<T>>() {};
      }
    }

    Field delegateField = SubBar.class.getDeclaredField("delegate");
    // barType is Bar<T>, a ParameterizedType with no generic arguments specified
    TypeToken<?> barType = TypeToken.of(delegateField.getGenericType());
    assertThat(barType.getSubtype(SubBar.class)).isEqualTo(new SubBar<Void>().fieldTypeAsSubBar());
  }

  @SuppressWarnings("unchecked") // To construct TypeToken<T> with TypeToken.of()
  public <T> void testWhere_circleRejected() {
    TypeToken<List<T>> type = new TypeToken<List<T>>() {};
    try {
      type.where(
          new TypeParameter<T>() {},
          (TypeToken<T>) TypeToken.of(new TypeCapture<T>() {}.capture()));
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }

  public void testWhere() {
    assertEquals(new TypeToken<Map<String, Integer>>() {}, mapOf(String.class, Integer.class));
    assertEquals(new TypeToken<int[]>() {}, arrayOf(int.class));
    assertEquals(int[].class, arrayOf(int.class).getRawType());
  }

  @SuppressWarnings("unused") // used by reflection
  private static class Holder<T> {
    List<T>[] matrix;

    void setList(List<T> list) {}
  }

  public void testWildcardCaptured_methodParameter_upperBound() throws Exception {
    TypeToken<Holder<?>> type = new TypeToken<Holder<?>>() {};
    ImmutableList<Parameter> parameters =
        type.method(Holder.class.getDeclaredMethod("setList", List.class)).getParameters();
    assertThat(parameters).hasSize(1);
    TypeToken<?> parameterType = parameters.get(0).getType();
    assertEquals(List.class, parameterType.getRawType());
    assertFalse(
        parameterType.getType().toString(),
        parameterType.isSupertypeOf(new TypeToken<List<Integer>>() {}));
  }

  public void testWildcardCaptured_field_upperBound() throws Exception {
    TypeToken<Holder<?>> type = new TypeToken<Holder<?>>() {};
    TypeToken<?> matrixType =
        type.resolveType(Holder.class.getDeclaredField("matrix").getGenericType());
    assertEquals(List[].class, matrixType.getRawType());
    assertThat(matrixType.getType()).isNotEqualTo(new TypeToken<List<?>[]>() {}.getType());
  }

  public void testWildcardCaptured_wildcardWithImplicitBound() throws Exception {
    TypeToken<Holder<?>> type = new TypeToken<Holder<?>>() {};
    ImmutableList<Parameter> parameters =
        type.method(Holder.class.getDeclaredMethod("setList", List.class)).getParameters();
    assertThat(parameters).hasSize(1);
    TypeToken<?> parameterType = parameters.get(0).getType();
    Type[] typeArgs = ((ParameterizedType) parameterType.getType()).getActualTypeArguments();
    assertThat(typeArgs).asList().hasSize(1);
    TypeVariable<?> captured = (TypeVariable<?>) typeArgs[0];
    assertThat(captured.getBounds()).asList().containsExactly(Object.class);
    assertThat(new TypeToken<List<?>>() {}.isSupertypeOf(parameterType)).isTrue();
  }

  public void testWildcardCaptured_wildcardWithExplicitBound() throws Exception {
    TypeToken<Holder<? extends Number>> type = new TypeToken<Holder<? extends Number>>() {};
    ImmutableList<Parameter> parameters =
        type.method(Holder.class.getDeclaredMethod("setList", List.class)).getParameters();
    assertThat(parameters).hasSize(1);
    TypeToken<?> parameterType = parameters.get(0).getType();
    Type[] typeArgs = ((ParameterizedType) parameterType.getType()).getActualTypeArguments();
    assertThat(typeArgs).asList().hasSize(1);
    TypeVariable<?> captured = (TypeVariable<?>) typeArgs[0];
    assertThat(captured.getBounds()).asList().containsExactly(Number.class);
    assertThat(new TypeToken<List<? extends Number>>() {}.isSupertypeOf(parameterType)).isTrue();
  }

  private static class Counter<N extends Number> {
    @SuppressWarnings("unused") // used by reflection
    List<N> counts;
  }

  public void testWildcardCaptured_typeVariableDeclaresTypeBound_wildcardHasNoExplicitUpperBound()
      throws Exception {
    TypeToken<Counter<?>> type = new TypeToken<Counter<?>>() {};
    TypeToken<?> fieldType =
        type.resolveType(Counter.class.getDeclaredField("counts").getGenericType());
    Type[] typeArgs = ((ParameterizedType) fieldType.getType()).getActualTypeArguments();
    assertThat(typeArgs).asList().hasSize(1);
    TypeVariable<?> captured = (TypeVariable<?>) typeArgs[0];
    assertThat(captured.getBounds()).asList().containsExactly(Number.class);
    assertThat(new TypeToken<List<? extends Number>>() {}.isSupertypeOf(fieldType)).isTrue();
    assertThat(new TypeToken<List<? extends Iterable<?>>>() {}.isSupertypeOf(fieldType)).isFalse();
  }

  public void testWildcardCaptured_typeVariableDeclaresTypeBound_wildcardHasExplicitUpperBound()
      throws Exception {
    TypeToken<Counter<? extends Number>> type = new TypeToken<Counter<? extends Number>>() {};
    TypeToken<?> fieldType =
        type.resolveType(Counter.class.getDeclaredField("counts").getGenericType());
    Type[] typeArgs = ((ParameterizedType) fieldType.getType()).getActualTypeArguments();
    assertThat(typeArgs).asList().hasSize(1);
    TypeVariable<?> captured = (TypeVariable<?>) typeArgs[0];
    assertThat(captured.getBounds()).asList().containsExactly(Number.class);
    assertThat(new TypeToken<List<? extends Number>>() {}.isSupertypeOf(fieldType)).isTrue();
    assertThat(new TypeToken<List<? extends Iterable<?>>>() {}.isSupertypeOf(fieldType)).isFalse();
  }

  public void testWildcardCaptured_typeVariableDeclaresTypeBound_wildcardAddsNewUpperBound()
      throws Exception {
    TypeToken<Counter<? extends Iterable<?>>> type =
        new TypeToken<Counter<? extends Iterable<?>>>() {};
    TypeToken<?> fieldType =
        type.resolveType(Counter.class.getDeclaredField("counts").getGenericType());
    Type[] typeArgs = ((ParameterizedType) fieldType.getType()).getActualTypeArguments();
    assertThat(typeArgs).asList().hasSize(1);
    TypeVariable<?> captured = (TypeVariable<?>) typeArgs[0];
    assertThat(captured.getBounds()).asList().contains(Number.class);
    assertThat(captured.getBounds())
        .asList()
        .containsNoneOf(Object.class, new TypeToken<Iterable<Number>>() {}.getType());
    assertThat(new TypeToken<List<? extends Number>>() {}.isSupertypeOf(fieldType)).isTrue();
    assertThat(new TypeToken<List<? extends Iterable<?>>>() {}.isSupertypeOf(fieldType)).isTrue();
    assertThat(new TypeToken<List<? extends Iterable<Number>>>() {}.isSupertypeOf(fieldType))
        .isFalse();
  }

  public void testArrayClassPreserved() {
    assertEquals(int[].class, TypeToken.of(int[].class).getType());
    assertEquals(int[][].class, TypeToken.of(int[][].class).getType());
    assertEquals(String[].class, TypeToken.of(String[].class).getType());
    assertEquals(Integer.class, new TypeToken<Integer>() {}.getType());
    assertEquals(Integer.class, TypeToken.of(Integer.class).getType());
  }

  public void testMethod_getOwnerType() throws NoSuchMethodException {
    Method sizeMethod = List.class.getMethod("size");
    assertEquals(
        TypeToken.of(List.class), TypeToken.of(List.class).method(sizeMethod).getOwnerType());
    assertEquals(
        new TypeToken<List<String>>() {},
        new TypeToken<List<String>>() {}.method(sizeMethod).getOwnerType());
  }

  public void testMethod_notDeclaredByType() throws NoSuchMethodException {
    Method sizeMethod = Map.class.getMethod("size");
    try {
      TypeToken.of(List.class).method(sizeMethod);
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }

  public void testMethod_declaredBySuperclass() throws Exception {
    Method toStringMethod = Object.class.getMethod("toString");
    ImmutableList<String> list = ImmutableList.of("foo");
    assertEquals(list.toString(), TypeToken.of(List.class).method(toStringMethod).invoke(list));
  }

  public <T extends Number & List<String>> void testMethod_returnType_resolvedAgainstTypeBound()
      throws NoSuchMethodException {
    Method getMethod = List.class.getMethod("get", int.class);
    Invokable<T, String> invokable =
        new TypeToken<T>(getClass()) {}.method(getMethod).returning(String.class);
    assertEquals(TypeToken.of(String.class), invokable.getReturnType());
  }

  public <T extends List<String>> void testMethod_parameterTypes() throws NoSuchMethodException {
    Method setMethod = List.class.getMethod("set", int.class, Object.class);
    Invokable<T, ?> invokable = new TypeToken<T>(getClass()) {}.method(setMethod);
    ImmutableList<Parameter> params = invokable.getParameters();
    assertEquals(2, params.size());
    assertEquals(TypeToken.of(int.class), params.get(0).getType());
    assertEquals(TypeToken.of(String.class), params.get(1).getType());
  }

  public void testMethod_equals() throws NoSuchMethodException {
    Method getMethod = List.class.getMethod("get", int.class);
    Method setMethod = List.class.getMethod("set", int.class, Object.class);
    new EqualsTester()
        .addEqualityGroup(Invokable.from(getMethod), Invokable.from(getMethod))
        .addEqualityGroup(Invokable.from(setMethod))
        .addEqualityGroup(new TypeToken<List<Integer>>() {}.method(getMethod))
        .addEqualityGroup(new TypeToken<List<String>>() {}.method(getMethod))
        .addEqualityGroup(new TypeToken<List<Integer>>() {}.method(setMethod))
        .addEqualityGroup(new TypeToken<List<String>>() {}.method(setMethod))
        .testEquals();
  }

  private interface Loser<E extends Throwable> {
    void lose() throws E;
  }

  public <T extends Loser<AssertionError>> void testMethod_exceptionTypes()
      throws NoSuchMethodException {
    Method failMethod = Loser.class.getMethod("lose");
    Invokable<T, ?> invokable = new TypeToken<T>(getClass()) {}.method(failMethod);
    assertThat(invokable.getExceptionTypes()).contains(TypeToken.of(AssertionError.class));
  }

  public void testConstructor_getOwnerType() throws NoSuchMethodException {
    @SuppressWarnings("rawtypes") // raw class ArrayList.class
    Constructor<ArrayList> constructor = ArrayList.class.getConstructor();
    assertEquals(
        TypeToken.of(ArrayList.class),
        TypeToken.of(ArrayList.class).constructor(constructor).getOwnerType());
    assertEquals(
        new TypeToken<ArrayList<String>>() {},
        new TypeToken<ArrayList<String>>() {}.constructor(constructor).getOwnerType());
  }

  public void testConstructor_notDeclaredByType() throws NoSuchMethodException {
    Constructor<String> constructor = String.class.getConstructor();
    try {
      TypeToken.of(Object.class).constructor(constructor);
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }

  public void testConstructor_declaredBySuperclass() throws NoSuchMethodException {
    Constructor<Object> constructor = Object.class.getConstructor();
    try {
      TypeToken.of(String.class).constructor(constructor);
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }

  public void testConstructor_equals() throws NoSuchMethodException {
    Constructor<?> defaultConstructor = ArrayList.class.getConstructor();
    Constructor<?> oneArgConstructor = ArrayList.class.getConstructor(int.class);
    new EqualsTester()
        .addEqualityGroup(Invokable.from(defaultConstructor), Invokable.from(defaultConstructor))
        .addEqualityGroup(Invokable.from(oneArgConstructor))
        .addEqualityGroup(new TypeToken<ArrayList<Integer>>() {}.constructor(defaultConstructor))
        .addEqualityGroup(new TypeToken<ArrayList<String>>() {}.constructor(defaultConstructor))
        .addEqualityGroup(new TypeToken<ArrayList<Integer>>() {}.constructor(oneArgConstructor))
        .addEqualityGroup(new TypeToken<ArrayList<String>>() {}.constructor(oneArgConstructor))
        .testEquals();
  }

  private static class Container<T> {
    @SuppressWarnings("unused")
    public Container(T data) {}
  }

  public <T extends Container<String>> void testConstructor_parameterTypes()
      throws NoSuchMethodException {
    @SuppressWarnings("rawtypes") // Reflection API skew
    Constructor<Container> constructor = Container.class.getConstructor(Object.class);
    Invokable<T, ?> invokable = new TypeToken<T>(getClass()) {}.constructor(constructor);
    ImmutableList<Parameter> params = invokable.getParameters();
    assertEquals(1, params.size());
    assertEquals(TypeToken.of(String.class), params.get(0).getType());
  }

  private static class CannotConstruct<E extends Throwable> {
    @SuppressWarnings("unused")
    public CannotConstruct() throws E {}
  }

  public <T extends CannotConstruct<AssertionError>> void testConstructor_exceptionTypes()
      throws NoSuchMethodException {
    @SuppressWarnings("rawtypes") // Reflection API skew
    Constructor<CannotConstruct> constructor = CannotConstruct.class.getConstructor();
    Invokable<T, ?> invokable = new TypeToken<T>(getClass()) {}.constructor(constructor);
    assertThat(invokable.getExceptionTypes()).contains(TypeToken.of(AssertionError.class));
  }

  public void testRejectTypeVariable_class() {
    assertNoTypeVariable(String.class);
    assertNoTypeVariable(String[].class);
    assertNoTypeVariable(int[].class);
  }

  public void testRejectTypeVariable_parameterizedType() {
    assertNoTypeVariable(new TypeCapture<Iterable<String>>() {}.capture());
  }

  public void testRejectTypeVariable_wildcardType() {
    assertNoTypeVariable(new TypeCapture<Iterable<? extends String>>() {}.capture());
    assertNoTypeVariable(new TypeCapture<Iterable<? super String>>() {}.capture());
  }

  public void testRejectTypeVariable_genericArrayType() {
    assertNoTypeVariable(new TypeCapture<Iterable<? extends String>[]>() {}.capture());
  }

  public <T> void testRejectTypeVariable_withTypeVariable() {
    assertHasTypeVariable(new TypeCapture<T>() {}.capture());
    assertHasTypeVariable(new TypeCapture<T[]>() {}.capture());
    assertHasTypeVariable(new TypeCapture<Iterable<T>>() {}.capture());
    assertHasTypeVariable(new TypeCapture<Map<String, T>>() {}.capture());
    assertHasTypeVariable(new TypeCapture<Map<String, ? extends T>>() {}.capture());
    assertHasTypeVariable(new TypeCapture<Map<String, ? super T[]>>() {}.capture());
  }

  private static class From<K> {
    class To<V> {
      Type type() {
        return new TypeToken<To<V>>(getClass()) {}.getType();
      }
    }
  }

  public <T> void testRejectTypeVariable_withOwnerType() {
    // Neither has subclass
    assertHasTypeVariable(new From<Integer>().new To<String>().type());
    assertHasTypeVariable(new From<T>().new To<String>().type());
    assertHasTypeVariable(new From<Integer>().new To<T>().type());

    // Owner is subclassed
    assertHasTypeVariable(new From<Integer>() {}.new To<String>().type());
    assertHasTypeVariable(new From<T>() {}.new To<String>().type());

    // Inner is subclassed
    assertNoTypeVariable(new From<Integer>().new To<String>() {}.type());
    assertHasTypeVariable(new From<Integer>().new To<T>() {}.type());
    assertHasTypeVariable(new From<T>().new To<String>() {}.type());

    // both subclassed
    assertHasTypeVariable(new From<T>() {}.new To<String>() {}.type());
    assertNoTypeVariable(new From<Integer>() {}.new To<String>() {}.type());
    assertHasTypeVariable(new From<Integer>() {}.new To<T>() {}.type());
  }

  private static void assertHasTypeVariable(Type type) {
    try {
      TypeToken.of(type).rejectTypeVariables();
      fail("Should contain TypeVariable");
    } catch (IllegalArgumentException expected) {
    }
  }

  private static void assertNoTypeVariable(Type type) {
    TypeToken.of(type).rejectTypeVariables();
  }

  private abstract static class RawTypeConsistencyTester<T extends Enum<T> & CharSequence> {
    abstract T returningT();

    abstract void acceptT(T t);

    abstract <X extends T> X returningX();

    abstract <X> void acceptX(X x);

    abstract <T2 extends Enum<T2> & CharSequence> T2 returningT2();

    abstract <T2 extends CharSequence & Iterable<T2>> void acceptT2(T2 t2);

    static void verifyConsitentRawType() {
      for (Method method : RawTypeConsistencyTester.class.getDeclaredMethods()) {
        assertEquals(
            method.getReturnType(), TypeToken.of(method.getGenericReturnType()).getRawType());
        for (int i = 0; i < method.getParameterTypes().length; i++) {
          assertEquals(
              method.getParameterTypes()[i],
              TypeToken.of(method.getGenericParameterTypes()[i]).getRawType());
        }
      }
    }
  }

  public void testRawTypes() {
    RawTypeConsistencyTester.verifyConsitentRawType();
    assertEquals(Object.class, TypeToken.of(Types.subtypeOf(Object.class)).getRawType());
    assertEquals(
        CharSequence.class, TypeToken.of(Types.subtypeOf(CharSequence.class)).getRawType());
    assertEquals(Object.class, TypeToken.of(Types.supertypeOf(CharSequence.class)).getRawType());
  }

  private abstract static class IKnowMyType<T> {
    TypeToken<T> type() {
      return new TypeToken<T>(getClass()) {};
    }
  }

  public void testTypeResolution() {
    assertEquals(String.class, new IKnowMyType<String>() {}.type().getType());
    assertEquals(
        new TypeToken<Map<String, Integer>>() {},
        new IKnowMyType<Map<String, Integer>>() {}.type());
  }

  public <A extends Iterable<? extends String>, B extends A> void testSerializable() {
    reserialize(TypeToken.of(String.class));
    reserialize(TypeToken.of(String.class).getTypes());
    reserialize(TypeToken.of(String.class).getTypes().classes());
    reserialize(TypeToken.of(String.class).getTypes().interfaces());
    reserialize(TypeToken.of(String.class).getTypes().rawTypes());
    reserialize(TypeToken.of(String.class).getTypes().classes().rawTypes());
    reserialize(TypeToken.of(String.class).getTypes().interfaces().rawTypes());
    reserialize(new TypeToken<int[]>() {});
    reserialize(new TypeToken<Map<String, Integer>>() {});
    reserialize(new IKnowMyType<Map<? super String, ? extends int[]>>() {}.type());
    reserialize(TypeToken.of(new TypeCapture<B>() {}.capture()).getTypes().rawTypes());
    try {
      SerializableTester.reserialize(TypeToken.of(new TypeCapture<B>() {}.capture()));
      fail();
    } catch (RuntimeException expected) {
    }
  }

  public <A> void testSerializable_typeVariableNotSupported() {
    try {
      new ITryToSerializeMyTypeVariable<String>().go();
      fail();
    } catch (RuntimeException expected) {
    }
  }

  private static class ITryToSerializeMyTypeVariable<T> {
    void go() {
      SerializableTester.reserialize(TypeToken.of(new TypeCapture<T>() {}.capture()));
    }
  }

  @CanIgnoreReturnValue
  private static <T> T reserialize(T object) {
    T copy = SerializableTester.reserialize(object);
    new EqualsTester().addEqualityGroup(object, copy).testEquals();
    return copy;
  }

  public void testTypeResolutionAfterReserialized() {
    reserialize(new TypeToken<String>() {});
    reserialize(new TypeToken<Map<String, Integer>>() {});
    TypeToken<Map<String, Integer>> reserialized =
        reserialize(new TypeToken<Map<String, Integer>>() {});
    assertEquals(reserialized, substitute(reserialized, String.class));
  }

  private static <T, X> TypeToken<T> substitute(TypeToken<T> type, Class<X> arg) {
    return type.where(new TypeParameter<X>() {}, arg);
  }

  private abstract static class ToReproduceGenericSignatureFormatError<V> {
    private abstract class BaseOuter {
      abstract class BaseInner {}
    }

    private abstract class SubOuter extends BaseOuter {
      private abstract class SubInner extends BaseInner {}
    }
  }

  // For Guava bug http://code.google.com/p/guava-libraries/issues/detail?id=1025
  public void testDespiteGenericSignatureFormatError() {
    ImmutableSet<?> unused =
        ImmutableSet.copyOf(
            TypeToken.of(ToReproduceGenericSignatureFormatError.SubOuter.SubInner.class)
                .getTypes()
                .rawTypes());
  }

  private abstract static class Entry<K, V> {
    TypeToken<K> keyType() {
      return new TypeToken<K>(getClass()) {};
    }

    TypeToken<V> valueType() {
      return new TypeToken<V>(getClass()) {};
    }
  }

  // The A and B type parameters are used inside the test to test type variable
  public <A, B> void testEquals() {
    new EqualsTester()
        .addEqualityGroup(
            TypeToken.of(String.class),
            TypeToken.of(String.class),
            new Entry<String, Integer>() {}.keyType(),
            new Entry<Integer, String>() {}.valueType(),
            new TypeToken<String>() {},
            new TypeToken<String>() {})
        .addEqualityGroup(
            TypeToken.of(Integer.class),
            new TypeToken<Integer>() {},
            new Entry<Integer, String>() {}.keyType(),
            new Entry<String, Integer>() {}.valueType())
        .addEqualityGroup(new TypeToken<List<String>>() {}, new TypeToken<List<String>>() {})
        .addEqualityGroup(new TypeToken<List<?>>() {}, new TypeToken<List<?>>() {})
        .addEqualityGroup(new TypeToken<Map<A, ?>>() {}, new TypeToken<Map<A, ?>>() {})
        .addEqualityGroup(new TypeToken<Map<B, ?>>() {})
        .addEqualityGroup(
            TypeToken.of(new TypeCapture<A>() {}.capture()),
            TypeToken.of(new TypeCapture<A>() {}.capture()))
        .addEqualityGroup(TypeToken.of(new TypeCapture<B>() {}.capture()))
        .testEquals();
  }

  // T is used inside to test type variable
  public <T> void testToString() {
    assertEquals(String.class.getName(), new TypeToken<String>() {}.toString());
    assertEquals("T", TypeToken.of(new TypeCapture<T>() {}.capture()).toString());
    assertEquals("java.lang.String", new Entry<String, Integer>() {}.keyType().toString());
  }

  private static <K, V> TypeToken<Map<K, V>> mapOf(Class<K> keyType, Class<V> valueType) {
    return new TypeToken<Map<K, V>>() {}.where(new TypeParameter<K>() {}, keyType)
        .where(new TypeParameter<V>() {}, valueType);
  }

  private static <T> TypeToken<T[]> arrayOf(Class<T> componentType) {
    return new TypeToken<T[]>() {}.where(new TypeParameter<T>() {}, componentType);
  }

  public <T> void testNulls() {
    new NullPointerTester().testAllPublicStaticMethods(TypeToken.class);
    new NullPointerTester()
        .setDefault(TypeParameter.class, new TypeParameter<T>() {})
        .testAllPublicInstanceMethods(TypeToken.of(String.class));
  }

  private static class Assignability<From, To> {

    boolean isAssignable() {
      return new TypeToken<To>(getClass()) {}.isSupertypeOf(new TypeToken<From>(getClass()) {});
    }

    static <From, To> Assignability<From, To> of() {
      return new Assignability<>();
    }
  }

  private static void assertAssignable(TypeToken<?> from, TypeToken<?> to) {
    assertTrue(
        from.getType() + " is expected to be assignable to " + to.getType(),
        to.isSupertypeOf(from));
    assertTrue(
        to.getType() + " is expected to be a supertype of " + from.getType(),
        to.isSupertypeOf(from));
    assertTrue(
        from.getType() + " is expected to be a subtype of " + to.getType(), from.isSubtypeOf(to));
  }

  private static void assertNotAssignable(TypeToken<?> from, TypeToken<?> to) {
    assertFalse(
        from.getType() + " shouldn't be assignable to " + to.getType(), to.isSupertypeOf(from));
    assertFalse(
        to.getType() + " shouldn't be a supertype of " + from.getType(), to.isSupertypeOf(from));
    assertFalse(
        from.getType() + " shouldn't be a subtype of " + to.getType(), from.isSubtypeOf(to));
  }

  private static void assertHasArrayInterfaces(TypeToken<?> arrayType) {
    assertEquals(arrayInterfaces(), ImmutableSet.copyOf(arrayType.getGenericInterfaces()));
  }

  private static ImmutableSet<TypeToken<?>> arrayInterfaces() {
    ImmutableSet.Builder<TypeToken<?>> builder = ImmutableSet.builder();
    for (Class<?> interfaceType : Object[].class.getInterfaces()) {
      builder.add(TypeToken.of(interfaceType));
    }
    return builder.build();
  }

  private static void assertIsPrimitive(TypeToken<?> type) {
    assertTrue(type.isPrimitive());
    assertNotWrapper(type);
    assertEquals(TypeToken.of(Primitives.wrap((Class<?>) type.getType())), type.wrap());
  }

  private static void assertNotPrimitive(TypeToken<?> type) {
    assertFalse(type.isPrimitive());
    assertSame(type, type.wrap());
  }

  private static void assertIsWrapper(TypeToken<?> type) {
    assertNotPrimitive(type);
    assertEquals(TypeToken.of(Primitives.unwrap((Class<?>) type.getType())), type.unwrap());
  }

  private static void assertNotWrapper(TypeToken<?> type) {
    assertSame(type, type.unwrap());
  }

  private static void assertNotPrimitiveNorWrapper(TypeToken<?> type) {
    assertNotPrimitive(type);
    assertNotWrapper(type);
  }

  private interface BaseInterface {}

  private static class Base implements BaseInterface {}

  private static class Sub extends Base {}

  private static class GenericClass<T> {
    private interface Base {}
  }

  private static IterableSubject makeUnmodifiable(Collection<?> actual) {
    return assertThat(Collections.<Object>unmodifiableCollection(actual));
  }
}
