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
public class NeitherAbstractNorAnnotatedInSuperclassTest
    extends AbstractEventBusTest<NeitherAbstractNorAnnotatedInSuperclassTest.SubClass> {

  static class SuperClass {

    final List<Object> neitherOverriddenNorAnnotatedEvents = Lists.newArrayList();
    final List<Object> overriddenInSubclassNowhereAnnotatedEvents = Lists.newArrayList();
    final List<Object> overriddenAndAnnotatedInSubclassEvents = Lists.newArrayList();

    public void neitherOverriddenNorAnnotated(Object o) {
      neitherOverriddenNorAnnotatedEvents.add(o);
    }

    public void overriddenInSubclassNowhereAnnotated(Object o) {
      overriddenInSubclassNowhereAnnotatedEvents.add(o);
    }

    public void overriddenAndAnnotatedInSubclass(Object o) {
      overriddenAndAnnotatedInSubclassEvents.add(o);
    }
  }

  static class SubClass extends SuperClass {

    @Override
    public void overriddenInSubclassNowhereAnnotated(Object o) {
      super.overriddenInSubclassNowhereAnnotated(o);
    }

    @Subscribe
    @Override
    public void overriddenAndAnnotatedInSubclass(Object o) {
      super.overriddenAndAnnotatedInSubclass(o);
    }
  }

  public void testNeitherOverriddenNorAnnotated() {
    assertThat(getSubscriber().neitherOverriddenNorAnnotatedEvents).isEmpty();
  }

  public void testOverriddenInSubclassNowhereAnnotated() {
    assertThat(getSubscriber().overriddenInSubclassNowhereAnnotatedEvents).isEmpty();
  }

  public void testOverriddenAndAnnotatedInSubclass() {
    assertThat(getSubscriber().overriddenAndAnnotatedInSubclassEvents).contains(EVENT);
  }

  @Override
  SubClass createSubscriber() {
    return new SubClass();
  }
}
