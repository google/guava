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

import java.util.ListIterator;

/**
 * Tests for {@code ForwardingListIterator}.
 *
 * @author Robert Konigsberg
 */
public class ForwardingListIteratorTest extends ForwardingTestCase {

  private ForwardingListIterator<String> forward;

  @Override public void setUp() throws Exception {
    super.setUp();
    /*
     * Class parameters must be raw, so we can't create a proxy with generic
     * type arguments. The created proxy only records calls and returns null, so
     * the type is irrelevant at runtime.
     */
    @SuppressWarnings("unchecked")
    final ListIterator<String> li = createProxyInstance(ListIterator.class);
    forward = new ForwardingListIterator<String>() {
      @Override protected ListIterator<String> delegate() {
        return li;
      }
    };
  }

  public void testAdd_T() {
    forward.add("asdf");
    assertEquals("[add(Object)]", getCalls());
  }

  public void testHasNext() {
    forward.hasNext();
    assertEquals("[hasNext]", getCalls());
  }

  public void testHasPrevious() {
    forward.hasPrevious();
    assertEquals("[hasPrevious]", getCalls());
  }

  public void testNext() {
    forward.next();
    assertEquals("[next]", getCalls());
  }

  public void testNextIndex() {
    forward.nextIndex();
    assertEquals("[nextIndex]", getCalls());
  }

  public void testPrevious() {
    forward.previous();
    assertEquals("[previous]", getCalls());
  }

  public void testPreviousIndex() {
    forward.previousIndex();
    assertEquals("[previousIndex]", getCalls());
  }

  public void testRemove() {
    forward.remove();
    assertEquals("[remove]", getCalls());
  }

  public void testSet_T() {
    forward.set("asdf");
    assertEquals("[set(Object)]", getCalls());
  }
}
