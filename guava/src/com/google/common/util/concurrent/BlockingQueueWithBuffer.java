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
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nullable;


/**
 * <p>
 * An extension to the implementation of the BlockingQueue data structure. It uses an ArrayBlockingQueue
 * as the delegate which performers most of the BlockingQueue interface API along with a buffer memory implemented
 * using the ArrayList which provides an additional memory to hold the elements when the queue is full. It overrides
 * the methods that adds new objects to the the queue data structure in order to implement the Blocking feature along
 *with using an additional memory that manages to preserve fairness.
 *</p>
 *
 * <p>
 * A Blocking queue must be configured with a maximum size along with a buffer .
 * Each time an element wishes to be enqueued to a full queue, the buffer adds that element to it and checks
 * for a vacancy in the queue. If the queue has empty slot then the buffer removes the first element from it
 * and add it to the tail of queue.This is different from the traditional bounded queues, which either block
 * or reject new elements when working in full potential size , instead it provides a layered memory structure
 * to store the element and provides each element with an equal opportunity for enqueueing .
 *</p>
 *
 * <p>
 * This class does not accept null elements.
 * </>
 *
 * @author Shreyans Jain
 *
 */

@GwtIncompatible
public final class BlockingQueueWithBuffer<E> extends ForwardingBlockingQueue<E> implements Serializable {

    private final BlockingQueue<E> delegate;
    private final ArrayList<E> buffer;

    private final Integer lock;

    @VisibleForTesting
    final int maxSize;

    private BlockingQueueWithBuffer(int maxSize) {
        checkArgument(maxSize >= 1, "maxSize (%s) must >= 1", maxSize);
        this.delegate = new ArrayBlockingQueue<E>(maxSize);
        this.buffer=new ArrayList<>();
        this.maxSize = maxSize;
        this.lock = Integer.valueOf(0);
    }

    /**
     * Creates and returns the object of BlockingQueueWithBuffer that will hold up to
     * {@code maxSize} elements.
     *
     */
    public static <E> BlockingQueueWithBuffer<E> create(int maxSize) {
        return new BlockingQueueWithBuffer<>(maxSize);
    }

    /**
     * @return
     */
//
    @Override
    protected BlockingQueue<E> delegate() {
        return this.delegate;
    }

    /**
     * Adds the given element to this queue if the queue is empty. If the queue is currently full, the
     * element is added to the end of buffer.
     *
     */
    @Override
    public void put(E e) {
        add(e);
    }

    /**
     * Adds the given element to this queue if the queue is empty. If the queue is currently full, the
     * element is added to the end of buffer.
     *
     * @return {@code true} always
     */
    @Override
    @CanIgnoreReturnValue
    public boolean offer(E e) {
        return add(e);
    }

    /**
     * Adds the given element to this queue if the queue is empty. If the queue is currently full, the
     * element is added to the end of buffer.
     *
     * @return {@code true} always
     */
    @Override
    @CanIgnoreReturnValue
    public boolean offer(E e, long timeout, @Nullable TimeUnit unit) {
        return add(e); // Timeout is useless in an BlockingQueueWithBuffer feature.
    }

    /**
     * Adds the given element to this queue if the queue is empty. If the queue is currently full, the
     * element is added to the end of buffer. If the queue has some space then the element at the top the
     * buffer is inserted at the tail of the queue and vacated from the buffer.
     *
     * @return {@code true} always
     */
    @Override
    @CanIgnoreReturnValue
    public boolean add(E e) {
        checkNotNull(e); // check before removing
        synchronized (lock) {
            if(size()==maxSize){
                buffer.add(e);
                return true;
            }
            else if(size()==maxSize-1 && buffer.size()!=0){
                E element = buffer.get(0);
                buffer.remove(0);
                delegate.add(element);
                buffer.add(e);
                return true;
            }
            else if(size()<maxSize && buffer.size()!=0){
                int size=maxSize-size();
                while(size!=0 && buffer.size()!=0){
                    E element = buffer.get(0);
                    delegate.add(element);
                    buffer.remove(0);
                    size=size-1;
                }
                if(size()<maxSize && buffer.size()==0){
                    delegate.add(e);
                    return true;
                }
                else if(size()==maxSize){
                    buffer.add(e);
                    return true;
                }
            }
                delegate.add(e);
                return true;
        }
    }

    @Override
    @CanIgnoreReturnValue
    public boolean addAll(Collection<? extends E> collection) {
//        int size = collection.size();
        synchronized (lock) {
            for(E element:collection){
                if(this.size()<maxSize){
                    this.add(element);
                }
                else{
                    buffer.add(element);
                }
            }
            return standardAddAll(collection);
        }
    }

    /**
     * Since the element must be accessed from the queue only so removal of element can only take place from the queue.
     * @return element
     */
    @Override
    public boolean remove(Object o) {
        checkNotNull(o);
        synchronized (lock) {
            boolean element=this.delegate.remove(o);
            if(buffer.size()!=0){
                E e = buffer.get(0);
                delegate.add(e);
            }
            return element;
        }
    }
    /**
     * Since the element must be accessed from the queue only, therefore
     * removal of element can only take place from the queue.
     * @return element
     */

    @Override
    public E remove() {
        synchronized (lock) {
            E element=  this.delegate.remove();
            if(buffer.size()!=0){
                E e = buffer.get(0);
                delegate.add(e);
            }
            return element;
        }
    }

    /**
     * Since the element must be accessed from the queue only, therefore
     * return the element at the top.
     * @return element
     */

    @Override
    public E poll() {
        synchronized (lock) {
            return this.delegate.poll();
        }
    }

    @Override
    public boolean contains(Object object) {
        synchronized (lock) {
            return delegate().contains(checkNotNull(object)) || buffer.contains(checkNotNull(object));
        }
    }

}