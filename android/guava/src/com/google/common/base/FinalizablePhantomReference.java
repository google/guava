/*
 * Copyright (C) 2007 The Guava Authors
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

package com.google.common.base;

import com.google.common.annotations.GwtIncompatible;
import com.google.common.annotations.J2ktIncompatible;
import java.lang.ref.PhantomReference;
import java.lang.ref.ReferenceQueue;
import org.jspecify.annotations.Nullable;

/**
 * Phantom reference with a {@code finalizeReferent()} method which a background thread invokes
 * after the garbage collector reclaims the referent. This is a simpler alternative to using a
 * {@link ReferenceQueue}.
 *
 * <p>Unlike a normal phantom reference, this reference will be cleared automatically.
 *
 * @author Bob Lee
 * @since 2.0
 */
@J2ktIncompatible
@GwtIncompatible
public abstract class FinalizablePhantomReference<T> extends PhantomReference<T>
    implements FinalizableReference {
  /**
   * Constructs a new finalizable phantom reference.
   *
   * @param referent to phantom reference
   * @param queue that should finalize the referent
   */
  protected FinalizablePhantomReference(@Nullable T referent, FinalizableReferenceQueue queue) {
    super(referent, queue.queue);
    queue.cleanUp();
  }
}
