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

import com.google.common.annotations.Beta;
import com.google.errorprone.annotations.CanIgnoreReturnValue;

import java.util.Map;

import javax.annotation.Nullable;

/**
 * A map, each entry of which maps a {@link TypeToken} to an instance of that type. In addition to
 * implementing {@code Map}, the additional type-safe operations {@link #putInstance} and
 * {@link #getInstance} are available.
 *
 * <p>Generally, implementations don't support {@link #put} and {@link #putAll} because there is no
 * way to check an object at runtime to be an instance of a {@link TypeToken}. Instead, caller
 * should use the type safe {@link #putInstance}.
 *
 * <p>Also, if caller suppresses unchecked warnings and passes in an {@code Iterable<String>} for
 * type {@code Iterable<Integer>}, the map won't be able to detect and throw type error.
 *
 * <p>Like any other {@code Map<Class, Object>}, this map may contain entries for primitive types,
 * and a primitive type and its corresponding wrapper type may map to different values.
 *
 * @param <B> the common supertype that all entries must share; often this is simply {@link Object}
 *
 * @author Ben Yu
 * @since 13.0
 */
@Beta
public interface TypeToInstanceMap<B> extends Map<TypeToken<? extends B>, B> {

  /**
   * Returns the value the specified class is mapped to, or {@code null} if no entry for this class
   * is present. This will only return a value that was bound to this specific class, not a value
   * that may have been bound to a subtype.
   *
   * <p>{@code getInstance(Foo.class)} is equivalent to
   * {@code getInstance(TypeToken.of(Foo.class))}.
   */
  @Nullable
  <T extends B> T getInstance(Class<T> type);

  /**
   * Maps the specified class to the specified value. Does <i>not</i> associate this value with any
   * of the class's supertypes.
   *
   * <p>{@code putInstance(Foo.class, foo)} is equivalent to
   * {@code putInstance(TypeToken.of(Foo.class), foo)}.
   *
   * @return the value previously associated with this class (possibly {@code null}), or
   *     {@code null} if there was no previous entry.
   */
  @Nullable
  @CanIgnoreReturnValue
  <T extends B> T putInstance(Class<T> type, @Nullable T value);

  /**
   * Returns the value the specified type is mapped to, or {@code null} if no entry for this type is
   * present. This will only return a value that was bound to this specific type, not a value that
   * may have been bound to a subtype.
   */
  @Nullable
  <T extends B> T getInstance(TypeToken<T> type);

  /**
   * Maps the specified type to the specified value. Does <i>not</i> associate this value with any
   * of the type's supertypes.
   *
   * @return the value previously associated with this type (possibly {@code null}), or {@code null}
   *     if there was no previous entry.
   */
  @Nullable
  @CanIgnoreReturnValue
  <T extends B> T putInstance(TypeToken<T> type, @Nullable T value);
}
