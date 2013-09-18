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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Wraps a single-argument subscriber method on a specific object, and ensures
 * that only one thread may enter the method at a time.
 *
 * <p>Beyond synchronization, this class behaves identically to
 * {@link EventSubscriber}.
 *
 * @author Cliff Biffle
 */
final class SynchronizedEventSubscriber extends EventSubscriber {
  /**
   * Creates a new SynchronizedEventSubscriber to wrap {@code method} on
   * {@code target}.
   *
   * @param target  object to which the method applies.
   * @param method  subscriber method.
   */
  public SynchronizedEventSubscriber(Object target, Method method) {
    super(target, method);
  }

  @Override
  public void handleEvent(Object event) throws InvocationTargetException {
    // https://code.google.com/p/guava-libraries/issues/detail?id=1403
    synchronized (this) {
      super.handleEvent(event);
    }
  }
}
