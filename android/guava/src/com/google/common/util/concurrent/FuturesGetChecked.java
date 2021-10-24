/*
 * Copyright (C) 2006 The Guava Authors
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

import static com.google.common.base.Preconditions.checkArgument;
import static java.lang.Thread.currentThread;
import static java.util.Arrays.asList;

import com.google.common.annotations.GwtIncompatible;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
import com.google.common.collect.Ordering;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.lang.ref.WeakReference;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import javax.annotation.CheckForNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/** Static methods used to implement {@link Futures#getChecked(Future, Class)}. */
@GwtIncompatible
@ElementTypesAreNonnullByDefault
final class FuturesGetChecked {
  @CanIgnoreReturnValue
  @ParametricNullness
  static <V extends @Nullable Object, X extends Exception> V getChecked(
      Future<V> future, Class<X> exceptionClass) throws X {
    return getChecked(bestGetCheckedTypeValidator(), future, exceptionClass);
  }

  /** Implementation of {@link Futures#getChecked(Future, Class)}. */
  @CanIgnoreReturnValue
  @VisibleForTesting
  @ParametricNullness
  static <V extends @Nullable Object, X extends Exception> V getChecked(
      GetCheckedTypeValidator validator, Future<V> future, Class<X> exceptionClass) throws X {
    validator.validateClass(exceptionClass);
    try {
      return future.get();
    } catch (InterruptedException e) {
      currentThread().interrupt();
      throw newWithCause(exceptionClass, e);
    } catch (ExecutionException e) {
      wrapAndThrowExceptionOrError(e.getCause(), exceptionClass);
      throw new AssertionError();
    }
  }

  /** Implementation of {@link Futures#getChecked(Future, Class, long, TimeUnit)}. */
  @CanIgnoreReturnValue
  @ParametricNullness
  static <V extends @Nullable Object, X extends Exception> V getChecked(
      Future<V> future, Class<X> exceptionClass, long timeout, TimeUnit unit) throws X {
    // TODO(cpovirk): benchmark a version of this method that accepts a GetCheckedTypeValidator
    bestGetCheckedTypeValidator().validateClass(exceptionClass);
    try {
      return future.get(timeout, unit);
    } catch (InterruptedException e) {
      currentThread().interrupt();
      throw newWithCause(exceptionClass, e);
    } catch (TimeoutException e) {
      throw newWithCause(exceptionClass, e);
    } catch (ExecutionException e) {
      wrapAndThrowExceptionOrError(e.getCause(), exceptionClass);
      throw new AssertionError();
    }
  }

  @VisibleForTesting
  interface GetCheckedTypeValidator {
    void validateClass(Class<? extends Exception> exceptionClass);
  }

  private static GetCheckedTypeValidator bestGetCheckedTypeValidator() {
    return GetCheckedTypeValidatorHolder.BEST_VALIDATOR;
  }

  @VisibleForTesting
  static GetCheckedTypeValidator weakSetValidator() {
    return GetCheckedTypeValidatorHolder.WeakSetValidator.INSTANCE;
  }

  /**
   * Provides a check of whether an exception type is valid for use with {@link
   * FuturesGetChecked#getChecked(Future, Class)}, possibly using caching.
   *
   * <p>Uses reflection to gracefully fall back to when certain implementations aren't available.
   */
  @VisibleForTesting
  static class GetCheckedTypeValidatorHolder {
    static final GetCheckedTypeValidator BEST_VALIDATOR = getBestValidator();

    enum WeakSetValidator implements GetCheckedTypeValidator {
      INSTANCE;

      /*
       * Static final fields are presumed to be fastest, based on our experience with
       * UnsignedBytesBenchmark. TODO(cpovirk): benchmark this
       */
      /*
       * A CopyOnWriteArraySet<WeakReference> is faster than a newSetFromMap of a MapMaker map with
       * weakKeys() and concurrencyLevel(1), even up to at least 12 cached exception types.
       */
      private static final Set<WeakReference<Class<? extends Exception>>> validClasses =
          new CopyOnWriteArraySet<>();

      @Override
      public void validateClass(Class<? extends Exception> exceptionClass) {
        for (WeakReference<Class<? extends Exception>> knownGood : validClasses) {
          if (exceptionClass.equals(knownGood.get())) {
            return;
          }
          // TODO(cpovirk): if reference has been cleared, remove it?
        }
        checkExceptionClassValidity(exceptionClass);

        /*
         * It's very unlikely that any loaded Futures class will see getChecked called with more
         * than a handful of exceptions. But it seems prudent to set a cap on how many we'll cache.
         * This avoids out-of-control memory consumption, and it keeps the cache from growing so
         * large that doing the lookup is noticeably slower than redoing the work would be.
         *
         * Ideally we'd have a real eviction policy, but until we see a problem in practice, I hope
         * that this will suffice. I have not even benchmarked with different size limits.
         */
        if (validClasses.size() > 1000) {
          validClasses.clear();
        }

        validClasses.add(new WeakReference<Class<? extends Exception>>(exceptionClass));
      }
    }

    /**
     * Returns the ClassValue-using validator, or falls back to the "weak Set" implementation if
     * unable to do so.
     */
    static GetCheckedTypeValidator getBestValidator() {
      return weakSetValidator();
    }
  }

  // TODO(cpovirk): change parameter order to match other helper methods (Class, Throwable)?
  private static <X extends Exception> void wrapAndThrowExceptionOrError(
      Throwable cause, Class<X> exceptionClass) throws X {
    if (cause instanceof Error) {
      throw new ExecutionError((Error) cause);
    }
    if (cause instanceof RuntimeException) {
      throw new UncheckedExecutionException(cause);
    }
    throw newWithCause(exceptionClass, cause);
  }

  /*
   * TODO(user): FutureChecker interface for these to be static methods on? If so, refer to it in
   * the (static-method) Futures.getChecked documentation
   */

  private static boolean hasConstructorUsableByGetChecked(
      Class<? extends Exception> exceptionClass) {
    try {
      Exception unused = newWithCause(exceptionClass, new Exception());
      return true;
    } catch (Exception e) {
      return false;
    }
  }

  private static <X extends Exception> X newWithCause(Class<X> exceptionClass, Throwable cause) {
    // getConstructors() guarantees this as long as we don't modify the array.
    @SuppressWarnings({"unchecked", "rawtypes"})
    List<Constructor<X>> constructors = (List) Arrays.asList(exceptionClass.getConstructors());
    for (Constructor<X> constructor : preferringStrings(constructors)) {
      X instance = newFromConstructor(constructor, cause);
      if (instance != null) {
        if (instance.getCause() == null) {
          instance.initCause(cause);
        }
        return instance;
      }
    }
    throw new IllegalArgumentException(
        "No appropriate constructor for exception of type "
            + exceptionClass
            + " in response to chained exception",
        cause);
  }

  private static <X extends Exception> List<Constructor<X>> preferringStrings(
      List<Constructor<X>> constructors) {
    return WITH_STRING_PARAM_FIRST.sortedCopy(constructors);
  }

  private static final Ordering<Constructor<?>> WITH_STRING_PARAM_FIRST =
      Ordering.natural()
          .onResultOf(
              new Function<Constructor<?>, Boolean>() {
                @Override
                public Boolean apply(Constructor<?> input) {
                  return asList(input.getParameterTypes()).contains(String.class);
                }
              })
          .reverse();

  @CheckForNull
  private static <X> X newFromConstructor(Constructor<X> constructor, Throwable cause) {
    Class<?>[] paramTypes = constructor.getParameterTypes();
    Object[] params = new Object[paramTypes.length];
    for (int i = 0; i < paramTypes.length; i++) {
      Class<?> paramType = paramTypes[i];
      if (paramType.equals(String.class)) {
        params[i] = cause.toString();
      } else if (paramType.equals(Throwable.class)) {
        params[i] = cause;
      } else {
        return null;
      }
    }
    try {
      return constructor.newInstance(params);
    } catch (IllegalArgumentException
        | InstantiationException
        | IllegalAccessException
        | InvocationTargetException e) {
      return null;
    }
  }

  @VisibleForTesting
  static boolean isCheckedException(Class<? extends Exception> type) {
    return !RuntimeException.class.isAssignableFrom(type);
  }

  @VisibleForTesting
  static void checkExceptionClassValidity(Class<? extends Exception> exceptionClass) {
    checkArgument(
        isCheckedException(exceptionClass),
        "Futures.getChecked exception type (%s) must not be a RuntimeException",
        exceptionClass);
    checkArgument(
        hasConstructorUsableByGetChecked(exceptionClass),
        "Futures.getChecked exception type (%s) must be an accessible class with an accessible "
            + "constructor whose parameters (if any) must be of type String and/or Throwable",
        exceptionClass);
  }

  private FuturesGetChecked() {}
}
