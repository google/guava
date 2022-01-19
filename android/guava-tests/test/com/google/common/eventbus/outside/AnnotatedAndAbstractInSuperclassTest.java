package com.google.common.eventbus.outside;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.Lists;
import com.google.common.eventbus.Subscribe;
import java.util.List;

/**
 * Test that EventBus finds the correct subscribers.
 *
 * <p>This test must be outside the c.g.c.eventbus package to test correctly.
 *
 * @author Louis Wasserman
 */
public class AnnotatedAndAbstractInSuperclassTest
    extends AbstractEventBusTest<AnnotatedAndAbstractInSuperclassTest.SubClass> {

  abstract static class SuperClass {

    @Subscribe
    public abstract void overriddenAndAnnotatedInSubclass(Object o);

    @Subscribe
    public abstract void overriddenInSubclass(Object o);
  }

  static class SubClass extends SuperClass {

    final List<Object> overriddenAndAnnotatedInSubclassEvents = Lists.newArrayList();
    final List<Object> overriddenInSubclassEvents = Lists.newArrayList();

    @Subscribe
    @Override
    public void overriddenAndAnnotatedInSubclass(Object o) {
      overriddenAndAnnotatedInSubclassEvents.add(o);
    }

    @Override
    public void overriddenInSubclass(Object o) {
      overriddenInSubclassEvents.add(o);
    }
  }

  public void testOverriddenAndAnnotatedInSubclass() {
    assertThat(getSubscriber().overriddenAndAnnotatedInSubclassEvents).contains(EVENT);
  }

  public void testOverriddenNotAnnotatedInSubclass() {
    assertThat(getSubscriber().overriddenInSubclassEvents).contains(EVENT);
  }

  @Override
  SubClass createSubscriber() {
    return new SubClass();
  }
}
