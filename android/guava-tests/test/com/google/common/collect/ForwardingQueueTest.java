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

package com.google.common.collect;

import static java.util.Arrays.asList;

import com.google.common.base.Function;
import com.google.common.collect.testing.QueueTestSuiteBuilder;
import com.google.common.collect.testing.TestStringQueueGenerator;
import com.google.common.collect.testing.features.CollectionFeature;
import com.google.common.collect.testing.features.CollectionSize;
import com.google.common.testing.ForwardingWrapperTester;
import java.util.Collection;
import java.util.Queue;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Tests for {@code ForwardingQueue}.
 *
 * @author Robert Konigsberg
 * @author Louis Wasserman
 */
public class ForwardingQueueTest extends TestCase {

  static final class StandardImplForwardingQueue<T> extends ForwardingQueue<T> {
    private final Queue<T> backingQueue;

    StandardImplForwardingQueue(Queue<T> backingQueue) {
      this.backingQueue = backingQueue;
    }

    @Override
    protected Queue<T> delegate() {
      return backingQueue;
    }

    @Override
    public boolean addAll(Collection<? extends T> collection) {
      return standardAddAll(collection);
    }

    @Override
    public void clear() {
      standardClear();
    }

    @Override
    public boolean contains(Object object) {
      return standardContains(object);
    }

    @Override
    public boolean containsAll(Collection<?> collection) {
      return standardContainsAll(collection);
    }

    @Override
    public boolean remove(Object object) {
      return standardRemove(object);
    }

    @Override
    public boolean removeAll(Collection<?> collection) {
      return standardRemoveAll(collection);
    }

    @Override
    public boolean retainAll(Collection<?> collection) {
      return standardRetainAll(collection);
    }

    @Override
    public Object[] toArray() {
      return standardToArray();
    }

    @Override
    public <T> T[] toArray(T[] array) {
      return standardToArray(array);
    }

    @Override
    public String toString() {
      return standardToString();
    }

    @Override
    public boolean offer(T o) {
      return standardOffer(o);
    }

    @Override
    public T peek() {
      return standardPeek();
    }

    @Override
    public T poll() {
      return standardPoll();
    }
  }

  public static Test suite() {
    TestSuite suite = new TestSuite();

    suite.addTestSuite(ForwardingQueueTest.class);
    suite.addTest(
        QueueTestSuiteBuilder.using(
                new TestStringQueueGenerator() {

                  @Override
                  protected Queue<String> create(String[] elements) {
                    return new StandardImplForwardingQueue<>(Lists.newLinkedList(asList(elements)));
                  }
                })
            .named("ForwardingQueue[LinkedList] with standard implementations")
            .withFeatures(
                CollectionSize.ANY,
                CollectionFeature.ALLOWS_NULL_VALUES,
                CollectionFeature.GENERAL_PURPOSE)
            .createTestSuite());

    return suite;
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  public void testForwarding() {
    new ForwardingWrapperTester()
        .testForwarding(
            Queue.class,
            new Function<Queue, Queue>() {
              @Override
              public Queue apply(Queue delegate) {
                return wrap(delegate);
              }
            });
  }

  private static <T> Queue<T> wrap(final Queue<T> delegate) {
    return new ForwardingQueue<T>() {
      @Override
      protected Queue<T> delegate() {
        return delegate;
      }
    };
  }
}
