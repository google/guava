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
import com.google.common.eventbus.outside.AbstractNotAnnotatedInSuperclassTest.SubClass;
import java.util.List;

public class AbstractNotAnnotatedInSuperclassTest extends AbstractEventBusTest<SubClass> {
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
