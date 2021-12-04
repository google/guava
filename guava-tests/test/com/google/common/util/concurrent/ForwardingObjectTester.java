/*
 * Copyright (C) 2012 The Guava Authors
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

import static org.mockito.Answers.CALLS_REAL_METHODS;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import com.google.common.base.Function;
import com.google.common.collect.ForwardingObject;
import com.google.common.collect.Iterables;
import com.google.common.testing.ForwardingWrapperTester;
import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * Tester for typical subclass of {@link ForwardingObject} by using EasyMock partial mocks.
 *
 * @author Ben Yu
 */
final class ForwardingObjectTester {

  private static final Method DELEGATE_METHOD;

  static {
    try {
      DELEGATE_METHOD = ForwardingObject.class.getDeclaredMethod("delegate");
      DELEGATE_METHOD.setAccessible(true);
    } catch (SecurityException e) {
      throw new RuntimeException(e);
    } catch (NoSuchMethodException e) {
      throw new AssertionError(e);
    }
  }

  /**
   * Ensures that all interface methods of {@code forwarderClass} are forwarded to the {@link
   * ForwardingObject#delegate}. {@code forwarderClass} is assumed to only implement one interface.
   */
  static <T extends ForwardingObject> void testForwardingObject(final Class<T> forwarderClass) {
    @SuppressWarnings("unchecked") // super interface type of T
    Class<? super T> interfaceType =
        (Class<? super T>) Iterables.getOnlyElement(Arrays.asList(forwarderClass.getInterfaces()));
    new ForwardingWrapperTester()
        .testForwarding(
            interfaceType,
            new Function<Object, T>() {
              @Override
              public T apply(Object delegate) {
                T mock = mock(forwarderClass, CALLS_REAL_METHODS);
                try {
                  T stubber = doReturn(delegate).when(mock);
                  DELEGATE_METHOD.invoke(stubber);
                } catch (Exception e) {
                  throw new RuntimeException(e);
                }
                return mock;
              }
            });
  }
}
