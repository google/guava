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

package com.google.common.util.concurrent;

import com.google.common.annotations.Beta;
import com.google.common.annotations.GwtCompatible;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * Provides a backup {@code Future} to replace an earlier failed {@code Future}. An implementation
 * of this interface can be applied to an input {@code Future} with {@link Futures#withFallback}.
 *
 * @param <V> the result type of the provided backup {@code Future}
 *
 * @author Bruno Diniz
 * @since 14.0
 * @deprecated This interface's main user, {@link Futures#withFallback(ListenableFuture,
 *     FutureFallback) Futures.withFallback}, has been updated to use {@link AsyncFunction}. We
 *     recommend that other APIs be updated in the same way. This interface will be removed in Guava
 *     release 20.0.
 */
@Beta
@Deprecated
@GwtCompatible
public interface FutureFallback<V> {
  /**
   * Returns a {@code Future} to be used in place of the {@code Future} that failed with the given
   * exception. The exception is provided so that the {@code Fallback} implementation can
   * conditionally determine whether to propagate the exception or to attempt to recover.
   *
   * @param t the exception that made the future fail. If the future's {@link Future#get() get}
   *     method throws an {@link ExecutionException}, then the cause is passed to this method. Any
   *     other thrown object is passed unaltered.
   */
  ListenableFuture<V> create(Throwable t) throws Exception;
}
