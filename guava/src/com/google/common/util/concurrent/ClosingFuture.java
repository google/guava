/*
 * Copyright (C) 2017 The Guava Authors
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

import static com.google.common.base.Functions.constant;
import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.Lists.asList;
import static com.google.common.util.concurrent.ClosingFuture.State.CLOSED;
import static com.google.common.util.concurrent.ClosingFuture.State.CLOSING;
import static com.google.common.util.concurrent.ClosingFuture.State.OPEN;
import static com.google.common.util.concurrent.ClosingFuture.State.SUBSUMED;
import static com.google.common.util.concurrent.ClosingFuture.State.WILL_CLOSE;
import static com.google.common.util.concurrent.ClosingFuture.State.WILL_CREATE_VALUE_AND_CLOSER;
import static com.google.common.util.concurrent.Futures.getDone;
import static com.google.common.util.concurrent.Futures.immediateFuture;
import static com.google.common.util.concurrent.Futures.nonCancellationPropagating;
import static com.google.common.util.concurrent.MoreExecutors.directExecutor;
import static java.util.logging.Level.FINER;
import static java.util.logging.Level.SEVERE;
import static java.util.logging.Level.WARNING;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.ClosingFuture.Combiner.AsyncCombiningCallable;
import com.google.common.util.concurrent.ClosingFuture.Combiner.CombiningCallable;
import com.google.common.util.concurrent.Futures.FutureCombiner;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.errorprone.annotations.DoNotMock;
import com.google.j2objc.annotations.RetainedWith;
import java.io.Closeable;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;
import javax.annotation.CheckForNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * A step in a pipeline of an asynchronous computation. When the last step in the computation is
 * complete, some objects captured during the computation are closed.
 *
 * <p>A pipeline of {@code ClosingFuture}s is a tree of steps. Each step represents either an
 * asynchronously-computed intermediate value, or else an exception that indicates the failure or
 * cancellation of the operation so far. The only way to extract the value or exception from a step
 * is by declaring that step to be the last step of the pipeline. Nevertheless, we refer to the
 * "value" of a successful step or the "result" (value or exception) of any step.
 *
 * <ol>
 *   <li>A pipeline starts at its leaf step (or steps), which is created from either a callable
 *       block or a {@link ListenableFuture}.
 *   <li>Each other step is derived from one or more input steps. At each step, zero or more objects
 *       can be captured for later closing.
 *   <li>There is one last step (the root of the tree), from which you can extract the final result
 *       of the computation. After that result is available (or the computation fails), all objects
 *       captured by any of the steps in the pipeline are closed.
 * </ol>
 *
 * <h3>Starting a pipeline</h3>
 *
 * Start a {@code ClosingFuture} pipeline {@linkplain #submit(ClosingCallable, Executor) from a
 * callable block} that may capture objects for later closing. To start a pipeline from a {@link
 * ListenableFuture} that doesn't create resources that should be closed later, you can use {@link
 * #from(ListenableFuture)} instead.
 *
 * <h3>Derived steps</h3>
 *
 * A {@code ClosingFuture} step can be derived from one or more input {@code ClosingFuture} steps in
 * ways similar to {@link FluentFuture}s:
 *
 * <ul>
 *   <li>by transforming the value from a successful input step,
 *   <li>by catching the exception from a failed input step, or
 *   <li>by combining the results of several input steps.
 * </ul>
 *
 * Each derivation can capture the next value or any intermediate objects for later closing.
 *
 * <p>A step can be the input to at most one derived step. Once you transform its value, catch its
 * exception, or combine it with others, you cannot do anything else with it, including declare it
 * to be the last step of the pipeline.
 *
 * <h4>Transforming</h4>
 *
 * To derive the next step by asynchronously applying a function to an input step's value, call
 * {@link #transform(ClosingFunction, Executor)} or {@link #transformAsync(AsyncClosingFunction,
 * Executor)} on the input step.
 *
 * <h4>Catching</h4>
 *
 * To derive the next step from a failed input step, call {@link #catching(Class, ClosingFunction,
 * Executor)} or {@link #catchingAsync(Class, AsyncClosingFunction, Executor)} on the input step.
 *
 * <h4>Combining</h4>
 *
 * To derive a {@code ClosingFuture} from two or more input steps, pass the input steps to {@link
 * #whenAllComplete(Iterable)} or {@link #whenAllSucceed(Iterable)} or its overloads.
 *
 * <h3>Cancelling</h3>
 *
 * Any step in a pipeline can be {@linkplain #cancel(boolean) cancelled}, even after another step
 * has been derived, with the same semantics as cancelling a {@link Future}. In addition, a
 * successfully cancelled step will immediately start closing all objects captured for later closing
 * by it and by its input steps.
 *
 * <h3>Ending a pipeline</h3>
 *
 * Each {@code ClosingFuture} pipeline must be ended. To end a pipeline, decide whether you want to
 * close the captured objects automatically or manually.
 *
 * <h4>Automatically closing</h4>
 *
 * You can extract a {@link Future} that represents the result of the last step in the pipeline by
 * calling {@link #finishToFuture()}. All objects the pipeline has captured for closing will begin
 * to be closed asynchronously <b>after</b> the returned {@code Future} is done: the future
 * completes before closing starts, rather than once it has finished.
 *
 * <pre>{@code
 * FluentFuture<UserName> userName =
 *     ClosingFuture.submit(
 *             closer -> closer.eventuallyClose(database.newTransaction(), closingExecutor),
 *             executor)
 *         .transformAsync((closer, transaction) -> transaction.queryClosingFuture("..."), executor)
 *         .transform((closer, result) -> result.get("userName"), directExecutor())
 *         .catching(DBException.class, e -> "no user", directExecutor())
 *         .finishToFuture();
 * }</pre>
 *
 * In this example, when the {@code userName} {@link Future} is done, the transaction and the query
 * result cursor will both be closed, even if the operation is cancelled or fails.
 *
 * <h4>Manually closing</h4>
 *
 * If you want to close the captured objects manually, after you've used the final result, call
 * {@link #finishToValueAndCloser(ValueAndCloserConsumer, Executor)} to get an object that holds the
 * final result. You then call {@link ValueAndCloser#closeAsync()} to close the captured objects.
 *
 * <pre>{@code
 *     ClosingFuture.submit(
 *             closer -> closer.eventuallyClose(database.newTransaction(), closingExecutor),
 *             executor)
 *     .transformAsync((closer, transaction) -> transaction.queryClosingFuture("..."), executor)
 *     .transform((closer, result) -> result.get("userName"), directExecutor())
 *     .catching(DBException.class, e -> "no user", directExecutor())
 *     .finishToValueAndCloser(
 *         valueAndCloser -> this.userNameValueAndCloser = valueAndCloser, executor);
 *
 * // later
 * try { // get() will throw if the operation failed or was cancelled.
 *   UserName userName = userNameValueAndCloser.get();
 *   // do something with userName
 * } finally {
 *   userNameValueAndCloser.closeAsync();
 * }
 * }</pre>
 *
 * In this example, when {@code userNameValueAndCloser.closeAsync()} is called, the transaction and
 * the query result cursor will both be closed, even if the operation is cancelled or fails.
 *
 * <p>Note that if you don't call {@code closeAsync()}, the captured objects will not be closed. The
 * automatic-closing approach described above is safer.
 *
 * @param <V> the type of the value of this step
 * @since 30.0
 */
// TODO(dpb): Consider reusing one CloseableList for the entire pipeline, modulo combinations.
@DoNotMock("Use ClosingFuture.from(Futures.immediate*Future)")
@ElementTypesAreNonnullByDefault
// TODO(dpb): GWT compatibility.
public final class ClosingFuture<V extends @Nullable Object> {

  private static final Logger logger = Logger.getLogger(ClosingFuture.class.getName());

  /**
   * An object that can capture objects to be closed later, when a {@link ClosingFuture} pipeline is
   * done.
   */
  public static final class DeferredCloser {
    @RetainedWith private final CloseableList list;

    DeferredCloser(CloseableList list) {
      this.list = list;
    }

    /**
     * Captures an object to be closed when a {@link ClosingFuture} pipeline is done.
     *
     * <p>For users of the {@code -jre} flavor of Guava, the object can be any {@code
     * AutoCloseable}. For users of the {@code -android} flavor, the object must be a {@code
     * Closeable}. (For more about the flavors, see <a
     * href="https://github.com/google/guava#adding-guava-to-your-build">Adding Guava to your
     * build</a>.)
     *
     * <p>Be careful when targeting an older SDK than you are building against (most commonly when
     * building for Android): Ensure that any object you pass implements the interface not just in
     * your current SDK version but also at the oldest version you support. For example, <a
     * href="https://developer.android.com/sdk/api_diff/16/">API Level 16</a> is the first version
     * in which {@code Cursor} is {@code Closeable}. To support older versions, pass a wrapper
     * {@code Closeable} with a method reference like {@code cursor::close}.
     *
     * <p>Note that this method is still binary-compatible between flavors because the erasure of
     * its parameter type is {@code Object}, not {@code AutoCloseable} or {@code Closeable}.
     *
     * @param closeable the object to be closed (see notes above)
     * @param closingExecutor the object will be closed on this executor
     * @return the first argument
     */
    @CanIgnoreReturnValue
    @ParametricNullness
    public <C extends @Nullable Object & @Nullable AutoCloseable> C eventuallyClose(
        @ParametricNullness C closeable, Executor closingExecutor) {
      checkNotNull(closingExecutor);
      if (closeable != null) {
        list.add(closeable, closingExecutor);
      }
      return closeable;
    }
  }

  /**
   * An operation that computes a result.
   *
   * @param <V> the type of the result
   */
  @FunctionalInterface
  public interface ClosingCallable<V extends @Nullable Object> {
    /**
     * Computes a result, or throws an exception if unable to do so.
     *
     * <p>Any objects that are passed to {@link DeferredCloser#eventuallyClose(Closeable, Executor)
     * closer.eventuallyClose()} will be closed when the {@link ClosingFuture} pipeline is done (but
     * not before this method completes), even if this method throws or the pipeline is cancelled.
     */
    @ParametricNullness
    V call(DeferredCloser closer) throws Exception;
  }

  /**
   * An operation that computes a {@link ClosingFuture} of a result.
   *
   * @param <V> the type of the result
   * @since 30.1
   */
  @FunctionalInterface
  public interface AsyncClosingCallable<V extends @Nullable Object> {
    /**
     * Computes a result, or throws an exception if unable to do so.
     *
     * <p>Any objects that are passed to {@link DeferredCloser#eventuallyClose(Closeable, Executor)
     * closer.eventuallyClose()} will be closed when the {@link ClosingFuture} pipeline is done (but
     * not before this method completes), even if this method throws or the pipeline is cancelled.
     */
    ClosingFuture<V> call(DeferredCloser closer) throws Exception;
  }

  /**
   * A function from an input to a result.
   *
   * @param <T> the type of the input to the function
   * @param <U> the type of the result of the function
   */
  @FunctionalInterface
  public interface ClosingFunction<T extends @Nullable Object, U extends @Nullable Object> {

    /**
     * Applies this function to an input, or throws an exception if unable to do so.
     *
     * <p>Any objects that are passed to {@link DeferredCloser#eventuallyClose(Closeable, Executor)
     * closer.eventuallyClose()} will be closed when the {@link ClosingFuture} pipeline is done (but
     * not before this method completes), even if this method throws or the pipeline is cancelled.
     */
    @ParametricNullness
    U apply(DeferredCloser closer, @ParametricNullness T input) throws Exception;
  }

  /**
   * A function from an input to a {@link ClosingFuture} of a result.
   *
   * @param <T> the type of the input to the function
   * @param <U> the type of the result of the function
   */
  @FunctionalInterface
  public interface AsyncClosingFunction<T extends @Nullable Object, U extends @Nullable Object> {
    /**
     * Applies this function to an input, or throws an exception if unable to do so.
     *
     * <p>Any objects that are passed to {@link DeferredCloser#eventuallyClose(Closeable, Executor)
     * closer.eventuallyClose()} will be closed when the {@link ClosingFuture} pipeline is done (but
     * not before this method completes), even if this method throws or the pipeline is cancelled.
     */
    ClosingFuture<U> apply(DeferredCloser closer, @ParametricNullness T input) throws Exception;
  }

  /**
   * An object that holds the final result of an asynchronous {@link ClosingFuture} operation and
   * allows the user to close all the closeable objects that were captured during it for later
   * closing.
   *
   * <p>The asynchronous operation will have completed before this object is created.
   *
   * @param <V> the type of the value of a successful operation
   * @see ClosingFuture#finishToValueAndCloser(ValueAndCloserConsumer, Executor)
   */
  public static final class ValueAndCloser<V extends @Nullable Object> {

    private final ClosingFuture<? extends V> closingFuture;

    ValueAndCloser(ClosingFuture<? extends V> closingFuture) {
      this.closingFuture = checkNotNull(closingFuture);
    }

    /**
     * Returns the final value of the associated {@link ClosingFuture}, or throws an exception as
     * {@link Future#get()} would.
     *
     * <p>Because the asynchronous operation has already completed, this method is synchronous and
     * returns immediately.
     *
     * @throws CancellationException if the computation was cancelled
     * @throws ExecutionException if the computation threw an exception
     */
    @ParametricNullness
    public V get() throws ExecutionException {
      return getDone(closingFuture.future);
    }

    /**
     * Starts closing all closeable objects captured during the {@link ClosingFuture}'s asynchronous
     * operation on the {@link Executor}s specified by calls to {@link
     * DeferredCloser#eventuallyClose(Closeable, Executor)}.
     *
     * <p>If any such calls specified {@link MoreExecutors#directExecutor()}, those objects will be
     * closed synchronously.
     *
     * <p>Idempotent: objects will be closed at most once.
     */
    public void closeAsync() {
      closingFuture.close();
    }
  }

  /**
   * Represents an operation that accepts a {@link ValueAndCloser} for the last step in a {@link
   * ClosingFuture} pipeline.
   *
   * @param <V> the type of the final value of a successful pipeline
   * @see ClosingFuture#finishToValueAndCloser(ValueAndCloserConsumer, Executor)
   */
  @FunctionalInterface
  public interface ValueAndCloserConsumer<V extends @Nullable Object> {

    /** Accepts a {@link ValueAndCloser} for the last step in a {@link ClosingFuture} pipeline. */
    void accept(ValueAndCloser<V> valueAndCloser);
  }

  /**
   * Starts a {@link ClosingFuture} pipeline by submitting a callable block to an executor.
   *
   * @throws java.util.concurrent.RejectedExecutionException if the task cannot be scheduled for
   *     execution
   */
  public static <V extends @Nullable Object> ClosingFuture<V> submit(
      ClosingCallable<V> callable, Executor executor) {
    return new ClosingFuture<>(callable, executor);
  }

  /**
   * Starts a {@link ClosingFuture} pipeline by submitting a callable block to an executor.
   *
   * @throws java.util.concurrent.RejectedExecutionException if the task cannot be scheduled for
   *     execution
   * @since 30.1
   */
  public static <V extends @Nullable Object> ClosingFuture<V> submitAsync(
      AsyncClosingCallable<V> callable, Executor executor) {
    return new ClosingFuture<>(callable, executor);
  }

  /**
   * Starts a {@link ClosingFuture} pipeline with a {@link ListenableFuture}.
   *
   * <p>{@code future}'s value will not be closed when the pipeline is done even if {@code V}
   * implements {@link Closeable}. In order to start a pipeline with a value that will be closed
   * when the pipeline is done, use {@link #submit(ClosingCallable, Executor)} instead.
   */
  public static <V extends @Nullable Object> ClosingFuture<V> from(ListenableFuture<V> future) {
    return new ClosingFuture<V>(future);
  }

  /**
   * Starts a {@link ClosingFuture} pipeline with a {@link ListenableFuture}.
   *
   * <p>If {@code future} succeeds, its value will be closed (using {@code closingExecutor)} when
   * the pipeline is done, even if the pipeline is canceled or fails.
   *
   * <p>Cancelling the pipeline will not cancel {@code future}, so that the pipeline can access its
   * value in order to close it.
   *
   * @param future the future to create the {@code ClosingFuture} from. For discussion of the
   *     future's result type {@code C}, see {@link DeferredCloser#eventuallyClose(Closeable,
   *     Executor)}.
   * @param closingExecutor the future's result will be closed on this executor
   * @deprecated Creating {@link Future}s of closeable types is dangerous in general because the
   *     underlying value may never be closed if the {@link Future} is canceled after its operation
   *     begins. Consider replacing code that creates {@link ListenableFuture}s of closeable types,
   *     including those that pass them to this method, with {@link #submit(ClosingCallable,
   *     Executor)} in order to ensure that resources do not leak. Or, to start a pipeline with a
   *     {@link ListenableFuture} that doesn't create values that should be closed, use {@link
   *     ClosingFuture#from}.
   */
  @Deprecated
  public static <C extends @Nullable Object & @Nullable AutoCloseable>
      ClosingFuture<C> eventuallyClosing(
          ListenableFuture<C> future, final Executor closingExecutor) {
    checkNotNull(closingExecutor);
    final ClosingFuture<C> closingFuture = new ClosingFuture<>(nonCancellationPropagating(future));
    Futures.addCallback(
        future,
        new FutureCallback<@Nullable AutoCloseable>() {
          @Override
          public void onSuccess(@CheckForNull AutoCloseable result) {
            closingFuture.closeables.closer.eventuallyClose(result, closingExecutor);
          }

          @Override
          public void onFailure(Throwable t) {}
        },
        directExecutor());
    return closingFuture;
  }

  /**
   * Starts specifying how to combine {@link ClosingFuture}s into a single pipeline.
   *
   * @throws IllegalStateException if a {@code ClosingFuture} has already been derived from any of
   *     the {@code futures}, or if any has already been {@linkplain #finishToFuture() finished}
   */
  public static Combiner whenAllComplete(Iterable<? extends ClosingFuture<?>> futures) {
    return new Combiner(false, futures);
  }

  /**
   * Starts specifying how to combine {@link ClosingFuture}s into a single pipeline.
   *
   * @throws IllegalStateException if a {@code ClosingFuture} has already been derived from any of
   *     the arguments, or if any has already been {@linkplain #finishToFuture() finished}
   */
  public static Combiner whenAllComplete(
      ClosingFuture<?> future1, ClosingFuture<?>... moreFutures) {
    return whenAllComplete(asList(future1, moreFutures));
  }

  /**
   * Starts specifying how to combine {@link ClosingFuture}s into a single pipeline, assuming they
   * all succeed. If any fail, the resulting pipeline will fail.
   *
   * @throws IllegalStateException if a {@code ClosingFuture} has already been derived from any of
   *     the {@code futures}, or if any has already been {@linkplain #finishToFuture() finished}
   */
  public static Combiner whenAllSucceed(Iterable<? extends ClosingFuture<?>> futures) {
    return new Combiner(true, futures);
  }

  /**
   * Starts specifying how to combine two {@link ClosingFuture}s into a single pipeline, assuming
   * they all succeed. If any fail, the resulting pipeline will fail.
   *
   * <p>Calling this method allows you to use lambdas or method references typed with the types of
   * the input {@link ClosingFuture}s.
   *
   * @throws IllegalStateException if a {@code ClosingFuture} has already been derived from any of
   *     the arguments, or if any has already been {@linkplain #finishToFuture() finished}
   */
  public static <V1 extends @Nullable Object, V2 extends @Nullable Object>
      Combiner2<V1, V2> whenAllSucceed(ClosingFuture<V1> future1, ClosingFuture<V2> future2) {
    return new Combiner2<>(future1, future2);
  }

  /**
   * Starts specifying how to combine three {@link ClosingFuture}s into a single pipeline, assuming
   * they all succeed. If any fail, the resulting pipeline will fail.
   *
   * <p>Calling this method allows you to use lambdas or method references typed with the types of
   * the input {@link ClosingFuture}s.
   *
   * @throws IllegalStateException if a {@code ClosingFuture} has already been derived from any of
   *     the arguments, or if any has already been {@linkplain #finishToFuture() finished}
   */
  public static <
          V1 extends @Nullable Object, V2 extends @Nullable Object, V3 extends @Nullable Object>
      Combiner3<V1, V2, V3> whenAllSucceed(
          ClosingFuture<V1> future1, ClosingFuture<V2> future2, ClosingFuture<V3> future3) {
    return new Combiner3<>(future1, future2, future3);
  }

  /**
   * Starts specifying how to combine four {@link ClosingFuture}s into a single pipeline, assuming
   * they all succeed. If any fail, the resulting pipeline will fail.
   *
   * <p>Calling this method allows you to use lambdas or method references typed with the types of
   * the input {@link ClosingFuture}s.
   *
   * @throws IllegalStateException if a {@code ClosingFuture} has already been derived from any of
   *     the arguments, or if any has already been {@linkplain #finishToFuture() finished}
   */
  public static <
          V1 extends @Nullable Object,
          V2 extends @Nullable Object,
          V3 extends @Nullable Object,
          V4 extends @Nullable Object>
      Combiner4<V1, V2, V3, V4> whenAllSucceed(
          ClosingFuture<V1> future1,
          ClosingFuture<V2> future2,
          ClosingFuture<V3> future3,
          ClosingFuture<V4> future4) {
    return new Combiner4<>(future1, future2, future3, future4);
  }

  /**
   * Starts specifying how to combine five {@link ClosingFuture}s into a single pipeline, assuming
   * they all succeed. If any fail, the resulting pipeline will fail.
   *
   * <p>Calling this method allows you to use lambdas or method references typed with the types of
   * the input {@link ClosingFuture}s.
   *
   * @throws IllegalStateException if a {@code ClosingFuture} has already been derived from any of
   *     the arguments, or if any has already been {@linkplain #finishToFuture() finished}
   */
  public static <
          V1 extends @Nullable Object,
          V2 extends @Nullable Object,
          V3 extends @Nullable Object,
          V4 extends @Nullable Object,
          V5 extends @Nullable Object>
      Combiner5<V1, V2, V3, V4, V5> whenAllSucceed(
          ClosingFuture<V1> future1,
          ClosingFuture<V2> future2,
          ClosingFuture<V3> future3,
          ClosingFuture<V4> future4,
          ClosingFuture<V5> future5) {
    return new Combiner5<>(future1, future2, future3, future4, future5);
  }

  /**
   * Starts specifying how to combine {@link ClosingFuture}s into a single pipeline, assuming they
   * all succeed. If any fail, the resulting pipeline will fail.
   *
   * @throws IllegalStateException if a {@code ClosingFuture} has already been derived from any of
   *     the arguments, or if any has already been {@linkplain #finishToFuture() finished}
   */
  public static Combiner whenAllSucceed(
      ClosingFuture<?> future1,
      ClosingFuture<?> future2,
      ClosingFuture<?> future3,
      ClosingFuture<?> future4,
      ClosingFuture<?> future5,
      ClosingFuture<?> future6,
      ClosingFuture<?>... moreFutures) {
    return whenAllSucceed(
        FluentIterable.of(future1, future2, future3, future4, future5, future6)
            .append(moreFutures));
  }

  private final AtomicReference<State> state = new AtomicReference<>(OPEN);
  private final CloseableList closeables = new CloseableList();
  private final FluentFuture<V> future;

  private ClosingFuture(ListenableFuture<V> future) {
    this.future = FluentFuture.from(future);
  }

  private ClosingFuture(final ClosingCallable<V> callable, Executor executor) {
    checkNotNull(callable);
    TrustedListenableFutureTask<V> task =
        TrustedListenableFutureTask.create(
            new Callable<V>() {
              @Override
              @ParametricNullness
              public V call() throws Exception {
                return callable.call(closeables.closer);
              }

              @Override
              public String toString() {
                return callable.toString();
              }
            });
    executor.execute(task);
    this.future = task;
  }

  private ClosingFuture(final AsyncClosingCallable<V> callable, Executor executor) {
    checkNotNull(callable);
    TrustedListenableFutureTask<V> task =
        TrustedListenableFutureTask.create(
            new AsyncCallable<V>() {
              @Override
              public ListenableFuture<V> call() throws Exception {
                CloseableList newCloseables = new CloseableList();
                try {
                  ClosingFuture<V> closingFuture = callable.call(newCloseables.closer);
                  closingFuture.becomeSubsumedInto(closeables);
                  return closingFuture.future;
                } finally {
                  closeables.add(newCloseables, directExecutor());
                }
              }

              @Override
              public String toString() {
                return callable.toString();
              }
            });
    executor.execute(task);
    this.future = task;
  }

  /**
   * Returns a future that finishes when this step does. Calling {@code get()} on the returned
   * future returns {@code null} if the step is successful or throws the same exception that would
   * be thrown by calling {@code finishToFuture().get()} if this were the last step. Calling {@code
   * cancel()} on the returned future has no effect on the {@code ClosingFuture} pipeline.
   *
   * <p>{@code statusFuture} differs from most methods on {@code ClosingFuture}: You can make calls
   * to {@code statusFuture} <i>in addition to</i> the call you make to {@link #finishToFuture()} or
   * a derivation method <i>on the same instance</i>. This is important because calling {@code
   * statusFuture} alone does not provide a way to close the pipeline.
   */
  public ListenableFuture<?> statusFuture() {
    return nonCancellationPropagating(future.transform(constant(null), directExecutor()));
  }

  /**
   * Returns a new {@code ClosingFuture} pipeline step derived from this one by applying a function
   * to its value. The function can use a {@link DeferredCloser} to capture objects to be closed
   * when the pipeline is done.
   *
   * <p>If this {@code ClosingFuture} fails, the function will not be called, and the derived {@code
   * ClosingFuture} will be equivalent to this one.
   *
   * <p>If the function throws an exception, that exception is used as the result of the derived
   * {@code ClosingFuture}.
   *
   * <p>Example usage:
   *
   * <pre>{@code
   * ClosingFuture<List<Row>> rowsFuture =
   *     queryFuture.transform((closer, result) -> result.getRows(), executor);
   * }</pre>
   *
   * <p>When selecting an executor, note that {@code directExecutor} is dangerous in some cases. See
   * the discussion in the {@link ListenableFuture#addListener} documentation. All its warnings
   * about heavyweight listeners are also applicable to heavyweight functions passed to this method.
   *
   * <p>After calling this method, you may not call {@link #finishToFuture()}, {@link
   * #finishToValueAndCloser(ValueAndCloserConsumer, Executor)}, or any other derivation method on
   * this {@code ClosingFuture}.
   *
   * @param function transforms the value of this step to the value of the derived step
   * @param executor executor to run the function in
   * @return the derived step
   * @throws IllegalStateException if a {@code ClosingFuture} has already been derived from this
   *     one, or if this {@code ClosingFuture} has already been {@linkplain #finishToFuture()
   *     finished}
   */
  public <U extends @Nullable Object> ClosingFuture<U> transform(
      final ClosingFunction<? super V, U> function, Executor executor) {
    checkNotNull(function);
    AsyncFunction<V, U> applyFunction =
        new AsyncFunction<V, U>() {
          @Override
          public ListenableFuture<U> apply(V input) throws Exception {
            return closeables.applyClosingFunction(function, input);
          }

          @Override
          public String toString() {
            return function.toString();
          }
        };
    // TODO(dpb): Switch to future.transformSync when that exists (passing a throwing function).
    return derive(future.transformAsync(applyFunction, executor));
  }

  /**
   * Returns a new {@code ClosingFuture} pipeline step derived from this one by applying a function
   * that returns a {@code ClosingFuture} to its value. The function can use a {@link
   * DeferredCloser} to capture objects to be closed when the pipeline is done (other than those
   * captured by the returned {@link ClosingFuture}).
   *
   * <p>If this {@code ClosingFuture} succeeds, the derived one will be equivalent to the one
   * returned by the function.
   *
   * <p>If this {@code ClosingFuture} fails, the function will not be called, and the derived {@code
   * ClosingFuture} will be equivalent to this one.
   *
   * <p>If the function throws an exception, that exception is used as the result of the derived
   * {@code ClosingFuture}. But if the exception is thrown after the function creates a {@code
   * ClosingFuture}, then none of the closeable objects in that {@code ClosingFuture} will be
   * closed.
   *
   * <p>Usage guidelines for this method:
   *
   * <ul>
   *   <li>Use this method only when calling an API that returns a {@link ListenableFuture} or a
   *       {@code ClosingFuture}. If possible, prefer calling {@link #transform(ClosingFunction,
   *       Executor)} instead, with a function that returns the next value directly.
   *   <li>Call {@link DeferredCloser#eventuallyClose(Closeable, Executor) closer.eventuallyClose()}
   *       for every closeable object this step creates in order to capture it for later closing.
   *   <li>Return a {@code ClosingFuture}. To turn a {@link ListenableFuture} into a {@code
   *       ClosingFuture} call {@link #from(ListenableFuture)}.
   *   <li>In case this step doesn't create new closeables, you can adapt an API that returns a
   *       {@link ListenableFuture} to return a {@code ClosingFuture} by wrapping it with a call to
   *       {@link #withoutCloser(AsyncFunction)}
   * </ul>
   *
   * <p>Example usage:
   *
   * <pre>{@code
   * // Result.getRowsClosingFuture() returns a ClosingFuture.
   * ClosingFuture<List<Row>> rowsFuture =
   *     queryFuture.transformAsync((closer, result) -> result.getRowsClosingFuture(), executor);
   *
   * // Result.writeRowsToOutputStreamFuture() returns a ListenableFuture that resolves to the
   * // number of written rows. openOutputFile() returns a FileOutputStream (which implements
   * // Closeable).
   * ClosingFuture<Integer> rowsFuture2 =
   *     queryFuture.transformAsync(
   *         (closer, result) -> {
   *           FileOutputStream fos = closer.eventuallyClose(openOutputFile(), closingExecutor);
   *           return ClosingFuture.from(result.writeRowsToOutputStreamFuture(fos));
   *      },
   *      executor);
   *
   * // Result.getRowsFuture() returns a ListenableFuture (no new closeables are created).
   * ClosingFuture<List<Row>> rowsFuture3 =
   *     queryFuture.transformAsync(withoutCloser(Result::getRowsFuture), executor);
   *
   * }</pre>
   *
   * <p>When selecting an executor, note that {@code directExecutor} is dangerous in some cases. See
   * the discussion in the {@link ListenableFuture#addListener} documentation. All its warnings
   * about heavyweight listeners are also applicable to heavyweight functions passed to this method.
   * (Specifically, {@code directExecutor} functions should avoid heavyweight operations inside
   * {@code AsyncClosingFunction.apply}. Any heavyweight operations should occur in other threads
   * responsible for completing the returned {@code ClosingFuture}.)
   *
   * <p>After calling this method, you may not call {@link #finishToFuture()}, {@link
   * #finishToValueAndCloser(ValueAndCloserConsumer, Executor)}, or any other derivation method on
   * this {@code ClosingFuture}.
   *
   * @param function transforms the value of this step to a {@code ClosingFuture} with the value of
   *     the derived step
   * @param executor executor to run the function in
   * @return the derived step
   * @throws IllegalStateException if a {@code ClosingFuture} has already been derived from this
   *     one, or if this {@code ClosingFuture} has already been {@linkplain #finishToFuture()
   *     finished}
   */
  public <U extends @Nullable Object> ClosingFuture<U> transformAsync(
      final AsyncClosingFunction<? super V, U> function, Executor executor) {
    checkNotNull(function);
    AsyncFunction<V, U> applyFunction =
        new AsyncFunction<V, U>() {
          @Override
          public ListenableFuture<U> apply(V input) throws Exception {
            return closeables.applyAsyncClosingFunction(function, input);
          }

          @Override
          public String toString() {
            return function.toString();
          }
        };
    return derive(future.transformAsync(applyFunction, executor));
  }

  /**
   * Returns an {@link AsyncClosingFunction} that applies an {@link AsyncFunction} to an input,
   * ignoring the DeferredCloser and returning a {@code ClosingFuture} derived from the returned
   * {@link ListenableFuture}.
   *
   * <p>Use this method to pass a transformation to {@link #transformAsync(AsyncClosingFunction,
   * Executor)} or to {@link #catchingAsync(Class, AsyncClosingFunction, Executor)} as long as it
   * meets these conditions:
   *
   * <ul>
   *   <li>It does not need to capture any {@link Closeable} objects by calling {@link
   *       DeferredCloser#eventuallyClose(Closeable, Executor)}.
   *   <li>It returns a {@link ListenableFuture}.
   * </ul>
   *
   * <p>Example usage:
   *
   * <pre>{@code
   * // Result.getRowsFuture() returns a ListenableFuture.
   * ClosingFuture<List<Row>> rowsFuture =
   *     queryFuture.transformAsync(withoutCloser(Result::getRowsFuture), executor);
   * }</pre>
   *
   * @param function transforms the value of a {@code ClosingFuture} step to a {@link
   *     ListenableFuture} with the value of a derived step
   */
  public static <V extends @Nullable Object, U extends @Nullable Object>
      AsyncClosingFunction<V, U> withoutCloser(final AsyncFunction<V, U> function) {
    checkNotNull(function);
    return new AsyncClosingFunction<V, U>() {
      @Override
      public ClosingFuture<U> apply(DeferredCloser closer, V input) throws Exception {
        return ClosingFuture.from(function.apply(input));
      }
    };
  }

  /**
   * Returns a new {@code ClosingFuture} pipeline step derived from this one by applying a function
   * to its exception if it is an instance of a given exception type. The function can use a {@link
   * DeferredCloser} to capture objects to be closed when the pipeline is done.
   *
   * <p>If this {@code ClosingFuture} succeeds or fails with a different exception type, the
   * function will not be called, and the derived {@code ClosingFuture} will be equivalent to this
   * one.
   *
   * <p>If the function throws an exception, that exception is used as the result of the derived
   * {@code ClosingFuture}.
   *
   * <p>Example usage:
   *
   * <pre>{@code
   * ClosingFuture<QueryResult> queryFuture =
   *     queryFuture.catching(
   *         QueryException.class, (closer, x) -> Query.emptyQueryResult(), executor);
   * }</pre>
   *
   * <p>When selecting an executor, note that {@code directExecutor} is dangerous in some cases. See
   * the discussion in the {@link ListenableFuture#addListener} documentation. All its warnings
   * about heavyweight listeners are also applicable to heavyweight functions passed to this method.
   *
   * <p>After calling this method, you may not call {@link #finishToFuture()}, {@link
   * #finishToValueAndCloser(ValueAndCloserConsumer, Executor)}, or any other derivation method on
   * this {@code ClosingFuture}.
   *
   * @param exceptionType the exception type that triggers use of {@code fallback}. The exception
   *     type is matched against this step's exception. "This step's exception" means the cause of
   *     the {@link ExecutionException} thrown by {@link Future#get()} on the {@link Future}
   *     underlying this step or, if {@code get()} throws a different kind of exception, that
   *     exception itself. To avoid hiding bugs and other unrecoverable errors, callers should
   *     prefer more specific types, avoiding {@code Throwable.class} in particular.
   * @param fallback the function to be called if this step fails with the expected exception type.
   *     The function's argument is this step's exception. "This step's exception" means the cause
   *     of the {@link ExecutionException} thrown by {@link Future#get()} on the {@link Future}
   *     underlying this step or, if {@code get()} throws a different kind of exception, that
   *     exception itself.
   * @param executor the executor that runs {@code fallback} if the input fails
   */
  public <X extends Throwable> ClosingFuture<V> catching(
      Class<X> exceptionType, ClosingFunction<? super X, ? extends V> fallback, Executor executor) {
    return catchingMoreGeneric(exceptionType, fallback, executor);
  }

  // Avoids generic type capture inconsistency problems where |? extends V| is incompatible with V.
  private <X extends Throwable, W extends V> ClosingFuture<V> catchingMoreGeneric(
      Class<X> exceptionType, final ClosingFunction<? super X, W> fallback, Executor executor) {
    checkNotNull(fallback);
    AsyncFunction<X, W> applyFallback =
        new AsyncFunction<X, W>() {
          @Override
          public ListenableFuture<W> apply(X exception) throws Exception {
            return closeables.applyClosingFunction(fallback, exception);
          }

          @Override
          public String toString() {
            return fallback.toString();
          }
        };
    // TODO(dpb): Switch to future.catchingSync when that exists (passing a throwing function).
    return derive(future.catchingAsync(exceptionType, applyFallback, executor));
  }

  /**
   * Returns a new {@code ClosingFuture} pipeline step derived from this one by applying a function
   * that returns a {@code ClosingFuture} to its exception if it is an instance of a given exception
   * type. The function can use a {@link DeferredCloser} to capture objects to be closed when the
   * pipeline is done (other than those captured by the returned {@link ClosingFuture}).
   *
   * <p>If this {@code ClosingFuture} fails with an exception of the given type, the derived {@code
   * ClosingFuture} will be equivalent to the one returned by the function.
   *
   * <p>If this {@code ClosingFuture} succeeds or fails with a different exception type, the
   * function will not be called, and the derived {@code ClosingFuture} will be equivalent to this
   * one.
   *
   * <p>If the function throws an exception, that exception is used as the result of the derived
   * {@code ClosingFuture}. But if the exception is thrown after the function creates a {@code
   * ClosingFuture}, then none of the closeable objects in that {@code ClosingFuture} will be
   * closed.
   *
   * <p>Usage guidelines for this method:
   *
   * <ul>
   *   <li>Use this method only when calling an API that returns a {@link ListenableFuture} or a
   *       {@code ClosingFuture}. If possible, prefer calling {@link #catching(Class,
   *       ClosingFunction, Executor)} instead, with a function that returns the next value
   *       directly.
   *   <li>Call {@link DeferredCloser#eventuallyClose(Closeable, Executor) closer.eventuallyClose()}
   *       for every closeable object this step creates in order to capture it for later closing.
   *   <li>Return a {@code ClosingFuture}. To turn a {@link ListenableFuture} into a {@code
   *       ClosingFuture} call {@link #from(ListenableFuture)}.
   *   <li>In case this step doesn't create new closeables, you can adapt an API that returns a
   *       {@link ListenableFuture} to return a {@code ClosingFuture} by wrapping it with a call to
   *       {@link #withoutCloser(AsyncFunction)}
   * </ul>
   *
   * <p>Example usage:
   *
   * <pre>{@code
   * // Fall back to a secondary input stream in case of IOException.
   * ClosingFuture<InputStream> inputFuture =
   *     firstInputFuture.catchingAsync(
   *         IOException.class, (closer, x) -> secondaryInputStreamClosingFuture(), executor);
   * }
   * }</pre>
   *
   * <p>When selecting an executor, note that {@code directExecutor} is dangerous in some cases. See
   * the discussion in the {@link ListenableFuture#addListener} documentation. All its warnings
   * about heavyweight listeners are also applicable to heavyweight functions passed to this method.
   * (Specifically, {@code directExecutor} functions should avoid heavyweight operations inside
   * {@code AsyncClosingFunction.apply}. Any heavyweight operations should occur in other threads
   * responsible for completing the returned {@code ClosingFuture}.)
   *
   * <p>After calling this method, you may not call {@link #finishToFuture()}, {@link
   * #finishToValueAndCloser(ValueAndCloserConsumer, Executor)}, or any other derivation method on
   * this {@code ClosingFuture}.
   *
   * @param exceptionType the exception type that triggers use of {@code fallback}. The exception
   *     type is matched against this step's exception. "This step's exception" means the cause of
   *     the {@link ExecutionException} thrown by {@link Future#get()} on the {@link Future}
   *     underlying this step or, if {@code get()} throws a different kind of exception, that
   *     exception itself. To avoid hiding bugs and other unrecoverable errors, callers should
   *     prefer more specific types, avoiding {@code Throwable.class} in particular.
   * @param fallback the function to be called if this step fails with the expected exception type.
   *     The function's argument is this step's exception. "This step's exception" means the cause
   *     of the {@link ExecutionException} thrown by {@link Future#get()} on the {@link Future}
   *     underlying this step or, if {@code get()} throws a different kind of exception, that
   *     exception itself.
   * @param executor the executor that runs {@code fallback} if the input fails
   */
  // TODO(dpb): Should this do something special if the function throws CancellationException or
  // ExecutionException?
  public <X extends Throwable> ClosingFuture<V> catchingAsync(
      Class<X> exceptionType,
      AsyncClosingFunction<? super X, ? extends V> fallback,
      Executor executor) {
    return catchingAsyncMoreGeneric(exceptionType, fallback, executor);
  }

  // Avoids generic type capture inconsistency problems where |? extends V| is incompatible with V.
  private <X extends Throwable, W extends V> ClosingFuture<V> catchingAsyncMoreGeneric(
      Class<X> exceptionType,
      final AsyncClosingFunction<? super X, W> fallback,
      Executor executor) {
    checkNotNull(fallback);
    AsyncFunction<X, W> asyncFunction =
        new AsyncFunction<X, W>() {
          @Override
          public ListenableFuture<W> apply(X exception) throws Exception {
            return closeables.applyAsyncClosingFunction(fallback, exception);
          }

          @Override
          public String toString() {
            return fallback.toString();
          }
        };
    return derive(future.catchingAsync(exceptionType, asyncFunction, executor));
  }

  /**
   * Marks this step as the last step in the {@code ClosingFuture} pipeline.
   *
   * <p>The returned {@link Future} is completed when the pipeline's computation completes, or when
   * the pipeline is cancelled.
   *
   * <p>All objects the pipeline has captured for closing will begin to be closed asynchronously
   * <b>after</b> the returned {@code Future} is done: the future completes before closing starts,
   * rather than once it has finished.
   *
   * <p>After calling this method, you may not call {@link
   * #finishToValueAndCloser(ValueAndCloserConsumer, Executor)}, this method, or any other
   * derivation method on this {@code ClosingFuture}.
   *
   * @return a {@link Future} that represents the final value or exception of the pipeline
   */
  public FluentFuture<V> finishToFuture() {
    if (compareAndUpdateState(OPEN, WILL_CLOSE)) {
      logger.log(FINER, "will close {0}", this);
      future.addListener(
          new Runnable() {
            @Override
            public void run() {
              checkAndUpdateState(WILL_CLOSE, CLOSING);
              close();
              checkAndUpdateState(CLOSING, CLOSED);
            }
          },
          directExecutor());
    } else {
      switch (state.get()) {
        case SUBSUMED:
          throw new IllegalStateException(
              "Cannot call finishToFuture() after deriving another step");

        case WILL_CREATE_VALUE_AND_CLOSER:
          throw new IllegalStateException(
              "Cannot call finishToFuture() after calling finishToValueAndCloser()");

        case WILL_CLOSE:
        case CLOSING:
        case CLOSED:
          throw new IllegalStateException("Cannot call finishToFuture() twice");

        case OPEN:
          throw new AssertionError();
      }
    }
    return future;
  }

  /**
   * Marks this step as the last step in the {@code ClosingFuture} pipeline. When this step is done,
   * {@code receiver} will be called with an object that contains the result of the operation. The
   * receiver can store the {@link ValueAndCloser} outside the receiver for later synchronous use.
   *
   * <p>After calling this method, you may not call {@link #finishToFuture()}, this method again, or
   * any other derivation method on this {@code ClosingFuture}.
   *
   * @param consumer a callback whose method will be called (using {@code executor}) when this
   *     operation is done
   */
  public void finishToValueAndCloser(
      final ValueAndCloserConsumer<? super V> consumer, Executor executor) {
    checkNotNull(consumer);
    if (!compareAndUpdateState(OPEN, WILL_CREATE_VALUE_AND_CLOSER)) {
      switch (state.get()) {
        case SUBSUMED:
          throw new IllegalStateException(
              "Cannot call finishToValueAndCloser() after deriving another step");

        case WILL_CLOSE:
        case CLOSING:
        case CLOSED:
          throw new IllegalStateException(
              "Cannot call finishToValueAndCloser() after calling finishToFuture()");

        case WILL_CREATE_VALUE_AND_CLOSER:
          throw new IllegalStateException("Cannot call finishToValueAndCloser() twice");

        case OPEN:
          break;
      }
      throw new AssertionError(state);
    }
    future.addListener(
        new Runnable() {
          @Override
          public void run() {
            provideValueAndCloser(consumer, ClosingFuture.this);
          }
        },
        executor);
  }

  private static <C extends @Nullable Object, V extends C> void provideValueAndCloser(
      ValueAndCloserConsumer<C> consumer, ClosingFuture<V> closingFuture) {
    consumer.accept(new ValueAndCloser<C>(closingFuture));
  }

  /**
   * Attempts to cancel execution of this step. This attempt will fail if the step has already
   * completed, has already been cancelled, or could not be cancelled for some other reason. If
   * successful, and this step has not started when {@code cancel} is called, this step should never
   * run.
   *
   * <p>If successful, causes the objects captured by this step (if already started) and its input
   * step(s) for later closing to be closed on their respective {@link Executor}s. If any such calls
   * specified {@link MoreExecutors#directExecutor()}, those objects will be closed synchronously.
   *
   * @param mayInterruptIfRunning {@code true} if the thread executing this task should be
   *     interrupted; otherwise, in-progress tasks are allowed to complete, but the step will be
   *     cancelled regardless
   * @return {@code false} if the step could not be cancelled, typically because it has already
   *     completed normally; {@code true} otherwise
   */
  @CanIgnoreReturnValue
  public boolean cancel(boolean mayInterruptIfRunning) {
    logger.log(FINER, "cancelling {0}", this);
    boolean cancelled = future.cancel(mayInterruptIfRunning);
    if (cancelled) {
      close();
    }
    return cancelled;
  }

  private void close() {
    logger.log(FINER, "closing {0}", this);
    closeables.close();
  }

  private <U extends @Nullable Object> ClosingFuture<U> derive(FluentFuture<U> future) {
    ClosingFuture<U> derived = new ClosingFuture<>(future);
    becomeSubsumedInto(derived.closeables);
    return derived;
  }

  private void becomeSubsumedInto(CloseableList otherCloseables) {
    checkAndUpdateState(OPEN, SUBSUMED);
    otherCloseables.add(closeables, directExecutor());
  }

  /**
   * An object that can return the value of the {@link ClosingFuture}s that are passed to {@link
   * #whenAllComplete(Iterable)} or {@link #whenAllSucceed(Iterable)}.
   *
   * <p>Only for use by a {@link CombiningCallable} or {@link AsyncCombiningCallable} object.
   */
  public static final class Peeker {
    private final ImmutableList<ClosingFuture<?>> futures;
    private volatile boolean beingCalled;

    private Peeker(ImmutableList<ClosingFuture<?>> futures) {
      this.futures = checkNotNull(futures);
    }

    /**
     * Returns the value of {@code closingFuture}.
     *
     * @throws ExecutionException if {@code closingFuture} is a failed step
     * @throws CancellationException if the {@code closingFuture}'s future was cancelled
     * @throws IllegalArgumentException if {@code closingFuture} is not one of the futures passed to
     *     {@link #whenAllComplete(Iterable)} or {@link #whenAllComplete(Iterable)}
     * @throws IllegalStateException if called outside of a call to {@link
     *     CombiningCallable#call(DeferredCloser, Peeker)} or {@link
     *     AsyncCombiningCallable#call(DeferredCloser, Peeker)}
     */
    @ParametricNullness
    public final <D extends @Nullable Object> D getDone(ClosingFuture<D> closingFuture)
        throws ExecutionException {
      checkState(beingCalled);
      checkArgument(futures.contains(closingFuture));
      return Futures.getDone(closingFuture.future);
    }

    @ParametricNullness
    private <V extends @Nullable Object> V call(
        CombiningCallable<V> combiner, CloseableList closeables) throws Exception {
      beingCalled = true;
      CloseableList newCloseables = new CloseableList();
      try {
        return combiner.call(newCloseables.closer, this);
      } finally {
        closeables.add(newCloseables, directExecutor());
        beingCalled = false;
      }
    }

    private <V extends @Nullable Object> FluentFuture<V> callAsync(
        AsyncCombiningCallable<V> combiner, CloseableList closeables) throws Exception {
      beingCalled = true;
      CloseableList newCloseables = new CloseableList();
      try {
        ClosingFuture<V> closingFuture = combiner.call(newCloseables.closer, this);
        closingFuture.becomeSubsumedInto(closeables);
        return closingFuture.future;
      } finally {
        closeables.add(newCloseables, directExecutor());
        beingCalled = false;
      }
    }
  }

  /**
   * A builder of a {@link ClosingFuture} step that is derived from more than one input step.
   *
   * <p>See {@link #whenAllComplete(Iterable)} and {@link #whenAllSucceed(Iterable)} for how to
   * instantiate this class.
   *
   * <p>Example:
   *
   * <pre>{@code
   * final ClosingFuture<BufferedReader> file1ReaderFuture = ...;
   * final ClosingFuture<BufferedReader> file2ReaderFuture = ...;
   * ListenableFuture<Integer> numberOfDifferentLines =
   *       ClosingFuture.whenAllSucceed(file1ReaderFuture, file2ReaderFuture)
   *           .call(
   *               (closer, peeker) -> {
   *                 BufferedReader file1Reader = peeker.getDone(file1ReaderFuture);
   *                 BufferedReader file2Reader = peeker.getDone(file2ReaderFuture);
   *                 return countDifferentLines(file1Reader, file2Reader);
   *               },
   *               executor)
   *           .closing(executor);
   * }</pre>
   */
  // TODO(cpovirk): Use simple name instead of fully qualified after we stop building with JDK 8.
  @com.google.errorprone.annotations.DoNotMock(
      "Use ClosingFuture.whenAllSucceed() or .whenAllComplete() instead.")
  public static class Combiner {

    private final CloseableList closeables = new CloseableList();

    /**
     * An operation that returns a result and may throw an exception.
     *
     * @param <V> the type of the result
     */
    @FunctionalInterface
    public interface CombiningCallable<V extends @Nullable Object> {
      /**
       * Computes a result, or throws an exception if unable to do so.
       *
       * <p>Any objects that are passed to {@link DeferredCloser#eventuallyClose(Closeable,
       * Executor) closer.eventuallyClose()} will be closed when the {@link ClosingFuture} pipeline
       * is done (but not before this method completes), even if this method throws or the pipeline
       * is cancelled.
       *
       * @param peeker used to get the value of any of the input futures
       */
      @ParametricNullness
      V call(DeferredCloser closer, Peeker peeker) throws Exception;
    }

    /**
     * An operation that returns a {@link ClosingFuture} result and may throw an exception.
     *
     * @param <V> the type of the result
     */
    @FunctionalInterface
    public interface AsyncCombiningCallable<V extends @Nullable Object> {
      /**
       * Computes a {@link ClosingFuture} result, or throws an exception if unable to do so.
       *
       * <p>Any objects that are passed to {@link DeferredCloser#eventuallyClose(Closeable,
       * Executor) closer.eventuallyClose()} will be closed when the {@link ClosingFuture} pipeline
       * is done (but not before this method completes), even if this method throws or the pipeline
       * is cancelled.
       *
       * @param peeker used to get the value of any of the input futures
       */
      ClosingFuture<V> call(DeferredCloser closer, Peeker peeker) throws Exception;
    }

    private final boolean allMustSucceed;
    protected final ImmutableList<ClosingFuture<?>> inputs;

    private Combiner(boolean allMustSucceed, Iterable<? extends ClosingFuture<?>> inputs) {
      this.allMustSucceed = allMustSucceed;
      this.inputs = ImmutableList.copyOf(inputs);
      for (ClosingFuture<?> input : inputs) {
        input.becomeSubsumedInto(closeables);
      }
    }

    /**
     * Returns a new {@code ClosingFuture} pipeline step derived from the inputs by applying a
     * combining function to their values. The function can use a {@link DeferredCloser} to capture
     * objects to be closed when the pipeline is done.
     *
     * <p>If this combiner was returned by a {@link #whenAllSucceed} method and any of the inputs
     * fail, so will the returned step.
     *
     * <p>If the combiningCallable throws a {@code CancellationException}, the pipeline will be
     * cancelled.
     *
     * <p>If the combiningCallable throws an {@code ExecutionException}, the cause of the thrown
     * {@code ExecutionException} will be extracted and used as the failure of the derived step.
     */
    public <V extends @Nullable Object> ClosingFuture<V> call(
        final CombiningCallable<V> combiningCallable, Executor executor) {
      Callable<V> callable =
          new Callable<V>() {
            @Override
            @ParametricNullness
            public V call() throws Exception {
              return new Peeker(inputs).call(combiningCallable, closeables);
            }

            @Override
            public String toString() {
              return combiningCallable.toString();
            }
          };
      ClosingFuture<V> derived = new ClosingFuture<>(futureCombiner().call(callable, executor));
      derived.closeables.add(closeables, directExecutor());
      return derived;
    }

    /**
     * Returns a new {@code ClosingFuture} pipeline step derived from the inputs by applying a
     * {@code ClosingFuture}-returning function to their values. The function can use a {@link
     * DeferredCloser} to capture objects to be closed when the pipeline is done (other than those
     * captured by the returned {@link ClosingFuture}).
     *
     * <p>If this combiner was returned by a {@link #whenAllSucceed} method and any of the inputs
     * fail, so will the returned step.
     *
     * <p>If the combiningCallable throws a {@code CancellationException}, the pipeline will be
     * cancelled.
     *
     * <p>If the combiningCallable throws an {@code ExecutionException}, the cause of the thrown
     * {@code ExecutionException} will be extracted and used as the failure of the derived step.
     *
     * <p>If the combiningCallable throws any other exception, it will be used as the failure of the
     * derived step.
     *
     * <p>If an exception is thrown after the combiningCallable creates a {@code ClosingFuture},
     * then none of the closeable objects in that {@code ClosingFuture} will be closed.
     *
     * <p>Usage guidelines for this method:
     *
     * <ul>
     *   <li>Use this method only when calling an API that returns a {@link ListenableFuture} or a
     *       {@code ClosingFuture}. If possible, prefer calling {@link #call(CombiningCallable,
     *       Executor)} instead, with a function that returns the next value directly.
     *   <li>Call {@link DeferredCloser#eventuallyClose(Closeable, Executor)
     *       closer.eventuallyClose()} for every closeable object this step creates in order to
     *       capture it for later closing.
     *   <li>Return a {@code ClosingFuture}. To turn a {@link ListenableFuture} into a {@code
     *       ClosingFuture} call {@link #from(ListenableFuture)}.
     * </ul>
     *
     * <p>The same warnings about doing heavyweight operations within {@link
     * ClosingFuture#transformAsync(AsyncClosingFunction, Executor)} apply here.
     */
    public <V extends @Nullable Object> ClosingFuture<V> callAsync(
        final AsyncCombiningCallable<V> combiningCallable, Executor executor) {
      AsyncCallable<V> asyncCallable =
          new AsyncCallable<V>() {
            @Override
            public ListenableFuture<V> call() throws Exception {
              return new Peeker(inputs).callAsync(combiningCallable, closeables);
            }

            @Override
            public String toString() {
              return combiningCallable.toString();
            }
          };
      ClosingFuture<V> derived =
          new ClosingFuture<>(futureCombiner().callAsync(asyncCallable, executor));
      derived.closeables.add(closeables, directExecutor());
      return derived;
    }

    private FutureCombiner<@Nullable Object> futureCombiner() {
      return allMustSucceed
          ? Futures.whenAllSucceed(inputFutures())
          : Futures.whenAllComplete(inputFutures());
    }

    private static final Function<ClosingFuture<?>, FluentFuture<?>> INNER_FUTURE =
        new Function<ClosingFuture<?>, FluentFuture<?>>() {
          @Override
          public FluentFuture<?> apply(ClosingFuture<?> future) {
            return future.future;
          }
        };

    private ImmutableList<FluentFuture<?>> inputFutures() {
      return FluentIterable.from(inputs).transform(INNER_FUTURE).toList();
    }
  }

  /**
   * A generic {@link Combiner} that lets you use a lambda or method reference to combine two {@link
   * ClosingFuture}s. Use {@link #whenAllSucceed(ClosingFuture, ClosingFuture)} to start this
   * combination.
   *
   * @param <V1> the type returned by the first future
   * @param <V2> the type returned by the second future
   */
  public static final class Combiner2<V1 extends @Nullable Object, V2 extends @Nullable Object>
      extends Combiner {

    /**
     * A function that returns a value when applied to the values of the two futures passed to
     * {@link #whenAllSucceed(ClosingFuture, ClosingFuture)}.
     *
     * @param <V1> the type returned by the first future
     * @param <V2> the type returned by the second future
     * @param <U> the type returned by the function
     */
    @FunctionalInterface
    public interface ClosingFunction2<
        V1 extends @Nullable Object, V2 extends @Nullable Object, U extends @Nullable Object> {

      /**
       * Applies this function to two inputs, or throws an exception if unable to do so.
       *
       * <p>Any objects that are passed to {@link DeferredCloser#eventuallyClose(Closeable,
       * Executor) closer.eventuallyClose()} will be closed when the {@link ClosingFuture} pipeline
       * is done (but not before this method completes), even if this method throws or the pipeline
       * is cancelled.
       */
      @ParametricNullness
      U apply(DeferredCloser closer, @ParametricNullness V1 value1, @ParametricNullness V2 value2)
          throws Exception;
    }

    /**
     * A function that returns a {@link ClosingFuture} when applied to the values of the two futures
     * passed to {@link #whenAllSucceed(ClosingFuture, ClosingFuture)}.
     *
     * @param <V1> the type returned by the first future
     * @param <V2> the type returned by the second future
     * @param <U> the type returned by the function
     */
    @FunctionalInterface
    public interface AsyncClosingFunction2<
        V1 extends @Nullable Object, V2 extends @Nullable Object, U extends @Nullable Object> {

      /**
       * Applies this function to two inputs, or throws an exception if unable to do so.
       *
       * <p>Any objects that are passed to {@link DeferredCloser#eventuallyClose(Closeable,
       * Executor) closer.eventuallyClose()} will be closed when the {@link ClosingFuture} pipeline
       * is done (but not before this method completes), even if this method throws or the pipeline
       * is cancelled.
       */
      ClosingFuture<U> apply(
          DeferredCloser closer, @ParametricNullness V1 value1, @ParametricNullness V2 value2)
          throws Exception;
    }

    private final ClosingFuture<V1> future1;
    private final ClosingFuture<V2> future2;

    private Combiner2(ClosingFuture<V1> future1, ClosingFuture<V2> future2) {
      super(true, ImmutableList.of(future1, future2));
      this.future1 = future1;
      this.future2 = future2;
    }

    /**
     * Returns a new {@code ClosingFuture} pipeline step derived from the inputs by applying a
     * combining function to their values. The function can use a {@link DeferredCloser} to capture
     * objects to be closed when the pipeline is done.
     *
     * <p>If this combiner was returned by {@link #whenAllSucceed(ClosingFuture, ClosingFuture)} and
     * any of the inputs fail, so will the returned step.
     *
     * <p>If the function throws a {@code CancellationException}, the pipeline will be cancelled.
     *
     * <p>If the function throws an {@code ExecutionException}, the cause of the thrown {@code
     * ExecutionException} will be extracted and used as the failure of the derived step.
     */
    public <U extends @Nullable Object> ClosingFuture<U> call(
        final ClosingFunction2<V1, V2, U> function, Executor executor) {
      return call(
          new CombiningCallable<U>() {
            @Override
            @ParametricNullness
            public U call(DeferredCloser closer, Peeker peeker) throws Exception {
              return function.apply(closer, peeker.getDone(future1), peeker.getDone(future2));
            }

            @Override
            public String toString() {
              return function.toString();
            }
          },
          executor);
    }

    /**
     * Returns a new {@code ClosingFuture} pipeline step derived from the inputs by applying a
     * {@code ClosingFuture}-returning function to their values. The function can use a {@link
     * DeferredCloser} to capture objects to be closed when the pipeline is done (other than those
     * captured by the returned {@link ClosingFuture}).
     *
     * <p>If this combiner was returned by {@link #whenAllSucceed(ClosingFuture, ClosingFuture)} and
     * any of the inputs fail, so will the returned step.
     *
     * <p>If the function throws a {@code CancellationException}, the pipeline will be cancelled.
     *
     * <p>If the function throws an {@code ExecutionException}, the cause of the thrown {@code
     * ExecutionException} will be extracted and used as the failure of the derived step.
     *
     * <p>If the function throws any other exception, it will be used as the failure of the derived
     * step.
     *
     * <p>If an exception is thrown after the function creates a {@code ClosingFuture}, then none of
     * the closeable objects in that {@code ClosingFuture} will be closed.
     *
     * <p>Usage guidelines for this method:
     *
     * <ul>
     *   <li>Use this method only when calling an API that returns a {@link ListenableFuture} or a
     *       {@code ClosingFuture}. If possible, prefer calling {@link #call(CombiningCallable,
     *       Executor)} instead, with a function that returns the next value directly.
     *   <li>Call {@link DeferredCloser#eventuallyClose(Closeable, Executor)
     *       closer.eventuallyClose()} for every closeable object this step creates in order to
     *       capture it for later closing.
     *   <li>Return a {@code ClosingFuture}. To turn a {@link ListenableFuture} into a {@code
     *       ClosingFuture} call {@link #from(ListenableFuture)}.
     * </ul>
     *
     * <p>The same warnings about doing heavyweight operations within {@link
     * ClosingFuture#transformAsync(AsyncClosingFunction, Executor)} apply here.
     */
    public <U extends @Nullable Object> ClosingFuture<U> callAsync(
        final AsyncClosingFunction2<V1, V2, U> function, Executor executor) {
      return callAsync(
          new AsyncCombiningCallable<U>() {
            @Override
            public ClosingFuture<U> call(DeferredCloser closer, Peeker peeker) throws Exception {
              return function.apply(closer, peeker.getDone(future1), peeker.getDone(future2));
            }

            @Override
            public String toString() {
              return function.toString();
            }
          },
          executor);
    }
  }

  /**
   * A generic {@link Combiner} that lets you use a lambda or method reference to combine three
   * {@link ClosingFuture}s. Use {@link #whenAllSucceed(ClosingFuture, ClosingFuture,
   * ClosingFuture)} to start this combination.
   *
   * @param <V1> the type returned by the first future
   * @param <V2> the type returned by the second future
   * @param <V3> the type returned by the third future
   */
  public static final class Combiner3<
          V1 extends @Nullable Object, V2 extends @Nullable Object, V3 extends @Nullable Object>
      extends Combiner {
    /**
     * A function that returns a value when applied to the values of the three futures passed to
     * {@link #whenAllSucceed(ClosingFuture, ClosingFuture, ClosingFuture)}.
     *
     * @param <V1> the type returned by the first future
     * @param <V2> the type returned by the second future
     * @param <V3> the type returned by the third future
     * @param <U> the type returned by the function
     */
    @FunctionalInterface
    public interface ClosingFunction3<
        V1 extends @Nullable Object,
        V2 extends @Nullable Object,
        V3 extends @Nullable Object,
        U extends @Nullable Object> {
      /**
       * Applies this function to three inputs, or throws an exception if unable to do so.
       *
       * <p>Any objects that are passed to {@link DeferredCloser#eventuallyClose(Closeable,
       * Executor) closer.eventuallyClose()} will be closed when the {@link ClosingFuture} pipeline
       * is done (but not before this method completes), even if this method throws or the pipeline
       * is cancelled.
       */
      @ParametricNullness
      U apply(
          DeferredCloser closer,
          @ParametricNullness V1 value1,
          @ParametricNullness V2 value2,
          @ParametricNullness V3 value3)
          throws Exception;
    }

    /**
     * A function that returns a {@link ClosingFuture} when applied to the values of the three
     * futures passed to {@link #whenAllSucceed(ClosingFuture, ClosingFuture, ClosingFuture)}.
     *
     * @param <V1> the type returned by the first future
     * @param <V2> the type returned by the second future
     * @param <V3> the type returned by the third future
     * @param <U> the type returned by the function
     */
    @FunctionalInterface
    public interface AsyncClosingFunction3<
        V1 extends @Nullable Object,
        V2 extends @Nullable Object,
        V3 extends @Nullable Object,
        U extends @Nullable Object> {
      /**
       * Applies this function to three inputs, or throws an exception if unable to do so.
       *
       * <p>Any objects that are passed to {@link DeferredCloser#eventuallyClose(Closeable,
       * Executor) closer.eventuallyClose()} will be closed when the {@link ClosingFuture} pipeline
       * is done (but not before this method completes), even if this method throws or the pipeline
       * is cancelled.
       */
      ClosingFuture<U> apply(
          DeferredCloser closer,
          @ParametricNullness V1 value1,
          @ParametricNullness V2 value2,
          @ParametricNullness V3 value3)
          throws Exception;
    }

    private final ClosingFuture<V1> future1;
    private final ClosingFuture<V2> future2;
    private final ClosingFuture<V3> future3;

    private Combiner3(
        ClosingFuture<V1> future1, ClosingFuture<V2> future2, ClosingFuture<V3> future3) {
      super(true, ImmutableList.of(future1, future2, future3));
      this.future1 = future1;
      this.future2 = future2;
      this.future3 = future3;
    }

    /**
     * Returns a new {@code ClosingFuture} pipeline step derived from the inputs by applying a
     * combining function to their values. The function can use a {@link DeferredCloser} to capture
     * objects to be closed when the pipeline is done.
     *
     * <p>If this combiner was returned by {@link #whenAllSucceed(ClosingFuture, ClosingFuture,
     * ClosingFuture)} and any of the inputs fail, so will the returned step.
     *
     * <p>If the function throws a {@code CancellationException}, the pipeline will be cancelled.
     *
     * <p>If the function throws an {@code ExecutionException}, the cause of the thrown {@code
     * ExecutionException} will be extracted and used as the failure of the derived step.
     */
    public <U extends @Nullable Object> ClosingFuture<U> call(
        final ClosingFunction3<V1, V2, V3, U> function, Executor executor) {
      return call(
          new CombiningCallable<U>() {
            @Override
            @ParametricNullness
            public U call(DeferredCloser closer, Peeker peeker) throws Exception {
              return function.apply(
                  closer,
                  peeker.getDone(future1),
                  peeker.getDone(future2),
                  peeker.getDone(future3));
            }

            @Override
            public String toString() {
              return function.toString();
            }
          },
          executor);
    }

    /**
     * Returns a new {@code ClosingFuture} pipeline step derived from the inputs by applying a
     * {@code ClosingFuture}-returning function to their values. The function can use a {@link
     * DeferredCloser} to capture objects to be closed when the pipeline is done (other than those
     * captured by the returned {@link ClosingFuture}).
     *
     * <p>If this combiner was returned by {@link #whenAllSucceed(ClosingFuture, ClosingFuture,
     * ClosingFuture)} and any of the inputs fail, so will the returned step.
     *
     * <p>If the function throws a {@code CancellationException}, the pipeline will be cancelled.
     *
     * <p>If the function throws an {@code ExecutionException}, the cause of the thrown {@code
     * ExecutionException} will be extracted and used as the failure of the derived step.
     *
     * <p>If the function throws any other exception, it will be used as the failure of the derived
     * step.
     *
     * <p>If an exception is thrown after the function creates a {@code ClosingFuture}, then none of
     * the closeable objects in that {@code ClosingFuture} will be closed.
     *
     * <p>Usage guidelines for this method:
     *
     * <ul>
     *   <li>Use this method only when calling an API that returns a {@link ListenableFuture} or a
     *       {@code ClosingFuture}. If possible, prefer calling {@link #call(CombiningCallable,
     *       Executor)} instead, with a function that returns the next value directly.
     *   <li>Call {@link DeferredCloser#eventuallyClose(Closeable, Executor)
     *       closer.eventuallyClose()} for every closeable object this step creates in order to
     *       capture it for later closing.
     *   <li>Return a {@code ClosingFuture}. To turn a {@link ListenableFuture} into a {@code
     *       ClosingFuture} call {@link #from(ListenableFuture)}.
     * </ul>
     *
     * <p>The same warnings about doing heavyweight operations within {@link
     * ClosingFuture#transformAsync(AsyncClosingFunction, Executor)} apply here.
     */
    public <U extends @Nullable Object> ClosingFuture<U> callAsync(
        final AsyncClosingFunction3<V1, V2, V3, U> function, Executor executor) {
      return callAsync(
          new AsyncCombiningCallable<U>() {
            @Override
            public ClosingFuture<U> call(DeferredCloser closer, Peeker peeker) throws Exception {
              return function.apply(
                  closer,
                  peeker.getDone(future1),
                  peeker.getDone(future2),
                  peeker.getDone(future3));
            }

            @Override
            public String toString() {
              return function.toString();
            }
          },
          executor);
    }
  }

  /**
   * A generic {@link Combiner} that lets you use a lambda or method reference to combine four
   * {@link ClosingFuture}s. Use {@link #whenAllSucceed(ClosingFuture, ClosingFuture, ClosingFuture,
   * ClosingFuture)} to start this combination.
   *
   * @param <V1> the type returned by the first future
   * @param <V2> the type returned by the second future
   * @param <V3> the type returned by the third future
   * @param <V4> the type returned by the fourth future
   */
  public static final class Combiner4<
          V1 extends @Nullable Object,
          V2 extends @Nullable Object,
          V3 extends @Nullable Object,
          V4 extends @Nullable Object>
      extends Combiner {
    /**
     * A function that returns a value when applied to the values of the four futures passed to
     * {@link #whenAllSucceed(ClosingFuture, ClosingFuture, ClosingFuture, ClosingFuture)}.
     *
     * @param <V1> the type returned by the first future
     * @param <V2> the type returned by the second future
     * @param <V3> the type returned by the third future
     * @param <V4> the type returned by the fourth future
     * @param <U> the type returned by the function
     */
    @FunctionalInterface
    public interface ClosingFunction4<
        V1 extends @Nullable Object,
        V2 extends @Nullable Object,
        V3 extends @Nullable Object,
        V4 extends @Nullable Object,
        U extends @Nullable Object> {
      /**
       * Applies this function to four inputs, or throws an exception if unable to do so.
       *
       * <p>Any objects that are passed to {@link DeferredCloser#eventuallyClose(Closeable,
       * Executor) closer.eventuallyClose()} will be closed when the {@link ClosingFuture} pipeline
       * is done (but not before this method completes), even if this method throws or the pipeline
       * is cancelled.
       */
      @ParametricNullness
      U apply(
          DeferredCloser closer,
          @ParametricNullness V1 value1,
          @ParametricNullness V2 value2,
          @ParametricNullness V3 value3,
          @ParametricNullness V4 value4)
          throws Exception;
    }

    /**
     * A function that returns a {@link ClosingFuture} when applied to the values of the four
     * futures passed to {@link #whenAllSucceed(ClosingFuture, ClosingFuture, ClosingFuture,
     * ClosingFuture)}.
     *
     * @param <V1> the type returned by the first future
     * @param <V2> the type returned by the second future
     * @param <V3> the type returned by the third future
     * @param <V4> the type returned by the fourth future
     * @param <U> the type returned by the function
     */
    @FunctionalInterface
    public interface AsyncClosingFunction4<
        V1 extends @Nullable Object,
        V2 extends @Nullable Object,
        V3 extends @Nullable Object,
        V4 extends @Nullable Object,
        U extends @Nullable Object> {
      /**
       * Applies this function to four inputs, or throws an exception if unable to do so.
       *
       * <p>Any objects that are passed to {@link DeferredCloser#eventuallyClose(Closeable,
       * Executor) closer.eventuallyClose()} will be closed when the {@link ClosingFuture} pipeline
       * is done (but not before this method completes), even if this method throws or the pipeline
       * is cancelled.
       */
      ClosingFuture<U> apply(
          DeferredCloser closer,
          @ParametricNullness V1 value1,
          @ParametricNullness V2 value2,
          @ParametricNullness V3 value3,
          @ParametricNullness V4 value4)
          throws Exception;
    }

    private final ClosingFuture<V1> future1;
    private final ClosingFuture<V2> future2;
    private final ClosingFuture<V3> future3;
    private final ClosingFuture<V4> future4;

    private Combiner4(
        ClosingFuture<V1> future1,
        ClosingFuture<V2> future2,
        ClosingFuture<V3> future3,
        ClosingFuture<V4> future4) {
      super(true, ImmutableList.of(future1, future2, future3, future4));
      this.future1 = future1;
      this.future2 = future2;
      this.future3 = future3;
      this.future4 = future4;
    }

    /**
     * Returns a new {@code ClosingFuture} pipeline step derived from the inputs by applying a
     * combining function to their values. The function can use a {@link DeferredCloser} to capture
     * objects to be closed when the pipeline is done.
     *
     * <p>If this combiner was returned by {@link #whenAllSucceed(ClosingFuture, ClosingFuture,
     * ClosingFuture, ClosingFuture)} and any of the inputs fail, so will the returned step.
     *
     * <p>If the function throws a {@code CancellationException}, the pipeline will be cancelled.
     *
     * <p>If the function throws an {@code ExecutionException}, the cause of the thrown {@code
     * ExecutionException} will be extracted and used as the failure of the derived step.
     */
    public <U extends @Nullable Object> ClosingFuture<U> call(
        final ClosingFunction4<V1, V2, V3, V4, U> function, Executor executor) {
      return call(
          new CombiningCallable<U>() {
            @Override
            @ParametricNullness
            public U call(DeferredCloser closer, Peeker peeker) throws Exception {
              return function.apply(
                  closer,
                  peeker.getDone(future1),
                  peeker.getDone(future2),
                  peeker.getDone(future3),
                  peeker.getDone(future4));
            }

            @Override
            public String toString() {
              return function.toString();
            }
          },
          executor);
    }

    /**
     * Returns a new {@code ClosingFuture} pipeline step derived from the inputs by applying a
     * {@code ClosingFuture}-returning function to their values. The function can use a {@link
     * DeferredCloser} to capture objects to be closed when the pipeline is done (other than those
     * captured by the returned {@link ClosingFuture}).
     *
     * <p>If this combiner was returned by {@link #whenAllSucceed(ClosingFuture, ClosingFuture,
     * ClosingFuture, ClosingFuture)} and any of the inputs fail, so will the returned step.
     *
     * <p>If the function throws a {@code CancellationException}, the pipeline will be cancelled.
     *
     * <p>If the function throws an {@code ExecutionException}, the cause of the thrown {@code
     * ExecutionException} will be extracted and used as the failure of the derived step.
     *
     * <p>If the function throws any other exception, it will be used as the failure of the derived
     * step.
     *
     * <p>If an exception is thrown after the function creates a {@code ClosingFuture}, then none of
     * the closeable objects in that {@code ClosingFuture} will be closed.
     *
     * <p>Usage guidelines for this method:
     *
     * <ul>
     *   <li>Use this method only when calling an API that returns a {@link ListenableFuture} or a
     *       {@code ClosingFuture}. If possible, prefer calling {@link #call(CombiningCallable,
     *       Executor)} instead, with a function that returns the next value directly.
     *   <li>Call {@link DeferredCloser#eventuallyClose(Closeable, Executor)
     *       closer.eventuallyClose()} for every closeable object this step creates in order to
     *       capture it for later closing.
     *   <li>Return a {@code ClosingFuture}. To turn a {@link ListenableFuture} into a {@code
     *       ClosingFuture} call {@link #from(ListenableFuture)}.
     * </ul>
     *
     * <p>The same warnings about doing heavyweight operations within {@link
     * ClosingFuture#transformAsync(AsyncClosingFunction, Executor)} apply here.
     */
    public <U extends @Nullable Object> ClosingFuture<U> callAsync(
        final AsyncClosingFunction4<V1, V2, V3, V4, U> function, Executor executor) {
      return callAsync(
          new AsyncCombiningCallable<U>() {
            @Override
            public ClosingFuture<U> call(DeferredCloser closer, Peeker peeker) throws Exception {
              return function.apply(
                  closer,
                  peeker.getDone(future1),
                  peeker.getDone(future2),
                  peeker.getDone(future3),
                  peeker.getDone(future4));
            }

            @Override
            public String toString() {
              return function.toString();
            }
          },
          executor);
    }
  }

  /**
   * A generic {@link Combiner} that lets you use a lambda or method reference to combine five
   * {@link ClosingFuture}s. Use {@link #whenAllSucceed(ClosingFuture, ClosingFuture, ClosingFuture,
   * ClosingFuture, ClosingFuture)} to start this combination.
   *
   * @param <V1> the type returned by the first future
   * @param <V2> the type returned by the second future
   * @param <V3> the type returned by the third future
   * @param <V4> the type returned by the fourth future
   * @param <V5> the type returned by the fifth future
   */
  public static final class Combiner5<
          V1 extends @Nullable Object,
          V2 extends @Nullable Object,
          V3 extends @Nullable Object,
          V4 extends @Nullable Object,
          V5 extends @Nullable Object>
      extends Combiner {
    /**
     * A function that returns a value when applied to the values of the five futures passed to
     * {@link #whenAllSucceed(ClosingFuture, ClosingFuture, ClosingFuture, ClosingFuture,
     * ClosingFuture)}.
     *
     * @param <V1> the type returned by the first future
     * @param <V2> the type returned by the second future
     * @param <V3> the type returned by the third future
     * @param <V4> the type returned by the fourth future
     * @param <V5> the type returned by the fifth future
     * @param <U> the type returned by the function
     */
    @FunctionalInterface
    public interface ClosingFunction5<
        V1 extends @Nullable Object,
        V2 extends @Nullable Object,
        V3 extends @Nullable Object,
        V4 extends @Nullable Object,
        V5 extends @Nullable Object,
        U extends @Nullable Object> {
      /**
       * Applies this function to five inputs, or throws an exception if unable to do so.
       *
       * <p>Any objects that are passed to {@link DeferredCloser#eventuallyClose(Closeable,
       * Executor) closer.eventuallyClose()} will be closed when the {@link ClosingFuture} pipeline
       * is done (but not before this method completes), even if this method throws or the pipeline
       * is cancelled.
       */
      @ParametricNullness
      U apply(
          DeferredCloser closer,
          @ParametricNullness V1 value1,
          @ParametricNullness V2 value2,
          @ParametricNullness V3 value3,
          @ParametricNullness V4 value4,
          @ParametricNullness V5 value5)
          throws Exception;
    }

    /**
     * A function that returns a {@link ClosingFuture} when applied to the values of the five
     * futures passed to {@link #whenAllSucceed(ClosingFuture, ClosingFuture, ClosingFuture,
     * ClosingFuture, ClosingFuture)}.
     *
     * @param <V1> the type returned by the first future
     * @param <V2> the type returned by the second future
     * @param <V3> the type returned by the third future
     * @param <V4> the type returned by the fourth future
     * @param <V5> the type returned by the fifth future
     * @param <U> the type returned by the function
     */
    @FunctionalInterface
    public interface AsyncClosingFunction5<
        V1 extends @Nullable Object,
        V2 extends @Nullable Object,
        V3 extends @Nullable Object,
        V4 extends @Nullable Object,
        V5 extends @Nullable Object,
        U extends @Nullable Object> {
      /**
       * Applies this function to five inputs, or throws an exception if unable to do so.
       *
       * <p>Any objects that are passed to {@link DeferredCloser#eventuallyClose(Closeable,
       * Executor) closer.eventuallyClose()} will be closed when the {@link ClosingFuture} pipeline
       * is done (but not before this method completes), even if this method throws or the pipeline
       * is cancelled.
       */
      ClosingFuture<U> apply(
          DeferredCloser closer,
          @ParametricNullness V1 value1,
          @ParametricNullness V2 value2,
          @ParametricNullness V3 value3,
          @ParametricNullness V4 value4,
          @ParametricNullness V5 value5)
          throws Exception;
    }

    private final ClosingFuture<V1> future1;
    private final ClosingFuture<V2> future2;
    private final ClosingFuture<V3> future3;
    private final ClosingFuture<V4> future4;
    private final ClosingFuture<V5> future5;

    private Combiner5(
        ClosingFuture<V1> future1,
        ClosingFuture<V2> future2,
        ClosingFuture<V3> future3,
        ClosingFuture<V4> future4,
        ClosingFuture<V5> future5) {
      super(true, ImmutableList.of(future1, future2, future3, future4, future5));
      this.future1 = future1;
      this.future2 = future2;
      this.future3 = future3;
      this.future4 = future4;
      this.future5 = future5;
    }

    /**
     * Returns a new {@code ClosingFuture} pipeline step derived from the inputs by applying a
     * combining function to their values. The function can use a {@link DeferredCloser} to capture
     * objects to be closed when the pipeline is done.
     *
     * <p>If this combiner was returned by {@link #whenAllSucceed(ClosingFuture, ClosingFuture,
     * ClosingFuture, ClosingFuture, ClosingFuture)} and any of the inputs fail, so will the
     * returned step.
     *
     * <p>If the function throws a {@code CancellationException}, the pipeline will be cancelled.
     *
     * <p>If the function throws an {@code ExecutionException}, the cause of the thrown {@code
     * ExecutionException} will be extracted and used as the failure of the derived step.
     */
    public <U extends @Nullable Object> ClosingFuture<U> call(
        final ClosingFunction5<V1, V2, V3, V4, V5, U> function, Executor executor) {
      return call(
          new CombiningCallable<U>() {
            @Override
            @ParametricNullness
            public U call(DeferredCloser closer, Peeker peeker) throws Exception {
              return function.apply(
                  closer,
                  peeker.getDone(future1),
                  peeker.getDone(future2),
                  peeker.getDone(future3),
                  peeker.getDone(future4),
                  peeker.getDone(future5));
            }

            @Override
            public String toString() {
              return function.toString();
            }
          },
          executor);
    }

    /**
     * Returns a new {@code ClosingFuture} pipeline step derived from the inputs by applying a
     * {@code ClosingFuture}-returning function to their values. The function can use a {@link
     * DeferredCloser} to capture objects to be closed when the pipeline is done (other than those
     * captured by the returned {@link ClosingFuture}).
     *
     * <p>If this combiner was returned by {@link #whenAllSucceed(ClosingFuture, ClosingFuture,
     * ClosingFuture, ClosingFuture, ClosingFuture)} and any of the inputs fail, so will the
     * returned step.
     *
     * <p>If the function throws a {@code CancellationException}, the pipeline will be cancelled.
     *
     * <p>If the function throws an {@code ExecutionException}, the cause of the thrown {@code
     * ExecutionException} will be extracted and used as the failure of the derived step.
     *
     * <p>If the function throws any other exception, it will be used as the failure of the derived
     * step.
     *
     * <p>If an exception is thrown after the function creates a {@code ClosingFuture}, then none of
     * the closeable objects in that {@code ClosingFuture} will be closed.
     *
     * <p>Usage guidelines for this method:
     *
     * <ul>
     *   <li>Use this method only when calling an API that returns a {@link ListenableFuture} or a
     *       {@code ClosingFuture}. If possible, prefer calling {@link #call(CombiningCallable,
     *       Executor)} instead, with a function that returns the next value directly.
     *   <li>Call {@link DeferredCloser#eventuallyClose(Closeable, Executor)
     *       closer.eventuallyClose()} for every closeable object this step creates in order to
     *       capture it for later closing.
     *   <li>Return a {@code ClosingFuture}. To turn a {@link ListenableFuture} into a {@code
     *       ClosingFuture} call {@link #from(ListenableFuture)}.
     * </ul>
     *
     * <p>The same warnings about doing heavyweight operations within {@link
     * ClosingFuture#transformAsync(AsyncClosingFunction, Executor)} apply here.
     */
    public <U extends @Nullable Object> ClosingFuture<U> callAsync(
        final AsyncClosingFunction5<V1, V2, V3, V4, V5, U> function, Executor executor) {
      return callAsync(
          new AsyncCombiningCallable<U>() {
            @Override
            public ClosingFuture<U> call(DeferredCloser closer, Peeker peeker) throws Exception {
              return function.apply(
                  closer,
                  peeker.getDone(future1),
                  peeker.getDone(future2),
                  peeker.getDone(future3),
                  peeker.getDone(future4),
                  peeker.getDone(future5));
            }

            @Override
            public String toString() {
              return function.toString();
            }
          },
          executor);
    }
  }

  @Override
  public String toString() {
    // TODO(dpb): Better toString, in the style of Futures.transform etc.
    return toStringHelper(this).add("state", state.get()).addValue(future).toString();
  }

  @Override
  protected void finalize() {
    if (state.get().equals(OPEN)) {
      logger.log(SEVERE, "Uh oh! An open ClosingFuture has leaked and will close: {0}", this);
      FluentFuture<V> unused = finishToFuture();
    }
  }

  private static void closeQuietly(@CheckForNull final AutoCloseable closeable, Executor executor) {
    if (closeable == null) {
      return;
    }
    try {
      executor.execute(
          new Runnable() {
            @Override
            public void run() {
              try {
                closeable.close();
              } catch (Exception e) {
                logger.log(WARNING, "thrown by close()", e);
              }
            }
          });
    } catch (RejectedExecutionException e) {
      if (logger.isLoggable(WARNING)) {
        logger.log(
            WARNING, String.format("while submitting close to %s; will close inline", executor), e);
      }
      closeQuietly(closeable, directExecutor());
    }
  }

  private void checkAndUpdateState(State oldState, State newState) {
    checkState(
        compareAndUpdateState(oldState, newState),
        "Expected state to be %s, but it was %s",
        oldState,
        newState);
  }

  private boolean compareAndUpdateState(State oldState, State newState) {
    return state.compareAndSet(oldState, newState);
  }

  // TODO(dpb): Should we use a pair of ArrayLists instead of an IdentityHashMap?
  private static final class CloseableList extends IdentityHashMap<AutoCloseable, Executor>
      implements Closeable {
    private final DeferredCloser closer = new DeferredCloser(this);
    private volatile boolean closed;
    @CheckForNull private volatile CountDownLatch whenClosed;

    <V extends @Nullable Object, U extends @Nullable Object>
        ListenableFuture<U> applyClosingFunction(
            ClosingFunction<? super V, U> transformation, @ParametricNullness V input)
            throws Exception {
      // TODO(dpb): Consider ways to defer closing without creating a separate CloseableList.
      CloseableList newCloseables = new CloseableList();
      try {
        return immediateFuture(transformation.apply(newCloseables.closer, input));
      } finally {
        add(newCloseables, directExecutor());
      }
    }

    <V extends @Nullable Object, U extends @Nullable Object>
        FluentFuture<U> applyAsyncClosingFunction(
            AsyncClosingFunction<V, U> transformation, @ParametricNullness V input)
            throws Exception {
      // TODO(dpb): Consider ways to defer closing without creating a separate CloseableList.
      CloseableList newCloseables = new CloseableList();
      try {
        ClosingFuture<U> closingFuture = transformation.apply(newCloseables.closer, input);
        closingFuture.becomeSubsumedInto(newCloseables);
        return closingFuture.future;
      } finally {
        add(newCloseables, directExecutor());
      }
    }

    @Override
    public void close() {
      if (closed) {
        return;
      }
      synchronized (this) {
        if (closed) {
          return;
        }
        closed = true;
      }
      for (Map.Entry<AutoCloseable, Executor> entry : entrySet()) {
        closeQuietly(entry.getKey(), entry.getValue());
      }
      clear();
      if (whenClosed != null) {
        whenClosed.countDown();
      }
    }

    void add(@CheckForNull AutoCloseable closeable, Executor executor) {
      checkNotNull(executor);
      if (closeable == null) {
        return;
      }
      synchronized (this) {
        if (!closed) {
          put(closeable, executor);
          return;
        }
      }
      closeQuietly(closeable, executor);
    }

    /**
     * Returns a latch that reaches zero when this objects' deferred closeables have been closed.
     */
    CountDownLatch whenClosedCountDown() {
      if (closed) {
        return new CountDownLatch(0);
      }
      synchronized (this) {
        if (closed) {
          return new CountDownLatch(0);
        }
        checkState(whenClosed == null);
        return whenClosed = new CountDownLatch(1);
      }
    }
  }

  /**
   * Returns an object that can be used to wait until this objects' deferred closeables have all had
   * {@link Runnable}s that close them submitted to each one's closing {@link Executor}.
   */
  @VisibleForTesting
  CountDownLatch whenClosedCountDown() {
    return closeables.whenClosedCountDown();
  }

  /** The state of a {@link CloseableList}. */
  enum State {
    /** The {@link CloseableList} has not been subsumed or closed. */
    OPEN,

    /**
     * The {@link CloseableList} has been subsumed into another. It may not be closed or subsumed
     * into any other.
     */
    SUBSUMED,

    /**
     * Some {@link ListenableFuture} has a callback attached that will close the {@link
     * CloseableList}, but it has not yet run. The {@link CloseableList} may not be subsumed.
     */
    WILL_CLOSE,

    /**
     * The callback that closes the {@link CloseableList} is running, but it has not completed. The
     * {@link CloseableList} may not be subsumed.
     */
    CLOSING,

    /** The {@link CloseableList} has been closed. It may not be further subsumed. */
    CLOSED,

    /**
     * {@link ClosingFuture#finishToValueAndCloser(ValueAndCloserConsumer, Executor)} has been
     * called. The step may not be further subsumed, nor may {@link #finishToFuture()} be called.
     */
    WILL_CREATE_VALUE_AND_CLOSER,
  }
}
