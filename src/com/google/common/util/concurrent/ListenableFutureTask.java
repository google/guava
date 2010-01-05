/*
 * Copyright (C) 2008 Google Inc.
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

import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.FutureTask;

/**
 * A {@link FutureTask} that also implements the {@link ListenableFuture}
 * interface.  Subclasses must make sure to call {@code super.done()} if they
 * also override the {@link #done()} method, otherwise the listeners will not
 * be called.
 * 
 * @author Sven Mawson
 * @since 2009.09.15 <b>tentative</b>
 */
public class ListenableFutureTask<V> extends FutureTask<V>
    implements ListenableFuture<V> {

  // The execution list to hold our listeners.
  private final ExecutionList executionList = new ExecutionList();
  
  /**
   * Creates a {@code ListenableFutureTask} that will upon running, execute the
   * given {@code Callable}.
   *
   * @param  callable the callable task
   * @throws NullPointerException if callable is null
   */
  public ListenableFutureTask(Callable<V> callable) {
    super(callable);
  }

  /**
   * Creates a {@code ListenableFutureTask} that will upon running, execute the
   * given {@code Runnable}, and arrange that {@code get} will return the
   * given result on successful completion.
   *
   * @param  runnable the runnable task
   * @param result the result to return on successful completion. If
   * you don't need a particular result, consider using
   * constructions of the form:
   * {@code ListenableFuture<?> f =
   *     new ListenableFutureTask<Object>(runnable, null)}
   * @throws NullPointerException if runnable is null
   */
  public ListenableFutureTask(Runnable runnable, V result) {
    super(runnable, result);
  }

  public void addListener(Runnable listener, Executor exec) {
    executionList.add(listener, exec);
  }
  
  @Override
  protected void done() {
    executionList.run();
  }
}
