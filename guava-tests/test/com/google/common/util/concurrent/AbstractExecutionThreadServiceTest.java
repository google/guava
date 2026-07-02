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
import static com.google.common.truth.Truth.assertWithMessage;
import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.junit.Assert.assertThrows;

import com.google.common.annotations.GwtIncompatible;
import com.google.common.annotations.J2ktIncompatible;
import com.google.common.testing.TearDown;
import com.google.common.testing.TearDownStack;
import com.google.common.util.concurrent.testing.TestingExecutors;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeoutException;
import junit.framework.TestCase;
import org.jspecify.annotations.NullUnmarked;

/**
 * Unit test for {@link AbstractExecutionThreadService}.
 *
 * @author Jesse Wilson
 */
@NullUnmarked
@GwtIncompatible
@J2ktIncompatible
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
              (thread, throwable) -> thrownByExecutionThread = throwable);
          executionThread.start();
        }
      };

  @Override
  protected final void tearDown() {
    tearDownStack.runTearDown();
    assertWithMessage("exceptions should not be propagated to uncaught exception handlers")
        .that(thrownByExecutionThread)
        .isNull();
  }

  public void testServiceStartStop() throws Exception {
    WaitOnRunService service = new WaitOnRunService();
    assertFalse(service.startUpCalled);

    service.startAsync().awaitRunning();
    assertTrue(service.startUpCalled);
    assertThat(service.state()).isEqualTo(Service.State.RUNNING);

    enterRun.await(); // to avoid stopping the service until run() is invoked

    service.stopAsync().awaitTerminated();
    assertTrue(service.shutDownCalled);
    assertThat(service.state()).isEqualTo(Service.State.TERMINATED);
    executionThread.join();
  }

  public void testServiceStopIdempotence() throws Exception {
    WaitOnRunService service = new WaitOnRunService();

    service.startAsync().awaitRunning();
    enterRun.await(); // to avoid stopping the service until run() is invoked

    service.stopAsync();
    service.stopAsync();
    service.stopAsync().awaitTerminated();
    assertThat(service.state()).isEqualTo(Service.State.TERMINATED);
    service.stopAsync().awaitTerminated();
    assertThat(service.state()).isEqualTo(Service.State.TERMINATED);

    executionThread.join();
  }

  public void testServiceExitingOnItsOwn() throws Exception {
    WaitOnRunService service = new WaitOnRunService();
    service.expectedShutdownState = Service.State.RUNNING;

    service.startAsync().awaitRunning();
    assertTrue(service.startUpCalled);
    assertThat(service.state()).isEqualTo(Service.State.RUNNING);

    exitRun.countDown(); // the service will exit voluntarily
    executionThread.join();

    assertTrue(service.shutDownCalled);
    assertThat(service.state()).isEqualTo(Service.State.TERMINATED);

    service.stopAsync().awaitTerminated(); // no-op
    assertThat(service.state()).isEqualTo(Service.State.TERMINATED);
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
      assertThat(state()).isEqualTo(State.STARTING);
    }

    @Override
    protected void run() {
      assertTrue(startUpCalled);
      assertFalse(runCalled);
      assertFalse(shutDownCalled);
      runCalled = true;
      assertThat(state()).isEqualTo(State.RUNNING);

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
      assertThat(state()).isEqualTo(expectedShutdownState);
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
        assertThrows(IllegalStateException.class, service::awaitRunning);
    assertThat(expected).hasCauseThat().hasMessageThat().isEqualTo("kaboom!");
    executionThread.join();

    assertTrue(service.startUpCalled);
    assertThat(service.state()).isEqualTo(Service.State.FAILED);
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
        assertThrows(IllegalStateException.class, service::awaitTerminated);
    executionThread.join();
    assertThat(expected).hasCauseThat().isEqualTo(service.failureCause());
    assertThat(expected).hasCauseThat().hasMessageThat().isEqualTo("kaboom!");
    assertTrue(service.shutDownCalled);
    assertThat(service.state()).isEqualTo(Service.State.FAILED);
  }

  public void testServiceThrowOnRunAndThenAgainOnShutDown() throws Exception {
    ThrowOnRunService service = new ThrowOnRunService();
    service.throwOnShutDown = true;

    service.startAsync();
    IllegalStateException expected =
        assertThrows(IllegalStateException.class, service::awaitTerminated);
    executionThread.join();
    assertThat(expected).hasCauseThat().isEqualTo(service.failureCause());
    assertThat(expected).hasCauseThat().hasMessageThat().isEqualTo("kaboom!");
    assertTrue(service.shutDownCalled);
    assertThat(service.state()).isEqualTo(Service.State.FAILED);
    assertThat(expected.getCause().getSuppressed()[0]).hasMessageThat().isEqualTo("double kaboom!");
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
    assertThat(service.state()).isEqualTo(Service.State.RUNNING);

    service.stopAsync();
    enterRun.countDown();
    executionThread.join();

    assertThat(service.state()).isEqualTo(Service.State.FAILED);
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

  public void testServiceTimeoutOnStartUp() {
    TimeoutOnStartUp service = new TimeoutOnStartUp();

    TimeoutException e =
        assertThrows(
            TimeoutException.class, () -> service.startAsync().awaitRunning(1, MILLISECONDS));
    assertThat(e).hasMessageThat().contains(Service.State.STARTING.toString());
  }

  private static final class TimeoutOnStartUp extends AbstractExecutionThreadService {
    @Override
    protected Executor executor() {
      return new Executor() {
        @Override
        public void execute(Runnable command) {}
      };
    }

    @Override
    protected void run() {}
  }

  public void testStopWhileStarting_runNotCalled() {
    CountDownLatch started = new CountDownLatch(1);
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
    assertThat(service.state()).isEqualTo(Service.State.TERMINATED);
    assertEquals(1, service.startupCalled);
    assertEquals(0, service.runCalled);
    assertEquals(1, service.shutdownCalled);
  }

  public void testStop_noStart() {
    FakeService service = new FakeService();
    service.stopAsync().awaitTerminated();
    assertThat(service.state()).isEqualTo(Service.State.TERMINATED);
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
          protected void run() {}

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
            TimeoutException.class, () -> service.startAsync().awaitRunning(1, MILLISECONDS));
    assertThat(e)
        .hasMessageThat()
        .isEqualTo("Timed out waiting for Foo [STARTING] to reach the RUNNING state.");
  }

  private class FakeService extends AbstractExecutionThreadService implements TearDown {

    private final ExecutorService executor = newSingleThreadExecutor();

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
    protected void run() {
      assertEquals(1, startupCalled);
      assertEquals(0, runCalled);
      assertEquals(0, shutdownCalled);
      runCalled++;
    }

    @Override
    protected void shutDown() {
      assertEquals(1, startupCalled);
      assertEquals(0, shutdownCalled);
      assertThat(state()).isEqualTo(State.STOPPING);
      shutdownCalled++;
    }

    @Override
    protected Executor executor() {
      return executor;
    }

    @Override
    public void tearDown() {
      executor.shutdown();
    }
  }
}
