/*
 * Copyright (C) 2006 The Guava Authors
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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import com.google.common.annotations.Beta;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Joiner;
import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ForwardingSet;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.collect.Ordering;
import com.google.common.primitives.Primitives;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.checkerframework.checker.nullness.compatqual.MonotonicNonNullDecl;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;

/**
 * A {@link Type} with generics.
 *
 * <p>Operations that are otherwise only available in {@link Class} are implemented to support
 * {@code Type}, for example {@link #isSubtypeOf}, {@link #isArray} and {@link #getComponentType}.
 * It also provides additional utilities such as {@link #getTypes}, {@link #resolveType}, etc.
 *
 * <p>There are three ways to get a {@code TypeToken} instance:
 *
 * <ul>
 *   <li>Wrap a {@code Type} obtained via reflection. For example: {@code
 *       TypeToken.of(method.getGenericReturnType())}.
 *   <li>Capture a generic type with a (usually anonymous) subclass. For example:
 *       <pre>{@code
 * new TypeToken<List<String>>() {}
 * }</pre>
 *       <p>Note that it's critical that the actual type argument is carried by a subclass. The
 *       following code is wrong because it only captures the {@code <T>} type variable of the
 *       {@code listType()} method signature; while {@code <String>} is lost in erasure:
 *       <pre>{@code
 * class Util {
 *   static <T> TypeToken<List<T>> listType() {
 *     return new TypeToken<List<T>>() {};
 *   }
 * }
 *
 * TypeToken<List<String>> stringListType = Util.<String>listType();
 * }</pre>
 *   <li>Capture a generic type with a (usually anonymous) subclass and resolve it against a context
 *       class that knows what the type parameters are. For example:
 *       <pre>{@code
 * abstract class IKnowMyType<T> {
 *   TypeToken<T> type = new TypeToken<T>(getClass()) {};
 * }
 * new IKnowMyType<String>() {}.type => String
 * }</pre>
 * </ul>
 *
 * <p>{@code TypeToken} is serializable when no type variable is contained in the type.
 *
 * <p>Note to Guice users: {@code} TypeToken is similar to Guice's {@code TypeLiteral} class except
 * that it is serializable and offers numerous additional utility methods.
 *
 * @author Bob Lee
 * @author Sven Mawson
 * @author Ben Yu
 * @since 12.0
 */
@Beta
@SuppressWarnings("serial") // SimpleTypeToken is the serialized form.
public abstract class TypeToken<T> extends TypeCapture<T> implements Serializable {

  private final Type runtimeType;

  /** Resolver for resolving parameter and field types with {@link #runtimeType} as context. */
  @MonotonicNonNullDecl private transient TypeResolver invariantTypeResolver;

  /** Resolver for resolving covariant types with {@link #runtimeType} as context. */
  @MonotonicNonNullDecl private transient TypeResolver covariantTypeResolver;

  /**
   * Constructs a new type token of {@code T}.
   *
   * <p>Clients create an empty anonymous subclass. Doing so embeds the type parameter in the
   * anonymous class's type hierarchy so we can reconstitute it at runtime despite erasure.
   *
   * <p>For example:
   *
   * <pre>{@code
   * TypeToken<List<String>> t = new TypeToken<List<String>>() {};
   * }</pre>
   */
  protected TypeToken() {
    this.runtimeType = capture();
    checkState(
        !(runtimeType instanceof TypeVariable),
        "Cannot construct a TypeToken for a type variable.\n"
            + "You probably meant to call new TypeToken<%s>(getClass()) "
            + "that can resolve the type variable for you.\n"
            + "If you do need to create a TypeToken of a type variable, "
            + "please use TypeToken.of() instead.",
        runtimeType);
  }

  /**
   * Constructs a new type token of {@code T} while resolving free type variables in the context of
   * {@code declaringClass}.
   *
   * <p>Clients create an empty anonymous subclass. Doing so embeds the type parameter in the
   * anonymous class's type hierarchy so we can reconstitute it at runtime despite erasure.
   *
   * <p>For example:
   *
   * <pre>{@code
   * abstract class IKnowMyType<T> {
   *   TypeToken<T> getMyType() {
   *     return new TypeToken<T>(getClass()) {};
   *   }
   * }
   *
   * new IKnowMyType<String>() {}.getMyType() => String
   * }</pre>
   */
  protected TypeToken(Class<?> declaringClass) {
    Type captured = super.capture();
    if (captured instanceof Class) {
      this.runtimeType = captured;
    } else {
      this.runtimeType = TypeResolver.covariantly(declaringClass).resolveType(captured);
    }
  }

  private TypeToken(Type type) {
    this.runtimeType = checkNotNull(type);
  }

  /** Returns an instance of type token that wraps {@code type}. */
  public static <T> TypeToken<T> of(Class<T> type) {
    return new SimpleTypeToken<T>(type);
  }

  /** Returns an instance of type token that wraps {@code type}. */
  public static TypeToken<?> of(Type type) {
    return new SimpleTypeToken<>(type);
  }

  /**
   * Returns the raw type of {@code T}. Formally speaking, if {@code T} is returned by {@link
   * java.lang.reflect.Method#getGenericReturnType}, the raw type is what's returned by {@link
   * java.lang.reflect.Method#getReturnType} of the same method object. Specifically:
   *
   * <ul>
   *   <li>If {@code T} is a {@code Class} itself, {@code T} itself is returned.
   *   <li>If {@code T} is a {@link ParameterizedType}, the raw type of the parameterized type is
   *       returned.
   *   <li>If {@code T} is a {@link GenericArrayType}, the returned type is the corresponding array
   *       class. For example: {@code List<Integer>[] => List[]}.
   *   <li>If {@code T} is a type variable or a wildcard type, the raw type of the first upper bound
   *       is returned. For example: {@code <X extends Foo> => Foo}.
   * </ul>
   */
  public final Class<? super T> getRawType() {
    // For wildcard or type variable, the first bound determines the runtime type.
    Class<?> rawType = getRawTypes().iterator().next();
    @SuppressWarnings("unchecked") // raw type is |T|
    Class<? super T> result = (Class<? super T>) rawType;
    return result;
  }

  /** Returns the represented type. */
  public final Type getType() {
    return runtimeType;
  }

  /**
   * Returns a new {@code TypeToken} where type variables represented by {@code typeParam} are
   * substituted by {@code typeArg}. For example, it can be used to construct {@code Map<K, V>} for
   * any {@code K} and {@code V} type:
   *
   * <pre>{@code
   * static <K, V> TypeToken<Map<K, V>> mapOf(
   *     TypeToken<K> keyType, TypeToken<V> valueType) {
   *   return new TypeToken<Map<K, V>>() {}
   *       .where(new TypeParameter<K>() {}, keyType)
   *       .where(new TypeParameter<V>() {}, valueType);
   * }
   * }</pre>
   *
   * @param <X> The parameter type
   * @param typeParam the parameter type variable
   * @param typeArg the actual type to substitute
   */
  public final <X> TypeToken<T> where(TypeParameter<X> typeParam, TypeToken<X> typeArg) {
    TypeResolver resolver =
        new TypeResolver()
            .where(
                ImmutableMap.of(
                    new TypeResolver.TypeVariableKey(typeParam.typeVariable), typeArg.runtimeType));
    // If there's any type error, we'd report now rather than later.
    return new SimpleTypeToken<T>(resolver.resolveType(runtimeType));
  }

  /**
   * Returns a new {@code TypeToken} where type variables represented by {@code typeParam} are
   * substituted by {@code typeArg}. For example, it can be used to construct {@code Map<K, V>} for
   * any {@code K} and {@code V} type:
   *
   * <pre>{@code
   * static <K, V> TypeToken<Map<K, V>> mapOf(
   *     Class<K> keyType, Class<V> valueType) {
   *   return new TypeToken<Map<K, V>>() {}
   *       .where(new TypeParameter<K>() {}, keyType)
   *       .where(new TypeParameter<V>() {}, valueType);
   * }
   * }</pre>
   *
   * @param <X> The parameter type
   * @param typeParam the parameter type variable
   * @param typeArg the actual type to substitute
   */
  public final <X> TypeToken<T> where(TypeParameter<X> typeParam, Class<X> typeArg) {
    return where(typeParam, of(typeArg));
  }

  /**
   * Resolves the given {@code type} against the type context represented by this type. For example:
   *
   * <pre>{@code
   * new TypeToken<List<String>>() {}.resolveType(
   *     List.class.getMethod("get", int.class).getGenericReturnType())
   * => String.class
   * }</pre>
   */
  public final TypeToken<?> resolveType(Type type) {
    checkNotNull(type);
    // Being conservative here because the user could use resolveType() to resolve a type in an
    // invariant context.
    return of(getInvariantTypeResolver().resolveType(type));
  }

  private TypeToken<?> resolveSupertype(Type type) {
    TypeToken<?> supertype = of(getCovariantTypeResolver().resolveType(type));
    // super types' type mapping is a subset of type mapping of this type.
    supertype.covariantTypeResolver = covariantTypeResolver;
    supertype.invariantTypeResolver = invariantTypeResolver;
    return supertype;
  }

  /**
   * Returns the generic superclass of this type or {@code null} if the type represents {@link
   * Object} or an interface. This method is similar but different from {@link
   * Class#getGenericSuperclass}. For example, {@code new TypeToken<StringArrayList>()
   * {}.getGenericSuperclass()} will return {@code new TypeToken<ArrayList<String>>() {}}; while
   * {@code StringArrayList.class.getGenericSuperclass()} will return {@code ArrayList<E>}, where
   * {@code E} is the type variable declared by class {@code ArrayList}.
   *
   * <p>If this type is a type variable or wildcard, its first upper bound is examined and returned
   * if the bound is a class or extends from a class. This means that the returned type could be a
   * type variable too.
   */
  @NullableDecl
  final TypeToken<? super T> getGenericSuperclass() {
    if (runtimeType instanceof TypeVariable) {
      // First bound is always the super class, if one exists.
      return boundAsSuperclass(((TypeVariable<?>) runtimeType).getBounds()[0]);
    }
    if (runtimeType instanceof WildcardType) {
      // wildcard has one and only one upper bound.
      return boundAsSuperclass(((WildcardType) runtimeType).getUpperBounds()[0]);
    }
    Type superclass = getRawType().getGenericSuperclass();
    if (superclass == null) {
      return null;
    }
    @SuppressWarnings("unchecked") // super class of T
    TypeToken<? super T> superToken = (TypeToken<? super T>) resolveSupertype(superclass);
    return superToken;
  }

  @NullableDecl
  private TypeToken<? super T> boundAsSuperclass(Type bound) {
    TypeToken<?> token = of(bound);
    if (token.getRawType().isInterface()) {
      return null;
    }
    @SuppressWarnings("unchecked") // only upper bound of T is passed in.
    TypeToken<? super T> superclass = (TypeToken<? super T>) token;
    return superclass;
  }

  /**
   * Returns the generic interfaces that this type directly {@code implements}. This method is
   * similar but different from {@link Class#getGenericInterfaces()}. For example, {@code new
   * TypeToken<List<String>>() {}.getGenericInterfaces()} will return a list that contains {@code
   * new TypeToken<Iterable<String>>() {}}; while {@code List.class.getGenericInterfaces()} will
   * return an array that contains {@code Iterable<T>}, where the {@code T} is the type variable
   * declared by interface {@code Iterable}.
   *
   * <p>If this type is a type variable or wildcard, its upper bounds are examined and those that
   * are either an interface or upper-bounded only by interfaces are returned. This means that the
   * returned types could include type variables too.
   */
  final ImmutableList<TypeToken<? super T>> getGenericInterfaces() {
    if (runtimeType instanceof TypeVariable) {
      return boundsAsInterfaces(((TypeVariable<?>) runtimeType).getBounds());
    }
    if (runtimeType instanceof WildcardType) {
      return boundsAsInterfaces(((WildcardType) runtimeType).getUpperBounds());
    }
    ImmutableList.Builder<TypeToken<? super T>> builder = ImmutableList.builder();
    for (Type interfaceType : getRawType().getGenericInterfaces()) {
      @SuppressWarnings("unchecked") // interface of T
      TypeToken<? super T> resolvedInterface =
          (TypeToken<? super T>) resolveSupertype(interfaceType);
      builder.add(resolvedInterface);
    }
    return builder.build();
  }

  private ImmutableList<TypeToken<? super T>> boundsAsInterfaces(Type[] bounds) {
    ImmutableList.Builder<TypeToken<? super T>> builder = ImmutableList.builder();
    for (Type bound : bounds) {
      @SuppressWarnings("unchecked") // upper bound of T
      TypeToken<? super T> boundType = (TypeToken<? super T>) of(bound);
      if (boundType.getRawType().isInterface()) {
        builder.add(boundType);
      }
    }
    return builder.build();
  }

  /**
   * Returns the set of interfaces and classes that this type is or is a subtype of. The returned
   * types are parameterized with proper type arguments.
   *
   * <p>Subtypes are always listed before supertypes. But the reverse is not true. A type isn't
   * necessarily a subtype of all the types following. Order between types without subtype
   * relationship is arbitrary and not guaranteed.
   *
   * <p>If this type is a type variable or wildcard, upper bounds that are themselves type variables
   * aren't included (their super interfaces and superclasses are).
   */
  public final TypeSet getTypes() {
    return new TypeSet();
  }

  /**
   * Returns the generic form of {@code superclass}. For example, if this is {@code
   * ArrayList<String>}, {@code Iterable<String>} is returned given the input {@code
   * Iterable.class}.
   */
  public final TypeToken<? super T> getSupertype(Class<? super T> superclass) {
    checkArgument(
        this.someRawTypeIsSubclassOf(superclass),
        "%s is not a super class of %s",
        superclass,
        this);
    if (runtimeType instanceof TypeVariable) {
      return getSupertypeFromUpperBounds(superclass, ((TypeVariable<?>) runtimeType).getBounds());
    }
    if (runtimeType instanceof WildcardType) {
      return getSupertypeFromUpperBounds(superclass, ((WildcardType) runtimeType).getUpperBounds());
    }
    if (superclass.isArray()) {
      return getArraySupertype(superclass);
    }
    @SuppressWarnings("unchecked") // resolved supertype
    TypeToken<? super T> supertype =
        (TypeToken<? super T>) resolveSupertype(toGenericType(superclass).runtimeType);
    return supertype;
  }

  /**
   * Returns subtype of {@code this} with {@code subclass} as the raw class. For example, if this is
   * {@code Iterable<String>} and {@code subclass} is {@code List}, {@code List<String>} is
   * returned.
   */
  public final TypeToken<? extends T> getSubtype(Class<?> subclass) {
    checkArgument(
        !(runtimeType instanceof TypeVariable), "Cannot get subtype of type variable <%s>", this);
    if (runtimeType instanceof WildcardType) {
      return getSubtypeFromLowerBounds(subclass, ((WildcardType) runtimeType).getLowerBounds());
    }
    // unwrap array type if necessary
    if (isArray()) {
      return getArraySubtype(subclass);
    }
    // At this point, it's either a raw class or parameterized type.
    checkArgument(
        getRawType().isAssignableFrom(subclass), "%s isn't a subclass of %s", subclass, this);
    Type resolvedTypeArgs = resolveTypeArgsForSubclass(subclass);
    @SuppressWarnings("unchecked") // guarded by the isAssignableFrom() statement above
    TypeToken<? extends T> subtype = (TypeToken<? extends T>) of(resolvedTypeArgs);
    checkArgument(
        subtype.isSubtypeOf(this), "%s does not appear to be a subtype of %s", subtype, this);
    return subtype;
  }

  /**
   * Returns true if this type is a supertype of the given {@code type}. "Supertype" is defined
   * according to <a
   * href="http://docs.oracle.com/javase/specs/jls/se8/html/jls-4.html#jls-4.5.1">the rules for type
   * arguments</a> introduced with Java generics.
   *
   * @since 19.0
   */
  public final boolean isSupertypeOf(TypeToken<?> type) {
    return type.isSubtypeOf(getType());
  }

  /**
   * Returns true if this type is a supertype of the given {@code type}. "Supertype" is defined
   * according to <a
   * href="http://docs.oracle.com/javase/specs/jls/se8/html/jls-4.html#jls-4.5.1">the rules for type
   * arguments</a> introduced with Java generics.
   *
   * @since 19.0
   */
  public final boolean isSupertypeOf(Type type) {
    return of(type).isSubtypeOf(getType());
  }

  /**
   * Returns true if this type is a subtype of the given {@code type}. "Subtype" is defined
   * according to <a
   * href="http://docs.oracle.com/javase/specs/jls/se8/html/jls-4.html#jls-4.5.1">the rules for type
   * arguments</a> introduced with Java generics.
   *
   * @since 19.0
   */
  public final boolean isSubtypeOf(TypeToken<?> type) {
    return isSubtypeOf(type.getType());
  }

  /**
   * Returns true if this type is a subtype of the given {@code type}. "Subtype" is defined
   * according to <a
   * href="http://docs.oracle.com/javase/specs/jls/se8/html/jls-4.html#jls-4.5.1">the rules for type
   * arguments</a> introduced with Java generics.
   *
   * @since 19.0
   */
  public final boolean isSubtypeOf(Type supertype) {
    checkNotNull(supertype);
    if (supertype instanceof WildcardType) {
      // if 'supertype' is <? super Foo>, 'this' can be:
      // Foo, SubFoo, <? extends Foo>.
      // if 'supertype' is <? extends Foo>, nothing is a subtype.
      return any(((WildcardType) supertype).getLowerBounds()).isSupertypeOf(runtimeType);
    }
    // if 'this' is wildcard, it's a suptype of to 'supertype' if any of its "extends"
    // bounds is a subtype of 'supertype'.
    if (runtimeType instanceof WildcardType) {
      // <? super Base> is of no use in checking 'from' being a subtype of 'to'.
      return any(((WildcardType) runtimeType).getUpperBounds()).isSubtypeOf(supertype);
    }
    // if 'this' is type variable, it's a subtype if any of its "extends"
    // bounds is a subtype of 'supertype'.
    if (runtimeType instanceof TypeVariable) {
      return runtimeType.equals(supertype)
          || any(((TypeVariable<?>) runtimeType).getBounds()).isSubtypeOf(supertype);
    }
    if (runtimeType instanceof GenericArrayType) {
      return of(supertype).isSupertypeOfArray((GenericArrayType) runtimeType);
    }
    // Proceed to regular Type subtype check
    if (supertype instanceof Class) {
      return this.someRawTypeIsSubclassOf((Class<?>) supertype);
    } else if (supertype instanceof ParameterizedType) {
      return this.isSubtypeOfParameterizedType((ParameterizedType) supertype);
    } else if (supertype instanceof GenericArrayType) {
      return this.isSubtypeOfArrayType((GenericArrayType) supertype);
    } else { // to instanceof TypeVariable
      return false;
    }
  }

  /**
   * Returns true if this type is known to be an array type, such as {@code int[]}, {@code T[]},
   * {@code <? extends Map<String, Integer>[]>} etc.
   */
  public final boolean isArray() {
    return getComponentType() != null;
  }

  /**
   * Returns true if this type is one of the nine primitive types (including {@code void}).
   *
   * @since 15.0
   */
  public final boolean isPrimitive() {
    return (runtimeType instanceof Class) && ((Class<?>) runtimeType).isPrimitive();
  }

  /**
   * Returns the corresponding wrapper type if this is a primitive type; otherwise returns {@code
   * this} itself. Idempotent.
   *
   * @since 15.0
   */
  public final TypeToken<T> wrap() {
    if (isPrimitive()) {
      @SuppressWarnings("unchecked") // this is a primitive class
      Class<T> type = (Class<T>) runtimeType;
      return of(Primitives.wrap(type));
    }
    return this;
  }

  private boolean isWrapper() {
    return Primitives.allWrapperTypes().contains(runtimeType);
  }

  /**
   * Returns the corresponding primitive type if this is a wrapper type; otherwise returns {@code
   * this} itself. Idempotent.
   *
   * @since 15.0
   */
  public final TypeToken<T> unwrap() {
    if (isWrapper()) {
      @SuppressWarnings("unchecked") // this is a wrapper class
      Class<T> type = (Class<T>) runtimeType;
      return of(Primitives.unwrap(type));
    }
    return this;
  }

  /**
   * Returns the array component type if this type represents an array ({@code int[]}, {@code T[]},
   * {@code <? extends Map<String, Integer>[]>} etc.), or else {@code null} is returned.
   */
  @NullableDecl
  public final TypeToken<?> getComponentType() {
    Type componentType = Types.getComponentType(runtimeType);
    if (componentType == null) {
      return null;
    }
    return of(componentType);
  }

  /**
   * Returns the {@link Invokable} for {@code method}, which must be a member of {@code T}.
   *
   * @since 14.0
   */
  public final Invokable<T, Object> method(Method method) {
    checkArgument(
        this.someRawTypeIsSubclassOf(method.getDeclaringClass()),
        "%s not declared by %s",
        method,
        this);
    return new Invokable.MethodInvokable<T>(method) {
      @Override
      Type getGenericReturnType() {
        return getCovariantTypeResolver().resolveType(super.getGenericReturnType());
      }

      @Override
      Type[] getGenericParameterTypes() {
        return getInvariantTypeResolver().resolveTypesInPlace(super.getGenericParameterTypes());
      }

      @Override
      Type[] getGenericExceptionTypes() {
        return getCovariantTypeResolver().resolveTypesInPlace(super.getGenericExceptionTypes());
      }

      @Override
      public TypeToken<T> getOwnerType() {
        return TypeToken.this;
      }

      @Override
      public String toString() {
        return getOwnerType() + "." + super.toString();
      }
    };
  }

  /**
   * Returns the {@link Invokable} for {@code constructor}, which must be a member of {@code T}.
   *
   * @since 14.0
   */
  public final Invokable<T, T> constructor(Constructor<?> constructor) {
    checkArgument(
        constructor.getDeclaringClass() == getRawType(),
        "%s not declared by %s",
        constructor,
        getRawType());
    return new Invokable.ConstructorInvokable<T>(constructor) {
      @Override
      Type getGenericReturnType() {
        return getCovariantTypeResolver().resolveType(super.getGenericReturnType());
      }

      @Override
      Type[] getGenericParameterTypes() {
        return getInvariantTypeResolver().resolveTypesInPlace(super.getGenericParameterTypes());
      }

      @Override
      Type[] getGenericExceptionTypes() {
        return getCovariantTypeResolver().resolveTypesInPlace(super.getGenericExceptionTypes());
      }

      @Override
      public TypeToken<T> getOwnerType() {
        return TypeToken.this;
      }

      @Override
      public String toString() {
        return getOwnerType() + "(" + Joiner.on(", ").join(getGenericParameterTypes()) + ")";
      }
    };
  }

  /**
   * The set of interfaces and classes that {@code T} is or is a subtype of. {@link Object} is not
   * included in the set if this type is an interface.
   *
   * @since 13.0
   */
  public class TypeSet extends ForwardingSet<TypeToken<? super T>> implements Serializable {

    @MonotonicNonNullDecl private transient ImmutableSet<TypeToken<? super T>> types;

    TypeSet() {}

    /** Returns the types that are interfaces implemented by this type. */
    public TypeSet interfaces() {
      return new InterfaceSet(this);
    }

    /** Returns the types that are classes. */
    public TypeSet classes() {
      return new ClassSet();
    }

    @Override
    protected Set<TypeToken<? super T>> delegate() {
      ImmutableSet<TypeToken<? super T>> filteredTypes = types;
      if (filteredTypes == null) {
        // Java has no way to express ? super T when we parameterize TypeToken vs. Class.
        @SuppressWarnings({"unchecked", "rawtypes"})
        ImmutableList<TypeToken<? super T>> collectedTypes =
            (ImmutableList) TypeCollector.FOR_GENERIC_TYPE.collectTypes(TypeToken.this);
        return (types =
            FluentIterable.from(collectedTypes)
                .filter(TypeFilter.IGNORE_TYPE_VARIABLE_OR_WILDCARD)
                .toSet());
      } else {
        return filteredTypes;
      }
    }

    /** Returns the raw types of the types in this set, in the same order. */
    public Set<Class<? super T>> rawTypes() {
      // Java has no way to express ? super T when we parameterize TypeToken vs. Class.
      @SuppressWarnings({"unchecked", "rawtypes"})
      ImmutableList<Class<? super T>> collectedTypes =
          (ImmutableList) TypeCollector.FOR_RAW_TYPE.collectTypes(getRawTypes());
      return ImmutableSet.copyOf(collectedTypes);
    }

    private static final long serialVersionUID = 0;
  }

  private final class InterfaceSet extends TypeSet {

    private final transient TypeSet allTypes;
    @MonotonicNonNullDecl private transient ImmutableSet<TypeToken<? super T>> interfaces;

    InterfaceSet(TypeSet allTypes) {
      this.allTypes = allTypes;
    }

    @Override
    protected Set<TypeToken<? super T>> delegate() {
      ImmutableSet<TypeToken<? super T>> result = interfaces;
      if (result == null) {
        return (interfaces =
            FluentIterable.from(allTypes).filter(TypeFilter.INTERFACE_ONLY).toSet());
      } else {
        return result;
      }
    }

    @Override
    public TypeSet interfaces() {
      return this;
    }

    @Override
    public Set<Class<? super T>> rawTypes() {
      // Java has no way to express ? super T when we parameterize TypeToken vs. Class.
      @SuppressWarnings({"unchecked", "rawtypes"})
      ImmutableList<Class<? super T>> collectedTypes =
          (ImmutableList) TypeCollector.FOR_RAW_TYPE.collectTypes(getRawTypes());
      return FluentIterable.from(collectedTypes)
          .filter(
              new Predicate<Class<?>>() {
                @Override
                public boolean apply(Class<?> type) {
                  return type.isInterface();
                }
              })
          .toSet();
    }

    @Override
    public TypeSet classes() {
      throw new UnsupportedOperationException("interfaces().classes() not supported.");
    }

    private Object readResolve() {
      return getTypes().interfaces();
    }

    private static final long serialVersionUID = 0;
  }

  private final class ClassSet extends TypeSet {

    @MonotonicNonNullDecl private transient ImmutableSet<TypeToken<? super T>> classes;

    @Override
    protected Set<TypeToken<? super T>> delegate() {
      ImmutableSet<TypeToken<? super T>> result = classes;
      if (result == null) {
        @SuppressWarnings({"unchecked", "rawtypes"})
        ImmutableList<TypeToken<? super T>> collectedTypes =
            (ImmutableList)
                TypeCollector.FOR_GENERIC_TYPE.classesOnly().collectTypes(TypeToken.this);
        return (classes =
            FluentIterable.from(collectedTypes)
                .filter(TypeFilter.IGNORE_TYPE_VARIABLE_OR_WILDCARD)
                .toSet());
      } else {
        return result;
      }
    }

    @Override
    public TypeSet classes() {
      return this;
    }

    @Override
    public Set<Class<? super T>> rawTypes() {
      // Java has no way to express ? super T when we parameterize TypeToken vs. Class.
      @SuppressWarnings({"unchecked", "rawtypes"})
      ImmutableList<Class<? super T>> collectedTypes =
          (ImmutableList) TypeCollector.FOR_RAW_TYPE.classesOnly().collectTypes(getRawTypes());
      return ImmutableSet.copyOf(collectedTypes);
    }

    @Override
    public TypeSet interfaces() {
      throw new UnsupportedOperationException("classes().interfaces() not supported.");
    }

    private Object readResolve() {
      return getTypes().classes();
    }

    private static final long serialVersionUID = 0;
  }

  private enum TypeFilter implements Predicate<TypeToken<?>> {
    IGNORE_TYPE_VARIABLE_OR_WILDCARD {
      @Override
      public boolean apply(TypeToken<?> type) {
        return !(type.runtimeType instanceof TypeVariable
            || type.runtimeType instanceof WildcardType);
      }
    },
    INTERFACE_ONLY {
      @Override
      public boolean apply(TypeToken<?> type) {
        return type.getRawType().isInterface();
      }
    }
  }

  /**
   * Returns true if {@code o} is another {@code TypeToken} that represents the same {@link Type}.
   */
  @Override
  public boolean equals(@NullableDecl Object o) {
    if (o instanceof TypeToken) {
      TypeToken<?> that = (TypeToken<?>) o;
      return runtimeType.equals(that.runtimeType);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return runtimeType.hashCode();
  }

  @Override
  public String toString() {
    return Types.toString(runtimeType);
  }

  /** Implemented to support serialization of subclasses. */
  protected Object writeReplace() {
    // TypeResolver just transforms the type to our own impls that are Serializable
    // except TypeVariable.
    return of(new TypeResolver().resolveType(runtimeType));
  }

  /**
   * Ensures that this type token doesn't contain type variables, which can cause unchecked type
   * errors for callers like {@link TypeToInstanceMap}.
   */
  @CanIgnoreReturnValue
  final TypeToken<T> rejectTypeVariables() {
    new TypeVisitor() {
      @Override
      void visitTypeVariable(TypeVariable<?> type) {
        throw new IllegalArgumentException(
            runtimeType + "contains a type variable and is not safe for the operation");
      }

      @Override
      void visitWildcardType(WildcardType type) {
        visit(type.getLowerBounds());
        visit(type.getUpperBounds());
      }

      @Override
      void visitParameterizedType(ParameterizedType type) {
        visit(type.getActualTypeArguments());
        visit(type.getOwnerType());
      }

      @Override
      void visitGenericArrayType(GenericArrayType type) {
        visit(type.getGenericComponentType());
      }
    }.visit(runtimeType);
    return this;
  }

  private boolean someRawTypeIsSubclassOf(Class<?> superclass) {
    for (Class<?> rawType : getRawTypes()) {
      if (superclass.isAssignableFrom(rawType)) {
        return true;
      }
    }
    return false;
  }

  private boolean isSubtypeOfParameterizedType(ParameterizedType supertype) {
    Class<?> matchedClass = of(supertype).getRawType();
    if (!someRawTypeIsSubclassOf(matchedClass)) {
      return false;
    }
    TypeVariable<?>[] typeVars = matchedClass.getTypeParameters();
    Type[] supertypeArgs = supertype.getActualTypeArguments();
    for (int i = 0; i < typeVars.length; i++) {
      Type subtypeParam = getCovariantTypeResolver().resolveType(typeVars[i]);
      // If 'supertype' is "List<? extends CharSequence>"
      // and 'this' is StringArrayList,
      // First step is to figure out StringArrayList "is-a" List<E> where <E> = String.
      // String is then matched against <? extends CharSequence>, the supertypeArgs[0].
      if (!of(subtypeParam).is(supertypeArgs[i], typeVars[i])) {
        return false;
      }
    }
    // We only care about the case when the supertype is a non-static inner class
    // in which case we need to make sure the subclass's owner type is a subtype of the
    // supertype's owner.
    return Modifier.isStatic(((Class<?>) supertype.getRawType()).getModifiers())
        || supertype.getOwnerType() == null
        || isOwnedBySubtypeOf(supertype.getOwnerType());
  }

  private boolean isSubtypeOfArrayType(GenericArrayType supertype) {
    if (runtimeType instanceof Class) {
      Class<?> fromClass = (Class<?>) runtimeType;
      if (!fromClass.isArray()) {
        return false;
      }
      return of(fromClass.getComponentType()).isSubtypeOf(supertype.getGenericComponentType());
    } else if (runtimeType instanceof GenericArrayType) {
      GenericArrayType fromArrayType = (GenericArrayType) runtimeType;
      return of(fromArrayType.getGenericComponentType())
          .isSubtypeOf(supertype.getGenericComponentType());
    } else {
      return false;
    }
  }

  private boolean isSupertypeOfArray(GenericArrayType subtype) {
    if (runtimeType instanceof Class) {
      Class<?> thisClass = (Class<?>) runtimeType;
      if (!thisClass.isArray()) {
        return thisClass.isAssignableFrom(Object[].class);
      }
      return of(subtype.getGenericComponentType()).isSubtypeOf(thisClass.getComponentType());
    } else if (runtimeType instanceof GenericArrayType) {
      return of(subtype.getGenericComponentType())
          .isSubtypeOf(((GenericArrayType) runtimeType).getGenericComponentType());
    } else {
      return false;
    }
  }

  /**
   * {@code A.is(B)} is defined as {@code Foo<A>.isSubtypeOf(Foo<B>)}.
   *
   * <p>Specifically, returns true if any of the following conditions is met:
   *
   * <ol>
   *   <li>'this' and {@code formalType} are equal.
   *   <li>'this' and {@code formalType} have equal canonical form.
   *   <li>{@code formalType} is {@code <? extends Foo>} and 'this' is a subtype of {@code Foo}.
   *   <li>{@code formalType} is {@code <? super Foo>} and 'this' is a supertype of {@code Foo}.
   * </ol>
   *
   * Note that condition 2 isn't technically accurate under the context of a recursively bounded
   * type variables. For example, {@code Enum<? extends Enum<E>>} canonicalizes to {@code Enum<?>}
   * where {@code E} is the type variable declared on the {@code Enum} class declaration. It's
   * technically <em>not</em> true that {@code Foo<Enum<? extends Enum<E>>>} is a subtype of {@code
   * Foo<Enum<?>>} according to JLS. See testRecursiveWildcardSubtypeBug() for a real example.
   *
   * <p>It appears that properly handling recursive type bounds in the presence of implicit type
   * bounds is not easy. For now we punt, hoping that this defect should rarely cause issues in real
   * code.
   *
   * @param formalType is {@code Foo<formalType>} a supertype of {@code Foo<T>}?
   * @param declaration The type variable in the context of a parameterized type. Used to infer type
   *     bound when {@code formalType} is a wildcard with implicit upper bound.
   */
  private boolean is(Type formalType, TypeVariable<?> declaration) {
    if (runtimeType.equals(formalType)) {
      return true;
    }
    if (formalType instanceof WildcardType) {
      WildcardType your = canonicalizeWildcardType(declaration, (WildcardType) formalType);
      // if "formalType" is <? extends Foo>, "this" can be:
      // Foo, SubFoo, <? extends Foo>, <? extends SubFoo>, <T extends Foo> or
      // <T extends SubFoo>.
      // if "formalType" is <? super Foo>, "this" can be:
      // Foo, SuperFoo, <? super Foo> or <? super SuperFoo>.
      return every(your.getUpperBounds()).isSupertypeOf(runtimeType)
          && every(your.getLowerBounds()).isSubtypeOf(runtimeType);
    }
    return canonicalizeWildcardsInType(runtimeType).equals(canonicalizeWildcardsInType(formalType));
  }

  /**
   * In reflection, {@code Foo<?>.getUpperBounds()[0]} is always {@code Object.class}, even when Foo
   * is defined as {@code Foo<T extends String>}. Thus directly calling {@code <?>.is(String.class)}
   * will return false. To mitigate, we canonicalize wildcards by enforcing the following
   * invariants:
   *
   * <ol>
   *   <li>{@code canonicalize(t)} always produces the equal result for equivalent types. For
   *       example both {@code Enum<?>} and {@code Enum<? extends Enum<?>>} canonicalize to {@code
   *       Enum<? extends Enum<E>}.
   *   <li>{@code canonicalize(t)} produces a "literal" supertype of t. For example: {@code Enum<?
   *       extends Enum<?>>} canonicalizes to {@code Enum<?>}, which is a supertype (if we disregard
   *       the upper bound is implicitly an Enum too).
   *   <li>If {@code canonicalize(A) == canonicalize(B)}, then {@code Foo<A>.isSubtypeOf(Foo<B>)}
   *       and vice versa. i.e. {@code A.is(B)} and {@code B.is(A)}.
   *   <li>{@code canonicalize(canonicalize(A)) == canonicalize(A)}.
   * </ol>
   */
  private static Type canonicalizeTypeArg(TypeVariable<?> declaration, Type typeArg) {
    return typeArg instanceof WildcardType
        ? canonicalizeWildcardType(declaration, ((WildcardType) typeArg))
        : canonicalizeWildcardsInType(typeArg);
  }

  private static Type canonicalizeWildcardsInType(Type type) {
    if (type instanceof ParameterizedType) {
      return canonicalizeWildcardsInParameterizedType((ParameterizedType) type);
    }
    if (type instanceof GenericArrayType) {
      return Types.newArrayType(
          canonicalizeWildcardsInType(((GenericArrayType) type).getGenericComponentType()));
    }
    return type;
  }

  // WARNING: the returned type may have empty upper bounds, which may violate common expectations
  // by user code or even some of our own code. It's fine for the purpose of checking subtypes.
  // Just don't ever let the user access it.
  private static WildcardType canonicalizeWildcardType(
      TypeVariable<?> declaration, WildcardType type) {
    Type[] declared = declaration.getBounds();
    List<Type> upperBounds = new ArrayList<>();
    for (Type bound : type.getUpperBounds()) {
      if (!any(declared).isSubtypeOf(bound)) {
        upperBounds.add(canonicalizeWildcardsInType(bound));
      }
    }
    return new Types.WildcardTypeImpl(type.getLowerBounds(), upperBounds.toArray(new Type[0]));
  }

  private static ParameterizedType canonicalizeWildcardsInParameterizedType(
      ParameterizedType type) {
    Class<?> rawType = (Class<?>) type.getRawType();
    TypeVariable<?>[] typeVars = rawType.getTypeParameters();
    Type[] typeArgs = type.getActualTypeArguments();
    for (int i = 0; i < typeArgs.length; i++) {
      typeArgs[i] = canonicalizeTypeArg(typeVars[i], typeArgs[i]);
    }
    return Types.newParameterizedTypeWithOwner(type.getOwnerType(), rawType, typeArgs);
  }

  private static Bounds every(Type[] bounds) {
    // Every bound must match. On any false, result is false.
    return new Bounds(bounds, false);
  }

  private static Bounds any(Type[] bounds) {
    // Any bound matches. On any true, result is true.
    return new Bounds(bounds, true);
  }

  private static class Bounds {
    private final Type[] bounds;
    private final boolean target;

    Bounds(Type[] bounds, boolean target) {
      this.bounds = bounds;
      this.target = target;
    }

    boolean isSubtypeOf(Type supertype) {
      for (Type bound : bounds) {
        if (of(bound).isSubtypeOf(supertype) == target) {
          return target;
        }
      }
      return !target;
    }

    boolean isSupertypeOf(Type subtype) {
      TypeToken<?> type = of(subtype);
      for (Type bound : bounds) {
        if (type.isSubtypeOf(bound) == target) {
          return target;
        }
      }
      return !target;
    }
  }

  private ImmutableSet<Class<? super T>> getRawTypes() {
    final ImmutableSet.Builder<Class<?>> builder = ImmutableSet.builder();
    new TypeVisitor() {
      @Override
      void visitTypeVariable(TypeVariable<?> t) {
        visit(t.getBounds());
      }

      @Override
      void visitWildcardType(WildcardType t) {
        visit(t.getUpperBounds());
      }

      @Override
      void visitParameterizedType(ParameterizedType t) {
        builder.add((Class<?>) t.getRawType());
      }

      @Override
      void visitClass(Class<?> t) {
        builder.add(t);
      }

      @Override
      void visitGenericArrayType(GenericArrayType t) {
        builder.add(Types.getArrayClass(of(t.getGenericComponentType()).getRawType()));
      }
    }.visit(runtimeType);
    // Cast from ImmutableSet<Class<?>> to ImmutableSet<Class<? super T>>
    @SuppressWarnings({"unchecked", "rawtypes"})
    ImmutableSet<Class<? super T>> result = (ImmutableSet) builder.build();
    return result;
  }

  private boolean isOwnedBySubtypeOf(Type supertype) {
    for (TypeToken<?> type : getTypes()) {
      Type ownerType = type.getOwnerTypeIfPresent();
      if (ownerType != null && of(ownerType).isSubtypeOf(supertype)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Returns the owner type of a {@link ParameterizedType} or enclosing class of a {@link Class}, or
   * null otherwise.
   */
  @NullableDecl
  private Type getOwnerTypeIfPresent() {
    if (runtimeType instanceof ParameterizedType) {
      return ((ParameterizedType) runtimeType).getOwnerType();
    } else if (runtimeType instanceof Class<?>) {
      return ((Class<?>) runtimeType).getEnclosingClass();
    } else {
      return null;
    }
  }

  /**
   * Returns the type token representing the generic type declaration of {@code cls}. For example:
   * {@code TypeToken.getGenericType(Iterable.class)} returns {@code Iterable<T>}.
   *
   * <p>If {@code cls} isn't parameterized and isn't a generic array, the type token of the class is
   * returned.
   */
  @VisibleForTesting
  static <T> TypeToken<? extends T> toGenericType(Class<T> cls) {
    if (cls.isArray()) {
      Type arrayOfGenericType =
          Types.newArrayType(
              // If we are passed with int[].class, don't turn it to GenericArrayType
              toGenericType(cls.getComponentType()).runtimeType);
      @SuppressWarnings("unchecked") // array is covariant
      TypeToken<? extends T> result = (TypeToken<? extends T>) of(arrayOfGenericType);
      return result;
    }
    TypeVariable<Class<T>>[] typeParams = cls.getTypeParameters();
    Type ownerType =
        cls.isMemberClass() && !Modifier.isStatic(cls.getModifiers())
            ? toGenericType(cls.getEnclosingClass()).runtimeType
            : null;

    if ((typeParams.length > 0) || ((ownerType != null) && ownerType != cls.getEnclosingClass())) {
      @SuppressWarnings("unchecked") // Like, it's Iterable<T> for Iterable.class
      TypeToken<? extends T> type =
          (TypeToken<? extends T>)
              of(Types.newParameterizedTypeWithOwner(ownerType, cls, typeParams));
      return type;
    } else {
      return of(cls);
    }
  }

  private TypeResolver getCovariantTypeResolver() {
    TypeResolver resolver = covariantTypeResolver;
    if (resolver == null) {
      resolver = (covariantTypeResolver = TypeResolver.covariantly(runtimeType));
    }
    return resolver;
  }

  private TypeResolver getInvariantTypeResolver() {
    TypeResolver resolver = invariantTypeResolver;
    if (resolver == null) {
      resolver = (invariantTypeResolver = TypeResolver.invariantly(runtimeType));
    }
    return resolver;
  }

  private TypeToken<? super T> getSupertypeFromUpperBounds(
      Class<? super T> supertype, Type[] upperBounds) {
    for (Type upperBound : upperBounds) {
      @SuppressWarnings("unchecked") // T's upperbound is <? super T>.
      TypeToken<? super T> bound = (TypeToken<? super T>) of(upperBound);
      if (bound.isSubtypeOf(supertype)) {
        @SuppressWarnings({"rawtypes", "unchecked"}) // guarded by the isSubtypeOf check.
        TypeToken<? super T> result = bound.getSupertype((Class) supertype);
        return result;
      }
    }
    throw new IllegalArgumentException(supertype + " isn't a super type of " + this);
  }

  private TypeToken<? extends T> getSubtypeFromLowerBounds(Class<?> subclass, Type[] lowerBounds) {
    if (lowerBounds.length > 0) {
      @SuppressWarnings("unchecked") // T's lower bound is <? extends T>
      TypeToken<? extends T> bound = (TypeToken<? extends T>) of(lowerBounds[0]);
      // Java supports only one lowerbound anyway.
      return bound.getSubtype(subclass);
    }
    throw new IllegalArgumentException(subclass + " isn't a subclass of " + this);
  }

  private TypeToken<? super T> getArraySupertype(Class<? super T> supertype) {
    // with component type, we have lost generic type information
    // Use raw type so that compiler allows us to call getSupertype()
    @SuppressWarnings("rawtypes")
    TypeToken componentType =
        checkNotNull(getComponentType(), "%s isn't a super type of %s", supertype, this);
    // array is covariant. component type is super type, so is the array type.
    @SuppressWarnings("unchecked") // going from raw type back to generics
    TypeToken<?> componentSupertype = componentType.getSupertype(supertype.getComponentType());
    @SuppressWarnings("unchecked") // component type is super type, so is array type.
    TypeToken<? super T> result =
        (TypeToken<? super T>)
            // If we are passed with int[].class, don't turn it to GenericArrayType
            of(newArrayClassOrGenericArrayType(componentSupertype.runtimeType));
    return result;
  }

  private TypeToken<? extends T> getArraySubtype(Class<?> subclass) {
    // array is covariant. component type is subtype, so is the array type.
    TypeToken<?> componentSubtype = getComponentType().getSubtype(subclass.getComponentType());
    @SuppressWarnings("unchecked") // component type is subtype, so is array type.
    TypeToken<? extends T> result =
        (TypeToken<? extends T>)
            // If we are passed with int[].class, don't turn it to GenericArrayType
            of(newArrayClassOrGenericArrayType(componentSubtype.runtimeType));
    return result;
  }

  private Type resolveTypeArgsForSubclass(Class<?> subclass) {
    // If both runtimeType and subclass are not parameterized, return subclass
    // If runtimeType is not parameterized but subclass is, process subclass as a parameterized type
    // If runtimeType is a raw type (i.e. is a parameterized type specified as a Class<?>), we
    // return subclass as a raw type
    if (runtimeType instanceof Class
        && ((subclass.getTypeParameters().length == 0)
            || (getRawType().getTypeParameters().length != 0))) {
      // no resolution needed
      return subclass;
    }
    // class Base<A, B> {}
    // class Sub<X, Y> extends Base<X, Y> {}
    // Base<String, Integer>.subtype(Sub.class):

    // Sub<X, Y>.getSupertype(Base.class) => Base<X, Y>
    // => X=String, Y=Integer
    // => Sub<X, Y>=Sub<String, Integer>
    TypeToken<?> genericSubtype = toGenericType(subclass);
    @SuppressWarnings({"rawtypes", "unchecked"}) // subclass isn't <? extends T>
    Type supertypeWithArgsFromSubtype =
        genericSubtype.getSupertype((Class) getRawType()).runtimeType;
    return new TypeResolver()
        .where(supertypeWithArgsFromSubtype, runtimeType)
        .resolveType(genericSubtype.runtimeType);
  }

  /**
   * Creates an array class if {@code componentType} is a class, or else, a {@link
   * GenericArrayType}. This is what Java7 does for generic array type parameters.
   */
  private static Type newArrayClassOrGenericArrayType(Type componentType) {
    return Types.JavaVersion.JAVA7.newArrayType(componentType);
  }

  private static final class SimpleTypeToken<T> extends TypeToken<T> {

    SimpleTypeToken(Type type) {
      super(type);
    }

    private static final long serialVersionUID = 0;
  }

  /**
   * Collects parent types from a sub type.
   *
   * @param <K> The type "kind". Either a TypeToken, or Class.
   */
  private abstract static class TypeCollector<K> {

    static final TypeCollector<TypeToken<?>> FOR_GENERIC_TYPE =
        new TypeCollector<TypeToken<?>>() {
          @Override
          Class<?> getRawType(TypeToken<?> type) {
            return type.getRawType();
          }

          @Override
          Iterable<? extends TypeToken<?>> getInterfaces(TypeToken<?> type) {
            return type.getGenericInterfaces();
          }

          @Override
          @NullableDecl
          TypeToken<?> getSuperclass(TypeToken<?> type) {
            return type.getGenericSuperclass();
          }
        };

    static final TypeCollector<Class<?>> FOR_RAW_TYPE =
        new TypeCollector<Class<?>>() {
          @Override
          Class<?> getRawType(Class<?> type) {
            return type;
          }

          @Override
          Iterable<? extends Class<?>> getInterfaces(Class<?> type) {
            return Arrays.asList(type.getInterfaces());
          }

          @Override
          @NullableDecl
          Class<?> getSuperclass(Class<?> type) {
            return type.getSuperclass();
          }
        };

    /** For just classes, we don't have to traverse interfaces. */
    final TypeCollector<K> classesOnly() {
      return new ForwardingTypeCollector<K>(this) {
        @Override
        Iterable<? extends K> getInterfaces(K type) {
          return ImmutableSet.of();
        }

        @Override
        ImmutableList<K> collectTypes(Iterable<? extends K> types) {
          ImmutableList.Builder<K> builder = ImmutableList.builder();
          for (K type : types) {
            if (!getRawType(type).isInterface()) {
              builder.add(type);
            }
          }
          return super.collectTypes(builder.build());
        }
      };
    }

    final ImmutableList<K> collectTypes(K type) {
      return collectTypes(ImmutableList.of(type));
    }

    ImmutableList<K> collectTypes(Iterable<? extends K> types) {
      // type -> order number. 1 for Object, 2 for anything directly below, so on so forth.
      Map<K, Integer> map = Maps.newHashMap();
      for (K type : types) {
        collectTypes(type, map);
      }
      return sortKeysByValue(map, Ordering.natural().reverse());
    }

    /** Collects all types to map, and returns the total depth from T up to Object. */
    @CanIgnoreReturnValue
    private int collectTypes(K type, Map<? super K, Integer> map) {
      Integer existing = map.get(type);
      if (existing != null) {
        // short circuit: if set contains type it already contains its supertypes
        return existing;
      }
      // Interfaces should be listed before Object.
      int aboveMe = getRawType(type).isInterface() ? 1 : 0;
      for (K interfaceType : getInterfaces(type)) {
        aboveMe = Math.max(aboveMe, collectTypes(interfaceType, map));
      }
      K superclass = getSuperclass(type);
      if (superclass != null) {
        aboveMe = Math.max(aboveMe, collectTypes(superclass, map));
      }
      /*
       * TODO(benyu): should we include Object for interface? Also, CharSequence[] and Object[] for
       * String[]?
       *
       */
      map.put(type, aboveMe + 1);
      return aboveMe + 1;
    }

    private static <K, V> ImmutableList<K> sortKeysByValue(
        final Map<K, V> map, final Comparator<? super V> valueComparator) {
      Ordering<K> keyOrdering =
          new Ordering<K>() {
            @Override
            public int compare(K left, K right) {
              return valueComparator.compare(map.get(left), map.get(right));
            }
          };
      return keyOrdering.immutableSortedCopy(map.keySet());
    }

    abstract Class<?> getRawType(K type);

    abstract Iterable<? extends K> getInterfaces(K type);

    @NullableDecl
    abstract K getSuperclass(K type);

    private static class ForwardingTypeCollector<K> extends TypeCollector<K> {

      private final TypeCollector<K> delegate;

      ForwardingTypeCollector(TypeCollector<K> delegate) {
        this.delegate = delegate;
      }

      @Override
      Class<?> getRawType(K type) {
        return delegate.getRawType(type);
      }

      @Override
      Iterable<? extends K> getInterfaces(K type) {
        return delegate.getInterfaces(type);
      }

      @Override
      K getSuperclass(K type) {
        return delegate.getSuperclass(type);
      }
    }
  }

  // This happens to be the hash of the class as of now. So setting it makes a backward compatible
  // change. Going forward, if any incompatible change is added, we can change the UID back to 1.
  private static final long serialVersionUID = 3637540370352322684L;
}
