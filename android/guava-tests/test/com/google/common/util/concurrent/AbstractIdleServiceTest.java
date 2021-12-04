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

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.util.concurrent.MoreExecutors.directExecutor;

import com.google.common.collect.Lists;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import junit.framework.TestCase;

/**
 * Tests for {@link AbstractIdleService}.
 *
 * @author Chris Nokleberg
 * @author Ben Yu
 */
public class AbstractIdleServiceTest extends TestCase {

  // Functional tests using real thread. We only verify publicly visible state.
  // Interaction assertions are done by the single-threaded unit tests.

  public static class FunctionalTest extends TestCase {

    private static class DefaultService extends AbstractIdleService {
      @Override
      protected void startUp() throws Exception {}

      @Override
      protected void shutDown() throws Exception {}
    }

    public void testServiceStartStop() throws Exception {
      AbstractIdleService service = new DefaultService();
      service.startAsync().awaitRunning();
      assertEquals(Service.State.RUNNING, service.state());
      service.stopAsync().awaitTerminated();
      assertEquals(Service.State.TERMINATED, service.state());
    }

    public void testStart_failed() throws Exception {
      final Exception exception = new Exception("deliberate");
      AbstractIdleService service =
          new DefaultService() {
            @Override
            protected void startUp() throws Exception {
              throw exception;
            }
          };
      try {
        service.startAsync().awaitRunning();
        fail();
      } catch (RuntimeException e) {
        assertThat(e).hasCauseThat().isSameInstanceAs(exception);
      }
      assertEquals(Service.State.FAILED, service.state());
    }

    public void testStop_failed() throws Exception {
      final Exception exception = new Exception("deliberate");
      AbstractIdleService service =
          new DefaultService() {
            @Override
            protected void shutDown() throws Exception {
              throw exception;
            }
          };
      service.startAsync().awaitRunning();
      try {
        service.stopAsync().awaitTerminated();
        fail();
      } catch (RuntimeException e) {
        assertThat(e).hasCauseThat().isSameInstanceAs(exception);
      }
      assertEquals(Service.State.FAILED, service.state());
    }
  }

  public void testStart() {
    TestService service = new TestService();
    assertEquals(0, service.startUpCalled);
    service.startAsync().awaitRunning();
    assertEquals(1, service.startUpCalled);
    assertEquals(Service.State.RUNNING, service.state());
    assertThat(service.transitionStates).containsExactly(Service.State.STARTING);
  }

  public void testStart_failed() {
    final Exception exception = new Exception("deliberate");
    TestService service =
        new TestService() {
          @Override
          protected void startUp() throws Exception {
            super.startUp();
            throw exception;
          }
        };
    assertEquals(0, service.startUpCalled);
    try {
      service.startAsync().awaitRunning();
      fail();
    } catch (RuntimeException e) {
      assertThat(e).hasCauseThat().isSameInstanceAs(exception);
    }
    assertEquals(1, service.startUpCalled);
    assertEquals(Service.State.FAILED, service.state());
    assertThat(service.transitionStates).containsExactly(Service.State.STARTING);
  }

  public void testStop_withoutStart() {
    TestService service = new TestService();
    service.stopAsync().awaitTerminated();
    assertEquals(0, service.startUpCalled);
    assertEquals(0, service.shutDownCalled);
    assertEquals(Service.State.TERMINATED, service.state());
    assertThat(service.transitionStates).isEmpty();
  }

  public void testStop_afterStart() {
    TestService service = new TestService();
    service.startAsync().awaitRunning();
    assertEquals(1, service.startUpCalled);
    assertEquals(0, service.shutDownCalled);
    service.stopAsync().awaitTerminated();
    assertEquals(1, service.startUpCalled);
    assertEquals(1, service.shutDownCalled);
    assertEquals(Service.State.TERMINATED, service.state());
    assertThat(service.transitionStates)
        .containsExactly(Service.State.STARTING, Service.State.STOPPING)
        .inOrder();
  }

  public void testStop_failed() {
    final Exception exception = new Exception("deliberate");
    TestService service =
        new TestService() {
          @Override
          protected void shutDown() throws Exception {
            super.shutDown();
            throw exception;
          }
        };
    service.startAsync().awaitRunning();
    assertEquals(1, service.startUpCalled);
    assertEquals(0, service.shutDownCalled);
    try {
      service.stopAsync().awaitTerminated();
      fail();
    } catch (RuntimeException e) {
      assertThat(e).hasCauseThat().isSameInstanceAs(exception);
    }
    assertEquals(1, service.startUpCalled);
    assertEquals(1, service.shutDownCalled);
    assertEquals(Service.State.FAILED, service.state());
    assertThat(service.transitionStates)
        .containsExactly(Service.State.STARTING, Service.State.STOPPING)
        .inOrder();
  }

  public void testServiceToString() {
    AbstractIdleService service = new TestService();
    assertEquals("TestService [NEW]", service.toString());
    service.startAsync().awaitRunning();
    assertEquals("TestService [RUNNING]", service.toString());
    service.stopAsync().awaitTerminated();
    assertEquals("TestService [TERMINATED]", service.toString());
  }

  public void testTimeout() throws Exception {
    // Create a service whose executor will never run its commands
    Service service =
        new TestService() {
          @Override
          protected Executor executor() {
            return new Executor() {
              @Override
              public void execute(Runnable command) {}
            };
          }

          @Override
          protected String serviceName() {
            return "Foo";
          }
        };
    try {
      service.startAsync().awaitRunning(1, TimeUnit.MILLISECONDS);
      fail("Expected timeout");
    } catch (TimeoutException e) {
      assertThat(e)
          .hasMessageThat()
          .isEqualTo("Timed out waiting for Foo [STARTING] to reach the RUNNING state.");
    }
  }

  private static class TestService extends AbstractIdleService {
    int startUpCalled = 0;
    int shutDownCalled = 0;
    final List<State> transitionStates = Lists.newArrayList();

    @Override
    protected void startUp() throws Exception {
      assertEquals(0, startUpCalled);
      assertEquals(0, shutDownCalled);
      startUpCalled++;
      assertEquals(State.STARTING, state());
    }

    @Override
    protected void shutDown() throws Exception {
      assertEquals(1, startUpCalled);
      assertEquals(0, shutDownCalled);
      shutDownCalled++;
      assertEquals(State.STOPPING, state());
    }

    @Override
    protected Executor executor() {
      transitionStates.add(state());
      return directExecutor();
    }
  }
}
