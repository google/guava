/*
 * Copyright (C) 2015 The Guava Authors
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

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import org.checkerframework.checker.nullness.qual.Nullable;

/** Emulation of Uninterruptibles in GWT. */
@ElementTypesAreNonnullByDefault
public final class Uninterruptibles {

  private Uninterruptibles() {}

  @CanIgnoreReturnValue
  public static <V extends @Nullable Object> V getUninterruptibly(Future<V> future)
      throws ExecutionException {
    try {
      return future.get();
    } catch (InterruptedException e) {
      // Should never be thrown in GWT but play it safe
      throw new IllegalStateException(e);
    }
  }
}
