/*
 * Copyright (C) 2010 The Guava Authors
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

package com.google.common.util.concurrent;

import com.google.common.testing.NullPointerTester;
import com.google.common.testing.TearDownStack;
import java.util.Random;
import junit.framework.TestCase;

/**
 * Tests for {@link Monitor}, either interruptible or uninterruptible.
 *
 * @author Justin T. Sampson
 */
public abstract class MonitorTestCase extends TestCase {

  public class TestGuard extends Monitor.Guard {
    private volatile boolean satisfied;

    public TestGuard(boolean satisfied) {
      super(MonitorTestCase.this.monitor);
      this.satisfied = satisfied;
    }

    @Override
    public boolean isSatisfied() {
      return this.satisfied;
    }

    public void setSatisfied(boolean satisfied) {
      this.satisfied = satisfied;
    }
  }

  private final boolean interruptible;
  private Monitor monitor;
  private final TearDownStack tearDownStack = new TearDownStack(true);
  private TestThread<Monitor> thread1;
  private TestThread<Monitor> thread2;

  protected MonitorTestCase(boolean interruptible) {
    this.interruptible = interruptible;
  }

  @Override
  protected final void setUp() throws Exception {
    boolean fair = new Random().nextBoolean();
    monitor = new Monitor(fair);
    tearDownStack.addTearDown(thread1 = new TestThread<>(monitor, "TestThread #1"));
    tearDownStack.addTearDown(thread2 = new TestThread<>(monitor, "TestThread #2"));
  }

  @Override
  protected final void tearDown() {
    tearDownStack.runTearDown();
  }

  private String enter() {
    return interruptible ? "enterInterruptibly" : "enter";
  }

  private String tryEnter() {
    return "tryEnter";
  }

  private String enterIf() {
    return interruptible ? "enterIfInterruptibly" : "enterIf";
  }

  private String tryEnterIf() {
    return "tryEnterIf";
  }

  private String enterWhen() {
    return interruptible ? "enterWhen" : "enterWhenUninterruptibly";
  }

  private String waitFor() {
    return interruptible ? "waitFor" : "waitForUninterruptibly";
  }

  private String leave() {
    return "leave";
  }

  public final void testMutualExclusion() throws Exception {
    thread1.callAndAssertReturns(enter());
    thread2.callAndAssertBlocks(enter());
    thread1.callAndAssertReturns(leave());
    thread2.assertPriorCallReturns(enter());
  }

  public final void testTryEnter() throws Exception {
    thread1.callAndAssertReturns(true, tryEnter());
    thread2.callAndAssertReturns(false, tryEnter());
    thread1.callAndAssertReturns(true, tryEnter());
    thread2.callAndAssertReturns(false, tryEnter());
    thread1.callAndAssertReturns(leave());
    thread2.callAndAssertReturns(false, tryEnter());
    thread1.callAndAssertReturns(leave());
    thread2.callAndAssertReturns(true, tryEnter());
  }

  public final void testSystemStateMethods() throws Exception {
    checkSystemStateMethods(0);
    thread1.callAndAssertReturns(enter());
    checkSystemStateMethods(1);
    thread1.callAndAssertReturns(enter());
    checkSystemStateMethods(2);
    thread1.callAndAssertReturns(leave());
    checkSystemStateMethods(1);
    thread1.callAndAssertReturns(leave());
    checkSystemStateMethods(0);
  }

  private void checkSystemStateMethods(int enterCount) throws Exception {
    thread1.callAndAssertReturns(enterCount != 0, "isOccupied");
    thread1.callAndAssertReturns(enterCount != 0, "isOccupiedByCurrentThread");
    thread1.callAndAssertReturns(enterCount, "getOccupiedDepth");

    thread2.callAndAssertReturns(enterCount != 0, "isOccupied");
    thread2.callAndAssertReturns(false, "isOccupiedByCurrentThread");
    thread2.callAndAssertReturns(0, "getOccupiedDepth");
  }

  public final void testEnterWhen_initiallyTrue() throws Exception {
    TestGuard guard = new TestGuard(true);
    thread1.callAndAssertReturns(enterWhen(), guard);
    // same as above but with the new syntax
    thread1.callAndAssertReturns(enterWhen(), monitor.newGuard(() -> true));
  }

  public final void testEnterWhen_initiallyFalse() throws Exception {
    TestGuard guard = new TestGuard(false);
    thread1.callAndAssertWaits(enterWhen(), guard);
    monitor.enter();
    guard.setSatisfied(true);
    monitor.leave();
    thread1.assertPriorCallReturns(enterWhen());
  }

  public final void testEnterWhen_alreadyOccupied() throws Exception {
    TestGuard guard = new TestGuard(true);
    thread2.callAndAssertReturns(enter());
    thread1.callAndAssertBlocks(enterWhen(), guard);
    thread2.callAndAssertReturns(leave());
    thread1.assertPriorCallReturns(enterWhen());
  }

  public final void testEnterIf_initiallyTrue() throws Exception {
    TestGuard guard = new TestGuard(true);
    thread1.callAndAssertReturns(true, enterIf(), guard);
    thread2.callAndAssertBlocks(enter());
  }

  public final void testEnterIf_initiallyFalse() throws Exception {
    TestGuard guard = new TestGuard(false);
    thread1.callAndAssertReturns(false, enterIf(), guard);
    thread2.callAndAssertReturns(enter());
  }

  public final void testEnterIf_alreadyOccupied() throws Exception {
    TestGuard guard = new TestGuard(true);
    thread2.callAndAssertReturns(enter());
    thread1.callAndAssertBlocks(enterIf(), guard);
    thread2.callAndAssertReturns(leave());
    thread1.assertPriorCallReturns(true, enterIf());
  }

  public final void testTryEnterIf_initiallyTrue() throws Exception {
    TestGuard guard = new TestGuard(true);
    thread1.callAndAssertReturns(true, tryEnterIf(), guard);
    thread2.callAndAssertBlocks(enter());
  }

  public final void testTryEnterIf_initiallyFalse() throws Exception {
    TestGuard guard = new TestGuard(false);
    thread1.callAndAssertReturns(false, tryEnterIf(), guard);
    thread2.callAndAssertReturns(enter());
  }

  public final void testTryEnterIf_alreadyOccupied() throws Exception {
    TestGuard guard = new TestGuard(true);
    thread2.callAndAssertReturns(enter());
    thread1.callAndAssertReturns(false, tryEnterIf(), guard);
  }

  public final void testWaitFor_initiallyTrue() throws Exception {
    TestGuard guard = new TestGuard(true);
    thread1.callAndAssertReturns(enter());
    thread1.callAndAssertReturns(waitFor(), guard);
  }

  public final void testWaitFor_initiallyFalse() throws Exception {
    TestGuard guard = new TestGuard(false);
    thread1.callAndAssertReturns(enter());
    thread1.callAndAssertWaits(waitFor(), guard);
    monitor.enter();
    guard.setSatisfied(true);
    monitor.leave();
    thread1.assertPriorCallReturns(waitFor());
  }

  public final void testWaitFor_withoutEnter() throws Exception {
    TestGuard guard = new TestGuard(true);
    thread1.callAndAssertThrows(IllegalMonitorStateException.class, waitFor(), guard);
  }

  public void testNulls() {
    monitor.enter(); // Inhibit IllegalMonitorStateException
    new NullPointerTester()
        .setDefault(Monitor.Guard.class, new TestGuard(true))
        .testAllPublicInstanceMethods(monitor);
  }

  // TODO: Test enter(long, TimeUnit).
  // TODO: Test enterWhen(Guard, long, TimeUnit).
  // TODO: Test enterIf(Guard, long, TimeUnit).
  // TODO: Test waitFor(Guard, long, TimeUnit).
  // TODO: Test getQueueLength().
  // TODO: Test hasQueuedThreads().
  // TODO: Test getWaitQueueLength(Guard).
  // TODO: Test automatic signaling before leave, waitFor, and reentrant enterWhen.
  // TODO: Test blocking to re-enter monitor after being signaled.
  // TODO: Test interrupts with both interruptible and uninterruptible monitor.
  // TODO: Test multiple waiters: If guard is still satisfied, signal next waiter.
  // TODO: Test multiple waiters: If guard is no longer satisfied, do not signal next waiter.

}
