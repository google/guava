/*
 * Copyright (C) 2009 Google Inc.
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

package com.google.common.util.concurrent;

import java.util.concurrent.Callable;

import javax.annotation.Nullable;

/**
 * Static utility methods pertaining to the {@link Callable} interface.
 *
 * @author Isaac Shum
 * @since 2009.09.15 <b>tentative</b>
 */
public final class Callables {
  private Callables() {}

  /**
   * Creates a {@code Callable} which immediately returns a preset value each
   * time it is called.
   */
  public static <T> Callable<T> returning(final @Nullable T value) {
    return new Callable<T>() {
      /*@Override*/ public T call() {
        return value;
      }
    };
  }
}
