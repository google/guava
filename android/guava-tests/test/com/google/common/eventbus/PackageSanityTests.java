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

package com.google.common.eventbus;

import com.google.common.testing.AbstractPackageSanityTests;
import java.lang.reflect.Method;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;

/**
 * Basic sanity tests for the entire package.
 *
 * @author Ben Yu
 */

public class PackageSanityTests extends AbstractPackageSanityTests {

  public PackageSanityTests() throws Exception {
    DummySubscriber dummySubscriber = new DummySubscriber();
    setDefault(Subscriber.class, dummySubscriber.toSubscriber());
    setDefault(Method.class, DummySubscriber.subscriberMethod());
    setDefault(SubscriberExceptionContext.class, dummySubscriber.toContext());
    setDefault(Dispatcher.class, Dispatcher.immediate());
  }

  private static class DummySubscriber {

    private final EventBus eventBus = new EventBus();

    @Subscribe
    public void handle(@NullableDecl Object anything) {}

    Subscriber toSubscriber() throws Exception {
      return Subscriber.create(eventBus, this, subscriberMethod());
    }

    SubscriberExceptionContext toContext() {
      return new SubscriberExceptionContext(eventBus, new Object(), this, subscriberMethod());
    }

    private static Method subscriberMethod() {
      try {
        return DummySubscriber.class.getMethod("handle", Object.class);
      } catch (NoSuchMethodException e) {
        throw new AssertionError(e);
      }
    }
  }
}
