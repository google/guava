/*
 * Copyright (C) 2006 The Guava Authors
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

import static com.google.common.util.concurrent.Futures.getDone;

import com.google.common.base.MoreObjects;
import com.google.common.util.concurrent.internal.InternalFutureFailureAccess;
import com.google.common.util.concurrent.internal.InternalFutures;
import java.util.concurrent.ExecutionException;
import org.jspecify.annotations.Nullable;

/** See {@link Futures#addCallback(ListenableFuture, FutureCallback, java.util.concurrent.Executor)}. */
final class CallbackListener<V extends @Nullable Object> implements Runnable {
  final ListenableFuture<V> future;
  final FutureCallback<? super V> callback;

  CallbackListener(ListenableFuture<V> future, FutureCallback<? super V> callback) {
    this.future = future;
    this.callback = callback;
  }

  @Override
  public void run() {
    if (future instanceof InternalFutureFailureAccess) {
      Throwable failure =
          InternalFutures.tryInternalFastPathGetFailure((InternalFutureFailureAccess) future);
      if (failure != null) {
        callback.onFailure(failure);
        return;
      }
    }
    V value;
    try {
      value = getDone(future);
    } catch (ExecutionException e) {
      callback.onFailure(e.getCause());
      return;
    } catch (Throwable e) {
      // Any Exception is either a RuntimeException or sneaky checked exception.
      callback.onFailure(e);
      return;
    }
    callback.onSuccess(value);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this).addValue(callback).toString();
  }
}
