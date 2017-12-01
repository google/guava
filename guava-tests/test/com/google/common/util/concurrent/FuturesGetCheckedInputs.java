/*
 * Copyright (C) 2008 The Guava Authors
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

import static com.google.common.util.concurrent.Futures.immediateFailedFuture;

import com.google.common.annotations.GwtCompatible;
import java.util.concurrent.Future;

/**
 * Classes and futures used in {@link FuturesGetCheckedTest} and {@link FuturesGetUncheckedTest}.
 */
@GwtCompatible
final class FuturesGetCheckedInputs {
  static final Exception CHECKED_EXCEPTION = new Exception("mymessage");
  static final Future<String> FAILED_FUTURE_CHECKED_EXCEPTION =
      immediateFailedFuture(CHECKED_EXCEPTION);
  static final RuntimeException UNCHECKED_EXCEPTION = new RuntimeException("mymessage");
  static final Future<String> FAILED_FUTURE_UNCHECKED_EXCEPTION =
      immediateFailedFuture(UNCHECKED_EXCEPTION);
  static final RuntimeException RUNTIME_EXCEPTION = new RuntimeException();
  static final OtherThrowable OTHER_THROWABLE = new OtherThrowable();
  static final Future<String> FAILED_FUTURE_OTHER_THROWABLE =
      immediateFailedFuture(OTHER_THROWABLE);
  static final Error ERROR = new Error("mymessage");
  static final Future<String> FAILED_FUTURE_ERROR = immediateFailedFuture(ERROR);
  static final Future<String> RUNTIME_EXCEPTION_FUTURE =
      UncheckedThrowingFuture.throwingRuntimeException(RUNTIME_EXCEPTION);
  static final Future<String> ERROR_FUTURE = UncheckedThrowingFuture.throwingError(ERROR);

  public static final class TwoArgConstructorException extends Exception {
    public TwoArgConstructorException(String message, Throwable cause) {
      super(message, cause);
    }
  }

  public static final class TwoArgConstructorRuntimeException extends RuntimeException {
    public TwoArgConstructorRuntimeException(String message, Throwable cause) {
      super(message, cause);
    }
  }

  public static final class ExceptionWithPrivateConstructor extends Exception {
    private ExceptionWithPrivateConstructor(String message, Throwable cause) {
      super(message, cause);
    }
  }

  @SuppressWarnings("unused") // we're testing that they're not used
  public static final class ExceptionWithSomePrivateConstructors extends Exception {
    private ExceptionWithSomePrivateConstructors(String a) {}

    private ExceptionWithSomePrivateConstructors(String a, String b) {}

    public ExceptionWithSomePrivateConstructors(String a, String b, String c) {}

    private ExceptionWithSomePrivateConstructors(String a, String b, String c, String d) {}

    private ExceptionWithSomePrivateConstructors(
        String a, String b, String c, String d, String e) {}
  }

  public static final class ExceptionWithManyConstructors extends Exception {
    boolean usedExpectedConstructor;

    public ExceptionWithManyConstructors() {}

    public ExceptionWithManyConstructors(Integer i) {}

    public ExceptionWithManyConstructors(Throwable a) {}

    public ExceptionWithManyConstructors(Throwable a, Throwable b) {}

    public ExceptionWithManyConstructors(String s, Throwable b) {
      usedExpectedConstructor = true;
    }

    public ExceptionWithManyConstructors(Throwable a, Throwable b, Throwable c) {}

    public ExceptionWithManyConstructors(Throwable a, Throwable b, Throwable c, Throwable d) {}

    public ExceptionWithManyConstructors(
        Throwable a, Throwable b, Throwable c, Throwable d, Throwable e) {}

    public ExceptionWithManyConstructors(
        Throwable a, Throwable b, Throwable c, Throwable d, Throwable e, String s, Integer i) {}
  }

  public static final class ExceptionWithoutThrowableConstructor extends Exception {
    public ExceptionWithoutThrowableConstructor(String s) {
      super(s);
    }
  }

  public static final class ExceptionWithWrongTypesConstructor extends Exception {
    public ExceptionWithWrongTypesConstructor(Integer i, String s) {
      super(s);
    }
  }

  static final class ExceptionWithGoodAndBadConstructor extends Exception {
    public ExceptionWithGoodAndBadConstructor(String message, Throwable cause) {
      throw new RuntimeException("bad constructor");
    }

    public ExceptionWithGoodAndBadConstructor(Throwable cause) {
      super(cause);
    }
  }

  static final class ExceptionWithBadConstructor extends Exception {
    public ExceptionWithBadConstructor(String message, Throwable cause) {
      throw new RuntimeException("bad constructor");
    }
  }

  static final class OtherThrowable extends Throwable {}

  private FuturesGetCheckedInputs() {}
}
