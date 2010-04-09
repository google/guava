/*
 * Copyright (C) 2007 Google Inc.
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

import java.util.HashMap;
import java.util.Map;

/**
 * This class provides default values for all Java types, as defined by the JLS.
 *
 * @author Ben Yu
 */
public final class Defaults {
  private Defaults() {}

  private static final Map<Class<?>, Object> DEFAULTS =
      new HashMap<Class<?>, Object>(16);

  private static <T> void put(Class<T> type, T value) {
    DEFAULTS.put(type, value);
  }

  static {
    put(boolean.class, false);
    put(char.class, '\0');
    put(byte.class, (byte) 0);
    put(short.class, (short) 0);
    put(int.class, 0);
    put(long.class, 0L);
    put(float.class, 0f);
    put(double.class, 0d);
  }

  /**
   * Returns the default value of {@code type} as defined by JLS --- {@code 0}
   * for numbers, {@code false} for {@code boolean} and {@code '\0'} for {@code
   * char}. For non-primitive types and {@code void}, null is returned.
   */
  @SuppressWarnings("unchecked")
  public static <T> T defaultValue(Class<T> type) {
    return (T) DEFAULTS.get(type);
  }
}
