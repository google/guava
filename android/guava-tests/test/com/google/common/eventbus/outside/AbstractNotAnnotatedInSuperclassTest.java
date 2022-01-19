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
public class AbstractNotAnnotatedInSuperclassTest
    extends AbstractEventBusTest<AbstractNotAnnotatedInSuperclassTest.SubClass> {

  abstract static class SuperClass {

    public abstract void overriddenInSubclassNowhereAnnotated(Object o);

    public abstract void overriddenAndAnnotatedInSubclass(Object o);
  }

  static class SubClass extends SuperClass {

    final List<Object> overriddenInSubclassNowhereAnnotatedEvents = Lists.newArrayList();
    final List<Object> overriddenAndAnnotatedInSubclassEvents = Lists.newArrayList();

    @Override
    public void overriddenInSubclassNowhereAnnotated(Object o) {
      overriddenInSubclassNowhereAnnotatedEvents.add(o);
    }

    @Subscribe
    @Override
    public void overriddenAndAnnotatedInSubclass(Object o) {
      overriddenAndAnnotatedInSubclassEvents.add(o);
    }
  }

  public void testOverriddenAndAnnotatedInSubclass() {
    assertThat(getSubscriber().overriddenAndAnnotatedInSubclassEvents).contains(EVENT);
  }

  public void testOverriddenInSubclassNowhereAnnotated() {
    assertThat(getSubscriber().overriddenInSubclassNowhereAnnotatedEvents).isEmpty();
  }

  @Override
  SubClass createSubscriber() {
    return new SubClass();
  }
}
