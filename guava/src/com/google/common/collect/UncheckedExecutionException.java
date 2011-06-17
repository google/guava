/*
 * Copyright (C) 2011 The Guava Authors
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

package com.google.common.collect;

/**
 * Unchecked version of {@link java.util.concurrent.ExecutionException}.
 *
 * @author fry@google.com (Charles Fry)
 */
// TODO(user): move to common.util.concurrent
public class UncheckedExecutionException extends RuntimeException {
  /**
   * Creates a new instance with the given cause.
   */
  public UncheckedExecutionException(Throwable cause) {
    super(cause);
  }

  /**
   * Creates a new instance with the given cause.
   */
  public UncheckedExecutionException(String message, Throwable cause) {
    super(message, cause);
  }
}
