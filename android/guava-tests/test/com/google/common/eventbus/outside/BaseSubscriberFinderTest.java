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

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.Lists;
import com.google.common.eventbus.Subscribe;
import com.google.common.eventbus.outside.BaseSubscriberFinderTest.Subscriber;
import java.util.List;

public class BaseSubscriberFinderTest extends AbstractEventBusTest<Subscriber> {
  static class Subscriber {
    final List<Object> nonSubscriberEvents = Lists.newArrayList();
    final List<Object> subscriberEvents = Lists.newArrayList();

    public void notASubscriber(Object o) {
      nonSubscriberEvents.add(o);
    }

    @Subscribe
    public void subscriber(Object o) {
      subscriberEvents.add(o);
    }
  }

  public void testNonSubscriber() {
    assertThat(getSubscriber().nonSubscriberEvents).isEmpty();
  }

  public void testSubscriber() {
    assertThat(getSubscriber().subscriberEvents).contains(EVENT);
  }

  @Override
  Subscriber createSubscriber() {
    return new Subscriber();
  }
}
