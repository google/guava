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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

import com.google.common.annotations.Beta;
import com.google.common.base.Function;
import com.google.common.base.Objects;
import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Ordering;
import com.google.common.collect.Queues;
import com.google.common.util.concurrent.Service.State;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.concurrent.GuardedBy;
import javax.annotation.concurrent.Immutable;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * A manager for monitoring and controlling a set of {@link Service services}. This class provides
 * methods for {@linkplain #startAsync() starting}, {@linkplain #stopAsync() stopping} and
 * {@linkplain #servicesByState inspecting} a collection of {@linkplain Service services}.
 * Additionally, users can monitor state transitions with the {@link Listener listener} mechanism.
 *
 * <p>While it is recommended that service lifecycles be managed via this class, state transitions
 * initiated via other mechanisms do not impact the correctness of its methods. For example, if the
 * services are started by some mechanism besides {@link #startAsync}, the listeners will be invoked
 * when appropriate and {@link #awaitHealthy} will still work as expected.
 *
 * <p>Here is a simple example of how to use a {@link ServiceManager} to start a server.
 * <pre>   {@code
 * class Server {
 *   public static void main(String[] args) {
 *     Set<Service> services = ...;
 *     ServiceManager manager = new ServiceManager(services);
 *     manager.addListener(new Listener() {
 *         public void stopped() {}
 *         public void healthy() {
 *           // Services have been initialized and are healthy, start accepting requests...
 *         }
 *         public void failure(Service service) {
 *           // Something failed, at this point we could log it, notify a load balancer, or take
 *           // some other action.  For now we will just exit.
 *           System.exit(1);
 *         }
 *       },
 *       MoreExecutors.sameThreadExecutor());
 *
 *     Runtime.getRuntime().addShutdownHook(new Thread() {
 *       public void run() {
 *         // Give the services 5 seconds to stop to ensure that we are responsive to shutdown 
 *         // requests.
 *         try {
 *           manager.stopAsync().awaitStopped(5, TimeUnit.SECONDS);
 *         } catch (TimeoutException timeout) {
 *           // stopping timed out
 *         }
 *       }
 *     });
 *     manager.startAsync();  // start all the services asynchronously
 *   }
 * }}</pre>
 *
 * This class uses the ServiceManager's methods to start all of its services, to respond to service
 * failure and to ensure that when the JVM is shutting down all the services are stopped.
 *
 * @author Luke Sandberg
 * @since 14.0
 */
@Beta
@Singleton
public final class ServiceManager {
  private static final Logger logger = Logger.getLogger(ServiceManager.class.getName());
  
  /**
   * A listener for the aggregate state changes of the services that are under management. Users
   * that need to listen to more fine-grained events (such as when each particular
   * {@link Service service} starts, or terminates), should attach {@link Service.Listener service
   * listeners} to each individual service.
   * 
   * @author Luke Sandberg
   * @since 14.0
   */
  @Beta  // Should come out of Beta when ServiceManager does
  public static interface Listener {
    /** 
     * Called when the service initially becomes healthy.
     * 
     * <p>This will be called at most once after all the services have entered the 
     * {@linkplain State#RUNNING running} state. If any services fail during start up or 
     * {@linkplain State#FAILED fail}/{@linkplain State#TERMINATED terminate} before all other 
     * services have started {@linkplain State#RUNNING running} then this method will not be called.
     */
    void healthy();
    
    /** 
     * Called when the all of the component services have reached a terminal state, either 
     * {@linkplain State#TERMINATED terminated} or {@linkplain State#FAILED failed}.
     */
    void stopped();
    
    /** 
     * Called when a component service has {@linkplain State#FAILED failed}.
     * 
     * @param service The service that failed.
     */
    void failure(Service service);
  }
  
  /**
   * An encapsulation of all of the state that is accessed by the {@linkplain ServiceListener 
   * service listeners}.  This is extracted into its own object so that {@link ServiceListener} 
   * could be made {@code static} and its instances can be safely constructed and added in the 
   * {@link ServiceManager} constructor without having to close over the partially constructed 
   * {@link ServiceManager} instance (i.e. avoid leaking a pointer to {@code this}).
   */
  private final ServiceManagerState state;
  private final ImmutableMap<Service, ServiceListener> services;
  
  /**
   * Constructs a new instance for managing the given services.
   * 
   * @param services The services to manage
   * 
   * @throws IllegalArgumentException if not all services are {@link State#NEW new} or if there are
   *     any duplicate services.
   */
  public ServiceManager(Iterable<? extends Service> services) {
    ImmutableList<Service> copy = ImmutableList.copyOf(services);
    this.state = new ServiceManagerState(copy.size());
    ImmutableMap.Builder<Service, ServiceListener> builder = ImmutableMap.builder();
    Executor executor = MoreExecutors.sameThreadExecutor();
    for (Service service : copy) {
      ServiceListener listener = new ServiceListener(service, state);
      service.addListener(listener, executor);
      // We check the state after adding the listener as a way to ensure that our listener was added
      // to a NEW service.
      checkArgument(service.state() == State.NEW, "Can only manage NEW services, %s", service);
      builder.put(service, listener);
    }
    this.services = builder.build();
  }
  
  /**
   * Constructs a new instance for managing the given services. This constructor is provided so that
   * dependency injection frameworks can inject instances of {@link ServiceManager}.
   * 
   * @param services The services to manage
   * 
   * @throws IllegalStateException if not all services are {@link State#NEW new}.
   */
  @Inject ServiceManager(Set<Service> services) {
    this((Iterable<Service>) services);
  }
  
  /**
   * Registers a {@link Listener} to be {@linkplain Executor#execute executed} on the given 
   * executor. The listener will not have previous state changes replayed, so it is 
   * suggested that listeners are added before any of the managed services are 
   * {@linkplain Service#start started}.
   *
   * <p>There is no guaranteed ordering of execution of listeners, but any listener added through 
   * this method is guaranteed to be called whenever there is a state change.
   *
   * <p>Exceptions thrown by a listener will be propagated up to the executor. Any exception thrown 
   * during {@code Executor.execute} (e.g., a {@code RejectedExecutionException} or an exception 
   * thrown by {@linkplain MoreExecutors#sameThreadExecutor inline execution}) will be caught and
   * logged.
   * 
   * @param listener the listener to run when the manager changes state
   * @param executor the executor in which the the listeners callback methods will be run. For fast,
   *     lightweight listeners that would be safe to execute in any thread, consider 
   *     {@link MoreExecutors#sameThreadExecutor}.
   */
  public void addListener(Listener listener, Executor executor) {
    state.addListener(listener, executor);
  }

  /**
   * Initiates service {@linkplain Service#start startup} on all the services being managed.  It is
   * only valid to call this method if all of the services are {@linkplain State#NEW new}.
   * 
   * @return this
   * @throws IllegalStateException if any of the Services are not {@link State#NEW new} when the 
   *     method is called, {@link State#TERMINATED terminated} or {@link State#FAILED failed}.
   */
  public ServiceManager startAsync() {
    for (Map.Entry<Service, ServiceListener> entry : services.entrySet()) {
      Service service = entry.getKey();
      State state = service.state();
      checkState(state == State.NEW, "Service %s is %s, cannot start it.", service, 
          state);
    }
    for (ServiceListener service : services.values()) {
      service.start();
    }
    return this;
  }
  
  /**
   * Waits for the {@link ServiceManager} to become {@linkplain #isHealthy() healthy}.  The manager
   * will become healthy after all the component services have reached the {@linkplain State#RUNNING
   * running} state.  
   * 
   * @throws IllegalStateException if the service manager reaches a state from which it cannot 
   *     become {@linkplain #isHealthy() healthy}.
   */
  public void awaitHealthy() {
    state.awaitHealthy();
    checkState(isHealthy(), "Expected to be healthy after starting");
  }
  
  /**
   * Waits for the {@link ServiceManager} to become {@linkplain #isHealthy() healthy} for no more 
   * than the given time.  The manager will become healthy after all the component services have 
   * reached the {@linkplain State#RUNNING running} state. 
   *
   * @param timeout the maximum time to wait
   * @param unit the time unit of the timeout argument
   * @throws TimeoutException if not all of the services have finished starting within the deadline
   * @throws IllegalStateException if the service manager reaches a state from which it cannot 
   *     become {@linkplain #isHealthy() healthy}.
   */
  public void awaitHealthy(long timeout, TimeUnit unit) throws TimeoutException {
    if (!state.awaitHealthy(timeout, unit)) {
      // It would be nice to tell the caller who we are still waiting on, and this information is 
      // likely to be in servicesByState(), however due to race conditions we can't actually tell 
      // which services are holding up healthiness. The current set of NEW or STARTING services is
      // likely to point out the culprit, but may not.  If we really wanted to solve this we could
      // change state to track exactly which services have started and then we could accurately 
      // report on this. But it is only for logging so we likely don't care.
      throw new TimeoutException("Timeout waiting for the services to become healthy.");
    }
    checkState(isHealthy(), "Expected to be healthy after starting");
  }

  /**
   * Initiates service {@linkplain Service#stop shutdown} if necessary on all the services being 
   * managed. 
   *    
   * @return this
   */
  public ServiceManager stopAsync() {
    for (Service service : services.keySet()) {
      service.stop();
    }
    return this;
  }
 
  /**
   * Waits for the all the services to reach a terminal state. After this method returns all
   * services will either be {@link Service.State#TERMINATED terminated} or 
   * {@link Service.State#FAILED failed}
   */
  public void awaitStopped() {
    state.awaitStopped();
  }
  
  /**
   * Waits for the all the services to reach a terminal state for no more than the given time. After
   * this method returns all services will either be {@link Service.State#TERMINATED terminated} or 
   * {@link Service.State#FAILED failed}
   *
   * @param timeout the maximum time to wait
   * @param unit the time unit of the timeout argument
   * @throws TimeoutException if not all of the services have stopped within the deadline
   */
  public void awaitStopped(long timeout, TimeUnit unit) throws TimeoutException {
    if (!state.awaitStopped(timeout, unit)) {
      throw new TimeoutException("Timeout waiting for the services to stop.");
    }
  }
  
  /**
   * Returns true if all services are currently in the {@linkplain State#RUNNING running} state.  
   * 
   * <p>Users who want more detailed information should use the {@link #servicesByState} method to 
   * get detailed information about which services are not running.
   */
  public boolean isHealthy() {
    for (Service service : services.keySet()) {
      if (!service.isRunning()) {
        return false;
      }
    }
    return true;
  }
  
  /**
   * Provides a snapshot of the current state of all the services under management.
   * 
   * <p>N.B. This snapshot it not guaranteed to be consistent, i.e. the set of states returned may
   * not correspond to any particular point in time view of the services. 
   */
  public ImmutableMultimap<State, Service> servicesByState() {
    ImmutableMultimap.Builder<State, Service> builder = ImmutableMultimap.builder();
    for (Service service : services.keySet()) {
      builder.put(service.state(), service);
    }
    return builder.build();
  }
  
  /**
   * Returns the service load times. This value will only return startup times for services that
   * have finished starting.
   *
   * @return Map of services and their corresponding startup time in millis, the map entries will be
   *     ordered by startup time.
   */
  public ImmutableMap<Service, Long> startupTimes() { 
    Map<Service, Long> loadTimeMap = Maps.newHashMapWithExpectedSize(services.size());
    for (Map.Entry<Service, ServiceListener> entry : services.entrySet()) {
      State state = entry.getKey().state();
      if (state != State.NEW && state != State.STARTING) {
        loadTimeMap.put(entry.getKey(), entry.getValue().startupTimeMillis());
      }
    }
    List<Entry<Service, Long>> servicesByStartTime = Ordering.<Long>natural()
        .onResultOf(new Function<Map.Entry<Service, Long>, Long>() {
          @Override public Long apply(Map.Entry<Service, Long> input) {
            return input.getValue();
          }
        })
        .sortedCopy(loadTimeMap.entrySet());
    ImmutableMap.Builder<Service, Long> builder = ImmutableMap.builder();
    for (Map.Entry<Service, Long> entry : servicesByStartTime) {
      builder.put(entry);
    }
    return builder.build();
  }
  
  @Override public String toString() {
    return Objects.toStringHelper(ServiceManager.class)
        .add("services", services.keySet())
        .toString();
  }
  
  /**
   * An encapsulation of all the mutable state of the {@link ServiceManager} that needs to be 
   * accessed by instances of {@link ServiceListener}.
   */
  private static final class ServiceManagerState {
    final Monitor monitor = new Monitor();
    final int numberOfServices;
    /** The number of services that have not finished starting up. */
    @GuardedBy("monitor")
    int unstartedServices;
    /** The number of services that have not reached a terminal state. */
    @GuardedBy("monitor")
    int unstoppedServices;
    /** 
     * Controls how long to wait for all the service manager to either become healthy or reach a 
     * state where it is guaranteed that it can never become healthy.
     */
    final Monitor.Guard awaitHealthGuard = new Monitor.Guard(monitor) {
      @Override public boolean isSatisfied() {
        // All services have started or some service has terminated/failed.
        return unstartedServices == 0 || unstoppedServices != numberOfServices;
      }
    };
    /**
     * Controls how long to wait for all services to reach a terminal state.
     */
    final Monitor.Guard stoppedGuard = new Monitor.Guard(monitor) {
      @Override public boolean isSatisfied() {
        return unstoppedServices == 0;
      }
    };
    /** The listeners to notify during a state transition. */
    @GuardedBy("monitor")
    final List<ListenerExecutorPair> listeners = Lists.newArrayList();
    /**
     * The queue of listeners that are waiting to be executed.
     *
     * <p>Enqueue operations should be protected by {@link #monitor} while dequeue operations should
     * be protected by the implicit lock on this object. This is to ensure that listeners are
     * executed in the correct order and also so that a listener can not hold the {@link #monitor} 
     * for an arbitrary amount of time (listeners can only block other listeners, not internal state
     * transitions). We use a concurrent queue implementation so that enqueues can be executed 
     * concurrently with dequeues.
     */
    @GuardedBy("queuedListeners")
    final Queue<Runnable> queuedListeners = Queues.newConcurrentLinkedQueue();
    
    ServiceManagerState(int numberOfServices) {
      this.numberOfServices = numberOfServices;
      this.unstoppedServices = numberOfServices;
      this.unstartedServices = numberOfServices;
    }
    
    void addListener(Listener listener, Executor executor) {
      checkNotNull(listener, "listener");
      checkNotNull(executor, "executor");
      monitor.enter();
      try {
        // no point in adding a listener that will never be called
        if (unstartedServices > 0 || unstoppedServices > 0) {
          listeners.add(new ListenerExecutorPair(listener, executor));
        }
      } finally {
        monitor.leave();
      }
    }
    
    void awaitHealthy() {
      monitor.enter();
      try {
        monitor.waitForUninterruptibly(awaitHealthGuard);
      } finally {
        monitor.leave();
      }
    }
    
    boolean awaitHealthy(long timeout, TimeUnit unit) {
      monitor.enter();
      try {
        if (monitor.waitForUninterruptibly(awaitHealthGuard, timeout, unit)) {
          return true;
        }
        return false;
      } finally {
        monitor.leave();
      }
    }
    
    void awaitStopped() {
      monitor.enter();
      try {
        monitor.waitForUninterruptibly(stoppedGuard);
      } finally {
        monitor.leave();
      }
    }
    
    boolean awaitStopped(long timeout, TimeUnit unit) {
      monitor.enter();
      try {
        return monitor.waitForUninterruptibly(stoppedGuard, timeout, unit);
      } finally {
        monitor.leave();
      }
    }
    
    /**
     * This should be called when a service finishes starting up.
     * 
     * @param currentlyHealthy whether or not the service that finished starting was healthy at the 
     *        time that it finished starting. 
     */
    @GuardedBy("monitor")
    private void serviceFinishedStarting(Service service, boolean currentlyHealthy) {
      checkState(unstartedServices > 0, 
          "All services should have already finished starting but %s just finished.", service);
      unstartedServices--;
      if (currentlyHealthy && unstartedServices == 0 && unstoppedServices == numberOfServices) {
        // This means that the manager is currently healthy, or at least it should have been
        // healthy at some point from some perspective. Calling isHealthy is not currently
        // guaranteed to return true because any service could fail right now. However, the
        // happens-before relationship enforced by the monitor ensures that this method was called
        // before either serviceTerminated or serviceFailed, so we know that the manager was at 
        // least healthy for some period of time. Furthermore we are guaranteed that this call to
        // healthy() will be before any call to terminated() or failure(Service) on the listener.
        // So it is correct to execute the listener's health() callback.
        for (final ListenerExecutorPair pair : listeners) {
          queuedListeners.add(new Runnable() {
            @Override public void run() {
              pair.execute(new Runnable() {
                @Override public void run() {
                  pair.listener.healthy();
                }
              });
            }
          });
        }
      }
    }
    
    /**
     * This should be called when a service is {@linkplain State#TERMINATED terminated}.
     */
    @GuardedBy("monitor")
    private void serviceTerminated(Service service) {
      serviceStopped(service);
    }
    
    /**
     * This should be called when a service is {@linkplain State#FAILED failed}.
     */
    @GuardedBy("monitor")
    private void serviceFailed(final Service service) {
      for (final ListenerExecutorPair pair : listeners) {
        queuedListeners.add(new Runnable() {
          @Override public void run() {
            pair.execute(new Runnable() {
              @Override public void run() {
                pair.listener.failure(service);
              }
            });
          }
        });
      }
      serviceStopped(service);
    }
    
    /**
     * Should be called whenever a service reaches a terminal state (
     * {@linkplain State#TERMINATED terminated} or 
     * {@linkplain State#FAILED failed}).
     */
    @GuardedBy("monitor")
    private void serviceStopped(Service service) {
      checkState(unstoppedServices > 0, 
          "All services should have already stopped but %s just stopped.", service);
      unstoppedServices--;
      if (unstoppedServices == 0) {
        checkState(unstartedServices == 0, 
            "All services are stopped but %d services haven't finished starting", 
            unstartedServices);
        for (final ListenerExecutorPair pair : listeners) {
          queuedListeners.add(new Runnable() {
            @Override public void run() {
              pair.execute(new Runnable() {
                @Override public void run() {
                  pair.listener.stopped();
                }
              });
            }
          });
        }
        // no more listeners could possibly be called, so clear them out
        listeners.clear();
      }
    }
    
    /** 
     * Attempts to execute all the listeners in {@link #queuedListeners}.
     */
    private void executeListeners() {
      checkState(!monitor.isOccupiedByCurrentThread(), 
          "It is incorrect to execute listeners with the monitor held.");
      synchronized (queuedListeners) {
        Runnable listener;
        while ((listener = queuedListeners.poll()) != null) {
          listener.run();
        }
      }
    }
  }

  /**
   * A {@link Service} that wraps another service and times how long it takes for it to start and 
   * also calls the {@link ServiceManagerState#serviceFinishedStarting}, 
   * {@link ServiceManagerState#serviceTerminated} and {@link ServiceManagerState#serviceFailed}
   * according to its current state.
   */
  private static final class ServiceListener implements Service.Listener {
    @GuardedBy("watch")  // AFAICT Stopwatch is not thread safe so we need to protect accesses
    final Stopwatch watch = new Stopwatch();
    final Service service;
    final ServiceManagerState state;
    
    /**
     * @param service the service that 
     */
    ServiceListener(Service service, ServiceManagerState state) {
      this.service = service;
      this.state = state;
    }
    
    @Override public void starting() {
      // This can happen if someone besides the ServiceManager starts the service, in this case
      // our timings may be inaccurate.
      startTimer();
    }
    
    @Override public void running() {
      state.monitor.enter();
      try {
        finishedStarting(true);
      } finally {
        state.monitor.leave();
        state.executeListeners();
      }
    }
    
    @Override public void stopping(State from) {
      if (from == State.STARTING) {
        state.monitor.enter();
        try {
          finishedStarting(false);
        } finally {
          state.monitor.leave();
          state.executeListeners();
        }
      }
    }
    
    @Override public void terminated(State from) {
      logger.info("Service " + service + " has terminated. Previous state was " + from + " state.");
      state.monitor.enter();
      try {
        if (from == State.NEW) {
          // startTimer is idempotent, so this is safe to call and it may be necessary if no one has
          // started the timer yet.
          startTimer(); 
          finishedStarting(false);
        }
        state.serviceTerminated(service);
      } finally {
        state.monitor.leave();
        state.executeListeners();
      }
    }
    
    @Override public void failed(State from, Throwable failure) {
      logger.log(Level.SEVERE, "Service " + service + " has failed in the " + from + " state.", 
          failure);
      state.monitor.enter();
      try {
        if (from == State.STARTING) {
          finishedStarting(false);
        }
        state.serviceFailed(service);
      } finally {
        state.monitor.leave();
        state.executeListeners();
      }
    }
    
    /** 
     * Stop the stopwatch, log the startup time and decrement the startup latch
     *  
     * @param currentlyHealthy whether or not the service that finished starting is currently 
     *        healthy 
     */
    @GuardedBy("monitor")
    void finishedStarting(boolean currentlyHealthy) {
      synchronized (watch) {
        watch.stop();
        logger.log(Level.INFO, "Started " + service + " in " + startupTimeMillis() + " ms.");
      }
      state.serviceFinishedStarting(service, currentlyHealthy);
    }
    
    void start() {
      startTimer();
      service.start();
    }
  
    /** Start the timer if it hasn't been started. */
    void startTimer() {
      synchronized (watch) {
        if (!watch.isRunning()) { // only start the watch once.
          watch.start();
          logger.log(Level.INFO, "Starting {0}", service);
        }
      }
    }
    
    /** Returns the amount of time it took for the service to finish starting in milliseconds. */
    synchronized long startupTimeMillis() {
      synchronized (watch) {
        return watch.elapsed(MILLISECONDS);
      }
    }
  }
  
  /** Simple value object binding a listener to its executor. */
  @Immutable private static final class ListenerExecutorPair {
    final Listener listener;
    final Executor executor;
    
    ListenerExecutorPair(Listener listener, Executor executor) {
      this.listener = listener;
      this.executor = executor;
    }
    
    /**
     * Executes the given {@link Runnable} on {@link #executor} logging and swallowing all 
     * exceptions
     */
    void execute(Runnable runnable) {
      try {
        executor.execute(runnable);
      } catch (Exception e) {
        logger.log(Level.SEVERE, "Exception while executing listener " + listener 
            + " with executor " + executor, e);
      }
    }
  }
}
