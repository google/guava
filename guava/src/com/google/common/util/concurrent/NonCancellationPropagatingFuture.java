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

import com.google.errorprone.annotations.concurrent.LazyInit;
import com.google.j2objc.annotations.RetainedLocalRef;
import org.jspecify.annotations.Nullable;

/** A wrapped future that does not propagate cancellation to its delegate. */
final class NonCancellationPropagatingFuture<V extends @Nullable Object>
    extends AbstractFuture.TrustedFuture<V> implements Runnable {
  @LazyInit private @Nullable ListenableFuture<V> delegate;

  NonCancellationPropagatingFuture(ListenableFuture<V> delegate) {
    this.delegate = delegate;
  }

  @Override
  public void run() {
    // This prevents cancellation from propagating because we don't call setFuture(delegate) until
    // delegate is already done, so calling cancel() on this future won't affect it.
    @RetainedLocalRef ListenableFuture<V> localDelegate = delegate;
    if (localDelegate != null) {
      setFuture(localDelegate);
    }
  }

  @Override
  protected @Nullable String pendingToString() {
    @RetainedLocalRef ListenableFuture<V> localDelegate = delegate;
    if (localDelegate != null) {
      return "delegate=[" + localDelegate + "]";
    }
    return null;
  }

  @Override
  protected void afterDone() {
    delegate = null;
  }
}
