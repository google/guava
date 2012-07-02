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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.Service.Listener;
import com.google.common.util.concurrent.Service.State;

import junit.framework.TestCase;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import javax.annotation.concurrent.GuardedBy;

/**
 * Unit test for {@link AbstractService}.
 *
 * @author Jesse Wilson
 */
public class AbstractServiceTest extends TestCase {

  private Thread executionThread;
  private Throwable thrownByExecutionThread;

  public void testNoOpServiceStartStop() throws Exception {
    NoOpService service = new NoOpService();
    RecordingListener listener = RecordingListener.record(service);

    assertEquals(State.NEW, service.state());
    assertFalse(service.isRunning());
    assertFalse(service.running);

    service.start();
    assertEquals(State.RUNNING, service.state());
    assertTrue(service.isRunning());
    assertTrue(service.running);

    service.stop();
    assertEquals(State.TERMINATED, service.state());
    assertFalse(service.isRunning());
    assertFalse(service.running);
    assertEquals(
        Arrays.asList(
            State.STARTING,
            State.RUNNING,
            State.STOPPING,
            State.TERMINATED),
        listener.getStateHistory());
  }

  public void testNoOpServiceStartAndWaitStopAndWait() throws Exception {
    NoOpService service = new NoOpService();

    service.start().get();
    assertEquals(State.RUNNING, service.state());

    service.stop().get();
    assertEquals(State.TERMINATED, service.state());
  }

  public void testNoOpServiceStartStopIdempotence() throws Exception {
    NoOpService service = new NoOpService();
    RecordingListener listener = RecordingListener.record(service);
    service.start();
    service.start();
    assertEquals(State.RUNNING, service.state());

    service.stop();
    service.stop();
    assertEquals(State.TERMINATED, service.state());
    assertEquals(
        Arrays.asList(
            State.STARTING,
            State.RUNNING,
            State.STOPPING,
            State.TERMINATED),
        listener.getStateHistory());
  }

  public void testNoOpServiceStartStopIdempotenceAfterWait() throws Exception {
    NoOpService service = new NoOpService();

    service.start().get();
    service.start();
    assertEquals(State.RUNNING, service.state());

    service.stop().get();
    service.stop();
    assertEquals(State.TERMINATED, service.state());
  }

  public void testNoOpServiceStartStopIdempotenceDoubleWait() throws Exception {
    NoOpService service = new NoOpService();

    service.start().get();
    service.start().get();
    assertEquals(State.RUNNING, service.state());

    service.stop().get();
    service.stop().get();
    assertEquals(State.TERMINATED, service.state());
  }

  public void testNoOpServiceStartStopAndWaitUninterruptible()
      throws Exception {
    NoOpService service = new NoOpService();

    currentThread().interrupt();
    try {
      service.startAndWait();
      assertEquals(State.RUNNING, service.state());

      service.stopAndWait();
      assertEquals(State.TERMINATED, service.state());

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

  public void testManualServiceStartStop() throws Exception {
    ManualSwitchedService service = new ManualSwitchedService();
    RecordingListener listener = RecordingListener.record(service);

    service.start();
    assertEquals(State.STARTING, service.state());
    assertFalse(service.isRunning());
    assertTrue(service.doStartCalled);

    service.notifyStarted(); // usually this would be invoked by another thread
    assertEquals(State.RUNNING, service.state());
    assertTrue(service.isRunning());

    service.stop();
    assertEquals(State.STOPPING, service.state());
    assertFalse(service.isRunning());
    assertTrue(service.doStopCalled);

    service.notifyStopped(); // usually this would be invoked by another thread
    assertEquals(State.TERMINATED, service.state());
    assertFalse(service.isRunning());
    assertEquals(
        Arrays.asList(
            State.STARTING,
            State.RUNNING,
            State.STOPPING,
            State.TERMINATED),
            listener.getStateHistory());

  }

  public void testManualServiceNotifyStoppedWhileRunning() throws Exception {
    ManualSwitchedService service = new ManualSwitchedService();
    RecordingListener listener = RecordingListener.record(service);

    service.start();
    service.notifyStarted();
    service.notifyStopped();
    assertEquals(State.TERMINATED, service.state());
    assertFalse(service.isRunning());
    assertFalse(service.doStopCalled);

    assertEquals(
        Arrays.asList(
            State.STARTING,
            State.RUNNING,
            State.TERMINATED),
            listener.getStateHistory());
  }

  public void testManualServiceStopWhileStarting() throws Exception {
    ManualSwitchedService service = new ManualSwitchedService();
    RecordingListener listener = RecordingListener.record(service);

    service.start();
    assertEquals(State.STARTING, service.state());
    assertFalse(service.isRunning());
    assertTrue(service.doStartCalled);

    service.stop();
    assertEquals(State.STOPPING, service.state());
    assertFalse(service.isRunning());
    assertFalse(service.doStopCalled);

    service.notifyStarted();
    assertEquals(State.STOPPING, service.state());
    assertFalse(service.isRunning());
    assertTrue(service.doStopCalled);

    service.notifyStopped();
    assertEquals(State.TERMINATED, service.state());
    assertFalse(service.isRunning());
    assertEquals(
        Arrays.asList(
            State.STARTING,
            State.STOPPING,
            State.TERMINATED),
            listener.getStateHistory());
  }

  public void testManualServiceStopWhileNew() throws Exception {
    ManualSwitchedService service = new ManualSwitchedService();
    RecordingListener listener = RecordingListener.record(service);

    service.stop();
    assertEquals(State.TERMINATED, service.state());
    assertFalse(service.isRunning());
    assertFalse(service.doStartCalled);
    assertFalse(service.doStopCalled);
    assertEquals(Arrays.asList(State.TERMINATED), listener.getStateHistory());
  }

  public void testManualServiceFailWhileStarting() throws Exception {
    ManualSwitchedService service = new ManualSwitchedService();
    RecordingListener listener = RecordingListener.record(service);
    service.start();
    service.notifyFailed(EXCEPTION);
    assertEquals(Arrays.asList(State.STARTING, State.FAILED), listener.getStateHistory());
  }

  public void testManualServiceFailWhileRunning() throws Exception {
    ManualSwitchedService service = new ManualSwitchedService();
    RecordingListener listener = RecordingListener.record(service);
    service.start();
    service.notifyStarted();
    service.notifyFailed(EXCEPTION);
    assertEquals(Arrays.asList(State.STARTING, State.RUNNING, State.FAILED),
        listener.getStateHistory());
  }

  public void testManualServiceFailWhileStopping() throws Exception {
    ManualSwitchedService service = new ManualSwitchedService();
    RecordingListener listener = RecordingListener.record(service);
    service.start();
    service.notifyStarted();
    service.stop();
    service.notifyFailed(EXCEPTION);
    assertEquals(Arrays.asList(State.STARTING, State.RUNNING, State.STOPPING, State.FAILED),
        listener.getStateHistory());
  }

  public void testManualServiceUnrequestedStop() {
    ManualSwitchedService service = new ManualSwitchedService();

    service.start();

    service.notifyStarted();
    assertEquals(State.RUNNING, service.state());
    assertTrue(service.isRunning());
    assertFalse(service.doStopCalled);

    service.notifyStopped();
    assertEquals(State.TERMINATED, service.state());
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
    RecordingListener listener = RecordingListener.record(service);
    service.start().get();
    assertEquals(State.RUNNING, service.state());

    service.awaitRunChecks();

    service.stop().get();
    assertEquals(State.TERMINATED, service.state());

    throwIfSet(thrownByExecutionThread);
    assertEquals(
        Arrays.asList(
            State.STARTING,
            State.RUNNING,
            State.STOPPING,
            State.TERMINATED),
            listener.getStateHistory());
  }

  public void testThreadedServiceStartStopIdempotence() throws Throwable {
    ThreadedService service = new ThreadedService();

    service.start();
    service.start().get();
    assertEquals(State.RUNNING, service.state());

    service.awaitRunChecks();

    service.stop();
    service.stop().get();
    assertEquals(State.TERMINATED, service.state());

    throwIfSet(thrownByExecutionThread);
  }

  public void testThreadedServiceStartStopIdempotenceAfterWait()
      throws Throwable {
    ThreadedService service = new ThreadedService();

    service.start().get();
    service.start();
    assertEquals(State.RUNNING, service.state());

    service.awaitRunChecks();

    service.stop().get();
    service.stop();
    assertEquals(State.TERMINATED, service.state());

    executionThread.join();

    throwIfSet(thrownByExecutionThread);
  }

  public void testThreadedServiceStartStopIdempotenceDoubleWait()
      throws Throwable {
    ThreadedService service = new ThreadedService();

    service.start().get();
    service.start().get();
    assertEquals(State.RUNNING, service.state());

    service.awaitRunChecks();

    service.stop().get();
    service.stop().get();
    assertEquals(State.TERMINATED, service.state());

    throwIfSet(thrownByExecutionThread);
  }

  public void testManualServiceFailureIdempotence() {
    ManualSwitchedService service = new ManualSwitchedService();
    RecordingListener listener = RecordingListener.record(service);
    service.start();
    service.notifyFailed(new Exception("1"));
    service.notifyFailed(new Exception("2"));
    try {
      service.startAndWait();
    } catch (UncheckedExecutionException e) {
      assertEquals("1", e.getCause().getMessage());
    }
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
    RecordingListener listener = RecordingListener.record(service);

    Future<State> stopResult = service.stop();
    assertEquals(State.TERMINATED, service.state());
    assertEquals(State.TERMINATED, stopResult.get());

    Future<State> startResult = service.start();
    assertEquals(State.TERMINATED, service.state());
    assertEquals(State.TERMINATED, startResult.get());
    assertEquals(State.TERMINATED, Iterables.getOnlyElement(listener.getStateHistory()));
  }

  public void testThrowingServiceStartAndWait() throws Exception {
    StartThrowingService service = new StartThrowingService();
    RecordingListener listener = RecordingListener.record(service);

    try {
      service.startAndWait();
      fail();
    } catch (UncheckedExecutionException e) {
      assertEquals(EXCEPTION, e.getCause());
    }
    assertEquals(
        Arrays.asList(
            State.STARTING,
            State.FAILED),
        listener.getStateHistory());
  }

  public void testThrowingServiceStopAndWait_stopThrowing() throws Exception {
    StopThrowingService service = new StopThrowingService();
    RecordingListener listener = RecordingListener.record(service);

    service.startAndWait();
    try {
      service.stopAndWait();
      fail();
    } catch (UncheckedExecutionException e) {
      assertEquals(EXCEPTION, e.getCause());
    }
    assertEquals(
        Arrays.asList(
            State.STARTING,
            State.RUNNING,
            State.STOPPING,
            State.FAILED),
        listener.getStateHistory());
  }

  public void testThrowingServiceStopAndWait_runThrowing() throws Exception {
    RunThrowingService service = new RunThrowingService();
    RecordingListener listener = RecordingListener.record(service);

    service.startAndWait();
    try {
      service.stopAndWait();
      fail();
    } catch (UncheckedExecutionException e) {
      assertEquals(EXCEPTION, e.getCause().getCause());
    }
    assertEquals(
        Arrays.asList(
            State.STARTING,
            State.RUNNING,
            State.FAILED),
        listener.getStateHistory());
  }

  public void testAddListenerAfterFailureDoesntCauseDeadlock() throws InterruptedException {
    final StartThrowingService service = new StartThrowingService();
    service.start();
    assertEquals(State.FAILED, service.state());
    service.addListener(new RecordingListener(service), MoreExecutors.sameThreadExecutor());
    Thread thread = new Thread() {
      @Override public void run() {
        // Internally start() grabs a lock, this could be any such method on AbstractService.
        service.start();
      }
    };
    thread.start();
    thread.join(100);
    assertFalse(thread + " is deadlocked", thread.isAlive());
  }

  public void testListenerDoesntDeadlockOnStartAndWaitFromRunning() throws Exception {
    final NoOpThreadedService service = new NoOpThreadedService();
    service.addListener(new Listener() {
      @Override public void starting() { }
      @Override public void running() {
        service.startAndWait();
      }
      @Override public void stopping(State from) { }
      @Override public void terminated(State from) { }
      @Override public void failed(State from, Throwable failure) { }
    }, MoreExecutors.sameThreadExecutor());
    service.start().get(10, TimeUnit.MILLISECONDS);
    service.stop();
  }

  public void testListenerDoesntDeadlockOnStopAndWaitFromTerminated() throws Exception {
    final NoOpThreadedService service = new NoOpThreadedService();
    service.addListener(new Listener() {
      @Override public void starting() { }
      @Override public void running() { }
      @Override public void stopping(State from) { }
      @Override public void terminated(State from) {
        service.stopAndWait();
      }
      @Override public void failed(State from, Throwable failure) { }
    }, MoreExecutors.sameThreadExecutor());
    service.startAndWait();

    Thread thread = new Thread() {
      @Override public void run() {
        service.stopAndWait();
      }
    };
    thread.start();
    thread.join(100);
    assertFalse(thread + " is deadlocked", thread.isAlive());
  }

  private static class NoOpThreadedService extends AbstractExecutionThreadService {
    @Override protected void run() throws Exception {}
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

  private static class RecordingListener implements Listener {
    static RecordingListener record(Service service) {
      RecordingListener listener = new RecordingListener(service);
      service.addListener(listener, MoreExecutors.sameThreadExecutor());
      return listener;
    }

    final Service service;

    RecordingListener(Service service) {
      this.service = service;
    }

    @GuardedBy("this")
    final List<State> stateHistory = Lists.newArrayList();
    final CountDownLatch completionLatch = new CountDownLatch(1);

    synchronized ImmutableList<State> getStateHistory() throws Exception {
      completionLatch.await();
      return ImmutableList.copyOf(stateHistory);
    }

    @Override public synchronized void starting() {
      assertTrue(stateHistory.isEmpty());
      assertNotSame(State.NEW, service.state());
      stateHistory.add(State.STARTING);
    }

    @Override public synchronized void running() {
      assertEquals(State.STARTING, Iterables.getOnlyElement(stateHistory));
      stateHistory.add(State.RUNNING);
      assertTrue(service.start().isDone());
      assertEquals(State.RUNNING, service.startAndWait());
      assertNotSame(State.STARTING, service.state());
    }

    @Override public synchronized void stopping(State from) {
      assertEquals(from, Iterables.getLast(stateHistory));
      stateHistory.add(State.STOPPING);
      if (from == State.STARTING) {
        assertTrue(service.start().isDone());
        assertEquals(State.STOPPING, service.startAndWait());
      }
      assertNotSame(from, service.state());
    }

    @Override public synchronized void terminated(State from) {
      assertEquals(from, Iterables.getLast(stateHistory, State.NEW));
      stateHistory.add(State.TERMINATED);
      assertEquals(State.TERMINATED, service.state());
      assertTrue(service.start().isDone());
      if (from == State.NEW) {
        assertEquals(State.TERMINATED, service.startAndWait());
      }
      assertTrue(service.stop().isDone());
      assertEquals(State.TERMINATED, service.stopAndWait());
      completionLatch.countDown();
    }

    @Override public synchronized void failed(State from, Throwable failure) {
      assertEquals(from, Iterables.getLast(stateHistory));
      stateHistory.add(State.FAILED);
      assertEquals(State.FAILED, service.state());
      if (from == State.STARTING) {
        try {
          service.startAndWait();
        } catch (UncheckedExecutionException e) {
          assertEquals(failure, e.getCause());
        }
      }
      try {
        service.stopAndWait();
      } catch (UncheckedExecutionException e) {
        if (from == State.STOPPING) {
          assertEquals(failure, e.getCause());
        } else {
          assertEquals(failure, e.getCause().getCause());
        }
      }
      completionLatch.countDown();
    }
  }

  private static final Exception EXCEPTION = new Exception();
}
