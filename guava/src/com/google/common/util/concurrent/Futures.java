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
import com.google.common.annotations.GwtIncompatible;
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Queues;

import java.lang.reflect.UndeclaredThrowableException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nullable;

/**
 * Static utility methods pertaining to the {@link Future} interface.
 *
 * <p>Many of these methods use the {@link ListenableFuture} API; consult the
 * Guava User Guide article on <a href=
 * "https://github.com/google/guava/wiki/ListenableFutureExplained">
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
  // A consequence of these requirements is that the delegate futures cannot be stored in
  // final fields.
  //
  // For simplicity the rest of this description will discuss Futures.catching since it is the
  // simplest instance, though very similar descriptions apply to many other classes in this file.
  //
  // In the constructor of AbstractCatchingFuture, the delegate future is assigned to a field
  // 'inputFuture'. That field is non-final and non-volatile.  There are 2 places where the
  // 'inputFuture' field is read and where we will have to consider visibility of the write
  // operation in the constructor.
  //
  // 1. In the listener that performs the callback.  In this case it is fine since inputFuture is
  //    assigned prior to calling addListener, and addListener happens-before any invocation of the
  //    listener. Notably, this means that 'volatile' is unnecessary to make 'inputFuture' visible
  //    to the listener.
  //
  // 2. In done() where we may propagate cancellation to the input.  In this case it is _not_ fine.
  //    There is currently nothing that enforces that the write to inputFuture in the constructor is
  //    visible to done().  This is because there is no happens before edge between the write and a
  //    (hypothetical) unsafe read by our caller. Note: adding 'volatile' does not fix this issue,
  //    it would just add an edge such that if done() observed non-null, then it would also
  //    definitely observe all earlier writes, but we still have no guarantee that done() would see
  //    the inital write (just stronger guarantees if it does).
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

  /**
   * Creates a {@link CheckedFuture} out of a normal {@link ListenableFuture} and a {@link Function}
   * that maps from {@link Exception} instances into the appropriate checked type.
   *
   * <p><b>Warning:</b> We recommend against using {@code CheckedFuture} in new projects. {@code
   * CheckedFuture} is difficult to build libraries atop. {@code CheckedFuture} ports of methods
   * like {@link Futures#transformAsync} have historically had bugs, and some of these bugs are
   * necessary, unavoidable consequences of the {@code CheckedFuture} API. Additionally, {@code
   * CheckedFuture} encourages users to take exceptions from one thread and rethrow them in another,
   * producing confusing stack traces.
   *
   * <p>The given mapping function will be applied to an
   * {@link InterruptedException}, a {@link CancellationException}, or an
   * {@link ExecutionException}.
   * See {@link Future#get()} for details on the exceptions thrown.
   *
   * @since 9.0 (source-compatible since 1.0)
   */
  @GwtIncompatible("TODO")
  @CheckReturnValue
  public static <V, X extends Exception> CheckedFuture<V, X> makeChecked(
      ListenableFuture<V> future, Function<? super Exception, X> mapper) {
    return new MappingCheckedFuture<V, X>(checkNotNull(future), mapper);
  }

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
    static final ImmediateSuccessfulFuture<Object> NULL =
        new ImmediateSuccessfulFuture<Object>(null);

    @Nullable private final V value;

    ImmediateSuccessfulFuture(@Nullable V value) {
      this.value = value;
    }

    @Override
    public V get() {
      return value;
    }
  }

  @GwtIncompatible("TODO")
  private static class ImmediateSuccessfulCheckedFuture<V, X extends Exception>
      extends ImmediateFuture<V> implements CheckedFuture<V, X> {

    @Nullable private final V value;

    ImmediateSuccessfulCheckedFuture(@Nullable V value) {
      this.value = value;
    }

    @Override
    public V get() {
      return value;
    }

    @Override
    public V checkedGet() {
      return value;
    }

    @Override
    public V checkedGet(long timeout, TimeUnit unit) {
      checkNotNull(unit);
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

  @GwtIncompatible("TODO")
  private static class ImmediateCancelledFuture<V> extends ImmediateFuture<V> {

    private final CancellationException thrown;

    ImmediateCancelledFuture() {
      this.thrown = new CancellationException("Immediate cancelled future.");
    }

    @Override
    public boolean isCancelled() {
      return true;
    }

    @Override
    public V get() {
      throw AbstractFuture.cancellationExceptionWithCause(
          "Task was cancelled.", thrown);
    }
  }

  @GwtIncompatible("TODO")
  private static class ImmediateFailedCheckedFuture<V, X extends Exception>
      extends ImmediateFuture<V> implements CheckedFuture<V, X> {

    private final X thrown;

    ImmediateFailedCheckedFuture(X thrown) {
      this.thrown = thrown;
    }

    @Override
    public V get() throws ExecutionException {
      throw new ExecutionException(thrown);
    }

    @Override
    public V checkedGet() throws X {
      throw thrown;
    }

    @Override
    public V checkedGet(long timeout, TimeUnit unit) throws X {
      checkNotNull(unit);
      throw thrown;
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
    if (value == null) {
      // This cast is safe because null is assignable to V for all V (i.e. it is covariant)
      @SuppressWarnings({"unchecked", "rawtypes"})
      ListenableFuture<V> typedNull = (ListenableFuture) ImmediateSuccessfulFuture.NULL;
      return typedNull;
    }
    return new ImmediateSuccessfulFuture<V>(value);
  }

  /**
   * Returns a {@code CheckedFuture} which has its value set immediately upon
   * construction.
   *
   * <p>The returned {@code Future} can't be cancelled, and its {@code isDone()}
   * method always returns {@code true}. Calling {@code get()} or {@code
   * checkedGet()} will immediately return the provided value.
   */
  @GwtIncompatible("TODO")
  @CheckReturnValue
  public static <V, X extends Exception> CheckedFuture<V, X>
      immediateCheckedFuture(@Nullable V value) {
    return new ImmediateSuccessfulCheckedFuture<V, X>(value);
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
   * Creates a {@code ListenableFuture} which is cancelled immediately upon
   * construction, so that {@code isCancelled()} always returns {@code true}.
   *
   * @since 14.0
   */
  @GwtIncompatible("TODO")
  @CheckReturnValue
  public static <V> ListenableFuture<V> immediateCancelledFuture() {
    return new ImmediateCancelledFuture<V>();
  }

  /**
   * Returns a {@code CheckedFuture} which has an exception set immediately upon
   * construction.
   *
   * <p>The returned {@code Future} can't be cancelled, and its {@code isDone()}
   * method always returns {@code true}. Calling {@code get()} will immediately
   * throw the provided {@code Exception} wrapped in an {@code
   * ExecutionException}, and calling {@code checkedGet()} will throw the
   * provided exception itself.
   */
  @GwtIncompatible("TODO")
  @CheckReturnValue
  public static <V, X extends Exception> CheckedFuture<V, X>
      immediateFailedCheckedFuture(X exception) {
    checkNotNull(exception);
    return new ImmediateFailedCheckedFuture<V, X>(exception);
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
   * <p>This overload, which does not accept an executor, uses {@code
   * directExecutor}, a dangerous choice in some cases. See the discussion in
   * the {@link ListenableFuture#addListener ListenableFuture.addListener}
   * documentation. The documentation's warnings about "lightweight listeners"
   * refer here to the work done during {@code FutureFallback.create}, not to
   * any work done to complete the returned {@code Future}.
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
   * <p>When selecting an executor, note that {@code directExecutor} is
   * dangerous in some cases. See the discussion in the {@link
   * ListenableFuture#addListener ListenableFuture.addListener} documentation.
   * The documentation's warnings about "lightweight listeners" refer here to
   * the work done during {@code FutureFallback.create}, not to any work done to
   * complete the returned {@code Future}.
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

  /**
   * Returns a {@code Future} whose result is taken from the given primary {@code input} or, if the
   * primary input fails with the given {@code exceptionType}, from the result provided by the
   * {@code fallback}. {@link Function#apply} is not invoked until the primary input has failed, so
   * if the primary input succeeds, it is never invoked. If, during the invocation of {@code
   * fallback}, an exception is thrown, this exception is used as the result of the output {@code
   * Future}.
   *
   * <p>Usage example:
   *
   * <pre>   {@code
   *   ListenableFuture<Integer> fetchCounterFuture = ...;
   *
   *   // Falling back to a zero counter in case an exception happens when
   *   // processing the RPC to fetch counters.
   *   ListenableFuture<Integer> faultTolerantFuture = Futures.catching(
   *       fetchCounterFuture, FetchException.class,
   *       new Function<FetchException, Integer>() {
   *         public Integer apply(FetchException e) {
   *           return 0;
   *         }
   *       });}</pre>
   *
   * <p>This overload, which does not accept an executor, uses {@code directExecutor}, a dangerous
   * choice in some cases. See the discussion in the {@link ListenableFuture#addListener
   * ListenableFuture.addListener} documentation. The documentation's warnings about "lightweight
   * listeners" refer here to the work done during {@code Function.apply}.
   *
   * @param input the primary input {@code Future}
   * @param exceptionType the exception type that triggers use of {@code fallback}. To avoid hiding
   *     bugs and other unrecoverable errors, callers should prefer more specific types, avoiding
   *     {@code Throwable.class} in particular.
   * @param fallback the {@link Function} implementation to be called if {@code input} fails with
   *     the expected exception type
   * @since 19.0
   */
  @GwtIncompatible("AVAILABLE but requires exceptionType to be Throwable.class")
  @CheckReturnValue
  public static <V, X extends Throwable> ListenableFuture<V> catching(
      ListenableFuture<? extends V> input, Class<X> exceptionType,
      Function<? super X, ? extends V> fallback) {
    CatchingFuture<V, X> future = new CatchingFuture<V, X>(input, exceptionType, fallback);
    input.addListener(future, directExecutor());
    return future;
  }

  /**
   * Returns a {@code Future} whose result is taken from the given primary {@code input} or, if the
   * primary input fails with the given {@code exceptionType}, from the result provided by the
   * {@code fallback}. {@link Function#apply} is not invoked until the primary input has failed, so
   * if the primary input succeeds, it is never invoked. If, during the invocation of {@code
   * fallback}, an exception is thrown, this exception is used as the result of the output {@code
   * Future}.
   *
   * <p>Usage example:
   *
   * <pre>   {@code
   *   ListenableFuture<Integer> fetchCounterFuture = ...;
   *
   *   // Falling back to a zero counter in case an exception happens when
   *   // processing the RPC to fetch counters.
   *   ListenableFuture<Integer> faultTolerantFuture = Futures.catching(
   *       fetchCounterFuture, FetchException.class,
   *       new Function<FetchException, Integer>() {
   *         public Integer apply(FetchException e) {
   *           return 0;
   *         }
   *       }, directExecutor());}</pre>
   *
   * <p>When selecting an executor, note that {@code directExecutor} is dangerous in some cases. See
   * the discussion in the {@link ListenableFuture#addListener ListenableFuture.addListener}
   * documentation. The documentation's warnings about "lightweight listeners" refer here to the
   * work done during {@code Function.apply}.
   *
   * @param input the primary input {@code Future}
   * @param exceptionType the exception type that triggers use of {@code fallback}. To avoid hiding
   *     bugs and other unrecoverable errors, callers should prefer more specific types, avoiding
   *     {@code Throwable.class} in particular.
   * @param fallback the {@link Function} implementation to be called if {@code input} fails with
   *     the expected exception type
   * @param executor the executor that runs {@code fallback} if {@code input} fails
   * @since 19.0
   */
  @GwtIncompatible("AVAILABLE but requires exceptionType to be Throwable.class")
  @CheckReturnValue
  public static <V, X extends Throwable> ListenableFuture<V> catching(
      ListenableFuture<? extends V> input, Class<X> exceptionType,
      Function<? super X, ? extends V> fallback, Executor executor) {
    CatchingFuture<V, X> future = new CatchingFuture<V, X>(input, exceptionType, fallback);
    input.addListener(future, rejectionPropagatingExecutor(executor, future));
    return future;
  }

  /**
   * Returns a {@code Future} whose result is taken from the given primary {@code input} or, if the
   * primary input fails with the given {@code exceptionType}, from the result provided by the
   * {@code fallback}. {@link AsyncFunction#apply} is not invoked until the primary input has
   * failed, so if the primary input succeeds, it is never invoked. If, during the invocation of
   * {@code fallback}, an exception is thrown, this exception is used as the result of the output
   * {@code Future}.
   *
   * <p>Usage examples:
   *
   * <pre>   {@code
   *   ListenableFuture<Integer> fetchCounterFuture = ...;
   *
   *   // Falling back to a zero counter in case an exception happens when
   *   // processing the RPC to fetch counters.
   *   ListenableFuture<Integer> faultTolerantFuture = Futures.catchingAsync(
   *       fetchCounterFuture, FetchException.class,
   *       new AsyncFunction<FetchException, Integer>() {
   *         public ListenableFuture<Integer> apply(FetchException e) {
   *           return immediateFuture(0);
   *         }
   *       });}</pre>
   *
   * <p>The fallback can also choose to propagate the original exception when desired:
   *
   * <pre>   {@code
   *   ListenableFuture<Integer> fetchCounterFuture = ...;
   *
   *   // Falling back to a zero counter only in case the exception was a
   *   // TimeoutException.
   *   ListenableFuture<Integer> faultTolerantFuture = Futures.catchingAsync(
   *       fetchCounterFuture, FetchException.class,
   *       new AsyncFunction<FetchException, Integer>() {
   *         public ListenableFuture<Integer> apply(FetchException e)
   *             throws FetchException {
   *           if (omitDataOnFetchFailure) {
   *             return immediateFuture(0);
   *           }
   *           throw e;
   *         }
   *       });}</pre>
   *
   * <p>This overload, which does not accept an executor, uses {@code directExecutor}, a dangerous
   * choice in some cases. See the discussion in the {@link ListenableFuture#addListener
   * ListenableFuture.addListener} documentation. The documentation's warnings about "lightweight
   * listeners" refer here to the work done during {@code AsyncFunction.apply}, not to any work done
   * to complete the returned {@code Future}.
   *
   * @param input the primary input {@code Future}
   * @param exceptionType the exception type that triggers use of {@code fallback}. To avoid hiding
   *     bugs and other unrecoverable errors, callers should prefer more specific types, avoiding
   *     {@code Throwable.class} in particular.
   * @param fallback the {@link AsyncFunction} implementation to be called if {@code input} fails
   *     with the expected exception type
   * @since 19.0 (similar functionality in 14.0 as {@code withFallback})
   */
  @GwtIncompatible("AVAILABLE but requires exceptionType to be Throwable.class")
  // TODO(kak): @CheckReturnValue
  public static <V, X extends Throwable> ListenableFuture<V> catchingAsync(
      ListenableFuture<? extends V> input, Class<X> exceptionType,
      AsyncFunction<? super X, ? extends V> fallback) {
    AsyncCatchingFuture<V, X> future =
        new AsyncCatchingFuture<V, X>(input, exceptionType, fallback);
    input.addListener(future, directExecutor());
    return future;
  }

  /**
   * Returns a {@code Future} whose result is taken from the given primary {@code input} or, if the
   * primary input fails with the given {@code exceptionType}, from the result provided by the
   * {@code fallback}. {@link AsyncFunction#apply} is not invoked until the primary input has
   * failed, so if the primary input succeeds, it is never invoked. If, during the invocation of
   * {@code fallback}, an exception is thrown, this exception is used as the result of the output
   * {@code Future}.
   *
   * <p>Usage examples:
   *
   * <pre>   {@code
   *   ListenableFuture<Integer> fetchCounterFuture = ...;
   *
   *   // Falling back to a zero counter in case an exception happens when
   *   // processing the RPC to fetch counters.
   *   ListenableFuture<Integer> faultTolerantFuture = Futures.catchingAsync(
   *       fetchCounterFuture, FetchException.class,
   *       new AsyncFunction<FetchException, Integer>() {
   *         public ListenableFuture<Integer> apply(FetchException e) {
   *           return immediateFuture(0);
   *         }
   *       }, directExecutor());}</pre>
   *
   * <p>The fallback can also choose to propagate the original exception when desired:
   *
   * <pre>   {@code
   *   ListenableFuture<Integer> fetchCounterFuture = ...;
   *
   *   // Falling back to a zero counter only in case the exception was a
   *   // TimeoutException.
   *   ListenableFuture<Integer> faultTolerantFuture = Futures.catchingAsync(
   *       fetchCounterFuture, FetchException.class,
   *       new AsyncFunction<FetchException, Integer>() {
   *         public ListenableFuture<Integer> apply(FetchException e)
   *             throws FetchException {
   *           if (omitDataOnFetchFailure) {
   *             return immediateFuture(0);
   *           }
   *           throw e;
   *         }
   *       }, directExecutor());}</pre>
   *
   * <p>When selecting an executor, note that {@code directExecutor} is dangerous in some cases. See
   * the discussion in the {@link ListenableFuture#addListener ListenableFuture.addListener}
   * documentation. The documentation's warnings about "lightweight listeners" refer here to the
   * work done during {@code AsyncFunction.apply}, not to any work done to complete the returned
   * {@code Future}.
   *
   * @param input the primary input {@code Future}
   * @param exceptionType the exception type that triggers use of {@code fallback}. To avoid hiding
   *     bugs and other unrecoverable errors, callers should prefer more specific types, avoiding
   *     {@code Throwable.class} in particular.
   * @param fallback the {@link AsyncFunction} implementation to be called if {@code input} fails
   *     with the expected exception type
   * @param executor the executor that runs {@code fallback} if {@code input} fails
   * @since 19.0 (similar functionality in 14.0 as {@code withFallback})
   */
  @GwtIncompatible("AVAILABLE but requires exceptionType to be Throwable.class")
  // TODO(kak): @CheckReturnValue
  public static <V, X extends Throwable> ListenableFuture<V> catchingAsync(
      ListenableFuture<? extends V> input, Class<X> exceptionType,
      AsyncFunction<? super X, ? extends V> fallback, Executor executor) {
    AsyncCatchingFuture<V, X> future =
        new AsyncCatchingFuture<V, X>(input, exceptionType, fallback);
    input.addListener(future, rejectionPropagatingExecutor(executor, future));
    return future;
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

  private abstract static class AbstractCatchingFuture<V, X extends Throwable, F>
      extends AbstractFuture.TrustedFuture<V> implements Runnable {
    @Nullable ListenableFuture<? extends V> inputFuture;
    @Nullable Class<X> exceptionType;
    @Nullable F fallback;

    AbstractCatchingFuture(
        ListenableFuture<? extends V> inputFuture, Class<X> exceptionType, F fallback) {
      this.inputFuture = checkNotNull(inputFuture);
      this.exceptionType = checkNotNull(exceptionType);
      this.fallback = checkNotNull(fallback);
    }

    @Override public final void run() {
      ListenableFuture<? extends V> localInputFuture = inputFuture;
      Class<X> localExceptionType = exceptionType;
      F localFallback = fallback;
      if (localInputFuture == null | localExceptionType == null | localFallback == null
          | isCancelled()) {
        return;
      }
      inputFuture = null;
      exceptionType = null;
      fallback = null;

      Throwable throwable;
      try {
        set(getUninterruptibly(localInputFuture));
        return;
      } catch (ExecutionException e) {
        throwable = e.getCause();
      } catch (Throwable e) {  // this includes cancellation exception
        throwable = e;
      }
      try {
        if (isInstanceOfThrowableClass(throwable, localExceptionType)) {
          @SuppressWarnings("unchecked") // verified safe by isInstance
          X castThrowable = (X) throwable;
          doFallback(localFallback, castThrowable);
        } else {
          setException(throwable);
        }
      } catch (Throwable e) {
        setException(e);
      }
    }

    /** Template method for subtypes to actually run the fallback. */
    abstract void doFallback(F fallback, X throwable) throws Exception;

    @Override final void done() {
      maybePropagateCancellation(inputFuture);
      this.inputFuture = null;
      this.exceptionType = null;
      this.fallback = null;
    }
  }

  /**
   * A {@link AbstractCatchingFuture} that delegates to an {@link AsyncFunction}
   * and {@link #setFuture(ListenableFuture)} to implement {@link #doFallback}
   */
  static final class AsyncCatchingFuture<V, X extends Throwable>
      extends AbstractCatchingFuture<V, X, AsyncFunction<? super X, ? extends V>> {

    AsyncCatchingFuture(ListenableFuture<? extends V> input, Class<X> exceptionType,
        AsyncFunction<? super X, ? extends V> fallback) {
      super(input, exceptionType, fallback);
    }

    @Override void doFallback(
        AsyncFunction<? super X, ? extends V> fallback, X cause) throws Exception {
      ListenableFuture<? extends V> replacement = fallback.apply(cause);
      checkNotNull(replacement, "AsyncFunction.apply returned null instead of a Future. "
          + "Did you mean to return immediateFuture(null)?");
      setFuture(replacement);
    }
  }

  /**
   * A {@link AbstractCatchingFuture} that delegates to a {@link Function}
   * and {@link #set(Object)} to implement {@link #doFallback}
   */
  static final class CatchingFuture<V, X extends Throwable>
      extends AbstractCatchingFuture<V, X, Function<? super X, ? extends V>> {
    CatchingFuture(ListenableFuture<? extends V> input, Class<X> exceptionType,
        Function<? super X, ? extends V> fallback) {
      super(input, exceptionType, fallback);
    }

    @Override void doFallback(Function<? super X, ? extends V> fallback, X cause) throws Exception {
      V replacement = fallback.apply(cause);
      set(replacement);
    }
  }

  /**
   * Returns a future that delegates to another but will finish early (via a
   * {@link TimeoutException} wrapped in an {@link ExecutionException}) if the
   * specified duration expires.
   *
   * <p>The delegate future is interrupted and cancelled if it times out.
   *
   * @param delegate The future to delegate to.
   * @param time when to timeout the future
   * @param unit the time unit of the time parameter
   * @param scheduledExecutor The executor service to enforce the timeout.
   *
   * @since 19.0
   */
  @GwtIncompatible("java.util.concurrent.ScheduledExecutorService")
  @CheckReturnValue
  public static <V> ListenableFuture<V> withTimeout(ListenableFuture<V> delegate,
      long time, TimeUnit unit, ScheduledExecutorService scheduledExecutor) {
    TimeoutFuture<V> result = new TimeoutFuture<V>(delegate);
    TimeoutFuture.Fire<V> fire = new TimeoutFuture.Fire<V>(result);
    result.timer = scheduledExecutor.schedule(fire, time, unit);
    delegate.addListener(fire, directExecutor());
    return result;
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

    @Nullable ListenableFuture<V> delegateRef;
    @Nullable Future<?> timer;

    TimeoutFuture(ListenableFuture<V> delegate) {
      this.delegateRef = Preconditions.checkNotNull(delegate);
    }

    /** A runnable that is called when the delegate or the timer completes. */
    private static final class Fire<V> implements Runnable {
      @Nullable TimeoutFuture<V> timeoutFutureRef;

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

        /*
         * If we're about to complete the TimeoutFuture, we want to release our reference to it.
         * Otherwise, we'll pin it (and its result) in memory until the timeout task is GCed. (The
         * need to clear our reference to the TimeoutFuture is the reason we use a *static* nested
         * class with a manual reference back to the "containing" class.)
         *
         * This has the nice-ish side effect of limiting reentrancy: run() calls
         * timeoutFuture.setException() calls run(). That reentrancy would already be harmless,
         * since timeoutFuture can be set (and delegate cancelled) only once. (And "set only once"
         * is important for other reasons: run() can still be invoked concurrently in different
         * threads, even with the above null checks.)
         */
        timeoutFutureRef = null;
        if (delegate.isDone()) {
          timeoutFuture.setFuture(delegate);
        } else {
          try {
            // TODO(lukes): this stack trace is particularly useless (all it does is point at the
            // scheduledexecutorservice thread), consider eliminating it altogether?
            timeoutFuture.setException(new TimeoutException("Future timed out: " + delegate));
          } finally {
            delegate.cancel(true);
          }
        }
      }
    }

    @Override void done() {
      maybePropagateCancellation(delegateRef);

      Future<?> localTimer = timer;
      // Try to cancel the timer as an optimization
      // timer may be null if this call to run was by the timer task since there is no
      // happens-before edge between the assignment to timer and an execution of the timer task.
      if (localTimer != null) {
        localTimer.cancel(false);
      }

      delegateRef = null;
      timer = null;
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
   * <p>This overload, which does not accept an executor, uses {@code
   * directExecutor}, a dangerous choice in some cases. See the discussion in
   * the {@link ListenableFuture#addListener ListenableFuture.addListener}
   * documentation. The documentation's warnings about "lightweight listeners"
   * refer here to the work done during {@code AsyncFunction.apply}, not to any
   * work done to complete the returned {@code Future}.
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
   * <p>When selecting an executor, note that {@code directExecutor} is
   * dangerous in some cases. See the discussion in the {@link
   * ListenableFuture#addListener ListenableFuture.addListener} documentation.
   * The documentation's warnings about "lightweight listeners" refer here to
   * the work done during {@code AsyncFunction.apply}, not to any work done to
   * complete the returned {@code Future}.
   *
   * <p>The returned {@code Future} attempts to keep its cancellation state in
   * sync with that of the input future and that of the future returned by the
   * chain function. That is, if the returned {@code Future} is cancelled, it
   * will attempt to cancel the other two, and if either of the other two is
   * cancelled, the returned {@code Future} will receive a callback in which it
   * will attempt to cancel itself.
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
   * <p>This overload, which does not accept an executor, uses {@code directExecutor}, a dangerous
   * choice in some cases. See the discussion in the {@link ListenableFuture#addListener
   * ListenableFuture.addListener} documentation. The documentation's warnings about "lightweight
   * listeners" refer here to the work done during {@code AsyncFunction.apply}, not to any work done
   * to complete the returned {@code Future}.
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
    AsyncChainingFuture<I, O> output = new AsyncChainingFuture<I, O>(input, function);
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
   * <p>When selecting an executor, note that {@code directExecutor} is dangerous in some cases. See
   * the discussion in the {@link ListenableFuture#addListener ListenableFuture.addListener}
   * documentation. The documentation's warnings about "lightweight listeners" refer here to the
   * work done during {@code AsyncFunction.apply}, not to any work done to complete the returned
   * {@code Future}.
   *
   * <p>The returned {@code Future} attempts to keep its cancellation state in sync with that of the
   * input future and that of the future returned by the chain function. That is, if the returned
   * {@code Future} is cancelled, it will attempt to cancel the other two, and if either of the
   * other two is cancelled, the returned {@code Future} will receive a callback in which it will
   * attempt to cancel itself.
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
    AsyncChainingFuture<I, O> output = new AsyncChainingFuture<I, O>(input, function);
    input.addListener(output, rejectionPropagatingExecutor(executor, output));
    return output;
  }

  /**
   * Returns an Executor that will propagate {@link RejectedExecutionException} from the delegate
   * executor to the given {@code future}.
   *
   * <p>Note, the returned executor can only be used once.
   */
  private static Executor rejectionPropagatingExecutor(
      final Executor delegate, final AbstractFuture<?> future) {
    checkNotNull(delegate);
    if (delegate == directExecutor()) {
      // directExecutor() cannot throw RejectedExecutionException
      return delegate;
    }
    return new Executor() {
      volatile boolean thrownFromDelegate = true;
      @Override public void execute(final Runnable command) {
        try {
          delegate.execute(new Runnable() {
            @Override public void run() {
              thrownFromDelegate = false;
              command.run();
            }
          });
        } catch (RejectedExecutionException e) {
          if (thrownFromDelegate) {
            // wrap exception?
            future.setException(e);
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
   * <p>This overload, which does not accept an executor, uses {@code
   * directExecutor}, a dangerous choice in some cases. See the discussion in
   * the {@link ListenableFuture#addListener ListenableFuture.addListener}
   * documentation. The documentation's warnings about "lightweight listeners"
   * refer here to the work done during {@code Function.apply}.
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
    ChainingFuture<I, O> output = new ChainingFuture<I, O>(input, function);
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
   * <p>When selecting an executor, note that {@code directExecutor} is
   * dangerous in some cases. See the discussion in the {@link
   * ListenableFuture#addListener ListenableFuture.addListener} documentation.
   * The documentation's warnings about "lightweight listeners" refer here to
   * the work done during {@code Function.apply}.
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
   *     to the results of the returned future.
   * @param executor Executor to run the function in.
   * @return A future that holds result of the transformation.
   * @since 9.0 (in 2.0 as {@code compose})
   */
  public static <I, O> ListenableFuture<O> transform(ListenableFuture<I> input,
      final Function<? super I, ? extends O> function, Executor executor) {
    checkNotNull(function);
    ChainingFuture<I, O> output = new ChainingFuture<I, O>(input, function);
    input.addListener(output, rejectionPropagatingExecutor(executor, output));
    return output;
  }

  /**
   * Like {@link #transform(ListenableFuture, Function)} except that the
   * transformation {@code function} is invoked on each call to
   * {@link Future#get() get()} on the returned future.
   *
   * <p>The returned {@code Future} reflects the input's cancellation
   * state directly, and any attempt to cancel the returned Future is likewise
   * passed through to the input Future.
   *
   * <p>Note that calls to {@linkplain Future#get(long, TimeUnit) timed get}
   * only apply the timeout to the execution of the underlying {@code Future},
   * <em>not</em> to the execution of the transformation function.
   *
   * <p>The primary audience of this method is callers of {@code transform}
   * who don't have a {@code ListenableFuture} available and
   * do not mind repeated, lazy function evaluation.
   *
   * @param input The future to transform
   * @param function A Function to transform the results of the provided future
   *     to the results of the returned future.
   * @return A future that returns the result of the transformation.
   * @since 10.0
   */
  @GwtIncompatible("TODO")
  @CheckReturnValue
  public static <I, O> Future<O> lazyTransform(final Future<I> input,
      final Function<? super I, ? extends O> function) {
    checkNotNull(input);
    checkNotNull(function);
    return new Future<O>() {

      @Override
      public boolean cancel(boolean mayInterruptIfRunning) {
        return input.cancel(mayInterruptIfRunning);
      }

      @Override
      public boolean isCancelled() {
        return input.isCancelled();
      }

      @Override
      public boolean isDone() {
        return input.isDone();
      }

      @Override
      public O get() throws InterruptedException, ExecutionException {
        return applyTransformation(input.get());
      }

      @Override
      public O get(long timeout, TimeUnit unit)
          throws InterruptedException, ExecutionException, TimeoutException {
        return applyTransformation(input.get(timeout, unit));
      }

      private O applyTransformation(I input) throws ExecutionException {
        try {
          return function.apply(input);
        } catch (Throwable t) {
          throw new ExecutionException(t);
        }
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
  private abstract static class AbstractChainingFuture<I, O, F>
      extends AbstractFuture.TrustedFuture<O> implements Runnable {
    // In theory, this field might not be visible to a cancel() call in certain circumstances. For
    // details, see the comments on the fields of TimeoutFuture.
    @Nullable ListenableFuture<? extends I> inputFuture;
    @Nullable F function;

    AbstractChainingFuture(ListenableFuture<? extends I> inputFuture, F function) {
      this.inputFuture = checkNotNull(inputFuture);
      this.function = checkNotNull(function);
    }

    @Override
    public final void run() {
      try {
        ListenableFuture<? extends I> localInputFuture = inputFuture;
        F localFunction = function;
        if (isCancelled() | localInputFuture == null | localFunction == null) {
          return;
        }
        inputFuture = null;
        function = null;

        I sourceResult;
        try {
          sourceResult = getUninterruptibly(localInputFuture);
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
        doTransform(localFunction, sourceResult);
      } catch (UndeclaredThrowableException e) {
        // Set the cause of the exception as this future's exception
        setException(e.getCause());
      } catch (Throwable t) {
        // This exception is irrelevant in this thread, but useful for the
        // client
        setException(t);
      }
    }

    /** Template method for subtypes to actually run the transform. */
    abstract void doTransform(F function, I result) throws Exception;

    @Override final void done() {
      maybePropagateCancellation(inputFuture);
      this.inputFuture = null;
      this.function = null;
    }
  }

  /**
   * A {@link AbstractChainingFuture} that delegates to an {@link AsyncFunction} and
   * {@link #setFuture(ListenableFuture)} to implement {@link #doTransform}.
   */
  private static final class AsyncChainingFuture<I, O>
      extends AbstractChainingFuture<I, O, AsyncFunction<? super I, ? extends O>> {
    AsyncChainingFuture(ListenableFuture<? extends I> inputFuture,
        AsyncFunction<? super I, ? extends O> function) {
      super(inputFuture, function);
    }

    @Override
    void doTransform(AsyncFunction<? super I, ? extends O> function, I input) throws Exception {
      ListenableFuture<? extends O> outputFuture = function.apply(input);
      checkNotNull(outputFuture, "AsyncFunction.apply returned null instead of a Future. "
          + "Did you mean to return immediateFuture(null)?");
      setFuture(outputFuture);
    }
  }

  /**
   * A {@link AbstractChainingFuture} that delegates to a {@link Function} and
   * {@link #set(Object)} to implement {@link #doTransform}.
   */
  private static final class ChainingFuture<I, O>
      extends AbstractChainingFuture<I, O, Function<? super I, ? extends O>> {

    ChainingFuture(ListenableFuture<? extends I> inputFuture,
        Function<? super I, ? extends O> function) {
      super(inputFuture, function);
    }

    @Override
    void doTransform(Function<? super I, ? extends O> function, I input) {
      // TODO(lukes): move the UndeclaredThrowable catch block here?
      set(function.apply(input));
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
   * Creates a new {@code ListenableFuture} whose result is set from the
   * supplied future when it completes.  Cancelling the supplied future
   * will also cancel the returned future, but cancelling the returned
   * future will have no effect on the supplied future.
   *
   * @since 15.0
   */
  @GwtIncompatible("TODO")
  @CheckReturnValue
  public static <V> ListenableFuture<V> nonCancellationPropagating(
      ListenableFuture<V> future) {
    return new NonCancellationPropagatingFuture<V>(future);
  }

  /**
   * A wrapped future that does not propagate cancellation to its delegate.
   */
  @GwtIncompatible("TODO")
  private static final class NonCancellationPropagatingFuture<V>
      extends AbstractFuture.TrustedFuture<V> {
    NonCancellationPropagatingFuture(final ListenableFuture<V> delegate) {
      delegate.addListener(new Runnable() {
        @Override public void run() {
          // This prevents cancellation from propagating because we don't assign delegate until
          // delegate is already done, so calling cancel() on it is a no-op.
          setFuture(delegate);
        }
      }, directExecutor());
    }
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
   * Returns a list of delegate futures that correspond to the futures received in the order
   * that they complete. Delegate futures return the same value or throw the same exception
   * as the corresponding input future returns/throws.
   *
   * <p>Cancelling a delegate future has no effect on any input future, since the delegate future
   * does not correspond to a specific input future until the appropriate number of input
   * futures have completed. At that point, it is too late to cancel the input future.
   * The input future's result, which cannot be stored into the cancelled delegate future,
   * is ignored.
   *
   * @since 17.0
   */
  @Beta
  @GwtIncompatible("TODO")
  @CheckReturnValue
  public static <T> ImmutableList<ListenableFuture<T>> inCompletionOrder(
      Iterable<? extends ListenableFuture<? extends T>> futures) {
    // A CLQ may be overkill here.  We could save some pointers/memory by synchronizing on an
    // ArrayDeque
    final ConcurrentLinkedQueue<SettableFuture<T>> delegates =
        Queues.newConcurrentLinkedQueue();
    ImmutableList.Builder<ListenableFuture<T>> listBuilder = ImmutableList.builder();
    // Using SerializingExecutor here will ensure that each CompletionOrderListener executes
    // atomically and therefore that each returned future is guaranteed to be in completion order.
    // N.B. there are some cases where the use of this executor could have possibly surprising
    // effects when input futures finish at approximately the same time _and_ the output futures
    // have directExecutor listeners. In this situation, the listeners may end up running on a
    // different thread than if they were attached to the corresponding input future.  We believe
    // this to be a negligible cost since:
    // 1. Using the directExecutor implies that your callback is safe to run on any thread.
    // 2. This would likely only be noticeable if you were doing something expensive or blocking on
    //    a directExecutor listener on one of the output futures which is an antipattern anyway.
    SerializingExecutor executor = new SerializingExecutor(directExecutor());
    for (final ListenableFuture<? extends T> future : futures) {
      SettableFuture<T> delegate = SettableFuture.create();
      // Must make sure to add the delegate to the queue first in case the future is already done
      delegates.add(delegate);
      future.addListener(new Runnable() {
        @Override public void run() {
          delegates.remove().setFuture(future);
        }
      }, executor);
      listBuilder.add(delegate);
    }
    return listBuilder.build();
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
   * <p>This overload, which does not accept an executor, uses {@code
   * directExecutor}, a dangerous choice in some cases. See the discussion in
   * the {@link ListenableFuture#addListener ListenableFuture.addListener}
   * documentation.
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
   * <p>When selecting an executor, note that {@code directExecutor} is
   * dangerous in some cases. See the discussion in the {@link
   * ListenableFuture#addListener ListenableFuture.addListener} documentation.
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

  /**
   * Returns the result of {@link Future#get()}, converting most exceptions to a
   * new instance of the given checked exception type. This reduces boilerplate
   * for a common use of {@code Future} in which it is unnecessary to
   * programmatically distinguish between exception types or to extract other
   * information from the exception instance.
   *
   * <p>Exceptions from {@code Future.get} are treated as follows:
   * <ul>
   * <li>Any {@link ExecutionException} has its <i>cause</i> wrapped in an
   *     {@code X} if the cause is a checked exception, an {@link
   *     UncheckedExecutionException} if the cause is a {@code
   *     RuntimeException}, or an {@link ExecutionError} if the cause is an
   *     {@code Error}.
   * <li>Any {@link InterruptedException} is wrapped in an {@code X} (after
   *     restoring the interrupt).
   * <li>Any {@link CancellationException} is propagated untouched, as is any
   *     other {@link RuntimeException} (though {@code get} implementations are
   *     discouraged from throwing such exceptions).
   * </ul>
   *
   * <p>The overall principle is to continue to treat every checked exception as a
   * checked exception, every unchecked exception as an unchecked exception, and
   * every error as an error. In addition, the cause of any {@code
   * ExecutionException} is wrapped in order to ensure that the new stack trace
   * matches that of the current thread.
   *
   * <p>Instances of {@code exceptionClass} are created by choosing an arbitrary
   * public constructor that accepts zero or more arguments, all of type {@code
   * String} or {@code Throwable} (preferring constructors with at least one
   * {@code String}) and calling the constructor via reflection. If the
   * exception did not already have a cause, one is set by calling {@link
   * Throwable#initCause(Throwable)} on it. If no such constructor exists, an
   * {@code IllegalArgumentException} is thrown.
   *
   * @throws X if {@code get} throws any checked exception except for an {@code
   *         ExecutionException} whose cause is not itself a checked exception
   * @throws UncheckedExecutionException if {@code get} throws an {@code
   *         ExecutionException} with a {@code RuntimeException} as its cause
   * @throws ExecutionError if {@code get} throws an {@code ExecutionException}
   *         with an {@code Error} as its cause
   * @throws CancellationException if {@code get} throws a {@code
   *         CancellationException}
   * @throws IllegalArgumentException if {@code exceptionClass} extends {@code
   *         RuntimeException} or does not have a suitable constructor
   * @since 10.0
   * @deprecated Use {@link #getChecked(Future, Class)}. This method will be
   *     removed in Guava release 20.0.
   */
  @Deprecated
  @GwtIncompatible("reflection")
  public static <V, X extends Exception> V get(
      Future<V> future, Class<X> exceptionClass) throws X {
    return getChecked(future, exceptionClass);
  }

  /**
   * Returns the result of {@link Future#get(long, TimeUnit)}, converting most
   * exceptions to a new instance of the given checked exception type. This
   * reduces boilerplate for a common use of {@code Future} in which it is
   * unnecessary to programmatically distinguish between exception types or to
   * extract other information from the exception instance.
   *
   * <p>Exceptions from {@code Future.get} are treated as follows:
   * <ul>
   * <li>Any {@link ExecutionException} has its <i>cause</i> wrapped in an
   *     {@code X} if the cause is a checked exception, an {@link
   *     UncheckedExecutionException} if the cause is a {@code
   *     RuntimeException}, or an {@link ExecutionError} if the cause is an
   *     {@code Error}.
   * <li>Any {@link InterruptedException} is wrapped in an {@code X} (after
   *     restoring the interrupt).
   * <li>Any {@link TimeoutException} is wrapped in an {@code X}.
   * <li>Any {@link CancellationException} is propagated untouched, as is any
   *     other {@link RuntimeException} (though {@code get} implementations are
   *     discouraged from throwing such exceptions).
   * </ul>
   *
   * <p>The overall principle is to continue to treat every checked exception as a
   * checked exception, every unchecked exception as an unchecked exception, and
   * every error as an error. In addition, the cause of any {@code
   * ExecutionException} is wrapped in order to ensure that the new stack trace
   * matches that of the current thread.
   *
   * <p>Instances of {@code exceptionClass} are created by choosing an arbitrary
   * public constructor that accepts zero or more arguments, all of type {@code
   * String} or {@code Throwable} (preferring constructors with at least one
   * {@code String}) and calling the constructor via reflection. If the
   * exception did not already have a cause, one is set by calling {@link
   * Throwable#initCause(Throwable)} on it. If no such constructor exists, an
   * {@code IllegalArgumentException} is thrown.
   *
   * @throws X if {@code get} throws any checked exception except for an {@code
   *         ExecutionException} whose cause is not itself a checked exception
   * @throws UncheckedExecutionException if {@code get} throws an {@code
   *         ExecutionException} with a {@code RuntimeException} as its cause
   * @throws ExecutionError if {@code get} throws an {@code ExecutionException}
   *         with an {@code Error} as its cause
   * @throws CancellationException if {@code get} throws a {@code
   *         CancellationException}
   * @throws IllegalArgumentException if {@code exceptionClass} extends {@code
   *         RuntimeException} or does not have a suitable constructor
   * @since 10.0
   * @deprecated Use {@link #getChecked(Future, Class, long, TimeUnit)}, noting
   *     the change in parameter order. This method will be removed in Guava
   *     release 20.0.
   */
  @Deprecated
  @GwtIncompatible("reflection")
  public static <V, X extends Exception> V get(
      Future<V> future, long timeout, TimeUnit unit, Class<X> exceptionClass)
      throws X {
    return getChecked(future, exceptionClass, timeout, unit);
  }

  /**
   * Returns the result of {@link Future#get()}, converting most exceptions to a
   * new instance of the given checked exception type. This reduces boilerplate
   * for a common use of {@code Future} in which it is unnecessary to
   * programmatically distinguish between exception types or to extract other
   * information from the exception instance.
   *
   * <p>Exceptions from {@code Future.get} are treated as follows:
   * <ul>
   * <li>Any {@link ExecutionException} has its <i>cause</i> wrapped in an
   *     {@code X} if the cause is a checked exception, an {@link
   *     UncheckedExecutionException} if the cause is a {@code
   *     RuntimeException}, or an {@link ExecutionError} if the cause is an
   *     {@code Error}.
   * <li>Any {@link InterruptedException} is wrapped in an {@code X} (after
   *     restoring the interrupt).
   * <li>Any {@link CancellationException} is propagated untouched, as is any
   *     other {@link RuntimeException} (though {@code get} implementations are
   *     discouraged from throwing such exceptions).
   * </ul>
   *
   * <p>The overall principle is to continue to treat every checked exception as a
   * checked exception, every unchecked exception as an unchecked exception, and
   * every error as an error. In addition, the cause of any {@code
   * ExecutionException} is wrapped in order to ensure that the new stack trace
   * matches that of the current thread.
   *
   * <p>Instances of {@code exceptionClass} are created by choosing an arbitrary
   * public constructor that accepts zero or more arguments, all of type {@code
   * String} or {@code Throwable} (preferring constructors with at least one
   * {@code String}) and calling the constructor via reflection. If the
   * exception did not already have a cause, one is set by calling {@link
   * Throwable#initCause(Throwable)} on it. If no such constructor exists, an
   * {@code IllegalArgumentException} is thrown.
   *
   * @throws X if {@code get} throws any checked exception except for an {@code
   *     ExecutionException} whose cause is not itself a checked exception
   * @throws UncheckedExecutionException if {@code get} throws an {@code
   *     ExecutionException} with a {@code RuntimeException} as its cause
   * @throws ExecutionError if {@code get} throws an {@code ExecutionException}
   *     with an {@code Error} as its cause
   * @throws CancellationException if {@code get} throws a {@code
   *     CancellationException}
   * @throws IllegalArgumentException if {@code exceptionClass} extends {@code
   *     RuntimeException} or does not have a suitable constructor
   * @since 19.0 (in 10.0 as {@code get})
   */
  @GwtIncompatible("reflection")
  public static <V, X extends Exception> V getChecked(
      Future<V> future, Class<X> exceptionClass) throws X {
    return FuturesGetChecked.getChecked(future, exceptionClass);
  }

  /**
   * Returns the result of {@link Future#get(long, TimeUnit)}, converting most
   * exceptions to a new instance of the given checked exception type. This
   * reduces boilerplate for a common use of {@code Future} in which it is
   * unnecessary to programmatically distinguish between exception types or to
   * extract other information from the exception instance.
   *
   * <p>Exceptions from {@code Future.get} are treated as follows:
   * <ul>
   * <li>Any {@link ExecutionException} has its <i>cause</i> wrapped in an
   *     {@code X} if the cause is a checked exception, an {@link
   *     UncheckedExecutionException} if the cause is a {@code
   *     RuntimeException}, or an {@link ExecutionError} if the cause is an
   *     {@code Error}.
   * <li>Any {@link InterruptedException} is wrapped in an {@code X} (after
   *     restoring the interrupt).
   * <li>Any {@link TimeoutException} is wrapped in an {@code X}.
   * <li>Any {@link CancellationException} is propagated untouched, as is any
   *     other {@link RuntimeException} (though {@code get} implementations are
   *     discouraged from throwing such exceptions).
   * </ul>
   *
   * <p>The overall principle is to continue to treat every checked exception as a
   * checked exception, every unchecked exception as an unchecked exception, and
   * every error as an error. In addition, the cause of any {@code
   * ExecutionException} is wrapped in order to ensure that the new stack trace
   * matches that of the current thread.
   *
   * <p>Instances of {@code exceptionClass} are created by choosing an arbitrary
   * public constructor that accepts zero or more arguments, all of type {@code
   * String} or {@code Throwable} (preferring constructors with at least one
   * {@code String}) and calling the constructor via reflection. If the
   * exception did not already have a cause, one is set by calling {@link
   * Throwable#initCause(Throwable)} on it. If no such constructor exists, an
   * {@code IllegalArgumentException} is thrown.
   *
   * @throws X if {@code get} throws any checked exception except for an {@code
   *     ExecutionException} whose cause is not itself a checked exception
   * @throws UncheckedExecutionException if {@code get} throws an {@code
   *     ExecutionException} with a {@code RuntimeException} as its cause
   * @throws ExecutionError if {@code get} throws an {@code ExecutionException}
   *     with an {@code Error} as its cause
   * @throws CancellationException if {@code get} throws a {@code
   *     CancellationException}
   * @throws IllegalArgumentException if {@code exceptionClass} extends {@code
   *     RuntimeException} or does not have a suitable constructor
   * @since 19.0 (in 10.0 as {@code get} and with different parameter order)
   */
  @GwtIncompatible("reflection")
  public static <V, X extends Exception> V getChecked(
      Future<V> future, Class<X> exceptionClass, long timeout, TimeUnit unit)
      throws X {
    return FuturesGetChecked.getChecked(future, exceptionClass, timeout, unit);
  }

  /**
   * Returns the result of calling {@link Future#get()} uninterruptibly on a
   * task known not to throw a checked exception. This makes {@code Future} more
   * suitable for lightweight, fast-running tasks that, barring bugs in the
   * code, will not fail. This gives it exception-handling behavior similar to
   * that of {@code ForkJoinTask.join}.
   *
   * <p>Exceptions from {@code Future.get} are treated as follows:
   * <ul>
   * <li>Any {@link ExecutionException} has its <i>cause</i> wrapped in an
   *     {@link UncheckedExecutionException} (if the cause is an {@code
   *     Exception}) or {@link ExecutionError} (if the cause is an {@code
   *     Error}).
   * <li>Any {@link InterruptedException} causes a retry of the {@code get}
   *     call. The interrupt is restored before {@code getUnchecked} returns.
   * <li>Any {@link CancellationException} is propagated untouched. So is any
   *     other {@link RuntimeException} ({@code get} implementations are
   *     discouraged from throwing such exceptions).
   * </ul>
   *
   * <p>The overall principle is to eliminate all checked exceptions: to loop to
   * avoid {@code InterruptedException}, to pass through {@code
   * CancellationException}, and to wrap any exception from the underlying
   * computation in an {@code UncheckedExecutionException} or {@code
   * ExecutionError}.
   *
   * <p>For an uninterruptible {@code get} that preserves other exceptions, see
   * {@link Uninterruptibles#getUninterruptibly(Future)}.
   *
   * @throws UncheckedExecutionException if {@code get} throws an {@code
   *         ExecutionException} with an {@code Exception} as its cause
   * @throws ExecutionError if {@code get} throws an {@code ExecutionException}
   *         with an {@code Error} as its cause
   * @throws CancellationException if {@code get} throws a {@code
   *         CancellationException}
   * @since 10.0
   */
  @GwtIncompatible("TODO")
  public static <V> V getUnchecked(Future<V> future) {
    checkNotNull(future);
    try {
      return getUninterruptibly(future);
    } catch (ExecutionException e) {
      wrapAndThrowUnchecked(e.getCause());
      throw new AssertionError();
    }
  }

  @GwtIncompatible("TODO")
  private static void wrapAndThrowUnchecked(Throwable cause) {
    if (cause instanceof Error) {
      throw new ExecutionError((Error) cause);
    }
    /*
     * It's a non-Error, non-Exception Throwable. From my survey of such
     * classes, I believe that most users intended to extend Exception, so we'll
     * treat it like an Exception.
     */
    throw new UncheckedExecutionException(cause);
  }

  /*
   * Arguably we don't need a timed getUnchecked because any operation slow
   * enough to require a timeout is heavyweight enough to throw a checked
   * exception and therefore be inappropriate to use with getUnchecked. Further,
   * it's not clear that converting the checked TimeoutException to a
   * RuntimeException -- especially to an UncheckedExecutionException, since it
   * wasn't thrown by the computation -- makes sense, and if we don't convert
   * it, the user still has to write a try-catch block.
   *
   * If you think you would use this method, let us know. You might also also
   * look into the Fork-Join framework:
   * http://docs.oracle.com/javase/tutorial/essential/concurrency/forkjoin.html
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

  /**
   * A checked future that uses a function to map from exceptions to the
   * appropriate checked type.
   */
  @GwtIncompatible("TODO")
  private static class MappingCheckedFuture<V, X extends Exception> extends
      AbstractCheckedFuture<V, X> {

    final Function<? super Exception, X> mapper;

    MappingCheckedFuture(ListenableFuture<V> delegate,
        Function<? super Exception, X> mapper) {
      super(delegate);

      this.mapper = checkNotNull(mapper);
    }

    @Override
    protected X mapException(Exception e) {
      return mapper.apply(e);
    }
  }
}
