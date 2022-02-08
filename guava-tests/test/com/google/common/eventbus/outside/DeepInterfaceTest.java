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
import com.google.common.eventbus.outside.DeepInterfaceTest.SubscriberClass;
import java.util.List;

public class DeepInterfaceTest extends AbstractEventBusTest<SubscriberClass> {
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
    assertThat(getSubscriber().annotatedIn1Events).contains(EVENT);
  }

  public void testAnnotatedIn2() {
    assertThat(getSubscriber().annotatedIn2Events).contains(EVENT);
  }

  public void testAnnotatedIn1And2() {
    assertThat(getSubscriber().annotatedIn1And2Events).contains(EVENT);
  }

  public void testAnnotatedIn1And2AndClass() {
    assertThat(getSubscriber().annotatedIn1And2AndClassEvents).contains(EVENT);
  }

  public void testDeclaredIn1AnnotatedIn2() {
    assertThat(getSubscriber().declaredIn1AnnotatedIn2Events).contains(EVENT);
  }

  public void testDeclaredIn1AnnotatedInClass() {
    assertThat(getSubscriber().declaredIn1AnnotatedInClassEvents).contains(EVENT);
  }

  public void testDeclaredIn2AnnotatedInClass() {
    assertThat(getSubscriber().declaredIn2AnnotatedInClassEvents).contains(EVENT);
  }

  public void testNowhereAnnotated() {
    assertThat(getSubscriber().nowhereAnnotatedEvents).isEmpty();
  }

  @Override
  SubscriberClass createSubscriber() {
    return new SubscriberClass();
  }
}
