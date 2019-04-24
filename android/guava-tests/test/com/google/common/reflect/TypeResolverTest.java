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

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import junit.framework.TestCase;

/**
 * Unit tests of {@link TypeResolver}.
 *
 * @author Ben Yu
 */
@AndroidIncompatible // lots of failures, possibly some related to bad equals() implementations?
public class TypeResolverTest extends TestCase {

  public void testWhere_noMapping() {
    Type t = aTypeVariable();
    assertEquals(t, new TypeResolver().resolveType(t));
  }

  public void testWhere_typeVariableMapping() {
    Type t = aTypeVariable();
    assertEquals(String.class, new TypeResolver().where(t, String.class).resolveType(t));
  }

  public <T> void testWhere_indirectMapping() {
    Type t1 = new TypeCapture<T>() {}.capture();
    Type t2 = aTypeVariable();
    assertEquals(
        String.class, new TypeResolver().where(t1, t2).where(t2, String.class).resolveType(t1));
  }

  public void testWhere_typeVariableSelfMapping() {
    TypeResolver resolver = new TypeResolver();
    Type t = aTypeVariable();
    assertEquals(t, resolver.where(t, t).resolveType(t));
  }

  public <T> void testWhere_parameterizedSelfMapping() {
    TypeResolver resolver = new TypeResolver();
    Type t = new TypeCapture<List<T>>() {}.capture();
    assertEquals(t, resolver.where(t, t).resolveType(t));
  }

  public <T> void testWhere_genericArraySelfMapping() {
    TypeResolver resolver = new TypeResolver();
    Type t = new TypeCapture<T[]>() {}.capture();
    assertEquals(t, resolver.where(t, t).resolveType(t));
  }

  public <T> void testWhere_rawClassSelfMapping() {
    TypeResolver resolver = new TypeResolver();
    assertEquals(
        String.class, resolver.where(String.class, String.class).resolveType(String.class));
  }

  public <T> void testWhere_wildcardSelfMapping() {
    TypeResolver resolver = new TypeResolver();
    Type t = aWildcardType();
    assertEquals(t, resolver.where(t, t).resolveType(t));
  }

  public <T> void testWhere_duplicateMapping() {
    Type t = aTypeVariable();
    TypeResolver resolver = new TypeResolver().where(t, String.class);
    try {
      resolver.where(t, String.class);
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }

  public <T1, T2 extends List<T1>> void testWhere_recursiveMapping() {
    Type t1 = new TypeCapture<T1>() {}.capture();
    Type t2 = new TypeCapture<T2>() {}.capture();
    assertEquals(t2, new TypeResolver().where(t1, t2).resolveType(t1));
  }

  public <T> void testWhere_genericArrayMapping() {
    Type t = new TypeCapture<T>() {}.capture();
    assertEquals(
        String.class,
        new TypeResolver()
            .where(new TypeCapture<T[]>() {}.capture(), String[].class)
            .resolveType(t));
  }

  public <T> void testWhere_primitiveArrayMapping() {
    Type t = new TypeCapture<T>() {}.capture();
    assertEquals(
        int.class,
        new TypeResolver().where(new TypeCapture<T[]>() {}.capture(), int[].class).resolveType(t));
  }

  public <T> void testWhere_parameterizedTypeMapping() {
    Type t = new TypeCapture<T>() {}.capture();
    assertEquals(
        String.class,
        new TypeResolver()
            .where(
                new TypeCapture<List<T>>() {}.capture(),
                new TypeCapture<List<String>>() {}.capture())
            .resolveType(t));
    assertEquals(
        Types.subtypeOf(String.class),
        new TypeResolver()
            .where(
                new TypeCapture<List<T>>() {}.capture(),
                new TypeCapture<List<? extends String>>() {}.capture())
            .resolveType(t));
    assertEquals(
        Types.supertypeOf(String.class),
        new TypeResolver()
            .where(
                new TypeCapture<List<T>>() {}.capture(),
                new TypeCapture<List<? super String>>() {}.capture())
            .resolveType(t));
  }

  public <T> void testWhere_wildcardTypeMapping() {
    Type t = new TypeCapture<T>() {}.capture();
    assertEquals(
        String.class,
        new TypeResolver()
            .where(
                new TypeCapture<List<? extends T>>() {}.capture(),
                new TypeCapture<List<? extends String>>() {}.capture())
            .resolveType(t));
    assertEquals(
        String.class,
        new TypeResolver()
            .where(
                new TypeCapture<List<? super T>>() {}.capture(),
                new TypeCapture<List<? super String>>() {}.capture())
            .resolveType(t));
  }

  public <T> void testWhere_incompatibleGenericArrayMapping() {
    try {
      new TypeResolver().where(new TypeCapture<T[]>() {}.capture(), String.class);
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }

  public <T> void testWhere_incompatibleParameterizedTypeMapping() {
    try {
      new TypeResolver().where(new TypeCapture<Iterable<T>>() {}.capture(), List.class);
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }

  public <T> void testWhere_impossibleParameterizedTypeMapping() {
    try {
      new TypeResolver()
          .where(
              new TypeCapture<List<T>>() {}.capture(),
              new TypeCapture<Map<String, Integer>>() {}.capture());
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }

  public <T> void testWhere_incompatibleWildcardUpperBound() {
    try {
      new TypeResolver()
          .where(
              new TypeCapture<List<? extends String>>() {}.capture(),
              new TypeCapture<List<? extends Integer>>() {}.capture());
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }

  public <T> void testWhere_incompatibleWildcardLowerBound() {
    try {
      new TypeResolver()
          .where(
              new TypeCapture<List<? super String>>() {}.capture(),
              new TypeCapture<List<? super Integer>>() {}.capture());
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }

  public <T> void testWhere_incompatibleWildcardBounds() {
    try {
      new TypeResolver()
          .where(
              new TypeCapture<List<? extends T>>() {}.capture(),
              new TypeCapture<List<? super String>>() {}.capture());
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }

  public <T> void testWhere_wrongOrder() {
    try {
      new TypeResolver().where(String.class, aTypeVariable());
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }

  public <T> void testWhere_mapFromConcreteParameterizedType() {
    try {
      new TypeResolver().where(new TypeCapture<List<String>>() {}.capture(), aTypeVariable());
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }

  public <T> void testWhere_mapFromConcreteGenericArrayType() {
    try {
      new TypeResolver().where(new TypeCapture<List<String>>() {}.capture(), aTypeVariable());
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }

  public <K, V> void testWhere_actualArgHasWildcard() {
    TypeResolver resolver =
        new TypeResolver()
            .where(
                new TypeCapture<Iterable<Map<?, V>>>() {}.capture(),
                new TypeCapture<Iterable<Map<String, Integer>>>() {}.capture());
    assertEquals(
        new TypeCapture<K>() {}.capture(), resolver.resolveType(new TypeCapture<K>() {}.capture()));
    assertEquals(Integer.class, resolver.resolveType(new TypeCapture<V>() {}.capture()));
  }

  public <T> void testWhere_mapFromWildcard() {
    Type subtype = new TypeCapture<TypedKeyMap<T>>() {}.capture();
    assertEquals(
        new TypeCapture<TypedKeyMap<String>>() {}.capture(),
        new TypeResolver()
            .where(
                new TypeCapture<Map<Integer, T>>() {}.capture(),
                new TypeCapture<Map<?, String>>() {}.capture())
            .resolveType(subtype));
  }

  public <T> void testWhere_mapFromWildcardToParameterized() {
    Type subtype = new TypeCapture<TypedListKeyMap<T>>() {}.capture();
    assertEquals(
        new TypeCapture<TypedListKeyMap<String>>() {}.capture(),
        new TypeResolver()
            .where(
                new TypeCapture<Map<List<Integer>, T>>() {}.capture(),
                new TypeCapture<Map<?, String>>() {}.capture())
            .resolveType(subtype));
  }

  public <T> void testWhere_mapFromBoundedWildcard() {
    Type subtype = new TypeCapture<TypedKeyMap<T>>() {}.capture();
    // TODO(benyu): This should check equality to an expected value, see discussion in cl/98674873
    Type unused =
        new TypeResolver()
            .where(
                new TypeCapture<Map<Integer, T>>() {}.capture(),
                new TypeCapture<Map<? extends Number, ? extends Number>>() {}.capture())
            .resolveType(subtype);
  }

  interface TypedKeyMap<T> extends Map<Integer, T> {}

  interface TypedListKeyMap<T> extends Map<List<Integer>, T> {}

  private static <T> Type aTypeVariable() {
    return new TypeCapture<T>() {}.capture();
  }

  private static <T> Type aWildcardType() {
    ParameterizedType parameterizedType =
        (ParameterizedType) new TypeCapture<List<? extends T>>() {}.capture();
    return parameterizedType.getActualTypeArguments()[0];
  }
}
