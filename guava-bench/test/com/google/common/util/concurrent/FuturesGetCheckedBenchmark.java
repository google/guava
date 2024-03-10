/*
 * Copyright (C) 2015 The Guava Authors
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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.util.concurrent.Futures.immediateFailedFuture;
import static com.google.common.util.concurrent.Futures.immediateFuture;
import static com.google.common.util.concurrent.FuturesGetChecked.checkExceptionClassValidity;
import static com.google.common.util.concurrent.FuturesGetChecked.classValueValidator;
import static com.google.common.util.concurrent.FuturesGetChecked.getChecked;
import static com.google.common.util.concurrent.FuturesGetChecked.isCheckedException;
import static com.google.common.util.concurrent.FuturesGetChecked.weakSetValidator;

import com.google.caliper.BeforeExperiment;
import com.google.caliper.Benchmark;
import com.google.caliper.Param;
import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.FuturesGetChecked.GetCheckedTypeValidator;
import java.io.IOException;
import java.net.URISyntaxException;
import java.security.GeneralSecurityException;
import java.security.KeyException;
import java.util.List;
import java.util.TooManyListenersException;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;
import java.util.prefs.BackingStoreException;
import java.util.prefs.InvalidPreferencesFormatException;
import java.util.zip.DataFormatException;
import javax.security.auth.RefreshFailedException;

/** Microbenchmark for {@link Futures#getChecked}. */
public class FuturesGetCheckedBenchmark {
  private enum Validator {
    NON_CACHING_WITH_CONSTRUCTOR_CHECK(nonCachingWithConstructorCheckValidator()),
    NON_CACHING_WITHOUT_CONSTRUCTOR_CHECK(nonCachingWithoutConstructorCheckValidator()),
    WEAK_SET(weakSetValidator()),
    CLASS_VALUE(classValueValidator()),
    ;

    final GetCheckedTypeValidator validator;

    Validator(GetCheckedTypeValidator validator) {
      this.validator = validator;
    }
  }

  private enum Result {
    SUCCESS(immediateFuture(new Object())),
    FAILURE(immediateFailedFuture(new Exception()));

    final Future<Object> future;

    Result(Future<Object> result) {
      this.future = result;
    }
  }

  private enum ExceptionType {
    CHECKED(IOException.class),
    UNCHECKED(RuntimeException.class);

    final Class<? extends Exception> exceptionType;

    ExceptionType(Class<? extends Exception> exceptionType) {
      this.exceptionType = exceptionType;
    }
  }

  private static final ImmutableSet<Class<? extends Exception>> OTHER_EXCEPTION_TYPES =
      ImmutableSet.of(
          BackingStoreException.class,
          BrokenBarrierException.class,
          CloneNotSupportedException.class,
          DataFormatException.class,
          ExecutionException.class,
          GeneralSecurityException.class,
          InvalidPreferencesFormatException.class,
          KeyException.class,
          RefreshFailedException.class,
          TimeoutException.class,
          TooManyListenersException.class,
          URISyntaxException.class);

  @Param Validator validator;
  @Param Result result;
  @Param ExceptionType exceptionType;
  /**
   * The number of other exception types in the cache of known-good exceptions and the number of
   * other {@code ClassValue} entries for the exception type to be tested. This lets us evaluate
   * whether our solution scales to use with multiple exception types and to whether it is affected
   * by other {@code ClassValue} users. Some of the benchmarked implementations don't use one or
   * both of these mechanisms, so they will be unaffected.
   */
  @Param({"0", "1", "12"})
  int otherEntriesInDataStructure;

  final List<ClassValue<?>> retainedReferencesToOtherClassValues = newArrayList();

  @BeforeExperiment
  void addOtherEntries() throws Exception {
    GetCheckedTypeValidator validator = this.validator.validator;
    Class<? extends Exception> exceptionType = this.exceptionType.exceptionType;

    for (Class<? extends Exception> exceptionClass :
        OTHER_EXCEPTION_TYPES.asList().subList(0, otherEntriesInDataStructure)) {
      getChecked(validator, immediateFuture(""), exceptionClass);
    }

    for (int i = 0; i < otherEntriesInDataStructure; i++) {
      ClassValue<Boolean> classValue =
          new ClassValue<Boolean>() {
            @Override
            protected Boolean computeValue(Class<?> type) {
              return true;
            }
          };
      classValue.get(exceptionType);
      retainedReferencesToOtherClassValues.add(classValue);
    }
  }

  @Benchmark
  int benchmarkGetChecked(int reps) {
    int tmp = 0;
    GetCheckedTypeValidator validator = this.validator.validator;
    Future<Object> future = this.result.future;
    Class<? extends Exception> exceptionType = this.exceptionType.exceptionType;
    for (int i = 0; i < reps; ++i) {
      try {
        tmp += getChecked(validator, future, exceptionType).hashCode();
      } catch (Exception e) {
        tmp += e.hashCode();
      }
    }
    return tmp;
  }

  private static GetCheckedTypeValidator nonCachingWithoutConstructorCheckValidator() {
    return NonCachingWithoutConstructorCheckValidator.INSTANCE;
  }

  private enum NonCachingWithoutConstructorCheckValidator implements GetCheckedTypeValidator {
    INSTANCE;

    @Override
    public void validateClass(Class<? extends Exception> exceptionClass) {
      checkArgument(
          isCheckedException(exceptionClass),
          "Futures.getChecked exception type (%s) must not be a RuntimeException",
          exceptionClass);
    }
  }

  private static GetCheckedTypeValidator nonCachingWithConstructorCheckValidator() {
    return NonCachingWithConstructorCheckValidator.INSTANCE;
  }

  private enum NonCachingWithConstructorCheckValidator implements GetCheckedTypeValidator {
    INSTANCE;

    @Override
    public void validateClass(Class<? extends Exception> exceptionClass) {
      checkExceptionClassValidity(exceptionClass);
    }
  }
}
