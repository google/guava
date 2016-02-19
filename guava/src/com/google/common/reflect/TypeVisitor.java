/*
 * Copyright (C) 2013 The Guava Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.common.reflect;

import com.google.common.collect.Sets;

import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.Set;

import javax.annotation.concurrent.NotThreadSafe;

/**
 * Based on what a {@link Type} is, dispatch it to the corresponding {@code visit*} method. By
 * default, no recursion is done for type arguments or type bounds. But subclasses can opt to do
 * recursion by calling {@link #visit} for any {@code Type} while visitation is in progress. For
 * example, this can be used to reject wildcards or type variables contained in a type as in:
 *
 * <pre>   {@code
 *   new TypeVisitor() {
 *     protected void visitParameterizedType(ParameterizedType t) {
 *       visit(t.getOwnerType());
 *       visit(t.getActualTypeArguments());
 *     }
 *     protected void visitGenericArrayType(GenericArrayType t) {
 *       visit(t.getGenericComponentType());
 *     }
 *     protected void visitTypeVariable(TypeVariable<?> t) {
 *       throw new IllegalArgumentException("Cannot contain type variable.");
 *     }
 *     protected void visitWildcardType(WildcardType t) {
 *       throw new IllegalArgumentException("Cannot contain wildcard type.");
 *     }
 *   }.visit(type);}</pre>
 * 
 * <p>One {@code Type} is visited at most once. The second time the same type is visited, it's
 * ignored by {@link #visit}. This avoids infinite recursion caused by recursive type bounds.
 *
 * <p>This class is <em>not</em> thread safe.
 *
 * @author Ben Yu
 */
@NotThreadSafe
abstract class TypeVisitor {

  private final Set<Type> visited = Sets.newHashSet();

  /**
   * Visits the given types. Null types are ignored. This allows subclasses to call
   * {@code visit(parameterizedType.getOwnerType())} safely without having to check nulls.
   */
  public final void visit(Type... types) {
    for (Type type : types) {
      if (type == null || !visited.add(type)) {
        // null owner type, or already visited;
        continue;
      }
      boolean succeeded = false;
      try {
        if (type instanceof TypeVariable) {
          visitTypeVariable((TypeVariable<?>) type);
        } else if (type instanceof WildcardType) {
          visitWildcardType((WildcardType) type);
        } else if (type instanceof ParameterizedType) {
          visitParameterizedType((ParameterizedType) type);
        } else if (type instanceof Class) {
          visitClass((Class<?>) type);
        } else if (type instanceof GenericArrayType) {
          visitGenericArrayType((GenericArrayType) type);
        } else {
          throw new AssertionError("Unknown type: " + type);
        }
        succeeded = true;
      } finally {
        if (!succeeded) { // When the visitation failed, we don't want to ignore the second.
          visited.remove(type);
        }
      }
    }
  }

  void visitClass(Class<?> t) {}

  void visitGenericArrayType(GenericArrayType t) {}

  void visitParameterizedType(ParameterizedType t) {}

  void visitTypeVariable(TypeVariable<?> t) {}

  void visitWildcardType(WildcardType t) {}
}
