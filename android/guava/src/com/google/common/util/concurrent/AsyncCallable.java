/*
 * Copyright (C) 2015 The Guava Authors
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

package com.google.common.util.concurrent;

import com.google.common.annotations.GwtCompatible;
import java.util.concurrent.Future;
import org.jspecify.annotations.Nullable;

/**
 * Computes a value, possibly asynchronously. For an example usage and more information, see {@link
 * Futures.FutureCombiner#callAsync(AsyncCallable, java.util.concurrent.Executor)}.
 *
 * <p>Much like {@link java.util.concurrent.Callable}, but returning a {@link ListenableFuture}
 * result.
 *
 * @since 20.0
 */
@GwtCompatible
public interface AsyncCallable<V extends @Nullable Object> {
  /**
   * Computes a result {@code Future}. The output {@code Future} need not be {@linkplain
   * Future#isDone done}, making {@code AsyncCallable} suitable for asynchronous derivations.
   *
   * <p>Throwing an exception from this method is equivalent to returning a failing {@link
   * ListenableFuture}.
   */
  ListenableFuture<V> call() throws Exception;
}
