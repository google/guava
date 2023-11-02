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
import static com.google.common.base.StandardSystemProperty.OS_NAME;
import static com.google.common.truth.Truth.assertThat;
import static com.google.common.util.concurrent.MoreExecutors.directExecutor;
import static java.util.Arrays.asList;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.assertThrows;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.testing.NullPointerTester;
import com.google.common.testing.TestLogHandler;
import com.google.common.util.concurrent.Service.State;
import com.google.common.util.concurrent.ServiceManager.Listener;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import junit.framework.TestCase;

/**
 * Tests for {@link ServiceManager}.
 *
 * @author Luke Sandberg
 * @author Chris Nokleberg
 */
public class ServiceManagerTest extends TestCase {

  private static class NoOpService extends AbstractService {
    @Override
    protected void doStart() {
      notifyStarted();
    }

    @Override
    protected void doStop() {
      notifyStopped();
    }
  }

  /*
   * A NoOp service that will delay the startup and shutdown notification for a configurable amount
   * of time.
   */
  private static class NoOpDelayedService extends NoOpService {
    private long delay;

    public NoOpDelayedService(long delay) {
      this.delay = delay;
    }

    @Override
    protected void doStart() {
      new Thread() {
        @Override
        public void run() {
          Uninterruptibles.sleepUninterruptibly(delay, TimeUnit.MILLISECONDS);
          notifyStarted();
        }
      }.start();
    }

    @Override
    protected void doStop() {
      new Thread() {
        @Override
        public void run() {
          Uninterruptibles.sleepUninterruptibly(delay, TimeUnit.MILLISECONDS);
          notifyStopped();
        }
      }.start();
    }
  }

  private static class FailStartService extends NoOpService {
    @Override
    protected void doStart() {
      notifyFailed(new IllegalStateException("start failure"));
    }
  }

  private static class FailRunService extends NoOpService {
    @Override
    protected void doStart() {
      super.doStart();
      notifyFailed(new IllegalStateException("run failure"));
    }
  }

  private static class FailStopService extends NoOpService {
    @Override
    protected void doStop() {
      notifyFailed(new IllegalStateException("stop failure"));
    }
  }

  public void testServiceStartupTimes() {
    if (isWindows() && isJava8()) {
      // Flaky there: https://github.com/google/guava/pull/6731#issuecomment-1736298607
      return;
    }
    Service a = new NoOpDelayedService(150);
    Service b = new NoOpDelayedService(353);
    ServiceManager serviceManager = new ServiceManager(asList(a, b));
    serviceManager.startAsync().awaitHealthy();
    ImmutableMap<Service, Long> startupTimes = serviceManager.startupTimes();
    assertThat(startupTimes).hasSize(2);
    assertThat(startupTimes.get(a)).isAtLeast(150);
    assertThat(startupTimes.get(b)).isAtLeast(353);
  }

  public void testServiceStartupDurations() {
    if (isWindows() && isJava8()) {
      // Flaky there: https://github.com/google/guava/pull/6731#issuecomment-1736298607
      return;
    }
    Service a = new NoOpDelayedService(150);
    Service b = new NoOpDelayedService(353);
    ServiceManager serviceManager = new ServiceManager(asList(a, b));
    serviceManager.startAsync().awaitHealthy();
    ImmutableMap<Service, Duration> startupTimes = serviceManager.startupDurations();
    assertThat(startupTimes).hasSize(2);
    assertThat(startupTimes.get(a)).isAtLeast(Duration.ofMillis(150));
    assertThat(startupTimes.get(b)).isAtLeast(Duration.ofMillis(353));
  }

  public void testServiceStartupTimes_selfStartingServices() {
    // This tests to ensure that:
    // 1. service times are accurate when the service is started by the manager
    // 2. service times are recorded when the service is not started by the manager (but they may
    // not be accurate).
    final Service b =
        new NoOpDelayedService(353) {
          @Override
          protected void doStart() {
            super.doStart();
            // This will delay service listener execution at least 150 milliseconds
            Uninterruptibles.sleepUninterruptibly(150, TimeUnit.MILLISECONDS);
          }
        };
    Service a =
        new NoOpDelayedService(150) {
          @Override
          protected void doStart() {
            b.startAsync();
            super.doStart();
          }
        };
    ServiceManager serviceManager = new ServiceManager(asList(a, b));
    serviceManager.startAsync().awaitHealthy();
    ImmutableMap<Service, Long> startupTimes = serviceManager.startupTimes();
    assertThat(startupTimes).hasSize(2);
    assertThat(startupTimes.get(a)).isAtLeast(150);
    // Service b startup takes at least 353 millis, but starting the timer is delayed by at least
    // 150 milliseconds. so in a perfect world the timing would be 353-150=203ms, but since either
    // of our sleep calls can be arbitrarily delayed we should just assert that there is a time
    // recorded.
    assertThat(startupTimes.get(b)).isNotNull();
  }

  public void testServiceStartStop() {
    Service a = new NoOpService();
    Service b = new NoOpService();
    ServiceManager manager = new ServiceManager(asList(a, b));
    RecordingListener listener = new RecordingListener();
    manager.addListener(listener, directExecutor());
    assertState(manager, Service.State.NEW, a, b);
    assertFalse(manager.isHealthy());
    manager.startAsync().awaitHealthy();
    assertState(manager, Service.State.RUNNING, a, b);
    assertTrue(manager.isHealthy());
    assertTrue(listener.healthyCalled);
    assertFalse(listener.stoppedCalled);
    assertTrue(listener.failedServices.isEmpty());
    manager.stopAsync().awaitStopped();
    assertState(manager, Service.State.TERMINATED, a, b);
    assertFalse(manager.isHealthy());
    assertTrue(listener.stoppedCalled);
    assertTrue(listener.failedServices.isEmpty());
  }

  public void testFailStart() throws Exception {
    Service a = new NoOpService();
    Service b = new FailStartService();
    Service c = new NoOpService();
    Service d = new FailStartService();
    Service e = new NoOpService();
    ServiceManager manager = new ServiceManager(asList(a, b, c, d, e));
    RecordingListener listener = new RecordingListener();
    manager.addListener(listener, directExecutor());
    assertState(manager, Service.State.NEW, a, b, c, d, e);
    assertThrows(IllegalStateException.class, () -> manager.startAsync().awaitHealthy());
    assertFalse(listener.healthyCalled);
    assertState(manager, Service.State.RUNNING, a, c, e);
    assertEquals(ImmutableSet.of(b, d), listener.failedServices);
    assertState(manager, Service.State.FAILED, b, d);
    assertFalse(manager.isHealthy());

    manager.stopAsync().awaitStopped();
    assertFalse(manager.isHealthy());
    assertFalse(listener.healthyCalled);
    assertTrue(listener.stoppedCalled);
  }

  public void testFailRun() throws Exception {
    Service a = new NoOpService();
    Service b = new FailRunService();
    ServiceManager manager = new ServiceManager(asList(a, b));
    RecordingListener listener = new RecordingListener();
    manager.addListener(listener, directExecutor());
    assertState(manager, Service.State.NEW, a, b);
    assertThrows(IllegalStateException.class, () -> manager.startAsync().awaitHealthy());
    assertTrue(listener.healthyCalled);
    assertEquals(ImmutableSet.of(b), listener.failedServices);

    manager.stopAsync().awaitStopped();
    assertState(manager, Service.State.FAILED, b);
    assertState(manager, Service.State.TERMINATED, a);

    assertTrue(listener.stoppedCalled);
  }

  public void testFailStop() throws Exception {
    Service a = new NoOpService();
    Service b = new FailStopService();
    Service c = new NoOpService();
    ServiceManager manager = new ServiceManager(asList(a, b, c));
    RecordingListener listener = new RecordingListener();
    manager.addListener(listener, directExecutor());

    manager.startAsync().awaitHealthy();
    assertTrue(listener.healthyCalled);
    assertFalse(listener.stoppedCalled);
    manager.stopAsync().awaitStopped();

    assertTrue(listener.stoppedCalled);
    assertEquals(ImmutableSet.of(b), listener.failedServices);
    assertState(manager, Service.State.FAILED, b);
    assertState(manager, Service.State.TERMINATED, a, c);
  }

  public void testToString() throws Exception {
    Service a = new NoOpService();
    Service b = new FailStartService();
    ServiceManager manager = new ServiceManager(asList(a, b));
    String toString = manager.toString();
    assertThat(toString).contains("NoOpService");
    assertThat(toString).contains("FailStartService");
  }

  public void testTimeouts() throws Exception {
    Service a = new NoOpDelayedService(50);
    ServiceManager manager = new ServiceManager(asList(a));
    manager.startAsync();
    assertThrows(TimeoutException.class, () -> manager.awaitHealthy(1, TimeUnit.MILLISECONDS));
    manager.awaitHealthy(5, SECONDS); // no exception thrown

    manager.stopAsync();
    assertThrows(TimeoutException.class, () -> manager.awaitStopped(1, TimeUnit.MILLISECONDS));
    manager.awaitStopped(5, SECONDS); // no exception thrown
  }

  /**
   * This covers a case where if the last service to stop failed then the stopped callback would
   * never be called.
   */
  public void testSingleFailedServiceCallsStopped() {
    Service a = new FailStartService();
    ServiceManager manager = new ServiceManager(asList(a));
    RecordingListener listener = new RecordingListener();
    manager.addListener(listener, directExecutor());
    assertThrows(IllegalStateException.class, () -> manager.startAsync().awaitHealthy());
    assertTrue(listener.stoppedCalled);
  }

  /**
   * This covers a bug where listener.healthy would get called when a single service failed during
   * startup (it occurred in more complicated cases also).
   */
  public void testFailStart_singleServiceCallsHealthy() {
    Service a = new FailStartService();
    ServiceManager manager = new ServiceManager(asList(a));
    RecordingListener listener = new RecordingListener();
    manager.addListener(listener, directExecutor());
    assertThrows(IllegalStateException.class, () -> manager.startAsync().awaitHealthy());
    assertFalse(listener.healthyCalled);
  }

  /**
   * This covers a bug where if a listener was installed that would stop the manager if any service
   * fails and something failed during startup before service.start was called on all the services,
   * then awaitStopped would deadlock due to an IllegalStateException that was thrown when trying to
   * stop the timer(!).
   */
  public void testFailStart_stopOthers() throws TimeoutException {
    Service a = new FailStartService();
    Service b = new NoOpService();
    final ServiceManager manager = new ServiceManager(asList(a, b));
    manager.addListener(
        new Listener() {
          @Override
          public void failure(Service service) {
            manager.stopAsync();
          }
        },
        directExecutor());
    manager.startAsync();
    manager.awaitStopped(10, TimeUnit.MILLISECONDS);
  }

  public void testDoCancelStart() throws TimeoutException {
    Service a =
        new AbstractService() {
          @Override
          protected void doStart() {
            // Never starts!
          }

          @Override
          protected void doCancelStart() {
            assertThat(state()).isEqualTo(Service.State.STOPPING);
            notifyStopped();
          }

          @Override
          protected void doStop() {
            throw new AssertionError(); // Should not be called.
          }
        };

    final ServiceManager manager = new ServiceManager(asList(a));
    manager.startAsync();
    manager.stopAsync();
    manager.awaitStopped(10, TimeUnit.MILLISECONDS);
    assertThat(manager.servicesByState().keySet()).containsExactly(Service.State.TERMINATED);
  }

  public void testNotifyStoppedAfterFailure() throws TimeoutException {
    Service a =
        new AbstractService() {
          @Override
          protected void doStart() {
            notifyFailed(new IllegalStateException("start failure"));
            notifyStopped(); // This will be a no-op.
          }

          @Override
          protected void doStop() {
            notifyStopped();
          }
        };
    final ServiceManager manager = new ServiceManager(asList(a));
    manager.startAsync();
    manager.awaitStopped(10, TimeUnit.MILLISECONDS);
    assertThat(manager.servicesByState().keySet()).containsExactly(Service.State.FAILED);
  }

  private static void assertState(
      ServiceManager manager, Service.State state, Service... services) {
    Collection<Service> managerServices = manager.servicesByState().get(state);
    for (Service service : services) {
      assertEquals(service.toString(), state, service.state());
      assertEquals(service.toString(), service.isRunning(), state == Service.State.RUNNING);
      assertTrue(managerServices + " should contain " + service, managerServices.contains(service));
    }
  }

  /**
   * This is for covering a case where the ServiceManager would behave strangely if constructed with
   * no service under management. Listeners would never fire because the ServiceManager was healthy
   * and stopped at the same time. This test ensures that listeners fire and isHealthy makes sense.
   */
  public void testEmptyServiceManager() {
    Logger logger = Logger.getLogger(ServiceManager.class.getName());
    logger.setLevel(Level.FINEST);
    TestLogHandler logHandler = new TestLogHandler();
    logger.addHandler(logHandler);
    ServiceManager manager = new ServiceManager(Arrays.<Service>asList());
    RecordingListener listener = new RecordingListener();
    manager.addListener(listener, directExecutor());
    manager.startAsync().awaitHealthy();
    assertTrue(manager.isHealthy());
    assertTrue(listener.healthyCalled);
    assertFalse(listener.stoppedCalled);
    assertTrue(listener.failedServices.isEmpty());
    manager.stopAsync().awaitStopped();
    assertFalse(manager.isHealthy());
    assertTrue(listener.stoppedCalled);
    assertTrue(listener.failedServices.isEmpty());
    // check that our NoOpService is not directly observable via any of the inspection methods or
    // via logging.
    assertEquals("ServiceManager{services=[]}", manager.toString());
    assertTrue(manager.servicesByState().isEmpty());
    assertTrue(manager.startupTimes().isEmpty());
    Formatter logFormatter =
        new Formatter() {
          @Override
          public String format(LogRecord record) {
            return formatMessage(record);
          }
        };
    for (LogRecord record : logHandler.getStoredLogRecords()) {
      assertThat(logFormatter.format(record)).doesNotContain("NoOpService");
    }
  }

  /**
   * Tests that a ServiceManager can be fully shut down if one of its failure listeners is slow or
   * even permanently blocked.
   */
  public void testListenerDeadlock() throws InterruptedException {
    final CountDownLatch failEnter = new CountDownLatch(1);
    final CountDownLatch failLeave = new CountDownLatch(1);
    final CountDownLatch afterStarted = new CountDownLatch(1);
    Service failRunService =
        new AbstractService() {
          @Override
          protected void doStart() {
            new Thread() {
              @Override
              public void run() {
                notifyStarted();
                // We need to wait for the main thread to leave the ServiceManager.startAsync call
                // to
                // ensure that the thread running the failure callbacks is not the main thread.
                Uninterruptibles.awaitUninterruptibly(afterStarted);
                notifyFailed(new Exception("boom"));
              }
            }.start();
          }

          @Override
          protected void doStop() {
            notifyStopped();
          }
        };
    final ServiceManager manager =
        new ServiceManager(Arrays.asList(failRunService, new NoOpService()));
    manager.addListener(
        new ServiceManager.Listener() {
          @Override
          public void failure(Service service) {
            failEnter.countDown();
            // block until after the service manager is shutdown
            Uninterruptibles.awaitUninterruptibly(failLeave);
          }
        },
        directExecutor());
    manager.startAsync();
    afterStarted.countDown();
    // We do not call awaitHealthy because, due to races, that method may throw an exception.  But
    // we really just want to wait for the thread to be in the failure callback so we wait for that
    // explicitly instead.
    failEnter.await();
    assertFalse("State should be updated before calling listeners", manager.isHealthy());
    // now we want to stop the services.
    Thread stoppingThread =
        new Thread() {
          @Override
          public void run() {
            manager.stopAsync().awaitStopped();
          }
        };
    stoppingThread.start();
    // this should be super fast since the only non-stopped service is a NoOpService
    stoppingThread.join(1000);
    assertFalse("stopAsync has deadlocked!.", stoppingThread.isAlive());
    failLeave.countDown(); // release the background thread
  }

  /**
   * Catches a bug where when constructing a service manager failed, later interactions with the
   * service could cause IllegalStateExceptions inside the partially constructed ServiceManager.
   * This ISE wouldn't actually bubble up but would get logged by ExecutionQueue. This obfuscated
   * the original error (which was not constructing ServiceManager correctly).
   */
  public void testPartiallyConstructedManager() {
    Logger logger = Logger.getLogger("global");
    logger.setLevel(Level.FINEST);
    TestLogHandler logHandler = new TestLogHandler();
    logger.addHandler(logHandler);
    NoOpService service = new NoOpService();
    service.startAsync();
    assertThrows(IllegalArgumentException.class, () -> new ServiceManager(Arrays.asList(service)));
    service.stopAsync();
    // Nothing was logged!
    assertEquals(0, logHandler.getStoredLogRecords().size());
  }

  public void testPartiallyConstructedManager_transitionAfterAddListenerBeforeStateIsReady() {
    // The implementation of this test is pretty sensitive to the implementation :( but we want to
    // ensure that if weird things happen during construction then we get exceptions.
    final NoOpService service1 = new NoOpService();
    // This service will start service1 when addListener is called.  This simulates service1 being
    // started asynchronously.
    Service service2 =
        new Service() {
          final NoOpService delegate = new NoOpService();

          @Override
          public final void addListener(Listener listener, Executor executor) {
            service1.startAsync();
            delegate.addListener(listener, executor);
          }

          // Delegates from here on down
          @Override
          public final Service startAsync() {
            return delegate.startAsync();
          }

          @Override
          public final Service stopAsync() {
            return delegate.stopAsync();
          }

          @Override
          public final void awaitRunning() {
            delegate.awaitRunning();
          }

          @Override
          public final void awaitRunning(long timeout, TimeUnit unit) throws TimeoutException {
            delegate.awaitRunning(timeout, unit);
          }

          @Override
          public final void awaitTerminated() {
            delegate.awaitTerminated();
          }

          @Override
          public final void awaitTerminated(long timeout, TimeUnit unit) throws TimeoutException {
            delegate.awaitTerminated(timeout, unit);
          }

          @Override
          public final boolean isRunning() {
            return delegate.isRunning();
          }

          @Override
          public final State state() {
            return delegate.state();
          }

          @Override
          public final Throwable failureCause() {
            return delegate.failureCause();
          }
        };
    IllegalArgumentException expected =
        assertThrows(
            IllegalArgumentException.class,
            () -> new ServiceManager(Arrays.asList(service1, service2)));
    assertThat(expected.getMessage()).contains("started transitioning asynchronously");
  }

  /**
   * This test is for a case where two Service.Listener callbacks for the same service would call
   * transitionService in the wrong order due to a race. Due to the fact that it is a race this test
   * isn't guaranteed to expose the issue, but it is at least likely to become flaky if the race
   * sneaks back in, and in this case flaky means something is definitely wrong.
   *
   * <p>Before the bug was fixed this test would fail at least 30% of the time.
   */
  public void testTransitionRace() throws TimeoutException {
    for (int k = 0; k < 1000; k++) {
      List<Service> services = Lists.newArrayList();
      for (int i = 0; i < 5; i++) {
        services.add(new SnappyShutdownService(i));
      }
      ServiceManager manager = new ServiceManager(services);
      manager.startAsync().awaitHealthy();
      manager.stopAsync().awaitStopped(10, TimeUnit.SECONDS);
    }
  }

  /**
   * This service will shut down very quickly after stopAsync is called and uses a background thread
   * so that we know that the stopping() listeners will execute on a different thread than the
   * terminated() listeners.
   */
  private static class SnappyShutdownService extends AbstractExecutionThreadService {
    final int index;
    final CountDownLatch latch = new CountDownLatch(1);

    SnappyShutdownService(int index) {
      this.index = index;
    }

    @Override
    protected void run() throws Exception {
      latch.await();
    }

    @Override
    protected void triggerShutdown() {
      latch.countDown();
    }

    @Override
    protected String serviceName() {
      return this.getClass().getSimpleName() + "[" + index + "]";
    }
  }

  public void testNulls() {
    ServiceManager manager = new ServiceManager(Arrays.<Service>asList());
    new NullPointerTester()
        .setDefault(ServiceManager.Listener.class, new RecordingListener())
        .testAllPublicInstanceMethods(manager);
  }

  private static final class RecordingListener extends ServiceManager.Listener {
    volatile boolean healthyCalled;
    volatile boolean stoppedCalled;
    final Set<Service> failedServices = Sets.newConcurrentHashSet();

    @Override
    public void healthy() {
      healthyCalled = true;
    }

    @Override
    public void stopped() {
      stoppedCalled = true;
    }

    @Override
    public void failure(Service service) {
      failedServices.add(service);
    }
  }

  private static boolean isWindows() {
    return OS_NAME.value().startsWith("Windows");
  }

  private static boolean isJava8() {
    return JAVA_SPECIFICATION_VERSION.value().equals("1.8");
  }
}
