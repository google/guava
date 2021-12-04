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

import com.google.common.annotations.GwtIncompatible;
import com.google.common.base.Supplier;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.j2objc.annotations.WeakOuter;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Base class for services that do not need a thread while "running" but may need one during startup
 * and shutdown. Subclasses can implement {@link #startUp} and {@link #shutDown} methods, each which
 * run in a executor which by default uses a separate thread for each method.
 *
 * @author Chris Nokleberg
 * @since 1.0
 */
@GwtIncompatible
@ElementTypesAreNonnullByDefault
public abstract class AbstractIdleService implements Service {

  /* Thread names will look like {@code "MyService STARTING"}. */
  private final Supplier<String> threadNameSupplier = new ThreadNameSupplier();

  @WeakOuter
  private final class ThreadNameSupplier implements Supplier<String> {
    @Override
    public String get() {
      return serviceName() + " " + state();
    }
  }

  /* use AbstractService for state management */
  private final Service delegate = new DelegateService();

  @WeakOuter
  private final class DelegateService extends AbstractService {
    @Override
    protected final void doStart() {
      MoreExecutors.renamingDecorator(executor(), threadNameSupplier)
          .execute(
              new Runnable() {
                @Override
                public void run() {
                  try {
                    startUp();
                    notifyStarted();
                  } catch (Throwable t) {
                    notifyFailed(t);
                  }
                }
              });
    }

    @Override
    protected final void doStop() {
      MoreExecutors.renamingDecorator(executor(), threadNameSupplier)
          .execute(
              new Runnable() {
                @Override
                public void run() {
                  try {
                    shutDown();
                    notifyStopped();
                  } catch (Throwable t) {
                    notifyFailed(t);
                  }
                }
              });
    }

    @Override
    public String toString() {
      return AbstractIdleService.this.toString();
    }
  }

  /** Constructor for use by subclasses. */
  protected AbstractIdleService() {}

  /** Start the service. */
  protected abstract void startUp() throws Exception;

  /** Stop the service. */
  protected abstract void shutDown() throws Exception;

  /**
   * Returns the {@link Executor} that will be used to run this service. Subclasses may override
   * this method to use a custom {@link Executor}, which may configure its worker thread with a
   * specific name, thread group or priority. The returned executor's {@link
   * Executor#execute(Runnable) execute()} method is called when this service is started and
   * stopped, and should return promptly.
   */
  protected Executor executor() {
    return new Executor() {
      @Override
      public void execute(Runnable command) {
        MoreExecutors.newThread(threadNameSupplier.get(), command).start();
      }
    };
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
   * Returns the name of this service. {@link AbstractIdleService} may include the name in debugging
   * output.
   *
   * @since 14.0
   */
  protected String serviceName() {
    return getClass().getSimpleName();
  }
}
