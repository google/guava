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


import com.google.common.annotations.GwtIncompatible;
import com.google.common.annotations.J2ktIncompatible;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.errorprone.annotations.DoNotMock;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Imposes a time limit on method calls.
 *
 * @author Kevin Bourrillion
 * @author Jens Nyman
 * @since 1.0
 */
@DoNotMock("Use FakeTimeLimiter")
@J2ktIncompatible
@GwtIncompatible
@SuppressWarnings("GoodTime") // should have java.time.Duration overloads
@ElementTypesAreNonnullByDefault
public interface TimeLimiter {

  /**
   * Returns an instance of {@code interfaceType} that delegates all method calls to the {@code
   * target} object, enforcing the specified time limit on each call. This time-limited delegation
   * is also performed for calls to {@link Object#equals}, {@link Object#hashCode}, and {@link
   * Object#toString}.
   *
   * <p>If the target method call finishes before the limit is reached, the return value or
   * exception is propagated to the caller exactly as-is. If, on the other hand, the time limit is
   * reached, the proxy will attempt to abort the call to the target, and will throw an {@link
   * UncheckedTimeoutException} to the caller.
   *
   * <p>It is important to note that the primary purpose of the proxy object is to return control to
   * the caller when the timeout elapses; aborting the target method call is of secondary concern.
   * The particular nature and strength of the guarantees made by the proxy is
   * implementation-dependent. However, it is important that each of the methods on the target
   * object behaves appropriately when its thread is interrupted.
   *
   * <p>For example, to return the value of {@code target.someMethod()}, but substitute {@code
   * DEFAULT_VALUE} if this method call takes over 50 ms, you can use this code:
   *
   * <pre>
   *   TimeLimiter limiter = . . .;
   *   TargetType proxy = limiter.newProxy(
   *       target, TargetType.class, 50, TimeUnit.MILLISECONDS);
   *   try {
   *     return proxy.someMethod();
   *   } catch (UncheckedTimeoutException e) {
   *     return DEFAULT_VALUE;
   *   }
   * </pre>
   *
   * @param target the object to proxy
   * @param interfaceType the interface you wish the returned proxy to implement
   * @param timeoutDuration with timeoutUnit, the maximum length of time that callers are willing to
   *     wait on each method call to the proxy
   * @param timeoutUnit with timeoutDuration, the maximum length of time that callers are willing to
   *     wait on each method call to the proxy
   * @return a time-limiting proxy
   * @throws IllegalArgumentException if {@code interfaceType} is a regular class, enum, or
   *     annotation type, rather than an interface
   */
  <T> T newProxy(T target, Class<T> interfaceType, long timeoutDuration, TimeUnit timeoutUnit);

  /**
   * Invokes a specified Callable, timing out after the specified time limit. If the target method
   * call finishes before the limit is reached, the return value or a wrapped exception is
   * propagated. If, on the other hand, the time limit is reached, we attempt to abort the call to
   * the target, and throw a {@link TimeoutException} to the caller.
   *
   * @param callable the Callable to execute
   * @param timeoutDuration with timeoutUnit, the maximum length of time to wait
   * @param timeoutUnit with timeoutDuration, the maximum length of time to wait
   * @return the result returned by the Callable
   * @throws TimeoutException if the time limit is reached
   * @throws InterruptedException if the current thread was interrupted during execution
   * @throws ExecutionException if {@code callable} throws a checked exception
   * @throws UncheckedExecutionException if {@code callable} throws a {@code RuntimeException}
   * @throws ExecutionError if {@code callable} throws an {@code Error}
   * @since 22.0
   */
  @CanIgnoreReturnValue
  @ParametricNullness
  <T extends @Nullable Object> T callWithTimeout(
      Callable<T> callable, long timeoutDuration, TimeUnit timeoutUnit)
      throws TimeoutException, InterruptedException, ExecutionException;

  /**
   * Invokes a specified Callable, timing out after the specified time limit. If the target method
   * call finishes before the limit is reached, the return value or a wrapped exception is
   * propagated. If, on the other hand, the time limit is reached, we attempt to abort the call to
   * the target, and throw a {@link TimeoutException} to the caller.
   *
   * <p>The difference with {@link #callWithTimeout(Callable, long, TimeUnit)} is that this method
   * will ignore interrupts on the current thread.
   *
   * @param callable the Callable to execute
   * @param timeoutDuration with timeoutUnit, the maximum length of time to wait
   * @param timeoutUnit with timeoutDuration, the maximum length of time to wait
   * @return the result returned by the Callable
   * @throws TimeoutException if the time limit is reached
   * @throws ExecutionException if {@code callable} throws a checked exception
   * @throws UncheckedExecutionException if {@code callable} throws a {@code RuntimeException}
   * @throws ExecutionError if {@code callable} throws an {@code Error}
   * @since 22.0
   */
  @CanIgnoreReturnValue
  @ParametricNullness
  <T extends @Nullable Object> T callUninterruptiblyWithTimeout(
      Callable<T> callable, long timeoutDuration, TimeUnit timeoutUnit)
      throws TimeoutException, ExecutionException;

  /**
   * Invokes a specified Runnable, timing out after the specified time limit. If the target method
   * run finishes before the limit is reached, this method returns or a wrapped exception is
   * propagated. If, on the other hand, the time limit is reached, we attempt to abort the run, and
   * throw a {@link TimeoutException} to the caller.
   *
   * @param runnable the Runnable to execute
   * @param timeoutDuration with timeoutUnit, the maximum length of time to wait
   * @param timeoutUnit with timeoutDuration, the maximum length of time to wait
   * @throws TimeoutException if the time limit is reached
   * @throws InterruptedException if the current thread was interrupted during execution
   * @throws UncheckedExecutionException if {@code runnable} throws a {@code RuntimeException}
   * @throws ExecutionError if {@code runnable} throws an {@code Error}
   * @since 22.0
   */
  void runWithTimeout(Runnable runnable, long timeoutDuration, TimeUnit timeoutUnit)
      throws TimeoutException, InterruptedException;

  /**
   * Invokes a specified Runnable, timing out after the specified time limit. If the target method
   * run finishes before the limit is reached, this method returns or a wrapped exception is
   * propagated. If, on the other hand, the time limit is reached, we attempt to abort the run, and
   * throw a {@link TimeoutException} to the caller.
   *
   * <p>The difference with {@link #runWithTimeout(Runnable, long, TimeUnit)} is that this method
   * will ignore interrupts on the current thread.
   *
   * @param runnable the Runnable to execute
   * @param timeoutDuration with timeoutUnit, the maximum length of time to wait
   * @param timeoutUnit with timeoutDuration, the maximum length of time to wait
   * @throws TimeoutException if the time limit is reached
   * @throws UncheckedExecutionException if {@code runnable} throws a {@code RuntimeException}
   * @throws ExecutionError if {@code runnable} throws an {@code Error}
   * @since 22.0
   */
  void runUninterruptiblyWithTimeout(Runnable runnable, long timeoutDuration, TimeUnit timeoutUnit)
      throws TimeoutException;
}
