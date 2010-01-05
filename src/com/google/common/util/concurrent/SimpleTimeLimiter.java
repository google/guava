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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import com.google.common.base.Throwables;
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
 * @since 2009.09.15 <b>tentative</b>
 */
public class SimpleTimeLimiter implements TimeLimiter {

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
   *     java.util.concurrent.Executors#newCachedThreadPool()}.
   */
  public SimpleTimeLimiter(ExecutorService executor) {
    checkNotNull(executor);
    this.executor = executor;
  }

  /**
   * Constructs a TimeLimiter instance using a {@link
   * java.util.concurrent.Executors#newCachedThreadPool()} to execute proxied
   * method calls.
   *
   * <p><b>Warning:</b> using a bounded executor may be counterproductive! If
   * the thread pool fills up, any time callers spend waiting for a thread may
   * count toward their time limit, and in this case the call may even time out
   * before the target method is ever invoked.
   */
  public SimpleTimeLimiter() {
    this(Executors.newCachedThreadPool());
  }

  public <T> T newProxy(final T target, Class<T> interfaceType,
      final long timeoutDuration, final TimeUnit timeoutUnit) {
    checkNotNull(target);
    checkNotNull(interfaceType);
    checkNotNull(timeoutUnit);
    checkArgument(timeoutDuration > 0, "bad timeout: " + timeoutDuration);
    checkArgument(interfaceType.isInterface(),
        "interfaceType must be an interface type");

    final Set<Method> interruptibleMethods
        = findInterruptibleMethods(interfaceType);

    InvocationHandler handler = new InvocationHandler() {
      public Object invoke(Object obj, final Method method, final Object[] args)
          throws Throwable {
        Callable<Object> callable = new Callable<Object>() {
          public Object call() throws Exception {
            try {
              return method.invoke(target, args);
            } catch (InvocationTargetException e) {
              Throwables.throwCause(e, false);
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
  public <T> T callWithTimeout(Callable<T> callable, long timeoutDuration,
      TimeUnit timeoutUnit, boolean amInterruptible) throws Exception {
    checkNotNull(callable);
    checkNotNull(timeoutUnit);
    checkArgument(timeoutDuration > 0, "bad timeout: " + timeoutDuration);
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
        Future<T> uninterruptible = Futures.makeUninterruptible(future);
        return uninterruptible.get(timeoutDuration, timeoutUnit);
      }
    } catch (ExecutionException e) {
      throw Throwables.throwCause(e, true);
    } catch (TimeoutException e) {
      future.cancel(true);
      throw new UncheckedTimeoutException(e);
    }
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

  // TODO: replace with version in common.reflect if and when that gets open-sourced
  private static <T> T newProxy(
      Class<T> interfaceType, InvocationHandler handler) {
    Object object = Proxy.newProxyInstance(
        interfaceType.getClassLoader(), new Class<?>[] { interfaceType }, handler);
    return interfaceType.cast(object);
  }
}
