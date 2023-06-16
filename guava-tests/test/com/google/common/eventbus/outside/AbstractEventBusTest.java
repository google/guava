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

package com.google.common.eventbus.outside;

import com.google.common.eventbus.EventBus;
import junit.framework.TestCase;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Abstract base class for tests that EventBus finds the correct subscribers.
 *
 * <p>The actual tests are distributed among the other classes in this package based on whether they
 * are annotated or abstract in the superclass.
 *
 * <p>This test must be outside the c.g.c.eventbus package to test correctly.
 *
 * @author Louis Wasserman
 */
abstract class AbstractEventBusTest<H> extends TestCase {
  static final Object EVENT = new Object();

  abstract H createSubscriber();

  private @Nullable H subscriber;

  H getSubscriber() {
    return subscriber;
  }

  @Override
  protected void setUp() throws Exception {
    subscriber = createSubscriber();
    EventBus bus = new EventBus();
    bus.register(subscriber);
    bus.post(EVENT);
  }

  @Override
  protected void tearDown() throws Exception {
    subscriber = null;
  }
}
