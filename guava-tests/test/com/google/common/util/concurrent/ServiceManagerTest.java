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

import static com.google.common.collect.ImmutableSet.of;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;

import junit.framework.TestCase;

import java.util.Collection;
import java.util.Set;
import java.util.concurrent.TimeUnit;

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

    public NoOpDelayedSerivce(long delay){
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
    ServiceManager serviceManager = new ServiceManager(of(a, b));
    serviceManager.startAsync().awaitHealthy();
    ImmutableMap<Service, Long> startupTimes = serviceManager.startupTimes();
    assertEquals(2, startupTimes.size());
    assertTrue(startupTimes.get(a) >= 150);
    assertTrue(startupTimes.get(b) >= 353);
  }

  public void testServiceStartStop() {
    Service a = new NoOpService();
    Service b = new NoOpService();
    ServiceManager manager = new ServiceManager(of(a, b));
    RecordingListener listener = new RecordingListener();
    manager.addListener(listener, MoreExecutors.sameThreadExecutor());
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
    ServiceManager manager = new ServiceManager(of(a, b, c, d, e));
    RecordingListener listener = new RecordingListener();
    manager.addListener(listener, MoreExecutors.sameThreadExecutor());
    assertState(manager, Service.State.NEW, a, b, c, d, e);
    try {
      manager.startAsync().awaitHealthy();
      fail("expected exception");
    } catch (Throwable expected) {
      assertTrue(expected instanceof IllegalStateException);
    }
    assertFalse(listener.healthyCalled);
    assertState(manager, Service.State.RUNNING, a, c, e);
    assertEquals(of(b, d), listener.failedServices);
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
    ServiceManager manager = new ServiceManager(of(a, b));
    RecordingListener listener = new RecordingListener();
    manager.addListener(listener, MoreExecutors.sameThreadExecutor());
    assertState(manager, Service.State.NEW, a, b);
    try {
      manager.startAsync().awaitHealthy();
      fail("expected exception");
    } catch (IllegalStateException e) {
      // expected
    }
    assertTrue(listener.healthyCalled);
    assertEquals(of(b), listener.failedServices);

    manager.stopAsync().awaitStopped();
    assertState(manager, Service.State.FAILED, b);
    assertState(manager, Service.State.TERMINATED, a);

    assertTrue(listener.stoppedCalled);
  }

  public void testFailStop() throws Exception {
    Service a = new NoOpService();
    Service b = new FailStopService();
    Service c = new NoOpService();
    ServiceManager manager = new ServiceManager(of(a, b, c));
    RecordingListener listener = new RecordingListener();
    manager.addListener(listener, MoreExecutors.sameThreadExecutor());

    manager.startAsync().awaitHealthy();
    assertTrue(listener.healthyCalled);
    assertFalse(listener.stoppedCalled);
    manager.stopAsync().awaitStopped();

    assertTrue(listener.stoppedCalled);
    assertEquals(of(b), listener.failedServices);
    assertState(manager, Service.State.FAILED, b);
    assertState(manager, Service.State.TERMINATED, a, c);
  }

  public void testToString() throws Exception {
    Service a = new NoOpService();
    Service b = new FailStartService();
    ServiceManager manager = new ServiceManager(of(a, b));
    String toString = manager.toString();
    assertTrue(toString.contains("NoOpService"));
    assertTrue(toString.contains("FailStartService"));
  }

  public void testTimeouts() {
    Service a = new NoOpDelayedSerivce(50);
    ServiceManager manager = new ServiceManager(of(a));
    manager.startAsync();
    assertFalse(manager.awaitHealthy(10, TimeUnit.MILLISECONDS));
    assertTrue(manager.awaitHealthy(50, TimeUnit.MILLISECONDS));

    manager.stopAsync();
    assertFalse(manager.awaitStopped(10, TimeUnit.MILLISECONDS));
    assertTrue(manager.awaitStopped(50, TimeUnit.MILLISECONDS));
  }

  private void assertState(ServiceManager manager, Service.State state, Service... services) {
    Collection<Service> managerServices = manager.servicesByState().get(state);
    for (Service service : services) {
      assertEquals(service.toString(), state, service.state());
      assertEquals(service.toString(), service.isRunning(), state == Service.State.RUNNING);
      assertTrue(managerServices + " should contain " + service.toString(),
          managerServices.contains(service));
    }
  }

  private static final class RecordingListener implements ServiceManager.Listener {
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
