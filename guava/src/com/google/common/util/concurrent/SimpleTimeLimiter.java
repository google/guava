/*
 * Copyright (C) 2006 The Guava Authors
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
import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.annotations.Beta;
import com.google.common.collect.ObjectArrays;
import com.google.common.collect.Sets;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * A TimeLimiter that runs method calls in the background using an
 * {@link ExecutorService}.  If the time limit expires for a given method call,
 * the thread running the call will be interrupted.
 *
 * @author Kevin Bourrillion
 * @since 1.0
 */
@Beta
public final class SimpleTimeLimiter implements TimeLimiter {

  private final ExecutorService executor;

  /**
   * Constructs a TimeLimiter instance using the given executor service to
   * execute proxied method calls.
   * <p>
   * <b>Warning:</b> using a bounded executor
   * may be counterproductive!  If the thread pool fills up, any time callers
   * spend waiting for a thread may count toward their time limit, and in
   * this case the call may even time out before the target method is ever
   * invoked.
   *
   * @param executor the ExecutorService that will execute the method calls on
   *     the target objects; for example, a {@link
   *     Executors#newCachedThreadPool()}.
   */
  public SimpleTimeLimiter(ExecutorService executor) {
    this.executor = checkNotNull(executor);
  }

  /**
   * Constructs a TimeLimiter instance using a {@link
   * Executors#newCachedThreadPool()} to execute proxied method calls.
   *
   * <p><b>Warning:</b> using a bounded executor may be counterproductive! If
   * the thread pool fills up, any time callers spend waiting for a thread may
   * count toward their time limit, and in this case the call may even time out
   * before the target method is ever invoked.
   */
  public SimpleTimeLimiter() {
    this(Executors.newCachedThreadPool());
  }

  @Override
  public <T> T newProxy(final T target, Class<T> interfaceType,
      final long timeoutDuration, final TimeUnit timeoutUnit) {
    checkNotNull(target);
    checkNotNull(interfaceType);
    checkNotNull(timeoutUnit);
    checkArgument(timeoutDuration > 0, "bad timeout: %s", timeoutDuration);
    checkArgument(interfaceType.isInterface(),
        "interfaceType must be an interface type");

    final Set<Method> interruptibleMethods
        = findInterruptibleMethods(interfaceType);

    InvocationHandler handler = new InvocationHandler() {
      @Override
      public Object invoke(Object obj, final Method method, final Object[] args)
          throws Throwable {
        Callable<Object> callable = new Callable<Object>() {
          @Override
          public Object call() throws Exception {
            try {
              return method.invoke(target, args);
            } catch (InvocationTargetException e) {
              throwCause(e, false);
              throw new AssertionError("can't get here");
            }
          }
        };
        return callWithTimeout(callable, timeoutDuration, timeoutUnit,
            interruptibleMethods.contains(method));
      }
    };
    return newProxy(interfaceType, handler);
  }

  // TODO: should this actually throw only ExecutionException?
  @Override
  public <T> T callWithTimeout(Callable<T> callable, long timeoutDuration,
      TimeUnit timeoutUnit, boolean amInterruptible) throws Exception {
    checkNotNull(callable);
    checkNotNull(timeoutUnit);
    checkArgument(timeoutDuration > 0, "timeout must be positive: %s",
        timeoutDuration);
    Future<T> future = executor.submit(callable);
    try {
      if (amInterruptible) {
        try {
          return future.get(timeoutDuration, timeoutUnit);
        } catch (InterruptedException e) {
          future.cancel(true);
          throw e;
        }
      } else {
        return Uninterruptibles.getUninterruptibly(future, 
            timeoutDuration, timeoutUnit);
      }
    } catch (ExecutionException e) {
      throw throwCause(e, true);
    } catch (TimeoutException e) {
      future.cancel(true);
      throw new UncheckedTimeoutException(e);
    }
  }

  private static Exception throwCause(Exception e, boolean combineStackTraces)
      throws Exception {
    Throwable cause = e.getCause();
    if (cause == null) {
      throw e;
    }
    if (combineStackTraces) {
      StackTraceElement[] combined = ObjectArrays.concat(cause.getStackTrace(),
          e.getStackTrace(), StackTraceElement.class);
      cause.setStackTrace(combined);
    }
    if (cause instanceof Exception) {
      throw (Exception) cause;
    }
    if (cause instanceof Error) {
      throw (Error) cause;
    }
    // The cause is a weird kind of Throwable, so throw the outer exception.
    throw e;
  }

  private static Set<Method> findInterruptibleMethods(Class<?> interfaceType) {
    Set<Method> set = Sets.newHashSet();
    for (Method m : interfaceType.getMethods()) {
      if (declaresInterruptedEx(m)) {
        set.add(m);
      }
    }
    return set;
  }

  private static boolean declaresInterruptedEx(Method method) {
    for (Class<?> exType : method.getExceptionTypes()) {
      // debate: == or isAssignableFrom?
      if (exType == InterruptedException.class) {
        return true;
      }
    }
    return false;
  }

  // TODO: replace with version in common.reflect if and when it's open-sourced
  private static <T> T newProxy(
      Class<T> interfaceType, InvocationHandler handler) {
    Object object = Proxy.newProxyInstance(interfaceType.getClassLoader(),
        new Class<?>[] { interfaceType }, handler);
    return interfaceType.cast(object);
  }
}
