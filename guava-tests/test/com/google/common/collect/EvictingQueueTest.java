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

import static java.util.Arrays.asList;

import com.google.common.testing.NullPointerTester;
import com.google.common.testing.SerializableTester;

import junit.framework.TestCase;

import java.util.NoSuchElementException;

/**
 * Tests for {@link EvictingQueue}.
 *
 * @author Kurt Alfred Kluever
 */
public class EvictingQueueTest extends TestCase {

  public void testCreateWithNegativeSize() throws Exception {
    try {
      EvictingQueue.create(-1);
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }

  public void testCreateWithZeroSize() throws Exception {
    EvictingQueue<String> queue = EvictingQueue.create(0);
    assertEquals(0, queue.size());

    assertTrue(queue.add("hi"));
    assertEquals(0, queue.size());

    assertTrue(queue.offer("hi"));
    assertEquals(0, queue.size());

    assertFalse(queue.remove("hi"));
    assertEquals(0, queue.size());

    try {
      queue.element();
      fail();
    } catch (NoSuchElementException expected) {}

    assertNull(queue.peek());
    assertNull(queue.poll());
    try {
      queue.remove();
      fail();
    } catch (NoSuchElementException expected) {}
  }

  public void testRemainingCapacity_maxSize0() {
    EvictingQueue<String> queue = EvictingQueue.create(0);
    assertEquals(0, queue.remainingCapacity());
  }

  public void testRemainingCapacity_maxSize1() {
    EvictingQueue<String> queue = EvictingQueue.create(1);
    assertEquals(1, queue.remainingCapacity());
    queue.add("hi");
    assertEquals(0, queue.remainingCapacity());
  }

  public void testRemainingCapacity_maxSize3() {
    EvictingQueue<String> queue = EvictingQueue.create(3);
    assertEquals(3, queue.remainingCapacity());
    queue.add("hi");
    assertEquals(2, queue.remainingCapacity());
    queue.add("hi");
    assertEquals(1, queue.remainingCapacity());
    queue.add("hi");
    assertEquals(0, queue.remainingCapacity());
  }

  public void testEvictingAfterOne() throws Exception {
    EvictingQueue<String> queue = EvictingQueue.create(1);
    assertEquals(0, queue.size());
    assertEquals(1, queue.remainingCapacity());

    assertTrue(queue.add("hi"));
    assertEquals("hi", queue.element());
    assertEquals("hi", queue.peek());
    assertEquals(1, queue.size());
    assertEquals(0, queue.remainingCapacity());

    assertTrue(queue.add("there"));
    assertEquals("there", queue.element());
    assertEquals("there", queue.peek());
    assertEquals(1, queue.size());
    assertEquals(0, queue.remainingCapacity());

    assertEquals("there", queue.remove());
    assertEquals(0, queue.size());
    assertEquals(1, queue.remainingCapacity());
  }

  public void testEvictingAfterThree() throws Exception {
    EvictingQueue<String> queue = EvictingQueue.create(3);
    assertEquals(0, queue.size());
    assertEquals(3, queue.remainingCapacity());

    assertTrue(queue.add("one"));
    assertTrue(queue.add("two"));
    assertTrue(queue.add("three"));
    assertEquals("one", queue.element());
    assertEquals("one", queue.peek());
    assertEquals(3, queue.size());
    assertEquals(0, queue.remainingCapacity());

    assertTrue(queue.add("four"));
    assertEquals("two", queue.element());
    assertEquals("two", queue.peek());
    assertEquals(3, queue.size());
    assertEquals(0, queue.remainingCapacity());

    assertEquals("two", queue.remove());
    assertEquals(2, queue.size());
    assertEquals(1, queue.remainingCapacity());
  }

  public void testAddAll() throws Exception {
    EvictingQueue<String> queue = EvictingQueue.create(3);
    assertEquals(0, queue.size());
    assertEquals(3, queue.remainingCapacity());

    assertTrue(queue.addAll(asList("one", "two", "three")));
    assertEquals("one", queue.element());
    assertEquals("one", queue.peek());
    assertEquals(3, queue.size());
    assertEquals(0, queue.remainingCapacity());

    assertTrue(queue.addAll(asList("four")));
    assertEquals("two", queue.element());
    assertEquals("two", queue.peek());
    assertEquals(3, queue.size());
    assertEquals(0, queue.remainingCapacity());

    assertEquals("two", queue.remove());
    assertEquals(2, queue.size());
    assertEquals(1, queue.remainingCapacity());
  }

  public void testNullPointerExceptions() {
    NullPointerTester tester = new NullPointerTester();
    tester.testAllPublicStaticMethods(EvictingQueue.class);
    tester.testAllPublicConstructors(EvictingQueue.class);
    EvictingQueue<String> queue = EvictingQueue.create(5);
    // The queue must be non-empty so it throws a NPE correctly
    queue.add("one");
    tester.testAllPublicInstanceMethods(queue);
  }

  public void testSerialization() {
    EvictingQueue<String> original = EvictingQueue.create(5);
    original.add("one");
    original.add("two");
    original.add("three");

    EvictingQueue<String> copy = SerializableTester.reserialize(original);
    assertEquals(copy.maxSize, original.maxSize);
    assertEquals("one", copy.remove());
    assertEquals("two", copy.remove());
    assertEquals("three", copy.remove());
    assertTrue(copy.isEmpty());
  }
}
