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
import static com.google.common.base.Predicates.instanceOf;
import static com.google.common.base.Predicates.not;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

import com.google.common.annotations.Beta;
import com.google.common.base.Function;
import com.google.common.base.Objects;
import com.google.common.base.Stopwatch;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Ordering;
import com.google.common.util.concurrent.Service.State;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
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
 * <p>This class uses the ServiceManager's methods to start all of its services, to respond to
 * service failure and to ensure that when the JVM is shutting down all the services are stopped.
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
   * @since 15.0 (present as an interface in 14.0)
   */
  @Beta  // Should come out of Beta when ServiceManager does
  public abstract static class Listener {
    /** 
     * Called when the service initially becomes healthy.
     * 
     * <p>This will be called at most once after all the services have entered the 
     * {@linkplain State#RUNNING running} state. If any services fail during start up or 
     * {@linkplain State#FAILED fail}/{@linkplain State#TERMINATED terminate} before all other 
     * services have started {@linkplain State#RUNNING running} then this method will not be called.
     */
    public void healthy() {}
    
    /** 
     * Called when the all of the component services have reached a terminal state, either 
     * {@linkplain State#TERMINATED terminated} or {@linkplain State#FAILED failed}.
     */
    public void stopped() {}
    
    /** 
     * Called when a component service has {@linkplain State#FAILED failed}.
     * 
     * @param service The service that failed.
     */
    public void failure(Service service) {}
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
    if (copy.isEmpty()) {
      // Having no services causes the manager to behave strangely. Notably, listeners are never 
      // fired.  To avoid this we substitute a placeholder service.
      logger.log(Level.WARNING, 
          "ServiceManager configured with no services.  Is your application configured properly?", 
          new EmptyServiceManagerWarning());
      copy = ImmutableList.<Service>of(new NoOpService());
    }
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
   * <p> For fast, lightweight listeners that would be safe to execute in any thread, consider 
   * calling {@link #addListener(Listener)}.
   * 
   * @param listener the listener to run when the manager changes state
   * @param executor the executor in which the listeners callback methods will be run.
   */
  public void addListener(Listener listener, Executor executor) {
    state.addListener(listener, executor);
  }

  /**
   * Registers a {@link Listener} to be run when this {@link ServiceManager} changes state. The 
   * listener will not have previous state changes replayed, so it is suggested that listeners are 
   * added before any of the managed services are {@linkplain Service#start started}.
   *
   * <p>There is no guaranteed ordering of execution of listeners, but any listener added through 
   * this method is guaranteed to be called whenever there is a state change.
   *
   * <p>Exceptions thrown by a listener will be will be caught and logged.
   * 
   * @param listener the listener to run when the manager changes state
   */
  public void addListener(Listener listener) {
    state.addListener(listener, MoreExecutors.sameThreadExecutor());
  }

  /**
   * Initiates service {@linkplain Service#start startup} on all the services being managed.  It is
   * only valid to call this method if all of the services are {@linkplain State#NEW new}.
   * 
   * @return this
   * @throws IllegalStateException if any of the Services are not {@link State#NEW new} when the 
   *     method is called.
   */
  public ServiceManager startAsync() {
    for (Map.Entry<Service, ServiceListener> entry : services.entrySet()) {
      Service service = entry.getKey();
      State state = service.state();
      checkState(state == State.NEW, "Service %s is %s, cannot start it.", service, 
          state);
    }
    for (ServiceListener service : services.values()) {
      try {
        service.start();
      } catch (IllegalStateException e) {
        // This can happen if the service has already been started or stopped (e.g. by another 
        // service or listener). Our contract says it is safe to call this method if
        // all services were NEW when it was called, and this has already been verified above, so we
        // don't propagate the exception.
        logger.log(Level.WARNING, "Unable to start Service " + service.service, e);
      }
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
      if (!(service instanceof NoOpService)) {
        builder.put(service.state(), service);
      }
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
    List<Entry<Service, Long>> loadTimes = Lists.newArrayListWithCapacity(services.size());
    for (Map.Entry<Service, ServiceListener> entry : services.entrySet()) {
      Service service = entry.getKey();
      State state = service.state();
      if (state != State.NEW & state != State.STARTING & !(service instanceof NoOpService)) {
        loadTimes.add(Maps.immutableEntry(service, entry.getValue().startupTimeMillis()));
      }
    }
    Collections.sort(loadTimes, Ordering.<Long>natural()
        .onResultOf(new Function<Entry<Service, Long>, Long>() {
          @Override public Long apply(Map.Entry<Service, Long> input) {
            return input.getValue();
          }
        }));
    ImmutableMap.Builder<Service, Long> builder = ImmutableMap.builder();
    for (Entry<Service, Long> entry : loadTimes) {
      builder.put(entry);
    }
    return builder.build();
  }
  
  @Override public String toString() {
    return Objects.toStringHelper(ServiceManager.class)
        .add("services", Collections2.filter(services.keySet(), not(instanceOf(NoOpService.class))))
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
        return unstartedServices == 0 | unstoppedServices != numberOfServices;
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
     * <p>Enqueue operations should be protected by {@link #monitor} while dequeue operations are
     * not protected. Holding {@link #monitor} while enqueuing ensures that listeners in the queue
     * are in the correct order and {@link ExecutionQueue} ensures that they are executed in the 
     * correct order.
     */
    @GuardedBy("monitor")
    final ExecutionQueue queuedListeners = new ExecutionQueue();
    
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
      monitor.enterWhenUninterruptibly(awaitHealthGuard);
      monitor.leave();
    }
    
    boolean awaitHealthy(long timeout, TimeUnit unit) {
      if (monitor.enterWhenUninterruptibly(awaitHealthGuard, timeout, unit)) {
        monitor.leave();
        return true;
      }
      return false;
    }
    
    void awaitStopped() {
      monitor.enterWhenUninterruptibly(stoppedGuard);
      monitor.leave();
    }
    
    boolean awaitStopped(long timeout, TimeUnit unit) {
      if (monitor.enterWhenUninterruptibly(stoppedGuard, timeout, unit)) {
        monitor.leave();
        return true;
      }
      return false;
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
              pair.listener.healthy();
            }
          }, pair.executor);
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
            pair.listener.failure(service);
          }
        }, pair.executor);
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
              pair.listener.stopped();
            }
          }, pair.executor);
        }
        // no more listeners could possibly be called, so clear them out
        listeners.clear();
      }
    }

    /** Attempts to execute all the listeners in {@link #queuedListeners}. */
    private void executeListeners() {
      checkState(!monitor.isOccupiedByCurrentThread(), 
          "It is incorrect to execute listeners with the monitor held.");
      queuedListeners.execute();
    }
  }

  /**
   * A {@link Service} that wraps another service and times how long it takes for it to start and 
   * also calls the {@link ServiceManagerState#serviceFinishedStarting}, 
   * {@link ServiceManagerState#serviceTerminated} and {@link ServiceManagerState#serviceFailed}
   * according to its current state.
   */
  private static final class ServiceListener extends Service.Listener {
    @GuardedBy("watch")  // AFAICT Stopwatch is not thread safe so we need to protect accesses
    final Stopwatch watch = Stopwatch.createUnstarted();
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
      if (!(service instanceof NoOpService)) {
        logger.log(Level.FINE, "Service {0} has terminated. Previous state was: {1}", 
            new Object[] {service, from});
      }
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
        if (!(service instanceof NoOpService)) {
          logger.log(Level.FINE, "Started {0} in {1} ms.", 
              new Object[] {service, startupTimeMillis()});
        }
      }
      state.serviceFinishedStarting(service, currentlyHealthy);
    }
    
    void start() {
      startTimer();
      service.startAsync();
    }
  
    /** Start the timer if it hasn't been started. */
    void startTimer() {
      synchronized (watch) {
        if (!watch.isRunning()) { // only start the watch once.
          watch.start();
          if (!(service instanceof NoOpService)) {
            logger.log(Level.FINE, "Starting {0}.", service);
          }
        }
      }
    }
    
    /** Returns the amount of time it took for the service to finish starting in milliseconds. */
    long startupTimeMillis() {
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
  }
  
  /**
   * A {@link Service} instance that does nothing.  This is only useful as a placeholder to
   * ensure that the {@link ServiceManager} functions properly even when it is managing no services.
   * 
   * <p>The use of this class is considered an implementation detail of the class and as such it is
   * excluded from {@link #servicesByState}, {@link #startupTimes}, {@link #toString} and all 
   * logging statements.
   */
  private static final class NoOpService extends AbstractService {
    @Override protected void doStart() { notifyStarted(); }
    @Override protected void doStop() { notifyStopped(); }
  }
  
  /** This is never thrown but only used for logging. */
  private static final class EmptyServiceManagerWarning extends Throwable {}
}
