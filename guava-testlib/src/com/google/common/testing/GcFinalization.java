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

import static java.util.concurrent.TimeUnit.NANOSECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;

import com.google.common.annotations.Beta;

import java.lang.ref.WeakReference;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;

/**
 * Testing utilities relating to garbage collection finalization.
 *
 * <p>Use this class to test code triggered by <em>finalization</em>, that is, one of the
 * following actions taken by the java garbage collection system:
 *
 * <ul>
 * <li>invoking the {@code finalize} methods of unreachable objects
 * <li>clearing weak references to unreachable referents
 * <li>enqueuing weak references to unreachable referents in their reference queue
 * </ul>
 *
 * <p>This class uses (possibly repeated) invocations of {@link java.lang.System#gc()} to cause
 * finalization to happen.  However, a call to {@code System.gc()} is specified to be no more
 * than a hint, so this technique may fail at the whim of the JDK implementation, for example if
 * a user specified the JVM flag {@code -XX:+DisableExplicitGC}.  But in practice, it works very
 * well for ordinary tests.
 *
 * <p>Failure of the expected event to occur within an implementation-defined "reasonable" time
 * period will result in a {@link TimeoutException}.
 *
 * <p>Here's an example that tests a {@code finalize} method:
 *
 * <pre>   {@code
 *   final CountDownLatch latch = new CountDownLatch(1);
 *   Object x = new MyClass() {
 *     ...
 *     protected void finalize() { latch.countDown(); ... }
 *   };
 *   x = null;  // Hint to the JIT that x is unreachable
 *   GcFinalization.await(latch);
 * }</pre>
 *
 * <p>Here's an example that uses a user-defined finalization predicate:
 *
 * <pre>   {@code
 *   final WeakHashMap<Object, Object> map = new WeakHashMap<Object, Object>();
 *   map.put(new Object(), Boolean.TRUE);
 *   GcFinalization.awaitDone(new FinalizationPredicate() {
 *     public boolean isDone() {
 *       return map.isEmpty();
 *     }
 *   });
 * }</pre>
 *
 * <p>This class cannot currently be used to test soft references, since this class does not try to
 * create the memory pressure required to cause soft references to be cleared.
 *
 * @author schmoe@google.com (mike nonemacher)
 * @author martinrb@google.com (Martin Buchholz)
 * @since 11.0
 */
@Beta
public final class GcFinalization {
  private GcFinalization() {}

  /**
   * 10 seconds ought to be long enough for any object to be GC'ed and finalized.  Unless we have a
   * gigantic heap, in which case we scale by heap size.
   */
  private static long timeoutNanos() {
    // This class can make no hard guarantees.  The methods in this class are inherently flaky, but
    // we try hard to make them robust in practice.  We could additionally try to add in a system
    // load timeout multiplier.  Or we could try to use a CPU time bound instead of wall clock time
    // bound.  But these ideas are harder to implement.  We do not try to detect or handle a
    // user-specified -XX:+DisableExplicitGC.
    //
    // TODO: Consider using java/lang/management/OperatingSystemMXBean.html#getSystemLoadAverage()
    //
    // TODO: Consider scaling by number of mutator threads, e.g. using Thread#activeCount()
    long seconds = Math.max(10L, Runtime.getRuntime().totalMemory() / (32L * 1024L * 1024L));
    return NANOSECONDS.convert(seconds, SECONDS);
  }

  /**
   * Waits until the given future {@linkplain Future#isDone is done}, invoking the garbage
   * collector as necessary to try to ensure that this will happen.
   *
   * @throws InterruptedException if interrupted while waiting
   * @throws TimeoutException if timed out
   */
  public static void awaitDone(Future<?> future) throws InterruptedException, TimeoutException {
    if (future.isDone()) {
      return;
    }
    final long deadline = System.nanoTime() + timeoutNanos();
    do {
      System.runFinalization();
      if (future.isDone()) {
        return;
      }
      System.gc();
      try {
        future.get(1L, SECONDS);
        return;
      } catch (CancellationException ok) {
        return;
      } catch (ExecutionException ok) {
        return;
      } catch (TimeoutException tryHarder) {
        /* OK */
      }
    } while (System.nanoTime() - deadline < 0);
    throw new TimeoutException("Future not done within timeout");
  }

  /**
   * Waits until the given latch has {@linkplain CountDownLatch#countDown counted down} to zero,
   * invoking the garbage collector as necessary to try to ensure that this will happen.
   *
   * @throws InterruptedException if interrupted while waiting
   * @throws TimeoutException if timed out
   */
  public static void await(CountDownLatch latch) throws InterruptedException, TimeoutException {
    if (latch.getCount() == 0) {
      return;
    }
    final long deadline = System.nanoTime() + timeoutNanos();
    do {
      System.runFinalization();
      if (latch.getCount() == 0) {
        return;
      }
      System.gc();
      if (latch.await(1L, SECONDS)) {
        return;
      }
    } while (System.nanoTime() - deadline < 0);
    throw new TimeoutException("CountDownLatch failed to count down within timeout");
  }

  /**
   * Creates a garbage object that counts down the latch in its finalizer.  Sequestered into a
   * separate method to make it somewhat more likely to be unreachable.
   */
  private static void createUnreachableLatchFinalizer(final CountDownLatch latch) {
    new Object() { @Override protected void finalize() { latch.countDown(); }};
  }

  /**
   * A predicate that is expected to return true subsequent to <em>finalization</em>, that is, one
   * of the following actions taken by the garbage collector when performing a full collection in
   * response to {@link System#gc()}:
   *
   * <ul>
   * <li>invoking the {@code finalize} methods of unreachable objects
   * <li>clearing weak references to unreachable referents
   * <li>enqueuing weak references to unreachable referents in their reference queue
   * </ul>
   */
  public interface FinalizationPredicate {
    boolean isDone();
  }

  /**
   * Waits until the given predicate returns true, invoking the garbage collector as necessary to
   * try to ensure that this will happen.
   *
   * @throws InterruptedException if interrupted while waiting
   * @throws TimeoutException if timed out
   */
  public static void awaitDone(FinalizationPredicate predicate)
      throws InterruptedException, TimeoutException {
    if (predicate.isDone()) {
      return;
    }
    final long deadline = System.nanoTime() + timeoutNanos();
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
    throw new TimeoutException("Predicate did not become true within timeout");
  }

  /**
   * Waits until the given weak reference is cleared, invoking the garbage collector as necessary
   * to try to ensure that this will happen.
   *
   * <p>This is a convenience method, equivalent to:
   * <pre>   {@code
   *   awaitDone(new FinalizationPredicate() {
   *     public boolean isDone() {
   *       return ref.get() == null;
   *     }
   *   });
   * }</pre>
   *
   * @throws InterruptedException if interrupted while waiting
   * @throws TimeoutException if timed out
   */
  public static void awaitClear(final WeakReference<?> ref)
      throws InterruptedException, TimeoutException {
    awaitDone(new FinalizationPredicate() {
      public boolean isDone() {
        return ref.get() == null;
      }
    });
  }
}
