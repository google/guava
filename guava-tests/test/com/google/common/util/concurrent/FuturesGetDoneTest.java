/*
 * Copyright (C) 2016 The Guava Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.common.util.concurrent;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.util.concurrent.Futures.getDone;
import static com.google.common.util.concurrent.Futures.immediateCancelledFuture;
import static com.google.common.util.concurrent.Futures.immediateFailedFuture;
import static com.google.common.util.concurrent.Futures.immediateFuture;
import static com.google.common.util.concurrent.ReflectionFreeAssertThrows.assertThrows;

import com.google.common.annotations.GwtCompatible;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import junit.framework.TestCase;
import org.jspecify.annotations.NullUnmarked;
import org.jspecify.annotations.Nullable;

/** Unit tests for {@link Futures#getDone}. */
@GwtCompatible
@NullUnmarked
public class FuturesGetDoneTest extends TestCase {
  public void testSuccessful() throws ExecutionException {
    assertThat(getDone(immediateFuture("a"))).isEqualTo("a");
  }

  public void testSuccessfulNull() throws ExecutionException {
    assertThat(getDone(Futures.<@Nullable String>immediateFuture(null))).isEqualTo(null);
  }

  public void testFailed() {
    Exception failureCause = new Exception();
    ExecutionException expected =
        assertThrows(ExecutionException.class, () -> getDone(immediateFailedFuture(failureCause)));
    assertThat(expected).hasCauseThat().isEqualTo(failureCause);
  }

  public void testCancelled() throws ExecutionException {
    assertThrows(CancellationException.class, () -> getDone(immediateCancelledFuture()));
  }

  public void testPending() throws ExecutionException {
    assertThrows(IllegalStateException.class, () -> getDone(SettableFuture.create()));
  }
}
