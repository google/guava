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

import static com.google.common.collect.testing.features.CollectionFeature.SUPPORTS_REMOVE;
import static com.google.common.collect.testing.features.CollectionSize.ONE;
import static com.google.common.collect.testing.features.CollectionSize.SEVERAL;
import static com.google.common.collect.testing.features.CollectionSize.ZERO;

import com.google.common.annotations.GwtIncompatible;
import com.google.common.collect.testing.Helpers;
import com.google.common.collect.testing.features.CollectionFeature;
import com.google.common.collect.testing.features.CollectionSize;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.NavigableSet;
import java.util.TreeSet;
import org.junit.Ignore;

/**
 * A generic JUnit test which tests operations on a NavigableSet. Can't be invoked directly; please
 * see {@code NavigableSetTestSuiteBuilder}.
 *
 * @author Jesse Wilson
 * @author Louis Wasserman
 */
@GwtIncompatible
@Ignore // Affects only Android test runner, which respects JUnit 4 annotations on JUnit 3 tests.
public class NavigableSetNavigationTester<E> extends AbstractSetTester<E> {

  private NavigableSet<E> navigableSet;
  private List<E> values;
  private E a;
  private E b;
  private E c;

  @Override
  public void setUp() throws Exception {
    super.setUp();
    navigableSet = (NavigableSet<E>) getSet();
    values =
        Helpers.copyToList(
            getSubjectGenerator()
                .getSampleElements(getSubjectGenerator().getCollectionSize().getNumElements()));
    Collections.sort(values, navigableSet.comparator());

    // some tests assume SEVERAL == 3
    if (values.size() >= 1) {
      a = values.get(0);
      if (values.size() >= 3) {
        b = values.get(1);
        c = values.get(2);
      }
    }
  }

  /** Resets the contents of navigableSet to have elements a, c, for the navigation tests. */
  protected void resetWithHole() {
    super.resetContainer(getSubjectGenerator().create(a, c));
    navigableSet = (NavigableSet<E>) getSet();
  }

  @CollectionFeature.Require(SUPPORTS_REMOVE)
  @CollectionSize.Require(ZERO)
  public void testEmptySetPollFirst() {
    assertNull(navigableSet.pollFirst());
  }

  @CollectionSize.Require(ZERO)
  public void testEmptySetNearby() {
    assertNull(navigableSet.lower(e0()));
    assertNull(navigableSet.floor(e0()));
    assertNull(navigableSet.ceiling(e0()));
    assertNull(navigableSet.higher(e0()));
  }

  @CollectionFeature.Require(SUPPORTS_REMOVE)
  @CollectionSize.Require(ZERO)
  public void testEmptySetPollLast() {
    assertNull(navigableSet.pollLast());
  }

  @CollectionFeature.Require(SUPPORTS_REMOVE)
  @CollectionSize.Require(ONE)
  public void testSingletonSetPollFirst() {
    assertEquals(a, navigableSet.pollFirst());
    assertTrue(navigableSet.isEmpty());
  }

  @CollectionSize.Require(ONE)
  public void testSingletonSetNearby() {
    assertNull(navigableSet.lower(e0()));
    assertEquals(a, navigableSet.floor(e0()));
    assertEquals(a, navigableSet.ceiling(e0()));
    assertNull(navigableSet.higher(e0()));
  }

  @CollectionFeature.Require(SUPPORTS_REMOVE)
  @CollectionSize.Require(ONE)
  public void testSingletonSetPollLast() {
    assertEquals(a, navigableSet.pollLast());
    assertTrue(navigableSet.isEmpty());
  }

  @CollectionFeature.Require(SUPPORTS_REMOVE)
  @CollectionSize.Require(SEVERAL)
  public void testPollFirst() {
    assertEquals(a, navigableSet.pollFirst());
    assertEquals(values.subList(1, values.size()), Helpers.copyToList(navigableSet));
  }

  @CollectionFeature.Require(absent = SUPPORTS_REMOVE)
  public void testPollFirstUnsupported() {
    try {
      navigableSet.pollFirst();
      fail();
    } catch (UnsupportedOperationException e) {
    }
  }

  @CollectionSize.Require(SEVERAL)
  public void testLowerHole() {
    resetWithHole();
    assertEquals(null, navigableSet.lower(a));
    assertEquals(a, navigableSet.lower(b));
    assertEquals(a, navigableSet.lower(c));
  }

  @CollectionSize.Require(SEVERAL)
  public void testFloorHole() {
    resetWithHole();
    assertEquals(a, navigableSet.floor(a));
    assertEquals(a, navigableSet.floor(b));
    assertEquals(c, navigableSet.floor(c));
  }

  @CollectionSize.Require(SEVERAL)
  public void testCeilingHole() {
    resetWithHole();
    assertEquals(a, navigableSet.ceiling(a));
    assertEquals(c, navigableSet.ceiling(b));
    assertEquals(c, navigableSet.ceiling(c));
  }

  @CollectionSize.Require(SEVERAL)
  public void testHigherHole() {
    resetWithHole();
    assertEquals(c, navigableSet.higher(a));
    assertEquals(c, navigableSet.higher(b));
    assertEquals(null, navigableSet.higher(c));
  }

  /*
   * TODO(cpovirk): make "too small" and "too large" elements available for better navigation
   * testing. At that point, we may be able to eliminate the "hole" tests, which would mean that
   * ContiguousSet's tests would no longer need to suppress them.
   */
  @CollectionSize.Require(SEVERAL)
  public void testLower() {
    assertEquals(null, navigableSet.lower(a));
    assertEquals(a, navigableSet.lower(b));
    assertEquals(b, navigableSet.lower(c));
  }

  @CollectionSize.Require(SEVERAL)
  public void testFloor() {
    assertEquals(a, navigableSet.floor(a));
    assertEquals(b, navigableSet.floor(b));
    assertEquals(c, navigableSet.floor(c));
  }

  @CollectionSize.Require(SEVERAL)
  public void testCeiling() {
    assertEquals(a, navigableSet.ceiling(a));
    assertEquals(b, navigableSet.ceiling(b));
    assertEquals(c, navigableSet.ceiling(c));
  }

  @CollectionSize.Require(SEVERAL)
  public void testHigher() {
    assertEquals(b, navigableSet.higher(a));
    assertEquals(c, navigableSet.higher(b));
    assertEquals(null, navigableSet.higher(c));
  }

  @CollectionFeature.Require(SUPPORTS_REMOVE)
  @CollectionSize.Require(SEVERAL)
  public void testPollLast() {
    assertEquals(c, navigableSet.pollLast());
    assertEquals(values.subList(0, values.size() - 1), Helpers.copyToList(navigableSet));
  }

  @CollectionFeature.Require(absent = SUPPORTS_REMOVE)
  public void testPollLastUnsupported() {
    try {
      navigableSet.pollLast();
      fail();
    } catch (UnsupportedOperationException e) {
    }
  }

  @CollectionSize.Require(SEVERAL)
  public void testDescendingNavigation() {
    List<E> descending = new ArrayList<>();
    for (Iterator<E> i = navigableSet.descendingIterator(); i.hasNext(); ) {
      descending.add(i.next());
    }
    Collections.reverse(descending);
    assertEquals(values, descending);
  }

  public void testEmptySubSet() {
    NavigableSet<E> empty = navigableSet.subSet(e0(), false, e0(), false);
    assertEquals(new TreeSet<E>(), empty);
  }

  /*
   * TODO(cpovirk): more testing of subSet/headSet/tailSet/descendingSet? and/or generate derived
   * suites?
   */

  /**
   * Returns the {@link Method} instances for the test methods in this class that create a set with
   * a "hole" in it so that set tests of {@code ContiguousSet} can suppress them with {@code
   * FeatureSpecificTestSuiteBuilder.suppressing()}.
   */
  /*
   * TODO(cpovirk): or we could make HOLES_FORBIDDEN a feature. Or we could declare that
   * implementations are permitted to throw IAE if a hole is requested, and we could update
   * test*Hole to permit IAE. (But might this ignore genuine bugs?) But see the TODO above
   * testLower, which could make this all unnecessary
   */
  public static Method[] getHoleMethods() {
    return new Method[] {
      Helpers.getMethod(NavigableSetNavigationTester.class, "testLowerHole"),
      Helpers.getMethod(NavigableSetNavigationTester.class, "testFloorHole"),
      Helpers.getMethod(NavigableSetNavigationTester.class, "testCeilingHole"),
      Helpers.getMethod(NavigableSetNavigationTester.class, "testHigherHole"),
    };
  }
}
