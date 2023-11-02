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

import com.google.common.collect.Lists;
import com.google.common.eventbus.Subscribe;
import com.google.common.eventbus.outside.NeitherAbstractNorAnnotatedInSuperclassTest.SubClass;
import java.util.List;

public class NeitherAbstractNorAnnotatedInSuperclassTest extends AbstractEventBusTest<SubClass> {
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
