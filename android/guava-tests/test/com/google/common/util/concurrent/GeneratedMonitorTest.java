/*
 * Copyright (C) 2014 The Guava Authors
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

import static com.google.common.util.concurrent.Uninterruptibles.awaitUninterruptibly;

import com.google.common.base.CaseFormat;
import com.google.common.collect.ImmutableList;
import com.google.common.primitives.Ints;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Generated tests for {@link Monitor}.
 *
 * <p>This test class generates all of its own test cases in the {@link #suite()} method. Every
 * {@code enterXxx}, {@code tryEnterXxx}, and {@code waitForXxx} method of the {@code Monitor} class
 * is analyzed reflectively to determine appropriate test cases based on its signature. Additional
 * ad hoc test cases can be found in {@link SupplementalMonitorTest}.
 *
 * @author Justin T. Sampson
 */

public class GeneratedMonitorTest extends TestCase {

  public static TestSuite suite() {
    TestSuite suite = new TestSuite();

    Method[] methods = Monitor.class.getMethods();
    sortMethods(methods);
    for (Method method : methods) {
      if (isAnyEnter(method) || isWaitFor(method)) {
        validateMethod(method);
        addTests(suite, method);
      }
    }

    assertEquals(548, suite.testCount());

    return suite;
  }

  /** A typical timeout value we'll use in the tests. */
  private static final long SMALL_TIMEOUT_MILLIS = 10;

  /** How long to wait when determining that a thread is blocked if we expect it to be blocked. */
  private static final long EXPECTED_HANG_DELAY_MILLIS = 75;

  /**
   * How long to wait when determining that a thread is blocked if we DON'T expect it to be blocked.
   */
  private static final long UNEXPECTED_HANG_DELAY_MILLIS = 10000;

  /**
   * Various scenarios to be generated for each method under test. The actual scenario generation
   * (determining which scenarios are applicable to which methods and what the outcome should be)
   * takes place in {@link #addTests(TestSuite, Method)}.
   */
  private enum Scenario {
    SATISFIED_AND_UNOCCUPIED_BEFORE_ENTERING,
    UNSATISFIED_AND_UNOCCUPIED_BEFORE_ENTERING,
    SATISFIED_AND_OCCUPIED_BEFORE_ENTERING,
    SATISFIED_UNOCCUPIED_AND_INTERRUPTED_BEFORE_ENTERING,

    SATISFIED_BEFORE_WAITING,
    SATISFIED_WHILE_WAITING,
    SATISFIED_AND_INTERRUPTED_BEFORE_WAITING,
    UNSATISFIED_BEFORE_AND_WHILE_WAITING,
    UNSATISFIED_AND_INTERRUPTED_BEFORE_WAITING;

    @Override
    public String toString() {
      return CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, name());
    }
  }

  /** Timeout values to combine with each {@link Scenario}. */
  private enum Timeout {
    MIN(Long.MIN_VALUE, "-oo"),
    MINUS_SMALL(-SMALL_TIMEOUT_MILLIS, "-" + SMALL_TIMEOUT_MILLIS + "ms"),
    ZERO(0L, "0ms"),
    SMALL(SMALL_TIMEOUT_MILLIS, SMALL_TIMEOUT_MILLIS + "ms"),
    LARGE(UNEXPECTED_HANG_DELAY_MILLIS * 2, (2 * UNEXPECTED_HANG_DELAY_MILLIS) + "ms"),
    MAX(Long.MAX_VALUE, "+oo");

    final long millis;
    final String label;

    Timeout(long millis, String label) {
      this.millis = millis;
      this.label = label;
    }

    @Override
    public String toString() {
      return label;
    }
  }

  /** Convenient subsets of the {@link Timeout} enumeration for specifying scenario outcomes. */
  private enum TimeoutsToUse {
    ANY(Timeout.values()),
    PAST(Timeout.MIN, Timeout.MINUS_SMALL, Timeout.ZERO),
    FUTURE(Timeout.SMALL, Timeout.MAX),
    SMALL(Timeout.SMALL),
    FINITE(Timeout.MIN, Timeout.MINUS_SMALL, Timeout.ZERO, Timeout.SMALL),
    INFINITE(Timeout.LARGE, Timeout.MAX);

    final ImmutableList<Timeout> timeouts;

    TimeoutsToUse(Timeout... timeouts) {
      this.timeouts = ImmutableList.copyOf(timeouts);
    }
  }

  /** Possible outcomes of calling any of the methods under test. */
  private enum Outcome {

    /** The method returned normally and is either void or returned true. */
    SUCCESS,

    /** The method returned false. */
    FAILURE,

    /** The method threw an InterruptedException. */
    INTERRUPT,

    /** The method did not return or throw anything. */
    HANG;

    @Override
    public String toString() {
      return CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, name());
    }
  }

  /** Identifies all enterXxx and tryEnterXxx methods. */
  private static boolean isAnyEnter(Method method) {
    return method.getName().startsWith("enter") || method.getName().startsWith("tryEnter");
  }

  /** Identifies just tryEnterXxx methods (a subset of {@link #isAnyEnter}), which never block. */
  private static boolean isTryEnter(Method method) {
    return method.getName().startsWith("tryEnter");
  }

  /**
   * Identifies just enterIfXxx methods (a subset of {@link #isAnyEnter}), which are mostly like the
   * enterXxx methods but behave like tryEnterXxx in some scenarios.
   */
  private static boolean isEnterIf(Method method) {
    return method.getName().startsWith("enterIf");
  }

  /** Identifies all waitForXxx methods, which must be called while occupying the monitor. */
  private static boolean isWaitFor(Method method) {
    return method.getName().startsWith("waitFor");
  }

  /** Determines whether the given method takes a Guard as its first parameter. */
  private static boolean isGuarded(Method method) {
    Class<?>[] parameterTypes = method.getParameterTypes();
    return parameterTypes.length >= 1 && parameterTypes[0] == Monitor.Guard.class;
  }

  /** Determines whether the given method takes a time and unit as its last two parameters. */
  private static boolean isTimed(Method method) {
    Class<?>[] parameterTypes = method.getParameterTypes();
    return parameterTypes.length >= 2
        && parameterTypes[parameterTypes.length - 2] == long.class
        && parameterTypes[parameterTypes.length - 1] == TimeUnit.class;
  }

  /** Determines whether the given method returns a boolean value. */
  private static boolean isBoolean(Method method) {
    return method.getReturnType() == boolean.class;
  }

  /** Determines whether the given method can throw InterruptedException. */
  private static boolean isInterruptible(Method method) {
    return Arrays.asList(method.getExceptionTypes()).contains(InterruptedException.class);
  }

  /** Sorts the given methods primarily by name and secondarily by number of parameters. */
  private static void sortMethods(Method[] methods) {
    Arrays.sort(
        methods,
        new Comparator<Method>() {
          @Override
          public int compare(Method m1, Method m2) {
            int nameComparison = m1.getName().compareTo(m2.getName());
            if (nameComparison != 0) {
              return nameComparison;
            } else {
              return Ints.compare(m1.getParameterTypes().length, m2.getParameterTypes().length);
            }
          }
        });
  }

  /** Validates that the given method's signature meets all of our assumptions. */
  private static void validateMethod(Method method) {
    String desc = method.toString();

    assertTrue(desc, isAnyEnter(method) || isWaitFor(method));

    switch (method.getParameterTypes().length) {
      case 0:
        assertFalse(desc, isGuarded(method));
        assertFalse(desc, isTimed(method));
        break;
      case 1:
        assertTrue(desc, isGuarded(method));
        assertFalse(desc, isTimed(method));
        break;
      case 2:
        assertFalse(desc, isGuarded(method));
        assertTrue(desc, isTimed(method));
        break;
      case 3:
        assertTrue(desc, isGuarded(method));
        assertTrue(desc, isTimed(method));
        break;
      default:
        fail(desc);
    }

    if (method.getReturnType() == void.class) {
      assertFalse(desc, isBoolean(method));
    } else {
      assertTrue(desc, isBoolean(method));
    }

    switch (method.getExceptionTypes().length) {
      case 0:
        assertFalse(desc, isInterruptible(method));
        break;
      case 1:
        assertTrue(desc, isInterruptible(method));
        break;
      default:
        fail(desc);
    }

    if (isEnterIf(method)) {
      assertTrue(desc, isGuarded(method));
      assertTrue(desc, isBoolean(method));
    } else if (isTryEnter(method)) {
      assertFalse(desc, isTimed(method));
      assertTrue(desc, isBoolean(method));
      assertFalse(desc, isInterruptible(method));
    } else if (isWaitFor(method)) {
      assertTrue(desc, isGuarded(method));
      assertEquals(desc, isTimed(method), isBoolean(method));
    } else { // any other enterXxx method
      assertEquals(desc, isTimed(method), isBoolean(method));
    }
  }

  /** Generates all test cases appropriate for the given method. */
  private static void addTests(TestSuite suite, Method method) {
    if (isGuarded(method)) {
      for (boolean fair1 : new boolean[] {true, false}) {
        for (boolean fair2 : new boolean[] {true, false}) {
          suite.addTest(generateGuardWithWrongMonitorTestCase(method, fair1, fair2));
        }
      }
    }
    if (isAnyEnter(method)) {
      addTests(
          suite,
          method,
          Scenario.SATISFIED_AND_UNOCCUPIED_BEFORE_ENTERING,
          TimeoutsToUse.ANY,
          Outcome.SUCCESS);
      addTests(
          suite,
          method,
          Scenario.UNSATISFIED_AND_UNOCCUPIED_BEFORE_ENTERING,
          TimeoutsToUse.FINITE,
          isGuarded(method)
              ? (isBoolean(method) ? Outcome.FAILURE : Outcome.HANG)
              : Outcome.SUCCESS);
      addTests(
          suite,
          method,
          Scenario.UNSATISFIED_AND_UNOCCUPIED_BEFORE_ENTERING,
          TimeoutsToUse.INFINITE,
          isGuarded(method)
              ? (isTryEnter(method) || isEnterIf(method) ? Outcome.FAILURE : Outcome.HANG)
              : Outcome.SUCCESS);
      addTests(
          suite,
          method,
          Scenario.SATISFIED_AND_OCCUPIED_BEFORE_ENTERING,
          TimeoutsToUse.FINITE,
          isBoolean(method) ? Outcome.FAILURE : Outcome.HANG);
      addTests(
          suite,
          method,
          Scenario.SATISFIED_AND_OCCUPIED_BEFORE_ENTERING,
          TimeoutsToUse.INFINITE,
          isGuarded(method) ? Outcome.HANG : (isTryEnter(method) ? Outcome.FAILURE : Outcome.HANG));
      addTests(
          suite,
          method,
          Scenario.SATISFIED_UNOCCUPIED_AND_INTERRUPTED_BEFORE_ENTERING,
          TimeoutsToUse.ANY,
          isInterruptible(method) ? Outcome.INTERRUPT : Outcome.SUCCESS);
    } else { // any waitForXxx method
      suite.addTest(generateWaitForWhenNotOccupyingTestCase(method, true));
      suite.addTest(generateWaitForWhenNotOccupyingTestCase(method, false));
      addTests(
          suite, method, Scenario.SATISFIED_BEFORE_WAITING, TimeoutsToUse.ANY, Outcome.SUCCESS);
      addTests(
          suite, method, Scenario.SATISFIED_WHILE_WAITING, TimeoutsToUse.INFINITE, Outcome.SUCCESS);
      addTests(
          suite, method, Scenario.SATISFIED_WHILE_WAITING, TimeoutsToUse.PAST, Outcome.FAILURE);
      addTests(
          suite,
          method,
          Scenario.SATISFIED_AND_INTERRUPTED_BEFORE_WAITING,
          TimeoutsToUse.ANY,
          Outcome.SUCCESS);
      addTests(
          suite,
          method,
          Scenario.UNSATISFIED_BEFORE_AND_WHILE_WAITING,
          TimeoutsToUse.FINITE,
          Outcome.FAILURE);
      addTests(
          suite,
          method,
          Scenario.UNSATISFIED_BEFORE_AND_WHILE_WAITING,
          TimeoutsToUse.INFINITE,
          Outcome.HANG);
      addTests(
          suite,
          method,
          Scenario.UNSATISFIED_AND_INTERRUPTED_BEFORE_WAITING,
          TimeoutsToUse.PAST,
          // prefer responding to interrupt over timing out
          isInterruptible(method) ? Outcome.INTERRUPT : Outcome.FAILURE);
      addTests(
          suite,
          method,
          Scenario.UNSATISFIED_AND_INTERRUPTED_BEFORE_WAITING,
          TimeoutsToUse.SMALL,
          isInterruptible(method) ? Outcome.INTERRUPT : Outcome.FAILURE);
      addTests(
          suite,
          method,
          Scenario.UNSATISFIED_AND_INTERRUPTED_BEFORE_WAITING,
          TimeoutsToUse.INFINITE,
          isInterruptible(method) ? Outcome.INTERRUPT : Outcome.HANG);
    }
  }

  /**
   * Generates test cases for the given combination of scenario and timeouts. For methods that take
   * an explicit timeout value, all of the given timeoutsToUse result in individual test cases. For
   * methods that do not take an explicit timeout value, a single test case is generated only if the
   * implicit timeout of that method matches the given timeoutsToUse. For example, enter() is
   * treated like enter(MAX, MILLIS) and tryEnter() is treated like enter(0, MILLIS).
   */
  private static void addTests(
      TestSuite suite,
      Method method,
      Scenario scenario,
      TimeoutsToUse timeoutsToUse,
      Outcome expectedOutcome) {
    for (boolean fair : new boolean[] {true, false}) {
      if (isTimed(method)) {
        for (Timeout timeout : timeoutsToUse.timeouts) {
          suite.addTest(new GeneratedMonitorTest(method, scenario, fair, timeout, expectedOutcome));
        }
      } else {
        Timeout implicitTimeout = (isTryEnter(method) ? Timeout.ZERO : Timeout.MAX);
        if (timeoutsToUse.timeouts.contains(implicitTimeout)) {
          suite.addTest(new GeneratedMonitorTest(method, scenario, fair, null, expectedOutcome));
        }
      }
    }
  }

  /** A guard that encapsulates a simple, mutable boolean flag. */
  static class FlagGuard extends Monitor.Guard {

    private boolean satisfied;

    protected FlagGuard(Monitor monitor) {
      super(monitor);
    }

    @Override
    public boolean isSatisfied() {
      return satisfied;
    }

    public void setSatisfied(boolean satisfied) {
      this.satisfied = satisfied;
    }
  }

  private final Method method;
  private final Scenario scenario;
  private final Timeout timeout;
  private final Outcome expectedOutcome;
  private final Monitor monitor;
  private final FlagGuard guard;
  private final CountDownLatch tearDownLatch;
  private final CountDownLatch doingCallLatch;
  private final CountDownLatch callCompletedLatch;

  private GeneratedMonitorTest(
      Method method, Scenario scenario, boolean fair, Timeout timeout, Outcome expectedOutcome) {
    super(nameFor(method, scenario, fair, timeout, expectedOutcome));
    this.method = method;
    this.scenario = scenario;
    this.timeout = timeout;
    this.expectedOutcome = expectedOutcome;
    this.monitor = new Monitor(fair);
    this.guard = new FlagGuard(monitor);
    this.tearDownLatch = new CountDownLatch(1);
    this.doingCallLatch = new CountDownLatch(1);
    this.callCompletedLatch = new CountDownLatch(1);
  }

  private static String nameFor(
      Method method, Scenario scenario, boolean fair, Timeout timeout, Outcome expectedOutcome) {
    return String.format(
        Locale.ROOT,
        "%s%s(%s)/%s->%s",
        method.getName(),
        fair ? "(fair)" : "(nonfair)",
        (timeout == null) ? "untimed" : timeout,
        scenario,
        expectedOutcome);
  }

  @Override
  protected void runTest() throws Throwable {
    final Runnable runChosenTest =
        new Runnable() {
          @Override
          public void run() {
            runChosenTest();
          }
        };
    final FutureTask<Void> task = new FutureTask<>(runChosenTest, null);
    startThread(
        new Runnable() {
          @Override
          public void run() {
            task.run();
          }
        });
    awaitUninterruptibly(doingCallLatch);
    long hangDelayMillis =
        (expectedOutcome == Outcome.HANG)
            ? EXPECTED_HANG_DELAY_MILLIS
            : UNEXPECTED_HANG_DELAY_MILLIS;
    boolean hung =
        !awaitUninterruptibly(callCompletedLatch, hangDelayMillis, TimeUnit.MILLISECONDS);
    if (hung) {
      assertEquals(expectedOutcome, Outcome.HANG);
    } else {
      assertNull(task.get(UNEXPECTED_HANG_DELAY_MILLIS, TimeUnit.MILLISECONDS));
    }
  }

  @Override
  protected void tearDown() throws Exception {
    // We don't want to leave stray threads running after each test. At this point, every thread
    // launched by this test is either:
    //
    // (a) Blocked attempting to enter the monitor.
    // (b) Waiting for the single guard to become satisfied.
    // (c) Occupying the monitor and awaiting the tearDownLatch.
    //
    // Except for (c), every thread should occupy the monitor very briefly, and every thread leaves
    // the monitor with the guard satisfied. Therefore as soon as tearDownLatch is triggered, we
    // should be able to enter the monitor, and then we set the guard to satisfied for the benefit
    // of any remaining waiting threads.

    tearDownLatch.countDown();
    assertTrue(
        "Monitor still occupied in tearDown()",
        monitor.enter(UNEXPECTED_HANG_DELAY_MILLIS, TimeUnit.MILLISECONDS));
    try {
      guard.setSatisfied(true);
    } finally {
      monitor.leave();
    }
  }

  private void runChosenTest() {
    if (isAnyEnter(method)) {
      runEnterTest();
    } else {
      runWaitTest();
    }
  }

  private void runEnterTest() {
    assertFalse(Thread.currentThread().isInterrupted());
    assertFalse(monitor.isOccupiedByCurrentThread());

    doEnterScenarioSetUp();

    boolean interruptedBeforeCall = Thread.currentThread().isInterrupted();
    Outcome actualOutcome = doCall();
    boolean occupiedAfterCall = monitor.isOccupiedByCurrentThread();
    boolean interruptedAfterCall = Thread.currentThread().isInterrupted();

    if (occupiedAfterCall) {
      guard.setSatisfied(true);
      monitor.leave();
      assertFalse(monitor.isOccupiedByCurrentThread());
    }

    assertEquals(expectedOutcome, actualOutcome);
    assertEquals(expectedOutcome == Outcome.SUCCESS, occupiedAfterCall);
    assertEquals(
        interruptedBeforeCall && expectedOutcome != Outcome.INTERRUPT, interruptedAfterCall);
  }

  private void doEnterScenarioSetUp() {
    switch (scenario) {
      case SATISFIED_AND_UNOCCUPIED_BEFORE_ENTERING:
        enterSatisfyGuardAndLeaveInCurrentThread();
        break;
      case UNSATISFIED_AND_UNOCCUPIED_BEFORE_ENTERING:
        break;
      case SATISFIED_AND_OCCUPIED_BEFORE_ENTERING:
        enterSatisfyGuardAndLeaveInCurrentThread();
        enterAndRemainOccupyingInAnotherThread();
        break;
      case SATISFIED_UNOCCUPIED_AND_INTERRUPTED_BEFORE_ENTERING:
        enterSatisfyGuardAndLeaveInCurrentThread();
        Thread.currentThread().interrupt();
        break;
      default:
        throw new AssertionError("unsupported scenario: " + scenario);
    }
  }

  private void runWaitTest() {
    assertFalse(Thread.currentThread().isInterrupted());
    assertFalse(monitor.isOccupiedByCurrentThread());
    monitor.enter();
    try {
      assertTrue(monitor.isOccupiedByCurrentThread());

      doWaitScenarioSetUp();

      boolean interruptedBeforeCall = Thread.currentThread().isInterrupted();
      Outcome actualOutcome = doCall();
      boolean occupiedAfterCall = monitor.isOccupiedByCurrentThread();
      boolean interruptedAfterCall = Thread.currentThread().isInterrupted();

      assertEquals(expectedOutcome, actualOutcome);
      assertTrue(occupiedAfterCall);
      assertEquals(
          interruptedBeforeCall && expectedOutcome != Outcome.INTERRUPT, interruptedAfterCall);
    } finally {
      guard.setSatisfied(true);
      monitor.leave();
      assertFalse(monitor.isOccupiedByCurrentThread());
    }
  }

  private void doWaitScenarioSetUp() {
    switch (scenario) {
      case SATISFIED_BEFORE_WAITING:
        guard.setSatisfied(true);
        break;
      case SATISFIED_WHILE_WAITING:
        guard.setSatisfied(false);
        enterSatisfyGuardAndLeaveInAnotherThread(); // enter blocks until we call waitFor
        break;
      case UNSATISFIED_BEFORE_AND_WHILE_WAITING:
        guard.setSatisfied(false);
        break;
      case SATISFIED_AND_INTERRUPTED_BEFORE_WAITING:
        guard.setSatisfied(true);
        Thread.currentThread().interrupt();
        break;
      case UNSATISFIED_AND_INTERRUPTED_BEFORE_WAITING:
        guard.setSatisfied(false);
        Thread.currentThread().interrupt();
        break;
      default:
        throw new AssertionError("unsupported scenario: " + scenario);
    }
  }

  private Outcome doCall() {
    boolean guarded = isGuarded(method);
    boolean timed = isTimed(method);
    Object[] arguments = new Object[(guarded ? 1 : 0) + (timed ? 2 : 0)];
    if (guarded) {
      arguments[0] = guard;
    }
    if (timed) {
      arguments[arguments.length - 2] = timeout.millis;
      arguments[arguments.length - 1] = TimeUnit.MILLISECONDS;
    }
    try {
      Object result;
      doingCallLatch.countDown();
      try {
        result = method.invoke(monitor, arguments);
      } finally {
        callCompletedLatch.countDown();
      }
      if (result == null) {
        return Outcome.SUCCESS;
      } else if ((Boolean) result) {
        return Outcome.SUCCESS;
      } else {
        return Outcome.FAILURE;
      }
    } catch (InvocationTargetException targetException) {
      Throwable actualException = targetException.getTargetException();
      if (actualException instanceof InterruptedException) {
        return Outcome.INTERRUPT;
      } else {
        throw newAssertionError("unexpected exception", targetException);
      }
    } catch (IllegalAccessException e) {
      throw newAssertionError("unexpected exception", e);
    }
  }

  private void enterSatisfyGuardAndLeaveInCurrentThread() {
    monitor.enter();
    try {
      guard.setSatisfied(true);
    } finally {
      monitor.leave();
    }
  }

  private void enterSatisfyGuardAndLeaveInAnotherThread() {
    final CountDownLatch startedLatch = new CountDownLatch(1);
    startThread(
        new Runnable() {
          @Override
          public void run() {
            startedLatch.countDown();
            enterSatisfyGuardAndLeaveInCurrentThread();
          }
        });
    awaitUninterruptibly(startedLatch);
  }

  private void enterAndRemainOccupyingInAnotherThread() {
    final CountDownLatch enteredLatch = new CountDownLatch(1);
    startThread(
        new Runnable() {
          @Override
          public void run() {
            monitor.enter();
            try {
              enteredLatch.countDown();
              awaitUninterruptibly(tearDownLatch);
              guard.setSatisfied(true);
            } finally {
              monitor.leave();
            }
          }
        });
    awaitUninterruptibly(enteredLatch);
  }

  @CanIgnoreReturnValue
  static Thread startThread(Runnable runnable) {
    Thread thread = new Thread(runnable);
    thread.setDaemon(true);
    thread.start();
    return thread;
  }

  /**
   * Generates a test case verifying that calling any enterXxx, tryEnterXxx, or waitForXxx method
   * with a guard that doesn't match the monitor produces an IllegalMonitorStateException.
   */
  private static TestCase generateGuardWithWrongMonitorTestCase(
      final Method method, final boolean fair1, final boolean fair2) {
    final boolean timed = isTimed(method); // Not going to bother with all timeouts, just 0ms.
    return new TestCase(method.getName() + (timed ? "(0ms)" : "()") + "/WrongMonitor->IMSE") {
      @Override
      protected void runTest() throws Throwable {
        Monitor monitor1 = new Monitor(fair1);
        Monitor monitor2 = new Monitor(fair2);
        FlagGuard guard = new FlagGuard(monitor2);
        Object[] arguments =
            (timed ? new Object[] {guard, 0L, TimeUnit.MILLISECONDS} : new Object[] {guard});
        boolean occupyMonitor = isWaitFor(method);
        if (occupyMonitor) {
          // If we don't already occupy the monitor, we'll get an IMSE regardless of the guard (see
          // generateWaitForWhenNotOccupyingTestCase).
          monitor1.enter();
        }
        try {
          method.invoke(monitor1, arguments);
          fail("expected IllegalMonitorStateException");
        } catch (InvocationTargetException e) {
          assertEquals(IllegalMonitorStateException.class, e.getTargetException().getClass());
        } finally {
          if (occupyMonitor) {
            monitor1.leave();
          }
        }
      }
    };
  }

  /**
   * Generates a test case verifying that calling any waitForXxx method when not occupying the
   * monitor produces an IllegalMonitorStateException.
   */
  private static TestCase generateWaitForWhenNotOccupyingTestCase(
      final Method method, final boolean fair) {
    final boolean timed = isTimed(method); // Not going to bother with all timeouts, just 0ms.
    String testName =
        method.getName()
            + (fair ? "(fair)" : "(nonfair)")
            + (timed ? "(0ms)" : "()")
            + "/NotOccupying->IMSE";
    return new TestCase(testName) {
      @Override
      protected void runTest() throws Throwable {
        Monitor monitor = new Monitor(fair);
        FlagGuard guard = new FlagGuard(monitor);
        Object[] arguments =
            (timed ? new Object[] {guard, 0L, TimeUnit.MILLISECONDS} : new Object[] {guard});
        try {
          method.invoke(monitor, arguments);
          fail("expected IllegalMonitorStateException");
        } catch (InvocationTargetException e) {
          assertEquals(IllegalMonitorStateException.class, e.getTargetException().getClass());
        }
      }
    };
  }

  /** Alternative to AssertionError(String, Throwable), which doesn't exist in Java 1.6 */
  private static AssertionError newAssertionError(String message, Throwable cause) {
    AssertionError e = new AssertionError(message);
    e.initCause(cause);
    return e;
  }
}
