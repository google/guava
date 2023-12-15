/*
 * Copyright (C) 2011 The Guava Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.common.util.concurrent;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Objects.requireNonNull;

import com.google.common.annotations.GwtIncompatible;
import com.google.common.annotations.J2ktIncompatible;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.MapMaker;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.j2objc.annotations.Weak;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;
import javax.annotation.CheckForNull;

/**
 * The {@code CycleDetectingLockFactory} creates {@link ReentrantLock} instances and {@link
 * ReentrantReadWriteLock} instances that detect potential deadlock by checking for cycles in lock
 * acquisition order.
 *
 * <p>Potential deadlocks detected when calling the {@code lock()}, {@code lockInterruptibly()}, or
 * {@code tryLock()} methods will result in the execution of the {@link Policy} specified when
 * creating the factory. The currently available policies are:
 *
 * <ul>
 *   <li>DISABLED
 *   <li>WARN
 *   <li>THROW
 * </ul>
 *
 * <p>The locks created by a factory instance will detect lock acquisition cycles with locks created
 * by other {@code CycleDetectingLockFactory} instances (except those with {@code Policy.DISABLED}).
 * A lock's behavior when a cycle is detected, however, is defined by the {@code Policy} of the
 * factory that created it. This allows detection of cycles across components while delegating
 * control over lock behavior to individual components.
 *
 * <p>Applications are encouraged to use a {@code CycleDetectingLockFactory} to create any locks for
 * which external/unmanaged code is executed while the lock is held. (See caveats under
 * <strong>Performance</strong>).
 *
 * <p><strong>Cycle Detection</strong>
 *
 * <p>Deadlocks can arise when locks are acquired in an order that forms a cycle. In a simple
 * example involving two locks and two threads, deadlock occurs when one thread acquires Lock A, and
 * then Lock B, while another thread acquires Lock B, and then Lock A:
 *
 * <pre>
 * Thread1: acquire(LockA) --X acquire(LockB)
 * Thread2: acquire(LockB) --X acquire(LockA)
 * </pre>
 *
 * <p>Neither thread will progress because each is waiting for the other. In more complex
 * applications, cycles can arise from interactions among more than 2 locks:
 *
 * <pre>
 * Thread1: acquire(LockA) --X acquire(LockB)
 * Thread2: acquire(LockB) --X acquire(LockC)
 * ...
 * ThreadN: acquire(LockN) --X acquire(LockA)
 * </pre>
 *
 * <p>The implementation detects cycles by constructing a directed graph in which each lock
 * represents a node and each edge represents an acquisition ordering between two locks.
 *
 * <ul>
 *   <li>Each lock adds (and removes) itself to/from a ThreadLocal Set of acquired locks when the
 *       Thread acquires its first hold (and releases its last remaining hold).
 *   <li>Before the lock is acquired, the lock is checked against the current set of acquired
 *       locks---to each of the acquired locks, an edge from the soon-to-be-acquired lock is either
 *       verified or created.
 *   <li>If a new edge needs to be created, the outgoing edges of the acquired locks are traversed
 *       to check for a cycle that reaches the lock to be acquired. If no cycle is detected, a new
 *       "safe" edge is created.
 *   <li>If a cycle is detected, an "unsafe" (cyclic) edge is created to represent a potential
 *       deadlock situation, and the appropriate Policy is executed.
 * </ul>
 *
 * <p>Note that detection of potential deadlock does not necessarily indicate that deadlock will
 * happen, as it is possible that higher level application logic prevents the cyclic lock
 * acquisition from occurring. One example of a false positive is:
 *
 * <pre>
 * LockA -&gt; LockB -&gt; LockC
 * LockA -&gt; LockC -&gt; LockB
 * </pre>
 *
 * <p><strong>ReadWriteLocks</strong>
 *
 * <p>While {@code ReadWriteLock} instances have different properties and can form cycles without
 * potential deadlock, this class treats {@code ReadWriteLock} instances as equivalent to
 * traditional exclusive locks. Although this increases the false positives that the locks detect
 * (i.e. cycles that will not actually result in deadlock), it simplifies the algorithm and
 * implementation considerably. The assumption is that a user of this factory wishes to eliminate
 * any cyclic acquisition ordering.
 *
 * <p><strong>Explicit Lock Acquisition Ordering</strong>
 *
 * <p>The {@link CycleDetectingLockFactory.WithExplicitOrdering} class can be used to enforce an
 * application-specific ordering in addition to performing general cycle detection.
 *
 * <p><strong>Garbage Collection</strong>
 *
 * <p>In order to allow proper garbage collection of unused locks, the edges of the lock graph are
 * weak references.
 *
 * <p><strong>Performance</strong>
 *
 * <p>The extra bookkeeping done by cycle detecting locks comes at some cost to performance.
 * Benchmarks (as of December 2011) show that:
 *
 * <ul>
 *   <li>for an unnested {@code lock()} and {@code unlock()}, a cycle detecting lock takes 38ns as
 *       opposed to the 24ns taken by a plain lock.
 *   <li>for nested locking, the cost increases with the depth of the nesting:
 *       <ul>
 *         <li>2 levels: average of 64ns per lock()/unlock()
 *         <li>3 levels: average of 77ns per lock()/unlock()
 *         <li>4 levels: average of 99ns per lock()/unlock()
 *         <li>5 levels: average of 103ns per lock()/unlock()
 *         <li>10 levels: average of 184ns per lock()/unlock()
 *         <li>20 levels: average of 393ns per lock()/unlock()
 *       </ul>
 * </ul>
 *
 * <p>As such, the CycleDetectingLockFactory may not be suitable for performance-critical
 * applications which involve tightly-looped or deeply-nested locking algorithms.
 *
 * @author Darick Tong
 * @since 13.0
 */
@J2ktIncompatible
@GwtIncompatible
@ElementTypesAreNonnullByDefault
public class CycleDetectingLockFactory {

  /**
   * Encapsulates the action to be taken when a potential deadlock is encountered. Clients can use
   * one of the predefined {@link Policies} or specify a custom implementation. Implementations must
   * be thread-safe.
   *
   * @since 13.0
   */
  public interface Policy {

    /**
     * Called when a potential deadlock is encountered. Implementations can throw the given {@code
     * exception} and/or execute other desired logic.
     *
     * <p>Note that the method will be called even upon an invocation of {@code tryLock()}. Although
     * {@code tryLock()} technically recovers from deadlock by eventually timing out, this behavior
     * is chosen based on the assumption that it is the application's wish to prohibit any cyclical
     * lock acquisitions.
     */
    void handlePotentialDeadlock(PotentialDeadlockException exception);
  }

  /**
   * Pre-defined {@link Policy} implementations.
   *
   * @since 13.0
   */
  public enum Policies implements Policy {
    /**
     * When potential deadlock is detected, this policy results in the throwing of the {@code
     * PotentialDeadlockException} indicating the potential deadlock, which includes stack traces
     * illustrating the cycle in lock acquisition order.
     */
    THROW {
      @Override
      public void handlePotentialDeadlock(PotentialDeadlockException e) {
        throw e;
      }
    },

    /**
     * When potential deadlock is detected, this policy results in the logging of a {@link
     * Level#SEVERE} message indicating the potential deadlock, which includes stack traces
     * illustrating the cycle in lock acquisition order.
     */
    WARN {
      @Override
      public void handlePotentialDeadlock(PotentialDeadlockException e) {
        logger.get().log(Level.SEVERE, "Detected potential deadlock", e);
      }
    },

    /**
     * Disables cycle detection. This option causes the factory to return unmodified lock
     * implementations provided by the JDK, and is provided to allow applications to easily
     * parameterize when cycle detection is enabled.
     *
     * <p>Note that locks created by a factory with this policy will <em>not</em> participate the
     * cycle detection performed by locks created by other factories.
     */
    DISABLED {
      @Override
      public void handlePotentialDeadlock(PotentialDeadlockException e) {}
    };
  }

  /** Creates a new factory with the specified policy. */
  public static CycleDetectingLockFactory newInstance(Policy policy) {
    return new CycleDetectingLockFactory(policy);
  }

  /** Equivalent to {@code newReentrantLock(lockName, false)}. */
  public ReentrantLock newReentrantLock(String lockName) {
    return newReentrantLock(lockName, false);
  }

  /**
   * Creates a {@link ReentrantLock} with the given fairness policy. The {@code lockName} is used in
   * the warning or exception output to help identify the locks involved in the detected deadlock.
   */
  public ReentrantLock newReentrantLock(String lockName, boolean fair) {
    return policy == Policies.DISABLED
        ? new ReentrantLock(fair)
        : new CycleDetectingReentrantLock(new LockGraphNode(lockName), fair);
  }

  /** Equivalent to {@code newReentrantReadWriteLock(lockName, false)}. */
  public ReentrantReadWriteLock newReentrantReadWriteLock(String lockName) {
    return newReentrantReadWriteLock(lockName, false);
  }

  /**
   * Creates a {@link ReentrantReadWriteLock} with the given fairness policy. The {@code lockName}
   * is used in the warning or exception output to help identify the locks involved in the detected
   * deadlock.
   */
  public ReentrantReadWriteLock newReentrantReadWriteLock(String lockName, boolean fair) {
    return policy == Policies.DISABLED
        ? new ReentrantReadWriteLock(fair)
        : new CycleDetectingReentrantReadWriteLock(new LockGraphNode(lockName), fair);
  }

  // A static mapping from an Enum type to its set of LockGraphNodes.
  private static final ConcurrentMap<
          Class<? extends Enum<?>>, Map<? extends Enum<?>, LockGraphNode>>
      lockGraphNodesPerType = new MapMaker().weakKeys().makeMap();

  /** Creates a {@code CycleDetectingLockFactory.WithExplicitOrdering<E>}. */
  public static <E extends Enum<E>> WithExplicitOrdering<E> newInstanceWithExplicitOrdering(
      Class<E> enumClass, Policy policy) {
    // createNodes maps each enumClass to a Map with the corresponding enum key
    // type.
    checkNotNull(enumClass);
    checkNotNull(policy);
    @SuppressWarnings("unchecked")
    Map<E, LockGraphNode> lockGraphNodes = (Map<E, LockGraphNode>) getOrCreateNodes(enumClass);
    return new WithExplicitOrdering<>(policy, lockGraphNodes);
  }

  @SuppressWarnings("unchecked")
  private static <E extends Enum<E>> Map<? extends E, LockGraphNode> getOrCreateNodes(
      Class<E> clazz) {
    Map<E, LockGraphNode> existing = (Map<E, LockGraphNode>) lockGraphNodesPerType.get(clazz);
    if (existing != null) {
      return existing;
    }
    Map<E, LockGraphNode> created = createNodes(clazz);
    existing = (Map<E, LockGraphNode>) lockGraphNodesPerType.putIfAbsent(clazz, created);
    return MoreObjects.firstNonNull(existing, created);
  }

  /**
   * For a given Enum type, creates an immutable map from each of the Enum's values to a
   * corresponding LockGraphNode, with the {@code allowedPriorLocks} and {@code
   * disallowedPriorLocks} prepopulated with nodes according to the natural ordering of the
   * associated Enum values.
   */
  @VisibleForTesting
  static <E extends Enum<E>> Map<E, LockGraphNode> createNodes(Class<E> clazz) {
    EnumMap<E, LockGraphNode> map = Maps.newEnumMap(clazz);
    E[] keys = clazz.getEnumConstants();
    int numKeys = keys.length;
    ArrayList<LockGraphNode> nodes = Lists.newArrayListWithCapacity(numKeys);
    // Create a LockGraphNode for each enum value.
    for (E key : keys) {
      LockGraphNode node = new LockGraphNode(getLockName(key));
      nodes.add(node);
      map.put(key, node);
    }
    // Pre-populate all allowedPriorLocks with nodes of smaller ordinal.
    for (int i = 1; i < numKeys; i++) {
      nodes.get(i).checkAcquiredLocks(Policies.THROW, nodes.subList(0, i));
    }
    // Pre-populate all disallowedPriorLocks with nodes of larger ordinal.
    for (int i = 0; i < numKeys - 1; i++) {
      nodes.get(i).checkAcquiredLocks(Policies.DISABLED, nodes.subList(i + 1, numKeys));
    }
    return Collections.unmodifiableMap(map);
  }

  /**
   * For the given Enum value {@code rank}, returns the value's {@code "EnumClass.name"}, which is
   * used in exception and warning output.
   */
  private static String getLockName(Enum<?> rank) {
    return rank.getDeclaringClass().getSimpleName() + "." + rank.name();
  }

  /**
   * A {@code CycleDetectingLockFactory.WithExplicitOrdering} provides the additional enforcement of
   * an application-specified ordering of lock acquisitions. The application defines the allowed
   * ordering with an {@code Enum} whose values each correspond to a lock type. The order in which
   * the values are declared dictates the allowed order of lock acquisition. In other words, locks
   * corresponding to smaller values of {@link Enum#ordinal()} should only be acquired before locks
   * with larger ordinals. Example:
   *
   * <pre>{@code
   * enum MyLockOrder {
   *   FIRST, SECOND, THIRD;
   * }
   *
   * CycleDetectingLockFactory.WithExplicitOrdering<MyLockOrder> factory =
   *   CycleDetectingLockFactory.newInstanceWithExplicitOrdering(Policies.THROW);
   *
   * Lock lock1 = factory.newReentrantLock(MyLockOrder.FIRST);
   * Lock lock2 = factory.newReentrantLock(MyLockOrder.SECOND);
   * Lock lock3 = factory.newReentrantLock(MyLockOrder.THIRD);
   *
   * lock1.lock();
   * lock3.lock();
   * lock2.lock();  // will throw an IllegalStateException
   * }</pre>
   *
   * <p>As with all locks created by instances of {@code CycleDetectingLockFactory} explicitly
   * ordered locks participate in general cycle detection with all other cycle detecting locks, and
   * a lock's behavior when detecting a cyclic lock acquisition is defined by the {@code Policy} of
   * the factory that created it.
   *
   * <p>Note, however, that although multiple locks can be created for a given Enum value, whether
   * it be through separate factory instances or through multiple calls to the same factory,
   * attempting to acquire multiple locks with the same Enum value (within the same thread) will
   * result in an IllegalStateException regardless of the factory's policy. For example:
   *
   * <pre>{@code
   * CycleDetectingLockFactory.WithExplicitOrdering<MyLockOrder> factory1 =
   *   CycleDetectingLockFactory.newInstanceWithExplicitOrdering(...);
   * CycleDetectingLockFactory.WithExplicitOrdering<MyLockOrder> factory2 =
   *   CycleDetectingLockFactory.newInstanceWithExplicitOrdering(...);
   *
   * Lock lockA = factory1.newReentrantLock(MyLockOrder.FIRST);
   * Lock lockB = factory1.newReentrantLock(MyLockOrder.FIRST);
   * Lock lockC = factory2.newReentrantLock(MyLockOrder.FIRST);
   *
   * lockA.lock();
   *
   * lockB.lock();  // will throw an IllegalStateException
   * lockC.lock();  // will throw an IllegalStateException
   *
   * lockA.lock();  // reentrant acquisition is okay
   * }</pre>
   *
   * <p>It is the responsibility of the application to ensure that multiple lock instances with the
   * same rank are never acquired in the same thread.
   *
   * @param <E> The Enum type representing the explicit lock ordering.
   * @since 13.0
   */
  public static final class WithExplicitOrdering<E extends Enum<E>>
      extends CycleDetectingLockFactory {

    private final Map<E, LockGraphNode> lockGraphNodes;

    @VisibleForTesting
    WithExplicitOrdering(Policy policy, Map<E, LockGraphNode> lockGraphNodes) {
      super(policy);
      this.lockGraphNodes = lockGraphNodes;
    }

    /** Equivalent to {@code newReentrantLock(rank, false)}. */
    public ReentrantLock newReentrantLock(E rank) {
      return newReentrantLock(rank, false);
    }

    /**
     * Creates a {@link ReentrantLock} with the given fairness policy and rank. The values returned
     * by {@link Enum#getDeclaringClass()} and {@link Enum#name()} are used to describe the lock in
     * warning or exception output.
     *
     * @throws IllegalStateException If the factory has already created a {@code Lock} with the
     *     specified rank.
     */
    public ReentrantLock newReentrantLock(E rank, boolean fair) {
      return policy == Policies.DISABLED
          ? new ReentrantLock(fair)
          // requireNonNull is safe because createNodes inserts an entry for every E.
          // (If the caller passes `null` for the `rank` parameter, this will throw, but that's OK.)
          : new CycleDetectingReentrantLock(requireNonNull(lockGraphNodes.get(rank)), fair);
    }

    /** Equivalent to {@code newReentrantReadWriteLock(rank, false)}. */
    public ReentrantReadWriteLock newReentrantReadWriteLock(E rank) {
      return newReentrantReadWriteLock(rank, false);
    }

    /**
     * Creates a {@link ReentrantReadWriteLock} with the given fairness policy and rank. The values
     * returned by {@link Enum#getDeclaringClass()} and {@link Enum#name()} are used to describe the
     * lock in warning or exception output.
     *
     * @throws IllegalStateException If the factory has already created a {@code Lock} with the
     *     specified rank.
     */
    public ReentrantReadWriteLock newReentrantReadWriteLock(E rank, boolean fair) {
      return policy == Policies.DISABLED
          ? new ReentrantReadWriteLock(fair)
          // requireNonNull is safe because createNodes inserts an entry for every E.
          // (If the caller passes `null` for the `rank` parameter, this will throw, but that's OK.)
          : new CycleDetectingReentrantReadWriteLock(
              requireNonNull(lockGraphNodes.get(rank)), fair);
    }
  }

  //////// Implementation /////////

  private static final LazyLogger logger = new LazyLogger(CycleDetectingLockFactory.class);

  final Policy policy;

  private CycleDetectingLockFactory(Policy policy) {
    this.policy = checkNotNull(policy);
  }

  /**
   * Tracks the currently acquired locks for each Thread, kept up to date by calls to {@link
   * #aboutToAcquire(CycleDetectingLock)} and {@link #lockStateChanged(CycleDetectingLock)}.
   */
  // This is logically a Set, but an ArrayList is used to minimize the amount
  // of allocation done on lock()/unlock().
  private static final ThreadLocal<ArrayList<LockGraphNode>> acquiredLocks =
      new ThreadLocal<ArrayList<LockGraphNode>>() {
        @Override
        protected ArrayList<LockGraphNode> initialValue() {
          return Lists.<LockGraphNode>newArrayListWithCapacity(3);
        }
      };

  /**
   * A Throwable used to record a stack trace that illustrates an example of a specific lock
   * acquisition ordering. The top of the stack trace is truncated such that it starts with the
   * acquisition of the lock in question, e.g.
   *
   * <pre>
   * com...ExampleStackTrace: LockB -&gt; LockC
   *   at com...CycleDetectingReentrantLock.lock(CycleDetectingLockFactory.java:443)
   *   at ...
   *   at ...
   *   at com...MyClass.someMethodThatAcquiresLockB(MyClass.java:123)
   * </pre>
   */
  private static class ExampleStackTrace extends IllegalStateException {

    static final StackTraceElement[] EMPTY_STACK_TRACE = new StackTraceElement[0];

    static final ImmutableSet<String> EXCLUDED_CLASS_NAMES =
        ImmutableSet.of(
            CycleDetectingLockFactory.class.getName(),
            ExampleStackTrace.class.getName(),
            LockGraphNode.class.getName());

    ExampleStackTrace(LockGraphNode node1, LockGraphNode node2) {
      super(node1.getLockName() + " -> " + node2.getLockName());
      StackTraceElement[] origStackTrace = getStackTrace();
      for (int i = 0, n = origStackTrace.length; i < n; i++) {
        if (WithExplicitOrdering.class.getName().equals(origStackTrace[i].getClassName())) {
          // For pre-populated disallowedPriorLocks edges, omit the stack trace.
          setStackTrace(EMPTY_STACK_TRACE);
          break;
        }
        if (!EXCLUDED_CLASS_NAMES.contains(origStackTrace[i].getClassName())) {
          setStackTrace(Arrays.copyOfRange(origStackTrace, i, n));
          break;
        }
      }
    }
  }

  /**
   * Represents a detected cycle in lock acquisition ordering. The exception includes a causal chain
   * of {@code ExampleStackTrace} instances to illustrate the cycle, e.g.
   *
   * <pre>
   * com....PotentialDeadlockException: Potential Deadlock from LockC -&gt; ReadWriteA
   *   at ...
   *   at ...
   * Caused by: com...ExampleStackTrace: LockB -&gt; LockC
   *   at ...
   *   at ...
   * Caused by: com...ExampleStackTrace: ReadWriteA -&gt; LockB
   *   at ...
   *   at ...
   * </pre>
   *
   * <p>Instances are logged for the {@code Policies.WARN}, and thrown for {@code Policies.THROW}.
   *
   * @since 13.0
   */
  public static final class PotentialDeadlockException extends ExampleStackTrace {

    private final ExampleStackTrace conflictingStackTrace;

    private PotentialDeadlockException(
        LockGraphNode node1, LockGraphNode node2, ExampleStackTrace conflictingStackTrace) {
      super(node1, node2);
      this.conflictingStackTrace = conflictingStackTrace;
      initCause(conflictingStackTrace);
    }

    public ExampleStackTrace getConflictingStackTrace() {
      return conflictingStackTrace;
    }

    /**
     * Appends the chain of messages from the {@code conflictingStackTrace} to the original {@code
     * message}.
     */
    @Override
    public String getMessage() {
      // requireNonNull is safe because ExampleStackTrace sets a non-null message.
      StringBuilder message = new StringBuilder(requireNonNull(super.getMessage()));
      for (Throwable t = conflictingStackTrace; t != null; t = t.getCause()) {
        message.append(", ").append(t.getMessage());
      }
      return message.toString();
    }
  }

  /**
   * Internal Lock implementations implement the {@code CycleDetectingLock} interface, allowing the
   * detection logic to treat all locks in the same manner.
   */
  private interface CycleDetectingLock {

    /** @return the {@link LockGraphNode} associated with this lock. */
    LockGraphNode getLockGraphNode();

    /** @return {@code true} if the current thread has acquired this lock. */
    boolean isAcquiredByCurrentThread();
  }

  /**
   * A {@code LockGraphNode} associated with each lock instance keeps track of the directed edges in
   * the lock acquisition graph.
   */
  private static class LockGraphNode {

    /**
     * The map tracking the locks that are known to be acquired before this lock, each associated
     * with an example stack trace. Locks are weakly keyed to allow proper garbage collection when
     * they are no longer referenced.
     */
    final Map<LockGraphNode, ExampleStackTrace> allowedPriorLocks =
        new MapMaker().weakKeys().makeMap();

    /**
     * The map tracking lock nodes that can cause a lock acquisition cycle if acquired before this
     * node.
     */
    final Map<LockGraphNode, PotentialDeadlockException> disallowedPriorLocks =
        new MapMaker().weakKeys().makeMap();

    final String lockName;

    LockGraphNode(String lockName) {
      this.lockName = Preconditions.checkNotNull(lockName);
    }

    String getLockName() {
      return lockName;
    }

    void checkAcquiredLocks(Policy policy, List<LockGraphNode> acquiredLocks) {
      for (LockGraphNode acquiredLock : acquiredLocks) {
        checkAcquiredLock(policy, acquiredLock);
      }
    }

    /**
     * Checks the acquisition-ordering between {@code this}, which is about to be acquired, and the
     * specified {@code acquiredLock}.
     *
     * <p>When this method returns, the {@code acquiredLock} should be in either the {@code
     * preAcquireLocks} map, for the case in which it is safe to acquire {@code this} after the
     * {@code acquiredLock}, or in the {@code disallowedPriorLocks} map, in which case it is not
     * safe.
     */
    void checkAcquiredLock(Policy policy, LockGraphNode acquiredLock) {
      // checkAcquiredLock() should never be invoked by a lock that has already
      // been acquired. For unordered locks, aboutToAcquire() ensures this by
      // checking isAcquiredByCurrentThread(). For ordered locks, however, this
      // can happen because multiple locks may share the same LockGraphNode. In
      // this situation, throw an IllegalStateException as defined by contract
      // described in the documentation of WithExplicitOrdering.
      Preconditions.checkState(
          this != acquiredLock,
          "Attempted to acquire multiple locks with the same rank %s",
          acquiredLock.getLockName());

      if (allowedPriorLocks.containsKey(acquiredLock)) {
        // The acquisition ordering from "acquiredLock" to "this" has already
        // been verified as safe. In a properly written application, this is
        // the common case.
        return;
      }
      PotentialDeadlockException previousDeadlockException = disallowedPriorLocks.get(acquiredLock);
      if (previousDeadlockException != null) {
        // Previously determined to be an unsafe lock acquisition.
        // Create a new PotentialDeadlockException with the same causal chain
        // (the example cycle) as that of the cached exception.
        PotentialDeadlockException exception =
            new PotentialDeadlockException(
                acquiredLock, this, previousDeadlockException.getConflictingStackTrace());
        policy.handlePotentialDeadlock(exception);
        return;
      }
      // Otherwise, it's the first time seeing this lock relationship. Look for
      // a path from the acquiredLock to this.
      Set<LockGraphNode> seen = Sets.newIdentityHashSet();
      ExampleStackTrace path = acquiredLock.findPathTo(this, seen);

      if (path == null) {
        // this can be safely acquired after the acquiredLock.
        //
        // Note that there is a race condition here which can result in missing
        // a cyclic edge: it's possible for two threads to simultaneous find
        // "safe" edges which together form a cycle. Preventing this race
        // condition efficiently without _introducing_ deadlock is probably
        // tricky. For now, just accept the race condition---missing a warning
        // now and then is still better than having no deadlock detection.
        allowedPriorLocks.put(acquiredLock, new ExampleStackTrace(acquiredLock, this));
      } else {
        // Unsafe acquisition order detected. Create and cache a
        // PotentialDeadlockException.
        PotentialDeadlockException exception =
            new PotentialDeadlockException(acquiredLock, this, path);
        disallowedPriorLocks.put(acquiredLock, exception);
        policy.handlePotentialDeadlock(exception);
      }
    }

    /**
     * Performs a depth-first traversal of the graph edges defined by each node's {@code
     * allowedPriorLocks} to find a path between {@code this} and the specified {@code lock}.
     *
     * @return If a path was found, a chained {@link ExampleStackTrace} illustrating the path to the
     *     {@code lock}, or {@code null} if no path was found.
     */
    @CheckForNull
    private ExampleStackTrace findPathTo(LockGraphNode node, Set<LockGraphNode> seen) {
      if (!seen.add(this)) {
        return null; // Already traversed this node.
      }
      ExampleStackTrace found = allowedPriorLocks.get(node);
      if (found != null) {
        return found; // Found a path ending at the node!
      }
      // Recurse the edges.
      for (Entry<LockGraphNode, ExampleStackTrace> entry : allowedPriorLocks.entrySet()) {
        LockGraphNode preAcquiredLock = entry.getKey();
        found = preAcquiredLock.findPathTo(node, seen);
        if (found != null) {
          // One of this node's allowedPriorLocks found a path. Prepend an
          // ExampleStackTrace(preAcquiredLock, this) to the returned chain of
          // ExampleStackTraces.
          ExampleStackTrace path = new ExampleStackTrace(preAcquiredLock, this);
          path.setStackTrace(entry.getValue().getStackTrace());
          path.initCause(found);
          return path;
        }
      }
      return null;
    }
  }

  /**
   * CycleDetectingLock implementations must call this method before attempting to acquire the lock.
   */
  private void aboutToAcquire(CycleDetectingLock lock) {
    if (!lock.isAcquiredByCurrentThread()) {
      // requireNonNull accommodates Android's @RecentlyNullable annotation on ThreadLocal.get
      ArrayList<LockGraphNode> acquiredLockList = requireNonNull(acquiredLocks.get());
      LockGraphNode node = lock.getLockGraphNode();
      node.checkAcquiredLocks(policy, acquiredLockList);
      acquiredLockList.add(node);
    }
  }

  /**
   * CycleDetectingLock implementations must call this method in a {@code finally} clause after any
   * attempt to change the lock state, including both lock and unlock attempts. Failure to do so can
   * result in corrupting the acquireLocks set.
   */
  private static void lockStateChanged(CycleDetectingLock lock) {
    if (!lock.isAcquiredByCurrentThread()) {
      // requireNonNull accommodates Android's @RecentlyNullable annotation on ThreadLocal.get
      ArrayList<LockGraphNode> acquiredLockList = requireNonNull(acquiredLocks.get());
      LockGraphNode node = lock.getLockGraphNode();
      // Iterate in reverse because locks are usually locked/unlocked in a
      // LIFO order.
      for (int i = acquiredLockList.size() - 1; i >= 0; i--) {
        if (acquiredLockList.get(i) == node) {
          acquiredLockList.remove(i);
          break;
        }
      }
    }
  }

  final class CycleDetectingReentrantLock extends ReentrantLock implements CycleDetectingLock {

    private final LockGraphNode lockGraphNode;

    private CycleDetectingReentrantLock(LockGraphNode lockGraphNode, boolean fair) {
      super(fair);
      this.lockGraphNode = Preconditions.checkNotNull(lockGraphNode);
    }

    ///// CycleDetectingLock methods. /////

    @Override
    public LockGraphNode getLockGraphNode() {
      return lockGraphNode;
    }

    @Override
    public boolean isAcquiredByCurrentThread() {
      return isHeldByCurrentThread();
    }

    ///// Overridden ReentrantLock methods. /////

    @Override
    public void lock() {
      aboutToAcquire(this);
      try {
        super.lock();
      } finally {
        lockStateChanged(this);
      }
    }

    @Override
    public void lockInterruptibly() throws InterruptedException {
      aboutToAcquire(this);
      try {
        super.lockInterruptibly();
      } finally {
        lockStateChanged(this);
      }
    }

    @Override
    public boolean tryLock() {
      aboutToAcquire(this);
      try {
        return super.tryLock();
      } finally {
        lockStateChanged(this);
      }
    }

    @Override
    public boolean tryLock(long timeout, TimeUnit unit) throws InterruptedException {
      aboutToAcquire(this);
      try {
        return super.tryLock(timeout, unit);
      } finally {
        lockStateChanged(this);
      }
    }

    @Override
    public void unlock() {
      try {
        super.unlock();
      } finally {
        lockStateChanged(this);
      }
    }
  }

  final class CycleDetectingReentrantReadWriteLock extends ReentrantReadWriteLock
      implements CycleDetectingLock {

    // These ReadLock/WriteLock implementations shadow those in the
    // ReentrantReadWriteLock superclass. They are simply wrappers around the
    // internal Sync object, so this is safe since the shadowed locks are never
    // exposed or used.
    private final CycleDetectingReentrantReadLock readLock;
    private final CycleDetectingReentrantWriteLock writeLock;

    private final LockGraphNode lockGraphNode;

    private CycleDetectingReentrantReadWriteLock(LockGraphNode lockGraphNode, boolean fair) {
      super(fair);
      this.readLock = new CycleDetectingReentrantReadLock(this);
      this.writeLock = new CycleDetectingReentrantWriteLock(this);
      this.lockGraphNode = Preconditions.checkNotNull(lockGraphNode);
    }

    ///// Overridden ReentrantReadWriteLock methods. /////

    @Override
    public ReadLock readLock() {
      return readLock;
    }

    @Override
    public WriteLock writeLock() {
      return writeLock;
    }

    ///// CycleDetectingLock methods. /////

    @Override
    public LockGraphNode getLockGraphNode() {
      return lockGraphNode;
    }

    @Override
    public boolean isAcquiredByCurrentThread() {
      return isWriteLockedByCurrentThread() || getReadHoldCount() > 0;
    }
  }

  private class CycleDetectingReentrantReadLock extends ReentrantReadWriteLock.ReadLock {

    @Weak final CycleDetectingReentrantReadWriteLock readWriteLock;

    CycleDetectingReentrantReadLock(CycleDetectingReentrantReadWriteLock readWriteLock) {
      super(readWriteLock);
      this.readWriteLock = readWriteLock;
    }

    @Override
    public void lock() {
      aboutToAcquire(readWriteLock);
      try {
        super.lock();
      } finally {
        lockStateChanged(readWriteLock);
      }
    }

    @Override
    public void lockInterruptibly() throws InterruptedException {
      aboutToAcquire(readWriteLock);
      try {
        super.lockInterruptibly();
      } finally {
        lockStateChanged(readWriteLock);
      }
    }

    @Override
    public boolean tryLock() {
      aboutToAcquire(readWriteLock);
      try {
        return super.tryLock();
      } finally {
        lockStateChanged(readWriteLock);
      }
    }

    @Override
    public boolean tryLock(long timeout, TimeUnit unit) throws InterruptedException {
      aboutToAcquire(readWriteLock);
      try {
        return super.tryLock(timeout, unit);
      } finally {
        lockStateChanged(readWriteLock);
      }
    }

    @Override
    public void unlock() {
      try {
        super.unlock();
      } finally {
        lockStateChanged(readWriteLock);
      }
    }
  }

  private class CycleDetectingReentrantWriteLock extends ReentrantReadWriteLock.WriteLock {

    @Weak final CycleDetectingReentrantReadWriteLock readWriteLock;

    CycleDetectingReentrantWriteLock(CycleDetectingReentrantReadWriteLock readWriteLock) {
      super(readWriteLock);
      this.readWriteLock = readWriteLock;
    }

    @Override
    public void lock() {
      aboutToAcquire(readWriteLock);
      try {
        super.lock();
      } finally {
        lockStateChanged(readWriteLock);
      }
    }

    @Override
    public void lockInterruptibly() throws InterruptedException {
      aboutToAcquire(readWriteLock);
      try {
        super.lockInterruptibly();
      } finally {
        lockStateChanged(readWriteLock);
      }
    }

    @Override
    public boolean tryLock() {
      aboutToAcquire(readWriteLock);
      try {
        return super.tryLock();
      } finally {
        lockStateChanged(readWriteLock);
      }
    }

    @Override
    public boolean tryLock(long timeout, TimeUnit unit) throws InterruptedException {
      aboutToAcquire(readWriteLock);
      try {
        return super.tryLock(timeout, unit);
      } finally {
        lockStateChanged(readWriteLock);
      }
    }

    @Override
    public void unlock() {
      try {
        super.unlock();
      } finally {
        lockStateChanged(readWriteLock);
      }
    }
  }
}
