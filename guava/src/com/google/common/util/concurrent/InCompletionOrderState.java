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

import com.google.common.collect.ImmutableList;
import java.util.concurrent.atomic.AtomicInteger;
import org.jspecify.annotations.Nullable;

final class InCompletionOrderState<T extends @Nullable Object> {
  // A happens-before edge between the writes of these fields and their reads exists, because
  // in order to read these fields, the corresponding write to incompleteOutputCount must have
  // been read.
  private boolean wasCancelled = false;
  private boolean shouldInterrupt = true;
  final AtomicInteger incompleteOutputCount;
  // We set the elements of the array to null as they complete.
  final @Nullable ListenableFuture<? extends T>[] inputFutures;
  private volatile int delegateIndex = 0;

  InCompletionOrderState(ListenableFuture<? extends T>[] inputFutures) {
    this.inputFutures = inputFutures;
    incompleteOutputCount = new AtomicInteger(inputFutures.length);
  }

  void recordOutputCancellation(boolean interruptIfRunning) {
    wasCancelled = true;
    // If all the futures were cancelled with interruption, cancel the input futures
    // with interruption; otherwise cancel without
    if (!interruptIfRunning) {
      shouldInterrupt = false;
    }
    recordCompletion();
  }

  void recordInputCompletion(ImmutableList<AbstractFuture<T>> delegates, int inputFutureIndex) {
    /*
     * requireNonNull is safe because we accepted an Iterable of non-null Future instances, and we
     * don't overwrite an element in the array until after reading it.
     */
    ListenableFuture<? extends T> inputFuture = requireNonNull(inputFutures[inputFutureIndex]);
    // Null out our reference to this future, so it can be GCed
    inputFutures[inputFutureIndex] = null;
    for (int i = delegateIndex; i < delegates.size(); i++) {
      if (delegates.get(i).setFuture(inputFuture)) {
        recordCompletion();
        // this is technically unnecessary, but should speed up later accesses
        delegateIndex = i + 1;
        return;
      }
    }
    // If all the delegates were complete, no reason for the next listener to have to
    // go through the whole list. Avoids O(n^2) behavior when the entire output list is
    // cancelled.
    delegateIndex = delegates.size();
  }

  @SuppressWarnings("Interruption") // We are propagating an interrupt from a caller.
  private void recordCompletion() {
    if (incompleteOutputCount.decrementAndGet() == 0 && wasCancelled) {
      for (ListenableFuture<? extends T> toCancel : inputFutures) {
        if (toCancel != null) {
          toCancel.cancel(shouldInterrupt);
        }
      }
    }
  }
}
