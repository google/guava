/*
 * Copyright (C) 2009 Google Inc.
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

import com.google.common.base.Service;
import com.google.common.base.Throwables;

import java.util.concurrent.Executor;
import java.util.concurrent.Future;

/**
 * Base class for services that can implement {@link #startUp}, {@link #run} and
 * {@link #shutDown} methods. This class uses a single thread to execute the
 * service; consider {@link AbstractService} if you would like to manage any
 * threading manually.
 *
 * @author Jesse Wilson
 * @since 2009.09.15 <b>tentative</b>
 */
public abstract class AbstractExecutionThreadService implements Service {

  /* use AbstractService for state management */
  private final Service delegate = new AbstractService() {
    @Override protected final void doStart() {
      executor().execute(new Runnable() {
        public void run() {
          try {
            startUp();
            notifyStarted();

            if (isRunning()) {
              try {
                AbstractExecutionThreadService.this.run();
              } catch (Throwable t) {
                try {
                  shutDown();
                } catch (Exception ignored) {}
                throw t;
              }
            }

            shutDown();
            notifyStopped();
          } catch (Throwable t) {
            notifyFailed(t);
            throw Throwables.propagate(t);
          }
        }
      });
    }

    @Override protected void doStop() {
      triggerShutdown();
    }
  };

  /**
   * Start the service. This method is invoked on the execution thread.
   */
  protected void startUp() throws Exception {}

  /**
   * Run the service. This method is invoked on the execution thread.
   * Implementations must respond to stop requests. You could poll for lifecycle
   * changes in a work loop:
   * <pre>
   *   public void run() {
   *     while ({@link #isRunning()}) {
   *       // perform a unit of work
   *     }
   *   }
   * </pre>
   * ...or you could respond to stop requests by implementing {@link
   * #triggerShutdown()}, which should cause {@link #run()} to return.
   */
  protected abstract void run() throws Exception;

  /**
   * Stop the service. This method is invoked on the execution thread.
   */
  // TODO: consider supporting a TearDownTestCase-like API
  protected void shutDown() throws Exception {}

  /**
   * Invoked to request the service to stop.
   */
  protected void triggerShutdown() {}

  /**
   * Returns the {@link Executor} that will be used to run this service.
   * Subclasses may override this method to use a custom {@link Executor}, which
   * may configure its worker thread with a specific name, thread group or
   * priority. The returned executor's {@link Executor#execute(Runnable)
   * execute()} method is called when this service is started, and should return
   * promptly.
   */
  protected Executor executor() {
    return new Executor() {
      public void execute(Runnable command) {
        new Thread(command, AbstractExecutionThreadService.this.toString())
            .start();
      }
    };
  }

  @Override public String toString() {
    return getClass().getSimpleName();
  }

  // We override instead of using ForwardingService so that these can be final.

  /*@Override*/ public final Future<State> start() {
    return delegate.start();
  }

  /*@Override*/ public final State startAndWait() {
    return delegate.startAndWait();
  }

  /*@Override*/ public final boolean isRunning() {
    return delegate.isRunning();
  }

  /*@Override*/ public final State state() {
    return delegate.state();
  }

  /*@Override*/ public final Future<State> stop() {
    return delegate.stop();
  }

  /*@Override*/ public final State stopAndWait() {
    return delegate.stopAndWait();
  }
}
