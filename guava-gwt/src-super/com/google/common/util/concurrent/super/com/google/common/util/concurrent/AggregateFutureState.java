/*
 * Copyright (C) 2006 The Guava Authors
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

import static com.google.common.collect.Sets.newHashSet;

import java.util.Set;

/** Emulation of AggregateFutureState. */
abstract class AggregateFutureState<OutputT> extends AbstractFuture.TrustedFuture<OutputT> {
  // Lazily initialized the first time we see an exception; not released until all the input futures
  // & this future completes. Released when the future releases the reference to the running state
  private Set<Throwable> seenExceptions = null;
  private int remaining;

  AggregateFutureState(int remainingFutures) {
    this.remaining = remainingFutures;
  }

  final Set<Throwable> getOrInitSeenExceptions() {
    if (seenExceptions == null) {
      seenExceptions = newHashSet();
      addInitialException(seenExceptions);
    }
    return seenExceptions;
  }

  abstract void addInitialException(Set<Throwable> seen);

  final int decrementRemainingAndGet() {
    return --remaining;
  }

  final void clearSeenExceptions() {
    seenExceptions = null;
  }
}
