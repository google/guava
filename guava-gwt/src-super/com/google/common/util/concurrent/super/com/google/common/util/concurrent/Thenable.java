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

import elemental2.promise.IThenable;
import jsinterop.annotations.JsFunction;
import jsinterop.annotations.JsOptional;
import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsType;

/**
 * Subset of the elemental2 IThenable interface without the single-parameter overload, which allows
 * us to implement it using a default implementation in J2cl ListenableFuture.
 */
@JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "IThenable")
interface Thenable<T> {
  <V> IThenable<V> then(
      @JsOptional ThenOnFulfilledCallbackFn<? super T, ? extends V> onFulfilled,
      @JsOptional ThenOnRejectedCallbackFn<? extends V> onRejected);

  @JsFunction
  interface ThenOnFulfilledCallbackFn<T, V> {
    V onInvoke(T p0);
  }

  @JsFunction
  interface ThenOnRejectedCallbackFn<V> {
    V onInvoke(Object p0);
  }
}
