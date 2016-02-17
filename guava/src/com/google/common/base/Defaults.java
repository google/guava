/*
 * Copyright (C) 2007 The Guava Authors
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

package com.google.common.base;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.annotations.GwtIncompatible;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nullable;

/**
 * This class provides default values for all Java types, as defined by the JLS.
 *
 * @author Ben Yu
 * @since 1.0
 */
@GwtIncompatible
public final class Defaults {
  private Defaults() {}

  private static final Map<Class<?>, Object> DEFAULTS;

  static {
    // Only add to this map via put(Map, Class<T>, T)
    Map<Class<?>, Object> map = new HashMap<Class<?>, Object>();
    put(map, boolean.class, false);
    put(map, char.class, '\0');
    put(map, byte.class, (byte) 0);
    put(map, short.class, (short) 0);
    put(map, int.class, 0);
    put(map, long.class, 0L);
    put(map, float.class, 0f);
    put(map, double.class, 0d);
    DEFAULTS = Collections.unmodifiableMap(map);
  }

  private static <T> void put(Map<Class<?>, Object> map, Class<T> type, T value) {
    map.put(type, value);
  }

  /**
   * Returns the default value of {@code type} as defined by JLS --- {@code 0} for numbers, {@code
   * false} for {@code boolean} and {@code '\0'} for {@code char}. For non-primitive types and
   * {@code void}, {@code null} is returned.
   */
  @Nullable
  public static <T> T defaultValue(Class<T> type) {
    // Primitives.wrap(type).cast(...) would avoid the warning, but we can't use that from here
    @SuppressWarnings("unchecked") // the put method enforces this key-value relationship
    T t = (T) DEFAULTS.get(checkNotNull(type));
    return t;
  }
}
