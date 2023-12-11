/*
 * Copyright (C) 2014 The Guava Authors
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

import static com.google.common.util.concurrent.GeneratedMonitorTest.startThread;
import static com.google.common.util.concurrent.Uninterruptibles.joinUninterruptibly;
import static org.junit.Assert.assertThrows;

import com.google.common.util.concurrent.GeneratedMonitorTest.FlagGuard;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import junit.framework.TestCase;

/**
 * Supplemental tests for {@link Monitor}.
 *
 * <p>This test class contains various test cases that don't fit into the test case generation in
 * {@link GeneratedMonitorTest}.
 *
 * @author Justin T. Sampson
 */
public class SupplementalMonitorTest extends TestCase {

  public void testLeaveWithoutEnterThrowsIMSE() {
    Monitor monitor = new Monitor();
    assertThrows(IllegalMonitorStateException.class, () -> monitor.leave());
  }

  public void testGetWaitQueueLengthWithWrongMonitorThrowsIMSE() {
    Monitor monitor1 = new Monitor();
    Monitor monitor2 = new Monitor();
    FlagGuard guard = new FlagGuard(monitor2);
    assertThrows(IllegalMonitorStateException.class, () -> monitor1.getWaitQueueLength(guard));
  }

  public void testHasWaitersWithWrongMonitorThrowsIMSE() {
    Monitor monitor1 = new Monitor();
    Monitor monitor2 = new Monitor();
    FlagGuard guard = new FlagGuard(monitor2);
    assertThrows(IllegalMonitorStateException.class, () -> monitor1.hasWaiters(guard));
  }

  public void testNullMonitorInGuardConstructorThrowsNPE() {
    assertThrows(NullPointerException.class, () -> new FlagGuard(null));
  }

  public void testIsFair() {
    assertTrue(new Monitor(true).isFair());
    assertFalse(new Monitor(false).isFair());
  }

  public void testOccupiedMethods() {
    Monitor monitor = new Monitor();
    verifyOccupiedMethodsInCurrentThread(monitor, false, false, 0);
    verifyOccupiedMethodsInAnotherThread(monitor, false, false, 0);
    monitor.enter();
    try {
      verifyOccupiedMethodsInCurrentThread(monitor, true, true, 1);
      verifyOccupiedMethodsInAnotherThread(monitor, true, false, 0);
      monitor.enter();
      try {
        verifyOccupiedMethodsInCurrentThread(monitor, true, true, 2);
        verifyOccupiedMethodsInAnotherThread(monitor, true, false, 0);
      } finally {
        monitor.leave();
      }
      verifyOccupiedMethodsInCurrentThread(monitor, true, true, 1);
      verifyOccupiedMethodsInAnotherThread(monitor, true, false, 0);
    } finally {
      monitor.leave();
    }
    verifyOccupiedMethodsInCurrentThread(monitor, false, false, 0);
    verifyOccupiedMethodsInAnotherThread(monitor, false, false, 0);
  }

  private static void verifyOccupiedMethodsInCurrentThread(
      Monitor monitor,
      boolean expectedIsOccupied,
      boolean expectedIsOccupiedByCurrentThread,
      int expectedOccupiedDepth) {
    assertEquals(expectedIsOccupied, monitor.isOccupied());
    assertEquals(expectedIsOccupiedByCurrentThread, monitor.isOccupiedByCurrentThread());
    assertEquals(expectedOccupiedDepth, monitor.getOccupiedDepth());
  }

  private static void verifyOccupiedMethodsInAnotherThread(
      final Monitor monitor,
      boolean expectedIsOccupied,
      boolean expectedIsOccupiedByCurrentThread,
      int expectedOccupiedDepth) {
    final AtomicBoolean actualIsOccupied = new AtomicBoolean();
    final AtomicBoolean actualIsOccupiedByCurrentThread = new AtomicBoolean();
    final AtomicInteger actualOccupiedDepth = new AtomicInteger();
    final AtomicReference<Throwable> thrown = new AtomicReference<>();
    joinUninterruptibly(
        startThread(
            new Runnable() {
              @Override
              public void run() {
                try {
                  actualIsOccupied.set(monitor.isOccupied());
                  actualIsOccupiedByCurrentThread.set(monitor.isOccupiedByCurrentThread());
                  actualOccupiedDepth.set(monitor.getOccupiedDepth());
                } catch (Throwable t) {
                  thrown.set(t);
                }
              }
            }));
    assertNull(thrown.get());
    assertEquals(expectedIsOccupied, actualIsOccupied.get());
    assertEquals(expectedIsOccupiedByCurrentThread, actualIsOccupiedByCurrentThread.get());
    assertEquals(expectedOccupiedDepth, actualOccupiedDepth.get());
  }
}
