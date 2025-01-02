/*
 * Copyright (C) 2007 The Guava Authors
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

package com.google.common.collect.testing.testers;

import static com.google.common.collect.testing.Helpers.copyToList;
import static com.google.common.collect.testing.Helpers.getMethod;
import static com.google.common.collect.testing.IteratorFeature.MODIFIABLE;
import static com.google.common.collect.testing.IteratorFeature.UNMODIFIABLE;
import static com.google.common.collect.testing.features.CollectionFeature.SUPPORTS_REMOVE;
import static com.google.common.collect.testing.features.ListFeature.SUPPORTS_ADD_WITH_INDEX;
import static com.google.common.collect.testing.features.ListFeature.SUPPORTS_SET;
import static com.google.common.collect.testing.testers.Platform.listListIteratorTesterNumIterations;
import static com.google.common.collect.testing.testers.ReflectionFreeAssertThrows.assertThrows;
import static java.util.Collections.singleton;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.annotations.J2ktIncompatible;
import com.google.common.collect.testing.IteratorFeature;
import com.google.common.collect.testing.ListIteratorTester;
import com.google.common.collect.testing.features.CollectionFeature;
import com.google.common.collect.testing.features.ListFeature;
import java.lang.reflect.Method;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.junit.Ignore;

/**
 * A generic JUnit test which tests {@code listIterator} operations on a list. Can't be invoked
 * directly; please see {@link com.google.common.collect.testing.ListTestSuiteBuilder}.
 *
 * @author Chris Povirk
 * @author Kevin Bourrillion
 */
@GwtCompatible(emulated = true)
@Ignore("test runners must not instantiate and run this directly, only via suites we build")
// @Ignore affects the Android test runner, which respects JUnit 4 annotations on JUnit 3 tests.
@SuppressWarnings("JUnit4ClassUsedInJUnit3")
@NullMarked
public class ListListIteratorTester<E extends @Nullable Object> extends AbstractListTester<E> {
  @CollectionFeature.Require(absent = SUPPORTS_REMOVE)
  @ListFeature.Require(absent = {SUPPORTS_SET, SUPPORTS_ADD_WITH_INDEX})
  public void testListIterator_unmodifiable() {
    runListIteratorTest(UNMODIFIABLE);
  }

  /*
   * For now, we don't cope with testing this when the list supports only some
   * modification operations.
   */
  @CollectionFeature.Require(SUPPORTS_REMOVE)
  @ListFeature.Require({SUPPORTS_SET, SUPPORTS_ADD_WITH_INDEX})
  public void testListIterator_fullyModifiable() {
    runListIteratorTest(MODIFIABLE);
  }

  private void runListIteratorTest(Set<IteratorFeature> features) {
    new ListIteratorTester<E>(
        listListIteratorTesterNumIterations(),
        singleton(e4()),
        features,
        copyToList(getOrderedElements()),
        0) {
      @Override
      protected ListIterator<E> newTargetIterator() {
        resetCollection();
        return getList().listIterator();
      }

      @Override
      protected void verify(List<E> elements) {
        expectContents(elements);
      }
    }.test();
  }

  public void testListIterator_tooLow() {
    assertThrows(IndexOutOfBoundsException.class, () -> getList().listIterator(-1));
  }

  public void testListIterator_tooHigh() {
    assertThrows(
        IndexOutOfBoundsException.class, () -> getList().listIterator(getNumElements() + 1));
  }

  public void testListIterator_atSize() {
    getList().listIterator(getNumElements());
    // TODO: run the iterator through ListIteratorTester
  }

  /**
   * Returns the {@link Method} instance for {@link #testListIterator_fullyModifiable()} so that
   * tests of {@link CopyOnWriteArraySet} can suppress it with {@code
   * FeatureSpecificTestSuiteBuilder.suppressing()} until <a
   * href="https://bugs.openjdk.org/browse/JDK-6570575">JDK-6570575</a> is fixed.
   */
  @J2ktIncompatible
  @GwtIncompatible // reflection
  public static Method getListIteratorFullyModifiableMethod() {
    return getMethod(ListListIteratorTester.class, "testListIterator_fullyModifiable");
  }

  /**
   * Returns the {@link Method} instance for {@link #testListIterator_unmodifiable()} so that it can
   * be suppressed in GWT tests.
   */
  @J2ktIncompatible
  @GwtIncompatible // reflection
  public static Method getListIteratorUnmodifiableMethod() {
    return getMethod(ListListIteratorTester.class, "testListIterator_unmodifiable");
  }
}
