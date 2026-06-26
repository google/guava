/*
 * Written by Doug Lea with assistance from members of JCP JSR-166
 * Expert Group and released to the public domain, as explained at
 * http://creativecommons.org/publicdomain/zero/1.0/
 * Other contributors include Andrew Wright, Jeffrey Hayes,
 * Pat Fisher, Mike Judd.
 */

/*
 * Source:
 * http://gee.cs.oswego.edu/cgi-bin/viewcvs.cgi/jsr166/src/test/tck/JSR166TestCase.java?revision=1.90
 * (We have made some trivial local modifications (commented out
 * uncompilable code).)
 */

package com.google.common.util.concurrent;

import com.google.common.annotations.GwtIncompatible;
import com.google.common.annotations.J2ktIncompatible;
import java.util.concurrent.atomic.AtomicReference;
import junit.framework.AssertionFailedError;
import junit.framework.TestCase;
import org.jspecify.annotations.NullUnmarked;

/**
 * Base class for JSR166 Junit TCK tests. Defines some constants, utility methods and classes, as
 * well as a simple framework for helping to make sure that assertions failing in generated threads
 * cause the associated test that generated them to itself fail (which JUnit does not otherwise
 * arrange). The rules for creating such tests are:
 *
 * <ol>
 *   <li>All assertions in code running in generated threads must use the forms {@link
 *       #threadUnexpectedException} or {@link #threadRecordFailure}.
 *   <li>If you override {@link #tearDown}, make sure to invoke {@code super.tearDown} within it.
 *       This method is used to clear and check for thread assertion failures.
 *   <li>All threads generated must be joined inside each test case method (or {@code fail} to do
 *       so) before returning from the method.
 * </ol>
 *
 * <p><b>Other notes</b>
 *
 * <ul>
 *   <li>Usually, there is one testcase method per JSR166 method covering "normal" operation, and
 *       then as many exception-testing methods as there are exceptions the method can throw.
 *       Sometimes there are multiple tests per JSR166 method when the different "normal" behaviors
 *       differ significantly. And sometimes testcases cover multiple methods when they cannot be
 *       tested in isolation.
 *   <li>The documentation style for testcases is to provide as javadoc a simple sentence or two
 *       describing the property that the testcase method purports to test. The javadocs do not say
 *       anything about how the property is tested. To find out, read the code.
 *   <li>These tests are "conformance tests", and do not attempt to test throughput, latency,
 *       scalability or other performance factors (see the separate "jtreg" tests for a set intended
 *       to check these for the most central aspects of functionality.) So, most tests use the
 *       smallest sensible numbers of threads, collection sizes, etc needed to check basic
 *       conformance.
 *   <li>The test classes currently do not declare inclusion in any particular package to simplify
 *       things for people integrating them in TCK test suites.
 * </ul>
 */
@SuppressWarnings({
  // We call threadUnexpectedException, which does the right thing for errors.
  "AssertionFailureIgnored",
  // We're following the upstream naming to reduce diffs.
  "IdentifierName",
  "ConstantCaseForConstants",
})
@NullUnmarked
@GwtIncompatible
@J2ktIncompatible
abstract class JSR166TestCase extends TestCase {
  private static final long LONG_DELAY_MS = 10000;

  /** The first exception encountered if any threadAssertXXX method fails. */
  private final AtomicReference<Throwable> threadFailure = new AtomicReference<>(null);

  /**
   * Records an exception so that it can be rethrown later in the test harness thread, triggering a
   * test case failure. Only the first failure is recorded; subsequent calls to this method from
   * within the same test have no effect.
   */
  final void threadRecordFailure(Throwable t) {
    threadFailure.compareAndSet(null, t);
  }

  /**
   * Extra checks that get done for all test cases.
   *
   * <p>Triggers test case failure if any thread assertions have failed, by rethrowing, in the test
   * harness thread, any exception recorded earlier by threadRecordFailure.
   *
   * <p>Triggers test case failure if interrupted status is set in the main thread.
   */
  @Override
  protected void tearDown() throws Exception {
    Throwable t = threadFailure.getAndSet(null);
    if (t != null) {
      if (t instanceof Error) {
        throw (Error) t;
      } else if (t instanceof RuntimeException) {
        throw (RuntimeException) t;
      } else if (t instanceof Exception) {
        throw (Exception) t;
      } else {
        AssertionFailedError afe = new AssertionFailedError(t.toString());
        afe.initCause(t);
        throw afe;
      }
    }

    if (Thread.interrupted()) {
      throw new AssertionFailedError("interrupted status set in main thread");
    }
  }

  /**
   * Records the given exception using {@link #threadRecordFailure}, then rethrows the exception,
   * wrapping it in an AssertionFailedError if necessary.
   */
  final void threadUnexpectedException(Throwable t) {
    threadRecordFailure(t);
    t.printStackTrace();
    if (t instanceof RuntimeException) {
      throw (RuntimeException) t;
    } else if (t instanceof Error) {
      throw (Error) t;
    } else {
      AssertionFailedError afe = new AssertionFailedError("unexpected exception: " + t);
      afe.initCause(t);
      throw afe;
    }
  }

  /** The number of elements to place in collections, arrays, etc. */
  static final int SIZE = 20;

  /** Returns a new started daemon Thread running the given runnable. */
  final Thread newStartedThread(Runnable runnable) {
    Thread t = new Thread(runnable);
    t.setDaemon(true);
    t.start();
    return t;
  }

  /**
   * Waits for the specified time (in milliseconds) for the thread to terminate (using {@link
   * Thread#join(long)}), else interrupts the thread (in the hope that it may terminate later) and
   * fails.
   */
  final void awaitTermination(Thread t, long timeoutMillis) {
    try {
      t.join(timeoutMillis);
    } catch (InterruptedException ie) {
      threadUnexpectedException(ie);
    } finally {
      if (t.getState() != Thread.State.TERMINATED) {
        t.interrupt();
        fail("Test timed out");
      }
    }
  }

  /**
   * Waits for LONG_DELAY_MS milliseconds for the thread to terminate (using {@link
   * Thread#join(long)}), else interrupts the thread (in the hope that it may terminate later) and
   * fails.
   */
  final void awaitTermination(Thread t) {
    awaitTermination(t, LONG_DELAY_MS);
  }
}
