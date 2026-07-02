/*
 * Copyright (C) 2010 The Guava Authors
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

package com.google.common.util.concurrent;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.annotations.GwtIncompatible;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Iterables;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.io.Serializable;
import java.util.Collection;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nullable;


/**
 * A thread-safe implementation of the EvictingQueue data structure. It holds an
 * ArrayBlockingQueue as the delegate which performers most of the BlockingQueue
 * interface API. It overrides the methods that adds new objects to the data
 * structure in order to implement the Evicting feature.
 * 
 * <p>
 * An evicting blocking queue must be configured with a maximum size. Each time
 * an element is added to a full queue, the queue automatically removes its head
 * element. This is different from conventional bounded queues, which either
 * block or reject new elements when full.
 * 
 * <p>
 * This class does not accept null elements.
 * 
 * @author Noam Greenshtain
 * 
 */
@GwtIncompatible
public final class EvictingBlockingQueue<E> extends ForwardingBlockingQueue<E> implements Serializable {

	private final BlockingQueue<E> delegate;

	private final Integer lock;

	@VisibleForTesting
	final int maxSize;

	private EvictingBlockingQueue(int maxSize) {
		checkArgument(maxSize >= 1, "maxSize (%s) must >= 1", maxSize);
		this.delegate = new ArrayBlockingQueue<E>(maxSize);
		this.maxSize = maxSize;
		this.lock = Integer.valueOf(0);
	}

	/**
	 * Creates and returns a new EvictingBlockingQueue that will hold up to
	 * {@code maxSize} elements.
	 * 
	 */
	public static <E> EvictingBlockingQueue<E> create(int maxSize) {
		return new EvictingBlockingQueue<E>(maxSize);
	}

	@Override
	protected BlockingQueue<E> delegate() {
		return this.delegate;
	}

	/**
	 * Adds the given element to this queue. If the queue is currently full, the
	 * element at the head of the queue is evicted to make room.
	 *
	 * @return {@code true} always
	 */
	@Override
	public void put(E e) {
		add(e);
	}

	/**
	 * Adds the given element to this queue. If the queue is currently full, the
	 * element at the head of the queue is evicted to make room.
	 *
	 * @return {@code true} always
	 */
	@Override
	@CanIgnoreReturnValue
	public boolean offer(E e) {
		return add(e);
	}

	/**
	 * Adds the given element to this queue. If the queue is currently full, the
	 * element at the head of the queue is evicted to make room.
	 *
	 * @return {@code true} always
	 */
	@Override
	@CanIgnoreReturnValue
	public boolean offer(E e, long timeout, @Nullable TimeUnit unit) {
		return add(e); // Timeout is useless in an EvictingQueue feature.
	}

	/**
	 * Adds the given element to this queue. If the queue is currently full, the
	 * element at the head of the queue is evicted to make room. This operation is
	 * synchronized due to BlockingQueue semantics.
	 *
	 * @return {@code true} always
	 */
	@Override
	@CanIgnoreReturnValue
	public boolean add(E e) {
		checkNotNull(e); // check before removing
		synchronized (lock) {
			if (size() == maxSize) {
				delegate.remove();
			}
			delegate.add(e);
			return true;
		}
	}

	@Override
	@CanIgnoreReturnValue
	public boolean addAll(Collection<? extends E> collection) {
		int size = collection.size();
		synchronized (lock) {
			if (size >= maxSize) {
				clear();
				return Iterables.addAll(this, Iterables.skip(collection, size - maxSize));
			}
			return standardAddAll(collection);
		}
	}

	@Override
	public boolean remove(Object o) {
		checkNotNull(o);
		synchronized (lock) {
			return this.delegate.remove(o);
		}
	}

	@Override
	public E remove() {
		synchronized (lock) {
			return this.delegate.remove();
		}
	}

	@Override
	public E poll() {
		synchronized (lock) {
			return this.delegate.poll();
		}
	}

	@Override
	public boolean contains(Object object) {
		synchronized (lock) {
			return delegate().contains(checkNotNull(object));
		}
	}

}
