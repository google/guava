/*
 * Copyright (C) 2015 The Guava Authors
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

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.Thread.currentThread;

import com.google.common.annotations.GwtCompatible;
import javax.annotation.CheckForNull;

/** Methods factored out so that they can be emulated differently in GWT. */
@GwtCompatible(emulated = true)
@ElementTypesAreNonnullByDefault
final class Platform {
  static boolean isInstanceOfThrowableClass(
      @CheckForNull Throwable t, Class<? extends Throwable> expectedClass) {
    return expectedClass.isInstance(t);
  }

  static void restoreInterruptIfIsInterruptedException(Throwable t) {
    checkNotNull(t); // to satisfy NullPointerTester
    if (t instanceof InterruptedException) {
      currentThread().interrupt();
    }
  }

  private Platform() {}
}
