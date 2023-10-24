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

import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import jsinterop.annotations.JsFunction;
import jsinterop.annotations.JsMethod;
import jsinterop.annotations.JsOptional;
import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsType;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Java super source for ListenableFuture, implementing a structural thenable via a default method.
 * For restrictions, please refer to the documentation of the then() method.
 */
@ElementTypesAreNonnullByDefault
public interface ListenableFuture<V extends @Nullable Object> extends Future<V>, IThenable<V> {
  void addListener(Runnable listener, Executor executor);

  /** Note that this method is not expected to be overridden. */
  @JsMethod
  @Override
  default <R extends @Nullable Object> IThenable<R> then(
      @JsOptional @Nullable IThenOnFulfilledCallbackFn<? super V, ? extends R> onFulfilled,
      @JsOptional @Nullable IThenOnRejectedCallbackFn<? extends R> onRejected) {
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
        .then(onFulfilled, onRejected);
  }
}

/**
 * Subset of the elemental2 IThenable interface without the single-parameter overload, which allows
 * us to implement it using a default implementation in J2cl ListenableFuture.
 */
@JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "IThenable")
interface IThenable<T extends @Nullable Object> {
  <V extends @Nullable Object> IThenable<V> then(
      @JsOptional @Nullable IThenOnFulfilledCallbackFn<? super T, ? extends V> onFulfilled,
      @JsOptional @Nullable IThenOnRejectedCallbackFn<? extends V> onRejected);

  @JsFunction
  interface IThenOnFulfilledCallbackFn<T extends @Nullable Object, V extends @Nullable Object> {
    V onInvoke(T p0);
  }

  @JsFunction
  interface IThenOnRejectedCallbackFn<V extends @Nullable Object> {
    V onInvoke(Object p0);
  }
}

/** Subset of the elemental2 Promise API. */
@JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "Promise")
class Promise<T extends @Nullable Object> implements IThenable<T> {

  @JsFunction
  interface PromiseExecutorCallbackFn<T extends @Nullable Object> {
    @JsFunction
    interface ResolveCallbackFn<T extends @Nullable Object> {
      void onInvoke(T value);
    }

    @JsFunction
    interface RejectCallbackFn {
      void onInvoke(Object error);
    }

    void onInvoke(ResolveCallbackFn<T> resolve, RejectCallbackFn reject);
  }

  public Promise(PromiseExecutorCallbackFn<T> executor) {}

  @Override
  public native <V extends @Nullable Object> Promise<V> then(
      @JsOptional @Nullable IThenOnFulfilledCallbackFn<? super T, ? extends V> onFulfilled,
      @JsOptional @Nullable IThenOnRejectedCallbackFn<? extends V> onRejected);
}
