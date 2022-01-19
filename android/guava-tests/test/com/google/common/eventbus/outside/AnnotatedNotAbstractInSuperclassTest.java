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
public class AnnotatedNotAbstractInSuperclassTest
    extends AbstractEventBusTest<AnnotatedNotAbstractInSuperclassTest.SubClass> {

  static class SuperClass {

    final List<Object> notOverriddenInSubclassEvents = Lists.newArrayList();
    final List<Object> overriddenNotAnnotatedInSubclassEvents = Lists.newArrayList();
    final List<Object> overriddenAndAnnotatedInSubclassEvents = Lists.newArrayList();
    final List<Object> differentlyOverriddenNotAnnotatedInSubclassBadEvents = Lists.newArrayList();
    final List<Object> differentlyOverriddenAnnotatedInSubclassBadEvents = Lists.newArrayList();

    @Subscribe
    public void notOverriddenInSubclass(Object o) {
      notOverriddenInSubclassEvents.add(o);
    }

    @Subscribe
    public void overriddenNotAnnotatedInSubclass(Object o) {
      overriddenNotAnnotatedInSubclassEvents.add(o);
    }

    @Subscribe
    public void overriddenAndAnnotatedInSubclass(Object o) {
      overriddenAndAnnotatedInSubclassEvents.add(o);
    }

    @Subscribe
    public void differentlyOverriddenNotAnnotatedInSubclass(Object o) {
      // the subclass overrides this and does *not* call super.dONAIS(o)
      differentlyOverriddenNotAnnotatedInSubclassBadEvents.add(o);
    }

    @Subscribe
    public void differentlyOverriddenAnnotatedInSubclass(Object o) {
      // the subclass overrides this and does *not* call super.dOAIS(o)
      differentlyOverriddenAnnotatedInSubclassBadEvents.add(o);
    }
  }

  static class SubClass extends SuperClass {

    final List<Object> differentlyOverriddenNotAnnotatedInSubclassGoodEvents = Lists.newArrayList();
    final List<Object> differentlyOverriddenAnnotatedInSubclassGoodEvents = Lists.newArrayList();

    @Override
    public void overriddenNotAnnotatedInSubclass(Object o) {
      super.overriddenNotAnnotatedInSubclass(o);
    }

    @Subscribe
    @Override
    public void overriddenAndAnnotatedInSubclass(Object o) {
      super.overriddenAndAnnotatedInSubclass(o);
    }

    @Override
    public void differentlyOverriddenNotAnnotatedInSubclass(Object o) {
      differentlyOverriddenNotAnnotatedInSubclassGoodEvents.add(o);
    }

    @Subscribe
    @Override
    public void differentlyOverriddenAnnotatedInSubclass(Object o) {
      differentlyOverriddenAnnotatedInSubclassGoodEvents.add(o);
    }
  }

  public void testNotOverriddenInSubclass() {
    assertThat(getSubscriber().notOverriddenInSubclassEvents).contains(EVENT);
  }

  public void testOverriddenNotAnnotatedInSubclass() {
    assertThat(getSubscriber().overriddenNotAnnotatedInSubclassEvents).contains(EVENT);
  }

  public void testDifferentlyOverriddenNotAnnotatedInSubclass() {
    assertThat(getSubscriber().differentlyOverriddenNotAnnotatedInSubclassGoodEvents)
        .contains(EVENT);
    assertThat(getSubscriber().differentlyOverriddenNotAnnotatedInSubclassBadEvents).isEmpty();
  }

  public void testOverriddenAndAnnotatedInSubclass() {
    assertThat(getSubscriber().overriddenAndAnnotatedInSubclassEvents).contains(EVENT);
  }

  public void testDifferentlyOverriddenAndAnnotatedInSubclass() {
    assertThat(getSubscriber().differentlyOverriddenAnnotatedInSubclassGoodEvents).contains(EVENT);
    assertThat(getSubscriber().differentlyOverriddenAnnotatedInSubclassBadEvents).isEmpty();
  }

  @Override
  SubClass createSubscriber() {
    return new SubClass();
  }
}
