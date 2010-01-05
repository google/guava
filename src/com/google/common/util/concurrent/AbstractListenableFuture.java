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

/**
 * <p>An abstract base implementation of the listener support provided by
 * {@link ListenableFuture}. This class uses an {@link ExecutionList} to
 * guarantee that all registered listeners will be executed. Listener/Executor
 * pairs are stored in the execution list and executed in the order in which
 * they were added, but because of thread scheduling issues there is no
 * guarantee that the JVM will execute them in order. In addition, listeners
 * added after the task is complete will be executed immediately, even if some
 * previously added listeners have not yet been executed.
 * 
 * <p>This class uses the {@link AbstractFuture} class to implement the
 * {@code ListenableFuture} interface and simply delegates the
 * {@link #addListener(Runnable, Executor)} and {@link #done()} methods to it.
 * 
 * @author Sven Mawson
 * @since 2009.09.15 <b>tentative</b>
 */
public abstract class AbstractListenableFuture<V>
    extends AbstractFuture<V> implements ListenableFuture<V> {

  // The execution list to hold our executors.
  private final ExecutionList executionList = new ExecutionList();

  /*
   * Adds a listener/executor pair to execution list to execute when this task
   * is completed.
   */
  public void addListener(Runnable listener, Executor exec) {
    executionList.add(listener, exec);
  }

  /*
   * Override the done method to execute the execution list.
   */
  @Override
  protected void done() {
    executionList.run();
  }
}
