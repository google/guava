/*
 * Copyright (C) 2024 The Guava Authors
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

package com.google.common.net;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.annotations.J2ktIncompatible;
import com.google.common.base.Predicate;
import com.google.common.base.VerifyException;
import com.google.common.collect.ImmutableMap;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.UnsupportedCharsetException;
import java.util.ConcurrentModificationException;
import java.util.NoSuchElementException;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import junit.framework.AssertionFailedError;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/** Replacements for JUnit's {@code assertThrows} that work under GWT/J2CL. */
@GwtCompatible(emulated = true)
@NullMarked
final class ReflectionFreeAssertThrows {
  interface ThrowingRunnable {
    void run() throws Throwable;
  }

  interface ThrowingSupplier {
    @Nullable Object get() throws Throwable;
  }

  @CanIgnoreReturnValue
  static <T extends Throwable> T assertThrows(
      Class<T> expectedThrowable, ThrowingSupplier supplier) {
    return doAssertThrows(expectedThrowable, supplier, /* userPassedSupplier= */ true);
  }

  @CanIgnoreReturnValue
  static <T extends Throwable> T assertThrows(
      Class<T> expectedThrowable, ThrowingRunnable runnable) {
    return doAssertThrows(
        expectedThrowable,
        () -> {
          runnable.run();
          return null;
        },
        /* userPassedSupplier= */ false);
  }

  private static <T extends Throwable> T doAssertThrows(
      Class<T> expectedThrowable, ThrowingSupplier supplier, boolean userPassedSupplier) {
    checkNotNull(expectedThrowable);
    checkNotNull(supplier);
    Predicate<Throwable> predicate = INSTANCE_OF.get(expectedThrowable);
    if (predicate == null) {
      throw new IllegalArgumentException(
          expectedThrowable
              + " is not yet supported by ReflectionFreeAssertThrows. Add an entry for it in the"
              + " map in that class.");
    }
    Object result;
    try {
      result = supplier.get();
    } catch (Throwable t) {
      if (predicate.apply(t)) {
        // We are careful to set up INSTANCE_OF to match each Predicate to its target Class.
        @SuppressWarnings("unchecked")
        T caught = (T) t;
        return caught;
      }
      throw new AssertionError(
          "expected to throw " + expectedThrowable.getSimpleName() + " but threw " + t, t);
    }
    if (userPassedSupplier) {
      throw new AssertionError(
          "expected to throw "
              + expectedThrowable.getSimpleName()
              + " but returned result: "
              + result);
    } else {
      throw new AssertionError("expected to throw " + expectedThrowable.getSimpleName());
    }
  }

  private enum PlatformSpecificExceptionBatch {
    PLATFORM {
      @GwtIncompatible
      @J2ktIncompatible
      @Override
      // returns the types available in "normal" environments
      ImmutableMap<Class<? extends Throwable>, Predicate<Throwable>> exceptions() {
        return ImmutableMap.of(
            InvocationTargetException.class,
            e -> e instanceof InvocationTargetException,
            StackOverflowError.class,
            e -> e instanceof StackOverflowError);
      }
    };

    // used under GWT, etc., since the override of this method does not exist there
    ImmutableMap<Class<? extends Throwable>, Predicate<Throwable>> exceptions() {
      return ImmutableMap.of();
    }
  }

  private static final ImmutableMap<Class<? extends Throwable>, Predicate<Throwable>> INSTANCE_OF =
      ImmutableMap.<Class<? extends Throwable>, Predicate<Throwable>>builder()
          .put(ArithmeticException.class, e -> e instanceof ArithmeticException)
          .put(
              ArrayIndexOutOfBoundsException.class,
              e -> e instanceof ArrayIndexOutOfBoundsException)
          .put(ArrayStoreException.class, e -> e instanceof ArrayStoreException)
          .put(AssertionFailedError.class, e -> e instanceof AssertionFailedError)
          .put(CancellationException.class, e -> e instanceof CancellationException)
          .put(ClassCastException.class, e -> e instanceof ClassCastException)
          .put(
              ConcurrentModificationException.class,
              e -> e instanceof ConcurrentModificationException)
          .put(ExecutionException.class, e -> e instanceof ExecutionException)
          .put(IllegalArgumentException.class, e -> e instanceof IllegalArgumentException)
          .put(IllegalStateException.class, e -> e instanceof IllegalStateException)
          .put(IndexOutOfBoundsException.class, e -> e instanceof IndexOutOfBoundsException)
          .put(NoSuchElementException.class, e -> e instanceof NoSuchElementException)
          .put(NullPointerException.class, e -> e instanceof NullPointerException)
          .put(NumberFormatException.class, e -> e instanceof NumberFormatException)
          .put(RuntimeException.class, e -> e instanceof RuntimeException)
          .put(TimeoutException.class, e -> e instanceof TimeoutException)
          .put(UnsupportedCharsetException.class, e -> e instanceof UnsupportedCharsetException)
          .put(UnsupportedOperationException.class, e -> e instanceof UnsupportedOperationException)
          .put(VerifyException.class, e -> e instanceof VerifyException)
          .putAll(PlatformSpecificExceptionBatch.PLATFORM.exceptions())
          .buildOrThrow();

  private ReflectionFreeAssertThrows() {}
}
