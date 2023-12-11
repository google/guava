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
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.assertThrows;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.Service.Listener;
import com.google.common.util.concurrent.Service.State;
import com.google.errorprone.annotations.concurrent.GuardedBy;
import java.lang.Thread.UncaughtExceptionHandler;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import junit.framework.TestCase;

/**
 * Unit test for {@link AbstractService}.
 *
 * @author Jesse Wilson
 */
public class AbstractServiceTest extends TestCase {

  private static final long LONG_TIMEOUT_MILLIS = 10000;
  private Thread executionThread;
  private Throwable thrownByExecutionThread;

  public void testNoOpServiceStartStop() throws Exception {
    NoOpService service = new NoOpService();
    RecordingListener listener = RecordingListener.record(service);

    assertEquals(State.NEW, service.state());
    assertFalse(service.isRunning());
    assertFalse(service.running);

    service.startAsync();
    assertEquals(State.RUNNING, service.state());
    assertTrue(service.isRunning());
    assertTrue(service.running);

    service.stopAsync();
    assertEquals(State.TERMINATED, service.state());
    assertFalse(service.isRunning());
    assertFalse(service.running);
    assertEquals(
        ImmutableList.of(State.STARTING, State.RUNNING, State.STOPPING, State.TERMINATED),
        listener.getStateHistory());
  }

  public void testNoOpServiceStartAndWaitStopAndWait() throws Exception {
    NoOpService service = new NoOpService();

    service.startAsync().awaitRunning();
    assertEquals(State.RUNNING, service.state());

    service.stopAsync().awaitTerminated();
    assertEquals(State.TERMINATED, service.state());
  }

  public void testNoOpServiceStartAsyncAndAwaitStopAsyncAndAwait() throws Exception {
    NoOpService service = new NoOpService();

    service.startAsync().awaitRunning();
    assertEquals(State.RUNNING, service.state());

    service.stopAsync().awaitTerminated();
    assertEquals(State.TERMINATED, service.state());
  }

  public void testNoOpServiceStopIdempotence() throws Exception {
    NoOpService service = new NoOpService();
    RecordingListener listener = RecordingListener.record(service);
    service.startAsync().awaitRunning();
    assertEquals(State.RUNNING, service.state());

    service.stopAsync();
    service.stopAsync();
    assertEquals(State.TERMINATED, service.state());
    assertEquals(
        ImmutableList.of(State.STARTING, State.RUNNING, State.STOPPING, State.TERMINATED),
        listener.getStateHistory());
  }

  public void testNoOpServiceStopIdempotenceAfterWait() throws Exception {
    NoOpService service = new NoOpService();

    service.startAsync().awaitRunning();

    service.stopAsync().awaitTerminated();
    service.stopAsync();
    assertEquals(State.TERMINATED, service.state());
  }

  public void testNoOpServiceStopIdempotenceDoubleWait() throws Exception {
    NoOpService service = new NoOpService();

    service.startAsync().awaitRunning();
    assertEquals(State.RUNNING, service.state());

    service.stopAsync().awaitTerminated();
    service.stopAsync().awaitTerminated();
    assertEquals(State.TERMINATED, service.state());
  }

  public void testNoOpServiceStartStopAndWaitUninterruptible() throws Exception {
    NoOpService service = new NoOpService();

    currentThread().interrupt();
    try {
      service.startAsync().awaitRunning();
      assertEquals(State.RUNNING, service.state());

      service.stopAsync().awaitTerminated();
      assertEquals(State.TERMINATED, service.state());

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
    assertEquals(State.STARTING, service.state());
    assertFalse(service.isRunning());
    assertTrue(service.doStartCalled);

    service.notifyStarted(); // usually this would be invoked by another thread
    assertEquals(State.RUNNING, service.state());
    assertTrue(service.isRunning());

    service.stopAsync();
    assertEquals(State.STOPPING, service.state());
    assertFalse(service.isRunning());
    assertTrue(service.doStopCalled);

    service.notifyStopped(); // usually this would be invoked by another thread
    assertEquals(State.TERMINATED, service.state());
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
    assertEquals(State.TERMINATED, service.state());
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
    assertEquals(State.STARTING, service.state());
    assertFalse(service.isRunning());
    assertTrue(service.doStartCalled);

    service.stopAsync();
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
        ImmutableList.of(State.STARTING, State.STOPPING, State.TERMINATED),
        listener.getStateHistory());
  }

  /**
   * This tests for a bug where if {@link Service#stopAsync()} was called while the service was
   * {@link State#STARTING} more than once, the {@link Listener#stopping(State)} callback would get
   * called multiple times.
   */
  public void testManualServiceStopMultipleTimesWhileStarting() throws Exception {
    ManualSwitchedService service = new ManualSwitchedService();
    final AtomicInteger stoppingCount = new AtomicInteger();
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
    assertEquals(State.TERMINATED, service.state());
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
    assertEquals(State.RUNNING, service.state());
    assertTrue(service.isRunning());
    assertFalse(service.doStopCalled);

    service.notifyStopped();
    assertEquals(State.TERMINATED, service.state());
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
    final NoOpService service = new NoOpService();
    Thread waiter =
        new Thread() {
          @Override
          public void run() {
            service.awaitTerminated();
          }
        };
    waiter.start();
    service.startAsync().awaitRunning();
    assertEquals(State.RUNNING, service.state());
    service.stopAsync();
    waiter.join(LONG_TIMEOUT_MILLIS); // ensure that the await in the other thread is triggered
    assertFalse(waiter.isAlive());
  }

  public void testAwaitTerminated_FailedService() throws Exception {
    final ManualSwitchedService service = new ManualSwitchedService();
    final AtomicReference<Throwable> exception = Atomics.newReference();
    Thread waiter =
        new Thread() {
          @Override
          public void run() {
            try {
              service.awaitTerminated();
              fail("Expected an IllegalStateException");
            } catch (Throwable t) {
              exception.set(t);
            }
          }
        };
    waiter.start();
    service.startAsync();
    service.notifyStarted();
    assertEquals(State.RUNNING, service.state());
    service.notifyFailed(EXCEPTION);
    assertEquals(State.FAILED, service.state());
    waiter.join(LONG_TIMEOUT_MILLIS);
    assertFalse(waiter.isAlive());
    assertThat(exception.get()).isInstanceOf(IllegalStateException.class);
    assertThat(exception.get()).hasCauseThat().isEqualTo(EXCEPTION);
  }

  public void testThreadedServiceStartAndWaitStopAndWait() throws Throwable {
    ThreadedService service = new ThreadedService();
    RecordingListener listener = RecordingListener.record(service);
    service.startAsync().awaitRunning();
    assertEquals(State.RUNNING, service.state());

    service.awaitRunChecks();

    service.stopAsync().awaitTerminated();
    assertEquals(State.TERMINATED, service.state());

    throwIfSet(thrownByExecutionThread);
    assertEquals(
        ImmutableList.of(State.STARTING, State.RUNNING, State.STOPPING, State.TERMINATED),
        listener.getStateHistory());
  }

  public void testThreadedServiceStopIdempotence() throws Throwable {
    ThreadedService service = new ThreadedService();

    service.startAsync().awaitRunning();
    assertEquals(State.RUNNING, service.state());

    service.awaitRunChecks();

    service.stopAsync();
    service.stopAsync().awaitTerminated();
    assertEquals(State.TERMINATED, service.state());

    throwIfSet(thrownByExecutionThread);
  }

  public void testThreadedServiceStopIdempotenceAfterWait() throws Throwable {
    ThreadedService service = new ThreadedService();

    service.startAsync().awaitRunning();
    assertEquals(State.RUNNING, service.state());

    service.awaitRunChecks();

    service.stopAsync().awaitTerminated();
    service.stopAsync();
    assertEquals(State.TERMINATED, service.state());

    executionThread.join();

    throwIfSet(thrownByExecutionThread);
  }

  public void testThreadedServiceStopIdempotenceDoubleWait() throws Throwable {
    ThreadedService service = new ThreadedService();

    service.startAsync().awaitRunning();
    assertEquals(State.RUNNING, service.state());

    service.awaitRunChecks();

    service.stopAsync().awaitTerminated();
    service.stopAsync().awaitTerminated();
    assertEquals(State.TERMINATED, service.state());

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
    IllegalStateException e =
        assertThrows(IllegalStateException.class, () -> service.awaitRunning());
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
      assertEquals(State.STARTING, state());
      invokeOnExecutionThreadForTest(
          new Runnable() {
            @Override
            public void run() {
              assertEquals(State.STARTING, state());
              notifyStarted();
              assertEquals(State.RUNNING, state());
              hasConfirmedIsRunning.countDown();
            }
          });
    }

    @Override
    protected void doStop() {
      assertEquals(State.STOPPING, state());
      invokeOnExecutionThreadForTest(
          new Runnable() {
            @Override
            public void run() {
              assertEquals(State.STOPPING, state());
              notifyStopped();
              assertEquals(State.TERMINATED, state());
            }
          });
    }
  }

  private void invokeOnExecutionThreadForTest(Runnable runnable) {
    executionThread = new Thread(runnable);
    executionThread.setUncaughtExceptionHandler(
        new UncaughtExceptionHandler() {
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

    service.stopAsync();
    assertEquals(State.TERMINATED, service.state());

    assertThrows(IllegalStateException.class, () -> service.startAsync());
    assertEquals(State.TERMINATED, Iterables.getOnlyElement(listener.getStateHistory()));
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
    IllegalStateException e =
        assertThrows(IllegalStateException.class, () -> service.awaitRunning());
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
    IllegalStateException e =
        assertThrows(IllegalStateException.class, () -> service.awaitTerminated());
    assertEquals(service.exception, service.failureCause());
    assertThat(e).hasCauseThat().isEqualTo(service.exception);
    assertEquals(
        ImmutableList.of(State.STARTING, State.RUNNING, State.FAILED), listener.getStateHistory());
  }

  public void testFailureCause_throwsIfNotFailed() {
    StopFailingService service = new StopFailingService();
    assertThrows(IllegalStateException.class, () -> service.failureCause());
    service.startAsync().awaitRunning();
    assertThrows(IllegalStateException.class, () -> service.failureCause());
    IllegalStateException e =
        assertThrows(IllegalStateException.class, () -> service.stopAsync().awaitTerminated());
    assertEquals(EXCEPTION, service.failureCause());
    assertThat(e).hasCauseThat().isEqualTo(EXCEPTION);
  }

  public void testAddListenerAfterFailureDoesntCauseDeadlock() throws InterruptedException {
    final StartFailingService service = new StartFailingService();
    service.startAsync();
    assertEquals(State.FAILED, service.state());
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
    final NoOpThreadedService service = new NoOpThreadedService();
    service.addListener(
        new Listener() {
          @Override
          public void running() {
            service.awaitRunning();
          }
        },
        directExecutor());
    service.startAsync().awaitRunning(LONG_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
    service.stopAsync();
  }

  public void testListenerDoesntDeadlockOnStopAndWaitFromTerminated() throws Exception {
    final NoOpThreadedService service = new NoOpThreadedService();
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
    final List<State> stateHistory = Lists.newArrayList();

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
      assertNotSame(State.NEW, service.state());
      stateHistory.add(State.STARTING);
    }

    @Override
    public synchronized void running() {
      assertEquals(State.STARTING, Iterables.getOnlyElement(stateHistory));
      stateHistory.add(State.RUNNING);
      service.awaitRunning();
      assertNotSame(State.STARTING, service.state());
    }

    @Override
    public synchronized void stopping(State from) {
      assertEquals(from, Iterables.getLast(stateHistory));
      stateHistory.add(State.STOPPING);
      if (from == State.STARTING) {
        try {
          service.awaitRunning();
          fail();
        } catch (IllegalStateException expected) {
          assertThat(expected).hasCauseThat().isNull();
          assertThat(expected)
              .hasMessageThat()
              .isEqualTo("Expected the service " + service + " to be RUNNING, but was STOPPING");
        }
      }
      assertNotSame(from, service.state());
    }

    @Override
    public synchronized void terminated(State from) {
      assertEquals(from, Iterables.getLast(stateHistory, State.NEW));
      stateHistory.add(State.TERMINATED);
      assertEquals(State.TERMINATED, service.state());
      if (from == State.NEW) {
        try {
          service.awaitRunning();
          fail();
        } catch (IllegalStateException expected) {
          assertThat(expected).hasCauseThat().isNull();
          assertThat(expected)
              .hasMessageThat()
              .isEqualTo("Expected the service " + service + " to be RUNNING, but was TERMINATED");
        }
      }
      completionLatch.countDown();
    }

    @Override
    public synchronized void failed(State from, Throwable failure) {
      assertEquals(from, Iterables.getLast(stateHistory));
      stateHistory.add(State.FAILED);
      assertEquals(State.FAILED, service.state());
      assertEquals(failure, service.failureCause());
      if (from == State.STARTING) {
        try {
          service.awaitRunning();
          fail();
        } catch (IllegalStateException e) {
          assertThat(e).hasCauseThat().isEqualTo(failure);
        }
      }
      try {
        service.awaitTerminated();
        fail();
      } catch (IllegalStateException e) {
        assertThat(e).hasCauseThat().isEqualTo(failure);
      }
      completionLatch.countDown();
    }
  }

  public void testNotifyStartedWhenNotStarting() {
    AbstractService service = new DefaultService();
    assertThrows(IllegalStateException.class, () -> service.notifyStarted());
  }

  public void testNotifyStoppedWhenNotRunning() {
    AbstractService service = new DefaultService();
    assertThrows(IllegalStateException.class, () -> service.notifyStopped());
  }

  public void testNotifyFailedWhenNotStarted() {
    AbstractService service = new DefaultService();
    assertThrows(IllegalStateException.class, () -> service.notifyFailed(new Exception()));
  }

  public void testNotifyFailedWhenTerminated() {
    NoOpService service = new NoOpService();
    service.startAsync().awaitRunning();
    service.stopAsync().awaitTerminated();
    assertThrows(IllegalStateException.class, () -> service.notifyFailed(new Exception()));
  }

  private static class DefaultService extends AbstractService {
    @Override
    protected void doStart() {}

    @Override
    protected void doStop() {}
  }

  private static final Exception EXCEPTION = new Exception();
}
