/*
 * Copyright (C) 2011 The Guava Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.common.collect;

import com.google.common.annotations.Beta;
import com.google.common.base.Preconditions;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;

/**
 * Static utility methods pertaining to {@link Queue}
 * instances. Also see this class's counterparts
 * {@link Lists}, {@link Sets}, and {@link Maps}.
 *
 * @author Kurt Alfred Kluever
 * @since 11.0
 */
@Beta
public final class Queues {
  private Queues() {}

  // ArrayBlockingQueue

  /**
   * Creates an empty {@code ArrayBlockingQueue} instance.
   *
   * @return a new, empty {@code ArrayBlockingQueue}
   */
  public static <E> ArrayBlockingQueue<E> newArrayBlockingQueue(int capacity) {
    return new ArrayBlockingQueue<E>(capacity);
  }

  // ArrayDeque

  // ConcurrentLinkedQueue

  /**
   * Creates an empty {@code ConcurrentLinkedQueue} instance.
   *
   * @return a new, empty {@code ConcurrentLinkedQueue}
   */
  public static <E> ConcurrentLinkedQueue<E> newConcurrentLinkedQueue() {
    return new ConcurrentLinkedQueue<E>();
  }

  /**
   * Creates an {@code ConcurrentLinkedQueue} instance containing the given elements.
   *
   * @param elements the elements that the queue should contain, in order
   * @return a new {@code ConcurrentLinkedQueue} containing those elements
   */
  public static <E> ConcurrentLinkedQueue<E> newConcurrentLinkedQueue(
      Iterable<? extends E> elements) {
    if (elements instanceof Collection) {
      return new ConcurrentLinkedQueue<E>(Collections2.cast(elements));
    }
    ConcurrentLinkedQueue<E> queue = new ConcurrentLinkedQueue<E>();
    Iterables.addAll(queue, elements);
    return queue;
  }

  // LinkedBlockingDeque

  // LinkedBlockingQueue

  /**
   * Creates an empty {@code LinkedBlockingQueue} instance.
   *
   * @return a new, empty {@code LinkedBlockingQueue}
   */
  public static <E> LinkedBlockingQueue<E> newLinkedBlockingQueue() {
    return new LinkedBlockingQueue<E>();
  }

  /**
   * Creates a {@code LinkedBlockingQueue} with the given (fixed) capacity.
   *
   * @param capacity the capacity of this queue
   * @return a new, empty {@code LinkedBlockingQueue}
   * @throws IllegalArgumentException if {@code capacity} is less than 1
   */
  public static <E> LinkedBlockingQueue<E> newLinkedBlockingQueue(int capacity) {
    return new LinkedBlockingQueue<E>(capacity);
  }

  /**
   * Creates an {@code LinkedBlockingQueue} instance containing the given elements.
   *
   * @param elements the elements that the queue should contain, in order
   * @return a new {@code LinkedBlockingQueue} containing those elements
   */
  public static <E> LinkedBlockingQueue<E> newLinkedBlockingQueue(Iterable<? extends E> elements) {
    if (elements instanceof Collection) {
      return new LinkedBlockingQueue<E>(Collections2.cast(elements));
    }
    LinkedBlockingQueue<E> queue = new LinkedBlockingQueue<E>();
    Iterables.addAll(queue, elements);
    return queue;
  }

  // LinkedList: see {@link com.google.common.collect.Lists}

  // PriorityBlockingQueue

  /**
   * Creates an empty {@code PriorityBlockingQueue} instance.
   *
   * @return a new, empty {@code PriorityBlockingQueue}
   */
  public static <E> PriorityBlockingQueue<E> newPriorityBlockingQueue() {
    return new PriorityBlockingQueue<E>();
  }

  /**
   * Creates an {@code PriorityBlockingQueue} instance containing the given elements.
   *
   * @param elements the elements that the queue should contain, in order
   * @return a new {@code PriorityBlockingQueue} containing those elements
   */
  public static <E> PriorityBlockingQueue<E> newPriorityBlockingQueue(
      Iterable<? extends E> elements) {
    if (elements instanceof Collection) {
      return new PriorityBlockingQueue<E>(Collections2.cast(elements));
    }
    PriorityBlockingQueue<E> queue = new PriorityBlockingQueue<E>();
    Iterables.addAll(queue, elements);
    return queue;
  }

  // PriorityQueue

  /**
   * Creates an empty {@code PriorityQueue} instance.
   *
   * @return a new, empty {@code PriorityQueue}
   */
  public static <E> PriorityQueue<E> newPriorityQueue() {
    return new PriorityQueue<E>();
  }

  /**
   * Creates an {@code PriorityQueue} instance containing the given elements.
   *
   * @param elements the elements that the queue should contain, in order
   * @return a new {@code PriorityQueue} containing those elements
   */
  public static <E> PriorityQueue<E> newPriorityQueue(Iterable<? extends E> elements) {
    if (elements instanceof Collection) {
      return new PriorityQueue<E>(Collections2.cast(elements));
    }
    PriorityQueue<E> queue = new PriorityQueue<E>();
    Iterables.addAll(queue, elements);
    return queue;
  }

  // SynchronousQueue

  /**
   * Creates an empty {@code SynchronousQueue} instance.
   *
   * @return a new, empty {@code SynchronousQueue}
   */
  public static <E> SynchronousQueue<E> newSynchronousQueue() {
    return new SynchronousQueue<E>();
  }

  /**
   * Drains the queue as {@link BlockingQueue#drainTo(Collection, int)}, but if the requested
   * {@code numElements} elements are not available, it will wait for them up to the specified
   * timeout.
   *
   * @param q the blocking queue to be drained
   * @param buffer where to add the transferred elements
   * @param numElements the number of elements to be waited for
   * @param timeout how long to wait before giving up, in units of {@code unit}
   * @param unit a {@code TimeUnit} determining how to interpret the timeout parameter
   * @return the number of elements transferred
   * @throws InterruptedException if interrupted while waiting
   */
  public static <E> int drain(BlockingQueue<E> q, Collection<? super E> buffer, int numElements,
      long timeout, TimeUnit unit) throws InterruptedException {
    Preconditions.checkNotNull(buffer);
    /*
     * This code performs one System.nanoTime() more than necessary, and in return, the time to
     * execute Queue#drainTo is not added *on top* of waiting for the timeout (which could make
     * the timeout arbitrarily inaccurate, given a queue that is slow to drain).
     */
    long deadline = System.nanoTime() + unit.toNanos(timeout);
    int added = 0;
    while (added < numElements) {
      // we could rely solely on #poll, but #drainTo might be more efficient when there are multiple
      // elements already available (e.g. LinkedBlockingQueue#drainTo locks only once)
      added += q.drainTo(buffer, numElements - added);
      if (added < numElements) { // not enough elements immediately available; will have to poll
        E e = q.poll(deadline - System.nanoTime(), TimeUnit.NANOSECONDS);
        if (e == null) {
          break; // we already waited enough, and there are no more elements in sight
        }
        buffer.add(e);
        added++;
      }
    }
    return added;
  }

  /**
   * Drains the queue as {@linkplain #drain(BlockingQueue, Collection, int, long, TimeUnit)},
   * but with a different behavior in case it is interrupted while waiting. In that case, the
   * operation will continue as usual, and in the end the thread's interruption status will be set
   * (no {@code InterruptedException} is thrown).
   *
   * @param q the blocking queue to be drained
   * @param buffer where to add the transferred elements
   * @param numElements the number of elements to be waited for
   * @param timeout how long to wait before giving up, in units of {@code unit}
   * @param unit a {@code TimeUnit} determining how to interpret the timeout parameter
   * @return the number of elements transferred
   */
  public static <E> int drainUninterruptibly(BlockingQueue<E> q, Collection<? super E> buffer,
      int numElements, long timeout, TimeUnit unit) {
    Preconditions.checkNotNull(buffer);
    long deadline = System.nanoTime() + unit.toNanos(timeout);
    int added = 0;
    boolean interrupted = false;
    try {
      while (added < numElements) {
        // we could rely solely on #poll, but #drainTo might be more efficient when there are
        // multiple elements already available (e.g. LinkedBlockingQueue#drainTo locks only once)
        added += q.drainTo(buffer, numElements - added);
        if (added < numElements) { // not enough elements immediately available; will have to poll
          E e; // written exactly once, by a successful (uninterrupted) invocation of #poll
          while (true) {
            try {
              e = q.poll(deadline - System.nanoTime(), TimeUnit.NANOSECONDS);
              break;
            } catch (InterruptedException ex) {
              interrupted = true; // note interruption and retry
            }
          }
          if (e == null) {
            break; // we already waited enough, and there are no more elements in sight
          }
          buffer.add(e);
          added++;
        }
      }
    } finally {
      if (interrupted) {
        Thread.currentThread().interrupt();
      }
    }
    return added;
  }
}
