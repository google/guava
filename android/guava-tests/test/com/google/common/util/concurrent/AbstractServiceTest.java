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
import static java.lang.Thread.currentThread;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.assertThrows;

import com.google.common.annotations.GwtIncompatible;
import com.google.common.annotations.J2ktIncompatible;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.util.concurrent.Service.Listener;
import com.google.common.util.concurrent.Service.State;
import com.google.errorprone.annotations.concurrent.GuardedBy;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import java.util.concurrent.atomic.AtomicInteger;
import junit.framework.TestCase;
import org.jspecify.annotations.NullUnmarked;

/**
 * Unit test for {@link AbstractService}.
 *
 * @author Jesse Wilson
 */
@NullUnmarked
@GwtIncompatible
@J2ktIncompatible
public class AbstractServiceTest extends TestCase {

  private static final long LONG_TIMEOUT_MILLIS = 10000;
  private Thread executionThread;
  private Throwable thrownByExecutionThread;

  public void testNoOpServiceStartStop() throws Exception {
    NoOpService service = new NoOpService();
    RecordingListener listener = RecordingListener.record(service);

    assertThat(service.state()).isEqualTo(State.NEW);
    assertFalse(service.isRunning());
    assertFalse(service.running);

    service.startAsync();
    assertThat(service.state()).isEqualTo(State.RUNNING);
    assertTrue(service.isRunning());
    assertTrue(service.running);

    service.stopAsync();
    assertThat(service.state()).isEqualTo(State.TERMINATED);
    assertFalse(service.isRunning());
    assertFalse(service.running);
    assertEquals(
        ImmutableList.of(State.STARTING, State.RUNNING, State.STOPPING, State.TERMINATED),
        listener.getStateHistory());
  }

  public void testNoOpServiceStartAndWaitStopAndWait() {
    NoOpService service = new NoOpService();

    service.startAsync().awaitRunning();
    assertThat(service.state()).isEqualTo(State.RUNNING);

    service.stopAsync().awaitTerminated();
    assertThat(service.state()).isEqualTo(State.TERMINATED);
  }

  public void testNoOpServiceStartAsyncAndAwaitStopAsyncAndAwait() {
    NoOpService service = new NoOpService();

    service.startAsync().awaitRunning();
    assertThat(service.state()).isEqualTo(State.RUNNING);

    service.stopAsync().awaitTerminated();
    assertThat(service.state()).isEqualTo(State.TERMINATED);
  }

  public void testNoOpServiceStopIdempotence() throws Exception {
    NoOpService service = new NoOpService();
    RecordingListener listener = RecordingListener.record(service);
    service.startAsync().awaitRunning();
    assertThat(service.state()).isEqualTo(State.RUNNING);

    service.stopAsync();
    service.stopAsync();
    assertThat(service.state()).isEqualTo(State.TERMINATED);
    assertEquals(
        ImmutableList.of(State.STARTING, State.RUNNING, State.STOPPING, State.TERMINATED),
        listener.getStateHistory());
  }

  public void testNoOpServiceStopIdempotenceAfterWait() {
    NoOpService service = new NoOpService();

    service.startAsync().awaitRunning();

    service.stopAsync().awaitTerminated();
    service.stopAsync();
    assertThat(service.state()).isEqualTo(State.TERMINATED);
  }

  public void testNoOpServiceStopIdempotenceDoubleWait() {
    NoOpService service = new NoOpService();

    service.startAsync().awaitRunning();
    assertThat(service.state()).isEqualTo(State.RUNNING);

    service.stopAsync().awaitTerminated();
    service.stopAsync().awaitTerminated();
    assertThat(service.state()).isEqualTo(State.TERMINATED);
  }

  public void testNoOpServiceStartStopAndWaitUninterruptible() {
    NoOpService service = new NoOpService();

    currentThread().interrupt();
    try {
      service.startAsync().awaitRunning();
      assertThat(service.state()).isEqualTo(State.RUNNING);

      service.stopAsync().awaitTerminated();
      assertThat(service.state()).isEqualTo(State.TERMINATED);

      assertTrue(currentThread().isInterrupted());
    } finally {
      Thread.interrupted(); // clear interrupt for future tests
    }
  }

  private static class NoOpService extends AbstractService {
    boolean running = false;

    @Override
    protected void doStart() {
      assertFalse(running);
      running = true;
      notifyStarted();
    }

    @Override
    protected void doStop() {
      assertTrue(running);
      running = false;
      notifyStopped();
    }
  }

  public void testManualServiceStartStop() throws Exception {
    ManualSwitchedService service = new ManualSwitchedService();
    RecordingListener listener = RecordingListener.record(service);

    service.startAsync();
    assertThat(service.state()).isEqualTo(State.STARTING);
    assertFalse(service.isRunning());
    assertTrue(service.doStartCalled);

    service.notifyStarted(); // usually this would be invoked by another thread
    assertThat(service.state()).isEqualTo(State.RUNNING);
    assertTrue(service.isRunning());

    service.stopAsync();
    assertThat(service.state()).isEqualTo(State.STOPPING);
    assertFalse(service.isRunning());
    assertTrue(service.doStopCalled);

    service.notifyStopped(); // usually this would be invoked by another thread
    assertThat(service.state()).isEqualTo(State.TERMINATED);
    assertFalse(service.isRunning());
    assertEquals(
        ImmutableList.of(State.STARTING, State.RUNNING, State.STOPPING, State.TERMINATED),
        listener.getStateHistory());
  }

  public void testManualServiceNotifyStoppedWhileRunning() throws Exception {
    ManualSwitchedService service = new ManualSwitchedService();
    RecordingListener listener = RecordingListener.record(service);

    service.startAsync();
    service.notifyStarted();
    service.notifyStopped();
    assertThat(service.state()).isEqualTo(State.TERMINATED);
    assertFalse(service.isRunning());
    assertFalse(service.doStopCalled);

    assertEquals(
        ImmutableList.of(State.STARTING, State.RUNNING, State.TERMINATED),
        listener.getStateHistory());
  }

  public void testManualServiceStopWhileStarting() throws Exception {
    ManualSwitchedService service = new ManualSwitchedService();
    RecordingListener listener = RecordingListener.record(service);

    service.startAsync();
    assertThat(service.state()).isEqualTo(State.STARTING);
    assertFalse(service.isRunning());
    assertTrue(service.doStartCalled);

    service.stopAsync();
    assertThat(service.state()).isEqualTo(State.STOPPING);
    assertFalse(service.isRunning());
    assertFalse(service.doStopCalled);

    service.notifyStarted();
    assertThat(service.state()).isEqualTo(State.STOPPING);
    assertFalse(service.isRunning());
    assertTrue(service.doStopCalled);

    service.notifyStopped();
    assertThat(service.state()).isEqualTo(State.TERMINATED);
    assertFalse(service.isRunning());
    assertEquals(
        ImmutableList.of(State.STARTING, State.STOPPING, State.TERMINATED),
        listener.getStateHistory());
  }

  /**
   * This tests for a bug where if {@link Service#stopAsync()} was called while the service was
   * {@link State#STARTING} more than once, the {@link Listener#stopping(State)} callback would get
   * called multiple times.
   */
  public void testManualServiceStopMultipleTimesWhileStarting() {
    ManualSwitchedService service = new ManualSwitchedService();
    AtomicInteger stoppingCount = new AtomicInteger();
    service.addListener(
        new Listener() {
          @Override
          public void stopping(State from) {
            stoppingCount.incrementAndGet();
          }
        },
        directExecutor());

    service.startAsync();
    service.stopAsync();
    assertEquals(1, stoppingCount.get());
    service.stopAsync();
    assertEquals(1, stoppingCount.get());
  }

  public void testManualServiceStopWhileNew() throws Exception {
    ManualSwitchedService service = new ManualSwitchedService();
    RecordingListener listener = RecordingListener.record(service);

    service.stopAsync();
    assertThat(service.state()).isEqualTo(State.TERMINATED);
    assertFalse(service.isRunning());
    assertFalse(service.doStartCalled);
    assertFalse(service.doStopCalled);
    assertEquals(ImmutableList.of(State.TERMINATED), listener.getStateHistory());
  }

  public void testManualServiceFailWhileStarting() throws Exception {
    ManualSwitchedService service = new ManualSwitchedService();
    RecordingListener listener = RecordingListener.record(service);
    service.startAsync();
    service.notifyFailed(EXCEPTION);
    assertEquals(ImmutableList.of(State.STARTING, State.FAILED), listener.getStateHistory());
  }

  public void testManualServiceFailWhileRunning() throws Exception {
    ManualSwitchedService service = new ManualSwitchedService();
    RecordingListener listener = RecordingListener.record(service);
    service.startAsync();
    service.notifyStarted();
    service.notifyFailed(EXCEPTION);
    assertEquals(
        ImmutableList.of(State.STARTING, State.RUNNING, State.FAILED), listener.getStateHistory());
  }

  public void testManualServiceFailWhileStopping() throws Exception {
    ManualSwitchedService service = new ManualSwitchedService();
    RecordingListener listener = RecordingListener.record(service);
    service.startAsync();
    service.notifyStarted();
    service.stopAsync();
    service.notifyFailed(EXCEPTION);
    assertEquals(
        ImmutableList.of(State.STARTING, State.RUNNING, State.STOPPING, State.FAILED),
        listener.getStateHistory());
  }

  public void testManualServiceUnrequestedStop() {
    ManualSwitchedService service = new ManualSwitchedService();

    service.startAsync();

    service.notifyStarted();
    assertThat(service.state()).isEqualTo(State.RUNNING);
    assertTrue(service.isRunning());
    assertFalse(service.doStopCalled);

    service.notifyStopped();
    assertThat(service.state()).isEqualTo(State.TERMINATED);
    assertFalse(service.isRunning());
    assertFalse(service.doStopCalled);
  }

  /**
   * The user of this service should call {@link #notifyStarted} and {@link #notifyStopped} after
   * calling {@link #startAsync} and {@link #stopAsync}.
   */
  private static class ManualSwitchedService extends AbstractService {
    boolean doStartCalled = false;
    boolean doStopCalled = false;

    @Override
    protected void doStart() {
      assertFalse(doStartCalled);
      doStartCalled = true;
    }

    @Override
    protected void doStop() {
      assertFalse(doStopCalled);
      doStopCalled = true;
    }
  }

  public void testAwaitTerminated() throws Exception {
    NoOpService service = new NoOpService();
    Thread waiter =
        new Thread() {
          @Override
          public void run() {
            service.awaitTerminated();
          }
        };
    waiter.start();
    service.startAsync().awaitRunning();
    assertThat(service.state()).isEqualTo(State.RUNNING);
    service.stopAsync();
    waiter.join(LONG_TIMEOUT_MILLIS); // ensure that the await in the other thread is triggered
    assertFalse(waiter.isAlive());
  }

  public void testAwaitTerminated_failedService() throws Exception {
    ManualSwitchedService service = new ManualSwitchedService();
    FutureTask<IllegalStateException> waiter =
        new FutureTask<>(() -> assertThrows(IllegalStateException.class, service::awaitTerminated));
    new Thread(waiter).start();
    service.startAsync();
    service.notifyStarted();
    assertThat(service.state()).isEqualTo(State.RUNNING);
    service.notifyFailed(EXCEPTION);
    assertThat(service.state()).isEqualTo(State.FAILED);
    assertThat(waiter.get()).hasCauseThat().isEqualTo(EXCEPTION);
  }

  public void testThreadedServiceStartAndWaitStopAndWait() throws Throwable {
    ThreadedService service = new ThreadedService();
    RecordingListener listener = RecordingListener.record(service);
    service.startAsync().awaitRunning();
    assertThat(service.state()).isEqualTo(State.RUNNING);

    service.awaitRunChecks();

    service.stopAsync().awaitTerminated();
    assertThat(service.state()).isEqualTo(State.TERMINATED);

    throwIfSet(thrownByExecutionThread);
    assertEquals(
        ImmutableList.of(State.STARTING, State.RUNNING, State.STOPPING, State.TERMINATED),
        listener.getStateHistory());
  }

  public void testThreadedServiceStopIdempotence() throws Throwable {
    ThreadedService service = new ThreadedService();

    service.startAsync().awaitRunning();
    assertThat(service.state()).isEqualTo(State.RUNNING);

    service.awaitRunChecks();

    service.stopAsync();
    service.stopAsync().awaitTerminated();
    assertThat(service.state()).isEqualTo(State.TERMINATED);

    throwIfSet(thrownByExecutionThread);
  }

  public void testThreadedServiceStopIdempotenceAfterWait() throws Throwable {
    ThreadedService service = new ThreadedService();

    service.startAsync().awaitRunning();
    assertThat(service.state()).isEqualTo(State.RUNNING);

    service.awaitRunChecks();

    service.stopAsync().awaitTerminated();
    service.stopAsync();
    assertThat(service.state()).isEqualTo(State.TERMINATED);

    executionThread.join();

    throwIfSet(thrownByExecutionThread);
  }

  public void testThreadedServiceStopIdempotenceDoubleWait() throws Throwable {
    ThreadedService service = new ThreadedService();

    service.startAsync().awaitRunning();
    assertThat(service.state()).isEqualTo(State.RUNNING);

    service.awaitRunChecks();

    service.stopAsync().awaitTerminated();
    service.stopAsync().awaitTerminated();
    assertThat(service.state()).isEqualTo(State.TERMINATED);

    throwIfSet(thrownByExecutionThread);
  }

  public void testManualServiceFailureIdempotence() {
    ManualSwitchedService service = new ManualSwitchedService();
    /*
     * Set up a RecordingListener to perform its built-in assertions, even though we won't look at
     * its state history.
     */
    RecordingListener unused = RecordingListener.record(service);
    service.startAsync();
    service.notifyFailed(new Exception("1"));
    service.notifyFailed(new Exception("2"));
    assertThat(service.failureCause()).hasMessageThat().isEqualTo("1");
    IllegalStateException e = assertThrows(IllegalStateException.class, service::awaitRunning);
    assertThat(e).hasCauseThat().hasMessageThat().isEqualTo("1");
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
      assertTrue(
          "Service thread hasn't finished its checks. "
              + "Exception status (possibly stale): "
              + thrownByExecutionThread,
          hasConfirmedIsRunning.await(10, SECONDS));
    }

    @Override
    protected void doStart() {
      assertThat(state()).isEqualTo(State.STARTING);
      invokeOnExecutionThreadForTest(
          () -> {
            assertThat(state()).isEqualTo(State.STARTING);
            notifyStarted();
            assertThat(state()).isEqualTo(State.RUNNING);
            hasConfirmedIsRunning.countDown();
          });
    }

    @Override
    protected void doStop() {
      assertThat(state()).isEqualTo(State.STOPPING);
      invokeOnExecutionThreadForTest(
          () -> {
            assertThat(state()).isEqualTo(State.STOPPING);
            notifyStopped();
            assertThat(state()).isEqualTo(State.TERMINATED);
          });
    }
  }

  private void invokeOnExecutionThreadForTest(Runnable runnable) {
    executionThread = new Thread(runnable);
    executionThread.setUncaughtExceptionHandler(
        (thread, throwable) -> thrownByExecutionThread = throwable);
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

    service.stopAsync();
    assertThat(service.state()).isEqualTo(State.TERMINATED);

    assertThrows(IllegalStateException.class, service::startAsync);
    assertThat(listener.getStateHistory()).containsExactly(State.TERMINATED);
  }

  public void testFailingServiceStartAndWait() throws Exception {
    StartFailingService service = new StartFailingService();
    RecordingListener listener = RecordingListener.record(service);

    IllegalStateException e =
        assertThrows(IllegalStateException.class, () -> service.startAsync().awaitRunning());
    assertEquals(EXCEPTION, service.failureCause());
    assertThat(e).hasCauseThat().isEqualTo(EXCEPTION);
    assertEquals(ImmutableList.of(State.STARTING, State.FAILED), listener.getStateHistory());
  }

  public void testFailingServiceStopAndWait_stopFailing() throws Exception {
    StopFailingService service = new StopFailingService();
    RecordingListener listener = RecordingListener.record(service);

    service.startAsync().awaitRunning();
    IllegalStateException e =
        assertThrows(IllegalStateException.class, () -> service.stopAsync().awaitTerminated());
    assertEquals(EXCEPTION, service.failureCause());
    assertThat(e).hasCauseThat().isEqualTo(EXCEPTION);
    assertEquals(
        ImmutableList.of(State.STARTING, State.RUNNING, State.STOPPING, State.FAILED),
        listener.getStateHistory());
  }

  public void testFailingServiceStopAndWait_runFailing() throws Exception {
    RunFailingService service = new RunFailingService();
    RecordingListener listener = RecordingListener.record(service);

    service.startAsync();
    IllegalStateException e = assertThrows(IllegalStateException.class, service::awaitRunning);
    assertEquals(EXCEPTION, service.failureCause());
    assertThat(e).hasCauseThat().isEqualTo(EXCEPTION);
    assertEquals(
        ImmutableList.of(State.STARTING, State.RUNNING, State.FAILED), listener.getStateHistory());
  }

  public void testThrowingServiceStartAndWait() throws Exception {
    StartThrowingService service = new StartThrowingService();
    RecordingListener listener = RecordingListener.record(service);

    IllegalStateException e =
        assertThrows(IllegalStateException.class, () -> service.startAsync().awaitRunning());
    assertEquals(service.exception, service.failureCause());
    assertThat(e).hasCauseThat().isEqualTo(service.exception);
    assertEquals(ImmutableList.of(State.STARTING, State.FAILED), listener.getStateHistory());
  }

  public void testThrowingServiceStopAndWait_stopThrowing() throws Exception {
    StopThrowingService service = new StopThrowingService();
    RecordingListener listener = RecordingListener.record(service);

    service.startAsync().awaitRunning();
    IllegalStateException e =
        assertThrows(IllegalStateException.class, () -> service.stopAsync().awaitTerminated());
    assertEquals(service.exception, service.failureCause());
    assertThat(e).hasCauseThat().isEqualTo(service.exception);
    assertEquals(
        ImmutableList.of(State.STARTING, State.RUNNING, State.STOPPING, State.FAILED),
        listener.getStateHistory());
  }

  public void testThrowingServiceStopAndWait_runThrowing() throws Exception {
    RunThrowingService service = new RunThrowingService();
    RecordingListener listener = RecordingListener.record(service);

    service.startAsync();
    IllegalStateException e = assertThrows(IllegalStateException.class, service::awaitTerminated);
    assertEquals(service.exception, service.failureCause());
    assertThat(e).hasCauseThat().isEqualTo(service.exception);
    assertEquals(
        ImmutableList.of(State.STARTING, State.RUNNING, State.FAILED), listener.getStateHistory());
  }

  public void testFailureCause_throwsIfNotFailed() {
    StopFailingService service = new StopFailingService();
    assertThrows(IllegalStateException.class, service::failureCause);
    service.startAsync().awaitRunning();
    assertThrows(IllegalStateException.class, service::failureCause);
    IllegalStateException e =
        assertThrows(IllegalStateException.class, () -> service.stopAsync().awaitTerminated());
    assertEquals(EXCEPTION, service.failureCause());
    assertThat(e).hasCauseThat().isEqualTo(EXCEPTION);
  }

  public void testAddListenerAfterFailureDoesntCauseDeadlock() throws InterruptedException {
    StartFailingService service = new StartFailingService();
    service.startAsync();
    assertThat(service.state()).isEqualTo(State.FAILED);
    service.addListener(new RecordingListener(service), directExecutor());
    Thread thread =
        new Thread() {
          @Override
          public void run() {
            // Internally stopAsync() grabs a lock, this could be any such method on
            // AbstractService.
            service.stopAsync();
          }
        };
    thread.start();
    thread.join(LONG_TIMEOUT_MILLIS);
    assertFalse(thread + " is deadlocked", thread.isAlive());
  }

  public void testListenerDoesntDeadlockOnStartAndWaitFromRunning() throws Exception {
    NoOpThreadedService service = new NoOpThreadedService();
    service.addListener(
        new Listener() {
          @Override
          public void running() {
            service.awaitRunning();
          }
        },
        directExecutor());
    service.startAsync().awaitRunning(LONG_TIMEOUT_MILLIS, MILLISECONDS);
    service.stopAsync();
  }

  public void testListenerDoesntDeadlockOnStopAndWaitFromTerminated() throws Exception {
    NoOpThreadedService service = new NoOpThreadedService();
    service.addListener(
        new Listener() {
          @Override
          public void terminated(State from) {
            service.stopAsync().awaitTerminated();
          }
        },
        directExecutor());
    service.startAsync().awaitRunning();

    Thread thread =
        new Thread() {
          @Override
          public void run() {
            service.stopAsync().awaitTerminated();
          }
        };
    thread.start();
    thread.join(LONG_TIMEOUT_MILLIS);
    assertFalse(thread + " is deadlocked", thread.isAlive());
  }

  private static class NoOpThreadedService extends AbstractExecutionThreadService {
    final CountDownLatch latch = new CountDownLatch(1);

    @Override
    protected void run() throws Exception {
      latch.await();
    }

    @Override
    protected void triggerShutdown() {
      latch.countDown();
    }
  }

  private static class StartFailingService extends AbstractService {
    @Override
    protected void doStart() {
      notifyFailed(EXCEPTION);
    }

    @Override
    protected void doStop() {
      fail();
    }
  }

  private static class RunFailingService extends AbstractService {
    @Override
    protected void doStart() {
      notifyStarted();
      notifyFailed(EXCEPTION);
    }

    @Override
    protected void doStop() {
      fail();
    }
  }

  private static class StopFailingService extends AbstractService {
    @Override
    protected void doStart() {
      notifyStarted();
    }

    @Override
    protected void doStop() {
      notifyFailed(EXCEPTION);
    }
  }

  private static class StartThrowingService extends AbstractService {

    final RuntimeException exception = new RuntimeException("deliberate");

    @Override
    protected void doStart() {
      throw exception;
    }

    @Override
    protected void doStop() {
      fail();
    }
  }

  private static class RunThrowingService extends AbstractService {

    final RuntimeException exception = new RuntimeException("deliberate");

    @Override
    protected void doStart() {
      notifyStarted();
      throw exception;
    }

    @Override
    protected void doStop() {
      fail();
    }
  }

  private static class StopThrowingService extends AbstractService {

    final RuntimeException exception = new RuntimeException("deliberate");

    @Override
    protected void doStart() {
      notifyStarted();
    }

    @Override
    protected void doStop() {
      throw exception;
    }
  }

  private static class RecordingListener extends Listener {
    static RecordingListener record(Service service) {
      RecordingListener listener = new RecordingListener(service);
      service.addListener(listener, directExecutor());
      return listener;
    }

    final Service service;

    RecordingListener(Service service) {
      this.service = service;
    }

    @GuardedBy("this")
    final List<State> stateHistory = new ArrayList<>();

    final CountDownLatch completionLatch = new CountDownLatch(1);

    ImmutableList<State> getStateHistory() throws Exception {
      completionLatch.await();
      synchronized (this) {
        return ImmutableList.copyOf(stateHistory);
      }
    }

    @Override
    public synchronized void starting() {
      assertTrue(stateHistory.isEmpty());
      assertThat(service.state()).isNotEqualTo(State.NEW);
      stateHistory.add(State.STARTING);
    }

    @Override
    public synchronized void running() {
      assertThat(stateHistory).containsExactly(State.STARTING);
      stateHistory.add(State.RUNNING);
      service.awaitRunning();
      assertThat(service.state()).isNotEqualTo(State.STARTING);
    }

    @Override
    public synchronized void stopping(State from) {
      assertThat(Iterables.getLast(stateHistory)).isEqualTo(from);
      stateHistory.add(State.STOPPING);
      if (from == State.STARTING) {
        IllegalStateException expected =
            assertThrows(IllegalStateException.class, () -> service.awaitRunning());
        assertThat(expected).hasCauseThat().isNull();
        assertThat(expected)
            .hasMessageThat()
            .isEqualTo("Expected the service " + service + " to be RUNNING, but was STOPPING");
      }
      assertThat(service.state()).isNotEqualTo(from);
    }

    @Override
    public synchronized void terminated(State from) {
      assertThat(Iterables.getLast(stateHistory, State.NEW)).isEqualTo(from);
      stateHistory.add(State.TERMINATED);
      assertThat(service.state()).isEqualTo(State.TERMINATED);
      if (from == State.NEW) {
        IllegalStateException expected =
            assertThrows(IllegalStateException.class, () -> service.awaitRunning());
        assertThat(expected).hasCauseThat().isNull();
        assertThat(expected)
            .hasMessageThat()
            .isEqualTo("Expected the service " + service + " to be RUNNING, but was TERMINATED");
      }
      completionLatch.countDown();
    }

    @Override
    public synchronized void failed(State from, Throwable failure) {
      assertThat(Iterables.getLast(stateHistory)).isEqualTo(from);
      stateHistory.add(State.FAILED);
      assertThat(service.state()).isEqualTo(State.FAILED);
      assertEquals(failure, service.failureCause());
      if (from == State.STARTING) {
        IllegalStateException e =
            assertThrows(IllegalStateException.class, () -> service.awaitRunning());
        assertThat(e).hasCauseThat().isEqualTo(failure);
      }
      IllegalStateException e =
          assertThrows(IllegalStateException.class, () -> service.awaitTerminated());
      assertThat(e).hasCauseThat().isEqualTo(failure);
      completionLatch.countDown();
    }
  }

  public void testNotifyStartedWhenNotStarting() {
    AbstractService service = new DefaultService();
    assertThrows(IllegalStateException.class, service::notifyStarted);
  }

  public void testNotifyStoppedWhenNotRunning() {
    AbstractService service = new DefaultService();
    assertThrows(IllegalStateException.class, service::notifyStopped);
  }

  public void testNotifyFailedWhenNotStarted() {
    AbstractService service = new DefaultService();
    Exception cause = new Exception();
    assertThrows(IllegalStateException.class, () -> service.notifyFailed(cause));
  }

  public void testNotifyFailedWhenTerminated() {
    NoOpService service = new NoOpService();
    service.startAsync().awaitRunning();
    service.stopAsync().awaitTerminated();
    Exception cause = new Exception();
    assertThrows(IllegalStateException.class, () -> service.notifyFailed(cause));
  }

  private static class DefaultService extends AbstractService {
    @Override
    protected void doStart() {}

    @Override
    protected void doStop() {}
  }

  private static final Exception EXCEPTION = new Exception();
}
