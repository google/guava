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

import static org.junit.contrib.truth.Truth.ASSERT;

import com.google.common.collect.Lists;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

import junit.framework.TestCase;

import java.util.List;

/**
 * Test that EventBus finds the correct handlers.
 *
 * This test must be outside the c.g.c.eventbus package to test correctly.
 * @author Louis Wasserman
 */
public class AnnotatedHandlerFinderTests {

  private static final Object EVENT = new Object();

  abstract static class AbstractEventBusTest<H> extends TestCase {
    abstract H createHandler();

    private H handler;

    H getHandler() {
      return handler;
    }

    @Override
    protected void setUp() throws Exception {
      handler = createHandler();
      EventBus bus = new EventBus();
      bus.register(handler);
      bus.post(EVENT);
    }

    @Override
    protected void tearDown() throws Exception {
      handler = null;
    }
  }

  /*
   * We break the tests up based on whether they are annotated or abstract in the superclass.
   */
  public static class BaseHandlerFinderTest extends
      AbstractEventBusTest<BaseHandlerFinderTest.Handler> {
    static class Handler {
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
      ASSERT.that(getHandler().nonSubscriberEvents).isEmpty();
    }

    public void testSubscriber() {
      ASSERT.that(getHandler().subscriberEvents).hasContentsInOrder(EVENT);
    }

    @Override
    Handler createHandler() {
      return new Handler();
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
      ASSERT.that(getHandler().overriddenAndAnnotatedInSubclassEvents).hasContentsInOrder(EVENT);
    }

    public void testOverriddenNotAnnotatedInSubclass() {
      ASSERT.that(getHandler().overriddenInSubclassEvents).hasContentsInOrder(EVENT);
    }

    @Override
    SubClass createHandler() {
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
      ASSERT.that(getHandler().notOverriddenInSubclassEvents).hasContentsInOrder(EVENT);
    }

    public void testOverriddenNotAnnotatedInSubclass() {
      ASSERT.that(getHandler().overriddenNotAnnotatedInSubclassEvents).hasContentsInOrder(EVENT);
    }

    public void testDifferentlyOverriddenNotAnnotatedInSubclass() {
      ASSERT
          .that(getHandler().differentlyOverriddenNotAnnotatedInSubclassGoodEvents)
          .hasContentsInOrder(EVENT);
      ASSERT.that(getHandler().differentlyOverriddenNotAnnotatedInSubclassBadEvents).isEmpty();
    }

    public void testOverriddenAndAnnotatedInSubclass() {
      ASSERT.that(getHandler().overriddenAndAnnotatedInSubclassEvents).hasContentsInOrder(EVENT);
    }

    public void testDifferentlyOverriddenAndAnnotatedInSubclass() {
      ASSERT
          .that(getHandler().differentlyOverriddenAnnotatedInSubclassGoodEvents)
          .hasContentsInOrder(EVENT);
      ASSERT.that(getHandler().differentlyOverriddenAnnotatedInSubclassBadEvents).isEmpty();
    }

    @Override
    SubClass createHandler() {
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
      ASSERT.that(getHandler().overriddenAndAnnotatedInSubclassEvents).hasContentsInOrder(EVENT);
    }

    public void testOverriddenInSubclassNowhereAnnotated() {
      ASSERT.that(getHandler().overriddenInSubclassNowhereAnnotatedEvents).isEmpty();
    }

    @Override
    SubClass createHandler() {
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
      ASSERT.that(getHandler().neitherOverriddenNorAnnotatedEvents).isEmpty();
    }

    public void testOverriddenInSubclassNowhereAnnotated() {
      ASSERT.that(getHandler().overriddenInSubclassNowhereAnnotatedEvents).isEmpty();
    }

    public void testOverriddenAndAnnotatedInSubclass() {
      ASSERT.that(getHandler().overriddenAndAnnotatedInSubclassEvents).hasContentsInOrder(EVENT);
    }

    @Override
    SubClass createHandler() {
      return new SubClass();
    }
  }

  public static class DeepInterfaceTest extends
      AbstractEventBusTest<DeepInterfaceTest.HandlerClass> {
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

    static class HandlerClass implements Interface2 {
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
      ASSERT.that(getHandler().annotatedIn1Events).hasContentsInOrder(EVENT);
    }

    public void testAnnotatedIn2() {
      ASSERT.that(getHandler().annotatedIn2Events).hasContentsInOrder(EVENT);
    }

    public void testAnnotatedIn1And2() {
      ASSERT.that(getHandler().annotatedIn1And2Events).hasContentsInOrder(EVENT);
    }

    public void testAnnotatedIn1And2AndClass() {
      ASSERT.that(getHandler().annotatedIn1And2AndClassEvents).hasContentsInOrder(EVENT);
    }

    public void testDeclaredIn1AnnotatedIn2() {
      ASSERT.that(getHandler().declaredIn1AnnotatedIn2Events).hasContentsInOrder(EVENT);
    }

    public void testDeclaredIn1AnnotatedInClass() {
      ASSERT.that(getHandler().declaredIn1AnnotatedInClassEvents).hasContentsInOrder(EVENT);
    }

    public void testDeclaredIn2AnnotatedInClass() {
      ASSERT.that(getHandler().declaredIn2AnnotatedInClassEvents).hasContentsInOrder(EVENT);
    }

    public void testNowhereAnnotated() {
      ASSERT.that(getHandler().nowhereAnnotatedEvents).isEmpty();
    }

    @Override
    HandlerClass createHandler() {
      return new HandlerClass();
    }
  }
}
