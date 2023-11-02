/*
 * Copyright (C) 2010 The Guava Authors
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

import static com.google.common.base.Preconditions.checkNotNull;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertSame;

import com.google.common.testing.TearDown;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import junit.framework.AssertionFailedError;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * A helper for concurrency testing. One or more {@code TestThread} instances are instantiated in a
 * test with reference to the same "lock-like object", and then their interactions with that object
 * are choreographed via the various methods on this class.
 *
 * <p>A "lock-like object" is really any object that may be used for concurrency control. If the
 * {@link #callAndAssertBlocks} method is ever called in a test, the lock-like object must have a
 * method equivalent to {@link java.util.concurrent.locks.ReentrantLock#hasQueuedThread(Thread)}. If
 * the {@link #callAndAssertWaits} method is ever called in a test, the lock-like object must have a
 * method equivalent to {@link
 * java.util.concurrent.locks.ReentrantLock#hasWaiters(java.util.concurrent.locks.Condition)},
 * except that the method parameter must accept whatever condition-like object is passed into {@code
 * callAndAssertWaits} by the test.
 *
 * @param <L> the type of the lock-like object to be used
 * @author Justin T. Sampson
 */
public final class TestThread<L> extends Thread implements TearDown {

  private static final long DUE_DILIGENCE_MILLIS = 100;
  private static final long TIMEOUT_MILLIS = 5000;

  private final L lockLikeObject;

  private final SynchronousQueue<Request> requestQueue = new SynchronousQueue<>();
  private final SynchronousQueue<Response> responseQueue = new SynchronousQueue<>();

  private @Nullable Throwable uncaughtThrowable = null;

  public TestThread(L lockLikeObject, String threadName) {
    super(threadName);
    this.lockLikeObject = checkNotNull(lockLikeObject);
    start();
  }

  // Thread.stop() is okay because all threads started by a test are dying at the end of the test,
  // so there is no object state put at risk by stopping the threads abruptly. In some cases a test
  // may put a thread into an uninterruptible operation intentionally, so there is no other way to
  // clean up these threads.
  @SuppressWarnings("deprecation")
  @Override
  public void tearDown() throws Exception {
    stop();
    join();

    if (uncaughtThrowable != null) {
      throw (AssertionFailedError)
          new AssertionFailedError("Uncaught throwable in " + getName())
              .initCause(uncaughtThrowable);
    }
  }

  /**
   * Causes this thread to call the named void method, and asserts that the call returns normally.
   */
  public void callAndAssertReturns(String methodName, Object... arguments) throws Exception {
    checkNotNull(methodName);
    checkNotNull(arguments);
    sendRequest(methodName, arguments);
    assertSame(null, getResponse(methodName).getResult());
  }

  /**
   * Causes this thread to call the named method, and asserts that the call returns the expected
   * boolean value.
   */
  public void callAndAssertReturns(boolean expected, String methodName, Object... arguments)
      throws Exception {
    checkNotNull(methodName);
    checkNotNull(arguments);
    sendRequest(methodName, arguments);
    assertEquals(expected, getResponse(methodName).getResult());
  }

  /**
   * Causes this thread to call the named method, and asserts that the call returns the expected int
   * value.
   */
  public void callAndAssertReturns(int expected, String methodName, Object... arguments)
      throws Exception {
    checkNotNull(methodName);
    checkNotNull(arguments);
    sendRequest(methodName, arguments);
    assertEquals(expected, getResponse(methodName).getResult());
  }

  /**
   * Causes this thread to call the named method, and asserts that the call throws the expected type
   * of throwable.
   */
  public void callAndAssertThrows(
      Class<? extends Throwable> expected, String methodName, Object... arguments)
      throws Exception {
    checkNotNull(expected);
    checkNotNull(methodName);
    checkNotNull(arguments);
    sendRequest(methodName, arguments);
    assertEquals(expected, getResponse(methodName).getThrowable().getClass());
  }

  /**
   * Causes this thread to call the named method, and asserts that this thread becomes blocked on
   * the lock-like object. The lock-like object must have a method equivalent to {@link
   * java.util.concurrent.locks.ReentrantLock#hasQueuedThread(Thread)}.
   */
  public void callAndAssertBlocks(String methodName, Object... arguments) throws Exception {
    checkNotNull(methodName);
    checkNotNull(arguments);
    assertEquals(false, invokeMethod("hasQueuedThread", this));
    sendRequest(methodName, arguments);
    Thread.sleep(DUE_DILIGENCE_MILLIS);
    assertEquals(true, invokeMethod("hasQueuedThread", this));
    assertNull(responseQueue.poll());
  }

  /**
   * Causes this thread to call the named method, and asserts that this thread thereby waits on the
   * given condition-like object. The lock-like object must have a method equivalent to {@link
   * java.util.concurrent.locks.ReentrantLock#hasWaiters(java.util.concurrent.locks.Condition)},
   * except that the method parameter must accept whatever condition-like object is passed into this
   * method.
   */
  public void callAndAssertWaits(String methodName, Object conditionLikeObject) throws Exception {
    checkNotNull(methodName);
    checkNotNull(conditionLikeObject);
    // TODO: Restore the following line when Monitor.hasWaiters() no longer acquires the lock.
    // assertEquals(false, invokeMethod("hasWaiters", conditionLikeObject));
    sendRequest(methodName, conditionLikeObject);
    Thread.sleep(DUE_DILIGENCE_MILLIS);
    assertEquals(true, invokeMethod("hasWaiters", conditionLikeObject));
    assertNull(responseQueue.poll());
  }

  /**
   * Asserts that a prior call that had caused this thread to block or wait has since returned
   * normally.
   */
  public void assertPriorCallReturns(@Nullable String methodName) throws Exception {
    assertEquals(null, getResponse(methodName).getResult());
  }

  /**
   * Asserts that a prior call that had caused this thread to block or wait has since returned the
   * expected boolean value.
   */
  public void assertPriorCallReturns(boolean expected, @Nullable String methodName)
      throws Exception {
    assertEquals(expected, getResponse(methodName).getResult());
  }

  /**
   * Sends the given method call to this thread.
   *
   * @throws TimeoutException if this thread does not accept the request within a reasonable amount
   *     of time
   */
  private void sendRequest(String methodName, Object... arguments) throws Exception {
    if (!requestQueue.offer(
        new Request(methodName, arguments), TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)) {
      throw new TimeoutException();
    }
  }

  /**
   * Receives a response from this thread.
   *
   * @throws TimeoutException if this thread does not offer a response within a reasonable amount of
   *     time
   * @throws AssertionFailedError if the given method name does not match the name of the method
   *     this thread has called most recently
   */
  private Response getResponse(String methodName) throws Exception {
    Response response = responseQueue.poll(TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
    if (response == null) {
      throw new TimeoutException();
    }
    assertEquals(methodName, response.methodName);
    return response;
  }

  private Object invokeMethod(String methodName, Object... arguments) throws Exception {
    return getMethod(methodName, arguments).invoke(lockLikeObject, arguments);
  }

  private Method getMethod(String methodName, Object... arguments) throws Exception {
    METHODS:
    for (Method method : lockLikeObject.getClass().getMethods()) {
      Class<?>[] parameterTypes = method.getParameterTypes();
      if (method.getName().equals(methodName) && (parameterTypes.length == arguments.length)) {
        for (int i = 0; i < arguments.length; i++) {
          if (!parameterTypes[i].isAssignableFrom(arguments[i].getClass())) {
            continue METHODS;
          }
        }
        return method;
      }
    }
    throw new NoSuchMethodError(methodName);
  }

  @Override
  public void run() {
    assertSame(this, Thread.currentThread());
    try {
      while (true) {
        Request request = requestQueue.take();
        Object result;
        try {
          result = invokeMethod(request.methodName, request.arguments);
        } catch (ThreadDeath death) {
          return;
        } catch (InvocationTargetException exception) {
          responseQueue.put(new Response(request.methodName, null, exception.getTargetException()));
          continue;
        } catch (Throwable throwable) {
          responseQueue.put(new Response(request.methodName, null, throwable));
          continue;
        }
        responseQueue.put(new Response(request.methodName, result, null));
      }
    } catch (ThreadDeath death) {
      return;
    } catch (InterruptedException ignored) {
      // SynchronousQueue sometimes throws InterruptedException while the threads are stopping.
    } catch (Throwable uncaught) {
      this.uncaughtThrowable = uncaught;
    }
  }

  private static class Request {
    final String methodName;
    final Object[] arguments;

    Request(String methodName, Object[] arguments) {
      this.methodName = checkNotNull(methodName);
      this.arguments = checkNotNull(arguments);
    }
  }

  private static class Response {
    final String methodName;
    final Object result;
    final Throwable throwable;

    Response(String methodName, @Nullable Object result, @Nullable Throwable throwable) {
      this.methodName = methodName;
      this.result = result;
      this.throwable = throwable;
    }

    Object getResult() {
      if (throwable != null) {
        throw (AssertionFailedError) new AssertionFailedError().initCause(throwable);
      }
      return result;
    }

    Throwable getThrowable() {
      assertNotNull(throwable);
      return throwable;
    }
  }
}
