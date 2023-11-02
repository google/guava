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
import static org.junit.Assert.assertThrows;

import com.google.common.testing.TearDown;
import com.google.common.testing.TearDownStack;
import com.google.common.util.concurrent.testing.TestingExecutors;
import java.lang.Thread.UncaughtExceptionHandler;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import junit.framework.TestCase;

/**
 * Unit test for {@link AbstractExecutionThreadService}.
 *
 * @author Jesse Wilson
 */
public class AbstractExecutionThreadServiceTest extends TestCase {

  private final TearDownStack tearDownStack = new TearDownStack(true);
  private final CountDownLatch enterRun = new CountDownLatch(1);
  private final CountDownLatch exitRun = new CountDownLatch(1);

  private Thread executionThread;
  private Throwable thrownByExecutionThread;
  private final Executor exceptionCatchingExecutor =
      new Executor() {
        @Override
        public void execute(Runnable command) {
          executionThread = new Thread(command);
          executionThread.setUncaughtExceptionHandler(
              new UncaughtExceptionHandler() {
                @Override
                public void uncaughtException(Thread thread, Throwable e) {
                  thrownByExecutionThread = e;
                }
              });
          executionThread.start();
        }
      };

  @Override
  protected final void tearDown() {
    tearDownStack.runTearDown();
    assertNull(
        "exceptions should not be propagated to uncaught exception handlers",
        thrownByExecutionThread);
  }

  public void testServiceStartStop() throws Exception {
    WaitOnRunService service = new WaitOnRunService();
    assertFalse(service.startUpCalled);

    service.startAsync().awaitRunning();
    assertTrue(service.startUpCalled);
    assertEquals(Service.State.RUNNING, service.state());

    enterRun.await(); // to avoid stopping the service until run() is invoked

    service.stopAsync().awaitTerminated();
    assertTrue(service.shutDownCalled);
    assertEquals(Service.State.TERMINATED, service.state());
    executionThread.join();
  }

  public void testServiceStopIdempotence() throws Exception {
    WaitOnRunService service = new WaitOnRunService();

    service.startAsync().awaitRunning();
    enterRun.await(); // to avoid stopping the service until run() is invoked

    service.stopAsync();
    service.stopAsync();
    service.stopAsync().awaitTerminated();
    assertEquals(Service.State.TERMINATED, service.state());
    service.stopAsync().awaitTerminated();
    assertEquals(Service.State.TERMINATED, service.state());

    executionThread.join();
  }

  public void testServiceExitingOnItsOwn() throws Exception {
    WaitOnRunService service = new WaitOnRunService();
    service.expectedShutdownState = Service.State.RUNNING;

    service.startAsync().awaitRunning();
    assertTrue(service.startUpCalled);
    assertEquals(Service.State.RUNNING, service.state());

    exitRun.countDown(); // the service will exit voluntarily
    executionThread.join();

    assertTrue(service.shutDownCalled);
    assertEquals(Service.State.TERMINATED, service.state());

    service.stopAsync().awaitTerminated(); // no-op
    assertEquals(Service.State.TERMINATED, service.state());
    assertTrue(service.shutDownCalled);
  }

  private class WaitOnRunService extends AbstractExecutionThreadService {
    private boolean startUpCalled = false;
    private boolean runCalled = false;
    private boolean shutDownCalled = false;
    private State expectedShutdownState = State.STOPPING;

    @Override
    protected void startUp() {
      assertFalse(startUpCalled);
      assertFalse(runCalled);
      assertFalse(shutDownCalled);
      startUpCalled = true;
      assertEquals(State.STARTING, state());
    }

    @Override
    protected void run() {
      assertTrue(startUpCalled);
      assertFalse(runCalled);
      assertFalse(shutDownCalled);
      runCalled = true;
      assertEquals(State.RUNNING, state());

      enterRun.countDown();
      try {
        exitRun.await();
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
    }

    @Override
    protected void shutDown() {
      assertTrue(startUpCalled);
      assertTrue(runCalled);
      assertFalse(shutDownCalled);
      shutDownCalled = true;
      assertEquals(expectedShutdownState, state());
    }

    @Override
    protected void triggerShutdown() {
      exitRun.countDown();
    }

    @Override
    protected Executor executor() {
      return exceptionCatchingExecutor;
    }
  }

  public void testServiceThrowOnStartUp() throws Exception {
    ThrowOnStartUpService service = new ThrowOnStartUpService();
    assertFalse(service.startUpCalled);

    service.startAsync();
    IllegalStateException expected =
        assertThrows(IllegalStateException.class, () -> service.awaitRunning());
    assertThat(expected).hasCauseThat().hasMessageThat().isEqualTo("kaboom!");
    executionThread.join();

    assertTrue(service.startUpCalled);
    assertEquals(Service.State.FAILED, service.state());
    assertThat(service.failureCause()).hasMessageThat().isEqualTo("kaboom!");
  }

  private class ThrowOnStartUpService extends AbstractExecutionThreadService {
    private boolean startUpCalled = false;

    @Override
    protected void startUp() {
      startUpCalled = true;
      throw new UnsupportedOperationException("kaboom!");
    }

    @Override
    protected void run() {
      throw new AssertionError("run() should not be called");
    }

    @Override
    protected Executor executor() {
      return exceptionCatchingExecutor;
    }
  }

  public void testServiceThrowOnRun() throws Exception {
    ThrowOnRunService service = new ThrowOnRunService();

    service.startAsync();
    IllegalStateException expected =
        assertThrows(IllegalStateException.class, () -> service.awaitTerminated());
    executionThread.join();
    assertThat(expected).hasCauseThat().isEqualTo(service.failureCause());
    assertThat(expected).hasCauseThat().hasMessageThat().isEqualTo("kaboom!");
    assertTrue(service.shutDownCalled);
    assertEquals(Service.State.FAILED, service.state());
  }

  public void testServiceThrowOnRunAndThenAgainOnShutDown() throws Exception {
    ThrowOnRunService service = new ThrowOnRunService();
    service.throwOnShutDown = true;

    service.startAsync();
    IllegalStateException expected =
        assertThrows(IllegalStateException.class, () -> service.awaitTerminated());
    executionThread.join();
    assertThat(expected).hasCauseThat().isEqualTo(service.failureCause());
    assertThat(expected).hasCauseThat().hasMessageThat().isEqualTo("kaboom!");
    assertTrue(service.shutDownCalled);
    assertEquals(Service.State.FAILED, service.state());
  }

  private class ThrowOnRunService extends AbstractExecutionThreadService {
    private boolean shutDownCalled = false;
    private boolean throwOnShutDown = false;

    @Override
    protected void run() {
      throw new UnsupportedOperationException("kaboom!");
    }

    @Override
    protected void shutDown() {
      shutDownCalled = true;
      if (throwOnShutDown) {
        throw new UnsupportedOperationException("double kaboom!");
      }
    }

    @Override
    protected Executor executor() {
      return exceptionCatchingExecutor;
    }
  }

  public void testServiceThrowOnShutDown() throws Exception {
    ThrowOnShutDown service = new ThrowOnShutDown();

    service.startAsync().awaitRunning();
    assertEquals(Service.State.RUNNING, service.state());

    service.stopAsync();
    enterRun.countDown();
    executionThread.join();

    assertEquals(Service.State.FAILED, service.state());
    assertThat(service.failureCause()).hasMessageThat().isEqualTo("kaboom!");
  }

  private class ThrowOnShutDown extends AbstractExecutionThreadService {
    @Override
    protected void run() {
      try {
        enterRun.await();
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
    }

    @Override
    protected void shutDown() {
      throw new UnsupportedOperationException("kaboom!");
    }

    @Override
    protected Executor executor() {
      return exceptionCatchingExecutor;
    }
  }

  public void testServiceTimeoutOnStartUp() throws Exception {
    TimeoutOnStartUp service = new TimeoutOnStartUp();

    TimeoutException e =
        assertThrows(
            TimeoutException.class,
            () -> service.startAsync().awaitRunning(1, TimeUnit.MILLISECONDS));
    assertThat(e.getMessage()).contains(Service.State.STARTING.toString());
  }

  private class TimeoutOnStartUp extends AbstractExecutionThreadService {
    @Override
    protected Executor executor() {
      return new Executor() {
        @Override
        public void execute(Runnable command) {}
      };
    }

    @Override
    protected void run() throws Exception {}
  }

  public void testStopWhileStarting_runNotCalled() throws Exception {
    final CountDownLatch started = new CountDownLatch(1);
    FakeService service =
        new FakeService() {
          @Override
          protected void startUp() throws Exception {
            super.startUp();
            started.await();
          }
        };
    service.startAsync();
    service.stopAsync();
    started.countDown();
    service.awaitTerminated();
    assertEquals(Service.State.TERMINATED, service.state());
    assertEquals(1, service.startupCalled);
    assertEquals(0, service.runCalled);
    assertEquals(1, service.shutdownCalled);
  }

  public void testStop_noStart() {
    FakeService service = new FakeService();
    service.stopAsync().awaitTerminated();
    assertEquals(Service.State.TERMINATED, service.state());
    assertEquals(0, service.startupCalled);
    assertEquals(0, service.runCalled);
    assertEquals(0, service.shutdownCalled);
  }

  public void testDefaultService() throws InterruptedException {
    WaitOnRunService service = new WaitOnRunService();
    service.startAsync().awaitRunning();
    enterRun.await();
    service.stopAsync().awaitTerminated();
  }

  public void testTimeout() {
    // Create a service whose executor will never run its commands
    Service service =
        new AbstractExecutionThreadService() {
          @Override
          protected void run() throws Exception {}

          @Override
          protected ScheduledExecutorService executor() {
            return TestingExecutors.noOpScheduledExecutor();
          }

          @Override
          protected String serviceName() {
            return "Foo";
          }
        };
    TimeoutException e =
        assertThrows(
            TimeoutException.class,
            () -> service.startAsync().awaitRunning(1, TimeUnit.MILLISECONDS));
    assertThat(e)
        .hasMessageThat()
        .isEqualTo("Timed out waiting for Foo [STARTING] to reach the RUNNING state.");
  }

  private class FakeService extends AbstractExecutionThreadService implements TearDown {

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    FakeService() {
      tearDownStack.addTearDown(this);
    }

    volatile int startupCalled = 0;
    volatile int shutdownCalled = 0;
    volatile int runCalled = 0;

    @Override
    protected void startUp() throws Exception {
      assertEquals(0, startupCalled);
      assertEquals(0, runCalled);
      assertEquals(0, shutdownCalled);
      startupCalled++;
    }

    @Override
    protected void run() throws Exception {
      assertEquals(1, startupCalled);
      assertEquals(0, runCalled);
      assertEquals(0, shutdownCalled);
      runCalled++;
    }

    @Override
    protected void shutDown() throws Exception {
      assertEquals(1, startupCalled);
      assertEquals(0, shutdownCalled);
      assertEquals(Service.State.STOPPING, state());
      shutdownCalled++;
    }

    @Override
    protected Executor executor() {
      return executor;
    }

    @Override
    public void tearDown() throws Exception {
      executor.shutdown();
    }
  }
}
