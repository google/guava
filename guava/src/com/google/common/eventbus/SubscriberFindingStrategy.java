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

import com.google.common.collect.Multimap;

/**
 * A method for finding event subscriber methods in objects, for use by
 * {@link EventBus}.
 *
 * @author Cliff Biffle
 */
interface SubscriberFindingStrategy {

  /**
   * Finds all suitable event subscriber methods in {@code source}, organizes them
   * by the type of event they handle, and wraps them in {@link EventSubscriber} instances.
   *
   * @param source  object whose subscribers are desired.
   * @return EventSubscriber objects for each subscriber method, organized by event
   *         type.
   *
   * @throws IllegalArgumentException if {@code source} is not appropriate for
   *         this strategy (in ways that this interface does not define).
   */
  Multimap<Class<?>, EventSubscriber> findAllSubscribers(Object source);

}
