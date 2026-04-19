/*
 * Copyright (C) 2008 The Guava Authors
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

import static com.google.common.base.Throwables.propagateIfInstanceOf;
import static com.google.common.util.concurrent.TestPlatform.clearInterrupt;
import static com.google.common.util.concurrent.Uninterruptibles.getUninterruptibly;
import static java.util.concurrent.Executors.newSingleThreadExecutor;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.annotations.J2ktIncompatible;
import com.google.common.testing.TestLogHandler;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Logger;
import junit.framework.TestCase;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
@GwtCompatible
abstract class AbstractFuturesTest extends TestCase {
  protected static final Logger aggregateFutureLogger =
      Logger.getLogger(AggregateFuture.class.getName());

  protected static final String DATA1 = "data";
  protected static final String DATA2 = "more data";
  protected static final String DATA3 = "most data";

  protected final TestLogHandler aggregateFutureLogHandler = new TestLogHandler();

  @Override
  public void setUp() throws Exception {
    super.setUp();
    aggregateFutureLogger.addHandler(aggregateFutureLogHandler);
  }

  @Override
  public void tearDown() throws Exception {
    clearInterrupt();
    aggregateFutureLogger.removeHandler(aggregateFutureLogHandler);
    super.tearDown();
  }

  protected static class MyException extends Exception {}

  protected static class Foo {}

  protected static class FooChild extends Foo {}

  protected static class Bar {}

  protected static class BarChild extends Bar {}

  protected static <V> AsyncFunction<V, V> asyncIdentity() {
    return Futures::immediateFuture;
  }

  protected static <I, O> AsyncFunction<I, O> tagged(
      String toString, AsyncFunction<I, O> function) {
    return new AsyncFunction<I, O>() {
      @Override
      public ListenableFuture<O> apply(I input) throws Exception {
        return function.apply(input);
      }

      @Override
      public String toString() {
        return toString;
      }
    };
  }

  protected static <V> AsyncCallable<V> tagged(String toString, AsyncCallable<V> callable) {
    return new AsyncCallable<V>() {
      @Override
      public ListenableFuture<V> call() throws Exception {
        return callable.call();
      }

      @Override
      public String toString() {
        return toString;
      }
    };
  }

  /**
   * Very rough equivalent of a timed get, produced by calling the no-arg get method in another
   * thread and waiting a short time for it.
   */
  @CanIgnoreReturnValue
  @J2ktIncompatible
  @GwtIncompatible // threads
  protected static <V> V pseudoTimedGetUninterruptibly(Future<V> input, long timeout, TimeUnit unit)
      throws ExecutionException, TimeoutException {
    ExecutorService executor = newSingleThreadExecutor();
    Future<V> waiter = executor.submit(() -> input.get());

    try {
      return getUninterruptibly(waiter, timeout, unit);
    } catch (ExecutionException e) {
      propagateIfInstanceOf(e.getCause(), ExecutionException.class);
      propagateIfInstanceOf(e.getCause(), CancellationException.class);
      throw new AssertionError("Unexpected exception", e);
    } finally {
      executor.shutdownNow();
      // TODO(cpovirk): assertTrue(awaitTerminationUninterruptibly(executor, 10, SECONDS));
    }
  }
}
