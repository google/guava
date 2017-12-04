/*
 * Copyright (C) 2013 The Guava Authors
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

import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.ArrayList;
import java.util.EnumSet;
import junit.framework.TestCase;

/**
 * Tests of {@link TypeVisitor}.
 *
 * @author Ben Yu
 */
public class TypeVisitorTest extends TestCase {

  public void testVisitNull() {
    new BaseTypeVisitor()
        .visit(((ParameterizedType) ArrayList.class.getGenericSuperclass()).getOwnerType());
  }

  public void testVisitClass() {
    assertVisited(String.class);
    new BaseTypeVisitor() {
      @Override
      void visitClass(Class<?> t) {}
    }.visit(String.class);
  }

  public <T> void testVisitTypeVariable() {
    Type type = new TypeCapture<T>() {}.capture();
    assertVisited(type);
    new BaseTypeVisitor() {
      @Override
      void visitTypeVariable(TypeVariable<?> t) {}
    }.visit(type);
  }

  public void testVisitWildcardType() {
    WildcardType type = Types.subtypeOf(String.class);
    assertVisited(type);
    new BaseTypeVisitor() {
      @Override
      void visitWildcardType(WildcardType t) {}
    }.visit(type);
  }

  public <T> void testVisitGenericArrayType() {
    Type type = new TypeCapture<T[]>() {}.capture();
    assertVisited(type);
    new BaseTypeVisitor() {
      @Override
      void visitGenericArrayType(GenericArrayType t) {}
    }.visit(type);
  }

  public <T> void testVisitParameterizedType() {
    Type type = new TypeCapture<Iterable<T>>() {}.capture();
    assertVisited(type);
    new BaseTypeVisitor() {
      @Override
      void visitParameterizedType(ParameterizedType t) {}
    }.visit(type);
  }

  public <E extends Enum<E>> void testVisitRecursiveTypeBounds() {
    Type type = new TypeCapture<EnumSet<E>>() {}.capture();
    assertVisited(type);
    new BaseTypeVisitor() {
      @Override
      void visitParameterizedType(ParameterizedType t) {
        visit(t.getActualTypeArguments());
      }

      @Override
      void visitTypeVariable(TypeVariable<?> t) {
        visit(t.getBounds());
      }
    }.visit(type);
  }

  private static void assertVisited(Type type) {
    TypeVisitor visitor = new BaseTypeVisitor();
    try {
      visitor.visit(type);
      fail("Type not visited");
    } catch (UnsupportedOperationException expected) {
    }
    try {
      visitor.visit(new Type[] {type});
      fail("Type not visited");
    } catch (UnsupportedOperationException expected) {
    }
  }

  private static class BaseTypeVisitor extends TypeVisitor {
    @Override
    void visitTypeVariable(TypeVariable<?> t) {
      throw new UnsupportedOperationException();
    }

    @Override
    void visitWildcardType(WildcardType t) {
      throw new UnsupportedOperationException();
    }

    @Override
    void visitParameterizedType(ParameterizedType t) {
      throw new UnsupportedOperationException();
    }

    @Override
    void visitClass(Class<?> t) {
      throw new UnsupportedOperationException();
    }

    @Override
    void visitGenericArrayType(GenericArrayType t) {
      throw new UnsupportedOperationException();
    }
  }
}
