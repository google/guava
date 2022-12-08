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
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Hidden superclass of {@link Futures} that provides us a place to declare special GWT versions of
 * the {@link Futures#catching(ListenableFuture, Class, com.google.common.base.Function)
 * Futures.catching} family of methods. Those versions have slightly different signatures.
 */
@ElementTypesAreNonnullByDefault
abstract class GwtFuturesCatchingSpecialization {
  /*
   * In the GWT versions of the methods (below), every exceptionType parameter is required to be
   * Class<Throwable>. To handle only certain kinds of exceptions under GWT, you'll need to write
   * your own instanceof tests.
   */

  public static <V extends @Nullable Object> ListenableFuture<V> catching(
      ListenableFuture<? extends V> input,
      Class<Throwable> exceptionType,
      Function<? super Throwable, ? extends V> fallback,
      Executor executor) {
    return AbstractCatchingFuture.create(input, exceptionType, fallback, executor);
  }

  public static <V extends @Nullable Object> ListenableFuture<V> catchingAsync(
      ListenableFuture<? extends V> input,
      Class<Throwable> exceptionType,
      AsyncFunction<? super Throwable, ? extends V> fallback,
      Executor executor) {
    return AbstractCatchingFuture.create(input, exceptionType, fallback, executor);
  }
}
