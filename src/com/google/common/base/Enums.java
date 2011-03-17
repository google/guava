// Copyright 2011 Google Inc. All Rights Reserved.

package com.google.common.base;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.annotations.Beta;
import com.google.common.annotations.GwtCompatible;

/**
 * Utility methods for working with {@link Enum} instances.
 *
 * @author smckay@google.com (Steve McKay)
 *
 * @since 9
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
      Function<String, T> {

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
  }
}
