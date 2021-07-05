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

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.NANOSECONDS;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.security.CodeSource;
import java.security.Permission;
import java.security.PermissionCollection;
import java.security.Permissions;
import java.security.Policy;
import java.security.ProtectionDomain;
import java.security.SecurityPermission;
import java.util.Arrays;
import java.util.Date;
import java.util.NoSuchElementException;
import java.util.PropertyPermission;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import junit.framework.AssertionFailedError;
import junit.framework.TestCase;

/**
 * Base class for JSR166 Junit TCK tests. Defines some constants, utility methods and classes, as
 * well as a simple framework for helping to make sure that assertions failing in generated threads
 * cause the associated test that generated them to itself fail (which JUnit does not otherwise
 * arrange). The rules for creating such tests are:
 *
 * <ol>
 *   <li>All assertions in code running in generated threads must use the forms {@link #threadFail},
 *       {@link #threadAssertTrue}, {@link #threadAssertEquals}, or {@link #threadAssertNull}, (not
 *       {@code fail}, {@code assertTrue}, etc.) It is OK (but not particularly recommended) for
 *       other code to use these forms too. Only the most typically used JUnit assertion methods are
 *       defined this way, but enough to live with.
 *   <li>If you override {@link #setUp} or {@link #tearDown}, make sure to invoke {@code
 *       super.setUp} and {@code super.tearDown} within them. These methods are used to clear and
 *       check for thread assertion failures.
 *   <li>All delays and timeouts must use one of the constants {@code SHORT_DELAY_MS}, {@code
 *       SMALL_DELAY_MS}, {@code MEDIUM_DELAY_MS}, {@code LONG_DELAY_MS}. The idea here is that a
 *       SHORT is always discriminable from zero time, and always allows enough time for the small
 *       amounts of computation (creating a thread, calling a few methods, etc) needed to reach a
 *       timeout point. Similarly, a SMALL is always discriminable as larger than SHORT and smaller
 *       than MEDIUM. And so on. These constants are set to conservative values, but even so, if
 *       there is ever any doubt, they can all be increased in one spot to rerun tests on slower
 *       platforms.
 *   <li>All threads generated must be joined inside each test case method (or {@code fail} to do
 *       so) before returning from the method. The {@code joinPool} method can be used to do this
 *       when using Executors.
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
 *   <li>As a convenience, the {@code main} of this class (JSR166TestCase) runs all JSR166 unit
 *       tests.
 * </ul>
 */
abstract class JSR166TestCase extends TestCase {
  private static final boolean useSecurityManager = Boolean.getBoolean("jsr166.useSecurityManager");

  protected static final boolean expensiveTests = Boolean.getBoolean("jsr166.expensiveTests");

  /**
   * If true, report on stdout all "slow" tests, that is, ones that take more than profileThreshold
   * milliseconds to execute.
   */
  private static final boolean profileTests = Boolean.getBoolean("jsr166.profileTests");

  /**
   * The number of milliseconds that tests are permitted for execution without being reported, when
   * profileTests is set.
   */
  private static final long profileThreshold = Long.getLong("jsr166.profileThreshold", 100);

  @Override
  protected void runTest() throws Throwable {
    if (profileTests) runTestProfiled();
    else super.runTest();
  }

  protected void runTestProfiled() throws Throwable {
    long t0 = System.nanoTime();
    try {
      super.runTest();
    } finally {
      long elapsedMillis = (System.nanoTime() - t0) / (1000L * 1000L);
      if (elapsedMillis >= profileThreshold)
        System.out.printf("%n%s: %d%n", toString(), elapsedMillis);
    }
  }

  //     /**
  //      * Runs all JSR166 unit tests using junit.textui.TestRunner
  //      */
  //     public static void main(String[] args) {
  //         if (useSecurityManager) {
  //             System.err.println("Setting a permissive security manager");
  //             Policy.setPolicy(permissivePolicy());
  //             System.setSecurityManager(new SecurityManager());
  //         }
  //         int iters = (args.length == 0) ? 1 : Integer.parseInt(args[0]);

  //         Test s = suite();
  //         for (int i = 0; i < iters; ++i) {
  //             junit.textui.TestRunner.run(s);
  //             System.gc();
  //             System.runFinalization();
  //         }
  //         System.exit(0);
  //     }

  //     public static TestSuite newTestSuite(Object... suiteOrClasses) {
  //         TestSuite suite = new TestSuite();
  //         for (Object suiteOrClass : suiteOrClasses) {
  //             if (suiteOrClass instanceof TestSuite)
  //                 suite.addTest((TestSuite) suiteOrClass);
  //             else if (suiteOrClass instanceof Class)
  //                 suite.addTest(new TestSuite((Class<?>) suiteOrClass));
  //             else
  //                 throw new ClassCastException("not a test suite or class");
  //         }
  //         return suite;
  //     }

  //     /**
  //      * Collects all JSR166 unit tests as one suite.
  //      */
  //     public static Test suite() {
  //         return newTestSuite(
  //             ForkJoinPoolTest.suite(),
  //             ForkJoinTaskTest.suite(),
  //             RecursiveActionTest.suite(),
  //             RecursiveTaskTest.suite(),
  //             LinkedTransferQueueTest.suite(),
  //             PhaserTest.suite(),
  //             ThreadLocalRandomTest.suite(),
  //             AbstractExecutorServiceTest.suite(),
  //             AbstractQueueTest.suite(),
  //             AbstractQueuedSynchronizerTest.suite(),
  //             AbstractQueuedLongSynchronizerTest.suite(),
  //             ArrayBlockingQueueTest.suite(),
  //             ArrayDequeTest.suite(),
  //             AtomicBooleanTest.suite(),
  //             AtomicIntegerArrayTest.suite(),
  //             AtomicIntegerFieldUpdaterTest.suite(),
  //             AtomicIntegerTest.suite(),
  //             AtomicLongArrayTest.suite(),
  //             AtomicLongFieldUpdaterTest.suite(),
  //             AtomicLongTest.suite(),
  //             AtomicMarkableReferenceTest.suite(),
  //             AtomicReferenceArrayTest.suite(),
  //             AtomicReferenceFieldUpdaterTest.suite(),
  //             AtomicReferenceTest.suite(),
  //             AtomicStampedReferenceTest.suite(),
  //             ConcurrentHashMapTest.suite(),
  //             ConcurrentLinkedDequeTest.suite(),
  //             ConcurrentLinkedQueueTest.suite(),
  //             ConcurrentSkipListMapTest.suite(),
  //             ConcurrentSkipListSubMapTest.suite(),
  //             ConcurrentSkipListSetTest.suite(),
  //             ConcurrentSkipListSubSetTest.suite(),
  //             CopyOnWriteArrayListTest.suite(),
  //             CopyOnWriteArraySetTest.suite(),
  //             CountDownLatchTest.suite(),
  //             CyclicBarrierTest.suite(),
  //             DelayQueueTest.suite(),
  //             EntryTest.suite(),
  //             ExchangerTest.suite(),
  //             ExecutorsTest.suite(),
  //             ExecutorCompletionServiceTest.suite(),
  //             FutureTaskTest.suite(),
  //             LinkedBlockingDequeTest.suite(),
  //             LinkedBlockingQueueTest.suite(),
  //             LinkedListTest.suite(),
  //             LockSupportTest.suite(),
  //             PriorityBlockingQueueTest.suite(),
  //             PriorityQueueTest.suite(),
  //             ReentrantLockTest.suite(),
  //             ReentrantReadWriteLockTest.suite(),
  //             ScheduledExecutorTest.suite(),
  //             ScheduledExecutorSubclassTest.suite(),
  //             SemaphoreTest.suite(),
  //             SynchronousQueueTest.suite(),
  //             SystemTest.suite(),
  //             ThreadLocalTest.suite(),
  //             ThreadPoolExecutorTest.suite(),
  //             ThreadPoolExecutorSubclassTest.suite(),
  //             ThreadTest.suite(),
  //             TimeUnitTest.suite(),
  //             TreeMapTest.suite(),
  //             TreeSetTest.suite(),
  //             TreeSubMapTest.suite(),
  //             TreeSubSetTest.suite());
  //     }

  public static long SHORT_DELAY_MS;
  public static long SMALL_DELAY_MS;
  public static long MEDIUM_DELAY_MS;
  public static long LONG_DELAY_MS;

  /**
   * Returns the shortest timed delay. This could be reimplemented to use for example a Property.
   */
  protected long getShortDelay() {
    return 50;
  }

  /** Sets delays as multiples of SHORT_DELAY. */
  protected void setDelays() {
    SHORT_DELAY_MS = getShortDelay();
    SMALL_DELAY_MS = SHORT_DELAY_MS * 5;
    MEDIUM_DELAY_MS = SHORT_DELAY_MS * 10;
    LONG_DELAY_MS = SHORT_DELAY_MS * 200;
  }

  /**
   * Returns a timeout in milliseconds to be used in tests that verify that operations block or time
   * out.
   */
  long timeoutMillis() {
    return SHORT_DELAY_MS / 4;
  }

  /** Returns a new Date instance representing a time delayMillis milliseconds in the future. */
  Date delayedDate(long delayMillis) {
    return new Date(System.currentTimeMillis() + delayMillis);
  }

  /** The first exception encountered if any threadAssertXXX method fails. */
  private final AtomicReference<Throwable> threadFailure = new AtomicReference<>(null);

  /**
   * Records an exception so that it can be rethrown later in the test harness thread, triggering a
   * test case failure. Only the first failure is recorded; subsequent calls to this method from
   * within the same test have no effect.
   */
  public void threadRecordFailure(Throwable t) {
    threadFailure.compareAndSet(null, t);
  }

  @Override
  public void setUp() {
    setDelays();
  }

  /**
   * Extra checks that get done for all test cases.
   *
   * <p>Triggers test case failure if any thread assertions have failed, by rethrowing, in the test
   * harness thread, any exception recorded earlier by threadRecordFailure.
   *
   * <p>Triggers test case failure if interrupt status is set in the main thread.
   */
  @Override
  public void tearDown() throws Exception {
    Throwable t = threadFailure.getAndSet(null);
    if (t != null) {
      if (t instanceof Error) throw (Error) t;
      else if (t instanceof RuntimeException) throw (RuntimeException) t;
      else if (t instanceof Exception) throw (Exception) t;
      else {
        AssertionFailedError afe = new AssertionFailedError(t.toString());
        afe.initCause(t);
        throw afe;
      }
    }

    if (Thread.interrupted()) throw new AssertionFailedError("interrupt status set in main thread");
  }

  /**
   * Just like fail(reason), but additionally recording (using threadRecordFailure) any
   * AssertionFailedError thrown, so that the current testcase will fail.
   */
  public void threadFail(String reason) {
    try {
      fail(reason);
    } catch (AssertionFailedError t) {
      threadRecordFailure(t);
      fail(reason);
    }
  }

  /**
   * Just like assertTrue(b), but additionally recording (using threadRecordFailure) any
   * AssertionFailedError thrown, so that the current testcase will fail.
   */
  public void threadAssertTrue(boolean b) {
    try {
      assertTrue(b);
    } catch (AssertionFailedError t) {
      threadRecordFailure(t);
      throw t;
    }
  }

  /**
   * Just like assertFalse(b), but additionally recording (using threadRecordFailure) any
   * AssertionFailedError thrown, so that the current testcase will fail.
   */
  public void threadAssertFalse(boolean b) {
    try {
      assertFalse(b);
    } catch (AssertionFailedError t) {
      threadRecordFailure(t);
      throw t;
    }
  }

  /**
   * Just like assertNull(x), but additionally recording (using threadRecordFailure) any
   * AssertionFailedError thrown, so that the current testcase will fail.
   */
  public void threadAssertNull(Object x) {
    try {
      assertNull(x);
    } catch (AssertionFailedError t) {
      threadRecordFailure(t);
      throw t;
    }
  }

  /**
   * Just like assertEquals(x, y), but additionally recording (using threadRecordFailure) any
   * AssertionFailedError thrown, so that the current testcase will fail.
   */
  public void threadAssertEquals(long x, long y) {
    try {
      assertEquals(x, y);
    } catch (AssertionFailedError t) {
      threadRecordFailure(t);
      throw t;
    }
  }

  /**
   * Just like assertEquals(x, y), but additionally recording (using threadRecordFailure) any
   * AssertionFailedError thrown, so that the current testcase will fail.
   */
  public void threadAssertEquals(Object x, Object y) {
    try {
      assertEquals(x, y);
    } catch (AssertionFailedError t) {
      threadRecordFailure(t);
      throw t;
    } catch (Throwable t) {
      threadUnexpectedException(t);
    }
  }

  /**
   * Just like assertSame(x, y), but additionally recording (using threadRecordFailure) any
   * AssertionFailedError thrown, so that the current testcase will fail.
   */
  public void threadAssertSame(Object x, Object y) {
    try {
      assertSame(x, y);
    } catch (AssertionFailedError t) {
      threadRecordFailure(t);
      throw t;
    }
  }

  /** Calls threadFail with message "should throw exception". */
  public void threadShouldThrow() {
    threadFail("should throw exception");
  }

  /** Calls threadFail with message "should throw" + exceptionName. */
  public void threadShouldThrow(String exceptionName) {
    threadFail("should throw " + exceptionName);
  }

  /**
   * Records the given exception using {@link #threadRecordFailure}, then rethrows the exception,
   * wrapping it in an AssertionFailedError if necessary.
   */
  public void threadUnexpectedException(Throwable t) {
    threadRecordFailure(t);
    t.printStackTrace();
    if (t instanceof RuntimeException) throw (RuntimeException) t;
    else if (t instanceof Error) throw (Error) t;
    else {
      AssertionFailedError afe = new AssertionFailedError("unexpected exception: " + t);
      afe.initCause(t);
      throw afe;
    }
  }

  /**
   * Delays, via Thread.sleep, for the given millisecond delay, but if the sleep is shorter than
   * specified, may re-sleep or yield until time elapses.
   */
  static void delay(long millis) throws InterruptedException {
    long startTime = System.nanoTime();
    long ns = millis * 1000 * 1000;
    for (; ; ) {
      if (millis > 0L) Thread.sleep(millis);
      else // too short to sleep
      Thread.yield();
      long d = ns - (System.nanoTime() - startTime);
      if (d > 0L) millis = d / (1000 * 1000);
      else break;
    }
  }

  /** Waits out termination of a thread pool or fails doing so. */
  void joinPool(ExecutorService exec) {
    try {
      exec.shutdown();
      assertTrue(
          "ExecutorService did not terminate in a timely manner",
          exec.awaitTermination(2 * LONG_DELAY_MS, MILLISECONDS));
    } catch (SecurityException ok) {
      // Allowed in case test doesn't have privs
    } catch (InterruptedException ie) {
      fail("Unexpected InterruptedException");
    }
  }

  /**
   * Checks that thread does not terminate within the default millisecond delay of {@code
   * timeoutMillis()}.
   */
  void assertThreadStaysAlive(Thread thread) {
    assertThreadStaysAlive(thread, timeoutMillis());
  }

  /** Checks that thread does not terminate within the given millisecond delay. */
  void assertThreadStaysAlive(Thread thread, long millis) {
    try {
      // No need to optimize the failing case via Thread.join.
      delay(millis);
      assertTrue(thread.isAlive());
    } catch (InterruptedException ie) {
      fail("Unexpected InterruptedException");
    }
  }

  /**
   * Checks that the threads do not terminate within the default millisecond delay of {@code
   * timeoutMillis()}.
   */
  void assertThreadsStayAlive(Thread... threads) {
    assertThreadsStayAlive(timeoutMillis(), threads);
  }

  /** Checks that the threads do not terminate within the given millisecond delay. */
  void assertThreadsStayAlive(long millis, Thread... threads) {
    try {
      // No need to optimize the failing case via Thread.join.
      delay(millis);
      for (Thread thread : threads) assertTrue(thread.isAlive());
    } catch (InterruptedException ie) {
      fail("Unexpected InterruptedException");
    }
  }

  /** Checks that future.get times out, with the default timeout of {@code timeoutMillis()}. */
  void assertFutureTimesOut(Future future) {
    assertFutureTimesOut(future, timeoutMillis());
  }

  /** Checks that future.get times out, with the given millisecond timeout. */
  void assertFutureTimesOut(Future future, long timeoutMillis) {
    long startTime = System.nanoTime();
    try {
      future.get(timeoutMillis, MILLISECONDS);
      shouldThrow();
    } catch (TimeoutException success) {
    } catch (Exception e) {
      threadUnexpectedException(e);
    } finally {
      future.cancel(true);
    }
    assertTrue(millisElapsedSince(startTime) >= timeoutMillis);
  }

  /** Fails with message "should throw exception". */
  public void shouldThrow() {
    fail("Should throw exception");
  }

  /** Fails with message "should throw " + exceptionName. */
  public void shouldThrow(String exceptionName) {
    fail("Should throw " + exceptionName);
  }

  /** The number of elements to place in collections, arrays, etc. */
  public static final int SIZE = 20;

  // Some convenient Integer constants

  public static final Integer zero = new Integer(0);
  public static final Integer one = new Integer(1);
  public static final Integer two = new Integer(2);
  public static final Integer three = new Integer(3);
  public static final Integer four = new Integer(4);
  public static final Integer five = new Integer(5);
  public static final Integer six = new Integer(6);
  public static final Integer seven = new Integer(7);
  public static final Integer eight = new Integer(8);
  public static final Integer nine = new Integer(9);
  public static final Integer m1 = new Integer(-1);
  public static final Integer m2 = new Integer(-2);
  public static final Integer m3 = new Integer(-3);
  public static final Integer m4 = new Integer(-4);
  public static final Integer m5 = new Integer(-5);
  public static final Integer m6 = new Integer(-6);
  public static final Integer m10 = new Integer(-10);

  /**
   * Runs Runnable r with a security policy that permits precisely the specified permissions. If
   * there is no current security manager, the runnable is run twice, both with and without a
   * security manager. We require that any security manager permit getPolicy/setPolicy.
   */
  public void runWithPermissions(Runnable r, Permission... permissions) {
    SecurityManager sm = System.getSecurityManager();
    if (sm == null) {
      r.run();
      Policy savedPolicy = Policy.getPolicy();
      try {
        Policy.setPolicy(permissivePolicy());
        System.setSecurityManager(new SecurityManager());
        runWithPermissions(r, permissions);
      } finally {
        System.setSecurityManager(null);
        Policy.setPolicy(savedPolicy);
      }
    } else {
      Policy savedPolicy = Policy.getPolicy();
      AdjustablePolicy policy = new AdjustablePolicy(permissions);
      Policy.setPolicy(policy);

      try {
        r.run();
      } finally {
        policy.addPermission(new SecurityPermission("setPolicy"));
        Policy.setPolicy(savedPolicy);
      }
    }
  }

  /** Runs a runnable without any permissions. */
  public void runWithoutPermissions(Runnable r) {
    runWithPermissions(r);
  }

  /** A security policy where new permissions can be dynamically added or all cleared. */
  public static class AdjustablePolicy extends java.security.Policy {
    Permissions perms = new Permissions();

    AdjustablePolicy(Permission... permissions) {
      for (Permission permission : permissions) perms.add(permission);
    }

    void addPermission(Permission perm) {
      perms.add(perm);
    }

    void clearPermissions() {
      perms = new Permissions();
    }

    @Override
    public PermissionCollection getPermissions(CodeSource cs) {
      return perms;
    }

    @Override
    public PermissionCollection getPermissions(ProtectionDomain pd) {
      return perms;
    }

    @Override
    public boolean implies(ProtectionDomain pd, Permission p) {
      return perms.implies(p);
    }

    @Override
    public void refresh() {}
  }

  /** Returns a policy containing all the permissions we ever need. */
  public static Policy permissivePolicy() {
    return new AdjustablePolicy
    // Permissions j.u.c. needs directly
    (
        new RuntimePermission("modifyThread"),
        new RuntimePermission("getClassLoader"),
        new RuntimePermission("setContextClassLoader"),
        // Permissions needed to change permissions!
        new SecurityPermission("getPolicy"),
        new SecurityPermission("setPolicy"),
        new RuntimePermission("setSecurityManager"),
        // Permissions needed by the junit test harness
        new RuntimePermission("accessDeclaredMembers"),
        new PropertyPermission("*", "read"),
        new java.io.FilePermission("<<ALL FILES>>", "read"));
  }

  /** Sleeps until the given time has elapsed. Throws AssertionFailedError if interrupted. */
  void sleep(long millis) {
    try {
      delay(millis);
    } catch (InterruptedException ie) {
      AssertionFailedError afe = new AssertionFailedError("Unexpected InterruptedException");
      afe.initCause(ie);
      throw afe;
    }
  }

  /**
   * Spin-waits up to the specified number of milliseconds for the given thread to enter a wait
   * state: BLOCKED, WAITING, or TIMED_WAITING.
   */
  void waitForThreadToEnterWaitState(Thread thread, long timeoutMillis) {
    long startTime = System.nanoTime();
    for (; ; ) {
      Thread.State s = thread.getState();
      if (s == Thread.State.BLOCKED || s == Thread.State.WAITING || s == Thread.State.TIMED_WAITING)
        return;
      else if (s == Thread.State.TERMINATED) fail("Unexpected thread termination");
      else if (millisElapsedSince(startTime) > timeoutMillis) {
        threadAssertTrue(thread.isAlive());
        return;
      }
      Thread.yield();
    }
  }

  /**
   * Waits up to LONG_DELAY_MS for the given thread to enter a wait state: BLOCKED, WAITING, or
   * TIMED_WAITING.
   */
  void waitForThreadToEnterWaitState(Thread thread) {
    waitForThreadToEnterWaitState(thread, LONG_DELAY_MS);
  }

  /**
   * Returns the number of milliseconds since time given by startNanoTime, which must have been
   * previously returned from a call to {@link System#nanoTime()}.
   */
  long millisElapsedSince(long startNanoTime) {
    return NANOSECONDS.toMillis(System.nanoTime() - startNanoTime);
  }

  /** Returns a new started daemon Thread running the given runnable. */
  Thread newStartedThread(Runnable runnable) {
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
  void awaitTermination(Thread t, long timeoutMillis) {
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
  void awaitTermination(Thread t) {
    awaitTermination(t, LONG_DELAY_MS);
  }

  // Some convenient Runnable classes

  public abstract class CheckedRunnable implements Runnable {
    protected abstract void realRun() throws Throwable;

    @Override
    public final void run() {
      try {
        realRun();
      } catch (Throwable t) {
        threadUnexpectedException(t);
      }
    }
  }

  public abstract class RunnableShouldThrow implements Runnable {
    protected abstract void realRun() throws Throwable;

    final Class<?> exceptionClass;

    <T extends Throwable> RunnableShouldThrow(Class<T> exceptionClass) {
      this.exceptionClass = exceptionClass;
    }

    @Override
    public final void run() {
      try {
        realRun();
        threadShouldThrow(exceptionClass.getSimpleName());
      } catch (Throwable t) {
        if (!exceptionClass.isInstance(t)) threadUnexpectedException(t);
      }
    }
  }

  public abstract class ThreadShouldThrow extends Thread {
    protected abstract void realRun() throws Throwable;

    final Class<?> exceptionClass;

    <T extends Throwable> ThreadShouldThrow(Class<T> exceptionClass) {
      this.exceptionClass = exceptionClass;
    }

    @Override
    public final void run() {
      try {
        realRun();
        threadShouldThrow(exceptionClass.getSimpleName());
      } catch (Throwable t) {
        if (!exceptionClass.isInstance(t)) threadUnexpectedException(t);
      }
    }
  }

  public abstract class CheckedInterruptedRunnable implements Runnable {
    protected abstract void realRun() throws Throwable;

    @Override
    public final void run() {
      try {
        realRun();
        threadShouldThrow("InterruptedException");
      } catch (InterruptedException success) {
        threadAssertFalse(Thread.interrupted());
      } catch (Throwable t) {
        threadUnexpectedException(t);
      }
    }
  }

  public abstract class CheckedCallable<T> implements Callable<T> {
    protected abstract T realCall() throws Throwable;

    @Override
    public final T call() {
      try {
        return realCall();
      } catch (Throwable t) {
        threadUnexpectedException(t);
        return null;
      }
    }
  }

  public abstract class CheckedInterruptedCallable<T> implements Callable<T> {
    protected abstract T realCall() throws Throwable;

    @Override
    public final T call() {
      try {
        T result = realCall();
        threadShouldThrow("InterruptedException");
        return result;
      } catch (InterruptedException success) {
        threadAssertFalse(Thread.interrupted());
      } catch (Throwable t) {
        threadUnexpectedException(t);
      }
      return null;
    }
  }

  public static class NoOpRunnable implements Runnable {
    @Override
    public void run() {}
  }

  public static class NoOpCallable implements Callable {
    @Override
    public Object call() {
      return Boolean.TRUE;
    }
  }

  public static final String TEST_STRING = "a test string";

  public static class StringTask implements Callable<String> {
    @Override
    public String call() {
      return TEST_STRING;
    }
  }

  public Callable<String> latchAwaitingStringTask(final CountDownLatch latch) {
    return new CheckedCallable<String>() {
      @Override
      protected String realCall() {
        try {
          latch.await();
        } catch (InterruptedException quittingTime) {
        }
        return TEST_STRING;
      }
    };
  }

  public Runnable awaiter(final CountDownLatch latch) {
    return new CheckedRunnable() {
      @Override
      public void realRun() throws InterruptedException {
        await(latch);
      }
    };
  }

  public void await(CountDownLatch latch) {
    try {
      assertTrue(latch.await(LONG_DELAY_MS, MILLISECONDS));
    } catch (Throwable t) {
      threadUnexpectedException(t);
    }
  }

  public void await(Semaphore semaphore) {
    try {
      assertTrue(semaphore.tryAcquire(LONG_DELAY_MS, MILLISECONDS));
    } catch (Throwable t) {
      threadUnexpectedException(t);
    }
  }

  //     /**
  //      * Spin-waits up to LONG_DELAY_MS until flag becomes true.
  //      */
  //     public void await(AtomicBoolean flag) {
  //         await(flag, LONG_DELAY_MS);
  //     }

  //     /**
  //      * Spin-waits up to the specified timeout until flag becomes true.
  //      */
  //     public void await(AtomicBoolean flag, long timeoutMillis) {
  //         long startTime = System.nanoTime();
  //         while (!flag.get()) {
  //             if (millisElapsedSince(startTime) > timeoutMillis)
  //                 throw new AssertionFailedError("timed out");
  //             Thread.yield();
  //         }
  //     }

  public static class NPETask implements Callable<String> {
    @Override
    public String call() {
      throw new NullPointerException();
    }
  }

  public static class CallableOne implements Callable<Integer> {
    @Override
    public Integer call() {
      return one;
    }
  }

  public class ShortRunnable extends CheckedRunnable {
    @Override
    protected void realRun() throws Throwable {
      delay(SHORT_DELAY_MS);
    }
  }

  public class ShortInterruptedRunnable extends CheckedInterruptedRunnable {
    @Override
    protected void realRun() throws InterruptedException {
      delay(SHORT_DELAY_MS);
    }
  }

  public class SmallRunnable extends CheckedRunnable {
    @Override
    protected void realRun() throws Throwable {
      delay(SMALL_DELAY_MS);
    }
  }

  public class SmallPossiblyInterruptedRunnable extends CheckedRunnable {
    @Override
    protected void realRun() {
      try {
        delay(SMALL_DELAY_MS);
      } catch (InterruptedException ok) {
      }
    }
  }

  public class SmallCallable extends CheckedCallable {
    @Override
    protected Object realCall() throws InterruptedException {
      delay(SMALL_DELAY_MS);
      return Boolean.TRUE;
    }
  }

  public class MediumRunnable extends CheckedRunnable {
    @Override
    protected void realRun() throws Throwable {
      delay(MEDIUM_DELAY_MS);
    }
  }

  public class MediumInterruptedRunnable extends CheckedInterruptedRunnable {
    @Override
    protected void realRun() throws InterruptedException {
      delay(MEDIUM_DELAY_MS);
    }
  }

  public Runnable possiblyInterruptedRunnable(final long timeoutMillis) {
    return new CheckedRunnable() {
      @Override
      protected void realRun() {
        try {
          delay(timeoutMillis);
        } catch (InterruptedException ok) {
        }
      }
    };
  }

  public class MediumPossiblyInterruptedRunnable extends CheckedRunnable {
    @Override
    protected void realRun() {
      try {
        delay(MEDIUM_DELAY_MS);
      } catch (InterruptedException ok) {
      }
    }
  }

  public class LongPossiblyInterruptedRunnable extends CheckedRunnable {
    @Override
    protected void realRun() {
      try {
        delay(LONG_DELAY_MS);
      } catch (InterruptedException ok) {
      }
    }
  }

  /** For use as ThreadFactory in constructors */
  public static class SimpleThreadFactory implements ThreadFactory {
    @Override
    public Thread newThread(Runnable r) {
      return new Thread(r);
    }
  }

  public interface TrackedRunnable extends Runnable {
    boolean isDone();
  }

  public static TrackedRunnable trackedRunnable(final long timeoutMillis) {
    return new TrackedRunnable() {
      private volatile boolean done = false;

      @Override
      public boolean isDone() {
        return done;
      }

      @Override
      public void run() {
        try {
          delay(timeoutMillis);
          done = true;
        } catch (InterruptedException ok) {
        }
      }
    };
  }

  public static class TrackedShortRunnable implements Runnable {
    public volatile boolean done = false;

    @Override
    public void run() {
      try {
        delay(SHORT_DELAY_MS);
        done = true;
      } catch (InterruptedException ok) {
      }
    }
  }

  public static class TrackedSmallRunnable implements Runnable {
    public volatile boolean done = false;

    @Override
    public void run() {
      try {
        delay(SMALL_DELAY_MS);
        done = true;
      } catch (InterruptedException ok) {
      }
    }
  }

  public static class TrackedMediumRunnable implements Runnable {
    public volatile boolean done = false;

    @Override
    public void run() {
      try {
        delay(MEDIUM_DELAY_MS);
        done = true;
      } catch (InterruptedException ok) {
      }
    }
  }

  public static class TrackedLongRunnable implements Runnable {
    public volatile boolean done = false;

    @Override
    public void run() {
      try {
        delay(LONG_DELAY_MS);
        done = true;
      } catch (InterruptedException ok) {
      }
    }
  }

  public static class TrackedNoOpRunnable implements Runnable {
    public volatile boolean done = false;

    @Override
    public void run() {
      done = true;
    }
  }

  public static class TrackedCallable implements Callable {
    public volatile boolean done = false;

    @Override
    public Object call() {
      try {
        delay(SMALL_DELAY_MS);
        done = true;
      } catch (InterruptedException ok) {
      }
      return Boolean.TRUE;
    }
  }

  //     /**
  //      * Analog of CheckedRunnable for RecursiveAction
  //      */
  //     public abstract class CheckedRecursiveAction extends RecursiveAction {
  //         protected abstract void realCompute() throws Throwable;

  //         public final void compute() {
  //             try {
  //                 realCompute();
  //             } catch (Throwable t) {
  //                 threadUnexpectedException(t);
  //             }
  //         }
  //     }

  //     /**
  //      * Analog of CheckedCallable for RecursiveTask
  //      */
  //     public abstract class CheckedRecursiveTask<T> extends RecursiveTask<T> {
  //         protected abstract T realCompute() throws Throwable;

  //         public final T compute() {
  //             try {
  //                 return realCompute();
  //             } catch (Throwable t) {
  //                 threadUnexpectedException(t);
  //                 return null;
  //             }
  //         }
  //     }

  /** For use as RejectedExecutionHandler in constructors */
  public static class NoOpREHandler implements RejectedExecutionHandler {
    @Override
    public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {}
  }

  /**
   * A CyclicBarrier that uses timed await and fails with AssertionFailedErrors instead of throwing
   * checked exceptions.
   */
  public class CheckedBarrier extends CyclicBarrier {
    public CheckedBarrier(int parties) {
      super(parties);
    }

    @Override
    public int await() {
      try {
        return super.await(2 * LONG_DELAY_MS, MILLISECONDS);
      } catch (TimeoutException e) {
        throw new AssertionFailedError("timed out");
      } catch (Exception e) {
        AssertionFailedError afe = new AssertionFailedError("Unexpected exception: " + e);
        afe.initCause(e);
        throw afe;
      }
    }
  }

  void checkEmpty(BlockingQueue q) {
    try {
      assertTrue(q.isEmpty());
      assertEquals(0, q.size());
      assertNull(q.peek());
      assertNull(q.poll());
      assertNull(q.poll(0, MILLISECONDS));
      assertEquals("[]", q.toString());
      assertTrue(Arrays.equals(q.toArray(), new Object[0]));
      assertFalse(q.iterator().hasNext());
      try {
        q.element();
        shouldThrow();
      } catch (NoSuchElementException success) {
      }
      try {
        q.iterator().next();
        shouldThrow();
      } catch (NoSuchElementException success) {
      }
      try {
        q.remove();
        shouldThrow();
      } catch (NoSuchElementException success) {
      }
    } catch (InterruptedException ie) {
      threadUnexpectedException(ie);
    }
  }

  @SuppressWarnings("unchecked")
  <T> T serialClone(T o) {
    try {
      ByteArrayOutputStream bos = new ByteArrayOutputStream();
      ObjectOutputStream oos = new ObjectOutputStream(bos);
      oos.writeObject(o);
      oos.flush();
      oos.close();
      ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(bos.toByteArray()));
      T clone = (T) ois.readObject();
      assertSame(o.getClass(), clone.getClass());
      return clone;
    } catch (Throwable t) {
      threadUnexpectedException(t);
      return null;
    }
  }
}
