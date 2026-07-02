/*
 * Copyright (C) 2026 The Guava Authors
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

import com.google.common.util.concurrent.AbstractFuture.TrustedFuture;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Interface that increases the visibility of some methods on {@link AbstractFuture} to work around
 * the runtime crashes from <a href="https://youtrack.jetbrains.com/issue/KT-67447">KT-67447</a>.
 * See also issue 320650932.
 */
@NullMarked
interface SettableAbstractFuture<V extends @Nullable Object> {
  /*
   * We'd like to use the method names from `AbstractFuture`, like "`setException`." But that would
   * break the declaration `F extends AbstractFuture<...> & SettableAbstractFuture<...>` in
   * `AbstractAbstractFutureTest`, as required by the JLS and discussed in
   * https://bugs.openjdk.org/browse/JDK-6946211.
   *
   * We can't reuse the name "`setExceptionInternal`" from `AbstractFuture` itself because the
   * method of that name on `AbstractFuture` is `final`.
   *
   * We can't override `wasInterrupted`/`wasInterruptedInternal` and `tryInternalFastPathGetFailure`
   * no matter what, since they're `final`.
   *
   * We *could* shadow(?) the `final` method `tryInternalFastPathGetFailureInternal`, since it's
   * declared in another package, but at this point, it seems clearest to just follow our `do*`
   * naming convention.
   */
  @CanIgnoreReturnValue
  boolean doSet(@ParametricNullness V value);

  @CanIgnoreReturnValue
  boolean doSetException(Throwable throwable);

  @CanIgnoreReturnValue
  boolean doSetFuture(ListenableFuture<? extends V> future);

  boolean doWasInterrupted();

  // We could add doTryInternalFastPathGetFailure (already implemented by both classes) if desired.

  final class UntrustedAbstractFuture<V extends @Nullable Object> extends AbstractFuture<V>
      implements SettableAbstractFuture<V> {
    @CanIgnoreReturnValue
    @Override
    public boolean doSet(@ParametricNullness V value) {
      return set(value);
    }

    @CanIgnoreReturnValue
    @Override
    public boolean doSetException(Throwable throwable) {
      return setException(throwable);
    }

    @CanIgnoreReturnValue
    @Override
    public boolean doSetFuture(ListenableFuture<? extends V> future) {
      return setFuture(future);
    }

    @Override
    public boolean doWasInterrupted() {
      return wasInterrupted();
    }

    public final @Nullable Throwable doTryInternalFastPathGetFailure() {
      return tryInternalFastPathGetFailure();
    }
  }

    final class TrustedAbstractFuture<V extends @Nullable Object> extends TrustedFuture<V>
      implements SettableAbstractFuture<V> {
    @CanIgnoreReturnValue
    @Override
    public boolean doSet(@ParametricNullness V value) {
      return set(value);
    }

    @CanIgnoreReturnValue
    @Override
    public boolean doSetException(Throwable throwable) {
      return setException(throwable);
    }

    @CanIgnoreReturnValue
    @Override
    public boolean doSetFuture(ListenableFuture<? extends V> future) {
      return setFuture(future);
    }

    @Override
    public boolean doWasInterrupted() {
      return wasInterrupted();
    }

    public final @Nullable Throwable doTryInternalFastPathGetFailure() {
      return tryInternalFastPathGetFailure();
    }
  }
}
