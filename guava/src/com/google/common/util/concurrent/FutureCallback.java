/*
 * Copyright (C) 2011 The Guava Authors
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

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import javax.annotation.Nullable;

/**
 * A callback for accepting the results of a {@link java.util.concurrent.Future} computation
 * asynchronously.
 *
 * <p>To attach to a {@link ListenableFuture} use {@link Futures#addCallback}.
 *
 * @author Anthony Zana
 * @since 10.0
 */
@GwtCompatible
public interface FutureCallback<V> {
  /**
   * Invoked with the result of the {@code Future} computation when it is successful.
   */
  void onSuccess(@Nullable V result);

  /**
   * Invoked when a {@code Future} computation fails or is canceled.
   *
   * <p>If the future's {@link Future#get() get} method throws an {@link ExecutionException}, then
   * the cause is passed to this method. Any other thrown object is passed unaltered.
   */
  void onFailure(Throwable t);
}
