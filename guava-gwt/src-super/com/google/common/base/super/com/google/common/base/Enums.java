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

package com.google.common.base;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.annotations.Beta;
import com.google.common.annotations.GwtCompatible;

import java.io.Serializable;

import javax.annotation.Nullable;

/**
 * Utility methods for working with {@link Enum} instances.
 *
 * @author Steve McKay
 *
 * @since 9.0
 */
@GwtCompatible(emulated = true)
@Beta
public final class Enums {

  private Enums() {}

  /**
   * Returns an optional enum constant for the given type, using {@link Enum#valueOf}. If the
   * constant does not exist, {@link Optional#absent} is returned. A common use case is for parsing
   * user input or falling back to a default enum constant. For example,
   * {@code Enums.getIfPresent(Country.class, countryInput).or(Country.DEFAULT);}
   *
   * @since 12.0
   */
  public static <T extends Enum<T>> Optional<T> getIfPresent(
      Class<T> enumClass, String value) {
    checkNotNull(enumClass);
    checkNotNull(value);
    return Platform.getEnumIfPresent(enumClass, value);
  }

  /**
   * Returns a converter that converts between strings and {@code enum} values of type
   * {@code enumClass} using {@link Enum#valueOf(Class, String)} and {@link Enum#name()}. The
   * converter will throw an {@code IllegalArgumentException} if the argument is not the name of
   * any enum constant in the specified enum.
   *
   * @since 16.0
   */
  public static <T extends Enum<T>> Converter<String, T> stringConverter(final Class<T> enumClass) {
    return new StringConverter<T>(enumClass);
  }

  private static final class StringConverter<T extends Enum<T>>
      extends Converter<String, T> implements Serializable {

    private final Class<T> enumClass;

    StringConverter(Class<T> enumClass) {
      this.enumClass = checkNotNull(enumClass);
    }

    @Override
    protected T doForward(String value) {
      return Enum.valueOf(enumClass, value);
    }

    @Override
    protected String doBackward(T enumValue) {
      return enumValue.name();
    }

    @Override
    public boolean equals(@Nullable Object object) {
      if (object instanceof StringConverter) {
        StringConverter<?> that = (StringConverter<?>) object;
        return this.enumClass.equals(that.enumClass);
      }
      return false;
    }

    @Override
    public int hashCode() {
      return enumClass.hashCode();
    }

    @Override
    public String toString() {
      return "Enums.stringConverter(" + enumClass.getName() + ".class)";
    }

    private static final long serialVersionUID = 0L;
  }
}

