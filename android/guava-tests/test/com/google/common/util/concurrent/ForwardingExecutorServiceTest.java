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

import static com.google.common.base.StandardSystemProperty.JAVA_SPECIFICATION_VERSION;
import static com.google.common.truth.Truth.assertThat;
import static java.lang.Integer.parseInt;
import static java.util.concurrent.TimeUnit.SECONDS;

import java.lang.reflect.Method;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import junit.framework.TestCase;
import org.jspecify.annotations.NullUnmarked;

/** Unit tests for {@link ForwardingExecutorService} */
@NullUnmarked
public class ForwardingExecutorServiceTest extends TestCase {
  public void testForwarding() {
    ForwardingObjectTester.testForwardingObject(ForwardingExecutorService.class);
  }

  public void testNoForwardingOfDefaultMethod() throws Exception {
    ExecutorService delegate =
        new ThreadPoolExecutor(0, Integer.MAX_VALUE, 60, SECONDS, new SynchronousQueue<>()) {
          @Override
          public void close() {
            throw new AssertionError(
                "ForwardingExecutorService should have used the default method"
                    + " ExecutorService.close() (which would forward to methods like shutdown() on"
                    + " the delegate) instead of forwarding to delegate.close()");
          }
        };
    ExecutorService wrapper =
        new ForwardingExecutorService() {
          @Override
          protected ExecutorService delegate() {
            return delegate;
          }
        };
    Method closeMethod;
    try {
      closeMethod = wrapper.getClass().getMethod("close");
    } catch (NoSuchMethodException e) {
      assertThat(isAndroid() || isBeforeJava19()).isTrue();
      return; // close() doesn't exist, so we can't test it.
    }
    closeMethod.invoke(wrapper);
    assertThat(delegate.isTerminated()).isTrue();
  }

  private static boolean isAndroid() {
    return System.getProperty("java.runtime.name", "").contains("Android");
  }

  private static boolean isBeforeJava19() {
    return JAVA_SPECIFICATION_VERSION.value().equals("1.8")
        || parseInt(JAVA_SPECIFICATION_VERSION.value()) < 19;
  }
}
