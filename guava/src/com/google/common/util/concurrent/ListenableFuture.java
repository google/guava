/*
 * Copyright (C) 2007 The Guava Authors
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

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RejectedExecutionException;

/**
 * A {@link Future} that accepts completion listeners.  Each listener has an
 * associated executor, and it is invoked using this executor once the future's
 * computation is {@linkplain Future#isDone() complete}.  If the computation has
 * already completed when the listener is added, the listener will execute
 * immediately.
 * 
 * <p>See the Guava User Guide article on <a href=
 * "http://code.google.com/p/guava-libraries/wiki/ListenableFutureExplained">
 * {@code ListenableFuture}</a>.
 *
 * <h3>Purpose</h3>
 *
 * <p>Most commonly, {@code ListenableFuture} is used as an input to another
 * derived {@code Future}, as in {@link Futures#allAsList(Iterable)
 * Futures.allAsList}. Many such methods are impossible to implement efficiently
 * without listener support.
 *
 * <p>It is possible to call {@link #addListener addListener} directly, but this
 * is uncommon because the {@code Runnable} interface does not provide direct
 * access to the {@code Future} result. (Users who want such access may prefer
 * {@link Futures#addCallback Futures.addCallback}.) Still, direct {@code
 * addListener} calls are occasionally useful:<pre>   {@code
 *   final String name = ...;
 *   inFlight.add(name);
 *   ListenableFuture<Result> future = service.query(name);
 *   future.addListener(new Runnable() {
 *     public void run() {
 *       processedCount.incrementAndGet();
 *       inFlight.remove(name);
 *       lastProcessed.set(name);
 *       logger.info("Done with {0}", name);
 *     }
 *   }, executor);}</pre>
 *
 * <h3>How to get an instance</h3>
 *
 * <p>Developers are encouraged to return {@code ListenableFuture} from their
 * methods so that users can take advantages of the utilities built atop the
 * class. The way that they will create {@code ListenableFuture} instances
 * depends on how they currently create {@code Future} instances:
 * <ul>
 * <li>If they are returned from an {@code ExecutorService}, convert that
 * service to a {@link ListeningExecutorService}, usually by calling {@link
 * MoreExecutors#listeningDecorator(ExecutorService)
 * MoreExecutors.listeningDecorator}. (Custom executors may find it more
 * convenient to use {@link ListenableFutureTask} directly.)
 * <li>If they are manually filled in by a call to {@link FutureTask#set} or a
 * similar method, create a {@link SettableFuture} instead. (Users with more
 * complex needs may prefer {@link AbstractFuture}.)
 * </ul>
 *
 * <p>Occasionally, an API will return a plain {@code Future} and it will be
 * impossible to change the return type. For this case, we provide a more
 * expensive workaround in {@code JdkFutureAdapters}. However, when possible, it
 * is more efficient and reliable to create a {@code ListenableFuture} directly.
 *
 * @author Sven Mawson
 * @author Nishant Thakkar
 * @since 1.0
 */
public interface ListenableFuture<V> extends Future<V> {
  /**
   * Registers a listener to be {@linkplain Executor#execute(Runnable) run} on
   * the given executor.  The listener will run when the {@code Future}'s
   * computation is {@linkplain Future#isDone() complete} or, if the computation
   * is already complete, immediately.
   *
   * <p>There is no guaranteed ordering of execution of listeners, but any
   * listener added through this method is guaranteed to be called once the
   * computation is complete.
   *
   * <p>Exceptions thrown by a listener will be propagated up to the executor.
   * Any exception thrown during {@code Executor.execute} (e.g., a {@code
   * RejectedExecutionException} or an exception thrown by {@linkplain
   * MoreExecutors#sameThreadExecutor inline execution}) will be caught and
   * logged.
   *
   * <p>Note: For fast, lightweight listeners that would be safe to execute in
   * any thread, consider {@link MoreExecutors#sameThreadExecutor}. For heavier
   * listeners, {@code sameThreadExecutor()} carries some caveats.  For
   * example, the listener may run on an unpredictable or undesirable thread:
   *
   * <ul>
   * <li>If this {@code Future} is done at the time {@code addListener} is
   * called, {@code addListener} will execute the listener inline.
   * <li>If this {@code Future} is not yet done, {@code addListener} will
   * schedule the listener to be run by the thread that completes this {@code
   * Future}, which may be an internal system thread such as an RPC network
   * thread.
   * </ul>
   *
   * <p>Also note that, regardless of which thread executes the
   * {@code sameThreadExecutor()} listener, all other registered but unexecuted
   * listeners are prevented from running during its execution, even if those
   * listeners are to run in other executors.
   *
   * <p>This is the most general listener interface. For common operations
   * performed using listeners, see {@link
   * com.google.common.util.concurrent.Futures}. For a simplified but general
   * listener interface, see {@link
   * com.google.common.util.concurrent.Futures#addCallback addCallback()}.
   *
   * @param listener the listener to run when the computation is complete
   * @param executor the executor to run the listener in
   * @throws NullPointerException if the executor or listener was null
   * @throws RejectedExecutionException if we tried to execute the listener
   *         immediately but the executor rejected it.
   */
  void addListener(Runnable listener, Executor executor);
}
