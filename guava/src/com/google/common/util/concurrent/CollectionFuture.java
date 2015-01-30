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

import static com.google.common.base.Preconditions.checkState;
import static com.google.common.util.concurrent.Uninterruptibles.getUninterruptibly;
import static java.util.concurrent.atomic.AtomicReferenceFieldUpdater.newUpdater;

import com.google.common.annotations.GwtCompatible;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
* Collection future.
*/
@GwtCompatible(emulated = true)
final class CollectionFuture<V, C> extends AbstractFuture.TrustedFuture<C> {
  private static final Logger logger =
      Logger.getLogger(CollectionFuture.class.getName());

  @SuppressWarnings({"rawtypes", "unchecked"})
  private static final AtomicReferenceFieldUpdater<CollectionFuture<?, ?>, Set<Throwable>>
      SEEN_EXCEPTIONS_UDPATER = newUpdater(
          (Class) CollectionFuture.class, (Class) Set.class, "seenExceptions");

  private ImmutableCollection<? extends ListenableFuture<? extends V>> futures;
  private final boolean allMustSucceed;
  private final AtomicInteger remaining;
  private FutureCollector<V, C> combiner;
  private List<Optional<V>> values;
  private volatile Set<Throwable> seenExceptions;

  CollectionFuture(
      ImmutableCollection<? extends ListenableFuture<? extends V>> futures,
      boolean allMustSucceed, Executor listenerExecutor,
      FutureCollector<V, C> combiner) {
    Preconditions.checkNotNull(futures);
    Preconditions.checkNotNull(listenerExecutor);
    Preconditions.checkNotNull(combiner);
    this.futures = futures;
    this.allMustSucceed = allMustSucceed;
    this.remaining = new AtomicInteger(futures.size());
    this.combiner = combiner;
    this.values = Lists.newArrayListWithCapacity(futures.size());
    this.seenExceptions = null; // Initialized once the first time we see an exception
    init(listenerExecutor);
  }

  @Override void done() {
    // Let go of the memory held by other futures
    this.futures = null;

    // By now the values array has either been set as the Future's value,
    // or (in case of failure) is no longer useful.
    this.values = null;

    // The combiner may also hold state, so free that as well
    this.combiner = null;
  }

  /**
   * Must be called at the end of the constructor.
   */
  protected void init(final Executor listenerExecutor) {
    // Now begin the "real" initialization.

    // Corner case: List is empty.
    if (futures.isEmpty()) {
      set(combiner.combine(ImmutableList.<Optional<V>>of()));
      return;
    }

    // Populate the results list with null initially.
    for (int i = 0; i < futures.size(); ++i) {
      values.add(null);
    }

    // Register a listener on each Future in the list to update
    // the state of this future.
    // Note that if all the futures on the list are done prior to completing
    // this loop, the last call to addListener() will callback to
    // setOneValue(), transitively call our cleanup listener, and set
    // this.futures to null.
    // This is not actually a problem, since the foreach only needs
    // this.futures to be non-null at the beginning of the loop.
    int i = 0;
    for (final ListenableFuture<? extends V> listenable : futures) {
      final int index = i++;
      listenable.addListener(new Runnable() {
        @Override
        public void run() {
          setOneValue(index, listenable);
        }
      }, listenerExecutor);
    }
  }

  @Override
  public boolean cancel(boolean mayInterruptIfRunning) {
    // Cancel all the component futures.
    ImmutableCollection<? extends ListenableFuture<?>> futuresToCancel = futures;
    boolean cancelled = super.cancel(mayInterruptIfRunning);
    if (cancelled && futuresToCancel != null) {
      for (ListenableFuture<?> future : futuresToCancel) {
        future.cancel(mayInterruptIfRunning);
      }
    }
    return cancelled;
  }

  /**
   * Fails this future with the given Throwable if {@link #allMustSucceed} is
   * true. Also, logs the throwable if it is an {@link Error} or if
   * {@link #allMustSucceed} is {@code true}, the throwable did not cause
   * this future to fail, and it is the first time we've seen that particular Throwable.
   */
  private void setExceptionAndMaybeLog(Throwable throwable) {
    boolean visibleFromOutputFuture = false;
    boolean firstTimeSeeingThisException = true;
    if (allMustSucceed) {
      // As soon as the first one fails, throw the exception up.
      // The result of all other inputs is then ignored.
      visibleFromOutputFuture = super.setException(throwable);

      // seenExceptions is only set once; but we don't allocate it until we get a failure
      Set<Throwable> seenExceptionsLocal = seenExceptions;
      if (seenExceptionsLocal == null) {
        SEEN_EXCEPTIONS_UDPATER.compareAndSet(this, null, Sets.<Throwable>newConcurrentHashSet());
        seenExceptionsLocal = seenExceptions;
      }

      // Go up the causal chain to see if we've already seen this cause; if we have,
      // even if it's wrapped by a different exception, don't log it.
      Throwable currentThrowable = throwable;
      while (currentThrowable != null) {
        firstTimeSeeingThisException = seenExceptionsLocal.add(currentThrowable);
        if (!firstTimeSeeingThisException) {
          break;
        }
        currentThrowable = currentThrowable.getCause();
      }
    }

    if (throwable instanceof Error
        || (allMustSucceed && !visibleFromOutputFuture && firstTimeSeeingThisException)) {
      logger.log(Level.SEVERE, "input future failed.", throwable);
    }
  }

  /**
   * Sets the value at the given index to that of the given future.
   */
  private void setOneValue(int index, Future<? extends V> future) {
    List<Optional<V>> localValues = values;
    if (isDone() || localValues == null) {
      // Some other future failed or has been cancelled, causing this one to
      // also be cancelled or have an exception set. This should only happen
      // if allMustSucceed is true or if the output itself has been
      // cancelled.
      checkState(allMustSucceed || isCancelled(),
          "Future was done before all dependencies completed");
    }

    try {
      checkState(future.isDone(),
          "Tried to set value from future which is not done");
      if (future.isCancelled()) {
        if (allMustSucceed) {
          // this.cancel propagates the cancellation to children; we use super.cancel
          // to set our own state but let the input futures keep running
          // as some of them may be used elsewhere.
          super.cancel(false);
        }
      } else {
        V returnValue = getUninterruptibly(future);
        if (localValues != null) {
          localValues.set(index, Optional.fromNullable(returnValue));
        }
      }
    } catch (ExecutionException e) {
      setExceptionAndMaybeLog(e.getCause());
    } catch (Throwable t) {
      setExceptionAndMaybeLog(t);
    } finally {
      int newRemaining = remaining.decrementAndGet();
      checkState(newRemaining >= 0, "Less than 0 remaining futures");
      if (newRemaining == 0) {
        FutureCollector<V, C> localCombiner = combiner;
        if (localCombiner != null && localValues != null) {
          set(localCombiner.combine(localValues));
        } else {
          checkState(isDone());
        }

        seenExceptions = null; // Done with tracking seen exceptions
      }
    }
  }

  interface FutureCollector<V, C> {
    C combine(List<Optional<V>> values);
  }
}
