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

import com.google.caliper.AfterExperiment;
import com.google.caliper.BeforeExperiment;
import com.google.caliper.Benchmark;
import com.google.caliper.Param;
import com.google.caliper.api.Footprint;
import com.google.caliper.api.VmOptions;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A benchmark comparing the {@link MoreExecutors#sameThreadExecutor()} to a trivial
 * implementation.
 */
@VmOptions({"-Xms12g", "-Xmx12g", "-d64"})
public class MoreExecutorsSameThreadExecutorBenchmark {
  private static final Executor INLINE_EXECUTOR = new Executor() {
    @Override public void execute(Runnable command) {
      command.run();
    }
  };
  enum Impl {
    OLD {
      @Override Executor executor() {
        return MoreExecutors.sameThreadExecutor();
      }
    },
    NEW {
      @Override Executor executor() {
        return INLINE_EXECUTOR;
      }
    };
    abstract Executor executor();
  }

  @Param Impl impl;
  Executor executor;

  static final class CountingRunnable implements Runnable {
    AtomicInteger integer = new AtomicInteger();
    @Override public void run() {
      integer.incrementAndGet();
    }
  }
  
  CountingRunnable countingRunnable = new CountingRunnable();

  Set<Thread> threads = new HashSet<Thread>();
  
  @BeforeExperiment void before() {
    executor = impl.executor();
    for (int i = 0; i < 4; i++) {
      Thread thread = new Thread() {
        @Override public void run() {
          CountingRunnable localRunnable = new CountingRunnable();
          while (!isInterrupted()) {
            executor.execute(localRunnable);
          }
          countingRunnable.integer.addAndGet(localRunnable.integer.get());
        }
      };
      threads.add(thread);
    }
  }

  @AfterExperiment void after() throws InterruptedException {
    for (Thread thread : threads) {
      thread.interrupt();  // try to get them to exit
    }
    threads.clear();
  }
  
  @Footprint Object measureSize() {
    return executor;
  }
  
  @Benchmark int timeUncontendedExecute(int reps) {
    final Executor executor = this.executor;
    final CountingRunnable countingRunnable = this.countingRunnable;
    for (int i = 0; i < reps; i++) {
      executor.execute(countingRunnable);
    }
    return countingRunnable.integer.get();
  }
  
  @Benchmark int timeContendedExecute(int reps) {
    final Executor executor = this.executor;
    for (Thread thread : threads) {
      if (!thread.isAlive()) {
        thread.start();
      }
    }
    final CountingRunnable countingRunnable = this.countingRunnable;
    for (int i = 0; i < reps; i++) {
      executor.execute(countingRunnable);
    }
    return countingRunnable.integer.get();
  }
}
