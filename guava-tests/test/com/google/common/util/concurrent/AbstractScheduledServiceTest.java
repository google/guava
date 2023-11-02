/*
 * Copyright (C) 2011 The Guava Authors
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
import static com.google.common.util.concurrent.AbstractScheduledService.Scheduler.newFixedDelaySchedule;
import static com.google.common.util.concurrent.MoreExecutors.directExecutor;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.NANOSECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.assertThrows;

import com.google.common.util.concurrent.AbstractScheduledService.Cancellable;
import com.google.common.util.concurrent.AbstractScheduledService.Scheduler;
import com.google.common.util.concurrent.Service.State;
import com.google.common.util.concurrent.testing.TestingExecutors;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Delayed;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import junit.framework.TestCase;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Unit test for {@link AbstractScheduledService}.
 *
 * @author Luke Sandberg
 */
public class AbstractScheduledServiceTest extends TestCase {

  volatile Scheduler configuration = newFixedDelaySchedule(0, 10, MILLISECONDS);
  volatile @Nullable ScheduledFuture<?> future = null;

  volatile boolean atFixedRateCalled = false;
  volatile boolean withFixedDelayCalled = false;
  volatile boolean scheduleCalled = false;

  final ScheduledExecutorService executor =
      new ScheduledThreadPoolExecutor(10) {
        @Override
        public ScheduledFuture<?> scheduleWithFixedDelay(
            Runnable command, long initialDelay, long delay, TimeUnit unit) {
          return future = super.scheduleWithFixedDelay(command, initialDelay, delay, unit);
        }
      };

  public void testServiceStartStop() throws Exception {
    NullService service = new NullService();
    service.startAsync().awaitRunning();
    assertFalse(future.isDone());
    service.stopAsync().awaitTerminated();
    assertTrue(future.isCancelled());
  }

  private class NullService extends AbstractScheduledService {
    @Override
    protected void runOneIteration() throws Exception {}

    @Override
    protected Scheduler scheduler() {
      return configuration;
    }

    @Override
    protected ScheduledExecutorService executor() {
      return executor;
    }
  }

  public void testFailOnExceptionFromRun() throws Exception {
    TestService service = new TestService();
    service.runException = new Exception();
    service.startAsync().awaitRunning();
    service.runFirstBarrier.await();
    service.runSecondBarrier.await();
    assertThrows(CancellationException.class, () -> future.get());
    // An execution exception holds a runtime exception (from throwables.propagate) that holds our
    // original exception.
    assertEquals(service.runException, service.failureCause());
    assertEquals(Service.State.FAILED, service.state());
  }

  public void testFailOnExceptionFromStartUp() {
    TestService service = new TestService();
    service.startUpException = new Exception();
    IllegalStateException e =
        assertThrows(IllegalStateException.class, () -> service.startAsync().awaitRunning());
    assertThat(e).hasCauseThat().isEqualTo(service.startUpException);
    assertEquals(0, service.numberOfTimesRunCalled.get());
    assertEquals(Service.State.FAILED, service.state());
  }

  public void testFailOnErrorFromStartUpListener() throws InterruptedException {
    final Error error = new Error();
    final CountDownLatch latch = new CountDownLatch(1);
    TestService service = new TestService();
    service.addListener(
        new Service.Listener() {
          @Override
          public void running() {
            throw error;
          }

          @Override
          public void failed(State from, Throwable failure) {
            assertEquals(State.RUNNING, from);
            assertEquals(error, failure);
            latch.countDown();
          }
        },
        directExecutor());
    service.startAsync();
    latch.await();

    assertEquals(0, service.numberOfTimesRunCalled.get());
    assertEquals(Service.State.FAILED, service.state());
  }

  public void testFailOnExceptionFromShutDown() throws Exception {
    TestService service = new TestService();
    service.shutDownException = new Exception();
    service.startAsync().awaitRunning();
    service.runFirstBarrier.await();
    service.stopAsync();
    service.runSecondBarrier.await();
    IllegalStateException e =
        assertThrows(IllegalStateException.class, () -> service.awaitTerminated());
    assertThat(e).hasCauseThat().isEqualTo(service.shutDownException);
    assertEquals(Service.State.FAILED, service.state());
  }

  public void testRunOneIterationCalledMultipleTimes() throws Exception {
    TestService service = new TestService();
    service.startAsync().awaitRunning();
    for (int i = 1; i < 10; i++) {
      service.runFirstBarrier.await();
      assertEquals(i, service.numberOfTimesRunCalled.get());
      service.runSecondBarrier.await();
    }
    service.runFirstBarrier.await();
    service.stopAsync();
    service.runSecondBarrier.await();
    service.stopAsync().awaitTerminated();
  }

  public void testExecutorOnlyCalledOnce() throws Exception {
    TestService service = new TestService();
    service.startAsync().awaitRunning();
    // It should be called once during startup.
    assertEquals(1, service.numberOfTimesExecutorCalled.get());
    for (int i = 1; i < 10; i++) {
      service.runFirstBarrier.await();
      assertEquals(i, service.numberOfTimesRunCalled.get());
      service.runSecondBarrier.await();
    }
    service.runFirstBarrier.await();
    service.stopAsync();
    service.runSecondBarrier.await();
    service.stopAsync().awaitTerminated();
    // Only called once overall.
    assertEquals(1, service.numberOfTimesExecutorCalled.get());
  }

  public void testDefaultExecutorIsShutdownWhenServiceIsStopped() throws Exception {
    final AtomicReference<ScheduledExecutorService> executor = Atomics.newReference();
    AbstractScheduledService service =
        new AbstractScheduledService() {
          @Override
          protected void runOneIteration() throws Exception {}

          @Override
          protected ScheduledExecutorService executor() {
            executor.set(super.executor());
            return executor.get();
          }

          @Override
          protected Scheduler scheduler() {
            return newFixedDelaySchedule(0, 1, MILLISECONDS);
          }
        };

    service.startAsync();
    assertFalse(service.executor().isShutdown());
    service.awaitRunning();
    service.stopAsync();
    service.awaitTerminated();
    assertTrue(executor.get().awaitTermination(100, MILLISECONDS));
  }

  public void testDefaultExecutorIsShutdownWhenServiceFails() throws Exception {
    final AtomicReference<ScheduledExecutorService> executor = Atomics.newReference();
    AbstractScheduledService service =
        new AbstractScheduledService() {
          @Override
          protected void startUp() throws Exception {
            throw new Exception("Failed");
          }

          @Override
          protected void runOneIteration() throws Exception {}

          @Override
          protected ScheduledExecutorService executor() {
            executor.set(super.executor());
            return executor.get();
          }

          @Override
          protected Scheduler scheduler() {
            return newFixedDelaySchedule(0, 1, MILLISECONDS);
          }
        };

    assertThrows(IllegalStateException.class, () -> service.startAsync().awaitRunning());

    assertTrue(executor.get().awaitTermination(100, MILLISECONDS));
  }

  public void testSchedulerOnlyCalledOnce() throws Exception {
    TestService service = new TestService();
    service.startAsync().awaitRunning();
    // It should be called once during startup.
    assertEquals(1, service.numberOfTimesSchedulerCalled.get());
    for (int i = 1; i < 10; i++) {
      service.runFirstBarrier.await();
      assertEquals(i, service.numberOfTimesRunCalled.get());
      service.runSecondBarrier.await();
    }
    service.runFirstBarrier.await();
    service.stopAsync();
    service.runSecondBarrier.await();
    service.awaitTerminated();
    // Only called once overall.
    assertEquals(1, service.numberOfTimesSchedulerCalled.get());
  }

  public void testTimeout() {
    // Create a service whose executor will never run its commands
    Service service =
        new AbstractScheduledService() {
          @Override
          protected Scheduler scheduler() {
            return Scheduler.newFixedDelaySchedule(0, 1, NANOSECONDS);
          }

          @Override
          protected ScheduledExecutorService executor() {
            return TestingExecutors.noOpScheduledExecutor();
          }

          @Override
          protected void runOneIteration() throws Exception {}

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

  private class TestService extends AbstractScheduledService {
    CyclicBarrier runFirstBarrier = new CyclicBarrier(2);
    CyclicBarrier runSecondBarrier = new CyclicBarrier(2);

    volatile boolean startUpCalled = false;
    volatile boolean shutDownCalled = false;
    AtomicInteger numberOfTimesRunCalled = new AtomicInteger(0);
    AtomicInteger numberOfTimesExecutorCalled = new AtomicInteger(0);
    AtomicInteger numberOfTimesSchedulerCalled = new AtomicInteger(0);
    volatile @Nullable Exception runException = null;
    volatile @Nullable Exception startUpException = null;
    volatile @Nullable Exception shutDownException = null;

    @Override
    protected void runOneIteration() throws Exception {
      assertTrue(startUpCalled);
      assertFalse(shutDownCalled);
      numberOfTimesRunCalled.incrementAndGet();
      assertEquals(State.RUNNING, state());
      runFirstBarrier.await();
      runSecondBarrier.await();
      if (runException != null) {
        throw runException;
      }
    }

    @Override
    protected void startUp() throws Exception {
      assertFalse(startUpCalled);
      assertFalse(shutDownCalled);
      startUpCalled = true;
      assertEquals(State.STARTING, state());
      if (startUpException != null) {
        throw startUpException;
      }
    }

    @Override
    protected void shutDown() throws Exception {
      assertTrue(startUpCalled);
      assertFalse(shutDownCalled);
      shutDownCalled = true;
      if (shutDownException != null) {
        throw shutDownException;
      }
    }

    @Override
    protected ScheduledExecutorService executor() {
      numberOfTimesExecutorCalled.incrementAndGet();
      return executor;
    }

    @Override
    protected Scheduler scheduler() {
      numberOfTimesSchedulerCalled.incrementAndGet();
      return configuration;
    }
  }

  // Tests for Scheduler:

  // These constants are arbitrary and just used to make sure that the correct method is called
  // with the correct parameters.
  private static final int INITIAL_DELAY = 10;
  private static final int DELAY = 20;
  private static final TimeUnit UNIT = MILLISECONDS;

  // Unique runnable object used for comparison.
  final Runnable testRunnable =
      new Runnable() {
        @Override
        public void run() {}
      };
  boolean called = false;

  private void assertSingleCallWithCorrectParameters(
      Runnable command, long initialDelay, long delay, TimeUnit unit) {
    assertFalse(called); // only called once.
    called = true;
    assertEquals(INITIAL_DELAY, initialDelay);
    assertEquals(DELAY, delay);
    assertEquals(UNIT, unit);
    assertEquals(testRunnable, command);
  }

  public void testFixedRateSchedule() {
    Scheduler schedule = Scheduler.newFixedRateSchedule(INITIAL_DELAY, DELAY, UNIT);
    Cancellable unused =
        schedule.schedule(
            null,
            new ScheduledThreadPoolExecutor(1) {
              @Override
              public ScheduledFuture<?> scheduleAtFixedRate(
                  Runnable command, long initialDelay, long period, TimeUnit unit) {
                assertSingleCallWithCorrectParameters(command, initialDelay, period, unit);
                return new ThrowingScheduledFuture<>();
              }
            },
            testRunnable);
    assertTrue(called);
  }

  public void testFixedDelaySchedule() {
    Scheduler schedule = newFixedDelaySchedule(INITIAL_DELAY, DELAY, UNIT);
    Cancellable unused =
        schedule.schedule(
            null,
            new ScheduledThreadPoolExecutor(10) {
              @Override
              public ScheduledFuture<?> scheduleWithFixedDelay(
                  Runnable command, long initialDelay, long delay, TimeUnit unit) {
                assertSingleCallWithCorrectParameters(command, initialDelay, delay, unit);
                return new ThrowingScheduledFuture<>();
              }
            },
            testRunnable);
    assertTrue(called);
  }

  private static final class ThrowingScheduledFuture<V> extends ForwardingFuture<V>
      implements ScheduledFuture<V> {
    @Override
    protected Future<V> delegate() {
      throw new UnsupportedOperationException("test should not care about this");
    }

    @Override
    public long getDelay(TimeUnit unit) {
      throw new UnsupportedOperationException("test should not care about this");
    }

    @Override
    public int compareTo(Delayed other) {
      throw new UnsupportedOperationException("test should not care about this");
    }
  }

  public void testFixedDelayScheduleFarFuturePotentiallyOverflowingScheduleIsNeverReached()
      throws Exception {
    TestAbstractScheduledCustomService service =
        new TestAbstractScheduledCustomService() {
          @Override
          protected Scheduler scheduler() {
            return newFixedDelaySchedule(Long.MAX_VALUE, Long.MAX_VALUE, SECONDS);
          }
        };
    service.startAsync().awaitRunning();
    assertThrows(TimeoutException.class, () -> service.firstBarrier.await(5, SECONDS));
    assertEquals(0, service.numIterations.get());
    service.stopAsync();
    service.awaitTerminated();
  }

  public void testCustomSchedulerFarFuturePotentiallyOverflowingScheduleIsNeverReached()
      throws Exception {
    TestAbstractScheduledCustomService service =
        new TestAbstractScheduledCustomService() {
          @Override
          protected Scheduler scheduler() {
            return new AbstractScheduledService.CustomScheduler() {
              @Override
              protected Schedule getNextSchedule() throws Exception {
                return new Schedule(Long.MAX_VALUE, SECONDS);
              }
            };
          }
        };
    service.startAsync().awaitRunning();
    assertThrows(TimeoutException.class, () -> service.firstBarrier.await(5, SECONDS));
    assertEquals(0, service.numIterations.get());
    service.stopAsync();
    service.awaitTerminated();
  }

  private static class TestCustomScheduler extends AbstractScheduledService.CustomScheduler {
    public AtomicInteger scheduleCounter = new AtomicInteger(0);

    @Override
    protected Schedule getNextSchedule() throws Exception {
      scheduleCounter.incrementAndGet();
      return new Schedule(0, SECONDS);
    }
  }

  public void testCustomSchedule_startStop() throws Exception {
    final CyclicBarrier firstBarrier = new CyclicBarrier(2);
    final CyclicBarrier secondBarrier = new CyclicBarrier(2);
    final AtomicBoolean shouldWait = new AtomicBoolean(true);
    Runnable task =
        new Runnable() {
          @Override
          public void run() {
            try {
              if (shouldWait.get()) {
                firstBarrier.await();
                secondBarrier.await();
              }
            } catch (Exception e) {
              throw new RuntimeException(e);
            }
          }
        };
    TestCustomScheduler scheduler = new TestCustomScheduler();
    Cancellable future = scheduler.schedule(null, Executors.newScheduledThreadPool(10), task);
    firstBarrier.await();
    assertEquals(1, scheduler.scheduleCounter.get());
    secondBarrier.await();
    firstBarrier.await();
    assertEquals(2, scheduler.scheduleCounter.get());
    shouldWait.set(false);
    secondBarrier.await();
    future.cancel(false);
  }

  public void testCustomSchedulerServiceStop() throws Exception {
    TestAbstractScheduledCustomService service = new TestAbstractScheduledCustomService();
    service.startAsync().awaitRunning();
    service.firstBarrier.await();
    assertEquals(1, service.numIterations.get());
    service.stopAsync();
    service.secondBarrier.await();
    service.awaitTerminated();
    // Sleep for a while just to ensure that our task wasn't called again.
    Thread.sleep(UNIT.toMillis(3 * DELAY));
    assertEquals(1, service.numIterations.get());
  }

  public void testCustomScheduler_deadlock() throws InterruptedException, BrokenBarrierException {
    final CyclicBarrier inGetNextSchedule = new CyclicBarrier(2);
    // This will flakily deadlock, so run it multiple times to increase the flake likelihood
    for (int i = 0; i < 1000; i++) {
      Service service =
          new AbstractScheduledService() {
            @Override
            protected void runOneIteration() {}

            @Override
            protected Scheduler scheduler() {
              return new CustomScheduler() {
                @Override
                protected Schedule getNextSchedule() throws Exception {
                  if (state() != State.STARTING) {
                    inGetNextSchedule.await();
                    Thread.yield();
                    throw new RuntimeException("boom");
                  }
                  return new Schedule(0, NANOSECONDS);
                }
              };
            }
          };
      service.startAsync().awaitRunning();
      inGetNextSchedule.await();
      service.stopAsync();
    }
  }

  public void testBig() throws Exception {
    TestAbstractScheduledCustomService service =
        new TestAbstractScheduledCustomService() {
          @Override
          protected Scheduler scheduler() {
            return new AbstractScheduledService.CustomScheduler() {
              @Override
              protected Schedule getNextSchedule() throws Exception {
                // Explicitly yield to increase the probability of a pathological scheduling.
                Thread.yield();
                return new Schedule(0, SECONDS);
              }
            };
          }
        };
    service.useBarriers = false;
    service.startAsync().awaitRunning();
    Thread.sleep(50);
    service.useBarriers = true;
    service.firstBarrier.await();
    int numIterations = service.numIterations.get();
    service.stopAsync();
    service.secondBarrier.await();
    service.awaitTerminated();
    assertEquals(numIterations, service.numIterations.get());
  }

  private static class TestAbstractScheduledCustomService extends AbstractScheduledService {
    final AtomicInteger numIterations = new AtomicInteger(0);
    volatile boolean useBarriers = true;
    final CyclicBarrier firstBarrier = new CyclicBarrier(2);
    final CyclicBarrier secondBarrier = new CyclicBarrier(2);

    @Override
    protected void runOneIteration() throws Exception {
      numIterations.incrementAndGet();
      if (useBarriers) {
        firstBarrier.await();
        secondBarrier.await();
      }
    }

    @Override
    protected ScheduledExecutorService executor() {
      // use a bunch of threads so that weird overlapping schedules are more likely to happen.
      return Executors.newScheduledThreadPool(10);
    }

    @Override
    protected Scheduler scheduler() {
      return new CustomScheduler() {
        @Override
        protected Schedule getNextSchedule() throws Exception {
          return new Schedule(DELAY, UNIT);
        }
      };
    }
  }

  public void testCustomSchedulerFailure() throws Exception {
    TestFailingCustomScheduledService service = new TestFailingCustomScheduledService();
    service.startAsync().awaitRunning();
    for (int i = 1; i < 4; i++) {
      service.firstBarrier.await();
      assertEquals(i, service.numIterations.get());
      service.secondBarrier.await();
    }
    Thread.sleep(1000);
    IllegalStateException e =
        assertThrows(
            IllegalStateException.class, () -> service.stopAsync().awaitTerminated(100, SECONDS));
    assertEquals(State.FAILED, service.state());
  }

  private static class TestFailingCustomScheduledService extends AbstractScheduledService {
    final AtomicInteger numIterations = new AtomicInteger(0);
    final CyclicBarrier firstBarrier = new CyclicBarrier(2);
    final CyclicBarrier secondBarrier = new CyclicBarrier(2);

    @Override
    protected void runOneIteration() throws Exception {
      numIterations.incrementAndGet();
      firstBarrier.await();
      secondBarrier.await();
    }

    @Override
    protected ScheduledExecutorService executor() {
      // use a bunch of threads so that weird overlapping schedules are more likely to happen.
      return Executors.newScheduledThreadPool(10);
    }

    @Override
    protected Scheduler scheduler() {
      return new CustomScheduler() {
        @Override
        protected Schedule getNextSchedule() throws Exception {
          if (numIterations.get() > 2) {
            throw new IllegalStateException("Failed");
          }
          return new Schedule(DELAY, UNIT);
        }
      };
    }
  }
}
