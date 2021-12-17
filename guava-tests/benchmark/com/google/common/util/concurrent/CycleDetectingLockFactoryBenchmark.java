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

import com.google.caliper.BeforeExperiment;
import com.google.caliper.Benchmark;
import com.google.caliper.Param;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Benchmarks for {@link CycleDetectingLockFactory}.
 *
 * @author Darick Tong
 */
public class CycleDetectingLockFactoryBenchmark {

  @Param({"2", "3", "4", "5", "10"})
  int lockNestingDepth;

  CycleDetectingLockFactory factory;
  private Lock[] plainLocks;
  private Lock[] detectingLocks;

  @BeforeExperiment
  void setUp() throws Exception {
    this.factory = CycleDetectingLockFactory.newInstance(CycleDetectingLockFactory.Policies.WARN);
    this.plainLocks = new Lock[lockNestingDepth];
    for (int i = 0; i < lockNestingDepth; i++) {
      plainLocks[i] = new ReentrantLock();
    }
    this.detectingLocks = new Lock[lockNestingDepth];
    for (int i = 0; i < lockNestingDepth; i++) {
      detectingLocks[i] = factory.newReentrantLock("Lock" + i);
    }
  }

  @Benchmark
  void unorderedPlainLocks(int reps) {
    lockAndUnlock(new ReentrantLock(), reps);
  }

  @Benchmark
  void unorderedCycleDetectingLocks(int reps) {
    lockAndUnlock(factory.newReentrantLock("foo"), reps);
  }

  private static void lockAndUnlock(Lock lock, int reps) {
    for (int i = 0; i < reps; i++) {
      lock.lock();
      lock.unlock();
    }
  }

  @Benchmark
  void orderedPlainLocks(int reps) {
    lockAndUnlockNested(plainLocks, reps);
  }

  @Benchmark
  void orderedCycleDetectingLocks(int reps) {
    lockAndUnlockNested(detectingLocks, reps);
  }

  private static void lockAndUnlockNested(Lock[] locks, int reps) {
    for (int i = 0; i < reps; i++) {
      for (int j = 0; j < locks.length; j++) {
        locks[j].lock();
      }
      for (int j = locks.length - 1; j >= 0; j--) {
        locks[j].unlock();
      }
    }
  }
}
