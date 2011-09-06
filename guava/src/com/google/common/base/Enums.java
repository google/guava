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
@GwtCompatible
@Beta
public final class Enums {

  private Enums() {}

  /**
   * Returns a {@link Function} that maps an {@link Enum} name to the associated
   * {@code Enum} constant. The {@code Function} will return {@code null} if the
   * {@code Enum} constant does not exist.
   *
   * @param enumClass the {@link Class} of the {@code Enum} declaring the
   *     constant values.
   */
  public static <T extends Enum<T>> Function<String, T> valueOfFunction(Class<T> enumClass) {
    return new ValueOfFunction<T>(enumClass);
  }

  /**
   * {@link Function} that maps an {@link Enum} name to the associated
   * constant, or {@code null} if the constant does not exist.
   */
  private static final class ValueOfFunction<T extends Enum<T>> implements
      Function<String, T>, Serializable {

    private final Class<T> enumClass;

    private ValueOfFunction(Class<T> enumClass) {
      this.enumClass = checkNotNull(enumClass);
    }

    @Override
    public T apply(String value) {
      try {
        return Enum.valueOf(enumClass, value);
      } catch (IllegalArgumentException e) {
        return null;
      }
    }

    @Override public boolean equals(@Nullable Object obj) {
      return obj instanceof ValueOfFunction &&
          enumClass.equals(((ValueOfFunction) obj).enumClass);
    }

    @Override public int hashCode() {
      return enumClass.hashCode();
    }

    @Override public String toString() {
      return "Enums.valueOf(" + enumClass + ")";
    }

    private static final long serialVersionUID = 0;
  }
}
