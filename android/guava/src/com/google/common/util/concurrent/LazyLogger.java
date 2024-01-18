/*
 * Copyright (C) 2023 The Guava Authors
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

import com.google.common.annotations.GwtCompatible;
import java.util.logging.Logger;
import org.checkerframework.checker.nullness.qual.Nullable;

/** A holder for a {@link Logger} that is initialized only when requested. */
@GwtCompatible
@ElementTypesAreNonnullByDefault
final class LazyLogger {
  private final String loggerName;
  private volatile @Nullable Logger logger;

  LazyLogger(Class<?> ownerOfLogger) {
    this.loggerName = ownerOfLogger.getName();
  }

  Logger get() {
    /*
     * We use double-checked locking. We could the try racy single-check idiom, but that would
     * depend on Logger not contain mutable state.
     *
     * We could use Suppliers.memoizingSupplier here, but I micro-optimized to this implementation
     * to avoid the extra class for the lambda (and maybe more for memoizingSupplier itself) and the
     * indirection.
     *
     * One thing to *avoid* is a change to make each Logger user use memoizingSupplier directly:
     * That may introduce an extra class for each lambda (currently a dozen).
     */
    Logger local = logger;
    if (local != null) {
      return local;
    }
    synchronized (this) {
      local = logger;
      if (local != null) {
        return local;
      }
      return logger = Logger.getLogger(loggerName);
    }
  }
}
