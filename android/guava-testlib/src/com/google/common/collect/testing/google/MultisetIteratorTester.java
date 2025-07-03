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

import static com.google.common.collect.testing.Helpers.getMethod;
import static com.google.common.collect.testing.IteratorFeature.MODIFIABLE;
import static com.google.common.collect.testing.IteratorFeature.UNMODIFIABLE;
import static com.google.common.collect.testing.features.CollectionFeature.KNOWN_ORDER;
import static com.google.common.collect.testing.features.CollectionFeature.SUPPORTS_ITERATOR_REMOVE;
import static java.util.Arrays.asList;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.annotations.J2ktIncompatible;
import com.google.common.collect.testing.IteratorTester;
import com.google.common.collect.testing.features.CollectionFeature;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.List;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.junit.Ignore;

/**
 * Tester to make sure the {@code iterator().remove()} implementation of {@code Multiset} works when
 * there are multiple occurrences of elements.
 *
 * @author Louis Wasserman
 */
@GwtCompatible(emulated = true)
@Ignore("test runners must not instantiate and run this directly, only via suites we build")
// @Ignore affects the Android test runner, which respects JUnit 4 annotations on JUnit 3 tests.
@SuppressWarnings("JUnit4ClassUsedInJUnit3")
@NullMarked
public class MultisetIteratorTester<E extends @Nullable Object> extends AbstractMultisetTester<E> {
  @CollectionFeature.Require({SUPPORTS_ITERATOR_REMOVE, KNOWN_ORDER})
  public void testRemovingIteratorKnownOrder() {
    new IteratorTester<E>(
        4,
        MODIFIABLE,
        getSubjectGenerator().order(asList(e0(), e1(), e1(), e2())),
        IteratorTester.KnownOrder.KNOWN_ORDER) {
      @Override
      protected Iterator<E> newTargetIterator() {
        return getSubjectGenerator().create(e0(), e1(), e1(), e2()).iterator();
      }
    }.test();
  }

  @CollectionFeature.Require(value = SUPPORTS_ITERATOR_REMOVE, absent = KNOWN_ORDER)
  public void testRemovingIteratorUnknownOrder() {
    new IteratorTester<E>(
        4, MODIFIABLE, asList(e0(), e1(), e1(), e2()), IteratorTester.KnownOrder.UNKNOWN_ORDER) {
      @Override
      protected Iterator<E> newTargetIterator() {
        return getSubjectGenerator().create(e0(), e1(), e1(), e2()).iterator();
      }
    }.test();
  }

  @CollectionFeature.Require(value = KNOWN_ORDER, absent = SUPPORTS_ITERATOR_REMOVE)
  public void testIteratorKnownOrder() {
    new IteratorTester<E>(
        4,
        UNMODIFIABLE,
        getSubjectGenerator().order(asList(e0(), e1(), e1(), e2())),
        IteratorTester.KnownOrder.KNOWN_ORDER) {
      @Override
      protected Iterator<E> newTargetIterator() {
        return getSubjectGenerator().create(e0(), e1(), e1(), e2()).iterator();
      }
    }.test();
  }

  @CollectionFeature.Require(absent = {SUPPORTS_ITERATOR_REMOVE, KNOWN_ORDER})
  public void testIteratorUnknownOrder() {
    new IteratorTester<E>(
        4, UNMODIFIABLE, asList(e0(), e1(), e1(), e2()), IteratorTester.KnownOrder.UNKNOWN_ORDER) {
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
  @J2ktIncompatible
  @GwtIncompatible // reflection
  public static List<Method> getIteratorDuplicateInitializingMethods() {
    return asList(
        getMethod(MultisetIteratorTester.class, "testIteratorKnownOrder"),
        getMethod(MultisetIteratorTester.class, "testIteratorUnknownOrder"),
        getMethod(MultisetIteratorTester.class, "testRemovingIteratorKnownOrder"),
        getMethod(MultisetIteratorTester.class, "testRemovingIteratorUnknownOrder"));
  }
}
