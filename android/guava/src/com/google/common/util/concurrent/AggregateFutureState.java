/*
 * Copyright (C) 2015 The Guava Authors
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

import static com.google.common.collect.Sets.newConcurrentHashSet;
import static java.util.Objects.requireNonNull;
import static java.util.concurrent.atomic.AtomicIntegerFieldUpdater.newUpdater;
import static java.util.concurrent.atomic.AtomicReferenceFieldUpdater.newUpdater;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.VisibleForTesting;
import com.google.j2objc.annotations.ReflectionSupport;
import java.util.Set;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import java.util.logging.Level;
import org.jspecify.annotations.Nullable;

/**
 * A helper which does some thread-safe operations for aggregate futures, which must be implemented
 * differently in GWT. Namely:
 *
 * <ul>
 *   <li>Lazily initializes a set of seen exceptions
 *   <li>Decrements a counter atomically
 * </ul>
 */
@GwtCompatible(emulated = true)
@ReflectionSupport(value = ReflectionSupport.Level.FULL)
abstract class AggregateFutureState<OutputT extends @Nullable Object>
    extends AbstractFuture.TrustedFuture<OutputT> {
  /*
   * The following fields are package-private, even though we intend never to use them outside this
   * file. For discussion, see AbstractFutureState.
   */

  // Lazily initialized the first time we see an exception; not released until all the input futures
  // have completed and we have processed them all.
  volatile @Nullable Set<Throwable> seenExceptionsField = null;

  volatile int remainingField;

  private static final AtomicHelper ATOMIC_HELPER;

  private static final LazyLogger log = new LazyLogger(AggregateFutureState.class);

  static {
    AtomicHelper helper;
    Throwable thrownReflectionFailure = null;
    try {
      helper = new SafeAtomicHelper();
    } catch (Throwable reflectionFailure) { // sneaky checked exception
      // Some Android 5.0.x Samsung devices have bugs in JDK reflection APIs that cause
      // getDeclaredField to throw a NoSuchFieldException when the field is definitely there.
      // For these users fallback to a suboptimal implementation, based on synchronized. This will
      // be a definite performance hit to those users.
      thrownReflectionFailure = reflectionFailure;
      helper = new SynchronizedAtomicHelper();
    }
    ATOMIC_HELPER = helper;
    // Log after all static init is finished; if an installed logger uses any Futures methods, it
    // shouldn't break in cases where reflection is missing/broken.
    if (thrownReflectionFailure != null) {
      log.get().log(Level.SEVERE, "SafeAtomicHelper is broken!", thrownReflectionFailure);
    }
  }

  AggregateFutureState(int remainingFutures) {
    this.remainingField = remainingFutures;
  }

  final Set<Throwable> getOrInitSeenExceptions() {
    /*
     * The initialization of seenExceptionsField has to be more complicated than we'd like. The
     * simple approach would be for each caller CAS it from null to a Set populated with its
     * exception. But there's another race: If the first thread fails with an exception and a second
     * thread immediately fails with the same exception:
     *
     * Thread1: calls setException(), which returns true, context switch before it can CAS
     * seenExceptionsField to its exception
     *
     * Thread2: calls setException(), which returns false, CASes seenExceptionsField to its
     * exception, and wrongly believes that its exception is new (leading it to logging it when it
     * shouldn't)
     *
     * Our solution is for threads to CAS seenExceptionsField from null to a Set populated with _the
     * initial exception_, no matter which thread does the work. This ensures that
     * seenExceptionsField always contains not just the current thread's exception but also the
     * initial thread's.
     */
    Set<Throwable> seenExceptionsLocal = seenExceptionsField;
    if (seenExceptionsLocal == null) {
      // TODO(cpovirk): Should we use a simpler (presumably cheaper) data structure?
      /*
       * Using weak references here could let us release exceptions earlier, but:
       *
       * 1. On Android, querying a WeakReference blocks if the GC is doing an otherwise-concurrent
       * pass.
       *
       * 2. We would probably choose to compare exceptions using == instead of equals() (for
       * consistency with how weak references are cleared). That's a behavior change -- arguably the
       * removal of a feature.
       *
       * Fortunately, exceptions rarely contain references to expensive resources.
       */

      //
      seenExceptionsLocal = newConcurrentHashSet();
      /*
       * Other handleException() callers may see this as soon as we publish it. We need to populate
       * it with the initial failure before we do, or else they may think that the initial failure
       * has never been seen before.
       */
      addInitialException(seenExceptionsLocal);

      ATOMIC_HELPER.compareAndSetSeenExceptions(this, null, seenExceptionsLocal);
      /*
       * If another handleException() caller created the set, we need to use that copy in case yet
       * other callers have added to it.
       *
       * This read is guaranteed to get us the right value because we only set this once (here).
       *
       * requireNonNull is safe because either our compareAndSet succeeded or it failed because
       * another thread did it for us.
       */
      seenExceptionsLocal = requireNonNull(seenExceptionsField);
    }
    return seenExceptionsLocal;
  }

  /** Populates {@code seen} with the exception that was passed to {@code setException}. */
  abstract void addInitialException(Set<Throwable> seen);

  final int decrementRemainingAndGet() {
    return ATOMIC_HELPER.decrementAndGetRemainingCount(this);
  }

  final void clearSeenExceptions() {
    seenExceptionsField = null;
  }

  @VisibleForTesting
  static String atomicHelperTypeForTest() {
    return ATOMIC_HELPER.atomicHelperTypeForTest();
  }

  private abstract static class AtomicHelper {
    /** Performs an atomic compare-and-set of {@link AggregateFutureState#seenExceptionsField}. */
    abstract void compareAndSetSeenExceptions(
        AggregateFutureState<?> state, @Nullable Set<Throwable> expect, Set<Throwable> update);

    /** Performs an atomic decrement-and-get of {@link AggregateFutureState#remainingField}. */
    abstract int decrementAndGetRemainingCount(AggregateFutureState<?> state);

    abstract String atomicHelperTypeForTest();
  }

  private static final class SafeAtomicHelper extends AtomicHelper {
    private static final AtomicReferenceFieldUpdater<
            ? super AggregateFutureState<?>, ? super @Nullable Set<Throwable>>
        seenExceptionsUpdater =
            newUpdater(AggregateFutureState.class, Set.class, "seenExceptionsField");

    private static final AtomicIntegerFieldUpdater<? super AggregateFutureState<?>>
        remainingCountUpdater = newUpdater(AggregateFutureState.class, "remainingField");

    @Override
    void compareAndSetSeenExceptions(
        AggregateFutureState<?> state, @Nullable Set<Throwable> expect, Set<Throwable> update) {
      seenExceptionsUpdater.compareAndSet(state, expect, update);
    }

    @Override
    int decrementAndGetRemainingCount(AggregateFutureState<?> state) {
      return remainingCountUpdater.decrementAndGet(state);
    }

    @Override
    String atomicHelperTypeForTest() {
      return "SafeAtomicHelper";
    }
  }

  private static final class SynchronizedAtomicHelper extends AtomicHelper {
    @Override
    void compareAndSetSeenExceptions(
        AggregateFutureState<?> state, @Nullable Set<Throwable> expect, Set<Throwable> update) {
      synchronized (state) {
        if (state.seenExceptionsField == expect) {
          state.seenExceptionsField = update;
        }
      }
    }

    @Override
    int decrementAndGetRemainingCount(AggregateFutureState<?> state) {
      synchronized (state) {
        return --state.remainingField;
      }
    }

    @Override
    String atomicHelperTypeForTest() {
      return "SynchronizedAtomicHelper";
    }
  }
}
