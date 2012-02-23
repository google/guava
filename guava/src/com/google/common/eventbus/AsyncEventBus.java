/*
 * Copyright (C) 2007 The Guava Authors
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

package com.google.common.eventbus;

import com.google.common.annotations.Beta;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;

/**
 * An {@link EventBus} that takes the Executor of your choice and uses it to
 * dispatch events, allowing dispatch to occur asynchronously.
 *
 * @author Cliff Biffle
 * @since 10.0
 */
@Beta
public class AsyncEventBus extends EventBus {
  private final Executor executor;

  /** the queue of events is shared across all threads */
  private final ConcurrentLinkedQueue<EventWithHandler> eventsToDispatch =
      new ConcurrentLinkedQueue<EventWithHandler>();

  /**
   * Creates a new AsyncEventBus that will use {@code executor} to dispatch
   * events.  Assigns {@code identifier} as the bus's name for logging purposes.
   *
   * @param identifier short name for the bus, for logging purposes.
   * @param executor   Executor to use to dispatch events. It is the caller's
   *        responsibility to shut down the executor after the last event has
   *        been posted to this event bus.
   */
  public AsyncEventBus(String identifier, Executor executor) {
    super(identifier);
    this.executor = executor;
  }

  /**
   * Creates a new AsyncEventBus that will use {@code executor} to dispatch
   * events.
   *
   * @param executor Executor to use to dispatch events. It is the caller's
   *        responsibility to shut down the executor after the last event has
   *        been posted to this event bus.
   */
  public AsyncEventBus(Executor executor) {
    this.executor = executor;
  }

  @Override
  protected void enqueueEvent(Object event, EventHandler handler) {
    eventsToDispatch.offer(new EventWithHandler(event, handler));
  }

  /**
   * Dispatch {@code events} in the order they were posted, regardless of
   * the posting thread.
   */
  @Override
  protected void dispatchQueuedEvents() {
    while (true) {
      EventWithHandler eventWithHandler = eventsToDispatch.poll();
      if (eventWithHandler == null) {
        break;
      }

      dispatch(eventWithHandler.event, eventWithHandler.handler);
    }
  }

  /**
   * Calls the {@link #executor} to dispatch {@code event} to {@code handler}.
   */
  @Override
  protected void dispatch(final Object event, final EventHandler handler) {
    executor.execute(new Runnable() {
          @Override
          @SuppressWarnings("synthetic-access")
          public void run() {
            AsyncEventBus.super.dispatch(event, handler);
          }
        });
  }

}
