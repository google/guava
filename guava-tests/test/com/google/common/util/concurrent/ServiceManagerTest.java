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

import static java.util.Arrays.asList;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.common.testing.NullPointerTester;
import com.google.common.testing.TestLogHandler;
import com.google.common.util.concurrent.ServiceManager.Listener;

import junit.framework.TestCase;

import java.util.Arrays;
import java.util.Collection;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

/**
 * Tests for {@link ServiceManager}.
 *
 * @author Luke Sandberg
 * @author Chris Nokleberg
 */
public class ServiceManagerTest extends TestCase {

  private static class NoOpService extends AbstractService {
    @Override protected void doStart() {
      notifyStarted();
    }

    @Override protected void doStop() {
      notifyStopped();
    }
  }

  /*
   * A NoOp service that will delay the startup and shutdown notification for a configurable amount
   * of time.
   */
  private static class NoOpDelayedSerivce extends NoOpService {
    private long delay;

    public NoOpDelayedSerivce(long delay) {
      this.delay = delay;
    }

    @Override protected void doStart() {
      new Thread() {
        @Override public void run() {
          Uninterruptibles.sleepUninterruptibly(delay, TimeUnit.MILLISECONDS);
          notifyStarted();
        }
      }.start();
    }

    @Override protected void doStop() {
      new Thread() {
        @Override public void run() {
          Uninterruptibles.sleepUninterruptibly(delay, TimeUnit.MILLISECONDS);
          notifyStopped();
        }
      }.start();
    }
  }

  private static class FailStartService extends NoOpService {
    @Override protected void doStart() {
      notifyFailed(new IllegalStateException("failed"));
    }
  }

  private static class FailRunService extends NoOpService {
    @Override protected void doStart() {
      super.doStart();
      notifyFailed(new IllegalStateException("failed"));
    }
  }

  private static class FailStopService extends NoOpService {
    @Override protected void doStop() {
      notifyFailed(new IllegalStateException("failed"));
    }
  }

  public void testServiceStartupTimes() {
    Service a = new NoOpDelayedSerivce(150);
    Service b = new NoOpDelayedSerivce(353);
    ServiceManager serviceManager = new ServiceManager(asList(a, b));
    serviceManager.startAsync().awaitHealthy();
    ImmutableMap<Service, Long> startupTimes = serviceManager.startupTimes();
    assertEquals(2, startupTimes.size());
    assertTrue(startupTimes.get(a) >= 150);
    assertTrue(startupTimes.get(b) >= 353);
  }

  public void testServiceStartStop() {
    Service a = new NoOpService();
    Service b = new NoOpService();
    ServiceManager manager = new ServiceManager(asList(a, b));
    RecordingListener listener = new RecordingListener();
    manager.addListener(listener);
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
    manager.addListener(listener);
    assertState(manager, Service.State.NEW, a, b, c, d, e);
    try {
      manager.startAsync().awaitHealthy();
      fail();
    } catch (IllegalStateException expected) {
    }
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
    manager.addListener(listener);
    assertState(manager, Service.State.NEW, a, b);
    try {
      manager.startAsync().awaitHealthy();
      fail();
    } catch (IllegalStateException expected) {
    }
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
    manager.addListener(listener);

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
    assertTrue(toString.contains("NoOpService"));
    assertTrue(toString.contains("FailStartService"));
  }

  public void testTimeouts() throws Exception {
    Service a = new NoOpDelayedSerivce(50);
    ServiceManager manager = new ServiceManager(asList(a));
    manager.startAsync();
    try {
      manager.awaitHealthy(1, TimeUnit.MILLISECONDS);
      fail();
    } catch (TimeoutException expected) {
    }
    manager.awaitHealthy(100, TimeUnit.MILLISECONDS); // no exception thrown

    manager.stopAsync();
    try {
      manager.awaitStopped(1, TimeUnit.MILLISECONDS);
      fail();
    } catch (TimeoutException expected) {
    }
    manager.awaitStopped(100, TimeUnit.MILLISECONDS);  // no exception thrown
  }

  /**
   * This covers a case where if the last service to stop failed then the stopped callback would
   * never be called.
   */
  public void testSingleFailedServiceCallsStopped() {
    Service a = new FailStartService();
    ServiceManager manager = new ServiceManager(asList(a));
    RecordingListener listener = new RecordingListener();
    manager.addListener(listener);
    try {
      manager.startAsync().awaitHealthy();
      fail();
    } catch (IllegalStateException expected) {
    }
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
    manager.addListener(listener);
    try {
      manager.startAsync().awaitHealthy();
      fail();
    } catch (IllegalStateException expected) {
    }
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
    manager.addListener(new Listener() {
      @Override public void failure(Service service) {
        manager.stopAsync();
      }});
    manager.startAsync();
    manager.awaitStopped(10, TimeUnit.MILLISECONDS);
  }

  private static void assertState(
      ServiceManager manager, Service.State state, Service... services) {
    Collection<Service> managerServices = manager.servicesByState().get(state);
    for (Service service : services) {
      assertEquals(service.toString(), state, service.state());
      assertEquals(service.toString(), service.isRunning(), state == Service.State.RUNNING);
      assertTrue(managerServices + " should contain " + service.toString(),
          managerServices.contains(service));
    }
  }

  /**
   * This is for covering a case where the ServiceManager would behave strangely if constructed
   * with no service under management.  Listeners would never fire because the ServiceManager was
   * healthy and stopped at the same time.  This test ensures that listeners fire and isHealthy
   * makes sense.
   */
  public void testEmptyServiceManager() {
    Logger logger = Logger.getLogger(ServiceManager.class.getName());
    logger.setLevel(Level.FINEST);
    TestLogHandler logHandler = new TestLogHandler();
    logger.addHandler(logHandler);
    ServiceManager manager = new ServiceManager(Arrays.<Service>asList());
    RecordingListener listener = new RecordingListener();
    manager.addListener(listener, MoreExecutors.sameThreadExecutor());
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
    Formatter logFormatter = new Formatter() {
      @Override public String format(LogRecord record) {
        return formatMessage(record);
      }
    };
    for (LogRecord record : logHandler.getStoredLogRecords()) {
      assertFalse(logFormatter.format(record).contains("NoOpService"));
    }
  }

  /**
   * This is for a case where a long running Listener using the sameThreadListener could deadlock
   * another thread calling stopAsync().
   */

  public void testListenerDeadlock() throws InterruptedException {
    final CountDownLatch failEnter = new CountDownLatch(1);
    Service failRunService = new AbstractService() {
      @Override protected void doStart() {
        new Thread() {
          @Override public void run() {
            notifyStarted();
            notifyFailed(new Exception("boom"));
          }
        }.start();
      }
      @Override protected void doStop() {
        notifyStopped();
      }
    };
    final ServiceManager manager = new ServiceManager(
        Arrays.asList(failRunService, new NoOpService()));
    manager.addListener(new ServiceManager.Listener() {
      @Override public void failure(Service service) {
        failEnter.countDown();
        // block forever!
        Uninterruptibles.awaitUninterruptibly(new CountDownLatch(1));
      }
    }, MoreExecutors.sameThreadExecutor());
    // We do not call awaitHealthy because, due to races, that method may throw an exception.  But
    // we really just want to wait for the thread to be in the failure callback so we wait for that
    // explicitly instead.
    manager.startAsync();
    failEnter.await();
    assertFalse("State should be updated before calling listeners", manager.isHealthy());
    // now we want to stop the services.
    Thread stoppingThread = new Thread() {
      @Override public void run() {
        manager.stopAsync().awaitStopped();
      }
    };
    stoppingThread.start();
    // this should be super fast since the only non stopped service is a NoOpService
    stoppingThread.join(1000);
    assertFalse("stopAsync has deadlocked!.", stoppingThread.isAlive());
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

    @Override public void healthy() {
      healthyCalled = true;
    }

    @Override public void stopped() {
      stoppedCalled = true;
    }

    @Override public void failure(Service service) {
      failedServices.add(service);
    }
  }
}
