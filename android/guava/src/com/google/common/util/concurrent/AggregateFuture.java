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

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.util.concurrent.AggregateFuture.ReleaseResourcesReason.ALL_INPUT_FUTURES_PROCESSED;
import static com.google.common.util.concurrent.AggregateFuture.ReleaseResourcesReason.OUTPUT_FUTURE_DONE;
import static com.google.common.util.concurrent.Futures.getDone;
import static com.google.common.util.concurrent.MoreExecutors.directExecutor;
import static java.util.Objects.requireNonNull;
import static java.util.logging.Level.SEVERE;

import com.google.common.annotations.GwtCompatible;
import com.google.common.collect.ImmutableCollection;
import com.google.errorprone.annotations.ForOverride;
import com.google.errorprone.annotations.OverridingMethodsMustInvokeSuper;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.logging.Logger;
import javax.annotation.CheckForNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * A future whose value is derived from a collection of input futures.
 *
 * @param <InputT> the type of the individual inputs
 * @param <OutputT> the type of the output (i.e. this) future
 */
@GwtCompatible
@ElementTypesAreNonnullByDefault
abstract class AggregateFuture<InputT extends @Nullable Object, OutputT extends @Nullable Object>
    extends AggregateFutureState<OutputT> {
  private static final Logger logger = Logger.getLogger(AggregateFuture.class.getName());

  /**
   * The input futures. After {@link #init}, this field is read only by {@link #afterDone()} (to
   * propagate cancellation) and {@link #toString()}. To access the futures' <i>values</i>, {@code
   * AggregateFuture} attaches listeners that hold references to one or more inputs. And in the case
   * of {@link CombinedFuture}, the user-supplied callback usually has its own references to inputs.
   */
  /*
   * In certain circumstances, this field might theoretically not be visible to an afterDone() call
   * triggered by cancel(). For details, see the comments on the fields of TimeoutFuture.
   */
  @CheckForNull private ImmutableCollection<? extends ListenableFuture<? extends InputT>> futures;

  private final boolean allMustSucceed;
  private final boolean collectsValues;

  AggregateFuture(
      ImmutableCollection<? extends ListenableFuture<? extends InputT>> futures,
      boolean allMustSucceed,
      boolean collectsValues) {
    super(futures.size());
    this.futures = checkNotNull(futures);
    this.allMustSucceed = allMustSucceed;
    this.collectsValues = collectsValues;
  }

  @Override
  protected final void afterDone() {
    super.afterDone();

    ImmutableCollection<? extends Future<?>> localFutures = futures;
    releaseResources(OUTPUT_FUTURE_DONE); // nulls out `futures`

    if (isCancelled() & localFutures != null) {
      boolean wasInterrupted = wasInterrupted();
      for (Future<?> future : localFutures) {
        future.cancel(wasInterrupted);
      }
    }
    /*
     * We don't call clearSeenExceptions() until processCompleted(). Prior to that, it may be needed
     * again if some outstanding input fails.
     */
  }

  @Override
  @CheckForNull
  protected final String pendingToString() {
    ImmutableCollection<? extends Future<?>> localFutures = futures;
    if (localFutures != null) {
      return "futures=" + localFutures;
    }
    return super.pendingToString();
  }

  /**
   * Must be called at the end of each subclass's constructor. This method performs the "real"
   * initialization; we can't put this in the constructor because, in the case where futures are
   * already complete, we would not initialize the subclass before calling {@link
   * #collectValueFromNonCancelledFuture}. As this is called after the subclass is constructed,
   * we're guaranteed to have properly initialized the subclass.
   */
  final void init() {
    /*
     * requireNonNull is safe because this is called from the constructor after `futures` is set but
     * before releaseResources could be called (because we have not yet set up any of the listeners
     * that could call it, nor exposed this Future for users to call cancel() on).
     */
    requireNonNull(futures);

    // Corner case: List is empty.
    if (futures.isEmpty()) {
      handleAllCompleted();
      return;
    }

    // NOTE: If we ever want to use a custom executor here, have a look at CombinedFuture as we'll
    // need to handle RejectedExecutionException

    if (allMustSucceed) {
      // We need fail fast, so we have to keep track of which future failed so we can propagate
      // the exception immediately

      // Register a listener on each Future in the list to update the state of this future.
      // Note that if all the futures on the list are done prior to completing this loop, the last
      // call to addListener() will callback to setOneValue(), transitively call our cleanup
      // listener, and set this.futures to null.
      // This is not actually a problem, since the foreach only needs this.futures to be non-null
      // at the beginning of the loop.
      int i = 0;
      for (ListenableFuture<? extends InputT> future : futures) {
        int index = i++;
        future.addListener(
            () -> {
              try {
                if (future.isCancelled()) {
                  // Clear futures prior to cancelling children. This sets our own state but lets
                  // the input futures keep running, as some of them may be used elsewhere.
                  futures = null;
                  cancel(false);
                } else {
                  collectValueFromNonCancelledFuture(index, future);
                }
              } finally {
                /*
                 * "null" means: There is no need to access `futures` again during
                 * `processCompleted` because we're reading each value during a call to
                 * handleOneInputDone.
                 */
                decrementCountAndMaybeComplete(null);
              }
            },
            directExecutor());
      }
    } else {
      /*
       * We'll call the user callback or collect the values only when all inputs complete,
       * regardless of whether some failed. This lets us avoid calling expensive methods like
       * Future.get() when we don't need to (specifically, for whenAllComplete().call*()), and it
       * lets all futures share the same listener.
       *
       * We store `localFutures` inside the listener because `this.futures` might be nulled out by
       * the time the listener runs for the final future -- at which point we need to check all
       * inputs for exceptions *if* we're collecting values. If we're not, then the listener doesn't
       * need access to the futures again, so we can just pass `null`.
       *
       * TODO(b/112550045): Allocating a single, cheaper listener is (I think) only an optimization.
       * If we make some other optimizations, this one will no longer be necessary. The optimization
       * could actually hurt in some cases, as it forces us to keep all inputs in memory until the
       * final input completes.
       */
      ImmutableCollection<? extends Future<? extends InputT>> localFutures =
          collectsValues ? futures : null;
      Runnable listener = () -> decrementCountAndMaybeComplete(localFutures);
      for (ListenableFuture<? extends InputT> future : futures) {
        future.addListener(listener, directExecutor());
      }
    }
  }

  /**
   * Fails this future with the given Throwable if {@link #allMustSucceed} is true. Also, logs the
   * throwable if it is an {@link Error} or if {@link #allMustSucceed} is {@code true}, the
   * throwable did not cause this future to fail, and it is the first time we've seen that
   * particular Throwable.
   */
  private void handleException(Throwable throwable) {
    checkNotNull(throwable);

    if (allMustSucceed) {
      // As soon as the first one fails, make that failure the result of the output future.
      // The results of all other inputs are then ignored (except for logging any failures).
      boolean completedWithFailure = setException(throwable);
      if (!completedWithFailure) {
        // Go up the causal chain to see if we've already seen this cause; if we have, even if
        // it's wrapped by a different exception, don't log it.
        boolean firstTimeSeeingThisException = addCausalChain(getOrInitSeenExceptions(), throwable);
        if (firstTimeSeeingThisException) {
          log(throwable);
          return;
        }
      }
    }

    /*
     * TODO(cpovirk): Should whenAllComplete().call*() log errors, too? Currently, it doesn't call
     * handleException() at all.
     */
    if (throwable instanceof Error) {
      /*
       * TODO(cpovirk): Do we really want to log this if we called setException(throwable) and it
       * returned true? This was intentional (CL 46470009), but it seems odd compared to how we
       * normally handle Error.
       *
       * Similarly, do we really want to log the same Error more than once?
       */
      log(throwable);
    }
  }

  private static void log(Throwable throwable) {
    String message =
        (throwable instanceof Error)
            ? "Input Future failed with Error"
            : "Got more than one input Future failure. Logging failures after the first";
    logger.log(SEVERE, message, throwable);
  }

  @Override
  final void addInitialException(Set<Throwable> seen) {
    checkNotNull(seen);
    if (!isCancelled()) {
      /*
       * requireNonNull is safe because:
       *
       * - This is a TrustedFuture, so tryInternalFastPathGetFailure will in fact return the failure
       *   cause if this Future has failed.
       *
       * - And this future *has* failed: This method is called only from handleException (through
       *   getOrInitSeenExceptions). handleException tried to call setException and failed, so
       *   either this Future was cancelled (which we ruled out with the isCancelled check above),
       *   or it had already failed. (It couldn't have completed *successfully* or even had
       *   setFuture called on it: Neither of those can happen until we've finished processing all
       *   the completed inputs. And we're still processing at least one input, the one that
       *   triggered handleException.)
       *
       * TODO(cpovirk): Think about whether we could/should use Verify to check the return value of
       * addCausalChain.
       */
      boolean unused = addCausalChain(seen, requireNonNull(tryInternalFastPathGetFailure()));
    }
  }

  /**
   * Collects the result (success or failure) of one input future. The input must not have been
   * cancelled. For details on when this is called, see {@link #collectOneValue}.
   */
  private void collectValueFromNonCancelledFuture(int index, Future<? extends InputT> future) {
    try {
      // We get the result, even if collectOneValue is a no-op, so that we can fail fast.
      collectOneValue(index, getDone(future));
    } catch (ExecutionException e) {
      handleException(e.getCause());
    } catch (Throwable t) {
      handleException(t);
    }
  }

  private void decrementCountAndMaybeComplete(
      @CheckForNull
          ImmutableCollection<? extends Future<? extends InputT>>
              futuresIfNeedToCollectAtCompletion) {
    int newRemaining = decrementRemainingAndGet();
    checkState(newRemaining >= 0, "Less than 0 remaining futures");
    if (newRemaining == 0) {
      processCompleted(futuresIfNeedToCollectAtCompletion);
    }
  }

  private void processCompleted(
      @CheckForNull
          ImmutableCollection<? extends Future<? extends InputT>>
              futuresIfNeedToCollectAtCompletion) {
    if (futuresIfNeedToCollectAtCompletion != null) {
      int i = 0;
      for (Future<? extends InputT> future : futuresIfNeedToCollectAtCompletion) {
        if (!future.isCancelled()) {
          collectValueFromNonCancelledFuture(i, future);
        }
        i++;
      }
    }
    clearSeenExceptions();
    handleAllCompleted();
    /*
     * Null out fields, including some used in handleAllCompleted() above (like
     * `CollectionFuture.values`). This might be a no-op: If this future completed during
     * handleAllCompleted(), they will already have been nulled out. But in the case of
     * whenAll*().call*(), this future may be pending until the callback runs -- or even longer in
     * the case of callAsync(), which waits for the callback's returned future to complete.
     */
    releaseResources(ALL_INPUT_FUTURES_PROCESSED);
  }

  /**
   * Clears fields that are no longer needed after this future has completed -- or at least all its
   * inputs have completed (more precisely, after {@link #handleAllCompleted()} has been called).
   * Often called multiple times (that is, both when the inputs complete and when the output
   * completes).
   *
   * <p>This is similar to our proposed {@code afterCommit} method but not quite the same. See the
   * description of CL 265462958.
   */
  // TODO(user): Write more tests for memory retention.
  @ForOverride
  @OverridingMethodsMustInvokeSuper
  void releaseResources(ReleaseResourcesReason reason) {
    checkNotNull(reason);
    /*
     * All elements of `futures` are completed, or this future has already completed and read
     * `futures` into a local variable (in preparation for propagating cancellation to them). In
     * either case, no one needs to read `futures` for cancellation purposes later. (And
     * cancellation purposes are the main reason to access `futures`, as discussed in its docs.)
     */
    this.futures = null;
  }

  enum ReleaseResourcesReason {
    OUTPUT_FUTURE_DONE,
    ALL_INPUT_FUTURES_PROCESSED,
  }

  /**
   * If {@code allMustSucceed} is true, called as each future completes; otherwise, if {@code
   * collectsValues} is true, called for each future when all futures complete.
   */
  abstract void collectOneValue(int index, @ParametricNullness InputT returnValue);

  abstract void handleAllCompleted();

  /** Adds the chain to the seen set, and returns whether all the chain was new to us. */
  private static boolean addCausalChain(Set<Throwable> seen, Throwable param) {
    // Declare a "true" local variable so that the Checker Framework will infer nullness.
    Throwable t = param;

    for (; t != null; t = t.getCause()) {
      boolean firstTimeSeen = seen.add(t);
      if (!firstTimeSeen) {
        /*
         * We've seen this, so we've seen its causes, too. No need to re-add them. (There's one case
         * where this isn't true, but we ignore it: If we record an exception, then someone calls
         * initCause() on it, and then we examine it again, we'll conclude that we've seen the whole
         * chain before when it fact we haven't. But this should be rare.)
         */
        return false;
      }
    }
    return true;
  }
}
