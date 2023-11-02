/*
 * Copyright (C) 2011 The Guava Authors
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
import static java.util.Arrays.asList;
import static org.junit.Assert.assertThrows;

import com.google.common.collect.Lists;
import com.google.common.testing.EqualsTester;
import com.google.common.testing.NullPointerTester;
import com.google.common.testing.NullPointerTester.Visibility;
import com.google.common.testing.SerializableTester;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.GenericDeclaration;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import junit.framework.TestCase;

/**
 * Tests for {@link Types}.
 *
 * @author Ben Yu
 */
@AndroidIncompatible // lots of failures, possibly some related to bad equals() implementations?
public class TypesTest extends TestCase {
  public void testNewParameterizedType_ownerTypeImplied() throws Exception {
    ParameterizedType jvmType =
        (ParameterizedType) new TypeCapture<Entry<String, Integer>>() {}.capture();
    ParameterizedType ourType =
        Types.newParameterizedType(Entry.class, String.class, Integer.class);
    assertEquals(jvmType, ourType);
    assertEquals(Map.class, ourType.getOwnerType());
  }

  public void testNewParameterizedType() {
    ParameterizedType jvmType =
        (ParameterizedType) new TypeCapture<HashMap<String, int[][]>>() {}.capture();
    ParameterizedType ourType =
        Types.newParameterizedType(HashMap.class, String.class, int[][].class);

    new EqualsTester().addEqualityGroup(jvmType, ourType).testEquals();
    assertEquals(jvmType.toString(), ourType.toString());
    assertEquals(jvmType.hashCode(), ourType.hashCode());
    assertEquals(HashMap.class, ourType.getRawType());
    assertThat(ourType.getActualTypeArguments())
        .asList()
        .containsExactlyElementsIn(asList(jvmType.getActualTypeArguments()))
        .inOrder();
    assertEquals(
        Arrays.asList(String.class, Types.newArrayType(Types.newArrayType(int.class))),
        Arrays.asList(ourType.getActualTypeArguments()));
    assertEquals(null, ourType.getOwnerType());
  }

  public void testNewParameterizedType_nonStaticLocalClass() {
    class LocalClass<T> {}
    Type jvmType = new LocalClass<String>() {}.getClass().getGenericSuperclass();
    Type ourType = Types.newParameterizedType(LocalClass.class, String.class);
    assertEquals(jvmType, ourType);
  }

  public void testNewParameterizedType_staticLocalClass() {
    doTestNewParameterizedType_staticLocalClass();
  }

  private static void doTestNewParameterizedType_staticLocalClass() {
    class LocalClass<T> {}
    Type jvmType = new LocalClass<String>() {}.getClass().getGenericSuperclass();
    Type ourType = Types.newParameterizedType(LocalClass.class, String.class);
    assertEquals(jvmType, ourType);
  }

  public void testNewParameterizedTypeWithOwner() {
    ParameterizedType jvmType =
        (ParameterizedType) new TypeCapture<Entry<String, int[][]>>() {}.capture();
    ParameterizedType ourType =
        Types.newParameterizedTypeWithOwner(Map.class, Entry.class, String.class, int[][].class);

    new EqualsTester()
        .addEqualityGroup(jvmType, ourType)
        .addEqualityGroup(new TypeCapture<Entry<String, String>>() {}.capture())
        .addEqualityGroup(new TypeCapture<Map<String, Integer>>() {}.capture())
        .testEquals();
    assertEquals(jvmType.toString(), ourType.toString());
    assertEquals(Map.class, ourType.getOwnerType());
    assertEquals(Entry.class, ourType.getRawType());
    assertThat(ourType.getActualTypeArguments())
        .asList()
        .containsExactlyElementsIn(asList(jvmType.getActualTypeArguments()))
        .inOrder();
  }

  public void testNewParameterizedType_serializable() {
    SerializableTester.reserializeAndAssert(
        Types.newParameterizedType(Entry.class, String.class, Integer.class));
  }

  public void testNewParameterizedType_ownerMismatch() {
    assertThrows(
        IllegalArgumentException.class,
        () -> Types.newParameterizedTypeWithOwner(Number.class, List.class, String.class));
  }

  public void testNewParameterizedType_ownerMissing() {
    assertEquals(
        Types.newParameterizedType(Entry.class, String.class, Integer.class),
        Types.newParameterizedTypeWithOwner(null, Entry.class, String.class, Integer.class));
  }

  public void testNewParameterizedType_invalidTypeParameters() {
    assertThrows(
        IllegalArgumentException.class,
        () -> Types.newParameterizedTypeWithOwner(Map.class, Entry.class, String.class));
  }

  public void testNewParameterizedType_primitiveTypeParameters() {
    assertThrows(
        IllegalArgumentException.class,
        () -> Types.newParameterizedTypeWithOwner(Map.class, Entry.class, int.class, int.class));
  }

  public void testNewArrayType() {
    Type jvmType1 = new TypeCapture<List<String>[]>() {}.capture();
    GenericArrayType ourType1 =
        (GenericArrayType) Types.newArrayType(Types.newParameterizedType(List.class, String.class));
    Type jvmType2 = new TypeCapture<List[]>() {}.capture();
    Type ourType2 = Types.newArrayType(List.class);
    new EqualsTester()
        .addEqualityGroup(jvmType1, ourType1)
        .addEqualityGroup(jvmType2, ourType2)
        .testEquals();
    assertEquals(new TypeCapture<List<String>>() {}.capture(), ourType1.getGenericComponentType());
    assertEquals(jvmType1.toString(), ourType1.toString());
    assertEquals(jvmType2.toString(), ourType2.toString());
  }

  public void testNewArrayTypeOfArray() {
    Type jvmType = new TypeCapture<int[][]>() {}.capture();
    Type ourType = Types.newArrayType(int[].class);
    assertEquals(jvmType.toString(), ourType.toString());
    new EqualsTester().addEqualityGroup(jvmType, ourType).testEquals();
  }

  public void testNewArrayType_primitive() {
    Type jvmType = new TypeCapture<int[]>() {}.capture();
    Type ourType = Types.newArrayType(int.class);
    assertEquals(jvmType.toString(), ourType.toString());
    new EqualsTester().addEqualityGroup(jvmType, ourType).testEquals();
  }

  public void testNewArrayType_upperBoundedWildcard() {
    Type wildcard = Types.subtypeOf(Number.class);
    assertEquals(Types.subtypeOf(Number[].class), Types.newArrayType(wildcard));
  }

  public void testNewArrayType_lowerBoundedWildcard() {
    Type wildcard = Types.supertypeOf(Number.class);
    assertEquals(Types.supertypeOf(Number[].class), Types.newArrayType(wildcard));
  }

  public void testNewArrayType_serializable() {
    SerializableTester.reserializeAndAssert(Types.newArrayType(int[].class));
  }

  private static class WithWildcardType {

    @SuppressWarnings("unused")
    void withoutBound(List<?> list) {}

    @SuppressWarnings("unused")
    void withObjectBound(List<? extends Object> list) {}

    @SuppressWarnings("unused")
    void withUpperBound(List<? extends int[][]> list) {}

    @SuppressWarnings("unused")
    void withLowerBound(List<? super String[][]> list) {}

    static WildcardType getWildcardType(String methodName) throws Exception {
      ParameterizedType parameterType =
          (ParameterizedType)
              WithWildcardType.class.getDeclaredMethod(methodName, List.class)
                  .getGenericParameterTypes()[0];
      return (WildcardType) parameterType.getActualTypeArguments()[0];
    }
  }

  public void testNewWildcardType() throws Exception {
    WildcardType noBoundJvmType = WithWildcardType.getWildcardType("withoutBound");
    WildcardType objectBoundJvmType = WithWildcardType.getWildcardType("withObjectBound");
    WildcardType upperBoundJvmType = WithWildcardType.getWildcardType("withUpperBound");
    WildcardType lowerBoundJvmType = WithWildcardType.getWildcardType("withLowerBound");
    WildcardType objectBound = Types.subtypeOf(Object.class);
    WildcardType upperBound = Types.subtypeOf(int[][].class);
    WildcardType lowerBound = Types.supertypeOf(String[][].class);

    assertEqualWildcardType(noBoundJvmType, objectBound);
    assertEqualWildcardType(objectBoundJvmType, objectBound);
    assertEqualWildcardType(upperBoundJvmType, upperBound);
    assertEqualWildcardType(lowerBoundJvmType, lowerBound);

    new EqualsTester()
        .addEqualityGroup(noBoundJvmType, objectBoundJvmType, objectBound)
        .addEqualityGroup(upperBoundJvmType, upperBound)
        .addEqualityGroup(lowerBoundJvmType, lowerBound)
        .testEquals();
  }

  public void testNewWildcardType_primitiveTypeBound() {
    assertThrows(IllegalArgumentException.class, () -> Types.subtypeOf(int.class));
  }

  public void testNewWildcardType_serializable() {
    SerializableTester.reserializeAndAssert(Types.supertypeOf(String.class));
    SerializableTester.reserializeAndAssert(Types.subtypeOf(String.class));
    SerializableTester.reserializeAndAssert(Types.subtypeOf(Object.class));
  }

  private static void assertEqualWildcardType(WildcardType expected, WildcardType actual) {
    assertEquals(expected.toString(), actual.toString());
    assertEquals(actual.toString(), expected.hashCode(), actual.hashCode());
    assertThat(actual.getLowerBounds())
        .asList()
        .containsExactlyElementsIn(asList(expected.getLowerBounds()))
        .inOrder();
    assertThat(actual.getUpperBounds())
        .asList()
        .containsExactlyElementsIn(asList(expected.getUpperBounds()))
        .inOrder();
  }

  private static class WithTypeVariable {

    @SuppressWarnings("unused")
    <T> void withoutBound(List<T> list) {}

    @SuppressWarnings("unused")
    <T extends Object> void withObjectBound(List<T> list) {}

    @SuppressWarnings("unused")
    <T extends Number & CharSequence> void withUpperBound(List<T> list) {}

    static TypeVariable<?> getTypeVariable(String methodName) throws Exception {
      ParameterizedType parameterType =
          (ParameterizedType)
              WithTypeVariable.class.getDeclaredMethod(methodName, List.class)
                  .getGenericParameterTypes()[0];
      return (TypeVariable<?>) parameterType.getActualTypeArguments()[0];
    }
  }

  public void testNewTypeVariable() throws Exception {
    TypeVariable<?> noBoundJvmType = WithTypeVariable.getTypeVariable("withoutBound");
    TypeVariable<?> objectBoundJvmType = WithTypeVariable.getTypeVariable("withObjectBound");
    TypeVariable<?> upperBoundJvmType = WithTypeVariable.getTypeVariable("withUpperBound");
    TypeVariable<?> noBound = withBounds(noBoundJvmType);
    TypeVariable<?> objectBound = withBounds(objectBoundJvmType, Object.class);
    TypeVariable<?> upperBound = withBounds(upperBoundJvmType, Number.class, CharSequence.class);

    assertEqualTypeVariable(noBoundJvmType, noBound);
    assertEqualTypeVariable(noBoundJvmType, withBounds(noBoundJvmType, Object.class));
    assertEqualTypeVariable(objectBoundJvmType, objectBound);
    assertEqualTypeVariable(upperBoundJvmType, upperBound);

    new TypeVariableEqualsTester()
        .addEqualityGroup(noBoundJvmType, noBound)
        .addEqualityGroup(objectBoundJvmType, objectBound)
        .addEqualityGroup(upperBoundJvmType, upperBound)
        .testEquals();
  }

  public void testNewTypeVariable_primitiveTypeBound() {
    assertThrows(
        IllegalArgumentException.class,
        () -> Types.newArtificialTypeVariable(List.class, "E", int.class));
  }

  public void testNewTypeVariable_serializable() throws Exception {
    assertThrows(
        RuntimeException.class,
        () -> SerializableTester.reserialize(Types.newArtificialTypeVariable(List.class, "E")));
  }

  private static <D extends GenericDeclaration> TypeVariable<D> withBounds(
      TypeVariable<D> typeVariable, Type... bounds) {
    return Types.newArtificialTypeVariable(
        typeVariable.getGenericDeclaration(), typeVariable.getName(), bounds);
  }

  private static class TypeVariableEqualsTester {
    private final EqualsTester tester = new EqualsTester();

    TypeVariableEqualsTester addEqualityGroup(Type jvmType, Type... types) {
      if (Types.NativeTypeVariableEquals.NATIVE_TYPE_VARIABLE_ONLY) {
        tester.addEqualityGroup(jvmType);
        tester.addEqualityGroup((Object[]) types);
      } else {
        tester.addEqualityGroup(Lists.asList(jvmType, types).toArray());
      }
      return this;
    }

    void testEquals() {
      tester.testEquals();
    }
  }

  private static void assertEqualTypeVariable(TypeVariable<?> expected, TypeVariable<?> actual) {
    assertEquals(expected.toString(), actual.toString());
    assertEquals(expected.getName(), actual.getName());
    assertEquals(expected.getGenericDeclaration(), actual.getGenericDeclaration());
    if (!Types.NativeTypeVariableEquals.NATIVE_TYPE_VARIABLE_ONLY) {
      assertEquals(actual.toString(), expected.hashCode(), actual.hashCode());
    }
    assertThat(actual.getBounds())
        .asList()
        .containsExactlyElementsIn(asList(expected.getBounds()))
        .inOrder();
  }

  /**
   * Working with arrays requires defensive code. Verify that we clone the type array for both input
   * and output.
   */
  public void testNewParameterizedTypeImmutability() {
    Type[] typesIn = {String.class, Integer.class};
    ParameterizedType parameterizedType = Types.newParameterizedType(Map.class, typesIn);
    typesIn[0] = null;
    typesIn[1] = null;

    Type[] typesOut = parameterizedType.getActualTypeArguments();
    typesOut[0] = null;
    typesOut[1] = null;

    assertEquals(String.class, parameterizedType.getActualTypeArguments()[0]);
    assertEquals(Integer.class, parameterizedType.getActualTypeArguments()[1]);
  }

  public void testNewParameterizedTypeWithWrongNumberOfTypeArguments() {
    assertThrows(
        IllegalArgumentException.class,
        () -> Types.newParameterizedType(Map.class, String.class, Integer.class, Long.class));
  }

  public void testToString() {
    assertEquals(int[].class.getName(), Types.toString(int[].class));
    assertEquals(int[][].class.getName(), Types.toString(int[][].class));
    assertEquals(String[].class.getName(), Types.toString(String[].class));
    Type elementType = List.class.getTypeParameters()[0];
    assertEquals(elementType.toString(), Types.toString(elementType));
  }

  public void testNullPointers() {
    new NullPointerTester().testStaticMethods(Types.class, Visibility.PACKAGE);
  }
}
