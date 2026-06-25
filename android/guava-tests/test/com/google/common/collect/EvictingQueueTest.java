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

import static com.google.common.testing.SerializableTester.reserialize;
import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.annotations.J2ktIncompatible;
import com.google.common.testing.NullPointerTester;
import java.util.AbstractList;
import java.util.List;
import java.util.NoSuchElementException;
import junit.framework.TestCase;
import org.jspecify.annotations.NullMarked;

/**
 * Tests for {@link EvictingQueue}.
 *
 * @author Kurt Alfred Kluever
 */
@GwtCompatible
@NullMarked
public class EvictingQueueTest extends TestCase {

  public void testCreateWithNegativeSize() {
    assertThrows(IllegalArgumentException.class, () -> EvictingQueue.create(-1));
  }

  public void testCreateWithZeroSize() {
    EvictingQueue<String> queue = EvictingQueue.create(0);
    assertEquals(0, queue.size());

    assertTrue(queue.add("hi"));
    assertEquals(0, queue.size());

    assertTrue(queue.offer("hi"));
    assertEquals(0, queue.size());

    assertFalse(queue.remove("hi"));
    assertEquals(0, queue.size());

    assertThrows(NoSuchElementException.class, () -> queue.element());

    assertThat(queue.peek()).isNull();
    assertThat(queue.poll()).isNull();
    assertThrows(NoSuchElementException.class, () -> queue.remove());
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

  public void testEvictingAfterOne() {
    EvictingQueue<String> queue = EvictingQueue.create(1);
    assertEquals(0, queue.size());
    assertEquals(1, queue.remainingCapacity());

    assertTrue(queue.add("hi"));
    assertThat(queue.element()).isEqualTo("hi");
    assertThat(queue.peek()).isEqualTo("hi");
    assertEquals(1, queue.size());
    assertEquals(0, queue.remainingCapacity());

    assertTrue(queue.add("there"));
    assertThat(queue.element()).isEqualTo("there");
    assertThat(queue.peek()).isEqualTo("there");
    assertEquals(1, queue.size());
    assertEquals(0, queue.remainingCapacity());

    assertThat(queue.remove()).isEqualTo("there");
    assertEquals(0, queue.size());
    assertEquals(1, queue.remainingCapacity());
  }

  public void testEvictingAfterThree() {
    EvictingQueue<String> queue = EvictingQueue.create(3);
    assertEquals(0, queue.size());
    assertEquals(3, queue.remainingCapacity());

    assertTrue(queue.add("one"));
    assertTrue(queue.add("two"));
    assertTrue(queue.add("three"));
    assertThat(queue.element()).isEqualTo("one");
    assertThat(queue.peek()).isEqualTo("one");
    assertEquals(3, queue.size());
    assertEquals(0, queue.remainingCapacity());

    assertTrue(queue.add("four"));
    assertThat(queue.element()).isEqualTo("two");
    assertThat(queue.peek()).isEqualTo("two");
    assertEquals(3, queue.size());
    assertEquals(0, queue.remainingCapacity());

    assertThat(queue.remove()).isEqualTo("two");
    assertEquals(2, queue.size());
    assertEquals(1, queue.remainingCapacity());
  }

  public void testAddAll() {
    EvictingQueue<String> queue = EvictingQueue.create(3);
    assertEquals(0, queue.size());
    assertEquals(3, queue.remainingCapacity());

    assertTrue(queue.addAll(ImmutableList.of("one", "two", "three")));
    assertThat(queue.element()).isEqualTo("one");
    assertThat(queue.peek()).isEqualTo("one");
    assertEquals(3, queue.size());
    assertEquals(0, queue.remainingCapacity());

    assertTrue(queue.addAll(ImmutableList.of("four")));
    assertThat(queue.element()).isEqualTo("two");
    assertThat(queue.peek()).isEqualTo("two");
    assertEquals(3, queue.size());
    assertEquals(0, queue.remainingCapacity());

    assertThat(queue.remove()).isEqualTo("two");
    assertEquals(2, queue.size());
    assertEquals(1, queue.remainingCapacity());
  }

  public void testAddAll_largeList() {
    List<String> list = ImmutableList.of("one", "two", "three", "four", "five");
    List<String> misbehavingList =
        new AbstractList<String>() {
          @Override
          public int size() {
            return list.size();
          }

          @Override
          public String get(int index) {
            if (index < 2) {
              throw new AssertionError();
            }
            return list.get(index);
          }
        };

    EvictingQueue<String> queue = EvictingQueue.create(3);
    assertTrue(queue.addAll(misbehavingList));

    assertThat(queue.remove()).isEqualTo("three");
    assertThat(queue.remove()).isEqualTo("four");
    assertThat(queue.remove()).isEqualTo("five");
    assertTrue(queue.isEmpty());
  }

  @J2ktIncompatible
  @GwtIncompatible // NullPointerTester
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

    EvictingQueue<String> copy = reserialize(original);
    assertEquals(copy.maxSize, original.maxSize);
    assertThat(copy.remove()).isEqualTo("one");
    assertThat(copy.remove()).isEqualTo("two");
    assertThat(copy.remove()).isEqualTo("three");
    assertTrue(copy.isEmpty());
  }
}
