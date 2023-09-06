/*
 * Copyright (C) 2009 The Guava Authors
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
import static org.junit.Assert.assertThrows;

import com.google.common.base.Predicate;
import com.google.common.base.Supplier;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import junit.framework.TestCase;

/**
 * Unit test for {@link TypeToken} and {@link TypeResolver}.
 *
 * @author Ben Yu
 */
@AndroidIncompatible // lots of failures, possibly some related to bad equals() implementations?
public class TypeTokenResolutionTest extends TestCase {

  private static class Foo<A, B> {

    Class<? super A> getClassA() {
      return new TypeToken<A>(getClass()) {}.getRawType();
    }

    Class<? super B> getClassB() {
      return new TypeToken<B>(getClass()) {}.getRawType();
    }

    Class<? super A[]> getArrayClassA() {
      return new TypeToken<A[]>(getClass()) {}.getRawType();
    }

    Type getArrayTypeA() {
      return new TypeToken<A[]>(getClass()) {}.getType();
    }

    Class<? super B[]> getArrayClassB() {
      return new TypeToken<B[]>(getClass()) {}.getRawType();
    }
  }

  public void testSimpleTypeToken() {
    Foo<String, Integer> foo = new Foo<String, Integer>() {};
    assertEquals(String.class, foo.getClassA());
    assertEquals(Integer.class, foo.getClassB());
    assertEquals(String[].class, foo.getArrayClassA());
    assertEquals(Integer[].class, foo.getArrayClassB());
  }

  public void testCompositeTypeToken() {
    Foo<String[], List<int[]>> foo = new Foo<String[], List<int[]>>() {};
    assertEquals(String[].class, foo.getClassA());
    assertEquals(List.class, foo.getClassB());
    assertEquals(String[][].class, foo.getArrayClassA());
    assertEquals(List[].class, foo.getArrayClassB());
  }

  private static class StringFoo<T> extends Foo<String, T> {}

  public void testPartialSpecialization() {
    StringFoo<Integer> foo = new StringFoo<Integer>() {};
    assertEquals(String.class, foo.getClassA());
    assertEquals(Integer.class, foo.getClassB());
    assertEquals(String[].class, foo.getArrayClassA());
    assertEquals(Integer[].class, foo.getArrayClassB());
    assertEquals(new TypeToken<String[]>() {}.getType(), foo.getArrayTypeA());
  }

  public void testTypeArgNotFound() {
    StringFoo<Integer> foo = new StringFoo<>();
    assertEquals(String.class, foo.getClassA());
    assertEquals(String[].class, foo.getArrayClassA());
    assertEquals(Object.class, foo.getClassB());
    assertEquals(Object[].class, foo.getArrayClassB());
  }

  private abstract static class Bar<T> {}

  private abstract static class Parameterized<O, T, P> {
    ParameterizedType parameterizedType() {
      return new ParameterizedType() {
        @Override
        public Type[] getActualTypeArguments() {
          return new Type[] {new TypeCapture<P>() {}.capture()};
        }

        @Override
        public Type getOwnerType() {
          return new TypeCapture<O>() {}.capture();
        }

        @Override
        public Type getRawType() {
          return new TypeCapture<T>() {}.capture();
        }
      };
    }
  }

  public void testResolveType_parameterizedType() {
    @SuppressWarnings("rawtypes") // trying to test raw type
    Parameterized<?, ?, ?> parameterized =
        new Parameterized<TypeTokenResolutionTest, Bar, String>() {};
    TypeResolver typeResolver = TypeResolver.covariantly(parameterized.getClass());
    ParameterizedType resolved =
        (ParameterizedType) typeResolver.resolveType(parameterized.parameterizedType());
    assertEquals(TypeTokenResolutionTest.class, resolved.getOwnerType());
    assertEquals(Bar.class, resolved.getRawType());
    assertThat(resolved.getActualTypeArguments()).asList().contains(String.class);
  }

  private interface StringListPredicate extends Predicate<List<String>> {}

  private interface IntegerSupplier extends Supplier<Integer> {}

  // Intentionally duplicate the Predicate interface to test that it won't cause
  // exceptions
  private interface IntegerStringFunction
      extends IntegerSupplier, Predicate<List<String>>, StringListPredicate {}

  public void testGenericInterface() {
    // test the 1st generic interface on the class
    Type fType = Supplier.class.getTypeParameters()[0];
    assertEquals(
        Integer.class, TypeToken.of(IntegerStringFunction.class).resolveType(fType).getRawType());

    // test the 2nd generic interface on the class
    Type predicateParameterType = Predicate.class.getTypeParameters()[0];
    assertEquals(
        new TypeToken<List<String>>() {}.getType(),
        TypeToken.of(IntegerStringFunction.class).resolveType(predicateParameterType).getType());
  }

  private abstract static class StringIntegerFoo extends Foo<String, Integer> {}

  public void testConstructor_typeArgsResolvedFromAncestorClass() {
    assertEquals(String.class, new StringIntegerFoo() {}.getClassA());
    assertEquals(Integer.class, new StringIntegerFoo() {}.getClassB());
  }

  private static class Owner<T> {
    private abstract static class Nested<X> {
      Class<? super X> getTypeArgument() {
        return new TypeToken<X>(getClass()) {}.getRawType();
      }
    }

    private abstract class Inner<Y> extends Nested<Y> {
      Class<? super T> getOwnerType() {
        return new TypeToken<T>(getClass()) {}.getRawType();
      }
    }
  }

  public void testResolveNestedClass() {
    assertEquals(String.class, new Owner.Nested<String>() {}.getTypeArgument());
  }

  @SuppressWarnings("RestrictedApiChecker") // crashes under JDK8, which EP no longer supports
  public void testResolveInnerClass() {
    assertEquals(String.class, new Owner<Integer>().new Inner<String>() {}.getTypeArgument());
  }

  @SuppressWarnings("RestrictedApiChecker") // crashes under JDK8, which EP no longer supports
  public void testResolveOwnerClass() {
    assertEquals(Integer.class, new Owner<Integer>().new Inner<String>() {}.getOwnerType());
  }

  private static class Mapping<F, T> {

    final Type f = new TypeToken<F>(getClass()) {}.getType();
    final Type t = new TypeToken<T>(getClass()) {}.getType();

    Type getFromType() {
      return new TypeToken<F>(getClass()) {}.getType();
    }

    Type getToType() {
      return new TypeToken<T>(getClass()) {}.getType();
    }

    Mapping<T, F> flip() {
      return new Mapping<T, F>() {};
    }

    Mapping<F, T> selfMapping() {
      return new Mapping<F, T>() {};
    }
  }

  public void testCyclicMapping() {
    Mapping<Integer, String> mapping = new Mapping<>();
    assertEquals(mapping.f, mapping.getFromType());
    assertEquals(mapping.t, mapping.getToType());
    assertEquals(mapping.f, mapping.flip().getFromType());
    assertEquals(mapping.t, mapping.flip().getToType());
    assertEquals(mapping.f, mapping.selfMapping().getFromType());
    assertEquals(mapping.t, mapping.selfMapping().getToType());
  }

  private static class ParameterizedOuter<T> {

    @SuppressWarnings("unused") // used by reflection
    public Inner field;

    class Inner {}
  }

  public void testInnerClassWithParameterizedOwner() throws Exception {
    Type fieldType = ParameterizedOuter.class.getField("field").getGenericType();
    assertEquals(
        fieldType, TypeToken.of(ParameterizedOuter.class).resolveType(fieldType).getType());
  }

  private interface StringIterable extends Iterable<String> {}

  public void testResolveType() {
    assertEquals(String.class, TypeToken.of(this.getClass()).resolveType(String.class).getType());
    assertEquals(
        String.class,
        TypeToken.of(StringIterable.class)
            .resolveType(Iterable.class.getTypeParameters()[0])
            .getType());
    assertEquals(
        String.class,
        TypeToken.of(StringIterable.class)
            .resolveType(Iterable.class.getTypeParameters()[0])
            .getType());
    assertThrows(NullPointerException.class, () -> TypeToken.of(this.getClass()).resolveType(null));
  }

  public void testContextIsParameterizedType() throws Exception {
    class Context {
      @SuppressWarnings("unused") // used by reflection
      Map<String, Integer> returningMap() {
        throw new AssertionError();
      }
    }
    Type context = Context.class.getDeclaredMethod("returningMap").getGenericReturnType();
    Type keyType = Map.class.getTypeParameters()[0];
    Type valueType = Map.class.getTypeParameters()[1];

    // context is parameterized type
    assertEquals(String.class, TypeToken.of(context).resolveType(keyType).getType());
    assertEquals(Integer.class, TypeToken.of(context).resolveType(valueType).getType());

    // context is type variable
    assertEquals(keyType, TypeToken.of(keyType).resolveType(keyType).getType());
    assertEquals(valueType, TypeToken.of(valueType).resolveType(valueType).getType());
  }

  private static final class GenericArray<T> {
    final Type t = new TypeToken<T>(getClass()) {}.getType();
    final Type array = new TypeToken<T[]>(getClass()) {}.getType();
  }

  public void testGenericArrayType() {
    GenericArray<?> genericArray = new GenericArray<>();
    assertEquals(GenericArray.class.getTypeParameters()[0], genericArray.t);
    assertEquals(Types.newArrayType(genericArray.t), genericArray.array);
  }

  public void testClassWrapper() {
    TypeToken<String> typeExpression = TypeToken.of(String.class);
    assertEquals(String.class, typeExpression.getType());
    assertEquals(String.class, typeExpression.getRawType());
  }

  private static class Red<A> {
    private class Orange {
      Class<?> getClassA() {
        return new TypeToken<A>(getClass()) {}.getRawType();
      }

      Red<A> getSelfB() {
        return Red.this;
      }
    }

    Red<A> getSelfA() {
      return this;
    }

    private class Yellow<B> extends Red<B>.Orange {
      Yellow(Red<B> red) {
        red.super();
      }

      Class<?> getClassB() {
        return new TypeToken<B>(getClass()) {}.getRawType();
      }

      Red<A> getA() {
        return getSelfA();
      }

      Red<B> getB() {
        return getSelfB();
      }
    }

    Class<?> getClassDirect() {
      return new TypeToken<A>(getClass()) {}.getRawType();
    }
  }

  public void test1() {
    Red<String> redString = new Red<String>() {};
    Red<Integer> redInteger = new Red<Integer>() {};
    assertEquals(String.class, redString.getClassDirect());
    assertEquals(Integer.class, redInteger.getClassDirect());

    Red<String>.Yellow<Integer> yellowInteger = redString.new Yellow<Integer>(redInteger) {};
    assertEquals(Integer.class, yellowInteger.getClassA());
    assertEquals(Integer.class, yellowInteger.getClassB());
    assertEquals(String.class, yellowInteger.getA().getClassDirect());
    assertEquals(Integer.class, yellowInteger.getB().getClassDirect());
  }

  public void test2() {
    Red<String> redString = new Red<>();
    Red<Integer> redInteger = new Red<>();
    Red<String>.Yellow<Integer> yellowInteger = redString.new Yellow<Integer>(redInteger) {};
    assertEquals(Integer.class, yellowInteger.getClassA());
    assertEquals(Integer.class, yellowInteger.getClassB());
  }

  private static <T> Type staticMethodWithLocalClass() {
    class MyLocalClass {
      Type getType() {
        return new TypeToken<T>(getClass()) {}.getType();
      }
    }
    return new MyLocalClass().getType();
  }

  public void testLocalClassInsideStaticMethod() {
    assertNotNull(staticMethodWithLocalClass());
  }

  public void testLocalClassInsideNonStaticMethod() {
    class MyLocalClass<T> {
      Type getType() {
        return new TypeToken<T>(getClass()) {}.getType();
      }
    }
    assertNotNull(new MyLocalClass<String>().getType());
  }

  private static <T> Type staticMethodWithAnonymousClass() {
    return new Object() {
      Type getType() {
        return new TypeToken<T>(getClass()) {}.getType();
      }
    }.getType();
  }

  public void testAnonymousClassInsideStaticMethod() {
    assertNotNull(staticMethodWithAnonymousClass());
  }

  public void testAnonymousClassInsideNonStaticMethod() {
    assertNotNull(
        new Object() {
          Type getType() {
            return new TypeToken<Object>() {}.getType();
          }
        }.getType());
  }

  public void testStaticContext() {
    assertEquals(Map.class, mapType().getRawType());
  }

  private abstract static class Holder<T> {
    Type getContentType() {
      return new TypeToken<T>(getClass()) {}.getType();
    }
  }

  public void testResolvePrimitiveArrayType() {
    assertEquals(new TypeToken<int[]>() {}.getType(), new Holder<int[]>() {}.getContentType());
    assertEquals(new TypeToken<int[][]>() {}.getType(), new Holder<int[][]>() {}.getContentType());
  }

  public void testResolveToGenericArrayType() {
    GenericArrayType arrayType =
        (GenericArrayType) new Holder<List<int[][]>[]>() {}.getContentType();
    ParameterizedType listType = (ParameterizedType) arrayType.getGenericComponentType();
    assertEquals(List.class, listType.getRawType());
    assertEquals(Types.newArrayType(int[].class), listType.getActualTypeArguments()[0]);
  }

  private abstract class WithGenericBound<A> {

    @SuppressWarnings("unused")
    public <B extends A> void withTypeVariable(List<B> list) {}

    @SuppressWarnings("unused")
    public <E extends Enum<E>> void withRecursiveBound(List<E> list) {}

    @SuppressWarnings("unused")
    public <K extends List<V>, V extends List<K>> void withMutualRecursiveBound(
        List<Map<K, V>> list) {}

    @SuppressWarnings("unused")
    void withWildcardLowerBound(List<? super A> list) {}

    @SuppressWarnings("unused")
    void withWildcardUpperBound(List<? extends A> list) {}

    Type getTargetType(String methodName) throws Exception {
      ParameterizedType parameterType =
          (ParameterizedType)
              WithGenericBound.class.getDeclaredMethod(methodName, List.class)
                  .getGenericParameterTypes()[0];
      parameterType =
          (ParameterizedType) TypeToken.of(this.getClass()).resolveType(parameterType).getType();
      return parameterType.getActualTypeArguments()[0];
    }
  }

  public void testWithGenericBoundInTypeVariable() throws Exception {
    TypeVariable<?> typeVariable =
        (TypeVariable<?>) new WithGenericBound<String>() {}.getTargetType("withTypeVariable");
    assertEquals(String.class, typeVariable.getBounds()[0]);
  }

  public void testWithRecursiveBoundInTypeVariable() throws Exception {
    TypeVariable<?> typeVariable =
        (TypeVariable<?>) new WithGenericBound<String>() {}.getTargetType("withRecursiveBound");
    assertEquals(Types.newParameterizedType(Enum.class, typeVariable), typeVariable.getBounds()[0]);
  }

  public void testWithMutualRecursiveBoundInTypeVariable() throws Exception {
    ParameterizedType paramType =
        (ParameterizedType)
            new WithGenericBound<String>() {}.getTargetType("withMutualRecursiveBound");
    TypeVariable<?> k = (TypeVariable<?>) paramType.getActualTypeArguments()[0];
    TypeVariable<?> v = (TypeVariable<?>) paramType.getActualTypeArguments()[1];
    assertEquals(Types.newParameterizedType(List.class, v), k.getBounds()[0]);
    assertEquals(Types.newParameterizedType(List.class, k), v.getBounds()[0]);
  }

  public void testWithGenericLowerBoundInWildcard() throws Exception {
    WildcardType wildcardType =
        (WildcardType) new WithGenericBound<String>() {}.getTargetType("withWildcardLowerBound");
    assertEquals(String.class, wildcardType.getLowerBounds()[0]);
  }

  public void testWithGenericUpperBoundInWildcard() throws Exception {
    WildcardType wildcardType =
        (WildcardType) new WithGenericBound<String>() {}.getTargetType("withWildcardUpperBound");
    assertEquals(String.class, wildcardType.getUpperBounds()[0]);
  }

  public void testInterfaceTypeParameterResolution() throws Exception {
    assertEquals(
        String.class,
        TypeToken.of(new TypeToken<ArrayList<String>>() {}.getType())
            .resolveType(List.class.getTypeParameters()[0])
            .getType());
  }

  private static TypeToken<Map<Object, Object>> mapType() {
    return new TypeToken<Map<Object, Object>>() {};
  }

  // Looks like recursive, but legit.
  private interface WithFalseRecursiveType<K, V> {
    WithFalseRecursiveType<List<V>, String> keyShouldNotResolveToStringList();

    WithFalseRecursiveType<List<K>, List<V>> shouldNotCauseInfiniteLoop();

    SubtypeOfWithFalseRecursiveType<List<V>, List<K>> evenSubtypeWorks();
  }

  private interface SubtypeOfWithFalseRecursiveType<K1, V1>
      extends WithFalseRecursiveType<List<K1>, List<V1>> {
    SubtypeOfWithFalseRecursiveType<V1, K1> revertKeyAndValueTypes();
  }

  public void testFalseRecursiveType_mappingOnTheSameDeclarationNotUsed() {
    Type returnType =
        genericReturnType(WithFalseRecursiveType.class, "keyShouldNotResolveToStringList");
    TypeToken<?> keyType =
        TypeToken.of(returnType).resolveType(WithFalseRecursiveType.class.getTypeParameters()[0]);
    assertEquals("java.util.List<V>", keyType.getType().toString());
  }

  public void testFalseRecursiveType_notRealRecursiveMapping() {
    Type returnType = genericReturnType(WithFalseRecursiveType.class, "shouldNotCauseInfiniteLoop");
    TypeToken<?> keyType =
        TypeToken.of(returnType).resolveType(WithFalseRecursiveType.class.getTypeParameters()[0]);
    assertEquals("java.util.List<K>", keyType.getType().toString());
  }

  public void testFalseRecursiveType_referenceOfSubtypeDoesNotConfuseMe() {
    Type returnType = genericReturnType(WithFalseRecursiveType.class, "evenSubtypeWorks");
    TypeToken<?> keyType =
        TypeToken.of(returnType).resolveType(WithFalseRecursiveType.class.getTypeParameters()[0]);
    assertEquals("java.util.List<java.util.List<V>>", keyType.getType().toString());
  }

  public void testFalseRecursiveType_intermediaryTypeMappingDoesNotConfuseMe() {
    Type returnType =
        genericReturnType(SubtypeOfWithFalseRecursiveType.class, "revertKeyAndValueTypes");
    TypeToken<?> keyType =
        TypeToken.of(returnType).resolveType(WithFalseRecursiveType.class.getTypeParameters()[0]);
    assertEquals("java.util.List<K1>", keyType.getType().toString());
  }

  private static Type genericReturnType(Class<?> cls, String methodName) {
    try {
      return cls.getMethod(methodName).getGenericReturnType();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public void testTwoStageResolution() {
    class ForTwoStageResolution<A extends Number> {
      <B extends A> void verifyTwoStageResolution() {
        @SuppressWarnings({"unchecked", "rawtypes"})
        Type type =
            new TypeToken<B>(getClass()) {}
            // B's bound may have already resolved to something.
            // Make sure it can still further resolve when given a context.
            .where(new TypeParameter<B>() {}, (Class) Integer.class).getType();
        assertEquals(Integer.class, type);
      }
    }
    new ForTwoStageResolution<Integer>().verifyTwoStageResolution();
    new ForTwoStageResolution<Integer>() {}.verifyTwoStageResolution();
  }
}
