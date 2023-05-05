/*
 * Copyright (C) 2018 The Guava Authors
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

package com.google.common.util.concurrent.internal;


/**
 * Static utilities for {@link InternalFutureFailureAccess}. Most users will never need to use this
 * class.
 *
 * <p>This class is GWT-compatible.
 *
 * @since {@code com.google.guava:failureaccess:1.0}, which was added as a dependency of Guava in
 *     Guava 27.0
 */
public final class InternalFutures {
  /**
   * Usually returns {@code null} but, if the given {@code Future} has failed, may <i>optionally</i>
   * return the cause of the failure. "Failure" means specifically "completed with an exception"; it
   * does not include "was cancelled." To be explicit: If this method returns a non-null value,
   * then:
   *
   * <ul>
   *   <li>{@code isDone()} must return {@code true}
   *   <li>{@code isCancelled()} must return {@code false}
   *   <li>{@code get()} must not block, and it must throw an {@code ExecutionException} with the
   *       return value of this method as its cause
   * </ul>
   */
  public static
      Throwable tryInternalFastPathGetFailure(InternalFutureFailureAccess future) {
    return future.tryInternalFastPathGetFailure();
  }

  private InternalFutures() {}
}
