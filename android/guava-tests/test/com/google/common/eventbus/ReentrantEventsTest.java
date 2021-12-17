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

import com.google.common.collect.Lists;
import java.util.List;
import junit.framework.TestCase;

/**
 * Validate that {@link EventBus} behaves carefully when listeners publish their own events.
 *
 * @author Jesse Wilson
 */
public class ReentrantEventsTest extends TestCase {

  static final String FIRST = "one";
  static final Double SECOND = 2.0d;

  final EventBus bus = new EventBus();

  public void testNoReentrantEvents() {
    ReentrantEventsHater hater = new ReentrantEventsHater();
    bus.register(hater);

    bus.post(FIRST);

    assertEquals(
        "ReentrantEventHater expected 2 events",
        Lists.<Object>newArrayList(FIRST, SECOND),
        hater.eventsReceived);
  }

  public class ReentrantEventsHater {
    boolean ready = true;
    List<Object> eventsReceived = Lists.newArrayList();

    @Subscribe
    public void listenForStrings(String event) {
      eventsReceived.add(event);
      ready = false;
      try {
        bus.post(SECOND);
      } finally {
        ready = true;
      }
    }

    @Subscribe
    public void listenForDoubles(Double event) {
      assertTrue("I received an event when I wasn't ready!", ready);
      eventsReceived.add(event);
    }
  }

  public void testEventOrderingIsPredictable() {
    EventProcessor processor = new EventProcessor();
    bus.register(processor);

    EventRecorder recorder = new EventRecorder();
    bus.register(recorder);

    bus.post(FIRST);

    assertEquals(
        "EventRecorder expected events in order",
        Lists.<Object>newArrayList(FIRST, SECOND),
        recorder.eventsReceived);
  }

  public class EventProcessor {
    @Subscribe
    public void listenForStrings(String event) {
      bus.post(SECOND);
    }
  }

  public class EventRecorder {
    List<Object> eventsReceived = Lists.newArrayList();

    @Subscribe
    public void listenForEverything(Object event) {
      eventsReceived.add(event);
    }
  }
}
