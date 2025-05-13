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

import com.google.common.eventbus.Subscribe;
import com.google.common.eventbus.outside.AnnotatedNotAbstractInSuperclassTest.SubClass;
import java.util.ArrayList;
import java.util.List;

public class AnnotatedNotAbstractInSuperclassTest extends AbstractEventBusTest<SubClass> {
  static class SuperClass {
    final List<Object> notOverriddenInSubclassEvents = new ArrayList<>();
    final List<Object> overriddenNotAnnotatedInSubclassEvents = new ArrayList<>();
    final List<Object> overriddenAndAnnotatedInSubclassEvents = new ArrayList<>();
    final List<Object> differentlyOverriddenNotAnnotatedInSubclassBadEvents = new ArrayList<>();
    final List<Object> differentlyOverriddenAnnotatedInSubclassBadEvents = new ArrayList<>();

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
    final List<Object> differentlyOverriddenNotAnnotatedInSubclassGoodEvents = new ArrayList<>();
    final List<Object> differentlyOverriddenAnnotatedInSubclassGoodEvents = new ArrayList<>();

    @Override
    public void overriddenNotAnnotatedInSubclass(Object o) {
      super.overriddenNotAnnotatedInSubclass(o);
    }

    @Subscribe
    @Override
    // We are testing how we treat an override with the same behavior and annotations.
    @SuppressWarnings("RedundantOverride")
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
