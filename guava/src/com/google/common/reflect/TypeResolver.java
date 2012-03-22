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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.Nullable;

/**
 * This class can be used by any generic super class to resolve one of its type
 * parameter to the actual type argument used by the subclass, provided the type
 * argument is carried on the class declaration.
 * 
 * @author Ben Yu
 */
class TypeResolver {
  
  private final ImmutableMap<TypeVariable<?>, Type> typeTable;

  static TypeResolver accordingTo(Type type) {
    return new TypeResolver().where(TypeMappingIntrospector.getTypeMappings(type));
  }

  TypeResolver() {
    this.typeTable = ImmutableMap.of();
  }
  
  private TypeResolver(ImmutableMap<TypeVariable<?>, Type> typeTable) {
    this.typeTable = typeTable;
  }
  
  /** Returns a new {@code TypeResolver} with {@code variable} mapping to {@code type}. */
  final TypeResolver where(Map<? extends TypeVariable<?>, ? extends Type> mappings) {
    ImmutableMap.Builder<TypeVariable<?>, Type> builder = ImmutableMap.builder();
    builder.putAll(typeTable);
    for (Map.Entry<? extends TypeVariable<?>, ? extends Type> mapping : mappings.entrySet()) {
      TypeVariable<?> variable = mapping.getKey();
      Type type = mapping.getValue();
      checkArgument(!variable.equals(type), "Type variable %s bound to itself", variable);
      builder.put(variable, type);
    }
    return new TypeResolver(builder.build());
  }

  /**
   * Returns a new {@code TypeResolver} with type variables in {@code mapFrom} mapping to types in
   * {@code type}. Either {@code mapFrom} is a type variable, or {@code mapFrom} and {@code mapTo}
   * are both {@link ParameterizedType} of the same raw type, or {@link GenericArrayType} of the
   * same component type. Caller needs to ensure this before calling.
   */
  final TypeResolver where(Type mapFrom, Type mapTo) {
    Map<TypeVariable<?>, Type> mappings = Maps.newHashMap();
    populateTypeMappings(mappings, mapFrom, mapTo);
    return where(mappings);
  }

  private static void populateTypeMappings(
      Map<TypeVariable<?>, Type> mappings, Type from, Type to) {
    if (from instanceof TypeVariable) {
      mappings.put((TypeVariable<?>) from, to);
    } else if (from instanceof GenericArrayType) {
      populateTypeMappings(mappings,
          ((GenericArrayType) from).getGenericComponentType(), Types.getComponentType(to));
    } else if (from instanceof ParameterizedType) {
      Type[] fromArgs = ((ParameterizedType) from).getActualTypeArguments();
      Type[] toArgs = ((ParameterizedType) to).getActualTypeArguments();
      checkArgument(fromArgs.length == toArgs.length);
      for (int i = 0; i < fromArgs.length; i++) {
        populateTypeMappings(mappings, fromArgs[i], toArgs[i]);
      }
    }
  }
  
  /**
   * Resolves all type variables in {@code type} and all downstream types and
   * returns a corresponding type with type variables resolved.
   */
  final Type resolve(Type type) {
    if (type instanceof TypeVariable<?>) {
      return resolveTypeVariable((TypeVariable<?>) type);
    } else if (type instanceof ParameterizedType) {
      return resolveParameterizedType((ParameterizedType) type);
    } else if (type instanceof GenericArrayType) {
      return resolveGenericArrayType((GenericArrayType) type);
    } else if (type instanceof WildcardType) {
      WildcardType wildcardType = (WildcardType) type;
      return new Types.WildcardTypeImpl(
          resolve(wildcardType.getLowerBounds()),
          resolve(wildcardType.getUpperBounds()));
    } else {
      // if Class<?>, no resolution needed, we are done.
      return type;
    }
  } 
  
  private Type[] resolve(Type[] types) {
    Type[] result = new Type[types.length];
    for (int i = 0; i < types.length; i++) {
      result[i] = resolve(types[i]);
    }
    return result;
  }
  
  private Type resolveGenericArrayType(GenericArrayType type) {
    Type componentType = resolve(type.getGenericComponentType());
    return Types.newArrayType(componentType);
  }
  
  private Type resolveTypeVariable(final TypeVariable<?> var) {
    final TypeResolver unguarded = this;
    TypeResolver guarded = new TypeResolver(typeTable) {
      @Override Type resolveTypeVariable(
          TypeVariable<?> intermediateVar, TypeResolver guardedResolver) {
        if (intermediateVar.getGenericDeclaration().equals(var.getGenericDeclaration())) {
          return intermediateVar;
        }
        return unguarded.resolveTypeVariable(intermediateVar, guardedResolver);
      }
    };
    return resolveTypeVariable(var, guarded);
  }
  
  /**
   * Resolves {@code var} using the encapsulated type mapping. If it maps to yet another
   * non-reified type, {@code guardedResolver} is used to do further resolution, which doesn't try
   * to resolve any type variable on generic declarations that are already being resolved.
   */
  Type resolveTypeVariable(TypeVariable<?> var, TypeResolver guardedResolver) {
    Type type = typeTable.get(var);
    if (type == null) {
      Type[] bounds = var.getBounds();
      if (bounds.length == 0) {
        return var;
      }
      return Types.newTypeVariable(
          var.getGenericDeclaration(),
          var.getName(),
          guardedResolver.resolve(bounds));
    }
    return guardedResolver.resolve(type); // in case the type is yet another type variable.
  }
  
  private ParameterizedType resolveParameterizedType(ParameterizedType type) {
    Type owner = type.getOwnerType();
    Type resolvedOwner = (owner == null) ? null : resolve(owner);
    Type resolvedRawType = resolve(type.getRawType());
    
    Type[] vars = type.getActualTypeArguments();
    Type[] resolvedArgs = new Type[vars.length];
    for (int i = 0; i < vars.length; i++) {
      resolvedArgs[i] = resolve(vars[i]);
    }
    return Types.newParameterizedTypeWithOwner(
        resolvedOwner, (Class<?>) resolvedRawType, resolvedArgs);
  }
  
  private static final class TypeMappingIntrospector {
    
    private static final WildcardCapturer wildcardCapturer = new WildcardCapturer();

    private final Map<TypeVariable<?>, Type> mappings = Maps.newHashMap();
    private final Set<Type> introspectedTypes = Sets.newHashSet();
    
    /**
     * Returns type mappings using type parameters and type arguments found in
     * the generic superclass and the super interfaces of {@code contextClass}.
     */
    static ImmutableMap<TypeVariable<?>, Type> getTypeMappings(
        Type contextType) {
      TypeMappingIntrospector introspector = new TypeMappingIntrospector();
      introspector.introspect(wildcardCapturer.capture(contextType));
      return ImmutableMap.copyOf(introspector.mappings);
    }
    
    private void introspect(Type type) {
      if (!introspectedTypes.add(type)) {
        return;
      }
      if (type instanceof ParameterizedType) {
        introspectParameterizedType((ParameterizedType) type);
      } else if (type instanceof Class<?>) {
        introspectClass((Class<?>) type);
      } else if (type instanceof TypeVariable) {
        for (Type bound : ((TypeVariable<?>) type).getBounds()) {
          introspect(bound);
        }
      } else if (type instanceof WildcardType) {
        for (Type bound : ((WildcardType) type).getUpperBounds()) {
          introspect(bound);
        }
      }
    }

    private void introspectClass(Class<?> clazz) {
      introspect(clazz.getGenericSuperclass());
      for (Type interfaceType : clazz.getGenericInterfaces()) {
        introspect(interfaceType);
      }
    }
    
    private void introspectParameterizedType(
        ParameterizedType parameterizedType) {
      Class<?> rawClass = (Class<?>) parameterizedType.getRawType();
      TypeVariable<?>[] vars = rawClass.getTypeParameters();
      Type[] typeArgs = parameterizedType.getActualTypeArguments();
      checkState(vars.length == typeArgs.length);
      for (int i = 0; i < vars.length; i++) {
        map(vars[i], typeArgs[i]);
      }
      introspectClass(rawClass);
      introspect(parameterizedType.getOwnerType());
    }
    
    private void map(final TypeVariable<?> var, final Type arg) {
      if (mappings.containsKey(var)) {
        // Mapping already established
        // This is possible when following both superClass -> enclosingClass
        // and enclosingclass -> superClass paths.
        // Since we follow the path of superclass first, enclosing second,
        // superclass mapping should take precedence.
        return;
      }
      // First, check whether var -> arg forms a cycle
      for (Type t = arg; t != null; t = mappings.get(t)) {
        if (var.equals(t)) {
          // cycle detected, remove the entire cycle from the mapping so that
          // each type variable resolves deterministically to itself.
          // Otherwise, a F -> T cycle will end up resolving both F and T
          // nondeterministically to either F or T.
          for (Type x = arg; x != null; x = mappings.remove(x)) {}
          return;
        }
      }
      mappings.put(var, arg);
    }
  }

  // This is needed when resolving types against a context with wildcards
  // For example:
  // class Holder<T> {
  //   void set(T data) {...}
  // }
  // Holder<List<?>> should *not* resolve the set() method to set(List<?> data).
  // Instead, it should create a capture of the wildcard so that set() rejects any List<T>.
  private static final class WildcardCapturer {

    private final AtomicInteger id = new AtomicInteger();

    Type capture(Type type) {
      checkNotNull(type);
      if (type instanceof Class) {
        return type;
      }
      if (type instanceof TypeVariable) {
        return type;
      }
      if (type instanceof GenericArrayType) {
        GenericArrayType arrayType = (GenericArrayType) type;
        return Types.newArrayType(capture(arrayType.getGenericComponentType()));
      }
      if (type instanceof ParameterizedType) {
        ParameterizedType parameterizedType = (ParameterizedType) type;
        return Types.newParameterizedTypeWithOwner(
            captureNullable(parameterizedType.getOwnerType()),
            (Class<?>) parameterizedType.getRawType(),
            capture(parameterizedType.getActualTypeArguments()));
      }
      if (type instanceof WildcardType) {
        WildcardType wildcardType = (WildcardType) type;
        Type[] lowerBounds = wildcardType.getLowerBounds();
        if (lowerBounds.length == 0) { // ? extends something changes to capture-of
          Type[] upperBounds = wildcardType.getUpperBounds();
          String name = "capture#" + id.incrementAndGet() + "-of ? extends "
              + Joiner.on('&').join(upperBounds);
          return Types.newTypeVariable(
              WildcardCapturer.class, name, wildcardType.getUpperBounds());
        } else {
          // TODO(benyu): handle ? super T somehow.
          return type;
        }
      }
      throw new AssertionError("must have been one of the known types");
    }
    
    private Type captureNullable(@Nullable Type type) {
      if (type == null) {
        return null;
      }
      return capture(type);
    }

    private Type[] capture(Type[] types) {
      Type[] result = new Type[types.length];
      for (int i = 0; i < types.length; i++) {
        result[i] = capture(types[i]);
      }
      return result;
    }
  }
}
