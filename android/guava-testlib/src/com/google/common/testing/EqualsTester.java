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

package com.google.common.testing;

import static com.google.common.base.Preconditions.checkNotNull;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

import com.google.common.annotations.GwtCompatible;
import com.google.common.base.Equivalence;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import java.util.List;

/**
 * Tester for equals() and hashCode() methods of a class.
 *
 * <p>The simplest use case is:
 *
 * <pre>
 * new EqualsTester().addEqualityGroup(foo).testEquals();
 * </pre>
 *
 * <p>This tests {@code foo.equals(foo)}, {@code foo.equals(null)}, and a few other operations.
 *
 * <p>For more extensive testing, add multiple equality groups. Each group should contain objects
 * that are equal to each other but unequal to the objects in any other group. For example:
 *
 * <pre>
 * new EqualsTester()
 *     .addEqualityGroup(new User("page"), new User("page"))
 *     .addEqualityGroup(new User("sergey"))
 *     .testEquals();
 * </pre>
 *
 * <p>This tests:
 *
 * <ul>
 *   <li>comparing each object against itself returns true
 *   <li>comparing each object against null returns false
 *   <li>comparing each object against an instance of an incompatible class returns false
 *   <li>comparing each pair of objects within the same equality group returns true
 *   <li>comparing each pair of objects from different equality groups returns false
 *   <li>the hash codes of any two equal objects are equal
 * </ul>
 *
 * <p>When a test fails, the error message labels the objects involved in the failed comparison as
 * follows:
 *
 * <ul>
 *   <li>"{@code [group }<i>i</i>{@code , item }<i>j</i>{@code ]}" refers to the
 *       <i>j</i><sup>th</sup> item in the <i>i</i><sup>th</sup> equality group, where both equality
 *       groups and the items within equality groups are numbered starting from 1. When either a
 *       constructor argument or an equal object is provided, that becomes group 1.
 * </ul>
 *
 * @author Jim McMaster
 * @author Jige Yu
 * @since 10.0
 */
@GwtCompatible
public final class EqualsTester {
  private static final int REPETITIONS = 3;

  private final List<List<Object>> equalityGroups = Lists.newArrayList();
  private final RelationshipTester.ItemReporter itemReporter;

  /** Constructs an empty EqualsTester instance */
  public EqualsTester() {
    this(new RelationshipTester.ItemReporter());
  }

  EqualsTester(RelationshipTester.ItemReporter itemReporter) {
    this.itemReporter = checkNotNull(itemReporter);
  }

  /**
   * Adds {@code equalityGroup} with objects that are supposed to be equal to each other and not
   * equal to any other equality groups added to this tester.
   */
  public EqualsTester addEqualityGroup(Object... equalityGroup) {
    checkNotNull(equalityGroup);
    equalityGroups.add(ImmutableList.copyOf(equalityGroup));
    return this;
  }

  /** Run tests on equals method, throwing a failure on an invalid test */
  public EqualsTester testEquals() {
    RelationshipTester<Object> delegate =
        new RelationshipTester<>(
            Equivalence.equals(), "Object#equals", "Object#hashCode", itemReporter);
    for (List<Object> group : equalityGroups) {
      delegate.addRelatedGroup(group);
    }
    for (int run = 0; run < REPETITIONS; run++) {
      testItems();
      delegate.test();
    }
    return this;
  }

  private void testItems() {
    for (Object item : Iterables.concat(equalityGroups)) {
      assertTrue(item + " must not be Object#equals to null", !item.equals(null));
      assertTrue(
          item + " must not be Object#equals to an arbitrary object of another class",
          !item.equals(NotAnInstance.EQUAL_TO_NOTHING));
      assertTrue(item + " must be Object#equals to itself", item.equals(item));
      assertEquals(
          "the Object#hashCode of " + item + " must be consistent",
          item.hashCode(),
          item.hashCode());
      if (!(item instanceof String)) {
        assertTrue(
            item + " must not be Object#equals to its Object#toString representation",
            !item.equals(item.toString()));
      }
    }
  }

  /**
   * Class used to test whether equals() correctly handles an instance of an incompatible class.
   * Since it is a private inner class, the invoker can never pass in an instance to the tester
   */
  private enum NotAnInstance {
    EQUAL_TO_NOTHING;
  }
}
