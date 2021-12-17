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

import com.google.common.base.Function;
import java.util.concurrent.Executor;

/**
 * Hidden superclass of {@link FluentFuture} that provides us a place to declare special GWT
 * versions of the {@link FluentFuture#catching(Class, com.google.common.base.Function)
 * FluentFuture.catching} family of methods. Those versions have slightly different signatures.
 */
abstract class GwtFluentFutureCatchingSpecialization<V> extends AbstractFuture<V> {
  /*
   * In the GWT versions of the methods (below), every exceptionType parameter is required to be
   * Class<Throwable>. To handle only certain kinds of exceptions under GWT, you'll need to write
   * your own instanceof tests.
   */

  public final FluentFuture<V> catching(
      Class<Throwable> exceptionType,
      Function<? super Throwable, ? extends V> fallback,
      Executor executor) {
    return (FluentFuture<V>) Futures.catching(this, exceptionType, fallback, executor);
  }

  public final FluentFuture<V> catchingAsync(
      Class<Throwable> exceptionType,
      AsyncFunction<? super Throwable, ? extends V> fallback,
      Executor executor) {
    return (FluentFuture<V>) Futures.catchingAsync(this, exceptionType, fallback, executor);
  }
}
