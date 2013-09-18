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

import static org.truth0.Truth.ASSERT;

import com.google.common.collect.Lists;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

import junit.framework.TestCase;

import java.util.List;

/**
 * Test that EventBus finds the correct subscribers.
 *
 * This test must be outside the c.g.c.eventbus package to test correctly.
 * @author Louis Wasserman
 */
public class AnnotatedSubscriberFinderTests {

  private static final Object EVENT = new Object();

  abstract static class AbstractEventBusTest<H> extends TestCase {
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

  /*
   * We break the tests up based on whether they are annotated or abstract in the superclass.
   */
  public static class BaseSubscriberFinderTest extends
      AbstractEventBusTest<BaseSubscriberFinderTest.Subscriber> {
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
      ASSERT.that(getSubscriber().nonSubscriberEvents).isEmpty();
    }

    public void testSubscriber() {
      ASSERT.that(getSubscriber().subscriberEvents).has().item(EVENT);
    }

    @Override
    Subscriber createSubscriber() {
      return new Subscriber();
    }
  }

  public static class AnnotatedAndAbstractInSuperclassTest extends
      AbstractEventBusTest<AnnotatedAndAbstractInSuperclassTest.SubClass> {
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
      ASSERT.that(getSubscriber().overriddenAndAnnotatedInSubclassEvents).has().item(EVENT);
    }

    public void testOverriddenNotAnnotatedInSubclass() {
      ASSERT.that(getSubscriber().overriddenInSubclassEvents).has().item(EVENT);
    }

    @Override
    SubClass createSubscriber() {
      return new SubClass();
    }
  }

  public static class AnnotatedNotAbstractInSuperclassTest extends
      AbstractEventBusTest<AnnotatedNotAbstractInSuperclassTest.SubClass> {
    static class SuperClass {
      final List<Object> notOverriddenInSubclassEvents = Lists.newArrayList();
      final List<Object> overriddenNotAnnotatedInSubclassEvents = Lists.newArrayList();
      final List<Object> overriddenAndAnnotatedInSubclassEvents = Lists.newArrayList();
      final List<Object> differentlyOverriddenNotAnnotatedInSubclassBadEvents = Lists
          .newArrayList();
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
      final List<Object> differentlyOverriddenNotAnnotatedInSubclassGoodEvents = Lists
          .newArrayList();
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
      ASSERT.that(getSubscriber().notOverriddenInSubclassEvents).has().item(EVENT);
    }

    public void testOverriddenNotAnnotatedInSubclass() {
      ASSERT.that(getSubscriber().overriddenNotAnnotatedInSubclassEvents).has().item(EVENT);
    }

    public void testDifferentlyOverriddenNotAnnotatedInSubclass() {
      ASSERT
          .that(getSubscriber().differentlyOverriddenNotAnnotatedInSubclassGoodEvents)
          .has().item(EVENT);
      ASSERT.that(getSubscriber().differentlyOverriddenNotAnnotatedInSubclassBadEvents).isEmpty();
    }

    public void testOverriddenAndAnnotatedInSubclass() {
      ASSERT.that(getSubscriber().overriddenAndAnnotatedInSubclassEvents).has().item(EVENT);
    }

    public void testDifferentlyOverriddenAndAnnotatedInSubclass() {
      ASSERT
          .that(getSubscriber().differentlyOverriddenAnnotatedInSubclassGoodEvents)
          .has().item(EVENT);
      ASSERT.that(getSubscriber().differentlyOverriddenAnnotatedInSubclassBadEvents).isEmpty();
    }

    @Override
    SubClass createSubscriber() {
      return new SubClass();
    }
  }

  public static class AbstractNotAnnotatedInSuperclassTest extends
      AbstractEventBusTest<AbstractNotAnnotatedInSuperclassTest.SubClass> {
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
      ASSERT.that(getSubscriber().overriddenAndAnnotatedInSubclassEvents).has().item(EVENT);
    }

    public void testOverriddenInSubclassNowhereAnnotated() {
      ASSERT.that(getSubscriber().overriddenInSubclassNowhereAnnotatedEvents).isEmpty();
    }

    @Override
    SubClass createSubscriber() {
      return new SubClass();
    }
  }

  public static class NeitherAbstractNorAnnotatedInSuperclassTest extends
      AbstractEventBusTest<NeitherAbstractNorAnnotatedInSuperclassTest.SubClass> {
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
      ASSERT.that(getSubscriber().neitherOverriddenNorAnnotatedEvents).isEmpty();
    }

    public void testOverriddenInSubclassNowhereAnnotated() {
      ASSERT.that(getSubscriber().overriddenInSubclassNowhereAnnotatedEvents).isEmpty();
    }

    public void testOverriddenAndAnnotatedInSubclass() {
      ASSERT.that(getSubscriber().overriddenAndAnnotatedInSubclassEvents).has().item(EVENT);
    }

    @Override
    SubClass createSubscriber() {
      return new SubClass();
    }
  }

  public static class DeepInterfaceTest extends
      AbstractEventBusTest<DeepInterfaceTest.SubscriberClass> {
    interface Interface1 {
      @Subscribe
      void annotatedIn1(Object o);

      @Subscribe
      void annotatedIn1And2(Object o);

      @Subscribe
      void annotatedIn1And2AndClass(Object o);

      void declaredIn1AnnotatedIn2(Object o);

      void declaredIn1AnnotatedInClass(Object o);

      void nowhereAnnotated(Object o);
    }

    interface Interface2 extends Interface1 {
      @Override
      @Subscribe
      void declaredIn1AnnotatedIn2(Object o);

      @Override
      @Subscribe
      void annotatedIn1And2(Object o);

      @Override
      @Subscribe
      void annotatedIn1And2AndClass(Object o);

      void declaredIn2AnnotatedInClass(Object o);

      @Subscribe
      void annotatedIn2(Object o);
    }

    static class SubscriberClass implements Interface2 {
      final List<Object> annotatedIn1Events = Lists.newArrayList();
      final List<Object> annotatedIn1And2Events = Lists.newArrayList();
      final List<Object> annotatedIn1And2AndClassEvents = Lists.newArrayList();
      final List<Object> declaredIn1AnnotatedIn2Events = Lists.newArrayList();
      final List<Object> declaredIn1AnnotatedInClassEvents = Lists.newArrayList();
      final List<Object> declaredIn2AnnotatedInClassEvents = Lists.newArrayList();
      final List<Object> annotatedIn2Events = Lists.newArrayList();
      final List<Object> nowhereAnnotatedEvents = Lists.newArrayList();

      @Override
      public void annotatedIn1(Object o) {
        annotatedIn1Events.add(o);
      }

      @Subscribe
      @Override
      public void declaredIn1AnnotatedInClass(Object o) {
        declaredIn1AnnotatedInClassEvents.add(o);
      }

      @Override
      public void declaredIn1AnnotatedIn2(Object o) {
        declaredIn1AnnotatedIn2Events.add(o);
      }

      @Override
      public void annotatedIn1And2(Object o) {
        annotatedIn1And2Events.add(o);
      }

      @Subscribe
      @Override
      public void annotatedIn1And2AndClass(Object o) {
        annotatedIn1And2AndClassEvents.add(o);
      }

      @Subscribe
      @Override
      public void declaredIn2AnnotatedInClass(Object o) {
        declaredIn2AnnotatedInClassEvents.add(o);
      }

      @Override
      public void annotatedIn2(Object o) {
        annotatedIn2Events.add(o);
      }

      @Override
      public void nowhereAnnotated(Object o) {
        nowhereAnnotatedEvents.add(o);
      }
    }

    public void testAnnotatedIn1() {
      ASSERT.that(getSubscriber().annotatedIn1Events).has().item(EVENT);
    }

    public void testAnnotatedIn2() {
      ASSERT.that(getSubscriber().annotatedIn2Events).has().item(EVENT);
    }

    public void testAnnotatedIn1And2() {
      ASSERT.that(getSubscriber().annotatedIn1And2Events).has().item(EVENT);
    }

    public void testAnnotatedIn1And2AndClass() {
      ASSERT.that(getSubscriber().annotatedIn1And2AndClassEvents).has().item(EVENT);
    }

    public void testDeclaredIn1AnnotatedIn2() {
      ASSERT.that(getSubscriber().declaredIn1AnnotatedIn2Events).has().item(EVENT);
    }

    public void testDeclaredIn1AnnotatedInClass() {
      ASSERT.that(getSubscriber().declaredIn1AnnotatedInClassEvents).has().item(EVENT);
    }

    public void testDeclaredIn2AnnotatedInClass() {
      ASSERT.that(getSubscriber().declaredIn2AnnotatedInClassEvents).has().item(EVENT);
    }

    public void testNowhereAnnotated() {
      ASSERT.that(getSubscriber().nowhereAnnotatedEvents).isEmpty();
    }

    @Override
    SubscriberClass createSubscriber() {
      return new SubscriberClass();
    }
  }
}
