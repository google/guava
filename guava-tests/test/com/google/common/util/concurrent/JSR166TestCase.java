/*
 * Written by Doug Lea with assistance from members of JCP JSR-166
 * Expert Group and released to the public domain, as explained at
 * http://creativecommons.org/publicdomain/zero/1.0/
 * Other contributors include Andrew Wright, Jeffrey Hayes,
 * Pat Fisher, Mike Judd.
 */

/*
 * Source:
 * http://gee.cs.oswego.edu/cgi-bin/viewcvs.cgi/jsr166/src/test/tck/JSR166TestCase.java?revision=1.74
 * (We have made some trivial local modifications.)
 */

package com.google.common.util.concurrent;

import junit.framework.*;
import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.PropertyPermission;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.NANOSECONDS;
import java.security.CodeSource;
import java.security.Permission;
import java.security.PermissionCollection;
import java.security.Permissions;
import java.security.Policy;
import java.security.ProtectionDomain;
import java.security.SecurityPermission;

/**
 * Base class for JSR166 Junit TCK tests.  Defines some constants,
 * utility methods and classes, as well as a simple framework for
 * helping to make sure that assertions failing in generated threads
 * cause the associated test that generated them to itself fail (which
 * JUnit does not otherwise arrange).  The rules for creating such
 * tests are:
 *
 * <ol>
 *
 * <li> All assertions in code running in generated threads must use
 * the forms {@link #threadFail}, {@link #threadAssertTrue}, {@link
 * #threadAssertEquals}, or {@link #threadAssertNull}, (not
 * {@code fail}, {@code assertTrue}, etc.) It is OK (but not
 * particularly recommended) for other code to use these forms too.
 * Only the most typically used JUnit assertion methods are defined
 * this way, but enough to live with.</li>
 *
 * <li> If you override {@link #setUp} or {@link #tearDown}, make sure
 * to invoke {@code super.setUp} and {@code super.tearDown} within
 * them. These methods are used to clear and check for thread
 * assertion failures.</li>
 *
 * <li>All delays and timeouts must use one of the constants {@code
 * SHORT_DELAY_MS}, {@code SMALL_DELAY_MS}, {@code MEDIUM_DELAY_MS},
 * {@code LONG_DELAY_MS}. The idea here is that a SHORT is always
 * discriminable from zero time, and always allows enough time for the
 * small amounts of computation (creating a thread, calling a few
 * methods, etc) needed to reach a timeout point. Similarly, a SMALL
 * is always discriminable as larger than SHORT and smaller than
 * MEDIUM.  And so on. These constants are set to conservative values,
 * but even so, if there is ever any doubt, they can all be increased
 * in one spot to rerun tests on slower platforms.</li>
 *
 * <li> All threads generated must be joined inside each test case
 * method (or {@code fail} to do so) before returning from the
 * method. The {@code joinPool} method can be used to do this when
 * using Executors.</li>
 *
 * </ol>
 *
 * <p> <b>Other notes</b>
 * <ul>
 *
 * <li> Usually, there is one testcase method per JSR166 method
 * covering "normal" operation, and then as many exception-testing
 * methods as there are exceptions the method can throw. Sometimes
 * there are multiple tests per JSR166 method when the different
 * "normal" behaviors differ significantly. And sometimes testcases
 * cover multiple methods when they cannot be tested in
 * isolation.</li>
 *
 * <li> The documentation style for testcases is to provide as javadoc
 * a simple sentence or two describing the property that the testcase
 * method purports to test. The javadocs do not say anything about how
 * the property is tested. To find out, read the code.</li>
 *
 * <li> These tests are "conformance tests", and do not attempt to
 * test throughput, latency, scalability or other performance factors
 * (see the separate "jtreg" tests for a set intended to check these
 * for the most central aspects of functionality.) So, most tests use
 * the smallest sensible numbers of threads, collection sizes, etc
 * needed to check basic conformance.</li>
 *
 * <li>The test classes currently do not declare inclusion in
 * any particular package to simplify things for people integrating
 * them in TCK test suites.</li>
 *
 * <li> As a convenience, the {@code main} of this class (JSR166TestCase)
 * runs all JSR166 unit tests.</li>
 *
 * </ul>
 */
class JSR166TestCase extends TestCase {
    protected static final boolean expensiveTests =
        Boolean.getBoolean("jsr166.expensiveTests");

    /**
     * If true, report on stdout all "slow" tests, that is, ones that
     * take more than profileThreshold milliseconds to execute.
     */
    private static final boolean profileTests =
        Boolean.getBoolean("jsr166.profileTests");

    /**
     * The number of milliseconds that tests are permitted for
     * execution without being reported, when profileTests is set.
     */
    private static final long profileThreshold =
        Long.getLong("jsr166.profileThreshold", 100);

    @Override
    protected void runTest() throws Throwable {
        if (profileTests)
            runTestProfiled();
        else
            super.runTest();
    }

    protected void runTestProfiled() throws Throwable {
        long t0 = System.nanoTime();
        try {
            super.runTest();
        } finally {
            long elapsedMillis =
                (System.nanoTime() - t0) / (1000L * 1000L);
            if (elapsedMillis >= profileThreshold)
                System.out.printf("%n%s: %d%n", toString(), elapsedMillis);
        }
    }

    public static long SHORT_DELAY_MS;
    public static long SMALL_DELAY_MS;
    public static long MEDIUM_DELAY_MS;
    public static long LONG_DELAY_MS;

    /**
     * Returns the shortest timed delay. This could
     * be reimplemented to use for example a Property.
     */
    protected long getShortDelay() {
        return 50;
    }

    /**
     * Sets delays as multiples of SHORT_DELAY.
     */
    protected void setDelays() {
        SHORT_DELAY_MS = getShortDelay();
        SMALL_DELAY_MS  = SHORT_DELAY_MS * 5;
        MEDIUM_DELAY_MS = SHORT_DELAY_MS * 10;
        LONG_DELAY_MS   = SHORT_DELAY_MS * 200;
    }

    /**
     * The first exception encountered if any threadAssertXXX method fails.
     */
    private final AtomicReference<Throwable> threadFailure
        = new AtomicReference<Throwable>(null);

    /**
     * Records an exception so that it can be rethrown later in the test
     * harness thread, triggering a test case failure.  Only the first
     * failure is recorded; subsequent calls to this method from within
     * the same test have no effect.
     */
    public void threadRecordFailure(Throwable t) {
        threadFailure.compareAndSet(null, t);
    }

    @Override
    public void setUp() {
        setDelays();
    }

    /**
     * Triggers test case failure if any thread assertions have failed,
     * by rethrowing, in the test harness thread, any exception recorded
     * earlier by threadRecordFailure.
     */
    @Override
    public void tearDown() throws Exception {
        Throwable t = threadFailure.getAndSet(null);
        if (t != null) {
            if (t instanceof Error)
                throw (Error) t;
            else if (t instanceof RuntimeException)
                throw (RuntimeException) t;
            else if (t instanceof Exception)
                throw (Exception) t;
            else {
                AssertionFailedError afe =
                    new AssertionFailedError(t.toString());
                afe.initCause(t);
                throw afe;
            }
        }
    }

    /**
     * Just like fail(reason), but additionally recording (using
     * threadRecordFailure) any AssertionFailedError thrown, so that
     * the current testcase will fail.
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
     * Just like assertTrue(b), but additionally recording (using
     * threadRecordFailure) any AssertionFailedError thrown, so that
     * the current testcase will fail.
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
     * Just like assertFalse(b), but additionally recording (using
     * threadRecordFailure) any AssertionFailedError thrown, so that
     * the current testcase will fail.
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
     * Just like assertNull(x), but additionally recording (using
     * threadRecordFailure) any AssertionFailedError thrown, so that
     * the current testcase will fail.
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
     * Just like assertEquals(x, y), but additionally recording (using
     * threadRecordFailure) any AssertionFailedError thrown, so that
     * the current testcase will fail.
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
     * Just like assertEquals(x, y), but additionally recording (using
     * threadRecordFailure) any AssertionFailedError thrown, so that
     * the current testcase will fail.
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
     * Just like assertSame(x, y), but additionally recording (using
     * threadRecordFailure) any AssertionFailedError thrown, so that
     * the current testcase will fail.
     */
    public void threadAssertSame(Object x, Object y) {
        try {
            assertSame(x, y);
        } catch (AssertionFailedError t) {
            threadRecordFailure(t);
            throw t;
        }
    }

    /**
     * Calls threadFail with message "should throw exception".
     */
    public void threadShouldThrow() {
        threadFail("should throw exception");
    }

    /**
     * Calls threadFail with message "should throw" + exceptionName.
     */
    public void threadShouldThrow(String exceptionName) {
        threadFail("should throw " + exceptionName);
    }

    /**
     * Records the given exception using {@link #threadRecordFailure},
     * then rethrows the exception, wrapping it in an
     * AssertionFailedError if necessary.
     */
    public void threadUnexpectedException(Throwable t) {
        threadRecordFailure(t);
        t.printStackTrace();
        if (t instanceof RuntimeException)
            throw (RuntimeException) t;
        else if (t instanceof Error)
            throw (Error) t;
        else {
            AssertionFailedError afe =
                new AssertionFailedError("unexpected exception: " + t);
            t.initCause(t);
            throw afe;
        }
    }

    /**
     * Waits out termination of a thread pool or fails doing so.
     */
    public void joinPool(ExecutorService exec) {
        try {
            exec.shutdown();
            assertTrue("ExecutorService did not terminate in a timely manner",
                       exec.awaitTermination(2 * LONG_DELAY_MS, MILLISECONDS));
        } catch (SecurityException ok) {
            // Allowed in case test doesn't have privs
        } catch (InterruptedException ie) {
            fail("Unexpected InterruptedException");
        }
    }

    /**
     * Fails with message "should throw exception".
     */
    public void shouldThrow() {
        fail("Should throw exception");
    }

    /**
     * Fails with message "should throw " + exceptionName.
     */
    public void shouldThrow(String exceptionName) {
        fail("Should throw " + exceptionName);
    }

    /**
     * The number of elements to place in collections, arrays, etc.
     */
    public static final int SIZE = 20;

    // Some convenient Integer constants

    public static final Integer zero  = new Integer(0);
    public static final Integer one   = new Integer(1);
    public static final Integer two   = new Integer(2);
    public static final Integer three = new Integer(3);
    public static final Integer four  = new Integer(4);
    public static final Integer five  = new Integer(5);
    public static final Integer six   = new Integer(6);
    public static final Integer seven = new Integer(7);
    public static final Integer eight = new Integer(8);
    public static final Integer nine  = new Integer(9);
    public static final Integer m1  = new Integer(-1);
    public static final Integer m2  = new Integer(-2);
    public static final Integer m3  = new Integer(-3);
    public static final Integer m4  = new Integer(-4);
    public static final Integer m5  = new Integer(-5);
    public static final Integer m6  = new Integer(-6);
    public static final Integer m10 = new Integer(-10);

    /**
     * Runs Runnable r with a security policy that permits precisely
     * the specified permissions.  If there is no current security
     * manager, the runnable is run twice, both with and without a
     * security manager.  We require that any security manager permit
     * getPolicy/setPolicy.
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

    /**
     * Runs a runnable without any permissions.
     */
    public void runWithoutPermissions(Runnable r) {
        runWithPermissions(r);
    }

    /**
     * A security policy where new permissions can be dynamically added
     * or all cleared.
     */
    public static class AdjustablePolicy extends java.security.Policy {
        Permissions perms = new Permissions();
        AdjustablePolicy(Permission... permissions) {
            for (Permission permission : permissions)
                perms.add(permission);
        }
        void addPermission(Permission perm) { perms.add(perm); }
        void clearPermissions() { perms = new Permissions(); }
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

    /**
     * Returns a policy containing all the permissions we ever need.
     */
    public static Policy permissivePolicy() {
        return new AdjustablePolicy
            // Permissions j.u.c. needs directly
            (new RuntimePermission("modifyThread"),
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

    /**
     * Sleeps until the given time has elapsed.
     * Throws AssertionFailedError if interrupted.
     */
    void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ie) {
            AssertionFailedError afe =
                new AssertionFailedError("Unexpected InterruptedException");
            afe.initCause(ie);
            throw afe;
        }
    }

    /**
     * Sleeps until the timeout has elapsed, or interrupted.
     * Does <em>NOT</em> throw InterruptedException.
     */
    void sleepTillInterrupted(long timeoutMillis) {
        try {
            Thread.sleep(timeoutMillis);
        } catch (InterruptedException wakeup) {}
    }

    /**
     * Waits up to the specified number of milliseconds for the given
     * thread to enter a wait state: BLOCKED, WAITING, or TIMED_WAITING.
     */
    void waitForThreadToEnterWaitState(Thread thread, long timeoutMillis) {
        long timeoutNanos = timeoutMillis * 1000L * 1000L;
        long t0 = System.nanoTime();
        for (;;) {
            Thread.State s = thread.getState();
            if (s == Thread.State.BLOCKED ||
                s == Thread.State.WAITING ||
                s == Thread.State.TIMED_WAITING)
                return;
            else if (s == Thread.State.TERMINATED)
                fail("Unexpected thread termination");
            else if (System.nanoTime() - t0 > timeoutNanos) {
                threadAssertTrue(thread.isAlive());
                return;
            }
            Thread.yield();
        }
    }

    /**
     * Returns the number of milliseconds since time given by
     * startNanoTime, which must have been previously returned from a
     * call to {@link System#nanoTime()}.
     */
    long millisElapsedSince(long startNanoTime) {
        return NANOSECONDS.toMillis(System.nanoTime() - startNanoTime);
    }

    /**
     * Returns a new started daemon Thread running the given runnable.
     */
    Thread newStartedThread(Runnable runnable) {
        Thread t = new Thread(runnable);
        t.setDaemon(true);
        t.start();
        return t;
    }

    /**
     * Waits for the specified time (in milliseconds) for the thread
     * to terminate (using {@link Thread#join(long)}), else interrupts
     * the thread (in the hope that it may terminate later) and fails.
     */
    void awaitTermination(Thread t, long timeoutMillis) {
        try {
            t.join(timeoutMillis);
        } catch (InterruptedException ie) {
            threadUnexpectedException(ie);
        } finally {
            if (t.isAlive()) {
                t.interrupt();
                fail("Test timed out");
            }
        }
    }

    // Some convenient Runnable classes

    public abstract class CheckedRunnable implements Runnable {
        protected abstract void realRun() throws Throwable;

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

        public final void run() {
            try {
                realRun();
                threadShouldThrow(exceptionClass.getSimpleName());
            } catch (Throwable t) {
                if (! exceptionClass.isInstance(t))
                    threadUnexpectedException(t);
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
                if (! exceptionClass.isInstance(t))
                    threadUnexpectedException(t);
            }
        }
    }

    public abstract class CheckedInterruptedRunnable implements Runnable {
        protected abstract void realRun() throws Throwable;

        public final void run() {
            try {
                realRun();
                threadShouldThrow("InterruptedException");
            } catch (InterruptedException success) {
            } catch (Throwable t) {
                threadUnexpectedException(t);
            }
        }
    }

    public abstract class CheckedCallable<T> implements Callable<T> {
        protected abstract T realCall() throws Throwable;

        public final T call() {
            try {
                return realCall();
            } catch (Throwable t) {
                threadUnexpectedException(t);
                return null;
            }
        }
    }

    public abstract class CheckedInterruptedCallable<T>
        implements Callable<T> {
        protected abstract T realCall() throws Throwable;

        public final T call() {
            try {
                T result = realCall();
                threadShouldThrow("InterruptedException");
                return result;
            } catch (InterruptedException success) {
            } catch (Throwable t) {
                threadUnexpectedException(t);
            }
            return null;
        }
    }

    public static class NoOpRunnable implements Runnable {
        public void run() {}
    }

    public static class NoOpCallable implements Callable {
        public Object call() { return Boolean.TRUE; }
    }

    public static final String TEST_STRING = "a test string";

    public static class StringTask implements Callable<String> {
        public String call() { return TEST_STRING; }
    }

    public Callable<String> latchAwaitingStringTask(
        final CountDownLatch latch) {
        return new CheckedCallable<String>() {
            @Override
            protected String realCall() {
                try {
                    latch.await();
                } catch (InterruptedException quittingTime) {}
                return TEST_STRING;
            }};
    }

    public Runnable awaiter(final CountDownLatch latch) {
        return new CheckedRunnable() {
            @Override
            public void realRun() throws InterruptedException {
                latch.await();
            }};
    }

    public static class NPETask implements Callable<String> {
        public String call() { throw new NullPointerException(); }
    }

    public static class CallableOne implements Callable<Integer> {
        public Integer call() { return one; }
    }

    public class ShortRunnable extends CheckedRunnable {
        @Override
        protected void realRun() throws Throwable {
            Thread.sleep(SHORT_DELAY_MS);
        }
    }

    public class ShortInterruptedRunnable extends CheckedInterruptedRunnable {
        @Override
        protected void realRun() throws InterruptedException {
            Thread.sleep(SHORT_DELAY_MS);
        }
    }

    public class SmallRunnable extends CheckedRunnable {
        @Override
        protected void realRun() throws Throwable {
            Thread.sleep(SMALL_DELAY_MS);
        }
    }

    public class SmallPossiblyInterruptedRunnable extends CheckedRunnable {
        @Override
        protected void realRun() {
            try {
                Thread.sleep(SMALL_DELAY_MS);
            } catch (InterruptedException ok) {}
        }
    }

    public class SmallCallable extends CheckedCallable {
        @Override
        protected Object realCall() throws InterruptedException {
            Thread.sleep(SMALL_DELAY_MS);
            return Boolean.TRUE;
        }
    }

    public class MediumRunnable extends CheckedRunnable {
        @Override
        protected void realRun() throws Throwable {
            Thread.sleep(MEDIUM_DELAY_MS);
        }
    }

    public class MediumInterruptedRunnable extends CheckedInterruptedRunnable {
        @Override
        protected void realRun() throws InterruptedException {
            Thread.sleep(MEDIUM_DELAY_MS);
        }
    }

    public Runnable possiblyInterruptedRunnable(final long timeoutMillis) {
        return new CheckedRunnable() {
            @Override
            protected void realRun() {
                try {
                    Thread.sleep(timeoutMillis);
                } catch (InterruptedException ok) {}
            }};
    }

    public class MediumPossiblyInterruptedRunnable extends CheckedRunnable {
        @Override
        protected void realRun() {
            try {
                Thread.sleep(MEDIUM_DELAY_MS);
            } catch (InterruptedException ok) {}
        }
    }

    public class LongPossiblyInterruptedRunnable extends CheckedRunnable {
        @Override
        protected void realRun() {
            try {
                Thread.sleep(LONG_DELAY_MS);
            } catch (InterruptedException ok) {}
        }
    }

    /**
     * For use as ThreadFactory in constructors
     */
    public static class SimpleThreadFactory implements ThreadFactory {
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
                public boolean isDone() { return done; }
                public void run() {
                    try {
                        Thread.sleep(timeoutMillis);
                        done = true;
                    } catch (InterruptedException ok) {}
                }
            };
    }

    public static class TrackedShortRunnable implements Runnable {
        public volatile boolean done = false;
        public void run() {
            try {
                Thread.sleep(SHORT_DELAY_MS);
                done = true;
            } catch (InterruptedException ok) {}
        }
    }

    public static class TrackedSmallRunnable implements Runnable {
        public volatile boolean done = false;
        public void run() {
            try {
                Thread.sleep(SMALL_DELAY_MS);
                done = true;
            } catch (InterruptedException ok) {}
        }
    }

    public static class TrackedMediumRunnable implements Runnable {
        public volatile boolean done = false;
        public void run() {
            try {
                Thread.sleep(MEDIUM_DELAY_MS);
                done = true;
            } catch (InterruptedException ok) {}
        }
    }

    public static class TrackedLongRunnable implements Runnable {
        public volatile boolean done = false;
        public void run() {
            try {
                Thread.sleep(LONG_DELAY_MS);
                done = true;
            } catch (InterruptedException ok) {}
        }
    }

    public static class TrackedNoOpRunnable implements Runnable {
        public volatile boolean done = false;
        public void run() {
            done = true;
        }
    }

    public static class TrackedCallable implements Callable {
        public volatile boolean done = false;
        public Object call() {
            try {
                Thread.sleep(SMALL_DELAY_MS);
                done = true;
            } catch (InterruptedException ok) {}
            return Boolean.TRUE;
        }
    }

    /**
     * For use as RejectedExecutionHandler in constructors
     */
    public static class NoOpREHandler implements RejectedExecutionHandler {
        public void rejectedExecution(Runnable r,
                                      ThreadPoolExecutor executor) {}
    }

    /**
     * A CyclicBarrier that fails with AssertionFailedErrors instead
     * of throwing checked exceptions.
     */
    public class CheckedBarrier extends CyclicBarrier {
        public CheckedBarrier(int parties) { super(parties); }

        @Override
        public int await() {
            try {
                return super.await();
            } catch (Exception e) {
                AssertionFailedError afe =
                    new AssertionFailedError("Unexpected exception: " + e);
                afe.initCause(e);
                throw afe;
            }
        }
    }

    public void checkEmpty(BlockingQueue q) {
        try {
            assertTrue(q.isEmpty());
            assertEquals(0, q.size());
            assertNull(q.peek());
            assertNull(q.poll());
            assertNull(q.poll(0, MILLISECONDS));
            assertEquals(q.toString(), "[]");
            assertTrue(Arrays.equals(q.toArray(), new Object[0]));
            assertFalse(q.iterator().hasNext());
            try {
                q.element();
                shouldThrow();
            } catch (NoSuchElementException success) {}
            try {
                q.iterator().next();
                shouldThrow();
            } catch (NoSuchElementException success) {}
            try {
                q.remove();
                shouldThrow();
            } catch (NoSuchElementException success) {}
        } catch (InterruptedException ie) {
            threadUnexpectedException(ie);
        }
    }

}
