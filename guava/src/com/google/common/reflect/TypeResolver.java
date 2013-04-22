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

import com.google.common.annotations.Beta;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.Nullable;

/**
 * An object of this class encapsulates type mappings from type variables. Mappings are established
 * with {@link #where} and types are resolved using {@link #resolveType}.
 *
 * <p>Note that usually type mappings are already implied by the static type hierarchy (for example,
 * the {@code E} type variable declared by class {@code List} naturally maps to {@code String} in
 * the context of {@code class MyStringList implements List<String>}. In such case, prefer to use
 * {@link TypeToken#resolveType} since it's simpler and more type safe. This class should only be
 * used when the type mapping isn't implied by the static type hierarchy, but provided through other
 * means such as an annotation or external configuration file.
 *
 * @author Ben Yu
 * @since 15.0
 */
@Beta
public final class TypeResolver {

  private final TypeTable typeTable;

  public TypeResolver() {
    this.typeTable = new TypeTable();
  }

  private TypeResolver(TypeTable typeTable) {
    this.typeTable = typeTable;
  }

  static TypeResolver accordingTo(Type type) {
    return new TypeResolver().where(TypeMappingIntrospector.getTypeMappings(type));
  }

  /**
   * Returns a new {@code TypeResolver} with type variables in {@code formal} mapping to types in
   * {@code actual}.
   *
   * <p>For example, if {@code formal} is a {@code TypeVariable T}, and {@code actual} is {@code
   * String.class}, then {@code new TypeResolver().where(formal, actual)} will {@linkplain
   * #resolveType resolve} {@code ParameterizedType List<T>} to {@code List<String>}, and resolve
   * {@code Map<T, Something>} to {@code Map<String, Something>} etc. Similarly, {@code formal} and
   * {@code actual} can be {@code Map<K, V>} and {@code Map<String, Integer>} respectively, or they
   * can be {@code E[]} and {@code String[]} respectively, or even any arbitrary combination
   * thereof.
   *
   * @param formal The type whose type variables or itself is mapped to other type(s). It's almost
   *        always a bug if {@code formal} isn't a type variable and contains no type variable. Make
   *        sure you are passing the two parameters in the right order.
   * @param actual The type that the formal type variable(s) are mapped to. It can be or contain yet
   *        other type variables, in which case these type variables will be further resolved if
   *        corresponding mappings exist in the current {@code TypeResolver} instance.
   */
  public TypeResolver where(Type formal, Type actual) {
    Map<TypeVariable<?>, Type> mappings = Maps.newHashMap();
    populateTypeMappings(mappings, checkNotNull(formal), checkNotNull(actual));
    return where(mappings);
  }

  /** Returns a new {@code TypeResolver} with {@code variable} mapping to {@code type}. */
  TypeResolver where(Map<? extends TypeVariable<?>, ? extends Type> mappings) {
    return new TypeResolver(typeTable.where(mappings));
  }

  private static void populateTypeMappings(
      final Map<TypeVariable<?>, Type> mappings, Type from, final Type to) {
    if (from.equals(to)) {
      return;
    }
    new TypeVisitor() {
      @Override void visitTypeVariable(TypeVariable<?> typeVariable) {
        mappings.put(typeVariable, to);
      }
      @Override void visitWildcardType(WildcardType fromWildcardType) {
        WildcardType toWildcardType = expectArgument(WildcardType.class, to);
        Type[] fromUpperBounds = fromWildcardType.getUpperBounds();
        Type[] toUpperBounds = toWildcardType.getUpperBounds();
        Type[] fromLowerBounds = fromWildcardType.getLowerBounds();
        Type[] toLowerBounds = toWildcardType.getLowerBounds();
        checkArgument(
            fromUpperBounds.length == toUpperBounds.length
                && fromLowerBounds.length == toLowerBounds.length,
            "Incompatible type: %s vs. %s", fromWildcardType, to);
        for (int i = 0; i < fromUpperBounds.length; i++) {
          populateTypeMappings(mappings, fromUpperBounds[i], toUpperBounds[i]);
        }
        for (int i = 0; i < fromLowerBounds.length; i++) {
          populateTypeMappings(mappings, fromLowerBounds[i], toLowerBounds[i]);
        }
      }
      @Override void visitParameterizedType(ParameterizedType fromParameterizedType) {
        ParameterizedType toParameterizedType = expectArgument(ParameterizedType.class, to);
        checkArgument(fromParameterizedType.getRawType().equals(toParameterizedType.getRawType()),
            "Inconsistent raw type: %s vs. %s", fromParameterizedType, to);
        Type[] fromArgs = fromParameterizedType.getActualTypeArguments();
        Type[] toArgs = toParameterizedType.getActualTypeArguments();
        checkArgument(fromArgs.length == toArgs.length,
            "%s not compatible with %s", fromParameterizedType, toParameterizedType);
        for (int i = 0; i < fromArgs.length; i++) {
          populateTypeMappings(mappings, fromArgs[i], toArgs[i]);
        }
      }
      @Override void visitGenericArrayType(GenericArrayType fromArrayType) {
        Type componentType = Types.getComponentType(to);
        checkArgument(componentType != null, "%s is not an array type.", to);
        populateTypeMappings(mappings, fromArrayType.getGenericComponentType(), componentType);
      }
      @Override void visitClass(Class<?> fromClass) {
        // Can't map from a raw class to anything other than itself.
        // You can't say "assuming String is Integer".
        // And we don't support "assuming String is T"; user has to say "assuming T is String". 
        throw new IllegalArgumentException("No type mapping from " + fromClass);
      }
    }.visit(from);
  }

  /**
   * Resolves all type variables in {@code type} and all downstream types and
   * returns a corresponding type with type variables resolved.
   */
  public Type resolveType(Type type) {
    checkNotNull(type);
    if (type instanceof TypeVariable) {
      return typeTable.resolve((TypeVariable<?>) type);
    } else if (type instanceof ParameterizedType) {
      return resolveParameterizedType((ParameterizedType) type);
    } else if (type instanceof GenericArrayType) {
      return resolveGenericArrayType((GenericArrayType) type);
    } else if (type instanceof WildcardType) {
      WildcardType wildcardType = (WildcardType) type;
      return new Types.WildcardTypeImpl(
          resolveTypes(wildcardType.getLowerBounds()),
          resolveTypes(wildcardType.getUpperBounds()));
    } else {
      // if Class<?>, no resolution needed, we are done.
      return type;
    }
  }

  private Type[] resolveTypes(Type[] types) {
    Type[] result = new Type[types.length];
    for (int i = 0; i < types.length; i++) {
      result[i] = resolveType(types[i]);
    }
    return result;
  }

  private Type resolveGenericArrayType(GenericArrayType type) {
    Type componentType = resolveType(type.getGenericComponentType());
    return Types.newArrayType(componentType);
  }

  private ParameterizedType resolveParameterizedType(ParameterizedType type) {
    Type owner = type.getOwnerType();
    Type resolvedOwner = (owner == null) ? null : resolveType(owner);
    Type resolvedRawType = resolveType(type.getRawType());

    Type[] vars = type.getActualTypeArguments();
    Type[] resolvedArgs = new Type[vars.length];
    for (int i = 0; i < vars.length; i++) {
      resolvedArgs[i] = resolveType(vars[i]);
    }
    return Types.newParameterizedTypeWithOwner(
        resolvedOwner, (Class<?>) resolvedRawType, resolvedArgs);
  }

  private static <T> T expectArgument(Class<T> type, Object arg) {
    try {
      return type.cast(arg);
    } catch (ClassCastException e) {
      throw new IllegalArgumentException(arg + " is not a " + type.getSimpleName());
    }
  }

  /** A TypeTable maintains mapping from {@link TypeVariable} to types. */
  private static class TypeTable {
    private final ImmutableMap<TypeVariable<?>, Type> map;
  
    TypeTable() {
      this.map = ImmutableMap.of();
    }
    
    private TypeTable(ImmutableMap<TypeVariable<?>, Type> map) {
      this.map = map;
    }

    /** Returns a new {@code TypeResolver} with {@code variable} mapping to {@code type}. */
    final TypeTable where(Map<? extends TypeVariable<?>, ? extends Type> mappings) {
      ImmutableMap.Builder<TypeVariable<?>, Type> builder = ImmutableMap.builder();
      builder.putAll(map);
      for (Map.Entry<? extends TypeVariable<?>, ? extends Type> mapping : mappings.entrySet()) {
        TypeVariable<?> variable = mapping.getKey();
        Type type = mapping.getValue();
        checkArgument(!variable.equals(type), "Type variable %s bound to itself", variable);
        builder.put(variable, type);
      }
      return new TypeTable(builder.build());
    }

    final Type resolve(final TypeVariable<?> var) {
      final TypeTable unguarded = this;
      TypeTable guarded = new TypeTable() {
        @Override public Type resolveInternal(
            TypeVariable<?> intermediateVar, TypeTable forDependent) {
          if (intermediateVar.getGenericDeclaration().equals(var.getGenericDeclaration())) {
            return intermediateVar;
          }
          return unguarded.resolveInternal(intermediateVar, forDependent);
        }
      };
      return resolveInternal(var, guarded);
    }

    /**
     * Resolves {@code var} using the encapsulated type mapping. If it maps to yet another
     * non-reified type or has bounds, {@code forDependants} is used to do further resolution, which
     * doesn't try to resolve any type variable on generic declarations that are already being
     * resolved.
     *
     * <p>Should only be called and overridden by {@link #resolve(TypeVariable)}.
     */
    Type resolveInternal(TypeVariable<?> var, TypeTable forDependants) {
      Type type = map.get(var);
      if (type == null) {
        Type[] bounds = var.getBounds();
        if (bounds.length == 0) {
          return var;
        }
        return Types.newTypeVariable(
            var.getGenericDeclaration(),
            var.getName(),
            new TypeResolver(forDependants).resolveTypes(bounds));
      }
      // in case the type is yet another type variable.
      return new TypeResolver(forDependants).resolveType(type);
    }
  }

  private static final class TypeMappingIntrospector extends TypeVisitor {

    private static final WildcardCapturer wildcardCapturer = new WildcardCapturer();

    private final Map<TypeVariable<?>, Type> mappings = Maps.newHashMap();

    /**
     * Returns type mappings using type parameters and type arguments found in
     * the generic superclass and the super interfaces of {@code contextClass}.
     */
    static ImmutableMap<TypeVariable<?>, Type> getTypeMappings(
        Type contextType) {
      TypeMappingIntrospector introspector = new TypeMappingIntrospector();
      introspector.visit(wildcardCapturer.capture(contextType));
      return ImmutableMap.copyOf(introspector.mappings);
    }

    @Override void visitClass(Class<?> clazz) {
      visit(clazz.getGenericSuperclass());
      visit(clazz.getGenericInterfaces());
    }

    @Override void visitParameterizedType(ParameterizedType parameterizedType) {
      Class<?> rawClass = (Class<?>) parameterizedType.getRawType();
      TypeVariable<?>[] vars = rawClass.getTypeParameters();
      Type[] typeArgs = parameterizedType.getActualTypeArguments();
      checkState(vars.length == typeArgs.length);
      for (int i = 0; i < vars.length; i++) {
        map(vars[i], typeArgs[i]);
      }
      visit(rawClass);
      visit(parameterizedType.getOwnerType());
    }

    @Override void visitTypeVariable(TypeVariable<?> t) {
      visit(t.getBounds());
    }

    @Override void visitWildcardType(WildcardType t) {
      visit(t.getUpperBounds());
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
