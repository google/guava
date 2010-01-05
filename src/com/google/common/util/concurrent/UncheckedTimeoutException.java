/*
 * Copyright (C) 2006 Google Inc.
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

/**
 * Unchecked version of {@link java.util.concurrent.TimeoutException}.
 *
 * @author Kevin Bourrillion
 * @since 2009.09.15 <b>tentative</b>
 */
public class UncheckedTimeoutException extends RuntimeException {
  public UncheckedTimeoutException() {}

  public UncheckedTimeoutException(String message) {
    super(message);
  }

  public UncheckedTimeoutException(Throwable cause) {
    super(cause);
  }

  public UncheckedTimeoutException(String message, Throwable cause) {
    super(message, cause);
  }

  private static final long serialVersionUID = 0;
}
