/*
 * Copyright (C) 2011 The Guava Authors
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.common.collect.testing.google;

import static com.google.common.collect.testing.features.CollectionFeature.KNOWN_ORDER;
import static com.google.common.collect.testing.features.CollectionFeature.SUPPORTS_ITERATOR_REMOVE;

import com.google.common.annotations.GwtCompatible;
import com.google.common.collect.testing.IteratorFeature;
import com.google.common.collect.testing.IteratorTester;
import com.google.common.collect.testing.features.CollectionFeature;

import java.util.Arrays;
import java.util.Iterator;

/**
 * Tester to make sure the {@code iterator().remove()} implementation of {@code Multiset} works when
 * there are multiple occurrences of elements.
 * 
 * @author Louis Wasserman
 */
@GwtCompatible(emulated = true)
public class MultisetIteratorTester<E> extends AbstractMultisetTester<E> {
  @SuppressWarnings("unchecked")
  @CollectionFeature.Require({SUPPORTS_ITERATOR_REMOVE, KNOWN_ORDER})
  public void testRemovingIteratorKnownOrder() {
    new IteratorTester<E>(4, IteratorFeature.MODIFIABLE, getSubjectGenerator().order(
        Arrays.asList(samples.e0, samples.e1, samples.e1, samples.e2)),
        IteratorTester.KnownOrder.KNOWN_ORDER) {
      @Override
      protected Iterator<E> newTargetIterator() {
        return getSubjectGenerator().create(samples.e0, samples.e1, samples.e1, samples.e2)
            .iterator();
      }
    }.test();
  }

  @SuppressWarnings("unchecked")
  @CollectionFeature.Require(value = SUPPORTS_ITERATOR_REMOVE, absent = KNOWN_ORDER)
  public void testRemovingIteratorUnknownOrder() {
    new IteratorTester<E>(4, IteratorFeature.MODIFIABLE, Arrays.asList(samples.e0, samples.e1,
        samples.e1, samples.e2), IteratorTester.KnownOrder.UNKNOWN_ORDER) {
      @Override
      protected Iterator<E> newTargetIterator() {
        return getSubjectGenerator().create(samples.e0, samples.e1, samples.e1, samples.e2)
            .iterator();
      }
    }.test();
  }

  @SuppressWarnings("unchecked")
  @CollectionFeature.Require(value = KNOWN_ORDER, absent = SUPPORTS_ITERATOR_REMOVE)
  public void testIteratorKnownOrder() {
    new IteratorTester<E>(4, IteratorFeature.UNMODIFIABLE, getSubjectGenerator().order(
        Arrays.asList(samples.e0, samples.e1, samples.e1, samples.e2)),
        IteratorTester.KnownOrder.KNOWN_ORDER) {
      @Override
      protected Iterator<E> newTargetIterator() {
        return getSubjectGenerator().create(samples.e0, samples.e1, samples.e1, samples.e2)
            .iterator();
      }
    }.test();
  }

  @SuppressWarnings("unchecked")
  @CollectionFeature.Require(absent = {SUPPORTS_ITERATOR_REMOVE, KNOWN_ORDER})
  public void testIteratorUnknownOrder() {
    new IteratorTester<E>(4, IteratorFeature.UNMODIFIABLE, Arrays.asList(samples.e0, samples.e1,
        samples.e1, samples.e2), IteratorTester.KnownOrder.UNKNOWN_ORDER) {
      @Override
      protected Iterator<E> newTargetIterator() {
        return getSubjectGenerator().create(samples.e0, samples.e1, samples.e1, samples.e2)
            .iterator();
      }
    }.test();
  }
}

