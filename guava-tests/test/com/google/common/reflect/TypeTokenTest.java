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

import static org.truth0.Truth.ASSERT;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.primitives.Primitives;
import com.google.common.testing.EqualsTester;
import com.google.common.testing.NullPointerTester;
import com.google.common.testing.SerializableTester;

import junit.framework.TestCase;

import org.truth0.subjects.CollectionSubject;

import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Test cases for {@link TypeToken}.
 *
 * @author Sven Mawson
 * @author Ben Yu
 */
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
    } catch (IllegalStateException expected) {}
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
    assertEquals(Types.newParameterizedType(Local.class, String.class),
        type.getType());
    assertEquals(new Local<String>() {}.getClass().getGenericSuperclass(), type.getType());
  }

  public void testStaticLocalClass() {
    doTestStaticLocalClass();
  }

  private static void doTestStaticLocalClass() {
    class Local<T> {}
    TypeToken<Local<String>> type = new TypeToken<Local<String>>() {};
    assertEquals(Types.newParameterizedType(Local.class, String.class),
        type.getType());
    assertEquals(new Local<String>() {}.getClass().getGenericSuperclass(), type.getType());
  }

  public void testGenericArrayType() throws Exception {
    TypeToken<List<String>[]> token = new TypeToken<List<String>[]>() {};
    assertEquals(List[].class, token.getRawType());
    assertTrue(token.getType() instanceof GenericArrayType);
  }

  public void testMultiDimensionalGenericArrayType() throws Exception {
    TypeToken<List<Long>[][][]> token = new TypeToken<List<Long>[][][]>() {};
    assertEquals(List[][][].class, token.getRawType());
    assertTrue(token.getType() instanceof GenericArrayType);
  }

  public <T> void testGenericVariableTypeArrays() throws Exception {
    assertEquals("T[]", new TypeToken<T[]>() {}.toString());
  }

  public void testResolveType() throws Exception {
    Method getFromList = List.class.getMethod("get", int.class);
    TypeToken<?> returnType = new TypeToken<List<String>>() {}
        .resolveType(getFromList.getGenericReturnType());
    assertEquals(String.class, returnType.getType());
  }

  public <F extends Enum<F> & Function<String, Integer> & Iterable<Long>>
  void testResolveType_fromTypeVariable() throws Exception {
    TypeToken<?> f = TypeToken.of(new TypeCapture<F>() {}.capture());
    assertEquals(String.class,
        f.resolveType(Function.class.getTypeParameters()[0]).getType());
    assertEquals(Integer.class,
        f.resolveType(Function.class.getTypeParameters()[1]).getType());
    assertEquals(Long.class,
        f.resolveType(Iterable.class.getTypeParameters()[0]).getType());
  }

  public <E extends Comparable<Iterable<String>> & Iterable<Integer>>
  void testResolveType_fromTypeVariable_onlyDirectBoundsAreUsed() throws Exception {
    TypeToken<?> e = TypeToken.of(new TypeCapture<E>() {}.capture());
    assertEquals(Integer.class,
        e.resolveType(Iterable.class.getTypeParameters()[0]).getType());
  }

  public void testResolveType_fromWildcard() throws Exception {
    ParameterizedType withWildcardType = (ParameterizedType)
        new TypeCapture<Comparable<? extends Iterable<String>>>() {}.capture();
    TypeToken<?> wildcardType = TypeToken.of(withWildcardType.getActualTypeArguments()[0]);
    assertEquals(String.class,
        wildcardType.resolveType(Iterable.class.getTypeParameters()[0]).getType());
  }

  public void testGetTypes_noSuperclass() {
    TypeToken<Object>.TypeSet types = new TypeToken<Object>() {}.getTypes();
    ASSERT.that(types).has().item(TypeToken.of(Object.class));
    ASSERT.that(types.rawTypes()).has().item(Object.class);
    ASSERT.that(types.interfaces()).isEmpty();
    ASSERT.that(types.interfaces().rawTypes()).isEmpty();
    ASSERT.that(types.classes()).has().item(TypeToken.of(Object.class));
    ASSERT.that(types.classes().rawTypes()).has().item(Object.class);
  }

  public void testGetTypes_fromInterface() {
    TypeToken<Interface1>.TypeSet types = new TypeToken<Interface1>() {}.getTypes();
    ASSERT.that(types).has().item(TypeToken.of(Interface1.class));
    ASSERT.that(types.rawTypes()).has().item(Interface1.class);
    ASSERT.that(types.interfaces()).has().item(TypeToken.of(Interface1.class));
    ASSERT.that(types.interfaces().rawTypes()).has().item(Interface1.class);
    ASSERT.that(types.classes()).isEmpty();
    ASSERT.that(types.classes().rawTypes()).isEmpty();
  }

  public void testGetTypes_fromPrimitive() {
    TypeToken<Integer>.TypeSet types = TypeToken.of(int.class).getTypes();
    ASSERT.that(types).has().item(TypeToken.of(int.class));
    ASSERT.that(types.rawTypes()).has().item(int.class);
    ASSERT.that(types.interfaces()).isEmpty();
    ASSERT.that(types.interfaces().rawTypes()).isEmpty();
    ASSERT.that(types.classes()).has().item(TypeToken.of(int.class));
    ASSERT.that(types.classes().rawTypes()).has().item(int.class);
  }

  public void testGetTypes_withInterfacesAndSuperclasses() {
    abstract class Class2 extends Class1 implements Interface12 {}
    abstract class Class3<T> extends Class2 implements Interface3<T> {}
    TypeToken<Class3<String>>.TypeSet types = new TypeToken<Class3<String>>() {}.getTypes();
    assertThat(types).has().exactly(
        new TypeToken<Class3<String>>() {},
        new TypeToken<Interface3<String>>() {},
        new TypeToken<Iterable<String>>() {},
        TypeToken.of(Class2.class),
        TypeToken.of(Interface12.class),
        TypeToken.of(Interface1.class),
        TypeToken.of(Interface2.class),
        TypeToken.of(Class1.class),
        TypeToken.of(Object.class));
    assertThat(types.interfaces()).has().exactly(
        new TypeToken<Interface3<String>>() {},
        TypeToken.of(Interface12.class),
        TypeToken.of(Interface1.class),
        TypeToken.of(Interface2.class),
        new TypeToken<Iterable<String>>() {});
    assertThat(types.classes()).has().exactly(
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
    assertThat(types.rawTypes()).has().exactly(
        Class3.class, Interface3.class,
        Iterable.class,
        Class2.class,
        Interface12.class,
        Interface1.class,
        Interface2.class,
        Class1.class,
        Object.class);
    assertThat(types.interfaces().rawTypes()).has().exactly(
        Interface3.class,
        Interface12.class,
        Interface1.class,
        Interface2.class,
        Iterable.class);
    assertThat(types.classes().rawTypes()).has().exactly(
        Class3.class,
        Class2.class,
        Class1.class,
        Object.class);
    assertSubtypeFirst(types);
  }

  public <A extends Class1 & Interface1, B extends A>
  void testGetTypes_ignoresTypeVariablesByDefault() {
    TypeToken<?>.TypeSet types = TypeToken.of(new TypeCapture<B>() {}.capture()).getTypes();
    assertThat(types).has().exactly(
        TypeToken.of(Interface1.class), TypeToken.of(Class1.class),
        TypeToken.of(Object.class));
    assertSubtypeFirst(types);
    assertThat(types.interfaces())
        .has().exactly(TypeToken.of(Interface1.class))
        .inOrder();
    assertThat(types.classes())
        .has().exactly(TypeToken.of(Class1.class), TypeToken.of(Object.class))
        .inOrder();
  }

  public <A extends Class1 & Interface1, B extends A>
  void testGetTypes_rawTypes_ignoresTypeVariablesByDefault() {
    TypeToken<?>.TypeSet types = TypeToken.of(new TypeCapture<B>() {}.capture()).getTypes();
    assertThat(types.rawTypes())
        .has().exactly(Interface1.class, Class1.class, Object.class);
    assertThat(types.interfaces().rawTypes())
        .has().exactly(Interface1.class)
        .inOrder();
    assertThat(types.classes().rawTypes())
        .has().exactly(Class1.class, Object.class)
        .inOrder();
  }

  public <A extends Interface1 & Interface2 & Interface3<String>>
  void testGetTypes_manyBounds() {
    TypeToken<?>.TypeSet types = TypeToken.of(new TypeCapture<A>() {}.capture()).getTypes();
    assertThat(types.rawTypes())
        .has().exactly(Interface1.class, Interface2.class, Interface3.class, Iterable.class);
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
        if (left.isAssignableFrom(right)) {
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
    assertSubtypeBeforeSupertype(
        ImmutableList.of(String.class, CharSequence.class));
  }

  public void testAssertSubtypeBeforeSupertype_supertypeFirst() {
    try {
      assertSubtypeBeforeSupertype(
          ImmutableList.of(CharSequence.class, String.class));
    } catch (AssertionError expected) {
      return;
    }
    fail();
  }

  public void testAssertSubtypeBeforeSupertype_duplicate() {
    try {
      assertSubtypeBeforeSupertype(
          ImmutableList.of(String.class, String.class));
    } catch (AssertionError expected) {
      return;
    }
    fail();
  }

  public void testGetGenericSuperclass_noSuperclass() {
    assertNull(new TypeToken<Object>() {}.getGenericSuperclass());
    assertEquals(TypeToken.of(Object.class),
        new TypeToken<Object[]>() {}.getGenericSuperclass());
    assertNull(new TypeToken<List<String>>() {}.getGenericSuperclass());
    assertEquals(TypeToken.of(Object.class),
        new TypeToken<List<String>[]>() {}.getGenericSuperclass());
  }

  public void testGetGenericSuperclass_withSuperclass() {
    TypeToken<? super ArrayList<String>> superToken =
        new TypeToken<ArrayList<String>>() {}.getGenericSuperclass();
    assertEquals(ArrayList.class.getSuperclass(), superToken.getRawType());
    assertEquals(String.class,
        ((ParameterizedType) superToken.getType()).getActualTypeArguments()[0]);
    assertEquals(TypeToken.of(Base.class), TypeToken.of(Sub.class).getGenericSuperclass());
    assertEquals(TypeToken.of(Object.class), TypeToken.of(Sub[].class).getGenericSuperclass());
  }

  public <T> void testGetGenericSuperclass_typeVariable_unbounded() {
    assertEquals(TypeToken.of(Object.class),
        TypeToken.of(new TypeCapture<T>() {}.capture()).getGenericSuperclass());
    assertEquals(TypeToken.of(Object.class), new TypeToken<T[]>() {}.getGenericSuperclass());
  }

  public <T extends ArrayList<String> & CharSequence>
  void testGetGenericSuperclass_typeVariable_boundIsClass() {
    assertEquals(new TypeToken<ArrayList<String>>() {},
        TypeToken.of(new TypeCapture<T>() {}.capture()).getGenericSuperclass());
    assertEquals(TypeToken.of(Object.class), new TypeToken<T[]>() {}.getGenericSuperclass());
  }

  public <T extends Enum<T> & CharSequence>
  void testGetGenericSuperclass_typeVariable_boundIsFBoundedClass() {
    assertEquals(new TypeToken<Enum<T>>() {},
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
    assertEquals(TypeToken.of(new TypeCapture<T>() {}.capture()),
        TypeToken.of(new TypeCapture<T1>() {}.capture()).getGenericSuperclass());
    assertEquals(TypeToken.of(Object.class), new TypeToken<T[]>() {}.getGenericSuperclass());
  }

  public <T extends List<String> & CharSequence, T1 extends T>
  void testGetGenericSuperclass_typeVariable_boundIsTypeVariableAndInterface() {
    assertNull(TypeToken.of(new TypeCapture<T1>() {}.capture()).getGenericSuperclass());
    assertEquals(TypeToken.of(Object.class), new TypeToken<T1[]>() {}.getGenericSuperclass());
  }

  public void testGetGenericSuperclass_wildcard_lowerBounded() {
    assertEquals(TypeToken.of(Object.class),
        TypeToken.of(Types.supertypeOf(String.class)).getGenericSuperclass());
    assertEquals(new TypeToken<Object>() {},
        TypeToken.of(Types.supertypeOf(String[].class)).getGenericSuperclass());
    assertEquals(new TypeToken<Object>() {},
        TypeToken.of(Types.supertypeOf(CharSequence.class)).getGenericSuperclass());
  }

  public void testGetGenericSuperclass_wildcard_boundIsClass() {
    assertEquals(TypeToken.of(Object.class),
        TypeToken.of(Types.subtypeOf(Object.class)).getGenericSuperclass());
    assertEquals(new TypeToken<Object[]>() {},
        TypeToken.of(Types.subtypeOf(Object[].class)).getGenericSuperclass());
  }

  public void testGetGenericSuperclass_wildcard_boundIsInterface() {
    assertNull(TypeToken.of(Types.subtypeOf(CharSequence.class)).getGenericSuperclass());
    assertEquals(new TypeToken<CharSequence[]>() {},
        TypeToken.of(Types.subtypeOf(CharSequence[].class)).getGenericSuperclass());
  }

  public <T> void testGetGenericInterfaces_typeVariable_unbounded() {
    ASSERT.that(TypeToken.of(new TypeCapture<T>() {}.capture()).getGenericInterfaces()).isEmpty();
    assertHasArrayInterfaces(new TypeToken<T[]>() {});
  }

  public <T extends NoInterface> void testGetGenericInterfaces_typeVariable_boundIsClass() {
    ASSERT.that(TypeToken.of(new TypeCapture<T>() {}.capture()).getGenericInterfaces()).isEmpty();
    assertHasArrayInterfaces(new TypeToken<T[]>() {});
  }

  public <T extends NoInterface&Iterable<String>>
  void testGetGenericInterfaces_typeVariable_boundsAreClassWithInterface() {
    assertThat(TypeToken.of(new TypeCapture<T>() {}.capture()).getGenericInterfaces())
        .has().exactly(new TypeToken<Iterable<String>>() {});
    assertHasArrayInterfaces(new TypeToken<T[]>() {});
  }

  public <T extends CharSequence&Iterable<String>>
  void testGetGenericInterfaces_typeVariable_boundsAreInterfaces() {
    assertThat(TypeToken.of(new TypeCapture<T>() {}.capture()).getGenericInterfaces())
        .has().exactly(TypeToken.of(CharSequence.class), new TypeToken<Iterable<String>>() {});
    assertHasArrayInterfaces(new TypeToken<T[]>() {});
  }

  public <T extends CharSequence&Iterable<T>>
  void testGetGenericInterfaces_typeVariable_boundsAreFBoundedInterfaces() {
    assertThat(TypeToken.of(new TypeCapture<T>() {}.capture()).getGenericInterfaces())
        .has().exactly(TypeToken.of(CharSequence.class), new TypeToken<Iterable<T>>() {});
    assertHasArrayInterfaces(new TypeToken<T[]>() {});
  }

  public <T extends Base&Iterable<T>>
  void testGetGenericInterfaces_typeVariable_boundsAreClassWithFBoundedInterface() {
    assertThat(TypeToken.of(new TypeCapture<T>() {}.capture()).getGenericInterfaces())
        .has().exactly(new TypeToken<Iterable<T>>() {});
    assertHasArrayInterfaces(new TypeToken<T[]>() {});
  }

  public <T extends NoInterface, T1 extends T, T2 extends T1>
  void testGetGenericInterfaces_typeVariable_boundIsTypeVariableAndClass() {
    ASSERT.that(TypeToken.of(new TypeCapture<T2>() {}.capture()).getGenericInterfaces()).isEmpty();
    assertHasArrayInterfaces(new TypeToken<T2[]>() {});
  }

  public <T extends Iterable<T>, T1 extends T, T2 extends T1>
  void testGetGenericInterfaces_typeVariable_boundIsTypeVariableAndInterface() {
    assertThat(TypeToken.of(new TypeCapture<T2>() {}.capture()).getGenericInterfaces())
        .has().exactly(TypeToken.of(new TypeCapture<T1>() {}.capture()));
    assertHasArrayInterfaces(new TypeToken<T2[]>() {});
  }

  public void testGetGenericInterfaces_wildcard_lowerBounded() {
    ASSERT.that(TypeToken.of(Types.supertypeOf(String.class)).getGenericInterfaces()).isEmpty();
    ASSERT.that(TypeToken.of(Types.supertypeOf(String[].class)).getGenericInterfaces()).isEmpty();
  }

  public void testGetGenericInterfaces_wildcard_boundIsClass() {
    ASSERT.that(TypeToken.of(Types.subtypeOf(Object.class)).getGenericInterfaces()).isEmpty();
    ASSERT.that(TypeToken.of(Types.subtypeOf(Object[].class)).getGenericInterfaces()).isEmpty();
  }

  public void testGetGenericInterfaces_wildcard_boundIsInterface() {
    TypeToken<Iterable<String>> interfaceType = new TypeToken<Iterable<String>>() {};
    assertThat(TypeToken.of(Types.subtypeOf(interfaceType.getType())).getGenericInterfaces())
        .has().exactly(interfaceType);
    assertHasArrayInterfaces(new TypeToken<Iterable<String>[]>() {});
  }

  public void testGetGenericInterfaces_noInterface() {
    ASSERT.that(new TypeToken<NoInterface>() {}.getGenericInterfaces()).isEmpty();
    assertHasArrayInterfaces(new TypeToken<NoInterface[]>() {});
  }

  public void testGetGenericInterfaces_withInterfaces() {
    Map<Class<?>, Type> interfaceMap = Maps.newHashMap();
    for (TypeToken<?> interfaceType:
        new TypeToken<Implementation<Integer, String>>() {}.getGenericInterfaces()) {
      interfaceMap.put(interfaceType.getRawType(), interfaceType.getType());
    }
    assertEquals(ImmutableMap.of(
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

  private abstract static class Implementation<K, V>
      implements Iterable<V>, Map<K, V> {}

  private abstract static class First<T> {}

  private abstract static class Second<D> extends First<D> {}

  private abstract static class Third<T, D> extends Second<T> {}

  private abstract static class Fourth<T, D> extends Third<D, T> {}

  private static class ConcreteIS extends Fourth<Integer, String> {}

  private static class ConcreteSI extends Fourth<String, Integer> {}

  public void testAssignableClassToClass() {
    @SuppressWarnings("rawtypes") // To test TypeToken<List>
    TypeToken<List> tokL = new TypeToken<List>() {};
    assertTrue(tokL.isAssignableFrom(List.class));
    assertTrue(tokL.isAssignableFrom(ArrayList.class));
    assertFalse(tokL.isAssignableFrom(List[].class));

    TypeToken<Number> tokN = new TypeToken<Number>() {};
    assertTrue(tokN.isAssignableFrom(Number.class));
    assertTrue(tokN.isAssignableFrom(Integer.class));
  }

  public <T> void testAssignableParameterizedTypeToObject() {
    assertTrue(TypeToken.of(Object.class).isAssignableFrom(
        TypeToken.of(new TypeCapture<T>() {}.capture())));
    assertFalse(TypeToken.of(int.class).isAssignableFrom(
        TypeToken.of(new TypeCapture<T>() {}.capture())));
  }

  public <T, T1 extends T> void testAssignableGenericArrayToGenericArray() {
    assertTrue(new TypeToken<T[]>() {}.isAssignableFrom(new TypeToken<T[]>() {}));
    assertTrue(new TypeToken<T[]>() {}.isAssignableFrom(new TypeToken<T1[]>() {}));
    assertFalse(new TypeToken<T[]>() {}.isAssignableFrom(new TypeToken<T[][]>() {}));
  }

  public void testAssignableWildcardBoundedByArrayToArrayClass() throws Exception {
    Type wildcardType = Types.subtypeOf(Object[].class);
    assertTrue(TypeToken.of(Object[].class).isAssignableFrom(wildcardType));
    assertTrue(TypeToken.of(Object.class).isAssignableFrom(wildcardType));
    assertTrue(TypeToken.of(wildcardType).isAssignableFrom(wildcardType));
    assertFalse(TypeToken.of(int[].class).isAssignableFrom(wildcardType));
  }

  public void testAssignableArrayClassToBoundedWildcard() throws Exception {
    TypeToken<?> upperBounded = TypeToken.of(Types.subtypeOf(Object[].class));
    TypeToken<?> lowerBounded = TypeToken.of(Types.supertypeOf(Object[].class));
    assertTrue(upperBounded.isAssignableFrom(Object[].class));
    assertTrue(upperBounded.isAssignableFrom(Object[][].class));
    assertTrue(upperBounded.isAssignableFrom(String[].class));
    assertTrue(lowerBounded.isAssignableFrom(Object[].class));
    assertTrue(lowerBounded.isAssignableFrom(Object.class));
    assertFalse(lowerBounded.isAssignableFrom(Object[][].class));
    assertFalse(lowerBounded.isAssignableFrom(String[].class));
  }

  public void testAssignableWildcardBoundedByIntArrayToArrayClass() throws Exception {
    Type wildcardType = Types.subtypeOf(int[].class);
    assertTrue(TypeToken.of(int[].class).isAssignableFrom(wildcardType));
    assertTrue(TypeToken.of(Object.class).isAssignableFrom(wildcardType));
    assertTrue(TypeToken.of(wildcardType).isAssignableFrom(wildcardType));
    assertFalse(TypeToken.of(Object[].class).isAssignableFrom(wildcardType));
  }

  public void testAssignableWildcardToWildcard() throws Exception {
    TypeToken<?> upperBounded = TypeToken.of(Types.subtypeOf(Object[].class));
    TypeToken<?> lowerBounded = TypeToken.of(Types.supertypeOf(Object[].class));
    assertFalse(lowerBounded.isAssignableFrom(upperBounded));
    assertTrue(lowerBounded.isAssignableFrom(lowerBounded));
    assertTrue(upperBounded.isAssignableFrom(upperBounded));
    assertFalse(upperBounded.isAssignableFrom(lowerBounded));
  }

  public <T> void testAssignableGenericArrayToArrayClass() {
    assertTrue(TypeToken.of(Object[].class).isAssignableFrom(new TypeToken<T[]>() {}));
    assertTrue(TypeToken.of(Object[].class).isAssignableFrom(new TypeToken<T[][]>() {}));
    assertTrue(TypeToken.of(Object[][].class).isAssignableFrom(new TypeToken<T[][]>() {}));
  }

  public void testAssignableParameterizedTypeToClass() {
    @SuppressWarnings("rawtypes") // Trying to test raw class
    TypeToken<List> tokL = new TypeToken<List>() {};
    assertTrue(tokL.isAssignableFrom(StringList.class));
    assertTrue(tokL.isAssignableFrom(
        StringList.class.getGenericInterfaces()[0]));

    @SuppressWarnings("rawtypes") // Trying to test raw class
    TypeToken<Second> tokS = new TypeToken<Second>() {};
    assertTrue(tokS.isAssignableFrom(Second.class));
    assertTrue(tokS.isAssignableFrom(Third.class.getGenericSuperclass()));
  }

  public void testAssignableArrayToClass() throws Exception {
    @SuppressWarnings("rawtypes") // Trying to test raw class
    TypeToken<List[]> tokL = new TypeToken<List[]>() {};
    assertTrue(tokL.isAssignableFrom(List[].class));
    assertFalse(tokL.isAssignableFrom(List.class));

    @SuppressWarnings("rawtypes") // Trying to test raw class
    TypeToken<Second[]> tokS = new TypeToken<Second[]>() {};
    assertTrue(tokS.isAssignableFrom(Second[].class));
    assertTrue(tokS.isAssignableFrom(Third[].class));
  }

  @SuppressWarnings("rawtypes") // Trying to test raw class
  public void testAssignableTokenToClass() {
    TypeToken<List> tokL = new TypeToken<List>() {};
    assertTrue(tokL.isAssignableFrom(new TypeToken<List>() {}));
    assertTrue(tokL.isAssignableFrom(new TypeToken<List<String>>() {}));
    assertTrue(tokL.isAssignableFrom(new TypeToken<List<?>>() {}));

    TypeToken<Second> tokS = new TypeToken<Second>() {};
    assertTrue(tokS.isAssignableFrom(new TypeToken<Second>() {}));
    assertTrue(tokS.isAssignableFrom(new TypeToken<Third>() {}));
    assertTrue(tokS.isAssignableFrom(
        new TypeToken<Third<String, Integer>>() {}));

    TypeToken<List[]> tokA = new TypeToken<List[]>() {};
    assertTrue(tokA.isAssignableFrom(new TypeToken<List[]>() {}));
    assertTrue(tokA.isAssignableFrom(new TypeToken<List<String>[]>() {}));
    assertTrue(tokA.isAssignableFrom(new TypeToken<List<?>[]>() {}));
  }

  public void testAssignableClassToType() {
    TypeToken<List<String>> tokenL = new TypeToken<List<String>>() {};
    assertTrue(tokenL.isAssignableFrom(StringList.class));
    assertFalse(tokenL.isAssignableFrom(List.class));

    TypeToken<First<String>> tokenF = new TypeToken<First<String>>() {};
    assertTrue(tokenF.isAssignableFrom(ConcreteIS.class));
    assertFalse(tokenF.isAssignableFrom(ConcreteSI.class));
  }

  public void testAssignableClassToArrayType() {
    TypeToken<List<String>[]> tokenL = new TypeToken<List<String>[]>() {};
    assertTrue(tokenL.isAssignableFrom(StringList[].class));
    assertFalse(tokenL.isAssignableFrom(List[].class));
  }

  public void testAssignableParameterizedTypeToType() {
    TypeToken<List<String>> tokenL = new TypeToken<List<String>>() {};
    assertTrue(tokenL.isAssignableFrom(
        StringList.class.getGenericInterfaces()[0]));
    assertFalse(tokenL.isAssignableFrom(
        IntegerList.class.getGenericInterfaces()[0]));

    TypeToken<First<String>> tokenF = new TypeToken<First<String>>() {};
    assertTrue(tokenF.isAssignableFrom(
        ConcreteIS.class.getGenericSuperclass()));
    assertFalse(tokenF.isAssignableFrom(
        ConcreteSI.class.getGenericSuperclass()));
  }

  public void testGenericArrayTypeToArrayType() throws Exception {
    TypeToken<List<String>[]> tokL = new TypeToken<List<String>[]>() {};
    TypeToken<ArrayList<String>[]> token =
        new TypeToken<ArrayList<String>[]>() {};
    assertTrue(tokL.isAssignableFrom(tokL.getType()));
    assertTrue(tokL.isAssignableFrom(token.getType()));
  }

  public void testAssignableTokenToType() {
    TypeToken<List<String>> tokenL = new TypeToken<List<String>>() {};
    assertTrue(tokenL.isAssignableFrom(new TypeToken<List<String>>() {}));
    assertTrue(tokenL.isAssignableFrom(new TypeToken<ArrayList<String>>() {}));
    assertTrue(tokenL.isAssignableFrom(new TypeToken<StringList>() {}));

    TypeToken<First<String>> tokenF = new TypeToken<First<String>>() {};
    assertTrue(tokenF.isAssignableFrom(new TypeToken<Second<String>>() {}));
    assertTrue(tokenF.isAssignableFrom(
        new TypeToken<Third<String, Integer>>() {}));
    assertFalse(tokenF.isAssignableFrom(
        new TypeToken<Third<Integer, String>>() {}));
    assertTrue(tokenF.isAssignableFrom(
        new TypeToken<Fourth<Integer, String>>() {}));
    assertFalse(tokenF.isAssignableFrom(
        new TypeToken<Fourth<String, Integer>>() {}));
    assertTrue(tokenF.isAssignableFrom(new TypeToken<ConcreteIS>() {}));
    assertFalse(tokenF.isAssignableFrom(new TypeToken<ConcreteSI>() {}));
  }

  public void testAssignableWithWildcards() {
    TypeToken<?> unboundedToken = new TypeToken<List<?>>() {};
    TypeToken<?> upperBoundToken = new TypeToken<List<? extends Number>>() {};
    TypeToken<?> lowerBoundToken = new TypeToken<List<? super Number>>() {};
    TypeToken<?> concreteToken = new TypeToken<List<Number>>() {};
    TypeToken<?> subtypeToken = new TypeToken<List<Integer>>() {};
    TypeToken<?> supertypeToken = new TypeToken<List<Serializable>>() {};
    List<TypeToken<?>> allTokens = ImmutableList.of(
        unboundedToken, upperBoundToken, lowerBoundToken,
        concreteToken, subtypeToken, supertypeToken);

    for (TypeToken<?> typeToken : allTokens) {
      assertTrue(typeToken.toString(), unboundedToken.isAssignableFrom(typeToken));
    }

    assertFalse(upperBoundToken.isAssignableFrom(unboundedToken));
    assertTrue(upperBoundToken.isAssignableFrom(upperBoundToken));
    assertFalse(upperBoundToken.isAssignableFrom(lowerBoundToken));
    assertTrue(upperBoundToken.isAssignableFrom(concreteToken));
    assertTrue(upperBoundToken.isAssignableFrom(subtypeToken));
    assertFalse(upperBoundToken.isAssignableFrom(supertypeToken));

    assertFalse(lowerBoundToken.isAssignableFrom(unboundedToken));
    assertFalse(lowerBoundToken.isAssignableFrom(upperBoundToken));
    assertTrue(lowerBoundToken.isAssignableFrom(lowerBoundToken));
    assertTrue(lowerBoundToken.isAssignableFrom(concreteToken));
    assertFalse(lowerBoundToken.isAssignableFrom(subtypeToken));
    assertTrue(lowerBoundToken.isAssignableFrom(supertypeToken));

    for (TypeToken<?> typeToken : allTokens) {
      assertEquals(typeToken.toString(),
          typeToken == concreteToken, concreteToken.isAssignableFrom(typeToken));
    }

    for (TypeToken<?> typeToken : allTokens) {
      assertEquals(typeToken.toString(),
          typeToken == subtypeToken, subtypeToken.isAssignableFrom(typeToken));
    }

    for (TypeToken<?> typeToken : allTokens) {
      assertEquals(typeToken.toString(),
          typeToken == supertypeToken, supertypeToken.isAssignableFrom(typeToken));
    }
  }

  public <N1 extends Number, N2 extends Number, N11 extends N1>
      void testIsAssignableFrom_typeVariable() {
    assertAssignable(TypeToken.of(new TypeCapture<N1>() {}.capture()),
        TypeToken.of(new TypeCapture<N1>() {}.capture()));
    assertNotAssignable(new TypeToken<List<N11>>() {},
        new TypeToken<List<N1>>() {});
    assertNotAssignable(new TypeToken<Number>() {},
        TypeToken.of(new TypeCapture<N1>() {}.capture()));
    assertAssignable(TypeToken.of(new TypeCapture<N11>() {}.capture()),
        TypeToken.of(new TypeCapture<N1>() {}.capture()));
    assertNotAssignable(TypeToken.of(new TypeCapture<N2>() {}.capture()),
        TypeToken.of(new TypeCapture<N1>() {}.capture()));
  }

  public <N1 extends Number, N2 extends Number, N11 extends N1>
      void testIsAssignableFrom_equalWildcardTypes() {
    assertAssignable(new TypeToken<List<? extends N1>>() {},
        new TypeToken<List<? extends N1>>() {});
    assertAssignable(new TypeToken<List<? super N1>>() {},
        new TypeToken<List<? super N1>>() {});
    assertAssignable(new TypeToken<List<? extends Number>>() {},
        new TypeToken<List<? extends Number>>() {});
    assertAssignable(new TypeToken<List<? super Number>>() {},
        new TypeToken<List<? super Number>>() {});
  }

  public <N> void testIsAssignableFrom_wildcard_noBound() {
    assertAssignable(new TypeToken<List<? super N>>() {},
        new TypeToken<List<?>>() {});
    assertAssignable(new TypeToken<List<N>>() {},
        new TypeToken<List<?>>() {});
  }

  public <N1 extends Number, N2 extends Number, N11 extends N1>
      void testIsAssignableFrom_wildcardType_upperBoundMatch() {
    // ? extends T
    assertAssignable(new TypeToken<List<N11>>() {},
        new TypeToken<List<? extends N1>>() {});
    assertNotAssignable(new TypeToken<List<N1>>() {},
        new TypeToken<List<? extends N11>>() {});
    assertNotAssignable(new TypeToken<List<Number>>() {},
        new TypeToken<List<? extends N11>>() {});

    // ? extends Number
    assertAssignable(new TypeToken<List<N1>>() {},
        new TypeToken<List<? extends Number>>() {});
    assertAssignable(new TypeToken<ArrayList<N1>>() {},
        new TypeToken<List<? extends Number>>() {});
    assertAssignable(new TypeToken<List<? extends N11>>() {},
        new TypeToken<List<? extends Number>>() {});
  }

  public <N1 extends Number, N2 extends Number, N11 extends N1>
      void testIsAssignableFrom_wildcardType_lowerBoundMatch() {
    // ? super T
    assertAssignable(new TypeToken<List<N1>>() {},
        new TypeToken<List<? super N11>>() {});
    assertAssignable(new TypeToken<ArrayList<Number>>() {},
        new TypeToken<List<? super N1>>() {});
    assertNotAssignable(new TypeToken<ArrayList<? super N11>>() {},
        new TypeToken<List<? super Number>>() {});
    assertAssignable(new TypeToken<ArrayList<? super N1>>() {},
        new TypeToken<List<? super N11>>() {});
    assertAssignable(new TypeToken<ArrayList<? super Number>>() {},
        new TypeToken<List<? super N11>>() {});

    // ? super Number
    assertNotAssignable(new TypeToken<ArrayList<N11>>() {},
        new TypeToken<List<? super Number>>() {});
    assertAssignable(new TypeToken<ArrayList<Number>>() {},
        new TypeToken<List<? super Number>>() {});
    assertAssignable(new TypeToken<ArrayList<Object>>() {},
        new TypeToken<List<? super Number>>() {});
  }

  public <L extends List<R>, R extends List<L>>
      void testIsAssignableFrom_recursiveTypeVariableBounds() {
    assertAssignable(TypeToken.of(new TypeCapture<L>() {}.capture()),
        TypeToken.of(new TypeCapture<L>() {}.capture()));
    assertNotAssignable(TypeToken.of(new TypeCapture<R>() {}.capture()),
        TypeToken.of(new TypeCapture<L>() {}.capture()));
    assertAssignable(TypeToken.of(new TypeCapture<L>() {}.capture()),
        new TypeToken<List<R>>() {});
  }

  public void testIsAssignableFrom_resolved() {
    assertFalse(Assignability.of().isAssignable());
    assertTrue(new Assignability<Integer, Integer>() {}.isAssignable());
    assertTrue(new Assignability<Integer, Object>() {}.isAssignable());
    assertFalse(new Assignability<Integer, String>() {}.isAssignable());
    TypeTokenTest.<Number, Integer>assignabilityTestWithTypeVariables();
  }

  private static <N1 extends Number, N11 extends N1>
      void assignabilityTestWithTypeVariables() {
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

  public void testIsArray_wildcardType() throws Exception {
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
    assertEquals(TypeToken.of(new TypeCapture<T>() {}.capture()),
        new TypeToken<T[]>() {}.getComponentType());
    assertEquals(new TypeToken<T[]>() {}, new TypeToken<T[][]>() {}.getComponentType());
  }

  public void testGetComponentType_wildcardType() throws Exception {
    assertEquals(Types.subtypeOf(Object.class),
        TypeToken.of(Types.subtypeOf(Object[].class)).getComponentType().getType());
    assertEquals(Types.subtypeOf(Object[].class),
        Types.newArrayType(
            TypeToken.of(Types.subtypeOf(Object[].class)).getComponentType().getType()));
    assertEquals(int.class,
        TypeToken.of(Types.subtypeOf(int[].class)).getComponentType().getType());
    assertNull(TypeToken.of(Types.subtypeOf(Object.class)).getComponentType());
    assertNull(TypeToken.of(Types.supertypeOf(Object[].class)).getComponentType());
  }

  private interface NumberList<T extends Number> {}

  public void testImplicitUpperBoundForWildcards() {
    assertAssignable(
        new TypeToken<NumberList<? extends Number>>() {},
        new TypeToken<NumberList<?>>() {});
    assertAssignable(
        new TypeToken<NumberList<? super Integer>>() {},
        new TypeToken<NumberList<?>>() {});
  }

  public <T extends Readable & Appendable> void testMultiBound() {
    assertAssignable(new TypeToken<List<T>>() {},
        new TypeToken<List<? extends Readable>>() {});
    assertAssignable(new TypeToken<List<T>>() {},
        new TypeToken<List<? extends Appendable>>() {});
  }

  public void testToGenericType() {
    assertEquals(TypeToken.of(String.class), TypeToken.toGenericType(String.class));
    assertEquals(new TypeToken<int[]>() {}, TypeToken.toGenericType(int[].class));
    @SuppressWarnings("rawtypes") // Iterable.class
    TypeToken<? extends Iterable> genericType = TypeToken.toGenericType(Iterable.class);
    assertEquals(Iterable.class, genericType.getRawType());
    assertEquals(Types.newParameterizedType(Iterable.class, Iterable.class.getTypeParameters()[0]),
        genericType.getType());
  }

  private interface ListIterable<T> extends Iterable<List<T>> {}
  private interface StringListIterable extends ListIterable<String> {}
  private interface ListArrayIterable<T> extends Iterable<List<T>[]> {}
  private interface StringListArrayIterable extends ListIterable<String> {}

  public void testGetSupertype_withTypeVariable() {
    ParameterizedType expectedType = Types.newParameterizedType(Iterable.class,
        Types.newParameterizedType(List.class, ListIterable.class.getTypeParameters()[0]));
    assertEquals(expectedType,
        TypeToken.of(ListIterable.class).getSupertype(Iterable.class).getType());
  }

  public void testGetSupertype_withoutTypeVariable() {
    ParameterizedType expectedType = Types.newParameterizedType(Iterable.class,
        Types.newParameterizedType(List.class, String.class));
    assertEquals(expectedType,
        TypeToken.of(StringListIterable.class).getSupertype(Iterable.class).getType());
  }

  public void testGetSupertype_chained() {
    @SuppressWarnings("unchecked") // StringListIterable extensd ListIterable<String>
    TypeToken<ListIterable<String>> listIterableType = (TypeToken<ListIterable<String>>)
        TypeToken.of(StringListIterable.class).getSupertype(ListIterable.class);
    ParameterizedType expectedType = Types.newParameterizedType(Iterable.class,
        Types.newParameterizedType(List.class, String.class));
    assertEquals(expectedType, listIterableType.getSupertype(Iterable.class).getType());
  }

  public void testGetSupertype_withArray() {
    assertEquals(new TypeToken<Iterable<List<String>>[]>() {},
        TypeToken.of(StringListIterable[].class).getSupertype(Iterable[].class));
    assertEquals(int[].class, TypeToken.of(int[].class).getSupertype(int[].class).getType());
    assertEquals(Object.class, TypeToken.of(int[].class).getSupertype(Object.class).getType());
    assertEquals(int[][].class, TypeToken.of(int[][].class).getSupertype(int[][].class).getType());
    assertEquals(Object[].class,
        TypeToken.of(String[].class).getSupertype(Object[].class).getType());
    assertEquals(Object.class, TypeToken.of(String[].class).getSupertype(Object.class).getType());
  }

  public void testGetSupertype_fromWildcard() {
    @SuppressWarnings("unchecked") // can't do new TypeToken<? extends ...>() {}
    TypeToken<? extends List<String>> type = (TypeToken<? extends List<String>>)
        TypeToken.of(Types.subtypeOf(new TypeToken<List<String>>() {}.getType()));
    assertEquals(new TypeToken<Iterable<String>>() {}, type.getSupertype(Iterable.class));
  }

  public <T extends Iterable<String>> void testGetSupertype_fromTypeVariable() {
    @SuppressWarnings("unchecked") // to construct TypeToken<T> from TypeToken.of()
    TypeToken<T> typeVariableToken = (TypeToken<T>) TypeToken.of(new TypeCapture<T>() {}.capture());
    assertEquals(new TypeToken<Iterable<String>>() {},
        typeVariableToken.getSupertype(Iterable.class));
  }

  @SuppressWarnings("rawtypes") // purpose is to test raw type
  public void testGetSupertype_fromRawClass() {
    assertEquals(Types.newParameterizedType(Iterable.class, List.class.getTypeParameters()[0]),
        new TypeToken<List>() {}.getSupertype(Iterable.class).getType());
  }

  @SuppressWarnings({"rawtypes", "unchecked"}) // purpose is to test raw type
  public void testGetSupertype_notSupertype() {
    try {
      new TypeToken<List<String>>() {}.getSupertype((Class) String.class);
      fail();
    } catch (IllegalArgumentException expected) {}
  }

  public void testGetSupertype_fromArray() {
    assertEquals(new TypeToken<Iterable<String>[]>() {},
        new TypeToken<List<String>[]>() {}.getSupertype(Iterable[].class));
  }

  private interface ListMap<K, V> extends Map<K, List<V>> {}

  public void testGetSupertype_fullyGenericType() {
    ParameterizedType expectedType = Types.newParameterizedType(Map.class,
        ListMap.class.getTypeParameters()[0],
        Types.newParameterizedType(List.class, ListMap.class.getTypeParameters()[1]));
    assertEquals(expectedType,
        TypeToken.of(ListMap.class).getSupertype(Map.class).getType());
  }

  public void testGetSupertype_fullySpecializedType() {
    Type expectedType = new TypeToken<Map<String, List<Object>>>() {}.getType();
    assertEquals(expectedType,
        new TypeToken<ListMap<String, Object>>() {}.getSupertype(Map.class).getType());
  }

  private interface StringListMap<V> extends ListMap<String, V> {}

  public <V> void testGetSupertype_partiallySpecializedType() {
    Type expectedType = new TypeToken<Map<String, List<V>>>() {}.getType();
    assertEquals(expectedType,
        new TypeToken<StringListMap<V>>() {}.getSupertype(Map.class).getType());
  }

  public void testGetSubtype_withTypeVariable() {
    assertEquals(new TypeToken<ListIterable<String>>() {},
        new TypeToken<Iterable<List<String>>>() {}.getSubtype(ListIterable.class));
    assertEquals(new TypeToken<ListArrayIterable<String>>() {},
        new TypeToken<Iterable<List<String>[]>>() {}.getSubtype(ListArrayIterable.class));
    assertEquals(new TypeToken<ListArrayIterable<String>[]>() {},
        new TypeToken<Iterable<List<String>[]>[]>() {}.getSubtype(ListArrayIterable[].class));
  }

  public void testGetSubtype_withoutTypeVariable() {
    assertEquals(StringListIterable.class,
        TypeToken.of(Iterable.class).getSubtype(StringListIterable.class).getType());
    assertEquals(StringListIterable[].class,
        TypeToken.of(Iterable[].class).getSubtype(StringListIterable[].class).getType());
    assertEquals(TypeToken.of(StringListArrayIterable.class),
        new TypeToken<Iterable<List<String>>>() {}.getSubtype(StringListArrayIterable.class));
    assertEquals(TypeToken.of(StringListArrayIterable[].class),
        new TypeToken<Iterable<List<String>>[]>() {}.getSubtype(StringListArrayIterable[].class));
  }

  public void testGetSubtype_withArray() {
    assertEquals(TypeToken.of(StringListIterable[].class),
        TypeToken.of(Iterable[].class).getSubtype(StringListIterable[].class));
    assertEquals(TypeToken.of(String[].class),
        TypeToken.of(Object[].class).getSubtype(String[].class));
    assertEquals(TypeToken.of(int[].class),
        TypeToken.of(Object.class).getSubtype(int[].class));
  }

  public void testGetSubtype_fromWildcard() {
    @SuppressWarnings("unchecked") // can't do new TypeToken<? extends ...>() {}
    TypeToken<? super Iterable<String>> type = (TypeToken<? super Iterable<String>>)
        TypeToken.of(Types.supertypeOf(new TypeToken<Iterable<String>>() {}.getType()));
    assertEquals(new TypeToken<List<String>>() {}, type.getSubtype(List.class));
  }

  public void testGetSubtype_fromWildcard_lowerBoundNotSupertype() {
    @SuppressWarnings("unchecked") // can't do new TypeToken<? extends ...>() {}
    TypeToken<? super Iterable<String>> type = (TypeToken<? super Iterable<String>>)
        TypeToken.of(Types.supertypeOf(new TypeToken<ImmutableList<String>>() {}.getType()));
    try {
      type.getSubtype(List.class);
      fail();
    } catch (IllegalArgumentException expected) {}
  }

  public void testGetSubtype_fromWildcard_upperBounded() {
    @SuppressWarnings("unchecked") // can't do new TypeToken<? extends ...>() {}
    TypeToken<? extends Iterable<String>> type = (TypeToken<? extends Iterable<String>>)
        TypeToken.of(Types.subtypeOf(new TypeToken<Iterable<String>>() {}.getType()));
    try {
      type.getSubtype(Iterable.class);
      fail();
    } catch (IllegalArgumentException expected) {}
  }

  public <T extends Iterable<String>> void testGetSubtype_fromTypeVariable() {
    try {
      TypeToken.of(new TypeCapture<T>() {}.capture()).getSubtype(List.class);
      fail();
    } catch (IllegalArgumentException expected) {}
  }

  @SuppressWarnings("rawtypes") // purpose is to test raw type
  public void testGetSubtype_fromRawClass() {
    assertEquals(List.class, new TypeToken<Iterable>() {}.getSubtype(List.class).getType());
  }

  public void testGetSubtype_fromArray() {
    assertEquals(new TypeToken<List<String>[]>() {},
        new TypeToken<Iterable<String>[]>() {}.getSubtype(List[].class));
  }

  @SuppressWarnings("unchecked") // To construct TypeToken<T> with TypeToken.of()
  public <T> void testWhere_circleRejected() {
    TypeToken<List<T>> type = new TypeToken<List<T>>() {};
    try {
      type.where(new TypeParameter<T>() {},
          (TypeToken<T>) TypeToken.of(new TypeCapture<T>() {}.capture()));
      fail();
    } catch (IllegalArgumentException expected) {}
  }

  public void testWhere() {
    assertEquals(
        new TypeToken<Map<String, Integer>>() {},
        mapOf(String.class, Integer.class));
    assertEquals(new TypeToken<int[]>() {}, arrayOf(int.class));
    assertEquals(int[].class, arrayOf(int.class).getRawType());
  }

  @SuppressWarnings("unused") // used by reflection
  private static class Holder<T> {
    T element;
    List<T> list;
    List<T>[] matrix;

    void setList(List<T> list) {
      this.list = list;
    }
  }

  public void testWildcardCaptured_methodParameter_upperBound() throws Exception {
    TypeToken<Holder<?>> type = new TypeToken<Holder<?>>() {};
    TypeToken<?> parameterType = type.resolveType(
        Holder.class.getDeclaredMethod("setList", List.class).getGenericParameterTypes()[0]);
    assertEquals(List.class, parameterType.getRawType());
    assertFalse(parameterType.getType().toString(),
        parameterType.isAssignableFrom(new TypeToken<List<Integer>>() {}));
  }

  public void testWildcardCaptured_field_upperBound() throws Exception {
    TypeToken<Holder<?>> type = new TypeToken<Holder<?>>() {};
    TypeToken<?> matrixType = type.resolveType(
        Holder.class.getDeclaredField("matrix").getGenericType());
    assertEquals(List[].class, matrixType.getRawType());
    ASSERT.that(matrixType.getType())
        .isNotEqualTo(new TypeToken<List<?>[]>() {}.getType());
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
    assertEquals(TypeToken.of(List.class),
        TypeToken.of(List.class).method(sizeMethod).getOwnerType());
    assertEquals(new TypeToken<List<String>>() {},
        new TypeToken<List<String>>() {}.method(sizeMethod).getOwnerType());
  }

  public void testMethod_notDeclaredByType() throws NoSuchMethodException {
    Method sizeMethod = Map.class.getMethod("size");
    try {
      TypeToken.of(List.class).method(sizeMethod);
      fail();
    } catch (IllegalArgumentException expected) {}
  }

  public void testMethod_declaredBySuperclass() throws Exception {
    Method toStringMethod = Object.class.getMethod("toString");
    ImmutableList<String> list = ImmutableList.of("foo");
    assertEquals(list.toString(), TypeToken.of(List.class).method(toStringMethod).invoke(list));
  }

  public <T extends Number & List<String>> void testMethod_returnType_resolvedAgainstTypeBound()
      throws NoSuchMethodException {
    Method getMethod = List.class.getMethod("get", int.class);
    Invokable<T, String> invokable = new TypeToken<T>(getClass()) {}
        .method(getMethod)
        .returning(String.class);
    assertEquals(TypeToken.of(String.class), invokable.getReturnType());
  }

  public <T extends List<String>> void testMethod_parameterTypes()
      throws NoSuchMethodException {
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
    ASSERT.that(invokable.getExceptionTypes()).has().item(TypeToken.of(AssertionError.class));
  }

  public void testConstructor_getOwnerType() throws NoSuchMethodException {
    @SuppressWarnings("rawtypes") // raw class ArrayList.class
    Constructor<ArrayList> constructor = ArrayList.class.getConstructor();
    assertEquals(TypeToken.of(ArrayList.class),
        TypeToken.of(ArrayList.class).constructor(constructor).getOwnerType());
    assertEquals(new TypeToken<ArrayList<String>>() {},
        new TypeToken<ArrayList<String>>() {}.constructor(constructor).getOwnerType());
  }

  public void testConstructor_notDeclaredByType() throws NoSuchMethodException {
    Constructor<String> constructor = String.class.getConstructor();
    try {
      TypeToken.of(Object.class).constructor(constructor);
      fail();
    } catch (IllegalArgumentException expected) {}
  }

  public void testConstructor_declaredBySuperclass() throws NoSuchMethodException {
    Constructor<Object> constructor = Object.class.getConstructor();
    try {
      TypeToken.of(String.class).constructor(constructor);
      fail();
    } catch (IllegalArgumentException expected) {}
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
    ASSERT.that(invokable.getExceptionTypes()).has().item(TypeToken.of(AssertionError.class));
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
    assertNoTypeVariable(
        new TypeCapture<Iterable<? extends String>>() {}.capture());
    assertNoTypeVariable(
        new TypeCapture<Iterable<? super String>>() {}.capture());
  }

  public void testRejectTypeVariable_genericArrayType() {
    assertNoTypeVariable(
        new TypeCapture<Iterable<? extends String>[]>() {}.capture());
  }

  public <T> void testRejectTypeVariable_withTypeVariable() {
    assertHasTypeVariable(new TypeCapture<T>() {}.capture());
    assertHasTypeVariable(new TypeCapture<T[]>() {}.capture());
    assertHasTypeVariable(new TypeCapture<Iterable<T>>() {}.capture());
    assertHasTypeVariable(new TypeCapture<Map<String, T>>() {}.capture());
    assertHasTypeVariable(
        new TypeCapture<Map<String, ? extends T>>() {}.capture());
    assertHasTypeVariable(
        new TypeCapture<Map<String, ? super T[]>>() {}.capture());
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
    } catch (IllegalArgumentException expected) {}
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
    abstract <T2 extends CharSequence&Iterable<T2>> void acceptT2(T2 t2);

    static void verifyConsitentRawType() {
      for (Method method : RawTypeConsistencyTester.class.getDeclaredMethods()) {
        assertEquals(method.getReturnType(), TypeToken.getRawType(method.getGenericReturnType()));
        for (int i = 0; i < method.getParameterTypes().length; i++) {
          assertEquals(method.getParameterTypes()[i],
              TypeToken.getRawType(method.getGenericParameterTypes()[i]));
        }
      }
    }
  }

  public void testRawTypes() throws Exception {
    RawTypeConsistencyTester.verifyConsitentRawType();
    assertEquals(Object.class, TypeToken.getRawType(Types.subtypeOf(Object.class)));
    assertEquals(CharSequence.class, TypeToken.getRawType(Types.subtypeOf(CharSequence.class)));
    assertEquals(Object.class, TypeToken.getRawType(Types.supertypeOf(CharSequence.class)));
  }

  private abstract static class IKnowMyType<T> {
    TypeToken<T> type() {
      return new TypeToken<T>(getClass()) {};
    }
  }

  public void testTypeResolution() {
    assertEquals(String.class,
        new IKnowMyType<String>() {}.type().getType());
    assertEquals(new TypeToken<Map<String, Integer>>() {},
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
    } catch (RuntimeException expected) {}
  }

  public <A> void testSerializable_typeVariableNotSupported() {
    try {
      new ITryToSerializeMyTypeVariable<String>().go();
      fail();
    } catch (RuntimeException expected) {}
  }

  private static class ITryToSerializeMyTypeVariable<T> {
    void go() {
      SerializableTester.reserialize(TypeToken.of(new TypeCapture<T>() {}.capture()));
    }
  }

  private static <T> T reserialize(T object) {
    T copy = SerializableTester.reserialize(object);
    new EqualsTester()
        .addEqualityGroup(object, copy)
        .testEquals();
    return copy;
  }

  public void testTypeResolutionAfterReserialized() {
    reserialize(new TypeToken<String>() {});
    reserialize(new TypeToken<Map<String, Integer>>() {});
    TypeToken<Map<String, Integer>> reserialized = reserialize(
        new TypeToken<Map<String, Integer>>() {});
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
        .addEqualityGroup(
            new TypeToken<List<String>>() {},
            new TypeToken<List<String>>() {})
        .addEqualityGroup(
            new TypeToken<List<?>>() {},
            new TypeToken<List<?>>() {})
        .addEqualityGroup(
            new TypeToken<Map<A, ?>>() {},
            new TypeToken<Map<A, ?>>() {})
        .addEqualityGroup(
            new TypeToken<Map<B, ?>>() {})
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
    return new TypeToken<Map<K, V>>() {}
        .where(new TypeParameter<K>() {}, keyType)
        .where(new TypeParameter<V>() {}, valueType);
  }

  private static <T> TypeToken<T[]> arrayOf(Class<T> componentType) {
    return new TypeToken<T[]>() {}
        .where(new TypeParameter<T>() {}, componentType);
  }

  public <T> void testNulls() {
    new NullPointerTester()
        .testAllPublicStaticMethods(TypeToken.class);
    new NullPointerTester()
        .setDefault(TypeParameter.class, new TypeParameter<T>() {})
        .testAllPublicInstanceMethods(TypeToken.of(String.class));
  }

  private static class Assignability<From, To> {

    boolean isAssignable() {
      return new TypeToken<To>(getClass()) {}.isAssignableFrom(new TypeToken<From>(getClass()) {});
    }

    static <From, To> Assignability<From, To> of() {
      return new Assignability<From, To>();
    }
  }

  private static void assertAssignable(TypeToken<?> from, TypeToken<?> to) {
    assertTrue(
        from.getType() + " is expected to be assignable to " + to.getType(),
        to.isAssignableFrom(from));
  }

  private static void assertNotAssignable(TypeToken<?> from, TypeToken<?> to) {
    assertFalse(
        from.getType() + " shouldn't be assignable to " + to.getType(),
        to.isAssignableFrom(from));
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

  private static CollectionSubject<?, Object, ?> assertThat(Collection<?> actual) {
    return ASSERT.that(Collections.<Object>unmodifiableCollection(actual));
  }
}
