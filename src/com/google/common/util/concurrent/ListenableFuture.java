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

import com.google.common.annotations.Beta;

import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;

/**
 * A {@link Future} that accepts completion listeners.  Each listener has an
 * associated executor, and is invoked using this executor once the future's
 * computation is {@linkplain Future#isDone() complete}.  If the computation has
 * already completed when the listener is added, the listener will execute
 * immediately.
 *
 * <p>Common {@code ListenableFuture} implementations include {@link
 * SettableFuture} and the futures returned by a {@link
 * ListeningExecutorService} (typically {@link ListenableFutureTask}
 * instances).
 *
 * <p>Usage:
 * <pre>   {@code
 *   final ListenableFuture<?> future = myService.async(myRequest);
 *   future.addListener(new Runnable() {
 *     public void run() {
 *       System.out.println("Operation Complete.");
 *       try {
 *         System.out.println("Result: " + future.get());
 *       } catch (Exception e) {
 *         System.out.println("Error: " + e.message());
 *       }
 *     }
 *   }, executor);}</pre>
 *
 * @author Sven Mawson
 * @author Nishant Thakkar
 * @since 1
 */
@Beta
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
   * <p>Listeners cannot throw checked exceptions and should not throw {@code
   * RuntimeException} unless their executors are prepared to handle it.
   * Listeners that will execute in {@link MoreExecutors#sameThreadExecutor}
   * should take special care, since they may run during the call to {@code
   * addListener} or during the call that sets the future's value.
   *
   * @param listener the listener to run when the computation is complete
   * @param executor the executor to run the listener in
   * @throws NullPointerException if the executor or listener was null
   * @throws RejectedExecutionException if we tried to execute the listener
   *         immediately but the executor rejected it.
   */
  void addListener(Runnable listener, Executor executor);
}
