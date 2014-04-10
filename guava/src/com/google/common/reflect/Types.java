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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Iterables.transform;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Objects;
import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;

import java.io.Serializable;
import java.lang.reflect.Array;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.GenericDeclaration;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.Nullable;

/**
 * Utilities for working with {@link Type}.
 *
 * @author Ben Yu
 */
final class Types {

  /** Class#toString without the "class " and "interface " prefixes */
  private static final Function<Type, String> TYPE_TO_STRING =
      new Function<Type, String>() {
        @Override public String apply(Type from) {
          return Types.toString(from);
        }
      };

  private static final Joiner COMMA_JOINER = Joiner.on(", ").useForNull("null");

  /** Returns the array type of {@code componentType}. */
  static Type newArrayType(Type componentType) {
    if (componentType instanceof WildcardType) {
      WildcardType wildcard = (WildcardType) componentType;
      Type[] lowerBounds = wildcard.getLowerBounds();
      checkArgument(lowerBounds.length <= 1, "Wildcard cannot have more than one lower bounds.");
      if (lowerBounds.length == 1) {
        return supertypeOf(newArrayType(lowerBounds[0]));
      } else {
        Type[] upperBounds = wildcard.getUpperBounds();
        checkArgument(upperBounds.length == 1, "Wildcard should have only one upper bound.");
        return subtypeOf(newArrayType(upperBounds[0]));
      }
    }
    return JavaVersion.CURRENT.newArrayType(componentType);
  }

  /**
   * Returns a type where {@code rawType} is parameterized by
   * {@code arguments} and is owned by {@code ownerType}.
   */
  static ParameterizedType newParameterizedTypeWithOwner(
      @Nullable Type ownerType, Class<?> rawType, Type... arguments) {
    if (ownerType == null) {
      return newParameterizedType(rawType, arguments);
    }
    // ParameterizedTypeImpl constructor already checks, but we want to throw NPE before IAE
    checkNotNull(arguments);
    checkArgument(rawType.getEnclosingClass() != null, "Owner type for unenclosed %s", rawType);
    return new ParameterizedTypeImpl(ownerType, rawType, arguments);
  }

  /**
   * Returns a type where {@code rawType} is parameterized by
   * {@code arguments}.
   */
  static ParameterizedType newParameterizedType(Class<?> rawType, Type... arguments) {
      return new ParameterizedTypeImpl(
          ClassOwnership.JVM_BEHAVIOR.getOwnerType(rawType), rawType, arguments);
  }

  /** Decides what owner type to use for constructing {@link ParameterizedType} from a raw class. */
  private enum ClassOwnership {

    OWNED_BY_ENCLOSING_CLASS {
      @Nullable
      @Override
      Class<?> getOwnerType(Class<?> rawType) {
        return rawType.getEnclosingClass();
      }
    },
    LOCAL_CLASS_HAS_NO_OWNER {
      @Nullable
      @Override
      Class<?> getOwnerType(Class<?> rawType) {
        if (rawType.isLocalClass()) {
          return null;
        } else {
          return rawType.getEnclosingClass();
        }
      }
    };

    @Nullable abstract Class<?> getOwnerType(Class<?> rawType);

    static final ClassOwnership JVM_BEHAVIOR = detectJvmBehavior();

    private static ClassOwnership detectJvmBehavior() {
      class LocalClass<T> {}
      Class<?> subclass = new LocalClass<String>() {}.getClass();
      ParameterizedType parameterizedType = (ParameterizedType)
          subclass.getGenericSuperclass();
      for (ClassOwnership behavior : ClassOwnership.values()) {
        if (behavior.getOwnerType(LocalClass.class) == parameterizedType.getOwnerType()) {
          return behavior;
        }
      }
      throw new AssertionError();
    }
  }

  /**
   * Returns a new {@link TypeVariable} that belongs to {@code declaration} with
   * {@code name} and {@code bounds}.
   */
  static <D extends GenericDeclaration> TypeVariable<D> newArtificialTypeVariable(
      D declaration, String name, Type... bounds) {
    return new TypeVariableImpl<D>(
        declaration,
        name,
        (bounds.length == 0)
            ? new Type[] { Object.class }
            : bounds);
  }

  /** Returns a new {@link WildcardType} with {@code upperBound}. */
  @VisibleForTesting static WildcardType subtypeOf(Type upperBound) {
    return new WildcardTypeImpl(new Type[0], new Type[] { upperBound });
  }

  /** Returns a new {@link WildcardType} with {@code lowerBound}. */
  @VisibleForTesting static WildcardType supertypeOf(Type lowerBound) {
    return new WildcardTypeImpl(new Type[] { lowerBound }, new Type[] { Object.class });
  }

  /**
   * Returns human readable string representation of {@code type}.
   * <ul>
   * <li> For array type {@code Foo[]}, {@code "com.mypackage.Foo[]"} are
   * returned.
   * <li> For any class, {@code theClass.getName()} are returned.
   * <li> For all other types, {@code type.toString()} are returned.
   * </ul>
   */
  static String toString(Type type) {
    return (type instanceof Class)
        ? ((Class<?>) type).getName()
        : type.toString();
  }

  @Nullable static Type getComponentType(Type type) {
    checkNotNull(type);
    final AtomicReference<Type> result = new AtomicReference<Type>();
    new TypeVisitor() {
      @Override void visitTypeVariable(TypeVariable<?> t) {
        result.set(subtypeOfComponentType(t.getBounds()));
      }
      @Override void visitWildcardType(WildcardType t) {
        result.set(subtypeOfComponentType(t.getUpperBounds()));
      }
      @Override void visitGenericArrayType(GenericArrayType t) {
        result.set(t.getGenericComponentType());
      }
      @Override void visitClass(Class<?> t) {
        result.set(t.getComponentType());
      }
    }.visit(type);
    return result.get();
  }

  /**
   * Returns {@code ? extends X} if any of {@code bounds} is a subtype of {@code X[]}; or null
   * otherwise.
   */
  @Nullable private static Type subtypeOfComponentType(Type[] bounds) {
    for (Type bound : bounds) {
      Type componentType = getComponentType(bound);
      if (componentType != null) {
        // Only the first bound can be a class or array.
        // Bounds after the first can only be interfaces.
        if (componentType instanceof Class) {
          Class<?> componentClass = (Class<?>) componentType;
          if (componentClass.isPrimitive()) {
            return componentClass;
          }
        }
        return subtypeOf(componentType);
      }
    }
    return null;
  }

  private static final class GenericArrayTypeImpl
      implements GenericArrayType, Serializable {

    private final Type componentType;

    GenericArrayTypeImpl(Type componentType) {
      this.componentType = JavaVersion.CURRENT.usedInGenericType(componentType);
    }

    @Override public Type getGenericComponentType() {
      return componentType;
    }

    @Override public String toString() {
      return Types.toString(componentType) + "[]";
    }

    @Override public int hashCode() {
      return componentType.hashCode();
    }

    @Override public boolean equals(Object obj) {
      if (obj instanceof GenericArrayType) {
        GenericArrayType that = (GenericArrayType) obj;
        return Objects.equal(
            getGenericComponentType(), that.getGenericComponentType());
      }
      return false;
    }

    private static final long serialVersionUID = 0;
  }

  private static final class ParameterizedTypeImpl
      implements ParameterizedType, Serializable {

    private final Type ownerType;
    private final ImmutableList<Type> argumentsList;
    private final Class<?> rawType;

    ParameterizedTypeImpl(
        @Nullable Type ownerType, Class<?> rawType, Type[] typeArguments) {
      checkNotNull(rawType);
      checkArgument(typeArguments.length == rawType.getTypeParameters().length);
      disallowPrimitiveType(typeArguments, "type parameter");
      this.ownerType = ownerType;
      this.rawType = rawType;
      this.argumentsList = JavaVersion.CURRENT.usedInGenericType(typeArguments);
    }

    @Override public Type[] getActualTypeArguments() {
      return toArray(argumentsList);
    }

    @Override public Type getRawType() {
      return rawType;
    }

    @Override public Type getOwnerType() {
      return ownerType;
    }

    @Override public String toString() {
      StringBuilder builder = new StringBuilder();
      if (ownerType != null) {
        builder.append(Types.toString(ownerType)).append('.');
      }
      builder.append(rawType.getName())
          .append('<')
          .append(COMMA_JOINER.join(transform(argumentsList, TYPE_TO_STRING)))
          .append('>');
      return builder.toString();
    }

    @Override public int hashCode() {
      return (ownerType == null ? 0 : ownerType.hashCode())
          ^ argumentsList.hashCode() ^ rawType.hashCode();
    }

    @Override public boolean equals(Object other) {
      if (!(other instanceof ParameterizedType)) {
        return false;
      }
      ParameterizedType that = (ParameterizedType) other;
      return getRawType().equals(that.getRawType())
          && Objects.equal(getOwnerType(), that.getOwnerType())
          && Arrays.equals(
              getActualTypeArguments(), that.getActualTypeArguments());
    }

    private static final long serialVersionUID = 0;
  }

  private static final class TypeVariableImpl<D extends GenericDeclaration>
      implements TypeVariable<D> {

    private final D genericDeclaration;
    private final String name;
    private final ImmutableList<Type> bounds;

    TypeVariableImpl(D genericDeclaration, String name, Type[] bounds) {
      disallowPrimitiveType(bounds, "bound for type variable");
      this.genericDeclaration = checkNotNull(genericDeclaration);
      this.name = checkNotNull(name);
      this.bounds = ImmutableList.copyOf(bounds);
    }

    @Override public Type[] getBounds() {
      return toArray(bounds);
    }

    @Override public D getGenericDeclaration() {
      return genericDeclaration;
    }

    @Override public String getName() {
      return name;
    }

    @Override public String toString() {
      return name;
    }

    @Override public int hashCode() {
      return genericDeclaration.hashCode() ^ name.hashCode();
    }

    @Override public boolean equals(Object obj) {
      if (NativeTypeVariableEquals.NATIVE_TYPE_VARIABLE_ONLY) {
        // equal only to our TypeVariable implementation with identical bounds
        if (obj instanceof TypeVariableImpl) {
          TypeVariableImpl<?> that = (TypeVariableImpl<?>) obj;
          return name.equals(that.getName())
              && genericDeclaration.equals(that.getGenericDeclaration())
              && bounds.equals(that.bounds);
        }
        return false;
      } else {
        // equal to any TypeVariable implementation regardless of bounds
        if (obj instanceof TypeVariable) {
          TypeVariable<?> that = (TypeVariable<?>) obj;
          return name.equals(that.getName())
              && genericDeclaration.equals(that.getGenericDeclaration());
        }
        return false;
      }
    }
  }

  static final class WildcardTypeImpl implements WildcardType, Serializable {

    private final ImmutableList<Type> lowerBounds;
    private final ImmutableList<Type> upperBounds;

    WildcardTypeImpl(Type[] lowerBounds, Type[] upperBounds) {
      disallowPrimitiveType(lowerBounds, "lower bound for wildcard");
      disallowPrimitiveType(upperBounds, "upper bound for wildcard");
      this.lowerBounds = JavaVersion.CURRENT.usedInGenericType(lowerBounds);
      this.upperBounds = JavaVersion.CURRENT.usedInGenericType(upperBounds);
    }

    @Override public Type[] getLowerBounds() {
      return toArray(lowerBounds);
    }

    @Override public Type[] getUpperBounds() {
      return toArray(upperBounds);
    }

    @Override public boolean equals(Object obj) {
      if (obj instanceof WildcardType) {
        WildcardType that = (WildcardType) obj;
        return lowerBounds.equals(Arrays.asList(that.getLowerBounds()))
            && upperBounds.equals(Arrays.asList(that.getUpperBounds()));
      }
      return false;
    }

    @Override public int hashCode() {
      return lowerBounds.hashCode() ^ upperBounds.hashCode();
    }

    @Override public String toString() {
      StringBuilder builder = new StringBuilder("?");
      for (Type lowerBound : lowerBounds) {
        builder.append(" super ").append(Types.toString(lowerBound));
      }
      for (Type upperBound : filterUpperBounds(upperBounds)) {
        builder.append(" extends ").append(Types.toString(upperBound));
      }
      return builder.toString();
    }

    private static final long serialVersionUID = 0;
  }

  private static Type[] toArray(Collection<Type> types) {
    return types.toArray(new Type[types.size()]);
  }

  private static Iterable<Type> filterUpperBounds(Iterable<Type> bounds) {
    return Iterables.filter(
        bounds, Predicates.not(Predicates.<Type>equalTo(Object.class)));
  }

  private static void disallowPrimitiveType(Type[] types, String usedAs) {
    for (Type type : types) {
      if (type instanceof Class) {
        Class<?> cls = (Class<?>) type;
        checkArgument(!cls.isPrimitive(),
            "Primitive type '%s' used as %s", cls, usedAs);
      }
    }
  }

  /** Returns the {@code Class} object of arrays with {@code componentType}. */
  static Class<?> getArrayClass(Class<?> componentType) {
    // TODO(user): This is not the most efficient way to handle generic
    // arrays, but is there another way to extract the array class in a
    // non-hacky way (i.e. using String value class names- "[L...")?
    return Array.newInstance(componentType, 0).getClass();
  }

  // TODO(benyu): Once we are on Java 7, delete this abstraction
  enum JavaVersion {

    JAVA6 {
      @Override GenericArrayType newArrayType(Type componentType) {
        return new GenericArrayTypeImpl(componentType);
      }
      @Override Type usedInGenericType(Type type) {
        checkNotNull(type);
        if (type instanceof Class) {
          Class<?> cls = (Class<?>) type;
          if (cls.isArray()) {
            return new GenericArrayTypeImpl(cls.getComponentType());
          }
        }
        return type;
      }
    },
    JAVA7 {
      @Override Type newArrayType(Type componentType) {
        if (componentType instanceof Class) {
          return getArrayClass((Class<?>) componentType);
        } else {
          return new GenericArrayTypeImpl(componentType);
        }
      }
      @Override Type usedInGenericType(Type type) {
        return checkNotNull(type);
      }
    }
    ;

    static final JavaVersion CURRENT =
        (new TypeCapture<int[]>() {}.capture() instanceof Class)
        ? JAVA7 : JAVA6;
    abstract Type newArrayType(Type componentType);
    abstract Type usedInGenericType(Type type);

    final ImmutableList<Type> usedInGenericType(Type[] types) {
      ImmutableList.Builder<Type> builder = ImmutableList.builder();
      for (Type type : types) {
        builder.add(usedInGenericType(type));
      }
      return builder.build();
    }
  }

  /**
   * Per https://code.google.com/p/guava-libraries/issues/detail?id=1635,
   * In JDK 1.7.0_51-b13, TypeVariableImpl.equals() is changed to no longer be equal to custom
   * TypeVariable implementations. As a result, we need to make sure our TypeVariable implementation
   * respects symmetry.
   * Moreover, we don't want to reconstruct a native type variable <A> using our implementation
   * unless some of its bounds have changed in resolution. This avoids creating unequal TypeVariable
   * implementation unnecessarily. When the bounds do change, however, it's fine for the synthetic
   * TypeVariable to be unequal to any native TypeVariable anyway.
   */
  static final class NativeTypeVariableEquals<X> {
    static final boolean NATIVE_TYPE_VARIABLE_ONLY =
        !NativeTypeVariableEquals.class.getTypeParameters()[0].equals(
            newArtificialTypeVariable(NativeTypeVariableEquals.class, "X"));
  }

  private Types() {}
}
