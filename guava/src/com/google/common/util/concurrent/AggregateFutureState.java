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

import static java.util.concurrent.atomic.AtomicIntegerFieldUpdater.newUpdater;
import static java.util.concurrent.atomic.AtomicReferenceFieldUpdater.newUpdater;

import com.google.common.annotations.GwtCompatible;
import com.google.common.collect.Sets;

import java.util.Set;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

/**
 * A helper which does some thread-safe operations for aggregate futures, which must be implemented
 * differently in GWT.  Namely:
 * <p>Lazily initializes a set of seen exceptions
 * <p>Decrements a counter atomically
 */
@GwtCompatible(emulated = true)
abstract class AggregateFutureState {
  @SuppressWarnings({"rawtypes", "unchecked"})
  private static final AtomicReferenceFieldUpdater<AggregateFutureState, Set<Throwable>>
      SEEN_EXCEPTIONS_UDPATER = newUpdater(
          AggregateFutureState.class, (Class) Set.class, "seenExceptions");

  private static final AtomicIntegerFieldUpdater<AggregateFutureState>
      REMAINING_COUNT_UPDATER = newUpdater(AggregateFutureState.class, "remaining");

  // Initialized once the first time we see an exception
  private volatile Set<Throwable> seenExceptions = null;
  @SuppressWarnings("unused") private volatile int remaining;

  AggregateFutureState(int remainingFutures) {
    this.remaining = remainingFutures;
  }

  final Set<Throwable> getSeenExceptions() {
    Set<Throwable> seenExceptionsLocal = seenExceptions;
    if (seenExceptionsLocal == null) {
      SEEN_EXCEPTIONS_UDPATER.compareAndSet(
          this, null, Sets.<Throwable>newConcurrentHashSet());
      seenExceptionsLocal = seenExceptions;
    }
    return seenExceptionsLocal;
  }

  final int decrementRemainingAndGet() {
    return REMAINING_COUNT_UPDATER.decrementAndGet(this);
  }

  void releaseResourcesAfterFailure() {
    seenExceptions = null;
  }
}
