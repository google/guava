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

import static com.google.common.util.concurrent.Callables.returning;
import static com.google.common.util.concurrent.TestPlatform.verifyThreadWasNotInterrupted;

import com.google.common.annotations.GwtCompatible;

import junit.framework.TestCase;

import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;

/**
 * Test case for {@link TrustedListenableFutureTask}.
 */
@GwtCompatible(emulated = true)
public class TrustedListenableFutureTaskTest extends TestCase {

  public void testSuccessful() throws Exception {
    TrustedListenableFutureTask<Integer> task = TrustedListenableFutureTask.create(returning(2));
    assertFalse(task.isDone());
    task.run();
    assertTrue(task.isDone());
    assertFalse(task.isCancelled());
    assertEquals(2, task.get().intValue());
  }

  public void testCancelled() throws Exception {
    TrustedListenableFutureTask<Integer> task = TrustedListenableFutureTask.create(returning(2));
    assertFalse(task.isDone());
    task.cancel(false);
    assertTrue(task.isDone());
    assertTrue(task.isCancelled());
    assertFalse(task.wasInterrupted());
    try {
      task.get();
      fail();
    } catch (CancellationException expected) {
    }
    verifyThreadWasNotInterrupted();
  }

  public void testFailed() throws Exception {
    final Exception e = new Exception();
    TrustedListenableFutureTask<Integer> task = TrustedListenableFutureTask.create(
        new Callable<Integer>() {
          @Override public Integer call() throws Exception {
            throw e;
          }
        });
    task.run();
    assertTrue(task.isDone());
    assertFalse(task.isCancelled());
    try {
      task.get();
      fail();
    } catch (ExecutionException executionException) {
      assertEquals(e, executionException.getCause());
    }
  }
}

