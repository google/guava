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

import static java.lang.Thread.currentThread;
import static java.util.concurrent.TimeUnit.SECONDS;

import junit.framework.Assert;
import junit.framework.TestCase;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;

/**
 * Unit test for {@link AbstractService}.
 *
 * @author Jesse Wilson
 */
public class AbstractServiceTest extends TestCase {

  private Thread executionThread;
  private Throwable thrownByExecutionThread;

  public void testNoOpServiceStartStop() {
    NoOpService service = new NoOpService();
    Assert.assertEquals(Service.State.NEW, service.state());
    assertFalse(service.isRunning());
    assertFalse(service.running);

    service.start();
    assertEquals(Service.State.RUNNING, service.state());
    assertTrue(service.isRunning());
    assertTrue(service.running);

    service.stop();
    assertEquals(Service.State.TERMINATED, service.state());
    assertFalse(service.isRunning());
    assertFalse(service.running);
  }

  public void testNoOpServiceStartAndWaitStopAndWait() throws Exception {
    NoOpService service = new NoOpService();

    service.start().get();
    assertEquals(Service.State.RUNNING, service.state());

    service.stop().get();
    assertEquals(Service.State.TERMINATED, service.state());
  }

  public void testNoOpServiceStartStopIdempotence() throws Exception {
    NoOpService service = new NoOpService();

    service.start();
    service.start();
    assertEquals(Service.State.RUNNING, service.state());

    service.stop();
    service.stop();
    assertEquals(Service.State.TERMINATED, service.state());
  }

  public void testNoOpServiceStartStopIdempotenceAfterWait() throws Exception {
    NoOpService service = new NoOpService();

    service.start().get();
    service.start();
    assertEquals(Service.State.RUNNING, service.state());

    service.stop().get();
    service.stop();
    assertEquals(Service.State.TERMINATED, service.state());
  }

  public void testNoOpServiceStartStopIdempotenceDoubleWait() throws Exception {
    NoOpService service = new NoOpService();

    service.start().get();
    service.start().get();
    assertEquals(Service.State.RUNNING, service.state());

    service.stop().get();
    service.stop().get();
    assertEquals(Service.State.TERMINATED, service.state());
  }

  public void testNoOpServiceStartStopAndWaitUninterruptible()
      throws Exception {
    NoOpService service = new NoOpService();

    currentThread().interrupt();
    try {
      service.startAndWait();
      assertEquals(Service.State.RUNNING, service.state());

      service.stopAndWait();
      assertEquals(Service.State.TERMINATED, service.state());

      assertTrue(currentThread().isInterrupted());
    } finally {
      Thread.interrupted(); // clear interrupt for future tests
    }
  }

  private static class NoOpService extends AbstractService {
    boolean running = false;

    @Override protected void doStart() {
      assertFalse(running);
      running = true;
      notifyStarted();
    }

    @Override protected void doStop() {
      assertTrue(running);
      running = false;
      notifyStopped();
    }
  }

  public void testManualServiceStartStop() {
    ManualSwitchedService service = new ManualSwitchedService();

    service.start();
    assertEquals(Service.State.STARTING, service.state());
    assertFalse(service.isRunning());
    assertTrue(service.doStartCalled);

    service.notifyStarted(); // usually this would be invoked by another thread
    assertEquals(Service.State.RUNNING, service.state());
    assertTrue(service.isRunning());

    service.stop();
    assertEquals(Service.State.STOPPING, service.state());
    assertFalse(service.isRunning());
    assertTrue(service.doStopCalled);

    service.notifyStopped(); // usually this would be invoked by another thread
    assertEquals(Service.State.TERMINATED, service.state());
    assertFalse(service.isRunning());
  }

  public void testManualServiceStopWhileStarting() {
    ManualSwitchedService service = new ManualSwitchedService();

    service.start();
    assertEquals(Service.State.STARTING, service.state());
    assertFalse(service.isRunning());
    assertTrue(service.doStartCalled);

    service.stop();
    assertEquals(Service.State.STOPPING, service.state());
    assertFalse(service.isRunning());
    assertFalse(service.doStopCalled);

    service.notifyStarted();
    assertEquals(Service.State.STOPPING, service.state());
    assertFalse(service.isRunning());
    assertTrue(service.doStopCalled);

    service.notifyStopped();
    assertEquals(Service.State.TERMINATED, service.state());
    assertFalse(service.isRunning());
  }

  public void testManualServiceUnrequestedStop() {
    ManualSwitchedService service = new ManualSwitchedService();

    service.start();

    service.notifyStarted();
    assertEquals(Service.State.RUNNING, service.state());
    assertTrue(service.isRunning());
    assertFalse(service.doStopCalled);

    service.notifyStopped();
    assertEquals(Service.State.TERMINATED, service.state());
    assertFalse(service.isRunning());
    assertFalse(service.doStopCalled);
  }

  /**
   * The user of this service should call {@link #notifyStarted} and {@link
   * #notifyStopped} after calling {@link #start} and {@link #stop}.
   */
  private static class ManualSwitchedService extends AbstractService {
    boolean doStartCalled = false;
    boolean doStopCalled = false;

    @Override protected void doStart() {
      assertFalse(doStartCalled);
      doStartCalled = true;
    }

    @Override protected void doStop() {
      assertFalse(doStopCalled);
      doStopCalled = true;
    }
  }

  public void testThreadedServiceStartAndWaitStopAndWait() throws Throwable {
    ThreadedService service = new ThreadedService();

    service.start().get();
    assertEquals(Service.State.RUNNING, service.state());

    service.awaitRunChecks();

    service.stop().get();
    assertEquals(Service.State.TERMINATED, service.state());

    throwIfSet(thrownByExecutionThread);
  }

  public void testThreadedServiceStartStopIdempotence() throws Throwable {
    ThreadedService service = new ThreadedService();

    service.start();
    service.start().get();
    assertEquals(Service.State.RUNNING, service.state());

    service.awaitRunChecks();

    service.stop();
    service.stop().get();
    assertEquals(Service.State.TERMINATED, service.state());

    throwIfSet(thrownByExecutionThread);
  }

  public void testThreadedServiceStartStopIdempotenceAfterWait()
      throws Throwable {
    ThreadedService service = new ThreadedService();

    service.start().get();
    service.start();
    assertEquals(Service.State.RUNNING, service.state());

    service.awaitRunChecks();

    service.stop().get();
    service.stop();
    assertEquals(Service.State.TERMINATED, service.state());

    executionThread.join();

    throwIfSet(thrownByExecutionThread);
  }

  public void testThreadedServiceStartStopIdempotenceDoubleWait()
      throws Throwable {
    ThreadedService service = new ThreadedService();

    service.start().get();
    service.start().get();
    assertEquals(Service.State.RUNNING, service.state());

    service.awaitRunChecks();

    service.stop().get();
    service.stop().get();
    assertEquals(Service.State.TERMINATED, service.state());

    throwIfSet(thrownByExecutionThread);
  }

  private class ThreadedService extends AbstractService {
    final CountDownLatch hasConfirmedIsRunning = new CountDownLatch(1);

    /*
     * The main test thread tries to stop() the service shortly after
     * confirming that it is running. Meanwhile, the service itself is trying
     * to confirm that it is running. If the main thread's stop() call happens
     * before it has the chance, the test will fail. To avoid this, the main
     * thread calls this method, which waits until the service has performed
     * its own "running" check.
     */
    void awaitRunChecks() throws InterruptedException {
      assertTrue("Service thread hasn't finished its checks. "
          + "Exception status (possibly stale): " + thrownByExecutionThread,
          hasConfirmedIsRunning.await(10, SECONDS));
    }

    @Override protected void doStart() {
      assertEquals(State.STARTING, state());
      invokeOnExecutionThreadForTest(new Runnable() {
        @Override public void run() {
          assertEquals(State.STARTING, state());
          notifyStarted();
          assertEquals(State.RUNNING, state());
          hasConfirmedIsRunning.countDown();
        }
      });
    }

    @Override protected void doStop() {
      assertEquals(State.STOPPING, state());
      invokeOnExecutionThreadForTest(new Runnable() {
        @Override public void run() {
          assertEquals(State.STOPPING, state());
          notifyStopped();
          assertEquals(State.TERMINATED, state());
        }
      });
    }
  }

  private void invokeOnExecutionThreadForTest(Runnable runnable) {
    executionThread = new Thread(runnable);
    executionThread.setUncaughtExceptionHandler(new UncaughtExceptionHandler() {
      @Override
      public void uncaughtException(Thread thread, Throwable e) {
        thrownByExecutionThread = e;
      }
    });
    executionThread.start();
  }

  private static void throwIfSet(Throwable t) throws Throwable {
    if (t != null) {
      throw t;
    }
  }

  public void testStopUnstartedService() throws Exception {
    NoOpService service = new NoOpService();
    Future<Service.State> stopResult = service.stop();
    assertEquals(Service.State.TERMINATED, service.state());
    assertEquals(Service.State.TERMINATED, stopResult.get());

    Future<Service.State> startResult = service.start();
    assertEquals(Service.State.TERMINATED, service.state());
    assertEquals(Service.State.TERMINATED, startResult.get());
  }

  public void testThrowingServiceStartAndWait() throws Exception {
    StartThrowingService service = new StartThrowingService();

    try {
      service.startAndWait();
      fail();
    } catch (UncheckedExecutionException e) {
      assertEquals(EXCEPTION, e.getCause());
    }
  }

  public void testThrowingServiceStopAndWait_stopThrowing() throws Exception {
    StopThrowingService service = new StopThrowingService();

    service.startAndWait();
    try {
      service.stopAndWait();
      fail();
    } catch (UncheckedExecutionException e) {
      assertEquals(EXCEPTION, e.getCause());
    }
  }

  public void testThrowingServiceStopAndWait_runThrowing() throws Exception {
    RunThrowingService service = new RunThrowingService();

    service.startAndWait();
    try {
      service.stopAndWait();
      fail();
    } catch (UncheckedExecutionException e) {
      assertEquals(EXCEPTION, e.getCause().getCause());
    }
  }

  private static class StartThrowingService extends AbstractService {
    @Override protected void doStart() {
      notifyFailed(EXCEPTION);
    }

    @Override protected void doStop() {
      fail();
    }
  }

  private static class RunThrowingService extends AbstractService {
    @Override protected void doStart() {
      notifyStarted();
      notifyFailed(EXCEPTION);
    }

    @Override protected void doStop() {
      fail();
    }
  }

  private static class StopThrowingService extends AbstractService {
    @Override protected void doStart() {
      notifyStarted();
    }

    @Override protected void doStop() {
      notifyFailed(EXCEPTION);
    }
  }

  private static final Exception EXCEPTION = new Exception();
}
