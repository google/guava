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

import com.google.common.annotations.GwtCompatible;

import java.util.concurrent.Executor;

/**
 * Factory and utility methods for {@link java.util.concurrent.Executor}, {@link
 * ExecutorService}, and {@link ThreadFactory}.
 *
 * @author Eric Fellheimer
 * @author Kyle Littlefield
 * @author Justin Mahoney
 * @since 3.0
 */
@GwtCompatible(emulated = true)
public final class MoreExecutors {
  private MoreExecutors() {}

  // See sameThreadExecutor javadoc for behavioral notes.

  /**
   * Returns an {@link Executor} that runs each task in the thread that invokes
   * {@link Executor#execute execute}, as in {@link CallerRunsPolicy}.
   *
   * <p>This instance is equivalent to: <pre>   {@code
   *   final class DirectExecutor implements Executor {
   *     public void execute(Runnable r) {
   *       r.run();
   *     }
   *   }}</pre>
   *
   * <p>This should be preferred to {@link #newDirectExecutorService()} because the implementing the
   * {@link ExecutorService} subinterface necessitates significant performance overhead.
   *
   * @since 18.0
   */
  public static Executor directExecutor() {
    return DirectExecutor.INSTANCE;
  }

  /** See {@link #directExecutor} for behavioral notes. */
  private enum DirectExecutor implements Executor {
    INSTANCE;
    @Override public void execute(Runnable command) {
      command.run();
    }

    @Override public String toString() {
      return "MoreExecutors.directExecutor()";
    }
  }

  /*
   * This following method is a modified version of one found in
   * http://gee.cs.oswego.edu/cgi-bin/viewcvs.cgi/jsr166/src/test/tck/AbstractExecutorServiceTest.java?revision=1.30
   * which contained the following notice:
   *
   * Written by Doug Lea with assistance from members of JCP JSR-166
   * Expert Group and released to the public domain, as explained at
   * http://creativecommons.org/publicdomain/zero/1.0/
   * Other contributors include Andrew Wright, Jeffrey Hayes,
   * Pat Fisher, Mike Judd.
   */

  // TODO(lukes): provide overloads for ListeningExecutorService? ListeningScheduledExecutorService?
  // TODO(lukes): provide overloads that take constant strings? Function<Runnable, String>s to
  // calculate names?
}
