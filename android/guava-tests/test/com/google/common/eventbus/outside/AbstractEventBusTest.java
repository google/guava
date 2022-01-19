package com.google.common.eventbus.outside;

import com.google.common.eventbus.EventBus;
import junit.framework.TestCase;

/**
 * Test that EventBus finds the correct subscribers.
 *
 * <p>This test must be outside the c.g.c.eventbus package to test correctly.
 *
 * @author Louis Wasserman
 */
abstract class AbstractEventBusTest<H> extends TestCase {

  static final Object EVENT = new Object();

  abstract H createSubscriber();

  private H subscriber;

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
