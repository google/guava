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

import java.util.Collections;
import java.util.Deque;

/**
 * Tests for {@code ForwardingDeque}.
 *
 * @author Kurt Alfred Kluever
 */
public class ForwardingDequeTest extends ForwardingTestCase {
  private Deque<String> forward;
  
  /*
   * Class parameters must be raw, so we can't create a proxy with generic
   * type arguments. The created proxy only records calls and returns null, so
   * the type is irrelevant at runtime.
   */
  @SuppressWarnings("unchecked")
  @Override protected void setUp() throws Exception {
    super.setUp();
    final Deque<String> deque = createProxyInstance(Deque.class);
    forward = new ForwardingDeque<String>() {
      @Override protected Deque<String> delegate() {
        return deque;
      }
    };
  }

  public void testAdd_T() {
    forward.add("asdf");
    assertEquals("[add(Object)]", getCalls());
  }

  public void testAddFirst_T() {
    forward.addFirst("asdf");
    assertEquals("[addFirst(Object)]", getCalls());
  }

  public void testAddLast_T() {
    forward.addLast("asdf");
    assertEquals("[addLast(Object)]", getCalls());
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

  public void testDescendingIterator() {
    forward.descendingIterator();
    assertEquals("[descendingIterator]", getCalls());
  }

  public void testElement() {
    forward.element();
    assertEquals("[element]", getCalls());
  }

  public void testGetFirst() {
    forward.getFirst();
    assertEquals("[getFirst]", getCalls());
  }

  public void testGetLast() {
    forward.getLast();
    assertEquals("[getLast]", getCalls());
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

  public void testOfferFirst_T() {
    forward.offerFirst("asdf");
    assertEquals("[offerFirst(Object)]", getCalls());
  }

  public void testOfferLast_T() {
    forward.offerLast("asdf");
    assertEquals("[offerLast(Object)]", getCalls());
  }

  public void testPeek() {
    forward.peek();
    assertEquals("[peek]", getCalls());
  }

  public void testPeekFirst() {
    forward.peekFirst();
    assertEquals("[peekFirst]", getCalls());
  }

  public void testPeekLast() {
    forward.peekLast();
    assertEquals("[peekLast]", getCalls());
  }

  public void testPoll() {
    forward.poll();
    assertEquals("[poll]", getCalls());
  }

  public void testPollFirst() {
    forward.pollFirst();
    assertEquals("[pollFirst]", getCalls());
  }

  public void testPollLast() {
    forward.pollLast();
    assertEquals("[pollLast]", getCalls());
  }

  public void testPop() {
    forward.pop();
    assertEquals("[pop]", getCalls());
  }

  public void testPush_Object() {
    forward.push("asdf");
    assertEquals("[push(Object)]", getCalls());
  }

  public void testRemove() {
    forward.remove();
    assertEquals("[remove]", getCalls());
  }

  public void testRemoveFirst() {
    forward.removeFirst();
    assertEquals("[removeFirst]", getCalls());
  }

  public void testRemoveLast() {
    forward.removeLast();
    assertEquals("[removeLast]", getCalls());
  }

  public void testRemove_Object() {
    forward.remove(Object.class);
    assertEquals("[remove(Object)]", getCalls());
  }

  public void testRemoveFirstOccurrence_Object() {
    forward.removeFirstOccurrence(Object.class);
    assertEquals("[removeFirstOccurrence(Object)]", getCalls());
  }

  public void testRemoveLastOccurrence_Object() {
    forward.removeLastOccurrence(Object.class);
    assertEquals("[removeLastOccurrence(Object)]", getCalls());
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
