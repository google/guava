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

package com.google.common.eventbus;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.testing.EqualsTester;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import junit.framework.TestCase;

/**
 * Tests for {@link Subscriber}.
 *
 * @author Cliff Biffle
 * @author Colin Decker
 */
public class SubscriberTest extends TestCase {

  private static final Object FIXTURE_ARGUMENT = new Object();

  private EventBus bus;
  private boolean methodCalled;
  private Object methodArgument;

  @Override
  protected void setUp() throws Exception {
    bus = new EventBus();
    methodCalled = false;
    methodArgument = null;
  }

  public void testCreate() {
    Subscriber s1 = Subscriber.create(bus, this, getTestSubscriberMethod("recordingMethod"));
    assertThat(s1).isInstanceOf(Subscriber.SynchronizedSubscriber.class);

    // a thread-safe method should not create a synchronized subscriber
    Subscriber s2 = Subscriber.create(bus, this, getTestSubscriberMethod("threadSafeMethod"));
    assertThat(s2).isNotInstanceOf(Subscriber.SynchronizedSubscriber.class);
  }

  public void testInvokeSubscriberMethod_basicMethodCall() throws Throwable {
    Method method = getTestSubscriberMethod("recordingMethod");
    Subscriber subscriber = Subscriber.create(bus, this, method);

    subscriber.invokeSubscriberMethod(FIXTURE_ARGUMENT);

    assertTrue("Subscriber must call provided method", methodCalled);
    assertTrue(
        "Subscriber argument must be exactly the provided object.",
        methodArgument == FIXTURE_ARGUMENT);
  }

  public void testInvokeSubscriberMethod_exceptionWrapping() throws Throwable {
    Method method = getTestSubscriberMethod("exceptionThrowingMethod");
    Subscriber subscriber = Subscriber.create(bus, this, method);

    try {
      subscriber.invokeSubscriberMethod(FIXTURE_ARGUMENT);
      fail("Subscribers whose methods throw must throw InvocationTargetException");
    } catch (InvocationTargetException expected) {
      assertThat(expected).hasCauseThat().isInstanceOf(IntentionalException.class);
    }
  }

  public void testInvokeSubscriberMethod_errorPassthrough() throws Throwable {
    Method method = getTestSubscriberMethod("errorThrowingMethod");
    Subscriber subscriber = Subscriber.create(bus, this, method);

    try {
      subscriber.invokeSubscriberMethod(FIXTURE_ARGUMENT);
      fail("Subscribers whose methods throw Errors must rethrow them");
    } catch (JudgmentError expected) {
    }
  }

  public void testEquals() throws Exception {
    Method charAt = String.class.getMethod("charAt", int.class);
    Method concat = String.class.getMethod("concat", String.class);
    new EqualsTester()
        .addEqualityGroup(
            Subscriber.create(bus, "foo", charAt), Subscriber.create(bus, "foo", charAt))
        .addEqualityGroup(Subscriber.create(bus, "bar", charAt))
        .addEqualityGroup(Subscriber.create(bus, "foo", concat))
        .testEquals();
  }

  private Method getTestSubscriberMethod(String name) {
    try {
      return getClass().getDeclaredMethod(name, Object.class);
    } catch (NoSuchMethodException e) {
      throw new AssertionError();
    }
  }

  /**
   * Records the provided object in {@link #methodArgument} and sets {@link #methodCalled}. This
   * method is called reflectively by Subscriber during tests, and must remain public.
   *
   * @param arg argument to record.
   */
  @Subscribe
  public void recordingMethod(Object arg) {
    assertFalse(methodCalled);
    methodCalled = true;
    methodArgument = arg;
  }

  @Subscribe
  public void exceptionThrowingMethod(Object arg) throws Exception {
    throw new IntentionalException();
  }

  /** Local exception subclass to check variety of exception thrown. */
  class IntentionalException extends Exception {

    private static final long serialVersionUID = -2500191180248181379L;
  }

  @Subscribe
  public void errorThrowingMethod(Object arg) {
    throw new JudgmentError();
  }

  @Subscribe
  @AllowConcurrentEvents
  public void threadSafeMethod(Object arg) {}

  /** Local Error subclass to check variety of error thrown. */
  class JudgmentError extends Error {

    private static final long serialVersionUID = 634248373797713373L;
  }
}
