/*
 * Copyright (C) 2013 The Guava Authors
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

import com.google.common.collect.Queues;

import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.concurrent.GuardedBy;
import javax.annotation.concurrent.ThreadSafe;

/**
 * <p>A thread-safe queue of listeners, each with an associated {@code Executor}, that guarantees 
 * that every {@code Runnable} that is {@linkplain #add added} will be 
 * {@link Executor#execute(Runnable) executed} in the same order that it was added and also that 
 * execution is delayed until the next call to {@link #execute}.  This is particularly useful if
 * you need to ensure that listeners are <em>not</em> executed with a lock held. 
 * 
 * <p>While similar, this is different than {@link ExecutionList} which makes much looser guarantees
 * around ordering and has a notion of state (the executed bit) that this class does not have.
 * 
 * <p>Listeners are queued by calling {@link #add} and flushed on calls to {@link #execute()}. 
 * Because calls to {@link #add} may be concurrent with calls to {@link #execute} there is no 
 * guarantee that the queue will be empty after calling {@link #execute}.
 *
 * <p>Exceptions thrown by a listener will be propagated up to the executor. Any exception thrown 
 * during {@code Executor.execute} (e.g., a {@code RejectedExecutionException} or an exception 
 * thrown by {@linkplain MoreExecutors#sameThreadExecutor inline execution}) will be caught and
 * logged.
 *
 * @author Luke Sandberg
 */
@ThreadSafe final class ExecutionQueue {
  private static final Logger logger = Logger.getLogger(ExecutionQueue.class.getName());

  /** The listeners to execute in order.  */
  private final ConcurrentLinkedQueue<RunnableExecutorPair> queuedListeners = 
      Queues.newConcurrentLinkedQueue();
  /**
   * This lock is used with {@link RunnableExecutorPair#submit} to ensure that each listener is 
   * executed at most once.
   */
  private final ReentrantLock lock = new ReentrantLock();

  /**
   * Adds the {@code Runnable} and accompanying {@code Executor} to the queue of listeners to
   * execute. 
   */
  void add(Runnable runnable, Executor executor) {
    queuedListeners.add(new RunnableExecutorPair(runnable, executor));
  }

  /**
   * Executes all listeners in the queue. However, note that listeners added concurrently with this
   * method may be executed as part of this call or not, so there is no guarantee that the queue is
   * empty after calling this method.
   */
  void execute() {
    // We need to make sure that listeners are submitted to their executors in the correct order. So
    // we cannot remove a listener from the queue until we know that it has been submited to its
    // executor.  So we use an iterator and only call remove after submit.  This iterator is 'weakly
    // consistent' which means it observes the list in the correct order but not neccesarily all of
    // it (i.e. concurrently added or removed items may or may not be observed correctly by this
    // iterator).  This is fine because 1. our contract says we may not execute all concurrently
    // added items and 2. calling listener.submit is idempotent, so it is safe (and generally cheap)
    // to call it multiple times.
    Iterator<RunnableExecutorPair> iterator = queuedListeners.iterator();
    while (iterator.hasNext()) {
      iterator.next().submit();
      iterator.remove();
    }
  }

  /**
   * The listener object for the queue.
   *
   * <p>This ensures that:
   * <ol>
   *   <li>{@link #executor executor}.{@link Executor#execute execute} is called at most once
   *   <li>{@link #runnable runnable}.{@link Runnable#run run} is called at most once by the
   *        executor
   *   <li>{@link #lock lock} is not held when {@link #runnable runnable}.{@link Runnable#run run}
   *       is called
   *   <li>no thread calling {@link #submit} can return until the task has been accepted by the
   *       executor
   * </ol>
   */
  private final class RunnableExecutorPair implements Runnable {
    private final Executor executor;
    private final Runnable runnable;
    /**
     * Should be set to {@code true} after {@link #executor}.{@link Executor#execute execute} has
     * been called.
     */
    @GuardedBy("lock")
    private boolean hasBeenExecuted = false;
  
    RunnableExecutorPair(Runnable runnable, Executor executor) {
      this.runnable = checkNotNull(runnable);
      this.executor = checkNotNull(executor);
    }
  
    /** Submit this listener to its executor */
    private void submit() {
      lock.lock();
      try {
        if (!hasBeenExecuted) {
          try {
            executor.execute(this);
          } catch (Exception e) {
            logger.log(Level.SEVERE, "Exception while executing listener " + runnable
                + " with executor " + executor, e);
          }
        }
      } finally {
        // If the executor was the sameThreadExecutor we may have already released the lock, so
        // check for that here.
        if (lock.isHeldByCurrentThread()) {
          hasBeenExecuted = true;
          lock.unlock();
        }
      }
    }
  
    @Override public final void run() {
      // If the executor was the sameThreadExecutor then we might still be holding the lock and
      // hasBeenExecuted may not have been assigned yet so we unlock now to ensure that we are not
      // still holding the lock while execute is called.
      if (lock.isHeldByCurrentThread()) {
        hasBeenExecuted = true;
        lock.unlock();
      }
      runnable.run();
    }
  }
}
