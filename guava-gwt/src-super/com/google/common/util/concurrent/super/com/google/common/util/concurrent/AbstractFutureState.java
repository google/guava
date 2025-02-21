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

import com.google.common.util.concurrent.AbstractFuture.Listener;
import com.google.common.util.concurrent.internal.InternalFutureFailureAccess;
import org.jspecify.annotations.Nullable;

abstract class AbstractFutureState<V extends @Nullable Object> extends InternalFutureFailureAccess
    implements ListenableFuture<V> {
  final boolean casListeners(@Nullable Listener expect, Listener update) {
    if (listeners == expect) {
      listeners = update;
      return true;
    }
    return false;
  }

  final @Nullable Listener gasListeners(Listener update) {
    Listener old = listeners;
    listeners = update;
    return old;
  }

  static boolean casValue(AbstractFutureState<?> future, @Nullable Object expect, Object update) {
    if (future.value == expect) {
      future.value = update;
      return true;
    }
    return false;
  }

  final @Nullable Object value() {
    return value;
  }

  final @Nullable Listener listeners() {
    return listeners;
  }

  final void releaseWaiters() {}

  AbstractFutureState() {}

  static final Object NULL = new Object();

  static final LazyLogger log = new LazyLogger(AbstractFuture.class);

  static final boolean GENERATE_CANCELLATION_CAUSES = false;

  private volatile @Nullable Object value;

  private volatile @Nullable Listener listeners;
}
