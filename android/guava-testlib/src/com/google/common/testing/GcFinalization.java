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

package com.google.common.testing;

import static java.util.concurrent.TimeUnit.SECONDS;

import com.google.common.annotations.Beta;
import com.google.common.annotations.GwtIncompatible;
import com.google.j2objc.annotations.J2ObjCIncompatible;
import java.lang.ref.WeakReference;
import java.util.Locale;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;

/**
 * Testing utilities relating to garbage collection finalization.
 *
 * <p>Use this class to test code triggered by <em>finalization</em>, that is, one of the following
 * actions taken by the java garbage collection system:
 *
 * <ul>
 *   <li>invoking the {@code finalize} methods of unreachable objects
 *   <li>clearing weak references to unreachable referents
 *   <li>enqueuing weak references to unreachable referents in their reference queue
 * </ul>
 *
 * <p>This class uses (possibly repeated) invocations of {@link java.lang.System#gc()} to cause
 * finalization to happen. However, a call to {@code System.gc()} is specified to be no more than a
 * hint, so this technique may fail at the whim of the JDK implementation, for example if a user
 * specified the JVM flag {@code -XX:+DisableExplicitGC}. But in practice, it works very well for
 * ordinary tests.
 *
 * <p>Failure of the expected event to occur within an implementation-defined "reasonable" time
 * period or an interrupt while waiting for the expected event will result in a {@link
 * RuntimeException}.
 *
 * <p>Here's an example that tests a {@code finalize} method:
 *
 * <pre>{@code
 * final CountDownLatch latch = new CountDownLatch(1);
 * Object x = new MyClass() {
 *   ...
 *   protected void finalize() { latch.countDown(); ... }
 * };
 * x = null;  // Hint to the JIT that x is stack-unreachable
 * GcFinalization.await(latch);
 * }</pre>
 *
 * <p>Here's an example that uses a user-defined finalization predicate:
 *
 * <pre>{@code
 * final WeakHashMap<Object, Object> map = new WeakHashMap<>();
 * map.put(new Object(), Boolean.TRUE);
 * GcFinalization.awaitDone(new FinalizationPredicate() {
 *   public boolean isDone() {
 *     return map.isEmpty();
 *   }
 * });
 * }</pre>
 *
 * <p>Even if your non-test code does not use finalization, you can use this class to test for
 * leaks, by ensuring that objects are no longer strongly referenced:
 *
 * <pre>{@code
 * // Helper function keeps victim stack-unreachable.
 * private WeakReference<Foo> fooWeakRef() {
 *   Foo x = ....;
 *   WeakReference<Foo> weakRef = new WeakReference<>(x);
 *   // ... use x ...
 *   x = null;  // Hint to the JIT that x is stack-unreachable
 *   return weakRef;
 * }
 * public void testFooLeak() {
 *   GcFinalization.awaitClear(fooWeakRef());
 * }
 * }</pre>
 *
 * <p>This class cannot currently be used to test soft references, since this class does not try to
 * create the memory pressure required to cause soft references to be cleared.
 *
 * <p>This class only provides testing utilities. It is not designed for direct use in production or
 * for benchmarking.
 *
 * @author mike nonemacher
 * @author Martin Buchholz
 * @since 11.0
 */
@Beta
@GwtIncompatible
@J2ObjCIncompatible // gc
public final class GcFinalization {
  private GcFinalization() {}

  /**
   * 10 seconds ought to be long enough for any object to be GC'ed and finalized. Unless we have a
   * gigantic heap, in which case we scale by heap size.
   */
  private static long timeoutSeconds() {
    // This class can make no hard guarantees.  The methods in this class are inherently flaky, but
    // we try hard to make them robust in practice.  We could additionally try to add in a system
    // load timeout multiplier.  Or we could try to use a CPU time bound instead of wall clock time
    // bound.  But these ideas are harder to implement.  We do not try to detect or handle a
    // user-specified -XX:+DisableExplicitGC.
    //
    // TODO(user): Consider using
    // java/lang/management/OperatingSystemMXBean.html#getSystemLoadAverage()
    //
    // TODO(user): Consider scaling by number of mutator threads,
    // e.g. using Thread#activeCount()
    return Math.max(10L, Runtime.getRuntime().totalMemory() / (32L * 1024L * 1024L));
  }

  /**
   * Waits until the given future {@linkplain Future#isDone is done}, invoking the garbage collector
   * as necessary to try to ensure that this will happen.
   *
   * @throws RuntimeException if timed out or interrupted while waiting
   */
  public static void awaitDone(Future<?> future) {
    if (future.isDone()) {
      return;
    }
    final long timeoutSeconds = timeoutSeconds();
    final long deadline = System.nanoTime() + SECONDS.toNanos(timeoutSeconds);
    do {
      System.runFinalization();
      if (future.isDone()) {
        return;
      }
      System.gc();
      try {
        future.get(1L, SECONDS);
        return;
      } catch (CancellationException | ExecutionException ok) {
        return;
      } catch (InterruptedException ie) {
        throw new RuntimeException("Unexpected interrupt while waiting for future", ie);
      } catch (TimeoutException tryHarder) {
        /* OK */
      }
    } while (System.nanoTime() - deadline < 0);
    throw formatRuntimeException("Future not done within %d second timeout", timeoutSeconds);
  }

  /**
   * Waits until the given predicate returns true, invoking the garbage collector as necessary to
   * try to ensure that this will happen.
   *
   * @throws RuntimeException if timed out or interrupted while waiting
   */
  public static void awaitDone(FinalizationPredicate predicate) {
    if (predicate.isDone()) {
      return;
    }
    final long timeoutSeconds = timeoutSeconds();
    final long deadline = System.nanoTime() + SECONDS.toNanos(timeoutSeconds);
    do {
      System.runFinalization();
      if (predicate.isDone()) {
        return;
      }
      CountDownLatch done = new CountDownLatch(1);
      createUnreachableLatchFinalizer(done);
      await(done);
      if (predicate.isDone()) {
        return;
      }
    } while (System.nanoTime() - deadline < 0);
    throw formatRuntimeException(
        "Predicate did not become true within %d second timeout", timeoutSeconds);
  }

  /**
   * Waits until the given latch has {@linkplain CountDownLatch#countDown counted down} to zero,
   * invoking the garbage collector as necessary to try to ensure that this will happen.
   *
   * @throws RuntimeException if timed out or interrupted while waiting
   */
  public static void await(CountDownLatch latch) {
    if (latch.getCount() == 0) {
      return;
    }
    final long timeoutSeconds = timeoutSeconds();
    final long deadline = System.nanoTime() + SECONDS.toNanos(timeoutSeconds);
    do {
      System.runFinalization();
      if (latch.getCount() == 0) {
        return;
      }
      System.gc();
      try {
        if (latch.await(1L, SECONDS)) {
          return;
        }
      } catch (InterruptedException ie) {
        throw new RuntimeException("Unexpected interrupt while waiting for latch", ie);
      }
    } while (System.nanoTime() - deadline < 0);
    throw formatRuntimeException(
        "Latch failed to count down within %d second timeout", timeoutSeconds);
  }

  /**
   * Creates a garbage object that counts down the latch in its finalizer. Sequestered into a
   * separate method to make it somewhat more likely to be unreachable.
   */
  private static void createUnreachableLatchFinalizer(final CountDownLatch latch) {
    new Object() {
      @Override
      protected void finalize() {
        latch.countDown();
      }
    };
  }

  /**
   * A predicate that is expected to return true subsequent to <em>finalization</em>, that is, one
   * of the following actions taken by the garbage collector when performing a full collection in
   * response to {@link System#gc()}:
   *
   * <ul>
   *   <li>invoking the {@code finalize} methods of unreachable objects
   *   <li>clearing weak references to unreachable referents
   *   <li>enqueuing weak references to unreachable referents in their reference queue
   * </ul>
   */
  public interface FinalizationPredicate {
    boolean isDone();
  }

  /**
   * Waits until the given weak reference is cleared, invoking the garbage collector as necessary to
   * try to ensure that this will happen.
   *
   * <p>This is a convenience method, equivalent to:
   *
   * <pre>{@code
   * awaitDone(new FinalizationPredicate() {
   *   public boolean isDone() {
   *     return ref.get() == null;
   *   }
   * });
   * }</pre>
   *
   * @throws RuntimeException if timed out or interrupted while waiting
   */
  public static void awaitClear(final WeakReference<?> ref) {
    awaitDone(
        new FinalizationPredicate() {
          @Override
          public boolean isDone() {
            return ref.get() == null;
          }
        });
  }

  /**
   * Tries to perform a "full" garbage collection cycle (including processing of weak references and
   * invocation of finalize methods) and waits for it to complete. Ensures that at least one weak
   * reference has been cleared and one {@code finalize} method has been run before this method
   * returns. This method may be useful when testing the garbage collection mechanism itself, or
   * inhibiting a spontaneous GC initiation in subsequent code.
   *
   * <p>In contrast, a plain call to {@link java.lang.System#gc()} does not ensure finalization
   * processing and may run concurrently, for example, if the JVM flag {@code
   * -XX:+ExplicitGCInvokesConcurrent} is used.
   *
   * <p>Whenever possible, it is preferable to test directly for some observable change resulting
   * from GC, as with {@link #awaitClear}. Because there are no guarantees for the order of GC
   * finalization processing, there may still be some unfinished work for the GC to do after this
   * method returns.
   *
   * <p>This method does not create any memory pressure as would be required to cause soft
   * references to be processed.
   *
   * @throws RuntimeException if timed out or interrupted while waiting
   * @since 12.0
   */
  public static void awaitFullGc() {
    final CountDownLatch finalizerRan = new CountDownLatch(1);
    WeakReference<Object> ref =
        new WeakReference<Object>(
            new Object() {
              @Override
              protected void finalize() {
                finalizerRan.countDown();
              }
            });

    await(finalizerRan);
    awaitClear(ref);

    // Hope to catch some stragglers queued up behind our finalizable object
    System.runFinalization();
  }

  private static RuntimeException formatRuntimeException(String format, Object... args) {
    return new RuntimeException(String.format(Locale.ROOT, format, args));
  }
}
