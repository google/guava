// Copyright 2011 Google Inc. All Rights Reserved.
package com.google.common.util.concurrent;

import static com.google.testing.util.MoreAsserts.assertContainsRegex;

import com.google.common.base.Joiner;
import com.google.common.util.concurrent.CycleDetectingLockFactory.Policy;
import com.google.common.util.concurrent.CycleDetectingLockFactory.PotentialDeadlockException;

import junit.framework.TestCase;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Unittests for {@link CycleDetectingLockFactory}.
 *
 * @author Darick Tong (darick@google.com)
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

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    CycleDetectingLockFactory factory =
        CycleDetectingLockFactory.newInstance(Policy.THROW);
    lockA = factory.newReentrantLock("LockA");
    lockB = factory.newReentrantLock("LockB");
    lockC = factory.newReentrantLock("LockC");
    ReentrantReadWriteLock readWriteLockA =
        factory.newReentrantReadWriteLock("ReadWriteA");
    ReentrantReadWriteLock readWriteLockB =
        factory.newReentrantReadWriteLock("ReadWriteB");
    ReentrantReadWriteLock readWriteLockC =
        factory.newReentrantReadWriteLock("ReadWriteC");
    readLockA = readWriteLockA.readLock();
    readLockB = readWriteLockB.readLock();
    readLockC = readWriteLockC.readLock();
    writeLockA = readWriteLockA.writeLock();
    writeLockB = readWriteLockB.writeLock();
    writeLockC = readWriteLockC.writeLock();
  }

  public void testDeadlock_twoLocks() {
    // Establish an acquisition order of lockA -> lockB.
    lockA.lock();
    lockB.lock();
    lockA.unlock();
    lockB.unlock();

    // The opposite order should fail (Policy.THROW).
    PotentialDeadlockException firstException = null;
    lockB.lock();
    try {
      lockA.lock();
      fail("Expected PotentialDeadlockException");
    } catch (PotentialDeadlockException expected) {
      checkMessage(expected, "LockB -> LockA", "LockA -> LockB");
      firstException = expected;
    }

    // Second time should also fail, with a cached causal chain.
    try {
      lockA.lock();
      fail("Expected PotentialDeadlockException");
    } catch (PotentialDeadlockException expected) {
      checkMessage(expected, "LockB -> LockA", "LockA -> LockB");
      // The causal chain should be cached.
      assertSame(firstException.getCause(), expected.getCause());
    }

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
    try {
      lockA.lock();
      fail("Expected PotentialDeadlockException");
    } catch (PotentialDeadlockException expected) {
      checkMessage(
          expected, "LockC -> LockA", "LockB -> LockC", "LockA -> LockB");
    }
  }

  public void testReentrancy_noDeadlock() {
    lockA.lock();
    lockB.lock();
    lockA.lock();  // Should not assert on lockB -> reentrant(lockA)
  }

  public void testReadLock_deadlock() {
    readLockA.lock();  // Establish an ordering from readLockA -> lockB.
    lockB.lock();
    lockB.unlock();
    readLockA.unlock();

    lockB.lock();
    try {
      readLockA.lock();
      fail("Expected PotentialDeadlockException");
    } catch (PotentialDeadlockException expected) {
      checkMessage(expected, "LockB -> ReadWriteA", "ReadWriteA -> LockB");
    }
  }

  public void testReadLock_transitive() {
    readLockA.lock();  // Establish an ordering from readLockA -> lockB.
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
    try {
      readLockA.lock();
      fail("Expected PotentialDeadlockException");
    } catch (PotentialDeadlockException expected) {
      checkMessage(
          expected,
          "ReadWriteC -> ReadWriteA",
          "LockB -> ReadWriteC",
          "ReadWriteA -> LockB");
    }
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
    try {
      writeLockA.lock();
      fail("Expected PotentialDeadlockException");
    } catch (PotentialDeadlockException expected) {
      checkMessage(
          expected,
          "ReadWriteC -> ReadWriteA",
          "ReadWriteB -> ReadWriteC",
          "ReadWriteA -> ReadWriteB");
    }
  }

  public void testWriteToReadLockDowngrading() {
    writeLockA.lock();  // writeLockA downgrades to readLockA
    readLockA.lock();
    writeLockA.unlock();

    lockB.lock();  // readLockA -> lockB
    readLockA.unlock();

    // lockB -> writeLockA should fail
    try {
      writeLockA.lock();
      fail("Expected PotentialDeadlockException");
    } catch (PotentialDeadlockException expected) {
      checkMessage(
          expected, "LockB -> ReadWriteA", "ReadWriteA -> LockB");
    }
  }

  public void testReadWriteLockDeadlock() {
    writeLockA.lock();  // Establish an ordering from writeLockA -> lockB
    lockB.lock();
    writeLockA.unlock();
    lockB.unlock();

    // lockB -> readLockA should fail.
    lockB.lock();
    try {
      readLockA.lock();
      fail("Expected PotentialDeadlockException");
    } catch (PotentialDeadlockException expected) {
      checkMessage(
          expected, "LockB -> ReadWriteA", "ReadWriteA -> LockB");
    }
  }

  public void testReadWriteLockDeadlock_transitive() {
    readLockA.lock();  // Establish an ordering from readLockA -> lockB
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
    try {
      writeLockA.lock();
      fail("Expected PotentialDeadlockException");
    } catch (PotentialDeadlockException expected) {
      checkMessage(
          expected,
          "LockC -> ReadWriteA",
          "LockB -> LockC",
          "ReadWriteA -> LockB");
    }
  }

  public void testReadWriteLockDeadlock_treatedEquivalently() {
    readLockA.lock();  // readLockA -> writeLockB
    writeLockB.lock();
    readLockA.unlock();
    writeLockB.unlock();

    // readLockB -> writeLockA should fail.
    readLockB.lock();
    try {
      writeLockA.lock();
      fail("Expected PotentialDeadlockException");
    } catch (PotentialDeadlockException expected) {
      checkMessage(
          expected, "ReadWriteB -> ReadWriteA", "ReadWriteA -> ReadWriteB");
    }
  }

  public void testDifferentLockFactories() {
    CycleDetectingLockFactory otherFactory =
        CycleDetectingLockFactory.newInstance(Policy.WARN);
    ReentrantLock lockD = otherFactory.newReentrantLock("LockD");

    // lockA -> lockD
    lockA.lock();
    lockD.lock();
    lockA.unlock();
    lockD.unlock();

    // lockD -> lockA should fail even though lockD is from a different factory.
    lockD.lock();
    try {
      lockA.lock();
      fail("Expected PotentialDeadlockException");
    } catch (PotentialDeadlockException expected) {
      checkMessage(expected, "LockD -> LockA", "LockA -> LockD");
    }
  }

  public void testDifferentLockFactories_policyExecution() {
    CycleDetectingLockFactory otherFactory =
        CycleDetectingLockFactory.newInstance(Policy.WARN);
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

  // Given a sequence of lock acquisition descriptions
  // (e.g. "LockA -> LockB", "LockB -> LockC", ...)
  // Checks that the exception.getMessage() matches a regex of the form:
  // "LockA -> LockB \b.*\b LockB -> LockC \b.*\b LockC -> LockA"
  private void checkMessage(
      PotentialDeadlockException exception, String... expectedLockCycle) {
    String regex = Joiner.on("\\b.*\\b").join(expectedLockCycle);
    assertContainsRegex(regex, exception.getMessage());
  }
}
