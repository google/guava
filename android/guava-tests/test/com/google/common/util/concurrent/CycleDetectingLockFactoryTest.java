/*
 * Copyright (C) 2011 The Guava Authors
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

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

import com.google.common.base.Joiner;
import com.google.common.util.concurrent.CycleDetectingLockFactory.Policies;
import com.google.common.util.concurrent.CycleDetectingLockFactory.Policy;
import com.google.common.util.concurrent.CycleDetectingLockFactory.PotentialDeadlockException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import junit.framework.TestCase;

/**
 * Unittests for {@link CycleDetectingLockFactory}.
 *
 * @author Darick Tong
 */
public class CycleDetectingLockFactoryTest extends TestCase {

  private ReentrantLock lockA;
  private ReentrantLock lockB;
  private ReentrantLock lockC;
  private ReentrantReadWriteLock.ReadLock readLockA;
  private ReentrantReadWriteLock.ReadLock readLockB;
  private ReentrantReadWriteLock.ReadLock readLockC;
  private ReentrantReadWriteLock.WriteLock writeLockA;
  private ReentrantReadWriteLock.WriteLock writeLockB;
  private ReentrantReadWriteLock.WriteLock writeLockC;
  private ReentrantLock lock1;
  private ReentrantLock lock2;
  private ReentrantLock lock3;
  private ReentrantLock lock01;
  private ReentrantLock lock02;
  private ReentrantLock lock03;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    CycleDetectingLockFactory factory = CycleDetectingLockFactory.newInstance(Policies.THROW);
    lockA = factory.newReentrantLock("LockA");
    lockB = factory.newReentrantLock("LockB");
    lockC = factory.newReentrantLock("LockC");
    ReentrantReadWriteLock readWriteLockA = factory.newReentrantReadWriteLock("ReadWriteA");
    ReentrantReadWriteLock readWriteLockB = factory.newReentrantReadWriteLock("ReadWriteB");
    ReentrantReadWriteLock readWriteLockC = factory.newReentrantReadWriteLock("ReadWriteC");
    readLockA = readWriteLockA.readLock();
    readLockB = readWriteLockB.readLock();
    readLockC = readWriteLockC.readLock();
    writeLockA = readWriteLockA.writeLock();
    writeLockB = readWriteLockB.writeLock();
    writeLockC = readWriteLockC.writeLock();

    CycleDetectingLockFactory.WithExplicitOrdering<MyOrder> factory2 =
        newInstanceWithExplicitOrdering(MyOrder.class, Policies.THROW);
    lock1 = factory2.newReentrantLock(MyOrder.FIRST);
    lock2 = factory2.newReentrantLock(MyOrder.SECOND);
    lock3 = factory2.newReentrantLock(MyOrder.THIRD);

    CycleDetectingLockFactory.WithExplicitOrdering<OtherOrder> factory3 =
        newInstanceWithExplicitOrdering(OtherOrder.class, Policies.THROW);
    lock01 = factory3.newReentrantLock(OtherOrder.FIRST);
    lock02 = factory3.newReentrantLock(OtherOrder.SECOND);
    lock03 = factory3.newReentrantLock(OtherOrder.THIRD);
  }

  // In the unittest, create each ordered factory with its own set of lock
  // graph nodes (as opposed to using the static per-Enum map) to avoid
  // conflicts across different test runs.
  private <E extends Enum<E>>
      CycleDetectingLockFactory.WithExplicitOrdering<E> newInstanceWithExplicitOrdering(
          Class<E> enumClass, Policy policy) {
    return new CycleDetectingLockFactory.WithExplicitOrdering<E>(
        policy, CycleDetectingLockFactory.createNodes(enumClass));
  }

  public void testDeadlock_twoLocks() {
    // Establish an acquisition order of lockA -> lockB.
    lockA.lock();
    lockB.lock();
    lockA.unlock();
    lockB.unlock();

    // The opposite order should fail (Policies.THROW).
    PotentialDeadlockException firstException = null;
    lockB.lock();
    PotentialDeadlockException expected =
        assertThrows(PotentialDeadlockException.class, () -> lockA.lock());
    checkMessage(expected, "LockB -> LockA", "LockA -> LockB");
    firstException = expected;
    // Second time should also fail, with a cached causal chain.
    expected = assertThrows(PotentialDeadlockException.class, () -> lockA.lock());
    checkMessage(expected, "LockB -> LockA", "LockA -> LockB");
    // The causal chain should be cached.
    assertSame(firstException.getCause(), expected.getCause());
    // lockA should work after lockB is released.
    lockB.unlock();
    lockA.lock();
  }

  // Tests transitive deadlock detection.
  public void testDeadlock_threeLocks() {
    // Establish an ordering from lockA -> lockB.
    lockA.lock();
    lockB.lock();
    lockB.unlock();
    lockA.unlock();

    // Establish an ordering from lockB -> lockC.
    lockB.lock();
    lockC.lock();
    lockB.unlock();

    // lockC -> lockA should fail.
    PotentialDeadlockException expected =
        assertThrows(PotentialDeadlockException.class, () -> lockA.lock());
    checkMessage(expected, "LockC -> LockA", "LockB -> LockC", "LockA -> LockB");
  }

  public void testReentrancy_noDeadlock() {
    lockA.lock();
    lockB.lock();
    lockA.lock(); // Should not assert on lockB -> reentrant(lockA)
  }

  public void testExplicitOrdering_noViolations() {
    lock1.lock();
    lock3.lock();
    lock3.unlock();
    lock2.lock();
    lock3.lock();
  }

  public void testExplicitOrdering_violations() {
    lock3.lock();
    PotentialDeadlockException expected =
        assertThrows(PotentialDeadlockException.class, () -> lock2.lock());
    checkMessage(expected, "MyOrder.THIRD -> MyOrder.SECOND");

    expected = assertThrows(PotentialDeadlockException.class, () -> lock1.lock());
    checkMessage(expected, "MyOrder.THIRD -> MyOrder.FIRST");

    lock3.unlock();
    lock2.lock();

    expected = assertThrows(PotentialDeadlockException.class, () -> lock1.lock());
    checkMessage(expected, "MyOrder.SECOND -> MyOrder.FIRST");
  }

  public void testDifferentOrderings_noViolations() {
    lock3.lock(); // MyOrder, ordinal() == 3
    lock01.lock(); // OtherOrder, ordinal() == 1
  }

  public void testExplicitOrderings_generalCycleDetection() {
    lock3.lock(); // MyOrder, ordinal() == 3
    lock01.lock(); // OtherOrder, ordinal() == 1

    lock3.unlock();
    PotentialDeadlockException expected =
        assertThrows(PotentialDeadlockException.class, () -> lock3.lock());
    checkMessage(
        expected, "OtherOrder.FIRST -> MyOrder.THIRD", "MyOrder.THIRD -> OtherOrder.FIRST");
    lockA.lock();
    lock01.unlock();
    lockB.lock();
    lockA.unlock();

    expected = assertThrows(PotentialDeadlockException.class, () -> lock01.lock());
    checkMessage(
        expected, "LockB -> OtherOrder.FIRST", "LockA -> LockB", "OtherOrder.FIRST -> LockA");
  }

  public void testExplicitOrdering_cycleWithUnorderedLock() {
    Lock myLock = CycleDetectingLockFactory.newInstance(Policies.THROW).newReentrantLock("MyLock");
    lock03.lock();
    myLock.lock();
    lock03.unlock();

    PotentialDeadlockException expected =
        assertThrows(PotentialDeadlockException.class, () -> lock01.lock());
    checkMessage(
        expected,
        "MyLock -> OtherOrder.FIRST",
        "OtherOrder.THIRD -> MyLock",
        "OtherOrder.FIRST -> OtherOrder.THIRD");
  }

  public void testExplicitOrdering_reentrantAcquisition() {
    CycleDetectingLockFactory.WithExplicitOrdering<OtherOrder> factory =
        newInstanceWithExplicitOrdering(OtherOrder.class, Policies.THROW);
    Lock lockA = factory.newReentrantReadWriteLock(OtherOrder.FIRST).readLock();
    Lock lockB = factory.newReentrantLock(OtherOrder.SECOND);

    lockA.lock();
    lockA.lock();
    lockB.lock();
    lockB.lock();
    lockA.unlock();
    lockA.unlock();
    lockB.unlock();
    lockB.unlock();
  }

  public void testExplicitOrdering_acquiringMultipleLocksWithSameRank() {
    CycleDetectingLockFactory.WithExplicitOrdering<OtherOrder> factory =
        newInstanceWithExplicitOrdering(OtherOrder.class, Policies.THROW);
    Lock lockA = factory.newReentrantLock(OtherOrder.FIRST);
    Lock lockB = factory.newReentrantReadWriteLock(OtherOrder.FIRST).readLock();

    lockA.lock();
    assertThrows(IllegalStateException.class, () -> lockB.lock());

    lockA.unlock();
    lockB.lock();
  }

  public void testReadLock_deadlock() {
    readLockA.lock(); // Establish an ordering from readLockA -> lockB.
    lockB.lock();
    lockB.unlock();
    readLockA.unlock();

    lockB.lock();
    PotentialDeadlockException expected =
        assertThrows(PotentialDeadlockException.class, () -> readLockA.lock());
    checkMessage(expected, "LockB -> ReadWriteA", "ReadWriteA -> LockB");
  }

  public void testReadLock_transitive() {
    readLockA.lock(); // Establish an ordering from readLockA -> lockB.
    lockB.lock();
    lockB.unlock();
    readLockA.unlock();

    // Establish an ordering from lockB -> readLockC.
    lockB.lock();
    readLockC.lock();
    lockB.unlock();
    readLockC.unlock();

    // readLockC -> readLockA
    readLockC.lock();
    PotentialDeadlockException expected =
        assertThrows(PotentialDeadlockException.class, () -> readLockA.lock());
    checkMessage(
        expected, "ReadWriteC -> ReadWriteA", "LockB -> ReadWriteC", "ReadWriteA -> LockB");
  }

  public void testWriteLock_threeLockDeadLock() {
    // Establish an ordering from writeLockA -> writeLockB.
    writeLockA.lock();
    writeLockB.lock();
    writeLockB.unlock();
    writeLockA.unlock();

    // Establish an ordering from writeLockB -> writeLockC.
    writeLockB.lock();
    writeLockC.lock();
    writeLockB.unlock();

    // writeLockC -> writeLockA should fail.
    PotentialDeadlockException expected =
        assertThrows(PotentialDeadlockException.class, () -> writeLockA.lock());
    checkMessage(
        expected,
        "ReadWriteC -> ReadWriteA",
        "ReadWriteB -> ReadWriteC",
        "ReadWriteA -> ReadWriteB");
  }

  public void testWriteToReadLockDowngrading() {
    writeLockA.lock(); // writeLockA downgrades to readLockA
    readLockA.lock();
    writeLockA.unlock();

    lockB.lock(); // readLockA -> lockB
    readLockA.unlock();

    // lockB -> writeLockA should fail
    PotentialDeadlockException expected =
        assertThrows(PotentialDeadlockException.class, () -> writeLockA.lock());
    checkMessage(expected, "LockB -> ReadWriteA", "ReadWriteA -> LockB");
  }

  public void testReadWriteLockDeadlock() {
    writeLockA.lock(); // Establish an ordering from writeLockA -> lockB
    lockB.lock();
    writeLockA.unlock();
    lockB.unlock();

    // lockB -> readLockA should fail.
    lockB.lock();
    PotentialDeadlockException expected =
        assertThrows(PotentialDeadlockException.class, () -> readLockA.lock());
    checkMessage(expected, "LockB -> ReadWriteA", "ReadWriteA -> LockB");
  }

  public void testReadWriteLockDeadlock_transitive() {
    readLockA.lock(); // Establish an ordering from readLockA -> lockB
    lockB.lock();
    readLockA.unlock();
    lockB.unlock();

    // Establish an ordering from lockB -> lockC
    lockB.lock();
    lockC.lock();
    lockB.unlock();
    lockC.unlock();

    // lockC -> writeLockA should fail.
    lockC.lock();
    PotentialDeadlockException expected =
        assertThrows(PotentialDeadlockException.class, () -> writeLockA.lock());
    checkMessage(expected, "LockC -> ReadWriteA", "LockB -> LockC", "ReadWriteA -> LockB");
  }

  public void testReadWriteLockDeadlock_treatedEquivalently() {
    readLockA.lock(); // readLockA -> writeLockB
    writeLockB.lock();
    readLockA.unlock();
    writeLockB.unlock();

    // readLockB -> writeLockA should fail.
    readLockB.lock();
    PotentialDeadlockException expected =
        assertThrows(PotentialDeadlockException.class, () -> writeLockA.lock());
    checkMessage(expected, "ReadWriteB -> ReadWriteA", "ReadWriteA -> ReadWriteB");
  }

  public void testDifferentLockFactories() {
    CycleDetectingLockFactory otherFactory = CycleDetectingLockFactory.newInstance(Policies.WARN);
    ReentrantLock lockD = otherFactory.newReentrantLock("LockD");

    // lockA -> lockD
    lockA.lock();
    lockD.lock();
    lockA.unlock();
    lockD.unlock();

    // lockD -> lockA should fail even though lockD is from a different factory.
    lockD.lock();
    PotentialDeadlockException expected =
        assertThrows(PotentialDeadlockException.class, () -> lockA.lock());
    checkMessage(expected, "LockD -> LockA", "LockA -> LockD");
  }

  public void testDifferentLockFactories_policyExecution() {
    CycleDetectingLockFactory otherFactory = CycleDetectingLockFactory.newInstance(Policies.WARN);
    ReentrantLock lockD = otherFactory.newReentrantLock("LockD");

    // lockD -> lockA
    lockD.lock();
    lockA.lock();
    lockA.unlock();
    lockD.unlock();

    // lockA -> lockD should warn but otherwise succeed because lockD was
    // created by a factory with the WARN policy.
    lockA.lock();
    lockD.lock();
  }

  public void testReentrantLock_tryLock() throws Exception {
    LockingThread thread = new LockingThread(lockA);
    thread.start();

    thread.waitUntilHoldingLock();
    assertFalse(lockA.tryLock());

    thread.releaseLockAndFinish();
    assertTrue(lockA.tryLock());
  }

  public void testReentrantWriteLock_tryLock() throws Exception {
    LockingThread thread = new LockingThread(writeLockA);
    thread.start();

    thread.waitUntilHoldingLock();
    assertFalse(writeLockA.tryLock());
    assertFalse(readLockA.tryLock());

    thread.releaseLockAndFinish();
    assertTrue(writeLockA.tryLock());
    assertTrue(readLockA.tryLock());
  }

  public void testReentrantReadLock_tryLock() throws Exception {
    LockingThread thread = new LockingThread(readLockA);
    thread.start();

    thread.waitUntilHoldingLock();
    assertFalse(writeLockA.tryLock());
    assertTrue(readLockA.tryLock());
    readLockA.unlock();

    thread.releaseLockAndFinish();
    assertTrue(writeLockA.tryLock());
    assertTrue(readLockA.tryLock());
  }

  private static class LockingThread extends Thread {
    final CountDownLatch locked = new CountDownLatch(1);
    final CountDownLatch finishLatch = new CountDownLatch(1);
    final Lock lock;

    LockingThread(Lock lock) {
      this.lock = lock;
    }

    @Override
    public void run() {
      lock.lock();
      try {
        locked.countDown();
        finishLatch.await(1, TimeUnit.MINUTES);
      } catch (InterruptedException e) {
        fail(e.toString());
      } finally {
        lock.unlock();
      }
    }

    void waitUntilHoldingLock() throws InterruptedException {
      locked.await(1, TimeUnit.MINUTES);
    }

    void releaseLockAndFinish() throws InterruptedException {
      finishLatch.countDown();
      this.join(10000);
      assertFalse(this.isAlive());
    }
  }

  public void testReentrantReadWriteLock_implDoesNotExposeShadowedLocks() {
    assertEquals(
        "Unexpected number of public methods in ReentrantReadWriteLock. "
            + "The correctness of CycleDetectingReentrantReadWriteLock depends on "
            + "the fact that the shadowed ReadLock and WriteLock are never used or "
            + "exposed by the superclass implementation. If the implementation has "
            + "changed, the code must be re-inspected to ensure that the "
            + "assumption is still valid.",
        24,
        ReentrantReadWriteLock.class.getMethods().length);
  }

  private enum MyOrder {
    FIRST,
    SECOND,
    THIRD;
  }

  private enum OtherOrder {
    FIRST,
    SECOND,
    THIRD;
  }

  // Given a sequence of lock acquisition descriptions
  // (e.g. "LockA -> LockB", "LockB -> LockC", ...)
  // Checks that the exception.getMessage() matches a regex of the form:
  // "LockA -> LockB \b.*\b LockB -> LockC \b.*\b LockC -> LockA"
  private void checkMessage(IllegalStateException exception, String... expectedLockCycle) {
    String regex = Joiner.on("\\b.*\\b").join(expectedLockCycle);
    assertThat(exception).hasMessageThat().containsMatch(regex);
  }
}
