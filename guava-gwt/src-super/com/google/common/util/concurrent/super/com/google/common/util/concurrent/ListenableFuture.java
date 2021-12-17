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

package com.google.common.util.concurrent;

import elemental2.promise.IThenable;
import elemental2.promise.Promise;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import jsinterop.annotations.JsMethod;
import jsinterop.annotations.JsOptional;

/**
 * Java super source for ListenableFuture, implementing a structural thenable via a default method.
 * For restrictions, please refer to the documentation of the then() method.
 *
 * <p>This class is not (explicitly) implementing IThenable<V> because "then" is overloaded there
 * and the single parameter version would need to be marked native, which does not seem to be
 * feasible in interfaces (see "subclassing a class with overloaded methods" in jsinterop
 * documentation).
 */
public interface ListenableFuture<V> extends Future<V>, Thenable<V> {
  void addListener(Runnable listener, Executor executor);

  /** Note that this method is not expected to be overridden. */
  @JsMethod
  @Override
  default <R> IThenable<R> then(
      @JsOptional ThenOnFulfilledCallbackFn<? super V, ? extends R> onFulfilled,
      @JsOptional ThenOnRejectedCallbackFn<? extends R> onRejected) {
    return new Promise<V>(
            (resolve, reject) -> {
              Futures.addCallback(
                  this,
                  new FutureCallback<V>() {
                    @Override
                    public void onSuccess(V value) {
                      resolve.onInvoke(value);
                    }

                    @Override
                    public void onFailure(Throwable throwable) {
                      reject.onInvoke(throwable.getBackingJsObject());
                    }
                  },
                  MoreExecutors.directExecutor());
            })
        .then(
            (IThenable.ThenOnFulfilledCallbackFn) onFulfilled,
            (IThenable.ThenOnRejectedCallbackFn) onRejected);
  }

  // TODO(b/141673833): If this would work, it would allow us to implement IThenable properly:
  // default <R> Promise<R> then(IThenable.ThenOnFulfilledCallbackFn<? super V, ? extends R>
  // onFulfilled) {
  //   return then(onFulfilled, null);
  // }
}
