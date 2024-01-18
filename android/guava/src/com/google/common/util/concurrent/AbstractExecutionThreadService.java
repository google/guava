/*
 * Copyright (C) 2009 The Guava Authors
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

import static com.google.common.util.concurrent.Platform.restoreInterruptIfIsInterruptedException;

import com.google.common.annotations.GwtIncompatible;
import com.google.common.annotations.J2ktIncompatible;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;

/**
 * Base class for services that can implement {@link #startUp}, {@link #run} and {@link #shutDown}
 * methods. This class uses a single thread to execute the service; consider {@link AbstractService}
 * if you would like to manage any threading manually.
 *
 * @author Jesse Wilson
 * @since 1.0
 */
@GwtIncompatible
@J2ktIncompatible
@ElementTypesAreNonnullByDefault
public abstract class AbstractExecutionThreadService implements Service {
  private static final LazyLogger logger = new LazyLogger(AbstractExecutionThreadService.class);

  /* use AbstractService for state management */
  private final Service delegate =
      new AbstractService() {
        @Override
        protected final void doStart() {
          Executor executor = MoreExecutors.renamingDecorator(executor(), () -> serviceName());
          executor.execute(
              () -> {
                try {
                  startUp();
                  notifyStarted();
                  // If stopAsync() is called while starting we may be in the STOPPING state in
                  // which case we should skip right down to shutdown.
                  if (isRunning()) {
                    try {
                      AbstractExecutionThreadService.this.run();
                    } catch (Throwable t) {
                      restoreInterruptIfIsInterruptedException(t);
                      try {
                        shutDown();
                      } catch (Exception ignored) {
                        restoreInterruptIfIsInterruptedException(ignored);
                        // TODO(lukes): if guava ever moves to java7, this would be a good
                        // candidate for a suppressed exception, or maybe we could generalize
                        // Closer.Suppressor
                        logger
                            .get()
                            .log(
                                Level.WARNING,
                                "Error while attempting to shut down the service after failure.",
                                ignored);
                      }
                      notifyFailed(t);
                      return;
                    }
                  }

                  shutDown();
                  notifyStopped();
                } catch (Throwable t) {
                  restoreInterruptIfIsInterruptedException(t);
                  notifyFailed(t);
                }
              });
        }

        @Override
        protected void doStop() {
          triggerShutdown();
        }

        @Override
        public String toString() {
          return AbstractExecutionThreadService.this.toString();
        }
      };

  /** Constructor for use by subclasses. */
  protected AbstractExecutionThreadService() {}

  /**
   * Start the service. This method is invoked on the execution thread.
   *
   * <p>By default this method does nothing.
   */
  protected void startUp() throws Exception {}

  /**
   * Run the service. This method is invoked on the execution thread. Implementations must respond
   * to stop requests. You could poll for lifecycle changes in a work loop:
   *
   * <pre>
   *   public void run() {
   *     while ({@link #isRunning()}) {
   *       // perform a unit of work
   *     }
   *   }
   * </pre>
   *
   * <p>...or you could respond to stop requests by implementing {@link #triggerShutdown()}, which
   * should cause {@link #run()} to return.
   */
  protected abstract void run() throws Exception;

  /**
   * Stop the service. This method is invoked on the execution thread.
   *
   * <p>By default this method does nothing.
   */
  // TODO: consider supporting a TearDownTestCase-like API
  protected void shutDown() throws Exception {}

  /**
   * Invoked to request the service to stop.
   *
   * <p>By default this method does nothing.
   *
   * <p>Currently, this method is invoked while holding a lock. If an implementation of this method
   * blocks, it can prevent this service from changing state. If you need to performing a blocking
   * operation in order to trigger shutdown, consider instead registering a listener and
   * implementing {@code stopping}. Note, however, that {@code stopping} does not run at exactly the
   * same times as {@code triggerShutdown}.
   */
  protected void triggerShutdown() {}

  /**
   * Returns the {@link Executor} that will be used to run this service. Subclasses may override
   * this method to use a custom {@link Executor}, which may configure its worker thread with a
   * specific name, thread group or priority. The returned executor's {@link
   * Executor#execute(Runnable) execute()} method is called when this service is started, and should
   * return promptly.
   *
   * <p>The default implementation returns a new {@link Executor} that sets the name of its threads
   * to the string returned by {@link #serviceName}
   */
  protected Executor executor() {
    return command -> MoreExecutors.newThread(serviceName(), command).start();
  }

  @Override
  public String toString() {
    return serviceName() + " [" + state() + "]";
  }

  @Override
  public final boolean isRunning() {
    return delegate.isRunning();
  }

  @Override
  public final State state() {
    return delegate.state();
  }

  /** @since 13.0 */
  @Override
  public final void addListener(Listener listener, Executor executor) {
    delegate.addListener(listener, executor);
  }

  /** @since 14.0 */
  @Override
  public final Throwable failureCause() {
    return delegate.failureCause();
  }

  /** @since 15.0 */
  @CanIgnoreReturnValue
  @Override
  public final Service startAsync() {
    delegate.startAsync();
    return this;
  }

  /** @since 15.0 */
  @CanIgnoreReturnValue
  @Override
  public final Service stopAsync() {
    delegate.stopAsync();
    return this;
  }

  /** @since 15.0 */
  @Override
  public final void awaitRunning() {
    delegate.awaitRunning();
  }

  /** @since 15.0 */
  @Override
  public final void awaitRunning(long timeout, TimeUnit unit) throws TimeoutException {
    delegate.awaitRunning(timeout, unit);
  }

  /** @since 15.0 */
  @Override
  public final void awaitTerminated() {
    delegate.awaitTerminated();
  }

  /** @since 15.0 */
  @Override
  public final void awaitTerminated(long timeout, TimeUnit unit) throws TimeoutException {
    delegate.awaitTerminated(timeout, unit);
  }

  /**
   * Returns the name of this service. {@link AbstractExecutionThreadService} may include the name
   * in debugging output.
   *
   * <p>Subclasses may override this method.
   *
   * @since 14.0 (present in 10.0 as getServiceName)
   */
  protected String serviceName() {
    return getClass().getSimpleName();
  }
}
