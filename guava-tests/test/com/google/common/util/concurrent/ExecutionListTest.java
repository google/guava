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

package com.google.common.util.concurrent;

import static com.google.common.util.concurrent.MoreExecutors.sameThreadExecutor;

import com.google.common.testing.NullPointerTester;
import com.google.common.util.concurrent.ExecutionList;

import junit.framework.TestCase;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Unit tests for {@link ExecutionList}.
 *
 * @author Nishant Thakkar
 * @author Sven Mawson
 */
public class ExecutionListTest extends TestCase {

  protected ExecutionList list = new ExecutionList();
  protected Executor exec = Executors.newCachedThreadPool();

  public void testRunOnPopulatedList() throws Exception {
    CountDownLatch countDownLatch = new CountDownLatch(3);
    list.add(new MockRunnable(countDownLatch), exec);
    list.add(new MockRunnable(countDownLatch), exec);
    list.add(new MockRunnable(countDownLatch), exec);
    assertEquals(countDownLatch.getCount(), 3L);

    list.execute();

    // Verify that all of the runnables execute in a reasonable amount of time.
    assertTrue(countDownLatch.await(1L, TimeUnit.SECONDS));
  }

  public void testAddAfterRun() throws Exception {
    // Run the previous test
    testRunOnPopulatedList();

    // If it passed, then verify an Add will be executed without calling run
    CountDownLatch countDownLatch = new CountDownLatch(1);
    list.add(new MockRunnable(countDownLatch), exec);
    assertTrue(countDownLatch.await(1L, TimeUnit.SECONDS));
  }

  private class MockRunnable implements Runnable {
    CountDownLatch countDownLatch;

    MockRunnable(CountDownLatch countDownLatch) {
      this.countDownLatch = countDownLatch;
    }

    @Override
    public void run() {
      countDownLatch.countDown();
    }
  }

  public void testExceptionsCaught() {
    ExecutionList list = new ExecutionList();
    list.add(THROWING_RUNNABLE, sameThreadExecutor());
    list.execute();
    list.add(THROWING_RUNNABLE, sameThreadExecutor());
  }

  public void testNulls() throws Exception {
    NullPointerTester tester = new NullPointerTester();
    tester.setDefault(Executor.class, sameThreadExecutor());
    tester.setDefault(Runnable.class, DO_NOTHING);
    tester.testAllPublicInstanceMethods(new ExecutionList());
  }

  private static final Runnable THROWING_RUNNABLE = new Runnable() {
    @Override public void run() {
      throw new RuntimeException();
    }
  };
  private static final Runnable DO_NOTHING = new Runnable() {
    @Override public void run() {
    }
  };
}
