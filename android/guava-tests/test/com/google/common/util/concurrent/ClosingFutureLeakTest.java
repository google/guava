/*
 * Copyright (C) 2026 The Guava Authors
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

import static com.google.common.testing.GcFinalization.awaitClear;
import static com.google.common.truth.Truth.assertThat;
import static com.google.common.util.concurrent.MoreExecutors.directExecutor;

import com.google.common.annotations.GwtIncompatible;
import com.google.common.annotations.J2ktIncompatible;
import com.google.common.util.concurrent.AbstractClosingFutureTest.TestCloseable;
import java.lang.ref.WeakReference;
import junit.framework.TestCase;
import org.jspecify.annotations.NullUnmarked;

/** Tests for {@link ClosingFuture} leaks. */
@NullUnmarked
@GwtIncompatible
@J2ktIncompatible
public class ClosingFutureLeakTest extends TestCase {
  public void testLeak() {
    TestCloseable closeable = new TestCloseable("closeable");
    awaitClear(leakClosingFuture(closeable));
    assertThat(closeable.awaitClosed()).isTrue();
  }

  private WeakReference<ClosingFuture<String>> leakClosingFuture(TestCloseable closeable) {
    ClosingFuture<String> willCloseCloseable =
        ClosingFuture.submit(
            closer -> {
              closer.eventuallyClose(closeable, directExecutor());
              return "";
            },
            directExecutor());
    return new WeakReference<>(willCloseCloseable);
  }
}
