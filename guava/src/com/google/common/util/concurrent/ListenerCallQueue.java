/*
 * Copyright (C) 2014 The Guava Authors
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

import com.google.common.base.Preconditions;
import com.google.common.collect.Queues;

import java.util.Queue;
import java.util.concurrent.Executor;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.concurrent.GuardedBy;

/**
 * A special purpose queue/executor that executes listener callbacks serially on a configured
 * executor.  Each callback task can be enqueued and executed as separate phases.
 * 
 * <p>This class is very similar to {@link SerializingExecutor} with the exception that tasks can
 * be enqueued without necessarily executing immediately.
 */
final class ListenerCallQueue<L> implements Runnable {
  // TODO(cpovirk): consider using the logger associated with listener.getClass().
  private static final Logger logger = Logger.getLogger(ListenerCallQueue.class.getName());

  abstract static class Callback<L> {
    private final String methodCall;

    Callback(String methodCall) {
      this.methodCall = methodCall;
    }

    abstract void call(L listener);
    
    /** Helper method to add this callback to all the queues. */
    void enqueueOn(Iterable<ListenerCallQueue<L>> queues) {
      for (ListenerCallQueue<L> queue : queues) {
        queue.add(this);
      }
    }
  }

  private final L listener;
  private final Executor executor;

  @GuardedBy("this") private final Queue<Callback<L>> waitQueue = Queues.newArrayDeque();
  @GuardedBy("this") private boolean isThreadScheduled;

  ListenerCallQueue(L listener, Executor executor) {
    this.listener = checkNotNull(listener);
    this.executor = checkNotNull(executor);
  }

  /** Enqueues a task to be run. */
  synchronized void add(Callback<L> callback) {
    waitQueue.add(callback);
  }

  /** Executes all listeners {@linkplain #add added} prior to this call, serially and in order.*/
  void execute() {
    boolean scheduleTaskRunner = false;
    synchronized (this) {
      if (!isThreadScheduled) {
        isThreadScheduled = true;
        scheduleTaskRunner = true;
      }
    }
    if (scheduleTaskRunner) {
      try {
        executor.execute(this);
      } catch (RuntimeException e) {
        // reset state in case of an error so that later calls to execute will actually do something
        synchronized (this) {
          isThreadScheduled = false;
        }
        // Log it and keep going.
        logger.log(Level.SEVERE,
            "Exception while running callbacks for " + listener + " on " + executor, 
            e);
        throw e;
      }
    }
  }

  @Override public void run() {
    boolean stillRunning = true;
    try {
      while (true) {
        Callback<L> nextToRun;
        synchronized (ListenerCallQueue.this) {
          Preconditions.checkState(isThreadScheduled);
          nextToRun = waitQueue.poll();
          if (nextToRun == null) {
            isThreadScheduled = false;
            stillRunning = false;
            break;
          }
        }

        // Always run while _not_ holding the lock, to avoid deadlocks.
        try {
          nextToRun.call(listener);
        } catch (RuntimeException e) {
          // Log it and keep going.
          logger.log(Level.SEVERE, 
              "Exception while executing callback: " + listener + "." + nextToRun.methodCall, 
              e);
        }
      }
    } finally {
      if (stillRunning) {
        // An Error is bubbling up, we should mark ourselves as no longer
        // running, that way if anyone tries to keep using us we won't be
        // corrupted.
        synchronized (ListenerCallQueue.this) {
          isThreadScheduled = false;
        }
      }
    }
  }
}
