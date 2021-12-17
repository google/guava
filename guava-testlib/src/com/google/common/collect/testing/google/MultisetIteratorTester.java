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

import static com.google.common.collect.testing.IteratorFeature.MODIFIABLE;
import static com.google.common.collect.testing.IteratorFeature.UNMODIFIABLE;
import static com.google.common.collect.testing.features.CollectionFeature.KNOWN_ORDER;
import static com.google.common.collect.testing.features.CollectionFeature.SUPPORTS_ITERATOR_REMOVE;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.collect.testing.Helpers;
import com.google.common.collect.testing.IteratorTester;
import com.google.common.collect.testing.features.CollectionFeature;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import org.junit.Ignore;

/**
 * Tester to make sure the {@code iterator().remove()} implementation of {@code Multiset} works when
 * there are multiple occurrences of elements.
 *
 * @author Louis Wasserman
 */
@GwtCompatible(emulated = true)
@Ignore // Affects only Android test runner, which respects JUnit 4 annotations on JUnit 3 tests.
public class MultisetIteratorTester<E> extends AbstractMultisetTester<E> {
  @SuppressWarnings("unchecked")
  @CollectionFeature.Require({SUPPORTS_ITERATOR_REMOVE, KNOWN_ORDER})
  public void testRemovingIteratorKnownOrder() {
    new IteratorTester<E>(
        4,
        MODIFIABLE,
        getSubjectGenerator().order(Arrays.asList(e0(), e1(), e1(), e2())),
        IteratorTester.KnownOrder.KNOWN_ORDER) {
      @Override
      protected Iterator<E> newTargetIterator() {
        return getSubjectGenerator().create(e0(), e1(), e1(), e2()).iterator();
      }
    }.test();
  }

  @SuppressWarnings("unchecked")
  @CollectionFeature.Require(value = SUPPORTS_ITERATOR_REMOVE, absent = KNOWN_ORDER)
  public void testRemovingIteratorUnknownOrder() {
    new IteratorTester<E>(
        4,
        MODIFIABLE,
        Arrays.asList(e0(), e1(), e1(), e2()),
        IteratorTester.KnownOrder.UNKNOWN_ORDER) {
      @Override
      protected Iterator<E> newTargetIterator() {
        return getSubjectGenerator().create(e0(), e1(), e1(), e2()).iterator();
      }
    }.test();
  }

  @SuppressWarnings("unchecked")
  @CollectionFeature.Require(value = KNOWN_ORDER, absent = SUPPORTS_ITERATOR_REMOVE)
  public void testIteratorKnownOrder() {
    new IteratorTester<E>(
        4,
        UNMODIFIABLE,
        getSubjectGenerator().order(Arrays.asList(e0(), e1(), e1(), e2())),
        IteratorTester.KnownOrder.KNOWN_ORDER) {
      @Override
      protected Iterator<E> newTargetIterator() {
        return getSubjectGenerator().create(e0(), e1(), e1(), e2()).iterator();
      }
    }.test();
  }

  @SuppressWarnings("unchecked")
  @CollectionFeature.Require(absent = {SUPPORTS_ITERATOR_REMOVE, KNOWN_ORDER})
  public void testIteratorUnknownOrder() {
    new IteratorTester<E>(
        4,
        UNMODIFIABLE,
        Arrays.asList(e0(), e1(), e1(), e2()),
        IteratorTester.KnownOrder.UNKNOWN_ORDER) {
      @Override
      protected Iterator<E> newTargetIterator() {
        return getSubjectGenerator().create(e0(), e1(), e1(), e2()).iterator();
      }
    }.test();
  }

  /**
   * Returns {@link Method} instances for the tests that assume multisets support duplicates so that
   * the test of {@code Multisets.forSet()} can suppress them.
   */
  @GwtIncompatible // reflection
  public static List<Method> getIteratorDuplicateInitializingMethods() {
    return Arrays.asList(
        Helpers.getMethod(MultisetIteratorTester.class, "testIteratorKnownOrder"),
        Helpers.getMethod(MultisetIteratorTester.class, "testIteratorUnknownOrder"),
        Helpers.getMethod(MultisetIteratorTester.class, "testRemovingIteratorKnownOrder"),
        Helpers.getMethod(MultisetIteratorTester.class, "testRemovingIteratorUnknownOrder"));
  }
}
