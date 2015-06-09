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

import static com.google.common.collect.Sets.newConcurrentHashSet;
import static java.util.concurrent.atomic.AtomicIntegerFieldUpdater.newUpdater;
import static java.util.concurrent.atomic.AtomicReferenceFieldUpdater.newUpdater;

import com.google.common.annotations.GwtCompatible;

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

  // Lazily initialized the first time we see an exception; not released until all the input futures
  // & this future completes. Released when the future releases the reference to the running state
  private volatile Set<Throwable> seenExceptions = null;
  @SuppressWarnings("unused") private volatile int remaining;

  AggregateFutureState(int remainingFutures) {
    this.remaining = remainingFutures;
  }

  final Set<Throwable> getOrInitSeenExceptions() {
    /*
     * The initialization of seenExceptions has to be more complicated than we'd like. The simple
     * approach would be for each caller CAS it from null to a Set populated with its exception. But
     * there's another race: If the first thread fails with an exception and a second thread
     * immediately fails with the same exception:
     *
     * Thread1: calls setException(), which returns true, context switch before it can CAS
     * seenExceptions to its exception
     *
     * Thread2: calls setException(), which returns false, CASes seenExceptions to its exception,
     * and wrongly believes that its exception is new (leading it to logging it when it shouldn't)
     *
     * Our solution is for threads to CAS seenExceptions from null to a Set population with _the
     * initial exception_, no matter which thread does the work. This ensures that seenExceptions
     * always contains not just the current thread's exception but also the initial thread's.
     */
    Set<Throwable> seenExceptionsLocal = seenExceptions;
    if (seenExceptionsLocal == null) {
      seenExceptionsLocal = newConcurrentHashSet();
      /*
       * Other handleException() callers may see this as soon as we publish it. We need to populate
       * it with the initial failure before we do, or else they may think that the initial failure
       * has never been seen before.
       */
      addInitialException(seenExceptionsLocal);

      SEEN_EXCEPTIONS_UDPATER.compareAndSet(this, null, seenExceptionsLocal);
      /*
       * If another handleException() caller created the set, we need to use that copy in case yet
       * other callers have added to it.
       *
       * This read is guaranteed to get us the right value because we only set this once (here).
       */
      seenExceptionsLocal = seenExceptions;
    }
    return seenExceptionsLocal;
  }

  /** Populates {@code seen} with the exception that was passed to {@code setException}. */
  abstract void addInitialException(Set<Throwable> seen);

  final int decrementRemainingAndGet() {
    return REMAINING_COUNT_UPDATER.decrementAndGet(this);
  }
}
