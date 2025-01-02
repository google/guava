/*
 * Copyright (C) 2010 The Guava Authors
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
import static com.google.common.collect.testing.features.CollectionSize.ONE;
import static com.google.common.collect.testing.features.CollectionSize.SEVERAL;
import static com.google.common.collect.testing.features.CollectionSize.ZERO;
import static com.google.common.collect.testing.testers.ReflectionFreeAssertThrows.assertThrows;
import static java.util.Collections.sort;

import com.google.common.annotations.GwtCompatible;
import com.google.common.collect.testing.features.CollectionSize;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.SortedSet;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.junit.Ignore;

/**
 * A generic JUnit test which tests operations on a SortedSet. Can't be invoked directly; please see
 * {@code SortedSetTestSuiteBuilder}.
 *
 * @author Jesse Wilson
 * @author Louis Wasserman
 */
@GwtCompatible
@Ignore("test runners must not instantiate and run this directly, only via suites we build")
// @Ignore affects the Android test runner, which respects JUnit 4 annotations on JUnit 3 tests.
@SuppressWarnings("JUnit4ClassUsedInJUnit3")
@NullMarked
public class SortedSetNavigationTester<E extends @Nullable Object> extends AbstractSetTester<E> {

  private SortedSet<E> sortedSet;
  private List<E> values;
  private @Nullable E a;
  private @Nullable E b;
  private @Nullable E c;

  @Override
  public void setUp() throws Exception {
    super.setUp();
    sortedSet = (SortedSet<E>) getSet();
    values =
        copyToList(
            getSubjectGenerator()
                .getSampleElements(getSubjectGenerator().getCollectionSize().getNumElements()));
    sort(values, sortedSet.comparator());

    // some tests assume SEVERAL == 3
    if (values.size() >= 1) {
      a = values.get(0);
      if (values.size() >= 3) {
        b = values.get(1);
        c = values.get(2);
      }
    }
  }

  @CollectionSize.Require(ZERO)
  public void testEmptySetFirst() {
    assertThrows(NoSuchElementException.class, () -> sortedSet.first());
  }

  @CollectionSize.Require(ZERO)
  public void testEmptySetLast() {
    assertThrows(NoSuchElementException.class, () -> sortedSet.last());
  }

  @CollectionSize.Require(ONE)
  public void testSingletonSetFirst() {
    assertEquals(a, sortedSet.first());
  }

  @CollectionSize.Require(ONE)
  public void testSingletonSetLast() {
    assertEquals(a, sortedSet.last());
  }

  @CollectionSize.Require(SEVERAL)
  public void testFirst() {
    assertEquals(a, sortedSet.first());
  }

  @CollectionSize.Require(SEVERAL)
  public void testLast() {
    assertEquals(c, sortedSet.last());
  }
}
