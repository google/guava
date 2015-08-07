/*
 * Copyright (C) 2012 The Guava Authors
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

package com.google.common.collect;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.annotations.Beta;
import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.VisibleForTesting;

import java.io.Serializable;
import java.util.Collection;
import java.util.Queue;

/**
 * A non-blocking queue which automatically evicts elements from the head of the queue when
 * attempting to add new elements onto the queue and it is full. This data structure is logically
 * equivalent to a circular buffer (i.e., cyclic buffer or ring buffer).
 *
 * <p>An evicting queue must be configured with a maximum size. Each time an element is added
 * to a full queue, the queue automatically removes its head element. This is different from
 * conventional bounded queues, which either block or reject new elements when full.
 *
 * <p>This class is not thread-safe, and does not accept null elements.
 *
 * @author Kurt Alfred Kluever
 * @since 15.0
 */
@Beta
@GwtCompatible
public final class EvictingQueue<E> extends ForwardingQueue<E> implements Serializable {

  private final Queue<E> delegate;

  @VisibleForTesting final int maxSize;

  private EvictingQueue(int maxSize) {
    checkArgument(maxSize >= 0, "maxSize (%s) must >= 0", maxSize);
    this.delegate = Platform.newFastestQueue(maxSize);
    this.maxSize = maxSize;
  }

  /**
   * Creates and returns a new evicting queue that will hold up to {@code maxSize} elements.
   *
   * <p>When {@code maxSize} is zero, elements will be evicted immediately after being added to the
   * queue.
   */
  public static <E> EvictingQueue<E> create(int maxSize) {
    return new EvictingQueue<E>(maxSize);
  }

  /**
   * Returns the number of additional elements that this queue can accept without evicting;
   * zero if the queue is currently full.
   *
   * @since 16.0
   */
  public int remainingCapacity() {
    return maxSize - size();
  }

  @Override
  protected Queue<E> delegate() {
    return delegate;
  }

  /**
   * Adds the given element to this queue. If the queue is currently full, the element at the head
   * of the queue is evicted to make room.
   *
   * @return {@code true} always
   */
  @Override
  public boolean offer(E e) {
    return add(e);
  }

  /**
   * Adds the given element to this queue. If the queue is currently full, the element at the head
   * of the queue is evicted to make room.
   *
   * @return {@code true} always
   */
  @Override
  public boolean add(E e) {
    checkNotNull(e); // check before removing
    if (maxSize == 0) {
      return true;
    }
    if (size() == maxSize) {
      delegate.remove();
    }
    delegate.add(e);
    return true;
  }

  @Override
  public boolean addAll(Collection<? extends E> collection) {
    return standardAddAll(collection);
  }

  @Override
  public boolean contains(Object object) {
    return delegate().contains(checkNotNull(object));
  }

  @Override
  public boolean remove(Object object) {
    return delegate().remove(checkNotNull(object));
  }

  // TODO(kak): Do we want to checkNotNull each element in containsAll, removeAll, and retainAll?

  private static final long serialVersionUID = 0L;
}
