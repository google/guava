/*
 * Copyright (C) 2008 The Guava Authors
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

import com.google.common.annotations.Beta;
import com.google.common.annotations.GwtCompatible;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * A {@code CheckedFuture} is a {@link ListenableFuture} that includes versions of the {@code get}
 * methods that can throw a checked exception. This makes it easier to create a future that executes
 * logic which can throw an exception.
 *
 * <p><b>Warning:</b> We recommend against using {@code CheckedFuture} in new projects. {@code
 * CheckedFuture} is difficult to build libraries atop. {@code CheckedFuture} ports of methods like
 * {@link Futures#transformAsync} have historically had bugs, and some of these bugs are necessary,
 * unavoidable consequences of the {@code CheckedFuture} API. Additionally, {@code CheckedFuture}
 * encourages users to take exceptions from one thread and rethrow them in another, producing
 * confusing stack traces.
 *
 * <p>A common implementation is {@link Futures#immediateCheckedFuture}.
 *
 * <p>Implementations of this interface must adapt the exceptions thrown by {@code Future#get()}:
 * {@link CancellationException}, {@link ExecutionException} and {@link InterruptedException} into
 * the type specified by the {@code X} type parameter.
 *
 * <p>This interface also extends the ListenableFuture interface to allow listeners to be added.
 * This allows the future to be used as a normal {@link Future} or as an asynchronous callback
 * mechanism as needed. This allows multiple callbacks to be registered for a particular task, and
 * the future will guarantee execution of all listeners when the task completes.
 *
 * <p>For a simpler alternative to CheckedFuture, consider accessing Future values with {@link
 * Futures#getChecked(Future, Class) Futures.getChecked()}.
 *
 * @author Sven Mawson
 * @since 1.0
 * @deprecated {@link CheckedFuture} cannot properly support the chained operations that are the
 *     primary goal of {@link ListenableFuture}. {@code CheckedFuture} also encourages users to
 *     rethrow exceptions from one thread in another thread, producing misleading stack traces.
 *     Additionally, it has a surprising policy about which exceptions to map and which to leave
 *     untouched. Guava users who want a {@code CheckedFuture} can fork the classes for their own
 *     use, possibly specializing them to the particular exception type they use. We recommend that
 *     most people use {@code ListenableFuture} and perform any exception wrapping themselves. This
 *     class is scheduled for removal from Guava in October 2018.
 */
@Beta
@CanIgnoreReturnValue
@Deprecated
@GwtCompatible
public interface CheckedFuture<V, X extends Exception> extends ListenableFuture<V> {

  /**
   * Exception checking version of {@link Future#get()} that will translate {@link
   * InterruptedException}, {@link CancellationException} and {@link ExecutionException} into
   * application-specific exceptions.
   *
   * @return the result of executing the future.
   * @throws X on interruption, cancellation or execution exceptions.
   */
  V checkedGet() throws X;

  /**
   * Exception checking version of {@link Future#get(long, TimeUnit)} that will translate {@link
   * InterruptedException}, {@link CancellationException} and {@link ExecutionException} into
   * application-specific exceptions. On timeout this method throws a normal {@link
   * TimeoutException}.
   *
   * @return the result of executing the future.
   * @throws TimeoutException if retrieving the result timed out.
   * @throws X on interruption, cancellation or execution exceptions.
   */
  V checkedGet(long timeout, TimeUnit unit) throws TimeoutException, X;
}
