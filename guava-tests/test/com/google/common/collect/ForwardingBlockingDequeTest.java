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

import java.util.concurrent.BlockingDeque;
import java.util.concurrent.TimeUnit;

/**
 * Test for {@link ForwardingBlockingDeque}
 *
 * @author Emily Soldal
 */
public class ForwardingBlockingDequeTest extends ForwardingTestCase {
  private BlockingDeque<String> forward;

  /*
   * Class parameters must be raw, so we can't create a proxy with generic
   * type arguments. The created proxy only records calls and returns null, so
   * the type is irrelevant at runtime.
   */
  @SuppressWarnings("unchecked")
  @Override protected void setUp() throws Exception {
    super.setUp();
    final BlockingDeque<String> deque = createProxyInstance(BlockingDeque.class);
    forward = new ForwardingBlockingDeque<String>() {
      @Override protected BlockingDeque<String> delegate() {
        return deque;
      }
    };
  }

  public void testRemainingCapacity() {
    forward.remainingCapacity();
    assertEquals("[remainingCapacity]", getCalls());
  }

  public void testPutFirst_T() throws InterruptedException {
    forward.putFirst("asf");
    assertEquals("[putFirst(Object)]", getCalls());
  }

  public void testPutLast_T() throws InterruptedException {
    forward.putFirst("asf");
    assertEquals("[putFirst(Object)]", getCalls());
  }

  public void testOfferFirst_T() throws InterruptedException {
    forward.offerFirst("asf", 2L, TimeUnit.SECONDS);
    assertEquals("[offerFirst(Object,long,TimeUnit)]", getCalls());
  }

  public void testOfferLast_T() throws InterruptedException {
    forward.offerLast("asf", 2L, TimeUnit.SECONDS);
    assertEquals("[offerLast(Object,long,TimeUnit)]", getCalls());
  }

  public void testTakeFirst() throws InterruptedException {
    forward.takeFirst();
    assertEquals("[takeFirst]", getCalls());
  }

  public void testTakeLast() throws InterruptedException {
    forward.takeLast();
    assertEquals("[takeLast]", getCalls());
  }

  public void testPollFirst() throws InterruptedException {
    forward.pollFirst(2L, TimeUnit.SECONDS);
    assertEquals("[pollFirst(long,TimeUnit)]", getCalls());
  }

  public void testPollLast() throws InterruptedException {
    forward.pollLast(2L, TimeUnit.SECONDS);
    assertEquals("[pollLast(long,TimeUnit)]", getCalls());
  }

  public void testPut_T() throws InterruptedException {
    forward.put("asf");
    assertEquals("[put(Object)]", getCalls());
  }

  public void testOffer_T() throws InterruptedException {
    forward.offer("asf", 2L, TimeUnit.SECONDS);
    assertEquals("[offer(Object,long,TimeUnit)]", getCalls());
  }

  public void testTake() throws InterruptedException {
    forward.take();
    assertEquals("[take]", getCalls());
  }

  public void testPoll() throws InterruptedException {
    forward.poll(2L, TimeUnit.SECONDS);
    assertEquals("[poll(long,TimeUnit)]", getCalls());
  }

  public void testDrainTo_T() {
    forward.drainTo(Lists.newArrayList());
    assertEquals("[drainTo(Collection)]", getCalls());
  }

  public void testDrainTo_T_maxElements() {
    forward.drainTo(Lists.newArrayList(), 3);
    assertEquals("[drainTo(Collection,int)]", getCalls());
  }
}
