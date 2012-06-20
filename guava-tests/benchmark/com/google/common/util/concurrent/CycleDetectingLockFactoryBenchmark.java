// Copyright 2011 Google Inc. All Rights Reserved.
package com.google.common.util.concurrent;

import com.google.caliper.Param;
import com.google.caliper.Runner;
import com.google.caliper.SimpleBenchmark;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Benchmarks for {@link CycleDetectingLockFactory}.
 *
 * @author Darick Tong (darick@google.com)
 */
public class CycleDetectingLockFactoryBenchmark extends SimpleBenchmark {

  @Param({"2","3","4","5","10"}) int lockNestingDepth;

  CycleDetectingLockFactory factory;
  private Lock[] plainLocks;
  private Lock[] detectingLocks;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    this.factory = CycleDetectingLockFactory.newInstance(
        CycleDetectingLockFactory.Policies.WARN);
    this.plainLocks = new Lock[lockNestingDepth];
    for (int i = 0; i < lockNestingDepth; i++) {
      plainLocks[i] = new ReentrantLock();
    }
    this.detectingLocks = new Lock[lockNestingDepth];
    for (int i = 0; i < lockNestingDepth; i++) {
      detectingLocks[i] = factory.newReentrantLock("Lock" + i);
    }
  }

  public void timeUnorderedPlainLocks(int reps) {
    lockAndUnlock(new ReentrantLock(), reps);
  }

  public void timeUnorderedCycleDetectingLocks(int reps) {
    lockAndUnlock(factory.newReentrantLock("foo"), reps);
  }

  private void lockAndUnlock(Lock lock, int reps) {
    for (int i = 0; i < reps; i++) {
      lock.lock();
      lock.unlock();
    }
  }

  public void timeOrderedPlainLocks(int reps) {
    lockAndUnlockNested(plainLocks, reps);
  }

  public void timeOrderedCycleDetectingLocks(int reps) {
    lockAndUnlockNested(detectingLocks, reps);
  }

  private void lockAndUnlockNested(Lock[] locks, int reps) {
    for (int i = 0; i < reps; i++) {
      for (int j = 0; j < locks.length; j++) {
        locks[j].lock();
      }
      for (int j = locks.length - 1; j >= 0; j--) {
        locks[j].unlock();
      }
    }
  }

  public static void main(String[] args) {
    Runner.main(CycleDetectingLockFactoryBenchmark.class, args);
  }

}
