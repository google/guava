/*
 * Copyright (C) 2009 The Guava Authors
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

import com.google.common.util.concurrent.Service.State;

import junit.framework.TestCase;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Unit test for {@link AbstractIdleService}.
 *
 * @author Chris Nokleberg
 */
public class AbstractIdleServiceTest extends TestCase {
  private Thread executorThread;
  private Throwable thrownByExecutorThread;
  private final Executor executor = new Executor() {
    @Override
    public void execute(Runnable command) {
      executorThread = new Thread(command);
      executorThread.setUncaughtExceptionHandler(
          new UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread thread, Throwable e) {
              thrownByExecutorThread = e;
            }
          });
      executorThread.start();
    }
  };

  public void testServiceStartStop() throws Exception {
    NullService service = new NullService();
    assertFalse(service.startUpCalled);

    service.start().get();
    assertTrue(service.startUpCalled);
    assertEquals(Service.State.RUNNING, service.state());

    service.stop().get();
    assertTrue(service.shutDownCalled);
    assertEquals(Service.State.TERMINATED, service.state());
    executorThread.join();
    assertNull(thrownByExecutorThread);
  }

  public void testServiceToString() throws Exception {
    NullService service = new NullService();
    assertEquals("NullService [" + Service.State.NEW + "]", service.toString());
    service.start().get();
    assertEquals("NullService [" + Service.State.RUNNING + "]", service.toString());
    service.stop().get();
    assertEquals("NullService [" + Service.State.TERMINATED + "]", service.toString());
  }

  public void testTimeout() throws Exception {
    // Create a service whose executor will never run its commands
    Service service = new NullService() {
      @Override protected Executor executor(Service.State state) {
        return new Executor() {
          @Override public void execute(Runnable command) {
          }
        };
      }
    };

    try {
      service.start().get(1, TimeUnit.MILLISECONDS);
      fail("Expected timeout");
    } catch (TimeoutException e) {
      assertTrue(e.getMessage().contains(State.STARTING.toString()));
    }
  }

  private class NullService extends AbstractIdleService {
    boolean startUpCalled = false;
    boolean shutDownCalled = false;
    State expectedShutdownState = State.STOPPING;

    @Override protected void startUp() {
      assertFalse(startUpCalled);
      assertFalse(shutDownCalled);
      startUpCalled = true;
      assertEquals(State.STARTING, state());
    }

    @Override protected void shutDown() {
      assertTrue(startUpCalled);
      assertFalse(shutDownCalled);
      shutDownCalled = true;
      assertEquals(expectedShutdownState, state());
    }

    @Override protected Executor executor(Service.State state) {
      switch (state) {
        case STARTING:
          assertFalse(startUpCalled);
          return executor;
        case STOPPING:
          assertTrue(startUpCalled);
          assertFalse(shutDownCalled);
          return executor;
        default:
          throw new IllegalStateException("unexpected state " + state);
      }
    }
  }
}
