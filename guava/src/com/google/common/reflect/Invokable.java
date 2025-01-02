/*
 * Copyright (C) 2012 The Guava Authors
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

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableList;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.lang.annotation.Annotation;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.Arrays;
import org.jspecify.annotations.Nullable;

/**
 * Wrapper around either a {@link Method} or a {@link Constructor}. Convenience API is provided to
 * make common reflective operation easier to deal with, such as {@link #isPublic}, {@link
 * #getParameters} etc.
 *
 * <p>In addition to convenience methods, {@link TypeToken#method} and {@link TypeToken#constructor}
 * will resolve the type parameters of the method or constructor in the context of the owner type,
 * which may be a subtype of the declaring class. For example:
 *
 * <pre>{@code
 * Method getMethod = List.class.getMethod("get", int.class);
 * Invokable<List<String>, ?> invokable = new TypeToken<List<String>>() {}.method(getMethod);
 * assertEquals(TypeToken.of(String.class), invokable.getReturnType()); // Not Object.class!
 * assertEquals(new TypeToken<List<String>>() {}, invokable.getOwnerType());
 * }</pre>
 *
 * <p><b>Note:</b> earlier versions of this class inherited from {@link
 * java.lang.reflect.AccessibleObject AccessibleObject} and {@link
 * java.lang.reflect.GenericDeclaration GenericDeclaration}. Since version 31.0 that is no longer
 * the case. However, most methods from those types are present with the same signature in this
 * class.
 *
 * @param <T> the type that owns this method or constructor.
 * @param <R> the return type of (or supertype thereof) the method or the declaring type of the
 *     constructor.
 * @author Ben Yu
 * @since 14.0 (no longer implements {@link AccessibleObject} or {@code GenericDeclaration} since
 *     31.0)
 */
public abstract class Invokable<T, R> implements AnnotatedElement, Member {
  private final AccessibleObject accessibleObject;
  private final Member member;

  <M extends AccessibleObject & Member> Invokable(M member) {
    checkNotNull(member);
    this.accessibleObject = member;
    this.member = member;
  }

  /** Returns {@link Invokable} of {@code method}. */
  public static Invokable<?, Object> from(Method method) {
    return new MethodInvokable<>(method);
  }

  /** Returns {@link Invokable} of {@code constructor}. */
  public static <T> Invokable<T, T> from(Constructor<T> constructor) {
    return new ConstructorInvokable<T>(constructor);
  }

  @Override
  public final boolean isAnnotationPresent(Class<? extends Annotation> annotationClass) {
    return accessibleObject.isAnnotationPresent(annotationClass);
  }

  @Override
  public final <A extends Annotation> @Nullable A getAnnotation(Class<A> annotationClass) {
    return accessibleObject.getAnnotation(annotationClass);
  }

  @Override
  public final Annotation[] getAnnotations() {
    return accessibleObject.getAnnotations();
  }

  @Override
  public final Annotation[] getDeclaredAnnotations() {
    return accessibleObject.getDeclaredAnnotations();
  }

  // We ought to be able to implement GenericDeclaration instead its parent AnnotatedElement.
  // That would give us this method declaration. But for some reason, implementing
  // GenericDeclaration leads to weird errors in Android tests:
  // IncompatibleClassChangeError: interface not implemented
  /** See {@link java.lang.reflect.GenericDeclaration#getTypeParameters()}. */
  public abstract TypeVariable<?>[] getTypeParameters();

  /** See {@link java.lang.reflect.AccessibleObject#setAccessible(boolean)}. */
  public final void setAccessible(boolean flag) {
    accessibleObject.setAccessible(flag);
  }

  /** See {@link java.lang.reflect.AccessibleObject#trySetAccessible()}. */
  @SuppressWarnings("CatchingUnchecked") // sneaky checked exception
  public final boolean trySetAccessible() {
    // We can't call accessibleObject.trySetAccessible since that was added in Java 9 and this code
    // should work on Java 8. So we emulate it this way.
    try {
      accessibleObject.setAccessible(true);
      return true;
    } catch (Exception e) { // sneaky checked exception
      return false;
    }
  }

  /** See {@link java.lang.reflect.AccessibleObject#isAccessible()}. */
  public final boolean isAccessible() {
    return accessibleObject.isAccessible();
  }

  @Override
  public final String getName() {
    return member.getName();
  }

  @Override
  public final int getModifiers() {
    return member.getModifiers();
  }

  @Override
  public final boolean isSynthetic() {
    return member.isSynthetic();
  }

  /** Returns true if the element is public. */
  public final boolean isPublic() {
    return Modifier.isPublic(getModifiers());
  }

  /** Returns true if the element is protected. */
  public final boolean isProtected() {
    return Modifier.isProtected(getModifiers());
  }

  /** Returns true if the element is package-private. */
  public final boolean isPackagePrivate() {
    return !isPrivate() && !isPublic() && !isProtected();
  }

  /** Returns true if the element is private. */
  public final boolean isPrivate() {
    return Modifier.isPrivate(getModifiers());
  }

  /** Returns true if the element is static. */
  public final boolean isStatic() {
    return Modifier.isStatic(getModifiers());
  }

  /**
   * Returns {@code true} if this method is final, per {@code Modifier.isFinal(getModifiers())}.
   *
   * <p>Note that a method may still be effectively "final", or non-overridable when it has no
   * {@code final} keyword. For example, it could be private, or it could be declared by a final
   * class. To tell whether a method is overridable, use {@link Invokable#isOverridable}.
   */
  public final boolean isFinal() {
    return Modifier.isFinal(getModifiers());
  }

  /** Returns true if the method is abstract. */
  public final boolean isAbstract() {
    return Modifier.isAbstract(getModifiers());
  }

  /** Returns true if the element is native. */
  public final boolean isNative() {
    return Modifier.isNative(getModifiers());
  }

  /** Returns true if the method is synchronized. */
  public final boolean isSynchronized() {
    return Modifier.isSynchronized(getModifiers());
  }

  /** Returns true if the field is volatile. */
  final boolean isVolatile() {
    return Modifier.isVolatile(getModifiers());
  }

  /** Returns true if the field is transient. */
  final boolean isTransient() {
    return Modifier.isTransient(getModifiers());
  }

  @Override
  public boolean equals(@Nullable Object obj) {
    if (obj instanceof Invokable) {
      Invokable<?, ?> that = (Invokable<?, ?>) obj;
      return getOwnerType().equals(that.getOwnerType()) && member.equals(that.member);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return member.hashCode();
  }

  @Override
  public String toString() {
    return member.toString();
  }

  /**
   * Returns {@code true} if this is an overridable method. Constructors, private, static or final
   * methods, or methods declared by final classes are not overridable.
   */
  public abstract boolean isOverridable();

  /** Returns {@code true} if this was declared to take a variable number of arguments. */
  public abstract boolean isVarArgs();

  /**
   * Invokes with {@code receiver} as 'this' and {@code args} passed to the underlying method and
   * returns the return value; or calls the underlying constructor with {@code args} and returns the
   * constructed instance.
   *
   * @throws IllegalAccessException if this {@code Constructor} object enforces Java language access
   *     control and the underlying method or constructor is inaccessible.
   * @throws IllegalArgumentException if the number of actual and formal parameters differ; if an
   *     unwrapping conversion for primitive arguments fails; or if, after possible unwrapping, a
   *     parameter value cannot be converted to the corresponding formal parameter type by a method
   *     invocation conversion.
   * @throws InvocationTargetException if the underlying method or constructor throws an exception.
   */
  // All subclasses are owned by us and we'll make sure to get the R type right, including nullness.
  @SuppressWarnings({"unchecked", "nullness"})
  @CanIgnoreReturnValue
  public final @Nullable R invoke(@Nullable T receiver, @Nullable Object... args)
      throws InvocationTargetException, IllegalAccessException {
    return (R) invokeInternal(receiver, checkNotNull(args));
  }

  /** Returns the return type of this {@code Invokable}. */
  // All subclasses are owned by us and we'll make sure to get the R type right.
  @SuppressWarnings("unchecked")
  public final TypeToken<? extends R> getReturnType() {
    return (TypeToken<? extends R>) TypeToken.of(getGenericReturnType());
  }

  /**
   * Returns all declared parameters of this {@code Invokable}. Note that if this is a constructor
   * of a non-static inner class, unlike {@link Constructor#getParameterTypes}, the hidden {@code
   * this} parameter of the enclosing class is excluded from the returned parameters.
   */
  @IgnoreJRERequirement
  public final ImmutableList<Parameter> getParameters() {
    Type[] parameterTypes = getGenericParameterTypes();
    Annotation[][] annotations = getParameterAnnotations();
    @Nullable Object[] annotatedTypes =
        ANNOTATED_TYPE_EXISTS ? getAnnotatedParameterTypes() : new Object[parameterTypes.length];
    ImmutableList.Builder<Parameter> builder = ImmutableList.builder();
    for (int i = 0; i < parameterTypes.length; i++) {
      builder.add(
          new Parameter(
              this, i, TypeToken.of(parameterTypes[i]), annotations[i], annotatedTypes[i]));
    }
    return builder.build();
  }

  /** Returns all declared exception types of this {@code Invokable}. */
  public final ImmutableList<TypeToken<? extends Throwable>> getExceptionTypes() {
    ImmutableList.Builder<TypeToken<? extends Throwable>> builder = ImmutableList.builder();
    for (Type type : getGenericExceptionTypes()) {
      // getGenericExceptionTypes() will never return a type that's not exception
      @SuppressWarnings("unchecked")
      TypeToken<? extends Throwable> exceptionType =
          (TypeToken<? extends Throwable>) TypeToken.of(type);
      builder.add(exceptionType);
    }
    return builder.build();
  }

  /**
   * Explicitly specifies the return type of this {@code Invokable}. For example:
   *
   * <pre>{@code
   * Method factoryMethod = Person.class.getMethod("create");
   * Invokable<?, Person> factory = Invokable.of(getNameMethod).returning(Person.class);
   * }</pre>
   */
  public final <R1 extends R> Invokable<T, R1> returning(Class<R1> returnType) {
    return returning(TypeToken.of(returnType));
  }

  /** Explicitly specifies the return type of this {@code Invokable}. */
  public final <R1 extends R> Invokable<T, R1> returning(TypeToken<R1> returnType) {
    if (!returnType.isSupertypeOf(getReturnType())) {
      throw new IllegalArgumentException(
          "Invokable is known to return " + getReturnType() + ", not " + returnType);
    }
    @SuppressWarnings("unchecked") // guarded by previous check
    Invokable<T, R1> specialized = (Invokable<T, R1>) this;
    return specialized;
  }

  @SuppressWarnings("unchecked") // The declaring class is T's raw class, or one of its supertypes.
  @Override
  public final Class<? super T> getDeclaringClass() {
    return (Class<? super T>) member.getDeclaringClass();
  }

  /** Returns the type of {@code T}. */
  // Overridden in TypeToken#method() and TypeToken#constructor()
  @SuppressWarnings("unchecked") // The declaring class is T.
  public TypeToken<T> getOwnerType() {
    return (TypeToken<T>) TypeToken.of(getDeclaringClass());
  }

  abstract @Nullable Object invokeInternal(@Nullable Object receiver, @Nullable Object[] args)
      throws InvocationTargetException, IllegalAccessException;

  abstract Type[] getGenericParameterTypes();

  @SuppressWarnings("Java7ApiChecker")
  abstract AnnotatedType[] getAnnotatedParameterTypes();

  /** This should never return a type that's not a subtype of Throwable. */
  abstract Type[] getGenericExceptionTypes();

  abstract Annotation[][] getParameterAnnotations();

  abstract Type getGenericReturnType();

  /**
   * Returns the {@link AnnotatedType} for the return type.
   *
   * @since 14.0
   */
  @SuppressWarnings("Java7ApiChecker")
  public abstract AnnotatedType getAnnotatedReturnType();

  static class MethodInvokable<T> extends Invokable<T, Object> {

    final Method method;

    MethodInvokable(Method method) {
      super(method);
      this.method = method;
    }

    @Override
    final @Nullable Object invokeInternal(@Nullable Object receiver, @Nullable Object[] args)
        throws InvocationTargetException, IllegalAccessException {
      return method.invoke(receiver, args);
    }

    @Override
    Type getGenericReturnType() {
      return method.getGenericReturnType();
    }

    @Override
    Type[] getGenericParameterTypes() {
      return method.getGenericParameterTypes();
    }

    @Override
    @SuppressWarnings("Java7ApiChecker")
    AnnotatedType[] getAnnotatedParameterTypes() {
      return method.getAnnotatedParameterTypes();
    }

    @Override
    @SuppressWarnings("Java7ApiChecker")
    public AnnotatedType getAnnotatedReturnType() {
      return method.getAnnotatedReturnType();
    }

    @Override
    Type[] getGenericExceptionTypes() {
      return method.getGenericExceptionTypes();
    }

    @Override
    final Annotation[][] getParameterAnnotations() {
      return method.getParameterAnnotations();
    }

    @Override
    public final TypeVariable<?>[] getTypeParameters() {
      return method.getTypeParameters();
    }

    @Override
    public final boolean isOverridable() {
      return !(isFinal()
          || isPrivate()
          || isStatic()
          || Modifier.isFinal(getDeclaringClass().getModifiers()));
    }

    @Override
    public final boolean isVarArgs() {
      return method.isVarArgs();
    }
  }

  static class ConstructorInvokable<T> extends Invokable<T, T> {

    final Constructor<?> constructor;

    ConstructorInvokable(Constructor<?> constructor) {
      super(constructor);
      this.constructor = constructor;
    }

    @Override
    final Object invokeInternal(@Nullable Object receiver, @Nullable Object[] args)
        throws InvocationTargetException, IllegalAccessException {
      try {
        return constructor.newInstance(args);
      } catch (InstantiationException e) {
        throw new RuntimeException(constructor + " failed.", e);
      }
    }

    /**
     * If the class is parameterized, such as {@link java.util.ArrayList ArrayList}, this returns
     * {@code ArrayList<E>}.
     */
    @Override
    Type getGenericReturnType() {
      Class<?> declaringClass = getDeclaringClass();
      TypeVariable<?>[] typeParams = declaringClass.getTypeParameters();
      if (typeParams.length > 0) {
        return Types.newParameterizedType(declaringClass, typeParams);
      } else {
        return declaringClass;
      }
    }

    @Override
    Type[] getGenericParameterTypes() {
      Type[] types = constructor.getGenericParameterTypes();
      if (types.length > 0 && mayNeedHiddenThis()) {
        Class<?>[] rawParamTypes = constructor.getParameterTypes();
        if (types.length == rawParamTypes.length
            && rawParamTypes[0] == getDeclaringClass().getEnclosingClass()) {
          // first parameter is the hidden 'this'
          return Arrays.copyOfRange(types, 1, types.length);
        }
      }
      return types;
    }

    @Override
    @SuppressWarnings("Java7ApiChecker")
    AnnotatedType[] getAnnotatedParameterTypes() {
      return constructor.getAnnotatedParameterTypes();
    }

    @Override
    @SuppressWarnings("Java7ApiChecker")
    public AnnotatedType getAnnotatedReturnType() {
      return constructor.getAnnotatedReturnType();
    }

    @Override
    Type[] getGenericExceptionTypes() {
      return constructor.getGenericExceptionTypes();
    }

    @Override
    final Annotation[][] getParameterAnnotations() {
      return constructor.getParameterAnnotations();
    }

    /**
     * {@inheritDoc}
     *
     * <p>{@code [<E>]} will be returned for ArrayList's constructor. When both the class and the
     * constructor have type parameters, the class parameters are prepended before those of the
     * constructor's. This is an arbitrary rule since no existing language spec mandates one way or
     * the other. From the declaration syntax, the class type parameter appears first, but the call
     * syntax may show up in opposite order such as {@code new <A>Foo<B>()}.
     */
    @Override
    public final TypeVariable<?>[] getTypeParameters() {
      TypeVariable<?>[] declaredByClass = getDeclaringClass().getTypeParameters();
      TypeVariable<?>[] declaredByConstructor = constructor.getTypeParameters();
      TypeVariable<?>[] result =
          new TypeVariable<?>[declaredByClass.length + declaredByConstructor.length];
      System.arraycopy(declaredByClass, 0, result, 0, declaredByClass.length);
      System.arraycopy(
          declaredByConstructor, 0, result, declaredByClass.length, declaredByConstructor.length);
      return result;
    }

    @Override
    public final boolean isOverridable() {
      return false;
    }

    @Override
    public final boolean isVarArgs() {
      return constructor.isVarArgs();
    }

    private boolean mayNeedHiddenThis() {
      Class<?> declaringClass = constructor.getDeclaringClass();
      if (declaringClass.getEnclosingConstructor() != null) {
        // Enclosed in a constructor, needs hidden this
        return true;
      }
      Method enclosingMethod = declaringClass.getEnclosingMethod();
      if (enclosingMethod != null) {
        // Enclosed in a method, if it's not static, must need hidden this.
        return !Modifier.isStatic(enclosingMethod.getModifiers());
      } else {
        // Strictly, this doesn't necessarily indicate a hidden 'this' in the case of
        // static initializer. But there seems no way to tell in that case. :(
        // This may cause issues when an anonymous class is created inside a static initializer,
        // and the class's constructor's first parameter happens to be the enclosing class.
        // In such case, we may mistakenly think that the class is within a non-static context
        // and the first parameter is the hidden 'this'.
        return declaringClass.getEnclosingClass() != null
            && !Modifier.isStatic(declaringClass.getModifiers());
      }
    }
  }

  private static final boolean ANNOTATED_TYPE_EXISTS = initAnnotatedTypeExists();

  private static boolean initAnnotatedTypeExists() {
    try {
      Class.forName("java.lang.reflect.AnnotatedType");
    } catch (ClassNotFoundException e) {
      return false;
    }
    return true;
  }
}
