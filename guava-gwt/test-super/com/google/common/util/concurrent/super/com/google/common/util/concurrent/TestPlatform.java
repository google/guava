/*
 * Copyright (C) 2015 The Guava Authors
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
import static junit.framework.Assert.fail;

import com.google.common.annotations.GwtCompatible;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Methods factored out so that they can be emulated differently in GWT.
 */
@GwtCompatible(emulated = true)
final class TestPlatform {
  static void verifyGetOnPendingFuture(Future<?> future) {
    try {
      future.get();
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(IllegalStateException.class);
      assertThat(e).hasMessage("Cannot get() on a pending future.");
    }
  }

  static void verifyTimedGetOnPendingFuture(Future<?> future) {
    try {
      future.get(0, TimeUnit.SECONDS);
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(IllegalStateException.class);
      assertThat(e).hasMessage("Cannot get() on a pending future.");
    }
  }

  static void verifyThreadWasNotInterrupted() {
    // There is no thread interruption in GWT, so there's nothing to do.
  }

  static void clearInterrupt() {
    // There is no thread interruption in GWT, so there's nothing to do.
  }

  private TestPlatform() {}
}
