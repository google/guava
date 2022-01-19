package com.google.common.eventbus.outside;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.Lists;
import com.google.common.eventbus.Subscribe;
import java.util.List;

/*
 * We break the tests up based on whether they are annotated or abstract in the superclass.
 */
public class BaseSubscriberFinderTest
    extends AbstractEventBusTest<BaseSubscriberFinderTest.Subscriber> {

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
