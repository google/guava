/*
 * Copyright (C) 2007 Google Inc.
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
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;

/**
 * <p>This interface defines a future that has listeners attached to it, which
 * is useful for asynchronous workflows.  Each listener has an associated
 * executor, and is invoked using this executor once the {@code Future}'s
 * computation is {@linkplain Future#isDone() complete}.  The listener will be
 * executed even if it is added after the computation is complete.
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
 *   }, exec);}</pre>
 *
 * @author Sven Mawson
 * @author Nishant Thakkar
 * @since 2009.09.15 <b>tentative</b>
 */
public interface ListenableFuture<V> extends Future<V> {

  /**
   * <p>Adds a listener and executor to the ListenableFuture.
   * The listener will be {@linkplain Executor#execute(Runnable) passed
   * to the executor} for execution when the {@code Future}'s computation is
   * {@linkplain Future#isDone() complete}.
   *
   * <p>There is no guaranteed ordering of execution of listeners, they may get
   * called in the order they were added and they may get called out of order,
   * but any listener added through this method is guaranteed to be called once
   * the computation is complete.
   *
   * @param listener the listener to run when the computation is complete.
   * @param exec the executor to run the listener in.
   * @throws NullPointerException if the executor or listener was null.
   * @throws RejectedExecutionException if we tried to execute the listener
   * immediately but the executor rejected it.
   */
  public void addListener(Runnable listener, Executor exec);
}
