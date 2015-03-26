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

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.util.concurrent.MoreExecutors.directExecutor;
import static com.google.common.util.concurrent.Platform.isInstanceOfThrowableClass;
import static com.google.common.util.concurrent.Uninterruptibles.getUninterruptibly;

import com.google.common.annotations.Beta;
import com.google.common.annotations.GwtCompatible;
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import java.lang.reflect.UndeclaredThrowableException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nullable;

/**
 * Static utility methods pertaining to the {@link Future} interface.
 *
 * <p>Many of these methods use the {@link ListenableFuture} API; consult the
 * Guava User Guide article on <a href=
 * "http://code.google.com/p/guava-libraries/wiki/ListenableFutureExplained">
 * {@code ListenableFuture}</a>.
 *
 * @author Kevin Bourrillion
 * @author Nishant Thakkar
 * @author Sven Mawson
 * @since 1.0
 */
@Beta
@GwtCompatible(emulated = true)
public final class Futures extends GwtFuturesCatchingSpecialization {

  // A note on memory visibility.
  // Many of the utilities in this class (transform, withFallback, withTimeout, asList, combine)
  // have two requirements that significantly complicate their design.
  // 1. Cancellation should propagate from the returned future to the input future(s).
  // 2. The returned futures shouldn't unnecessarily 'pin' their inputs after completion.
  //
  // A consequence of these these requirements is that the delegate futures cannot be stored in
  // final fields.
  //
  // For simplicity the rest of this description will discuss Futures.withFallback since it is the
  // simplest instance, though very similar descriptions apply to many other classes in this file.
  //
  // In the constructor of FutureFallback, the delegate future is assigned to a field 'running'.
  // That field is non-final and non-volatile.  There are 2 places where the 'running' field is read
  // and where we will have to consider visibility of the write operation in the constructor.
  //
  // 1. In the listener that performs the callback.  In this case it is fine since running is
  //    assigned prior to calling addListener, and addListener happens-before any invocation of the
  //    listener. Notably, this means that 'volatile' is unnecessary to make 'running' visible to
  //    the listener.
  //
  // 2. In cancel() where we propagate cancellation to the input.  In this case it is _not_ fine.
  //    There is currently nothing that enforces that the write to running in the constructor is
  //    visible to cancel().  This is because there is no happens before edge between the write and
  //    a (hypothetical) unsafe read by our caller. Note: adding 'volatile' does not fix this issue,
  //    it would just add an edge such that if cancel() observed non-null, then it would also
  //    definitely observe all earlier writes, but we still have no guarantee that cancel() would
  //    see the inital write (just stronger guarantees if it does).
  //
  // See: http://cs.oswego.edu/pipermail/concurrency-interest/2015-January/013800.html
  // For a (long) discussion about this specific issue and the general futility of life.
  //
  // For the time being we are OK with the problem discussed above since it requires a caller to
  // introduce a very specific kind of data-race.  And given the other operations performed by these
  // methods that involve volatile read/write operations, in practice there is no issue.  Also, the
  // way in such a visibility issue would surface is most likely as a failure of cancel() to
  // propagate to the input.  Cancellation propagation is fundamentally racy so this is fine.
  //
  // Future versions of the JMM may revise safe construction semantics in such a way that we can
  // safely publish these objects and we won't need this whole discussion.
  // TODO(user,lukes): consider adding volatile to all these fields since in current known JVMs
  // that should resolve the issue.  This comes at the cost of adding more write barriers to the
  // implementations.

  private Futures() {}

  private abstract static class ImmediateFuture<V>
      implements ListenableFuture<V> {

    private static final Logger log =
        Logger.getLogger(ImmediateFuture.class.getName());

    @Override
    public void addListener(Runnable listener, Executor executor) {
      checkNotNull(listener, "Runnable was null.");
      checkNotNull(executor, "Executor was null.");
      try {
        executor.execute(listener);
      } catch (RuntimeException e) {
        // ListenableFuture's contract is that it will not throw unchecked
        // exceptions, so log the bad runnable and/or executor and swallow it.
        log.log(Level.SEVERE, "RuntimeException while executing runnable "
            + listener + " with executor " + executor, e);
      }
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
      return false;
    }

    @Override
    public abstract V get() throws ExecutionException;

    @Override
    public V get(long timeout, TimeUnit unit) throws ExecutionException {
      checkNotNull(unit);
      return get();
    }

    @Override
    public boolean isCancelled() {
      return false;
    }

    @Override
    public boolean isDone() {
      return true;
    }
  }

  private static class ImmediateSuccessfulFuture<V> extends ImmediateFuture<V> {

    @Nullable private final V value;

    ImmediateSuccessfulFuture(@Nullable V value) {
      this.value = value;
    }

    @Override
    public V get() {
      return value;
    }
  }

  private static class ImmediateFailedFuture<V> extends ImmediateFuture<V> {

    private final Throwable thrown;

    ImmediateFailedFuture(Throwable thrown) {
      this.thrown = thrown;
    }

    @Override
    public V get() throws ExecutionException {
      throw new ExecutionException(thrown);
    }
  }

  /**
   * Creates a {@code ListenableFuture} which has its value set immediately upon
   * construction. The getters just return the value. This {@code Future} can't
   * be canceled or timed out and its {@code isDone()} method always returns
   * {@code true}.
   */
  @CheckReturnValue
  public static <V> ListenableFuture<V> immediateFuture(@Nullable V value) {
    return new ImmediateSuccessfulFuture<V>(value);
  }

  /**
   * Returns a {@code ListenableFuture} which has an exception set immediately
   * upon construction.
   *
   * <p>The returned {@code Future} can't be cancelled, and its {@code isDone()}
   * method always returns {@code true}. Calling {@code get()} will immediately
   * throw the provided {@code Throwable} wrapped in an {@code
   * ExecutionException}.
   */
  @CheckReturnValue
  public static <V> ListenableFuture<V> immediateFailedFuture(
      Throwable throwable) {
    checkNotNull(throwable);
    return new ImmediateFailedFuture<V>(throwable);
  }

  /**
   * Returns a {@code Future} whose result is taken from the given primary
   * {@code input} or, if the primary input fails, from the {@code Future}
   * provided by the {@code fallback}. {@link FutureFallback#create} is not
   * invoked until the primary input has failed, so if the primary input
   * succeeds, it is never invoked. If, during the invocation of {@code
   * fallback}, an exception is thrown, this exception is used as the result of
   * the output {@code Future}.
   *
   * <p>Below is an example of a fallback that returns a default value if an
   * exception occurs:
   *
   * <pre>   {@code
   *   ListenableFuture<Integer> fetchCounterFuture = ...;
   *
   *   // Falling back to a zero counter in case an exception happens when
   *   // processing the RPC to fetch counters.
   *   ListenableFuture<Integer> faultTolerantFuture = Futures.withFallback(
   *       fetchCounterFuture, new FutureFallback<Integer>() {
   *         public ListenableFuture<Integer> create(Throwable t) {
   *           // Returning "0" as the default for the counter when the
   *           // exception happens.
   *           return immediateFuture(0);
   *         }
   *       });}</pre>
   *
   * <p>The fallback can also choose to propagate the original exception when
   * desired:
   *
   * <pre>   {@code
   *   ListenableFuture<Integer> fetchCounterFuture = ...;
   *
   *   // Falling back to a zero counter only in case the exception was a
   *   // TimeoutException.
   *   ListenableFuture<Integer> faultTolerantFuture = Futures.withFallback(
   *       fetchCounterFuture, new FutureFallback<Integer>() {
   *         public ListenableFuture<Integer> create(Throwable t) {
   *           if (t instanceof TimeoutException) {
   *             return immediateFuture(0);
   *           }
   *           return immediateFailedFuture(t);
   *         }
   *       });}</pre>
   *
   * <p>Note: If the derived {@code Future} is slow or heavyweight to create
   * (whether the {@code Future} itself is slow or heavyweight to complete is
   * irrelevant), consider {@linkplain #withFallback(ListenableFuture,
   * FutureFallback, Executor) supplying an executor}. If you do not supply an
   * executor, {@code withFallback} will use a
   * {@linkplain MoreExecutors#directExecutor direct executor}, which carries
   * some caveats for heavier operations. For example, the call to {@code
   * fallback.create} may run on an unpredictable or undesirable thread:
   *
   * <ul>
   * <li>If the input {@code Future} is done at the time {@code withFallback}
   * is called, {@code withFallback} will call {@code fallback.create} inline.
   * <li>If the input {@code Future} is not yet done, {@code withFallback} will
   * schedule {@code fallback.create} to be run by the thread that completes
   * the input {@code Future}, which may be an internal system thread such as
   * an RPC network thread.
   * </ul>
   *
   * <p>Also note that, regardless of which thread executes {@code
   * fallback.create}, all other registered but unexecuted listeners are
   * prevented from running during its execution, even if those listeners are
   * to run in other executors.
   *
   * @param input the primary input {@code Future}
   * @param fallback the {@link FutureFallback} implementation to be called if
   *     {@code input} fails
   * @since 14.0
   * @deprecated Use {@link #catchingAsync(ListenableFuture, Class,
   *     AsyncFunction) catchingAsync(input, Throwable.class,
   *     fallbackImplementedAsAnAsyncFunction)}, usually replacing {@code
   *     Throwable.class} with the specific type you want to handle. This method
   *     will be removed in Guava release 20.0.
   */
  @Deprecated
  @CheckReturnValue
  public static <V> ListenableFuture<V> withFallback(
      ListenableFuture<? extends V> input,
      FutureFallback<? extends V> fallback) {
    return withFallback(input, fallback, directExecutor());
  }

  /**
   * Returns a {@code Future} whose result is taken from the given primary
   * {@code input} or, if the primary input fails, from the {@code Future}
   * provided by the {@code fallback}. {@link FutureFallback#create} is not
   * invoked until the primary input has failed, so if the primary input
   * succeeds, it is never invoked. If, during the invocation of {@code
   * fallback}, an exception is thrown, this exception is used as the result of
   * the output {@code Future}.
   *
   * <p>Below is an example of a fallback that returns a default value if an
   * exception occurs:
   *
   * <pre>   {@code
   *   ListenableFuture<Integer> fetchCounterFuture = ...;
   *
   *   // Falling back to a zero counter in case an exception happens when
   *   // processing the RPC to fetch counters.
   *   ListenableFuture<Integer> faultTolerantFuture = Futures.withFallback(
   *       fetchCounterFuture, new FutureFallback<Integer>() {
   *         public ListenableFuture<Integer> create(Throwable t) {
   *           // Returning "0" as the default for the counter when the
   *           // exception happens.
   *           return immediateFuture(0);
   *         }
   *       }, directExecutor());}</pre>
   *
   * <p>The fallback can also choose to propagate the original exception when
   * desired:
   *
   * <pre>   {@code
   *   ListenableFuture<Integer> fetchCounterFuture = ...;
   *
   *   // Falling back to a zero counter only in case the exception was a
   *   // TimeoutException.
   *   ListenableFuture<Integer> faultTolerantFuture = Futures.withFallback(
   *       fetchCounterFuture, new FutureFallback<Integer>() {
   *         public ListenableFuture<Integer> create(Throwable t) {
   *           if (t instanceof TimeoutException) {
   *             return immediateFuture(0);
   *           }
   *           return immediateFailedFuture(t);
   *         }
   *       }, directExecutor());}</pre>
   *
   * <p>When the execution of {@code fallback.create} is fast and lightweight
   * (though the {@code Future} it returns need not meet these criteria),
   * consider {@linkplain #withFallback(ListenableFuture, FutureFallback)
   * omitting the executor} or explicitly specifying {@code
   * directExecutor}. However, be aware of the caveats documented in the
   * link above.
   *
   * @param input the primary input {@code Future}
   * @param fallback the {@link FutureFallback} implementation to be called if
   *     {@code input} fails
   * @param executor the executor that runs {@code fallback} if {@code input}
   *     fails
   * @since 14.0
   * @deprecated Use {@link #catchingAsync(ListenableFuture, Class,
   *     AsyncFunction, Executor) catchingAsync(input, Throwable.class,
   *     fallbackImplementedAsAnAsyncFunction, executor)}, usually replacing
   *     {@code Throwable.class} with the specific type you want to handle. This method
   *     will be removed in Guava release 20.0.
   */
  @Deprecated
  @CheckReturnValue
  public static <V> ListenableFuture<V> withFallback(
      ListenableFuture<? extends V> input,
      FutureFallback<? extends V> fallback, Executor executor) {
    return catchingAsync(
        input, Throwable.class, asAsyncFunction(fallback), executor);
  }

  @Deprecated
  static <V> AsyncFunction<Throwable, V> asAsyncFunction(final FutureFallback<V> fallback) {
    checkNotNull(fallback);
    return new AsyncFunction<Throwable, V>() {
      @Override
      public ListenableFuture<V> apply(Throwable t) throws Exception {
        return checkNotNull(fallback.create(t), "FutureFallback.create returned null instead of a "
            + "Future. Did you mean to return immediateFuture(null)?");
      }
    };
  }

  static class CatchingFuture<V, X extends Throwable> extends AbstractFuture.TrustedFuture<V> {
    ListenableFuture<? extends V> running;

    CatchingFuture(ListenableFuture<? extends V> input,
        final Class<X> exceptionType,
        final AsyncFunction<? super X, ? extends V> fallback,
        final Executor executor) {
      checkNotNull(exceptionType);
      checkNotNull(fallback);

      running = input;
      input.addListener(new Runnable() {
        @Override public void run() {
          ListenableFuture<? extends V> localRunning = running;
          running = null;
          if (localRunning == null | isCancelled()) {
            return;
          }
          Throwable throwable;
          try {
            set(getUninterruptibly(localRunning));
            return;
          } catch (ExecutionException e) {
            throwable = e.getCause();
          } catch (Throwable e) {  // this includes cancellation exception
            throwable = e;
          }
          try {
            if (isInstanceOfThrowableClass(throwable, exceptionType)) {
              @SuppressWarnings("unchecked") // verified safe by isInstance
              X castThrowable = (X) throwable;
              ListenableFuture<? extends V> replacement = fallback.apply(castThrowable);
              checkNotNull(replacement, "AsyncFunction.apply returned null instead of a Future. "
                  + "Did you mean to return immediateFuture(null)?");
              setFuture(replacement);
            } else {
              setException(throwable);
            }
          } catch (Throwable e) {
            setException(e);
          }
        }
      }, executor);
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
      ListenableFuture<?> current = this.running;
      if (super.cancel(mayInterruptIfRunning)) {
        // May be null if the original future completed, but we were cancelled while the fallback
        // is still pending.  This is fine because if the original future completed, then there is
        // nothing to cancel and if the fallback is pending, cancellation would be handled by
        // super.cancel().
        if (current != null) {
          current.cancel(mayInterruptIfRunning);
        }
        return true;
      }
      return false;
    }
  }

  /**
   * Future that delegates to another but will finish early (via a {@link
   * TimeoutException} wrapped in an {@link ExecutionException}) if the
   * specified duration expires.
   * The delegate future is interrupted and cancelled if it times out.
   */
  private static final class TimeoutFuture<V> extends AbstractFuture.TrustedFuture<V> {
    // Memory visibility of these fields.
    // There are two cases to consider.
    // 1. visibility of the writes to these fields to Fire.run
    //    The initial write to delegateRef is made definitely visible via the semantics of
    //    addListener/SES.schedule.  The later racy write in cancel() is not guaranteed to be
    //    observed, however that is fine since the correctness is based on the atomic state in
    //    our base class.
    //    The initial write to timer is never definitely visible to Fire.run since it is assigned
    //    after SES.schedule is called. Therefore Fire.run has to check for null.  However, it
    //    should be visible if Fire.run is called by delegate.addListener since addListener is
    //    called after the assignment to timer, and importantly this is the main situation in which
    //    we need to be able to see the write.
    // 2. visibility of the writes to cancel
    //    Since these fields are non-final that means that TimeoutFuture is not being 'safely
    //    published', thus a motivated caller may be able to expose the reference to another thread
    //    that would then call cancel() and be unable to cancel the delegate.
    //    There are a number of ways to solve this, none of which are very pretty, and it is
    //    currently believed to be a purely theoretical problem (since the other actions should
    //    supply sufficient write-barriers).

    ListenableFuture<V> delegateRef;
    Future<?> timer;

    TimeoutFuture(ListenableFuture<V> delegate) {
      this.delegateRef = Preconditions.checkNotNull(delegate);
    }

    /** A runnable that is called when the delegate or the timer completes. */
    private static final class Fire<V> implements Runnable {
      // Holding a strong reference to the enclosing class (as we would do if
      // this weren't a static nested class) could cause retention of the
      // delegate's return value (in AbstractFuture) for the duration of the
      // timeout in the case of successful completion. We clear this on run.
      TimeoutFuture<V> timeoutFutureRef;

      Fire(TimeoutFuture<V> timeoutFuture) {
        this.timeoutFutureRef = timeoutFuture;
      }

      @Override public void run() {
        // If either of these reads return null then we must be after a successful cancel
        // or another call to this method.
        TimeoutFuture<V> timeoutFuture = timeoutFutureRef;
        if (timeoutFuture == null) {
          return;
        }
        ListenableFuture<V> delegate = timeoutFuture.delegateRef;
        if (delegate == null) {
          return;
        }

        // Unpin all the memory before attempting to complete.  Not only does this save us from
        // wrapping everything in a finally block, it also ensures that if delegate.cancel() (in the
        // else block), causes delegate to complete, then it won't reentrantly call back in and
        // cause TimeoutFuture to finish with cancellation.
        timeoutFutureRef = null;
        Future<?> timer = timeoutFuture.timer;
        timeoutFuture.delegateRef = null;
        timeoutFuture.timer = null;
        if (delegate.isDone()) {
          timeoutFuture.setFuture(delegate);
          // Try to cancel the timer as an optimization
          // timer may be null if this call to run was by the timer task since there is no
          // happens-before edge between the assignment to timer and an execution of the timer
          // task.
          if (timer != null) {
            timer.cancel(false);
          }
        } else {
          // Some users, for better or worse, rely on the delegate definitely being cancelled prior
          // to the timeout future completing.  We wrap in a try...finally... for the off chance
          // that cancelling the delegate causes an Error to be thrown from a listener on the
          // delegate.
          try {
            delegate.cancel(true);
          } finally {
            // TODO(lukes): this stack trace is particularly useless (all it does is point at the
            // scheduledexecutorservice thread), consider eliminating it altogether?
            timeoutFuture.setException(new TimeoutException("Future timed out: " + delegate));
          }
        }
      }
    }

    @Override public boolean cancel(boolean mayInterruptIfRunning) {
      Future<?> localTimer = timer;
      ListenableFuture<V> delegate = delegateRef;
      if (super.cancel(mayInterruptIfRunning)) {
        // Either can be null if super.cancel() races with an execution of Fire.run, but it doesn't
        // matter because either 1. the delegate is already done (so there is no point in
        // propagating cancellation and Fire.run will cancel the timer. or 2. the timeout occurred
        // and Fire.run will cancel the delegate
        // Technically this is also possible in the 'unsafe publishing' case described above.
        if (delegate != null) {
          // Unpin and prevent Fire from doing anything if delegate.cancel finishes the delegate.
          delegateRef = null;
          delegate.cancel(mayInterruptIfRunning);
        }
        if (localTimer != null) {
          timer = null;
          localTimer.cancel(false);
        }
        return true;
      }
      return false;
    }
  }

  /**
   * Returns a new {@code ListenableFuture} whose result is asynchronously
   * derived from the result of the given {@code Future}. More precisely, the
   * returned {@code Future} takes its result from a {@code Future} produced by
   * applying the given {@code AsyncFunction} to the result of the original
   * {@code Future}. Example:
   *
   * <pre>   {@code
   *   ListenableFuture<RowKey> rowKeyFuture = indexService.lookUp(query);
   *   AsyncFunction<RowKey, QueryResult> queryFunction =
   *       new AsyncFunction<RowKey, QueryResult>() {
   *         public ListenableFuture<QueryResult> apply(RowKey rowKey) {
   *           return dataService.read(rowKey);
   *         }
   *       };
   *   ListenableFuture<QueryResult> queryFuture =
   *       transform(rowKeyFuture, queryFunction);}</pre>
   *
   * <p>Note: If the derived {@code Future} is slow or heavyweight to create
   * (whether the {@code Future} itself is slow or heavyweight to complete is
   * irrelevant), consider {@linkplain #transform(ListenableFuture,
   * AsyncFunction, Executor) supplying an executor}. If you do not supply an
   * executor, {@code transform} will use a
   * {@linkplain MoreExecutors#directExecutor direct executor}, which carries
   * some caveats for heavier operations. For example, the call to {@code
   * function.apply} may run on an unpredictable or undesirable thread:
   *
   * <ul>
   * <li>If the input {@code Future} is done at the time {@code transform} is
   * called, {@code transform} will call {@code function.apply} inline.
   * <li>If the input {@code Future} is not yet done, {@code transform} will
   * schedule {@code function.apply} to be run by the thread that completes the
   * input {@code Future}, which may be an internal system thread such as an
   * RPC network thread.
   * </ul>
   *
   * <p>Also note that, regardless of which thread executes {@code
   * function.apply}, all other registered but unexecuted listeners are
   * prevented from running during its execution, even if those listeners are
   * to run in other executors.
   *
   * <p>The returned {@code Future} attempts to keep its cancellation state in
   * sync with that of the input future and that of the future returned by the
   * function. That is, if the returned {@code Future} is cancelled, it will
   * attempt to cancel the other two, and if either of the other two is
   * cancelled, the returned {@code Future} will receive a callback in which it
   * will attempt to cancel itself.
   *
   * @param input The future to transform
   * @param function A function to transform the result of the input future
   *     to the result of the output future
   * @return A future that holds result of the function (if the input succeeded)
   *     or the original input's failure (if not)
   * @since 11.0
   * @deprecated These {@code AsyncFunction} overloads of {@code transform} are
   *     being renamed to {@code transformAsync}. (The {@code Function}
   *     overloads are keeping the "transform" name.) This method will be removed in Guava release
   *     20.0.
   */
  @Deprecated
  public static <I, O> ListenableFuture<O> transform(ListenableFuture<I> input,
      AsyncFunction<? super I, ? extends O> function) {
    return transformAsync(input, function);
  }

  /**
   * Returns a new {@code ListenableFuture} whose result is asynchronously
   * derived from the result of the given {@code Future}. More precisely, the
   * returned {@code Future} takes its result from a {@code Future} produced by
   * applying the given {@code AsyncFunction} to the result of the original
   * {@code Future}. Example:
   *
   * <pre>   {@code
   *   ListenableFuture<RowKey> rowKeyFuture = indexService.lookUp(query);
   *   AsyncFunction<RowKey, QueryResult> queryFunction =
   *       new AsyncFunction<RowKey, QueryResult>() {
   *         public ListenableFuture<QueryResult> apply(RowKey rowKey) {
   *           return dataService.read(rowKey);
   *         }
   *       };
   *   ListenableFuture<QueryResult> queryFuture =
   *       transform(rowKeyFuture, queryFunction, executor);}</pre>
   *
   * <p>The returned {@code Future} attempts to keep its cancellation state in
   * sync with that of the input future and that of the future returned by the
   * chain function. That is, if the returned {@code Future} is cancelled, it
   * will attempt to cancel the other two, and if either of the other two is
   * cancelled, the returned {@code Future} will receive a callback in which it
   * will attempt to cancel itself.
   *
   * <p>When the execution of {@code function.apply} is fast and lightweight
   * (though the {@code Future} it returns need not meet these criteria),
   * consider {@linkplain #transform(ListenableFuture, AsyncFunction) omitting
   * the executor} or explicitly specifying {@code directExecutor}.
   * However, be aware of the caveats documented in the link above.
   *
   * @param input The future to transform
   * @param function A function to transform the result of the input future
   *     to the result of the output future
   * @param executor Executor to run the function in.
   * @return A future that holds result of the function (if the input succeeded)
   *     or the original input's failure (if not)
   * @since 11.0
   * @deprecated These {@code AsyncFunction} overloads of {@code transform} are
   *     being renamed to {@code transformAsync}. (The {@code Function}
   *     overloads are keeping the "transform" name.) This method will be removed in Guava release
   *     20.0.
   */
  @Deprecated
  public static <I, O> ListenableFuture<O> transform(ListenableFuture<I> input,
      AsyncFunction<? super I, ? extends O> function,
      Executor executor) {
    return transformAsync(input, function, executor);
  }

  /**
   * Returns a new {@code ListenableFuture} whose result is asynchronously derived from the result
   * of the given {@code Future}. More precisely, the returned {@code Future} takes its result from
   * a {@code Future} produced by applying the given {@code AsyncFunction} to the result of the
   * original {@code Future}. Example:
   *
   * <pre>   {@code
   *   ListenableFuture<RowKey> rowKeyFuture = indexService.lookUp(query);
   *   AsyncFunction<RowKey, QueryResult> queryFunction =
   *       new AsyncFunction<RowKey, QueryResult>() {
   *         public ListenableFuture<QueryResult> apply(RowKey rowKey) {
   *           return dataService.read(rowKey);
   *         }
   *       };
   *   ListenableFuture<QueryResult> queryFuture =
   *       transformAsync(rowKeyFuture, queryFunction);}</pre>
   *
   * <p>Note: If the derived {@code Future} is slow or heavyweight to create (whether the {@code
   * Future} itself is slow or heavyweight to complete is irrelevant), consider {@linkplain
   * #transformAsync(ListenableFuture, AsyncFunction, Executor) supplying an executor}. If you do
   * not supply an executor, {@code transformAsync} will use a {@linkplain
   * MoreExecutors#directExecutor direct executor}, which carries some caveats for heavier
   * operations. For example, the call to {@code function.apply} may run on an unpredictable or
   * undesirable thread:
   *
   * <ul>
   * <li>If the input {@code Future} is done at the time {@code transformAsync} is called, {@code
   * transformAsync} will call {@code function.apply} inline.
   * <li>If the input {@code Future} is not yet done, {@code transformAsync} will schedule {@code
   * function.apply} to be run by the thread that completes the input {@code Future}, which may be
   * an internal system thread such as an RPC network thread.
   * </ul>
   *
   * <p>Also note that, regardless of which thread executes {@code function.apply}, all other
   * registered but unexecuted listeners are prevented from running during its execution, even if
   * those listeners are to run in other executors.
   *
   * <p>The returned {@code Future} attempts to keep its cancellation state in sync with that of the
   * input future and that of the future returned by the function. That is, if the returned {@code
   * Future} is cancelled, it will attempt to cancel the other two, and if either of the other two
   * is cancelled, the returned {@code Future} will receive a callback in which it will attempt to
   * cancel itself.
   *
   * @param input The future to transform
   * @param function A function to transform the result of the input future to the result of the
   *     output future
   * @return A future that holds result of the function (if the input succeeded) or the original
   *     input's failure (if not)
   * @since 19.0 (in 11.0 as {@code transform})
   */
  public static <I, O> ListenableFuture<O> transformAsync(
      ListenableFuture<I> input, AsyncFunction<? super I, ? extends O> function) {
    ChainingListenableFuture<I, O> output = new ChainingListenableFuture<I, O>(function, input);
    input.addListener(output, directExecutor());
    return output;
  }

  /**
   * Returns a new {@code ListenableFuture} whose result is asynchronously derived from the result
   * of the given {@code Future}. More precisely, the returned {@code Future} takes its result from
   * a {@code Future} produced by applying the given {@code AsyncFunction} to the result of the
   * original {@code Future}. Example:
   *
   * <pre>   {@code
   *   ListenableFuture<RowKey> rowKeyFuture = indexService.lookUp(query);
   *   AsyncFunction<RowKey, QueryResult> queryFunction =
   *       new AsyncFunction<RowKey, QueryResult>() {
   *         public ListenableFuture<QueryResult> apply(RowKey rowKey) {
   *           return dataService.read(rowKey);
   *         }
   *       };
   *   ListenableFuture<QueryResult> queryFuture =
   *       transformAsync(rowKeyFuture, queryFunction, executor);}</pre>
   *
   * <p>The returned {@code Future} attempts to keep its cancellation state in sync with that of the
   * input future and that of the future returned by the chain function. That is, if the returned
   * {@code Future} is cancelled, it will attempt to cancel the other two, and if either of the
   * other two is cancelled, the returned {@code Future} will receive a callback in which it will
   * attempt to cancel itself.
   *
   * <p>When the execution of {@code function.apply} is fast and lightweight (though the {@code
   * Future} it returns need not meet these criteria), consider {@linkplain
   * #transformAsync(ListenableFuture, AsyncFunction) omitting the executor} or explicitly
   * specifying {@code directExecutor}. However, be aware of the caveats documented in the link
   * above.
   *
   * @param input The future to transform
   * @param function A function to transform the result of the input future to the result of the
   *     output future
   * @param executor Executor to run the function in.
   * @return A future that holds result of the function (if the input succeeded) or the original
   *     input's failure (if not)
   * @since 19.0 (in 11.0 as {@code transform})
   */
  public static <I, O> ListenableFuture<O> transformAsync(ListenableFuture<I> input,
      AsyncFunction<? super I, ? extends O> function, Executor executor) {
    checkNotNull(executor);
    ChainingListenableFuture<I, O> output = new ChainingListenableFuture<I, O>(function, input);
    input.addListener(rejectionPropagatingRunnable(output, output, executor), directExecutor());
    return output;
  }

  /**
   * Returns a Runnable that will invoke the delegate Runnable on the delegate executor, but if the
   * task is rejected, it will propagate that rejection to the output future.
   */
  private static Runnable rejectionPropagatingRunnable(
      final AbstractFuture<?> outputFuture,
      final Runnable delegateTask,
      final Executor delegateExecutor) {
    return new Runnable() {
      @Override public void run() {
        final AtomicBoolean thrownFromDelegate = new AtomicBoolean(true);
        try {
          delegateExecutor.execute(new Runnable() {
            @Override public void run() {
              thrownFromDelegate.set(false);
              delegateTask.run();
            }
          });
        } catch (RejectedExecutionException e) {
          if (thrownFromDelegate.get()) {
            // wrap exception?
            outputFuture.setException(e);
          }
          // otherwise it must have been thrown from a transitive call and the delegate runnable
          // should have handled it.
        }
      }
    };
  }

  /**
   * Returns a new {@code ListenableFuture} whose result is the product of
   * applying the given {@code Function} to the result of the given {@code
   * Future}. Example:
   *
   * <pre>   {@code
   *   ListenableFuture<QueryResult> queryFuture = ...;
   *   Function<QueryResult, List<Row>> rowsFunction =
   *       new Function<QueryResult, List<Row>>() {
   *         public List<Row> apply(QueryResult queryResult) {
   *           return queryResult.getRows();
   *         }
   *       };
   *   ListenableFuture<List<Row>> rowsFuture =
   *       transform(queryFuture, rowsFunction);}</pre>
   *
   * <p>Note: If the transformation is slow or heavyweight, consider {@linkplain
   * #transform(ListenableFuture, Function, Executor) supplying an executor}.
   * If you do not supply an executor, {@code transform} will use an inline
   * executor, which carries some caveats for heavier operations.  For example,
   * the call to {@code function.apply} may run on an unpredictable or
   * undesirable thread:
   *
   * <ul>
   * <li>If the input {@code Future} is done at the time {@code transform} is
   * called, {@code transform} will call {@code function.apply} inline.
   * <li>If the input {@code Future} is not yet done, {@code transform} will
   * schedule {@code function.apply} to be run by the thread that completes the
   * input {@code Future}, which may be an internal system thread such as an
   * RPC network thread.
   * </ul>
   *
   * <p>Also note that, regardless of which thread executes {@code
   * function.apply}, all other registered but unexecuted listeners are
   * prevented from running during its execution, even if those listeners are
   * to run in other executors.
   *
   * <p>The returned {@code Future} attempts to keep its cancellation state in
   * sync with that of the input future. That is, if the returned {@code Future}
   * is cancelled, it will attempt to cancel the input, and if the input is
   * cancelled, the returned {@code Future} will receive a callback in which it
   * will attempt to cancel itself.
   *
   * <p>An example use of this method is to convert a serializable object
   * returned from an RPC into a POJO.
   *
   * @param input The future to transform
   * @param function A Function to transform the results of the provided future
   *     to the results of the returned future.  This will be run in the thread
   *     that notifies input it is complete.
   * @return A future that holds result of the transformation.
   * @since 9.0 (in 1.0 as {@code compose})
   */
  public static <I, O> ListenableFuture<O> transform(ListenableFuture<I> input,
      final Function<? super I, ? extends O> function) {
    checkNotNull(function);
    ChainingListenableFuture<I, O> output =
        new ChainingListenableFuture<I, O>(asAsyncFunction(function), input);
    input.addListener(output, directExecutor());
    return output;
  }

  /**
   * Returns a new {@code ListenableFuture} whose result is the product of
   * applying the given {@code Function} to the result of the given {@code
   * Future}. Example:
   *
   * <pre>   {@code
   *   ListenableFuture<QueryResult> queryFuture = ...;
   *   Function<QueryResult, List<Row>> rowsFunction =
   *       new Function<QueryResult, List<Row>>() {
   *         public List<Row> apply(QueryResult queryResult) {
   *           return queryResult.getRows();
   *         }
   *       };
   *   ListenableFuture<List<Row>> rowsFuture =
   *       transform(queryFuture, rowsFunction, executor);}</pre>
   *
   * <p>The returned {@code Future} attempts to keep its cancellation state in
   * sync with that of the input future. That is, if the returned {@code Future}
   * is cancelled, it will attempt to cancel the input, and if the input is
   * cancelled, the returned {@code Future} will receive a callback in which it
   * will attempt to cancel itself.
   *
   * <p>An example use of this method is to convert a serializable object
   * returned from an RPC into a POJO.
   *
   * <p>When the transformation is fast and lightweight, consider {@linkplain
   * #transform(ListenableFuture, Function) omitting the executor} or
   * explicitly specifying {@code directExecutor}. However, be aware of the
   * caveats documented in the link above.
   *
   * @param input The future to transform
   * @param function A Function to transform the results of the provided future
   *     to the results of the returned future.
   * @param executor Executor to run the function in.
   * @return A future that holds result of the transformation.
   * @since 9.0 (in 2.0 as {@code compose})
   */
  public static <I, O> ListenableFuture<O> transform(ListenableFuture<I> input,
      final Function<? super I, ? extends O> function, Executor executor) {
    checkNotNull(function);
    return transformAsync(input, asAsyncFunction(function), executor);
  }

  /** Wraps the given function as an AsyncFunction. */
  static <I, O> AsyncFunction<I, O> asAsyncFunction(
      final Function<? super I, ? extends O> function) {
    checkNotNull(function);
    return new AsyncFunction<I, O>() {
      @Override public ListenableFuture<O> apply(I input) {
        O output = function.apply(input);
        return immediateFuture(output);
      }
    };
  }

  /**
   * An implementation of {@code ListenableFuture} that also implements
   * {@code Runnable} so that it can be used to nest ListenableFutures.
   * Once the passed-in {@code ListenableFuture} is complete, it calls the
   * passed-in {@code Function} to generate the result.
   *
   * <p>For historical reasons, this class has a special case in its exception
   * handling: If the given {@code AsyncFunction} throws an {@code
   * UndeclaredThrowableException}, {@code ChainingListenableFuture} unwraps it
   * and uses its <i>cause</i> as the output future's exception, rather than
   * using the {@code UndeclaredThrowableException} itself as it would for other
   * exception types. The reason for this is that {@code Futures.transform} used
   * to require a {@code Function}, whose {@code apply} method is not allowed to
   * throw checked exceptions. Nowadays, {@code Futures.transform} has an
   * overload that accepts an {@code AsyncFunction}, whose {@code apply} method
   * <i>is</i> allowed to throw checked exception. Users who wish to throw
   * checked exceptions should use that overload instead, and <a
   * href="http://code.google.com/p/guava-libraries/issues/detail?id=1548">we
   * should remove the {@code UndeclaredThrowableException} special case</a>.
   */
  private static final class ChainingListenableFuture<I, O>
      extends AbstractFuture.TrustedFuture<O> implements Runnable {

    private AsyncFunction<? super I, ? extends O> function;
    // In theory, this field might not be visible to a cancel() call in certain circumstances. For
    // details, see the comments on the fields of TimeoutFuture.
    private ListenableFuture<? extends I> inputFuture;

    private ChainingListenableFuture(
        AsyncFunction<? super I, ? extends O> function,
        ListenableFuture<? extends I> inputFuture) {
      this.function = checkNotNull(function);
      this.inputFuture = checkNotNull(inputFuture);
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
      /*
       * Our additional cancellation work needs to occur even if
       * !mayInterruptIfRunning, so we can't move it into interruptTask().
       */
      if (super.cancel(mayInterruptIfRunning)) {
        ListenableFuture<? extends I> localInputFuture = inputFuture;
        if (localInputFuture != null) {
          localInputFuture.cancel(mayInterruptIfRunning);
        }
        return true;
      }
      return false;
    }

    @Override
    public void run() {
      try {
        if (isCancelled()) {
          return;
        }
        I sourceResult;
        try {
          sourceResult = getUninterruptibly(inputFuture);
        } catch (CancellationException e) {
          // Cancel this future and return.
          // At this point, inputFuture is cancelled and outputFuture doesn't
          // exist, so the value of mayInterruptIfRunning is irrelevant.
          cancel(false);
          return;
        } catch (ExecutionException e) {
          // Set the cause of the exception as this future's exception
          setException(e.getCause());
          return;
        }

        ListenableFuture<? extends O> outputFuture = function.apply(sourceResult);
        checkNotNull(outputFuture, "AsyncFunction.apply returned null instead of a Future. "
            + "Did you mean to return immediateFuture(null)?");
        setFuture(outputFuture);
      } catch (UndeclaredThrowableException e) {
        // Set the cause of the exception as this future's exception
        setException(e.getCause());
      } catch (Throwable t) {
        // This exception is irrelevant in this thread, but useful for the
        // client
        setException(t);
      } finally {
        // Don't pin inputs beyond completion
        function = null;
        inputFuture = null;
      }
    }
  }

  /**
   * Returns a new {@code ListenableFuture} whose result is the product of
   * calling {@code get()} on the {@code Future} nested within the given {@code
   * Future}, effectively chaining the futures one after the other.  Example:
   *
   * <pre>   {@code
   *   SettableFuture<ListenableFuture<String>> nested = SettableFuture.create();
   *   ListenableFuture<String> dereferenced = dereference(nested);}</pre>
   *
   * <p>This call has the same cancellation and execution semantics as {@link
   * #transform(ListenableFuture, AsyncFunction)}, in that the returned {@code
   * Future} attempts to keep its cancellation state in sync with both the
   * input {@code Future} and the nested {@code Future}.  The transformation
   * is very lightweight and therefore takes place in the same thread (either
   * the thread that called {@code dereference}, or the thread in which the
   * dereferenced future completes).
   *
   * @param nested The nested future to transform.
   * @return A future that holds result of the inner future.
   * @since 13.0
   */
  @SuppressWarnings({"rawtypes", "unchecked"})
  @CheckReturnValue
  public static <V> ListenableFuture<V> dereference(
      ListenableFuture<? extends ListenableFuture<? extends V>> nested) {
    return transformAsync((ListenableFuture) nested, (AsyncFunction) DEREFERENCER);
  }

  /**
   * Helper {@code Function} for {@link #dereference}.
   */
  private static final AsyncFunction<ListenableFuture<Object>, Object> DEREFERENCER =
      new AsyncFunction<ListenableFuture<Object>, Object>() {
        @Override public ListenableFuture<Object> apply(ListenableFuture<Object> input) {
          return input;
        }
      };

  /**
   * Creates a new {@code ListenableFuture} whose value is a list containing the
   * values of all its input futures, if all succeed. If any input fails, the
   * returned future fails immediately.
   *
   * <p>The list of results is in the same order as the input list.
   *
   * <p>Canceling this future will attempt to cancel all the component futures,
   * and if any of the provided futures fails or is canceled, this one is,
   * too.
   *
   * @param futures futures to combine
   * @return a future that provides a list of the results of the component
   *         futures
   * @since 10.0
   */
  @Beta
  @SafeVarargs
  @CheckReturnValue
  public static <V> ListenableFuture<List<V>> allAsList(
      ListenableFuture<? extends V>... futures) {
    return new ListFuture<V>(ImmutableList.copyOf(futures), true);
  }

  /**
   * Creates a new {@code ListenableFuture} whose value is a list containing the
   * values of all its input futures, if all succeed. If any input fails, the
   * returned future fails immediately.
   *
   * <p>The list of results is in the same order as the input list.
   *
   * <p>Canceling this future will attempt to cancel all the component futures,
   * and if any of the provided futures fails or is canceled, this one is,
   * too.
   *
   * @param futures futures to combine
   * @return a future that provides a list of the results of the component
   *         futures
   * @since 10.0
   */
  @Beta
  @CheckReturnValue
  public static <V> ListenableFuture<List<V>> allAsList(
      Iterable<? extends ListenableFuture<? extends V>> futures) {
    return new ListFuture<V>(ImmutableList.copyOf(futures), true);
  }

  /**
   * Creates a new {@code ListenableFuture} whose value is a list containing the
   * values of all its successful input futures. The list of results is in the
   * same order as the input list, and if any of the provided futures fails or
   * is canceled, its corresponding position will contain {@code null} (which is
   * indistinguishable from the future having a successful value of
   * {@code null}).
   *
   * <p>Canceling this future will attempt to cancel all the component futures.
   *
   * @param futures futures to combine
   * @return a future that provides a list of the results of the component
   *         futures
   * @since 10.0
   */
  @Beta
  @SafeVarargs
  @CheckReturnValue
  public static <V> ListenableFuture<List<V>> successfulAsList(
      ListenableFuture<? extends V>... futures) {
    return new ListFuture<V>(ImmutableList.copyOf(futures), false);
  }

  /**
   * Creates a new {@code ListenableFuture} whose value is a list containing the
   * values of all its successful input futures. The list of results is in the
   * same order as the input list, and if any of the provided futures fails or
   * is canceled, its corresponding position will contain {@code null} (which is
   * indistinguishable from the future having a successful value of
   * {@code null}).
   *
   * <p>Canceling this future will attempt to cancel all the component futures.
   *
   * @param futures futures to combine
   * @return a future that provides a list of the results of the component
   *         futures
   * @since 10.0
   */
  @Beta
  @CheckReturnValue
  public static <V> ListenableFuture<List<V>> successfulAsList(
      Iterable<? extends ListenableFuture<? extends V>> futures) {
    return new ListFuture<V>(ImmutableList.copyOf(futures), false);
  }

  /**
   * Registers separate success and failure callbacks to be run when the {@code
   * Future}'s computation is {@linkplain java.util.concurrent.Future#isDone()
   * complete} or, if the computation is already complete, immediately.
   *
   * <p>There is no guaranteed ordering of execution of callbacks, but any
   * callback added through this method is guaranteed to be called once the
   * computation is complete.
   *
   * Example: <pre> {@code
   * ListenableFuture<QueryResult> future = ...;
   * addCallback(future,
   *     new FutureCallback<QueryResult> {
   *       public void onSuccess(QueryResult result) {
   *         storeInCache(result);
   *       }
   *       public void onFailure(Throwable t) {
   *         reportError(t);
   *       }
   *     });}</pre>
   *
   * <p>Note: If the callback is slow or heavyweight, consider {@linkplain
   * #addCallback(ListenableFuture, FutureCallback, Executor) supplying an
   * executor}. If you do not supply an executor, {@code addCallback} will use
   * a {@linkplain MoreExecutors#directExecutor direct executor}, which carries
   * some caveats for heavier operations. For example, the callback may run on
   * an unpredictable or undesirable thread:
   *
   * <ul>
   * <li>If the input {@code Future} is done at the time {@code addCallback} is
   * called, {@code addCallback} will execute the callback inline.
   * <li>If the input {@code Future} is not yet done, {@code addCallback} will
   * schedule the callback to be run by the thread that completes the input
   * {@code Future}, which may be an internal system thread such as an RPC
   * network thread.
   * </ul>
   *
   * <p>Also note that, regardless of which thread executes the callback, all
   * other registered but unexecuted listeners are prevented from running
   * during its execution, even if those listeners are to run in other
   * executors.
   *
   * <p>For a more general interface to attach a completion listener to a
   * {@code Future}, see {@link ListenableFuture#addListener addListener}.
   *
   * @param future The future attach the callback to.
   * @param callback The callback to invoke when {@code future} is completed.
   * @since 10.0
   */
  public static <V> void addCallback(ListenableFuture<V> future,
      FutureCallback<? super V> callback) {
    addCallback(future, callback, directExecutor());
  }

  /**
   * Registers separate success and failure callbacks to be run when the {@code
   * Future}'s computation is {@linkplain java.util.concurrent.Future#isDone()
   * complete} or, if the computation is already complete, immediately.
   *
   * <p>The callback is run in {@code executor}.
   * There is no guaranteed ordering of execution of callbacks, but any
   * callback added through this method is guaranteed to be called once the
   * computation is complete.
   *
   * Example: <pre> {@code
   * ListenableFuture<QueryResult> future = ...;
   * Executor e = ...
   * addCallback(future,
   *     new FutureCallback<QueryResult> {
   *       public void onSuccess(QueryResult result) {
   *         storeInCache(result);
   *       }
   *       public void onFailure(Throwable t) {
   *         reportError(t);
   *       }
   *     }, e);}</pre>
   *
   * <p>When the callback is fast and lightweight, consider {@linkplain
   * #addCallback(ListenableFuture, FutureCallback) omitting the executor} or
   * explicitly specifying {@code directExecutor}. However, be aware of the
   * caveats documented in the link above.
   *
   * <p>For a more general interface to attach a completion listener to a
   * {@code Future}, see {@link ListenableFuture#addListener addListener}.
   *
   * @param future The future attach the callback to.
   * @param callback The callback to invoke when {@code future} is completed.
   * @param executor The executor to run {@code callback} when the future
   *    completes.
   * @since 10.0
   */
  public static <V> void addCallback(final ListenableFuture<V> future,
      final FutureCallback<? super V> callback, Executor executor) {
    Preconditions.checkNotNull(callback);
    Runnable callbackListener = new Runnable() {
      @Override
      public void run() {
        final V value;
        try {
          // TODO(user): (Before Guava release), validate that this
          // is the thing for IE.
          value = getUninterruptibly(future);
        } catch (ExecutionException e) {
          callback.onFailure(e.getCause());
          return;
        } catch (RuntimeException e) {
          callback.onFailure(e);
          return;
        } catch (Error e) {
          callback.onFailure(e);
          return;
        }
        callback.onSuccess(value);
      }
    };
    future.addListener(callbackListener, executor);
  }

  /*
   * TODO(user): FutureChecker interface for these to be static methods on? If
   * so, refer to it in the (static-method) Futures.getChecked documentation
   */

  /*
   * Arguably we don't need a timed getUnchecked because any operation slow
   * enough to require a timeout is heavyweight enough to throw a checked
   * exception and therefore be inappropriate to use with getUnchecked. Further,
   * it's not clear that converting the checked TimeoutException to a
   * RuntimeException -- especially to an UncheckedExecutionException, since it
   * wasn't thrown by the computation -- makes sense, and if we don't convert
   * it, the user still has to write a try-catch block.
   *
   * If you think you would use this method, let us know.
   */

  /** Used for {@link #allAsList} and {@link #successfulAsList}. */
  private static final class ListFuture<V> extends CollectionFuture<V, List<V>> {
    ListFuture(ImmutableCollection<? extends ListenableFuture<? extends V>> futures,
        boolean allMustSucceed) {
      init(new ListFutureRunningState(futures, allMustSucceed));
    }

    private final class ListFutureRunningState extends CollectionFutureRunningState {
      ListFutureRunningState(ImmutableCollection<? extends ListenableFuture<? extends V>> futures,
        boolean allMustSucceed) {
        super(futures, allMustSucceed);
      }

      @Override
      public List<V> combine(List<Optional<V>> values) {
        List<V> result = Lists.newArrayList();
        for (Optional<V> element : values) {
          result.add(element != null ? element.orNull() : null);
        }
        return Collections.unmodifiableList(result);
      }
    }
  }
}
