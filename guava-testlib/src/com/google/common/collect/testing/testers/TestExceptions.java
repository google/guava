/*
 * Copyright (C) 2007 The Guava Authors
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

package com.google.common.collect.testing.testers;

import com.google.common.annotations.GwtCompatible;

/** Exception classes for use in tests. */
@GwtCompatible
final class TestExceptions {
  static final class SomeError extends Error {}

  static final class SomeCheckedException extends Exception {}

  static final class SomeOtherCheckedException extends Exception {}

  static final class YetAnotherCheckedException extends Exception {}

  static final class SomeUncheckedException extends RuntimeException {}

  static final class SomeChainingException extends RuntimeException {
    public SomeChainingException(Throwable cause) {
      super(cause);
    }
  }

  private TestExceptions() {}
}
