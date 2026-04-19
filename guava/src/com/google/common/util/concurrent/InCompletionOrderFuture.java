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

import static java.util.Objects.requireNonNull;

import org.jspecify.annotations.Nullable;

// This can't be a TrustedFuture, because TrustedFuture has clever optimizations that
// mean cancel won't be called if this Future is passed into setFuture, and then
// cancelled.
final class InCompletionOrderFuture<T extends @Nullable Object> extends AbstractFuture<T> {
  private @Nullable InCompletionOrderState<T> state;

  InCompletionOrderFuture(InCompletionOrderState<T> state) {
    this.state = state;
  }

  @Override
  public boolean cancel(boolean interruptIfRunning) {
    InCompletionOrderState<T> localState = state;
    if (super.cancel(interruptIfRunning)) {
      /*
       * requireNonNull is generally safe: If cancel succeeded, then this Future was still pending,
       * so its `state` field hasn't been nulled out yet.
       *
       * OK, it's technically possible for this to fail in the presence of unsafe publishing, as
       * discussed in the comments in TimeoutFuture. TODO(cpovirk): Maybe check for null before
       * calling recordOutputCancellation?
       */
      requireNonNull(localState).recordOutputCancellation(interruptIfRunning);
      return true;
    }
    return false;
  }

  @Override
  protected void afterDone() {
    state = null;
  }

  @Override
  protected @Nullable String pendingToString() {
    InCompletionOrderState<T> localState = state;
    if (localState != null) {
      // Don't print the actual array! We don't want inCompletionOrder(list).toString() to have
      // quadratic output.
      return "inputCount=["
          + localState.inputFutures.length
          + "], remaining=["
          + localState.incompleteOutputCount.get()
          + "]";
    }
    return null;
  }
}
