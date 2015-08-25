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

import com.google.common.collect.testing.QueueTestSuiteBuilder;
import com.google.common.collect.testing.TestStringQueueGenerator;
import com.google.common.collect.testing.features.CollectionFeature;
import com.google.common.collect.testing.features.CollectionSize;

import junit.framework.Test;
import junit.framework.TestSuite;

import java.util.Collection;
import java.util.Collections;
import java.util.Queue;

/**
 * Tests for {@code ForwardingQueue}.
 *
 * @author Robert Konigsberg
 * @author Louis Wasserman
 */
public class ForwardingQueueTest extends ForwardingTestCase {

  static final class StandardImplForwardingQueue<T>
      extends ForwardingQueue<T> {
    private final Queue<T> backingQueue;

    StandardImplForwardingQueue(Queue<T> backingQueue) {
      this.backingQueue = backingQueue;
    }

    @Override protected Queue<T> delegate() {
      return backingQueue;
    }

    @Override public boolean addAll(Collection<? extends T> collection) {
      return standardAddAll(collection);
    }

    @Override public void clear() {
      standardClear();
    }

    @Override public boolean contains(Object object) {
      return standardContains(object);
    }

    @Override public boolean containsAll(Collection<?> collection) {
      return standardContainsAll(collection);
    }

    @Override public boolean remove(Object object) {
      return standardRemove(object);
    }

    @Override public boolean removeAll(Collection<?> collection) {
      return standardRemoveAll(collection);
    }

    @Override public boolean retainAll(Collection<?> collection) {
      return standardRetainAll(collection);
    }

    @Override public Object[] toArray() {
      return standardToArray();
    }

    @Override public <T> T[] toArray(T[] array) {
      return standardToArray(array);
    }

    @Override public String toString() {
      return standardToString();
    }

    @Override public boolean offer(T o) {
      return standardOffer(o);
    }

    @Override public T peek() {
      return standardPeek();
    }

    @Override public T poll() {
      return standardPoll();
    }
  }
  
  private Queue<String> forward;
  private Queue<String> queue;
  
  public static Test suite() {
    TestSuite suite = new TestSuite();
    
    suite.addTestSuite(ForwardingQueueTest.class);
    suite.addTest(
        QueueTestSuiteBuilder.using(new TestStringQueueGenerator() {

          @Override protected Queue<String> create(String[] elements) {
            return new StandardImplForwardingQueue<String>(
                Lists.newLinkedList(asList(elements)));
          }
        }).named(
            "ForwardingQueue[LinkedList] with standard implementations")
            .withFeatures(CollectionSize.ANY,
                CollectionFeature.ALLOWS_NULL_VALUES,
                CollectionFeature.GENERAL_PURPOSE).createTestSuite());
    
    return suite;
  }
  
  /*
   * Class parameters must be raw, so we can't create a proxy with generic
   * type arguments. The created proxy only records calls and returns null, so
   * the type is irrelevant at runtime.
   */
  @SuppressWarnings("unchecked")
  @Override protected void setUp() throws Exception {
    super.setUp();
    queue = createProxyInstance(Queue.class);
    forward = new ForwardingQueue<String>() {
      @Override protected Queue<String> delegate() {
        return queue;
      }
    };
  }

  public void testAdd_T() {
    forward.add("asdf");
    assertEquals("[add(Object)]", getCalls());
  }

  public void testAddAll_Collection() {
    forward.addAll(Collections.singleton("asdf"));
    assertEquals("[addAll(Collection)]", getCalls());
  }

  public void testClear() {
    forward.clear();
    assertEquals("[clear]", getCalls());
  }

  public void testContains_T() {
    forward.contains("asdf");
    assertEquals("[contains(Object)]", getCalls());
  }

  public void testContainsAll_Collection() {
    forward.containsAll(Collections.singleton("asdf"));
    assertEquals("[containsAll(Collection)]", getCalls());
  }

  public void testElement() {
    forward.element();
    assertEquals("[element]", getCalls());
  }

  public void testIterator() {
    forward.iterator();
    assertEquals("[iterator]", getCalls());
  }

  public void testIsEmpty() {
    forward.isEmpty();
    assertEquals("[isEmpty]", getCalls());
  }

  public void testOffer_T() {
    forward.offer("asdf");
    assertEquals("[offer(Object)]", getCalls());
  }

  public void testPeek() {
    forward.peek();
    assertEquals("[peek]", getCalls());
  }

  public void testPoll() {
    forward.poll();
    assertEquals("[poll]", getCalls());
  }

  public void testRemove() {
    forward.remove();
    assertEquals("[remove]", getCalls());
  }

  public void testRemove_Object() {
    forward.remove(Object.class);
    assertEquals("[remove(Object)]", getCalls());
  }

  public void testRemoveAll_Collection() {
    forward.removeAll(Collections.singleton("asdf"));
    assertEquals("[removeAll(Collection)]", getCalls());
  }

  public void testRetainAll_Collection() {
    forward.retainAll(Collections.singleton("asdf"));
    assertEquals("[retainAll(Collection)]", getCalls());
  }

  public void testSize() {
    forward.size();
    assertEquals("[size]", getCalls());
  }

  public void testToArray() {
    forward.toArray();
    assertEquals("[toArray]", getCalls());
  }

  public void testToArray_TArray() {
    forward.toArray(new String[0]);
    assertEquals("[toArray(Object[])]", getCalls());
  }
      
  public void testToString() {
    forward.toString();
    assertEquals("[toString]", getCalls());
  }
}
