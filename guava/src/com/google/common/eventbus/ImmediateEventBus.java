/*
 * Copyright (C) 2007 The Guava Authors
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

package com.google.common.eventbus;

import com.google.common.annotations.Beta;
import com.google.common.eventbus.EventBus.LoggingHandler;
import com.google.common.util.concurrent.MoreExecutors;

import java.util.concurrent.Executor;

/**
 * An {@link EventBus} that takes the Executor of your choice and uses it to
 * dispatch events to subscribers immediately as they're posted without using an
 * intermediate queue to change the dispatch order.
 * 
 * @see Dispatcher#immediate()
 *
 * @author David Rain
 * @since 22.0
 */
@Beta
public class ImmediateEventBus extends EventBus {
  /**
   * Creates a new ImmediateEventBus with the given {@code identifier}.
   *
   * @param identifier
   *          a brief name for this bus, for logging purposes. Should be a valid
   *          Java identifier.
   */
  public ImmediateEventBus(String identifier) {
    super(identifier, MoreExecutors.directExecutor(), Dispatcher.immediate(), LoggingHandler.INSTANCE);
  }

  /**
   * Creates a new ImmediateEventBus that will use {@code executor} to dispatch
   * events. Assigns {@code identifier} as the bus's name for logging purposes.
   *
   * @param identifier
   *          short name for the bus, for logging purposes.
   * @param executor
   *          Executor to use to dispatch events. It is the caller's
   *          responsibility to shut down the executor after the last event has
   *          been posted to this event bus.
   */
  public ImmediateEventBus(String identifier, Executor executor) {
    super(identifier, executor, Dispatcher.immediate(), LoggingHandler.INSTANCE);
  }

  /**
   * Creates a new ImmediateEventBus that will use {@code executor} to dispatch
   * events.
   *
   * @param executor
   *          Executor to use to dispatch events. It is the caller's
   *          responsibility to shut down the executor after the last event has
   *          been posted to this event bus.
   * @param subscriberExceptionHandler
   *          Handler used to handle exceptions thrown from subscribers. See
   *          {@link SubscriberExceptionHandler} for more information.
   * @since 16.0
   */
  public ImmediateEventBus(Executor executor, SubscriberExceptionHandler subscriberExceptionHandler) {
    super("default", executor, Dispatcher.immediate(), subscriberExceptionHandler);
  }

  /**
   * Creates a new ImmediateEventBus that will use {@code executor} to dispatch
   * events.
   *
   * @param executor
   *          Executor to use to dispatch events. It is the caller's
   *          responsibility to shut down the executor after the last event has
   *          been posted to this event bus.
   */
  public ImmediateEventBus(Executor executor) {
    super("default", executor, Dispatcher.immediate(), LoggingHandler.INSTANCE);
  }
}
